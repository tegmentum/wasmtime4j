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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.config.StoreLimits;
import ai.tegmentum.wasmtime4j.config.StoreLimitsBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StoreLimitsBuilder} class.
 *
 * <p>StoreLimitsBuilder is a standalone fluent builder for creating StoreLimits instances with
 * additional fields (tables, memories) beyond the inner Builder.
 */
@DisplayName("StoreLimitsBuilder Tests")
class StoreLimitsBuilderTest {

  @Nested
  @DisplayName("Default Values Tests")
  class DefaultValuesTests {

    @Test
    @DisplayName("should have all defaults as zero (unlimited)")
    void shouldHaveAllDefaultsAsZero() {
      final StoreLimitsBuilder builder = new StoreLimitsBuilder();

      assertEquals(0L, builder.getMemorySize(), "Default memorySize should be 0");
      assertEquals(0L, builder.getTableElements(), "Default tableElements should be 0");
      assertEquals(0L, builder.getInstances(), "Default instances should be 0");
      assertEquals(0L, builder.getTables(), "Default tables should be 0");
      assertEquals(0L, builder.getMemories(), "Default memories should be 0");
    }
  }

  @Nested
  @DisplayName("Fluent API Tests")
  class FluentApiTests {

    @Test
    @DisplayName("should set and get memorySize")
    void shouldSetAndGetMemorySize() {
      final long bytes = 10L * 1024 * 1024;
      final StoreLimitsBuilder builder = new StoreLimitsBuilder().memorySize(bytes);

      assertEquals(bytes, builder.getMemorySize(), "memorySize should be set to 10 MB");
    }

    @Test
    @DisplayName("should set and get tableElements")
    void shouldSetAndGetTableElements() {
      final long elements = 5000L;
      final StoreLimitsBuilder builder = new StoreLimitsBuilder().tableElements(elements);

      assertEquals(elements, builder.getTableElements(), "tableElements should be 5000");
    }

    @Test
    @DisplayName("should set and get instances")
    void shouldSetAndGetInstances() {
      final long count = 10L;
      final StoreLimitsBuilder builder = new StoreLimitsBuilder().instances(count);

      assertEquals(count, builder.getInstances(), "instances should be 10");
    }

    @Test
    @DisplayName("should set and get tables")
    void shouldSetAndGetTables() {
      final long count = 5L;
      final StoreLimitsBuilder builder = new StoreLimitsBuilder().tables(count);

      assertEquals(count, builder.getTables(), "tables should be 5");
    }

    @Test
    @DisplayName("should set and get memories")
    void shouldSetAndGetMemories() {
      final long count = 2L;
      final StoreLimitsBuilder builder = new StoreLimitsBuilder().memories(count);

      assertEquals(count, builder.getMemories(), "memories should be 2");
    }

    @Test
    @DisplayName("should support full method chaining")
    void shouldSupportFullMethodChaining() {
      final StoreLimitsBuilder builder =
          new StoreLimitsBuilder()
              .memorySize(1024L * 1024)
              .tableElements(10000L)
              .instances(10L)
              .tables(5L)
              .memories(2L);

      assertEquals(1024L * 1024, builder.getMemorySize(), "memorySize should be 1 MB");
      assertEquals(10000L, builder.getTableElements(), "tableElements should be 10000");
      assertEquals(10L, builder.getInstances(), "instances should be 10");
      assertEquals(5L, builder.getTables(), "tables should be 5");
      assertEquals(2L, builder.getMemories(), "memories should be 2");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("should reject negative memorySize")
    void shouldRejectNegativeMemorySize() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new StoreLimitsBuilder().memorySize(-1L),
              "Negative memorySize should throw IllegalArgumentException");
      assertTrue(
          exception.getMessage().contains("negative"),
          "Message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject negative tableElements")
    void shouldRejectNegativeTableElements() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new StoreLimitsBuilder().tableElements(-1L),
              "Negative tableElements should throw IllegalArgumentException");
      assertTrue(
          exception.getMessage().contains("negative"),
          "Message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject negative instances")
    void shouldRejectNegativeInstances() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new StoreLimitsBuilder().instances(-1L),
              "Negative instances should throw IllegalArgumentException");
      assertTrue(
          exception.getMessage().contains("negative"),
          "Message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject negative tables")
    void shouldRejectNegativeTables() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new StoreLimitsBuilder().tables(-1L),
              "Negative tables should throw IllegalArgumentException");
      assertTrue(
          exception.getMessage().contains("negative"),
          "Message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject negative memories")
    void shouldRejectNegativeMemories() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new StoreLimitsBuilder().memories(-1L),
              "Negative memories should throw IllegalArgumentException");
      assertTrue(
          exception.getMessage().contains("negative"),
          "Message should mention 'negative': " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Build Tests")
  class BuildTests {

    @Test
    @DisplayName("should build StoreLimits from configured builder")
    void shouldBuildStoreLimitsFromConfiguredBuilder() {
      final StoreLimits limits =
          new StoreLimitsBuilder().memorySize(2048L).tableElements(500L).instances(3L).build();

      assertNotNull(limits, "Built StoreLimits should not be null");
      assertEquals(2048L, limits.getMemorySize(), "memorySize should match builder value");
      assertEquals(500L, limits.getTableElements(), "tableElements should match builder value");
      assertEquals(3L, limits.getInstances(), "instances should match builder value");
    }

    @Test
    @DisplayName("should build StoreLimits with default values")
    void shouldBuildStoreLimitsWithDefaults() {
      final StoreLimits limits = new StoreLimitsBuilder().build();

      assertNotNull(limits, "Built StoreLimits should not be null");
      assertEquals(0L, limits.getMemorySize(), "Default memorySize should be 0");
      assertEquals(0L, limits.getTableElements(), "Default tableElements should be 0");
      assertEquals(0L, limits.getInstances(), "Default instances should be 0");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce readable toString output")
    void shouldProduceReadableToString() {
      final StoreLimitsBuilder builder =
          new StoreLimitsBuilder()
              .memorySize(1024L)
              .tableElements(100L)
              .instances(5L)
              .tables(3L)
              .memories(1L);

      final String result = builder.toString();
      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("1024"), "toString should contain memorySize value");
      assertTrue(result.contains("100"), "toString should contain tableElements value");
      assertTrue(result.contains("StoreLimitsBuilder"), "toString should contain class name");
    }
  }
}
