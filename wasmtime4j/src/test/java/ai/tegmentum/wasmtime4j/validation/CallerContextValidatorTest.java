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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Caller;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CallerContextValidator} class.
 *
 * <p>CallerContextValidator provides a validation framework for caller context implementations.
 */
@DisplayName("CallerContextValidator Class Tests")
class CallerContextValidatorTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(CallerContextValidator.class.getModifiers()),
          "CallerContextValidator should be a final class");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<CallerContextValidator> constructor =
          CallerContextValidator.class.getDeclaredConstructor();
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have static validate method")
    void shouldHaveStaticValidateMethod() throws NoSuchMethodException {
      final Method method =
          CallerContextValidator.class.getMethod(
              "validate", Caller.class, CallerContextValidator.ValidationConfig.class);
      assertNotNull(method, "validate method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "validate should be static");
      assertEquals(
          CallerContextValidator.ValidationResult.class,
          method.getReturnType(),
          "Should return ValidationResult");
    }

    @Test
    @DisplayName("should have static createComprehensiveConfig method")
    void shouldHaveStaticCreateComprehensiveConfigMethod() throws NoSuchMethodException {
      final Method method = CallerContextValidator.class.getMethod("createComprehensiveConfig");
      assertNotNull(method, "createComprehensiveConfig method should exist");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "createComprehensiveConfig should be static");
      assertEquals(
          CallerContextValidator.ValidationConfig.class,
          method.getReturnType(),
          "Should return ValidationConfig");
    }

    @Test
    @DisplayName("should have static createBasicConfig method")
    void shouldHaveStaticCreateBasicConfigMethod() throws NoSuchMethodException {
      final Method method = CallerContextValidator.class.getMethod("createBasicConfig");
      assertNotNull(method, "createBasicConfig method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "createBasicConfig should be static");
      assertEquals(
          CallerContextValidator.ValidationConfig.class,
          method.getReturnType(),
          "Should return ValidationConfig");
    }
  }

  @Nested
  @DisplayName("ValidationConfig Tests")
  class ValidationConfigTests {

    @Test
    @DisplayName("ValidationConfig should be a static nested class")
    void validationConfigShouldBeStaticNestedClass() {
      assertTrue(
          Modifier.isStatic(CallerContextValidator.ValidationConfig.class.getModifiers()),
          "ValidationConfig should be static");
    }

    @Test
    @DisplayName("ValidationConfig should have validateBasicFunctionality field")
    void validationConfigShouldHaveValidateBasicFunctionalityField() throws NoSuchFieldException {
      final var field =
          CallerContextValidator.ValidationConfig.class.getField("validateBasicFunctionality");
      assertNotNull(field, "validateBasicFunctionality field should exist");
      assertEquals(boolean.class, field.getType(), "Should be boolean type");
    }

    @Test
    @DisplayName("ValidationConfig should have validateExportAccess field")
    void validationConfigShouldHaveValidateExportAccessField() throws NoSuchFieldException {
      final var field =
          CallerContextValidator.ValidationConfig.class.getField("validateExportAccess");
      assertNotNull(field, "validateExportAccess field should exist");
      assertEquals(boolean.class, field.getType(), "Should be boolean type");
    }

    @Test
    @DisplayName("ValidationConfig should have validateFuelOperations field")
    void validationConfigShouldHaveValidateFuelOperationsField() throws NoSuchFieldException {
      final var field =
          CallerContextValidator.ValidationConfig.class.getField("validateFuelOperations");
      assertNotNull(field, "validateFuelOperations field should exist");
      assertEquals(boolean.class, field.getType(), "Should be boolean type");
    }

    @Test
    @DisplayName("ValidationConfig should have validateEpochOperations field")
    void validationConfigShouldHaveValidateEpochOperationsField() throws NoSuchFieldException {
      final var field =
          CallerContextValidator.ValidationConfig.class.getField("validateEpochOperations");
      assertNotNull(field, "validateEpochOperations field should exist");
      assertEquals(boolean.class, field.getType(), "Should be boolean type");
    }

    @Test
    @DisplayName("ValidationConfig should have validateErrorHandling field")
    void validationConfigShouldHaveValidateErrorHandlingField() throws NoSuchFieldException {
      final var field =
          CallerContextValidator.ValidationConfig.class.getField("validateErrorHandling");
      assertNotNull(field, "validateErrorHandling field should exist");
      assertEquals(boolean.class, field.getType(), "Should be boolean type");
    }

    @Test
    @DisplayName("ValidationConfig fields should be final")
    void validationConfigFieldsShouldBeFinal() throws NoSuchFieldException {
      assertTrue(
          Modifier.isFinal(
              CallerContextValidator.ValidationConfig.class
                  .getField("validateBasicFunctionality")
                  .getModifiers()),
          "validateBasicFunctionality should be final");
      assertTrue(
          Modifier.isFinal(
              CallerContextValidator.ValidationConfig.class
                  .getField("validateExportAccess")
                  .getModifiers()),
          "validateExportAccess should be final");
      assertTrue(
          Modifier.isFinal(
              CallerContextValidator.ValidationConfig.class
                  .getField("validateFuelOperations")
                  .getModifiers()),
          "validateFuelOperations should be final");
      assertTrue(
          Modifier.isFinal(
              CallerContextValidator.ValidationConfig.class
                  .getField("validateEpochOperations")
                  .getModifiers()),
          "validateEpochOperations should be final");
      assertTrue(
          Modifier.isFinal(
              CallerContextValidator.ValidationConfig.class
                  .getField("validateErrorHandling")
                  .getModifiers()),
          "validateErrorHandling should be final");
    }

    @Test
    @DisplayName("createComprehensiveConfig should enable all validations")
    void createComprehensiveConfigShouldEnableAllValidations() {
      final var config = CallerContextValidator.createComprehensiveConfig();
      assertTrue(config.validateBasicFunctionality, "validateBasicFunctionality should be true");
      assertTrue(config.validateExportAccess, "validateExportAccess should be true");
      assertTrue(config.validateFuelOperations, "validateFuelOperations should be true");
      assertTrue(config.validateEpochOperations, "validateEpochOperations should be true");
      assertTrue(config.validateErrorHandling, "validateErrorHandling should be true");
    }

    @Test
    @DisplayName("createBasicConfig should enable only basic validation")
    void createBasicConfigShouldEnableOnlyBasicValidation() {
      final var config = CallerContextValidator.createBasicConfig();
      assertTrue(config.validateBasicFunctionality, "validateBasicFunctionality should be true");
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
    @DisplayName("ValidationResult should be a static nested class")
    void validationResultShouldBeStaticNestedClass() {
      assertTrue(
          Modifier.isStatic(CallerContextValidator.ValidationResult.class.getModifiers()),
          "ValidationResult should be static");
    }

    @Test
    @DisplayName("ValidationResult should have isValid method")
    void validationResultShouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = CallerContextValidator.ValidationResult.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("ValidationResult should have getSuccesses method")
    void validationResultShouldHaveGetSuccessesMethod() throws NoSuchMethodException {
      final Method method = CallerContextValidator.ValidationResult.class.getMethod("getSuccesses");
      assertNotNull(method, "getSuccesses method should exist");
      assertEquals(java.util.List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("ValidationResult should have getWarnings method")
    void validationResultShouldHaveGetWarningsMethod() throws NoSuchMethodException {
      final Method method = CallerContextValidator.ValidationResult.class.getMethod("getWarnings");
      assertNotNull(method, "getWarnings method should exist");
      assertEquals(java.util.List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("ValidationResult should have getErrors method")
    void validationResultShouldHaveGetErrorsMethod() throws NoSuchMethodException {
      final Method method = CallerContextValidator.ValidationResult.class.getMethod("getErrors");
      assertNotNull(method, "getErrors method should exist");
      assertEquals(java.util.List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("ValidationResult should have forEachError method")
    void validationResultShouldHaveForEachErrorMethod() throws NoSuchMethodException {
      final Method method =
          CallerContextValidator.ValidationResult.class.getMethod(
              "forEachError", java.util.function.Consumer.class);
      assertNotNull(method, "forEachError method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("ValidationResult should have createSummary method")
    void validationResultShouldHaveCreateSummaryMethod() throws NoSuchMethodException {
      final Method method =
          CallerContextValidator.ValidationResult.class.getMethod("createSummary");
      assertNotNull(method, "createSummary method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("ValidationResult Behavior Tests")
  class ValidationResultBehaviorTests {

    @Test
    @DisplayName("new ValidationResult should be valid")
    void newValidationResultShouldBeValid() {
      final CallerContextValidator.ValidationResult result =
          new CallerContextValidator.ValidationResult();
      assertTrue(result.isValid(), "New ValidationResult should be valid");
    }

    @Test
    @DisplayName("new ValidationResult should have empty lists")
    void newValidationResultShouldHaveEmptyLists() {
      final CallerContextValidator.ValidationResult result =
          new CallerContextValidator.ValidationResult();
      assertTrue(result.getSuccesses().isEmpty(), "Successes should be empty");
      assertTrue(result.getWarnings().isEmpty(), "Warnings should be empty");
      assertTrue(result.getErrors().isEmpty(), "Errors should be empty");
    }

    @Test
    @DisplayName("getSuccesses should return unmodifiable list")
    void getSuccessesShouldReturnUnmodifiableList() {
      final CallerContextValidator.ValidationResult result =
          new CallerContextValidator.ValidationResult();
      final java.util.List<String> successes = result.getSuccesses();
      assertTrue(
          successes.getClass().getName().contains("Unmodifiable"),
          "Successes list should be unmodifiable");
    }

    @Test
    @DisplayName("getWarnings should return unmodifiable list")
    void getWarningsShouldReturnUnmodifiableList() {
      final CallerContextValidator.ValidationResult result =
          new CallerContextValidator.ValidationResult();
      final java.util.List<String> warnings = result.getWarnings();
      assertTrue(
          warnings.getClass().getName().contains("Unmodifiable"),
          "Warnings list should be unmodifiable");
    }

    @Test
    @DisplayName("getErrors should return unmodifiable list")
    void getErrorsShouldReturnUnmodifiableList() {
      final CallerContextValidator.ValidationResult result =
          new CallerContextValidator.ValidationResult();
      final java.util.List<String> errors = result.getErrors();
      assertTrue(
          errors.getClass().getName().contains("Unmodifiable"),
          "Errors list should be unmodifiable");
    }

    @Test
    @DisplayName("createSummary should return non-null string")
    void createSummaryShouldReturnNonNullString() {
      final CallerContextValidator.ValidationResult result =
          new CallerContextValidator.ValidationResult();
      final String summary = result.createSummary();
      assertNotNull(summary, "Summary should not be null");
      assertFalse(summary.isEmpty(), "Summary should not be empty");
    }

    @Test
    @DisplayName("createSummary should contain validation result")
    void createSummaryShouldContainValidationResult() {
      final CallerContextValidator.ValidationResult result =
          new CallerContextValidator.ValidationResult();
      final String summary = result.createSummary();
      assertTrue(summary.contains("Validation"), "Summary should contain 'Validation'");
    }
  }
}
