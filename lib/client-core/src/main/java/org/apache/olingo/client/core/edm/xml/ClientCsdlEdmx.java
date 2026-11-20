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

import org.apache.olingo.client.api.edm.xml.DataServices;
import org.apache.olingo.client.api.edm.xml.Edmx;
import org.apache.olingo.client.api.edm.xml.Reference;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmItem;

import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = ClientCsdlEdmx.EdmxDeserializer.class)
public class ClientCsdlEdmx extends CsdlAbstractEdmItem implements Serializable, Edmx {

  private static final long serialVersionUID = -6293476719276092572L;

  private final List<Reference> references = new ArrayList<>();

  private String version;

  private DataServices dataServices;

  @Override
  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  @Override
  public DataServices getDataServices() {
    return dataServices;
  }

  public void setDataServices(final DataServices dataServices) {
    this.dataServices = dataServices;
  }
  
  @Override
  public List<Reference> getReferences() {
    return references;
  }

  static class EdmxDeserializer extends AbstractClientCsdlEdmDeserializer<ClientCsdlEdmx> {

    @Override
    protected ClientCsdlEdmx doDeserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {

      final ClientCsdlEdmx edmx = new ClientCsdlEdmx();

      for (; (jp.currentToken() != null && jp.currentToken() != JsonToken.END_OBJECT); jp.nextToken()) {
        final JsonToken token = jp.currentToken();
        if (token == JsonToken.PROPERTY_NAME) {
          if ("Version".equals(jp.currentName())) {
            edmx.setVersion(nextTextValue(jp));
          } else if ("DataServices".equals(jp.currentName())) {
            jp.nextToken();
            edmx.setDataServices(jp.readValueAs(ClientCsdlDataServices.class));
          } else if ("Reference".equals(jp.currentName())) {
            jp.nextToken();
            edmx.getReferences().add(jp.readValueAs(ClientCsdlReference.class));
          }
        }
      }

      return edmx;
    }
  }
}
