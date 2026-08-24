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
    @DisplayName("should have exactly 7 values")
    void shouldHaveExactly7Values() {
      assertEquals(7, SocketAddrUse.values().length);
    }

    @Test
    @DisplayName("should contain all expected values")
    void shouldContainAllExpectedValues() {
      Set<SocketAddrUse> values = new HashSet<>(Arrays.asList(SocketAddrUse.values()));

      assertTrue(values.contains(SocketAddrUse.TCP_BIND));
      assertTrue(values.contains(SocketAddrUse.TCP_LISTEN));
      assertTrue(values.contains(SocketAddrUse.TCP_ACCEPT));
      assertTrue(values.contains(SocketAddrUse.TCP_CONNECT));
      assertTrue(values.contains(SocketAddrUse.UDP_BIND));
      assertTrue(values.contains(SocketAddrUse.UDP_SEND));
      assertTrue(values.contains(SocketAddrUse.UDP_RECEIVE));
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
    @DisplayName("TCP_LISTEN should have value 1")
    void tcpListenShouldHaveValue1() {
      assertEquals(1, SocketAddrUse.TCP_LISTEN.getValue());
    }

    @Test
    @DisplayName("TCP_ACCEPT should have value 2")
    void tcpAcceptShouldHaveValue2() {
      assertEquals(2, SocketAddrUse.TCP_ACCEPT.getValue());
    }

    @Test
    @DisplayName("TCP_CONNECT should have value 3")
    void tcpConnectShouldHaveValue3() {
      assertEquals(3, SocketAddrUse.TCP_CONNECT.getValue());
    }

    @Test
    @DisplayName("UDP_BIND should have value 4")
    void udpBindShouldHaveValue4() {
      assertEquals(4, SocketAddrUse.UDP_BIND.getValue());
    }

    @Test
    @DisplayName("UDP_SEND should have value 5")
    void udpSendShouldHaveValue5() {
      assertEquals(5, SocketAddrUse.UDP_SEND.getValue());
    }

    @Test
    @DisplayName("UDP_RECEIVE should have value 6")
    void udpReceiveShouldHaveValue6() {
      assertEquals(6, SocketAddrUse.UDP_RECEIVE.getValue());
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
      assertEquals(SocketAddrUse.TCP_LISTEN, SocketAddrUse.fromValue(1));
      assertEquals(SocketAddrUse.TCP_ACCEPT, SocketAddrUse.fromValue(2));
      assertEquals(SocketAddrUse.TCP_CONNECT, SocketAddrUse.fromValue(3));
      assertEquals(SocketAddrUse.UDP_BIND, SocketAddrUse.fromValue(4));
      assertEquals(SocketAddrUse.UDP_SEND, SocketAddrUse.fromValue(5));
      assertEquals(SocketAddrUse.UDP_RECEIVE, SocketAddrUse.fromValue(6));
    }

    @Test
    @DisplayName("should throw for invalid value")
    void shouldThrowForInvalidValue() {
      assertThrows(IllegalArgumentException.class, () -> SocketAddrUse.fromValue(7));
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
      assertEquals(SocketAddrUse.TCP_LISTEN, SocketAddrUse.valueOf("TCP_LISTEN"));
      assertEquals(SocketAddrUse.TCP_ACCEPT, SocketAddrUse.valueOf("TCP_ACCEPT"));
      assertEquals(SocketAddrUse.TCP_CONNECT, SocketAddrUse.valueOf("TCP_CONNECT"));
      assertEquals(SocketAddrUse.UDP_BIND, SocketAddrUse.valueOf("UDP_BIND"));
      assertEquals(SocketAddrUse.UDP_SEND, SocketAddrUse.valueOf("UDP_SEND"));
      assertEquals(SocketAddrUse.UDP_RECEIVE, SocketAddrUse.valueOf("UDP_RECEIVE"));
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
      assertEquals("TCP_LISTEN", SocketAddrUse.TCP_LISTEN.name());
      assertEquals("TCP_ACCEPT", SocketAddrUse.TCP_ACCEPT.name());
      assertEquals("TCP_CONNECT", SocketAddrUse.TCP_CONNECT.name());
      assertEquals("UDP_BIND", SocketAddrUse.UDP_BIND.name());
      assertEquals("UDP_SEND", SocketAddrUse.UDP_SEND.name());
      assertEquals("UDP_RECEIVE", SocketAddrUse.UDP_RECEIVE.name());
    }
  }
}
