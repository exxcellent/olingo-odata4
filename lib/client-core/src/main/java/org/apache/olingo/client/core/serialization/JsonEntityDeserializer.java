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
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.client.api.data.ResWrap;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.Annotation;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Operation;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.core.edm.EdmTypeInfo;

import tools.jackson.core.JsonParser;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Reads JSON string into an entity.
 * <br/>
 * If metadata information is available, the corresponding entity fields and content will be populated.
 */
public class JsonEntityDeserializer extends JsonDeserializer {

  public JsonEntityDeserializer(final boolean serverMode) {
    super(serverMode);
  }

  protected ResWrap<Entity> doDeserialize(final JsonParser parser) throws IOException {

    final ObjectNode tree = (ObjectNode) MAPPER.readTree(parser);

    if (tree.has(Constants.VALUE) && tree.get(Constants.VALUE).isArray()) {
      throw new StreamReadException(parser, "Expected OData Entity, found EntitySet");
    }

    final Entity entity = new Entity();

    final URI contextURL;
    if (tree.hasNonNull(Constants.JSON_CONTEXT)) {
      contextURL = URI.create(tree.get(Constants.JSON_CONTEXT).stringValue());
      tree.remove(Constants.JSON_CONTEXT);
    } else if (tree.hasNonNull(Constants.JSON_METADATA)) {
      contextURL = URI.create(tree.get(Constants.JSON_METADATA).stringValue());
      tree.remove(Constants.JSON_METADATA);
    } else {
      contextURL = null;
    }
    if (contextURL != null) {
      entity.setBaseURI(URI.create(StringUtils.substringBefore(contextURL.toASCIIString(), Constants.METADATA)));
    }

    final String metadataETag;
    if (tree.hasNonNull(Constants.JSON_METADATA_ETAG)) {
      metadataETag = tree.get(Constants.JSON_METADATA_ETAG).stringValue();
      tree.remove(Constants.JSON_METADATA_ETAG);
    } else {
      metadataETag = null;
    }

    if (tree.hasNonNull(Constants.JSON_ETAG)) {
      entity.setETag(tree.get(Constants.JSON_ETAG).stringValue());
      tree.remove(Constants.JSON_ETAG);
    }

    if (tree.hasNonNull(Constants.JSON_TYPE)) {
      entity.setType(new EdmTypeInfo.Builder().setTypeExpression(tree.get(Constants.JSON_TYPE).stringValue()).build()
          .internal());
      tree.remove(Constants.JSON_TYPE);
    }

    if (tree.hasNonNull(Constants.JSON_ID)) {
      entity.setId(URI.create(tree.get(Constants.JSON_ID).stringValue()));
      tree.remove(Constants.JSON_ID);
    }

    if (tree.hasNonNull(Constants.JSON_READ_LINK)) {
      final Link link = new Link();
      link.setRel(Constants.SELF_LINK_REL);
      link.setHref(tree.get(Constants.JSON_READ_LINK).stringValue());
      entity.setSelfLink(link);

      tree.remove(Constants.JSON_READ_LINK);
    }

    if (tree.hasNonNull(Constants.JSON_EDIT_LINK)) {
      final Link link = new Link();
      if (serverMode) {
        link.setRel(Constants.EDIT_LINK_REL);
      }
      link.setHref(tree.get(Constants.JSON_EDIT_LINK).stringValue());
      entity.setEditLink(link);

      tree.remove(Constants.JSON_EDIT_LINK);
    }

    if (tree.hasNonNull(Constants.JSON_MEDIA_READ_LINK)) {
      entity.setMediaContentSource(URI.create(tree.get(Constants.JSON_MEDIA_READ_LINK).stringValue()));
      tree.remove(Constants.JSON_MEDIA_READ_LINK);
    }
    if (tree.hasNonNull(Constants.JSON_MEDIA_EDIT_LINK)) {
      entity.setMediaContentSource(URI.create(tree.get(Constants.JSON_MEDIA_EDIT_LINK).stringValue()));
      tree.remove(Constants.JSON_MEDIA_EDIT_LINK);
    }
    if (tree.hasNonNull(Constants.JSON_MEDIA_CONTENT_TYPE)) {
      entity.setMediaContentType(tree.get(Constants.JSON_MEDIA_CONTENT_TYPE).stringValue());
      tree.remove(Constants.JSON_MEDIA_CONTENT_TYPE);
    }
    if (tree.hasNonNull(Constants.JSON_MEDIA_ETAG)) {
      entity.setMediaETag(tree.get(Constants.JSON_MEDIA_ETAG).stringValue());
      tree.remove(Constants.JSON_MEDIA_ETAG);
    }

    final Set<String> toRemove = new HashSet<>();

    final Map<String, List<Annotation>> annotations = new HashMap<>();
    for (final Map.Entry<String, JsonNode> field : tree.properties()) {
      final Matcher customAnnotation = CUSTOM_ANNOTATION.matcher(field.getKey());

      links(field, entity, toRemove, tree);
      if (field.getKey().endsWith(getJSONAnnotation(Constants.JSON_MEDIA_READ_LINK))) {
        final Link link = new Link();
        link.setTitle(getTitle(field));
        link.setRel(Constants.NS_MEDIA_READ_LINK_REL + getTitle(field));
        link.setType(Constants.MEDIA_EDIT_LINK_TYPE);
        link.setHref(field.getValue().stringValue());
        entity.getMediaEditLinks().add(link);

        if (tree.has(link.getTitle() + getJSONAnnotation(Constants.JSON_MEDIA_ETAG))) {
          link.setMediaETag(tree.get(link.getTitle() + getJSONAnnotation(Constants.JSON_MEDIA_ETAG)).asString());
          toRemove.add(link.getTitle() + getJSONAnnotation(Constants.JSON_MEDIA_ETAG));
        }

        if (tree.has(link.getTitle() + getJSONAnnotation(Constants.JSON_MEDIA_CONTENT_TYPE))) {
          link.setType(tree.get(link.getTitle() + getJSONAnnotation(Constants.JSON_MEDIA_CONTENT_TYPE)).asString());
          toRemove.add(link.getTitle() + getJSONAnnotation(Constants.JSON_MEDIA_CONTENT_TYPE));
        }

        toRemove.add(field.getKey());
        toRemove.add(setInline(field.getKey(), getJSONAnnotation(Constants.JSON_MEDIA_READ_LINK), tree, link));
      } else if (field.getKey().endsWith(getJSONAnnotation(Constants.JSON_MEDIA_EDIT_LINK))) {
        final Link link = getOrCreateMediaLink(entity, getTitle(field));
        link.setRel(Constants.NS_MEDIA_EDIT_LINK_REL + getTitle(field));
        link.setHref(field.getValue().stringValue());
        toRemove.add(field.getKey());
        toRemove.add(setInline(field.getKey(), getJSONAnnotation(Constants.JSON_MEDIA_EDIT_LINK), tree, link));
      } else if (field.getKey().endsWith(getJSONAnnotation(Constants.JSON_MEDIA_CONTENT_TYPE))) {
        final Link link = getOrCreateMediaLink(entity, getTitle(field));
        link.setType(field.getValue().asString());
        toRemove.add(field.getKey());
      } else if (field.getKey().endsWith(getJSONAnnotation(Constants.JSON_MEDIA_ETAG))) {
        final Link link = getOrCreateMediaLink(entity, getTitle(field));
        link.setMediaETag(field.getValue().asString());
        toRemove.add(field.getKey());
      } else if (field.getKey().charAt(0) == '#') {
        final Operation operation = new Operation();
        operation.setMetadataAnchor(field.getKey());

        final ObjectNode opNode = (ObjectNode) tree.get(field.getKey());
        operation.setTitle(opNode.get(Constants.ATTR_TITLE).asString());
        operation.setTarget(URI.create(opNode.get(Constants.ATTR_TARGET).asString()));

        entity.getOperations().add(operation);

        toRemove.add(field.getKey());
      } else if (customAnnotation.matches() && !"odata".equals(customAnnotation.group(2))) {
        final Annotation annotation = new Annotation();
        annotation.setTerm(customAnnotation.group(2) + "." + customAnnotation.group(3));
        try {
          value(annotation, field.getValue());
        } catch (final EdmPrimitiveTypeException e) {
          throw new IOException(e);
        }

        if (!annotations.containsKey(customAnnotation.group(1))) {
          annotations.put(customAnnotation.group(1), new ArrayList<Annotation>());
        }
        annotations.get(customAnnotation.group(1)).add(annotation);
      }
    }

    for (Link link : entity.getNavigationLinks()) {
      if (annotations.containsKey(link.getTitle())) {
        link.getAnnotations().addAll(annotations.get(link.getTitle()));
        for (Annotation annotation : annotations.get(link.getTitle())) {
          toRemove.add(link.getTitle() + "@" + annotation.getTerm());
        }
      }
    }
    for (Link link : entity.getMediaEditLinks()) {
      if (annotations.containsKey(link.getTitle())) {
        link.getAnnotations().addAll(annotations.get(link.getTitle()));
        for (Annotation annotation : annotations.get(link.getTitle())) {
          toRemove.add(link.getTitle() + "@" + annotation.getTerm());
        }
      }
      if (tree.get(link.getTitle() + Constants.JSON_TYPE) != null) {
        toRemove.add(link.getTitle() + Constants.JSON_TYPE);
      }
    }

    tree.remove(toRemove);

    try {
      populate(entity, entity.getProperties(), tree);
    } catch (final EdmPrimitiveTypeException e) {
      throw new IOException(e);
    }

    return new ResWrap<>(contextURL, metadataETag, entity);
  }
  
  private Link getOrCreateMediaLink(final Entity entity, final String name) {
    final String rel = Constants.NS_MEDIA_EDIT_LINK_REL + name;
    for (Link link : entity.getMediaEditLinks()) {
      if (link.getRel().equals(rel)) {        
        return link;
      }
    }
    final Link link = new Link();
    link.setTitle(name);
    link.setRel(rel);
    link.setType(Constants.MEDIA_EDIT_LINK_TYPE);
    entity.getMediaEditLinks().add(link);
    return link;
  }
}
