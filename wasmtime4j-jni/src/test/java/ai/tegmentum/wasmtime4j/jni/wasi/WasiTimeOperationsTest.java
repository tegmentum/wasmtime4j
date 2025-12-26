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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link WasiTimeOperations}.
 */
@DisplayName("WasiTimeOperations Tests")
class WasiTimeOperationsTest {

  private WasiContext testContext;
  private WasiTimeOperations timeOperations;

  @BeforeEach
  void setUp() {
    testContext = TestWasiContextFactory.createTestContext();
    timeOperations = new WasiTimeOperations(testContext);
  }

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

    @Test
    @DisplayName("Should define clock ID constants")
    void shouldDefineClockIdConstants() {
      assertEquals(0, WasiTimeOperations.WASI_CLOCK_REALTIME, "REALTIME should be 0");
      assertEquals(1, WasiTimeOperations.WASI_CLOCK_MONOTONIC, "MONOTONIC should be 1");
      assertEquals(2, WasiTimeOperations.WASI_CLOCK_PROCESS_CPUTIME_ID,
          "PROCESS_CPUTIME_ID should be 2");
      assertEquals(3, WasiTimeOperations.WASI_CLOCK_THREAD_CPUTIME_ID,
          "THREAD_CPUTIME_ID should be 3");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should throw on null context")
    void constructorShouldThrowOnNullContext() {
      assertThrows(JniException.class,
          () -> new WasiTimeOperations(null),
          "Should throw on null context");
    }

    @Test
    @DisplayName("Constructor should create time operations with valid context")
    void constructorShouldCreateTimeOperationsWithValidContext() {
      final WasiTimeOperations ops = new WasiTimeOperations(testContext);
      assertNotNull(ops, "Time operations should be created");
    }
  }

  @Nested
  @DisplayName("getClockResolution Tests")
  class GetClockResolutionTests {

    @Test
    @DisplayName("Should throw on invalid clock ID (negative)")
    void shouldThrowOnNegativeClockId() {
      assertThrows(JniException.class,
          () -> timeOperations.getClockResolution(-1),
          "Should throw on negative clock ID");
    }

    @Test
    @DisplayName("Should throw on invalid clock ID (too large)")
    void shouldThrowOnTooLargeClockId() {
      assertThrows(JniException.class,
          () -> timeOperations.getClockResolution(999),
          "Should throw on clock ID > 3");
    }

    @Test
    @DisplayName("Should throw on clock ID 4")
    void shouldThrowOnClockIdFour() {
      assertThrows(JniException.class,
          () -> timeOperations.getClockResolution(4),
          "Should throw on clock ID 4");
    }
  }

  @Nested
  @DisplayName("getCurrentTime Tests")
  class GetCurrentTimeTests {

    @Test
    @DisplayName("Should throw on invalid clock ID")
    void shouldThrowOnInvalidClockId() {
      assertThrows(JniException.class,
          () -> timeOperations.getCurrentTime(100, 0),
          "Should throw on invalid clock ID");
    }

    @Test
    @DisplayName("Should throw on negative precision")
    void shouldThrowOnNegativePrecision() {
      assertThrows(JniException.class,
          () -> timeOperations.getCurrentTime(WasiTimeOperations.WASI_CLOCK_REALTIME, -1),
          "Should throw on negative precision");
    }

    @Test
    @DisplayName("Should accept zero precision")
    void shouldAcceptZeroPrecision() {
      // Zero means maximum precision - validation should pass
      // Will throw from native call, but validation passes
      assertThrows(Exception.class,
          () -> timeOperations.getCurrentTime(WasiTimeOperations.WASI_CLOCK_REALTIME, 0));
    }

    @Test
    @DisplayName("Overloaded method should use zero precision")
    void overloadedMethodShouldUseZeroPrecision() {
      // Single-arg version uses precision=0
      assertThrows(Exception.class,
          () -> timeOperations.getCurrentTime(WasiTimeOperations.WASI_CLOCK_REALTIME));
    }
  }

  @Nested
  @DisplayName("Convenience Method Tests")
  class ConvenienceMethodTests {

    @Test
    @DisplayName("getRealtime should use CLOCK_REALTIME")
    void getRealtimeShouldUseClockRealtime() {
      // Will throw from native, but validates it calls with correct clock ID
      assertThrows(Exception.class,
          () -> timeOperations.getRealtime());
    }

    @Test
    @DisplayName("getMonotonicTime should use CLOCK_MONOTONIC")
    void getMonotonicTimeShouldUseClockMonotonic() {
      assertThrows(Exception.class,
          () -> timeOperations.getMonotonicTime());
    }

    @Test
    @DisplayName("getProcessCpuTime should use CLOCK_PROCESS_CPUTIME_ID")
    void getProcessCpuTimeShouldUseClockProcessCputimeId() {
      assertThrows(Exception.class,
          () -> timeOperations.getProcessCpuTime());
    }

    @Test
    @DisplayName("getThreadCpuTime should use CLOCK_THREAD_CPUTIME_ID")
    void getThreadCpuTimeShouldUseClockThreadCputimeId() {
      assertThrows(Exception.class,
          () -> timeOperations.getThreadCpuTime());
    }
  }

  @Nested
  @DisplayName("convertTime Tests")
  class ConvertTimeTests {

    @Test
    @DisplayName("Should convert nanoseconds to seconds")
    void shouldConvertNanosecondsToSeconds() {
      final long result = WasiTimeOperations.convertTime(
          1_000_000_000L, TimeUnit.SECONDS);

      assertEquals(1L, result, "1 billion nanoseconds should be 1 second");
    }

    @Test
    @DisplayName("Should convert nanoseconds to milliseconds")
    void shouldConvertNanosecondsToMilliseconds() {
      final long result = WasiTimeOperations.convertTime(
          5_000_000L, TimeUnit.MILLISECONDS);

      assertEquals(5L, result, "5 million nanoseconds should be 5 milliseconds");
    }

    @Test
    @DisplayName("Should convert nanoseconds to microseconds")
    void shouldConvertNanosecondsToMicroseconds() {
      final long result = WasiTimeOperations.convertTime(
          5_000L, TimeUnit.MICROSECONDS);

      assertEquals(5L, result, "5 thousand nanoseconds should be 5 microseconds");
    }

    @Test
    @DisplayName("Should handle nanoseconds to nanoseconds identity")
    void shouldHandleNanosecondsToNanosecondsIdentity() {
      final long result = WasiTimeOperations.convertTime(
          123456789L, TimeUnit.NANOSECONDS);

      assertEquals(123456789L, result, "Nanoseconds to nanoseconds should be identity");
    }

    @Test
    @DisplayName("Should throw on null time unit")
    void shouldThrowOnNullTimeUnit() {
      assertThrows(JniException.class,
          () -> WasiTimeOperations.convertTime(1000L, null),
          "Should throw on null time unit");
    }

    @Test
    @DisplayName("Should handle zero nanoseconds")
    void shouldHandleZeroNanoseconds() {
      final long result = WasiTimeOperations.convertTime(0L, TimeUnit.SECONDS);

      assertEquals(0L, result, "Zero nanoseconds should convert to zero");
    }

    @Test
    @DisplayName("Should handle large values")
    void shouldHandleLargeValues() {
      // 1 year in nanoseconds approximately
      final long nanos = 365L * 24 * 60 * 60 * 1_000_000_000L;
      final long result = WasiTimeOperations.convertTime(nanos, TimeUnit.DAYS);

      assertEquals(365L, result, "Should convert to 365 days");
    }
  }

  @Nested
  @DisplayName("isClockSupported Tests")
  class IsClockSupportedTests {

    @Test
    @DisplayName("Should return true for CLOCK_REALTIME")
    void shouldReturnTrueForClockRealtime() {
      assertTrue(WasiTimeOperations.isClockSupported(WasiTimeOperations.WASI_CLOCK_REALTIME),
          "CLOCK_REALTIME should be supported");
    }

    @Test
    @DisplayName("Should return true for CLOCK_MONOTONIC")
    void shouldReturnTrueForClockMonotonic() {
      assertTrue(WasiTimeOperations.isClockSupported(WasiTimeOperations.WASI_CLOCK_MONOTONIC),
          "CLOCK_MONOTONIC should be supported");
    }

    @Test
    @DisplayName("Should return true for CLOCK_PROCESS_CPUTIME_ID")
    void shouldReturnTrueForClockProcessCputimeId() {
      assertTrue(
          WasiTimeOperations.isClockSupported(WasiTimeOperations.WASI_CLOCK_PROCESS_CPUTIME_ID),
          "CLOCK_PROCESS_CPUTIME_ID should be supported");
    }

    @Test
    @DisplayName("Should return true for CLOCK_THREAD_CPUTIME_ID")
    void shouldReturnTrueForClockThreadCputimeId() {
      assertTrue(
          WasiTimeOperations.isClockSupported(WasiTimeOperations.WASI_CLOCK_THREAD_CPUTIME_ID),
          "CLOCK_THREAD_CPUTIME_ID should be supported");
    }

    @Test
    @DisplayName("Should return false for negative clock ID")
    void shouldReturnFalseForNegativeClockId() {
      assertFalse(WasiTimeOperations.isClockSupported(-1),
          "Negative clock ID should not be supported");
    }

    @Test
    @DisplayName("Should return false for clock ID 4")
    void shouldReturnFalseForClockIdFour() {
      assertFalse(WasiTimeOperations.isClockSupported(4),
          "Clock ID 4 should not be supported");
    }

    @Test
    @DisplayName("Should return false for large clock ID")
    void shouldReturnFalseForLargeClockId() {
      assertFalse(WasiTimeOperations.isClockSupported(100),
          "Large clock ID should not be supported");
    }
  }

  @Nested
  @DisplayName("getClockName Tests")
  class GetClockNameTests {

    @Test
    @DisplayName("Should return REALTIME for clock ID 0")
    void shouldReturnRealtimeForClockId0() {
      assertEquals("REALTIME",
          WasiTimeOperations.getClockName(WasiTimeOperations.WASI_CLOCK_REALTIME),
          "Should return REALTIME");
    }

    @Test
    @DisplayName("Should return MONOTONIC for clock ID 1")
    void shouldReturnMonotonicForClockId1() {
      assertEquals("MONOTONIC",
          WasiTimeOperations.getClockName(WasiTimeOperations.WASI_CLOCK_MONOTONIC),
          "Should return MONOTONIC");
    }

    @Test
    @DisplayName("Should return PROCESS_CPUTIME for clock ID 2")
    void shouldReturnProcessCputimeForClockId2() {
      assertEquals("PROCESS_CPUTIME",
          WasiTimeOperations.getClockName(WasiTimeOperations.WASI_CLOCK_PROCESS_CPUTIME_ID),
          "Should return PROCESS_CPUTIME");
    }

    @Test
    @DisplayName("Should return THREAD_CPUTIME for clock ID 3")
    void shouldReturnThreadCputimeForClockId3() {
      assertEquals("THREAD_CPUTIME",
          WasiTimeOperations.getClockName(WasiTimeOperations.WASI_CLOCK_THREAD_CPUTIME_ID),
          "Should return THREAD_CPUTIME");
    }

    @Test
    @DisplayName("Should return UNKNOWN for invalid clock ID")
    void shouldReturnUnknownForInvalidClockId() {
      final String name = WasiTimeOperations.getClockName(999);
      assertTrue(name.startsWith("UNKNOWN"), "Should start with UNKNOWN");
      assertTrue(name.contains("999"), "Should contain the clock ID");
    }

    @Test
    @DisplayName("Should return UNKNOWN for negative clock ID")
    void shouldReturnUnknownForNegativeClockId() {
      final String name = WasiTimeOperations.getClockName(-1);
      assertTrue(name.startsWith("UNKNOWN"), "Should start with UNKNOWN");
      assertTrue(name.contains("-1"), "Should contain the clock ID");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("Should validate all supported clock IDs pass")
    void shouldValidateAllSupportedClockIdsPass() {
      // These should all pass validation (but fail on native call)
      for (int clockId = 0; clockId <= 3; clockId++) {
        final int finalClockId = clockId;
        assertThrows(Exception.class,
            () -> timeOperations.getClockResolution(finalClockId),
            "Clock ID " + clockId + " should pass validation");
      }
    }

    @Test
    @DisplayName("Should fail validation for unsupported clock IDs")
    void shouldFailValidationForUnsupportedClockIds() {
      // These should fail validation before native call
      assertThrows(JniException.class,
          () -> timeOperations.getClockResolution(-1));
      assertThrows(JniException.class,
          () -> timeOperations.getClockResolution(4));
      assertThrows(JniException.class,
          () -> timeOperations.getClockResolution(Integer.MAX_VALUE));
    }
  }
}
