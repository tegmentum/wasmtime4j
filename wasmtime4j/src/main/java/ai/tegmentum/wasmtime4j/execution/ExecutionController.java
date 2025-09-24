package ai.tegmentum.wasmtime4j.execution;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Comprehensive execution controller for WebAssembly runtime management.
 *
 * <p>The ExecutionController provides unified control over fuel management,
 * epoch interruption, resource quotas, and execution policies for WebAssembly
 * contexts. It integrates all execution control mechanisms into a cohesive
 * management interface.
 *
 * @since 1.0.0
 */
public interface ExecutionController {

  /**
   * Creates a new execution context with specified control parameters.
   *
   * @param contextId unique identifier for the execution context
   * @param config execution context configuration
   * @return execution context handle for management operations
   * @throws WasmException if context creation fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  ExecutionContext createContext(String contextId, ExecutionContextConfig config) throws WasmException;

  /**
   * Gets an existing execution context by identifier.
   *
   * @param contextId execution context identifier
   * @return execution context handle, or null if not found
   * @throws WasmException if context retrieval fails
   * @throws IllegalArgumentException if contextId is null
   */
  ExecutionContext getContext(String contextId) throws WasmException;

  /**
   * Applies execution control policies to a context.
   *
   * @param contextId execution context identifier
   * @param policies set of execution policies to apply
   * @throws WasmException if policy application fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void applyExecutionPolicies(String contextId, Set<ExecutionPolicy> policies) throws WasmException;

  /**
   * Sets execution quotas for resource management and enforcement.
   *
   * @param contextId execution context identifier
   * @param quotas resource quotas configuration
   * @throws WasmException if quota setting fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void setExecutionQuotas(String contextId, ExecutionQuotas quotas) throws WasmException;

  /**
   * Enables comprehensive execution monitoring and analytics.
   *
   * @param contextId execution context identifier
   * @param monitoring monitoring configuration
   * @throws WasmException if monitoring setup fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void enableMonitoring(String contextId, ExecutionMonitoringConfig monitoring) throws WasmException;

  /**
   * Configures execution debugging and tracing capabilities.
   *
   * @param contextId execution context identifier
   * @param tracing debugging and tracing configuration
   * @throws WasmException if tracing setup fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void configureTracing(String contextId, ExecutionTracingConfig tracing) throws WasmException;

  /**
   * Starts execution of WebAssembly code within a controlled context.
   *
   * @param contextId execution context identifier
   * @param executionRequest execution parameters and configuration
   * @return execution result future
   * @throws WasmException if execution start fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  CompletableFuture<ExecutionResult> startExecution(String contextId, ExecutionRequest executionRequest) throws WasmException;

  /**
   * Pauses execution in a context with optional state preservation.
   *
   * @param contextId execution context identifier
   * @param preserveState true to preserve full execution state
   * @return pause token for resuming execution later
   * @throws WasmException if pause fails
   * @throws IllegalArgumentException if contextId is invalid
   */
  String pauseExecution(String contextId, boolean preserveState) throws WasmException;

  /**
   * Resumes previously paused execution using a pause token.
   *
   * @param contextId execution context identifier
   * @param pauseToken token returned from pauseExecution
   * @throws WasmException if resume fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void resumeExecution(String contextId, String pauseToken) throws WasmException;

  /**
   * Terminates execution in a context with configurable cleanup behavior.
   *
   * @param contextId execution context identifier
   * @param termination termination configuration and cleanup options
   * @throws WasmException if termination fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void terminateExecution(String contextId, ExecutionTerminationConfig termination) throws WasmException;

  /**
   * Adjusts execution parameters dynamically during runtime.
   *
   * @param contextId execution context identifier
   * @param adjustments dynamic parameter adjustments
   * @throws WasmException if adjustments fail
   * @throws IllegalArgumentException if parameters are invalid
   */
  void adjustExecutionParameters(String contextId, ExecutionAdjustments adjustments) throws WasmException;

  /**
   * Gets comprehensive execution status for a context.
   *
   * @param contextId execution context identifier
   * @return detailed execution status and metrics
   * @throws WasmException if status retrieval fails
   * @throws IllegalArgumentException if contextId is invalid
   */
  ExecutionStatus getExecutionStatus(String contextId) throws WasmException;

  /**
   * Gets execution analytics and performance metrics.
   *
   * @param contextId execution context identifier
   * @param timeWindow time window for analytics aggregation
   * @return execution analytics and performance data
   * @throws WasmException if analytics retrieval fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  ExecutionAnalytics getExecutionAnalytics(String contextId, Duration timeWindow) throws WasmException;

  /**
   * Exports execution traces for debugging and optimization analysis.
   *
   * @param contextId execution context identifier
   * @param traceFilter filter criteria for trace export
   * @return execution trace data in specified format
   * @throws WasmException if trace export fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  ExecutionTraceData exportExecutionTrace(String contextId, TraceFilter traceFilter) throws WasmException;

  /**
   * Enables emergency execution termination with immediate effect.
   *
   * @param contextId execution context identifier
   * @param reason reason for emergency termination
   * @throws WasmException if emergency termination fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void emergencyTermination(String contextId, String reason) throws WasmException;

  /**
   * Sets up fair resource allocation between multiple execution contexts.
   *
   * @param contextIds set of contexts for fair allocation
   * @param allocationStrategy fair allocation strategy configuration
   * @throws WasmException if fair allocation setup fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void setupFairResourceAllocation(Set<String> contextIds, FairAllocationStrategy allocationStrategy) throws WasmException;

  /**
   * Configures dynamic quota adjustment based on system load.
   *
   * @param contextId execution context identifier
   * @param loadBasedConfig load-based adjustment configuration
   * @throws WasmException if configuration fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void configureDynamicQuotaAdjustment(String contextId, LoadBasedQuotaConfig loadBasedConfig) throws WasmException;

  /**
   * Enables execution anomaly detection and alerting.
   *
   * @param contextId execution context identifier
   * @param anomalyDetection anomaly detection configuration
   * @throws WasmException if anomaly detection setup fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void enableAnomalyDetection(String contextId, AnomalyDetectionConfig anomalyDetection) throws WasmException;

  /**
   * Gets execution controller statistics across all managed contexts.
   *
   * @return comprehensive controller statistics and health metrics
   * @throws WasmException if statistics retrieval fails
   */
  ControllerStatistics getControllerStatistics() throws WasmException;

  /**
   * Gets all active execution contexts managed by this controller.
   *
   * @return map of context IDs to their current execution states
   */
  Map<String, ExecutionState> getActiveContexts();

  /**
   * Validates execution controller state and configuration consistency.
   *
   * @return validation result with any detected issues
   * @throws WasmException if validation fails
   */
  ControllerValidationResult validate() throws WasmException;

  /**
   * Performs comprehensive cleanup of execution controller resources.
   *
   * @param preserveStatistics true to preserve historical statistics
   * @throws WasmException if cleanup fails
   */
  void cleanup(boolean preserveStatistics) throws WasmException;

  /**
   * Resets execution controller to default configuration.
   *
   * @param preserveActiveContexts true to preserve currently active contexts
   * @throws WasmException if reset fails
   */
  void reset(boolean preserveActiveContexts) throws WasmException;
}