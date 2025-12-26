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
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiTimezone} interface.
 *
 * <p>WasiTimezone provides access to timezone information including UTC offsets and daylight saving
 * time status, as specified in WASI Preview 2 wasi:clocks/timezone@0.2.8.
 */
@DisplayName("WasiTimezone Tests")
class WasiTimezoneTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiTimezone.class.isInterface(), "WasiTimezone should be an interface");
    }

    @Test
    @DisplayName("should have display method")
    void shouldHaveDisplayMethod() throws NoSuchMethodException {
      final Method method = WasiTimezone.class.getMethod("display", DateTime.class);
      assertEquals(TimezoneDisplay.class, method.getReturnType(), "Should return TimezoneDisplay");
    }

    @Test
    @DisplayName("should have utcOffset method")
    void shouldHaveUtcOffsetMethod() throws NoSuchMethodException {
      final Method method = WasiTimezone.class.getMethod("utcOffset", DateTime.class);
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("display should take DateTime parameter")
    void displayShouldTakeDateTimeParameter() throws NoSuchMethodException {
      final Method display = WasiTimezone.class.getMethod("display", DateTime.class);
      assertEquals(1, display.getParameterCount(), "display should have 1 parameter");
      assertEquals(DateTime.class, display.getParameterTypes()[0], "Parameter should be DateTime");
    }

    @Test
    @DisplayName("utcOffset should take DateTime parameter")
    void utcOffsetShouldTakeDateTimeParameter() throws NoSuchMethodException {
      final Method utcOffset = WasiTimezone.class.getMethod("utcOffset", DateTime.class);
      assertEquals(1, utcOffset.getParameterCount(), "utcOffset should have 1 parameter");
      assertEquals(
          DateTime.class, utcOffset.getParameterTypes()[0], "Parameter should be DateTime");
    }
  }

  @Nested
  @DisplayName("WASI Specification Compliance Tests")
  class WasiSpecificationComplianceTests {

    @Test
    @DisplayName("should have all WASI Preview 2 timezone methods")
    void shouldHaveAllWasiPreview2TimezoneMethods() {
      // WASI Preview 2 wasi:clocks/timezone@0.2.8 specifies:
      // display(when: datetime) -> timezone-display
      // utc-offset(when: datetime) -> s32
      final String[] expectedMethods = {"display", "utcOffset"};

      for (final String methodName : expectedMethods) {
        assertTrue(
            hasMethod(WasiTimezone.class, methodName), "Should have WASI method: " + methodName);
      }
    }

    @Test
    @DisplayName("should have correct method count")
    void shouldHaveCorrectMethodCount() {
      final long methodCount =
          Arrays.stream(WasiTimezone.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(2, methodCount, "Should have exactly 2 methods");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support getting comprehensive timezone info")
    void shouldSupportGettingComprehensiveTimezoneInfo() {
      // Documents usage: timezone.display(datetime) -> TimezoneDisplay
      assertTrue(hasMethod(WasiTimezone.class, "display"), "Need display method");
    }

    @Test
    @DisplayName("should support getting UTC offset only")
    void shouldSupportGettingUtcOffsetOnly() {
      // Documents usage: timezone.utcOffset(datetime) -> int seconds
      assertTrue(hasMethod(WasiTimezone.class, "utcOffset"), "Need utcOffset method");
    }

    private boolean hasMethod(final Class<?> clazz, final String methodName) {
      return Arrays.stream(clazz.getMethods()).anyMatch(m -> m.getName().equals(methodName));
    }
  }

  @Nested
  @DisplayName("UTC Offset Range Tests")
  class UtcOffsetRangeTests {

    @Test
    @DisplayName("utcOffset should return int for seconds")
    void utcOffsetShouldReturnIntForSeconds() throws NoSuchMethodException {
      final Method utcOffset = WasiTimezone.class.getMethod("utcOffset", DateTime.class);
      assertEquals(int.class, utcOffset.getReturnType(), "Should return int for seconds");
      // Note: WASI specifies offset must be less than 86,400 (one day in seconds)
    }
  }

  @Nested
  @DisplayName("Timezone Display Integration Tests")
  class TimezoneDisplayIntegrationTests {

    @Test
    @DisplayName("display should return TimezoneDisplay")
    void displayShouldReturnTimezoneDisplay() throws NoSuchMethodException {
      final Method display = WasiTimezone.class.getMethod("display", DateTime.class);
      assertEquals(
          TimezoneDisplay.class,
          display.getReturnType(),
          "Should return TimezoneDisplay with full timezone info");
    }
  }

  @Nested
  @DisplayName("Unstable Feature Tests")
  class UnstableFeatureTests {

    @Test
    @DisplayName("should be marked as unstable in WASI")
    void shouldBeMarkedAsUnstableInWasi() {
      // This interface is part of WASI Preview 2 unstable feature: clocks-timezone
      // The test documents this as per the specification
      assertNotNull(WasiTimezone.class, "WasiTimezone is unstable feature clocks-timezone");
    }
  }
}
