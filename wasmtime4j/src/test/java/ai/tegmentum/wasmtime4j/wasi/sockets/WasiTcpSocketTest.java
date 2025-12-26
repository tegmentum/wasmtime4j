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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiTcpSocket} interface.
 *
 * <p>This test class verifies the interface structure and method signatures for the WASI TCP socket
 * API using reflection-based testing.
 */
@DisplayName("WasiTcpSocket Interface Tests")
class WasiTcpSocketTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WasiTcpSocket should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiTcpSocket.class.isInterface(), "WasiTcpSocket should be an interface");
    }

    @Test
    @DisplayName("WasiTcpSocket should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiTcpSocket.class.getModifiers()), "WasiTcpSocket should be public");
    }

    @Test
    @DisplayName("WasiTcpSocket should not extend any interface")
    void shouldNotExtendAnyInterface() {
      Class<?>[] interfaces = WasiTcpSocket.class.getInterfaces();
      assertEquals(0, interfaces.length, "WasiTcpSocket should not extend any interface");
    }
  }

  // ========================================================================
  // Bind Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Bind Method Tests")
  class BindMethodTests {

    @Test
    @DisplayName("should have startBind method with WasiNetwork and IpSocketAddress parameters")
    void shouldHaveStartBindMethod() throws NoSuchMethodException {
      Method method =
          WasiTcpSocket.class.getMethod("startBind", WasiNetwork.class, IpSocketAddress.class);
      assertNotNull(method, "startBind method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(2, method.getParameterCount(), "startBind should have 2 parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "startBind should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have finishBind method")
    void shouldHaveFinishBindMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("finishBind");
      assertNotNull(method, "finishBind method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "finishBind should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "finishBind should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Connect Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Connect Method Tests")
  class ConnectMethodTests {

    @Test
    @DisplayName("should have startConnect method with WasiNetwork and IpSocketAddress parameters")
    void shouldHaveStartConnectMethod() throws NoSuchMethodException {
      Method method =
          WasiTcpSocket.class.getMethod("startConnect", WasiNetwork.class, IpSocketAddress.class);
      assertNotNull(method, "startConnect method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(2, method.getParameterCount(), "startConnect should have 2 parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "startConnect should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have finishConnect method returning ConnectionStreams")
    void shouldHaveFinishConnectMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("finishConnect");
      assertNotNull(method, "finishConnect method should exist");
      assertEquals(
          WasiTcpSocket.ConnectionStreams.class,
          method.getReturnType(),
          "Return type should be ConnectionStreams");
      assertEquals(0, method.getParameterCount(), "finishConnect should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "finishConnect should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Listen Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Listen Method Tests")
  class ListenMethodTests {

    @Test
    @DisplayName("should have startListen method")
    void shouldHaveStartListenMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("startListen");
      assertNotNull(method, "startListen method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "startListen should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "startListen should throw 1 exception");
    }

    @Test
    @DisplayName("should have finishListen method")
    void shouldHaveFinishListenMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("finishListen");
      assertNotNull(method, "finishListen method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "finishListen should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "finishListen should throw 1 exception");
    }

    @Test
    @DisplayName("should have accept method returning AcceptResult")
    void shouldHaveAcceptMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("accept");
      assertNotNull(method, "accept method should exist");
      assertEquals(
          WasiTcpSocket.AcceptResult.class,
          method.getReturnType(),
          "Return type should be AcceptResult");
      assertEquals(0, method.getParameterCount(), "accept should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "accept should throw 1 exception");
    }
  }

  // ========================================================================
  // Address Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Address Method Tests")
  class AddressMethodTests {

    @Test
    @DisplayName("should have localAddress method returning IpSocketAddress")
    void shouldHaveLocalAddressMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("localAddress");
      assertNotNull(method, "localAddress method should exist");
      assertEquals(
          IpSocketAddress.class, method.getReturnType(), "Return type should be IpSocketAddress");
      assertEquals(0, method.getParameterCount(), "localAddress should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "localAddress should throw 1 exception");
    }

    @Test
    @DisplayName("should have remoteAddress method returning IpSocketAddress")
    void shouldHaveRemoteAddressMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("remoteAddress");
      assertNotNull(method, "remoteAddress method should exist");
      assertEquals(
          IpSocketAddress.class, method.getReturnType(), "Return type should be IpSocketAddress");
      assertEquals(0, method.getParameterCount(), "remoteAddress should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "remoteAddress should throw 1 exception");
    }

    @Test
    @DisplayName("should have addressFamily method returning IpAddressFamily")
    void shouldHaveAddressFamilyMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("addressFamily");
      assertNotNull(method, "addressFamily method should exist");
      assertEquals(
          IpAddressFamily.class, method.getReturnType(), "Return type should be IpAddressFamily");
      assertEquals(0, method.getParameterCount(), "addressFamily should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "addressFamily should throw 1 exception");
    }
  }

  // ========================================================================
  // Socket Options Tests
  // ========================================================================

  @Nested
  @DisplayName("Socket Options Tests")
  class SocketOptionsTests {

    @Test
    @DisplayName("should have setListenBacklogSize method")
    void shouldHaveSetListenBacklogSizeMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("setListenBacklogSize", long.class);
      assertNotNull(method, "setListenBacklogSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "setListenBacklogSize should have 1 parameter");
      assertEquals(
          long.class, method.getParameterTypes()[0], "Parameter should be long for backlog size");
    }

    @Test
    @DisplayName("should have setKeepAliveEnabled method")
    void shouldHaveSetKeepAliveEnabledMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("setKeepAliveEnabled", boolean.class);
      assertNotNull(method, "setKeepAliveEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "setKeepAliveEnabled should have 1 parameter");
      assertEquals(boolean.class, method.getParameterTypes()[0], "Parameter should be boolean");
    }

    @Test
    @DisplayName("should have setKeepAliveIdleTime method")
    void shouldHaveSetKeepAliveIdleTimeMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("setKeepAliveIdleTime", long.class);
      assertNotNull(method, "setKeepAliveIdleTime method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "setKeepAliveIdleTime should have 1 parameter");
    }

    @Test
    @DisplayName("should have setKeepAliveInterval method")
    void shouldHaveSetKeepAliveIntervalMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("setKeepAliveInterval", long.class);
      assertNotNull(method, "setKeepAliveInterval method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have setKeepAliveCount method")
    void shouldHaveSetKeepAliveCountMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("setKeepAliveCount", int.class);
      assertNotNull(method, "setKeepAliveCount method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(int.class, method.getParameterTypes()[0], "Parameter should be int");
    }

    @Test
    @DisplayName("should have setHopLimit method")
    void shouldHaveSetHopLimitMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("setHopLimit", int.class);
      assertNotNull(method, "setHopLimit method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(int.class, method.getParameterTypes()[0], "Parameter should be int");
    }
  }

  // ========================================================================
  // Buffer Size Tests
  // ========================================================================

  @Nested
  @DisplayName("Buffer Size Tests")
  class BufferSizeTests {

    @Test
    @DisplayName("should have receiveBufferSize method returning long")
    void shouldHaveReceiveBufferSizeMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("receiveBufferSize");
      assertNotNull(method, "receiveBufferSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(0, method.getParameterCount(), "receiveBufferSize should have no parameters");
    }

    @Test
    @DisplayName("should have setReceiveBufferSize method")
    void shouldHaveSetReceiveBufferSizeMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("setReceiveBufferSize", long.class);
      assertNotNull(method, "setReceiveBufferSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "setReceiveBufferSize should have 1 parameter");
    }

    @Test
    @DisplayName("should have sendBufferSize method returning long")
    void shouldHaveSendBufferSizeMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("sendBufferSize");
      assertNotNull(method, "sendBufferSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setSendBufferSize method")
    void shouldHaveSetSendBufferSizeMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("setSendBufferSize", long.class);
      assertNotNull(method, "setSendBufferSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // Lifecycle Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have subscribe method returning WasiPollable")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(
          WasiPollable.class, method.getReturnType(), "Return type should be WasiPollable");
      assertEquals(0, method.getParameterCount(), "subscribe should have no parameters");
    }

    @Test
    @DisplayName("should have shutdown method with ShutdownType parameter")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("shutdown", WasiTcpSocket.ShutdownType.class);
      assertNotNull(method, "shutdown method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "shutdown should have 1 parameter");
      assertEquals(
          WasiTcpSocket.ShutdownType.class,
          method.getParameterTypes()[0],
          "Parameter should be ShutdownType");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "close should have no parameters");
    }
  }

  // ========================================================================
  // ConnectionStreams Nested Class Tests
  // ========================================================================

  @Nested
  @DisplayName("ConnectionStreams Nested Class Tests")
  class ConnectionStreamsTests {

    @Test
    @DisplayName("ConnectionStreams should be a nested final class")
    void shouldBeNestedFinalClass() {
      Class<?> connectionStreamsClass = WasiTcpSocket.ConnectionStreams.class;
      assertTrue(
          Modifier.isFinal(connectionStreamsClass.getModifiers()),
          "ConnectionStreams should be final");
      assertTrue(
          Modifier.isPublic(connectionStreamsClass.getModifiers()),
          "ConnectionStreams should be public");
      assertTrue(
          Modifier.isStatic(connectionStreamsClass.getModifiers()),
          "ConnectionStreams should be static (accessible without parent instance)");
    }

    @Test
    @DisplayName("ConnectionStreams should have constructor with InputStream and OutputStream")
    void shouldHaveConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          WasiTcpSocket.ConnectionStreams.class.getConstructor(
              WasiInputStream.class, WasiOutputStream.class);
      assertNotNull(constructor, "Constructor should exist");
      assertEquals(2, constructor.getParameterCount(), "Constructor should have 2 parameters");
    }

    @Test
    @DisplayName("ConnectionStreams should have getInputStream method")
    void shouldHaveGetInputStreamMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.ConnectionStreams.class.getMethod("getInputStream");
      assertNotNull(method, "getInputStream method should exist");
      assertEquals(
          WasiInputStream.class, method.getReturnType(), "Return type should be WasiInputStream");
    }

    @Test
    @DisplayName("ConnectionStreams should have getOutputStream method")
    void shouldHaveGetOutputStreamMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.ConnectionStreams.class.getMethod("getOutputStream");
      assertNotNull(method, "getOutputStream method should exist");
      assertEquals(
          WasiOutputStream.class, method.getReturnType(), "Return type should be WasiOutputStream");
    }

    @Test
    @DisplayName("ConnectionStreams constructor should reject null inputStream")
    void constructorShouldRejectNullInputStream() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiTcpSocket.ConnectionStreams(null, createMockOutputStream()),
          "Should throw IllegalArgumentException for null inputStream");
    }

    @Test
    @DisplayName("ConnectionStreams constructor should reject null outputStream")
    void constructorShouldRejectNullOutputStream() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiTcpSocket.ConnectionStreams(createMockInputStream(), null),
          "Should throw IllegalArgumentException for null outputStream");
    }
  }

  // ========================================================================
  // AcceptResult Nested Class Tests
  // ========================================================================

  @Nested
  @DisplayName("AcceptResult Nested Class Tests")
  class AcceptResultTests {

    @Test
    @DisplayName("AcceptResult should be a nested final class")
    void shouldBeNestedFinalClass() {
      Class<?> acceptResultClass = WasiTcpSocket.AcceptResult.class;
      assertTrue(
          Modifier.isFinal(acceptResultClass.getModifiers()), "AcceptResult should be final");
      assertTrue(
          Modifier.isPublic(acceptResultClass.getModifiers()), "AcceptResult should be public");
    }

    @Test
    @DisplayName("AcceptResult should have constructor with socket and streams")
    void shouldHaveConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          WasiTcpSocket.AcceptResult.class.getConstructor(
              WasiTcpSocket.class, WasiInputStream.class, WasiOutputStream.class);
      assertNotNull(constructor, "Constructor should exist");
      assertEquals(3, constructor.getParameterCount(), "Constructor should have 3 parameters");
    }

    @Test
    @DisplayName("AcceptResult should have getSocket method")
    void shouldHaveGetSocketMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.AcceptResult.class.getMethod("getSocket");
      assertNotNull(method, "getSocket method should exist");
      assertEquals(
          WasiTcpSocket.class, method.getReturnType(), "Return type should be WasiTcpSocket");
    }

    @Test
    @DisplayName("AcceptResult should have getInputStream method")
    void shouldHaveGetInputStreamMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.AcceptResult.class.getMethod("getInputStream");
      assertNotNull(method, "getInputStream method should exist");
      assertEquals(
          WasiInputStream.class, method.getReturnType(), "Return type should be WasiInputStream");
    }

    @Test
    @DisplayName("AcceptResult should have getOutputStream method")
    void shouldHaveGetOutputStreamMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.AcceptResult.class.getMethod("getOutputStream");
      assertNotNull(method, "getOutputStream method should exist");
      assertEquals(
          WasiOutputStream.class, method.getReturnType(), "Return type should be WasiOutputStream");
    }

    @Test
    @DisplayName("AcceptResult constructor should reject null socket")
    void constructorShouldRejectNullSocket() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new WasiTcpSocket.AcceptResult(
                  null, createMockInputStream(), createMockOutputStream()),
          "Should throw IllegalArgumentException for null socket");
    }

    @Test
    @DisplayName("AcceptResult constructor should reject null inputStream")
    void constructorShouldRejectNullInputStream() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new WasiTcpSocket.AcceptResult(createMockTcpSocket(), null, createMockOutputStream()),
          "Should throw IllegalArgumentException for null inputStream");
    }

    @Test
    @DisplayName("AcceptResult constructor should reject null outputStream")
    void constructorShouldRejectNullOutputStream() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new WasiTcpSocket.AcceptResult(createMockTcpSocket(), createMockInputStream(), null),
          "Should throw IllegalArgumentException for null outputStream");
    }
  }

  // ========================================================================
  // ShutdownType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ShutdownType Enum Tests")
  class ShutdownTypeTests {

    @Test
    @DisplayName("ShutdownType should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiTcpSocket.ShutdownType.class.isEnum(), "ShutdownType should be an enum");
    }

    @Test
    @DisplayName("ShutdownType should have RECEIVE constant")
    void shouldHaveReceiveConstant() {
      WasiTcpSocket.ShutdownType receive = WasiTcpSocket.ShutdownType.RECEIVE;
      assertNotNull(receive, "RECEIVE constant should exist");
      assertEquals("RECEIVE", receive.name(), "Name should be RECEIVE");
    }

    @Test
    @DisplayName("ShutdownType should have SEND constant")
    void shouldHaveSendConstant() {
      WasiTcpSocket.ShutdownType send = WasiTcpSocket.ShutdownType.SEND;
      assertNotNull(send, "SEND constant should exist");
      assertEquals("SEND", send.name(), "Name should be SEND");
    }

    @Test
    @DisplayName("ShutdownType should have BOTH constant")
    void shouldHaveBothConstant() {
      WasiTcpSocket.ShutdownType both = WasiTcpSocket.ShutdownType.BOTH;
      assertNotNull(both, "BOTH constant should exist");
      assertEquals("BOTH", both.name(), "Name should be BOTH");
    }

    @Test
    @DisplayName("ShutdownType should have exactly 3 values")
    void shouldHaveExactlyThreeValues() {
      WasiTcpSocket.ShutdownType[] values = WasiTcpSocket.ShutdownType.values();
      assertEquals(3, values.length, "ShutdownType should have exactly 3 values");
    }

    @Test
    @DisplayName("ShutdownType values should be in expected order")
    void valuesShouldBeInExpectedOrder() {
      WasiTcpSocket.ShutdownType[] values = WasiTcpSocket.ShutdownType.values();
      assertArrayEquals(
          new WasiTcpSocket.ShutdownType[] {
            WasiTcpSocket.ShutdownType.RECEIVE,
            WasiTcpSocket.ShutdownType.SEND,
            WasiTcpSocket.ShutdownType.BOTH
          },
          values,
          "Values should be in order: RECEIVE, SEND, BOTH");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("WasiTcpSocket should have exactly 23 declared methods")
    void shouldHaveExactMethodCount() {
      Method[] methods = WasiTcpSocket.class.getDeclaredMethods();
      assertEquals(23, methods.length, "WasiTcpSocket should have exactly 23 methods");
    }
  }

  // ========================================================================
  // Helper Methods for Creating Minimal Implementations
  // ========================================================================

  private WasiInputStream createMockInputStream() {
    return new WasiInputStream() {
      @Override
      public byte[] read(final long length) {
        return new byte[0];
      }

      @Override
      public byte[] blockingRead(final long length) {
        return new byte[0];
      }

      @Override
      public long skip(final long length) {
        return 0;
      }

      @Override
      public long blockingSkip(final long length) {
        return 0;
      }

      @Override
      public WasiPollable subscribe() {
        return null;
      }

      @Override
      public long getId() {
        return 0;
      }

      @Override
      public String getType() {
        return "input-stream";
      }

      @Override
      public ai.tegmentum.wasmtime4j.wasi.WasiInstance getOwner() {
        return null;
      }

      @Override
      public boolean isOwned() {
        return true;
      }

      @Override
      public boolean isValid() {
        return true;
      }

      @Override
      public java.time.Instant getCreatedAt() {
        return java.time.Instant.now();
      }

      @Override
      public java.util.Optional<java.time.Instant> getLastAccessedAt() {
        return java.util.Optional.empty();
      }

      @Override
      public ai.tegmentum.wasmtime4j.wasi.WasiResourceMetadata getMetadata() {
        return null;
      }

      @Override
      public ai.tegmentum.wasmtime4j.wasi.WasiResourceState getState() {
        return null;
      }

      @Override
      public ai.tegmentum.wasmtime4j.wasi.WasiResourceStats getStats() {
        return null;
      }

      @Override
      public Object invoke(final String operation, final Object... parameters) {
        return null;
      }

      @Override
      public java.util.List<String> getAvailableOperations() {
        return java.util.Collections.emptyList();
      }

      @Override
      public ai.tegmentum.wasmtime4j.wasi.WasiResourceHandle createHandle() {
        return null;
      }

      @Override
      public void transferOwnership(
          final ai.tegmentum.wasmtime4j.wasi.WasiInstance targetInstance) {}

      @Override
      public void close() {}
    };
  }

  private WasiOutputStream createMockOutputStream() {
    return new WasiOutputStream() {
      @Override
      public long checkWrite() {
        return 0;
      }

      @Override
      public void write(final byte[] contents) {}

      @Override
      public void blockingWriteAndFlush(final byte[] contents) {}

      @Override
      public void flush() {}

      @Override
      public void blockingFlush() {}

      @Override
      public void writeZeroes(final long length) {}

      @Override
      public void blockingWriteZeroesAndFlush(final long length) {}

      @Override
      public long splice(final WasiInputStream source, final long length) {
        return 0;
      }

      @Override
      public long blockingSplice(final WasiInputStream source, final long length) {
        return 0;
      }

      @Override
      public WasiPollable subscribe() {
        return null;
      }

      @Override
      public long getId() {
        return 0;
      }

      @Override
      public String getType() {
        return "output-stream";
      }

      @Override
      public ai.tegmentum.wasmtime4j.wasi.WasiInstance getOwner() {
        return null;
      }

      @Override
      public boolean isOwned() {
        return true;
      }

      @Override
      public boolean isValid() {
        return true;
      }

      @Override
      public java.time.Instant getCreatedAt() {
        return java.time.Instant.now();
      }

      @Override
      public java.util.Optional<java.time.Instant> getLastAccessedAt() {
        return java.util.Optional.empty();
      }

      @Override
      public ai.tegmentum.wasmtime4j.wasi.WasiResourceMetadata getMetadata() {
        return null;
      }

      @Override
      public ai.tegmentum.wasmtime4j.wasi.WasiResourceState getState() {
        return null;
      }

      @Override
      public ai.tegmentum.wasmtime4j.wasi.WasiResourceStats getStats() {
        return null;
      }

      @Override
      public Object invoke(final String operation, final Object... parameters) {
        return null;
      }

      @Override
      public java.util.List<String> getAvailableOperations() {
        return java.util.Collections.emptyList();
      }

      @Override
      public ai.tegmentum.wasmtime4j.wasi.WasiResourceHandle createHandle() {
        return null;
      }

      @Override
      public void transferOwnership(
          final ai.tegmentum.wasmtime4j.wasi.WasiInstance targetInstance) {}

      @Override
      public void close() {}
    };
  }

  private WasiTcpSocket createMockTcpSocket() {
    return new WasiTcpSocket() {
      @Override
      public void startBind(WasiNetwork network, IpSocketAddress localAddress) {}

      @Override
      public void finishBind() {}

      @Override
      public void startConnect(WasiNetwork network, IpSocketAddress remoteAddress) {}

      @Override
      public ConnectionStreams finishConnect() {
        return null;
      }

      @Override
      public void startListen() {}

      @Override
      public void finishListen() {}

      @Override
      public AcceptResult accept() {
        return null;
      }

      @Override
      public IpSocketAddress localAddress() {
        return null;
      }

      @Override
      public IpSocketAddress remoteAddress() {
        return null;
      }

      @Override
      public IpAddressFamily addressFamily() {
        return null;
      }

      @Override
      public void setListenBacklogSize(long value) {}

      @Override
      public void setKeepAliveEnabled(boolean value) {}

      @Override
      public void setKeepAliveIdleTime(long value) {}

      @Override
      public void setKeepAliveInterval(long value) {}

      @Override
      public void setKeepAliveCount(int value) {}

      @Override
      public void setHopLimit(int value) {}

      @Override
      public long receiveBufferSize() {
        return 0;
      }

      @Override
      public void setReceiveBufferSize(long value) {}

      @Override
      public long sendBufferSize() {
        return 0;
      }

      @Override
      public void setSendBufferSize(long value) {}

      @Override
      public WasiPollable subscribe() {
        return null;
      }

      @Override
      public void shutdown(ShutdownType shutdownType) {}

      @Override
      public void close() {}
    };
  }
}
