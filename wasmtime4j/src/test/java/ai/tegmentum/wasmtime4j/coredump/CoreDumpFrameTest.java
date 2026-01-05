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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CoreDumpFrame interface.
 *
 * <p>CoreDumpFrame represents a WebAssembly stack frame in a core dump, providing access to
 * function information, module context, and local/stack values.
 */
@DisplayName("CoreDumpFrame Interface Tests")
class CoreDumpFrameTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CoreDumpFrame.class.isInterface(), "CoreDumpFrame should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CoreDumpFrame.class.getModifiers()), "CoreDumpFrame should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(CoreDumpFrame.class.getModifiers()),
          "CoreDumpFrame should not be final (interfaces cannot be final)");
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
          CoreDumpFrame.class.getInterfaces().length,
          "CoreDumpFrame should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getFuncIndex method")
    void shouldHaveGetFuncIndexMethod() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getFuncIndex");
      assertNotNull(method, "getFuncIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getFuncIndex should be abstract");
    }

    @Test
    @DisplayName("should have getFuncName method")
    void shouldHaveGetFuncNameMethod() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getFuncName");
      assertNotNull(method, "getFuncName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
      assertFalse(method.isDefault(), "getFuncName should be abstract");
    }

    @Test
    @DisplayName("should have getModuleIndex method")
    void shouldHaveGetModuleIndexMethod() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getModuleIndex");
      assertNotNull(method, "getModuleIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getModuleIndex should be abstract");
    }

    @Test
    @DisplayName("should have getModuleName method")
    void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getModuleName");
      assertNotNull(method, "getModuleName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
      assertFalse(method.isDefault(), "getModuleName should be abstract");
    }

    @Test
    @DisplayName("should have getOffset method")
    void shouldHaveGetOffsetMethod() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getOffset");
      assertNotNull(method, "getOffset method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getOffset should be abstract");
    }

    @Test
    @DisplayName("should have getLocals method")
    void shouldHaveGetLocalsMethod() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getLocals");
      assertNotNull(method, "getLocals method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
      assertFalse(method.isDefault(), "getLocals should be abstract");
    }

    @Test
    @DisplayName("should have getStack method")
    void shouldHaveGetStackMethod() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getStack");
      assertNotNull(method, "getStack method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
      assertFalse(method.isDefault(), "getStack should be abstract");
    }

    @Test
    @DisplayName("should have isTrapFrame method")
    void shouldHaveIsTrapFrameMethod() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("isTrapFrame");
      assertNotNull(method, "isTrapFrame method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertFalse(method.isDefault(), "isTrapFrame should be abstract");
    }

    @Test
    @DisplayName("should have exactly 8 abstract methods")
    void shouldHaveExactly8AbstractMethods() {
      long abstractMethods =
          Arrays.stream(CoreDumpFrame.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(8, abstractMethods, "CoreDumpFrame should have exactly 8 abstract methods");
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
              "getFuncIndex",
              "getFuncName",
              "getModuleIndex",
              "getModuleName",
              "getOffset",
              "getLocals",
              "getStack",
              "isTrapFrame");

      Set<String> actualMethods =
          Arrays.stream(CoreDumpFrame.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "CoreDumpFrame should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 8 declared methods")
    void shouldHaveExactly8DeclaredMethods() {
      long methodCount =
          Arrays.stream(CoreDumpFrame.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(8, methodCount, "CoreDumpFrame should have exactly 8 declared methods");
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
          Arrays.stream(CoreDumpFrame.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultMethods, "CoreDumpFrame should have no default methods");
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
          Arrays.stream(CoreDumpFrame.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "CoreDumpFrame should have no static methods");
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
          CoreDumpFrame.class.getDeclaredFields().length,
          "CoreDumpFrame should have no declared fields");
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
          CoreDumpFrame.class.getDeclaredClasses().length,
          "CoreDumpFrame should have no nested classes");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("getFuncIndex should have no parameters")
    void getFuncIndexShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getFuncIndex");
      assertEquals(0, method.getParameterCount(), "getFuncIndex should have no parameters");
    }

    @Test
    @DisplayName("getFuncName should have no parameters")
    void getFuncNameShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getFuncName");
      assertEquals(0, method.getParameterCount(), "getFuncName should have no parameters");
    }

    @Test
    @DisplayName("getModuleIndex should have no parameters")
    void getModuleIndexShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getModuleIndex");
      assertEquals(0, method.getParameterCount(), "getModuleIndex should have no parameters");
    }

    @Test
    @DisplayName("getModuleName should have no parameters")
    void getModuleNameShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getModuleName");
      assertEquals(0, method.getParameterCount(), "getModuleName should have no parameters");
    }

    @Test
    @DisplayName("getOffset should have no parameters")
    void getOffsetShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getOffset");
      assertEquals(0, method.getParameterCount(), "getOffset should have no parameters");
    }

    @Test
    @DisplayName("getLocals should have no parameters")
    void getLocalsShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getLocals");
      assertEquals(0, method.getParameterCount(), "getLocals should have no parameters");
    }

    @Test
    @DisplayName("getStack should have no parameters")
    void getStackShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getStack");
      assertEquals(0, method.getParameterCount(), "getStack should have no parameters");
    }

    @Test
    @DisplayName("isTrapFrame should have no parameters")
    void isTrapFrameShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("isTrapFrame");
      assertEquals(0, method.getParameterCount(), "isTrapFrame should have no parameters");
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
      Arrays.stream(CoreDumpFrame.class.getDeclaredMethods())
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
    @DisplayName("getFuncName should return Optional<String>")
    void getFuncNameShouldReturnOptionalString() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getFuncName");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      assertEquals(
          String.class, paramType.getActualTypeArguments()[0], "Type argument should be String");
    }

    @Test
    @DisplayName("getModuleName should return Optional<String>")
    void getModuleNameShouldReturnOptionalString() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getModuleName");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      assertEquals(
          String.class, paramType.getActualTypeArguments()[0], "Type argument should be String");
    }

    @Test
    @DisplayName("getLocals should return List<byte[]>")
    void getLocalsShouldReturnListByteArray() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getLocals");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(List.class, paramType.getRawType(), "Raw type should be List");
      assertEquals(
          byte[].class, paramType.getActualTypeArguments()[0], "Type argument should be byte[]");
    }

    @Test
    @DisplayName("getStack should return List<byte[]>")
    void getStackShouldReturnListByteArray() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getStack");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(List.class, paramType.getRawType(), "Raw type should be List");
      assertEquals(
          byte[].class, paramType.getActualTypeArguments()[0], "Type argument should be byte[]");
    }
  }

  // ========================================================================
  // Semantic Tests
  // ========================================================================

  @Nested
  @DisplayName("Semantic Tests")
  class SemanticTests {

    @Test
    @DisplayName("getFuncIndex should return primitive int")
    void getFuncIndexShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getFuncIndex");
      assertEquals(
          int.class,
          method.getReturnType(),
          "getFuncIndex should return primitive int, not Integer");
    }

    @Test
    @DisplayName("getModuleIndex should return primitive int")
    void getModuleIndexShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getModuleIndex");
      assertEquals(
          int.class,
          method.getReturnType(),
          "getModuleIndex should return primitive int, not Integer");
    }

    @Test
    @DisplayName("getOffset should return primitive int")
    void getOffsetShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("getOffset");
      assertEquals(
          int.class, method.getReturnType(), "getOffset should return primitive int, not Integer");
    }

    @Test
    @DisplayName("isTrapFrame should return primitive boolean")
    void isTrapFrameShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = CoreDumpFrame.class.getMethod("isTrapFrame");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isTrapFrame should return primitive boolean, not Boolean");
    }
  }
}
