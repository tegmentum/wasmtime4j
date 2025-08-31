package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmEngine;
import ai.tegmentum.wasmtime4j.WasmInstance;
import ai.tegmentum.wasmtime4j.WasmModule;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmStore;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiFileOperation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiFileSystemException;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiPermissionException;
import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import ai.tegmentum.wasmtime4j.webassembly.WasmTestModules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for WASI filesystem access controls and security boundaries.
 * 
 * This test class validates:
 * - Filesystem access control and permission validation
 * - Security boundary enforcement and path traversal prevention
 * - Preopen directory access controls
 * - File operation permissions and restrictions
 * - Cross-runtime filesystem behavior consistency
 */
@Tag(TestCategories.WASI_FILESYSTEM)
@Tag(TestCategories.SECURITY_TESTING)
@Tag(TestCategories.COMPREHENSIVE_TESTING)
@DisplayName("WASI Filesystem Access Control Tests")
@Timeout(value = 5, unit = TimeUnit.MINUTES)
public final class WasiFilesystemTest extends BaseIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(WasiFilesystemTest.class.getName());

  @TempDir
  private Path tempDirectory;

  private Path allowedDirectory;
  private Path restrictedDirectory;
  private Path readOnlyDirectory;
  private Path nestedDirectory;

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    super.doSetUp(testInfo);

    try {
      // Set up filesystem test structure
      allowedDirectory = tempDirectory.resolve("allowed");
      restrictedDirectory = tempDirectory.resolve("restricted");
      readOnlyDirectory = tempDirectory.resolve("readonly");
      nestedDirectory = allowedDirectory.resolve("nested").resolve("deep");

      Files.createDirectories(allowedDirectory);
      Files.createDirectories(restrictedDirectory);
      Files.createDirectories(readOnlyDirectory);
      Files.createDirectories(nestedDirectory);

      // Create test files with various permissions
      createTestFilesystem();

      LOGGER.info("WASI filesystem test environment initialized:");
      LOGGER.info("  Allowed: " + allowedDirectory);
      LOGGER.info("  Restricted: " + restrictedDirectory);
      LOGGER.info("  Read-only: " + readOnlyDirectory);
      LOGGER.info("  Nested: " + nestedDirectory);

    } catch (final Exception e) {
      throw new RuntimeException("Failed to initialize WASI filesystem test environment", e);
    }
  }

  /**
   * Tests basic filesystem access controls within preopen directories.
   * Validates that files within preopen directories can be accessed.
   */
  @Test
  @DisplayName("Filesystem Access Control - Preopen Directory Access")
  void testPreopenDirectoryAccess() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing preopen directory access with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final WasiContext context = createWasiContextWithPreopen(runtimeType);
        registerForCleanup(context);

        // Test access to files within preopen directory
        final Path allowedFile = context.validatePath("/allowed/test.txt");
        assertNotNull(allowedFile, "Should be able to validate path within preopen directory");
        assertTrue(Files.exists(allowedFile), "Validated path should exist on filesystem");

        // Test access to subdirectory within preopen
        final Path nestedFile = context.validatePath("/allowed/nested/deep/nested.txt");
        assertNotNull(nestedFile, "Should be able to validate nested path within preopen directory");
        assertTrue(Files.exists(nestedFile), "Nested validated path should exist on filesystem");

        // Test validation of different file operations
        final Path readFile = context.validatePath("/allowed/test.txt", WasiFileOperation.READ);
        assertNotNull(readFile, "Should be able to validate read access");

        final Path writeFile = context.validatePath("/allowed/writable.txt", WasiFileOperation.WRITE);
        assertNotNull(writeFile, "Should be able to validate write access");

        LOGGER.info("Successfully validated preopen directory access");
      }
    });
  }

  /**
   * Tests filesystem access restrictions outside preopen directories.
   * Validates that access to non-preopen directories is properly blocked.
   */
  @Test
  @DisplayName("Filesystem Security - Access Restriction Enforcement")
  void testFilesystemAccessRestrictions() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing filesystem access restrictions with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final WasiContext context = createWasiContextWithPreopen(runtimeType);
        registerForCleanup(context);

        // Test access to restricted directory (not preopen)
        assertThrows(WasiPermissionException.class, () -> {
          context.validatePath(restrictedDirectory.toString() + "/secret.txt");
        }, "Should not be able to access files outside preopen directories");

        // Test access to system directories
        assertThrows(WasiPermissionException.class, () -> {
          context.validatePath("/etc/passwd");
        }, "Should not be able to access system files");

        assertThrows(WasiPermissionException.class, () -> {
          context.validatePath("/root/.ssh/id_rsa");
        }, "Should not be able to access sensitive system files");

        // Test access to parent directory of preopen
        assertThrows(WasiPermissionException.class, () -> {
          context.validatePath(tempDirectory.toString() + "/other.txt");
        }, "Should not be able to access parent directory of preopen");

        LOGGER.info("Successfully validated filesystem access restrictions");
      }
    });
  }

  /**
   * Tests path traversal attack prevention.
   * Validates that path traversal attempts are properly blocked.
   */
  @Test
  @DisplayName("Security Validation - Path Traversal Attack Prevention")
  void testPathTraversalPrevention() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing path traversal prevention with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final WasiContext context = createWasiContextWithPreopen(runtimeType);
        registerForCleanup(context);

        // Test various path traversal attack patterns
        final String[] traversalAttacks = {
            "../secret.txt",
            "../../etc/passwd",
            "/allowed/../restricted/secret.txt",
            "/allowed/./../../restricted/secret.txt",
            "/allowed/nested/../../../restricted/secret.txt",
            "..\\..\\windows\\system32\\config\\sam", // Windows-style
            "/allowed/nested/..%2F..%2F..%2Frestricted%2Fsecret.txt", // URL encoded
            "/allowed/nested/..%252F..%252F..%252Frestricted%252Fsecret.txt" // Double encoded
        };

        for (final String attackPath : traversalAttacks) {
          assertThrows(Exception.class, () -> {
            context.validatePath(attackPath);
          }, "Path traversal attack should be blocked: " + attackPath);
        }

        // Test symbolic link traversal prevention
        if (!TestUtils.isWindows()) {
          final Path symlink = allowedDirectory.resolve("symlink");
          Files.createSymbolicLink(symlink, restrictedDirectory);
          
          assertThrows(Exception.class, () -> {
            context.validatePath("/allowed/symlink/secret.txt");
          }, "Symbolic link traversal should be blocked");
        }

        LOGGER.info("Successfully validated path traversal prevention");
      }
    });
  }

  /**
   * Tests file operation permission validation.
   * Validates that different file operations are properly controlled.
   */
  @Test
  @DisplayName("File Operations - Permission Validation by Operation Type")
  void testFileOperationPermissions() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing file operation permissions with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final WasiContext context = createWasiContextWithPreopen(runtimeType);
        registerForCleanup(context);

        // Test read permissions on readable files
        assertDoesNotThrow(() -> {
          context.validatePath("/allowed/test.txt", WasiFileOperation.READ);
        }, "Should be able to read allowed files");

        // Test write permissions on writable files
        assertDoesNotThrow(() -> {
          context.validatePath("/allowed/writable.txt", WasiFileOperation.WRITE);
        }, "Should be able to write to allowed files");

        // Test read permissions on read-only files
        assertDoesNotThrow(() -> {
          context.validatePath("/readonly/readonly.txt", WasiFileOperation.READ);
        }, "Should be able to read read-only files");

        // Test write permissions on read-only files (should fail)
        if (!TestUtils.isWindows()) { // Windows doesn't enforce read-only at filesystem level
          assertThrows(WasiPermissionException.class, () -> {
            context.validatePath("/readonly/readonly.txt", WasiFileOperation.WRITE);
          }, "Should not be able to write to read-only files");
        }

        // Test execute permissions
        if (!TestUtils.isWindows()) {
          assertDoesNotThrow(() -> {
            context.validatePath("/allowed/executable.sh", WasiFileOperation.EXECUTE);
          }, "Should be able to validate execute permission on executable files");
        }

        // Test directory operations
        assertDoesNotThrow(() -> {
          context.validatePath("/allowed/nested", WasiFileOperation.LIST_DIRECTORY);
        }, "Should be able to list allowed directories");

        LOGGER.info("Successfully validated file operation permissions");
      }
    });
  }

  /**
   * Tests filesystem boundary enforcement with edge cases.
   * Validates boundary conditions and edge cases in filesystem access.
   */
  @Test
  @DisplayName("Filesystem Boundaries - Edge Case and Boundary Condition Testing")
  void testFilesystemBoundaryEdgeCases() {
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing filesystem boundary edge cases with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final WasiContext context = createWasiContextWithPreopen(runtimeType);
        registerForCleanup(context);

        // Test empty path
        assertThrows(IllegalArgumentException.class, () -> {
          context.validatePath("");
        }, "Empty path should be rejected");

        // Test null path
        assertThrows(IllegalArgumentException.class, () -> {
          context.validatePath(null);
        }, "Null path should be rejected");

        // Test root path access
        assertThrows(WasiPermissionException.class, () -> {
          context.validatePath("/");
        }, "Root path access should be restricted");

        // Test current directory references
        final Path currentDir = context.validatePath("/allowed/./test.txt");
        assertNotNull(currentDir, "Current directory reference should be normalized");
        assertTrue(Files.exists(currentDir), "Normalized path should exist");

        // Test very long paths
        final StringBuilder longPath = new StringBuilder("/allowed/");
        for (int i = 0; i < 100; i++) {
          longPath.append("very_long_directory_name_").append(i).append("/");
        }
        longPath.append("file.txt");
        
        assertThrows(Exception.class, () -> {
          context.validatePath(longPath.toString());
        }, "Very long paths should be rejected or fail validation");

        // Test paths with special characters
        assertDoesNotThrow(() -> {
          context.validatePath("/allowed/file with spaces.txt");
        }, "Paths with spaces should be handled correctly");

        // Test Unicode paths
        assertDoesNotThrow(() -> {
          context.validatePath("/allowed/файл.txt"); // Cyrillic
        }, "Unicode paths should be handled correctly");

        LOGGER.info("Successfully validated filesystem boundary edge cases");
      }
    });
  }

  /**
   * Tests concurrent filesystem access and thread safety.
   * Validates that filesystem operations are thread-safe.
   */
  @Test
  @DisplayName("Filesystem Thread Safety - Concurrent Access Validation")
  void testFilesystemConcurrentAccess() {
    skipIfCategoryNotEnabled(TestCategories.PERFORMANCE_TESTING);
    
    runWithBothRuntimes((runtime, runtimeType) -> {
      LOGGER.info("Testing concurrent filesystem access with " + runtimeType + " runtime");

      try (final WasmEngine engine = runtime.createEngine()) {
        
        final WasiContext context = createWasiContextWithPreopen(runtimeType);
        registerForCleanup(context);

        final int threadCount = 10;
        final Thread[] threads = new Thread[threadCount];
        final Exception[] exceptions = new Exception[threadCount];

        for (int i = 0; i < threadCount; i++) {
          final int threadIndex = i;
          threads[i] = new Thread(() -> {
            try {
              for (int j = 0; j < 50; j++) {
                // Test various filesystem operations concurrently
                assertNotNull(context.validatePath("/allowed/test.txt"), 
                    "Thread " + threadIndex + " should be able to validate allowed paths");
                
                assertThrows(Exception.class, () -> {
                  context.validatePath("/restricted/secret.txt");
                }, "Thread " + threadIndex + " should be blocked from restricted paths");

                // Test different file operations
                assertNotNull(context.validatePath("/allowed/test.txt", WasiFileOperation.READ),
                    "Thread " + threadIndex + " should be able to validate read operations");
                
                assertNotNull(context.validatePath("/allowed/writable.txt", WasiFileOperation.WRITE),
                    "Thread " + threadIndex + " should be able to validate write operations");
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

        // Wait for completion
        for (final Thread thread : threads) {
          thread.join(10000); // 10 second timeout
        }

        // Check for exceptions
        for (int i = 0; i < threadCount; i++) {
          if (exceptions[i] != null) {
            throw new AssertionError("Thread " + i + " encountered exception", exceptions[i]);
          }
        }

        LOGGER.info("Successfully tested concurrent filesystem access with " + threadCount + " threads");
      }
    });
  }

  /**
   * Creates the test filesystem structure with various permissions.
   */
  private void createTestFilesystem() throws Exception {
    // Create files in allowed directory
    Files.write(allowedDirectory.resolve("test.txt"), "Test file content".getBytes());
    Files.write(allowedDirectory.resolve("writable.txt"), "Writable file content".getBytes());
    Files.write(nestedDirectory.resolve("nested.txt"), "Nested file content".getBytes());

    // Create executable file if not on Windows
    if (!TestUtils.isWindows()) {
      final Path executable = allowedDirectory.resolve("executable.sh");
      Files.write(executable, "#!/bin/bash\necho 'Hello World'\n".getBytes());
      Files.setPosixFilePermissions(executable, PosixFilePermissions.fromString("rwxr--r--"));
    }

    // Create read-only files
    final Path readOnlyFile = readOnlyDirectory.resolve("readonly.txt");
    Files.write(readOnlyFile, "Read-only content".getBytes());
    if (!TestUtils.isWindows()) {
      Files.setPosixFilePermissions(readOnlyFile, PosixFilePermissions.fromString("r--r--r--"));
    }

    // Create files in restricted directory
    Files.write(restrictedDirectory.resolve("secret.txt"), "Secret content".getBytes());
    Files.write(restrictedDirectory.resolve("config.ini"), "password=secret123".getBytes());
  }

  /**
   * Creates a WASI context with preopen directory configuration.
   */
  private WasiContext createWasiContextWithPreopen(final RuntimeType runtimeType) throws Exception {
    if (runtimeType == RuntimeType.JNI) {
      return ai.tegmentum.wasmtime4j.jni.wasi.WasiContext.builder()
          .withEnvironment("HOME", "/home/test")
          .withArgument("test-program")
          .withPreopenDirectory("/allowed", allowedDirectory.toString())
          .withPreopenDirectory("/readonly", readOnlyDirectory.toString())
          .withWorkingDirectory("/app")
          .build();
    } else {
      return (WasiContext) ai.tegmentum.wasmtime4j.panama.wasi.WasiContext.builder()
          .withEnvironment("HOME", "/home/test")
          .withArgument("test-program")
          .withPreopenDirectory("/allowed", allowedDirectory.toString())
          .withPreopenDirectory("/readonly", readOnlyDirectory.toString())
          .withWorkingDirectory("/app")
          .build();
    }
  }
}