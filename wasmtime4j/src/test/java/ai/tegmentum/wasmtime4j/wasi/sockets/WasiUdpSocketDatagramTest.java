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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link WasiUdpSocket.IncomingDatagram} and {@link WasiUdpSocket.OutgoingDatagram}. */
@DisplayName("WasiUdpSocket Datagram Types Tests")
class WasiUdpSocketDatagramTest {

  private static IpSocketAddress createTestAddress() {
    return IpSocketAddress.ipv4(
        new Ipv4SocketAddress(8080, new Ipv4Address(new byte[] {127, 0, 0, 1})));
  }

  @Nested
  @DisplayName("IncomingDatagram")
  class IncomingDatagramTests {

    @Test
    @DisplayName("should store data and remote address")
    void shouldStoreDataAndRemoteAddress() {
      byte[] data = new byte[] {1, 2, 3};
      IpSocketAddress addr = createTestAddress();

      WasiUdpSocket.IncomingDatagram dg = new WasiUdpSocket.IncomingDatagram(data, addr);

      assertArrayEquals(new byte[] {1, 2, 3}, dg.getData());
      assertEquals(addr, dg.getRemoteAddress());
    }

    @Test
    @DisplayName("should defensive-copy data on construction")
    void shouldDefensiveCopyDataOnConstruction() {
      byte[] data = new byte[] {1, 2, 3};
      WasiUdpSocket.IncomingDatagram dg =
          new WasiUdpSocket.IncomingDatagram(data, createTestAddress());

      data[0] = 99;
      assertEquals(1, dg.getData()[0], "Internal data should not be affected by external mutation");
    }

    @Test
    @DisplayName("getData should return defensive copy")
    void getDataShouldReturnDefensiveCopy() {
      WasiUdpSocket.IncomingDatagram dg =
          new WasiUdpSocket.IncomingDatagram(new byte[] {1, 2, 3}, createTestAddress());

      byte[] first = dg.getData();
      byte[] second = dg.getData();
      assertNotSame(first, second, "getData should return new array each time");
    }

    @Test
    @DisplayName("should throw when data is null")
    void shouldThrowWhenDataIsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiUdpSocket.IncomingDatagram(null, createTestAddress()));
    }

    @Test
    @DisplayName("should throw when remote address is null")
    void shouldThrowWhenRemoteAddressIsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiUdpSocket.IncomingDatagram(new byte[] {1}, null));
    }

    @Test
    @DisplayName("should handle empty data")
    void shouldHandleEmptyData() {
      WasiUdpSocket.IncomingDatagram dg =
          new WasiUdpSocket.IncomingDatagram(new byte[0], createTestAddress());

      assertEquals(0, dg.getData().length);
    }
  }

  @Nested
  @DisplayName("OutgoingDatagram with remote address")
  class OutgoingDatagramWithAddress {

    @Test
    @DisplayName("should store data and remote address")
    void shouldStoreDataAndRemoteAddress() {
      byte[] data = new byte[] {4, 5, 6};
      IpSocketAddress addr = createTestAddress();

      WasiUdpSocket.OutgoingDatagram dg = new WasiUdpSocket.OutgoingDatagram(data, addr);

      assertArrayEquals(new byte[] {4, 5, 6}, dg.getData());
      assertEquals(addr, dg.getRemoteAddress());
      assertTrue(dg.hasRemoteAddress());
    }

    @Test
    @DisplayName("should defensive-copy data")
    void shouldDefensiveCopyData() {
      byte[] data = new byte[] {1, 2};
      WasiUdpSocket.OutgoingDatagram dg =
          new WasiUdpSocket.OutgoingDatagram(data, createTestAddress());

      data[0] = 99;
      assertEquals(1, dg.getData()[0]);
    }

    @Test
    @DisplayName("should throw when data is null")
    void shouldThrowWhenDataIsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiUdpSocket.OutgoingDatagram(null, createTestAddress()));
    }
  }

  @Nested
  @DisplayName("OutgoingDatagram without remote address")
  class OutgoingDatagramWithoutAddress {

    @Test
    @DisplayName("should store data without remote address")
    void shouldStoreDataWithoutRemoteAddress() {
      WasiUdpSocket.OutgoingDatagram dg = new WasiUdpSocket.OutgoingDatagram(new byte[] {7, 8, 9});

      assertArrayEquals(new byte[] {7, 8, 9}, dg.getData());
      assertNull(dg.getRemoteAddress());
      assertFalse(dg.hasRemoteAddress());
    }

    @Test
    @DisplayName("should throw when data is null")
    void shouldThrowWhenDataIsNull() {
      assertThrows(IllegalArgumentException.class, () -> new WasiUdpSocket.OutgoingDatagram(null));
    }

    @Test
    @DisplayName("getData should return defensive copy")
    void getDataShouldReturnDefensiveCopy() {
      WasiUdpSocket.OutgoingDatagram dg = new WasiUdpSocket.OutgoingDatagram(new byte[] {1});

      byte[] first = dg.getData();
      byte[] second = dg.getData();
      assertNotSame(first, second);
    }
  }
}
