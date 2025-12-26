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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link WasiTimeOperations}. */
@DisplayName("WasiTimeOperations Tests")
class WasiTimeOperationsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiTimeOperations should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiTimeOperations.class.getModifiers()),
          "WasiTimeOperations should be final");
    }
  }

  @Nested
  @DisplayName("Clock Constants Tests")
  class ClockConstantsTests {

    @Test
    @DisplayName("Should have correct WASI_CLOCK_REALTIME value")
    void shouldHaveCorrectRealtimeValue() {
      assertEquals(0, WasiTimeOperations.WASI_CLOCK_REALTIME, "WASI_CLOCK_REALTIME should be 0");
    }

    @Test
    @DisplayName("Should have correct WASI_CLOCK_MONOTONIC value")
    void shouldHaveCorrectMonotonicValue() {
      assertEquals(1, WasiTimeOperations.WASI_CLOCK_MONOTONIC, "WASI_CLOCK_MONOTONIC should be 1");
    }

    @Test
    @DisplayName("Should have correct WASI_CLOCK_PROCESS_CPUTIME_ID value")
    void shouldHaveCorrectProcessCputimeValue() {
      assertEquals(
          2,
          WasiTimeOperations.WASI_CLOCK_PROCESS_CPUTIME_ID,
          "WASI_CLOCK_PROCESS_CPUTIME_ID should be 2");
    }

    @Test
    @DisplayName("Should have correct WASI_CLOCK_THREAD_CPUTIME_ID value")
    void shouldHaveCorrectThreadCputimeValue() {
      assertEquals(
          3,
          WasiTimeOperations.WASI_CLOCK_THREAD_CPUTIME_ID,
          "WASI_CLOCK_THREAD_CPUTIME_ID should be 3");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should throw on null context")
    void constructorShouldThrowOnNullContext() {
      assertThrows(
          PanamaException.class,
          () -> new WasiTimeOperations(null, null),
          "Should throw on null context");
    }
  }

  @Nested
  @DisplayName("isClockSupported Tests")
  class IsClockSupportedTests {

    @Test
    @DisplayName("Should return true for REALTIME clock")
    void shouldReturnTrueForRealtimeClock() {
      assertTrue(
          WasiTimeOperations.isClockSupported(WasiTimeOperations.WASI_CLOCK_REALTIME),
          "REALTIME clock should be supported");
    }

    @Test
    @DisplayName("Should return true for MONOTONIC clock")
    void shouldReturnTrueForMonotonicClock() {
      assertTrue(
          WasiTimeOperations.isClockSupported(WasiTimeOperations.WASI_CLOCK_MONOTONIC),
          "MONOTONIC clock should be supported");
    }

    @Test
    @DisplayName("Should return true for PROCESS_CPUTIME clock")
    void shouldReturnTrueForProcessCputimeClock() {
      assertTrue(
          WasiTimeOperations.isClockSupported(WasiTimeOperations.WASI_CLOCK_PROCESS_CPUTIME_ID),
          "PROCESS_CPUTIME clock should be supported");
    }

    @Test
    @DisplayName("Should return true for THREAD_CPUTIME clock")
    void shouldReturnTrueForThreadCputimeClock() {
      assertTrue(
          WasiTimeOperations.isClockSupported(WasiTimeOperations.WASI_CLOCK_THREAD_CPUTIME_ID),
          "THREAD_CPUTIME clock should be supported");
    }

    @Test
    @DisplayName("Should return false for negative clock ID")
    void shouldReturnFalseForNegativeClockId() {
      assertFalse(
          WasiTimeOperations.isClockSupported(-1), "Negative clock ID should not be supported");
    }

    @Test
    @DisplayName("Should return false for clock ID above max")
    void shouldReturnFalseForClockIdAboveMax() {
      assertFalse(WasiTimeOperations.isClockSupported(4), "Clock ID 4 should not be supported");
      assertFalse(WasiTimeOperations.isClockSupported(100), "Clock ID 100 should not be supported");
    }
  }

  @Nested
  @DisplayName("getClockName Tests")
  class GetClockNameTests {

    @Test
    @DisplayName("Should return REALTIME for clock ID 0")
    void shouldReturnRealtimeForClockId0() {
      assertEquals(
          "REALTIME",
          WasiTimeOperations.getClockName(WasiTimeOperations.WASI_CLOCK_REALTIME),
          "Clock name for REALTIME should be 'REALTIME'");
    }

    @Test
    @DisplayName("Should return MONOTONIC for clock ID 1")
    void shouldReturnMonotonicForClockId1() {
      assertEquals(
          "MONOTONIC",
          WasiTimeOperations.getClockName(WasiTimeOperations.WASI_CLOCK_MONOTONIC),
          "Clock name for MONOTONIC should be 'MONOTONIC'");
    }

    @Test
    @DisplayName("Should return PROCESS_CPUTIME for clock ID 2")
    void shouldReturnProcessCputimeForClockId2() {
      assertEquals(
          "PROCESS_CPUTIME",
          WasiTimeOperations.getClockName(WasiTimeOperations.WASI_CLOCK_PROCESS_CPUTIME_ID),
          "Clock name for PROCESS_CPUTIME should be 'PROCESS_CPUTIME'");
    }

    @Test
    @DisplayName("Should return THREAD_CPUTIME for clock ID 3")
    void shouldReturnThreadCputimeForClockId3() {
      assertEquals(
          "THREAD_CPUTIME",
          WasiTimeOperations.getClockName(WasiTimeOperations.WASI_CLOCK_THREAD_CPUTIME_ID),
          "Clock name for THREAD_CPUTIME should be 'THREAD_CPUTIME'");
    }

    @Test
    @DisplayName("Should return UNKNOWN for invalid clock ID")
    void shouldReturnUnknownForInvalidClockId() {
      assertTrue(
          WasiTimeOperations.getClockName(4).contains("UNKNOWN"),
          "Clock name for invalid ID should contain 'UNKNOWN'");
      assertTrue(
          WasiTimeOperations.getClockName(-1).contains("UNKNOWN"),
          "Clock name for negative ID should contain 'UNKNOWN'");
    }

    @Test
    @DisplayName("Should include clock ID in UNKNOWN name")
    void shouldIncludeClockIdInUnknownName() {
      String name = WasiTimeOperations.getClockName(99);
      assertTrue(name.contains("99"), "UNKNOWN clock name should include the clock ID");
    }
  }

  @Nested
  @DisplayName("convertTime Tests")
  class ConvertTimeTests {

    @Test
    @DisplayName("Should throw on null time unit")
    void shouldThrowOnNullTimeUnit() {
      assertThrows(
          PanamaException.class,
          () -> WasiTimeOperations.convertTime(1000000000L, null),
          "Should throw on null time unit");
    }

    @Test
    @DisplayName("Should convert nanoseconds to nanoseconds correctly")
    void shouldConvertNanosecondsToNanosecondsCorrectly() {
      final long nanoseconds = 1234567890L;
      assertEquals(
          nanoseconds,
          WasiTimeOperations.convertTime(nanoseconds, TimeUnit.NANOSECONDS),
          "Nanoseconds to nanoseconds should be unchanged");
    }

    @Test
    @DisplayName("Should convert nanoseconds to microseconds correctly")
    void shouldConvertNanosecondsToMicrosecondsCorrectly() {
      final long nanoseconds = 1000000L;
      assertEquals(
          1000L,
          WasiTimeOperations.convertTime(nanoseconds, TimeUnit.MICROSECONDS),
          "1000000 nanoseconds should equal 1000 microseconds");
    }

    @Test
    @DisplayName("Should convert nanoseconds to milliseconds correctly")
    void shouldConvertNanosecondsToMillisecondsCorrectly() {
      final long nanoseconds = 1000000000L;
      assertEquals(
          1000L,
          WasiTimeOperations.convertTime(nanoseconds, TimeUnit.MILLISECONDS),
          "1000000000 nanoseconds should equal 1000 milliseconds");
    }

    @Test
    @DisplayName("Should convert nanoseconds to seconds correctly")
    void shouldConvertNanosecondsToSecondsCorrectly() {
      final long nanoseconds = 5000000000L;
      assertEquals(
          5L,
          WasiTimeOperations.convertTime(nanoseconds, TimeUnit.SECONDS),
          "5000000000 nanoseconds should equal 5 seconds");
    }

    @Test
    @DisplayName("Should convert nanoseconds to minutes correctly")
    void shouldConvertNanosecondsToMinutesCorrectly() {
      final long nanoseconds = 60000000000L;
      assertEquals(
          1L,
          WasiTimeOperations.convertTime(nanoseconds, TimeUnit.MINUTES),
          "60000000000 nanoseconds should equal 1 minute");
    }

    @Test
    @DisplayName("Should convert nanoseconds to hours correctly")
    void shouldConvertNanosecondsToHoursCorrectly() {
      final long nanoseconds = 3600000000000L;
      assertEquals(
          1L,
          WasiTimeOperations.convertTime(nanoseconds, TimeUnit.HOURS),
          "3600000000000 nanoseconds should equal 1 hour");
    }

    @Test
    @DisplayName("Should convert nanoseconds to days correctly")
    void shouldConvertNanosecondsToDaysCorrectly() {
      final long nanoseconds = 86400000000000L;
      assertEquals(
          1L,
          WasiTimeOperations.convertTime(nanoseconds, TimeUnit.DAYS),
          "86400000000000 nanoseconds should equal 1 day");
    }

    @Test
    @DisplayName("Should handle zero nanoseconds")
    void shouldHandleZeroNanoseconds() {
      assertEquals(
          0L,
          WasiTimeOperations.convertTime(0L, TimeUnit.SECONDS),
          "Zero nanoseconds should convert to zero in any unit");
    }

    @Test
    @DisplayName("Should handle negative nanoseconds")
    void shouldHandleNegativeNanoseconds() {
      final long negativeNanos = -1000000000L;
      assertEquals(
          -1L,
          WasiTimeOperations.convertTime(negativeNanos, TimeUnit.SECONDS),
          "Negative nanoseconds should convert to negative seconds");
    }
  }

  @Nested
  @DisplayName("Clock ID Boundary Tests")
  class ClockIdBoundaryTests {

    @Test
    @DisplayName("Should support clock ID 0")
    void shouldSupportClockId0() {
      assertTrue(WasiTimeOperations.isClockSupported(0), "Clock ID 0 should be supported");
    }

    @Test
    @DisplayName("Should support clock ID 3")
    void shouldSupportClockId3() {
      assertTrue(WasiTimeOperations.isClockSupported(3), "Clock ID 3 should be supported");
    }

    @Test
    @DisplayName("Should not support clock ID 4")
    void shouldNotSupportClockId4() {
      assertFalse(WasiTimeOperations.isClockSupported(4), "Clock ID 4 should not be supported");
    }

    @Test
    @DisplayName("Should not support Integer.MAX_VALUE clock ID")
    void shouldNotSupportMaxIntClockId() {
      assertFalse(
          WasiTimeOperations.isClockSupported(Integer.MAX_VALUE),
          "MAX_VALUE clock ID should not be supported");
    }

    @Test
    @DisplayName("Should not support Integer.MIN_VALUE clock ID")
    void shouldNotSupportMinIntClockId() {
      assertFalse(
          WasiTimeOperations.isClockSupported(Integer.MIN_VALUE),
          "MIN_VALUE clock ID should not be supported");
    }
  }
}
