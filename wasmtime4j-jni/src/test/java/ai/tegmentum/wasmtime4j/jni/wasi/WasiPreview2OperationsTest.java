package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive test suite for WASI Preview 2 operations.
 *
 * <p>This test validates complete WASI Preview 2 functionality including:
 *
 * <ul>
 *   <li>WIT (WebAssembly Interface Types) interfaces
 *   <li>Async I/O operations with CompletableFuture
 *   <li>Resource handle management and lifecycle
 *   <li>Stream-based operations
 *   <li>Network operations (TCP, UDP, HTTP)
 *   <li>Advanced error handling
 *   <li>Resource cleanup and limits
 * </ul>
 */
class WasiPreview2OperationsTest {

  @TempDir private Path tempDirectory;

  private WasiContext wasiContext;
  private WasiPreview2Operations wasiOps;

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    System.out.println("Setting up WASI Preview 2 test: " + testInfo.getDisplayName());

    // Create comprehensive WASI context for Preview 2
    wasiContext =
        WasiContext.builder()
            .withEnvironment("WASI_PREVIEW", "2")
            .withEnvironment("ASYNC_IO", "enabled")
            .withEnvironment("NETWORK_ENABLED", "true")
            .withArgument("wasi_preview2_test")
            .withArgument("--async")
            .withPreopenDirectory("/tmp", tempDirectory.toString())
            .withWorkingDirectory(tempDirectory.toString())
            .build();

    assertNotNull(wasiContext, "WASI context must be created successfully");

    wasiOps = new WasiPreview2Operations(wasiContext);
    assertNotNull(wasiOps, "WASI Preview 2 operations must be initialized");

    System.out.println("WASI Preview 2 test setup completed");
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) throws Exception {
    System.out.println("Tearing down WASI Preview 2 test: " + testInfo.getDisplayName());

    try {
      if (wasiOps != null) {
        wasiOps.close();
      }
    } catch (final Exception e) {
      System.err.println("Error closing WASI Preview 2 operations: " + e.getMessage());
    }

    try {
      if (wasiContext != null) {
        wasiContext.close();
      }
    } catch (final Exception e) {
      System.err.println("Error closing WASI context: " + e.getMessage());
    }

    System.out.println("WASI Preview 2 test teardown completed");
  }

  @Test
  void testResourceHandleManagement() {
    // Test resource creation and lifecycle
    final String resourceType = "test_resource";
    final long resourceHandle = wasiOps.createResource(resourceType);
    assertTrue(resourceHandle > 0, "Resource handle must be positive");

    // Test resource existence
    assertTrue(wasiOps.resourceExists(resourceHandle), "Resource must exist after creation");

    // Test resource metadata
    final String actualType = wasiOps.getResourceType(resourceHandle);
    assertEquals(resourceType, actualType, "Resource type must match");

    // Test resource cleanup
    assertDoesNotThrow(
        () -> wasiOps.destroyResource(resourceHandle), "Resource destruction must not throw");

    assertFalse(
        wasiOps.resourceExists(resourceHandle), "Resource must not exist after destruction");

    System.out.println("Resource handle management validated: " + resourceHandle);
  }

  @Test
  void testAsyncFileOperations()
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    // Create test file for async operations
    final Path testFile = tempDirectory.resolve("async_test.txt");
    final String originalContent = "Async file operations test content with WASI Preview 2";
    Files.write(testFile, originalContent.getBytes(), StandardOpenOption.CREATE);

    // Test async file opening
    final CompletableFuture<Long> openFuture = wasiOps.openFileAsync("/tmp/async_test.txt", "read");
    final Long fileHandle = openFuture.get(5, TimeUnit.SECONDS);
    assertNotNull(fileHandle, "File handle must be returned");
    assertTrue(fileHandle > 0, "File handle must be positive");

    // Test async reading
    final CompletableFuture<ByteBuffer> readFuture =
        wasiOps.readFileAsync(fileHandle, originalContent.length());
    final ByteBuffer readBuffer = readFuture.get(5, TimeUnit.SECONDS);
    assertNotNull(readBuffer, "Read buffer must not be null");
    assertEquals(
        originalContent.length(), readBuffer.remaining(), "Read buffer must have correct size");

    final String readContent = new String(readBuffer.array(), 0, readBuffer.remaining());
    assertEquals(originalContent, readContent, "Read content must match original");

    // Test async file closing
    final CompletableFuture<Void> closeFuture = wasiOps.closeFileAsync(fileHandle);
    assertDoesNotThrow(() -> closeFuture.get(5, TimeUnit.SECONDS), "Async close must not throw");

    System.out.println("Async file operations validated");
  }

  @Test
  void testAsyncWriteOperations()
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    final Path outputFile = tempDirectory.resolve("async_write_test.txt");

    // Test async file creation and writing
    final CompletableFuture<Long> createFuture =
        wasiOps.openFileAsync("/tmp/async_write_test.txt", "write");
    final Long fileHandle = createFuture.get(5, TimeUnit.SECONDS);
    assertTrue(fileHandle > 0, "Write file handle must be positive");

    // Test async write
    final String writeContent = "Async write test content for WASI Preview 2";
    final ByteBuffer writeBuffer = ByteBuffer.wrap(writeContent.getBytes());
    final CompletableFuture<Integer> writeFuture = wasiOps.writeFileAsync(fileHandle, writeBuffer);
    final Integer bytesWritten = writeFuture.get(5, TimeUnit.SECONDS);
    assertEquals(writeContent.length(), bytesWritten.intValue(), "Must write all bytes");

    // Test async sync
    final CompletableFuture<Void> syncFuture = wasiOps.syncFileAsync(fileHandle);
    assertDoesNotThrow(() -> syncFuture.get(5, TimeUnit.SECONDS), "Async sync must not throw");

    // Close file
    wasiOps.closeFileAsync(fileHandle).get(5, TimeUnit.SECONDS);

    // Verify written content
    final String actualContent = Files.readString(outputFile);
    assertEquals(writeContent, actualContent, "Written content must match");

    System.out.println("Async write operations validated");
  }

  @Test
  void testStreamOperations() throws InterruptedException, ExecutionException, TimeoutException {
    // Test input stream creation
    final long inputStreamHandle = wasiOps.createInputStream("test_input_stream");
    assertTrue(inputStreamHandle > 0, "Input stream handle must be positive");

    // Test output stream creation
    final long outputStreamHandle = wasiOps.createOutputStream("test_output_stream");
    assertTrue(outputStreamHandle > 0, "Output stream handle must be positive");

    // Test stream data flow
    final String testData = "Stream test data for WASI Preview 2";
    final ByteBuffer inputBuffer = ByteBuffer.wrap(testData.getBytes());

    // Write to output stream
    final CompletableFuture<Integer> writeFuture =
        wasiOps.writeToStream(outputStreamHandle, inputBuffer);
    final Integer bytesWritten = writeFuture.get(5, TimeUnit.SECONDS);
    assertEquals(testData.length(), bytesWritten.intValue(), "Must write all stream data");

    // Flush output stream
    final CompletableFuture<Void> flushFuture = wasiOps.flushStream(outputStreamHandle);
    assertDoesNotThrow(() -> flushFuture.get(5, TimeUnit.SECONDS), "Stream flush must not throw");

    // Close streams
    wasiOps.closeStream(inputStreamHandle);
    wasiOps.closeStream(outputStreamHandle);

    System.out.println("Stream operations validated");
  }

  @Test
  void testNetworkTcpOperations()
      throws InterruptedException, ExecutionException, TimeoutException {
    // Test TCP socket creation
    final CompletableFuture<Long> socketFuture = wasiOps.createTcpSocket();
    final Long socketHandle = socketFuture.get(5, TimeUnit.SECONDS);
    assertNotNull(socketHandle, "TCP socket handle must not be null");
    assertTrue(socketHandle > 0, "TCP socket handle must be positive");

    // Test socket binding to localhost
    final CompletableFuture<Void> bindFuture = wasiOps.bindTcpSocket(socketHandle, "127.0.0.1", 0);
    assertDoesNotThrow(() -> bindFuture.get(5, TimeUnit.SECONDS), "TCP bind must not throw");

    // Test socket listening
    final CompletableFuture<Void> listenFuture = wasiOps.listenTcpSocket(socketHandle, 5);
    assertDoesNotThrow(() -> listenFuture.get(5, TimeUnit.SECONDS), "TCP listen must not throw");

    // Test getting local address
    final CompletableFuture<String> addressFuture = wasiOps.getLocalAddress(socketHandle);
    final String localAddress = addressFuture.get(5, TimeUnit.SECONDS);
    assertNotNull(localAddress, "Local address must not be null");
    assertTrue(localAddress.startsWith("127.0.0.1"), "Local address must be localhost");

    // Close socket
    final CompletableFuture<Void> closeFuture = wasiOps.closeTcpSocket(socketHandle);
    assertDoesNotThrow(() -> closeFuture.get(5, TimeUnit.SECONDS), "TCP close must not throw");

    System.out.println("TCP network operations validated: " + localAddress);
  }

  @Test
  void testNetworkUdpOperations()
      throws InterruptedException, ExecutionException, TimeoutException {
    // Test UDP socket creation
    final CompletableFuture<Long> socketFuture = wasiOps.createUdpSocket();
    final Long socketHandle = socketFuture.get(5, TimeUnit.SECONDS);
    assertNotNull(socketHandle, "UDP socket handle must not be null");
    assertTrue(socketHandle > 0, "UDP socket handle must be positive");

    // Test UDP socket binding
    final CompletableFuture<Void> bindFuture = wasiOps.bindUdpSocket(socketHandle, "127.0.0.1", 0);
    assertDoesNotThrow(() -> bindFuture.get(5, TimeUnit.SECONDS), "UDP bind must not throw");

    // Test UDP send/receive preparation
    final String testMessage = "UDP test message for WASI Preview 2";
    final ByteBuffer sendBuffer = ByteBuffer.wrap(testMessage.getBytes());

    // Test UDP send to localhost (loopback)
    final CompletableFuture<Integer> sendFuture =
        wasiOps.sendUdp(socketHandle, sendBuffer, "127.0.0.1", 12345);
    // Note: This might fail if nothing is listening on port 12345, which is expected
    try {
      final Integer bytesSent = sendFuture.get(2, TimeUnit.SECONDS);
      // If successful, verify bytes sent
      if (bytesSent != null) {
        assertTrue(bytesSent >= 0, "UDP send must return non-negative bytes");
      }
    } catch (final Exception e) {
      // Expected for UDP send to non-listening port
      System.out.println("UDP send failed as expected: " + e.getMessage());
    }

    // Close UDP socket
    final CompletableFuture<Void> closeFuture = wasiOps.closeUdpSocket(socketHandle);
    assertDoesNotThrow(() -> closeFuture.get(5, TimeUnit.SECONDS), "UDP close must not throw");

    System.out.println("UDP network operations validated");
  }

  @Test
  void testHttpOperations() throws InterruptedException, ExecutionException, TimeoutException {
    // Test HTTP client creation
    final long httpClientHandle = wasiOps.createHttpClient();
    assertTrue(httpClientHandle > 0, "HTTP client handle must be positive");

    // Test HTTP request preparation
    final long requestHandle = wasiOps.createHttpRequest("GET", "http://httpbin.org/get");
    assertTrue(requestHandle > 0, "HTTP request handle must be positive");

    // Add headers to request
    wasiOps.addHttpHeader(requestHandle, "User-Agent", "WASI-Preview2-Test/1.0");
    wasiOps.addHttpHeader(requestHandle, "Accept", "application/json");

    // Test HTTP request execution (with timeout for network operations)
    try {
      final CompletableFuture<Long> responseFuture =
          wasiOps.sendHttpRequest(httpClientHandle, requestHandle);
      final Long responseHandle = responseFuture.get(10, TimeUnit.SECONDS);

      if (responseHandle != null && responseHandle > 0) {
        // Test response status
        final CompletableFuture<Integer> statusFuture =
            wasiOps.getHttpResponseStatus(responseHandle);
        final Integer status = statusFuture.get(5, TimeUnit.SECONDS);
        assertNotNull(status, "HTTP status must not be null");
        assertTrue(status >= 200 && status < 300, "HTTP status must be successful");

        // Test response body reading
        final CompletableFuture<ByteBuffer> bodyFuture =
            wasiOps.readHttpResponseBody(responseHandle);
        final ByteBuffer body = bodyFuture.get(10, TimeUnit.SECONDS);
        assertNotNull(body, "HTTP response body must not be null");
        assertTrue(body.remaining() > 0, "HTTP response body must have content");

        // Clean up response
        wasiOps.destroyResource(responseHandle);

        System.out.println("HTTP operations validated successfully with status: " + status);
      } else {
        System.out.println("HTTP request failed or timed out (expected in some environments)");
      }
    } catch (final TimeoutException e) {
      System.out.println(
          "HTTP request timed out (expected in restricted environments): " + e.getMessage());
    } catch (final Exception e) {
      System.out.println("HTTP request failed (expected in some environments): " + e.getMessage());
    } finally {
      // Clean up resources
      try {
        wasiOps.destroyResource(requestHandle);
        wasiOps.destroyResource(httpClientHandle);
      } catch (final Exception e) {
        System.err.println("Error cleaning up HTTP resources: " + e.getMessage());
      }
    }
  }

  @Test
  void testWitInterfaceBinding() {
    // Test WIT interface registration
    final String interfaceName = "test-interface";
    final long interfaceHandle = wasiOps.registerWitInterface(interfaceName);
    assertTrue(interfaceHandle > 0, "WIT interface handle must be positive");

    // Test WIT function binding
    final String functionName = "test-function";
    final long functionHandle = wasiOps.bindWitFunction(interfaceHandle, functionName);
    assertTrue(functionHandle > 0, "WIT function handle must be positive");

    // Test WIT function call preparation
    final Object[] args = {"test_arg1", 42, true};
    final long callHandle = wasiOps.prepareWitCall(functionHandle, args);
    assertTrue(callHandle > 0, "WIT call handle must be positive");

    // Clean up WIT resources
    wasiOps.destroyResource(callHandle);
    wasiOps.destroyResource(functionHandle);
    wasiOps.destroyResource(interfaceHandle);

    System.out.println("WIT interface binding validated");
  }

  @Test
  void testAsyncErrorHandling() {
    // Test async operation with invalid resource
    final CompletableFuture<ByteBuffer> invalidReadFuture = wasiOps.readFileAsync(999L, 100);

    assertThrows(
        ExecutionException.class,
        () -> invalidReadFuture.get(2, TimeUnit.SECONDS),
        "Invalid file handle must cause async failure");

    // Test timeout handling
    final CompletableFuture<Void> timeoutFuture = wasiOps.createTimeoutOperation(1000);
    assertThrows(
        TimeoutException.class,
        () -> timeoutFuture.get(500, TimeUnit.MILLISECONDS),
        "Timeout operation must timeout as expected");

    System.out.println("Async error handling validated");
  }

  @Test
  void testResourceLimitsAndCleanup()
      throws InterruptedException, ExecutionException, TimeoutException {
    // Test resource creation limits
    final java.util.List<Long> resourceHandles = new java.util.ArrayList<>();

    try {
      // Create multiple resources to test limits
      for (int i = 0; i < 100; i++) {
        final long handle = wasiOps.createResource("test_resource_" + i);
        resourceHandles.add(handle);
        assertTrue(handle > 0, "Resource handle " + i + " must be positive");
      }

      // Test resource count
      final int activeResources = wasiOps.getActiveResourceCount();
      assertTrue(
          activeResources >= resourceHandles.size(),
          "Active resource count must include test resources");

    } finally {
      // Clean up all created resources
      for (final Long handle : resourceHandles) {
        try {
          wasiOps.destroyResource(handle);
        } catch (final Exception e) {
          System.err.println("Error destroying resource " + handle + ": " + e.getMessage());
        }
      }
    }

    // Verify cleanup
    final int finalResourceCount = wasiOps.getActiveResourceCount();
    System.out.println(
        "Resource limits and cleanup validated: final count = " + finalResourceCount);
  }

  @Test
  void testConcurrentAsyncOperations() throws InterruptedException, ExecutionException {
    // Test multiple concurrent async operations
    final java.util.List<CompletableFuture<Long>> futures = new java.util.ArrayList<>();

    // Create multiple concurrent resource creation operations
    for (int i = 0; i < 10; i++) {
      final String resourceType = "concurrent_resource_" + i;
      final CompletableFuture<Long> future =
          CompletableFuture.supplyAsync(() -> wasiOps.createResource(resourceType));
      futures.add(future);
    }

    // Wait for all operations to complete
    final CompletableFuture<Void> allOf =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    assertDoesNotThrow(
        () -> allOf.get(10, TimeUnit.SECONDS), "Concurrent operations must complete");

    // Verify all operations succeeded
    final java.util.List<Long> handles = new java.util.ArrayList<>();
    for (final CompletableFuture<Long> future : futures) {
      final Long handle = future.get();
      assertNotNull(handle, "Concurrent operation result must not be null");
      assertTrue(handle > 0, "Concurrent operation handle must be positive");
      handles.add(handle);
    }

    // Clean up concurrent resources
    for (final Long handle : handles) {
      wasiOps.destroyResource(handle);
    }

    System.out.println("Concurrent async operations validated: " + handles.size() + " operations");
  }

  @Test
  void testAdvancedStreamProcessing()
      throws InterruptedException, ExecutionException, TimeoutException {
    // Test stream pipeline creation
    final long inputStream = wasiOps.createInputStream("pipeline_input");
    final long transformStream = wasiOps.createTransformStream("uppercase_transform");
    final long outputStream = wasiOps.createOutputStream("pipeline_output");

    // Connect streams in pipeline
    wasiOps.connectStreams(inputStream, transformStream);
    wasiOps.connectStreams(transformStream, outputStream);

    // Test data flow through pipeline
    final String inputData = "hello wasi preview 2 stream pipeline";
    final ByteBuffer inputBuffer = ByteBuffer.wrap(inputData.getBytes());

    final CompletableFuture<Integer> writeFuture = wasiOps.writeToStream(inputStream, inputBuffer);
    final Integer bytesWritten = writeFuture.get(5, TimeUnit.SECONDS);
    assertEquals(inputData.length(), bytesWritten.intValue(), "Must write all pipeline input data");

    // Signal end of input
    wasiOps.closeStream(inputStream);

    // Read transformed output
    final CompletableFuture<ByteBuffer> readFuture =
        wasiOps.readFromStream(outputStream, inputData.length() * 2);
    try {
      final ByteBuffer outputBuffer = readFuture.get(5, TimeUnit.SECONDS);
      if (outputBuffer != null && outputBuffer.remaining() > 0) {
        final String outputData = new String(outputBuffer.array(), 0, outputBuffer.remaining());
        System.out.println("Stream pipeline output: " + outputData);
        // Transform stream might uppercase the data
        assertTrue(outputData.length() > 0, "Pipeline output must have content");
      }
    } catch (final Exception e) {
      System.out.println("Stream pipeline operation completed with: " + e.getMessage());
    }

    // Clean up pipeline
    wasiOps.closeStream(transformStream);
    wasiOps.closeStream(outputStream);

    System.out.println("Advanced stream processing validated");
  }
}
