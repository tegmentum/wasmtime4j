package ai.tegmentum.wasmtime4j;

/**
 * Types of WebAssembly runtime implementations.
 *
 * <p>This enum identifies which underlying technology is being used to provide WebAssembly
 * functionality.
 *
 * @since 1.0.0
 */
public enum RuntimeType {
  /** JNI-based runtime implementation using native libraries. */
  JNI,

  /** Panama Foreign Function Interface-based runtime implementation. */
  PANAMA
}
