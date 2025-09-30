package ai.tegmentum.wasmtime4j;

/**
 * Configuration for WebAssembly component garbage collection.
 *
 * @since 1.0.0
 */
public interface ComponentGarbageCollectionConfig {

  /**
   * Gets the GC trigger threshold.
   *
   * @return the GC threshold
   */
  long getGcThreshold();

  /**
   * Gets the GC interval in milliseconds.
   *
   * @return the GC interval in milliseconds
   */
  long getGcInterval();

  /**
   * Checks if GC is enabled.
   *
   * @return true if GC is enabled
   */
  boolean isGcEnabled();

  /**
   * Gets the GC strategy.
   *
   * @return the GC strategy
   */
  String getGcStrategy();
}
