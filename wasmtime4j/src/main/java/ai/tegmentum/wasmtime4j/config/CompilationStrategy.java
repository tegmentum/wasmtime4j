package ai.tegmentum.wasmtime4j.config;

/**
 * Compilation strategy enumeration for WebAssembly code generation.
 *
 * <p>Maps to Wasmtime's {@code Strategy} enum which determines which compiler backend is used.
 *
 * @since 1.0.0
 */
public enum CompilationStrategy {
  /** Auto-detect best compilation strategy. */
  AUTO,
  /** Use the Cranelift compiler backend (optimizing). */
  CRANELIFT,
  /** Use the Winch compiler backend (baseline). */
  WINCH
}
