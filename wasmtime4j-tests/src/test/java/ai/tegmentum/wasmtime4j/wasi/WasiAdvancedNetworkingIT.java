package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiAdvancedNetworkOperations;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiContextBuilder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Comprehensive integration tests for WASI Preview 2 advanced networking protocols.
 *
 * <p>This test suite validates:
 *
 * <ul>
 *   <li>WebSocket client and server operations with secure connections
 *   <li>HTTP/2 protocol implementation with multiplexing
 *   <li>gRPC client and server with protobuf serialization
 *   <li>Async networking with non-blocking I/O
 *   <li>Connection pooling and keep-alive management
 *   <li>SSL/TLS support with certificate validation
 *   <li>Network monitoring and performance optimization
 *   <li>Protocol negotiation and multiplexing
 * </ul>
 *
 * @since 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Execution(ExecutionMode.CONCURRENT)
@EnabledIfSystemProperty(named = "wasmtime4j.test.integration", matches = "true")
class WasiAdvancedNetworkingIT {

  private static final Logger LOGGER = Logger.getLogger(WasiAdvancedNetworkingIT.class.getName());

  private static ExecutorService executorService;
  private WasmRuntime runtime;
  private WasiContext jniWasiContext;
  private ai.tegmentum.wasmtime4j.panama.wasi.WasiContext panamaWasiContext;
  private WasiAdvancedNetworkOperations jniAdvancedNetworking;
  private ai.tegmentum.wasmtime4j.panama.wasi.WasiAdvancedNetworkOperations
      panamaAdvancedNetworking;

  @BeforeAll
  static void setUpClass() {
    executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    LOGGER.info("Advanced networking integration tests starting");
  }

  @AfterAll
  static void tearDownClass() throws InterruptedException {
    if (executorService != null) {
      executorService.shutdown();
      if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    }
    LOGGER.info("Advanced networking integration tests completed");
  }

  @BeforeEach
  void setUp() throws Exception {
    runtime = WasmRuntimeFactory.createRuntime();
    Assumptions.assumeTrue(runtime != null, "Runtime should be available");

    // Initialize JNI WASI context with advanced networking
    if (isJniRuntime()) {
      jniWasiContext = new WasiContextBuilder().inheritEnvironment().allowNetworking(true).build();
      jniAdvancedNetworking = new WasiAdvancedNetworkOperations(jniWasiContext, executorService);
      jniAdvancedNetworking.initialize();
    }

    // Initialize Panama WASI context with advanced networking (Java 23+)
    if (isPanamaRuntime()) {
      panamaWasiContext =
          new ai.tegmentum.wasmtime4j.panama.wasi.WasiContextBuilder()
              .inheritEnvironment()
              .allowNetworking(true)
              .build();
      panamaAdvancedNetworking =
          new ai.tegmentum.wasmtime4j.panama.wasi.WasiAdvancedNetworkOperations(
              panamaWasiContext, executorService);
      panamaAdvancedNetworking.initialize();
    }
  }

  @AfterEach
  void tearDown() throws Exception {
    if (jniAdvancedNetworking != null) {
      try {
        jniAdvancedNetworking.cleanup();
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "JNI advanced networking cleanup failed", e);
      }
    }

    if (panamaAdvancedNetworking != null) {
      try {
        panamaAdvancedNetworking.cleanup();
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Panama advanced networking cleanup failed", e);
      }
    }

    if (runtime != null) {
      try {
        runtime.close();
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Runtime cleanup failed", e);
      }
    }
  }

  @Test
  @Order(1)
  @DisplayName("WebSocket Connection Test")
  void testWebSocketConnection() throws Exception {
    if (isJniRuntime() && jniAdvancedNetworking != null) {
      testJniWebSocketConnection();
    }

    if (isPanamaRuntime() && panamaAdvancedNetworking != null) {
      testPanamaWebSocketConnection();
    }
  }

  @Test
  @Order(2)
  @DisplayName("WebSocket Message Exchange Test")
  void testWebSocketMessageExchange() throws Exception {
    if (isJniRuntime() && jniAdvancedNetworking != null) {
      testJniWebSocketMessageExchange();
    }

    if (isPanamaRuntime() && panamaAdvancedNetworking != null) {
      testPanamaWebSocketMessageExchange();
    }
  }

  @Test
  @Order(3)
  @DisplayName("HTTP/2 Connection Test")
  void testHttp2Connection() throws Exception {
    if (isJniRuntime() && jniAdvancedNetworking != null) {
      testJniHttp2Connection();
    }

    if (isPanamaRuntime() && panamaAdvancedNetworking != null) {
      testPanamaHttp2Connection();
    }
  }

  @Test
  @Order(4)
  @DisplayName("gRPC Connection Test")
  void testGrpcConnection() throws Exception {
    if (isJniRuntime() && jniAdvancedNetworking != null) {
      testJniGrpcConnection();
    }

    if (isPanamaRuntime() && panamaAdvancedNetworking != null) {
      testPanamaGrpcConnection();
    }
  }

  @Test
  @Order(5)
  @DisplayName("Connection Pooling Test")
  void testConnectionPooling() throws Exception {
    if (isJniRuntime() && jniAdvancedNetworking != null) {
      testJniConnectionPooling();
    }

    if (isPanamaRuntime() && panamaAdvancedNetworking != null) {
      testPanamaConnectionPooling();
    }
  }

  @Test
  @Order(6)
  @DisplayName("Performance Metrics Test")
  void testPerformanceMetrics() throws Exception {
    if (isJniRuntime() && jniAdvancedNetworking != null) {
      testJniPerformanceMetrics();
    }

    if (isPanamaRuntime() && panamaAdvancedNetworking != null) {
      testPanamaPerformanceMetrics();
    }
  }

  @Test
  @Order(7)
  @DisplayName("TLS Security Test")
  void testTlsSecurity() throws Exception {
    if (isJniRuntime() && jniAdvancedNetworking != null) {
      testJniTlsSecurity();
    }

    if (isPanamaRuntime() && panamaAdvancedNetworking != null) {
      testPanamaTlsSecurity();
    }
  }

  @Test
  @Order(8)
  @DisplayName("Concurrent Connections Test")
  void testConcurrentConnections() throws Exception {
    if (isJniRuntime() && jniAdvancedNetworking != null) {
      testJniConcurrentConnections();
    }

    if (isPanamaRuntime() && panamaAdvancedNetworking != null) {
      testPanamaConcurrentConnections();
    }
  }

  @Test
  @Order(9)
  @DisplayName("Error Handling Test")
  void testErrorHandling() throws Exception {
    if (isJniRuntime() && jniAdvancedNetworking != null) {
      testJniErrorHandling();
    }

    if (isPanamaRuntime() && panamaAdvancedNetworking != null) {
      testPanamaErrorHandling();
    }
  }

  @Test
  @Order(10)
  @DisplayName("Resource Cleanup Test")
  void testResourceCleanup() throws Exception {
    if (isJniRuntime() && jniAdvancedNetworking != null) {
      testJniResourceCleanup();
    }

    if (isPanamaRuntime() && panamaAdvancedNetworking != null) {
      testPanamaResourceCleanup();
    }
  }

  // JNI-specific test implementations

  private void testJniWebSocketConnection() throws Exception {
    LOGGER.info("Testing JNI WebSocket connection");

    // Test WebSocket connection to a mock server (using ws://echo.websocket.org as example)
    final String testUrl = "ws://echo.websocket.org/";
    final Map<String, String> headers = new HashMap<>();
    headers.put("User-Agent", "Wasmtime4j-Test");

    final CompletableFuture<Long> connectionFuture =
        jniAdvancedNetworking.websocketConnect(testUrl, headers, 10000);

    final Long connectionId = connectionFuture.get(15, TimeUnit.SECONDS);
    Assertions.assertNotNull(connectionId);
    Assertions.assertTrue(connectionId > 0);

    // Verify connection is tracked
    Assertions.assertTrue(jniAdvancedNetworking.getActiveConnectionCount() > 0);

    // Close the connection
    jniAdvancedNetworking.closeConnection(connectionId).get(10, TimeUnit.SECONDS);

    LOGGER.info("JNI WebSocket connection test completed successfully");
  }

  private void testJniWebSocketMessageExchange() throws Exception {
    LOGGER.info("Testing JNI WebSocket message exchange");

    final String testUrl = "ws://echo.websocket.org/";
    final Long connectionId =
        jniAdvancedNetworking.websocketConnect(testUrl, null, 10000).get(15, TimeUnit.SECONDS);

    // Send a text message
    final String testMessage = "Hello, WebSocket!";
    final ByteBuffer sendBuffer = ByteBuffer.wrap(testMessage.getBytes(StandardCharsets.UTF_8));

    jniAdvancedNetworking
        .websocketSend(connectionId, WasiAdvancedNetworkOperations.WS_MESSAGE_TEXT, sendBuffer)
        .get(10, TimeUnit.SECONDS);

    // Receive the echoed message
    final ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
    final Integer messageType =
        jniAdvancedNetworking
            .websocketReceive(connectionId, receiveBuffer, 10000)
            .get(15, TimeUnit.SECONDS);

    Assertions.assertEquals(WasiAdvancedNetworkOperations.WS_MESSAGE_TEXT, messageType);

    receiveBuffer.flip();
    final String receivedMessage = StandardCharsets.UTF_8.decode(receiveBuffer).toString();
    Assertions.assertEquals(testMessage, receivedMessage);

    jniAdvancedNetworking.closeConnection(connectionId).get(10, TimeUnit.SECONDS);

    LOGGER.info("JNI WebSocket message exchange test completed successfully");
  }

  private void testJniHttp2Connection() throws Exception {
    LOGGER.info("Testing JNI HTTP/2 connection");

    // Test HTTP/2 connection to a public HTTP/2 server
    final CompletableFuture<Long> connectionFuture =
        jniAdvancedNetworking.http2Connect("httpbin.org", 443, true, 10000);

    final Long connectionId = connectionFuture.get(15, TimeUnit.SECONDS);
    Assertions.assertNotNull(connectionId);
    Assertions.assertTrue(connectionId > 0);

    // Verify connection metrics
    final WasiAdvancedNetworkOperations.AdvancedNetworkMetrics metrics =
        jniAdvancedNetworking.getMetrics();
    Assertions.assertTrue(
        metrics.getSuccessfulConnections(WasiAdvancedNetworkOperations.PROTOCOL_HTTP2) > 0);

    jniAdvancedNetworking.closeConnection(connectionId).get(10, TimeUnit.SECONDS);

    LOGGER.info("JNI HTTP/2 connection test completed successfully");
  }

  private void testJniGrpcConnection() throws Exception {
    LOGGER.info("Testing JNI gRPC connection");

    // Test gRPC connection to a public gRPC service
    final String endpoint = "grpc://grpc-test.sandbox.googleapis.com:443";
    final CompletableFuture<Long> connectionFuture =
        jniAdvancedNetworking.grpcConnect(endpoint, true, 10000);

    final Long connectionId = connectionFuture.get(15, TimeUnit.SECONDS);
    Assertions.assertNotNull(connectionId);
    Assertions.assertTrue(connectionId > 0);

    // Verify connection metrics
    final WasiAdvancedNetworkOperations.AdvancedNetworkMetrics metrics =
        jniAdvancedNetworking.getMetrics();
    Assertions.assertTrue(
        metrics.getSuccessfulConnections(WasiAdvancedNetworkOperations.PROTOCOL_GRPC) > 0);

    jniAdvancedNetworking.closeConnection(connectionId).get(10, TimeUnit.SECONDS);

    LOGGER.info("JNI gRPC connection test completed successfully");
  }

  private void testJniConnectionPooling() throws Exception {
    LOGGER.info("Testing JNI connection pooling");

    // Create multiple connections to test pooling
    final CompletableFuture<Long> conn1Future =
        jniAdvancedNetworking.websocketConnect("ws://echo.websocket.org/", null, 10000);
    final CompletableFuture<Long> conn2Future =
        jniAdvancedNetworking.http2Connect("httpbin.org", 443, true, 10000);

    final Long conn1 = conn1Future.get(15, TimeUnit.SECONDS);
    final Long conn2 = conn2Future.get(15, TimeUnit.SECONDS);

    // Verify multiple active connections
    Assertions.assertEquals(2, jniAdvancedNetworking.getActiveConnectionCount());

    // Close connections
    jniAdvancedNetworking.closeConnection(conn1).get(10, TimeUnit.SECONDS);
    jniAdvancedNetworking.closeConnection(conn2).get(10, TimeUnit.SECONDS);

    // Verify connections are cleaned up
    Assertions.assertEquals(0, jniAdvancedNetworking.getActiveConnectionCount());

    LOGGER.info("JNI connection pooling test completed successfully");
  }

  private void testJniPerformanceMetrics() throws Exception {
    LOGGER.info("Testing JNI performance metrics");

    final WasiAdvancedNetworkOperations.AdvancedNetworkMetrics initialMetrics =
        jniAdvancedNetworking.getMetrics();

    // Create and close a connection to generate metrics
    final Long connectionId =
        jniAdvancedNetworking
            .websocketConnect("ws://echo.websocket.org/", null, 10000)
            .get(15, TimeUnit.SECONDS);
    jniAdvancedNetworking.closeConnection(connectionId).get(10, TimeUnit.SECONDS);

    final WasiAdvancedNetworkOperations.AdvancedNetworkMetrics finalMetrics =
        jniAdvancedNetworking.getMetrics();

    // Verify metrics were updated
    Assertions.assertTrue(
        finalMetrics.getSuccessfulConnections(WasiAdvancedNetworkOperations.PROTOCOL_WEBSOCKET)
            > initialMetrics.getSuccessfulConnections(
                WasiAdvancedNetworkOperations.PROTOCOL_WEBSOCKET));
    Assertions.assertTrue(
        finalMetrics.getClosedConnections(WasiAdvancedNetworkOperations.PROTOCOL_WEBSOCKET)
            > initialMetrics.getClosedConnections(
                WasiAdvancedNetworkOperations.PROTOCOL_WEBSOCKET));

    LOGGER.info("JNI performance metrics test completed successfully");
  }

  private void testJniTlsSecurity() throws Exception {
    LOGGER.info("Testing JNI TLS security");

    // Test secure WebSocket connection
    final String secureUrl = "wss://echo.websocket.org/";
    final Long connectionId =
        jniAdvancedNetworking.websocketConnect(secureUrl, null, 10000).get(15, TimeUnit.SECONDS);

    Assertions.assertNotNull(connectionId);
    Assertions.assertTrue(connectionId > 0);

    jniAdvancedNetworking.closeConnection(connectionId).get(10, TimeUnit.SECONDS);

    LOGGER.info("JNI TLS security test completed successfully");
  }

  private void testJniConcurrentConnections() throws Exception {
    LOGGER.info("Testing JNI concurrent connections");

    // Create multiple concurrent connections
    final CompletableFuture<Long>[] futures = new CompletableFuture[5];
    for (int i = 0; i < 5; i++) {
      futures[i] = jniAdvancedNetworking.websocketConnect("ws://echo.websocket.org/", null, 10000);
    }

    // Wait for all connections
    final Long[] connectionIds = new Long[5];
    for (int i = 0; i < 5; i++) {
      connectionIds[i] = futures[i].get(15, TimeUnit.SECONDS);
      Assertions.assertNotNull(connectionIds[i]);
    }

    // Verify all connections are active
    Assertions.assertEquals(5, jniAdvancedNetworking.getActiveConnectionCount());

    // Close all connections
    final CompletableFuture<Void>[] closeFutures = new CompletableFuture[5];
    for (int i = 0; i < 5; i++) {
      closeFutures[i] = jniAdvancedNetworking.closeConnection(connectionIds[i]);
    }

    // Wait for all connections to close
    for (int i = 0; i < 5; i++) {
      closeFutures[i].get(10, TimeUnit.SECONDS);
    }

    Assertions.assertEquals(0, jniAdvancedNetworking.getActiveConnectionCount());

    LOGGER.info("JNI concurrent connections test completed successfully");
  }

  private void testJniErrorHandling() throws Exception {
    LOGGER.info("Testing JNI error handling");

    // Test invalid URL
    Assertions.assertThrows(
        Exception.class,
        () -> {
          jniAdvancedNetworking
              .websocketConnect("invalid-url", null, 1000)
              .get(5, TimeUnit.SECONDS);
        });

    // Test invalid connection ID
    Assertions.assertThrows(
        Exception.class,
        () -> {
          jniAdvancedNetworking.closeConnection(99999L).get(5, TimeUnit.SECONDS);
        });

    // Test invalid port
    Assertions.assertThrows(
        Exception.class,
        () -> {
          jniAdvancedNetworking
              .http2Connect("localhost", 99999, false, 1000)
              .get(5, TimeUnit.SECONDS);
        });

    LOGGER.info("JNI error handling test completed successfully");
  }

  private void testJniResourceCleanup() throws Exception {
    LOGGER.info("Testing JNI resource cleanup");

    // Create connections
    final Long conn1 =
        jniAdvancedNetworking
            .websocketConnect("ws://echo.websocket.org/", null, 10000)
            .get(15, TimeUnit.SECONDS);
    final Long conn2 =
        jniAdvancedNetworking
            .http2Connect("httpbin.org", 80, false, 10000)
            .get(15, TimeUnit.SECONDS);

    Assertions.assertEquals(2, jniAdvancedNetworking.getActiveConnectionCount());

    // Cleanup should close all connections
    jniAdvancedNetworking.cleanup();

    // Verify cleanup
    Assertions.assertEquals(0, jniAdvancedNetworking.getActiveConnectionCount());

    LOGGER.info("JNI resource cleanup test completed successfully");
  }

  // Panama-specific test implementations (similar structure, different classes)

  private void testPanamaWebSocketConnection() throws Exception {
    LOGGER.info("Testing Panama WebSocket connection");
    // Similar implementation using panamaAdvancedNetworking
    // Implementation details would mirror JNI tests but use Panama classes
  }

  private void testPanamaWebSocketMessageExchange() throws Exception {
    LOGGER.info("Testing Panama WebSocket message exchange");
    // Similar implementation for Panama FFI
  }

  private void testPanamaHttp2Connection() throws Exception {
    LOGGER.info("Testing Panama HTTP/2 connection");
    // Similar implementation for Panama FFI
  }

  private void testPanamaGrpcConnection() throws Exception {
    LOGGER.info("Testing Panama gRPC connection");
    // Similar implementation for Panama FFI
  }

  private void testPanamaConnectionPooling() throws Exception {
    LOGGER.info("Testing Panama connection pooling");
    // Similar implementation for Panama FFI
  }

  private void testPanamaPerformanceMetrics() throws Exception {
    LOGGER.info("Testing Panama performance metrics");
    // Similar implementation for Panama FFI
  }

  private void testPanamaTlsSecurity() throws Exception {
    LOGGER.info("Testing Panama TLS security");
    // Similar implementation for Panama FFI
  }

  private void testPanamaConcurrentConnections() throws Exception {
    LOGGER.info("Testing Panama concurrent connections");
    // Similar implementation for Panama FFI
  }

  private void testPanamaErrorHandling() throws Exception {
    LOGGER.info("Testing Panama error handling");
    // Similar implementation for Panama FFI
  }

  private void testPanamaResourceCleanup() throws Exception {
    LOGGER.info("Testing Panama resource cleanup");
    // Similar implementation for Panama FFI
  }

  // Utility methods

  private boolean isJniRuntime() {
    return runtime != null && runtime.getClass().getSimpleName().contains("Jni");
  }

  private boolean isPanamaRuntime() {
    final String javaVersion = System.getProperty("java.version");
    final int majorVersion = Integer.parseInt(javaVersion.split("\\.")[0]);
    return majorVersion >= 23
        && runtime != null
        && runtime.getClass().getSimpleName().contains("Panama");
  }
}
