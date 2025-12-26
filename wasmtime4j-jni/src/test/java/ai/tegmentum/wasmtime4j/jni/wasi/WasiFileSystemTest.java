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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiFileSystemException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Comprehensive tests for {@link WasiFileSystem}. */
@DisplayName("WasiFileSystem Tests")
class WasiFileSystemTest {

  @TempDir Path tempDir;

  private WasiContext testContext;
  private WasiFileSystem fileSystem;

  @BeforeEach
  void setUp() throws Exception {
    testContext = TestWasiContextFactory.createTestContextWithWorkingDir(tempDir);
    fileSystem = new WasiFileSystem(testContext);
  }

  @AfterEach
  void tearDown() {
    if (fileSystem != null) {
      fileSystem.closeAll();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiFileSystem should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiFileSystem.class.getModifiers()),
          "WasiFileSystem should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should throw on null context")
    void constructorShouldThrowOnNullContext() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiFileSystem(null),
          "Should throw on null context");
    }

    @Test
    @DisplayName("Constructor should create file system with valid context")
    void constructorShouldCreateFileSystemWithValidContext() {
      final WasiFileSystem fs = new WasiFileSystem(testContext);
      assertNotNull(fs, "File system should be created");
      assertEquals(0, fs.getOpenFileCount(), "Should have no open files initially");
    }
  }

  @Nested
  @DisplayName("openFile Tests")
  class OpenFileTests {

    @Test
    @DisplayName("Should open file for reading")
    void shouldOpenFileForReading() throws IOException {
      // Create a test file
      final Path testFile = tempDir.resolve("test.txt");
      writeStringToFile(testFile, "test content");

      final int fd = fileSystem.openFile("test.txt", WasiFileOperation.READ, false, false);

      assertTrue(fd >= 3, "File descriptor should be >= 3");
      assertEquals(1, fileSystem.getOpenFileCount(), "Should have 1 open file");
    }

    @Test
    @DisplayName("Should open file for writing")
    void shouldOpenFileForWriting() throws IOException {
      final Path testFile = tempDir.resolve("write.txt");
      Files.createFile(testFile);

      final int fd = fileSystem.openFile("write.txt", WasiFileOperation.WRITE, false, false);

      assertTrue(fd >= 3, "File descriptor should be >= 3");
      assertEquals(1, fileSystem.getOpenFileCount(), "Should have 1 open file");
    }

    @Test
    @DisplayName("Should create file if not exists")
    void shouldCreateFileIfNotExists() {
      final int fd = fileSystem.openFile("new.txt", WasiFileOperation.WRITE, true, false);

      assertTrue(fd >= 3, "File descriptor should be >= 3");
      assertTrue(Files.exists(tempDir.resolve("new.txt")), "File should be created");
    }

    @Test
    @DisplayName("Should throw on null path")
    void shouldThrowOnNullPath() {
      assertThrows(
          IllegalArgumentException.class,
          () -> fileSystem.openFile(null, WasiFileOperation.READ, false, false),
          "Should throw on null path");
    }

    @Test
    @DisplayName("Should throw on empty path")
    void shouldThrowOnEmptyPath() {
      assertThrows(
          IllegalArgumentException.class,
          () -> fileSystem.openFile("", WasiFileOperation.READ, false, false),
          "Should throw on empty path");
    }

    @Test
    @DisplayName("Should throw on null operation")
    void shouldThrowOnNullOperation() {
      assertThrows(
          IllegalArgumentException.class,
          () -> fileSystem.openFile("test.txt", null, false, false),
          "Should throw on null operation");
    }
  }

  @Nested
  @DisplayName("readFile Tests")
  class ReadFileTests {

    @Test
    @DisplayName("Should read data from file")
    void shouldReadDataFromFile() throws IOException {
      final Path testFile = tempDir.resolve("read.txt");
      writeStringToFile(testFile, "Hello World");

      final int fd = fileSystem.openFile("read.txt", WasiFileOperation.READ, false, false);
      final byte[] buffer = new byte[100];
      final int bytesRead = fileSystem.readFile(fd, buffer, 0, buffer.length);

      assertTrue(bytesRead > 0, "Should read some bytes");
      assertEquals("Hello World", new String(buffer, 0, bytesRead).trim());
    }

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() throws IOException {
      final Path testFile = tempDir.resolve("read2.txt");
      writeStringToFile(testFile, "test");
      final int fd = fileSystem.openFile("read2.txt", WasiFileOperation.READ, false, false);

      assertThrows(
          IllegalArgumentException.class,
          () -> fileSystem.readFile(fd, null, 0, 10),
          "Should throw on null buffer");
    }

    @Test
    @DisplayName("Should throw on negative offset")
    void shouldThrowOnNegativeOffset() throws IOException {
      final Path testFile = tempDir.resolve("read3.txt");
      writeStringToFile(testFile, "test");
      final int fd = fileSystem.openFile("read3.txt", WasiFileOperation.READ, false, false);
      final byte[] buffer = new byte[100];

      assertThrows(
          IllegalArgumentException.class,
          () -> fileSystem.readFile(fd, buffer, -1, 10),
          "Should throw on negative offset");
    }

    @Test
    @DisplayName("Should throw on buffer overflow")
    void shouldThrowOnBufferOverflow() throws IOException {
      final Path testFile = tempDir.resolve("read4.txt");
      writeStringToFile(testFile, "test");
      final int fd = fileSystem.openFile("read4.txt", WasiFileOperation.READ, false, false);
      final byte[] buffer = new byte[10];

      assertThrows(
          IllegalArgumentException.class,
          () -> fileSystem.readFile(fd, buffer, 5, 10),
          "Should throw on buffer overflow");
    }

    @Test
    @DisplayName("Should throw on invalid file descriptor")
    void shouldThrowOnInvalidFileDescriptor() {
      final byte[] buffer = new byte[100];

      assertThrows(
          WasiFileSystemException.class,
          () -> fileSystem.readFile(999, buffer, 0, buffer.length),
          "Should throw on invalid file descriptor");
    }
  }

  @Nested
  @DisplayName("writeFile Tests")
  class WriteFileTests {

    @Test
    @DisplayName("Should write data to file")
    void shouldWriteDataToFile() throws IOException {
      final Path testFile = tempDir.resolve("write.txt");
      Files.createFile(testFile);

      final int fd = fileSystem.openFile("write.txt", WasiFileOperation.WRITE, false, false);
      final byte[] data = "Hello World".getBytes();
      final int bytesWritten = fileSystem.writeFile(fd, data, 0, data.length);

      assertEquals(data.length, bytesWritten, "Should write all bytes");
    }

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() throws IOException {
      final Path testFile = tempDir.resolve("write2.txt");
      Files.createFile(testFile);
      final int fd = fileSystem.openFile("write2.txt", WasiFileOperation.WRITE, false, false);

      assertThrows(
          IllegalArgumentException.class,
          () -> fileSystem.writeFile(fd, null, 0, 10),
          "Should throw on null buffer");
    }

    @Test
    @DisplayName("Should throw when writing to read-only file")
    void shouldThrowWhenWritingToReadOnlyFile() throws IOException {
      final Path testFile = tempDir.resolve("readonly.txt");
      writeStringToFile(testFile, "test");
      final int fd = fileSystem.openFile("readonly.txt", WasiFileOperation.READ, false, false);
      final byte[] data = "test".getBytes();

      assertThrows(
          WasiFileSystemException.class,
          () -> fileSystem.writeFile(fd, data, 0, data.length),
          "Should throw when writing to read-only file");
    }
  }

  @Nested
  @DisplayName("seekFile Tests")
  class SeekFileTests {

    @Test
    @DisplayName("Should seek to position from start")
    void shouldSeekToPositionFromStart() throws IOException {
      final Path testFile = tempDir.resolve("seek.txt");
      writeStringToFile(testFile, "0123456789");

      final int fd = fileSystem.openFile("seek.txt", WasiFileOperation.READ, false, false);
      final long newPosition = fileSystem.seekFile(fd, 5, 0); // SEEK_SET

      assertEquals(5, newPosition, "Should seek to position 5");
    }

    @Test
    @DisplayName("Should throw on invalid whence")
    void shouldThrowOnInvalidWhence() throws IOException {
      final Path testFile = tempDir.resolve("seek2.txt");
      writeStringToFile(testFile, "test");
      final int fd = fileSystem.openFile("seek2.txt", WasiFileOperation.READ, false, false);

      assertThrows(
          WasiFileSystemException.class,
          () -> fileSystem.seekFile(fd, 0, 99),
          "Should throw on invalid whence");
    }
  }

  @Nested
  @DisplayName("closeFile Tests")
  class CloseFileTests {

    @Test
    @DisplayName("Should close open file")
    void shouldCloseOpenFile() throws IOException {
      final Path testFile = tempDir.resolve("close.txt");
      writeStringToFile(testFile, "test");

      final int fd = fileSystem.openFile("close.txt", WasiFileOperation.READ, false, false);
      assertEquals(1, fileSystem.getOpenFileCount(), "Should have 1 open file");

      fileSystem.closeFile(fd);
      assertEquals(0, fileSystem.getOpenFileCount(), "Should have 0 open files");
    }

    @Test
    @DisplayName("Should throw on invalid file descriptor")
    void shouldThrowOnInvalidFileDescriptor() {
      assertThrows(
          WasiFileSystemException.class,
          () -> fileSystem.closeFile(999),
          "Should throw on invalid file descriptor");
    }
  }

  @Nested
  @DisplayName("getFileMetadata Tests")
  class GetFileMetadataTests {

    @Test
    @DisplayName("Should get metadata for regular file")
    void shouldGetMetadataForRegularFile() throws IOException {
      final Path testFile = tempDir.resolve("meta.txt");
      writeStringToFile(testFile, "test content");

      final WasiFileMetadata metadata = fileSystem.getFileMetadata("meta.txt");

      assertNotNull(metadata, "Metadata should not be null");
      assertTrue(metadata.isRegularFile(), "Should be regular file");
      assertTrue(metadata.getSize() > 0, "Size should be > 0");
    }

    @Test
    @DisplayName("Should get metadata for directory")
    void shouldGetMetadataForDirectory() throws IOException {
      final Path dir = tempDir.resolve("metadir");
      Files.createDirectory(dir);

      final WasiFileMetadata metadata = fileSystem.getFileMetadata("metadir");

      assertNotNull(metadata, "Metadata should not be null");
      assertTrue(metadata.isDirectory(), "Should be directory");
    }

    @Test
    @DisplayName("Should throw on null path")
    void shouldThrowOnNullPath() {
      assertThrows(
          IllegalArgumentException.class,
          () -> fileSystem.getFileMetadata(null),
          "Should throw on null path");
    }
  }

  @Nested
  @DisplayName("listDirectory Tests")
  class ListDirectoryTests {

    @Test
    @DisplayName("Should list directory contents")
    void shouldListDirectoryContents() throws IOException {
      final Path dir = tempDir.resolve("listdir");
      Files.createDirectory(dir);
      Files.createFile(dir.resolve("file1.txt"));
      Files.createFile(dir.resolve("file2.txt"));
      Files.createDirectory(dir.resolve("subdir"));

      final List<WasiDirectoryEntry> entries = fileSystem.listDirectory("listdir");

      assertEquals(3, entries.size(), "Should have 3 entries");
    }

    @Test
    @DisplayName("Should list empty directory")
    void shouldListEmptyDirectory() throws IOException {
      final Path dir = tempDir.resolve("emptydir");
      Files.createDirectory(dir);

      final List<WasiDirectoryEntry> entries = fileSystem.listDirectory("emptydir");

      assertTrue(entries.isEmpty(), "Should have no entries");
    }

    @Test
    @DisplayName("Should throw on non-directory path")
    void shouldThrowOnNonDirectoryPath() throws IOException {
      final Path file = tempDir.resolve("notdir.txt");
      Files.createFile(file);

      assertThrows(
          WasiFileSystemException.class,
          () -> fileSystem.listDirectory("notdir.txt"),
          "Should throw on non-directory path");
    }
  }

  @Nested
  @DisplayName("createDirectory Tests")
  class CreateDirectoryTests {

    @Test
    @DisplayName("Should create directory")
    void shouldCreateDirectory() {
      fileSystem.createDirectory("newdir");

      assertTrue(Files.isDirectory(tempDir.resolve("newdir")), "Directory should be created");
    }

    @Test
    @DisplayName("Should throw when directory exists")
    void shouldThrowWhenDirectoryExists() throws IOException {
      final Path dir = tempDir.resolve("existingdir");
      Files.createDirectory(dir);

      assertThrows(
          WasiFileSystemException.class,
          () -> fileSystem.createDirectory("existingdir"),
          "Should throw when directory exists");
    }
  }

  @Nested
  @DisplayName("removeFileOrDirectory Tests")
  class RemoveFileOrDirectoryTests {

    @Test
    @DisplayName("Should remove file")
    void shouldRemoveFile() throws IOException {
      final Path file = tempDir.resolve("remove.txt");
      Files.createFile(file);

      fileSystem.removeFileOrDirectory("remove.txt");

      assertTrue(Files.notExists(file), "File should be removed");
    }

    @Test
    @DisplayName("Should remove empty directory")
    void shouldRemoveEmptyDirectory() throws IOException {
      final Path dir = tempDir.resolve("removedir");
      Files.createDirectory(dir);

      fileSystem.removeFileOrDirectory("removedir");

      assertTrue(Files.notExists(dir), "Directory should be removed");
    }

    @Test
    @DisplayName("Should throw on non-existent path")
    void shouldThrowOnNonExistentPath() {
      assertThrows(
          WasiFileSystemException.class,
          () -> fileSystem.removeFileOrDirectory("nonexistent"),
          "Should throw on non-existent path");
    }
  }

  @Nested
  @DisplayName("renameFileOrDirectory Tests")
  class RenameFileOrDirectoryTests {

    @Test
    @DisplayName("Should rename file")
    void shouldRenameFile() throws IOException {
      final Path file = tempDir.resolve("original.txt");
      Files.createFile(file);

      fileSystem.renameFileOrDirectory("original.txt", "renamed.txt");

      assertTrue(Files.notExists(file), "Original should not exist");
      assertTrue(Files.exists(tempDir.resolve("renamed.txt")), "Renamed should exist");
    }
  }

  @Nested
  @DisplayName("closeAll Tests")
  class CloseAllTests {

    @Test
    @DisplayName("Should close all open files")
    void shouldCloseAllOpenFiles() throws IOException {
      Files.createFile(tempDir.resolve("file1.txt"));
      Files.createFile(tempDir.resolve("file2.txt"));
      Files.createFile(tempDir.resolve("file3.txt"));

      fileSystem.openFile("file1.txt", WasiFileOperation.READ, false, false);
      fileSystem.openFile("file2.txt", WasiFileOperation.READ, false, false);
      fileSystem.openFile("file3.txt", WasiFileOperation.READ, false, false);

      assertEquals(3, fileSystem.getOpenFileCount(), "Should have 3 open files");

      fileSystem.closeAll();

      assertEquals(0, fileSystem.getOpenFileCount(), "Should have 0 open files");
    }
  }

  /**
   * Helper method to write string to file - Java 8 compatible alternative to Files.writeString().
   *
   * @param path the path to write to
   * @param content the string content to write
   * @throws IOException if writing fails
   */
  private static void writeStringToFile(final Path path, final String content) throws IOException {
    Files.write(path, content.getBytes(StandardCharsets.UTF_8));
  }
}
