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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentResourceHandle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitBorrow} class.
 *
 * <p>WitBorrow represents a WIT borrowed resource handle value. When a component receives a
 * borrowed handle, it can use the resource but does not take ownership.
 */
@DisplayName("WitBorrow Tests")
class WitBorrowTest {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create WitBorrow from resource type and index")
    void shouldCreateFromResourceTypeAndIndex() {
      final WitBorrow value = WitBorrow.of("file-handle", 42);
      assertNotNull(value, "Should create WitBorrow");
      assertEquals("file-handle", value.getResourceType(), "Should have resource type");
      assertEquals(42, value.getIndex(), "Should have index");
    }

    @Test
    @DisplayName("should create WitBorrow with host object")
    void shouldCreateWithHostObject() {
      final String hostObject = "test-host";
      final WitBorrow value = WitBorrow.ofWithHost("resource", 1, hostObject);
      assertNotNull(value, "Should create WitBorrow with host object");
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
          () -> WitBorrow.of("resource", -1),
          "Should reject negative index");
    }

    @Test
    @DisplayName("fromHandle should reject null handle")
    void fromHandleShouldRejectNullHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitBorrow.fromHandle(null),
          "Should reject null handle");
    }

    @Test
    @DisplayName("fromHandle should reject owned handle")
    void fromHandleShouldRejectOwnedHandle() {
      final ComponentResourceHandle owned = ComponentResourceHandle.own("resource", 1);
      assertThrows(
          IllegalArgumentException.class,
          () -> WitBorrow.fromHandle(owned),
          "Should reject owned handle");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getHandle should return the resource handle")
    void getHandleShouldReturnResourceHandle() {
      final WitBorrow value = WitBorrow.of("resource", 1);
      assertNotNull(value.getHandle(), "Should return handle");
      assertFalse(value.getHandle().isOwned(), "Handle should not be owned");
    }

    @Test
    @DisplayName("getResourceType should return correct type")
    void getResourceTypeShouldReturnCorrectType() {
      final WitBorrow value = WitBorrow.of("my-resource", 5);
      assertEquals("my-resource", value.getResourceType(), "Should return resource type");
    }

    @Test
    @DisplayName("getIndex should return correct index")
    void getIndexShouldReturnCorrectIndex() {
      final WitBorrow value = WitBorrow.of("resource", 99);
      assertEquals(99, value.getIndex(), "Should return index");
    }

    @Test
    @DisplayName("toJava should return ComponentResourceHandle")
    void toJavaShouldReturnComponentResourceHandle() {
      final WitBorrow value = WitBorrow.of("resource", 1);
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
      final WitBorrow value1 = WitBorrow.of("resource", 1);
      final WitBorrow value2 = WitBorrow.of("resource", 1);
      assertEquals(value1, value2, "Same values should be equal");
    }

    @Test
    @DisplayName("different resource types should not be equal")
    void differentResourceTypesShouldNotBeEqual() {
      final WitBorrow value1 = WitBorrow.of("resource1", 1);
      final WitBorrow value2 = WitBorrow.of("resource2", 1);
      assertNotEquals(value1, value2, "Different resource types should not be equal");
    }

    @Test
    @DisplayName("different indices should not be equal")
    void differentIndicesShouldNotBeEqual() {
      final WitBorrow value1 = WitBorrow.of("resource", 1);
      final WitBorrow value2 = WitBorrow.of("resource", 2);
      assertNotEquals(value1, value2, "Different indices should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same values should have same hash code")
    void sameValuesShouldHaveSameHashCode() {
      final WitBorrow value1 = WitBorrow.of("resource", 1);
      final WitBorrow value2 = WitBorrow.of("resource", 1);
      assertEquals(value1.hashCode(), value2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain resource type")
    void toStringShouldContainResourceType() {
      final WitBorrow value = WitBorrow.of("file-handle", 42);
      final String str = value.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("file-handle"), "Should contain resource type");
    }

    @Test
    @DisplayName("toString should contain index")
    void toStringShouldContainIndex() {
      final WitBorrow value = WitBorrow.of("resource", 42);
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
      final WitBorrow value = WitBorrow.of("resource", 1);
      assertNotNull(value.getType(), "Should have WitType");
    }
  }

  @Nested
  @DisplayName("WitOwn vs WitBorrow Distinction Tests")
  class OwnershipDistinctionTests {

    @Test
    @DisplayName("WitBorrow and WitOwn should not be equal even with same values")
    void borrowAndOwnShouldNotBeEqual() {
      final WitBorrow borrow = WitBorrow.of("resource", 1);
      final WitOwn own = WitOwn.of("resource", 1);
      assertNotEquals(borrow, own, "WitBorrow and WitOwn should not be equal");
    }
  }

  @Nested
  @DisplayName("Surviving Mutant Killer Tests")
  class SurvivingMutantKillerTests {

    @Test
    @DisplayName("constructor must reject null handle - line 66")
    void constructorMustRejectNullHandle() {
      // Targets line 66: handle == null check
      assertThrows(
          IllegalArgumentException.class,
          () -> WitBorrow.fromHandle(null),
          "Must reject null handle");
    }

    @Test
    @DisplayName("constructor must reject owned handle - line 69")
    void constructorMustRejectOwnedHandle() {
      // Targets line 69: handle.isOwned() check
      final ComponentResourceHandle owned = ComponentResourceHandle.own("resource", 1);
      assertThrows(
          IllegalArgumentException.class,
          () -> WitBorrow.fromHandle(owned),
          "Must reject owned handle for WitBorrow");
    }

    @Test
    @DisplayName("fromHandle must check isOwned conditional - line 133")
    void fromHandleMustCheckIsOwnedConditional() {
      // Targets line 133: handle.isOwned() conditional negation
      // Valid borrowed handle should work
      final ComponentResourceHandle borrowed = ComponentResourceHandle.borrow("resource", 1);
      final WitBorrow value = WitBorrow.fromHandle(borrowed);
      assertNotNull(value, "Valid borrowed handle should create WitBorrow");
      assertFalse(value.getHandle().isOwned(), "Handle must not be owned");

      // Owned handle should be rejected by fromHandle's own check
      final ComponentResourceHandle owned = ComponentResourceHandle.own("resource", 1);
      assertThrows(
          IllegalArgumentException.class,
          () -> WitBorrow.fromHandle(owned),
          "fromHandle must reject owned handle");
    }

    @Test
    @DisplayName("validate must check handle index boundary - line 190")
    void validateMustCheckHandleIndexBoundary() {
      // Targets line 190: handle.getNativeHandle() < 0 && handle.getIndex() < 0
      // Valid positive and zero index must pass
      final WitBorrow zero = WitBorrow.of("resource", 0);
      assertEquals(0, zero.getIndex(), "Zero index must be accepted");

      final WitBorrow positive = WitBorrow.of("resource", 100);
      assertEquals(100, positive.getIndex(), "Positive index must be accepted");

      // Negative index without native handle must be rejected
      assertThrows(
          IllegalArgumentException.class,
          () -> WitBorrow.of("resource", -1),
          "Negative index must be rejected");
    }

    @Test
    @DisplayName("long handle factory should work with positive values")
    void longHandleFactoryShouldWork() {
      final WitBorrow value = WitBorrow.of("resource", 42L);
      assertNotNull(value, "Long handle factory should create WitBorrow");
      assertEquals("resource", value.getResourceType(), "Resource type must match");
    }
  }
}
