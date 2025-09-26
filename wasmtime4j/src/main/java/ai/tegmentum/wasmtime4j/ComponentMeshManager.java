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

import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Component mesh networking manager for advanced distributed component operations.
 *
 * <p>The ComponentMeshManager provides a comprehensive suite of features for managing
 * WebAssembly components in a distributed, networked environment. It enables:
 *
 * <h3>Service Discovery and Load Balancing</h3>
 * <ul>
 *   <li>Automatic service registration and discovery</li>
 *   <li>Multiple load balancing strategies (round-robin, weighted, least-connections, etc.)</li>
 *   <li>Health monitoring and circuit breaking</li>
 *   <li>Failover and high availability</li>
 * </ul>
 *
 * <h3>Component Federation</h3>
 * <ul>
 *   <li>Multi-cluster component deployment</li>
 *   <li>Cross-cluster communication and coordination</li>
 *   <li>State synchronization across federated instances</li>
 *   <li>Conflict resolution and consistency guarantees</li>
 * </ul>
 *
 * <h3>Real-time Streaming</h3>
 * <ul>
 *   <li>Event-driven component communication</li>
 *   <li>Stream processing pipelines</li>
 *   <li>Backpressure management</li>
 *   <li>Real-time analytics and monitoring</li>
 * </ul>
 *
 * <h3>Content Delivery Network Integration</h3>
 * <ul>
 *   <li>Geographic content distribution</li>
 *   <li>Edge caching and optimization</li>
 *   <li>Intelligent routing based on location and load</li>
 *   <li>Content versioning and invalidation</li>
 * </ul>
 *
 * <h3>Advanced Analytics</h3>
 * <ul>
 *   <li>Performance monitoring and profiling</li>
 *   <li>Anomaly detection and alerting</li>
 *   <li>Optimization recommendations</li>
 *   <li>Business intelligence and reporting</li>
 * </ul>
 *
 * <h3>Comprehensive Security</h3>
 * <ul>
 *   <li>End-to-end encryption for component communication</li>
 *   <li>Fine-grained access control and authorization</li>
 *   <li>Security audit logging and compliance</li>
 *   <li>Threat detection and response</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Create component mesh manager
 * try (ComponentMeshManager meshManager = ComponentMeshManager.create()) {
 *     // Register a service
 *     ServiceEndpoint service = ServiceEndpoint.builder()
 *         .serviceId("user-service")
 *         .serviceName("User Management Service")
 *         .address("https://user-service.example.com")
 *         .version("1.2.0")
 *         .build();
 *     meshManager.registerService(service);
 *
 *     // Discover services
 *     ServiceDiscoveryCriteria criteria = ServiceDiscoveryCriteria.builder()
 *         .serviceName("user-service")
 *         .minVersion("1.0.0")
 *         .capability("authentication")
 *         .build();
 *     List<ServiceEndpoint> services = meshManager.discoverServices(criteria);
 *
 *     // Route a request
 *     RequestContext request = RequestContext.builder()
 *         .requestId("req-123")
 *         .clientId("web-app")
 *         .priority(RequestPriority.HIGH)
 *         .timeout(Duration.ofSeconds(30))
 *         .build();
 *     ServiceEndpoint selectedService = meshManager.routeRequest(request);
 *
 *     // Start streaming pipeline
 *     StreamPipelineConfig pipelineConfig = StreamPipelineConfig.builder()
 *         .name("user-events-pipeline")
 *         .addProcessor("filter", "event-filter", Map.of("eventType", "user-login"))
 *         .addProcessor("transform", "data-transformer", Map.of("format", "json"))
 *         .addProcessor("analytics", "analytics-engine", Map.of("window", "1m"))
 *         .build();
 *     String pipelineId = meshManager.startStreaming(pipelineConfig);
 *
 *     // Enable CDN for component
 *     CdnConfig cdnConfig = CdnConfig.builder()
 *         .addEdgeLocation("us-east-1")
 *         .addEdgeLocation("eu-west-1")
 *         .addEdgeLocation("asia-pacific-1")
 *         .cachePolicy("aggressive")
 *         .build();
 *     meshManager.enableCdn(ComponentId.of("user-service-ui"), cdnConfig);
 *
 *     // Apply security policy
 *     SecurityPolicy securityPolicy = SecurityPolicy.builder()
 *         .name("production-security")
 *         .enableEncryption(EncryptionAlgorithm.AES_256_GCM)
 *         .addAccessRule("admin", "user-service", "read,write")
 *         .addAccessRule("user", "user-service", "read")
 *         .enableAuditLogging(AuditLevel.COMPREHENSIVE)
 *         .build();
 *     meshManager.applySecurityPolicy(securityPolicy);
 *
 *     // Join federation cluster
 *     ClusterInfo clusterInfo = ClusterInfo.builder()
 *         .clusterId("production-cluster")
 *         .name("Production Environment")
 *         .version("2.1.0")
 *         .addEndpoint("https://cluster-master.example.com")
 *         .build();
 *     ClusterJoinConfig joinConfig = ClusterJoinConfig.builder()
 *         .authToken("cluster-auth-token")
 *         .secureConnection(true)
 *         .timeout(Duration.ofMinutes(5))
 *         .build();
 *     meshManager.joinCluster(clusterInfo, joinConfig).get();
 *
 *     // Get analytics
 *     ComponentAnalytics analytics = meshManager.getAnalytics(ComponentId.of("user-service"));
 *     System.out.println("Performance Score: " + analytics.getPerformanceScore());
 *     System.out.println("Anomalies Detected: " + analytics.getAnomalies().size());
 *     System.out.println("Optimization Recommendations: " + analytics.getRecommendations().size());
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ComponentMeshManager extends AutoCloseable {

    /**
     * Create a new component mesh manager instance.
     *
     * @return new component mesh manager
     * @throws WasmRuntimeException if creation fails
     */
    static ComponentMeshManager create() {
        return ComponentMeshManagerFactory.create();
    }

    /**
     * Create a new component mesh manager with configuration.
     *
     * @param config the mesh configuration
     * @return new component mesh manager
     * @throws WasmRuntimeException if creation fails
     */
    static ComponentMeshManager create(final MeshConfig config) {
        return ComponentMeshManagerFactory.create(config);
    }

    // Service Discovery and Load Balancing

    /**
     * Register a service with the mesh for discovery by other components.
     *
     * @param serviceEndpoint the service endpoint to register
     * @throws WasmRuntimeException if service registration fails
     * @throws IllegalStateException if manager is closed
     */
    void registerService(ServiceEndpoint serviceEndpoint);

    /**
     * Unregister a service from the mesh.
     *
     * @param serviceId the service identifier to unregister
     * @throws WasmRuntimeException if service unregistration fails
     * @throws IllegalStateException if manager is closed
     */
    void unregisterService(String serviceId);

    /**
     * Discover services matching the specified criteria.
     *
     * @param criteria the service discovery criteria
     * @return list of matching service endpoints
     * @throws WasmRuntimeException if service discovery fails
     * @throws IllegalStateException if manager is closed
     */
    List<ServiceEndpoint> discoverServices(ServiceDiscoveryCriteria criteria);

    /**
     * Route a request to the best available service endpoint.
     *
     * @param request the request context
     * @return selected service endpoint
     * @throws WasmRuntimeException if request routing fails
     * @throws IllegalStateException if manager is closed
     */
    ServiceEndpoint routeRequest(RequestContext request);

    /**
     * Update the health status of a service.
     *
     * @param serviceId the service identifier
     * @param healthStatus the new health status
     * @throws WasmRuntimeException if health status update fails
     * @throws IllegalStateException if manager is closed
     */
    void updateServiceHealth(String serviceId, ServiceHealthStatus healthStatus);

    // Real-time Streaming and Event Processing

    /**
     * Start a streaming pipeline for real-time event processing.
     *
     * @param pipelineConfig the streaming pipeline configuration
     * @return unique pipeline identifier
     * @throws WasmRuntimeException if pipeline start fails
     * @throws IllegalStateException if manager is closed
     */
    String startStreaming(StreamPipelineConfig pipelineConfig);

    /**
     * Stop a streaming pipeline.
     *
     * @param pipelineId the pipeline identifier
     * @throws WasmRuntimeException if pipeline stop fails
     * @throws IllegalStateException if manager is closed
     */
    void stopStreaming(String pipelineId);

    /**
     * Process a streaming event through the specified pipeline.
     *
     * @param pipelineId the pipeline identifier
     * @param event the stream event to process
     * @throws WasmRuntimeException if event processing fails
     * @throws IllegalStateException if manager is closed
     */
    void processEvent(String pipelineId, StreamEvent event);

    /**
     * Get streaming pipeline statistics.
     *
     * @param pipelineId the pipeline identifier
     * @return pipeline statistics
     * @throws WasmRuntimeException if statistics retrieval fails
     * @throws IllegalStateException if manager is closed
     */
    StreamingStatistics getStreamingStatistics(String pipelineId);

    // Component Federation

    /**
     * Join a federation cluster to enable cross-cluster component operations.
     *
     * @param clusterInfo the cluster information
     * @param joinConfig the join configuration
     * @return future that completes when cluster join is finished
     */
    CompletableFuture<Void> joinCluster(ClusterInfo clusterInfo, ClusterJoinConfig joinConfig);

    /**
     * Leave a federation cluster.
     *
     * @param clusterId the cluster identifier
     * @return future that completes when cluster leave is finished
     */
    CompletableFuture<Void> leaveCluster(String clusterId);

    /**
     * Federate a component across multiple clusters.
     *
     * @param componentId the component identifier
     * @param federationConfig the federation configuration
     * @throws WasmRuntimeException if component federation fails
     * @throws IllegalStateException if manager is closed
     */
    void federateComponent(ComponentId componentId, ComponentFederationConfig federationConfig);

    /**
     * Remove component federation.
     *
     * @param componentId the component identifier
     * @throws WasmRuntimeException if federation removal fails
     * @throws IllegalStateException if manager is closed
     */
    void unfederateComponent(ComponentId componentId);

    /**
     * Get federation status for a component.
     *
     * @param componentId the component identifier
     * @return federation status
     * @throws WasmRuntimeException if status retrieval fails
     * @throws IllegalStateException if manager is closed
     */
    FederationStatus getFederationStatus(ComponentId componentId);

    // Content Delivery Network Integration

    /**
     * Enable CDN for a component to improve content delivery performance.
     *
     * @param componentId the component identifier
     * @param cdnConfig the CDN configuration
     * @throws WasmRuntimeException if CDN enablement fails
     * @throws IllegalStateException if manager is closed
     */
    void enableCdn(ComponentId componentId, CdnConfig cdnConfig);

    /**
     * Disable CDN for a component.
     *
     * @param componentId the component identifier
     * @throws WasmRuntimeException if CDN disablement fails
     * @throws IllegalStateException if manager is closed
     */
    void disableCdn(ComponentId componentId);

    /**
     * Cache component content at edge locations.
     *
     * @param componentId the component identifier
     * @param contentKey the content key
     * @param content the content data
     * @throws WasmRuntimeException if content caching fails
     * @throws IllegalStateException if manager is closed
     */
    void cacheContent(ComponentId componentId, String contentKey, byte[] content);

    /**
     * Retrieve cached content from edge locations.
     *
     * @param componentId the component identifier
     * @param contentKey the content key
     * @return cached content, or null if not found
     * @throws WasmRuntimeException if content retrieval fails
     * @throws IllegalStateException if manager is closed
     */
    byte[] getCachedContent(ComponentId componentId, String contentKey);

    /**
     * Invalidate cached content at all edge locations.
     *
     * @param componentId the component identifier
     * @param contentKey the content key
     * @throws WasmRuntimeException if content invalidation fails
     * @throws IllegalStateException if manager is closed
     */
    void invalidateContent(ComponentId componentId, String contentKey);

    /**
     * Get CDN statistics for a component.
     *
     * @param componentId the component identifier
     * @return CDN statistics
     * @throws WasmRuntimeException if statistics retrieval fails
     * @throws IllegalStateException if manager is closed
     */
    CdnStatistics getCdnStatistics(ComponentId componentId);

    // Advanced Analytics and Monitoring

    /**
     * Get comprehensive analytics for a component.
     *
     * @param componentId the component identifier
     * @return component analytics
     * @throws WasmRuntimeException if analytics retrieval fails
     * @throws IllegalStateException if manager is closed
     */
    ComponentAnalytics getAnalytics(ComponentId componentId);

    /**
     * Get mesh-wide analytics and statistics.
     *
     * @return mesh analytics
     * @throws WasmRuntimeException if analytics retrieval fails
     * @throws IllegalStateException if manager is closed
     */
    MeshAnalytics getMeshAnalytics();

    /**
     * Configure performance monitoring for a component.
     *
     * @param componentId the component identifier
     * @param monitoringConfig the monitoring configuration
     * @throws WasmRuntimeException if monitoring configuration fails
     * @throws IllegalStateException if manager is closed
     */
    void configureMonitoring(ComponentId componentId, MonitoringConfig monitoringConfig);

    /**
     * Get performance optimization recommendations for a component.
     *
     * @param componentId the component identifier
     * @return optimization recommendations
     * @throws WasmRuntimeException if recommendations retrieval fails
     * @throws IllegalStateException if manager is closed
     */
    List<OptimizationRecommendation> getOptimizationRecommendations(ComponentId componentId);

    // Comprehensive Security

    /**
     * Apply a security policy to the mesh or specific components.
     *
     * @param policy the security policy to apply
     * @throws WasmRuntimeException if security policy application fails
     * @throws IllegalStateException if manager is closed
     */
    void applySecurityPolicy(SecurityPolicy policy);

    /**
     * Remove a security policy from the mesh.
     *
     * @param policyName the policy name to remove
     * @throws WasmRuntimeException if security policy removal fails
     * @throws IllegalStateException if manager is closed
     */
    void removeSecurityPolicy(String policyName);

    /**
     * Encrypt data using the mesh encryption system.
     *
     * @param data the data to encrypt
     * @param encryptionContext the encryption context
     * @return encrypted data
     * @throws WasmRuntimeException if encryption fails
     * @throws IllegalStateException if manager is closed
     */
    byte[] encryptData(byte[] data, EncryptionContext encryptionContext);

    /**
     * Decrypt data using the mesh encryption system.
     *
     * @param encryptedData the encrypted data
     * @param decryptionContext the decryption context
     * @return decrypted data
     * @throws WasmRuntimeException if decryption fails
     * @throws IllegalStateException if manager is closed
     */
    byte[] decryptData(byte[] encryptedData, DecryptionContext decryptionContext);

    /**
     * Get security audit events.
     *
     * @param criteria the audit criteria
     * @return security audit events
     * @throws WasmRuntimeException if audit retrieval fails
     * @throws IllegalStateException if manager is closed
     */
    List<SecurityAuditEvent> getAuditEvents(AuditCriteria criteria);

    /**
     * Get compliance status for configured requirements.
     *
     * @return compliance status report
     * @throws WasmRuntimeException if compliance check fails
     * @throws IllegalStateException if manager is closed
     */
    ComplianceStatus getComplianceStatus();

    // General Operations

    /**
     * Get overall mesh statistics and health information.
     *
     * @return mesh statistics
     * @throws WasmRuntimeException if statistics retrieval fails
     * @throws IllegalStateException if manager is closed
     */
    MeshStatistics getStatistics();

    /**
     * Get the current mesh configuration.
     *
     * @return mesh configuration
     * @throws IllegalStateException if manager is closed
     */
    MeshConfig getConfiguration();

    /**
     * Update the mesh configuration.
     *
     * @param config the new configuration
     * @throws WasmRuntimeException if configuration update fails
     * @throws IllegalStateException if manager is closed
     */
    void updateConfiguration(MeshConfig config);

    /**
     * Check if the mesh manager is healthy and operational.
     *
     * @return true if healthy, false otherwise
     * @throws IllegalStateException if manager is closed
     */
    boolean isHealthy();

    /**
     * Close the component mesh manager and release all resources.
     */
    @Override
    void close();
}