package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Panama FFI implementation of WIT interface definition.
 *
 * <p>This class provides a concrete implementation of the WitInterfaceDefinition interface
 * for use with Panama FFI-based component operations.
 *
 * @since 1.0.0
 */
public final class PanamaWitInterfaceDefinition implements WitInterfaceDefinition {

  private final String name;
  private final String version;
  private final String packageName;
  private final List<String> functionNames;
  private final List<String> typeNames;
  private final Set<String> dependencies;
  private final List<String> importNames;
  private final List<String> exportNames;

  /**
   * Creates a new Panama WIT interface definition.
   *
   * @param name the interface name
   * @param version the interface version
   * @param packageName the package name
   * @param exportNames the list of export names
   * @param importNames the list of import names
   */
  public PanamaWitInterfaceDefinition(
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
      return WitCompatibilityResult.incompatible("Other interface is null", Set.of());
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
    return "PanamaWitInterfaceDefinition{" +
        "name='" + name + '\'' +
        ", version='" + version + '\'' +
        ", packageName='" + packageName + '\'' +
        ", functionCount=" + functionNames.size() +
        ", typeCount=" + typeNames.size() +
        ", dependencyCount=" + dependencies.size() +
        '}';
  }
}