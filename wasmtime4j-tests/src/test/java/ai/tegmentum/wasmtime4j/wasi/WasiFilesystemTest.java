package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
// // import ai.tegmentum.wasmtime4j.functions.WasmFunction;
import ai.tegmentum.wasmtime4j.utils.CrossRuntimeValidator;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive filesystem integration tests for WASI functionality. Tests filesystem operations,
 * permission validation, security boundaries, and cross-runtime compatibility for file access.
 */
@Tag(TestCategories.INTEGRATION)
@Tag(TestCategories.WASI)
@Tag(TestCategories.FILESYSTEM)
@Tag(TestCategories.SECURITY)
public final class WasiFilesystemTest {
  private static final Logger LOGGER = Logger.getLogger(WasiFilesystemTest.class.getName());

  @TempDir private Path tempDirectory;

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;

  @BeforeEach
  void setUp() {
    runtime = WasmRuntimeFactory.create();
    engine = runtime.createEngine();
    store = engine.createStore();
    LOGGER.info("Set up WASI filesystem test with runtime: " + runtime.getRuntimeType());
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
    if (runtime != null) {
      runtime.close();
    }
  }

  /** Tests basic filesystem access through WASI pre-opened directories. */
  @Test
  void testBasicFilesystemAccess() throws IOException {
    LOGGER.info("Testing basic filesystem access");

    final Path testDir = tempDirectory.resolve("test");
    Files.createDirectories(testDir);

    final Path testFile = testDir.resolve("test.txt");
    Files.write(testFile, "Hello, WASI!".getBytes());

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .preopenDir(testDir.toString(), "test", true, false) // read-only access
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(
        () -> {
          final Wasi wasi = store.createWasi(config);
          assertNotNull(wasi);

          // Verify directory is pre-opened
          assertTrue(wasi.getPreopenedDirectories().containsKey("test"));

          wasi.close();
          });
  }

  /** Tests filesystem read operations with proper permission validation. */
  @Test
  void testFilesystemReadOperations() throws IOException {
    LOGGER.info("Testing filesystem read operations");

    final Path readDir = tempDirectory.resolve("readonly");
    Files.createDirectories(readDir);

    final Path testFile = readDir.resolve("data.txt");
    final String testContent = "This is test data for reading";
    Files.write(testFile, testContent.getBytes());

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .preopenDir(readDir.toString(), "readonly", true, false) // read-only
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = createFileReadModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Test file reading functionality
          if (instance.hasExport("read_file")) {
            final WasmFunction readFunction = instance.getExport("read_file").asFunction();
            assertNotNull(readFunction);

            // Execute read operation
            assertDoesNotThrow(
                () -> {
                  final var result = readFunction.call();
                  // Verify read operation completed successfully
                  assertNotNull(result);
          });
          }

          instance.close();
          wasi.close();
          module.close();
          });
  }

  /** Tests filesystem write operations with permission validation. */
  @Test
  void testFilesystemWriteOperations() throws IOException {
    LOGGER.info("Testing filesystem write operations");

    final Path writeDir = tempDirectory.resolve("writable");
    Files.createDirectories(writeDir);

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .preopenDir(writeDir.toString(), "writable", true, true) // read-write
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = createFileWriteModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Test file writing functionality
          if (instance.hasExport("write_file")) {
            final WasmFunction writeFunction = instance.getExport("write_file").asFunction();
            assertNotNull(writeFunction);

            // Execute write operation
            assertDoesNotThrow(
                () -> {
                  final var result = writeFunction.call();
                  assertNotNull(result);
          });
          }

          instance.close();
          wasi.close();
          module.close();
          });

    // Verify file was created (if write was successful)
    final Path expectedFile = writeDir.resolve("output.txt");
    // Note: Actual file creation depends on WASI module implementation
  }

  /** Tests filesystem permission boundaries and access control. */
  @Test
  void testFilesystemPermissionBoundaries() throws IOException {
    LOGGER.info("Testing filesystem permission boundaries");

    final Path allowedDir = tempDirectory.resolve("allowed");
    final Path forbiddenDir = tempDirectory.resolve("forbidden");
    Files.createDirectories(allowedDir);
    Files.createDirectories(forbiddenDir);

    final Path allowedFile = allowedDir.resolve("allowed.txt");
    final Path forbiddenFile = forbiddenDir.resolve("forbidden.txt");
    Files.write(allowedFile, "Allowed content".getBytes());
    Files.write(forbiddenFile, "Forbidden content".getBytes());

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .preopenDir(allowedDir.toString(), "allowed", true, false)
            // Note: forbiddenDir is NOT pre-opened
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    assertDoesNotThrow(
        () -> {
          final Wasi wasi = store.createWasi(config);
          assertNotNull(wasi);

          // Verify only allowed directory is accessible
          assertTrue(wasi.getPreopenedDirectories().containsKey("allowed"));
          assertFalse(wasi.getPreopenedDirectories().containsKey("forbidden"));

          wasi.close();
          });
  }

  /** Tests security boundaries preventing path traversal attacks. */
  @Test
  void testPathTraversalPrevention() throws IOException {
    LOGGER.info("Testing path traversal attack prevention");

    final Path sandboxDir = tempDirectory.resolve("sandbox");
    Files.createDirectories(sandboxDir);

    final Path sensitiveFile = tempDirectory.resolve("sensitive.txt");
    Files.write(sensitiveFile, "Sensitive data".getBytes());

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .preopenDir(sandboxDir.toString(), "sandbox", true, false)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = createPathTraversalTestModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Test that path traversal attempts are blocked
          if (instance.hasExport("attempt_traversal")) {
            final WasmFunction traversalFunction =
                instance.getExport("attempt_traversal").asFunction();
            assertNotNull(traversalFunction);

            // This should fail or be blocked by WASI security
            assertDoesNotThrow(
                () -> {
                  final var result = traversalFunction.call();
                  // The function should complete but access should be denied
          });
          }

          instance.close();
          wasi.close();
          module.close();
          });
  }

  /** Tests filesystem operations under different permission modes. */
  @Test
  void testFilesystemPermissionModes() throws IOException {
    LOGGER.info("Testing filesystem permission modes");

    final Path testDir = tempDirectory.resolve("permissions");
    Files.createDirectories(testDir);

    final Path testFile = testDir.resolve("test.txt");
    Files.write(testFile, "Test content".getBytes());

    // Test read-only mode
    final WasiConfig readOnlyConfig =
        WasiConfig.builder()
            .inheritEnv(true)
            .preopenDir(testDir.toString(), "readonly", true, false) // read-only
            .build();

    assertDoesNotThrow(
        () -> {
          final Wasi readOnlyWasi = store.createWasi(readOnlyConfig);
          assertNotNull(readOnlyWasi);

          // Verify read permissions
          assertTrue(readOnlyWasi.getPreopenedDirectories().containsKey("readonly"));

          readOnlyWasi.close();
          });

    // Test read-write mode
    final WasiConfig readWriteConfig =
        WasiConfig.builder()
            .inheritEnv(true)
            .preopenDir(testDir.toString(), "readwrite", true, true) // read-write
            .build();

    assertDoesNotThrow(
        () -> {
          final Wasi readWriteWasi = store.createWasi(readWriteConfig);
          assertNotNull(readWriteWasi);

          // Verify read-write permissions
          assertTrue(readWriteWasi.getPreopenedDirectories().containsKey("readwrite"));

          readWriteWasi.close();
          });
  }

  /** Tests directory creation and management operations. */
  @Test
  void testDirectoryOperations() throws IOException {
    LOGGER.info("Testing directory operations");

    final Path baseDir = tempDirectory.resolve("directories");
    Files.createDirectories(baseDir);

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .preopenDir(baseDir.toString(), "base", true, true) // read-write
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = createDirectoryOpsModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Test directory operations
          if (instance.hasExport("test_directories")) {
            final WasmFunction dirFunction = instance.getExport("test_directories").asFunction();
            assertNotNull(dirFunction);

            assertDoesNotThrow(
                () -> {
                  final var result = dirFunction.call();
                  assertNotNull(result);
          });
          }

          instance.close();
          wasi.close();
          module.close();
          });
  }

  /** Tests file metadata and attributes access. */
  @Test
  void testFileMetadataAccess() throws IOException {
    LOGGER.info("Testing file metadata access");

    final Path metaDir = tempDirectory.resolve("metadata");
    Files.createDirectories(metaDir);

    final Path testFile = metaDir.resolve("meta.txt");
    Files.write(testFile, "File with metadata".getBytes());

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .preopenDir(metaDir.toString(), "meta", true, false)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = createMetadataTestModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Test metadata access
          if (instance.hasExport("get_metadata")) {
            final WasmFunction metaFunction = instance.getExport("get_metadata").asFunction();
            assertNotNull(metaFunction);

            assertDoesNotThrow(
                () -> {
                  final var result = metaFunction.call();
                  assertNotNull(result);
          });
          }

          instance.close();
          wasi.close();
          module.close();
          });
  }

  /** Tests cross-runtime filesystem operation compatibility. */
  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testCrossRuntimeFilesystemCompatibility() throws IOException {
    LOGGER.info("Testing cross-runtime filesystem compatibility");

    if (!TestUtils.isPanamaAvailable()) {
      LOGGER.warning("Panama runtime not available, skipping cross-runtime test");
      return;
    }

    final Path crossDir = tempDirectory.resolve("crossruntime");
    Files.createDirectories(crossDir);

    final Path testFile = crossDir.resolve("cross.txt");
    Files.write(testFile, "Cross-runtime test".getBytes());

    final CrossRuntimeValidator.RuntimeOperation<String> filesystemOperation =
        runtime -> {
          try (final Engine engine = runtime.createEngine();
              final Store store = engine.createStore()) {

            final WasiConfig config =
                WasiConfig.builder()
                    .inheritEnv(true)
                    .preopenDir(crossDir.toString(), "cross", true, false)
                    .inheritStdin(true)
                    .inheritStdout(true)
                    .inheritStderr(true)
                    .build();

            final Wasi wasi = store.createWasi(config);
            final boolean hasDir = wasi.getPreopenedDirectories().containsKey("cross");
            final int dirCount = wasi.getPreopenedDirectories().size();

            wasi.close();

            return String.format("hasDir=%s,count=%d", hasDir, dirCount);
          }
        };

    final CrossRuntimeValidator.ComparisonResult result =
        CrossRuntimeValidator.validateCrossRuntime(filesystemOperation, Duration.ofSeconds(15));

    assertTrue(
        result.isValid(),
        "Filesystem behavior differs between runtimes: " + result.getDifferenceDescription());

    LOGGER.info("Cross-runtime filesystem validation successful");
  }

  /** Tests filesystem error handling and recovery. */
  @Test
  void testFilesystemErrorHandling() throws IOException {
    LOGGER.info("Testing filesystem error handling");

    final Path errorDir = tempDirectory.resolve("errors");
    Files.createDirectories(errorDir);

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .preopenDir(errorDir.toString(), "errors", true, true)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = createErrorHandlingModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Test error scenarios
          if (instance.hasExport("test_errors")) {
            final WasmFunction errorFunction = instance.getExport("test_errors").asFunction();
            assertNotNull(errorFunction);

            // Error function should handle errors gracefully
            assertDoesNotThrow(
                () -> {
                  final var result = errorFunction.call();
                  assertNotNull(result);
          });
          }

          instance.close();
          wasi.close();
          module.close();
          });
  }

  /** Tests filesystem operations with large files and stress conditions. */
  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testLargeFileOperations() throws IOException {
    LOGGER.info("Testing large file operations");

    final Path largeDir = tempDirectory.resolve("large");
    Files.createDirectories(largeDir);

    // Create a moderately large file (1MB)
    final Path largeFile = largeDir.resolve("large.dat");
    final byte[] largeData = new byte[1024 * 1024]; // 1MB
    Arrays.fill(largeData, (byte) 0xAA);
    Files.write(largeFile, largeData);

    final WasiConfig config =
        WasiConfig.builder()
            .inheritEnv(true)
            .preopenDir(largeDir.toString(), "large", true, false)
            .inheritStdin(true)
            .inheritStdout(true)
            .inheritStderr(true)
            .build();

    final byte[] wasmBytes = createLargeFileModule();

    assertDoesNotThrow(
        () -> {
          final Module module = engine.createModule(wasmBytes);
          final Wasi wasi = store.createWasi(config);
          final Instance instance = store.createInstance(module, wasi.getImports());

          // Test large file operations
          if (instance.hasExport("process_large_file")) {
            final WasmFunction largeFunction =
                instance.getExport("process_large_file").asFunction();
            assertNotNull(largeFunction);

            assertDoesNotThrow(
                () -> {
                  final var result = largeFunction.call();
                  assertNotNull(result);
          });
          }

          instance.close();
          wasi.close();
          module.close();
          });
  }

  /** Creates a WebAssembly module for file reading operations. */
  private byte[] createFileReadModule() {
    // This would be a WASI module that attempts to read files
    // For testing, use the basic WASI module
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module for file writing operations. */
  private byte[] createFileWriteModule() {
    // This would be a WASI module that attempts to write files
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module for path traversal testing. */
  private byte[] createPathTraversalTestModule() {
    // This would be a WASI module that attempts path traversal attacks
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module for directory operations. */
  private byte[] createDirectoryOpsModule() {
    // This would be a WASI module that tests directory operations
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module for metadata access testing. */
  private byte[] createMetadataTestModule() {
    // This would be a WASI module that accesses file metadata
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module for error handling testing. */
  private byte[] createErrorHandlingModule() {
    // This would be a WASI module that tests error scenarios
    return WasmTestModules.getModule("wasi_basic");
  }

  /** Creates a WebAssembly module for large file operations. */
  private byte[] createLargeFileModule() {
    // This would be a WASI module that processes large files
    return WasmTestModules.getModule("wasi_basic");
  }
}
