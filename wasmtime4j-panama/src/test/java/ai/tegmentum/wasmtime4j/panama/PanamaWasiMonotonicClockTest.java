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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasiMonotonicClock;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiMonotonicClock} class.
 *
 * <p>PanamaWasiMonotonicClock provides monotonic clock functionality using Panama FFI.
 */
@DisplayName("PanamaWasiMonotonicClock Tests")
class PanamaWasiMonotonicClockTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiMonotonicClock.class.getModifiers()),
          "PanamaWasiMonotonicClock should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiMonotonicClock.class.getModifiers()),
          "PanamaWasiMonotonicClock should be final");
    }

    @Test
    @DisplayName("should implement WasiMonotonicClock interface")
    void shouldImplementWasiMonotonicClockInterface() {
      assertTrue(
          WasiMonotonicClock.class.isAssignableFrom(PanamaWasiMonotonicClock.class),
          "PanamaWasiMonotonicClock should implement WasiMonotonicClock");
    }
  }

  @Nested
  @DisplayName("Clock Method Tests")
  class ClockMethodTests {

    @Test
    @DisplayName("should have now method")
    void shouldHaveNowMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiMonotonicClock.class.getMethod("now");
      assertNotNull(method, "now method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have resolution method")
    void shouldHaveResolutionMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiMonotonicClock.class.getMethod("resolution");
      assertNotNull(method, "resolution method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have subscribeInstant method")
    void shouldHaveSubscribeInstantMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiMonotonicClock.class.getMethod("subscribeInstant", long.class);
      assertNotNull(method, "subscribeInstant method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have subscribeDuration method")
    void shouldHaveSubscribeDurationMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiMonotonicClock.class.getMethod("subscribeDuration", long.class);
      assertNotNull(method, "subscribeDuration method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public default constructor")
    void shouldHavePublicDefaultConstructor() throws NoSuchMethodException {
      var constructor = PanamaWasiMonotonicClock.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }
}
