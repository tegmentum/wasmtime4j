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

package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.resource.*;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * JMH benchmarks for resource management system performance. Measures overhead and throughput of
 * various resource management operations.
 *
 * @since 1.0.0
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
public class ResourceManagementBenchmark {

  private ResourceQuotaManager quotaManager;
  private ResourceScheduler scheduler;
  private ResourcePoolManager poolManager;
  private ResourceGovernanceManager governanceManager;
  private ResourceOptimizationEngine optimizationEngine;
  private ResourceSecurityManager securityManager;
  private ResourceObservabilityManager observabilityManager;

  private String[] tenantIds;
  private ResourceQuotaManager.ResourceType[] resourceTypes;
  private ResourceSecurityManager.SecurityContext[] securityContexts;

  @Setup(Level.Trial)
  public void setupBenchmark() {
    // Initialize all resource management components
    quotaManager = new ResourceQuotaManager();
    scheduler =
        new ResourceScheduler(ResourceScheduler.SchedulingAlgorithm.FAIR_SHARE, quotaManager);
    poolManager = new ResourcePoolManager();
    governanceManager = new ResourceGovernanceManager();
    optimizationEngine = new ResourceOptimizationEngine();
    securityManager = new ResourceSecurityManager();
    observabilityManager = new ResourceObservabilityManager();

    // Setup test data
    setupTestData();

    System.out.println("Resource management benchmark initialized");
  }

  @TearDown(Level.Trial)
  public void teardownBenchmark() throws Exception {
    if (observabilityManager != null) observabilityManager.shutdown();
    if (securityManager != null) securityManager.shutdown();
    if (optimizationEngine != null) optimizationEngine.shutdown();
    if (governanceManager != null) governanceManager.shutdown();
    if (poolManager != null) poolManager.shutdown();
    if (scheduler != null) scheduler.shutdown();
    if (quotaManager != null) quotaManager.shutdown();

    System.out.println("Resource management benchmark cleaned up");
  }

  private void setupTestData() {
    // Create test tenants
    tenantIds = new String[100];
    for (int i = 0; i < tenantIds.length; i++) {
      tenantIds[i] = "tenant-" + i;
    }

    // Setup resource types
    resourceTypes = ResourceQuotaManager.ResourceType.values();

    // Setup quotas for all tenants and resource types
    for (String tenantId : tenantIds) {
      for (ResourceQuotaManager.ResourceType resourceType : resourceTypes) {
        ResourceQuotaManager.ResourceQuota quota =
            ResourceQuotaManager.ResourceQuota.builder(resourceType, tenantId)
                .withSoftLimit(1000000L)
                .withHardLimit(2000000L)
                .withBurstLimit(4000000L)
                .withStrategy(ResourceQuotaManager.QuotaEnforcementStrategy.THROTTLE)
                .build();
        quotaManager.setQuota(quota);
      }

      // Create security principals
      securityManager.createPrincipal(
          "user-" + tenantId,
          tenantId,
          ResourceSecurityManager.PrincipalType.USER,
          Set.of("RESOURCE_USER"),
          Map.of());
    }

    // Authenticate security contexts
    securityContexts = new ResourceSecurityManager.SecurityContext[tenantIds.length];
    for (int i = 0; i < tenantIds.length; i++) {
      securityContexts[i] =
          securityManager.authenticate(
              "user-" + tenantIds[i], Map.of("password", "benchmark-password"), "benchmark-client");
    }

    // Setup resource pools
    for (ResourceQuotaManager.ResourceType resourceType : resourceTypes) {
      ResourcePoolManager.PoolConfiguration config =
          ResourcePoolManager.PoolConfiguration.builder(resourceType)
              .withMinSize(10)
              .withMaxSize(100)
              .withInitialSize(20)
              .build();

      poolManager.createPool(
          config, () -> "BenchmarkResource-" + System.nanoTime(), resource -> true);
    }

    // Setup governance policies
    for (int i = 0; i < 10; i++) {
      ResourceGovernanceManager.ResourcePolicy policy =
          ResourceGovernanceManager.ResourcePolicy.builder(
                  "benchmark-policy-" + i, "Benchmark Policy " + i)
              .withAction(ResourceGovernanceManager.PolicyAction.MONITOR)
              .withPriority(100)
              .build();
      governanceManager.addPolicy(policy);
    }

    // Setup observability rules
    for (int i = 0; i < 5; i++) {
      ResourceObservabilityManager.AlertRule alertRule =
          ResourceObservabilityManager.AlertRule.builder(
                  "benchmark-alert-" + i, "Benchmark Alert " + i)
              .withMetricQuery("benchmark.metric." + i)
              .withCondition(ResourceObservabilityManager.AlertCondition.GREATER_THAN)
              .withThreshold(1000.0)
              .build();
      observabilityManager.addAlertRule(alertRule);
    }
  }

  @Benchmark
  @Group("quota_management")
  @GroupThreads(4)
  public void benchmarkQuotaCheck(Blackhole bh) {
    String tenantId = getRandomTenant();
    ResourceQuotaManager.ResourceType resourceType = getRandomResourceType();
    long amount = ThreadLocalRandom.current().nextLong(1000, 50000);

    ResourceQuotaManager.QuotaCheckResult result =
        quotaManager.checkQuota(tenantId, resourceType, amount);
    bh.consume(result);
  }

  @Benchmark
  @Group("quota_management")
  @GroupThreads(2)
  public void benchmarkUsageRecording(Blackhole bh) {
    String tenantId = getRandomTenant();
    ResourceQuotaManager.ResourceType resourceType = getRandomResourceType();
    long amount = ThreadLocalRandom.current().nextLong(100, 5000);

    quotaManager.recordUsage(tenantId, resourceType, amount);
    ResourceQuotaManager.ResourceUsageStats stats =
        quotaManager.getUsageStats(tenantId, resourceType);
    bh.consume(stats);
  }

  @Benchmark
  @Group("quota_management")
  @GroupThreads(2)
  public void benchmarkQuotaDeallocation(Blackhole bh) {
    String tenantId = getRandomTenant();
    ResourceQuotaManager.ResourceType resourceType = getRandomResourceType();
    long amount = ThreadLocalRandom.current().nextLong(100, 2000);

    quotaManager.recordDeallocation(tenantId, resourceType, amount);
    bh.consume(amount);
  }

  @Benchmark
  @Group("security")
  @GroupThreads(4)
  public void benchmarkSecurityAuthorization(Blackhole bh) {
    ResourceSecurityManager.SecurityContext context = getRandomSecurityContext();
    String tenantId = context.getPrincipal().getTenantId();
    ResourceQuotaManager.ResourceType resourceType = getRandomResourceType();
    ResourceSecurityManager.ResourceOperation operation = getRandomOperation();

    boolean authorized = securityManager.authorize(context, tenantId, resourceType, operation);
    bh.consume(authorized);
  }

  @Benchmark
  @Group("security")
  @GroupThreads(2)
  public void benchmarkSecurityAuthentication(Blackhole bh) {
    String tenantId = getRandomTenant();
    String principalId = "user-" + tenantId;

    ResourceSecurityManager.SecurityContext context =
        securityManager.authenticate(
            principalId, Map.of("password", "benchmark-password"), "benchmark-client");
    bh.consume(context);
  }

  @Benchmark
  @Group("pool_management")
  @GroupThreads(4)
  public void benchmarkResourceAcquisition(Blackhole bh) {
    String tenantId = getRandomTenant();
    ResourceQuotaManager.ResourceType resourceType = getRandomResourceType();

    ResourcePoolManager.PooledResource<String> resource =
        poolManager.acquire(resourceType, tenantId, Duration.ofMillis(100));

    if (resource != null) {
      bh.consume(resource);
      // Immediately release to avoid pool exhaustion
      poolManager.release(resource);
    }
  }

  @Benchmark
  @Group("observability")
  @GroupThreads(4)
  public void benchmarkMetricRecording(Blackhole bh) {
    String tenantId = getRandomTenant();
    double value = ThreadLocalRandom.current().nextDouble(0, 1000);

    ResourceObservabilityManager.ResourceMetric metric =
        new ResourceObservabilityManager.ResourceMetric(
            "benchmark-metric-" + System.nanoTime(),
            "benchmark.throughput",
            "Benchmark throughput metric",
            ResourceObservabilityManager.MetricType.GAUGE,
            "ops/sec",
            Map.of("tenant", tenantId, "benchmark", "resource_management"),
            value,
            ResourceObservabilityManager.MetricSource.APPLICATION);

    observabilityManager.recordMetric(metric);
    bh.consume(metric);
  }

  @Benchmark
  @Group("optimization")
  @GroupThreads(2)
  public void benchmarkOptimizationDataRecording(Blackhole bh) {
    String tenantId = getRandomTenant();
    ResourceQuotaManager.ResourceType resourceType = getRandomResourceType();
    double usage = ThreadLocalRandom.current().nextDouble(100, 1000);
    double quota = ThreadLocalRandom.current().nextDouble(1000, 2000);

    optimizationEngine.recordUsage(
        tenantId, resourceType, usage, quota, Map.of("benchmark", "optimization"));
    bh.consume(usage);
  }

  @Benchmark
  @Group("optimization")
  @GroupThreads(1)
  public void benchmarkUsagePatternAnalysis(Blackhole bh) {
    String tenantId = getRandomTenant();
    ResourceQuotaManager.ResourceType resourceType = getRandomResourceType();

    // Record some usage data first
    for (int i = 0; i < 10; i++) {
      double usage = ThreadLocalRandom.current().nextDouble(100, 1000);
      optimizationEngine.recordUsage(tenantId, resourceType, usage, 2000.0, Map.of());
    }

    ResourceOptimizationEngine.UsagePattern pattern =
        optimizationEngine.analyzeUsagePattern(tenantId, resourceType);
    bh.consume(pattern);
  }

  @Benchmark
  @Group("governance")
  @GroupThreads(2)
  public void benchmarkPolicyEvaluation(Blackhole bh) {
    String tenantId = getRandomTenant();
    ResourceQuotaManager.ResourceType resourceType = getRandomResourceType();
    long amount = ThreadLocalRandom.current().nextLong(100, 1000);

    ResourceGovernanceManager.PolicyAction action =
        governanceManager.evaluateRequest(
            tenantId, resourceType, amount, Map.of("benchmark", "governance"));
    bh.consume(action);
  }

  @Benchmark
  public void benchmarkSchedulerRequestSubmission(Blackhole bh) {
    String tenantId = getRandomTenant();
    ResourceQuotaManager.ResourceType resourceType = getRandomResourceType();
    long amount = ThreadLocalRandom.current().nextLong(10, 100);

    ResourceScheduler.ResourceRequest request =
        ResourceScheduler.ResourceRequest.builder(
                "benchmark-req-" + System.nanoTime(), tenantId, resourceType, amount)
            .withEstimatedDuration(Duration.ofMillis(50))
            .withPriority(ThreadLocalRandom.current().nextInt(50, 200))
            .build();

    boolean submitted = scheduler.submit(request);
    bh.consume(submitted);

    // Cancel request to avoid queue buildup
    scheduler.cancel(request.getRequestId());
  }

  // Comprehensive benchmark combining all components
  @Benchmark
  public void benchmarkFullResourceOperation(Blackhole bh) {
    String tenantId = getRandomTenant();
    ResourceQuotaManager.ResourceType resourceType = getRandomResourceType();
    ResourceSecurityManager.SecurityContext context = getRandomSecurityContext();
    long amount = ThreadLocalRandom.current().nextLong(100, 1000);

    // 1. Security check
    boolean authorized =
        securityManager.authorize(
            context, tenantId, resourceType, ResourceSecurityManager.ResourceOperation.ALLOCATE);

    if (authorized) {
      // 2. Policy evaluation
      ResourceGovernanceManager.PolicyAction policyAction =
          governanceManager.evaluateRequest(
              tenantId, resourceType, amount, Map.of("operation", "benchmark"));

      if (policyAction == ResourceGovernanceManager.PolicyAction.ALLOW
          || policyAction == ResourceGovernanceManager.PolicyAction.MONITOR) {

        // 3. Quota check
        ResourceQuotaManager.QuotaCheckResult quotaResult =
            quotaManager.checkQuota(tenantId, resourceType, amount);

        if (quotaResult.isAllowed()) {
          // 4. Record usage
          quotaManager.recordUsage(tenantId, resourceType, amount);

          // 5. Record optimization data
          optimizationEngine.recordUsage(
              tenantId, resourceType, amount, 2000.0, Map.of("benchmark", "full_operation"));

          // 6. Record metrics
          ResourceObservabilityManager.ResourceMetric metric =
              new ResourceObservabilityManager.ResourceMetric(
                  "full-op-" + System.nanoTime(),
                  "resource.operation.success",
                  "Successful resource operation",
                  ResourceObservabilityManager.MetricType.COUNTER,
                  "operations",
                  Map.of("tenant", tenantId, "resource", resourceType.name()),
                  1.0,
                  ResourceObservabilityManager.MetricSource.APPLICATION);
          observabilityManager.recordMetric(metric);

          bh.consume(amount);
        } else {
          bh.consume(quotaResult.getReason());
        }
      } else {
        bh.consume(policyAction);
      }
    } else {
      bh.consume("unauthorized");
    }
  }

  // Utility methods for random test data selection
  private String getRandomTenant() {
    return tenantIds[ThreadLocalRandom.current().nextInt(tenantIds.length)];
  }

  private ResourceQuotaManager.ResourceType getRandomResourceType() {
    return resourceTypes[ThreadLocalRandom.current().nextInt(resourceTypes.length)];
  }

  private ResourceSecurityManager.SecurityContext getRandomSecurityContext() {
    ResourceSecurityManager.SecurityContext context;
    do {
      context = securityContexts[ThreadLocalRandom.current().nextInt(securityContexts.length)];
    } while (context == null);
    return context;
  }

  private ResourceSecurityManager.ResourceOperation getRandomOperation() {
    ResourceSecurityManager.ResourceOperation[] operations =
        ResourceSecurityManager.ResourceOperation.values();
    return operations[ThreadLocalRandom.current().nextInt(operations.length)];
  }

  // Benchmark for measuring baseline overhead
  @Benchmark
  public void benchmarkBaseline(Blackhole bh) {
    // Minimal operations to establish baseline
    String tenantId = getRandomTenant();
    ResourceQuotaManager.ResourceType resourceType = getRandomResourceType();
    long amount = ThreadLocalRandom.current().nextLong(100, 1000);

    bh.consume(tenantId);
    bh.consume(resourceType);
    bh.consume(amount);
  }

  // Memory allocation benchmark
  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void benchmarkMemoryAllocation(Blackhole bh) {
    // Create temporary objects similar to what resource management creates
    String tenantId = getRandomTenant();
    ResourceQuotaManager.ResourceType resourceType = getRandomResourceType();

    Map<String, String> labels =
        Map.of(
            "tenant", tenantId, "resource", resourceType.name(), "benchmark", "memory_allocation");

    ResourceObservabilityManager.ResourceMetric metric =
        new ResourceObservabilityManager.ResourceMetric(
            "memory-test-" + System.nanoTime(),
            "memory.allocation.test",
            "Memory allocation test metric",
            ResourceObservabilityManager.MetricType.GAUGE,
            "bytes",
            labels,
            ThreadLocalRandom.current().nextDouble(1000, 10000),
            ResourceObservabilityManager.MetricSource.SYSTEM);

    bh.consume(metric);
  }

  public static void main(String[] args) throws RunnerException {
    Options options =
        new OptionsBuilder()
            .include(ResourceManagementBenchmark.class.getSimpleName())
            .forks(1)
            .warmupIterations(2)
            .measurementIterations(3)
            .threads(1)
            .shouldDoGC(true)
            .build();

    new Runner(options).run();
  }
}
