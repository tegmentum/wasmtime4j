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
package ai.tegmentum.wasmtime4j.jni.wasi.sockets;

import ai.tegmentum.wasmtime4j.wasi.sockets.IpSocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6SocketAddress;

/**
 * Shared address encoding/decoding utilities for JNI WASI socket implementations.
 *
 * <p>Provides common functionality for converting between {@link IpSocketAddress} and the
 * byte-array/long-array representations used by JNI native socket operations.
 *
 * @since 1.0.0
 */
final class SocketAddressCodec {

  private SocketAddressCodec() {
    // Utility class
  }

  /**
   * Holds encoded address parameters for passing to JNI native socket operations.
   *
   * @since 1.0.0
   */
  static final class AddressParams {
    final boolean isIpv4;
    final byte[] ipv4Octets;
    final byte[] ipv6Segments;
    final int port;
    final int flowInfo;
    final int scopeId;

    AddressParams(
        final boolean isIpv4,
        final byte[] ipv4Octets,
        final byte[] ipv6Segments,
        final int port,
        final int flowInfo,
        final int scopeId) {
      this.isIpv4 = isIpv4;
      this.ipv4Octets = ipv4Octets;
      this.ipv6Segments = ipv6Segments;
      this.port = port;
      this.flowInfo = flowInfo;
      this.scopeId = scopeId;
    }
  }

  /**
   * Encodes an {@link IpSocketAddress} into byte arrays for JNI.
   *
   * @param address the socket address to encode
   * @return the encoded address parameters
   */
  static AddressParams encodeAddress(final IpSocketAddress address) {
    if (address.isIpv4()) {
      final Ipv4SocketAddress ipv4 = address.getIpv4();
      return new AddressParams(
          true,
          ipv4.getAddress().getOctets(),
          null,
          ipv4.getPort(),
          0, // IPv4 doesn't have flow info
          0 // IPv4 doesn't have scope ID
          );
    } else {
      final Ipv6SocketAddress ipv6 = address.getIpv6();
      final short[] segments = ipv6.getAddress().getSegments();

      // Convert shorts to bytes (big-endian)
      final byte[] segmentBytes = new byte[16];
      for (int i = 0; i < 8; i++) {
        segmentBytes[i * 2] = (byte) (segments[i] >> 8);
        segmentBytes[i * 2 + 1] = (byte) segments[i];
      }

      return new AddressParams(
          false, null, segmentBytes, ipv6.getPort(), ipv6.getFlowInfo(), ipv6.getScopeId());
    }
  }

  /**
   * Decodes a long array from JNI into an {@link IpSocketAddress}.
   *
   * <p>The array format is:
   *
   * <ul>
   *   <li>IPv4: [is_ipv4=1, octet0, octet1, octet2, octet3, port, flow_info, scope_id]
   *   <li>IPv6: [is_ipv4=0, seg0, seg1, ..., seg7, port, flow_info, scope_id]
   * </ul>
   *
   * @param encoded the encoded long array from native code
   * @return the decoded socket address
   */
  static IpSocketAddress decodeAddress(final long[] encoded) {
    final boolean isIpv4 = encoded[0] != 0;

    if (isIpv4) {
      final byte[] octets =
          new byte[] {(byte) encoded[1], (byte) encoded[2], (byte) encoded[3], (byte) encoded[4]};
      final int port = (int) encoded[5] & 0xFFFF;

      final Ipv4Address ipv4Address = new Ipv4Address(octets);
      return IpSocketAddress.ipv4(new Ipv4SocketAddress(port, ipv4Address));
    } else {
      final short[] segments = new short[8];
      for (int i = 0; i < 8; i++) {
        segments[i] = (short) encoded[i + 1];
      }
      final int port = (int) encoded[9] & 0xFFFF;
      final long flowInfo = encoded[10];
      final long scopeId = encoded[11];

      final Ipv6Address ipv6Address = new Ipv6Address(segments);
      return IpSocketAddress.ipv6(
          new Ipv6SocketAddress(port, (int) flowInfo, ipv6Address, (int) scopeId));
    }
  }
}
