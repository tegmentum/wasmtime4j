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
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiUdpSocket;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiUdpSocket} class.
 *
 * <p>PanamaWasiUdpSocket is a Panama FFI implementation of the WasiUdpSocket interface providing
 * access to WASI Preview 2 UDP socket operations for connectionless datagram communication.
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
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have create static factory method")
    void shouldHaveCreateMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiUdpSocket.class.getMethod("create", MemorySegment.class, IpAddressFamily.class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "create should be static");
      assertEquals(
          PanamaWasiUdpSocket.class, method.getReturnType(), "Should return PanamaWasiUdpSocket");
    }
  }

  @Nested
  @DisplayName("Bind Method Tests")
  class BindMethodTests {

    @Test
    @DisplayName("should have startBind method")
    void shouldHaveStartBindMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiUdpSocket.class.getMethod(
              "startBind", WasiNetwork.class, IpSocketAddress.class);
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
  @DisplayName("Stream Method Tests")
  class StreamMethodTests {

    @Test
    @DisplayName("should have stream method")
    void shouldHaveStreamMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiUdpSocket.class.getMethod("stream", WasiNetwork.class, IpSocketAddress.class);
      assertNotNull(method, "stream method should exist");
      // Returns tuple of (IncomingDatagramStream, OutgoingDatagramStream) in WASI spec
    }
  }

  @Nested
  @DisplayName("Address Method Tests")
  class AddressMethodTests {

    @Test
    @DisplayName("should have localAddress method")
    void shouldHaveLocalAddressMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("localAddress");
      assertNotNull(method, "localAddress method should exist");
      assertEquals(IpSocketAddress.class, method.getReturnType(), "Should return IpSocketAddress");
    }

    @Test
    @DisplayName("should have remoteAddress method")
    void shouldHaveRemoteAddressMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("remoteAddress");
      assertNotNull(method, "remoteAddress method should exist");
      assertEquals(IpSocketAddress.class, method.getReturnType(), "Should return IpSocketAddress");
    }

    @Test
    @DisplayName("should have addressFamily method")
    void shouldHaveAddressFamilyMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("addressFamily");
      assertNotNull(method, "addressFamily method should exist");
      assertEquals(IpAddressFamily.class, method.getReturnType(), "Should return IpAddressFamily");
    }
  }

  @Nested
  @DisplayName("Configuration Method Tests")
  class ConfigurationMethodTests {

    @Test
    @DisplayName("should have unicastHopLimit method")
    void shouldHaveUnicastHopLimitMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("unicastHopLimit");
      assertNotNull(method, "unicastHopLimit method should exist");
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
    @DisplayName("should have receiveBufferSize method")
    void shouldHaveReceiveBufferSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("receiveBufferSize");
      assertNotNull(method, "receiveBufferSize method should exist");
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
    @DisplayName("should have sendBufferSize method")
    void shouldHaveSendBufferSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("sendBufferSize");
      assertNotNull(method, "sendBufferSize method should exist");
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
  @DisplayName("Data Transfer Method Tests")
  class DataTransferMethodTests {

    @Test
    @DisplayName("should have receive method")
    void shouldHaveReceiveMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("receive", long.class);
      assertNotNull(method, "receive method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have send method")
    void shouldHaveSendMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiUdpSocket.class.getMethod("send", WasiUdpSocket.OutgoingDatagram[].class);
      assertNotNull(method, "send method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Operation Method Tests")
  class OperationMethodTests {

    @Test
    @DisplayName("should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(WasiPollable.class, method.getReturnType(), "Should return WasiPollable");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiUdpSocket.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
