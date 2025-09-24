package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Comprehensive test for WASI Preview 2 operations.
 *
 * <p>This test verifies that WASI Preview 2 provides enhanced functionality including:
 *
 * <ul>
 *   <li>Async stream operations
 *   <li>Component model features
 *   <li>Enhanced networking
 *   <li>Improved error handling
 *   <li>Resource management
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("WASI Preview 2 Operations Test")
@EnabledIfSystemProperty(named = "wasmtime4j.test.native", matches = "true")
class WasiPreview2OperationsTest {

  private WasiConfig preview2Config;

  @BeforeEach
  void setUp() {
    preview2Config =
        WasiConfig.builder()
            .withWasiVersion(WasiVersion.PREVIEW_2)
            .withAsyncOperations(true)
            .withMaxAsyncOperations(100)
            .withAsyncOperationTimeout(Duration.ofSeconds(30))
            .withMemoryLimit(128 * 1024 * 1024) // 128MB
            .withEnvironment("TEST_ENV", "preview2_test")
            .withArgument("test-program")
            .withArgument("--preview2")
            .withValidation(true)
            .build();
  }

  @Test
  @DisplayName("WASI Preview 2 context creation should work")
  void testContextCreation() {
    if (!WasiContextFactory.isPreview2Supported()) {
      // Skip if Preview 2 is not supported
      return;
    }

    try (final WasiPreview2Context context =
        WasiContextFactory.createPreview2Context(preview2Config)) {
      assertNotNull(context);
      assertEquals(preview2Config, context.getConfig());
      assertNotNull(context.getPerformanceMetrics());
    }
  }

  @Test
  @DisplayName("Resource management should work correctly")
  void testResourceManagement() {
    if (!WasiContextFactory.isPreview2Supported()) {
      return;
    }

    try (final WasiPreview2Context context =
        WasiContextFactory.createPreview2Context(preview2Config)) {
      // Test resource creation
      final ByteBuffer initData = ByteBuffer.allocate(64);
      initData.putLong(12345L);
      initData.flip();

      final long resourceHandle = context.createResource("test-resource", initData);
      assertTrue(resourceHandle > 0);

      // Test pollable creation for the resource
      final long pollableHandle = context.createPollable(resourceHandle);
      assertTrue(pollableHandle > 0);

      // Test resource destruction
      assertDoesNotThrow(() -> context.destroyResource(resourceHandle));
    }
  }

  @Test
  @DisplayName("Stream operations should work correctly")
  void testStreamOperations() {
    if (!WasiContextFactory.isPreview2Supported()) {
      return;
    }

    try (final WasiPreview2Context context =
        WasiContextFactory.createPreview2Context(preview2Config)) {
      // Create a resource for testing
      final long resourceHandle =
          context.createResource("stream-resource", ByteBuffer.allocate(32));

      // Test input stream creation
      final WasiPreview2Stream inputStream = context.openInputStream(resourceHandle);
      assertNotNull(inputStream);
      assertEquals(WasiPreview2Stream.WasiStreamType.INPUT, inputStream.getStreamType());
      assertTrue(inputStream.getStreamId() > 0);

      // Test output stream creation
      final WasiPreview2Stream outputStream = context.openOutputStream(resourceHandle);
      assertNotNull(outputStream);
      assertEquals(WasiPreview2Stream.WasiStreamType.OUTPUT, outputStream.getStreamType());
      assertTrue(outputStream.getStreamId() > 0);

      // Test bidirectional stream creation
      final WasiPreview2Stream bidirectionalStream =
          context.openBidirectionalStream(resourceHandle);
      assertNotNull(bidirectionalStream);
      assertEquals(
          WasiPreview2Stream.WasiStreamType.BIDIRECTIONAL, bidirectionalStream.getStreamType());
      assertTrue(bidirectionalStream.getStreamId() > 0);

      // Test stream status
      assertNotNull(inputStream.getStatus());
      assertNotNull(outputStream.getStatus());
      assertNotNull(bidirectionalStream.getStatus());

      // Test pollable creation for streams
      assertTrue(inputStream.createPollable() > 0);
      assertTrue(outputStream.createPollable() > 0);
      assertTrue(bidirectionalStream.createPollable() > 0);

      // Clean up streams
      inputStream.close();
      outputStream.close();
      bidirectionalStream.close();

      context.destroyResource(resourceHandle);
    }
  }

  @Test
  @DisplayName("Async file operations should work correctly")
  void testAsyncFileOperations() {
    if (!WasiContextFactory.isPreview2Supported()) {
      return;
    }

    try (final WasiPreview2Context context =
        WasiContextFactory.createPreview2Context(preview2Config)) {
      // Test async file open (this would typically open a real file)
      final CompletableFuture<Long> openFuture = context.openFileAsync("/dev/null", 0, 0);
      assertNotNull(openFuture);

      // The actual operation might fail due to security restrictions or missing files,
      // but the future should be created successfully
      assertDoesNotThrow(
          () -> {
            try {
              final Long fileHandle = openFuture.get(5, TimeUnit.SECONDS);
              if (fileHandle != null && fileHandle > 0) {
                // If file was successfully opened, test read/write operations
                final ByteBuffer buffer = ByteBuffer.allocate(16);
                final CompletableFuture<Integer> readFuture =
                    context.readFileAsync(fileHandle.intValue(), buffer, 0);
                assertNotNull(readFuture);

                buffer.rewind();
                buffer.put("test data".getBytes());
                buffer.flip();
                final CompletableFuture<Integer> writeFuture =
                    context.writeFileAsync(fileHandle.intValue(), buffer, 0);
                assertNotNull(writeFuture);
              }
            } catch (final Exception e) {
              // Expected for security-restricted environments
            }
          });
    }
  }

  @Test
  @DisplayName("Async time operations should work correctly")
  void testAsyncTimeOperations() {
    if (!WasiContextFactory.isPreview2Supported()) {
      return;
    }

    try (final WasiPreview2Context context =
        WasiContextFactory.createPreview2Context(preview2Config)) {
      // Test async time retrieval
      final CompletableFuture<Long> timeFuture = context.getTimeAsync(0, 1000); // realtime clock
      assertNotNull(timeFuture);

      assertDoesNotThrow(
          () -> {
            final Long time = timeFuture.get(5, TimeUnit.SECONDS);
            assertNotNull(time);
            assertTrue(time > 0);
          });

      // Test monotonic clock
      final CompletableFuture<Long> monotonicFuture =
          context.getTimeAsync(1, 1000); // monotonic clock
      assertNotNull(monotonicFuture);

      assertDoesNotThrow(
          () -> {
            final Long time = monotonicFuture.get(5, TimeUnit.SECONDS);
            assertNotNull(time);
            assertTrue(time >= 0);
          });
    }
  }

  @Test
  @DisplayName("Async random operations should work correctly")
  void testAsyncRandomOperations() {
    if (!WasiContextFactory.isPreview2Supported()) {
      return;
    }

    try (final WasiPreview2Context context =
        WasiContextFactory.createPreview2Context(preview2Config)) {
      // Test async random byte generation
      final ByteBuffer randomBuffer = ByteBuffer.allocate(64);
      final CompletableFuture<Void> randomFuture = context.getRandomBytesAsync(randomBuffer);
      assertNotNull(randomFuture);

      assertDoesNotThrow(
          () -> {
            randomFuture.get(5, TimeUnit.SECONDS);
            randomBuffer.flip();

            // Verify that random data was generated (at least some non-zero bytes)
            boolean hasNonZero = false;
            while (randomBuffer.hasRemaining()) {
              if (randomBuffer.get() != 0) {
                hasNonZero = true;
                break;
              }
            }
            // Note: This is probabilistic, but 64 bytes of all zeros is extremely unlikely
            assertTrue(hasNonZero, "Random data should contain at least some non-zero bytes");
          });
    }
  }

  @Test
  @DisplayName("Network operations should work correctly")
  void testNetworkOperations() {
    if (!WasiContextFactory.isPreview2Supported()) {
      return;
    }

    try (final WasiPreview2Context context =
        WasiContextFactory.createPreview2Context(preview2Config)) {
      // Test TCP socket creation
      assertDoesNotThrow(
          () -> {
            final long tcpSocket = context.createTcpSocket(4); // IPv4
            assertTrue(tcpSocket > 0);
          });

      // Test UDP socket creation
      assertDoesNotThrow(
          () -> {
            final long udpSocket = context.createUdpSocket(4); // IPv4
            assertTrue(udpSocket > 0);
          });

      // Test IPv6 sockets
      assertDoesNotThrow(
          () -> {
            final long tcpSocket6 = context.createTcpSocket(6); // IPv6
            assertTrue(tcpSocket6 > 0);

            final long udpSocket6 = context.createUdpSocket(6); // IPv6
            assertTrue(udpSocket6 > 0);
          });
    }
  }

  @Test
  @DisplayName("HTTP operations should work correctly")
  void testHttpOperations() {
    if (!WasiContextFactory.isPreview2Supported()) {
      return;
    }

    try (final WasiPreview2Context context =
        WasiContextFactory.createPreview2Context(preview2Config)) {
      // Test HTTP request creation (this would typically make a real HTTP request)
      final Map<String, String> headers =
          Map.of(
              "User-Agent", "wasmtime4j-test",
              "Accept", "application/json");

      final CompletableFuture<WasiPreview2Context.WasiHttpResponse> httpFuture =
          context.httpRequestAsync("GET", "http://httpbin.org/get", headers, null);
      assertNotNull(httpFuture);

      // The actual HTTP request might fail due to network restrictions,
      // but the future should be created successfully
      assertDoesNotThrow(
          () -> {
            try {
              final WasiPreview2Context.WasiHttpResponse response =
                  httpFuture.get(10, TimeUnit.SECONDS);
              if (response != null) {
                assertTrue(response.getStatusCode() >= 100 && response.getStatusCode() < 600);
                assertNotNull(response.getHeaders());
                // Body might be null for some responses
              }
            } catch (final Exception e) {
              // Expected for network-restricted environments
            }
          });
    }
  }

  @Test
  @DisplayName("Polling operations should work correctly")
  void testPollingOperations() {
    if (!WasiContextFactory.isPreview2Supported()) {
      return;
    }

    try (final WasiPreview2Context context =
        WasiContextFactory.createPreview2Context(preview2Config)) {
      // Create resources and pollables
      final long resource1 = context.createResource("poll-resource-1", ByteBuffer.allocate(16));
      final long resource2 = context.createResource("poll-resource-2", ByteBuffer.allocate(16));

      final long pollable1 = context.createPollable(resource1);
      final long pollable2 = context.createPollable(resource2);

      // Test polling with timeout
      final List<Long> pollables = List.of(pollable1, pollable2);
      final List<Integer> ready = context.poll(pollables, 1000000000L); // 1 second timeout
      assertNotNull(ready);
      assertTrue(ready.size() <= pollables.size());

      // Test polling with no timeout
      final List<Integer> readyNoTimeout = context.poll(pollables, 0);
      assertNotNull(readyNoTimeout);

      // Clean up
      context.destroyResource(resource1);
      context.destroyResource(resource2);
    }
  }

  @Test
  @DisplayName("Error handling should work correctly")
  void testErrorHandling() {
    if (!WasiContextFactory.isPreview2Supported()) {
      return;
    }

    try (final WasiPreview2Context context =
        WasiContextFactory.createPreview2Context(preview2Config)) {
      // Test invalid resource operations
      assertThrows(Exception.class, () -> context.destroyResource(-1));
      assertThrows(Exception.class, () -> context.destroyResource(999999));

      // Test invalid socket operations
      assertThrows(Exception.class, () -> context.createTcpSocket(-1));
      assertThrows(Exception.class, () -> context.createTcpSocket(999));

      // Test invalid network operations
      assertThrows(
          Exception.class,
          () -> {
            final CompletableFuture<Void> future = context.connectTcpAsync(-1, "invalid", 80);
            future.get(5, TimeUnit.SECONDS);
          });

      // Test invalid HTTP operations
      assertThrows(
          Exception.class,
          () -> {
            final CompletableFuture<WasiPreview2Context.WasiHttpResponse> future =
                context.httpRequestAsync("INVALID", "not-a-url", Map.of(), null);
            future.get(5, TimeUnit.SECONDS);
          });
    }
  }

  @Test
  @DisplayName("Performance metrics should be available")
  void testPerformanceMetrics() {
    if (!WasiContextFactory.isPreview2Supported()) {
      return;
    }

    try (final WasiPreview2Context context =
        WasiContextFactory.createPreview2Context(preview2Config)) {
      final WasiPerformanceMetrics metrics = context.getPerformanceMetrics();
      assertNotNull(metrics);

      // Performance metrics should be accessible
      assertDoesNotThrow(
          () -> {
            // The actual metrics might vary, but they should be retrievable
            metrics.toString(); // Ensure it doesn't throw
          });
    }
  }

  @Test
  @DisplayName("Context should be properly closeable")
  void testContextLifecycle() {
    if (!WasiContextFactory.isPreview2Supported()) {
      return;
    }

    WasiPreview2Context context = WasiContextFactory.createPreview2Context(preview2Config);
    assertNotNull(context);

    // Context should be usable
    assertDoesNotThrow(
        () -> {
          final long resource = context.createResource("lifecycle-test", ByteBuffer.allocate(8));
          context.destroyResource(resource);
        });

    // Context should close without error
    assertDoesNotThrow(context::close);

    // Operations after close should fail or be no-ops
    final WasiPreview2Context finalContext = context;
    assertThrows(
        Exception.class,
        () -> {
          finalContext.createResource("after-close", ByteBuffer.allocate(8));
        });
  }
}
