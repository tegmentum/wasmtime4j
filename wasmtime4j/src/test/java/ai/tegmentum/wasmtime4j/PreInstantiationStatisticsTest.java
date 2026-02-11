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

import ai.tegmentum.wasmtime4j.memory.Memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PreInstantiationStatistics} class.
 *
 * <p>PreInstantiationStatistics provides information about performance and resource usage of
 * pre-instantiation operations. Constructed via Builder pattern.
 */
@DisplayName("PreInstantiationStatistics Tests")
class PreInstantiationStatisticsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PreInstantiationStatistics.class.getModifiers()),
          "PreInstantiationStatistics should be public");
      assertTrue(
          Modifier.isFinal(PreInstantiationStatistics.class.getModifiers()),
          "PreInstantiationStatistics should be final");
    }
  }

  @Nested
  @DisplayName("Builder Default Tests")
  class BuilderDefaultTests {

    @Test
    @DisplayName("builder should create instance with defaults")
    void builderShouldCreateInstanceWithDefaults() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder().build();

      assertNotNull(stats, "Statistics should not be null");
      assertNotNull(stats.getCreationTime(), "Creation time should have a default");
      assertEquals(Duration.ZERO, stats.getPreparationTime(),
          "Default preparation time should be Duration.ZERO");
      assertEquals(0L, stats.getMemoryFootprint(), "Default memory footprint should be 0");
      assertEquals(0L, stats.getFunctionsPrecompiled(),
          "Default functions precompiled should be 0");
      assertEquals(0L, stats.getTotalFunctions(), "Default total functions should be 0");
      assertEquals(0L, stats.getInstancesCreated(), "Default instances created should be 0");
      assertEquals(Duration.ZERO, stats.getAverageInstantiationTime(),
          "Default average instantiation time should be Duration.ZERO");
      assertEquals(0L, stats.getMemoryPoolSize(), "Default memory pool size should be 0");
      assertFalse(stats.isPoolingEnabled(), "Default pooling enabled should be false");
    }

    @Test
    @DisplayName("builder default creationTime should be close to now")
    void builderDefaultCreationTimeShouldBeCloseToNow() {
      final Instant before = Instant.now();
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder().build();
      final Instant after = Instant.now();

      assertTrue(
          !stats.getCreationTime().isBefore(before) && !stats.getCreationTime().isAfter(after),
          "Default creation time should be within the test execution window");
    }
  }

  @Nested
  @DisplayName("Builder Setter Tests")
  class BuilderSetterTests {

    @Test
    @DisplayName("should set creationTime via builder")
    void shouldSetCreationTimeViaBuilder() {
      final Instant fixedTime = Instant.parse("2025-01-01T00:00:00Z");
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .creationTime(fixedTime)
          .build();

      assertEquals(fixedTime, stats.getCreationTime(), "Creation time should match set value");
    }

    @Test
    @DisplayName("should set preparationTime via builder")
    void shouldSetPreparationTimeViaBuilder() {
      final Duration prepTime = Duration.ofMillis(500);
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .preparationTime(prepTime)
          .build();

      assertEquals(prepTime, stats.getPreparationTime(),
          "Preparation time should match set value");
    }

    @Test
    @DisplayName("should set memoryFootprint via builder")
    void shouldSetMemoryFootprintViaBuilder() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .memoryFootprint(65536L)
          .build();

      assertEquals(65536L, stats.getMemoryFootprint(), "Memory footprint should be 65536");
    }

    @Test
    @DisplayName("should set functionsPrecompiled via builder")
    void shouldSetFunctionsPrecompiledViaBuilder() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .functionsPrecompiled(42L)
          .build();

      assertEquals(42L, stats.getFunctionsPrecompiled(),
          "Functions precompiled should be 42");
    }

    @Test
    @DisplayName("should set totalFunctions via builder")
    void shouldSetTotalFunctionsViaBuilder() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .totalFunctions(100L)
          .build();

      assertEquals(100L, stats.getTotalFunctions(), "Total functions should be 100");
    }

    @Test
    @DisplayName("should set instancesCreated via builder")
    void shouldSetInstancesCreatedViaBuilder() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .instancesCreated(25L)
          .build();

      assertEquals(25L, stats.getInstancesCreated(), "Instances created should be 25");
    }

    @Test
    @DisplayName("should set averageInstantiationTime via builder")
    void shouldSetAverageInstantiationTimeViaBuilder() {
      final Duration avgTime = Duration.ofMillis(10);
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .averageInstantiationTime(avgTime)
          .build();

      assertEquals(avgTime, stats.getAverageInstantiationTime(),
          "Average instantiation time should match set value");
    }

    @Test
    @DisplayName("should set memoryPoolSize via builder")
    void shouldSetMemoryPoolSizeViaBuilder() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .memoryPoolSize(1048576L)
          .build();

      assertEquals(1048576L, stats.getMemoryPoolSize(), "Memory pool size should be 1048576");
    }

    @Test
    @DisplayName("should set poolingEnabled via builder")
    void shouldSetPoolingEnabledViaBuilder() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .poolingEnabled(true)
          .build();

      assertTrue(stats.isPoolingEnabled(), "Pooling should be enabled");
    }

    @Test
    @DisplayName("should set all fields via builder")
    void shouldSetAllFieldsViaBuilder() {
      final Instant fixedTime = Instant.parse("2025-06-15T12:00:00Z");
      final Duration prepTime = Duration.ofMillis(250);
      final Duration avgTime = Duration.ofMillis(5);

      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .creationTime(fixedTime)
          .preparationTime(prepTime)
          .memoryFootprint(32768L)
          .functionsPrecompiled(80L)
          .totalFunctions(100L)
          .instancesCreated(50L)
          .averageInstantiationTime(avgTime)
          .memoryPoolSize(524288L)
          .poolingEnabled(true)
          .build();

      assertEquals(fixedTime, stats.getCreationTime(), "Creation time should match");
      assertEquals(prepTime, stats.getPreparationTime(), "Preparation time should match");
      assertEquals(32768L, stats.getMemoryFootprint(), "Memory footprint should match");
      assertEquals(80L, stats.getFunctionsPrecompiled(), "Functions precompiled should match");
      assertEquals(100L, stats.getTotalFunctions(), "Total functions should match");
      assertEquals(50L, stats.getInstancesCreated(), "Instances created should match");
      assertEquals(avgTime, stats.getAverageInstantiationTime(),
          "Average instantiation time should match");
      assertEquals(524288L, stats.getMemoryPoolSize(), "Memory pool size should match");
      assertTrue(stats.isPoolingEnabled(), "Pooling enabled should match");
    }
  }

  @Nested
  @DisplayName("Builder Validation Tests")
  class BuilderValidationTests {

    @Test
    @DisplayName("should throw NullPointerException for null creationTime")
    void shouldThrowNpeForNullCreationTime() {
      assertThrows(
          NullPointerException.class,
          () -> PreInstantiationStatistics.builder().creationTime(null),
          "Setting null creation time should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null preparationTime")
    void shouldThrowNpeForNullPreparationTime() {
      assertThrows(
          NullPointerException.class,
          () -> PreInstantiationStatistics.builder().preparationTime(null),
          "Setting null preparation time should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null averageInstantiationTime")
    void shouldThrowNpeForNullAverageInstantiationTime() {
      assertThrows(
          NullPointerException.class,
          () -> PreInstantiationStatistics.builder().averageInstantiationTime(null),
          "Setting null average instantiation time should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Computed Method Tests")
  class ComputedMethodTests {

    @Test
    @DisplayName("getFunctionPrecompilationRatio should compute correct ratio")
    void getFunctionPrecompilationRatioShouldComputeCorrectRatio() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .functionsPrecompiled(75L)
          .totalFunctions(100L)
          .build();

      assertEquals(0.75, stats.getFunctionPrecompilationRatio(), 0.001,
          "Precompilation ratio should be 0.75");
    }

    @Test
    @DisplayName("getFunctionPrecompilationRatio should return zero when no total functions")
    void getFunctionPrecompilationRatioShouldReturnZeroWhenNoTotalFunctions() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .functionsPrecompiled(10L)
          .totalFunctions(0L)
          .build();

      assertEquals(0.0, stats.getFunctionPrecompilationRatio(), 0.001,
          "Precompilation ratio should be 0.0 when totalFunctions is 0");
    }

    @Test
    @DisplayName("getFunctionPrecompilationRatio should return 1.0 when all precompiled")
    void getFunctionPrecompilationRatioShouldReturnOneWhenAllPrecompiled() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .functionsPrecompiled(50L)
          .totalFunctions(50L)
          .build();

      assertEquals(1.0, stats.getFunctionPrecompilationRatio(), 0.001,
          "Precompilation ratio should be 1.0 when all functions are precompiled");
    }

    @Test
    @DisplayName("getPreparationEfficiency should compute instances per second")
    void getPreparationEfficiencyShouldComputeInstancesPerSecond() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .preparationTime(Duration.ofSeconds(2))
          .instancesCreated(100L)
          .build();

      assertEquals(50.0, stats.getPreparationEfficiency(), 0.001,
          "Preparation efficiency should be 50 instances/second");
    }

    @Test
    @DisplayName("getPreparationEfficiency should return zero when preparation time is zero")
    void getPreparationEfficiencyShouldReturnZeroWhenPrepTimeZero() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .preparationTime(Duration.ZERO)
          .instancesCreated(100L)
          .build();

      assertEquals(0.0, stats.getPreparationEfficiency(), 0.001,
          "Preparation efficiency should be 0.0 when preparation time is zero");
    }

    @Test
    @DisplayName("getPreparationEfficiency should return zero when no instances created")
    void getPreparationEfficiencyShouldReturnZeroWhenNoInstances() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .preparationTime(Duration.ofSeconds(1))
          .instancesCreated(0L)
          .build();

      assertEquals(0.0, stats.getPreparationEfficiency(), 0.001,
          "Preparation efficiency should be 0.0 when no instances created");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for identical values")
    void equalsShouldReturnTrueForIdenticalValues() {
      final Instant fixedTime = Instant.parse("2025-01-01T00:00:00Z");
      final Duration prepTime = Duration.ofMillis(100);

      final PreInstantiationStatistics stats1 = PreInstantiationStatistics.builder()
          .creationTime(fixedTime)
          .preparationTime(prepTime)
          .memoryFootprint(1024L)
          .functionsPrecompiled(10L)
          .totalFunctions(20L)
          .instancesCreated(5L)
          .averageInstantiationTime(Duration.ofMillis(2))
          .memoryPoolSize(4096L)
          .poolingEnabled(true)
          .build();

      final PreInstantiationStatistics stats2 = PreInstantiationStatistics.builder()
          .creationTime(fixedTime)
          .preparationTime(prepTime)
          .memoryFootprint(1024L)
          .functionsPrecompiled(10L)
          .totalFunctions(20L)
          .instancesCreated(5L)
          .averageInstantiationTime(Duration.ofMillis(2))
          .memoryPoolSize(4096L)
          .poolingEnabled(true)
          .build();

      assertEquals(stats1, stats2, "Statistics with identical values should be equal");
    }

    @Test
    @DisplayName("equals should return false for different memoryFootprint")
    void equalsShouldReturnFalseForDifferentMemoryFootprint() {
      final Instant fixedTime = Instant.parse("2025-01-01T00:00:00Z");

      final PreInstantiationStatistics stats1 = PreInstantiationStatistics.builder()
          .creationTime(fixedTime)
          .memoryFootprint(1024L)
          .build();

      final PreInstantiationStatistics stats2 = PreInstantiationStatistics.builder()
          .creationTime(fixedTime)
          .memoryFootprint(2048L)
          .build();

      assertNotEquals(stats1, stats2,
          "Statistics with different memoryFootprint should not be equal");
    }

    @Test
    @DisplayName("equals should return true for same reference")
    void equalsShouldReturnTrueForSameReference() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder().build();

      assertEquals(stats, stats, "Same reference should be equal to itself");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder().build();

      assertNotEquals(null, stats, "Statistics should not be equal to null");
    }

    @Test
    @DisplayName("hashCode should be equal for equal objects")
    void hashCodeShouldBeEqualForEqualObjects() {
      final Instant fixedTime = Instant.parse("2025-01-01T00:00:00Z");

      final PreInstantiationStatistics stats1 = PreInstantiationStatistics.builder()
          .creationTime(fixedTime)
          .memoryFootprint(1024L)
          .build();

      final PreInstantiationStatistics stats2 = PreInstantiationStatistics.builder()
          .creationTime(fixedTime)
          .memoryFootprint(1024L)
          .build();

      assertEquals(stats1.hashCode(), stats2.hashCode(),
          "Equal objects should have equal hash codes");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain PreInstantiationStatistics prefix")
    void toStringShouldContainPrefix() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder().build();

      assertTrue(stats.toString().startsWith("PreInstantiationStatistics{"),
          "toString should start with 'PreInstantiationStatistics{'");
    }

    @Test
    @DisplayName("toString should contain key field names")
    void toStringShouldContainKeyFieldNames() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder()
          .memoryFootprint(4096L)
          .poolingEnabled(true)
          .build();

      final String str = stats.toString();

      assertTrue(str.contains("memoryFootprint"), "toString should contain 'memoryFootprint'");
      assertTrue(str.contains("poolingEnabled"), "toString should contain 'poolingEnabled'");
      assertTrue(str.contains("4096"), "toString should contain memory footprint value");
      assertTrue(str.contains("true"), "toString should contain poolingEnabled value");
    }
  }
}
