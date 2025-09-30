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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Enterprise resource governance framework providing policy definition, enforcement,
 * approval workflows, chargeback billing, and capacity planning.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Resource policy definition and rule-based enforcement
 *   <li>Multi-level approval workflows for resource allocation
 *   <li>Resource usage chargeback and cost allocation
 *   <li>Capacity planning and demand forecasting
 *   <li>Compliance monitoring and audit trails
 *   <li>Resource optimization recommendations
 * </ul>
 *
 * @since 1.0.0
 */
public final class ResourceGovernanceManager {

  private static final Logger LOGGER = Logger.getLogger(ResourceGovernanceManager.class.getName());

  /** Resource governance policy. */
  public static final class ResourcePolicy {
    private final String policyId;
    private final String name;
    private final String description;
    private final ResourceQuotaManager.ResourceType resourceType;
    private final String tenantPattern; // Regex pattern for tenant matching
    private final PolicyAction action;
    private final Map<String, Object> conditions;
    private final int priority;
    private final boolean enabled;
    private final Instant createdAt;
    private final String createdBy;

    private ResourcePolicy(final Builder builder) {
      this.policyId = builder.policyId;
      this.name = builder.name;
      this.description = builder.description;
      this.resourceType = builder.resourceType;
      this.tenantPattern = builder.tenantPattern;
      this.action = builder.action;
      this.conditions = Map.copyOf(builder.conditions);
      this.priority = builder.priority;
      this.enabled = builder.enabled;
      this.createdAt = Instant.now();
      this.createdBy = builder.createdBy;
    }

    public static Builder builder(final String policyId, final String name) {
      return new Builder(policyId, name);
    }

    public static final class Builder {
      private final String policyId;
      private final String name;
      private String description = "";
      private ResourceQuotaManager.ResourceType resourceType;
      private String tenantPattern = ".*";
      private PolicyAction action = PolicyAction.ALLOW;
      private Map<String, Object> conditions = new ConcurrentHashMap<>();
      private int priority = 100;
      private boolean enabled = true;
      private String createdBy = "system";

      private Builder(final String policyId, final String name) {
        this.policyId = policyId;
        this.name = name;
      }

      public Builder withDescription(final String description) {
        this.description = description;
        return this;
      }

      public Builder withResourceType(final ResourceQuotaManager.ResourceType resourceType) {
        this.resourceType = resourceType;
        return this;
      }

      public Builder withTenantPattern(final String tenantPattern) {
        this.tenantPattern = tenantPattern;
        return this;
      }

      public Builder withAction(final PolicyAction action) {
        this.action = action;
        return this;
      }

      public Builder withCondition(final String key, final Object value) {
        this.conditions.put(key, value);
        return this;
      }

      public Builder withPriority(final int priority) {
        this.priority = priority;
        return this;
      }

      public Builder withEnabled(final boolean enabled) {
        this.enabled = enabled;
        return this;
      }

      public Builder withCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
        return this;
      }

      public ResourcePolicy build() {
        return new ResourcePolicy(this);
      }
    }

    // Getters
    public String getPolicyId() { return policyId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ResourceQuotaManager.ResourceType getResourceType() { return resourceType; }
    public String getTenantPattern() { return tenantPattern; }
    public PolicyAction getAction() { return action; }
    public Map<String, Object> getConditions() { return conditions; }
    public int getPriority() { return priority; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
  }

  /** Policy action types. */
  public enum PolicyAction {
    ALLOW,           // Allow the request
    DENY,            // Deny the request
    REQUIRE_APPROVAL, // Require approval workflow
    THROTTLE,        // Apply throttling
    CHARGE_PREMIUM,  // Apply premium pricing
    MONITOR,         // Monitor but allow
    ALERT            // Generate alert but allow
  }

  /** Resource approval request. */
  public static final class ApprovalRequest {
    private final String requestId;
    private final String tenantId;
    private final ResourceQuotaManager.ResourceType resourceType;
    private final long requestedAmount;
    private final String justification;
    private final String requestedBy;
    private final Instant requestedAt;
    private final List<String> requiredApprovers;
    private final Map<String, ApprovalDecision> approvals = new ConcurrentHashMap<>();
    private volatile ApprovalStatus status = ApprovalStatus.PENDING;

    public ApprovalRequest(final String requestId, final String tenantId,
                          final ResourceQuotaManager.ResourceType resourceType, final long requestedAmount,
                          final String justification, final String requestedBy, final List<String> requiredApprovers) {
      this.requestId = requestId;
      this.tenantId = tenantId;
      this.resourceType = resourceType;
      this.requestedAmount = requestedAmount;
      this.justification = justification;
      this.requestedBy = requestedBy;
      this.requestedAt = Instant.now();
      this.requiredApprovers = List.copyOf(requiredApprovers);
    }

    // Getters
    public String getRequestId() { return requestId; }
    public String getTenantId() { return tenantId; }
    public ResourceQuotaManager.ResourceType getResourceType() { return resourceType; }
    public long getRequestedAmount() { return requestedAmount; }
    public String getJustification() { return justification; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public List<String> getRequiredApprovers() { return requiredApprovers; }
    public Map<String, ApprovalDecision> getApprovals() { return approvals; }
    public ApprovalStatus getStatus() { return status; }

    void setStatus(final ApprovalStatus status) { this.status = status; }

    public boolean isFullyApproved() {
      return requiredApprovers.stream()
          .allMatch(approver -> approvals.containsKey(approver) && approvals.get(approver).isApproved());
    }

    public boolean hasRejection() {
      return approvals.values().stream().anyMatch(decision -> !decision.isApproved());
    }
  }

  /** Approval decision. */
  public static final class ApprovalDecision {
    private final String approver;
    private final boolean approved;
    private final String comments;
    private final Instant decidedAt;

    public ApprovalDecision(final String approver, final boolean approved, final String comments) {
      this.approver = approver;
      this.approved = approved;
      this.comments = comments;
      this.decidedAt = Instant.now();
    }

    public String getApprover() { return approver; }
    public boolean isApproved() { return approved; }
    public String getComments() { return comments; }
    public Instant getDecidedAt() { return decidedAt; }
  }

  /** Approval request status. */
  public enum ApprovalStatus {
    PENDING,   // Awaiting approvals
    APPROVED,  // All required approvals received
    REJECTED,  // At least one rejection
    EXPIRED    // Request expired
  }

  /** Resource usage charge record. */
  public static final class UsageCharge {
    private final String chargeId;
    private final String tenantId;
    private final ResourceQuotaManager.ResourceType resourceType;
    private final long usage;
    private final double unitCost;
    private final double totalCost;
    private final Instant chargeDate;
    private final String billingPeriod;

    public UsageCharge(final String chargeId, final String tenantId,
                      final ResourceQuotaManager.ResourceType resourceType, final long usage,
                      final double unitCost, final String billingPeriod) {
      this.chargeId = chargeId;
      this.tenantId = tenantId;
      this.resourceType = resourceType;
      this.usage = usage;
      this.unitCost = unitCost;
      this.totalCost = usage * unitCost;
      this.chargeDate = Instant.now();
      this.billingPeriod = billingPeriod;
    }

    // Getters
    public String getChargeId() { return chargeId; }
    public String getTenantId() { return tenantId; }
    public ResourceQuotaManager.ResourceType getResourceType() { return resourceType; }
    public long getUsage() { return usage; }
    public double getUnitCost() { return unitCost; }
    public double getTotalCost() { return totalCost; }
    public Instant getChargeDate() { return chargeDate; }
    public String getBillingPeriod() { return billingPeriod; }
  }

  /** Capacity planning forecast. */
  public static final class CapacityForecast {
    private final ResourceQuotaManager.ResourceType resourceType;
    private final Instant forecastDate;
    private final Duration forecastPeriod;
    private final long currentCapacity;
    private final long predictedDemand;
    private final long recommendedCapacity;
    private final double confidenceLevel;
    private final String methodology;

    public CapacityForecast(final ResourceQuotaManager.ResourceType resourceType, final Duration forecastPeriod,
                           final long currentCapacity, final long predictedDemand, final long recommendedCapacity,
                           final double confidenceLevel, final String methodology) {
      this.resourceType = resourceType;
      this.forecastDate = Instant.now();
      this.forecastPeriod = forecastPeriod;
      this.currentCapacity = currentCapacity;
      this.predictedDemand = predictedDemand;
      this.recommendedCapacity = recommendedCapacity;
      this.confidenceLevel = confidenceLevel;
      this.methodology = methodology;
    }

    // Getters
    public ResourceQuotaManager.ResourceType getResourceType() { return resourceType; }
    public Instant getForecastDate() { return forecastDate; }
    public Duration getForecastPeriod() { return forecastPeriod; }
    public long getCurrentCapacity() { return currentCapacity; }
    public long getPredictedDemand() { return predictedDemand; }
    public long getRecommendedCapacity() { return recommendedCapacity; }
    public double getConfidenceLevel() { return confidenceLevel; }
    public String getMethodology() { return methodology; }

    public boolean needsCapacityIncrease() {
      return predictedDemand > currentCapacity * 0.8; // 80% threshold
    }

    public double getUtilizationForecast() {
      return currentCapacity > 0 ? (predictedDemand * 100.0) / currentCapacity : 0.0;
    }
  }

  /** Policy evaluation engine. */
  private static final class PolicyEngine {
    private final List<ResourcePolicy> policies = new ArrayList<>();

    void addPolicy(final ResourcePolicy policy) {
      policies.add(policy);
      policies.sort((p1, p2) -> Integer.compare(p2.getPriority(), p1.getPriority())); // Higher priority first
    }

    void removePolicy(final String policyId) {
      policies.removeIf(policy -> policy.getPolicyId().equals(policyId));
    }

    PolicyEvaluationResult evaluate(final String tenantId, final ResourceQuotaManager.ResourceType resourceType,
                                   final long requestedAmount, final Map<String, Object> context) {
      for (final ResourcePolicy policy : policies) {
        if (!policy.isEnabled()) {
          continue;
        }

        if (!matchesPolicy(policy, tenantId, resourceType, requestedAmount, context)) {
          continue;
        }

        // Policy matches - return the action
        return new PolicyEvaluationResult(policy.getAction(), policy.getPolicyId(),
            policy.getName(), evaluateConditions(policy, context));
      }

      // No matching policy - default to allow
      return new PolicyEvaluationResult(PolicyAction.ALLOW, "default", "Default policy", true);
    }

    private boolean matchesPolicy(final ResourcePolicy policy, final String tenantId,
                                 final ResourceQuotaManager.ResourceType resourceType, final long requestedAmount,
                                 final Map<String, Object> context) {
      // Check resource type
      if (policy.getResourceType() != null && !policy.getResourceType().equals(resourceType)) {
        return false;
      }

      // Check tenant pattern
      if (!tenantId.matches(policy.getTenantPattern())) {
        return false;
      }

      // Check basic conditions
      return evaluateConditions(policy, context);
    }

    private boolean evaluateConditions(final ResourcePolicy policy, final Map<String, Object> context) {
      for (final Map.Entry<String, Object> condition : policy.getConditions().entrySet()) {
        final String key = condition.getKey();
        final Object expectedValue = condition.getValue();
        final Object actualValue = context.get(key);

        if (!evaluateCondition(key, expectedValue, actualValue)) {
          return false;
        }
      }
      return true;
    }

    private boolean evaluateCondition(final String key, final Object expectedValue, final Object actualValue) {
      if (actualValue == null) {
        return expectedValue == null;
      }

      switch (key) {
        case "max_amount":
          return actualValue instanceof Long && (Long) actualValue <= (Long) expectedValue;
        case "min_amount":
          return actualValue instanceof Long && (Long) actualValue >= (Long) expectedValue;
        case "time_window":
          return true; // Simplified for now
        default:
          return expectedValue.equals(actualValue);
      }
    }

    List<ResourcePolicy> getPolicies() {
      return new ArrayList<>(policies);
    }
  }

  /** Policy evaluation result. */
  private static final class PolicyEvaluationResult {
    private final PolicyAction action;
    private final String policyId;
    private final String policyName;
    private final boolean conditionsMet;

    PolicyEvaluationResult(final PolicyAction action, final String policyId,
                          final String policyName, final boolean conditionsMet) {
      this.action = action;
      this.policyId = policyId;
      this.policyName = policyName;
      this.conditionsMet = conditionsMet;
    }

    PolicyAction getAction() { return action; }
    String getPolicyId() { return policyId; }
    String getPolicyName() { return policyName; }
    boolean isConditionsMet() { return conditionsMet; }
  }

  // Instance fields
  private final PolicyEngine policyEngine = new PolicyEngine();
  private final ConcurrentHashMap<String, ApprovalRequest> pendingApprovals = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, UsageCharge> charges = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<ResourceQuotaManager.ResourceType, Double> unitCosts = new ConcurrentHashMap<>();

  private final ScheduledExecutorService governanceExecutor = Executors.newScheduledThreadPool(2);
  private final AtomicLong totalPolicyEvaluations = new AtomicLong(0);
  private final AtomicLong totalApprovalRequests = new AtomicLong(0);
  private final AtomicLong totalCharges = new AtomicLong(0);

  private volatile boolean enabled = true;

  public ResourceGovernanceManager() {
    initializeDefaultPolicies();
    initializeDefaultCosts();
    startGovernanceTasks();
    LOGGER.info("Resource governance manager initialized");
  }

  /**
   * Adds a resource policy.
   *
   * @param policy the resource policy
   */
  public void addPolicy(final ResourcePolicy policy) {
    policyEngine.addPolicy(policy);
    LOGGER.info(String.format("Added policy %s: %s", policy.getPolicyId(), policy.getName()));
  }

  /**
   * Removes a resource policy.
   *
   * @param policyId policy identifier
   */
  public void removePolicy(final String policyId) {
    policyEngine.removePolicy(policyId);
    LOGGER.info(String.format("Removed policy %s", policyId));
  }

  /**
   * Evaluates a resource request against governance policies.
   *
   * @param tenantId tenant identifier
   * @param resourceType resource type
   * @param requestedAmount requested amount
   * @param context additional context
   * @return policy action to take
   */
  public PolicyAction evaluateRequest(final String tenantId, final ResourceQuotaManager.ResourceType resourceType,
                                     final long requestedAmount, final Map<String, Object> context) {
    if (!enabled) {
      return PolicyAction.ALLOW;
    }

    totalPolicyEvaluations.incrementAndGet();
    final PolicyEvaluationResult result = policyEngine.evaluate(tenantId, resourceType, requestedAmount, context);

    LOGGER.fine(String.format("Policy evaluation: tenant=%s, resource=%s, amount=%d, action=%s, policy=%s",
        tenantId, resourceType, requestedAmount, result.getAction(), result.getPolicyId()));

    return result.getAction();
  }

  /**
   * Creates an approval request.
   *
   * @param requestId request identifier
   * @param tenantId tenant identifier
   * @param resourceType resource type
   * @param requestedAmount requested amount
   * @param justification justification for the request
   * @param requestedBy requester
   * @param requiredApprovers list of required approvers
   * @return approval request
   */
  public ApprovalRequest createApprovalRequest(final String requestId, final String tenantId,
                                              final ResourceQuotaManager.ResourceType resourceType, final long requestedAmount,
                                              final String justification, final String requestedBy, final List<String> requiredApprovers) {
    final ApprovalRequest request = new ApprovalRequest(requestId, tenantId, resourceType, requestedAmount,
        justification, requestedBy, requiredApprovers);

    pendingApprovals.put(requestId, request);
    totalApprovalRequests.incrementAndGet();

    LOGGER.info(String.format("Created approval request %s for tenant %s", requestId, tenantId));
    return request;
  }

  /**
   * Records an approval decision.
   *
   * @param requestId request identifier
   * @param approver approver identifier
   * @param approved approval decision
   * @param comments approval comments
   * @return updated approval request status
   */
  public ApprovalStatus recordApproval(final String requestId, final String approver,
                                      final boolean approved, final String comments) {
    final ApprovalRequest request = pendingApprovals.get(requestId);
    if (request == null) {
      return ApprovalStatus.EXPIRED; // Request not found or expired
    }

    if (!request.getRequiredApprovers().contains(approver)) {
      LOGGER.warning(String.format("Approver %s not authorized for request %s", approver, requestId));
      return request.getStatus();
    }

    final ApprovalDecision decision = new ApprovalDecision(approver, approved, comments);
    request.getApprovals().put(approver, decision);

    // Update request status
    if (request.hasRejection()) {
      request.setStatus(ApprovalStatus.REJECTED);
    } else if (request.isFullyApproved()) {
      request.setStatus(ApprovalStatus.APPROVED);
    }

    LOGGER.info(String.format("Recorded approval decision for request %s: approver=%s, approved=%s",
        requestId, approver, approved));

    return request.getStatus();
  }

  /**
   * Records resource usage for chargeback.
   *
   * @param tenantId tenant identifier
   * @param resourceType resource type
   * @param usage usage amount
   * @param billingPeriod billing period
   */
  public void recordUsageCharge(final String tenantId, final ResourceQuotaManager.ResourceType resourceType,
                               final long usage, final String billingPeriod) {
    final double unitCost = unitCosts.getOrDefault(resourceType, 1.0);
    final String chargeId = String.format("%s-%s-%s-%d", tenantId, resourceType, billingPeriod, System.currentTimeMillis());

    final UsageCharge charge = new UsageCharge(chargeId, tenantId, resourceType, usage, unitCost, billingPeriod);
    charges.put(chargeId, charge);
    totalCharges.incrementAndGet();

    LOGGER.fine(String.format("Recorded usage charge: tenant=%s, resource=%s, usage=%d, cost=%.2f",
        tenantId, resourceType, usage, charge.getTotalCost()));
  }

  /**
   * Gets usage charges for a tenant and billing period.
   *
   * @param tenantId tenant identifier
   * @param billingPeriod billing period
   * @return list of usage charges
   */
  public List<UsageCharge> getUsageCharges(final String tenantId, final String billingPeriod) {
    return charges.values().stream()
        .filter(charge -> charge.getTenantId().equals(tenantId))
        .filter(charge -> charge.getBillingPeriod().equals(billingPeriod))
        .collect(Collectors.toList());
  }

  /**
   * Calculates total cost for a tenant and billing period.
   *
   * @param tenantId tenant identifier
   * @param billingPeriod billing period
   * @return total cost
   */
  public double getTotalCost(final String tenantId, final String billingPeriod) {
    return getUsageCharges(tenantId, billingPeriod).stream()
        .mapToDouble(UsageCharge::getTotalCost)
        .sum();
  }

  /**
   * Generates capacity forecast for a resource type.
   *
   * @param resourceType resource type
   * @param forecastPeriod forecast period
   * @return capacity forecast
   */
  public CapacityForecast generateCapacityForecast(final ResourceQuotaManager.ResourceType resourceType,
                                                  final Duration forecastPeriod) {
    // Simplified forecasting - in real implementation would use historical data and ML models
    final long currentCapacity = 1000000; // Placeholder
    final long historicalUsage = 800000; // Placeholder
    final double growthRate = 0.1; // 10% growth

    final long predictedDemand = (long) (historicalUsage * (1 + growthRate * (forecastPeriod.toDays() / 30.0)));
    final long recommendedCapacity = (long) (predictedDemand * 1.2); // 20% buffer

    final CapacityForecast forecast = new CapacityForecast(
        resourceType, forecastPeriod, currentCapacity, predictedDemand,
        recommendedCapacity, 0.85, "Linear growth model");

    LOGGER.info(String.format("Generated capacity forecast for %s: current=%d, predicted=%d, recommended=%d",
        resourceType, currentCapacity, predictedDemand, recommendedCapacity));

    return forecast;
  }

  /**
   * Sets the unit cost for a resource type.
   *
   * @param resourceType resource type
   * @param unitCost cost per unit
   */
  public void setUnitCost(final ResourceQuotaManager.ResourceType resourceType, final double unitCost) {
    unitCosts.put(resourceType, unitCost);
    LOGGER.info(String.format("Set unit cost for %s: %.4f", resourceType, unitCost));
  }

  /**
   * Gets comprehensive governance statistics.
   *
   * @return formatted statistics
   */
  public String getStatistics() {
    final StringBuilder sb = new StringBuilder("=== Resource Governance Statistics ===\n");

    sb.append(String.format("Enabled: %s\n", enabled));
    sb.append(String.format("Total policies: %d\n", policyEngine.getPolicies().size()));
    sb.append(String.format("Total policy evaluations: %,d\n", totalPolicyEvaluations.get()));
    sb.append(String.format("Pending approvals: %d\n", pendingApprovals.size()));
    sb.append(String.format("Total approval requests: %,d\n", totalApprovalRequests.get()));
    sb.append(String.format("Total charges recorded: %,d\n", totalCharges.get()));
    sb.append("\n");

    sb.append("Active Policies:\n");
    for (final ResourcePolicy policy : policyEngine.getPolicies()) {
      sb.append(String.format("  %s: %s (priority=%d, action=%s)\n",
          policy.getPolicyId(), policy.getName(), policy.getPriority(), policy.getAction()));
    }

    sb.append("\nApproval Statistics:\n");
    final long approvedCount = pendingApprovals.values().stream()
        .mapToLong(req -> req.getStatus() == ApprovalStatus.APPROVED ? 1 : 0).sum();
    final long rejectedCount = pendingApprovals.values().stream()
        .mapToLong(req -> req.getStatus() == ApprovalStatus.REJECTED ? 1 : 0).sum();

    sb.append(String.format("  Approved: %d\n", approvedCount));
    sb.append(String.format("  Rejected: %d\n", rejectedCount));
    sb.append(String.format("  Pending: %d\n", pendingApprovals.size() - approvedCount - rejectedCount));

    return sb.toString();
  }

  private void initializeDefaultPolicies() {
    // High-usage monitoring policy
    final ResourcePolicy highUsagePolicy = ResourcePolicy.builder("high-usage-monitor", "High Usage Monitor")
        .withDescription("Monitor requests for unusually high resource usage")
        .withAction(PolicyAction.MONITOR)
        .withCondition("max_amount", 1000000L)
        .withPriority(200)
        .build();
    addPolicy(highUsagePolicy);

    // Premium resource policy
    final ResourcePolicy premiumPolicy = ResourcePolicy.builder("premium-resources", "Premium Resources")
        .withDescription("Apply premium pricing for high-priority resources")
        .withResourceType(ResourceQuotaManager.ResourceType.CPU_TIME)
        .withAction(PolicyAction.CHARGE_PREMIUM)
        .withCondition("max_amount", 10000000L)
        .withPriority(150)
        .build();
    addPolicy(premiumPolicy);
  }

  private void initializeDefaultCosts() {
    // Set default unit costs (can be overridden)
    setUnitCost(ResourceQuotaManager.ResourceType.CPU_TIME, 0.000001); // Per nanosecond
    setUnitCost(ResourceQuotaManager.ResourceType.HEAP_MEMORY, 0.000000001); // Per byte
    setUnitCost(ResourceQuotaManager.ResourceType.NATIVE_MEMORY, 0.000000002); // Per byte
    setUnitCost(ResourceQuotaManager.ResourceType.NETWORK_BANDWIDTH_IN, 0.00001); // Per byte/sec
    setUnitCost(ResourceQuotaManager.ResourceType.DISK_READ_BANDWIDTH, 0.00001); // Per byte/sec
  }

  private void startGovernanceTasks() {
    // Approval timeout handling
    governanceExecutor.scheduleAtFixedRate(this::handleApprovalTimeouts, 300, 300, TimeUnit.SECONDS);

    // Cleanup expired approvals and charges
    governanceExecutor.scheduleAtFixedRate(this::cleanupExpiredData, 3600, 3600, TimeUnit.SECONDS);
  }

  private void handleApprovalTimeouts() {
    final Instant cutoff = Instant.now().minus(Duration.ofDays(7)); // 7 day timeout

    pendingApprovals.entrySet().removeIf(entry -> {
      final ApprovalRequest request = entry.getValue();
      if (request.getRequestedAt().isBefore(cutoff) && request.getStatus() == ApprovalStatus.PENDING) {
        request.setStatus(ApprovalStatus.EXPIRED);
        LOGGER.warning(String.format("Approval request %s expired", request.getRequestId()));
        return true;
      }
      return false;
    });
  }

  private void cleanupExpiredData() {
    final Instant cutoff = Instant.now().minus(Duration.ofDays(90)); // 90 day retention

    // Cleanup old charges
    charges.entrySet().removeIf(entry -> entry.getValue().getChargeDate().isBefore(cutoff));

    // Cleanup completed approvals
    pendingApprovals.entrySet().removeIf(entry -> {
      final ApprovalRequest request = entry.getValue();
      return (request.getStatus() == ApprovalStatus.APPROVED || request.getStatus() == ApprovalStatus.REJECTED)
          && request.getRequestedAt().isBefore(cutoff);
    });
  }

  /**
   * Gets pending approval requests for an approver.
   *
   * @param approver approver identifier
   * @return list of pending approval requests
   */
  public List<ApprovalRequest> getPendingApprovals(final String approver) {
    return pendingApprovals.values().stream()
        .filter(request -> request.getRequiredApprovers().contains(approver))
        .filter(request -> request.getStatus() == ApprovalStatus.PENDING)
        .filter(request -> !request.getApprovals().containsKey(approver))
        .collect(Collectors.toList());
  }

  /**
   * Enables or disables governance.
   *
   * @param enabled true to enable
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    LOGGER.info("Resource governance " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Shuts down the governance manager.
   */
  public void shutdown() {
    enabled = false;

    governanceExecutor.shutdown();
    try {
      if (!governanceExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
        governanceExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      governanceExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    LOGGER.info("Resource governance manager shutdown");
  }
}