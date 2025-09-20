package ai.tegmentum.wasmtime4j.security;

import java.time.Duration;
import java.util.Objects;

/**
 * Resource limits for WebAssembly execution to prevent resource exhaustion attacks.
 *
 * <p>Defines comprehensive resource constraints including memory, execution time,
 * instruction counts, and system resource usage limits.
 *
 * @since 1.0.0
 */
public final class ResourceLimits {

    private final long maxMemoryBytes;
    private final Duration maxExecutionTime;
    private final long maxInstructions;
    private final int maxInstances;
    private final int maxModules;
    private final int maxFileDescriptors;
    private final int maxNetworkConnections;
    private final int maxThreads;
    private final long maxStackSize;
    private final long maxHeapSize;
    private final double maxCpuUsage;
    private final long maxDiskUsage;
    private final int maxCallDepth;
    private final boolean enablePreemption;

    private ResourceLimits(final Builder builder) {
        this.maxMemoryBytes = builder.maxMemoryBytes;
        this.maxExecutionTime = builder.maxExecutionTime;
        this.maxInstructions = builder.maxInstructions;
        this.maxInstances = builder.maxInstances;
        this.maxModules = builder.maxModules;
        this.maxFileDescriptors = builder.maxFileDescriptors;
        this.maxNetworkConnections = builder.maxNetworkConnections;
        this.maxThreads = builder.maxThreads;
        this.maxStackSize = builder.maxStackSize;
        this.maxHeapSize = builder.maxHeapSize;
        this.maxCpuUsage = builder.maxCpuUsage;
        this.maxDiskUsage = builder.maxDiskUsage;
        this.maxCallDepth = builder.maxCallDepth;
        this.enablePreemption = builder.enablePreemption;
    }

    /**
     * Creates minimal resource limits suitable for untrusted code.
     *
     * @return minimal resource limits
     */
    public static ResourceLimits minimal() {
        return builder()
            .withMaxMemory(16 * 1024 * 1024) // 16MB
            .withMaxExecutionTime(Duration.ofSeconds(5))
            .withMaxInstructions(1_000_000)
            .withMaxInstances(1)
            .withMaxModules(1)
            .withMaxFileDescriptors(0)
            .withMaxNetworkConnections(0)
            .withMaxThreads(1)
            .withMaxCallDepth(100)
            .withPreemption(true)
            .build();
    }

    /**
     * Creates standard resource limits for general use.
     *
     * @return standard resource limits
     */
    public static ResourceLimits standard() {
        return builder()
            .withMaxMemory(128 * 1024 * 1024) // 128MB
            .withMaxExecutionTime(Duration.ofSeconds(30))
            .withMaxInstructions(10_000_000)
            .withMaxInstances(10)
            .withMaxModules(10)
            .withMaxFileDescriptors(10)
            .withMaxNetworkConnections(5)
            .withMaxThreads(4)
            .withMaxCallDepth(1000)
            .withPreemption(true)
            .build();
    }

    /**
     * Creates generous resource limits for trusted code.
     *
     * @return generous resource limits
     */
    public static ResourceLimits generous() {
        return builder()
            .withMaxMemory(1024 * 1024 * 1024) // 1GB
            .withMaxExecutionTime(Duration.ofMinutes(5))
            .withMaxInstructions(100_000_000)
            .withMaxInstances(100)
            .withMaxModules(100)
            .withMaxFileDescriptors(100)
            .withMaxNetworkConnections(50)
            .withMaxThreads(16)
            .withMaxCallDepth(10000)
            .withPreemption(false)
            .build();
    }

    /**
     * Creates unlimited resource limits (use with extreme caution).
     *
     * @return unlimited resource limits
     */
    public static ResourceLimits unlimited() {
        return builder()
            .withMaxMemory(Long.MAX_VALUE)
            .withMaxExecutionTime(Duration.ofDays(1))
            .withMaxInstructions(Long.MAX_VALUE)
            .withMaxInstances(Integer.MAX_VALUE)
            .withMaxModules(Integer.MAX_VALUE)
            .withMaxFileDescriptors(Integer.MAX_VALUE)
            .withMaxNetworkConnections(Integer.MAX_VALUE)
            .withMaxThreads(Integer.MAX_VALUE)
            .withMaxCallDepth(Integer.MAX_VALUE)
            .withPreemption(false)
            .build();
    }

    /**
     * Creates a new resource limits builder.
     *
     * @return resource limits builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public long getMaxMemoryBytes() { return maxMemoryBytes; }
    public Duration getMaxExecutionTime() { return maxExecutionTime; }
    public long getMaxInstructions() { return maxInstructions; }
    public int getMaxInstances() { return maxInstances; }
    public int getMaxModules() { return maxModules; }
    public int getMaxFileDescriptors() { return maxFileDescriptors; }
    public int getMaxNetworkConnections() { return maxNetworkConnections; }
    public int getMaxThreads() { return maxThreads; }
    public long getMaxStackSize() { return maxStackSize; }
    public long getMaxHeapSize() { return maxHeapSize; }
    public double getMaxCpuUsage() { return maxCpuUsage; }
    public long getMaxDiskUsage() { return maxDiskUsage; }
    public int getMaxCallDepth() { return maxCallDepth; }
    public boolean isPreemptionEnabled() { return enablePreemption; }

    /**
     * Checks if the given memory size is within limits.
     *
     * @param memoryBytes memory size in bytes
     * @return true if within limits
     */
    public boolean isMemoryWithinLimits(final long memoryBytes) {
        return memoryBytes <= maxMemoryBytes;
    }

    /**
     * Checks if the given execution time is within limits.
     *
     * @param executionTime execution duration
     * @return true if within limits
     */
    public boolean isExecutionTimeWithinLimits(final Duration executionTime) {
        return executionTime.compareTo(maxExecutionTime) <= 0;
    }

    /**
     * Checks if the given instruction count is within limits.
     *
     * @param instructions instruction count
     * @return true if within limits
     */
    public boolean isInstructionCountWithinLimits(final long instructions) {
        return instructions <= maxInstructions;
    }

    /**
     * Creates a more restrictive version of these limits.
     *
     * @param other the limits to intersect with
     * @return more restrictive limits
     */
    public ResourceLimits restrictTo(final ResourceLimits other) {
        return builder()
            .withMaxMemory(Math.min(this.maxMemoryBytes, other.maxMemoryBytes))
            .withMaxExecutionTime(this.maxExecutionTime.compareTo(other.maxExecutionTime) < 0
                ? this.maxExecutionTime : other.maxExecutionTime)
            .withMaxInstructions(Math.min(this.maxInstructions, other.maxInstructions))
            .withMaxInstances(Math.min(this.maxInstances, other.maxInstances))
            .withMaxModules(Math.min(this.maxModules, other.maxModules))
            .withMaxFileDescriptors(Math.min(this.maxFileDescriptors, other.maxFileDescriptors))
            .withMaxNetworkConnections(Math.min(this.maxNetworkConnections, other.maxNetworkConnections))
            .withMaxThreads(Math.min(this.maxThreads, other.maxThreads))
            .withMaxCallDepth(Math.min(this.maxCallDepth, other.maxCallDepth))
            .withPreemption(this.enablePreemption || other.enablePreemption)
            .build();
    }

    /**
     * Validates these resource limits for consistency and safety.
     *
     * @throws IllegalArgumentException if limits are invalid
     */
    public void validate() {
        if (maxMemoryBytes <= 0) {
            throw new IllegalArgumentException("Max memory must be positive");
        }
        if (maxExecutionTime.isNegative() || maxExecutionTime.isZero()) {
            throw new IllegalArgumentException("Max execution time must be positive");
        }
        if (maxInstructions <= 0) {
            throw new IllegalArgumentException("Max instructions must be positive");
        }
        if (maxInstances <= 0) {
            throw new IllegalArgumentException("Max instances must be positive");
        }
        if (maxModules <= 0) {
            throw new IllegalArgumentException("Max modules must be positive");
        }
        if (maxFileDescriptors < 0) {
            throw new IllegalArgumentException("Max file descriptors cannot be negative");
        }
        if (maxNetworkConnections < 0) {
            throw new IllegalArgumentException("Max network connections cannot be negative");
        }
        if (maxThreads <= 0) {
            throw new IllegalArgumentException("Max threads must be positive");
        }
        if (maxCallDepth <= 0) {
            throw new IllegalArgumentException("Max call depth must be positive");
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ResourceLimits that = (ResourceLimits) o;
        return maxMemoryBytes == that.maxMemoryBytes &&
               maxInstructions == that.maxInstructions &&
               maxInstances == that.maxInstances &&
               maxModules == that.maxModules &&
               maxFileDescriptors == that.maxFileDescriptors &&
               maxNetworkConnections == that.maxNetworkConnections &&
               maxThreads == that.maxThreads &&
               maxStackSize == that.maxStackSize &&
               maxHeapSize == that.maxHeapSize &&
               Double.compare(that.maxCpuUsage, maxCpuUsage) == 0 &&
               maxDiskUsage == that.maxDiskUsage &&
               maxCallDepth == that.maxCallDepth &&
               enablePreemption == that.enablePreemption &&
               Objects.equals(maxExecutionTime, that.maxExecutionTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxMemoryBytes, maxExecutionTime, maxInstructions, maxInstances,
                           maxModules, maxFileDescriptors, maxNetworkConnections, maxThreads,
                           maxStackSize, maxHeapSize, maxCpuUsage, maxDiskUsage, maxCallDepth,
                           enablePreemption);
    }

    @Override
    public String toString() {
        return "ResourceLimits{" +
               "maxMemoryBytes=" + maxMemoryBytes +
               ", maxExecutionTime=" + maxExecutionTime +
               ", maxInstructions=" + maxInstructions +
               ", maxInstances=" + maxInstances +
               ", maxModules=" + maxModules +
               ", maxFileDescriptors=" + maxFileDescriptors +
               ", maxNetworkConnections=" + maxNetworkConnections +
               ", maxThreads=" + maxThreads +
               ", enablePreemption=" + enablePreemption +
               '}';
    }

    /**
     * Builder for ResourceLimits.
     */
    public static final class Builder {
        private long maxMemoryBytes = 64 * 1024 * 1024; // 64MB default
        private Duration maxExecutionTime = Duration.ofSeconds(10);
        private long maxInstructions = 1_000_000;
        private int maxInstances = 1;
        private int maxModules = 1;
        private int maxFileDescriptors = 0;
        private int maxNetworkConnections = 0;
        private int maxThreads = 1;
        private long maxStackSize = 1024 * 1024; // 1MB
        private long maxHeapSize = 64 * 1024 * 1024; // 64MB
        private double maxCpuUsage = 1.0; // 100%
        private long maxDiskUsage = 100 * 1024 * 1024; // 100MB
        private int maxCallDepth = 1000;
        private boolean enablePreemption = true;

        public Builder withMaxMemory(final long maxMemoryBytes) {
            this.maxMemoryBytes = maxMemoryBytes;
            return this;
        }

        public Builder withMaxExecutionTime(final Duration maxExecutionTime) {
            this.maxExecutionTime = maxExecutionTime;
            return this;
        }

        public Builder withMaxInstructions(final long maxInstructions) {
            this.maxInstructions = maxInstructions;
            return this;
        }

        public Builder withMaxInstances(final int maxInstances) {
            this.maxInstances = maxInstances;
            return this;
        }

        public Builder withMaxModules(final int maxModules) {
            this.maxModules = maxModules;
            return this;
        }

        public Builder withMaxFileDescriptors(final int maxFileDescriptors) {
            this.maxFileDescriptors = maxFileDescriptors;
            return this;
        }

        public Builder withMaxNetworkConnections(final int maxNetworkConnections) {
            this.maxNetworkConnections = maxNetworkConnections;
            return this;
        }

        public Builder withMaxThreads(final int maxThreads) {
            this.maxThreads = maxThreads;
            return this;
        }

        public Builder withMaxStackSize(final long maxStackSize) {
            this.maxStackSize = maxStackSize;
            return this;
        }

        public Builder withMaxHeapSize(final long maxHeapSize) {
            this.maxHeapSize = maxHeapSize;
            return this;
        }

        public Builder withMaxCpuUsage(final double maxCpuUsage) {
            this.maxCpuUsage = maxCpuUsage;
            return this;
        }

        public Builder withMaxDiskUsage(final long maxDiskUsage) {
            this.maxDiskUsage = maxDiskUsage;
            return this;
        }

        public Builder withMaxCallDepth(final int maxCallDepth) {
            this.maxCallDepth = maxCallDepth;
            return this;
        }

        public Builder withPreemption(final boolean enablePreemption) {
            this.enablePreemption = enablePreemption;
            return this;
        }

        public ResourceLimits build() {
            final ResourceLimits limits = new ResourceLimits(this);
            limits.validate();
            return limits;
        }
    }
}