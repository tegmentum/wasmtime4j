package ai.tegmentum.wasmtime4j.comparison.reporters;

/**
 * Overall verdict for comparison results.
 *
 * @since 1.0.0
 */
public enum OverallVerdict {
  /** All tests passed successfully. */
  PASSED,

  /** Tests passed but with warnings. */
  PASSED_WITH_WARNINGS,

  /** Tests failed. */
  FAILED,

  /** Tests failed with additional issues. */
  FAILED_WITH_ISSUES,

  /** All tests passed with no significant issues. */
  EXCELLENT,

  /** Most tests passed with minor issues. */
  GOOD,

  /** Tests passed but with notable issues or performance concerns. */
  ACCEPTABLE,

  /** Some tests failed or significant issues detected. */
  CONCERNING,

  /** Many tests failed or critical issues detected. */
  POOR,

  /** Majority of tests failed or system-level failures. */
  CRITICAL
}
