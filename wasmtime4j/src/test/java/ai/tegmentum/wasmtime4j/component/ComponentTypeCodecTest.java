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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentTypeCodec}.
 *
 * <p>Validates JSON deserialization of component type descriptors, with particular focus on
 * resource metadata parsing (resourceTypeDebug field) added for diagnostic correlation.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentTypeCodec")
class ComponentTypeCodecTest {

  private static final Logger LOGGER = Logger.getLogger(ComponentTypeCodecTest.class.getName());

  @Nested
  @DisplayName("Resource Item Parsing")
  class ResourceItemParsing {

    @Test
    @DisplayName("should parse resource with resourceTypeDebug into ResourceInfo.name")
    void shouldParseResourceWithDebugString() {
      final String json =
          "{\"imports\":{},\"exports\":{\"my-resource\":"
              + "{\"kind\":\"resource\",\"resourceTypeDebug\":\"ResourceType { index: 5 }\"}}}";

      LOGGER.info("Parsing JSON: " + json);
      final ComponentTypeInfo info = ComponentTypeCodec.deserialize(json);

      assertNotNull(info, "ComponentTypeInfo should not be null");
      assertNotNull(info.exportItems(), "Export items should not be null");
      assertEquals(1, info.exportItems().size(), "Should have exactly one export");

      final ComponentItemInfo item = info.exportItems().get("my-resource");
      assertNotNull(item, "Export 'my-resource' should exist");
      assertInstanceOf(ComponentItemInfo.ResourceInfo.class, item, "Item should be a ResourceInfo");

      final ComponentItemInfo.ResourceInfo resource = (ComponentItemInfo.ResourceInfo) item;
      assertEquals(ComponentItemKind.RESOURCE, resource.kind(), "Kind should be RESOURCE");
      assertEquals(
          "ResourceType { index: 5 }",
          resource.name(),
          "ResourceInfo.name should contain the resourceTypeDebug string");
      assertEquals(0, resource.resourceTypeId(), "resourceTypeId should be 0 (opaque)");

      LOGGER.info(
          "Parsed ResourceInfo: name="
              + resource.name()
              + ", resourceTypeId="
              + resource.resourceTypeId());
    }

    @Test
    @DisplayName("should parse resource without resourceTypeDebug as null name")
    void shouldParseResourceWithoutDebugString() {
      final String json = "{\"imports\":{},\"exports\":{\"legacy-res\":{\"kind\":\"resource\"}}}";

      LOGGER.info("Parsing JSON without resourceTypeDebug: " + json);
      final ComponentTypeInfo info = ComponentTypeCodec.deserialize(json);

      final ComponentItemInfo.ResourceInfo resource =
          (ComponentItemInfo.ResourceInfo) info.exportItems().get("legacy-res");
      assertNotNull(resource, "Resource should be parsed");
      assertNull(resource.name(), "Name should be null when resourceTypeDebug is absent");
      assertEquals(0, resource.resourceTypeId(), "resourceTypeId should be 0");

      LOGGER.info("Parsed legacy resource with null name as expected");
    }

    @Test
    @DisplayName("should parse resource with escaped characters in debug string")
    void shouldParseResourceWithEscapedDebugString() {
      final String json =
          "{\"imports\":{},\"exports\":{\"esc-res\":"
              + "{\"kind\":\"resource\",\"resourceTypeDebug\":"
              + "\"ResourceType { name: \\\"file-handle\\\" }\"}}}";

      LOGGER.info("Parsing JSON with escaped debug string: " + json);
      final ComponentTypeInfo info = ComponentTypeCodec.deserialize(json);

      final ComponentItemInfo.ResourceInfo resource =
          (ComponentItemInfo.ResourceInfo) info.exportItems().get("esc-res");
      assertNotNull(resource, "Resource should be parsed");
      assertEquals(
          "ResourceType { name: \"file-handle\" }",
          resource.name(),
          "Escaped quotes in debug string should be unescaped");

      LOGGER.info("Parsed escaped debug string: " + resource.name());
    }

    @Test
    @DisplayName("should preserve resource name as map key separately from debug info")
    void shouldPreserveResourceNameAsMapKey() {
      final String json =
          "{\"imports\":{\"input-stream\":"
              + "{\"kind\":\"resource\",\"resourceTypeDebug\":\"ResourceType(42)\"}}"
              + ",\"exports\":{\"output-stream\":"
              + "{\"kind\":\"resource\",\"resourceTypeDebug\":\"ResourceType(43)\"}}}";

      LOGGER.info("Parsing JSON with resources in both imports and exports: " + json);
      final ComponentTypeInfo info = ComponentTypeCodec.deserialize(json);

      assertEquals(1, info.importItems().size(), "Should have one import");
      assertEquals(1, info.exportItems().size(), "Should have one export");

      final ComponentItemInfo.ResourceInfo importRes =
          (ComponentItemInfo.ResourceInfo) info.importItems().get("input-stream");
      final ComponentItemInfo.ResourceInfo exportRes =
          (ComponentItemInfo.ResourceInfo) info.exportItems().get("output-stream");

      assertNotNull(importRes, "Import resource should exist");
      assertNotNull(exportRes, "Export resource should exist");

      assertEquals(
          "ResourceType(42)", importRes.name(), "Import resource debug string should match");
      assertEquals(
          "ResourceType(43)", exportRes.name(), "Export resource debug string should match");

      LOGGER.info(
          "Import resource debug: "
              + importRes.name()
              + ", Export resource debug: "
              + exportRes.name());
    }
  }

  @Nested
  @DisplayName("Own/Borrow Type Descriptor Parsing")
  class OwnBorrowParsing {

    @Test
    @DisplayName("should parse own type with resourceTypeDebug")
    void shouldParseOwnWithDebugString() {
      final String json =
          "{\"imports\":{},\"exports\":{\"create\":"
              + "{\"kind\":\"component_func\","
              + "\"params\":[],\"results\":[{\"type\":\"own\","
              + "\"resourceTypeDebug\":\"ResourceType { index: 7 }\"}]}}}";

      LOGGER.info("Parsing own type with debug string: " + json);
      final ComponentTypeInfo info = ComponentTypeCodec.deserialize(json);

      final ComponentItemInfo.ComponentFuncInfo func =
          (ComponentItemInfo.ComponentFuncInfo) info.exportItems().get("create");
      assertNotNull(func, "Function should exist");
      assertEquals(1, func.results().size(), "Should have one result");

      final ComponentTypeDescriptor result = func.results().get(0);
      assertEquals(ComponentType.OWN, result.getType(), "Result type should be OWN");
      assertEquals(
          "ResourceType { index: 7 }",
          result.getResourceTypeName(),
          "Own type should carry the resourceTypeDebug string as resource type name");
      assertEquals(0, result.getResourceTypeId(), "Resource type ID should be 0");

      LOGGER.info(
          "Parsed own type: resourceTypeName="
              + result.getResourceTypeName()
              + ", resourceTypeId="
              + result.getResourceTypeId());
    }

    @Test
    @DisplayName("should parse borrow type with resourceTypeDebug")
    void shouldParseBorrowWithDebugString() {
      final String json =
          "{\"imports\":{},\"exports\":{\"read\":"
              + "{\"kind\":\"component_func\","
              + "\"params\":[{\"name\":\"handle\",\"type\":"
              + "{\"type\":\"borrow\","
              + "\"resourceTypeDebug\":\"ResourceType { index: 7 }\"}}],"
              + "\"results\":[\"string\"]}}}";

      LOGGER.info("Parsing borrow type with debug string: " + json);
      final ComponentTypeInfo info = ComponentTypeCodec.deserialize(json);

      final ComponentItemInfo.ComponentFuncInfo func =
          (ComponentItemInfo.ComponentFuncInfo) info.exportItems().get("read");
      assertNotNull(func, "Function should exist");
      assertEquals(1, func.params().size(), "Should have one param");

      final ComponentTypeDescriptor paramType = func.params().get(0).type();
      assertEquals(ComponentType.BORROW, paramType.getType(), "Param type should be BORROW");
      assertEquals(
          "ResourceType { index: 7 }",
          paramType.getResourceTypeName(),
          "Borrow type should carry the resourceTypeDebug string");

      LOGGER.info("Parsed borrow param: " + paramType.getResourceTypeName());
    }

    @Test
    @DisplayName("should fall back to 'resource' when own has no resourceTypeDebug")
    void shouldFallBackForOwnWithoutDebug() {
      final String json =
          "{\"imports\":{},\"exports\":{\"create\":"
              + "{\"kind\":\"component_func\","
              + "\"params\":[],\"results\":[{\"type\":\"own\"}]}}}";

      LOGGER.info("Parsing own type without debug string: " + json);
      final ComponentTypeInfo info = ComponentTypeCodec.deserialize(json);

      final ComponentItemInfo.ComponentFuncInfo func =
          (ComponentItemInfo.ComponentFuncInfo) info.exportItems().get("create");
      final ComponentTypeDescriptor result = func.results().get(0);

      assertEquals(ComponentType.OWN, result.getType(), "Result type should be OWN");
      assertEquals(
          "resource",
          result.getResourceTypeName(),
          "Should fall back to 'resource' when resourceTypeDebug is absent");

      LOGGER.info("Fallback own type name: " + result.getResourceTypeName());
    }

    @Test
    @DisplayName("should fall back to 'resource' when borrow has no resourceTypeDebug")
    void shouldFallBackForBorrowWithoutDebug() {
      final String json =
          "{\"imports\":{},\"exports\":{\"read\":"
              + "{\"kind\":\"component_func\","
              + "\"params\":[{\"name\":\"h\",\"type\":{\"type\":\"borrow\"}}],"
              + "\"results\":[\"u32\"]}}}";

      LOGGER.info("Parsing borrow type without debug string: " + json);
      final ComponentTypeInfo info = ComponentTypeCodec.deserialize(json);

      final ComponentItemInfo.ComponentFuncInfo func =
          (ComponentItemInfo.ComponentFuncInfo) info.exportItems().get("read");
      final ComponentTypeDescriptor paramType = func.params().get(0).type();

      assertEquals(ComponentType.BORROW, paramType.getType(), "Param type should be BORROW");
      assertEquals(
          "resource",
          paramType.getResourceTypeName(),
          "Should fall back to 'resource' when resourceTypeDebug is absent");

      LOGGER.info("Fallback borrow type name: " + paramType.getResourceTypeName());
    }
  }

  @Nested
  @DisplayName("Full Component With Resources")
  class FullComponentWithResources {

    @Test
    @DisplayName("should parse a component with resource export and functions using own/borrow")
    void shouldParseComponentWithResourceAndFunctions() {
      // Simulates a WASI-like component type with a resource and functions that use it
      final String json =
          "{\"imports\":{},\"exports\":{"
              + "\"file-handle\":"
              + "{\"kind\":\"resource\",\"resourceTypeDebug\":\"ResourceType(99)\"},"
              + "\"open\":"
              + "{\"kind\":\"component_func\","
              + "\"params\":[{\"name\":\"path\",\"type\":\"string\"}],"
              + "\"results\":[{\"type\":\"own\","
              + "\"resourceTypeDebug\":\"ResourceType(99)\"}]},"
              + "\"read\":"
              + "{\"kind\":\"component_func\","
              + "\"params\":[{\"name\":\"fd\",\"type\":"
              + "{\"type\":\"borrow\","
              + "\"resourceTypeDebug\":\"ResourceType(99)\"}}],"
              + "\"results\":[{\"type\":\"list\",\"element\":\"u8\"}]}"
              + "}}";

      LOGGER.info("Parsing full component with resource: " + json);
      final ComponentTypeInfo info = ComponentTypeCodec.deserialize(json);

      final Map<String, ComponentItemInfo> exports = info.exportItems();
      assertEquals(3, exports.size(), "Should have 3 exports (resource + 2 functions)");

      // Verify the resource export
      final ComponentItemInfo.ResourceInfo resource =
          assertInstanceOf(
              ComponentItemInfo.ResourceInfo.class,
              exports.get("file-handle"),
              "file-handle should be a ResourceInfo");
      assertEquals("ResourceType(99)", resource.name(), "Resource debug string should match");

      // Verify open() returns own<file-handle>
      final ComponentItemInfo.ComponentFuncInfo openFunc =
          assertInstanceOf(
              ComponentItemInfo.ComponentFuncInfo.class,
              exports.get("open"),
              "open should be a ComponentFuncInfo");
      assertEquals(1, openFunc.params().size(), "open should have 1 param");
      assertEquals(
          ComponentType.STRING,
          openFunc.params().get(0).type().getType(),
          "open param should be string");
      assertEquals(1, openFunc.results().size(), "open should have 1 result");
      assertEquals(
          ComponentType.OWN, openFunc.results().get(0).getType(), "open result should be own");
      assertEquals(
          "ResourceType(99)",
          openFunc.results().get(0).getResourceTypeName(),
          "own result should reference the same resource debug string");

      // Verify read() takes borrow<file-handle>
      final ComponentItemInfo.ComponentFuncInfo readFunc =
          assertInstanceOf(
              ComponentItemInfo.ComponentFuncInfo.class,
              exports.get("read"),
              "read should be a ComponentFuncInfo");
      assertEquals(1, readFunc.params().size(), "read should have 1 param");
      assertEquals(
          ComponentType.BORROW,
          readFunc.params().get(0).type().getType(),
          "read param should be borrow");
      assertEquals(
          "ResourceType(99)",
          readFunc.params().get(0).type().getResourceTypeName(),
          "borrow param should reference the same resource debug string");

      // Verify the debug strings match across resource/own/borrow for correlation
      assertEquals(
          resource.name(),
          openFunc.results().get(0).getResourceTypeName(),
          "Resource debug string should match own result debug string for correlation");
      assertEquals(
          resource.name(),
          readFunc.params().get(0).type().getResourceTypeName(),
          "Resource debug string should match borrow param debug string for correlation");

      LOGGER.info(
          "Full component parsed successfully with correlated resource debug strings: "
              + resource.name());
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCases {

    @Test
    @DisplayName("should throw on missing kind field")
    void shouldThrowOnMissingKind() {
      final String json = "{\"imports\":{},\"exports\":{\"bad\":{\"noKind\":true}}}";

      LOGGER.info("Parsing JSON with missing kind: " + json);
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> ComponentTypeCodec.deserialize(json),
              "Should throw on missing kind");
      LOGGER.info("Got expected exception: " + ex.getMessage());
    }

    @Test
    @DisplayName("should throw on unknown kind")
    void shouldThrowOnUnknownKind() {
      final String json = "{\"imports\":{},\"exports\":{\"bad\":{\"kind\":\"alien\"}}}";

      LOGGER.info("Parsing JSON with unknown kind: " + json);
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> ComponentTypeCodec.deserialize(json),
              "Should throw on unknown kind");
      LOGGER.info("Got expected exception: " + ex.getMessage());
    }

    @Test
    @DisplayName("should handle empty imports and exports")
    void shouldHandleEmptyImportsAndExports() {
      final String json = "{\"imports\":{},\"exports\":{}}";

      LOGGER.info("Parsing empty component type: " + json);
      final ComponentTypeInfo info = ComponentTypeCodec.deserialize(json);

      assertNotNull(info, "Should parse successfully");
      assertEquals(0, info.importItems().size(), "Imports should be empty");
      assertEquals(0, info.exportItems().size(), "Exports should be empty");

      LOGGER.info("Empty component type parsed successfully");
    }
  }
}
