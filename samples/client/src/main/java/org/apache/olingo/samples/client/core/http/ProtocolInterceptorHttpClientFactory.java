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

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.olingo.client.api.http.ODataHttpClient;
import org.apache.olingo.client.core.http.DefaultHttpClientFactory;
import org.apache.olingo.commons.api.http.HttpMethod;

/**
 * Shows how to install HTTP protocol interceptors, an easy handle to hook into HTTP request / response processing.
 * <br/>
 * Usually protocol interceptors are expected to act upon one specific header or a group of related headers of the
 * incoming message, or populate the outgoing message with one specific header or a group of related headers. Protocol
 * interceptors can also manipulate content entities enclosed with messages - transparent content compression /
 * decompression being a good example. Usually this is accomplished by using the 'Decorator' pattern where a wrapper
 * entity class is used to decorate the original entity. Several protocol interceptors can be combined to form one
 * logical unit.
 * <a
 * href="http://svn.apache.org/repos/asf/httpcomponents/site/httpcomponents-client-4.2.x/tutorial/html/fundamentals.html#protocol_interceptors">More
 * information</a>.
 */
public class ProtocolInterceptorHttpClientFactory extends DefaultHttpClientFactory {

  @Override
  public ODataHttpClient create(final HttpMethod method, final URI uri) {
    return DefaultHttpClientFactory.wrapHC5Client(HttpClients.custom()
        .setUserAgent(USER_AGENT)
        .addRequestInterceptorLast((request, entity, context) ->
            request.addHeader("CUSTOM_HEADER", "CUSTOM VALUE"))
        .addResponseInterceptorLast((response, entity, context) -> {
          if (response.containsHeader("ANOTHER_CUSTOM_HEADER")
              && "ANOTHER CUSTOM VALUE".equals(response.getFirstHeader("ANOTHER_CUSTOM_HEADER").getValue())) {
            // do something
          }
        })
        .build());
  }

}
