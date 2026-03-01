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
public class ConfigTest {

  private static final Logger LOGGER = Logger.getLogger(ConfigTest.class.getName());

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
      assertEquals(3, strategies.length, "Should have 3 compilation strategies");

      assertNotNull(CompilationStrategy.AUTO, "AUTO should exist");
      assertNotNull(CompilationStrategy.CRANELIFT, "CRANELIFT should exist");
      assertNotNull(CompilationStrategy.WINCH, "WINCH should exist");

      LOGGER.info("CompilationStrategy enum values verified: " + strategies.length);
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
}
