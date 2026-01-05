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
 * Tests for {@link WasiSubscription} class.
 *
 * <p>WasiSubscription represents an event subscription for use with poll_oneoff operations,
 * containing user data, event type, file descriptor, and flags.
 */
@DisplayName("WasiSubscription Tests")
class WasiSubscriptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiSubscription.class.getModifiers()),
          "WasiSubscription should be public");
      assertTrue(
          Modifier.isFinal(WasiSubscription.class.getModifiers()),
          "WasiSubscription should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default public constructor")
    void shouldHaveDefaultPublicConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = WasiSubscription.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("User Data Method Tests")
  class UserDataMethodTests {

    @Test
    @DisplayName("should have getUserData method")
    void shouldHaveGetUserDataMethod() throws NoSuchMethodException {
      final Method method = WasiSubscription.class.getMethod("getUserData");
      assertNotNull(method, "getUserData method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have setUserData method")
    void shouldHaveSetUserDataMethod() throws NoSuchMethodException {
      final Method method = WasiSubscription.class.getMethod("setUserData", long.class);
      assertNotNull(method, "setUserData method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Type Method Tests")
  class TypeMethodTests {

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = WasiSubscription.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have setType method")
    void shouldHaveSetTypeMethod() throws NoSuchMethodException {
      final Method method = WasiSubscription.class.getMethod("setType", int.class);
      assertNotNull(method, "setType method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("File Descriptor Method Tests")
  class FileDescriptorMethodTests {

    @Test
    @DisplayName("should have getFd method")
    void shouldHaveGetFdMethod() throws NoSuchMethodException {
      final Method method = WasiSubscription.class.getMethod("getFd");
      assertNotNull(method, "getFd method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have setFd method")
    void shouldHaveSetFdMethod() throws NoSuchMethodException {
      final Method method = WasiSubscription.class.getMethod("setFd", int.class);
      assertNotNull(method, "setFd method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Flags Method Tests")
  class FlagsMethodTests {

    @Test
    @DisplayName("should have getFlags method")
    void shouldHaveGetFlagsMethod() throws NoSuchMethodException {
      final Method method = WasiSubscription.class.getMethod("getFlags");
      assertNotNull(method, "getFlags method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have setFlags method")
    void shouldHaveSetFlagsMethod() throws NoSuchMethodException {
      final Method method = WasiSubscription.class.getMethod("setFlags", int.class);
      assertNotNull(method, "setFlags method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("FD Readwrite Method Tests")
  class FdReadwriteMethodTests {

    @Test
    @DisplayName("should have setFdReadwrite method")
    void shouldHaveSetFdReadwriteMethod() throws NoSuchMethodException {
      final Method method =
          WasiSubscription.class.getMethod("setFdReadwrite", int.class, int.class);
      assertNotNull(method, "setFdReadwrite method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("ToString Method Tests")
  class ToStringMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = WasiSubscription.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
