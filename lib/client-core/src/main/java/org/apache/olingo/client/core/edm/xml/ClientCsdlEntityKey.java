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
package org.apache.olingo.client.core.edm.xml;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;

import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmItem;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;

import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = ClientCsdlEntityKey.EntityKeyDeserializer.class)
class ClientCsdlEntityKey extends CsdlAbstractEdmItem implements Serializable {

  private static final long serialVersionUID = 520227585458843347L;

  private final List<CsdlPropertyRef> propertyRefs = new ArrayList<>();

  public List<CsdlPropertyRef> getPropertyRefs() {
    return propertyRefs;
  }

  static class EntityKeyDeserializer extends AbstractClientCsdlEdmDeserializer<ClientCsdlEntityKey> {
    @Override
    protected ClientCsdlEntityKey doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {

      final ClientCsdlEntityKey entityKey = new ClientCsdlEntityKey();

      for (; jp.currentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.currentToken();

        if (token == JsonToken.PROPERTY_NAME && "PropertyRef".equals(jp.currentName())) {
          jp.nextToken();
          entityKey.getPropertyRefs().add(jp.readValueAs(ClientCsdlPropertyRef.class));
        }
      }

      return entityKey;
    }
  }
}
