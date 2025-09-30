package ai.tegmentum.wasmtime4j.execution;

import java.time.Duration;

/**
 * Execution quotas and resource limits for WebAssembly execution control.
 *
 * <p>Defines comprehensive resource quotas including CPU time, memory usage,
 * I/O operations, and custom resource limits with enforcement policies.
 *
 * @since 1.0.0
 */
public final class ExecutionQuotas {

  private final long fuelQuota;
  private final Duration cpuTimeQuota;
  private final long memoryQuota;
  private final long ioOperationQuota;
  private final long networkRequestQuota;
  private final double ioRateLimit;
  private final QuotaEnforcementPolicy enforcementPolicy;
  private final java.util.Map<String, Long> customQuotas;
  private final boolean enableDynamicAdjustment;
  private final double overallocationRatio;
  private final Duration quotaResetPeriod;

  private ExecutionQuotas(final Builder builder) {
    this.fuelQuota = builder.fuelQuota;
    this.cpuTimeQuota = builder.cpuTimeQuota;
    this.memoryQuota = builder.memoryQuota;
    this.ioOperationQuota = builder.ioOperationQuota;
    this.networkRequestQuota = builder.networkRequestQuota;
    this.ioRateLimit = builder.ioRateLimit;
    this.enforcementPolicy = builder.enforcementPolicy;
    this.customQuotas = java.util.Map.copyOf(builder.customQuotas);
    this.enableDynamicAdjustment = builder.enableDynamicAdjustment;
    this.overallocationRatio = builder.overallocationRatio;
    this.quotaResetPeriod = builder.quotaResetPeriod;
  }

  public long getFuelQuota() {
    return fuelQuota;
  }

  public Duration getCpuTimeQuota() {
    return cpuTimeQuota;
  }

  public long getMemoryQuota() {
    return memoryQuota;
  }

  public long getIoOperationQuota() {
    return ioOperationQuota;
  }

  public long getNetworkRequestQuota() {
    return networkRequestQuota;
  }

  public double getIoRateLimit() {
    return ioRateLimit;
  }

  public QuotaEnforcementPolicy getEnforcementPolicy() {
    return enforcementPolicy;
  }

  public java.util.Map<String, Long> getCustomQuotas() {
    return customQuotas;
  }

  public boolean isEnableDynamicAdjustment() {
    return enableDynamicAdjustment;
  }

  public double getOverallocationRatio() {
    return overallocationRatio;
  }

  public Duration getQuotaResetPeriod() {
    return quotaResetPeriod;
  }

  /**
   * Creates a new builder for execution quotas.
   *
   * @return new quotas builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates default execution quotas suitable for general use.
   *
   * @return default execution quotas
   */
  public static ExecutionQuotas createDefault() {
    return builder()
        .fuelQuota(1000000L)
        .cpuTimeQuota(Duration.ofSeconds(30))
        .memoryQuota(64 * 1024 * 1024L) // 64MB
        .ioOperationQuota(10000L)
        .networkRequestQuota(100L)
        .ioRateLimit(1000.0) // operations per second
        .enforcementPolicy(QuotaEnforcementPolicy.STRICT)
        .enableDynamicAdjustment(true)
        .overallocationRatio(1.2)
        .quotaResetPeriod(Duration.ofMinutes(5))
        .build();
  }

  public static final class Builder {
    private long fuelQuota = 0L;
    private Duration cpuTimeQuota = Duration.ZERO;
    private long memoryQuota = 0L;
    private long ioOperationQuota = 0L;
    private long networkRequestQuota = 0L;
    private double ioRateLimit = 0.0;
    private QuotaEnforcementPolicy enforcementPolicy = QuotaEnforcementPolicy.STRICT;
    private java.util.Map<String, Long> customQuotas = new java.util.HashMap<>();
    private boolean enableDynamicAdjustment = false;
    private double overallocationRatio = 1.0;
    private Duration quotaResetPeriod = Duration.ofHours(1);

    public Builder fuelQuota(final long quota) {
      this.fuelQuota = quota;
      return this;
    }

    public Builder cpuTimeQuota(final Duration quota) {
      this.cpuTimeQuota = quota;
      return this;
    }

    public Builder memoryQuota(final long quota) {
      this.memoryQuota = quota;
      return this;
    }

    public Builder ioOperationQuota(final long quota) {
      this.ioOperationQuota = quota;
      return this;
    }

    public Builder networkRequestQuota(final long quota) {
      this.networkRequestQuota = quota;
      return this;
    }

    public Builder ioRateLimit(final double limit) {
      this.ioRateLimit = limit;
      return this;
    }

    public Builder enforcementPolicy(final QuotaEnforcementPolicy policy) {
      this.enforcementPolicy = policy;
      return this;
    }

    public Builder customQuotas(final java.util.Map<String, Long> quotas) {
      this.customQuotas.clear();
      this.customQuotas.putAll(quotas);
      return this;
    }

    public Builder addCustomQuota(final String name, final long quota) {
      this.customQuotas.put(name, quota);
      return this;
    }

    public Builder enableDynamicAdjustment(final boolean enable) {
      this.enableDynamicAdjustment = enable;
      return this;
    }

    public Builder overallocationRatio(final double ratio) {
      this.overallocationRatio = ratio;
      return this;
    }

    public Builder quotaResetPeriod(final Duration period) {
      this.quotaResetPeriod = period;
      return this;
    }

    public ExecutionQuotas build() {
      return new ExecutionQuotas(this);
    }
  }

  public enum QuotaEnforcementPolicy {
    STRICT,     // Hard limits, execution fails when exceeded
    THROTTLED,  // Slow down execution when approaching limits
    GRACEFUL,   // Allow temporary overages with warnings
    ADAPTIVE    // Dynamically adjust limits based on system load
  }
}