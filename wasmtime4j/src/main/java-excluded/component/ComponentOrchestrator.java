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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Advanced component orchestrator for managing complex component graphs with sophisticated
 * dependency resolution, lifecycle coordination, and failure recovery.
 *
 * <p>This interface provides enterprise-grade orchestration features including:
 *
 * <ul>
 *   <li>Complex dependency graph resolution with circular dependency detection
 *   <li>Component lifecycle coordination with rollback capabilities
 *   <li>Load balancing and auto-scaling across component instances
 *   <li>Failure detection and automatic recovery mechanisms
 *   <li>Resource optimization and intelligent scheduling
 * </ul>
 *
 * @since 1.0.0
 */
public interface ComponentOrchestrator extends AutoCloseable {

  /**
   * Gets the unique identifier for this orchestrator.
   *
   * @return the orchestrator identifier
   */
  String getId();

  /**
   * Gets the configuration used to create this orchestrator.
   *
   * @return the orchestration configuration
   */
  ComponentOrchestrationConfig getConfig();

  // Dependency Resolution and Graph Management

  /**
   * Resolves dependencies for a complex component graph with circular dependency detection.
   *
   * @param components the components to resolve dependencies for
   * @param resolutionStrategy the dependency resolution strategy
   * @return the resolved dependency graph
   * @throws WasmException if dependency resolution fails
   */
  ComponentDependencyGraph resolveDependencyGraph(
      Set<Component> components, DependencyResolutionStrategy resolutionStrategy)
      throws WasmException;

  /**
   * Validates a component dependency graph for circular dependencies and conflicts.
   *
   * @param dependencyGraph the dependency graph to validate
   * @return the validation result
   * @throws WasmException if validation fails
   */
  ComponentDependencyGraphValidationResult validateDependencyGraph(
      ComponentDependencyGraph dependencyGraph) throws WasmException;

  /**
   * Optimizes a component dependency graph for performance and resource usage.
   *
   * @param dependencyGraph the dependency graph to optimize
   * @param optimizationStrategy the optimization strategy
   * @return the optimized dependency graph
   * @throws WasmException if optimization fails
   */
  ComponentDependencyGraph optimizeDependencyGraph(
      ComponentDependencyGraph dependencyGraph,
      ComponentDependencyGraphOptimizationStrategy optimizationStrategy)
      throws WasmException;

  /**
   * Resolves circular dependencies using the specified resolution strategy.
   *
   * @param circularDependencies the circular dependencies to resolve
   * @param resolutionStrategy the circular dependency resolution strategy
   * @return the resolution result
   * @throws WasmException if resolution fails
   */
  CircularDependencyResolutionResult resolveCircularDependencies(
      Set<ComponentDependencyCycle> circularDependencies,
      CircularDependencyResolutionStrategy resolutionStrategy)
      throws WasmException;

  // Lifecycle Coordination and Management

  /**
   * Orchestrates component lifecycle coordination across multiple components with rollback.
   *
   * @param components the components to coordinate
   * @param coordinationPlan the lifecycle coordination plan
   * @return a future that completes when coordination is finished
   * @throws WasmException if coordination fails
   */
  CompletableFuture<ComponentLifecycleCoordinationResult> coordinateLifecycles(
      Set<Component> components, ComponentLifecycleCoordinationPlan coordinationPlan)
      throws WasmException;

  /**
   * Performs a coordinated start operation across multiple components.
   *
   * @param components the components to start
   * @param startConfig the start configuration
   * @return a future that completes when all components are started
   * @throws WasmException if starting fails
   */
  CompletableFuture<ComponentStartResult> startComponents(
      Set<Component> components, ComponentStartConfig startConfig) throws WasmException;

  /**
   * Performs a coordinated stop operation across multiple components.
   *
   * @param components the components to stop
   * @param stopConfig the stop configuration
   * @return a future that completes when all components are stopped
   * @throws WasmException if stopping fails
   */
  CompletableFuture<ComponentStopResult> stopComponents(
      Set<Component> components, ComponentStopConfig stopConfig) throws WasmException;

  /**
   * Performs a coordinated restart operation across multiple components.
   *
   * @param components the components to restart
   * @param restartConfig the restart configuration
   * @return a future that completes when all components are restarted
   * @throws WasmException if restarting fails
   */
  CompletableFuture<ComponentRestartResult> restartComponents(
      Set<Component> components, ComponentRestartConfig restartConfig) throws WasmException;

  /**
   * Performs a rollback operation to a previous stable state.
   *
   * @param rollbackTarget the target state to rollback to
   * @param rollbackConfig the rollback configuration
   * @return a future that completes when rollback is finished
   * @throws WasmException if rollback fails
   */
  CompletableFuture<ComponentRollbackResult> rollbackToState(
      ComponentOrchestrationState rollbackTarget, ComponentRollbackConfig rollbackConfig)
      throws WasmException;

  // Hot-Swapping and Live Updates

  /**
   * Performs coordinated hot-swapping across multiple components.
   *
   * @param swapPlan the hot-swap plan defining component replacements
   * @param swapStrategy the hot-swap strategy
   * @return a future that completes when hot-swap is finished
   * @throws WasmException if hot-swap fails
   */
  CompletableFuture<ComponentHotSwapResult> performCoordinatedHotSwap(
      ComponentHotSwapPlan swapPlan, CoordinatedHotSwapStrategy swapStrategy) throws WasmException;

  /**
   * Performs a rolling update across component instances.
   *
   * @param updatePlan the rolling update plan
   * @param updateStrategy the rolling update strategy
   * @return a future that completes when rolling update is finished
   * @throws WasmException if rolling update fails
   */
  CompletableFuture<ComponentRollingUpdateResult> performRollingUpdate(
      ComponentRollingUpdatePlan updatePlan, ComponentRollingUpdateStrategy updateStrategy)
      throws WasmException;

  /**
   * Performs a blue-green deployment for zero-downtime updates.
   *
   * @param deploymentPlan the blue-green deployment plan
   * @param deploymentStrategy the blue-green deployment strategy
   * @return a future that completes when deployment is finished
   * @throws WasmException if deployment fails
   */
  CompletableFuture<ComponentBlueGreenDeploymentResult> performBlueGreenDeployment(
      ComponentBlueGreenDeploymentPlan deploymentPlan,
      ComponentBlueGreenDeploymentStrategy deploymentStrategy)
      throws WasmException;

  // Load Balancing and Auto-Scaling

  /**
   * Sets up load balancing across component instances.
   *
   * @param components the components to load balance
   * @param loadBalancingConfig the load balancing configuration
   * @return a load-balanced component proxy
   * @throws WasmException if load balancing setup fails
   */
  LoadBalancedComponent setupLoadBalancing(
      Set<Component> components, ComponentLoadBalancingConfig loadBalancingConfig)
      throws WasmException;

  /**
   * Configures auto-scaling policies for components.
   *
   * @param components the components to configure auto-scaling for
   * @param autoScalingPolicies the auto-scaling policies
   * @throws WasmException if auto-scaling configuration fails
   */
  void configureAutoScaling(
      Set<Component> components, Map<Component, ComponentAutoScalingPolicy> autoScalingPolicies)
      throws WasmException;

  /**
   * Triggers manual scaling for specific components.
   *
   * @param scalingRequests the scaling requests
   * @return a future that completes when scaling is finished
   * @throws WasmException if scaling fails
   */
  CompletableFuture<ComponentScalingResult> performManualScaling(
      Set<ComponentScalingRequest> scalingRequests) throws WasmException;

  /**
   * Rebalances load across component instances based on current metrics.
   *
   * @param rebalancingConfig the rebalancing configuration
   * @return a future that completes when rebalancing is finished
   * @throws WasmException if rebalancing fails
   */
  CompletableFuture<ComponentLoadRebalancingResult> rebalanceLoad(
      ComponentLoadRebalancingConfig rebalancingConfig) throws WasmException;

  // Failure Detection and Recovery

  /**
   * Sets up failure detection monitoring for orchestrated components.
   *
   * @param failureDetectionConfig the failure detection configuration
   * @throws WasmException if failure detection setup fails
   */
  void setupFailureDetection(ComponentFailureDetectionConfig failureDetectionConfig)
      throws WasmException;

  /**
   * Configures automatic recovery policies for component failures.
   *
   * @param recoveryPolicies the recovery policies per component
   * @throws WasmException if recovery configuration fails
   */
  void configureAutomaticRecovery(Map<Component, ComponentRecoveryPolicy> recoveryPolicies)
      throws WasmException;

  /**
   * Performs manual recovery for failed components.
   *
   * @param failedComponents the failed components to recover
   * @param recoveryStrategy the recovery strategy
   * @return a future that completes when recovery is finished
   * @throws WasmException if recovery fails
   */
  CompletableFuture<ComponentRecoveryResult> performManualRecovery(
      Set<Component> failedComponents, ComponentRecoveryStrategy recoveryStrategy)
      throws WasmException;

  /**
   * Performs a health check across all orchestrated components.
   *
   * @param healthCheckConfig the health check configuration
   * @return the overall health check result
   * @throws WasmException if health check fails
   */
  ComponentOrchestrationHealthCheckResult performHealthCheck(
      ComponentOrchestrationHealthCheckConfig healthCheckConfig) throws WasmException;

  // Resource Optimization and Scheduling

  /**
   * Optimizes resource allocation across orchestrated components.
   *
   * @param optimizationConfig the resource optimization configuration
   * @return a future that completes when optimization is finished
   * @throws WasmException if optimization fails
   */
  CompletableFuture<ComponentResourceOptimizationResult> optimizeResourceAllocation(
      ComponentResourceOptimizationConfig optimizationConfig) throws WasmException;

  /**
   * Schedules component operations based on resource availability and priorities.
   *
   * @param schedulingRequests the scheduling requests
   * @param schedulingStrategy the scheduling strategy
   * @return a future that completes when scheduling is finished
   * @throws WasmException if scheduling fails
   */
  CompletableFuture<ComponentSchedulingResult> scheduleOperations(
      List<ComponentSchedulingRequest> schedulingRequests,
      ComponentSchedulingStrategy schedulingStrategy)
      throws WasmException;

  /**
   * Performs intelligent placement of component instances across available resources.
   *
   * @param placementRequests the placement requests
   * @param placementStrategy the placement strategy
   * @return a future that completes when placement is finished
   * @throws WasmException if placement fails
   */
  CompletableFuture<ComponentPlacementResult> performIntelligentPlacement(
      Set<ComponentPlacementRequest> placementRequests,
      ComponentPlacementStrategy placementStrategy)
      throws WasmException;

  // Monitoring and Observability

  /**
   * Gets orchestration metrics and statistics.
   *
   * @return the orchestration metrics
   */
  ComponentOrchestrationMetrics getMetrics();

  /**
   * Gets the current state of the orchestrator.
   *
   * @return the orchestrator state
   */
  ComponentOrchestrationState getState();

  /**
   * Gets all components currently managed by this orchestrator.
   *
   * @return the set of managed components
   */
  Set<Component> getManagedComponents();

  /**
   * Gets debugging information for the orchestrator.
   *
   * @return the orchestrator debugging information
   */
  ComponentOrchestrationDebugInfo getDebugInfo();

  /**
   * Checks if this orchestrator is still valid and operational.
   *
   * @return true if the orchestrator is valid, false otherwise
   */
  boolean isValid();

  @Override
  void close() throws Exception;
}
