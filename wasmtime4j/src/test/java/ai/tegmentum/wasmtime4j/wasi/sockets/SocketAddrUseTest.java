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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for the {@link SocketAddrUse} enum. */
@DisplayName("SocketAddrUse Tests")
class SocketAddrUseTest {

  @Nested
  @DisplayName("Enum Structure")
  class EnumStructure {

    @Test
    @DisplayName("should have exactly 5 values")
    void shouldHaveExactly5Values() {
      assertEquals(5, SocketAddrUse.values().length);
    }

    @Test
    @DisplayName("should contain all expected values")
    void shouldContainAllExpectedValues() {
      Set<SocketAddrUse> values = new HashSet<>(Arrays.asList(SocketAddrUse.values()));

      assertTrue(values.contains(SocketAddrUse.TCP_BIND));
      assertTrue(values.contains(SocketAddrUse.TCP_CONNECT));
      assertTrue(values.contains(SocketAddrUse.UDP_BIND));
      assertTrue(values.contains(SocketAddrUse.UDP_CONNECT));
      assertTrue(values.contains(SocketAddrUse.UDP_OUTGOING_DATAGRAM));
    }
  }

  @Nested
  @DisplayName("getValue")
  class GetValue {

    @Test
    @DisplayName("TCP_BIND should have value 0")
    void tcpBindShouldHaveValue0() {
      assertEquals(0, SocketAddrUse.TCP_BIND.getValue());
    }

    @Test
    @DisplayName("TCP_CONNECT should have value 1")
    void tcpConnectShouldHaveValue1() {
      assertEquals(1, SocketAddrUse.TCP_CONNECT.getValue());
    }

    @Test
    @DisplayName("UDP_BIND should have value 2")
    void udpBindShouldHaveValue2() {
      assertEquals(2, SocketAddrUse.UDP_BIND.getValue());
    }

    @Test
    @DisplayName("UDP_CONNECT should have value 3")
    void udpConnectShouldHaveValue3() {
      assertEquals(3, SocketAddrUse.UDP_CONNECT.getValue());
    }

    @Test
    @DisplayName("UDP_OUTGOING_DATAGRAM should have value 4")
    void udpOutgoingDatagramShouldHaveValue4() {
      assertEquals(4, SocketAddrUse.UDP_OUTGOING_DATAGRAM.getValue());
    }

    @Test
    @DisplayName("all values should be unique")
    void allValuesShouldBeUnique() {
      Set<Integer> intValues = new HashSet<>();
      for (SocketAddrUse use : SocketAddrUse.values()) {
        assertTrue(intValues.add(use.getValue()), "Duplicate value found: " + use.getValue());
      }
    }
  }

  @Nested
  @DisplayName("fromValue")
  class FromValue {

    @Test
    @DisplayName("should return correct constant for each value")
    void shouldReturnCorrectConstantForEachValue() {
      assertEquals(SocketAddrUse.TCP_BIND, SocketAddrUse.fromValue(0));
      assertEquals(SocketAddrUse.TCP_CONNECT, SocketAddrUse.fromValue(1));
      assertEquals(SocketAddrUse.UDP_BIND, SocketAddrUse.fromValue(2));
      assertEquals(SocketAddrUse.UDP_CONNECT, SocketAddrUse.fromValue(3));
      assertEquals(SocketAddrUse.UDP_OUTGOING_DATAGRAM, SocketAddrUse.fromValue(4));
    }

    @Test
    @DisplayName("should throw for invalid value")
    void shouldThrowForInvalidValue() {
      assertThrows(IllegalArgumentException.class, () -> SocketAddrUse.fromValue(5));
    }

    @Test
    @DisplayName("should throw for negative value")
    void shouldThrowForNegativeValue() {
      assertThrows(IllegalArgumentException.class, () -> SocketAddrUse.fromValue(-1));
    }

    @Test
    @DisplayName("fromValue round-trips with getValue")
    void fromValueRoundTripsWithGetValue() {
      for (SocketAddrUse use : SocketAddrUse.values()) {
        assertEquals(use, SocketAddrUse.fromValue(use.getValue()));
      }
    }
  }

  @Nested
  @DisplayName("valueOf")
  class ValueOf {

    @Test
    @DisplayName("valueOf should return correct constant")
    void valueOfShouldReturnCorrectConstant() {
      assertEquals(SocketAddrUse.TCP_BIND, SocketAddrUse.valueOf("TCP_BIND"));
      assertEquals(SocketAddrUse.TCP_CONNECT, SocketAddrUse.valueOf("TCP_CONNECT"));
      assertEquals(SocketAddrUse.UDP_BIND, SocketAddrUse.valueOf("UDP_BIND"));
      assertEquals(SocketAddrUse.UDP_CONNECT, SocketAddrUse.valueOf("UDP_CONNECT"));
      assertEquals(
          SocketAddrUse.UDP_OUTGOING_DATAGRAM, SocketAddrUse.valueOf("UDP_OUTGOING_DATAGRAM"));
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class, () -> SocketAddrUse.valueOf("INVALID"));
    }
  }

  @Nested
  @DisplayName("name")
  class NameTests {

    @Test
    @DisplayName("name should return correct string for each constant")
    void nameShouldReturnCorrectString() {
      assertEquals("TCP_BIND", SocketAddrUse.TCP_BIND.name());
      assertEquals("TCP_CONNECT", SocketAddrUse.TCP_CONNECT.name());
      assertEquals("UDP_BIND", SocketAddrUse.UDP_BIND.name());
      assertEquals("UDP_CONNECT", SocketAddrUse.UDP_CONNECT.name());
      assertEquals("UDP_OUTGOING_DATAGRAM", SocketAddrUse.UDP_OUTGOING_DATAGRAM.name());
    }
  }
}
