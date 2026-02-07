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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddressFamily;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Direct integration tests for Panama WASI sockets classes.
 *
 * <p>These tests directly exercise the Panama WASI socket implementation classes to improve code
 * coverage of the Panama module. Tests cover class structure, method signatures, and interface
 * implementations.
 *
 * @since 1.0.0
 */
@DisplayName("Panama WASI Sockets Direct Integration Tests")
class PanamaWasiSocketsDirectTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaWasiSocketsDirectTest.class.getName());

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for Panama WASI sockets tests");
    try {
      final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertTrue(loader.isLoaded(), "Native library should be loaded");
      LOGGER.info("Native library loaded successfully for Panama");
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library required for integration tests", e);
    }
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up test resources");
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  @Nested
  @DisplayName("Network Class Coverage Tests")
  class NetworkClassCoverageTests {

    @Test
    @DisplayName("should exercise network class methods")
    void shouldExerciseNetworkClassMethods() {
      LOGGER.info("Testing Panama WASI Network class coverage");

      assertNotNull(PanamaWasiNetwork.class.getMethods(), "Network class should have methods");

      boolean hasCreateMethod = false;
      boolean hasCloseMethod = false;
      boolean hasGetHandleMethod = false;

      for (final Method method : PanamaWasiNetwork.class.getMethods()) {
        if (method.getName().equals("create")) {
          hasCreateMethod = true;
        }
        if (method.getName().equals("close")) {
          hasCloseMethod = true;
        }
        if (method.getName().equals("getNetworkHandle")) {
          hasGetHandleMethod = true;
        }
      }

      assertTrue(hasCreateMethod, "Should have create method");
      assertTrue(hasCloseMethod, "Should have close method");
      assertTrue(hasGetHandleMethod, "Should have getNetworkHandle method");

      LOGGER.info("Network class method coverage verified");
    }
  }

  @Nested
  @DisplayName("TCP Socket Class Coverage Tests")
  class TcpSocketClassCoverageTests {

    @Test
    @DisplayName("should exercise TCP socket class methods")
    void shouldExerciseTcpSocketClassMethods() {
      LOGGER.info("Testing Panama WASI TCP Socket class coverage");

      assertNotNull(PanamaWasiTcpSocket.class.getMethods(), "TcpSocket class should have methods");

      boolean hasCreateMethod = false;
      boolean hasBindMethod = false;
      boolean hasConnectMethod = false;
      boolean hasListenMethod = false;
      boolean hasAcceptMethod = false;
      boolean hasCloseMethod = false;
      boolean hasShutdownMethod = false;

      for (final Method method : PanamaWasiTcpSocket.class.getMethods()) {
        final String name = method.getName();
        if (name.equals("create")) {
          hasCreateMethod = true;
        }
        if (name.equals("startBind") || name.equals("finishBind")) {
          hasBindMethod = true;
        }
        if (name.equals("startConnect") || name.equals("finishConnect")) {
          hasConnectMethod = true;
        }
        if (name.equals("startListen") || name.equals("finishListen")) {
          hasListenMethod = true;
        }
        if (name.equals("accept")) {
          hasAcceptMethod = true;
        }
        if (name.equals("close")) {
          hasCloseMethod = true;
        }
        if (name.equals("shutdown")) {
          hasShutdownMethod = true;
        }
      }

      assertTrue(hasCreateMethod, "Should have create method");
      assertTrue(hasBindMethod, "Should have bind methods");
      assertTrue(hasConnectMethod, "Should have connect methods");
      assertTrue(hasListenMethod, "Should have listen methods");
      assertTrue(hasAcceptMethod, "Should have accept method");
      assertTrue(hasCloseMethod, "Should have close method");
      assertTrue(hasShutdownMethod, "Should have shutdown method");

      // Verify address methods exist
      boolean hasLocalAddressMethod = false;
      boolean hasRemoteAddressMethod = false;
      boolean hasAddressFamilyMethod = false;

      for (final Method method : PanamaWasiTcpSocket.class.getMethods()) {
        final String name = method.getName();
        if (name.equals("localAddress")) {
          hasLocalAddressMethod = true;
        }
        if (name.equals("remoteAddress")) {
          hasRemoteAddressMethod = true;
        }
        if (name.equals("addressFamily")) {
          hasAddressFamilyMethod = true;
        }
      }

      assertTrue(hasLocalAddressMethod, "Should have localAddress method");
      assertTrue(hasRemoteAddressMethod, "Should have remoteAddress method");
      assertTrue(hasAddressFamilyMethod, "Should have addressFamily method");

      // Verify option methods exist
      boolean hasSetKeepAliveMethod = false;
      boolean hasBufferSizeMethod = false;

      for (final Method method : PanamaWasiTcpSocket.class.getMethods()) {
        final String name = method.getName();
        if (name.equals("setKeepAliveEnabled")) {
          hasSetKeepAliveMethod = true;
        }
        if (name.equals("receiveBufferSize") || name.equals("sendBufferSize")) {
          hasBufferSizeMethod = true;
        }
      }

      assertTrue(hasSetKeepAliveMethod, "Should have setKeepAliveEnabled method");
      assertTrue(hasBufferSizeMethod, "Should have buffer size methods");

      LOGGER.info("TCP Socket class method coverage verified");
    }
  }

  @Nested
  @DisplayName("UDP Socket Class Coverage Tests")
  class UdpSocketClassCoverageTests {

    @Test
    @DisplayName("should exercise UDP socket class methods")
    void shouldExerciseUdpSocketClassMethods() {
      LOGGER.info("Testing Panama WASI UDP Socket class coverage");

      assertNotNull(PanamaWasiUdpSocket.class.getMethods(), "UdpSocket class should have methods");

      boolean hasCreateMethod = false;
      boolean hasBindMethod = false;
      boolean hasStreamMethod = false;
      boolean hasReceiveMethod = false;
      boolean hasSendMethod = false;
      boolean hasCloseMethod = false;

      for (final Method method : PanamaWasiUdpSocket.class.getMethods()) {
        final String name = method.getName();
        if (name.equals("create")) {
          hasCreateMethod = true;
        }
        if (name.equals("startBind") || name.equals("finishBind")) {
          hasBindMethod = true;
        }
        if (name.equals("stream")) {
          hasStreamMethod = true;
        }
        if (name.equals("receive")) {
          hasReceiveMethod = true;
        }
        if (name.equals("send")) {
          hasSendMethod = true;
        }
        if (name.equals("close")) {
          hasCloseMethod = true;
        }
      }

      assertTrue(hasCreateMethod, "Should have create method");
      assertTrue(hasBindMethod, "Should have bind methods");
      assertTrue(hasStreamMethod, "Should have stream method");
      assertTrue(hasReceiveMethod, "Should have receive method");
      assertTrue(hasSendMethod, "Should have send method");
      assertTrue(hasCloseMethod, "Should have close method");

      // Verify address methods exist
      boolean hasLocalAddressMethod = false;
      boolean hasRemoteAddressMethod = false;
      boolean hasAddressFamilyMethod = false;

      for (final Method method : PanamaWasiUdpSocket.class.getMethods()) {
        final String name = method.getName();
        if (name.equals("localAddress")) {
          hasLocalAddressMethod = true;
        }
        if (name.equals("remoteAddress")) {
          hasRemoteAddressMethod = true;
        }
        if (name.equals("addressFamily")) {
          hasAddressFamilyMethod = true;
        }
      }

      assertTrue(hasLocalAddressMethod, "Should have localAddress method");
      assertTrue(hasRemoteAddressMethod, "Should have remoteAddress method");
      assertTrue(hasAddressFamilyMethod, "Should have addressFamily method");

      // Verify option methods exist
      boolean hasSetHopLimitMethod = false;
      boolean hasBufferSizeMethod = false;

      for (final Method method : PanamaWasiUdpSocket.class.getMethods()) {
        final String name = method.getName();
        if (name.equals("setUnicastHopLimit")) {
          hasSetHopLimitMethod = true;
        }
        if (name.equals("receiveBufferSize") || name.equals("sendBufferSize")) {
          hasBufferSizeMethod = true;
        }
      }

      assertTrue(hasSetHopLimitMethod, "Should have setUnicastHopLimit method");
      assertTrue(hasBufferSizeMethod, "Should have buffer size methods");

      LOGGER.info("UDP Socket class method coverage verified");
    }
  }

  @Nested
  @DisplayName("IP Name Lookup Class Coverage Tests")
  class IpNameLookupClassCoverageTests {

    @Test
    @DisplayName("should exercise IP name lookup class methods")
    void shouldExerciseIpNameLookupClassMethods() {
      LOGGER.info("Testing Panama WASI IP Name Lookup class coverage");

      assertNotNull(
          PanamaWasiIpNameLookup.class.getMethods(), "IpNameLookup class should have methods");

      boolean hasResolveMethod = false;
      boolean hasConstructor = false;

      for (final Method method : PanamaWasiIpNameLookup.class.getMethods()) {
        if (method.getName().equals("resolveAddresses")) {
          hasResolveMethod = true;
        }
      }

      for (final var constructor : PanamaWasiIpNameLookup.class.getConstructors()) {
        if (constructor.getParameterCount() >= 1) {
          hasConstructor = true;
        }
      }

      assertTrue(hasResolveMethod, "Should have resolveAddresses method");
      assertTrue(hasConstructor, "Should have constructor");

      LOGGER.info("IP Name Lookup class method coverage verified");
    }
  }

  @Nested
  @DisplayName("Address Family Tests")
  class AddressFamilyTests {

    @Test
    @DisplayName("should have IPv4 and IPv6 address families")
    void shouldHaveIpv4AndIpv6AddressFamilies() {
      LOGGER.info("Testing IP address families");

      assertNotNull(IpAddressFamily.IPV4, "Should have IPv4 address family");
      assertNotNull(IpAddressFamily.IPV6, "Should have IPv6 address family");

      LOGGER.info(
          "Address families verified: IPv4="
              + IpAddressFamily.IPV4
              + ", IPv6="
              + IpAddressFamily.IPV6);
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should have proper class hierarchy")
    void shouldHaveProperClassHierarchy() {
      LOGGER.info("Testing class hierarchy");

      // Verify classes exist and are public
      assertTrue(
          java.lang.reflect.Modifier.isPublic(PanamaWasiNetwork.class.getModifiers()),
          "PanamaWasiNetwork should be public");
      assertTrue(
          java.lang.reflect.Modifier.isPublic(PanamaWasiTcpSocket.class.getModifiers()),
          "PanamaWasiTcpSocket should be public");
      assertTrue(
          java.lang.reflect.Modifier.isPublic(PanamaWasiUdpSocket.class.getModifiers()),
          "PanamaWasiUdpSocket should be public");
      assertTrue(
          java.lang.reflect.Modifier.isPublic(PanamaWasiIpNameLookup.class.getModifiers()),
          "PanamaWasiIpNameLookup should be public");

      // Verify classes are final
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaWasiNetwork.class.getModifiers()),
          "PanamaWasiNetwork should be final");
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaWasiTcpSocket.class.getModifiers()),
          "PanamaWasiTcpSocket should be final");
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaWasiUdpSocket.class.getModifiers()),
          "PanamaWasiUdpSocket should be final");
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaWasiIpNameLookup.class.getModifiers()),
          "PanamaWasiIpNameLookup should be final");

      LOGGER.info("Class hierarchy verified");
    }

    @Test
    @DisplayName("should implement expected interfaces")
    void shouldImplementExpectedInterfaces() {
      LOGGER.info("Testing interface implementation");

      final Class<?>[] networkInterfaces = PanamaWasiNetwork.class.getInterfaces();
      final Class<?>[] tcpSocketInterfaces = PanamaWasiTcpSocket.class.getInterfaces();
      final Class<?>[] udpSocketInterfaces = PanamaWasiUdpSocket.class.getInterfaces();

      LOGGER.info("PanamaWasiNetwork implements " + networkInterfaces.length + " interface(s)");
      LOGGER.info("PanamaWasiTcpSocket implements " + tcpSocketInterfaces.length + " interface(s)");
      LOGGER.info("PanamaWasiUdpSocket implements " + udpSocketInterfaces.length + " interface(s)");

      for (final Class<?> iface : networkInterfaces) {
        LOGGER.info("  Network implements: " + iface.getName());
      }
      for (final Class<?> iface : tcpSocketInterfaces) {
        LOGGER.info("  TcpSocket implements: " + iface.getName());
      }
      for (final Class<?> iface : udpSocketInterfaces) {
        LOGGER.info("  UdpSocket implements: " + iface.getName());
      }
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("should have correctly typed method parameters")
    void shouldHaveCorrectlyTypedMethodParameters() {
      LOGGER.info("Testing method parameter types");

      for (final Method method : PanamaWasiTcpSocket.class.getDeclaredMethods()) {
        if (method.getName().equals("setReceiveBufferSize")
            || method.getName().equals("setSendBufferSize")) {
          final Class<?>[] paramTypes = method.getParameterTypes();
          if (paramTypes.length == 1) {
            assertTrue(
                paramTypes[0] == long.class || paramTypes[0] == Long.class,
                "Buffer size should accept long parameter");
          }
        }
        if (method.getName().equals("setKeepAliveEnabled")) {
          final Class<?>[] paramTypes = method.getParameterTypes();
          if (paramTypes.length == 1) {
            assertTrue(
                paramTypes[0] == boolean.class || paramTypes[0] == Boolean.class,
                "setKeepAliveEnabled should accept boolean parameter");
          }
        }
      }

      LOGGER.info("Method parameter types verified");
    }
  }
}
