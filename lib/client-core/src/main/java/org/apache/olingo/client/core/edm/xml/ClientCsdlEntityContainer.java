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

import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;

import tools.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.io.Serializable;

@JsonDeserialize(using = ClientCsdlEntityContainer.EntityContainerDeserializer.class)
class ClientCsdlEntityContainer extends CsdlEntityContainer implements Serializable {

  private static final long serialVersionUID = 5631432527646955795L;

  static class EntityContainerDeserializer extends AbstractClientCsdlEdmDeserializer<ClientCsdlEntityContainer> {

    @Override
    protected ClientCsdlEntityContainer doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {

      final ClientCsdlEntityContainer entityContainer = new ClientCsdlEntityContainer();

      for (; jp.currentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
        final JsonToken token = jp.currentToken();
        if (token == JsonToken.PROPERTY_NAME) {
          if ("Name".equals(jp.currentName())) {
            entityContainer.setName(nextTextValue(jp));
          } else if ("Extends".equals(jp.currentName())) {
            entityContainer.setExtendsContainer(nextTextValue(jp));
          } else if ("EntitySet".equals(jp.currentName())) {
            jp.nextToken();
            entityContainer.getEntitySets().add(jp.readValueAs(ClientCsdlEntitySet.class));
          } else if ("Singleton".equals(jp.currentName())) {
            jp.nextToken();
            entityContainer.getSingletons().add(jp.readValueAs(ClientCsdlSingleton.class));
          } else if ("ActionImport".equals(jp.currentName())) {
            jp.nextToken();
            entityContainer.getActionImports().add(jp.readValueAs(ClientCsdlActionImport.class));
          } else if ("FunctionImport".equals(jp.currentName())) {
            jp.nextToken();
            entityContainer.getFunctionImports().add(jp.readValueAs(ClientCsdlFunctionImport.class));
          } else if ("Annotation".equals(jp.currentName())) {
            jp.nextToken();
            entityContainer.getAnnotations().add(jp.readValueAs(ClientCsdlAnnotation.class));
          }
        }
      }

      return entityContainer;
    }
  }
}
