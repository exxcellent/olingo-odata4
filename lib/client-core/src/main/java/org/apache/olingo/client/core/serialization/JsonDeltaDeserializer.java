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
package org.apache.olingo.client.core.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.client.api.data.ResWrap;
import org.apache.olingo.client.api.serialization.ODataDeserializerException;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.DeletedEntity;
import org.apache.olingo.commons.api.data.Delta;
import org.apache.olingo.commons.api.data.DeltaLink;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class JsonDeltaDeserializer extends JsonDeserializer {

  public JsonDeltaDeserializer(final boolean serverMode) {
    super(serverMode);
  }

  protected ResWrap<Delta> doDeserialize(final JsonParser parser) throws IOException {

    final ObjectNode tree = (ObjectNode) MAPPER.readTree(parser);

    final Delta delta = new Delta();

    final URI contextURL = tree.hasNonNull(Constants.JSON_CONTEXT) ?
        URI.create(tree.get(Constants.JSON_CONTEXT).stringValue()) : null;
    if (contextURL != null) {
      delta.setBaseURI(URI.create(StringUtils.substringBefore(contextURL.toASCIIString(), Constants.METADATA)));
    }

    if (tree.hasNonNull(Constants.JSON_COUNT)) {
      delta.setCount(tree.get(Constants.JSON_COUNT).asInt());
    }
    if (tree.hasNonNull(Constants.JSON_NEXT_LINK)) {
      delta.setNext(URI.create(tree.get(Constants.JSON_NEXT_LINK).stringValue()));
    }
    if (tree.hasNonNull(Constants.JSON_DELTA_LINK)) {
      delta.setDeltaLink(URI.create(tree.get(Constants.JSON_DELTA_LINK).stringValue()));
    }

    if (tree.hasNonNull(Constants.VALUE)) {
      JsonEntityDeserializer entityDeserializer = new JsonEntityDeserializer(serverMode);
      for (JsonNode jsonNode : tree.get(Constants.VALUE)) {
        final ObjectNode item = (ObjectNode) jsonNode;
        final ContextURL itemContextURL = item.hasNonNull(Constants.JSON_CONTEXT) ?
            ContextURLParser.parse(URI.create(item.get(Constants.JSON_CONTEXT).stringValue())) : null;
        item.remove(Constants.JSON_CONTEXT);

        if (itemContextURL == null || itemContextURL.isEntity()) {
          delta.getEntities().add(
              entityDeserializer.doDeserialize(MAPPER.treeAsTokens(item)).getPayload());
        } else if (itemContextURL.isDeltaDeletedEntity()) {
          delta.getDeletedEntities().add(MAPPER.treeToValue(item, DeletedEntity.class));
        } else if (itemContextURL.isDeltaLink()) {
          delta.getAddedLinks().add(MAPPER.treeToValue(item, DeltaLink.class));
        } else if (itemContextURL.isDeltaDeletedLink()) {
          delta.getDeletedLinks().add(MAPPER.treeToValue(item, DeltaLink.class));
        }
      }
    }

    return new ResWrap<>(contextURL, null, delta);
  }

  public ResWrap<Delta> toDelta(final InputStream input) throws ODataDeserializerException {
    try {
      JsonParser parser = new ObjectMapper().createParser(input);
      return doDeserialize(parser);
    } catch (final IOException e) {
      throw new ODataDeserializerException(e);
    }
  }
}
