/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.olingo.client.core.communication.request;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.olingo.client.api.EdmEnabledODataClient;
import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.ODataClientErrorException;
import org.apache.olingo.client.core.communication.header.ODataErrorResponseChecker;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class AbstractRequest {

  /**
   * Logger.
   */
  protected static final Logger LOG = LoggerFactory.getLogger(AbstractRequest.class);

  protected void checkRequest(final ODataClient odataClient, final HttpUriRequestBase request) {
    // If using an Edm enabled client, checks that the cached service root matches the request URI.
    // HC5: getRequestUri() returns only the path (no scheme/host); use getUri() for the full absolute URI.
    if (odataClient instanceof EdmEnabledODataClient) {
      final String requestUriStr;
      try {
        requestUriStr = request.getUri().toString();
      } catch (URISyntaxException e) {
        return; // cannot validate — skip check for malformed URIs
      }
      if (!requestUriStr.startsWith(((EdmEnabledODataClient) odataClient).getServiceRoot())) {
        throw new IllegalArgumentException(
                String.format("The current request URI %s does not match the configured service root %s",
                        requestUriStr,
                        ((EdmEnabledODataClient) odataClient).getServiceRoot()));
      }
    }
  }

  protected void checkResponse(
          final ODataClient odataClient, final ClassicHttpResponse response, final String accept) {

    if (response.getCode() >= 400) {
      final ContentType contentType = determineContentType(response, accept);
      try {
        final org.apache.hc.core5.http.message.StatusLine statusLine =
            new org.apache.hc.core5.http.message.StatusLine(response);
        final ODataRuntimeException exception = ODataErrorResponseChecker.checkResponse(
                odataClient,
                statusLine,
                response.getEntity() == null ? null : response.getEntity().getContent(),
                contentType);
        if (exception != null) {
          if (exception instanceof ODataClientErrorException) {
            ((ODataClientErrorException)exception).setHeaderInfo(response.getHeaders());
          }
          throw exception;
        }
      } catch (IOException e) {
        throw new ODataRuntimeException(
                "Received '" + response.getCode() + " " + response.getReasonPhrase()
                + "' but could not extract error body", e);
      }
    }
  }

  private static ContentType determineContentType(ClassicHttpResponse response, String accept) {
    if (response.getEntity() == null
            || response.getEntity().getContentType() == null
            || StringUtils.isBlank(response.getEntity().getContentType())) {
      return ContentType.fromAcceptHeader(accept);
    }
    try {
      return ContentType.create(response.getEntity().getContentType());
    } catch (Exception exception) {
      return ContentType.JSON;
    }
  }

}
