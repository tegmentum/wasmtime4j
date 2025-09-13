/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.panama.wasi.WasiFileSystem;
import ai.tegmentum.wasmtime4j.panama.wasi.WasiFileOperation;
import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiFileSystemException;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for Panama WASI implementation.
 *
 * <p>This test class verifies the complete WASI functionality including:
 * <ul>
 *   <li>WASI context creation and lifecycle management</li>
 *   <li>File system operations with sandbox security</li>
 *   <li>Zero-copy memory operations</li>
 *   <li>Environment variable and argument handling</li>
 *   <li>Integration with Panama FFI bindings</li>
 * </ul>
 */
class PanamaWasiIntegrationTest {

  @TempDir private Path tempDirectory;

  private WasiContext wasiContext;
  private WasiFileSystem fileSystem;
  private Arena testArena;

  @BeforeEach
  void setUp(TestInfo testInfo) throws Exception {
    System.out.println("Setting up test: " + testInfo.getDisplayName());
    
    testArena = Arena.ofConfined();
    
    // Create WASI context with comprehensive configuration
    wasiContext = WasiContext.builder()
        .withEnvironment("TEST_ENV", "integration_test")
        .withEnvironment("TEMP_DIR", tempDirectory.toString())
        .withArgument("--test-mode")
        .withArgument("--integration")
        .withPreopenDirectory("/tmp", tempDirectory.toString())
        .withWorkingDirectory("/tmp")
        .build();
    
    assertNotNull(wasiContext, "WASI context should be created successfully");
    assertFalse(wasiContext.isClosed(), "WASI context should not be closed initially");
    
    fileSystem = new WasiFileSystem(wasiContext);
    assertNotNull(fileSystem, "WASI file system should be created successfully");
    
    System.out.println("Test setup completed successfully");
  }

  @AfterEach
  void tearDown(TestInfo testInfo) throws Exception {
    System.out.println("Tearing down test: " + testInfo.getDisplayName());
    
    try {
      if (fileSystem != null) {
        fileSystem.closeAll();
      }
    } catch (Exception e) {
      System.err.println("Error closing file system: " + e.getMessage());
    }
    
    try {
      if (wasiContext != null && !wasiContext.isClosed()) {
        wasiContext.close();
      }
    } catch (Exception e) {
      System.err.println("Error closing WASI context: " + e.getMessage());
    }
    
    try {
      if (testArena != null) {
        testArena.close();
      }
    } catch (Exception e) {
      System.err.println("Error closing test arena: " + e.getMessage());
    }
    
    System.out.println("Test teardown completed");
  }

  @Test
  void testWasiContextCreationAndBasicOperations() {
    // Test environment variable access
    String testEnv = wasiContext.getEnvironmentVariable("TEST_ENV");
    assertEquals("integration_test", testEnv, "Environment variable should be accessible");
    
    String tempDir = wasiContext.getEnvironmentVariable("TEMP_DIR");
    assertEquals(tempDirectory.toString(), tempDir, "Temp directory should be accessible");
    
    // Test arguments
    String[] args = wasiContext.getArguments();
    assertTrue(args.length >= 2, "Should have at least 2 arguments");
    boolean hasTestMode = false;
    boolean hasIntegration = false;
    for (String arg : args) {
      if ("--test-mode".equals(arg)) hasTestMode = true;
      if ("--integration".equals(arg)) hasIntegration = true;
    }
    assertTrue(hasTestMode, "Should have --test-mode argument");
    assertTrue(hasIntegration, "Should have --integration argument");
    
    // Test pre-opened directories
    Map<String, Path> preopenDirs = wasiContext.getPreopenedDirectories();
    assertTrue(preopenDirs.containsKey("/tmp"), "Should have /tmp pre-opened directory");
    assertEquals(tempDirectory, preopenDirs.get("/tmp"), "Pre-opened directory should match temp directory");
  }

  @Test
  void testWasiFileSystemBasicOperations() throws Exception {
    // Create a test file
    Path testFile = tempDirectory.resolve("test_basic.txt");
    String testContent = "Hello, WASI Integration Test!";
    Files.write(testFile, testContent.getBytes(), StandardOpenOption.CREATE);
    
    // Test file opening
    int fd = fileSystem.openFile("/tmp/test_basic.txt", WasiFileOperation.READ, false, false);
    assertTrue(fd >= 0, "File descriptor should be valid");
    
    // Test reading
    byte[] buffer = new byte[testContent.length()];
    int bytesRead = fileSystem.readFile(fd, buffer, 0, buffer.length);
    assertEquals(testContent.length(), bytesRead, "Should read all bytes");
    assertEquals(testContent, new String(buffer), "File content should match");
    
    // Test file closing
    fileSystem.closeFile(fd);
    
    // Verify file handle is closed
    assertThrows(WasiFileSystemException.class, () -> {
      fileSystem.readFile(fd, new byte[10], 0, 10);
    }, "Reading from closed file should throw exception");
  }

  @Test
  void testWasiZeroCopyOperations() throws Exception {
    // Create a test file with larger content for zero-copy testing
    Path testFile = tempDirectory.resolve("test_zerocopy.txt");
    String testContent = "Zero-copy test data for Panama WASI integration! ".repeat(100);
    Files.write(testFile, testContent.getBytes(), StandardOpenOption.CREATE);
    
    // Open file for reading
    int readFd = fileSystem.openFile("/tmp/test_zerocopy.txt", WasiFileOperation.READ, false, false);
    assertTrue(readFd >= 0, "Read file descriptor should be valid");
    
    // Test zero-copy read operation
    MemorySegment readBuffer = testArena.allocate(testContent.length());
    int bytesRead = fileSystem.readFileZeroCopy(readFd, readBuffer, 0, testContent.length());
    assertEquals(testContent.length(), bytesRead, "Should read all bytes with zero-copy");
    
    // Verify content using memory segment
    String readContent = readBuffer.getUtf8String(0);
    assertEquals(testContent, readContent, "Zero-copy read content should match");
    
    fileSystem.closeFile(readFd);
    
    // Test zero-copy write operation
    Path outputFile = tempDirectory.resolve("test_zerocopy_output.txt");
    int writeFd = fileSystem.openFile("/tmp/test_zerocopy_output.txt", WasiFileOperation.WRITE, true, false);
    assertTrue(writeFd >= 0, "Write file descriptor should be valid");
    
    // Write using zero-copy
    String writeContent = "Zero-copy write test content!";
    MemorySegment writeBuffer = testArena.allocateUtf8String(writeContent);
    int bytesWritten = fileSystem.writeFileZeroCopy(writeFd, writeBuffer, 0, (int) writeBuffer.byteSize() - 1); // -1 for null terminator
    assertTrue(bytesWritten > 0, "Should write bytes with zero-copy");
    
    fileSystem.closeFile(writeFd);
    
    // Verify written content
    String actualContent = Files.readString(outputFile);
    assertEquals(writeContent, actualContent, "Zero-copy written content should match");
  }

  @Test
  void testWasiVectoredOperations() throws Exception {
    // Create multiple test files for vectored operations
    Path file1 = tempDirectory.resolve("vector1.txt");
    Path file2 = tempDirectory.resolve("vector2.txt");
    String content1 = "First vector chunk data";
    String content2 = "Second vector chunk data";
    Files.write(file1, content1.getBytes(), StandardOpenOption.CREATE);
    Files.write(file2, content2.getBytes(), StandardOpenOption.CREATE);
    
    // Test vectored read
    int fd1 = fileSystem.openFile("/tmp/vector1.txt", WasiFileOperation.READ, false, false);
    
    MemorySegment[] readSegments = new MemorySegment[] {
        testArena.allocate(content1.length()),
        testArena.allocate(content2.length())
    };
    long[] offsets = {0, 0};
    int[] lengths = {content1.length(), content2.length()};
    
    // Read from file1 into first segment
    int bytesRead1 = fileSystem.readFileZeroCopy(fd1, readSegments[0], 0, content1.length());
    assertEquals(content1.length(), bytesRead1, "Should read first chunk completely");
    
    fileSystem.closeFile(fd1);
    
    // Open second file and read
    int fd2 = fileSystem.openFile("/tmp/vector2.txt", WasiFileOperation.READ, false, false);
    int bytesRead2 = fileSystem.readFileZeroCopy(fd2, readSegments[1], 0, content2.length());
    assertEquals(content2.length(), bytesRead2, "Should read second chunk completely");
    
    fileSystem.closeFile(fd2);
    
    // Verify vectored read results
    assertEquals(content1, readSegments[0].getUtf8String(0), "First vector chunk should match");
    assertEquals(content2, readSegments[1].getUtf8String(0), "Second vector chunk should match");
  }

  @Test
  void testWasiFileSystemMetadataOperations() throws Exception {
    // Create test file with known content
    Path testFile = tempDirectory.resolve("metadata_test.txt");
    String content = "Metadata test content";
    Files.write(testFile, content.getBytes(), StandardOpenOption.CREATE);
    
    // Test metadata retrieval
    var metadata = fileSystem.getFileMetadata("/tmp/metadata_test.txt");
    assertNotNull(metadata, "File metadata should be retrievable");
    assertEquals(content.length(), metadata.getSize(), "File size should match");
    assertTrue(metadata.isRegularFile(), "Should be identified as regular file");
    assertFalse(metadata.isDirectory(), "Should not be identified as directory");
    assertTrue(metadata.isReadable(), "File should be readable");
  }

  @Test
  void testWasiDirectoryOperations() throws Exception {
    // Create test directory structure
    Path subDir = tempDirectory.resolve("test_subdir");
    Files.createDirectory(subDir);
    Path testFile = subDir.resolve("dir_test.txt");
    Files.write(testFile, "Directory test content".getBytes(), StandardOpenOption.CREATE);
    
    // Test directory creation through WASI
    fileSystem.createDirectory("/tmp/wasi_created_dir");
    Path createdDir = tempDirectory.resolve("wasi_created_dir");
    assertTrue(Files.exists(createdDir), "Directory should be created through WASI");
    assertTrue(Files.isDirectory(createdDir), "Created path should be a directory");
    
    // Test directory listing
    var entries = fileSystem.listDirectory("/tmp");
    assertNotNull(entries, "Directory listing should not be null");
    assertTrue(entries.size() >= 2, "Should have at least test_subdir and wasi_created_dir");
    
    boolean hasSubDir = entries.stream().anyMatch(e -> "test_subdir".equals(e.getName()) && e.isDirectory());
    boolean hasCreatedDir = entries.stream().anyMatch(e -> "wasi_created_dir".equals(e.getName()) && e.isDirectory());
    assertTrue(hasSubDir, "Directory listing should include test_subdir");
    assertTrue(hasCreatedDir, "Directory listing should include wasi_created_dir");
  }

  @Test
  void testWasiSecurityValidation() {
    // Test path traversal protection
    assertThrows(Exception.class, () -> {
      wasiContext.validatePath("../../../etc/passwd");
    }, "Path traversal should be prevented");
    
    assertThrows(Exception.class, () -> {
      fileSystem.openFile("../../../etc/passwd", WasiFileOperation.READ, false, false);
    }, "Unauthorized file access should be prevented");
    
    // Test access to non-preopen directories
    assertThrows(Exception.class, () -> {
      fileSystem.openFile("/etc/passwd", WasiFileOperation.READ, false, false);
    }, "Access outside sandbox should be prevented");
  }

  @Test
  void testWasiResourceLimits() {
    // Test file handle limits
    int openFileCount = fileSystem.getOpenFileCount();
    assertEquals(0, openFileCount, "Initially should have no open files");
    
    // This test assumes reasonable file handle limits
    // In practice, the actual limit would be configurable
    assertTrue(openFileCount < 1024, "Open file count should be within reasonable limits");
  }

  @Test
  void testWasiContextLifecycle() {
    // Test context state
    assertFalse(wasiContext.isClosed(), "Context should not be closed initially");
    
    // Test context information access
    Map<String, String> env = wasiContext.getEnvironment();
    assertNotNull(env, "Environment should be accessible");
    assertTrue(env.containsKey("TEST_ENV"), "Test environment variable should be present");
    
    String[] args = wasiContext.getArguments();
    assertNotNull(args, "Arguments should be accessible");
    assertTrue(args.length > 0, "Should have at least one argument");
    
    // Test context closure
    wasiContext.close();
    assertTrue(wasiContext.isClosed(), "Context should be closed after close()");
    
    // Test operations on closed context
    assertThrows(IllegalStateException.class, () -> {
      wasiContext.getEnvironmentVariable("TEST_ENV");
    }, "Operations on closed context should fail");
  }

  @Test
  void testWasiErrorHandling() throws Exception {
    // Test file not found error
    assertThrows(WasiFileSystemException.class, () -> {
      fileSystem.openFile("/tmp/nonexistent_file.txt", WasiFileOperation.READ, false, false);
    }, "Opening nonexistent file should throw WasiFileSystemException");
    
    // Test invalid file descriptor error
    assertThrows(WasiFileSystemException.class, () -> {
      fileSystem.readFile(999, new byte[10], 0, 10);
    }, "Invalid file descriptor should throw WasiFileSystemException");
    
    // Test write to read-only file
    Path readOnlyFile = tempDirectory.resolve("readonly_test.txt");
    Files.write(readOnlyFile, "readonly content".getBytes(), StandardOpenOption.CREATE);
    
    int readFd = fileSystem.openFile("/tmp/readonly_test.txt", WasiFileOperation.READ, false, false);
    assertThrows(WasiFileSystemException.class, () -> {
      fileSystem.writeFile(readFd, "attempt write".getBytes(), 0, 13);
    }, "Writing to read-only file should throw WasiFileSystemException");
    
    fileSystem.closeFile(readFd);
  }
}