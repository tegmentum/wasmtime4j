package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A plan for instantiating multiple interdependent WebAssembly modules.
 *
 * <p>An instantiation plan provides an ordered sequence of steps for creating
 * module instances while respecting dependency relationships. The plan can be
 * executed incrementally with progress tracking.
 *
 * @since 1.0.0
 */
public final class InstantiationPlan {

  private final List<InstantiationStep> steps;
  private final DependencyResolution dependencyResolution;
  private final Duration planningTime;
  private final boolean executable;

  /**
   * Creates a new instantiation plan.
   *
   * @param steps the ordered list of instantiation steps
   * @param dependencyResolution the dependency resolution result
   * @param planningTime the time taken to create this plan
   * @param executable whether this plan can be executed successfully
   */
  public InstantiationPlan(
      final List<InstantiationStep> steps,
      final DependencyResolution dependencyResolution,
      final Duration planningTime,
      final boolean executable) {
    this.steps = Collections.unmodifiableList(
        Objects.requireNonNull(steps, "steps"));
    this.dependencyResolution = Objects.requireNonNull(dependencyResolution, "dependencyResolution");
    this.planningTime = Objects.requireNonNull(planningTime, "planningTime");
    this.executable = executable;
  }

  /**
   * Gets the ordered list of instantiation steps.
   *
   * @return an unmodifiable list of instantiation steps
   */
  public List<InstantiationStep> getSteps() {
    return steps;
  }

  /**
   * Gets the dependency resolution result this plan is based on.
   *
   * @return the dependency resolution
   */
  public DependencyResolution getDependencyResolution() {
    return dependencyResolution;
  }

  /**
   * Gets the time taken to create this plan.
   *
   * @return the planning duration
   */
  public Duration getPlanningTime() {
    return planningTime;
  }

  /**
   * Checks whether this plan can be executed successfully.
   *
   * <p>A plan is executable if all dependencies can be resolved and there
   * are no circular dependencies or other blocking issues.
   *
   * @return true if the plan is executable, false otherwise
   */
  public boolean isExecutable() {
    return executable;
  }

  /**
   * Gets the total number of steps in this plan.
   *
   * @return the step count
   */
  public int getStepCount() {
    return steps.size();
  }

  /**
   * Executes the instantiation plan using the given linker and store.
   *
   * <p>This method executes all steps in order, creating instances for
   * each module according to the dependency resolution.
   *
   * @param linker the linker to use for instantiation
   * @param store the store to create instances in
   * @return a list of created instances in the order they were instantiated
   * @throws WasmException if instantiation fails at any step
   * @throws IllegalStateException if the plan is not executable
   */
  public List<Instance> execute(final Linker<?> linker, final Store store) throws WasmException {
    if (!executable) {
      throw new IllegalStateException("Cannot execute non-executable instantiation plan");
    }

    Objects.requireNonNull(linker, "linker");
    Objects.requireNonNull(store, "store");

    final java.util.List<Instance> instances = new java.util.ArrayList<>();

    for (final InstantiationStep step : steps) {
      try {
        final Instance instance = step.execute(linker, store);
        instances.add(instance);
      } catch (final WasmException e) {
        throw new WasmException(
            "Failed to execute instantiation step " + step.getStepNumber() + ": " + e.getMessage(),
            e);
      }
    }

    return Collections.unmodifiableList(instances);
  }

  /**
   * Validates that the plan is still valid against the current linker state.
   *
   * <p>This method checks that all required imports are still available
   * and that no conflicting definitions have been added.
   *
   * @param linker the linker to validate against
   * @return true if the plan is still valid, false otherwise
   */
  public boolean isValidFor(final Linker<?> linker) {
    Objects.requireNonNull(linker, "linker");

    // Check that all required imports are still available
    for (final InstantiationStep step : steps) {
      if (!step.canExecuteWith(linker)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public String toString() {
    return String.format(
        "InstantiationPlan{steps=%d, executable=%s, planningTime=%s, " +
        "totalModules=%d, resolutionSuccessful=%s}",
        steps.size(),
        executable,
        planningTime,
        dependencyResolution.getTotalModules(),
        dependencyResolution.isResolutionSuccessful());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final InstantiationPlan that = (InstantiationPlan) obj;
    return executable == that.executable
        && Objects.equals(steps, that.steps)
        && Objects.equals(dependencyResolution, that.dependencyResolution)
        && Objects.equals(planningTime, that.planningTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(steps, dependencyResolution, planningTime, executable);
  }
}