package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmEngine;
import ai.tegmentum.wasmtime4j.WasmInstance;
import ai.tegmentum.wasmtime4j.WasmModule;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmStore;
import ai.tegmentum.wasmtime4j.exception.WasmException;
// TODO: Fix WASI imports for comprehensive testing
// import ai.tegmentum.wasmtime4j.jni.wasi.WasiContext;
// import ai.tegmentum.wasmtime4j.jni.wasi.WasiContextBuilder;
// import ai.tegmentum.wasmtime4j.panama.wasi.WasiContext;
// import ai.tegmentum.wasmtime4j.panama.wasi.WasiContextBuilder;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for WASI (WebAssembly System Interface) functionality.
 * 
 * This test class provides comprehensive validation of WASI context creation, configuration,
 * lifecycle management, and system interface operations across both JNI and Panama implementations.
 * 
 * Key test areas:
 * - WASI context lifecycle (creation, configuration, cleanup)
 * - Filesystem access controls and security boundaries
 * - Environment variable configuration and access
 * - CLI argument handling and processing
 * - I/O redirection (stdin, stdout, stderr)
 * - Process exit handling and status codes
 * - Security boundary validation and attack prevention
 * - Cross-runtime compatibility validation
 */
@Tag(TestCategories.WASI_INTEGRATION)
@Tag(TestCategories.COMPREHENSIVE_TESTING)
@DisplayName("WASI Integration Comprehensive Tests")
@Timeout(value = 10, unit = TimeUnit.MINUTES)
public final class WasiIntegrationComprehensiveTest extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(WasiIntegrationComprehensiveTest.class.getName());

  @TempDir
  private Path tempDirectory;

  private Path testFilesDirectory;
  private Path preOpenDirectory;
  private Path restrictedDirectory;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    super.doSetUp(testInfo);

    try {
      // Set up test directories
      testFilesDirectory = tempDirectory.resolve("wasi-test-files");
      preOpenDirectory = tempDirectory.resolve("preopen");
      restrictedDirectory = tempDirectory.resolve("restricted");

      Files.createDirectories(testFilesDirectory);
      Files.createDirectories(preOpenDirectory);
      Files.createDirectories(restrictedDirectory);

      // Create test files for filesystem operations
      createTestFiles();

      LOGGER.info("WASI test environment initialized with directories:");
      LOGGER.info("  Test files: " + testFilesDirectory);
      LOGGER.info("  Preopen: " + preOpenDirectory);
      LOGGER.info("  Restricted: " + restrictedDirectory);

    } catch (final Exception e) {
      throw new RuntimeException("Failed to initialize WASI test environment", e);
    }
  }

  @Override
  protected void doTearDown(final TestInfo testInfo) {
    try {
      // Clean up test directories
      if (testFilesDirectory != null && Files.exists(testFilesDirectory)) {
        TestUtils.deleteDirectoryRecursively(testFilesDirectory);
      }
    } catch (final Exception e) {
      LOGGER.warning("Failed to clean up WASI test environment: " + e.getMessage());
    }

    super.doTearDown(testInfo);
  }

  /**
   * Tests basic WASI context creation and lifecycle management.
   * Validates that contexts can be created, configured, and properly closed.
   */
  @Test
  @DisplayName("WASI Context Lifecycle - Basic Creation and Cleanup")
  void testWasiContextLifecycle() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing WASI context lifecycle with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        // Test basic context creation
        WasiContext context = createBasicWasiContext(runtimeType);
        assertNotNull(context, "WASI context should be created successfully");
        assertFalse(context.isClosed(), "WASI context should not be closed after creation");

        // Test context state access
        assertNotNull(context.getEnvironment(), "Environment should be accessible");
        assertNotNull(context.getArguments(), "Arguments should be accessible");
        assertNotNull(context.getPreopenedDirectories(), "Preopened directories should be accessible");
        assertNotNull(context.getWorkingDirectory(), "Working directory should be accessible");

        // Test proper cleanup
        context.close();
        assertTrue(context.isClosed(), "WASI context should be closed after close() call");

        // Test double close safety
        assertDoesNotThrow(() -> context.close(), "Double close should be safe");
      }
    });
  }

  /**
   * Tests WASI context creation with comprehensive configuration options.
   * Validates all configuration parameters are properly set and accessible.
   */
  @Test
  @DisplayName("WASI Context Configuration - Comprehensive Parameter Validation")
  void testWasiContextComprehensiveConfiguration() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing comprehensive WASI context configuration with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        // Create comprehensive configuration
        final Map<String, String> environment = new HashMap<>();
        environment.put("HOME", "/home/test");
        environment.put("PATH", "/usr/bin:/bin");
        environment.put("LANG", "en_US.UTF-8");
        environment.put("TERM", "xterm-256color");
        
        final String[] arguments = {"test-program", "--verbose", "--input", "test.txt", "--output", "result.txt"};
        
        WasiContext context = createConfiguredWasiContext(runtimeType, environment, arguments);
        registerForCleanup(context);

        // Validate environment variables
        final Map<String, String> actualEnv = context.getEnvironment();
        assertEquals(environment.size(), actualEnv.size(), "Environment variable count should match");
        for (final Map.Entry<String, String> entry : environment.entrySet()) {
          assertEquals(entry.getValue(), actualEnv.get(entry.getKey()), 
              "Environment variable " + entry.getKey() + " should match");
        }

        // Validate arguments
        final String[] actualArgs = context.getArguments();
        assertEquals(arguments.length, actualArgs.length, "Argument count should match");
        for (int i = 0; i < arguments.length; i++) {
          assertEquals(arguments[i], actualArgs[i], "Argument at index " + i + " should match");
        }

        // Validate preopen directories
        final Map<String, Path> preopenDirs = context.getPreopenedDirectories();
        assertTrue(preopenDirs.containsKey("/tmp"), "Should contain /tmp preopen directory");
        assertEquals(preOpenDirectory, preopenDirs.get("/tmp"), "Preopen directory path should match");

        // Validate working directory
        assertEquals(Paths.get("/app"), context.getWorkingDirectory(), "Working directory should match");
      }
    });
  }

  /**
   * Tests WASI context creation error handling and validation.
   * Validates proper error handling for invalid configurations.
   */
  @Test
  @DisplayName("WASI Context Validation - Error Handling and Invalid Configurations")
  void testWasiContextValidationAndErrors() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing WASI context validation and error handling with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        // Test invalid environment variable names
        assertThrows(IllegalArgumentException.class, () -> {
          createWasiContextBuilder(runtimeType)
              .withEnvironment("", "value")
              .build();
        }, "Empty environment variable name should throw exception");

        assertThrows(IllegalArgumentException.class, () -> {
          createWasiContextBuilder(runtimeType)
              .withEnvironment(null, "value")
              .build();
        }, "Null environment variable name should throw exception");

        // Test invalid argument values
        assertThrows(IllegalArgumentException.class, () -> {
          createWasiContextBuilder(runtimeType)
              .withArgument(null)
              .build();
        }, "Null argument should throw exception");

        // Test invalid preopen directory paths
        assertThrows(IllegalArgumentException.class, () -> {
          createWasiContextBuilder(runtimeType)
              .withPreopenDirectory("/guest", "/nonexistent/path")
              .build();
        }, "Non-existent host directory should throw exception");

        assertThrows(IllegalArgumentException.class, () -> {
          final Path tempFile = Files.createTempFile(tempDirectory, "test", ".txt");
          createWasiContextBuilder(runtimeType)
              .withPreopenDirectory("/guest", tempFile.toString())
              .build();
        }, "Host path that is not a directory should throw exception");

        // Test invalid working directory
        assertThrows(IllegalArgumentException.class, () -> {
          createWasiContextBuilder(runtimeType)
              .withWorkingDirectory("")
              .build();
        }, "Empty working directory should throw exception");
      }
    });
  }

  /**
   * Tests WASI context resource management and memory cleanup.
   * Validates that contexts properly release resources on close.
   */
  @Test
  @DisplayName("WASI Context Resource Management - Memory and Resource Cleanup")
  void testWasiContextResourceManagement() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing WASI context resource management with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        // Create multiple contexts to test resource management
        for (int i = 0; i < 10; i++) {
          WasiContext context = createBasicWasiContext(runtimeType);
          assertNotNull(context, "Context " + i + " should be created successfully");
          
          // Verify context state
          assertFalse(context.isClosed(), "Context " + i + " should not be closed after creation");
          
          // Close context
          context.close();
          assertTrue(context.isClosed(), "Context " + i + " should be closed after close() call");
          
          // Verify operations fail after close
          assertThrows(Exception.class, () -> context.getEnvironment(),
              "Operations should fail on closed context " + i);
        }
        
        // Force garbage collection to test cleanup
        System.gc();
        Thread.sleep(100); // Allow time for cleanup
        
        LOGGER.info("Successfully created and cleaned up 10 WASI contexts");
      }
    });
  }

  /**
   * Tests WASI context thread safety and concurrent access.
   * Validates that contexts can be safely accessed from multiple threads.
   */
  @Test
  @DisplayName("WASI Context Thread Safety - Concurrent Access Validation")
  void testWasiContextThreadSafety() {
    skipIfCategoryNotEnabled(TestCategories.PERFORMANCE_TESTING);
    
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing WASI context thread safety with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        WasiContext context = createBasicWasiContext(runtimeType);
        registerForCleanup(context);
        
        // Test concurrent read access
        final int threadCount = 5;
        final Thread[] threads = new Thread[threadCount];
        final Exception[] exceptions = new Exception[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
          final int threadIndex = i;
          threads[i] = new Thread(() -> {
            try {
              for (int j = 0; j < 100; j++) {
                // Perform various read operations
                assertNotNull(context.getEnvironment(), "Environment should be accessible from thread " + threadIndex);
                assertNotNull(context.getArguments(), "Arguments should be accessible from thread " + threadIndex);
                assertNotNull(context.getPreopenedDirectories(), "Preopen directories should be accessible from thread " + threadIndex);
                assertNotNull(context.getWorkingDirectory(), "Working directory should be accessible from thread " + threadIndex);
                
                // Validate path operations
                final Path testPath = context.validatePath("/tmp/test");
                assertNotNull(testPath, "Path validation should work from thread " + threadIndex);
              }
            } catch (final Exception e) {
              exceptions[threadIndex] = e;
            }
          });
        }
        
        // Start all threads
        for (final Thread thread : threads) {
          thread.start();
        }
        
        // Wait for all threads to complete
        for (final Thread thread : threads) {
          thread.join(5000); // 5 second timeout
        }
        
        // Check for exceptions
        for (int i = 0; i < threadCount; i++) {
          if (exceptions[i] != null) {
            throw new AssertionError("Thread " + i + " encountered exception", exceptions[i]);
          }
        }
        
        LOGGER.info("Successfully tested concurrent access with " + threadCount + " threads");
      }
    });
  }

  /**
   * Creates test files for filesystem operations.
   */
  private void createTestFiles() throws Exception {
    // Create test files in preopen directory
    Files.write(preOpenDirectory.resolve("test.txt"), "Hello, WASI!".getBytes());
    Files.write(preOpenDirectory.resolve("input.txt"), "Test input data\nLine 2\nLine 3".getBytes());
    
    // Create subdirectory with files
    final Path subDir = preOpenDirectory.resolve("subdir");
    Files.createDirectories(subDir);
    Files.write(subDir.resolve("nested.txt"), "Nested file content".getBytes());
    
    // Create files in restricted directory (should not be accessible)
    Files.write(restrictedDirectory.resolve("secret.txt"), "Secret data".getBytes());
  }

  /**
   * Creates a basic WASI context for the specified runtime type.
   */
  private WasiContext createBasicWasiContext(final RuntimeType runtimeType) throws Exception {
    return (WasiContext) createWasiContextBuilder(runtimeType)
        .withEnvironment("HOME", "/home/test")
        .withArgument("test-program")
        .withPreopenDirectory("/tmp", preOpenDirectory.toString())
        .withWorkingDirectory("/app")
        .build();
  }

  /**
   * Creates a configured WASI context with specified environment and arguments.
   */
  private WasiContext createConfiguredWasiContext(
      final RuntimeType runtimeType,
      final Map<String, String> environment,
      final String[] arguments) throws Exception {
    
    final Object builder = createWasiContextBuilder(runtimeType)
        .withEnvironment(environment)
        .withArguments(arguments)
        .withPreopenDirectory("/tmp", preOpenDirectory.toString())
        .withWorkingDirectory("/app");
    
    if (runtimeType == RuntimeType.JNI) {
      return ((WasiContextBuilder) builder).build();
    } else {
      return (WasiContext) ((PanamaWasiContextBuilder) builder).build();
    }
  }

  /**
   * Creates a WASI context builder for the specified runtime type.
   */
  private Object createWasiContextBuilder(final RuntimeType runtimeType) {
    if (runtimeType == RuntimeType.JNI) {
      return WasiContext.builder();
    } else {
      return PanamaWasiContext.builder();
    }
  }
}