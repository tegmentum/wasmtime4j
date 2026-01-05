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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiInstanceState} enum.
 *
 * <p>WasiInstanceState tracks the lifecycle of component instances.
 */
@DisplayName("WasiInstanceState Tests")
class WasiInstanceStateTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have exactly 8 values")
    void shouldHaveExactly8Values() {
      final WasiInstanceState[] values = WasiInstanceState.values();
      assertEquals(8, values.length, "WasiInstanceState should have 8 values");
    }

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      assertNotNull(WasiInstanceState.valueOf("CREATED"), "CREATED should exist");
      assertNotNull(WasiInstanceState.valueOf("RUNNING"), "RUNNING should exist");
      assertNotNull(WasiInstanceState.valueOf("SUSPENDED"), "SUSPENDED should exist");
      assertNotNull(WasiInstanceState.valueOf("WAITING"), "WAITING should exist");
      assertNotNull(WasiInstanceState.valueOf("COMPLETED"), "COMPLETED should exist");
      assertNotNull(WasiInstanceState.valueOf("TERMINATED"), "TERMINATED should exist");
      assertNotNull(WasiInstanceState.valueOf("ERROR"), "ERROR should exist");
      assertNotNull(WasiInstanceState.valueOf("CLOSED"), "CLOSED should exist");
    }
  }

  @Nested
  @DisplayName("isActive Method Tests")
  class IsActiveMethodTests {

    @Test
    @DisplayName("CREATED should be active")
    void createdShouldBeActive() {
      assertTrue(WasiInstanceState.CREATED.isActive(), "CREATED should be active");
    }

    @Test
    @DisplayName("RUNNING should be active")
    void runningShouldBeActive() {
      assertTrue(WasiInstanceState.RUNNING.isActive(), "RUNNING should be active");
    }

    @Test
    @DisplayName("SUSPENDED should be active")
    void suspendedShouldBeActive() {
      assertTrue(WasiInstanceState.SUSPENDED.isActive(), "SUSPENDED should be active");
    }

    @Test
    @DisplayName("WAITING should be active")
    void waitingShouldBeActive() {
      assertTrue(WasiInstanceState.WAITING.isActive(), "WAITING should be active");
    }

    @Test
    @DisplayName("COMPLETED should be active")
    void completedShouldBeActive() {
      assertTrue(WasiInstanceState.COMPLETED.isActive(), "COMPLETED should be active");
    }

    @Test
    @DisplayName("TERMINATED should not be active")
    void terminatedShouldNotBeActive() {
      assertFalse(WasiInstanceState.TERMINATED.isActive(), "TERMINATED should not be active");
    }

    @Test
    @DisplayName("ERROR should not be active")
    void errorShouldNotBeActive() {
      assertFalse(WasiInstanceState.ERROR.isActive(), "ERROR should not be active");
    }

    @Test
    @DisplayName("CLOSED should not be active")
    void closedShouldNotBeActive() {
      assertFalse(WasiInstanceState.CLOSED.isActive(), "CLOSED should not be active");
    }
  }

  @Nested
  @DisplayName("isTerminal Method Tests")
  class IsTerminalMethodTests {

    @Test
    @DisplayName("CREATED should not be terminal")
    void createdShouldNotBeTerminal() {
      assertFalse(WasiInstanceState.CREATED.isTerminal(), "CREATED should not be terminal");
    }

    @Test
    @DisplayName("RUNNING should not be terminal")
    void runningShouldNotBeTerminal() {
      assertFalse(WasiInstanceState.RUNNING.isTerminal(), "RUNNING should not be terminal");
    }

    @Test
    @DisplayName("TERMINATED should be terminal")
    void terminatedShouldBeTerminal() {
      assertTrue(WasiInstanceState.TERMINATED.isTerminal(), "TERMINATED should be terminal");
    }

    @Test
    @DisplayName("ERROR should be terminal")
    void errorShouldBeTerminal() {
      assertTrue(WasiInstanceState.ERROR.isTerminal(), "ERROR should be terminal");
    }

    @Test
    @DisplayName("CLOSED should be terminal")
    void closedShouldBeTerminal() {
      assertTrue(WasiInstanceState.CLOSED.isTerminal(), "CLOSED should be terminal");
    }
  }

  @Nested
  @DisplayName("isCallable Method Tests")
  class IsCallableMethodTests {

    @Test
    @DisplayName("CREATED should be callable")
    void createdShouldBeCallable() {
      assertTrue(WasiInstanceState.CREATED.isCallable(), "CREATED should be callable");
    }

    @Test
    @DisplayName("COMPLETED should be callable")
    void completedShouldBeCallable() {
      assertTrue(WasiInstanceState.COMPLETED.isCallable(), "COMPLETED should be callable");
    }

    @Test
    @DisplayName("RUNNING should not be callable")
    void runningShouldNotBeCallable() {
      assertFalse(WasiInstanceState.RUNNING.isCallable(), "RUNNING should not be callable");
    }

    @Test
    @DisplayName("SUSPENDED should not be callable")
    void suspendedShouldNotBeCallable() {
      assertFalse(WasiInstanceState.SUSPENDED.isCallable(), "SUSPENDED should not be callable");
    }

    @Test
    @DisplayName("CLOSED should not be callable")
    void closedShouldNotBeCallable() {
      assertFalse(WasiInstanceState.CLOSED.isCallable(), "CLOSED should not be callable");
    }
  }

  @Nested
  @DisplayName("isSuspendable Method Tests")
  class IsSuspendableMethodTests {

    @Test
    @DisplayName("RUNNING should be suspendable")
    void runningShouldBeSuspendable() {
      assertTrue(WasiInstanceState.RUNNING.isSuspendable(), "RUNNING should be suspendable");
    }

    @Test
    @DisplayName("WAITING should be suspendable")
    void waitingShouldBeSuspendable() {
      assertTrue(WasiInstanceState.WAITING.isSuspendable(), "WAITING should be suspendable");
    }

    @Test
    @DisplayName("CREATED should not be suspendable")
    void createdShouldNotBeSuspendable() {
      assertFalse(WasiInstanceState.CREATED.isSuspendable(), "CREATED should not be suspendable");
    }

    @Test
    @DisplayName("SUSPENDED should not be suspendable")
    void suspendedShouldNotBeSuspendable() {
      assertFalse(
          WasiInstanceState.SUSPENDED.isSuspendable(), "SUSPENDED should not be suspendable");
    }
  }

  @Nested
  @DisplayName("isResumable Method Tests")
  class IsResumableMethodTests {

    @Test
    @DisplayName("SUSPENDED should be resumable")
    void suspendedShouldBeResumable() {
      assertTrue(WasiInstanceState.SUSPENDED.isResumable(), "SUSPENDED should be resumable");
    }

    @Test
    @DisplayName("RUNNING should not be resumable")
    void runningShouldNotBeResumable() {
      assertFalse(WasiInstanceState.RUNNING.isResumable(), "RUNNING should not be resumable");
    }

    @Test
    @DisplayName("WAITING should not be resumable")
    void waitingShouldNotBeResumable() {
      assertFalse(WasiInstanceState.WAITING.isResumable(), "WAITING should not be resumable");
    }
  }

  @Nested
  @DisplayName("getDescription Method Tests")
  class GetDescriptionMethodTests {

    @Test
    @DisplayName("all states should have descriptions")
    void allStatesShouldHaveDescriptions() {
      for (final WasiInstanceState state : WasiInstanceState.values()) {
        final String description = state.getDescription();
        assertNotNull(description, state + " should have description");
        assertFalse(description.isEmpty(), state + " description should not be empty");
      }
    }

    @Test
    @DisplayName("CREATED description should mention ready")
    void createdDescriptionShouldMentionReady() {
      assertTrue(
          WasiInstanceState.CREATED.getDescription().toLowerCase().contains("ready"),
          "CREATED description should mention ready");
    }

    @Test
    @DisplayName("RUNNING description should mention executing")
    void runningDescriptionShouldMentionExecuting() {
      assertTrue(
          WasiInstanceState.RUNNING.getDescription().toLowerCase().contains("executing"),
          "RUNNING description should mention executing");
    }

    @Test
    @DisplayName("ERROR description should mention error")
    void errorDescriptionShouldMentionError() {
      assertTrue(
          WasiInstanceState.ERROR.getDescription().toLowerCase().contains("error"),
          "ERROR description should mention error");
    }
  }

  @Nested
  @DisplayName("State Consistency Tests")
  class StateConsistencyTests {

    @Test
    @DisplayName("active and terminal should be mutually exclusive")
    void activeAndTerminalShouldBeMutuallyExclusive() {
      for (final WasiInstanceState state : WasiInstanceState.values()) {
        if (state.isActive()) {
          assertFalse(state.isTerminal(), state + " active state should not be terminal");
        }
        if (state.isTerminal()) {
          assertFalse(state.isActive(), state + " terminal state should not be active");
        }
      }
    }

    @Test
    @DisplayName("callable states should be active")
    void callableStatesShouldBeActive() {
      for (final WasiInstanceState state : WasiInstanceState.values()) {
        if (state.isCallable()) {
          assertTrue(state.isActive(), state + " callable state should be active");
        }
      }
    }

    @Test
    @DisplayName("terminal states should not be callable")
    void terminalStatesShouldNotBeCallable() {
      for (final WasiInstanceState state : WasiInstanceState.values()) {
        if (state.isTerminal()) {
          assertFalse(state.isCallable(), state + " terminal state should not be callable");
        }
      }
    }
  }
}
