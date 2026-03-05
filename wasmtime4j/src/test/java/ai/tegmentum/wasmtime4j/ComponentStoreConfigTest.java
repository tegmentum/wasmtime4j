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

import ai.tegmentum.wasmtime4j.component.ComponentStoreConfig;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests for {@link ComponentStoreConfig} builder class.
 *
 * <p>ComponentStoreConfig controls resource limits such as fuel metering, epoch deadlines, and
 * memory bounds for component instance stores created by pre-instantiation.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentStoreConfig Tests")
class ComponentStoreConfigTest {

  private static final Logger LOGGER = Logger.getLogger(ComponentStoreConfigTest.class.getName());

  @Nested
  @DisplayName("Builder Default Values Tests")
  class BuilderDefaultValueTests {

    @Test
    @DisplayName("should create builder successfully")
    void shouldCreateBuilderSuccessfully(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentStoreConfig.Builder builder = ComponentStoreConfig.builder();
      assertNotNull(builder, "Builder should not be null");

      LOGGER.info("Builder created successfully");
    }

    @Test
    @DisplayName("should create config with default values via builder")
    void shouldCreateConfigWithDefaultValues(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentStoreConfig config = ComponentStoreConfig.builder().build();

      assertNotNull(config, "Config should not be null");
      assertEquals(0L, config.getFuelLimit(), "Default fuel limit should be 0 (unlimited)");
      assertEquals(0L, config.getEpochDeadline(), "Default epoch deadline should be 0 (none)");
      assertEquals(
          0L, config.getMaxMemoryBytes(), "Default max memory bytes should be 0 (unlimited)");

      LOGGER.info(
          "Config defaults: fuelLimit="
              + config.getFuelLimit()
              + ", epochDeadline="
              + config.getEpochDeadline()
              + ", maxMemoryBytes="
              + config.getMaxMemoryBytes());
    }
  }

  @Nested
  @DisplayName("Fuel Limit Tests")
  class FuelLimitTests {

    @Test
    @DisplayName("should set fuel limit")
    void shouldSetFuelLimit(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentStoreConfig config =
          ComponentStoreConfig.builder().fuelLimit(1_000_000L).build();

      assertEquals(1_000_000L, config.getFuelLimit(), "Fuel limit should be 1,000,000");

      LOGGER.info("Fuel limit set to: " + config.getFuelLimit());
    }

    @Test
    @DisplayName("should accept zero fuel limit as unlimited")
    void shouldAcceptZeroFuelLimit(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentStoreConfig config = ComponentStoreConfig.builder().fuelLimit(0L).build();

      assertEquals(0L, config.getFuelLimit(), "Zero fuel limit should be valid (unlimited)");

      LOGGER.info("Zero fuel limit accepted for unlimited");
    }

    @Test
    @DisplayName("should accept max long fuel limit")
    void shouldAcceptMaxLongFuelLimit(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentStoreConfig config =
          ComponentStoreConfig.builder().fuelLimit(Long.MAX_VALUE).build();

      assertEquals(Long.MAX_VALUE, config.getFuelLimit(), "Max long fuel limit should be accepted");

      LOGGER.info("Max long fuel limit accepted: " + config.getFuelLimit());
    }

    @Test
    @DisplayName("should reject negative fuel limit")
    void shouldRejectNegativeFuelLimit(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentStoreConfig.builder().fuelLimit(-1L),
          "Negative fuel limit should be rejected");

      LOGGER.info("Negative fuel limit correctly rejected");
    }
  }

  @Nested
  @DisplayName("Epoch Deadline Tests")
  class EpochDeadlineTests {

    @Test
    @DisplayName("should set epoch deadline")
    void shouldSetEpochDeadline(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentStoreConfig config = ComponentStoreConfig.builder().epochDeadline(5L).build();

      assertEquals(5L, config.getEpochDeadline(), "Epoch deadline should be 5");

      LOGGER.info("Epoch deadline set to: " + config.getEpochDeadline());
    }

    @Test
    @DisplayName("should accept zero epoch deadline as no deadline")
    void shouldAcceptZeroEpochDeadline(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentStoreConfig config = ComponentStoreConfig.builder().epochDeadline(0L).build();

      assertEquals(0L, config.getEpochDeadline(), "Zero epoch deadline should be valid (none)");

      LOGGER.info("Zero epoch deadline accepted for no deadline");
    }

    @Test
    @DisplayName("should reject negative epoch deadline")
    void shouldRejectNegativeEpochDeadline(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentStoreConfig.builder().epochDeadline(-1L),
          "Negative epoch deadline should be rejected");

      LOGGER.info("Negative epoch deadline correctly rejected");
    }
  }

  @Nested
  @DisplayName("Max Memory Bytes Tests")
  class MaxMemoryBytesTests {

    @Test
    @DisplayName("should set max memory bytes")
    void shouldSetMaxMemoryBytes(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final long sixtyFourMb = 64L * 1024 * 1024;
      final ComponentStoreConfig config =
          ComponentStoreConfig.builder().maxMemoryBytes(sixtyFourMb).build();

      assertEquals(sixtyFourMb, config.getMaxMemoryBytes(), "Max memory should be 64MB");

      LOGGER.info("Max memory bytes set to: " + config.getMaxMemoryBytes() + " bytes");
    }

    @Test
    @DisplayName("should accept zero max memory as unlimited")
    void shouldAcceptZeroMaxMemory(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentStoreConfig config = ComponentStoreConfig.builder().maxMemoryBytes(0L).build();

      assertEquals(0L, config.getMaxMemoryBytes(), "Zero max memory should be valid (unlimited)");

      LOGGER.info("Zero max memory accepted for unlimited");
    }

    @Test
    @DisplayName("should reject negative max memory bytes")
    void shouldRejectNegativeMaxMemoryBytes(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentStoreConfig.builder().maxMemoryBytes(-1L),
          "Negative max memory bytes should be rejected");

      LOGGER.info("Negative max memory bytes correctly rejected");
    }
  }

  @Nested
  @DisplayName("Builder Chaining Tests")
  class BuilderChainingTests {

    @Test
    @DisplayName("should chain all builder methods")
    void shouldChainAllBuilderMethods(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentStoreConfig config =
          ComponentStoreConfig.builder()
              .fuelLimit(500_000L)
              .epochDeadline(10L)
              .maxMemoryBytes(128L * 1024 * 1024)
              .build();

      assertEquals(500_000L, config.getFuelLimit(), "Fuel limit should be 500,000");
      assertEquals(10L, config.getEpochDeadline(), "Epoch deadline should be 10");
      assertEquals(128L * 1024 * 1024, config.getMaxMemoryBytes(), "Max memory should be 128MB");

      LOGGER.info(
          "Chained config: fuelLimit="
              + config.getFuelLimit()
              + ", epochDeadline="
              + config.getEpochDeadline()
              + ", maxMemoryBytes="
              + config.getMaxMemoryBytes());
    }

    @Test
    @DisplayName("should allow setting only fuel limit")
    void shouldAllowSettingOnlyFuelLimit(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentStoreConfig config = ComponentStoreConfig.builder().fuelLimit(100L).build();

      assertEquals(100L, config.getFuelLimit(), "Fuel limit should be set");
      assertEquals(0L, config.getEpochDeadline(), "Epoch deadline should remain default");
      assertEquals(0L, config.getMaxMemoryBytes(), "Max memory should remain default");

      LOGGER.info("Config with only fuel limit set");
    }

    @Test
    @DisplayName("should allow setting only epoch deadline")
    void shouldAllowSettingOnlyEpochDeadline(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentStoreConfig config = ComponentStoreConfig.builder().epochDeadline(3L).build();

      assertEquals(0L, config.getFuelLimit(), "Fuel limit should remain default");
      assertEquals(3L, config.getEpochDeadline(), "Epoch deadline should be set");
      assertEquals(0L, config.getMaxMemoryBytes(), "Max memory should remain default");

      LOGGER.info("Config with only epoch deadline set");
    }

    @Test
    @DisplayName("should allow setting only max memory bytes")
    void shouldAllowSettingOnlyMaxMemoryBytes(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final long oneMb = 1024L * 1024;
      final ComponentStoreConfig config =
          ComponentStoreConfig.builder().maxMemoryBytes(oneMb).build();

      assertEquals(0L, config.getFuelLimit(), "Fuel limit should remain default");
      assertEquals(0L, config.getEpochDeadline(), "Epoch deadline should remain default");
      assertEquals(oneMb, config.getMaxMemoryBytes(), "Max memory should be set");

      LOGGER.info("Config with only max memory bytes set");
    }

    @Test
    @DisplayName("should allow overwriting builder values")
    void shouldAllowOverwritingBuilderValues(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentStoreConfig config =
          ComponentStoreConfig.builder()
              .fuelLimit(100L)
              .fuelLimit(200L)
              .epochDeadline(5L)
              .epochDeadline(10L)
              .maxMemoryBytes(1024L)
              .maxMemoryBytes(2048L)
              .build();

      assertEquals(200L, config.getFuelLimit(), "Fuel limit should be last set value");
      assertEquals(10L, config.getEpochDeadline(), "Epoch deadline should be last set value");
      assertEquals(2048L, config.getMaxMemoryBytes(), "Max memory should be last set value");

      LOGGER.info("Builder values overwritten correctly");
    }
  }

  @Nested
  @DisplayName("Multiple Build Tests")
  class MultipleBuildTests {

    @Test
    @DisplayName("should create independent configs from same builder")
    void shouldCreateIndependentConfigsFromSameBuilder(final TestInfo testInfo) {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final ComponentStoreConfig.Builder builder =
          ComponentStoreConfig.builder().fuelLimit(100L).epochDeadline(5L);

      final ComponentStoreConfig config1 = builder.build();
      builder.fuelLimit(200L);
      final ComponentStoreConfig config2 = builder.build();

      assertEquals(100L, config1.getFuelLimit(), "Config1 fuel limit should be 100");
      assertEquals(200L, config2.getFuelLimit(), "Config2 fuel limit should be 200");
      assertEquals(
          5L, config1.getEpochDeadline(), "Config1 epoch deadline should remain unchanged");
      assertEquals(5L, config2.getEpochDeadline(), "Config2 epoch deadline should be same");

      LOGGER.info(
          "Independent configs: config1.fuel="
              + config1.getFuelLimit()
              + ", config2.fuel="
              + config2.getFuelLimit());
    }
  }
}
