package ai.tegmentum.wasmtime4j.exception;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exception thrown when component linking operations fail.
 *
 * <p>This exception provides detailed information about linking failures, including specific
 * components involved, compatibility issues, dependency problems, and suggested resolutions.
 *
 * @since 1.0.0
 */
public class ComponentLinkingException extends WasmException {

  private final LinkingFailureType failureType;
  private final List<String> involvedComponents;
  private final Map<String, String> compatibilityIssues;
  private final Set<String> missingDependencies;
  private final Set<String> circularDependencies;
  private final List<String> suggestedResolutions;

  /**
   * Creates a new component linking exception with detailed failure information.
   *
   * @param message the exception message
   * @param failureType the type of linking failure
   * @param involvedComponents list of component IDs involved in the linking failure
   * @param compatibilityIssues map of component pairs to their compatibility issues
   * @param missingDependencies set of missing dependency names
   * @param circularDependencies set of components involved in circular dependencies
   * @param suggestedResolutions list of suggested resolutions for the failure
   */
  public ComponentLinkingException(
      String message,
      LinkingFailureType failureType,
      List<String> involvedComponents,
      Map<String, String> compatibilityIssues,
      Set<String> missingDependencies,
      Set<String> circularDependencies,
      List<String> suggestedResolutions) {
    super(message);
    this.failureType = failureType;
    this.involvedComponents = involvedComponents;
    this.compatibilityIssues = compatibilityIssues;
    this.missingDependencies = missingDependencies;
    this.circularDependencies = circularDependencies;
    this.suggestedResolutions = suggestedResolutions;
  }

  /**
   * Creates a new component linking exception with cause.
   *
   * @param message the exception message
   * @param cause the underlying cause
   * @param failureType the type of linking failure
   * @param involvedComponents list of component IDs involved in the linking failure
   * @param compatibilityIssues map of component pairs to their compatibility issues
   * @param missingDependencies set of missing dependency names
   * @param circularDependencies set of components involved in circular dependencies
   * @param suggestedResolutions list of suggested resolutions for the failure
   */
  public ComponentLinkingException(
      String message,
      Throwable cause,
      LinkingFailureType failureType,
      List<String> involvedComponents,
      Map<String, String> compatibilityIssues,
      Set<String> missingDependencies,
      Set<String> circularDependencies,
      List<String> suggestedResolutions) {
    super(message, cause);
    this.failureType = failureType;
    this.involvedComponents = involvedComponents;
    this.compatibilityIssues = compatibilityIssues;
    this.missingDependencies = missingDependencies;
    this.circularDependencies = circularDependencies;
    this.suggestedResolutions = suggestedResolutions;
  }

  /**
   * Gets the type of linking failure.
   *
   * @return the failure type
   */
  public LinkingFailureType getFailureType() {
    return failureType;
  }

  /**
   * Gets the list of component IDs involved in the linking failure.
   *
   * @return list of component IDs
   */
  public List<String> getInvolvedComponents() {
    return involvedComponents;
  }

  /**
   * Gets the compatibility issues between component pairs.
   *
   * @return map of component pairs to their compatibility issues
   */
  public Map<String, String> getCompatibilityIssues() {
    return compatibilityIssues;
  }

  /**
   * Gets the set of missing dependency names.
   *
   * @return set of missing dependency names
   */
  public Set<String> getMissingDependencies() {
    return missingDependencies;
  }

  /**
   * Gets the set of components involved in circular dependencies.
   *
   * @return set of component IDs with circular dependencies
   */
  public Set<String> getCircularDependencies() {
    return circularDependencies;
  }

  /**
   * Gets the list of suggested resolutions for the linking failure.
   *
   * @return list of suggested resolutions
   */
  public List<String> getSuggestedResolutions() {
    return suggestedResolutions;
  }

  /**
   * Checks if the failure is recoverable.
   *
   * @return true if the failure might be recoverable with user action
   */
  public boolean isRecoverable() {
    return failureType == LinkingFailureType.MISSING_DEPENDENCIES
        || failureType == LinkingFailureType.VERSION_INCOMPATIBILITY
        || failureType == LinkingFailureType.INTERFACE_MISMATCH;
  }

  /**
   * Gets a formatted error report with all failure details.
   *
   * @return formatted error report
   */
  public String getDetailedErrorReport() {
    final StringBuilder report = new StringBuilder();
    report.append("Component Linking Failure Report\n");
    report.append("================================\n\n");

    report.append("Failure Type: ").append(failureType).append("\n");
    report.append("Error Message: ").append(getMessage()).append("\n");
    report.append("Recoverable: ").append(isRecoverable() ? "Yes" : "No").append("\n\n");

    if (!involvedComponents.isEmpty()) {
      report.append("Involved Components:\n");
      for (String component : involvedComponents) {
        report.append("  - ").append(component).append("\n");
      }
      report.append("\n");
    }

    if (!compatibilityIssues.isEmpty()) {
      report.append("Compatibility Issues:\n");
      for (Map.Entry<String, String> entry : compatibilityIssues.entrySet()) {
        report
            .append("  - ")
            .append(entry.getKey())
            .append(": ")
            .append(entry.getValue())
            .append("\n");
      }
      report.append("\n");
    }

    if (!missingDependencies.isEmpty()) {
      report.append("Missing Dependencies:\n");
      for (String dependency : missingDependencies) {
        report.append("  - ").append(dependency).append("\n");
      }
      report.append("\n");
    }

    if (!circularDependencies.isEmpty()) {
      report.append("Circular Dependencies:\n");
      for (String component : circularDependencies) {
        report.append("  - ").append(component).append("\n");
      }
      report.append("\n");
    }

    if (!suggestedResolutions.isEmpty()) {
      report.append("Suggested Resolutions:\n");
      for (int i = 0; i < suggestedResolutions.size(); i++) {
        report
            .append("  ")
            .append(i + 1)
            .append(". ")
            .append(suggestedResolutions.get(i))
            .append("\n");
      }
    }

    return report.toString();
  }

  @Override
  public String toString() {
    return String.format(
        "ComponentLinkingException{type=%s, components=%s, message='%s'}",
        failureType, involvedComponents, getMessage());
  }

  /** Types of component linking failures. */
  public enum LinkingFailureType {
    /** Interface mismatch between components. */
    INTERFACE_MISMATCH,

    /** Missing required dependencies. */
    MISSING_DEPENDENCIES,

    /** Circular dependency detected. */
    CIRCULAR_DEPENDENCY,

    /** Version incompatibility between components. */
    VERSION_INCOMPATIBILITY,

    /** Native linking error from the WebAssembly runtime. */
    NATIVE_LINKING_ERROR,

    /** Invalid component state for linking. */
    INVALID_COMPONENT_STATE,

    /** Resource constraints preventing linking. */
    RESOURCE_CONSTRAINTS,

    /** Security policy violation during linking. */
    SECURITY_VIOLATION,

    /** Unknown or unspecified linking failure. */
    UNKNOWN
  }
}
