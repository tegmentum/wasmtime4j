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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Store;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RootScope} class.
 *
 * <p>RootScope provides scoped lifecycle management for rooted GC references with automatic cleanup
 * when the scope is closed.
 */
@DisplayName("RootScope Tests")
class RootScopeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(RootScope.class.getModifiers()), "RootScope should be final");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(RootScope.class),
          "RootScope should implement AutoCloseable");
    }

    @Test
    @DisplayName("should have create factory method")
    void shouldHaveCreateFactoryMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("create", Store.class);
      assertNotNull(method, "Should have create(Store) method");
      assertEquals(
          RootScope.class, method.getReturnType(), "create should return RootScope");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "create should be static");
    }

    @Test
    @DisplayName("should have root method for generic type")
    void shouldHaveGenericRootMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("root", Object.class);
      assertNotNull(method, "Should have root(Object) method");
      assertEquals(
          Rooted.class, method.getReturnType(), "root should return Rooted");
    }

    @Test
    @DisplayName("should have root method for AnyRef")
    void shouldHaveAnyRefRootMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("root", AnyRef.class);
      assertNotNull(method, "Should have root(AnyRef) method");
    }

    @Test
    @DisplayName("should have root method for EqRef")
    void shouldHaveEqRefRootMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("root", EqRef.class);
      assertNotNull(method, "Should have root(EqRef) method");
    }

    @Test
    @DisplayName("should have root method for StructRef")
    void shouldHaveStructRefRootMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("root", StructRef.class);
      assertNotNull(method, "Should have root(StructRef) method");
    }

    @Test
    @DisplayName("should have root method for ArrayRef")
    void shouldHaveArrayRefRootMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("root", ArrayRef.class);
      assertNotNull(method, "Should have root(ArrayRef) method");
    }

    @Test
    @DisplayName("should have getRootedCount method")
    void shouldHaveGetRootedCountMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("getRootedCount");
      assertNotNull(method, "Should have getRootedCount() method");
      assertEquals(
          int.class, method.getReturnType(), "getRootedCount should return int");
    }

    @Test
    @DisplayName("should have getScopeId method")
    void shouldHaveGetScopeIdMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("getScopeId");
      assertNotNull(method, "Should have getScopeId() method");
      assertEquals(
          long.class, method.getReturnType(), "getScopeId should return long");
    }

    @Test
    @DisplayName("should have isOpen method")
    void shouldHaveIsOpenMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("isOpen");
      assertNotNull(method, "Should have isOpen() method");
      assertEquals(
          boolean.class, method.getReturnType(), "isOpen should return boolean");
    }

    @Test
    @DisplayName("should have getStore method")
    void shouldHaveGetStoreMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("getStore");
      assertNotNull(method, "Should have getStore() method");
      assertEquals(
          Store.class, method.getReturnType(), "getStore should return Store");
    }
  }

  @Nested
  @DisplayName("Factory Method Null Validation Tests")
  class FactoryMethodNullTests {

    @Test
    @DisplayName("create with null store should throw NullPointerException")
    void createWithNullStoreShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> RootScope.create(null),
          "create(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Root Method Count Tests")
  class RootMethodCountTests {

    @Test
    @DisplayName("should have multiple root overloads")
    void shouldHaveMultipleRootOverloads() {
      int rootCount = 0;
      for (final Method method : RootScope.class.getMethods()) {
        if ("root".equals(method.getName()) && !Modifier.isStatic(method.getModifiers())) {
          rootCount++;
        }
      }
      assertTrue(
          rootCount >= 5,
          "Should have at least 5 root overloads, found: " + rootCount);
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("toString");
      assertNotNull(method, "Should have toString() method");
      assertEquals(
          String.class, method.getReturnType(), "toString should return String");
    }
  }
}
