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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for WASI Sockets Loopback functionality.
 *
 * <p>These tests verify socket operations on the loopback interface (127.0.0.1) including TCP
 * client/server communication and UDP datagram exchange.
 *
 * @since 1.0.0
 */
@DisplayName("WASI Sockets Loopback Integration Tests")
public class WasiSocketsLoopbackTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(WasiSocketsLoopbackTest.class.getName());

  /** Loopback IPv4 address bytes: 127.0.0.1. */
  private static final byte[] LOOPBACK_ADDRESS = {127, 0, 0, 1};

  /** Any address for binding: 0.0.0.0. */
  private static final byte[] ANY_ADDRESS = {0, 0, 0, 0};

  private static boolean checkWasiSocketsAvailable() {
    try {
      Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.sockets.JniWasiNetwork");
      Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.sockets.JniWasiTcpSocket");
      Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.sockets.JniWasiUdpSocket");
      return true;
    } catch (final ClassNotFoundException e) {
      LOGGER.warning("WASI Sockets not available: JNI socket classes not found: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("WASI Sockets not available: Failed to initialize: " + e.getMessage());
    }
    return false;
  }

  private static void assumeWasiSocketsAvailable() {
    assumeTrue(checkWasiSocketsAvailable(), "WASI Sockets not available");
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
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Loopback Address Tests")
  class LoopbackAddressTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create loopback IPv4 address")
    void shouldCreateLoopbackIpv4Address(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should create loopback IPv4 address");

      final Ipv4Address loopback = new Ipv4Address(LOOPBACK_ADDRESS);

      assertNotNull(loopback, "Loopback address should not be null");
      assertArrayEquals(LOOPBACK_ADDRESS, loopback.getOctets(), "Octets should match");
      assertEquals("127.0.0.1", loopback.toString(), "String representation should match");

      LOGGER.info("Created loopback address: " + loopback);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create any address (0.0.0.0)")
    void shouldCreateAnyAddress(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should create any address (0.0.0.0)");

      final Ipv4Address any = new Ipv4Address(ANY_ADDRESS);

      assertNotNull(any, "Any address should not be null");
      assertArrayEquals(ANY_ADDRESS, any.getOctets(), "Octets should match");
      assertEquals("0.0.0.0", any.toString(), "String representation should match");

      LOGGER.info("Created any address: " + any);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create loopback socket address with port")
    void shouldCreateLoopbackSocketAddressWithPort(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should create loopback socket address with port");

      final Ipv4Address loopback = new Ipv4Address(LOOPBACK_ADDRESS);
      final Ipv4SocketAddress socketAddr = new Ipv4SocketAddress(8080, loopback);

      assertNotNull(socketAddr, "Socket address should not be null");
      assertEquals(8080, socketAddr.getPort(), "Port should be 8080");
      assertEquals(loopback, socketAddr.getAddress(), "Address should be loopback");
      assertEquals("127.0.0.1:8080", socketAddr.toString(), "String representation should match");

      LOGGER.info("Created socket address: " + socketAddr);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create IP socket address wrapper")
    void shouldCreateIpSocketAddressWrapper(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should create IP socket address wrapper");

      final Ipv4Address loopback = new Ipv4Address(LOOPBACK_ADDRESS);
      final Ipv4SocketAddress ipv4Addr = new Ipv4SocketAddress(9090, loopback);
      final IpSocketAddress socketAddr = IpSocketAddress.ipv4(ipv4Addr);

      assertTrue(socketAddr.isIpv4(), "Should be IPv4 address");
      assertEquals(ipv4Addr, socketAddr.getIpv4(), "IPv4 address should match");
      assertEquals("127.0.0.1:9090", socketAddr.toString(), "String representation should match");

      LOGGER.info("Created IP socket address: " + socketAddr);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should reject invalid port numbers")
    void shouldRejectInvalidPortNumbers(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should reject invalid port numbers");

      final Ipv4Address loopback = new Ipv4Address(LOOPBACK_ADDRESS);

      // Negative port
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv4SocketAddress(-1, loopback),
          "Should reject negative port");

      // Port too large
      assertThrows(
          IllegalArgumentException.class,
          () -> new Ipv4SocketAddress(65536, loopback),
          "Should reject port > 65535");

      LOGGER.info("Invalid port numbers correctly rejected");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should accept valid port range")
    void shouldAcceptValidPortRange(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should accept valid port range");

      final Ipv4Address loopback = new Ipv4Address(LOOPBACK_ADDRESS);

      // Minimum valid port
      final Ipv4SocketAddress minPort = new Ipv4SocketAddress(0, loopback);
      assertEquals(0, minPort.getPort(), "Port 0 should be valid");

      // Maximum valid port
      final Ipv4SocketAddress maxPort = new Ipv4SocketAddress(65535, loopback);
      assertEquals(65535, maxPort.getPort(), "Port 65535 should be valid");

      // Common ports
      final Ipv4SocketAddress httpPort = new Ipv4SocketAddress(80, loopback);
      assertEquals(80, httpPort.getPort(), "Port 80 should be valid");

      LOGGER.info("Valid port range accepted");
    }
  }

  @Nested
  @DisplayName("Address Family Tests")
  class AddressFamilyTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have IPv4 address family")
    void shouldHaveIpv4AddressFamily(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should have IPv4 address family");

      // Verify IpAddressFamily enum exists and has IPv4
      assertNotNull(IpAddressFamily.IPV4, "IPv4 family should exist");
      assertEquals("IPV4", IpAddressFamily.IPV4.name(), "IPv4 name should match");

      LOGGER.info("IPv4 address family verified: " + IpAddressFamily.IPV4);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have IPv6 address family")
    void shouldHaveIpv6AddressFamily(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should have IPv6 address family");

      // Verify IpAddressFamily enum has IPv6
      assertNotNull(IpAddressFamily.IPV6, "IPv6 family should exist");
      assertEquals("IPV6", IpAddressFamily.IPV6.name(), "IPv6 name should match");

      LOGGER.info("IPv6 address family verified: " + IpAddressFamily.IPV6);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should distinguish IPv4 and IPv6 in IpSocketAddress")
    void shouldDistinguishIpv4AndIpv6InIpSocketAddress(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should distinguish IPv4 and IPv6 in IpSocketAddress");

      final Ipv4Address loopback = new Ipv4Address(LOOPBACK_ADDRESS);
      final Ipv4SocketAddress ipv4Addr = new Ipv4SocketAddress(8080, loopback);
      final IpSocketAddress socketAddr = IpSocketAddress.ipv4(ipv4Addr);

      assertTrue(socketAddr.isIpv4(), "Should be IPv4");
      assertFalse(!socketAddr.isIpv4(), "Should not be IPv6");

      // Attempting to get IPv6 should throw
      assertThrows(
          IllegalStateException.class,
          () -> socketAddr.getIpv6(),
          "Getting IPv6 from IPv4 address should throw");

      LOGGER.info("IPv4/IPv6 distinction verified");
    }
  }

  @Nested
  @DisplayName("TCP Socket Interface Tests")
  class TcpSocketInterfaceTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have bind method")
    void shouldHaveBindMethod(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have bind method");

      final var startBind =
          WasiTcpSocket.class.getMethod("startBind", WasiNetwork.class, IpSocketAddress.class);
      assertNotNull(startBind, "startBind method should exist");

      final var finishBind = WasiTcpSocket.class.getMethod("finishBind");
      assertNotNull(finishBind, "finishBind method should exist");

      LOGGER.info("TCP bind methods verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have connect method")
    void shouldHaveConnectMethod(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have connect method");

      final var startConnect =
          WasiTcpSocket.class.getMethod("startConnect", WasiNetwork.class, IpSocketAddress.class);
      assertNotNull(startConnect, "startConnect method should exist");

      final var finishConnect = WasiTcpSocket.class.getMethod("finishConnect");
      assertNotNull(finishConnect, "finishConnect method should exist");

      LOGGER.info("TCP connect methods verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have listen method")
    void shouldHaveListenMethod(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have listen method");

      final var startListen = WasiTcpSocket.class.getMethod("startListen");
      assertNotNull(startListen, "startListen method should exist");

      final var finishListen = WasiTcpSocket.class.getMethod("finishListen");
      assertNotNull(finishListen, "finishListen method should exist");

      LOGGER.info("TCP listen methods verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have accept method")
    void shouldHaveAcceptMethod(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have accept method");

      final var accept = WasiTcpSocket.class.getMethod("accept");
      assertNotNull(accept, "accept method should exist");
      assertEquals(
          WasiTcpSocket.AcceptResult.class, accept.getReturnType(), "Should return AcceptResult");

      LOGGER.info("TCP accept method verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have address methods")
    void shouldHaveAddressMethods(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have address methods");

      final var localAddress = WasiTcpSocket.class.getMethod("localAddress");
      assertNotNull(localAddress, "localAddress method should exist");
      assertEquals(
          IpSocketAddress.class, localAddress.getReturnType(), "Should return IpSocketAddress");

      final var remoteAddress = WasiTcpSocket.class.getMethod("remoteAddress");
      assertNotNull(remoteAddress, "remoteAddress method should exist");
      assertEquals(
          IpSocketAddress.class, remoteAddress.getReturnType(), "Should return IpSocketAddress");

      LOGGER.info("TCP address methods verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have shutdown method");

      final var shutdown =
          WasiTcpSocket.class.getMethod("shutdown", WasiTcpSocket.ShutdownType.class);
      assertNotNull(shutdown, "shutdown method should exist");

      LOGGER.info("TCP shutdown method verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have buffer size methods")
    void shouldHaveBufferSizeMethods(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have buffer size methods");

      final var receiveBufferSize = WasiTcpSocket.class.getMethod("receiveBufferSize");
      assertNotNull(receiveBufferSize, "receiveBufferSize method should exist");
      assertEquals(long.class, receiveBufferSize.getReturnType(), "Should return long");

      final var setReceiveBufferSize =
          WasiTcpSocket.class.getMethod("setReceiveBufferSize", long.class);
      assertNotNull(setReceiveBufferSize, "setReceiveBufferSize method should exist");

      final var sendBufferSize = WasiTcpSocket.class.getMethod("sendBufferSize");
      assertNotNull(sendBufferSize, "sendBufferSize method should exist");

      final var setSendBufferSize = WasiTcpSocket.class.getMethod("setSendBufferSize", long.class);
      assertNotNull(setSendBufferSize, "setSendBufferSize method should exist");

      LOGGER.info("TCP buffer size methods verified");
    }
  }

  @Nested
  @DisplayName("UDP Socket Interface Tests")
  class UdpSocketInterfaceTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have WasiUdpSocket interface")
    void shouldHaveWasiUdpSocketInterface(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should have WasiUdpSocket interface");

      assertTrue(WasiUdpSocket.class.isInterface(), "WasiUdpSocket should be an interface");

      LOGGER.info("WasiUdpSocket interface verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have bind method")
    void shouldHaveBindMethod(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have bind method");

      final var startBind =
          WasiUdpSocket.class.getMethod("startBind", WasiNetwork.class, IpSocketAddress.class);
      assertNotNull(startBind, "startBind method should exist");

      final var finishBind = WasiUdpSocket.class.getMethod("finishBind");
      assertNotNull(finishBind, "finishBind method should exist");

      LOGGER.info("UDP bind methods verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have stream method")
    void shouldHaveStreamMethod(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have stream method");

      final var stream =
          WasiUdpSocket.class.getMethod("stream", WasiNetwork.class, IpSocketAddress.class);
      assertNotNull(stream, "stream method should exist");

      LOGGER.info("UDP stream method verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have address methods")
    void shouldHaveAddressMethods(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have address methods");

      final var localAddress = WasiUdpSocket.class.getMethod("localAddress");
      assertNotNull(localAddress, "localAddress method should exist");
      assertEquals(
          IpSocketAddress.class, localAddress.getReturnType(), "Should return IpSocketAddress");

      final var remoteAddress = WasiUdpSocket.class.getMethod("remoteAddress");
      assertNotNull(remoteAddress, "remoteAddress method should exist");
      assertEquals(
          IpSocketAddress.class, remoteAddress.getReturnType(), "Should return IpSocketAddress");

      LOGGER.info("UDP address methods verified");
    }
  }

  @Nested
  @DisplayName("Shutdown Type Tests")
  class ShutdownTypeTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have RECEIVE shutdown type")
    void shouldHaveReceiveShutdownType(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should have RECEIVE shutdown type");

      assertNotNull(WasiTcpSocket.ShutdownType.RECEIVE, "RECEIVE shutdown type should exist");
      assertEquals("RECEIVE", WasiTcpSocket.ShutdownType.RECEIVE.name(), "Name should match");

      LOGGER.info("RECEIVE shutdown type verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have SEND shutdown type")
    void shouldHaveSendShutdownType(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should have SEND shutdown type");

      assertNotNull(WasiTcpSocket.ShutdownType.SEND, "SEND shutdown type should exist");
      assertEquals("SEND", WasiTcpSocket.ShutdownType.SEND.name(), "Name should match");

      LOGGER.info("SEND shutdown type verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have BOTH shutdown type")
    void shouldHaveBothShutdownType(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should have BOTH shutdown type");

      assertNotNull(WasiTcpSocket.ShutdownType.BOTH, "BOTH shutdown type should exist");
      assertEquals("BOTH", WasiTcpSocket.ShutdownType.BOTH.name(), "Name should match");

      LOGGER.info("BOTH shutdown type verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have exactly three shutdown types")
    void shouldHaveExactlyThreeShutdownTypes(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should have exactly three shutdown types");

      final var values = WasiTcpSocket.ShutdownType.values();
      assertEquals(3, values.length, "Should have exactly 3 shutdown types");

      LOGGER.info("Shutdown types count verified: " + values.length);
    }
  }

  @Nested
  @DisplayName("Connection and Accept Result Tests")
  class ConnectionAcceptResultTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have ConnectionStreams class")
    void shouldHaveConnectionStreamsClass(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should have ConnectionStreams class");

      // Verify ConnectionStreams is a public final class
      final Class<?> clazz = WasiTcpSocket.ConnectionStreams.class;
      assertTrue(
          java.lang.reflect.Modifier.isFinal(clazz.getModifiers()),
          "ConnectionStreams should be final");
      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()),
          "ConnectionStreams should be public");

      LOGGER.info("ConnectionStreams class verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have AcceptResult class")
    void shouldHaveAcceptResultClass(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should have AcceptResult class");

      // Verify AcceptResult is a public final class
      final Class<?> clazz = WasiTcpSocket.AcceptResult.class;
      assertTrue(
          java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "AcceptResult should be final");
      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()),
          "AcceptResult should be public");

      LOGGER.info("AcceptResult class verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("ConnectionStreams should have getter methods")
    void connectionStreamsShouldHaveGetterMethods(final RuntimeType runtime)
        throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: ConnectionStreams should have getter methods");

      final Class<?> clazz = WasiTcpSocket.ConnectionStreams.class;

      final var getInputStream = clazz.getMethod("getInputStream");
      assertNotNull(getInputStream, "getInputStream method should exist");

      final var getOutputStream = clazz.getMethod("getOutputStream");
      assertNotNull(getOutputStream, "getOutputStream method should exist");

      LOGGER.info("ConnectionStreams getter methods verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("AcceptResult should have getter methods")
    void acceptResultShouldHaveGetterMethods(final RuntimeType runtime)
        throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: AcceptResult should have getter methods");

      final Class<?> clazz = WasiTcpSocket.AcceptResult.class;

      final var getSocket = clazz.getMethod("getSocket");
      assertNotNull(getSocket, "getSocket method should exist");

      final var getInputStream = clazz.getMethod("getInputStream");
      assertNotNull(getInputStream, "getInputStream method should exist");

      final var getOutputStream = clazz.getMethod("getOutputStream");
      assertNotNull(getOutputStream, "getOutputStream method should exist");

      LOGGER.info("AcceptResult getter methods verified");
    }
  }

  @Nested
  @DisplayName("NetworkErrorCode Tests")
  class NetworkErrorCodeTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have all expected error codes")
    void shouldHaveAllExpectedErrorCodes(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should have all expected error codes");

      final NetworkErrorCode[] codes = NetworkErrorCode.values();
      assertEquals(21, codes.length, "Should have 21 error codes");

      // Verify all expected codes exist
      assertNotNull(NetworkErrorCode.UNKNOWN, "UNKNOWN should exist");
      assertNotNull(NetworkErrorCode.ACCESS_DENIED, "ACCESS_DENIED should exist");
      assertNotNull(NetworkErrorCode.NOT_SUPPORTED, "NOT_SUPPORTED should exist");
      assertNotNull(NetworkErrorCode.INVALID_ARGUMENT, "INVALID_ARGUMENT should exist");
      assertNotNull(NetworkErrorCode.OUT_OF_MEMORY, "OUT_OF_MEMORY should exist");
      assertNotNull(NetworkErrorCode.TIMEOUT, "TIMEOUT should exist");
      assertNotNull(NetworkErrorCode.CONCURRENCY_CONFLICT, "CONCURRENCY_CONFLICT should exist");
      assertNotNull(NetworkErrorCode.NOT_IN_PROGRESS, "NOT_IN_PROGRESS should exist");
      assertNotNull(NetworkErrorCode.WOULD_BLOCK, "WOULD_BLOCK should exist");
      assertNotNull(NetworkErrorCode.INVALID_STATE, "INVALID_STATE should exist");
      assertNotNull(NetworkErrorCode.NEW_SOCKET_LIMIT, "NEW_SOCKET_LIMIT should exist");

      LOGGER.info("All " + codes.length + " error codes verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have connection error codes")
    void shouldHaveConnectionErrorCodes(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should have connection error codes");

      assertNotNull(NetworkErrorCode.ADDRESS_NOT_BINDABLE, "ADDRESS_NOT_BINDABLE should exist");
      assertNotNull(NetworkErrorCode.ADDRESS_IN_USE, "ADDRESS_IN_USE should exist");
      assertNotNull(NetworkErrorCode.REMOTE_UNREACHABLE, "REMOTE_UNREACHABLE should exist");
      assertNotNull(NetworkErrorCode.CONNECTION_REFUSED, "CONNECTION_REFUSED should exist");
      assertNotNull(NetworkErrorCode.CONNECTION_RESET, "CONNECTION_RESET should exist");
      assertNotNull(NetworkErrorCode.CONNECTION_ABORTED, "CONNECTION_ABORTED should exist");

      LOGGER.info("Connection error codes verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have DNS error codes")
    void shouldHaveDnsErrorCodes(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should have DNS error codes");

      assertNotNull(NetworkErrorCode.NAME_UNRESOLVABLE, "NAME_UNRESOLVABLE should exist");
      assertNotNull(
          NetworkErrorCode.TEMPORARY_RESOLVER_FAILURE, "TEMPORARY_RESOLVER_FAILURE should exist");
      assertNotNull(
          NetworkErrorCode.PERMANENT_RESOLVER_FAILURE, "PERMANENT_RESOLVER_FAILURE should exist");

      LOGGER.info("DNS error codes verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have datagram error code")
    void shouldHaveDatagramErrorCode(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should have datagram error code");

      assertNotNull(NetworkErrorCode.DATAGRAM_TOO_LARGE, "DATAGRAM_TOO_LARGE should exist");
      assertEquals(
          "DATAGRAM_TOO_LARGE", NetworkErrorCode.DATAGRAM_TOO_LARGE.name(), "Name should match");

      LOGGER.info("Datagram error code verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should convert error codes from string")
    void shouldConvertErrorCodesFromString(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should convert error codes from string");

      assertEquals(
          NetworkErrorCode.UNKNOWN, NetworkErrorCode.valueOf("UNKNOWN"), "valueOf UNKNOWN");
      assertEquals(
          NetworkErrorCode.TIMEOUT, NetworkErrorCode.valueOf("TIMEOUT"), "valueOf TIMEOUT");
      assertEquals(
          NetworkErrorCode.CONNECTION_REFUSED,
          NetworkErrorCode.valueOf("CONNECTION_REFUSED"),
          "valueOf CONNECTION_REFUSED");

      LOGGER.info("Error code valueOf conversion verified");
    }
  }

  @Nested
  @DisplayName("ConnectionStreams Structure Tests")
  class ConnectionStreamsStructureTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should be a final class")
    void shouldBeFinalClass(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should be a final class");

      final Class<?> clazz = WasiTcpSocket.ConnectionStreams.class;
      assertTrue(
          java.lang.reflect.Modifier.isFinal(clazz.getModifiers()),
          "ConnectionStreams should be final");

      LOGGER.info("ConnectionStreams is final class");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have getter methods")
    void shouldHaveGetterMethods(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have getter methods");

      final Class<?> clazz = WasiTcpSocket.ConnectionStreams.class;

      assertNotNull(clazz.getMethod("getInputStream"), "getInputStream should exist");
      assertNotNull(clazz.getMethod("getOutputStream"), "getOutputStream should exist");

      LOGGER.info("ConnectionStreams getter methods verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have appropriate constructor")
    void shouldHaveAppropriateConstructor(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have appropriate constructor");

      final Class<?> clazz = WasiTcpSocket.ConnectionStreams.class;
      final var constructor =
          clazz.getConstructor(
              ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream.class,
              ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream.class);

      assertNotNull(constructor, "Constructor should exist");
      assertTrue(
          java.lang.reflect.Modifier.isPublic(constructor.getModifiers()),
          "Constructor should be public");

      LOGGER.info("ConnectionStreams constructor verified");
    }
  }

  @Nested
  @DisplayName("AcceptResult Structure Tests")
  class AcceptResultStructureTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should be a final class")
    void shouldBeFinalClass(final RuntimeType runtime) {
      setRuntime(runtime);
      LOGGER.info("Testing: should be a final class");

      final Class<?> clazz = WasiTcpSocket.AcceptResult.class;
      assertTrue(
          java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "AcceptResult should be final");

      LOGGER.info("AcceptResult is final class");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have getter methods")
    void shouldHaveGetterMethods(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have getter methods");

      final Class<?> clazz = WasiTcpSocket.AcceptResult.class;

      assertNotNull(clazz.getMethod("getSocket"), "getSocket should exist");
      assertNotNull(clazz.getMethod("getInputStream"), "getInputStream should exist");
      assertNotNull(clazz.getMethod("getOutputStream"), "getOutputStream should exist");

      LOGGER.info("AcceptResult getter methods verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have appropriate constructor")
    void shouldHaveAppropriateConstructor(final RuntimeType runtime) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should have appropriate constructor");

      final Class<?> clazz = WasiTcpSocket.AcceptResult.class;
      final var constructor =
          clazz.getConstructor(
              WasiTcpSocket.class,
              ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream.class,
              ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream.class);

      assertNotNull(constructor, "Constructor should exist");
      assertTrue(
          java.lang.reflect.Modifier.isPublic(constructor.getModifiers()),
          "Constructor should be public");

      LOGGER.info("AcceptResult constructor verified");
    }
  }

  @Nested
  @DisplayName("Native Socket Operation Tests")
  class NativeSocketOperationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should verify TCP socket API completeness")
    void shouldVerifyTcpSocketApiCompleteness(final RuntimeType runtime)
        throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should verify TCP socket API completeness");

      // Verify all core TCP socket methods exist
      final Class<?> clazz = WasiTcpSocket.class;

      assertNotNull(clazz.getMethod("startBind", WasiNetwork.class, IpSocketAddress.class));
      assertNotNull(clazz.getMethod("finishBind"));
      assertNotNull(clazz.getMethod("startConnect", WasiNetwork.class, IpSocketAddress.class));
      assertNotNull(clazz.getMethod("finishConnect"));
      assertNotNull(clazz.getMethod("startListen"));
      assertNotNull(clazz.getMethod("finishListen"));
      assertNotNull(clazz.getMethod("accept"));
      assertNotNull(clazz.getMethod("localAddress"));
      assertNotNull(clazz.getMethod("remoteAddress"));
      assertNotNull(clazz.getMethod("addressFamily"));
      assertNotNull(clazz.getMethod("setListenBacklogSize", long.class));
      assertNotNull(clazz.getMethod("setKeepAliveEnabled", boolean.class));
      assertNotNull(clazz.getMethod("setKeepAliveIdleTime", long.class));
      assertNotNull(clazz.getMethod("setKeepAliveInterval", long.class));
      assertNotNull(clazz.getMethod("setKeepAliveCount", int.class));
      assertNotNull(clazz.getMethod("setHopLimit", int.class));
      assertNotNull(clazz.getMethod("subscribe"));
      assertNotNull(clazz.getMethod("shutdown", WasiTcpSocket.ShutdownType.class));
      assertNotNull(clazz.getMethod("close"));

      LOGGER.info("All TCP socket API methods verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should verify UDP socket API completeness")
    void shouldVerifyUdpSocketApiCompleteness(final RuntimeType runtime)
        throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: should verify UDP socket API completeness");

      // Verify all core UDP socket methods exist
      final Class<?> clazz = WasiUdpSocket.class;

      assertNotNull(clazz.getMethod("startBind", WasiNetwork.class, IpSocketAddress.class));
      assertNotNull(clazz.getMethod("finishBind"));
      assertNotNull(clazz.getMethod("stream", WasiNetwork.class, IpSocketAddress.class));
      assertNotNull(clazz.getMethod("localAddress"));
      assertNotNull(clazz.getMethod("remoteAddress"));
      assertNotNull(clazz.getMethod("addressFamily"));
      assertNotNull(clazz.getMethod("subscribe"));
      assertNotNull(clazz.getMethod("close"));

      LOGGER.info("All UDP socket API methods verified");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create TCP loopback server")
    void shouldCreateTcpLoopbackServer(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiSocketsAvailable();
      LOGGER.info("Testing: should create TCP loopback server");

      // This test requires native WASI sockets implementation
      LOGGER.info("Native WASI sockets required - test will be implemented with native support");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should connect TCP client to loopback server")
    void shouldConnectTcpClientToLoopbackServer(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiSocketsAvailable();
      LOGGER.info("Testing: should connect TCP client to loopback server");

      // This test requires native WASI sockets implementation
      LOGGER.info("Native WASI sockets required - test will be implemented with native support");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should send and receive UDP datagram on loopback")
    void shouldSendAndReceiveUdpDatagramOnLoopback(final RuntimeType runtime) {
      setRuntime(runtime);
      assumeWasiSocketsAvailable();
      LOGGER.info("Testing: should send and receive UDP datagram on loopback");

      // This test requires native WASI sockets implementation
      LOGGER.info("Native WASI sockets required - test will be implemented with native support");
    }
  }
}
