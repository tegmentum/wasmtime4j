/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentItemInfo} and its nested records.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentItemInfo")
class ComponentItemInfoTest {

  @Nested
  @DisplayName("ComponentFuncInfo")
  class ComponentFuncInfoTests {

    @Test
    @DisplayName("creates with params and results")
    void createsWithParamsAndResults() {
      final ComponentTypeDescriptor s32Desc =
          ComponentTypeDescriptor.fromComponentType(ComponentType.S32);
      final ComponentItemInfo.NamedType param = new ComponentItemInfo.NamedType("x", s32Desc);
      final ComponentItemInfo.ComponentFuncInfo info =
          new ComponentItemInfo.ComponentFuncInfo(List.of(param), List.of(s32Desc), false);
      assertEquals(ComponentItemKind.COMPONENT_FUNC, info.kind());
      assertEquals(1, info.params().size());
      assertEquals("x", info.params().get(0).name());
      assertEquals(1, info.results().size());
      assertFalse(info.isAsync());
    }

    @Test
    @DisplayName("params list is immutable")
    void paramsImmutable() {
      final ComponentItemInfo.ComponentFuncInfo info =
          new ComponentItemInfo.ComponentFuncInfo(
              Collections.emptyList(), Collections.emptyList(), false);
      assertThrows(
          UnsupportedOperationException.class,
          () ->
              info.params()
                  .add(
                      new ComponentItemInfo.NamedType(
                          "a", ComponentTypeDescriptor.fromComponentType(ComponentType.S32))));
    }

    @Test
    @DisplayName("results list is immutable")
    void resultsImmutable() {
      final ComponentItemInfo.ComponentFuncInfo info =
          new ComponentItemInfo.ComponentFuncInfo(
              Collections.emptyList(), Collections.emptyList(), false);
      assertThrows(
          UnsupportedOperationException.class,
          () -> info.results().add(ComponentTypeDescriptor.fromComponentType(ComponentType.S32)));
    }
  }

  @Nested
  @DisplayName("CoreFuncInfo")
  class CoreFuncInfoTests {

    @Test
    @DisplayName("creates with core types")
    void createsWithCoreTypes() {
      final ComponentItemInfo.CoreFuncInfo info =
          new ComponentItemInfo.CoreFuncInfo(List.of("i32", "i64"), List.of("i32"));
      assertEquals(ComponentItemKind.CORE_FUNC, info.kind());
      assertEquals(2, info.params().size());
      assertEquals(1, info.results().size());
      assertEquals("i32", info.params().get(0));
    }
  }

  @Nested
  @DisplayName("ModuleInfo")
  class ModuleInfoTests {

    @Test
    @DisplayName("creates with name")
    void createsWithName() {
      final ComponentItemInfo.ModuleInfo info = new ComponentItemInfo.ModuleInfo("myModule");
      assertEquals(ComponentItemKind.MODULE, info.kind());
      assertEquals("myModule", info.name());
    }

    @Test
    @DisplayName("creates with null name")
    void createsWithNullName() {
      final ComponentItemInfo.ModuleInfo info = new ComponentItemInfo.ModuleInfo(null);
      assertNull(info.name());
    }
  }

  @Nested
  @DisplayName("ComponentInfo")
  class ComponentInfoTests {

    @Test
    @DisplayName("creates with imports and exports")
    void createsWithImportsAndExports() {
      final ComponentItemInfo.ModuleInfo modInfo = new ComponentItemInfo.ModuleInfo("inner");
      final ComponentItemInfo.ComponentInfo info =
          new ComponentItemInfo.ComponentInfo(
              Map.of("import1", modInfo), Map.of("export1", modInfo));
      assertEquals(ComponentItemKind.COMPONENT, info.kind());
      assertEquals(1, info.imports().size());
      assertEquals(1, info.exports().size());
    }

    @Test
    @DisplayName("maps are immutable")
    void mapsImmutable() {
      final ComponentItemInfo.ComponentInfo info =
          new ComponentItemInfo.ComponentInfo(Collections.emptyMap(), Collections.emptyMap());
      assertThrows(
          UnsupportedOperationException.class,
          () -> info.imports().put("key", new ComponentItemInfo.ModuleInfo("x")));
      assertThrows(
          UnsupportedOperationException.class,
          () -> info.exports().put("key", new ComponentItemInfo.ModuleInfo("x")));
    }
  }

  @Nested
  @DisplayName("ComponentInstanceInfo")
  class ComponentInstanceInfoTests {

    @Test
    @DisplayName("creates with exports")
    void createsWithExports() {
      final ComponentItemInfo.ComponentInstanceInfo info =
          new ComponentItemInfo.ComponentInstanceInfo(
              Map.of("func1", new ComponentItemInfo.ModuleInfo("m")));
      assertEquals(ComponentItemKind.COMPONENT_INSTANCE, info.kind());
      assertEquals(1, info.exports().size());
    }
  }

  @Nested
  @DisplayName("TypeInfo")
  class TypeInfoTests {

    @Test
    @DisplayName("creates with descriptor")
    void createsWithDescriptor() {
      final ComponentTypeDescriptor desc =
          ComponentTypeDescriptor.fromComponentType(ComponentType.STRING);
      final ComponentItemInfo.TypeInfo info = new ComponentItemInfo.TypeInfo(desc);
      assertEquals(ComponentItemKind.TYPE, info.kind());
      assertEquals(desc, info.descriptor());
    }

    @Test
    @DisplayName("rejects null descriptor")
    void rejectsNullDescriptor() {
      assertThrows(NullPointerException.class, () -> new ComponentItemInfo.TypeInfo(null));
    }
  }

  @Nested
  @DisplayName("ResourceInfo")
  class ResourceInfoTests {

    @Test
    @DisplayName("creates with all fields")
    void createsWithAllFields() {
      final ComponentItemInfo.ResourceInfo info =
          new ComponentItemInfo.ResourceInfo("file", 42L, "debug-id", true);
      assertEquals(ComponentItemKind.RESOURCE, info.kind());
      assertEquals("file", info.name());
      assertEquals(42L, info.resourceTypeId());
      assertEquals("debug-id", info.debugIdentity());
      assertTrue(info.hostDefined());
    }

    @Test
    @DisplayName("creates with backward-compatible constructor")
    void backwardCompatibleConstructor() {
      final ComponentItemInfo.ResourceInfo info = new ComponentItemInfo.ResourceInfo("stream", 7L);
      assertEquals("stream", info.name());
      assertEquals(7L, info.resourceTypeId());
      assertNull(info.debugIdentity());
      assertFalse(info.hostDefined());
    }
  }

  @Nested
  @DisplayName("NamedType")
  class NamedTypeTests {

    @Test
    @DisplayName("creates with name and type")
    void createsWithNameAndType() {
      final ComponentTypeDescriptor desc =
          ComponentTypeDescriptor.fromComponentType(ComponentType.BOOL);
      final ComponentItemInfo.NamedType namedType = new ComponentItemInfo.NamedType("flag", desc);
      assertEquals("flag", namedType.name());
      assertEquals(desc, namedType.type());
    }

    @Test
    @DisplayName("rejects null name")
    void rejectsNullName() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ComponentItemInfo.NamedType(
                  null, ComponentTypeDescriptor.fromComponentType(ComponentType.S32)));
    }

    @Test
    @DisplayName("rejects null type")
    void rejectsNullType() {
      assertThrows(NullPointerException.class, () -> new ComponentItemInfo.NamedType("x", null));
    }
  }
}
