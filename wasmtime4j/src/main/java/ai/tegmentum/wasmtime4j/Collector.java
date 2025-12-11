package ai.tegmentum.wasmtime4j;

/**
 * Garbage collector implementations available in Wasmtime.
 *
 * <p>The collector strategy determines how WebAssembly GC objects are managed. Different strategies
 * have different trade-offs between performance, memory usage, and collection behavior.
 *
 * @since 1.0.0
 */
public enum Collector {

  /**
   * Automatic collector selection by Wasmtime.
   *
   * <p>Wasmtime will automatically select the most appropriate collector based on the enabled
   * features and runtime conditions. Currently defaults to {@link #DEFERRED_REFERENCE_COUNTING}.
   */
  AUTO(0),

  /**
   * Deferred reference-counting collector optimized for low latency.
   *
   * <p>This collector uses reference counting with deferred decrements to minimize pause times. It
   * provides excellent latency characteristics but cannot collect cyclic references - they will
   * leak until the store is dropped.
   *
   * <p>This is currently the default collector in Wasmtime and is recommended for most use cases.
   */
  DEFERRED_REFERENCE_COUNTING(1),

  /**
   * Non-collecting allocator that traps on memory exhaustion.
   *
   * <p>The null collector does not perform any garbage collection. Allocations continue until GC
   * heap memory is exhausted, at which point execution traps with an out-of-memory error.
   *
   * <p>This collector is useful for:
   *
   * <ul>
   *   <li>Short-lived instances where collection overhead is unnecessary
   *   <li>Measuring the overhead of the actual collector
   *   <li>Scenarios where deterministic allocation behavior is required
   * </ul>
   */
  NULL(2);

  private final int nativeCode;

  Collector(final int nativeCode) {
    this.nativeCode = nativeCode;
  }

  /**
   * Returns the native code value for this collector.
   *
   * @return the native code
   */
  public int toNativeCode() {
    return nativeCode;
  }

  /**
   * Converts a native code value to a Collector enum.
   *
   * @param code the native code value
   * @return the corresponding Collector, or {@link #AUTO} if the code is invalid
   */
  public static Collector fromNativeCode(final int code) {
    for (Collector collector : values()) {
      if (collector.nativeCode == code) {
        return collector;
      }
    }
    return AUTO;
  }
}
