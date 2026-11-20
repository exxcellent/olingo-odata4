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

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.olingo.client.api.http.ODataHttpClient;
import org.apache.olingo.client.core.http.DefaultHttpClientFactory;
import org.apache.olingo.commons.api.http.HttpMethod;

/**
 * Shows how to work with HTTP cookies.
 * <a
 * href="http://svn.apache.org/repos/asf/httpcomponents/site/httpcomponents-client-4.2.x/tutorial/html/statemgmt.html#d5e669">More
 * information</a>.
 */
public class CookieHttpClientFactory extends DefaultHttpClientFactory {

  @Override
  public ODataHttpClient create(final HttpMethod method, final URI uri) {
    final CookieStore cookieStore = new BasicCookieStore();

    // Populate cookies if needed
    final BasicClientCookie cookie = new BasicClientCookie("name", "value");
    cookie.setDomain(".mycompany.com");
    cookie.setPath("/");
    cookieStore.addCookie(cookie);

    return DefaultHttpClientFactory.wrapHC5Client(HttpClients.custom()
        .setUserAgent(USER_AGENT)
        .setDefaultCookieStore(cookieStore)
        .build());
  }

}
