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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiProcessId} class.
 *
 * <p>Verifies factory creation, validation, accessors, equals/hashCode, and toString behavior.
 */
@DisplayName("WasiProcessId Tests")
class WasiProcessIdTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiProcessId.class.getModifiers()),
          "WasiProcessId should be a final class");
    }

    @Test
    @DisplayName("constructor should be private")
    void constructorShouldBePrivate() throws NoSuchMethodException {
      final Constructor<WasiProcessId> constructor =
          WasiProcessId.class.getDeclaredConstructor(long.class);
      assertTrue(
          Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("of() should create process ID with value zero")
    void ofShouldCreateWithZero() {
      final WasiProcessId pid = WasiProcessId.of(0);
      assertEquals(0L, pid.getId(), "Process ID should be 0");
    }

    @Test
    @DisplayName("of() should create process ID with positive value")
    void ofShouldCreateWithPositiveValue() {
      final WasiProcessId pid = WasiProcessId.of(42);
      assertEquals(42L, pid.getId(), "Process ID should be 42");
    }

    @Test
    @DisplayName("of() should create process ID with large value")
    void ofShouldCreateWithLargeValue() {
      final long largeId = Long.MAX_VALUE;
      final WasiProcessId pid = WasiProcessId.of(largeId);
      assertEquals(largeId, pid.getId(), "Process ID should be Long.MAX_VALUE");
    }

    @Test
    @DisplayName("of() should throw for negative value")
    void ofShouldThrowForNegativeValue() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiProcessId.of(-1),
              "Should throw for negative ID");
      assertTrue(
          exception.getMessage().contains("-1"),
          "Message should contain the invalid value: " + exception.getMessage());
    }

    @Test
    @DisplayName("of() should throw for Long.MIN_VALUE")
    void ofShouldThrowForMinValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiProcessId.of(Long.MIN_VALUE),
          "Should throw for Long.MIN_VALUE");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equal IDs should be equal")
    void equalIdsShouldBeEqual() {
      final WasiProcessId pid1 = WasiProcessId.of(100);
      final WasiProcessId pid2 = WasiProcessId.of(100);
      assertEquals(pid1, pid2, "Process IDs with same value should be equal");
    }

    @Test
    @DisplayName("equal IDs should have same hashCode")
    void equalIdsShouldHaveSameHashCode() {
      final WasiProcessId pid1 = WasiProcessId.of(100);
      final WasiProcessId pid2 = WasiProcessId.of(100);
      assertEquals(
          pid1.hashCode(), pid2.hashCode(),
          "Process IDs with same value should have same hashCode");
    }

    @Test
    @DisplayName("different IDs should not be equal")
    void differentIdsShouldNotBeEqual() {
      final WasiProcessId pid1 = WasiProcessId.of(1);
      final WasiProcessId pid2 = WasiProcessId.of(2);
      assertNotEquals(pid1, pid2, "Process IDs with different values should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final WasiProcessId pid = WasiProcessId.of(1);
      assertNotEquals(null, pid, "Process ID should not equal null");
    }

    @Test
    @DisplayName("should not equal different type")
    void shouldNotEqualDifferentType() {
      final WasiProcessId pid = WasiProcessId.of(1);
      assertNotEquals("1", pid, "Process ID should not equal a String");
    }

    @Test
    @DisplayName("should equal itself")
    void shouldEqualItself() {
      final WasiProcessId pid = WasiProcessId.of(42);
      assertEquals(pid, pid, "Process ID should equal itself");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain the ID value")
    void toStringShouldContainIdValue() {
      final WasiProcessId pid = WasiProcessId.of(42);
      final String result = pid.toString();
      assertTrue(
          result.contains("42"), "toString should contain the ID value: " + result);
    }

    @Test
    @DisplayName("toString should contain class name")
    void toStringShouldContainClassName() {
      final WasiProcessId pid = WasiProcessId.of(0);
      final String result = pid.toString();
      assertTrue(
          result.contains("WasiProcessId"),
          "toString should contain the class name: " + result);
    }

    @Test
    @DisplayName("toString should match expected format")
    void toStringShouldMatchExpectedFormat() {
      final WasiProcessId pid = WasiProcessId.of(99);
      assertEquals(
          "WasiProcessId{id=99}", pid.toString(),
          "toString should match expected format");
    }
  }
}
