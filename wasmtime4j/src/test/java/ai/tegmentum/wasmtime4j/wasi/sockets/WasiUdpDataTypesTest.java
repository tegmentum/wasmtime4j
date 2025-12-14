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

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.wasi.sockets.WasiUdpSocket.IncomingDatagram;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiUdpSocket.OutgoingDatagram;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive tests for WASI UDP socket data types.
 *
 * <p>Tests cover IPv4/IPv6 addresses, socket addresses, and datagram classes used in UDP socket
 * operations. These tests validate constructors, getters, equals, hashCode, toString, and defensive
 * copying behavior.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/udp@0.2.0
 *
 * @since 1.0.0
 */
@DisplayName("WASI UDP Data Types Tests")
public class WasiUdpDataTypesTest {

  private static final Logger LOGGER = Logger.getLogger(WasiUdpDataTypesTest.class.getName());

  @BeforeEach
  void setUp(final TestInfo testInfo) {
    LOGGER.info("Starting test: " + testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Completed test: " + testInfo.getDisplayName());
  }

  @Nested
  @DisplayName("Ipv4Address Tests")
  class Ipv4AddressTests {

    @Test
    @DisplayName("Should create IPv4 address with valid octets")
    void shouldCreateWithValidOctets() {
      final byte[] octets = new byte[] {(byte) 192, (byte) 168, 1, 1};
      final Ipv4Address address = new Ipv4Address(octets);

      assertNotNull(address);
      assertArrayEquals(octets, address.getOctets());
      LOGGER.info("Created IPv4 address: " + address);
    }

    @Test
    @DisplayName("Should create loopback IPv4 address")
    void shouldCreateLoopbackAddress() {
      final byte[] octets = new byte[] {127, 0, 0, 1};
      final Ipv4Address address = new Ipv4Address(octets);

      assertEquals("127.0.0.1", address.toString());
      LOGGER.info("Created loopback IPv4 address: " + address);
    }

    @Test
    @DisplayName("Should create broadcast IPv4 address")
    void shouldCreateBroadcastAddress() {
      final byte[] octets = new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255};
      final Ipv4Address address = new Ipv4Address(octets);

      assertEquals("255.255.255.255", address.toString());
      LOGGER.info("Created broadcast IPv4 address: " + address);
    }

    @Test
    @DisplayName("Should defensively copy octets on construction")
    void shouldDefensivelyCopyOctetsOnConstruction() {
      final byte[] octets = new byte[] {(byte) 192, (byte) 168, 1, 1};
      final Ipv4Address address = new Ipv4Address(octets);

      // Modify original array
      octets[0] = 10;

      // Address should not be affected
      final byte[] retrieved = address.getOctets();
      assertEquals((byte) 192, retrieved[0]);
      LOGGER.info("Verified defensive copy on construction");
    }

    @Test
    @DisplayName("Should defensively copy octets on retrieval")
    void shouldDefensivelyCopyOctetsOnRetrieval() {
      final byte[] octets = new byte[] {(byte) 192, (byte) 168, 1, 1};
      final Ipv4Address address = new Ipv4Address(octets);

      // Modify retrieved array
      final byte[] retrieved = address.getOctets();
      retrieved[0] = 10;

      // Address should not be affected
      final byte[] retrievedAgain = address.getOctets();
      assertEquals((byte) 192, retrievedAgain[0]);
      LOGGER.info("Verified defensive copy on retrieval");
    }

    @Test
    @DisplayName("Should reject null octets")
    void shouldRejectNullOctets() {
      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> new Ipv4Address(null));

      assertTrue(exception.getMessage().contains("null"));
      LOGGER.info("Correctly rejected null octets");
    }

    @Test
    @DisplayName("Should reject octets with wrong length")
    void shouldRejectOctetsWithWrongLength() {
      final byte[] tooShort = new byte[] {127, 0, 0};
      final IllegalArgumentException exception1 =
          assertThrows(IllegalArgumentException.class, () -> new Ipv4Address(tooShort));
      assertTrue(exception1.getMessage().contains("4"));

      final byte[] tooLong = new byte[] {127, 0, 0, 1, 0};
      final IllegalArgumentException exception2 =
          assertThrows(IllegalArgumentException.class, () -> new Ipv4Address(tooLong));
      assertTrue(exception2.getMessage().contains("4"));

      LOGGER.info("Correctly rejected invalid length octets");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      final Ipv4Address addr1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address addr2 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address addr3 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 2});

      assertEquals(addr1, addr2);
      assertNotEquals(addr1, addr3);
      assertEquals(addr1, addr1); // reflexive
      assertEquals(addr2, addr1); // symmetric
      assertNotEquals(addr1, null);
      assertNotEquals(addr1, "192.168.1.1");
      LOGGER.info("Verified equals implementation");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      final Ipv4Address addr1 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4Address addr2 = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});

      assertEquals(addr1.hashCode(), addr2.hashCode());
      LOGGER.info("Verified hashCode implementation");
    }

    @Test
    @DisplayName("Should format toString correctly")
    void shouldFormatToStringCorrectly() {
      final Ipv4Address address = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 100});

      assertEquals("192.168.1.100", address.toString());
      LOGGER.info("Verified toString format: " + address);
    }
  }

  @Nested
  @DisplayName("Ipv6Address Tests")
  class Ipv6AddressTests {

    @Test
    @DisplayName("Should create IPv6 address with valid segments")
    void shouldCreateWithValidSegments() {
      final short[] segments = new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1};
      final Ipv6Address address = new Ipv6Address(segments);

      assertNotNull(address);
      assertArrayEquals(segments, address.getSegments());
      LOGGER.info("Created IPv6 address: " + address);
    }

    @Test
    @DisplayName("Should create loopback IPv6 address")
    void shouldCreateLoopbackAddress() {
      final short[] segments = new short[] {0, 0, 0, 0, 0, 0, 0, 1};
      final Ipv6Address address = new Ipv6Address(segments);

      assertNotNull(address);
      assertEquals("0:0:0:0:0:0:0:1", address.toString());
      LOGGER.info("Created loopback IPv6 address: " + address);
    }

    @Test
    @DisplayName("Should defensively copy segments on construction")
    void shouldDefensivelyCopySegmentsOnConstruction() {
      final short[] segments = new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1};
      final Ipv6Address address = new Ipv6Address(segments);

      // Modify original array
      segments[0] = (short) 0xfe80;

      // Address should not be affected
      final short[] retrieved = address.getSegments();
      assertEquals((short) 0x2001, retrieved[0]);
      LOGGER.info("Verified defensive copy on construction");
    }

    @Test
    @DisplayName("Should defensively copy segments on retrieval")
    void shouldDefensivelyCopySegmentsOnRetrieval() {
      final short[] segments = new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1};
      final Ipv6Address address = new Ipv6Address(segments);

      // Modify retrieved array
      final short[] retrieved = address.getSegments();
      retrieved[0] = (short) 0xfe80;

      // Address should not be affected
      final short[] retrievedAgain = address.getSegments();
      assertEquals((short) 0x2001, retrievedAgain[0]);
      LOGGER.info("Verified defensive copy on retrieval");
    }

    @Test
    @DisplayName("Should reject null segments")
    void shouldRejectNullSegments() {
      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> new Ipv6Address(null));

      assertTrue(exception.getMessage().contains("null"));
      LOGGER.info("Correctly rejected null segments");
    }

    @Test
    @DisplayName("Should reject segments with wrong length")
    void shouldRejectSegmentsWithWrongLength() {
      final short[] tooShort = new short[] {0, 0, 0, 0, 0, 0, 1};
      final IllegalArgumentException exception1 =
          assertThrows(IllegalArgumentException.class, () -> new Ipv6Address(tooShort));
      assertTrue(exception1.getMessage().contains("8"));

      final short[] tooLong = new short[] {0, 0, 0, 0, 0, 0, 0, 0, 1};
      final IllegalArgumentException exception2 =
          assertThrows(IllegalArgumentException.class, () -> new Ipv6Address(tooLong));
      assertTrue(exception2.getMessage().contains("8"));

      LOGGER.info("Correctly rejected invalid length segments");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      final Ipv6Address addr1 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address addr2 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address addr3 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 2});

      assertEquals(addr1, addr2);
      assertNotEquals(addr1, addr3);
      assertEquals(addr1, addr1); // reflexive
      assertEquals(addr2, addr1); // symmetric
      assertNotEquals(addr1, null);
      LOGGER.info("Verified equals implementation");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      final Ipv6Address addr1 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6Address addr2 = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});

      assertEquals(addr1.hashCode(), addr2.hashCode());
      LOGGER.info("Verified hashCode implementation");
    }

    @Test
    @DisplayName("Should format toString correctly with hexadecimal")
    void shouldFormatToStringCorrectly() {
      final Ipv6Address address =
          new Ipv6Address(new short[] {0x2001, 0x0db8, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 1});

      String str = address.toString();
      assertTrue(str.contains("2001"));
      assertTrue(str.contains("db8"));
      LOGGER.info("Verified toString format: " + address);
    }
  }

  @Nested
  @DisplayName("Ipv4SocketAddress Tests")
  class Ipv4SocketAddressTests {

    @Test
    @DisplayName("Should create IPv4 socket address with valid port and address")
    void shouldCreateWithValidPortAndAddress() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(8080, addr);

      assertEquals(8080, socketAddr.getPort());
      assertEquals(addr, socketAddr.getAddress());
      LOGGER.info("Created IPv4 socket address: " + socketAddr);
    }

    @Test
    @DisplayName("Should accept port 0 (ephemeral)")
    void shouldAcceptPortZero() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(0, addr);

      assertEquals(0, socketAddr.getPort());
      LOGGER.info("Accepted ephemeral port 0");
    }

    @Test
    @DisplayName("Should accept port 65535 (max)")
    void shouldAcceptMaxPort() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(65535, addr);

      assertEquals(65535, socketAddr.getPort());
      LOGGER.info("Accepted max port 65535");
    }

    @Test
    @DisplayName("Should reject negative port")
    void shouldRejectNegativePort() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});

      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> new Ipv4SocketAddress(-1, addr));

      assertTrue(exception.getMessage().contains("Port"));
      LOGGER.info("Correctly rejected negative port");
    }

    @Test
    @DisplayName("Should reject port above 65535")
    void shouldRejectPortAboveMax() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});

      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> new Ipv4SocketAddress(65536, addr));

      assertTrue(exception.getMessage().contains("Port"));
      LOGGER.info("Correctly rejected port above max");
    }

    @Test
    @DisplayName("Should reject null address")
    void shouldRejectNullAddress() {
      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> new Ipv4SocketAddress(8080, null));

      assertTrue(exception.getMessage().contains("null"));
      LOGGER.info("Correctly rejected null address");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr1 = new Ipv4SocketAddress(8080, addr);
      final Ipv4SocketAddress socketAddr2 = new Ipv4SocketAddress(8080, addr);
      final Ipv4SocketAddress socketAddr3 = new Ipv4SocketAddress(9090, addr);

      assertEquals(socketAddr1, socketAddr2);
      assertNotEquals(socketAddr1, socketAddr3);
      LOGGER.info("Verified equals implementation");
    }

    @Test
    @DisplayName("Should format toString correctly")
    void shouldFormatToStringCorrectly() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(8080, addr);

      assertEquals("192.168.1.1:8080", socketAddr.toString());
      LOGGER.info("Verified toString format: " + socketAddr);
    }
  }

  @Nested
  @DisplayName("Ipv6SocketAddress Tests")
  class Ipv6SocketAddressTests {

    @Test
    @DisplayName("Should create IPv6 socket address with all fields")
    void shouldCreateWithAllFields() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0x2001, 0x0db8, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddr = new Ipv6SocketAddress(8080, 0, addr, 0);

      assertEquals(8080, socketAddr.getPort());
      assertEquals(0, socketAddr.getFlowInfo());
      assertEquals(addr, socketAddr.getAddress());
      assertEquals(0, socketAddr.getScopeId());
      LOGGER.info("Created IPv6 socket address: " + socketAddr);
    }

    @Test
    @DisplayName("Should create link-local IPv6 address with scope ID")
    void shouldCreateLinkLocalWithScopeId() {
      final Ipv6Address addr = new Ipv6Address(new short[] {(short) 0xfe80, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddr = new Ipv6SocketAddress(8080, 0, addr, 1);

      assertEquals(1, socketAddr.getScopeId());
      LOGGER.info("Created link-local IPv6 socket address with scope ID: " + socketAddr);
    }

    @Test
    @DisplayName("Should accept port 0 and 65535")
    void shouldAcceptBoundaryPorts() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});

      final Ipv6SocketAddress socketAddr0 = new Ipv6SocketAddress(0, 0, addr, 0);
      assertEquals(0, socketAddr0.getPort());

      final Ipv6SocketAddress socketAddr65535 = new Ipv6SocketAddress(65535, 0, addr, 0);
      assertEquals(65535, socketAddr65535.getPort());

      LOGGER.info("Accepted boundary ports");
    }

    @Test
    @DisplayName("Should reject invalid ports")
    void shouldRejectInvalidPorts() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});

      assertThrows(IllegalArgumentException.class, () -> new Ipv6SocketAddress(-1, 0, addr, 0));
      assertThrows(IllegalArgumentException.class, () -> new Ipv6SocketAddress(65536, 0, addr, 0));

      LOGGER.info("Correctly rejected invalid ports");
    }

    @Test
    @DisplayName("Should reject null address")
    void shouldRejectNullAddress() {
      assertThrows(IllegalArgumentException.class, () -> new Ipv6SocketAddress(8080, 0, null, 0));

      LOGGER.info("Correctly rejected null address");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddr1 = new Ipv6SocketAddress(8080, 10, addr, 1);
      final Ipv6SocketAddress socketAddr2 = new Ipv6SocketAddress(8080, 10, addr, 1);
      final Ipv6SocketAddress socketAddr3 = new Ipv6SocketAddress(8080, 10, addr, 2);

      assertEquals(socketAddr1, socketAddr2);
      assertNotEquals(socketAddr1, socketAddr3);
      LOGGER.info("Verified equals implementation");
    }

    @Test
    @DisplayName("Should format toString with brackets")
    void shouldFormatToStringWithBrackets() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress socketAddr = new Ipv6SocketAddress(8080, 0, addr, 0);

      String str = socketAddr.toString();
      assertTrue(str.startsWith("["));
      assertTrue(str.contains("]:8080"));
      LOGGER.info("Verified toString format: " + socketAddr);
    }
  }

  @Nested
  @DisplayName("IpSocketAddress Tests")
  class IpSocketAddressTests {

    @Test
    @DisplayName("Should create IPv4 variant")
    void shouldCreateIpv4Variant() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress ipv4SocketAddr = new Ipv4SocketAddress(8080, addr);
      final IpSocketAddress socketAddr = IpSocketAddress.ipv4(ipv4SocketAddr);

      assertTrue(socketAddr.isIpv4());
      assertEquals(ipv4SocketAddr, socketAddr.getIpv4());
      LOGGER.info("Created IPv4 variant: " + socketAddr);
    }

    @Test
    @DisplayName("Should create IPv6 variant")
    void shouldCreateIpv6Variant() {
      final Ipv6Address addr = new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final Ipv6SocketAddress ipv6SocketAddr = new Ipv6SocketAddress(8080, 0, addr, 0);
      final IpSocketAddress socketAddr = IpSocketAddress.ipv6(ipv6SocketAddr);

      assertFalse(socketAddr.isIpv4());
      assertEquals(ipv6SocketAddr, socketAddr.getIpv6());
      LOGGER.info("Created IPv6 variant: " + socketAddr);
    }

    @Test
    @DisplayName("Should reject null IPv4 address")
    void shouldRejectNullIpv4Address() {
      assertThrows(IllegalArgumentException.class, () -> IpSocketAddress.ipv4(null));

      LOGGER.info("Correctly rejected null IPv4 address");
    }

    @Test
    @DisplayName("Should reject null IPv6 address")
    void shouldRejectNullIpv6Address() {
      assertThrows(IllegalArgumentException.class, () -> IpSocketAddress.ipv6(null));

      LOGGER.info("Correctly rejected null IPv6 address");
    }

    @Test
    @DisplayName("Should throw when getting wrong address type")
    void shouldThrowWhenGettingWrongAddressType() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress ipv4SocketAddr = new Ipv4SocketAddress(8080, addr);
      final IpSocketAddress socketAddr = IpSocketAddress.ipv4(ipv4SocketAddr);

      assertThrows(IllegalStateException.class, socketAddr::getIpv6);
      LOGGER.info("Correctly threw when accessing wrong address type");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress ipv4SocketAddr = new Ipv4SocketAddress(8080, addr);

      final IpSocketAddress socketAddr1 = IpSocketAddress.ipv4(ipv4SocketAddr);
      final IpSocketAddress socketAddr2 = IpSocketAddress.ipv4(ipv4SocketAddr);

      assertEquals(socketAddr1, socketAddr2);
      LOGGER.info("Verified equals implementation");
    }
  }

  @Nested
  @DisplayName("IncomingDatagram Tests")
  class IncomingDatagramTests {

    @Test
    @DisplayName("Should create IncomingDatagram with valid data and address")
    void shouldCreateWithValidDataAndAddress() {
      final byte[] data = "Hello, UDP!".getBytes(StandardCharsets.UTF_8);
      final Ipv4Address addr = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 100});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(12345, addr);
      final IpSocketAddress remoteAddr = IpSocketAddress.ipv4(socketAddr);

      final IncomingDatagram datagram = new IncomingDatagram(data, remoteAddr);

      assertArrayEquals(data, datagram.getData());
      assertEquals(remoteAddr, datagram.getRemoteAddress());
      LOGGER.info("Created IncomingDatagram with " + data.length + " bytes from " + remoteAddr);
    }

    @Test
    @DisplayName("Should create IncomingDatagram with empty data")
    void shouldCreateWithEmptyData() {
      final byte[] data = new byte[0];
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(8080, addr);
      final IpSocketAddress remoteAddr = IpSocketAddress.ipv4(socketAddr);

      final IncomingDatagram datagram = new IncomingDatagram(data, remoteAddr);

      assertEquals(0, datagram.getData().length);
      LOGGER.info("Created IncomingDatagram with empty data");
    }

    @Test
    @DisplayName("Should defensively copy data on construction")
    void shouldDefensivelyCopyDataOnConstruction() {
      final byte[] data = "Original".getBytes(StandardCharsets.UTF_8);
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(8080, addr);
      final IpSocketAddress remoteAddr = IpSocketAddress.ipv4(socketAddr);

      final IncomingDatagram datagram = new IncomingDatagram(data, remoteAddr);

      // Modify original array
      data[0] = 'X';

      // Datagram should not be affected
      final byte[] retrieved = datagram.getData();
      assertEquals('O', retrieved[0]);
      LOGGER.info("Verified defensive copy on construction");
    }

    @Test
    @DisplayName("Should defensively copy data on retrieval")
    void shouldDefensivelyCopyDataOnRetrieval() {
      final byte[] data = "Original".getBytes(StandardCharsets.UTF_8);
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(8080, addr);
      final IpSocketAddress remoteAddr = IpSocketAddress.ipv4(socketAddr);

      final IncomingDatagram datagram = new IncomingDatagram(data, remoteAddr);

      // Modify retrieved array
      final byte[] retrieved = datagram.getData();
      retrieved[0] = 'X';

      // Datagram should not be affected
      final byte[] retrievedAgain = datagram.getData();
      assertEquals('O', retrievedAgain[0]);
      LOGGER.info("Verified defensive copy on retrieval");
    }

    @Test
    @DisplayName("Should reject null data")
    void shouldRejectNullData() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(8080, addr);
      final IpSocketAddress remoteAddr = IpSocketAddress.ipv4(socketAddr);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> new IncomingDatagram(null, remoteAddr));

      assertTrue(exception.getMessage().contains("data"));
      LOGGER.info("Correctly rejected null data");
    }

    @Test
    @DisplayName("Should reject null remote address")
    void shouldRejectNullRemoteAddress() {
      final byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);

      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> new IncomingDatagram(data, null));

      assertTrue(exception.getMessage().contains("remoteAddress"));
      LOGGER.info("Correctly rejected null remote address");
    }
  }

  @Nested
  @DisplayName("OutgoingDatagram Tests")
  class OutgoingDatagramTests {

    @Test
    @DisplayName("Should create OutgoingDatagram with data and address")
    void shouldCreateWithDataAndAddress() {
      final byte[] data = "Hello, UDP!".getBytes(StandardCharsets.UTF_8);
      final Ipv4Address addr = new Ipv4Address(new byte[] {(byte) 192, (byte) 168, 1, 100});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(12345, addr);
      final IpSocketAddress remoteAddr = IpSocketAddress.ipv4(socketAddr);

      final OutgoingDatagram datagram = new OutgoingDatagram(data, remoteAddr);

      assertArrayEquals(data, datagram.getData());
      assertEquals(remoteAddr, datagram.getRemoteAddress());
      assertTrue(datagram.hasRemoteAddress());
      LOGGER.info("Created OutgoingDatagram with " + data.length + " bytes to " + remoteAddr);
    }

    @Test
    @DisplayName("Should create OutgoingDatagram with data only (for connected socket)")
    void shouldCreateWithDataOnly() {
      final byte[] data = "Hello, connected UDP!".getBytes(StandardCharsets.UTF_8);

      final OutgoingDatagram datagram = new OutgoingDatagram(data);

      assertArrayEquals(data, datagram.getData());
      assertNull(datagram.getRemoteAddress());
      assertFalse(datagram.hasRemoteAddress());
      LOGGER.info("Created OutgoingDatagram without remote address");
    }

    @Test
    @DisplayName("Should create OutgoingDatagram with empty data")
    void shouldCreateWithEmptyData() {
      final byte[] data = new byte[0];

      final OutgoingDatagram datagram = new OutgoingDatagram(data);

      assertEquals(0, datagram.getData().length);
      LOGGER.info("Created OutgoingDatagram with empty data");
    }

    @Test
    @DisplayName("Should defensively copy data on construction")
    void shouldDefensivelyCopyDataOnConstruction() {
      final byte[] data = "Original".getBytes(StandardCharsets.UTF_8);

      final OutgoingDatagram datagram = new OutgoingDatagram(data);

      // Modify original array
      data[0] = 'X';

      // Datagram should not be affected
      final byte[] retrieved = datagram.getData();
      assertEquals('O', retrieved[0]);
      LOGGER.info("Verified defensive copy on construction");
    }

    @Test
    @DisplayName("Should defensively copy data on retrieval")
    void shouldDefensivelyCopyDataOnRetrieval() {
      final byte[] data = "Original".getBytes(StandardCharsets.UTF_8);

      final OutgoingDatagram datagram = new OutgoingDatagram(data);

      // Modify retrieved array
      final byte[] retrieved = datagram.getData();
      retrieved[0] = 'X';

      // Datagram should not be affected
      final byte[] retrievedAgain = datagram.getData();
      assertEquals('O', retrievedAgain[0]);
      LOGGER.info("Verified defensive copy on retrieval");
    }

    @Test
    @DisplayName("Should reject null data in constructor with address")
    void shouldRejectNullDataWithAddress() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(8080, addr);
      final IpSocketAddress remoteAddr = IpSocketAddress.ipv4(socketAddr);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> new OutgoingDatagram(null, remoteAddr));

      assertTrue(exception.getMessage().contains("data"));
      LOGGER.info("Correctly rejected null data");
    }

    @Test
    @DisplayName("Should reject null data in constructor without address")
    void shouldRejectNullDataWithoutAddress() {
      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> new OutgoingDatagram(null));

      assertTrue(exception.getMessage().contains("data"));
      LOGGER.info("Correctly rejected null data");
    }

    @Test
    @DisplayName("Should allow null remote address in constructor with address")
    void shouldAllowNullRemoteAddress() {
      final byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);

      final OutgoingDatagram datagram = new OutgoingDatagram(data, null);

      assertFalse(datagram.hasRemoteAddress());
      assertNull(datagram.getRemoteAddress());
      LOGGER.info("Allowed null remote address");
    }
  }

  @Nested
  @DisplayName("IpAddressFamily Tests")
  class IpAddressFamilyTests {

    @Test
    @DisplayName("Should have IPV4 and IPV6 values")
    void shouldHaveIpv4AndIpv6Values() {
      assertEquals(2, IpAddressFamily.values().length);
      assertNotNull(IpAddressFamily.IPV4);
      assertNotNull(IpAddressFamily.IPV6);
      LOGGER.info("Verified IpAddressFamily enum values");
    }

    @Test
    @DisplayName("Should support valueOf for IPV4")
    void shouldSupportValueOfForIpv4() {
      assertEquals(IpAddressFamily.IPV4, IpAddressFamily.valueOf("IPV4"));
      LOGGER.info("Verified valueOf for IPV4");
    }

    @Test
    @DisplayName("Should support valueOf for IPV6")
    void shouldSupportValueOfForIpv6() {
      assertEquals(IpAddressFamily.IPV6, IpAddressFamily.valueOf("IPV6"));
      LOGGER.info("Verified valueOf for IPV6");
    }
  }
}
