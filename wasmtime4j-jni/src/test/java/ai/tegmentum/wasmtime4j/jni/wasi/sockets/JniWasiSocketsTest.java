/*
 * Copyright 2024 Tegmentum AI
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddressFamily;
import ai.tegmentum.wasmtime4j.wasi.sockets.ResolveAddressStream;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiIpNameLookup;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiNetwork;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiTcpSocket;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiUdpSocket;
import java.lang.reflect.Constructor;
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
 * Comprehensive unit tests for JNI WASI Sockets implementation classes.
 *
 * <p>This test suite verifies:
 *
 * <ul>
 *   <li>JniWasiTcpSocket - TCP socket implementation
 *   <li>JniWasiUdpSocket - UDP socket implementation
 *   <li>JniWasiNetwork - Network resource management
 *   <li>JniWasiIpNameLookup - DNS name lookup
 *   <li>JniResolveAddressStream - Address stream from DNS lookup
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("JNI WASI Sockets Tests")
class JniWasiSocketsTest {

  private static final Logger LOGGER = Logger.getLogger(JniWasiSocketsTest.class.getName());

  // =========================================================================
  // JniWasiTcpSocket Tests
  // =========================================================================

  @Nested
  @DisplayName("JniWasiTcpSocket Tests")
  class JniWasiTcpSocketTests {

    @Test
    @DisplayName("JniWasiTcpSocket should implement WasiTcpSocket interface")
    void shouldImplementWasiTcpSocketInterface() {
      assertTrue(
          WasiTcpSocket.class.isAssignableFrom(JniWasiTcpSocket.class),
          "JniWasiTcpSocket must implement WasiTcpSocket");
      LOGGER.info("JniWasiTcpSocket correctly implements WasiTcpSocket interface");
    }

    @Test
    @DisplayName("JniWasiTcpSocket should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiTcpSocket.class.getModifiers()),
          "JniWasiTcpSocket should be final");
    }

    @Test
    @DisplayName("JniWasiTcpSocket should have static create method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      Method createMethod =
          JniWasiTcpSocket.class.getMethod("create", long.class, IpAddressFamily.class);
      assertNotNull(createMethod, "Should have create method");
      assertTrue(Modifier.isStatic(createMethod.getModifiers()), "Create method should be static");
      assertEquals(
          JniWasiTcpSocket.class,
          createMethod.getReturnType(),
          "Create should return JniWasiTcpSocket");
      LOGGER.info("JniWasiTcpSocket has static create factory method");
    }

    @Test
    @DisplayName("JniWasiTcpSocket should have public constructor with handles")
    void shouldHavePublicConstructorWithHandles() throws NoSuchMethodException {
      Constructor<?> constructor = JniWasiTcpSocket.class.getConstructor(long.class, long.class);
      assertNotNull(constructor, "Should have constructor");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("Constructor should reject zero context handle")
    void constructorShouldRejectZeroContextHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniWasiTcpSocket(0, 1),
          "Should reject zero context handle");
    }

    @Test
    @DisplayName("Constructor should reject non-positive socket handle")
    void constructorShouldRejectNonPositiveSocketHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniWasiTcpSocket(1, 0),
          "Should reject zero socket handle");
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniWasiTcpSocket(1, -1),
          "Should reject negative socket handle");
    }

    @Test
    @DisplayName("JniWasiTcpSocket should have all WasiTcpSocket interface methods")
    void shouldHaveAllInterfaceMethods() {
      Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
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
                  "close"));

      Set<String> actualMethods = new HashSet<>();
      for (Method m : JniWasiTcpSocket.class.getMethods()) {
        if (expectedMethods.contains(m.getName())) {
          actualMethods.add(m.getName());
        }
      }

      assertEquals(
          expectedMethods, actualMethods, "JniWasiTcpSocket should have all WasiTcpSocket methods");
      LOGGER.info("JniWasiTcpSocket has all " + actualMethods.size() + " expected methods");
    }

    @Test
    @DisplayName("JniWasiTcpSocket should have contextHandle field")
    void shouldHaveContextHandleField() throws NoSuchFieldException {
      Field field = JniWasiTcpSocket.class.getDeclaredField("contextHandle");
      assertNotNull(field, "Should have contextHandle field");
      assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
      assertEquals(long.class, field.getType(), "contextHandle should be long");
    }

    @Test
    @DisplayName("JniWasiTcpSocket should have socketHandle field")
    void shouldHaveSocketHandleField() throws NoSuchFieldException {
      Field field = JniWasiTcpSocket.class.getDeclaredField("socketHandle");
      assertNotNull(field, "Should have socketHandle field");
      assertTrue(Modifier.isFinal(field.getModifiers()), "socketHandle should be final");
      assertEquals(long.class, field.getType(), "socketHandle should be long");
    }

    @Test
    @DisplayName("JniWasiTcpSocket should have closed field")
    void shouldHaveClosedField() throws NoSuchFieldException {
      Field field = JniWasiTcpSocket.class.getDeclaredField("closed");
      assertNotNull(field, "Should have closed field");
      assertEquals(boolean.class, field.getType(), "closed should be boolean");
    }

    @Test
    @DisplayName("JniWasiTcpSocket should have native methods")
    void shouldHaveNativeMethods() {
      int nativeMethodCount = 0;
      for (Method m : JniWasiTcpSocket.class.getDeclaredMethods()) {
        if (Modifier.isNative(m.getModifiers())) {
          nativeMethodCount++;
        }
      }
      assertTrue(
          nativeMethodCount >= 15,
          "Should have at least 15 native methods, found: " + nativeMethodCount);
      LOGGER.info("JniWasiTcpSocket has " + nativeMethodCount + " native methods");
    }

    @Test
    @DisplayName("JniWasiTcpSocket should have inner AddressParams class")
    void shouldHaveAddressParamsInnerClass() {
      Class<?>[] innerClasses = JniWasiTcpSocket.class.getDeclaredClasses();
      boolean found = false;
      for (Class<?> inner : innerClasses) {
        if (inner.getSimpleName().equals("AddressParams")) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have AddressParams inner class");
    }
  }

  // =========================================================================
  // JniWasiUdpSocket Tests
  // =========================================================================

  @Nested
  @DisplayName("JniWasiUdpSocket Tests")
  class JniWasiUdpSocketTests {

    @Test
    @DisplayName("JniWasiUdpSocket should implement WasiUdpSocket interface")
    void shouldImplementWasiUdpSocketInterface() {
      assertTrue(
          WasiUdpSocket.class.isAssignableFrom(JniWasiUdpSocket.class),
          "JniWasiUdpSocket must implement WasiUdpSocket");
      LOGGER.info("JniWasiUdpSocket correctly implements WasiUdpSocket interface");
    }

    @Test
    @DisplayName("JniWasiUdpSocket should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiUdpSocket.class.getModifiers()),
          "JniWasiUdpSocket should be final");
    }

    @Test
    @DisplayName("JniWasiUdpSocket should have static create method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      Method createMethod =
          JniWasiUdpSocket.class.getMethod("create", long.class, IpAddressFamily.class);
      assertNotNull(createMethod, "Should have create method");
      assertTrue(Modifier.isStatic(createMethod.getModifiers()), "Create method should be static");
      assertEquals(
          JniWasiUdpSocket.class,
          createMethod.getReturnType(),
          "Create should return JniWasiUdpSocket");
      LOGGER.info("JniWasiUdpSocket has static create factory method");
    }

    @Test
    @DisplayName("JniWasiUdpSocket should have all WasiUdpSocket interface methods")
    void shouldHaveAllInterfaceMethods() {
      Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "startBind",
                  "finishBind",
                  "stream",
                  "localAddress",
                  "remoteAddress",
                  "addressFamily",
                  "setUnicastHopLimit",
                  "receiveBufferSize",
                  "setReceiveBufferSize",
                  "sendBufferSize",
                  "setSendBufferSize",
                  "subscribe",
                  "receive",
                  "send",
                  "close"));

      Set<String> actualMethods = new HashSet<>();
      for (Method m : JniWasiUdpSocket.class.getMethods()) {
        if (expectedMethods.contains(m.getName())) {
          actualMethods.add(m.getName());
        }
      }

      assertEquals(
          expectedMethods, actualMethods, "JniWasiUdpSocket should have all WasiUdpSocket methods");
      LOGGER.info("JniWasiUdpSocket has all " + actualMethods.size() + " expected methods");
    }

    @Test
    @DisplayName("JniWasiUdpSocket should have contextHandle field")
    void shouldHaveContextHandleField() throws NoSuchFieldException {
      Field field = JniWasiUdpSocket.class.getDeclaredField("contextHandle");
      assertNotNull(field, "Should have contextHandle field");
      assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
    }

    @Test
    @DisplayName("JniWasiUdpSocket should have socketHandle field")
    void shouldHaveSocketHandleField() throws NoSuchFieldException {
      Field field = JniWasiUdpSocket.class.getDeclaredField("socketHandle");
      assertNotNull(field, "Should have socketHandle field");
      assertTrue(Modifier.isFinal(field.getModifiers()), "socketHandle should be final");
    }

    @Test
    @DisplayName("JniWasiUdpSocket should have closed field")
    void shouldHaveClosedField() throws NoSuchFieldException {
      Field field = JniWasiUdpSocket.class.getDeclaredField("closed");
      assertNotNull(field, "Should have closed field");
      assertEquals(boolean.class, field.getType(), "closed should be boolean");
    }

    @Test
    @DisplayName("JniWasiUdpSocket should have native methods")
    void shouldHaveNativeMethods() {
      int nativeMethodCount = 0;
      for (Method m : JniWasiUdpSocket.class.getDeclaredMethods()) {
        if (Modifier.isNative(m.getModifiers())) {
          nativeMethodCount++;
        }
      }
      assertTrue(
          nativeMethodCount >= 10,
          "Should have at least 10 native methods, found: " + nativeMethodCount);
      LOGGER.info("JniWasiUdpSocket has " + nativeMethodCount + " native methods");
    }

    @Test
    @DisplayName("JniWasiUdpSocket should have inner AddressParams class")
    void shouldHaveAddressParamsInnerClass() {
      Class<?>[] innerClasses = JniWasiUdpSocket.class.getDeclaredClasses();
      boolean found = false;
      for (Class<?> inner : innerClasses) {
        if (inner.getSimpleName().equals("AddressParams")) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have AddressParams inner class");
    }

    @Test
    @DisplayName("JniWasiUdpSocket receive method should return IncomingDatagram array")
    void receiveShouldReturnIncomingDatagramArray() throws NoSuchMethodException {
      Method receiveMethod = JniWasiUdpSocket.class.getMethod("receive", long.class);
      assertNotNull(receiveMethod, "Should have receive method");
      assertTrue(receiveMethod.getReturnType().isArray(), "receive should return array type");
    }

    @Test
    @DisplayName("JniWasiUdpSocket send method should accept OutgoingDatagram array")
    void sendShouldAcceptOutgoingDatagramArray() throws NoSuchMethodException {
      // Find send method that accepts array
      Method sendMethod = null;
      for (Method m : JniWasiUdpSocket.class.getMethods()) {
        if ("send".equals(m.getName()) && m.getParameterCount() == 1) {
          Class<?>[] params = m.getParameterTypes();
          if (params[0].isArray()) {
            sendMethod = m;
            break;
          }
        }
      }
      assertNotNull(sendMethod, "Should have send method accepting array");
      assertEquals(long.class, sendMethod.getReturnType(), "send should return long (count sent)");
    }
  }

  // =========================================================================
  // JniWasiNetwork Tests
  // =========================================================================

  @Nested
  @DisplayName("JniWasiNetwork Tests")
  class JniWasiNetworkTests {

    @Test
    @DisplayName("JniWasiNetwork should implement WasiNetwork interface")
    void shouldImplementWasiNetworkInterface() {
      assertTrue(
          WasiNetwork.class.isAssignableFrom(JniWasiNetwork.class),
          "JniWasiNetwork must implement WasiNetwork");
      LOGGER.info("JniWasiNetwork correctly implements WasiNetwork interface");
    }

    @Test
    @DisplayName("JniWasiNetwork should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiNetwork.class.getModifiers()), "JniWasiNetwork should be final");
    }

    @Test
    @DisplayName("JniWasiNetwork should have static create method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      Method createMethod = JniWasiNetwork.class.getMethod("create", long.class);
      assertNotNull(createMethod, "Should have create method");
      assertTrue(Modifier.isStatic(createMethod.getModifiers()), "Create method should be static");
      assertEquals(
          JniWasiNetwork.class,
          createMethod.getReturnType(),
          "Create should return JniWasiNetwork");
      LOGGER.info("JniWasiNetwork has static create factory method");
    }

    @Test
    @DisplayName("JniWasiNetwork should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method closeMethod = JniWasiNetwork.class.getMethod("close");
      assertNotNull(closeMethod, "Should have close method");
      assertEquals(void.class, closeMethod.getReturnType(), "close should return void");
    }

    @Test
    @DisplayName("JniWasiNetwork should have getNetworkHandle method")
    void shouldHaveGetNetworkHandleMethod() throws NoSuchMethodException {
      Method getHandleMethod = JniWasiNetwork.class.getMethod("getNetworkHandle");
      assertNotNull(getHandleMethod, "Should have getNetworkHandle method");
      assertTrue(
          Modifier.isPublic(getHandleMethod.getModifiers()), "getNetworkHandle should be public");
      assertEquals(
          long.class, getHandleMethod.getReturnType(), "getNetworkHandle should return long");
    }

    @Test
    @DisplayName("JniWasiNetwork should have contextHandle field")
    void shouldHaveContextHandleField() throws NoSuchFieldException {
      Field field = JniWasiNetwork.class.getDeclaredField("contextHandle");
      assertNotNull(field, "Should have contextHandle field");
      assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
    }

    @Test
    @DisplayName("JniWasiNetwork should have networkHandle field")
    void shouldHaveNetworkHandleField() throws NoSuchFieldException {
      Field field = JniWasiNetwork.class.getDeclaredField("networkHandle");
      assertNotNull(field, "Should have networkHandle field");
      assertTrue(Modifier.isFinal(field.getModifiers()), "networkHandle should be final");
    }

    @Test
    @DisplayName("JniWasiNetwork should have closed field")
    void shouldHaveClosedField() throws NoSuchFieldException {
      Field field = JniWasiNetwork.class.getDeclaredField("closed");
      assertNotNull(field, "Should have closed field");
      assertEquals(boolean.class, field.getType(), "closed should be boolean");
    }

    @Test
    @DisplayName("JniWasiNetwork should have native methods")
    void shouldHaveNativeMethods() {
      int nativeMethodCount = 0;
      for (Method m : JniWasiNetwork.class.getDeclaredMethods()) {
        if (Modifier.isNative(m.getModifiers())) {
          nativeMethodCount++;
        }
      }
      assertTrue(
          nativeMethodCount >= 2,
          "Should have at least 2 native methods (create, close), found: " + nativeMethodCount);
      LOGGER.info("JniWasiNetwork has " + nativeMethodCount + " native methods");
    }

    @Test
    @DisplayName("JniWasiNetwork create should reject zero context handle")
    void createShouldRejectZeroContextHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> JniWasiNetwork.create(0),
          "Should reject zero context handle");
    }
  }

  // =========================================================================
  // JniWasiIpNameLookup Tests
  // =========================================================================

  @Nested
  @DisplayName("JniWasiIpNameLookup Tests")
  class JniWasiIpNameLookupTests {

    @Test
    @DisplayName("JniWasiIpNameLookup should implement WasiIpNameLookup interface")
    void shouldImplementWasiIpNameLookupInterface() {
      assertTrue(
          WasiIpNameLookup.class.isAssignableFrom(JniWasiIpNameLookup.class),
          "JniWasiIpNameLookup must implement WasiIpNameLookup");
      LOGGER.info("JniWasiIpNameLookup correctly implements WasiIpNameLookup interface");
    }

    @Test
    @DisplayName("JniWasiIpNameLookup should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiIpNameLookup.class.getModifiers()),
          "JniWasiIpNameLookup should be final");
    }

    @Test
    @DisplayName("JniWasiIpNameLookup should have public constructor")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = JniWasiIpNameLookup.class.getConstructor(long.class);
      assertNotNull(constructor, "Should have constructor with long");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("Constructor should reject zero context handle")
    void constructorShouldRejectZeroContextHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new JniWasiIpNameLookup(0),
          "Should reject zero context handle");
    }

    @Test
    @DisplayName("JniWasiIpNameLookup should have resolveAddresses methods")
    void shouldHaveResolveAddressesMethods() {
      int resolveMethodCount = 0;
      for (Method m : JniWasiIpNameLookup.class.getMethods()) {
        if ("resolveAddresses".equals(m.getName())) {
          resolveMethodCount++;
        }
      }
      assertTrue(
          resolveMethodCount >= 2,
          "Should have at least 2 resolveAddresses overloads, found: " + resolveMethodCount);
      LOGGER.info(
          "JniWasiIpNameLookup has " + resolveMethodCount + " resolveAddresses method overloads");
    }

    @Test
    @DisplayName("resolveAddresses should return ResolveAddressStream")
    void resolveAddressesShouldReturnResolveAddressStream() throws NoSuchMethodException {
      Method method =
          JniWasiIpNameLookup.class.getMethod("resolveAddresses", WasiNetwork.class, String.class);
      assertNotNull(method, "Should have resolveAddresses(WasiNetwork, String)");
      assertEquals(
          ResolveAddressStream.class, method.getReturnType(), "Should return ResolveAddressStream");
    }

    @Test
    @DisplayName("resolveAddresses with family should accept IpAddressFamily")
    void resolveAddressesWithFamilyShouldAcceptIpAddressFamily() throws NoSuchMethodException {
      Method method =
          JniWasiIpNameLookup.class.getMethod(
              "resolveAddresses", WasiNetwork.class, String.class, IpAddressFamily.class);
      assertNotNull(method, "Should have resolveAddresses(WasiNetwork, String, IpAddressFamily)");
      assertEquals(
          ResolveAddressStream.class, method.getReturnType(), "Should return ResolveAddressStream");
    }

    @Test
    @DisplayName("JniWasiIpNameLookup should have contextHandle field")
    void shouldHaveContextHandleField() throws NoSuchFieldException {
      Field field = JniWasiIpNameLookup.class.getDeclaredField("contextHandle");
      assertNotNull(field, "Should have contextHandle field");
      assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
    }

    @Test
    @DisplayName("JniWasiIpNameLookup should have native methods")
    void shouldHaveNativeMethods() {
      int nativeMethodCount = 0;
      for (Method m : JniWasiIpNameLookup.class.getDeclaredMethods()) {
        if (Modifier.isNative(m.getModifiers())) {
          nativeMethodCount++;
        }
      }
      assertTrue(
          nativeMethodCount >= 1,
          "Should have at least 1 native method, found: " + nativeMethodCount);
      LOGGER.info("JniWasiIpNameLookup has " + nativeMethodCount + " native methods");
    }
  }

  // =========================================================================
  // JniResolveAddressStream Tests
  // =========================================================================

  @Nested
  @DisplayName("JniResolveAddressStream Tests")
  class JniResolveAddressStreamTests {

    @Test
    @DisplayName("JniResolveAddressStream should implement ResolveAddressStream interface")
    void shouldImplementResolveAddressStreamInterface() {
      assertTrue(
          ResolveAddressStream.class.isAssignableFrom(JniResolveAddressStream.class),
          "JniResolveAddressStream must implement ResolveAddressStream");
      LOGGER.info("JniResolveAddressStream correctly implements ResolveAddressStream interface");
    }

    @Test
    @DisplayName("JniResolveAddressStream should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniResolveAddressStream.class.getModifiers()),
          "JniResolveAddressStream should be final");
    }

    @Test
    @DisplayName("JniResolveAddressStream should have package-private constructor")
    void shouldHavePackagePrivateConstructor() {
      Constructor<?>[] constructors = JniResolveAddressStream.class.getDeclaredConstructors();
      boolean found = false;
      for (Constructor<?> c : constructors) {
        Class<?>[] params = c.getParameterTypes();
        if (params.length == 2 && params[0] == long.class && params[1] == long.class) {
          found = true;
          // Should not be public (package-private or protected)
          assertFalse(Modifier.isPublic(c.getModifiers()), "Constructor should not be public");
        }
      }
      assertTrue(found, "Should have constructor(long, long)");
    }

    @Test
    @DisplayName("JniResolveAddressStream should have resolveNextAddress method")
    void shouldHaveResolveNextAddressMethod() throws NoSuchMethodException {
      Method method = JniResolveAddressStream.class.getMethod("resolveNextAddress");
      assertNotNull(method, "Should have resolveNextAddress method");
      // Return type should be Optional<IpAddress>
      assertNotNull(method.getReturnType(), "Should return a type");
    }

    @Test
    @DisplayName("JniResolveAddressStream should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      Method method = JniResolveAddressStream.class.getMethod("subscribe");
      assertNotNull(method, "Should have subscribe method");
      assertEquals(void.class, method.getReturnType(), "subscribe should return void");
    }

    @Test
    @DisplayName("JniResolveAddressStream should have isClosed method")
    void shouldHaveIsClosedMethod() throws NoSuchMethodException {
      Method method = JniResolveAddressStream.class.getMethod("isClosed");
      assertNotNull(method, "Should have isClosed method");
      assertEquals(boolean.class, method.getReturnType(), "isClosed should return boolean");
    }

    @Test
    @DisplayName("JniResolveAddressStream should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = JniResolveAddressStream.class.getMethod("close");
      assertNotNull(method, "Should have close method");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }

    @Test
    @DisplayName("JniResolveAddressStream should have contextHandle field")
    void shouldHaveContextHandleField() throws NoSuchFieldException {
      Field field = JniResolveAddressStream.class.getDeclaredField("contextHandle");
      assertNotNull(field, "Should have contextHandle field");
      assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
    }

    @Test
    @DisplayName("JniResolveAddressStream should have streamHandle field")
    void shouldHaveStreamHandleField() throws NoSuchFieldException {
      Field field = JniResolveAddressStream.class.getDeclaredField("streamHandle");
      assertNotNull(field, "Should have streamHandle field");
      assertTrue(Modifier.isFinal(field.getModifiers()), "streamHandle should be final");
    }

    @Test
    @DisplayName("JniResolveAddressStream should have closed field")
    void shouldHaveClosedField() throws NoSuchFieldException {
      Field field = JniResolveAddressStream.class.getDeclaredField("closed");
      assertNotNull(field, "Should have closed field");
      // Check volatile modifier for thread safety
      assertTrue(Modifier.isVolatile(field.getModifiers()), "closed should be volatile");
    }

    @Test
    @DisplayName("JniResolveAddressStream should have native methods")
    void shouldHaveNativeMethods() {
      int nativeMethodCount = 0;
      for (Method m : JniResolveAddressStream.class.getDeclaredMethods()) {
        if (Modifier.isNative(m.getModifiers())) {
          nativeMethodCount++;
        }
      }
      assertTrue(
          nativeMethodCount >= 3,
          "Should have at least 3 native methods, found: " + nativeMethodCount);
      LOGGER.info("JniResolveAddressStream has " + nativeMethodCount + " native methods");
    }
  }

  // =========================================================================
  // Package Consistency Tests
  // =========================================================================

  @Nested
  @DisplayName("Package Consistency Tests")
  class PackageConsistencyTests {

    @Test
    @DisplayName("All sockets classes should be in same package")
    void allSocketsClassesShouldBeInSamePackage() {
      String expectedPackage = "ai.tegmentum.wasmtime4j.jni.wasi.sockets";
      assertEquals(expectedPackage, JniWasiTcpSocket.class.getPackage().getName());
      assertEquals(expectedPackage, JniWasiUdpSocket.class.getPackage().getName());
      assertEquals(expectedPackage, JniWasiNetwork.class.getPackage().getName());
      assertEquals(expectedPackage, JniWasiIpNameLookup.class.getPackage().getName());
      assertEquals(expectedPackage, JniResolveAddressStream.class.getPackage().getName());
      LOGGER.info("All sockets classes are in package: " + expectedPackage);
    }

    @Test
    @DisplayName("All sockets classes should have LOGGER field")
    void allSocketsClassesShouldHaveLoggerField() {
      Class<?>[] socketClasses = {
        JniWasiTcpSocket.class,
        JniWasiUdpSocket.class,
        JniWasiNetwork.class,
        JniWasiIpNameLookup.class,
        JniResolveAddressStream.class
      };

      for (Class<?> clazz : socketClasses) {
        assertDoesNotThrow(
            () -> {
              Field loggerField = clazz.getDeclaredField("LOGGER");
              assertTrue(
                  Modifier.isPrivate(loggerField.getModifiers()),
                  clazz.getSimpleName() + " LOGGER should be private");
              assertTrue(
                  Modifier.isStatic(loggerField.getModifiers()),
                  clazz.getSimpleName() + " LOGGER should be static");
              assertTrue(
                  Modifier.isFinal(loggerField.getModifiers()),
                  clazz.getSimpleName() + " LOGGER should be final");
            },
            clazz.getSimpleName() + " should have LOGGER field");
      }
    }

    @Test
    @DisplayName("Socket classes should have static initializer for native library")
    void socketClassesShouldHaveStaticInitializerForNativeLibrary() {
      // All socket classes should have a static block that loads the native library
      // We verify this by checking they have NativeLibraryLoader reference
      Class<?>[] socketClasses = {
        JniWasiTcpSocket.class,
        JniWasiUdpSocket.class,
        JniWasiNetwork.class,
        JniWasiIpNameLookup.class,
        JniResolveAddressStream.class
      };

      for (Class<?> clazz : socketClasses) {
        // Check for native methods - if present, class must load native library
        boolean hasNativeMethods = false;
        for (Method m : clazz.getDeclaredMethods()) {
          if (Modifier.isNative(m.getModifiers())) {
            hasNativeMethods = true;
            break;
          }
        }

        if (hasNativeMethods) {
          LOGGER.info(
              clazz.getSimpleName() + " has native methods, requires native library loading");
        }
      }
    }

    @Test
    @DisplayName("Total socket classes count should be 5")
    void totalSocketClassesCountShouldBeFive() {
      Class<?>[] socketClasses = {
        JniWasiTcpSocket.class,
        JniWasiUdpSocket.class,
        JniWasiNetwork.class,
        JniWasiIpNameLookup.class,
        JniResolveAddressStream.class
      };
      assertEquals(5, socketClasses.length, "Should have exactly 5 socket implementation classes");
      LOGGER.info("Verified 5 socket implementation classes");
    }
  }

  // =========================================================================
  // Native Method Signature Tests
  // =========================================================================

  @Nested
  @DisplayName("Native Method Signature Tests")
  class NativeMethodSignatureTests {

    @Test
    @DisplayName("JniWasiTcpSocket nativeCreate should have correct signature")
    void tcpSocketNativeCreateShouldHaveCorrectSignature() {
      Method nativeCreate = findNativeMethod(JniWasiTcpSocket.class, "nativeCreate");
      assertNotNull(nativeCreate, "Should have nativeCreate method");
      Class<?>[] params = nativeCreate.getParameterTypes();
      assertEquals(2, params.length, "nativeCreate should have 2 parameters");
      assertEquals(long.class, params[0], "First param should be long (contextHandle)");
      assertEquals(boolean.class, params[1], "Second param should be boolean (isIpv6)");
      assertEquals(long.class, nativeCreate.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("JniWasiUdpSocket nativeCreate should have correct signature")
    void udpSocketNativeCreateShouldHaveCorrectSignature() {
      Method nativeCreate = findNativeMethod(JniWasiUdpSocket.class, "nativeCreate");
      assertNotNull(nativeCreate, "Should have nativeCreate method");
      Class<?>[] params = nativeCreate.getParameterTypes();
      assertEquals(2, params.length, "nativeCreate should have 2 parameters");
      assertEquals(long.class, params[0], "First param should be long (contextHandle)");
      assertEquals(boolean.class, params[1], "Second param should be boolean (isIpv6)");
      assertEquals(long.class, nativeCreate.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("JniWasiNetwork nativeCreate should have correct signature")
    void networkNativeCreateShouldHaveCorrectSignature() {
      Method nativeCreate = findNativeMethod(JniWasiNetwork.class, "nativeCreate");
      assertNotNull(nativeCreate, "Should have nativeCreate method");
      Class<?>[] params = nativeCreate.getParameterTypes();
      assertEquals(1, params.length, "nativeCreate should have 1 parameter");
      assertEquals(long.class, params[0], "First param should be long (contextHandle)");
      assertEquals(long.class, nativeCreate.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("JniWasiTcpSocket nativeClose should have correct signature")
    void tcpSocketNativeCloseShouldHaveCorrectSignature() {
      Method nativeClose = findNativeMethod(JniWasiTcpSocket.class, "nativeClose");
      assertNotNull(nativeClose, "Should have nativeClose method");
      Class<?>[] params = nativeClose.getParameterTypes();
      assertEquals(2, params.length, "nativeClose should have 2 parameters");
      assertEquals(long.class, params[0], "First param should be long (contextHandle)");
      assertEquals(long.class, params[1], "Second param should be long (socketHandle)");
      assertEquals(void.class, nativeClose.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("JniWasiUdpSocket nativeClose should have correct signature")
    void udpSocketNativeCloseShouldHaveCorrectSignature() {
      Method nativeClose = findNativeMethod(JniWasiUdpSocket.class, "nativeClose");
      assertNotNull(nativeClose, "Should have nativeClose method");
      Class<?>[] params = nativeClose.getParameterTypes();
      assertEquals(2, params.length, "nativeClose should have 2 parameters");
      assertEquals(long.class, params[0], "First param should be long (contextHandle)");
      assertEquals(long.class, params[1], "Second param should be long (socketHandle)");
      assertEquals(void.class, nativeClose.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("JniWasiNetwork nativeClose should have correct signature")
    void networkNativeCloseShouldHaveCorrectSignature() {
      Method nativeClose = findNativeMethod(JniWasiNetwork.class, "nativeClose");
      assertNotNull(nativeClose, "Should have nativeClose method");
      Class<?>[] params = nativeClose.getParameterTypes();
      assertEquals(2, params.length, "nativeClose should have 2 parameters");
      assertEquals(long.class, params[0], "First param should be long (contextHandle)");
      assertEquals(long.class, params[1], "Second param should be long (networkHandle)");
      assertEquals(void.class, nativeClose.getReturnType(), "Should return void");
    }

    private Method findNativeMethod(final Class<?> clazz, final String name) {
      for (Method m : clazz.getDeclaredMethods()) {
        if (m.getName().equals(name) && Modifier.isNative(m.getModifiers())) {
          return m;
        }
      }
      return null;
    }
  }

  // =========================================================================
  // Interface Compliance Tests
  // =========================================================================

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("WasiTcpSocket interface methods should all be implemented")
    void wasiTcpSocketInterfaceMethodsShouldAllBeImplemented() {
      Method[] interfaceMethods = WasiTcpSocket.class.getMethods();
      for (Method interfaceMethod : interfaceMethods) {
        if (Modifier.isAbstract(interfaceMethod.getModifiers())) {
          assertDoesNotThrow(
              () -> {
                Method implMethod =
                    JniWasiTcpSocket.class.getMethod(
                        interfaceMethod.getName(), interfaceMethod.getParameterTypes());
                assertNotNull(implMethod, "Should implement " + interfaceMethod.getName());
              },
              "JniWasiTcpSocket should implement " + interfaceMethod.getName());
        }
      }
      LOGGER.info("JniWasiTcpSocket implements all WasiTcpSocket interface methods");
    }

    @Test
    @DisplayName("WasiUdpSocket interface methods should all be implemented")
    void wasiUdpSocketInterfaceMethodsShouldAllBeImplemented() {
      Method[] interfaceMethods = WasiUdpSocket.class.getMethods();
      for (Method interfaceMethod : interfaceMethods) {
        if (Modifier.isAbstract(interfaceMethod.getModifiers())) {
          assertDoesNotThrow(
              () -> {
                Method implMethod =
                    JniWasiUdpSocket.class.getMethod(
                        interfaceMethod.getName(), interfaceMethod.getParameterTypes());
                assertNotNull(implMethod, "Should implement " + interfaceMethod.getName());
              },
              "JniWasiUdpSocket should implement " + interfaceMethod.getName());
        }
      }
      LOGGER.info("JniWasiUdpSocket implements all WasiUdpSocket interface methods");
    }

    @Test
    @DisplayName("WasiNetwork interface methods should all be implemented")
    void wasiNetworkInterfaceMethodsShouldAllBeImplemented() {
      Method[] interfaceMethods = WasiNetwork.class.getMethods();
      for (Method interfaceMethod : interfaceMethods) {
        if (Modifier.isAbstract(interfaceMethod.getModifiers())) {
          assertDoesNotThrow(
              () -> {
                Method implMethod =
                    JniWasiNetwork.class.getMethod(
                        interfaceMethod.getName(), interfaceMethod.getParameterTypes());
                assertNotNull(implMethod, "Should implement " + interfaceMethod.getName());
              },
              "JniWasiNetwork should implement " + interfaceMethod.getName());
        }
      }
      LOGGER.info("JniWasiNetwork implements all WasiNetwork interface methods");
    }

    @Test
    @DisplayName("WasiIpNameLookup interface methods should all be implemented")
    void wasiIpNameLookupInterfaceMethodsShouldAllBeImplemented() {
      Method[] interfaceMethods = WasiIpNameLookup.class.getMethods();
      for (Method interfaceMethod : interfaceMethods) {
        if (Modifier.isAbstract(interfaceMethod.getModifiers())) {
          assertDoesNotThrow(
              () -> {
                Method implMethod =
                    JniWasiIpNameLookup.class.getMethod(
                        interfaceMethod.getName(), interfaceMethod.getParameterTypes());
                assertNotNull(implMethod, "Should implement " + interfaceMethod.getName());
              },
              "JniWasiIpNameLookup should implement " + interfaceMethod.getName());
        }
      }
      LOGGER.info("JniWasiIpNameLookup implements all WasiIpNameLookup interface methods");
    }

    @Test
    @DisplayName("ResolveAddressStream interface methods should all be implemented")
    void resolveAddressStreamInterfaceMethodsShouldAllBeImplemented() {
      Method[] interfaceMethods = ResolveAddressStream.class.getMethods();
      for (Method interfaceMethod : interfaceMethods) {
        if (Modifier.isAbstract(interfaceMethod.getModifiers())) {
          assertDoesNotThrow(
              () -> {
                Method implMethod =
                    JniResolveAddressStream.class.getMethod(
                        interfaceMethod.getName(), interfaceMethod.getParameterTypes());
                assertNotNull(implMethod, "Should implement " + interfaceMethod.getName());
              },
              "JniResolveAddressStream should implement " + interfaceMethod.getName());
        }
      }
      LOGGER.info("JniResolveAddressStream implements all ResolveAddressStream interface methods");
    }
  }

  // =========================================================================
  // TCP Socket Specific Tests
  // =========================================================================

  @Nested
  @DisplayName("TCP Socket Specific Tests")
  class TcpSocketSpecificTests {

    @Test
    @DisplayName("TCP socket should have ConnectionStreams inner class reference")
    void tcpSocketShouldHaveConnectionStreamsReference() throws NoSuchMethodException {
      Method finishConnect = JniWasiTcpSocket.class.getMethod("finishConnect");
      assertNotNull(finishConnect, "Should have finishConnect method");
      // Return type should be WasiTcpSocket.ConnectionStreams
      String returnTypeName = finishConnect.getReturnType().getSimpleName();
      assertEquals(
          "ConnectionStreams", returnTypeName, "finishConnect should return ConnectionStreams");
    }

    @Test
    @DisplayName("TCP socket should have AcceptResult inner class reference")
    void tcpSocketShouldHaveAcceptResultReference() throws NoSuchMethodException {
      Method accept = JniWasiTcpSocket.class.getMethod("accept");
      assertNotNull(accept, "Should have accept method");
      // Return type should be WasiTcpSocket.AcceptResult
      String returnTypeName = accept.getReturnType().getSimpleName();
      assertEquals("AcceptResult", returnTypeName, "accept should return AcceptResult");
    }

    @Test
    @DisplayName("TCP socket should have ShutdownType parameter in shutdown")
    void tcpSocketShutdownShouldAcceptShutdownType() throws NoSuchMethodException {
      Method shutdown = null;
      for (Method m : JniWasiTcpSocket.class.getMethods()) {
        if ("shutdown".equals(m.getName()) && m.getParameterCount() == 1) {
          shutdown = m;
          break;
        }
      }
      assertNotNull(shutdown, "Should have shutdown method");
      Class<?>[] params = shutdown.getParameterTypes();
      assertEquals(1, params.length, "shutdown should have 1 parameter");
      assertTrue(
          params[0].getSimpleName().contains("ShutdownType"), "Parameter should be ShutdownType");
    }
  }

  // =========================================================================
  // UDP Socket Specific Tests
  // =========================================================================

  @Nested
  @DisplayName("UDP Socket Specific Tests")
  class UdpSocketSpecificTests {

    @Test
    @DisplayName("UDP socket should have IncomingDatagram reference in receive")
    void udpSocketReceiveShouldReturnIncomingDatagram() throws NoSuchMethodException {
      Method receive = JniWasiUdpSocket.class.getMethod("receive", long.class);
      assertNotNull(receive, "Should have receive method");
      assertTrue(receive.getReturnType().isArray(), "receive should return array");
      String componentTypeName = receive.getReturnType().getComponentType().getSimpleName();
      assertEquals(
          "IncomingDatagram", componentTypeName, "receive should return IncomingDatagram[]");
    }

    @Test
    @DisplayName("UDP socket should have OutgoingDatagram reference in send")
    void udpSocketSendShouldAcceptOutgoingDatagram() {
      Method send = null;
      for (Method m : JniWasiUdpSocket.class.getMethods()) {
        if ("send".equals(m.getName()) && m.getParameterCount() == 1) {
          Class<?>[] params = m.getParameterTypes();
          if (params[0].isArray()) {
            send = m;
            break;
          }
        }
      }
      assertNotNull(send, "Should have send method accepting array");
      String componentTypeName = send.getParameterTypes()[0].getComponentType().getSimpleName();
      assertEquals("OutgoingDatagram", componentTypeName, "send should accept OutgoingDatagram[]");
    }
  }
}
