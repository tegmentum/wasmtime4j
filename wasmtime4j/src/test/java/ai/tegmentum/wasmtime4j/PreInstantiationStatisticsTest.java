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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics;
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
  @DisplayName("Builder Default Tests")
  class BuilderDefaultTests {

    @Test
    @DisplayName("builder should create instance with defaults")
    void builderShouldCreateInstanceWithDefaults() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder().build();

      assertNotNull(stats, "Statistics should not be null");
      assertNotNull(stats.getCreationTime(), "Creation time should have a default");
      assertEquals(
          Duration.ZERO,
          stats.getPreparationTime(),
          "Default preparation time should be Duration.ZERO");
      assertEquals(0L, stats.getInstancesCreated(), "Default instances created should be 0");
      assertEquals(
          Duration.ZERO,
          stats.getAverageInstantiationTime(),
          "Default average instantiation time should be Duration.ZERO");
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
      final PreInstantiationStatistics stats =
          PreInstantiationStatistics.builder().creationTime(fixedTime).build();

      assertEquals(fixedTime, stats.getCreationTime(), "Creation time should match set value");
    }

    @Test
    @DisplayName("should set preparationTime via builder")
    void shouldSetPreparationTimeViaBuilder() {
      final Duration prepTime = Duration.ofMillis(500);
      final PreInstantiationStatistics stats =
          PreInstantiationStatistics.builder().preparationTime(prepTime).build();

      assertEquals(prepTime, stats.getPreparationTime(), "Preparation time should match set value");
    }

    @Test
    @DisplayName("should set instancesCreated via builder")
    void shouldSetInstancesCreatedViaBuilder() {
      final PreInstantiationStatistics stats =
          PreInstantiationStatistics.builder().instancesCreated(25L).build();

      assertEquals(25L, stats.getInstancesCreated(), "Instances created should be 25");
    }

    @Test
    @DisplayName("should set averageInstantiationTime via builder")
    void shouldSetAverageInstantiationTimeViaBuilder() {
      final Duration avgTime = Duration.ofMillis(10);
      final PreInstantiationStatistics stats =
          PreInstantiationStatistics.builder().averageInstantiationTime(avgTime).build();

      assertEquals(
          avgTime,
          stats.getAverageInstantiationTime(),
          "Average instantiation time should match set value");
    }

    @Test
    @DisplayName("should set all fields via builder")
    void shouldSetAllFieldsViaBuilder() {
      final Instant fixedTime = Instant.parse("2025-06-15T12:00:00Z");
      final Duration prepTime = Duration.ofMillis(250);
      final Duration avgTime = Duration.ofMillis(5);

      final PreInstantiationStatistics stats =
          PreInstantiationStatistics.builder()
              .creationTime(fixedTime)
              .preparationTime(prepTime)
              .instancesCreated(50L)
              .averageInstantiationTime(avgTime)
              .build();

      assertEquals(fixedTime, stats.getCreationTime(), "Creation time should match");
      assertEquals(prepTime, stats.getPreparationTime(), "Preparation time should match");
      assertEquals(50L, stats.getInstancesCreated(), "Instances created should match");
      assertEquals(
          avgTime, stats.getAverageInstantiationTime(), "Average instantiation time should match");
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
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for identical values")
    void equalsShouldReturnTrueForIdenticalValues() {
      final Instant fixedTime = Instant.parse("2025-01-01T00:00:00Z");
      final Duration prepTime = Duration.ofMillis(100);
      final Duration avgTime = Duration.ofMillis(2);

      final PreInstantiationStatistics stats1 =
          PreInstantiationStatistics.builder()
              .creationTime(fixedTime)
              .preparationTime(prepTime)
              .instancesCreated(5L)
              .averageInstantiationTime(avgTime)
              .build();

      final PreInstantiationStatistics stats2 =
          PreInstantiationStatistics.builder()
              .creationTime(fixedTime)
              .preparationTime(prepTime)
              .instancesCreated(5L)
              .averageInstantiationTime(avgTime)
              .build();

      assertEquals(stats1, stats2, "Statistics with identical values should be equal");
    }

    @Test
    @DisplayName("equals should return false for different instancesCreated")
    void equalsShouldReturnFalseForDifferentInstancesCreated() {
      final Instant fixedTime = Instant.parse("2025-01-01T00:00:00Z");

      final PreInstantiationStatistics stats1 =
          PreInstantiationStatistics.builder().creationTime(fixedTime).instancesCreated(5L).build();

      final PreInstantiationStatistics stats2 =
          PreInstantiationStatistics.builder()
              .creationTime(fixedTime)
              .instancesCreated(10L)
              .build();

      assertNotEquals(
          stats1, stats2, "Statistics with different instancesCreated should not be equal");
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

      final PreInstantiationStatistics stats1 =
          PreInstantiationStatistics.builder().creationTime(fixedTime).instancesCreated(5L).build();

      final PreInstantiationStatistics stats2 =
          PreInstantiationStatistics.builder().creationTime(fixedTime).instancesCreated(5L).build();

      assertEquals(
          stats1.hashCode(), stats2.hashCode(), "Equal objects should have equal hash codes");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain PreInstantiationStatistics prefix")
    void toStringShouldContainPrefix() {
      final PreInstantiationStatistics stats = PreInstantiationStatistics.builder().build();

      assertTrue(
          stats.toString().startsWith("PreInstantiationStatistics{"),
          "toString should start with 'PreInstantiationStatistics{'");
    }

    @Test
    @DisplayName("toString should contain key field names")
    void toStringShouldContainKeyFieldNames() {
      final PreInstantiationStatistics stats =
          PreInstantiationStatistics.builder().instancesCreated(42L).build();

      final String str = stats.toString();

      assertTrue(str.contains("instancesCreated"), "toString should contain 'instancesCreated'");
      assertTrue(str.contains("42"), "toString should contain instancesCreated value");
    }
  }
}
