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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CompiledModule interface.
 *
 * <p>CompiledModule provides low-level access to compiled WebAssembly module data. This test
 * verifies the interface structure, nested interfaces, and API conformance.
 */
@DisplayName("CompiledModule Interface Tests")
class CompiledModuleTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CompiledModule.class.isInterface(), "CompiledModule should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CompiledModule.class.getModifiers()),
          "CompiledModule should be public");
    }
  }

  // ========================================================================
  // Size Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Size Method Tests")
  class SizeMethodTests {

    @Test
    @DisplayName("should have getCodeSize method")
    void shouldHaveGetCodeSizeMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.class.getMethod("getCodeSize");
      assertNotNull(method, "getCodeSize method should exist");
      assertEquals(long.class, method.getReturnType(), "getCodeSize should return long");
    }

    @Test
    @DisplayName("should have getReadOnlyDataSize method")
    void shouldHaveGetReadOnlyDataSizeMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.class.getMethod("getReadOnlyDataSize");
      assertNotNull(method, "getReadOnlyDataSize method should exist");
      assertEquals(long.class, method.getReturnType(), "getReadOnlyDataSize should return long");
    }

    @Test
    @DisplayName("should have getFunctionCount method")
    void shouldHaveGetFunctionCountMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.class.getMethod("getFunctionCount");
      assertNotNull(method, "getFunctionCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getFunctionCount should return int");
    }
  }

  // ========================================================================
  // Serialization Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Serialization Method Tests")
  class SerializationMethodTests {

    @Test
    @DisplayName("should have serialize method")
    void shouldHaveSerializeMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.class.getMethod("serialize");
      assertNotNull(method, "serialize method should exist");
      assertEquals(byte[].class, method.getReturnType(), "serialize should return byte[]");
    }

    @Test
    @DisplayName("serialize should declare WasmException")
    void serializeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = CompiledModule.class.getMethod("serialize");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(
          Arrays.asList(exceptionTypes).contains(WasmException.class),
          "serialize should declare WasmException");
    }
  }

  // ========================================================================
  // Function Access Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Function Access Method Tests")
  class FunctionAccessMethodTests {

    @Test
    @DisplayName("should have getFunction method with int parameter")
    void shouldHaveGetFunctionMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.class.getMethod("getFunction", int.class);
      assertNotNull(method, "getFunction method should exist");
      assertEquals(
          CompiledModule.CompiledFunction.class,
          method.getReturnType(),
          "getFunction should return CompiledFunction");
    }

    @Test
    @DisplayName("should have getFunctions method")
    void shouldHaveGetFunctionsMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.class.getMethod("getFunctions");
      assertNotNull(method, "getFunctions method should exist");
      assertEquals(List.class, method.getReturnType(), "getFunctions should return List");
    }
  }

  // ========================================================================
  // Memory and Code Range Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Memory and Code Range Method Tests")
  class MemoryAndCodeRangeMethodTests {

    @Test
    @DisplayName("should have getMemoryImage method")
    void shouldHaveGetMemoryImageMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.class.getMethod("getMemoryImage");
      assertNotNull(method, "getMemoryImage method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getMemoryImage should return Optional");
    }

    @Test
    @DisplayName("should have getCodeRange method")
    void shouldHaveGetCodeRangeMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.class.getMethod("getCodeRange");
      assertNotNull(method, "getCodeRange method should exist");
      assertEquals(
          CompiledModule.AddressRange.class,
          method.getReturnType(),
          "getCodeRange should return AddressRange");
    }
  }

  // ========================================================================
  // Metadata Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Metadata Method Tests")
  class MetadataMethodTests {

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
      assertEquals(
          CompiledModule.CompilationMetadata.class,
          method.getReturnType(),
          "getMetadata should return CompilationMetadata");
    }
  }

  // ========================================================================
  // CompiledFunction Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("CompiledFunction Nested Interface Tests")
  class CompiledFunctionTests {

    @Test
    @DisplayName("CompiledFunction should be a nested interface")
    void compiledFunctionShouldBeNestedInterface() {
      Class<?>[] declaredClasses = CompiledModule.class.getDeclaredClasses();
      boolean found =
          Arrays.stream(declaredClasses)
              .anyMatch(c -> c.getSimpleName().equals("CompiledFunction") && c.isInterface());
      assertTrue(found, "CompiledFunction should be a nested interface");
    }

    @Test
    @DisplayName("CompiledFunction should have getIndex method")
    void compiledFunctionShouldHaveGetIndexMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.CompiledFunction.class.getMethod("getIndex");
      assertNotNull(method, "getIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "getIndex should return int");
    }

    @Test
    @DisplayName("CompiledFunction should have getName method")
    void compiledFunctionShouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.CompiledFunction.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getName should return Optional");
    }

    @Test
    @DisplayName("CompiledFunction should have getCodeSize method")
    void compiledFunctionShouldHaveGetCodeSizeMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.CompiledFunction.class.getMethod("getCodeSize");
      assertNotNull(method, "getCodeSize method should exist");
      assertEquals(long.class, method.getReturnType(), "getCodeSize should return long");
    }

    @Test
    @DisplayName("CompiledFunction should have getStartAddress method")
    void compiledFunctionShouldHaveGetStartAddressMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.CompiledFunction.class.getMethod("getStartAddress");
      assertNotNull(method, "getStartAddress method should exist");
      assertEquals(long.class, method.getReturnType(), "getStartAddress should return long");
    }

    @Test
    @DisplayName("CompiledFunction should have getEndAddress method")
    void compiledFunctionShouldHaveGetEndAddressMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.CompiledFunction.class.getMethod("getEndAddress");
      assertNotNull(method, "getEndAddress method should exist");
      assertEquals(long.class, method.getReturnType(), "getEndAddress should return long");
    }

    @Test
    @DisplayName("CompiledFunction should have getStackSlots method")
    void compiledFunctionShouldHaveGetStackSlotsMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.CompiledFunction.class.getMethod("getStackSlots");
      assertNotNull(method, "getStackSlots method should exist");
      assertEquals(int.class, method.getReturnType(), "getStackSlots should return int");
    }
  }

  // ========================================================================
  // AddressRange Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("AddressRange Nested Interface Tests")
  class AddressRangeTests {

    @Test
    @DisplayName("AddressRange should be a nested interface")
    void addressRangeShouldBeNestedInterface() {
      Class<?>[] declaredClasses = CompiledModule.class.getDeclaredClasses();
      boolean found =
          Arrays.stream(declaredClasses)
              .anyMatch(c -> c.getSimpleName().equals("AddressRange") && c.isInterface());
      assertTrue(found, "AddressRange should be a nested interface");
    }

    @Test
    @DisplayName("AddressRange should have getStart method")
    void addressRangeShouldHaveGetStartMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.AddressRange.class.getMethod("getStart");
      assertNotNull(method, "getStart method should exist");
      assertEquals(long.class, method.getReturnType(), "getStart should return long");
    }

    @Test
    @DisplayName("AddressRange should have getEnd method")
    void addressRangeShouldHaveGetEndMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.AddressRange.class.getMethod("getEnd");
      assertNotNull(method, "getEnd method should exist");
      assertEquals(long.class, method.getReturnType(), "getEnd should return long");
    }

    @Test
    @DisplayName("AddressRange should have getSize default method")
    void addressRangeShouldHaveGetSizeDefaultMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.AddressRange.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "getSize should return long");
      assertTrue(method.isDefault(), "getSize should be a default method");
    }

    @Test
    @DisplayName("AddressRange should have contains default method")
    void addressRangeShouldHaveContainsDefaultMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.AddressRange.class.getMethod("contains", long.class);
      assertNotNull(method, "contains method should exist");
      assertEquals(boolean.class, method.getReturnType(), "contains should return boolean");
      assertTrue(method.isDefault(), "contains should be a default method");
    }
  }

  // ========================================================================
  // CompilationMetadata Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("CompilationMetadata Nested Interface Tests")
  class CompilationMetadataTests {

    @Test
    @DisplayName("CompilationMetadata should be a nested interface")
    void compilationMetadataShouldBeNestedInterface() {
      Class<?>[] declaredClasses = CompiledModule.class.getDeclaredClasses();
      boolean found =
          Arrays.stream(declaredClasses)
              .anyMatch(c -> c.getSimpleName().equals("CompilationMetadata") && c.isInterface());
      assertTrue(found, "CompilationMetadata should be a nested interface");
    }

    @Test
    @DisplayName("CompilationMetadata should have getWasmtimeVersion method")
    void compilationMetadataShouldHaveGetWasmtimeVersionMethod() throws NoSuchMethodException {
      final Method method =
          CompiledModule.CompilationMetadata.class.getMethod("getWasmtimeVersion");
      assertNotNull(method, "getWasmtimeVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "getWasmtimeVersion should return String");
    }

    @Test
    @DisplayName("CompilationMetadata should have getTarget method")
    void compilationMetadataShouldHaveGetTargetMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.CompilationMetadata.class.getMethod("getTarget");
      assertNotNull(method, "getTarget method should exist");
      assertEquals(String.class, method.getReturnType(), "getTarget should return String");
    }

    @Test
    @DisplayName("CompilationMetadata should have getOptimizationLevel method")
    void compilationMetadataShouldHaveGetOptimizationLevelMethod() throws NoSuchMethodException {
      final Method method =
          CompiledModule.CompilationMetadata.class.getMethod("getOptimizationLevel");
      assertNotNull(method, "getOptimizationLevel method should exist");
      assertEquals(
          OptimizationLevel.class,
          method.getReturnType(),
          "getOptimizationLevel should return OptimizationLevel");
    }

    @Test
    @DisplayName("CompilationMetadata should have hasDebugInfo method")
    void compilationMetadataShouldHaveHasDebugInfoMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.CompilationMetadata.class.getMethod("hasDebugInfo");
      assertNotNull(method, "hasDebugInfo method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasDebugInfo should return boolean");
    }

    @Test
    @DisplayName("CompilationMetadata should have getTimestamp method")
    void compilationMetadataShouldHaveGetTimestampMethod() throws NoSuchMethodException {
      final Method method = CompiledModule.CompilationMetadata.class.getMethod("getTimestamp");
      assertNotNull(method, "getTimestamp method should exist");
      assertEquals(long.class, method.getReturnType(), "getTimestamp should return long");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods in CompiledModule")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "getCodeSize",
              "getReadOnlyDataSize",
              "getFunctionCount",
              "serialize",
              "getFunction",
              "getFunctions",
              "getMemoryImage",
              "getCodeRange",
              "getMetadata");

      Set<String> actualMethods =
          Arrays.stream(CompiledModule.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "CompiledModule should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have all three nested interfaces")
    void shouldHaveAllNestedInterfaces() {
      Set<String> expectedNestedInterfaces =
          Set.of("CompiledFunction", "AddressRange", "CompilationMetadata");

      Set<String> actualNestedInterfaces =
          Arrays.stream(CompiledModule.class.getDeclaredClasses())
              .filter(Class::isInterface)
              .map(Class::getSimpleName)
              .collect(Collectors.toSet());

      for (String expected : expectedNestedInterfaces) {
        assertTrue(
            actualNestedInterfaces.contains(expected),
            "CompiledModule should have nested interface: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly three nested interfaces")
    void shouldHaveExactlyThreeNestedInterfaces() {
      long nestedInterfaceCount =
          Arrays.stream(CompiledModule.class.getDeclaredClasses())
              .filter(Class::isInterface)
              .count();
      assertEquals(
          3,
          nestedInterfaceCount,
          "CompiledModule should have exactly 3 nested interfaces, found: " + nestedInterfaceCount);
    }
  }
}
