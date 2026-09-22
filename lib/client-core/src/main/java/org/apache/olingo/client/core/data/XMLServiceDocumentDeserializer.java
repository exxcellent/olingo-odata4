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
package org.apache.olingo.client.core.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.olingo.client.api.data.ResWrap;
import org.apache.olingo.client.api.data.ServiceDocument;
import org.apache.olingo.client.api.serialization.ODataDeserializerException;
import org.apache.olingo.client.core.serialization.JsonDeserializer;
import org.apache.olingo.client.core.uri.URIUtils;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.dataformat.xml.XmlFactory;
import tools.jackson.dataformat.xml.deser.FromXmlParser;

public class XMLServiceDocumentDeserializer extends JsonDeserializer {

  public XMLServiceDocumentDeserializer(final boolean serverMode) {
    super(serverMode);
  }

  private String getName(final JsonParser jp) throws IOException {
    jp.nextToken();
    if (jp.currentToken() == JsonToken.VALUE_STRING) {
      return jp.getString();
    }
    // Element has attributes (START_OBJECT): skip attribute name/value pair,
    // then the empty-key text-content property, and read the text value.
    // Jackson 3.x: getString() on START_OBJECT returns "{" instead of null,
    // so we must check currentToken() rather than the getString() return value.
    jp.nextToken(); // PROPERTY_NAME (attribute, e.g. "type")
    jp.nextToken(); // VALUE_STRING  (attribute value, e.g. "text")
    jp.nextToken(); // PROPERTY_NAME "" (text content key)
    jp.nextToken(); // VALUE_STRING  (text content)
    return jp.getString();
  }

  private ServiceDocumentItemImpl deserializeElement(final JsonParser jp, final String elementName)
      throws IOException {

    final ServiceDocumentItemImpl element = new ServiceDocumentItemImpl();
    for (; jp.currentToken() != JsonToken.END_OBJECT
        || !elementName.equals(((FromXmlParser) jp).getStaxReader().getLocalName()); jp.nextToken()) {

      final JsonToken token = jp.currentToken();
      if (token == JsonToken.PROPERTY_NAME) {
        if ("href".equals(jp.currentName())) {
          jp.nextToken();
          element.setUrl(jp.getString());
        } else if ("name".equals(jp.currentName())) {
          jp.nextToken();
          element.setName(jp.getString());
        } else if ("title".equals(jp.currentName())) {
          element.setTitle(getName(jp));
        }
      }
    }

    if (element.getName() == null) {
      element.setName(element.getTitle());
    }
    return element;
  }

  protected ResWrap<ServiceDocument> doDeserialize(final JsonParser jp) throws IOException {

    ServiceDocumentImpl sdoc = new ServiceDocumentImpl();

    URI contextURL = null;
    String metadataETag = null;
    String base = null;

    for (; jp.currentToken() != JsonToken.END_OBJECT
        || !"service".equals(((FromXmlParser) jp).getStaxReader().getLocalName()); jp.nextToken()) {

      final JsonToken token = jp.currentToken();
      if (token == JsonToken.PROPERTY_NAME) {
        if ("base".equals(jp.currentName())) {
          jp.nextToken();
          base = jp.getString();
        } else if ("context".equals(jp.currentName())) {
          jp.nextToken();
          contextURL = URI.create(jp.getString());
        } else if ("metadata-etag".equals(jp.currentName())) {
          jp.nextToken();
          metadataETag = jp.getString();
        } else if ("workspace".equals(jp.currentName())) {
          jp.nextToken();
          jp.nextToken();
          if ("title".equals(jp.currentName())) {
            sdoc.setTitle(getName(jp));
          }
        } else if ("collection".equals(jp.currentName())) {
          jp.nextToken();
          sdoc.getEntitySets().add(deserializeElement(jp, "collection"));
        } else if ("function-import".equals(jp.currentName())) {
          jp.nextToken();
          sdoc.getFunctionImports().add(deserializeElement(jp, "function-import"));
        } else if ("singleton".equals(jp.currentName())) {
          jp.nextToken();
          sdoc.getSingletons().add(deserializeElement(jp, "singleton"));
        } else if ("service-document".equals(jp.currentName())) {
          jp.nextToken();
          sdoc.getRelatedServiceDocuments().add(deserializeElement(jp, "service-document"));
        }
      }
    }

    sdoc.setMetadata((contextURL == null
        ? URIUtils.getURI(base, "$metadata")
        : URIUtils.getURI(base, contextURL.toASCIIString())).toASCIIString());

    return new ResWrap<>(
        contextURL == null ? null : URIUtils.getURI(sdoc.getBaseURI(), contextURL),
        metadataETag, sdoc);
  }

  public ResWrap<ServiceDocument> toServiceDocument(InputStream input) throws ODataDeserializerException {
    try {
      JsonParser parser = new XmlFactory().createParser(ObjectReadContext.empty(), input);
      return doDeserialize(parser);
    } catch (final IOException e) {
      throw new ODataDeserializerException(e);
    }
  }
}
