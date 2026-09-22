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
import org.apache.olingo.commons.api.edm.geo.SRID;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;

import tools.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.io.Serializable;

@JsonDeserialize(using = ClientCsdlReturnType.ReturnTypeDeserializer.class)
class ClientCsdlReturnType extends CsdlReturnType implements Serializable {

  private static final long serialVersionUID = 6261092793901735110L;

  static class ReturnTypeDeserializer extends AbstractClientCsdlEdmDeserializer<ClientCsdlReturnType> {
    @Override
    protected ClientCsdlReturnType doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {
      final ClientCsdlReturnType returnType = new ClientCsdlReturnType();

      for (; jp.currentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.currentToken();
        if (token == JsonToken.PROPERTY_NAME) {
          if ("Type".equals(jp.currentName())) {
            jp.nextToken();
            String metadataTypeName = jp.getString();
            if (metadataTypeName.startsWith("Collection(")) {
              returnType.setType(metadataTypeName.substring(metadataTypeName.indexOf("(") + 1,
                      metadataTypeName.length() - 1));
              returnType.setCollection(true);
            } else {
              returnType.setType(metadataTypeName);
              returnType.setCollection(false);
            }
          } else if ("Nullable".equals(jp.currentName())) {
            jp.nextToken();
            returnType.setNullable(BooleanUtils.toBoolean(jp.getString()));
          } else if ("MaxLength".equals(jp.currentName())) {
            jp.nextToken();
            final String maxLenght = jp.getString();
            returnType.setMaxLength("max".equalsIgnoreCase(maxLenght) ? Integer.MAX_VALUE : Integer.valueOf(maxLenght));
          } else if ("Precision".equals(jp.currentName())) {
            jp.nextToken();
            returnType.setPrecision(Integer.valueOf(jp.getString()));
          } else if ("Scale".equals(jp.currentName())) {
            jp.nextToken();
            final String scale = jp.getString();
            returnType.setScale("variable".equalsIgnoreCase(scale) || "floating".equalsIgnoreCase(scale) ?
                0 : Integer.valueOf(scale));
          } else if ("SRID".equals(jp.currentName())) {
            jp.nextToken();
            final String srid = jp.getString();
            if (srid != null) {
              returnType.setSrid(SRID.valueOf(srid));
            }
          } else if ("Annotation".equals(jp.currentName())) {
            jp.nextToken();
            returnType.getAnnotations().add(jp.readValueAs(ClientCsdlAnnotation.class));
          }
        }
      }

      return returnType;
    }
  }
}
