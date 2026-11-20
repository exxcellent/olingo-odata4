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
package org.apache.olingo.client.core.edm.xml.annotation;

import java.io.IOException;
import java.io.Serializable;

import org.apache.olingo.client.core.edm.xml.AbstractClientCsdlEdmDeserializer;
import org.apache.olingo.client.core.edm.xml.ClientCsdlAnnotation;
import org.apache.olingo.commons.api.edm.geo.SRID;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlIsOf;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = ClientCsdlIsOf.IsOfDeserializer.class)
class ClientCsdlIsOf extends CsdlIsOf implements Serializable {

  private static final long serialVersionUID = -893355856129761174L;

  static class IsOfDeserializer extends AbstractClientCsdlEdmDeserializer<ClientCsdlIsOf> {
    @Override
    protected ClientCsdlIsOf doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
        throws IOException {
      final ClientCsdlIsOf isof = new ClientCsdlIsOf();
      for (; jp.currentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.currentToken();
        if (token == JsonToken.PROPERTY_NAME) {
          if ("Type".equals(jp.currentName())) {
            isof.setType(nextTextValue(jp));
          } else if ("Annotation".equals(jp.currentName())) {
            isof.getAnnotations().add(jp.readValueAs(ClientCsdlAnnotation.class));
          } else if ("MaxLength".equals(jp.currentName())) {
            final String maxLenght = nextTextValue(jp);
            isof.setMaxLength("max".equalsIgnoreCase(maxLenght) ? Integer.MAX_VALUE : Integer.valueOf(maxLenght));
          } else if ("Precision".equals(jp.currentName())) {
            isof.setPrecision(Integer.valueOf(nextTextValue(jp)));
          } else if ("Scale".equals(jp.currentName())) {
            final String scale = nextTextValue(jp);
            isof.setScale("variable".equalsIgnoreCase(scale) || "floating".equalsIgnoreCase(scale) ?
                0 : Integer.valueOf(scale));
          } else if ("SRID".equals(jp.currentName())) {
            final String srid = nextTextValue(jp);
            if (srid != null) {
              isof.setSrid(SRID.valueOf(srid));
            }
          } else {
            isof.setValue(jp.readValueAs(ClientCsdlDynamicExpression.class));
          }
        }
      }
      return isof;
    }
  }
}
