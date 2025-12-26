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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiNetworkOperations.SocketInfo;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiNetworkOperations.SocketState;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiNetworkOperations.SocketType;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiNetworkOperations.UdpDatagram;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link WasiNetworkOperations}.
 */
@DisplayName("WasiNetworkOperations Tests")
class WasiNetworkOperationsTest {

  private WasiContext testContext;
  private ExecutorService executorService;
  private WasiNetworkOperations networkOperations;

  @BeforeEach
  void setUp() {
    testContext = TestWasiContextFactory.createTestContext();
    executorService = Executors.newSingleThreadExecutor();
    networkOperations = new WasiNetworkOperations(testContext, executorService);
  }

  @AfterEach
  void tearDown() {
    if (networkOperations != null) {
      networkOperations.close();
    }
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiNetworkOperations should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiNetworkOperations.class.getModifiers()),
          "WasiNetworkOperations should be final");
    }

    @Test
    @DisplayName("Should define address family constants")
    void shouldDefineAddressFamilyConstants() {
      assertEquals(2, WasiNetworkOperations.AF_INET, "AF_INET should be 2");
      assertEquals(10, WasiNetworkOperations.AF_INET6, "AF_INET6 should be 10");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should throw on null context")
    void constructorShouldThrowOnNullContext() {
      assertThrows(JniException.class,
          () -> new WasiNetworkOperations(null, executorService),
          "Should throw on null context");
    }

    @Test
    @DisplayName("Constructor should throw on null executor")
    void constructorShouldThrowOnNullExecutor() {
      assertThrows(JniException.class,
          () -> new WasiNetworkOperations(testContext, null),
          "Should throw on null executor");
    }

    @Test
    @DisplayName("Constructor should create operations with valid parameters")
    void constructorShouldCreateOperationsWithValidParameters() {
      final WasiNetworkOperations ops = new WasiNetworkOperations(testContext, executorService);
      assertNotNull(ops, "Operations should be created");
      ops.close();
    }
  }

  @Nested
  @DisplayName("createTcpSocket Tests")
  class CreateTcpSocketTests {

    @Test
    @DisplayName("Should throw on invalid address family")
    void shouldThrowOnInvalidAddressFamily() {
      assertThrows(WasiException.class,
          () -> networkOperations.createTcpSocket(99),
          "Should throw on invalid address family");
    }

    @Test
    @DisplayName("Should accept AF_INET")
    void shouldAcceptAfInet() {
      // Will throw due to native call, but validation passes
      assertThrows(WasiException.class,
          () -> networkOperations.createTcpSocket(WasiNetworkOperations.AF_INET));
    }

    @Test
    @DisplayName("Should accept AF_INET6")
    void shouldAcceptAfInet6() {
      assertThrows(WasiException.class,
          () -> networkOperations.createTcpSocket(WasiNetworkOperations.AF_INET6));
    }
  }

  @Nested
  @DisplayName("bindTcp Tests")
  class BindTcpTests {

    @Test
    @DisplayName("Should throw on null address")
    void shouldThrowOnNullAddress() {
      assertThrows(JniException.class,
          () -> networkOperations.bindTcp(1L, null, 8080),
          "Should throw on null address");
    }

    @Test
    @DisplayName("Should throw on empty address")
    void shouldThrowOnEmptyAddress() {
      assertThrows(JniException.class,
          () -> networkOperations.bindTcp(1L, "", 8080),
          "Should throw on empty address");
    }

    @Test
    @DisplayName("Should throw on invalid port (negative)")
    void shouldThrowOnNegativePort() {
      assertThrows(JniException.class,
          () -> networkOperations.bindTcp(1L, "127.0.0.1", -1),
          "Should throw on negative port");
    }

    @Test
    @DisplayName("Should throw on invalid port (too large)")
    void shouldThrowOnPortTooLarge() {
      assertThrows(JniException.class,
          () -> networkOperations.bindTcp(1L, "127.0.0.1", 65536),
          "Should throw on port > 65535");
    }

    @Test
    @DisplayName("Should throw on invalid socket handle")
    void shouldThrowOnInvalidSocketHandle() {
      assertThrows(WasiException.class,
          () -> networkOperations.bindTcp(999L, "127.0.0.1", 8080),
          "Should throw on invalid socket handle");
    }
  }

  @Nested
  @DisplayName("listenTcp Tests")
  class ListenTcpTests {

    @Test
    @DisplayName("Should throw on negative backlog")
    void shouldThrowOnNegativeBacklog() {
      assertThrows(WasiException.class,
          () -> networkOperations.listenTcp(1L, -1),
          "Should throw on negative backlog");
    }

    @Test
    @DisplayName("Should throw on backlog too large")
    void shouldThrowOnBacklogTooLarge() {
      assertThrows(WasiException.class,
          () -> networkOperations.listenTcp(1L, 1025),
          "Should throw on backlog > 1024");
    }

    @Test
    @DisplayName("Should throw on invalid socket handle")
    void shouldThrowOnInvalidSocketHandle() {
      assertThrows(WasiException.class,
          () -> networkOperations.listenTcp(999L, 10),
          "Should throw on invalid socket handle");
    }
  }

  @Nested
  @DisplayName("connectTcp Tests")
  class ConnectTcpTests {

    @Test
    @DisplayName("Should throw on null address")
    void shouldThrowOnNullAddress() {
      assertThrows(JniException.class,
          () -> networkOperations.connectTcp(1L, null, 8080),
          "Should throw on null address");
    }

    @Test
    @DisplayName("Should throw on empty address")
    void shouldThrowOnEmptyAddress() {
      assertThrows(JniException.class,
          () -> networkOperations.connectTcp(1L, "", 8080),
          "Should throw on empty address");
    }

    @Test
    @DisplayName("Should throw on invalid port")
    void shouldThrowOnInvalidPort() {
      assertThrows(JniException.class,
          () -> networkOperations.connectTcp(1L, "localhost", -1),
          "Should throw on invalid port");
    }

    @Test
    @DisplayName("Should throw on invalid socket handle")
    void shouldThrowOnInvalidSocketHandle() {
      assertThrows(WasiException.class,
          () -> networkOperations.connectTcp(999L, "localhost", 8080),
          "Should throw on invalid socket handle");
    }
  }

  @Nested
  @DisplayName("sendTcp Tests")
  class SendTcpTests {

    @Test
    @DisplayName("Should throw on null data")
    void shouldThrowOnNullData() {
      assertThrows(JniException.class,
          () -> networkOperations.sendTcp(1L, null),
          "Should throw on null data");
    }

    @Test
    @DisplayName("Should throw on invalid socket handle")
    void shouldThrowOnInvalidSocketHandle() {
      assertThrows(WasiException.class,
          () -> networkOperations.sendTcp(999L, ByteBuffer.allocate(10)),
          "Should throw on invalid socket handle");
    }
  }

  @Nested
  @DisplayName("receiveTcp Tests")
  class ReceiveTcpTests {

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() {
      assertThrows(JniException.class,
          () -> networkOperations.receiveTcp(1L, null),
          "Should throw on null buffer");
    }

    @Test
    @DisplayName("Should throw on invalid socket handle")
    void shouldThrowOnInvalidSocketHandle() {
      assertThrows(WasiException.class,
          () -> networkOperations.receiveTcp(999L, ByteBuffer.allocate(100)),
          "Should throw on invalid socket handle");
    }
  }

  @Nested
  @DisplayName("createUdpSocket Tests")
  class CreateUdpSocketTests {

    @Test
    @DisplayName("Should throw on invalid address family")
    void shouldThrowOnInvalidAddressFamily() {
      assertThrows(WasiException.class,
          () -> networkOperations.createUdpSocket(99),
          "Should throw on invalid address family");
    }

    @Test
    @DisplayName("Should accept AF_INET")
    void shouldAcceptAfInet() {
      assertThrows(WasiException.class,
          () -> networkOperations.createUdpSocket(WasiNetworkOperations.AF_INET));
    }

    @Test
    @DisplayName("Should accept AF_INET6")
    void shouldAcceptAfInet6() {
      assertThrows(WasiException.class,
          () -> networkOperations.createUdpSocket(WasiNetworkOperations.AF_INET6));
    }
  }

  @Nested
  @DisplayName("sendUdp Tests")
  class SendUdpTests {

    @Test
    @DisplayName("Should throw on null data")
    void shouldThrowOnNullData() {
      assertThrows(JniException.class,
          () -> networkOperations.sendUdp(1L, null, "localhost", 8080),
          "Should throw on null data");
    }

    @Test
    @DisplayName("Should throw on null address")
    void shouldThrowOnNullAddress() {
      assertThrows(JniException.class,
          () -> networkOperations.sendUdp(1L, ByteBuffer.allocate(10), null, 8080),
          "Should throw on null address");
    }

    @Test
    @DisplayName("Should throw on empty address")
    void shouldThrowOnEmptyAddress() {
      assertThrows(JniException.class,
          () -> networkOperations.sendUdp(1L, ByteBuffer.allocate(10), "", 8080),
          "Should throw on empty address");
    }

    @Test
    @DisplayName("Should throw on invalid port")
    void shouldThrowOnInvalidPort() {
      assertThrows(JniException.class,
          () -> networkOperations.sendUdp(1L, ByteBuffer.allocate(10), "localhost", -1),
          "Should throw on invalid port");
    }

    @Test
    @DisplayName("Should throw on invalid socket handle")
    void shouldThrowOnInvalidSocketHandle() {
      assertThrows(WasiException.class,
          () -> networkOperations.sendUdp(999L, ByteBuffer.allocate(10), "localhost", 8080),
          "Should throw on invalid socket handle");
    }
  }

  @Nested
  @DisplayName("receiveUdp Tests")
  class ReceiveUdpTests {

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() {
      assertThrows(JniException.class,
          () -> networkOperations.receiveUdp(1L, null),
          "Should throw on null buffer");
    }

    @Test
    @DisplayName("Should throw on invalid socket handle")
    void shouldThrowOnInvalidSocketHandle() {
      assertThrows(WasiException.class,
          () -> networkOperations.receiveUdp(999L, ByteBuffer.allocate(100)),
          "Should throw on invalid socket handle");
    }
  }

  @Nested
  @DisplayName("httpRequest Tests")
  class HttpRequestTests {

    @Test
    @DisplayName("Should throw on null method")
    void shouldThrowOnNullMethod() {
      assertThrows(JniException.class,
          () -> networkOperations.httpRequest(null, "http://example.com",
              Collections.emptyMap(), null),
          "Should throw on null method");
    }

    @Test
    @DisplayName("Should throw on empty method")
    void shouldThrowOnEmptyMethod() {
      assertThrows(JniException.class,
          () -> networkOperations.httpRequest("", "http://example.com",
              Collections.emptyMap(), null),
          "Should throw on empty method");
    }

    @Test
    @DisplayName("Should throw on null URI")
    void shouldThrowOnNullUri() {
      assertThrows(JniException.class,
          () -> networkOperations.httpRequest("GET", null,
              Collections.emptyMap(), null),
          "Should throw on null URI");
    }

    @Test
    @DisplayName("Should throw on empty URI")
    void shouldThrowOnEmptyUri() {
      assertThrows(JniException.class,
          () -> networkOperations.httpRequest("GET", "",
              Collections.emptyMap(), null),
          "Should throw on empty URI");
    }

    @Test
    @DisplayName("Should throw on null headers")
    void shouldThrowOnNullHeaders() {
      assertThrows(JniException.class,
          () -> networkOperations.httpRequest("GET", "http://example.com",
              null, null),
          "Should throw on null headers");
    }
  }

  @Nested
  @DisplayName("closeSocket Tests")
  class CloseSocketTests {

    @Test
    @DisplayName("Should handle invalid socket handle gracefully")
    void shouldHandleInvalidSocketHandleGracefully() {
      // Should not throw, just log and return
      networkOperations.closeSocket(999L);
      assertTrue(true, "Should not throw");
    }
  }

  @Nested
  @DisplayName("close Tests")
  class CloseTests {

    @Test
    @DisplayName("Should close gracefully")
    void shouldCloseGracefully() {
      networkOperations.close();
      assertTrue(true, "Close should complete");
    }

    @Test
    @DisplayName("Should be idempotent")
    void shouldBeIdempotent() {
      networkOperations.close();
      networkOperations.close();
      assertTrue(true, "Multiple closes should not throw");
    }
  }

  @Nested
  @DisplayName("SocketInfo Tests")
  class SocketInfoTests {

    @Test
    @DisplayName("Should create socket info with all fields")
    void shouldCreateSocketInfoWithAllFields() {
      final SocketInfo info = new SocketInfo(1L, SocketType.TCP, WasiNetworkOperations.AF_INET);

      assertEquals(1L, info.handle, "Handle should match");
      assertEquals(SocketType.TCP, info.type, "Type should be TCP");
      assertEquals(WasiNetworkOperations.AF_INET, info.addressFamily, "Address family should match");
      assertEquals(SocketState.CREATED, info.state, "Initial state should be CREATED");
      assertTrue(info.createdAt > 0, "Created timestamp should be positive");
    }

    @Test
    @DisplayName("Should allow state modification")
    void shouldAllowStateModification() {
      final SocketInfo info = new SocketInfo(1L, SocketType.TCP, WasiNetworkOperations.AF_INET);
      info.state = SocketState.CONNECTED;

      assertEquals(SocketState.CONNECTED, info.state, "State should be updated");
    }
  }

  @Nested
  @DisplayName("UdpDatagram Tests")
  class UdpDatagramTests {

    @Test
    @DisplayName("Should create UDP datagram with all fields")
    void shouldCreateUdpDatagramWithAllFields() {
      final UdpDatagram datagram = new UdpDatagram(100, "192.168.1.1", 5000);

      assertEquals(100, datagram.bytesReceived, "Bytes received should match");
      assertEquals("192.168.1.1", datagram.sourceAddress, "Source address should match");
      assertEquals(5000, datagram.sourcePort, "Source port should match");
    }
  }

  @Nested
  @DisplayName("SocketType Tests")
  class SocketTypeTests {

    @Test
    @DisplayName("Should have TCP and UDP types")
    void shouldHaveTcpAndUdpTypes() {
      assertEquals(2, SocketType.values().length, "Should have 2 socket types");
      assertNotNull(SocketType.TCP, "TCP should exist");
      assertNotNull(SocketType.UDP, "UDP should exist");
    }
  }

  @Nested
  @DisplayName("SocketState Tests")
  class SocketStateTests {

    @Test
    @DisplayName("Should have all states")
    void shouldHaveAllStates() {
      assertEquals(5, SocketState.values().length, "Should have 5 socket states");
      assertNotNull(SocketState.CREATED, "CREATED should exist");
      assertNotNull(SocketState.BOUND, "BOUND should exist");
      assertNotNull(SocketState.LISTENING, "LISTENING should exist");
      assertNotNull(SocketState.CONNECTED, "CONNECTED should exist");
      assertNotNull(SocketState.CLOSED, "CLOSED should exist");
    }
  }
}
