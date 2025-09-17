package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Categories of issues that can be identified and addressed.
 *
 * @since 1.0.0
 */
public enum IssueCategory {
  /** Behavioral compatibility issues between runtimes. */
  BEHAVIORAL,

  /** Performance-related issues and optimization opportunities. */
  PERFORMANCE,

  /** Test coverage gaps and missing feature validation. */
  COVERAGE,

  /** Integration issues spanning multiple categories. */
  INTEGRATION
}
