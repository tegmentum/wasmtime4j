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
 * Tests for {@link ComponentResourceUsage} class.
 *
 * <p>ComponentResourceUsage provides metrics about component resource consumption.
 */
@DisplayName("ComponentResourceUsage Tests")
class ComponentResourceUsageTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentResourceUsage.class.getModifiers()),
          "ComponentResourceUsage should be public");
      assertTrue(
          Modifier.isFinal(ComponentResourceUsage.class.getModifiers()),
          "ComponentResourceUsage should be final");
    }
  }

  @Nested
  @DisplayName("Full Constructor Tests")
  class FullConstructorTests {

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final var usage = new ComponentResourceUsage("comp-1", 1024L * 1024, 512L * 1024, 50.5, 10);

      assertEquals("comp-1", usage.getComponentId(), "Component ID should match");
      assertEquals(1024L * 1024, usage.getMemoryUsedBytes(), "Memory should be 1MB");
      assertEquals(512L * 1024, usage.getStackUsedBytes(), "Stack should be 512KB");
      assertEquals(50.5, usage.getCpuUsagePercent(), 0.001, "CPU usage should be 50.5%");
      assertEquals(10, usage.getActiveInstances(), "Active instances should be 10");
    }

    @Test
    @DisplayName("should set timestamp on creation")
    void shouldSetTimestampOnCreation() {
      final long beforeCreation = System.currentTimeMillis();
      final var usage = new ComponentResourceUsage("comp-1", 0L, 0L, 0.0, 0);
      final long afterCreation = System.currentTimeMillis();

      assertTrue(
          usage.getTimestamp() >= beforeCreation, "Timestamp should be >= creation start time");
      assertTrue(usage.getTimestamp() <= afterCreation, "Timestamp should be <= creation end time");
    }
  }

  @Nested
  @DisplayName("Minimal Constructor Tests")
  class MinimalConstructorTests {

    @Test
    @DisplayName("should create instance with component ID only")
    void shouldCreateInstanceWithComponentIdOnly() {
      final var usage = new ComponentResourceUsage("minimal-comp");

      assertEquals("minimal-comp", usage.getComponentId(), "Component ID should match");
      assertEquals(0L, usage.getMemoryUsedBytes(), "Memory should default to 0");
      assertEquals(0L, usage.getStackUsedBytes(), "Stack should default to 0");
      assertEquals(0.0, usage.getCpuUsagePercent(), 0.001, "CPU should default to 0.0");
      assertEquals(0, usage.getActiveInstances(), "Instances should default to 0");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getComponentId should return correct value")
    void getComponentIdShouldReturnCorrectValue() {
      final var usage = new ComponentResourceUsage("test-component", 0L, 0L, 0.0, 0);

      assertEquals("test-component", usage.getComponentId(), "Should return component ID");
    }

    @Test
    @DisplayName("getMemoryUsedBytes should return correct value")
    void getMemoryUsedBytesShouldReturnCorrectValue() {
      final var usage = new ComponentResourceUsage("comp", 2L * 1024 * 1024 * 1024, 0L, 0.0, 0);

      assertEquals(2L * 1024 * 1024 * 1024, usage.getMemoryUsedBytes(), "Should return 2GB");
    }

    @Test
    @DisplayName("getStackUsedBytes should return correct value")
    void getStackUsedBytesShouldReturnCorrectValue() {
      final var usage = new ComponentResourceUsage("comp", 0L, 1024L * 1024, 0.0, 0);

      assertEquals(1024L * 1024, usage.getStackUsedBytes(), "Should return 1MB stack");
    }

    @Test
    @DisplayName("getCpuUsagePercent should return correct value")
    void getCpuUsagePercentShouldReturnCorrectValue() {
      final var usage = new ComponentResourceUsage("comp", 0L, 0L, 99.9, 0);

      assertEquals(99.9, usage.getCpuUsagePercent(), 0.001, "Should return 99.9%");
    }

    @Test
    @DisplayName("getActiveInstances should return correct value")
    void getActiveInstancesShouldReturnCorrectValue() {
      final var usage = new ComponentResourceUsage("comp", 0L, 0L, 0.0, 100);

      assertEquals(100, usage.getActiveInstances(), "Should return 100 instances");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle zero CPU usage")
    void shouldHandleZeroCpuUsage() {
      final var usage = new ComponentResourceUsage("comp", 0L, 0L, 0.0, 0);

      assertEquals(0.0, usage.getCpuUsagePercent(), 0.001, "Zero CPU should work");
    }

    @Test
    @DisplayName("should handle 100% CPU usage")
    void shouldHandle100PercentCpuUsage() {
      final var usage = new ComponentResourceUsage("comp", 0L, 0L, 100.0, 0);

      assertEquals(100.0, usage.getCpuUsagePercent(), 0.001, "100% CPU should work");
    }

    @Test
    @DisplayName("should handle max long values for memory")
    void shouldHandleMaxLongValuesForMemory() {
      final var usage = new ComponentResourceUsage("comp", Long.MAX_VALUE, Long.MAX_VALUE, 0.0, 0);

      assertEquals(Long.MAX_VALUE, usage.getMemoryUsedBytes(), "Should handle max memory");
      assertEquals(Long.MAX_VALUE, usage.getStackUsedBytes(), "Should handle max stack");
    }

    @Test
    @DisplayName("should handle max int instances")
    void shouldHandleMaxIntInstances() {
      final var usage = new ComponentResourceUsage("comp", 0L, 0L, 0.0, Integer.MAX_VALUE);

      assertEquals(Integer.MAX_VALUE, usage.getActiveInstances(), "Should handle max instances");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final var usage = new ComponentResourceUsage("my-component", 1024L, 512L, 25.5, 5);

      final String result = usage.toString();

      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("my-component"), "Should contain component ID");
      assertTrue(result.contains("1024"), "Should contain memory");
      assertTrue(result.contains("512"), "Should contain stack");
      assertTrue(result.contains("25.5") || result.contains("25,5"), "Should contain CPU");
      assertTrue(result.contains("5"), "Should contain instances");
    }
  }
}
