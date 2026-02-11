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
 * Tests for {@link OwnedRooted} class.
 *
 * <p>OwnedRooted is an owned rooted reference to a GC-managed WebAssembly object that implements
 * AutoCloseable for convenient try-with-resources usage.
 */
@DisplayName("OwnedRooted Tests")
class OwnedRootedTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(OwnedRooted.class.getModifiers()), "OwnedRooted should be final");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(OwnedRooted.class),
          "OwnedRooted should implement AutoCloseable");
    }

    @Test
    @DisplayName("should have get method accepting Store")
    void shouldHaveGetMethodAcceptingStore() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("get", Store.class);
      assertNotNull(method, "Should have get(Store) method");
    }

    @Test
    @DisplayName("should have release method")
    void shouldHaveReleaseMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("release");
      assertNotNull(method, "Should have release() method");
    }

    @Test
    @DisplayName("should have isValid method returning boolean")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("isValid");
      assertNotNull(method, "Should have isValid() method");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("close");
      assertNotNull(method, "Should have close() method");
    }

    @Test
    @DisplayName("should have getRootId method returning long")
    void shouldHaveGetRootIdMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("getRootId");
      assertNotNull(method, "Should have getRootId() method");
      assertEquals(long.class, method.getReturnType(), "getRootId should return long");
    }

    @Test
    @DisplayName("should have getStoreId method returning long")
    void shouldHaveGetStoreIdMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("getStoreId");
      assertNotNull(method, "Should have getStoreId() method");
      assertEquals(long.class, method.getReturnType(), "getStoreId should return long");
    }

    @Test
    @DisplayName("should have transferToScope method")
    void shouldHaveTransferToScopeMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("transferToScope", RootScope.class);
      assertNotNull(method, "Should have transferToScope(RootScope) method");
      assertEquals(Rooted.class, method.getReturnType(), "transferToScope should return Rooted");
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
          () -> OwnedRooted.create(null, "value"),
          "create(null store, value) should throw NullPointerException");
    }

    @Test
    @DisplayName("create AnyRef with null store should throw NullPointerException")
    void createAnyRefWithNullStoreShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> OwnedRooted.create(null, AnyRef.nullRef()),
          "create(null store, AnyRef) should throw NullPointerException");
    }

    @Test
    @DisplayName("create EqRef with null store should throw NullPointerException")
    void createEqRefWithNullStoreShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> OwnedRooted.create(null, EqRef.nullRef()),
          "create(null store, EqRef) should throw NullPointerException");
    }

    @Test
    @DisplayName("create StructRef with null store should throw NullPointerException")
    void createStructRefWithNullStoreShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> OwnedRooted.create(null, StructRef.nullRef()),
          "create(null store, StructRef) should throw NullPointerException");
    }

    @Test
    @DisplayName("create ArrayRef with null store should throw NullPointerException")
    void createArrayRefWithNullStoreShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> OwnedRooted.create(null, ArrayRef.nullRef()),
          "create(null store, ArrayRef) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Static Create Method Count Tests")
  class StaticCreateMethodTests {

    @Test
    @DisplayName("should have multiple create overloads")
    void shouldHaveMultipleCreateOverloads() {
      int createCount = 0;
      for (final Method method : OwnedRooted.class.getMethods()) {
        if ("create".equals(method.getName()) && Modifier.isStatic(method.getModifiers())) {
          createCount++;
        }
      }
      assertTrue(
          createCount >= 5, "Should have at least 5 create overloads, found: " + createCount);
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("equals", Object.class);
      assertNotNull(method, "Should have equals(Object) method");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("hashCode");
      assertNotNull(method, "Should have hashCode() method");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = OwnedRooted.class.getMethod("toString");
      assertNotNull(method, "Should have toString() method");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
    }
  }
}
