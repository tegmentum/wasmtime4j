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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.Breakpoint;
import ai.tegmentum.wasmtime4j.debug.DebugConfig;
import ai.tegmentum.wasmtime4j.debug.DebugSession;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaDebugSession} class.
 *
 * <p>PanamaDebugSession provides Panama implementation of WebAssembly debug sessions.
 */
@DisplayName("PanamaDebugSession Tests")
class PanamaDebugSessionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaDebugSession.class.getModifiers()),
          "PanamaDebugSession should be public");
      assertTrue(
          Modifier.isFinal(PanamaDebugSession.class.getModifiers()),
          "PanamaDebugSession should be final");
    }

    @Test
    @DisplayName("should implement DebugSession interface")
    void shouldImplementDebugSessionInterface() {
      assertTrue(
          DebugSession.class.isAssignableFrom(PanamaDebugSession.class),
          "PanamaDebugSession should implement DebugSession");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with MemorySegment and Arena")
    void shouldHaveMemorySegmentArenaConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaDebugSession.class.getConstructor(MemorySegment.class, Arena.class);
      assertNotNull(constructor, "Constructor with MemorySegment, Arena should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with two MemorySegments and Arena")
    void shouldHaveTwoMemorySegmentArenaConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaDebugSession.class.getConstructor(
              MemorySegment.class, MemorySegment.class, Arena.class);
      assertNotNull(constructor, "Constructor with two MemorySegments, Arena should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with MemorySegment array and Arena")
    void shouldHaveMemorySegmentArrayArenaConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaDebugSession.class.getConstructor(
              MemorySegment.class, MemorySegment[].class, Arena.class);
      assertNotNull(constructor, "Constructor with MemorySegment[], Arena should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with config")
    void shouldHaveConfigConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaDebugSession.class.getConstructor(
              MemorySegment.class, MemorySegment.class, DebugConfig.class, Arena.class);
      assertNotNull(constructor, "Constructor with DebugConfig should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have createLocal static method")
    void shouldHaveCreateLocalMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("createLocal", Arena.class);
      assertNotNull(method, "createLocal method should exist");
      assertEquals(
          PanamaDebugSession.class, method.getReturnType(), "Should return PanamaDebugSession");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
    }
  }

  @Nested
  @DisplayName("Session Control Method Tests")
  class SessionControlMethodTests {

    @Test
    @DisplayName("should have start method")
    void shouldHaveStartMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("start");
      assertNotNull(method, "start method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have stop method")
    void shouldHaveStopMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("stop");
      assertNotNull(method, "stop method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have continueExecution method")
    void shouldHaveContinueExecutionMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("continueExecution");
      assertNotNull(method, "continueExecution method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have step method")
    void shouldHaveStepMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("step", DebugSession.StepType.class);
      assertNotNull(method, "step method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Breakpoint Method Tests")
  class BreakpointMethodTests {

    @Test
    @DisplayName("should have addBreakpoint method")
    void shouldHaveAddBreakpointMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("addBreakpoint", Breakpoint.class);
      assertNotNull(method, "addBreakpoint method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have removeBreakpoint method")
    void shouldHaveRemoveBreakpointMethod() throws NoSuchMethodException {
      final Method method =
          PanamaDebugSession.class.getMethod("removeBreakpoint", Breakpoint.class);
      assertNotNull(method, "removeBreakpoint method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getBreakpoints method")
    void shouldHaveGetBreakpointsMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("getBreakpoints");
      assertNotNull(method, "getBreakpoints method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Status Method Tests")
  class StatusMethodTests {

    @Test
    @DisplayName("should have getSessionId method")
    void shouldHaveGetSessionIdMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("getSessionId");
      assertNotNull(method, "getSessionId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have isActive method")
    void shouldHaveIsActiveMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("isActive");
      assertNotNull(method, "isActive method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isPaused method")
    void shouldHaveIsPausedMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("isPaused");
      assertNotNull(method, "isPaused method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isClosed method")
    void shouldHaveIsClosedMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("isClosed");
      assertNotNull(method, "isClosed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Handle Access Method Tests")
  class HandleAccessMethodTests {

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("should have getDebuggerHandle method")
    void shouldHaveGetDebuggerHandleMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("getDebuggerHandle");
      assertNotNull(method, "getDebuggerHandle method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("should have getInstanceHandles method")
    void shouldHaveGetInstanceHandlesMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("getInstanceHandles");
      assertNotNull(method, "getInstanceHandles method should exist");
      assertEquals(MemorySegment[].class, method.getReturnType(), "Should return MemorySegment[]");
    }

    @Test
    @DisplayName("should have getArena method")
    void shouldHaveGetArenaMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("getArena");
      assertNotNull(method, "getArena method should exist");
      assertEquals(Arena.class, method.getReturnType(), "Should return Arena");
    }
  }

  @Nested
  @DisplayName("Call Stack Method Tests")
  class CallStackMethodTests {

    @Test
    @DisplayName("should have getCallStack method")
    void shouldHaveGetCallStackMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("getCallStack");
      assertNotNull(method, "getCallStack method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have updateCallStack method")
    void shouldHaveUpdateCallStackMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("updateCallStack", List.class);
      assertNotNull(method, "updateCallStack method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Event Handler Method Tests")
  class EventHandlerMethodTests {

    @Test
    @DisplayName("should have onBreakpointHit method")
    void shouldHaveOnBreakpointHitMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("onBreakpointHit", String.class);
      assertNotNull(method, "onBreakpointHit method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have onStepComplete method")
    void shouldHaveOnStepCompleteMethod() throws NoSuchMethodException {
      final Method method =
          PanamaDebugSession.class.getMethod("onStepComplete", DebugSession.StepType.class);
      assertNotNull(method, "onStepComplete method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Config Method Tests")
  class ConfigMethodTests {

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(DebugConfig.class, method.getReturnType(), "Should return DebugConfig");
    }

    @Test
    @DisplayName("should have getPendingStep method")
    void shouldHaveGetPendingStepMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("getPendingStep");
      assertNotNull(method, "getPendingStep method should exist");
      assertEquals(DebugSession.StepType.class, method.getReturnType(), "Should return StepType");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaDebugSession.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
