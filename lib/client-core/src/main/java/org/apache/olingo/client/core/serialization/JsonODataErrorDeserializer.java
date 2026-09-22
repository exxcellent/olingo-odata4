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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.ex.ODataError;
import org.apache.olingo.commons.api.ex.ODataErrorDetail;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public class JsonODataErrorDeserializer extends JsonDeserializer {

  public JsonODataErrorDeserializer(final boolean serverMode) {
    super(serverMode);
  }

  protected ODataError doDeserialize(final JsonParser parser) throws IOException {

    final ODataError error = new ODataError();

    final ObjectNode tree = (ObjectNode) MAPPER.readTree(parser);
    if (tree.has(Constants.JSON_ERROR)) {
      final JsonNode errorNode = tree.get(Constants.JSON_ERROR);

      if (errorNode.has(Constants.ERROR_CODE)) {
        error.setCode(errorNode.get(Constants.ERROR_CODE).stringValue());
      }
      if (errorNode.has(Constants.ERROR_MESSAGE)) {
        final JsonNode message = errorNode.get(Constants.ERROR_MESSAGE);
        if (message.isValueNode()) {
          error.setMessage(message.stringValue());
        } else if (message.isObject()) {
          error.setMessage(message.get(Constants.VALUE).asString());
        }
      }
      if (errorNode.has(Constants.ERROR_TARGET)) {
        error.setTarget(errorNode.get(Constants.ERROR_TARGET).stringValue());
      }
      if (errorNode.hasNonNull(Constants.ERROR_DETAILS)) {
        List<ODataErrorDetail> details = new ArrayList<>();
        JsonODataErrorDetailDeserializer detailDeserializer = new JsonODataErrorDetailDeserializer(serverMode);
        for (JsonNode jsonNode : errorNode.get(Constants.ERROR_DETAILS)) {
          details.add(detailDeserializer.doDeserialize(MAPPER.treeAsTokens(jsonNode))
              .getPayload());
        }

        error.setDetails(details);
      }
      if (errorNode.hasNonNull(Constants.ERROR_INNERERROR)) {
        HashMap<String, String> innerErrorMap = new HashMap<>();
        final JsonNode innerError = errorNode.get(Constants.ERROR_INNERERROR);
        for (final Map.Entry<String, JsonNode> entry : innerError.properties()) {
          innerErrorMap.put(entry.getKey(), entry.getValue().toString());
        }
        error.setInnerError(innerErrorMap);
      }
    }

    return error;
  }
}
