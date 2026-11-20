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
import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.commons.api.edm.geo.SRID;
import org.apache.olingo.commons.api.edm.provider.CsdlTerm;

import tools.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

@JsonDeserialize(using = ClientCsdlTerm.TermDeserializer.class)
class ClientCsdlTerm extends CsdlTerm implements Serializable {

  private static final long serialVersionUID = -8350072064720586186L;

  static class TermDeserializer extends AbstractClientCsdlEdmDeserializer<ClientCsdlTerm> {
    @Override
    protected ClientCsdlTerm doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {
      final ClientCsdlTerm term = new ClientCsdlTerm();

      for (; jp.currentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.currentToken();
        if (token == JsonToken.PROPERTY_NAME) {
          if ("Name".equals(jp.currentName())) {
            term.setName(nextTextValue(jp));
          } else if ("Type".equals(jp.currentName())) {
            term.setType(nextTextValue(jp));
          } else if ("BaseTerm".equals(jp.currentName())) {
            term.setBaseTerm(nextTextValue(jp));
          } else if ("DefaultValue".equals(jp.currentName())) {
            term.setDefaultValue(nextTextValue(jp));
          } else if ("Nullable".equals(jp.currentName())) {
            term.setNullable(BooleanUtils.toBoolean(nextTextValue(jp)));
          } else if ("MaxLength".equals(jp.currentName())) {
            final String maxLenght = nextTextValue(jp);
            term.setMaxLength("max".equalsIgnoreCase(maxLenght) ? Integer.MAX_VALUE : Integer.valueOf(maxLenght));
          } else if ("Precision".equals(jp.currentName())) {
            term.setPrecision(Integer.valueOf(nextTextValue(jp)));
          } else if ("Scale".equals(jp.currentName())) {
            final String scale = nextTextValue(jp);
            term.setScale("variable".equalsIgnoreCase(scale) || "floating".equalsIgnoreCase(scale) ?
                0 : Integer.valueOf(scale));
          } else if ("SRID".equals(jp.currentName())) {
            final String srid = nextTextValue(jp);
            if (srid != null) {
              term.setSrid(SRID.valueOf(srid));
            }
          } else if ("AppliesTo".equals(jp.currentName())) {
            term.getAppliesTo().addAll(Arrays.asList(StringUtils.split(nextTextValue(jp))));
          } else if ("Annotation".equals(jp.currentName())) {
            jp.nextToken();
            term.getAnnotations().add(jp.readValueAs(ClientCsdlAnnotation.class));
          }
        }
      }

      return term;
    }
  }
}
