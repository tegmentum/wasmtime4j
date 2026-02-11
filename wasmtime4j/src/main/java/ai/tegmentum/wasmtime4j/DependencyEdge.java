package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.func.Function;

import java.util.Objects;

/**
 * Represents a dependency relationship between two WebAssembly modules.
 *
 * <p>A dependency edge indicates that one module (the dependent) requires functionality from
 * another module (the dependency) through imports.
 *
 * @since 1.0.0
 */
public final class DependencyEdge {

  private final Module dependent;
  private final Module dependency;
  private final String importModule;
  private final String importName;
  private final DependencyType dependencyType;
  private final boolean resolved;

  /**
   * Creates a new dependency edge.
   *
   * @param dependent the module that has the dependency
   * @param dependency the module that satisfies the dependency
   * @param importModule the import module name
   * @param importName the import name
   * @param dependencyType the type of dependency
   * @param resolved whether this dependency has been resolved
   */
  public DependencyEdge(
      final Module dependent,
      final Module dependency,
      final String importModule,
      final String importName,
      final DependencyType dependencyType,
      final boolean resolved) {
    this.dependent = Objects.requireNonNull(dependent, "dependent");
    this.dependency = Objects.requireNonNull(dependency, "dependency");
    this.importModule = Objects.requireNonNull(importModule, "importModule");
    this.importName = Objects.requireNonNull(importName, "importName");
    this.dependencyType = Objects.requireNonNull(dependencyType, "dependencyType");
    this.resolved = resolved;
  }

  /**
   * Gets the module that has the dependency.
   *
   * @return the dependent module
   */
  public Module getDependent() {
    return dependent;
  }

  /**
   * Gets the module that satisfies the dependency.
   *
   * @return the dependency module
   */
  public Module getDependency() {
    return dependency;
  }

  /**
   * Gets the import module name.
   *
   * @return the import module name
   */
  public String getImportModule() {
    return importModule;
  }

  /**
   * Gets the import name.
   *
   * @return the import name
   */
  public String getImportName() {
    return importName;
  }

  /**
   * Gets the type of dependency.
   *
   * @return the dependency type
   */
  public DependencyType getDependencyType() {
    return dependencyType;
  }

  /**
   * Checks whether this dependency has been resolved.
   *
   * @return true if resolved, false otherwise
   */
  public boolean isResolved() {
    return resolved;
  }

  /**
   * Gets a string representation of the dependency relationship.
   *
   * @return a string in the format "dependent -> dependency (importModule::importName)"
   */
  public String getDependencyString() {
    return String.format("%s -> %s (%s::%s)", "module", "module", importModule, importName);
  }

  @Override
  public String toString() {
    return String.format(
        "DependencyEdge{%s, type=%s, resolved=%s}",
        getDependencyString(), dependencyType, resolved);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final DependencyEdge that = (DependencyEdge) obj;
    return resolved == that.resolved
        && Objects.equals(dependent, that.dependent)
        && Objects.equals(dependency, that.dependency)
        && Objects.equals(importModule, that.importModule)
        && Objects.equals(importName, that.importName)
        && dependencyType == that.dependencyType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dependent, dependency, importModule, importName, dependencyType, resolved);
  }

  /** Types of dependencies between WebAssembly modules. */
  public enum DependencyType {
    /** Function import dependency. */
    FUNCTION,
    /** Memory import dependency. */
    MEMORY,
    /** Table import dependency. */
    TABLE,
    /** Global import dependency. */
    GLOBAL,
    /** Instance import dependency (all exports from another module). */
    INSTANCE
  }
}
