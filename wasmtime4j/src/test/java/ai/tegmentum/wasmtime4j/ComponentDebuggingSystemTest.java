/*
 * Copyright 2025 Tegmentum AI
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentDebuggingSystem} interface.
 *
 * <p>ComponentDebuggingSystem provides comprehensive debugging capabilities for WebAssembly
 * components including execution tracing, profiling, and state inspection.
 */
@DisplayName("ComponentDebuggingSystem Tests")
class ComponentDebuggingSystemTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentDebuggingSystem.class.getModifiers()),
          "ComponentDebuggingSystem should be public");
      assertTrue(
          ComponentDebuggingSystem.class.isInterface(),
          "ComponentDebuggingSystem should be an interface");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ComponentDebuggingSystem.class),
          "ComponentDebuggingSystem should extend AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = ComponentDebuggingSystem.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getConfiguration method")
    void shouldHaveGetConfigurationMethod() throws NoSuchMethodException {
      final Method method = ComponentDebuggingSystem.class.getMethod("getConfiguration");
      assertNotNull(method, "getConfiguration method should exist");
    }

    @Test
    @DisplayName("should have startDebugging method")
    void shouldHaveStartDebuggingMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod(
              "startDebugging",
              ComponentSimple.class,
              ComponentDebuggingSystem.ComponentDebugConfig.class);
      assertNotNull(method, "startDebugging method should exist");
    }

    @Test
    @DisplayName("should have stopDebugging method")
    void shouldHaveStopDebuggingMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod(
              "stopDebugging", ComponentDebuggingSystem.DebugSession.class);
      assertNotNull(method, "stopDebugging method should exist");
    }

    @Test
    @DisplayName("should have getActiveSessions method")
    void shouldHaveGetActiveSessionsMethod() throws NoSuchMethodException {
      final Method method = ComponentDebuggingSystem.class.getMethod("getActiveSessions");
      assertNotNull(method, "getActiveSessions method should exist");
    }

    @Test
    @DisplayName("should have attachDebugger method")
    void shouldHaveAttachDebuggerMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod(
              "attachDebugger",
              ComponentInstance.class,
              ComponentDebuggingSystem.DebugAttachConfig.class);
      assertNotNull(method, "attachDebugger method should exist");
    }

    @Test
    @DisplayName("should have detachDebugger method")
    void shouldHaveDetachDebuggerMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod(
              "detachDebugger", ComponentDebuggingSystem.DebugSession.class);
      assertNotNull(method, "detachDebugger method should exist");
    }

    @Test
    @DisplayName("should have setupExecutionTracing method")
    void shouldHaveSetupExecutionTracingMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod(
              "setupExecutionTracing",
              ComponentSimple.class,
              ComponentDebuggingSystem.TraceConfig.class);
      assertNotNull(method, "setupExecutionTracing method should exist");
    }

    @Test
    @DisplayName("should have setupPerformanceProfiling method")
    void shouldHaveSetupPerformanceProfilingMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod(
              "setupPerformanceProfiling",
              ComponentSimple.class,
              ComponentDebuggingSystem.ProfileConfig.class);
      assertNotNull(method, "setupPerformanceProfiling method should exist");
    }

    @Test
    @DisplayName("should have setupInterfaceMonitoring method")
    void shouldHaveSetupInterfaceMonitoringMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod(
              "setupInterfaceMonitoring",
              ComponentSimple.class,
              ComponentDebuggingSystem.InterfaceMonitorConfig.class);
      assertNotNull(method, "setupInterfaceMonitoring method should exist");
    }

    @Test
    @DisplayName("should have inspectComponentState method")
    void shouldHaveInspectComponentStateMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod(
              "inspectComponentState",
              ComponentSimple.class,
              ComponentDebuggingSystem.StateInspectionConfig.class);
      assertNotNull(method, "inspectComponentState method should exist");
    }

    @Test
    @DisplayName("should have analyzePerformance method")
    void shouldHaveAnalyzePerformanceMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod(
              "analyzePerformance",
              ComponentSimple.class,
              ComponentDebuggingSystem.PerformanceAnalysisConfig.class);
      assertNotNull(method, "analyzePerformance method should exist");
    }

    @Test
    @DisplayName("should have setupLifecycleMonitoring method")
    void shouldHaveSetupLifecycleMonitoringMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod(
              "setupLifecycleMonitoring",
              ComponentSimple.class,
              ComponentDebuggingSystem.LifecycleMonitorConfig.class);
      assertNotNull(method, "setupLifecycleMonitoring method should exist");
    }

    @Test
    @DisplayName("should have createMemoryDump method")
    void shouldHaveCreateMemoryDumpMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod(
              "createMemoryDump",
              ComponentSimple.class,
              ComponentDebuggingSystem.MemoryDumpConfig.class);
      assertNotNull(method, "createMemoryDump method should exist");
    }

    @Test
    @DisplayName("should have analyzeMemoryDump method")
    void shouldHaveAnalyzeMemoryDumpMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod(
              "analyzeMemoryDump",
              ComponentDebuggingSystem.MemoryDump.class,
              ComponentDebuggingSystem.MemoryAnalysisConfig.class);
      assertNotNull(method, "analyzeMemoryDump method should exist");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = ComponentDebuggingSystem.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
    }

    @Test
    @DisplayName("should have setDebuggingEventListener method")
    void shouldHaveSetDebuggingEventListenerMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod(
              "setDebuggingEventListener", ComponentDebuggingSystem.DebuggingEventListener.class);
      assertNotNull(method, "setDebuggingEventListener method should exist");
    }

    @Test
    @DisplayName("should have removeDebuggingEventListener method")
    void shouldHaveRemoveDebuggingEventListenerMethod() throws NoSuchMethodException {
      final Method method =
          ComponentDebuggingSystem.class.getMethod("removeDebuggingEventListener");
      assertNotNull(method, "removeDebuggingEventListener method should exist");
    }

    @Test
    @DisplayName("should have start method")
    void shouldHaveStartMethod() throws NoSuchMethodException {
      final Method method = ComponentDebuggingSystem.class.getMethod("start");
      assertNotNull(method, "start method should exist");
    }

    @Test
    @DisplayName("should have stop method")
    void shouldHaveStopMethod() throws NoSuchMethodException {
      final Method method = ComponentDebuggingSystem.class.getMethod("stop");
      assertNotNull(method, "stop method should exist");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = ComponentDebuggingSystem.class.getMethod("close");
      assertNotNull(method, "close method should exist");
    }
  }

  @Nested
  @DisplayName("DebugSessionState Enum Tests")
  class DebugSessionStateEnumTests {

    @Test
    @DisplayName("should have all debug session states")
    void shouldHaveAllDebugSessionStates() {
      final var states = ComponentDebuggingSystem.DebugSessionState.values();
      assertEquals(6, states.length, "Should have 6 debug session states");
    }

    @Test
    @DisplayName("should have STARTING state")
    void shouldHaveStartingState() {
      assertEquals(
          ComponentDebuggingSystem.DebugSessionState.STARTING,
          ComponentDebuggingSystem.DebugSessionState.valueOf("STARTING"));
    }

    @Test
    @DisplayName("should have RUNNING state")
    void shouldHaveRunningState() {
      assertEquals(
          ComponentDebuggingSystem.DebugSessionState.RUNNING,
          ComponentDebuggingSystem.DebugSessionState.valueOf("RUNNING"));
    }

    @Test
    @DisplayName("should have PAUSED state")
    void shouldHavePausedState() {
      assertEquals(
          ComponentDebuggingSystem.DebugSessionState.PAUSED,
          ComponentDebuggingSystem.DebugSessionState.valueOf("PAUSED"));
    }

    @Test
    @DisplayName("should have STEPPING state")
    void shouldHaveSteppingState() {
      assertEquals(
          ComponentDebuggingSystem.DebugSessionState.STEPPING,
          ComponentDebuggingSystem.DebugSessionState.valueOf("STEPPING"));
    }

    @Test
    @DisplayName("should have STOPPED state")
    void shouldHaveStoppedState() {
      assertEquals(
          ComponentDebuggingSystem.DebugSessionState.STOPPED,
          ComponentDebuggingSystem.DebugSessionState.valueOf("STOPPED"));
    }

    @Test
    @DisplayName("should have ERROR state")
    void shouldHaveErrorState() {
      assertEquals(
          ComponentDebuggingSystem.DebugSessionState.ERROR,
          ComponentDebuggingSystem.DebugSessionState.valueOf("ERROR"));
    }
  }

  @Nested
  @DisplayName("BreakpointType Enum Tests")
  class BreakpointTypeEnumTests {

    @Test
    @DisplayName("should have all breakpoint types")
    void shouldHaveAllBreakpointTypes() {
      final var types = ComponentDebuggingSystem.BreakpointType.values();
      assertEquals(4, types.length, "Should have 4 breakpoint types");
    }

    @Test
    @DisplayName("should have LINE type")
    void shouldHaveLineType() {
      assertEquals(
          ComponentDebuggingSystem.BreakpointType.LINE,
          ComponentDebuggingSystem.BreakpointType.valueOf("LINE"));
    }

    @Test
    @DisplayName("should have FUNCTION type")
    void shouldHaveFunctionType() {
      assertEquals(
          ComponentDebuggingSystem.BreakpointType.FUNCTION,
          ComponentDebuggingSystem.BreakpointType.valueOf("FUNCTION"));
    }

    @Test
    @DisplayName("should have EXCEPTION type")
    void shouldHaveExceptionType() {
      assertEquals(
          ComponentDebuggingSystem.BreakpointType.EXCEPTION,
          ComponentDebuggingSystem.BreakpointType.valueOf("EXCEPTION"));
    }

    @Test
    @DisplayName("should have CONDITION type")
    void shouldHaveConditionType() {
      assertEquals(
          ComponentDebuggingSystem.BreakpointType.CONDITION,
          ComponentDebuggingSystem.BreakpointType.valueOf("CONDITION"));
    }
  }

  @Nested
  @DisplayName("TraceExportFormat Enum Tests")
  class TraceExportFormatEnumTests {

    @Test
    @DisplayName("should have all trace export formats")
    void shouldHaveAllTraceExportFormats() {
      final var formats = ComponentDebuggingSystem.TraceExportFormat.values();
      assertEquals(4, formats.length, "Should have 4 trace export formats");
    }

    @Test
    @DisplayName("should have JSON format")
    void shouldHaveJsonFormat() {
      assertEquals(
          ComponentDebuggingSystem.TraceExportFormat.JSON,
          ComponentDebuggingSystem.TraceExportFormat.valueOf("JSON"));
    }

    @Test
    @DisplayName("should have XML format")
    void shouldHaveXmlFormat() {
      assertEquals(
          ComponentDebuggingSystem.TraceExportFormat.XML,
          ComponentDebuggingSystem.TraceExportFormat.valueOf("XML"));
    }

    @Test
    @DisplayName("should have BINARY format")
    void shouldHaveBinaryFormat() {
      assertEquals(
          ComponentDebuggingSystem.TraceExportFormat.BINARY,
          ComponentDebuggingSystem.TraceExportFormat.valueOf("BINARY"));
    }

    @Test
    @DisplayName("should have CSV format")
    void shouldHaveCsvFormat() {
      assertEquals(
          ComponentDebuggingSystem.TraceExportFormat.CSV,
          ComponentDebuggingSystem.TraceExportFormat.valueOf("CSV"));
    }
  }

  @Nested
  @DisplayName("ProfileExportFormat Enum Tests")
  class ProfileExportFormatEnumTests {

    @Test
    @DisplayName("should have all profile export formats")
    void shouldHaveAllProfileExportFormats() {
      final var formats = ComponentDebuggingSystem.ProfileExportFormat.values();
      assertEquals(3, formats.length, "Should have 3 profile export formats");
    }

    @Test
    @DisplayName("should have FLAMEGRAPH format")
    void shouldHaveFlamegraphFormat() {
      assertEquals(
          ComponentDebuggingSystem.ProfileExportFormat.FLAMEGRAPH,
          ComponentDebuggingSystem.ProfileExportFormat.valueOf("FLAMEGRAPH"));
    }

    @Test
    @DisplayName("should have JSON format")
    void shouldHaveJsonFormat() {
      assertEquals(
          ComponentDebuggingSystem.ProfileExportFormat.JSON,
          ComponentDebuggingSystem.ProfileExportFormat.valueOf("JSON"));
    }

    @Test
    @DisplayName("should have CHROME_DEVTOOLS format")
    void shouldHaveChromeDevtoolsFormat() {
      assertEquals(
          ComponentDebuggingSystem.ProfileExportFormat.CHROME_DEVTOOLS,
          ComponentDebuggingSystem.ProfileExportFormat.valueOf("CHROME_DEVTOOLS"));
    }
  }

  @Nested
  @DisplayName("CallLogFormat Enum Tests")
  class CallLogFormatEnumTests {

    @Test
    @DisplayName("should have all call log formats")
    void shouldHaveAllCallLogFormats() {
      final var formats = ComponentDebuggingSystem.CallLogFormat.values();
      assertEquals(3, formats.length, "Should have 3 call log formats");
    }

    @Test
    @DisplayName("should have JSON format")
    void shouldHaveJsonFormat() {
      assertEquals(
          ComponentDebuggingSystem.CallLogFormat.JSON,
          ComponentDebuggingSystem.CallLogFormat.valueOf("JSON"));
    }

    @Test
    @DisplayName("should have CSV format")
    void shouldHaveCsvFormat() {
      assertEquals(
          ComponentDebuggingSystem.CallLogFormat.CSV,
          ComponentDebuggingSystem.CallLogFormat.valueOf("CSV"));
    }

    @Test
    @DisplayName("should have TEXT format")
    void shouldHaveTextFormat() {
      assertEquals(
          ComponentDebuggingSystem.CallLogFormat.TEXT,
          ComponentDebuggingSystem.CallLogFormat.valueOf("TEXT"));
    }
  }

  @Nested
  @DisplayName("MemoryDumpFormat Enum Tests")
  class MemoryDumpFormatEnumTests {

    @Test
    @DisplayName("should have all memory dump formats")
    void shouldHaveAllMemoryDumpFormats() {
      final var formats = ComponentDebuggingSystem.MemoryDumpFormat.values();
      assertEquals(3, formats.length, "Should have 3 memory dump formats");
    }

    @Test
    @DisplayName("should have BINARY format")
    void shouldHaveBinaryFormat() {
      assertEquals(
          ComponentDebuggingSystem.MemoryDumpFormat.BINARY,
          ComponentDebuggingSystem.MemoryDumpFormat.valueOf("BINARY"));
    }

    @Test
    @DisplayName("should have JSON format")
    void shouldHaveJsonFormat() {
      assertEquals(
          ComponentDebuggingSystem.MemoryDumpFormat.JSON,
          ComponentDebuggingSystem.MemoryDumpFormat.valueOf("JSON"));
    }

    @Test
    @DisplayName("should have HPROF format")
    void shouldHaveHprofFormat() {
      assertEquals(
          ComponentDebuggingSystem.MemoryDumpFormat.HPROF,
          ComponentDebuggingSystem.MemoryDumpFormat.valueOf("HPROF"));
    }
  }

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have DebugSession nested interface")
    void shouldHaveDebugSessionNestedInterface() {
      final var nestedClasses = ComponentDebuggingSystem.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("DebugSession")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "DebugSession should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have DebugSession nested interface");
    }

    @Test
    @DisplayName("should have ExecutionTracer nested interface")
    void shouldHaveExecutionTracerNestedInterface() {
      final var nestedClasses = ComponentDebuggingSystem.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ExecutionTracer")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "ExecutionTracer should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have ExecutionTracer nested interface");
    }

    @Test
    @DisplayName("should have PerformanceProfiler nested interface")
    void shouldHavePerformanceProfilerNestedInterface() {
      final var nestedClasses = ComponentDebuggingSystem.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("PerformanceProfiler")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "PerformanceProfiler should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have PerformanceProfiler nested interface");
    }

    @Test
    @DisplayName("should have InterfaceCallMonitor nested interface")
    void shouldHaveInterfaceCallMonitorNestedInterface() {
      final var nestedClasses = ComponentDebuggingSystem.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("InterfaceCallMonitor")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "InterfaceCallMonitor should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have InterfaceCallMonitor nested interface");
    }

    @Test
    @DisplayName("should have ComponentLifecycleMonitor nested interface")
    void shouldHaveComponentLifecycleMonitorNestedInterface() {
      final var nestedClasses = ComponentDebuggingSystem.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("ComponentLifecycleMonitor")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "ComponentLifecycleMonitor should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have ComponentLifecycleMonitor nested interface");
    }

    @Test
    @DisplayName("should have DebuggingStatistics nested interface")
    void shouldHaveDebuggingStatisticsNestedInterface() {
      final var nestedClasses = ComponentDebuggingSystem.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("DebuggingStatistics")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "DebuggingStatistics should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have DebuggingStatistics nested interface");
    }

    @Test
    @DisplayName("should have DebuggingEventListener nested interface")
    void shouldHaveDebuggingEventListenerNestedInterface() {
      final var nestedClasses = ComponentDebuggingSystem.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("DebuggingEventListener")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "DebuggingEventListener should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have DebuggingEventListener nested interface");
    }

    @Test
    @DisplayName("should have MemoryDump nested interface")
    void shouldHaveMemoryDumpNestedInterface() {
      final var nestedClasses = ComponentDebuggingSystem.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("MemoryDump")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "MemoryDump should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have MemoryDump nested interface");
    }

    @Test
    @DisplayName("should have MemoryAnalysisResult nested interface")
    void shouldHaveMemoryAnalysisResultNestedInterface() {
      final var nestedClasses = ComponentDebuggingSystem.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("MemoryAnalysisResult")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "MemoryAnalysisResult should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have MemoryAnalysisResult nested interface");
    }
  }

  @Nested
  @DisplayName("DebugSession Interface Methods")
  class DebugSessionInterfaceMethodsTests {

    @Test
    @DisplayName("DebugSession should have required methods")
    void debugSessionShouldHaveRequiredMethods() throws NoSuchMethodException {
      final Class<?> debugSessionClass = ComponentDebuggingSystem.DebugSession.class;

      assertNotNull(debugSessionClass.getMethod("getSessionId"), "Should have getSessionId");
      assertNotNull(debugSessionClass.getMethod("getComponent"), "Should have getComponent");
      assertNotNull(debugSessionClass.getMethod("getState"), "Should have getState");
      assertNotNull(
          debugSessionClass.getMethod("getConfiguration"), "Should have getConfiguration");
      assertNotNull(debugSessionClass.getMethod("getStartTime"), "Should have getStartTime");
      assertNotNull(debugSessionClass.getMethod("getBreakpoints"), "Should have getBreakpoints");
      assertNotNull(debugSessionClass.getMethod("pause"), "Should have pause");
      assertNotNull(debugSessionClass.getMethod("resume"), "Should have resume");
      assertNotNull(debugSessionClass.getMethod("stepInto"), "Should have stepInto");
      assertNotNull(debugSessionClass.getMethod("stepOver"), "Should have stepOver");
      assertNotNull(debugSessionClass.getMethod("stepOut"), "Should have stepOut");
      assertNotNull(debugSessionClass.getMethod("getCallStack"), "Should have getCallStack");
      assertNotNull(
          debugSessionClass.getMethod("getLocalVariables"), "Should have getLocalVariables");
      assertNotNull(
          debugSessionClass.getMethod("getStateSnapshot"), "Should have getStateSnapshot");
      assertNotNull(debugSessionClass.getMethod("getEvents"), "Should have getEvents");
      assertNotNull(debugSessionClass.getMethod("close"), "Should have close");
    }
  }

  @Nested
  @DisplayName("DebuggingStatistics Interface Methods")
  class DebuggingStatisticsInterfaceMethodsTests {

    @Test
    @DisplayName("DebuggingStatistics should have required methods")
    void debuggingStatisticsShouldHaveRequiredMethods() throws NoSuchMethodException {
      final Class<?> statsClass = ComponentDebuggingSystem.DebuggingStatistics.class;

      assertNotNull(
          statsClass.getMethod("getActiveDebugSessions"), "Should have getActiveDebugSessions");
      assertNotNull(statsClass.getMethod("getActiveTracers"), "Should have getActiveTracers");
      assertNotNull(statsClass.getMethod("getActiveProfilers"), "Should have getActiveProfilers");
      assertNotNull(
          statsClass.getMethod("getTotalDebugOperations"), "Should have getTotalDebugOperations");
      assertNotNull(
          statsClass.getMethod("getTotalBreakpointsHit"), "Should have getTotalBreakpointsHit");
      assertNotNull(
          statsClass.getMethod("getAverageDebuggingOverhead"),
          "Should have getAverageDebuggingOverhead");
      assertNotNull(statsClass.getMethod("getOperationCounts"), "Should have getOperationCounts");
    }
  }
}
