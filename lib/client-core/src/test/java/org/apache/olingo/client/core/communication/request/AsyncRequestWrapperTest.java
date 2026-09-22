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
package org.apache.olingo.client.core.communication.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.olingo.client.api.Configuration;
import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.ODataBatchableRequest;
import org.apache.olingo.client.api.communication.request.ODataRequest;
import org.apache.olingo.client.api.communication.response.AsyncResponseWrapper;
import org.apache.olingo.client.api.communication.response.ODataResponse;
import org.apache.olingo.client.api.domain.ClientInvokeResult;
import org.apache.olingo.client.api.http.HttpClientFactory;
import org.apache.olingo.client.api.http.HttpUriRequestFactory;
import org.apache.olingo.client.api.http.ODataHttpClient;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.client.core.communication.request.AsyncRequestWrapperImpl.AsyncResponseWrapperImpl;
import org.apache.olingo.client.core.communication.request.batch.ODataBatchRequestImpl;
import org.apache.olingo.client.core.communication.request.invoke.ODataInvokeRequestImpl;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.junit.jupiter.api.Test;

public class AsyncRequestWrapperTest {

  @Test
  public void testBatchReq() throws URISyntaxException {

    ODataClient client = ODataClientFactory.getClient();
    URI uri = new URI("localhost:8080");
    AsyncBatchRequestWrapperImpl req = new AsyncBatchRequestWrapperImpl(client,
        client.getBatchRequestFactory().getBatchRequest("root"));
    assertNotNull(req.addChangeset());
    ODataBatchableRequest request = new ODataInvokeRequestImpl<ClientInvokeResult>(
        client, ClientInvokeResult.class, HttpMethod.GET, uri);
    req.addRetrieve(request);
    req.addOutsideUpdate(request);
    assertNotNull(client.getAsyncRequestFactory().getAsyncRequestWrapper(request));
    ODataBatchRequestImpl batchRequest = new ODataBatchRequestImpl(client, uri);
    assertNotNull(client.getAsyncRequestFactory().getAsyncBatchRequestWrapper(batchRequest));
    assertNotNull(req.wait(10));
  }

  @Test
  public void testReq() throws URISyntaxException {

    ODataClient client = ODataClientFactory.getClient();
    URI uri = new URI("localhost:8080");
    AsyncRequestWrapperImpl<ODataResponse> req = new AsyncRequestWrapperImpl<>(client,
        client.getBatchRequestFactory().getBatchRequest("root"));
    assertNotNull(req);
    req.checkRequest(client, null);
    assertNotNull(req.callback(uri));
    req.extendHeader("header", "value");
    AsyncResponseWrapper<ODataResponse> res = req.new AsyncResponseWrapperImpl();
    res.forceNextMonitorCheck(uri);
  }

  private AsyncRequestWrapperImpl<ODataResponse> createAsyncRequestWrapperImplWithRetryAfter(int retryAfter)
      throws IOException, URISyntaxException {

    ODataHttpClient httpClient = mock(ODataHttpClient.class);
    ODataClient oDataClient = mock(ODataClient.class);
    Configuration configuration = mock(Configuration.class);
    @SuppressWarnings("unchecked")
    HttpClientFactory<ODataHttpClient> httpClientFactory = mock(HttpClientFactory.class);
    HttpUriRequestFactory httpUriRequestFactory = mock(HttpUriRequestFactory.class);
    HttpUriRequestBase httpUriRequest = new HttpGet("http://localhost/path");

    when(oDataClient.getConfiguration()).thenReturn(configuration);
    when(configuration.getHttpClientFactory()).thenReturn(httpClientFactory);
    when(configuration.getHttpUriRequestFactory()).thenReturn(httpUriRequestFactory);
    when(httpClientFactory.create(any(), any())).thenReturn(httpClient);
    when(httpUriRequestFactory.create(any(), any())).thenReturn(httpUriRequest);

    BasicClassicHttpResponse firstResponse = new BasicClassicHttpResponse(202);
    firstResponse.addHeader(HttpHeader.LOCATION, "http://localhost/monitor");
    firstResponse.addHeader(HttpHeader.RETRY_AFTER, String.valueOf(retryAfter));
    when(httpClient.execute(any(ClassicHttpRequest.class))).thenReturn(firstResponse);

    ODataRequest oDataRequest = mock(ODataRequest.class);
    when(oDataRequest.getURI()).thenReturn(new URI("http://localhost/path"));

    return new AsyncRequestWrapperImpl<ODataResponse>(oDataClient, oDataRequest);
  }

  @Test
  public void testTooBigRetryAfter() throws IOException, URISyntaxException {

    AsyncRequestWrapperImpl<ODataResponse> req = createAsyncRequestWrapperImplWithRetryAfter(Integer.MAX_VALUE);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    assertEquals(AsyncResponseWrapperImpl.MAX_RETRY_AFTER,
        ((AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl) wrappedResponse).retryAfter);
  }

  @Test
  public void testZeroRetryAfter() throws IOException, URISyntaxException {

    AsyncRequestWrapperImpl<ODataResponse> req = createAsyncRequestWrapperImplWithRetryAfter(0);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    assertEquals(0, ((AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl) wrappedResponse).retryAfter);
  }

  @Test
  public void testNegativeRetryAfter() throws IOException, URISyntaxException {

    AsyncRequestWrapperImpl<ODataResponse> req = createAsyncRequestWrapperImplWithRetryAfter(-1);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    assertEquals(AsyncResponseWrapperImpl.DEFAULT_RETRY_AFTER,
        ((AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl) wrappedResponse).retryAfter);
  }

  @Test
  public void testRetryAfter() throws IOException, URISyntaxException {

    int retryAfter = 7;
    assertNotEquals(retryAfter, AsyncResponseWrapperImpl.DEFAULT_RETRY_AFTER);
    AsyncRequestWrapperImpl<ODataResponse> req = createAsyncRequestWrapperImplWithRetryAfter(retryAfter);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    assertEquals(retryAfter,
        ((AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl) wrappedResponse).retryAfter);
  }

  @Test
  public void testWrapper() {

    Wrapper<String> wrap = new Wrapper<>();
    wrap.setWrapped("test");
    assertEquals("test", wrap.getWrapped());
  }

  @Test
  public void testException() {

    AsyncRequestException ex = new AsyncRequestException("Exception");
    assertEquals("Exception", ex.getMessage());
  }

  @SuppressWarnings("unchecked")
  private <R extends AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl> R 
    createAsyncRequestWrapperImplWithLocation(String target, String location)
      throws IOException, URISyntaxException {

    ODataHttpClient httpClient = mock(ODataHttpClient.class);
    ODataClient oDataClient = mock(ODataClient.class);
    Configuration configuration = mock(Configuration.class);
    HttpClientFactory<ODataHttpClient> httpClientFactory = mock(HttpClientFactory.class);
    HttpUriRequestFactory httpUriRequestFactory = mock(HttpUriRequestFactory.class);
    HttpUriRequestBase httpUriRequest = new HttpGet("http://localhost/path");

    when(oDataClient.getConfiguration()).thenReturn(configuration);
    when(configuration.getHttpClientFactory()).thenReturn(httpClientFactory);
    when(configuration.getHttpUriRequestFactory()).thenReturn(httpUriRequestFactory);
    when(httpClientFactory.create(any(), any())).thenReturn(httpClient);
    when(httpUriRequestFactory.create(any(), any())).thenReturn(httpUriRequest);

    BasicClassicHttpResponse firstResponse = new BasicClassicHttpResponse(202);
    firstResponse.addHeader(HttpHeader.LOCATION, location);
    when(httpClient.execute(any(ClassicHttpRequest.class))).thenReturn(firstResponse);

    ODataRequest oDataRequest = mock(ODataRequest.class);
    when(oDataRequest.getURI()).thenReturn(new URI(target));

    AsyncRequestWrapperImpl<ODataResponse> req = new AsyncRequestWrapperImpl<>(oDataClient, oDataRequest);
    AsyncResponseWrapper<ODataResponse> wrappedResponse = req.execute();
    assertTrue(wrappedResponse instanceof AsyncResponseWrapperImpl);
    return (R) wrappedResponse;
  }

  @Test
  public void testLocationWithInvalidScheme() throws IOException, URISyntaxException {
    assertThrows(AsyncRequestException.class, () -> {
      String target = "https://server/path";
      String location = "http://server/path";
      createAsyncRequestWrapperImplWithLocation(target, location);
    });
  }

  @Test
  public void testLocationWithInvalidHost() throws IOException, URISyntaxException {
    assertThrows(AsyncRequestException.class, () -> {
      String target = "http://server/path";
      String location = "http://something.else/path";
      createAsyncRequestWrapperImplWithLocation(target, location);
    });
  }

  @Test
  public void testLocationWithInvalidPort() throws IOException, URISyntaxException {
    assertThrows(AsyncRequestException.class, () -> {
      String target = "http://server/path";
      String location = "http://server:8080/path";
      createAsyncRequestWrapperImplWithLocation(target, location);
    });
  }

  @Test
  public void testLocationWithDifferentPaths() throws IOException, URISyntaxException {
    String target = "http://server/path";
    String location = "http://server/monitor";
    AsyncRequestWrapperImpl<ODataResponse>.AsyncResponseWrapperImpl wrapper = createAsyncRequestWrapperImplWithLocation(
        target, location);
    assertEquals(new URI(location), wrapper.location);
  }

}
