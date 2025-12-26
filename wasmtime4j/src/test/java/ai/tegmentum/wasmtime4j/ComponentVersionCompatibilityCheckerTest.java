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

import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.BreakingChange;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.BreakingChangeAnalysisResult;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.BreakingChangeType;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.CompatibilityLevel;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.CompatibilityRule;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.CompatibleChange;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.CompatibleChangeType;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.ComponentCompatibilityMatrix;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.ComponentVersionCompatibilityResult;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.ComponentVersionRequirement;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.InterfaceCompatibilityResult;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.PatternType;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.RequirementType;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.Severity;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.UpgradePathValidationResult;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.VersionChangeType;
import ai.tegmentum.wasmtime4j.ComponentVersionCompatibilityChecker.VersionPattern;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentVersionCompatibilityChecker} interface.
 *
 * <p>ComponentVersionCompatibilityChecker provides comprehensive version compatibility validation
 * for WebAssembly components.
 */
@DisplayName("ComponentVersionCompatibilityChecker Tests")
class ComponentVersionCompatibilityCheckerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentVersionCompatibilityChecker.class.getModifiers()),
          "ComponentVersionCompatibilityChecker should be public");
      assertTrue(
          ComponentVersionCompatibilityChecker.class.isInterface(),
          "ComponentVersionCompatibilityChecker should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have checkCompatibility method")
    void shouldHaveCheckCompatibilityMethod() throws NoSuchMethodException {
      final Method method =
          ComponentVersionCompatibilityChecker.class.getMethod(
              "checkCompatibility", ComponentVersion.class, ComponentVersion.class);
      assertNotNull(method, "checkCompatibility method should exist");
      assertEquals(
          ComponentVersionCompatibilityResult.class,
          method.getReturnType(),
          "Should return ComponentVersionCompatibilityResult");
    }

    @Test
    @DisplayName("should have checkDependencyCompatibility method")
    void shouldHaveCheckDependencyCompatibilityMethod() throws NoSuchMethodException {
      final Method method =
          ComponentVersionCompatibilityChecker.class.getMethod(
              "checkDependencyCompatibility", ComponentVersion.class, Map.class);
      assertNotNull(method, "checkDependencyCompatibility method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have findBestCompatibleVersion method")
    void shouldHaveFindBestCompatibleVersionMethod() throws NoSuchMethodException {
      final Method method =
          ComponentVersionCompatibilityChecker.class.getMethod(
              "findBestCompatibleVersion", ComponentVersionRequirement.class, Set.class);
      assertNotNull(method, "findBestCompatibleVersion method should exist");
      assertEquals(
          ComponentVersion.class, method.getReturnType(), "Should return ComponentVersion");
    }

    @Test
    @DisplayName("should have checkInterfaceCompatibility method")
    void shouldHaveCheckInterfaceCompatibilityMethod() throws NoSuchMethodException {
      final Method method =
          ComponentVersionCompatibilityChecker.class.getMethod(
              "checkInterfaceCompatibility", Set.class, Set.class);
      assertNotNull(method, "checkInterfaceCompatibility method should exist");
      assertEquals(
          InterfaceCompatibilityResult.class,
          method.getReturnType(),
          "Should return InterfaceCompatibilityResult");
    }

    @Test
    @DisplayName("should have analyzeBreakingChanges method")
    void shouldHaveAnalyzeBreakingChangesMethod() throws NoSuchMethodException {
      final Method method =
          ComponentVersionCompatibilityChecker.class.getMethod(
              "analyzeBreakingChanges", ComponentVersion.class, ComponentVersion.class);
      assertNotNull(method, "analyzeBreakingChanges method should exist");
      assertEquals(
          BreakingChangeAnalysisResult.class,
          method.getReturnType(),
          "Should return BreakingChangeAnalysisResult");
    }

    @Test
    @DisplayName("should have createCompatibilityMatrix method")
    void shouldHaveCreateCompatibilityMatrixMethod() throws NoSuchMethodException {
      final Method method =
          ComponentVersionCompatibilityChecker.class.getMethod(
              "createCompatibilityMatrix", Set.class);
      assertNotNull(method, "createCompatibilityMatrix method should exist");
      assertEquals(
          ComponentCompatibilityMatrix.class,
          method.getReturnType(),
          "Should return ComponentCompatibilityMatrix");
    }

    @Test
    @DisplayName("should have validateUpgradePath method")
    void shouldHaveValidateUpgradePathMethod() throws NoSuchMethodException {
      final Method method =
          ComponentVersionCompatibilityChecker.class.getMethod(
              "validateUpgradePath", ComponentVersion.class, ComponentVersion.class);
      assertNotNull(method, "validateUpgradePath method should exist");
      assertEquals(
          UpgradePathValidationResult.class,
          method.getReturnType(),
          "Should return UpgradePathValidationResult");
    }

    @Test
    @DisplayName("should have getCompatibilityRules method")
    void shouldHaveGetCompatibilityRulesMethod() throws NoSuchMethodException {
      final Method method =
          ComponentVersionCompatibilityChecker.class.getMethod(
              "getCompatibilityRules", String.class);
      assertNotNull(method, "getCompatibilityRules method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have addCompatibilityRule method")
    void shouldHaveAddCompatibilityRuleMethod() throws NoSuchMethodException {
      final Method method =
          ComponentVersionCompatibilityChecker.class.getMethod(
              "addCompatibilityRule", CompatibilityRule.class);
      assertNotNull(method, "addCompatibilityRule method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have removeCompatibilityRule method")
    void shouldHaveRemoveCompatibilityRuleMethod() throws NoSuchMethodException {
      final Method method =
          ComponentVersionCompatibilityChecker.class.getMethod(
              "removeCompatibilityRule", String.class);
      assertNotNull(method, "removeCompatibilityRule method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("CompatibilityLevel Enum Tests")
  class CompatibilityLevelEnumTests {

    @Test
    @DisplayName("should have all compatibility levels")
    void shouldHaveAllCompatibilityLevels() {
      final CompatibilityLevel[] levels = CompatibilityLevel.values();
      assertEquals(4, levels.length, "Should have 4 compatibility levels");
    }

    @Test
    @DisplayName("should have FULLY_COMPATIBLE level")
    void shouldHaveFullyCompatibleLevel() {
      assertEquals(
          CompatibilityLevel.FULLY_COMPATIBLE, CompatibilityLevel.valueOf("FULLY_COMPATIBLE"));
    }

    @Test
    @DisplayName("should have BACKWARD_COMPATIBLE level")
    void shouldHaveBackwardCompatibleLevel() {
      assertEquals(
          CompatibilityLevel.BACKWARD_COMPATIBLE,
          CompatibilityLevel.valueOf("BACKWARD_COMPATIBLE"));
    }

    @Test
    @DisplayName("should have FORWARD_COMPATIBLE level")
    void shouldHaveForwardCompatibleLevel() {
      assertEquals(
          CompatibilityLevel.FORWARD_COMPATIBLE, CompatibilityLevel.valueOf("FORWARD_COMPATIBLE"));
    }

    @Test
    @DisplayName("should have INCOMPATIBLE level")
    void shouldHaveIncompatibleLevel() {
      assertEquals(CompatibilityLevel.INCOMPATIBLE, CompatibilityLevel.valueOf("INCOMPATIBLE"));
    }
  }

  @Nested
  @DisplayName("VersionChangeType Enum Tests")
  class VersionChangeTypeEnumTests {

    @Test
    @DisplayName("should have all version change types")
    void shouldHaveAllVersionChangeTypes() {
      final VersionChangeType[] types = VersionChangeType.values();
      assertEquals(5, types.length, "Should have 5 version change types");
    }

    @Test
    @DisplayName("should have MAJOR type")
    void shouldHaveMajorType() {
      assertEquals(VersionChangeType.MAJOR, VersionChangeType.valueOf("MAJOR"));
    }

    @Test
    @DisplayName("should have MINOR type")
    void shouldHaveMinorType() {
      assertEquals(VersionChangeType.MINOR, VersionChangeType.valueOf("MINOR"));
    }

    @Test
    @DisplayName("should have PATCH type")
    void shouldHavePatchType() {
      assertEquals(VersionChangeType.PATCH, VersionChangeType.valueOf("PATCH"));
    }

    @Test
    @DisplayName("should have PRERELEASE type")
    void shouldHavePrereleaseType() {
      assertEquals(VersionChangeType.PRERELEASE, VersionChangeType.valueOf("PRERELEASE"));
    }

    @Test
    @DisplayName("should have BUILD type")
    void shouldHaveBuildType() {
      assertEquals(VersionChangeType.BUILD, VersionChangeType.valueOf("BUILD"));
    }
  }

  @Nested
  @DisplayName("BreakingChangeType Enum Tests")
  class BreakingChangeTypeEnumTests {

    @Test
    @DisplayName("should have all breaking change types")
    void shouldHaveAllBreakingChangeTypes() {
      final BreakingChangeType[] types = BreakingChangeType.values();
      assertEquals(6, types.length, "Should have 6 breaking change types");
    }

    @Test
    @DisplayName("should have FUNCTION_REMOVED type")
    void shouldHaveFunctionRemovedType() {
      assertEquals(
          BreakingChangeType.FUNCTION_REMOVED, BreakingChangeType.valueOf("FUNCTION_REMOVED"));
    }

    @Test
    @DisplayName("should have FUNCTION_SIGNATURE_CHANGED type")
    void shouldHaveFunctionSignatureChangedType() {
      assertEquals(
          BreakingChangeType.FUNCTION_SIGNATURE_CHANGED,
          BreakingChangeType.valueOf("FUNCTION_SIGNATURE_CHANGED"));
    }

    @Test
    @DisplayName("should have TYPE_REMOVED type")
    void shouldHaveTypeRemovedType() {
      assertEquals(BreakingChangeType.TYPE_REMOVED, BreakingChangeType.valueOf("TYPE_REMOVED"));
    }

    @Test
    @DisplayName("should have TYPE_DEFINITION_CHANGED type")
    void shouldHaveTypeDefinitionChangedType() {
      assertEquals(
          BreakingChangeType.TYPE_DEFINITION_CHANGED,
          BreakingChangeType.valueOf("TYPE_DEFINITION_CHANGED"));
    }

    @Test
    @DisplayName("should have INTERFACE_REMOVED type")
    void shouldHaveInterfaceRemovedType() {
      assertEquals(
          BreakingChangeType.INTERFACE_REMOVED, BreakingChangeType.valueOf("INTERFACE_REMOVED"));
    }

    @Test
    @DisplayName("should have INTERFACE_VERSION_INCOMPATIBLE type")
    void shouldHaveInterfaceVersionIncompatibleType() {
      assertEquals(
          BreakingChangeType.INTERFACE_VERSION_INCOMPATIBLE,
          BreakingChangeType.valueOf("INTERFACE_VERSION_INCOMPATIBLE"));
    }
  }

  @Nested
  @DisplayName("CompatibleChangeType Enum Tests")
  class CompatibleChangeTypeEnumTests {

    @Test
    @DisplayName("should have all compatible change types")
    void shouldHaveAllCompatibleChangeTypes() {
      final CompatibleChangeType[] types = CompatibleChangeType.values();
      assertEquals(5, types.length, "Should have 5 compatible change types");
    }

    @Test
    @DisplayName("should have FUNCTION_ADDED type")
    void shouldHaveFunctionAddedType() {
      assertEquals(
          CompatibleChangeType.FUNCTION_ADDED, CompatibleChangeType.valueOf("FUNCTION_ADDED"));
    }

    @Test
    @DisplayName("should have TYPE_ADDED type")
    void shouldHaveTypeAddedType() {
      assertEquals(CompatibleChangeType.TYPE_ADDED, CompatibleChangeType.valueOf("TYPE_ADDED"));
    }

    @Test
    @DisplayName("should have INTERFACE_ADDED type")
    void shouldHaveInterfaceAddedType() {
      assertEquals(
          CompatibleChangeType.INTERFACE_ADDED, CompatibleChangeType.valueOf("INTERFACE_ADDED"));
    }

    @Test
    @DisplayName("should have PARAMETER_ADDED_WITH_DEFAULT type")
    void shouldHaveParameterAddedWithDefaultType() {
      assertEquals(
          CompatibleChangeType.PARAMETER_ADDED_WITH_DEFAULT,
          CompatibleChangeType.valueOf("PARAMETER_ADDED_WITH_DEFAULT"));
    }

    @Test
    @DisplayName("should have DOCUMENTATION_UPDATED type")
    void shouldHaveDocumentationUpdatedType() {
      assertEquals(
          CompatibleChangeType.DOCUMENTATION_UPDATED,
          CompatibleChangeType.valueOf("DOCUMENTATION_UPDATED"));
    }
  }

  @Nested
  @DisplayName("Severity Enum Tests")
  class SeverityEnumTests {

    @Test
    @DisplayName("should have all severity levels")
    void shouldHaveAllSeverityLevels() {
      final Severity[] severities = Severity.values();
      assertEquals(4, severities.length, "Should have 4 severity levels");
    }

    @Test
    @DisplayName("should have LOW severity")
    void shouldHaveLowSeverity() {
      assertEquals(Severity.LOW, Severity.valueOf("LOW"));
    }

    @Test
    @DisplayName("should have MEDIUM severity")
    void shouldHaveMediumSeverity() {
      assertEquals(Severity.MEDIUM, Severity.valueOf("MEDIUM"));
    }

    @Test
    @DisplayName("should have HIGH severity")
    void shouldHaveHighSeverity() {
      assertEquals(Severity.HIGH, Severity.valueOf("HIGH"));
    }

    @Test
    @DisplayName("should have CRITICAL severity")
    void shouldHaveCriticalSeverity() {
      assertEquals(Severity.CRITICAL, Severity.valueOf("CRITICAL"));
    }
  }

  @Nested
  @DisplayName("PatternType Enum Tests")
  class PatternTypeEnumTests {

    @Test
    @DisplayName("should have all pattern types")
    void shouldHaveAllPatternTypes() {
      final PatternType[] types = PatternType.values();
      assertEquals(3, types.length, "Should have 3 pattern types");
    }

    @Test
    @DisplayName("should have EXACT type")
    void shouldHaveExactType() {
      assertEquals(PatternType.EXACT, PatternType.valueOf("EXACT"));
    }

    @Test
    @DisplayName("should have SEMANTIC_RANGE type")
    void shouldHaveSemanticRangeType() {
      assertEquals(PatternType.SEMANTIC_RANGE, PatternType.valueOf("SEMANTIC_RANGE"));
    }

    @Test
    @DisplayName("should have REGEX type")
    void shouldHaveRegexType() {
      assertEquals(PatternType.REGEX, PatternType.valueOf("REGEX"));
    }
  }

  @Nested
  @DisplayName("RequirementType Enum Tests")
  class RequirementTypeEnumTests {

    @Test
    @DisplayName("should have all requirement types")
    void shouldHaveAllRequirementTypes() {
      final RequirementType[] types = RequirementType.values();
      assertEquals(6, types.length, "Should have 6 requirement types");
    }

    @Test
    @DisplayName("should have EXACT type")
    void shouldHaveExactRequirement() {
      assertEquals(RequirementType.EXACT, RequirementType.valueOf("EXACT"));
    }

    @Test
    @DisplayName("should have MINIMUM type")
    void shouldHaveMinimumRequirement() {
      assertEquals(RequirementType.MINIMUM, RequirementType.valueOf("MINIMUM"));
    }

    @Test
    @DisplayName("should have MAXIMUM type")
    void shouldHaveMaximumRequirement() {
      assertEquals(RequirementType.MAXIMUM, RequirementType.valueOf("MAXIMUM"));
    }

    @Test
    @DisplayName("should have RANGE type")
    void shouldHaveRangeRequirement() {
      assertEquals(RequirementType.RANGE, RequirementType.valueOf("RANGE"));
    }

    @Test
    @DisplayName("should have COMPATIBLE type")
    void shouldHaveCompatibleRequirement() {
      assertEquals(RequirementType.COMPATIBLE, RequirementType.valueOf("COMPATIBLE"));
    }

    @Test
    @DisplayName("should have LATEST type")
    void shouldHaveLatestRequirement() {
      assertEquals(RequirementType.LATEST, RequirementType.valueOf("LATEST"));
    }
  }

  @Nested
  @DisplayName("Nested Class Structure Tests")
  class NestedClassStructureTests {

    @Test
    @DisplayName("should have ComponentVersionCompatibilityResult class")
    void shouldHaveCompatibilityResultClass() {
      final ComponentVersionCompatibilityResult result =
          new ComponentVersionCompatibilityResult(
              true, "Compatible", CompatibilityLevel.FULLY_COMPATIBLE, Set.of(), Set.of());

      assertTrue(result.isCompatible(), "Should be compatible");
      assertEquals("Compatible", result.getMessage(), "Message should match");
      assertEquals(CompatibilityLevel.FULLY_COMPATIBLE, result.getLevel(), "Level should match");
      assertNotNull(result.getIssues(), "Issues should not be null");
      assertNotNull(result.getWarnings(), "Warnings should not be null");
    }

    @Test
    @DisplayName("should have InterfaceCompatibilityResult class")
    void shouldHaveInterfaceCompatibilityResultClass() {
      final InterfaceCompatibilityResult result =
          new InterfaceCompatibilityResult(true, Map.of(), Set.of(), Set.of());

      assertTrue(result.isCompatible(), "Should be compatible");
      assertNotNull(result.getInterfaceResults(), "Interface results should not be null");
      assertNotNull(result.getMissingInterfaces(), "Missing interfaces should not be null");
      assertNotNull(result.getExtraInterfaces(), "Extra interfaces should not be null");
    }

    @Test
    @DisplayName("should have BreakingChangeAnalysisResult class")
    void shouldHaveBreakingChangeAnalysisResultClass() {
      final BreakingChangeAnalysisResult result =
          new BreakingChangeAnalysisResult(false, List.of(), List.of(), VersionChangeType.MINOR);

      assertTrue(!result.hasBreakingChanges(), "Should not have breaking changes");
      assertNotNull(result.getBreakingChanges(), "Breaking changes should not be null");
      assertNotNull(result.getCompatibleChanges(), "Compatible changes should not be null");
      assertEquals(VersionChangeType.MINOR, result.getChangeType(), "Change type should match");
    }

    @Test
    @DisplayName("should have BreakingChange class")
    void shouldHaveBreakingChangeClass() {
      final BreakingChange change =
          new BreakingChange(
              BreakingChangeType.FUNCTION_REMOVED,
              "Function removed",
              "someFunction",
              Severity.HIGH);

      assertEquals(BreakingChangeType.FUNCTION_REMOVED, change.getType(), "Type should match");
      assertEquals("Function removed", change.getDescription(), "Description should match");
      assertEquals("someFunction", change.getAffectedElement(), "Affected element should match");
      assertEquals(Severity.HIGH, change.getSeverity(), "Severity should match");
    }

    @Test
    @DisplayName("should have CompatibleChange class")
    void shouldHaveCompatibleChangeClass() {
      final CompatibleChange change =
          new CompatibleChange(
              CompatibleChangeType.FUNCTION_ADDED, "Function added", "newFunction");

      assertEquals(CompatibleChangeType.FUNCTION_ADDED, change.getType(), "Type should match");
      assertEquals("Function added", change.getDescription(), "Description should match");
      assertEquals("newFunction", change.getAffectedElement(), "Affected element should match");
    }

    @Test
    @DisplayName("should have UpgradePathValidationResult class")
    void shouldHaveUpgradePathValidationResultClass() {
      final UpgradePathValidationResult result =
          new UpgradePathValidationResult(true, List.of(), Set.of(), Set.of());

      assertTrue(result.isValid(), "Should be valid");
      assertNotNull(result.getIntermediatePath(), "Intermediate path should not be null");
      assertNotNull(result.getBlockers(), "Blockers should not be null");
      assertNotNull(result.getWarnings(), "Warnings should not be null");
    }

    @Test
    @DisplayName("should have VersionPattern class")
    void shouldHaveVersionPatternClass() {
      final VersionPattern pattern = new VersionPattern("1.0.0", PatternType.EXACT);

      assertEquals("1.0.0", pattern.getPattern(), "Pattern should match");
      assertEquals(PatternType.EXACT, pattern.getType(), "Type should match");
    }

    @Test
    @DisplayName("should have ComponentVersionRequirement class")
    void shouldHaveComponentVersionRequirementClass() {
      final ComponentVersionRequirement requirement =
          new ComponentVersionRequirement(">=1.0.0", RequirementType.MINIMUM);

      assertEquals(">=1.0.0", requirement.getRequirement(), "Requirement should match");
      assertEquals(RequirementType.MINIMUM, requirement.getType(), "Type should match");
    }

    @Test
    @DisplayName("should have ComponentCompatibilityMatrix class")
    void shouldHaveComponentCompatibilityMatrixClass() {
      final ComponentCompatibilityMatrix matrix =
          new ComponentCompatibilityMatrix(Map.of(), Set.of());

      assertNotNull(matrix.getMatrix(), "Matrix should not be null");
      assertNotNull(matrix.getComponentIds(), "Component IDs should not be null");
    }

    @Test
    @DisplayName("should have CompatibilityRule class")
    void shouldHaveCompatibilityRuleClass() {
      final VersionPattern sourcePattern = new VersionPattern("1.*", PatternType.SEMANTIC_RANGE);
      final VersionPattern targetPattern = new VersionPattern("2.*", PatternType.SEMANTIC_RANGE);
      final CompatibilityRule rule =
          new CompatibilityRule(
              "rule-1",
              "wasm-component",
              sourcePattern,
              targetPattern,
              CompatibilityLevel.BACKWARD_COMPATIBLE,
              "Test rule");

      assertEquals("rule-1", rule.getId(), "ID should match");
      assertEquals("wasm-component", rule.getComponentType(), "Component type should match");
      assertEquals(sourcePattern, rule.getSourcePattern(), "Source pattern should match");
      assertEquals(targetPattern, rule.getTargetPattern(), "Target pattern should match");
      assertEquals(CompatibilityLevel.BACKWARD_COMPATIBLE, rule.getLevel(), "Level should match");
      assertEquals("Test rule", rule.getDescription(), "Description should match");
    }
  }
}
