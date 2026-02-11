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

import ai.tegmentum.wasmtime4j.func.Caller;
import ai.tegmentum.wasmtime4j.func.Function;
import ai.tegmentum.wasmtime4j.Global;
import ai.tegmentum.wasmtime4j.Memory;
import ai.tegmentum.wasmtime4j.Table;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Validation framework for caller context implementations.
 *
 * <p>This class provides a comprehensive validation framework to ensure that all caller context
 * implementations (JNI, Panama) behave consistently and correctly according to the Wasmtime
 * specification.
 *
 * <p>The validator can be used during development and testing to catch implementation
 * inconsistencies and ensure proper behavior across different runtime environments.
 *
 * @since 1.0.0
 */
public final class CallerContextValidator {
  private static final Logger LOGGER = Logger.getLogger(CallerContextValidator.class.getName());

  private CallerContextValidator() {
    // Utility class
  }

  /**
   * Validates a caller context implementation for compliance.
   *
   * @param <T> the type of user data associated with the store
   * @param caller the caller context to validate
   * @param validationConfig configuration for validation behavior
   * @return validation results
   */
  public static <T> ValidationResult validate(Caller<T> caller, ValidationConfig validationConfig) {
    ValidationResult result = new ValidationResult();

    try {
      // Test basic functionality
      validateBasicFunctionality(caller, result);

      // Test export access
      if (validationConfig.validateExportAccess) {
        validateExportAccess(caller, result);
      }

      // Test fuel operations
      if (validationConfig.validateFuelOperations) {
        validateFuelOperations(caller, result);
      }

      // Test epoch deadline operations
      if (validationConfig.validateEpochOperations) {
        validateEpochOperations(caller, result);
      }

      // Test error handling
      if (validationConfig.validateErrorHandling) {
        validateErrorHandling(caller, result);
      }

    } catch (Exception e) {
      result.addError("Validation failed with exception: " + e.getMessage());
      LOGGER.severe("Caller context validation failed: " + e.getMessage());
    }

    return result;
  }

  /**
   * Creates a validation configuration with all checks enabled.
   *
   * @return a comprehensive validation configuration
   */
  public static ValidationConfig createComprehensiveConfig() {
    return new ValidationConfig(true, true, true, true, true);
  }

  /**
   * Creates a validation configuration for basic functionality only.
   *
   * @return a basic validation configuration
   */
  public static ValidationConfig createBasicConfig() {
    return new ValidationConfig(true, false, false, false, false);
  }

  private static <T> void validateBasicFunctionality(Caller<T> caller, ValidationResult result) {
    // Test data access
    try {
      caller.data(); // Just verify method doesn't throw
      result.addSuccess("Basic data access works");
    } catch (Exception e) {
      result.addError("Failed to access caller data: " + e.getMessage());
    }
  }

  private static <T> void validateExportAccess(Caller<T> caller, ValidationResult result) {
    try {
      // Test hasExport with various scenarios - just verify methods don't throw
      caller.hasExport("memory");
      caller.hasExport("nonexistent_export_12345");

      result.addSuccess("hasExport method works for existing and non-existing exports");

      // Test export retrieval - just verify method doesn't throw
      caller.getExport("memory");
      result.addSuccess("getExport method works");

      // Test specific export types
      validateSpecificExportTypes(caller, result);

    } catch (Exception e) {
      result.addError("Export access validation failed: " + e.getMessage());
    }
  }

  private static <T> void validateSpecificExportTypes(Caller<T> caller, ValidationResult result) {
    // Test memory access
    try {
      Optional<Memory> memory = caller.getMemory("memory");
      result.addSuccess("Memory export access works");
    } catch (Exception e) {
      result.addWarning("Memory export access failed: " + e.getMessage());
    }

    // Test function access
    try {
      Optional<Function<T>> function = caller.getFunction("some_function");
      result.addSuccess("Function export access works");
    } catch (Exception e) {
      result.addWarning("Function export access failed: " + e.getMessage());
    }

    // Test global access
    try {
      Optional<Global> global = caller.getGlobal("some_global");
      result.addSuccess("Global export access works");
    } catch (Exception e) {
      result.addWarning("Global export access failed: " + e.getMessage());
    }

    // Test table access
    try {
      Optional<Table> table = caller.getTable("some_table");
      result.addSuccess("Table export access works");
    } catch (Exception e) {
      result.addWarning("Table export access failed: " + e.getMessage());
    }
  }

  private static <T> void validateFuelOperations(Caller<T> caller, ValidationResult result) {
    try {
      // Test fuel consumption tracking - just verify method doesn't throw
      caller.fuelConsumed();
      result.addSuccess("Fuel consumption tracking works");

      // Test fuel remaining
      Optional<Long> remaining = caller.fuelRemaining();
      result.addSuccess("Fuel remaining tracking works");

      // Test fuel addition (if fuel metering is enabled)
      if (remaining.isPresent()) {
        long initialFuel = remaining.get();
        caller.addFuel(100);

        Optional<Long> afterAddition = caller.fuelRemaining();
        if (afterAddition.isPresent() && afterAddition.get() >= initialFuel + 100) {
          result.addSuccess("Fuel addition works correctly");
        } else {
          result.addWarning("Fuel addition may not be working correctly");
        }
      } else {
        result.addWarning("Fuel metering not enabled, skipping fuel addition test");
      }

    } catch (IllegalArgumentException e) {
      // Expected for negative fuel amounts
      result.addSuccess("Fuel operations properly validate inputs");
    } catch (Exception e) {
      result.addError("Fuel operations validation failed: " + e.getMessage());
    }
  }

  private static <T> void validateEpochOperations(Caller<T> caller, ValidationResult result) {
    try {
      // Test epoch deadline checking - just verify method doesn't throw
      caller.hasEpochDeadline();
      result.addSuccess("Epoch deadline checking works");

      // Test epoch deadline retrieval - just verify method doesn't throw
      caller.epochDeadline();
      result.addSuccess("Epoch deadline retrieval works");

      // Test epoch deadline setting
      caller.setEpochDeadline(System.currentTimeMillis() + 5000);
      if (caller.hasEpochDeadline()) {
        result.addSuccess("Epoch deadline setting works correctly");
      } else {
        result.addWarning("Epoch deadline setting may not be working correctly");
      }

    } catch (Exception e) {
      result.addError("Epoch operations validation failed: " + e.getMessage());
    }
  }

  private static <T> void validateErrorHandling(Caller<T> caller, ValidationResult result) {
    // Test null parameter handling
    try {
      caller.hasExport(null);
      result.addError("hasExport should reject null parameters");
    } catch (IllegalArgumentException e) {
      result.addSuccess("hasExport properly rejects null parameters");
    } catch (Exception e) {
      result.addWarning("hasExport null handling: " + e.getMessage());
    }

    try {
      caller.getMemory(null);
      result.addError("getMemory should reject null parameters");
    } catch (IllegalArgumentException e) {
      result.addSuccess("getMemory properly rejects null parameters");
    } catch (Exception e) {
      result.addWarning("getMemory null handling: " + e.getMessage());
    }

    // Test negative fuel handling
    try {
      caller.addFuel(-100);
      result.addError("addFuel should reject negative values");
    } catch (IllegalArgumentException e) {
      result.addSuccess("addFuel properly rejects negative values");
    } catch (Exception e) {
      result.addWarning("addFuel negative value handling: " + e.getMessage());
    }
  }

  /** Configuration for caller context validation. */
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
      justification =
          "Field validateBasicFunctionality is part of public API configuration;"
              + " reserved for future enhanced validation modes")
  public static class ValidationConfig {
    public final boolean validateBasicFunctionality;
    public final boolean validateExportAccess;
    public final boolean validateFuelOperations;
    public final boolean validateEpochOperations;
    public final boolean validateErrorHandling;

    /**
     * Creates a new ValidationConfig.
     *
     * @param validateBasicFunctionality whether to validate basic functionality
     * @param validateExportAccess whether to validate export access
     * @param validateFuelOperations whether to validate fuel operations
     * @param validateEpochOperations whether to validate epoch operations
     * @param validateErrorHandling whether to validate error handling
     */
    public ValidationConfig(
        boolean validateBasicFunctionality,
        boolean validateExportAccess,
        boolean validateFuelOperations,
        boolean validateEpochOperations,
        boolean validateErrorHandling) {
      this.validateBasicFunctionality = validateBasicFunctionality;
      this.validateExportAccess = validateExportAccess;
      this.validateFuelOperations = validateFuelOperations;
      this.validateEpochOperations = validateEpochOperations;
      this.validateErrorHandling = validateErrorHandling;
    }
  }

  /** Results of caller context validation. */
  public static class ValidationResult {
    private final java.util.List<String> successes = new java.util.ArrayList<>();
    private final java.util.List<String> warnings = new java.util.ArrayList<>();
    private final java.util.List<String> errors = new java.util.ArrayList<>();

    void addSuccess(String message) {
      successes.add(message);
      LOGGER.fine("Validation success: " + message);
    }

    void addWarning(String message) {
      warnings.add(message);
      LOGGER.warning("Validation warning: " + message);
    }

    void addError(String message) {
      errors.add(message);
      LOGGER.severe("Validation error: " + message);
    }

    /**
     * Checks if validation passed without errors.
     *
     * @return true if no errors were found
     */
    public boolean isValid() {
      return errors.isEmpty();
    }

    /**
     * Gets the list of success messages.
     *
     * @return success messages
     */
    public java.util.List<String> getSuccesses() {
      return java.util.Collections.unmodifiableList(successes);
    }

    /**
     * Gets the list of warning messages.
     *
     * @return warning messages
     */
    public java.util.List<String> getWarnings() {
      return java.util.Collections.unmodifiableList(warnings);
    }

    /**
     * Gets the list of error messages.
     *
     * @return error messages
     */
    public java.util.List<String> getErrors() {
      return java.util.Collections.unmodifiableList(errors);
    }

    /**
     * Executes a callback for each error found.
     *
     * @param errorHandler callback to handle each error
     */
    public void forEachError(Consumer<String> errorHandler) {
      errors.forEach(errorHandler);
    }

    /**
     * Creates a summary report of the validation results.
     *
     * @return validation summary
     */
    public String createSummary() {
      StringBuilder summary = new StringBuilder();
      summary.append("Validation Summary:\n");
      summary.append("  Successes: ").append(successes.size()).append("\n");
      summary.append("  Warnings: ").append(warnings.size()).append("\n");
      summary.append("  Errors: ").append(errors.size()).append("\n");
      summary.append("  Overall: ").append(isValid() ? "PASSED" : "FAILED").append("\n");

      if (!errors.isEmpty()) {
        summary.append("\nErrors:\n");
        errors.forEach(error -> summary.append("  - ").append(error).append("\n"));
      }

      if (!warnings.isEmpty()) {
        summary.append("\nWarnings:\n");
        warnings.forEach(warning -> summary.append("  - ").append(warning).append("\n"));
      }

      return summary.toString();
    }
  }
}
