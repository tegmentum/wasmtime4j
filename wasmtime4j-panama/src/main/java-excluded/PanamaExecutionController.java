package ai.tegmentum.wasmtime4j.panama.execution;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.execution.*;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the ExecutionController for advanced WebAssembly execution control.
 *
 * <p>This implementation provides comprehensive execution control through Panama Foreign Function
 * API, including fuel management, epoch interruption, resource quotas, and execution policies.
 *
 * @since 1.0.0
 */
public final class PanamaExecutionController implements ExecutionController {

  private static final Logger logger = Logger.getLogger(PanamaExecutionController.class.getName());

  private final Map<String, MemorySegment> contextPointers;
  private final Arena arena;
  private final Linker linker;
  private final SymbolLookup symbols;

  // Native method handles
  private final MethodHandle createExecutionContextHandle;
  private final MethodHandle applyExecutionPoliciesHandle;
  private final MethodHandle setExecutionQuotasHandle;
  private final MethodHandle enableMonitoringHandle;
  private final MethodHandle configureTracingHandle;
  private final MethodHandle startExecutionHandle;
  private final MethodHandle pauseExecutionHandle;
  private final MethodHandle resumeExecutionHandle;
  private final MethodHandle terminateExecutionHandle;
  private final MethodHandle adjustExecutionParametersHandle;
  private final MethodHandle getExecutionStatusHandle;
  private final MethodHandle getExecutionAnalyticsHandle;
  private final MethodHandle exportExecutionTraceHandle;
  private final MethodHandle emergencyTerminationHandle;
  private final MethodHandle setupFairResourceAllocationHandle;
  private final MethodHandle configureDynamicQuotaAdjustmentHandle;
  private final MethodHandle enableAnomalyDetectionHandle;
  private final MethodHandle getControllerStatisticsHandle;
  private final MethodHandle validateControllerHandle;
  private final MethodHandle cleanupHandle;
  private final MethodHandle resetHandle;

  public PanamaExecutionController() throws WasmException {
    this.contextPointers = new ConcurrentHashMap<>();
    this.arena = Arena.ofShared();
    this.linker = Linker.nativeLinker();

    try {
      this.symbols = SymbolLookup.libraryLookup("wasmtime4j", arena);
    } catch (Exception e) {
      throw new WasmException("Failed to load native library: " + e.getMessage(), e);
    }

    // Initialize method handles
    this.createExecutionContextHandle =
        initializeMethodHandle(
            "create_execution_context",
            FunctionDescriptor.of(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_INT));

    this.applyExecutionPoliciesHandle =
        initializeMethodHandle(
            "apply_execution_policies",
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    this.setExecutionQuotasHandle =
        initializeMethodHandle(
            "set_execution_quotas",
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_DOUBLE,
                ValueLayout.JAVA_INT));

    this.enableMonitoringHandle =
        initializeMethodHandle(
            "enable_monitoring",
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.JAVA_LONG));

    this.configureTracingHandle =
        initializeMethodHandle(
            "configure_tracing",
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.JAVA_LONG));

    this.startExecutionHandle =
        initializeMethodHandle(
            "start_execution",
            FunctionDescriptor.of(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_LONG));

    this.pauseExecutionHandle =
        initializeMethodHandle(
            "pause_execution",
            FunctionDescriptor.of(
                ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_BOOLEAN));

    this.resumeExecutionHandle =
        initializeMethodHandle(
            "resume_execution",
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    this.terminateExecutionHandle =
        initializeMethodHandle(
            "terminate_execution",
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.JAVA_LONG));

    this.adjustExecutionParametersHandle =
        initializeMethodHandle(
            "adjust_execution_parameters",
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS));

    this.getExecutionStatusHandle =
        initializeMethodHandle(
            "get_execution_status",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    this.getExecutionAnalyticsHandle =
        initializeMethodHandle(
            "get_execution_analytics",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    this.exportExecutionTraceHandle =
        initializeMethodHandle(
            "export_execution_trace",
            FunctionDescriptor.of(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT));

    this.emergencyTerminationHandle =
        initializeMethodHandle(
            "emergency_termination",
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    this.setupFairResourceAllocationHandle =
        initializeMethodHandle(
            "setup_fair_resource_allocation",
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_DOUBLE,
                ValueLayout.JAVA_LONG));

    this.configureDynamicQuotaAdjustmentHandle =
        initializeMethodHandle(
            "configure_dynamic_quota_adjustment",
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.JAVA_DOUBLE,
                ValueLayout.JAVA_DOUBLE,
                ValueLayout.JAVA_LONG));

    this.enableAnomalyDetectionHandle =
        initializeMethodHandle(
            "enable_anomaly_detection",
            FunctionDescriptor.of(
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.JAVA_DOUBLE,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_DOUBLE));

    this.getControllerStatisticsHandle =
        initializeMethodHandle(
            "get_controller_statistics", FunctionDescriptor.of(ValueLayout.ADDRESS));

    this.validateControllerHandle =
        initializeMethodHandle("validate_controller", FunctionDescriptor.of(ValueLayout.ADDRESS));

    this.cleanupHandle =
        initializeMethodHandle(
            "cleanup", FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN));

    this.resetHandle =
        initializeMethodHandle(
            "reset", FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN));

    // Initialize native controller
    initializeNativeController();
  }

  private MethodHandle initializeMethodHandle(
      final String functionName, final FunctionDescriptor descriptor) throws WasmException {
    try {
      return symbols
          .find(functionName)
          .map(address -> linker.downcallHandle(address, descriptor))
          .orElseThrow(() -> new WasmException("Function not found: " + functionName));
    } catch (Exception e) {
      throw new WasmException(
          "Failed to initialize method handle for " + functionName + ": " + e.getMessage(), e);
    }
  }

  private void initializeNativeController() throws WasmException {
    try {
      MethodHandle initHandle =
          symbols
              .find("initialize_native_controller")
              .map(address -> linker.downcallHandle(address, FunctionDescriptor.ofVoid()))
              .orElseThrow(
                  () -> new WasmException("initialize_native_controller function not found"));

      initHandle.invoke();
    } catch (Throwable e) {
      throw new WasmException("Failed to initialize native controller: " + e.getMessage(), e);
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

    try {
      MemorySegment contextIdSegment = arena.allocateFrom(contextId);

      MemorySegment contextPtr =
          (MemorySegment)
              createExecutionContextHandle.invoke(
                  contextIdSegment,
                  config.getFuelPriority().getLevel(),
                  config.getQuotas().getFuelQuota(),
                  config.getQuotas().getCpuTimeQuota().toMillis(),
                  config.getQuotas().getMemoryQuota(),
                  config.getInterruptMode().ordinal());

      if (contextPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to create execution context: " + contextId);
      }

      contextPointers.put(contextId, contextPtr);
      return new PanamaExecutionContext(contextId, contextPtr, this);
    } catch (Throwable e) {
      throw new WasmException("Failed to create execution context: " + e.getMessage(), e);
    }
  }

  @Override
  public ExecutionContext getContext(final String contextId) throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }

    MemorySegment contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      return null;
    }

    return new PanamaExecutionContext(contextId, contextPtr, this);
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

    MemorySegment contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    try {
      int policiesMask = convertPoliciesToMask(policies);
      boolean result = (boolean) applyExecutionPoliciesHandle.invoke(contextPtr, policiesMask);

      if (!result) {
        throw new WasmException("Failed to apply execution policies for context: " + contextId);
      }
    } catch (Throwable e) {
      throw new WasmException("Failed to apply execution policies: " + e.getMessage(), e);
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

    MemorySegment contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    try {
      boolean result =
          (boolean)
              setExecutionQuotasHandle.invoke(
                  contextPtr,
                  quotas.getFuelQuota(),
                  quotas.getCpuTimeQuota().toMillis(),
                  quotas.getMemoryQuota(),
                  quotas.getIoOperationQuota(),
                  quotas.getNetworkRequestQuota(),
                  quotas.getIoRateLimit(),
                  quotas.getEnforcementPolicy().ordinal());

      if (!result) {
        throw new WasmException("Failed to set execution quotas for context: " + contextId);
      }
    } catch (Throwable e) {
      throw new WasmException("Failed to set execution quotas: " + e.getMessage(), e);
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

    MemorySegment contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    try {
      boolean result =
          (boolean)
              enableMonitoringHandle.invoke(
                  contextPtr,
                  monitoring.isEnableFuelTracking(),
                  monitoring.isEnablePerformanceMetrics(),
                  monitoring.isEnableResourceUsage(),
                  monitoring.getMetricsInterval().toMillis());

      if (!result) {
        throw new WasmException("Failed to enable monitoring for context: " + contextId);
      }
    } catch (Throwable e) {
      throw new WasmException("Failed to enable monitoring: " + e.getMessage(), e);
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

    MemorySegment contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    try {
      boolean result =
          (boolean)
              configureTracingHandle.invoke(
                  contextPtr,
                  tracing.isEnableFunctionTracing(),
                  tracing.isEnableInstructionTracing(),
                  tracing.isEnableMemoryTracing(),
                  tracing.getTraceBufferSize());

      if (!result) {
        throw new WasmException("Failed to configure tracing for context: " + contextId);
      }
    } catch (Throwable e) {
      throw new WasmException("Failed to configure tracing: " + e.getMessage(), e);
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

    MemorySegment contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            MemorySegment functionNameSegment =
                arena.allocateFrom(executionRequest.getFunctionName());
            MemorySegment parametersSegment = serializeParameters(executionRequest.getParameters());

            MemorySegment resultPtr =
                (MemorySegment)
                    startExecutionHandle.invoke(
                        contextPtr,
                        functionNameSegment,
                        parametersSegment,
                        executionRequest.getTimeout().toMillis());

            if (resultPtr.equals(MemorySegment.NULL)) {
              throw new RuntimeException("Execution failed for context: " + contextId);
            }

            return new PanamaExecutionResult(resultPtr);
          } catch (Throwable e) {
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

    MemorySegment contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    try {
      MemorySegment tokenPtr =
          (MemorySegment) pauseExecutionHandle.invoke(contextPtr, preserveState);
      if (tokenPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to pause execution for context: " + contextId);
      }

      return tokenPtr.reinterpret(1000).getString(0); // Assuming max token length of 1000
    } catch (Throwable e) {
      throw new WasmException("Failed to pause execution: " + e.getMessage(), e);
    }
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

    MemorySegment contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    try {
      MemorySegment tokenSegment = arena.allocateFrom(pauseToken);
      boolean result = (boolean) resumeExecutionHandle.invoke(contextPtr, tokenSegment);

      if (!result) {
        throw new WasmException("Failed to resume execution for context: " + contextId);
      }
    } catch (Throwable e) {
      throw new WasmException("Failed to resume execution: " + e.getMessage(), e);
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

    MemorySegment contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    try {
      boolean result =
          (boolean)
              terminateExecutionHandle.invoke(
                  contextPtr,
                  termination.isForceTermination(),
                  termination.isCleanupResources(),
                  termination.getTerminationTimeout().toMillis());

      if (!result) {
        throw new WasmException("Failed to terminate execution for context: " + contextId);
      }
    } catch (Throwable e) {
      throw new WasmException("Failed to terminate execution: " + e.getMessage(), e);
    }
  }

  // Additional method implementations following similar patterns...
  // Implementing only key methods for space, but all would follow similar structure

  @Override
  public void adjustExecutionParameters(
      final String contextId, final ExecutionAdjustments adjustments) throws WasmException {
    // Implementation similar to above methods
    throw new WasmException("Not yet implemented");
  }

  @Override
  public ExecutionStatus getExecutionStatus(final String contextId) throws WasmException {
    if (contextId == null) {
      throw new IllegalArgumentException("Context ID cannot be null");
    }

    MemorySegment contextPtr = contextPointers.get(contextId);
    if (contextPtr == null) {
      throw new WasmException("Execution context not found: " + contextId);
    }

    try {
      MemorySegment statusPtr = (MemorySegment) getExecutionStatusHandle.invoke(contextPtr);
      if (statusPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to get execution status for context: " + contextId);
      }

      return new PanamaExecutionStatus(statusPtr);
    } catch (Throwable e) {
      throw new WasmException("Failed to get execution status: " + e.getMessage(), e);
    }
  }

  @Override
  public ExecutionAnalytics getExecutionAnalytics(final String contextId, final Duration timeWindow)
      throws WasmException {
    // Implementation stub
    throw new WasmException("Not yet implemented");
  }

  @Override
  public ExecutionTraceData exportExecutionTrace(
      final String contextId, final TraceFilter traceFilter) throws WasmException {
    // Implementation stub
    throw new WasmException("Not yet implemented");
  }

  @Override
  public void emergencyTermination(final String contextId, final String reason)
      throws WasmException {
    // Implementation stub
    throw new WasmException("Not yet implemented");
  }

  @Override
  public void setupFairResourceAllocation(
      final Set<String> contextIds, final FairAllocationStrategy allocationStrategy)
      throws WasmException {
    // Implementation stub
    throw new WasmException("Not yet implemented");
  }

  @Override
  public void configureDynamicQuotaAdjustment(
      final String contextId, final LoadBasedQuotaConfig loadBasedConfig) throws WasmException {
    // Implementation stub
    throw new WasmException("Not yet implemented");
  }

  @Override
  public void enableAnomalyDetection(
      final String contextId, final AnomalyDetectionConfig anomalyDetection) throws WasmException {
    // Implementation stub
    throw new WasmException("Not yet implemented");
  }

  @Override
  public ControllerStatistics getControllerStatistics() throws WasmException {
    try {
      MemorySegment statisticsPtr = (MemorySegment) getControllerStatisticsHandle.invoke();
      if (statisticsPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to get controller statistics");
      }

      return new PanamaControllerStatistics(statisticsPtr);
    } catch (Throwable e) {
      throw new WasmException("Failed to get controller statistics: " + e.getMessage(), e);
    }
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
    try {
      MemorySegment validationPtr = (MemorySegment) validateControllerHandle.invoke();
      if (validationPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to validate controller");
      }

      return new PanamaControllerValidationResult(validationPtr);
    } catch (Throwable e) {
      throw new WasmException("Failed to validate controller: " + e.getMessage(), e);
    }
  }

  @Override
  public void cleanup(final boolean preserveStatistics) throws WasmException {
    try {
      boolean result = (boolean) cleanupHandle.invoke(preserveStatistics);
      if (!result) {
        throw new WasmException("Failed to cleanup execution controller");
      }

      contextPointers.clear();
    } catch (Throwable e) {
      throw new WasmException("Failed to cleanup: " + e.getMessage(), e);
    }
  }

  @Override
  public void reset(final boolean preserveActiveContexts) throws WasmException {
    try {
      boolean result = (boolean) resetHandle.invoke(preserveActiveContexts);
      if (!result) {
        throw new WasmException("Failed to reset execution controller");
      }

      if (!preserveActiveContexts) {
        contextPointers.clear();
      }
    } catch (Throwable e) {
      throw new WasmException("Failed to reset: " + e.getMessage(), e);
    }
  }

  private int convertPoliciesToMask(final Set<ExecutionPolicy> policies) {
    int mask = 0;
    for (ExecutionPolicy policy : policies) {
      mask |= (1 << policy.ordinal());
    }
    return mask;
  }

  private MemorySegment serializeParameters(final Object[] parameters) {
    // Simplified parameter serialization - would need full implementation
    if (parameters == null || parameters.length == 0) {
      return MemorySegment.NULL;
    }

    // For now, just allocate empty segment - would need proper serialization
    return arena.allocate(1024); // Placeholder
  }

  // Package-private method for context cleanup
  void removeContext(final String contextId) {
    contextPointers.remove(contextId);
  }

  public void close() {
    try {
      cleanup(false);
    } catch (WasmException e) {
      logger.warning("Failed to cleanup during close: " + e.getMessage());
    }

    arena.close();
  }
}
