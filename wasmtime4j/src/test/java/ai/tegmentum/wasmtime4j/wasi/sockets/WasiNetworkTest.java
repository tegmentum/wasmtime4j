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

package ai.tegmentum.wasmtime4j.wasi.sockets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiNetwork} interface.
 *
 * <p>This test class verifies the interface structure and method signatures for the WASI network
 * API using reflection-based testing.
 */
@DisplayName("WasiNetwork Interface Tests")
class WasiNetworkTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WasiNetwork should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiNetwork.class.isInterface(), "WasiNetwork should be an interface");
    }

    @Test
    @DisplayName("WasiNetwork should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiNetwork.class.getModifiers()), "WasiNetwork should be public");
    }

    @Test
    @DisplayName("WasiNetwork should not extend any interface")
    void shouldNotExtendAnyInterface() {
      Class<?>[] interfaces = WasiNetwork.class.getInterfaces();
      assertEquals(0, interfaces.length, "WasiNetwork should not extend any interface");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiNetwork.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "close should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "close should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("WasiNetwork should have exactly 1 declared method")
    void shouldHaveExactMethodCount() {
      Method[] methods = WasiNetwork.class.getDeclaredMethods();
      assertEquals(1, methods.length, "WasiNetwork should have exactly 1 method");
    }
  }

  // ========================================================================
  // Method Signature Verification Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Signature Verification Tests")
  class MethodSignatureVerificationTests {

    @Test
    @DisplayName("close method should be public and abstract")
    void closeMethodShouldBePublicAbstract() throws NoSuchMethodException {
      Method method = WasiNetwork.class.getMethod("close");
      int modifiers = method.getModifiers();
      assertTrue(
          Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
          "close method should be public and abstract");
    }
  }
}
