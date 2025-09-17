package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Types of coverage gaps.
 *
 * @since 1.0.0
 */
public enum CoverageGapType {
  /** A runtime is missing from testing. */
  RUNTIME_MISSING,

  /** Feature coverage is incomplete for a runtime. */
  FEATURE_INCOMPLETE,

  /** An entire category of features is untested. */
  CATEGORY_UNTESTED,

  /** Feature interaction testing is insufficient. */
  INTERACTION_INCOMPLETE
}
