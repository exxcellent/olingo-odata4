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

import org.apache.olingo.commons.api.edm.provider.CsdlSchema;

import tools.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.io.Serializable;

@JsonDeserialize(using = ClientCsdlSchema.SchemaDeserializer.class)
class ClientCsdlSchema extends CsdlSchema implements Serializable {

  private static final long serialVersionUID = 1911087363912024939L;

  static class SchemaDeserializer extends AbstractClientCsdlEdmDeserializer<ClientCsdlSchema> {
    @Override
    protected ClientCsdlSchema doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {
      final ClientCsdlSchema schema = new ClientCsdlSchema();

      for (; jp.currentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.currentToken();
        if (token == JsonToken.PROPERTY_NAME) {
          if ("Namespace".equals(jp.currentName())) {
            schema.setNamespace(nextTextValue(jp));
          } else if ("Alias".equals(jp.currentName())) {
            schema.setAlias(nextTextValue(jp));
          } else if ("ComplexType".equals(jp.currentName())) {
            jp.nextToken();
            schema.getComplexTypes().add(jp.readValueAs(ClientCsdlComplexType.class));
          } else if ("EntityType".equals(jp.currentName())) {
            jp.nextToken();
            schema.getEntityTypes().add(jp.readValueAs(ClientCsdlEntityType.class));
          } else if ("EnumType".equals(jp.currentName())) {
            jp.nextToken();
            schema.getEnumTypes().add(jp.readValueAs(ClientCsdlEnumType.class));
          } else if ("EntityContainer".equals(jp.currentName())) {
            jp.nextToken();
            ClientCsdlEntityContainer entityContainer = jp.readValueAs(ClientCsdlEntityContainer.class);
            schema.setEntityContainer(entityContainer);
          } else if ("Action".equals(jp.currentName())) {
            jp.nextToken();
            schema.getActions().add(jp.readValueAs(ClientCsdlAction.class));
          } else if ("Function".equals(jp.currentName())) {
            jp.nextToken();
            schema.getFunctions().add(jp.readValueAs(ClientCsdlFunction.class));
          } else if ("TypeDefinition".equals(jp.currentName())) {
            jp.nextToken();
            schema.getTypeDefinitions().add(jp.readValueAs(ClientCsdlTypeDefinition.class));
          }
        } else if ("Annotations".equals(jp.currentName())) {
          jp.nextToken();
          schema.getAnnotationGroups().add(jp.readValueAs(ClientCsdlAnnotations.class));
        } else if ("Annotation".equals(jp.currentName())) {
          jp.nextToken();
          schema.getAnnotations().add(jp.readValueAs(ClientCsdlAnnotation.class));
        } else if ("Term".equals(jp.currentName())) {
          jp.nextToken();
          schema.getTerms().add(jp.readValueAs(ClientCsdlTerm.class));
        }
      }

      return schema;
    }
  }
}
