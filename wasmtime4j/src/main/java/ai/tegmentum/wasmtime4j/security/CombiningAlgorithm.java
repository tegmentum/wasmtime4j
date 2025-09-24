package ai.tegmentum.wasmtime4j.security;

/**
 * Authorization combining algorithms.
 *
 * @since 1.0.0
 */
public enum CombiningAlgorithm {
  PERMIT_OVERRIDES,
  DENY_OVERRIDES,
  ALLOW_ONLY_IF_ALL_PERMIT,
  FIRST_APPLICABLE
}
