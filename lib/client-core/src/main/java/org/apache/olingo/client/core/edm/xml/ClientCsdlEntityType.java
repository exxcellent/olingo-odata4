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

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;

import tools.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.io.Serializable;

@JsonDeserialize(using = ClientCsdlEntityType.EntityTypeDeserializer.class)
class ClientCsdlEntityType extends CsdlEntityType implements Serializable {

  private static final long serialVersionUID = -3986417775876689669L;

  static class EntityTypeDeserializer extends AbstractClientCsdlEdmDeserializer<CsdlEntityType> {
    @Override
    protected CsdlEntityType doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {

      final ClientCsdlEntityType entityType = new ClientCsdlEntityType();

      for (; jp.currentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.currentToken();
        if (token == JsonToken.PROPERTY_NAME) {
          if ("Name".equals(jp.currentName())) {
            entityType.setName(nextTextValue(jp));
          } else if ("Abstract".equals(jp.currentName())) {
            entityType.setAbstract(BooleanUtils.toBoolean(nextTextValue(jp)));
          } else if ("BaseType".equals(jp.currentName())) {
            entityType.setBaseType(nextTextValue(jp));
          } else if ("OpenType".equals(jp.currentName())) {
            entityType.setOpenType(BooleanUtils.toBoolean(nextTextValue(jp)));
          } else if ("HasStream".equals(jp.currentName())) {
            entityType.setHasStream(BooleanUtils.toBoolean(nextTextValue(jp)));
          } else if ("Key".equals(jp.currentName())) {
            jp.nextToken();
            ClientCsdlEntityKey keyImpl = jp.readValueAs(ClientCsdlEntityKey.class);
            entityType.setKey(keyImpl.getPropertyRefs());
          } else if ("Property".equals(jp.currentName())) {
            jp.nextToken();
            entityType.getProperties().add(jp.readValueAs(ClientCsdlProperty.class));
          } else if ("NavigationProperty".equals(jp.currentName())) {
            jp.nextToken();
            entityType.getNavigationProperties().add(jp.readValueAs(ClientCsdlNavigationProperty.class));
          } else if ("Annotation".equals(jp.currentName())) {
            jp.nextToken();
            entityType.getAnnotations().add(jp.readValueAs(ClientCsdlAnnotation.class));
          }
        }
      }

      return entityType;
    }
  }
}
