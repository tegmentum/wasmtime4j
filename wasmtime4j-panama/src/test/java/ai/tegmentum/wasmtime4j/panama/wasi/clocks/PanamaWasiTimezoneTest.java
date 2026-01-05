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

package ai.tegmentum.wasmtime4j.panama.wasi.clocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.clocks.DateTime;
import ai.tegmentum.wasmtime4j.wasi.clocks.TimezoneDisplay;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiTimezone;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiTimezone} class.
 *
 * <p>PanamaWasiTimezone is a Panama FFI implementation of the WasiTimezone interface providing
 * access to WASI Preview 2 timezone operations including UTC offsets, timezone names, and daylight
 * saving time status.
 */
@DisplayName("PanamaWasiTimezone Tests")
class PanamaWasiTimezoneTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiTimezone.class.getModifiers()),
          "PanamaWasiTimezone should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiTimezone.class.getModifiers()),
          "PanamaWasiTimezone should be final");
    }

    @Test
    @DisplayName("should implement WasiTimezone interface")
    void shouldImplementWasiTimezoneInterface() {
      assertTrue(
          WasiTimezone.class.isAssignableFrom(PanamaWasiTimezone.class),
          "PanamaWasiTimezone should implement WasiTimezone");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with context handle")
    void shouldHaveConstructorWithContextHandle() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaWasiTimezone.class.getConstructor(MemorySegment.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Display Method Tests")
  class DisplayMethodTests {

    @Test
    @DisplayName("should have display method")
    void shouldHaveDisplayMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTimezone.class.getMethod("display", DateTime.class);
      assertNotNull(method, "display method should exist");
      assertEquals(TimezoneDisplay.class, method.getReturnType(), "Should return TimezoneDisplay");
    }
  }

  @Nested
  @DisplayName("UTC Offset Method Tests")
  class UtcOffsetMethodTests {

    @Test
    @DisplayName("should have utcOffset method")
    void shouldHaveUtcOffsetMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTimezone.class.getMethod("utcOffset", DateTime.class);
      assertNotNull(method, "utcOffset method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }
}
