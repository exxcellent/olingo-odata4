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
package org.apache.olingo.fit.tecsvc.client;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.olingo.commons.api.format.ContentType;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractParamTecSvcITCase extends AbstractTecSvcITCase {

  public ContentType contentType;

  /**
   * Sets the default content type for each test run.
   * Override in subclasses to use a different content type.
   */
  @BeforeEach
  public void setUpContentType() {
    if (contentType == null) {
      contentType = ContentType.APPLICATION_JSON;
    }
  }

  @Override
  protected ContentType getContentType() {
    return contentType;
  }

  protected void assertContentType(final String content) {
    assertThat(content, startsWith(contentType.toContentTypeString()));
  }

  protected boolean isJson() {
    return ContentType.JSON.isCompatible(contentType);
  }

  protected void assertShortOrInt(final int value, final Object n) {
    assertTrue(n instanceof Number);
    assertEquals(value, ((Number) n).intValue());
  }
}
