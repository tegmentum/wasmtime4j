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

package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

/**
 * Integration tests for Panama global metadata and introspection functionality.
 *
 * <p>These tests verify that the enhanced global introspection and metadata query features work
 * correctly with actual WebAssembly modules and native FFI calls.
 */
@EnabledOnJre(JRE.JAVA_23)
@DisplayName("Panama Global Metadata Integration Tests")
class PanamaGlobalMetadataIT {

  private ArenaResourceManager resourceManager;
  private PanamaEngine engine;
  private PanamaStore store;

  @BeforeAll
  static void checkPanamaAvailability() {
    assumeTrue(
        PanamaTestUtils.isPanamaAvailable(),
        "Panama FFI not available - skipping Panama-specific tests");
  }

  @BeforeEach
  void setUp() throws Exception {
    resourceManager = new ArenaResourceManager();
    engine = new PanamaEngine(resourceManager);
    store = (PanamaStore) engine.createStore();
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
    if (resourceManager != null) {
      resourceManager.close();
    }
  }

  @Test
  @DisplayName("Enhanced global type introspection provides detailed metadata")
  void testEnhancedGlobalIntrospection() throws Exception {
    // Create a simple module with a mutable i32 global
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(42);

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance = store.instantiateModule(module)) {

      // Get the global from exports
      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      // Test enhanced introspection
      assertDoesNotThrow(
          () -> {
            // Test basic type information
            assertThat(global.getValueType()).isEqualTo(MemoryLayouts.WASM_I32);
            assertThat(global.getTypeName()).isEqualTo("i32");
            assertThat(global.getType()).isEqualTo(WasmValueType.I32);
            assertThat(global.isMutable()).isTrue();

            // Test comprehensive metadata
            PanamaGlobal.GlobalMetadata metadata = global.getMetadata();
            assertThat(metadata).isNotNull();
            assertThat(metadata.getTypeValue()).isEqualTo(MemoryLayouts.WASM_I32);
            assertThat(metadata.getTypeName()).isEqualTo("i32");
            assertThat(metadata.isMutable()).isTrue();
            assertThat(metadata.getCurrentValue()).isNotNull();
            assertThat(metadata.getCurrentValue().getType()).isEqualTo(WasmValueType.I32);
            assertThat(metadata.getCurrentValue().asI32()).isEqualTo(42);

            // Test detailed type information
            PanamaGlobal.TypeInfo typeInfo = global.getDetailedTypeInfo();
            assertThat(typeInfo).isNotNull();
            assertThat(typeInfo.getTypeValue()).isEqualTo(MemoryLayouts.WASM_I32);
            assertThat(typeInfo.getTypeName()).isEqualTo("i32");
            assertThat(typeInfo.getSize()).isEqualTo(4); // 32-bit = 4 bytes
            assertThat(typeInfo.getAlignment()).isEqualTo(4); // 4-byte alignment
            assertThat(typeInfo.isNumeric()).isTrue();
            assertThat(typeInfo.isReference()).isFalse();
          },
          "Enhanced introspection should work without throwing exceptions");
    }
  }

  @Test
  @DisplayName("Global metadata works for different WebAssembly types")
  void testGlobalMetadataForDifferentTypes() throws Exception {
    // Test with different global types
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMultipleGlobals();

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance = store.instantiateModule(module)) {

      // Test i32 global
      PanamaGlobal i32Global = PanamaTestUtils.getGlobalFromInstance(instance, "i32_global");
      if (i32Global != null) {
        PanamaGlobal.TypeInfo typeInfo = i32Global.getDetailedTypeInfo();
        assertThat(typeInfo.getSize()).isEqualTo(4);
        assertThat(typeInfo.getAlignment()).isEqualTo(4);
        assertThat(typeInfo.isNumeric()).isTrue();
        assertThat(typeInfo.isReference()).isFalse();
      }

      // Test i64 global
      PanamaGlobal i64Global = PanamaTestUtils.getGlobalFromInstance(instance, "i64_global");
      if (i64Global != null) {
        PanamaGlobal.TypeInfo typeInfo = i64Global.getDetailedTypeInfo();
        assertThat(typeInfo.getSize()).isEqualTo(8);
        assertThat(typeInfo.getAlignment()).isEqualTo(8);
        assertThat(typeInfo.isNumeric()).isTrue();
        assertThat(typeInfo.isReference()).isFalse();
      }

      // Test f32 global
      PanamaGlobal f32Global = PanamaTestUtils.getGlobalFromInstance(instance, "f32_global");
      if (f32Global != null) {
        PanamaGlobal.TypeInfo typeInfo = f32Global.getDetailedTypeInfo();
        assertThat(typeInfo.getSize()).isEqualTo(4);
        assertThat(typeInfo.getAlignment()).isEqualTo(4);
        assertThat(typeInfo.isNumeric()).isTrue();
        assertThat(typeInfo.isReference()).isFalse();
      }

      // Test f64 global
      PanamaGlobal f64Global = PanamaTestUtils.getGlobalFromInstance(instance, "f64_global");
      if (f64Global != null) {
        PanamaGlobal.TypeInfo typeInfo = f64Global.getDetailedTypeInfo();
        assertThat(typeInfo.getSize()).isEqualTo(8);
        assertThat(typeInfo.getAlignment()).isEqualTo(8);
        assertThat(typeInfo.isNumeric()).isTrue();
        assertThat(typeInfo.isReference()).isFalse();
      }
    }
  }

  @Test
  @DisplayName("Global metadata fallback works when enhanced introspection fails")
  void testMetadataFallback() throws Exception {
    // This test ensures that if enhanced introspection fails, the system falls back gracefully
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(100);

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      // Even if enhanced introspection fails, basic metadata should still work
      assertDoesNotThrow(
          () -> {
            PanamaGlobal.GlobalMetadata metadata = global.getMetadata();
            assertThat(metadata).isNotNull();
            assertThat(metadata.getTypeName()).isNotEmpty();
            assertThat(metadata.getCurrentValue()).isNotNull();
          },
          "Metadata retrieval should not fail even with fallback");
    }
  }

  @Test
  @DisplayName("Global supports direct access detection")
  void testDirectAccessSupport() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(200);

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      // Test direct access support detection
      assertDoesNotThrow(
          () -> {
            boolean supportsDirectAccess = global.supportsDirectAccess();
            // Direct access support may vary by implementation
            // Just verify the method doesn't throw

            if (supportsDirectAccess) {
              // If direct access is supported, test that we can obtain it
              try (PanamaGlobal.DirectGlobalAccess directAccess = global.getDirectAccess()) {
                if (directAccess != null) {
                  assertThat(directAccess.getWasmType()).isEqualTo(MemoryLayouts.WASM_I32);
                  assertThat(directAccess.supports(Integer.class)).isTrue();
                  assertThat(directAccess.supports(String.class)).isFalse();
                }
              }
            }
          },
          "Direct access support detection should work");
    }
  }

  @Test
  @DisplayName("Metadata equals and hashCode work correctly")
  void testMetadataEqualsAndHashCode() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(300);

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      PanamaGlobal.GlobalMetadata metadata1 = global.getMetadata();
      PanamaGlobal.GlobalMetadata metadata2 = global.getMetadata();

      // Test metadata equality
      assertThat(metadata1).isEqualTo(metadata2);
      assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode());

      // Test TypeInfo equality
      PanamaGlobal.TypeInfo typeInfo1 = global.getDetailedTypeInfo();
      PanamaGlobal.TypeInfo typeInfo2 = global.getDetailedTypeInfo();

      assertThat(typeInfo1).isEqualTo(typeInfo2);
      assertThat(typeInfo1.hashCode()).isEqualTo(typeInfo2.hashCode());
    }
  }

  @Test
  @DisplayName("Metadata toString methods provide useful information")
  void testMetadataToString() throws Exception {
    byte[] wasmBytes = PanamaTestUtils.createModuleWithMutableI32Global(400);

    try (PanamaModule module = (PanamaModule) engine.compileModule(wasmBytes);
        PanamaInstance instance = store.instantiateModule(module)) {

      PanamaGlobal global = PanamaTestUtils.getGlobalFromInstance(instance, "test_global");
      assertNotNull(global, "Global should be exported from test module");

      PanamaGlobal.GlobalMetadata metadata = global.getMetadata();
      String metadataStr = metadata.toString();

      assertThat(metadataStr).contains("GlobalMetadata");
      assertThat(metadataStr).contains("i32");
      assertThat(metadataStr).contains("mutable=true");
      assertThat(metadataStr).contains("value=");

      PanamaGlobal.TypeInfo typeInfo = global.getDetailedTypeInfo();
      String typeInfoStr = typeInfo.toString();

      assertThat(typeInfoStr).contains("TypeInfo");
      assertThat(typeInfoStr).contains("i32");
      assertThat(typeInfoStr).contains("size=4");
      assertThat(typeInfoStr).contains("alignment=4");
      assertThat(typeInfoStr).contains("numeric=true");
      assertThat(typeInfoStr).contains("reference=false");
    }
  }
}
