package ai.tegmentum.wasmtime4j;

/**
 * Advanced WebAssembly features for testing and validation, focusing on cutting-edge capabilities
 * that require comprehensive testing coverage.
 *
 * <p>These features extend beyond basic WebAssembly functionality to include complex operations
 * that are critical for high-performance applications and require specialized testing frameworks.
 *
 * @since 1.0.0
 */
public enum AdvancedWasmFeature {
  /** WebAssembly exception handling proposal support (try/catch/throw operations). */
  EXCEPTIONS,

  /** WebAssembly SIMD vector arithmetic operations (add, sub, mul, div for all types). */
  SIMD_ARITHMETIC,

  /** WebAssembly SIMD memory operations (load, store with various alignments). */
  SIMD_MEMORY,

  /** WebAssembly SIMD manipulation operations (shuffle, swizzle, lane operations). */
  SIMD_MANIPULATION,

  /** WebAssembly atomic load/store operations for shared memory. */
  ATOMIC_OPERATIONS,

  /** WebAssembly compare-and-swap (CAS) operations. */
  ATOMIC_CAS,

  /** WebAssembly shared memory creation and access patterns. */
  SHARED_MEMORY,

  /** WebAssembly memory ordering and synchronization primitives. */
  MEMORY_ORDERING,

  /** Cross-module exception propagation. */
  CROSS_MODULE_EXCEPTIONS,

  /** Nested exception handling scenarios. */
  NESTED_EXCEPTIONS,

  /** Exception type validation and propagation. */
  EXCEPTION_TYPES,

  /** Thread-safe WebAssembly execution validation. */
  THREAD_SAFETY,

  /** SIMD performance optimization validation. */
  SIMD_PERFORMANCE,

  /** Atomic operation performance validation. */
  ATOMIC_PERFORMANCE,

  /** Exception handling performance impact analysis. */
  EXCEPTION_PERFORMANCE
}
