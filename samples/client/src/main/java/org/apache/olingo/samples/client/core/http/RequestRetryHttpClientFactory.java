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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.olingo.client.api.http.ODataHttpClient;
import org.apache.olingo.client.core.http.DefaultHttpClientFactory;
import org.apache.olingo.commons.api.http.HttpMethod;

/**
 * Shows how to install a custom exception recovery mechanism.
 * <a
 * href="http://svn.apache.org/repos/asf/httpcomponents/site/httpcomponents-client-4.2.x/tutorial/html/fundamentals.html#d5e281">More
 * information</a>.
 */
public class RequestRetryHttpClientFactory extends DefaultHttpClientFactory {

  @Override
  public ODataHttpClient create(final HttpMethod method, final URI uri) {
    final HttpRequestRetryStrategy myRetryStrategy = new HttpRequestRetryStrategy() {

      @Override
      public boolean retryRequest(final HttpRequest request, final IOException exception,
          final int executionCount, final HttpContext context) {
        if (executionCount >= 5) {
          return false;
        }
        if (exception instanceof InterruptedIOException) {
          return false;
        }
        if (exception instanceof UnknownHostException) {
          return false;
        }
        if (exception instanceof ConnectException) {
          return false;
        }
        if (exception instanceof SSLException) {
          return false;
        }
        // Retry idempotent (non-entity-enclosing) requests
        return true;
      }

      @Override
      public boolean retryRequest(final HttpResponse response, final int executionCount,
          final HttpContext context) {
        return false;
      }

      @Override
      public TimeValue getRetryInterval(final HttpResponse response, final int executionCount,
          final HttpContext context) {
        return TimeValue.ZERO_MILLISECONDS;
      }
    };

    return DefaultHttpClientFactory.wrapHC5Client(HttpClients.custom()
        .setUserAgent(USER_AGENT)
        .setRetryStrategy(myRetryStrategy)
        .build());
  }

}
