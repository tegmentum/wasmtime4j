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
  MEMORY64,

  /** WebAssembly exception handling proposal support. */
  EXCEPTIONS,

  /** WebAssembly Component Model support with WIT interfaces. */
  COMPONENT_MODEL,

  /** WebAssembly garbage collection proposal support. */
  GC,

  /** WebAssembly typed function references proposal support. */
  TYPED_FUNCTION_REFERENCES,

  /** WebAssembly branch hinting proposal support. */
  BRANCH_HINTING,

  /** WebAssembly extended reference types proposal support. */
  EXTENDED_REFERENCE_TYPES,

  /** WebAssembly function subtyping proposal support. */
  FUNCTION_SUBTYPING
}
