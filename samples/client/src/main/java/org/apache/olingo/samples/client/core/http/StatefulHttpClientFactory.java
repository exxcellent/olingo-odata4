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
 * Shows how to work with stateful HTTP connections.
 * <br/>
 * In HC5, connection state management is handled automatically via connection pooling.
 * User-specific connection state can be managed via HttpContext in request execution.
 * <a
 * href="http://svn.apache.org/repos/asf/httpcomponents/site/httpcomponents-client-4.2.x/tutorial/html/advanced.html#stateful_conn">More
 * information</a>.
 */
public class StatefulHttpClientFactory extends DefaultHttpClientFactory {

  @Override
  public ODataHttpClient create(final HttpMethod method, final URI uri) {
    // In HC5, stateful connections are managed via connection pooling and HttpContext.
    // User-token-based connection affinity of HC4 is replaced by connection pooling strategies.
    return DefaultHttpClientFactory.wrapHC5Client(HttpClients.custom()
        .setUserAgent(USER_AGENT)
        .build());
  }

}
