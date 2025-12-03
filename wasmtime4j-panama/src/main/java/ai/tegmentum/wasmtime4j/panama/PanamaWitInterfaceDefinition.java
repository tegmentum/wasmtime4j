package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Panama implementation of WIT interface definition.
 *
 * @since 1.0.0
 */
final class PanamaWitInterfaceDefinition implements WitInterfaceDefinition {

  private final String name;
  private final String version;
  private final String packageName;
  private final List<String> functionNames;
  private final List<String> typeNames;
  private final Set<String> dependencies;
  private final String witText;
  private final List<String> importNames;
  private final List<String> exportNames;

  /**
   * Creates a new PanamaWitInterfaceDefinition.
   *
   * @param name the interface name
   * @param version the interface version
   * @param packageName the package name
   * @param witText the raw WIT definition text
   */
  PanamaWitInterfaceDefinition(
      final String name, final String version, final String packageName, final String witText) {
    this.name = name != null ? name : "";
    this.version = version != null ? version : "0.0.0";
    this.packageName = packageName != null ? packageName : "";
    this.witText = witText != null ? witText : "";
    this.functionNames = new ArrayList<>();
    this.typeNames = new ArrayList<>();
    this.dependencies = new HashSet<>();
    this.importNames = new ArrayList<>();
    this.exportNames = new ArrayList<>();

    if (witText != null && !witText.isEmpty()) {
      parseWitText(witText);
    }
  }

  private void parseWitText(final String text) {
    for (final String line : text.split("\n")) {
      final String trimmed = line.trim();

      if (trimmed.contains(": func(") || trimmed.matches("^[a-z][a-z0-9-]*\\s*:\\s*func.*")) {
        final int colonIdx = trimmed.indexOf(':');
        if (colonIdx > 0) {
          functionNames.add(trimmed.substring(0, colonIdx).trim());
        }
      }

      if (trimmed.startsWith("type ")) {
        final String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
          typeNames.add(parts[1]);
        }
      }

      if (trimmed.startsWith("record ")) {
        final String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
          typeNames.add(parts[1].replace("{", "").trim());
        }
      }

      if (trimmed.startsWith("enum ")) {
        final String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
          typeNames.add(parts[1].replace("{", "").trim());
        }
      }

      if (trimmed.startsWith("variant ")) {
        final String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
          typeNames.add(parts[1].replace("{", "").trim());
        }
      }

      if (trimmed.startsWith("use ")) {
        dependencies.add(trimmed.substring(4).replace(";", "").trim());
      }

      if (trimmed.startsWith("import ")) {
        importNames.add(trimmed.substring(7).replace(";", "").trim());
      }

      if (trimmed.startsWith("export ")) {
        exportNames.add(trimmed.substring(7).replace(";", "").trim());
      }
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

    if (!name.equals(other.getName())) {
      return WitCompatibilityResult.incompatible(
          "Interface names do not match: " + name + " vs " + other.getName(), Set.of());
    }

    final String[] thisVersionParts = version.split("\\.");
    final String[] otherVersionParts = other.getVersion().split("\\.");

    if (thisVersionParts.length >= 1 && otherVersionParts.length >= 1) {
      try {
        final int thisMajor = Integer.parseInt(thisVersionParts[0]);
        final int otherMajor = Integer.parseInt(otherVersionParts[0]);
        if (thisMajor != otherMajor) {
          return WitCompatibilityResult.incompatible(
              "Major version mismatch: " + version + " vs " + other.getVersion(), Set.of());
        }
      } catch (final NumberFormatException e) {
        // Non-numeric version
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

  @Override
  public String getWitText() {
    return witText;
  }

  @Override
  public List<String> getImportNames() {
    return Collections.unmodifiableList(importNames);
  }

  @Override
  public List<String> getExportNames() {
    return Collections.unmodifiableList(exportNames);
  }

  void addFunction(final String functionName) {
    if (functionName != null && !functionName.isEmpty()) {
      functionNames.add(functionName);
    }
  }

  void addType(final String typeName) {
    if (typeName != null && !typeName.isEmpty()) {
      typeNames.add(typeName);
    }
  }

  void addDependency(final String dependency) {
    if (dependency != null && !dependency.isEmpty()) {
      dependencies.add(dependency);
    }
  }
}
