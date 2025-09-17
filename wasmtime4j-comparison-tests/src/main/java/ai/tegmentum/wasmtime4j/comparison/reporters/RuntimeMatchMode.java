package ai.tegmentum.wasmtime4j.comparison.reporters;

/**
 * Runtime matching modes for filtering.
 *
 * @since 1.0.0
 */
public enum RuntimeMatchMode {
  /** Test must include ANY of the specified runtimes. */
  ANY,

  /** Test must include ALL of the specified runtimes. */
  ALL,

  /** Test must include EXACTLY the specified runtimes. */
  EXACT
}
