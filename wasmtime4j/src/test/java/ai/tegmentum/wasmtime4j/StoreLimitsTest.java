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

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StoreLimits} class.
 *
 * <p>StoreLimits configures resource limits on WebAssembly stores including memory size, table
 * elements, and instance counts. Uses inner Builder pattern.
 */
@DisplayName("StoreLimits Tests")
class StoreLimitsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(StoreLimits.class.getModifiers()),
          "StoreLimits should be public");
      assertTrue(
          Modifier.isFinal(StoreLimits.class.getModifiers()),
          "StoreLimits should be final");
    }

    @Test
    @DisplayName("should have static builder() factory method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final var method = StoreLimits.class.getMethod("builder");
      assertNotNull(method, "builder() method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder() should be static");
    }
  }

  @Nested
  @DisplayName("Builder Default Values Tests")
  class BuilderDefaultValuesTests {

    @Test
    @DisplayName("should build with all defaults as zero (unlimited)")
    void shouldBuildWithAllDefaultsAsZero() {
      final StoreLimits limits = StoreLimits.builder().build();

      assertNotNull(limits, "StoreLimits instance should not be null");
      assertEquals(0L, limits.getMemorySize(), "Default memorySize should be 0 (unlimited)");
      assertEquals(0L, limits.getTableElements(), "Default tableElements should be 0 (unlimited)");
      assertEquals(0L, limits.getInstances(), "Default instances should be 0 (unlimited)");
    }
  }

  @Nested
  @DisplayName("Builder Fluent API Tests")
  class BuilderFluentApiTests {

    @Test
    @DisplayName("should set memorySize via builder")
    void shouldSetMemorySize() {
      final long memorySizeBytes = 10L * 1024 * 1024; // 10 MB
      final StoreLimits limits = StoreLimits.builder()
          .memorySize(memorySizeBytes)
          .build();

      assertEquals(
          memorySizeBytes, limits.getMemorySize(),
          "memorySize should be 10 MB in bytes");
    }

    @Test
    @DisplayName("should set tableElements via builder")
    void shouldSetTableElements() {
      final long elements = 5000L;
      final StoreLimits limits = StoreLimits.builder()
          .tableElements(elements)
          .build();

      assertEquals(
          elements, limits.getTableElements(),
          "tableElements should be 5000");
    }

    @Test
    @DisplayName("should set instances via builder")
    void shouldSetInstances() {
      final long count = 42L;
      final StoreLimits limits = StoreLimits.builder()
          .instances(count)
          .build();

      assertEquals(count, limits.getInstances(), "instances should be 42");
    }

    @Test
    @DisplayName("should support method chaining for all builder methods")
    void shouldSupportMethodChaining() {
      final StoreLimits limits = StoreLimits.builder()
          .memorySize(1024 * 1024L)
          .tableElements(1000L)
          .instances(10L)
          .build();

      assertEquals(
          1024 * 1024L, limits.getMemorySize(),
          "memorySize should be 1 MB");
      assertEquals(1000L, limits.getTableElements(), "tableElements should be 1000");
      assertEquals(10L, limits.getInstances(), "instances should be 10");
    }
  }

  @Nested
  @DisplayName("Builder Validation Tests")
  class BuilderValidationTests {

    @Test
    @DisplayName("should reject negative memorySize")
    void shouldRejectNegativeMemorySize() {
      final IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          () -> StoreLimits.builder().memorySize(-1L),
          "Negative memorySize should throw IllegalArgumentException");
      assertTrue(
          exception.getMessage().toLowerCase().contains("negative"),
          "Exception message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject negative tableElements")
    void shouldRejectNegativeTableElements() {
      final IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          () -> StoreLimits.builder().tableElements(-100L),
          "Negative tableElements should throw IllegalArgumentException");
      assertTrue(
          exception.getMessage().toLowerCase().contains("negative"),
          "Exception message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should reject negative instances")
    void shouldRejectNegativeInstances() {
      final IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          () -> StoreLimits.builder().instances(-5L),
          "Negative instances should throw IllegalArgumentException");
      assertTrue(
          exception.getMessage().toLowerCase().contains("negative"),
          "Exception message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should accept zero values as unlimited")
    void shouldAcceptZeroValues() {
      final StoreLimits limits = StoreLimits.builder()
          .memorySize(0L)
          .tableElements(0L)
          .instances(0L)
          .build();

      assertEquals(0L, limits.getMemorySize(), "Zero memorySize should be accepted");
      assertEquals(0L, limits.getTableElements(), "Zero tableElements should be accepted");
      assertEquals(0L, limits.getInstances(), "Zero instances should be accepted");
    }
  }

  @Nested
  @DisplayName("Boundary Value Tests")
  class BoundaryValueTests {

    @Test
    @DisplayName("should accept Long.MAX_VALUE for memorySize")
    void shouldAcceptMaxValueForMemorySize() {
      final StoreLimits limits = StoreLimits.builder()
          .memorySize(Long.MAX_VALUE)
          .build();

      assertEquals(
          Long.MAX_VALUE, limits.getMemorySize(),
          "Long.MAX_VALUE should be accepted for memorySize");
    }

    @Test
    @DisplayName("should accept value of 1 for all limits")
    void shouldAcceptMinimumPositiveValues() {
      final StoreLimits limits = StoreLimits.builder()
          .memorySize(1L)
          .tableElements(1L)
          .instances(1L)
          .build();

      assertEquals(1L, limits.getMemorySize(), "memorySize of 1 should be accepted");
      assertEquals(1L, limits.getTableElements(), "tableElements of 1 should be accepted");
      assertEquals(1L, limits.getInstances(), "instances of 1 should be accepted");
    }
  }
}
