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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for the {@link SocketAddrCheck} functional interface. */
@DisplayName("SocketAddrCheck Tests")
class SocketAddrCheckTest {

  @Nested
  @DisplayName("Functional Interface Usage")
  class FunctionalInterfaceUsage {

    @Test
    @DisplayName("should be implementable as lambda that allows all")
    void shouldBeImplementableAsLambdaThatAllowsAll() {
      SocketAddrCheck allowAll = (addr, use) -> true;

      assertTrue(allowAll.check(new InetSocketAddress("localhost", 80), SocketAddrUse.TCP_CONNECT));
    }

    @Test
    @DisplayName("should be implementable as lambda that denies all")
    void shouldBeImplementableAsLambdaThatDeniesAll() {
      SocketAddrCheck denyAll = (addr, use) -> false;

      assertFalse(denyAll.check(new InetSocketAddress("localhost", 80), SocketAddrUse.TCP_CONNECT));
    }

    @Test
    @DisplayName("should filter by port")
    void shouldFilterByPort() {
      SocketAddrCheck onlyHttps = (addr, use) -> addr.getPort() == 443;

      assertTrue(
          onlyHttps.check(new InetSocketAddress("example.com", 443), SocketAddrUse.TCP_CONNECT));
      assertFalse(
          onlyHttps.check(new InetSocketAddress("example.com", 80), SocketAddrUse.TCP_CONNECT));
    }

    @Test
    @DisplayName("should filter by socket addr use")
    void shouldFilterBySocketAddrUse() {
      SocketAddrCheck onlyTcpConnect = (addr, use) -> use == SocketAddrUse.TCP_CONNECT;

      assertTrue(
          onlyTcpConnect.check(new InetSocketAddress("localhost", 80), SocketAddrUse.TCP_CONNECT));
      assertFalse(
          onlyTcpConnect.check(new InetSocketAddress("localhost", 80), SocketAddrUse.UDP_BIND));
    }

    @Test
    @DisplayName("should combine address and use checks")
    void shouldCombineAddressAndUseChecks() {
      SocketAddrCheck check =
          (addr, use) -> addr.getPort() == 443 && use == SocketAddrUse.TCP_CONNECT;

      assertTrue(check.check(new InetSocketAddress("example.com", 443), SocketAddrUse.TCP_CONNECT));
      assertFalse(check.check(new InetSocketAddress("example.com", 443), SocketAddrUse.TCP_BIND));
      assertFalse(check.check(new InetSocketAddress("example.com", 80), SocketAddrUse.TCP_CONNECT));
    }
  }
}
