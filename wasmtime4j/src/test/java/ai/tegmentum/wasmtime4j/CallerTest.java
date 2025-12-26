/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the Caller interface.
 *
 * <p>The Caller interface provides access to the calling WebAssembly instance context within host
 * functions, allowing host functions to interact with the WebAssembly module's state and resources.
 */
@DisplayName("Caller Interface Tests")
class CallerTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Caller.class.isInterface(), "Caller should be an interface");
    }

    @Test
    @DisplayName("should have generic type parameter T")
    void shouldHaveGenericTypeParameter() {
      TypeVariable<?>[] typeParams = Caller.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Caller should have exactly one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          java.lang.reflect.Modifier.isPublic(Caller.class.getModifiers()),
          "Caller should be public");
    }
  }

  // ========================================================================
  // Data Access Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Data Access Method Tests")
  class DataAccessMethodTests {

    @Test
    @DisplayName("should have data method returning generic type T")
    void shouldHaveDataMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("data");
      assertNotNull(method, "data method should exist");
      Type returnType = method.getGenericReturnType();
      assertTrue(
          returnType instanceof TypeVariable, "Return type should be generic type variable T");
      assertEquals("T", ((TypeVariable<?>) returnType).getName(), "Return type should be T");
    }
  }

  // ========================================================================
  // Export Access Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Export Access Method Tests")
  class ExportAccessMethodTests {

    @Test
    @DisplayName("should have getExport method")
    void shouldHaveGetExportMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("getExport", String.class);
      assertNotNull(method, "getExport method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have getFunction method")
    void shouldHaveGetFunctionMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("getFunction", String.class);
      assertNotNull(method, "getFunction method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have getMemory method with name parameter")
    void shouldHaveGetMemoryMethodWithName() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("getMemory", String.class);
      assertNotNull(method, "getMemory(String) method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have default getMemory method without parameters")
    void shouldHaveDefaultGetMemoryMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("getMemory");
      assertNotNull(method, "getMemory() method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
      assertTrue(method.isDefault(), "getMemory() should be a default method");
    }

    @Test
    @DisplayName("should have getTable method")
    void shouldHaveGetTableMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("getTable", String.class);
      assertNotNull(method, "getTable method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have getGlobal method")
    void shouldHaveGetGlobalMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("getGlobal", String.class);
      assertNotNull(method, "getGlobal method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have hasExport method")
    void shouldHaveHasExportMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("hasExport", String.class);
      assertNotNull(method, "hasExport method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getExportByModuleExport method")
    void shouldHaveGetExportByModuleExportMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("getExportByModuleExport", ModuleExport.class);
      assertNotNull(method, "getExportByModuleExport method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }
  }

  // ========================================================================
  // Fuel Management Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Fuel Management Method Tests")
  class FuelManagementMethodTests {

    @Test
    @DisplayName("should have fuelConsumed method")
    void shouldHaveFuelConsumedMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("fuelConsumed");
      assertNotNull(method, "fuelConsumed method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have fuelRemaining method")
    void shouldHaveFuelRemainingMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("fuelRemaining");
      assertNotNull(method, "fuelRemaining method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have addFuel method")
    void shouldHaveAddFuelMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("addFuel", long.class);
      assertNotNull(method, "addFuel method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have fuelAsyncYieldInterval method")
    void shouldHaveFuelAsyncYieldIntervalMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("fuelAsyncYieldInterval");
      assertNotNull(method, "fuelAsyncYieldInterval method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have setFuelAsyncYieldInterval method")
    void shouldHaveSetFuelAsyncYieldIntervalMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("setFuelAsyncYieldInterval", long.class);
      assertNotNull(method, "setFuelAsyncYieldInterval method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // Epoch Deadline Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Epoch Deadline Method Tests")
  class EpochDeadlineMethodTests {

    @Test
    @DisplayName("should have hasEpochDeadline method")
    void shouldHaveHasEpochDeadlineMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("hasEpochDeadline");
      assertNotNull(method, "hasEpochDeadline method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have epochDeadline method")
    void shouldHaveEpochDeadlineMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("epochDeadline");
      assertNotNull(method, "epochDeadline method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }

    @Test
    @DisplayName("should have setEpochDeadline method")
    void shouldHaveSetEpochDeadlineMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("setEpochDeadline", long.class);
      assertNotNull(method, "setEpochDeadline method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // Engine and GC Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Engine and GC Method Tests")
  class EngineAndGcMethodTests {

    @Test
    @DisplayName("should have engine method")
    void shouldHaveEngineMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("engine");
      assertNotNull(method, "engine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "Return type should be Engine");
    }

    @Test
    @DisplayName("should have gc method")
    void shouldHaveGcMethod() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("gc");
      assertNotNull(method, "gc method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // Method Count and Completeness Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count and Completeness Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "data",
              "getExport",
              "getFunction",
              "getMemory",
              "getTable",
              "getGlobal",
              "hasExport",
              "fuelConsumed",
              "fuelRemaining",
              "addFuel",
              "hasEpochDeadline",
              "epochDeadline",
              "setEpochDeadline",
              "getExportByModuleExport",
              "engine",
              "gc",
              "fuelAsyncYieldInterval",
              "setFuelAsyncYieldInterval");

      Set<String> actualMethods =
          Arrays.stream(Caller.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Caller should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have reasonable method count")
    void shouldHaveReasonableMethodCount() {
      int methodCount = Caller.class.getDeclaredMethods().length;
      assertTrue(
          methodCount >= 15, "Caller should have at least 15 methods, found: " + methodCount);
    }
  }

  // ========================================================================
  // Exception Declaration Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("addFuel should declare WasmException")
    void addFuelShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("addFuel", long.class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptions).contains(ai.tegmentum.wasmtime4j.exception.WasmException.class),
          "addFuel should declare WasmException");
    }

    @Test
    @DisplayName("setEpochDeadline should declare WasmException")
    void setEpochDeadlineShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("setEpochDeadline", long.class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptions).contains(ai.tegmentum.wasmtime4j.exception.WasmException.class),
          "setEpochDeadline should declare WasmException");
    }

    @Test
    @DisplayName("gc should declare WasmException")
    void gcShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = Caller.class.getMethod("gc");
      Class<?>[] exceptions = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptions).contains(ai.tegmentum.wasmtime4j.exception.WasmException.class),
          "gc should declare WasmException");
    }
  }
}
