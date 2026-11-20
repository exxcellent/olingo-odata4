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

import java.net.URI;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.apache.olingo.client.api.http.ODataHttpClient;
import org.apache.olingo.commons.api.http.HttpMethod;

/**
 * Implementation for working behind an HTTP proxy (possibly requiring authentication); requires another concrete
 * {@link org.apache.olingo.client.api.http.HttpClientFactory} implementation acting as real HTTP client factory.
 */
public class ProxyWrappingHttpClientFactory extends AbstractHttpClientFactory<ODataHttpClient> {

  private final URI proxy;

  private String proxyUsername;

  private String proxyPassword;

  public ProxyWrappingHttpClientFactory(final URI proxy, final String proxyUsername, final String proxyPassword) {
    super();
    this.proxy = proxy;
    this.proxyUsername = proxyUsername;
    this.proxyPassword = proxyPassword;
  }

  @Override
  public ODataHttpClient create(final HttpMethod method, final URI uri) {
    final HttpHost proxyHost = new HttpHost(proxy.getHost(), proxy.getPort());
    final RequestConfig requestConfig = RequestConfig.custom()
            .setProxy(proxyHost)
            .build();

    final org.apache.hc.client5.http.impl.classic.HttpClientBuilder builder = HttpClients.custom()
            .setUserAgent(AbstractHttpClientFactory.USER_AGENT)
            .setDefaultRequestConfig(requestConfig);

    // Sets proxy authentication, if credentials were provided
    if (proxyUsername != null && proxyPassword != null) {
      final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(
              new AuthScope(proxyHost),
              new UsernamePasswordCredentials(proxyUsername, proxyPassword.toCharArray()));
      builder.setDefaultCredentialsProvider(credsProvider);
    }

    return DefaultHttpClientFactory.wrapHC5Client(builder.build());
  }

}
