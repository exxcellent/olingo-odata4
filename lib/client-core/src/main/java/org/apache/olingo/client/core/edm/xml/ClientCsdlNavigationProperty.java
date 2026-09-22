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
package org.apache.olingo.client.core.edm.xml;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;

import tools.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.io.Serializable;

@JsonDeserialize(using = ClientCsdlNavigationProperty.NavigationPropertyDeserializer.class)
class ClientCsdlNavigationProperty extends CsdlNavigationProperty implements Serializable {

  private static final long serialVersionUID = 6240231735592427582L;

  static class NavigationPropertyDeserializer extends AbstractClientCsdlEdmDeserializer<CsdlNavigationProperty> {

    @Override
    protected CsdlNavigationProperty doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {

      final CsdlNavigationProperty property = new ClientCsdlNavigationProperty();

      for (; jp.currentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.currentToken();
        if (token == JsonToken.PROPERTY_NAME) {
          if ("Name".equals(jp.currentName())) {
            property.setName(nextTextValue(jp));
          } else if ("Type".equals(jp.currentName())) {
            String metadataTypeName = nextTextValue(jp);
            if (metadataTypeName.startsWith("Collection(")) {
              property.setType(metadataTypeName.substring(metadataTypeName.indexOf("(") + 1,
                      metadataTypeName.length() - 1));
              property.setCollection(true);
            } else {
              property.setType(metadataTypeName);
              property.setCollection(false);
            }
          } else if ("Nullable".equals(jp.currentName())) {
            property.setNullable(BooleanUtils.toBoolean(nextTextValue(jp)));
          } else if ("Partner".equals(jp.currentName())) {
            property.setPartner(nextTextValue(jp));
          } else if ("ContainsTarget".equals(jp.currentName())) {
            property.setContainsTarget(BooleanUtils.toBoolean(nextTextValue(jp)));
          } else if ("ReferentialConstraint".equals(jp.currentName())) {
            jp.nextToken();
            property.getReferentialConstraints().add(jp.readValueAs(ClientCsdlReferentialConstraint.class));
          } else if ("OnDelete".equals(jp.currentName())) {
            jp.nextToken();
            property.setOnDelete(jp.readValueAs(ClientCsdlOnDelete.class));
          } else if ("Annotation".equals(jp.currentName())) {
            jp.nextToken();
            property.getAnnotations().add(jp.readValueAs(ClientCsdlAnnotation.class));
          }
        }
      }
      return property;
    }
  }
}