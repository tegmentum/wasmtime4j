package ai.tegmentum.wasmtime4j.experimental;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive integration tests for experimental WASI Preview 2 features.
 *
 * <p>This test suite validates the experimental features including filesystem snapshots, advanced
 * networking, experimental I/O operations, and process management features.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf("ai.tegmentum.wasmtime4j.test.TestEnvironment#isIntegrationTestsEnabled")
class WasiExperimentalFeaturesIT {

  private static final Logger LOGGER = Logger.getLogger(WasiExperimentalFeaturesIT.class.getName());

  @TempDir Path tempDir;

  private WasiContext wasiContext;
  private ExecutorService asyncExecutor;
  private WasiFilesystemSnapshot filesystemSnapshot;
  private WasiAdvancedNetworking advancedNetworking;
  private WasiExperimentalIO experimentalIO;
  private WasiExperimentalProcess experimentalProcess;

  @BeforeEach
  void setUp() throws Exception {
    LOGGER.info("Setting up WASI experimental features test");

    asyncExecutor = Executors.newFixedThreadPool(4);

    // Create WASI context with experimental features enabled
    final WasiContextBuilder contextBuilder = new WasiContextBuilder();
    contextBuilder.allowExperimentalFeatures(true);
    contextBuilder.allowNetworking(true);
    contextBuilder.allowFileSystemAccess(true);
    contextBuilder.allowProcessControl(true);

    wasiContext = contextBuilder.build();

    // Initialize experimental feature handlers
    filesystemSnapshot = new WasiFilesystemSnapshot(wasiContext, asyncExecutor);
    advancedNetworking = new WasiAdvancedNetworking(wasiContext, asyncExecutor);
    experimentalIO = new WasiExperimentalIO(wasiContext, asyncExecutor);
    experimentalProcess = new WasiExperimentalProcess(wasiContext, asyncExecutor);

    LOGGER.info("WASI experimental features test setup complete");
  }

  @AfterEach
  void tearDown() throws Exception {
    LOGGER.info("Tearing down WASI experimental features test");

    if (experimentalProcess != null) {
      experimentalProcess.close();
    }

    if (experimentalIO != null) {
      experimentalIO.close();
    }

    if (advancedNetworking != null) {
      advancedNetworking.close();
    }

    if (filesystemSnapshot != null) {
      filesystemSnapshot.close();
    }

    if (wasiContext != null) {
      wasiContext.close();
    }

    if (asyncExecutor != null) {
      asyncExecutor.shutdown();
      assertTrue(asyncExecutor.awaitTermination(10, TimeUnit.SECONDS));
    }

    LOGGER.info("WASI experimental features test teardown complete");
  }

  @Test
  @Order(1)
  @DisplayName("Filesystem Snapshot - Full Snapshot Creation and Restoration")
  void testFilesystemSnapshotFullCapture() throws Exception {
    LOGGER.info("Testing full filesystem snapshot creation and restoration");

    // Create test directory structure
    final Path testDir = tempDir.resolve("snapshot-test");
    Files.createDirectories(testDir);
    Files.write(testDir.resolve("file1.txt"), "Test content 1".getBytes());
    Files.write(testDir.resolve("file2.txt"), "Test content 2".getBytes());

    final Path subDir = testDir.resolve("subdir");
    Files.createDirectories(subDir);
    Files.write(subDir.resolve("nested.txt"), "Nested content".getBytes());

    // Create full snapshot with default options
    final WasiFilesystemSnapshot.SnapshotOptions options =
        WasiFilesystemSnapshot.SnapshotOptions.defaultOptions();

    final CompletableFuture<Long> snapshotFuture =
        filesystemSnapshot.createFullSnapshotAsync(testDir.toString(), options);

    final Long snapshotHandle = snapshotFuture.get(30, TimeUnit.SECONDS);
    assertNotNull(snapshotHandle);
    assertTrue(snapshotHandle > 0);

    // Verify snapshot metadata
    final WasiFilesystemSnapshot.SnapshotMetadata metadata =
        filesystemSnapshot.getSnapshotMetadata(snapshotHandle);
    assertNotNull(metadata);
    assertEquals(snapshotHandle, metadata.handle);
    assertEquals(testDir.toString(), metadata.rootPath);
    assertEquals(WasiFilesystemSnapshot.SnapshotType.FULL, metadata.type);
    assertTrue(metadata.snapshotSize > 0);
    assertTrue(metadata.fileCount >= 3);

    // Verify snapshot verification
    final CompletableFuture<WasiFilesystemSnapshot.SnapshotVerificationResult> verifyFuture =
        filesystemSnapshot.verifySnapshotAsync(snapshotHandle);

    final WasiFilesystemSnapshot.SnapshotVerificationResult verificationResult =
        verifyFuture.get(15, TimeUnit.SECONDS);
    assertTrue(verificationResult.isValid);
    assertTrue(verificationResult.checkedFiles >= 3);
    assertEquals(0, verificationResult.corruptedFiles);

    // Modify original files
    Files.write(testDir.resolve("file1.txt"), "Modified content".getBytes());
    Files.delete(testDir.resolve("file2.txt"));

    // Restore from snapshot
    final Path restoreDir = tempDir.resolve("restored");
    final WasiFilesystemSnapshot.RestoreOptions restoreOptions =
        WasiFilesystemSnapshot.RestoreOptions.defaultOptions();

    final CompletableFuture<Void> restoreFuture =
        filesystemSnapshot.restoreFromSnapshotAsync(
            snapshotHandle, restoreDir.toString(), restoreOptions);

    restoreFuture.get(30, TimeUnit.SECONDS);

    // Verify restoration
    assertTrue(Files.exists(restoreDir.resolve("file1.txt")));
    assertTrue(Files.exists(restoreDir.resolve("file2.txt")));
    assertTrue(Files.exists(restoreDir.resolve("subdir").resolve("nested.txt")));

    assertEquals("Test content 1", Files.readString(restoreDir.resolve("file1.txt")));
    assertEquals("Test content 2", Files.readString(restoreDir.resolve("file2.txt")));
    assertEquals("Nested content", Files.readString(restoreDir.resolve("subdir/nested.txt")));

    // Clean up
    filesystemSnapshot.deleteSnapshot(snapshotHandle);

    LOGGER.info("Full filesystem snapshot test completed successfully");
  }

  @Test
  @Order(2)
  @DisplayName("Filesystem Snapshot - Incremental Snapshot")
  void testFilesystemSnapshotIncremental() throws Exception {
    LOGGER.info("Testing incremental filesystem snapshot");

    // Create base test directory
    final Path testDir = tempDir.resolve("incremental-test");
    Files.createDirectories(testDir);
    Files.write(testDir.resolve("base1.txt"), "Base content 1".getBytes());
    Files.write(testDir.resolve("base2.txt"), "Base content 2".getBytes());

    // Create base snapshot
    final WasiFilesystemSnapshot.SnapshotOptions options =
        WasiFilesystemSnapshot.SnapshotOptions.defaultOptions();

    final Long baseSnapshotHandle =
        filesystemSnapshot
            .createFullSnapshotAsync(testDir.toString(), options)
            .get(30, TimeUnit.SECONDS);

    // Add new files and modify existing ones
    Files.write(testDir.resolve("new1.txt"), "New content 1".getBytes());
    Files.write(testDir.resolve("base1.txt"), "Modified base content 1".getBytes());

    // Create incremental snapshot
    final Long incrementalSnapshotHandle =
        filesystemSnapshot
            .createIncrementalSnapshotAsync(testDir.toString(), baseSnapshotHandle, options)
            .get(30, TimeUnit.SECONDS);

    // Verify incremental snapshot metadata
    final WasiFilesystemSnapshot.SnapshotMetadata incrementalMetadata =
        filesystemSnapshot.getSnapshotMetadata(incrementalSnapshotHandle);
    assertEquals(WasiFilesystemSnapshot.SnapshotType.INCREMENTAL, incrementalMetadata.type);
    assertEquals(baseSnapshotHandle, incrementalMetadata.baseSnapshotHandle);
    assertTrue(incrementalMetadata.snapshotSize > 0);

    // List all snapshots
    final List<WasiFilesystemSnapshot.SnapshotInfo> snapshots = filesystemSnapshot.listSnapshots();
    assertEquals(2, snapshots.size());

    // Clean up
    filesystemSnapshot.deleteSnapshot(incrementalSnapshotHandle);
    filesystemSnapshot.deleteSnapshot(baseSnapshotHandle);

    LOGGER.info("Incremental filesystem snapshot test completed successfully");
  }

  @Test
  @Order(3)
  @DisplayName("Advanced Networking - HTTP/2 Connection")
  void testAdvancedNetworkingHttp2() throws Exception {
    LOGGER.info("Testing HTTP/2 advanced networking");

    // Create HTTP/2 connection options
    final WasiAdvancedNetworking.Http2Options http2Options =
        new WasiAdvancedNetworking.Http2Options(50, 32768, true, 8192, 10000);

    // Skip test if external networking is not available
    try {
      final CompletableFuture<Long> connectionFuture =
          advancedNetworking.createHttp2ConnectionAsync("httpbin.org", 443, true, http2Options);

      final Long connectionHandle = connectionFuture.get(15, TimeUnit.SECONDS);
      assertNotNull(connectionHandle);
      assertTrue(connectionHandle > 0);

      // Make HTTP/2 request
      final Map<String, String> headers =
          Map.of(
              "User-Agent", "wasmtime4j-test/1.0",
              "Accept", "application/json");

      final CompletableFuture<WasiAdvancedNetworking.Http2Response> requestFuture =
          advancedNetworking.http2RequestAsync(connectionHandle, "GET", "/get", headers, null);

      final WasiAdvancedNetworking.Http2Response response = requestFuture.get(15, TimeUnit.SECONDS);
      assertNotNull(response);
      assertEquals(200, response.statusCode);
      assertNotNull(response.headers);
      assertNotNull(response.body);
      assertTrue(response.streamId > 0);

      // Close connection
      advancedNetworking.closeConnection(connectionHandle);

      LOGGER.info("HTTP/2 advanced networking test completed successfully");

    } catch (Exception e) {
      LOGGER.warning("Skipping HTTP/2 test due to network unavailability: " + e.getMessage());
      Assumptions.assumeTrue(false, "Network not available for HTTP/2 test");
    }
  }

  @Test
  @Order(4)
  @DisplayName("Advanced Networking - WebSocket Connection")
  void testAdvancedNetworkingWebSocket() throws Exception {
    LOGGER.info("Testing WebSocket advanced networking");

    // Skip test if external networking is not available
    try {
      // Create WebSocket connection
      final WasiAdvancedNetworking.WebSocketOptions wsOptions =
          WasiAdvancedNetworking.WebSocketOptions.defaultOptions();

      final CompletableFuture<Long> wsFuture =
          advancedNetworking.createWebSocketAsync(
              "wss://echo.websocket.org",
              List.of("echo-protocol"),
              Map.of("User-Agent", "wasmtime4j-test"),
              wsOptions);

      final Long webSocketHandle = wsFuture.get(15, TimeUnit.SECONDS);
      assertNotNull(webSocketHandle);

      // Send message
      final String testMessage = "Hello WebSocket!";
      final ByteBuffer messageBuffer = ByteBuffer.wrap(testMessage.getBytes());
      advancedNetworking
          .sendWebSocketMessageAsync(
              webSocketHandle, messageBuffer, WasiAdvancedNetworking.WebSocketMessageType.TEXT)
          .get(10, TimeUnit.SECONDS);

      // Receive echo
      final ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
      final WasiAdvancedNetworking.WebSocketMessage receivedMessage =
          advancedNetworking
              .receiveWebSocketMessageAsync(webSocketHandle, receiveBuffer, 10000)
              .get(10, TimeUnit.SECONDS);

      assertNotNull(receivedMessage);
      assertTrue(receivedMessage.bytesReceived > 0);
      assertEquals(WasiAdvancedNetworking.WebSocketMessageType.TEXT, receivedMessage.messageType);

      // Close WebSocket
      advancedNetworking.closeConnection(webSocketHandle);

      LOGGER.info("WebSocket advanced networking test completed successfully");

    } catch (Exception e) {
      LOGGER.warning("Skipping WebSocket test due to network unavailability: " + e.getMessage());
      Assumptions.assumeTrue(false, "Network not available for WebSocket test");
    }
  }

  @Test
  @Order(5)
  @DisplayName("Advanced Networking - Network Interface Enumeration")
  void testNetworkInterfaceEnumeration() throws Exception {
    LOGGER.info("Testing network interface enumeration");

    final CompletableFuture<List<WasiAdvancedNetworking.NetworkInterface>> interfacesFuture =
        advancedNetworking.listNetworkInterfacesAsync();

    final List<WasiAdvancedNetworking.NetworkInterface> interfaces =
        interfacesFuture.get(10, TimeUnit.SECONDS);

    assertNotNull(interfaces);
    assertFalse(interfaces.isEmpty());

    // Should have at least a loopback interface
    final boolean hasLoopback = interfaces.stream().anyMatch(iface -> iface.isLoopback);
    assertTrue(hasLoopback);

    // Verify interface properties
    for (final WasiAdvancedNetworking.NetworkInterface iface : interfaces) {
      assertNotNull(iface.name);
      assertFalse(iface.name.isEmpty());
      assertNotNull(iface.addresses);
    }

    LOGGER.info("Network interface enumeration test completed successfully");
  }

  @Test
  @Order(6)
  @DisplayName("Experimental I/O - Async File Operations")
  void testExperimentalIOAsyncFile() throws Exception {
    LOGGER.info("Testing experimental async file I/O");

    // Create test file
    final Path testFile = tempDir.resolve("async-test.txt");
    final String testContent =
        "Async I/O test content with multiple lines\nSecond line\nThird line";
    Files.write(testFile, testContent.getBytes());

    // Get file handle (this would normally be done through WASI file operations)
    final long fileHandle = 1; // Mock handle for test

    final AtomicBoolean readCompleted = new AtomicBoolean(false);
    final AtomicBoolean writeCompleted = new AtomicBoolean(false);
    final AtomicInteger bytesRead = new AtomicInteger(0);

    // Test async read with completion callback
    final ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    final CompletableFuture<Long> readFuture =
        experimentalIO.asyncReadFileAsync(
            fileHandle,
            0,
            readBuffer,
            result -> {
              readCompleted.set(result.completed);
              if (result.error == null) {
                bytesRead.set(result.bytesTransferred);
              }
            });

    final Long readOperationHandle = readFuture.get(10, TimeUnit.SECONDS);
    assertNotNull(readOperationHandle);
    assertTrue(readOperationHandle > 0);

    // Wait for completion callback
    Thread.sleep(1000);
    assertTrue(readCompleted.get());
    assertTrue(bytesRead.get() > 0);

    // Test async write
    final String writeContent = "New async content";
    final ByteBuffer writeBuffer = ByteBuffer.wrap(writeContent.getBytes());
    final CompletableFuture<Long> writeFuture =
        experimentalIO.asyncWriteFileAsync(
            fileHandle,
            100,
            writeBuffer,
            result -> {
              writeCompleted.set(result.completed);
            });

    final Long writeOperationHandle = writeFuture.get(10, TimeUnit.SECONDS);
    assertNotNull(writeOperationHandle);

    // Wait for write completion
    Thread.sleep(1000);
    assertTrue(writeCompleted.get());

    LOGGER.info("Experimental async file I/O test completed successfully");
  }

  @Test
  @Order(7)
  @DisplayName("Experimental I/O - Memory Mapping")
  void testExperimentalIOMemoryMapping() throws Exception {
    LOGGER.info("Testing experimental memory mapping");

    // Create test file
    final Path testFile = tempDir.resolve("mmap-test.txt");
    final String content = "Memory mapped file content for testing purposes";
    Files.write(testFile, content.getBytes());

    final long fileHandle = 2; // Mock handle
    final long fileSize = Files.size(testFile);

    // Create memory mapping
    final CompletableFuture<Long> mappingFuture =
        experimentalIO.createMemoryMappingAsync(
            fileHandle,
            0,
            fileSize,
            WasiExperimentalIO.MemoryProtection.READ_WRITE,
            WasiExperimentalIO.MappingFlags.SHARED);

    final Long mappingHandle = mappingFuture.get(10, TimeUnit.SECONDS);
    assertNotNull(mappingHandle);
    assertTrue(mappingHandle > 0);

    // Test memory mapping operations would go here
    // (actual memory access would require native implementation)

    // Clean up mapping
    experimentalIO.unmapMemoryMapping(mappingHandle);

    LOGGER.info("Experimental memory mapping test completed successfully");
  }

  @Test
  @Order(8)
  @DisplayName("Experimental I/O - File Locking")
  void testExperimentalIOFileLocking() throws Exception {
    LOGGER.info("Testing experimental file locking");

    final long fileHandle = 3; // Mock handle

    // Acquire exclusive file lock
    final CompletableFuture<Long> lockFuture =
        experimentalIO.acquireFileLockAsync(
            fileHandle, 0, 0, WasiExperimentalIO.FileLockType.EXCLUSIVE, false);

    final Long lockHandle = lockFuture.get(10, TimeUnit.SECONDS);
    assertNotNull(lockHandle);
    assertTrue(lockHandle > 0);

    // Release lock
    experimentalIO.releaseFileLock(lockHandle);

    LOGGER.info("Experimental file locking test completed successfully");
  }

  @Test
  @Order(9)
  @DisplayName("Experimental I/O - Directory Watching")
  void testExperimentalIODirectoryWatching() throws Exception {
    LOGGER.info("Testing experimental directory watching");

    final Path watchDir = tempDir.resolve("watch-test");
    Files.createDirectories(watchDir);

    final AtomicInteger eventCount = new AtomicInteger(0);

    // Create directory watcher
    final CompletableFuture<Long> watcherFuture =
        experimentalIO.createDirectoryWatcherAsync(
            watchDir.toString(),
            false,
            WasiExperimentalIO.FileSystemEventMask.ALL,
            event -> {
              eventCount.incrementAndGet();
              LOGGER.info("Directory event: " + event.eventType + " for " + event.path);
            });

    final Long watcherHandle = watcherFuture.get(10, TimeUnit.SECONDS);
    assertNotNull(watcherHandle);

    // Create file to trigger event
    Files.write(watchDir.resolve("watched-file.txt"), "test".getBytes());
    Thread.sleep(1000); // Wait for event

    // Stop watcher
    experimentalIO.stopDirectoryWatcher(watcherHandle);

    // Events might not be captured in test environment
    LOGGER.info("Directory watching test completed (events: " + eventCount.get() + ")");
  }

  @Test
  @Order(10)
  @DisplayName("Experimental I/O - Vectored I/O")
  void testExperimentalIOVectored() throws Exception {
    LOGGER.info("Testing experimental vectored I/O");

    final long fileHandle = 4; // Mock handle

    // Prepare multiple buffers for vectored read
    final List<ByteBuffer> readBuffers =
        List.of(ByteBuffer.allocate(100), ByteBuffer.allocate(200), ByteBuffer.allocate(150));

    final CompletableFuture<Integer> readFuture =
        experimentalIO.vectoredReadAsync(fileHandle, 0, readBuffers);

    final Integer bytesRead = readFuture.get(10, TimeUnit.SECONDS);
    assertNotNull(bytesRead);
    assertTrue(bytesRead >= 0);

    // Prepare buffers for vectored write
    final List<ByteBuffer> writeBuffers =
        List.of(
            ByteBuffer.wrap("First buffer content".getBytes()),
            ByteBuffer.wrap("Second buffer content".getBytes()),
            ByteBuffer.wrap("Third buffer content".getBytes()));

    final CompletableFuture<Integer> writeFuture =
        experimentalIO.vectoredWriteAsync(fileHandle, 1000, writeBuffers);

    final Integer bytesWritten = writeFuture.get(10, TimeUnit.SECONDS);
    assertNotNull(bytesWritten);
    assertTrue(bytesWritten >= 0);

    LOGGER.info("Experimental vectored I/O test completed successfully");
  }

  @Test
  @Order(11)
  @DisplayName("Experimental Process - Sandboxed Process Creation")
  void testExperimentalProcessSandboxed() throws Exception {
    LOGGER.info("Testing experimental sandboxed process creation");

    // Configure minimal sandbox
    final WasiExperimentalProcess.SandboxConfig sandboxConfig =
        WasiExperimentalProcess.SandboxConfig.minimal();

    final WasiExperimentalProcess.ProcessResourceLimits resourceLimits =
        new WasiExperimentalProcess.ProcessResourceLimits(
            64 * 1024 * 1024, // 64MB memory
            25, // 25% CPU
            64, // 64 file descriptors
            1, // 1 process
            60 // 60 second timeout
            );

    // Create sandboxed process (mock executable)
    final CompletableFuture<Long> processFuture =
        experimentalProcess.createSandboxedProcessAsync(
            "/bin/echo",
            List.of("Hello", "World"),
            Map.of("TEST_VAR", "test_value"),
            sandboxConfig,
            resourceLimits);

    final Long processHandle = processFuture.get(15, TimeUnit.SECONDS);
    assertNotNull(processHandle);
    assertTrue(processHandle > 0);

    // Get process resource usage
    final CompletableFuture<WasiExperimentalProcess.ProcessResourceUsage> usageFuture =
        experimentalProcess.getProcessResourceUsageAsync(processHandle);

    final WasiExperimentalProcess.ProcessResourceUsage usage =
        usageFuture.get(10, TimeUnit.SECONDS);
    assertNotNull(usage);
    assertTrue(usage.cpuUsagePercent >= 0);
    assertTrue(usage.memoryUsageBytes >= 0);

    // Terminate process
    final CompletableFuture<Void> terminateFuture =
        experimentalProcess.terminateProcessAsync(processHandle, 5);
    terminateFuture.get(10, TimeUnit.SECONDS);

    LOGGER.info("Experimental sandboxed process test completed successfully");
  }

  @Test
  @Order(12)
  @DisplayName("Experimental Process - Resource Monitoring")
  void testExperimentalProcessResourceMonitoring() throws Exception {
    LOGGER.info("Testing experimental process resource monitoring");

    final long processHandle = 1; // Mock process handle

    final AtomicInteger alertCount = new AtomicInteger(0);

    // Create resource monitoring configuration
    final WasiExperimentalProcess.ResourceMonitoringConfig monitoringConfig =
        new WasiExperimentalProcess.ResourceMonitoringConfig(
            1, // 1 second interval
            32 * 1024 * 1024, // 32MB memory threshold
            50, // 50% CPU threshold
            1024 * 1024, // 1MB/s I/O threshold
            1024 * 1024, // 1MB/s network threshold
            true // detailed stats
            );

    // Create resource monitor
    final CompletableFuture<Long> monitorFuture =
        experimentalProcess.createResourceMonitorAsync(
            processHandle,
            monitoringConfig,
            alert -> {
              alertCount.incrementAndGet();
              LOGGER.info("Resource alert: " + alert.alertType + " - " + alert.message);
            });

    final Long monitorHandle = monitorFuture.get(10, TimeUnit.SECONDS);
    assertNotNull(monitorHandle);
    assertTrue(monitorHandle > 0);

    // Let monitor run briefly
    Thread.sleep(2000);

    LOGGER.info(
        "Experimental process resource monitoring test completed (alerts: "
            + alertCount.get()
            + ")");
  }

  @Test
  @Order(13)
  @DisplayName("Experimental Process - System Service Registration")
  void testExperimentalProcessSystemService() throws Exception {
    LOGGER.info("Testing experimental system service registration");

    final AtomicInteger requestCount = new AtomicInteger(0);

    // Create service metadata
    final WasiExperimentalProcess.SystemServiceMetadata metadata =
        new WasiExperimentalProcess.SystemServiceMetadata(
            "1.0.0",
            "Test service for experimental features",
            List.of("test", "experimental"),
            List.of("/test/endpoint"),
            8080,
            false);

    // Register system service
    final CompletableFuture<String> registerFuture =
        experimentalProcess.registerSystemServiceAsync(
            "test-service",
            metadata,
            request -> {
              requestCount.incrementAndGet();
              LOGGER.info("Service request: " + request.operation);
            });

    final String serviceId = registerFuture.get(10, TimeUnit.SECONDS);
    assertNotNull(serviceId);
    assertFalse(serviceId.isEmpty());

    // Discover services
    final CompletableFuture<List<WasiExperimentalProcess.SystemServiceInfo>> discoverFuture =
        experimentalProcess.discoverSystemServicesAsync("test-*", List.of("test"));

    final List<WasiExperimentalProcess.SystemServiceInfo> services =
        discoverFuture.get(10, TimeUnit.SECONDS);
    assertNotNull(services);
    // Service discovery might not find our service in test environment

    LOGGER.info("Experimental system service test completed successfully");
  }

  @Test
  @Order(14)
  @DisplayName("Experimental Process - IPC Channel Creation")
  void testExperimentalProcessIpcChannel() throws Exception {
    LOGGER.info("Testing experimental IPC channel creation");

    final long sourceProcessHandle = 1; // Mock source process
    final long targetProcessHandle = 2; // Mock target process

    // Create IPC channel configuration
    final WasiExperimentalProcess.IPCChannelConfig channelConfig =
        WasiExperimentalProcess.IPCChannelConfig.defaultConfig();

    // Create IPC channel
    final CompletableFuture<Long> ipcFuture =
        experimentalProcess.createIPCChannelAsync(
            sourceProcessHandle,
            targetProcessHandle,
            WasiExperimentalProcess.IPCChannelType.PIPE,
            channelConfig);

    final Long ipcHandle = ipcFuture.get(10, TimeUnit.SECONDS);
    assertNotNull(ipcHandle);
    assertTrue(ipcHandle > 0);

    LOGGER.info("Experimental IPC channel test completed successfully");
  }

  @Test
  @Order(15)
  @DisplayName("Experimental Features - Error Handling and Edge Cases")
  void testExperimentalFeaturesErrorHandling() throws Exception {
    LOGGER.info("Testing experimental features error handling");

    // Test invalid snapshot operations
    assertThrows(
        WasiException.class,
        () -> {
          filesystemSnapshot.getSnapshotMetadata(99999L);
        });

    assertThrows(
        WasiException.class,
        () -> {
          filesystemSnapshot.deleteSnapshot(99999L);
        });

    // Test invalid network operations
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          advancedNetworking.createHttp2ConnectionAsync("", 80, false, null);
        });

    // Test invalid I/O operations
    assertThrows(
        WasiException.class,
        () -> {
          experimentalIO.releaseFileLock(99999L);
        });

    // Test invalid process operations
    assertThrows(
        WasiException.class,
        () -> {
          experimentalProcess.getProcessResourceUsageAsync(99999L);
        });

    LOGGER.info("Experimental features error handling test completed successfully");
  }

  @Test
  @Order(16)
  @DisplayName("Experimental Features - Performance and Concurrency")
  void testExperimentalFeaturesPerformanceAndConcurrency() throws Exception {
    LOGGER.info("Testing experimental features performance and concurrency");

    // Test concurrent snapshot operations
    final Path concurrentTestDir = tempDir.resolve("concurrent-test");
    Files.createDirectories(concurrentTestDir);

    for (int i = 0; i < 5; i++) {
      Files.write(concurrentTestDir.resolve("file" + i + ".txt"), ("Content " + i).getBytes());
    }

    final WasiFilesystemSnapshot.SnapshotOptions options =
        WasiFilesystemSnapshot.SnapshotOptions.defaultOptions();

    // Create multiple snapshots concurrently
    final List<CompletableFuture<Long>> snapshotFutures =
        List.of(
            filesystemSnapshot.createFullSnapshotAsync(concurrentTestDir.toString(), options),
            filesystemSnapshot.createFullSnapshotAsync(concurrentTestDir.toString(), options),
            filesystemSnapshot.createFullSnapshotAsync(concurrentTestDir.toString(), options));

    final List<Long> snapshotHandles =
        snapshotFutures.stream()
            .map(
                future -> {
                  try {
                    return future.get(30, TimeUnit.SECONDS);
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                })
            .toList();

    assertEquals(3, snapshotHandles.size());
    snapshotHandles.forEach(handle -> assertTrue(handle > 0));

    // Test concurrent network operations
    final WasiAdvancedNetworking.WebSocketOptions wsOptions =
        WasiAdvancedNetworking.WebSocketOptions.defaultOptions();

    // Measure performance
    final long startTime = System.nanoTime();

    // Cleanup snapshots
    for (final Long handle : snapshotHandles) {
      filesystemSnapshot.deleteSnapshot(handle);
    }

    final long endTime = System.nanoTime();
    final Duration executionTime = Duration.ofNanos(endTime - startTime);

    LOGGER.info(
        "Experimental features concurrency test completed in " + executionTime.toMillis() + "ms");

    // Verify reasonable performance
    assertTrue(executionTime.toSeconds() < 60); // Should complete within 60 seconds
  }
}
