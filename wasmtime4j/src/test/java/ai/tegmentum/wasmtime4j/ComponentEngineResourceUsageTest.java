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

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentEngineResourceUsage} class.
 *
 * <p>ComponentEngineResourceUsage provides metrics about engine-level resource consumption.
 */
@DisplayName("ComponentEngineResourceUsage Tests")
class ComponentEngineResourceUsageTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentEngineResourceUsage.class.getModifiers()),
          "ComponentEngineResourceUsage should be public");
      assertTrue(
          Modifier.isFinal(ComponentEngineResourceUsage.class.getModifiers()),
          "ComponentEngineResourceUsage should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final var usage = new ComponentEngineResourceUsage(10, 5, 1024L * 1024, 512L * 1024);

      assertEquals(10, usage.getTotalComponents(), "Total components should be 10");
      assertEquals(5, usage.getActiveInstances(), "Active instances should be 5");
      assertEquals(1024L * 1024, usage.getTotalMemoryUsedBytes(), "Memory should be 1MB");
      assertEquals(512L * 1024, usage.getTotalStackUsedBytes(), "Stack should be 512KB");
    }

    @Test
    @DisplayName("should set timestamp on creation")
    void shouldSetTimestampOnCreation() {
      final long beforeCreation = System.currentTimeMillis();
      final var usage = new ComponentEngineResourceUsage(10, 5, 1024L, 512L);
      final long afterCreation = System.currentTimeMillis();

      assertTrue(
          usage.getTimestamp() >= beforeCreation, "Timestamp should be >= creation start time");
      assertTrue(usage.getTimestamp() <= afterCreation, "Timestamp should be <= creation end time");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getTotalComponents should return correct value")
    void getTotalComponentsShouldReturnCorrectValue() {
      final var usage = new ComponentEngineResourceUsage(100, 0, 0L, 0L);

      assertEquals(100, usage.getTotalComponents(), "Should return 100 components");
    }

    @Test
    @DisplayName("getActiveInstances should return correct value")
    void getActiveInstancesShouldReturnCorrectValue() {
      final var usage = new ComponentEngineResourceUsage(0, 50, 0L, 0L);

      assertEquals(50, usage.getActiveInstances(), "Should return 50 active instances");
    }

    @Test
    @DisplayName("getTotalMemoryUsedBytes should return correct value")
    void getTotalMemoryUsedBytesShouldReturnCorrectValue() {
      final var usage = new ComponentEngineResourceUsage(0, 0, 1024L * 1024 * 1024, 0L);

      assertEquals(
          1024L * 1024 * 1024, usage.getTotalMemoryUsedBytes(), "Should return 1GB memory");
    }

    @Test
    @DisplayName("getTotalStackUsedBytes should return correct value")
    void getTotalStackUsedBytesShouldReturnCorrectValue() {
      final var usage = new ComponentEngineResourceUsage(0, 0, 0L, 256L * 1024);

      assertEquals(256L * 1024, usage.getTotalStackUsedBytes(), "Should return 256KB stack");
    }

    @Test
    @DisplayName("getTimestamp should return non-null value")
    void getTimestampShouldReturnNonNullValue() {
      final var usage = new ComponentEngineResourceUsage(0, 0, 0L, 0L);

      assertTrue(usage.getTimestamp() > 0, "Timestamp should be positive");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle zero values")
    void shouldHandleZeroValues() {
      final var usage = new ComponentEngineResourceUsage(0, 0, 0L, 0L);

      assertEquals(0, usage.getTotalComponents(), "Zero components should work");
      assertEquals(0, usage.getActiveInstances(), "Zero instances should work");
      assertEquals(0L, usage.getTotalMemoryUsedBytes(), "Zero memory should work");
      assertEquals(0L, usage.getTotalStackUsedBytes(), "Zero stack should work");
    }

    @Test
    @DisplayName("should handle max int values")
    void shouldHandleMaxIntValues() {
      final var usage =
          new ComponentEngineResourceUsage(Integer.MAX_VALUE, Integer.MAX_VALUE, 0L, 0L);

      assertEquals(Integer.MAX_VALUE, usage.getTotalComponents(), "Should handle max int");
      assertEquals(Integer.MAX_VALUE, usage.getActiveInstances(), "Should handle max int");
    }

    @Test
    @DisplayName("should handle max long values")
    void shouldHandleMaxLongValues() {
      final var usage = new ComponentEngineResourceUsage(0, 0, Long.MAX_VALUE, Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, usage.getTotalMemoryUsedBytes(), "Should handle max long");
      assertEquals(Long.MAX_VALUE, usage.getTotalStackUsedBytes(), "Should handle max long");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final var usage = new ComponentEngineResourceUsage(10, 5, 1024L, 512L);

      final String result = usage.toString();

      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("10"), "Should contain component count");
      assertTrue(result.contains("5"), "Should contain instance count");
      assertTrue(result.contains("1024"), "Should contain memory");
      assertTrue(result.contains("512"), "Should contain stack");
    }
  }
}
