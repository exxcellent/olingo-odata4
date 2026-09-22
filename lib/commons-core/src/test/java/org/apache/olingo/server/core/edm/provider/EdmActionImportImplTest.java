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
package org.apache.olingo.server.core.edm.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.EdmActionImport;
import org.apache.olingo.commons.api.edm.EdmEntityContainer;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmException;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlActionImport;
import org.apache.olingo.commons.core.edm.EdmActionImportImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EdmActionImportImplTest {

  EdmEntityContainer container;

  EdmActionImport actionImport;

  private EdmAction action;

  private EdmEntitySet entitySet;

  @BeforeEach
  public void setup() {
    FullQualifiedName actionFqn = new FullQualifiedName("namespace", "actionName");
    FullQualifiedName entityContainerFqn = new FullQualifiedName("namespace", "containerName");
    String target = entityContainerFqn.getFullQualifiedNameAsString() + "/entitySetName";
    CsdlActionImport providerActionImport =
        new CsdlActionImport().setName("actionImportName").setAction(actionFqn).setEntitySet(target);

    Edm edm = mock(Edm.class);
    container = mock(EdmEntityContainer.class);
    when(edm.getEntityContainer(entityContainerFqn)).thenReturn(container);
    action = mock(EdmAction.class);
    when(edm.getUnboundAction(actionFqn)).thenReturn(action);

    entitySet = mock(EdmEntitySet.class);
    when(container.getEntitySet("entitySetName")).thenReturn(entitySet);
    actionImport = new EdmActionImportImpl(edm, container, providerActionImport);
  }

  @Test
  public void simpleActionTest() {
    assertEquals("actionImportName", actionImport.getName());
    assertTrue(container == actionImport.getEntityContainer());
    assertTrue(action == actionImport.getUnboundAction());
  }

  @Test
  public void getReturnedEntitySet() {
    EdmEntitySet returnedEntitySet = actionImport.getReturnedEntitySet();
    assertNotNull(returnedEntitySet);
    assertTrue(returnedEntitySet == entitySet);

    // Chaching
    assertTrue(returnedEntitySet == actionImport.getReturnedEntitySet());
  }

  @Test
  public void getReturnedEntitySetNonExistingContainer() {
      assertThrows(EdmException.class, () -> {
      String target = "alias.nonexisting/Es";
      CsdlActionImport providerActionImport = new CsdlActionImport().setName("actionImportName").setEntitySet(target);
      EdmActionImport actionImport =
          new EdmActionImportImpl(mock(Edm.class), container, providerActionImport);
      actionImport.getReturnedEntitySet();
      });
  }

  @Test
  public void getReturnedEntitySetNonExistingEntitySet() {
      assertThrows(EdmException.class, () -> {
      String target = "nonExisting";
      CsdlActionImport providerActionImport = new CsdlActionImport().setName("actionImportName").setEntitySet(target);
      Edm edm = mock(Edm.class);
      when(edm.getEntityContainer()).thenReturn(container);
      EdmActionImport actionImport = new EdmActionImportImpl(edm, container, providerActionImport);
      actionImport.getReturnedEntitySet();
      });
  }

}
