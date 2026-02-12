/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for config package.
 *
 * <p>This test class validates the configuration enums and classes.
 */
@DisplayName("Config Integration Tests")
public class ConfigIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ConfigIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Config Integration Tests");
  }

  @Nested
  @DisplayName("OptimizationLevel Tests")
  class OptimizationLevelTests {

    @Test
    @DisplayName("Should have all expected optimization levels")
    void shouldHaveAllExpectedOptimizationLevels() {
      LOGGER.info("Testing OptimizationLevel enum values");

      OptimizationLevel[] levels = OptimizationLevel.values();
      assertEquals(4, levels.length, "Should have 4 optimization levels");

      assertNotNull(OptimizationLevel.NONE, "NONE should exist");
      assertNotNull(OptimizationLevel.SPEED, "SPEED should exist");
      assertNotNull(OptimizationLevel.SIZE, "SIZE should exist");
      assertNotNull(OptimizationLevel.SPEED_AND_SIZE, "SPEED_AND_SIZE should exist");

      LOGGER.info("OptimizationLevel enum values verified: " + levels.length);
    }

    @Test
    @DisplayName("Should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues() {
      LOGGER.info("Testing OptimizationLevel ordinal values");

      assertEquals(0, OptimizationLevel.NONE.ordinal(), "NONE should have ordinal 0");
      assertEquals(1, OptimizationLevel.SPEED.ordinal(), "SPEED should have ordinal 1");
      assertEquals(2, OptimizationLevel.SIZE.ordinal(), "SIZE should have ordinal 2");
      assertEquals(
          3, OptimizationLevel.SPEED_AND_SIZE.ordinal(), "SPEED_AND_SIZE should have ordinal 3");

      LOGGER.info("OptimizationLevel ordinal values verified");
    }

    @Test
    @DisplayName("Should support valueOf lookup")
    void shouldSupportValueOfLookup() {
      LOGGER.info("Testing OptimizationLevel valueOf");

      assertEquals(OptimizationLevel.NONE, OptimizationLevel.valueOf("NONE"));
      assertEquals(OptimizationLevel.SPEED, OptimizationLevel.valueOf("SPEED"));
      assertEquals(OptimizationLevel.SIZE, OptimizationLevel.valueOf("SIZE"));
      assertEquals(OptimizationLevel.SPEED_AND_SIZE, OptimizationLevel.valueOf("SPEED_AND_SIZE"));

      LOGGER.info("OptimizationLevel valueOf verified");
    }
  }

  @Nested
  @DisplayName("CompilationStrategy Tests")
  class CompilationStrategyTests {

    @Test
    @DisplayName("Should have all expected compilation strategies")
    void shouldHaveAllExpectedCompilationStrategies() {
      LOGGER.info("Testing CompilationStrategy enum values");

      CompilationStrategy[] strategies = CompilationStrategy.values();
      assertEquals(5, strategies.length, "Should have 5 compilation strategies");

      assertNotNull(CompilationStrategy.AUTO, "AUTO should exist");
      assertNotNull(CompilationStrategy.SPEED, "SPEED should exist");
      assertNotNull(CompilationStrategy.PERFORMANCE, "PERFORMANCE should exist");
      assertNotNull(CompilationStrategy.SIZE, "SIZE should exist");
      assertNotNull(CompilationStrategy.DEFAULT, "DEFAULT should exist");

      LOGGER.info("CompilationStrategy enum values verified: " + strategies.length);
    }

    @Test
    @DisplayName("Should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues() {
      LOGGER.info("Testing CompilationStrategy ordinal values");

      assertEquals(0, CompilationStrategy.AUTO.ordinal(), "AUTO should have ordinal 0");
      assertEquals(1, CompilationStrategy.SPEED.ordinal(), "SPEED should have ordinal 1");
      assertEquals(
          2, CompilationStrategy.PERFORMANCE.ordinal(), "PERFORMANCE should have ordinal 2");
      assertEquals(3, CompilationStrategy.SIZE.ordinal(), "SIZE should have ordinal 3");
      assertEquals(4, CompilationStrategy.DEFAULT.ordinal(), "DEFAULT should have ordinal 4");

      LOGGER.info("CompilationStrategy ordinal values verified");
    }
  }

  @Nested
  @DisplayName("RegallocAlgorithm Tests")
  class RegallocAlgorithmTests {

    @Test
    @DisplayName("Should verify RegallocAlgorithm enum exists")
    void shouldVerifyRegallocAlgorithmEnumExists() {
      LOGGER.info("Testing RegallocAlgorithm enum existence");

      assertTrue(RegallocAlgorithm.class.isEnum(), "RegallocAlgorithm should be an enum");
      RegallocAlgorithm[] algorithms = RegallocAlgorithm.values();
      assertTrue(algorithms.length > 0, "Should have at least one algorithm");

      LOGGER.info("RegallocAlgorithm enum verified: " + algorithms.length + " algorithms");
    }
  }

  @Nested
  @DisplayName("WasmBacktraceDetails Tests")
  class WasmBacktraceDetailsTests {

    @Test
    @DisplayName("Should verify WasmBacktraceDetails enum exists")
    void shouldVerifyWasmBacktraceDetailsEnumExists() {
      LOGGER.info("Testing WasmBacktraceDetails enum existence");

      assertTrue(WasmBacktraceDetails.class.isEnum(), "WasmBacktraceDetails should be an enum");
      WasmBacktraceDetails[] details = WasmBacktraceDetails.values();
      assertTrue(details.length > 0, "Should have at least one detail level");

      LOGGER.info("WasmBacktraceDetails enum verified: " + details.length + " levels");
    }
  }

  @Nested
  @DisplayName("ResourceLimits Tests")
  class ResourceLimitsTests {

    @Test
    @DisplayName("Should verify ResourceLimits interface exists")
    void shouldVerifyResourceLimitsInterfaceExists() {
      LOGGER.info("Testing ResourceLimits interface existence");

      assertNotNull(ResourceLimits.class, "ResourceLimits class should exist");
      assertTrue(ResourceLimits.class.isInterface(), "ResourceLimits should be an interface");

      LOGGER.info("ResourceLimits interface verified");
    }
  }

  @Nested
  @DisplayName("AdvancedConfiguration Tests")
  class AdvancedConfigurationTests {

    @Test
    @DisplayName("Should verify AdvancedConfiguration class exists")
    void shouldVerifyAdvancedConfigurationClassExists() {
      LOGGER.info("Testing AdvancedConfiguration class existence");

      assertNotNull(AdvancedConfiguration.class, "AdvancedConfiguration class should exist");

      LOGGER.info("AdvancedConfiguration class verified");
    }
  }
}
