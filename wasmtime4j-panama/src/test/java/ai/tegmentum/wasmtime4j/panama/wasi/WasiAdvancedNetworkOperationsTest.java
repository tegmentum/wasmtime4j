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

package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiAdvancedNetworkOperations} class.
 *
 * <p>WasiAdvancedNetworkOperations provides Panama FFI implementation of WASI Preview 2 advanced
 * network operations including WebSocket, HTTP/2, and gRPC support.
 */
@DisplayName("WasiAdvancedNetworkOperations Tests")
class WasiAdvancedNetworkOperationsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiAdvancedNetworkOperations.class.getModifiers()),
          "WasiAdvancedNetworkOperations should be public");
      assertTrue(
          Modifier.isFinal(WasiAdvancedNetworkOperations.class.getModifiers()),
          "WasiAdvancedNetworkOperations should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with WasiContext and ExecutorService")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiAdvancedNetworkOperations.class.getConstructor(
              WasiContext.class, ExecutorService.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Protocol Constant Tests")
  class ProtocolConstantTests {

    @Test
    @DisplayName("should have PROTOCOL_WEBSOCKET constant")
    void shouldHaveWebSocketProtocolConstant() throws NoSuchFieldException {
      final Field field = WasiAdvancedNetworkOperations.class.getField("PROTOCOL_WEBSOCKET");
      assertNotNull(field, "PROTOCOL_WEBSOCKET should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int");
    }

    @Test
    @DisplayName("should have PROTOCOL_HTTP2 constant")
    void shouldHaveHttp2ProtocolConstant() throws NoSuchFieldException {
      final Field field = WasiAdvancedNetworkOperations.class.getField("PROTOCOL_HTTP2");
      assertNotNull(field, "PROTOCOL_HTTP2 should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("should have PROTOCOL_GRPC constant")
    void shouldHaveGrpcProtocolConstant() throws NoSuchFieldException {
      final Field field = WasiAdvancedNetworkOperations.class.getField("PROTOCOL_GRPC");
      assertNotNull(field, "PROTOCOL_GRPC should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }
  }

  @Nested
  @DisplayName("WebSocket Message Type Constant Tests")
  class WebSocketMessageTypeConstantTests {

    @Test
    @DisplayName("should have WS_MESSAGE_TEXT constant")
    void shouldHaveWsMessageTextConstant() throws NoSuchFieldException {
      final Field field = WasiAdvancedNetworkOperations.class.getField("WS_MESSAGE_TEXT");
      assertNotNull(field, "WS_MESSAGE_TEXT should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("should have WS_MESSAGE_BINARY constant")
    void shouldHaveWsMessageBinaryConstant() throws NoSuchFieldException {
      final Field field = WasiAdvancedNetworkOperations.class.getField("WS_MESSAGE_BINARY");
      assertNotNull(field, "WS_MESSAGE_BINARY should exist");
    }

    @Test
    @DisplayName("should have WS_MESSAGE_CLOSE constant")
    void shouldHaveWsMessageCloseConstant() throws NoSuchFieldException {
      final Field field = WasiAdvancedNetworkOperations.class.getField("WS_MESSAGE_CLOSE");
      assertNotNull(field, "WS_MESSAGE_CLOSE should exist");
    }
  }

  @Nested
  @DisplayName("Initialization Method Tests")
  class InitializationMethodTests {

    @Test
    @DisplayName("should have initialize method")
    void shouldHaveInitializeMethod() throws NoSuchMethodException {
      final Method method = WasiAdvancedNetworkOperations.class.getMethod("initialize");
      assertNotNull(method, "initialize method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("WebSocket Method Tests")
  class WebSocketMethodTests {

    @Test
    @DisplayName("should have websocketConnect method")
    void shouldHaveWebsocketConnectMethod() throws NoSuchMethodException {
      final Method method =
          WasiAdvancedNetworkOperations.class.getMethod(
              "websocketConnect", String.class, Map.class, long.class);
      assertNotNull(method, "websocketConnect method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have websocketSend method")
    void shouldHaveWebsocketSendMethod() throws NoSuchMethodException {
      final Method method =
          WasiAdvancedNetworkOperations.class.getMethod(
              "websocketSend", long.class, int.class, ByteBuffer.class);
      assertNotNull(method, "websocketSend method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have websocketReceive method")
    void shouldHaveWebsocketReceiveMethod() throws NoSuchMethodException {
      final Method method =
          WasiAdvancedNetworkOperations.class.getMethod(
              "websocketReceive", long.class, ByteBuffer.class, long.class);
      assertNotNull(method, "websocketReceive method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("HTTP/2 Method Tests")
  class Http2MethodTests {

    @Test
    @DisplayName("should have http2Connect method")
    void shouldHaveHttp2ConnectMethod() throws NoSuchMethodException {
      final Method method =
          WasiAdvancedNetworkOperations.class.getMethod(
              "http2Connect", String.class, int.class, boolean.class, long.class);
      assertNotNull(method, "http2Connect method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("gRPC Method Tests")
  class GrpcMethodTests {

    @Test
    @DisplayName("should have grpcConnect method")
    void shouldHaveGrpcConnectMethod() throws NoSuchMethodException {
      final Method method =
          WasiAdvancedNetworkOperations.class.getMethod(
              "grpcConnect", String.class, boolean.class, long.class);
      assertNotNull(method, "grpcConnect method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("Connection Management Method Tests")
  class ConnectionManagementMethodTests {

    @Test
    @DisplayName("should have closeConnection method")
    void shouldHaveCloseConnectionMethod() throws NoSuchMethodException {
      final Method method =
          WasiAdvancedNetworkOperations.class.getMethod("closeConnection", long.class);
      assertNotNull(method, "closeConnection method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have getActiveConnectionCount method")
    void shouldHaveGetActiveConnectionCountMethod() throws NoSuchMethodException {
      final Method method =
          WasiAdvancedNetworkOperations.class.getMethod("getActiveConnectionCount");
      assertNotNull(method, "getActiveConnectionCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Metrics Method Tests")
  class MetricsMethodTests {

    @Test
    @DisplayName("should have getMetrics method")
    void shouldHaveGetMetricsMethod() throws NoSuchMethodException {
      final Method method = WasiAdvancedNetworkOperations.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
      assertEquals(
          WasiAdvancedNetworkOperations.AdvancedNetworkMetrics.class,
          method.getReturnType(),
          "Should return AdvancedNetworkMetrics");
    }
  }

  @Nested
  @DisplayName("Cleanup Method Tests")
  class CleanupMethodTests {

    @Test
    @DisplayName("should have cleanup method")
    void shouldHaveCleanupMethod() throws NoSuchMethodException {
      final Method method = WasiAdvancedNetworkOperations.class.getMethod("cleanup");
      assertNotNull(method, "cleanup method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("AdvancedNetworkMetrics Inner Class Tests")
  class AdvancedNetworkMetricsTests {

    @Test
    @DisplayName("AdvancedNetworkMetrics should be public static final class")
    void advancedNetworkMetricsShouldBePublicStaticFinal() {
      Class<?> innerClass = WasiAdvancedNetworkOperations.AdvancedNetworkMetrics.class;
      assertTrue(Modifier.isPublic(innerClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(innerClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(innerClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("AdvancedNetworkMetrics should have getSuccessfulConnections method")
    void advancedNetworkMetricsShouldHaveGetSuccessfulConnectionsMethod()
        throws NoSuchMethodException {
      final Method method =
          WasiAdvancedNetworkOperations.AdvancedNetworkMetrics.class.getMethod(
              "getSuccessfulConnections", int.class);
      assertNotNull(method, "getSuccessfulConnections method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("AdvancedNetworkMetrics should have getErrors method")
    void advancedNetworkMetricsShouldHaveGetErrorsMethod() throws NoSuchMethodException {
      final Method method =
          WasiAdvancedNetworkOperations.AdvancedNetworkMetrics.class.getMethod(
              "getErrors", int.class);
      assertNotNull(method, "getErrors method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("AdvancedNetworkMetrics should have copy method")
    void advancedNetworkMetricsShouldHaveCopyMethod() throws NoSuchMethodException {
      final Method method =
          WasiAdvancedNetworkOperations.AdvancedNetworkMetrics.class.getMethod("copy");
      assertNotNull(method, "copy method should exist");
      assertEquals(
          WasiAdvancedNetworkOperations.AdvancedNetworkMetrics.class,
          method.getReturnType(),
          "Should return AdvancedNetworkMetrics");
    }
  }
}
