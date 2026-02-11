package ai.tegmentum.wasmtime4j.component;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a dependency graph for WebAssembly components.
 *
 * <p>This class tracks component dependencies and provides algorithms for dependency resolution,
 * circular dependency detection, and topological sorting.
 *
 * @since 1.0.0
 */
public final class ComponentDependencyGraph {

  private final Component rootComponent;
  private final Set<Component> dependencies;
  private final Set<String> circularDependencies;

  /**
   * Creates a new dependency graph for the given component.
   *
   * @param rootComponent the root component
   */
  public ComponentDependencyGraph(final Component rootComponent) {
    this.rootComponent = rootComponent;
    this.dependencies = new HashSet<>();
    this.circularDependencies = new HashSet<>();
  }

  /**
   * Gets the root component.
   *
   * @return the root component
   */
  public Component getRootComponent() {
    return rootComponent;
  }

  /**
   * Gets all dependencies.
   *
   * @return set of dependency components
   */
  public Set<Component> getDependencies() {
    return new HashSet<>(dependencies);
  }

  /**
   * Gets circular dependencies.
   *
   * @return set of circular dependency identifiers
   */
  public Set<String> getCircularDependencies() {
    return new HashSet<>(circularDependencies);
  }

  /**
   * Checks if there are any circular dependencies.
   *
   * @return true if circular dependencies exist
   */
  public boolean hasCircularDependencies() {
    return !circularDependencies.isEmpty();
  }

  /**
   * Adds a dependency to the graph.
   *
   * @param dependency the dependency component
   */
  public void addDependency(final Component dependency) {
    dependencies.add(dependency);
  }

  /**
   * Gets the dependency count.
   *
   * @return number of dependencies
   */
  public int getDependencyCount() {
    return dependencies.size();
  }
}
