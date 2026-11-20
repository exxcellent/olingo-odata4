/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.client.core.http;

import java.io.IOException;
import java.net.URI;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.olingo.client.api.http.ODataHttpClient;
import org.apache.olingo.commons.api.http.HttpMethod;

public abstract class AbstractOAuth2HttpClientFactory
        extends AbstractHttpClientFactory<ODataHttpClient> {

  protected final URI oauth2GrantServiceURI;

  protected final URI oauth2TokenServiceURI;

  protected HttpUriRequestBase currentRequest;

  public AbstractOAuth2HttpClientFactory(final URI oauth2GrantServiceURI, final URI oauth2TokenServiceURI) {
    super();
    this.oauth2GrantServiceURI = oauth2GrantServiceURI;
    this.oauth2TokenServiceURI = oauth2TokenServiceURI;
  }

  protected abstract boolean isInited() throws OAuth2Exception;

  protected abstract void init() throws OAuth2Exception;

  protected abstract void accessToken(CloseableHttpClient client) throws OAuth2Exception;

  protected abstract void refreshToken(CloseableHttpClient client) throws OAuth2Exception;

  /**
   * Returns the value for the {@code Authorization} HTTP header to be added to every outgoing
   * request, e.g. {@code "Bearer <token>"}. Return {@code null} to skip adding the header.
   * Subclasses should override this to expose their stored access token.
   */
  protected String getAuthorizationHeaderValue() {
    return null;
  }

  @Override
  public ODataHttpClient create(final HttpMethod method, final URI uri) {
    if (!isInited()) {
      init();
    }

    final CloseableHttpClient[] holder = new CloseableHttpClient[1];

    holder[0] = HttpClients.custom()
        .setUserAgent(AbstractHttpClientFactory.USER_AGENT)
        .addRequestInterceptorLast(new HttpRequestInterceptor() {
          @Override
          public void process(final HttpRequest request, final EntityDetails entityDetails,
              final HttpContext context) throws HttpException, IOException {
            if (request instanceof HttpUriRequestBase) {
              currentRequest = (HttpUriRequestBase) request;
            } else {
              currentRequest = null;
            }
            final String authHeader = getAuthorizationHeaderValue();
            if (authHeader != null && !request.containsHeader(HttpHeaders.AUTHORIZATION)) {
              request.addHeader(HttpHeaders.AUTHORIZATION, authHeader);
            }
          }
        })
        .addResponseInterceptorLast(new HttpResponseInterceptor() {
          @Override
          public void process(final HttpResponse response, final EntityDetails entityDetails,
              final HttpContext context) throws HttpException, IOException {
            if (response.getCode() == HttpStatus.SC_UNAUTHORIZED) {
              refreshToken(holder[0]);

              if (currentRequest != null) {
                try {
                  holder[0].execute(currentRequest, res -> res);
                } catch (Exception e) {
                  // ignore retry errors
                }
              }
            }
          }
        })
        .build();

    accessToken(holder[0]);

    return DefaultHttpClientFactory.wrapHC5Client(holder[0]);
  }

}
