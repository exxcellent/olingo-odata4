/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.fit;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.grants.refresh.RefreshTokenGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.routing.RoutingSupport;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.olingo.client.core.http.AbstractOAuth2HttpClientFactory;
import org.apache.olingo.client.core.http.OAuth2Exception;
import org.apache.olingo.fit.rest.OAuth2Provider;
import org.apache.cxf.rs.security.oauth2.client.Consumer;

import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.xml.XmlMapper;

public class CXFOAuth2HttpClientFactory extends AbstractOAuth2HttpClientFactory {

  private static final Consumer OAUTH2_CONSUMER = new Consumer(OAuth2Provider.CLIENT_ID, OAuth2Provider.CLIENT_SECRET);

  private ClientAccessToken accessToken;

  public CXFOAuth2HttpClientFactory(final URI oauth2GrantServiceURI, final URI oauth2TokenServiceURI) {
    super(oauth2GrantServiceURI, oauth2TokenServiceURI);
  }

  private WebClient getAccessTokenService() {
    final JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    bean.setAddress(oauth2TokenServiceURI.toASCIIString());
    bean.setUsername("odatajclient");
    bean.setPassword("odatajclient");
    return bean.createWebClient().type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        .accept(MediaType.APPLICATION_JSON_TYPE);
  }

  @Override
  protected boolean isInited() throws OAuth2Exception {
    return accessToken != null;
  }

  @Override
  protected void init() throws OAuth2Exception {
    final URI authURI = OAuthClientUtils.getAuthorizationURI(
        oauth2GrantServiceURI.toASCIIString(),
        OAuth2Provider.CLIENT_ID,
        OAuth2Provider.REDIRECT_URI,
        null,
        "foo bar");

    // Disable automatic redirects handling
    final CloseableHttpClient httpClient = HttpClients.custom()
        .disableRedirectHandling()
        .build();

    JsonNode oAuthAuthorizationData = null;
    String authenticityCookie = null;
    try {
      // 1. Need to (basic) authenticate against the OAuth2 service
      final HttpGet method = new HttpGet(authURI);
      method.addHeader("Authorization", "Basic " + Base64.encodeBase64String("odatajclient:odatajclient".getBytes(
          StandardCharsets.UTF_8)));
      final ClassicHttpResponse response = (ClassicHttpResponse) httpClient
          .executeOpen(RoutingSupport.determineHost(method), method, null);

      // 2. Pull out OAuth2 authorization data and "authenticity" cookie (CXF specific)
      oAuthAuthorizationData = XmlMapper.builder().build()
          .readTree(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8, Integer.MAX_VALUE));

      final Header setCookieHeader = response.getFirstHeader("Set-Cookie");
      response.close();
      if (setCookieHeader == null) {
        throw new IllegalStateException("OAuth flow is broken");
      }
      authenticityCookie = setCookieHeader.getValue();
    } catch (Exception e) {
      throw new OAuth2Exception(e);
    }

    String code = null;
    try {
      // 3. Submit the HTTP form for allowing access to the application
      final URI location = new URIBuilder(oAuthAuthorizationData.get("replyTo").asString())
          .addParameter("session_authenticity_token", oAuthAuthorizationData.get("authenticityToken").asString())
          .addParameter("client_id", oAuthAuthorizationData.get("clientId").asString())
          .addParameter("redirect_uri", oAuthAuthorizationData.get("redirectUri").asString())
          .addParameter("oauthDecision", "allow").addParameter("scope", "foo bar").build();
      final HttpGet method = new HttpGet(location);
      method.addHeader("Authorization", "Basic " + Base64.encodeBase64String("odatajclient:odatajclient".getBytes(
          StandardCharsets.UTF_8)));
      method.addHeader("Cookie", authenticityCookie);

      final ClassicHttpResponse response = (ClassicHttpResponse) httpClient
          .executeOpen(RoutingSupport.determineHost(method), method, null);

      final Header locationHeader = response.getFirstHeader("Location");
      if (response.getCode() != 303 || locationHeader == null) {
        throw new IllegalStateException("OAuth flow is broken");
      }

      // 4. Get the authorization code value out of this last redirect
      code = StringUtils.substringAfterLast(locationHeader.getValue(), "=");

      EntityUtils.consume(response.getEntity());
      response.close();
    } catch (Exception e) {
      throw new OAuth2Exception(e);
    }

    // 5. Obtain the access token
    try {
      accessToken = OAuthClientUtils.getAccessToken(
          getAccessTokenService(), OAUTH2_CONSUMER, new AuthorizationCodeGrant(code));
    } catch (OAuthServiceException e) {
      throw new OAuth2Exception(e);
    }

    if (accessToken == null) {
      throw new OAuth2Exception("No OAuth2 access token");
    }
  }

  @Override
  protected String getAuthorizationHeaderValue() {
    if (accessToken == null) {
      return null;
    }
    return accessToken.getTokenType() + " " + accessToken.getTokenKey();
  }

  @Override
  protected void accessToken(final CloseableHttpClient client) throws OAuth2Exception {
    // Token is applied via getAuthorizationHeaderValue() called from the parent request interceptor.
  }

  @Override
  protected void refreshToken(final CloseableHttpClient client) throws OAuth2Exception {
    final String refreshToken = accessToken.getRefreshToken();
    if (refreshToken == null) {
      throw new OAuth2Exception("No OAuth2 refresh token");
    }

    // refresh the token
    try {
      accessToken = OAuthClientUtils.getAccessToken(
          getAccessTokenService(), OAUTH2_CONSUMER, new RefreshTokenGrant(refreshToken));
    } catch (OAuthServiceException e) {
      throw new OAuth2Exception(e);
    }
  }

}
