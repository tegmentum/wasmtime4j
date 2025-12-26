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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RootScope} class.
 *
 * <p>RootScope provides automatic cleanup for rooted GC references within a specific scope.
 */
@DisplayName("RootScope Tests")
class RootScopeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(RootScope.class.getModifiers()), "RootScope should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(RootScope.class.getModifiers()), "RootScope should be public");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(RootScope.class),
          "RootScope should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have create factory method")
    void shouldHaveCreateFactoryMethod() throws NoSuchMethodException {
      final Method method =
          RootScope.class.getMethod("create", ai.tegmentum.wasmtime4j.Store.class);
      assertNotNull(method, "create method should exist");
      assertEquals(RootScope.class, method.getReturnType(), "create should return RootScope");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
    }

    @Test
    @DisplayName("create method should take Store parameter")
    void createMethodShouldTakeStoreParameter() throws NoSuchMethodException {
      final Method method =
          RootScope.class.getMethod("create", ai.tegmentum.wasmtime4j.Store.class);
      assertEquals(1, method.getParameterCount(), "create should take one parameter");
      assertEquals(
          ai.tegmentum.wasmtime4j.Store.class,
          method.getParameterTypes()[0],
          "Parameter should be Store");
    }
  }

  @Nested
  @DisplayName("Root Method Signature Tests")
  class RootMethodSignatureTests {

    @Test
    @DisplayName("should have generic root method")
    void shouldHaveGenericRootMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("root", Object.class);
      assertNotNull(method, "generic root method should exist");
      assertEquals(Rooted.class, method.getReturnType(), "root should return Rooted");
    }

    @Test
    @DisplayName("should have root method for AnyRef")
    void shouldHaveRootMethodForAnyRef() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("root", AnyRef.class);
      assertNotNull(method, "root(AnyRef) method should exist");
      assertEquals(Rooted.class, method.getReturnType(), "Should return Rooted");
    }

    @Test
    @DisplayName("should have root method for EqRef")
    void shouldHaveRootMethodForEqRef() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("root", EqRef.class);
      assertNotNull(method, "root(EqRef) method should exist");
      assertEquals(Rooted.class, method.getReturnType(), "Should return Rooted");
    }

    @Test
    @DisplayName("should have root method for StructRef")
    void shouldHaveRootMethodForStructRef() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("root", StructRef.class);
      assertNotNull(method, "root(StructRef) method should exist");
      assertEquals(Rooted.class, method.getReturnType(), "Should return Rooted");
    }

    @Test
    @DisplayName("should have root method for ArrayRef")
    void shouldHaveRootMethodForArrayRef() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("root", ArrayRef.class);
      assertNotNull(method, "root(ArrayRef) method should exist");
      assertEquals(Rooted.class, method.getReturnType(), "Should return Rooted");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getRootedCount method")
    void shouldHaveGetRootedCountMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("getRootedCount");
      assertNotNull(method, "getRootedCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getRootedCount should return int");
    }

    @Test
    @DisplayName("should have getScopeId method")
    void shouldHaveGetScopeIdMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("getScopeId");
      assertNotNull(method, "getScopeId method should exist");
      assertEquals(long.class, method.getReturnType(), "getScopeId should return long");
    }

    @Test
    @DisplayName("should have isOpen method")
    void shouldHaveIsOpenMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("isOpen");
      assertNotNull(method, "isOpen method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isOpen should return boolean");
    }

    @Test
    @DisplayName("should have getStore method")
    void shouldHaveGetStoreMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("getStore");
      assertNotNull(method, "getStore method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.Store.class,
          method.getReturnType(),
          "getStore should return Store");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }

    @Test
    @DisplayName("close method should take no parameters")
    void closeMethodShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("close");
      assertEquals(0, method.getParameterCount(), "close should take no parameters");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = RootScope.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected public methods")
    void shouldHaveExpectedPublicMethods() {
      // Count all public instance methods (excluding inherited from Object)
      int methodCount = 0;
      for (final Method method : RootScope.class.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
          methodCount++;
        }
      }
      // root (5 overloads), getRootedCount, getScopeId, isOpen, getStore, close, toString
      assertTrue(methodCount >= 10, "RootScope should have at least 10 public instance methods");
    }
  }
}
