package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.List;
import java.util.Optional;

/**
 * Represents a WebAssembly validation issue.
 *
 * <p>This interface provides detailed information about validation problems found in WebAssembly
 * modules, including location, severity, and suggested fixes.
 *
 * @since 1.0.0
 */
public interface ValidationIssue {

  /** Validation issue severity levels. */
  enum Severity {
    /** Information about the validation process */
    INFO,
    /** Warning that doesn't prevent execution */
    WARNING,
    /** Error that prevents execution */
    ERROR,
    /** Critical error that indicates serious problems */
    CRITICAL
  }

  /** Categories of validation issues. */
  enum Category {
    /** Type system violations */
    TYPE_MISMATCH,
    /** Invalid instruction sequences */
    INSTRUCTION_ERROR,
    /** Module structure problems */
    STRUCTURE_ERROR,
    /** Import/export mismatches */
    INTERFACE_ERROR,
    /** Memory access violations */
    MEMORY_ERROR,
    /** Table access violations */
    TABLE_ERROR,
    /** Control flow errors */
    CONTROL_FLOW_ERROR,
    /** Feature usage errors */
    FEATURE_ERROR,
    /** Resource limit violations */
    RESOURCE_ERROR,
    /** Unknown validation error */
    UNKNOWN
  }

  /**
   * Gets the unique issue identifier.
   *
   * @return the issue ID
   */
  String getIssueId();

  /**
   * Gets the validation issue message.
   *
   * @return the issue message
   */
  String getMessage();

  /**
   * Gets the issue category.
   *
   * @return the issue category
   */
  Category getCategory();

  /**
   * Gets the issue severity.
   *
   * @return the issue severity
   */
  Severity getSeverity();

  /**
   * Gets the location where the issue was found.
   *
   * @return the issue location, or empty if not available
   */
  Optional<ValidationLocation> getLocation();

  /**
   * Gets the source location if debug information is available.
   *
   * @return the source location, or empty if not available
   */
  Optional<SourceLocation> getSourceLocation();

  /**
   * Gets the detailed description of the issue.
   *
   * @return the detailed description
   */
  String getDetailedDescription();

  /**
   * Gets the suggested fixes for this issue.
   *
   * @return list of suggested fixes
   */
  List<SuggestedFix> getSuggestedFixes();

  /**
   * Gets related documentation links.
   *
   * @return list of documentation URLs
   */
  List<String> getDocumentationLinks();

  /**
   * Gets the WebAssembly specification sections relevant to this issue.
   *
   * @return list of specification section references
   */
  List<String> getSpecificationReferences();

  /**
   * Checks if this issue prevents module instantiation.
   *
   * @return true if this issue is blocking
   */
  boolean isBlocking();

  /**
   * Checks if this issue can be automatically fixed.
   *
   * @return true if auto-fixable
   */
  boolean isAutoFixable();

  /**
   * Gets the expected type information if this is a type error.
   *
   * @return the expected type, or empty if not applicable
   */
  Optional<String> getExpectedType();

  /**
   * Gets the actual type information if this is a type error.
   *
   * @return the actual type, or empty if not applicable
   */
  Optional<String> getActualType();

  /**
   * Creates a builder for constructing ValidationIssue instances.
   *
   * @return a new validation issue builder
   */
  static ValidationIssueBuilder builder() {
    return new ValidationIssueBuilder();
  }

  /**
   * Creates a simple ValidationIssue with basic information.
   *
   * @param issueId the issue ID
   * @param message the issue message
   * @param category the issue category
   * @param severity the issue severity
   * @return the validation issue
   */
  static ValidationIssue of(
      final String issueId,
      final String message,
      final Category category,
      final Severity severity) {
    return builder()
        .issueId(issueId)
        .message(message)
        .category(category)
        .severity(severity)
        .build();
  }
}
