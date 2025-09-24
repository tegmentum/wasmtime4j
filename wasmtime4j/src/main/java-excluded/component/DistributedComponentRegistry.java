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

import ai.tegmentum.wasmtime4j.Component;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Distributed registry for WebAssembly components with secure cross-network discovery,
 * authentication, and coordination capabilities.
 *
 * <p>This interface provides enterprise-grade distributed component management including:
 *
 * <ul>
 *   <li>Secure component registration and discovery across network boundaries
 *   <li>Component authentication and authorization mechanisms
 *   <li>Distributed consensus for component state synchronization
 *   <li>Component replication and failover support
 *   <li>Network partition tolerance and recovery
 * </ul>
 *
 * @since 1.0.0
 */
public interface DistributedComponentRegistry extends AutoCloseable {

  /**
   * Gets the unique identifier for this registry.
   *
   * @return the registry identifier
   */
  String getId();

  /**
   * Gets the configuration used to create this registry.
   *
   * @return the registry configuration
   */
  DistributedRegistryConfig getConfig();

  /**
   * Gets the current status of this registry.
   *
   * @return the registry status
   */
  DistributedRegistryStatus getStatus();

  // Component Registration and Management

  /**
   * Registers a component with the distributed registry.
   *
   * @param component the component to register
   * @param registrationConfig the registration configuration
   * @param authentication the authentication credentials
   * @return a future that completes when registration is finished
   * @throws WasmException if registration fails
   */
  CompletableFuture<ComponentRegistrationResult> registerComponent(
      Component component,
      ComponentRegistrationConfig registrationConfig,
      ComponentAuthentication authentication)
      throws WasmException;

  /**
   * Unregisters a component from the distributed registry.
   *
   * @param componentId the ID of the component to unregister
   * @param authentication the authentication credentials
   * @return a future that completes when unregistration is finished
   * @throws WasmException if unregistration fails
   */
  CompletableFuture<Void> unregisterComponent(
      String componentId, ComponentAuthentication authentication) throws WasmException;

  /**
   * Updates the registration information for a component.
   *
   * @param componentId the ID of the component to update
   * @param updateConfig the update configuration
   * @param authentication the authentication credentials
   * @return a future that completes when update is finished
   * @throws WasmException if update fails
   */
  CompletableFuture<ComponentRegistrationResult> updateComponentRegistration(
      String componentId,
      ComponentRegistrationUpdateConfig updateConfig,
      ComponentAuthentication authentication)
      throws WasmException;

  // Component Discovery

  /**
   * Discovers components based on search criteria.
   *
   * @param discoveryQuery the component discovery query
   * @param discoveryConfig the discovery configuration
   * @return a future that completes with discovered components
   * @throws WasmException if discovery fails
   */
  CompletableFuture<Set<DistributedComponent>> discoverComponents(
      ComponentDiscoveryQuery discoveryQuery, ComponentDiscoveryConfig discoveryConfig)
      throws WasmException;

  /**
   * Finds a specific component by its identifier.
   *
   * @param componentId the component identifier
   * @param searchConfig the search configuration
   * @return a future that completes with the found component, or null if not found
   * @throws WasmException if search fails
   */
  CompletableFuture<DistributedComponent> findComponent(
      String componentId, ComponentSearchConfig searchConfig) throws WasmException;

  /**
   * Lists all components registered in the distributed registry.
   *
   * @param listConfig the listing configuration
   * @return a future that completes with all registered components
   * @throws WasmException if listing fails
   */
  CompletableFuture<Set<DistributedComponent>> listAllComponents(ComponentListConfig listConfig)
      throws WasmException;

  /**
   * Searches for components by metadata criteria.
   *
   * @param metadataQuery the metadata search query
   * @param searchConfig the search configuration
   * @return a future that completes with matching components
   * @throws WasmException if search fails
   */
  CompletableFuture<Set<DistributedComponent>> searchByMetadata(
      ComponentMetadataQuery metadataQuery, ComponentSearchConfig searchConfig)
      throws WasmException;

  // Authentication and Authorization

  /**
   * Authenticates a request to access the registry.
   *
   * @param authentication the authentication credentials
   * @return a future that completes with authentication result
   * @throws WasmException if authentication fails
   */
  CompletableFuture<ComponentAuthenticationResult> authenticate(
      ComponentAuthentication authentication) throws WasmException;

  /**
   * Authorizes access to a specific component.
   *
   * @param componentId the component identifier
   * @param accessRequest the access request details
   * @param authentication the authentication credentials
   * @return a future that completes with authorization result
   * @throws WasmException if authorization fails
   */
  CompletableFuture<ComponentAuthorizationResult> authorizeAccess(
      String componentId,
      ComponentAccessRequest accessRequest,
      ComponentAuthentication authentication)
      throws WasmException;

  /**
   * Revokes access permissions for a component.
   *
   * @param componentId the component identifier
   * @param revocationRequest the revocation request
   * @param authentication the authentication credentials
   * @return a future that completes when revocation is finished
   * @throws WasmException if revocation fails
   */
  CompletableFuture<Void> revokeAccess(
      String componentId,
      ComponentAccessRevocationRequest revocationRequest,
      ComponentAuthentication authentication)
      throws WasmException;

  // Distributed Consensus and Synchronization

  /**
   * Participates in distributed consensus for component state updates.
   *
   * @param consensusRequest the consensus request
   * @param timeout the consensus timeout
   * @return a future that completes with consensus result
   * @throws WasmException if consensus fails
   */
  CompletableFuture<DistributedConsensusResult> participateInConsensus(
      ComponentConsensusRequest consensusRequest, Duration timeout) throws WasmException;

  /**
   * Synchronizes registry state with other registry nodes.
   *
   * @param syncConfig the synchronization configuration
   * @return a future that completes when synchronization is finished
   * @throws WasmException if synchronization fails
   */
  CompletableFuture<RegistrySynchronizationResult> synchronizeWithPeers(
      RegistrySynchronizationConfig syncConfig) throws WasmException;

  /**
   * Broadcasts a state change to all registry peers.
   *
   * @param stateChange the state change to broadcast
   * @param broadcastConfig the broadcast configuration
   * @return a future that completes when broadcast is finished
   * @throws WasmException if broadcast fails
   */
  CompletableFuture<RegistryBroadcastResult> broadcastStateChange(
      RegistryStateChange stateChange, RegistryBroadcastConfig broadcastConfig)
      throws WasmException;

  // Component Replication and Failover

  /**
   * Sets up component replication across registry nodes.
   *
   * @param componentId the component to replicate
   * @param replicationConfig the replication configuration
   * @return a future that completes when replication setup is finished
   * @throws WasmException if replication setup fails
   */
  CompletableFuture<ComponentReplicationResult> setupComponentReplication(
      String componentId, ComponentReplicationConfig replicationConfig) throws WasmException;

  /**
   * Triggers failover for a component to backup nodes.
   *
   * @param componentId the component to failover
   * @param failoverConfig the failover configuration
   * @return a future that completes when failover is finished
   * @throws WasmException if failover fails
   */
  CompletableFuture<ComponentFailoverResult> triggerComponentFailover(
      String componentId, ComponentFailoverConfig failoverConfig) throws WasmException;

  /**
   * Monitors the health of registered components.
   *
   * @param monitoringConfig the health monitoring configuration
   * @throws WasmException if monitoring setup fails
   */
  void setupComponentHealthMonitoring(ComponentHealthMonitoringConfig monitoringConfig)
      throws WasmException;

  // Network Partition Handling

  /**
   * Handles network partition scenarios by isolating affected nodes.
   *
   * @param partitionInfo the network partition information
   * @param recoveryStrategy the partition recovery strategy
   * @return a future that completes when partition handling is finished
   * @throws WasmException if partition handling fails
   */
  CompletableFuture<NetworkPartitionRecoveryResult> handleNetworkPartition(
      NetworkPartitionInfo partitionInfo, NetworkPartitionRecoveryStrategy recoveryStrategy)
      throws WasmException;

  /**
   * Recovers from network partition by rejoining the registry cluster.
   *
   * @param recoveryConfig the partition recovery configuration
   * @return a future that completes when recovery is finished
   * @throws WasmException if recovery fails
   */
  CompletableFuture<PartitionRecoveryResult> recoverFromPartition(
      PartitionRecoveryConfig recoveryConfig) throws WasmException;

  // Registry Cluster Management

  /**
   * Joins a distributed registry cluster.
   *
   * @param clusterConfig the cluster configuration
   * @return a future that completes when cluster join is finished
   * @throws WasmException if cluster join fails
   */
  CompletableFuture<RegistryClusterJoinResult> joinCluster(
      DistributedRegistryClusterConfig clusterConfig) throws WasmException;

  /**
   * Leaves the distributed registry cluster.
   *
   * @param leaveConfig the cluster leave configuration
   * @return a future that completes when cluster leave is finished
   * @throws WasmException if cluster leave fails
   */
  CompletableFuture<Void> leaveCluster(RegistryClusterLeaveConfig leaveConfig) throws WasmException;

  /**
   * Gets information about the current registry cluster.
   *
   * @return the cluster information
   */
  DistributedRegistryClusterInfo getClusterInfo();

  /**
   * Gets the status of all nodes in the registry cluster.
   *
   * @return the cluster node status information
   */
  DistributedRegistryClusterStatus getClusterStatus();

  // Monitoring and Metrics

  /**
   * Gets metrics about registry operations and performance.
   *
   * @return the registry metrics
   */
  DistributedRegistryMetrics getMetrics();

  /**
   * Gets the current load and capacity information for the registry.
   *
   * @return the registry load information
   */
  DistributedRegistryLoadInfo getLoadInfo();

  /**
   * Performs a health check on the distributed registry.
   *
   * @param healthCheckConfig the health check configuration
   * @return the health check result
   * @throws WasmException if health check fails
   */
  DistributedRegistryHealthCheckResult performHealthCheck(
      DistributedRegistryHealthCheckConfig healthCheckConfig) throws WasmException;

  // Security and Compliance

  /**
   * Gets audit logs for registry operations.
   *
   * @param auditQuery the audit log query
   * @return the audit log entries
   * @throws WasmException if audit log retrieval fails
   */
  CompletableFuture<Set<RegistryAuditLogEntry>> getAuditLogs(RegistryAuditLogQuery auditQuery)
      throws WasmException;

  /**
   * Enforces security policies across the distributed registry.
   *
   * @param securityPolicies the security policies to enforce
   * @throws WasmException if policy enforcement fails
   */
  void enforceSecurityPolicies(Set<DistributedRegistrySecurityPolicy> securityPolicies)
      throws WasmException;

  /**
   * Validates compliance with regulatory requirements.
   *
   * @param complianceConfig the compliance validation configuration
   * @return the compliance validation result
   * @throws WasmException if compliance validation fails
   */
  DistributedRegistryComplianceResult validateCompliance(
      DistributedRegistryComplianceConfig complianceConfig) throws WasmException;

  /**
   * Checks if this registry is still valid and operational.
   *
   * @return true if the registry is valid, false otherwise
   */
  boolean isValid();

  @Override
  void close() throws Exception;
}
