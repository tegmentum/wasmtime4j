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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Edge case integration tests for WASI Socket timeout and configuration.
 *
 * <p>This test class focuses on boundary conditions and error handling scenarios:
 *
 * <ul>
 *   <li>Keep-alive timeout configuration edge cases
 *   <li>Buffer size edge cases
 *   <li>Socket address validation edge cases
 *   <li>Connection lifecycle edge cases
 *   <li>Concurrent socket operations
 * </ul>
 *
 * @since 1.1.0
 */
@DisplayName("WASI Socket Timeout Integration Tests")
public final class WasiSocketTimeoutIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiSocketTimeoutIntegrationTest.class.getName());

  private static boolean wasiSocketsAvailable = false;
  private static WasmRuntime sharedRuntime;
  private static Engine sharedEngine;

  @BeforeAll
  static void checkWasiSocketsAvailable() {
    try {
      sharedRuntime = WasmRuntimeFactory.create();
      sharedEngine = sharedRuntime.createEngine();

      // Try to load the JNI WASI Sockets classes to verify native implementation
      final Class<?> jniTcpSocketClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.sockets.JniWasiTcpSocket");
      final Class<?> jniUdpSocketClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.sockets.JniWasiUdpSocket");

      if (jniTcpSocketClass != null && jniUdpSocketClass != null) {
        wasiSocketsAvailable = true;
        LOGGER.info("WASI Sockets is available (JNI classes loaded successfully)");
      }
    } catch (final Exception e) {
      LOGGER.warning("WASI Sockets not available: " + e.getMessage());
      wasiSocketsAvailable = false;
    }
  }

  @AfterAll
  static void cleanup() {
    if (sharedEngine != null) {
      try {
        sharedEngine.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close shared engine: " + e.getMessage());
      }
    }
    if (sharedRuntime != null) {
      try {
        sharedRuntime.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close shared runtime: " + e.getMessage());
      }
    }
  }

  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  @Nested
  @DisplayName("IPv4 Address Edge Cases")
  class Ipv4AddressEdgeCases {

    @Test
    @DisplayName("should handle all zeros address (0.0.0.0)")
    void shouldHandleAllZerosAddress() {
      LOGGER.info("Testing all zeros IPv4 address");

      final Ipv4Address address = new Ipv4Address(new byte[] {0, 0, 0, 0});
      assertNotNull(address, "Address should not be null");
      assertEquals("0.0.0.0", address.toString(), "Should be 0.0.0.0");

      LOGGER.info("All zeros address handled: " + address);
    }

    @Test
    @DisplayName("should handle broadcast address (255.255.255.255)")
    void shouldHandleBroadcastAddress() {
      LOGGER.info("Testing broadcast IPv4 address");

      final Ipv4Address address =
          new Ipv4Address(new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255});
      assertNotNull(address, "Address should not be null");
      assertEquals("255.255.255.255", address.toString(), "Should be 255.255.255.255");

      LOGGER.info("Broadcast address handled: " + address);
    }

    @Test
    @DisplayName("should handle loopback address range")
    void shouldHandleLoopbackAddressRange() {
      LOGGER.info("Testing loopback IPv4 addresses");

      // Standard loopback
      final Ipv4Address loopback127 = new Ipv4Address(new byte[] {127, 0, 0, 1});
      assertEquals("127.0.0.1", loopback127.toString(), "Should be standard loopback");

      // Alternative loopback addresses
      final Ipv4Address loopback127_255 = new Ipv4Address(new byte[] {127, (byte) 255, 0, 1});
      assertTrue(
          loopback127_255.toString().startsWith("127."),
          "Should be in loopback range: " + loopback127_255);

      LOGGER.info("Loopback addresses handled");
    }

    @Test
    @DisplayName("should correctly compare address equality")
    void shouldCorrectlyCompareAddressEquality() {
      LOGGER.info("Testing IPv4 address equality");

      final byte[] octets1 = new byte[] {10, 0, 0, 1};
      final byte[] octets2 = new byte[] {10, 0, 0, 1};
      final byte[] octets3 = new byte[] {10, 0, 0, 2};

      final Ipv4Address addr1 = new Ipv4Address(octets1);
      final Ipv4Address addr2 = new Ipv4Address(octets2);
      final Ipv4Address addr3 = new Ipv4Address(octets3);

      assertEquals(addr1, addr2, "Same octets should be equal");
      assertNotEquals(addr1, addr3, "Different octets should not be equal");
      assertEquals(addr1.hashCode(), addr2.hashCode(), "Hash codes should match");

      LOGGER.info("Address equality verified");
    }

    @Test
    @DisplayName("should handle defensive copy of octets")
    void shouldHandleDefensiveCopyOfOctets() {
      LOGGER.info("Testing defensive copy of IPv4 octets");

      final byte[] original = new byte[] {(byte) 192, (byte) 168, 1, 1};
      final Ipv4Address address = new Ipv4Address(original);

      // Modify original array
      original[0] = 10;

      // Address should be unchanged
      final byte[] retrieved = address.getOctets();
      assertEquals((byte) 192, retrieved[0], "Address should not be affected by original change");

      // Modify retrieved array
      retrieved[1] = 0;

      // Address should still be unchanged
      final byte[] retrieved2 = address.getOctets();
      assertEquals((byte) 168, retrieved2[1], "Address should not be affected by retrieved change");

      LOGGER.info("Defensive copy verified");
    }
  }

  @Nested
  @DisplayName("IPv6 Address Edge Cases")
  class Ipv6AddressEdgeCases {

    @Test
    @DisplayName("should handle all zeros address (::)")
    void shouldHandleAllZerosIpv6Address() {
      LOGGER.info("Testing all zeros IPv6 address");

      final Ipv6Address address = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 0});
      assertNotNull(address, "Address should not be null");

      final short[] segments = address.getSegments();
      for (final short segment : segments) {
        assertEquals(0, segment, "All segments should be zero");
      }

      LOGGER.info("All zeros IPv6 address handled");
    }

    @Test
    @DisplayName("should handle loopback address (::1)")
    void shouldHandleLoopbackIpv6Address() {
      LOGGER.info("Testing loopback IPv6 address");

      final Ipv6Address address = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      assertNotNull(address, "Address should not be null");

      final short[] segments = address.getSegments();
      for (int i = 0; i < 7; i++) {
        assertEquals(0, segments[i], "First 7 segments should be zero");
      }
      assertEquals(1, segments[7], "Last segment should be 1");

      LOGGER.info("Loopback IPv6 address handled");
    }

    @Test
    @DisplayName("should handle max value segments")
    void shouldHandleMaxValueSegments() {
      LOGGER.info("Testing max value IPv6 segments");

      final short maxSegment = (short) 0xFFFF;
      final Ipv6Address address =
          new Ipv6Address(
              new short[] {
                maxSegment, maxSegment, maxSegment, maxSegment,
                maxSegment, maxSegment, maxSegment, maxSegment
              });

      final short[] segments = address.getSegments();
      for (final short segment : segments) {
        assertEquals(maxSegment, segment, "All segments should be max value");
      }

      LOGGER.info("Max value segments handled");
    }

    @Test
    @DisplayName("should handle link-local address")
    void shouldHandleLinkLocalIpv6Address() {
      LOGGER.info("Testing link-local IPv6 address");

      // fe80::1 (link-local)
      final Ipv6Address address =
          new Ipv6Address(new short[] {(short) 0xFE80, 0, 0, 0, 0, 0, 0, 1});
      assertNotNull(address, "Link-local address should not be null");

      final short[] segments = address.getSegments();
      assertEquals((short) 0xFE80, segments[0], "First segment should be fe80");

      LOGGER.info("Link-local IPv6 address handled");
    }
  }

  @Nested
  @DisplayName("Socket Address Port Edge Cases")
  class SocketAddressPortEdgeCases {

    @Test
    @DisplayName("should handle ephemeral port zero")
    void shouldHandleEphemeralPortZero() {
      LOGGER.info("Testing ephemeral port (0)");

      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(0, addr);

      assertEquals(0, socketAddr.getPort(), "Port should be 0 (ephemeral)");

      LOGGER.info("Ephemeral port handled");
    }

    @Test
    @DisplayName("should handle maximum port 65535")
    void shouldHandleMaximumPort() {
      LOGGER.info("Testing maximum port (65535)");

      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(65535, addr);

      assertEquals(65535, socketAddr.getPort(), "Port should be 65535");

      LOGGER.info("Maximum port handled");
    }

    @Test
    @DisplayName("should reject negative port")
    void shouldRejectNegativePort() {
      LOGGER.info("Testing negative port rejection");

      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});

      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv4SocketAddress(-1, addr),
          "Should reject negative port");

      LOGGER.info("Negative port rejected");
    }

    @Test
    @DisplayName("should reject port exceeding 65535")
    void shouldRejectPortExceedingMax() {
      LOGGER.info("Testing port exceeding max rejection");

      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});

      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv4SocketAddress(65536, addr),
          "Should reject port > 65535");

      LOGGER.info("Port exceeding max rejected");
    }

    @ParameterizedTest
    @ValueSource(ints = {20, 21, 22, 23, 25, 53, 80, 110, 143, 443, 993, 995, 3306, 5432, 8080})
    @DisplayName("should handle well-known ports")
    void shouldHandleWellKnownPorts(final int port) {
      LOGGER.info("Testing well-known port: " + port);

      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(port, addr);

      assertEquals(port, socketAddr.getPort(), "Should handle well-known port: " + port);

      LOGGER.info("Well-known port " + port + " handled");
    }
  }

  @Nested
  @DisplayName("IPv6 Socket Address Edge Cases")
  class Ipv6SocketAddressEdgeCases {

    @Test
    @DisplayName("should handle flow info values")
    void shouldHandleFlowInfoValues() {
      LOGGER.info("Testing IPv6 flow info");

      final Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});

      // Zero flow info
      final Ipv6SocketAddress socketAddr1 = new Ipv6SocketAddress(80, 0, addr, 0);
      assertEquals(0, socketAddr1.getFlowInfo(), "Flow info should be 0");

      // Max flow info (20 bits)
      final int maxFlowInfo = 0xFFFFF;
      final Ipv6SocketAddress socketAddr2 = new Ipv6SocketAddress(80, maxFlowInfo, addr, 0);
      assertEquals(maxFlowInfo, socketAddr2.getFlowInfo(), "Flow info should be max value");

      LOGGER.info("Flow info values handled");
    }

    @Test
    @DisplayName("should handle scope ID values")
    void shouldHandleScopeIdValues() {
      LOGGER.info("Testing IPv6 scope ID");

      final Ipv6Address addr = new Ipv6Address(new short[] {(short) 0xFE80, 0, 0, 0, 0, 0, 0, 1});

      // Zero scope ID
      final Ipv6SocketAddress socketAddr1 = new Ipv6SocketAddress(80, 0, addr, 0);
      assertEquals(0, socketAddr1.getScopeId(), "Scope ID should be 0");

      // Non-zero scope ID (interface index)
      final int scopeId = 1;
      final Ipv6SocketAddress socketAddr2 = new Ipv6SocketAddress(80, 0, addr, scopeId);
      assertEquals(scopeId, socketAddr2.getScopeId(), "Scope ID should match");

      LOGGER.info("Scope ID values handled");
    }
  }

  @Nested
  @DisplayName("IpSocketAddress Variant Edge Cases")
  class IpSocketAddressVariantEdgeCases {

    @Test
    @DisplayName("should correctly identify IPv4 vs IPv6")
    void shouldCorrectlyIdentifyAddressFamily() {
      LOGGER.info("Testing address family identification");

      final Ipv4Address ipv4Addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpSocketAddress ipv4Socket = IpSocketAddress.ipv4(new Ipv4SocketAddress(80, ipv4Addr));

      final Ipv6Address ipv6Addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final IpSocketAddress ipv6Socket =
          IpSocketAddress.ipv6(new Ipv6SocketAddress(80, 0, ipv6Addr, 0));

      assertTrue(ipv4Socket.isIpv4(), "IPv4 socket should report isIpv4=true");
      assertFalse(ipv6Socket.isIpv4(), "IPv6 socket should report isIpv4=false");

      LOGGER.info("Address family identification verified");
    }

    @Test
    @DisplayName("should throw when accessing wrong address type")
    void shouldThrowWhenAccessingWrongAddressType() {
      LOGGER.info("Testing wrong address type access");

      final Ipv4Address ipv4Addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpSocketAddress ipv4Socket = IpSocketAddress.ipv4(new Ipv4SocketAddress(80, ipv4Addr));

      assertThrows(
          IllegalStateException.class,
          ipv4Socket::getIpv6,
          "Should throw when getting IPv6 from IPv4 socket");

      final Ipv6Address ipv6Addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final IpSocketAddress ipv6Socket =
          IpSocketAddress.ipv6(new Ipv6SocketAddress(80, 0, ipv6Addr, 0));

      assertThrows(
          IllegalStateException.class,
          ipv6Socket::getIpv4,
          "Should throw when getting IPv4 from IPv6 socket");

      LOGGER.info("Wrong address type access throws correctly");
    }

    @Test
    @DisplayName("should reject null addresses in factory methods")
    void shouldRejectNullAddressesInFactoryMethods() {
      LOGGER.info("Testing null address rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> IpSocketAddress.ipv4(null),
          "Should reject null IPv4 address");

      assertThrows(
          IllegalArgumentException.class,
          () -> IpSocketAddress.ipv6(null),
          "Should reject null IPv6 address");

      LOGGER.info("Null addresses rejected correctly");
    }
  }

  @Nested
  @DisplayName("Datagram Edge Cases")
  class DatagramEdgeCases {

    @Test
    @DisplayName("should handle empty datagram data")
    void shouldHandleEmptyDatagramData() {
      LOGGER.info("Testing empty datagram data");

      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpSocketAddress socketAddr = IpSocketAddress.ipv4(new Ipv4SocketAddress(8080, addr));

      // Empty outgoing datagram
      final WasiUdpSocket.OutgoingDatagram outgoing =
          new WasiUdpSocket.OutgoingDatagram(new byte[0], socketAddr);
      assertEquals(0, outgoing.getData().length, "Empty data should have length 0");

      // Empty incoming datagram
      final WasiUdpSocket.IncomingDatagram incoming =
          new WasiUdpSocket.IncomingDatagram(new byte[0], socketAddr);
      assertEquals(0, incoming.getData().length, "Empty data should have length 0");

      LOGGER.info("Empty datagram handled");
    }

    @Test
    @DisplayName("should handle large datagram data")
    void shouldHandleLargeDatagramData() {
      LOGGER.info("Testing large datagram data");

      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpSocketAddress socketAddr = IpSocketAddress.ipv4(new Ipv4SocketAddress(8080, addr));

      // Max UDP payload (65507 bytes for IPv4)
      final byte[] largeData = new byte[65507];
      for (int i = 0; i < largeData.length; i++) {
        largeData[i] = (byte) (i % 256);
      }

      final WasiUdpSocket.OutgoingDatagram outgoing =
          new WasiUdpSocket.OutgoingDatagram(largeData, socketAddr);
      assertArrayEquals(largeData, outgoing.getData(), "Large data should be preserved");

      LOGGER.info("Large datagram handled: " + largeData.length + " bytes");
    }

    @Test
    @DisplayName("should handle datagram without remote address")
    void shouldHandleDatagramWithoutRemoteAddress() {
      LOGGER.info("Testing datagram without remote address");

      final byte[] data = new byte[] {1, 2, 3, 4};
      final WasiUdpSocket.OutgoingDatagram outgoing = new WasiUdpSocket.OutgoingDatagram(data);

      assertFalse(outgoing.hasRemoteAddress(), "Should not have remote address");
      assertArrayEquals(data, outgoing.getData(), "Data should be preserved");

      LOGGER.info("Datagram without remote address handled");
    }

    @Test
    @DisplayName("should reject null data in datagram")
    void shouldRejectNullDataInDatagram() {
      LOGGER.info("Testing null data rejection");

      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpSocketAddress socketAddr = IpSocketAddress.ipv4(new Ipv4SocketAddress(8080, addr));

      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiUdpSocket.OutgoingDatagram(null, socketAddr),
          "Should reject null data in outgoing datagram");

      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiUdpSocket.OutgoingDatagram(null),
          "Should reject null data in outgoing datagram without address");

      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiUdpSocket.IncomingDatagram(null, socketAddr),
          "Should reject null data in incoming datagram");

      LOGGER.info("Null data rejected correctly");
    }

    @Test
    @DisplayName("should make defensive copy of datagram data")
    void shouldMakeDefensiveCopyOfDatagramData() {
      LOGGER.info("Testing defensive copy of datagram data");

      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpSocketAddress socketAddr = IpSocketAddress.ipv4(new Ipv4SocketAddress(8080, addr));

      final byte[] originalData = new byte[] {1, 2, 3, 4};
      final WasiUdpSocket.OutgoingDatagram datagram =
          new WasiUdpSocket.OutgoingDatagram(originalData, socketAddr);

      // Modify original
      originalData[0] = 99;

      // Datagram should be unchanged
      final byte[] retrieved = datagram.getData();
      assertEquals(1, retrieved[0], "Datagram should not be affected by original change");

      // Modify retrieved
      retrieved[1] = 99;

      // Datagram should still be unchanged
      final byte[] retrieved2 = datagram.getData();
      assertEquals(2, retrieved2[1], "Datagram should not be affected by retrieved change");

      LOGGER.info("Defensive copy verified");
    }
  }

  @Nested
  @DisplayName("TCP Socket Inner Class Edge Cases")
  class TcpSocketInnerClassEdgeCases {

    @Test
    @DisplayName("should reject null in ConnectionStreams")
    void shouldRejectNullInConnectionStreams() {
      LOGGER.info("Testing null rejection in ConnectionStreams");

      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiTcpSocket.ConnectionStreams(null, null),
          "Should reject null input stream");

      LOGGER.info("Null rejected in ConnectionStreams");
    }

    @Test
    @DisplayName("should reject null in AcceptResult")
    void shouldRejectNullInAcceptResult() {
      LOGGER.info("Testing null rejection in AcceptResult");

      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiTcpSocket.AcceptResult(null, null, null),
          "Should reject null socket");

      LOGGER.info("Null rejected in AcceptResult");
    }

    @Test
    @DisplayName("should enumerate all shutdown types")
    void shouldEnumerateAllShutdownTypes() {
      LOGGER.info("Testing shutdown type enumeration");

      final WasiTcpSocket.ShutdownType[] types = WasiTcpSocket.ShutdownType.values();
      assertEquals(3, types.length, "Should have exactly 3 shutdown types");

      boolean hasReceive = false;
      boolean hasSend = false;
      boolean hasBoth = false;

      for (final WasiTcpSocket.ShutdownType type : types) {
        if (type == WasiTcpSocket.ShutdownType.RECEIVE) {
          hasReceive = true;
        }
        if (type == WasiTcpSocket.ShutdownType.SEND) {
          hasSend = true;
        }
        if (type == WasiTcpSocket.ShutdownType.BOTH) {
          hasBoth = true;
        }
      }

      assertTrue(hasReceive, "Should have RECEIVE");
      assertTrue(hasSend, "Should have SEND");
      assertTrue(hasBoth, "Should have BOTH");

      LOGGER.info("Shutdown types enumerated: " + types.length);
    }
  }

  @Nested
  @DisplayName("Address Family Edge Cases")
  class AddressFamilyEdgeCases {

    @Test
    @DisplayName("should enumerate all address families")
    void shouldEnumerateAllAddressFamilies() {
      LOGGER.info("Testing address family enumeration");

      final IpAddressFamily[] families = IpAddressFamily.values();
      assertEquals(2, families.length, "Should have exactly 2 address families");

      boolean hasIpv4 = false;
      boolean hasIpv6 = false;

      for (final IpAddressFamily family : families) {
        if (family == IpAddressFamily.IPV4) {
          hasIpv4 = true;
        }
        if (family == IpAddressFamily.IPV6) {
          hasIpv6 = true;
        }
      }

      assertTrue(hasIpv4, "Should have IPV4");
      assertTrue(hasIpv6, "Should have IPV6");

      LOGGER.info("Address families enumerated: " + families.length);
    }

    @Test
    @DisplayName("should convert address family by name")
    void shouldConvertAddressFamilyByName() {
      LOGGER.info("Testing address family valueOf");

      assertEquals(IpAddressFamily.IPV4, IpAddressFamily.valueOf("IPV4"), "Should find IPV4");
      assertEquals(IpAddressFamily.IPV6, IpAddressFamily.valueOf("IPV6"), "Should find IPV6");

      assertThrows(
          IllegalArgumentException.class,
          () -> IpAddressFamily.valueOf("INVALID"),
          "Should throw for invalid name");

      LOGGER.info("Address family valueOf verified");
    }
  }

  @Nested
  @DisplayName("Concurrent Socket Address Operations")
  class ConcurrentSocketAddressOperations {

    @Test
    @Timeout(30)
    @DisplayName("should handle concurrent address creation")
    void shouldHandleConcurrentAddressCreation() throws Exception {
      LOGGER.info("Testing concurrent address creation");

      final int threadCount = 10;
      final int addressesPerThread = 100;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch completionLatch = new CountDownLatch(threadCount);
      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicInteger errorCount = new AtomicInteger(0);

      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        executor.submit(
            () -> {
              try {
                startLatch.await();
                for (int i = 0; i < addressesPerThread; i++) {
                  // Create IPv4 address
                  final Ipv4Address ipv4Addr =
                      new Ipv4Address(
                          new byte[] {
                            (byte) (10 + threadId), 0, (byte) (i / 256), (byte) (i % 256)
                          });
                  final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(8080 + i, ipv4Addr);
                  final IpSocketAddress wrapped = IpSocketAddress.ipv4(socketAddr);

                  if (wrapped.isIpv4() && wrapped.getIpv4().getPort() == 8080 + i) {
                    successCount.incrementAndGet();
                  }
                }
              } catch (final Exception e) {
                errorCount.incrementAndGet();
                LOGGER.warning("Concurrent address creation failed: " + e.getMessage());
              } finally {
                completionLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      assertTrue(
          completionLatch.await(25, TimeUnit.SECONDS),
          "All threads should complete within timeout");

      executor.shutdown();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor should terminate");

      final int expectedTotal = threadCount * addressesPerThread;
      assertEquals(expectedTotal, successCount.get(), "All addresses should be created correctly");
      assertEquals(0, errorCount.get(), "No errors should occur");

      LOGGER.info(
          "Concurrent address creation: "
              + successCount.get()
              + " successes, "
              + errorCount.get()
              + " errors");
    }
  }
}
