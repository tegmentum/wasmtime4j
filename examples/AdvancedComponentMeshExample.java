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

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.jni.JniComponentMeshManager.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Advanced Component Model demonstration showcasing distributed components,
 * mesh networking, streaming, federation, CDN, analytics, and security.
 *
 * <p>This example demonstrates the comprehensive capabilities of wasmtime4j's
 * advanced component model for building distributed, scalable, and secure
 * WebAssembly component systems.
 */
public class AdvancedComponentMeshExample {

    public static void main(final String[] args) {
        System.out.println("=== Advanced Component Mesh Example ===\n");

        try {
            runAdvancedComponentMeshDemo();
        } catch (final Exception e) {
            System.err.println("Error running advanced component mesh demo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runAdvancedComponentMeshDemo() throws Exception {
        // Create comprehensive mesh configuration
        final MeshConfig meshConfig = MeshConfig.builder()
            .serviceDiscovery(createServiceDiscoveryConfig())
            .loadBalancing(createLoadBalancingConfig())
            .security(createSecurityConfig())
            .streaming(createStreamingConfig())
            .cdn(createCdnConfig())
            .analytics(createAnalyticsConfig())
            .federation(createFederationConfig())
            .monitoring(createMonitoringConfig())
            .build();

        System.out.println("Creating component mesh manager with comprehensive configuration...");

        try (ComponentMeshManager meshManager = ComponentMeshManager.create(meshConfig)) {
            System.out.println("✓ Component mesh manager created successfully");
            System.out.println("✓ Mesh health status: " + (meshManager.isHealthy() ? "HEALTHY" : "UNHEALTHY"));
            System.out.println();

            // Demonstrate service discovery and load balancing
            demonstrateServiceDiscoveryAndLoadBalancing(meshManager);

            // Demonstrate real-time streaming
            demonstrateRealtimeStreaming(meshManager);

            // Demonstrate CDN integration
            demonstrateCdnIntegration(meshManager);

            // Demonstrate security features
            demonstrateSecurityFeatures(meshManager);

            // Demonstrate analytics and monitoring
            demonstrateAnalyticsAndMonitoring(meshManager);

            // Demonstrate federation capabilities
            demonstrateFederationCapabilities(meshManager);

            // Display final statistics
            displayMeshStatistics(meshManager);

            System.out.println("\n=== Advanced Component Mesh Demo Completed Successfully ===");
        }
    }

    private static void demonstrateServiceDiscoveryAndLoadBalancing(final ComponentMeshManager meshManager) {
        System.out.println("--- Service Discovery and Load Balancing Demo ---");

        try {
            // Register multiple service instances
            for (int i = 1; i <= 3; i++) {
                final ServiceEndpoint service = new ServiceEndpoint(
                    "user-service-" + i,
                    "User Management Service",
                    List.of("https://user-service-" + i + ".example.com:8080"),
                    Map.of(
                        "version", "2.1.0",
                        "region", "us-east-" + i,
                        "datacenter", "dc-" + i,
                        "capabilities", "authentication,authorization,user-profile"
                    ),
                    ServiceHealthStatus.HEALTHY,
                    1.0 + (i * 0.1), // Varying weights for load balancing
                    "2.1.0"
                );

                meshManager.registerService(service);
                System.out.println("✓ Registered service: " + service.getServiceId() +
                                 " (weight: " + service.getWeight() + ")");
            }

            // Discover services
            final ServiceDiscoveryCriteria criteria = new ServiceDiscoveryCriteria(
                "User Management Service",
                List.of("https"),
                "2.0.0",
                List.of("authentication")
            );

            final List<ServiceEndpoint> discoveredServices = meshManager.discoverServices(criteria);
            System.out.println("✓ Discovered " + discoveredServices.size() + " services matching criteria");

            // Demonstrate load balancing
            System.out.println("✓ Testing load balancing with different strategies:");

            final String[] strategies = {"round_robin", "weighted_round_robin", "least_connections"};

            for (final String strategy : strategies) {
                System.out.println("  - Testing " + strategy + " strategy:");

                for (int i = 0; i < 3; i++) {
                    final RequestContext request = new RequestContext(
                        "req-" + System.currentTimeMillis() + "-" + i,
                        "web-client-demo",
                        Map.of("service_name", "User Management Service", "load_balancing_strategy", strategy),
                        RequestPriority.NORMAL,
                        30000L
                    );

                    final ServiceEndpoint selectedService = meshManager.routeRequest(request);
                    System.out.println("    → Routed to: " + selectedService.getServiceId() +
                                     " (weight: " + selectedService.getWeight() + ")");
                }
            }

        } catch (final Exception e) {
            System.err.println("✗ Error in service discovery demo: " + e.getMessage());
        }

        System.out.println();
    }

    private static void demonstrateRealtimeStreaming(final ComponentMeshManager meshManager) {
        System.out.println("--- Real-time Streaming Demo ---");

        try {
            // Create streaming pipeline for user events
            final StreamPipelineConfig pipelineConfig = new StreamPipelineConfig(
                "user-activity-pipeline",
                List.of(
                    new StreamProcessorConfig("auth-filter", "authentication-filter",
                        Map.of("requireAuth", "true", "validTokenOnly", "true")),
                    new StreamProcessorConfig("data-enricher", "user-data-enricher",
                        Map.of("includeProfile", "true", "includePreferences", "true")),
                    new StreamProcessorConfig("analytics-processor", "real-time-analytics",
                        Map.of("window", "60s", "aggregations", "count,sum,avg")),
                    new StreamProcessorConfig("notification-sender", "notification-dispatcher",
                        Map.of("channels", "email,push,sms"))
                ),
                Map.of(
                    "routing", "event-type-based",
                    "backpressure", "drop-oldest",
                    "maxQueueSize", "1000"
                )
            );

            final String pipelineId = meshManager.startStreaming(pipelineConfig);
            System.out.println("✓ Started streaming pipeline: " + pipelineId);

            // Process sample events through the pipeline
            final String[] eventTypes = {"user-login", "user-logout", "profile-update", "purchase", "message-sent"};

            for (int i = 0; i < 10; i++) {
                final String eventType = eventTypes[i % eventTypes.length];
                final StreamEvent event = new StreamEvent(
                    "event-" + System.currentTimeMillis() + "-" + i,
                    eventType,
                    System.currentTimeMillis(),
                    createEventPayload(eventType, i),
                    Map.of(
                        "source", "web-application",
                        "userId", "user-" + (i % 5 + 1),
                        "sessionId", "session-" + (i / 5 + 1),
                        "region", "us-east-1"
                    )
                );

                meshManager.processEvent(pipelineId, event);
                System.out.println("  → Processed event: " + event.getEventId() + " (type: " + eventType + ")");
            }

            // Get streaming statistics
            final StreamingStatistics stats = meshManager.getStreamingStatistics(pipelineId);
            System.out.println("✓ Pipeline statistics:");
            System.out.println("  - Events processed: " + stats.getEventsProcessed());
            System.out.println("  - Average processing time: " + stats.getAvgProcessingTimeMs() + "ms");
            System.out.println("  - Throughput: " + stats.getEventsPerSecond() + " events/sec");
            System.out.println("  - Error rate: " + String.format("%.2f", stats.getErrorRate() * 100) + "%");

            // Stop the pipeline
            meshManager.stopStreaming(pipelineId);
            System.out.println("✓ Stopped streaming pipeline: " + pipelineId);

        } catch (final Exception e) {
            System.err.println("✗ Error in streaming demo: " + e.getMessage());
        }

        System.out.println();
    }

    private static void demonstrateCdnIntegration(final ComponentMeshManager meshManager) {
        System.out.println("--- CDN Integration Demo ---");

        try {
            final ComponentId uiComponentId = ComponentId.of("user-interface-component");

            // Configure CDN with multiple edge locations
            final CdnConfig cdnConfig = new CdnConfig(
                List.of("us-east-1", "us-west-2", "eu-west-1", "asia-pacific-1"),
                Map.of(
                    "cacheTtl", Duration.ofHours(6).toString(),
                    "maxCacheSize", "500MB",
                    "compressionEnabled", "true",
                    "compressionAlgorithm", "gzip,brotli",
                    "routingStrategy", "latency-optimized"
                ),
                true
            );

            meshManager.enableCdn(uiComponentId, cdnConfig);
            System.out.println("✓ Enabled CDN for component: " + uiComponentId);

            // Cache various types of content
            final Map<String, String> contentMap = Map.of(
                "index.html", "<!DOCTYPE html><html><head><title>User Dashboard</title></head><body><h1>Welcome!</h1></body></html>",
                "app.js", "console.log('Application started'); class UserManager { constructor() { this.users = []; } }",
                "styles.css", "body { font-family: Arial, sans-serif; } .header { background-color: #333; color: white; }",
                "config.json", "{\"apiEndpoint\": \"https://api.example.com\", \"version\": \"2.1.0\", \"features\": [\"auth\", \"profiles\"]}",
                "logo.png", "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="
            );

            for (final Map.Entry<String, String> entry : contentMap.entrySet()) {
                final String contentKey = entry.getKey();
                final byte[] content = entry.getValue().getBytes();

                meshManager.cacheContent(uiComponentId, contentKey, content);
                System.out.println("  → Cached content: " + contentKey + " (" + content.length + " bytes)");
            }

            // Retrieve and verify cached content
            for (final String contentKey : contentMap.keySet()) {
                final byte[] cachedContent = meshManager.getCachedContent(uiComponentId, contentKey);
                if (cachedContent != null) {
                    System.out.println("  ← Retrieved cached content: " + contentKey + " (" + cachedContent.length + " bytes)");
                }
            }

            // Get CDN statistics
            final CdnStatistics cdnStats = meshManager.getCdnStatistics(uiComponentId);
            System.out.println("✓ CDN Performance:");
            System.out.println("  - Cache hit rate: " + String.format("%.1f", cdnStats.getCacheHitRate() * 100) + "%");
            System.out.println("  - Total requests: " + cdnStats.getTotalRequests());
            System.out.println("  - Bandwidth saved: " + formatBytes(cdnStats.getBandwidthSaved()));
            System.out.println("  - Average response time: " + cdnStats.getAvgResponseTimeMs() + "ms");

        } catch (final Exception e) {
            System.err.println("✗ Error in CDN demo: " + e.getMessage());
        }

        System.out.println();
    }

    private static void demonstrateSecurityFeatures(final ComponentMeshManager meshManager) {
        System.out.println("--- Security Features Demo ---");

        try {
            // Create comprehensive security policy
            final SecurityPolicy securityPolicy = new SecurityPolicy(
                "enterprise-security-policy",
                Map.of(
                    "encryptionAlgorithm", "AES_256_GCM",
                    "keySize", "256",
                    "keyRotationInterval", Duration.ofDays(30).toString(),
                    "hashAlgorithm", "SHA-256"
                ),
                List.of(
                    "admin:*:allow",
                    "developer:read,write:allow",
                    "operator:read:allow",
                    "guest:read:deny"
                ),
                true
            );

            meshManager.applySecurityPolicy(securityPolicy);
            System.out.println("✓ Applied comprehensive security policy: " + securityPolicy.getName());

            // Demonstrate encryption and decryption
            final String[] sensitiveData = {
                "User authentication token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                "Database connection string: postgresql://user:pass@localhost:5432/db",
                "API key: sk_live_51HqKpnKjYxDZq3r2L8K9Q...",
                "User personal information: {name: 'John Doe', ssn: '123-45-6789'}"
            };

            final EncryptionContext encryptionContext = EncryptionContext.builder()
                .algorithm("AES_256_GCM")
                .keyId("enterprise-master-key")
                .addMetadata("component", "security-demo")
                .addMetadata("timestamp", String.valueOf(System.currentTimeMillis()))
                .build();

            System.out.println("✓ Encrypting sensitive data:");
            for (int i = 0; i < sensitiveData.length; i++) {
                final byte[] plaintext = sensitiveData[i].getBytes();
                final byte[] encrypted = meshManager.encryptData(plaintext, encryptionContext);

                System.out.println("  → Data " + (i + 1) + ": " + plaintext.length + " bytes → " +
                                 encrypted.length + " bytes encrypted");

                // Decrypt to verify
                final DecryptionContext decryptionContext = DecryptionContext.builder()
                    .algorithm("AES_256_GCM")
                    .keyId("enterprise-master-key")
                    .build();

                final byte[] decrypted = meshManager.decryptData(encrypted, decryptionContext);
                final boolean verified = java.util.Arrays.equals(plaintext, decrypted);
                System.out.println("    ✓ Decryption verified: " + verified);
            }

            // Get compliance status
            final ComplianceStatus complianceStatus = meshManager.getComplianceStatus();
            System.out.println("✓ Compliance Status:");
            System.out.println("  - Overall score: " + complianceStatus.getOverallScore() + "%");
            System.out.println("  - Frameworks: " + String.join(", ", complianceStatus.getFrameworks()));
            System.out.println("  - Violations: " + complianceStatus.getViolations().size());
            System.out.println("  - Last audit: " + complianceStatus.getLastAuditDate());

        } catch (final Exception e) {
            System.err.println("✗ Error in security demo: " + e.getMessage());
        }

        System.out.println();
    }

    private static void demonstrateAnalyticsAndMonitoring(final ComponentMeshManager meshManager) {
        System.out.println("--- Analytics and Monitoring Demo ---");

        try {
            final ComponentId analyticsComponentId = ComponentId.of("analytics-demo-component");

            // Configure advanced monitoring
            final MonitoringConfig monitoringConfig = MonitoringConfig.builder()
                .metricsInterval(Duration.ofSeconds(15))
                .enablePerformanceTracking(true)
                .enableAnomalyDetection(true)
                .enableOptimizationRecommendations(true)
                .enablePredictiveAnalytics(true)
                .addCustomMetric("business_transactions_per_minute")
                .addCustomMetric("user_engagement_score")
                .addCustomMetric("system_efficiency_ratio")
                .build();

            meshManager.configureMonitoring(analyticsComponentId, monitoringConfig);
            System.out.println("✓ Configured advanced monitoring for component: " + analyticsComponentId);

            // Get component analytics
            final ComponentAnalytics analytics = meshManager.getAnalytics(analyticsComponentId);
            System.out.println("✓ Component Analytics Summary:");
            System.out.println("  - Performance score: " + analytics.getPerformanceScore() + "/100");
            System.out.println("  - Availability: " + String.format("%.3f", analytics.getAvailability() * 100) + "%");
            System.out.println("  - Average response time: " + analytics.getAvgResponseTimeMs() + "ms");
            System.out.println("  - Throughput: " + analytics.getThroughputPerSecond() + " ops/sec");
            System.out.println("  - Error rate: " + String.format("%.2f", analytics.getErrorRate() * 100) + "%");

            // Display anomalies detected
            if (!analytics.getAnomalies().isEmpty()) {
                System.out.println("  - Anomalies detected: " + analytics.getAnomalies().size());
                for (final String anomaly : analytics.getAnomalies()) {
                    System.out.println("    → " + anomaly);
                }
            } else {
                System.out.println("  - No anomalies detected");
            }

            // Get optimization recommendations
            final List<OptimizationRecommendation> recommendations =
                meshManager.getOptimizationRecommendations(analyticsComponentId);

            if (!recommendations.isEmpty()) {
                System.out.println("✓ Optimization Recommendations:");
                for (int i = 0; i < Math.min(recommendations.size(), 3); i++) {
                    final OptimizationRecommendation rec = recommendations.get(i);
                    System.out.println("  " + (i + 1) + ". " + rec.getTitle() +
                                     " (Impact: " + rec.getExpectedImpact() + "%)");
                    System.out.println("     " + rec.getDescription());
                }
            }

            // Get mesh-wide analytics
            final MeshAnalytics meshAnalytics = meshManager.getMeshAnalytics();
            System.out.println("✓ Mesh-wide Analytics:");
            System.out.println("  - Total components: " + meshAnalytics.getTotalComponents());
            System.out.println("  - Active services: " + meshAnalytics.getActiveServices());
            System.out.println("  - Network efficiency: " + meshAnalytics.getNetworkEfficiency() + "%");
            System.out.println("  - Resource utilization: " + meshAnalytics.getResourceUtilization() + "%");

        } catch (final Exception e) {
            System.err.println("✗ Error in analytics demo: " + e.getMessage());
        }

        System.out.println();
    }

    private static void demonstrateFederationCapabilities(final ComponentMeshManager meshManager) {
        System.out.println("--- Federation Capabilities Demo ---");

        try {
            // Create cluster information for federation
            final ClusterInfo productionCluster = new ClusterInfo(
                "production-cluster-east",
                "Production East Coast",
                "3.1.0",
                List.of("https://prod-east-1.example.com", "https://prod-east-2.example.com")
            );

            final ClusterInfo developmentCluster = new ClusterInfo(
                "development-cluster",
                "Development Environment",
                "3.1.0",
                List.of("https://dev-1.example.com")
            );

            // Configure cluster join settings
            final ClusterJoinConfig joinConfig = ClusterJoinConfig.builder()
                .authToken("federation-auth-token-" + System.currentTimeMillis())
                .secureConnection(true)
                .timeout(Duration.ofMinutes(3))
                .enableHeartbeat(true)
                .heartbeatInterval(Duration.ofSeconds(30))
                .build();

            // Join production cluster (async)
            System.out.println("✓ Joining production cluster...");
            final CompletableFuture<Void> prodJoinFuture = meshManager.joinCluster(productionCluster, joinConfig);

            // Join development cluster (async)
            System.out.println("✓ Joining development cluster...");
            final CompletableFuture<Void> devJoinFuture = meshManager.joinCluster(developmentCluster, joinConfig);

            // Wait for cluster joins to complete
            CompletableFuture.allOf(prodJoinFuture, devJoinFuture).get();
            System.out.println("✓ Successfully joined both clusters");

            // Federate a component across clusters
            final ComponentId federatedComponentId = ComponentId.of("user-authentication-service");
            final ComponentFederationConfig federationConfig = new ComponentFederationConfig(
                List.of("production-cluster-east", "development-cluster"),
                "selective_replication", // Only replicate to production for critical services
                "strong" // Strong consistency for authentication service
            );

            meshManager.federateComponent(federatedComponentId, federationConfig);
            System.out.println("✓ Federated component across clusters: " + federatedComponentId);

            // Check federation status
            final FederationStatus federationStatus = meshManager.getFederationStatus(federatedComponentId);
            System.out.println("✓ Federation Status:");
            System.out.println("  - State: " + federationStatus.getState());
            System.out.println("  - Clusters: " + String.join(", ", federationStatus.getClusters()));
            System.out.println("  - Synchronization: " + federationStatus.getSynchronizationStatus());
            System.out.println("  - Last sync: " + federationStatus.getLastSyncTime());

        } catch (final Exception e) {
            System.err.println("✗ Error in federation demo: " + e.getMessage());
        }

        System.out.println();
    }

    private static void displayMeshStatistics(final ComponentMeshManager meshManager) {
        System.out.println("--- Final Mesh Statistics ---");

        try {
            final MeshStatistics statistics = meshManager.getStatistics();

            System.out.println("✓ Overall Mesh Performance:");
            System.out.println("  - Total services: " + statistics.getTotalServices());
            System.out.println("  - Active services: " + statistics.getActiveServices());
            System.out.println("  - Average response time: " + String.format("%.2f", statistics.getAvgResponseTime()) + "ms");
            System.out.println("  - Total requests processed: " + statistics.getTotalRequests());
            System.out.println("  - Error rate: " + String.format("%.3f", statistics.getErrorRate() * 100) + "%");
            System.out.println("  - Uptime: " + formatDuration(statistics.getUptimeMs()));
            System.out.println("  - Memory usage: " + formatBytes(statistics.getMemoryUsageBytes()));
            System.out.println("  - Network I/O: " + formatBytes(statistics.getNetworkBytesTransferred()));

        } catch (final Exception e) {
            System.err.println("✗ Error getting mesh statistics: " + e.getMessage());
        }
    }

    // Helper methods for configuration creation

    private static MeshConfig.ServiceDiscoveryConfig createServiceDiscoveryConfig() {
        return MeshConfig.ServiceDiscoveryConfig.defaultConfig();
    }

    private static MeshConfig.LoadBalancingConfig createLoadBalancingConfig() {
        return MeshConfig.LoadBalancingConfig.defaultConfig();
    }

    private static MeshConfig.SecurityConfig createSecurityConfig() {
        return MeshConfig.SecurityConfig.defaultConfig();
    }

    private static MeshConfig.StreamingConfig createStreamingConfig() {
        return MeshConfig.StreamingConfig.defaultConfig();
    }

    private static MeshConfig.CdnConfig createCdnConfig() {
        return MeshConfig.CdnConfig.defaultConfig();
    }

    private static MeshConfig.AnalyticsConfig createAnalyticsConfig() {
        return MeshConfig.AnalyticsConfig.defaultConfig();
    }

    private static MeshConfig.FederationConfig createFederationConfig() {
        return MeshConfig.FederationConfig.defaultConfig();
    }

    private static MeshConfig.MonitoringConfig createMonitoringConfig() {
        return MeshConfig.MonitoringConfig.defaultConfig();
    }

    // Helper methods

    private static byte[] createEventPayload(final String eventType, final int index) {
        final String payload = String.format(
            "{\"eventType\":\"%s\",\"index\":%d,\"timestamp\":%d,\"data\":{\"action\":\"%s\",\"details\":\"Event %d details\"}}",
            eventType, index, System.currentTimeMillis(), eventType, index
        );
        return payload.getBytes();
    }

    private static String formatBytes(final long bytes) {
        if (bytes < 1024) return bytes + " B";
        final int exp = (int) (Math.log(bytes) / Math.log(1024));
        final String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private static String formatDuration(final long millis) {
        final long seconds = millis / 1000;
        final long minutes = seconds / 60;
        final long hours = minutes / 60;
        final long days = hours / 24;

        if (days > 0) return days + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}