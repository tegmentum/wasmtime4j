package ai.tegmentum.wasmtime4j.jni.execution;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.execution.*;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * JNI implementation of the ExecutionController for advanced WebAssembly execution control.
 *
 * <p>This implementation provides comprehensive execution control through native bindings,
 * including fuel management, epoch interruption, resource quotas, and execution policies.
 *
 * @since 1.0.0
 */
public final class JniExecutionController implements ExecutionController {

  private static final Logger logger = Logger.getLogger(JniExecutionController.class.getName());

  private final Map<String, Long> contextPointers;
  private final boolean isNativeLoaded;

  public JniExecutionController() {
    this.contextPointers = new ConcurrentHashMap<>();
    this.isNativeLoaded = loadNativeLibrary();
    if (isNativeLoaded) {
      initializeNativeController();
    }
  }

  private boolean loadNativeLibrary() {
    try {
      System.loadLibrary("wasmtime4j");
      return true;
    } catch (UnsatisfiedLinkError e) {
      logger.severe("Failed to load native library: " + e.getMessage());
      return false;
    }
  }

  private void checkNativeLibrary() throws WasmException {
    if (!isNativeLoaded) {
      throw new WasmException("Native library not loaded");
    }
  }

  @Override
  public ExecutionContext createContext(final String contextId, final ExecutionContextConfig config)
      throws WasmException {
    if (contextId == null || contextId.trim().isEmpty()) {
      throw new IllegalArgumentException("Context ID cannot be null or empty");
    }
    if (config == null) {
      throw new IllegalArgumentException("Execution context config cannot be null");
    }

    checkNativeLibrary();

    long contextPtr =
        nativeCreateExecutionContext(
            contextId,
            config.getFuelPriority().getLevel(),
            config.getQuotas().getFuelQuota(),
            config.getQuotas().getCpuTimeQuota().toMillis(),
            config.getQuotas().getMemoryQuota(),
            config.getInterruptMode().ordinal());

    if (contextPtr == 0) {
      throw new WasmException("Failed to create execution context: " + contextId);
    }

    contextPointers.put(contextId, contextPtr);
    return new JniExecutionContext(contextId, contextPtr, this);
  }

  @Override
  public ExecutionContext getContext(final String contextId) throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }

    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      return null;
    }

    return new JniExecutionContext(contextId, contextPtr, this);
  }

  @Override
  public void applyExecutionPolicies(final String contextId, final Set<ExecutionPolicy> policies)
      throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }
    if (policies == null) {
      throw new IllegalArgumentException("Execution policies cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    // Convert policies to native representation
    int policiesMask = convertPoliciesToMask(policies);

    if (!nativeApplyExecutionPolicies(contextPtr, policiesMask)) {
      throw new WasmException("Failed to apply execution policies for context: " + contextId);
    }
  }

  @Override
  public void setExecutionQuotas(final String contextId, final ExecutionQuotas quotas)
      throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }
    if (quotas == null) {
      throw new IllegalArgumentException("Execution quotas cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    if (!nativeSetExecutionQuotas(
        contextPtr,
        quotas.getFuelQuota(),
        quotas.getCpuTimeQuota().toMillis(),
        quotas.getMemoryQuota(),
        quotas.getIoOperationQuota(),
        quotas.getNetworkRequestQuota(),
        quotas.getIoRateLimit(),
        quotas.getEnforcementPolicy().ordinal())) {
      throw new WasmException("Failed to set execution quotas for context: " + contextId);
    }
  }

  @Override
  public void enableMonitoring(final String contextId, final ExecutionMonitoringConfig monitoring)
      throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }
    if (monitoring == null) {
      throw new IllegalArgumentException("Monitoring config cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    if (!nativeEnableMonitoring(
        contextPtr,
        monitoring.isEnableFuelTracking(),
        monitoring.isEnablePerformanceMetrics(),
        monitoring.isEnableResourceUsage(),
        monitoring.getMetricsInterval().toMillis())) {
      throw new WasmException("Failed to enable monitoring for context: " + contextId);
    }
  }

  @Override
  public void configureTracing(final String contextId, final ExecutionTracingConfig tracing)
      throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }
    if (tracing == null) {
      throw new IllegalArgumentException("Tracing config cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    if (!nativeConfigureTracing(
        contextPtr,
        tracing.isEnableFunctionTracing(),
        tracing.isEnableInstructionTracing(),
        tracing.isEnableMemoryTracing(),
        tracing.getTraceBufferSize())) {
      throw new WasmException("Failed to configure tracing for context: " + contextId);
    }
  }

  @Override
  public CompletableFuture<ExecutionResult> startExecution(
      final String contextId, final ExecutionRequest executionRequest) throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }
    if (executionRequest == null) {
      throw new IllegalArgumentException("Execution request cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            long resultPtr =
                nativeStartExecution(
                    contextPtr,
                    executionRequest.getFunctionName(),
                    executionRequest.getParameters(),
                    executionRequest.getTimeout().toMillis());

            if (resultPtr == 0) {
              throw new RuntimeException("Execution failed for context: " + contextId);
            }

            return new JniExecutionResult(resultPtr);
          } catch (Exception e) {
            throw new RuntimeException("Execution error: " + e.getMessage(), e);
          }
        });
  }

  @Override
  public String pauseExecution(final String contextId, final boolean preserveState)
      throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    String pauseToken = nativePauseExecution(contextPtr, preserveState);
    if (pauseToken == null || pauseToken.isEmpty()) {
      throw new WasmException("Failed to pause execution for context: " + contextId);
    }

    return pauseToken;
  }

  @Override
  public void resumeExecution(final String contextId, final String pauseToken)
      throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }
    if (pauseToken == null || pauseToken.isEmpty()) {
      throw new IllegalArgumentException("Pause token cannot be null or empty");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    if (!nativeResumeExecution(contextPtr, pauseToken)) {
      throw new WasmException("Failed to resume execution for context: " + contextId);
    }
  }

  @Override
  public void terminateExecution(
      final String contextId, final ExecutionTerminationConfig termination) throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }
    if (termination == null) {
      throw new IllegalArgumentException("Termination config cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    if (!nativeTerminateExecution(
        contextPtr,
        termination.isForceTermination(),
        termination.isCleanupResources(),
        termination.getTerminationTimeout().toMillis())) {
      throw new WasmException("Failed to terminate execution for context: " + contextId);
    }
  }

  @Override
  public void adjustExecutionParameters(
      final String contextId, final ExecutionAdjustments adjustments) throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }
    if (adjustments == null) {
      throw new IllegalArgumentException("Execution adjustments cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    if (!nativeAdjustExecutionParameters(
        contextPtr,
        adjustments.getFuelAdjustment(),
        adjustments.getPriorityAdjustment().getLevel(),
        adjustments.getResourceAdjustments())) {
      throw new WasmException("Failed to adjust execution parameters for context: " + contextId);
    }
  }

  @Override
  public ExecutionStatus getExecutionStatus(final String contextId) throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    long statusPtr = nativeGetExecutionStatus(contextPtr);
    if (statusPtr == 0) {
      throw new WasmException("Failed to get execution status for context: " + contextId);
    }

    return new JniExecutionStatus(statusPtr);
  }

  @Override
  public ExecutionAnalytics getExecutionAnalytics(final String contextId, final Duration timeWindow)
      throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }
    if (timeWindow == null) {
      throw new IllegalArgumentException("Time window cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    long analyticsPtr = nativeGetExecutionAnalytics(contextPtr, timeWindow.toMillis());
    if (analyticsPtr == 0) {
      throw new WasmException("Failed to get execution analytics for context: " + contextId);
    }

    return new JniExecutionAnalytics(analyticsPtr);
  }

  @Override
  public ExecutionTraceData exportExecutionTrace(
      final String contextId, final TraceFilter traceFilter) throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }
    if (traceFilter == null) {
      throw new IllegalArgumentException("Trace filter cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    long traceDataPtr =
        nativeExportExecutionTrace(
            contextPtr,
            traceFilter.getStartTime().toEpochMilli(),
            traceFilter.getEndTime().toEpochMilli(),
            traceFilter.getTraceTypes(),
            traceFilter.getMaxEntries());

    if (traceDataPtr == 0) {
      throw new WasmException("Failed to export execution trace for context: " + contextId);
    }

    return new JniExecutionTraceData(traceDataPtr);
  }

  @Override
  public void emergencyTermination(final String contextId, final String reason)
      throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }
    if (reason == null) {
      throw new IllegalArgumentException("Reason cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    if (!nativeEmergencyTermination(contextPtr, reason)) {
      throw new WasmException("Failed to perform emergency termination for context: " + contextId);
    }
  }

  @Override
  public void setupFairResourceAllocation(
      final Set<String> contextIds, final FairAllocationStrategy allocationStrategy)
      throws WasmException {
    if (contextIds == null || contextIds.isEmpty()) {
      throw new IllegalArgumentException("Context IDs cannot be null or empty");
    }
    if (allocationStrategy == null) {
      throw new IllegalArgumentException("Allocation strategy cannot be null");
    }

    checkNativeLibrary();

    String[] contextArray = contextIds.toArray(new String[0]);
    long[] contextPtrArray = new long[contextArray.length];

    for (int i = 0; i < contextArray.length; i++) {
      Long ptr = contextPointers.get(contextArray[i]);
      if (ptr == null) {
        throw new WasmException("Execution context not found: " + contextArray[i]);
      }
      contextPtrArray[i] = ptr;
    }

    if (!nativeSetupFairResourceAllocation(
        contextPtrArray,
        allocationStrategy.getAllocationMode().ordinal(),
        allocationStrategy.getFairnessWeight(),
        allocationStrategy.getRebalanceInterval().toMillis())) {
      throw new WasmException("Failed to setup fair resource allocation");
    }
  }

  @Override
  public void configureDynamicQuotaAdjustment(
      final String contextId, final LoadBasedQuotaConfig loadBasedConfig) throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }
    if (loadBasedConfig == null) {
      throw new IllegalArgumentException("Load-based config cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    if (!nativeConfigureDynamicQuotaAdjustment(
        contextPtr,
        loadBasedConfig.isEnabled(),
        loadBasedConfig.getLoadThreshold(),
        loadBasedConfig.getAdjustmentFactor(),
        loadBasedConfig.getAdjustmentInterval().toMillis())) {
      throw new WasmException(
          "Failed to configure dynamic quota adjustment for context: " + contextId);
    }
  }

  @Override
  public void enableAnomalyDetection(
      final String contextId, final AnomalyDetectionConfig anomalyDetection) throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }
    if (anomalyDetection == null) {
      throw new IllegalArgumentException("Anomaly detection config cannot be null");
    }

    checkNativeLibrary();
    Long contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    if (!nativeEnableAnomalyDetection(
        contextPtr,
        anomalyDetection.isEnabled(),
        anomalyDetection.getSensitivity(),
        anomalyDetection.getDetectionWindow().toMillis(),
        anomalyDetection.getAnomalyThreshold())) {
      throw new WasmException("Failed to enable anomaly detection for context: " + contextId);
    }
  }

  @Override
  public ControllerStatistics getControllerStatistics() throws WasmException {
    checkNativeLibrary();

    long statisticsPtr = nativeGetControllerStatistics();
    if (statisticsPtr == 0) {
      throw new WasmException("Failed to get controller statistics");
    }

    return new JniControllerStatistics(statisticsPtr);
  }

  @Override
  public Map<String, ExecutionState> getActiveContexts() {
    return contextPointers.entrySet().stream()
        .collect(
            java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                  try {
                    return getExecutionStatus(entry.getKey()).getExecutionState();
                  } catch (WasmException e) {
                    logger.warning(
                        "Failed to get execution state for context "
                            + entry.getKey()
                            + ": "
                            + e.getMessage());
                    return null;
                  }
                },
                (existing, replacement) -> existing,
                ConcurrentHashMap::new));
  }

  @Override
  public ControllerValidationResult validate() throws WasmException {
    checkNativeLibrary();

    long validationPtr = nativeValidateController();
    if (validationPtr == 0) {
      throw new WasmException("Failed to validate controller");
    }

    return new JniControllerValidationResult(validationPtr);
  }

  @Override
  public void cleanup(final boolean preserveStatistics) throws WasmException {
    checkNativeLibrary();

    if (!nativeCleanup(preserveStatistics)) {
      throw new WasmException("Failed to cleanup execution controller");
    }

    contextPointers.clear();
  }

  @Override
  public void reset(final boolean preserveActiveContexts) throws WasmException {
    checkNativeLibrary();

    if (!nativeReset(preserveActiveContexts)) {
      throw new WasmException("Failed to reset execution controller");
    }

    if (!preserveActiveContexts) {
      contextPointers.clear();
    }
  }

  private int convertPoliciesToMask(final Set<ExecutionPolicy> policies) {
    int mask = 0;
    for (ExecutionPolicy policy : policies) {
      mask |= (1 << policy.ordinal());
    }
    return mask;
  }

  // Package-private method for context cleanup
  void removeContext(final String contextId) {
    contextPointers.remove(contextId);
  }

  // Native method declarations
  private native void initializeNativeController();

  private native long nativeCreateExecutionContext(
      String contextId,
      int fuelPriority,
      long fuelQuota,
      long cpuTimeQuotaMs,
      long memoryQuota,
      int interruptMode);

  private native boolean nativeApplyExecutionPolicies(long contextPtr, int policiesMask);

  private native boolean nativeSetExecutionQuotas(
      long contextPtr,
      long fuelQuota,
      long cpuTimeQuotaMs,
      long memoryQuota,
      long ioOperationQuota,
      long networkRequestQuota,
      double ioRateLimit,
      int enforcementPolicy);

  private native boolean nativeEnableMonitoring(
      long contextPtr,
      boolean enableFuelTracking,
      boolean enablePerformanceMetrics,
      boolean enableResourceUsage,
      long metricsIntervalMs);

  private native boolean nativeConfigureTracing(
      long contextPtr,
      boolean enableFunctionTracing,
      boolean enableInstructionTracing,
      boolean enableMemoryTracing,
      long traceBufferSize);

  private native long nativeStartExecution(
      long contextPtr, String functionName, Object[] parameters, long timeoutMs);

  private native String nativePauseExecution(long contextPtr, boolean preserveState);

  private native boolean nativeResumeExecution(long contextPtr, String pauseToken);

  private native boolean nativeTerminateExecution(
      long contextPtr,
      boolean forceTermination,
      boolean cleanupResources,
      long terminationTimeoutMs);

  private native boolean nativeAdjustExecutionParameters(
      long contextPtr, long fuelAdjustment, int priorityLevel, Object resourceAdjustments);

  private native long nativeGetExecutionStatus(long contextPtr);

  private native long nativeGetExecutionAnalytics(long contextPtr, long timeWindowMs);

  private native long nativeExportExecutionTrace(
      long contextPtr, long startTimeMs, long endTimeMs, int traceTypes, int maxEntries);

  private native boolean nativeEmergencyTermination(long contextPtr, String reason);

  private native boolean nativeSetupFairResourceAllocation(
      long[] contextPtrs, int allocationMode, double fairnessWeight, long rebalanceIntervalMs);

  private native boolean nativeConfigureDynamicQuotaAdjustment(
      long contextPtr,
      boolean enabled,
      double loadThreshold,
      double adjustmentFactor,
      long adjustmentIntervalMs);

  private native boolean nativeEnableAnomalyDetection(
      long contextPtr,
      boolean enabled,
      double sensitivity,
      long detectionWindowMs,
      double anomalyThreshold);

  private native long nativeGetControllerStatistics();

  private native long nativeValidateController();

  private native boolean nativeCleanup(boolean preserveStatistics);

  private native boolean nativeReset(boolean preserveActiveContexts);
}
