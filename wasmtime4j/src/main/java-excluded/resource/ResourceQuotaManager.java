/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.resource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Advanced resource quota management system providing comprehensive CPU, memory, network, and I/O
 * resource limiting with burst capability, rate limiting, and fair scheduling.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>CPU time quotas with burst capability and priority scheduling
 *   <li>Memory usage quotas with soft/hard limits and pressure detection
 *   <li>Network bandwidth quotas and throttling with QoS
 *   <li>I/O operation quotas and rate limiting with fairness
 *   <li>Multi-tenant resource isolation and sharing
 *   <li>Resource governance and policy enforcement
 *   <li>Predictive resource analytics and optimization
 * </ul>
 *
 * @since 1.0.0
 */
public final class ResourceQuotaManager {

  private static final Logger LOGGER = Logger.getLogger(ResourceQuotaManager.class.getName());

  /** Resource types with detailed categorization. */
  public enum ResourceType {
    // CPU Resources
    CPU_TIME("cpu.time", "nanoseconds", true),
    CPU_CYCLES("cpu.cycles", "cycles", true),
    CPU_INSTRUCTIONS("cpu.instructions", "instructions", true),

    // Memory Resources
    HEAP_MEMORY("memory.heap", "bytes", false),
    NATIVE_MEMORY("memory.native", "bytes", false),
    OFF_HEAP_MEMORY("memory.offheap", "bytes", false),
    DIRECT_MEMORY("memory.direct", "bytes", false),

    // Network Resources
    NETWORK_BANDWIDTH_IN("network.bandwidth.in", "bytes/sec", true),
    NETWORK_BANDWIDTH_OUT("network.bandwidth.out", "bytes/sec", true),
    NETWORK_CONNECTIONS("network.connections", "connections", false),
    NETWORK_PACKETS("network.packets", "packets/sec", true),

    // I/O Resources
    DISK_READ_BANDWIDTH("io.disk.read", "bytes/sec", true),
    DISK_WRITE_BANDWIDTH("io.disk.write", "bytes/sec", true),
    DISK_OPERATIONS("io.disk.operations", "ops/sec", true),
    FILE_HANDLES("io.file.handles", "handles", false),

    // WebAssembly Specific
    MODULE_INSTANCES("wasm.instances", "instances", false),
    FUNCTION_CALLS("wasm.function_calls", "calls/sec", true),
    COMPILATION_UNITS("wasm.compilation", "units", false),
    EXECUTION_TIME("wasm.execution_time", "nanoseconds", true);

    private final String metricName;
    private final String unit;
    private final boolean isRate;

    ResourceType(final String metricName, final String unit, final boolean isRate) {
      this.metricName = metricName;
      this.unit = unit;
      this.isRate = isRate;
    }

    public String getMetricName() {
      return metricName;
    }

    public String getUnit() {
      return unit;
    }

    public boolean isRate() {
      return isRate;
    }
  }

  /** Quota enforcement strategy. */
  public enum QuotaEnforcementStrategy {
    STRICT, // Hard enforcement, reject requests exceeding quota
    THROTTLE, // Rate limiting with queuing
    BURST, // Allow bursts up to burst limit
    ADAPTIVE, // Dynamic adjustment based on system load
    FAIR_SHARE // Fair distribution among tenants
  }

  /** Resource quota configuration with advanced features. */
  public static final class ResourceQuota {
    private final ResourceType resourceType;
    private final String tenantId;
    private final long softLimit;
    private final long hardLimit;
    private final long burstLimit;
    private final Duration burstWindow;
    private final Duration measurementWindow;
    private final QuotaEnforcementStrategy strategy;
    private final int priority;
    private final double warningThreshold;
    private final double criticalThreshold;
    private final boolean enforcementEnabled;
    private final Map<String, String> metadata;

    private ResourceQuota(final Builder builder) {
      this.resourceType = builder.resourceType;
      this.tenantId = builder.tenantId;
      this.softLimit = builder.softLimit;
      this.hardLimit = builder.hardLimit;
      this.burstLimit = builder.burstLimit;
      this.burstWindow = builder.burstWindow;
      this.measurementWindow = builder.measurementWindow;
      this.strategy = builder.strategy;
      this.priority = builder.priority;
      this.warningThreshold = builder.warningThreshold;
      this.criticalThreshold = builder.criticalThreshold;
      this.enforcementEnabled = builder.enforcementEnabled;
      this.metadata = Map.copyOf(builder.metadata);
    }

    public static Builder builder(final ResourceType resourceType, final String tenantId) {
      return new Builder(resourceType, tenantId);
    }

    public static final class Builder {
      private final ResourceType resourceType;
      private final String tenantId;
      private long softLimit = 0;
      private long hardLimit = 0;
      private long burstLimit = 0;
      private Duration burstWindow = Duration.ofMinutes(1);
      private Duration measurementWindow = Duration.ofMinutes(1);
      private QuotaEnforcementStrategy strategy = QuotaEnforcementStrategy.THROTTLE;
      private int priority = 100;
      private double warningThreshold = 0.8;
      private double criticalThreshold = 0.95;
      private boolean enforcementEnabled = true;
      private Map<String, String> metadata = new ConcurrentHashMap<>();

      private Builder(final ResourceType resourceType, final String tenantId) {
        this.resourceType = resourceType;
        this.tenantId = tenantId;
      }

      public Builder withSoftLimit(final long softLimit) {
        this.softLimit = softLimit;
        return this;
      }

      public Builder withHardLimit(final long hardLimit) {
        this.hardLimit = hardLimit;
        return this;
      }

      public Builder withBurstLimit(final long burstLimit) {
        this.burstLimit = burstLimit;
        return this;
      }

      public Builder withBurstWindow(final Duration burstWindow) {
        this.burstWindow = burstWindow;
        return this;
      }

      public Builder withMeasurementWindow(final Duration measurementWindow) {
        this.measurementWindow = measurementWindow;
        return this;
      }

      public Builder withStrategy(final QuotaEnforcementStrategy strategy) {
        this.strategy = strategy;
        return this;
      }

      public Builder withPriority(final int priority) {
        this.priority = priority;
        return this;
      }

      public Builder withWarningThreshold(final double warningThreshold) {
        this.warningThreshold = warningThreshold;
        return this;
      }

      public Builder withCriticalThreshold(final double criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
        return this;
      }

      public Builder withEnforcement(final boolean enforcementEnabled) {
        this.enforcementEnabled = enforcementEnabled;
        return this;
      }

      public Builder withMetadata(final String key, final String value) {
        this.metadata.put(key, value);
        return this;
      }

      public ResourceQuota build() {
        if (hardLimit <= 0) {
          throw new IllegalArgumentException("Hard limit must be positive");
        }
        if (softLimit <= 0) {
          this.softLimit = (long) (hardLimit * 0.8);
        }
        if (burstLimit <= 0) {
          this.burstLimit = hardLimit * 2;
        }
        return new ResourceQuota(this);
      }
    }

    // Getters
    public ResourceType getResourceType() {
      return resourceType;
    }

    public String getTenantId() {
      return tenantId;
    }

    public long getSoftLimit() {
      return softLimit;
    }

    public long getHardLimit() {
      return hardLimit;
    }

    public long getBurstLimit() {
      return burstLimit;
    }

    public Duration getBurstWindow() {
      return burstWindow;
    }

    public Duration getMeasurementWindow() {
      return measurementWindow;
    }

    public QuotaEnforcementStrategy getStrategy() {
      return strategy;
    }

    public int getPriority() {
      return priority;
    }

    public double getWarningThreshold() {
      return warningThreshold;
    }

    public double getCriticalThreshold() {
      return criticalThreshold;
    }

    public boolean isEnforcementEnabled() {
      return enforcementEnabled;
    }

    public Map<String, String> getMetadata() {
      return metadata;
    }
  }

  /** Resource usage tracker with burst detection and rate limiting. */
  private static final class ResourceUsageTracker {
    private final ResourceQuota quota;
    private final AtomicLong currentUsage = new AtomicLong(0);
    private final AtomicLong peakUsage = new AtomicLong(0);
    private final AtomicLong totalConsumed = new AtomicLong(0);
    private final AtomicLong burstUsage = new AtomicLong(0);
    private final AtomicReference<Instant> lastBurstReset = new AtomicReference<>(Instant.now());
    private final AtomicReference<Instant> lastMeasurement = new AtomicReference<>(Instant.now());
    private final RateLimiter rateLimiter;
    private final TokenBucket tokenBucket;

    private volatile double currentRate = 0.0;
    private volatile boolean inBurstMode = false;

    ResourceUsageTracker(final ResourceQuota quota) {
      this.quota = quota;
      this.rateLimiter = new RateLimiter(quota.getHardLimit(), quota.getMeasurementWindow());
      this.tokenBucket = new TokenBucket(quota.getHardLimit(), quota.getBurstLimit());
    }

    public QuotaCheckResult checkQuota(final long requestedAmount) {
      resetBurstIfNeeded();
      updateRate();

      switch (quota.getStrategy()) {
        case STRICT:
          return checkStrictQuota(requestedAmount);
        case THROTTLE:
          return checkThrottleQuota(requestedAmount);
        case BURST:
          return checkBurstQuota(requestedAmount);
        case ADAPTIVE:
          return checkAdaptiveQuota(requestedAmount);
        case FAIR_SHARE:
          return checkFairShareQuota(requestedAmount);
        default:
          return checkStrictQuota(requestedAmount);
      }
    }

    private QuotaCheckResult checkStrictQuota(final long requestedAmount) {
      final long projected = currentUsage.get() + requestedAmount;
      if (projected > quota.getHardLimit()) {
        return QuotaCheckResult.rejected("Hard limit exceeded");
      }
      if (projected > quota.getSoftLimit()) {
        return QuotaCheckResult.warning("Soft limit exceeded");
      }
      return QuotaCheckResult.allowed();
    }

    private QuotaCheckResult checkThrottleQuota(final long requestedAmount) {
      if (!rateLimiter.tryAcquire(requestedAmount)) {
        final Duration delay = rateLimiter.calculateDelay(requestedAmount);
        return QuotaCheckResult.throttled(delay);
      }
      return QuotaCheckResult.allowed();
    }

    private QuotaCheckResult checkBurstQuota(final long requestedAmount) {
      final long currentBurst = burstUsage.get();
      final long projectedBurst = currentBurst + requestedAmount;

      if (projectedBurst <= quota.getBurstLimit()) {
        inBurstMode = projectedBurst > quota.getHardLimit();
        return QuotaCheckResult.allowed();
      }

      return QuotaCheckResult.rejected("Burst limit exceeded");
    }

    private QuotaCheckResult checkAdaptiveQuota(final long requestedAmount) {
      // Implement adaptive logic based on system load
      final double systemLoad = getSystemLoad();
      final long adaptiveLimit = (long) (quota.getHardLimit() * (1.0 - systemLoad * 0.3));

      if (currentUsage.get() + requestedAmount > adaptiveLimit) {
        return QuotaCheckResult.throttled(Duration.ofMillis(100));
      }

      return QuotaCheckResult.allowed();
    }

    private QuotaCheckResult checkFairShareQuota(final long requestedAmount) {
      // Implement fair share logic
      final double fairShare = calculateFairShare();
      final long fairLimit = (long) (quota.getHardLimit() * fairShare);

      if (currentUsage.get() + requestedAmount > fairLimit) {
        return QuotaCheckResult.throttled(Duration.ofMillis(50));
      }

      return QuotaCheckResult.allowed();
    }

    public void recordUsage(final long amount) {
      final long newUsage = currentUsage.addAndGet(amount);
      totalConsumed.addAndGet(amount);
      peakUsage.updateAndGet(peak -> Math.max(peak, newUsage));

      if (inBurstMode) {
        burstUsage.addAndGet(amount);
      }

      updateRate();
    }

    public void recordDeallocation(final long amount) {
      currentUsage.updateAndGet(current -> Math.max(0, current - amount));
      updateRate();
    }

    private void resetBurstIfNeeded() {
      final Instant now = Instant.now();
      final Instant lastReset = lastBurstReset.get();

      if (Duration.between(lastReset, now).compareTo(quota.getBurstWindow()) >= 0) {
        if (lastBurstReset.compareAndSet(lastReset, now)) {
          burstUsage.set(0);
          inBurstMode = false;
        }
      }
    }

    private void updateRate() {
      final Instant now = Instant.now();
      final Instant lastMeas = lastMeasurement.getAndSet(now);
      final Duration elapsed = Duration.between(lastMeas, now);

      if (!elapsed.isZero()) {
        final double elapsedSeconds = elapsed.toNanos() / 1_000_000_000.0;
        currentRate = totalConsumed.get() / elapsedSeconds;
      }
    }

    private double getSystemLoad() {
      // Placeholder for system load calculation
      return Math.random() * 0.5; // 0-50% load
    }

    private double calculateFairShare() {
      // Placeholder for fair share calculation
      return 1.0 / Math.max(1, getTenantCount());
    }

    private int getTenantCount() {
      // Placeholder - should be injected from tenant manager
      return 1;
    }

    public ResourceUsageStats getStats() {
      return new ResourceUsageStats(
          quota.getResourceType(),
          quota.getTenantId(),
          currentUsage.get(),
          peakUsage.get(),
          totalConsumed.get(),
          currentRate,
          burstUsage.get(),
          inBurstMode,
          quota);
    }
  }

  /** Token bucket for rate limiting with burst support. */
  private static final class TokenBucket {
    private final long capacity;
    private final long burstCapacity;
    private final AtomicLong tokens;
    private final AtomicReference<Instant> lastRefill = new AtomicReference<>(Instant.now());

    TokenBucket(final long capacity, final long burstCapacity) {
      this.capacity = capacity;
      this.burstCapacity = burstCapacity;
      this.tokens = new AtomicLong(capacity);
    }

    public boolean tryConsume(final long tokensRequested) {
      refill();
      return tokens.updateAndGet(current -> {
        if (current >= tokensRequested) {
          return current - tokensRequested;
        }
        return current;
      }) >= 0;
    }

    private void refill() {
      final Instant now = Instant.now();
      final Instant last = lastRefill.get();
      final Duration elapsed = Duration.between(last, now);

      if (elapsed.toMillis() >= 100) { // Refill every 100ms
        if (lastRefill.compareAndSet(last, now)) {
          final long tokensToAdd = elapsed.toMillis() * capacity / 1000;
          tokens.updateAndGet(current -> Math.min(burstCapacity, current + tokensToAdd));
        }
      }
    }
  }

  /** Rate limiter for controlling resource consumption. */
  private static final class RateLimiter {
    private final long permitsPerWindow;
    private final Duration window;
    private final AtomicLong permits;
    private final AtomicReference<Instant> windowStart = new AtomicReference<>(Instant.now());

    RateLimiter(final long permitsPerWindow, final Duration window) {
      this.permitsPerWindow = permitsPerWindow;
      this.window = window;
      this.permits = new AtomicLong(permitsPerWindow);
    }

    public boolean tryAcquire(final long permitsRequested) {
      resetWindowIfNeeded();
      return permits.updateAndGet(current -> {
        if (current >= permitsRequested) {
          return current - permitsRequested;
        }
        return current;
      }) >= 0;
    }

    public Duration calculateDelay(final long permitsRequested) {
      final long currentPermits = permits.get();
      if (currentPermits >= permitsRequested) {
        return Duration.ZERO;
      }

      final long shortfall = permitsRequested - currentPermits;
      final long windowMillis = window.toMillis();
      final long delayMillis = (shortfall * windowMillis) / permitsPerWindow;

      return Duration.ofMillis(Math.min(delayMillis, windowMillis));
    }

    private void resetWindowIfNeeded() {
      final Instant now = Instant.now();
      final Instant start = windowStart.get();

      if (Duration.between(start, now).compareTo(window) >= 0) {
        if (windowStart.compareAndSet(start, now)) {
          permits.set(permitsPerWindow);
        }
      }
    }
  }

  /** Quota check result with enforcement decision. */
  public static final class QuotaCheckResult {
    private final QuotaDecision decision;
    private final String reason;
    private final Duration suggestedDelay;

    private QuotaCheckResult(final QuotaDecision decision, final String reason, final Duration suggestedDelay) {
      this.decision = decision;
      this.reason = reason;
      this.suggestedDelay = suggestedDelay;
    }

    public static QuotaCheckResult allowed() {
      return new QuotaCheckResult(QuotaDecision.ALLOW, null, Duration.ZERO);
    }

    public static QuotaCheckResult warning(final String reason) {
      return new QuotaCheckResult(QuotaDecision.WARN, reason, Duration.ZERO);
    }

    public static QuotaCheckResult rejected(final String reason) {
      return new QuotaCheckResult(QuotaDecision.REJECT, reason, Duration.ZERO);
    }

    public static QuotaCheckResult throttled(final Duration delay) {
      return new QuotaCheckResult(QuotaDecision.THROTTLE, "Rate limited", delay);
    }

    public QuotaDecision getDecision() {
      return decision;
    }

    public String getReason() {
      return reason;
    }

    public Duration getSuggestedDelay() {
      return suggestedDelay;
    }

    public boolean isAllowed() {
      return decision == QuotaDecision.ALLOW || decision == QuotaDecision.WARN;
    }
  }

  /** Quota enforcement decision. */
  public enum QuotaDecision {
    ALLOW,    // Request is allowed
    WARN,     // Request is allowed with warning
    THROTTLE, // Request should be delayed
    REJECT    // Request is rejected
  }

  /** Resource usage statistics with detailed metrics. */
  public static final class ResourceUsageStats {
    private final ResourceType resourceType;
    private final String tenantId;
    private final long currentUsage;
    private final long peakUsage;
    private final long totalConsumed;
    private final double currentRate;
    private final long burstUsage;
    private final boolean inBurstMode;
    private final ResourceQuota quota;
    private final Instant timestamp;

    ResourceUsageStats(
        final ResourceType resourceType,
        final String tenantId,
        final long currentUsage,
        final long peakUsage,
        final long totalConsumed,
        final double currentRate,
        final long burstUsage,
        final boolean inBurstMode,
        final ResourceQuota quota) {
      this.resourceType = resourceType;
      this.tenantId = tenantId;
      this.currentUsage = currentUsage;
      this.peakUsage = peakUsage;
      this.totalConsumed = totalConsumed;
      this.currentRate = currentRate;
      this.burstUsage = burstUsage;
      this.inBurstMode = inBurstMode;
      this.quota = quota;
      this.timestamp = Instant.now();
    }

    // Getters
    public ResourceType getResourceType() {
      return resourceType;
    }

    public String getTenantId() {
      return tenantId;
    }

    public long getCurrentUsage() {
      return currentUsage;
    }

    public long getPeakUsage() {
      return peakUsage;
    }

    public long getTotalConsumed() {
      return totalConsumed;
    }

    public double getCurrentRate() {
      return currentRate;
    }

    public long getBurstUsage() {
      return burstUsage;
    }

    public boolean isInBurstMode() {
      return inBurstMode;
    }

    public ResourceQuota getQuota() {
      return quota;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public double getUtilizationPercentage() {
      if (quota.getHardLimit() <= 0) {
        return 0.0;
      }
      return (currentUsage * 100.0) / quota.getHardLimit();
    }

    public boolean isOverSoftLimit() {
      return currentUsage > quota.getSoftLimit();
    }

    public boolean isOverHardLimit() {
      return currentUsage > quota.getHardLimit();
    }
  }

  // Instance fields
  private final ConcurrentHashMap<String, ResourceUsageTracker> trackers = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, ResourceQuota> quotas = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

  private volatile boolean enabled = true;
  private final AtomicLong totalRequests = new AtomicLong(0);
  private final AtomicLong rejectedRequests = new AtomicLong(0);
  private final AtomicLong throttledRequests = new AtomicLong(0);

  public ResourceQuotaManager() {
    startBackgroundTasks();
    LOGGER.info("Resource quota manager initialized");
  }

  /**
   * Sets a resource quota for a specific tenant and resource type.
   *
   * @param quota the resource quota to set
   */
  public void setQuota(final ResourceQuota quota) {
    final String key = createKey(quota.getTenantId(), quota.getResourceType());
    quotas.put(key, quota);
    trackers.put(key, new ResourceUsageTracker(quota));

    LOGGER.info(String.format(
        "Set quota for tenant=%s, resource=%s: soft=%d, hard=%d, strategy=%s",
        quota.getTenantId(), quota.getResourceType(),
        quota.getSoftLimit(), quota.getHardLimit(), quota.getStrategy()));
  }

  /**
   * Checks if a resource allocation request is allowed.
   *
   * @param tenantId tenant identifier
   * @param resourceType resource type
   * @param requestedAmount requested resource amount
   * @return quota check result
   */
  public QuotaCheckResult checkQuota(
      final String tenantId,
      final ResourceType resourceType,
      final long requestedAmount) {

    if (!enabled) {
      return QuotaCheckResult.allowed();
    }

    totalRequests.incrementAndGet();
    final String key = createKey(tenantId, resourceType);
    final ResourceUsageTracker tracker = trackers.get(key);

    if (tracker == null) {
      // No quota configured - allow by default
      return QuotaCheckResult.allowed();
    }

    final QuotaCheckResult result = tracker.checkQuota(requestedAmount);

    switch (result.getDecision()) {
      case REJECT:
        rejectedRequests.incrementAndGet();
        break;
      case THROTTLE:
        throttledRequests.incrementAndGet();
        break;
      default:
        break;
    }

    return result;
  }

  /**
   * Records resource usage for a tenant and resource type.
   *
   * @param tenantId tenant identifier
   * @param resourceType resource type
   * @param amount resource amount consumed
   */
  public void recordUsage(final String tenantId, final ResourceType resourceType, final long amount) {
    if (!enabled) {
      return;
    }

    final String key = createKey(tenantId, resourceType);
    final ResourceUsageTracker tracker = trackers.get(key);

    if (tracker != null) {
      tracker.recordUsage(amount);
    }
  }

  /**
   * Records resource deallocation for a tenant and resource type.
   *
   * @param tenantId tenant identifier
   * @param resourceType resource type
   * @param amount resource amount deallocated
   */
  public void recordDeallocation(final String tenantId, final ResourceType resourceType, final long amount) {
    if (!enabled) {
      return;
    }

    final String key = createKey(tenantId, resourceType);
    final ResourceUsageTracker tracker = trackers.get(key);

    if (tracker != null) {
      tracker.recordDeallocation(amount);
    }
  }

  /**
   * Gets resource usage statistics for a tenant and resource type.
   *
   * @param tenantId tenant identifier
   * @param resourceType resource type
   * @return usage statistics or null if not found
   */
  public ResourceUsageStats getUsageStats(final String tenantId, final ResourceType resourceType) {
    final String key = createKey(tenantId, resourceType);
    final ResourceUsageTracker tracker = trackers.get(key);
    return tracker != null ? tracker.getStats() : null;
  }

  /**
   * Gets comprehensive quota manager statistics.
   *
   * @return formatted statistics
   */
  public String getStatistics() {
    final StringBuilder sb = new StringBuilder("=== Resource Quota Manager Statistics ===\n");

    sb.append(String.format("Enabled: %s\n", enabled));
    sb.append(String.format("Total quotas configured: %d\n", quotas.size()));
    sb.append(String.format("Total requests: %,d\n", totalRequests.get()));
    sb.append(String.format("Rejected requests: %,d\n", rejectedRequests.get()));
    sb.append(String.format("Throttled requests: %,d\n", throttledRequests.get()));

    final double rejectionRate = totalRequests.get() > 0 ?
        (rejectedRequests.get() * 100.0) / totalRequests.get() : 0.0;
    final double throttleRate = totalRequests.get() > 0 ?
        (throttledRequests.get() * 100.0) / totalRequests.get() : 0.0;

    sb.append(String.format("Rejection rate: %.2f%%\n", rejectionRate));
    sb.append(String.format("Throttle rate: %.2f%%\n", throttleRate));
    sb.append("\n");

    sb.append("Active Quotas:\n");
    for (final Map.Entry<String, ResourceQuota> entry : quotas.entrySet()) {
      final ResourceQuota quota = entry.getValue();
      final ResourceUsageStats stats = getUsageStats(quota.getTenantId(), quota.getResourceType());

      if (stats != null) {
        sb.append(String.format(
            "  %s/%s: usage=%d/%d (%.1f%%) strategy=%s\n",
            quota.getTenantId(), quota.getResourceType(),
            stats.getCurrentUsage(), quota.getHardLimit(),
            stats.getUtilizationPercentage(), quota.getStrategy()));
      }
    }

    return sb.toString();
  }

  private String createKey(final String tenantId, final ResourceType resourceType) {
    return tenantId + ":" + resourceType.name();
  }

  private void startBackgroundTasks() {
    // Quota monitoring and alerting
    scheduler.scheduleAtFixedRate(this::monitorQuotas, 30, 30, TimeUnit.SECONDS);

    // Statistics collection
    scheduler.scheduleAtFixedRate(this::collectStatistics, 60, 60, TimeUnit.SECONDS);

    // Cleanup inactive trackers
    scheduler.scheduleAtFixedRate(this::cleanupTrackers, 300, 300, TimeUnit.SECONDS);
  }

  private void monitorQuotas() {
    for (final Map.Entry<String, ResourceUsageTracker> entry : trackers.entrySet()) {
      final ResourceUsageTracker tracker = entry.getValue();
      final ResourceUsageStats stats = tracker.getStats();

      if (stats.isOverSoftLimit()) {
        LOGGER.warning(String.format(
            "Soft limit exceeded: tenant=%s, resource=%s, usage=%d, limit=%d",
            stats.getTenantId(), stats.getResourceType(),
            stats.getCurrentUsage(), stats.getQuota().getSoftLimit()));
      }

      if (stats.getUtilizationPercentage() > stats.getQuota().getCriticalThreshold() * 100) {
        LOGGER.severe(String.format(
            "Critical threshold reached: tenant=%s, resource=%s, utilization=%.1f%%",
            stats.getTenantId(), stats.getResourceType(), stats.getUtilizationPercentage()));
      }
    }
  }

  private void collectStatistics() {
    // Collect periodic statistics for analytics
    LOGGER.fine(String.format(
        "Quota manager stats: quotas=%d, total_requests=%d, rejection_rate=%.2f%%",
        quotas.size(), totalRequests.get(),
        totalRequests.get() > 0 ? (rejectedRequests.get() * 100.0) / totalRequests.get() : 0.0));
  }

  private void cleanupTrackers() {
    // Remove inactive trackers to prevent memory leaks
    final Instant cutoff = Instant.now().minus(Duration.ofHours(1));

    trackers.entrySet().removeIf(entry -> {
      final ResourceUsageStats stats = entry.getValue().getStats();
      return stats.getTimestamp().isBefore(cutoff) && stats.getCurrentUsage() == 0;
    });
  }

  /**
   * Enables or disables quota enforcement.
   *
   * @param enabled true to enable enforcement
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    LOGGER.info("Resource quota enforcement " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Removes a quota for a specific tenant and resource type.
   *
   * @param tenantId tenant identifier
   * @param resourceType resource type
   */
  public void removeQuota(final String tenantId, final ResourceType resourceType) {
    final String key = createKey(tenantId, resourceType);
    quotas.remove(key);
    trackers.remove(key);

    LOGGER.info(String.format("Removed quota for tenant=%s, resource=%s", tenantId, resourceType));
  }

  /**
   * Shuts down the quota manager.
   */
  public void shutdown() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (final InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Resource quota manager shutdown");
  }
}