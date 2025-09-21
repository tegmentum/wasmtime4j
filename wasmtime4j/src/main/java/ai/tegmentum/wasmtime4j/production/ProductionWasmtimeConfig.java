/*
 * Copyright 2024 Tegmentum AI Inc.
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

package ai.tegmentum.wasmtime4j.production;

import ai.tegmentum.wasmtime4j.OptimizationLevel;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Production-grade configuration management for WebAssembly runtime in different deployment
 * environments.
 *
 * <p>Features:
 * - Environment-specific configuration profiles
 * - Runtime configuration validation
 * - Configuration hot-reloading support
 * - Security-focused configuration management
 * - Performance tuning for different scenarios
 * - Integration with external configuration systems
 */
public final class ProductionWasmtimeConfig {

  private static final Logger LOGGER = Logger.getLogger(ProductionWasmtimeConfig.class.getName());

  /** Deployment environments. */
  public enum Environment {
    DEVELOPMENT("development"),
    TESTING("testing"),
    STAGING("staging"),
    PRODUCTION("production");

    private final String name;

    Environment(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public static Environment fromString(final String name) {
      for (final Environment env : values()) {
        if (env.name.equalsIgnoreCase(name)) {
          return env;
        }
      }
      throw new IllegalArgumentException("Unknown environment: " + name);
    }
  }

  /** Security levels for different deployment environments. */
  public enum SecurityLevel {
    PERMISSIVE("permissive"),
    BALANCED("balanced"),
    STRICT("strict"),
    MAXIMUM("maximum");

    private final String name;

    SecurityLevel(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public static SecurityLevel fromString(final String name) {
      for (final SecurityLevel level : values()) {
        if (level.name.equalsIgnoreCase(name)) {
          return level;
        }
      }
      throw new IllegalArgumentException("Unknown security level: " + name);
    }
  }

  /** Engine configuration for different environments. */
  public static final class EngineConfig {
    private OptimizationLevel optimizationLevel;
    private boolean parallelCompilation;
    private int craneliftOptLevel;
    private boolean enableProfiling;
    private Duration compilationTimeout;
    private long maxMemorySize;
    private boolean enableDebugInfo;
    private boolean enableConsumeFuel;
    private long fuelConsumption;

    private EngineConfig() {
      // Default values
      this.optimizationLevel = OptimizationLevel.SPEED_AND_SIZE;
      this.parallelCompilation = true;
      this.craneliftOptLevel = 2;
      this.enableProfiling = false;
      this.compilationTimeout = Duration.ofMinutes(5);
      this.maxMemorySize = 1024 * 1024 * 1024; // 1GB
      this.enableDebugInfo = false;
      this.enableConsumeFuel = false;
      this.fuelConsumption = 1000000;
    }

    public OptimizationLevel getOptimizationLevel() {
      return optimizationLevel;
    }

    public void setOptimizationLevel(final OptimizationLevel optimizationLevel) {
      this.optimizationLevel = optimizationLevel;
    }

    public boolean isParallelCompilation() {
      return parallelCompilation;
    }

    public void setParallelCompilation(final boolean parallelCompilation) {
      this.parallelCompilation = parallelCompilation;
    }

    public int getCraneliftOptLevel() {
      return craneliftOptLevel;
    }

    public void setCraneliftOptLevel(final int craneliftOptLevel) {
      this.craneliftOptLevel = craneliftOptLevel;
    }

    public boolean isEnableProfiling() {
      return enableProfiling;
    }

    public void setEnableProfiling(final boolean enableProfiling) {
      this.enableProfiling = enableProfiling;
    }

    public Duration getCompilationTimeout() {
      return compilationTimeout;
    }

    public void setCompilationTimeout(final Duration compilationTimeout) {
      this.compilationTimeout = compilationTimeout;
    }

    public long getMaxMemorySize() {
      return maxMemorySize;
    }

    public void setMaxMemorySize(final long maxMemorySize) {
      this.maxMemorySize = maxMemorySize;
    }

    public boolean isEnableDebugInfo() {
      return enableDebugInfo;
    }

    public void setEnableDebugInfo(final boolean enableDebugInfo) {
      this.enableDebugInfo = enableDebugInfo;
    }

    public boolean isEnableConsumeFuel() {
      return enableConsumeFuel;
    }

    public void setEnableConsumeFuel(final boolean enableConsumeFuel) {
      this.enableConsumeFuel = enableConsumeFuel;
    }

    public long getFuelConsumption() {
      return fuelConsumption;
    }

    public void setFuelConsumption(final long fuelConsumption) {
      this.fuelConsumption = fuelConsumption;
    }
  }

  /** Pool configuration for different environments. */
  public static final class PoolConfig {
    private int maxEngines;
    private int minEngines;
    private Duration maxIdleTime;
    private Duration acquireTimeout;
    private boolean enableHealthChecks;
    private Duration healthCheckInterval;

    private PoolConfig() {
      // Default values
      this.maxEngines = 10;
      this.minEngines = 2;
      this.maxIdleTime = Duration.ofMinutes(10);
      this.acquireTimeout = Duration.ofSeconds(30);
      this.enableHealthChecks = true;
      this.healthCheckInterval = Duration.ofMinutes(5);
    }

    public int getMaxEngines() {
      return maxEngines;
    }

    public void setMaxEngines(final int maxEngines) {
      this.maxEngines = maxEngines;
    }

    public int getMinEngines() {
      return minEngines;
    }

    public void setMinEngines(final int minEngines) {
      this.minEngines = minEngines;
    }

    public Duration getMaxIdleTime() {
      return maxIdleTime;
    }

    public void setMaxIdleTime(final Duration maxIdleTime) {
      this.maxIdleTime = maxIdleTime;
    }

    public Duration getAcquireTimeout() {
      return acquireTimeout;
    }

    public void setAcquireTimeout(final Duration acquireTimeout) {
      this.acquireTimeout = acquireTimeout;
    }

    public boolean isEnableHealthChecks() {
      return enableHealthChecks;
    }

    public void setEnableHealthChecks(final boolean enableHealthChecks) {
      this.enableHealthChecks = enableHealthChecks;
    }

    public Duration getHealthCheckInterval() {
      return healthCheckInterval;
    }

    public void setHealthCheckInterval(final Duration healthCheckInterval) {
      this.healthCheckInterval = healthCheckInterval;
    }
  }

  /** Security configuration for different environments. */
  public static final class SecurityConfig {
    private boolean enableSandboxing;
    private SecurityLevel securityLevel;
    private List<String> allowedModules;
    private Duration maxExecutionTime;
    private boolean enableResourceLimits;
    private long maxMemoryLimit;
    private int maxFunctionCalls;

    private SecurityConfig() {
      // Default values
      this.enableSandboxing = true;
      this.securityLevel = SecurityLevel.STRICT;
      this.allowedModules = new ArrayList<>();
      this.maxExecutionTime = Duration.ofSeconds(30);
      this.enableResourceLimits = true;
      this.maxMemoryLimit = 512 * 1024 * 1024; // 512MB
      this.maxFunctionCalls = 10000;
    }

    public boolean isEnableSandboxing() {
      return enableSandboxing;
    }

    public void setEnableSandboxing(final boolean enableSandboxing) {
      this.enableSandboxing = enableSandboxing;
    }

    public SecurityLevel getSecurityLevel() {
      return securityLevel;
    }

    public void setSecurityLevel(final SecurityLevel securityLevel) {
      this.securityLevel = securityLevel;
    }

    public List<String> getAllowedModules() {
      return allowedModules;
    }

    public void setAllowedModules(final List<String> allowedModules) {
      this.allowedModules = new ArrayList<>(allowedModules);
    }

    public Duration getMaxExecutionTime() {
      return maxExecutionTime;
    }

    public void setMaxExecutionTime(final Duration maxExecutionTime) {
      this.maxExecutionTime = maxExecutionTime;
    }

    public boolean isEnableResourceLimits() {
      return enableResourceLimits;
    }

    public void setEnableResourceLimits(final boolean enableResourceLimits) {
      this.enableResourceLimits = enableResourceLimits;
    }

    public long getMaxMemoryLimit() {
      return maxMemoryLimit;
    }

    public void setMaxMemoryLimit(final long maxMemoryLimit) {
      this.maxMemoryLimit = maxMemoryLimit;
    }

    public int getMaxFunctionCalls() {
      return maxFunctionCalls;
    }

    public void setMaxFunctionCalls(final int maxFunctionCalls) {
      this.maxFunctionCalls = maxFunctionCalls;
    }
  }

  /** Monitoring configuration for different environments. */
  public static final class MonitoringConfig {
    private boolean enableMetrics;
    private boolean enableTracing;
    private boolean enableHealthChecks;
    private Duration healthCheckInterval;
    private List<String> monitoredOperations;
    private boolean enableAlerting;
    private double alertThresholdErrorRate;
    private Duration alertThresholdResponseTime;

    private MonitoringConfig() {
      // Default values
      this.enableMetrics = true;
      this.enableTracing = false;
      this.enableHealthChecks = true;
      this.healthCheckInterval = Duration.ofMinutes(1);
      this.monitoredOperations = List.of("compile", "instantiate", "call");
      this.enableAlerting = true;
      this.alertThresholdErrorRate = 5.0; // 5%
      this.alertThresholdResponseTime = Duration.ofSeconds(5);
    }

    public boolean isEnableMetrics() {
      return enableMetrics;
    }

    public void setEnableMetrics(final boolean enableMetrics) {
      this.enableMetrics = enableMetrics;
    }

    public boolean isEnableTracing() {
      return enableTracing;
    }

    public void setEnableTracing(final boolean enableTracing) {
      this.enableTracing = enableTracing;
    }

    public boolean isEnableHealthChecks() {
      return enableHealthChecks;
    }

    public void setEnableHealthChecks(final boolean enableHealthChecks) {
      this.enableHealthChecks = enableHealthChecks;
    }

    public Duration getHealthCheckInterval() {
      return healthCheckInterval;
    }

    public void setHealthCheckInterval(final Duration healthCheckInterval) {
      this.healthCheckInterval = healthCheckInterval;
    }

    public List<String> getMonitoredOperations() {
      return monitoredOperations;
    }

    public void setMonitoredOperations(final List<String> monitoredOperations) {
      this.monitoredOperations = new ArrayList<>(monitoredOperations);
    }

    public boolean isEnableAlerting() {
      return enableAlerting;
    }

    public void setEnableAlerting(final boolean enableAlerting) {
      this.enableAlerting = enableAlerting;
    }

    public double getAlertThresholdErrorRate() {
      return alertThresholdErrorRate;
    }

    public void setAlertThresholdErrorRate(final double alertThresholdErrorRate) {
      this.alertThresholdErrorRate = alertThresholdErrorRate;
    }

    public Duration getAlertThresholdResponseTime() {
      return alertThresholdResponseTime;
    }

    public void setAlertThresholdResponseTime(final Duration alertThresholdResponseTime) {
      this.alertThresholdResponseTime = alertThresholdResponseTime;
    }
  }

  // Configuration sections
  private final Environment environment;
  private final EngineConfig engine;
  private final PoolConfig pool;
  private final SecurityConfig security;
  private final MonitoringConfig monitoring;

  /**
   * Creates a new production configuration for the specified environment.
   *
   * @param environment the deployment environment
   */
  public ProductionWasmtimeConfig(final Environment environment) {
    this.environment = environment;
    this.engine = new EngineConfig();
    this.pool = new PoolConfig();
    this.security = new SecurityConfig();
    this.monitoring = new MonitoringConfig();

    // Apply environment-specific defaults
    applyEnvironmentDefaults();
  }

  /**
   * Creates a production configuration from system properties.
   *
   * @return a new production configuration
   */
  public static ProductionWasmtimeConfig fromSystemProperties() {
    final String envName = System.getProperty("wasmtime.environment", "production");
    final Environment environment = Environment.fromString(envName);
    final ProductionWasmtimeConfig config = new ProductionWasmtimeConfig(environment);

    // Load configuration from system properties
    config.loadFromSystemProperties();

    return config;
  }

  /**
   * Creates a production configuration from a properties file.
   *
   * @param resourcePath the path to the properties file
   * @return a new production configuration
   * @throws IOException if the properties file cannot be loaded
   */
  public static ProductionWasmtimeConfig fromPropertiesFile(final String resourcePath) throws IOException {
    try (final InputStream input = ProductionWasmtimeConfig.class.getClassLoader()
        .getResourceAsStream(resourcePath)) {
      if (input == null) {
        throw new IOException("Configuration file not found: " + resourcePath);
      }

      final Properties properties = new Properties();
      properties.load(input);

      final String envName = properties.getProperty("wasmtime.environment", "production");
      final Environment environment = Environment.fromString(envName);
      final ProductionWasmtimeConfig config = new ProductionWasmtimeConfig(environment);

      // Load configuration from properties
      config.loadFromProperties(properties);

      return config;
    }
  }

  /**
   * Creates a development-optimized configuration.
   *
   * @return a development configuration
   */
  public static ProductionWasmtimeConfig forDevelopment() {
    final ProductionWasmtimeConfig config = new ProductionWasmtimeConfig(Environment.DEVELOPMENT);

    // Development-specific overrides
    config.engine.setEnableProfiling(true);
    config.engine.setEnableDebugInfo(true);
    config.security.setSecurityLevel(SecurityLevel.PERMISSIVE);
    config.monitoring.setEnableTracing(true);

    return config;
  }

  /**
   * Creates a testing-optimized configuration.
   *
   * @return a testing configuration
   */
  public static ProductionWasmtimeConfig forTesting() {
    final ProductionWasmtimeConfig config = new ProductionWasmtimeConfig(Environment.TESTING);

    // Testing-specific overrides
    config.pool.setMaxEngines(5);
    config.pool.setMinEngines(1);
    config.security.setSecurityLevel(SecurityLevel.BALANCED);
    config.monitoring.setEnableMetrics(true);

    return config;
  }

  /**
   * Creates a production-optimized configuration.
   *
   * @return a production configuration
   */
  public static ProductionWasmtimeConfig forProduction() {
    final ProductionWasmtimeConfig config = new ProductionWasmtimeConfig(Environment.PRODUCTION);

    // Production-specific overrides
    config.engine.setOptimizationLevel(OptimizationLevel.SPEED_AND_SIZE);
    config.engine.setCraneliftOptLevel(2);
    config.pool.setMaxEngines(20);
    config.pool.setMinEngines(5);
    config.security.setSecurityLevel(SecurityLevel.MAXIMUM);
    config.monitoring.setEnableAlerting(true);

    return config;
  }

  public Environment getEnvironment() {
    return environment;
  }

  public EngineConfig getEngine() {
    return engine;
  }

  public PoolConfig getPool() {
    return pool;
  }

  public SecurityConfig getSecurity() {
    return security;
  }

  public MonitoringConfig getMonitoring() {
    return monitoring;
  }

  /**
   * Validates the current configuration for consistency and security.
   *
   * @throws IllegalStateException if the configuration is invalid
   */
  public void validate() {
    // Validate engine configuration
    if (engine.getMaxMemorySize() <= 0) {
      throw new IllegalStateException("Engine max memory size must be positive");
    }

    if (engine.getCraneliftOptLevel() < 0 || engine.getCraneliftOptLevel() > 2) {
      throw new IllegalStateException("Cranelift optimization level must be 0, 1, or 2");
    }

    // Validate pool configuration
    if (pool.getMinEngines() > pool.getMaxEngines()) {
      throw new IllegalStateException("Pool min engines cannot be greater than max engines");
    }

    if (pool.getMaxEngines() <= 0) {
      throw new IllegalStateException("Pool max engines must be positive");
    }

    // Validate security configuration
    if (security.getMaxMemoryLimit() <= 0) {
      throw new IllegalStateException("Security max memory limit must be positive");
    }

    if (security.getMaxFunctionCalls() <= 0) {
      throw new IllegalStateException("Security max function calls must be positive");
    }

    // Production-specific validations
    if (environment == Environment.PRODUCTION) {
      if (security.getSecurityLevel() == SecurityLevel.PERMISSIVE) {
        throw new IllegalStateException("Production environment cannot use permissive security level");
      }

      if (!security.isEnableSandboxing()) {
        throw new IllegalStateException("Production environment must enable sandboxing");
      }

      if (!monitoring.isEnableMetrics()) {
        throw new IllegalStateException("Production environment must enable metrics");
      }
    }

    LOGGER.info("Configuration validation passed for environment: " + environment);
  }

  /**
   * Gets the configuration as a properties map for external integration.
   *
   * @return configuration properties map
   */
  public Map<String, String> toPropertiesMap() {
    final Map<String, String> props = new HashMap<>();

    props.put("wasmtime.environment", environment.getName());

    // Engine properties
    props.put("wasmtime.engine.optimizationLevel", engine.getOptimizationLevel().toString());
    props.put("wasmtime.engine.parallelCompilation", String.valueOf(engine.isParallelCompilation()));
    props.put("wasmtime.engine.craneliftOptLevel", String.valueOf(engine.getCraneliftOptLevel()));
    props.put("wasmtime.engine.enableProfiling", String.valueOf(engine.isEnableProfiling()));
    props.put("wasmtime.engine.compilationTimeout", String.valueOf(engine.getCompilationTimeout().toMillis()));
    props.put("wasmtime.engine.maxMemorySize", String.valueOf(engine.getMaxMemorySize()));

    // Pool properties
    props.put("wasmtime.pool.maxEngines", String.valueOf(pool.getMaxEngines()));
    props.put("wasmtime.pool.minEngines", String.valueOf(pool.getMinEngines()));
    props.put("wasmtime.pool.maxIdleTime", String.valueOf(pool.getMaxIdleTime().toMillis()));
    props.put("wasmtime.pool.acquireTimeout", String.valueOf(pool.getAcquireTimeout().toMillis()));

    // Security properties
    props.put("wasmtime.security.enableSandboxing", String.valueOf(security.isEnableSandboxing()));
    props.put("wasmtime.security.securityLevel", security.getSecurityLevel().getName());
    props.put("wasmtime.security.maxExecutionTime", String.valueOf(security.getMaxExecutionTime().toMillis()));
    props.put("wasmtime.security.maxMemoryLimit", String.valueOf(security.getMaxMemoryLimit()));

    // Monitoring properties
    props.put("wasmtime.monitoring.enableMetrics", String.valueOf(monitoring.isEnableMetrics()));
    props.put("wasmtime.monitoring.enableTracing", String.valueOf(monitoring.isEnableTracing()));
    props.put("wasmtime.monitoring.enableHealthChecks", String.valueOf(monitoring.isEnableHealthChecks()));
    props.put("wasmtime.monitoring.enableAlerting", String.valueOf(monitoring.isEnableAlerting()));

    return props;
  }

  /** Applies environment-specific default configurations. */
  private void applyEnvironmentDefaults() {
    switch (environment) {
      case DEVELOPMENT:
        engine.setEnableProfiling(true);
        engine.setEnableDebugInfo(true);
        security.setSecurityLevel(SecurityLevel.BALANCED);
        monitoring.setEnableTracing(true);
        break;

      case TESTING:
        pool.setMaxEngines(5);
        pool.setMinEngines(1);
        security.setSecurityLevel(SecurityLevel.BALANCED);
        break;

      case STAGING:
        security.setSecurityLevel(SecurityLevel.STRICT);
        monitoring.setEnableAlerting(true);
        break;

      case PRODUCTION:
        engine.setOptimizationLevel(OptimizationLevel.SPEED_AND_SIZE);
        engine.setCraneliftOptLevel(2);
        pool.setMaxEngines(20);
        pool.setMinEngines(5);
        security.setSecurityLevel(SecurityLevel.MAXIMUM);
        monitoring.setEnableAlerting(true);
        break;
    }
  }

  /** Loads configuration from system properties. */
  private void loadFromSystemProperties() {
    loadFromProperties(System.getProperties());
  }

  /** Loads configuration from properties. */
  private void loadFromProperties(final Properties properties) {
    // Engine properties
    loadOptionalProperty(properties, "wasmtime.engine.optimizationLevel",
        value -> engine.setOptimizationLevel(OptimizationLevel.valueOf(value.toUpperCase())));

    loadOptionalProperty(properties, "wasmtime.engine.parallelCompilation",
        value -> engine.setParallelCompilation(Boolean.parseBoolean(value)));

    loadOptionalProperty(properties, "wasmtime.engine.craneliftOptLevel",
        value -> engine.setCraneliftOptLevel(Integer.parseInt(value)));

    loadOptionalProperty(properties, "wasmtime.engine.enableProfiling",
        value -> engine.setEnableProfiling(Boolean.parseBoolean(value)));

    loadOptionalProperty(properties, "wasmtime.engine.maxMemorySize",
        value -> engine.setMaxMemorySize(Long.parseLong(value)));

    // Pool properties
    loadOptionalProperty(properties, "wasmtime.pool.maxEngines",
        value -> pool.setMaxEngines(Integer.parseInt(value)));

    loadOptionalProperty(properties, "wasmtime.pool.minEngines",
        value -> pool.setMinEngines(Integer.parseInt(value)));

    // Security properties
    loadOptionalProperty(properties, "wasmtime.security.enableSandboxing",
        value -> security.setEnableSandboxing(Boolean.parseBoolean(value)));

    loadOptionalProperty(properties, "wasmtime.security.securityLevel",
        value -> security.setSecurityLevel(SecurityLevel.fromString(value)));

    loadOptionalProperty(properties, "wasmtime.security.maxMemoryLimit",
        value -> security.setMaxMemoryLimit(Long.parseLong(value)));

    // Monitoring properties
    loadOptionalProperty(properties, "wasmtime.monitoring.enableMetrics",
        value -> monitoring.setEnableMetrics(Boolean.parseBoolean(value)));

    loadOptionalProperty(properties, "wasmtime.monitoring.enableTracing",
        value -> monitoring.setEnableTracing(Boolean.parseBoolean(value)));

    loadOptionalProperty(properties, "wasmtime.monitoring.enableAlerting",
        value -> monitoring.setEnableAlerting(Boolean.parseBoolean(value)));
  }

  /** Helper method to load optional properties with error handling. */
  private void loadOptionalProperty(final Properties properties, final String key,
      final java.util.function.Consumer<String> setter) {
    final String value = properties.getProperty(key);
    if (value != null && !value.trim().isEmpty()) {
      try {
        setter.accept(value.trim());
      } catch (final Exception e) {
        LOGGER.warning(String.format("Failed to set property %s=%s: %s", key, value, e.getMessage()));
      }
    }
  }

  @Override
  public String toString() {
    return String.format("ProductionWasmtimeConfig{environment=%s, engine=%s, pool=%s, security=%s, monitoring=%s}",
        environment, engine.getOptimizationLevel(), pool.getMaxEngines(),
        security.getSecurityLevel(), monitoring.isEnableMetrics());
  }
}