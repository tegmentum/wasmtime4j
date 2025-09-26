package ai.tegmentum.wasmtime4j.hotreload;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for file system watcher functionality.
 *
 * <p>These tests verify file system monitoring capabilities including:
 * <ul>
 *   <li>Directory and file watching setup</li>
 *   <li>File change detection and debouncing</li>
 *   <li>Hot-reload trigger integration</li>
 *   <li>Pattern matching and filtering</li>
 *   <li>Component name and version extraction</li>
 *   <li>Error handling and recovery</li>
 *   <li>Resource management and cleanup</li>
 * </ul>
 */
@DisplayName("File System Watcher Tests")
class FileSystemWatcherTest {

    @Mock
    private HotReloadManager mockHotReloadManager;

    @Mock
    private Engine mockEngine;

    private Path tempDir;
    private AutoCloseable mockCloseable;

    @BeforeEach
    void setUp() throws IOException {
        mockCloseable = MockitoAnnotations.openMocks(this);

        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("filesystem-watcher-test");

        // Setup mock behavior
        when(mockHotReloadManager.isClosed()).thenReturn(false);
        when(mockHotReloadManager.startHotSwap(anyString(), anyString(), any()))
                .thenReturn("mock-operation-id");

        System.out.println("Test setup completed with temp dir: " + tempDir);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockCloseable != null) {
            mockCloseable.close();
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
    @DisplayName("Create watcher with valid manager")
    void testCreateWatcher() {
        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            assertNotNull(watcher, "Watcher should not be null");
            assertFalse(watcher.isRunning(), "Watcher should not be running initially");
            assertEquals(0, watcher.getWatchCount(), "Should have no watches initially");

            System.out.println("Created file system watcher successfully");
        }
    }

    @Test
    @DisplayName("Create watcher with null manager should throw exception")
    void testCreateWatcherWithNullManager() {
        final var exception = assertThrows(IllegalArgumentException.class, () ->
                new FileSystemWatcher(null));

        assertEquals("Hot reload manager cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Watch directory with default config")
    void testWatchDirectory() throws IOException {
        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);

            assertEquals(1, watcher.getWatchCount(), "Should have one watch registered");

            System.out.println("Successfully watching directory: " + tempDir);
        }
    }

    @Test
    @DisplayName("Watch specific file")
    void testWatchFile() throws IOException {
        // Create a test file
        final Path testFile = tempDir.resolve("test-component.wasm");
        Files.createFile(testFile);

        final var config = FileSystemWatcher.WatchConfig.builder()
                .debounceDelayMs(100)
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchFile(testFile, config);

            assertEquals(1, watcher.getWatchCount(), "Should have one watch registered");

            System.out.println("Successfully watching file: " + testFile);
        }
    }

    @Test
    @DisplayName("Watch non-existent directory should throw exception")
    void testWatchNonExistentDirectory() {
        final Path nonExistent = tempDir.resolve("non-existent");
        final var config = FileSystemWatcher.WatchConfig.builder().build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            final var exception = assertThrows(IllegalArgumentException.class, () ->
                    watcher.watchDirectory(nonExistent, config));

            assertTrue(exception.getMessage().contains("does not exist"));
        }
    }

    @Test
    @DisplayName("Watch non-existent file should throw exception")
    void testWatchNonExistentFile() {
        final Path nonExistent = tempDir.resolve("non-existent.wasm");
        final var config = FileSystemWatcher.WatchConfig.builder().build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            final var exception = assertThrows(IllegalArgumentException.class, () ->
                    watcher.watchFile(nonExistent, config));

            assertTrue(exception.getMessage().contains("does not exist"));
        }
    }

    @Test
    @DisplayName("Watch file that is actually a directory should throw exception")
    void testWatchDirectoryAsFile() {
        final var config = FileSystemWatcher.WatchConfig.builder().build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            final var exception = assertThrows(IllegalArgumentException.class, () ->
                    watcher.watchFile(tempDir, config)); // tempDir is a directory

            assertTrue(exception.getMessage().contains("not a regular file"));
        }
    }

    @Test
    @DisplayName("Start and stop watcher")
    void testStartStopWatcher() throws InterruptedException {
        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);

            // Start the watcher
            watcher.start();
            assertTrue(watcher.isRunning(), "Watcher should be running after start");

            // Give it a moment to initialize
            Thread.sleep(100);

            // Stop the watcher
            watcher.stop();
            assertFalse(watcher.isRunning(), "Watcher should not be running after stop");

            System.out.println("Successfully started and stopped watcher");
        }
    }

    @Test
    @DisplayName("Starting already running watcher should throw exception")
    void testStartAlreadyRunningWatcher() {
        final var config = FileSystemWatcher.WatchConfig.builder().build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);

            watcher.start();
            assertTrue(watcher.isRunning(), "Watcher should be running");

            final var exception = assertThrows(IllegalStateException.class, () ->
                    watcher.start());

            assertEquals("File system watcher is already running", exception.getMessage());

            watcher.stop();
        }
    }

    @Test
    @DisplayName("File modification triggers hot reload")
    void testFileModificationTriggersReload() throws Exception {
        // Create a test WASM file
        final Path wasmFile = tempDir.resolve("test-component.wasm");
        Files.write(wasmFile, "initial content".getBytes());

        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .debounceDelayMs(50) // Short delay for testing
                .reloadStrategy(HotReloadManager.SwapStrategy.immediate())
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);
            watcher.start();

            // Give watcher time to initialize
            Thread.sleep(100);

            // Modify the file
            Files.write(wasmFile, "modified content".getBytes(), StandardOpenOption.WRITE);

            // Wait for debounce and processing
            Thread.sleep(200);

            // Verify hot reload was triggered
            verify(mockHotReloadManager, timeout(1000).atLeastOnce())
                    .startHotSwap(eq("test-component"), anyString(), any());

            System.out.println("File modification successfully triggered hot reload");
        }
    }

    @Test
    @DisplayName("File creation triggers hot reload")
    void testFileCreationTriggersReload() throws Exception {
        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .debounceDelayMs(50)
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);
            watcher.start();

            // Give watcher time to initialize
            Thread.sleep(100);

            // Create a new WASM file
            final Path newFile = tempDir.resolve("new-component.wasm");
            Files.write(newFile, "new content".getBytes());

            // Wait for debounce and processing
            Thread.sleep(200);

            // Verify hot reload was triggered
            verify(mockHotReloadManager, timeout(1000).atLeastOnce())
                    .startHotSwap(eq("new-component"), anyString(), any());

            System.out.println("File creation successfully triggered hot reload");
        }
    }

    @Test
    @DisplayName("Non-matching files are ignored")
    void testNonMatchingFilesIgnored() throws Exception {
        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm") // Only watch WASM files
                .debounceDelayMs(50)
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);
            watcher.start();

            // Give watcher time to initialize
            Thread.sleep(100);

            // Create non-matching files
            Files.write(tempDir.resolve("config.json"), "{}".getBytes());
            Files.write(tempDir.resolve("readme.txt"), "readme".getBytes());
            Files.write(tempDir.resolve("script.js"), "console.log('test')".getBytes());

            // Wait to ensure no reload is triggered
            Thread.sleep(300);

            // Verify no hot reload was triggered
            verify(mockHotReloadManager, never())
                    .startHotSwap(anyString(), anyString(), any());

            System.out.println("Non-matching files were correctly ignored");
        }
    }

    @Test
    @DisplayName("Multiple file patterns work correctly")
    void testMultiplePatterns() throws Exception {
        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .pattern("*.wit")
                .debounceDelayMs(50)
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);
            watcher.start();

            // Give watcher time to initialize
            Thread.sleep(100);

            // Create matching files
            Files.write(tempDir.resolve("component.wasm"), "wasm content".getBytes());
            Files.write(tempDir.resolve("interface.wit"), "wit content".getBytes());

            // Wait for processing
            Thread.sleep(300);

            // Verify hot reload was triggered for both
            verify(mockHotReloadManager, timeout(1000).atLeast(2))
                    .startHotSwap(anyString(), anyString(), any());

            System.out.println("Multiple patterns worked correctly");
        }
    }

    @Test
    @DisplayName("Ignore patterns work correctly")
    void testIgnorePatterns() throws Exception {
        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .ignorePattern("*test*")
                .ignorePattern("*backup*")
                .debounceDelayMs(50)
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);
            watcher.start();

            // Give watcher time to initialize
            Thread.sleep(100);

            // Create files that should be ignored
            Files.write(tempDir.resolve("test-component.wasm"), "test content".getBytes());
            Files.write(tempDir.resolve("backup-component.wasm"), "backup content".getBytes());

            // Create file that should NOT be ignored
            Files.write(tempDir.resolve("main-component.wasm"), "main content".getBytes());

            // Wait for processing
            Thread.sleep(300);

            // Verify only non-ignored file triggered reload
            verify(mockHotReloadManager, timeout(1000).times(1))
                    .startHotSwap(eq("main-component"), anyString(), any());

            System.out.println("Ignore patterns worked correctly");
        }
    }

    @Test
    @DisplayName("Custom component name extractor works")
    void testCustomComponentNameExtractor() throws Exception {
        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .debounceDelayMs(50)
                .componentNameExtractor(path -> {
                    final String fileName = path.getFileName().toString();
                    // Extract component name from pattern like "prefix-COMPONENT-suffix.wasm"
                    if (fileName.startsWith("prefix-") && fileName.endsWith("-suffix.wasm")) {
                        return fileName.substring(7, fileName.length() - 12); // Remove prefix and suffix
                    }
                    return fileName.replaceAll("\\.[^.]+$", ""); // Default behavior
                })
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);
            watcher.start();

            // Give watcher time to initialize
            Thread.sleep(100);

            // Create file with custom pattern
            Files.write(tempDir.resolve("prefix-mycomponent-suffix.wasm"), "content".getBytes());

            // Wait for processing
            Thread.sleep(200);

            // Verify correct component name was extracted
            verify(mockHotReloadManager, timeout(1000).times(1))
                    .startHotSwap(eq("mycomponent"), anyString(), any());

            System.out.println("Custom component name extractor worked correctly");
        }
    }

    @Test
    @DisplayName("Custom version extractor works")
    void testCustomVersionExtractor() throws Exception {
        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .debounceDelayMs(50)
                .versionExtractor(path -> {
                    final String fileName = path.getFileName().toString();
                    // Extract version from pattern like "component-v1.2.3.wasm"
                    if (fileName.contains("-v") && fileName.endsWith(".wasm")) {
                        final int vIndex = fileName.indexOf("-v");
                        final int dotIndex = fileName.lastIndexOf(".wasm");
                        return fileName.substring(vIndex + 2, dotIndex);
                    }
                    return "1.0.0"; // Default version
                })
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);
            watcher.start();

            // Give watcher time to initialize
            Thread.sleep(100);

            // Create file with version in name
            Files.write(tempDir.resolve("mycomponent-v2.5.1.wasm"), "content".getBytes());

            // Wait for processing
            Thread.sleep(200);

            // Verify correct version was extracted
            verify(mockHotReloadManager, timeout(1000).times(1))
                    .startHotSwap(anyString(), eq("2.5.1"), any());

            System.out.println("Custom version extractor worked correctly");
        }
    }

    @Test
    @DisplayName("Debouncing prevents rapid triggers")
    void testDebouncingPreventsRapidTriggers() throws Exception {
        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .debounceDelayMs(200) // Longer delay for testing
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);
            watcher.start();

            // Give watcher time to initialize
            Thread.sleep(100);

            // Create a file
            final Path testFile = tempDir.resolve("debounce-test.wasm");
            Files.write(testFile, "initial".getBytes());

            // Rapidly modify the file multiple times
            for (int i = 0; i < 5; i++) {
                Files.write(testFile, ("content " + i).getBytes(), StandardOpenOption.WRITE);
                Thread.sleep(20); // Rapid modifications
            }

            // Wait for debounce period plus processing time
            Thread.sleep(400);

            // Should only trigger once due to debouncing
            verify(mockHotReloadManager, timeout(1000).times(1))
                    .startHotSwap(anyString(), anyString(), any());

            System.out.println("Debouncing successfully prevented rapid triggers");
        }
    }

    @Test
    @DisplayName("Different reload strategies are applied correctly")
    void testDifferentReloadStrategies() throws Exception {
        // Test with canary strategy
        final var canaryStrategy = HotReloadManager.SwapStrategy.canary(15.0f, 30.0f, 0.95f);
        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .debounceDelayMs(50)
                .reloadStrategy(canaryStrategy)
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);
            watcher.start();

            // Give watcher time to initialize
            Thread.sleep(100);

            // Create a file
            Files.write(tempDir.resolve("strategy-test.wasm"), "content".getBytes());

            // Wait for processing
            Thread.sleep(200);

            // Verify the specific strategy was used
            verify(mockHotReloadManager, timeout(1000).times(1))
                    .startHotSwap(anyString(), anyString(), eq(canaryStrategy));

            System.out.println("Custom reload strategy was applied correctly");
        }
    }

    @Test
    @DisplayName("Progress monitoring can be disabled")
    void testProgressMonitoringDisabled() throws Exception {
        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .debounceDelayMs(50)
                .monitorProgress(false)
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);
            watcher.start();

            // Give watcher time to initialize
            Thread.sleep(100);

            // Create a file
            Files.write(tempDir.resolve("no-monitor.wasm"), "content".getBytes());

            // Wait for processing
            Thread.sleep(200);

            // Verify hot reload was triggered but status is not monitored repeatedly
            verify(mockHotReloadManager, timeout(1000).times(1))
                    .startHotSwap(anyString(), anyString(), any());

            // Should not call getSwapStatus repeatedly when monitoring is disabled
            Thread.sleep(1000);
            verify(mockHotReloadManager, times(0))
                    .getSwapStatus(anyString());

            System.out.println("Progress monitoring was correctly disabled");
        }
    }

    @Test
    @DisplayName("Watcher handles multiple directories")
    void testMultipleDirectories() throws Exception {
        // Create additional directories
        final Path dir2 = tempDir.resolve("modules");
        final Path dir3 = tempDir.resolve("components");
        Files.createDirectories(dir2);
        Files.createDirectories(dir3);

        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .debounceDelayMs(50)
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            // Watch multiple directories
            watcher.watchDirectory(tempDir, config);
            watcher.watchDirectory(dir2, config);
            watcher.watchDirectory(dir3, config);

            assertEquals(3, watcher.getWatchCount(), "Should have three watches");

            watcher.start();
            Thread.sleep(100);

            // Create files in different directories
            Files.write(tempDir.resolve("root-component.wasm"), "root".getBytes());
            Files.write(dir2.resolve("module-component.wasm"), "module".getBytes());
            Files.write(dir3.resolve("comp-component.wasm"), "comp".getBytes());

            // Wait for processing
            Thread.sleep(300);

            // Verify all triggered hot reloads
            verify(mockHotReloadManager, timeout(1000).times(3))
                    .startHotSwap(anyString(), anyString(), any());

            System.out.println("Multiple directories watched successfully");
        }
    }

    @Test
    @DisplayName("Watcher resource cleanup on close")
    void testResourceCleanupOnClose() throws Exception {
        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .build();

        final var watcher = new FileSystemWatcher(mockHotReloadManager);

        // Setup some watches
        watcher.watchDirectory(tempDir, config);
        assertEquals(1, watcher.getWatchCount(), "Should have one watch");

        // Start and then close
        watcher.start();
        assertTrue(watcher.isRunning(), "Should be running");

        watcher.close();

        // Verify cleanup
        assertFalse(watcher.isRunning(), "Should not be running after close");

        // Multiple closes should be safe
        watcher.close(); // Should not throw
        watcher.close(); // Should not throw

        System.out.println("Resource cleanup on close worked correctly");
    }

    @Test
    @DisplayName("Watcher handles errors gracefully")
    void testErrorHandling() throws Exception {
        // Setup mock to throw exception
        when(mockHotReloadManager.startHotSwap(anyString(), anyString(), any()))
                .thenThrow(new WasmRuntimeException("Test exception"));

        final var config = FileSystemWatcher.WatchConfig.builder()
                .pattern("*.wasm")
                .debounceDelayMs(50)
                .build();

        try (final var watcher = new FileSystemWatcher(mockHotReloadManager)) {
            watcher.watchDirectory(tempDir, config);
            watcher.start();

            // Give watcher time to initialize
            Thread.sleep(100);

            // Create a file that will trigger an error
            Files.write(tempDir.resolve("error-test.wasm"), "content".getBytes());

            // Wait for processing
            Thread.sleep(300);

            // Watcher should still be running despite the error
            assertTrue(watcher.isRunning(), "Watcher should still be running after error");

            // Exception should have been caught and logged, not propagated
            verify(mockHotReloadManager, timeout(1000).atLeastOnce())
                    .startHotSwap(anyString(), anyString(), any());

            System.out.println("Error handling worked correctly");
        }
    }
}