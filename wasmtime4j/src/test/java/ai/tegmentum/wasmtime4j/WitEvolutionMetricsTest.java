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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wit.WitEvolutionMetrics;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitEvolutionMetrics} class.
 *
 * <p>WitEvolutionMetrics provides comprehensive metrics about WIT interface evolution operations.
 */
@DisplayName("WitEvolutionMetrics Tests")
class WitEvolutionMetricsTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create metrics with all fields")
    void shouldCreateMetricsWithAllFields() {
      final Instant start = Instant.now().minusSeconds(10);
      final Instant end = Instant.now();
      final WitEvolutionMetrics metrics =
          new WitEvolutionMetrics(
              Duration.ofSeconds(10),
              5,
              3,
              2,
              8,
              0.85,
              1024L,
              start,
              end,
              true,
              Map.of("key", "value"));

      assertEquals(Duration.ofSeconds(10), metrics.getEvolutionDuration());
      assertEquals(5, metrics.getTypesAnalyzed());
      assertEquals(3, metrics.getFunctionsAnalyzed());
      assertEquals(2, metrics.getAdaptersCreated());
      assertEquals(8, metrics.getValidationChecks());
      assertEquals(0.85, metrics.getCompatibilityScore(), 0.001);
      assertEquals(1024L, metrics.getMemoryUsed());
      assertEquals(start, metrics.getStartTime());
      assertEquals(end, metrics.getEndTime());
      assertTrue(metrics.isSuccessful());
      assertNotNull(metrics.getDetailedMetrics());
    }

    @Test
    @DisplayName("should clamp negative values to zero")
    void shouldClampNegativeValuesToZero() {
      final Instant now = Instant.now();
      final WitEvolutionMetrics metrics =
          new WitEvolutionMetrics(
              Duration.ofSeconds(1), -5, -3, -2, -8, -0.5, -100L, now, now, false, Map.of());

      assertEquals(0, metrics.getTypesAnalyzed());
      assertEquals(0, metrics.getFunctionsAnalyzed());
      assertEquals(0, metrics.getAdaptersCreated());
      assertEquals(0, metrics.getValidationChecks());
      assertEquals(0.0, metrics.getCompatibilityScore(), 0.001);
      assertEquals(0L, metrics.getMemoryUsed());
    }

    @Test
    @DisplayName("should clamp compatibility score to 1.0")
    void shouldClampCompatibilityScoreToOne() {
      final Instant now = Instant.now();
      final WitEvolutionMetrics metrics =
          new WitEvolutionMetrics(
              Duration.ofSeconds(1), 0, 0, 0, 0, 1.5, 0, now, now, false, Map.of());

      assertEquals(1.0, metrics.getCompatibilityScore(), 0.001);
    }

    @Test
    @DisplayName("should throw on null duration")
    void shouldThrowOnNullDuration() {
      final Instant now = Instant.now();
      assertThrows(
          NullPointerException.class,
          () -> new WitEvolutionMetrics(null, 0, 0, 0, 0, 0.0, 0, now, now, false, Map.of()));
    }

    @Test
    @DisplayName("should throw on null start time")
    void shouldThrowOnNullStartTime() {
      assertThrows(
          NullPointerException.class,
          () ->
              new WitEvolutionMetrics(
                  Duration.ZERO, 0, 0, 0, 0, 0.0, 0, null, Instant.now(), false, Map.of()));
    }

    @Test
    @DisplayName("should throw on null end time")
    void shouldThrowOnNullEndTime() {
      assertThrows(
          NullPointerException.class,
          () ->
              new WitEvolutionMetrics(
                  Duration.ZERO, 0, 0, 0, 0, 0.0, 0, Instant.now(), null, false, Map.of()));
    }

    @Test
    @DisplayName("should throw on null detailed metrics")
    void shouldThrowOnNullDetailedMetrics() {
      final Instant now = Instant.now();
      assertThrows(
          NullPointerException.class,
          () -> new WitEvolutionMetrics(Duration.ZERO, 0, 0, 0, 0, 0.0, 0, now, now, false, null));
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("empty should create empty metrics")
    void emptyShouldCreateEmptyMetrics() {
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      assertEquals(Duration.ZERO, metrics.getEvolutionDuration());
      assertEquals(0, metrics.getTypesAnalyzed());
      assertEquals(0, metrics.getFunctionsAnalyzed());
      assertEquals(0, metrics.getAdaptersCreated());
      assertEquals(0, metrics.getValidationChecks());
      assertEquals(0.0, metrics.getCompatibilityScore(), 0.001);
      assertEquals(0L, metrics.getMemoryUsed());
      assertFalse(metrics.isSuccessful());
      assertTrue(metrics.getDetailedMetrics().isEmpty());
    }

    @Test
    @DisplayName("success should create successful metrics")
    void successShouldCreateSuccessfulMetrics() {
      final WitEvolutionMetrics metrics =
          WitEvolutionMetrics.success(Duration.ofMillis(100), 10, 5, 3, 0.95);

      assertEquals(Duration.ofMillis(100), metrics.getEvolutionDuration());
      assertEquals(10, metrics.getTypesAnalyzed());
      assertEquals(5, metrics.getFunctionsAnalyzed());
      assertEquals(3, metrics.getAdaptersCreated());
      assertEquals(15, metrics.getValidationChecks()); // typesAnalyzed + functionsAnalyzed
      assertEquals(0.95, metrics.getCompatibilityScore(), 0.001);
      assertTrue(metrics.isSuccessful());
      assertTrue(metrics.getMemoryUsed() >= 0);
    }
  }

  @Nested
  @DisplayName("Throughput Calculation Tests")
  class ThroughputCalculationTests {

    @Test
    @DisplayName("getEvolutionThroughput should calculate items per second")
    void getEvolutionThroughputShouldCalculateItemsPerSecond() {
      final Instant now = Instant.now();
      final WitEvolutionMetrics metrics =
          new WitEvolutionMetrics(
              Duration.ofSeconds(2),
              10,
              10,
              0,
              0,
              0.0,
              0,
              now.minusSeconds(2),
              now,
              true,
              Map.of());

      assertEquals(10.0, metrics.getEvolutionThroughput(), 0.001);
    }

    @Test
    @DisplayName("getEvolutionThroughput should return zero for zero duration")
    void getEvolutionThroughputShouldReturnZeroForZeroDuration() {
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      assertEquals(0.0, metrics.getEvolutionThroughput(), 0.001);
    }

    @Test
    @DisplayName("getAdapterCreationRate should calculate adapters per second")
    void getAdapterCreationRateShouldCalculateAdaptersPerSecond() {
      final Instant now = Instant.now();
      final WitEvolutionMetrics metrics =
          new WitEvolutionMetrics(
              Duration.ofSeconds(2), 0, 0, 10, 0, 0.0, 0, now.minusSeconds(2), now, true, Map.of());

      assertEquals(5.0, metrics.getAdapterCreationRate(), 0.001);
    }

    @Test
    @DisplayName("getValidationEfficiency should calculate checks per second")
    void getValidationEfficiencyShouldCalculateChecksPerSecond() {
      final Instant now = Instant.now();
      final WitEvolutionMetrics metrics =
          new WitEvolutionMetrics(
              Duration.ofSeconds(2), 0, 0, 0, 20, 0.0, 0, now.minusSeconds(2), now, true, Map.of());

      assertEquals(10.0, metrics.getValidationEfficiency(), 0.001);
    }
  }

  @Nested
  @DisplayName("Memory Calculation Tests")
  class MemoryCalculationTests {

    @Test
    @DisplayName("getMemoryPerItem should calculate memory per item")
    void getMemoryPerItemShouldCalculateMemoryPerItem() {
      final Instant now = Instant.now();
      final WitEvolutionMetrics metrics =
          new WitEvolutionMetrics(
              Duration.ofSeconds(1),
              5,
              5,
              0,
              0,
              0.0,
              1000L,
              now.minusSeconds(1),
              now,
              true,
              Map.of());

      assertEquals(100L, metrics.getMemoryPerItem());
    }

    @Test
    @DisplayName("getMemoryPerItem should return zero for no items")
    void getMemoryPerItemShouldReturnZeroForNoItems() {
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      assertEquals(0L, metrics.getMemoryPerItem());
    }
  }

  @Nested
  @DisplayName("Detailed Metrics Tests")
  class DetailedMetricsTests {

    @Test
    @DisplayName("getDetailedMetric should return typed value")
    void getDetailedMetricShouldReturnTypedValue() {
      final Instant now = Instant.now();
      final WitEvolutionMetrics metrics =
          new WitEvolutionMetrics(
              Duration.ofSeconds(1),
              0,
              0,
              0,
              0,
              0.0,
              0,
              now,
              now,
              true,
              Map.of("count", 42, "name", "test"));

      final Optional<Integer> count = metrics.getDetailedMetric("count", Integer.class);
      assertTrue(count.isPresent());
      assertEquals(42, count.get());

      final Optional<String> name = metrics.getDetailedMetric("name", String.class);
      assertTrue(name.isPresent());
      assertEquals("test", name.get());
    }

    @Test
    @DisplayName("getDetailedMetric should return empty for missing key")
    void getDetailedMetricShouldReturnEmptyForMissingKey() {
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.empty();

      assertTrue(metrics.getDetailedMetric("nonexistent", String.class).isEmpty());
    }

    @Test
    @DisplayName("getDetailedMetric should return empty for wrong type")
    void getDetailedMetricShouldReturnEmptyForWrongType() {
      final Instant now = Instant.now();
      final WitEvolutionMetrics metrics =
          new WitEvolutionMetrics(
              Duration.ofSeconds(1), 0, 0, 0, 0, 0.0, 0, now, now, true, Map.of("count", 42));

      assertTrue(metrics.getDetailedMetric("count", String.class).isEmpty());
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create metrics with all fields")
    void builderShouldCreateMetricsWithAllFields() {
      final Instant start = Instant.now().minusSeconds(5);
      final Instant end = Instant.now();

      final WitEvolutionMetrics metrics =
          WitEvolutionMetrics.builder()
              .evolutionDuration(Duration.ofSeconds(5))
              .typesAnalyzed(10)
              .functionsAnalyzed(5)
              .adaptersCreated(3)
              .validationChecks(15)
              .compatibilityScore(0.9)
              .memoryUsed(2048L)
              .startTime(start)
              .endTime(end)
              .successful(true)
              .detailedMetric("key", "value")
              .build();

      assertEquals(Duration.ofSeconds(5), metrics.getEvolutionDuration());
      assertEquals(10, metrics.getTypesAnalyzed());
      assertEquals(5, metrics.getFunctionsAnalyzed());
      assertEquals(3, metrics.getAdaptersCreated());
      assertEquals(15, metrics.getValidationChecks());
      assertEquals(0.9, metrics.getCompatibilityScore(), 0.001);
      assertEquals(2048L, metrics.getMemoryUsed());
      assertTrue(metrics.isSuccessful());
      assertEquals("value", metrics.getDetailedMetrics().get("key"));
    }

    @Test
    @DisplayName("builder should use defaults")
    void builderShouldUseDefaults() {
      final WitEvolutionMetrics metrics = WitEvolutionMetrics.builder().build();

      assertEquals(Duration.ZERO, metrics.getEvolutionDuration());
      assertEquals(0, metrics.getTypesAnalyzed());
      assertFalse(metrics.isSuccessful());
    }

    @Test
    @DisplayName("builder should clamp values")
    void builderShouldClampValues() {
      final WitEvolutionMetrics metrics =
          WitEvolutionMetrics.builder()
              .typesAnalyzed(-5)
              .compatibilityScore(1.5)
              .memoryUsed(-100L)
              .build();

      assertEquals(0, metrics.getTypesAnalyzed());
      assertEquals(1.0, metrics.getCompatibilityScore(), 0.001);
      assertEquals(0L, metrics.getMemoryUsed());
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("equal metrics should be equal")
    void equalMetricsShouldBeEqual() {
      final Instant start = Instant.parse("2025-01-01T00:00:00Z");
      final Instant end = Instant.parse("2025-01-01T00:00:01Z");

      final WitEvolutionMetrics metrics1 =
          new WitEvolutionMetrics(
              Duration.ofSeconds(1), 5, 3, 2, 8, 0.9, 1024L, start, end, true, Map.of());
      final WitEvolutionMetrics metrics2 =
          new WitEvolutionMetrics(
              Duration.ofSeconds(1), 5, 3, 2, 8, 0.9, 1024L, start, end, true, Map.of());

      assertEquals(metrics1, metrics2);
      assertEquals(metrics1.hashCode(), metrics2.hashCode());
    }

    @Test
    @DisplayName("different metrics should not be equal")
    void differentMetricsShouldNotBeEqual() {
      final Instant now = Instant.now();
      final WitEvolutionMetrics metrics1 =
          new WitEvolutionMetrics(
              Duration.ofSeconds(1), 5, 3, 2, 8, 0.9, 1024L, now, now, true, Map.of());
      final WitEvolutionMetrics metrics2 =
          new WitEvolutionMetrics(
              Duration.ofSeconds(1), 10, 3, 2, 8, 0.9, 1024L, now, now, true, Map.of());

      assertNotEquals(metrics1, metrics2);
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain key fields")
    void toStringShouldContainKeyFields() {
      final WitEvolutionMetrics metrics =
          WitEvolutionMetrics.success(Duration.ofMillis(100), 10, 5, 3, 0.95);

      final String str = metrics.toString();
      assertTrue(str.contains("evolutionDuration"));
      assertTrue(str.contains("typesAnalyzed=10"));
      assertTrue(str.contains("functionsAnalyzed=5"));
      assertTrue(str.contains("adaptersCreated=3"));
      assertTrue(str.contains("successful=true"));
    }
  }
}
