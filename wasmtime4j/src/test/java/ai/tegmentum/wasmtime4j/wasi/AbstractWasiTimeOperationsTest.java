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

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link AbstractWasiTimeOperations} static constants and utility methods. */
@DisplayName("AbstractWasiTimeOperations Tests")
class AbstractWasiTimeOperationsTest {

  @Nested
  @DisplayName("Clock ID Constants")
  class ClockIdConstants {

    @Test
    @DisplayName("WASI_CLOCK_REALTIME should match WasiClockId.REALTIME")
    void realtimeConstantShouldMatch() {
      assertEquals(WasiClockId.REALTIME.getValue(), AbstractWasiTimeOperations.WASI_CLOCK_REALTIME);
    }

    @Test
    @DisplayName("WASI_CLOCK_MONOTONIC should match WasiClockId.MONOTONIC")
    void monotonicConstantShouldMatch() {
      assertEquals(
          WasiClockId.MONOTONIC.getValue(), AbstractWasiTimeOperations.WASI_CLOCK_MONOTONIC);
    }

    @Test
    @DisplayName("WASI_CLOCK_PROCESS_CPUTIME_ID should match WasiClockId.PROCESS_CPUTIME_ID")
    void processCpuTimeConstantShouldMatch() {
      assertEquals(
          WasiClockId.PROCESS_CPUTIME_ID.getValue(),
          AbstractWasiTimeOperations.WASI_CLOCK_PROCESS_CPUTIME_ID);
    }

    @Test
    @DisplayName("WASI_CLOCK_THREAD_CPUTIME_ID should match WasiClockId.THREAD_CPUTIME_ID")
    void threadCpuTimeConstantShouldMatch() {
      assertEquals(
          WasiClockId.THREAD_CPUTIME_ID.getValue(),
          AbstractWasiTimeOperations.WASI_CLOCK_THREAD_CPUTIME_ID);
    }
  }

  @Nested
  @DisplayName("isClockSupported")
  class IsClockSupported {

    @Test
    @DisplayName("should support realtime clock")
    void shouldSupportRealtimeClock() {
      assertTrue(
          AbstractWasiTimeOperations.isClockSupported(
              AbstractWasiTimeOperations.WASI_CLOCK_REALTIME));
    }

    @Test
    @DisplayName("should support monotonic clock")
    void shouldSupportMonotonicClock() {
      assertTrue(
          AbstractWasiTimeOperations.isClockSupported(
              AbstractWasiTimeOperations.WASI_CLOCK_MONOTONIC));
    }

    @Test
    @DisplayName("should not support invalid clock ID")
    void shouldNotSupportInvalidClockId() {
      assertFalse(AbstractWasiTimeOperations.isClockSupported(999));
    }

    @Test
    @DisplayName("should not support negative clock ID")
    void shouldNotSupportNegativeClockId() {
      assertFalse(AbstractWasiTimeOperations.isClockSupported(-1));
    }
  }

  @Nested
  @DisplayName("getClockName")
  class GetClockName {

    @Test
    @DisplayName("should return name for realtime clock")
    void shouldReturnNameForRealtimeClock() {
      String name =
          AbstractWasiTimeOperations.getClockName(AbstractWasiTimeOperations.WASI_CLOCK_REALTIME);
      assertNotNull(name);
      assertFalse(name.isEmpty());
    }

    @Test
    @DisplayName("should return name for monotonic clock")
    void shouldReturnNameForMonotonicClock() {
      String name =
          AbstractWasiTimeOperations.getClockName(AbstractWasiTimeOperations.WASI_CLOCK_MONOTONIC);
      assertNotNull(name);
      assertFalse(name.isEmpty());
    }
  }

  @Nested
  @DisplayName("convertTime")
  class ConvertTime {

    @Test
    @DisplayName("should convert nanoseconds to seconds")
    void shouldConvertNanosecondsToSeconds() {
      long nanos = 5_000_000_000L;
      assertEquals(5L, AbstractWasiTimeOperations.convertTime(nanos, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("should convert nanoseconds to milliseconds")
    void shouldConvertNanosecondsToMilliseconds() {
      long nanos = 5_000_000L;
      assertEquals(5L, AbstractWasiTimeOperations.convertTime(nanos, TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("should convert nanoseconds to microseconds")
    void shouldConvertNanosecondsToMicroseconds() {
      long nanos = 5_000L;
      assertEquals(5L, AbstractWasiTimeOperations.convertTime(nanos, TimeUnit.MICROSECONDS));
    }

    @Test
    @DisplayName("should return same value for nanoseconds")
    void shouldReturnSameValueForNanoseconds() {
      long nanos = 12345L;
      assertEquals(12345L, AbstractWasiTimeOperations.convertTime(nanos, TimeUnit.NANOSECONDS));
    }
  }
}
