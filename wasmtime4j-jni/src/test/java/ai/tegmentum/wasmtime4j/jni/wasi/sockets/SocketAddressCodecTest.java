package ai.tegmentum.wasmtime4j.jni.wasi.sockets;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.wasi.sockets.IpSocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6SocketAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SocketAddressCodec}.
 *
 * <p>Verifies IPv4 and IPv6 address encoding to byte arrays, decoding from long arrays, and
 * round-trip encode-decode consistency. This test must reside in the same package since
 * SocketAddressCodec is package-private.
 */
@DisplayName("SocketAddressCodec Tests")
class SocketAddressCodecTest {

  @Nested
  @DisplayName("IPv4 Encoding")
  class Ipv4Encoding {

    @Test
    @DisplayName("Should encode 127.0.0.1:80 correctly")
    void shouldEncodeLoopbackAddress() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {127, 0, 0, 1});
      final IpSocketAddress socketAddr = IpSocketAddress.ipv4(new Ipv4SocketAddress(80, addr));

      final SocketAddressCodec.AddressParams params = SocketAddressCodec.encodeAddress(socketAddr);

      assertThat(params.isIpv4).as("Should be marked as IPv4").isTrue();
      assertThat(params.ipv4Octets).as("Should have 4 octets").hasSize(4);
      assertThat(params.ipv4Octets[0]).isEqualTo((byte) 127);
      assertThat(params.ipv4Octets[1]).isEqualTo((byte) 0);
      assertThat(params.ipv4Octets[2]).isEqualTo((byte) 0);
      assertThat(params.ipv4Octets[3]).isEqualTo((byte) 1);
      assertThat(params.port).isEqualTo(80);
      assertThat(params.ipv6Segments).as("IPv6 segments should be null for IPv4").isNull();
      assertThat(params.flowInfo).isZero();
      assertThat(params.scopeId).isZero();
    }

    @Test
    @DisplayName("Should encode 255.255.255.255:65535 (boundary values)")
    void shouldEncodeBoundaryValues() {
      final Ipv4Address addr =
          new Ipv4Address(new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255});
      final IpSocketAddress socketAddr = IpSocketAddress.ipv4(new Ipv4SocketAddress(65535, addr));

      final SocketAddressCodec.AddressParams params = SocketAddressCodec.encodeAddress(socketAddr);

      assertThat(params.isIpv4).isTrue();
      assertThat(params.ipv4Octets[0]).isEqualTo((byte) 255);
      assertThat(params.ipv4Octets[1]).isEqualTo((byte) 255);
      assertThat(params.ipv4Octets[2]).isEqualTo((byte) 255);
      assertThat(params.ipv4Octets[3]).isEqualTo((byte) 255);
      assertThat(params.port).isEqualTo(65535);
    }

    @Test
    @DisplayName("Should encode 0.0.0.0:0 (zero values)")
    void shouldEncodeZeroValues() {
      final Ipv4Address addr = new Ipv4Address(new byte[] {0, 0, 0, 0});
      final IpSocketAddress socketAddr = IpSocketAddress.ipv4(new Ipv4SocketAddress(0, addr));

      final SocketAddressCodec.AddressParams params = SocketAddressCodec.encodeAddress(socketAddr);

      assertThat(params.isIpv4).isTrue();
      assertThat(params.ipv4Octets).containsExactly(0, 0, 0, 0);
      assertThat(params.port).isZero();
    }
  }

  @Nested
  @DisplayName("IPv6 Encoding")
  class Ipv6Encoding {

    @Test
    @DisplayName("Should encode [::1]:8080 correctly")
    void shouldEncodeLoopbackAddress() {
      final Ipv6Address addr =
          new Ipv6Address(new short[] {0, 0, 0, 0, 0, 0, 0, 1});
      final IpSocketAddress socketAddr =
          IpSocketAddress.ipv6(new Ipv6SocketAddress(8080, 0, addr, 0));

      final SocketAddressCodec.AddressParams params = SocketAddressCodec.encodeAddress(socketAddr);

      assertThat(params.isIpv4).as("Should be marked as IPv6").isFalse();
      assertThat(params.ipv4Octets).as("IPv4 octets should be null for IPv6").isNull();
      assertThat(params.ipv6Segments).as("Should have 16 bytes for 8 segments").hasSize(16);
      // Segment 7 (last) = 1 -> bytes at [14]=0x00, [15]=0x01
      assertThat(params.ipv6Segments[14]).isEqualTo((byte) 0);
      assertThat(params.ipv6Segments[15]).isEqualTo((byte) 1);
      // All other segments are 0
      for (int i = 0; i < 14; i++) {
        assertThat(params.ipv6Segments[i])
            .as("Byte at index %d should be 0 for ::1 address", i)
            .isEqualTo((byte) 0);
      }
      assertThat(params.port).isEqualTo(8080);
    }

    @Test
    @DisplayName("Should preserve flowInfo and scopeId")
    void shouldPreserveFlowInfoAndScopeId() {
      final Ipv6Address addr =
          new Ipv6Address(new short[] {(short) 0xFE80, 0, 0, 0, 0, 0, 0, 1});
      final IpSocketAddress socketAddr =
          IpSocketAddress.ipv6(new Ipv6SocketAddress(443, 12345, addr, 42));

      final SocketAddressCodec.AddressParams params = SocketAddressCodec.encodeAddress(socketAddr);

      assertThat(params.flowInfo).isEqualTo(12345);
      assertThat(params.scopeId).isEqualTo(42);
      assertThat(params.port).isEqualTo(443);
    }

    @Test
    @DisplayName("Should encode segments in big-endian byte pairs")
    void shouldEncodeSegmentsInBigEndian() {
      // Segment 0 = 0x2001, Segment 1 = 0x0DB8
      final Ipv6Address addr =
          new Ipv6Address(
              new short[] {
                (short) 0x2001, (short) 0x0DB8, 0, 0, 0, 0, 0, (short) 0x0001
              });
      final IpSocketAddress socketAddr =
          IpSocketAddress.ipv6(new Ipv6SocketAddress(80, 0, addr, 0));

      final SocketAddressCodec.AddressParams params = SocketAddressCodec.encodeAddress(socketAddr);

      // Segment 0 = 0x2001 -> [0]=0x20, [1]=0x01
      assertThat(params.ipv6Segments[0]).isEqualTo((byte) 0x20);
      assertThat(params.ipv6Segments[1]).isEqualTo((byte) 0x01);
      // Segment 1 = 0x0DB8 -> [2]=0x0D, [3]=0xB8
      assertThat(params.ipv6Segments[2]).isEqualTo((byte) 0x0D);
      assertThat(params.ipv6Segments[3]).isEqualTo((byte) 0xB8);
    }
  }

  @Nested
  @DisplayName("IPv4 Decoding")
  class Ipv4Decoding {

    @Test
    @DisplayName("Should decode long array to correct IPv4 address")
    void shouldDecodeIpv4Address() {
      // Format: [is_ipv4=1, octet0, octet1, octet2, octet3, port, flow_info, scope_id]
      final long[] encoded = {1, 192, 168, 1, 100, 8080, 0, 0};

      final IpSocketAddress result = SocketAddressCodec.decodeAddress(encoded);

      assertThat(result.isIpv4()).isTrue();
      final Ipv4SocketAddress ipv4 = result.getIpv4();
      final byte[] octets = ipv4.getAddress().getOctets();
      assertThat(octets[0]).isEqualTo((byte) 192);
      assertThat(octets[1]).isEqualTo((byte) 168);
      assertThat(octets[2]).isEqualTo((byte) 1);
      assertThat(octets[3]).isEqualTo((byte) 100);
      assertThat(ipv4.getPort()).isEqualTo(8080);
    }

    @Test
    @DisplayName("Port masking should handle values requiring 0xFFFF mask")
    void portMaskingShouldHandleSignExtension() {
      // Port 65535 stored as long, should be masked correctly
      final long[] encoded = {1, 10, 0, 0, 1, 65535, 0, 0};

      final IpSocketAddress result = SocketAddressCodec.decodeAddress(encoded);
      assertThat(result.getIpv4().getPort()).isEqualTo(65535);
    }
  }

  @Nested
  @DisplayName("IPv6 Decoding")
  class Ipv6Decoding {

    @Test
    @DisplayName("Should decode long array to correct IPv6 address with all fields")
    void shouldDecodeIpv6Address() {
      // Format: [is_ipv4=0, seg0..seg7, port, flow_info, scope_id]
      final long[] encoded = {
        0, // is_ipv4 = false
        0x2001, 0x0DB8, 0, 0, 0, 0, 0, 0x0001, // 8 segments
        443, // port
        99, // flowInfo
        7 // scopeId
      };

      final IpSocketAddress result = SocketAddressCodec.decodeAddress(encoded);

      assertThat(result.isIpv4()).isFalse();
      final Ipv6SocketAddress ipv6 = result.getIpv6();
      final short[] segments = ipv6.getAddress().getSegments();
      assertThat(segments[0]).isEqualTo((short) 0x2001);
      assertThat(segments[1]).isEqualTo((short) 0x0DB8);
      assertThat(segments[7]).isEqualTo((short) 0x0001);
      assertThat(ipv6.getPort()).isEqualTo(443);
      assertThat(ipv6.getFlowInfo()).isEqualTo(99);
      assertThat(ipv6.getScopeId()).isEqualTo(7);
    }
  }

  @Nested
  @DisplayName("Round Trip")
  class RoundTrip {

    @Test
    @DisplayName("IPv4 encode then decode should return equivalent address")
    void ipv4RoundTrip() {
      final Ipv4Address originalAddr = new Ipv4Address(new byte[] {10, 20, 30, 40});
      final IpSocketAddress original = IpSocketAddress.ipv4(new Ipv4SocketAddress(9090, originalAddr));

      final SocketAddressCodec.AddressParams encoded = SocketAddressCodec.encodeAddress(original);

      // Build the long[] that native code would produce from these params
      final long[] nativeEncoded = {
        1, // is_ipv4
        encoded.ipv4Octets[0] & 0xFF,
        encoded.ipv4Octets[1] & 0xFF,
        encoded.ipv4Octets[2] & 0xFF,
        encoded.ipv4Octets[3] & 0xFF,
        encoded.port,
        encoded.flowInfo,
        encoded.scopeId
      };

      final IpSocketAddress decoded = SocketAddressCodec.decodeAddress(nativeEncoded);

      assertThat(decoded.isIpv4()).isTrue();
      assertThat(decoded.getIpv4().getPort()).isEqualTo(original.getIpv4().getPort());
      assertThat(decoded.getIpv4().getAddress().getOctets())
          .containsExactly(original.getIpv4().getAddress().getOctets());
    }

    @Test
    @DisplayName("IPv6 encode then decode should return equivalent address")
    void ipv6RoundTrip() {
      final Ipv6Address originalAddr =
          new Ipv6Address(
              new short[] {
                (short) 0xFE80, 0, 0, 0, (short) 0xDEAD, (short) 0xBEEF, 0, 1
              });
      final IpSocketAddress original =
          IpSocketAddress.ipv6(new Ipv6SocketAddress(8443, 42, originalAddr, 3));

      final SocketAddressCodec.AddressParams encoded = SocketAddressCodec.encodeAddress(original);

      // Build the long[] that native code would produce from the encoded byte pairs
      // Each segment is in big-endian byte pairs, decode back to short values
      final long[] nativeEncoded = new long[12];
      nativeEncoded[0] = 0; // is_ipv4 = false
      for (int i = 0; i < 8; i++) {
        final int high = encoded.ipv6Segments[i * 2] & 0xFF;
        final int low = encoded.ipv6Segments[i * 2 + 1] & 0xFF;
        nativeEncoded[i + 1] = (high << 8) | low;
      }
      nativeEncoded[9] = encoded.port;
      nativeEncoded[10] = encoded.flowInfo;
      nativeEncoded[11] = encoded.scopeId;

      final IpSocketAddress decoded = SocketAddressCodec.decodeAddress(nativeEncoded);

      assertThat(decoded.isIpv4()).isFalse();
      final Ipv6SocketAddress decodedIpv6 = decoded.getIpv6();
      final Ipv6SocketAddress originalIpv6 = original.getIpv6();
      assertThat(decodedIpv6.getPort()).isEqualTo(originalIpv6.getPort());
      assertThat(decodedIpv6.getFlowInfo()).isEqualTo(originalIpv6.getFlowInfo());
      assertThat(decodedIpv6.getScopeId()).isEqualTo(originalIpv6.getScopeId());
      assertThat(decodedIpv6.getAddress().getSegments())
          .containsExactly(originalIpv6.getAddress().getSegments());
    }
  }
}
