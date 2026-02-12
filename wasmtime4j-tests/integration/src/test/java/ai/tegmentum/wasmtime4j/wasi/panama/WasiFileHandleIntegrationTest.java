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

package ai.tegmentum.wasmtime4j.wasi.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.panama.wasi.WasiFileHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiFileOperation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for Panama WasiFileHandle.
 *
 * <p>These tests verify the Panama WasiFileHandle wrapper, including file I/O operations, resource
 * lifecycle management, and proper cleanup.
 *
 * @since 1.0.0
 */
@DisplayName("Panama WasiFileHandle Integration Tests")
class WasiFileHandleIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiFileHandleIntegrationTest.class.getName());

  /** Test content for file operations. */
  private static final String TEST_CONTENT = "Hello, WASI FileHandle!";

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  @TempDir Path tempDir;

  private Path testFile;
  private SeekableByteChannel channel;
  private FileChannel fileChannel;

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for Panama WASI file handle tests");
    try {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertTrue(loader.isLoaded(), "Native library should be loaded");
      LOGGER.info("Native library loaded successfully for Panama");
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library required for integration tests", e);
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    LOGGER.info("Setting up test resources");

    // Create test file with content
    testFile = tempDir.resolve("test-file.txt");
    Files.writeString(testFile, TEST_CONTENT);

    // Open file channel for reading and writing
    fileChannel =
        FileChannel.open(
            testFile, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
    resources.add(fileChannel);
    channel = fileChannel;

    LOGGER.info("Test setup complete with test file: " + testFile);
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up test resources");
    // Close resources in reverse order to ensure child resources
    // are closed before parent resources
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    channel = null;
    fileChannel = null;
  }

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("should reject null path")
    void shouldRejectNullPath() {
      LOGGER.info("Testing constructor rejection of null path");

      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiFileHandle(3, null, channel, fileChannel, WasiFileOperation.READ),
          "Should reject null path");

      LOGGER.info("Constructor correctly rejected null path");
    }

    @Test
    @DisplayName("should reject null channel")
    void shouldRejectNullChannel() throws IOException {
      LOGGER.info("Testing constructor rejection of null channel");

      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiFileHandle(3, testFile, null, fileChannel, WasiFileOperation.READ),
          "Should reject null channel");

      LOGGER.info("Constructor correctly rejected null channel");
    }

    @Test
    @DisplayName("should reject null operation")
    void shouldRejectNullOperation() {
      LOGGER.info("Testing constructor rejection of null operation");

      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiFileHandle(3, testFile, channel, fileChannel, null),
          "Should reject null operation");

      LOGGER.info("Constructor correctly rejected null operation");
    }

    @Test
    @DisplayName("should accept null file channel")
    void shouldAcceptNullFileChannel() {
      LOGGER.info("Testing constructor accepts null file channel");

      final WasiFileHandle handle =
          new WasiFileHandle(3, testFile, channel, null, WasiFileOperation.READ);
      resources.add(handle);

      assertNotNull(handle, "Handle should be created even with null fileChannel");

      LOGGER.info("Constructor correctly accepted null file channel");
    }
  }

  @Nested
  @DisplayName("File Descriptor Tests")
  class FileDescriptorTests {

    @Test
    @DisplayName("should return correct file descriptor")
    void shouldReturnCorrectFileDescriptor() {
      LOGGER.info("Testing file descriptor retrieval");

      final WasiFileHandle handle =
          new WasiFileHandle(42, testFile, channel, fileChannel, WasiFileOperation.READ);
      resources.add(handle);

      assertEquals(
          42, handle.getFileDescriptor(), "File descriptor should match constructor value");

      LOGGER.info("File descriptor correctly returned: " + handle.getFileDescriptor());
    }
  }

  @Nested
  @DisplayName("Path Tests")
  class PathTests {

    @Test
    @DisplayName("should return absolute path")
    void shouldReturnAbsolutePath() {
      LOGGER.info("Testing path retrieval");

      final WasiFileHandle handle =
          new WasiFileHandle(3, testFile, channel, fileChannel, WasiFileOperation.READ);
      resources.add(handle);

      final Path returnedPath = handle.getPath();
      assertNotNull(returnedPath, "Path should not be null");
      assertTrue(returnedPath.isAbsolute(), "Path should be absolute");
      assertEquals(testFile.toAbsolutePath(), returnedPath, "Path should match test file");

      LOGGER.info("Path correctly returned: " + returnedPath);
    }
  }

  @Nested
  @DisplayName("Operation Type Tests")
  class OperationTypeTests {

    @Test
    @DisplayName("should return correct operation for READ")
    void shouldReturnCorrectOperationForRead() {
      LOGGER.info("Testing operation type READ");

      final WasiFileHandle handle =
          new WasiFileHandle(3, testFile, channel, fileChannel, WasiFileOperation.READ);
      resources.add(handle);

      assertEquals(WasiFileOperation.READ, handle.getOperation(), "Operation should be READ");

      LOGGER.info("Operation correctly returned: " + handle.getOperation());
    }

    @Test
    @DisplayName("should return correct operation for WRITE")
    void shouldReturnCorrectOperationForWrite() {
      LOGGER.info("Testing operation type WRITE");

      final WasiFileHandle handle =
          new WasiFileHandle(3, testFile, channel, fileChannel, WasiFileOperation.WRITE);
      resources.add(handle);

      assertEquals(WasiFileOperation.WRITE, handle.getOperation(), "Operation should be WRITE");

      LOGGER.info("Operation correctly returned: " + handle.getOperation());
    }
  }

  @Nested
  @DisplayName("Channel Access Tests")
  class ChannelAccessTests {

    @Test
    @DisplayName("should return channel")
    void shouldReturnChannel() {
      LOGGER.info("Testing channel retrieval");

      final WasiFileHandle handle =
          new WasiFileHandle(3, testFile, channel, fileChannel, WasiFileOperation.READ);
      resources.add(handle);

      final SeekableByteChannel returnedChannel = handle.getChannel();
      assertNotNull(returnedChannel, "Channel should not be null");
      assertEquals(channel, returnedChannel, "Channel should match constructor value");

      LOGGER.info("Channel correctly returned");
    }

    @Test
    @DisplayName("should return file channel when available")
    void shouldReturnFileChannelWhenAvailable() {
      LOGGER.info("Testing file channel retrieval");

      final WasiFileHandle handle =
          new WasiFileHandle(3, testFile, channel, fileChannel, WasiFileOperation.READ);
      resources.add(handle);

      final FileChannel returnedFileChannel = handle.getFileChannel();
      assertNotNull(returnedFileChannel, "FileChannel should not be null");
      assertEquals(fileChannel, returnedFileChannel, "FileChannel should match constructor value");

      LOGGER.info("File channel correctly returned");
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should not be closed after creation")
    void shouldNotBeClosedAfterCreation() {
      LOGGER.info("Testing initial closed state");

      final WasiFileHandle handle =
          new WasiFileHandle(3, testFile, channel, fileChannel, WasiFileOperation.READ);
      resources.add(handle);

      assertFalse(handle.isClosed(), "Handle should not be closed after creation");

      LOGGER.info("Handle correctly reports not closed after creation");
    }

    @Test
    @DisplayName("should be closed after close() call")
    void shouldBeClosedAfterCloseCall() throws Exception {
      LOGGER.info("Testing closed state after close()");

      // Create new channel for this test to avoid affecting other tests
      final FileChannel localChannel =
          FileChannel.open(testFile, StandardOpenOption.READ, StandardOpenOption.WRITE);

      final WasiFileHandle handle =
          new WasiFileHandle(3, testFile, localChannel, localChannel, WasiFileOperation.READ);

      handle.close();

      assertTrue(handle.isClosed(), "Handle should be closed after close()");

      LOGGER.info("Handle correctly reports closed after close()");
    }

    @Test
    @DisplayName("should throw when accessing file descriptor after close")
    void shouldThrowWhenAccessingFileDescriptorAfterClose() throws Exception {
      LOGGER.info("Testing access after close");

      // Create new channel for this test
      final FileChannel localChannel =
          FileChannel.open(testFile, StandardOpenOption.READ, StandardOpenOption.WRITE);

      final WasiFileHandle handle =
          new WasiFileHandle(3, testFile, localChannel, localChannel, WasiFileOperation.READ);

      handle.close();

      assertThrows(
          IllegalStateException.class,
          handle::getFileDescriptor,
          "Should throw when accessing file descriptor after close");

      LOGGER.info("Correctly threw exception when accessing after close");
    }
  }

  @Nested
  @DisplayName("I/O Operation Tests")
  class IOOperationTests {

    @Test
    @DisplayName("should read data through channel")
    void shouldReadDataThroughChannel() throws Exception {
      LOGGER.info("Testing read through channel");

      final WasiFileHandle handle =
          new WasiFileHandle(3, testFile, channel, fileChannel, WasiFileOperation.READ);
      resources.add(handle);

      final ByteBuffer buffer = ByteBuffer.allocate(1024);
      handle.getChannel().position(0);
      final int bytesRead = handle.getChannel().read(buffer);

      assertTrue(bytesRead > 0, "Should read some bytes");
      buffer.flip();
      final byte[] data = new byte[buffer.remaining()];
      buffer.get(data);
      final String content = new String(data);
      assertEquals(TEST_CONTENT, content, "Content should match test content");

      LOGGER.info("Successfully read: " + content);
    }

    @Test
    @DisplayName("should write data through channel")
    void shouldWriteDataThroughChannel() throws Exception {
      LOGGER.info("Testing write through channel");

      // Create new file for write test
      final Path writeFile = tempDir.resolve("write-test.txt");
      Files.writeString(writeFile, "");

      final FileChannel writeChannel =
          FileChannel.open(
              writeFile,
              StandardOpenOption.READ,
              StandardOpenOption.WRITE,
              StandardOpenOption.TRUNCATE_EXISTING);
      resources.add(writeChannel);

      final WasiFileHandle handle =
          new WasiFileHandle(4, writeFile, writeChannel, writeChannel, WasiFileOperation.WRITE);
      resources.add(handle);

      final String writeContent = "Written via WasiFileHandle";
      final ByteBuffer buffer = ByteBuffer.wrap(writeContent.getBytes());
      handle.getChannel().write(buffer);
      handle.getFileChannel().force(false);

      // Read back and verify
      writeChannel.position(0);
      final ByteBuffer readBuffer = ByteBuffer.allocate(1024);
      writeChannel.read(readBuffer);
      readBuffer.flip();
      final byte[] data = new byte[readBuffer.remaining()];
      readBuffer.get(data);
      final String readContent = new String(data);

      assertEquals(writeContent, readContent, "Written content should match");

      LOGGER.info("Successfully wrote and verified: " + readContent);
    }
  }
}
