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

package ai.tegmentum.wasmtime4j.panama.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.debug.DebugCapabilities;
import ai.tegmentum.wasmtime4j.debug.DebugConfig;
import ai.tegmentum.wasmtime4j.debug.DebugEventListener;
import ai.tegmentum.wasmtime4j.debug.DebugOptions;
import ai.tegmentum.wasmtime4j.debug.DebugSession;
import ai.tegmentum.wasmtime4j.debug.Debugger;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for PanamaDebugger.
 *
 * <p>Tests use reflection to verify class structure, method signatures, and field declarations
 * without triggering native library initialization.
 */
@DisplayName("PanamaDebugger Tests")
class PanamaDebuggerTest {

  private static final Class<?> DEBUGGER_CLASS = PanamaDebugger.class;

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaDebugger should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(DEBUGGER_CLASS.getModifiers()),
          "PanamaDebugger should be declared as final");
    }

    @Test
    @DisplayName("PanamaDebugger should implement Debugger interface")
    void shouldImplementDebuggerInterface() {
      assertTrue(
          Debugger.class.isAssignableFrom(DEBUGGER_CLASS),
          "PanamaDebugger should implement Debugger interface");
    }

    @Test
    @DisplayName("PanamaDebugger should be in debug package")
    void shouldBeInDebugPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.panama.debug",
          DEBUGGER_CLASS.getPackageName(),
          "PanamaDebugger should be in panama.debug package");
    }

    @Test
    @DisplayName("PanamaDebugger should have expected number of declared fields")
    void shouldHaveExpectedFieldCount() {
      Field[] declaredFields = DEBUGGER_CLASS.getDeclaredFields();
      // Filter out synthetic fields
      long nonSyntheticFieldCount =
          Arrays.stream(declaredFields).filter(f -> !f.isSynthetic()).count();
      assertTrue(
          nonSyntheticFieldCount >= 10,
          "PanamaDebugger should have at least 10 declared fields, found: "
              + nonSyntheticFieldCount);
    }

    @Test
    @DisplayName("PanamaDebugger should have expected inner classes")
    void shouldHaveExpectedInnerClasses() {
      Class<?>[] declaredClasses = DEBUGGER_CLASS.getDeclaredClasses();
      Set<String> innerClassNames =
          Arrays.stream(declaredClasses).map(Class::getSimpleName).collect(Collectors.toSet());

      Set<String> expectedInnerClasses =
          new HashSet<>(
              Arrays.asList(
                  "DwarfDebugInfo",
                  "SourceMapIntegration",
                  "ProfilingData",
                  "MemoryInfo",
                  "ExecutionResult",
                  "PanamaExecutionTracer"));

      for (String expected : expectedInnerClasses) {
        assertTrue(
            innerClassNames.contains(expected),
            "PanamaDebugger should have inner class: " + expected);
      }
    }
  }

  @Nested
  @DisplayName("Field Declaration Tests")
  class FieldDeclarationTests {

    @Test
    @DisplayName("Should have private final nativeHandle field of MemorySegment type")
    void shouldHaveNativeHandleField() throws NoSuchFieldException {
      Field field = DEBUGGER_CLASS.getDeclaredField("nativeHandle");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "nativeHandle should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "nativeHandle should be final");
      assertEquals(MemorySegment.class, field.getType(), "nativeHandle should be MemorySegment");
    }

    @Test
    @DisplayName("Should have private final engine field of Engine type")
    void shouldHaveEngineField() throws NoSuchFieldException {
      Field field = DEBUGGER_CLASS.getDeclaredField("engine");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "engine should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "engine should be final");
      assertEquals(Engine.class, field.getType(), "engine should be Engine type");
    }

    @Test
    @DisplayName("Should have private final arena field of Arena type")
    void shouldHaveArenaField() throws NoSuchFieldException {
      Field field = DEBUGGER_CLASS.getDeclaredField("arena");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "arena should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "arena should be final");
      assertEquals(Arena.class, field.getType(), "arena should be Arena type");
    }

    @Test
    @DisplayName("Should have private final activeSessions field of List type")
    void shouldHaveActiveSessionsField() throws NoSuchFieldException {
      Field field = DEBUGGER_CLASS.getDeclaredField("activeSessions");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "activeSessions should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "activeSessions should be final");
      assertEquals(List.class, field.getType(), "activeSessions should be List type");
    }

    @Test
    @DisplayName("Should have private final dwarfInfoCache field of Map type")
    void shouldHaveDwarfInfoCacheField() throws NoSuchFieldException {
      Field field = DEBUGGER_CLASS.getDeclaredField("dwarfInfoCache");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "dwarfInfoCache should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "dwarfInfoCache should be final");
      assertEquals(Map.class, field.getType(), "dwarfInfoCache should be Map type");
    }

    @Test
    @DisplayName("Should have private final sourceMapCache field of Map type")
    void shouldHaveSourceMapCacheField() throws NoSuchFieldException {
      Field field = DEBUGGER_CLASS.getDeclaredField("sourceMapCache");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "sourceMapCache should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "sourceMapCache should be final");
      assertEquals(Map.class, field.getType(), "sourceMapCache should be Map type");
    }

    @Test
    @DisplayName("Should have private final executionTracers field of Map type")
    void shouldHaveExecutionTracersField() throws NoSuchFieldException {
      Field field = DEBUGGER_CLASS.getDeclaredField("executionTracers");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "executionTracers should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "executionTracers should be final");
      assertEquals(Map.class, field.getType(), "executionTracers should be Map type");
    }

    @Test
    @DisplayName("Should have volatile closed field")
    void shouldHaveClosedField() throws NoSuchFieldException {
      Field field = DEBUGGER_CLASS.getDeclaredField("closed");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "closed should be private");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "closed should be volatile");
      assertEquals(boolean.class, field.getType(), "closed should be boolean type");
    }

    @Test
    @DisplayName("Should have volatile dwarfEnabled field")
    void shouldHaveDwarfEnabledField() throws NoSuchFieldException {
      Field field = DEBUGGER_CLASS.getDeclaredField("dwarfEnabled");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "dwarfEnabled should be private");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "dwarfEnabled should be volatile");
      assertEquals(boolean.class, field.getType(), "dwarfEnabled should be boolean type");
    }

    @Test
    @DisplayName("Should have volatile profilingEnabled field")
    void shouldHaveProfilingEnabledField() throws NoSuchFieldException {
      Field field = DEBUGGER_CLASS.getDeclaredField("profilingEnabled");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "profilingEnabled should be private");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "profilingEnabled should be volatile");
      assertEquals(boolean.class, field.getType(), "profilingEnabled should be boolean type");
    }

    @Test
    @DisplayName("Should have volatile eventListener field")
    void shouldHaveEventListenerField() throws NoSuchFieldException {
      Field field = DEBUGGER_CLASS.getDeclaredField("eventListener");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "eventListener should be private");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "eventListener should be volatile");
      assertEquals(
          DebugEventListener.class, field.getType(), "eventListener should be DebugEventListener");
    }

    @Test
    @DisplayName("Should have static LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      Field field = DEBUGGER_CLASS.getDeclaredField("LOGGER");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      assertEquals(Logger.class, field.getType(), "LOGGER should be Logger type");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have constructor with Engine parameter")
    void shouldHaveEngineConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = DEBUGGER_CLASS.getConstructor(Engine.class);
      assertNotNull(constructor, "Should have constructor with Engine parameter");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()), "Engine constructor should be public");
    }

    @Test
    @DisplayName("Should have constructor with Engine and Arena parameters")
    void shouldHaveEngineArenaConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = DEBUGGER_CLASS.getConstructor(Engine.class, Arena.class);
      assertNotNull(constructor, "Should have constructor with Engine and Arena parameters");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()),
          "Engine+Arena constructor should be public");
    }
  }

  @Nested
  @DisplayName("Debugger Interface Method Tests")
  class DebuggerInterfaceMethodTests {

    @Test
    @DisplayName("Should have createSession method with DebugConfig parameter")
    void shouldHaveCreateSessionWithConfig() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("createSession", DebugConfig.class);
      assertNotNull(method, "Should have createSession(DebugConfig)");
      assertEquals(DebugSession.class, method.getReturnType(), "Should return DebugSession");
    }

    @Test
    @DisplayName("Should have getCurrentSession method")
    void shouldHaveGetCurrentSession() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("getCurrentSession");
      assertNotNull(method, "Should have getCurrentSession()");
      assertEquals(DebugSession.class, method.getReturnType(), "Should return DebugSession");
    }

    @Test
    @DisplayName("Should have getDebuggerName method")
    void shouldHaveGetDebuggerName() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("getDebuggerName");
      assertNotNull(method, "Should have getDebuggerName()");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("Should have isEnabled method")
    void shouldHaveIsEnabled() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("isEnabled");
      assertNotNull(method, "Should have isEnabled()");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have setEventListener method")
    void shouldHaveSetEventListener() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("setEventListener", DebugEventListener.class);
      assertNotNull(method, "Should have setEventListener(DebugEventListener)");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have detach method with no parameters")
    void shouldHaveDetach() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("detach");
      assertNotNull(method, "Should have detach()");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have attach method with String parameter")
    void shouldHaveAttachWithString() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("attach", String.class);
      assertNotNull(method, "Should have attach(String)");
      assertEquals(DebugSession.class, method.getReturnType(), "Should return DebugSession");
    }
  }

  @Nested
  @DisplayName("Session Management Method Tests")
  class SessionManagementMethodTests {

    @Test
    @DisplayName("Should have createSession method with Instance parameter")
    void shouldHaveCreateSessionWithInstance() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("createSession", Instance.class);
      assertNotNull(method, "Should have createSession(Instance)");
      assertEquals(
          PanamaDebugSession.class, method.getReturnType(), "Should return PanamaDebugSession");
    }

    @Test
    @DisplayName("Should have createSession method with List parameter")
    void shouldHaveCreateSessionWithList() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("createSession", List.class);
      assertNotNull(method, "Should have createSession(List)");
      assertEquals(
          PanamaDebugSession.class, method.getReturnType(), "Should return PanamaDebugSession");
    }

    @Test
    @DisplayName("Should have createSession method with Instance and DebugConfig parameters")
    void shouldHaveCreateSessionWithInstanceAndConfig() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("createSession", Instance.class, DebugConfig.class);
      assertNotNull(method, "Should have createSession(Instance, DebugConfig)");
      assertEquals(
          PanamaDebugSession.class, method.getReturnType(), "Should return PanamaDebugSession");
    }

    @Test
    @DisplayName("Should have getActiveSessions method")
    void shouldHaveGetActiveSessions() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("getActiveSessions");
      assertNotNull(method, "Should have getActiveSessions()");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have closeSession method")
    void shouldHaveCloseSession() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("closeSession", PanamaDebugSession.class);
      assertNotNull(method, "Should have closeSession(PanamaDebugSession)");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have closeAllSessions method")
    void shouldHaveCloseAllSessions() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("closeAllSessions");
      assertNotNull(method, "Should have closeAllSessions()");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("Should have attach method with Instance parameter")
    void shouldHaveAttachWithInstance() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("attach", Instance.class);
      assertNotNull(method, "Should have attach(Instance)");
      assertEquals(
          PanamaDebugSession.class, method.getReturnType(), "Should return PanamaDebugSession");
    }

    @Test
    @DisplayName("Should have detach method with Instance parameter")
    void shouldHaveDetachWithInstance() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("detach", Instance.class);
      assertNotNull(method, "Should have detach(Instance)");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Debug Control Method Tests")
  class DebugControlMethodTests {

    @Test
    @DisplayName("Should have getEngine method")
    void shouldHaveGetEngine() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("getEngine");
      assertNotNull(method, "Should have getEngine()");
      assertEquals(Engine.class, method.getReturnType(), "Should return Engine");
    }

    @Test
    @DisplayName("Should have getCapabilities method")
    void shouldHaveGetCapabilities() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("getCapabilities");
      assertNotNull(method, "Should have getCapabilities()");
      assertEquals(
          DebugCapabilities.class, method.getReturnType(), "Should return DebugCapabilities");
    }

    @Test
    @DisplayName("Should have getDebugInfo method")
    void shouldHaveGetDebugInfo() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("getDebugInfo");
      assertNotNull(method, "Should have getDebugInfo()");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have setDebugModeEnabled method")
    void shouldHaveSetDebugModeEnabled() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("setDebugModeEnabled", boolean.class);
      assertNotNull(method, "Should have setDebugModeEnabled(boolean)");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have isDebugModeEnabled method")
    void shouldHaveIsDebugModeEnabled() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("isDebugModeEnabled");
      assertNotNull(method, "Should have isDebugModeEnabled()");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have setDebugOptions method")
    void shouldHaveSetDebugOptions() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("setDebugOptions", DebugOptions.class);
      assertNotNull(method, "Should have setDebugOptions(DebugOptions)");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have getDebugOptions method")
    void shouldHaveGetDebugOptions() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("getDebugOptions");
      assertNotNull(method, "Should have getDebugOptions()");
      assertEquals(DebugOptions.class, method.getReturnType(), "Should return DebugOptions");
    }

    @Test
    @DisplayName("Should have isValid method")
    void shouldHaveIsValid() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("isValid");
      assertNotNull(method, "Should have isValid()");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("DWARF Debug Method Tests")
  class DwarfDebugMethodTests {

    @Test
    @DisplayName("Should have getDwarfInfo method")
    void shouldHaveGetDwarfInfo() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("getDwarfInfo", Module.class);
      assertNotNull(method, "Should have getDwarfInfo(Module)");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("Should have setDwarfEnabled method")
    void shouldHaveSetDwarfEnabled() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("setDwarfEnabled", boolean.class);
      assertNotNull(method, "Should have setDwarfEnabled(boolean)");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have isDwarfEnabled method")
    void shouldHaveIsDwarfEnabled() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("isDwarfEnabled");
      assertNotNull(method, "Should have isDwarfEnabled()");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Source Map Method Tests")
  class SourceMapMethodTests {

    @Test
    @DisplayName("Should have createSourceMapIntegration method")
    void shouldHaveCreateSourceMapIntegration() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("createSourceMapIntegration", String.class);
      assertNotNull(method, "Should have createSourceMapIntegration(String)");
      Class<?> returnType = method.getReturnType();
      assertEquals(
          "SourceMapIntegration", returnType.getSimpleName(), "Should return SourceMapIntegration");
    }
  }

  @Nested
  @DisplayName("Execution Tracer Method Tests")
  class ExecutionTracerMethodTests {

    @Test
    @DisplayName("Should have createExecutionTracer method")
    void shouldHaveCreateExecutionTracer() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("createExecutionTracer", Instance.class);
      assertNotNull(method, "Should have createExecutionTracer(Instance)");
      Class<?> returnType = method.getReturnType();
      assertEquals(
          "PanamaExecutionTracer",
          returnType.getSimpleName(),
          "Should return PanamaExecutionTracer");
    }
  }

  @Nested
  @DisplayName("Profiling Method Tests")
  class ProfilingMethodTests {

    @Test
    @DisplayName("Should have startProfiling method")
    void shouldHaveStartProfiling() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("startProfiling", boolean.class, boolean.class);
      assertNotNull(method, "Should have startProfiling(boolean, boolean)");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have stopProfiling method")
    void shouldHaveStopProfiling() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("stopProfiling");
      assertNotNull(method, "Should have stopProfiling()");
      Class<?> returnType = method.getReturnType();
      assertEquals("ProfilingData", returnType.getSimpleName(), "Should return ProfilingData");
    }

    @Test
    @DisplayName("Should have isProfilingEnabled method")
    void shouldHaveIsProfilingEnabled() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("isProfilingEnabled");
      assertNotNull(method, "Should have isProfilingEnabled()");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Breakpoint Method Tests")
  class BreakpointMethodTests {

    @Test
    @DisplayName("Should have setBreakpointAtAddress method")
    void shouldHaveSetBreakpointAtAddress() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("setBreakpointAtAddress", Instance.class, int.class);
      assertNotNull(method, "Should have setBreakpointAtAddress(Instance, int)");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Should have setBreakpointAtFunction method")
    void shouldHaveSetBreakpointAtFunction() throws NoSuchMethodException {
      Method method =
          DEBUGGER_CLASS.getMethod("setBreakpointAtFunction", Instance.class, String.class);
      assertNotNull(method, "Should have setBreakpointAtFunction(Instance, String)");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Should have setBreakpointAtLine method")
    void shouldHaveSetBreakpointAtLine() throws NoSuchMethodException {
      Method method =
          DEBUGGER_CLASS.getMethod("setBreakpointAtLine", Instance.class, String.class, int.class);
      assertNotNull(method, "Should have setBreakpointAtLine(Instance, String, int)");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Should have removeBreakpoint method")
    void shouldHaveRemoveBreakpoint() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("removeBreakpoint", long.class);
      assertNotNull(method, "Should have removeBreakpoint(long)");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Stack and Variable Method Tests")
  class StackAndVariableMethodTests {

    @Test
    @DisplayName("Should have getCallStack method")
    void shouldHaveGetCallStack() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("getCallStack", Instance.class);
      assertNotNull(method, "Should have getCallStack(Instance)");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have getLocalVariables method")
    void shouldHaveGetLocalVariables() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("getLocalVariables", Instance.class, int.class);
      assertNotNull(method, "Should have getLocalVariables(Instance, int)");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("Should have evaluateExpression method")
    void shouldHaveEvaluateExpression() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("evaluateExpression", Instance.class, String.class);
      assertNotNull(method, "Should have evaluateExpression(Instance, String)");
      assertNotNull(method.getReturnType(), "Should have a return type");
    }
  }

  @Nested
  @DisplayName("Memory Inspection Method Tests")
  class MemoryInspectionMethodTests {

    @Test
    @DisplayName("Should have inspectMemory method")
    void shouldHaveInspectMemory() throws NoSuchMethodException {
      Method method =
          DEBUGGER_CLASS.getMethod("inspectMemory", Instance.class, int.class, int.class);
      assertNotNull(method, "Should have inspectMemory(Instance, int, int)");
      Class<?> returnType = method.getReturnType();
      assertEquals("MemoryInfo", returnType.getSimpleName(), "Should return MemoryInfo");
    }
  }

  @Nested
  @DisplayName("Execution Control Method Tests")
  class ExecutionControlMethodTests {

    @Test
    @DisplayName("Should have stepInto method")
    void shouldHaveStepInto() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("stepInto", Instance.class);
      assertNotNull(method, "Should have stepInto(Instance)");
      Class<?> returnType = method.getReturnType();
      assertEquals("ExecutionResult", returnType.getSimpleName(), "Should return ExecutionResult");
    }

    @Test
    @DisplayName("Should have stepOver method")
    void shouldHaveStepOver() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("stepOver", Instance.class);
      assertNotNull(method, "Should have stepOver(Instance)");
      Class<?> returnType = method.getReturnType();
      assertEquals("ExecutionResult", returnType.getSimpleName(), "Should return ExecutionResult");
    }

    @Test
    @DisplayName("Should have stepOut method")
    void shouldHaveStepOut() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("stepOut", Instance.class);
      assertNotNull(method, "Should have stepOut(Instance)");
      Class<?> returnType = method.getReturnType();
      assertEquals("ExecutionResult", returnType.getSimpleName(), "Should return ExecutionResult");
    }

    @Test
    @DisplayName("Should have continueExecution method")
    void shouldHaveContinueExecution() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("continueExecution", Instance.class);
      assertNotNull(method, "Should have continueExecution(Instance)");
      Class<?> returnType = method.getReturnType();
      assertEquals("ExecutionResult", returnType.getSimpleName(), "Should return ExecutionResult");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("Should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getMethod("close");
      assertNotNull(method, "Should have close()");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have getNativeHandle package-private method")
    void shouldHaveGetNativeHandle() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getDeclaredMethod("getNativeHandle");
      assertNotNull(method, "Should have getNativeHandle()");
      assertFalse(Modifier.isPublic(method.getModifiers()), "getNativeHandle should not be public");
      assertFalse(
          Modifier.isPrivate(method.getModifiers()), "getNativeHandle should not be private");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("Should have removeSession package-private method")
    void shouldHaveRemoveSession() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getDeclaredMethod("removeSession", PanamaDebugSession.class);
      assertNotNull(method, "Should have removeSession(PanamaDebugSession)");
      assertFalse(Modifier.isPublic(method.getModifiers()), "removeSession should not be public");
      assertFalse(Modifier.isPrivate(method.getModifiers()), "removeSession should not be private");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("Should have validateNotClosed private method")
    void shouldHaveValidateNotClosed() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getDeclaredMethod("validateNotClosed");
      assertNotNull(method, "Should have validateNotClosed()");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "validateNotClosed should be private");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have extractEngineHandle private method")
    void shouldHaveExtractEngineHandle() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getDeclaredMethod("extractEngineHandle", Engine.class);
      assertNotNull(method, "Should have extractEngineHandle(Engine)");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()), "extractEngineHandle should be private");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("Should have extractInstanceHandle private method")
    void shouldHaveExtractInstanceHandle() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getDeclaredMethod("extractInstanceHandle", Instance.class);
      assertNotNull(method, "Should have extractInstanceHandle(Instance)");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()), "extractInstanceHandle should be private");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("Should have extractModuleHandle private method")
    void shouldHaveExtractModuleHandle() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getDeclaredMethod("extractModuleHandle", Module.class);
      assertNotNull(method, "Should have extractModuleHandle(Module)");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()), "extractModuleHandle should be private");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }
  }

  @Nested
  @DisplayName("Native Method Tests")
  class NativeMethodTests {

    @Test
    @DisplayName("Should have expected native method count")
    void shouldHaveExpectedNativeMethodCount() {
      Method[] allMethods = DEBUGGER_CLASS.getDeclaredMethods();
      long nativeMethodCount =
          Arrays.stream(allMethods)
              .filter(m -> m.getName().startsWith("native"))
              .filter(m -> Modifier.isPrivate(m.getModifiers()))
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();

      assertTrue(
          nativeMethodCount >= 20,
          "Should have at least 20 native methods, found: " + nativeMethodCount);
    }

    @Test
    @DisplayName("Should have nativeCreateDebugger method")
    void shouldHaveNativeCreateDebugger() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getDeclaredMethod("nativeCreateDebugger", MemorySegment.class);
      assertNotNull(method, "Should have nativeCreateDebugger(MemorySegment)");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("Should have nativeCloseDebugger method")
    void shouldHaveNativeCloseDebugger() throws NoSuchMethodException {
      Method method = DEBUGGER_CLASS.getDeclaredMethod("nativeCloseDebugger", MemorySegment.class);
      assertNotNull(method, "Should have nativeCloseDebugger(MemorySegment)");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("Should have nativeIsValidDebugger method")
    void shouldHaveNativeIsValidDebugger() throws NoSuchMethodException {
      Method method =
          DEBUGGER_CLASS.getDeclaredMethod("nativeIsValidDebugger", MemorySegment.class);
      assertNotNull(method, "Should have nativeIsValidDebugger(MemorySegment)");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have nativeGetCapabilities method")
    void shouldHaveNativeGetCapabilities() throws NoSuchMethodException {
      Method method =
          DEBUGGER_CLASS.getDeclaredMethod("nativeGetCapabilities", MemorySegment.class);
      assertNotNull(method, "Should have nativeGetCapabilities(MemorySegment)");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("Should have nativeAttachToInstance method")
    void shouldHaveNativeAttachToInstance() throws NoSuchMethodException {
      Method method =
          DEBUGGER_CLASS.getDeclaredMethod(
              "nativeAttachToInstance", MemorySegment.class, MemorySegment.class);
      assertNotNull(method, "Should have nativeAttachToInstance(MemorySegment, MemorySegment)");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }

    @Test
    @DisplayName("Should have nativeDetachFromInstance method")
    void shouldHaveNativeDetachFromInstance() throws NoSuchMethodException {
      Method method =
          DEBUGGER_CLASS.getDeclaredMethod(
              "nativeDetachFromInstance", MemorySegment.class, MemorySegment.class);
      assertNotNull(method, "Should have nativeDetachFromInstance(MemorySegment, MemorySegment)");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Inner Class Tests")
  class InnerClassTests {

    @Test
    @DisplayName("DwarfDebugInfo should be public static final")
    void dwarfDebugInfoShouldBePublicStaticFinal() {
      Class<?>[] declaredClasses = DEBUGGER_CLASS.getDeclaredClasses();
      Class<?> dwarfClass =
          Arrays.stream(declaredClasses)
              .filter(c -> c.getSimpleName().equals("DwarfDebugInfo"))
              .findFirst()
              .orElse(null);

      assertNotNull(dwarfClass, "Should have DwarfDebugInfo inner class");
      assertTrue(Modifier.isPublic(dwarfClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(dwarfClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(dwarfClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("SourceMapIntegration should be public static final")
    void sourceMapIntegrationShouldBePublicStaticFinal() {
      Class<?>[] declaredClasses = DEBUGGER_CLASS.getDeclaredClasses();
      Class<?> sourceMapClass =
          Arrays.stream(declaredClasses)
              .filter(c -> c.getSimpleName().equals("SourceMapIntegration"))
              .findFirst()
              .orElse(null);

      assertNotNull(sourceMapClass, "Should have SourceMapIntegration inner class");
      assertTrue(Modifier.isPublic(sourceMapClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(sourceMapClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(sourceMapClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("ProfilingData should be public static final")
    void profilingDataShouldBePublicStaticFinal() {
      Class<?>[] declaredClasses = DEBUGGER_CLASS.getDeclaredClasses();
      Class<?> profilingClass =
          Arrays.stream(declaredClasses)
              .filter(c -> c.getSimpleName().equals("ProfilingData"))
              .findFirst()
              .orElse(null);

      assertNotNull(profilingClass, "Should have ProfilingData inner class");
      assertTrue(Modifier.isPublic(profilingClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(profilingClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(profilingClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("MemoryInfo should be public static final")
    void memoryInfoShouldBePublicStaticFinal() {
      Class<?>[] declaredClasses = DEBUGGER_CLASS.getDeclaredClasses();
      Class<?> memoryClass =
          Arrays.stream(declaredClasses)
              .filter(c -> c.getSimpleName().equals("MemoryInfo"))
              .findFirst()
              .orElse(null);

      assertNotNull(memoryClass, "Should have MemoryInfo inner class");
      assertTrue(Modifier.isPublic(memoryClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(memoryClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(memoryClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("ExecutionResult should be public static final")
    void executionResultShouldBePublicStaticFinal() {
      Class<?>[] declaredClasses = DEBUGGER_CLASS.getDeclaredClasses();
      Class<?> executionClass =
          Arrays.stream(declaredClasses)
              .filter(c -> c.getSimpleName().equals("ExecutionResult"))
              .findFirst()
              .orElse(null);

      assertNotNull(executionClass, "Should have ExecutionResult inner class");
      assertTrue(Modifier.isPublic(executionClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(executionClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(executionClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("PanamaExecutionTracer should be public static final")
    void panamaExecutionTracerShouldBePublicStaticFinal() {
      Class<?>[] declaredClasses = DEBUGGER_CLASS.getDeclaredClasses();
      Class<?> tracerClass =
          Arrays.stream(declaredClasses)
              .filter(c -> c.getSimpleName().equals("PanamaExecutionTracer"))
              .findFirst()
              .orElse(null);

      assertNotNull(tracerClass, "Should have PanamaExecutionTracer inner class");
      assertTrue(Modifier.isPublic(tracerClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(tracerClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(tracerClass.getModifiers()), "Should be final");
    }
  }

  @Nested
  @DisplayName("PanamaExecutionTracer Inner Class Tests")
  class PanamaExecutionTracerTests {

    private Class<?> getTracerClass() {
      return Arrays.stream(DEBUGGER_CLASS.getDeclaredClasses())
          .filter(c -> c.getSimpleName().equals("PanamaExecutionTracer"))
          .findFirst()
          .orElseThrow(() -> new AssertionError("PanamaExecutionTracer class not found"));
    }

    @Test
    @DisplayName("PanamaExecutionTracer should have nativeHandle field")
    void shouldHaveNativeHandleField() throws NoSuchFieldException {
      Class<?> tracerClass = getTracerClass();
      Field field = tracerClass.getDeclaredField("nativeHandle");
      assertNotNull(field, "Should have nativeHandle field");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(MemorySegment.class, field.getType(), "Should be MemorySegment type");
    }

    @Test
    @DisplayName("PanamaExecutionTracer should have instance field")
    void shouldHaveInstanceField() throws NoSuchFieldException {
      Class<?> tracerClass = getTracerClass();
      Field field = tracerClass.getDeclaredField("instance");
      assertNotNull(field, "Should have instance field");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(Instance.class, field.getType(), "Should be Instance type");
    }

    @Test
    @DisplayName("PanamaExecutionTracer should have volatile started field")
    void shouldHaveStartedField() throws NoSuchFieldException {
      Class<?> tracerClass = getTracerClass();
      Field field = tracerClass.getDeclaredField("started");
      assertNotNull(field, "Should have started field");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "Should be volatile");
      assertEquals(boolean.class, field.getType(), "Should be boolean type");
    }

    @Test
    @DisplayName("PanamaExecutionTracer should have start method")
    void shouldHaveStartMethod() throws NoSuchMethodException {
      Class<?> tracerClass = getTracerClass();
      Method method = tracerClass.getMethod("start");
      assertNotNull(method, "Should have start()");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("PanamaExecutionTracer should have stop method")
    void shouldHaveStopMethod() throws NoSuchMethodException {
      Class<?> tracerClass = getTracerClass();
      Method method = tracerClass.getMethod("stop");
      assertNotNull(method, "Should have stop()");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("PanamaExecutionTracer should have isStarted method")
    void shouldHaveIsStartedMethod() throws NoSuchMethodException {
      Class<?> tracerClass = getTracerClass();
      Method method = tracerClass.getMethod("isStarted");
      assertNotNull(method, "Should have isStarted()");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("PanamaExecutionTracer should have getEvents method")
    void shouldHaveGetEventsMethod() throws NoSuchMethodException {
      Class<?> tracerClass = getTracerClass();
      Method method = tracerClass.getMethod("getEvents");
      assertNotNull(method, "Should have getEvents()");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("PanamaExecutionTracer should have clearEvents method")
    void shouldHaveClearEventsMethod() throws NoSuchMethodException {
      Class<?> tracerClass = getTracerClass();
      Method method = tracerClass.getMethod("clearEvents");
      assertNotNull(method, "Should have clearEvents()");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Method Count Verification Tests")
  class MethodCountTests {

    @Test
    @DisplayName("Should have expected total public method count")
    void shouldHaveExpectedPublicMethodCount() {
      Method[] publicMethods = DEBUGGER_CLASS.getMethods();
      // Filter out Object methods
      long debuggerMethodCount =
          Arrays.stream(publicMethods)
              .filter(m -> m.getDeclaringClass().equals(DEBUGGER_CLASS))
              .count();

      assertTrue(
          debuggerMethodCount >= 30,
          "Should have at least 30 public methods, found: " + debuggerMethodCount);
    }

    @Test
    @DisplayName("Should have expected total declared method count")
    void shouldHaveExpectedDeclaredMethodCount() {
      Method[] allMethods = DEBUGGER_CLASS.getDeclaredMethods();
      // Filter out synthetic methods
      long nonSyntheticCount = Arrays.stream(allMethods).filter(m -> !m.isSynthetic()).count();

      assertTrue(
          nonSyntheticCount >= 50,
          "Should have at least 50 declared methods (including private), found: "
              + nonSyntheticCount);
    }
  }
}
