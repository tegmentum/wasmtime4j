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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link WasiProcessId} class.
 *
 * <p>WasiProcessId represents a WASI process identifier for process management.
 */
@DisplayName("WasiProcessId Tests")
class WasiProcessIdTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiProcessId.class.getModifiers()), "WasiProcessId should be public");
      assertTrue(
          Modifier.isFinal(WasiProcessId.class.getModifiers()), "WasiProcessId should be final");
      assertFalse(WasiProcessId.class.isInterface(), "WasiProcessId should not be an interface");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("of should create instance with valid id")
    void ofShouldCreateInstanceWithValidId() {
      final WasiProcessId pid = WasiProcessId.of(12345L);

      assertNotNull(pid, "ProcessId should not be null");
      assertEquals(12345L, pid.getId(), "Id should match");
    }

    @Test
    @DisplayName("of should create instance with zero id")
    void ofShouldCreateInstanceWithZeroId() {
      final WasiProcessId pid = WasiProcessId.of(0L);

      assertNotNull(pid, "ProcessId should not be null");
      assertEquals(0L, pid.getId(), "Id should be 0");
    }

    @Test
    @DisplayName("of should create instance with max long value")
    void ofShouldCreateInstanceWithMaxLongValue() {
      final WasiProcessId pid = WasiProcessId.of(Long.MAX_VALUE);

      assertNotNull(pid, "ProcessId should not be null");
      assertEquals(Long.MAX_VALUE, pid.getId(), "Id should match max long");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1L, -100L, Long.MIN_VALUE})
    @DisplayName("of should throw for negative id")
    void ofShouldThrowForNegativeId(final long negativeId) {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiProcessId.of(negativeId),
              "Should throw for negative id");
      assertTrue(
          exception.getMessage().contains(String.valueOf(negativeId)),
          "Exception should contain the negative id");
    }
  }

  @Nested
  @DisplayName("getId Method Tests")
  class GetIdMethodTests {

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 42L, 999999L})
    @DisplayName("getId should return the correct id")
    void getIdShouldReturnTheCorrectId(final long id) {
      final WasiProcessId pid = WasiProcessId.of(id);
      assertEquals(id, pid.getId(), "getId should return the correct id");
    }
  }

  @Nested
  @DisplayName("Equals Method Tests")
  class EqualsMethodTests {

    @Test
    @DisplayName("equals should return true for same id")
    void equalsShouldReturnTrueForSameId() {
      final WasiProcessId pid1 = WasiProcessId.of(100L);
      final WasiProcessId pid2 = WasiProcessId.of(100L);

      assertEquals(pid1, pid2, "ProcessIds with same id should be equal");
    }

    @Test
    @DisplayName("equals should return false for different id")
    void equalsShouldReturnFalseForDifferentId() {
      final WasiProcessId pid1 = WasiProcessId.of(100L);
      final WasiProcessId pid2 = WasiProcessId.of(200L);

      assertNotEquals(pid1, pid2, "ProcessIds with different id should not be equal");
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final WasiProcessId pid = WasiProcessId.of(100L);

      assertEquals(pid, pid, "Same instance should be equal to itself");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final WasiProcessId pid = WasiProcessId.of(100L);

      assertFalse(pid.equals(null), "ProcessId should not be equal to null");
    }

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
      final WasiProcessId pid = WasiProcessId.of(100L);

      assertFalse(pid.equals("100"), "ProcessId should not be equal to String");
      assertFalse(pid.equals(100L), "ProcessId should not be equal to Long");
    }
  }

  @Nested
  @DisplayName("HashCode Method Tests")
  class HashCodeMethodTests {

    @Test
    @DisplayName("hashCode should be consistent for same id")
    void hashCodeShouldBeConsistentForSameId() {
      final WasiProcessId pid1 = WasiProcessId.of(100L);
      final WasiProcessId pid2 = WasiProcessId.of(100L);

      assertEquals(pid1.hashCode(), pid2.hashCode(), "Equal ProcessIds should have same hashCode");
    }

    @Test
    @DisplayName("hashCode should match Long.hashCode for id")
    void hashCodeShouldMatchLongHashCodeForId() {
      final long id = 12345L;
      final WasiProcessId pid = WasiProcessId.of(id);

      assertEquals(Long.hashCode(id), pid.hashCode(), "HashCode should match Long.hashCode of id");
    }
  }

  @Nested
  @DisplayName("ToString Method Tests")
  class ToStringMethodTests {

    @Test
    @DisplayName("toString should contain id")
    void toStringShouldContainId() {
      final WasiProcessId pid = WasiProcessId.of(42L);
      final String str = pid.toString();

      assertTrue(str.contains("42"), "Should contain the id");
      assertTrue(str.contains("WasiProcessId"), "Should contain class name");
    }

    @Test
    @DisplayName("toString should contain id for large value")
    void toStringShouldContainIdForLargeValue() {
      final WasiProcessId pid = WasiProcessId.of(9999999999L);
      final String str = pid.toString();

      assertTrue(str.contains("9999999999"), "Should contain the large id");
    }
  }
}
