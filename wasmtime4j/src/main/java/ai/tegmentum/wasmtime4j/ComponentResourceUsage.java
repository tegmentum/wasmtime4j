package ai.tegmentum.wasmtime4j;

/**
 * Represents resource usage information for a WebAssembly component.
 *
 * <p>This class provides metrics about component resource consumption including memory usage, CPU
 * usage, and other performance indicators.
 *
 * @since 1.0.0
 */
public final class ComponentResourceUsage {

  private final String componentId;
  private final long memoryUsedBytes;
  private final long stackUsedBytes;
  private final double cpuUsagePercent;
  private final int activeInstances;
  private final long timestamp;

  /**
   * Creates a new component resource usage record.
   *
   * @param componentId the component identifier
   * @param memoryUsedBytes memory used in bytes
   * @param stackUsedBytes stack used in bytes
   * @param cpuUsagePercent CPU usage as a percentage
   * @param activeInstances number of active instances
   */
  public ComponentResourceUsage(
      final String componentId,
      final long memoryUsedBytes,
      final long stackUsedBytes,
      final double cpuUsagePercent,
      final int activeInstances) {
    this.componentId = componentId;
    this.memoryUsedBytes = memoryUsedBytes;
    this.stackUsedBytes = stackUsedBytes;
    this.cpuUsagePercent = cpuUsagePercent;
    this.activeInstances = activeInstances;
    this.timestamp = System.currentTimeMillis();
  }

  /**
   * Creates a new component resource usage record with minimal data.
   *
   * @param componentId the component identifier
   */
  public ComponentResourceUsage(final String componentId) {
    this(componentId, 0, 0, 0.0, 0);
  }

  public String getComponentId() {
    return componentId;
  }

  public long getMemoryUsedBytes() {
    return memoryUsedBytes;
  }

  public long getStackUsedBytes() {
    return stackUsedBytes;
  }

  public double getCpuUsagePercent() {
    return cpuUsagePercent;
  }

  public int getActiveInstances() {
    return activeInstances;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return String.format(
        "ComponentResourceUsage{componentId='%s', memory=%d bytes, stack=%d bytes, cpu=%.2f%%,"
            + " instances=%d}",
        componentId, memoryUsedBytes, stackUsedBytes, cpuUsagePercent, activeInstances);
  }
}
