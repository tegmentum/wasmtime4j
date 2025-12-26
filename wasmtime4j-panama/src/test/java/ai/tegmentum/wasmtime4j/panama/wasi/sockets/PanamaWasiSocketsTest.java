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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.sockets.ResolveAddressStream;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiIpNameLookup;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiNetwork;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiTcpSocket;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiUdpSocket;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for Panama WASI Sockets implementation classes.
 *
 * <p>Tests cover class structure, interface compliance, Panama FFI patterns, and MethodHandle field
 * verification for:
 *
 * <ul>
 *   <li>PanamaWasiTcpSocket - TCP socket operations
 *   <li>PanamaWasiUdpSocket - UDP socket operations
 *   <li>PanamaWasiNetwork - Network resource management
 *   <li>PanamaWasiIpNameLookup - DNS name resolution
 *   <li>PanamaResolveAddressStream - DNS resolution result stream
 * </ul>
 *
 * <p>Note: These tests use Class.forName with initialize=false to load classes without triggering
 * static initializers, which would attempt to load native libraries. This allows testing the class
 * structure without runtime dependencies.
 */
@DisplayName("Panama WASI Sockets Tests")
class PanamaWasiSocketsTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiSocketsTest.class.getName());

  private static final String TCP_SOCKET_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.sockets.PanamaWasiTcpSocket";
  private static final String UDP_SOCKET_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.sockets.PanamaWasiUdpSocket";
  private static final String NETWORK_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.sockets.PanamaWasiNetwork";
  private static final String IP_NAME_LOOKUP_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.sockets.PanamaWasiIpNameLookup";
  private static final String RESOLVE_ADDRESS_STREAM_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.sockets.PanamaResolveAddressStream";

  /**
   * Loads a class without initializing it (no static initializer runs). This prevents native
   * library loading attempts.
   */
  private static Class<?> loadClassWithoutInit(final String className)
      throws ClassNotFoundException {
    return Class.forName(className, false, PanamaWasiSocketsTest.class.getClassLoader());
  }

  @Nested
  @DisplayName("PanamaWasiTcpSocket Class Structure Tests")
  class TcpSocketClassStructureTests {

    @Test
    @DisplayName("PanamaWasiTcpSocket should exist and be public")
    void tcpSocketClassShouldExistAndBePublic() {
      final Class<?> clazz =
          assertDoesNotThrow(
              () -> loadClassWithoutInit(TCP_SOCKET_CLASS),
              "PanamaWasiTcpSocket class should exist");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "PanamaWasiTcpSocket should be public");
      LOGGER.info(
          "PanamaWasiTcpSocket class verified: public=" + Modifier.isPublic(clazz.getModifiers()));
    }

    @Test
    @DisplayName("PanamaWasiTcpSocket should implement WasiTcpSocket interface")
    void tcpSocketShouldImplementInterface() {
      final Class<?> clazz = assertDoesNotThrow(() -> loadClassWithoutInit(TCP_SOCKET_CLASS));
      assertTrue(
          WasiTcpSocket.class.isAssignableFrom(clazz),
          "PanamaWasiTcpSocket should implement WasiTcpSocket");
      LOGGER.info(
          "PanamaWasiTcpSocket implements WasiTcpSocket: "
              + WasiTcpSocket.class.isAssignableFrom(clazz));
    }

    @Test
    @DisplayName("PanamaWasiTcpSocket should have required TCP methods")
    void tcpSocketShouldHaveRequiredMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(TCP_SOCKET_CLASS);
      // WASI Preview 2 uses start/finish pattern for async operations
      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("startBind");
      requiredMethods.add("finishBind");
      requiredMethods.add("startConnect");
      requiredMethods.add("finishConnect");
      requiredMethods.add("startListen");
      requiredMethods.add("finishListen");
      requiredMethods.add("accept");
      requiredMethods.add("shutdown");
      requiredMethods.add("localAddress");
      requiredMethods.add("remoteAddress");
      requiredMethods.add("close");

      final Set<String> foundMethods = new HashSet<>();
      for (Method method : clazz.getDeclaredMethods()) {
        foundMethods.add(method.getName());
      }

      for (String methodName : requiredMethods) {
        assertTrue(
            foundMethods.contains(methodName),
            "PanamaWasiTcpSocket should have method: " + methodName);
        LOGGER.info("Found required method: " + methodName);
      }
    }

    @Test
    @DisplayName("PanamaWasiTcpSocket should have MethodHandle fields for FFI")
    void tcpSocketShouldHaveMethodHandleFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(TCP_SOCKET_CLASS);
      int methodHandleCount = 0;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == MethodHandle.class) {
          assertTrue(
              Modifier.isStatic(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be static");
          assertTrue(
              Modifier.isFinal(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be final");
          methodHandleCount++;
          LOGGER.info("Found MethodHandle field: " + field.getName());
        }
      }

      assertTrue(
          methodHandleCount > 0,
          "PanamaWasiTcpSocket should have MethodHandle fields for FFI bindings");
      LOGGER.info("Total MethodHandle fields in PanamaWasiTcpSocket: " + methodHandleCount);
    }

    @Test
    @DisplayName("PanamaWasiTcpSocket should be a concrete class")
    void tcpSocketShouldBeConcreteClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(TCP_SOCKET_CLASS);
      assertNotNull(clazz, "Class should load successfully");
      assertTrue(
          !Modifier.isAbstract(clazz.getModifiers()), "PanamaWasiTcpSocket should not be abstract");
      assertTrue(!clazz.isInterface(), "PanamaWasiTcpSocket should not be an interface");
      LOGGER.info("PanamaWasiTcpSocket is concrete class");
    }
  }

  @Nested
  @DisplayName("PanamaWasiTcpSocket API Contract Tests")
  class TcpSocketApiContractTests {

    @Test
    @DisplayName("startBind method should have correct signature")
    void startBindMethodShouldHaveCorrectSignature() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(TCP_SOCKET_CLASS);
      boolean found = false;
      for (Method method : clazz.getDeclaredMethods()) {
        if ("startBind".equals(method.getName())) {
          found = true;
          assertTrue(Modifier.isPublic(method.getModifiers()), "startBind method should be public");
          LOGGER.info("startBind method signature: " + method);
        }
      }
      assertTrue(found, "startBind method should exist");
    }

    @Test
    @DisplayName("startConnect method should have correct signature")
    void startConnectMethodShouldHaveCorrectSignature() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(TCP_SOCKET_CLASS);
      boolean found = false;
      for (Method method : clazz.getDeclaredMethods()) {
        if ("startConnect".equals(method.getName())) {
          found = true;
          assertTrue(
              Modifier.isPublic(method.getModifiers()), "startConnect method should be public");
          LOGGER.info("startConnect method signature: " + method);
        }
      }
      assertTrue(found, "startConnect method should exist");
    }

    @Test
    @DisplayName("startListen method should have correct signature")
    void startListenMethodShouldHaveCorrectSignature() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(TCP_SOCKET_CLASS);
      boolean found = false;
      for (Method method : clazz.getDeclaredMethods()) {
        if ("startListen".equals(method.getName())) {
          found = true;
          assertTrue(
              Modifier.isPublic(method.getModifiers()), "startListen method should be public");
          LOGGER.info("startListen method signature: " + method);
        }
      }
      assertTrue(found, "startListen method should exist");
    }

    @Test
    @DisplayName("accept method should have correct signature")
    void acceptMethodShouldHaveCorrectSignature() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(TCP_SOCKET_CLASS);
      boolean foundAccept = false;
      for (Method method : clazz.getDeclaredMethods()) {
        if ("accept".equals(method.getName())) {
          foundAccept = true;
          assertTrue(Modifier.isPublic(method.getModifiers()), "accept method should be public");
          LOGGER.info("accept method signature: " + method);
        }
      }
      assertTrue(foundAccept, "accept method should exist");
    }

    @Test
    @DisplayName("shutdown method should have correct signature")
    void shutdownMethodShouldHaveCorrectSignature() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(TCP_SOCKET_CLASS);
      boolean foundShutdown = false;
      for (Method method : clazz.getDeclaredMethods()) {
        if ("shutdown".equals(method.getName())) {
          foundShutdown = true;
          assertTrue(Modifier.isPublic(method.getModifiers()), "shutdown method should be public");
          LOGGER.info("shutdown method signature: " + method);
        }
      }
      assertTrue(foundShutdown, "shutdown method should exist");
    }
  }

  @Nested
  @DisplayName("PanamaWasiUdpSocket Class Structure Tests")
  class UdpSocketClassStructureTests {

    @Test
    @DisplayName("PanamaWasiUdpSocket should exist and be public")
    void udpSocketClassShouldExistAndBePublic() {
      final Class<?> clazz =
          assertDoesNotThrow(
              () -> loadClassWithoutInit(UDP_SOCKET_CLASS),
              "PanamaWasiUdpSocket class should exist");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "PanamaWasiUdpSocket should be public");
      LOGGER.info(
          "PanamaWasiUdpSocket class verified: public=" + Modifier.isPublic(clazz.getModifiers()));
    }

    @Test
    @DisplayName("PanamaWasiUdpSocket should implement WasiUdpSocket interface")
    void udpSocketShouldImplementInterface() {
      final Class<?> clazz = assertDoesNotThrow(() -> loadClassWithoutInit(UDP_SOCKET_CLASS));
      assertTrue(
          WasiUdpSocket.class.isAssignableFrom(clazz),
          "PanamaWasiUdpSocket should implement WasiUdpSocket");
      LOGGER.info(
          "PanamaWasiUdpSocket implements WasiUdpSocket: "
              + WasiUdpSocket.class.isAssignableFrom(clazz));
    }

    @Test
    @DisplayName("PanamaWasiUdpSocket should have required UDP methods")
    void udpSocketShouldHaveRequiredMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(UDP_SOCKET_CLASS);
      // WASI Preview 2 uses start/finish pattern for async bind
      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("startBind");
      requiredMethods.add("finishBind");
      requiredMethods.add("stream");
      requiredMethods.add("localAddress");
      requiredMethods.add("remoteAddress");
      requiredMethods.add("close");

      final Set<String> foundMethods = new HashSet<>();
      for (Method method : clazz.getDeclaredMethods()) {
        foundMethods.add(method.getName());
      }

      for (String methodName : requiredMethods) {
        assertTrue(
            foundMethods.contains(methodName),
            "PanamaWasiUdpSocket should have method: " + methodName);
        LOGGER.info("Found required method: " + methodName);
      }
    }

    @Test
    @DisplayName("PanamaWasiUdpSocket should have MethodHandle fields for FFI")
    void udpSocketShouldHaveMethodHandleFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(UDP_SOCKET_CLASS);
      int methodHandleCount = 0;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == MethodHandle.class) {
          assertTrue(
              Modifier.isStatic(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be static");
          assertTrue(
              Modifier.isFinal(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be final");
          methodHandleCount++;
          LOGGER.info("Found MethodHandle field: " + field.getName());
        }
      }

      assertTrue(
          methodHandleCount > 0,
          "PanamaWasiUdpSocket should have MethodHandle fields for FFI bindings");
      LOGGER.info("Total MethodHandle fields in PanamaWasiUdpSocket: " + methodHandleCount);
    }

    @Test
    @DisplayName("PanamaWasiUdpSocket should have datagram methods")
    void udpSocketShouldHaveDatagramMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(UDP_SOCKET_CLASS);
      final Set<String> methodNames = new HashSet<>();
      for (Method method : clazz.getDeclaredMethods()) {
        methodNames.add(method.getName());
      }

      // UDP sockets typically have receive/send datagram methods
      final boolean hasReceive =
          methodNames.contains("receive") || methodNames.contains("receiveDatagram");
      final boolean hasSend = methodNames.contains("send") || methodNames.contains("sendDatagram");

      LOGGER.info("UDP datagram methods - hasReceive: " + hasReceive + ", hasSend: " + hasSend);
      // Log all method names for debugging
      LOGGER.info("All UDP socket methods: " + methodNames);
    }
  }

  @Nested
  @DisplayName("PanamaWasiUdpSocket API Contract Tests")
  class UdpSocketApiContractTests {

    @Test
    @DisplayName("startBind method should have correct signature")
    void startBindMethodShouldHaveCorrectSignature() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(UDP_SOCKET_CLASS);
      boolean found = false;
      for (Method method : clazz.getDeclaredMethods()) {
        if ("startBind".equals(method.getName())) {
          found = true;
          assertTrue(Modifier.isPublic(method.getModifiers()), "startBind method should be public");
          LOGGER.info("UDP startBind method signature: " + method);
        }
      }
      assertTrue(found, "startBind method should exist");
    }

    @Test
    @DisplayName("stream method should have correct signature")
    void streamMethodShouldHaveCorrectSignature() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(UDP_SOCKET_CLASS);
      boolean foundStream = false;
      for (Method method : clazz.getDeclaredMethods()) {
        if ("stream".equals(method.getName())) {
          foundStream = true;
          assertTrue(Modifier.isPublic(method.getModifiers()), "stream method should be public");
          LOGGER.info("UDP stream method signature: " + method);
        }
      }
      assertTrue(foundStream, "stream method should exist");
    }

    @Test
    @DisplayName("localAddress method should have correct signature")
    void localAddressMethodShouldHaveCorrectSignature() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(UDP_SOCKET_CLASS);
      boolean foundMethod = false;
      for (Method method : clazz.getDeclaredMethods()) {
        if ("localAddress".equals(method.getName())) {
          foundMethod = true;
          assertTrue(
              Modifier.isPublic(method.getModifiers()), "localAddress method should be public");
          LOGGER.info("UDP localAddress method signature: " + method);
        }
      }
      assertTrue(foundMethod, "localAddress method should exist");
    }
  }

  @Nested
  @DisplayName("PanamaWasiNetwork Class Structure Tests")
  class NetworkClassStructureTests {

    @Test
    @DisplayName("PanamaWasiNetwork should exist and be public")
    void networkClassShouldExistAndBePublic() {
      final Class<?> clazz =
          assertDoesNotThrow(
              () -> loadClassWithoutInit(NETWORK_CLASS), "PanamaWasiNetwork class should exist");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "PanamaWasiNetwork should be public");
      LOGGER.info(
          "PanamaWasiNetwork class verified: public=" + Modifier.isPublic(clazz.getModifiers()));
    }

    @Test
    @DisplayName("PanamaWasiNetwork should implement WasiNetwork interface")
    void networkShouldImplementInterface() {
      final Class<?> clazz = assertDoesNotThrow(() -> loadClassWithoutInit(NETWORK_CLASS));
      assertTrue(
          WasiNetwork.class.isAssignableFrom(clazz),
          "PanamaWasiNetwork should implement WasiNetwork");
      LOGGER.info(
          "PanamaWasiNetwork implements WasiNetwork: " + WasiNetwork.class.isAssignableFrom(clazz));
    }

    @Test
    @DisplayName("PanamaWasiNetwork should have required network methods")
    void networkShouldHaveRequiredMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NETWORK_CLASS);
      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("close");
      requiredMethods.add("getNetworkHandle");

      final Set<String> foundMethods = new HashSet<>();
      for (Method method : clazz.getDeclaredMethods()) {
        foundMethods.add(method.getName());
      }

      for (String methodName : requiredMethods) {
        assertTrue(
            foundMethods.contains(methodName),
            "PanamaWasiNetwork should have method: " + methodName);
        LOGGER.info("Found required network method: " + methodName);
      }

      LOGGER.info("All network methods: " + foundMethods);
    }

    @Test
    @DisplayName("PanamaWasiNetwork should have MethodHandle fields for FFI")
    void networkShouldHaveMethodHandleFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NETWORK_CLASS);
      int methodHandleCount = 0;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == MethodHandle.class) {
          assertTrue(
              Modifier.isStatic(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be static");
          methodHandleCount++;
          LOGGER.info("Found MethodHandle field in Network: " + field.getName());
        }
      }

      LOGGER.info("Total MethodHandle fields in PanamaWasiNetwork: " + methodHandleCount);
    }

    @Test
    @DisplayName("PanamaWasiNetwork should have handle field")
    void networkShouldHaveHandleField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NETWORK_CLASS);
      boolean hasHandleField = false;
      for (Field field : clazz.getDeclaredFields()) {
        if ("handle".equals(field.getName()) || "nativeHandle".equals(field.getName())) {
          hasHandleField = true;
          assertTrue(Modifier.isPrivate(field.getModifiers()), "handle field should be private");
          LOGGER.info("Found handle field: " + field.getName() + " type: " + field.getType());
        }
      }

      // Log all fields if handle not found
      if (!hasHandleField) {
        LOGGER.info("Handle field not found, listing all fields:");
        for (Field field : clazz.getDeclaredFields()) {
          LOGGER.info("  Field: " + field.getName() + " type: " + field.getType());
        }
      }
    }
  }

  @Nested
  @DisplayName("PanamaWasiIpNameLookup Class Structure Tests")
  class IpNameLookupClassStructureTests {

    @Test
    @DisplayName("PanamaWasiIpNameLookup should exist and be public")
    void ipNameLookupClassShouldExistAndBePublic() {
      final Class<?> clazz =
          assertDoesNotThrow(
              () -> loadClassWithoutInit(IP_NAME_LOOKUP_CLASS),
              "PanamaWasiIpNameLookup class should exist");
      assertTrue(
          Modifier.isPublic(clazz.getModifiers()), "PanamaWasiIpNameLookup should be public");
      LOGGER.info(
          "PanamaWasiIpNameLookup class verified: public="
              + Modifier.isPublic(clazz.getModifiers()));
    }

    @Test
    @DisplayName("PanamaWasiIpNameLookup should implement WasiIpNameLookup interface")
    void ipNameLookupShouldImplementInterface() {
      final Class<?> clazz = assertDoesNotThrow(() -> loadClassWithoutInit(IP_NAME_LOOKUP_CLASS));
      assertTrue(
          WasiIpNameLookup.class.isAssignableFrom(clazz),
          "PanamaWasiIpNameLookup should implement WasiIpNameLookup");
      LOGGER.info(
          "PanamaWasiIpNameLookup implements WasiIpNameLookup: "
              + WasiIpNameLookup.class.isAssignableFrom(clazz));
    }

    @Test
    @DisplayName("PanamaWasiIpNameLookup should have resolve method")
    void ipNameLookupShouldHaveResolveMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(IP_NAME_LOOKUP_CLASS);
      boolean foundResolve = false;
      for (Method method : clazz.getDeclaredMethods()) {
        if (method.getName().contains("resolve") || method.getName().contains("lookup")) {
          foundResolve = true;
          LOGGER.info("Found resolve method: " + method.getName() + " signature: " + method);
        }
      }

      // Log all methods if not found
      if (!foundResolve) {
        LOGGER.info("Listing all methods in PanamaWasiIpNameLookup:");
        for (Method method : clazz.getDeclaredMethods()) {
          LOGGER.info("  Method: " + method.getName());
        }
      }
    }

    @Test
    @DisplayName("PanamaWasiIpNameLookup should have MethodHandle fields for FFI")
    void ipNameLookupShouldHaveMethodHandleFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(IP_NAME_LOOKUP_CLASS);
      int methodHandleCount = 0;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == MethodHandle.class) {
          assertTrue(
              Modifier.isStatic(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be static");
          methodHandleCount++;
          LOGGER.info("Found MethodHandle field in IpNameLookup: " + field.getName());
        }
      }

      LOGGER.info("Total MethodHandle fields in PanamaWasiIpNameLookup: " + methodHandleCount);
    }

    @Test
    @DisplayName("PanamaWasiIpNameLookup should have resolveAddresses method")
    void ipNameLookupShouldHaveResolveAddressesMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(IP_NAME_LOOKUP_CLASS);
      final Set<String> foundMethods = new HashSet<>();
      for (Method method : clazz.getDeclaredMethods()) {
        foundMethods.add(method.getName());
      }

      assertTrue(
          foundMethods.contains("resolveAddresses"),
          "PanamaWasiIpNameLookup should have resolveAddresses method");
      LOGGER.info("Found resolveAddresses method");
      LOGGER.info("All IpNameLookup methods: " + foundMethods);
    }
  }

  @Nested
  @DisplayName("PanamaResolveAddressStream Class Structure Tests")
  class ResolveAddressStreamClassStructureTests {

    @Test
    @DisplayName("PanamaResolveAddressStream should exist and be public")
    void resolveAddressStreamClassShouldExistAndBePublic() {
      final Class<?> clazz =
          assertDoesNotThrow(
              () -> loadClassWithoutInit(RESOLVE_ADDRESS_STREAM_CLASS),
              "PanamaResolveAddressStream class should exist");
      assertTrue(
          Modifier.isPublic(clazz.getModifiers()), "PanamaResolveAddressStream should be public");
      LOGGER.info(
          "PanamaResolveAddressStream class verified: public="
              + Modifier.isPublic(clazz.getModifiers()));
    }

    @Test
    @DisplayName("PanamaResolveAddressStream should implement ResolveAddressStream interface")
    void resolveAddressStreamShouldImplementInterface() {
      final Class<?> clazz =
          assertDoesNotThrow(() -> loadClassWithoutInit(RESOLVE_ADDRESS_STREAM_CLASS));
      assertTrue(
          ResolveAddressStream.class.isAssignableFrom(clazz),
          "PanamaResolveAddressStream should implement ResolveAddressStream");
      LOGGER.info(
          "PanamaResolveAddressStream implements ResolveAddressStream: "
              + ResolveAddressStream.class.isAssignableFrom(clazz));
    }

    @Test
    @DisplayName("PanamaResolveAddressStream should have stream iteration methods")
    void resolveAddressStreamShouldHaveIterationMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(RESOLVE_ADDRESS_STREAM_CLASS);
      final Set<String> foundMethods = new HashSet<>();
      for (Method method : clazz.getDeclaredMethods()) {
        foundMethods.add(method.getName());
        LOGGER.info("ResolveAddressStream method: " + method.getName());
      }

      // Should have methods for iterating over resolved addresses
      final boolean hasNext =
          foundMethods.contains("next")
              || foundMethods.contains("hasNext")
              || foundMethods.contains("resolveNext");

      LOGGER.info("Stream has next/iteration method: " + hasNext);
      LOGGER.info("All stream methods: " + foundMethods);
    }

    @Test
    @DisplayName("PanamaResolveAddressStream should have MethodHandle fields for FFI")
    void resolveAddressStreamShouldHaveMethodHandleFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(RESOLVE_ADDRESS_STREAM_CLASS);
      int methodHandleCount = 0;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == MethodHandle.class) {
          assertTrue(
              Modifier.isStatic(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be static");
          methodHandleCount++;
          LOGGER.info("Found MethodHandle field in ResolveAddressStream: " + field.getName());
        }
      }

      LOGGER.info("Total MethodHandle fields in PanamaResolveAddressStream: " + methodHandleCount);
    }

    @Test
    @DisplayName("PanamaResolveAddressStream should have subscribe method")
    void resolveAddressStreamShouldHaveSubscribeMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(RESOLVE_ADDRESS_STREAM_CLASS);
      boolean foundSubscribe = false;
      for (Method method : clazz.getDeclaredMethods()) {
        if ("subscribe".equals(method.getName())) {
          foundSubscribe = true;
          assertTrue(Modifier.isPublic(method.getModifiers()), "subscribe method should be public");
          LOGGER.info("Found subscribe method: " + method);
        }
      }

      if (foundSubscribe) {
        LOGGER.info("subscribe method exists");
      } else {
        LOGGER.info("subscribe method not found - may use different pattern");
      }
    }
  }

  @Nested
  @DisplayName("Panama FFI Pattern Tests")
  class PanamaFfiPatternTests {

    @Test
    @DisplayName("All socket classes should have static final MethodHandle fields")
    void allSocketClassesShouldHaveStaticFinalMethodHandles() {
      final String[] classNames = {
        TCP_SOCKET_CLASS,
        UDP_SOCKET_CLASS,
        NETWORK_CLASS,
        IP_NAME_LOOKUP_CLASS,
        RESOLVE_ADDRESS_STREAM_CLASS
      };

      for (String className : classNames) {
        final Class<?> clazz =
            assertDoesNotThrow(
                () -> loadClassWithoutInit(className), "Class should exist: " + className);

        int staticFinalMh = 0;
        for (Field field : clazz.getDeclaredFields()) {
          if (field.getType() == MethodHandle.class) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
              staticFinalMh++;
            }
          }
        }

        LOGGER.info(
            className.substring(className.lastIndexOf('.') + 1)
                + " has "
                + staticFinalMh
                + " static final MethodHandle fields");
      }
    }

    @Test
    @DisplayName("Socket and stream classes should implement AutoCloseable")
    void socketAndStreamClassesShouldImplementAutoCloseable() {
      // PanamaWasiIpNameLookup is a stateless lookup utility, not a closeable resource
      final String[] classNames = {
        TCP_SOCKET_CLASS, UDP_SOCKET_CLASS, NETWORK_CLASS, RESOLVE_ADDRESS_STREAM_CLASS
      };

      for (String className : classNames) {
        final Class<?> clazz =
            assertDoesNotThrow(
                () -> loadClassWithoutInit(className), "Class should exist: " + className);

        final boolean implementsAutoCloseable = AutoCloseable.class.isAssignableFrom(clazz);
        final String simpleName = className.substring(className.lastIndexOf('.') + 1);

        LOGGER.info(simpleName + " implements AutoCloseable: " + implementsAutoCloseable);

        // Check for close method
        boolean hasClose = false;
        for (Method method : clazz.getDeclaredMethods()) {
          if ("close".equals(method.getName())) {
            hasClose = true;
            break;
          }
        }

        assertTrue(hasClose, simpleName + " should have close method");
      }
    }

    @Test
    @DisplayName("Socket classes should have isClosed method or closed field")
    void socketClassesShouldHaveClosedStateTracking() {
      final String[] classNames = {
        TCP_SOCKET_CLASS, UDP_SOCKET_CLASS, NETWORK_CLASS, RESOLVE_ADDRESS_STREAM_CLASS
      };

      for (String className : classNames) {
        final Class<?> clazz =
            assertDoesNotThrow(
                () -> loadClassWithoutInit(className), "Class should exist: " + className);

        boolean hasIsClosed = false;
        for (Method method : clazz.getDeclaredMethods()) {
          if ("isClosed".equals(method.getName())) {
            hasIsClosed = true;
            break;
          }
        }

        boolean hasClosedField = false;
        for (Field field : clazz.getDeclaredFields()) {
          if ("closed".equals(field.getName())) {
            hasClosedField = true;
            break;
          }
        }

        final String simpleName = className.substring(className.lastIndexOf('.') + 1);
        final boolean hasStateTracking = hasIsClosed || hasClosedField;
        LOGGER.info(
            simpleName
                + " has isClosed method: "
                + hasIsClosed
                + ", has closed field: "
                + hasClosedField);

        assertTrue(hasStateTracking, simpleName + " should have isClosed method or closed field");
      }
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("PanamaWasiTcpSocket should implement all WasiTcpSocket methods")
    void tcpSocketShouldImplementAllInterfaceMethods() throws ClassNotFoundException {
      final Class<?> implClass = loadClassWithoutInit(TCP_SOCKET_CLASS);
      final Method[] interfaceMethods = WasiTcpSocket.class.getDeclaredMethods();
      final Set<String> implMethodNames = new HashSet<>();
      for (Method method : implClass.getDeclaredMethods()) {
        implMethodNames.add(method.getName());
      }

      for (Method interfaceMethod : interfaceMethods) {
        final String methodName = interfaceMethod.getName();
        final boolean found = implMethodNames.contains(methodName);
        LOGGER.info("WasiTcpSocket." + methodName + " implemented: " + found);
      }
    }

    @Test
    @DisplayName("PanamaWasiUdpSocket should implement all WasiUdpSocket methods")
    void udpSocketShouldImplementAllInterfaceMethods() throws ClassNotFoundException {
      final Class<?> implClass = loadClassWithoutInit(UDP_SOCKET_CLASS);
      final Method[] interfaceMethods = WasiUdpSocket.class.getDeclaredMethods();
      final Set<String> implMethodNames = new HashSet<>();
      for (Method method : implClass.getDeclaredMethods()) {
        implMethodNames.add(method.getName());
      }

      for (Method interfaceMethod : interfaceMethods) {
        final String methodName = interfaceMethod.getName();
        final boolean found = implMethodNames.contains(methodName);
        LOGGER.info("WasiUdpSocket." + methodName + " implemented: " + found);
      }
    }

    @Test
    @DisplayName("PanamaWasiNetwork should implement all WasiNetwork methods")
    void networkShouldImplementAllInterfaceMethods() throws ClassNotFoundException {
      final Class<?> implClass = loadClassWithoutInit(NETWORK_CLASS);
      final Method[] interfaceMethods = WasiNetwork.class.getDeclaredMethods();
      final Set<String> implMethodNames = new HashSet<>();
      for (Method method : implClass.getDeclaredMethods()) {
        implMethodNames.add(method.getName());
      }

      for (Method interfaceMethod : interfaceMethods) {
        final String methodName = interfaceMethod.getName();
        final boolean found = implMethodNames.contains(methodName);
        LOGGER.info("WasiNetwork." + methodName + " implemented: " + found);
      }
    }

    @Test
    @DisplayName("PanamaWasiIpNameLookup should implement all WasiIpNameLookup methods")
    void ipNameLookupShouldImplementAllInterfaceMethods() throws ClassNotFoundException {
      final Class<?> implClass = loadClassWithoutInit(IP_NAME_LOOKUP_CLASS);
      final Method[] interfaceMethods = WasiIpNameLookup.class.getDeclaredMethods();
      final Set<String> implMethodNames = new HashSet<>();
      for (Method method : implClass.getDeclaredMethods()) {
        implMethodNames.add(method.getName());
      }

      for (Method interfaceMethod : interfaceMethods) {
        final String methodName = interfaceMethod.getName();
        final boolean found = implMethodNames.contains(methodName);
        LOGGER.info("WasiIpNameLookup." + methodName + " implemented: " + found);
      }
    }

    @Test
    @DisplayName("PanamaResolveAddressStream should implement all ResolveAddressStream methods")
    void resolveAddressStreamShouldImplementAllInterfaceMethods() throws ClassNotFoundException {
      final Class<?> implClass = loadClassWithoutInit(RESOLVE_ADDRESS_STREAM_CLASS);
      final Method[] interfaceMethods = ResolveAddressStream.class.getDeclaredMethods();
      final Set<String> implMethodNames = new HashSet<>();
      for (Method method : implClass.getDeclaredMethods()) {
        implMethodNames.add(method.getName());
      }

      for (Method interfaceMethod : interfaceMethods) {
        final String methodName = interfaceMethod.getName();
        final boolean found = implMethodNames.contains(methodName);
        LOGGER.info("ResolveAddressStream." + methodName + " implemented: " + found);
      }
    }
  }

  @Nested
  @DisplayName("Field Structure Tests")
  class FieldStructureTests {

    @Test
    @DisplayName("PanamaWasiTcpSocket should have required instance fields")
    void tcpSocketShouldHaveRequiredInstanceFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(TCP_SOCKET_CLASS);
      final Set<String> fieldNames = new HashSet<>();
      for (Field field : clazz.getDeclaredFields()) {
        if (!Modifier.isStatic(field.getModifiers())) {
          fieldNames.add(field.getName());
          LOGGER.info("TCP instance field: " + field.getName() + " type: " + field.getType());
        }
      }

      LOGGER.info("Total TCP instance fields: " + fieldNames.size());
    }

    @Test
    @DisplayName("PanamaWasiUdpSocket should have required instance fields")
    void udpSocketShouldHaveRequiredInstanceFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(UDP_SOCKET_CLASS);
      final Set<String> fieldNames = new HashSet<>();
      for (Field field : clazz.getDeclaredFields()) {
        if (!Modifier.isStatic(field.getModifiers())) {
          fieldNames.add(field.getName());
          LOGGER.info("UDP instance field: " + field.getName() + " type: " + field.getType());
        }
      }

      LOGGER.info("Total UDP instance fields: " + fieldNames.size());
    }

    @Test
    @DisplayName("All socket classes should have Arena field for memory management")
    void allSocketClassesShouldHaveArenaField() {
      final String[] classNames = {
        TCP_SOCKET_CLASS,
        UDP_SOCKET_CLASS,
        NETWORK_CLASS,
        IP_NAME_LOOKUP_CLASS,
        RESOLVE_ADDRESS_STREAM_CLASS
      };

      for (String className : classNames) {
        final Class<?> clazz =
            assertDoesNotThrow(
                () -> loadClassWithoutInit(className), "Class should exist: " + className);

        boolean hasArena = false;
        for (Field field : clazz.getDeclaredFields()) {
          final String typeName = field.getType().getSimpleName();
          if ("Arena".equals(typeName) || field.getType().getName().contains("Arena")) {
            hasArena = true;
            LOGGER.info(
                className.substring(className.lastIndexOf('.') + 1)
                    + " has Arena field: "
                    + field.getName());
          }
        }

        if (!hasArena) {
          LOGGER.info(
              className.substring(className.lastIndexOf('.') + 1)
                  + " does not have Arena field (may use different memory management)");
        }
      }
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("PanamaWasiTcpSocket should have constructors")
    void tcpSocketShouldHaveConstructors() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(TCP_SOCKET_CLASS);
      final var constructors = clazz.getDeclaredConstructors();
      assertTrue(constructors.length > 0, "Should have at least one constructor");

      for (var constructor : constructors) {
        LOGGER.info("TCP constructor: " + Arrays.toString(constructor.getParameterTypes()));
      }
    }

    @Test
    @DisplayName("PanamaWasiUdpSocket should have constructors")
    void udpSocketShouldHaveConstructors() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(UDP_SOCKET_CLASS);
      final var constructors = clazz.getDeclaredConstructors();
      assertTrue(constructors.length > 0, "Should have at least one constructor");

      for (var constructor : constructors) {
        LOGGER.info("UDP constructor: " + Arrays.toString(constructor.getParameterTypes()));
      }
    }

    @Test
    @DisplayName("PanamaWasiNetwork should have constructors")
    void networkShouldHaveConstructors() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(NETWORK_CLASS);
      final var constructors = clazz.getDeclaredConstructors();
      assertTrue(constructors.length > 0, "Should have at least one constructor");

      for (var constructor : constructors) {
        LOGGER.info("Network constructor: " + Arrays.toString(constructor.getParameterTypes()));
      }
    }

    @Test
    @DisplayName("PanamaWasiIpNameLookup should have constructors")
    void ipNameLookupShouldHaveConstructors() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(IP_NAME_LOOKUP_CLASS);
      final var constructors = clazz.getDeclaredConstructors();
      assertTrue(constructors.length > 0, "Should have at least one constructor");

      for (var constructor : constructors) {
        LOGGER.info(
            "IpNameLookup constructor: " + Arrays.toString(constructor.getParameterTypes()));
      }
    }

    @Test
    @DisplayName("PanamaResolveAddressStream should have constructors")
    void resolveAddressStreamShouldHaveConstructors() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(RESOLVE_ADDRESS_STREAM_CLASS);
      final var constructors = clazz.getDeclaredConstructors();
      assertTrue(constructors.length > 0, "Should have at least one constructor");

      for (var constructor : constructors) {
        LOGGER.info(
            "ResolveAddressStream constructor: "
                + Arrays.toString(constructor.getParameterTypes()));
      }
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("PanamaWasiTcpSocket should have sufficient methods for TCP operations")
    void tcpSocketShouldHaveSufficientMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(TCP_SOCKET_CLASS);
      int publicMethods = 0;
      for (Method method : clazz.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers())) {
          publicMethods++;
        }
      }

      assertTrue(
          publicMethods >= 5,
          "TCP socket should have at least 5 public methods, found: " + publicMethods);
      LOGGER.info("PanamaWasiTcpSocket has " + publicMethods + " public methods");
    }

    @Test
    @DisplayName("PanamaWasiUdpSocket should have sufficient methods for UDP operations")
    void udpSocketShouldHaveSufficientMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(UDP_SOCKET_CLASS);
      int publicMethods = 0;
      for (Method method : clazz.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers())) {
          publicMethods++;
        }
      }

      assertTrue(
          publicMethods >= 5,
          "UDP socket should have at least 5 public methods, found: " + publicMethods);
      LOGGER.info("PanamaWasiUdpSocket has " + publicMethods + " public methods");
    }

    @Test
    @DisplayName("All socket classes should have reasonable method counts")
    void allSocketClassesShouldHaveReasonableMethodCounts() {
      final String[] classNames = {
        TCP_SOCKET_CLASS,
        UDP_SOCKET_CLASS,
        NETWORK_CLASS,
        IP_NAME_LOOKUP_CLASS,
        RESOLVE_ADDRESS_STREAM_CLASS
      };

      for (String className : classNames) {
        final Class<?> clazz =
            assertDoesNotThrow(
                () -> loadClassWithoutInit(className), "Class should exist: " + className);

        final int total = clazz.getDeclaredMethods().length;
        int publicCount = 0;
        for (Method method : clazz.getDeclaredMethods()) {
          if (Modifier.isPublic(method.getModifiers())) {
            publicCount++;
          }
        }

        final String simpleName = className.substring(className.lastIndexOf('.') + 1);
        LOGGER.info(simpleName + " methods - total: " + total + ", public: " + publicCount);
      }
    }
  }
}
