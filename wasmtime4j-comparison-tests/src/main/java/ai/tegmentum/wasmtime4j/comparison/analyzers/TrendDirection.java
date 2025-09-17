package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Direction of coverage trend.
 *
 * @since 1.0.0
 */
public enum TrendDirection {
  /** Coverage is improving. */
  IMPROVING,

  /** Coverage is declining. */
  DECLINING,

  /** Coverage is stable. */
  STABLE
}
