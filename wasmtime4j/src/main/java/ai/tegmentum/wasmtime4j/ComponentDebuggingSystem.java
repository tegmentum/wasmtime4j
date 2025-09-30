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

/**
 * Advanced component debugging and introspection system.
 *
 * <p>The ComponentDebuggingSystem provides comprehensive debugging capabilities including:
 *
 * <ul>
 *   <li>Component execution tracing and profiling
 *   <li>Interface call monitoring and logging
 *   <li>Component state inspection and debugging
 *   <li>Performance analysis and optimization hints
 *   <li>Real-time debugging with breakpoints
 *   <li>Component lifecycle event monitoring
 * </ul>
 *
 * @since 1.0.0
 */
public interface ComponentDebuggingSystem extends AutoCloseable {

  /**
   * Gets the debugging system identifier.
   *
   * @return the system ID
   */
  String getId();

  /**
   * Gets the debugging system configuration.
   *
   * @return the configuration
   */
  DebuggingConfig getConfiguration();

  /**
   * Starts debugging a component with specified configuration.
   *
   * @param component the component to debug
   * @param debugConfig debug configuration
   * @return debug session handle
   * @throws WasmException if debugging start fails
   */
  DebugSession startDebugging(ComponentSimple component, ComponentDebugConfig debugConfig)
      throws WasmException;

  /**
   * Stops debugging a component.
   *
   * @param session the debug session to stop
   * @throws WasmException if debugging stop fails
   */
  void stopDebugging(DebugSession session) throws WasmException;

  /**
   * Gets all active debug sessions.
   *
   * @return list of active debug sessions
   */
  List<DebugSession> getActiveSessions();

  /**
   * Attaches a debugger to a running component instance.
   *
   * @param componentInstance the component instance to attach to
   * @param attachConfig attachment configuration
   * @return debug session for the attached component
   * @throws WasmException if attachment fails
   */
  DebugSession attachDebugger(ComponentInstance componentInstance, DebugAttachConfig attachConfig)
      throws WasmException;

  /**
   * Detaches a debugger from a component instance.
   *
   * @param session the debug session to detach
   * @throws WasmException if detachment fails
   */
  void detachDebugger(DebugSession session) throws WasmException;

  /**
   * Sets up execution tracing for a component.
   *
   * @param component the component to trace
   * @param traceConfig trace configuration
   * @return execution tracer
   * @throws WasmException if tracing setup fails
   */
  ExecutionTracer setupExecutionTracing(ComponentSimple component, TraceConfig traceConfig)
      throws WasmException;

  /**
   * Sets up performance profiling for a component.
   *
   * @param component the component to profile
   * @param profileConfig profiling configuration
   * @return performance profiler
   * @throws WasmException if profiling setup fails
   */
  PerformanceProfiler setupPerformanceProfiling(
      ComponentSimple component, ProfileConfig profileConfig) throws WasmException;

  /**
   * Monitors interface calls for a component.
   *
   * @param component the component to monitor
   * @param monitorConfig monitoring configuration
   * @return interface call monitor
   * @throws WasmException if monitoring setup fails
   */
  InterfaceCallMonitor setupInterfaceMonitoring(
      ComponentSimple component, InterfaceMonitorConfig monitorConfig) throws WasmException;

  /**
   * Inspects the current state of a component.
   *
   * @param component the component to inspect
   * @param inspectionConfig inspection configuration
   * @return component state inspection result
   * @throws WasmException if inspection fails
   */
  ComponentStateInspection inspectComponentState(
      ComponentSimple component, StateInspectionConfig inspectionConfig) throws WasmException;

  /**
   * Analyzes component performance and provides optimization suggestions.
   *
   * @param component the component to analyze
   * @param analysisConfig analysis configuration
   * @return performance analysis result
   * @throws WasmException if analysis fails
   */
  PerformanceAnalysisResult analyzePerformance(
      ComponentSimple component, PerformanceAnalysisConfig analysisConfig) throws WasmException;

  /**
   * Sets up component lifecycle event monitoring.
   *
   * @param component the component to monitor
   * @param lifecycleConfig lifecycle monitoring configuration
   * @return lifecycle event monitor
   * @throws WasmException if lifecycle monitoring setup fails
   */
  ComponentLifecycleMonitor setupLifecycleMonitoring(
      ComponentSimple component, LifecycleMonitorConfig lifecycleConfig) throws WasmException;

  /**
   * Creates a memory dump of a component's state.
   *
   * @param component the component to dump
   * @param dumpConfig dump configuration
   * @return memory dump result
   * @throws WasmException if memory dump fails
   */
  MemoryDump createMemoryDump(ComponentSimple component, MemoryDumpConfig dumpConfig)
      throws WasmException;

  /**
   * Analyzes a memory dump for issues.
   *
   * @param memoryDump the memory dump to analyze
   * @param analysisConfig analysis configuration
   * @return memory analysis result
   * @throws WasmException if analysis fails
   */
  MemoryAnalysisResult analyzeMemoryDump(MemoryDump memoryDump, MemoryAnalysisConfig analysisConfig)
      throws WasmException;

  /**
   * Gets debugging statistics and metrics.
   *
   * @return debugging system statistics
   */
  DebuggingStatistics getStatistics();

  /**
   * Sets a global debugging event listener.
   *
   * @param listener the event listener
   */
  void setDebuggingEventListener(DebuggingEventListener listener);

  /** Removes the global debugging event listener. */
  void removeDebuggingEventListener();

  /**
   * Starts the debugging system services.
   *
   * @throws WasmException if startup fails
   */
  void start() throws WasmException;

  /**
   * Stops the debugging system services.
   *
   * @throws WasmException if shutdown fails
   */
  void stop() throws WasmException;

  @Override
  void close();

  /** Debug session for a component. */
  interface DebugSession {
    String getSessionId();

    ComponentSimple getComponent();

    DebugSessionState getState();

    ComponentDebugConfig getConfiguration();

    Instant getStartTime();

    List<Breakpoint> getBreakpoints();

    void pause() throws WasmException;

    void resume() throws WasmException;

    void stepInto() throws WasmException;

    void stepOver() throws WasmException;

    void stepOut() throws WasmException;

    Breakpoint setBreakpoint(String location, BreakpointConfig config) throws WasmException;

    void removeBreakpoint(String breakpointId) throws WasmException;

    void clearAllBreakpoints() throws WasmException;

    Optional<CallStack> getCallStack();

    Map<String, WasmValue> getLocalVariables() throws WasmException;

    ComponentStateSnapshot getStateSnapshot() throws WasmException;

    void evaluate(String expression) throws WasmException;

    List<DebugEvent> getEvents();

    void close() throws WasmException;
  }

  /** Execution tracer for recording component execution. */
  interface ExecutionTracer {
    String getTracerId();

    ComponentSimple getComponent();

    TraceConfig getConfiguration();

    boolean isTracing();

    void startTracing() throws WasmException;

    void stopTracing() throws WasmException;

    void pauseTracing() throws WasmException;

    void resumeTracing() throws WasmException;

    ExecutionTrace getTrace();

    void exportTrace(String filePath, TraceExportFormat format) throws WasmException;

    ExecutionTraceStatistics getStatistics();

    void clearTrace();

    void close() throws WasmException;
  }

  /** Performance profiler for analyzing component performance. */
  interface PerformanceProfiler {
    String getProfilerId();

    ComponentSimple getComponent();

    ProfileConfig getConfiguration();

    boolean isProfiling();

    void startProfiling() throws WasmException;

    void stopProfiling() throws WasmException;

    PerformanceProfile getProfile();

    void exportProfile(String filePath, ProfileExportFormat format) throws WasmException;

    PerformanceProfileStatistics getStatistics();

    List<PerformanceIssue> getIdentifiedIssues();

    void clearProfile();

    void close() throws WasmException;
  }

  /** Interface call monitor for tracking component interactions. */
  interface InterfaceCallMonitor {
    String getMonitorId();

    ComponentSimple getComponent();

    InterfaceMonitorConfig getConfiguration();

    boolean isMonitoring();

    void startMonitoring() throws WasmException;

    void stopMonitoring() throws WasmException;

    List<InterfaceCall> getRecordedCalls();

    InterfaceCallStatistics getStatistics();

    void exportCallLog(String filePath, CallLogFormat format) throws WasmException;

    void clearCallLog();

    void close() throws WasmException;
  }

  /** Component lifecycle monitor for tracking component lifecycle events. */
  interface ComponentLifecycleMonitor {
    String getMonitorId();

    ComponentSimple getComponent();

    LifecycleMonitorConfig getConfiguration();

    boolean isMonitoring();

    void startMonitoring() throws WasmException;

    void stopMonitoring() throws WasmException;

    List<LifecycleEvent> getLifecycleEvents();

    LifecycleStatistics getStatistics();

    void exportLifecycleLog(String filePath) throws WasmException;

    void close() throws WasmException;
  }

  /** Component state inspection result. */
  interface ComponentStateInspection {
    ComponentSimple getComponent();

    Instant getInspectionTime();

    ComponentLifecycleState getLifecycleState();

    ComponentResourceUsage getResourceUsage();

    Map<String, WasmValue> getGlobalVariables();

    List<ActiveFunction> getActiveFunctions();

    MemoryUsageBreakdown getMemoryBreakdown();

    List<ComponentIssue> getIdentifiedIssues();
  }

  /** Performance analysis result with optimization suggestions. */
  interface PerformanceAnalysisResult {
    ComponentSimple getComponent();

    Instant getAnalysisTime();

    PerformanceSummary getSummary();

    List<PerformanceBottleneck> getBottlenecks();

    List<OptimizationSuggestion> getOptimizationSuggestions();

    PerformanceComparison getComparison();

    PerformanceTrends getTrends();
  }

  /** Memory dump of component state. */
  interface MemoryDump {
    String getDumpId();

    ComponentSimple getComponent();

    Instant getCreationTime();

    long getTotalSize();

    byte[] getHeapSnapshot();

    Map<String, Object> getMetadata();

    MemoryDumpStatistics getStatistics();

    void exportDump(String filePath, MemoryDumpFormat format) throws WasmException;
  }

  /** Memory analysis result. */
  interface MemoryAnalysisResult {
    MemoryDump getMemoryDump();

    Instant getAnalysisTime();

    List<MemoryLeak> getMemoryLeaks();

    List<MemoryFragment> getFragmentation();

    MemoryAllocationPattern getAllocationPatterns();

    List<MemoryOptimizationSuggestion> getOptimizationSuggestions();
  }

  /** Debugging system statistics. */
  interface DebuggingStatistics {
    int getActiveDebugSessions();

    int getActiveTracers();

    int getActiveProfilers();

    long getTotalDebugOperations();

    long getTotalBreakpointsHit();

    double getAverageDebuggingOverhead();

    Map<String, Long> getOperationCounts();
  }

  /** Debugging event listener interface. */
  interface DebuggingEventListener {
    void onDebugSessionStarted(DebugSession session);

    void onDebugSessionStopped(DebugSession session);

    void onBreakpointHit(DebugSession session, Breakpoint breakpoint);

    void onComponentPaused(DebugSession session);

    void onComponentResumed(DebugSession session);

    void onPerformanceIssueDetected(ComponentSimple component, PerformanceIssue issue);

    void onMemoryLeakDetected(ComponentSimple component, MemoryLeak leak);
  }

  // Enums and supporting types
  /** States of a debugging session. */
  enum DebugSessionState {
    STARTING,
    RUNNING,
    PAUSED,
    STEPPING,
    STOPPED,
    ERROR
  }

  /** Types of breakpoints for debugging. */
  enum BreakpointType {
    LINE,
    FUNCTION,
    EXCEPTION,
    CONDITION
  }

  /** Export formats for execution traces. */
  enum TraceExportFormat {
    JSON,
    XML,
    BINARY,
    CSV
  }

  /** Export formats for performance profiles. */
  enum ProfileExportFormat {
    FLAMEGRAPH,
    JSON,
    CHROME_DEVTOOLS
  }

  /** Export formats for call logs. */
  enum CallLogFormat {
    JSON,
    CSV,
    TEXT
  }

  /** Export formats for memory dumps. */
  enum MemoryDumpFormat {
    BINARY,
    JSON,
    HPROF
  }

  // Configuration and data interfaces
  /** Configuration for debugging system. */
  interface DebuggingConfig {}

  /** Configuration for component debugging. */
  interface ComponentDebugConfig {}

  /** Configuration for debug session attachment. */
  interface DebugAttachConfig {}

  /** Configuration for execution tracing. */
  interface TraceConfig {}

  /** Configuration for performance profiling. */
  interface ProfileConfig {}

  /** Configuration for interface monitoring. */
  interface InterfaceMonitorConfig {}

  /** Configuration for state inspection. */
  interface StateInspectionConfig {}

  /** Configuration for performance analysis. */
  interface PerformanceAnalysisConfig {}

  /** Configuration for lifecycle monitoring. */
  interface LifecycleMonitorConfig {}

  /** Configuration for memory dumps. */
  interface MemoryDumpConfig {}

  /** Configuration for memory analysis. */
  interface MemoryAnalysisConfig {}

  /** Configuration for breakpoints. */
  interface BreakpointConfig {}

  /** Represents a debugging breakpoint. */
  interface Breakpoint {}

  /** Represents a call stack trace. */
  interface CallStack {}

  /** Snapshot of component state. */
  interface ComponentStateSnapshot {}

  /** Represents a debugging event. */
  interface DebugEvent {}

  /** Execution trace information. */
  interface ExecutionTrace {}

  /** Statistics for execution traces. */
  interface ExecutionTraceStatistics {}

  /** Performance profiling data. */
  interface PerformanceProfile {}

  /** Statistics for performance profiles. */
  interface PerformanceProfileStatistics {}

  /** Represents a performance issue. */
  interface PerformanceIssue {}

  /** Represents an interface call. */
  interface InterfaceCall {}

  /** Statistics for interface calls. */
  interface InterfaceCallStatistics {}

  /** Represents a lifecycle event. */
  interface LifecycleEvent {}

  /** Statistics for lifecycle events. */
  interface LifecycleStatistics {}

  /** Represents an active function. */
  interface ActiveFunction {}

  /** Breakdown of memory usage. */
  interface MemoryUsageBreakdown {}

  /** Represents a component issue. */
  interface ComponentIssue {}

  /** Summary of performance metrics. */
  interface PerformanceSummary {}

  /** Represents a performance bottleneck. */
  interface PerformanceBottleneck {}

  /** Suggestion for performance optimization. */
  interface OptimizationSuggestion {}

  /** Comparison of performance metrics. */
  interface PerformanceComparison {}

  /** Trends in performance over time. */
  interface PerformanceTrends {}

  /** Statistics for memory dumps. */
  interface MemoryDumpStatistics {}

  /** Represents a memory leak. */
  interface MemoryLeak {}

  /** Represents a memory fragment. */
  interface MemoryFragment {}

  /** Pattern of memory allocations. */
  interface MemoryAllocationPattern {}

  /** Suggestion for memory optimization. */
  interface MemoryOptimizationSuggestion {}
}
