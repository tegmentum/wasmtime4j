/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import ai.tegmentum.wasmtime4j.wasi.io.WasiStreamError;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddressFamily;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpSocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.NetworkErrorCode;
import ai.tegmentum.wasmtime4j.wasi.sockets.ResolveAddressStream;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiIpNameLookup;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiNetwork;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiTcpSocket;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiUdpSocket;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WASI sockets and IO subpackages.
 *
 * <p>This test suite validates the API contracts for WASI Preview 2 networking and stream
 * interfaces including sockets, streams, and address types.
 */
@DisplayName("WASI Sockets and IO Package Tests")
class WasiSocketsPackageTest {

  // ========================================================================
  // IpAddressFamily Tests
  // ========================================================================

  @Nested
  @DisplayName("IpAddressFamily Tests")
  class IpAddressFamilyTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(IpAddressFamily.class.isEnum(), "IpAddressFamily should be an enum");
    }

    @Test
    @DisplayName("should have IPV4 constant")
    void shouldHaveIpv4Constant() {
      IpAddressFamily ipv4 = IpAddressFamily.IPV4;
      assertNotNull(ipv4, "IPV4 constant should exist");
      assertEquals("IPV4", ipv4.name(), "IPV4 constant should have correct name");
    }

    @Test
    @DisplayName("should have IPV6 constant")
    void shouldHaveIpv6Constant() {
      IpAddressFamily ipv6 = IpAddressFamily.IPV6;
      assertNotNull(ipv6, "IPV6 constant should exist");
      assertEquals("IPV6", ipv6.name(), "IPV6 constant should have correct name");
    }

    @Test
    @DisplayName("should have exactly two values")
    void shouldHaveExactlyTwoValues() {
      assertEquals(
          2, IpAddressFamily.values().length, "IpAddressFamily should have exactly 2 values");
    }
  }

  // ========================================================================
  // NetworkErrorCode Tests
  // ========================================================================

  @Nested
  @DisplayName("NetworkErrorCode Tests")
  class NetworkErrorCodeTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NetworkErrorCode.class.isEnum(), "NetworkErrorCode should be an enum");
    }

    @Test
    @DisplayName("should have multiple error codes")
    void shouldHaveMultipleErrorCodes() {
      NetworkErrorCode[] values = NetworkErrorCode.values();
      assertTrue(values.length > 5, "NetworkErrorCode should have multiple error codes");
    }
  }

  // ========================================================================
  // IpAddress Tests (Variant Pattern - holds either Ipv4Address or Ipv6Address)
  // ========================================================================

  @Nested
  @DisplayName("IpAddress Tests")
  class IpAddressTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(IpAddress.class.getModifiers()), "IpAddress should be a final class");
      assertFalse(IpAddress.class.isInterface(), "IpAddress should not be an interface");
    }

    @Test
    @DisplayName("should have static ipv4 factory method")
    void shouldHaveStaticIpv4FactoryMethod() throws NoSuchMethodException {
      Method method = IpAddress.class.getMethod("ipv4", Ipv4Address.class);
      assertNotNull(method, "ipv4 factory method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ipv4 method should be static");
      assertEquals(IpAddress.class, method.getReturnType(), "Return type should be IpAddress");
    }

    @Test
    @DisplayName("should have static ipv6 factory method")
    void shouldHaveStaticIpv6FactoryMethod() throws NoSuchMethodException {
      Method method = IpAddress.class.getMethod("ipv6", Ipv6Address.class);
      assertNotNull(method, "ipv6 factory method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ipv6 method should be static");
      assertEquals(IpAddress.class, method.getReturnType(), "Return type should be IpAddress");
    }

    @Test
    @DisplayName("should have isIpv4 method")
    void shouldHaveIsIpv4Method() throws NoSuchMethodException {
      Method method = IpAddress.class.getMethod("isIpv4");
      assertNotNull(method, "isIpv4 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getIpv4 method")
    void shouldHaveGetIpv4Method() throws NoSuchMethodException {
      Method method = IpAddress.class.getMethod("getIpv4");
      assertNotNull(method, "getIpv4 method should exist");
      assertEquals(Ipv4Address.class, method.getReturnType(), "Return type should be Ipv4Address");
    }

    @Test
    @DisplayName("should have getIpv6 method")
    void shouldHaveGetIpv6Method() throws NoSuchMethodException {
      Method method = IpAddress.class.getMethod("getIpv6");
      assertNotNull(method, "getIpv6 method should exist");
      assertEquals(Ipv6Address.class, method.getReturnType(), "Return type should be Ipv6Address");
    }
  }

  // ========================================================================
  // Ipv4Address Tests (Standalone final class for IPv4 addresses)
  // ========================================================================

  @Nested
  @DisplayName("Ipv4Address Tests")
  class Ipv4AddressTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(Ipv4Address.class.getModifiers()),
          "Ipv4Address should be a final class");
      assertFalse(Ipv4Address.class.isInterface(), "Ipv4Address should not be an interface");
    }

    @Test
    @DisplayName("should have constructor with byte array")
    void shouldHaveConstructorWithByteArray() throws NoSuchMethodException {
      var constructor = Ipv4Address.class.getConstructor(byte[].class);
      assertNotNull(constructor, "Constructor with byte[] should exist");
    }

    @Test
    @DisplayName("should have getOctets method")
    void shouldHaveGetOctetsMethod() throws NoSuchMethodException {
      Method method = Ipv4Address.class.getMethod("getOctets");
      assertNotNull(method, "getOctets method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Return type should be byte[]");
    }
  }

  // ========================================================================
  // Ipv6Address Tests (Standalone final class for IPv6 addresses)
  // ========================================================================

  @Nested
  @DisplayName("Ipv6Address Tests")
  class Ipv6AddressTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(Ipv6Address.class.getModifiers()),
          "Ipv6Address should be a final class");
      assertFalse(Ipv6Address.class.isInterface(), "Ipv6Address should not be an interface");
    }

    @Test
    @DisplayName("should have constructor with short array")
    void shouldHaveConstructorWithShortArray() throws NoSuchMethodException {
      var constructor = Ipv6Address.class.getConstructor(short[].class);
      assertNotNull(constructor, "Constructor with short[] should exist");
    }

    @Test
    @DisplayName("should have getSegments method")
    void shouldHaveGetSegmentsMethod() throws NoSuchMethodException {
      Method method = Ipv6Address.class.getMethod("getSegments");
      assertNotNull(method, "getSegments method should exist");
      assertEquals(short[].class, method.getReturnType(), "Return type should be short[]");
    }
  }

  // ========================================================================
  // IpSocketAddress Tests (Variant Pattern - holds either Ipv4SocketAddress or Ipv6SocketAddress)
  // ========================================================================

  @Nested
  @DisplayName("IpSocketAddress Tests")
  class IpSocketAddressTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(IpSocketAddress.class.getModifiers()),
          "IpSocketAddress should be a final class");
      assertFalse(
          IpSocketAddress.class.isInterface(), "IpSocketAddress should not be an interface");
    }

    @Test
    @DisplayName("should have static ipv4 factory method")
    void shouldHaveStaticIpv4FactoryMethod() throws NoSuchMethodException {
      Method method = IpSocketAddress.class.getMethod("ipv4", Ipv4SocketAddress.class);
      assertNotNull(method, "ipv4 factory method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ipv4 method should be static");
      assertEquals(
          IpSocketAddress.class, method.getReturnType(), "Return type should be IpSocketAddress");
    }

    @Test
    @DisplayName("should have static ipv6 factory method")
    void shouldHaveStaticIpv6FactoryMethod() throws NoSuchMethodException {
      Method method = IpSocketAddress.class.getMethod("ipv6", Ipv6SocketAddress.class);
      assertNotNull(method, "ipv6 factory method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ipv6 method should be static");
      assertEquals(
          IpSocketAddress.class, method.getReturnType(), "Return type should be IpSocketAddress");
    }

    @Test
    @DisplayName("should have isIpv4 method")
    void shouldHaveIsIpv4Method() throws NoSuchMethodException {
      Method method = IpSocketAddress.class.getMethod("isIpv4");
      assertNotNull(method, "isIpv4 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getIpv4 method")
    void shouldHaveGetIpv4Method() throws NoSuchMethodException {
      Method method = IpSocketAddress.class.getMethod("getIpv4");
      assertNotNull(method, "getIpv4 method should exist");
      assertEquals(
          Ipv4SocketAddress.class,
          method.getReturnType(),
          "Return type should be Ipv4SocketAddress");
    }

    @Test
    @DisplayName("should have getIpv6 method")
    void shouldHaveGetIpv6Method() throws NoSuchMethodException {
      Method method = IpSocketAddress.class.getMethod("getIpv6");
      assertNotNull(method, "getIpv6 method should exist");
      assertEquals(
          Ipv6SocketAddress.class,
          method.getReturnType(),
          "Return type should be Ipv6SocketAddress");
    }
  }

  // ========================================================================
  // Ipv4SocketAddress Tests (Standalone final class for IPv4 socket addresses)
  // ========================================================================

  @Nested
  @DisplayName("Ipv4SocketAddress Tests")
  class Ipv4SocketAddressTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(Ipv4SocketAddress.class.getModifiers()),
          "Ipv4SocketAddress should be a final class");
      assertFalse(
          Ipv4SocketAddress.class.isInterface(), "Ipv4SocketAddress should not be an interface");
    }

    @Test
    @DisplayName("should have getPort method")
    void shouldHaveGetPortMethod() throws NoSuchMethodException {
      Method method = Ipv4SocketAddress.class.getMethod("getPort");
      assertNotNull(method, "getPort method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getAddress method")
    void shouldHaveGetAddressMethod() throws NoSuchMethodException {
      Method method = Ipv4SocketAddress.class.getMethod("getAddress");
      assertNotNull(method, "getAddress method should exist");
      assertEquals(Ipv4Address.class, method.getReturnType(), "Return type should be Ipv4Address");
    }
  }

  // ========================================================================
  // Ipv6SocketAddress Tests (Standalone final class for IPv6 socket addresses)
  // ========================================================================

  @Nested
  @DisplayName("Ipv6SocketAddress Tests")
  class Ipv6SocketAddressTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(Ipv6SocketAddress.class.getModifiers()),
          "Ipv6SocketAddress should be a final class");
      assertFalse(
          Ipv6SocketAddress.class.isInterface(), "Ipv6SocketAddress should not be an interface");
    }

    @Test
    @DisplayName("should have getPort method")
    void shouldHaveGetPortMethod() throws NoSuchMethodException {
      Method method = Ipv6SocketAddress.class.getMethod("getPort");
      assertNotNull(method, "getPort method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getAddress method")
    void shouldHaveGetAddressMethod() throws NoSuchMethodException {
      Method method = Ipv6SocketAddress.class.getMethod("getAddress");
      assertNotNull(method, "getAddress method should exist");
      assertEquals(Ipv6Address.class, method.getReturnType(), "Return type should be Ipv6Address");
    }

    @Test
    @DisplayName("should have getFlowInfo method")
    void shouldHaveGetFlowInfoMethod() throws NoSuchMethodException {
      Method method = Ipv6SocketAddress.class.getMethod("getFlowInfo");
      assertNotNull(method, "getFlowInfo method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have getScopeId method")
    void shouldHaveGetScopeIdMethod() throws NoSuchMethodException {
      Method method = Ipv6SocketAddress.class.getMethod("getScopeId");
      assertNotNull(method, "getScopeId method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }
  }

  // ========================================================================
  // WasiNetwork Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiNetwork Tests")
  class WasiNetworkTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiNetwork.class.isInterface(), "WasiNetwork should be an interface");
    }
  }

  // ========================================================================
  // WasiTcpSocket Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiTcpSocket Tests")
  class WasiTcpSocketTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiTcpSocket.class.isInterface(), "WasiTcpSocket should be an interface");
    }

    @Test
    @DisplayName("should have startBind method")
    void shouldHaveStartBindMethod() throws NoSuchMethodException {
      Method method =
          WasiTcpSocket.class.getMethod("startBind", WasiNetwork.class, IpSocketAddress.class);
      assertNotNull(method, "startBind method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have finishBind method")
    void shouldHaveFinishBindMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("finishBind");
      assertNotNull(method, "finishBind method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have startConnect method")
    void shouldHaveStartConnectMethod() throws NoSuchMethodException {
      Method method =
          WasiTcpSocket.class.getMethod("startConnect", WasiNetwork.class, IpSocketAddress.class);
      assertNotNull(method, "startConnect method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have finishConnect method")
    void shouldHaveFinishConnectMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("finishConnect");
      assertNotNull(method, "finishConnect method should exist");
      assertEquals(
          WasiTcpSocket.ConnectionStreams.class,
          method.getReturnType(),
          "Return type should be ConnectionStreams");
    }

    @Test
    @DisplayName("should have startListen method")
    void shouldHaveStartListenMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("startListen");
      assertNotNull(method, "startListen method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have finishListen method")
    void shouldHaveFinishListenMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("finishListen");
      assertNotNull(method, "finishListen method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have accept method")
    void shouldHaveAcceptMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("accept");
      assertNotNull(method, "accept method should exist");
      assertEquals(
          WasiTcpSocket.AcceptResult.class,
          method.getReturnType(),
          "Return type should be AcceptResult");
    }

    @Test
    @DisplayName("should have localAddress method")
    void shouldHaveLocalAddressMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("localAddress");
      assertNotNull(method, "localAddress method should exist");
      assertEquals(
          IpSocketAddress.class, method.getReturnType(), "Return type should be IpSocketAddress");
    }

    @Test
    @DisplayName("should have remoteAddress method")
    void shouldHaveRemoteAddressMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("remoteAddress");
      assertNotNull(method, "remoteAddress method should exist");
      assertEquals(
          IpSocketAddress.class, method.getReturnType(), "Return type should be IpSocketAddress");
    }

    @Test
    @DisplayName("should have addressFamily method")
    void shouldHaveAddressFamilyMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("addressFamily");
      assertNotNull(method, "addressFamily method should exist");
      assertEquals(
          IpAddressFamily.class, method.getReturnType(), "Return type should be IpAddressFamily");
    }

    @Test
    @DisplayName("should have setListenBacklogSize method")
    void shouldHaveSetListenBacklogSizeMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("setListenBacklogSize", long.class);
      assertNotNull(method, "setListenBacklogSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have setKeepAliveEnabled method")
    void shouldHaveSetKeepAliveEnabledMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("setKeepAliveEnabled", boolean.class);
      assertNotNull(method, "setKeepAliveEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have receiveBufferSize method")
    void shouldHaveReceiveBufferSizeMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("receiveBufferSize");
      assertNotNull(method, "receiveBufferSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have sendBufferSize method")
    void shouldHaveSendBufferSizeMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("sendBufferSize");
      assertNotNull(method, "sendBufferSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(
          WasiPollable.class, method.getReturnType(), "Return type should be WasiPollable");
    }

    @Test
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("shutdown", WasiTcpSocket.ShutdownType.class);
      assertNotNull(method, "shutdown method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "startBind",
              "finishBind",
              "startConnect",
              "finishConnect",
              "startListen",
              "finishListen",
              "accept",
              "localAddress",
              "remoteAddress",
              "addressFamily",
              "setListenBacklogSize",
              "setKeepAliveEnabled",
              "setKeepAliveIdleTime",
              "setKeepAliveInterval",
              "setKeepAliveCount",
              "setHopLimit",
              "receiveBufferSize",
              "setReceiveBufferSize",
              "sendBufferSize",
              "setSendBufferSize",
              "subscribe",
              "shutdown",
              "close");

      Set<String> actualMethods =
          Arrays.stream(WasiTcpSocket.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "WasiTcpSocket should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // WasiTcpSocket.ShutdownType Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiTcpSocket.ShutdownType Tests")
  class ShutdownTypeTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasiTcpSocket.ShutdownType.class.isEnum(), "ShutdownType should be an enum");
    }

    @Test
    @DisplayName("should have RECEIVE constant")
    void shouldHaveReceiveConstant() {
      WasiTcpSocket.ShutdownType receive = WasiTcpSocket.ShutdownType.RECEIVE;
      assertNotNull(receive, "RECEIVE constant should exist");
    }

    @Test
    @DisplayName("should have SEND constant")
    void shouldHaveSendConstant() {
      WasiTcpSocket.ShutdownType send = WasiTcpSocket.ShutdownType.SEND;
      assertNotNull(send, "SEND constant should exist");
    }

    @Test
    @DisplayName("should have BOTH constant")
    void shouldHaveBothConstant() {
      WasiTcpSocket.ShutdownType both = WasiTcpSocket.ShutdownType.BOTH;
      assertNotNull(both, "BOTH constant should exist");
    }
  }

  // ========================================================================
  // WasiTcpSocket.ConnectionStreams Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiTcpSocket.ConnectionStreams Tests")
  class ConnectionStreamsTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiTcpSocket.ConnectionStreams.class.getModifiers()),
          "ConnectionStreams should be final");
    }

    @Test
    @DisplayName("should have getInputStream method")
    void shouldHaveGetInputStreamMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.ConnectionStreams.class.getMethod("getInputStream");
      assertNotNull(method, "getInputStream method should exist");
      assertEquals(
          WasiInputStream.class, method.getReturnType(), "Return type should be WasiInputStream");
    }

    @Test
    @DisplayName("should have getOutputStream method")
    void shouldHaveGetOutputStreamMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.ConnectionStreams.class.getMethod("getOutputStream");
      assertNotNull(method, "getOutputStream method should exist");
      assertEquals(
          WasiOutputStream.class, method.getReturnType(), "Return type should be WasiOutputStream");
    }
  }

  // ========================================================================
  // WasiTcpSocket.AcceptResult Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiTcpSocket.AcceptResult Tests")
  class AcceptResultTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiTcpSocket.AcceptResult.class.getModifiers()),
          "AcceptResult should be final");
    }

    @Test
    @DisplayName("should have getSocket method")
    void shouldHaveGetSocketMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.AcceptResult.class.getMethod("getSocket");
      assertNotNull(method, "getSocket method should exist");
      assertEquals(
          WasiTcpSocket.class, method.getReturnType(), "Return type should be WasiTcpSocket");
    }

    @Test
    @DisplayName("should have getInputStream method")
    void shouldHaveGetInputStreamMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.AcceptResult.class.getMethod("getInputStream");
      assertNotNull(method, "getInputStream method should exist");
      assertEquals(
          WasiInputStream.class, method.getReturnType(), "Return type should be WasiInputStream");
    }

    @Test
    @DisplayName("should have getOutputStream method")
    void shouldHaveGetOutputStreamMethod() throws NoSuchMethodException {
      Method method = WasiTcpSocket.AcceptResult.class.getMethod("getOutputStream");
      assertNotNull(method, "getOutputStream method should exist");
      assertEquals(
          WasiOutputStream.class, method.getReturnType(), "Return type should be WasiOutputStream");
    }
  }

  // ========================================================================
  // WasiUdpSocket Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiUdpSocket Tests")
  class WasiUdpSocketTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiUdpSocket.class.isInterface(), "WasiUdpSocket should be an interface");
    }
  }

  // ========================================================================
  // WasiIpNameLookup Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiIpNameLookup Tests")
  class WasiIpNameLookupTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiIpNameLookup.class.isInterface(), "WasiIpNameLookup should be an interface");
    }
  }

  // ========================================================================
  // ResolveAddressStream Tests
  // ========================================================================

  @Nested
  @DisplayName("ResolveAddressStream Tests")
  class ResolveAddressStreamTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ResolveAddressStream.class.isInterface(), "ResolveAddressStream should be an interface");
    }
  }

  // ========================================================================
  // WasiInputStream Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiInputStream Tests")
  class WasiInputStreamTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiInputStream.class.isInterface(), "WasiInputStream should be an interface");
    }

    @Test
    @DisplayName("should have read method")
    void shouldHaveReadMethod() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("read", long.class);
      assertNotNull(method, "read method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Return type should be byte[]");
    }

    @Test
    @DisplayName("should have blockingRead method")
    void shouldHaveBlockingReadMethod() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("blockingRead", long.class);
      assertNotNull(method, "blockingRead method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Return type should be byte[]");
    }

    @Test
    @DisplayName("should have skip method")
    void shouldHaveSkipMethod() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("skip", long.class);
      assertNotNull(method, "skip method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have blockingSkip method")
    void shouldHaveBlockingSkipMethod() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("blockingSkip", long.class);
      assertNotNull(method, "blockingSkip method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      Method method = WasiInputStream.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(
          WasiPollable.class, method.getReturnType(), "Return type should be WasiPollable");
    }
  }

  // ========================================================================
  // WasiOutputStream Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiOutputStream Tests")
  class WasiOutputStreamTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiOutputStream.class.isInterface(), "WasiOutputStream should be an interface");
    }
  }

  // ========================================================================
  // WasiPollable Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiPollable Tests")
  class WasiPollableTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiPollable.class.isInterface(), "WasiPollable should be an interface");
    }
  }

  // ========================================================================
  // WasiStreamError Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiStreamError Tests")
  class WasiStreamErrorTests {

    @Test
    @DisplayName("should be an enum or class")
    void shouldBeAnEnumOrClass() {
      assertNotNull(WasiStreamError.class, "WasiStreamError should exist");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("WasiTcpSocket should have at least 20 methods")
    void wasiTcpSocketShouldHaveMinimumMethods() {
      int methodCount = WasiTcpSocket.class.getDeclaredMethods().length;
      assertTrue(
          methodCount >= 20,
          "WasiTcpSocket should have at least 20 methods, found: " + methodCount);
    }

    @Test
    @DisplayName("WasiInputStream should have at least 4 methods")
    void wasiInputStreamShouldHaveMinimumMethods() {
      int methodCount = WasiInputStream.class.getDeclaredMethods().length;
      assertTrue(
          methodCount >= 4,
          "WasiInputStream should have at least 4 methods, found: " + methodCount);
    }
  }
}
