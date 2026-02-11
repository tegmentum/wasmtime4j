/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the util package classes.
 *
 * <p>This test class validates HealthCheck and LibraryValidator classes.
 */
@DisplayName("Util Package Integration Tests")
public class UtilPackageIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(UtilPackageIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Util Package Integration Tests");
  }

  @Nested
  @DisplayName("HealthCheck Tests")
  class HealthCheckTests {

    @Test
    @DisplayName("Should perform health check successfully")
    void shouldPerformHealthCheckSuccessfully() {
      LOGGER.info("Testing performHealthCheck");

      boolean result = HealthCheck.performHealthCheck();

      assertTrue(result, "Health check should pass on properly configured system");
      LOGGER.info("performHealthCheck returned: " + result);
    }

    @Test
    @DisplayName("Should return true for isReady")
    void shouldReturnTrueForIsReady() {
      LOGGER.info("Testing isReady");

      boolean result = HealthCheck.isReady();

      assertTrue(result, "isReady should return true when runtime is available");
      LOGGER.info("isReady returned: " + result);
    }

    @Test
    @DisplayName("Should return true for isLive")
    void shouldReturnTrueForIsLive() {
      LOGGER.info("Testing isLive");

      boolean result = HealthCheck.isLive();

      assertTrue(result, "isLive should always return true");
      LOGGER.info("isLive returned: " + result);
    }

    @Test
    @DisplayName("Should return consistent results for repeated health checks")
    void shouldReturnConsistentResultsForRepeatedHealthChecks() {
      LOGGER.info("Testing health check consistency");

      boolean result1 = HealthCheck.performHealthCheck();
      boolean result2 = HealthCheck.performHealthCheck();
      boolean result3 = HealthCheck.performHealthCheck();

      assertEquals(result1, result2, "Health check results should be consistent");
      assertEquals(result2, result3, "Health check results should be consistent");

      LOGGER.info("Health check consistency verified: " + result1);
    }

    @Test
    @DisplayName("Should return consistent results for isReady")
    void shouldReturnConsistentResultsForIsReady() {
      LOGGER.info("Testing isReady consistency");

      boolean result1 = HealthCheck.isReady();
      boolean result2 = HealthCheck.isReady();
      boolean result3 = HealthCheck.isReady();

      assertEquals(result1, result2, "isReady results should be consistent");
      assertEquals(result2, result3, "isReady results should be consistent");

      LOGGER.info("isReady consistency verified: " + result1);
    }

    @Test
    @DisplayName("Should execute isLive quickly")
    void shouldExecuteIsLiveQuickly() {
      LOGGER.info("Testing isLive performance");

      long startTime = System.nanoTime();
      for (int i = 0; i < 100; i++) {
        HealthCheck.isLive();
      }
      long elapsed = System.nanoTime() - startTime;
      long elapsedMs = elapsed / 1_000_000;

      assertTrue(elapsedMs < 100, "100 isLive calls should complete in under 100ms");
      LOGGER.info("100 isLive calls completed in " + elapsedMs + "ms");
    }

    @Test
    @DisplayName("Health check utility class should not be instantiable")
    void healthCheckUtilityClassShouldNotBeInstantiable() {
      LOGGER.info("Testing HealthCheck instantiation prevention");

      // HealthCheck has private constructor that throws AssertionError
      // We can verify it has private constructor through reflection
      try {
        java.lang.reflect.Constructor<?> constructor = HealthCheck.class.getDeclaredConstructor();
        assertTrue(
            java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
            "Constructor should be private");
        constructor.setAccessible(true);
        assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance(),
            "Constructor should throw AssertionError");
      } catch (NoSuchMethodException e) {
        // No constructor found - also acceptable
      }

      LOGGER.info("HealthCheck instantiation prevention verified");
    }
  }

  @Nested
  @DisplayName("LibraryValidator Tests")
  class LibraryValidatorTests {

    @Test
    @DisplayName("Should validate libraries successfully")
    void shouldValidateLibrariesSuccessfully() {
      LOGGER.info("Testing validateLibraries");

      boolean result = LibraryValidator.validateLibraries();

      assertTrue(result, "Library validation should pass on properly configured system");
      LOGGER.info("validateLibraries returned: " + result);
    }

    @Test
    @DisplayName("Should check library loading capability")
    void shouldCheckLibraryLoadingCapability() {
      LOGGER.info("Testing canLoadLibraries");

      boolean result = LibraryValidator.canLoadLibraries();

      assertTrue(result, "Should be able to load libraries on properly configured system");
      LOGGER.info("canLoadLibraries returned: " + result);
    }

    @Test
    @DisplayName("Should get runtime summary")
    void shouldGetRuntimeSummary() {
      LOGGER.info("Testing getRuntimeSummary");

      String summary = LibraryValidator.getRuntimeSummary();

      assertNotNull(summary, "Runtime summary should not be null");
      assertFalse(summary.isEmpty(), "Runtime summary should not be empty");
      assertTrue(
          summary.startsWith("Runtime Summary:"), "Summary should start with 'Runtime Summary:'");

      LOGGER.info("Runtime summary: " + summary);
    }

    @Test
    @DisplayName("Should return consistent runtime summary")
    void shouldReturnConsistentRuntimeSummary() {
      LOGGER.info("Testing runtime summary consistency");

      String summary1 = LibraryValidator.getRuntimeSummary();
      String summary2 = LibraryValidator.getRuntimeSummary();
      String summary3 = LibraryValidator.getRuntimeSummary();

      assertEquals(summary1, summary2, "Runtime summary should be consistent");
      assertEquals(summary2, summary3, "Runtime summary should be consistent");

      LOGGER.info("Runtime summary consistency verified");
    }

    @Test
    @DisplayName("Should return consistent canLoadLibraries results")
    void shouldReturnConsistentCanLoadLibrariesResults() {
      LOGGER.info("Testing canLoadLibraries consistency");

      boolean result1 = LibraryValidator.canLoadLibraries();
      boolean result2 = LibraryValidator.canLoadLibraries();
      boolean result3 = LibraryValidator.canLoadLibraries();

      assertEquals(result1, result2, "canLoadLibraries results should be consistent");
      assertEquals(result2, result3, "canLoadLibraries results should be consistent");

      LOGGER.info("canLoadLibraries consistency verified: " + result1);
    }

    @Test
    @DisplayName("Library validator utility class should not be instantiable")
    void libraryValidatorUtilityClassShouldNotBeInstantiable() {
      LOGGER.info("Testing LibraryValidator instantiation prevention");

      try {
        java.lang.reflect.Constructor<?> constructor =
            LibraryValidator.class.getDeclaredConstructor();
        assertTrue(
            java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
            "Constructor should be private");
        constructor.setAccessible(true);
        assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance(),
            "Constructor should throw AssertionError");
      } catch (NoSuchMethodException e) {
        // No constructor found - also acceptable
      }

      LOGGER.info("LibraryValidator instantiation prevention verified");
    }
  }
}
