/*
 * Copyright 2024 Tegmentum AI
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

import ai.tegmentum.wasmtime4j.exception.ValidationException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validator for WebAssembly Interface Type (WIT) definitions.
 *
 * <p>This class provides comprehensive validation of WIT types, interfaces, and function signatures
 * to ensure type safety and conformance to the WIT specification.
 *
 * @since 1.0.0
 */
public final class WitTypeValidator {

  private static final int MAX_RECURSION_DEPTH = 100;
  private static final int MAX_RECORD_FIELDS = 1000;
  private static final int MAX_VARIANT_CASES = 1000;
  private static final int MAX_ENUM_VALUES = 1000;
  private static final int MAX_FLAGS = 64; // Due to implementation constraints

  /**
   * Validates a WIT type definition.
   *
   * @param type the type to validate
   * @return validation result
   * @throws ValidationException if validation fails
   */
  public WitTypeValidationResult validateType(final WitType type) throws ValidationException {
    Objects.requireNonNull(type, "type");

    final List<String> errors = new ArrayList<>();
    final List<String> warnings = new ArrayList<>();
    final Set<String> visitedTypes = new HashSet<>();

    validateTypeRecursive(type, errors, warnings, visitedTypes, 0);

    final boolean valid = errors.isEmpty();
    return new WitTypeValidationResult(valid, errors, warnings);
  }

  /**
   * Validates a WIT interface definition.
   *
   * @param interfaceDefinition the interface to validate
   * @return validation result
   * @throws ValidationException if validation fails
   */
  public WitInterfaceValidationResult validateInterface(
      final WitInterfaceDefinition interfaceDefinition) throws ValidationException {
    Objects.requireNonNull(interfaceDefinition, "interfaceDefinition");

    final List<String> errors = new ArrayList<>();
    final List<String> warnings = new ArrayList<>();

    // Validate interface metadata
    validateInterfaceMetadata(interfaceDefinition, errors, warnings);

    // Validate function names and signatures
    validateFunctions(interfaceDefinition, errors, warnings);

    // Validate type definitions
    validateInterfaceTypes(interfaceDefinition, errors, warnings);

    // Validate imports and exports
    validateImportsExports(interfaceDefinition, errors, warnings);

    final boolean valid = errors.isEmpty();
    return new WitInterfaceValidationResult(valid, errors, warnings);
  }

  /**
   * Validates type compatibility between two WIT types.
   *
   * @param sourceType the source type
   * @param targetType the target type
   * @return compatibility result
   */
  public WitTypeCompatibilityResult validateTypeCompatibility(
      final WitType sourceType, final WitType targetType) {
    Objects.requireNonNull(sourceType, "sourceType");
    Objects.requireNonNull(targetType, "targetType");

    final List<String> issues = new ArrayList<>();
    final boolean compatible = checkTypeCompatibility(sourceType, targetType, issues);

    return new WitTypeCompatibilityResult(
        compatible, compatible ? "Types are compatible" : String.join("; ", issues), issues);
  }

  /**
   * Recursively validates a type definition.
   *
   * @param type the type to validate
   * @param errors the error list
   * @param warnings the warning list
   * @param visitedTypes types already visited (for cycle detection)
   * @param depth current recursion depth
   */
  private void validateTypeRecursive(
      final WitType type,
      final List<String> errors,
      final List<String> warnings,
      final Set<String> visitedTypes,
      final int depth) {

    // Check recursion depth
    if (depth > MAX_RECURSION_DEPTH) {
      errors.add("Type definition exceeds maximum recursion depth: " + MAX_RECURSION_DEPTH);
      return;
    }

    // Check for cycles
    if (visitedTypes.contains(type.getName())) {
      warnings.add("Potential cycle detected in type definition: " + type.getName());
      return;
    }

    visitedTypes.add(type.getName());

    // Validate based on type category
    final WitTypeKind kind = type.getKind();
    switch (kind.getCategory()) {
      case PRIMITIVE:
        validatePrimitiveType(type, errors, warnings);
        break;
      case RECORD:
        validateRecordType(type, errors, warnings, visitedTypes, depth);
        break;
      case VARIANT:
        validateVariantType(type, errors, warnings, visitedTypes, depth);
        break;
      case ENUM:
        validateEnumType(type, errors, warnings);
        break;
      case FLAGS:
        validateFlagsType(type, errors, warnings);
        break;
      case LIST:
        validateListType(type, errors, warnings, visitedTypes, depth);
        break;
      case OPTION:
        validateOptionType(type, errors, warnings, visitedTypes, depth);
        break;
      case RESULT:
        validateResultType(type, errors, warnings, visitedTypes, depth);
        break;
      case RESOURCE:
        validateResourceType(type, errors, warnings);
        break;
      default:
        errors.add("Unknown type category: " + kind.getCategory());
    }

    visitedTypes.remove(type.getName());
  }

  /**
   * Validates a primitive type.
   *
   * @param type the type to validate
   * @param errors the error list
   * @param warnings the warning list
   */
  private void validatePrimitiveType(
      final WitType type, final List<String> errors, final List<String> warnings) {
    // Primitive types are always valid by construction
    if (!type.isPrimitive()) {
      errors.add("Type marked as primitive but is not primitive: " + type.getName());
    }
  }

  /**
   * Validates a record type.
   *
   * @param type the type to validate
   * @param errors the error list
   * @param warnings the warning list
   * @param visitedTypes visited types set
   * @param depth current recursion depth
   */
  private void validateRecordType(
      final WitType type,
      final List<String> errors,
      final List<String> warnings,
      final Set<String> visitedTypes,
      final int depth) {

    final WitTypeKind kind = type.getKind();
    if (kind instanceof WitTypeKind) {
      // Access record fields through reflection or provide accessor methods
      // For now, validate that the type is properly constructed
      if (type.getMetadata().containsKey("fieldCount")) {
        final int fieldCount = (Integer) type.getMetadata().get("fieldCount");
        if (fieldCount > MAX_RECORD_FIELDS) {
          errors.add(
              "Record has too many fields: " + fieldCount + " (max: " + MAX_RECORD_FIELDS + ")");
        }
        if (fieldCount == 0) {
          warnings.add("Record has no fields: " + type.getName());
        }
      }
    }
  }

  /**
   * Validates a variant type.
   *
   * @param type the type to validate
   * @param errors the error list
   * @param warnings the warning list
   * @param visitedTypes visited types set
   * @param depth current recursion depth
   */
  private void validateVariantType(
      final WitType type,
      final List<String> errors,
      final List<String> warnings,
      final Set<String> visitedTypes,
      final int depth) {

    if (type.getMetadata().containsKey("caseCount")) {
      final int caseCount = (Integer) type.getMetadata().get("caseCount");
      if (caseCount > MAX_VARIANT_CASES) {
        errors.add(
            "Variant has too many cases: " + caseCount + " (max: " + MAX_VARIANT_CASES + ")");
      }
      if (caseCount == 0) {
        errors.add("Variant must have at least one case: " + type.getName());
      }
    }
  }

  /**
   * Validates an enum type.
   *
   * @param type the type to validate
   * @param errors the error list
   * @param warnings the warning list
   */
  private void validateEnumType(
      final WitType type, final List<String> errors, final List<String> warnings) {

    if (type.getMetadata().containsKey("valueCount")) {
      final int valueCount = (Integer) type.getMetadata().get("valueCount");
      if (valueCount > MAX_ENUM_VALUES) {
        errors.add("Enum has too many values: " + valueCount + " (max: " + MAX_ENUM_VALUES + ")");
      }
      if (valueCount == 0) {
        errors.add("Enum must have at least one value: " + type.getName());
      }
    }
  }

  /**
   * Validates a flags type.
   *
   * @param type the type to validate
   * @param errors the error list
   * @param warnings the warning list
   */
  private void validateFlagsType(
      final WitType type, final List<String> errors, final List<String> warnings) {

    if (type.getMetadata().containsKey("flagCount")) {
      final int flagCount = (Integer) type.getMetadata().get("flagCount");
      if (flagCount > MAX_FLAGS) {
        errors.add("Flags has too many flags: " + flagCount + " (max: " + MAX_FLAGS + ")");
      }
      if (flagCount == 0) {
        warnings.add("Flags type has no flags defined: " + type.getName());
      }
    }
  }

  /**
   * Validates a list type.
   *
   * @param type the type to validate
   * @param errors the error list
   * @param warnings the warning list
   * @param visitedTypes visited types set
   * @param depth current recursion depth
   */
  private void validateListType(
      final WitType type,
      final List<String> errors,
      final List<String> warnings,
      final Set<String> visitedTypes,
      final int depth) {

    // List element type validation would require access to element type
    // This is simplified for now
    if (!type.getName().startsWith("list<")) {
      errors.add("List type name should start with 'list<': " + type.getName());
    }
  }

  /**
   * Validates an option type.
   *
   * @param type the type to validate
   * @param errors the error list
   * @param warnings the warning list
   * @param visitedTypes visited types set
   * @param depth current recursion depth
   */
  private void validateOptionType(
      final WitType type,
      final List<String> errors,
      final List<String> warnings,
      final Set<String> visitedTypes,
      final int depth) {

    if (!type.getName().startsWith("option<")) {
      errors.add("Option type name should start with 'option<': " + type.getName());
    }
  }

  /**
   * Validates a result type.
   *
   * @param type the type to validate
   * @param errors the error list
   * @param warnings the warning list
   * @param visitedTypes visited types set
   * @param depth current recursion depth
   */
  private void validateResultType(
      final WitType type,
      final List<String> errors,
      final List<String> warnings,
      final Set<String> visitedTypes,
      final int depth) {

    if (!type.getName().startsWith("result")) {
      errors.add("Result type name should start with 'result': " + type.getName());
    }
  }

  /**
   * Validates a resource type.
   *
   * @param type the type to validate
   * @param errors the error list
   * @param warnings the warning list
   */
  private void validateResourceType(
      final WitType type, final List<String> errors, final List<String> warnings) {

    if (!type.getMetadata().containsKey("resourceId")) {
      errors.add("Resource type must have a resource ID: " + type.getName());
    }
  }

  /**
   * Validates interface metadata.
   *
   * @param interfaceDefinition the interface to validate
   * @param errors the error list
   * @param warnings the warning list
   */
  private void validateInterfaceMetadata(
      final WitInterfaceDefinition interfaceDefinition,
      final List<String> errors,
      final List<String> warnings) {

    final String name = interfaceDefinition.getName();
    if (name == null || name.trim().isEmpty()) {
      errors.add("Interface name cannot be null or empty");
    }

    final String version = interfaceDefinition.getVersion();
    if (version == null || version.trim().isEmpty()) {
      warnings.add("Interface version is not specified");
    }

    final String packageName = interfaceDefinition.getPackageName();
    if (packageName == null || packageName.trim().isEmpty()) {
      warnings.add("Interface package name is not specified");
    }
  }

  /**
   * Validates interface functions.
   *
   * @param interfaceDefinition the interface to validate
   * @param errors the error list
   * @param warnings the warning list
   */
  private void validateFunctions(
      final WitInterfaceDefinition interfaceDefinition,
      final List<String> errors,
      final List<String> warnings) {

    final List<String> functionNames = interfaceDefinition.getFunctionNames();
    final Set<String> uniqueNames = new HashSet<>();

    for (final String functionName : functionNames) {
      if (functionName == null || functionName.trim().isEmpty()) {
        errors.add("Function name cannot be null or empty");
        continue;
      }

      if (!uniqueNames.add(functionName)) {
        errors.add("Duplicate function name: " + functionName);
      }
    }

    if (functionNames.isEmpty()) {
      warnings.add("Interface has no functions defined");
    }
  }

  /**
   * Validates interface type definitions.
   *
   * @param interfaceDefinition the interface to validate
   * @param errors the error list
   * @param warnings the warning list
   */
  private void validateInterfaceTypes(
      final WitInterfaceDefinition interfaceDefinition,
      final List<String> errors,
      final List<String> warnings) {

    final List<String> typeNames = interfaceDefinition.getTypeNames();
    final Set<String> uniqueNames = new HashSet<>();

    for (final String typeName : typeNames) {
      if (typeName == null || typeName.trim().isEmpty()) {
        errors.add("Type name cannot be null or empty");
        continue;
      }

      if (!uniqueNames.add(typeName)) {
        errors.add("Duplicate type name: " + typeName);
      }
    }
  }

  /**
   * Validates interface imports and exports.
   *
   * @param interfaceDefinition the interface to validate
   * @param errors the error list
   * @param warnings the warning list
   */
  private void validateImportsExports(
      final WitInterfaceDefinition interfaceDefinition,
      final List<String> errors,
      final List<String> warnings) {

    final List<String> imports = interfaceDefinition.getImportNames();
    final List<String> exports = interfaceDefinition.getExportNames();

    if (imports.isEmpty() && exports.isEmpty()) {
      warnings.add("Interface has no imports or exports");
    }
  }

  /**
   * Checks type compatibility between two types.
   *
   * @param sourceType the source type
   * @param targetType the target type
   * @param issues list to collect issues
   * @return true if compatible, false otherwise
   */
  private boolean checkTypeCompatibility(
      final WitType sourceType, final WitType targetType, final List<String> issues) {

    // Direct compatibility check
    if (sourceType.isCompatibleWith(targetType)) {
      return true;
    }

    // Detailed compatibility analysis
    if (!sourceType.getKind().getCategory().equals(targetType.getKind().getCategory())) {
      issues.add(
          "Type categories differ: "
              + sourceType.getKind().getCategory()
              + " vs "
              + targetType.getKind().getCategory());
      return false;
    }

    // Size compatibility for primitive types
    if (sourceType.isPrimitive() && targetType.isPrimitive()) {
      final int sourceSize = sourceType.getSizeBytes().orElse(-1);
      final int targetSize = targetType.getSizeBytes().orElse(-1);
      if (sourceSize != targetSize) {
        issues.add("Type sizes differ: " + sourceSize + " vs " + targetSize);
        return false;
      }
    }

    return true;
  }

  /** WIT type validation result. */
  public static final class WitTypeValidationResult {
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    /**
     * Creates a new WIT type validation result.
     *
     * @param valid whether the type validation passed
     * @param errors list of validation errors
     * @param warnings list of validation warnings
     */
    public WitTypeValidationResult(
        final boolean valid, final List<String> errors, final List<String> warnings) {
      this.valid = valid;
      this.errors = List.copyOf(errors);
      this.warnings = List.copyOf(warnings);
    }

    public boolean isValid() {
      return valid;
    }

    public List<String> getErrors() {
      return errors;
    }

    public List<String> getWarnings() {
      return warnings;
    }
  }

  /** WIT interface validation result. */
  public static final class WitInterfaceValidationResult {
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    /**
     * Creates a new WIT interface validation result.
     *
     * @param valid whether the interface validation passed
     * @param errors list of validation errors
     * @param warnings list of validation warnings
     */
    public WitInterfaceValidationResult(
        final boolean valid, final List<String> errors, final List<String> warnings) {
      this.valid = valid;
      this.errors = List.copyOf(errors);
      this.warnings = List.copyOf(warnings);
    }

    public boolean isValid() {
      return valid;
    }

    public List<String> getErrors() {
      return errors;
    }

    public List<String> getWarnings() {
      return warnings;
    }
  }

  /** WIT type compatibility result. */
  public static final class WitTypeCompatibilityResult {
    private final boolean compatible;
    private final String message;
    private final List<String> issues;

    /**
     * Creates a new WIT type compatibility result.
     *
     * @param compatible whether the types are compatible
     * @param message compatibility message or description
     * @param issues list of compatibility issues
     */
    public WitTypeCompatibilityResult(
        final boolean compatible, final String message, final List<String> issues) {
      this.compatible = compatible;
      this.message = message;
      this.issues = List.copyOf(issues);
    }

    public boolean isCompatible() {
      return compatible;
    }

    public String getMessage() {
      return message;
    }

    public List<String> getIssues() {
      return issues;
    }
  }
}
