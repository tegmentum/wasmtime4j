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

package ai.tegmentum.wasmtime4j.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.CodeBuilder;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FuncType;
import ai.tegmentum.wasmtime4j.GlobalType;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for GlobalType.fromNative() functionality.
 *
 * <p>These tests verify that GlobalType instances are correctly parsed from native handles when
 * retrieved from compiled WebAssembly modules. This exercises the fromNative() code path in both
 * JNI and Panama implementations.
 *
 * @since 1.0.0
 */
@DisplayName("GlobalType fromNative Integration Tests")
public final class GlobalTypeFromNativeTest {

  private static final Logger LOGGER = Logger.getLogger(GlobalTypeFromNativeTest.class.getName());

  /** Helper method to create a FuncType without a factory method. */
  private static FuncType funcType(
      final List<WasmValueType> params, final List<WasmValueType> results) {
    return new FuncType() {
      @Override
      public List<WasmValueType> getParams() {
        return Collections.unmodifiableList(params);
      }

      @Override
      public List<WasmValueType> getResults() {
        return Collections.unmodifiableList(results);
      }
    };
  }

  /**
   * Creates a WebAssembly module with an i32 mutable global.
   *
   * <pre>
   * (module
   *   (global $mutable_i32 (export "mutable_i32") (mut i32) (i32.const 42))
   *   (func (export "get_i32") (result i32) global.get 0))
   * </pre>
   */
  private static byte[] createMutableI32GlobalModule() throws Exception {
    return new CodeBuilder()
        .addType(funcType(List.of(), List.of(WasmValueType.I32))) // type 0: () -> i32
        .addGlobal(WasmValueType.I32, true, 42) // global 0: mut i32 = 42
        .addFunction(0, List.of(), new byte[] {0x23, 0x00}) // get: global.get 0
        .addExport("mutable_i32", CodeBuilder.ExportKind.GLOBAL, 0)
        .addExport("get_i32", CodeBuilder.ExportKind.FUNCTION, 0)
        .build();
  }

  /**
   * Creates a WebAssembly module with an i32 immutable global.
   *
   * <pre>
   * (module
   *   (global $const_i32 (export "const_i32") i32 (i32.const 100))
   *   (func (export "get_const") (result i32) global.get 0))
   * </pre>
   */
  private static byte[] createImmutableI32GlobalModule() throws Exception {
    return new CodeBuilder()
        .addType(funcType(List.of(), List.of(WasmValueType.I32))) // type 0: () -> i32
        .addGlobal(WasmValueType.I32, false, 100) // global 0: i32 = 100 (const)
        .addFunction(0, List.of(), new byte[] {0x23, 0x00}) // get: global.get 0
        .addExport("const_i32", CodeBuilder.ExportKind.GLOBAL, 0)
        .addExport("get_const", CodeBuilder.ExportKind.FUNCTION, 0)
        .build();
  }

  /**
   * Creates a WebAssembly module with an i64 mutable global.
   *
   * <pre>
   * (module
   *   (global $mutable_i64 (export "mutable_i64") (mut i64) (i64.const 0))
   *   (func (export "get_i64") (result i64) global.get 0))
   * </pre>
   */
  private static byte[] createMutableI64GlobalModule() throws Exception {
    return new CodeBuilder()
        .addType(funcType(List.of(), List.of(WasmValueType.I64))) // type 0: () -> i64
        .addGlobal(WasmValueType.I64, true, 0L) // global 0: mut i64 = 0
        .addFunction(0, List.of(), new byte[] {0x23, 0x00}) // get: global.get 0
        .addExport("mutable_i64", CodeBuilder.ExportKind.GLOBAL, 0)
        .addExport("get_i64", CodeBuilder.ExportKind.FUNCTION, 0)
        .build();
  }

  /**
   * Creates a WebAssembly module with an f32 mutable global. Uses raw WASM binary since CodeBuilder
   * doesn't support float initial values directly.
   *
   * <pre>
   * (module
   *   (global $mutable_f32 (export "mutable_f32") (mut f32) (f32.const 3.14)))
   * </pre>
   */
  private static byte[] createMutableF32GlobalModule() {
    // 3.14f in IEEE 754: 0x4048F5C3
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6D, // magic
      0x01,
      0x00,
      0x00,
      0x00, // version 1
      // Global section (section 6)
      0x06, // section id
      0x09, // section size (9 bytes)
      0x01, // number of globals
      0x7D, // global type: f32
      0x01, // mutable
      0x43, // f32.const
      (byte) 0xC3,
      (byte) 0xF5,
      0x48,
      0x40, // 3.14f in little-endian IEEE 754
      0x0B, // end
      // Export section (section 7)
      0x07, // section id
      0x0F, // section size (15 bytes)
      0x01, // number of exports
      0x0B, // name length
      'm',
      'u',
      't',
      'a',
      'b',
      'l',
      'e',
      '_',
      'f',
      '3',
      '2', // name: "mutable_f32"
      0x03, // export kind: global
      0x00 // global index: 0
    };
  }

  /**
   * Creates a WebAssembly module with an f64 mutable global. Uses raw WASM binary since CodeBuilder
   * doesn't support double initial values directly.
   *
   * <pre>
   * (module
   *   (global $mutable_f64 (export "mutable_f64") (mut f64) (f64.const 2.718)))
   * </pre>
   */
  private static byte[] createMutableF64GlobalModule() {
    // 2.718 in IEEE 754 double: 0x4005BE76C8B43958
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6D, // magic
      0x01,
      0x00,
      0x00,
      0x00, // version 1
      // Global section (section 6)
      0x06, // section id
      0x0D, // section size (13 bytes)
      0x01, // number of globals
      0x7C, // global type: f64
      0x01, // mutable
      0x44, // f64.const
      0x58,
      0x39,
      (byte) 0xB4,
      (byte) 0xC8,
      0x76,
      (byte) 0xBE,
      0x05,
      0x40, // 2.718 little-endian
      0x0B, // end
      // Export section (section 7)
      0x07, // section id
      0x0F, // section size (15 bytes)
      0x01, // number of exports
      0x0B, // name length
      'm',
      'u',
      't',
      'a',
      'b',
      'l',
      'e',
      '_',
      'f',
      '6',
      '4', // name: "mutable_f64"
      0x03, // export kind: global
      0x00 // global index: 0
    };
  }

  /**
   * Creates a WebAssembly module with multiple globals of different types. Uses raw WASM binary to
   * support all types including f32 and f64.
   *
   * <pre>
   * (module
   *   (global $g0 (export "g_i32") (mut i32) (i32.const 1))
   *   (global $g1 (export "g_i64") (mut i64) (i64.const 2)))
   * </pre>
   */
  private static byte[] createMultiGlobalModule() throws Exception {
    // Using CodeBuilder for i32/i64 globals only
    return new CodeBuilder()
        .addGlobal(WasmValueType.I32, true, 1) // global 0: mut i32 = 1
        .addGlobal(WasmValueType.I64, true, 2L) // global 1: mut i64 = 2
        .addGlobal(WasmValueType.I32, false, 3) // global 2: i32 = 3 (const)
        .addGlobal(WasmValueType.I64, false, 4L) // global 3: i64 = 4 (const)
        .addExport("g_i32_mut", CodeBuilder.ExportKind.GLOBAL, 0)
        .addExport("g_i64_mut", CodeBuilder.ExportKind.GLOBAL, 1)
        .addExport("g_i32_const", CodeBuilder.ExportKind.GLOBAL, 2)
        .addExport("g_i64_const", CodeBuilder.ExportKind.GLOBAL, 3)
        .build();
  }

  @Nested
  @DisplayName("Module.getGlobalType() Tests")
  class ModuleGetGlobalTypeTests {

    @Test
    @DisplayName("should get mutable i32 global type from module")
    void shouldGetMutableI32GlobalTypeFromModule() throws Exception {
      LOGGER.info("Testing Module.getGlobalType() for mutable i32 global");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createMutableI32GlobalModule())) {

        final Optional<GlobalType> globalTypeOpt = module.getGlobalType("mutable_i32");

        assertTrue(globalTypeOpt.isPresent(), "GlobalType should be present");
        final GlobalType globalType = globalTypeOpt.get();

        assertNotNull(globalType, "GlobalType should not be null");
        assertEquals(WasmValueType.I32, globalType.getValueType(), "Value type should be I32");
        assertTrue(globalType.isMutable(), "Global should be mutable");
        assertEquals(WasmTypeKind.GLOBAL, globalType.getKind(), "Kind should be GLOBAL");

        LOGGER.info("Mutable i32 global type parsed correctly from native");
      }
    }

    @Test
    @DisplayName("should get immutable i32 global type from module")
    void shouldGetImmutableI32GlobalTypeFromModule() throws Exception {
      LOGGER.info("Testing Module.getGlobalType() for immutable i32 global");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createImmutableI32GlobalModule())) {

        final Optional<GlobalType> globalTypeOpt = module.getGlobalType("const_i32");

        assertTrue(globalTypeOpt.isPresent(), "GlobalType should be present");
        final GlobalType globalType = globalTypeOpt.get();

        assertEquals(WasmValueType.I32, globalType.getValueType(), "Value type should be I32");
        assertFalse(globalType.isMutable(), "Global should be immutable");
        assertEquals(WasmTypeKind.GLOBAL, globalType.getKind(), "Kind should be GLOBAL");

        LOGGER.info("Immutable i32 global type parsed correctly from native");
      }
    }

    @Test
    @DisplayName("should get i64 global type from module")
    void shouldGetI64GlobalTypeFromModule() throws Exception {
      LOGGER.info("Testing Module.getGlobalType() for i64 global");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createMutableI64GlobalModule())) {

        final Optional<GlobalType> globalTypeOpt = module.getGlobalType("mutable_i64");

        assertTrue(globalTypeOpt.isPresent(), "GlobalType should be present");
        final GlobalType globalType = globalTypeOpt.get();

        assertEquals(WasmValueType.I64, globalType.getValueType(), "Value type should be I64");
        assertTrue(globalType.isMutable(), "Global should be mutable");

        LOGGER.info("I64 global type parsed correctly from native");
      }
    }

    @Test
    @DisplayName("should get f32 global type from module")
    void shouldGetF32GlobalTypeFromModule() throws Exception {
      LOGGER.info("Testing Module.getGlobalType() for f32 global");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createMutableF32GlobalModule())) {

        final Optional<GlobalType> globalTypeOpt = module.getGlobalType("mutable_f32");

        assertTrue(globalTypeOpt.isPresent(), "GlobalType should be present");
        final GlobalType globalType = globalTypeOpt.get();

        assertEquals(WasmValueType.F32, globalType.getValueType(), "Value type should be F32");
        assertTrue(globalType.isMutable(), "Global should be mutable");

        LOGGER.info("F32 global type parsed correctly from native");
      }
    }

    @Test
    @DisplayName("should get f64 global type from module")
    void shouldGetF64GlobalTypeFromModule() throws Exception {
      LOGGER.info("Testing Module.getGlobalType() for f64 global");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createMutableF64GlobalModule())) {

        final Optional<GlobalType> globalTypeOpt = module.getGlobalType("mutable_f64");

        assertTrue(globalTypeOpt.isPresent(), "GlobalType should be present");
        final GlobalType globalType = globalTypeOpt.get();

        assertEquals(WasmValueType.F64, globalType.getValueType(), "Value type should be F64");
        assertTrue(globalType.isMutable(), "Global should be mutable");

        LOGGER.info("F64 global type parsed correctly from native");
      }
    }

    @Test
    @DisplayName("should return empty for non-existent global")
    void shouldReturnEmptyForNonExistentGlobal() throws Exception {
      LOGGER.info("Testing Module.getGlobalType() for non-existent global");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createMutableI32GlobalModule())) {

        final Optional<GlobalType> globalTypeOpt = module.getGlobalType("nonexistent");

        assertFalse(globalTypeOpt.isPresent(), "GlobalType should not be present for non-existent");

        LOGGER.info("Non-existent global correctly returns empty");
      }
    }
  }

  @Nested
  @DisplayName("Instance.getGlobalType() Tests")
  class InstanceGetGlobalTypeTests {

    @Test
    @DisplayName("should get global type from instance")
    void shouldGetGlobalTypeFromInstance() throws Exception {
      LOGGER.info("Testing Instance.getGlobalType()");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Module module = engine.compileModule(createMutableI32GlobalModule());
        final Instance instance = store.createInstance(module);

        final Optional<GlobalType> globalTypeOpt = instance.getGlobalType("mutable_i32");

        assertTrue(globalTypeOpt.isPresent(), "GlobalType should be present");
        final GlobalType globalType = globalTypeOpt.get();

        assertEquals(WasmValueType.I32, globalType.getValueType(), "Value type should be I32");
        assertTrue(globalType.isMutable(), "Global should be mutable");

        LOGGER.info("Instance global type retrieved correctly from native");
      }
    }
  }

  @Nested
  @DisplayName("Module.getGlobalTypes() List Tests")
  class ModuleGetGlobalTypesListTests {

    @Test
    @DisplayName("should get all global types from module")
    void shouldGetAllGlobalTypesFromModule() throws Exception {
      LOGGER.info("Testing Module.getGlobalTypes() list");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createMultiGlobalModule())) {

        final List<GlobalType> globalTypes = module.getGlobalTypes();

        assertNotNull(globalTypes, "Global types list should not be null");
        assertEquals(4, globalTypes.size(), "Should have 4 global types");

        // Verify each global type from native
        // Note: Order depends on how globals are stored in the module
        int mutableI32Count = 0;
        int mutableI64Count = 0;
        int immutableI32Count = 0;
        int immutableI64Count = 0;

        for (final GlobalType gt : globalTypes) {
          assertNotNull(gt, "GlobalType in list should not be null");
          assertEquals(WasmTypeKind.GLOBAL, gt.getKind(), "Kind should be GLOBAL");

          switch (gt.getValueType()) {
            case I32:
              if (gt.isMutable()) {
                mutableI32Count++;
              } else {
                immutableI32Count++;
              }
              break;
            case I64:
              if (gt.isMutable()) {
                mutableI64Count++;
              } else {
                immutableI64Count++;
              }
              break;
            default:
              // Other types might appear based on module structure
              break;
          }
        }

        assertEquals(1, mutableI32Count, "Should have 1 mutable I32 global");
        assertEquals(1, mutableI64Count, "Should have 1 mutable I64 global");
        assertEquals(1, immutableI32Count, "Should have 1 immutable I32 global");
        assertEquals(1, immutableI64Count, "Should have 1 immutable I64 global");

        LOGGER.info("All global types parsed correctly from native list");
      }
    }
  }

  @Nested
  @DisplayName("GlobalType Consistency Tests")
  class GlobalTypeConsistencyTests {

    @Test
    @DisplayName("module and instance global types should match")
    void moduleAndInstanceGlobalTypesShouldMatch() throws Exception {
      LOGGER.info("Testing consistency between module and instance global types");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Module module = engine.compileModule(createMutableI32GlobalModule());
        final Instance instance = store.createInstance(module);

        final Optional<GlobalType> moduleGlobalType = module.getGlobalType("mutable_i32");
        final Optional<GlobalType> instanceGlobalType = instance.getGlobalType("mutable_i32");

        assertTrue(moduleGlobalType.isPresent(), "Module global type should be present");
        assertTrue(instanceGlobalType.isPresent(), "Instance global type should be present");

        assertEquals(
            moduleGlobalType.get().getValueType(),
            instanceGlobalType.get().getValueType(),
            "Value types should match");
        assertEquals(
            moduleGlobalType.get().isMutable(),
            instanceGlobalType.get().isMutable(),
            "Mutability should match");
        assertEquals(
            moduleGlobalType.get().getKind(),
            instanceGlobalType.get().getKind(),
            "Kinds should match");

        LOGGER.info("Module and instance global types are consistent");
      }
    }
  }
}
