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
 * Tests for {@link WasiResourceState} enum.
 *
 * <p>WasiResourceState tracks the lifecycle of WASI resources.
 */
@DisplayName("WasiResourceState Tests")
class WasiResourceStateTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have exactly 6 values")
    void shouldHaveExactly6Values() {
      final WasiResourceState[] values = WasiResourceState.values();
      assertEquals(6, values.length, "WasiResourceState should have 6 values");
    }

    @Test
    @DisplayName("should have CREATED")
    void shouldHaveCreated() {
      assertNotNull(WasiResourceState.valueOf("CREATED"), "CREATED should exist");
    }

    @Test
    @DisplayName("should have OPEN")
    void shouldHaveOpen() {
      assertNotNull(WasiResourceState.valueOf("OPEN"), "OPEN should exist");
    }

    @Test
    @DisplayName("should have ACTIVE")
    void shouldHaveActive() {
      assertNotNull(WasiResourceState.valueOf("ACTIVE"), "ACTIVE should exist");
    }

    @Test
    @DisplayName("should have SUSPENDED")
    void shouldHaveSuspended() {
      assertNotNull(WasiResourceState.valueOf("SUSPENDED"), "SUSPENDED should exist");
    }

    @Test
    @DisplayName("should have ERROR")
    void shouldHaveError() {
      assertNotNull(WasiResourceState.valueOf("ERROR"), "ERROR should exist");
    }

    @Test
    @DisplayName("should have CLOSED")
    void shouldHaveClosed() {
      assertNotNull(WasiResourceState.valueOf("CLOSED"), "CLOSED should exist");
    }
  }

  @Nested
  @DisplayName("isUsable Method Tests")
  class IsUsableMethodTests {

    @Test
    @DisplayName("CREATED should not be usable")
    void createdShouldNotBeUsable() {
      assertFalse(WasiResourceState.CREATED.isUsable(), "CREATED should not be usable");
    }

    @Test
    @DisplayName("OPEN should be usable")
    void openShouldBeUsable() {
      assertTrue(WasiResourceState.OPEN.isUsable(), "OPEN should be usable");
    }

    @Test
    @DisplayName("ACTIVE should be usable")
    void activeShouldBeUsable() {
      assertTrue(WasiResourceState.ACTIVE.isUsable(), "ACTIVE should be usable");
    }

    @Test
    @DisplayName("SUSPENDED should not be usable")
    void suspendedShouldNotBeUsable() {
      assertFalse(WasiResourceState.SUSPENDED.isUsable(), "SUSPENDED should not be usable");
    }

    @Test
    @DisplayName("ERROR should not be usable")
    void errorShouldNotBeUsable() {
      assertFalse(WasiResourceState.ERROR.isUsable(), "ERROR should not be usable");
    }

    @Test
    @DisplayName("CLOSED should not be usable")
    void closedShouldNotBeUsable() {
      assertFalse(WasiResourceState.CLOSED.isUsable(), "CLOSED should not be usable");
    }
  }

  @Nested
  @DisplayName("isTerminal Method Tests")
  class IsTerminalMethodTests {

    @Test
    @DisplayName("CREATED should not be terminal")
    void createdShouldNotBeTerminal() {
      assertFalse(WasiResourceState.CREATED.isTerminal(), "CREATED should not be terminal");
    }

    @Test
    @DisplayName("OPEN should not be terminal")
    void openShouldNotBeTerminal() {
      assertFalse(WasiResourceState.OPEN.isTerminal(), "OPEN should not be terminal");
    }

    @Test
    @DisplayName("ACTIVE should not be terminal")
    void activeShouldNotBeTerminal() {
      assertFalse(WasiResourceState.ACTIVE.isTerminal(), "ACTIVE should not be terminal");
    }

    @Test
    @DisplayName("SUSPENDED should not be terminal")
    void suspendedShouldNotBeTerminal() {
      assertFalse(WasiResourceState.SUSPENDED.isTerminal(), "SUSPENDED should not be terminal");
    }

    @Test
    @DisplayName("ERROR should be terminal")
    void errorShouldBeTerminal() {
      assertTrue(WasiResourceState.ERROR.isTerminal(), "ERROR should be terminal");
    }

    @Test
    @DisplayName("CLOSED should be terminal")
    void closedShouldBeTerminal() {
      assertTrue(WasiResourceState.CLOSED.isTerminal(), "CLOSED should be terminal");
    }
  }

  @Nested
  @DisplayName("State Consistency Tests")
  class StateConsistencyTests {

    @Test
    @DisplayName("usable states should not be terminal")
    void usableStatesShouldNotBeTerminal() {
      for (final WasiResourceState state : WasiResourceState.values()) {
        if (state.isUsable()) {
          assertFalse(state.isTerminal(), state + " is usable so should not be terminal");
        }
      }
    }

    @Test
    @DisplayName("terminal states should not be usable")
    void terminalStatesShouldNotBeUsable() {
      for (final WasiResourceState state : WasiResourceState.values()) {
        if (state.isTerminal()) {
          assertFalse(state.isUsable(), state + " is terminal so should not be usable");
        }
      }
    }
  }
}
