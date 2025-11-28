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

import java.util.ArrayList;
import java.util.List;

/**
 * Result of validating component imports against a {@link ComponentLinker}.
 *
 * <p>This class provides detailed information about which imports are satisfied and which are
 * missing when attempting to instantiate a component.
 *
 * @since 1.0.0
 */
public final class ComponentImportValidation {

  private final boolean valid;
  private final List<String> satisfiedImports;
  private final List<MissingImport> missingImports;
  private final List<TypeMismatch> typeMismatches;

  private ComponentImportValidation(
      final boolean valid,
      final List<String> satisfiedImports,
      final List<MissingImport> missingImports,
      final List<TypeMismatch> typeMismatches) {
    this.valid = valid;
    this.satisfiedImports = List.copyOf(satisfiedImports);
    this.missingImports = List.copyOf(missingImports);
    this.typeMismatches = List.copyOf(typeMismatches);
  }

  /**
   * Checks if all imports are satisfied.
   *
   * @return true if validation passed
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * Gets the list of satisfied import paths.
   *
   * @return list of satisfied imports in WIT path format
   */
  public List<String> getSatisfiedImports() {
    return satisfiedImports;
  }

  /**
   * Gets the list of missing imports.
   *
   * @return list of missing imports
   */
  public List<MissingImport> getMissingImports() {
    return missingImports;
  }

  /**
   * Gets the list of type mismatches.
   *
   * @return list of type mismatches
   */
  public List<TypeMismatch> getTypeMismatches() {
    return typeMismatches;
  }

  /**
   * Gets a summary message describing the validation result.
   *
   * @return summary message
   */
  public String getSummary() {
    if (valid) {
      return "All " + satisfiedImports.size() + " imports satisfied";
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("Import validation failed: ");

    if (!missingImports.isEmpty()) {
      sb.append(missingImports.size()).append(" missing import(s)");
    }

    if (!typeMismatches.isEmpty()) {
      if (!missingImports.isEmpty()) {
        sb.append(", ");
      }
      sb.append(typeMismatches.size()).append(" type mismatch(es)");
    }

    return sb.toString();
  }

  /**
   * Creates a successful validation result.
   *
   * @param satisfiedImports the satisfied imports
   * @return a valid result
   */
  public static ComponentImportValidation success(final List<String> satisfiedImports) {
    return new ComponentImportValidation(true, satisfiedImports, List.of(), List.of());
  }

  /**
   * Creates a failed validation result.
   *
   * @param satisfiedImports the satisfied imports
   * @param missingImports the missing imports
   * @param typeMismatches the type mismatches
   * @return an invalid result
   */
  public static ComponentImportValidation failure(
      final List<String> satisfiedImports,
      final List<MissingImport> missingImports,
      final List<TypeMismatch> typeMismatches) {
    return new ComponentImportValidation(false, satisfiedImports, missingImports, typeMismatches);
  }

  /**
   * Creates a builder for validation results.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Represents a missing import. */
  public static final class MissingImport {
    private final String interfaceNamespace;
    private final String interfaceName;
    private final String functionName;
    private final String witPath;

    /**
     * Creates a missing import descriptor.
     *
     * @param interfaceNamespace the interface namespace
     * @param interfaceName the interface name
     * @param functionName the function name (may be null for whole interface)
     */
    public MissingImport(
        final String interfaceNamespace, final String interfaceName, final String functionName) {
      this.interfaceNamespace = interfaceNamespace;
      this.interfaceName = interfaceName;
      this.functionName = functionName;
      this.witPath = buildWitPath();
    }

    private String buildWitPath() {
      final StringBuilder sb = new StringBuilder();
      if (interfaceNamespace != null) {
        sb.append(interfaceNamespace).append("/");
      }
      sb.append(interfaceName);
      if (functionName != null) {
        sb.append("#").append(functionName);
      }
      return sb.toString();
    }

    public String getInterfaceNamespace() {
      return interfaceNamespace;
    }

    public String getInterfaceName() {
      return interfaceName;
    }

    public String getFunctionName() {
      return functionName;
    }

    public String getWitPath() {
      return witPath;
    }

    @Override
    public String toString() {
      return witPath;
    }
  }

  /** Represents a type mismatch between expected and provided imports. */
  public static final class TypeMismatch {
    private final String witPath;
    private final String expectedType;
    private final String actualType;
    private final String details;

    /**
     * Creates a type mismatch descriptor.
     *
     * @param witPath the WIT path of the import
     * @param expectedType the expected type
     * @param actualType the actual type provided
     * @param details additional details
     */
    public TypeMismatch(
        final String witPath,
        final String expectedType,
        final String actualType,
        final String details) {
      this.witPath = witPath;
      this.expectedType = expectedType;
      this.actualType = actualType;
      this.details = details;
    }

    public String getWitPath() {
      return witPath;
    }

    public String getExpectedType() {
      return expectedType;
    }

    public String getActualType() {
      return actualType;
    }

    public String getDetails() {
      return details;
    }

    @Override
    public String toString() {
      return witPath + ": expected " + expectedType + ", got " + actualType;
    }
  }

  /** Builder for ComponentImportValidation. */
  public static final class Builder {
    private final List<String> satisfiedImports = new ArrayList<>();
    private final List<MissingImport> missingImports = new ArrayList<>();
    private final List<TypeMismatch> typeMismatches = new ArrayList<>();

    Builder() {}

    /**
     * Adds a satisfied import.
     *
     * @param witPath the WIT path of the satisfied import
     * @return this builder
     */
    public Builder addSatisfied(final String witPath) {
      satisfiedImports.add(witPath);
      return this;
    }

    /**
     * Adds a missing import.
     *
     * @param missing the missing import
     * @return this builder
     */
    public Builder addMissing(final MissingImport missing) {
      missingImports.add(missing);
      return this;
    }

    /**
     * Adds a type mismatch.
     *
     * @param mismatch the type mismatch
     * @return this builder
     */
    public Builder addMismatch(final TypeMismatch mismatch) {
      typeMismatches.add(mismatch);
      return this;
    }

    /**
     * Builds the validation result.
     *
     * @return the validation result
     */
    public ComponentImportValidation build() {
      final boolean valid = missingImports.isEmpty() && typeMismatches.isEmpty();
      return new ComponentImportValidation(valid, satisfiedImports, missingImports, typeMismatches);
    }
  }
}
