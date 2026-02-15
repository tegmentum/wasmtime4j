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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiPreview2Context interface.
 *
 * <p>WasiPreview2Context provides access to WASI Preview 2 functionality including async I/O
 * operations, component model features, and WIT interface support.
 */
@DisplayName("WasiPreview2Context Interface Tests")
class WasiPreview2ContextTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiPreview2Context.class.isInterface(), "WasiPreview2Context should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiPreview2Context.class.getModifiers()),
          "WasiPreview2Context should be public");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiPreview2Context.class),
          "WasiPreview2Context should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have exactly 1 interface")
    void shouldHaveExactlyOneInterface() {
      assertEquals(
          1,
          WasiPreview2Context.class.getInterfaces().length,
          "WasiPreview2Context should extend 1 interface");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static create factory method")
    void shouldHaveStaticCreateMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("create", WasiConfig.class);
      assertNotNull(method, "create method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(
          WasiPreview2Context.class, method.getReturnType(), "Should return WasiPreview2Context");
    }
  }

  // ========================================================================
  // Resource Management Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Resource Management Method Tests")
  class ResourceManagementMethodTests {

    @Test
    @DisplayName("should have createResource method")
    void shouldHaveCreateResourceMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod("createResource", String.class, ByteBuffer.class);
      assertNotNull(method, "createResource method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have destroyResource method")
    void shouldHaveDestroyResourceMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("destroyResource", long.class);
      assertNotNull(method, "destroyResource method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Stream Operation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Stream Operation Method Tests")
  class StreamOperationMethodTests {

    @Test
    @DisplayName("should have openInputStream method")
    void shouldHaveOpenInputStreamMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("openInputStream", long.class);
      assertNotNull(method, "openInputStream method should exist");
      assertEquals(
          WasiPreview2Stream.class, method.getReturnType(), "Should return WasiPreview2Stream");
    }

    @Test
    @DisplayName("should have openOutputStream method")
    void shouldHaveOpenOutputStreamMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("openOutputStream", long.class);
      assertNotNull(method, "openOutputStream method should exist");
      assertEquals(
          WasiPreview2Stream.class, method.getReturnType(), "Should return WasiPreview2Stream");
    }

    @Test
    @DisplayName("should have openBidirectionalStream method")
    void shouldHaveOpenBidirectionalStreamMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("openBidirectionalStream", long.class);
      assertNotNull(method, "openBidirectionalStream method should exist");
      assertEquals(
          WasiPreview2Stream.class, method.getReturnType(), "Should return WasiPreview2Stream");
    }
  }

  // ========================================================================
  // Network Operation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Network Operation Method Tests")
  class NetworkOperationMethodTests {

    @Test
    @DisplayName("should have createTcpSocket method")
    void shouldHaveCreateTcpSocketMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("createTcpSocket", int.class);
      assertNotNull(method, "createTcpSocket method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have connectTcpAsync method")
    void shouldHaveConnectTcpAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod(
              "connectTcpAsync", long.class, String.class, int.class);
      assertNotNull(method, "connectTcpAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have createUdpSocket method")
    void shouldHaveCreateUdpSocketMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("createUdpSocket", int.class);
      assertNotNull(method, "createUdpSocket method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have sendUdpAsync method")
    void shouldHaveSendUdpAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod(
              "sendUdpAsync", long.class, ByteBuffer.class, String.class, int.class);
      assertNotNull(method, "sendUdpAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  // ========================================================================
  // HTTP Operation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("HTTP Operation Method Tests")
  class HttpOperationMethodTests {

    @Test
    @DisplayName("should have httpRequestAsync method")
    void shouldHaveHttpRequestAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod(
              "httpRequestAsync", String.class, String.class, Map.class, ByteBuffer.class);
      assertNotNull(method, "httpRequestAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  // ========================================================================
  // Filesystem Operation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Filesystem Operation Method Tests")
  class FilesystemOperationMethodTests {

    @Test
    @DisplayName("should have openFileAsync method")
    void shouldHaveOpenFileAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod("openFileAsync", String.class, int.class, long.class);
      assertNotNull(method, "openFileAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have readFileAsync method")
    void shouldHaveReadFileAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod(
              "readFileAsync", int.class, ByteBuffer.class, long.class);
      assertNotNull(method, "readFileAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have writeFileAsync method")
    void shouldHaveWriteFileAsyncMethod() throws NoSuchMethodException {
      Method method =
          WasiPreview2Context.class.getMethod(
              "writeFileAsync", int.class, ByteBuffer.class, long.class);
      assertNotNull(method, "writeFileAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  // ========================================================================
  // Clock and Random Operation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Clock and Random Operation Method Tests")
  class ClockAndRandomOperationMethodTests {

    @Test
    @DisplayName("should have getTimeAsync method")
    void shouldHaveGetTimeAsyncMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("getTimeAsync", int.class, long.class);
      assertNotNull(method, "getTimeAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have getRandomBytesAsync method")
    void shouldHaveGetRandomBytesAsyncMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("getRandomBytesAsync", ByteBuffer.class);
      assertNotNull(method, "getRandomBytesAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  // ========================================================================
  // Polling and Event Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Polling and Event Method Tests")
  class PollingAndEventMethodTests {

    @Test
    @DisplayName("should have poll method")
    void shouldHavePollMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("poll", List.class, long.class);
      assertNotNull(method, "poll method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have createPollable method")
    void shouldHaveCreatePollableMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("createPollable", long.class);
      assertNotNull(method, "createPollable method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  // ========================================================================
  // Configuration and Status Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Configuration and Status Method Tests")
  class ConfigurationAndStatusMethodTests {

    @Test
    @DisplayName("should have getConfig method")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(WasiConfig.class, method.getReturnType(), "Should return WasiConfig");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // WasiHttpResponse Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiHttpResponse Nested Interface Tests")
  class WasiHttpResponseNestedInterfaceTests {

    @Test
    @DisplayName("should be a nested interface")
    void shouldBeNestedInterface() {
      assertTrue(
          WasiPreview2Context.WasiHttpResponse.class.isInterface(),
          "WasiHttpResponse should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiPreview2Context.WasiHttpResponse.class.getModifiers()),
          "WasiHttpResponse should be public");
    }

    @Test
    @DisplayName("should have getStatusCode method")
    void shouldHaveGetStatusCodeMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.WasiHttpResponse.class.getMethod("getStatusCode");
      assertNotNull(method, "getStatusCode method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getHeaders method")
    void shouldHaveGetHeadersMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.WasiHttpResponse.class.getMethod("getHeaders");
      assertNotNull(method, "getHeaders method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getBody method")
    void shouldHaveGetBodyMethod() throws NoSuchMethodException {
      Method method = WasiPreview2Context.WasiHttpResponse.class.getMethod("getBody");
      assertNotNull(method, "getBody method should exist");
      assertEquals(ByteBuffer.class, method.getReturnType(), "Should return ByteBuffer");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have WasiHttpResponse nested interface")
    void shouldHaveWasiHttpResponseNestedInterface() {
      Set<String> nestedClassNames =
          Arrays.stream(WasiPreview2Context.class.getDeclaredClasses())
              .map(Class::getSimpleName)
              .collect(Collectors.toSet());

      assertTrue(nestedClassNames.contains("WasiHttpResponse"), "Should have WasiHttpResponse");
    }

    @Test
    @DisplayName("should have exactly 1 nested type")
    void shouldHaveExactly1NestedType() {
      assertEquals(
          1,
          WasiPreview2Context.class.getDeclaredClasses().length,
          "Should have 1 nested type (WasiHttpResponse)");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have at least 17 abstract methods")
    void shouldHaveExpectedMethodCount() {
      long abstractMethodCount =
          Arrays.stream(WasiPreview2Context.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertTrue(abstractMethodCount >= 17, "Should have at least 17 abstract methods");
    }
  }
}
