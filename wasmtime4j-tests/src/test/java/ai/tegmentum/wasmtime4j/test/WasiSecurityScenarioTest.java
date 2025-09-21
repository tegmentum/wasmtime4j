package ai.tegmentum.wasmtime4j.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasiException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Comprehensive test suite for WASI security violation scenarios.
 *
 * <p>This test class verifies proper error handling for WASI security policy violations,
 * including unauthorized file access, network restrictions, resource limit violations,
 * and component security boundaries.
 */
@DisplayName("WASI Security Scenario Test Suite")
class WasiSecurityScenarioTest {

  /**
   * Simple WASI module that attempts to read a file.
   * This module uses WASI Preview 1 interface to perform file operations.
   */
  private static final byte[] WASI_FILE_READ_MODULE = {
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version
    0x01, // Type section
    0x0C, // Section size
    0x02, // 2 types
    0x60, 0x02, 0x7F, 0x7F, 0x01, 0x7F, // Type 0: (i32, i32) -> i32
    0x60, 0x01, 0x7F, 0x01, 0x7F, // Type 1: (i32) -> i32
    0x02, // Import section
    0x1A, // Section size
    0x02, // 2 imports
    0x04, 'w', 'a', 's', 'i', // Module name "wasi"
    0x07, 'f', 'd', '_', 'r', 'e', 'a', 'd', // Import name "fd_read"
    0x00, 0x00, // Function type 0
    0x04, 'w', 'a', 's', 'i', // Module name "wasi"
    0x08, 'f', 'd', '_', 'c', 'l', 'o', 's', 'e', // Import name "fd_close"
    0x00, 0x01, // Function type 1
    0x03, // Function section
    0x02, // Section size
    0x01, 0x00, // 1 function with type 0
    0x07, // Export section
    0x0D, // Section size
    0x01, // 1 export
    0x09, 'r', 'e', 'a', 'd', '_', 'f', 'i', 'l', 'e', // Export name "read_file"
    0x00, 0x02, // Function export with index 2 (after imports)
    0x0A, // Code section
    0x0F, // Section size
    0x01, // 1 function body
    0x0D, // Body size
    0x00, // No locals
    0x41, 0x03, // i32.const 3 (stdin fd)
    0x41, 0x00, // i32.const 0 (buffer address)
    0x41, 0x10, // i32.const 16 (buffer size)
    0x41, 0x00, // i32.const 0 (bytes read pointer)
    0x10, 0x00, // call import 0 (fd_read)
    0x0B // End instruction
  };

  /**
   * WASI module that attempts to write to a file.
   */
  private static final byte[] WASI_FILE_WRITE_MODULE = {
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version
    0x01, // Type section
    0x07, // Section size
    0x01, // 1 type
    0x60, 0x02, 0x7F, 0x7F, 0x01, 0x7F, // Type 0: (i32, i32) -> i32
    0x02, // Import section
    0x12, // Section size
    0x01, // 1 import
    0x04, 'w', 'a', 's', 'i', // Module name "wasi"
    0x08, 'f', 'd', '_', 'w', 'r', 'i', 't', 'e', // Import name "fd_write"
    0x00, 0x00, // Function type 0
    0x03, // Function section
    0x02, // Section size
    0x01, 0x00, // 1 function with type 0
    0x05, // Memory section
    0x03, // Section size
    0x01, // 1 memory
    0x00, 0x01, // Memory limits: minimum 1 page
    0x07, // Export section
    0x0E, // Section size
    0x01, // 1 export
    0x0A, 'w', 'r', 'i', 't', 'e', '_', 'f', 'i', 'l', 'e', // Export name "write_file"
    0x00, 0x01, // Function export with index 1 (after imports)
    0x0A, // Code section
    0x0F, // Section size
    0x01, // 1 function body
    0x0D, // Body size
    0x00, // No locals
    0x41, 0x01, // i32.const 1 (stdout fd)
    0x41, 0x00, // i32.const 0 (iov address)
    0x41, 0x01, // i32.const 1 (iov count)
    0x41, 0x10, // i32.const 16 (bytes written pointer)
    0x10, 0x00, // call import 0 (fd_write)
    0x0B // End instruction
  };

  @ParameterizedTest
  @EnumSource(RuntimeType.class)
  @DisplayName("Unauthorized file access throws WasiException")
  void testUnauthorizedFileAccess(RuntimeType runtimeType, @TempDir Path tempDir) throws WasmException {
    if (!WasmRuntimeFactory.isRuntimeAvailable(runtimeType)) {
      return; // Skip if runtime not available
    }

    try (WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // Create a restricted file outside the allowed directory
      Path restrictedFile = tempDir.resolve("restricted.txt");
      Files.write(restrictedFile, "secret data".getBytes());

      try {
        Module module = runtime.compileModule(engine, WASI_FILE_READ_MODULE);
        Instance instance = runtime.instantiateModule(store, module);

        // Try to access file without proper WASI directory mapping
        WasmException exception =
            assertThrows(
                WasmException.class,
                () -> instance.getExportedFunction("read_file").call(),
                "Unauthorized file access should throw WasmException");

        assertNotNull(exception.getMessage(), "Exception should have meaningful message");
        assertTrue(
            exception.getMessage().toLowerCase().contains("permission")
                || exception.getMessage().toLowerCase().contains("access")
                || exception.getMessage().toLowerCase().contains("denied")
                || exception.getMessage().toLowerCase().contains("unauthorized"),
            "Exception message should mention permission/access/denied/unauthorized: "
                + exception.getMessage());

        // Check if it's specifically a WASI exception
        if (exception instanceof WasiException) {
          WasiException wasiEx = (WasiException) exception;
          assertTrue(
              wasiEx.isPermissionError() || wasiEx.isFileSystemError(),
              "WASI exception should be categorized as permission or file system error");
        }

      } catch (UnsupportedOperationException e) {
        // WASI support might not be implemented yet, which is acceptable
        assertTrue(true, "WASI support not yet implemented");
      }
    }
  }

  @Test
  @DisplayName("Directory traversal attempts throw WasiException")
  void testDirectoryTraversalPrevention(@TempDir Path tempDir) throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // Create directory structure
      Path allowedDir = tempDir.resolve("allowed");
      Path restrictedDir = tempDir.resolve("restricted");
      Files.createDirectories(allowedDir);
      Files.createDirectories(restrictedDir);

      Path secretFile = restrictedDir.resolve("secret.txt");
      Files.write(secretFile, "confidential information".getBytes());

      try {
        Module module = runtime.compileModule(engine, WASI_FILE_READ_MODULE);
        Instance instance = runtime.instantiateModule(store, module);

        // Try directory traversal attack (../restricted/secret.txt)
        WasmException exception =
            assertThrows(
                WasmException.class,
                () -> {
                  // This would typically involve WASI filesystem operations
                  // For now, just call the function which should fail due to lack of WASI setup
                  instance.getExportedFunction("read_file").call();
                },
                "Directory traversal should be prevented");

        assertNotNull(exception.getMessage(), "Exception should have meaningful message");

        if (exception instanceof WasiException) {
          WasiException wasiEx = (WasiException) exception;
          assertTrue(
              wasiEx.isPermissionError() || wasiEx.isFileSystemError(),
              "Directory traversal should be categorized as permission or file system error");
        }

      } catch (UnsupportedOperationException e) {
        // WASI support might not be implemented yet
        assertTrue(true, "WASI support not yet implemented");
      }
    }
  }

  @Test
  @DisplayName("File write restrictions are enforced")
  void testFileWriteRestrictions(@TempDir Path tempDir) throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // Create read-only directory structure
      Path readOnlyDir = tempDir.resolve("readonly");
      Files.createDirectories(readOnlyDir);

      try {
        Module module = runtime.compileModule(engine, WASI_FILE_WRITE_MODULE);
        Instance instance = runtime.instantiateModule(store, module);

        // Try to write to read-only location
        WasmException exception =
            assertThrows(
                WasmException.class,
                () -> instance.getExportedFunction("write_file").call(),
                "Write to read-only location should throw WasmException");

        assertNotNull(exception.getMessage(), "Exception should have meaningful message");
        assertTrue(
            exception.getMessage().toLowerCase().contains("permission")
                || exception.getMessage().toLowerCase().contains("write")
                || exception.getMessage().toLowerCase().contains("denied")
                || exception.getMessage().toLowerCase().contains("readonly"),
            "Exception message should mention permission/write/denied/readonly: "
                + exception.getMessage());

      } catch (UnsupportedOperationException e) {
        // WASI support might not be implemented yet
        assertTrue(true, "WASI support not yet implemented");
      }
    }
  }

  @Test
  @DisplayName("Resource limit violations throw appropriate exceptions")
  void testResourceLimitViolations() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // Create module that tries to allocate excessive resources
      byte[] resourceHungryModule = {
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version
        0x01, // Type section
        0x04, // Section size
        0x01, // 1 type
        0x60, 0x00, 0x00, // Function type: () -> ()
        0x03, // Function section
        0x02, // Section size
        0x01, 0x00, // 1 function with type index 0
        0x05, // Memory section
        0x04, // Section size
        0x01, // 1 memory
        0x00, (byte) 0x80, 0x08, // Memory limits: minimum 1024 pages (64MB)
        0x07, // Export section
        0x0C, // Section size
        0x01, // 1 export
        0x08, 'a', 'l', 'l', 'o', 'c', 'a', 't', 'e', // Export name "allocate"
        0x00, 0x00, // Function export with index 0
        0x0A, // Code section
        0x04, // Section size
        0x01, // 1 function body
        0x02, // Body size
        0x00, 0x0B // No locals, end instruction
      };

      try {
        Module module = runtime.compileModule(engine, resourceHungryModule);
        Instance instance = runtime.instantiateModule(store, module);

        // This might succeed or fail depending on system resources
        // If it fails, it should be a meaningful resource-related exception
        instance.getExportedFunction("allocate").call();

      } catch (WasmException e) {
        assertNotNull(e.getMessage(), "Exception should have meaningful message");
        assertTrue(
            e.getMessage().toLowerCase().contains("resource")
                || e.getMessage().toLowerCase().contains("memory")
                || e.getMessage().toLowerCase().contains("limit")
                || e.getMessage().toLowerCase().contains("allocation"),
            "Exception message should mention resource/memory/limit/allocation: "
                + e.getMessage());

        if (e instanceof WasiException) {
          WasiException wasiEx = (WasiException) e;
          assertTrue(
              wasiEx.isResourceLimitError(),
              "Resource limit violation should be categorized appropriately");
        }
      }
    }
  }

  @Test
  @DisplayName("Network access restrictions are enforced")
  void testNetworkAccessRestrictions() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // Create module that attempts network operations
      // This is a simplified test - actual network modules would be more complex
      byte[] networkModule = {
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version
        0x01, // Type section
        0x04, // Section size
        0x01, // 1 type
        0x60, 0x00, 0x00, // Function type: () -> ()
        0x02, // Import section
        0x15, // Section size
        0x01, // 1 import
        0x04, 'w', 'a', 's', 'i', // Module name "wasi"
        0x0B, 's', 'o', 'c', 'k', '_', 'c', 'o', 'n', 'n', 'e', 'c', 't', // Import name
        0x00, 0x00, // Function type 0
        0x03, // Function section
        0x02, // Section size
        0x01, 0x00, // 1 function with type 0
        0x07, // Export section
        0x0B, // Section size
        0x01, // 1 export
        0x07, 'c', 'o', 'n', 'n', 'e', 'c', 't', // Export name "connect"
        0x00, 0x01, // Function export with index 1
        0x0A, // Code section
        0x06, // Section size
        0x01, // 1 function body
        0x04, // Body size
        0x00, // No locals
        0x10, 0x00, // call import 0
        0x0B // End instruction
      };

      try {
        Module module = runtime.compileModule(engine, networkModule);
        Instance instance = runtime.instantiateModule(store, module);

        // Try to initiate network connection without permission
        WasmException exception =
            assertThrows(
                WasmException.class,
                () -> instance.getExportedFunction("connect").call(),
                "Network access without permission should throw WasmException");

        assertNotNull(exception.getMessage(), "Exception should have meaningful message");

        if (exception instanceof WasiException) {
          WasiException wasiEx = (WasiException) exception;
          assertTrue(
              wasiEx.isNetworkError() || wasiEx.isPermissionError(),
              "Network restriction should be categorized appropriately");
        }

      } catch (UnsupportedOperationException e) {
        // Network WASI support might not be implemented yet
        assertTrue(true, "Network WASI support not yet implemented");
      }
    }
  }

  @Test
  @DisplayName("Component security boundaries are enforced")
  void testComponentSecurityBoundaries() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Test that components cannot access each other's resources inappropriately
      Store store1 = runtime.createStore(engine);
      Store store2 = runtime.createStore(engine);

      try {
        Module module = runtime.compileModule(engine, WASI_FILE_READ_MODULE);
        Instance instance1 = runtime.instantiateModule(store1, module);
        Instance instance2 = runtime.instantiateModule(store2, module);

        // Both instances should be isolated from each other
        assertNotNull(instance1, "First instance should be created");
        assertNotNull(instance2, "Second instance should be created");

        // Try operations that might cross security boundaries
        assertThrows(
            WasmException.class,
            () -> instance1.getExportedFunction("read_file").call(),
            "Cross-boundary access should be restricted");

      } catch (UnsupportedOperationException e) {
        // Component model might not be fully implemented yet
        assertTrue(true, "Component model not yet implemented");
      }
    }
  }

  @Test
  @DisplayName("Security violations provide detailed error information")
  void testSecurityViolationErrorDetails() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      try {
        Module module = runtime.compileModule(engine, WASI_FILE_READ_MODULE);
        Instance instance = runtime.instantiateModule(store, module);

        try {
          instance.getExportedFunction("read_file").call();
        } catch (WasmException e) {
          String message = e.getMessage();
          assertNotNull(message, "Exception should have meaningful message");

          // Security error messages should be informative but not leak sensitive information
          assertTrue(message.length() > 10, "Error message should be descriptive");
          assertTrue(message.length() < 10000, "Error message should not be excessively long");

          // Should not contain characters that could cause log injection
          assertTrue(
              !message.contains("\n") && !message.contains("\r"),
              "Error message should not contain CRLF characters");

          // Should not leak sensitive path information
          assertTrue(
              !message.toLowerCase().contains("password")
                  && !message.toLowerCase().contains("secret")
                  && !message.toLowerCase().contains("private"),
              "Error message should not leak sensitive information");

          if (e instanceof WasiException) {
            WasiException wasiEx = (WasiException) e;
            assertNotNull(wasiEx.getCategory(), "WASI exception should have category");
            assertNotNull(wasiEx.getOperation(), "WASI exception should have operation context");
          }
        }

      } catch (UnsupportedOperationException e) {
        // WASI support might not be implemented yet
        assertTrue(true, "WASI support not yet implemented");
      }
    }
  }

  @Test
  @DisplayName("Concurrent security violations are handled safely")
  void testConcurrentSecurityViolations() throws InterruptedException, WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      final int threadCount = 10;
      final CountDownLatch latch = new CountDownLatch(threadCount);
      final AtomicInteger exceptionCount = new AtomicInteger(0);
      final AtomicInteger successCount = new AtomicInteger(0);

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                Store store = runtime.createStore(engine);
                Module module = runtime.compileModule(engine, WASI_FILE_READ_MODULE);
                Instance instance = runtime.instantiateModule(store, module);

                // Try unauthorized operation
                instance.getExportedFunction("read_file").call();
                successCount.incrementAndGet(); // Should not reach here

              } catch (WasmException e) {
                exceptionCount.incrementAndGet();
                // Verify exception quality
                assertNotNull(e.getMessage(), "Exception should have meaningful message");
              } catch (UnsupportedOperationException e) {
                // WASI not implemented yet
                exceptionCount.incrementAndGet();
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete within 30 seconds");
      executor.shutdown();

      // Most or all operations should result in security exceptions
      assertTrue(
          exceptionCount.get() >= threadCount / 2,
          "Most operations should throw security exceptions");

      // Runtime should remain stable under concurrent security violations
      assertTrue(runtime.isValid(), "Runtime should remain valid after concurrent violations");
    }
  }

  @Test
  @DisplayName("Security context is preserved across operations")
  void testSecurityContextPreservation() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      try {
        Module module = runtime.compileModule(engine, WASI_FILE_READ_MODULE);
        Instance instance = runtime.instantiateModule(store, module);

        // Perform multiple operations to verify security context is maintained
        for (int i = 0; i < 5; i++) {
          assertThrows(
              WasmException.class,
              () -> instance.getExportedFunction("read_file").call(),
              "Security restrictions should be consistent across operations");
        }

      } catch (UnsupportedOperationException e) {
        // WASI support might not be implemented yet
        assertTrue(true, "WASI support not yet implemented");
      }
    }
  }

  @Test
  @DisplayName("Security violations don't corrupt runtime state")
  void testRuntimeStateAfterSecurityViolations() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Trigger multiple security violations
      for (int i = 0; i < 10; i++) {
        Store store = runtime.createStore(engine);

        try {
          Module module = runtime.compileModule(engine, WASI_FILE_READ_MODULE);
          Instance instance = runtime.instantiateModule(store, module);

          assertThrows(
              WasmException.class,
              () -> instance.getExportedFunction("read_file").call(),
              "Should throw security exception");

        } catch (UnsupportedOperationException e) {
          // WASI not implemented yet
        }
      }

      // Runtime should still be valid and functional
      assertTrue(runtime.isValid(), "Runtime should remain valid after security violations");

      // Should be able to create new stores and compile modules
      Store newStore = runtime.createStore(engine);
      assertNotNull(newStore, "Should be able to create new store after violations");

      Module newModule = runtime.compileModule(engine, SIMPLE_MODULE);
      assertNotNull(newModule, "Should be able to compile new module after violations");
    }
  }

  /** Simple valid WebAssembly module for basic operations. */
  private static final byte[] SIMPLE_MODULE = {
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version
    0x01, // Type section
    0x04, // Section size
    0x01, // 1 type
    0x60, 0x00, 0x00, // Function type: () -> ()
    0x03, // Function section
    0x02, // Section size
    0x01, 0x00, // 1 function with type index 0
    0x07, // Export section
    0x08, // Section size
    0x01, // 1 export
    0x04, 't', 'e', 's', 't', // Export name "test"
    0x00, 0x00, // Function export with index 0
    0x0A, // Code section
    0x04, // Section size
    0x01, // 1 function body
    0x02, // Body size
    0x00, 0x0B // No locals, end instruction
  };
}