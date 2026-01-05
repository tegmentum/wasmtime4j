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
 * Comprehensive test suite for the WasiThreadsContextBuilder interface.
 *
 * <p>Tests the builder interface for creating WASI-Threads contexts.
 */
@DisplayName("WasiThreadsContextBuilder Interface Tests")
class WasiThreadsContextBuilderTest {

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
          WasiThreadsContextBuilder.class.isInterface(),
          "WasiThreadsContextBuilder should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiThreadsContextBuilder.class.getModifiers()),
          "WasiThreadsContextBuilder should be public");
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
          WasiThreadsContextBuilder.class.getInterfaces().length,
          "WasiThreadsContextBuilder should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have withModule method")
    void shouldHaveWithModuleMethod() throws NoSuchMethodException {
      Method method =
          WasiThreadsContextBuilder.class.getMethod(
              "withModule", ai.tegmentum.wasmtime4j.Module.class);
      assertNotNull(method, "withModule method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "Should return WasiThreadsContextBuilder for fluent chaining");
    }

    @Test
    @DisplayName("should have withLinker method")
    void shouldHaveWithLinkerMethod() throws NoSuchMethodException {
      Method method =
          WasiThreadsContextBuilder.class.getMethod(
              "withLinker", ai.tegmentum.wasmtime4j.Linker.class);
      assertNotNull(method, "withLinker method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "Should return WasiThreadsContextBuilder for fluent chaining");
    }

    @Test
    @DisplayName("should have withStore method")
    void shouldHaveWithStoreMethod() throws NoSuchMethodException {
      Method method =
          WasiThreadsContextBuilder.class.getMethod(
              "withStore", ai.tegmentum.wasmtime4j.Store.class);
      assertNotNull(method, "withStore method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "Should return WasiThreadsContextBuilder for fluent chaining");
    }

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContextBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasiThreadsContext.class, method.getReturnType(), "Should return WasiThreadsContext");
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
      Set<String> expectedMethods = Set.of("withModule", "withLinker", "withStore", "build");

      Set<String> actualMethods =
          Arrays.stream(WasiThreadsContextBuilder.class.getDeclaredMethods())
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
          Arrays.stream(WasiThreadsContextBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(4, methodCount, "Should have exactly 4 declared methods");
    }

    @Test
    @DisplayName("all methods should be abstract")
    void allMethodsShouldBeAbstract() {
      boolean allAbstract =
          Arrays.stream(WasiThreadsContextBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .allMatch(m -> Modifier.isAbstract(m.getModifiers()));
      assertTrue(allAbstract, "All methods should be abstract");
    }

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      boolean allPublic =
          Arrays.stream(WasiThreadsContextBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .allMatch(m -> Modifier.isPublic(m.getModifiers()));
      assertTrue(allPublic, "All methods should be public");
    }

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticCount =
          Arrays.stream(WasiThreadsContextBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticCount, "Should have no static methods");
    }

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultCount =
          Arrays.stream(WasiThreadsContextBuilder.class.getDeclaredMethods())
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
    @DisplayName("build should declare WasmException")
    void buildShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = WasiThreadsContextBuilder.class.getMethod("build");
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(e -> e.getSimpleName().equals("WasmException"));
      assertTrue(declaresWasmException, "build should declare WasmException");
    }

    @Test
    @DisplayName("withModule should not declare exceptions")
    void withModuleShouldNotDeclareExceptions() throws NoSuchMethodException {
      Method method =
          WasiThreadsContextBuilder.class.getMethod(
              "withModule", ai.tegmentum.wasmtime4j.Module.class);
      assertEquals(
          0, method.getExceptionTypes().length, "withModule should not declare any exceptions");
    }

    @Test
    @DisplayName("withLinker should not declare exceptions")
    void withLinkerShouldNotDeclareExceptions() throws NoSuchMethodException {
      Method method =
          WasiThreadsContextBuilder.class.getMethod(
              "withLinker", ai.tegmentum.wasmtime4j.Linker.class);
      assertEquals(
          0, method.getExceptionTypes().length, "withLinker should not declare any exceptions");
    }

    @Test
    @DisplayName("withStore should not declare exceptions")
    void withStoreShouldNotDeclareExceptions() throws NoSuchMethodException {
      Method method =
          WasiThreadsContextBuilder.class.getMethod(
              "withStore", ai.tegmentum.wasmtime4j.Store.class);
      assertEquals(
          0, method.getExceptionTypes().length, "withStore should not declare any exceptions");
    }
  }

  // ========================================================================
  // Fluent Builder Pattern Tests
  // ========================================================================

  @Nested
  @DisplayName("Fluent Builder Pattern Tests")
  class FluentBuilderPatternTests {

    @Test
    @DisplayName("withModule should return builder for chaining")
    void withModuleShouldReturnBuilderForChaining() throws NoSuchMethodException {
      Method method =
          WasiThreadsContextBuilder.class.getMethod(
              "withModule", ai.tegmentum.wasmtime4j.Module.class);
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "withModule should return WasiThreadsContextBuilder");
    }

    @Test
    @DisplayName("withLinker should return builder for chaining")
    void withLinkerShouldReturnBuilderForChaining() throws NoSuchMethodException {
      Method method =
          WasiThreadsContextBuilder.class.getMethod(
              "withLinker", ai.tegmentum.wasmtime4j.Linker.class);
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "withLinker should return WasiThreadsContextBuilder");
    }

    @Test
    @DisplayName("withStore should return builder for chaining")
    void withStoreShouldReturnBuilderForChaining() throws NoSuchMethodException {
      Method method =
          WasiThreadsContextBuilder.class.getMethod(
              "withStore", ai.tegmentum.wasmtime4j.Store.class);
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "withStore should return WasiThreadsContextBuilder");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("withModule should have 1 parameter")
    void withModuleShouldHave1Parameter() throws NoSuchMethodException {
      Method method =
          WasiThreadsContextBuilder.class.getMethod(
              "withModule", ai.tegmentum.wasmtime4j.Module.class);
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("withLinker should have 1 parameter")
    void withLinkerShouldHave1Parameter() throws NoSuchMethodException {
      Method method =
          WasiThreadsContextBuilder.class.getMethod(
              "withLinker", ai.tegmentum.wasmtime4j.Linker.class);
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("withStore should have 1 parameter")
    void withStoreShouldHave1Parameter() throws NoSuchMethodException {
      Method method =
          WasiThreadsContextBuilder.class.getMethod(
              "withStore", ai.tegmentum.wasmtime4j.Store.class);
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("build should have no parameters")
    void buildShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = WasiThreadsContextBuilder.class.getMethod("build");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
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
          WasiThreadsContextBuilder.class.getDeclaredClasses().length,
          "WasiThreadsContextBuilder should have no nested classes");
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
          WasiThreadsContextBuilder.class.getDeclaredFields().length,
          "WasiThreadsContextBuilder should have no declared fields");
    }
  }
}
