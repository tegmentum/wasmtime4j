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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeInfo;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.gc.GcRuntime;
import ai.tegmentum.wasmtime4j.monitoring.MetricsCollector;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Resource-managed WebAssembly runtime that integrates comprehensive resource management
 * with both JNI and Panama implementations.
 *
 * <p>This wrapper provides:
 *
 * <ul>
 *   <li>Automatic resource quota enforcement and monitoring
 *   <li>Performance optimization and analytics integration
 *   <li>Security and access control for resource operations
 *   <li>Observability and health monitoring
 *   <li>Multi-tenant resource isolation and governance
 * </ul>
 *
 * @since 1.0.0
 */
public final class ResourceManagedWasmRuntime implements WasmRuntime {

  private static final Logger LOGGER = Logger.getLogger(ResourceManagedWasmRuntime.class.getName());

  /** Resource management components. */
  private final ResourceQuotaManager quotaManager;
  private final ResourceScheduler scheduler;
  private final ResourcePoolManager poolManager;
  private final ResourceGovernanceManager governanceManager;
  private final ResourceOptimizationEngine optimizationEngine;
  private final ResourceSecurityManager securityManager;
  private final ResourceObservabilityManager observabilityManager;

  /** Underlying runtime implementation. */
  private final WasmRuntime delegate;

  /** Current security context. */
  private final AtomicReference<ResourceSecurityManager.SecurityContext> currentSecurityContext = new AtomicReference<>();

  /** Resource usage tracking. */
  private final AtomicLong totalEnginesCreated = new AtomicLong(0);
  private final AtomicLong totalModulesCompiled = new AtomicLong(0);
  private final AtomicLong totalInstancesCreated = new AtomicLong(0);
  private final ConcurrentHashMap<String, AtomicLong> tenantResourceUsage = new ConcurrentHashMap<>();

  /** Configuration. */
  private volatile String defaultTenantId = "default";
  private volatile boolean resourceManagementEnabled = true;
  private volatile boolean securityEnforcementEnabled = true;

  /**
   * Creates a resource-managed WebAssembly runtime.
   *
   * @param delegate the underlying runtime implementation
   */
  public ResourceManagedWasmRuntime(final WasmRuntime delegate) {
    this.delegate = delegate;

    // Initialize resource management components
    this.quotaManager = new ResourceQuotaManager();
    this.scheduler = new ResourceScheduler(ResourceScheduler.SchedulingAlgorithm.FAIR_SHARE, quotaManager);
    this.poolManager = new ResourcePoolManager();
    this.governanceManager = new ResourceGovernanceManager();
    this.optimizationEngine = new ResourceOptimizationEngine();
    this.securityManager = new ResourceSecurityManager();
    this.observabilityManager = new ResourceObservabilityManager();

    // Initialize default configurations
    initializeDefaultQuotas();
    initializeDefaultPools();
    initializeDefaultSecurity();

    // Setup observability integration
    setupObservabilityIntegration();

    LOGGER.info("Resource-managed WebAssembly runtime initialized with delegate: " + delegate.getClass().getSimpleName());
  }

  @Override
  public Engine createEngine() throws WasmException {
    return createEngine(null);
  }

  @Override
  public Engine createEngine(final EngineConfig config) throws WasmException {
    final String tenantId = getCurrentTenantId();
    final Instant startTime = Instant.now();

    // Check security authorization
    if (securityEnforcementEnabled && !isAuthorized(ResourceSecurityManager.ResourceOperation.ALLOCATE)) {
      throw new WasmException("Access denied: insufficient permissions to create engine");
    }

    // Check resource quotas
    if (resourceManagementEnabled) {
      final ResourceQuotaManager.QuotaCheckResult quotaResult = quotaManager.checkQuota(
          tenantId, ResourceQuotaManager.ResourceType.MODULE_INSTANCES, 1);

      if (!quotaResult.isAllowed()) {
        recordMetric("engine_creation_denied", 1.0, Map.of("tenant", tenantId, "reason", quotaResult.getReason()));
        throw new WasmException("Resource quota exceeded: " + quotaResult.getReason());
      }
    }

    try {
      // Create engine through delegate
      final Engine engine = delegate.createEngine(config);

      // Record resource usage
      if (resourceManagementEnabled) {
        quotaManager.recordUsage(tenantId, ResourceQuotaManager.ResourceType.MODULE_INSTANCES, 1);
        recordTenantResourceUsage(tenantId, "engines", 1);
      }

      // Update analytics
      optimizationEngine.recordUsage(tenantId, ResourceQuotaManager.ResourceType.MODULE_INSTANCES, 1.0, 1000.0, Map.of());

      // Record metrics
      final Duration creationTime = Duration.between(startTime, Instant.now());
      recordMetric("engine_creation_time", creationTime.toMillis(), Map.of("tenant", tenantId));
      recordMetric("engines_created_total", 1.0, Map.of("tenant", tenantId));

      totalEnginesCreated.incrementAndGet();

      LOGGER.fine(String.format("Created engine for tenant %s in %dms", tenantId, creationTime.toMillis()));
      return new ResourceManagedEngine(engine, tenantId, this);

    } catch (final WasmException e) {
      recordMetric("engine_creation_failed", 1.0, Map.of("tenant", tenantId, "error", e.getClass().getSimpleName()));
      throw e;
    } catch (final Exception e) {
      recordMetric("engine_creation_failed", 1.0, Map.of("tenant", tenantId, "error", e.getClass().getSimpleName()));
      throw new WasmException("Failed to create engine", e);
    }
  }

  @Override
  public Store createStore(final Engine engine) throws WasmException {
    final String tenantId = getCurrentTenantId();

    if (securityEnforcementEnabled && !isAuthorized(ResourceSecurityManager.ResourceOperation.ALLOCATE)) {
      throw new WasmException("Access denied: insufficient permissions to create store");
    }

    try {
      final Store store = delegate.createStore(engine);
      recordMetric("stores_created_total", 1.0, Map.of("tenant", tenantId));
      return store;
    } catch (final Exception e) {
      recordMetric("store_creation_failed", 1.0, Map.of("tenant", tenantId));
      throw e instanceof WasmException ? (WasmException) e : new WasmException("Failed to create store", e);
    }
  }

  @Override
  public Linker createLinker(final Engine engine) throws WasmException {
    final String tenantId = getCurrentTenantId();

    if (securityEnforcementEnabled && !isAuthorized(ResourceSecurityManager.ResourceOperation.ALLOCATE)) {
      throw new WasmException("Access denied: insufficient permissions to create linker");
    }

    try {
      final Linker linker = delegate.createLinker(engine);
      recordMetric("linkers_created_total", 1.0, Map.of("tenant", tenantId));
      return linker;
    } catch (final Exception e) {
      recordMetric("linker_creation_failed", 1.0, Map.of("tenant", tenantId));
      throw e instanceof WasmException ? (WasmException) e : new WasmException("Failed to create linker", e);
    }
  }

  @Override
  public GcRuntime createGcRuntime(final Engine engine) throws WasmException {
    final String tenantId = getCurrentTenantId();

    if (securityEnforcementEnabled && !isAuthorized(ResourceSecurityManager.ResourceOperation.ALLOCATE)) {
      throw new WasmException("Access denied: insufficient permissions to create GC runtime");
    }

    try {
      final GcRuntime gcRuntime = delegate.createGcRuntime(engine);
      recordMetric("gc_runtimes_created_total", 1.0, Map.of("tenant", tenantId));
      return gcRuntime;
    } catch (final Exception e) {
      recordMetric("gc_runtime_creation_failed", 1.0, Map.of("tenant", tenantId));
      throw e instanceof WasmException ? (WasmException) e : new WasmException("Failed to create GC runtime", e);
    }
  }

  @Override
  public Module compileModule(final Engine engine, final byte[] wasmBytes) throws WasmException {
    final String tenantId = getCurrentTenantId();
    final Instant startTime = Instant.now();

    if (securityEnforcementEnabled && !isAuthorized(ResourceSecurityManager.ResourceOperation.ALLOCATE)) {
      throw new WasmException("Access denied: insufficient permissions to compile module");
    }

    // Check compilation quotas
    if (resourceManagementEnabled) {
      final ResourceQuotaManager.QuotaCheckResult quotaResult = quotaManager.checkQuota(
          tenantId, ResourceQuotaManager.ResourceType.COMPILATION_UNITS, 1);

      if (!quotaResult.isAllowed()) {
        recordMetric("module_compilation_denied", 1.0, Map.of("tenant", tenantId, "reason", quotaResult.getReason()));
        throw new WasmException("Compilation quota exceeded: " + quotaResult.getReason());
      }
    }

    try {
      final Module module = delegate.compileModule(engine, wasmBytes);

      // Record resource usage
      if (resourceManagementEnabled) {
        quotaManager.recordUsage(tenantId, ResourceQuotaManager.ResourceType.COMPILATION_UNITS, 1);
        quotaManager.recordUsage(tenantId, ResourceQuotaManager.ResourceType.NATIVE_MEMORY, wasmBytes.length);
        recordTenantResourceUsage(tenantId, "modules", 1);
      }

      // Record metrics
      final Duration compilationTime = Duration.between(startTime, Instant.now());
      recordMetric("module_compilation_time", compilationTime.toMillis(),
          Map.of("tenant", tenantId, "module_size", wasmBytes.length));
      recordMetric("modules_compiled_total", 1.0, Map.of("tenant", tenantId));
      recordMetric("module_bytecode_size", wasmBytes.length, Map.of("tenant", tenantId));

      totalModulesCompiled.incrementAndGet();

      LOGGER.fine(String.format("Compiled module for tenant %s: %d bytes in %dms",
          tenantId, wasmBytes.length, compilationTime.toMillis()));

      return new ResourceManagedModule(module, tenantId, this);

    } catch (final WasmException e) {
      recordMetric("module_compilation_failed", 1.0, Map.of("tenant", tenantId, "error", e.getClass().getSimpleName()));
      throw e;
    } catch (final Exception e) {
      recordMetric("module_compilation_failed", 1.0, Map.of("tenant", tenantId, "error", e.getClass().getSimpleName()));
      throw new WasmException("Failed to compile module", e);
    }
  }

  @Override
  public Instance instantiate(final Module module) throws WasmException {
    return instantiate(module, null);
  }

  @Override
  public Instance instantiate(final Module module, final ImportMap imports) throws WasmException {
    final String tenantId = getCurrentTenantId();
    final Instant startTime = Instant.now();

    if (securityEnforcementEnabled && !isAuthorized(ResourceSecurityManager.ResourceOperation.ALLOCATE)) {
      throw new WasmException("Access denied: insufficient permissions to instantiate module");
    }

    // Check instantiation quotas
    if (resourceManagementEnabled) {
      final ResourceQuotaManager.QuotaCheckResult quotaResult = quotaManager.checkQuota(
          tenantId, ResourceQuotaManager.ResourceType.MODULE_INSTANCES, 1);

      if (!quotaResult.isAllowed()) {
        recordMetric("instance_creation_denied", 1.0, Map.of("tenant", tenantId, "reason", quotaResult.getReason()));
        throw new WasmException("Instance quota exceeded: " + quotaResult.getReason());
      }
    }

    try {
      final Instance instance = delegate.instantiate(module, imports);

      // Record resource usage
      if (resourceManagementEnabled) {
        quotaManager.recordUsage(tenantId, ResourceQuotaManager.ResourceType.MODULE_INSTANCES, 1);
        recordTenantResourceUsage(tenantId, "instances", 1);
      }

      // Record metrics
      final Duration instantiationTime = Duration.between(startTime, Instant.now());
      recordMetric("instance_creation_time", instantiationTime.toMillis(), Map.of("tenant", tenantId));
      recordMetric("instances_created_total", 1.0, Map.of("tenant", tenantId));

      totalInstancesCreated.incrementAndGet();

      LOGGER.fine(String.format("Instantiated module for tenant %s in %dms", tenantId, instantiationTime.toMillis()));
      return instance;

    } catch (final WasmException e) {
      recordMetric("instance_creation_failed", 1.0, Map.of("tenant", tenantId, "error", e.getClass().getSimpleName()));
      throw e;
    } catch (final Exception e) {
      recordMetric("instance_creation_failed", 1.0, Map.of("tenant", tenantId, "error", e.getClass().getSimpleName()));
      throw new WasmException("Failed to instantiate module", e);
    }
  }

  @Override
  public RuntimeInfo getRuntimeInfo() {
    final RuntimeInfo delegateInfo = delegate.getRuntimeInfo();

    // Enhance with resource management info
    return new RuntimeInfo(
        "resource-managed-" + delegateInfo.getName(),
        delegateInfo.getVersion(),
        delegateInfo.getWasmtimeVersion(),
        delegateInfo.getType(),
        delegateInfo.getJavaVersion(),
        delegateInfo.getPlatform()
    );
  }

  @Override
  public boolean isValid() {
    return delegate.isValid();
  }

  @Override
  public void close() throws Exception {
    LOGGER.info("Shutting down resource-managed WebAssembly runtime");

    // Shutdown resource management components
    try {
      observabilityManager.shutdown();
      securityManager.shutdown();
      optimizationEngine.shutdown();
      governanceManager.shutdown();
      poolManager.shutdown();
      scheduler.shutdown();
      quotaManager.shutdown();
    } catch (final Exception e) {
      LOGGER.warning("Error shutting down resource management components: " + e.getMessage());
    }

    // Close delegate
    delegate.close();
  }

  /**
   * Sets the security context for the current thread.
   *
   * @param context security context
   */
  public void setSecurityContext(final ResourceSecurityManager.SecurityContext context) {
    currentSecurityContext.set(context);
    if (context != null) {
      this.defaultTenantId = context.getPrincipal().getTenantId();
    }
  }

  /**
   * Authenticates a principal and sets the security context.
   *
   * @param principalId principal identifier
   * @param credentials authentication credentials
   * @param clientInfo client information
   * @return security context
   * @throws WasmException if authentication fails
   */
  public ResourceSecurityManager.SecurityContext authenticate(final String principalId,
                                                              final Map<String, Object> credentials,
                                                              final String clientInfo) throws WasmException {
    final ResourceSecurityManager.SecurityContext context = securityManager.authenticate(principalId, credentials, clientInfo);
    if (context == null) {
      throw new WasmException("Authentication failed for principal: " + principalId);
    }
    setSecurityContext(context);
    return context;
  }

  /**
   * Gets comprehensive resource management statistics.
   *
   * @return formatted statistics
   */
  public String getResourceStatistics() {
    final StringBuilder sb = new StringBuilder("=== Resource-Managed WebAssembly Runtime Statistics ===\n");

    sb.append(String.format("Delegate Runtime: %s\n", delegate.getClass().getSimpleName()));
    sb.append(String.format("Resource Management Enabled: %s\n", resourceManagementEnabled));
    sb.append(String.format("Security Enforcement Enabled: %s\n", securityEnforcementEnabled));
    sb.append(String.format("Default Tenant: %s\n", defaultTenantId));
    sb.append("\n");

    sb.append("Resource Usage:\n");
    sb.append(String.format("  Total Engines Created: %,d\n", totalEnginesCreated.get()));
    sb.append(String.format("  Total Modules Compiled: %,d\n", totalModulesCompiled.get()));
    sb.append(String.format("  Total Instances Created: %,d\n", totalInstancesCreated.get()));
    sb.append("\n");

    sb.append("Per-Tenant Resource Usage:\n");
    for (final Map.Entry<String, AtomicLong> entry : tenantResourceUsage.entrySet()) {
      sb.append(String.format("  %s: %,d\n", entry.getKey(), entry.getValue().get()));
    }
    sb.append("\n");

    // Add component statistics
    sb.append(quotaManager.getStatistics());
    sb.append("\n");
    sb.append(scheduler.getStatistics());
    sb.append("\n");
    sb.append(poolManager.getAllStatistics());
    sb.append("\n");
    sb.append(governanceManager.getStatistics());
    sb.append("\n");
    sb.append(optimizationEngine.getStatistics());
    sb.append("\n");
    sb.append(securityManager.getStatistics());
    sb.append("\n");
    sb.append(observabilityManager.getStatistics());

    return sb.toString();
  }

  // Resource management component getters
  public ResourceQuotaManager getQuotaManager() { return quotaManager; }
  public ResourceScheduler getScheduler() { return scheduler; }
  public ResourcePoolManager getPoolManager() { return poolManager; }
  public ResourceGovernanceManager getGovernanceManager() { return governanceManager; }
  public ResourceOptimizationEngine getOptimizationEngine() { return optimizationEngine; }
  public ResourceSecurityManager getSecurityManager() { return securityManager; }
  public ResourceObservabilityManager getObservabilityManager() { return observabilityManager; }

  // Configuration methods
  public void setResourceManagementEnabled(final boolean enabled) {
    this.resourceManagementEnabled = enabled;
    LOGGER.info("Resource management " + (enabled ? "enabled" : "disabled"));
  }

  public void setSecurityEnforcementEnabled(final boolean enabled) {
    this.securityEnforcementEnabled = enabled;
    LOGGER.info("Security enforcement " + (enabled ? "enabled" : "disabled"));
  }

  public void setDefaultTenantId(final String tenantId) {
    this.defaultTenantId = tenantId;
    LOGGER.info("Default tenant ID set to: " + tenantId);
  }

  // Private helper methods
  private String getCurrentTenantId() {
    final ResourceSecurityManager.SecurityContext context = currentSecurityContext.get();
    return context != null ? context.getPrincipal().getTenantId() : defaultTenantId;
  }

  private boolean isAuthorized(final ResourceSecurityManager.ResourceOperation operation) {
    if (!securityEnforcementEnabled) {
      return true;
    }

    final ResourceSecurityManager.SecurityContext context = currentSecurityContext.get();
    if (context == null) {
      return false;
    }

    return securityManager.authorize(context, getCurrentTenantId(), null, operation);
  }

  private void recordTenantResourceUsage(final String tenantId, final String resourceType, final long amount) {
    final String key = tenantId + ":" + resourceType;
    tenantResourceUsage.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(amount);
  }

  private void recordMetric(final String name, final double value, final Map<String, String> labels) {
    final ResourceObservabilityManager.ResourceMetric metric = new ResourceObservabilityManager.ResourceMetric(
        name + "-" + System.currentTimeMillis(),
        name,
        "Resource management metric",
        ResourceObservabilityManager.MetricType.COUNTER,
        "",
        labels,
        value,
        ResourceObservabilityManager.MetricSource.APPLICATION
    );
    observabilityManager.recordMetric(metric);
  }

  private void initializeDefaultQuotas() {
    // Set reasonable default quotas for different resource types
    for (final ResourceQuotaManager.ResourceType resourceType : ResourceQuotaManager.ResourceType.values()) {
      final ResourceQuotaManager.ResourceQuota quota = ResourceQuotaManager.ResourceQuota
          .builder(resourceType, defaultTenantId)
          .withSoftLimit(getDefaultSoftLimit(resourceType))
          .withHardLimit(getDefaultHardLimit(resourceType))
          .withStrategy(ResourceQuotaManager.QuotaEnforcementStrategy.THROTTLE)
          .withPriority(100)
          .build();

      quotaManager.setQuota(quota);
    }
  }

  private long getDefaultSoftLimit(final ResourceQuotaManager.ResourceType resourceType) {
    switch (resourceType) {
      case CPU_TIME: return 1_000_000_000L; // 1 second in nanoseconds
      case HEAP_MEMORY: return 100 * 1024 * 1024L; // 100 MB
      case NATIVE_MEMORY: return 50 * 1024 * 1024L; // 50 MB
      case MODULE_INSTANCES: return 50L;
      case FUNCTION_CALLS: return 10000L;
      case COMPILATION_UNITS: return 100L;
      default: return 1000L;
    }
  }

  private long getDefaultHardLimit(final ResourceQuotaManager.ResourceType resourceType) {
    return getDefaultSoftLimit(resourceType) * 2; // Hard limit is 2x soft limit
  }

  private void initializeDefaultPools() {
    // Create default resource pools for commonly used resources
    // Engine pool
    final ResourcePoolManager.PoolConfiguration enginePoolConfig = ResourcePoolManager.PoolConfiguration
        .builder(ResourceQuotaManager.ResourceType.MODULE_INSTANCES)
        .withMinSize(2)
        .withMaxSize(20)
        .withInitialSize(5)
        .withMaxIdleTime(Duration.ofMinutes(30))
        .build();

    poolManager.createPool(enginePoolConfig,
        () -> {
          try {
            return delegate.createEngine();
          } catch (final WasmException e) {
            throw new RuntimeException(e);
          }
        },
        engine -> engine != null && engine.isValid()
    );
  }

  private void initializeDefaultSecurity() {
    // Create default security principals and permissions
    securityManager.createPrincipal("default-user", defaultTenantId,
        ResourceSecurityManager.PrincipalType.USER,
        java.util.Set.of("RESOURCE_USER"),
        java.util.Map.of("description", "Default user account"));

    securityManager.createPrincipal("system-admin", "system",
        ResourceSecurityManager.PrincipalType.SYSTEM,
        java.util.Set.of("SYSTEM_ADMIN"),
        java.util.Map.of("description", "System administrator"));
  }

  private void setupObservabilityIntegration() {
    // Add default health checks
    observabilityManager.addHealthCheck(new ResourceObservabilityManager.HealthCheck(
        "delegate-runtime-health", "Delegate Runtime Health",
        "Checks if the underlying runtime is healthy",
        () -> {
          if (delegate.isValid()) {
            return ResourceObservabilityManager.HealthCheckResult.healthy("Delegate runtime is valid");
          } else {
            return ResourceObservabilityManager.HealthCheckResult.unhealthy("Delegate runtime is invalid");
          }
        },
        Duration.ofMinutes(1), Duration.ofSeconds(30), true));

    // Add resource management health checks
    observabilityManager.addHealthCheck(new ResourceObservabilityManager.HealthCheck(
        "quota-manager-health", "Quota Manager Health",
        "Checks if quota management is functioning",
        () -> ResourceObservabilityManager.HealthCheckResult.healthy("Quota manager operational"),
        Duration.ofMinutes(2), Duration.ofSeconds(30), true));
  }

  /** Resource-managed engine wrapper. */
  private static final class ResourceManagedEngine implements Engine {
    private final Engine delegate;
    private final String tenantId;
    private final ResourceManagedWasmRuntime runtime;

    ResourceManagedEngine(final Engine delegate, final String tenantId, final ResourceManagedWasmRuntime runtime) {
      this.delegate = delegate;
      this.tenantId = tenantId;
      this.runtime = runtime;
    }

    @Override
    public boolean isValid() {
      return delegate.isValid();
    }

    @Override
    public void close() throws Exception {
      // Record resource deallocation
      if (runtime.resourceManagementEnabled) {
        runtime.quotaManager.recordDeallocation(tenantId, ResourceQuotaManager.ResourceType.MODULE_INSTANCES, 1);
      }
      delegate.close();
    }
  }

  /** Resource-managed module wrapper. */
  private static final class ResourceManagedModule implements Module {
    private final Module delegate;
    private final String tenantId;
    private final ResourceManagedWasmRuntime runtime;

    ResourceManagedModule(final Module delegate, final String tenantId, final ResourceManagedWasmRuntime runtime) {
      this.delegate = delegate;
      this.tenantId = tenantId;
      this.runtime = runtime;
    }

    @Override
    public boolean isValid() {
      return delegate.isValid();
    }

    @Override
    public void close() throws Exception {
      // Record resource deallocation
      if (runtime.resourceManagementEnabled) {
        runtime.quotaManager.recordDeallocation(tenantId, ResourceQuotaManager.ResourceType.COMPILATION_UNITS, 1);
      }
      delegate.close();
    }
  }
}