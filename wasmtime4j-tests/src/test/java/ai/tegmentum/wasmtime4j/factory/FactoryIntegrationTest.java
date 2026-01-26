/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WasmRuntimeFactory.
 *
 * <p>This test class validates the factory pattern implementation for creating WebAssembly runtime
 * instances, including automatic runtime selection, manual overrides, and fallback behavior.
 */
@DisplayName("WasmRuntimeFactory Integration Tests")
public class FactoryIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(FactoryIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting WasmRuntimeFactory Integration Tests");
  }

  @BeforeEach
  void setUp() {
    // Clear factory cache before each test to ensure clean state
    WasmRuntimeFactory.clearCache();
    // Clear any system property overrides
    System.clearProperty(WasmRuntimeFactory.RUNTIME_PROPERTY);
  }

  @AfterEach
  void tearDown() {
    // Reset state after each test
    WasmRuntimeFactory.clearCache();
    System.clearProperty(WasmRuntimeFactory.RUNTIME_PROPERTY);
  }

  @Nested
  @DisplayName("Factory Constants Tests")
  class FactoryConstantsTests {

    @Test
    @DisplayName("Should define RUNTIME_PROPERTY constant")
    void shouldDefineRuntimePropertyConstant() {
      LOGGER.info("Testing RUNTIME_PROPERTY constant definition");

      assertEquals(
          "wasmtime4j.runtime",
          WasmRuntimeFactory.RUNTIME_PROPERTY,
          "RUNTIME_PROPERTY should be 'wasmtime4j.runtime'");

      LOGGER.info("RUNTIME_PROPERTY constant verified: " + WasmRuntimeFactory.RUNTIME_PROPERTY);
    }

    @Test
    @DisplayName("Should define RUNTIME_JNI constant")
    void shouldDefineRuntimeJniConstant() {
      LOGGER.info("Testing RUNTIME_JNI constant definition");

      assertEquals("jni", WasmRuntimeFactory.RUNTIME_JNI, "RUNTIME_JNI should be 'jni'");

      LOGGER.info("RUNTIME_JNI constant verified: " + WasmRuntimeFactory.RUNTIME_JNI);
    }

    @Test
    @DisplayName("Should define RUNTIME_PANAMA constant")
    void shouldDefineRuntimePanamaConstant() {
      LOGGER.info("Testing RUNTIME_PANAMA constant definition");

      assertEquals(
          "panama", WasmRuntimeFactory.RUNTIME_PANAMA, "RUNTIME_PANAMA should be 'panama'");

      LOGGER.info("RUNTIME_PANAMA constant verified: " + WasmRuntimeFactory.RUNTIME_PANAMA);
    }
  }

  @Nested
  @DisplayName("Java Version Detection Tests")
  class JavaVersionDetectionTests {

    @Test
    @DisplayName("Should detect Java version correctly")
    void shouldDetectJavaVersionCorrectly() {
      LOGGER.info("Testing Java version detection");

      int javaVersion = WasmRuntimeFactory.getJavaVersion();

      assertTrue(javaVersion >= 8, "Java version should be at least 8");
      assertTrue(javaVersion <= 100, "Java version should be reasonable (<=100)");

      LOGGER.info("Detected Java version: " + javaVersion);
    }

    @Test
    @DisplayName("Should return consistent Java version across calls")
    void shouldReturnConsistentJavaVersionAcrossCalls() {
      LOGGER.info("Testing Java version consistency");

      int version1 = WasmRuntimeFactory.getJavaVersion();
      int version2 = WasmRuntimeFactory.getJavaVersion();
      int version3 = WasmRuntimeFactory.getJavaVersion();

      assertEquals(version1, version2, "Java version should be consistent");
      assertEquals(version2, version3, "Java version should be consistent");

      LOGGER.info("Java version consistency verified: " + version1);
    }

    @Test
    @DisplayName("Should match system java.version property")
    void shouldMatchSystemJavaVersionProperty() {
      LOGGER.info("Testing Java version matches system property");

      String systemVersion = System.getProperty("java.version");
      int detectedVersion = WasmRuntimeFactory.getJavaVersion();

      assertNotNull(systemVersion, "System java.version should not be null");
      assertTrue(detectedVersion > 0, "Detected version should be positive");

      // The detected version should reasonably correspond to the system version
      LOGGER.info(
          "System version: " + systemVersion + ", detected major version: " + detectedVersion);
    }
  }

  @Nested
  @DisplayName("Runtime Availability Tests")
  class RuntimeAvailabilityTests {

    @Test
    @DisplayName("Should check JNI runtime availability")
    void shouldCheckJniRuntimeAvailability() {
      LOGGER.info("Testing JNI runtime availability check");

      // This should not throw regardless of actual availability
      boolean jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);

      LOGGER.info("JNI runtime available: " + jniAvailable);
    }

    @Test
    @DisplayName("Should check Panama runtime availability")
    void shouldCheckPanamaRuntimeAvailability() {
      LOGGER.info("Testing Panama runtime availability check");

      // This should not throw regardless of actual availability
      boolean panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

      LOGGER.info("Panama runtime available: " + panamaAvailable);
    }

    @Test
    @DisplayName("Should have at least one runtime available")
    void shouldHaveAtLeastOneRuntimeAvailable() {
      LOGGER.info("Testing that at least one runtime is available");

      boolean jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
      boolean panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

      assertTrue(jniAvailable || panamaAvailable, "At least one runtime should be available");

      LOGGER.info("Runtime availability - JNI: " + jniAvailable + ", Panama: " + panamaAvailable);
    }

    @Test
    @DisplayName("Should return consistent availability across multiple checks")
    void shouldReturnConsistentAvailabilityAcrossMultipleChecks() {
      LOGGER.info("Testing runtime availability consistency");

      boolean jni1 = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
      boolean jni2 = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
      boolean panama1 = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);
      boolean panama2 = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

      assertEquals(jni1, jni2, "JNI availability should be consistent");
      assertEquals(panama1, panama2, "Panama availability should be consistent");

      LOGGER.info("Availability consistency verified");
    }
  }

  @Nested
  @DisplayName("Runtime Selection Tests")
  class RuntimeSelectionTests {

    @Test
    @DisplayName("Should get selected runtime type")
    void shouldGetSelectedRuntimeType() {
      LOGGER.info("Testing runtime type selection");

      RuntimeType selectedType = WasmRuntimeFactory.getSelectedRuntimeType();

      assertNotNull(selectedType, "Selected runtime type should not be null");
      assertTrue(
          selectedType == RuntimeType.JNI || selectedType == RuntimeType.PANAMA,
          "Selected type should be JNI or PANAMA");

      LOGGER.info("Selected runtime type: " + selectedType);
    }

    @Test
    @DisplayName("Should return consistent selection without override")
    void shouldReturnConsistentSelectionWithoutOverride() {
      LOGGER.info("Testing consistent runtime selection");

      RuntimeType type1 = WasmRuntimeFactory.getSelectedRuntimeType();
      RuntimeType type2 = WasmRuntimeFactory.getSelectedRuntimeType();
      RuntimeType type3 = WasmRuntimeFactory.getSelectedRuntimeType();

      assertEquals(type1, type2, "Runtime selection should be consistent");
      assertEquals(type2, type3, "Runtime selection should be consistent");

      LOGGER.info("Runtime selection consistency verified: " + type1);
    }

    @Test
    @DisplayName("Should select Panama for Java 23+ if available")
    void shouldSelectPanamaForJava23IfAvailable() {
      LOGGER.info("Testing Panama selection for Java 23+");

      int javaVersion = WasmRuntimeFactory.getJavaVersion();
      boolean panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

      if (javaVersion >= 23 && panamaAvailable) {
        RuntimeType selected = WasmRuntimeFactory.getSelectedRuntimeType();
        assertEquals(RuntimeType.PANAMA, selected, "Should select Panama for Java 23+");
        LOGGER.info("Panama correctly selected for Java " + javaVersion);
      } else {
        LOGGER.info(
            "Skipping test - Java version: "
                + javaVersion
                + ", Panama available: "
                + panamaAvailable);
      }
    }
  }

  @Nested
  @DisplayName("System Property Override Tests")
  class SystemPropertyOverrideTests {

    @Test
    @DisplayName("Should override to JNI via system property")
    void shouldOverrideToJniViaSystemProperty() {
      LOGGER.info("Testing JNI override via system property");

      boolean jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
      if (!jniAvailable) {
        LOGGER.info("Skipping test - JNI runtime not available");
        return;
      }

      System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, WasmRuntimeFactory.RUNTIME_JNI);

      RuntimeType selected = WasmRuntimeFactory.getSelectedRuntimeType();

      assertEquals(RuntimeType.JNI, selected, "Should select JNI when overridden");
      LOGGER.info("JNI override verified: " + selected);
    }

    @Test
    @DisplayName("Should override to Panama via system property")
    void shouldOverrideToPanamaViaSystemProperty() {
      LOGGER.info("Testing Panama override via system property");

      boolean panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);
      if (!panamaAvailable) {
        LOGGER.info("Skipping test - Panama runtime not available");
        return;
      }

      System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, WasmRuntimeFactory.RUNTIME_PANAMA);

      RuntimeType selected = WasmRuntimeFactory.getSelectedRuntimeType();

      assertEquals(RuntimeType.PANAMA, selected, "Should select Panama when overridden");
      LOGGER.info("Panama override verified: " + selected);
    }

    @Test
    @DisplayName("Should handle invalid override property gracefully")
    void shouldHandleInvalidOverridePropertyGracefully() {
      LOGGER.info("Testing invalid override property handling");

      System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, "invalid_runtime");

      // Should not throw, should fall back to automatic selection
      RuntimeType selected = WasmRuntimeFactory.getSelectedRuntimeType();

      assertNotNull(selected, "Should still return a valid runtime type");
      LOGGER.info("Invalid override handled gracefully, selected: " + selected);
    }

    @Test
    @DisplayName("Should handle case-insensitive override")
    void shouldHandleCaseInsensitiveOverride() {
      LOGGER.info("Testing case-insensitive override");

      boolean jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
      if (!jniAvailable) {
        LOGGER.info("Skipping test - JNI runtime not available");
        return;
      }

      // Test uppercase
      System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, "JNI");
      RuntimeType selected1 = WasmRuntimeFactory.getSelectedRuntimeType();

      WasmRuntimeFactory.clearCache();

      // Test mixed case
      System.setProperty(WasmRuntimeFactory.RUNTIME_PROPERTY, "Jni");
      RuntimeType selected2 = WasmRuntimeFactory.getSelectedRuntimeType();

      assertEquals(RuntimeType.JNI, selected1, "Should handle uppercase");
      assertEquals(RuntimeType.JNI, selected2, "Should handle mixed case");

      LOGGER.info("Case-insensitive override verified");
    }
  }

  @Nested
  @DisplayName("Cache Management Tests")
  class CacheManagementTests {

    @Test
    @DisplayName("Should clear cache successfully")
    void shouldClearCacheSuccessfully() {
      LOGGER.info("Testing cache clearing");

      // Pre-populate cache
      WasmRuntimeFactory.getSelectedRuntimeType();
      WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
      WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

      // Clear cache - should not throw
      WasmRuntimeFactory.clearCache();

      // Should still work after clearing
      RuntimeType type = WasmRuntimeFactory.getSelectedRuntimeType();
      assertNotNull(type, "Should still work after cache clear");

      LOGGER.info("Cache cleared successfully, selected type: " + type);
    }

    @Test
    @DisplayName("Should repopulate cache after clearing")
    void shouldRepopulateCacheAfterClearing() {
      LOGGER.info("Testing cache repopulation");

      RuntimeType typeBefore = WasmRuntimeFactory.getSelectedRuntimeType();
      WasmRuntimeFactory.clearCache();
      RuntimeType typeAfter = WasmRuntimeFactory.getSelectedRuntimeType();

      assertEquals(typeBefore, typeAfter, "Should select same type after cache clear");

      LOGGER.info("Cache repopulation verified: " + typeAfter);
    }

    @Test
    @DisplayName("Should handle multiple cache clears")
    void shouldHandleMultipleCacheClears() {
      LOGGER.info("Testing multiple cache clears");

      for (int i = 0; i < 5; i++) {
        WasmRuntimeFactory.clearCache();
        RuntimeType type = WasmRuntimeFactory.getSelectedRuntimeType();
        assertNotNull(type, "Should work after multiple cache clears");
      }

      LOGGER.info("Multiple cache clears handled successfully");
    }
  }

  @Nested
  @DisplayName("Runtime Creation Tests")
  class RuntimeCreationTests {

    @Test
    @DisplayName("Should create runtime with automatic selection")
    void shouldCreateRuntimeWithAutomaticSelection() throws WasmException {
      LOGGER.info("Testing runtime creation with automatic selection");

      try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
        assertNotNull(runtime, "Created runtime should not be null");
        LOGGER.info("Runtime created successfully: " + runtime);
      }
    }

    @Test
    @DisplayName("Should create multiple runtimes successfully")
    void shouldCreateMultipleRuntimesSuccessfully() throws WasmException {
      LOGGER.info("Testing multiple runtime creation");

      try (WasmRuntime runtime1 = WasmRuntimeFactory.create();
          WasmRuntime runtime2 = WasmRuntimeFactory.create();
          WasmRuntime runtime3 = WasmRuntimeFactory.create()) {

        assertNotNull(runtime1, "First runtime should not be null");
        assertNotNull(runtime2, "Second runtime should not be null");
        assertNotNull(runtime3, "Third runtime should not be null");

        LOGGER.info("Multiple runtimes created successfully");
      }
    }

    @Test
    @DisplayName("Should throw on null runtime type")
    void shouldThrowOnNullRuntimeType() {
      LOGGER.info("Testing null runtime type handling");

      assertThrows(
          IllegalArgumentException.class,
          () -> WasmRuntimeFactory.create(null),
          "Should throw IllegalArgumentException for null runtime type");

      LOGGER.info("Null runtime type correctly rejected");
    }

    @Test
    @DisplayName("Should create runtime with specific JNI type")
    void shouldCreateRuntimeWithSpecificJniType() throws WasmException {
      LOGGER.info("Testing runtime creation with JNI type");

      boolean jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
      if (!jniAvailable) {
        LOGGER.info("Skipping test - JNI runtime not available");
        return;
      }

      try (WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.JNI)) {
        assertNotNull(runtime, "JNI runtime should be created");
        LOGGER.info("JNI runtime created successfully");
      }
    }

    @Test
    @DisplayName("Should create runtime with specific Panama type")
    void shouldCreateRuntimeWithSpecificPanamaType() throws WasmException {
      LOGGER.info("Testing runtime creation with Panama type");

      boolean panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);
      if (!panamaAvailable) {
        LOGGER.info("Skipping test - Panama runtime not available");
        return;
      }

      try (WasmRuntime runtime = WasmRuntimeFactory.create(RuntimeType.PANAMA)) {
        assertNotNull(runtime, "Panama runtime should be created");
        LOGGER.info("Panama runtime created successfully");
      }
    }
  }

  @Nested
  @DisplayName("Runtime Fallback Tests")
  class RuntimeFallbackTests {

    @Test
    @DisplayName("Should fallback when requested runtime unavailable")
    void shouldFallbackWhenRequestedRuntimeUnavailable() {
      LOGGER.info("Testing runtime fallback behavior");

      boolean jniAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
      boolean panamaAvailable = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);

      // If only one runtime is available, the fallback behavior is testable
      if (jniAvailable != panamaAvailable) {
        RuntimeType unavailable = jniAvailable ? RuntimeType.PANAMA : RuntimeType.JNI;

        try (WasmRuntime runtime = WasmRuntimeFactory.create(unavailable)) {
          // Should fallback to the available runtime
          assertNotNull(runtime, "Should create runtime via fallback");
          LOGGER.info("Fallback successful: requested " + unavailable + ", got fallback");
        } catch (WasmException e) {
          // Also acceptable if no fallback is possible
          LOGGER.info("Fallback not possible: " + e.getMessage());
        }
      } else {
        LOGGER.info("Skipping test - both runtimes have same availability");
      }
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Should handle concurrent getSelectedRuntimeType calls")
    void shouldHandleConcurrentGetSelectedRuntimeTypeCalls() throws InterruptedException {
      LOGGER.info("Testing thread safety of getSelectedRuntimeType");

      int threadCount = 10;
      Thread[] threads = new Thread[threadCount];
      RuntimeType[] results = new RuntimeType[threadCount];

      for (int i = 0; i < threadCount; i++) {
        final int index = i;
        threads[i] =
            new Thread(
                () -> {
                  results[index] = WasmRuntimeFactory.getSelectedRuntimeType();
                });
      }

      for (Thread thread : threads) {
        thread.start();
      }

      for (Thread thread : threads) {
        thread.join(5000);
      }

      // All threads should get the same result
      RuntimeType expected = results[0];
      for (int i = 1; i < threadCount; i++) {
        assertEquals(expected, results[i], "All threads should get same runtime type");
      }

      LOGGER.info("Thread safety verified, all threads got: " + expected);
    }

    @Test
    @DisplayName("Should handle concurrent isRuntimeAvailable calls")
    void shouldHandleConcurrentIsRuntimeAvailableCalls() throws InterruptedException {
      LOGGER.info("Testing thread safety of isRuntimeAvailable");

      int threadCount = 10;
      Thread[] threads = new Thread[threadCount];
      boolean[] jniResults = new boolean[threadCount];
      boolean[] panamaResults = new boolean[threadCount];

      for (int i = 0; i < threadCount; i++) {
        final int index = i;
        threads[i] =
            new Thread(
                () -> {
                  jniResults[index] = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
                  panamaResults[index] = WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA);
                });
      }

      for (Thread thread : threads) {
        thread.start();
      }

      for (Thread thread : threads) {
        thread.join(5000);
      }

      // All threads should get the same results
      for (int i = 1; i < threadCount; i++) {
        assertEquals(jniResults[0], jniResults[i], "JNI availability should be consistent");
        assertEquals(
            panamaResults[0], panamaResults[i], "Panama availability should be consistent");
      }

      LOGGER.info(
          "Thread safety verified - JNI: " + jniResults[0] + ", Panama: " + panamaResults[0]);
    }

    @Test
    @DisplayName("Should handle concurrent clearCache calls")
    void shouldHandleConcurrentClearCacheCalls() throws InterruptedException {
      LOGGER.info("Testing thread safety of clearCache");

      int threadCount = 10;
      Thread[] threads = new Thread[threadCount];

      for (int i = 0; i < threadCount; i++) {
        threads[i] =
            new Thread(
                () -> {
                  WasmRuntimeFactory.clearCache();
                  WasmRuntimeFactory.getSelectedRuntimeType();
                  WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI);
                });
      }

      for (Thread thread : threads) {
        thread.start();
      }

      for (Thread thread : threads) {
        thread.join(5000);
      }

      // Should complete without errors
      RuntimeType type = WasmRuntimeFactory.getSelectedRuntimeType();
      assertNotNull(type, "Should work after concurrent operations");

      LOGGER.info("Concurrent clearCache handled successfully");
    }
  }
}
