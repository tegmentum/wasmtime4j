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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the nested types in {@link WasiTcpSocket}: {@link WasiTcpSocket.ConnectionStreams},
 * {@link WasiTcpSocket.AcceptResult}, and {@link WasiTcpSocket.ShutdownType}.
 *
 * <p>These are simple data containers and enums. The tests verify construction, null validation,
 * and getter behavior. Interface stubs are created via {@link Proxy} to avoid implementing every
 * abstract method on large WASI interfaces.
 */
@DisplayName("WasiTcpSocket Nested Types Tests")
class WasiTcpSocketTypesTest {

  /** Creates a no-op proxy for the given interface. All methods return default values or throw. */
  @SuppressWarnings("unchecked")
  private static <T> T stubOf(final Class<T> iface) {
    return (T)
        Proxy.newProxyInstance(
            iface.getClassLoader(),
            new Class<?>[] {iface},
            (proxy, method, args) -> {
              final Class<?> returnType = method.getReturnType();
              if (returnType == void.class) {
                return null;
              }
              if (returnType.isPrimitive()) {
                if (returnType == boolean.class) {
                  return false;
                }
                if (returnType == int.class) {
                  return 0;
                }
                if (returnType == long.class) {
                  return 0L;
                }
                if (returnType == byte.class) {
                  return (byte) 0;
                }
              }
              return null;
            });
  }

  private static final WasiInputStream STUB_INPUT = stubOf(WasiInputStream.class);
  private static final WasiOutputStream STUB_OUTPUT = stubOf(WasiOutputStream.class);
  private static final WasiTcpSocket STUB_SOCKET = stubOf(WasiTcpSocket.class);

  @Nested
  @DisplayName("ConnectionStreams")
  class ConnectionStreamsTests {

    @Test
    @DisplayName("should store and return input and output streams")
    void shouldStoreAndReturnStreams() {
      final WasiTcpSocket.ConnectionStreams streams =
          new WasiTcpSocket.ConnectionStreams(STUB_INPUT, STUB_OUTPUT);

      assertSame(
          STUB_INPUT, streams.getInputStream(), "getInputStream should return the input stream");
      assertSame(
          STUB_OUTPUT,
          streams.getOutputStream(),
          "getOutputStream should return the output stream");
    }

    @Test
    @DisplayName("should reject null inputStream")
    void shouldRejectNullInputStream() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> new WasiTcpSocket.ConnectionStreams(null, STUB_OUTPUT));
      assertNotNull(ex.getMessage(), "Exception should have a message");
      assertEquals(
          "inputStream cannot be null",
          ex.getMessage(),
          "Error message should identify the null parameter");
    }

    @Test
    @DisplayName("should reject null outputStream")
    void shouldRejectNullOutputStream() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> new WasiTcpSocket.ConnectionStreams(STUB_INPUT, null));
      assertEquals(
          "outputStream cannot be null",
          ex.getMessage(),
          "Error message should identify the null parameter");
    }
  }

  @Nested
  @DisplayName("AcceptResult")
  class AcceptResultTests {

    @Test
    @DisplayName("should store and return socket, input, and output streams")
    void shouldStoreAndReturnAll() {
      final WasiTcpSocket.AcceptResult result =
          new WasiTcpSocket.AcceptResult(STUB_SOCKET, STUB_INPUT, STUB_OUTPUT);

      assertSame(STUB_SOCKET, result.getSocket(), "getSocket should return the socket");
      assertSame(
          STUB_INPUT, result.getInputStream(), "getInputStream should return the input stream");
      assertSame(
          STUB_OUTPUT, result.getOutputStream(), "getOutputStream should return the output stream");
    }

    @Test
    @DisplayName("should reject null socket")
    void shouldRejectNullSocket() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> new WasiTcpSocket.AcceptResult(null, STUB_INPUT, STUB_OUTPUT));
      assertEquals(
          "socket cannot be null",
          ex.getMessage(),
          "Error message should identify the null parameter");
    }

    @Test
    @DisplayName("should reject null inputStream")
    void shouldRejectNullInputStream() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> new WasiTcpSocket.AcceptResult(STUB_SOCKET, null, STUB_OUTPUT));
      assertEquals(
          "inputStream cannot be null",
          ex.getMessage(),
          "Error message should identify the null parameter");
    }

    @Test
    @DisplayName("should reject null outputStream")
    void shouldRejectNullOutputStream() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> new WasiTcpSocket.AcceptResult(STUB_SOCKET, STUB_INPUT, null));
      assertEquals(
          "outputStream cannot be null",
          ex.getMessage(),
          "Error message should identify the null parameter");
    }
  }

  @Nested
  @DisplayName("ShutdownType")
  class ShutdownTypeTests {

    @Test
    @DisplayName("should have exactly three values: RECEIVE, SEND, BOTH")
    void shouldHaveThreeValues() {
      final WasiTcpSocket.ShutdownType[] values = WasiTcpSocket.ShutdownType.values();
      assertEquals(3, values.length, "ShutdownType should have exactly 3 values");
      assertArrayEquals(
          new WasiTcpSocket.ShutdownType[] {
            WasiTcpSocket.ShutdownType.RECEIVE,
            WasiTcpSocket.ShutdownType.SEND,
            WasiTcpSocket.ShutdownType.BOTH
          },
          values,
          "ShutdownType values should be RECEIVE, SEND, BOTH in order");
    }

    @Test
    @DisplayName("valueOf should return correct enum constants")
    void valueOfShouldWork() {
      assertEquals(
          WasiTcpSocket.ShutdownType.RECEIVE, WasiTcpSocket.ShutdownType.valueOf("RECEIVE"));
      assertEquals(WasiTcpSocket.ShutdownType.SEND, WasiTcpSocket.ShutdownType.valueOf("SEND"));
      assertEquals(WasiTcpSocket.ShutdownType.BOTH, WasiTcpSocket.ShutdownType.valueOf("BOTH"));
    }

    @Test
    @DisplayName("valueOf should throw for unknown value")
    void valueOfShouldThrowForUnknown() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiTcpSocket.ShutdownType.valueOf("UNKNOWN"));
    }
  }
}
