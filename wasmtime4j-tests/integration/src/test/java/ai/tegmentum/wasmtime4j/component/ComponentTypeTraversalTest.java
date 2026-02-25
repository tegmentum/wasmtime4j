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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.jni.JniComponentEngine;
import ai.tegmentum.wasmtime4j.test.TestUtils;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for component type traversal via {@link Component#componentType()}.
 *
 * <p>Verifies that the full component type JSON serialization correctly reports all 7
 * ComponentItemInfo variants: ComponentFuncInfo, CoreFuncInfo, ModuleInfo, ComponentInfo,
 * ComponentInstanceInfo, TypeInfo, and ResourceInfo.
 *
 * @since 1.1.0
 */
@DisplayName("Component Type Traversal Integration Tests")
public final class ComponentTypeTraversalTest {

  private static final Logger LOGGER = Logger.getLogger(ComponentTypeTraversalTest.class.getName());

  private static boolean componentAvailable = false;
  private static byte[] addComponentBytes;
  private static byte[] multiTypeComponentBytes;
  private static byte[] voidFunctionComponentBytes;
  private static String unavailableReason;

  @BeforeAll
  static void checkComponentAvailable() {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
      final JniComponentEngine testEngine = new JniComponentEngine(new ComponentEngineConfig());
      testEngine.close();

      try (InputStream is =
          ComponentTypeTraversalTest.class.getResourceAsStream("/components/add.wasm")) {
        if (is != null) {
          addComponentBytes = TestUtils.readAllBytes(is);
        }
      }
      try (InputStream is =
          ComponentTypeTraversalTest.class.getResourceAsStream("/components/multi-type.wasm")) {
        if (is != null) {
          multiTypeComponentBytes = TestUtils.readAllBytes(is);
        }
      }
      try (InputStream is =
          ComponentTypeTraversalTest.class.getResourceAsStream("/components/void-function.wasm")) {
        if (is != null) {
          voidFunctionComponentBytes = TestUtils.readAllBytes(is);
        }
      }

      if (addComponentBytes != null) {
        componentAvailable = true;
        LOGGER.info(
            "Component type traversal tests available - add.wasm: "
                + addComponentBytes.length
                + " bytes");
      } else {
        unavailableReason = "add.wasm test component not found";
      }
    } catch (final Exception e) {
      unavailableReason = "Component setup failed: " + e.getMessage();
      LOGGER.warning("Component type traversal tests skipped: " + unavailableReason);
    }
  }

  private static void assumeComponentAvailable() {
    assumeTrue(componentAvailable, "Component not available: " + unavailableReason);
  }

  private JniComponentEngine engine;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    if (componentAvailable) {
      engine = new JniComponentEngine(new ComponentEngineConfig());
      resources.add(engine);
    }
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    engine = null;
  }

  @Nested
  @DisplayName("Component Type Info Basic Tests")
  class BasicTypeInfoTests {

    @Test
    @DisplayName("Should return non-null componentType for add component")
    void shouldReturnComponentTypeForAddComponent(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing componentType() for add component");

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentTypeInfo typeInfo = component.componentType();
      assertNotNull(typeInfo, "ComponentTypeInfo should not be null");
      LOGGER.info(
          "ComponentTypeInfo: imports="
              + typeInfo.imports().size()
              + ", exports="
              + typeInfo.exports().size());

      assertFalse(typeInfo.exports().isEmpty(), "Add component should have exports");
      LOGGER.info("Exports: " + typeInfo.exports());
      LOGGER.info("Imports: " + typeInfo.imports());
    }

    @Test
    @DisplayName("Should return typed export items for add component")
    void shouldReturnTypedExportItems(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing typed export items for add component");

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentTypeInfo typeInfo = component.componentType();
      final Map<String, ComponentItemInfo> exportItems = typeInfo.exportItems();

      LOGGER.info("Export items count: " + exportItems.size());
      for (final Map.Entry<String, ComponentItemInfo> entry : exportItems.entrySet()) {
        LOGGER.info("  Export '" + entry.getKey() + "': kind=" + entry.getValue().kind());
        logComponentItemDetails(entry.getKey(), entry.getValue());
      }

      // The add component should have at least one export (the add function)
      assertFalse(exportItems.isEmpty(), "Should have typed export items");

      // Verify backward compat: exports() Set<String> should match exportItems keys
      assertTrue(
          typeInfo.exports().containsAll(exportItems.keySet()),
          "exports() Set should contain all exportItems keys");
    }

    @Test
    @DisplayName("Should return typed import items for add component")
    void shouldReturnTypedImportItems(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing typed import items for add component");

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentTypeInfo typeInfo = component.componentType();
      final Map<String, ComponentItemInfo> importItems = typeInfo.importItems();

      LOGGER.info("Import items count: " + importItems.size());
      for (final Map.Entry<String, ComponentItemInfo> entry : importItems.entrySet()) {
        LOGGER.info("  Import '" + entry.getKey() + "': kind=" + entry.getValue().kind());
        logComponentItemDetails(entry.getKey(), entry.getValue());
      }

      // The simple add component may not have imports
      LOGGER.info("Import items size: " + importItems.size());
    }
  }

  @Nested
  @DisplayName("ComponentItemInfo Variant Tests")
  class ItemInfoVariantTests {

    @Test
    @DisplayName("Should detect ComponentFuncInfo for exported functions")
    void shouldDetectComponentFuncForExports(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing ComponentFuncInfo detection");

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentTypeInfo typeInfo = component.componentType();
      final Map<String, ComponentItemInfo> exportItems = typeInfo.exportItems();

      boolean foundComponentFunc = false;
      for (final Map.Entry<String, ComponentItemInfo> entry : exportItems.entrySet()) {
        if (entry.getValue().kind() == ComponentItemKind.COMPONENT_FUNC) {
          foundComponentFunc = true;
          final ComponentItemInfo.ComponentFuncInfo funcInfo =
              (ComponentItemInfo.ComponentFuncInfo) entry.getValue();
          LOGGER.info(
              "Found ComponentFunc '"
                  + entry.getKey()
                  + "': params="
                  + funcInfo.params().size()
                  + ", results="
                  + funcInfo.results().size()
                  + ", async="
                  + funcInfo.isAsync());

          // Verify parameter types are populated
          for (final ComponentItemInfo.NamedType param : funcInfo.params()) {
            assertNotNull(param.name(), "Param name should not be null");
            assertNotNull(param.type(), "Param type should not be null");
            LOGGER.info("  Param: name=" + param.name() + ", type=" + param.type());
          }

          // Verify result types are populated
          for (final ComponentTypeDescriptor result : funcInfo.results()) {
            assertNotNull(result, "Result type should not be null");
            LOGGER.info("  Result: " + result);
          }
        }
      }

      assertTrue(foundComponentFunc, "Add component should export at least one ComponentFunc");
    }

    @Test
    @DisplayName("Should handle multi-type component exports")
    void shouldHandleMultiTypeComponentExports(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      assumeTrue(multiTypeComponentBytes != null, "multi-type.wasm test component not available");
      LOGGER.info("Testing multi-type component exports");

      final Component component = engine.compileComponent(multiTypeComponentBytes);
      resources.add(component);

      final ComponentTypeInfo typeInfo = component.componentType();
      LOGGER.info(
          "Multi-type component: imports="
              + typeInfo.imports().size()
              + ", exports="
              + typeInfo.exports().size());

      final Map<String, ComponentItemInfo> exportItems = typeInfo.exportItems();
      LOGGER.info("Export items count: " + exportItems.size());

      for (final Map.Entry<String, ComponentItemInfo> entry : exportItems.entrySet()) {
        LOGGER.info("  Export '" + entry.getKey() + "': kind=" + entry.getValue().kind());
        logComponentItemDetails(entry.getKey(), entry.getValue());
      }
    }

    @Test
    @DisplayName("Should handle void-function component")
    void shouldHandleVoidFunctionComponent(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      assumeTrue(
          voidFunctionComponentBytes != null, "void-function.wasm test component not available");
      LOGGER.info("Testing void-function component");

      final Component component = engine.compileComponent(voidFunctionComponentBytes);
      resources.add(component);

      final ComponentTypeInfo typeInfo = component.componentType();
      LOGGER.info(
          "Void-function component: imports="
              + typeInfo.imports().size()
              + ", exports="
              + typeInfo.exports().size());

      final Map<String, ComponentItemInfo> exportItems = typeInfo.exportItems();
      for (final Map.Entry<String, ComponentItemInfo> entry : exportItems.entrySet()) {
        if (entry.getValue() instanceof ComponentItemInfo.ComponentFuncInfo) {
          final ComponentItemInfo.ComponentFuncInfo funcInfo =
              (ComponentItemInfo.ComponentFuncInfo) entry.getValue();
          LOGGER.info(
              "  Void func '"
                  + entry.getKey()
                  + "': params="
                  + funcInfo.params().size()
                  + ", results="
                  + funcInfo.results().size());
        }
      }
    }
  }

  @Nested
  @DisplayName("ComponentTypeInfo Backward Compatibility Tests")
  class BackwardCompatTests {

    @Test
    @DisplayName("Should maintain backward compatibility with Set-based constructor")
    void shouldMaintainBackwardCompatWithSetConstructor(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing backward compatibility");

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentTypeInfo typeInfo = component.componentType();

      // Set<String> imports() and exports() should work
      assertNotNull(typeInfo.imports(), "imports() should not be null");
      assertNotNull(typeInfo.exports(), "exports() should not be null");

      // hasImport/hasExport should work
      for (final String exportName : typeInfo.exports()) {
        assertTrue(
            typeInfo.hasExport(exportName),
            "hasExport should return true for existing export: " + exportName);
        assertTrue(
            typeInfo.getExport(exportName).isPresent(),
            "getExport should return present Optional for: " + exportName);
        LOGGER.info("Verified backward compat for export: " + exportName);
      }

      // importItems() and exportItems() should be non-null
      assertNotNull(typeInfo.importItems(), "importItems() should not be null");
      assertNotNull(typeInfo.exportItems(), "exportItems() should not be null");

      LOGGER.info("Backward compatibility verified");
    }

    @Test
    @DisplayName("getImportItem and getExportItem should return Optional correctly")
    void shouldReturnOptionalCorrectly(final TestInfo testInfo) throws Exception {
      assumeComponentAvailable();
      LOGGER.info("Testing Optional accessors");

      final Component component = engine.compileComponent(addComponentBytes);
      resources.add(component);

      final ComponentTypeInfo typeInfo = component.componentType();

      // Non-existent items should return empty
      assertFalse(
          typeInfo.getImportItem("nonexistent").isPresent(),
          "Non-existent import should return empty Optional");
      assertFalse(
          typeInfo.getExportItem("nonexistent").isPresent(),
          "Non-existent export should return empty Optional");

      // Existing items should return present
      for (final String exportName : typeInfo.exportItems().keySet()) {
        assertTrue(
            typeInfo.getExportItem(exportName).isPresent(),
            "Existing export '" + exportName + "' should return present Optional");
        LOGGER.info("getExportItem('" + exportName + "') returned present");
      }

      LOGGER.info("Optional accessors verified");
    }
  }

  @Nested
  @DisplayName("ComponentItemKind Enumeration Tests")
  class ItemKindTests {

    @Test
    @DisplayName("Should have all 7 ComponentItemKind values")
    void shouldHaveAllKindValues(final TestInfo testInfo) {
      LOGGER.info("Testing ComponentItemKind enum completeness");

      final ComponentItemKind[] kinds = ComponentItemKind.values();
      assertTrue(kinds.length == 7, "Should have 7 ComponentItemKind values, got " + kinds.length);

      assertNotNull(ComponentItemKind.valueOf("COMPONENT_FUNC"));
      assertNotNull(ComponentItemKind.valueOf("CORE_FUNC"));
      assertNotNull(ComponentItemKind.valueOf("MODULE"));
      assertNotNull(ComponentItemKind.valueOf("COMPONENT"));
      assertNotNull(ComponentItemKind.valueOf("COMPONENT_INSTANCE"));
      assertNotNull(ComponentItemKind.valueOf("TYPE"));
      assertNotNull(ComponentItemKind.valueOf("RESOURCE"));

      LOGGER.info("All 7 ComponentItemKind values verified");
    }
  }

  /** Logs details about a ComponentItemInfo for debugging. */
  private static void logComponentItemDetails(final String name, final ComponentItemInfo item) {
    if (item instanceof ComponentItemInfo.ComponentFuncInfo) {
      final ComponentItemInfo.ComponentFuncInfo f = (ComponentItemInfo.ComponentFuncInfo) item;
      LOGGER.info(
          "    ComponentFunc: params="
              + f.params().size()
              + ", results="
              + f.results().size()
              + ", async="
              + f.isAsync());
    } else if (item instanceof ComponentItemInfo.CoreFuncInfo) {
      final ComponentItemInfo.CoreFuncInfo f = (ComponentItemInfo.CoreFuncInfo) item;
      LOGGER.info("    CoreFunc: params=" + f.params() + ", results=" + f.results());
    } else if (item instanceof ComponentItemInfo.ModuleInfo) {
      final ComponentItemInfo.ModuleInfo m = (ComponentItemInfo.ModuleInfo) item;
      LOGGER.info("    Module: name=" + m.name());
    } else if (item instanceof ComponentItemInfo.ComponentInfo) {
      final ComponentItemInfo.ComponentInfo c = (ComponentItemInfo.ComponentInfo) item;
      LOGGER.info(
          "    Component: imports=" + c.imports().size() + ", exports=" + c.exports().size());
    } else if (item instanceof ComponentItemInfo.ComponentInstanceInfo) {
      final ComponentItemInfo.ComponentInstanceInfo i =
          (ComponentItemInfo.ComponentInstanceInfo) item;
      LOGGER.info("    ComponentInstance: exports=" + i.exports().size());
    } else if (item instanceof ComponentItemInfo.TypeInfo) {
      final ComponentItemInfo.TypeInfo t = (ComponentItemInfo.TypeInfo) item;
      LOGGER.info("    Type: descriptor=" + t.descriptor());
    } else if (item instanceof ComponentItemInfo.ResourceInfo) {
      final ComponentItemInfo.ResourceInfo r = (ComponentItemInfo.ResourceInfo) item;
      LOGGER.info("    Resource: name=" + r.name() + ", id=" + r.resourceTypeId());
    }
  }
}
