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

package ai.tegmentum.wasmtime4j.panama.wasi.sockets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddressFamily;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpSocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiNetwork;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiTcpSocket;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiTcpSocket} class.
 *
 * <p>PanamaWasiTcpSocket is a Panama FFI implementation of the WasiTcpSocket interface providing
 * access to WASI Preview 2 TCP socket operations for connection-oriented reliable byte stream
 * communication.
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
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have create static factory method")
    void shouldHaveCreateMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiTcpSocket.class.getMethod("create", MemorySegment.class, IpAddressFamily.class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(
          PanamaWasiTcpSocket.class, method.getReturnType(), "Should return PanamaWasiTcpSocket");
    }
  }

  @Nested
  @DisplayName("Bind Method Tests")
  class BindMethodTests {

    @Test
    @DisplayName("should have startBind method")
    void shouldHaveStartBindMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiTcpSocket.class.getMethod(
              "startBind", WasiNetwork.class, IpSocketAddress.class);
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
          PanamaWasiTcpSocket.class.getMethod(
              "startConnect", WasiNetwork.class, IpSocketAddress.class);
      assertNotNull(method, "startConnect method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have finishConnect method")
    void shouldHaveFinishConnectMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("finishConnect");
      assertNotNull(method, "finishConnect method should exist");
      // Returns tuple of (WasiInputStream, WasiOutputStream) in WASI spec
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
      // Returns tuple of (WasiTcpSocket, WasiInputStream, WasiOutputStream) in WASI spec
    }
  }

  @Nested
  @DisplayName("Address Method Tests")
  class AddressMethodTests {

    @Test
    @DisplayName("should have localAddress method")
    void shouldHaveLocalAddressMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("localAddress");
      assertNotNull(method, "localAddress method should exist");
      assertEquals(IpSocketAddress.class, method.getReturnType(), "Should return IpSocketAddress");
    }

    @Test
    @DisplayName("should have remoteAddress method")
    void shouldHaveRemoteAddressMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("remoteAddress");
      assertNotNull(method, "remoteAddress method should exist");
      assertEquals(IpSocketAddress.class, method.getReturnType(), "Should return IpSocketAddress");
    }

    @Test
    @DisplayName("should have addressFamily method")
    void shouldHaveAddressFamilyMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("addressFamily");
      assertNotNull(method, "addressFamily method should exist");
      assertEquals(IpAddressFamily.class, method.getReturnType(), "Should return IpAddressFamily");
    }
  }

  @Nested
  @DisplayName("Configuration Method Tests")
  class ConfigurationMethodTests {

    @Test
    @DisplayName("should have setListenBacklogSize method")
    void shouldHaveSetListenBacklogSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("setListenBacklogSize", long.class);
      assertNotNull(method, "setListenBacklogSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setKeepAliveEnabled method")
    void shouldHaveSetKeepAliveEnabledMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiTcpSocket.class.getMethod("setKeepAliveEnabled", boolean.class);
      assertNotNull(method, "setKeepAliveEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setKeepAliveIdleTime method")
    void shouldHaveSetKeepAliveIdleTimeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("setKeepAliveIdleTime", long.class);
      assertNotNull(method, "setKeepAliveIdleTime method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setKeepAliveInterval method")
    void shouldHaveSetKeepAliveIntervalMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("setKeepAliveInterval", long.class);
      assertNotNull(method, "setKeepAliveInterval method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setKeepAliveCount method")
    void shouldHaveSetKeepAliveCountMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("setKeepAliveCount", int.class);
      assertNotNull(method, "setKeepAliveCount method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setHopLimit method")
    void shouldHaveSetHopLimitMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("setHopLimit", int.class);
      assertNotNull(method, "setHopLimit method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Buffer Size Method Tests")
  class BufferSizeMethodTests {

    @Test
    @DisplayName("should have receiveBufferSize method")
    void shouldHaveReceiveBufferSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("receiveBufferSize");
      assertNotNull(method, "receiveBufferSize method should exist");
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
    @DisplayName("should have sendBufferSize method")
    void shouldHaveSendBufferSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("sendBufferSize");
      assertNotNull(method, "sendBufferSize method should exist");
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
  @DisplayName("Operation Method Tests")
  class OperationMethodTests {

    @Test
    @DisplayName("should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiTcpSocket.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(WasiPollable.class, method.getReturnType(), "Should return WasiPollable");
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
}
