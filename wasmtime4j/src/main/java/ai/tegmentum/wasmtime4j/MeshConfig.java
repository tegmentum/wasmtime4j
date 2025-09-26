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

package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for component mesh networking.
 *
 * <p>This class provides comprehensive configuration options for all aspects of
 * the component mesh, including service discovery, load balancing, security,
 * streaming, CDN, analytics, and federation settings.
 *
 * @since 1.0.0
 */
public final class MeshConfig {

    private final ServiceDiscoveryConfig serviceDiscovery;
    private final LoadBalancingConfig loadBalancing;
    private final SecurityConfig security;
    private final StreamingConfig streaming;
    private final CdnConfig cdn;
    private final AnalyticsConfig analytics;
    private final FederationConfig federation;
    private final MonitoringConfig monitoring;

    private MeshConfig(final Builder builder) {
        this.serviceDiscovery = builder.serviceDiscovery != null ? builder.serviceDiscovery : ServiceDiscoveryConfig.defaultConfig();
        this.loadBalancing = builder.loadBalancing != null ? builder.loadBalancing : LoadBalancingConfig.defaultConfig();
        this.security = builder.security != null ? builder.security : SecurityConfig.defaultConfig();
        this.streaming = builder.streaming != null ? builder.streaming : StreamingConfig.defaultConfig();
        this.cdn = builder.cdn != null ? builder.cdn : CdnConfig.defaultConfig();
        this.analytics = builder.analytics != null ? builder.analytics : AnalyticsConfig.defaultConfig();
        this.federation = builder.federation != null ? builder.federation : FederationConfig.defaultConfig();
        this.monitoring = builder.monitoring != null ? builder.monitoring : MonitoringConfig.defaultConfig();
    }

    /**
     * Create a default mesh configuration.
     *
     * @return default configuration
     */
    public static MeshConfig defaultConfig() {
        return builder().build();
    }

    /**
     * Create a new builder for mesh configuration.
     *
     * @return configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public ServiceDiscoveryConfig getServiceDiscovery() { return serviceDiscovery; }
    public LoadBalancingConfig getLoadBalancing() { return loadBalancing; }
    public SecurityConfig getSecurity() { return security; }
    public StreamingConfig getStreaming() { return streaming; }
    public CdnConfig getCdn() { return cdn; }
    public AnalyticsConfig getAnalytics() { return analytics; }
    public FederationConfig getFederation() { return federation; }
    public MonitoringConfig getMonitoring() { return monitoring; }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final MeshConfig that = (MeshConfig) obj;
        return Objects.equals(serviceDiscovery, that.serviceDiscovery) &&
               Objects.equals(loadBalancing, that.loadBalancing) &&
               Objects.equals(security, that.security) &&
               Objects.equals(streaming, that.streaming) &&
               Objects.equals(cdn, that.cdn) &&
               Objects.equals(analytics, that.analytics) &&
               Objects.equals(federation, that.federation) &&
               Objects.equals(monitoring, that.monitoring);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceDiscovery, loadBalancing, security, streaming, cdn, analytics, federation, monitoring);
    }

    @Override
    public String toString() {
        return "MeshConfig{" +
               "serviceDiscovery=" + serviceDiscovery +
               ", loadBalancing=" + loadBalancing +
               ", security=" + security +
               ", streaming=" + streaming +
               ", cdn=" + cdn +
               ", analytics=" + analytics +
               ", federation=" + federation +
               ", monitoring=" + monitoring +
               '}';
    }

    /**
     * Builder for mesh configuration.
     */
    public static final class Builder {
        private ServiceDiscoveryConfig serviceDiscovery;
        private LoadBalancingConfig loadBalancing;
        private SecurityConfig security;
        private StreamingConfig streaming;
        private CdnConfig cdn;
        private AnalyticsConfig analytics;
        private FederationConfig federation;
        private MonitoringConfig monitoring;

        private Builder() {}

        public Builder serviceDiscovery(final ServiceDiscoveryConfig serviceDiscovery) {
            this.serviceDiscovery = serviceDiscovery;
            return this;
        }

        public Builder loadBalancing(final LoadBalancingConfig loadBalancing) {
            this.loadBalancing = loadBalancing;
            return this;
        }

        public Builder security(final SecurityConfig security) {
            this.security = security;
            return this;
        }

        public Builder streaming(final StreamingConfig streaming) {
            this.streaming = streaming;
            return this;
        }

        public Builder cdn(final CdnConfig cdn) {
            this.cdn = cdn;
            return this;
        }

        public Builder analytics(final AnalyticsConfig analytics) {
            this.analytics = analytics;
            return this;
        }

        public Builder federation(final FederationConfig federation) {
            this.federation = federation;
            return this;
        }

        public Builder monitoring(final MonitoringConfig monitoring) {
            this.monitoring = monitoring;
            return this;
        }

        public MeshConfig build() {
            return new MeshConfig(this);
        }
    }

    // Configuration sub-classes

    /**
     * Service discovery configuration.
     */
    public static final class ServiceDiscoveryConfig {
        private final List<String> discoveryProtocols;
        private final Duration healthCheckInterval;
        private final Duration serviceTimeout;
        private final boolean autoRegistration;
        private final Map<String, String> metadata;

        private ServiceDiscoveryConfig(final List<String> discoveryProtocols, final Duration healthCheckInterval,
                                      final Duration serviceTimeout, final boolean autoRegistration,
                                      final Map<String, String> metadata) {
            this.discoveryProtocols = discoveryProtocols;
            this.healthCheckInterval = healthCheckInterval;
            this.serviceTimeout = serviceTimeout;
            this.autoRegistration = autoRegistration;
            this.metadata = metadata;
        }

        public static ServiceDiscoveryConfig defaultConfig() {
            return new ServiceDiscoveryConfig(
                List.of("dns", "consul", "etcd"),
                Duration.ofSeconds(30),
                Duration.ofMinutes(5),
                true,
                Map.of()
            );
        }

        public List<String> getDiscoveryProtocols() { return discoveryProtocols; }
        public Duration getHealthCheckInterval() { return healthCheckInterval; }
        public Duration getServiceTimeout() { return serviceTimeout; }
        public boolean isAutoRegistration() { return autoRegistration; }
        public Map<String, String> getMetadata() { return metadata; }
    }

    /**
     * Load balancing configuration.
     */
    public static final class LoadBalancingConfig {
        private final String defaultStrategy;
        private final List<String> availableStrategies;
        private final Duration circuitBreakerTimeout;
        private final int circuitBreakerFailureThreshold;
        private final boolean healthCheckEnabled;

        private LoadBalancingConfig(final String defaultStrategy, final List<String> availableStrategies,
                                   final Duration circuitBreakerTimeout, final int circuitBreakerFailureThreshold,
                                   final boolean healthCheckEnabled) {
            this.defaultStrategy = defaultStrategy;
            this.availableStrategies = availableStrategies;
            this.circuitBreakerTimeout = circuitBreakerTimeout;
            this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
            this.healthCheckEnabled = healthCheckEnabled;
        }

        public static LoadBalancingConfig defaultConfig() {
            return new LoadBalancingConfig(
                "round_robin",
                List.of("round_robin", "weighted_round_robin", "least_connections", "least_response_time", "consistent_hash"),
                Duration.ofSeconds(30),
                5,
                true
            );
        }

        public String getDefaultStrategy() { return defaultStrategy; }
        public List<String> getAvailableStrategies() { return availableStrategies; }
        public Duration getCircuitBreakerTimeout() { return circuitBreakerTimeout; }
        public int getCircuitBreakerFailureThreshold() { return circuitBreakerFailureThreshold; }
        public boolean isHealthCheckEnabled() { return healthCheckEnabled; }
    }

    /**
     * Security configuration.
     */
    public static final class SecurityConfig {
        private final boolean encryptionEnabled;
        private final String encryptionAlgorithm;
        private final boolean accessControlEnabled;
        private final boolean auditLoggingEnabled;
        private final boolean threatDetectionEnabled;
        private final List<String> complianceFrameworks;

        private SecurityConfig(final boolean encryptionEnabled, final String encryptionAlgorithm,
                              final boolean accessControlEnabled, final boolean auditLoggingEnabled,
                              final boolean threatDetectionEnabled, final List<String> complianceFrameworks) {
            this.encryptionEnabled = encryptionEnabled;
            this.encryptionAlgorithm = encryptionAlgorithm;
            this.accessControlEnabled = accessControlEnabled;
            this.auditLoggingEnabled = auditLoggingEnabled;
            this.threatDetectionEnabled = threatDetectionEnabled;
            this.complianceFrameworks = complianceFrameworks;
        }

        public static SecurityConfig defaultConfig() {
            return new SecurityConfig(
                true,
                "AES_256_GCM",
                true,
                true,
                true,
                List.of()
            );
        }

        public boolean isEncryptionEnabled() { return encryptionEnabled; }
        public String getEncryptionAlgorithm() { return encryptionAlgorithm; }
        public boolean isAccessControlEnabled() { return accessControlEnabled; }
        public boolean isAuditLoggingEnabled() { return auditLoggingEnabled; }
        public boolean isThreatDetectionEnabled() { return threatDetectionEnabled; }
        public List<String> getComplianceFrameworks() { return complianceFrameworks; }
    }

    /**
     * Streaming configuration.
     */
    public static final class StreamingConfig {
        private final boolean enabled;
        private final int maxPipelines;
        private final int backpressureThreshold;
        private final Duration processingTimeout;
        private final boolean analyticsEnabled;

        private StreamingConfig(final boolean enabled, final int maxPipelines, final int backpressureThreshold,
                               final Duration processingTimeout, final boolean analyticsEnabled) {
            this.enabled = enabled;
            this.maxPipelines = maxPipelines;
            this.backpressureThreshold = backpressureThreshold;
            this.processingTimeout = processingTimeout;
            this.analyticsEnabled = analyticsEnabled;
        }

        public static StreamingConfig defaultConfig() {
            return new StreamingConfig(
                true,
                10,
                1000,
                Duration.ofMinutes(1),
                true
            );
        }

        public boolean isEnabled() { return enabled; }
        public int getMaxPipelines() { return maxPipelines; }
        public int getBackpressureThreshold() { return backpressureThreshold; }
        public Duration getProcessingTimeout() { return processingTimeout; }
        public boolean isAnalyticsEnabled() { return analyticsEnabled; }
    }

    /**
     * CDN configuration.
     */
    public static final class CdnConfig {
        private final boolean enabled;
        private final List<String> edgeLocations;
        private final Duration cacheTtl;
        private final long maxCacheSize;
        private final String routingStrategy;

        private CdnConfig(final boolean enabled, final List<String> edgeLocations, final Duration cacheTtl,
                         final long maxCacheSize, final String routingStrategy) {
            this.enabled = enabled;
            this.edgeLocations = edgeLocations;
            this.cacheTtl = cacheTtl;
            this.maxCacheSize = maxCacheSize;
            this.routingStrategy = routingStrategy;
        }

        public static CdnConfig defaultConfig() {
            return new CdnConfig(
                false,
                List.of(),
                Duration.ofHours(1),
                1024L * 1024L * 1024L, // 1GB
                "nearest_edge"
            );
        }

        public boolean isEnabled() { return enabled; }
        public List<String> getEdgeLocations() { return edgeLocations; }
        public Duration getCacheTtl() { return cacheTtl; }
        public long getMaxCacheSize() { return maxCacheSize; }
        public String getRoutingStrategy() { return routingStrategy; }
    }

    /**
     * Analytics configuration.
     */
    public static final class AnalyticsConfig {
        private final boolean enabled;
        private final Duration metricsInterval;
        private final boolean anomalyDetectionEnabled;
        private final boolean optimizationEnabled;
        private final List<String> metricTypes;

        private AnalyticsConfig(final boolean enabled, final Duration metricsInterval,
                               final boolean anomalyDetectionEnabled, final boolean optimizationEnabled,
                               final List<String> metricTypes) {
            this.enabled = enabled;
            this.metricsInterval = metricsInterval;
            this.anomalyDetectionEnabled = anomalyDetectionEnabled;
            this.optimizationEnabled = optimizationEnabled;
            this.metricTypes = metricTypes;
        }

        public static AnalyticsConfig defaultConfig() {
            return new AnalyticsConfig(
                true,
                Duration.ofMinutes(1),
                true,
                true,
                List.of("performance", "usage", "errors")
            );
        }

        public boolean isEnabled() { return enabled; }
        public Duration getMetricsInterval() { return metricsInterval; }
        public boolean isAnomalyDetectionEnabled() { return anomalyDetectionEnabled; }
        public boolean isOptimizationEnabled() { return optimizationEnabled; }
        public List<String> getMetricTypes() { return metricTypes; }
    }

    /**
     * Federation configuration.
     */
    public static final class FederationConfig {
        private final boolean enabled;
        private final String consistencyLevel;
        private final String replicationStrategy;
        private final Duration syncInterval;
        private final String conflictResolution;

        private FederationConfig(final boolean enabled, final String consistencyLevel,
                                final String replicationStrategy, final Duration syncInterval,
                                final String conflictResolution) {
            this.enabled = enabled;
            this.consistencyLevel = consistencyLevel;
            this.replicationStrategy = replicationStrategy;
            this.syncInterval = syncInterval;
            this.conflictResolution = conflictResolution;
        }

        public static FederationConfig defaultConfig() {
            return new FederationConfig(
                false,
                "eventual",
                "full",
                Duration.ofSeconds(30),
                "last_write_wins"
            );
        }

        public boolean isEnabled() { return enabled; }
        public String getConsistencyLevel() { return consistencyLevel; }
        public String getReplicationStrategy() { return replicationStrategy; }
        public Duration getSyncInterval() { return syncInterval; }
        public String getConflictResolution() { return conflictResolution; }
    }

    /**
     * Monitoring configuration.
     */
    public static final class MonitoringConfig {
        private final boolean enabled;
        private final Duration healthCheckInterval;
        private final List<String> healthChecks;
        private final boolean metricsExportEnabled;
        private final String metricsFormat;

        private MonitoringConfig(final boolean enabled, final Duration healthCheckInterval,
                                final List<String> healthChecks, final boolean metricsExportEnabled,
                                final String metricsFormat) {
            this.enabled = enabled;
            this.healthCheckInterval = healthCheckInterval;
            this.healthChecks = healthChecks;
            this.metricsExportEnabled = metricsExportEnabled;
            this.metricsFormat = metricsFormat;
        }

        public static MonitoringConfig defaultConfig() {
            return new MonitoringConfig(
                true,
                Duration.ofSeconds(30),
                List.of("service_health", "component_health", "system_health"),
                true,
                "prometheus"
            );
        }

        public boolean isEnabled() { return enabled; }
        public Duration getHealthCheckInterval() { return healthCheckInterval; }
        public List<String> getHealthChecks() { return healthChecks; }
        public boolean isMetricsExportEnabled() { return metricsExportEnabled; }
        public String getMetricsFormat() { return metricsFormat; }
    }
}