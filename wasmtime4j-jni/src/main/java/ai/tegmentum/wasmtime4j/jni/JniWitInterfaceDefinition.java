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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JNI implementation of WIT interface definition.
 *
 * <p>This class provides a concrete implementation of the WitInterfaceDefinition interface for use
 * with JNI-based component operations.
 *
 * @since 1.0.0
 */
public final class JniWitInterfaceDefinition implements WitInterfaceDefinition {

  private final String name;
  private final String version;
  private final String packageName;
  private final List<String> functionNames;
  private final List<String> typeNames;
  private final Set<String> dependencies;
  private final List<String> importNames;
  private final List<String> exportNames;

  /**
   * Creates a new JNI WIT interface definition.
   *
   * @param name the interface name
   * @param version the interface version
   * @param packageName the package name
   * @param exportNames the list of export names
   * @param importNames the list of import names
   */
  public JniWitInterfaceDefinition(
      final String name,
      final String version,
      final String packageName,
      final Set<String> exportNames,
      final Set<String> importNames) {
    this.name = name != null ? name : "unknown";
    this.version = version != null ? version : "1.0.0";
    this.packageName = packageName != null ? packageName : "unknown";
    this.exportNames = new ArrayList<>(exportNames != null ? exportNames : Collections.emptySet());
    this.importNames = new ArrayList<>(importNames != null ? importNames : Collections.emptySet());

    // Generate basic function and type names from exports/imports
    this.functionNames = new ArrayList<>();
    this.typeNames = new ArrayList<>();
    this.dependencies = new HashSet<>();

    // Add placeholder functions based on exports
    for (final String export : this.exportNames) {
      this.functionNames.add(export + "-func");
      this.typeNames.add(export + "-type");
    }

    // Add dependencies based on imports
    for (final String import_ : this.importNames) {
      this.dependencies.add(import_);
    }
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
  public WitCompatibilityResult isCompatibleWith(final WitInterfaceDefinition other) {
    if (other == null) {
      return WitCompatibilityResult.incompatible("Other interface is null", Collections.emptySet());
    }

    // Basic compatibility check - same package and compatible version
    final boolean samePackage = this.packageName.equals(other.getPackageName());
    final boolean compatibleVersion = this.version.equals(other.getVersion());

    if (samePackage && compatibleVersion) {
      return WitCompatibilityResult.compatible("Interfaces are compatible", this.dependencies);
    } else {
      final Set<String> unsatisfied = new HashSet<>();
      if (!samePackage) {
        unsatisfied.add("package-mismatch");
      }
      if (!compatibleVersion) {
        unsatisfied.add("version-mismatch");
      }
      return WitCompatibilityResult.incompatible("Interfaces are not compatible", unsatisfied);
    }
  }

  @Override
  public String getWitText() {
    final StringBuilder wit = new StringBuilder();
    wit.append("interface ").append(name).append(" {\n");

    for (final String func : functionNames) {
      wit.append("  ").append(func).append("() -> ();\n");
    }

    for (final String type : typeNames) {
      wit.append("  type ").append(type).append(" = string;\n");
    }

    wit.append("}\n");
    return wit.toString();
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
  public String toString() {
    return "JniWitInterfaceDefinition{"
        + "name='"
        + name
        + '\''
        + ", version='"
        + version
        + '\''
        + ", packageName='"
        + packageName
        + '\''
        + ", functionCount="
        + functionNames.size()
        + ", typeCount="
        + typeNames.size()
        + ", dependencyCount="
        + dependencies.size()
        + '}';
  }
}
