package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A single step in an instantiation plan.
 *
 * <p>Each step represents the instantiation of one module, along with any necessary setup or
 * dependency resolution.
 *
 * @since 1.0.0
 */
public final class InstantiationStep {

  private final int stepNumber;
  private final Module module;
  private final Optional<String> instanceName;
  private final List<String> requiredImports;
  private final List<String> providedExports;
  private final String description;

  /**
   * Creates a new instantiation step.
   *
   * @param stepNumber the order of this step in the plan
   * @param module the module to instantiate
   * @param instanceName the name to register the instance under (if any)
   * @param requiredImports list of imports this module requires
   * @param providedExports list of exports this module provides
   * @param description a human-readable description of this step
   */
  public InstantiationStep(
      final int stepNumber,
      final Module module,
      final Optional<String> instanceName,
      final List<String> requiredImports,
      final List<String> providedExports,
      final String description) {
    this.stepNumber = stepNumber;
    this.module = Objects.requireNonNull(module, "module");
    this.instanceName = Objects.requireNonNull(instanceName, "instanceName");
    this.requiredImports =
        Collections.unmodifiableList(Objects.requireNonNull(requiredImports, "requiredImports"));
    this.providedExports =
        Collections.unmodifiableList(Objects.requireNonNull(providedExports, "providedExports"));
    this.description = Objects.requireNonNull(description, "description");
  }

  /**
   * Gets the step number in the instantiation plan.
   *
   * @return the step number (1-based)
   */
  public int getStepNumber() {
    return stepNumber;
  }

  /**
   * Gets the module to instantiate in this step.
   *
   * @return the module
   */
  public Module getModule() {
    return module;
  }

  /**
   * Gets the name to register the instance under.
   *
   * @return the instance name if specified
   */
  public Optional<String> getInstanceName() {
    return instanceName;
  }

  /**
   * Gets the list of imports this module requires.
   *
   * @return an unmodifiable list of required import identifiers
   */
  public List<String> getRequiredImports() {
    return requiredImports;
  }

  /**
   * Gets the list of exports this module provides.
   *
   * @return an unmodifiable list of provided export identifiers
   */
  public List<String> getProvidedExports() {
    return providedExports;
  }

  /**
   * Gets a human-readable description of this step.
   *
   * @return the step description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Executes this instantiation step.
   *
   * @param linker the linker to use for instantiation
   * @param store the store to create the instance in
   * @return the created instance
   * @throws WasmException if instantiation fails
   */
  public Instance execute(final Linker<?> linker, final Store store) throws WasmException {
    Objects.requireNonNull(linker, "linker");
    Objects.requireNonNull(store, "store");

    final Instance instance;
    if (instanceName.isPresent()) {
      instance = linker.instantiate(store, instanceName.get(), module);
    } else {
      instance = linker.instantiate(store, module);
    }

    return instance;
  }

  /**
   * Checks whether this step can be executed with the given linker.
   *
   * <p>This method verifies that all required imports are available in the linker.
   *
   * @param linker the linker to check against
   * @return true if the step can be executed, false otherwise
   */
  public boolean canExecuteWith(final Linker<?> linker) {
    Objects.requireNonNull(linker, "linker");

    // Check that all required imports are available
    for (final String importId : requiredImports) {
      final String[] parts = importId.split("::", 2);
      if (parts.length != 2) {
        continue; // Invalid import ID format
      }

      if (!linker.hasImport(parts[0], parts[1])) {
        return false;
      }
    }

    return true;
  }

  @Override
  public String toString() {
    return String.format(
        "InstantiationStep{step=%d, module=%s, instanceName=%s, "
            + "imports=%d, exports=%d, description='%s'}",
        stepNumber,
        "module",
        instanceName.orElse("none"),
        requiredImports.size(),
        providedExports.size(),
        description);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final InstantiationStep that = (InstantiationStep) obj;
    return stepNumber == that.stepNumber
        && Objects.equals(module, that.module)
        && Objects.equals(instanceName, that.instanceName)
        && Objects.equals(requiredImports, that.requiredImports)
        && Objects.equals(providedExports, that.providedExports)
        && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        stepNumber, module, instanceName, requiredImports, providedExports, description);
  }
}
