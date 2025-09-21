package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.List;
import java.util.Optional;

/**
 * Represents a known issue similar to the current error.
 *
 * <p>This interface provides information about previously documented issues that share similarities
 * with the current error, including resolutions and workarounds.
 *
 * @since 1.0.0
 */
public interface KnownIssue {

  /** Status of the known issue. */
  enum Status {
    /** Issue is open and not yet resolved */
    OPEN,
    /** Issue is currently being investigated */
    IN_PROGRESS,
    /** Issue has been resolved */
    RESOLVED,
    /** Issue has been closed without resolution */
    CLOSED,
    /** Issue is a duplicate of another issue */
    DUPLICATE,
    /** Issue cannot be reproduced */
    CANNOT_REPRODUCE,
    /** Issue will not be fixed */
    WONT_FIX
  }

  /** Similarity level to the current issue. */
  enum SimilarityLevel {
    /** Low similarity */
    LOW,
    /** Medium similarity */
    MEDIUM,
    /** High similarity */
    HIGH,
    /** Exact match */
    EXACT
  }

  /**
   * Gets the unique issue identifier.
   *
   * @return the issue ID
   */
  String getIssueId();

  /**
   * Gets the issue title.
   *
   * @return the issue title
   */
  String getTitle();

  /**
   * Gets the issue description.
   *
   * @return the issue description
   */
  String getDescription();

  /**
   * Gets the issue status.
   *
   * @return the issue status
   */
  Status getStatus();

  /**
   * Gets the similarity level to the current issue.
   *
   * @return the similarity level
   */
  SimilarityLevel getSimilarityLevel();

  /**
   * Gets the similarity score (0.0 - 1.0).
   *
   * @return the similarity score
   */
  double getSimilarityScore();

  /**
   * Gets the issue category.
   *
   * @return the issue category
   */
  WasmError.Category getCategory();

  /**
   * Gets the issue severity.
   *
   * @return the issue severity
   */
  WasmError.Severity getSeverity();

  /**
   * Gets the resolution description if the issue is resolved.
   *
   * @return the resolution description, or empty if not resolved
   */
  Optional<String> getResolution();

  /**
   * Gets available workarounds for this issue.
   *
   * @return list of workarounds
   */
  List<String> getWorkarounds();

  /**
   * Gets the suggested fixes that worked for this issue.
   *
   * @return list of suggested fixes
   */
  List<SuggestedFix> getSuggestedFixes();

  /**
   * Gets related bug reports or issue tracker links.
   *
   * @return list of related links
   */
  List<String> getRelatedLinks();

  /**
   * Gets the tags associated with this issue.
   *
   * @return list of tags
   */
  List<String> getTags();

  /**
   * Gets the WebAssembly runtime versions affected by this issue.
   *
   * @return list of affected versions
   */
  List<String> getAffectedVersions();

  /**
   * Gets the WebAssembly runtime versions that fixed this issue.
   *
   * @return list of fixed versions, or empty if not applicable
   */
  List<String> getFixedVersions();

  /**
   * Gets the platforms affected by this issue.
   *
   * @return list of affected platforms
   */
  List<String> getAffectedPlatforms();

  /**
   * Gets the first reported date of this issue.
   *
   * @return the first reported timestamp
   */
  long getFirstReported();

  /**
   * Gets the last updated date of this issue.
   *
   * @return the last updated timestamp
   */
  long getLastUpdated();

  /**
   * Gets the resolution date if the issue is resolved.
   *
   * @return the resolution timestamp, or empty if not resolved
   */
  Optional<Long> getResolvedDate();

  /**
   * Gets the number of times this issue has been reported.
   *
   * @return the report count
   */
  int getReportCount();

  /**
   * Checks if this issue affects the current environment.
   *
   * @return true if this issue affects the current environment
   */
  boolean affectsCurrentEnvironment();

  /**
   * Checks if this issue has been verified as reproducible.
   *
   * @return true if the issue is reproducible
   */
  boolean isReproducible();

  /**
   * Gets the reproduction steps for this issue.
   *
   * @return the reproduction steps, or empty if not available
   */
  Optional<String> getReproductionSteps();

  /**
   * Creates a builder for constructing KnownIssue instances.
   *
   * @return a new known issue builder
   */
  static KnownIssueBuilder builder() {
    return new KnownIssueBuilder();
  }

  /**
   * Creates a KnownIssue with basic information.
   *
   * @param issueId the issue ID
   * @param title the issue title
   * @param status the issue status
   * @param similarityLevel the similarity level
   * @return the known issue
   */
  static KnownIssue of(
      final String issueId,
      final String title,
      final Status status,
      final SimilarityLevel similarityLevel) {
    return builder()
        .issueId(issueId)
        .title(title)
        .status(status)
        .similarityLevel(similarityLevel)
        .build();
  }
}
