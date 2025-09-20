package ai.tegmentum.wasmtime4j.wasi.impl;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WasiResourceException;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceLimits;
import ai.tegmentum.wasmtime4j.wasi.WasiResourcePermissions;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test suite for WASI Filesystem Resource implementation.
 *
 * <p>Tests the WasiFileResourceImpl class to ensure proper filesystem operations,
 * sandboxing, permission enforcement, and error handling for file-based resources.
 *
 * <p>These tests validate filesystem operations including reading, writing, deletion,
 * and sandbox security to prevent path traversal attacks.
 */
@DisplayName("WASI Filesystem Resource Implementation Tests")
class WasiFileResourceImplTest {

  @TempDir
  Path tempDir;

  private WasiFileResourceImpl fileResource;
  private FilesystemResourceConfig config;

  @BeforeEach
  void setUp() throws IOException, WasmException {
    // Create test directory structure
    Files.createDirectories(tempDir.resolve("subdir"));
    Files.write(tempDir.resolve("test.txt"), "Hello, World!".getBytes());
    Files.write(tempDir.resolve("subdir/nested.txt"), "Nested content".getBytes());

    // Create filesystem resource configuration
    config = new FilesystemResourceConfig(tempDir.toString());

    // Create filesystem resource
    fileResource = new WasiFileResourceImpl(1L, "test-filesystem", config);
  }

  @AfterEach
  void tearDown() {
    if (fileResource != null) {
      fileResource.close();
    }
  }

  @Test
  @DisplayName("Filesystem resource should read files correctly")
  void testFileReading() throws WasmException {
    final byte[] buffer = new byte[1024];
    final int bytesRead = fileResource.readFile("test.txt", buffer, 0);

    assertEquals(13, bytesRead); // "Hello, World!" length
    assertEquals("Hello, World!", new String(buffer, 0, bytesRead));
  }

  @Test
  @DisplayName("Filesystem resource should read nested files correctly")
  void testNestedFileReading() throws WasmException {
    final byte[] buffer = new byte[1024];
    final int bytesRead = fileResource.readFile("subdir/nested.txt", buffer, 0);

    assertEquals(14, bytesRead); // "Nested content" length
    assertEquals("Nested content", new String(buffer, 0, bytesRead));
  }

  @Test
  @DisplayName("Filesystem resource should handle file reading with offset")
  void testFileReadingWithOffset() throws WasmException {
    final byte[] buffer = new byte[1024];
    final int bytesRead = fileResource.readFile("test.txt", buffer, 7); // Start from "World!"

    assertEquals(6, bytesRead); // "World!" length
    assertEquals("World!", new String(buffer, 0, bytesRead));
  }

  @Test
  @DisplayName("Filesystem resource should write files correctly")
  void testFileWriting() throws WasmException {
    final String content = "New file content";
    final byte[] data = content.getBytes();

    final int bytesWritten = fileResource.writeFile("newfile.txt", data, 0);
    assertEquals(data.length, bytesWritten);

    // Verify the file was created and has correct content
    final byte[] buffer = new byte[1024];
    final int bytesRead = fileResource.readFile("newfile.txt", buffer, 0);
    assertEquals(content.length(), bytesRead);
    assertEquals(content, new String(buffer, 0, bytesRead));
  }

  @Test
  @DisplayName("Filesystem resource should create parent directories when writing")
  void testFileWritingWithDirectoryCreation() throws WasmException {
    final String content = "Content in new directory";
    final byte[] data = content.getBytes();

    // This should create the parent directory
    final int bytesWritten = fileResource.writeFile("newdir/newfile.txt", data, 0);
    assertEquals(data.length, bytesWritten);

    // Verify the file was created
    final byte[] buffer = new byte[1024];
    final int bytesRead = fileResource.readFile("newdir/newfile.txt", buffer, 0);
    assertEquals(content.length(), bytesRead);
    assertEquals(content, new String(buffer, 0, bytesRead));
  }

  @Test
  @DisplayName("Filesystem resource should delete files correctly")
  void testFileDeletion() throws WasmException {
    // Verify file exists first
    assertTrue(Files.exists(tempDir.resolve("test.txt")));

    // Delete the file
    fileResource.deleteFile("test.txt");

    // Verify file no longer exists
    assertFalse(Files.exists(tempDir.resolve("test.txt")));
  }

  @Test
  @DisplayName("Filesystem resource should handle sandbox violations")
  void testSandboxViolation() {
    // Attempting to access files outside the sandbox should fail
    assertThrows(WasiResourceException.class, () -> {
      fileResource.readFile("../outside.txt", new byte[1024], 0);
    });

    assertThrows(WasiResourceException.class, () -> {
      fileResource.readFile("/etc/passwd", new byte[1024], 0);
    });

    assertThrows(WasiResourceException.class, () -> {
      fileResource.writeFile("../outside.txt", "data".getBytes(), 0);
    });
  }

  @Test
  @DisplayName("Filesystem resource should enforce read permissions")
  void testReadPermissionEnforcement() throws WasmException {
    // Create resource with write-only permissions
    final FilesystemResourceConfig writeOnlyConfig = new FilesystemResourceConfig(
        tempDir.toString(), WasiResourcePermissions.WRITE_ONLY);
    final WasiFileResourceImpl writeOnlyResource = new WasiFileResourceImpl(
        2L, "write-only", writeOnlyConfig);

    try {
      // Reading should fail with write-only permissions
      assertThrows(WasiResourceException.class, () -> {
        writeOnlyResource.readFile("test.txt", new byte[1024], 0);
      });
    } finally {
      writeOnlyResource.close();
    }
  }

  @Test
  @DisplayName("Filesystem resource should enforce write permissions")
  void testWritePermissionEnforcement() throws WasmException {
    // Create resource with read-only permissions
    final FilesystemResourceConfig readOnlyConfig = new FilesystemResourceConfig(
        tempDir.toString(), WasiResourcePermissions.READ_ONLY);
    final WasiFileResourceImpl readOnlyResource = new WasiFileResourceImpl(
        3L, "read-only", readOnlyConfig);

    try {
      // Writing should fail with read-only permissions
      assertThrows(WasiResourceException.class, () -> {
        readOnlyResource.writeFile("readonly.txt", "data".getBytes(), 0);
      });
    } finally {
      readOnlyResource.close();
    }
  }

  @Test
  @DisplayName("Filesystem resource should enforce delete permissions")
  void testDeletePermissionEnforcement() throws WasmException {
    // Create resource without delete permissions
    final FilesystemResourceConfig noDeleteConfig = new FilesystemResourceConfig(
        tempDir.toString(), WasiResourcePermissions.READ_WRITE);
    final WasiFileResourceImpl noDeleteResource = new WasiFileResourceImpl(
        4L, "no-delete", noDeleteConfig);

    try {
      // Deleting should fail without delete permissions
      assertThrows(WasiResourceException.class, () -> {
        noDeleteResource.deleteFile("test.txt");
      });
    } finally {
      noDeleteResource.close();
    }
  }

  @Test
  @DisplayName("Filesystem resource should handle nonexistent files")
  void testNonexistentFileHandling() {
    // Reading nonexistent file should fail
    assertThrows(WasiResourceException.class, () -> {
      fileResource.readFile("nonexistent.txt", new byte[1024], 0);
    });

    // Deleting nonexistent file should fail
    assertThrows(WasiResourceException.class, () -> {
      fileResource.deleteFile("nonexistent.txt");
    });
  }

  @Test
  @DisplayName("Filesystem resource should provide accurate statistics")
  void testFilesystemStatistics() throws WasmException {
    final WasiFileResourceImpl.FileSystemStats initialStats = fileResource.getFileSystemStats();
    assertEquals(0, initialStats.getBytesRead());
    assertEquals(0, initialStats.getBytesWritten());
    assertEquals(0, initialStats.getFileOperations());

    // Perform some operations
    fileResource.readFile("test.txt", new byte[1024], 0);
    fileResource.writeFile("newfile.txt", "test data".getBytes(), 0);

    final WasiFileResourceImpl.FileSystemStats updatedStats = fileResource.getFileSystemStats();
    assertEquals(13, updatedStats.getBytesRead()); // "Hello, World!" length
    assertEquals(9, updatedStats.getBytesWritten()); // "test data" length
    assertEquals(2, updatedStats.getFileOperations());
    assertEquals(tempDir.toString(), updatedStats.getRootPath());
  }

  @Test
  @DisplayName("Filesystem resource should support invoke operations")
  void testInvokeOperations() throws WasmException {
    // Test read_file operation
    final byte[] buffer = new byte[1024];
    final Integer bytesRead = (Integer) fileResource.invoke("read_file", "test.txt", buffer, 0L);
    assertEquals(13, bytesRead);

    // Test write_file operation
    final byte[] data = "invoke test".getBytes();
    final Integer bytesWritten = (Integer) fileResource.invoke("write_file", "invoke.txt", data, 0L);
    assertEquals(data.length, bytesWritten);

    // Test get_stats operation
    final WasiFileResourceImpl.FileSystemStats stats =
        (WasiFileResourceImpl.FileSystemStats) fileResource.invoke("get_stats");
    assertNotNull(stats);

    // Test get_root_path operation
    final String rootPath = (String) fileResource.invoke("get_root_path");
    assertEquals(tempDir.toString(), rootPath);
  }

  @Test
  @DisplayName("Filesystem resource should handle parameter validation")
  void testParameterValidation() {
    // Test null path
    assertThrows(WasiResourceException.class, () -> {
      fileResource.readFile(null, new byte[1024], 0);
    });

    assertThrows(WasiResourceException.class, () -> {
      fileResource.writeFile(null, "data".getBytes(), 0);
    });

    // Test empty path
    assertThrows(WasiResourceException.class, () -> {
      fileResource.readFile("", new byte[1024], 0);
    });

    // Test null buffer
    assertThrows(IllegalArgumentException.class, () -> {
      fileResource.invoke("read_file", "test.txt", null, 0L);
    });
  }

  @Test
  @DisplayName("Filesystem resource should handle invalid invoke operations")
  void testInvalidInvokeOperations() {
    // Test unsupported operation
    assertThrows(WasiResourceException.class, () -> {
      fileResource.invoke("unsupported_operation");
    });

    // Test operations with wrong parameters
    assertThrows(IllegalArgumentException.class, () -> {
      fileResource.invoke("read_file", "test.txt"); // Missing parameters
    });
  }

  @Test
  @DisplayName("Filesystem resource should handle resource cleanup")
  void testResourceCleanup() throws WasmException {
    assertTrue(fileResource.isValid());

    // Close the resource
    fileResource.close();

    // Resource should no longer be valid
    assertFalse(fileResource.isValid());

    // Operations should fail
    assertThrows(WasiResourceException.class, () -> {
      fileResource.readFile("test.txt", new byte[1024], 0);
    });
  }

  /**
   * Test implementation of WasiResourceConfig for filesystem resources.
   */
  private static class FilesystemResourceConfig implements WasiResourceConfig {
    private final String rootPath;
    private final Set<WasiResourcePermissions> permissions;

    public FilesystemResourceConfig(final String rootPath) {
      this(rootPath, WasiResourcePermissions.READ_WRITE);
    }

    public FilesystemResourceConfig(final String rootPath,
                                  final Set<WasiResourcePermissions> permissions) {
      this.rootPath = rootPath;
      this.permissions = permissions;
    }

    @Override
    public WasiResourceType getResourceType() {
      return WasiResourceType.FILESYSTEM;
    }

    @Override
    public Optional<String> getName() {
      return Optional.of("test-filesystem");
    }

    @Override
    public Optional<Object> getProperty(final String name) {
      if ("root_path".equals(name)) {
        return Optional.of(rootPath);
      }
      return Optional.empty();
    }

    @Override
    public Map<String, Object> getProperties() {
      final Map<String, Object> props = new HashMap<>();
      props.put("root_path", rootPath);
      return props;
    }

    @Override
    public boolean hasProperty(final String name) {
      return "root_path".equals(name);
    }

    @Override
    public Set<WasiResourcePermissions> getPermissions() {
      return permissions;
    }

    @Override
    public WasiResourceLimits getResourceLimits() {
      return new WasiResourceLimits() {
        @Override
        public int getMaxResources() {
          return 10;
        }

        @Override
        public long getMaxMemoryPerResource() {
          return 1024 * 1024;
        }

        @Override
        public long getTotalMaxMemory() {
          return 10 * 1024 * 1024;
        }

        @Override
        public int getMaxResourcesPerType() {
          return 5;
        }

        @Override
        public Map<String, Object> getCustomLimits() {
          return new HashMap<>();
        }
      };
    }

    @Override
    public Map<String, Object> getMetadata() {
      final Map<String, Object> metadata = new HashMap<>();
      metadata.put("root_path", rootPath);
      metadata.put("permissions", permissions);
      return metadata;
    }

    @Override
    public void validate() {
      if (rootPath == null || rootPath.trim().isEmpty()) {
        throw new IllegalArgumentException("Root path cannot be null or empty");
      }
    }

    @Override
    public boolean isCompatibleWith(final WasiResourceConfig other) {
      return other != null && other.getResourceType() == WasiResourceType.FILESYSTEM;
    }
  }
}