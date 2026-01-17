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
 * Integration tests for FuncType.fromNative() functionality.
 *
 * <p>These tests verify that FuncType instances are correctly parsed from native handles when
 * retrieved from compiled WebAssembly modules. This exercises the fromNative() code path in both
 * JNI and Panama implementations.
 *
 * @since 1.0.0
 */
@DisplayName("FuncType fromNative Integration Tests")
public final class FuncTypeFromNativeTest {

  private static final Logger LOGGER = Logger.getLogger(FuncTypeFromNativeTest.class.getName());

  /** Helper method to create a FuncType for CodeBuilder. */
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
   * Creates a WebAssembly module with a function that takes no params and returns nothing.
   *
   * <pre>
   * (module
   *   (func (export "void_func")))
   * </pre>
   */
  private static byte[] createVoidFunctionModule() throws Exception {
    return new CodeBuilder()
        .addType(funcType(List.of(), List.of())) // type 0: () -> ()
        .addFunction(0, List.of(), new byte[] {}) // empty function body
        .addExport("void_func", CodeBuilder.ExportKind.FUNCTION, 0)
        .build();
  }

  /**
   * Creates a WebAssembly module with a function that takes one i32 param and returns i32.
   *
   * <pre>
   * (module
   *   (func (export "identity_i32") (param i32) (result i32)
   *     local.get 0))
   * </pre>
   */
  private static byte[] createIdentityI32Module() throws Exception {
    return new CodeBuilder()
        .addType(funcType(List.of(WasmValueType.I32), List.of(WasmValueType.I32)))
        .addFunction(0, List.of(), new byte[] {0x20, 0x00}) // local.get 0
        .addExport("identity_i32", CodeBuilder.ExportKind.FUNCTION, 0)
        .build();
  }

  /**
   * Creates a WebAssembly module with a function that takes two i32 params and returns i32.
   *
   * <pre>
   * (module
   *   (func (export "add") (param i32 i32) (result i32)
   *     local.get 0
   *     local.get 1
   *     i32.add))
   * </pre>
   */
  private static byte[] createAddModule() throws Exception {
    return new CodeBuilder()
        .addType(
            funcType(List.of(WasmValueType.I32, WasmValueType.I32), List.of(WasmValueType.I32)))
        .addFunction(0, List.of(), new byte[] {0x20, 0x00, 0x20, 0x01, 0x6a}) // add
        .addExport("add", CodeBuilder.ExportKind.FUNCTION, 0)
        .build();
  }

  /**
   * Creates a WebAssembly module with an i64 function.
   *
   * <pre>
   * (module
   *   (func (export "identity_i64") (param i64) (result i64)
   *     local.get 0))
   * </pre>
   */
  private static byte[] createIdentityI64Module() throws Exception {
    return new CodeBuilder()
        .addType(funcType(List.of(WasmValueType.I64), List.of(WasmValueType.I64)))
        .addFunction(0, List.of(), new byte[] {0x20, 0x00}) // local.get 0
        .addExport("identity_i64", CodeBuilder.ExportKind.FUNCTION, 0)
        .build();
  }

  /**
   * Creates a WebAssembly module with an f32 function.
   *
   * <pre>
   * (module
   *   (func (export "identity_f32") (param f32) (result f32)
   *     local.get 0))
   * </pre>
   */
  private static byte[] createIdentityF32Module() throws Exception {
    return new CodeBuilder()
        .addType(funcType(List.of(WasmValueType.F32), List.of(WasmValueType.F32)))
        .addFunction(0, List.of(), new byte[] {0x20, 0x00}) // local.get 0
        .addExport("identity_f32", CodeBuilder.ExportKind.FUNCTION, 0)
        .build();
  }

  /**
   * Creates a WebAssembly module with an f64 function.
   *
   * <pre>
   * (module
   *   (func (export "identity_f64") (param f64) (result f64)
   *     local.get 0))
   * </pre>
   */
  private static byte[] createIdentityF64Module() throws Exception {
    return new CodeBuilder()
        .addType(funcType(List.of(WasmValueType.F64), List.of(WasmValueType.F64)))
        .addFunction(0, List.of(), new byte[] {0x20, 0x00}) // local.get 0
        .addExport("identity_f64", CodeBuilder.ExportKind.FUNCTION, 0)
        .build();
  }

  /**
   * Creates a WebAssembly module with a multi-return function.
   *
   * <pre>
   * (module
   *   (func (export "multi_return") (param i32) (result i32 i32)
   *     local.get 0
   *     local.get 0))
   * </pre>
   */
  private static byte[] createMultiReturnModule() throws Exception {
    return new CodeBuilder()
        .addType(
            funcType(List.of(WasmValueType.I32), List.of(WasmValueType.I32, WasmValueType.I32)))
        .addFunction(0, List.of(), new byte[] {0x20, 0x00, 0x20, 0x00}) // local.get 0 twice
        .addExport("multi_return", CodeBuilder.ExportKind.FUNCTION, 0)
        .build();
  }

  /**
   * Creates a WebAssembly module with a mixed-type function.
   *
   * <pre>
   * (module
   *   (func (export "mixed") (param i32 i64 f32 f64) (result f64)
   *     local.get 3))
   * </pre>
   */
  private static byte[] createMixedTypesModule() throws Exception {
    return new CodeBuilder()
        .addType(
            funcType(
                List.of(WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64),
                List.of(WasmValueType.F64)))
        .addFunction(0, List.of(), new byte[] {0x20, 0x03}) // local.get 3
        .addExport("mixed", CodeBuilder.ExportKind.FUNCTION, 0)
        .build();
  }

  /**
   * Creates a WebAssembly module with multiple functions of different types.
   *
   * <pre>
   * (module
   *   (func (export "fn_void"))
   *   (func (export "fn_i32") (param i32) (result i32) local.get 0)
   *   (func (export "fn_i64") (param i64) (result i64) local.get 0))
   * </pre>
   */
  private static byte[] createMultiFunctionModule() throws Exception {
    return new CodeBuilder()
        .addType(funcType(List.of(), List.of())) // type 0: () -> ()
        .addType(funcType(List.of(WasmValueType.I32), List.of(WasmValueType.I32))) // type 1
        .addType(funcType(List.of(WasmValueType.I64), List.of(WasmValueType.I64))) // type 2
        .addFunction(0, List.of(), new byte[] {}) // void function
        .addFunction(1, List.of(), new byte[] {0x20, 0x00}) // i32 identity
        .addFunction(2, List.of(), new byte[] {0x20, 0x00}) // i64 identity
        .addExport("fn_void", CodeBuilder.ExportKind.FUNCTION, 0)
        .addExport("fn_i32", CodeBuilder.ExportKind.FUNCTION, 1)
        .addExport("fn_i64", CodeBuilder.ExportKind.FUNCTION, 2)
        .build();
  }

  @Nested
  @DisplayName("Module.getFunctionType() Tests")
  class ModuleGetFunctionTypeTests {

    @Test
    @DisplayName("should get void function type from module")
    void shouldGetVoidFunctionTypeFromModule() throws Exception {
      LOGGER.info("Testing Module.getFunctionType() for void function");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createVoidFunctionModule())) {

        final Optional<FuncType> funcTypeOpt = module.getFunctionType("void_func");

        assertTrue(funcTypeOpt.isPresent(), "FuncType should be present");
        final FuncType funcType = funcTypeOpt.get();

        assertNotNull(funcType, "FuncType should not be null");
        assertEquals(0, funcType.getParamCount(), "Should have 0 params");
        assertEquals(0, funcType.getResultCount(), "Should have 0 results");
        assertTrue(funcType.getParams().isEmpty(), "Params list should be empty");
        assertTrue(funcType.getResults().isEmpty(), "Results list should be empty");
        assertEquals(WasmTypeKind.FUNCTION, funcType.getKind(), "Kind should be FUNCTION");

        LOGGER.info("Void function type parsed correctly from native");
      }
    }

    @Test
    @DisplayName("should get i32 identity function type from module")
    void shouldGetI32IdentityFunctionTypeFromModule() throws Exception {
      LOGGER.info("Testing Module.getFunctionType() for i32 identity function");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createIdentityI32Module())) {

        final Optional<FuncType> funcTypeOpt = module.getFunctionType("identity_i32");

        assertTrue(funcTypeOpt.isPresent(), "FuncType should be present");
        final FuncType funcType = funcTypeOpt.get();

        assertEquals(1, funcType.getParamCount(), "Should have 1 param");
        assertEquals(1, funcType.getResultCount(), "Should have 1 result");
        assertEquals(WasmValueType.I32, funcType.getParams().get(0), "Param should be I32");
        assertEquals(WasmValueType.I32, funcType.getResults().get(0), "Result should be I32");

        LOGGER.info("I32 identity function type parsed correctly from native");
      }
    }

    @Test
    @DisplayName("should get add function type from module")
    void shouldGetAddFunctionTypeFromModule() throws Exception {
      LOGGER.info("Testing Module.getFunctionType() for add function");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createAddModule())) {

        final Optional<FuncType> funcTypeOpt = module.getFunctionType("add");

        assertTrue(funcTypeOpt.isPresent(), "FuncType should be present");
        final FuncType funcType = funcTypeOpt.get();

        assertEquals(2, funcType.getParamCount(), "Should have 2 params");
        assertEquals(1, funcType.getResultCount(), "Should have 1 result");
        assertEquals(WasmValueType.I32, funcType.getParams().get(0), "First param should be I32");
        assertEquals(WasmValueType.I32, funcType.getParams().get(1), "Second param should be I32");
        assertEquals(WasmValueType.I32, funcType.getResults().get(0), "Result should be I32");

        LOGGER.info("Add function type parsed correctly from native");
      }
    }

    @Test
    @DisplayName("should get i64 function type from module")
    void shouldGetI64FunctionTypeFromModule() throws Exception {
      LOGGER.info("Testing Module.getFunctionType() for i64 function");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createIdentityI64Module())) {

        final Optional<FuncType> funcTypeOpt = module.getFunctionType("identity_i64");

        assertTrue(funcTypeOpt.isPresent(), "FuncType should be present");
        final FuncType funcType = funcTypeOpt.get();

        assertEquals(WasmValueType.I64, funcType.getParams().get(0), "Param should be I64");
        assertEquals(WasmValueType.I64, funcType.getResults().get(0), "Result should be I64");

        LOGGER.info("I64 function type parsed correctly from native");
      }
    }

    @Test
    @DisplayName("should get f32 function type from module")
    void shouldGetF32FunctionTypeFromModule() throws Exception {
      LOGGER.info("Testing Module.getFunctionType() for f32 function");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createIdentityF32Module())) {

        final Optional<FuncType> funcTypeOpt = module.getFunctionType("identity_f32");

        assertTrue(funcTypeOpt.isPresent(), "FuncType should be present");
        final FuncType funcType = funcTypeOpt.get();

        assertEquals(WasmValueType.F32, funcType.getParams().get(0), "Param should be F32");
        assertEquals(WasmValueType.F32, funcType.getResults().get(0), "Result should be F32");

        LOGGER.info("F32 function type parsed correctly from native");
      }
    }

    @Test
    @DisplayName("should get f64 function type from module")
    void shouldGetF64FunctionTypeFromModule() throws Exception {
      LOGGER.info("Testing Module.getFunctionType() for f64 function");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createIdentityF64Module())) {

        final Optional<FuncType> funcTypeOpt = module.getFunctionType("identity_f64");

        assertTrue(funcTypeOpt.isPresent(), "FuncType should be present");
        final FuncType funcType = funcTypeOpt.get();

        assertEquals(WasmValueType.F64, funcType.getParams().get(0), "Param should be F64");
        assertEquals(WasmValueType.F64, funcType.getResults().get(0), "Result should be F64");

        LOGGER.info("F64 function type parsed correctly from native");
      }
    }

    @Test
    @DisplayName("should get multi-return function type from module")
    void shouldGetMultiReturnFunctionTypeFromModule() throws Exception {
      LOGGER.info("Testing Module.getFunctionType() for multi-return function");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createMultiReturnModule())) {

        final Optional<FuncType> funcTypeOpt = module.getFunctionType("multi_return");

        assertTrue(funcTypeOpt.isPresent(), "FuncType should be present");
        final FuncType funcType = funcTypeOpt.get();

        assertEquals(1, funcType.getParamCount(), "Should have 1 param");
        assertEquals(2, funcType.getResultCount(), "Should have 2 results");
        assertEquals(WasmValueType.I32, funcType.getParams().get(0), "Param should be I32");
        assertEquals(WasmValueType.I32, funcType.getResults().get(0), "First result should be I32");
        assertEquals(
            WasmValueType.I32, funcType.getResults().get(1), "Second result should be I32");

        LOGGER.info("Multi-return function type parsed correctly from native");
      }
    }

    @Test
    @DisplayName("should get mixed-type function type from module")
    void shouldGetMixedTypeFunctionTypeFromModule() throws Exception {
      LOGGER.info("Testing Module.getFunctionType() for mixed-type function");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createMixedTypesModule())) {

        final Optional<FuncType> funcTypeOpt = module.getFunctionType("mixed");

        assertTrue(funcTypeOpt.isPresent(), "FuncType should be present");
        final FuncType funcType = funcTypeOpt.get();

        assertEquals(4, funcType.getParamCount(), "Should have 4 params");
        assertEquals(1, funcType.getResultCount(), "Should have 1 result");

        final List<WasmValueType> params = funcType.getParams();
        assertEquals(WasmValueType.I32, params.get(0), "First param should be I32");
        assertEquals(WasmValueType.I64, params.get(1), "Second param should be I64");
        assertEquals(WasmValueType.F32, params.get(2), "Third param should be F32");
        assertEquals(WasmValueType.F64, params.get(3), "Fourth param should be F64");
        assertEquals(WasmValueType.F64, funcType.getResults().get(0), "Result should be F64");

        LOGGER.info("Mixed-type function type parsed correctly from native");
      }
    }

    @Test
    @DisplayName("should return empty for non-existent function")
    void shouldReturnEmptyForNonExistentFunction() throws Exception {
      LOGGER.info("Testing Module.getFunctionType() for non-existent function");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createVoidFunctionModule())) {

        final Optional<FuncType> funcTypeOpt = module.getFunctionType("nonexistent");

        assertFalse(funcTypeOpt.isPresent(), "FuncType should not be present for non-existent");

        LOGGER.info("Non-existent function correctly returns empty");
      }
    }
  }

  @Nested
  @DisplayName("Instance.getFunctionType() Tests")
  class InstanceGetFunctionTypeTests {

    @Test
    @DisplayName("should get function type from instance")
    void shouldGetFunctionTypeFromInstance() throws Exception {
      LOGGER.info("Testing Instance.getFunctionType()");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Module module = engine.compileModule(createAddModule());
        final Instance instance = store.createInstance(module);

        final Optional<FuncType> funcTypeOpt = instance.getFunctionType("add");

        assertTrue(funcTypeOpt.isPresent(), "FuncType should be present");
        final FuncType funcType = funcTypeOpt.get();

        assertEquals(2, funcType.getParamCount(), "Should have 2 params");
        assertEquals(1, funcType.getResultCount(), "Should have 1 result");
        assertEquals(WasmValueType.I32, funcType.getParams().get(0), "First param should be I32");
        assertEquals(WasmValueType.I32, funcType.getParams().get(1), "Second param should be I32");
        assertEquals(WasmValueType.I32, funcType.getResults().get(0), "Result should be I32");

        LOGGER.info("Instance function type retrieved correctly from native");
      }
    }
  }

  @Nested
  @DisplayName("Module.getFunctionTypes() List Tests")
  class ModuleGetFunctionTypesListTests {

    @Test
    @DisplayName("should get all function types from module")
    void shouldGetAllFunctionTypesFromModule() throws Exception {
      LOGGER.info("Testing Module.getFunctionTypes() list");

      try (final Engine engine = Engine.create();
          final Module module = engine.compileModule(createMultiFunctionModule())) {

        final List<FuncType> funcTypes = module.getFunctionTypes();

        assertNotNull(funcTypes, "Function types list should not be null");
        assertEquals(3, funcTypes.size(), "Should have 3 function types");

        // Verify each function type from native
        boolean foundVoid = false;
        boolean foundI32 = false;
        boolean foundI64 = false;

        for (final FuncType ft : funcTypes) {
          assertNotNull(ft, "FuncType in list should not be null");
          assertEquals(WasmTypeKind.FUNCTION, ft.getKind(), "Kind should be FUNCTION");

          if (ft.getParamCount() == 0 && ft.getResultCount() == 0) {
            foundVoid = true;
          } else if (ft.getParamCount() == 1 && ft.getResultCount() == 1) {
            if (ft.getParams().get(0) == WasmValueType.I32) {
              foundI32 = true;
            } else if (ft.getParams().get(0) == WasmValueType.I64) {
              foundI64 = true;
            }
          }
        }

        assertTrue(foundVoid, "Should have found void function type");
        assertTrue(foundI32, "Should have found I32 function type");
        assertTrue(foundI64, "Should have found I64 function type");

        LOGGER.info("All function types parsed correctly from native list");
      }
    }
  }

  @Nested
  @DisplayName("FuncType Consistency Tests")
  class FuncTypeConsistencyTests {

    @Test
    @DisplayName("module and instance function types should match")
    void moduleAndInstanceFunctionTypesShouldMatch() throws Exception {
      LOGGER.info("Testing consistency between module and instance function types");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {
        final Module module = engine.compileModule(createAddModule());
        final Instance instance = store.createInstance(module);

        final Optional<FuncType> moduleFuncType = module.getFunctionType("add");
        final Optional<FuncType> instanceFuncType = instance.getFunctionType("add");

        assertTrue(moduleFuncType.isPresent(), "Module function type should be present");
        assertTrue(instanceFuncType.isPresent(), "Instance function type should be present");

        assertEquals(
            moduleFuncType.get().getParamCount(),
            instanceFuncType.get().getParamCount(),
            "Param count should match");
        assertEquals(
            moduleFuncType.get().getResultCount(),
            instanceFuncType.get().getResultCount(),
            "Result count should match");
        assertEquals(
            moduleFuncType.get().getParams(),
            instanceFuncType.get().getParams(),
            "Params should match");
        assertEquals(
            moduleFuncType.get().getResults(),
            instanceFuncType.get().getResults(),
            "Results should match");
        assertEquals(
            moduleFuncType.get().getKind(), instanceFuncType.get().getKind(), "Kinds should match");

        LOGGER.info("Module and instance function types are consistent");
      }
    }
  }
}
