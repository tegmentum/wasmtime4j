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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiWallClock} interface.
 *
 * <p>WasiWallClock provides real-world time as seconds and nanoseconds since the Unix epoch. Unlike
 * the monotonic clock, this clock is not guaranteed to be monotonic and may be reset.
 */
@DisplayName("WasiWallClock Tests")
class WasiWallClockTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiWallClock.class.isInterface(), "WasiWallClock should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiWallClock.class.getModifiers()), "WasiWallClock should be public");
    }

    @Test
    @DisplayName("should have now method")
    void shouldHaveNowMethod() throws NoSuchMethodException {
      final Method method = WasiWallClock.class.getMethod("now");
      assertNotNull(method, "now method should exist");
      assertEquals(DateTime.class, method.getReturnType(), "now should return DateTime");
    }

    @Test
    @DisplayName("should have resolution method")
    void shouldHaveResolutionMethod() throws NoSuchMethodException {
      final Method method = WasiWallClock.class.getMethod("resolution");
      assertNotNull(method, "resolution method should exist");
      assertEquals(DateTime.class, method.getReturnType(), "resolution should return DateTime");
    }

    @Test
    @DisplayName("should have exactly two methods")
    void shouldHaveExactlyTwoMethods() {
      int methodCount = 0;
      for (final Method method : WasiWallClock.class.getDeclaredMethods()) {
        if (!method.isSynthetic()) {
          methodCount++;
        }
      }
      assertEquals(2, methodCount, "WasiWallClock should have exactly 2 methods");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("now should take no parameters")
    void nowShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WasiWallClock.class.getMethod("now");
      assertEquals(0, method.getParameterCount(), "now should take no parameters");
    }

    @Test
    @DisplayName("resolution should take no parameters")
    void resolutionShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WasiWallClock.class.getMethod("resolution");
      assertEquals(0, method.getParameterCount(), "resolution should take no parameters");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock should return current time")
    void mockShouldReturnCurrentTime() {
      final MockWasiWallClock clock = new MockWasiWallClock();
      clock.setTime(1704067200, 500_000_000); // 2024-01-01 00:00:00.5 UTC

      final DateTime now = clock.now();

      assertNotNull(now, "now should return non-null DateTime");
      assertEquals(1704067200, now.getSeconds(), "Seconds should match");
      assertEquals(500_000_000, now.getNanoseconds(), "Nanoseconds should match");
    }

    @Test
    @DisplayName("mock should return resolution")
    void mockShouldReturnResolution() {
      final MockWasiWallClock clock = new MockWasiWallClock();

      final DateTime resolution = clock.resolution();

      assertNotNull(resolution, "resolution should return non-null DateTime");
      assertTrue(resolution.getSeconds() >= 0, "Resolution seconds should be non-negative");
    }

    @Test
    @DisplayName("mock should allow time adjustments (non-monotonic)")
    void mockShouldAllowTimeAdjustments() {
      final MockWasiWallClock clock = new MockWasiWallClock();

      clock.setTime(1000, 0);
      final DateTime first = clock.now();

      clock.setTime(500, 0); // Set time backwards
      final DateTime second = clock.now();

      assertTrue(
          second.getSeconds() < first.getSeconds(),
          "Wall clock should allow non-monotonic time (adjustments)");
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      for (final Method method : WasiWallClock.class.getDeclaredMethods()) {
        if (!method.isSynthetic()) {
          assertTrue(
              Modifier.isPublic(method.getModifiers()),
              "Method " + method.getName() + " should be public");
        }
      }
    }
  }

  /** Mock implementation of WasiWallClock for testing. */
  private static class MockWasiWallClock implements WasiWallClock {
    private long seconds = 0;
    private int nanoseconds = 0;

    @Override
    public DateTime now() {
      return new DateTime(seconds, nanoseconds);
    }

    @Override
    public DateTime resolution() {
      return new DateTime(0, 1); // 1 nanosecond resolution
    }

    public void setTime(final long seconds, final int nanoseconds) {
      this.seconds = seconds;
      this.nanoseconds = nanoseconds;
    }
  }
}
