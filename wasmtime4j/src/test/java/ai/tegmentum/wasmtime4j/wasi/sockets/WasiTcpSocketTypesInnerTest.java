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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiTcpSocket} inner classes: ConnectionStreams, AcceptResult, and ShutdownType.
 */
@DisplayName("WasiTcpSocket Inner Types Tests")
class WasiTcpSocketTypesInnerTest {

  /** Minimal stub for WasiInputStream. */
  private static final WasiInputStream STUB_INPUT =
      new WasiInputStream() {
        @Override
        public byte[] read(long length) {
          return new byte[0];
        }

        @Override
        public byte[] blockingRead(long length) {
          return new byte[0];
        }

        @Override
        public long skip(long length) {
          return 0;
        }

        @Override
        public long blockingSkip(long length) {
          return 0;
        }

        @Override
        public WasiPollable subscribe() {
          return null;
        }

        @Override
        public void close() {}
      };

  /** Minimal stub for WasiOutputStream. */
  private static final WasiOutputStream STUB_OUTPUT =
      new WasiOutputStream() {
        @Override
        public long checkWrite() {
          return 0;
        }

        @Override
        public void write(byte[] contents) {}

        @Override
        public void blockingWriteAndFlush(byte[] contents) {}

        @Override
        public void flush() {}

        @Override
        public void blockingFlush() {}

        @Override
        public void writeZeroes(long length) {}

        @Override
        public void blockingWriteZeroesAndFlush(long length) {}

        @Override
        public long splice(WasiInputStream source, long length) {
          return 0;
        }

        @Override
        public long blockingSplice(WasiInputStream source, long length) {
          return 0;
        }

        @Override
        public WasiPollable subscribe() {
          return null;
        }

        @Override
        public void close() {}
      };

  @Nested
  @DisplayName("ConnectionStreams")
  class ConnectionStreamsTests {

    @Test
    @DisplayName("should store input and output streams")
    void shouldStoreInputAndOutputStreams() {
      WasiTcpSocket.ConnectionStreams cs =
          new WasiTcpSocket.ConnectionStreams(STUB_INPUT, STUB_OUTPUT);

      assertSame(STUB_INPUT, cs.getInputStream());
      assertSame(STUB_OUTPUT, cs.getOutputStream());
    }

    @Test
    @DisplayName("should throw when input stream is null")
    void shouldThrowWhenInputStreamIsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiTcpSocket.ConnectionStreams(null, STUB_OUTPUT));
    }

    @Test
    @DisplayName("should throw when output stream is null")
    void shouldThrowWhenOutputStreamIsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiTcpSocket.ConnectionStreams(STUB_INPUT, null));
    }
  }

  @Nested
  @DisplayName("AcceptResult")
  class AcceptResultTests {

    /** Minimal stub for WasiTcpSocket. */
    private final WasiTcpSocket STUB_SOCKET =
        new WasiTcpSocket() {
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

    @Test
    @DisplayName("should store socket, input and output streams")
    void shouldStoreSocketInputAndOutputStreams() {
      WasiTcpSocket.AcceptResult ar =
          new WasiTcpSocket.AcceptResult(STUB_SOCKET, STUB_INPUT, STUB_OUTPUT);

      assertSame(STUB_SOCKET, ar.getSocket());
      assertSame(STUB_INPUT, ar.getInputStream());
      assertSame(STUB_OUTPUT, ar.getOutputStream());
    }

    @Test
    @DisplayName("should throw when socket is null")
    void shouldThrowWhenSocketIsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiTcpSocket.AcceptResult(null, STUB_INPUT, STUB_OUTPUT));
    }

    @Test
    @DisplayName("should throw when input stream is null")
    void shouldThrowWhenInputStreamIsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiTcpSocket.AcceptResult(STUB_SOCKET, null, STUB_OUTPUT));
    }

    @Test
    @DisplayName("should throw when output stream is null")
    void shouldThrowWhenOutputStreamIsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiTcpSocket.AcceptResult(STUB_SOCKET, STUB_INPUT, null));
    }
  }

  @Nested
  @DisplayName("ShutdownType Enum")
  class ShutdownTypeTests {

    @Test
    @DisplayName("should have exactly 3 values")
    void shouldHaveExactly3Values() {
      assertEquals(3, WasiTcpSocket.ShutdownType.values().length);
    }

    @Test
    @DisplayName("should contain RECEIVE, SEND, and BOTH")
    void shouldContainAllValues() {
      Set<WasiTcpSocket.ShutdownType> values =
          new HashSet<>(Arrays.asList(WasiTcpSocket.ShutdownType.values()));

      assertTrue(values.contains(WasiTcpSocket.ShutdownType.RECEIVE));
      assertTrue(values.contains(WasiTcpSocket.ShutdownType.SEND));
      assertTrue(values.contains(WasiTcpSocket.ShutdownType.BOTH));
    }

    @Test
    @DisplayName("valueOf should return correct constant")
    void valueOfShouldReturnCorrectConstant() {
      assertEquals(
          WasiTcpSocket.ShutdownType.RECEIVE, WasiTcpSocket.ShutdownType.valueOf("RECEIVE"));
      assertEquals(WasiTcpSocket.ShutdownType.SEND, WasiTcpSocket.ShutdownType.valueOf("SEND"));
      assertEquals(WasiTcpSocket.ShutdownType.BOTH, WasiTcpSocket.ShutdownType.valueOf("BOTH"));
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiTcpSocket.ShutdownType.valueOf("INVALID"));
    }
  }
}
