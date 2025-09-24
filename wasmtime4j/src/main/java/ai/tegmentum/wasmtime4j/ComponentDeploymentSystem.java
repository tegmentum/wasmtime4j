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
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Component deployment and distribution system.
 *
 * <p>The ComponentDeploymentSystem provides comprehensive deployment capabilities including:
 * <ul>
 *   <li>Component packaging and distribution</li>
 *   <li>Deployment configuration and environment handling</li>
 *   <li>Component rollback and version management</li>
 *   <li>Health checking and monitoring integration</li>
 *   <li>Multi-environment deployment support</li>
 *   <li>Automated deployment pipelines</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface ComponentDeploymentSystem extends AutoCloseable {

  /**
   * Gets the deployment system identifier.
   *
   * @return the system ID
   */
  String getId();

  /**
   * Gets the deployment system configuration.
   *
   * @return the configuration
   */
  DeploymentConfig getConfiguration();

  /**
   * Creates a deployment package from a component.
   *
   * @param component the component to package
   * @param packageConfig package configuration
   * @return future containing the deployment package
   * @throws WasmException if packaging fails
   */
  CompletableFuture<DeploymentPackage> createDeploymentPackage(ComponentSimple component,
                                                               PackageConfig packageConfig) throws WasmException;

  /**
   * Validates a deployment package before deployment.
   *
   * @param deploymentPackage the package to validate
   * @param validationConfig validation configuration
   * @return package validation result
   * @throws WasmException if validation fails
   */
  PackageValidationResult validatePackage(DeploymentPackage deploymentPackage,
                                          PackageValidationConfig validationConfig) throws WasmException;

  /**
   * Deploys a component package to target environments.
   *
   * @param deploymentPackage the package to deploy
   * @param deploymentTargets target environments for deployment
   * @param deploymentOptions deployment options
   * @return future containing deployment result
   * @throws WasmException if deployment fails
   */
  CompletableFuture<DeploymentResult> deployPackage(DeploymentPackage deploymentPackage,
                                                    List<DeploymentTarget> deploymentTargets,
                                                    DeploymentOptions deploymentOptions) throws WasmException;

  /**
   * Performs a rolling deployment across multiple environments.
   *
   * @param deploymentPackage the package to deploy
   * @param rolloutStrategy rolling deployment strategy
   * @return future containing rollout result
   * @throws WasmException if rollout fails
   */
  CompletableFuture<RolloutResult> performRollingDeployment(DeploymentPackage deploymentPackage,
                                                            RolloutStrategy rolloutStrategy) throws WasmException;

  /**
   * Rolls back a deployment to a previous version.
   *
   * @param deploymentId the deployment to rollback
   * @param rollbackConfig rollback configuration
   * @return future containing rollback result
   * @throws WasmException if rollback fails
   */
  CompletableFuture<RollbackResult> rollbackDeployment(String deploymentId,
                                                       RollbackConfig rollbackConfig) throws WasmException;

  /**
   * Updates the configuration of a deployed component.
   *
   * @param deploymentId the deployment to update
   * @param configurationUpdate configuration updates
   * @return future containing update result
   * @throws WasmException if configuration update fails
   */
  CompletableFuture<ConfigurationUpdateResult> updateDeploymentConfiguration(String deploymentId,
                                                                             ConfigurationUpdate configurationUpdate) throws WasmException;

  /**
   * Scales a deployment up or down.
   *
   * @param deploymentId the deployment to scale
   * @param scalingConfig scaling configuration
   * @return future containing scaling result
   * @throws WasmException if scaling fails
   */
  CompletableFuture<ScalingResult> scaleDeployment(String deploymentId,
                                                   ScalingConfig scalingConfig) throws WasmException;

  /**
   * Monitors the health of deployed components.
   *
   * @param deploymentId the deployment to monitor
   * @param healthCheckConfig health check configuration
   * @return deployment health monitor
   * @throws WasmException if health monitoring setup fails
   */
  DeploymentHealthMonitor startHealthMonitoring(String deploymentId,
                                                HealthCheckConfig healthCheckConfig) throws WasmException;

  /**
   * Stops health monitoring for a deployment.
   *
   * @param monitor the health monitor to stop
   * @throws WasmException if health monitoring stop fails
   */
  void stopHealthMonitoring(DeploymentHealthMonitor monitor) throws WasmException;

  /**
   * Gets the current status of a deployment.
   *
   * @param deploymentId the deployment ID
   * @return deployment status
   */
  Optional<DeploymentStatus> getDeploymentStatus(String deploymentId);

  /**
   * Gets deployment history for a component.
   *
   * @param componentId the component ID
   * @return deployment history
   */
  DeploymentHistory getDeploymentHistory(String componentId);

  /**
   * Creates a deployment environment.
   *
   * @param environmentConfig environment configuration
   * @return the created deployment environment
   * @throws WasmException if environment creation fails
   */
  DeploymentEnvironment createEnvironment(EnvironmentConfig environmentConfig) throws WasmException;

  /**
   * Removes a deployment environment.
   *
   * @param environmentId the environment ID to remove
   * @throws WasmException if environment removal fails
   */
  void removeEnvironment(String environmentId) throws WasmException;

  /**
   * Gets all available deployment environments.
   *
   * @return list of deployment environments
   */
  List<DeploymentEnvironment> getEnvironments();

  /**
   * Creates a deployment pipeline.
   *
   * @param pipelineConfig pipeline configuration
   * @return the created deployment pipeline
   * @throws WasmException if pipeline creation fails
   */
  DeploymentPipeline createPipeline(PipelineConfig pipelineConfig) throws WasmException;

  /**
   * Executes a deployment pipeline.
   *
   * @param pipelineId the pipeline ID to execute
   * @param executionConfig execution configuration
   * @return future containing pipeline execution result
   * @throws WasmException if pipeline execution fails
   */
  CompletableFuture<PipelineExecutionResult> executePipeline(String pipelineId,
                                                             PipelineExecutionConfig executionConfig) throws WasmException;

  /**
   * Gets all active deployments.
   *
   * @return list of active deployments
   */
  List<ActiveDeployment> getActiveDeployments();

  /**
   * Gets deployment statistics and metrics.
   *
   * @return deployment system statistics
   */
  DeploymentStatistics getStatistics();

  /**
   * Sets a deployment event listener.
   *
   * @param listener the event listener
   */
  void setDeploymentEventListener(DeploymentEventListener listener);

  /**
   * Removes the deployment event listener.
   */
  void removeDeploymentEventListener();

  /**
   * Starts the deployment system services.
   *
   * @throws WasmException if startup fails
   */
  void start() throws WasmException;

  /**
   * Stops the deployment system services.
   *
   * @throws WasmException if shutdown fails
   */
  void stop() throws WasmException;

  @Override
  void close();

  /**
   * Deployment package containing a component and its deployment metadata.
   */
  interface DeploymentPackage {
    String getPackageId();
    ComponentSimple getComponent();
    ComponentVersion getVersion();
    PackageConfig getConfiguration();
    Map<String, Object> getMetadata();
    byte[] getPackageData();
    String getChecksum();
    Instant getCreationTime();
    long getSize();
    ComponentSignature getSignature();
    Set<String> getSupportedPlatforms();
    Map<String, String> getDependencies();
  }

  /**
   * Deployment target environment.
   */
  interface DeploymentTarget {
    String getTargetId();
    String getEnvironmentName();
    URI getEndpoint();
    Map<String, String> getProperties();
    Set<String> getCapabilities();
    DeploymentTargetType getType();
    boolean isHealthy();
    ResourceRequirements getResourceRequirements();
  }

  /**
   * Deployment result information.
   */
  interface DeploymentResult {
    String getDeploymentId();
    boolean isSuccessful();
    Instant getStartTime();
    Instant getEndTime();
    java.time.Duration getTotalTime();
    List<DeploymentTargetResult> getTargetResults();
    Optional<Exception> getError();
    Map<String, Object> getMetrics();
    DeploymentStatus getFinalStatus();
  }

  /**
   * Deployment target-specific result.
   */
  interface DeploymentTargetResult {
    DeploymentTarget getTarget();
    boolean isSuccessful();
    java.time.Duration getDeploymentTime();
    Optional<String> getDeployedVersion();
    Optional<Exception> getError();
    List<String> getWarnings();
    Map<String, Object> getTargetMetrics();
  }

  /**
   * Rolling deployment rollout result.
   */
  interface RolloutResult {
    String getRolloutId();
    boolean isSuccessful();
    RolloutStrategy getStrategy();
    Instant getStartTime();
    Instant getEndTime();
    List<RolloutStage> getCompletedStages();
    List<RolloutStage> getFailedStages();
    Optional<Exception> getError();
    RolloutStatus getFinalStatus();
  }

  /**
   * Deployment rollback result.
   */
  interface RollbackResult {
    String getRollbackId();
    String getOriginalDeploymentId();
    ComponentVersion getRolledBackToVersion();
    boolean isSuccessful();
    Instant getStartTime();
    Instant getEndTime();
    List<DeploymentTargetResult> getTargetResults();
    Optional<Exception> getError();
  }

  /**
   * Deployment health monitor.
   */
  interface DeploymentHealthMonitor {
    String getMonitorId();
    String getDeploymentId();
    HealthCheckConfig getConfiguration();
    boolean isMonitoring();

    DeploymentHealthStatus getCurrentHealth();
    List<HealthCheckResult> getRecentHealthChecks();
    HealthStatistics getHealthStatistics();

    void pause();
    void resume();
    void stop();
  }

  /**
   * Deployment status information.
   */
  interface DeploymentStatus {
    String getDeploymentId();
    ComponentSimple getComponent();
    ComponentVersion getVersion();
    DeploymentState getState();
    Instant getDeploymentTime();
    List<DeploymentTarget> getTargets();
    Map<String, String> getCurrentVersions();
    HealthStatus getHealthStatus();
    ResourceUsageStatus getResourceUsage();
  }

  /**
   * Deployment history for a component.
   */
  interface DeploymentHistory {
    String getComponentId();
    List<HistoricalDeployment> getDeployments();
    int getTotalDeployments();
    int getSuccessfulDeployments();
    int getFailedDeployments();
    ComponentVersion getCurrentVersion();
    Optional<HistoricalDeployment> getLastDeployment();
  }

  /**
   * Historical deployment record.
   */
  interface HistoricalDeployment {
    String getDeploymentId();
    ComponentVersion getVersion();
    Instant getDeploymentTime();
    java.time.Duration getDeploymentDuration();
    boolean wasSuccessful();
    List<String> getTargetEnvironments();
    Optional<String> getDeployedBy();
    Map<String, Object> getDeploymentMetrics();
  }

  /**
   * Deployment environment.
   */
  interface DeploymentEnvironment {
    String getEnvironmentId();
    String getName();
    EnvironmentType getType();
    Map<String, String> getProperties();
    List<String> getSupportedPlatforms();
    ResourceCapacity getResourceCapacity();
    boolean isActive();
    List<ActiveDeployment> getActiveDeployments();
  }

  /**
   * Deployment pipeline for automated deployments.
   */
  interface DeploymentPipeline {
    String getPipelineId();
    String getName();
    PipelineConfig getConfiguration();
    List<PipelineStage> getStages();
    PipelineStatus getStatus();
    List<PipelineExecution> getExecutionHistory();
  }

  /**
   * Active deployment information.
   */
  interface ActiveDeployment {
    String getDeploymentId();
    ComponentSimple getComponent();
    ComponentVersion getVersion();
    String getEnvironment();
    Instant getDeploymentTime();
    DeploymentState getState();
    HealthStatus getHealthStatus();
    ResourceUsage getResourceUsage();
  }

  /**
   * Deployment system statistics.
   */
  interface DeploymentStatistics {
    int getTotalDeployments();
    int getActiveDeployments();
    int getSuccessfulDeployments();
    int getFailedDeployments();
    double getSuccessRate();
    java.time.Duration getAverageDeploymentTime();
    Map<String, Integer> getDeploymentsByEnvironment();
    Map<String, Integer> getDeploymentsByComponent();
    long getTotalUptime();
  }

  /**
   * Deployment event listener interface.
   */
  interface DeploymentEventListener {
    void onDeploymentStarted(String deploymentId, ComponentSimple component);
    void onDeploymentCompleted(DeploymentResult result);
    void onDeploymentFailed(String deploymentId, Exception error);
    void onRollbackStarted(String rollbackId, String deploymentId);
    void onRollbackCompleted(RollbackResult result);
    void onHealthCheckFailed(String deploymentId, HealthCheckResult result);
    void onScalingEvent(String deploymentId, ScalingResult result);
  }

  // Enums and supporting types
  enum DeploymentState { PENDING, DEPLOYING, DEPLOYED, FAILED, ROLLING_BACK, ROLLED_BACK }
  enum DeploymentTargetType { PRODUCTION, STAGING, DEVELOPMENT, TEST, CANARY }
  enum RolloutStatus { STARTING, IN_PROGRESS, COMPLETED, FAILED, PAUSED }
  enum EnvironmentType { CLOUD, ON_PREMISE, HYBRID, EDGE }
  enum PipelineStatus { IDLE, RUNNING, COMPLETED, FAILED, PAUSED }
  enum HealthStatus { HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN }

  // Configuration and data interfaces
  interface DeploymentConfig {}
  interface PackageConfig {}
  interface PackageValidationConfig {}
  interface DeploymentOptions {}
  interface RolloutStrategy {}
  interface RollbackConfig {}
  interface ConfigurationUpdate {}
  interface ConfigurationUpdateResult {}
  interface ScalingConfig {}
  interface ScalingResult {}
  interface HealthCheckConfig {}
  interface EnvironmentConfig {}
  interface PipelineConfig {}
  interface PipelineExecutionConfig {}
  interface PipelineExecutionResult {}

  interface PackageValidationResult {}
  interface RolloutStage {}
  interface DeploymentHealthStatus {}
  interface HealthCheckResult {}
  interface HealthStatistics {}
  interface ResourceUsageStatus {}
  interface ResourceRequirements {}
  interface ResourceCapacity {}
  interface PipelineStage {}
  interface PipelineExecution {}
}