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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for ResourceLimiterConfig class.
 *
 * <p>Verifies the builder pattern, validation, and getter methods for resource limiter
 * configuration.
 */
@DisplayName("ResourceLimiterConfig Tests")
class ResourceLimiterConfigTest {

  @Nested
  @DisplayName("Builder Pattern Tests")
  class BuilderPatternTests {

    @Test
    @DisplayName("should create builder via static method")
    void shouldCreateBuilderViaStaticMethod() {
      ResourceLimiterConfig.Builder builder = ResourceLimiterConfig.builder();
      assertNotNull(builder, "Builder should not be null");
    }

    @Test
    @DisplayName("should build config with no limits set")
    void shouldBuildConfigWithNoLimitsSet() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().build();
      assertNotNull(config, "Config should not be null");
    }

    @Test
    @DisplayName("should support fluent builder chaining")
    void shouldSupportFluentBuilderChaining() {
      ResourceLimiterConfig config =
          ResourceLimiterConfig.builder()
              .maxMemoryBytes(1024)
              .maxMemoryPages(10)
              .maxTableElements(100)
              .maxInstances(5)
              .maxTables(3)
              .maxMemories(2)
              .build();

      assertNotNull(config, "Config should not be null after chaining all methods");
    }
  }

  @Nested
  @DisplayName("Defaults Factory Tests")
  class DefaultsFactoryTests {

    @Test
    @DisplayName("should create default config with no limits")
    void shouldCreateDefaultConfigWithNoLimits() {
      ResourceLimiterConfig config = ResourceLimiterConfig.defaults();

      assertNotNull(config, "Default config should not be null");
      assertFalse(config.getMaxMemoryBytes().isPresent(), "Default maxMemoryBytes should be empty");
      assertFalse(config.getMaxMemoryPages().isPresent(), "Default maxMemoryPages should be empty");
      assertFalse(
          config.getMaxTableElements().isPresent(), "Default maxTableElements should be empty");
      assertFalse(config.getMaxInstances().isPresent(), "Default maxInstances should be empty");
      assertFalse(config.getMaxTables().isPresent(), "Default maxTables should be empty");
      assertFalse(config.getMaxMemories().isPresent(), "Default maxMemories should be empty");
    }
  }

  @Nested
  @DisplayName("MaxMemoryBytes Tests")
  class MaxMemoryBytesTests {

    @Test
    @DisplayName("should set and get maxMemoryBytes")
    void shouldSetAndGetMaxMemoryBytes() {
      long expectedBytes = 1024L * 1024L * 10L; // 10 MB
      ResourceLimiterConfig config =
          ResourceLimiterConfig.builder().maxMemoryBytes(expectedBytes).build();

      OptionalLong result = config.getMaxMemoryBytes();
      assertTrue(result.isPresent(), "maxMemoryBytes should be present");
      assertEquals(expectedBytes, result.getAsLong(), "maxMemoryBytes should match expected value");
    }

    @Test
    @DisplayName("should accept zero for maxMemoryBytes")
    void shouldAcceptZeroForMaxMemoryBytes() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().maxMemoryBytes(0).build();

      OptionalLong result = config.getMaxMemoryBytes();
      assertTrue(result.isPresent(), "maxMemoryBytes should be present");
      assertEquals(0L, result.getAsLong(), "maxMemoryBytes should be zero");
    }

    @Test
    @DisplayName("should reject negative maxMemoryBytes")
    void shouldRejectNegativeMaxMemoryBytes() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ResourceLimiterConfig.builder().maxMemoryBytes(-1),
              "Should throw IllegalArgumentException for negative bytes");

      assertTrue(
          exception.getMessage().contains("negative"),
          "Exception message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should accept Long.MAX_VALUE for maxMemoryBytes")
    void shouldAcceptLongMaxValueForMaxMemoryBytes() {
      ResourceLimiterConfig config =
          ResourceLimiterConfig.builder().maxMemoryBytes(Long.MAX_VALUE).build();

      OptionalLong result = config.getMaxMemoryBytes();
      assertTrue(result.isPresent(), "maxMemoryBytes should be present");
      assertEquals(Long.MAX_VALUE, result.getAsLong(), "maxMemoryBytes should be Long.MAX_VALUE");
    }

    @Test
    @DisplayName("should return empty Optional when maxMemoryBytes not set")
    void shouldReturnEmptyOptionalWhenMaxMemoryBytesNotSet() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().build();

      OptionalLong result = config.getMaxMemoryBytes();
      assertFalse(result.isPresent(), "maxMemoryBytes should not be present when not set");
    }
  }

  @Nested
  @DisplayName("MaxMemoryPages Tests")
  class MaxMemoryPagesTests {

    @Test
    @DisplayName("should set and get maxMemoryPages")
    void shouldSetAndGetMaxMemoryPages() {
      long expectedPages = 160L; // 10 MB in 64KB pages
      ResourceLimiterConfig config =
          ResourceLimiterConfig.builder().maxMemoryPages(expectedPages).build();

      OptionalLong result = config.getMaxMemoryPages();
      assertTrue(result.isPresent(), "maxMemoryPages should be present");
      assertEquals(expectedPages, result.getAsLong(), "maxMemoryPages should match expected value");
    }

    @Test
    @DisplayName("should accept zero for maxMemoryPages")
    void shouldAcceptZeroForMaxMemoryPages() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().maxMemoryPages(0).build();

      OptionalLong result = config.getMaxMemoryPages();
      assertTrue(result.isPresent(), "maxMemoryPages should be present");
      assertEquals(0L, result.getAsLong(), "maxMemoryPages should be zero");
    }

    @Test
    @DisplayName("should reject negative maxMemoryPages")
    void shouldRejectNegativeMaxMemoryPages() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ResourceLimiterConfig.builder().maxMemoryPages(-1),
              "Should throw IllegalArgumentException for negative pages");

      assertTrue(
          exception.getMessage().contains("negative"),
          "Exception message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should return empty Optional when maxMemoryPages not set")
    void shouldReturnEmptyOptionalWhenMaxMemoryPagesNotSet() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().build();

      OptionalLong result = config.getMaxMemoryPages();
      assertFalse(result.isPresent(), "maxMemoryPages should not be present when not set");
    }
  }

  @Nested
  @DisplayName("MaxTableElements Tests")
  class MaxTableElementsTests {

    @Test
    @DisplayName("should set and get maxTableElements")
    void shouldSetAndGetMaxTableElements() {
      long expectedElements = 1000L;
      ResourceLimiterConfig config =
          ResourceLimiterConfig.builder().maxTableElements(expectedElements).build();

      OptionalLong result = config.getMaxTableElements();
      assertTrue(result.isPresent(), "maxTableElements should be present");
      assertEquals(
          expectedElements, result.getAsLong(), "maxTableElements should match expected value");
    }

    @Test
    @DisplayName("should accept zero for maxTableElements")
    void shouldAcceptZeroForMaxTableElements() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().maxTableElements(0).build();

      OptionalLong result = config.getMaxTableElements();
      assertTrue(result.isPresent(), "maxTableElements should be present");
      assertEquals(0L, result.getAsLong(), "maxTableElements should be zero");
    }

    @Test
    @DisplayName("should reject negative maxTableElements")
    void shouldRejectNegativeMaxTableElements() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ResourceLimiterConfig.builder().maxTableElements(-1),
              "Should throw IllegalArgumentException for negative elements");

      assertTrue(
          exception.getMessage().contains("negative"),
          "Exception message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should return empty Optional when maxTableElements not set")
    void shouldReturnEmptyOptionalWhenMaxTableElementsNotSet() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().build();

      OptionalLong result = config.getMaxTableElements();
      assertFalse(result.isPresent(), "maxTableElements should not be present when not set");
    }
  }

  @Nested
  @DisplayName("MaxInstances Tests")
  class MaxInstancesTests {

    @Test
    @DisplayName("should set and get maxInstances")
    void shouldSetAndGetMaxInstances() {
      int expectedInstances = 5;
      ResourceLimiterConfig config =
          ResourceLimiterConfig.builder().maxInstances(expectedInstances).build();

      OptionalInt result = config.getMaxInstances();
      assertTrue(result.isPresent(), "maxInstances should be present");
      assertEquals(
          expectedInstances, result.getAsInt(), "maxInstances should match expected value");
    }

    @Test
    @DisplayName("should accept zero for maxInstances")
    void shouldAcceptZeroForMaxInstances() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().maxInstances(0).build();

      OptionalInt result = config.getMaxInstances();
      assertTrue(result.isPresent(), "maxInstances should be present");
      assertEquals(0, result.getAsInt(), "maxInstances should be zero");
    }

    @Test
    @DisplayName("should reject negative maxInstances")
    void shouldRejectNegativeMaxInstances() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ResourceLimiterConfig.builder().maxInstances(-1),
              "Should throw IllegalArgumentException for negative instances");

      assertTrue(
          exception.getMessage().contains("negative"),
          "Exception message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should return empty Optional when maxInstances not set")
    void shouldReturnEmptyOptionalWhenMaxInstancesNotSet() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().build();

      OptionalInt result = config.getMaxInstances();
      assertFalse(result.isPresent(), "maxInstances should not be present when not set");
    }
  }

  @Nested
  @DisplayName("MaxTables Tests")
  class MaxTablesTests {

    @Test
    @DisplayName("should set and get maxTables")
    void shouldSetAndGetMaxTables() {
      int expectedTables = 3;
      ResourceLimiterConfig config =
          ResourceLimiterConfig.builder().maxTables(expectedTables).build();

      OptionalInt result = config.getMaxTables();
      assertTrue(result.isPresent(), "maxTables should be present");
      assertEquals(expectedTables, result.getAsInt(), "maxTables should match expected value");
    }

    @Test
    @DisplayName("should accept zero for maxTables")
    void shouldAcceptZeroForMaxTables() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().maxTables(0).build();

      OptionalInt result = config.getMaxTables();
      assertTrue(result.isPresent(), "maxTables should be present");
      assertEquals(0, result.getAsInt(), "maxTables should be zero");
    }

    @Test
    @DisplayName("should reject negative maxTables")
    void shouldRejectNegativeMaxTables() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ResourceLimiterConfig.builder().maxTables(-1),
              "Should throw IllegalArgumentException for negative tables");

      assertTrue(
          exception.getMessage().contains("negative"),
          "Exception message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should return empty Optional when maxTables not set")
    void shouldReturnEmptyOptionalWhenMaxTablesNotSet() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().build();

      OptionalInt result = config.getMaxTables();
      assertFalse(result.isPresent(), "maxTables should not be present when not set");
    }
  }

  @Nested
  @DisplayName("MaxMemories Tests")
  class MaxMemoriesTests {

    @Test
    @DisplayName("should set and get maxMemories")
    void shouldSetAndGetMaxMemories() {
      int expectedMemories = 2;
      ResourceLimiterConfig config =
          ResourceLimiterConfig.builder().maxMemories(expectedMemories).build();

      OptionalInt result = config.getMaxMemories();
      assertTrue(result.isPresent(), "maxMemories should be present");
      assertEquals(expectedMemories, result.getAsInt(), "maxMemories should match expected value");
    }

    @Test
    @DisplayName("should accept zero for maxMemories")
    void shouldAcceptZeroForMaxMemories() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().maxMemories(0).build();

      OptionalInt result = config.getMaxMemories();
      assertTrue(result.isPresent(), "maxMemories should be present");
      assertEquals(0, result.getAsInt(), "maxMemories should be zero");
    }

    @Test
    @DisplayName("should reject negative maxMemories")
    void shouldRejectNegativeMaxMemories() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> ResourceLimiterConfig.builder().maxMemories(-1),
              "Should throw IllegalArgumentException for negative memories");

      assertTrue(
          exception.getMessage().contains("negative"),
          "Exception message should mention 'negative': " + exception.getMessage());
    }

    @Test
    @DisplayName("should return empty Optional when maxMemories not set")
    void shouldReturnEmptyOptionalWhenMaxMemoriesNotSet() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().build();

      OptionalInt result = config.getMaxMemories();
      assertFalse(result.isPresent(), "maxMemories should not be present when not set");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce non-null toString output")
    void shouldProduceNonNullToStringOutput() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().build();
      String result = config.toString();
      assertNotNull(result, "toString should not return null");
    }

    @Test
    @DisplayName("should include class name in toString")
    void shouldIncludeClassNameInToString() {
      ResourceLimiterConfig config = ResourceLimiterConfig.builder().build();
      String result = config.toString();
      assertTrue(
          result.contains("ResourceLimiterConfig"),
          "toString should contain class name: " + result);
    }

    @Test
    @DisplayName("should include field names in toString")
    void shouldIncludeFieldNamesInToString() {
      ResourceLimiterConfig config =
          ResourceLimiterConfig.builder()
              .maxMemoryBytes(1024)
              .maxMemoryPages(10)
              .maxTableElements(100)
              .maxInstances(5)
              .maxTables(3)
              .maxMemories(2)
              .build();

      String result = config.toString();
      assertTrue(result.contains("maxMemoryBytes"), "toString should contain maxMemoryBytes");
      assertTrue(result.contains("maxMemoryPages"), "toString should contain maxMemoryPages");
      assertTrue(result.contains("maxTableElements"), "toString should contain maxTableElements");
      assertTrue(result.contains("maxInstances"), "toString should contain maxInstances");
      assertTrue(result.contains("maxTables"), "toString should contain maxTables");
      assertTrue(result.contains("maxMemories"), "toString should contain maxMemories");
    }

    @Test
    @DisplayName("should include set values in toString")
    void shouldIncludeSetValuesInToString() {
      ResourceLimiterConfig config =
          ResourceLimiterConfig.builder().maxMemoryBytes(1024).maxInstances(5).build();

      String result = config.toString();
      assertTrue(result.contains("1024"), "toString should contain the set memory bytes value");
      assertTrue(result.contains("5"), "toString should contain the set instances value");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should allow partial configuration")
    void shouldAllowPartialConfiguration() {
      ResourceLimiterConfig config =
          ResourceLimiterConfig.builder().maxMemoryBytes(1024L * 1024L * 10L).build();

      assertTrue(config.getMaxMemoryBytes().isPresent(), "maxMemoryBytes should be set");
      assertFalse(config.getMaxMemoryPages().isPresent(), "maxMemoryPages should not be set");
      assertFalse(config.getMaxTableElements().isPresent(), "maxTableElements should not be set");
      assertFalse(config.getMaxInstances().isPresent(), "maxInstances should not be set");
      assertFalse(config.getMaxTables().isPresent(), "maxTables should not be set");
      assertFalse(config.getMaxMemories().isPresent(), "maxMemories should not be set");
    }

    @Test
    @DisplayName("should allow multiple builds from same builder")
    void shouldAllowMultipleBuildsFromSameBuilder() {
      ResourceLimiterConfig.Builder builder =
          ResourceLimiterConfig.builder().maxMemoryBytes(1024).maxInstances(5);

      ResourceLimiterConfig config1 = builder.build();
      ResourceLimiterConfig config2 = builder.build();

      assertNotNull(config1, "First config should not be null");
      assertNotNull(config2, "Second config should not be null");

      assertEquals(
          config1.getMaxMemoryBytes(),
          config2.getMaxMemoryBytes(),
          "Both configs should have same maxMemoryBytes");
      assertEquals(
          config1.getMaxInstances(),
          config2.getMaxInstances(),
          "Both configs should have same maxInstances");
    }

    @Test
    @DisplayName("should create typical production config")
    void shouldCreateTypicalProductionConfig() {
      // 100 MB total memory, 1600 pages (100 MB in 64KB pages), 10000 table elements
      ResourceLimiterConfig config =
          ResourceLimiterConfig.builder()
              .maxMemoryBytes(100L * 1024L * 1024L)
              .maxMemoryPages(1600)
              .maxTableElements(10000)
              .maxInstances(10)
              .maxTables(5)
              .maxMemories(3)
              .build();

      assertTrue(config.getMaxMemoryBytes().isPresent(), "maxMemoryBytes should be set");
      assertEquals(
          100L * 1024L * 1024L,
          config.getMaxMemoryBytes().getAsLong(),
          "maxMemoryBytes should be 100 MB");

      assertTrue(config.getMaxMemoryPages().isPresent(), "maxMemoryPages should be set");
      assertEquals(1600L, config.getMaxMemoryPages().getAsLong(), "maxMemoryPages should be 1600");

      assertTrue(config.getMaxTableElements().isPresent(), "maxTableElements should be set");
      assertEquals(
          10000L, config.getMaxTableElements().getAsLong(), "maxTableElements should be 10000");

      assertTrue(config.getMaxInstances().isPresent(), "maxInstances should be set");
      assertEquals(10, config.getMaxInstances().getAsInt(), "maxInstances should be 10");

      assertTrue(config.getMaxTables().isPresent(), "maxTables should be set");
      assertEquals(5, config.getMaxTables().getAsInt(), "maxTables should be 5");

      assertTrue(config.getMaxMemories().isPresent(), "maxMemories should be set");
      assertEquals(3, config.getMaxMemories().getAsInt(), "maxMemories should be 3");
    }
  }
}
