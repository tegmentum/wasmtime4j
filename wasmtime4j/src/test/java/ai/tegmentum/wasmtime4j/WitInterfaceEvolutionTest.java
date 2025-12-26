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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WitInterfaceEvolution} interface.
 *
 * <p>This test class verifies the interface structure, method signatures, nested interfaces, and
 * enums for the WIT interface evolution and versioning API.
 */
@DisplayName("WitInterfaceEvolution Tests")
class WitInterfaceEvolutionTest {

  // ========================================================================
  // Main Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WitInterfaceEvolution should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WitInterfaceEvolution.class.isInterface(),
          "WitInterfaceEvolution should be an interface");
    }

    @Test
    @DisplayName("WitInterfaceEvolution should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WitInterfaceEvolution.class.getModifiers()),
          "WitInterfaceEvolution should be public");
    }

    @Test
    @DisplayName("WitInterfaceEvolution should not extend any interface")
    void shouldNotExtendAnyInterface() {
      Class<?>[] interfaces = WitInterfaceEvolution.class.getInterfaces();
      assertEquals(0, interfaces.length, "WitInterfaceEvolution should not extend any interface");
    }
  }

  @Nested
  @DisplayName("Main Interface Method Tests")
  class MainInterfaceMethodTests {

    @Test
    @DisplayName("should have analyzeEvolution method")
    void shouldHaveAnalyzeEvolutionMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.class.getMethod(
              "analyzeEvolution", WitInterfaceVersion.class, WitInterfaceVersion.class);
      assertNotNull(method, "analyzeEvolution method should exist");
      assertEquals(
          WitInterfaceEvolution.InterfaceEvolutionAnalysis.class,
          method.getReturnType(),
          "Return type should be InterfaceEvolutionAnalysis");
      assertEquals(2, method.getParameterCount(), "analyzeEvolution should have 2 parameters");

      // Should throw WasmException
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have checkBackwardCompatibility method")
    void shouldHaveCheckBackwardCompatibilityMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.class.getMethod(
              "checkBackwardCompatibility", WitInterfaceVersion.class, WitInterfaceVersion.class);
      assertNotNull(method, "checkBackwardCompatibility method should exist");
      assertEquals(
          WitInterfaceEvolution.BackwardCompatibilityResult.class,
          method.getReturnType(),
          "Return type should be BackwardCompatibilityResult");
    }

    @Test
    @DisplayName("should have checkForwardCompatibility method")
    void shouldHaveCheckForwardCompatibilityMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.class.getMethod(
              "checkForwardCompatibility", WitInterfaceVersion.class, WitInterfaceVersion.class);
      assertNotNull(method, "checkForwardCompatibility method should exist");
      assertEquals(
          WitInterfaceEvolution.ForwardCompatibilityResult.class,
          method.getReturnType(),
          "Return type should be ForwardCompatibilityResult");
    }

    @Test
    @DisplayName("should have createAdapter method")
    void shouldHaveCreateAdapterMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.class.getMethod(
              "createAdapter",
              WitInterfaceVersion.class,
              WitInterfaceVersion.class,
              AdaptationConfig.class);
      assertNotNull(method, "createAdapter method should exist");
      assertEquals(
          WitInterfaceEvolution.InterfaceAdapter.class,
          method.getReturnType(),
          "Return type should be InterfaceAdapter");
      assertEquals(3, method.getParameterCount(), "createAdapter should have 3 parameters");

      // Should throw WasmException
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have validateEvolutionStrategy method")
    void shouldHaveValidateEvolutionStrategyMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.class.getMethod(
              "validateEvolutionStrategy", InterfaceEvolutionStrategy.class);
      assertNotNull(method, "validateEvolutionStrategy method should exist");
      assertEquals(
          EvolutionValidationResult.class,
          method.getReturnType(),
          "Return type should be EvolutionValidationResult");
    }

    @Test
    @DisplayName("should have createMigrationPlan method")
    void shouldHaveCreateMigrationPlanMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.class.getMethod(
              "createMigrationPlan",
              WitInterfaceDefinition.class,
              WitInterfaceDefinition.class,
              MigrationConfig.class);
      assertNotNull(method, "createMigrationPlan method should exist");
      assertEquals(
          WitInterfaceEvolution.InterfaceMigrationPlan.class,
          method.getReturnType(),
          "Return type should be InterfaceMigrationPlan");
    }

    @Test
    @DisplayName("should have executeMigration method")
    void shouldHaveExecuteMigrationMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.class.getMethod(
              "executeMigration", WitInterfaceEvolution.InterfaceMigrationPlan.class);
      assertNotNull(method, "executeMigration method should exist");
      assertEquals(
          WitInterfaceEvolution.MigrationExecutionResult.class,
          method.getReturnType(),
          "Return type should be MigrationExecutionResult");
    }

    @Test
    @DisplayName("should have getEvolutionHistory method")
    void shouldHaveGetEvolutionHistoryMethod() throws NoSuchMethodException {
      Method method = WitInterfaceEvolution.class.getMethod("getEvolutionHistory", String.class);
      assertNotNull(method, "getEvolutionHistory method should exist");
      assertEquals(
          WitInterfaceEvolution.InterfaceEvolutionHistory.class,
          method.getReturnType(),
          "Return type should be InterfaceEvolutionHistory");
    }

    @Test
    @DisplayName("should have registerInterfaceVersion method")
    void shouldHaveRegisterInterfaceVersionMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.class.getMethod(
              "registerInterfaceVersion", WitInterfaceVersion.class);
      assertNotNull(method, "registerInterfaceVersion method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");

      // Should throw WasmException
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "Should throw 1 exception");
    }

    @Test
    @DisplayName("should have deprecateInterfaceVersion method")
    void shouldHaveDeprecateInterfaceVersionMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.class.getMethod(
              "deprecateInterfaceVersion",
              WitInterfaceVersion.class,
              WitInterfaceEvolution.DeprecationInfo.class);
      assertNotNull(method, "deprecateInterfaceVersion method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getInterfaceVersions method")
    void shouldHaveGetInterfaceVersionsMethod() throws NoSuchMethodException {
      Method method = WitInterfaceEvolution.class.getMethod("getInterfaceVersions", String.class);
      assertNotNull(method, "getInterfaceVersions method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have findCompatibleVersion method")
    void shouldHaveFindCompatibleVersionMethod() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.class.getMethod(
              "findCompatibleVersion", String.class, CompatibilityRequirements.class);
      assertNotNull(method, "findCompatibleVersion method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Return type should be Optional");
    }
  }

  // ========================================================================
  // Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("InterfaceEvolutionAnalysis Interface Tests")
  class InterfaceEvolutionAnalysisTests {

    @Test
    @DisplayName("InterfaceEvolutionAnalysis should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceEvolution.InterfaceEvolutionAnalysis.class;
      assertTrue(nestedClass.isInterface(), "InterfaceEvolutionAnalysis should be an interface");
    }

    @Test
    @DisplayName("InterfaceEvolutionAnalysis should have all required methods")
    void shouldHaveAllRequiredMethods() {
      Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "getSourceVersion",
                  "getTargetVersion",
                  "getEvolutionType",
                  "getBreakingChanges",
                  "getNonBreakingChanges",
                  "getRequiredAdaptations",
                  "getMigrationComplexity",
                  "getEstimatedEffort"));

      Set<String> actualMethods = new HashSet<>();
      for (Method m : WitInterfaceEvolution.InterfaceEvolutionAnalysis.class.getDeclaredMethods()) {
        actualMethods.add(m.getName());
      }

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected),
            "InterfaceEvolutionAnalysis should have method: " + expected);
      }
    }

    @Test
    @DisplayName("getEvolutionType should return EvolutionType")
    void getEvolutionTypeShouldReturnEvolutionType() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.InterfaceEvolutionAnalysis.class.getMethod("getEvolutionType");
      assertEquals(
          WitInterfaceEvolution.EvolutionType.class,
          method.getReturnType(),
          "Should return EvolutionType");
    }

    @Test
    @DisplayName("getMigrationComplexity should return MigrationComplexity")
    void getMigrationComplexityShouldReturnMigrationComplexity() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.InterfaceEvolutionAnalysis.class.getMethod(
              "getMigrationComplexity");
      assertEquals(
          WitInterfaceEvolution.MigrationComplexity.class,
          method.getReturnType(),
          "Should return MigrationComplexity");
    }
  }

  @Nested
  @DisplayName("BackwardCompatibilityResult Interface Tests")
  class BackwardCompatibilityResultTests {

    @Test
    @DisplayName("BackwardCompatibilityResult should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceEvolution.BackwardCompatibilityResult.class;
      assertTrue(nestedClass.isInterface(), "BackwardCompatibilityResult should be an interface");
    }

    @Test
    @DisplayName("isBackwardCompatible should return boolean")
    void isBackwardCompatibleShouldReturnBoolean() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.BackwardCompatibilityResult.class.getMethod("isBackwardCompatible");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("getCompatibilityLevel should return CompatibilityLevel")
    void getCompatibilityLevelShouldReturnCompatibilityLevel() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.BackwardCompatibilityResult.class.getMethod(
              "getCompatibilityLevel");
      assertEquals(
          WitInterfaceEvolution.CompatibilityLevel.class,
          method.getReturnType(),
          "Should return CompatibilityLevel");
    }
  }

  @Nested
  @DisplayName("ForwardCompatibilityResult Interface Tests")
  class ForwardCompatibilityResultTests {

    @Test
    @DisplayName("ForwardCompatibilityResult should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceEvolution.ForwardCompatibilityResult.class;
      assertTrue(nestedClass.isInterface(), "ForwardCompatibilityResult should be an interface");
    }

    @Test
    @DisplayName("isForwardCompatible should return boolean")
    void isForwardCompatibleShouldReturnBoolean() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.ForwardCompatibilityResult.class.getMethod("isForwardCompatible");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("getRiskAssessment should return RiskAssessment")
    void getRiskAssessmentShouldReturnRiskAssessment() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.ForwardCompatibilityResult.class.getMethod("getRiskAssessment");
      assertEquals(
          WitInterfaceEvolution.RiskAssessment.class,
          method.getReturnType(),
          "Should return RiskAssessment");
    }
  }

  @Nested
  @DisplayName("InterfaceAdapter Interface Tests")
  class InterfaceAdapterTests {

    @Test
    @DisplayName("InterfaceAdapter should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceEvolution.InterfaceAdapter.class;
      assertTrue(nestedClass.isInterface(), "InterfaceAdapter should be an interface");
    }

    @Test
    @DisplayName("adaptCall should have correct signature")
    void adaptCallShouldHaveCorrectSignature() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.InterfaceAdapter.class.getMethod(
              "adaptCall", String.class, WasmValue[].class);
      assertNotNull(method, "adaptCall method should exist");
      assertEquals(WasmValue[].class, method.getReturnType(), "Should return WasmValue[]");
    }

    @Test
    @DisplayName("adaptReturn should have correct signature")
    void adaptReturnShouldHaveCorrectSignature() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.InterfaceAdapter.class.getMethod(
              "adaptReturn", String.class, WasmValue.class);
      assertNotNull(method, "adaptReturn method should exist");
      assertEquals(WasmValue.class, method.getReturnType(), "Should return WasmValue");
    }

    @Test
    @DisplayName("getStatistics should return AdaptationStatistics")
    void getStatisticsShouldReturnAdaptationStatistics() throws NoSuchMethodException {
      Method method = WitInterfaceEvolution.InterfaceAdapter.class.getMethod("getStatistics");
      assertEquals(
          WitInterfaceEvolution.AdaptationStatistics.class,
          method.getReturnType(),
          "Should return AdaptationStatistics");
    }
  }

  @Nested
  @DisplayName("InterfaceMigrationPlan Interface Tests")
  class InterfaceMigrationPlanTests {

    @Test
    @DisplayName("InterfaceMigrationPlan should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceEvolution.InterfaceMigrationPlan.class;
      assertTrue(nestedClass.isInterface(), "InterfaceMigrationPlan should be an interface");
    }

    @Test
    @DisplayName("getId should return String")
    void getIdShouldReturnString() throws NoSuchMethodException {
      Method method = WitInterfaceEvolution.InterfaceMigrationPlan.class.getMethod("getId");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("getEstimatedDuration should return Duration")
    void getEstimatedDurationShouldReturnDuration() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.InterfaceMigrationPlan.class.getMethod("getEstimatedDuration");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("getSteps should return List")
    void getStepsShouldReturnList() throws NoSuchMethodException {
      Method method = WitInterfaceEvolution.InterfaceMigrationPlan.class.getMethod("getSteps");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("getRollbackSteps should return List")
    void getRollbackStepsShouldReturnList() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.InterfaceMigrationPlan.class.getMethod("getRollbackSteps");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("MigrationExecutionResult Interface Tests")
  class MigrationExecutionResultTests {

    @Test
    @DisplayName("MigrationExecutionResult should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceEvolution.MigrationExecutionResult.class;
      assertTrue(nestedClass.isInterface(), "MigrationExecutionResult should be an interface");
    }

    @Test
    @DisplayName("isSuccessful should return boolean")
    void isSuccessfulShouldReturnBoolean() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.MigrationExecutionResult.class.getMethod("isSuccessful");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("getStartTime should return Instant")
    void getStartTimeShouldReturnInstant() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.MigrationExecutionResult.class.getMethod("getStartTime");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("getActualDuration should return Duration")
    void getActualDurationShouldReturnDuration() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.MigrationExecutionResult.class.getMethod("getActualDuration");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("getError should return Optional")
    void getErrorShouldReturnOptional() throws NoSuchMethodException {
      Method method = WitInterfaceEvolution.MigrationExecutionResult.class.getMethod("getError");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("getMetrics should return Map")
    void getMetricsShouldReturnMap() throws NoSuchMethodException {
      Method method = WitInterfaceEvolution.MigrationExecutionResult.class.getMethod("getMetrics");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("InterfaceEvolutionHistory Interface Tests")
  class InterfaceEvolutionHistoryTests {

    @Test
    @DisplayName("InterfaceEvolutionHistory should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceEvolution.InterfaceEvolutionHistory.class;
      assertTrue(nestedClass.isInterface(), "InterfaceEvolutionHistory should be an interface");
    }

    @Test
    @DisplayName("getInterfaceName should return String")
    void getInterfaceNameShouldReturnString() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.InterfaceEvolutionHistory.class.getMethod("getInterfaceName");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("getVersionHistory should return List")
    void getVersionHistoryShouldReturnList() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.InterfaceEvolutionHistory.class.getMethod("getVersionHistory");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("BreakingChange Interface Tests")
  class BreakingChangeTests {

    @Test
    @DisplayName("BreakingChange should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceEvolution.BreakingChange.class;
      assertTrue(nestedClass.isInterface(), "BreakingChange should be an interface");
    }

    @Test
    @DisplayName("BreakingChange should have all required methods")
    void shouldHaveAllRequiredMethods() throws NoSuchMethodException {
      assertNotNull(
          WitInterfaceEvolution.BreakingChange.class.getMethod("getDescription"),
          "Should have getDescription");
      assertNotNull(
          WitInterfaceEvolution.BreakingChange.class.getMethod("getType"), "Should have getType");
      assertNotNull(
          WitInterfaceEvolution.BreakingChange.class.getMethod("getLocation"),
          "Should have getLocation");
      assertNotNull(
          WitInterfaceEvolution.BreakingChange.class.getMethod("getImpact"),
          "Should have getImpact");
      assertNotNull(
          WitInterfaceEvolution.BreakingChange.class.getMethod("getSuggestions"),
          "Should have getSuggestions");
    }
  }

  @Nested
  @DisplayName("MigrationStep Interface Tests")
  class MigrationStepTests {

    @Test
    @DisplayName("MigrationStep should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceEvolution.MigrationStep.class;
      assertTrue(nestedClass.isInterface(), "MigrationStep should be an interface");
    }

    @Test
    @DisplayName("MigrationStep should have all required methods")
    void shouldHaveAllRequiredMethods() throws NoSuchMethodException {
      assertNotNull(
          WitInterfaceEvolution.MigrationStep.class.getMethod("getId"), "Should have getId");
      assertNotNull(
          WitInterfaceEvolution.MigrationStep.class.getMethod("getDescription"),
          "Should have getDescription");
      assertNotNull(
          WitInterfaceEvolution.MigrationStep.class.getMethod("getType"), "Should have getType");
      assertNotNull(
          WitInterfaceEvolution.MigrationStep.class.getMethod("getEstimatedDuration"),
          "Should have getEstimatedDuration");
      assertNotNull(
          WitInterfaceEvolution.MigrationStep.class.getMethod("isRollbackable"),
          "Should have isRollbackable");
    }

    @Test
    @DisplayName("getType should return MigrationStepType")
    void getTypeShouldReturnMigrationStepType() throws NoSuchMethodException {
      Method method = WitInterfaceEvolution.MigrationStep.class.getMethod("getType");
      assertEquals(
          WitInterfaceEvolution.MigrationStepType.class,
          method.getReturnType(),
          "Should return MigrationStepType");
    }
  }

  @Nested
  @DisplayName("MigrationRisk Interface Tests")
  class MigrationRiskTests {

    @Test
    @DisplayName("MigrationRisk should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceEvolution.MigrationRisk.class;
      assertTrue(nestedClass.isInterface(), "MigrationRisk should be an interface");
    }

    @Test
    @DisplayName("getProbability should return double")
    void getProbabilityShouldReturnDouble() throws NoSuchMethodException {
      Method method = WitInterfaceEvolution.MigrationRisk.class.getMethod("getProbability");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("getLevel should return RiskLevel")
    void getLevelShouldReturnRiskLevel() throws NoSuchMethodException {
      Method method = WitInterfaceEvolution.MigrationRisk.class.getMethod("getLevel");
      assertEquals(
          WitInterfaceEvolution.RiskLevel.class, method.getReturnType(), "Should return RiskLevel");
    }
  }

  @Nested
  @DisplayName("AdaptationStatistics Interface Tests")
  class AdaptationStatisticsTests {

    @Test
    @DisplayName("AdaptationStatistics should be a nested interface")
    void shouldBeNestedInterface() {
      Class<?> nestedClass = WitInterfaceEvolution.AdaptationStatistics.class;
      assertTrue(nestedClass.isInterface(), "AdaptationStatistics should be an interface");
    }

    @Test
    @DisplayName("getTotalAdaptations should return long")
    void getTotalAdaptationsShouldReturnLong() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.AdaptationStatistics.class.getMethod("getTotalAdaptations");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("getAverageAdaptationTime should return double")
    void getAverageAdaptationTimeShouldReturnDouble() throws NoSuchMethodException {
      Method method =
          WitInterfaceEvolution.AdaptationStatistics.class.getMethod("getAverageAdaptationTime");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  // ========================================================================
  // Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("EvolutionType Enum Tests")
  class EvolutionTypeEnumTests {

    @Test
    @DisplayName("EvolutionType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceEvolution.EvolutionType.class.isEnum(), "EvolutionType should be an enum");
    }

    @Test
    @DisplayName("EvolutionType should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceEvolution.EvolutionType[] values = WitInterfaceEvolution.EvolutionType.values();
      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceEvolution.EvolutionType v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("MAJOR"), "Should have MAJOR");
      assertTrue(valueNames.contains("MINOR"), "Should have MINOR");
      assertTrue(valueNames.contains("PATCH"), "Should have PATCH");
      assertTrue(valueNames.contains("CUSTOM"), "Should have CUSTOM");
      assertEquals(4, values.length, "Should have exactly 4 values");
    }
  }

  @Nested
  @DisplayName("MigrationComplexity Enum Tests")
  class MigrationComplexityEnumTests {

    @Test
    @DisplayName("MigrationComplexity should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceEvolution.MigrationComplexity.class.isEnum(),
          "MigrationComplexity should be an enum");
    }

    @Test
    @DisplayName("MigrationComplexity should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceEvolution.MigrationComplexity[] values =
          WitInterfaceEvolution.MigrationComplexity.values();
      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceEvolution.MigrationComplexity v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("SIMPLE"), "Should have SIMPLE");
      assertTrue(valueNames.contains("MODERATE"), "Should have MODERATE");
      assertTrue(valueNames.contains("COMPLEX"), "Should have COMPLEX");
      assertTrue(valueNames.contains("CRITICAL"), "Should have CRITICAL");
      assertEquals(4, values.length, "Should have exactly 4 values");
    }
  }

  @Nested
  @DisplayName("MigrationEffort Enum Tests")
  class MigrationEffortEnumTests {

    @Test
    @DisplayName("MigrationEffort should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceEvolution.MigrationEffort.class.isEnum(),
          "MigrationEffort should be an enum");
    }

    @Test
    @DisplayName("MigrationEffort should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceEvolution.MigrationEffort[] values =
          WitInterfaceEvolution.MigrationEffort.values();
      assertEquals(4, values.length, "Should have exactly 4 values");

      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceEvolution.MigrationEffort v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("LOW"), "Should have LOW");
      assertTrue(valueNames.contains("MEDIUM"), "Should have MEDIUM");
      assertTrue(valueNames.contains("HIGH"), "Should have HIGH");
      assertTrue(valueNames.contains("VERY_HIGH"), "Should have VERY_HIGH");
    }
  }

  @Nested
  @DisplayName("CompatibilityLevel Enum Tests")
  class CompatibilityLevelEnumTests {

    @Test
    @DisplayName("CompatibilityLevel should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceEvolution.CompatibilityLevel.class.isEnum(),
          "CompatibilityLevel should be an enum");
    }

    @Test
    @DisplayName("CompatibilityLevel should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceEvolution.CompatibilityLevel[] values =
          WitInterfaceEvolution.CompatibilityLevel.values();
      assertEquals(4, values.length, "Should have exactly 4 values");

      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceEvolution.CompatibilityLevel v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("FULL"), "Should have FULL");
      assertTrue(valueNames.contains("PARTIAL"), "Should have PARTIAL");
      assertTrue(valueNames.contains("LIMITED"), "Should have LIMITED");
      assertTrue(valueNames.contains("NONE"), "Should have NONE");
    }
  }

  @Nested
  @DisplayName("ChangeType Enum Tests")
  class ChangeTypeEnumTests {

    @Test
    @DisplayName("ChangeType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WitInterfaceEvolution.ChangeType.class.isEnum(), "ChangeType should be an enum");
    }

    @Test
    @DisplayName("ChangeType should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceEvolution.ChangeType[] values = WitInterfaceEvolution.ChangeType.values();
      assertEquals(4, values.length, "Should have exactly 4 values");

      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceEvolution.ChangeType v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("ADDITION"), "Should have ADDITION");
      assertTrue(valueNames.contains("REMOVAL"), "Should have REMOVAL");
      assertTrue(valueNames.contains("MODIFICATION"), "Should have MODIFICATION");
      assertTrue(valueNames.contains("RENAME"), "Should have RENAME");
    }
  }

  @Nested
  @DisplayName("ChangeImpact Enum Tests")
  class ChangeImpactEnumTests {

    @Test
    @DisplayName("ChangeImpact should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceEvolution.ChangeImpact.class.isEnum(), "ChangeImpact should be an enum");
    }

    @Test
    @DisplayName("ChangeImpact should have LOW, MEDIUM, HIGH, CRITICAL")
    void shouldHaveAllExpectedValues() {
      WitInterfaceEvolution.ChangeImpact[] values = WitInterfaceEvolution.ChangeImpact.values();
      assertEquals(4, values.length, "Should have exactly 4 values");
    }
  }

  @Nested
  @DisplayName("AdaptationType Enum Tests")
  class AdaptationTypeEnumTests {

    @Test
    @DisplayName("AdaptationType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceEvolution.AdaptationType.class.isEnum(), "AdaptationType should be an enum");
    }

    @Test
    @DisplayName("AdaptationType should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceEvolution.AdaptationType[] values = WitInterfaceEvolution.AdaptationType.values();
      assertEquals(4, values.length, "Should have exactly 4 values");

      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceEvolution.AdaptationType v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("TYPE_CONVERSION"), "Should have TYPE_CONVERSION");
      assertTrue(valueNames.contains("PARAMETER_MAPPING"), "Should have PARAMETER_MAPPING");
      assertTrue(valueNames.contains("RETURN_TRANSFORMATION"), "Should have RETURN_TRANSFORMATION");
      assertTrue(valueNames.contains("INTERFACE_BRIDGING"), "Should have INTERFACE_BRIDGING");
    }
  }

  @Nested
  @DisplayName("RiskLevel Enum Tests")
  class RiskLevelEnumTests {

    @Test
    @DisplayName("RiskLevel should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WitInterfaceEvolution.RiskLevel.class.isEnum(), "RiskLevel should be an enum");
    }

    @Test
    @DisplayName("RiskLevel should have LOW, MEDIUM, HIGH, CRITICAL")
    void shouldHaveAllExpectedValues() {
      WitInterfaceEvolution.RiskLevel[] values = WitInterfaceEvolution.RiskLevel.values();
      assertEquals(4, values.length, "Should have exactly 4 values");
    }
  }

  @Nested
  @DisplayName("MigrationStepType Enum Tests")
  class MigrationStepTypeEnumTests {

    @Test
    @DisplayName("MigrationStepType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(
          WitInterfaceEvolution.MigrationStepType.class.isEnum(),
          "MigrationStepType should be an enum");
    }

    @Test
    @DisplayName("MigrationStepType should have all expected values")
    void shouldHaveAllExpectedValues() {
      WitInterfaceEvolution.MigrationStepType[] values =
          WitInterfaceEvolution.MigrationStepType.values();
      assertEquals(4, values.length, "Should have exactly 4 values");

      Set<String> valueNames = new HashSet<>();
      for (WitInterfaceEvolution.MigrationStepType v : values) {
        valueNames.add(v.name());
      }

      assertTrue(valueNames.contains("PREPARATION"), "Should have PREPARATION");
      assertTrue(valueNames.contains("TRANSFORMATION"), "Should have TRANSFORMATION");
      assertTrue(valueNames.contains("VALIDATION"), "Should have VALIDATION");
      assertTrue(valueNames.contains("CLEANUP"), "Should have CLEANUP");
    }
  }

  // ========================================================================
  // Nested Interface Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Interface and Enum Count Tests")
  class NestedInterfaceCountTests {

    @Test
    @DisplayName("WitInterfaceEvolution should have expected number of nested types")
    void shouldHaveExpectedNestedTypes() {
      Class<?>[] declaredClasses = WitInterfaceEvolution.class.getDeclaredClasses();

      int interfaceCount = 0;
      int enumCount = 0;
      for (Class<?> clazz : declaredClasses) {
        if (clazz.isInterface()) {
          interfaceCount++;
        } else if (clazz.isEnum()) {
          enumCount++;
        }
      }

      // Verify we have a substantial number of nested interfaces
      assertTrue(interfaceCount >= 10, "Should have at least 10 nested interfaces");

      // Verify we have a substantial number of enums
      assertTrue(enumCount >= 10, "Should have at least 10 enums");
    }
  }
}
