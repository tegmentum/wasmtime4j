package ai.tegmentum.wasmtime4j.wasi;

/**
 * Types of WASI runtime implementations.
 *
 * <p>This enum identifies which underlying technology is being used to provide WASI functionality.
 *
 * @since 1.0.0
 */
public enum WasiRuntimeType {
  /** JNI-based WASI runtime implementation using native libraries. */
  JNI,

  /** Panama Foreign Function Interface-based WASI runtime implementation. */
  PANAMA
}
