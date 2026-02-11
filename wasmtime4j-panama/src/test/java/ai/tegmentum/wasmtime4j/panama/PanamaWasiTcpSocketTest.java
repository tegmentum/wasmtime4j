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
import ai.tegmentum.wasmtime4j.wasi.WasiTcpSocket;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiTcpSocket} class.
 *
 * <p>PanamaWasiTcpSocket provides TCP socket functionality using Panama FFI.
 */
@DisplayName("PanamaWasiTcpSocket Tests")
class PanamaWasiTcpSocketTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiTcpSocket.class.getModifiers()),
          "PanamaWasiTcpSocket should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiTcpSocket.class.getModifiers()),
          "PanamaWasiTcpSocket should be final");
    }

    @Test
    @DisplayName("should implement WasiTcpSocket interface")
    void shouldImplementWasiTcpSocketInterface() {
      assertTrue(
          WasiTcpSocket.class.isAssignableFrom(PanamaWasiTcpSocket.class),
          "PanamaWasiTcpSocket should implement WasiTcpSocket");
    }

    @Test
    @DisplayName("should implement AutoCloseable interface")
    void shouldImplementAutoCloseableInterface() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaWasiTcpSocket.class),
          "PanamaWasiTcpSocket should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Bind Method Tests")
  class BindMethodTests {

    @Test
    @DisplayName("should have startBind method")
    void shouldHaveStartBindMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiTcpSocket.class.getMethod("startBind", WasiSocketAddress.class);
      assertNotNull(method, "startBind method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have finishBind method")
    void shouldHaveFinishBindMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("finishBind");
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
          PanamaWasiTcpSocket.class.getMethod("startConnect", WasiSocketAddress.class);
      assertNotNull(method, "startConnect method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have finishConnect method")
    void shouldHaveFinishConnectMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("finishConnect");
      assertNotNull(method, "finishConnect method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Listen Method Tests")
  class ListenMethodTests {

    @Test
    @DisplayName("should have startListen method")
    void shouldHaveStartListenMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("startListen");
      assertNotNull(method, "startListen method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have finishListen method")
    void shouldHaveFinishListenMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("finishListen");
      assertNotNull(method, "finishListen method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have accept method")
    void shouldHaveAcceptMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("accept");
      assertNotNull(method, "accept method should exist");
      assertEquals(WasiTcpSocket.class, method.getReturnType(), "Should return WasiTcpSocket");
    }
  }

  @Nested
  @DisplayName("Address Method Tests")
  class AddressMethodTests {

    @Test
    @DisplayName("should have getLocalAddress method")
    void shouldHaveGetLocalAddressMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("getLocalAddress");
      assertNotNull(method, "getLocalAddress method should exist");
      assertEquals(
          WasiSocketAddress.class, method.getReturnType(), "Should return WasiSocketAddress");
    }

    @Test
    @DisplayName("should have getRemoteAddress method")
    void shouldHaveGetRemoteAddressMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("getRemoteAddress");
      assertNotNull(method, "getRemoteAddress method should exist");
      assertEquals(
          WasiSocketAddress.class, method.getReturnType(), "Should return WasiSocketAddress");
    }

    @Test
    @DisplayName("should have getAddressFamily method")
    void shouldHaveGetAddressFamilyMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("getAddressFamily");
      assertNotNull(method, "getAddressFamily method should exist");
      assertEquals(
          WasiAddressFamily.class, method.getReturnType(), "Should return WasiAddressFamily");
    }
  }

  @Nested
  @DisplayName("Option Method Tests")
  class OptionMethodTests {

    @Test
    @DisplayName("should have setListenBacklogSize method")
    void shouldHaveSetListenBacklogSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("setListenBacklogSize", long.class);
      assertNotNull(method, "setListenBacklogSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have isKeepAliveEnabled method")
    void shouldHaveIsKeepAliveEnabledMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("isKeepAliveEnabled");
      assertNotNull(method, "isKeepAliveEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have setKeepAliveEnabled method")
    void shouldHaveSetKeepAliveEnabledMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiTcpSocket.class.getMethod("setKeepAliveEnabled", boolean.class);
      assertNotNull(method, "setKeepAliveEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Buffer Size Method Tests")
  class BufferSizeMethodTests {

    @Test
    @DisplayName("should have getReceiveBufferSize method")
    void shouldHaveGetReceiveBufferSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("getReceiveBufferSize");
      assertNotNull(method, "getReceiveBufferSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have setReceiveBufferSize method")
    void shouldHaveSetReceiveBufferSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("setReceiveBufferSize", long.class);
      assertNotNull(method, "setReceiveBufferSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getSendBufferSize method")
    void shouldHaveGetSendBufferSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("getSendBufferSize");
      assertNotNull(method, "getSendBufferSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have setSendBufferSize method")
    void shouldHaveSetSendBufferSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("setSendBufferSize", long.class);
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
      final Method method = PanamaWasiTcpSocket.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiTcpSocket.class.getMethod("shutdown", WasiTcpSocket.ShutdownType.class);
      assertNotNull(method, "shutdown method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("close");
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
      var constructor = PanamaWasiTcpSocket.class.getConstructor(WasiAddressFamily.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }
}
