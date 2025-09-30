package ai.tegmentum.wasmtime4j;

/**
 * Component resource limits interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ComponentResourceLimits {

  /**
   * Gets memory limits.
   *
   * @return memory limits
   */
  MemoryLimits getMemoryLimits();

  /**
   * Gets execution limits.
   *
   * @return execution limits
   */
  ExecutionLimits getExecutionLimits();

  /**
   * Gets I/O limits.
   *
   * @return I/O limits
   */
  IoLimits getIoLimits();

  /**
   * Gets network limits.
   *
   * @return network limits
   */
  NetworkLimits getNetworkLimits();

  /**
   * Gets file system limits.
   *
   * @return file system limits
   */
  FileSystemLimits getFileSystemLimits();

  /**
   * Validates resource usage against limits.
   *
   * @param usage current usage
   * @return validation result
   */
  ValidationResult validate(ResourceUsage usage);

  /** Memory limits interface. */
  interface MemoryLimits {
    /**
     * Gets maximum heap size.
     *
     * @return max heap size in bytes
     */
    long getMaxHeapSize();

    /**
     * Gets maximum stack size.
     *
     * @return max stack size in bytes
     */
    long getMaxStackSize();

    /**
     * Gets maximum total memory.
     *
     * @return max total memory in bytes
     */
    long getMaxTotalMemory();
  }

  /** Execution limits interface. */
  interface ExecutionLimits {
    /**
     * Gets maximum execution time.
     *
     * @return max time in milliseconds
     */
    long getMaxExecutionTime();

    /**
     * Gets maximum fuel consumption.
     *
     * @return max fuel
     */
    long getMaxFuel();

    /**
     * Gets maximum instruction count.
     *
     * @return max instructions
     */
    long getMaxInstructions();
  }

  /** I/O limits interface. */
  interface IoLimits {
    /**
     * Gets maximum read operations per second.
     *
     * @return max read ops per second
     */
    int getMaxReadOpsPerSecond();

    /**
     * Gets maximum write operations per second.
     *
     * @return max write ops per second
     */
    int getMaxWriteOpsPerSecond();

    /**
     * Gets maximum bytes read per second.
     *
     * @return max bytes per second
     */
    long getMaxBytesReadPerSecond();

    /**
     * Gets maximum bytes written per second.
     *
     * @return max bytes per second
     */
    long getMaxBytesWrittenPerSecond();
  }

  /** Network limits interface. */
  interface NetworkLimits {
    /**
     * Gets maximum concurrent connections.
     *
     * @return max connections
     */
    int getMaxConnections();

    /**
     * Gets maximum bandwidth.
     *
     * @return max bandwidth in bytes per second
     */
    long getMaxBandwidth();

    /**
     * Gets maximum requests per second.
     *
     * @return max requests per second
     */
    int getMaxRequestsPerSecond();
  }

  /** File system limits interface. */
  interface FileSystemLimits {
    /**
     * Gets maximum open files.
     *
     * @return max open files
     */
    int getMaxOpenFiles();

    /**
     * Gets maximum disk usage.
     *
     * @return max disk usage in bytes
     */
    long getMaxDiskUsage();

    /**
     * Gets maximum file size.
     *
     * @return max file size in bytes
     */
    long getMaxFileSize();
  }

  /** Resource usage interface. */
  interface ResourceUsage {
    /**
     * Gets current memory usage.
     *
     * @return memory usage in bytes
     */
    long getCurrentMemoryUsage();

    /**
     * Gets current execution time.
     *
     * @return execution time in milliseconds
     */
    long getCurrentExecutionTime();

    /**
     * Gets current fuel consumption.
     *
     * @return fuel consumed
     */
    long getCurrentFuelConsumption();

    /**
     * Gets current instruction count.
     *
     * @return instruction count
     */
    long getCurrentInstructionCount();

    /**
     * Gets current I/O usage.
     *
     * @return I/O usage
     */
    IoUsage getCurrentIoUsage();

    /**
     * Gets current network usage.
     *
     * @return network usage
     */
    NetworkUsage getCurrentNetworkUsage();

    /**
     * Gets current file system usage.
     *
     * @return file system usage
     */
    FileSystemUsage getCurrentFileSystemUsage();
  }

  /** I/O usage interface. */
  interface IoUsage {
    /**
     * Gets read operations count.
     *
     * @return read operations
     */
    long getReadOperations();

    /**
     * Gets write operations count.
     *
     * @return write operations
     */
    long getWriteOperations();

    /**
     * Gets bytes read.
     *
     * @return bytes read
     */
    long getBytesRead();

    /**
     * Gets bytes written.
     *
     * @return bytes written
     */
    long getBytesWritten();
  }

  /** Network usage interface. */
  interface NetworkUsage {
    /**
     * Gets active connection count.
     *
     * @return active connections
     */
    int getActiveConnections();

    /**
     * Gets bandwidth usage.
     *
     * @return bandwidth in bytes per second
     */
    long getBandwidthUsage();

    /**
     * Gets request count.
     *
     * @return request count
     */
    long getRequestCount();
  }

  /** File system usage interface. */
  interface FileSystemUsage {
    /**
     * Gets open file count.
     *
     * @return open files
     */
    int getOpenFiles();

    /**
     * Gets disk usage.
     *
     * @return disk usage in bytes
     */
    long getDiskUsage();

    /**
     * Gets largest file size.
     *
     * @return largest file size in bytes
     */
    long getLargestFileSize();
  }

  /** Validation result interface. */
  interface ValidationResult {
    /**
     * Checks if validation passed.
     *
     * @return true if valid
     */
    boolean isValid();

    /**
     * Gets validation violations.
     *
     * @return list of violations
     */
    java.util.List<LimitViolation> getViolations();
  }

  /** Limit violation interface. */
  interface LimitViolation {
    /**
     * Gets violation type.
     *
     * @return violation type
     */
    ViolationType getType();

    /**
     * Gets limit name.
     *
     * @return limit name
     */
    String getLimitName();

    /**
     * Gets current value.
     *
     * @return current value
     */
    long getCurrentValue();

    /**
     * Gets limit value.
     *
     * @return limit value
     */
    long getLimitValue();

    /**
     * Gets violation message.
     *
     * @return violation message
     */
    String getMessage();
  }

  /** Violation type enumeration. */
  enum ViolationType {
    /** Memory limit violation. */
    MEMORY_LIMIT,
    /** Execution time violation. */
    EXECUTION_TIME,
    /** Fuel limit violation. */
    FUEL_LIMIT,
    /** Instruction limit violation. */
    INSTRUCTION_LIMIT,
    /** I/O limit violation. */
    IO_LIMIT,
    /** Network limit violation. */
    NETWORK_LIMIT,
    /** File system limit violation. */
    FILESYSTEM_LIMIT
  }
}
