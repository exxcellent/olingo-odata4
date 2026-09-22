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

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.olingo.client.api.http.ODataHttpClient;
import org.apache.olingo.commons.api.http.HttpMethod;

/**
 * Default implementation returning HttpClients with default parameters.
 */
public class DefaultHttpClientFactory extends AbstractHttpClientFactory<ODataHttpClient> {

  private static class HC5ClientAdapter implements ODataHttpClient {
    private final HttpClient client;

    public HC5ClientAdapter(HttpClient client) {
      if (!AutoCloseable.class.isAssignableFrom(client.getClass())) {
        throw new IllegalArgumentException(
            "Given client must implement '" + AutoCloseable.class.getName() + "' interface");
      }
      this.client = client;
    }

    @SuppressWarnings("deprecation")
    @Override
    public HttpResponse execute(ClassicHttpRequest request) throws IOException {
      //TODO implement something to avoid auto closing of response in case having a HttpClientResponseHandler
      return client.execute(request);
    }
    
    @Override
    public void close() throws RuntimeException {
      try {
        ((AutoCloseable) client).close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Adapt a common HC5 client to Olingo environment
   */
  public static ODataHttpClient wrapHC5Client(HttpClient client) {
    return new HC5ClientAdapter(client);
  }

  @Override
  public ODataHttpClient create(final HttpMethod method, final URI uri) {
    return wrapHC5Client(HttpClients.custom().setUserAgent(USER_AGENT).build());
  }

}
