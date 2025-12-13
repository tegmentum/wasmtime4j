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

package ai.tegmentum.wasmtime4j.jni.wasi.sockets;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddressFamily;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpSocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiUdpSocket.IncomingDatagram;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiUdpSocket.OutgoingDatagram;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Validation tests for JniWasiUdpSocket parameter validation and state management.
 *
 * <p>These tests focus on defensive programming validation without requiring native library
 * loading. They verify that the Java layer correctly validates parameters before making native
 * calls.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/udp@0.2.0
 *
 * @since 1.0.0
 */
@DisplayName("JNI WASI UDP Socket Validation Tests")
public class JniWasiUdpSocketValidationTest {

  private static final Logger LOGGER =
      Logger.getLogger(JniWasiUdpSocketValidationTest.class.getName());

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    LOGGER.info("Starting test: " + testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Completed test: " + testInfo.getDisplayName());
  }

  @Nested
  @DisplayName("Factory Method Validation Tests")
  class FactoryMethodValidationTests {

    @Test
    @DisplayName("Should reject zero context handle")
    void shouldRejectZeroContextHandle() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> JniWasiUdpSocket.create(0L, IpAddressFamily.IPV4));

      assertTrue(exception.getMessage().contains("Context handle"));
      LOGGER.info("Correctly rejected zero context handle: " + exception.getMessage());
    }

    @Test
    @DisplayName("Should reject null address family")
    void shouldRejectNullAddressFamily() {
      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> JniWasiUdpSocket.create(1L, null));

      assertTrue(exception.getMessage().contains("Address family"));
      LOGGER.info("Correctly rejected null address family: " + exception.getMessage());
    }

    @Test
    @DisplayName("Should attempt IPv4 socket creation with valid parameters")
    void shouldAttemptIpv4SocketCreation() {
      // This test validates that parameters pass Java-layer validation.
      // The native layer behavior varies based on the context handle validity.
      // We accept three outcomes:
      // 1. WasmException - native call detected invalid context
      // 2. UnsatisfiedLinkError - native library not loaded
      // 3. Success - native call succeeded (context handle happened to be valid)
      try {
        final JniWasiUdpSocket socket = JniWasiUdpSocket.create(12345L, IpAddressFamily.IPV4);
        // If we get here, the native call succeeded - close the socket to clean up
        LOGGER.info("IPv4 socket created successfully with handle: " + socket);
        socket.close();
      } catch (WasmException e) {
        // Expected - native call failed but Java validation passed
        LOGGER.info("Java validation passed, native call failed as expected: " + e.getMessage());
      } catch (UnsatisfiedLinkError e) {
        // Also acceptable - native library not loaded
        LOGGER.info("Native library not loaded (expected in unit test): " + e.getMessage());
      }
    }

    @Test
    @DisplayName("Should attempt IPv6 socket creation with valid parameters")
    void shouldAttemptIpv6SocketCreation() {
      // Same as IPv4 test - validates Java-layer parameter validation
      try {
        final JniWasiUdpSocket socket = JniWasiUdpSocket.create(12345L, IpAddressFamily.IPV6);
        LOGGER.info("IPv6 socket created successfully with handle: " + socket);
        socket.close();
      } catch (WasmException e) {
        LOGGER.info("Java validation passed, native call failed as expected: " + e.getMessage());
      } catch (UnsatisfiedLinkError e) {
        LOGGER.info("Native library not loaded (expected in unit test): " + e.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Datagram Data Types Tests")
  class DatagramDataTypesTests {

    @Test
    @DisplayName("Should create IPv4 datagram with all octets")
    void shouldCreateIpv4Datagram() {
      final byte[] data = "Test UDP payload".getBytes();
      final Ipv4Address addr = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 100});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(12345, addr);
      final IpSocketAddress remoteAddr = IpSocketAddress.ipv4(socketAddr);

      final IncomingDatagram incoming = new IncomingDatagram(data, remoteAddr);
      assertArrayEquals(data, incoming.getData());
      assertEquals(remoteAddr, incoming.getRemoteAddress());

      final OutgoingDatagram outgoing = new OutgoingDatagram(data, remoteAddr);
      assertArrayEquals(data, outgoing.getData());
      assertEquals(remoteAddr, outgoing.getRemoteAddress());
      assertTrue(outgoing.hasRemoteAddress());

      LOGGER.info("Created IPv4 datagrams successfully");
    }

    @Test
    @DisplayName("Should create IPv6 datagram with all segments")
    void shouldCreateIpv6Datagram() {
      final byte[] data = "Test UDP payload IPv6".getBytes();
      final Ipv6Address addr = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddr = new Ipv6SocketAddress(12345, 0, addr, 0);
      final IpSocketAddress remoteAddr = IpSocketAddress.ipv6(socketAddr);

      final IncomingDatagram incoming = new IncomingDatagram(data, remoteAddr);
      assertArrayEquals(data, incoming.getData());
      assertFalse(incoming.getRemoteAddress().isIpv4());

      final OutgoingDatagram outgoing = new OutgoingDatagram(data, remoteAddr);
      assertArrayEquals(data, outgoing.getData());
      assertFalse(outgoing.getRemoteAddress().isIpv4());

      LOGGER.info("Created IPv6 datagrams successfully");
    }

    @Test
    @DisplayName("Should handle maximum datagram size")
    void shouldHandleMaximumDatagramSize() {
      // UDP max theoretical payload is 65507 bytes (65535 - 8 UDP header - 20 IP header)
      final int maxSize = 65507;
      final byte[] data = new byte[maxSize];
      for (int i = 0; i < maxSize; i++) {
        data[i] = (byte) (i % 256);
      }

      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(8080, addr);
      final IpSocketAddress remoteAddr = IpSocketAddress.ipv4(socketAddr);

      final OutgoingDatagram datagram = new OutgoingDatagram(data, remoteAddr);
      assertEquals(maxSize, datagram.getData().length);

      LOGGER.info("Handled maximum datagram size: " + maxSize + " bytes");
    }

    @Test
    @DisplayName("Should handle empty datagram")
    void shouldHandleEmptyDatagram() {
      final byte[] data = new byte[0];
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(8080, addr);
      final IpSocketAddress remoteAddr = IpSocketAddress.ipv4(socketAddr);

      final OutgoingDatagram datagram = new OutgoingDatagram(data, remoteAddr);
      assertEquals(0, datagram.getData().length);

      LOGGER.info("Handled empty datagram successfully");
    }

    @Test
    @DisplayName("Should support connected socket datagram without address")
    void shouldSupportConnectedSocketDatagram() {
      final byte[] data = "Connected UDP message".getBytes();

      final OutgoingDatagram datagram = new OutgoingDatagram(data);
      assertArrayEquals(data, datagram.getData());
      assertNull(datagram.getRemoteAddress());
      assertFalse(datagram.hasRemoteAddress());

      LOGGER.info("Created connected socket datagram without address");
    }
  }

  @Nested
  @DisplayName("Address Encoding Tests")
  class AddressEncodingTests {

    @Test
    @DisplayName("Should handle loopback IPv4 address")
    void shouldHandleLoopbackIpv4() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      assertEquals("127.0.0.1", addr.toString());

      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(8080, addr);
      assertEquals("127.0.0.1:8080", socketAddr.toString());

      LOGGER.info("Handled loopback IPv4 address correctly");
    }

    @Test
    @DisplayName("Should handle all zeros IPv4 address")
    void shouldHandleAllZerosIpv4() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {0, 0, 0, 0});
      assertEquals("0.0.0.0", addr.toString());

      LOGGER.info("Handled all zeros IPv4 address correctly");
    }

    @Test
    @DisplayName("Should handle broadcast IPv4 address")
    void shouldHandleBroadcastIpv4() {
      final Ipv4Address addr =
          new Ipv4Address(new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255});
      assertEquals("255.255.255.255", addr.toString());

      LOGGER.info("Handled broadcast IPv4 address correctly");
    }

    @Test
    @DisplayName("Should handle loopback IPv6 address")
    void shouldHandleLoopbackIpv6() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      String str = addr.toString();
      assertTrue(str.endsWith(":1"));

      LOGGER.info("Handled loopback IPv6 address: " + str);
    }

    @Test
    @DisplayName("Should handle link-local IPv6 address with scope ID")
    void shouldHandleLinkLocalIpv6WithScopeId() {
      final Ipv6Address addr = new Ipv6Address(new short[] {(short) 0xfe80, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddr = new Ipv6SocketAddress(8080, 0, addr, 2);

      assertEquals(2, socketAddr.getScopeId());
      assertEquals(0, socketAddr.getFlowInfo());

      LOGGER.info("Handled link-local IPv6 with scope ID: " + socketAddr);
    }

    @Test
    @DisplayName("Should handle IPv6 with flow info")
    void shouldHandleIpv6WithFlowInfo() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddr = new Ipv6SocketAddress(8080, 123456, addr, 0);

      assertEquals(123456, socketAddr.getFlowInfo());

      LOGGER.info("Handled IPv6 with flow info: " + socketAddr);
    }
  }

  @Nested
  @DisplayName("Port Range Tests")
  class PortRangeTests {

    @Test
    @DisplayName("Should accept ephemeral port 0")
    void shouldAcceptEphemeralPort() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(0, addr);

      assertEquals(0, socketAddr.getPort());
      LOGGER.info("Accepted ephemeral port 0");
    }

    @Test
    @DisplayName("Should accept system ports 1-1023")
    void shouldAcceptSystemPorts() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});

      final Ipv4SocketAddress port1 = new Ipv4SocketAddress(1, addr);
      assertEquals(1, port1.getPort());

      final Ipv4SocketAddress port80 = new Ipv4SocketAddress(80, addr);
      assertEquals(80, port80.getPort());

      final Ipv4SocketAddress port443 = new Ipv4SocketAddress(443, addr);
      assertEquals(443, port443.getPort());

      final Ipv4SocketAddress port1023 = new Ipv4SocketAddress(1023, addr);
      assertEquals(1023, port1023.getPort());

      LOGGER.info("Accepted system ports 1-1023");
    }

    @Test
    @DisplayName("Should accept registered ports 1024-49151")
    void shouldAcceptRegisteredPorts() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});

      final Ipv4SocketAddress port1024 = new Ipv4SocketAddress(1024, addr);
      assertEquals(1024, port1024.getPort());

      final Ipv4SocketAddress port8080 = new Ipv4SocketAddress(8080, addr);
      assertEquals(8080, port8080.getPort());

      final Ipv4SocketAddress port49151 = new Ipv4SocketAddress(49151, addr);
      assertEquals(49151, port49151.getPort());

      LOGGER.info("Accepted registered ports 1024-49151");
    }

    @Test
    @DisplayName("Should accept dynamic ports 49152-65535")
    void shouldAcceptDynamicPorts() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});

      final Ipv4SocketAddress port49152 = new Ipv4SocketAddress(49152, addr);
      assertEquals(49152, port49152.getPort());

      final Ipv4SocketAddress port65535 = new Ipv4SocketAddress(65535, addr);
      assertEquals(65535, port65535.getPort());

      LOGGER.info("Accepted dynamic ports 49152-65535");
    }

    @Test
    @DisplayName("Should reject negative ports")
    void shouldRejectNegativePorts() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});

      assertThrows(IllegalArgumentException.class, () -> new Ipv4SocketAddress(-1, addr));
      assertThrows(IllegalArgumentException.class, () -> new Ipv4SocketAddress(-65535, addr));
      assertThrows(
          IllegalArgumentException.class, () -> new Ipv4SocketAddress(Integer.MIN_VALUE, addr));

      LOGGER.info("Correctly rejected negative ports");
    }

    @Test
    @DisplayName("Should reject ports above 65535")
    void shouldRejectPortsAboveMax() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});

      assertThrows(IllegalArgumentException.class, () -> new Ipv4SocketAddress(65536, addr));
      assertThrows(IllegalArgumentException.class, () -> new Ipv4SocketAddress(100000, addr));
      assertThrows(
          IllegalArgumentException.class, () -> new Ipv4SocketAddress(Integer.MAX_VALUE, addr));

      LOGGER.info("Correctly rejected ports above 65535");
    }
  }

  @Nested
  @DisplayName("Multiple Datagram Tests")
  class MultipleDatagramTests {

    @Test
    @DisplayName("Should handle array of outgoing datagrams")
    void shouldHandleArrayOfOutgoingDatagrams() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 100});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(8080, addr);
      final IpSocketAddress remoteAddr = IpSocketAddress.ipv4(socketAddr);

      final OutgoingDatagram[] datagrams = new OutgoingDatagram[10];
      for (int i = 0; i < 10; i++) {
        final byte[] data = ("Message " + i).getBytes();
        datagrams[i] = new OutgoingDatagram(data, remoteAddr);
      }

      assertEquals(10, datagrams.length);
      for (int i = 0; i < 10; i++) {
        assertNotNull(datagrams[i]);
        assertTrue(datagrams[i].hasRemoteAddress());
      }

      LOGGER.info("Handled array of 10 outgoing datagrams");
    }

    @Test
    @DisplayName("Should handle array of incoming datagrams with different sources")
    void shouldHandleArrayOfIncomingDatagrams() {
      final IncomingDatagram[] datagrams = new IncomingDatagram[5];
      for (int i = 0; i < 5; i++) {
        final byte[] data = ("Response " + i).getBytes();
        final Ipv4Address addr =
            new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, (byte) (100 + i)});
        final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(10000 + i, addr);
        final IpSocketAddress remoteAddr = IpSocketAddress.ipv4(socketAddr);
        datagrams[i] = new IncomingDatagram(data, remoteAddr);
      }

      assertEquals(5, datagrams.length);
      for (int i = 0; i < 5; i++) {
        assertNotNull(datagrams[i]);
        final Ipv4SocketAddress socketAddr = datagrams[i].getRemoteAddress().getIpv4();
        assertEquals(100 + i, socketAddr.getAddress().getOctets()[3] & 0xFF);
        assertEquals(10000 + i, socketAddr.getPort());
      }

      LOGGER.info("Handled array of 5 incoming datagrams with different sources");
    }

    @Test
    @DisplayName("Should handle mixed IPv4 and IPv6 datagrams")
    void shouldHandleMixedIpv4AndIpv6Datagrams() {
      final OutgoingDatagram[] datagrams = new OutgoingDatagram[4];

      // IPv4 datagrams
      final Ipv4Address ipv4Addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress ipv4SocketAddr = new Ipv4SocketAddress(8080, ipv4Addr);
      final IpSocketAddress ipv4RemoteAddr = IpSocketAddress.ipv4(ipv4SocketAddr);
      datagrams[0] = new OutgoingDatagram("IPv4 message 1".getBytes(), ipv4RemoteAddr);
      datagrams[1] = new OutgoingDatagram("IPv4 message 2".getBytes(), ipv4RemoteAddr);

      // IPv6 datagrams
      final Ipv6Address ipv6Addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress ipv6SocketAddr = new Ipv6SocketAddress(8080, 0, ipv6Addr, 0);
      final IpSocketAddress ipv6RemoteAddr = IpSocketAddress.ipv6(ipv6SocketAddr);
      datagrams[2] = new OutgoingDatagram("IPv6 message 1".getBytes(), ipv6RemoteAddr);
      datagrams[3] = new OutgoingDatagram("IPv6 message 2".getBytes(), ipv6RemoteAddr);

      assertTrue(datagrams[0].getRemoteAddress().isIpv4());
      assertTrue(datagrams[1].getRemoteAddress().isIpv4());
      assertFalse(datagrams[2].getRemoteAddress().isIpv4());
      assertFalse(datagrams[3].getRemoteAddress().isIpv4());

      LOGGER.info("Handled mixed IPv4 and IPv6 datagrams");
    }
  }
}
