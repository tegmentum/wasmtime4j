package ai.tegmentum.wasmtime4j;

/**
 * Represents resource usage information for a component engine.
 *
 * <p>This class provides metrics about engine-level resource consumption across all managed
 * components and instances.
 *
 * @since 1.0.0
 */
public final class ComponentEngineResourceUsage {

  private final int totalComponents;
  private final int activeInstances;
  private final long totalMemoryUsedBytes;
  private final long totalStackUsedBytes;
  private final long timestamp;

  /**
   * Creates a new component engine resource usage record.
   *
   * @param totalComponents total number of components
   * @param activeInstances total number of active instances
   * @param totalMemoryUsedBytes total memory used in bytes
   * @param totalStackUsedBytes total stack used in bytes
   */
  public ComponentEngineResourceUsage(
      final int totalComponents,
      final int activeInstances,
      final long totalMemoryUsedBytes,
      final long totalStackUsedBytes) {
    this.totalComponents = totalComponents;
    this.activeInstances = activeInstances;
    this.totalMemoryUsedBytes = totalMemoryUsedBytes;
    this.totalStackUsedBytes = totalStackUsedBytes;
    this.timestamp = System.currentTimeMillis();
  }

  public int getTotalComponents() {
    return totalComponents;
  }

  public int getActiveInstances() {
    return activeInstances;
  }

  public long getTotalMemoryUsedBytes() {
    return totalMemoryUsedBytes;
  }

  public long getTotalStackUsedBytes() {
    return totalStackUsedBytes;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return String.format(
        "ComponentEngineResourceUsage{components=%d, instances=%d, memory=%d bytes, stack=%d"
            + " bytes}",
        totalComponents, activeInstances, totalMemoryUsedBytes, totalStackUsedBytes);
  }
}
