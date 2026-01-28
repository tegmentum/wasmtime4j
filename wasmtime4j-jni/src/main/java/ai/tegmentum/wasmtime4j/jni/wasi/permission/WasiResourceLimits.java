package ai.tegmentum.wasmtime4j.jni.wasi.permission;

import java.time.Duration;

/**
 * Resource limiting and quota enforcement configuration for WASI contexts.
 *
 * <p>This class defines various resource limits that can be enforced for WASI operations to prevent
 * resource exhaustion and ensure fair resource allocation. All limits are optional and can be set
 * to unlimited (negative values).
 *
 * <p>Resource limits include:
 *
 * <ul>
 *   <li>Memory usage limits
 *   <li>File descriptor limits
 *   <li>Disk I/O limits
 *   <li>Network connection limits
 *   <li>Execution time limits
 *   <li>CPU usage limits
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiResourceLimits {

  /** Unlimited resource value. */
  public static final long UNLIMITED = -1L;

  /** Maximum memory usage in bytes (unlimited if negative). */
  private final long maxMemoryBytes;

  /** Maximum number of open file descriptors (unlimited if negative). */
  private final int maxFileDescriptors;

  /** Maximum disk read operations per second (unlimited if negative). */
  private final long maxDiskReadsPerSecond;

  /** Maximum disk write operations per second (unlimited if negative). */
  private final long maxDiskWritesPerSecond;

  /** Maximum bytes read from disk per second (unlimited if negative). */
  private final long maxDiskReadBytesPerSecond;

  /** Maximum bytes written to disk per second (unlimited if negative). */
  private final long maxDiskWriteBytesPerSecond;

  /** Maximum number of network connections (unlimited if negative). */
  private final int maxNetworkConnections;

  /** Maximum execution time before timeout (null for unlimited). */
  private final Duration maxExecutionTime;

  /** Maximum CPU time usage (null for unlimited). */
  private final Duration maxCpuTime;

  /** Maximum wall clock time (null for unlimited). */
  private final Duration maxWallClockTime;

  /** Maximum size of individual files that can be created (unlimited if negative). */
  private final long maxFileSize;

  /** Maximum total disk space that can be used (unlimited if negative). */
  private final long maxDiskSpaceUsage;

  /**
   * Creates new WASI resource limits with the specified configuration.
   *
   * @param builder the resource limits builder
   */
  private WasiResourceLimits(final Builder builder) {
    this.maxMemoryBytes = builder.maxMemoryBytes;
    this.maxFileDescriptors = builder.maxFileDescriptors;
    this.maxDiskReadsPerSecond = builder.maxDiskReadsPerSecond;
    this.maxDiskWritesPerSecond = builder.maxDiskWritesPerSecond;
    this.maxDiskReadBytesPerSecond = builder.maxDiskReadBytesPerSecond;
    this.maxDiskWriteBytesPerSecond = builder.maxDiskWriteBytesPerSecond;
    this.maxNetworkConnections = builder.maxNetworkConnections;
    this.maxExecutionTime = builder.maxExecutionTime;
    this.maxCpuTime = builder.maxCpuTime;
    this.maxWallClockTime = builder.maxWallClockTime;
    this.maxFileSize = builder.maxFileSize;
    this.maxDiskSpaceUsage = builder.maxDiskSpaceUsage;
  }

  /**
   * Gets the maximum memory usage in bytes.
   *
   * @return the maximum memory bytes, or {@link #UNLIMITED} if unlimited
   */
  public long getMaxMemoryBytes() {
    return maxMemoryBytes;
  }

  /**
   * Gets the maximum number of open file descriptors.
   *
   * @return the maximum file descriptors, or {@link #UNLIMITED} if unlimited
   */
  public int getMaxFileDescriptors() {
    return maxFileDescriptors;
  }

  /**
   * Gets the maximum disk read operations per second.
   *
   * @return the maximum disk reads per second, or {@link #UNLIMITED} if unlimited
   */
  public long getMaxDiskReadsPerSecond() {
    return maxDiskReadsPerSecond;
  }

  /**
   * Gets the maximum disk write operations per second.
   *
   * @return the maximum disk writes per second, or {@link #UNLIMITED} if unlimited
   */
  public long getMaxDiskWritesPerSecond() {
    return maxDiskWritesPerSecond;
  }

  /**
   * Gets the maximum bytes read from disk per second.
   *
   * @return the maximum disk read bytes per second, or {@link #UNLIMITED} if unlimited
   */
  public long getMaxDiskReadBytesPerSecond() {
    return maxDiskReadBytesPerSecond;
  }

  /**
   * Gets the maximum bytes written to disk per second.
   *
   * @return the maximum disk write bytes per second, or {@link #UNLIMITED} if unlimited
   */
  public long getMaxDiskWriteBytesPerSecond() {
    return maxDiskWriteBytesPerSecond;
  }

  /**
   * Gets the maximum number of network connections.
   *
   * @return the maximum network connections, or {@link #UNLIMITED} if unlimited
   */
  public int getMaxNetworkConnections() {
    return maxNetworkConnections;
  }

  /**
   * Gets the maximum execution time.
   *
   * @return the maximum execution time, or null if unlimited
   */
  public Duration getMaxExecutionTime() {
    return maxExecutionTime;
  }

  /**
   * Gets the maximum CPU time usage.
   *
   * @return the maximum CPU time, or null if unlimited
   */
  public Duration getMaxCpuTime() {
    return maxCpuTime;
  }

  /**
   * Gets the maximum wall clock time.
   *
   * @return the maximum wall clock time, or null if unlimited
   */
  public Duration getMaxWallClockTime() {
    return maxWallClockTime;
  }

  /**
   * Gets the maximum size of individual files that can be created.
   *
   * @return the maximum file size, or {@link #UNLIMITED} if unlimited
   */
  public long getMaxFileSize() {
    return maxFileSize;
  }

  /**
   * Gets the maximum total disk space that can be used.
   *
   * @return the maximum disk space usage, or {@link #UNLIMITED} if unlimited
   */
  public long getMaxDiskSpaceUsage() {
    return maxDiskSpaceUsage;
  }

  /**
   * Checks if memory usage is limited.
   *
   * @return true if memory usage has a limit, false if unlimited
   */
  public boolean isMemoryLimited() {
    return maxMemoryBytes != UNLIMITED;
  }

  /**
   * Checks if file descriptors are limited.
   *
   * @return true if file descriptors have a limit, false if unlimited
   */
  public boolean areFileDescriptorsLimited() {
    return maxFileDescriptors != UNLIMITED;
  }

  /**
   * Checks if disk I/O operations are rate limited.
   *
   * @return true if disk I/O has rate limits, false if unlimited
   */
  public boolean isDiskIoLimited() {
    return maxDiskReadsPerSecond != UNLIMITED
        || maxDiskWritesPerSecond != UNLIMITED
        || maxDiskReadBytesPerSecond != UNLIMITED
        || maxDiskWriteBytesPerSecond != UNLIMITED;
  }

  /**
   * Checks if network connections are limited.
   *
   * @return true if network connections have a limit, false if unlimited
   */
  public boolean areNetworkConnectionsLimited() {
    return maxNetworkConnections != UNLIMITED;
  }

  /**
   * Checks if execution time is limited.
   *
   * @return true if execution time has limits, false if unlimited
   */
  public boolean isExecutionTimeLimited() {
    return maxExecutionTime != null || maxCpuTime != null || maxWallClockTime != null;
  }

  /**
   * Checks if file operations are limited.
   *
   * @return true if file operations have limits, false if unlimited
   */
  public boolean areFileOperationsLimited() {
    return maxFileSize != UNLIMITED || maxDiskSpaceUsage != UNLIMITED;
  }

  /**
   * Creates default resource limits with moderate restrictions.
   *
   * @return default resource limits
   */
  public static WasiResourceLimits defaultLimits() {
    return builder()
        .withMaxMemoryBytes(256L * 1024L * 1024L) // 256 MB
        .withMaxFileDescriptors(1024)
        .withMaxDiskReadsPerSecond(1000)
        .withMaxDiskWritesPerSecond(1000)
        .withMaxDiskReadBytesPerSecond(10L * 1024L * 1024L) // 10 MB/s
        .withMaxDiskWriteBytesPerSecond(10L * 1024L * 1024L) // 10 MB/s
        .withMaxNetworkConnections(100)
        .withMaxExecutionTime(Duration.ofMinutes(5))
        .withMaxFileSize(100L * 1024L * 1024L) // 100 MB
        .withMaxDiskSpaceUsage(1L * 1024L * 1024L * 1024L) // 1 GB
        .build();
  }

  /**
   * Creates restrictive resource limits with tight restrictions.
   *
   * @return restrictive resource limits
   */
  public static WasiResourceLimits restrictiveLimits() {
    return builder()
        .withMaxMemoryBytes(64L * 1024L * 1024L) // 64 MB
        .withMaxFileDescriptors(256)
        .withMaxDiskReadsPerSecond(100)
        .withMaxDiskWritesPerSecond(100)
        .withMaxDiskReadBytesPerSecond(1024L * 1024L) // 1 MB/s
        .withMaxDiskWriteBytesPerSecond(1024L * 1024L) // 1 MB/s
        .withMaxNetworkConnections(10)
        .withMaxExecutionTime(Duration.ofMinutes(1))
        .withMaxFileSize(10L * 1024L * 1024L) // 10 MB
        .withMaxDiskSpaceUsage(100L * 1024L * 1024L) // 100 MB
        .build();
  }

  /**
   * Creates permissive resource limits with generous restrictions.
   *
   * @return permissive resource limits
   */
  public static WasiResourceLimits permissiveLimits() {
    return builder()
        .withMaxMemoryBytes(2L * 1024L * 1024L * 1024L) // 2 GB
        .withMaxFileDescriptors(8192)
        .withMaxDiskReadsPerSecond(10000)
        .withMaxDiskWritesPerSecond(10000)
        .withMaxDiskReadBytesPerSecond(100L * 1024L * 1024L) // 100 MB/s
        .withMaxDiskWriteBytesPerSecond(100L * 1024L * 1024L) // 100 MB/s
        .withMaxNetworkConnections(1000)
        .withMaxExecutionTime(Duration.ofHours(1))
        .withMaxFileSize(1L * 1024L * 1024L * 1024L) // 1 GB
        .withMaxDiskSpaceUsage(10L * 1024L * 1024L * 1024L) // 10 GB
        .build();
  }

  /**
   * Creates unlimited resource limits (no restrictions).
   *
   * @return unlimited resource limits
   */
  public static WasiResourceLimits unlimitedLimits() {
    return builder().build(); // All defaults are unlimited
  }

  /**
   * Creates a new resource limits builder.
   *
   * @return a new resource limits builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String toString() {
    return String.format(
        "WasiResourceLimits{memory=%s, fileDescriptors=%s, diskReads=%s/s, diskWrites=%s/s, "
            + "diskReadBytes=%s/s, diskWriteBytes=%s/s, networkConnections=%s, "
            + "executionTime=%s, cpuTime=%s, wallClockTime=%s, fileSize=%s, diskSpace=%s}",
        formatBytes(maxMemoryBytes),
        maxFileDescriptors == UNLIMITED ? "unlimited" : maxFileDescriptors,
        maxDiskReadsPerSecond == UNLIMITED ? "unlimited" : maxDiskReadsPerSecond,
        maxDiskWritesPerSecond == UNLIMITED ? "unlimited" : maxDiskWritesPerSecond,
        formatBytes(maxDiskReadBytesPerSecond),
        formatBytes(maxDiskWriteBytesPerSecond),
        maxNetworkConnections == UNLIMITED ? "unlimited" : maxNetworkConnections,
        maxExecutionTime != null ? maxExecutionTime.toString() : "unlimited",
        maxCpuTime != null ? maxCpuTime.toString() : "unlimited",
        maxWallClockTime != null ? maxWallClockTime.toString() : "unlimited",
        formatBytes(maxFileSize),
        formatBytes(maxDiskSpaceUsage));
  }

  /**
   * Formats a byte count for human-readable display.
   *
   * @param bytes the byte count
   * @return formatted byte count string
   */
  private String formatBytes(final long bytes) {
    if (bytes == UNLIMITED) {
      return "unlimited";
    }

    if (bytes < 1024) {
      return bytes + " B";
    }

    final String[] units = {"B", "KB", "MB", "GB", "TB"};
    int unitIndex = 0;
    double size = bytes;

    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024;
      unitIndex++;
    }

    return String.format("%.1f %s", size, units[unitIndex]);
  }

  /** Builder for creating WASI resource limits. */
  public static final class Builder {

    private long maxMemoryBytes = UNLIMITED;
    private int maxFileDescriptors = (int) UNLIMITED;
    private long maxDiskReadsPerSecond = UNLIMITED;
    private long maxDiskWritesPerSecond = UNLIMITED;
    private long maxDiskReadBytesPerSecond = UNLIMITED;
    private long maxDiskWriteBytesPerSecond = UNLIMITED;
    private int maxNetworkConnections = (int) UNLIMITED;
    private Duration maxExecutionTime = null;
    private Duration maxCpuTime = null;
    private Duration maxWallClockTime = null;
    private long maxFileSize = UNLIMITED;
    private long maxDiskSpaceUsage = UNLIMITED;

    private Builder() {}

    /**
     * Sets the maximum memory usage in bytes.
     *
     * @param maxMemoryBytes the maximum memory bytes (negative for unlimited)
     * @return this builder for method chaining
     */
    public Builder withMaxMemoryBytes(final long maxMemoryBytes) {
      this.maxMemoryBytes = maxMemoryBytes;
      return this;
    }

    /**
     * Sets the maximum number of open file descriptors.
     *
     * @param maxFileDescriptors the maximum file descriptors (negative for unlimited)
     * @return this builder for method chaining
     */
    public Builder withMaxFileDescriptors(final int maxFileDescriptors) {
      this.maxFileDescriptors = maxFileDescriptors;
      return this;
    }

    /**
     * Sets the maximum disk read operations per second.
     *
     * @param maxDiskReadsPerSecond the maximum disk reads per second (negative for unlimited)
     * @return this builder for method chaining
     */
    public Builder withMaxDiskReadsPerSecond(final long maxDiskReadsPerSecond) {
      this.maxDiskReadsPerSecond = maxDiskReadsPerSecond;
      return this;
    }

    /**
     * Sets the maximum disk write operations per second.
     *
     * @param maxDiskWritesPerSecond the maximum disk writes per second (negative for unlimited)
     * @return this builder for method chaining
     */
    public Builder withMaxDiskWritesPerSecond(final long maxDiskWritesPerSecond) {
      this.maxDiskWritesPerSecond = maxDiskWritesPerSecond;
      return this;
    }

    /**
     * Sets the maximum bytes read from disk per second.
     *
     * @param maxDiskReadBytesPerSecond the maximum disk read bytes per second (negative for
     *     unlimited)
     * @return this builder for method chaining
     */
    public Builder withMaxDiskReadBytesPerSecond(final long maxDiskReadBytesPerSecond) {
      this.maxDiskReadBytesPerSecond = maxDiskReadBytesPerSecond;
      return this;
    }

    /**
     * Sets the maximum bytes written to disk per second.
     *
     * @param maxDiskWriteBytesPerSecond the maximum disk write bytes per second (negative for
     *     unlimited)
     * @return this builder for method chaining
     */
    public Builder withMaxDiskWriteBytesPerSecond(final long maxDiskWriteBytesPerSecond) {
      this.maxDiskWriteBytesPerSecond = maxDiskWriteBytesPerSecond;
      return this;
    }

    /**
     * Sets the maximum number of network connections.
     *
     * @param maxNetworkConnections the maximum network connections (negative for unlimited)
     * @return this builder for method chaining
     */
    public Builder withMaxNetworkConnections(final int maxNetworkConnections) {
      this.maxNetworkConnections = maxNetworkConnections;
      return this;
    }

    /**
     * Sets the maximum execution time.
     *
     * @param maxExecutionTime the maximum execution time (null for unlimited)
     * @return this builder for method chaining
     */
    public Builder withMaxExecutionTime(final Duration maxExecutionTime) {
      this.maxExecutionTime = maxExecutionTime;
      return this;
    }

    /**
     * Sets the maximum CPU time usage.
     *
     * @param maxCpuTime the maximum CPU time (null for unlimited)
     * @return this builder for method chaining
     */
    public Builder withMaxCpuTime(final Duration maxCpuTime) {
      this.maxCpuTime = maxCpuTime;
      return this;
    }

    /**
     * Sets the maximum wall clock time.
     *
     * @param maxWallClockTime the maximum wall clock time (null for unlimited)
     * @return this builder for method chaining
     */
    public Builder withMaxWallClockTime(final Duration maxWallClockTime) {
      this.maxWallClockTime = maxWallClockTime;
      return this;
    }

    /**
     * Sets the maximum size of individual files that can be created.
     *
     * @param maxFileSize the maximum file size (negative for unlimited)
     * @return this builder for method chaining
     */
    public Builder withMaxFileSize(final long maxFileSize) {
      this.maxFileSize = maxFileSize;
      return this;
    }

    /**
     * Sets the maximum total disk space that can be used.
     *
     * @param maxDiskSpaceUsage the maximum disk space usage (negative for unlimited)
     * @return this builder for method chaining
     */
    public Builder withMaxDiskSpaceUsage(final long maxDiskSpaceUsage) {
      this.maxDiskSpaceUsage = maxDiskSpaceUsage;
      return this;
    }

    /**
     * Builds the resource limits.
     *
     * @return the configured resource limits
     */
    public WasiResourceLimits build() {
      return new WasiResourceLimits(this);
    }
  }
}
