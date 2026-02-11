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

package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentResourceHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitOwn} class.
 *
 * <p>WitOwn represents a WIT owned resource handle value. When a component receives an owned
 * handle, it takes ownership of the resource.
 */
@DisplayName("WitOwn Tests")
class WitOwnTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitOwn.class.getModifiers()), "WitOwn should be final");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(WitValue.class.isAssignableFrom(WitOwn.class), "WitOwn should extend WitValue");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have of factory method")
    void shouldHaveOfFactoryMethod() throws NoSuchMethodException {
      final Method method = WitOwn.class.getMethod("of", String.class, int.class);
      assertNotNull(method, "Should have of(String, int) method");
      assertEquals(WitOwn.class, method.getReturnType(), "Should return WitOwn");
    }

    @Test
    @DisplayName("should have ofWithHost factory method")
    void shouldHaveOfWithHostFactoryMethod() throws NoSuchMethodException {
      final Method method =
          WitOwn.class.getMethod("ofWithHost", String.class, int.class, Object.class);
      assertNotNull(method, "Should have ofWithHost method");
      assertEquals(WitOwn.class, method.getReturnType(), "Should return WitOwn");
    }

    @Test
    @DisplayName("should have fromHandle factory method")
    void shouldHaveFromHandleFactoryMethod() throws NoSuchMethodException {
      final Method method = WitOwn.class.getMethod("fromHandle", ComponentResourceHandle.class);
      assertNotNull(method, "Should have fromHandle method");
      assertEquals(WitOwn.class, method.getReturnType(), "Should return WitOwn");
    }

    @Test
    @DisplayName("should create WitOwn from resource type and index")
    void shouldCreateFromResourceTypeAndIndex() {
      final WitOwn value = WitOwn.of("file-handle", 42);
      assertNotNull(value, "Should create WitOwn");
      assertEquals("file-handle", value.getResourceType(), "Should have resource type");
      assertEquals(42, value.getIndex(), "Should have index");
    }

    @Test
    @DisplayName("should create WitOwn with host object")
    void shouldCreateWithHostObject() {
      final String hostObject = "test-host";
      final WitOwn value = WitOwn.ofWithHost("resource", 1, hostObject);
      assertNotNull(value, "Should create WitOwn with host object");
      assertEquals("test-host", value.getHostObject(String.class), "Should return host object");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("should reject negative index")
    void shouldRejectNegativeIndex() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitOwn.of("resource", -1),
          "Should reject negative index");
    }

    @Test
    @DisplayName("fromHandle should reject null handle")
    void fromHandleShouldRejectNullHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitOwn.fromHandle(null),
          "Should reject null handle");
    }

    @Test
    @DisplayName("fromHandle should reject borrowed handle")
    void fromHandleShouldRejectBorrowedHandle() {
      final ComponentResourceHandle borrowed = ComponentResourceHandle.borrow("resource", 1);
      assertThrows(
          IllegalArgumentException.class,
          () -> WitOwn.fromHandle(borrowed),
          "Should reject borrowed handle");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getHandle should return the resource handle")
    void getHandleShouldReturnResourceHandle() {
      final WitOwn value = WitOwn.of("resource", 1);
      assertNotNull(value.getHandle(), "Should return handle");
      assertTrue(value.getHandle().isOwned(), "Handle should be owned");
    }

    @Test
    @DisplayName("getResourceType should return correct type")
    void getResourceTypeShouldReturnCorrectType() {
      final WitOwn value = WitOwn.of("my-resource", 5);
      assertEquals("my-resource", value.getResourceType(), "Should return resource type");
    }

    @Test
    @DisplayName("getIndex should return correct index")
    void getIndexShouldReturnCorrectIndex() {
      final WitOwn value = WitOwn.of("resource", 99);
      assertEquals(99, value.getIndex(), "Should return index");
    }

    @Test
    @DisplayName("toJava should return ComponentResourceHandle")
    void toJavaShouldReturnComponentResourceHandle() {
      final WitOwn value = WitOwn.of("resource", 1);
      assertTrue(
          value.toJava() instanceof ComponentResourceHandle,
          "toJava should return ComponentResourceHandle");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same values should be equal")
    void sameValuesShouldBeEqual() {
      final WitOwn value1 = WitOwn.of("resource", 1);
      final WitOwn value2 = WitOwn.of("resource", 1);
      assertEquals(value1, value2, "Same values should be equal");
    }

    @Test
    @DisplayName("different resource types should not be equal")
    void differentResourceTypesShouldNotBeEqual() {
      final WitOwn value1 = WitOwn.of("resource1", 1);
      final WitOwn value2 = WitOwn.of("resource2", 1);
      assertNotEquals(value1, value2, "Different resource types should not be equal");
    }

    @Test
    @DisplayName("different indices should not be equal")
    void differentIndicesShouldNotBeEqual() {
      final WitOwn value1 = WitOwn.of("resource", 1);
      final WitOwn value2 = WitOwn.of("resource", 2);
      assertNotEquals(value1, value2, "Different indices should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same values should have same hash code")
    void sameValuesShouldHaveSameHashCode() {
      final WitOwn value1 = WitOwn.of("resource", 1);
      final WitOwn value2 = WitOwn.of("resource", 1);
      assertEquals(value1.hashCode(), value2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain resource type")
    void toStringShouldContainResourceType() {
      final WitOwn value = WitOwn.of("file-handle", 42);
      final String str = value.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("file-handle"), "Should contain resource type");
    }

    @Test
    @DisplayName("toString should contain index")
    void toStringShouldContainIndex() {
      final WitOwn value = WitOwn.of("resource", 42);
      final String str = value.toString();
      assertTrue(str.contains("42"), "Should contain index");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitOwn value = WitOwn.of("resource", 1);
      assertNotNull(value.getType(), "Should have WitType");
    }
  }
}
