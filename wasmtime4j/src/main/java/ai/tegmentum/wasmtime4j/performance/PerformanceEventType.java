package ai.tegmentum.wasmtime4j.performance;

/**
 * Enumeration of performance event types in WebAssembly execution.
 *
 * <p>These event types categorize different kinds of performance-related occurrences during
 * WebAssembly execution for analysis and monitoring purposes.
 *
 * @since 1.0.0
 */
public enum PerformanceEventType {

  /** Function call started. */
  FUNCTION_CALL_START,

  /** Function call completed. */
  FUNCTION_CALL_END,

  /** Host function call started. */
  HOST_FUNCTION_CALL_START,

  /** Host function call completed. */
  HOST_FUNCTION_CALL_END,

  /** Memory allocation occurred. */
  MEMORY_ALLOCATION,

  /** Memory deallocation occurred. */
  MEMORY_DEALLOCATION,

  /** Linear memory growth operation. */
  MEMORY_GROWTH,

  /** JIT compilation started. */
  JIT_COMPILATION_START,

  /** JIT compilation completed. */
  JIT_COMPILATION_END,

  /** Module instantiation started. */
  INSTANCE_CREATION_START,

  /** Module instantiation completed. */
  INSTANCE_CREATION_END,

  /** Instance cleanup started. */
  INSTANCE_CLEANUP_START,

  /** Instance cleanup completed. */
  INSTANCE_CLEANUP_END,

  /** Garbage collection event. */
  GARBAGE_COLLECTION,

  /** Performance threshold exceeded. */
  THRESHOLD_VIOLATION,

  /** Slow operation detected. */
  SLOW_OPERATION,

  /** Memory pressure detected. */
  MEMORY_PRESSURE,

  /** CPU usage spike detected. */
  CPU_SPIKE,

  /** WebAssembly trap occurred. */
  WASM_TRAP,

  /** Runtime error occurred. */
  RUNTIME_ERROR,

  /** Performance monitoring started. */
  MONITORING_START,

  /** Performance monitoring stopped. */
  MONITORING_STOP,

  /** Cache hit occurred. */
  CACHE_HIT,

  /** Cache miss occurred. */
  CACHE_MISS,

  /** Custom performance event. */
  CUSTOM
}
