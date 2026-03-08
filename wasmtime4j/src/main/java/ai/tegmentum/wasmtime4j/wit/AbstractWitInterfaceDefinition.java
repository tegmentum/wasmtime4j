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
package ai.tegmentum.wasmtime4j.wit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract base class for WIT interface definitions providing shared compatibility logic.
 *
 * <p>This class implements the {@link WitInterfaceDefinition} interface with unified compatibility
 * checking semantics across all runtime implementations. Compatibility is determined by:
 *
 * <ol>
 *   <li>Interface name equality
 *   <li>Major version compatibility (same major version number)
 *   <li>Function and type presence (all functions/types in this interface must exist in the other)
 * </ol>
 *
 * @since 1.0.0
 */
public abstract class AbstractWitInterfaceDefinition implements WitInterfaceDefinition {

  private final String name;
  private final String version;
  private final String packageName;
  private final List<String> functionNames;
  private final List<String> typeNames;
  private final Set<String> dependencies;
  private final List<String> importNames;
  private final List<String> exportNames;

  /**
   * Creates a new abstract WIT interface definition with the given metadata.
   *
   * @param name the interface name, defaults to empty string if null
   * @param version the interface version, defaults to "0.0.0" if null
   * @param packageName the package name, defaults to empty string if null
   * @param functionNames the function names
   * @param typeNames the type names
   * @param dependencies the interface dependencies
   * @param importNames the import names
   * @param exportNames the export names
   */
  protected AbstractWitInterfaceDefinition(
      final String name,
      final String version,
      final String packageName,
      final List<String> functionNames,
      final List<String> typeNames,
      final Set<String> dependencies,
      final List<String> importNames,
      final List<String> exportNames) {
    this.name = name != null ? name : "";
    this.version = version != null ? version : "0.0.0";
    this.packageName = packageName != null ? packageName : "";
    this.functionNames =
        new ArrayList<>(functionNames != null ? functionNames : Collections.emptyList());
    this.typeNames = new ArrayList<>(typeNames != null ? typeNames : Collections.emptyList());
    this.dependencies = new HashSet<>(dependencies != null ? dependencies : Collections.emptySet());
    this.importNames = new ArrayList<>(importNames != null ? importNames : Collections.emptyList());
    this.exportNames = new ArrayList<>(exportNames != null ? exportNames : Collections.emptyList());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public String getPackageName() {
    return packageName;
  }

  @Override
  public List<String> getFunctionNames() {
    return Collections.unmodifiableList(functionNames);
  }

  @Override
  public List<String> getTypeNames() {
    return Collections.unmodifiableList(typeNames);
  }

  @Override
  public Set<String> getDependencies() {
    return Collections.unmodifiableSet(dependencies);
  }

  @Override
  public List<String> getImportNames() {
    return Collections.unmodifiableList(importNames);
  }

  @Override
  public List<String> getExportNames() {
    return Collections.unmodifiableList(exportNames);
  }

  @Override
  public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
    if (other == null) {
      return WitCompatibilityResult.incompatible("Other interface is null", Collections.emptySet());
    }

    if (!name.equals(other.getName())) {
      return WitCompatibilityResult.incompatible(
          "Interface names do not match: " + name + " vs " + other.getName(),
          Collections.emptySet());
    }

    final String[] thisVersionParts = version.split("\\.");
    final String[] otherVersionParts = other.getVersion().split("\\.");

    if (thisVersionParts.length >= 1 && otherVersionParts.length >= 1) {
      try {
        final int thisMajor = Integer.parseInt(thisVersionParts[0]);
        final int otherMajor = Integer.parseInt(otherVersionParts[0]);
        if (thisMajor != otherMajor) {
          return WitCompatibilityResult.incompatible(
              "Major version mismatch: " + version + " vs " + other.getVersion(),
              Collections.emptySet());
        }
      } catch (final NumberFormatException e) {
        // Non-numeric version, skip major version check
      }
    }

    final Set<String> missingFunctions = new HashSet<>();
    for (final String func : functionNames) {
      if (!other.getFunctionNames().contains(func)) {
        missingFunctions.add("function:" + func);
      }
    }

    final Set<String> missingTypes = new HashSet<>();
    for (final String type : typeNames) {
      if (!other.getTypeNames().contains(type)) {
        missingTypes.add("type:" + type);
      }
    }

    if (!missingFunctions.isEmpty() || !missingTypes.isEmpty()) {
      final Set<String> allMissing = new HashSet<>();
      allMissing.addAll(missingFunctions);
      allMissing.addAll(missingTypes);
      return WitCompatibilityResult.incompatible(
          "Missing elements in target interface", allMissing);
    }

    // Collect satisfied imports
    final Set<String> satisfied = new HashSet<>();
    satisfied.addAll(functionNames);
    satisfied.addAll(typeNames);

    return WitCompatibilityResult.compatible("Interfaces are compatible", satisfied);
  }

  /**
   * Provides mutable access to the function names list for subclass construction.
   *
   * @return the mutable function names list
   */
  protected List<String> mutableFunctionNames() {
    return functionNames;
  }

  /**
   * Provides mutable access to the type names list for subclass construction.
   *
   * @return the mutable type names list
   */
  protected List<String> mutableTypeNames() {
    return typeNames;
  }

  /**
   * Provides mutable access to the dependencies set for subclass construction.
   *
   * @return the mutable dependencies set
   */
  protected Set<String> mutableDependencies() {
    return dependencies;
  }

  /**
   * Provides mutable access to the import names list for subclass construction.
   *
   * @return the mutable import names list
   */
  protected List<String> mutableImportNames() {
    return importNames;
  }

  /**
   * Provides mutable access to the export names list for subclass construction.
   *
   * @return the mutable export names list
   */
  protected List<String> mutableExportNames() {
    return exportNames;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{name='"
        + name
        + "', version='"
        + version
        + "', packageName='"
        + packageName
        + "', functionCount="
        + functionNames.size()
        + ", typeCount="
        + typeNames.size()
        + ", dependencyCount="
        + dependencies.size()
        + '}';
  }
}
