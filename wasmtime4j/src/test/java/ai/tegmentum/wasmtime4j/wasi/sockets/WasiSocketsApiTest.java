/*
 * Copyright 2024 Tegmentum AI
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive API tests for WASI-sockets package.
 *
 * <p>These tests verify the API contracts, class structures, and method signatures for the sockets
 * package without requiring native runtime initialization.
 *
 * @since 1.0.0
 */
@DisplayName("WASI Sockets API Tests")
class WasiSocketsApiTest {

  private static final Logger LOGGER = Logger.getLogger(WasiSocketsApiTest.class.getName());

  // ==================== IpAddressFamily Enum Tests ====================

  @Nested
  @DisplayName("IpAddressFamily Enum Tests")
  class IpAddressFamilyTests {

    @Test
    @DisplayName("IpAddressFamily should have expected values")
    void shouldHaveExpectedValues() {
      LOGGER.info("Testing IpAddressFamily enum values");
      IpAddressFamily[] values = IpAddressFamily.values();
      assertEquals(2, values.length, "IpAddressFamily should have 2 values");

      assertEquals(IpAddressFamily.IPV4, IpAddressFamily.valueOf("IPV4"));
      assertEquals(IpAddressFamily.IPV6, IpAddressFamily.valueOf("IPV6"));
    }

    @Test
    @DisplayName("IpAddressFamily should be an enum")
    void shouldBeEnum() {
      assertTrue(IpAddressFamily.class.isEnum());
    }

    @Test
    @DisplayName("IpAddressFamily ordinals should be sequential")
    void ordinalsShouldBeSequential() {
      assertEquals(0, IpAddressFamily.IPV4.ordinal());
      assertEquals(1, IpAddressFamily.IPV6.ordinal());
    }
  }

  // ==================== NetworkErrorCode Enum Tests ====================

  @Nested
  @DisplayName("NetworkErrorCode Enum Tests")
  class NetworkErrorCodeTests {

    @Test
    @DisplayName("NetworkErrorCode should have all expected values")
    void shouldHaveAllExpectedValues() {
      LOGGER.info("Testing NetworkErrorCode enum values");
      NetworkErrorCode[] values = NetworkErrorCode.values();
      assertEquals(21, values.length, "NetworkErrorCode should have 21 values");

      Set<String> expectedValues =
          new HashSet<>(
              Arrays.asList(
                  "UNKNOWN",
                  "ACCESS_DENIED",
                  "NOT_SUPPORTED",
                  "INVALID_ARGUMENT",
                  "OUT_OF_MEMORY",
                  "TIMEOUT",
                  "CONCURRENCY_CONFLICT",
                  "NOT_IN_PROGRESS",
                  "WOULD_BLOCK",
                  "INVALID_STATE",
                  "NEW_SOCKET_LIMIT",
                  "ADDRESS_NOT_BINDABLE",
                  "ADDRESS_IN_USE",
                  "REMOTE_UNREACHABLE",
                  "CONNECTION_REFUSED",
                  "CONNECTION_RESET",
                  "CONNECTION_ABORTED",
                  "DATAGRAM_TOO_LARGE",
                  "NAME_UNRESOLVABLE",
                  "TEMPORARY_RESOLVER_FAILURE",
                  "PERMANENT_RESOLVER_FAILURE"));

      for (NetworkErrorCode code : values) {
        LOGGER.info("  Found error code: " + code.name());
        assertTrue(expectedValues.contains(code.name()), "Unexpected error code: " + code.name());
      }
    }

    @Test
    @DisplayName("NetworkErrorCode valueOf should work correctly")
    void valueOfShouldWork() {
      assertEquals(NetworkErrorCode.UNKNOWN, NetworkErrorCode.valueOf("UNKNOWN"));
      assertEquals(NetworkErrorCode.TIMEOUT, NetworkErrorCode.valueOf("TIMEOUT"));
      assertEquals(
          NetworkErrorCode.CONNECTION_REFUSED, NetworkErrorCode.valueOf("CONNECTION_REFUSED"));
    }

    @Test
    @DisplayName("NetworkErrorCode should be an enum")
    void shouldBeEnum() {
      assertTrue(NetworkErrorCode.class.isEnum());
    }
  }

  // ==================== Ipv4Address Tests ====================

  @Nested
  @DisplayName("Ipv4Address Tests")
  class Ipv4AddressTests {

    @Test
    @DisplayName("Ipv4Address should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(Ipv4Address.class.getModifiers()));
    }

    @Test
    @DisplayName("Ipv4Address constructor should validate length")
    void constructorShouldValidateLength() {
      assertThrows(IllegalArgumentException.class, () -> new Ipv4Address(new byte[] {1, 2, 3}));
      assertThrows(
          IllegalArgumentException.class, () -> new Ipv4Address(new byte[] {1, 2, 3, 4, 5}));
      assertThrows(IllegalArgumentException.class, () -> new Ipv4Address(null));
    }

    @Test
    @DisplayName("Ipv4Address should create valid address")
    void shouldCreateValidAddress() {
      byte[] octets = {(byte) 192, (byte) 168, 1, 1};
      Ipv4Address addr = new Ipv4Address(octets);
      assertArrayEquals(octets, addr.getOctets());
    }

    @Test
    @DisplayName("Ipv4Address should defensive copy octets")
    void shouldDefensiveCopyOctets() {
      byte[] octets = {(byte) 192, (byte) 168, 1, 1};
      Ipv4Address addr = new Ipv4Address(octets);
      octets[0] = 10;
      assertNotEquals(octets[0], addr.getOctets()[0]);
    }

    @Test
    @DisplayName("Ipv4Address toString should format correctly")
    void toStringShouldFormatCorrectly() {
      Ipv4Address addr = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      assertEquals("192.168.1.1", addr.toString());
    }

    @Test
    @DisplayName("Ipv4Address equals and hashCode should work")
    void equalsAndHashCodeShouldWork() {
      Ipv4Address addr1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      Ipv4Address addr2 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      Ipv4Address addr3 = new Ipv4Address(new byte[] {10, 0, 0, 1});

      assertEquals(addr1, addr2);
      assertEquals(addr1.hashCode(), addr2.hashCode());
      assertNotEquals(addr1, addr3);
    }
  }

  // ==================== Ipv6Address Tests ====================

  @Nested
  @DisplayName("Ipv6Address Tests")
  class Ipv6AddressTests {

    @Test
    @DisplayName("Ipv6Address should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(Ipv6Address.class.getModifiers()));
    }

    @Test
    @DisplayName("Ipv6Address constructor should validate length")
    void constructorShouldValidateLength() {
      assertThrows(
          IllegalArgumentException.class, () -> new Ipv6Address(new short[] {1, 2, 3, 4, 5, 6, 7}));
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv6Address(new short[] {1, 2, 3, 4, 5, 6, 7, 8, 9}));
      assertThrows(IllegalArgumentException.class, () -> new Ipv6Address(null));
    }

    @Test
    @DisplayName("Ipv6Address should create valid address")
    void shouldCreateValidAddress() {
      short[] segments = {0, 0, 0, 0, 0, 0, 0, 1};
      Ipv6Address addr = new Ipv6Address(segments);
      assertArrayEquals(segments, addr.getSegments());
    }

    @Test
    @DisplayName("Ipv6Address should defensive copy segments")
    void shouldDefensiveCopySegments() {
      short[] segments = {0, 0, 0, 0, 0, 0, 0, 1};
      Ipv6Address addr = new Ipv6Address(segments);
      segments[7] = 2;
      assertNotEquals(segments[7], addr.getSegments()[7]);
    }

    @Test
    @DisplayName("Ipv6Address toString should format correctly")
    void toStringShouldFormatCorrectly() {
      Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      assertEquals("0:0:0:0:0:0:0:1", addr.toString());
    }

    @Test
    @DisplayName("Ipv6Address equals and hashCode should work")
    void equalsAndHashCodeShouldWork() {
      Ipv6Address addr1 = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      Ipv6Address addr2 = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      Ipv6Address addr3 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});

      assertEquals(addr1, addr2);
      assertEquals(addr1.hashCode(), addr2.hashCode());
      assertNotEquals(addr1, addr3);
    }
  }

  // ==================== IpAddress Tests ====================

  @Nested
  @DisplayName("IpAddress Tests")
  class IpAddressTests {

    @Test
    @DisplayName("IpAddress should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(IpAddress.class.getModifiers()));
    }

    @Test
    @DisplayName("IpAddress.ipv4 should create IPv4 variant")
    void ipv4FactoryShouldWork() {
      Ipv4Address ipv4 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      IpAddress addr = IpAddress.ipv4(ipv4);
      assertTrue(addr.isIpv4());
      assertEquals(ipv4, addr.getIpv4());
    }

    @Test
    @DisplayName("IpAddress.ipv6 should create IPv6 variant")
    void ipv6FactoryShouldWork() {
      Ipv6Address ipv6 = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      IpAddress addr = IpAddress.ipv6(ipv6);
      assertFalse(addr.isIpv4());
      assertEquals(ipv6, addr.getIpv6());
    }

    @Test
    @DisplayName("IpAddress factory methods should reject null")
    void factoryMethodsShouldRejectNull() {
      assertThrows(IllegalArgumentException.class, () -> IpAddress.ipv4(null));
      assertThrows(IllegalArgumentException.class, () -> IpAddress.ipv6(null));
    }

    @Test
    @DisplayName("IpAddress.getIpv4 should throw for IPv6 address")
    void getIpv4ShouldThrowForIpv6() {
      Ipv6Address ipv6 = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      IpAddress addr = IpAddress.ipv6(ipv6);
      assertThrows(IllegalStateException.class, addr::getIpv4);
    }

    @Test
    @DisplayName("IpAddress.getIpv6 should throw for IPv4 address")
    void getIpv6ShouldThrowForIpv4() {
      Ipv4Address ipv4 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      IpAddress addr = IpAddress.ipv4(ipv4);
      assertThrows(IllegalStateException.class, addr::getIpv6);
    }

    @Test
    @DisplayName("IpAddress equals and hashCode should work")
    void equalsAndHashCodeShouldWork() {
      Ipv4Address ipv4 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      IpAddress addr1 = IpAddress.ipv4(ipv4);
      IpAddress addr2 = IpAddress.ipv4(ipv4);
      assertEquals(addr1, addr2);
      assertEquals(addr1.hashCode(), addr2.hashCode());
    }
  }

  // ==================== Ipv4SocketAddress Tests ====================

  @Nested
  @DisplayName("Ipv4SocketAddress Tests")
  class Ipv4SocketAddressTests {

    @Test
    @DisplayName("Ipv4SocketAddress should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(Ipv4SocketAddress.class.getModifiers()));
    }

    @Test
    @DisplayName("Ipv4SocketAddress constructor should validate port range")
    void constructorShouldValidatePortRange() {
      Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      assertThrows(IllegalArgumentException.class, () -> new Ipv4SocketAddress(-1, addr));
      assertThrows(IllegalArgumentException.class, () -> new Ipv4SocketAddress(65536, addr));
    }

    @Test
    @DisplayName("Ipv4SocketAddress constructor should reject null address")
    void constructorShouldRejectNullAddress() {
      assertThrows(IllegalArgumentException.class, () -> new Ipv4SocketAddress(80, null));
    }

    @Test
    @DisplayName("Ipv4SocketAddress should store port and address")
    void shouldStorePortAndAddress() {
      Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      Ipv4SocketAddress sockAddr = new Ipv4SocketAddress(8080, addr);
      assertEquals(8080, sockAddr.getPort());
      assertEquals(addr, sockAddr.getAddress());
    }

    @Test
    @DisplayName("Ipv4SocketAddress toString should format correctly")
    void toStringShouldFormatCorrectly() {
      Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      Ipv4SocketAddress sockAddr = new Ipv4SocketAddress(8080, addr);
      assertEquals("127.0.0.1:8080", sockAddr.toString());
    }

    @Test
    @DisplayName("Ipv4SocketAddress equals and hashCode should work")
    void equalsAndHashCodeShouldWork() {
      Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      Ipv4SocketAddress sockAddr1 = new Ipv4SocketAddress(8080, addr);
      Ipv4SocketAddress sockAddr2 = new Ipv4SocketAddress(8080, addr);
      Ipv4SocketAddress sockAddr3 = new Ipv4SocketAddress(9090, addr);

      assertEquals(sockAddr1, sockAddr2);
      assertEquals(sockAddr1.hashCode(), sockAddr2.hashCode());
      assertNotEquals(sockAddr1, sockAddr3);
    }
  }

  // ==================== Ipv6SocketAddress Tests ====================

  @Nested
  @DisplayName("Ipv6SocketAddress Tests")
  class Ipv6SocketAddressTests {

    @Test
    @DisplayName("Ipv6SocketAddress should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(Ipv6SocketAddress.class.getModifiers()));
    }

    @Test
    @DisplayName("Ipv6SocketAddress constructor should validate port range")
    void constructorShouldValidatePortRange() {
      Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      assertThrows(IllegalArgumentException.class, () -> new Ipv6SocketAddress(-1, 0, addr, 0));
      assertThrows(IllegalArgumentException.class, () -> new Ipv6SocketAddress(65536, 0, addr, 0));
    }

    @Test
    @DisplayName("Ipv6SocketAddress constructor should reject null address")
    void constructorShouldRejectNullAddress() {
      assertThrows(IllegalArgumentException.class, () -> new Ipv6SocketAddress(80, 0, null, 0));
    }

    @Test
    @DisplayName("Ipv6SocketAddress should store all fields")
    void shouldStoreAllFields() {
      Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      Ipv6SocketAddress sockAddr = new Ipv6SocketAddress(8080, 123, addr, 456);
      assertEquals(8080, sockAddr.getPort());
      assertEquals(123, sockAddr.getFlowInfo());
      assertEquals(addr, sockAddr.getAddress());
      assertEquals(456, sockAddr.getScopeId());
    }

    @Test
    @DisplayName("Ipv6SocketAddress toString should format correctly")
    void toStringShouldFormatCorrectly() {
      Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      Ipv6SocketAddress sockAddr = new Ipv6SocketAddress(8080, 0, addr, 0);
      assertEquals("[0:0:0:0:0:0:0:1]:8080", sockAddr.toString());
    }

    @Test
    @DisplayName("Ipv6SocketAddress equals and hashCode should work")
    void equalsAndHashCodeShouldWork() {
      Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      Ipv6SocketAddress sockAddr1 = new Ipv6SocketAddress(8080, 0, addr, 0);
      Ipv6SocketAddress sockAddr2 = new Ipv6SocketAddress(8080, 0, addr, 0);
      Ipv6SocketAddress sockAddr3 = new Ipv6SocketAddress(9090, 0, addr, 0);

      assertEquals(sockAddr1, sockAddr2);
      assertEquals(sockAddr1.hashCode(), sockAddr2.hashCode());
      assertNotEquals(sockAddr1, sockAddr3);
    }
  }

  // ==================== IpSocketAddress Tests ====================

  @Nested
  @DisplayName("IpSocketAddress Tests")
  class IpSocketAddressTests {

    @Test
    @DisplayName("IpSocketAddress should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(IpSocketAddress.class.getModifiers()));
    }

    @Test
    @DisplayName("IpSocketAddress.ipv4 should create IPv4 variant")
    void ipv4FactoryShouldWork() {
      Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      Ipv4SocketAddress sockAddr = new Ipv4SocketAddress(8080, addr);
      IpSocketAddress ipSockAddr = IpSocketAddress.ipv4(sockAddr);
      assertTrue(ipSockAddr.isIpv4());
      assertEquals(sockAddr, ipSockAddr.getIpv4());
    }

    @Test
    @DisplayName("IpSocketAddress.ipv6 should create IPv6 variant")
    void ipv6FactoryShouldWork() {
      Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      Ipv6SocketAddress sockAddr = new Ipv6SocketAddress(8080, 0, addr, 0);
      IpSocketAddress ipSockAddr = IpSocketAddress.ipv6(sockAddr);
      assertFalse(ipSockAddr.isIpv4());
      assertEquals(sockAddr, ipSockAddr.getIpv6());
    }

    @Test
    @DisplayName("IpSocketAddress factory methods should reject null")
    void factoryMethodsShouldRejectNull() {
      assertThrows(IllegalArgumentException.class, () -> IpSocketAddress.ipv4(null));
      assertThrows(IllegalArgumentException.class, () -> IpSocketAddress.ipv6(null));
    }

    @Test
    @DisplayName("IpSocketAddress.getIpv4 should throw for IPv6 address")
    void getIpv4ShouldThrowForIpv6() {
      Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      Ipv6SocketAddress sockAddr = new Ipv6SocketAddress(8080, 0, addr, 0);
      IpSocketAddress ipSockAddr = IpSocketAddress.ipv6(sockAddr);
      assertThrows(IllegalStateException.class, ipSockAddr::getIpv4);
    }

    @Test
    @DisplayName("IpSocketAddress.getIpv6 should throw for IPv4 address")
    void getIpv6ShouldThrowForIpv4() {
      Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      Ipv4SocketAddress sockAddr = new Ipv4SocketAddress(8080, addr);
      IpSocketAddress ipSockAddr = IpSocketAddress.ipv4(sockAddr);
      assertThrows(IllegalStateException.class, ipSockAddr::getIpv6);
    }
  }

  // ==================== WasiNetwork Interface Tests ====================

  @Nested
  @DisplayName("WasiNetwork Interface Tests")
  class WasiNetworkInterfaceTests {

    @Test
    @DisplayName("WasiNetwork should be an interface")
    void shouldBeInterface() {
      assertTrue(WasiNetwork.class.isInterface());
    }

    @Test
    @DisplayName("WasiNetwork should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method close = WasiNetwork.class.getMethod("close");
      assertEquals(void.class, close.getReturnType());
    }
  }

  // ==================== WasiTcpSocket Interface Tests ====================

  @Nested
  @DisplayName("WasiTcpSocket Interface Tests")
  class WasiTcpSocketInterfaceTests {

    @Test
    @DisplayName("WasiTcpSocket should be an interface")
    void shouldBeInterface() {
      assertTrue(WasiTcpSocket.class.isInterface());
    }

    @Test
    @DisplayName("WasiTcpSocket should have bind methods")
    void shouldHaveBindMethods() throws NoSuchMethodException {
      Method startBind =
          WasiTcpSocket.class.getMethod("startBind", WasiNetwork.class, IpSocketAddress.class);
      assertEquals(void.class, startBind.getReturnType());

      Method finishBind = WasiTcpSocket.class.getMethod("finishBind");
      assertEquals(void.class, finishBind.getReturnType());
    }

    @Test
    @DisplayName("WasiTcpSocket should have connect methods")
    void shouldHaveConnectMethods() throws NoSuchMethodException {
      Method startConnect =
          WasiTcpSocket.class.getMethod("startConnect", WasiNetwork.class, IpSocketAddress.class);
      assertEquals(void.class, startConnect.getReturnType());

      Method finishConnect = WasiTcpSocket.class.getMethod("finishConnect");
      assertEquals(WasiTcpSocket.ConnectionStreams.class, finishConnect.getReturnType());
    }

    @Test
    @DisplayName("WasiTcpSocket should have listen methods")
    void shouldHaveListenMethods() throws NoSuchMethodException {
      Method startListen = WasiTcpSocket.class.getMethod("startListen");
      assertEquals(void.class, startListen.getReturnType());

      Method finishListen = WasiTcpSocket.class.getMethod("finishListen");
      assertEquals(void.class, finishListen.getReturnType());
    }

    @Test
    @DisplayName("WasiTcpSocket should have accept method")
    void shouldHaveAcceptMethod() throws NoSuchMethodException {
      Method accept = WasiTcpSocket.class.getMethod("accept");
      assertEquals(WasiTcpSocket.AcceptResult.class, accept.getReturnType());
    }

    @Test
    @DisplayName("WasiTcpSocket should have address methods")
    void shouldHaveAddressMethods() throws NoSuchMethodException {
      Method localAddress = WasiTcpSocket.class.getMethod("localAddress");
      assertEquals(IpSocketAddress.class, localAddress.getReturnType());

      Method remoteAddress = WasiTcpSocket.class.getMethod("remoteAddress");
      assertEquals(IpSocketAddress.class, remoteAddress.getReturnType());

      Method addressFamily = WasiTcpSocket.class.getMethod("addressFamily");
      assertEquals(IpAddressFamily.class, addressFamily.getReturnType());
    }

    @Test
    @DisplayName("WasiTcpSocket should have socket option methods")
    void shouldHaveSocketOptionMethods() throws NoSuchMethodException {
      assertNotNull(WasiTcpSocket.class.getMethod("setListenBacklogSize", long.class));
      assertNotNull(WasiTcpSocket.class.getMethod("setKeepAliveEnabled", boolean.class));
      assertNotNull(WasiTcpSocket.class.getMethod("setKeepAliveIdleTime", long.class));
      assertNotNull(WasiTcpSocket.class.getMethod("setKeepAliveInterval", long.class));
      assertNotNull(WasiTcpSocket.class.getMethod("setKeepAliveCount", int.class));
      assertNotNull(WasiTcpSocket.class.getMethod("setHopLimit", int.class));
      assertNotNull(WasiTcpSocket.class.getMethod("receiveBufferSize"));
      assertNotNull(WasiTcpSocket.class.getMethod("setReceiveBufferSize", long.class));
      assertNotNull(WasiTcpSocket.class.getMethod("sendBufferSize"));
      assertNotNull(WasiTcpSocket.class.getMethod("setSendBufferSize", long.class));
    }

    @Test
    @DisplayName("WasiTcpSocket should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      Method subscribe = WasiTcpSocket.class.getMethod("subscribe");
      assertEquals(WasiPollable.class, subscribe.getReturnType());
    }

    @Test
    @DisplayName("WasiTcpSocket should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      Method shutdown = WasiTcpSocket.class.getMethod("shutdown", WasiTcpSocket.ShutdownType.class);
      assertEquals(void.class, shutdown.getReturnType());
    }

    @Test
    @DisplayName("WasiTcpSocket should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method close = WasiTcpSocket.class.getMethod("close");
      assertEquals(void.class, close.getReturnType());
    }

    @Test
    @DisplayName("WasiTcpSocket.ShutdownType should have expected values")
    void shutdownTypeShouldHaveExpectedValues() {
      WasiTcpSocket.ShutdownType[] values = WasiTcpSocket.ShutdownType.values();
      assertEquals(3, values.length);
      assertNotNull(WasiTcpSocket.ShutdownType.valueOf("RECEIVE"));
      assertNotNull(WasiTcpSocket.ShutdownType.valueOf("SEND"));
      assertNotNull(WasiTcpSocket.ShutdownType.valueOf("BOTH"));
    }
  }

  // ==================== WasiTcpSocket.ConnectionStreams Tests ====================

  @Nested
  @DisplayName("WasiTcpSocket.ConnectionStreams Tests")
  class ConnectionStreamsTests {

    @Test
    @DisplayName("ConnectionStreams should be a static final inner class")
    void shouldBeStaticFinalInnerClass() throws ClassNotFoundException {
      Class<?> clazz =
          Class.forName("ai.tegmentum.wasmtime4j.wasi.sockets.WasiTcpSocket$ConnectionStreams");
      assertTrue(Modifier.isStatic(clazz.getModifiers()));
      assertTrue(Modifier.isFinal(clazz.getModifiers()));
    }

    @Test
    @DisplayName("ConnectionStreams should have getInputStream method")
    void shouldHaveGetInputStreamMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.ConnectionStreams.class.getMethod("getInputStream");
      assertEquals(WasiInputStream.class, method.getReturnType());
    }

    @Test
    @DisplayName("ConnectionStreams should have getOutputStream method")
    void shouldHaveGetOutputStreamMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.ConnectionStreams.class.getMethod("getOutputStream");
      assertEquals(WasiOutputStream.class, method.getReturnType());
    }
  }

  // ==================== WasiTcpSocket.AcceptResult Tests ====================

  @Nested
  @DisplayName("WasiTcpSocket.AcceptResult Tests")
  class AcceptResultTests {

    @Test
    @DisplayName("AcceptResult should be a static final inner class")
    void shouldBeStaticFinalInnerClass() throws ClassNotFoundException {
      Class<?> clazz =
          Class.forName("ai.tegmentum.wasmtime4j.wasi.sockets.WasiTcpSocket$AcceptResult");
      assertTrue(Modifier.isStatic(clazz.getModifiers()));
      assertTrue(Modifier.isFinal(clazz.getModifiers()));
    }

    @Test
    @DisplayName("AcceptResult should have expected getters")
    void shouldHaveExpectedGetters() throws NoSuchMethodException {
      Method getSocket = WasiTcpSocket.AcceptResult.class.getMethod("getSocket");
      assertEquals(WasiTcpSocket.class, getSocket.getReturnType());

      Method getInputStream = WasiTcpSocket.AcceptResult.class.getMethod("getInputStream");
      assertEquals(WasiInputStream.class, getInputStream.getReturnType());

      Method getOutputStream = WasiTcpSocket.AcceptResult.class.getMethod("getOutputStream");
      assertEquals(WasiOutputStream.class, getOutputStream.getReturnType());
    }
  }

  // ==================== WasiUdpSocket Interface Tests ====================

  @Nested
  @DisplayName("WasiUdpSocket Interface Tests")
  class WasiUdpSocketInterfaceTests {

    @Test
    @DisplayName("WasiUdpSocket should be an interface")
    void shouldBeInterface() {
      assertTrue(WasiUdpSocket.class.isInterface());
    }

    @Test
    @DisplayName("WasiUdpSocket should have bind methods")
    void shouldHaveBindMethods() throws NoSuchMethodException {
      Method startBind =
          WasiUdpSocket.class.getMethod("startBind", WasiNetwork.class, IpSocketAddress.class);
      assertEquals(void.class, startBind.getReturnType());

      Method finishBind = WasiUdpSocket.class.getMethod("finishBind");
      assertEquals(void.class, finishBind.getReturnType());
    }

    @Test
    @DisplayName("WasiUdpSocket should have stream method")
    void shouldHaveStreamMethod() throws NoSuchMethodException {
      Method stream =
          WasiUdpSocket.class.getMethod("stream", WasiNetwork.class, IpSocketAddress.class);
      assertEquals(void.class, stream.getReturnType());
    }

    @Test
    @DisplayName("WasiUdpSocket should have address methods")
    void shouldHaveAddressMethods() throws NoSuchMethodException {
      Method localAddress = WasiUdpSocket.class.getMethod("localAddress");
      assertEquals(IpSocketAddress.class, localAddress.getReturnType());

      Method remoteAddress = WasiUdpSocket.class.getMethod("remoteAddress");
      assertEquals(IpSocketAddress.class, remoteAddress.getReturnType());

      Method addressFamily = WasiUdpSocket.class.getMethod("addressFamily");
      assertEquals(IpAddressFamily.class, addressFamily.getReturnType());
    }

    @Test
    @DisplayName("WasiUdpSocket should have socket option methods")
    void shouldHaveSocketOptionMethods() throws NoSuchMethodException {
      assertNotNull(WasiUdpSocket.class.getMethod("setUnicastHopLimit", int.class));
      assertNotNull(WasiUdpSocket.class.getMethod("receiveBufferSize"));
      assertNotNull(WasiUdpSocket.class.getMethod("setReceiveBufferSize", long.class));
      assertNotNull(WasiUdpSocket.class.getMethod("sendBufferSize"));
      assertNotNull(WasiUdpSocket.class.getMethod("setSendBufferSize", long.class));
    }

    @Test
    @DisplayName("WasiUdpSocket should have receive and send methods")
    void shouldHaveReceiveAndSendMethods() throws NoSuchMethodException {
      Method receive = WasiUdpSocket.class.getMethod("receive", long.class);
      assertEquals(WasiUdpSocket.IncomingDatagram[].class, receive.getReturnType());

      Method send = WasiUdpSocket.class.getMethod("send", WasiUdpSocket.OutgoingDatagram[].class);
      assertEquals(long.class, send.getReturnType());
    }

    @Test
    @DisplayName("WasiUdpSocket should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      Method subscribe = WasiUdpSocket.class.getMethod("subscribe");
      assertEquals(WasiPollable.class, subscribe.getReturnType());
    }

    @Test
    @DisplayName("WasiUdpSocket should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method close = WasiUdpSocket.class.getMethod("close");
      assertEquals(void.class, close.getReturnType());
    }
  }

  // ==================== WasiUdpSocket.IncomingDatagram Tests ====================

  @Nested
  @DisplayName("WasiUdpSocket.IncomingDatagram Tests")
  class IncomingDatagramTests {

    @Test
    @DisplayName("IncomingDatagram should be a static final inner class")
    void shouldBeStaticFinalInnerClass() throws ClassNotFoundException {
      Class<?> clazz =
          Class.forName("ai.tegmentum.wasmtime4j.wasi.sockets.WasiUdpSocket$IncomingDatagram");
      assertTrue(Modifier.isStatic(clazz.getModifiers()));
      assertTrue(Modifier.isFinal(clazz.getModifiers()));
    }

    @Test
    @DisplayName("IncomingDatagram should have expected getters")
    void shouldHaveExpectedGetters() throws NoSuchMethodException {
      Method getData = WasiUdpSocket.IncomingDatagram.class.getMethod("getData");
      assertEquals(byte[].class, getData.getReturnType());

      Method getRemoteAddress = WasiUdpSocket.IncomingDatagram.class.getMethod("getRemoteAddress");
      assertEquals(IpSocketAddress.class, getRemoteAddress.getReturnType());
    }

    @Test
    @DisplayName("IncomingDatagram constructor should reject null data")
    void constructorShouldRejectNullData() {
      Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      Ipv4SocketAddress sockAddr = new Ipv4SocketAddress(8080, addr);
      IpSocketAddress ipSockAddr = IpSocketAddress.ipv4(sockAddr);
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiUdpSocket.IncomingDatagram(null, ipSockAddr));
    }

    @Test
    @DisplayName("IncomingDatagram constructor should reject null remoteAddress")
    void constructorShouldRejectNullRemoteAddress() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiUdpSocket.IncomingDatagram("data".getBytes(), null));
    }
  }

  // ==================== WasiUdpSocket.OutgoingDatagram Tests ====================

  @Nested
  @DisplayName("WasiUdpSocket.OutgoingDatagram Tests")
  class OutgoingDatagramTests {

    @Test
    @DisplayName("OutgoingDatagram should be a static final inner class")
    void shouldBeStaticFinalInnerClass() throws ClassNotFoundException {
      Class<?> clazz =
          Class.forName("ai.tegmentum.wasmtime4j.wasi.sockets.WasiUdpSocket$OutgoingDatagram");
      assertTrue(Modifier.isStatic(clazz.getModifiers()));
      assertTrue(Modifier.isFinal(clazz.getModifiers()));
    }

    @Test
    @DisplayName("OutgoingDatagram should have expected getters")
    void shouldHaveExpectedGetters() throws NoSuchMethodException {
      Method getData = WasiUdpSocket.OutgoingDatagram.class.getMethod("getData");
      assertEquals(byte[].class, getData.getReturnType());

      Method getRemoteAddress = WasiUdpSocket.OutgoingDatagram.class.getMethod("getRemoteAddress");
      assertEquals(IpSocketAddress.class, getRemoteAddress.getReturnType());

      Method hasRemoteAddress = WasiUdpSocket.OutgoingDatagram.class.getMethod("hasRemoteAddress");
      assertEquals(boolean.class, hasRemoteAddress.getReturnType());
    }

    @Test
    @DisplayName("OutgoingDatagram with remote address should work")
    void withRemoteAddressShouldWork() {
      Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      Ipv4SocketAddress sockAddr = new Ipv4SocketAddress(8080, addr);
      IpSocketAddress ipSockAddr = IpSocketAddress.ipv4(sockAddr);
      WasiUdpSocket.OutgoingDatagram datagram =
          new WasiUdpSocket.OutgoingDatagram("data".getBytes(), ipSockAddr);
      assertTrue(datagram.hasRemoteAddress());
      assertEquals(ipSockAddr, datagram.getRemoteAddress());
    }

    @Test
    @DisplayName("OutgoingDatagram without remote address should work")
    void withoutRemoteAddressShouldWork() {
      WasiUdpSocket.OutgoingDatagram datagram =
          new WasiUdpSocket.OutgoingDatagram("data".getBytes());
      assertFalse(datagram.hasRemoteAddress());
      assertNull(datagram.getRemoteAddress());
    }

    @Test
    @DisplayName("OutgoingDatagram constructor should reject null data")
    void constructorShouldRejectNullData() {
      assertThrows(
          IllegalArgumentException.class, () -> new WasiUdpSocket.OutgoingDatagram((byte[]) null));
    }
  }

  // ==================== WasiIpNameLookup Interface Tests ====================

  @Nested
  @DisplayName("WasiIpNameLookup Interface Tests")
  class WasiIpNameLookupInterfaceTests {

    @Test
    @DisplayName("WasiIpNameLookup should be an interface")
    void shouldBeInterface() {
      assertTrue(WasiIpNameLookup.class.isInterface());
    }

    @Test
    @DisplayName("WasiIpNameLookup should have resolveAddresses methods")
    void shouldHaveResolveAddressesMethods() throws NoSuchMethodException {
      Method resolve =
          WasiIpNameLookup.class.getMethod("resolveAddresses", WasiNetwork.class, String.class);
      assertEquals(ResolveAddressStream.class, resolve.getReturnType());

      Method resolveWithFamily =
          WasiIpNameLookup.class.getMethod(
              "resolveAddresses", WasiNetwork.class, String.class, IpAddressFamily.class);
      assertEquals(ResolveAddressStream.class, resolveWithFamily.getReturnType());
    }
  }

  // ==================== ResolveAddressStream Interface Tests ====================

  @Nested
  @DisplayName("ResolveAddressStream Interface Tests")
  class ResolveAddressStreamInterfaceTests {

    @Test
    @DisplayName("ResolveAddressStream should be an interface")
    void shouldBeInterface() {
      assertTrue(ResolveAddressStream.class.isInterface());
    }

    @Test
    @DisplayName("ResolveAddressStream should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(AutoCloseable.class.isAssignableFrom(ResolveAddressStream.class));
    }

    @Test
    @DisplayName("ResolveAddressStream should have resolveNextAddress method")
    void shouldHaveResolveNextAddressMethod() throws NoSuchMethodException {
      Method method = ResolveAddressStream.class.getMethod("resolveNextAddress");
      assertEquals(Optional.class, method.getReturnType());
    }

    @Test
    @DisplayName("ResolveAddressStream should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      Method method = ResolveAddressStream.class.getMethod("subscribe");
      assertEquals(void.class, method.getReturnType());
    }

    @Test
    @DisplayName("ResolveAddressStream should have isClosed method")
    void shouldHaveIsClosedMethod() throws NoSuchMethodException {
      Method method = ResolveAddressStream.class.getMethod("isClosed");
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("ResolveAddressStream should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = ResolveAddressStream.class.getMethod("close");
      assertEquals(void.class, method.getReturnType());
    }
  }

  // ==================== Package Consistency Tests ====================

  @Nested
  @DisplayName("Package Consistency Tests")
  class PackageConsistencyTests {

    @Test
    @DisplayName("All sockets classes should be in correct package")
    void allClassesShouldBeInCorrectPackage() {
      String expectedPackage = "ai.tegmentum.wasmtime4j.wasi.sockets";
      Class<?>[] classes = {
        IpAddressFamily.class,
        NetworkErrorCode.class,
        IpAddress.class,
        Ipv4Address.class,
        Ipv6Address.class,
        IpSocketAddress.class,
        Ipv4SocketAddress.class,
        Ipv6SocketAddress.class,
        WasiNetwork.class,
        WasiTcpSocket.class,
        WasiUdpSocket.class,
        WasiIpNameLookup.class,
        ResolveAddressStream.class
      };

      for (Class<?> clazz : classes) {
        assertEquals(
            expectedPackage,
            clazz.getPackage().getName(),
            clazz.getSimpleName() + " should be in " + expectedPackage);
      }
    }

    @Test
    @DisplayName("Package should have expected number of public classes")
    void shouldHaveExpectedNumberOfPublicClasses() {
      // 2 enums (IpAddressFamily, NetworkErrorCode)
      // 5 interfaces (WasiNetwork, WasiTcpSocket, WasiUdpSocket, WasiIpNameLookup,
      // ResolveAddressStream)
      // 6 classes (IpAddress, Ipv4Address, Ipv6Address, IpSocketAddress, Ipv4SocketAddress,
      // Ipv6SocketAddress)
      // Total: 13 main classes
      int expectedClasses = 13;
      Class<?>[] knownClasses = {
        IpAddressFamily.class,
        NetworkErrorCode.class,
        IpAddress.class,
        Ipv4Address.class,
        Ipv6Address.class,
        IpSocketAddress.class,
        Ipv4SocketAddress.class,
        Ipv6SocketAddress.class,
        WasiNetwork.class,
        WasiTcpSocket.class,
        WasiUdpSocket.class,
        WasiIpNameLookup.class,
        ResolveAddressStream.class
      };
      assertEquals(
          expectedClasses,
          knownClasses.length,
          "Package should have expected number of main classes");
    }

    @Test
    @DisplayName("Address classes should be final")
    void addressClassesShouldBeFinal() {
      Class<?>[] finalClasses = {
        IpAddress.class,
        Ipv4Address.class,
        Ipv6Address.class,
        IpSocketAddress.class,
        Ipv4SocketAddress.class,
        Ipv6SocketAddress.class
      };

      for (Class<?> clazz : finalClasses) {
        assertTrue(
            Modifier.isFinal(clazz.getModifiers()), clazz.getSimpleName() + " should be final");
      }
    }
  }
}
