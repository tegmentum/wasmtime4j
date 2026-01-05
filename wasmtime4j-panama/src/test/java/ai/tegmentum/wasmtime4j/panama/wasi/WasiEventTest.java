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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiEvent} class.
 *
 * <p>WasiEvent represents an event returned by poll_oneoff operations, containing user data, error
 * code, event type, and number of bytes available for I/O.
 */
@DisplayName("WasiEvent Tests")
class WasiEventTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(Modifier.isPublic(WasiEvent.class.getModifiers()), "WasiEvent should be public");
      assertTrue(Modifier.isFinal(WasiEvent.class.getModifiers()), "WasiEvent should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with all parameters")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiEvent.class.getConstructor(
              long.class, // userData
              int.class, // error
              int.class, // type
              int.class // nbytes
              );
      assertNotNull(constructor, "Constructor with all parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("User Data Method Tests")
  class UserDataMethodTests {

    @Test
    @DisplayName("should have getUserData method")
    void shouldHaveGetUserDataMethod() throws NoSuchMethodException {
      final Method method = WasiEvent.class.getMethod("getUserData");
      assertNotNull(method, "getUserData method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Error Method Tests")
  class ErrorMethodTests {

    @Test
    @DisplayName("should have getError method")
    void shouldHaveGetErrorMethod() throws NoSuchMethodException {
      final Method method = WasiEvent.class.getMethod("getError");
      assertNotNull(method, "getError method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have hasError method")
    void shouldHaveHasErrorMethod() throws NoSuchMethodException {
      final Method method = WasiEvent.class.getMethod("hasError");
      assertNotNull(method, "hasError method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Type Method Tests")
  class TypeMethodTests {

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = WasiEvent.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Nbytes Method Tests")
  class NbytesMethodTests {

    @Test
    @DisplayName("should have getNbytes method")
    void shouldHaveGetNbytesMethod() throws NoSuchMethodException {
      final Method method = WasiEvent.class.getMethod("getNbytes");
      assertNotNull(method, "getNbytes method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("ToString Method Tests")
  class ToStringMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = WasiEvent.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
