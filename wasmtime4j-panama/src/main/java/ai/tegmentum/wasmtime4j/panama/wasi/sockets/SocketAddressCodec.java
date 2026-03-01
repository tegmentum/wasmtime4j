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
package ai.tegmentum.wasmtime4j.panama.wasi.sockets;

import ai.tegmentum.wasmtime4j.wasi.sockets.IpSocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6SocketAddress;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Shared address encoding/decoding utilities for Panama WASI socket implementations.
 *
 * <p>Provides common functionality for converting between {@link IpSocketAddress} and native memory
 * representations used by both TCP and UDP socket implementations.
 *
 * @since 1.0.0
 */
final class SocketAddressCodec {

  private SocketAddressCodec() {
    // Utility class
  }

  /**
   * Holds encoded address parameters for passing to native socket operations.
   *
   * @since 1.0.0
   */
  static final class AddressParams {
    final boolean isIpv4;
    final MemorySegment ipv4Octets;
    final MemorySegment ipv6Segments;
    final int port;
    final int flowInfo;
    final int scopeId;

    AddressParams(
        final boolean isIpv4,
        final MemorySegment ipv4Octets,
        final MemorySegment ipv6Segments,
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
   * Encodes an {@link IpSocketAddress} into native memory segments.
   *
   * @param address the socket address to encode
   * @param arena the arena for memory allocation
   * @return the encoded address parameters
   */
  static AddressParams encodeAddress(final IpSocketAddress address, final Arena arena) {
    if (address.isIpv4()) {
      final Ipv4SocketAddress ipv4 = address.getIpv4();
      final byte[] octets = ipv4.getAddress().getOctets();
      final MemorySegment octetsSegment = arena.allocate(4);
      for (int i = 0; i < 4; i++) {
        octetsSegment.set(ValueLayout.JAVA_BYTE, i, octets[i]);
      }
      return new AddressParams(
          true,
          octetsSegment,
          MemorySegment.NULL,
          ipv4.getPort(),
          0, // IPv4 doesn't have flow info
          0 // IPv4 doesn't have scope ID
          );
    } else {
      final Ipv6SocketAddress ipv6 = address.getIpv6();
      final short[] segments = ipv6.getAddress().getSegments();
      final MemorySegment segmentsSegment = arena.allocate(ValueLayout.JAVA_SHORT, 8);
      for (int i = 0; i < 8; i++) {
        segmentsSegment.setAtIndex(ValueLayout.JAVA_SHORT, i, segments[i]);
      }
      return new AddressParams(
          false,
          MemorySegment.NULL,
          segmentsSegment,
          ipv6.getPort(),
          ipv6.getFlowInfo(),
          ipv6.getScopeId());
    }
  }

  /**
   * Decodes native memory segments into an {@link IpSocketAddress}.
   *
   * @param outIsIpv4 memory segment containing the IPv4 flag
   * @param outAddrBuf memory segment containing the address bytes/segments
   * @param outPort memory segment containing the port
   * @param outFlowInfo memory segment containing the IPv6 flow info
   * @param outScopeId memory segment containing the IPv6 scope ID
   * @return the decoded socket address
   */
  static IpSocketAddress decodeAddress(
      final MemorySegment outIsIpv4,
      final MemorySegment outAddrBuf,
      final MemorySegment outPort,
      final MemorySegment outFlowInfo,
      final MemorySegment outScopeId) {
    final boolean isIpv4 = outIsIpv4.get(ValueLayout.JAVA_INT, 0) != 0;

    if (isIpv4) {
      final byte[] octets = new byte[4];
      for (int i = 0; i < 4; i++) {
        octets[i] = outAddrBuf.get(ValueLayout.JAVA_BYTE, i);
      }
      final int port = outPort.get(ValueLayout.JAVA_SHORT, 0) & 0xFFFF;

      final Ipv4Address ipv4Address = new Ipv4Address(octets);
      return IpSocketAddress.ipv4(new Ipv4SocketAddress(port, ipv4Address));
    } else {
      final short[] segments = new short[8];
      for (int i = 0; i < 8; i++) {
        segments[i] = outAddrBuf.getAtIndex(ValueLayout.JAVA_SHORT, i);
      }
      final int port = outPort.get(ValueLayout.JAVA_SHORT, 0) & 0xFFFF;
      final int flowInfo = outFlowInfo.get(ValueLayout.JAVA_INT, 0);
      final int scopeId = outScopeId.get(ValueLayout.JAVA_INT, 0);

      final Ipv6Address ipv6Address = new Ipv6Address(segments);
      return IpSocketAddress.ipv6(new Ipv6SocketAddress(port, flowInfo, ipv6Address, scopeId));
    }
  }
}
