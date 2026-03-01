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
package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiClockId;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WASI enum types.
 *
 * <p>These tests exercise actual code execution for WasiClockId to improve JaCoCo coverage.
 */
@DisplayName("WASI Enum Integration Tests")
public class WasiEnumTest {

  private static final Logger LOGGER = Logger.getLogger(WasiEnumTest.class.getName());

  @Nested
  @DisplayName("WasiClockId Tests")
  class WasiClockIdTests {

    @Test
    @DisplayName("Should have correct enum values")
    void shouldHaveCorrectEnumValues() {
      LOGGER.info("Testing WasiClockId enum values");

      assertEquals(0, WasiClockId.REALTIME.getValue(), "REALTIME should be 0");
      assertEquals(1, WasiClockId.MONOTONIC.getValue(), "MONOTONIC should be 1");
      assertEquals(2, WasiClockId.PROCESS_CPUTIME_ID.getValue(), "PROCESS_CPUTIME_ID should be 2");
      assertEquals(3, WasiClockId.THREAD_CPUTIME_ID.getValue(), "THREAD_CPUTIME_ID should be 3");

      LOGGER.info("WasiClockId values verified");
    }

    @Test
    @DisplayName("Should find clock ID by value")
    void shouldFindClockIdByValue() {
      LOGGER.info("Testing WasiClockId.fromValue");

      assertEquals(
          WasiClockId.REALTIME, WasiClockId.fromValue(0), "Value 0 should return REALTIME");
      assertEquals(
          WasiClockId.MONOTONIC, WasiClockId.fromValue(1), "Value 1 should return MONOTONIC");
      assertEquals(
          WasiClockId.PROCESS_CPUTIME_ID,
          WasiClockId.fromValue(2),
          "Value 2 should return PROCESS_CPUTIME_ID");
      assertEquals(
          WasiClockId.THREAD_CPUTIME_ID,
          WasiClockId.fromValue(3),
          "Value 3 should return THREAD_CPUTIME_ID");

      LOGGER.info("WasiClockId.fromValue verified");
    }

    @Test
    @DisplayName("Should throw exception for invalid clock ID")
    void shouldThrowExceptionForInvalidClockId() {
      LOGGER.info("Testing invalid clock ID");

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> WasiClockId.fromValue(99));

      assertTrue(
          ex.getMessage().contains("Invalid clock ID"),
          "Error should mention invalid clock ID: " + ex.getMessage());

      LOGGER.info("Correctly threw exception: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should iterate all clock IDs")
    void shouldIterateAllClockIds() {
      LOGGER.info("Testing all clock IDs");

      assertEquals(4, WasiClockId.values().length, "Should have 4 clock IDs");

      for (final WasiClockId clockId : WasiClockId.values()) {
        final int value = clockId.getValue();
        final WasiClockId found = WasiClockId.fromValue(value);
        assertEquals(clockId, found, "Should find " + clockId + " by value " + value);
        LOGGER.fine("Verified: " + clockId + " = " + value);
      }

      LOGGER.info("All clock IDs verified");
    }
  }
}
