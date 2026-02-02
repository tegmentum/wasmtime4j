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

package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.ComponentLinkingException.LinkingFailureType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ComponentLinkingException} class.
 *
 * <p>This test class verifies the construction and behavior of component linking exceptions,
 * including failure types and detailed error reporting.
 */
@DisplayName("ComponentLinkingException Tests")
class ComponentLinkingExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ComponentLinkingException should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(ComponentLinkingException.class),
          "ComponentLinkingException should extend WasmException");
    }

    @Test
    @DisplayName("ComponentLinkingException should be serializable")
    void shouldBeSerializable() {
      assertTrue(
          java.io.Serializable.class.isAssignableFrom(ComponentLinkingException.class),
          "ComponentLinkingException should be serializable");
    }
  }

  @Nested
  @DisplayName("LinkingFailureType Enum Tests")
  class LinkingFailureTypeEnumTests {

    @Test
    @DisplayName("Should have INTERFACE_MISMATCH value")
    void shouldHaveInterfaceMismatchValue() {
      assertNotNull(
          LinkingFailureType.valueOf("INTERFACE_MISMATCH"), "Should have INTERFACE_MISMATCH value");
    }

    @Test
    @DisplayName("Should have MISSING_DEPENDENCIES value")
    void shouldHaveMissingDependenciesValue() {
      assertNotNull(
          LinkingFailureType.valueOf("MISSING_DEPENDENCIES"),
          "Should have MISSING_DEPENDENCIES value");
    }

    @Test
    @DisplayName("Should have CIRCULAR_DEPENDENCY value")
    void shouldHaveCircularDependencyValue() {
      assertNotNull(
          LinkingFailureType.valueOf("CIRCULAR_DEPENDENCY"),
          "Should have CIRCULAR_DEPENDENCY value");
    }

    @Test
    @DisplayName("Should have VERSION_INCOMPATIBILITY value")
    void shouldHaveVersionIncompatibilityValue() {
      assertNotNull(
          LinkingFailureType.valueOf("VERSION_INCOMPATIBILITY"),
          "Should have VERSION_INCOMPATIBILITY value");
    }

    @Test
    @DisplayName("Should have NATIVE_LINKING_ERROR value")
    void shouldHaveNativeLinkingErrorValue() {
      assertNotNull(
          LinkingFailureType.valueOf("NATIVE_LINKING_ERROR"),
          "Should have NATIVE_LINKING_ERROR value");
    }

    @Test
    @DisplayName("Should have INVALID_COMPONENT_STATE value")
    void shouldHaveInvalidComponentStateValue() {
      assertNotNull(
          LinkingFailureType.valueOf("INVALID_COMPONENT_STATE"),
          "Should have INVALID_COMPONENT_STATE value");
    }

    @Test
    @DisplayName("Should have RESOURCE_CONSTRAINTS value")
    void shouldHaveResourceConstraintsValue() {
      assertNotNull(
          LinkingFailureType.valueOf("RESOURCE_CONSTRAINTS"),
          "Should have RESOURCE_CONSTRAINTS value");
    }

    @Test
    @DisplayName("Should have SECURITY_VIOLATION value")
    void shouldHaveSecurityViolationValue() {
      assertNotNull(
          LinkingFailureType.valueOf("SECURITY_VIOLATION"), "Should have SECURITY_VIOLATION value");
    }

    @Test
    @DisplayName("Should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(LinkingFailureType.valueOf("UNKNOWN"), "Should have UNKNOWN value");
    }

    @Test
    @DisplayName("Should have 9 failure types")
    void shouldHave9FailureTypes() {
      assertEquals(9, LinkingFailureType.values().length, "Should have 9 failure types");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor without cause should set all fields")
    void constructorWithoutCauseShouldSetAllFields() {
      final List<String> components = Arrays.asList("component-a", "component-b");
      final Map<String, String> compatibilityIssues = new HashMap<>();
      compatibilityIssues.put("a-b", "Version mismatch");
      final Set<String> missingDeps = new HashSet<>(Arrays.asList("missing-lib"));
      final Set<String> circularDeps = Collections.emptySet();
      final List<String> suggestions = Arrays.asList("Update versions");

      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Linking failed",
              LinkingFailureType.INTERFACE_MISMATCH,
              components,
              compatibilityIssues,
              missingDeps,
              circularDeps,
              suggestions);

      assertEquals("Linking failed", exception.getMessage(), "Message should match");
      assertEquals(
          LinkingFailureType.INTERFACE_MISMATCH,
          exception.getFailureType(),
          "Failure type should be INTERFACE_MISMATCH");
      assertNotNull(exception.getInvolvedComponents(), "Components should not be null");
      assertEquals(2, exception.getInvolvedComponents().size(), "Should have 2 components");
    }

    @Test
    @DisplayName("Constructor with cause should set all fields")
    void constructorWithCauseShouldSetAllFields() {
      final Throwable cause = new RuntimeException("Root cause");

      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error",
              cause,
              LinkingFailureType.MISSING_DEPENDENCIES,
              null,
              null,
              null,
              null,
              null);

      assertSame(cause, exception.getCause(), "Cause should be set");
      assertEquals(
          LinkingFailureType.MISSING_DEPENDENCIES,
          exception.getFailureType(),
          "Failure type should be MISSING_DEPENDENCIES");
    }

    @Test
    @DisplayName("Constructor should handle null collections")
    void constructorShouldHandleNullCollections() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.UNKNOWN, null, null, null, null, null);

      assertNull(exception.getInvolvedComponents(), "Components should be null");
      assertNull(exception.getCompatibilityIssues(), "Compatibility issues should be null");
      assertNull(exception.getMissingDependencies(), "Missing dependencies should be null");
      assertNull(exception.getCircularDependencies(), "Circular dependencies should be null");
      assertNull(exception.getSuggestedResolutions(), "Suggestions should be null");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getFailureType should return failure type")
    void getFailureTypeShouldReturnFailureType() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.CIRCULAR_DEPENDENCY, null, null, null, null, null);

      assertEquals(
          LinkingFailureType.CIRCULAR_DEPENDENCY,
          exception.getFailureType(),
          "getFailureType should return CIRCULAR_DEPENDENCY");
    }

    @Test
    @DisplayName("getInvolvedComponents should return defensive copy")
    void getInvolvedComponentsShouldReturnDefensiveCopy() {
      final List<String> components = Arrays.asList("comp-a");
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.UNKNOWN, components, null, null, null, null);

      final List<String> retrieved = exception.getInvolvedComponents();
      assertNotSame(components, retrieved, "Should return a copy");
    }

    @Test
    @DisplayName("getCompatibilityIssues should return defensive copy")
    void getCompatibilityIssuesShouldReturnDefensiveCopy() {
      final Map<String, String> issues = new HashMap<>();
      issues.put("key", "value");
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.UNKNOWN, null, issues, null, null, null);

      final Map<String, String> retrieved = exception.getCompatibilityIssues();
      assertNotSame(issues, retrieved, "Should return a copy");
    }

    @Test
    @DisplayName("getMissingDependencies should return defensive copy")
    void getMissingDependenciesShouldReturnDefensiveCopy() {
      final Set<String> missing = new HashSet<>(Arrays.asList("dep"));
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.UNKNOWN, null, null, missing, null, null);

      final Set<String> retrieved = exception.getMissingDependencies();
      assertNotSame(missing, retrieved, "Should return a copy");
    }

    @Test
    @DisplayName("getCircularDependencies should return defensive copy")
    void getCircularDependenciesShouldReturnDefensiveCopy() {
      final Set<String> circular = new HashSet<>(Arrays.asList("comp"));
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.UNKNOWN, null, null, null, circular, null);

      final Set<String> retrieved = exception.getCircularDependencies();
      assertNotSame(circular, retrieved, "Should return a copy");
    }

    @Test
    @DisplayName("getSuggestedResolutions should return defensive copy")
    void getSuggestedResolutionsShouldReturnDefensiveCopy() {
      final List<String> suggestions = Arrays.asList("Fix it");
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.UNKNOWN, null, null, null, null, suggestions);

      final List<String> retrieved = exception.getSuggestedResolutions();
      assertNotSame(suggestions, retrieved, "Should return a copy");
    }
  }

  @Nested
  @DisplayName("isRecoverable Tests")
  class IsRecoverableTests {

    @Test
    @DisplayName("MISSING_DEPENDENCIES should be recoverable")
    void missingDependenciesShouldBeRecoverable() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.MISSING_DEPENDENCIES, null, null, null, null, null);

      assertTrue(exception.isRecoverable(), "MISSING_DEPENDENCIES should be recoverable");
    }

    @Test
    @DisplayName("VERSION_INCOMPATIBILITY should be recoverable")
    void versionIncompatibilityShouldBeRecoverable() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.VERSION_INCOMPATIBILITY, null, null, null, null, null);

      assertTrue(exception.isRecoverable(), "VERSION_INCOMPATIBILITY should be recoverable");
    }

    @Test
    @DisplayName("INTERFACE_MISMATCH should be recoverable")
    void interfaceMismatchShouldBeRecoverable() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.INTERFACE_MISMATCH, null, null, null, null, null);

      assertTrue(exception.isRecoverable(), "INTERFACE_MISMATCH should be recoverable");
    }

    @Test
    @DisplayName("CIRCULAR_DEPENDENCY should not be recoverable")
    void circularDependencyShouldNotBeRecoverable() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.CIRCULAR_DEPENDENCY, null, null, null, null, null);

      assertFalse(exception.isRecoverable(), "CIRCULAR_DEPENDENCY should not be recoverable");
    }

    @Test
    @DisplayName("NATIVE_LINKING_ERROR should not be recoverable")
    void nativeLinkingErrorShouldNotBeRecoverable() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.NATIVE_LINKING_ERROR, null, null, null, null, null);

      assertFalse(exception.isRecoverable(), "NATIVE_LINKING_ERROR should not be recoverable");
    }
  }

  @Nested
  @DisplayName("getDetailedErrorReport Tests")
  class GetDetailedErrorReportTests {

    @Test
    @DisplayName("Report should include failure type")
    void reportShouldIncludeFailureType() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error",
              LinkingFailureType.MISSING_DEPENDENCIES,
              Arrays.asList("comp"),
              null,
              null,
              null,
              null);

      final String report = exception.getDetailedErrorReport();

      assertTrue(report.contains("MISSING_DEPENDENCIES"), "Report should contain failure type");
    }

    @Test
    @DisplayName("Report should include error message")
    void reportShouldIncludeErrorMessage() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Custom error message",
              LinkingFailureType.UNKNOWN,
              Arrays.asList("comp"),
              null,
              null,
              null,
              null);

      final String report = exception.getDetailedErrorReport();

      assertTrue(report.contains("Custom error message"), "Report should contain error message");
    }

    @Test
    @DisplayName("Report should include recoverable status")
    void reportShouldIncludeRecoverableStatus() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error",
              LinkingFailureType.MISSING_DEPENDENCIES,
              Arrays.asList("comp"),
              null,
              null,
              null,
              null);

      final String report = exception.getDetailedErrorReport();

      assertTrue(report.contains("Recoverable: Yes"), "Report should contain recoverable status");
    }

    @Test
    @DisplayName("Report should list involved components")
    void reportShouldListInvolvedComponents() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error",
              LinkingFailureType.UNKNOWN,
              Arrays.asList("component-a", "component-b"),
              null,
              null,
              null,
              null);

      final String report = exception.getDetailedErrorReport();

      assertTrue(report.contains("component-a"), "Report should contain first component");
      assertTrue(report.contains("component-b"), "Report should contain second component");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include type and components")
    void toStringShouldIncludeTypeAndComponents() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error message",
              LinkingFailureType.INTERFACE_MISMATCH,
              Arrays.asList("comp-a"),
              null,
              null,
              null,
              null);

      final String result = exception.toString();

      assertTrue(result.contains("INTERFACE_MISMATCH"), "toString should contain failure type");
      assertTrue(result.contains("comp-a"), "toString should contain component");
      assertTrue(result.contains("Error message"), "toString should contain message");
    }
  }

  @Nested
  @DisplayName("isRecoverable Mutation Tests")
  class IsRecoverableMutationTests {

    @Test
    @DisplayName("INVALID_COMPONENT_STATE should not be recoverable")
    void invalidComponentStateShouldNotBeRecoverable() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.INVALID_COMPONENT_STATE, null, null, null, null, null);

      assertFalse(exception.isRecoverable(), "INVALID_COMPONENT_STATE should not be recoverable");
    }

    @Test
    @DisplayName("RESOURCE_CONSTRAINTS should not be recoverable")
    void resourceConstraintsShouldNotBeRecoverable() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.RESOURCE_CONSTRAINTS, null, null, null, null, null);

      assertFalse(exception.isRecoverable(), "RESOURCE_CONSTRAINTS should not be recoverable");
    }

    @Test
    @DisplayName("SECURITY_VIOLATION should not be recoverable")
    void securityViolationShouldNotBeRecoverable() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.SECURITY_VIOLATION, null, null, null, null, null);

      assertFalse(exception.isRecoverable(), "SECURITY_VIOLATION should not be recoverable");
    }

    @Test
    @DisplayName("UNKNOWN should not be recoverable")
    void unknownShouldNotBeRecoverable() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.UNKNOWN, null, null, null, null, null);

      assertFalse(exception.isRecoverable(), "UNKNOWN should not be recoverable");
    }

    @Test
    @DisplayName("Only exactly three failure types should be recoverable")
    void onlyThreeFailureTypesShouldBeRecoverable() {
      int recoverableCount = 0;
      for (LinkingFailureType type : LinkingFailureType.values()) {
        final ComponentLinkingException exception =
            new ComponentLinkingException("Error", type, null, null, null, null, null);
        if (exception.isRecoverable()) {
          recoverableCount++;
        }
      }

      assertEquals(3, recoverableCount, "Exactly 3 failure types should be recoverable");
    }
  }

  @Nested
  @DisplayName("getDetailedErrorReport Empty Collection Tests")
  class GetDetailedErrorReportEmptyCollectionTests {

    @Test
    @DisplayName("Report should not include empty components section")
    void reportShouldNotIncludeEmptyComponentsSection() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error",
              LinkingFailureType.UNKNOWN,
              Collections.emptyList(),
              null,
              null,
              null,
              null);

      final String report = exception.getDetailedErrorReport();

      assertFalse(
          report.contains("Involved Components:"),
          "Report should not contain Involved Components section when empty");
    }

    @Test
    @DisplayName("Report should not include empty compatibility issues section")
    void reportShouldNotIncludeEmptyCompatibilityIssuesSection() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error",
              LinkingFailureType.UNKNOWN,
              null,
              Collections.emptyMap(),
              null,
              null,
              null);

      final String report = exception.getDetailedErrorReport();

      assertFalse(
          report.contains("Compatibility Issues:"),
          "Report should not contain Compatibility Issues section when empty");
    }

    @Test
    @DisplayName("Report should not include empty missing dependencies section")
    void reportShouldNotIncludeEmptyMissingDependenciesSection() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error",
              LinkingFailureType.UNKNOWN,
              null,
              null,
              Collections.emptySet(),
              null,
              null);

      final String report = exception.getDetailedErrorReport();

      assertFalse(
          report.contains("Missing Dependencies:"),
          "Report should not contain Missing Dependencies section when empty");
    }

    @Test
    @DisplayName("Report should not include empty circular dependencies section")
    void reportShouldNotIncludeEmptyCircularDependenciesSection() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error",
              LinkingFailureType.UNKNOWN,
              null,
              null,
              null,
              Collections.emptySet(),
              null);

      final String report = exception.getDetailedErrorReport();

      assertFalse(
          report.contains("Circular Dependencies:"),
          "Report should not contain Circular Dependencies section when empty");
    }

    @Test
    @DisplayName("Report should not include empty suggested resolutions section")
    void reportShouldNotIncludeEmptySuggestedResolutionsSection() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error",
              LinkingFailureType.UNKNOWN,
              null,
              null,
              null,
              null,
              Collections.emptyList());

      final String report = exception.getDetailedErrorReport();

      assertFalse(
          report.contains("Suggested Resolutions:"),
          "Report should not contain Suggested Resolutions section when empty");
    }

    @Test
    @DisplayName("Report should not include null components section")
    void reportShouldNotIncludeNullComponentsSection() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.UNKNOWN, null, null, null, null, null);

      final String report = exception.getDetailedErrorReport();

      assertFalse(
          report.contains("Involved Components:"),
          "Report should not contain Involved Components section when null");
    }
  }

  @Nested
  @DisplayName("getDetailedErrorReport Content Tests")
  class GetDetailedErrorReportContentTests {

    @Test
    @DisplayName("Report should include compatibility issues")
    void reportShouldIncludeCompatibilityIssues() {
      final Map<String, String> issues = new HashMap<>();
      issues.put("comp-a:comp-b", "Version mismatch");
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.VERSION_INCOMPATIBILITY, null, issues, null, null, null);

      final String report = exception.getDetailedErrorReport();

      assertTrue(
          report.contains("Compatibility Issues:"), "Report should contain Compatibility Issues");
      assertTrue(report.contains("comp-a:comp-b"), "Report should contain issue key");
      assertTrue(report.contains("Version mismatch"), "Report should contain issue value");
    }

    @Test
    @DisplayName("Report should include missing dependencies")
    void reportShouldIncludeMissingDependencies() {
      final Set<String> missing = new HashSet<>(Arrays.asList("dep-a", "dep-b"));
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.MISSING_DEPENDENCIES, null, null, missing, null, null);

      final String report = exception.getDetailedErrorReport();

      assertTrue(
          report.contains("Missing Dependencies:"), "Report should contain Missing Dependencies");
      assertTrue(report.contains("dep-a"), "Report should contain first missing dependency");
      assertTrue(report.contains("dep-b"), "Report should contain second missing dependency");
    }

    @Test
    @DisplayName("Report should include circular dependencies")
    void reportShouldIncludeCircularDependencies() {
      final Set<String> circular = new HashSet<>(Arrays.asList("comp-x", "comp-y"));
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.CIRCULAR_DEPENDENCY, null, null, null, circular, null);

      final String report = exception.getDetailedErrorReport();

      assertTrue(
          report.contains("Circular Dependencies:"), "Report should contain Circular Dependencies");
      assertTrue(report.contains("comp-x"), "Report should contain first circular component");
      assertTrue(report.contains("comp-y"), "Report should contain second circular component");
    }

    @Test
    @DisplayName("Report should include numbered suggested resolutions")
    void reportShouldIncludeNumberedSuggestedResolutions() {
      final List<String> suggestions = Arrays.asList("First fix", "Second fix", "Third fix");
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.UNKNOWN, null, null, null, null, suggestions);

      final String report = exception.getDetailedErrorReport();

      assertTrue(
          report.contains("Suggested Resolutions:"), "Report should contain Suggested Resolutions");
      assertTrue(report.contains("1. First fix"), "Report should contain numbered first suggestion");
      assertTrue(
          report.contains("2. Second fix"), "Report should contain numbered second suggestion");
      assertTrue(report.contains("3. Third fix"), "Report should contain numbered third suggestion");
    }

    @Test
    @DisplayName("Report should show No for non-recoverable failure")
    void reportShouldShowNoForNonRecoverableFailure() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", LinkingFailureType.CIRCULAR_DEPENDENCY, null, null, null, null, null);

      final String report = exception.getDetailedErrorReport();

      assertTrue(report.contains("Recoverable: No"), "Report should contain 'Recoverable: No'");
    }
  }

  @Nested
  @DisplayName("Constructor with Cause Mutation Tests")
  class ConstructorWithCauseMutationTests {

    @Test
    @DisplayName("Constructor with cause should copy involved components")
    void constructorWithCauseShouldCopyInvolvedComponents() {
      final List<String> components = Arrays.asList("comp-1", "comp-2");
      final Throwable cause = new RuntimeException("cause");

      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error",
              cause,
              LinkingFailureType.UNKNOWN,
              components,
              null,
              null,
              null,
              null);

      final List<String> retrieved = exception.getInvolvedComponents();
      assertNotNull(retrieved, "Should have involved components");
      assertEquals(2, retrieved.size(), "Should have 2 components");
      assertNotSame(components, retrieved, "Should return a copy");
    }

    @Test
    @DisplayName("Constructor with cause should copy compatibility issues")
    void constructorWithCauseShouldCopyCompatibilityIssues() {
      final Map<String, String> issues = new HashMap<>();
      issues.put("key", "value");
      final Throwable cause = new RuntimeException("cause");

      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", cause, LinkingFailureType.UNKNOWN, null, issues, null, null, null);

      final Map<String, String> retrieved = exception.getCompatibilityIssues();
      assertNotNull(retrieved, "Should have compatibility issues");
      assertEquals(1, retrieved.size(), "Should have 1 issue");
      assertNotSame(issues, retrieved, "Should return a copy");
    }

    @Test
    @DisplayName("Constructor with cause should copy missing dependencies")
    void constructorWithCauseShouldCopyMissingDependencies() {
      final Set<String> missing = new HashSet<>(Arrays.asList("dep"));
      final Throwable cause = new RuntimeException("cause");

      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", cause, LinkingFailureType.UNKNOWN, null, null, missing, null, null);

      final Set<String> retrieved = exception.getMissingDependencies();
      assertNotNull(retrieved, "Should have missing dependencies");
      assertEquals(1, retrieved.size(), "Should have 1 dependency");
      assertNotSame(missing, retrieved, "Should return a copy");
    }

    @Test
    @DisplayName("Constructor with cause should copy circular dependencies")
    void constructorWithCauseShouldCopyCircularDependencies() {
      final Set<String> circular = new HashSet<>(Arrays.asList("comp"));
      final Throwable cause = new RuntimeException("cause");

      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", cause, LinkingFailureType.UNKNOWN, null, null, null, circular, null);

      final Set<String> retrieved = exception.getCircularDependencies();
      assertNotNull(retrieved, "Should have circular dependencies");
      assertEquals(1, retrieved.size(), "Should have 1 circular dependency");
      assertNotSame(circular, retrieved, "Should return a copy");
    }

    @Test
    @DisplayName("Constructor with cause should copy suggested resolutions")
    void constructorWithCauseShouldCopySuggestedResolutions() {
      final List<String> suggestions = Arrays.asList("fix");
      final Throwable cause = new RuntimeException("cause");

      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", cause, LinkingFailureType.UNKNOWN, null, null, null, null, suggestions);

      final List<String> retrieved = exception.getSuggestedResolutions();
      assertNotNull(retrieved, "Should have suggested resolutions");
      assertEquals(1, retrieved.size(), "Should have 1 suggestion");
      assertNotSame(suggestions, retrieved, "Should return a copy");
    }

    @Test
    @DisplayName("Constructor with cause should handle null collections")
    void constructorWithCauseShouldHandleNullCollections() {
      final Throwable cause = new RuntimeException("cause");

      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Error", cause, LinkingFailureType.UNKNOWN, null, null, null, null, null);

      assertNull(exception.getInvolvedComponents(), "Components should be null");
      assertNull(exception.getCompatibilityIssues(), "Compatibility issues should be null");
      assertNull(exception.getMissingDependencies(), "Missing dependencies should be null");
      assertNull(exception.getCircularDependencies(), "Circular dependencies should be null");
      assertNull(exception.getSuggestedResolutions(), "Suggestions should be null");
    }
  }

  @Nested
  @DisplayName("Usage Tests")
  class UsageTests {

    @Test
    @DisplayName("Should be throwable")
    void shouldBeThrowable() {
      final ComponentLinkingException exception =
          new ComponentLinkingException(
              "Test", LinkingFailureType.UNKNOWN, null, null, null, null, null);

      assertTrue(exception instanceof Throwable, "ComponentLinkingException should be throwable");
    }

    @Test
    @DisplayName("Should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      try {
        throw new ComponentLinkingException(
            "Test error", LinkingFailureType.UNKNOWN, null, null, null, null, null);
      } catch (WasmException e) {
        assertEquals("Test error", e.getMessage(), "Should be catchable as WasmException");
      }
    }
  }
}
