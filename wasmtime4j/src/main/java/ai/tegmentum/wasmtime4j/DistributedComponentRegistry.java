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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Distributed component registry supporting multi-node component discovery and management.
 *
 * <p>The DistributedComponentRegistry extends the basic ComponentRegistry interface to support
 * distributed scenarios including:
 * <ul>
 *   <li>Multi-node component registration and discovery</li>
 *   <li>Component caching and replication</li>
 *   <li>Distributed dependency resolution</li>
 *   <li>Node health monitoring and failover</li>
 *   <li>Component security and signing verification</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface DistributedComponentRegistry extends ComponentRegistry {

  /**
   * Gets the registry node identifier.
   *
   * @return the node ID
   */
  String getNodeId();

  /**
   * Gets the distributed registry configuration.
   *
   * @return the registry configuration
   */
  DistributedRegistryConfig getConfiguration();

  /**
   * Registers a component locally and propagates to other nodes.
   *
   * @param component the component to register
   * @param propagationConfig propagation configuration
   * @return registration result future
   * @throws WasmException if registration fails
   */
  CompletableFuture<DistributedRegistrationResult> registerDistributed(ComponentSimple component,
                                                                       PropagationConfig propagationConfig) throws WasmException;

  /**
   * Searches for components across all registry nodes.
   *
   * @param criteria search criteria
   * @param searchConfig distributed search configuration
   * @return future containing search results from all nodes
   * @throws WasmException if search fails
   */
  CompletableFuture<DistributedSearchResult> searchDistributed(ComponentSearchCriteria criteria,
                                                              DistributedSearchConfig searchConfig) throws WasmException;

  /**
   * Downloads a component from a remote registry node.
   *
   * @param componentId the component ID to download
   * @param sourceNode the source node to download from
   * @param downloadConfig download configuration
   * @return future containing the downloaded component
   * @throws WasmException if download fails
   */
  CompletableFuture<ComponentSimple> downloadComponent(String componentId, String sourceNode,
                                                      ComponentDownloadConfig downloadConfig) throws WasmException;

  /**
   * Synchronizes components with other registry nodes.
   *
   * @param syncConfig synchronization configuration
   * @return future containing synchronization result
   * @throws WasmException if synchronization fails
   */
  CompletableFuture<SynchronizationResult> synchronize(SynchronizationConfig syncConfig) throws WasmException;

  /**
   * Gets all connected registry nodes.
   *
   * @return list of connected node information
   */
  List<RegistryNode> getConnectedNodes();

  /**
   * Gets the health status of a registry node.
   *
   * @param nodeId the node ID to check
   * @return node health status
   */
  Optional<NodeHealthStatus> getNodeHealth(String nodeId);

  /**
   * Adds a new registry node to the distributed system.
   *
   * @param nodeInfo information about the node to add
   * @return future that completes when the node is added
   * @throws WasmException if node addition fails
   */
  CompletableFuture<Void> addNode(RegistryNode nodeInfo) throws WasmException;

  /**
   * Removes a registry node from the distributed system.
   *
   * @param nodeId the ID of the node to remove
   * @return future that completes when the node is removed
   * @throws WasmException if node removal fails
   */
  CompletableFuture<Void> removeNode(String nodeId) throws WasmException;

  /**
   * Gets component metadata with distribution information.
   *
   * @param componentId the component ID
   * @return distributed component metadata
   */
  Optional<DistributedComponentMetadata> getDistributedMetadata(String componentId);

  /**
   * Caches a component locally for faster access.
   *
   * @param component the component to cache
   * @param cacheConfig cache configuration
   * @throws WasmException if caching fails
   */
  void cacheComponent(ComponentSimple component, CacheConfig cacheConfig) throws WasmException;

  /**
   * Evicts a component from the local cache.
   *
   * @param componentId the component ID to evict
   * @throws WasmException if eviction fails
   */
  void evictFromCache(String componentId) throws WasmException;

  /**
   * Gets cache statistics for the registry.
   *
   * @return cache statistics
   */
  CacheStatistics getCacheStatistics();

  /**
   * Verifies the digital signature of a component.
   *
   * @param component the component to verify
   * @param signature the digital signature
   * @return signature verification result
   */
  SignatureVerificationResult verifyComponentSignature(ComponentSimple component, ComponentSignature signature);

  /**
   * Signs a component with the registry's private key.
   *
   * @param component the component to sign
   * @param signingConfig signing configuration
   * @return the component signature
   * @throws WasmException if signing fails
   */
  ComponentSignature signComponent(ComponentSimple component, SigningConfig signingConfig) throws WasmException;

  /**
   * Sets up a component replication strategy.
   *
   * @param componentId the component to replicate
   * @param replicationConfig replication configuration
   * @return future that completes when replication is set up
   * @throws WasmException if replication setup fails
   */
  CompletableFuture<ReplicationResult> setupReplication(String componentId,
                                                        ReplicationConfig replicationConfig) throws WasmException;

  /**
   * Gets replication status for a component.
   *
   * @param componentId the component ID
   * @return replication status
   */
  Optional<ReplicationStatus> getReplicationStatus(String componentId);

  /**
   * Performs distributed dependency resolution.
   *
   * @param component the component to resolve dependencies for
   * @param resolutionConfig resolution configuration
   * @return future containing resolved dependencies
   * @throws WasmException if resolution fails
   */
  CompletableFuture<DistributedDependencyResolution> resolveDependenciesDistributed(ComponentSimple component,
                                                                                   DependencyResolutionConfig resolutionConfig) throws WasmException;

  /**
   * Publishes component metadata to discovery services.
   *
   * @param component the component to publish
   * @param publishConfig publish configuration
   * @return future that completes when publishing is done
   * @throws WasmException if publishing fails
   */
  CompletableFuture<PublishResult> publishComponent(ComponentSimple component,
                                                   PublishConfig publishConfig) throws WasmException;

  /**
   * Unpublishes component metadata from discovery services.
   *
   * @param componentId the component ID to unpublish
   * @return future that completes when unpublishing is done
   * @throws WasmException if unpublishing fails
   */
  CompletableFuture<Void> unpublishComponent(String componentId) throws WasmException;

  /**
   * Gets distributed registry statistics.
   *
   * @return distributed registry statistics
   */
  DistributedRegistryStatistics getDistributedStatistics();

  /**
   * Sets up monitoring for distributed registry events.
   *
   * @param listener the event listener
   */
  void setDistributedEventListener(DistributedRegistryEventListener listener);

  /**
   * Removes the distributed registry event listener.
   */
  void removeDistributedEventListener();

  /**
   * Starts the distributed registry services.
   *
   * @throws WasmException if startup fails
   */
  void startDistributedServices() throws WasmException;

  /**
   * Stops the distributed registry services.
   *
   * @throws WasmException if shutdown fails
   */
  void stopDistributedServices() throws WasmException;

  /**
   * Distributed registration result.
   */
  interface DistributedRegistrationResult {
    boolean isSuccessful();
    String getComponentId();
    List<String> getSuccessfulNodes();
    List<String> getFailedNodes();
    Map<String, Exception> getNodeErrors();
    Instant getRegistrationTime();
  }

  /**
   * Distributed search result.
   */
  interface DistributedSearchResult {
    List<ComponentSearchMatch> getAllMatches();
    Map<String, List<ComponentSearchMatch>> getMatchesByNode();
    List<String> getSearchedNodes();
    List<String> getUnreachableNodes();
    long getTotalSearchTime();
    Map<String, Long> getNodeSearchTimes();
  }

  /**
   * Component search match with node information.
   */
  interface ComponentSearchMatch {
    ComponentSimple getComponent();
    String getSourceNode();
    double getRelevanceScore();
    Instant getLastUpdated();
    boolean isLocal();
    Optional<ComponentSignature> getSignature();
  }

  /**
   * Registry node information.
   */
  interface RegistryNode {
    String getNodeId();
    String getAddress();
    int getPort();
    NodeType getType();
    Set<String> getCapabilities();
    Instant getLastSeen();
    NodeHealthStatus getHealthStatus();
    Map<String, Object> getMetadata();
  }

  /**
   * Node health status.
   */
  interface NodeHealthStatus {
    boolean isHealthy();
    double getCpuUsage();
    double getMemoryUsage();
    long getComponentCount();
    Instant getLastHealthCheck();
    List<String> getHealthIssues();
    NodeAvailability getAvailability();
  }

  /**
   * Distributed component metadata.
   */
  interface DistributedComponentMetadata extends ComponentMetadata {
    Set<String> getAvailableNodes();
    String getPrimaryNode();
    ReplicationLevel getReplicationLevel();
    Optional<ComponentSignature> getSignature();
    Map<String, Instant> getNodeLastUpdated();
    DistributionPolicy getDistributionPolicy();
  }

  /**
   * Cache statistics.
   */
  interface CacheStatistics {
    long getCacheSize();
    long getHitCount();
    long getMissCount();
    double getHitRate();
    double getEvictionRate();
    long getAverageLoadTime();
    Map<String, Object> getAdditionalMetrics();
  }

  /**
   * Component signature for security verification.
   */
  interface ComponentSignature {
    String getAlgorithm();
    byte[] getSignature();
    String getSignerId();
    Instant getSigningTime();
    Optional<String> getCertificateChain();
    Map<String, Object> getAdditionalInfo();
  }

  /**
   * Signature verification result.
   */
  interface SignatureVerificationResult {
    boolean isValid();
    String getSignerId();
    Instant getVerificationTime();
    List<String> getIssues();
    TrustLevel getTrustLevel();
  }

  /**
   * Replication result.
   */
  interface ReplicationResult {
    boolean isSuccessful();
    List<String> getReplicatedNodes();
    List<String> getFailedNodes();
    ReplicationLevel getAchievedLevel();
    Map<String, Exception> getReplicationErrors();
  }

  /**
   * Replication status.
   */
  interface ReplicationStatus {
    String getComponentId();
    ReplicationLevel getTargetLevel();
    ReplicationLevel getCurrentLevel();
    Set<String> getReplicatedNodes();
    Set<String> getFailedNodes();
    Instant getLastReplication();
    boolean isHealthy();
  }

  /**
   * Distributed dependency resolution result.
   */
  interface DistributedDependencyResolution {
    Set<ComponentSimple> getResolvedDependencies();
    Map<String, String> getDependencyNodes();
    List<String> getUnresolvedDependencies();
    Map<String, Exception> getResolutionErrors();
    long getResolutionTime();
  }

  /**
   * Component publish result.
   */
  interface PublishResult {
    boolean isSuccessful();
    List<String> getPublishedServices();
    List<String> getFailedServices();
    Map<String, Exception> getPublishErrors();
    Instant getPublishTime();
  }

  /**
   * Distributed registry statistics.
   */
  interface DistributedRegistryStatistics extends ComponentRegistryStatistics {
    int getConnectedNodeCount();
    long getTotalDistributedComponents();
    double getAverageReplicationFactor();
    long getDistributedSearches();
    double getAverageSearchTime();
    Map<String, NodeStatistics> getNodeStatistics();
  }

  /**
   * Node-specific statistics.
   */
  interface NodeStatistics {
    String getNodeId();
    long getComponentCount();
    long getRequestCount();
    double getAverageResponseTime();
    double getErrorRate();
    Instant getLastUpdate();
  }

  /**
   * Distributed registry event listener.
   */
  interface DistributedRegistryEventListener {
    void onNodeConnected(RegistryNode node);
    void onNodeDisconnected(String nodeId);
    void onComponentReplicated(String componentId, String toNode);
    void onReplicationFailed(String componentId, String toNode, Exception error);
    void onSynchronizationCompleted(SynchronizationResult result);
    void onDistributedSearchCompleted(DistributedSearchResult result);
  }

  // Enums and configuration types
  enum NodeType { PRIMARY, SECONDARY, CACHE, PROXY }
  enum NodeAvailability { AVAILABLE, BUSY, UNAVAILABLE, MAINTENANCE }
  enum ReplicationLevel { NONE, LOW, MEDIUM, HIGH, FULL }
  enum TrustLevel { TRUSTED, VERIFIED, UNVERIFIED, UNTRUSTED }

  /**
   * Synchronization result.
   */
  interface SynchronizationResult {
    boolean isSuccessful();
    int getSynchronizedComponents();
    int getConflictCount();
    List<String> getConflictedComponents();
    long getSynchronizationTime();
    Map<String, Exception> getSyncErrors();
  }
}