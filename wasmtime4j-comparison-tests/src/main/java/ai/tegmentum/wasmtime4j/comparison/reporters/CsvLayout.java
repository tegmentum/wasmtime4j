package ai.tegmentum.wasmtime4j.comparison.reporters;

/**
 * CSV layout options for different analysis needs.
 *
 * @since 1.0.0
 */
public enum CsvLayout {
  /** Summary view with key metrics per test. */
  SUMMARY,

  /** Detailed view with normalized key-value structure. */
  DETAILED,

  /** Recommendations-focused view. */
  RECOMMENDATIONS,

  /** Performance-focused view. */
  PERFORMANCE,

  /** Discrepancies-focused view. */
  DISCREPANCIES,

  /** Custom view with user-specified columns. */
  CUSTOM
}
