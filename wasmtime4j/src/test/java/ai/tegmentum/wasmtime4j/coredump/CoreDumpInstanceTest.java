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

package ai.tegmentum.wasmtime4j.coredump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CoreDumpInstance interface.
 *
 * <p>CoreDumpInstance represents a WebAssembly instance captured in a core dump, providing access
 * to instance metadata including module index, name, and resource counts.
 */
@DisplayName("CoreDumpInstance Interface Tests")
class CoreDumpInstanceTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CoreDumpInstance.class.isInterface(), "CoreDumpInstance should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CoreDumpInstance.class.getModifiers()),
          "CoreDumpInstance should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(CoreDumpInstance.class.getModifiers()),
          "CoreDumpInstance should not be final (interfaces cannot be final)");
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
          CoreDumpInstance.class.getInterfaces().length,
          "CoreDumpInstance should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getIndex method")
    void shouldHaveGetIndexMethod() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getIndex");
      assertNotNull(method, "getIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getIndex should be abstract");
    }

    @Test
    @DisplayName("should have getModuleIndex method")
    void shouldHaveGetModuleIndexMethod() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getModuleIndex");
      assertNotNull(method, "getModuleIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getModuleIndex should be abstract");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
      assertFalse(method.isDefault(), "getName should be abstract");
    }

    @Test
    @DisplayName("should have getMemoryCount method")
    void shouldHaveGetMemoryCountMethod() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getMemoryCount");
      assertNotNull(method, "getMemoryCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getMemoryCount should be abstract");
    }

    @Test
    @DisplayName("should have getGlobalCount method")
    void shouldHaveGetGlobalCountMethod() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getGlobalCount");
      assertNotNull(method, "getGlobalCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getGlobalCount should be abstract");
    }

    @Test
    @DisplayName("should have getTableCount method")
    void shouldHaveGetTableCountMethod() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getTableCount");
      assertNotNull(method, "getTableCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getTableCount should be abstract");
    }

    @Test
    @DisplayName("should have exactly 6 abstract methods")
    void shouldHaveExactly6AbstractMethods() {
      long abstractMethods =
          Arrays.stream(CoreDumpInstance.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(6, abstractMethods, "CoreDumpInstance should have exactly 6 abstract methods");
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
          Set.of(
              "getIndex",
              "getModuleIndex",
              "getName",
              "getMemoryCount",
              "getGlobalCount",
              "getTableCount");

      Set<String> actualMethods =
          Arrays.stream(CoreDumpInstance.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "CoreDumpInstance should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 6 declared methods")
    void shouldHaveExactly6DeclaredMethods() {
      long methodCount =
          Arrays.stream(CoreDumpInstance.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(6, methodCount, "CoreDumpInstance should have exactly 6 declared methods");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultMethods =
          Arrays.stream(CoreDumpInstance.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultMethods, "CoreDumpInstance should have no default methods");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticMethods =
          Arrays.stream(CoreDumpInstance.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "CoreDumpInstance should have no static methods");
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
          CoreDumpInstance.class.getDeclaredFields().length,
          "CoreDumpInstance should have no declared fields");
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
          CoreDumpInstance.class.getDeclaredClasses().length,
          "CoreDumpInstance should have no nested classes");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("getIndex should have no parameters")
    void getIndexShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getIndex");
      assertEquals(0, method.getParameterCount(), "getIndex should have no parameters");
    }

    @Test
    @DisplayName("getModuleIndex should have no parameters")
    void getModuleIndexShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getModuleIndex");
      assertEquals(0, method.getParameterCount(), "getModuleIndex should have no parameters");
    }

    @Test
    @DisplayName("getName should have no parameters")
    void getNameShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getName");
      assertEquals(0, method.getParameterCount(), "getName should have no parameters");
    }

    @Test
    @DisplayName("getMemoryCount should have no parameters")
    void getMemoryCountShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getMemoryCount");
      assertEquals(0, method.getParameterCount(), "getMemoryCount should have no parameters");
    }

    @Test
    @DisplayName("getGlobalCount should have no parameters")
    void getGlobalCountShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getGlobalCount");
      assertEquals(0, method.getParameterCount(), "getGlobalCount should have no parameters");
    }

    @Test
    @DisplayName("getTableCount should have no parameters")
    void getTableCountShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getTableCount");
      assertEquals(0, method.getParameterCount(), "getTableCount should have no parameters");
    }
  }

  // ========================================================================
  // Method Visibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Visibility Tests")
  class MethodVisibilityTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      Arrays.stream(CoreDumpInstance.class.getDeclaredMethods())
          .filter(m -> !m.isSynthetic())
          .forEach(
              m ->
                  assertTrue(
                      Modifier.isPublic(m.getModifiers()),
                      "Method " + m.getName() + " should be public"));
    }
  }

  // ========================================================================
  // Generic Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Generic Return Type Tests")
  class GenericReturnTypeTests {

    @Test
    @DisplayName("getName should return Optional<String>")
    void getNameShouldReturnOptionalString() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getName");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      assertEquals(
          String.class, paramType.getActualTypeArguments()[0], "Type argument should be String");
    }
  }

  // ========================================================================
  // Semantic Tests
  // ========================================================================

  @Nested
  @DisplayName("Semantic Tests")
  class SemanticTests {

    @Test
    @DisplayName("getIndex should return primitive int")
    void getIndexShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getIndex");
      assertEquals(
          int.class, method.getReturnType(), "getIndex should return primitive int, not Integer");
    }

    @Test
    @DisplayName("getModuleIndex should return primitive int")
    void getModuleIndexShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getModuleIndex");
      assertEquals(
          int.class,
          method.getReturnType(),
          "getModuleIndex should return primitive int, not Integer");
    }

    @Test
    @DisplayName("getMemoryCount should return primitive int")
    void getMemoryCountShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getMemoryCount");
      assertEquals(
          int.class,
          method.getReturnType(),
          "getMemoryCount should return primitive int, not Integer");
    }

    @Test
    @DisplayName("getGlobalCount should return primitive int")
    void getGlobalCountShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getGlobalCount");
      assertEquals(
          int.class,
          method.getReturnType(),
          "getGlobalCount should return primitive int, not Integer");
    }

    @Test
    @DisplayName("getTableCount should return primitive int")
    void getTableCountShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = CoreDumpInstance.class.getMethod("getTableCount");
      assertEquals(
          int.class,
          method.getReturnType(),
          "getTableCount should return primitive int, not Integer");
    }
  }
}
