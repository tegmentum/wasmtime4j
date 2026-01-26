/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.chaos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for chaos engineering package.
 *
 * <p>This test class validates the chaos engineering framework components.
 */
@DisplayName("Chaos Engineering Integration Tests")
public class ChaosIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ChaosIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Chaos Engineering Integration Tests");
  }

  @Nested
  @DisplayName("ChaosExperimentType Tests")
  class ChaosExperimentTypeTests {

    @Test
    @DisplayName("Should have all expected experiment types")
    void shouldHaveAllExpectedExperimentTypes() {
      LOGGER.info("Testing ChaosExperimentType enum values");

      ChaosEngineeringFramework.ChaosExperimentType[] types =
          ChaosEngineeringFramework.ChaosExperimentType.values();
      assertEquals(10, types.length, "Should have 10 experiment types");

      assertNotNull(
          ChaosEngineeringFramework.ChaosExperimentType.MEMORY_EXHAUSTION,
          "MEMORY_EXHAUSTION should exist");
      assertNotNull(
          ChaosEngineeringFramework.ChaosExperimentType.CPU_SATURATION,
          "CPU_SATURATION should exist");
      assertNotNull(
          ChaosEngineeringFramework.ChaosExperimentType.TIMEOUT_INJECTION,
          "TIMEOUT_INJECTION should exist");
      assertNotNull(
          ChaosEngineeringFramework.ChaosExperimentType.NATIVE_FAILURE,
          "NATIVE_FAILURE should exist");
      assertNotNull(
          ChaosEngineeringFramework.ChaosExperimentType.NETWORK_PARTITION,
          "NETWORK_PARTITION should exist");
      assertNotNull(
          ChaosEngineeringFramework.ChaosExperimentType.DISK_FULL, "DISK_FULL should exist");
      assertNotNull(
          ChaosEngineeringFramework.ChaosExperimentType.GRADUAL_DEGRADATION,
          "GRADUAL_DEGRADATION should exist");
      assertNotNull(
          ChaosEngineeringFramework.ChaosExperimentType.CASCADE_FAILURE,
          "CASCADE_FAILURE should exist");
      assertNotNull(
          ChaosEngineeringFramework.ChaosExperimentType.RANDOM_ERRORS,
          "RANDOM_ERRORS should exist");
      assertNotNull(
          ChaosEngineeringFramework.ChaosExperimentType.RESOURCE_STARVATION,
          "RESOURCE_STARVATION should exist");

      LOGGER.info("ChaosExperimentType enum values verified: " + types.length);
    }

    @Test
    @DisplayName("Should have descriptions for all experiment types")
    void shouldHaveDescriptionsForAllExperimentTypes() {
      LOGGER.info("Testing ChaosExperimentType descriptions");

      for (ChaosEngineeringFramework.ChaosExperimentType type :
          ChaosEngineeringFramework.ChaosExperimentType.values()) {
        assertNotNull(type.getDescription(), type.name() + " should have a description");
        assertFalse(
            type.getDescription().isEmpty(), type.name() + " description should not be empty");
      }

      LOGGER.info("ChaosExperimentType descriptions verified");
    }
  }

  @Nested
  @DisplayName("ChaosSeverity Tests")
  class ChaosSeverityTests {

    @Test
    @DisplayName("Should have all expected severity levels")
    void shouldHaveAllExpectedSeverityLevels() {
      LOGGER.info("Testing ChaosSeverity enum values");

      ChaosEngineeringFramework.ChaosSeverity[] severities =
          ChaosEngineeringFramework.ChaosSeverity.values();
      assertEquals(4, severities.length, "Should have 4 severity levels");

      assertNotNull(ChaosEngineeringFramework.ChaosSeverity.LOW, "LOW should exist");
      assertNotNull(ChaosEngineeringFramework.ChaosSeverity.MEDIUM, "MEDIUM should exist");
      assertNotNull(ChaosEngineeringFramework.ChaosSeverity.HIGH, "HIGH should exist");
      assertNotNull(ChaosEngineeringFramework.ChaosSeverity.EXTREME, "EXTREME should exist");

      LOGGER.info("ChaosSeverity enum values verified: " + severities.length);
    }

    @Test
    @DisplayName("Should have correct failure rates")
    void shouldHaveCorrectFailureRates() {
      LOGGER.info("Testing ChaosSeverity failure rates");

      assertEquals(
          0.1,
          ChaosEngineeringFramework.ChaosSeverity.LOW.getFailureRate(),
          0.001,
          "LOW should have 10% failure rate");
      assertEquals(
          0.25,
          ChaosEngineeringFramework.ChaosSeverity.MEDIUM.getFailureRate(),
          0.001,
          "MEDIUM should have 25% failure rate");
      assertEquals(
          0.5,
          ChaosEngineeringFramework.ChaosSeverity.HIGH.getFailureRate(),
          0.001,
          "HIGH should have 50% failure rate");
      assertEquals(
          0.8,
          ChaosEngineeringFramework.ChaosSeverity.EXTREME.getFailureRate(),
          0.001,
          "EXTREME should have 80% failure rate");

      LOGGER.info("ChaosSeverity failure rates verified");
    }

    @Test
    @DisplayName("Should have increasing failure rates")
    void shouldHaveIncreasingFailureRates() {
      LOGGER.info("Testing ChaosSeverity failure rate ordering");

      double lowRate = ChaosEngineeringFramework.ChaosSeverity.LOW.getFailureRate();
      double mediumRate = ChaosEngineeringFramework.ChaosSeverity.MEDIUM.getFailureRate();
      double highRate = ChaosEngineeringFramework.ChaosSeverity.HIGH.getFailureRate();
      double extremeRate = ChaosEngineeringFramework.ChaosSeverity.EXTREME.getFailureRate();

      assertTrue(lowRate < mediumRate, "LOW should be less than MEDIUM");
      assertTrue(mediumRate < highRate, "MEDIUM should be less than HIGH");
      assertTrue(highRate < extremeRate, "HIGH should be less than EXTREME");

      LOGGER.info("ChaosSeverity failure rate ordering verified");
    }
  }

  @Nested
  @DisplayName("ChaosExperiment Tests")
  class ChaosExperimentTests {

    @Test
    @DisplayName("Should create ChaosExperiment with all parameters")
    void shouldCreateChaosExperimentWithAllParameters() {
      LOGGER.info("Testing ChaosExperiment creation");

      ChaosEngineeringFramework.ChaosExperiment experiment =
          new ChaosEngineeringFramework.ChaosExperiment(
              "test-experiment-1",
              ChaosEngineeringFramework.ChaosExperimentType.MEMORY_EXHAUSTION,
              ChaosEngineeringFramework.ChaosSeverity.MEDIUM,
              Duration.ofMinutes(5),
              "test-component",
              Map.of("param1", "value1"));

      assertNotNull(experiment, "Experiment should not be null");
      assertEquals("test-experiment-1", experiment.getExperimentId(), "Experiment ID should match");
      assertEquals(
          ChaosEngineeringFramework.ChaosExperimentType.MEMORY_EXHAUSTION,
          experiment.getType(),
          "Type should match");
      assertEquals(
          ChaosEngineeringFramework.ChaosSeverity.MEDIUM,
          experiment.getSeverity(),
          "Severity should match");

      LOGGER.info("ChaosExperiment creation verified");
    }

    @Test
    @DisplayName("Should create ChaosExperiment with null parameters")
    void shouldCreateChaosExperimentWithNullParameters() {
      LOGGER.info("Testing ChaosExperiment creation with null parameters");

      ChaosEngineeringFramework.ChaosExperiment experiment =
          new ChaosEngineeringFramework.ChaosExperiment(
              "test-experiment-2",
              ChaosEngineeringFramework.ChaosExperimentType.CPU_SATURATION,
              ChaosEngineeringFramework.ChaosSeverity.LOW,
              Duration.ofSeconds(30),
              "test-component",
              null);

      assertNotNull(experiment, "Experiment should not be null");
      assertEquals("test-experiment-2", experiment.getExperimentId(), "Experiment ID should match");

      LOGGER.info("ChaosExperiment creation with null parameters verified");
    }
  }

  @Nested
  @DisplayName("ChaosEngineeringFramework Tests")
  class ChaosEngineeringFrameworkTests {

    @Test
    @DisplayName("Should verify ChaosEngineeringFramework class exists")
    void shouldVerifyChaosEngineeringFrameworkClassExists() {
      LOGGER.info("Testing ChaosEngineeringFramework class existence");

      assertNotNull(
          ChaosEngineeringFramework.class, "ChaosEngineeringFramework class should exist");
      assertFalse(
          ChaosEngineeringFramework.class.isInterface(),
          "ChaosEngineeringFramework should be a class");

      LOGGER.info("ChaosEngineeringFramework class verified");
    }
  }
}
