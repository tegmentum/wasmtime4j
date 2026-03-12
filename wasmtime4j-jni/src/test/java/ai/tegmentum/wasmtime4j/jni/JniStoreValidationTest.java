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
package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive validation tests for JniStore.
 *
 * <p>These tests exercise real pre-native-call validation code paths in JniStore: null checks,
 * negative value rejection, type mismatch detection, and accessor behavior. All validation happens
 * in Java before any native call is made.
 *
 * <p>These tests use fake handles (which never reach native code) because the validation logic
 * under test runs entirely in Java. This is intentional -- the goal is to verify that invalid
 * inputs are rejected before crossing the JNI boundary.
 */
@DisplayName("JniStore Validation Tests")
final class JniStoreValidationTest {

  private static final long VALID_HANDLE = 0x12345678L;

  private JniEngine testEngine;
  private JniStore testStore;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testStore = new JniStore(VALID_HANDLE, testEngine);
  }

  @AfterEach
  void tearDown() {
    testStore.markClosedForTesting();
    testEngine.markClosedForTesting();
  }

  @Nested
  @DisplayName("Constructor Validation")
  class ConstructorValidation {

    @Test
    @DisplayName("forModule should reject null module")
    void forModuleShouldRejectNullModule() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> JniStore.forModule(null));
      assertTrue(
          e.getMessage().contains("module cannot be null"),
          "Expected message to contain: module cannot be null");
    }

    @Test
    @DisplayName("forModule should reject non-JniModule instance")
    void forModuleShouldRejectNonJniModule() {
      ai.tegmentum.wasmtime4j.Module nonJniModule = new NonJniModuleStub();
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> JniStore.forModule(nonJniModule));
      assertTrue(
          e.getMessage().contains("module must be a JniModule instance"),
          "Expected message to contain: module must be a JniModule instance");
    }
  }

  @Nested
  @DisplayName("Fuel Validation")
  class FuelValidation {

    @Test
    @DisplayName("addFuel should reject negative value")
    void addFuelShouldRejectNegativeValue() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testStore.addFuel(-1));
      assertTrue(
          e.getMessage().contains("additionalFuel"), "Expected message to contain: additionalFuel");
      assertTrue(
          e.getMessage().contains("non-negative"), "Expected message to contain: non-negative");
    }

    @Test
    @DisplayName("addFuel should reject large negative value")
    void addFuelShouldRejectLargeNegativeValue() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testStore.addFuel(Long.MIN_VALUE));
      assertTrue(
          e.getMessage().contains("additionalFuel"), "Expected message to contain: additionalFuel");
      assertTrue(
          e.getMessage().contains("non-negative"), "Expected message to contain: non-negative");
    }

    @Test
    @DisplayName("setFuel should reject negative value")
    void setFuelShouldRejectNegativeValue() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testStore.setFuel(-1));
      assertTrue(e.getMessage().contains("fuel"), "Expected message to contain: fuel");
      assertTrue(
          e.getMessage().contains("non-negative"), "Expected message to contain: non-negative");
    }

    @Test
    @DisplayName("setFuel should reject large negative value")
    void setFuelShouldRejectLargeNegativeValue() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testStore.setFuel(Long.MIN_VALUE));
      assertTrue(e.getMessage().contains("fuel"), "Expected message to contain: fuel");
      assertTrue(
          e.getMessage().contains("non-negative"), "Expected message to contain: non-negative");
    }

    @Test
    @DisplayName("consumeFuel should reject negative value")
    void consumeFuelShouldRejectNegativeValue() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testStore.consumeFuel(-1));
      assertTrue(e.getMessage().contains("fuel"), "Expected message to contain: fuel");
      assertTrue(
          e.getMessage().contains("non-negative"), "Expected message to contain: non-negative");
    }

    @Test
    @DisplayName("consumeFuel should reject large negative value")
    void consumeFuelShouldRejectLargeNegativeValue() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testStore.consumeFuel(Long.MIN_VALUE));
      assertTrue(e.getMessage().contains("fuel"), "Expected message to contain: fuel");
      assertTrue(
          e.getMessage().contains("non-negative"), "Expected message to contain: non-negative");
    }

    @Test
    @DisplayName("setHostcallFuel should reject negative value")
    void setHostcallFuelShouldRejectNegativeValue() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testStore.setHostcallFuel(-1));
      assertTrue(e.getMessage().contains("fuel"), "Expected message to contain: fuel");
      assertTrue(
          e.getMessage().contains("non-negative"), "Expected message to contain: non-negative");
    }

    @Test
    @DisplayName("setHostcallFuel should reject large negative value")
    void setHostcallFuelShouldRejectLargeNegativeValue() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> testStore.setHostcallFuel(Long.MIN_VALUE));
      assertTrue(e.getMessage().contains("fuel"), "Expected message to contain: fuel");
      assertTrue(
          e.getMessage().contains("non-negative"), "Expected message to contain: non-negative");
    }
  }

  @Nested
  @DisplayName("Resource Limiter Validation")
  class ResourceLimiterValidation {

    @Test
    @DisplayName("setResourceLimiter should reject null limiter")
    void setResourceLimiterShouldRejectNull() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testStore.setResourceLimiter(null));
      assertTrue(
          e.getMessage().contains("ResourceLimiter cannot be null"),
          "Expected message to contain: ResourceLimiter cannot be null");
    }

    @Test
    @DisplayName("setResourceLimiterAsync should reject null limiter")
    void setResourceLimiterAsyncShouldRejectNull() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class, () -> testStore.setResourceLimiterAsync(null));
      assertTrue(
          e.getMessage().contains("ResourceLimiterAsync cannot be null"),
          "Expected message to contain: ResourceLimiterAsync cannot be null");
    }
  }

  @Nested
  @DisplayName("Host Function Validation")
  class HostFunctionValidation {

    @Test
    @DisplayName("createHostFunction should reject null name")
    void createHostFunctionShouldRejectNullName() {
      FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
      NullPointerException e =
          assertThrows(
              NullPointerException.class,
              () -> testStore.createHostFunction(null, funcType, args -> args));
      assertTrue(
          e.getMessage().contains("name cannot be null"),
          "Expected message to contain: name cannot be null");
    }

    @Test
    @DisplayName("createHostFunction should reject null functionType")
    void createHostFunctionShouldRejectNullFunctionType() {
      NullPointerException e =
          assertThrows(
              NullPointerException.class,
              () -> testStore.createHostFunction("test", null, args -> args));
      assertTrue(
          e.getMessage().contains("functionType cannot be null"),
          "Expected message to contain: functionType cannot be null");
    }

    @Test
    @DisplayName("createHostFunction should reject null implementation")
    void createHostFunctionShouldRejectNullImplementation() {
      FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
      NullPointerException e =
          assertThrows(
              NullPointerException.class,
              () -> testStore.createHostFunction("test", funcType, null));
      assertTrue(
          e.getMessage().contains("implementation cannot be null"),
          "Expected message to contain: implementation cannot be null");
    }

    @Test
    @DisplayName("createHostFunctionUnchecked should reject null name")
    void createHostFunctionUncheckedShouldRejectNullName() {
      FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
      NullPointerException e =
          assertThrows(
              NullPointerException.class,
              () -> testStore.createHostFunctionUnchecked(null, funcType, args -> args));
      assertTrue(
          e.getMessage().contains("name cannot be null"),
          "Expected message to contain: name cannot be null");
    }

    @Test
    @DisplayName("createHostFunctionUnchecked should reject null functionType")
    void createHostFunctionUncheckedShouldRejectNullFunctionType() {
      NullPointerException e =
          assertThrows(
              NullPointerException.class,
              () -> testStore.createHostFunctionUnchecked("test", null, args -> args));
      assertTrue(
          e.getMessage().contains("functionType cannot be null"),
          "Expected message to contain: functionType cannot be null");
    }

    @Test
    @DisplayName("createHostFunctionUnchecked should reject null implementation")
    void createHostFunctionUncheckedShouldRejectNullImplementation() {
      FunctionType funcType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
      NullPointerException e =
          assertThrows(
              NullPointerException.class,
              () -> testStore.createHostFunctionUnchecked("test", funcType, null));
      assertTrue(
          e.getMessage().contains("implementation cannot be null"),
          "Expected message to contain: implementation cannot be null");
    }
  }

  @Nested
  @DisplayName("Memory, Table, and Global Validation")
  class MemoryTableGlobalValidation {

    @Test
    @DisplayName("createMemory should reject negative initial pages")
    void createMemoryShouldRejectNegativeInitialPages() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testStore.createMemory(-1, 10));
      assertTrue(
          e.getMessage().contains("Initial pages"), "Expected message to contain: Initial pages");
      assertTrue(e.getMessage().contains("negative"), "Expected message to contain: negative");
    }

    @Test
    @DisplayName("createMemory should reject max pages less than initial pages")
    void createMemoryShouldRejectMaxLessThanInitial() {
      IllegalArgumentException e =
          assertThrows(IllegalArgumentException.class, () -> testStore.createMemory(10, 5));
      assertTrue(e.getMessage().contains("Max pages"), "Expected message to contain: Max pages");
      assertTrue(
          e.getMessage().contains("cannot be less than initial pages"),
          "Expected message to contain: cannot be less than initial pages");
    }

    @Test
    @DisplayName("createTable should reject non-reference element type")
    void createTableShouldRejectNonReferenceType() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> testStore.createTable(WasmValueType.I32, 10, 20));
      assertTrue(
          e.getMessage().contains("Element type"), "Expected message to contain: Element type");
      assertTrue(
          e.getMessage().contains("must be FUNCREF or EXTERNREF"),
          "Expected message to contain: must be FUNCREF or EXTERNREF");
    }

    @Test
    @DisplayName("createTable should reject I64 element type")
    void createTableShouldRejectI64Type() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> testStore.createTable(WasmValueType.I64, 5, 10));
      assertTrue(
          e.getMessage().contains("Element type"), "Expected message to contain: Element type");
      assertTrue(
          e.getMessage().contains("must be FUNCREF or EXTERNREF"),
          "Expected message to contain: must be FUNCREF or EXTERNREF");
    }

    @Test
    @DisplayName("createTable should reject F32 element type")
    void createTableShouldRejectF32Type() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> testStore.createTable(WasmValueType.F32, 5, 10));
      assertTrue(
          e.getMessage().contains("Element type"), "Expected message to contain: Element type");
      assertTrue(
          e.getMessage().contains("must be FUNCREF or EXTERNREF"),
          "Expected message to contain: must be FUNCREF or EXTERNREF");
    }

    @Test
    @DisplayName("createGlobal should reject mismatched value type")
    void createGlobalShouldRejectMismatchedType() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> testStore.createGlobal(WasmValueType.I64, false, WasmValue.i32(42)));
      assertTrue(
          e.getMessage().contains("Initial value type"),
          "Expected message to contain: Initial value type");
      assertTrue(
          e.getMessage().contains("does not match"), "Expected message to contain: does not match");
    }

    @Test
    @DisplayName("createGlobal should reject F64 value for I32 type")
    void createGlobalShouldRejectF64ForI32() {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> testStore.createGlobal(WasmValueType.I32, true, WasmValue.f64(3.14)));
      assertTrue(
          e.getMessage().contains("Initial value type"),
          "Expected message to contain: Initial value type");
      assertTrue(
          e.getMessage().contains("does not match"), "Expected message to contain: does not match");
    }
  }

  @Nested
  @DisplayName("Data Management")
  class DataManagement {

    @Test
    @DisplayName("getData should return null when no data set")
    void getDataShouldReturnNullInitially() {
      assertNull(testStore.getData(), "Expected getData to return null when no data has been set");
    }

    @Test
    @DisplayName("setData and getData should round-trip custom data")
    void setDataAndGetDataShouldRoundTrip() {
      String testData = "hello world";
      testStore.setData(testData);
      assertEquals(testData, testStore.getData(), "Expected getData to return the value set");
    }

    @Test
    @DisplayName("setData should allow null to clear data")
    void setDataShouldAllowNull() {
      testStore.setData("something");
      testStore.setData(null);
      assertNull(testStore.getData(), "Expected getData to return null after setting null");
    }

    @Test
    @DisplayName("setData and getData should handle complex objects")
    void setDataShouldHandleComplexObjects() {
      Object complexData = Arrays.asList("a", "b", "c");
      testStore.setData(complexData);
      assertSame(
          complexData,
          testStore.getData(),
          "Expected getData to return the exact same object reference");
    }

    @Test
    @DisplayName("getEngine should return the engine passed to constructor")
    void getEngineShouldReturnConstructorEngine() {
      assertSame(
          testEngine,
          testStore.getEngine(),
          "Expected getEngine to return the engine passed to constructor");
    }

    @Test
    @DisplayName("getResourceType should return Store")
    void getResourceTypeShouldReturnStore() {
      assertEquals(
          "Store", testStore.getResourceType(), "Expected getResourceType to return 'Store'");
    }

    @Test
    @DisplayName("getWasiContext should return empty Optional when no context set")
    void getWasiContextShouldReturnEmptyOptional() {
      assertNotNull(testStore.getWasiContext(), "Expected getWasiContext to return non-null");
      assertFalse(
          testStore.getWasiContext().isPresent(),
          "Expected getWasiContext to return empty Optional when no context is set");
    }
  }

  /**
   * Minimal stub implementation of Module that is NOT a JniModule, used to test the forModule
   * type-check validation path.
   */
  private static final class NonJniModuleStub implements ai.tegmentum.wasmtime4j.Module {

    @Override
    public ai.tegmentum.wasmtime4j.Instance instantiate(final ai.tegmentum.wasmtime4j.Store store) {
      throw new UnsupportedOperationException("stub");
    }

    @Override
    public ai.tegmentum.wasmtime4j.Instance instantiate(
        final ai.tegmentum.wasmtime4j.Store store,
        final ai.tegmentum.wasmtime4j.validation.ImportMap imports) {
      throw new UnsupportedOperationException("stub");
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.type.ExportType> getExports() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.type.ImportType> getImports() {
      return java.util.Collections.emptyList();
    }

    @Override
    public boolean hasExport(final String name) {
      return false;
    }

    @Override
    public boolean hasImport(final String moduleName, final String fieldName) {
      return false;
    }

    @Override
    public ai.tegmentum.wasmtime4j.Engine getEngine() {
      return null;
    }

    @Override
    public boolean validateImports(final ai.tegmentum.wasmtime4j.validation.ImportMap imports) {
      return false;
    }

    @Override
    public ai.tegmentum.wasmtime4j.validation.ImportValidation validateImportsDetailed(
        final ai.tegmentum.wasmtime4j.validation.ImportMap imports) {
      throw new UnsupportedOperationException("stub");
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public byte[] text() {
      return new byte[0];
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.Module.AddressMapping> addressMap() {
      return java.util.Collections.emptyList();
    }

    @Override
    public ai.tegmentum.wasmtime4j.ImageRange imageRange() {
      throw new UnsupportedOperationException("stub");
    }

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public void close() {}

    @Override
    public boolean same(final ai.tegmentum.wasmtime4j.Module other) {
      return false;
    }

    @Override
    public int getExportIndex(final String name) {
      return -1;
    }

    @Override
    public java.util.Optional<ai.tegmentum.wasmtime4j.ModuleExport> getModuleExport(
        final String name) {
      return java.util.Optional.empty();
    }

    @Override
    public byte[] serialize() {
      return new byte[0];
    }
  }
}
