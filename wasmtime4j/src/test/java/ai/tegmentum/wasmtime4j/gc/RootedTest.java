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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Rooted} class.
 *
 * <p>Rooted represents a rooted reference to a GC-managed WebAssembly object.
 */
@DisplayName("Rooted Tests")
class RootedTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create rooted with value and root id")
    void shouldCreateRootedWithValueAndRootId() {
      final String testValue = "test";
      final long rootId = 42L;
      final Rooted<String> rooted = new Rooted<>(testValue, rootId);

      assertNotNull(rooted, "Rooted should be created");
      assertEquals(rootId, rooted.getRootId(), "Root ID should match");
      assertTrue(rooted.isValid(), "Rooted should be valid initially");
    }

    @Test
    @DisplayName("should throw NPE for null value")
    void shouldThrowNpeForNullValue() {
      assertThrows(
          NullPointerException.class,
          () -> new Rooted<>(null, 1L),
          "Constructor should throw NPE for null value");
    }
  }

  @Nested
  @DisplayName("Validity Tests")
  class ValidityTests {

    @Test
    @DisplayName("new rooted should be valid")
    void newRootedShouldBeValid() {
      final Rooted<String> rooted = new Rooted<>("test", 1L);
      assertTrue(rooted.isValid(), "New rooted should be valid");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same value and rootId should be equal")
    void sameValueAndRootIdShouldBeEqual() {
      final Rooted<String> rooted1 = new Rooted<>("test", 1L);
      final Rooted<String> rooted2 = new Rooted<>("test", 1L);
      assertEquals(rooted1, rooted2, "Same value and rootId should be equal");
    }

    @Test
    @DisplayName("different rootId should not be equal")
    void differentRootIdShouldNotBeEqual() {
      final Rooted<String> rooted1 = new Rooted<>("test", 1L);
      final Rooted<String> rooted2 = new Rooted<>("test", 2L);
      assertNotEquals(rooted1, rooted2, "Different rootId should not be equal");
    }

    @Test
    @DisplayName("different value should not be equal")
    void differentValueShouldNotBeEqual() {
      final Rooted<String> rooted1 = new Rooted<>("test1", 1L);
      final Rooted<String> rooted2 = new Rooted<>("test2", 1L);
      assertNotEquals(rooted1, rooted2, "Different value should not be equal");
    }

    @Test
    @DisplayName("equals with non-Rooted should return false")
    void equalsWithNonRootedShouldReturnFalse() {
      final Rooted<String> rooted = new Rooted<>("test", 1L);
      assertFalse(rooted.equals("not a Rooted"), "equals with non-Rooted should return false");
    }

    @Test
    @DisplayName("equal objects should have same hashCode")
    void equalObjectsShouldHaveSameHashCode() {
      final Rooted<String> rooted1 = new Rooted<>("test", 1L);
      final Rooted<String> rooted2 = new Rooted<>("test", 1L);
      assertEquals(
          rooted1.hashCode(), rooted2.hashCode(), "Equal objects should have same hashCode");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain 'Rooted'")
    void toStringShouldContainRooted() {
      final Rooted<String> rooted = new Rooted<>("test", 1L);
      final String str = rooted.toString();
      assertTrue(str.contains("Rooted"), "toString should contain 'Rooted': " + str);
    }

    @Test
    @DisplayName("toString should contain rootId")
    void toStringShouldContainRootId() {
      final Rooted<String> rooted = new Rooted<>("test", 42L);
      final String str = rooted.toString();
      assertTrue(str.contains("42"), "toString should contain rootId: " + str);
    }
  }
}
