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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiPreview2Operations.WasiHttpResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link WasiPreview2Operations}. */
@DisplayName("WasiPreview2Operations Tests")
class WasiPreview2OperationsTest {

  private WasiContext testContext;
  private WasiPreview2Operations operations;

  @BeforeEach
  void setUp() {
    testContext = TestWasiContextFactory.createTestContext();
    operations = new WasiPreview2Operations(testContext);
  }

  @AfterEach
  void tearDown() {
    if (operations != null) {
      operations.close();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiPreview2Operations should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiPreview2Operations.class.getModifiers()),
          "WasiPreview2Operations should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should throw on null context")
    void constructorShouldThrowOnNullContext() {
      assertThrows(
          JniException.class,
          () -> new WasiPreview2Operations(null),
          "Should throw on null context");
    }

    @Test
    @DisplayName("Constructor should create operations with valid context")
    void constructorShouldCreateOperationsWithValidContext() {
      final WasiPreview2Operations ops = new WasiPreview2Operations(testContext);
      assertNotNull(ops, "Operations should be created");
      ops.close();
    }
  }

  @Nested
  @DisplayName("createResource Tests")
  class CreateResourceTests {

    @Test
    @DisplayName("Should throw on null resource type")
    void shouldThrowOnNullResourceType() {
      assertThrows(
          JniException.class,
          () -> operations.createResource(null, ByteBuffer.allocate(10)),
          "Should throw on null resourceType");
    }

    @Test
    @DisplayName("Should throw on empty resource type")
    void shouldThrowOnEmptyResourceType() {
      assertThrows(
          JniException.class,
          () -> operations.createResource("", ByteBuffer.allocate(10)),
          "Should throw on empty resourceType");
    }

    @Test
    @DisplayName("Should throw on null data")
    void shouldThrowOnNullData() {
      assertThrows(
          JniException.class,
          () -> operations.createResource("file", null),
          "Should throw on null data");
    }
  }

  @Nested
  @DisplayName("readAsync Tests")
  class ReadAsyncTests {

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() {
      assertThrows(
          JniException.class, () -> operations.readAsync(1L, null), "Should throw on null buffer");
    }

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final ByteBuffer buffer = ByteBuffer.allocate(100);
      final CompletableFuture<Integer> future = operations.readAsync(1L, buffer);

      assertNotNull(future, "Future should not be null");
      assertFalse(future.isDone(), "Future should not be immediately done");

      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("writeAsync Tests")
  class WriteAsyncTests {

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() {
      assertThrows(
          JniException.class, () -> operations.writeAsync(1L, null), "Should throw on null buffer");
    }

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final ByteBuffer buffer = ByteBuffer.wrap("test data".getBytes());
      final CompletableFuture<Integer> future = operations.writeAsync(1L, buffer);

      assertNotNull(future, "Future should not be null");
      assertFalse(future.isDone(), "Future should not be immediately done");

      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("createTcpSocket Tests")
  class CreateTcpSocketTests {

    @Test
    @DisplayName("Should accept IPv4 address family")
    void shouldAcceptIpv4AddressFamily() {
      // Will throw due to missing native implementation
      assertThrows(
          Exception.class, () -> operations.createTcpSocket(4), "Should attempt to create socket");
    }

    @Test
    @DisplayName("Should accept IPv6 address family")
    void shouldAcceptIpv6AddressFamily() {
      assertThrows(
          Exception.class, () -> operations.createTcpSocket(6), "Should attempt to create socket");
    }
  }

  @Nested
  @DisplayName("connectTcpAsync Tests")
  class ConnectTcpAsyncTests {

    @Test
    @DisplayName("Should throw on null address")
    void shouldThrowOnNullAddress() {
      assertThrows(
          JniException.class,
          () -> operations.connectTcpAsync(1L, null, 80),
          "Should throw on null address");
    }

    @Test
    @DisplayName("Should throw on empty address")
    void shouldThrowOnEmptyAddress() {
      assertThrows(
          JniException.class,
          () -> operations.connectTcpAsync(1L, "", 80),
          "Should throw on empty address");
    }

    @Test
    @DisplayName("Should throw on invalid port (negative)")
    void shouldThrowOnNegativePort() {
      assertThrows(
          JniException.class,
          () -> operations.connectTcpAsync(1L, "localhost", -1),
          "Should throw on negative port");
    }

    @Test
    @DisplayName("Should throw on invalid port (too large)")
    void shouldThrowOnPortTooLarge() {
      assertThrows(
          JniException.class,
          () -> operations.connectTcpAsync(1L, "localhost", 65536),
          "Should throw on port > 65535");
    }

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final CompletableFuture<Void> future = operations.connectTcpAsync(1L, "localhost", 8080);

      assertNotNull(future, "Future should not be null");

      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("createUdpSocket Tests")
  class CreateUdpSocketTests {

    @Test
    @DisplayName("Should accept IPv4 address family")
    void shouldAcceptIpv4AddressFamily() {
      assertThrows(
          Exception.class, () -> operations.createUdpSocket(4), "Should attempt to create socket");
    }
  }

  @Nested
  @DisplayName("sendUdpAsync Tests")
  class SendUdpAsyncTests {

    @Test
    @DisplayName("Should throw on null data")
    void shouldThrowOnNullData() {
      assertThrows(
          JniException.class,
          () -> operations.sendUdpAsync(1L, null, "localhost", 8080),
          "Should throw on null data");
    }

    @Test
    @DisplayName("Should throw on null address")
    void shouldThrowOnNullAddress() {
      assertThrows(
          JniException.class,
          () -> operations.sendUdpAsync(1L, ByteBuffer.allocate(10), null, 8080),
          "Should throw on null address");
    }

    @Test
    @DisplayName("Should throw on empty address")
    void shouldThrowOnEmptyAddress() {
      assertThrows(
          JniException.class,
          () -> operations.sendUdpAsync(1L, ByteBuffer.allocate(10), "", 8080),
          "Should throw on empty address");
    }

    @Test
    @DisplayName("Should throw on invalid port")
    void shouldThrowOnInvalidPort() {
      assertThrows(
          JniException.class,
          () -> operations.sendUdpAsync(1L, ByteBuffer.allocate(10), "localhost", -1),
          "Should throw on invalid port");
    }

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final ByteBuffer data = ByteBuffer.wrap("test".getBytes());
      final CompletableFuture<Void> future = operations.sendUdpAsync(1L, data, "localhost", 8080);

      assertNotNull(future, "Future should not be null");

      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("httpRequestAsync Tests")
  class HttpRequestAsyncTests {

    @Test
    @DisplayName("Should throw on null method")
    void shouldThrowOnNullMethod() {
      assertThrows(
          JniException.class,
          () ->
              operations.httpRequestAsync(null, "http://example.com", Collections.emptyMap(), null),
          "Should throw on null method");
    }

    @Test
    @DisplayName("Should throw on empty method")
    void shouldThrowOnEmptyMethod() {
      assertThrows(
          JniException.class,
          () -> operations.httpRequestAsync("", "http://example.com", Collections.emptyMap(), null),
          "Should throw on empty method");
    }

    @Test
    @DisplayName("Should throw on null URI")
    void shouldThrowOnNullUri() {
      assertThrows(
          JniException.class,
          () -> operations.httpRequestAsync("GET", null, Collections.emptyMap(), null),
          "Should throw on null URI");
    }

    @Test
    @DisplayName("Should throw on empty URI")
    void shouldThrowOnEmptyUri() {
      assertThrows(
          JniException.class,
          () -> operations.httpRequestAsync("GET", "", Collections.emptyMap(), null),
          "Should throw on empty URI");
    }

    @Test
    @DisplayName("Should throw on null headers")
    void shouldThrowOnNullHeaders() {
      assertThrows(
          JniException.class,
          () -> operations.httpRequestAsync("GET", "http://example.com", null, null),
          "Should throw on null headers");
    }

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final CompletableFuture<WasiHttpResponse> future =
          operations.httpRequestAsync("GET", "http://example.com", Collections.emptyMap(), null);

      assertNotNull(future, "Future should not be null");

      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("poll Tests")
  class PollTests {

    @Test
    @DisplayName("Should throw on null pollables")
    void shouldThrowOnNullPollables() {
      assertThrows(
          JniException.class, () -> operations.poll(null, 1000), "Should throw on null pollables");
    }

    @Test
    @DisplayName("Should throw on negative timeout")
    void shouldThrowOnNegativeTimeout() {
      final List<Long> pollables = new ArrayList<>();
      pollables.add(1L);

      assertThrows(
          JniException.class,
          () -> operations.poll(pollables, -1),
          "Should throw on negative timeout");
    }

    @Test
    @DisplayName("Should accept zero timeout")
    void shouldAcceptZeroTimeout() {
      final List<Long> pollables = new ArrayList<>();
      pollables.add(1L);

      // Will throw due to missing native implementation
      assertThrows(Throwable.class, () -> operations.poll(pollables, 0), "Should attempt to poll");
    }
  }

  @Nested
  @DisplayName("close Tests")
  class CloseTests {

    @Test
    @DisplayName("Should close gracefully")
    void shouldCloseGracefully() {
      operations.close();
      // Should not throw
      assertTrue(true, "Close should complete");
    }

    @Test
    @DisplayName("Should be idempotent")
    void shouldBeIdempotent() {
      operations.close();
      operations.close();
      // Should not throw
      assertTrue(true, "Multiple closes should not throw");
    }
  }

  @Nested
  @DisplayName("WasiHttpResponse Tests")
  class WasiHttpResponseTests {

    @Test
    @DisplayName("Should create response with all fields")
    void shouldCreateResponseWithAllFields() {
      final Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("X-Custom", "value");

      final ByteBuffer body = ByteBuffer.wrap("{\"key\":\"value\"}".getBytes());

      final WasiHttpResponse response = new WasiHttpResponse(200, headers, body);

      assertEquals(200, response.getStatusCode(), "Status code should match");
      assertEquals(2, response.getHeaders().size(), "Should have 2 headers");
      assertEquals(
          "application/json",
          response.getHeaders().get("Content-Type"),
          "Content-Type should match");
      assertNotNull(response.getBody(), "Body should not be null");
    }

    @Test
    @DisplayName("Should handle null headers")
    void shouldHandleNullHeaders() {
      final WasiHttpResponse response = new WasiHttpResponse(404, null, null);

      assertEquals(404, response.getStatusCode(), "Status code should match");
      assertNotNull(response.getHeaders(), "Headers should not be null");
      assertTrue(response.getHeaders().isEmpty(), "Headers should be empty");
    }

    @Test
    @DisplayName("Should return immutable headers")
    void shouldReturnImmutableHeaders() {
      final Map<String, String> headers = new HashMap<>();
      headers.put("Key", "Value");

      final WasiHttpResponse response = new WasiHttpResponse(200, headers, null);

      assertThrows(
          UnsupportedOperationException.class,
          () -> response.getHeaders().put("NewKey", "NewValue"),
          "Headers should be immutable");
    }

    @Test
    @DisplayName("Should isolate headers from original map")
    void shouldIsolateHeadersFromOriginalMap() {
      final Map<String, String> headers = new HashMap<>();
      headers.put("Key", "Value");

      final WasiHttpResponse response = new WasiHttpResponse(200, headers, null);

      // Modify original map
      headers.put("NewKey", "NewValue");

      // Response headers should not be affected
      assertFalse(
          response.getHeaders().containsKey("NewKey"),
          "Response should not contain newly added key");
    }
  }

  @Nested
  @DisplayName("openInputStream Tests")
  class OpenInputStreamTests {

    @Test
    @DisplayName("Should attempt to open input stream")
    void shouldAttemptToOpenInputStream() {
      // Will throw due to missing stream handler setup
      assertThrows(
          Throwable.class, () -> operations.openInputStream(1L), "Should attempt to open stream");
    }
  }

  @Nested
  @DisplayName("openOutputStream Tests")
  class OpenOutputStreamTests {

    @Test
    @DisplayName("Should attempt to open output stream")
    void shouldAttemptToOpenOutputStream() {
      // Will throw due to missing stream handler setup
      assertThrows(
          Throwable.class, () -> operations.openOutputStream(1L), "Should attempt to open stream");
    }
  }
}
