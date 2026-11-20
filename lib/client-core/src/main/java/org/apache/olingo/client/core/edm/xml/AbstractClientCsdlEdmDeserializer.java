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

import java.io.IOException;

import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.dataformat.xml.deser.FromXmlParser;

public abstract class AbstractClientCsdlEdmDeserializer<T> extends ValueDeserializer<T> {

  protected boolean isAnnotationConstExprConstruct(final JsonParser jp) throws IOException {
    return CsdlConstantExpression.ConstantExpressionType.fromString(jp.currentName()) != null;
  }

  protected CsdlConstantExpression parseAnnotationConstExprConstruct(final JsonParser jp)
      throws IOException {
    final CsdlConstantExpression constExpr =
        new CsdlConstantExpression(CsdlConstantExpression.ConstantExpressionType.fromString(jp.currentName()));
    jp.nextToken();
    constExpr.setValue(jp.getString());
    return constExpr;
  }

  protected ClientCsdlReturnType parseReturnType(final JsonParser jp, final String elementName) throws IOException {
    final ClientCsdlReturnType returnType;
    if (elementName.equals(((FromXmlParser) jp).getStaxReader().getLocalName())) {
      returnType = new ClientCsdlReturnType();
      jp.nextToken();
      returnType.setType(jp.getString());
    } else {
      jp.nextToken();
      returnType = jp.readValueAs(ClientCsdlReturnType.class);
    }
    return returnType;
  }

  /**
   * Jackson 3.x removed nextTextValue(). This helper advances the parser and returns the string value.
   */
  protected String nextTextValue(final JsonParser jp) throws IOException {
    jp.nextToken();
    return jp.getString();
  }

  protected abstract T doDeserialize(JsonParser jp, DeserializationContext ctxt) throws IOException;

  @Override
  public T deserialize(final JsonParser jp, final DeserializationContext ctxt) throws JacksonException {
    try {
      return doDeserialize(jp, ctxt);
    } catch (IOException e) {
      throw new tools.jackson.core.exc.StreamReadException(jp, e.getMessage(), e);
    }
  }
}
