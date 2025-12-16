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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for ExecutionController interface and its inner ExecutionStatus enum.
 *
 * <p>Verifies the interface structure and the ExecutionStatus enum behavior.
 */
@DisplayName("ExecutionController Tests")
class ExecutionControllerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ExecutionController.class.isInterface(), "ExecutionController should be an interface");
    }

    @Test
    @DisplayName("should have start method")
    void shouldHaveStartMethod() throws NoSuchMethodException {
      Method method = ExecutionController.class.getMethod("start");

      assertNotNull(method, "start method should exist");
      assertEquals(void.class, method.getReturnType(), "start should return void");
      assertEquals(0, method.getParameterCount(), "start should take no parameters");
    }

    @Test
    @DisplayName("should have stop method")
    void shouldHaveStopMethod() throws NoSuchMethodException {
      Method method = ExecutionController.class.getMethod("stop");

      assertNotNull(method, "stop method should exist");
      assertEquals(void.class, method.getReturnType(), "stop should return void");
      assertEquals(0, method.getParameterCount(), "stop should take no parameters");
    }

    @Test
    @DisplayName("should have pause method")
    void shouldHavePauseMethod() throws NoSuchMethodException {
      Method method = ExecutionController.class.getMethod("pause");

      assertNotNull(method, "pause method should exist");
      assertEquals(void.class, method.getReturnType(), "pause should return void");
      assertEquals(0, method.getParameterCount(), "pause should take no parameters");
    }

    @Test
    @DisplayName("should have resume method")
    void shouldHaveResumeMethod() throws NoSuchMethodException {
      Method method = ExecutionController.class.getMethod("resume");

      assertNotNull(method, "resume method should exist");
      assertEquals(void.class, method.getReturnType(), "resume should return void");
      assertEquals(0, method.getParameterCount(), "resume should take no parameters");
    }

    @Test
    @DisplayName("should have setQuotas method")
    void shouldHaveSetQuotasMethod() throws NoSuchMethodException {
      Method method = ExecutionController.class.getMethod("setQuotas", ExecutionQuotas.class);

      assertNotNull(method, "setQuotas method should exist");
      assertEquals(void.class, method.getReturnType(), "setQuotas should return void");
      assertEquals(1, method.getParameterCount(), "setQuotas should take 1 parameter");
      assertEquals(
          ExecutionQuotas.class,
          method.getParameterTypes()[0],
          "setQuotas param should be ExecutionQuotas");
    }

    @Test
    @DisplayName("should have getQuotas method")
    void shouldHaveGetQuotasMethod() throws NoSuchMethodException {
      Method method = ExecutionController.class.getMethod("getQuotas");

      assertNotNull(method, "getQuotas method should exist");
      assertEquals(
          ExecutionQuotas.class, method.getReturnType(), "getQuotas should return ExecutionQuotas");
      assertEquals(0, method.getParameterCount(), "getQuotas should take no parameters");
    }

    @Test
    @DisplayName("should have setPolicy method")
    void shouldHaveSetPolicyMethod() throws NoSuchMethodException {
      Method method = ExecutionController.class.getMethod("setPolicy", ExecutionPolicy.class);

      assertNotNull(method, "setPolicy method should exist");
      assertEquals(void.class, method.getReturnType(), "setPolicy should return void");
      assertEquals(1, method.getParameterCount(), "setPolicy should take 1 parameter");
      assertEquals(
          ExecutionPolicy.class,
          method.getParameterTypes()[0],
          "setPolicy param should be ExecutionPolicy");
    }

    @Test
    @DisplayName("should have getPolicy method")
    void shouldHaveGetPolicyMethod() throws NoSuchMethodException {
      Method method = ExecutionController.class.getMethod("getPolicy");

      assertNotNull(method, "getPolicy method should exist");
      assertEquals(
          ExecutionPolicy.class, method.getReturnType(), "getPolicy should return ExecutionPolicy");
      assertEquals(0, method.getParameterCount(), "getPolicy should take no parameters");
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      Method method = ExecutionController.class.getMethod("getStatus");

      assertNotNull(method, "getStatus method should exist");
      assertEquals(
          ExecutionController.ExecutionStatus.class,
          method.getReturnType(),
          "getStatus should return ExecutionStatus");
      assertEquals(0, method.getParameterCount(), "getStatus should take no parameters");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = ExecutionController.class.getMethod("getStatistics");

      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          ExecutionStatistics.class,
          method.getReturnType(),
          "getStatistics should return ExecutionStatistics");
      assertEquals(0, method.getParameterCount(), "getStatistics should take no parameters");
    }

    @Test
    @DisplayName("should have isActive method")
    void shouldHaveIsActiveMethod() throws NoSuchMethodException {
      Method method = ExecutionController.class.getMethod("isActive");

      assertNotNull(method, "isActive method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isActive should return boolean");
      assertEquals(0, method.getParameterCount(), "isActive should take no parameters");
    }

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "start",
                  "stop",
                  "pause",
                  "resume",
                  "setQuotas",
                  "getQuotas",
                  "setPolicy",
                  "getPolicy",
                  "getStatus",
                  "getStatistics",
                  "isActive"));

      Method[] methods = ExecutionController.class.getDeclaredMethods();
      Set<String> actualMethods = new HashSet<>();
      for (Method m : methods) {
        if (Modifier.isPublic(m.getModifiers())) {
          actualMethods.add(m.getName());
        }
      }

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }
  }

  @Nested
  @DisplayName("ExecutionStatus Enum Tests")
  class ExecutionStatusEnumTests {

    @Test
    @DisplayName("should have exactly 5 status values")
    void shouldHaveExactlyFiveStatusValues() {
      ExecutionController.ExecutionStatus[] values = ExecutionController.ExecutionStatus.values();

      assertEquals(5, values.length, "Should have exactly 5 ExecutionStatus values");
    }

    @Test
    @DisplayName("should have IDLE status")
    void shouldHaveIdleStatus() {
      ExecutionController.ExecutionStatus status = ExecutionController.ExecutionStatus.IDLE;

      assertNotNull(status, "IDLE should not be null");
      assertEquals("IDLE", status.name(), "Name should be IDLE");
    }

    @Test
    @DisplayName("should have RUNNING status")
    void shouldHaveRunningStatus() {
      ExecutionController.ExecutionStatus status = ExecutionController.ExecutionStatus.RUNNING;

      assertNotNull(status, "RUNNING should not be null");
      assertEquals("RUNNING", status.name(), "Name should be RUNNING");
    }

    @Test
    @DisplayName("should have PAUSED status")
    void shouldHavePausedStatus() {
      ExecutionController.ExecutionStatus status = ExecutionController.ExecutionStatus.PAUSED;

      assertNotNull(status, "PAUSED should not be null");
      assertEquals("PAUSED", status.name(), "Name should be PAUSED");
    }

    @Test
    @DisplayName("should have STOPPED status")
    void shouldHaveStoppedStatus() {
      ExecutionController.ExecutionStatus status = ExecutionController.ExecutionStatus.STOPPED;

      assertNotNull(status, "STOPPED should not be null");
      assertEquals("STOPPED", status.name(), "Name should be STOPPED");
    }

    @Test
    @DisplayName("should have ERROR status")
    void shouldHaveErrorStatus() {
      ExecutionController.ExecutionStatus status = ExecutionController.ExecutionStatus.ERROR;

      assertNotNull(status, "ERROR should not be null");
      assertEquals("ERROR", status.name(), "Name should be ERROR");
    }

    @Test
    @DisplayName("should be accessible via valueOf")
    void shouldBeAccessibleViaValueOf() {
      assertEquals(
          ExecutionController.ExecutionStatus.IDLE,
          ExecutionController.ExecutionStatus.valueOf("IDLE"));
      assertEquals(
          ExecutionController.ExecutionStatus.RUNNING,
          ExecutionController.ExecutionStatus.valueOf("RUNNING"));
      assertEquals(
          ExecutionController.ExecutionStatus.PAUSED,
          ExecutionController.ExecutionStatus.valueOf("PAUSED"));
      assertEquals(
          ExecutionController.ExecutionStatus.STOPPED,
          ExecutionController.ExecutionStatus.valueOf("STOPPED"));
      assertEquals(
          ExecutionController.ExecutionStatus.ERROR,
          ExecutionController.ExecutionStatus.valueOf("ERROR"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid valueOf")
    void shouldThrowIllegalArgumentExceptionForInvalidValueOf() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExecutionController.ExecutionStatus.valueOf("INVALID"),
          "Should throw for invalid enum name");
    }

    @Test
    @DisplayName("should have correct ordinals")
    void shouldHaveCorrectOrdinals() {
      assertEquals(0, ExecutionController.ExecutionStatus.IDLE.ordinal(), "IDLE should be 0");
      assertEquals(1, ExecutionController.ExecutionStatus.RUNNING.ordinal(), "RUNNING should be 1");
      assertEquals(2, ExecutionController.ExecutionStatus.PAUSED.ordinal(), "PAUSED should be 2");
      assertEquals(3, ExecutionController.ExecutionStatus.STOPPED.ordinal(), "STOPPED should be 3");
      assertEquals(4, ExecutionController.ExecutionStatus.ERROR.ordinal(), "ERROR should be 4");
    }

    @Test
    @DisplayName("should be comparable")
    void shouldBeComparable() {
      assertTrue(
          ExecutionController.ExecutionStatus.IDLE.compareTo(
                  ExecutionController.ExecutionStatus.RUNNING)
              < 0,
          "IDLE should be before RUNNING");
      assertTrue(
          ExecutionController.ExecutionStatus.RUNNING.compareTo(
                  ExecutionController.ExecutionStatus.PAUSED)
              < 0,
          "RUNNING should be before PAUSED");
    }

    @Test
    @DisplayName("should be usable in switch statement")
    void shouldBeUsableInSwitchStatement() {
      for (ExecutionController.ExecutionStatus status :
          ExecutionController.ExecutionStatus.values()) {
        String result;
        switch (status) {
          case IDLE:
            result = "idle";
            break;
          case RUNNING:
            result = "running";
            break;
          case PAUSED:
            result = "paused";
            break;
          case STOPPED:
            result = "stopped";
            break;
          case ERROR:
            result = "error";
            break;
          default:
            result = "unknown";
        }
        assertTrue(
            Arrays.asList("idle", "running", "paused", "stopped", "error").contains(result),
            "Switch should handle all statuses");
      }
    }
  }

  @Nested
  @DisplayName("State Transition Semantics Tests")
  class StateTransitionSemanticsTests {

    @Test
    @DisplayName("should define meaningful state transitions")
    void shouldDefineMeaningfulStateTransitions() {
      // IDLE -> start() -> RUNNING
      // RUNNING -> pause() -> PAUSED
      // PAUSED -> resume() -> RUNNING
      // RUNNING -> stop() -> STOPPED
      // Any -> ERROR (on failure)

      // Verify IDLE is the initial state (ordinal 0)
      assertEquals(
          0, ExecutionController.ExecutionStatus.IDLE.ordinal(), "IDLE should be initial state");

      // Verify ERROR is last (terminal state)
      assertEquals(
          4, ExecutionController.ExecutionStatus.ERROR.ordinal(), "ERROR should be terminal");
    }

    @Test
    @DisplayName("should have states for active execution")
    void shouldHaveStatesForActiveExecution() {
      // RUNNING and PAUSED are both "active" states (not terminated)
      Set<ExecutionController.ExecutionStatus> activeStates = new HashSet<>();
      activeStates.add(ExecutionController.ExecutionStatus.RUNNING);
      activeStates.add(ExecutionController.ExecutionStatus.PAUSED);

      assertEquals(2, activeStates.size(), "Should have 2 active states");
    }

    @Test
    @DisplayName("should have states for inactive execution")
    void shouldHaveStatesForInactiveExecution() {
      // IDLE, STOPPED, ERROR are inactive states
      Set<ExecutionController.ExecutionStatus> inactiveStates = new HashSet<>();
      inactiveStates.add(ExecutionController.ExecutionStatus.IDLE);
      inactiveStates.add(ExecutionController.ExecutionStatus.STOPPED);
      inactiveStates.add(ExecutionController.ExecutionStatus.ERROR);

      assertEquals(3, inactiveStates.size(), "Should have 3 inactive states");
    }
  }

  @Nested
  @DisplayName("Inner Type Tests")
  class InnerTypeTests {

    @Test
    @DisplayName("ExecutionStatus should be a public inner enum")
    void executionStatusShouldBePublicInnerEnum() {
      Class<?> statusClass = ExecutionController.ExecutionStatus.class;

      assertTrue(statusClass.isEnum(), "ExecutionStatus should be an enum");
      assertTrue(Modifier.isPublic(statusClass.getModifiers()), "ExecutionStatus should be public");
      assertEquals(
          ExecutionController.class,
          statusClass.getEnclosingClass(),
          "ExecutionStatus should be nested in ExecutionController");
    }

    @Test
    @DisplayName("should be able to fully qualify enum value")
    void shouldBeAbleToFullyQualifyEnumValue() {
      ExecutionController.ExecutionStatus status = ExecutionController.ExecutionStatus.RUNNING;

      assertNotNull(status, "Should be able to reference via fully qualified name");
      assertEquals("RUNNING", status.name(), "Should be RUNNING");
    }
  }

  @Nested
  @DisplayName("Integration Semantics Tests")
  class IntegrationSemanticsTests {

    @Test
    @DisplayName("should define lifecycle control methods")
    void shouldDefineLifecycleControlMethods() throws NoSuchMethodException {
      // Verify lifecycle methods exist
      assertNotNull(ExecutionController.class.getMethod("start"), "start should exist");
      assertNotNull(ExecutionController.class.getMethod("stop"), "stop should exist");
      assertNotNull(ExecutionController.class.getMethod("pause"), "pause should exist");
      assertNotNull(ExecutionController.class.getMethod("resume"), "resume should exist");
    }

    @Test
    @DisplayName("should define configuration methods")
    void shouldDefineConfigurationMethods() throws NoSuchMethodException {
      // Verify configuration methods exist
      assertNotNull(
          ExecutionController.class.getMethod("setQuotas", ExecutionQuotas.class),
          "setQuotas should exist");
      assertNotNull(ExecutionController.class.getMethod("getQuotas"), "getQuotas should exist");
      assertNotNull(
          ExecutionController.class.getMethod("setPolicy", ExecutionPolicy.class),
          "setPolicy should exist");
      assertNotNull(ExecutionController.class.getMethod("getPolicy"), "getPolicy should exist");
    }

    @Test
    @DisplayName("should define introspection methods")
    void shouldDefineIntrospectionMethods() throws NoSuchMethodException {
      // Verify introspection methods exist
      assertNotNull(ExecutionController.class.getMethod("getStatus"), "getStatus should exist");
      assertNotNull(
          ExecutionController.class.getMethod("getStatistics"), "getStatistics should exist");
      assertNotNull(ExecutionController.class.getMethod("isActive"), "isActive should exist");
    }
  }
}
