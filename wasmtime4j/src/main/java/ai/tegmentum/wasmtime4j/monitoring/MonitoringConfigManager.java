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

package ai.tegmentum.wasmtime4j.monitoring;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Configuration management system for monitoring and production deployment features.
 *
 * <p>This manager provides:
 *
 * <ul>
 *   <li>Environment-specific configuration loading
 *   <li>Feature flag support for monitoring components
 *   <li>Dynamic configuration updates
 *   <li>Configuration validation and defaults
 *   <li>Integration with system properties and environment variables
 * </ul>
 *
 * @since 1.0.0
 */
public final class MonitoringConfigManager {

  private static final Logger LOGGER = Logger.getLogger(MonitoringConfigManager.class.getName());

  /** Configuration keys. */
  public static final class ConfigKeys {
    // Monitoring enablement
    public static final String MONITORING_ENABLED = "wasmtime4j.monitoring.enabled";
    public static final String METRICS_ENABLED = "wasmtime4j.metrics.enabled";
    public static final String DIAGNOSTICS_ENABLED = "wasmtime4j.diagnostics.enabled";
    public static final String HEALTH_CHECKS_ENABLED = "wasmtime4j.health.enabled";

    // Integration flags
    public static final String JMX_ENABLED = "wasmtime4j.jmx.enabled";
    public static final String PROMETHEUS_ENABLED = "wasmtime4j.prometheus.enabled";
    public static final String MICROMETER_ENABLED = "wasmtime4j.micrometer.enabled";
    public static final String OPENTELEMETRY_ENABLED = "wasmtime4j.opentelemetry.enabled";

    // Performance configuration
    public static final String METRICS_COLLECTION_INTERVAL =
        "wasmtime4j.metrics.collection.interval";
    public static final String HEALTH_CHECK_INTERVAL = "wasmtime4j.health.check.interval";
    public static final String DIAGNOSTIC_RETENTION_PERIOD =
        "wasmtime4j.diagnostics.retention.period";

    // Thresholds
    public static final String MEMORY_THRESHOLD_WARNING =
        "wasmtime4j.health.memory.warning.threshold";
    public static final String MEMORY_THRESHOLD_CRITICAL =
        "wasmtime4j.health.memory.critical.threshold";
    public static final String THREAD_COUNT_WARNING = "wasmtime4j.health.threads.warning.threshold";
    public static final String THREAD_COUNT_CRITICAL =
        "wasmtime4j.health.threads.critical.threshold";

    // Alert configuration
    public static final String ALERT_ENABLED = "wasmtime4j.alerts.enabled";
    public static final String ALERT_THRESHOLDS_CONFIG = "wasmtime4j.alerts.thresholds.config";
    public static final String ALERT_NOTIFICATION_ENABLED =
        "wasmtime4j.alerts.notifications.enabled";

    // Service identification
    public static final String SERVICE_NAME = "wasmtime4j.service.name";
    public static final String SERVICE_VERSION = "wasmtime4j.service.version";
    public static final String SERVICE_ENVIRONMENT = "wasmtime4j.service.environment";

    // Production features
    public static final String GRACEFUL_DEGRADATION_ENABLED =
        "wasmtime4j.production.graceful.degradation.enabled";
    public static final String AUTO_RECOVERY_ENABLED =
        "wasmtime4j.production.auto.recovery.enabled";
    public static final String CIRCUIT_BREAKER_ENABLED =
        "wasmtime4j.production.circuit.breaker.enabled";

    private ConfigKeys() {}
  }

  /** Environment profiles. */
  public enum Environment {
    DEVELOPMENT("development", "dev"),
    TESTING("testing", "test"),
    STAGING("staging", "stage"),
    PRODUCTION("production", "prod");

    private final String name;
    private final String shortName;

    Environment(final String name, final String shortName) {
      this.name = name;
      this.shortName = shortName;
    }

    public String getName() {
      return name;
    }

    public String getShortName() {
      return shortName;
    }

    public static Environment fromString(final String value) {
      if (value == null) {
        return DEVELOPMENT;
      }
      final String normalized = value.toLowerCase();
      for (final Environment env : values()) {
        if (env.name.equals(normalized) || env.shortName.equals(normalized)) {
          return env;
        }
      }
      return DEVELOPMENT;
    }
  }

  /** Configuration source priority. */
  private enum ConfigSource {
    SYSTEM_PROPERTIES(1),
    ENVIRONMENT_VARIABLES(2),
    CONFIG_FILE(3),
    DEFAULTS(4);

    private final int priority;

    ConfigSource(final int priority) {
      this.priority = priority;
    }

    public int getPriority() {
      return priority;
    }
  }

  /** Configuration holder. */
  private static final class ConfigValue {
    private final String value;
    private final ConfigSource source;
    private final long timestamp;

    ConfigValue(final String value, final ConfigSource source) {
      this.value = value;
      this.source = source;
      this.timestamp = System.currentTimeMillis();
    }

    public String getValue() {
      return value;
    }

    public ConfigSource getSource() {
      return source;
    }

    public long getTimestamp() {
      return timestamp;
    }
  }

  /** Configuration storage. */
  private final ConcurrentHashMap<String, ConfigValue> configValues = new ConcurrentHashMap<>();

  private final Environment environment;
  private volatile long lastConfigLoad = 0;

  /** Singleton instance. */
  private static volatile MonitoringConfigManager instance;

  private static final Object LOCK = new Object();

  /**
   * Gets the singleton configuration manager instance.
   *
   * @return configuration manager instance
   */
  public static MonitoringConfigManager getInstance() {
    if (instance == null) {
      synchronized (LOCK) {
        if (instance == null) {
          instance = new MonitoringConfigManager();
        }
      }
    }
    return instance;
  }

  /** Private constructor for singleton. */
  private MonitoringConfigManager() {
    this.environment = detectEnvironment();
    loadConfiguration();
    LOGGER.info(
        "Monitoring configuration manager initialized for environment: " + environment.getName());
  }

  /** Detects the current environment. */
  private Environment detectEnvironment() {
    // Check system property first
    final String envProperty = System.getProperty("wasmtime4j.environment");
    if (envProperty != null) {
      return Environment.fromString(envProperty);
    }

    // Check environment variable
    final String envVariable = System.getenv("WASMTIME4J_ENVIRONMENT");
    if (envVariable != null) {
      return Environment.fromString(envVariable);
    }

    // Default to development
    return Environment.DEVELOPMENT;
  }

  /** Loads configuration from all sources. */
  private void loadConfiguration() {
    lastConfigLoad = System.currentTimeMillis();

    // Load defaults first
    loadDefaults();

    // Load from configuration file
    loadConfigFile();

    // Load from environment variables
    loadEnvironmentVariables();

    // Load from system properties (highest priority)
    loadSystemProperties();

    LOGGER.info("Configuration loaded from all sources");
  }

  /** Loads default configuration values. */
  private void loadDefaults() {
    final Map<String, String> defaults =
        Map.of(
            ConfigKeys.MONITORING_ENABLED, "true",
            ConfigKeys.METRICS_ENABLED, "true",
            ConfigKeys.DIAGNOSTICS_ENABLED, "true",
            ConfigKeys.HEALTH_CHECKS_ENABLED, "true",
            ConfigKeys.JMX_ENABLED, "true",
            ConfigKeys.PROMETHEUS_ENABLED, "true",
            ConfigKeys.MICROMETER_ENABLED, "false",
            ConfigKeys.OPENTELEMETRY_ENABLED, "false",
            ConfigKeys.METRICS_COLLECTION_INTERVAL, "30",
            ConfigKeys.HEALTH_CHECK_INTERVAL, "60",
            ConfigKeys.DIAGNOSTIC_RETENTION_PERIOD, "86400", // 24 hours
            ConfigKeys.MEMORY_THRESHOLD_WARNING, "0.80",
            ConfigKeys.MEMORY_THRESHOLD_CRITICAL, "0.95",
            ConfigKeys.THREAD_COUNT_WARNING, "200",
            ConfigKeys.THREAD_COUNT_CRITICAL, "500",
            ConfigKeys.ALERT_ENABLED, "true",
            ConfigKeys.ALERT_NOTIFICATION_ENABLED,
                environment == Environment.PRODUCTION ? "true" : "false",
            ConfigKeys.SERVICE_NAME, "wasmtime4j",
            ConfigKeys.SERVICE_VERSION, "1.0.0",
            ConfigKeys.SERVICE_ENVIRONMENT, environment.getName(),
            ConfigKeys.GRACEFUL_DEGRADATION_ENABLED,
                environment == Environment.PRODUCTION ? "true" : "false",
            ConfigKeys.AUTO_RECOVERY_ENABLED, "true",
            ConfigKeys.CIRCUIT_BREAKER_ENABLED,
                environment == Environment.PRODUCTION ? "true" : "false");

    for (final Map.Entry<String, String> entry : defaults.entrySet()) {
      configValues.put(entry.getKey(), new ConfigValue(entry.getValue(), ConfigSource.DEFAULTS));
    }
  }

  /** Loads configuration from file. */
  private void loadConfigFile() {
    final String configFileName =
        "wasmtime4j-monitoring-" + environment.getShortName() + ".properties";

    try (final InputStream stream =
        getClass().getClassLoader().getResourceAsStream(configFileName)) {
      if (stream != null) {
        final Properties props = new Properties();
        props.load(stream);

        for (final String key : props.stringPropertyNames()) {
          final String value = props.getProperty(key);
          if (value != null && !value.trim().isEmpty()) {
            configValues.put(key, new ConfigValue(value.trim(), ConfigSource.CONFIG_FILE));
          }
        }

        LOGGER.info("Loaded configuration from " + configFileName);
      }
    } catch (final IOException e) {
      LOGGER.warning("Could not load configuration file " + configFileName + ": " + e.getMessage());
    }
  }

  /** Loads configuration from environment variables. */
  private void loadEnvironmentVariables() {
    final Map<String, String> envVars = System.getenv();
    for (final Map.Entry<String, String> entry : envVars.entrySet()) {
      final String key = entry.getKey();
      if (key.startsWith("WASMTIME4J_")) {
        // Convert WASMTIME4J_MONITORING_ENABLED to wasmtime4j.monitoring.enabled
        final String configKey = key.toLowerCase().replace('_', '.');
        final String value = entry.getValue();
        if (value != null && !value.trim().isEmpty()) {
          configValues.put(
              configKey, new ConfigValue(value.trim(), ConfigSource.ENVIRONMENT_VARIABLES));
        }
      }
    }
  }

  /** Loads configuration from system properties. */
  private void loadSystemProperties() {
    final Properties sysProps = System.getProperties();
    for (final String key : sysProps.stringPropertyNames()) {
      if (key.startsWith("wasmtime4j.")) {
        final String value = sysProps.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
          configValues.put(key, new ConfigValue(value.trim(), ConfigSource.SYSTEM_PROPERTIES));
        }
      }
    }
  }

  /**
   * Gets a string configuration value.
   *
   * @param key configuration key
   * @return configuration value or null if not found
   */
  public String getString(final String key) {
    final ConfigValue configValue = configValues.get(key);
    return configValue != null ? configValue.getValue() : null;
  }

  /**
   * Gets a string configuration value with default.
   *
   * @param key configuration key
   * @param defaultValue default value if not found
   * @return configuration value or default
   */
  public String getString(final String key, final String defaultValue) {
    final String value = getString(key);
    return value != null ? value : defaultValue;
  }

  /**
   * Gets a boolean configuration value.
   *
   * @param key configuration key
   * @return boolean value or false if not found/invalid
   */
  public boolean getBoolean(final String key) {
    return getBoolean(key, false);
  }

  /**
   * Gets a boolean configuration value with default.
   *
   * @param key configuration key
   * @param defaultValue default value
   * @return boolean value or default
   */
  public boolean getBoolean(final String key, final boolean defaultValue) {
    final String value = getString(key);
    if (value == null) {
      return defaultValue;
    }
    return Boolean.parseBoolean(value.trim());
  }

  /**
   * Gets an integer configuration value.
   *
   * @param key configuration key
   * @return integer value or 0 if not found/invalid
   */
  public int getInt(final String key) {
    return getInt(key, 0);
  }

  /**
   * Gets an integer configuration value with default.
   *
   * @param key configuration key
   * @param defaultValue default value
   * @return integer value or default
   */
  public int getInt(final String key, final int defaultValue) {
    final String value = getString(key);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (final NumberFormatException e) {
      LOGGER.warning(
          "Invalid integer value for " + key + ": " + value + ", using default: " + defaultValue);
      return defaultValue;
    }
  }

  /**
   * Gets a long configuration value.
   *
   * @param key configuration key
   * @return long value or 0 if not found/invalid
   */
  public long getLong(final String key) {
    return getLong(key, 0L);
  }

  /**
   * Gets a long configuration value with default.
   *
   * @param key configuration key
   * @param defaultValue default value
   * @return long value or default
   */
  public long getLong(final String key, final long defaultValue) {
    final String value = getString(key);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (final NumberFormatException e) {
      LOGGER.warning(
          "Invalid long value for " + key + ": " + value + ", using default: " + defaultValue);
      return defaultValue;
    }
  }

  /**
   * Gets a double configuration value.
   *
   * @param key configuration key
   * @return double value or 0.0 if not found/invalid
   */
  public double getDouble(final String key) {
    return getDouble(key, 0.0);
  }

  /**
   * Gets a double configuration value with default.
   *
   * @param key configuration key
   * @param defaultValue default value
   * @return double value or default
   */
  public double getDouble(final String key, final double defaultValue) {
    final String value = getString(key);
    if (value == null) {
      return defaultValue;
    }
    try {
      return Double.parseDouble(value.trim());
    } catch (final NumberFormatException e) {
      LOGGER.warning(
          "Invalid double value for " + key + ": " + value + ", using default: " + defaultValue);
      return defaultValue;
    }
  }

  /**
   * Gets a Duration configuration value.
   *
   * @param key configuration key
   * @param defaultValue default value
   * @return Duration value or default
   */
  public Duration getDuration(final String key, final Duration defaultValue) {
    final long seconds = getLong(key, defaultValue.toSeconds());
    return Duration.ofSeconds(seconds);
  }

  /**
   * Sets a configuration value dynamically.
   *
   * @param key configuration key
   * @param value configuration value
   */
  public void setString(final String key, final String value) {
    configValues.put(key, new ConfigValue(value, ConfigSource.SYSTEM_PROPERTIES));
    LOGGER.info("Configuration updated: " + key + " = " + value);
  }

  /**
   * Gets configuration source information.
   *
   * @param key configuration key
   * @return configuration source or null if not found
   */
  public String getConfigSource(final String key) {
    final ConfigValue configValue = configValues.get(key);
    return configValue != null ? configValue.getSource().name() : null;
  }

  /**
   * Gets all configuration keys.
   *
   * @return set of configuration keys
   */
  public java.util.Set<String> getConfigKeys() {
    return java.util.Set.copyOf(configValues.keySet());
  }

  /**
   * Gets current environment.
   *
   * @return current environment
   */
  public Environment getEnvironment() {
    return environment;
  }

  /**
   * Checks if monitoring is enabled.
   *
   * @return true if monitoring is enabled
   */
  public boolean isMonitoringEnabled() {
    return getBoolean(ConfigKeys.MONITORING_ENABLED, true);
  }

  /**
   * Checks if metrics collection is enabled.
   *
   * @return true if metrics collection is enabled
   */
  public boolean isMetricsEnabled() {
    return getBoolean(ConfigKeys.METRICS_ENABLED, true);
  }

  /**
   * Checks if diagnostics are enabled.
   *
   * @return true if diagnostics are enabled
   */
  public boolean isDiagnosticsEnabled() {
    return getBoolean(ConfigKeys.DIAGNOSTICS_ENABLED, true);
  }

  /**
   * Checks if health checks are enabled.
   *
   * @return true if health checks are enabled
   */
  public boolean isHealthChecksEnabled() {
    return getBoolean(ConfigKeys.HEALTH_CHECKS_ENABLED, true);
  }

  /**
   * Gets monitoring system health configuration.
   *
   * @return health system configuration
   */
  public HealthCheckSystem.SystemHealthConfig getHealthSystemConfig() {
    return new HealthCheckSystem.SystemHealthConfig(
        getDouble(ConfigKeys.MEMORY_THRESHOLD_WARNING, 0.8),
        getDouble(ConfigKeys.MEMORY_THRESHOLD_CRITICAL, 0.95),
        getInt(ConfigKeys.THREAD_COUNT_WARNING, 200),
        getInt(ConfigKeys.THREAD_COUNT_CRITICAL, 500),
        getDuration(ConfigKeys.HEALTH_CHECK_INTERVAL, Duration.ofMinutes(1)),
        Duration.ofMinutes(5),
        getBoolean(ConfigKeys.AUTO_RECOVERY_ENABLED, true));
  }

  /**
   * Gets monitoring integration configuration.
   *
   * @return monitoring integration configuration
   */
  public MonitoringIntegration.MonitoringConfig getMonitoringIntegrationConfig() {
    return new MonitoringIntegration.MonitoringConfig(
        getBoolean(ConfigKeys.JMX_ENABLED, true),
        getBoolean(ConfigKeys.MICROMETER_ENABLED, false),
        getBoolean(ConfigKeys.OPENTELEMETRY_ENABLED, false),
        getBoolean(ConfigKeys.PROMETHEUS_ENABLED, true),
        getBoolean(ConfigKeys.HEALTH_CHECKS_ENABLED, true),
        getString(ConfigKeys.SERVICE_NAME, "wasmtime4j"),
        getString(ConfigKeys.SERVICE_VERSION, "1.0.0"),
        Map.of("environment", environment.getName()));
  }

  /**
   * Validates current configuration.
   *
   * @return validation result
   */
  public String validateConfiguration() {
    final StringBuilder issues = new StringBuilder();

    // Validate threshold values
    final double memoryWarning = getDouble(ConfigKeys.MEMORY_THRESHOLD_WARNING);
    final double memoryCritical = getDouble(ConfigKeys.MEMORY_THRESHOLD_CRITICAL);
    if (memoryWarning >= memoryCritical) {
      issues.append("Memory warning threshold must be less than critical threshold\n");
    }

    // Validate intervals
    final long metricsInterval = getLong(ConfigKeys.METRICS_COLLECTION_INTERVAL);
    if (metricsInterval < 1 || metricsInterval > 3600) {
      issues.append("Metrics collection interval must be between 1 and 3600 seconds\n");
    }

    // Validate service name
    final String serviceName = getString(ConfigKeys.SERVICE_NAME);
    if (serviceName == null || serviceName.trim().isEmpty()) {
      issues.append("Service name is required\n");
    }

    return issues.length() > 0
        ? "Configuration Issues:\n" + issues.toString()
        : "Configuration valid";
  }

  /** Reloads configuration from all sources. */
  public void reloadConfiguration() {
    LOGGER.info("Reloading configuration");
    loadConfiguration();
  }

  /**
   * Gets configuration summary.
   *
   * @return formatted configuration summary
   */
  public String getConfigurationSummary() {
    final StringBuilder sb = new StringBuilder("=== Monitoring Configuration ===\n");

    sb.append(String.format("Environment: %s\n", environment.getName()));
    sb.append(String.format("Last loaded: %s\n", new java.util.Date(lastConfigLoad)));
    sb.append("\n");

    sb.append("Feature Flags:\n");
    sb.append(String.format("  Monitoring: %s\n", isMonitoringEnabled()));
    sb.append(String.format("  Metrics: %s\n", isMetricsEnabled()));
    sb.append(String.format("  Diagnostics: %s\n", isDiagnosticsEnabled()));
    sb.append(String.format("  Health Checks: %s\n", isHealthChecksEnabled()));
    sb.append(String.format("  JMX: %s\n", getBoolean(ConfigKeys.JMX_ENABLED)));
    sb.append(String.format("  Prometheus: %s\n", getBoolean(ConfigKeys.PROMETHEUS_ENABLED)));
    sb.append("\n");

    sb.append("Service Info:\n");
    sb.append(String.format("  Name: %s\n", getString(ConfigKeys.SERVICE_NAME)));
    sb.append(String.format("  Version: %s\n", getString(ConfigKeys.SERVICE_VERSION)));
    sb.append(String.format("  Environment: %s\n", getString(ConfigKeys.SERVICE_ENVIRONMENT)));

    return sb.toString();
  }
}
