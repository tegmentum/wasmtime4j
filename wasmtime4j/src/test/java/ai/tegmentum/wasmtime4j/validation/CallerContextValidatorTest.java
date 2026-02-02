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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.tegmentum.wasmtime4j.Caller;
import ai.tegmentum.wasmtime4j.Function;
import ai.tegmentum.wasmtime4j.Global;
import ai.tegmentum.wasmtime4j.Memory;
import ai.tegmentum.wasmtime4j.Table;
import ai.tegmentum.wasmtime4j.validation.CallerContextValidator.ValidationConfig;
import ai.tegmentum.wasmtime4j.validation.CallerContextValidator.ValidationResult;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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

  @Nested
  @DisplayName("Validate Method Tests")
  class ValidateMethodTests {

    private Caller<String> mockCaller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
      mockCaller = mock(Caller.class);
    }

    @Test
    @DisplayName("validate with basic config should only validate basic functionality")
    void validateWithBasicConfigShouldOnlyValidateBasicFunctionality() {
      when(mockCaller.data()).thenReturn("test data");

      final ValidationConfig config = CallerContextValidator.createBasicConfig();
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(result.isValid(), "Validation should pass with working basic functionality");
      assertFalse(
          result.getSuccesses().isEmpty(), "Should have at least one success for basic validation");
    }

    @Test
    @DisplayName("validate with comprehensive config should validate all aspects")
    @SuppressWarnings("unchecked")
    void validateWithComprehensiveConfigShouldValidateAllAspects() throws Exception {
      // Setup mock for all validation methods
      when(mockCaller.data()).thenReturn("test data");
      when(mockCaller.hasExport(anyString())).thenReturn(true);
      when(mockCaller.getExport(anyString())).thenReturn(Optional.empty());
      when(mockCaller.getMemory(anyString())).thenReturn(Optional.of(mock(Memory.class)));
      when(mockCaller.getFunction(anyString())).thenReturn(Optional.of(mock(Function.class)));
      when(mockCaller.getGlobal(anyString())).thenReturn(Optional.of(mock(Global.class)));
      when(mockCaller.getTable(anyString())).thenReturn(Optional.of(mock(Table.class)));
      when(mockCaller.fuelConsumed()).thenReturn(Optional.of(100L));
      when(mockCaller.fuelRemaining()).thenReturn(Optional.of(1000L));
      when(mockCaller.hasEpochDeadline()).thenReturn(true);
      when(mockCaller.epochDeadline()).thenReturn(Optional.of(System.currentTimeMillis() + 5000));
      // Error handling - null parameter rejection
      when(mockCaller.hasExport(null)).thenThrow(new IllegalArgumentException("null not allowed"));
      when(mockCaller.getMemory(null)).thenThrow(new IllegalArgumentException("null not allowed"));
      doThrow(new IllegalArgumentException("negative fuel"))
          .when(mockCaller)
          .addFuel(-100);

      final ValidationConfig config = CallerContextValidator.createComprehensiveConfig();
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(
          result.getSuccesses().size() > 5,
          "Should have multiple successes for comprehensive validation: " + result.getSuccesses());
    }

    @Test
    @DisplayName("validate should handle exception during validation")
    void validateShouldHandleExceptionDuringValidation() {
      when(mockCaller.data()).thenThrow(new RuntimeException("Simulated failure"));

      final ValidationConfig config = CallerContextValidator.createBasicConfig();
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertFalse(result.isValid(), "Validation should fail when exception occurs");
      assertTrue(
          result.getErrors().stream().anyMatch(e -> e.contains("Failed to access caller data")),
          "Should record exception in errors: " + result.getErrors());
    }
  }

  @Nested
  @DisplayName("Validate Basic Functionality Tests")
  class ValidateBasicFunctionalityTests {

    @Test
    @DisplayName("should succeed when data() works")
    @SuppressWarnings("unchecked")
    void shouldSucceedWhenDataWorks() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");

      final ValidationConfig config = new ValidationConfig(true, false, false, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(result.isValid(), "Should be valid");
      assertTrue(
          result.getSuccesses().stream().anyMatch(s -> s.contains("Basic data access")),
          "Should have basic data access success");
    }

    @Test
    @DisplayName("should add error when data() throws")
    @SuppressWarnings("unchecked")
    void shouldAddErrorWhenDataThrows() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenThrow(new RuntimeException("data access failed"));

      final ValidationConfig config = new ValidationConfig(true, false, false, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertFalse(result.isValid(), "Should be invalid when data() throws");
    }
  }

  @Nested
  @DisplayName("Validate Export Access Tests")
  class ValidateExportAccessTests {

    @Test
    @DisplayName("should succeed when export methods work")
    @SuppressWarnings("unchecked")
    void shouldSucceedWhenExportMethodsWork() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(anyString())).thenReturn(true);
      when(mockCaller.getExport(anyString())).thenReturn(Optional.empty());
      when(mockCaller.getMemory(anyString())).thenReturn(Optional.of(mock(Memory.class)));
      when(mockCaller.getFunction(anyString())).thenReturn(Optional.of(mock(Function.class)));
      when(mockCaller.getGlobal(anyString())).thenReturn(Optional.of(mock(Global.class)));
      when(mockCaller.getTable(anyString())).thenReturn(Optional.of(mock(Table.class)));

      final ValidationConfig config = new ValidationConfig(true, true, false, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(result.isValid(), "Should be valid when export access works");
      assertTrue(
          result.getSuccesses().stream().anyMatch(s -> s.contains("hasExport")),
          "Should have hasExport success");
    }

    @Test
    @DisplayName("should add error when export access fails")
    @SuppressWarnings("unchecked")
    void shouldAddErrorWhenExportAccessFails() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(anyString())).thenThrow(new RuntimeException("export failed"));

      final ValidationConfig config = new ValidationConfig(true, true, false, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertFalse(result.isValid(), "Should be invalid when export access fails");
    }

    @Test
    @DisplayName("should add warnings when specific export types fail")
    @SuppressWarnings("unchecked")
    void shouldAddWarningsWhenSpecificExportTypesFail() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(anyString())).thenReturn(false);
      when(mockCaller.getExport(anyString())).thenReturn(Optional.empty());
      when(mockCaller.getMemory(anyString())).thenThrow(new RuntimeException("no memory"));
      when(mockCaller.getFunction(anyString())).thenThrow(new RuntimeException("no function"));
      when(mockCaller.getGlobal(anyString())).thenThrow(new RuntimeException("no global"));
      when(mockCaller.getTable(anyString())).thenThrow(new RuntimeException("no table"));

      final ValidationConfig config = new ValidationConfig(true, true, false, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(result.isValid(), "Should still be valid (warnings, not errors)");
      assertFalse(result.getWarnings().isEmpty(), "Should have warnings for failed export types");
    }
  }

  @Nested
  @DisplayName("Validate Fuel Operations Tests")
  class ValidateFuelOperationsTests {

    @Test
    @DisplayName("should succeed when fuel operations work")
    @SuppressWarnings("unchecked")
    void shouldSucceedWhenFuelOperationsWork() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.fuelConsumed()).thenReturn(Optional.of(100L));
      when(mockCaller.fuelRemaining()).thenReturn(Optional.of(1000L), Optional.of(1100L));

      final ValidationConfig config = new ValidationConfig(true, false, true, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(result.isValid(), "Should be valid when fuel operations work");
      assertTrue(
          result.getSuccesses().stream().anyMatch(s -> s.contains("Fuel")),
          "Should have fuel-related success");
    }

    @Test
    @DisplayName("should add warning when fuel metering not enabled")
    @SuppressWarnings("unchecked")
    void shouldAddWarningWhenFuelMeteringNotEnabled() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.fuelConsumed()).thenReturn(Optional.of(0L));
      when(mockCaller.fuelRemaining()).thenReturn(Optional.empty());

      final ValidationConfig config = new ValidationConfig(true, false, true, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(result.isValid(), "Should still be valid");
      assertTrue(
          result.getWarnings().stream().anyMatch(s -> s.contains("not enabled")),
          "Should have warning about fuel metering not enabled");
    }

    @Test
    @DisplayName("should add warning when fuel addition not working correctly")
    @SuppressWarnings("unchecked")
    void shouldAddWarningWhenFuelAdditionNotWorkingCorrectly() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.fuelConsumed()).thenReturn(Optional.of(100L));
      // Return same value before and after addFuel (simulating it not working)
      when(mockCaller.fuelRemaining()).thenReturn(Optional.of(1000L));

      final ValidationConfig config = new ValidationConfig(true, false, true, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(
          result.getWarnings().stream().anyMatch(s -> s.contains("may not be working")),
          "Should have warning about fuel addition not working correctly");
    }

    @Test
    @DisplayName("should handle IllegalArgumentException for input validation")
    @SuppressWarnings("unchecked")
    void shouldHandleIllegalArgumentExceptionForInputValidation() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.fuelConsumed()).thenThrow(new IllegalArgumentException("invalid"));

      final ValidationConfig config = new ValidationConfig(true, false, true, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(result.isValid(), "IAE should be treated as proper input validation");
      assertTrue(
          result.getSuccesses().stream().anyMatch(s -> s.contains("properly validate")),
          "Should note that inputs are properly validated");
    }

    @Test
    @DisplayName("should add error when fuel operations fail")
    @SuppressWarnings("unchecked")
    void shouldAddErrorWhenFuelOperationsFail() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.fuelConsumed()).thenThrow(new RuntimeException("fuel error"));

      final ValidationConfig config = new ValidationConfig(true, false, true, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertFalse(result.isValid(), "Should be invalid when fuel operations fail");
    }
  }

  @Nested
  @DisplayName("Validate Epoch Operations Tests")
  class ValidateEpochOperationsTests {

    @Test
    @DisplayName("should succeed when epoch operations work")
    @SuppressWarnings("unchecked")
    void shouldSucceedWhenEpochOperationsWork() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasEpochDeadline()).thenReturn(false, true);
      when(mockCaller.epochDeadline()).thenReturn(Optional.of(System.currentTimeMillis() + 5000));

      final ValidationConfig config = new ValidationConfig(true, false, false, true, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(result.isValid(), "Should be valid when epoch operations work");
      assertTrue(
          result.getSuccesses().stream().anyMatch(s -> s.contains("Epoch")),
          "Should have epoch-related success");
    }

    @Test
    @DisplayName("should add warning when epoch deadline setting not working")
    @SuppressWarnings("unchecked")
    void shouldAddWarningWhenEpochDeadlineSettingNotWorking() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasEpochDeadline()).thenReturn(false);
      when(mockCaller.epochDeadline()).thenReturn(Optional.empty());

      final ValidationConfig config = new ValidationConfig(true, false, false, true, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(
          result.getWarnings().stream().anyMatch(s -> s.contains("may not be working")),
          "Should have warning about epoch deadline not working");
    }

    @Test
    @DisplayName("should add error when epoch operations fail")
    @SuppressWarnings("unchecked")
    void shouldAddErrorWhenEpochOperationsFail() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasEpochDeadline()).thenThrow(new RuntimeException("epoch error"));

      final ValidationConfig config = new ValidationConfig(true, false, false, true, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertFalse(result.isValid(), "Should be invalid when epoch operations fail");
    }
  }

  @Nested
  @DisplayName("Validate Error Handling Tests")
  class ValidateErrorHandlingTests {

    @Test
    @DisplayName("should succeed when null parameters are properly rejected")
    @SuppressWarnings("unchecked")
    void shouldSucceedWhenNullParametersProperlyRejected() throws Exception {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(null)).thenThrow(new IllegalArgumentException("null"));
      when(mockCaller.getMemory(null)).thenThrow(new IllegalArgumentException("null"));
      doThrow(new IllegalArgumentException("negative")).when(mockCaller).addFuel(-100);

      final ValidationConfig config = new ValidationConfig(true, false, false, false, true);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(result.isValid(), "Should be valid when error handling works");
      assertTrue(
          result.getSuccesses().stream().anyMatch(s -> s.contains("rejects null")),
          "Should note proper null rejection");
      assertTrue(
          result.getSuccesses().stream().anyMatch(s -> s.contains("rejects negative")),
          "Should note proper negative value rejection");
    }

    @Test
    @DisplayName("should add error when hasExport accepts null")
    @SuppressWarnings("unchecked")
    void shouldAddErrorWhenHasExportAcceptsNull() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(null)).thenReturn(false); // Should throw, not return

      final ValidationConfig config = new ValidationConfig(true, false, false, false, true);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertFalse(result.isValid(), "Should be invalid when hasExport accepts null");
      assertTrue(
          result.getErrors().stream().anyMatch(e -> e.contains("should reject null")),
          "Should have error about accepting null");
    }

    @Test
    @DisplayName("should add error when getMemory accepts null")
    @SuppressWarnings("unchecked")
    void shouldAddErrorWhenGetMemoryAcceptsNull() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(null)).thenThrow(new IllegalArgumentException("null"));
      when(mockCaller.getMemory(null)).thenReturn(Optional.empty()); // Should throw

      final ValidationConfig config = new ValidationConfig(true, false, false, false, true);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertFalse(result.isValid(), "Should be invalid when getMemory accepts null");
    }

    @Test
    @DisplayName("should add error when addFuel accepts negative values")
    @SuppressWarnings("unchecked")
    void shouldAddErrorWhenAddFuelAcceptsNegativeValues() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(null)).thenThrow(new IllegalArgumentException("null"));
      when(mockCaller.getMemory(null)).thenThrow(new IllegalArgumentException("null"));
      // addFuel doesn't throw for negative - should be an error

      final ValidationConfig config = new ValidationConfig(true, false, false, false, true);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertFalse(result.isValid(), "Should be invalid when addFuel accepts negative");
    }

    @Test
    @DisplayName("should add warnings for non-IAE exceptions")
    @SuppressWarnings("unchecked")
    void shouldAddWarningsForNonIaeExceptions() throws Exception {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(null)).thenThrow(new NullPointerException("null"));
      when(mockCaller.getMemory(null)).thenThrow(new NullPointerException("null"));
      doThrow(new RuntimeException("error")).when(mockCaller).addFuel(-100);

      final ValidationConfig config = new ValidationConfig(true, false, false, false, true);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(result.isValid(), "Non-IAE exceptions should result in warnings, not errors");
      assertFalse(result.getWarnings().isEmpty(), "Should have warnings for non-IAE handling");
    }
  }

  @Nested
  @DisplayName("Success Count Verification Tests")
  class SuccessCountVerificationTests {

    @Test
    @DisplayName("validateFuelOperations should add multiple success messages when working correctly")
    @SuppressWarnings("unchecked")
    void validateFuelOperationsShouldAddMultipleSuccessMessages() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.fuelConsumed()).thenReturn(Optional.of(50L));
      // Initial: 1000, after adding 100: >= 1100
      when(mockCaller.fuelRemaining())
          .thenReturn(Optional.of(1000L))
          .thenReturn(Optional.of(1100L));

      final ValidationConfig config = new ValidationConfig(true, false, true, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      // Should have: basic data access + fuel consumed + fuel remaining + fuel addition
      // Count fuel-related successes
      long fuelSuccesses =
          result.getSuccesses().stream().filter(s -> s.toLowerCase().contains("fuel")).count();
      assertTrue(
          fuelSuccesses >= 3,
          "Should have at least 3 fuel-related successes for consumption, remaining, and addition: "
              + result.getSuccesses());
    }

    @Test
    @DisplayName("validateEpochOperations should add multiple success messages when working")
    @SuppressWarnings("unchecked")
    void validateEpochOperationsShouldAddMultipleSuccessMessages() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasEpochDeadline()).thenReturn(false, true);
      when(mockCaller.epochDeadline()).thenReturn(Optional.of(System.currentTimeMillis() + 10000));

      final ValidationConfig config = new ValidationConfig(true, false, false, true, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      // Count epoch-related successes
      long epochSuccesses =
          result.getSuccesses().stream().filter(s -> s.toLowerCase().contains("epoch")).count();
      assertTrue(
          epochSuccesses >= 3,
          "Should have at least 3 epoch-related successes for checking, retrieval, and setting: "
              + result.getSuccesses());
    }

    @Test
    @DisplayName("validateErrorHandling should add success for each proper null rejection")
    @SuppressWarnings("unchecked")
    void validateErrorHandlingShouldAddSuccessForEachProperNullRejection() throws Exception {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(null)).thenThrow(new IllegalArgumentException("null"));
      when(mockCaller.getMemory(null)).thenThrow(new IllegalArgumentException("null"));
      doThrow(new IllegalArgumentException("negative")).when(mockCaller).addFuel(-100);

      final ValidationConfig config = new ValidationConfig(true, false, false, false, true);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      // Count rejection successes
      long rejectSuccesses =
          result.getSuccesses().stream().filter(s -> s.toLowerCase().contains("reject")).count();
      assertTrue(
          rejectSuccesses >= 3,
          "Should have at least 3 rejection successes for hasExport, getMemory, and addFuel: "
              + result.getSuccesses());
    }

    @Test
    @DisplayName("validateExportAccess should add success for successful export check")
    @SuppressWarnings("unchecked")
    void validateExportAccessShouldAddSuccessForSuccessfulExportCheck() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(anyString())).thenReturn(true);
      when(mockCaller.getExport(anyString())).thenReturn(Optional.empty());
      when(mockCaller.getMemory(anyString())).thenReturn(Optional.of(mock(Memory.class)));
      when(mockCaller.getFunction(anyString())).thenReturn(Optional.of(mock(Function.class)));
      when(mockCaller.getGlobal(anyString())).thenReturn(Optional.of(mock(Global.class)));
      when(mockCaller.getTable(anyString())).thenReturn(Optional.of(mock(Table.class)));

      final ValidationConfig config = new ValidationConfig(true, true, false, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      // Verify export access successes
      long exportSuccesses =
          result.getSuccesses().stream()
              .filter(s -> s.toLowerCase().contains("export") || s.toLowerCase().contains("memory")
                  || s.toLowerCase().contains("function") || s.toLowerCase().contains("global")
                  || s.toLowerCase().contains("table"))
              .count();
      assertTrue(
          exportSuccesses >= 2,
          "Should have multiple export-related successes: " + result.getSuccesses());
    }
  }

  @Nested
  @DisplayName("Fuel Addition Boundary Tests")
  class FuelAdditionBoundaryTests {

    @Test
    @DisplayName("fuel addition should check against initialFuel plus exactly 100")
    @SuppressWarnings("unchecked")
    void fuelAdditionShouldCheckAgainstInitialFuelPlusExactly100() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.fuelConsumed()).thenReturn(Optional.of(0L));
      // Initial: 500, after: exactly 600 (= 500 + 100)
      when(mockCaller.fuelRemaining())
          .thenReturn(Optional.of(500L))
          .thenReturn(Optional.of(600L));

      final ValidationConfig config = new ValidationConfig(true, false, true, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      // Should succeed because 600 >= 500 + 100
      assertTrue(
          result.getSuccesses().stream().anyMatch(s -> s.contains("Fuel addition works correctly")),
          "Should have success when afterAddition >= initialFuel + 100: " + result.getSuccesses());
    }

    @Test
    @DisplayName("fuel addition should warn when fuel does not increase by at least 100")
    @SuppressWarnings("unchecked")
    void fuelAdditionShouldWarnWhenFuelDoesNotIncreaseByAtLeast100() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.fuelConsumed()).thenReturn(Optional.of(0L));
      // Initial: 500, after: 599 (= 500 + 99, less than 500 + 100)
      when(mockCaller.fuelRemaining())
          .thenReturn(Optional.of(500L))
          .thenReturn(Optional.of(599L));

      final ValidationConfig config = new ValidationConfig(true, false, true, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      // Should warn because 599 < 500 + 100
      assertTrue(
          result.getWarnings().stream().anyMatch(s -> s.contains("may not be working")),
          "Should warn when afterAddition < initialFuel + 100: " + result.getWarnings());
    }

    @Test
    @DisplayName("fuel addition should succeed at boundary when fuel increases by exactly 100")
    @SuppressWarnings("unchecked")
    void fuelAdditionShouldSucceedAtBoundaryWhenFuelIncreasesByExactly100() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.fuelConsumed()).thenReturn(Optional.of(0L));
      // Initial: 0, after: exactly 100 (= 0 + 100)
      when(mockCaller.fuelRemaining())
          .thenReturn(Optional.of(0L))
          .thenReturn(Optional.of(100L));

      final ValidationConfig config = new ValidationConfig(true, false, true, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      // Should succeed because 100 >= 0 + 100
      assertTrue(
          result.getSuccesses().stream().anyMatch(s -> s.contains("Fuel addition works correctly")),
          "Should succeed when afterAddition equals initialFuel + 100: " + result.getSuccesses());
    }
  }

  @Nested
  @DisplayName("Epoch Deadline Constant Tests")
  class EpochDeadlineConstantTests {

    @Test
    @DisplayName("epoch deadline should be set using system time plus offset")
    @SuppressWarnings("unchecked")
    void epochDeadlineShouldBeSetUsingSystemTimePlusOffset() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      // hasEpochDeadline returns false initially, then true after setting
      when(mockCaller.hasEpochDeadline()).thenReturn(false, true);
      when(mockCaller.epochDeadline()).thenReturn(Optional.of(System.currentTimeMillis() + 5000));

      final ValidationConfig config = new ValidationConfig(true, false, false, true, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      // The setEpochDeadline method should have been called and epoch should be set
      assertTrue(
          result.getSuccesses().stream()
              .anyMatch(s -> s.contains("Epoch deadline setting works correctly")),
          "Should have success for epoch deadline setting: " + result.getSuccesses());
    }
  }

  @Nested
  @DisplayName("Error and Warning Count Verification Tests")
  class ErrorAndWarningCountVerificationTests {

    @Test
    @DisplayName("validateErrorHandling should add error when hasExport does not reject null")
    @SuppressWarnings("unchecked")
    void validateErrorHandlingShouldAddErrorWhenHasExportDoesNotRejectNull() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(null)).thenReturn(false); // Should throw IAE

      final ValidationConfig config = new ValidationConfig(true, false, false, false, true);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(
          result.getErrors().stream().anyMatch(e -> e.contains("should reject null")),
          "Should add error message about rejecting null: " + result.getErrors());
      assertEquals(
          1,
          result.getErrors().stream().filter(e -> e.contains("hasExport")).count(),
          "Should have exactly one error about hasExport null rejection");
    }

    @Test
    @DisplayName("validateErrorHandling should add warning for non-IAE hasExport exception")
    @SuppressWarnings("unchecked")
    void validateErrorHandlingShouldAddWarningForNonIaeHasExportException() throws Exception {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(null)).thenThrow(new NullPointerException("npe"));
      when(mockCaller.getMemory(null)).thenThrow(new IllegalArgumentException("iae"));
      doThrow(new IllegalArgumentException("iae")).when(mockCaller).addFuel(anyLong());

      final ValidationConfig config = new ValidationConfig(true, false, false, false, true);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(
          result.getWarnings().stream().anyMatch(w -> w.contains("hasExport")),
          "Should add warning for non-IAE exception in hasExport: " + result.getWarnings());
    }

    @Test
    @DisplayName("validateErrorHandling should add warning for non-IAE addFuel exception")
    @SuppressWarnings("unchecked")
    void validateErrorHandlingShouldAddWarningForNonIaeAddFuelException() throws Exception {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(null)).thenThrow(new IllegalArgumentException("iae"));
      when(mockCaller.getMemory(null)).thenThrow(new IllegalArgumentException("iae"));
      doThrow(new RuntimeException("not IAE")).when(mockCaller).addFuel(-100);

      final ValidationConfig config = new ValidationConfig(true, false, false, false, true);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(
          result.getWarnings().stream().anyMatch(w -> w.contains("addFuel")),
          "Should add warning for non-IAE exception in addFuel: " + result.getWarnings());
    }

    @Test
    @DisplayName("validateSpecificExportTypes should add success for memory access")
    @SuppressWarnings("unchecked")
    void validateSpecificExportTypesShouldAddSuccessForMemoryAccess() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(anyString())).thenReturn(false);
      when(mockCaller.getExport(anyString())).thenReturn(Optional.empty());
      when(mockCaller.getMemory(anyString())).thenReturn(Optional.of(mock(Memory.class)));
      when(mockCaller.getFunction(anyString())).thenReturn(Optional.empty());
      when(mockCaller.getGlobal(anyString())).thenReturn(Optional.empty());
      when(mockCaller.getTable(anyString())).thenReturn(Optional.empty());

      final ValidationConfig config = new ValidationConfig(true, true, false, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(
          result.getSuccesses().stream()
              .anyMatch(s -> s.toLowerCase().contains("memory")),
          "Should add success for memory access: " + result.getSuccesses());
    }

    @Test
    @DisplayName("validateSpecificExportTypes should add warning when memory access throws")
    @SuppressWarnings("unchecked")
    void validateSpecificExportTypesShouldAddWarningWhenMemoryAccessThrows() {
      final Caller<String> mockCaller = mock(Caller.class);
      when(mockCaller.data()).thenReturn("test");
      when(mockCaller.hasExport(anyString())).thenReturn(false);
      when(mockCaller.getExport(anyString())).thenReturn(Optional.empty());
      when(mockCaller.getMemory(anyString())).thenThrow(new RuntimeException("no memory"));
      when(mockCaller.getFunction(anyString())).thenReturn(Optional.empty());
      when(mockCaller.getGlobal(anyString())).thenReturn(Optional.empty());
      when(mockCaller.getTable(anyString())).thenReturn(Optional.empty());

      final ValidationConfig config = new ValidationConfig(true, true, false, false, false);
      final ValidationResult result = CallerContextValidator.validate(mockCaller, config);

      assertTrue(
          result.getWarnings().stream()
              .anyMatch(w -> w.toLowerCase().contains("memory")),
          "Should add warning for failed memory access: " + result.getWarnings());
    }
  }
}
