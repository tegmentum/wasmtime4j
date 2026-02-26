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

  // Committee-stage experimental proposals (use with caution)

  /** WebAssembly stack switching proposal support for coroutines and fibers. */
  STACK_SWITCHING,

  /** WebAssembly extended constant expressions proposal support. */
  EXTENDED_CONST_EXPRESSIONS,

  /** WebAssembly custom page sizes proposal support. */
  CUSTOM_PAGE_SIZES,

  /** WebAssembly wide arithmetic proposal support. */
  WIDE_ARITHMETIC,

  /** WebAssembly shared-everything threads proposal support. */
  SHARED_EVERYTHING_THREADS,

  /** WebAssembly Component Model async support. */
  COMPONENT_MODEL_ASYNC,

  /** WebAssembly Component Model async builtins support. */
  COMPONENT_MODEL_ASYNC_BUILTINS,

  /** WebAssembly Component Model async stackful support. */
  COMPONENT_MODEL_ASYNC_STACKFUL,

  /** WebAssembly Component Model error context support. */
  COMPONENT_MODEL_ERROR_CONTEXT,

  /** WebAssembly Component Model GC support. */
  COMPONENT_MODEL_GC,

  /** WebAssembly Component Model threading support. */
  COMPONENT_MODEL_THREADING,

  /** WebAssembly Component Model fixed-length lists support. */
  COMPONENT_MODEL_FIXED_LENGTH_LISTS
}
