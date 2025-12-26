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

package ai.tegmentum.wasmtime4j.chaos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ChaosEngineeringFramework} class.
 *
 * <p>ChaosEngineeringFramework provides chaos engineering capabilities for testing system
 * resilience.
 */
@DisplayName("ChaosEngineeringFramework Class Tests")
class ChaosEngineeringFrameworkTest {

  @Nested
  @DisplayName("ChaosExperimentType Enum Tests")
  class ChaosExperimentTypeEnumTests {

    @Test
    @DisplayName("should have all expected experiment types")
    void shouldHaveAllExpectedExperimentTypes() {
      final ChaosEngineeringFramework.ChaosExperimentType[] types =
          ChaosEngineeringFramework.ChaosExperimentType.values();

      assertEquals(10, types.length, "Should have 10 experiment types");

      final Set<String> typeNames =
          Set.of(
              "MEMORY_EXHAUSTION",
              "CPU_SATURATION",
              "TIMEOUT_INJECTION",
              "NATIVE_FAILURE",
              "NETWORK_PARTITION",
              "DISK_FULL",
              "GRADUAL_DEGRADATION",
              "CASCADE_FAILURE",
              "RANDOM_ERRORS",
              "RESOURCE_STARVATION");

      for (final ChaosEngineeringFramework.ChaosExperimentType type : types) {
        assertTrue(typeNames.contains(type.name()), "Type " + type.name() + " should be expected");
      }
    }

    @Test
    @DisplayName("valueOf should return correct type")
    void valueOfShouldReturnCorrectType() {
      assertEquals(
          ChaosEngineeringFramework.ChaosExperimentType.MEMORY_EXHAUSTION,
          ChaosEngineeringFramework.ChaosExperimentType.valueOf("MEMORY_EXHAUSTION"));
      assertEquals(
          ChaosEngineeringFramework.ChaosExperimentType.CPU_SATURATION,
          ChaosEngineeringFramework.ChaosExperimentType.valueOf("CPU_SATURATION"));
    }

    @Test
    @DisplayName("experiment types should have description method")
    void experimentTypesShouldHaveDescriptionMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosExperimentType.class.getMethod("getDescription");
      assertNotNull(method, "getDescription method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("each experiment type should have non-empty description")
    void eachExperimentTypeShouldHaveNonEmptyDescription() {
      for (final ChaosEngineeringFramework.ChaosExperimentType type :
          ChaosEngineeringFramework.ChaosExperimentType.values()) {
        assertNotNull(type.getDescription(), type.name() + " description should not be null");
        assertFalse(
            type.getDescription().isEmpty(), type.name() + " description should not be empty");
      }
    }
  }

  @Nested
  @DisplayName("ChaosSeverity Enum Tests")
  class ChaosSeverityEnumTests {

    @Test
    @DisplayName("should have all expected severity levels")
    void shouldHaveAllExpectedSeverityLevels() {
      final ChaosEngineeringFramework.ChaosSeverity[] severities =
          ChaosEngineeringFramework.ChaosSeverity.values();

      assertEquals(4, severities.length, "Should have 4 severity levels");

      assertArrayEquals(
          new ChaosEngineeringFramework.ChaosSeverity[] {
            ChaosEngineeringFramework.ChaosSeverity.LOW,
            ChaosEngineeringFramework.ChaosSeverity.MEDIUM,
            ChaosEngineeringFramework.ChaosSeverity.HIGH,
            ChaosEngineeringFramework.ChaosSeverity.EXTREME
          },
          severities,
          "Severity levels should be in order: LOW, MEDIUM, HIGH, EXTREME");
    }

    @Test
    @DisplayName("severity levels should have failure rate method")
    void severityLevelsShouldHaveFailureRateMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosSeverity.class.getMethod("getFailureRate");
      assertNotNull(method, "getFailureRate method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("failure rates should increase with severity")
    void failureRatesShouldIncreaseWithSeverity() {
      final double lowRate = ChaosEngineeringFramework.ChaosSeverity.LOW.getFailureRate();
      final double mediumRate = ChaosEngineeringFramework.ChaosSeverity.MEDIUM.getFailureRate();
      final double highRate = ChaosEngineeringFramework.ChaosSeverity.HIGH.getFailureRate();
      final double extremeRate = ChaosEngineeringFramework.ChaosSeverity.EXTREME.getFailureRate();

      assertTrue(lowRate < mediumRate, "LOW rate should be less than MEDIUM rate");
      assertTrue(mediumRate < highRate, "MEDIUM rate should be less than HIGH rate");
      assertTrue(highRate < extremeRate, "HIGH rate should be less than EXTREME rate");
    }

    @Test
    @DisplayName("failure rates should be between 0 and 1")
    void failureRatesShouldBeBetween0And1() {
      for (final ChaosEngineeringFramework.ChaosSeverity severity :
          ChaosEngineeringFramework.ChaosSeverity.values()) {
        final double rate = severity.getFailureRate();
        assertTrue(rate >= 0.0, severity.name() + " rate should be >= 0");
        assertTrue(rate <= 1.0, severity.name() + " rate should be <= 1");
      }
    }

    @Test
    @DisplayName("valueOf should return correct severity")
    void valueOfShouldReturnCorrectSeverity() {
      assertEquals(
          ChaosEngineeringFramework.ChaosSeverity.LOW,
          ChaosEngineeringFramework.ChaosSeverity.valueOf("LOW"));
      assertEquals(
          ChaosEngineeringFramework.ChaosSeverity.EXTREME,
          ChaosEngineeringFramework.ChaosSeverity.valueOf("EXTREME"));
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(ChaosEngineeringFramework.class.getModifiers()),
          "ChaosEngineeringFramework should be a final class");
    }

    @Test
    @DisplayName("should have public constructor")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      final var constructor = ChaosEngineeringFramework.class.getConstructor();
      assertNotNull(constructor, "Public constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should be instantiable")
    void shouldBeInstantiable() {
      final ChaosEngineeringFramework framework = new ChaosEngineeringFramework();
      assertNotNull(framework, "Framework should be instantiable");
    }

    @Test
    @DisplayName("each instance should be independent")
    void eachInstanceShouldBeIndependent() {
      final ChaosEngineeringFramework framework1 = new ChaosEngineeringFramework();
      final ChaosEngineeringFramework framework2 = new ChaosEngineeringFramework();

      assertNotNull(framework1, "Instance 1 should not be null");
      assertNotNull(framework2, "Instance 2 should not be null");
      assertNotSame(framework1, framework2, "Instances should be different objects");
    }
  }

  @Nested
  @DisplayName("ChaosExperiment Class Tests")
  class ChaosExperimentClassTests {

    @Test
    @DisplayName("ChaosExperiment should be a static nested class")
    void chaosExperimentShouldBeStaticNestedClass() {
      assertTrue(
          Modifier.isStatic(ChaosEngineeringFramework.ChaosExperiment.class.getModifiers()),
          "ChaosExperiment should be static");
      assertTrue(
          Modifier.isFinal(ChaosEngineeringFramework.ChaosExperiment.class.getModifiers()),
          "ChaosExperiment should be final");
    }

    @Test
    @DisplayName("ChaosExperiment should have getExperimentId method")
    void chaosExperimentShouldHaveGetExperimentIdMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosExperiment.class.getMethod("getExperimentId");
      assertNotNull(method, "getExperimentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("ChaosExperiment should have getType method")
    void chaosExperimentShouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.ChaosExperiment.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(
          ChaosEngineeringFramework.ChaosExperimentType.class,
          method.getReturnType(),
          "Should return ChaosExperimentType");
    }

    @Test
    @DisplayName("ChaosExperiment should have getSeverity method")
    void chaosExperimentShouldHaveGetSeverityMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosExperiment.class.getMethod("getSeverity");
      assertNotNull(method, "getSeverity method should exist");
      assertEquals(
          ChaosEngineeringFramework.ChaosSeverity.class,
          method.getReturnType(),
          "Should return ChaosSeverity");
    }

    @Test
    @DisplayName("ChaosExperiment should have getDuration method")
    void chaosExperimentShouldHaveGetDurationMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosExperiment.class.getMethod("getDuration");
      assertNotNull(method, "getDuration method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("ChaosExperiment should have getTargetComponent method")
    void chaosExperimentShouldHaveGetTargetComponentMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosExperiment.class.getMethod("getTargetComponent");
      assertNotNull(method, "getTargetComponent method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("ChaosExperiment should have isEnabled method")
    void chaosExperimentShouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.ChaosExperiment.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("ChaosExperiment should have getParameters method")
    void chaosExperimentShouldHaveGetParametersMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosExperiment.class.getMethod("getParameters");
      assertNotNull(method, "getParameters method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("ChaosExperiment should have isActive method")
    void chaosExperimentShouldHaveIsActiveMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.ChaosExperiment.class.getMethod("isActive");
      assertNotNull(method, "isActive method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("ChaosExperiment should have start method")
    void chaosExperimentShouldHaveStartMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.ChaosExperiment.class.getMethod("start");
      assertNotNull(method, "start method should exist");
    }

    @Test
    @DisplayName("ChaosExperiment should have stop method")
    void chaosExperimentShouldHaveStopMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.ChaosExperiment.class.getMethod("stop");
      assertNotNull(method, "stop method should exist");
    }
  }

  @Nested
  @DisplayName("ChaosExperimentResult Class Tests")
  class ChaosExperimentResultClassTests {

    @Test
    @DisplayName("ChaosExperimentResult should be a static nested class")
    void chaosExperimentResultShouldBeStaticNestedClass() {
      assertTrue(
          Modifier.isStatic(ChaosEngineeringFramework.ChaosExperimentResult.class.getModifiers()),
          "ChaosExperimentResult should be static");
      assertTrue(
          Modifier.isFinal(ChaosEngineeringFramework.ChaosExperimentResult.class.getModifiers()),
          "ChaosExperimentResult should be final");
    }

    @Test
    @DisplayName("ChaosExperimentResult should have getExperimentId method")
    void chaosExperimentResultShouldHaveGetExperimentIdMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosExperimentResult.class.getMethod("getExperimentId");
      assertNotNull(method, "getExperimentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("ChaosExperimentResult should have isSuccessful method")
    void chaosExperimentResultShouldHaveIsSuccessfulMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosExperimentResult.class.getMethod("isSuccessful");
      assertNotNull(method, "isSuccessful method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("ChaosExperimentResult should have getExecutionTime method")
    void chaosExperimentResultShouldHaveGetExecutionTimeMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosExperimentResult.class.getMethod("getExecutionTime");
      assertNotNull(method, "getExecutionTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("ChaosExperimentResult should have getResultMessage method")
    void chaosExperimentResultShouldHaveGetResultMessageMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosExperimentResult.class.getMethod("getResultMessage");
      assertNotNull(method, "getResultMessage method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("ChaosExperimentResult should have getMetrics method")
    void chaosExperimentResultShouldHaveGetMetricsMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosExperimentResult.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("ChaosExperimentResult should have getException method")
    void chaosExperimentResultShouldHaveGetExceptionMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosExperimentResult.class.getMethod("getException");
      assertNotNull(method, "getException method should exist");
      assertEquals(Throwable.class, method.getReturnType(), "Should return Throwable");
    }

    @Test
    @DisplayName("ChaosExperimentResult should have getTimestamp method")
    void chaosExperimentResultShouldHaveGetTimestampMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.ChaosExperimentResult.class.getMethod("getTimestamp");
      assertNotNull(method, "getTimestamp method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }
  }

  @Nested
  @DisplayName("FaultInjector Interface Tests")
  class FaultInjectorInterfaceTests {

    @Test
    @DisplayName("FaultInjector should be an interface")
    void faultInjectorShouldBeInterface() {
      assertTrue(
          ChaosEngineeringFramework.FaultInjector.class.isInterface(),
          "FaultInjector should be an interface");
    }

    @Test
    @DisplayName("FaultInjector should have injectFault method")
    void faultInjectorShouldHaveInjectFaultMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.FaultInjector.class.getMethod(
              "injectFault", ChaosEngineeringFramework.ChaosExperiment.class);
      assertNotNull(method, "injectFault method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("FaultInjector should have removeFault method")
    void faultInjectorShouldHaveRemoveFaultMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.FaultInjector.class.getMethod(
              "removeFault", ChaosEngineeringFramework.ChaosExperiment.class);
      assertNotNull(method, "removeFault method should exist");
    }

    @Test
    @DisplayName("FaultInjector should have getName method")
    void faultInjectorShouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.FaultInjector.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Framework Method Tests")
  class FrameworkMethodTests {

    @Test
    @DisplayName("should have enable method")
    void shouldHaveEnableMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.class.getMethod("enable", boolean.class);
      assertNotNull(method, "enable method should exist");
    }

    @Test
    @DisplayName("should have disable method")
    void shouldHaveDisableMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.class.getMethod("disable");
      assertNotNull(method, "disable method should exist");
    }

    @Test
    @DisplayName("should have startExperiment method")
    void shouldHaveStartExperimentMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.class.getMethod(
              "startExperiment", ChaosEngineeringFramework.ChaosExperiment.class);
      assertNotNull(method, "startExperiment method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have stopExperiment method")
    void shouldHaveStopExperimentMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.class.getMethod("stopExperiment", String.class);
      assertNotNull(method, "stopExperiment method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have stopAllExperiments method")
    void shouldHaveStopAllExperimentsMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.class.getMethod("stopAllExperiments");
      assertNotNull(method, "stopAllExperiments method should exist");
    }

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isSafetyMode method")
    void shouldHaveIsSafetyModeMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.class.getMethod("isSafetyMode");
      assertNotNull(method, "isSafetyMode method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getActiveExperimentCount method")
    void shouldHaveGetActiveExperimentCountMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.class.getMethod("getActiveExperimentCount");
      assertNotNull(method, "getActiveExperimentCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getActiveExperimentsStatus method")
    void shouldHaveGetActiveExperimentsStatusMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.class.getMethod("getActiveExperimentsStatus");
      assertNotNull(method, "getActiveExperimentsStatus method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have setGlobalFailureRate method")
    void shouldHaveSetGlobalFailureRateMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.class.getMethod("setGlobalFailureRate", double.class);
      assertNotNull(method, "setGlobalFailureRate method should exist");
    }

    @Test
    @DisplayName("should have getGlobalFailureRate method")
    void shouldHaveGetGlobalFailureRateMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.class.getMethod("getGlobalFailureRate");
      assertNotNull(method, "getGlobalFailureRate method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      final Method method = ChaosEngineeringFramework.class.getMethod("shutdown");
      assertNotNull(method, "shutdown method should exist");
    }

    @Test
    @DisplayName("should have shouldInjectFault method")
    void shouldHaveShouldInjectFaultMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.class.getMethod(
              "shouldInjectFault", String.class, String.class);
      assertNotNull(method, "shouldInjectFault method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have maybeInjectNetworkLatency method")
    void shouldHaveMaybeInjectNetworkLatencyMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.class.getMethod("maybeInjectNetworkLatency", String.class);
      assertNotNull(method, "maybeInjectNetworkLatency method should exist");
    }

    @Test
    @DisplayName("should have createMemoryExhaustionExperiment method")
    void shouldHaveCreateMemoryExhaustionExperimentMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.class.getMethod(
              "createMemoryExhaustionExperiment",
              Duration.class,
              ChaosEngineeringFramework.ChaosSeverity.class);
      assertNotNull(method, "createMemoryExhaustionExperiment method should exist");
      assertEquals(
          ChaosEngineeringFramework.ChaosExperiment.class,
          method.getReturnType(),
          "Should return ChaosExperiment");
    }

    @Test
    @DisplayName("should have createCpuSaturationExperiment method")
    void shouldHaveCreateCpuSaturationExperimentMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.class.getMethod(
              "createCpuSaturationExperiment",
              Duration.class,
              ChaosEngineeringFramework.ChaosSeverity.class);
      assertNotNull(method, "createCpuSaturationExperiment method should exist");
      assertEquals(
          ChaosEngineeringFramework.ChaosExperiment.class,
          method.getReturnType(),
          "Should return ChaosExperiment");
    }

    @Test
    @DisplayName("should have createRandomErrorExperiment method")
    void shouldHaveCreateRandomErrorExperimentMethod() throws NoSuchMethodException {
      final Method method =
          ChaosEngineeringFramework.class.getMethod(
              "createRandomErrorExperiment",
              Duration.class,
              ChaosEngineeringFramework.ChaosSeverity.class);
      assertNotNull(method, "createRandomErrorExperiment method should exist");
      assertEquals(
          ChaosEngineeringFramework.ChaosExperiment.class,
          method.getReturnType(),
          "Should return ChaosExperiment");
    }
  }

  @Nested
  @DisplayName("Framework Instance Behavior Tests")
  class FrameworkInstanceBehaviorTests {

    private ChaosEngineeringFramework framework;

    @AfterEach
    void tearDown() {
      if (framework != null) {
        framework.shutdown();
      }
    }

    @Test
    @DisplayName("framework should be disabled initially")
    void frameworkShouldBeDisabledInitially() {
      framework = new ChaosEngineeringFramework();
      assertFalse(framework.isEnabled(), "Framework should be disabled initially");
    }

    @Test
    @DisplayName("framework should have zero active experiments initially")
    void frameworkShouldHaveZeroActiveExperimentsInitially() {
      framework = new ChaosEngineeringFramework();
      assertEquals(0, framework.getActiveExperimentCount(), "Should have 0 active experiments");
    }

    @Test
    @DisplayName("enable should enable framework")
    void enableShouldEnableFramework() {
      framework = new ChaosEngineeringFramework();
      framework.enable(true);
      assertTrue(framework.isEnabled(), "Framework should be enabled");
    }

    @Test
    @DisplayName("disable should disable framework")
    void disableShouldDisableFramework() {
      framework = new ChaosEngineeringFramework();
      framework.enable(true);
      framework.disable();
      assertFalse(framework.isEnabled(), "Framework should be disabled");
    }

    @Test
    @DisplayName("enable with safety should set safety mode")
    void enableWithSafetyShouldSetSafetyMode() {
      framework = new ChaosEngineeringFramework();
      framework.enable(true);
      assertTrue(framework.isSafetyMode(), "Safety mode should be enabled");
    }

    @Test
    @DisplayName("enable without safety should disable safety mode")
    void enableWithoutSafetyShouldDisableSafetyMode() {
      framework = new ChaosEngineeringFramework();
      framework.enable(false);
      assertFalse(framework.isSafetyMode(), "Safety mode should be disabled");
    }

    @Test
    @DisplayName("getStatistics should return non-null string")
    void getStatisticsShouldReturnNonNullString() {
      framework = new ChaosEngineeringFramework();
      final String stats = framework.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertFalse(stats.isEmpty(), "Statistics should not be empty");
    }

    @Test
    @DisplayName("getActiveExperimentsStatus should return string")
    void getActiveExperimentsStatusShouldReturnString() {
      framework = new ChaosEngineeringFramework();
      final String status = framework.getActiveExperimentsStatus();
      assertNotNull(status, "Status should not be null");
    }

    @Test
    @DisplayName("setGlobalFailureRate should set rate")
    void setGlobalFailureRateShouldSetRate() {
      framework = new ChaosEngineeringFramework();
      framework.setGlobalFailureRate(0.5);
      assertEquals(0.5, framework.getGlobalFailureRate(), 0.001, "Rate should be 0.5");
    }

    @Test
    @DisplayName("setGlobalFailureRate should clamp values")
    void setGlobalFailureRateShouldClampValues() {
      framework = new ChaosEngineeringFramework();

      framework.setGlobalFailureRate(-0.5);
      assertEquals(0.0, framework.getGlobalFailureRate(), 0.001, "Rate should be clamped to 0.0");

      framework.setGlobalFailureRate(1.5);
      assertEquals(1.0, framework.getGlobalFailureRate(), 0.001, "Rate should be clamped to 1.0");
    }

    @Test
    @DisplayName("shouldInjectFault should return false when disabled")
    void shouldInjectFaultShouldReturnFalseWhenDisabled() {
      framework = new ChaosEngineeringFramework();
      assertFalse(
          framework.shouldInjectFault("component", "operation"),
          "Should not inject fault when disabled");
    }

    @Test
    @DisplayName("stopExperiment should return false for non-existent experiment")
    void stopExperimentShouldReturnFalseForNonExistent() {
      framework = new ChaosEngineeringFramework();
      assertFalse(
          framework.stopExperiment("non-existent-id"),
          "Should return false for non-existent experiment");
    }
  }

  @Nested
  @DisplayName("ChaosExperiment Creation Tests")
  class ChaosExperimentCreationTests {

    private ChaosEngineeringFramework framework;

    @AfterEach
    void tearDown() {
      if (framework != null) {
        framework.shutdown();
      }
    }

    @Test
    @DisplayName("createMemoryExhaustionExperiment should return valid experiment")
    void createMemoryExhaustionExperimentShouldReturnValidExperiment() {
      framework = new ChaosEngineeringFramework();
      final ChaosEngineeringFramework.ChaosExperiment experiment =
          framework.createMemoryExhaustionExperiment(
              Duration.ofMinutes(5), ChaosEngineeringFramework.ChaosSeverity.MEDIUM);

      assertNotNull(experiment, "Experiment should not be null");
      assertEquals(
          ChaosEngineeringFramework.ChaosExperimentType.MEMORY_EXHAUSTION,
          experiment.getType(),
          "Type should be MEMORY_EXHAUSTION");
      assertEquals(
          ChaosEngineeringFramework.ChaosSeverity.MEDIUM,
          experiment.getSeverity(),
          "Severity should be MEDIUM");
      assertEquals(Duration.ofMinutes(5), experiment.getDuration(), "Duration should be 5 minutes");
      assertTrue(experiment.isEnabled(), "Experiment should be enabled");
    }

    @Test
    @DisplayName("createCpuSaturationExperiment should return valid experiment")
    void createCpuSaturationExperimentShouldReturnValidExperiment() {
      framework = new ChaosEngineeringFramework();
      final ChaosEngineeringFramework.ChaosExperiment experiment =
          framework.createCpuSaturationExperiment(
              Duration.ofMinutes(3), ChaosEngineeringFramework.ChaosSeverity.LOW);

      assertNotNull(experiment, "Experiment should not be null");
      assertEquals(
          ChaosEngineeringFramework.ChaosExperimentType.CPU_SATURATION,
          experiment.getType(),
          "Type should be CPU_SATURATION");
      assertEquals(
          ChaosEngineeringFramework.ChaosSeverity.LOW,
          experiment.getSeverity(),
          "Severity should be LOW");
    }

    @Test
    @DisplayName("createRandomErrorExperiment should return valid experiment")
    void createRandomErrorExperimentShouldReturnValidExperiment() {
      framework = new ChaosEngineeringFramework();
      final ChaosEngineeringFramework.ChaosExperiment experiment =
          framework.createRandomErrorExperiment(
              Duration.ofMinutes(10), ChaosEngineeringFramework.ChaosSeverity.HIGH);

      assertNotNull(experiment, "Experiment should not be null");
      assertEquals(
          ChaosEngineeringFramework.ChaosExperimentType.RANDOM_ERRORS,
          experiment.getType(),
          "Type should be RANDOM_ERRORS");
      assertEquals(
          ChaosEngineeringFramework.ChaosSeverity.HIGH,
          experiment.getSeverity(),
          "Severity should be HIGH");
    }

    @Test
    @DisplayName("experiment should have unique id")
    void experimentShouldHaveUniqueId() {
      framework = new ChaosEngineeringFramework();

      final ChaosEngineeringFramework.ChaosExperiment experiment1 =
          framework.createMemoryExhaustionExperiment(
              Duration.ofMinutes(1), ChaosEngineeringFramework.ChaosSeverity.LOW);

      final ChaosEngineeringFramework.ChaosExperiment experiment2 =
          framework.createMemoryExhaustionExperiment(
              Duration.ofMinutes(1), ChaosEngineeringFramework.ChaosSeverity.LOW);

      assertNotNull(experiment1.getExperimentId(), "Experiment 1 ID should not be null");
      assertNotNull(experiment2.getExperimentId(), "Experiment 2 ID should not be null");
      assertNotSame(
          experiment1.getExperimentId(),
          experiment2.getExperimentId(),
          "Experiment IDs should be different");
    }

    @Test
    @DisplayName("experiment should have target component")
    void experimentShouldHaveTargetComponent() {
      framework = new ChaosEngineeringFramework();
      final ChaosEngineeringFramework.ChaosExperiment experiment =
          framework.createMemoryExhaustionExperiment(
              Duration.ofMinutes(1), ChaosEngineeringFramework.ChaosSeverity.LOW);

      assertNotNull(experiment.getTargetComponent(), "Target component should not be null");
      assertFalse(
          experiment.getTargetComponent().isEmpty(), "Target component should not be empty");
    }
  }

  @Nested
  @DisplayName("Experiment Lifecycle Tests")
  class ExperimentLifecycleTests {

    private ChaosEngineeringFramework framework;

    @AfterEach
    void tearDown() {
      if (framework != null) {
        framework.shutdown();
      }
    }

    @Test
    @DisplayName("startExperiment should fail when framework is disabled")
    void startExperimentShouldFailWhenDisabled() {
      framework = new ChaosEngineeringFramework();
      final ChaosEngineeringFramework.ChaosExperiment experiment =
          framework.createMemoryExhaustionExperiment(
              Duration.ofMinutes(1), ChaosEngineeringFramework.ChaosSeverity.LOW);

      assertFalse(
          framework.startExperiment(experiment), "Should fail to start experiment when disabled");
    }

    @Test
    @DisplayName("startExperiment should fail for EXTREME severity in safety mode")
    void startExperimentShouldFailForExtremeSeverityInSafetyMode() {
      framework = new ChaosEngineeringFramework();
      framework.enable(true); // Enable with safety mode
      final ChaosEngineeringFramework.ChaosExperiment experiment =
          framework.createMemoryExhaustionExperiment(
              Duration.ofMinutes(1), ChaosEngineeringFramework.ChaosSeverity.EXTREME);

      assertFalse(
          framework.startExperiment(experiment),
          "Should fail to start EXTREME experiment in safety mode");
    }
  }

  @Nested
  @DisplayName("ChaosExperiment Direct Construction Tests")
  class ChaosExperimentDirectConstructionTests {

    @Test
    @DisplayName("ChaosExperiment constructor should work")
    void chaosExperimentConstructorShouldWork() {
      final ChaosEngineeringFramework.ChaosExperiment experiment =
          new ChaosEngineeringFramework.ChaosExperiment(
              "test-id",
              ChaosEngineeringFramework.ChaosExperimentType.MEMORY_EXHAUSTION,
              ChaosEngineeringFramework.ChaosSeverity.MEDIUM,
              Duration.ofMinutes(5),
              "test-component",
              Map.of("key", "value"));

      assertEquals("test-id", experiment.getExperimentId(), "ID should match");
      assertEquals(
          ChaosEngineeringFramework.ChaosExperimentType.MEMORY_EXHAUSTION,
          experiment.getType(),
          "Type should match");
      assertEquals(
          ChaosEngineeringFramework.ChaosSeverity.MEDIUM,
          experiment.getSeverity(),
          "Severity should match");
      assertEquals(Duration.ofMinutes(5), experiment.getDuration(), "Duration should match");
      assertEquals("test-component", experiment.getTargetComponent(), "Target should match");
      assertTrue(experiment.isEnabled(), "Should be enabled by default");
      assertEquals(0, experiment.getExecutionCount(), "Execution count should be 0");
      assertEquals(0, experiment.getFailureCount(), "Failure count should be 0");
    }

    @Test
    @DisplayName("ChaosExperiment should handle null parameters map")
    void chaosExperimentShouldHandleNullParametersMap() {
      final ChaosEngineeringFramework.ChaosExperiment experiment =
          new ChaosEngineeringFramework.ChaosExperiment(
              "test-id",
              ChaosEngineeringFramework.ChaosExperimentType.CPU_SATURATION,
              ChaosEngineeringFramework.ChaosSeverity.LOW,
              Duration.ofMinutes(1),
              "test-component",
              null);

      assertNotNull(experiment.getParameters(), "Parameters should not be null");
      assertTrue(experiment.getParameters().isEmpty(), "Parameters should be empty");
    }

    @Test
    @DisplayName("ChaosExperiment start and stop should work")
    void chaosExperimentStartAndStopShouldWork() {
      final ChaosEngineeringFramework.ChaosExperiment experiment =
          new ChaosEngineeringFramework.ChaosExperiment(
              "test-id",
              ChaosEngineeringFramework.ChaosExperimentType.RANDOM_ERRORS,
              ChaosEngineeringFramework.ChaosSeverity.LOW,
              Duration.ofMinutes(1),
              "test-component",
              Map.of());

      experiment.start();
      assertNotNull(experiment.getStartTime(), "Start time should be set");
      assertNotNull(experiment.getEndTime(), "End time should be set");

      experiment.stop();
      assertNotNull(experiment.getEndTime(), "End time should be updated");
    }

    @Test
    @DisplayName("ChaosExperiment recordExecution should increment count")
    void chaosExperimentRecordExecutionShouldIncrementCount() {
      final ChaosEngineeringFramework.ChaosExperiment experiment =
          new ChaosEngineeringFramework.ChaosExperiment(
              "test-id",
              ChaosEngineeringFramework.ChaosExperimentType.TIMEOUT_INJECTION,
              ChaosEngineeringFramework.ChaosSeverity.MEDIUM,
              Duration.ofMinutes(1),
              "test-component",
              Map.of());

      assertEquals(0, experiment.getExecutionCount(), "Initial count should be 0");
      experiment.recordExecution();
      assertEquals(1, experiment.getExecutionCount(), "Count should be 1 after recording");
      experiment.recordExecution();
      assertEquals(2, experiment.getExecutionCount(), "Count should be 2 after recording again");
    }

    @Test
    @DisplayName("ChaosExperiment recordFailure should increment count")
    void chaosExperimentRecordFailureShouldIncrementCount() {
      final ChaosEngineeringFramework.ChaosExperiment experiment =
          new ChaosEngineeringFramework.ChaosExperiment(
              "test-id",
              ChaosEngineeringFramework.ChaosExperimentType.NETWORK_PARTITION,
              ChaosEngineeringFramework.ChaosSeverity.HIGH,
              Duration.ofMinutes(1),
              "test-component",
              Map.of());

      assertEquals(0, experiment.getFailureCount(), "Initial count should be 0");
      experiment.recordFailure();
      assertEquals(1, experiment.getFailureCount(), "Count should be 1 after recording");
    }
  }

  @Nested
  @DisplayName("ChaosExperimentResult Direct Construction Tests")
  class ChaosExperimentResultDirectConstructionTests {

    @Test
    @DisplayName("ChaosExperimentResult constructor should work")
    void chaosExperimentResultConstructorShouldWork() {
      final ChaosEngineeringFramework.ChaosExperimentResult result =
          new ChaosEngineeringFramework.ChaosExperimentResult(
              "test-id",
              true,
              Duration.ofSeconds(30),
              "Test completed",
              Map.of("metric1", 100),
              null);

      assertEquals("test-id", result.getExperimentId(), "ID should match");
      assertTrue(result.isSuccessful(), "Should be successful");
      assertEquals(Duration.ofSeconds(30), result.getExecutionTime(), "Duration should match");
      assertEquals("Test completed", result.getResultMessage(), "Message should match");
      assertNotNull(result.getMetrics(), "Metrics should not be null");
      assertEquals(100, result.getMetrics().get("metric1"), "Metric should match");
      assertNotNull(result.getTimestamp(), "Timestamp should be set");
    }

    @Test
    @DisplayName("ChaosExperimentResult should store exception")
    void chaosExperimentResultShouldStoreException() {
      final RuntimeException exception = new RuntimeException("Test error");
      final ChaosEngineeringFramework.ChaosExperimentResult result =
          new ChaosEngineeringFramework.ChaosExperimentResult(
              "test-id", false, Duration.ofSeconds(5), "Test failed", Map.of(), exception);

      assertFalse(result.isSuccessful(), "Should not be successful");
      assertEquals(exception, result.getException(), "Exception should match");
    }

    @Test
    @DisplayName("ChaosExperimentResult should handle null metrics")
    void chaosExperimentResultShouldHandleNullMetrics() {
      final ChaosEngineeringFramework.ChaosExperimentResult result =
          new ChaosEngineeringFramework.ChaosExperimentResult(
              "test-id", true, Duration.ofSeconds(10), "OK", null, null);

      assertNotNull(result.getMetrics(), "Metrics should not be null");
      assertTrue(result.getMetrics().isEmpty(), "Metrics should be empty");
    }
  }
}
