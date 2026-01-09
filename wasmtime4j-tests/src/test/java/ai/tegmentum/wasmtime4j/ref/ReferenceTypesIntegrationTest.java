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

package ai.tegmentum.wasmtime4j.ref;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.FunctionReference;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WebAssembly reference types functionality.
 *
 * <p>Reference types (funcref, externref) enable WebAssembly to work with references to host
 * objects and functions, enabling callbacks and dynamic dispatch patterns.
 *
 * @since 1.0.0
 */
@DisplayName("Reference Types Integration Tests")
public final class ReferenceTypesIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ReferenceTypesIntegrationTest.class.getName());

  private static boolean referenceTypesAvailable = false;

  /** Simple WebAssembly module that exports an add function. */
  private static final byte[] ADD_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x07, // type section
        0x01,
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x01,
        0x7F, // (i32, i32) -> i32
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x07,
        0x01,
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // export "add"
        0x0A,
        0x09,
        0x01,
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6A,
        0x0B // code
      };

  @BeforeAll
  static void checkReferenceTypesAvailable() {
    try {
      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.REFERENCE_TYPES);
      try (final Engine engine = Engine.create(config)) {
        referenceTypesAvailable = engine.supportsFeature(WasmFeature.REFERENCE_TYPES);
        LOGGER.info("Reference types available: " + referenceTypesAvailable);
      }
    } catch (final Exception e) {
      LOGGER.warning("Reference types not available: " + e.getMessage());
      referenceTypesAvailable = false;
    }
  }

  @Nested
  @DisplayName("HeapType Tests")
  class HeapTypeTests {

    @Test
    @DisplayName("should have none heap type")
    void shouldHaveNoneHeapType() {
      LOGGER.info("Testing none heap type");

      final HeapType noneType = HeapType.none();

      assertNotNull(noneType, "None heap type should not be null");
      assertTrue(noneType.isBottom(), "Should be bottom type");
      assertEquals("none", noneType.getTypeName());

      LOGGER.info("None heap type: " + noneType);
    }

    @Test
    @DisplayName("should have nofunc heap type")
    void shouldHaveNofuncHeapType() {
      LOGGER.info("Testing nofunc heap type");

      final HeapType nofuncType = HeapType.nofunc();

      assertNotNull(nofuncType, "Nofunc heap type should not be null");
      assertTrue(nofuncType.isBottom(), "Should be bottom type");
      assertEquals("nofunc", nofuncType.getTypeName());

      LOGGER.info("Nofunc heap type: " + nofuncType);
    }

    @Test
    @DisplayName("should have noextern heap type")
    void shouldHaveNoexternHeapType() {
      LOGGER.info("Testing noextern heap type");

      final HeapType noexternType = HeapType.noextern();

      assertNotNull(noexternType, "Noextern heap type should not be null");
      assertTrue(noexternType.isBottom(), "Should be bottom type");
      assertEquals("noextern", noexternType.getTypeName());

      LOGGER.info("Noextern heap type: " + noexternType);
    }

    @Test
    @DisplayName("should have funcref null heap type")
    void shouldHaveFuncrefNullHeapType() {
      LOGGER.info("Testing funcref null heap type");

      final FuncRef nullFuncRef = HeapType.funcNull();

      assertNotNull(nullFuncRef, "Funcref null should not be null");
      assertTrue(nullFuncRef.isNull(), "Should be null reference");
      assertEquals("funcref", nullFuncRef.getTypeName());

      LOGGER.info("Funcref null: " + nullFuncRef);
    }

    @Test
    @DisplayName("should have funcref value type")
    void shouldHaveFuncrefValueType() {
      LOGGER.info("Testing funcref value type");

      final FuncRef nullFuncRef = FuncRef.nullRef();

      assertEquals(WasmValueType.FUNCREF, nullFuncRef.getValueType());

      LOGGER.info("Funcref value type: " + nullFuncRef.getValueType());
    }

    @Test
    @DisplayName("should check subtype relationships with bottom types")
    void shouldCheckSubtypeRelationshipsWithBottomTypes() {
      LOGGER.info("Testing subtype relationships with bottom types");

      final HeapType none = HeapType.none();
      final HeapType nofunc = HeapType.nofunc();
      final HeapType noextern = HeapType.noextern();

      // Bottom types are subtypes of themselves
      assertTrue(none.isSubtypeOf(none), "none should be subtype of itself");
      assertTrue(nofunc.isSubtypeOf(nofunc), "nofunc should be subtype of itself");
      assertTrue(noextern.isSubtypeOf(noextern), "noextern should be subtype of itself");

      LOGGER.info("Subtype relationship checks passed");
    }
  }

  @Nested
  @DisplayName("FuncRef Tests")
  class FuncRefTests {

    @Test
    @DisplayName("should create null funcref")
    void shouldCreateNullFuncref() {
      LOGGER.info("Testing FuncRef null creation");

      final FuncRef nullRef = FuncRef.nullRef();

      assertNotNull(nullRef, "FuncRef object should not be null");
      assertTrue(nullRef.isNull(), "FuncRef should be null reference");

      LOGGER.info("FuncRef null creation passed");
    }

    @Test
    @DisplayName("should get value type from funcref")
    void shouldGetValueTypeFromFuncref() {
      LOGGER.info("Testing FuncRef value type");

      final FuncRef nullRef = FuncRef.nullRef();

      assertEquals(WasmValueType.FUNCREF, nullRef.getValueType(), "Should be FUNCREF type");

      LOGGER.info("FuncRef value type: " + nullRef.getValueType());
    }

    @Test
    @DisplayName("should get type name from funcref")
    void shouldGetTypeNameFromFuncref() {
      LOGGER.info("Testing FuncRef type name");

      final FuncRef nullRef = FuncRef.nullRef();

      assertEquals("funcref", nullRef.getTypeName());

      LOGGER.info("FuncRef type name: " + nullRef.getTypeName());
    }
  }

  @Nested
  @DisplayName("NoneRef Tests")
  class NoneRefTests {

    @Test
    @DisplayName("should create none ref")
    void shouldCreateNoneRef() {
      LOGGER.info("Testing NoneRef creation");

      final NoneRef noneRef = NoneRef.getInstance();

      assertNotNull(noneRef, "NoneRef should not be null");
      assertTrue(noneRef.isBottom(), "NoneRef should be bottom type");

      LOGGER.info("NoneRef created: " + noneRef);
    }

    @Test
    @DisplayName("should have none type name")
    void shouldHaveNoneTypeName() {
      LOGGER.info("Testing NoneRef type name");

      final NoneRef noneRef = NoneRef.getInstance();

      assertEquals("none", noneRef.getTypeName());

      LOGGER.info("NoneRef type name: " + noneRef.getTypeName());
    }

    @Test
    @DisplayName("should return same singleton instance")
    void shouldReturnSameSingletonInstance() {
      LOGGER.info("Testing NoneRef singleton");

      final NoneRef ref1 = NoneRef.getInstance();
      final NoneRef ref2 = NoneRef.getInstance();

      assertEquals(ref1, ref2, "Should be same singleton instance");

      LOGGER.info("NoneRef singleton verified");
    }
  }

  @Nested
  @DisplayName("NoExtern Tests")
  class NoExternTests {

    @Test
    @DisplayName("should create noextern ref")
    void shouldCreateNoexternRef() {
      LOGGER.info("Testing NoExtern creation");

      final NoExtern noExtern = NoExtern.getInstance();

      assertNotNull(noExtern, "NoExtern should not be null");
      assertTrue(noExtern.isBottom(), "NoExtern should be bottom type");

      LOGGER.info("NoExtern created: " + noExtern);
    }

    @Test
    @DisplayName("should have noextern type name")
    void shouldHaveNoexternTypeName() {
      LOGGER.info("Testing NoExtern type name");

      final NoExtern noExtern = NoExtern.getInstance();

      assertEquals("noextern", noExtern.getTypeName());

      LOGGER.info("NoExtern type name: " + noExtern.getTypeName());
    }

    @Test
    @DisplayName("should return same singleton instance")
    void shouldReturnSameSingletonInstance() {
      LOGGER.info("Testing NoExtern singleton");

      final NoExtern ref1 = NoExtern.getInstance();
      final NoExtern ref2 = NoExtern.getInstance();

      assertEquals(ref1, ref2, "Should be same singleton instance");

      LOGGER.info("NoExtern singleton verified");
    }
  }

  @Nested
  @DisplayName("NoFunc Tests")
  class NoFuncTests {

    @Test
    @DisplayName("should create nofunc ref")
    void shouldCreateNofuncRef() {
      LOGGER.info("Testing NoFunc creation");

      final NoFunc noFunc = NoFunc.getInstance();

      assertNotNull(noFunc, "NoFunc should not be null");
      assertTrue(noFunc.isBottom(), "NoFunc should be bottom type");

      LOGGER.info("NoFunc created: " + noFunc);
    }

    @Test
    @DisplayName("should have nofunc type name")
    void shouldHaveNofuncTypeName() {
      LOGGER.info("Testing NoFunc type name");

      final NoFunc noFunc = NoFunc.getInstance();

      assertEquals("nofunc", noFunc.getTypeName());

      LOGGER.info("NoFunc type name: " + noFunc.getTypeName());
    }

    @Test
    @DisplayName("should return same singleton instance")
    void shouldReturnSameSingletonInstance() {
      LOGGER.info("Testing NoFunc singleton");

      final NoFunc ref1 = NoFunc.getInstance();
      final NoFunc ref2 = NoFunc.getInstance();

      assertEquals(ref1, ref2, "Should be same singleton instance");

      LOGGER.info("NoFunc singleton verified");
    }
  }

  @Nested
  @DisplayName("FunctionReference Tests")
  class FunctionReferenceTests {

    @Test
    @DisplayName("should create function reference from host function")
    void shouldCreateFunctionReferenceFromHostFunction() throws Exception {
      assumeTrue(referenceTypesAvailable, "Reference types not available");

      LOGGER.info("Testing function reference creation from host function");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final HostFunction hostFunc =
            params -> new WasmValue[] {WasmValue.i32(params[0].asInt() * 2)};

        final FunctionType funcType =
            new FunctionType(
                new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

        final FunctionReference funcRef = store.createFunctionReference(hostFunc, funcType);

        assertNotNull(funcRef, "Function reference should not be null");
        assertTrue(funcRef.isValid(), "Function reference should be valid");
        assertNotNull(funcRef.getFunctionType(), "Function type should not be null");

        LOGGER.info("Created function reference: id=" + funcRef.getId());
      }
    }

    @Test
    @DisplayName("should call function reference")
    void shouldCallFunctionReference() throws Exception {
      assumeTrue(referenceTypesAvailable, "Reference types not available");

      LOGGER.info("Testing function reference call");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final HostFunction hostFunc =
            params -> new WasmValue[] {WasmValue.i32(params[0].asInt() + 100)};

        final FunctionType funcType =
            new FunctionType(
                new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

        final FunctionReference funcRef = store.createFunctionReference(hostFunc, funcType);
        final WasmValue[] result = funcRef.call(WasmValue.i32(42));

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.length, "Should have one result");
        assertEquals(142, result[0].asInt(), "Result should be 142");

        LOGGER.info("Function reference call result: " + result[0].asInt());
      }
    }

    @Test
    @DisplayName("should create function reference from WASM function")
    void shouldCreateFunctionReferenceFromWasmFunction() throws Exception {
      assumeTrue(referenceTypesAvailable, "Reference types not available");

      LOGGER.info("Testing function reference creation from WASM function");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore();
          final Module module = engine.compileModule(ADD_WASM);
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmFunction> addFunc = instance.getFunction("add");
        assertTrue(addFunc.isPresent(), "Should have add function");

        final FunctionReference funcRef = store.createFunctionReference(addFunc.get());

        assertNotNull(funcRef, "Function reference should not be null");
        assertTrue(funcRef.isValid(), "Function reference should be valid");

        LOGGER.info("Created function reference from WASM function");
      }
    }
  }

  @Nested
  @DisplayName("Reference Type Value Tests")
  class ReferenceTypeValueTests {

    @Test
    @DisplayName("should create funcref value")
    void shouldCreateFuncrefValue() throws Exception {
      assumeTrue(referenceTypesAvailable, "Reference types not available");

      LOGGER.info("Testing funcref value creation");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final HostFunction hostFunc = params -> new WasmValue[0];
        final FunctionType funcType =
            new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});

        final FunctionReference funcRef = store.createFunctionReference(hostFunc, funcType);
        final WasmValue refValue = WasmValue.funcref(funcRef);

        assertNotNull(refValue, "Funcref value should not be null");
        assertEquals(WasmValueType.FUNCREF, refValue.getType(), "Should be funcref type");

        LOGGER.info("Created funcref value: " + refValue);
      }
    }

    @Test
    @DisplayName("should create null funcref value")
    void shouldCreateNullFuncrefValue() {
      LOGGER.info("Testing null funcref value creation");

      final WasmValue nullFuncref = WasmValue.nullFuncref();

      assertNotNull(nullFuncref, "Null funcref should not be null");
      assertEquals(WasmValueType.FUNCREF, nullFuncref.getType());

      LOGGER.info("Created null funcref value");
    }

    @Test
    @DisplayName("should create null externref value")
    void shouldCreateNullExternrefValue() {
      LOGGER.info("Testing null externref value creation");

      final WasmValue nullExternref = WasmValue.nullExternref();

      assertNotNull(nullExternref, "Null externref should not be null");
      assertEquals(WasmValueType.EXTERNREF, nullExternref.getType());

      LOGGER.info("Created null externref value");
    }

    @Test
    @DisplayName("should create externref from Java object")
    void shouldCreateExternrefFromJavaObject() {
      LOGGER.info("Testing externref from Java object");

      final String testObject = "Hello, externref!";
      final WasmValue externref = WasmValue.externref(testObject);

      assertNotNull(externref, "Externref should not be null");
      assertEquals(WasmValueType.EXTERNREF, externref.getType());
      assertEquals(testObject, externref.getValue());

      LOGGER.info("Created externref from Java object: " + externref.getValue());
    }
  }

  @Nested
  @DisplayName("Reference Type Table Tests")
  class ReferenceTypeTableTests {

    @Test
    @DisplayName("should create funcref table")
    void shouldCreateFuncrefTable() throws Exception {
      assumeTrue(referenceTypesAvailable, "Reference types not available");

      LOGGER.info("Testing funcref table creation");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final var table = store.createTable(WasmValueType.FUNCREF, 10, 100);

        assertNotNull(table, "Funcref table should not be null");

        LOGGER.info("Created funcref table");
      }
    }

    @Test
    @DisplayName("should create externref table")
    void shouldCreateExternrefTable() throws Exception {
      assumeTrue(referenceTypesAvailable, "Reference types not available");

      LOGGER.info("Testing externref table creation");

      try (final Engine engine = Engine.create();
          final Store store = engine.createStore()) {

        final var table = store.createTable(WasmValueType.EXTERNREF, 10, 100);

        assertNotNull(table, "Externref table should not be null");

        LOGGER.info("Created externref table");
      }
    }
  }

  @Nested
  @DisplayName("Engine Feature Tests")
  class EngineFeatureTests {

    @Test
    @DisplayName("should enable reference types via config")
    void shouldEnableReferenceTypesViaConfig() throws Exception {
      LOGGER.info("Testing reference types enablement");

      final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.REFERENCE_TYPES);

      try (final Engine engine = Engine.create(config)) {
        assertTrue(engine.isValid(), "Engine should be valid");
        LOGGER.info(
            "Reference types enabled: " + engine.supportsFeature(WasmFeature.REFERENCE_TYPES));
      }
    }

    @Test
    @DisplayName("should enable function references via config")
    void shouldEnableFunctionReferencesViaConfig() throws Exception {
      LOGGER.info("Testing function references enablement");

      final EngineConfig config =
          new EngineConfig().addWasmFeature(WasmFeature.TYPED_FUNCTION_REFERENCES);

      try (final Engine engine = Engine.create(config)) {
        assertTrue(engine.isValid(), "Engine should be valid");
        LOGGER.info("Function references config applied");
      }
    }
  }

  @Nested
  @DisplayName("HeapType Singleton Tests")
  class HeapTypeSingletonTests {

    @Test
    @DisplayName("should return same none heap type instance")
    void shouldReturnSameNoneHeapTypeInstance() {
      LOGGER.info("Testing HeapType.none() singleton");

      final HeapType type1 = HeapType.none();
      final HeapType type2 = HeapType.none();

      assertEquals(type1, type2, "Should return same singleton");
      assertTrue(type1 == type2, "Should be same reference");

      LOGGER.info("HeapType.none() singleton verified");
    }

    @Test
    @DisplayName("should return same nofunc heap type instance")
    void shouldReturnSameNofuncHeapTypeInstance() {
      LOGGER.info("Testing HeapType.nofunc() singleton");

      final HeapType type1 = HeapType.nofunc();
      final HeapType type2 = HeapType.nofunc();

      assertEquals(type1, type2, "Should return same singleton");
      assertTrue(type1 == type2, "Should be same reference");

      LOGGER.info("HeapType.nofunc() singleton verified");
    }

    @Test
    @DisplayName("should return same noextern heap type instance")
    void shouldReturnSameNoexternHeapTypeInstance() {
      LOGGER.info("Testing HeapType.noextern() singleton");

      final HeapType type1 = HeapType.noextern();
      final HeapType type2 = HeapType.noextern();

      assertEquals(type1, type2, "Should return same singleton");
      assertTrue(type1 == type2, "Should be same reference");

      LOGGER.info("HeapType.noextern() singleton verified");
    }
  }
}
