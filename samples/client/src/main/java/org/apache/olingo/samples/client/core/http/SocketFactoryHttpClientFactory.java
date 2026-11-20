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
package org.apache.olingo.samples.client.core.http;

import java.net.URI;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.reactor.ssl.SSLBufferMode;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.olingo.client.api.http.ODataHttpClient;
import org.apache.olingo.client.core.http.AbstractHttpClientFactory;
import org.apache.olingo.client.core.http.DefaultHttpClientFactory;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpMethod;

/**
 * Shows how to customize the way how the underlying network sockets are managed by the HTTP component; the specific
 * sample is about how to trust self-signed SSL certificates.
 * <a
 * href="http://svn.apache.org/repos/asf/httpcomponents/site/httpcomponents-client-4.2.x/tutorial/html/connmgmt.html#d5e512">More
 * information</a>.
 */
public class SocketFactoryHttpClientFactory extends AbstractHttpClientFactory<ODataHttpClient> {

  @Override
  public ODataHttpClient create(final HttpMethod method, final URI uri) {
    try {
      final SSLContext sslContext = SSLContextBuilder.create()
          .loadTrustMaterial(null, (chain, authType) -> true)
          .build();

      TlsSocketStrategy tls = new DefaultClientTlsStrategy(sslContext, HttpsSupport.getSystemProtocols(),
          HttpsSupport.getSystemCipherSuits(),
          SSLBufferMode.STATIC,
          HostnameVerificationPolicy.BUILTIN, NoopHostnameVerifier.INSTANCE);
      return DefaultHttpClientFactory.wrapHC5Client(HttpClients.custom()
          .setUserAgent(USER_AGENT)
          .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create().setTlsSocketStrategy(tls).build())
          .build());
    } catch (Exception e) {
      throw new ODataRuntimeException(e);
    }
  }

}
