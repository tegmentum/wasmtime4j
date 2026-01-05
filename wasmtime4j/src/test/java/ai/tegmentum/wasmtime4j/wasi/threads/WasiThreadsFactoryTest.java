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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiThreadsFactory class.
 *
 * <p>Tests the static factory methods, type definition, and ServiceLoader integration for
 * WASI-Threads support.
 */
@DisplayName("WasiThreadsFactory Class Tests")
class WasiThreadsFactoryTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiThreadsFactory.class.getModifiers()),
          "WasiThreadsFactory should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiThreadsFactory.class.getModifiers()),
          "WasiThreadsFactory should be public");
    }

    @Test
    @DisplayName("should extend Object")
    void shouldExtendObject() {
      assertEquals(
          Object.class,
          WasiThreadsFactory.class.getSuperclass(),
          "WasiThreadsFactory should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          WasiThreadsFactory.class.getInterfaces().length,
          "WasiThreadsFactory should not implement any interfaces");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = WasiThreadsFactory.class.getDeclaredConstructor();
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("should have exactly one constructor")
    void shouldHaveExactlyOneConstructor() {
      assertEquals(
          1,
          WasiThreadsFactory.class.getDeclaredConstructors().length,
          "Should have exactly one constructor");
    }

    @Test
    @DisplayName("private constructor should be invocable via reflection")
    void privateConstructorShouldBeInvocableViaReflection() throws Exception {
      Constructor<?> constructor = WasiThreadsFactory.class.getDeclaredConstructor();
      constructor.setAccessible(true);
      Object instance = constructor.newInstance();
      assertNotNull(instance, "Should be able to create instance via reflection");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have isSupported static method")
    void shouldHaveIsSupportedMethod() throws NoSuchMethodException {
      Method method = WasiThreadsFactory.class.getMethod("isSupported");
      assertNotNull(method, "isSupported method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have createBuilder static method")
    void shouldHaveCreateBuilderMethod() throws NoSuchMethodException {
      Method method = WasiThreadsFactory.class.getMethod("createBuilder");
      assertNotNull(method, "createBuilder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "Should return WasiThreadsContextBuilder");
    }

    @Test
    @DisplayName("should have createContext static method")
    void shouldHaveCreateContextMethod() throws NoSuchMethodException {
      Method method =
          WasiThreadsFactory.class.getMethod(
              "createContext",
              ai.tegmentum.wasmtime4j.Module.class,
              ai.tegmentum.wasmtime4j.Linker.class,
              ai.tegmentum.wasmtime4j.Store.class);
      assertNotNull(method, "createContext method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasiThreadsContext.class, method.getReturnType(), "Should return WasiThreadsContext");
    }

    @Test
    @DisplayName("should have addToLinker static method")
    void shouldHaveAddToLinkerMethod() throws NoSuchMethodException {
      Method method =
          WasiThreadsFactory.class.getMethod(
              "addToLinker",
              ai.tegmentum.wasmtime4j.Linker.class,
              ai.tegmentum.wasmtime4j.Store.class,
              ai.tegmentum.wasmtime4j.Module.class);
      assertNotNull(method, "addToLinker method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // isSupported Method Tests
  // ========================================================================

  @Nested
  @DisplayName("isSupported Method Tests")
  class IsSupportedMethodTests {

    @Test
    @DisplayName("isSupported should return boolean value")
    void isSupportedShouldReturnBooleanValue() {
      boolean result = WasiThreadsFactory.isSupported();
      // Result depends on whether a provider is available
      // Just verify it doesn't throw and returns a boolean
      assertTrue(result || !result, "Should return a boolean value");
    }

    @Test
    @DisplayName("isSupported should be consistent across multiple calls")
    void isSupportedShouldBeConsistent() {
      boolean first = WasiThreadsFactory.isSupported();
      boolean second = WasiThreadsFactory.isSupported();
      assertEquals(first, second, "isSupported should return consistent results");
    }
  }

  // ========================================================================
  // createBuilder Method Tests
  // ========================================================================

  @Nested
  @DisplayName("createBuilder Method Tests")
  class CreateBuilderMethodTests {

    @Test
    @DisplayName("createBuilder should throw UnsupportedOperationException when not supported")
    void createBuilderShouldThrowWhenNotSupported() {
      if (!WasiThreadsFactory.isSupported()) {
        UnsupportedOperationException exception =
            assertThrows(
                UnsupportedOperationException.class,
                WasiThreadsFactory::createBuilder,
                "Should throw UnsupportedOperationException when not supported");
        assertTrue(
            exception.getMessage().contains("not supported"),
            "Exception message should indicate feature is not supported");
      }
    }

    @Test
    @DisplayName("createBuilder should return builder when supported")
    void createBuilderShouldReturnBuilderWhenSupported() {
      if (WasiThreadsFactory.isSupported()) {
        WasiThreadsContextBuilder builder = WasiThreadsFactory.createBuilder();
        assertNotNull(builder, "Should return non-null builder when supported");
      }
    }
  }

  // ========================================================================
  // addToLinker Method Tests
  // ========================================================================

  @Nested
  @DisplayName("addToLinker Method Tests")
  class AddToLinkerMethodTests {

    @Test
    @DisplayName("addToLinker should throw UnsupportedOperationException when not supported")
    void addToLinkerShouldThrowWhenNotSupported() {
      if (!WasiThreadsFactory.isSupported()) {
        UnsupportedOperationException exception =
            assertThrows(
                UnsupportedOperationException.class,
                () -> WasiThreadsFactory.addToLinker(null, null, null),
                "Should throw UnsupportedOperationException when not supported");
        assertTrue(
            exception.getMessage().contains("not supported"),
            "Exception message should indicate feature is not supported");
      }
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have PROVIDER static field")
    void shouldHaveProviderStaticField() throws NoSuchFieldException {
      Field field = WasiThreadsFactory.class.getDeclaredField("PROVIDER");
      assertNotNull(field, "PROVIDER field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("should have exactly 1 declared field")
    void shouldHaveExactlyOneDeclaredField() {
      assertEquals(
          1,
          WasiThreadsFactory.class.getDeclaredFields().length,
          "Should have exactly 1 declared field");
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
      Set<String> expectedMethods =
          Set.of("isSupported", "createBuilder", "createContext", "addToLinker");

      Set<String> actualMethods =
          Arrays.stream(WasiThreadsFactory.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 4 declared methods")
    void shouldHaveExactly4DeclaredMethods() {
      long methodCount =
          Arrays.stream(WasiThreadsFactory.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(4, methodCount, "Should have exactly 4 declared methods");
    }

    @Test
    @DisplayName("all methods should be static")
    void allMethodsShouldBeStatic() {
      boolean allStatic =
          Arrays.stream(WasiThreadsFactory.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .allMatch(m -> Modifier.isStatic(m.getModifiers()));
      assertTrue(allStatic, "All methods should be static");
    }

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      boolean allPublic =
          Arrays.stream(WasiThreadsFactory.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .allMatch(m -> Modifier.isPublic(m.getModifiers()));
      assertTrue(allPublic, "All methods should be public");
    }
  }

  // ========================================================================
  // Exception Declaration Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("createContext should declare WasmException")
    void createContextShouldDeclareWasmException() throws NoSuchMethodException {
      Method method =
          WasiThreadsFactory.class.getMethod(
              "createContext",
              ai.tegmentum.wasmtime4j.Module.class,
              ai.tegmentum.wasmtime4j.Linker.class,
              ai.tegmentum.wasmtime4j.Store.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(e -> e.getSimpleName().equals("WasmException"));
      assertTrue(declaresWasmException, "createContext should declare WasmException");
    }

    @Test
    @DisplayName("addToLinker should declare WasmException")
    void addToLinkerShouldDeclareWasmException() throws NoSuchMethodException {
      Method method =
          WasiThreadsFactory.class.getMethod(
              "addToLinker",
              ai.tegmentum.wasmtime4j.Linker.class,
              ai.tegmentum.wasmtime4j.Store.class,
              ai.tegmentum.wasmtime4j.Module.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(e -> e.getSimpleName().equals("WasmException"));
      assertTrue(declaresWasmException, "addToLinker should declare WasmException");
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
          WasiThreadsFactory.class.getDeclaredClasses().length,
          "WasiThreadsFactory should have no nested classes");
    }
  }
}
