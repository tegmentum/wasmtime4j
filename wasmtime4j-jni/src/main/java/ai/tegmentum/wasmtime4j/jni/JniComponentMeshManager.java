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

package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.ComponentId;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * JNI implementation of component mesh networking manager.
 *
 * <p>This class provides comprehensive component mesh networking capabilities including:
 *
 * <ul>
 *   <li>Service discovery and registration
 *   <li>Load balancing and failover
 *   <li>Component federation across clusters
 *   <li>Real-time streaming and analytics
 *   <li>Content delivery network integration
 *   <li>End-to-end security and compliance
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniComponentMeshManager implements AutoCloseable {
  private static final Logger logger = Logger.getLogger(JniComponentMeshManager.class.getName());

  private final long nativeHandle;
  private volatile boolean closed = false;

  static {
    JniLoader.loadNativeLibrary();
  }

  /**
   * Create a new component mesh manager.
   *
   * @throws WasmRuntimeException if mesh manager creation fails
   */
  public JniComponentMeshManager() {
    this.nativeHandle = nativeCreate();
    if (this.nativeHandle == 0) {
      throw new WasmRuntimeException("Failed to create component mesh manager");
    }
    logger.fine("Created JniComponentMeshManager with handle: " + nativeHandle);
  }

  /**
   * Register a service with the mesh.
   *
   * @param serviceEndpoint the service endpoint to register
   * @throws WasmRuntimeException if service registration fails
   * @throws IllegalStateException if manager is closed
   */
  public void registerService(final ServiceEndpoint serviceEndpoint) {
    JniValidation.requireNonNull(serviceEndpoint, "serviceEndpoint");
    ensureNotClosed();

    final boolean success = nativeRegisterService(nativeHandle, serviceEndpoint);
    if (!success) {
      throw new WasmRuntimeException(
          "Failed to register service: " + serviceEndpoint.getServiceId());
    }
    logger.fine("Registered service: " + serviceEndpoint.getServiceId());
  }

  /**
   * Discover services matching criteria.
   *
   * @param criteria the service discovery criteria
   * @return list of matching service endpoints
   * @throws WasmRuntimeException if service discovery fails
   * @throws IllegalStateException if manager is closed
   */
  public List<ServiceEndpoint> discoverServices(final ServiceDiscoveryCriteria criteria) {
    JniValidation.requireNonNull(criteria, "criteria");
    ensureNotClosed();

    final List<ServiceEndpoint> services = nativeDiscoverServices(nativeHandle, criteria);
    if (services == null) {
      throw new WasmRuntimeException("Failed to discover services");
    }
    logger.fine("Discovered " + services.size() + " services");
    return services;
  }

  /**
   * Route request to best available service.
   *
   * @param request the request context
   * @return selected service endpoint
   * @throws WasmRuntimeException if request routing fails
   * @throws IllegalStateException if manager is closed
   */
  public ServiceEndpoint routeRequest(final RequestContext request) {
    JniValidation.requireNonNull(request, "request");
    ensureNotClosed();

    final ServiceEndpoint endpoint = nativeRouteRequest(nativeHandle, request);
    if (endpoint == null) {
      throw new WasmRuntimeException("Failed to route request: " + request.getRequestId());
    }
    logger.fine(
        "Routed request " + request.getRequestId() + " to service " + endpoint.getServiceId());
    return endpoint;
  }

  /**
   * Start a streaming pipeline.
   *
   * @param pipelineConfig the streaming pipeline configuration
   * @return pipeline identifier
   * @throws WasmRuntimeException if pipeline start fails
   * @throws IllegalStateException if manager is closed
   */
  public String startStreaming(final StreamPipelineConfig pipelineConfig) {
    JniValidation.requireNonNull(pipelineConfig, "pipelineConfig");
    ensureNotClosed();

    final String pipelineId = nativeStartStreaming(nativeHandle, pipelineConfig);
    if (pipelineId == null || pipelineId.isEmpty()) {
      throw new WasmRuntimeException(
          "Failed to start streaming pipeline: " + pipelineConfig.getName());
    }
    logger.info("Started streaming pipeline: " + pipelineId);
    return pipelineId;
  }

  /**
   * Stop a streaming pipeline.
   *
   * @param pipelineId the pipeline identifier
   * @throws WasmRuntimeException if pipeline stop fails
   * @throws IllegalStateException if manager is closed
   */
  public void stopStreaming(final String pipelineId) {
    JniValidation.requireNonNull(pipelineId, "pipelineId");
    ensureNotClosed();

    final boolean success = nativeStopStreaming(nativeHandle, pipelineId);
    if (!success) {
      throw new WasmRuntimeException("Failed to stop streaming pipeline: " + pipelineId);
    }
    logger.info("Stopped streaming pipeline: " + pipelineId);
  }

  /**
   * Enable CDN for a component.
   *
   * @param componentId the component identifier
   * @param cdnConfig the CDN configuration
   * @throws WasmRuntimeException if CDN enablement fails
   * @throws IllegalStateException if manager is closed
   */
  public void enableCdn(final ComponentId componentId, final CdnConfig cdnConfig) {
    JniValidation.requireNonNull(componentId, "componentId");
    JniValidation.requireNonNull(cdnConfig, "cdnConfig");
    ensureNotClosed();

    final boolean success = nativeEnableCdn(nativeHandle, componentId.toString(), cdnConfig);
    if (!success) {
      throw new WasmRuntimeException("Failed to enable CDN for component: " + componentId);
    }
    logger.info("Enabled CDN for component: " + componentId);
  }

  /**
   * Get component analytics.
   *
   * @param componentId the component identifier
   * @return component analytics
   * @throws WasmRuntimeException if analytics retrieval fails
   * @throws IllegalStateException if manager is closed
   */
  public ComponentAnalytics getAnalytics(final ComponentId componentId) {
    JniValidation.requireNonNull(componentId, "componentId");
    ensureNotClosed();

    final ComponentAnalytics analytics = nativeGetAnalytics(nativeHandle, componentId.toString());
    if (analytics == null) {
      throw new WasmRuntimeException("Failed to get analytics for component: " + componentId);
    }
    logger.fine("Retrieved analytics for component: " + componentId);
    return analytics;
  }

  /**
   * Apply security policy.
   *
   * @param policy the security policy to apply
   * @throws WasmRuntimeException if security policy application fails
   * @throws IllegalStateException if manager is closed
   */
  public void applySecurityPolicy(final SecurityPolicy policy) {
    JniValidation.requireNonNull(policy, "policy");
    ensureNotClosed();

    final boolean success = nativeApplySecurityPolicy(nativeHandle, policy);
    if (!success) {
      throw new WasmRuntimeException("Failed to apply security policy: " + policy.getName());
    }
    logger.info("Applied security policy: " + policy.getName());
  }

  /**
   * Join a federation cluster.
   *
   * @param clusterInfo the cluster information
   * @param joinConfig the join configuration
   * @return future that completes when cluster join is finished
   */
  public CompletableFuture<Void> joinCluster(
      final ClusterInfo clusterInfo, final ClusterJoinConfig joinConfig) {
    JniValidation.requireNonNull(clusterInfo, "clusterInfo");
    JniValidation.requireNonNull(joinConfig, "joinConfig");
    ensureNotClosed();

    return CompletableFuture.runAsync(
        () -> {
          final boolean success = nativeJoinCluster(nativeHandle, clusterInfo, joinConfig);
          if (!success) {
            throw new WasmRuntimeException("Failed to join cluster: " + clusterInfo.getClusterId());
          }
          logger.info("Joined cluster: " + clusterInfo.getClusterId());
        });
  }

  /**
   * Federate component across clusters.
   *
   * @param componentId the component identifier
   * @param federationConfig the federation configuration
   * @throws WasmRuntimeException if component federation fails
   * @throws IllegalStateException if manager is closed
   */
  public void federateComponent(
      final ComponentId componentId, final ComponentFederationConfig federationConfig) {
    JniValidation.requireNonNull(componentId, "componentId");
    JniValidation.requireNonNull(federationConfig, "federationConfig");
    ensureNotClosed();

    final boolean success =
        nativeFederateComponent(nativeHandle, componentId.toString(), federationConfig);
    if (!success) {
      throw new WasmRuntimeException("Failed to federate component: " + componentId);
    }
    logger.info("Federated component: " + componentId);
  }

  /**
   * Process streaming event.
   *
   * @param pipelineId the pipeline identifier
   * @param event the stream event
   * @throws WasmRuntimeException if event processing fails
   * @throws IllegalStateException if manager is closed
   */
  public void processEvent(final String pipelineId, final StreamEvent event) {
    JniValidation.requireNonNull(pipelineId, "pipelineId");
    JniValidation.requireNonNull(event, "event");
    ensureNotClosed();

    final boolean success = nativeProcessEvent(nativeHandle, pipelineId, event);
    if (!success) {
      throw new WasmRuntimeException("Failed to process event in pipeline: " + pipelineId);
    }
    logger.finest("Processed event " + event.getEventId() + " in pipeline " + pipelineId);
  }

  /**
   * Cache component content.
   *
   * @param componentId the component identifier
   * @param contentKey the content key
   * @param content the content data
   * @throws WasmRuntimeException if content caching fails
   * @throws IllegalStateException if manager is closed
   */
  public void cacheContent(
      final ComponentId componentId, final String contentKey, final byte[] content) {
    JniValidation.requireNonNull(componentId, "componentId");
    JniValidation.requireNonNull(contentKey, "contentKey");
    JniValidation.requireNonNull(content, "content");
    ensureNotClosed();

    final boolean success =
        nativeCacheContent(nativeHandle, componentId.toString(), contentKey, content);
    if (!success) {
      throw new WasmRuntimeException("Failed to cache content for component: " + componentId);
    }
    logger.fine("Cached content " + contentKey + " for component " + componentId);
  }

  /**
   * Retrieve cached content.
   *
   * @param componentId the component identifier
   * @param contentKey the content key
   * @return cached content, or null if not found
   * @throws WasmRuntimeException if content retrieval fails
   * @throws IllegalStateException if manager is closed
   */
  public byte[] getCachedContent(final ComponentId componentId, final String contentKey) {
    JniValidation.requireNonNull(componentId, "componentId");
    JniValidation.requireNonNull(contentKey, "contentKey");
    ensureNotClosed();

    final byte[] content = nativeGetCachedContent(nativeHandle, componentId.toString(), contentKey);
    if (content != null) {
      logger.fine("Retrieved cached content " + contentKey + " for component " + componentId);
    }
    return content;
  }

  /**
   * Get mesh statistics.
   *
   * @return mesh statistics
   * @throws WasmRuntimeException if statistics retrieval fails
   * @throws IllegalStateException if manager is closed
   */
  public MeshStatistics getStatistics() {
    ensureNotClosed();

    final MeshStatistics statistics = nativeGetStatistics(nativeHandle);
    if (statistics == null) {
      throw new WasmRuntimeException("Failed to get mesh statistics");
    }
    return statistics;
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      nativeClose(nativeHandle);
      logger.fine("Closed JniComponentMeshManager with handle: " + nativeHandle);
    }
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("ComponentMeshManager is closed");
    }
  }

  // Native method declarations
  private static native long nativeCreate();

  private static native boolean nativeRegisterService(long handle, ServiceEndpoint serviceEndpoint);

  private static native List<ServiceEndpoint> nativeDiscoverServices(
      long handle, ServiceDiscoveryCriteria criteria);

  private static native ServiceEndpoint nativeRouteRequest(long handle, RequestContext request);

  private static native String nativeStartStreaming(
      long handle, StreamPipelineConfig pipelineConfig);

  private static native boolean nativeStopStreaming(long handle, String pipelineId);

  private static native boolean nativeEnableCdn(
      long handle, String componentId, CdnConfig cdnConfig);

  private static native ComponentAnalytics nativeGetAnalytics(long handle, String componentId);

  private static native boolean nativeApplySecurityPolicy(long handle, SecurityPolicy policy);

  private static native boolean nativeJoinCluster(
      long handle, ClusterInfo clusterInfo, ClusterJoinConfig joinConfig);

  private static native boolean nativeFederateComponent(
      long handle, String componentId, ComponentFederationConfig federationConfig);

  private static native boolean nativeProcessEvent(
      long handle, String pipelineId, StreamEvent event);

  private static native boolean nativeCacheContent(
      long handle, String componentId, String contentKey, byte[] content);

  private static native byte[] nativeGetCachedContent(
      long handle, String componentId, String contentKey);

  private static native MeshStatistics nativeGetStatistics(long handle);

  private static native void nativeClose(long handle);

  /** Service endpoint information for component mesh. */
  public static final class ServiceEndpoint {
    private final String serviceId;
    private final String serviceName;
    private final List<String> addresses;
    private final Map<String, String> metadata;
    private final ServiceHealthStatus healthStatus;
    private final double weight;
    private final String version;

    /**
     * Creates a new service endpoint.
     *
     * @param serviceId the service ID
     * @param serviceName the service name
     * @param addresses list of service addresses
     * @param metadata service metadata
     * @param healthStatus health status of the service
     * @param weight routing weight
     * @param version service version
     */
    public ServiceEndpoint(
        final String serviceId,
        final String serviceName,
        final List<String> addresses,
        final Map<String, String> metadata,
        final ServiceHealthStatus healthStatus,
        final double weight,
        final String version) {
      this.serviceId = JniValidation.requireNonNull(serviceId, "serviceId");
      this.serviceName = JniValidation.requireNonNull(serviceName, "serviceName");
      this.addresses = JniValidation.requireNonNull(addresses, "addresses");
      this.metadata = JniValidation.requireNonNull(metadata, "metadata");
      this.healthStatus = JniValidation.requireNonNull(healthStatus, "healthStatus");
      this.weight = weight;
      this.version = JniValidation.requireNonNull(version, "version");
    }

    public String getServiceId() {
      return serviceId;
    }

    public String getServiceName() {
      return serviceName;
    }

    public List<String> getAddresses() {
      return addresses;
    }

    public Map<String, String> getMetadata() {
      return metadata;
    }

    public ServiceHealthStatus getHealthStatus() {
      return healthStatus;
    }

    public double getWeight() {
      return weight;
    }

    public String getVersion() {
      return version;
    }
  }

  /** Health status of a service endpoint. */
  public enum ServiceHealthStatus {
    HEALTHY,
    DEGRADED,
    UNHEALTHY,
    UNKNOWN,
    STARTING,
    STOPPING
  }

  /** Criteria for discovering services in the mesh. */
  public static final class ServiceDiscoveryCriteria {
    private final String serviceName;
    private final List<String> protocols;
    private final String minVersion;
    private final List<String> capabilities;

    /**
     * Creates service discovery criteria.
     *
     * @param serviceName the service name to discover
     * @param protocols supported protocols
     * @param minVersion minimum version requirement
     * @param capabilities required capabilities
     */
    public ServiceDiscoveryCriteria(
        final String serviceName,
        final List<String> protocols,
        final String minVersion,
        final List<String> capabilities) {
      this.serviceName = serviceName;
      this.protocols = protocols;
      this.minVersion = minVersion;
      this.capabilities = capabilities;
    }

    public String getServiceName() {
      return serviceName;
    }

    public List<String> getProtocols() {
      return protocols;
    }

    public String getMinVersion() {
      return minVersion;
    }

    public List<String> getCapabilities() {
      return capabilities;
    }
  }

  /** Context information for a mesh request. */
  public static final class RequestContext {
    private final String requestId;
    private final String clientId;
    private final Map<String, String> metadata;
    private final RequestPriority priority;
    private final long timeoutMs;

    /**
     * Creates request context.
     *
     * @param requestId the request ID
     * @param clientId the client ID
     * @param metadata request metadata
     * @param priority request priority
     * @param timeoutMs timeout in milliseconds
     */
    public RequestContext(
        final String requestId,
        final String clientId,
        final Map<String, String> metadata,
        final RequestPriority priority,
        final long timeoutMs) {
      this.requestId = JniValidation.requireNonNull(requestId, "requestId");
      this.clientId = JniValidation.requireNonNull(clientId, "clientId");
      this.metadata = JniValidation.requireNonNull(metadata, "metadata");
      this.priority = JniValidation.requireNonNull(priority, "priority");
      this.timeoutMs = timeoutMs;
    }

    public String getRequestId() {
      return requestId;
    }

    public String getClientId() {
      return clientId;
    }

    public Map<String, String> getMetadata() {
      return metadata;
    }

    public RequestPriority getPriority() {
      return priority;
    }

    public long getTimeoutMs() {
      return timeoutMs;
    }
  }

  /** Priority levels for mesh requests. */
  public enum RequestPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL,
    EMERGENCY
  }

  /** Configuration for a stream processing pipeline. */
  public static final class StreamPipelineConfig {
    private final String name;
    private final List<StreamProcessorConfig> processors;
    private final Map<String, String> routingConfig;

    /**
     * Creates stream pipeline configuration.
     *
     * @param name the pipeline name
     * @param processors list of stream processors
     * @param routingConfig routing configuration
     */
    public StreamPipelineConfig(
        final String name,
        final List<StreamProcessorConfig> processors,
        final Map<String, String> routingConfig) {
      this.name = JniValidation.requireNonNull(name, "name");
      this.processors = JniValidation.requireNonNull(processors, "processors");
      this.routingConfig = JniValidation.requireNonNull(routingConfig, "routingConfig");
    }

    public String getName() {
      return name;
    }

    public List<StreamProcessorConfig> getProcessors() {
      return processors;
    }

    public Map<String, String> getRoutingConfig() {
      return routingConfig;
    }
  }

  /** Configuration for a stream processor. */
  public static final class StreamProcessorConfig {
    private final String name;
    private final String processorType;
    private final Map<String, String> parameters;

    /**
     * Creates stream processor configuration.
     *
     * @param name the processor name
     * @param processorType the processor type
     * @param parameters processor parameters
     */
    public StreamProcessorConfig(
        final String name, final String processorType, final Map<String, String> parameters) {
      this.name = JniValidation.requireNonNull(name, "name");
      this.processorType = JniValidation.requireNonNull(processorType, "processorType");
      this.parameters = JniValidation.requireNonNull(parameters, "parameters");
    }

    public String getName() {
      return name;
    }

    public String getProcessorType() {
      return processorType;
    }

    public Map<String, String> getParameters() {
      return parameters;
    }
  }

  /** CDN configuration for content delivery. */
  public static final class CdnConfig {
    private final List<String> edgeLocations;
    private final Map<String, Object> cachePolicy;
    private final boolean enabled;

    /**
     * Creates CDN configuration.
     *
     * @param edgeLocations list of edge locations
     * @param cachePolicy cache policy configuration
     * @param enabled whether CDN is enabled
     */
    public CdnConfig(
        final List<String> edgeLocations,
        final Map<String, Object> cachePolicy,
        final boolean enabled) {
      this.edgeLocations = JniValidation.requireNonNull(edgeLocations, "edgeLocations");
      this.cachePolicy = JniValidation.requireNonNull(cachePolicy, "cachePolicy");
      this.enabled = enabled;
    }

    public List<String> getEdgeLocations() {
      return edgeLocations;
    }

    public Map<String, Object> getCachePolicy() {
      return cachePolicy;
    }

    public boolean isEnabled() {
      return enabled;
    }
  }

  /** Component analytics information. */
  public static final class ComponentAnalytics {
    private final String componentId;
    private final Map<String, Object> performanceMetrics;
    private final List<String> anomalies;
    private final List<String> recommendations;

    /**
     * Creates component analytics.
     *
     * @param componentId the component ID
     * @param performanceMetrics performance metrics map
     * @param anomalies detected anomalies
     * @param recommendations optimization recommendations
     */
    public ComponentAnalytics(
        final String componentId,
        final Map<String, Object> performanceMetrics,
        final List<String> anomalies,
        final List<String> recommendations) {
      this.componentId = JniValidation.requireNonNull(componentId, "componentId");
      this.performanceMetrics =
          JniValidation.requireNonNull(performanceMetrics, "performanceMetrics");
      this.anomalies = JniValidation.requireNonNull(anomalies, "anomalies");
      this.recommendations = JniValidation.requireNonNull(recommendations, "recommendations");
    }

    public String getComponentId() {
      return componentId;
    }

    public Map<String, Object> getPerformanceMetrics() {
      return performanceMetrics;
    }

    public List<String> getAnomalies() {
      return anomalies;
    }

    public List<String> getRecommendations() {
      return recommendations;
    }
  }

  /** Security policy configuration. */
  public static final class SecurityPolicy {
    private final String name;
    private final Map<String, Object> encryptionConfig;
    private final List<String> accessControlRules;
    private final boolean enabled;

    /**
     * Creates security policy.
     *
     * @param name policy name
     * @param encryptionConfig encryption configuration
     * @param accessControlRules access control rules
     * @param enabled whether policy is enabled
     */
    public SecurityPolicy(
        final String name,
        final Map<String, Object> encryptionConfig,
        final List<String> accessControlRules,
        final boolean enabled) {
      this.name = JniValidation.requireNonNull(name, "name");
      this.encryptionConfig = JniValidation.requireNonNull(encryptionConfig, "encryptionConfig");
      this.accessControlRules =
          JniValidation.requireNonNull(accessControlRules, "accessControlRules");
      this.enabled = enabled;
    }

    public String getName() {
      return name;
    }

    public Map<String, Object> getEncryptionConfig() {
      return encryptionConfig;
    }

    public List<String> getAccessControlRules() {
      return accessControlRules;
    }

    public boolean isEnabled() {
      return enabled;
    }
  }

  /** Cluster information for federation. */
  public static final class ClusterInfo {
    private final String clusterId;
    private final String name;
    private final String version;
    private final List<String> endpoints;

    /**
     * Creates cluster information.
     *
     * @param clusterId the cluster ID
     * @param name cluster name
     * @param version cluster version
     * @param endpoints cluster endpoints
     */
    public ClusterInfo(
        final String clusterId,
        final String name,
        final String version,
        final List<String> endpoints) {
      this.clusterId = JniValidation.requireNonNull(clusterId, "clusterId");
      this.name = JniValidation.requireNonNull(name, "name");
      this.version = JniValidation.requireNonNull(version, "version");
      this.endpoints = JniValidation.requireNonNull(endpoints, "endpoints");
    }

    public String getClusterId() {
      return clusterId;
    }

    public String getName() {
      return name;
    }

    public String getVersion() {
      return version;
    }

    public List<String> getEndpoints() {
      return endpoints;
    }
  }

  /** Configuration for joining a cluster. */
  public static final class ClusterJoinConfig {
    private final String authToken;
    private final boolean secureConnection;
    private final long timeoutMs;

    /**
     * Creates cluster join configuration.
     *
     * @param authToken authentication token
     * @param secureConnection whether to use secure connection
     * @param timeoutMs connection timeout in milliseconds
     */
    public ClusterJoinConfig(
        final String authToken, final boolean secureConnection, final long timeoutMs) {
      this.authToken = JniValidation.requireNonNull(authToken, "authToken");
      this.secureConnection = secureConnection;
      this.timeoutMs = timeoutMs;
    }

    public String getAuthToken() {
      return authToken;
    }

    public boolean isSecureConnection() {
      return secureConnection;
    }

    public long getTimeoutMs() {
      return timeoutMs;
    }
  }

  /** Configuration for component federation. */
  public static final class ComponentFederationConfig {
    private final List<String> targetClusters;
    private final String replicationStrategy;
    private final String consistencyLevel;

    /**
     * Creates component federation configuration.
     *
     * @param targetClusters list of target clusters
     * @param replicationStrategy replication strategy
     * @param consistencyLevel consistency level
     */
    public ComponentFederationConfig(
        final List<String> targetClusters,
        final String replicationStrategy,
        final String consistencyLevel) {
      this.targetClusters = JniValidation.requireNonNull(targetClusters, "targetClusters");
      this.replicationStrategy =
          JniValidation.requireNonNull(replicationStrategy, "replicationStrategy");
      this.consistencyLevel = JniValidation.requireNonNull(consistencyLevel, "consistencyLevel");
    }

    public List<String> getTargetClusters() {
      return targetClusters;
    }

    public String getReplicationStrategy() {
      return replicationStrategy;
    }

    public String getConsistencyLevel() {
      return consistencyLevel;
    }
  }

  /** Stream event information. */
  public static final class StreamEvent {
    private final String eventId;
    private final String eventType;
    private final long timestamp;
    private final byte[] payload;
    private final Map<String, String> metadata;

    /**
     * Creates stream event.
     *
     * @param eventId the event ID
     * @param eventType event type
     * @param timestamp event timestamp
     * @param payload event payload
     * @param metadata event metadata
     */
    public StreamEvent(
        final String eventId,
        final String eventType,
        final long timestamp,
        final byte[] payload,
        final Map<String, String> metadata) {
      this.eventId = JniValidation.requireNonNull(eventId, "eventId");
      this.eventType = JniValidation.requireNonNull(eventType, "eventType");
      this.timestamp = timestamp;
      this.payload = JniValidation.requireNonNull(payload, "payload");
      this.metadata = JniValidation.requireNonNull(metadata, "metadata");
    }

    public String getEventId() {
      return eventId;
    }

    public String getEventType() {
      return eventType;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public byte[] getPayload() {
      return payload;
    }

    public Map<String, String> getMetadata() {
      return metadata;
    }
  }

  /** Statistics for mesh operations. */
  public static final class MeshStatistics {
    private final long totalServices;
    private final long activeServices;
    private final double avgResponseTime;
    private final double errorRate;
    private final long totalRequests;

    /**
     * Creates mesh statistics.
     *
     * @param totalServices total number of services
     * @param activeServices number of active services
     * @param avgResponseTime average response time
     * @param errorRate error rate
     * @param totalRequests total number of requests
     */
    public MeshStatistics(
        final long totalServices,
        final long activeServices,
        final double avgResponseTime,
        final double errorRate,
        final long totalRequests) {
      this.totalServices = totalServices;
      this.activeServices = activeServices;
      this.avgResponseTime = avgResponseTime;
      this.errorRate = errorRate;
      this.totalRequests = totalRequests;
    }

    public long getTotalServices() {
      return totalServices;
    }

    public long getActiveServices() {
      return activeServices;
    }

    public double getAvgResponseTime() {
      return avgResponseTime;
    }

    public double getErrorRate() {
      return errorRate;
    }

    public long getTotalRequests() {
      return totalRequests;
    }
  }
}
