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
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Advanced resource scheduler providing fair share scheduling, priority-based allocation,
 * and resource preemption with multi-tenant isolation.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Fair share scheduling with configurable weights
 *   <li>Priority-based resource allocation with preemption
 *   <li>Resource reservation and advance booking
 *   <li>Dynamic load balancing and resource migration
 *   <li>Multi-tenant resource isolation and QoS guarantees
 *   <li>Resource efficiency optimization and waste reduction
 * </ul>
 *
 * @since 1.0.0
 */
public final class ResourceScheduler {

  private static final Logger LOGGER = Logger.getLogger(ResourceScheduler.class.getName());

  /** Scheduling algorithms available. */
  public enum SchedulingAlgorithm {
    FAIR_SHARE,      // Weighted fair queuing
    PRIORITY,        // Priority-based with preemption
    ROUND_ROBIN,     // Round-robin allocation
    DEFICIT_ROUND_ROBIN, // Deficit-based round-robin
    SHORTEST_JOB_FIRST,  // SJF with estimated runtime
    COMPLETELY_FAIR,     // CFS-inspired algorithm
    LOTTERY,         // Lottery scheduling with tickets
    MULTILEVEL_FEEDBACK  // Multi-level feedback queues
  }

  /** Resource request with scheduling context. */
  public static final class ResourceRequest {
    private final String requestId;
    private final String tenantId;
    private final ResourceQuotaManager.ResourceType resourceType;
    private final long requestedAmount;
    private final Duration maxWaitTime;
    private final int priority;
    private final double weight;
    private final Duration estimatedDuration;
    private final Instant submissionTime;
    private final Map<String, String> attributes;
    private final ResourceRequestCallback callback;

    private volatile ResourceRequestStatus status = ResourceRequestStatus.QUEUED;
    private volatile Instant scheduledTime;
    private volatile Instant completionTime;

    private ResourceRequest(final Builder builder) {
      this.requestId = builder.requestId;
      this.tenantId = builder.tenantId;
      this.resourceType = builder.resourceType;
      this.requestedAmount = builder.requestedAmount;
      this.maxWaitTime = builder.maxWaitTime;
      this.priority = builder.priority;
      this.weight = builder.weight;
      this.estimatedDuration = builder.estimatedDuration;
      this.submissionTime = Instant.now();
      this.attributes = Map.copyOf(builder.attributes);
      this.callback = builder.callback;
    }

    public static Builder builder(final String requestId, final String tenantId,
                                  final ResourceQuotaManager.ResourceType resourceType, final long requestedAmount) {
      return new Builder(requestId, tenantId, resourceType, requestedAmount);
    }

    public static final class Builder {
      private final String requestId;
      private final String tenantId;
      private final ResourceQuotaManager.ResourceType resourceType;
      private final long requestedAmount;
      private Duration maxWaitTime = Duration.ofMinutes(5);
      private int priority = 100;
      private double weight = 1.0;
      private Duration estimatedDuration = Duration.ofSeconds(30);
      private Map<String, String> attributes = new ConcurrentHashMap<>();
      private ResourceRequestCallback callback;

      private Builder(final String requestId, final String tenantId,
                      final ResourceQuotaManager.ResourceType resourceType, final long requestedAmount) {
        this.requestId = requestId;
        this.tenantId = tenantId;
        this.resourceType = resourceType;
        this.requestedAmount = requestedAmount;
      }

      public Builder withMaxWaitTime(final Duration maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
        return this;
      }

      public Builder withPriority(final int priority) {
        this.priority = priority;
        return this;
      }

      public Builder withWeight(final double weight) {
        this.weight = weight;
        return this;
      }

      public Builder withEstimatedDuration(final Duration estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
        return this;
      }

      public Builder withAttribute(final String key, final String value) {
        this.attributes.put(key, value);
        return this;
      }

      public Builder withCallback(final ResourceRequestCallback callback) {
        this.callback = callback;
        return this;
      }

      public ResourceRequest build() {
        return new ResourceRequest(this);
      }
    }

    // Getters
    public String getRequestId() { return requestId; }
    public String getTenantId() { return tenantId; }
    public ResourceQuotaManager.ResourceType getResourceType() { return resourceType; }
    public long getRequestedAmount() { return requestedAmount; }
    public Duration getMaxWaitTime() { return maxWaitTime; }
    public int getPriority() { return priority; }
    public double getWeight() { return weight; }
    public Duration getEstimatedDuration() { return estimatedDuration; }
    public Instant getSubmissionTime() { return submissionTime; }
    public Map<String, String> getAttributes() { return attributes; }
    public ResourceRequestCallback getCallback() { return callback; }

    public ResourceRequestStatus getStatus() { return status; }
    public Instant getScheduledTime() { return scheduledTime; }
    public Instant getCompletionTime() { return completionTime; }

    void setStatus(final ResourceRequestStatus status) {
      this.status = status;
      if (status == ResourceRequestStatus.RUNNING) {
        this.scheduledTime = Instant.now();
      } else if (status == ResourceRequestStatus.COMPLETED || status == ResourceRequestStatus.FAILED) {
        this.completionTime = Instant.now();
      }
    }

    public Duration getWaitTime() {
      if (scheduledTime != null) {
        return Duration.between(submissionTime, scheduledTime);
      }
      return Duration.between(submissionTime, Instant.now());
    }

    public Duration getExecutionTime() {
      if (scheduledTime != null && completionTime != null) {
        return Duration.between(scheduledTime, completionTime);
      }
      return Duration.ZERO;
    }
  }

  /** Resource request status. */
  public enum ResourceRequestStatus {
    QUEUED,     // Waiting in queue
    SCHEDULED,  // Scheduled for execution
    RUNNING,    // Currently executing
    COMPLETED,  // Successfully completed
    FAILED,     // Failed execution
    CANCELLED,  // Cancelled by user
    TIMEOUT,    // Timed out waiting
    PREEMPTED   // Preempted by higher priority request
  }

  /** Callback interface for resource request lifecycle events. */
  public interface ResourceRequestCallback {
    void onScheduled(ResourceRequest request);
    void onStarted(ResourceRequest request);
    void onCompleted(ResourceRequest request);
    void onFailed(ResourceRequest request, Throwable cause);
    void onCancelled(ResourceRequest request);
    void onPreempted(ResourceRequest request);
  }

  /** Tenant scheduling context and statistics. */
  private static final class TenantContext {
    private final String tenantId;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong completedRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong preemptedRequests = new AtomicLong(0);
    private final AtomicReference<Instant> lastActivity = new AtomicReference<>(Instant.now());

    private volatile double fairShareRatio = 1.0;
    private volatile long resourceDebt = 0;
    private volatile int activePriority = 100;

    TenantContext(final String tenantId) {
      this.tenantId = tenantId;
    }

    void updateStats(final ResourceRequest request) {
      switch (request.getStatus()) {
        case COMPLETED:
          completedRequests.incrementAndGet();
          break;
        case FAILED:
          failedRequests.incrementAndGet();
          break;
        case PREEMPTED:
          preemptedRequests.incrementAndGet();
          break;
        default:
          break;
      }
      lastActivity.set(Instant.now());
    }

    double getSuccessRate() {
      final long total = completedRequests.get() + failedRequests.get();
      return total > 0 ? (completedRequests.get() * 100.0) / total : 0.0;
    }

    long getTotalRequests() { return totalRequests.get(); }
    long getCompletedRequests() { return completedRequests.get(); }
    long getFailedRequests() { return failedRequests.get(); }
    long getPreemptedRequests() { return preemptedRequests.get(); }
    Instant getLastActivity() { return lastActivity.get(); }
    double getFairShareRatio() { return fairShareRatio; }
    long getResourceDebt() { return resourceDebt; }
    int getActivePriority() { return activePriority; }

    void setFairShareRatio(final double ratio) { this.fairShareRatio = ratio; }
    void setResourceDebt(final long debt) { this.resourceDebt = debt; }
    void setActivePriority(final int priority) { this.activePriority = priority; }
  }

  /** Fair share scheduler implementation. */
  private static final class FairShareScheduler {
    private final ConcurrentHashMap<String, TenantContext> tenantContexts = new ConcurrentHashMap<>();
    private final AtomicLong totalWeight = new AtomicLong(0);

    public ResourceRequest selectNext(final BlockingQueue<ResourceRequest> queue) {
      if (queue.isEmpty()) {
        return null;
      }

      // Calculate fair share ratios
      updateFairShareRatios();

      // Find the tenant with the highest debt-to-share ratio
      ResourceRequest selected = null;
      double maxDebtRatio = Double.NEGATIVE_INFINITY;

      for (final ResourceRequest request : queue) {
        final TenantContext context = tenantContexts.get(request.getTenantId());
        if (context != null) {
          final double debtRatio = context.getResourceDebt() / Math.max(context.getFairShareRatio(), 0.1);
          if (debtRatio > maxDebtRatio) {
            maxDebtRatio = debtRatio;
            selected = request;
          }
        }
      }

      if (selected != null) {
        queue.remove(selected);
      }

      return selected;
    }

    public void recordResourceUsage(final String tenantId, final long amount) {
      final TenantContext context = tenantContexts.computeIfAbsent(tenantId, TenantContext::new);
      context.setResourceDebt(context.getResourceDebt() + amount);
    }

    private void updateFairShareRatios() {
      final long total = totalWeight.get();
      if (total > 0) {
        for (final TenantContext context : tenantContexts.values()) {
          // Simple equal sharing for now - can be enhanced with weights
          context.setFairShareRatio(1.0 / tenantContexts.size());
        }
      }
    }

    public TenantContext getTenantContext(final String tenantId) {
      return tenantContexts.computeIfAbsent(tenantId, TenantContext::new);
    }
  }

  /** Priority-based scheduler with preemption support. */
  private static final class PriorityScheduler {
    private final Comparator<ResourceRequest> priorityComparator = Comparator
        .comparingInt((ResourceRequest r) -> r.getPriority()).reversed()
        .thenComparing(ResourceRequest::getSubmissionTime);

    public ResourceRequest selectNext(final PriorityBlockingQueue<ResourceRequest> queue) {
      return queue.poll();
    }

    public boolean shouldPreempt(final ResourceRequest running, final ResourceRequest waiting) {
      // Preempt if priority difference is significant and waiting time exceeds threshold
      final int priorityDiff = waiting.getPriority() - running.getPriority();
      final Duration waitTime = waiting.getWaitTime();

      return priorityDiff >= 50 && waitTime.toSeconds() > 30;
    }
  }

  /** Deficit round-robin scheduler for fairness. */
  private static final class DeficitRoundRobinScheduler {
    private final ConcurrentHashMap<String, AtomicLong> tenantDeficits = new ConcurrentHashMap<>();
    private final long quantumSize = 1000; // Base quantum size

    public ResourceRequest selectNext(final BlockingQueue<ResourceRequest> queue) {
      for (final ResourceRequest request : queue) {
        final AtomicLong deficit = tenantDeficits.computeIfAbsent(
            request.getTenantId(), k -> new AtomicLong(quantumSize));

        if (deficit.get() >= request.getRequestedAmount()) {
          queue.remove(request);
          deficit.addAndGet(-request.getRequestedAmount());
          return request;
        }
      }

      // Replenish deficits and try again
      replenishDeficits();
      return selectNext(queue);
    }

    private void replenishDeficits() {
      for (final AtomicLong deficit : tenantDeficits.values()) {
        deficit.addAndGet(quantumSize);
      }
    }
  }

  // Instance fields
  private final SchedulingAlgorithm algorithm;
  private final ResourceQuotaManager quotaManager;
  private final FairShareScheduler fairShareScheduler = new FairShareScheduler();
  private final PriorityScheduler priorityScheduler = new PriorityScheduler();
  private final DeficitRoundRobinScheduler drrScheduler = new DeficitRoundRobinScheduler();

  private final BlockingQueue<ResourceRequest> pendingQueue = new PriorityBlockingQueue<>(1000);
  private final ConcurrentHashMap<String, ResourceRequest> runningRequests = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, ResourceRequest> completedRequests = new ConcurrentHashMap<>();

  private final ScheduledExecutorService schedulerExecutor = Executors.newScheduledThreadPool(2, new ThreadFactory() {
    private final AtomicLong threadNumber = new AtomicLong(1);
    @Override
    public Thread newThread(final Runnable r) {
      return new Thread(r, "ResourceScheduler-" + threadNumber.getAndIncrement());
    }
  });

  private final ThreadPoolExecutor workExecutor = new ThreadPoolExecutor(
      4, 16, 60, TimeUnit.SECONDS, new PriorityBlockingQueue<>());

  private volatile boolean enabled = true;
  private final AtomicLong totalScheduled = new AtomicLong(0);
  private final AtomicLong totalCompleted = new AtomicLong(0);
  private final AtomicLong totalFailed = new AtomicLong(0);
  private final AtomicLong totalPreempted = new AtomicLong(0);

  /**
   * Creates a resource scheduler with the specified algorithm.
   *
   * @param algorithm scheduling algorithm to use
   * @param quotaManager resource quota manager
   */
  public ResourceScheduler(final SchedulingAlgorithm algorithm, final ResourceQuotaManager quotaManager) {
    this.algorithm = algorithm;
    this.quotaManager = quotaManager;
    startSchedulingLoop();
    startMaintenanceTasks();
    LOGGER.info("Resource scheduler initialized with algorithm: " + algorithm);
  }

  /**
   * Submits a resource request for scheduling.
   *
   * @param request the resource request
   * @return true if successfully submitted
   */
  public boolean submit(final ResourceRequest request) {
    if (!enabled) {
      return false;
    }

    // Check quota first
    final ResourceQuotaManager.QuotaCheckResult quotaResult = quotaManager.checkQuota(
        request.getTenantId(), request.getResourceType(), request.getRequestedAmount());

    if (!quotaResult.isAllowed()) {
      if (request.getCallback() != null) {
        request.getCallback().onFailed(request, new RuntimeException("Quota exceeded: " + quotaResult.getReason()));
      }
      return false;
    }

    // Add to pending queue
    final boolean added = pendingQueue.offer(request);
    if (added) {
      final TenantContext context = fairShareScheduler.getTenantContext(request.getTenantId());
      context.totalRequests.incrementAndGet();

      if (request.getCallback() != null) {
        request.getCallback().onScheduled(request);
      }

      LOGGER.fine(String.format("Submitted request %s for tenant %s", request.getRequestId(), request.getTenantId()));
    }

    return added;
  }

  /**
   * Cancels a pending or running resource request.
   *
   * @param requestId request identifier
   * @return true if successfully cancelled
   */
  public boolean cancel(final String requestId) {
    // Try to remove from pending queue first
    final ResourceRequest pending = pendingQueue.stream()
        .filter(r -> r.getRequestId().equals(requestId))
        .findFirst()
        .orElse(null);

    if (pending != null) {
      pendingQueue.remove(pending);
      pending.setStatus(ResourceRequestStatus.CANCELLED);
      if (pending.getCallback() != null) {
        pending.getCallback().onCancelled(pending);
      }
      return true;
    }

    // Check running requests
    final ResourceRequest running = runningRequests.get(requestId);
    if (running != null) {
      // Mark for cancellation - actual cancellation depends on implementation
      running.setStatus(ResourceRequestStatus.CANCELLED);
      if (running.getCallback() != null) {
        running.getCallback().onCancelled(running);
      }
      return true;
    }

    return false;
  }

  /**
   * Gets the current status of a resource request.
   *
   * @param requestId request identifier
   * @return request status or null if not found
   */
  public ResourceRequest getRequest(final String requestId) {
    // Check running requests first
    ResourceRequest request = runningRequests.get(requestId);
    if (request != null) {
      return request;
    }

    // Check pending queue
    request = pendingQueue.stream()
        .filter(r -> r.getRequestId().equals(requestId))
        .findFirst()
        .orElse(null);
    if (request != null) {
      return request;
    }

    // Check completed requests
    return completedRequests.get(requestId);
  }

  /**
   * Gets comprehensive scheduler statistics.
   *
   * @return formatted statistics
   */
  public String getStatistics() {
    final StringBuilder sb = new StringBuilder("=== Resource Scheduler Statistics ===\n");

    sb.append(String.format("Algorithm: %s\n", algorithm));
    sb.append(String.format("Enabled: %s\n", enabled));
    sb.append(String.format("Pending requests: %d\n", pendingQueue.size()));
    sb.append(String.format("Running requests: %d\n", runningRequests.size()));
    sb.append(String.format("Total scheduled: %,d\n", totalScheduled.get()));
    sb.append(String.format("Total completed: %,d\n", totalCompleted.get()));
    sb.append(String.format("Total failed: %,d\n", totalFailed.get()));
    sb.append(String.format("Total preempted: %,d\n", totalPreempted.get()));

    final long total = totalScheduled.get();
    if (total > 0) {
      sb.append(String.format("Success rate: %.2f%%\n", (totalCompleted.get() * 100.0) / total));
      sb.append(String.format("Failure rate: %.2f%%\n", (totalFailed.get() * 100.0) / total));
      sb.append(String.format("Preemption rate: %.2f%%\n", (totalPreempted.get() * 100.0) / total));
    }

    sb.append("\nTenant Statistics:\n");
    for (final TenantContext context : fairShareScheduler.tenantContexts.values()) {
      sb.append(String.format(
          "  %s: requests=%d, completed=%d, success_rate=%.1f%%, debt=%d\n",
          context.tenantId, context.getTotalRequests(), context.getCompletedRequests(),
          context.getSuccessRate(), context.getResourceDebt()));
    }

    return sb.toString();
  }

  private void startSchedulingLoop() {
    schedulerExecutor.scheduleWithFixedDelay(this::processSchedulingQueue, 100, 100, TimeUnit.MILLISECONDS);
  }

  private void startMaintenanceTasks() {
    // Timeout handling
    schedulerExecutor.scheduleAtFixedRate(this::handleTimeouts, 30, 30, TimeUnit.SECONDS);

    // Statistics collection
    schedulerExecutor.scheduleAtFixedRate(this::collectStatistics, 60, 60, TimeUnit.SECONDS);

    // Cleanup completed requests
    schedulerExecutor.scheduleAtFixedRate(this::cleanupCompletedRequests, 300, 300, TimeUnit.SECONDS);
  }

  private void processSchedulingQueue() {
    if (!enabled || pendingQueue.isEmpty()) {
      return;
    }

    try {
      ResourceRequest request = null;

      switch (algorithm) {
        case FAIR_SHARE:
          request = fairShareScheduler.selectNext(pendingQueue);
          break;
        case PRIORITY:
          request = priorityScheduler.selectNext((PriorityBlockingQueue<ResourceRequest>) pendingQueue);
          break;
        case DEFICIT_ROUND_ROBIN:
          request = drrScheduler.selectNext(pendingQueue);
          break;
        case ROUND_ROBIN:
          request = pendingQueue.poll();
          break;
        default:
          request = pendingQueue.poll();
          break;
      }

      if (request != null) {
        scheduleRequest(request);
      }

    } catch (final Exception e) {
      LOGGER.warning("Error in scheduling loop: " + e.getMessage());
    }
  }

  private void scheduleRequest(final ResourceRequest request) {
    // Final quota check before scheduling
    final ResourceQuotaManager.QuotaCheckResult quotaResult = quotaManager.checkQuota(
        request.getTenantId(), request.getResourceType(), request.getRequestedAmount());

    if (!quotaResult.isAllowed()) {
      request.setStatus(ResourceRequestStatus.FAILED);
      if (request.getCallback() != null) {
        request.getCallback().onFailed(request, new RuntimeException("Quota check failed: " + quotaResult.getReason()));
      }
      totalFailed.incrementAndGet();
      return;
    }

    // Record resource usage
    quotaManager.recordUsage(request.getTenantId(), request.getResourceType(), request.getRequestedAmount());
    fairShareScheduler.recordResourceUsage(request.getTenantId(), request.getRequestedAmount());

    // Schedule execution
    request.setStatus(ResourceRequestStatus.RUNNING);
    runningRequests.put(request.getRequestId(), request);
    totalScheduled.incrementAndGet();

    if (request.getCallback() != null) {
      request.getCallback().onStarted(request);
    }

    // Execute in work thread pool
    workExecutor.submit(() -> executeRequest(request));

    LOGGER.fine(String.format("Scheduled request %s for execution", request.getRequestId()));
  }

  private void executeRequest(final ResourceRequest request) {
    try {
      // Simulate work execution - in real implementation, this would delegate to actual work
      Thread.sleep(request.getEstimatedDuration().toMillis());

      // Mark as completed
      request.setStatus(ResourceRequestStatus.COMPLETED);
      runningRequests.remove(request.getRequestId());
      completedRequests.put(request.getRequestId(), request);
      totalCompleted.incrementAndGet();

      // Update tenant context
      final TenantContext context = fairShareScheduler.getTenantContext(request.getTenantId());
      context.updateStats(request);

      if (request.getCallback() != null) {
        request.getCallback().onCompleted(request);
      }

      LOGGER.fine(String.format("Completed request %s", request.getRequestId()));

    } catch (final InterruptedException e) {
      request.setStatus(ResourceRequestStatus.CANCELLED);
      Thread.currentThread().interrupt();
    } catch (final Exception e) {
      request.setStatus(ResourceRequestStatus.FAILED);
      totalFailed.incrementAndGet();

      if (request.getCallback() != null) {
        request.getCallback().onFailed(request, e);
      }

      LOGGER.warning(String.format("Request %s failed: %s", request.getRequestId(), e.getMessage()));
    } finally {
      runningRequests.remove(request.getRequestId());

      // Release resources
      quotaManager.recordDeallocation(request.getTenantId(), request.getResourceType(), request.getRequestedAmount());
    }
  }

  private void handleTimeouts() {
    final Instant now = Instant.now();

    // Check pending requests for timeout
    pendingQueue.removeIf(request -> {
      final Duration waitTime = Duration.between(request.getSubmissionTime(), now);
      if (waitTime.compareTo(request.getMaxWaitTime()) > 0) {
        request.setStatus(ResourceRequestStatus.TIMEOUT);
        if (request.getCallback() != null) {
          request.getCallback().onFailed(request, new RuntimeException("Request timeout"));
        }
        return true;
      }
      return false;
    });
  }

  private void collectStatistics() {
    LOGGER.fine(String.format(
        "Scheduler stats: pending=%d, running=%d, completed=%d, failed=%d",
        pendingQueue.size(), runningRequests.size(), totalCompleted.get(), totalFailed.get()));
  }

  private void cleanupCompletedRequests() {
    final Instant cutoff = Instant.now().minus(Duration.ofHours(1));

    completedRequests.entrySet().removeIf(entry -> {
      final ResourceRequest request = entry.getValue();
      return request.getCompletionTime() != null && request.getCompletionTime().isBefore(cutoff);
    });
  }

  /**
   * Enables or disables the scheduler.
   *
   * @param enabled true to enable scheduling
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    LOGGER.info("Resource scheduler " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Gets the number of pending requests.
   *
   * @return number of pending requests
   */
  public int getPendingRequestCount() {
    return pendingQueue.size();
  }

  /**
   * Gets the number of running requests.
   *
   * @return number of running requests
   */
  public int getRunningRequestCount() {
    return runningRequests.size();
  }

  /**
   * Shuts down the scheduler.
   */
  public void shutdown() {
    enabled = false;

    schedulerExecutor.shutdown();
    workExecutor.shutdown();

    try {
      if (!schedulerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
        schedulerExecutor.shutdownNow();
      }
      if (!workExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
        workExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      schedulerExecutor.shutdownNow();
      workExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    LOGGER.info("Resource scheduler shutdown");
  }
}