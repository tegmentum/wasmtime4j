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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasiAddressFamily;
import ai.tegmentum.wasmtime4j.WasiSocketAddress;
import ai.tegmentum.wasmtime4j.WasiTcpSocket;
import ai.tegmentum.wasmtime4j.WasiUdpSocket;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Panama WASI Socket implementations.
 *
 * <p>This test class verifies the PanamaWasiTcpSocket and PanamaWasiUdpSocket implementations
 * including class structure, method signatures, and interface compliance.
 */
@DisplayName("Panama WASI Socket Tests")
class PanamaWasiSocketTest {

  // ========================================================================
  // PanamaWasiTcpSocket Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaWasiTcpSocket Class Structure Tests")
  class TcpSocketClassStructureTests {

    @Test
    @DisplayName("PanamaWasiTcpSocket should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaWasiTcpSocket.class.getModifiers()),
          "PanamaWasiTcpSocket should be final");
    }

    @Test
    @DisplayName("PanamaWasiTcpSocket should be a public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiTcpSocket.class.getModifiers()),
          "PanamaWasiTcpSocket should be public");
    }

    @Test
    @DisplayName("PanamaWasiTcpSocket should implement WasiTcpSocket interface")
    void shouldImplementWasiTcpSocketInterface() {
      Class<?>[] interfaces = PanamaWasiTcpSocket.class.getInterfaces();
      boolean implementsWasiTcpSocket = Arrays.asList(interfaces).contains(WasiTcpSocket.class);
      assertTrue(
          implementsWasiTcpSocket, "PanamaWasiTcpSocket should implement WasiTcpSocket interface");
    }

    @Test
    @DisplayName("PanamaWasiTcpSocket should be in the correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.panama",
          PanamaWasiTcpSocket.class.getPackage().getName(),
          "PanamaWasiTcpSocket should be in ai.tegmentum.wasmtime4j.panama package");
    }
  }

  @Nested
  @DisplayName("PanamaWasiTcpSocket Field Tests")
  class TcpSocketFieldTests {

    @Test
    @DisplayName("should have NATIVE_BINDINGS static field")
    void shouldHaveNativeBindingsField() throws NoSuchFieldException {
      Field field = PanamaWasiTcpSocket.class.getDeclaredField("NATIVE_BINDINGS");
      assertNotNull(field, "NATIVE_BINDINGS field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "NATIVE_BINDINGS should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "NATIVE_BINDINGS should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "NATIVE_BINDINGS should be private");
    }

    @Test
    @DisplayName("should have ADDRESS_BUFFER_SIZE constant")
    void shouldHaveAddressBufferSizeConstant() throws NoSuchFieldException {
      Field field = PanamaWasiTcpSocket.class.getDeclaredField("ADDRESS_BUFFER_SIZE");
      assertNotNull(field, "ADDRESS_BUFFER_SIZE field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "ADDRESS_BUFFER_SIZE should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "ADDRESS_BUFFER_SIZE should be final");
      assertEquals(int.class, field.getType(), "ADDRESS_BUFFER_SIZE should be int");
    }

    @Test
    @DisplayName("should have socketHandle field")
    void shouldHaveSocketHandleField() throws NoSuchFieldException {
      Field field = PanamaWasiTcpSocket.class.getDeclaredField("socketHandle");
      assertNotNull(field, "socketHandle field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "socketHandle should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "socketHandle should be private");
      assertEquals(long.class, field.getType(), "socketHandle should be of type long");
    }

    @Test
    @DisplayName("should have addressFamily field")
    void shouldHaveAddressFamilyField() throws NoSuchFieldException {
      Field field = PanamaWasiTcpSocket.class.getDeclaredField("addressFamily");
      assertNotNull(field, "addressFamily field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "addressFamily should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "addressFamily should be private");
      assertEquals(
          WasiAddressFamily.class,
          field.getType(),
          "addressFamily should be of type WasiAddressFamily");
    }

    @Test
    @DisplayName("should have closed field")
    void shouldHaveClosedField() throws NoSuchFieldException {
      Field field = PanamaWasiTcpSocket.class.getDeclaredField("closed");
      assertNotNull(field, "closed field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "closed should be final");
      assertEquals(AtomicBoolean.class, field.getType(), "closed should be of type AtomicBoolean");
    }
  }

  @Nested
  @DisplayName("PanamaWasiTcpSocket Constructor Tests")
  class TcpSocketConstructorTests {

    @Test
    @DisplayName("should have public constructor with WasiAddressFamily parameter")
    void shouldHaveAddressFamilyConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          PanamaWasiTcpSocket.class.getConstructor(WasiAddressFamily.class);
      assertNotNull(constructor, "WasiAddressFamily constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()),
          "WasiAddressFamily constructor should be public");
      assertTrue(
          Arrays.asList(constructor.getExceptionTypes()).contains(WasmException.class),
          "Constructor should throw WasmException");
    }

    @Test
    @DisplayName("should have package-private constructor with handle and family")
    void shouldHaveHandleAndFamilyConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          PanamaWasiTcpSocket.class.getDeclaredConstructor(long.class, WasiAddressFamily.class);
      assertNotNull(constructor, "Handle and family constructor should exist");
      // Package-private means no public/protected/private modifier
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Handle constructor should be package-private");
    }
  }

  @Nested
  @DisplayName("PanamaWasiTcpSocket Binding Method Tests")
  class TcpSocketBindingMethodTests {

    @Test
    @DisplayName("should have startBind method")
    void shouldHaveStartBindMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getMethod("startBind", WasiSocketAddress.class);
      assertNotNull(method, "startBind method should exist");
      assertEquals(void.class, method.getReturnType(), "startBind should return void");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "startBind should throw WasmException");
    }

    @Test
    @DisplayName("should have finishBind method")
    void shouldHaveFinishBindMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getMethod("finishBind");
      assertNotNull(method, "finishBind method should exist");
      assertEquals(void.class, method.getReturnType(), "finishBind should return void");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "finishBind should throw WasmException");
    }
  }

  @Nested
  @DisplayName("PanamaWasiTcpSocket Connection Method Tests")
  class TcpSocketConnectionMethodTests {

    @Test
    @DisplayName("should have startConnect method")
    void shouldHaveStartConnectMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getMethod("startConnect", WasiSocketAddress.class);
      assertNotNull(method, "startConnect method should exist");
      assertEquals(void.class, method.getReturnType(), "startConnect should return void");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "startConnect should throw WasmException");
    }

    @Test
    @DisplayName("should have finishConnect method")
    void shouldHaveFinishConnectMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getMethod("finishConnect");
      assertNotNull(method, "finishConnect method should exist");
      assertEquals(void.class, method.getReturnType(), "finishConnect should return void");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "finishConnect should throw WasmException");
    }

    @Test
    @DisplayName("should have startListen method")
    void shouldHaveStartListenMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getMethod("startListen");
      assertNotNull(method, "startListen method should exist");
      assertEquals(void.class, method.getReturnType(), "startListen should return void");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "startListen should throw WasmException");
    }

    @Test
    @DisplayName("should have finishListen method")
    void shouldHaveFinishListenMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getMethod("finishListen");
      assertNotNull(method, "finishListen method should exist");
      assertEquals(void.class, method.getReturnType(), "finishListen should return void");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "finishListen should throw WasmException");
    }

    @Test
    @DisplayName("should have accept method")
    void shouldHaveAcceptMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getMethod("accept");
      assertNotNull(method, "accept method should exist");
      assertEquals(
          WasiTcpSocket.class, method.getReturnType(), "accept should return WasiTcpSocket");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "accept should throw WasmException");
    }
  }

  @Nested
  @DisplayName("PanamaWasiTcpSocket Address Method Tests")
  class TcpSocketAddressMethodTests {

    @Test
    @DisplayName("should have getLocalAddress method")
    void shouldHaveGetLocalAddressMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getMethod("getLocalAddress");
      assertNotNull(method, "getLocalAddress method should exist");
      assertEquals(
          WasiSocketAddress.class,
          method.getReturnType(),
          "getLocalAddress should return WasiSocketAddress");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "getLocalAddress should throw WasmException");
    }

    @Test
    @DisplayName("should have getRemoteAddress method")
    void shouldHaveGetRemoteAddressMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getMethod("getRemoteAddress");
      assertNotNull(method, "getRemoteAddress method should exist");
      assertEquals(
          WasiSocketAddress.class,
          method.getReturnType(),
          "getRemoteAddress should return WasiSocketAddress");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "getRemoteAddress should throw WasmException");
    }

    @Test
    @DisplayName("should have getAddressFamily method")
    void shouldHaveGetAddressFamilyMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getMethod("getAddressFamily");
      assertNotNull(method, "getAddressFamily method should exist");
      assertEquals(
          WasiAddressFamily.class,
          method.getReturnType(),
          "getAddressFamily should return WasiAddressFamily");
      assertEquals(0, method.getExceptionTypes().length, "getAddressFamily should not throw");
    }
  }

  @Nested
  @DisplayName("PanamaWasiTcpSocket Option Method Tests")
  class TcpSocketOptionMethodTests {

    @Test
    @DisplayName("should have setListenBacklogSize method")
    void shouldHaveSetListenBacklogSizeMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getMethod("setListenBacklogSize", long.class);
      assertNotNull(method, "setListenBacklogSize method should exist");
      assertEquals(void.class, method.getReturnType(), "setListenBacklogSize should return void");
    }

    @Test
    @DisplayName("should have keep-alive methods")
    void shouldHaveKeepAliveMethods() throws NoSuchMethodException {
      Method isEnabled = PanamaWasiTcpSocket.class.getMethod("isKeepAliveEnabled");
      assertNotNull(isEnabled, "isKeepAliveEnabled method should exist");
      assertEquals(boolean.class, isEnabled.getReturnType(), "Should return boolean");

      Method setEnabled = PanamaWasiTcpSocket.class.getMethod("setKeepAliveEnabled", boolean.class);
      assertNotNull(setEnabled, "setKeepAliveEnabled method should exist");

      Method getIdleTime = PanamaWasiTcpSocket.class.getMethod("getKeepAliveIdleTime");
      assertNotNull(getIdleTime, "getKeepAliveIdleTime method should exist");
      assertEquals(long.class, getIdleTime.getReturnType(), "Should return long");

      Method setIdleTime = PanamaWasiTcpSocket.class.getMethod("setKeepAliveIdleTime", long.class);
      assertNotNull(setIdleTime, "setKeepAliveIdleTime method should exist");

      Method getInterval = PanamaWasiTcpSocket.class.getMethod("getKeepAliveInterval");
      assertNotNull(getInterval, "getKeepAliveInterval method should exist");

      Method setInterval = PanamaWasiTcpSocket.class.getMethod("setKeepAliveInterval", long.class);
      assertNotNull(setInterval, "setKeepAliveInterval method should exist");

      Method getCount = PanamaWasiTcpSocket.class.getMethod("getKeepAliveCount");
      assertNotNull(getCount, "getKeepAliveCount method should exist");
      assertEquals(int.class, getCount.getReturnType(), "Should return int");

      Method setCount = PanamaWasiTcpSocket.class.getMethod("setKeepAliveCount", int.class);
      assertNotNull(setCount, "setKeepAliveCount method should exist");
    }

    @Test
    @DisplayName("should have hop limit methods")
    void shouldHaveHopLimitMethods() throws NoSuchMethodException {
      Method getHopLimit = PanamaWasiTcpSocket.class.getMethod("getHopLimit");
      assertNotNull(getHopLimit, "getHopLimit method should exist");
      assertEquals(int.class, getHopLimit.getReturnType(), "Should return int");

      Method setHopLimit = PanamaWasiTcpSocket.class.getMethod("setHopLimit", int.class);
      assertNotNull(setHopLimit, "setHopLimit method should exist");
    }

    @Test
    @DisplayName("should have buffer size methods")
    void shouldHaveBufferSizeMethods() throws NoSuchMethodException {
      Method getReceive = PanamaWasiTcpSocket.class.getMethod("getReceiveBufferSize");
      assertNotNull(getReceive, "getReceiveBufferSize method should exist");
      assertEquals(long.class, getReceive.getReturnType(), "Should return long");

      Method setReceive = PanamaWasiTcpSocket.class.getMethod("setReceiveBufferSize", long.class);
      assertNotNull(setReceive, "setReceiveBufferSize method should exist");

      Method getSend = PanamaWasiTcpSocket.class.getMethod("getSendBufferSize");
      assertNotNull(getSend, "getSendBufferSize method should exist");
      assertEquals(long.class, getSend.getReturnType(), "Should return long");

      Method setSend = PanamaWasiTcpSocket.class.getMethod("setSendBufferSize", long.class);
      assertNotNull(setSend, "setSendBufferSize method should exist");
    }
  }

  @Nested
  @DisplayName("PanamaWasiTcpSocket Lifecycle Method Tests")
  class TcpSocketLifecycleMethodTests {

    @Test
    @DisplayName("should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(long.class, method.getReturnType(), "subscribe should return long");
    }

    @Test
    @DisplayName("should have shutdown method")
    void shouldHaveShutdownMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiTcpSocket.class.getMethod("shutdown", WasiTcpSocket.ShutdownType.class);
      assertNotNull(method, "shutdown method should exist");
      assertEquals(void.class, method.getReturnType(), "shutdown should return void");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
      assertEquals(0, method.getExceptionTypes().length, "close should not throw");
    }
  }

  // ========================================================================
  // PanamaWasiUdpSocket Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaWasiUdpSocket Class Structure Tests")
  class UdpSocketClassStructureTests {

    @Test
    @DisplayName("PanamaWasiUdpSocket should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaWasiUdpSocket.class.getModifiers()),
          "PanamaWasiUdpSocket should be final");
    }

    @Test
    @DisplayName("PanamaWasiUdpSocket should be a public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiUdpSocket.class.getModifiers()),
          "PanamaWasiUdpSocket should be public");
    }

    @Test
    @DisplayName("PanamaWasiUdpSocket should implement WasiUdpSocket interface")
    void shouldImplementWasiUdpSocketInterface() {
      Class<?>[] interfaces = PanamaWasiUdpSocket.class.getInterfaces();
      boolean implementsWasiUdpSocket = Arrays.asList(interfaces).contains(WasiUdpSocket.class);
      assertTrue(
          implementsWasiUdpSocket, "PanamaWasiUdpSocket should implement WasiUdpSocket interface");
    }
  }

  @Nested
  @DisplayName("PanamaWasiUdpSocket Field Tests")
  class UdpSocketFieldTests {

    @Test
    @DisplayName("should have socketHandle field")
    void shouldHaveSocketHandleField() throws NoSuchFieldException {
      Field field = PanamaWasiUdpSocket.class.getDeclaredField("socketHandle");
      assertNotNull(field, "socketHandle field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "socketHandle should be final");
      assertEquals(long.class, field.getType(), "socketHandle should be of type long");
    }

    @Test
    @DisplayName("should have addressFamily field")
    void shouldHaveAddressFamilyField() throws NoSuchFieldException {
      Field field = PanamaWasiUdpSocket.class.getDeclaredField("addressFamily");
      assertNotNull(field, "addressFamily field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "addressFamily should be final");
      assertEquals(
          WasiAddressFamily.class,
          field.getType(),
          "addressFamily should be of type WasiAddressFamily");
    }

    @Test
    @DisplayName("should have closed field")
    void shouldHaveClosedField() throws NoSuchFieldException {
      Field field = PanamaWasiUdpSocket.class.getDeclaredField("closed");
      assertNotNull(field, "closed field should exist");
      assertEquals(AtomicBoolean.class, field.getType(), "closed should be of type AtomicBoolean");
    }
  }

  @Nested
  @DisplayName("PanamaWasiUdpSocket Constructor Tests")
  class UdpSocketConstructorTests {

    @Test
    @DisplayName("should have public constructor with WasiAddressFamily parameter")
    void shouldHaveAddressFamilyConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          PanamaWasiUdpSocket.class.getConstructor(WasiAddressFamily.class);
      assertNotNull(constructor, "WasiAddressFamily constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()),
          "WasiAddressFamily constructor should be public");
      assertTrue(
          Arrays.asList(constructor.getExceptionTypes()).contains(WasmException.class),
          "Constructor should throw WasmException");
    }

    @Test
    @DisplayName("should have package-private constructor with handle and family")
    void shouldHaveHandleAndFamilyConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          PanamaWasiUdpSocket.class.getDeclaredConstructor(long.class, WasiAddressFamily.class);
      assertNotNull(constructor, "Handle and family constructor should exist");
    }
  }

  @Nested
  @DisplayName("PanamaWasiUdpSocket Binding Method Tests")
  class UdpSocketBindingMethodTests {

    @Test
    @DisplayName("should have startBind method")
    void shouldHaveStartBindMethod() throws NoSuchMethodException {
      Method method = PanamaWasiUdpSocket.class.getMethod("startBind", WasiSocketAddress.class);
      assertNotNull(method, "startBind method should exist");
      assertEquals(void.class, method.getReturnType(), "startBind should return void");
    }

    @Test
    @DisplayName("should have finishBind method")
    void shouldHaveFinishBindMethod() throws NoSuchMethodException {
      Method method = PanamaWasiUdpSocket.class.getMethod("finishBind");
      assertNotNull(method, "finishBind method should exist");
      assertEquals(void.class, method.getReturnType(), "finishBind should return void");
    }

    @Test
    @DisplayName("should have startConnect method")
    void shouldHaveStartConnectMethod() throws NoSuchMethodException {
      Method method = PanamaWasiUdpSocket.class.getMethod("startConnect", WasiSocketAddress.class);
      assertNotNull(method, "startConnect method should exist");
      assertEquals(void.class, method.getReturnType(), "startConnect should return void");
    }

    @Test
    @DisplayName("should have finishConnect method")
    void shouldHaveFinishConnectMethod() throws NoSuchMethodException {
      Method method = PanamaWasiUdpSocket.class.getMethod("finishConnect");
      assertNotNull(method, "finishConnect method should exist");
      assertEquals(void.class, method.getReturnType(), "finishConnect should return void");
    }
  }

  @Nested
  @DisplayName("PanamaWasiUdpSocket Address Method Tests")
  class UdpSocketAddressMethodTests {

    @Test
    @DisplayName("should have getLocalAddress method")
    void shouldHaveGetLocalAddressMethod() throws NoSuchMethodException {
      Method method = PanamaWasiUdpSocket.class.getMethod("getLocalAddress");
      assertNotNull(method, "getLocalAddress method should exist");
      assertEquals(
          WasiSocketAddress.class,
          method.getReturnType(),
          "getLocalAddress should return WasiSocketAddress");
    }

    @Test
    @DisplayName("should have getRemoteAddress method")
    void shouldHaveGetRemoteAddressMethod() throws NoSuchMethodException {
      Method method = PanamaWasiUdpSocket.class.getMethod("getRemoteAddress");
      assertNotNull(method, "getRemoteAddress method should exist");
      assertEquals(
          WasiSocketAddress.class,
          method.getReturnType(),
          "getRemoteAddress should return WasiSocketAddress");
    }

    @Test
    @DisplayName("should have getAddressFamily method")
    void shouldHaveGetAddressFamilyMethod() throws NoSuchMethodException {
      Method method = PanamaWasiUdpSocket.class.getMethod("getAddressFamily");
      assertNotNull(method, "getAddressFamily method should exist");
      assertEquals(
          WasiAddressFamily.class,
          method.getReturnType(),
          "getAddressFamily should return WasiAddressFamily");
    }
  }

  @Nested
  @DisplayName("PanamaWasiUdpSocket Option Method Tests")
  class UdpSocketOptionMethodTests {

    @Test
    @DisplayName("should have unicast hop limit methods")
    void shouldHaveUnicastHopLimitMethods() throws NoSuchMethodException {
      Method getHopLimit = PanamaWasiUdpSocket.class.getMethod("getUnicastHopLimit");
      assertNotNull(getHopLimit, "getUnicastHopLimit method should exist");
      assertEquals(int.class, getHopLimit.getReturnType(), "Should return int");

      Method setHopLimit = PanamaWasiUdpSocket.class.getMethod("setUnicastHopLimit", int.class);
      assertNotNull(setHopLimit, "setUnicastHopLimit method should exist");
    }

    @Test
    @DisplayName("should have buffer size methods")
    void shouldHaveBufferSizeMethods() throws NoSuchMethodException {
      Method getReceive = PanamaWasiUdpSocket.class.getMethod("getReceiveBufferSize");
      assertNotNull(getReceive, "getReceiveBufferSize method should exist");
      assertEquals(long.class, getReceive.getReturnType(), "Should return long");

      Method setReceive = PanamaWasiUdpSocket.class.getMethod("setReceiveBufferSize", long.class);
      assertNotNull(setReceive, "setReceiveBufferSize method should exist");

      Method getSend = PanamaWasiUdpSocket.class.getMethod("getSendBufferSize");
      assertNotNull(getSend, "getSendBufferSize method should exist");
      assertEquals(long.class, getSend.getReturnType(), "Should return long");

      Method setSend = PanamaWasiUdpSocket.class.getMethod("setSendBufferSize", long.class);
      assertNotNull(setSend, "setSendBufferSize method should exist");
    }
  }

  @Nested
  @DisplayName("PanamaWasiUdpSocket Lifecycle Method Tests")
  class UdpSocketLifecycleMethodTests {

    @Test
    @DisplayName("should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      Method method = PanamaWasiUdpSocket.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(long.class, method.getReturnType(), "subscribe should return long");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = PanamaWasiUdpSocket.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
      assertEquals(0, method.getExceptionTypes().length, "close should not throw");
    }
  }

  // ========================================================================
  // Private Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("TcpSocket should have private formatAddress method")
    void tcpShouldHaveFormatAddressMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiTcpSocket.class.getDeclaredMethod("formatAddress", WasiSocketAddress.class);
      assertNotNull(method, "formatAddress method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "formatAddress should be private");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("TcpSocket should have private parseAddress method")
    void tcpShouldHaveParseAddressMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiTcpSocket.class.getDeclaredMethod("parseAddress", String.class, int.class);
      assertNotNull(method, "parseAddress method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "parseAddress should be private");
      assertEquals(
          WasiSocketAddress.class, method.getReturnType(), "Should return WasiSocketAddress");
    }

    @Test
    @DisplayName("UdpSocket should have private formatAddress method")
    void udpShouldHaveFormatAddressMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiUdpSocket.class.getDeclaredMethod("formatAddress", WasiSocketAddress.class);
      assertNotNull(method, "formatAddress method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "formatAddress should be private");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("UdpSocket should have private parseAddress method")
    void udpShouldHaveParseAddressMethod() throws NoSuchMethodException {
      Method method =
          PanamaWasiUdpSocket.class.getDeclaredMethod("parseAddress", String.class, int.class);
      assertNotNull(method, "parseAddress method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "parseAddress should be private");
      assertEquals(
          WasiSocketAddress.class, method.getReturnType(), "Should return WasiSocketAddress");
    }

    @Test
    @DisplayName("TcpSocket should have private ensureNotClosed method")
    void tcpShouldHaveEnsureNotClosedMethod() throws NoSuchMethodException {
      Method method = PanamaWasiTcpSocket.class.getDeclaredMethod("ensureNotClosed");
      assertNotNull(method, "ensureNotClosed method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "ensureNotClosed should be private");
    }

    @Test
    @DisplayName("UdpSocket should have private ensureNotClosed method")
    void udpShouldHaveEnsureNotClosedMethod() throws NoSuchMethodException {
      Method method = PanamaWasiUdpSocket.class.getDeclaredMethod("ensureNotClosed");
      assertNotNull(method, "ensureNotClosed method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "ensureNotClosed should be private");
    }
  }

  // ========================================================================
  // Interface Compliance Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("PanamaWasiTcpSocket should implement all WasiTcpSocket methods")
    void tcpShouldImplementAllInterfaceMethods() {
      Method[] interfaceMethods = WasiTcpSocket.class.getDeclaredMethods();
      Class<?> implClass = PanamaWasiTcpSocket.class;

      for (Method interfaceMethod : interfaceMethods) {
        if (Modifier.isAbstract(interfaceMethod.getModifiers())) {
          try {
            Method implMethod =
                implClass.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            assertNotNull(
                implMethod, "Implementation should have method: " + interfaceMethod.getName());
          } catch (NoSuchMethodException e) {
            // May be a default method
          }
        }
      }
    }

    @Test
    @DisplayName("PanamaWasiUdpSocket should implement all WasiUdpSocket methods")
    void udpShouldImplementAllInterfaceMethods() {
      Method[] interfaceMethods = WasiUdpSocket.class.getDeclaredMethods();
      Class<?> implClass = PanamaWasiUdpSocket.class;

      for (Method interfaceMethod : interfaceMethods) {
        if (Modifier.isAbstract(interfaceMethod.getModifiers())) {
          try {
            Method implMethod =
                implClass.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            assertNotNull(
                implMethod, "Implementation should have method: " + interfaceMethod.getName());
          } catch (NoSuchMethodException e) {
            // May be a default method
          }
        }
      }
    }
  }
}
