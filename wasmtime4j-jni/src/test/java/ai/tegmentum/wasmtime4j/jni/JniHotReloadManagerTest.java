package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for JNI hot reload manager functionality.
 *
 * <p>These tests verify all aspects of hot-reload operations including:
 * <ul>
 *   <li>Manager creation and configuration</li>
 *   <li>Hot swap operations with different strategies</li>
 *   <li>Asynchronous component loading</li>
 *   <li>Status monitoring and progress tracking</li>
 *   <li>Metrics collection and reporting</li>
 *   <li>Error handling and recovery</li>
 *   <li>Resource management and cleanup</li>
 * </ul>
 */
@DisplayName("JNI Hot Reload Manager Tests")
@EnabledOnJre({JRE.JAVA_8, JRE.JAVA_11, JRE.JAVA_17, JRE.JAVA_21})
class JniHotReloadManagerTest {

    private static final String TEST_COMPONENT_NAME = "test-component";
    private static final String TEST_VERSION_1 = "1.0.0";
    private static final String TEST_VERSION_2 = "2.0.0";

    private JniEngine engine;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create engine with default configuration
        engine = JniEngine.newEngine();
        assertNotNull(engine, "Engine should not be null");

        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("hotreload-test");

        System.out.println("Test setup completed with temp dir: " + tempDir);
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
                    .forEach(path -> {
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
    @DisplayName("Create hot reload manager with default configuration")
    void testCreateManagerWithDefaultConfig() {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            assertNotNull(manager, "Manager should not be null");
            assertFalse(manager.isClosed(), "Manager should not be closed initially");
            assertTrue(manager.getNativeHandle() != 0, "Manager should have valid native handle");

            System.out.println("Created manager: " + manager);
        }
    }

    @Test
    @DisplayName("Create hot reload manager with custom configuration")
    void testCreateManagerWithCustomConfig() {
        final var config = JniHotReloadManager.HotReloadConfig.builder()
                .validationEnabled(false)
                .statePreservationEnabled(false)
                .debounceDelayMs(200)
                .precompilationEnabled(false)
                .maxReloadAttempts(5)
                .healthCheckIntervalSecs(60)
                .loaderThreadCount(2)
                .cacheSize(50)
                .build();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            assertNotNull(manager, "Manager should not be null");
            assertFalse(manager.isClosed(), "Manager should not be closed initially");

            System.out.println("Created manager with custom config: " + manager);
        }
    }

    @Test
    @DisplayName("Create manager with null engine should throw exception")
    void testCreateManagerWithNullEngine() {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        final var exception = assertThrows(IllegalArgumentException.class, () ->
                new JniHotReloadManager(null, config));

        assertEquals("Engine cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Create manager with null config should throw exception")
    void testCreateManagerWithNullConfig() {
        final var exception = assertThrows(IllegalArgumentException.class, () ->
                new JniHotReloadManager(engine, null));

        assertEquals("Config cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Start hot swap with immediate strategy")
    void testStartHotSwapImmediate() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final var strategy = JniHotReloadManager.SwapStrategy.immediate();

            final String operationId = manager.startHotSwap(TEST_COMPONENT_NAME, TEST_VERSION_2, strategy);

            assertNotNull(operationId, "Operation ID should not be null");
            assertFalse(operationId.trim().isEmpty(), "Operation ID should not be empty");

            System.out.println("Started hot swap with operation ID: " + operationId);

            // Give the operation a moment to process
            Thread.sleep(100);

            // Check initial status
            final var status = manager.getSwapStatus(operationId);
            assertNotNull(status, "Status should not be null");
            assertEquals(operationId, status.getOperationId());
            assertEquals(TEST_COMPONENT_NAME, status.getComponentName());

            System.out.println("Hot swap status: " + status);
        }
    }

    @Test
    @DisplayName("Start hot swap with canary strategy")
    void testStartHotSwapCanary() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final var strategy = JniHotReloadManager.SwapStrategy.canary(5.0f, 20.0f, 0.95f);

            final String operationId = manager.startHotSwap(TEST_COMPONENT_NAME, TEST_VERSION_2, strategy);

            assertNotNull(operationId, "Operation ID should not be null");

            System.out.println("Started canary hot swap: " + operationId);

            // Monitor progress briefly
            for (int i = 0; i < 3; i++) {
                Thread.sleep(200);
                final var status = manager.getSwapStatus(operationId);
                if (status != null) {
                    System.out.println("Canary progress: " + status.getProgress() * 100 + "% - " + status.getStatus());
                }
            }
        }
    }

    @Test
    @DisplayName("Start hot swap with blue-green strategy")
    void testStartHotSwapBlueGreen() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final var strategy = JniHotReloadManager.SwapStrategy.blueGreen();

            final String operationId = manager.startHotSwap(TEST_COMPONENT_NAME, TEST_VERSION_2, strategy);

            assertNotNull(operationId, "Operation ID should not be null");

            System.out.println("Started blue-green hot swap: " + operationId);
        }
    }

    @Test
    @DisplayName("Start hot swap with rolling update strategy")
    void testStartHotSwapRollingUpdate() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final var strategy = JniHotReloadManager.SwapStrategy.rollingUpdate(3, 30);

            final String operationId = manager.startHotSwap(TEST_COMPONENT_NAME, TEST_VERSION_2, strategy);

            assertNotNull(operationId, "Operation ID should not be null");

            System.out.println("Started rolling update hot swap: " + operationId);
        }
    }

    @Test
    @DisplayName("Start hot swap with A/B test strategy")
    void testStartHotSwapABTest() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final var strategy = JniHotReloadManager.SwapStrategy.abTest(15.0f, 300);

            final String operationId = manager.startHotSwap(TEST_COMPONENT_NAME, TEST_VERSION_2, strategy);

            assertNotNull(operationId, "Operation ID should not be null");

            System.out.println("Started A/B test hot swap: " + operationId);
        }
    }

    @Test
    @DisplayName("Start hot swap with default strategy when null provided")
    void testStartHotSwapWithNullStrategy() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final String operationId = manager.startHotSwap(TEST_COMPONENT_NAME, TEST_VERSION_2, null);

            assertNotNull(operationId, "Operation ID should not be null");

            System.out.println("Started hot swap with default strategy: " + operationId);
        }
    }

    @Test
    @DisplayName("Start hot swap with invalid component name should throw exception")
    void testStartHotSwapWithInvalidComponentName() {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final var strategy = JniHotReloadManager.SwapStrategy.immediate();

            // Null component name
            assertThrows(IllegalArgumentException.class, () ->
                    manager.startHotSwap(null, TEST_VERSION_2, strategy));

            // Empty component name
            assertThrows(IllegalArgumentException.class, () ->
                    manager.startHotSwap("", TEST_VERSION_2, strategy));

            // Whitespace only component name
            assertThrows(IllegalArgumentException.class, () ->
                    manager.startHotSwap("   ", TEST_VERSION_2, strategy));
        }
    }

    @Test
    @DisplayName("Start hot swap with invalid version should throw exception")
    void testStartHotSwapWithInvalidVersion() {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final var strategy = JniHotReloadManager.SwapStrategy.immediate();

            // Null version
            assertThrows(IllegalArgumentException.class, () ->
                    manager.startHotSwap(TEST_COMPONENT_NAME, null, strategy));

            // Empty version
            assertThrows(IllegalArgumentException.class, () ->
                    manager.startHotSwap(TEST_COMPONENT_NAME, "", strategy));

            // Whitespace only version
            assertThrows(IllegalArgumentException.class, () ->
                    manager.startHotSwap(TEST_COMPONENT_NAME, "   ", strategy));
        }
    }

    @Test
    @DisplayName("Get status of non-existent operation should return null")
    void testGetSwapStatusNonExistent() {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final var status = manager.getSwapStatus("non-existent-operation");

            assertNull(status, "Status should be null for non-existent operation");
        }
    }

    @Test
    @DisplayName("Get status with invalid operation ID should throw exception")
    void testGetSwapStatusWithInvalidId() {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            // Null operation ID
            assertThrows(IllegalArgumentException.class, () ->
                    manager.getSwapStatus(null));

            // Empty operation ID
            assertThrows(IllegalArgumentException.class, () ->
                    manager.getSwapStatus(""));

            // Whitespace only operation ID
            assertThrows(IllegalArgumentException.class, () ->
                    manager.getSwapStatus("   "));
        }
    }

    @Test
    @DisplayName("Cancel hot swap operation")
    void testCancelHotSwap() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final var strategy = JniHotReloadManager.SwapStrategy.canary(10.0f, 25.0f, 0.99f);
            final String operationId = manager.startHotSwap(TEST_COMPONENT_NAME, TEST_VERSION_2, strategy);

            assertNotNull(operationId, "Operation ID should not be null");

            // Give the operation a moment to start
            Thread.sleep(50);

            // Attempt to cancel
            final boolean cancelled = manager.cancelHotSwap(operationId);

            System.out.println("Cancel result: " + cancelled + " for operation: " + operationId);

            // Check status after cancellation attempt
            final var status = manager.getSwapStatus(operationId);
            if (status != null) {
                System.out.println("Status after cancel attempt: " + status.getStatus());
            }
        }
    }

    @Test
    @DisplayName("Cancel non-existent operation should return false")
    void testCancelNonExistentOperation() {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final boolean cancelled = manager.cancelHotSwap("non-existent-operation");

            assertFalse(cancelled, "Cancel should return false for non-existent operation");
        }
    }

    @Test
    @DisplayName("Load component asynchronously")
    void testLoadComponentAsync() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            // Create a test file
            final Path testFile = tempDir.resolve("test-component.wasm");
            Files.write(testFile, "fake wasm content".getBytes());

            final var validationConfig = new JniHotReloadManager.ValidationConfig(
                    true, true, false, false, 30);

            final var loadRequest = new JniHotReloadManager.LoadRequest(
                    TEST_COMPONENT_NAME,
                    testFile.toString(),
                    TEST_VERSION_1,
                    JniHotReloadManager.LoadPriority.NORMAL,
                    validationConfig);

            final CompletableFuture<String> future = manager.loadComponentAsync(loadRequest);

            assertNotNull(future, "Future should not be null");

            // Wait for completion
            final String requestId = future.get(5, TimeUnit.SECONDS);

            assertNotNull(requestId, "Request ID should not be null");
            assertFalse(requestId.trim().isEmpty(), "Request ID should not be empty");

            System.out.println("Async load request ID: " + requestId);
        }
    }

    @Test
    @DisplayName("Load component async with null request should throw exception")
    void testLoadComponentAsyncWithNullRequest() {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            assertThrows(IllegalArgumentException.class, () ->
                    manager.loadComponentAsync(null));
        }
    }

    @Test
    @DisplayName("Load component async with invalid request should throw exception")
    void testLoadComponentAsyncWithInvalidRequest() {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final var validationConfig = new JniHotReloadManager.ValidationConfig(
                    true, true, false, false, 30);

            // Invalid component name
            final var invalidRequest1 = new JniHotReloadManager.LoadRequest(
                    null, "/path/to/component.wasm", TEST_VERSION_1,
                    JniHotReloadManager.LoadPriority.NORMAL, validationConfig);

            assertThrows(Exception.class, () -> {
                final CompletableFuture<String> future = manager.loadComponentAsync(invalidRequest1);
                future.get(1, TimeUnit.SECONDS);
            });

            // Invalid component path
            final var invalidRequest2 = new JniHotReloadManager.LoadRequest(
                    TEST_COMPONENT_NAME, null, TEST_VERSION_1,
                    JniHotReloadManager.LoadPriority.NORMAL, validationConfig);

            assertThrows(Exception.class, () -> {
                final CompletableFuture<String> future = manager.loadComponentAsync(invalidRequest2);
                future.get(1, TimeUnit.SECONDS);
            });
        }
    }

    @Test
    @DisplayName("Get hot reload metrics")
    void testGetMetrics() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            // Get initial metrics
            final var metrics = manager.getMetrics();

            assertNotNull(metrics, "Metrics should not be null");
            assertTrue(metrics.getTotalSwaps() >= 0, "Total swaps should be non-negative");
            assertTrue(metrics.getSuccessfulSwaps() >= 0, "Successful swaps should be non-negative");
            assertTrue(metrics.getFailedSwaps() >= 0, "Failed swaps should be non-negative");
            assertTrue(metrics.getRollbacks() >= 0, "Rollbacks should be non-negative");
            assertTrue(metrics.getCurrentActiveSwaps() >= 0, "Active swaps should be non-negative");
            assertTrue(metrics.getComponentsLoaded() >= 0, "Components loaded should be non-negative");
            assertTrue(metrics.getCacheEfficiency() >= 0.0f && metrics.getCacheEfficiency() <= 1.0f,
                    "Cache efficiency should be between 0 and 1");

            System.out.println("Hot reload metrics: " + metrics);

            // Perform a hot swap to see metrics change
            final var strategy = JniHotReloadManager.SwapStrategy.immediate();
            final String operationId = manager.startHotSwap(TEST_COMPONENT_NAME, TEST_VERSION_2, strategy);

            // Give it a moment to process
            Thread.sleep(100);

            // Get updated metrics
            final var updatedMetrics = manager.getMetrics();
            System.out.println("Updated metrics: " + updatedMetrics);

            // Should have at least one active swap or completed swap
            assertTrue(updatedMetrics.getTotalSwaps() > 0 || updatedMetrics.getCurrentActiveSwaps() > 0,
                    "Should have some swap activity");
        }
    }

    @Test
    @DisplayName("Multiple operations can run concurrently")
    void testMultipleConcurrentOperations() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.builder()
                .loaderThreadCount(4)
                .build();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final var strategy = JniHotReloadManager.SwapStrategy.canary(5.0f, 15.0f, 0.95f);

            // Start multiple operations
            final String operation1 = manager.startHotSwap("component-1", TEST_VERSION_2, strategy);
            final String operation2 = manager.startHotSwap("component-2", TEST_VERSION_2, strategy);
            final String operation3 = manager.startHotSwap("component-3", TEST_VERSION_2, strategy);

            assertNotNull(operation1, "Operation 1 should not be null");
            assertNotNull(operation2, "Operation 2 should not be null");
            assertNotNull(operation3, "Operation 3 should not be null");

            // Verify they are different operations
            assertNotEquals(operation1, operation2, "Operations should have different IDs");
            assertNotEquals(operation2, operation3, "Operations should have different IDs");
            assertNotEquals(operation1, operation3, "Operations should have different IDs");

            System.out.println("Started concurrent operations: " + operation1 + ", " + operation2 + ", " + operation3);

            // Monitor all operations briefly
            for (int i = 0; i < 5; i++) {
                Thread.sleep(200);

                final var status1 = manager.getSwapStatus(operation1);
                final var status2 = manager.getSwapStatus(operation2);
                final var status3 = manager.getSwapStatus(operation3);

                System.out.println(String.format("Iteration %d - Op1: %s, Op2: %s, Op3: %s",
                        i + 1,
                        status1 != null ? status1.getStatus() : "null",
                        status2 != null ? status2.getStatus() : "null",
                        status3 != null ? status3.getStatus() : "null"));
            }

            final var finalMetrics = manager.getMetrics();
            System.out.println("Final metrics after concurrent operations: " + finalMetrics);
        }
    }

    @Test
    @DisplayName("Manager operations should fail after close")
    void testOperationsAfterClose() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();
        final var manager = new JniHotReloadManager(engine, config);

        // Manager should work initially
        assertFalse(manager.isClosed(), "Manager should not be closed initially");

        // Close the manager
        manager.close();

        // Manager should be marked as closed
        assertTrue(manager.isClosed(), "Manager should be closed after close()");

        // Operations should fail
        final var strategy = JniHotReloadManager.SwapStrategy.immediate();

        assertThrows(IllegalStateException.class, () ->
                manager.startHotSwap(TEST_COMPONENT_NAME, TEST_VERSION_2, strategy));

        assertThrows(IllegalStateException.class, () ->
                manager.getSwapStatus("test-operation"));

        assertThrows(IllegalStateException.class, () ->
                manager.cancelHotSwap("test-operation"));

        assertThrows(IllegalStateException.class, () ->
                manager.getMetrics());

        // Multiple closes should be safe
        manager.close(); // Should not throw
    }

    @Test
    @DisplayName("Test all swap strategy types")
    void testAllSwapStrategies() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            // Test all strategy types
            final var strategies = new JniHotReloadManager.SwapStrategy[]{
                    JniHotReloadManager.SwapStrategy.immediate(),
                    JniHotReloadManager.SwapStrategy.canary(10.0f, 20.0f, 0.95f),
                    JniHotReloadManager.SwapStrategy.blueGreen(),
                    JniHotReloadManager.SwapStrategy.rollingUpdate(5, 60),
                    JniHotReloadManager.SwapStrategy.abTest(25.0f, 600),
                    JniHotReloadManager.SwapStrategy.getDefault()
            };

            for (int i = 0; i < strategies.length; i++) {
                final var strategy = strategies[i];
                final String componentName = "test-component-" + i;

                final String operationId = manager.startHotSwap(componentName, TEST_VERSION_2, strategy);

                assertNotNull(operationId, "Operation ID should not be null for strategy " + i);
                System.out.println(String.format("Strategy %d (%s): %s", i, strategy.getClass().getSimpleName(), operationId));

                // Brief pause between operations
                Thread.sleep(50);
            }

            // Check final metrics
            final var metrics = manager.getMetrics();
            assertTrue(metrics.getTotalSwaps() >= strategies.length || metrics.getCurrentActiveSwaps() > 0,
                    "Should have processed all strategy operations");

            System.out.println("All strategies tested. Final metrics: " + metrics);
        }
    }

    @Test
    @DisplayName("Test load priorities")
    void testLoadPriorities() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.getDefault();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            // Create test files
            final Path testFile1 = tempDir.resolve("low-priority.wasm");
            final Path testFile2 = tempDir.resolve("high-priority.wasm");
            final Path testFile3 = tempDir.resolve("critical-priority.wasm");

            Files.write(testFile1, "low priority content".getBytes());
            Files.write(testFile2, "high priority content".getBytes());
            Files.write(testFile3, "critical priority content".getBytes());

            final var validationConfig = new JniHotReloadManager.ValidationConfig(
                    true, false, false, false, 10);

            // Submit requests with different priorities
            final var priorities = JniHotReloadManager.LoadPriority.values();
            final var testFiles = new Path[]{testFile1, testFile2, testFile3, testFile1};

            for (int i = 0; i < priorities.length; i++) {
                final var priority = priorities[i];
                final var testFile = testFiles[i % testFiles.length];

                final var loadRequest = new JniHotReloadManager.LoadRequest(
                        "component-" + priority.name().toLowerCase(),
                        testFile.toString(),
                        "1.0." + i,
                        priority,
                        validationConfig);

                final CompletableFuture<String> future = manager.loadComponentAsync(loadRequest);
                final String requestId = future.get(3, TimeUnit.SECONDS);

                assertNotNull(requestId, "Request ID should not be null for priority " + priority);
                System.out.println(String.format("Priority %s: %s", priority, requestId));
            }
        }
    }

    @Test
    @DisplayName("Stress test with many operations")
    void testStressOperations() throws Exception {
        final var config = JniHotReloadManager.HotReloadConfig.builder()
                .loaderThreadCount(8)
                .cacheSize(200)
                .build();

        try (final var manager = new JniHotReloadManager(engine, config)) {
            final int operationCount = 20;
            final var operationIds = new String[operationCount];

            // Start many operations rapidly
            for (int i = 0; i < operationCount; i++) {
                final var strategy = i % 2 == 0
                        ? JniHotReloadManager.SwapStrategy.immediate()
                        : JniHotReloadManager.SwapStrategy.canary(5.0f, 10.0f, 0.9f);

                operationIds[i] = manager.startHotSwap("stress-component-" + i, "1.0." + i, strategy);
                assertNotNull(operationIds[i], "Operation ID should not be null for operation " + i);

                // Small delay to avoid overwhelming the system
                if (i % 5 == 0) {
                    Thread.sleep(10);
                }
            }

            System.out.println("Started " + operationCount + " stress operations");

            // Monitor for a few seconds
            for (int second = 0; second < 3; second++) {
                Thread.sleep(1000);

                final var metrics = manager.getMetrics();
                System.out.println(String.format("Stress test second %d: %s", second + 1, metrics));

                // Check a few random operations
                for (int i = 0; i < 5; i++) {
                    final int randomIndex = (int) (Math.random() * operationCount);
                    final var status = manager.getSwapStatus(operationIds[randomIndex]);
                    if (status != null) {
                        System.out.println(String.format("Operation %d status: %s (%.1f%%)",
                                randomIndex, status.getStatus(), status.getProgress() * 100));
                    }
                }
            }

            final var finalMetrics = manager.getMetrics();
            System.out.println("Stress test completed. Final metrics: " + finalMetrics);

            // Should have processed many operations
            assertTrue(finalMetrics.getTotalSwaps() > 0 || finalMetrics.getCurrentActiveSwaps() > 0,
                    "Should have swap activity from stress test");
        }
    }
}