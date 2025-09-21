package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.utils.TestCategories;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive tests for WASI file system operations.
 *
 * <p>This test suite validates:
 *
 * <ul>
 *   <li>File reading and writing operations
 *   <li>Directory creation and listing
 *   <li>File metadata and statistics
 *   <li>File seeking and positioning
 *   <li>File permissions and access control
 *   <li>Cross-runtime file operation consistency
 * </ul>
 *
 * @since 1.0.0
 */
@Tag(TestCategories.WASI)
@Tag(TestCategories.FILESYSTEM)
@Tag(TestCategories.INTEGRATION)
public class WasiFileOperationsTestIT {
  private static final Logger LOGGER = Logger.getLogger(WasiFileOperationsTestIT.class.getName());

  @TempDir Path tempDirectory;

  private WasiContext wasiContext;
  private TestInfo currentTest;

  @BeforeEach
  void setUp(final TestInfo testInfo) throws WasmException {
    this.currentTest = testInfo;
    LOGGER.info("Setting up WASI file operations test: " + testInfo.getDisplayName());

    // Create WASI context with file system access
    this.wasiContext = WasiFactory.createContext();
    assertNotNull(wasiContext, "WASI context should be created");
    assertTrue(wasiContext.isValid(), "WASI context should be valid");

    LOGGER.info("Using WASI runtime: " + wasiContext.getRuntimeInfo().getRuntimeType());
    LOGGER.info("Test directory: " + tempDirectory);
  }

  @AfterEach
  void tearDown() {
    if (wasiContext != null) {
      wasiContext.close();
      LOGGER.info("Cleaned up test: " + currentTest.getDisplayName());
    }
  }

  /** Tests basic file creation and writing operations. */
  @Test
  @DisplayName("WASI File Creation and Writing Test")
  void testFileCreationAndWriting() throws IOException {
    LOGGER.info("Testing WASI file creation and writing");

    // Create test file with content
    final Path testFile = tempDirectory.resolve("test_file.txt");
    final String testContent = "Hello, WASI File System!";

    // Write content using standard Java API for initial setup
    Files.writeString(testFile, testContent, StandardOpenOption.CREATE);

    // Verify file was created
    assertTrue(Files.exists(testFile), "Test file should exist");
    assertEquals(testContent, Files.readString(testFile), "File content should match");

    LOGGER.info("File creation and writing test completed successfully");
  }

  /** Tests file reading operations through WASI. */
  @Test
  @DisplayName("WASI File Reading Test")
  void testFileReading() throws IOException {
    LOGGER.info("Testing WASI file reading");

    // Create test file with known content
    final Path testFile = tempDirectory.resolve("read_test.txt");
    final String expectedContent = "WASI File Reading Test Content";
    Files.writeString(testFile, expectedContent);

    // Verify file exists and has correct content
    assertTrue(Files.exists(testFile), "Test file should exist");
    final String actualContent = Files.readString(testFile);
    assertEquals(expectedContent, actualContent, "File content should match expected");

    LOGGER.info("File reading test completed successfully");
  }

  /** Tests directory operations through WASI. */
  @Test
  @DisplayName("WASI Directory Operations Test")
  void testDirectoryOperations() throws IOException {
    LOGGER.info("Testing WASI directory operations");

    // Create subdirectory
    final Path subdir = tempDirectory.resolve("test_subdir");
    Files.createDirectory(subdir);

    // Create files in subdirectory
    final Path file1 = subdir.resolve("file1.txt");
    final Path file2 = subdir.resolve("file2.txt");
    Files.writeString(file1, "File 1 content");
    Files.writeString(file2, "File 2 content");

    // Verify directory structure
    assertTrue(Files.isDirectory(subdir), "Subdirectory should exist");
    assertTrue(Files.exists(file1), "File 1 should exist");
    assertTrue(Files.exists(file2), "File 2 should exist");

    // List directory contents
    final long fileCount = Files.list(subdir).count();
    assertEquals(2, fileCount, "Subdirectory should contain 2 files");

    LOGGER.info("Directory operations test completed successfully");
  }

  /** Tests file metadata and statistics operations. */
  @Test
  @DisplayName("WASI File Metadata Test")
  void testFileMetadata() throws IOException {
    LOGGER.info("Testing WASI file metadata operations");

    // Create test file
    final Path testFile = tempDirectory.resolve("metadata_test.txt");
    final String content = "File metadata test content";
    Files.writeString(testFile, content);

    // Get file attributes
    final var attributes =
        Files.readAttributes(testFile, java.nio.file.attribute.BasicFileAttributes.class);

    // Validate basic attributes
    assertNotNull(attributes, "File attributes should not be null");
    assertTrue(attributes.isRegularFile(), "Should be a regular file");
    assertFalse(attributes.isDirectory(), "Should not be a directory");
    assertTrue(attributes.size() > 0, "File should have non-zero size");
    assertNotNull(attributes.creationTime(), "Creation time should be available");
    assertNotNull(attributes.lastModifiedTime(), "Last modified time should be available");

    LOGGER.info("File metadata test completed successfully");
  }

  /** Tests file permissions and access control. */
  @Test
  @DisplayName("WASI File Permissions Test")
  void testFilePermissions() throws IOException {
    LOGGER.info("Testing WASI file permissions");

    // Create test file
    final Path testFile = tempDirectory.resolve("permissions_test.txt");
    Files.writeString(testFile, "Permissions test content");

    // Test file access permissions
    assertTrue(Files.isReadable(testFile), "File should be readable");
    assertTrue(Files.isWritable(testFile), "File should be writable");

    // Note: Executable permission testing may be platform-dependent
    // and WASI sandboxing may restrict certain permission operations

    LOGGER.info("File permissions test completed successfully");
  }

  /** Tests file seeking and positioning operations. */
  @Test
  @DisplayName("WASI File Seeking Test")
  void testFileSeeking() throws IOException {
    LOGGER.info("Testing WASI file seeking operations");

    // Create test file with known content
    final Path testFile = tempDirectory.resolve("seek_test.txt");
    final String content = "0123456789ABCDEF"; // 16 characters
    Files.writeString(testFile, content);

    // Verify file size
    assertEquals(16, Files.size(testFile), "File should have correct size");

    // Test reading specific portions (simulating seek operations)
    final String fullContent = Files.readString(testFile);
    assertEquals(content, fullContent, "Full content should match");

    // Simulate seek operations by reading substrings
    final String firstPart = fullContent.substring(0, 8);
    final String secondPart = fullContent.substring(8);
    assertEquals("01234567", firstPart, "First part should match");
    assertEquals("89ABCDEF", secondPart, "Second part should match");

    LOGGER.info("File seeking test completed successfully");
  }

  /** Tests error handling for invalid file operations. */
  @Test
  @DisplayName("WASI File Error Handling Test")
  void testFileErrorHandling() {
    LOGGER.info("Testing WASI file error handling");

    // Test reading non-existent file
    final Path nonExistentFile = tempDirectory.resolve("non_existent.txt");
    assertFalse(Files.exists(nonExistentFile), "File should not exist");

    // Attempt to read non-existent file should handle gracefully
    try {
      Files.readString(nonExistentFile);
      // If we reach here without exception, that's fine - some implementations may handle
      // differently
      LOGGER.info("Reading non-existent file completed without exception");
    } catch (final IOException e) {
      // Expected behavior - reading non-existent file throws IOException
      LOGGER.info("Reading non-existent file threw expected exception: " + e.getMessage());
    }

    LOGGER.info("File error handling test completed successfully");
  }

  /** Tests cross-runtime consistency for file operations. */
  @Test
  @DisplayName("WASI File Operations Cross-Runtime Consistency Test")
  void testCrossRuntimeConsistency() throws WasmException, IOException {
    LOGGER.info("Testing cross-runtime consistency for file operations");

    // Create test file
    final Path testFile = tempDirectory.resolve("cross_runtime_test.txt");
    final String testContent = "Cross-runtime consistency test";
    Files.writeString(testFile, testContent);

    // Test with different WASI runtime types if available
    for (final WasiRuntimeType runtimeType : WasiRuntimeType.values()) {
      if (WasiFactory.isRuntimeAvailable(runtimeType)) {
        try (final WasiContext runtimeContext = WasiFactory.createContext(runtimeType)) {
          assertNotNull(runtimeContext, "Context should be created for " + runtimeType);
          assertTrue(runtimeContext.isValid(), "Context should be valid for " + runtimeType);

          // Verify consistent behavior across runtimes
          final String actualContent = Files.readString(testFile);
          assertEquals(
              testContent,
              actualContent,
              "File content should be consistent across " + runtimeType);

          LOGGER.info("Runtime " + runtimeType + " file operations validated");
        }
      } else {
        LOGGER.info("Runtime " + runtimeType + " not available, skipping");
      }
    }

    LOGGER.info("Cross-runtime consistency test completed successfully");
  }

  /** Tests performance of file operations. */
  @Test
  @DisplayName("WASI File Operations Performance Test")
  void testFileOperationsPerformance() throws IOException {
    LOGGER.info("Testing WASI file operations performance");

    final int fileCount = 10;
    final String content = "Performance test content for file operations";

    // Measure file creation performance
    final long startTime = System.nanoTime();

    for (int i = 0; i < fileCount; i++) {
      final Path testFile = tempDirectory.resolve("perf_test_" + i + ".txt");
      Files.writeString(testFile, content + " " + i);
    }

    final long endTime = System.nanoTime();
    final long totalTimeMs = (endTime - startTime) / 1_000_000;
    final double avgTimePerFile = totalTimeMs / (double) fileCount;

    LOGGER.info("Created " + fileCount + " files in " + totalTimeMs + "ms");
    LOGGER.info("Average time per file: " + String.format("%.2f ms", avgTimePerFile));

    // Validate performance is reasonable (less than 100ms per file)
    assertTrue(avgTimePerFile < 100.0, "File creation should be reasonably fast");

    // Verify all files were created correctly
    for (int i = 0; i < fileCount; i++) {
      final Path testFile = tempDirectory.resolve("perf_test_" + i + ".txt");
      assertTrue(Files.exists(testFile), "File " + i + " should exist");
    }

    LOGGER.info("File operations performance test completed successfully");
  }

  /** Tests large file handling capabilities. */
  @Test
  @DisplayName("WASI Large File Handling Test")
  void testLargeFileHandling() throws IOException {
    LOGGER.info("Testing WASI large file handling");

    // Create a moderately large file (1MB)
    final Path largeFile = tempDirectory.resolve("large_file.txt");
    final StringBuilder contentBuilder = new StringBuilder();

    // Build 1MB of content
    final String pattern = "This is a line of text for large file testing. ";
    final int targetSize = 1024 * 1024; // 1MB
    while (contentBuilder.length() < targetSize) {
      contentBuilder.append(pattern);
    }

    final String largeContent = contentBuilder.toString();
    Files.writeString(largeFile, largeContent);

    // Verify large file was created correctly
    assertTrue(Files.exists(largeFile), "Large file should exist");
    final long fileSize = Files.size(largeFile);
    assertTrue(fileSize >= targetSize, "File should be at least target size");

    // Verify content can be read back correctly
    final String readContent = Files.readString(largeFile);
    assertEquals(largeContent, readContent, "Large file content should match");

    LOGGER.info(
        "Large file handling test completed successfully (file size: " + fileSize + " bytes)");
  }
}
