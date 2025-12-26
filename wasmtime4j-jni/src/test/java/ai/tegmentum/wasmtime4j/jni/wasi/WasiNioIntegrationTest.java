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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiFileSystemException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive tests for {@link WasiNioIntegration}.
 */
@DisplayName("WasiNioIntegration Tests")
class WasiNioIntegrationTest {

  @TempDir
  Path tempDir;

  private WasiNioIntegration nioIntegration;

  @BeforeEach
  void setUp() {
    nioIntegration = new WasiNioIntegration();
  }

  @AfterEach
  void tearDown() {
    if (nioIntegration != null) {
      nioIntegration.shutdown();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiNioIntegration should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiNioIntegration.class.getModifiers()),
          "WasiNioIntegration should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Default constructor should create integration")
    void defaultConstructorShouldCreateIntegration() {
      final WasiNioIntegration nio = new WasiNioIntegration();
      assertNotNull(nio, "Integration should be created");
      nio.shutdown();
    }

    @Test
    @DisplayName("Parameterized constructor should create integration")
    void parameterizedConstructorShouldCreateIntegration() {
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      final WasiNioIntegration nio = new WasiNioIntegration(true, true, executor);
      assertNotNull(nio, "Integration should be created");
      nio.shutdown();
      executor.shutdown();
    }

    @Test
    @DisplayName("Constructor should throw on null executor")
    void constructorShouldThrowOnNullExecutor() {
      assertThrows(IllegalArgumentException.class,
          () -> new WasiNioIntegration(true, true, null),
          "Should throw on null executor");
    }
  }

  @Nested
  @DisplayName("bulkRead Tests")
  class BulkReadTests {

    @Test
    @DisplayName("Should perform bulk read")
    void shouldPerformBulkRead() throws IOException {
      final Path testFile = tempDir.resolve("bulkread.txt");
      writeStringToFile(testFile, "Hello World!");

      try (final FileChannel channel = FileChannel.open(testFile, StandardOpenOption.READ)) {
        final ByteBuffer buffer = nioIntegration.bulkRead(channel, 0, 100);

        assertNotNull(buffer, "Buffer should not be null");
        assertTrue(buffer.remaining() > 0, "Buffer should have data");
      }
    }

    @Test
    @DisplayName("Should throw on null channel")
    void shouldThrowOnNullChannel() {
      assertThrows(IllegalArgumentException.class,
          () -> nioIntegration.bulkRead(null, 0, 100),
          "Should throw on null channel");
    }

    @Test
    @DisplayName("Should throw on negative position")
    void shouldThrowOnNegativePosition() throws IOException {
      final Path testFile = tempDir.resolve("bulkread2.txt");
      writeStringToFile(testFile, "test");

      try (final FileChannel channel = FileChannel.open(testFile, StandardOpenOption.READ)) {
        assertThrows(IllegalArgumentException.class,
            () -> nioIntegration.bulkRead(channel, -1, 100),
            "Should throw on negative position");
      }
    }

    @Test
    @DisplayName("Should throw on zero buffer size")
    void shouldThrowOnZeroBufferSize() throws IOException {
      final Path testFile = tempDir.resolve("bulkread3.txt");
      writeStringToFile(testFile, "test");

      try (final FileChannel channel = FileChannel.open(testFile, StandardOpenOption.READ)) {
        assertThrows(IllegalArgumentException.class,
            () -> nioIntegration.bulkRead(channel, 0, 0),
            "Should throw on zero buffer size");
      }
    }
  }

  @Nested
  @DisplayName("bulkWrite Tests")
  class BulkWriteTests {

    @Test
    @DisplayName("Should perform bulk write")
    void shouldPerformBulkWrite() throws IOException {
      final Path testFile = tempDir.resolve("bulkwrite.txt");
      Files.createFile(testFile);

      try (final FileChannel channel = FileChannel.open(testFile, StandardOpenOption.WRITE)) {
        final ByteBuffer data = ByteBuffer.wrap("Hello World!".getBytes());
        final int written = nioIntegration.bulkWrite(channel, 0, data);

        assertEquals(12, written, "Should write all bytes");
      }

      assertEquals("Hello World!", readStringFromFile(testFile), "File should contain data");
    }

    @Test
    @DisplayName("Should throw on null channel")
    void shouldThrowOnNullChannel() {
      assertThrows(IllegalArgumentException.class,
          () -> nioIntegration.bulkWrite(null, 0, ByteBuffer.allocate(10)),
          "Should throw on null channel");
    }

    @Test
    @DisplayName("Should throw on null data")
    void shouldThrowOnNullData() throws IOException {
      final Path testFile = tempDir.resolve("bulkwrite2.txt");
      Files.createFile(testFile);

      try (final FileChannel channel = FileChannel.open(testFile, StandardOpenOption.WRITE)) {
        assertThrows(IllegalArgumentException.class,
            () -> nioIntegration.bulkWrite(channel, 0, null),
            "Should throw on null data");
      }
    }
  }

  @Nested
  @DisplayName("vectoredRead Tests")
  class VectoredReadTests {

    @Test
    @DisplayName("Should perform vectored read")
    void shouldPerformVectoredRead() throws IOException {
      final Path testFile = tempDir.resolve("vectoredread.txt");
      writeStringToFile(testFile, "0123456789ABCDEF");

      try (final FileChannel channel = FileChannel.open(testFile, StandardOpenOption.READ)) {
        final ByteBuffer[] buffers = new ByteBuffer[] {
            ByteBuffer.allocate(5),
            ByteBuffer.allocate(5)
        };

        final long bytesRead = nioIntegration.vectoredRead(channel, buffers, 0);

        assertTrue(bytesRead > 0, "Should read some bytes");
      }
    }

    @Test
    @DisplayName("Should throw on null buffers")
    void shouldThrowOnNullBuffers() throws IOException {
      final Path testFile = tempDir.resolve("vectoredread2.txt");
      writeStringToFile(testFile, "test");

      try (final FileChannel channel = FileChannel.open(testFile, StandardOpenOption.READ)) {
        assertThrows(IllegalArgumentException.class,
            () -> nioIntegration.vectoredRead(channel, null, 0),
            "Should throw on null buffers");
      }
    }
  }

  @Nested
  @DisplayName("vectoredWrite Tests")
  class VectoredWriteTests {

    @Test
    @DisplayName("Should perform vectored write")
    void shouldPerformVectoredWrite() throws IOException {
      final Path testFile = tempDir.resolve("vectoredwrite.txt");
      Files.createFile(testFile);

      try (final FileChannel channel = FileChannel.open(testFile, StandardOpenOption.WRITE)) {
        final ByteBuffer[] buffers = new ByteBuffer[] {
            ByteBuffer.wrap("Hello".getBytes()),
            ByteBuffer.wrap("World".getBytes())
        };

        final long bytesWritten = nioIntegration.vectoredWrite(channel, buffers, 0);

        assertEquals(10, bytesWritten, "Should write all bytes");
      }
    }
  }

  @Nested
  @DisplayName("asyncRead Tests")
  class AsyncReadTests {

    @Test
    @DisplayName("Should perform async read")
    void shouldPerformAsyncRead() throws Exception {
      final Path testFile = tempDir.resolve("asyncread.txt");
      writeStringToFile(testFile, "Async Hello!");

      final CompletableFuture<ByteBuffer> future =
          nioIntegration.asyncRead(testFile, 0, 100);

      final ByteBuffer buffer = future.get(5, TimeUnit.SECONDS);
      assertNotNull(buffer, "Buffer should not be null");
      assertTrue(buffer.remaining() > 0, "Buffer should have data");
    }

    @Test
    @DisplayName("Should throw on null path")
    void shouldThrowOnNullPath() {
      assertThrows(IllegalArgumentException.class,
          () -> nioIntegration.asyncRead(null, 0, 100),
          "Should throw on null path");
    }
  }

  @Nested
  @DisplayName("asyncWrite Tests")
  class AsyncWriteTests {

    @Test
    @DisplayName("Should perform async write")
    void shouldPerformAsyncWrite() throws Exception {
      final Path testFile = tempDir.resolve("asyncwrite.txt");

      final ByteBuffer data = ByteBuffer.wrap("Async Write!".getBytes());
      final CompletableFuture<Integer> future =
          nioIntegration.asyncWrite(testFile, 0, data);

      final Integer written = future.get(5, TimeUnit.SECONDS);
      assertEquals(12, written, "Should write all bytes");
      assertTrue(Files.exists(testFile), "File should exist");
    }

    @Test
    @DisplayName("Should throw on null path")
    void shouldThrowOnNullPath() {
      assertThrows(IllegalArgumentException.class,
          () -> nioIntegration.asyncWrite(null, 0, ByteBuffer.allocate(10)),
          "Should throw on null path");
    }

    @Test
    @DisplayName("Should throw on null data")
    void shouldThrowOnNullData() {
      assertThrows(IllegalArgumentException.class,
          () -> nioIntegration.asyncWrite(tempDir.resolve("test.txt"), 0, null),
          "Should throw on null data");
    }
  }

  @Nested
  @DisplayName("transferFile Tests")
  class TransferFileTests {

    @Test
    @DisplayName("Should transfer between file channels")
    void shouldTransferBetweenFileChannels() throws IOException {
      final Path sourceFile = tempDir.resolve("source.txt");
      final Path targetFile = tempDir.resolve("target.txt");
      writeStringToFile(sourceFile, "Transfer Data!");
      Files.createFile(targetFile);

      try (final FileChannel source = FileChannel.open(sourceFile, StandardOpenOption.READ);
           final FileChannel target = FileChannel.open(targetFile, StandardOpenOption.WRITE)) {

        final long transferred = nioIntegration.transferFile(source, 0, 14, target);

        assertEquals(14, transferred, "Should transfer all bytes");
      }

      assertEquals("Transfer Data!", readStringFromFile(targetFile), "Target should have data");
    }

    @Test
    @DisplayName("Should throw on null source")
    void shouldThrowOnNullSource() throws IOException {
      final Path targetFile = tempDir.resolve("target2.txt");
      Files.createFile(targetFile);

      try (final FileChannel target = FileChannel.open(targetFile, StandardOpenOption.WRITE)) {
        assertThrows(IllegalArgumentException.class,
            () -> nioIntegration.transferFile(null, 0, 10, target),
            "Should throw on null source");
      }
    }
  }

  @Nested
  @DisplayName("lockFile Tests")
  class LockFileTests {

    @Test
    @DisplayName("Should acquire exclusive lock")
    void shouldAcquireExclusiveLock() throws IOException {
      final Path testFile = tempDir.resolve("lock.txt");
      writeStringToFile(testFile, "Lock Test");

      try (final FileChannel channel = FileChannel.open(testFile,
          StandardOpenOption.READ, StandardOpenOption.WRITE)) {

        final FileLock lock = nioIntegration.lockFile(channel, 0, 10, false);

        assertNotNull(lock, "Lock should be acquired");
        assertTrue(lock.isValid(), "Lock should be valid");

        lock.release();
      }
    }

    @Test
    @DisplayName("Should acquire shared lock")
    void shouldAcquireSharedLock() throws IOException {
      final Path testFile = tempDir.resolve("sharedlock.txt");
      writeStringToFile(testFile, "Shared Lock Test");

      try (final FileChannel channel = FileChannel.open(testFile, StandardOpenOption.READ)) {
        final FileLock lock = nioIntegration.lockFile(channel, 0, 10, true);

        assertNotNull(lock, "Lock should be acquired");
        assertTrue(lock.isShared(), "Lock should be shared");

        lock.release();
      }
    }

    @Test
    @DisplayName("Should throw on null channel")
    void shouldThrowOnNullChannel() {
      assertThrows(IllegalArgumentException.class,
          () -> nioIntegration.lockFile(null, 0, 10, false),
          "Should throw on null channel");
    }

    @Test
    @DisplayName("Should throw on zero size")
    void shouldThrowOnZeroSize() throws IOException {
      final Path testFile = tempDir.resolve("locksize.txt");
      writeStringToFile(testFile, "test");

      try (final FileChannel channel = FileChannel.open(testFile,
          StandardOpenOption.READ, StandardOpenOption.WRITE)) {
        assertThrows(IllegalArgumentException.class,
            () -> nioIntegration.lockFile(channel, 0, 0, false),
            "Should throw on zero size");
      }
    }
  }

  @Nested
  @DisplayName("tryLockFile Tests")
  class TryLockFileTests {

    @Test
    @DisplayName("Should try to acquire lock")
    void shouldTryToAcquireLock() throws IOException {
      final Path testFile = tempDir.resolve("trylock.txt");
      writeStringToFile(testFile, "TryLock Test");

      try (final FileChannel channel = FileChannel.open(testFile,
          StandardOpenOption.READ, StandardOpenOption.WRITE)) {

        final FileLock lock = nioIntegration.tryLockFile(channel, 0, 10, false);

        assertNotNull(lock, "Lock should be acquired");
        lock.release();
      }
    }

    @Test
    @DisplayName("Should return null when lock unavailable")
    void shouldReturnNullWhenLockUnavailable() throws IOException {
      final Path testFile = tempDir.resolve("trylock2.txt");
      writeStringToFile(testFile, "TryLock Test 2");

      try (final FileChannel channel1 = FileChannel.open(testFile,
              StandardOpenOption.READ, StandardOpenOption.WRITE);
           final FileChannel channel2 = FileChannel.open(testFile,
               StandardOpenOption.READ, StandardOpenOption.WRITE)) {

        final FileLock lock1 = channel1.lock(0, 10, false);

        // Try to acquire overlapping lock
        final FileLock lock2 = nioIntegration.tryLockFile(channel2, 0, 10, false);

        assertNull(lock2, "Second lock should fail");
        lock1.release();
      }
    }
  }

  @Nested
  @DisplayName("shutdown Tests")
  class ShutdownTests {

    @Test
    @DisplayName("Should shutdown gracefully")
    void shouldShutdownGracefully() {
      nioIntegration.shutdown();
      // Just verify no exception is thrown
      assertTrue(true, "Shutdown should complete");
    }

    @Test
    @DisplayName("Should be idempotent")
    void shouldBeIdempotent() {
      nioIntegration.shutdown();
      nioIntegration.shutdown(); // Should not throw

      assertTrue(true, "Shutdown should be idempotent");
    }
  }

  /**
   * Helper method to write string to file - Java 8 compatible alternative to Files.writeString().
   *
   * @param path    the path to write to
   * @param content the string content to write
   * @throws IOException if writing fails
   */
  private static void writeStringToFile(final Path path, final String content) throws IOException {
    Files.write(path, content.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Helper method to read string from file - Java 8 compatible alternative to readStringFromFile().
   *
   * @param path the path to read from
   * @return the file content as a string
   * @throws IOException if reading fails
   */
  private static String readStringFromFile(final Path path) throws IOException {
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }
}
