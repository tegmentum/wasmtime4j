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

package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.DebugEvent.DebugEventType;
import ai.tegmentum.wasmtime4j.debug.DebugHandler.DebugAction;
import ai.tegmentum.wasmtime4j.debug.ExecutionState.ExecutionStatistics;
import ai.tegmentum.wasmtime4j.debug.ExecutionState.ExecutionStatus;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DebugHandler} interface.
 *
 * <p>DebugHandler provides callback handling for debug events during WebAssembly execution.
 */
@DisplayName("DebugHandler Tests")
class DebugHandlerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(DebugHandler.class.isInterface(), "DebugHandler should be an interface");
    }

    @Test
    @DisplayName("should be a functional interface")
    void shouldBeFunctionalInterface() {
      assertTrue(
          DebugHandler.class.isAnnotationPresent(FunctionalInterface.class),
          "DebugHandler should be annotated with @FunctionalInterface");
    }

    @Test
    @DisplayName("should have onEvent method")
    void shouldHaveOnEventMethod() throws NoSuchMethodException {
      final Method method = DebugHandler.class.getMethod("onEvent", DebugEvent.class);
      assertNotNull(method, "onEvent method should exist");
      assertEquals(DebugAction.class, method.getReturnType(), "Should return DebugAction");
    }

    @Test
    @DisplayName("should have onUnregister default method")
    void shouldHaveOnUnregisterMethod() throws NoSuchMethodException {
      final Method method = DebugHandler.class.getMethod("onUnregister");
      assertNotNull(method, "onUnregister method should exist");
      assertTrue(method.isDefault(), "onUnregister should be a default method");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("DebugAction Enum Tests")
  class DebugActionEnumTests {

    @Test
    @DisplayName("should have CONTINUE value")
    void shouldHaveContinueValue() {
      assertNotNull(DebugAction.valueOf("CONTINUE"), "CONTINUE should exist");
    }

    @Test
    @DisplayName("should have STEP value")
    void shouldHaveStepValue() {
      assertNotNull(DebugAction.valueOf("STEP"), "STEP should exist");
    }

    @Test
    @DisplayName("should have STEP_INTO value")
    void shouldHaveStepIntoValue() {
      assertNotNull(DebugAction.valueOf("STEP_INTO"), "STEP_INTO should exist");
    }

    @Test
    @DisplayName("should have STEP_OVER value")
    void shouldHaveStepOverValue() {
      assertNotNull(DebugAction.valueOf("STEP_OVER"), "STEP_OVER should exist");
    }

    @Test
    @DisplayName("should have STEP_OUT value")
    void shouldHaveStepOutValue() {
      assertNotNull(DebugAction.valueOf("STEP_OUT"), "STEP_OUT should exist");
    }

    @Test
    @DisplayName("should have PAUSE value")
    void shouldHavePauseValue() {
      assertNotNull(DebugAction.valueOf("PAUSE"), "PAUSE should exist");
    }

    @Test
    @DisplayName("should have ABORT value")
    void shouldHaveAbortValue() {
      assertNotNull(DebugAction.valueOf("ABORT"), "ABORT should exist");
    }

    @Test
    @DisplayName("should have exactly seven values")
    void shouldHaveExactlySevenValues() {
      assertEquals(7, DebugAction.values().length, "Should have exactly 7 DebugAction values");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have logging static method")
    void shouldHaveLoggingMethod() throws NoSuchMethodException {
      final Method method = DebugHandler.class.getMethod("logging");
      assertNotNull(method, "logging method should exist");
      assertEquals(DebugHandler.class, method.getReturnType(), "Should return DebugHandler");
    }

    @Test
    @DisplayName("should have breakAll static method")
    void shouldHaveBreakAllMethod() throws NoSuchMethodException {
      final Method method = DebugHandler.class.getMethod("breakAll");
      assertNotNull(method, "breakAll method should exist");
      assertEquals(DebugHandler.class, method.getReturnType(), "Should return DebugHandler");
    }

    @Test
    @DisplayName("should have breakOnTrap static method")
    void shouldHaveBreakOnTrapMethod() throws NoSuchMethodException {
      final Method method = DebugHandler.class.getMethod("breakOnTrap");
      assertNotNull(method, "breakOnTrap method should exist");
      assertEquals(DebugHandler.class, method.getReturnType(), "Should return DebugHandler");
    }
  }

  @Nested
  @DisplayName("Functional Implementation Tests")
  class FunctionalImplementationTests {

    @Test
    @DisplayName("logging handler should return CONTINUE")
    void loggingHandlerShouldReturnContinue() {
      final DebugHandler handler = DebugHandler.logging();
      final DebugEvent event = createDebugEvent(DebugEventType.BREAKPOINT);

      final DebugAction action = handler.onEvent(event);
      assertEquals(DebugAction.CONTINUE, action, "Logging handler should return CONTINUE");
    }

    @Test
    @DisplayName("breakAll handler should return PAUSE")
    void breakAllHandlerShouldReturnPause() {
      final DebugHandler handler = DebugHandler.breakAll();
      final DebugEvent event = createDebugEvent(DebugEventType.STEP);

      final DebugAction action = handler.onEvent(event);
      assertEquals(DebugAction.PAUSE, action, "BreakAll handler should return PAUSE");
    }

    @Test
    @DisplayName("breakOnTrap handler should return PAUSE on exception")
    void breakOnTrapHandlerShouldReturnPauseOnException() {
      final DebugHandler handler = DebugHandler.breakOnTrap();
      final DebugEvent trapEvent = createDebugEvent(DebugEventType.EXCEPTION);

      final DebugAction action = handler.onEvent(trapEvent);
      assertEquals(DebugAction.PAUSE, action, "BreakOnTrap handler should return PAUSE on trap");
    }

    @Test
    @DisplayName("breakOnTrap handler should return CONTINUE on non-trap")
    void breakOnTrapHandlerShouldReturnContinueOnNonTrap() {
      final DebugHandler handler = DebugHandler.breakOnTrap();
      final DebugEvent normalEvent = createDebugEvent(DebugEventType.STEP);

      final DebugAction action = handler.onEvent(normalEvent);
      assertEquals(
          DebugAction.CONTINUE, action, "BreakOnTrap handler should return CONTINUE on non-trap");
    }

    @Test
    @DisplayName("lambda handler should work correctly")
    void lambdaHandlerShouldWorkCorrectly() {
      final DebugHandler handler = event -> DebugAction.STEP;
      final DebugEvent event = createDebugEvent(DebugEventType.PAUSE);

      final DebugAction action = handler.onEvent(event);
      assertEquals(DebugAction.STEP, action, "Lambda handler should return STEP");
    }

    private DebugEvent createDebugEvent(final DebugEventType type) {
      return new DebugEvent(type, new MockExecutionState());
    }
  }

  /** Mock implementation of ExecutionState for testing. */
  private static class MockExecutionState implements ExecutionState {

    @Override
    public ExecutionStatus getStatus() {
      return ExecutionStatus.PAUSED;
    }

    @Override
    public long getInstructionPointer() {
      return 0x1000;
    }

    @Override
    public List<StackFrame> getStackFrames() {
      return Collections.emptyList();
    }

    @Override
    public String getCurrentModule() {
      return "test.wasm";
    }

    @Override
    public String getCurrentFunction() {
      return "testFunction";
    }

    @Override
    public ExecutionStatistics getStatistics() {
      return new MockExecutionStatistics();
    }
  }

  /** Mock implementation of ExecutionStatistics for testing. */
  private static class MockExecutionStatistics implements ExecutionStatistics {

    @Override
    public long getInstructionCount() {
      return 1000;
    }

    @Override
    public long getExecutionTime() {
      return 100;
    }

    @Override
    public long getFunctionCallCount() {
      return 10;
    }
  }
}
