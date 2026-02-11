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

import ai.tegmentum.wasmtime4j.wasi.WasiAddressFamily;
import ai.tegmentum.wasmtime4j.wasi.WasiSocketAddress;
import ai.tegmentum.wasmtime4j.wasi.WasiUdpSocket;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiUdpSocket} class.
 *
 * <p>PanamaWasiUdpSocket provides UDP socket functionality using Panama FFI.
 */
@DisplayName("PanamaWasiUdpSocket Tests")
class PanamaWasiUdpSocketTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiUdpSocket.class.getModifiers()),
          "PanamaWasiUdpSocket should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiUdpSocket.class.getModifiers()),
          "PanamaWasiUdpSocket should be final");
    }

    @Test
    @DisplayName("should implement WasiUdpSocket interface")
    void shouldImplementWasiUdpSocketInterface() {
      assertTrue(
          WasiUdpSocket.class.isAssignableFrom(PanamaWasiUdpSocket.class),
          "PanamaWasiUdpSocket should implement WasiUdpSocket");
    }

    @Test
    @DisplayName("should implement AutoCloseable interface")
    void shouldImplementAutoCloseableInterface() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaWasiUdpSocket.class),
          "PanamaWasiUdpSocket should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Bind Method Tests")
  class BindMethodTests {

    @Test
    @DisplayName("should have startBind method")
    void shouldHaveStartBindMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiUdpSocket.class.getMethod("startBind", WasiSocketAddress.class);
      assertNotNull(method, "startBind method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have finishBind method")
    void shouldHaveFinishBindMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("finishBind");
      assertNotNull(method, "finishBind method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Connect Method Tests")
  class ConnectMethodTests {

    @Test
    @DisplayName("should have startConnect method")
    void shouldHaveStartConnectMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiUdpSocket.class.getMethod("startConnect", WasiSocketAddress.class);
      assertNotNull(method, "startConnect method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have finishConnect method")
    void shouldHaveFinishConnectMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("finishConnect");
      assertNotNull(method, "finishConnect method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Address Method Tests")
  class AddressMethodTests {

    @Test
    @DisplayName("should have getLocalAddress method")
    void shouldHaveGetLocalAddressMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("getLocalAddress");
      assertNotNull(method, "getLocalAddress method should exist");
      assertEquals(
          WasiSocketAddress.class, method.getReturnType(), "Should return WasiSocketAddress");
    }

    @Test
    @DisplayName("should have getRemoteAddress method")
    void shouldHaveGetRemoteAddressMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("getRemoteAddress");
      assertNotNull(method, "getRemoteAddress method should exist");
      assertEquals(
          WasiSocketAddress.class, method.getReturnType(), "Should return WasiSocketAddress");
    }

    @Test
    @DisplayName("should have getAddressFamily method")
    void shouldHaveGetAddressFamilyMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("getAddressFamily");
      assertNotNull(method, "getAddressFamily method should exist");
      assertEquals(
          WasiAddressFamily.class, method.getReturnType(), "Should return WasiAddressFamily");
    }
  }

  @Nested
  @DisplayName("Option Method Tests")
  class OptionMethodTests {

    @Test
    @DisplayName("should have getUnicastHopLimit method")
    void shouldHaveGetUnicastHopLimitMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("getUnicastHopLimit");
      assertNotNull(method, "getUnicastHopLimit method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have setUnicastHopLimit method")
    void shouldHaveSetUnicastHopLimitMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("setUnicastHopLimit", int.class);
      assertNotNull(method, "setUnicastHopLimit method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Buffer Size Method Tests")
  class BufferSizeMethodTests {

    @Test
    @DisplayName("should have getReceiveBufferSize method")
    void shouldHaveGetReceiveBufferSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("getReceiveBufferSize");
      assertNotNull(method, "getReceiveBufferSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have setReceiveBufferSize method")
    void shouldHaveSetReceiveBufferSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("setReceiveBufferSize", long.class);
      assertNotNull(method, "setReceiveBufferSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getSendBufferSize method")
    void shouldHaveGetSendBufferSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("getSendBufferSize");
      assertNotNull(method, "getSendBufferSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have setSendBufferSize method")
    void shouldHaveSetSendBufferSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("setSendBufferSize", long.class);
      assertNotNull(method, "setSendBufferSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with WasiAddressFamily")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      var constructor = PanamaWasiUdpSocket.class.getConstructor(WasiAddressFamily.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }
}
