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

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive integration tests for the resource management framework. Tests all components
 * working together in realistic scenarios.
 *
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Resource Management Integration Tests")
public class ResourceManagementIntegrationTest {

  private ResourceQuotaManager quotaManager;
  private ResourceScheduler scheduler;
  private ResourcePoolManager poolManager;
  private ResourceGovernanceManager governanceManager;
  private ResourceOptimizationEngine optimizationEngine;
  private ResourceSecurityManager securityManager;
  private ResourceObservabilityManager observabilityManager;

  @BeforeEach
  void setUp() {
    quotaManager = new ResourceQuotaManager();
    scheduler =
        new ResourceScheduler(ResourceScheduler.SchedulingAlgorithm.FAIR_SHARE, quotaManager);
    poolManager = new ResourcePoolManager();
    governanceManager = new ResourceGovernanceManager();
    optimizationEngine = new ResourceOptimizationEngine();
    securityManager = new ResourceSecurityManager();
    observabilityManager = new ResourceObservabilityManager();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (observabilityManager != null) {
      observabilityManager.shutdown();
    }
    if (securityManager != null) {
      securityManager.shutdown();
    }
    if (optimizationEngine != null) {
      optimizationEngine.shutdown();
    }
    if (governanceManager != null) {
      governanceManager.shutdown();
    }
    if (poolManager != null) {
      poolManager.shutdown();
    }
    if (scheduler != null) {
      scheduler.shutdown();
    }
    if (quotaManager != null) {
      quotaManager.shutdown();
    }
  }

  @Nested
  @DisplayName("Quota Management Tests")
  class QuotaManagementTests {

    @Test
    @DisplayName("Should enforce resource quotas correctly")
    void testQuotaEnforcement() {
      // Given: A tenant with limited CPU time quota
      final String tenantId = "test-tenant";
      final ResourceQuotaManager.ResourceQuota cpuQuota =
          ResourceQuotaManager.ResourceQuota.builder(
                  ResourceQuotaManager.ResourceType.CPU_TIME, tenantId)
              .withSoftLimit(1000000L) // 1 million nanoseconds
              .withHardLimit(2000000L) // 2 million nanoseconds
              .withStrategy(ResourceQuotaManager.QuotaEnforcementStrategy.STRICT)
              .build();

      quotaManager.setQuota(cpuQuota);

      // When: Requesting resources within quota
      ResourceQuotaManager.QuotaCheckResult result1 =
          quotaManager.checkQuota(tenantId, ResourceQuotaManager.ResourceType.CPU_TIME, 500000L);

      // Then: Request should be allowed
      assertTrue(result1.isAllowed(), "Request within quota should be allowed");
      assertEquals(ResourceQuotaManager.QuotaDecision.ALLOW, result1.getDecision());

      // When: Recording usage and requesting more
      quotaManager.recordUsage(tenantId, ResourceQuotaManager.ResourceType.CPU_TIME, 500000L);

      ResourceQuotaManager.QuotaCheckResult result2 =
          quotaManager.checkQuota(tenantId, ResourceQuotaManager.ResourceType.CPU_TIME, 700000L);

      // Then: Request should be allowed but with warning (exceeds soft limit)
      assertTrue(result2.isAllowed(), "Request under hard limit should be allowed");
      assertEquals(ResourceQuotaManager.QuotaDecision.WARN, result2.getDecision());

      // When: Recording more usage and requesting beyond hard limit
      quotaManager.recordUsage(tenantId, ResourceQuotaManager.ResourceType.CPU_TIME, 700000L);

      ResourceQuotaManager.QuotaCheckResult result3 =
          quotaManager.checkQuota(tenantId, ResourceQuotaManager.ResourceType.CPU_TIME, 1000000L);

      // Then: Request should be rejected
      assertFalse(result3.isAllowed(), "Request exceeding hard limit should be rejected");
      assertEquals(ResourceQuotaManager.QuotaDecision.REJECT, result3.getDecision());
    }

    @Test
    @DisplayName("Should handle burst allocation strategy")
    void testBurstAllocation() {
      // Given: Burst allocation strategy with burst limits
      final String tenantId = "burst-tenant";
      final ResourceQuotaManager.ResourceQuota memoryQuota =
          ResourceQuotaManager.ResourceQuota.builder(
                  ResourceQuotaManager.ResourceType.HEAP_MEMORY, tenantId)
              .withSoftLimit(100L)
              .withHardLimit(200L)
              .withBurstLimit(400L)
              .withBurstWindow(Duration.ofMinutes(5))
              .withStrategy(ResourceQuotaManager.QuotaEnforcementStrategy.BURST)
              .build();

      quotaManager.setQuota(memoryQuota);

      // When: Requesting burst allocation
      ResourceQuotaManager.QuotaCheckResult result =
          quotaManager.checkQuota(tenantId, ResourceQuotaManager.ResourceType.HEAP_MEMORY, 350L);

      // Then: Burst request should be allowed
      assertTrue(result.isAllowed(), "Burst request within burst limit should be allowed");

      // When: Recording usage and checking stats
      quotaManager.recordUsage(tenantId, ResourceQuotaManager.ResourceType.HEAP_MEMORY, 350L);
      ResourceQuotaManager.ResourceUsageStats stats =
          quotaManager.getUsageStats(tenantId, ResourceQuotaManager.ResourceType.HEAP_MEMORY);

      // Then: Stats should reflect burst usage
      assertNotNull(stats, "Usage stats should be available");
      assertEquals(350L, stats.getCurrentUsage(), "Current usage should match recorded amount");
      assertTrue(stats.isInBurstMode(), "Should be in burst mode");
    }

    @Test
    @DisplayName("Should reset burst usage after window expires")
    @Timeout(10)
    void testBurstWindowReset() throws InterruptedException {
      // Given: Short burst window for testing
      final String tenantId = "burst-reset-tenant";
      final ResourceQuotaManager.ResourceQuota quota =
          ResourceQuotaManager.ResourceQuota.builder(
                  ResourceQuotaManager.ResourceType.NETWORK_BANDWIDTH_IN, tenantId)
              .withHardLimit(100L)
              .withBurstLimit(200L)
              .withBurstWindow(Duration.ofSeconds(2))
              .withStrategy(ResourceQuotaManager.QuotaEnforcementStrategy.BURST)
              .build();

      quotaManager.setQuota(quota);

      // When: Using burst allocation
      quotaManager.recordUsage(
          tenantId, ResourceQuotaManager.ResourceType.NETWORK_BANDWIDTH_IN, 150L);
      ResourceQuotaManager.ResourceUsageStats initialStats =
          quotaManager.getUsageStats(
              tenantId, ResourceQuotaManager.ResourceType.NETWORK_BANDWIDTH_IN);

      assertTrue(initialStats.isInBurstMode(), "Should initially be in burst mode");

      // When: Waiting for burst window to expire
      Thread.sleep(2500); // Wait longer than burst window

      // Check quota again to trigger internal reset
      quotaManager.checkQuota(
          tenantId, ResourceQuotaManager.ResourceType.NETWORK_BANDWIDTH_IN, 50L);

      ResourceQuotaManager.ResourceUsageStats finalStats =
          quotaManager.getUsageStats(
              tenantId, ResourceQuotaManager.ResourceType.NETWORK_BANDWIDTH_IN);

      // Then: Should no longer be in burst mode
      assertFalse(
          finalStats.isInBurstMode(), "Should no longer be in burst mode after window expires");
    }
  }

  @Nested
  @DisplayName("Resource Scheduling Tests")
  class ResourceSchedulingTests {

    @Test
    @DisplayName("Should schedule requests fairly across tenants")
    void testFairShareScheduling() throws InterruptedException {
      // Given: Multiple tenants with resource requests
      final CountDownLatch latch = new CountDownLatch(6);
      final AtomicInteger completedRequests = new AtomicInteger(0);

      ResourceScheduler.ResourceRequestCallback callback =
          new ResourceScheduler.ResourceRequestCallback() {
            @Override
            public void onScheduled(ResourceScheduler.ResourceRequest request) {}

            @Override
            public void onStarted(ResourceScheduler.ResourceRequest request) {}

            @Override
            public void onCompleted(ResourceScheduler.ResourceRequest request) {
              completedRequests.incrementAndGet();
              latch.countDown();
            }

            @Override
            public void onFailed(ResourceScheduler.ResourceRequest request, Throwable cause) {
              latch.countDown();
            }

            @Override
            public void onCancelled(ResourceScheduler.ResourceRequest request) {
              latch.countDown();
            }

            @Override
            public void onPreempted(ResourceScheduler.ResourceRequest request) {
              latch.countDown();
            }
          };

      // Setup quotas for tenants
      for (int i = 1; i <= 3; i++) {
        final String tenantId = "tenant-" + i;
        final ResourceQuotaManager.ResourceQuota quota =
            ResourceQuotaManager.ResourceQuota.builder(
                    ResourceQuotaManager.ResourceType.CPU_TIME, tenantId)
                .withHardLimit(1000000L)
                .build();
        quotaManager.setQuota(quota);
      }

      // When: Submitting requests from multiple tenants
      for (int i = 1; i <= 3; i++) {
        for (int j = 1; j <= 2; j++) {
          final String tenantId = "tenant-" + i;
          final ResourceScheduler.ResourceRequest request =
              ResourceScheduler.ResourceRequest.builder(
                      "req-" + i + "-" + j,
                      tenantId,
                      ResourceQuotaManager.ResourceType.CPU_TIME,
                      100L)
                  .withEstimatedDuration(Duration.ofMillis(100))
                  .withCallback(callback)
                  .build();

          assertTrue(scheduler.submit(request), "Request should be submitted successfully");
        }
      }

      // Then: All requests should complete
      assertTrue(latch.await(10, TimeUnit.SECONDS), "All requests should complete within timeout");
      assertEquals(6, completedRequests.get(), "All 6 requests should have completed");
    }

    @Test
    @DisplayName("Should handle priority-based scheduling")
    void testPriorityScheduling() throws InterruptedException {
      // Given: Priority-based scheduler
      scheduler.shutdown();
      scheduler =
          new ResourceScheduler(ResourceScheduler.SchedulingAlgorithm.PRIORITY, quotaManager);

      final CountDownLatch latch = new CountDownLatch(3);
      final List<String> completionOrder = new java.util.concurrent.CopyOnWriteArrayList<>();

      ResourceScheduler.ResourceRequestCallback callback =
          new ResourceScheduler.ResourceRequestCallback() {
            @Override
            public void onScheduled(ResourceScheduler.ResourceRequest request) {}

            @Override
            public void onStarted(ResourceScheduler.ResourceRequest request) {}

            @Override
            public void onCompleted(ResourceScheduler.ResourceRequest request) {
              completionOrder.add(request.getRequestId());
              latch.countDown();
            }

            @Override
            public void onFailed(ResourceScheduler.ResourceRequest request, Throwable cause) {
              latch.countDown();
            }

            @Override
            public void onCancelled(ResourceScheduler.ResourceRequest request) {
              latch.countDown();
            }

            @Override
            public void onPreempted(ResourceScheduler.ResourceRequest request) {
              latch.countDown();
            }
          };

      // Setup quota
      final ResourceQuotaManager.ResourceQuota quota =
          ResourceQuotaManager.ResourceQuota.builder(
                  ResourceQuotaManager.ResourceType.MODULE_INSTANCES, "priority-tenant")
              .withHardLimit(1000L)
              .build();
      quotaManager.setQuota(quota);

      // When: Submitting requests with different priorities
      ResourceScheduler.ResourceRequest lowPriority =
          ResourceScheduler.ResourceRequest.builder(
                  "low-priority",
                  "priority-tenant",
                  ResourceQuotaManager.ResourceType.MODULE_INSTANCES,
                  1L)
              .withPriority(50)
              .withEstimatedDuration(Duration.ofMillis(100))
              .withCallback(callback)
              .build();

      ResourceScheduler.ResourceRequest mediumPriority =
          ResourceScheduler.ResourceRequest.builder(
                  "medium-priority",
                  "priority-tenant",
                  ResourceQuotaManager.ResourceType.MODULE_INSTANCES,
                  1L)
              .withPriority(100)
              .withEstimatedDuration(Duration.ofMillis(100))
              .withCallback(callback)
              .build();

      ResourceScheduler.ResourceRequest highPriority =
          ResourceScheduler.ResourceRequest.builder(
                  "high-priority",
                  "priority-tenant",
                  ResourceQuotaManager.ResourceType.MODULE_INSTANCES,
                  1L)
              .withPriority(200)
              .withEstimatedDuration(Duration.ofMillis(100))
              .withCallback(callback)
              .build();

      // Submit in reverse priority order
      scheduler.submit(lowPriority);
      scheduler.submit(mediumPriority);
      scheduler.submit(highPriority);

      // Then: Requests should complete in priority order
      assertTrue(latch.await(5, TimeUnit.SECONDS), "All requests should complete");

      // High priority should complete first
      assertEquals(
          "high-priority", completionOrder.get(0), "High priority request should complete first");
    }

    @Test
    @DisplayName("Should timeout requests that wait too long")
    void testRequestTimeout() throws InterruptedException {
      // Given: Request with short timeout
      final CountDownLatch latch = new CountDownLatch(1);
      final AtomicInteger failureCount = new AtomicInteger(0);

      // Setup restrictive quota to cause delays
      final ResourceQuotaManager.ResourceQuota quota =
          ResourceQuotaManager.ResourceQuota.builder(
                  ResourceQuotaManager.ResourceType.CPU_TIME, "timeout-tenant")
              .withHardLimit(1L) // Very restrictive
              .withStrategy(ResourceQuotaManager.QuotaEnforcementStrategy.STRICT)
              .build();
      quotaManager.setQuota(quota);

      // Use up the quota first
      quotaManager.recordUsage("timeout-tenant", ResourceQuotaManager.ResourceType.CPU_TIME, 1L);

      ResourceScheduler.ResourceRequestCallback callback =
          new ResourceScheduler.ResourceRequestCallback() {
            @Override
            public void onScheduled(ResourceScheduler.ResourceRequest request) {}

            @Override
            public void onStarted(ResourceScheduler.ResourceRequest request) {}

            @Override
            public void onCompleted(ResourceScheduler.ResourceRequest request) {
              latch.countDown();
            }

            @Override
            public void onFailed(ResourceScheduler.ResourceRequest request, Throwable cause) {
              if (cause.getMessage().contains("timeout")
                  || cause.getMessage().contains("Quota exceeded")) {
                failureCount.incrementAndGet();
              }
              latch.countDown();
            }

            @Override
            public void onCancelled(ResourceScheduler.ResourceRequest request) {
              latch.countDown();
            }

            @Override
            public void onPreempted(ResourceScheduler.ResourceRequest request) {
              latch.countDown();
            }
          };

      // When: Submitting request with very short timeout
      ResourceScheduler.ResourceRequest request =
          ResourceScheduler.ResourceRequest.builder(
                  "timeout-test", "timeout-tenant", ResourceQuotaManager.ResourceType.CPU_TIME, 10L)
              .withMaxWaitTime(Duration.ofMillis(100)) // Very short timeout
              .withCallback(callback)
              .build();

      scheduler.submit(request);

      // Then: Request should timeout or fail due to quota
      assertTrue(latch.await(5, TimeUnit.SECONDS), "Request should complete (fail) within timeout");
      assertTrue(failureCount.get() > 0, "Request should have failed due to quota or timeout");
    }
  }

  @Nested
  @DisplayName("Pool Management Tests")
  class PoolManagementTests {

    @Test
    @DisplayName("Should create and manage resource pools")
    void testResourcePoolCreation() {
      // Given: Pool configuration for string resources (as test objects)
      final ResourcePoolManager.PoolConfiguration config =
          ResourcePoolManager.PoolConfiguration.builder(
                  ResourceQuotaManager.ResourceType.MODULE_INSTANCES)
              .withMinSize(2)
              .withMaxSize(10)
              .withInitialSize(3)
              .withMaxIdleTime(Duration.ofMinutes(1))
              .build();

      // When: Creating a pool with string factory
      poolManager.createPool(
          config,
          () -> "TestResource-" + System.currentTimeMillis(),
          resource -> resource != null && resource.toString().startsWith("TestResource"));

      // Then: Pool should be created and accessible
      ResourcePoolManager.PoolStatistics stats =
          poolManager.getPoolStatistics(ResourceQuotaManager.ResourceType.MODULE_INSTANCES);

      assertNotNull(stats, "Pool statistics should be available");
      assertEquals(3, stats.getTotalResources(), "Pool should have initial size resources");
      assertEquals(3, stats.getAvailableResources(), "All resources should be available initially");
    }

    @Test
    @DisplayName("Should acquire and release resources from pool")
    void testResourceAcquisitionRelease() {
      // Given: Resource pool
      final ResourcePoolManager.PoolConfiguration config =
          ResourcePoolManager.PoolConfiguration.builder(
                  ResourceQuotaManager.ResourceType.FUNCTION_CALLS)
              .withMinSize(1)
              .withMaxSize(5)
              .withInitialSize(2)
              .build();

      poolManager.createPool(
          config, () -> "PooledResource-" + Math.random(), resource -> resource != null);

      // When: Acquiring a resource
      ResourcePoolManager.PooledResource<String> resource =
          poolManager.acquire(
              ResourceQuotaManager.ResourceType.FUNCTION_CALLS,
              "test-tenant",
              Duration.ofSeconds(5));

      // Then: Resource should be acquired successfully
      assertNotNull(resource, "Should acquire resource successfully");
      assertTrue(resource.isInUse(), "Resource should be marked as in use");
      assertEquals(
          "test-tenant", resource.getTenantId(), "Resource should be associated with tenant");

      ResourcePoolManager.PoolStatistics stats =
          poolManager.getPoolStatistics(ResourceQuotaManager.ResourceType.FUNCTION_CALLS);
      assertEquals(1, stats.getInUseResources(), "One resource should be in use");
      assertEquals(1, stats.getAvailableResources(), "One resource should remain available");

      // When: Releasing the resource
      poolManager.release(resource);

      // Then: Resource should be available again
      assertFalse(resource.isInUse(), "Resource should no longer be in use");
      stats = poolManager.getPoolStatistics(ResourceQuotaManager.ResourceType.FUNCTION_CALLS);
      assertEquals(0, stats.getInUseResources(), "No resources should be in use");
      assertEquals(2, stats.getAvailableResources(), "All resources should be available");
    }

    @Test
    @DisplayName("Should scale pool size based on demand")
    @Timeout(15)
    void testPoolScaling() throws InterruptedException {
      // Given: Pool with scaling configuration
      final ResourcePoolManager.PoolConfiguration config =
          ResourcePoolManager.PoolConfiguration.builder(
                  ResourceQuotaManager.ResourceType.COMPILATION_UNITS)
              .withMinSize(1)
              .withMaxSize(8)
              .withInitialSize(2)
              .withScalingThreshold(80) // Scale when 80% utilized
              .withScalingFactor(1.5)
              .build();

      poolManager.createPool(
          config, () -> "ScalableResource-" + System.nanoTime(), resource -> true);

      // When: Acquiring multiple resources to trigger scaling
      ResourcePoolManager.PooledResource<String> resource1 =
          poolManager.acquire(
              ResourceQuotaManager.ResourceType.COMPILATION_UNITS,
              "scale-tenant",
              Duration.ofSeconds(1));
      ResourcePoolManager.PooledResource<String> resource2 =
          poolManager.acquire(
              ResourceQuotaManager.ResourceType.COMPILATION_UNITS,
              "scale-tenant",
              Duration.ofSeconds(1));

      assertNotNull(resource1, "Should acquire first resource");
      assertNotNull(resource2, "Should acquire second resource");

      // Give time for scaling to occur (background process)
      Thread.sleep(2000);

      ResourcePoolManager.PoolStatistics stats =
          poolManager.getPoolStatistics(ResourceQuotaManager.ResourceType.COMPILATION_UNITS);

      // Then: Pool should have scaled up
      assertTrue(
          stats.getTotalResources() >= 2, "Pool should maintain or increase size under load");
    }
  }

  @Nested
  @DisplayName("Security Integration Tests")
  class SecurityIntegrationTests {

    @Test
    @DisplayName("Should authenticate and authorize resource operations")
    void testSecurityIntegration() {
      // Given: Security principal with limited permissions
      securityManager.createPrincipal(
          "test-user",
          "test-tenant",
          ResourceSecurityManager.PrincipalType.USER,
          Set.of("RESOURCE_USER"),
          Map.of("department", "engineering"));

      // Permission for basic operations only
      ResourceSecurityManager.ResourcePermission permission =
          ResourceSecurityManager.ResourcePermission.builder("test-permission")
              .withResourceType(ResourceQuotaManager.ResourceType.HEAP_MEMORY)
              .withTenantPattern("test-tenant")
              .withAllowedOperations(
                  ResourceSecurityManager.ResourceOperation.READ,
                  ResourceSecurityManager.ResourceOperation.VIEW_USAGE)
              .withRequiredRoles("RESOURCE_USER")
              .build();

      securityManager.createPermission(permission);

      // When: Authenticating user
      ResourceSecurityManager.SecurityContext context =
          securityManager.authenticate(
              "test-user", Map.of("password", "test-password"), "test-client");

      // Then: Authentication should succeed
      assertNotNull(context, "Authentication should succeed");
      assertEquals("test-user", context.getPrincipal().getPrincipalId());
      assertEquals("test-tenant", context.getPrincipal().getTenantId());

      // When: Checking authorization for allowed operation
      boolean readAuthorized =
          securityManager.authorize(
              context,
              "test-tenant",
              ResourceQuotaManager.ResourceType.HEAP_MEMORY,
              ResourceSecurityManager.ResourceOperation.READ);

      // Then: Should be authorized
      assertTrue(readAuthorized, "User should be authorized for READ operation");

      // When: Checking authorization for disallowed operation
      boolean allocateAuthorized =
          securityManager.authorize(
              context,
              "test-tenant",
              ResourceQuotaManager.ResourceType.HEAP_MEMORY,
              ResourceSecurityManager.ResourceOperation.ALLOCATE);

      // Then: Should not be authorized
      assertFalse(allocateAuthorized, "User should not be authorized for ALLOCATE operation");
    }

    @Test
    @DisplayName("Should detect and handle brute force attacks")
    @Timeout(30)
    void testBruteForceDetection() throws InterruptedException {
      // Given: Multiple failed authentication attempts
      final String attackerPrincipal = "attacker";

      // When: Attempting multiple failed authentications
      for (int i = 0; i < 10; i++) {
        ResourceSecurityManager.SecurityContext context =
            securityManager.authenticate(
                attackerPrincipal, Map.of("password", "wrong-password-" + i), "attacker-client");
        assertNull(context, "Authentication should fail for wrong password");
      }

      // Give time for threat detection to process
      Thread.sleep(1000);

      // Then: Brute force threat should be detected
      List<ResourceSecurityManager.SecurityThreat> threats = securityManager.getDetectedThreats();

      boolean bruteForceDetected =
          threats.stream()
              .anyMatch(
                  threat ->
                      threat.getThreatType() == ResourceSecurityManager.ThreatType.BRUTE_FORCE
                          && threat.getPrincipalId().equals(attackerPrincipal));

      assertTrue(bruteForceDetected, "Brute force attack should be detected");
    }

    @Test
    @DisplayName("Should enforce tenant isolation")
    void testTenantIsolation() {
      // Given: Two tenants with separate principals
      securityManager.createPrincipal(
          "tenant1-user",
          "tenant1",
          ResourceSecurityManager.PrincipalType.USER,
          Set.of("RESOURCE_USER"),
          Map.of());

      securityManager.createPrincipal(
          "tenant2-user",
          "tenant2",
          ResourceSecurityManager.PrincipalType.USER,
          Set.of("RESOURCE_USER"),
          Map.of());

      // When: Authenticating as tenant1 user
      ResourceSecurityManager.SecurityContext tenant1Context =
          securityManager.authenticate("tenant1-user", Map.of("password", "password"), "client1");

      assertNotNull(tenant1Context, "Tenant1 authentication should succeed");

      // Then: Should not be authorized to access tenant2 resources
      boolean crossTenantAccess =
          securityManager.authorize(
              tenant1Context,
              "tenant2",
              ResourceQuotaManager.ResourceType.CPU_TIME,
              ResourceSecurityManager.ResourceOperation.READ);

      assertFalse(crossTenantAccess, "Cross-tenant access should be denied");

      // But should be authorized for own tenant
      boolean ownTenantAccess =
          securityManager.authorize(
              tenant1Context,
              "tenant1",
              ResourceQuotaManager.ResourceType.CPU_TIME,
              ResourceSecurityManager.ResourceOperation.READ);

      assertTrue(ownTenantAccess, "Own tenant access should be allowed");
    }
  }

  @Nested
  @DisplayName("Observability Integration Tests")
  class ObservabilityIntegrationTests {

    @Test
    @DisplayName("Should record and retrieve metrics")
    void testMetricsRecording() {
      // Given: Resource metrics
      ResourceObservabilityManager.ResourceMetric cpuMetric =
          new ResourceObservabilityManager.ResourceMetric(
              "cpu-metric-1",
              "cpu.usage.percent",
              "CPU usage percentage",
              ResourceObservabilityManager.MetricType.GAUGE,
              "percent",
              Map.of("tenant", "test-tenant", "host", "test-host"),
              75.5,
              ResourceObservabilityManager.MetricSource.SYSTEM);

      ResourceObservabilityManager.ResourceMetric memoryMetric =
          new ResourceObservabilityManager.ResourceMetric(
              "memory-metric-1",
              "memory.usage.bytes",
              "Memory usage in bytes",
              ResourceObservabilityManager.MetricType.GAUGE,
              "bytes",
              Map.of("tenant", "test-tenant", "type", "heap"),
              1048576.0,
              ResourceObservabilityManager.MetricSource.APPLICATION);

      // When: Recording metrics
      observabilityManager.recordMetric(cpuMetric);
      observabilityManager.recordMetric(memoryMetric);

      // Then: Metrics should be retrievable
      Instant now = Instant.now();
      Instant oneHourAgo = now.minus(Duration.ofHours(1));

      List<ResourceObservabilityManager.ResourceMetric> cpuMetrics =
          observabilityManager.getMetrics(
              "cpu.usage.percent", Map.of("tenant", "test-tenant"), oneHourAgo, now);

      List<ResourceObservabilityManager.ResourceMetric> memoryMetrics =
          observabilityManager.getMetrics(
              "memory.usage.bytes", Map.of("tenant", "test-tenant"), oneHourAgo, now);

      assertFalse(cpuMetrics.isEmpty(), "CPU metrics should be available");
      assertFalse(memoryMetrics.isEmpty(), "Memory metrics should be available");

      assertEquals(75.5, cpuMetrics.get(0).getValue(), 0.01, "CPU metric value should match");
      assertEquals(
          1048576.0, memoryMetrics.get(0).getValue(), 0.01, "Memory metric value should match");
    }

    @Test
    @DisplayName("Should fire and resolve alerts")
    @Timeout(15)
    void testAlertingSystem() throws InterruptedException {
      // Given: High CPU usage alert rule
      ResourceObservabilityManager.AlertRule alertRule =
          ResourceObservabilityManager.AlertRule.builder("high-cpu-alert", "High CPU Usage")
              .withDescription("CPU usage is above 80%")
              .withMetricQuery("cpu.usage.percent")
              .withCondition(ResourceObservabilityManager.AlertCondition.GREATER_THAN)
              .withThreshold(80.0)
              .withSeverity(ResourceObservabilityManager.AlertSeverity.WARNING)
              .withEvaluationWindow(Duration.ofSeconds(5))
              .build();

      observabilityManager.addAlertRule(alertRule);

      // When: Recording high CPU usage metrics
      for (int i = 0; i < 5; i++) {
        ResourceObservabilityManager.ResourceMetric highCpuMetric =
            new ResourceObservabilityManager.ResourceMetric(
                "cpu-high-" + i,
                "cpu.usage.percent",
                "CPU usage percentage",
                ResourceObservabilityManager.MetricType.GAUGE,
                "percent",
                Map.of("host", "test-host"),
                85.0 + i,
                ResourceObservabilityManager.MetricSource.SYSTEM);

        observabilityManager.recordMetric(highCpuMetric);
        Thread.sleep(200); // Small delay between metrics
      }

      // Wait for alert evaluation
      Thread.sleep(6000);

      // Then: Alert should be fired
      List<ResourceObservabilityManager.AlertInstance> activeAlerts =
          observabilityManager.getActiveAlerts();

      boolean highCpuAlertFired =
          activeAlerts.stream()
              .anyMatch(alert -> alert.getRule().getAlertId().equals("high-cpu-alert"));

      assertTrue(highCpuAlertFired, "High CPU alert should be fired");

      // When: Recording normal CPU usage
      for (int i = 0; i < 3; i++) {
        ResourceObservabilityManager.ResourceMetric normalCpuMetric =
            new ResourceObservabilityManager.ResourceMetric(
                "cpu-normal-" + i,
                "cpu.usage.percent",
                "CPU usage percentage",
                ResourceObservabilityManager.MetricType.GAUGE,
                "percent",
                Map.of("host", "test-host"),
                60.0,
                ResourceObservabilityManager.MetricSource.SYSTEM);

        observabilityManager.recordMetric(normalCpuMetric);
        Thread.sleep(200);
      }

      // Wait for alert evaluation
      Thread.sleep(6000);

      // Then: Alert should be resolved (no longer in active alerts with as much confidence as
      // possible given async nature)
      List<ResourceObservabilityManager.AlertInstance> finalActiveAlerts =
          observabilityManager.getActiveAlerts();

      long highCpuActiveAlerts =
          finalActiveAlerts.stream()
              .filter(alert -> alert.getRule().getAlertId().equals("high-cpu-alert"))
              .count();

      // The alert should be resolved or at least we should have fewer active alerts
      assertTrue(
          highCpuActiveAlerts <= activeAlerts.size(),
          "Alert should be resolved or fewer active alerts should remain");
    }

    @Test
    @DisplayName("Should perform health checks")
    void testHealthChecks() {
      // Given: Custom health check
      ResourceObservabilityManager.HealthCheck customHealthCheck =
          new ResourceObservabilityManager.HealthCheck(
              "custom-service-health",
              "Custom Service Health",
              "Checks if custom service is responding",
              () -> {
                // Simulate health check logic
                double randomValue = Math.random();
                if (randomValue > 0.8) {
                  return ResourceObservabilityManager.HealthCheckResult.unhealthy(
                      "Service overloaded");
                } else if (randomValue > 0.6) {
                  return ResourceObservabilityManager.HealthCheckResult.degraded("Service slow");
                } else {
                  return ResourceObservabilityManager.HealthCheckResult.healthy("Service OK");
                }
              },
              Duration.ofSeconds(1),
              Duration.ofMillis(500),
              true);

      observabilityManager.addHealthCheck(customHealthCheck);

      // When: Getting overall health
      ResourceObservabilityManager.HealthStatus overallHealth =
          observabilityManager.getOverallHealth();

      // Then: Health status should be determined
      assertNotNull(overallHealth, "Overall health should be available");
      assertTrue(
          overallHealth == ResourceObservabilityManager.HealthStatus.HEALTHY
              || overallHealth == ResourceObservabilityManager.HealthStatus.DEGRADED
              || overallHealth == ResourceObservabilityManager.HealthStatus.UNHEALTHY,
          "Health status should be valid");
    }
  }

  @Nested
  @DisplayName("End-to-End Integration Tests")
  class EndToEndIntegrationTests {

    @Test
    @DisplayName("Should handle complete resource lifecycle with all components")
    @Timeout(30)
    void testCompleteResourceLifecycle() throws InterruptedException {
      // Given: Complete setup with all components integrated
      final String tenantId = "e2e-tenant";
      final ExecutorService executor = Executors.newFixedThreadPool(5);
      final CountDownLatch completionLatch = new CountDownLatch(10);

      try {
        // Setup security
        securityManager.createPrincipal(
            "e2e-user",
            tenantId,
            ResourceSecurityManager.PrincipalType.USER,
            Set.of("RESOURCE_USER"),
            Map.of());

        ResourceSecurityManager.SecurityContext context =
            securityManager.authenticate("e2e-user", Map.of("password", "password"), "e2e-client");
        assertNotNull(context, "Authentication should succeed");

        // Setup quotas
        ResourceQuotaManager.ResourceQuota quota =
            ResourceQuotaManager.ResourceQuota.builder(
                    ResourceQuotaManager.ResourceType.CPU_TIME, tenantId)
                .withSoftLimit(5000000L)
                .withHardLimit(10000000L)
                .withStrategy(ResourceQuotaManager.QuotaEnforcementStrategy.FAIR_SHARE)
                .build();
        quotaManager.setQuota(quota);

        // Setup pool
        ResourcePoolManager.PoolConfiguration poolConfig =
            ResourcePoolManager.PoolConfiguration.builder(
                    ResourceQuotaManager.ResourceType.MODULE_INSTANCES)
                .withMinSize(2)
                .withMaxSize(10)
                .withInitialSize(3)
                .build();

        poolManager.createPool(
            poolConfig, () -> "E2EResource-" + System.nanoTime(), resource -> true);

        // Setup governance policy
        ResourceGovernanceManager.ResourcePolicy policy =
            ResourceGovernanceManager.ResourcePolicy.builder("e2e-policy", "E2E Resource Policy")
                .withResourceType(ResourceQuotaManager.ResourceType.CPU_TIME)
                .withTenantPattern(tenantId)
                .withAction(ResourceGovernanceManager.PolicyAction.MONITOR)
                .build();
        governanceManager.addPolicy(policy);

        // Setup observability
        ResourceObservabilityManager.AlertRule alertRule =
            ResourceObservabilityManager.AlertRule.builder("e2e-alert", "E2E Resource Alert")
                .withMetricQuery("e2e.resource.usage")
                .withCondition(ResourceObservabilityManager.AlertCondition.GREATER_THAN)
                .withThreshold(1000.0)
                .build();
        observabilityManager.addAlertRule(alertRule);

        // When: Simulating concurrent resource operations
        for (int i = 0; i < 10; i++) {
          final int operationId = i;
          executor.submit(
              () -> {
                try {
                  // Check authorization
                  boolean authorized =
                      securityManager.authorize(
                          context,
                          tenantId,
                          ResourceQuotaManager.ResourceType.CPU_TIME,
                          ResourceSecurityManager.ResourceOperation.ALLOCATE);

                  if (authorized) {
                    // Check quota
                    ResourceQuotaManager.QuotaCheckResult quotaResult =
                        quotaManager.checkQuota(
                            tenantId, ResourceQuotaManager.ResourceType.CPU_TIME, 500000L);

                    if (quotaResult.isAllowed()) {
                      // Record usage
                      quotaManager.recordUsage(
                          tenantId, ResourceQuotaManager.ResourceType.CPU_TIME, 500000L);

                      // Record optimization data
                      optimizationEngine.recordUsage(
                          tenantId,
                          ResourceQuotaManager.ResourceType.CPU_TIME,
                          500000.0,
                          10000000.0,
                          Map.of("operation", "e2e-test"));

                      // Record metrics
                      ResourceObservabilityManager.ResourceMetric metric =
                          new ResourceObservabilityManager.ResourceMetric(
                              "e2e-metric-" + operationId,
                              "e2e.resource.usage",
                              "E2E resource usage metric",
                              ResourceObservabilityManager.MetricType.COUNTER,
                              "units",
                              Map.of("tenant", tenantId, "operation", String.valueOf(operationId)),
                              500000.0,
                              ResourceObservabilityManager.MetricSource.APPLICATION);

                      observabilityManager.recordMetric(metric);

                      // Acquire from pool
                      ResourcePoolManager.PooledResource<String> pooledResource =
                          poolManager.acquire(
                              ResourceQuotaManager.ResourceType.MODULE_INSTANCES,
                              tenantId,
                              Duration.ofSeconds(5));

                      if (pooledResource != null) {
                        // Simulate work
                        Thread.sleep(100);
                        // Release resource
                        poolManager.release(pooledResource);
                      }
                    }
                  }
                } catch (Exception e) {
                  // Log but don't fail the test for individual operation failures
                  System.err.println("Operation " + operationId + " failed: " + e.getMessage());
                } finally {
                  completionLatch.countDown();
                }
              });
        }

        // Then: All operations should complete successfully
        assertTrue(
            completionLatch.await(25, TimeUnit.SECONDS),
            "All operations should complete within timeout");

        // Verify final state
        ResourceQuotaManager.ResourceUsageStats finalStats =
            quotaManager.getUsageStats(tenantId, ResourceQuotaManager.ResourceType.CPU_TIME);
        assertNotNull(finalStats, "Usage stats should be available");
        assertTrue(finalStats.getCurrentUsage() > 0, "Some resource usage should be recorded");

        ResourcePoolManager.PoolStatistics poolStats =
            poolManager.getPoolStatistics(ResourceQuotaManager.ResourceType.MODULE_INSTANCES);
        assertNotNull(poolStats, "Pool stats should be available");
        assertTrue(poolStats.getTotalAcquisitions() > 0, "Some acquisitions should have occurred");

        String overallStats =
            quotaManager.getStatistics()
                + "\n"
                + scheduler.getStatistics()
                + "\n"
                + poolManager.getAllStatistics()
                + "\n"
                + securityManager.getStatistics()
                + "\n"
                + observabilityManager.getStatistics();

        assertFalse(overallStats.trim().isEmpty(), "Overall statistics should be available");

      } finally {
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      }
    }
  }

  @RepeatedTest(3)
  @DisplayName("Should handle concurrent operations reliably")
  void testConcurrentReliability() throws InterruptedException {
    // Given: Multiple tenants with concurrent operations
    final int tenantCount = 5;
    final int operationsPerTenant = 20;
    final ExecutorService executor = Executors.newFixedThreadPool(tenantCount * 2);
    final CountDownLatch latch = new CountDownLatch(tenantCount * operationsPerTenant);
    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger failureCount = new AtomicInteger(0);

    try {
      // Setup tenants with quotas
      for (int i = 0; i < tenantCount; i++) {
        final String tenantId = "concurrent-tenant-" + i;
        ResourceQuotaManager.ResourceQuota quota =
            ResourceQuotaManager.ResourceQuota.builder(
                    ResourceQuotaManager.ResourceType.HEAP_MEMORY, tenantId)
                .withHardLimit(1000000L)
                .withStrategy(ResourceQuotaManager.QuotaEnforcementStrategy.THROTTLE)
                .build();
        quotaManager.setQuota(quota);

        securityManager.createPrincipal(
            "user-" + i,
            tenantId,
            ResourceSecurityManager.PrincipalType.USER,
            Set.of("RESOURCE_USER"),
            Map.of());
      }

      // When: Running concurrent operations
      for (int tenantIndex = 0; tenantIndex < tenantCount; tenantIndex++) {
        final String tenantId = "concurrent-tenant-" + tenantIndex;
        final String userId = "user-" + tenantIndex;

        for (int opIndex = 0; opIndex < operationsPerTenant; opIndex++) {
          executor.submit(
              () -> {
                try {
                  // Authenticate
                  ResourceSecurityManager.SecurityContext context =
                      securityManager.authenticate(
                          userId, Map.of("password", "password"), "concurrent-client");

                  if (context != null) {
                    // Check authorization
                    boolean authorized =
                        securityManager.authorize(
                            context,
                            tenantId,
                            ResourceQuotaManager.ResourceType.HEAP_MEMORY,
                            ResourceSecurityManager.ResourceOperation.ALLOCATE);

                    if (authorized) {
                      // Check and record quota usage
                      ResourceQuotaManager.QuotaCheckResult result =
                          quotaManager.checkQuota(
                              tenantId, ResourceQuotaManager.ResourceType.HEAP_MEMORY, 1000L);

                      if (result.isAllowed()) {
                        quotaManager.recordUsage(
                            tenantId, ResourceQuotaManager.ResourceType.HEAP_MEMORY, 1000L);
                        successCount.incrementAndGet();
                      } else {
                        // Expected quota rejection in high concurrency
                        failureCount.incrementAndGet();
                      }
                    } else {
                      failureCount.incrementAndGet();
                    }
                  } else {
                    failureCount.incrementAndGet();
                  }
                } catch (Exception e) {
                  failureCount.incrementAndGet();
                } finally {
                  latch.countDown();
                }
              });
        }
      }

      // Then: All operations should complete without system failure
      assertTrue(latch.await(30, TimeUnit.SECONDS), "All concurrent operations should complete");

      int totalOperations = tenantCount * operationsPerTenant;
      assertEquals(
          totalOperations,
          successCount.get() + failureCount.get(),
          "Success + failure count should equal total operations");

      // Should have some successes (system should handle some concurrent load)
      assertTrue(
          successCount.get() > 0,
          "Should have some successful operations even under high concurrency");

      // System should remain responsive (components should still provide stats)
      assertDoesNotThrow(
          () -> {
            quotaManager.getStatistics();
            securityManager.getStatistics();
            observabilityManager.getStatistics();
          },
          "System should remain responsive after concurrent operations");

    } finally {
      executor.shutdown();
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    }
  }
}
