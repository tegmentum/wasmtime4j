package ai.tegmentum.wasmtime4j.comparison.reporters;

/**
 * Status levels for test comparison results.
 *
 * @since 1.0.0
 */
public enum TestResultStatus {
  /** Test passed successfully across all runtimes. */
  SUCCESS,

  /** Test passed but with warnings or minor discrepancies. */
  WARNING,

  /** Test failed but without critical implications. */
  FAILURE,

  /** Test failed with critical issues requiring immediate attention. */
  CRITICAL
}
