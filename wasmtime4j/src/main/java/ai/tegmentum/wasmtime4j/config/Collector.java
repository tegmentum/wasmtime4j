package ai.tegmentum.wasmtime4j.config;

/**
 * GC collector implementation strategies for the WebAssembly engine.
 *
 * <p>Controls which garbage collector implementation is used by the engine for managing GC-managed
 * WebAssembly objects (structs, arrays, etc.). The collector choice affects both performance
 * characteristics and feature support.
 *
 * @since 1.1.0
 */
public enum Collector {

  /** Automatically select the best available collector (default). */
  AUTO("auto"),

  /**
   * Deferred reference counting collector.
   *
   * <p>Uses reference counting with deferred cycle collection. This is currently the only non-null
   * collector implementation in Wasmtime.
   */
  DEFERRED_REFERENCE_COUNTING("deferred_reference_counting"),

  /**
   * Null collector that never collects garbage.
   *
   * <p>Useful for short-lived instances where GC overhead is not desired. All allocations remain
   * until the store is dropped.
   */
  NULL("null");

  private final String rustName;

  Collector(final String rustName) {
    this.rustName = rustName;
  }

  /**
   * Gets the Rust-side configuration name for this collector.
   *
   * @return the Rust configuration string
   */
  public String getRustName() {
    return rustName;
  }

  /**
   * Parses a collector from its Rust configuration string.
   *
   * @param name the Rust configuration name
   * @return the corresponding Collector
   * @throws IllegalArgumentException if the name is not recognized
   */
  public static Collector fromString(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Collector name cannot be null");
    }
    for (final Collector collector : values()) {
      if (collector.rustName.equals(name)) {
        return collector;
      }
    }
    throw new IllegalArgumentException("Unknown collector: " + name);
  }
}
