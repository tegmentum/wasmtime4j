package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of dependency resolution analysis for a set of WebAssembly modules.
 *
 * <p>This class provides detailed information about the dependency relationships between modules,
 * the optimal instantiation order, and any issues detected during analysis.
 *
 * @since 1.0.0
 */
public final class DependencyResolution {

  private final List<Module> instantiationOrder;
  private final List<DependencyEdge> dependencies;
  private final boolean hasCircularDependencies;
  private final List<String> circularDependencyChains;
  private final int totalModules;
  private final int resolvedDependencies;
  private final Duration analysisTime;
  private final boolean resolutionSuccessful;

  /**
   * Creates a new dependency resolution result.
   *
   * @param instantiationOrder the optimal order for module instantiation
   * @param dependencies the complete list of dependency relationships
   * @param hasCircularDependencies whether circular dependencies were detected
   * @param circularDependencyChains descriptions of any circular dependency chains
   * @param totalModules the total number of modules analyzed
   * @param resolvedDependencies the number of dependencies successfully resolved
   * @param analysisTime the time taken for dependency analysis
   * @param resolutionSuccessful whether dependency resolution was successful
   */
  public DependencyResolution(
      final List<Module> instantiationOrder,
      final List<DependencyEdge> dependencies,
      final boolean hasCircularDependencies,
      final List<String> circularDependencyChains,
      final int totalModules,
      final int resolvedDependencies,
      final Duration analysisTime,
      final boolean resolutionSuccessful) {
    this.instantiationOrder =
        Collections.unmodifiableList(
            Objects.requireNonNull(instantiationOrder, "instantiationOrder"));
    this.dependencies =
        Collections.unmodifiableList(Objects.requireNonNull(dependencies, "dependencies"));
    this.hasCircularDependencies = hasCircularDependencies;
    this.circularDependencyChains =
        Collections.unmodifiableList(
            Objects.requireNonNull(circularDependencyChains, "circularDependencyChains"));
    this.totalModules = totalModules;
    this.resolvedDependencies = resolvedDependencies;
    this.analysisTime = Objects.requireNonNull(analysisTime, "analysisTime");
    this.resolutionSuccessful = resolutionSuccessful;
  }

  /**
   * Gets the optimal instantiation order for the modules.
   *
   * <p>Modules should be instantiated in this order to ensure all dependencies are satisfied. If
   * resolution was not successful, this list may be incomplete or empty.
   *
   * @return an unmodifiable list of modules in instantiation order
   */
  public List<Module> getInstantiationOrder() {
    return instantiationOrder;
  }

  /**
   * Gets all dependency relationships between the modules.
   *
   * @return an unmodifiable list of dependency edges
   */
  public List<DependencyEdge> getDependencies() {
    return dependencies;
  }

  /**
   * Checks whether circular dependencies were detected.
   *
   * @return true if circular dependencies exist, false otherwise
   */
  public boolean hasCircularDependencies() {
    return hasCircularDependencies;
  }

  /**
   * Gets descriptions of any circular dependency chains detected.
   *
   * <p>Each string describes a cycle in the dependency graph, typically in the format "ModuleA ->
   * ModuleB -> ModuleC -> ModuleA".
   *
   * @return an unmodifiable list of circular dependency descriptions
   */
  public List<String> getCircularDependencyChains() {
    return circularDependencyChains;
  }

  /**
   * Gets the total number of modules that were analyzed.
   *
   * @return the total module count
   */
  public int getTotalModules() {
    return totalModules;
  }

  /**
   * Gets the number of dependencies that were successfully resolved.
   *
   * @return the resolved dependency count
   */
  public int getResolvedDependencies() {
    return resolvedDependencies;
  }

  /**
   * Gets the time taken for dependency analysis.
   *
   * @return the analysis duration
   */
  public Duration getAnalysisTime() {
    return analysisTime;
  }

  /**
   * Checks whether dependency resolution was successful.
   *
   * <p>Resolution is considered successful if all dependencies can be satisfied and no circular
   * dependencies exist.
   *
   * @return true if resolution was successful, false otherwise
   */
  public boolean isResolutionSuccessful() {
    return resolutionSuccessful;
  }

  /**
   * Calculates the dependency resolution rate as a percentage.
   *
   * @return the percentage of dependencies that were resolved (0.0 to 100.0)
   */
  public double getResolutionRate() {
    if (dependencies.isEmpty()) {
      return 100.0;
    }
    return (double) resolvedDependencies / dependencies.size() * 100.0;
  }

  @Override
  public String toString() {
    return String.format(
        "DependencyResolution{modules=%d, dependencies=%d, resolved=%d (%.1f%%), "
            + "circular=%s, successful=%s, analysisTime=%s}",
        totalModules,
        dependencies.size(),
        resolvedDependencies,
        getResolutionRate(),
        hasCircularDependencies,
        resolutionSuccessful,
        analysisTime);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final DependencyResolution that = (DependencyResolution) obj;
    return hasCircularDependencies == that.hasCircularDependencies
        && totalModules == that.totalModules
        && resolvedDependencies == that.resolvedDependencies
        && resolutionSuccessful == that.resolutionSuccessful
        && Objects.equals(instantiationOrder, that.instantiationOrder)
        && Objects.equals(dependencies, that.dependencies)
        && Objects.equals(circularDependencyChains, that.circularDependencyChains)
        && Objects.equals(analysisTime, that.analysisTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        instantiationOrder,
        dependencies,
        hasCircularDependencies,
        circularDependencyChains,
        totalModules,
        resolvedDependencies,
        analysisTime,
        resolutionSuccessful);
  }
}
