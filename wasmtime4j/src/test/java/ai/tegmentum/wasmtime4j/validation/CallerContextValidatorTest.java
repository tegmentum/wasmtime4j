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

package ai.tegmentum.wasmtime4j.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.validation.CallerContextValidator.ValidationConfig;
import ai.tegmentum.wasmtime4j.validation.CallerContextValidator.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CallerContextValidator} - validation framework for caller context
 * implementations.
 *
 * <p>Validates utility class structure, configuration factories, ValidationConfig fields, and
 * ValidationResult behavior.
 */
@DisplayName("CallerContextValidator Tests")
class CallerContextValidatorTest {

  @Nested
  @DisplayName("Utility Class Structure Tests")
  class UtilityClassStructureTests {

    @Test
    @DisplayName("class should be final")
    void classShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(CallerContextValidator.class.getModifiers()),
          "CallerContextValidator should be a final class");
    }

    @Test
    @DisplayName("constructor should be private")
    void constructorShouldBePrivate() throws NoSuchMethodException {
      final java.lang.reflect.Constructor<CallerContextValidator> constructor =
          CallerContextValidator.class.getDeclaredConstructor();
      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "CallerContextValidator constructor should be private");
    }
  }

  @Nested
  @DisplayName("CreateComprehensiveConfig Tests")
  class CreateComprehensiveConfigTests {

    @Test
    @DisplayName("createComprehensiveConfig should return non-null config")
    void createComprehensiveConfigShouldReturnNonNull() {
      final ValidationConfig config = CallerContextValidator.createComprehensiveConfig();
      assertNotNull(config, "createComprehensiveConfig should return non-null config");
    }

    @Test
    @DisplayName("createComprehensiveConfig should enable all validations")
    void createComprehensiveConfigShouldEnableAllValidations() {
      final ValidationConfig config = CallerContextValidator.createComprehensiveConfig();
      assertTrue(
          config.validateBasicFunctionality,
          "Comprehensive config should enable basic functionality validation");
      assertTrue(
          config.validateExportAccess,
          "Comprehensive config should enable export access validation");
      assertTrue(
          config.validateFuelOperations,
          "Comprehensive config should enable fuel operations validation");
      assertTrue(
          config.validateEpochOperations,
          "Comprehensive config should enable epoch operations validation");
      assertTrue(
          config.validateErrorHandling,
          "Comprehensive config should enable error handling validation");
    }
  }

  @Nested
  @DisplayName("CreateBasicConfig Tests")
  class CreateBasicConfigTests {

    @Test
    @DisplayName("createBasicConfig should return non-null config")
    void createBasicConfigShouldReturnNonNull() {
      final ValidationConfig config = CallerContextValidator.createBasicConfig();
      assertNotNull(config, "createBasicConfig should return non-null config");
    }

    @Test
    @DisplayName("createBasicConfig should only enable basic functionality")
    void createBasicConfigShouldOnlyEnableBasicFunctionality() {
      final ValidationConfig config = CallerContextValidator.createBasicConfig();
      assertTrue(
          config.validateBasicFunctionality,
          "Basic config should enable basic functionality validation");
      assertFalse(
          config.validateExportAccess,
          "Basic config should disable export access validation");
      assertFalse(
          config.validateFuelOperations,
          "Basic config should disable fuel operations validation");
      assertFalse(
          config.validateEpochOperations,
          "Basic config should disable epoch operations validation");
      assertFalse(
          config.validateErrorHandling,
          "Basic config should disable error handling validation");
    }
  }

  @Nested
  @DisplayName("ValidationConfig Tests")
  class ValidationConfigTests {

    @Test
    @DisplayName("ValidationConfig constructor should set all fields")
    void validationConfigConstructorShouldSetAllFields() {
      final ValidationConfig config = new ValidationConfig(true, false, true, false, true);
      assertTrue(config.validateBasicFunctionality, "validateBasicFunctionality should be true");
      assertFalse(config.validateExportAccess, "validateExportAccess should be false");
      assertTrue(config.validateFuelOperations, "validateFuelOperations should be true");
      assertFalse(config.validateEpochOperations, "validateEpochOperations should be false");
      assertTrue(config.validateErrorHandling, "validateErrorHandling should be true");
    }

    @Test
    @DisplayName("ValidationConfig with all false should have all fields false")
    void validationConfigAllFalseShouldHaveAllFieldsFalse() {
      final ValidationConfig config = new ValidationConfig(false, false, false, false, false);
      assertFalse(config.validateBasicFunctionality, "validateBasicFunctionality should be false");
      assertFalse(config.validateExportAccess, "validateExportAccess should be false");
      assertFalse(config.validateFuelOperations, "validateFuelOperations should be false");
      assertFalse(config.validateEpochOperations, "validateEpochOperations should be false");
      assertFalse(config.validateErrorHandling, "validateErrorHandling should be false");
    }
  }

  @Nested
  @DisplayName("ValidationResult Tests")
  class ValidationResultTests {

    @Test
    @DisplayName("new ValidationResult should be valid")
    void newValidationResultShouldBeValid() {
      final ValidationResult result = new ValidationResult();
      assertTrue(result.isValid(), "New ValidationResult should be valid (no errors)");
    }

    @Test
    @DisplayName("new ValidationResult should have empty lists")
    void newValidationResultShouldHaveEmptyLists() {
      final ValidationResult result = new ValidationResult();
      assertNotNull(result.getSuccesses(), "Successes list should not be null");
      assertTrue(result.getSuccesses().isEmpty(), "Successes list should be empty initially");
      assertNotNull(result.getWarnings(), "Warnings list should not be null");
      assertTrue(result.getWarnings().isEmpty(), "Warnings list should be empty initially");
      assertNotNull(result.getErrors(), "Errors list should not be null");
      assertTrue(result.getErrors().isEmpty(), "Errors list should be empty initially");
    }

    @Test
    @DisplayName("addSuccess should add to successes list")
    void addSuccessShouldAddToSuccessesList() {
      final ValidationResult result = new ValidationResult();
      result.addSuccess("test success");
      assertEquals(1, result.getSuccesses().size(), "Should have 1 success");
      assertEquals("test success", result.getSuccesses().get(0), "Success message should match");
      assertTrue(result.isValid(), "Result should still be valid after adding success");
    }

    @Test
    @DisplayName("addWarning should add to warnings list")
    void addWarningShouldAddToWarningsList() {
      final ValidationResult result = new ValidationResult();
      result.addWarning("test warning");
      assertEquals(1, result.getWarnings().size(), "Should have 1 warning");
      assertEquals("test warning", result.getWarnings().get(0), "Warning message should match");
      assertTrue(result.isValid(), "Result should still be valid after adding warning");
    }

    @Test
    @DisplayName("addError should add to errors list and make result invalid")
    void addErrorShouldAddToErrorsListAndMakeResultInvalid() {
      final ValidationResult result = new ValidationResult();
      result.addError("test error");
      assertEquals(1, result.getErrors().size(), "Should have 1 error");
      assertEquals("test error", result.getErrors().get(0), "Error message should match");
      assertFalse(result.isValid(), "Result should be invalid after adding error");
    }

    @Test
    @DisplayName("isValid should return false when errors present")
    void isValidShouldReturnFalseWhenErrorsPresent() {
      final ValidationResult result = new ValidationResult();
      result.addSuccess("success");
      result.addWarning("warning");
      result.addError("error");
      assertFalse(result.isValid(), "Result should be invalid when errors are present");
    }

    @Test
    @DisplayName("getSuccesses should return unmodifiable list")
    void getSuccessesShouldReturnUnmodifiableList() {
      final ValidationResult result = new ValidationResult();
      result.addSuccess("test");
      final java.util.List<String> successes = result.getSuccesses();
      assertThrows(
          UnsupportedOperationException.class,
          () -> successes.add("should fail"),
          "Successes list should be unmodifiable");
    }

    @Test
    @DisplayName("getWarnings should return unmodifiable list")
    void getWarningsShouldReturnUnmodifiableList() {
      final ValidationResult result = new ValidationResult();
      result.addWarning("test");
      final java.util.List<String> warnings = result.getWarnings();
      assertThrows(
          UnsupportedOperationException.class,
          () -> warnings.add("should fail"),
          "Warnings list should be unmodifiable");
    }

    @Test
    @DisplayName("getErrors should return unmodifiable list")
    void getErrorsShouldReturnUnmodifiableList() {
      final ValidationResult result = new ValidationResult();
      result.addError("test");
      final java.util.List<String> errors = result.getErrors();
      assertThrows(
          UnsupportedOperationException.class,
          () -> errors.add("should fail"),
          "Errors list should be unmodifiable");
    }

    @Test
    @DisplayName("forEachError should iterate over all errors")
    void forEachErrorShouldIterateOverAllErrors() {
      final ValidationResult result = new ValidationResult();
      result.addError("error1");
      result.addError("error2");
      result.addError("error3");

      final java.util.List<String> collected = new java.util.ArrayList<>();
      result.forEachError(collected::add);
      assertEquals(3, collected.size(), "forEachError should iterate over all 3 errors");
      assertEquals("error1", collected.get(0), "First error should be 'error1'");
      assertEquals("error2", collected.get(1), "Second error should be 'error2'");
      assertEquals("error3", collected.get(2), "Third error should be 'error3'");
    }
  }

  @Nested
  @DisplayName("CreateSummary Tests")
  class CreateSummaryTests {

    @Test
    @DisplayName("createSummary should return non-null string")
    void createSummaryShouldReturnNonNull() {
      final ValidationResult result = new ValidationResult();
      final String summary = result.createSummary();
      assertNotNull(summary, "createSummary should return non-null string");
    }

    @Test
    @DisplayName("createSummary should contain PASSED for valid result")
    void createSummaryShouldContainPassedForValidResult() {
      final ValidationResult result = new ValidationResult();
      result.addSuccess("ok");
      final String summary = result.createSummary();
      assertTrue(
          summary.contains("PASSED"),
          "Summary should contain PASSED for valid result, got: " + summary);
    }

    @Test
    @DisplayName("createSummary should contain FAILED for invalid result")
    void createSummaryShouldContainFailedForInvalidResult() {
      final ValidationResult result = new ValidationResult();
      result.addError("failed");
      final String summary = result.createSummary();
      assertTrue(
          summary.contains("FAILED"),
          "Summary should contain FAILED for invalid result, got: " + summary);
    }

    @Test
    @DisplayName("createSummary should include counts")
    void createSummaryShouldIncludeCounts() {
      final ValidationResult result = new ValidationResult();
      result.addSuccess("s1");
      result.addSuccess("s2");
      result.addWarning("w1");
      result.addError("e1");
      final String summary = result.createSummary();
      assertTrue(
          summary.contains("Successes: 2"),
          "Summary should show 2 successes, got: " + summary);
      assertTrue(
          summary.contains("Warnings: 1"),
          "Summary should show 1 warning, got: " + summary);
      assertTrue(
          summary.contains("Errors: 1"),
          "Summary should show 1 error, got: " + summary);
    }

    @Test
    @DisplayName("createSummary should list error details")
    void createSummaryShouldListErrorDetails() {
      final ValidationResult result = new ValidationResult();
      result.addError("specific error detail");
      final String summary = result.createSummary();
      assertTrue(
          summary.contains("specific error detail"),
          "Summary should include error details, got: " + summary);
    }

    @Test
    @DisplayName("createSummary should list warning details")
    void createSummaryShouldListWarningDetails() {
      final ValidationResult result = new ValidationResult();
      result.addWarning("specific warning detail");
      final String summary = result.createSummary();
      assertTrue(
          summary.contains("specific warning detail"),
          "Summary should include warning details, got: " + summary);
    }
  }

}
