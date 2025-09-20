package ai.tegmentum.wasmtime4j;

/**
 * WebAssembly features that can be enabled or disabled in an engine configuration.
 *
 * <p>These features correspond to various WebAssembly proposals that extend the core specification
 * with additional functionality.
 *
 * @since 1.0.0
 */
public enum WasmFeature {
  /** WebAssembly reference types proposal support. */
  REFERENCE_TYPES,

  /** WebAssembly SIMD (Single Instruction, Multiple Data) support. */
  SIMD,

  /** WebAssembly relaxed SIMD proposal support. */
  RELAXED_SIMD,

  /** WebAssembly multi-value proposal support (multiple return values). */
  MULTI_VALUE,

  /** WebAssembly bulk memory operations proposal support. */
  BULK_MEMORY,

  /** WebAssembly threads proposal support. */
  THREADS,

  /** WebAssembly tail call proposal support. */
  TAIL_CALL,

  /** WebAssembly multi-memory proposal support. */
  MULTI_MEMORY,

  /** WebAssembly 64-bit memory support. */
  MEMORY64
}
