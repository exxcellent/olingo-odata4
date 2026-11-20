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
package org.apache.olingo.client.core.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.olingo.client.api.data.ResWrap;
import org.apache.olingo.client.api.serialization.ODataDeserializer;
import org.apache.olingo.client.api.serialization.ODataDeserializerException;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.Annotatable;
import org.apache.olingo.commons.api.data.Annotation;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Linked;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.PropertyType;
import org.apache.olingo.commons.api.data.Valuable;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.geo.Geospatial;
import org.apache.olingo.commons.api.ex.ODataError;
import org.apache.olingo.commons.core.edm.EdmTypeInfo;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class JsonDeserializer implements ODataDeserializer {

  protected static final ObjectMapper MAPPER = new ObjectMapper();

  protected final Pattern CUSTOM_ANNOTATION = Pattern.compile("(.+)@(.+)\\.(.+)");

  protected final boolean serverMode;

  private JsonGeoValueDeserializer geoDeserializer;

  private JsonParser parser;

  public JsonDeserializer(final boolean serverMode) {
    this.serverMode = serverMode;
  }

  private JsonGeoValueDeserializer getGeoDeserializer() {
    if (geoDeserializer == null) {
      geoDeserializer = new JsonGeoValueDeserializer();
    }
    return geoDeserializer;
  }

  protected String getJSONAnnotation(final String string) {
    return Strings.CS.prependIfMissing(string, "@");
  }

  protected String getTitle(final Map.Entry<String, JsonNode> entry) {
    return entry.getKey().substring(0, entry.getKey().indexOf('@'));
  }

  protected String setInline(final String name, final String suffix, final JsonNode tree,
      final Link link) throws IOException {

    final String entityNamePrefix = name.substring(0, name.indexOf(suffix));

    Integer count = null;
    if (tree.hasNonNull(entityNamePrefix+Constants.JSON_COUNT)) {
      count = tree.get(entityNamePrefix+Constants.JSON_COUNT).asInt();
    }

    if (tree.has(entityNamePrefix)) {
      final JsonNode inline = tree.path(entityNamePrefix);
      JsonEntityDeserializer entityDeserializer = new JsonEntityDeserializer(serverMode);

      if (inline instanceof ObjectNode) {
        link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
        link.setInlineEntity(entityDeserializer.doDeserialize(MAPPER.treeAsTokens(inline)).getPayload());

      } else if (inline instanceof ArrayNode) {
        link.setType(Constants.ENTITY_SET_NAVIGATION_LINK_TYPE);

        final EntityCollection entitySet = new EntityCollection();
        if (count != null) {
          entitySet.setCount(count);
        }
        for (final Iterator<JsonNode> entries = inline.iterator(); entries.hasNext();) {
          entitySet.getEntities().add(
              entityDeserializer.doDeserialize(MAPPER.treeAsTokens(entries.next())).getPayload());
        }

        link.setInlineEntitySet(entitySet);
      }
    }
    return entityNamePrefix;
  }

  protected void links(final Map.Entry<String, JsonNode> field, final Linked linked, final Set<String> toRemove,
      final JsonNode tree) throws IOException {
    if (serverMode) {
      serverLinks(field, linked, toRemove, tree);
    } else {
      clientLinks(field, linked, toRemove, tree);
    }
  }

  private void clientLinks(final Map.Entry<String, JsonNode> field, final Linked linked, final Set<String> toRemove,
      final JsonNode tree) throws IOException {

    if (field.getKey().endsWith(Constants.JSON_NAVIGATION_LINK)) {
      final Link link = new Link();
      link.setTitle(getTitle(field));
      link.setRel(Constants.NS_NAVIGATION_LINK_REL + getTitle(field));

      if (field.getValue().isValueNode()) {
        link.setHref(field.getValue().stringValue());
        link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
      }

      linked.getNavigationLinks().add(link);

      toRemove.add(field.getKey());
      toRemove.add(setInline(field.getKey(), Constants.JSON_NAVIGATION_LINK, tree, link));
    } else if (field.getKey().endsWith(Constants.JSON_ASSOCIATION_LINK)) {
      final Link link = new Link();
      link.setTitle(getTitle(field));
      link.setRel(Constants.NS_ASSOCIATION_LINK_REL + getTitle(field));
      link.setHref(field.getValue().stringValue());
      link.setType(Constants.ASSOCIATION_LINK_TYPE);
      linked.getAssociationLinks().add(link);

      toRemove.add(field.getKey());
    }
  }

  private void serverLinks(final Map.Entry<String, JsonNode> field, final Linked linked, final Set<String> toRemove,
      final JsonNode tree) throws IOException {

    if (field.getKey().endsWith(Constants.JSON_BIND_LINK_SUFFIX)
        || field.getKey().endsWith(Constants.JSON_NAVIGATION_LINK)) {

      if (field.getValue().isValueNode()) {
        final String suffix = field.getKey().replaceAll("^.*@", "@");

        final Link link = new Link();
        link.setTitle(getTitle(field));
        link.setRel(Constants.NS_NAVIGATION_LINK_REL + getTitle(field));
        link.setHref(field.getValue().stringValue());
        link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
        linked.getNavigationLinks().add(link);

        toRemove.add(setInline(field.getKey(), suffix, tree, link));
      } else if (field.getValue().isArray()) {
        for (final Iterator<JsonNode> itor = field.getValue().iterator(); itor.hasNext();) {
          final JsonNode node = itor.next();

          final Link link = new Link();
          link.setTitle(getTitle(field));
          link.setRel(Constants.NS_NAVIGATION_LINK_REL + getTitle(field));
          link.setHref(node.asString());
          link.setType(Constants.ENTITY_SET_NAVIGATION_LINK_TYPE);
          linked.getNavigationLinks().add(link);
          toRemove.add(setInline(field.getKey(), Constants.JSON_BIND_LINK_SUFFIX, tree, link));
        }
      }
      toRemove.add(field.getKey());
    }
  }

  private Map.Entry<PropertyType, EdmTypeInfo> guessPropertyType(final JsonNode node) {
    PropertyType type;
    String typeExpression = null;

    if (node.isValueNode() || node.isNull()) {
      type = PropertyType.PRIMITIVE;
      typeExpression = guessPrimitiveTypeKind(node).getFullQualifiedName().toString();
    } else if (node.isArray()) {
      type = PropertyType.COLLECTION;
      if (node.has(0) && node.get(0).isValueNode()) {
        typeExpression = "Collection(" + guessPrimitiveTypeKind(node.get(0)) + ')';
      }
    } else if (node.isObject()) {
      if (node.has(Constants.ATTR_TYPE)) {
        type = PropertyType.PRIMITIVE;
        typeExpression = "Edm.Geography" + node.get(Constants.ATTR_TYPE).asString();
      } else {
        type = PropertyType.COMPLEX;
      }
    } else {
      type = PropertyType.EMPTY;
    }

    final EdmTypeInfo typeInfo = typeExpression == null ? null :
      new EdmTypeInfo.Builder().setTypeExpression(typeExpression).build();
    return new SimpleEntry<>(type, typeInfo);
  }

  private EdmPrimitiveTypeKind guessPrimitiveTypeKind(final JsonNode node) {
    return node.isShort() ? EdmPrimitiveTypeKind.Int16 :
      node.isInt() ? EdmPrimitiveTypeKind.Int32 :
        node.isLong() ? EdmPrimitiveTypeKind.Int64 :
          node.isBoolean() ? EdmPrimitiveTypeKind.Boolean :
            node.isFloat() ? EdmPrimitiveTypeKind.Single :
              node.isDouble() ? EdmPrimitiveTypeKind.Double :
                node.isBigDecimal() ? EdmPrimitiveTypeKind.Decimal :
                  EdmPrimitiveTypeKind.String;
  }

  protected void populate(final Annotatable annotatable, final List<Property> properties,
      final ObjectNode tree)
          throws IOException, EdmPrimitiveTypeException {

    String type = null;
    Annotation annotation = null;
    for (final Map.Entry<String, JsonNode> field : tree.properties()) {
      final Matcher customAnnotation = CUSTOM_ANNOTATION.matcher(field.getKey());

      if (field.getKey().charAt(0) == '@') {
        final Annotation entityAnnot = new Annotation();
        entityAnnot.setTerm(field.getKey().substring(1));

        value(entityAnnot, field.getValue());
        if (annotatable != null) {
          annotatable.getAnnotations().add(entityAnnot);
        }
      } else if (type == null && field.getKey().endsWith(getJSONAnnotation(Constants.JSON_TYPE))) {
        type = field.getValue().asString();
      } else if (field.getKey().endsWith(getJSONAnnotation(Constants.JSON_COUNT))) {
        final Property property = new Property();
        property.setName(field.getKey());
        property.setValue(ValueType.PRIMITIVE, Integer.parseInt(field.getValue().asString()));
        properties.add(property);
      } else if (annotation == null && customAnnotation.matches() && !"odata".equals(customAnnotation.group(2))) {
        annotation = new Annotation();
        annotation.setTerm(customAnnotation.group(2) + "." + customAnnotation.group(3));
        value(annotation, field.getValue());
      } else {
        final Property property = new Property();
        property.setName(field.getKey());
        property.setType(type == null
            ? null
                : new EdmTypeInfo.Builder().setTypeExpression(type).build().internal());
        type = null;

        value(property, field.getValue());
        properties.add(property);

        if (annotation != null) {
          property.getAnnotations().add(annotation);
          annotation = null;
        }
      }
    }
  }

  private Object fromPrimitive(final JsonNode node, final EdmTypeInfo typeInfo) throws EdmPrimitiveTypeException {
    return node.isNull() ? null
        : typeInfo == null ? node.asString()
            : typeInfo.getPrimitiveTypeKind().isGeospatial()
            ? getGeoDeserializer().deserialize(node, typeInfo)
                : node.isBigDecimal()
                ?((EdmPrimitiveType) typeInfo.getType())
                    .valueOfString(node.asString(), true, null,
                           node.decimalValue().precision(), node.decimalValue().scale(), true,
                           ((EdmPrimitiveType) typeInfo.getType()).getDefaultType())
                 :((EdmPrimitiveType) typeInfo.getType())
                    .valueOfString(node.asString(), true, null,
                           Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, true,
                           ((EdmPrimitiveType) typeInfo.getType()).getDefaultType());
  }

  private Object fromComplex(final ObjectNode node)
      throws IOException, EdmPrimitiveTypeException {

    final ComplexValue complexValue = new ComplexValue();
    final Set<String> toRemove = new HashSet<>();
    for (final Map.Entry<String, JsonNode> field : node.properties()) {
      links(field, complexValue, toRemove, node);
    }
    node.remove(toRemove);

    populate(complexValue, complexValue.getValue(), node);
    return complexValue;
  }

  private void fromCollection(final Valuable valuable, final Iterator<JsonNode> nodeItor, final EdmTypeInfo typeInfo)
      throws IOException, EdmPrimitiveTypeException {

    final List<Object> values = new ArrayList<>();
    ValueType valueType = ValueType.COLLECTION_PRIMITIVE;

    final EdmTypeInfo type = typeInfo == null ? null
        : new EdmTypeInfo.Builder().setTypeExpression(typeInfo.getFullQualifiedName().toString()).build();

    while (nodeItor.hasNext()) {
      final JsonNode child = nodeItor.next();

      if (child.isValueNode()) {
        if (typeInfo == null || typeInfo.isPrimitiveType()) {
          final Object value = fromPrimitive(child, type);
          valueType = value instanceof Geospatial ? ValueType.COLLECTION_GEOSPATIAL : ValueType.COLLECTION_PRIMITIVE;
          values.add(value);
        } else {
          valueType = ValueType.COLLECTION_ENUM;
          values.add(child.asString());
        }
      } else if (child.isObject() || child.isArray()) {
        EdmTypeInfo childType = null;
        if (child.has(Constants.JSON_TYPE)) {
          String typeName = child.get(Constants.JSON_TYPE).asString();
          childType = typeName == null ? null
              : new EdmTypeInfo.Builder().setTypeExpression(typeName).build();
          ((ObjectNode) child).remove(Constants.JSON_TYPE);
        }
        final Object value = fromComplex((ObjectNode) child);
        if (childType != null) {
          ((ComplexValue)value).setTypeName(childType.external());
        }
        valueType = ValueType.COLLECTION_COMPLEX;
        values.add(value);
      }
    }
    valuable.setValue(valueType, values);
  }

  protected void value(final Valuable valuable, final JsonNode node)
      throws IOException, EdmPrimitiveTypeException {

    EdmTypeInfo typeInfo = StringUtils.isBlank(valuable.getType()) ? null
        : new EdmTypeInfo.Builder().setTypeExpression(valuable.getType()).build();

    final Map.Entry<PropertyType, EdmTypeInfo> guessed = guessPropertyType(node);
    if (typeInfo == null) {
      typeInfo = guessed.getValue();
    }

    final PropertyType propType = typeInfo == null ? guessed.getKey()
        : typeInfo.isCollection() ? PropertyType.COLLECTION
            : typeInfo.isPrimitiveType() ? PropertyType.PRIMITIVE
                : node.isValueNode() ? PropertyType.ENUM : PropertyType.COMPLEX;

    switch (propType) {
    case COLLECTION:
      fromCollection(valuable, node.iterator(), typeInfo);
      break;

    case COMPLEX:
      if (node.has(Constants.JSON_TYPE)) {
        valuable.setType(node.get(Constants.JSON_TYPE).asString());
        ((ObjectNode) node).remove(Constants.JSON_TYPE);
      }
      final Object value = fromComplex((ObjectNode) node);
      if (value instanceof ComplexValue) {
        ((ComplexValue)value).setTypeName(valuable.getType());
      }
      valuable.setValue(ValueType.COMPLEX, value);
      break;

    case ENUM:
      if(!node.isNull()) {
    	  valuable.setValue(ValueType.ENUM, node.asString());
      }
      break;

    case PRIMITIVE:
      if (valuable.getType() == null && typeInfo != null) {
        valuable.setType(typeInfo.getFullQualifiedName().toString());
      }
      final Object primitiveValue = fromPrimitive(node, typeInfo);
      valuable.setValue(primitiveValue instanceof Geospatial ? ValueType.GEOSPATIAL : ValueType.PRIMITIVE,
          primitiveValue);
      break;

    case EMPTY:
    default:
      valuable.setValue(ValueType.PRIMITIVE, StringUtils.EMPTY);
    }
  }

  @Override
  public ResWrap<EntityCollection> toEntitySet(final InputStream input) throws ODataDeserializerException {
    try {
      parser = new ObjectMapper().createParser(input);
      return new JsonEntitySetDeserializer(serverMode).doDeserialize(parser);
    } catch (final IOException e) {
      throw new ODataDeserializerException(e);
    }
  }

  @Override
  public ResWrap<Entity> toEntity(final InputStream input) throws ODataDeserializerException {
    try {
      parser = new ObjectMapper().createParser(input);
      return new JsonEntityDeserializer(serverMode).doDeserialize(parser);
    } catch (final IOException e) {
      throw new ODataDeserializerException(e);
    }
  }

  @Override
  public ResWrap<Property> toProperty(final InputStream input) throws ODataDeserializerException {
    try {
      parser = new ObjectMapper().createParser(input);
      return new JsonPropertyDeserializer(serverMode).doDeserialize(parser);
    } catch (final IOException e) {
      throw new ODataDeserializerException(e);
    }
  }

  @Override
  public ODataError toError(final InputStream input) throws ODataDeserializerException {
    try {
      parser = new ObjectMapper().createParser(input);
      return new JsonODataErrorDeserializer(serverMode).doDeserialize(parser);
    } catch (final IOException e) {
      throw new ODataDeserializerException(e);
    }
  }
}
