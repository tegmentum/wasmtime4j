/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.profiler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for profiler package.
 *
 * <p>This test class validates the profiler interface and factory components.
 */
@DisplayName("Profiler Integration Tests")
public class ProfilerIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(ProfilerIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Profiler Integration Tests");
  }

  @Nested
  @DisplayName("Profiler Interface Tests")
  class ProfilerInterfaceTests {

    @Test
    @DisplayName("Should verify Profiler interface exists")
    void shouldVerifyProfilerInterfaceExists() {
      LOGGER.info("Testing Profiler interface existence");

      assertTrue(Profiler.class.isInterface(), "Profiler should be an interface");
      assertTrue(
          AutoCloseable.class.isAssignableFrom(Profiler.class),
          "Profiler should extend AutoCloseable");

      LOGGER.info("Profiler interface verified");
    }

    @Test
    @DisplayName("Should have profiling control methods")
    void shouldHaveProfilingControlMethods() throws Exception {
      LOGGER.info("Testing Profiler profiling control methods");

      Method startProfiling = Profiler.class.getMethod("startProfiling");
      assertNotNull(startProfiling, "startProfiling method should exist");

      Method stopProfiling = Profiler.class.getMethod("stopProfiling");
      assertNotNull(stopProfiling, "stopProfiling method should exist");

      Method isProfiling = Profiler.class.getMethod("isProfiling");
      assertNotNull(isProfiling, "isProfiling method should exist");

      LOGGER.info("Profiler profiling control methods verified");
    }

    @Test
    @DisplayName("Should have function execution recording method")
    void shouldHaveFunctionExecutionRecordingMethod() throws Exception {
      LOGGER.info("Testing Profiler function execution recording method");

      Method recordFunctionExecution =
          Profiler.class.getMethod(
              "recordFunctionExecution", String.class, Duration.class, long.class);
      assertNotNull(recordFunctionExecution, "recordFunctionExecution method should exist");

      LOGGER.info("Profiler function execution recording method verified");
    }

    @Test
    @DisplayName("Should have compilation recording method")
    void shouldHaveCompilationRecordingMethod() throws Exception {
      LOGGER.info("Testing Profiler compilation recording method");

      Method recordCompilation =
          Profiler.class.getMethod(
              "recordCompilation", Duration.class, long.class, boolean.class, boolean.class);
      assertNotNull(recordCompilation, "recordCompilation method should exist");

      LOGGER.info("Profiler compilation recording method verified");
    }

    @Test
    @DisplayName("Should have compilation statistics methods")
    void shouldHaveCompilationStatisticsMethods() throws Exception {
      LOGGER.info("Testing Profiler compilation statistics methods");

      Method getModulesCompiled = Profiler.class.getMethod("getModulesCompiled");
      assertNotNull(getModulesCompiled, "getModulesCompiled method should exist");

      Method getTotalCompilationTime = Profiler.class.getMethod("getTotalCompilationTime");
      assertNotNull(getTotalCompilationTime, "getTotalCompilationTime method should exist");

      Method getAverageCompilationTime = Profiler.class.getMethod("getAverageCompilationTime");
      assertNotNull(getAverageCompilationTime, "getAverageCompilationTime method should exist");

      Method getBytesCompiled = Profiler.class.getMethod("getBytesCompiled");
      assertNotNull(getBytesCompiled, "getBytesCompiled method should exist");

      LOGGER.info("Profiler compilation statistics methods verified");
    }

    @Test
    @DisplayName("Should have cache statistics methods")
    void shouldHaveCacheStatisticsMethods() throws Exception {
      LOGGER.info("Testing Profiler cache statistics methods");

      Method getCacheHits = Profiler.class.getMethod("getCacheHits");
      assertNotNull(getCacheHits, "getCacheHits method should exist");

      Method getCacheMisses = Profiler.class.getMethod("getCacheMisses");
      assertNotNull(getCacheMisses, "getCacheMisses method should exist");

      Method getOptimizedModules = Profiler.class.getMethod("getOptimizedModules");
      assertNotNull(getOptimizedModules, "getOptimizedModules method should exist");

      LOGGER.info("Profiler cache statistics methods verified");
    }

    @Test
    @DisplayName("Should have memory statistics methods")
    void shouldHaveMemoryStatisticsMethods() throws Exception {
      LOGGER.info("Testing Profiler memory statistics methods");

      Method getCurrentMemoryBytes = Profiler.class.getMethod("getCurrentMemoryBytes");
      assertNotNull(getCurrentMemoryBytes, "getCurrentMemoryBytes method should exist");

      Method getPeakMemoryBytes = Profiler.class.getMethod("getPeakMemoryBytes");
      assertNotNull(getPeakMemoryBytes, "getPeakMemoryBytes method should exist");

      LOGGER.info("Profiler memory statistics methods verified");
    }

    @Test
    @DisplayName("Should have execution statistics methods")
    void shouldHaveExecutionStatisticsMethods() throws Exception {
      LOGGER.info("Testing Profiler execution statistics methods");

      Method getUptime = Profiler.class.getMethod("getUptime");
      assertNotNull(getUptime, "getUptime method should exist");

      Method getFunctionCallsPerSecond = Profiler.class.getMethod("getFunctionCallsPerSecond");
      assertNotNull(getFunctionCallsPerSecond, "getFunctionCallsPerSecond method should exist");

      Method getTotalFunctionCalls = Profiler.class.getMethod("getTotalFunctionCalls");
      assertNotNull(getTotalFunctionCalls, "getTotalFunctionCalls method should exist");

      Method getTotalExecutionTime = Profiler.class.getMethod("getTotalExecutionTime");
      assertNotNull(getTotalExecutionTime, "getTotalExecutionTime method should exist");

      LOGGER.info("Profiler execution statistics methods verified");
    }

    @Test
    @DisplayName("Should have reset method")
    void shouldHaveResetMethod() throws Exception {
      LOGGER.info("Testing Profiler reset method");

      Method reset = Profiler.class.getMethod("reset");
      assertNotNull(reset, "reset method should exist");

      LOGGER.info("Profiler reset method verified");
    }
  }

  @Nested
  @DisplayName("ProfilerFactory Tests")
  class ProfilerFactoryTests {

    @Test
    @DisplayName("Should verify ProfilerFactory class exists")
    void shouldVerifyProfilerFactoryClassExists() {
      LOGGER.info("Testing ProfilerFactory class existence");

      assertNotNull(ProfilerFactory.class, "ProfilerFactory class should exist");
      assertFalse(ProfilerFactory.class.isInterface(), "ProfilerFactory should be a class");
      assertTrue(
          Modifier.isFinal(ProfilerFactory.class.getModifiers()),
          "ProfilerFactory should be final");

      LOGGER.info("ProfilerFactory class verified");
    }

    @Test
    @DisplayName("Should have static create method")
    void shouldHaveStaticCreateMethod() throws Exception {
      LOGGER.info("Testing ProfilerFactory create method");

      Method createMethod = ProfilerFactory.class.getMethod("create");
      assertNotNull(createMethod, "create method should exist");
      assertTrue(Modifier.isStatic(createMethod.getModifiers()), "create method should be static");

      LOGGER.info("ProfilerFactory create method verified");
    }

    @Test
    @DisplayName("Should have private constructor")
    void shouldHavePrivateConstructor() throws Exception {
      LOGGER.info("Testing ProfilerFactory private constructor");

      var constructors = ProfilerFactory.class.getDeclaredConstructors();
      assertTrue(constructors.length > 0, "Should have at least one constructor");
      assertTrue(
          Modifier.isPrivate(constructors[0].getModifiers()), "Constructor should be private");

      LOGGER.info("ProfilerFactory private constructor verified");
    }
  }

  @Nested
  @DisplayName("ProfilerProvider Interface Tests")
  class ProfilerProviderInterfaceTests {

    @Test
    @DisplayName("Should verify ProfilerProvider nested interface exists")
    void shouldVerifyProfilerProviderNestedInterfaceExists() {
      LOGGER.info("Testing ProfilerProvider nested interface existence");

      assertTrue(
          ProfilerFactory.ProfilerProvider.class.isInterface(),
          "ProfilerProvider should be an interface");

      LOGGER.info("ProfilerProvider nested interface verified");
    }

    @Test
    @DisplayName("Should have create method")
    void shouldHaveCreateMethod() throws Exception {
      LOGGER.info("Testing ProfilerProvider create method");

      Method createMethod = ProfilerFactory.ProfilerProvider.class.getMethod("create");
      assertNotNull(createMethod, "create method should exist");

      LOGGER.info("ProfilerProvider create method verified");
    }
  }
}
