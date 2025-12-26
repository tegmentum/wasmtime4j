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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiUdpSocket} interface.
 *
 * <p>This test class verifies the interface structure and method signatures for the WASI UDP socket
 * API using reflection-based testing.
 */
@DisplayName("WasiUdpSocket Interface Tests")
class WasiUdpSocketTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WasiUdpSocket should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiUdpSocket.class.isInterface(), "WasiUdpSocket should be an interface");
    }

    @Test
    @DisplayName("WasiUdpSocket should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiUdpSocket.class.getModifiers()), "WasiUdpSocket should be public");
    }

    @Test
    @DisplayName("WasiUdpSocket should not extend any interface")
    void shouldNotExtendAnyInterface() {
      Class<?>[] interfaces = WasiUdpSocket.class.getInterfaces();
      assertEquals(0, interfaces.length, "WasiUdpSocket should not extend any interface");
    }
  }

  // ========================================================================
  // Bind Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Bind Method Tests")
  class BindMethodTests {

    @Test
    @DisplayName("should have startBind method with WasiNetwork and IpSocketAddress parameters")
    void shouldHaveStartBindMethod() throws NoSuchMethodException {
      Method method =
          WasiUdpSocket.class.getMethod("startBind", WasiNetwork.class, IpSocketAddress.class);
      assertNotNull(method, "startBind method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(2, method.getParameterCount(), "startBind should have 2 parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "startBind should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have finishBind method")
    void shouldHaveFinishBindMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.class.getMethod("finishBind");
      assertNotNull(method, "finishBind method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "finishBind should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "finishBind should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Stream Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Stream Method Tests")
  class StreamMethodTests {

    @Test
    @DisplayName("should have stream method with WasiNetwork and IpSocketAddress parameters")
    void shouldHaveStreamMethod() throws NoSuchMethodException {
      Method method =
          WasiUdpSocket.class.getMethod("stream", WasiNetwork.class, IpSocketAddress.class);
      assertNotNull(method, "stream method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(2, method.getParameterCount(), "stream should have 2 parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "stream should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }
  }

  // ========================================================================
  // Address Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Address Method Tests")
  class AddressMethodTests {

    @Test
    @DisplayName("should have localAddress method returning IpSocketAddress")
    void shouldHaveLocalAddressMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.class.getMethod("localAddress");
      assertNotNull(method, "localAddress method should exist");
      assertEquals(
          IpSocketAddress.class, method.getReturnType(), "Return type should be IpSocketAddress");
      assertEquals(0, method.getParameterCount(), "localAddress should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "localAddress should throw 1 exception");
    }

    @Test
    @DisplayName("should have remoteAddress method returning IpSocketAddress")
    void shouldHaveRemoteAddressMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.class.getMethod("remoteAddress");
      assertNotNull(method, "remoteAddress method should exist");
      assertEquals(
          IpSocketAddress.class, method.getReturnType(), "Return type should be IpSocketAddress");
      assertEquals(0, method.getParameterCount(), "remoteAddress should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "remoteAddress should throw 1 exception");
    }

    @Test
    @DisplayName("should have addressFamily method returning IpAddressFamily")
    void shouldHaveAddressFamilyMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.class.getMethod("addressFamily");
      assertNotNull(method, "addressFamily method should exist");
      assertEquals(
          IpAddressFamily.class, method.getReturnType(), "Return type should be IpAddressFamily");
      assertEquals(0, method.getParameterCount(), "addressFamily should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "addressFamily should throw 1 exception");
    }
  }

  // ========================================================================
  // Socket Options Tests
  // ========================================================================

  @Nested
  @DisplayName("Socket Options Tests")
  class SocketOptionsTests {

    @Test
    @DisplayName("should have setUnicastHopLimit method")
    void shouldHaveSetUnicastHopLimitMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.class.getMethod("setUnicastHopLimit", int.class);
      assertNotNull(method, "setUnicastHopLimit method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "setUnicastHopLimit should have 1 parameter");
      assertEquals(int.class, method.getParameterTypes()[0], "Parameter should be int");
    }
  }

  // ========================================================================
  // Buffer Size Tests
  // ========================================================================

  @Nested
  @DisplayName("Buffer Size Tests")
  class BufferSizeTests {

    @Test
    @DisplayName("should have receiveBufferSize method returning long")
    void shouldHaveReceiveBufferSizeMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.class.getMethod("receiveBufferSize");
      assertNotNull(method, "receiveBufferSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(0, method.getParameterCount(), "receiveBufferSize should have no parameters");
    }

    @Test
    @DisplayName("should have setReceiveBufferSize method")
    void shouldHaveSetReceiveBufferSizeMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.class.getMethod("setReceiveBufferSize", long.class);
      assertNotNull(method, "setReceiveBufferSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(1, method.getParameterCount(), "setReceiveBufferSize should have 1 parameter");
    }

    @Test
    @DisplayName("should have sendBufferSize method returning long")
    void shouldHaveSendBufferSizeMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.class.getMethod("sendBufferSize");
      assertNotNull(method, "sendBufferSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setSendBufferSize method")
    void shouldHaveSetSendBufferSizeMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.class.getMethod("setSendBufferSize", long.class);
      assertNotNull(method, "setSendBufferSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // Datagram Transfer Tests
  // ========================================================================

  @Nested
  @DisplayName("Datagram Transfer Tests")
  class DatagramTransferTests {

    @Test
    @DisplayName("should have receive method returning array of IncomingDatagram")
    void shouldHaveReceiveMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.class.getMethod("receive", long.class);
      assertNotNull(method, "receive method should exist");
      assertEquals(
          WasiUdpSocket.IncomingDatagram[].class,
          method.getReturnType(),
          "Return type should be IncomingDatagram[]");
      assertEquals(1, method.getParameterCount(), "receive should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "receive should throw 1 exception");
    }

    @Test
    @DisplayName("should have send method returning long")
    void shouldHaveSendMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.class.getMethod("send", WasiUdpSocket.OutgoingDatagram[].class);
      assertNotNull(method, "send method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(1, method.getParameterCount(), "send should have 1 parameter");
      assertEquals(
          WasiUdpSocket.OutgoingDatagram[].class,
          method.getParameterTypes()[0],
          "Parameter should be OutgoingDatagram[]");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "send should throw 1 exception");
    }
  }

  // ========================================================================
  // Lifecycle Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have subscribe method returning WasiPollable")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(
          WasiPollable.class, method.getReturnType(), "Return type should be WasiPollable");
      assertEquals(0, method.getParameterCount(), "subscribe should have no parameters");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "close should have no parameters");
    }
  }

  // ========================================================================
  // IncomingDatagram Nested Class Tests
  // ========================================================================

  @Nested
  @DisplayName("IncomingDatagram Nested Class Tests")
  class IncomingDatagramTests {

    @Test
    @DisplayName("IncomingDatagram should be a nested final class")
    void shouldBeNestedFinalClass() {
      Class<?> incomingDatagramClass = WasiUdpSocket.IncomingDatagram.class;
      assertTrue(
          Modifier.isFinal(incomingDatagramClass.getModifiers()),
          "IncomingDatagram should be final");
      assertTrue(
          Modifier.isPublic(incomingDatagramClass.getModifiers()),
          "IncomingDatagram should be public");
    }

    @Test
    @DisplayName("IncomingDatagram should have constructor with byte[] and IpSocketAddress")
    void shouldHaveConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          WasiUdpSocket.IncomingDatagram.class.getConstructor(byte[].class, IpSocketAddress.class);
      assertNotNull(constructor, "Constructor should exist");
      assertEquals(2, constructor.getParameterCount(), "Constructor should have 2 parameters");
    }

    @Test
    @DisplayName("IncomingDatagram should have getData method")
    void shouldHaveGetDataMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.IncomingDatagram.class.getMethod("getData");
      assertNotNull(method, "getData method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Return type should be byte[]");
    }

    @Test
    @DisplayName("IncomingDatagram should have getRemoteAddress method")
    void shouldHaveGetRemoteAddressMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.IncomingDatagram.class.getMethod("getRemoteAddress");
      assertNotNull(method, "getRemoteAddress method should exist");
      assertEquals(
          IpSocketAddress.class, method.getReturnType(), "Return type should be IpSocketAddress");
    }

    @Test
    @DisplayName("IncomingDatagram constructor should reject null data")
    void constructorShouldRejectNullData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiUdpSocket.IncomingDatagram(null, createMockIpSocketAddress()),
          "Should throw IllegalArgumentException for null data");
    }

    @Test
    @DisplayName("IncomingDatagram constructor should reject null remoteAddress")
    void constructorShouldRejectNullRemoteAddress() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiUdpSocket.IncomingDatagram(new byte[] {1, 2, 3}, null),
          "Should throw IllegalArgumentException for null remoteAddress");
    }

    @Test
    @DisplayName("IncomingDatagram getData should return defensive copy")
    void getDataShouldReturnDefensiveCopy() {
      byte[] originalData = new byte[] {1, 2, 3, 4, 5};
      WasiUdpSocket.IncomingDatagram datagram =
          new WasiUdpSocket.IncomingDatagram(originalData, createMockIpSocketAddress());

      byte[] retrievedData = datagram.getData();
      assertArrayEquals(originalData, retrievedData, "Data should match original");

      // Modify retrieved data
      retrievedData[0] = 99;

      // Original should be unchanged
      byte[] secondRetrieval = datagram.getData();
      assertEquals(1, secondRetrieval[0], "Original data should be unchanged");
    }
  }

  // ========================================================================
  // OutgoingDatagram Nested Class Tests
  // ========================================================================

  @Nested
  @DisplayName("OutgoingDatagram Nested Class Tests")
  class OutgoingDatagramTests {

    @Test
    @DisplayName("OutgoingDatagram should be a nested final class")
    void shouldBeNestedFinalClass() {
      Class<?> outgoingDatagramClass = WasiUdpSocket.OutgoingDatagram.class;
      assertTrue(
          Modifier.isFinal(outgoingDatagramClass.getModifiers()),
          "OutgoingDatagram should be final");
      assertTrue(
          Modifier.isPublic(outgoingDatagramClass.getModifiers()),
          "OutgoingDatagram should be public");
    }

    @Test
    @DisplayName("OutgoingDatagram should have constructor with byte[] and IpSocketAddress")
    void shouldHaveConstructorWithAddress() throws NoSuchMethodException {
      Constructor<?> constructor =
          WasiUdpSocket.OutgoingDatagram.class.getConstructor(byte[].class, IpSocketAddress.class);
      assertNotNull(constructor, "Constructor should exist");
      assertEquals(2, constructor.getParameterCount(), "Constructor should have 2 parameters");
    }

    @Test
    @DisplayName("OutgoingDatagram should have constructor with only byte[]")
    void shouldHaveConstructorWithoutAddress() throws NoSuchMethodException {
      Constructor<?> constructor =
          WasiUdpSocket.OutgoingDatagram.class.getConstructor(byte[].class);
      assertNotNull(constructor, "Constructor should exist");
      assertEquals(1, constructor.getParameterCount(), "Constructor should have 1 parameter");
    }

    @Test
    @DisplayName("OutgoingDatagram should have getData method")
    void shouldHaveGetDataMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.OutgoingDatagram.class.getMethod("getData");
      assertNotNull(method, "getData method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Return type should be byte[]");
    }

    @Test
    @DisplayName("OutgoingDatagram should have getRemoteAddress method")
    void shouldHaveGetRemoteAddressMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.OutgoingDatagram.class.getMethod("getRemoteAddress");
      assertNotNull(method, "getRemoteAddress method should exist");
      assertEquals(
          IpSocketAddress.class, method.getReturnType(), "Return type should be IpSocketAddress");
    }

    @Test
    @DisplayName("OutgoingDatagram should have hasRemoteAddress method")
    void shouldHaveHasRemoteAddressMethod() throws NoSuchMethodException {
      Method method = WasiUdpSocket.OutgoingDatagram.class.getMethod("hasRemoteAddress");
      assertNotNull(method, "hasRemoteAddress method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("OutgoingDatagram constructor should reject null data")
    void constructorShouldRejectNullData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiUdpSocket.OutgoingDatagram(null, createMockIpSocketAddress()),
          "Should throw IllegalArgumentException for null data");
    }

    @Test
    @DisplayName("OutgoingDatagram constructor without address should reject null data")
    void constructorWithoutAddressShouldRejectNullData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiUdpSocket.OutgoingDatagram(null),
          "Should throw IllegalArgumentException for null data");
    }

    @Test
    @DisplayName("OutgoingDatagram without address should have null remoteAddress")
    void withoutAddressShouldHaveNullRemoteAddress() {
      WasiUdpSocket.OutgoingDatagram datagram =
          new WasiUdpSocket.OutgoingDatagram(new byte[] {1, 2, 3});
      assertNull(datagram.getRemoteAddress(), "remoteAddress should be null");
      assertFalse(datagram.hasRemoteAddress(), "hasRemoteAddress should return false");
    }

    @Test
    @DisplayName("OutgoingDatagram with address should have non-null remoteAddress")
    void withAddressShouldHaveNonNullRemoteAddress() {
      IpSocketAddress address = createMockIpSocketAddress();
      WasiUdpSocket.OutgoingDatagram datagram =
          new WasiUdpSocket.OutgoingDatagram(new byte[] {1, 2, 3}, address);
      assertNotNull(datagram.getRemoteAddress(), "remoteAddress should not be null");
      assertTrue(datagram.hasRemoteAddress(), "hasRemoteAddress should return true");
    }

    @Test
    @DisplayName("OutgoingDatagram getData should return defensive copy")
    void getDataShouldReturnDefensiveCopy() {
      byte[] originalData = new byte[] {1, 2, 3, 4, 5};
      WasiUdpSocket.OutgoingDatagram datagram =
          new WasiUdpSocket.OutgoingDatagram(originalData, createMockIpSocketAddress());

      byte[] retrievedData = datagram.getData();
      assertArrayEquals(originalData, retrievedData, "Data should match original");

      // Modify retrieved data
      retrievedData[0] = 99;

      // Original should be unchanged
      byte[] secondRetrieval = datagram.getData();
      assertEquals(1, secondRetrieval[0], "Original data should be unchanged");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("WasiUdpSocket should have exactly 15 declared methods")
    void shouldHaveExactMethodCount() {
      Method[] methods = WasiUdpSocket.class.getDeclaredMethods();
      assertEquals(15, methods.length, "WasiUdpSocket should have exactly 15 methods");
    }
  }

  // ========================================================================
  // Helper Methods
  // ========================================================================

  private IpSocketAddress createMockIpSocketAddress() {
    // Create a real IpSocketAddress using the static factory method
    Ipv4Address ipv4Address = new Ipv4Address(new byte[] {127, 0, 0, 1});
    Ipv4SocketAddress ipv4SocketAddress = new Ipv4SocketAddress(8080, ipv4Address);
    return IpSocketAddress.ipv4(ipv4SocketAddress);
  }
}
