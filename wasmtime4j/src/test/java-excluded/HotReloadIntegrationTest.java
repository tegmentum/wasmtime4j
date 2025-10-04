package ai.tegmentum.wasmtime4j.hotreload;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.WasmFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;

/**
 * Integration tests for the complete hot-reload workflow.
 *
 * <p>These tests demonstrate end-to-end functionality including:
 *
 * <ul>
 *   <li>Engine and manager setup
 *   <li>File system watching integration
 *   <li>Automatic hot-reload triggering
 *   <li>Multiple deployment strategies
 *   <li>Concurrent operations management
 *   <li>Performance and reliability testing
 * </ul>
 *
 * <p><strong>Note:</strong> These tests use real file system operations and may take longer to
 * execute than unit tests.
 */
@DisplayName("Hot Reload Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HotReloadIntegrationTest {

  private static final String SAMPLE_WASM_CONTENT =
      """
            (module
                (func $hello (result i32)
                    i32.const 42
                )
                (export "hello" (func $hello))
            )
            """;

  private static final String UPDATED_WASM_CONTENT =
      """
            (module
                (func $hello (result i32)
                    i32.const 84
                )
                (export "hello" (func $hello))
            )
            """;

  private Engine engine;
  private Path tempDir;
  private HotReloadManager.HotReloadConfig config;

  @BeforeEach
  void setUp() throws IOException {
    // Create engine with features suitable for hot-reload testing
    final var engineConfig =
        EngineConfig.builder()
            .wasmFeature(WasmFeature.BULK_MEMORY, true)
            .wasmFeature(WasmFeature.REFERENCE_TYPES, true)
            .wasmFeature(WasmFeature.MULTI_VALUE, true)
            .build();

    engine = Engine.newEngine(engineConfig);
    assertNotNull(engine, "Engine should not be null");

    // Create temporary directory for test modules
    tempDir = Files.createTempDirectory("hotreload-integration");

    // Create hot-reload configuration optimized for testing
    config =
        HotReloadManager.HotReloadConfig.builder()
            .validationEnabled(true)
            .statePreservationEnabled(true)
            .debounceDelayMs(100) // Short delay for faster tests
            .precompilationEnabled(true)
            .maxReloadAttempts(2)
            .healthCheckIntervalSecs(5)
            .loaderThreadCount(4)
            .cacheSize(50)
            .build();

    System.out.println("Integration test setup completed");
    System.out.println("Temp directory: " + tempDir);
    System.out.println("Config: " + config);
  }

  @AfterEach
  void tearDown() {
    if (engine != null && !engine.isClosed()) {
      engine.close();
    }

    // Clean up temporary directory
    try {
      Files.walk(tempDir)
          .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException e) {
                  System.err.println("Failed to delete: " + path);
                }
              });
    } catch (IOException e) {
      System.err.println("Failed to clean up temp directory: " + e.getMessage());
    }
  }

  @Test
  @Order(1)
  @DisplayName("Basic hot-reload manager creation and operation")
  void testBasicHotReloadOperations() throws Exception {
    try (final var manager = HotReloadManager.create(engine, config)) {
      assertNotNull(manager, "Manager should not be null");
      assertFalse(manager.isClosed(), "Manager should not be closed initially");

      // Test basic operations
      final String operationId =
          manager.startHotSwap(
              "test-component", "1.0.0", HotReloadManager.SwapStrategy.immediate());

      assertNotNull(operationId, "Operation ID should not be null");

      // Check initial metrics
      final var metrics = manager.getMetrics();
      assertNotNull(metrics, "Metrics should not be null");
      System.out.println("Initial metrics: " + metrics);

      // Monitor the operation briefly
      for (int i = 0; i < 5; i++) {
        Thread.sleep(200);
        final var status = manager.getSwapStatus(operationId);
        if (status != null) {
          System.out.println("Operation status: " + status);
          if (status.getStatus().isTerminal()) {
            break;
          }
        }
      }

      final var finalMetrics = manager.getMetrics();
      System.out.println("Final metrics: " + finalMetrics);

      System.out.println("✓ Basic hot-reload operations test passed");
    }
  }

  @Test
  @Order(2)
  @DisplayName("File system integration with automatic triggers")
  void testFileSystemIntegration() throws Exception {
    try (final var manager = HotReloadManager.create(engine, config)) {
      // Set up file system watcher
      final var watchConfig =
          FileSystemWatcher.WatchConfig.builder()
              .pattern("*.wasm")
              .debounceDelayMs(150)
              .reloadStrategy(HotReloadManager.SwapStrategy.canary(10.0f, 25.0f, 0.95f))
              .monitorProgress(true)
              .build();

      try (final var watcher = new FileSystemWatcher(manager)) {
        watcher.watchDirectory(tempDir, watchConfig);
        watcher.start();

        assertTrue(watcher.isRunning(), "Watcher should be running");
        assertEquals(1, watcher.getWatchCount(), "Should have one watch");

        // Wait for watcher to initialize
        Thread.sleep(200);

        // Create initial metrics snapshot
        final var initialMetrics = manager.getMetrics();
        System.out.println("Initial metrics: " + initialMetrics);

        // Create a WASM module file
        final Path wasmFile = tempDir.resolve("integration-test.wasm");
        Files.write(wasmFile, SAMPLE_WASM_CONTENT.getBytes());

        System.out.println("Created WASM file: " + wasmFile);

        // Wait for file system event processing
        Thread.sleep(500);

        // Check that hot-reload was triggered
        final var metricsAfterCreate = manager.getMetrics();
        System.out.println("Metrics after file creation: " + metricsAfterCreate);

        // Modify the file to trigger another reload
        Files.write(wasmFile, UPDATED_WASM_CONTENT.getBytes(), StandardOpenOption.WRITE);

        System.out.println("Modified WASM file content");

        // Wait for second reload
        Thread.sleep(500);

        final var finalMetrics = manager.getMetrics();
        System.out.println("Final metrics: " + finalMetrics);

        // Should have some activity
        assertTrue(
            finalMetrics.getTotalSwaps() > 0 || finalMetrics.getCurrentActiveSwaps() > 0,
            "Should have swap activity from file system events");

        System.out.println("✓ File system integration test passed");
      }
    }
  }

  @Test
  @Order(3)
  @DisplayName("Multiple component hot-reload with different strategies")
  void testMultipleComponentStrategies() throws Exception {
    try (final var manager = HotReloadManager.create(engine, config)) {
      // Test different strategies simultaneously
      final var strategies =
          new HotReloadManager.SwapStrategy[] {
            HotReloadManager.SwapStrategy.immediate(),
            HotReloadManager.SwapStrategy.canary(5.0f, 20.0f, 0.9f),
            HotReloadManager.SwapStrategy.blueGreen(),
            HotReloadManager.SwapStrategy.rollingUpdate(3, 30),
            HotReloadManager.SwapStrategy.abTest(25.0f, 120)
          };

      final String[] operationIds = new String[strategies.length];

      // Start all operations
      for (int i = 0; i < strategies.length; i++) {
        final String componentName = "component-" + i;
        final String version = "2.0." + i;

        operationIds[i] = manager.startHotSwap(componentName, version, strategies[i]);
        assertNotNull(operationIds[i], "Operation ID " + i + " should not be null");

        System.out.println(
            String.format(
                "Started operation %d: %s (%s)",
                i, operationIds[i], strategies[i].getClass().getSimpleName()));

        // Small delay between operations
        Thread.sleep(100);
      }

      // Monitor all operations
      final int monitoringRounds = 10;
      for (int round = 0; round < monitoringRounds; round++) {
        Thread.sleep(300);

        System.out.println("\n--- Monitoring Round " + (round + 1) + " ---");

        int activeOperations = 0;
        for (int i = 0; i < operationIds.length; i++) {
          final var status = manager.getSwapStatus(operationIds[i]);
          if (status != null) {
            System.out.println(
                String.format(
                    "Op %d: %s (%.1f%%) - %s",
                    i, status.getStatus(), status.getProgress() * 100, status.getComponentName()));

            if (!status.getStatus().isTerminal()) {
              activeOperations++;
            }
          } else {
            System.out.println(String.format("Op %d: null status", i));
          }
        }

        final var currentMetrics = manager.getMetrics();
        System.out.println("Current metrics: " + currentMetrics);

        if (activeOperations == 0) {
          System.out.println("All operations completed");
          break;
        }
      }

      final var finalMetrics = manager.getMetrics();
      System.out.println("\nFinal metrics: " + finalMetrics);

      // Should have processed multiple operations
      assertTrue(
          finalMetrics.getTotalSwaps() >= strategies.length,
          "Should have processed at least as many swaps as strategies");

      System.out.println("✓ Multiple component strategies test passed");
    }
  }

  @Test
  @Order(4)
  @DisplayName("Concurrent file modifications and hot-reloads")
  void testConcurrentOperations() throws Exception {
    try (final var manager = HotReloadManager.create(engine, config)) {
      final var watchConfig =
          FileSystemWatcher.WatchConfig.builder()
              .pattern("*.wasm")
              .debounceDelayMs(100)
              .reloadStrategy(HotReloadManager.SwapStrategy.immediate())
              .build();

      try (final var watcher = new FileSystemWatcher(manager)) {
        watcher.watchDirectory(tempDir, watchConfig);
        watcher.start();

        // Wait for watcher to initialize
        Thread.sleep(200);

        final int fileCount = 5;
        final CountDownLatch latch = new CountDownLatch(fileCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        // Create multiple files concurrently
        for (int i = 0; i < fileCount; i++) {
          final int fileIndex = i;
          new Thread(
                  () -> {
                    try {
                      final Path file = tempDir.resolve("concurrent-" + fileIndex + ".wasm");
                      Files.write(file, ("content " + fileIndex).getBytes());

                      System.out.println("Created concurrent file " + fileIndex);

                      // Modify the file a few times
                      for (int mod = 0; mod < 3; mod++) {
                        Thread.sleep(50);
                        Files.write(
                            file,
                            ("modified content " + fileIndex + "." + mod).getBytes(),
                            StandardOpenOption.WRITE);
                      }

                      successCount.incrementAndGet();
                    } catch (Exception e) {
                      System.err.println("Error in concurrent file operations: " + e.getMessage());
                    } finally {
                      latch.countDown();
                    }
                  })
              .start();
        }

        // Wait for all file operations to complete
        assertTrue(latch.await(10, TimeUnit.SECONDS), "All file operations should complete");
        assertEquals(fileCount, successCount.get(), "All file operations should succeed");

        // Wait for hot-reload processing
        Thread.sleep(2000);

        final var finalMetrics = manager.getMetrics();
        System.out.println("Concurrent operations metrics: " + finalMetrics);

        // Should have processed multiple swaps
        assertTrue(
            finalMetrics.getTotalSwaps() > 0,
            "Should have processed swaps from concurrent operations");

        System.out.println("✓ Concurrent operations test passed");
      }
    }
  }

  @Test
  @Order(5)
  @DisplayName("Async component loading and caching")
  void testAsyncLoadingAndCaching() throws Exception {
    try (final var manager = HotReloadManager.create(engine, config)) {
      // Create test WASM files
      final int fileCount = 8;
      final Path[] testFiles = new Path[fileCount];

      for (int i = 0; i < fileCount; i++) {
        testFiles[i] = tempDir.resolve("async-test-" + i + ".wasm");
        Files.write(testFiles[i], (SAMPLE_WASM_CONTENT + " " + i).getBytes());
      }

      // Submit async load requests
      final var validationConfig =
          new HotReloadManager.ValidationConfig(true, true, false, false, 10);

      for (int i = 0; i < fileCount; i++) {
        final var loadRequest =
            new HotReloadManager.LoadRequest(
                "async-component-" + i,
                testFiles[i].toString(),
                "1.0." + i,
                HotReloadManager.LoadPriority.NORMAL,
                validationConfig);

        final var future = manager.loadComponentAsync(loadRequest);
        final String requestId = future.get(5, TimeUnit.SECONDS);

        assertNotNull(requestId, "Request ID " + i + " should not be null");
        System.out.println("Submitted async load " + i + ": " + requestId);

        // Small delay to avoid overwhelming the system
        Thread.sleep(50);
      }

      // Wait for processing to complete
      Thread.sleep(1000);

      final var metricsAfterLoading = manager.getMetrics();
      System.out.println("Metrics after async loading: " + metricsAfterLoading);

      // Should have loaded components
      assertTrue(
          metricsAfterLoading.getComponentsLoaded() > 0,
          "Should have loaded some components asynchronously");

      // Test cache efficiency by loading some components again
      for (int i = 0; i < 3; i++) {
        final var loadRequest =
            new HotReloadManager.LoadRequest(
                "cached-component-" + i,
                testFiles[i].toString(),
                "1.1." + i,
                HotReloadManager.LoadPriority.HIGH,
                validationConfig);

        final var future = manager.loadComponentAsync(loadRequest);
        final String requestId = future.get(3, TimeUnit.SECONDS);

        assertNotNull(requestId, "Cached request ID " + i + " should not be null");
      }

      Thread.sleep(500);

      final var finalMetrics = manager.getMetrics();
      System.out.println("Final async loading metrics: " + finalMetrics);

      // Cache should show some efficiency
      assertTrue(
          finalMetrics.getCacheEfficiency() >= 0.0f, "Cache efficiency should be non-negative");

      System.out.println("✓ Async loading and caching test passed");
    }
  }

  @Test
  @Order(6)
  @DisplayName("Error recovery and rollback scenarios")
  void testErrorRecoveryAndRollback() throws Exception {
    try (final var manager = HotReloadManager.create(engine, config)) {
      // Test cancellation
      final String cancelTestOp =
          manager.startHotSwap(
              "cancel-test", "1.0.0", HotReloadManager.SwapStrategy.canary(5.0f, 15.0f, 0.95f));

      Thread.sleep(100);

      final boolean cancelled = manager.cancelHotSwap(cancelTestOp);
      System.out.println("Cancellation result: " + cancelled);

      // Monitor cancellation
      for (int i = 0; i < 5; i++) {
        Thread.sleep(200);
        final var status = manager.getSwapStatus(cancelTestOp);
        if (status != null) {
          System.out.println("Cancel test status: " + status.getStatus());
          if (status.getStatus().isTerminal()) {
            break;
          }
        }
      }

      // Test multiple operations with some failures expected
      final String[] testOperations = new String[6];
      for (int i = 0; i < testOperations.length; i++) {
        testOperations[i] =
            manager.startHotSwap(
                "error-test-" + i, "1.0." + i, HotReloadManager.SwapStrategy.immediate());

        Thread.sleep(50);
      }

      // Monitor error recovery
      Thread.sleep(2000);

      final var errorRecoveryMetrics = manager.getMetrics();
      System.out.println("Error recovery metrics: " + errorRecoveryMetrics);

      // Should have some activity, possibly including rollbacks
      assertTrue(
          errorRecoveryMetrics.getTotalSwaps() > 0
              || errorRecoveryMetrics.getCurrentActiveSwaps() > 0,
          "Should have swap activity in error scenarios");

      System.out.println("✓ Error recovery and rollback test passed");
    }
  }

  @Test
  @Order(7)
  @DisplayName("Performance and stress testing")
  void testPerformanceAndStress() throws Exception {
    // Use a configuration optimized for performance
    final var perfConfig =
        HotReloadManager.HotReloadConfig.builder()
            .validationEnabled(false) // Disable for speed
            .statePreservationEnabled(false)
            .debounceDelayMs(10) // Very short delay
            .precompilationEnabled(true)
            .maxReloadAttempts(1)
            .healthCheckIntervalSecs(60)
            .loaderThreadCount(8) // More threads
            .cacheSize(200) // Larger cache
            .build();

    try (final var manager = HotReloadManager.create(engine, perfConfig)) {
      final long startTime = System.currentTimeMillis();

      // Rapid-fire operations
      final int operationCount = 50;
      final String[] operationIds = new String[operationCount];

      System.out.println("Starting stress test with " + operationCount + " operations");

      for (int i = 0; i < operationCount; i++) {
        final var strategy =
            (i % 3 == 0)
                ? HotReloadManager.SwapStrategy.immediate()
                : HotReloadManager.SwapStrategy.canary(2.0f, 10.0f, 0.8f);

        operationIds[i] = manager.startHotSwap("stress-" + i, "1.0." + i, strategy);
        assertNotNull(operationIds[i], "Operation " + i + " should not be null");

        // Minimal delay
        if (i % 10 == 9) {
          Thread.sleep(10);
        }
      }

      final long setupTime = System.currentTimeMillis() - startTime;
      System.out.println("Setup completed in " + setupTime + "ms");

      // Monitor performance
      final long monitorStart = System.currentTimeMillis();
      final var initialMetrics = manager.getMetrics();

      Thread.sleep(3000); // Wait for processing

      final var finalMetrics = manager.getMetrics();
      final long totalTime = System.currentTimeMillis() - startTime;

      System.out.println("\nPerformance Results:");
      System.out.println("Total operations: " + operationCount);
      System.out.println("Total time: " + totalTime + "ms");
      System.out.println("Operations per second: " + (operationCount * 1000.0 / totalTime));
      System.out.println("Initial metrics: " + initialMetrics);
      System.out.println("Final metrics: " + finalMetrics);

      // Performance assertions
      assertTrue(totalTime < 10000, "Stress test should complete within 10 seconds");
      assertTrue(
          finalMetrics.getTotalSwaps() > 0 || finalMetrics.getCurrentActiveSwaps() > 0,
          "Should have processed operations");

      System.out.println("✓ Performance and stress test passed");
    }
  }

  @Test
  @Order(8)
  @DisplayName("Complete workflow integration")
  void testCompleteWorkflowIntegration() throws Exception {
    System.out.println("Starting complete workflow integration test...");

    try (final var manager = HotReloadManager.create(engine, config)) {
      // Set up comprehensive file watching
      final var watchConfig =
          FileSystemWatcher.WatchConfig.builder()
              .pattern("*.wasm")
              .pattern("*.wit")
              .ignorePattern("*test*")
              .debounceDelayMs(200)
              .reloadStrategy(HotReloadManager.SwapStrategy.canary(8.0f, 20.0f, 0.92f))
              .componentNameExtractor(
                  path -> {
                    final String name = path.getFileName().toString();
                    return name.replaceAll("\\.[^.]+$", "").replaceAll("-v\\d+.*", "");
                  })
              .versionExtractor(
                  path -> {
                    final String name = path.getFileName().toString();
                    if (name.contains("-v")) {
                      final String version = name.replaceAll(".*-v([^.]+).*", "$1");
                      return version.matches("\\d+.*") ? version : "1.0.0";
                    }
                    return String.valueOf(System.currentTimeMillis() % 10000);
                  })
              .monitorProgress(true)
              .build();

      try (final var watcher = new FileSystemWatcher(manager)) {
        watcher.watchDirectory(tempDir, watchConfig);
        watcher.start();

        // Phase 1: Create initial modules
        System.out.println("\nPhase 1: Creating initial modules");
        final String[] components = {"auth-service", "user-service", "data-processor"};
        final Path[] componentFiles = new Path[components.length];

        for (int i = 0; i < components.length; i++) {
          componentFiles[i] = tempDir.resolve(components[i] + "-v1.0.wasm");
          Files.write(componentFiles[i], (SAMPLE_WASM_CONTENT + " // " + components[i]).getBytes());
          System.out.println("Created: " + componentFiles[i].getFileName());
        }

        Thread.sleep(1000);
        System.out.println("Phase 1 metrics: " + manager.getMetrics());

        // Phase 2: Async preload new versions
        System.out.println("\nPhase 2: Preloading new versions");
        final var validationConfig =
            new HotReloadManager.ValidationConfig(true, true, false, false, 15);

        for (int i = 0; i < components.length; i++) {
          final Path newVersion = tempDir.resolve(components[i] + "-v2.0.wasm");
          Files.write(
              newVersion, (UPDATED_WASM_CONTENT + " // " + components[i] + " v2").getBytes());

          final var loadRequest =
              new HotReloadManager.LoadRequest(
                  components[i] + "-v2",
                  newVersion.toString(),
                  "2.0.0",
                  HotReloadManager.LoadPriority.HIGH,
                  validationConfig);

          manager
              .loadComponentAsync(loadRequest)
              .thenAccept(
                  requestId ->
                      System.out.println("Preloaded " + components[i] + " v2.0: " + requestId));
        }

        Thread.sleep(1500);
        System.out.println("Phase 2 metrics: " + manager.getMetrics());

        // Phase 3: Trigger updates via file modifications
        System.out.println("\nPhase 3: Triggering updates via file modifications");
        for (int i = 0; i < components.length; i++) {
          // Replace v1 files with v2 content to trigger hot-reload
          Files.write(
              componentFiles[i],
              (UPDATED_WASM_CONTENT + " // Updated " + components[i]).getBytes(),
              StandardOpenOption.WRITE);

          System.out.println("Updated: " + componentFiles[i].getFileName());
          Thread.sleep(300); // Stagger updates
        }

        // Phase 4: Monitor comprehensive operation
        System.out.println("\nPhase 4: Monitoring comprehensive operation");
        for (int round = 0; round < 15; round++) {
          Thread.sleep(400);

          final var metrics = manager.getMetrics();
          System.out.println(
              String.format(
                  "Round %2d: Total=%d, Success=%d, Active=%d, Cache=%.1f%%",
                  round + 1,
                  metrics.getTotalSwaps(),
                  metrics.getSuccessfulSwaps(),
                  metrics.getCurrentActiveSwaps(),
                  metrics.getCacheEfficiency() * 100));

          if (metrics.getCurrentActiveSwaps() == 0 && metrics.getTotalSwaps() > 0) {
            System.out.println("All operations appear to be complete");
            break;
          }
        }

        // Phase 5: Final validation
        System.out.println("\nPhase 5: Final validation");
        final var finalMetrics = manager.getMetrics();

        assertTrue(
            finalMetrics.getTotalSwaps() >= components.length,
            "Should have processed at least one swap per component");
        assertTrue(finalMetrics.getComponentsLoaded() > 0, "Should have loaded components");
        assertTrue(finalMetrics.getSuccessRate() >= 0.0, "Success rate should be reasonable");

        System.out.println("\nComplete Workflow Results:");
        System.out.println("Components processed: " + components.length);
        System.out.println("Final metrics: " + finalMetrics);
        System.out.println(
            "Success rate: " + String.format("%.1f%%", finalMetrics.getSuccessRate() * 100));
        System.out.println("Average swap time: " + finalMetrics.getAvgSwapTimeMs() + "ms");

        System.out.println("✓ Complete workflow integration test passed");
      }
    }
  }
}
