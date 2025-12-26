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

package ai.tegmentum.wasmtime4j.wasi.clocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiMonotonicClock} interface.
 *
 * <p>WasiMonotonicClock provides monotonic time measurement for elapsed time, performance timing,
 * and timeouts, as specified in WASI Preview 2 wasi:clocks/monotonic-clock@0.2.8.
 */
@DisplayName("WasiMonotonicClock Tests")
class WasiMonotonicClockTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiMonotonicClock.class.isInterface(), "WasiMonotonicClock should be an interface");
    }

    @Test
    @DisplayName("should have now method")
    void shouldHaveNowMethod() throws NoSuchMethodException {
      final Method method = WasiMonotonicClock.class.getMethod("now");
      assertEquals(long.class, method.getReturnType(), "Should return long (nanoseconds)");
    }

    @Test
    @DisplayName("should have resolution method")
    void shouldHaveResolutionMethod() throws NoSuchMethodException {
      final Method method = WasiMonotonicClock.class.getMethod("resolution");
      assertEquals(long.class, method.getReturnType(), "Should return long (nanoseconds)");
    }

    @Test
    @DisplayName("should have subscribeInstant method")
    void shouldHaveSubscribeInstantMethod() throws NoSuchMethodException {
      final Method method = WasiMonotonicClock.class.getMethod("subscribeInstant", long.class);
      assertEquals(WasiPollable.class, method.getReturnType(), "Should return WasiPollable");
    }

    @Test
    @DisplayName("should have subscribeDuration method")
    void shouldHaveSubscribeDurationMethod() throws NoSuchMethodException {
      final Method method = WasiMonotonicClock.class.getMethod("subscribeDuration", long.class);
      assertEquals(WasiPollable.class, method.getReturnType(), "Should return WasiPollable");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("now should take no parameters")
    void nowShouldTakeNoParameters() throws NoSuchMethodException {
      final Method now = WasiMonotonicClock.class.getMethod("now");
      assertEquals(0, now.getParameterCount(), "now should have no parameters");
    }

    @Test
    @DisplayName("resolution should take no parameters")
    void resolutionShouldTakeNoParameters() throws NoSuchMethodException {
      final Method resolution = WasiMonotonicClock.class.getMethod("resolution");
      assertEquals(0, resolution.getParameterCount(), "resolution should have no parameters");
    }

    @Test
    @DisplayName("subscribeInstant should take long parameter")
    void subscribeInstantShouldTakeLongParameter() throws NoSuchMethodException {
      final Method subscribeInstant =
          WasiMonotonicClock.class.getMethod("subscribeInstant", long.class);
      assertEquals(1, subscribeInstant.getParameterCount(), "Should have 1 parameter");
      assertEquals(long.class, subscribeInstant.getParameterTypes()[0], "Parameter should be long");
    }

    @Test
    @DisplayName("subscribeDuration should take long parameter")
    void subscribeDurationShouldTakeLongParameter() throws NoSuchMethodException {
      final Method subscribeDuration =
          WasiMonotonicClock.class.getMethod("subscribeDuration", long.class);
      assertEquals(1, subscribeDuration.getParameterCount(), "Should have 1 parameter");
      assertEquals(
          long.class, subscribeDuration.getParameterTypes()[0], "Parameter should be long");
    }
  }

  @Nested
  @DisplayName("WASI Specification Compliance Tests")
  class WasiSpecificationComplianceTests {

    @Test
    @DisplayName("should have all WASI Preview 2 monotonic-clock methods")
    void shouldHaveAllWasiPreview2MonotonicClockMethods() {
      // WASI Preview 2 wasi:clocks/monotonic-clock@0.2.8 specifies:
      // now() -> instant
      // resolution() -> duration
      // subscribe-instant(when: instant) -> pollable
      // subscribe-duration(when: duration) -> pollable
      final String[] expectedMethods = {
        "now", "resolution", "subscribeInstant", "subscribeDuration"
      };

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(WasiMonotonicClock.class, methodName),
            "Should have WASI method: " + methodName);
      }
    }

    @Test
    @DisplayName("should have correct method count")
    void shouldHaveCorrectMethodCount() {
      final long methodCount =
          Arrays.stream(WasiMonotonicClock.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(4, methodCount, "Should have exactly 4 methods");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support measuring elapsed time")
    void shouldSupportMeasuringElapsedTime() {
      // Pattern: long start = clock.now(); ... long elapsed = clock.now() - start;
      assertTrue(hasMethod(WasiMonotonicClock.class, "now"), "Need now method");
    }

    @Test
    @DisplayName("should support checking clock precision")
    void shouldSupportCheckingClockPrecision() {
      // Pattern: long precision = clock.resolution();
      assertTrue(hasMethod(WasiMonotonicClock.class, "resolution"), "Need resolution method");
    }

    @Test
    @DisplayName("should support timeout implementation")
    void shouldSupportTimeoutImplementation() {
      // Pattern: WasiPollable pollable = clock.subscribeDuration(timeoutNanos);
      assertTrue(
          hasMethod(WasiMonotonicClock.class, "subscribeDuration"), "Need subscribeDuration");
    }

    @Test
    @DisplayName("should support scheduled task implementation")
    void shouldSupportScheduledTaskImplementation() {
      // Pattern: WasiPollable pollable = clock.subscribeInstant(when);
      assertTrue(hasMethod(WasiMonotonicClock.class, "subscribeInstant"), "Need subscribeInstant");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Monotonic Clock Property Tests")
  class MonotonicClockPropertyTests {

    @Test
    @DisplayName("now should return nanoseconds")
    void nowShouldReturnNanoseconds() throws NoSuchMethodException {
      final Method now = WasiMonotonicClock.class.getMethod("now");
      assertEquals(long.class, now.getReturnType(), "Should return long for nanoseconds");
      // Nanoseconds require long to hold large values (up to billions of years)
    }

    @Test
    @DisplayName("resolution should return nanoseconds")
    void resolutionShouldReturnNanoseconds() throws NoSuchMethodException {
      final Method resolution = WasiMonotonicClock.class.getMethod("resolution");
      assertEquals(
          long.class, resolution.getReturnType(), "Should return long for resolution in ns");
    }
  }

  @Nested
  @DisplayName("Pollable Integration Tests")
  class PollableIntegrationTests {

    @Test
    @DisplayName("subscribeInstant should return WasiPollable")
    void subscribeInstantShouldReturnWasiPollable() throws NoSuchMethodException {
      final Method subscribeInstant =
          WasiMonotonicClock.class.getMethod("subscribeInstant", long.class);
      assertEquals(
          WasiPollable.class,
          subscribeInstant.getReturnType(),
          "Should return WasiPollable for async waiting");
    }

    @Test
    @DisplayName("subscribeDuration should return WasiPollable")
    void subscribeDurationShouldReturnWasiPollable() throws NoSuchMethodException {
      final Method subscribeDuration =
          WasiMonotonicClock.class.getMethod("subscribeDuration", long.class);
      assertEquals(
          WasiPollable.class,
          subscribeDuration.getReturnType(),
          "Should return WasiPollable for duration-based waiting");
    }
  }

  @Nested
  @DisplayName("Difference from Wall Clock Tests")
  class DifferenceFromWallClockTests {

    @Test
    @DisplayName("should be designed for elapsed time measurement")
    void shouldBeDesignedForElapsedTimeMeasurement() {
      // Monotonic clock is for measuring elapsed time, not wall time
      // now() returns nanoseconds relative to unspecified start
      assertTrue(
          hasMethod(WasiMonotonicClock.class, "now"), "now() is for elapsed time, not date/time");
    }

    @Test
    @DisplayName("should not have methods for date/time retrieval")
    void shouldNotHaveMethodsForDateTimeRetrieval() {
      // WasiMonotonicClock should NOT have wall clock methods
      final String[] wallClockMethods = {"getDateTime", "getDate", "getTime"};

      for (final String methodName : wallClockMethods) {
        assertTrue(
            !hasMethod(WasiMonotonicClock.class, methodName),
            "Monotonic clock should not have: " + methodName);
      }
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }
}
