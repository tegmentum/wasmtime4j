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

package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import ai.tegmentum.wasmtime4j.jni.JniComponentMeshManager.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for advanced Component Model features.
 *
 * <p>This test validates the implementation of distributed components, component mesh networking,
 * streaming, federation, CDN integration, analytics, and security features.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfSystemProperty(named = "wasmtime4j.test.advanced-components", matches = "true")
class AdvancedComponentModelTest {

    private ComponentMeshManager meshManager;
    private MeshConfig testConfig;

    @BeforeEach
    void setUp() {
        // Create comprehensive test configuration
        testConfig = MeshConfig.builder()
            .serviceDiscovery(createServiceDiscoveryConfig())
            .loadBalancing(createLoadBalancingConfig())
            .security(createSecurityConfig())
            .streaming(createStreamingConfig())
            .cdn(createCdnConfig())
            .analytics(createAnalyticsConfig())
            .federation(createFederationConfig())
            .monitoring(createMonitoringConfig())
            .build();

        meshManager = ComponentMeshManager.create(testConfig);
        assertNotNull(meshManager, "ComponentMeshManager should be created successfully");
        assertTrue(meshManager.isHealthy(), "ComponentMeshManager should be healthy");
    }

    @AfterEach
    void tearDown() {
        if (meshManager != null) {
            meshManager.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test service discovery and registration")
    void testServiceDiscoveryAndRegistration() {
        // Create test service endpoint
        final ServiceEndpoint serviceEndpoint = new ServiceEndpoint(
            "user-service-1",
            "User Management Service",
            List.of("https://user-service.example.com:8080"),
            Map.of("version", "1.2.0", "region", "us-east-1"),
            ServiceHealthStatus.HEALTHY,
            1.0,
            "1.2.0"
        );

        // Register service
        assertDoesNotThrow(() -> meshManager.registerService(serviceEndpoint),
            "Service registration should not throw exception");

        // Discover services
        final ServiceDiscoveryCriteria criteria = new ServiceDiscoveryCriteria(
            "User Management Service",
            List.of("https"),
            "1.0.0",
            List.of("user-management")
        );

        final List<ServiceEndpoint> discoveredServices = assertDoesNotThrow(
            () -> meshManager.discoverServices(criteria),
            "Service discovery should not throw exception"
        );

        assertNotNull(discoveredServices, "Discovered services should not be null");
        assertFalse(discoveredServices.isEmpty(), "Should discover at least one service");

        final ServiceEndpoint discovered = discoveredServices.get(0);
        assertEquals("user-service-1", discovered.getServiceId(), "Service ID should match");
        assertEquals(ServiceHealthStatus.HEALTHY, discovered.getHealthStatus(), "Service should be healthy");

        // Update health status
        assertDoesNotThrow(() -> meshManager.updateServiceHealth("user-service-1", ServiceHealthStatus.DEGRADED),
            "Health status update should not throw exception");

        // Unregister service
        assertDoesNotThrow(() -> meshManager.unregisterService("user-service-1"),
            "Service unregistration should not throw exception");
    }

    @Test
    @Order(2)
    @DisplayName("Test load balancing and request routing")
    void testLoadBalancingAndRequestRouting() {
        // Register multiple service instances
        for (int i = 1; i <= 3; i++) {
            final ServiceEndpoint serviceEndpoint = new ServiceEndpoint(
                "api-service-" + i,
                "API Service",
                List.of("https://api-service-" + i + ".example.com:8080"),
                Map.of("instance", String.valueOf(i), "datacenter", "dc-" + i),
                ServiceHealthStatus.HEALTHY,
                1.0,
                "2.1.0"
            );
            meshManager.registerService(serviceEndpoint);
        }

        // Create request context
        final RequestContext request = new RequestContext(
            "req-12345",
            "web-client",
            Map.of("service_name", "API Service", "load_balancing_strategy", "round_robin"),
            RequestPriority.HIGH,
            30000L
        );

        // Route request multiple times to test load balancing
        for (int i = 0; i < 5; i++) {
            final ServiceEndpoint selectedEndpoint = assertDoesNotThrow(
                () -> meshManager.routeRequest(request),
                "Request routing should not throw exception"
            );

            assertNotNull(selectedEndpoint, "Selected endpoint should not be null");
            assertTrue(selectedEndpoint.getServiceId().startsWith("api-service-"), "Should route to API service");
            assertEquals(ServiceHealthStatus.HEALTHY, selectedEndpoint.getHealthStatus(), "Selected service should be healthy");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test streaming pipeline creation and event processing")
    void testStreamingPipelineAndEventProcessing() {
        // Create streaming pipeline configuration
        final StreamPipelineConfig pipelineConfig = new StreamPipelineConfig(
            "user-events-pipeline",
            List.of(
                new StreamProcessorConfig("filter", "event-filter", Map.of("eventType", "user-login")),
                new StreamProcessorConfig("transform", "data-transformer", Map.of("format", "json")),
                new StreamProcessorConfig("analytics", "analytics-processor", Map.of("window", "60s"))
            ),
            Map.of("routing", "event-type-based")
        );

        // Start streaming pipeline
        final String pipelineId = assertDoesNotThrow(
            () -> meshManager.startStreaming(pipelineConfig),
            "Pipeline start should not throw exception"
        );

        assertNotNull(pipelineId, "Pipeline ID should not be null");
        assertFalse(pipelineId.isEmpty(), "Pipeline ID should not be empty");

        // Create test stream events
        final StreamEvent event1 = new StreamEvent(
            "event-001",
            "user-login",
            System.currentTimeMillis(),
            "{\"userId\":123,\"timestamp\":\"2024-01-01T10:00:00Z\"}".getBytes(),
            Map.of("source", "web-app", "region", "us-east-1")
        );

        final StreamEvent event2 = new StreamEvent(
            "event-002",
            "user-logout",
            System.currentTimeMillis(),
            "{\"userId\":123,\"timestamp\":\"2024-01-01T10:30:00Z\"}".getBytes(),
            Map.of("source", "web-app", "region", "us-east-1")
        );

        // Process events through pipeline
        assertDoesNotThrow(() -> meshManager.processEvent(pipelineId, event1),
            "Event processing should not throw exception");
        assertDoesNotThrow(() -> meshManager.processEvent(pipelineId, event2),
            "Event processing should not throw exception");

        // Get streaming statistics
        final StreamingStatistics stats = assertDoesNotThrow(
            () -> meshManager.getStreamingStatistics(pipelineId),
            "Getting streaming statistics should not throw exception"
        );

        assertNotNull(stats, "Streaming statistics should not be null");

        // Stop streaming pipeline
        assertDoesNotThrow(() -> meshManager.stopStreaming(pipelineId),
            "Pipeline stop should not throw exception");
    }

    @Test
    @Order(4)
    @DisplayName("Test CDN integration and content caching")
    void testCdnIntegrationAndContentCaching() {
        final ComponentId componentId = ComponentId.of("ui-component");

        // Create CDN configuration
        final CdnConfig cdnConfig = new CdnConfig(
            List.of("us-east-1", "eu-west-1", "asia-pacific-1"),
            Map.of(
                "cacheTtl", Duration.ofHours(1).toString(),
                "maxSize", "100MB",
                "strategy", "nearest_edge"
            ),
            true
        );

        // Enable CDN for component
        assertDoesNotThrow(() -> meshManager.enableCdn(componentId, cdnConfig),
            "CDN enablement should not throw exception");

        // Cache content
        final String contentKey = "main.js";
        final byte[] content = "console.log('Hello, World!');".getBytes();

        assertDoesNotThrow(() -> meshManager.cacheContent(componentId, contentKey, content),
            "Content caching should not throw exception");

        // Retrieve cached content
        final byte[] cachedContent = assertDoesNotThrow(
            () -> meshManager.getCachedContent(componentId, contentKey),
            "Content retrieval should not throw exception"
        );

        assertNotNull(cachedContent, "Cached content should not be null");
        assertArrayEquals(content, cachedContent, "Cached content should match original content");

        // Get CDN statistics
        final CdnStatistics cdnStats = assertDoesNotThrow(
            () -> meshManager.getCdnStatistics(componentId),
            "Getting CDN statistics should not throw exception"
        );

        assertNotNull(cdnStats, "CDN statistics should not be null");

        // Invalidate content
        assertDoesNotThrow(() -> meshManager.invalidateContent(componentId, contentKey),
            "Content invalidation should not throw exception");

        // Disable CDN
        assertDoesNotThrow(() -> meshManager.disableCdn(componentId),
            "CDN disablement should not throw exception");
    }

    @Test
    @Order(5)
    @DisplayName("Test component analytics and monitoring")
    void testComponentAnalyticsAndMonitoring() {
        final ComponentId componentId = ComponentId.of("analytics-test-component");

        // Configure monitoring
        final MonitoringConfig monitoringConfig = MonitoringConfig.builder()
            .metricsInterval(Duration.ofSeconds(10))
            .enablePerformanceTracking(true)
            .enableAnomalyDetection(true)
            .enableOptimizationRecommendations(true)
            .build();

        assertDoesNotThrow(() -> meshManager.configureMonitoring(componentId, monitoringConfig),
            "Monitoring configuration should not throw exception");

        // Get component analytics
        final ComponentAnalytics analytics = assertDoesNotThrow(
            () -> meshManager.getAnalytics(componentId),
            "Getting component analytics should not throw exception"
        );

        assertNotNull(analytics, "Component analytics should not be null");
        assertEquals(componentId.toString(), analytics.getComponentId(), "Component ID should match");

        // Get optimization recommendations
        final List<OptimizationRecommendation> recommendations = assertDoesNotThrow(
            () -> meshManager.getOptimizationRecommendations(componentId),
            "Getting optimization recommendations should not throw exception"
        );

        assertNotNull(recommendations, "Optimization recommendations should not be null");

        // Get mesh-wide analytics
        final MeshAnalytics meshAnalytics = assertDoesNotThrow(
            () -> meshManager.getMeshAnalytics(),
            "Getting mesh analytics should not throw exception"
        );

        assertNotNull(meshAnalytics, "Mesh analytics should not be null");
    }

    @Test
    @Order(6)
    @DisplayName("Test security policy application and encryption")
    void testSecurityPolicyAndEncryption() {
        // Create security policy
        final SecurityPolicy securityPolicy = new SecurityPolicy(
            "test-security-policy",
            Map.of(
                "algorithm", "AES_256_GCM",
                "keyRotation", Duration.ofHours(24).toString()
            ),
            List.of("admin:read,write", "user:read"),
            true
        );

        // Apply security policy
        assertDoesNotThrow(() -> meshManager.applySecurityPolicy(securityPolicy),
            "Security policy application should not throw exception");

        // Test encryption and decryption
        final byte[] plaintext = "Sensitive component data".getBytes();
        final EncryptionContext encryptionContext = EncryptionContext.builder()
            .algorithm("AES_256_GCM")
            .keyId("test-key-1")
            .build();

        final byte[] encrypted = assertDoesNotThrow(
            () -> meshManager.encryptData(plaintext, encryptionContext),
            "Data encryption should not throw exception"
        );

        assertNotNull(encrypted, "Encrypted data should not be null");
        assertFalse(java.util.Arrays.equals(plaintext, encrypted), "Encrypted data should differ from plaintext");

        final DecryptionContext decryptionContext = DecryptionContext.builder()
            .algorithm("AES_256_GCM")
            .keyId("test-key-1")
            .build();

        final byte[] decrypted = assertDoesNotThrow(
            () -> meshManager.decryptData(encrypted, decryptionContext),
            "Data decryption should not throw exception"
        );

        assertNotNull(decrypted, "Decrypted data should not be null");
        assertArrayEquals(plaintext, decrypted, "Decrypted data should match original plaintext");

        // Get compliance status
        final ComplianceStatus complianceStatus = assertDoesNotThrow(
            () -> meshManager.getComplianceStatus(),
            "Getting compliance status should not throw exception"
        );

        assertNotNull(complianceStatus, "Compliance status should not be null");

        // Remove security policy
        assertDoesNotThrow(() -> meshManager.removeSecurityPolicy("test-security-policy"),
            "Security policy removal should not throw exception");
    }

    @Test
    @Order(7)
    @DisplayName("Test federation cluster operations")
    void testFederationClusterOperations() {
        // Create cluster information
        final ClusterInfo clusterInfo = new ClusterInfo(
            "test-cluster-1",
            "Test Cluster",
            "1.0.0",
            List.of("https://cluster-node-1.example.com", "https://cluster-node-2.example.com")
        );

        // Create join configuration
        final ClusterJoinConfig joinConfig = new ClusterJoinConfig(
            "test-auth-token",
            true,
            Duration.ofMinutes(5).toMillis()
        );

        // Join cluster (async operation)
        final CompletableFuture<Void> joinFuture = meshManager.joinCluster(clusterInfo, joinConfig);

        assertDoesNotThrow(() -> joinFuture.get(30, TimeUnit.SECONDS),
            "Cluster join should complete successfully");

        // Create federation configuration
        final ComponentFederationConfig federationConfig = new ComponentFederationConfig(
            List.of("test-cluster-1"),
            "full_replication",
            "eventual"
        );

        final ComponentId componentId = ComponentId.of("federated-component");

        // Federate component
        assertDoesNotThrow(() -> meshManager.federateComponent(componentId, federationConfig),
            "Component federation should not throw exception");

        // Get federation status
        final FederationStatus federationStatus = assertDoesNotThrow(
            () -> meshManager.getFederationStatus(componentId),
            "Getting federation status should not throw exception"
        );

        assertNotNull(federationStatus, "Federation status should not be null");

        // Unfederate component
        assertDoesNotThrow(() -> meshManager.unfederateComponent(componentId),
            "Component unfederation should not throw exception");

        // Leave cluster (async operation)
        final CompletableFuture<Void> leaveFuture = meshManager.leaveCluster("test-cluster-1");

        assertDoesNotThrow(() -> leaveFuture.get(30, TimeUnit.SECONDS),
            "Cluster leave should complete successfully");
    }

    @Test
    @Order(8)
    @DisplayName("Test mesh statistics and health monitoring")
    void testMeshStatisticsAndHealthMonitoring() {
        // Get mesh statistics
        final MeshStatistics statistics = assertDoesNotThrow(
            () -> meshManager.getStatistics(),
            "Getting mesh statistics should not throw exception"
        );

        assertNotNull(statistics, "Mesh statistics should not be null");
        assertTrue(statistics.getTotalServices() >= 0, "Total services should be non-negative");
        assertTrue(statistics.getActiveServices() >= 0, "Active services should be non-negative");
        assertTrue(statistics.getAvgResponseTime() >= 0, "Average response time should be non-negative");
        assertTrue(statistics.getErrorRate() >= 0.0 && statistics.getErrorRate() <= 1.0, "Error rate should be between 0 and 1");

        // Check mesh health
        assertTrue(meshManager.isHealthy(), "Mesh should be healthy");

        // Get current configuration
        final MeshConfig currentConfig = meshManager.getConfiguration();
        assertNotNull(currentConfig, "Current configuration should not be null");

        // Update configuration
        final MeshConfig updatedConfig = MeshConfig.builder()
            .serviceDiscovery(testConfig.getServiceDiscovery())
            .loadBalancing(testConfig.getLoadBalancing())
            .security(testConfig.getSecurity())
            .streaming(testConfig.getStreaming())
            .cdn(testConfig.getCdn())
            .analytics(testConfig.getAnalytics())
            .federation(testConfig.getFederation())
            .monitoring(testConfig.getMonitoring())
            .build();

        assertDoesNotThrow(() -> meshManager.updateConfiguration(updatedConfig),
            "Configuration update should not throw exception");
    }

    @Test
    @Order(9)
    @DisplayName("Test error handling and edge cases")
    void testErrorHandlingAndEdgeCases() {
        // Test null parameter handling
        assertThrows(IllegalArgumentException.class, () -> meshManager.registerService(null),
            "Should throw exception for null service endpoint");

        assertThrows(IllegalArgumentException.class, () -> meshManager.discoverServices(null),
            "Should throw exception for null discovery criteria");

        assertThrows(IllegalArgumentException.class, () -> meshManager.routeRequest(null),
            "Should throw exception for null request context");

        // Test operations on non-existent resources
        assertThrows(WasmRuntimeException.class, () -> meshManager.updateServiceHealth("non-existent-service", ServiceHealthStatus.HEALTHY),
            "Should throw exception for non-existent service");

        assertThrows(WasmRuntimeException.class, () -> meshManager.stopStreaming("non-existent-pipeline"),
            "Should throw exception for non-existent pipeline");

        assertThrows(WasmRuntimeException.class, () -> meshManager.getFederationStatus(ComponentId.of("non-existent-component")),
            "Should throw exception for non-existent component");

        // Test operations after manager is closed
        meshManager.close();

        assertThrows(IllegalStateException.class, () -> meshManager.getStatistics(),
            "Should throw exception when manager is closed");

        assertThrows(IllegalStateException.class, () -> meshManager.isHealthy(),
            "Should throw exception when manager is closed");
    }

    // Helper methods for creating test configurations

    private MeshConfig.ServiceDiscoveryConfig createServiceDiscoveryConfig() {
        return MeshConfig.ServiceDiscoveryConfig.defaultConfig();
    }

    private MeshConfig.LoadBalancingConfig createLoadBalancingConfig() {
        return MeshConfig.LoadBalancingConfig.defaultConfig();
    }

    private MeshConfig.SecurityConfig createSecurityConfig() {
        return MeshConfig.SecurityConfig.defaultConfig();
    }

    private MeshConfig.StreamingConfig createStreamingConfig() {
        return MeshConfig.StreamingConfig.defaultConfig();
    }

    private MeshConfig.CdnConfig createCdnConfig() {
        return MeshConfig.CdnConfig.defaultConfig();
    }

    private MeshConfig.AnalyticsConfig createAnalyticsConfig() {
        return MeshConfig.AnalyticsConfig.defaultConfig();
    }

    private MeshConfig.FederationConfig createFederationConfig() {
        return MeshConfig.FederationConfig.defaultConfig();
    }

    private MeshConfig.MonitoringConfig createMonitoringConfig() {
        return MeshConfig.MonitoringConfig.defaultConfig();
    }
}