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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the ResourceTable interface.
 *
 * <p>ResourceTable is a low-level resource table for Component Model resource handles. This test
 * verifies the interface structure and method signatures.
 */
@DisplayName("ResourceTable Interface Tests")
class ResourceTableTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ResourceTable.class.isInterface(), "ResourceTable should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ResourceTable.class.getModifiers()), "ResourceTable should be public");
    }

    @Test
    @DisplayName("should have one type parameter")
    void shouldHaveOneTypeParameter() {
      TypeVariable<?>[] typeParams = ResourceTable.class.getTypeParameters();
      assertEquals(1, typeParams.length, "ResourceTable should have exactly one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }
  }

  // ========================================================================
  // Push Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Push Method Tests")
  class PushMethodTests {

    @Test
    @DisplayName("should have push method")
    void shouldHavePushMethod() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("push", Object.class);
      assertNotNull(method, "push method should exist");
      assertEquals(int.class, method.getReturnType(), "push should return int");
    }

    @Test
    @DisplayName("push should declare ResourceException")
    void pushShouldDeclareResourceException() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("push", Object.class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "push should declare one exception");
      assertEquals(
          "ai.tegmentum.wasmtime4j.exception.ResourceException",
          exceptions[0].getName(),
          "push should declare ResourceException");
    }
  }

  // ========================================================================
  // Get Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Get Methods Tests")
  class GetMethodsTests {

    @Test
    @DisplayName("should have get method")
    void shouldHaveGetMethod() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("get", int.class);
      assertNotNull(method, "get method should exist");
      assertEquals(Optional.class, method.getReturnType(), "get should return Optional");
    }

    @Test
    @DisplayName("should have getOrThrow method")
    void shouldHaveGetOrThrowMethod() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("getOrThrow", int.class);
      assertNotNull(method, "getOrThrow method should exist");
    }

    @Test
    @DisplayName("getOrThrow should declare ResourceException")
    void getOrThrowShouldDeclareResourceException() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("getOrThrow", int.class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "getOrThrow should declare one exception");
      assertEquals(
          "ai.tegmentum.wasmtime4j.exception.ResourceException",
          exceptions[0].getName(),
          "getOrThrow should declare ResourceException");
    }
  }

  // ========================================================================
  // Delete Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Delete Methods Tests")
  class DeleteMethodsTests {

    @Test
    @DisplayName("should have delete method")
    void shouldHaveDeleteMethod() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("delete", int.class);
      assertNotNull(method, "delete method should exist");
      assertEquals(Optional.class, method.getReturnType(), "delete should return Optional");
    }

    @Test
    @DisplayName("should have deleteOrThrow method")
    void shouldHaveDeleteOrThrowMethod() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("deleteOrThrow", int.class);
      assertNotNull(method, "deleteOrThrow method should exist");
    }

    @Test
    @DisplayName("deleteOrThrow should declare ResourceException")
    void deleteOrThrowShouldDeclareResourceException() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("deleteOrThrow", int.class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "deleteOrThrow should declare one exception");
      assertEquals(
          "ai.tegmentum.wasmtime4j.exception.ResourceException",
          exceptions[0].getName(),
          "deleteOrThrow should declare ResourceException");
    }
  }

  // ========================================================================
  // Query Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Query Methods Tests")
  class QueryMethodsTests {

    @Test
    @DisplayName("should have contains method")
    void shouldHaveContainsMethod() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("contains", int.class);
      assertNotNull(method, "contains method should exist");
      assertEquals(boolean.class, method.getReturnType(), "contains should return boolean");
    }

    @Test
    @DisplayName("should have size method")
    void shouldHaveSizeMethod() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertEquals(int.class, method.getReturnType(), "size should return int");
    }

    @Test
    @DisplayName("should have isEmpty method")
    void shouldHaveIsEmptyMethod() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("isEmpty");
      assertNotNull(method, "isEmpty method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isEmpty should return boolean");
    }

    @Test
    @DisplayName("should have capacity method")
    void shouldHaveCapacityMethod() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("capacity");
      assertNotNull(method, "capacity method should exist");
      assertEquals(int.class, method.getReturnType(), "capacity should return int");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "clear should return void");
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
              "push",
              "get",
              "getOrThrow",
              "delete",
              "deleteOrThrow",
              "contains",
              "size",
              "isEmpty",
              "capacity",
              "clear");

      Set<String> actualMethods =
          Arrays.stream(ResourceTable.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "ResourceTable should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 10 declared methods")
    void shouldHaveTenDeclaredMethods() {
      assertEquals(
          10,
          ResourceTable.class.getDeclaredMethods().length,
          "ResourceTable should have exactly 10 methods");
    }

    @Test
    @DisplayName("all methods should be abstract")
    void allMethodsShouldBeAbstract() {
      for (Method method : ResourceTable.class.getDeclaredMethods()) {
        assertTrue(
            Modifier.isAbstract(method.getModifiers()), method.getName() + " should be abstract");
      }
    }
  }

  // ========================================================================
  // Method Signature Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("push should accept Object parameter")
    void pushShouldAcceptObjectParameter() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("push", Object.class);
      assertEquals(1, method.getParameterCount(), "push should have 1 parameter");
    }

    @Test
    @DisplayName("get should accept int parameter")
    void getShouldAcceptIntParameter() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("get", int.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "get should have 1 parameter");
      assertEquals(int.class, paramTypes[0], "get parameter should be int");
    }

    @Test
    @DisplayName("contains should accept int parameter")
    void containsShouldAcceptIntParameter() throws NoSuchMethodException {
      final Method method = ResourceTable.class.getMethod("contains", int.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "contains should have 1 parameter");
      assertEquals(int.class, paramTypes[0], "contains parameter should be int");
    }

    @Test
    @DisplayName("query methods should have no parameters")
    void queryMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      assertEquals(
          0,
          ResourceTable.class.getMethod("size").getParameterCount(),
          "size should have 0 params");
      assertEquals(
          0,
          ResourceTable.class.getMethod("isEmpty").getParameterCount(),
          "isEmpty should have 0 params");
      assertEquals(
          0,
          ResourceTable.class.getMethod("capacity").getParameterCount(),
          "capacity should have 0 params");
      assertEquals(
          0,
          ResourceTable.class.getMethod("clear").getParameterCount(),
          "clear should have 0 params");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interface")
    void shouldNotExtendAnyInterface() {
      assertEquals(
          0,
          ResourceTable.class.getInterfaces().length,
          "ResourceTable should not extend any interface");
    }
  }
}
