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

package ai.tegmentum.wasmtime4j.wasi.threads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiThreadsProvider interface.
 *
 * <p>Tests the SPI (Service Provider Interface) for WASI-Threads implementations.
 */
@DisplayName("WasiThreadsProvider Interface Tests")
class WasiThreadsProviderTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiThreadsProvider.class.isInterface(), "WasiThreadsProvider should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiThreadsProvider.class.getModifiers()),
          "WasiThreadsProvider should be public");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interfaces")
    void shouldNotExtendAnyInterfaces() {
      assertEquals(
          0,
          WasiThreadsProvider.class.getInterfaces().length,
          "WasiThreadsProvider should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have isAvailable method")
    void shouldHaveIsAvailableMethod() throws NoSuchMethodException {
      Method method = WasiThreadsProvider.class.getMethod("isAvailable");
      assertNotNull(method, "isAvailable method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have createBuilder method")
    void shouldHaveCreateBuilderMethod() throws NoSuchMethodException {
      Method method = WasiThreadsProvider.class.getMethod("createBuilder");
      assertNotNull(method, "createBuilder method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "Should return WasiThreadsContextBuilder");
    }

    @Test
    @DisplayName("should have addToLinker method")
    void shouldHaveAddToLinkerMethod() throws NoSuchMethodException {
      Method method =
          WasiThreadsProvider.class.getMethod(
              "addToLinker",
              ai.tegmentum.wasmtime4j.Linker.class,
              ai.tegmentum.wasmtime4j.Store.class,
              ai.tegmentum.wasmtime4j.Module.class);
      assertNotNull(method, "addToLinker method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods = Set.of("isAvailable", "createBuilder", "addToLinker");

      Set<String> actualMethods =
          Arrays.stream(WasiThreadsProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 3 declared methods")
    void shouldHaveExactly3DeclaredMethods() {
      long methodCount =
          Arrays.stream(WasiThreadsProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(3, methodCount, "Should have exactly 3 declared methods");
    }

    @Test
    @DisplayName("all methods should be abstract")
    void allMethodsShouldBeAbstract() {
      boolean allAbstract =
          Arrays.stream(WasiThreadsProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .allMatch(m -> Modifier.isAbstract(m.getModifiers()));
      assertTrue(allAbstract, "All methods should be abstract");
    }

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      boolean allPublic =
          Arrays.stream(WasiThreadsProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .allMatch(m -> Modifier.isPublic(m.getModifiers()));
      assertTrue(allPublic, "All methods should be public");
    }

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticCount =
          Arrays.stream(WasiThreadsProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticCount, "Should have no static methods");
    }

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultCount =
          Arrays.stream(WasiThreadsProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultCount, "Should have no default methods");
    }
  }

  // ========================================================================
  // Exception Declaration Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("addToLinker should declare WasmException")
    void addToLinkerShouldDeclareWasmException() throws NoSuchMethodException {
      Method method =
          WasiThreadsProvider.class.getMethod(
              "addToLinker",
              ai.tegmentum.wasmtime4j.Linker.class,
              ai.tegmentum.wasmtime4j.Store.class,
              ai.tegmentum.wasmtime4j.Module.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(e -> e.getSimpleName().equals("WasmException"));
      assertTrue(declaresWasmException, "addToLinker should declare WasmException");
    }

    @Test
    @DisplayName("isAvailable should not declare exceptions")
    void isAvailableShouldNotDeclareExceptions() throws NoSuchMethodException {
      Method method = WasiThreadsProvider.class.getMethod("isAvailable");
      assertEquals(
          0, method.getExceptionTypes().length, "isAvailable should not declare any exceptions");
    }

    @Test
    @DisplayName("createBuilder should not declare exceptions")
    void createBuilderShouldNotDeclareExceptions() throws NoSuchMethodException {
      Method method = WasiThreadsProvider.class.getMethod("createBuilder");
      assertEquals(
          0, method.getExceptionTypes().length, "createBuilder should not declare any exceptions");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("isAvailable should have no parameters")
    void isAvailableShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = WasiThreadsProvider.class.getMethod("isAvailable");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("createBuilder should have no parameters")
    void createBuilderShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = WasiThreadsProvider.class.getMethod("createBuilder");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("addToLinker should have 3 parameters")
    void addToLinkerShouldHave3Parameters() throws NoSuchMethodException {
      Method method =
          WasiThreadsProvider.class.getMethod(
              "addToLinker",
              ai.tegmentum.wasmtime4j.Linker.class,
              ai.tegmentum.wasmtime4j.Store.class,
              ai.tegmentum.wasmtime4j.Module.class);
      assertEquals(3, method.getParameterCount(), "Should have 3 parameters");
    }

    @Test
    @DisplayName("addToLinker parameters should have correct types")
    void addToLinkerParametersShouldHaveCorrectTypes() throws NoSuchMethodException {
      Method method =
          WasiThreadsProvider.class.getMethod(
              "addToLinker",
              ai.tegmentum.wasmtime4j.Linker.class,
              ai.tegmentum.wasmtime4j.Store.class,
              ai.tegmentum.wasmtime4j.Module.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(
          ai.tegmentum.wasmtime4j.Linker.class, paramTypes[0], "First parameter should be Linker");
      assertEquals(
          ai.tegmentum.wasmtime4j.Store.class, paramTypes[1], "Second parameter should be Store");
      assertEquals(
          ai.tegmentum.wasmtime4j.Module.class, paramTypes[2], "Third parameter should be Module");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          WasiThreadsProvider.class.getDeclaredClasses().length,
          "WasiThreadsProvider should have no nested classes");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0,
          WasiThreadsProvider.class.getDeclaredFields().length,
          "WasiThreadsProvider should have no declared fields");
    }
  }
}
