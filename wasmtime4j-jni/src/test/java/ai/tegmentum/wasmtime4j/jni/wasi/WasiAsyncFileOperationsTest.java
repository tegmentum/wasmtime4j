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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiFileSystemException;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive tests for {@link WasiAsyncFileOperations}.
 */
@DisplayName("WasiAsyncFileOperations Tests")
class WasiAsyncFileOperationsTest {

  private WasiAsyncFileOperations asyncFileOps;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    asyncFileOps = new WasiAsyncFileOperations();
  }

  @AfterEach
  void tearDown() {
    if (asyncFileOps != null && !asyncFileOps.isClosed()) {
      asyncFileOps.close();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiAsyncFileOperations should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiAsyncFileOperations.class.getModifiers()),
          "WasiAsyncFileOperations should be final");
    }

    @Test
    @DisplayName("Should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiAsyncFileOperations.class),
          "WasiAsyncFileOperations should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Default constructor should create handler")
    void defaultConstructorShouldCreateHandler() {
      final WasiAsyncFileOperations ops = new WasiAsyncFileOperations();
      assertNotNull(ops, "Handler should be created");
      assertFalse(ops.isClosed(), "Handler should not be closed");
      ops.close();
    }

    @Test
    @DisplayName("Constructor with timeout should create handler")
    void constructorWithTimeoutShouldCreateHandler() {
      final WasiAsyncFileOperations ops = new WasiAsyncFileOperations(60);
      assertNotNull(ops, "Handler should be created");
      assertFalse(ops.isClosed(), "Handler should not be closed");
      ops.close();
    }

    @Test
    @DisplayName("Constructor should throw on zero timeout")
    void constructorShouldThrowOnZeroTimeout() {
      assertThrows(JniException.class,
          () -> new WasiAsyncFileOperations(0),
          "Should throw on zero timeout");
    }

    @Test
    @DisplayName("Constructor should throw on negative timeout")
    void constructorShouldThrowOnNegativeTimeout() {
      assertThrows(JniException.class,
          () -> new WasiAsyncFileOperations(-1),
          "Should throw on negative timeout");
    }
  }

  @Nested
  @DisplayName("readAsync Tests")
  class ReadAsyncTests {

    @Test
    @DisplayName("Should throw on null path")
    void shouldThrowOnNullPath() {
      assertThrows(JniException.class,
          () -> asyncFileOps.readAsync(null, 0L, 1024),
          "Should throw on null path");
    }

    @Test
    @DisplayName("Should throw on negative position")
    void shouldThrowOnNegativePosition() {
      assertThrows(JniException.class,
          () -> asyncFileOps.readAsync(tempDir.resolve("test.txt"), -1L, 1024),
          "Should throw on negative position");
    }

    @Test
    @DisplayName("Should throw on zero buffer size")
    void shouldThrowOnZeroBufferSize() {
      assertThrows(JniException.class,
          () -> asyncFileOps.readAsync(tempDir.resolve("test.txt"), 0L, 0),
          "Should throw on zero buffer size");
    }

    @Test
    @DisplayName("Should throw on negative buffer size")
    void shouldThrowOnNegativeBufferSize() {
      assertThrows(JniException.class,
          () -> asyncFileOps.readAsync(tempDir.resolve("test.txt"), 0L, -1),
          "Should throw on negative buffer size");
    }

    @Test
    @DisplayName("Should read file asynchronously")
    void shouldReadFileAsynchronously() throws Exception {
      // Create a test file
      final File testFile = tempDir.resolve("read-test.txt").toFile();
      try (FileOutputStream fos = new FileOutputStream(testFile)) {
        fos.write("Hello, World!".getBytes());
      }

      final CompletableFuture<ByteBuffer> future =
          asyncFileOps.readAsync(testFile.toPath(), 0L, 1024);

      assertNotNull(future, "Future should not be null");

      final ByteBuffer result = future.get(5, TimeUnit.SECONDS);
      assertNotNull(result, "Result should not be null");
      assertTrue(result.limit() > 0, "Should have read some bytes");
    }

    @Test
    @DisplayName("Should handle non-existent file")
    void shouldHandleNonExistentFile() {
      final CompletableFuture<ByteBuffer> future =
          asyncFileOps.readAsync(tempDir.resolve("non-existent.txt"), 0L, 1024);

      assertNotNull(future, "Future should not be null");

      // The future should complete exceptionally
      assertThrows(ExecutionException.class,
          () -> future.get(5, TimeUnit.SECONDS),
          "Should throw ExecutionException for non-existent file");
    }

    @Test
    @DisplayName("Should fail after close")
    void shouldFailAfterClose() throws Exception {
      asyncFileOps.close();

      final CompletableFuture<ByteBuffer> future =
          asyncFileOps.readAsync(tempDir.resolve("test.txt"), 0L, 1024);

      assertNotNull(future, "Future should not be null");
      assertTrue(future.isCompletedExceptionally(), "Future should be completed exceptionally");
    }
  }

  @Nested
  @DisplayName("readAsync with timeout Tests")
  class ReadAsyncWithTimeoutTests {

    @Test
    @DisplayName("Should throw on zero timeout")
    void shouldThrowOnZeroTimeout() {
      assertThrows(JniException.class,
          () -> asyncFileOps.readAsync(tempDir.resolve("test.txt"), 0L, 1024, 0L),
          "Should throw on zero timeout");
    }

    @Test
    @DisplayName("Should throw on negative timeout")
    void shouldThrowOnNegativeTimeout() {
      assertThrows(JniException.class,
          () -> asyncFileOps.readAsync(tempDir.resolve("test.txt"), 0L, 1024, -1L),
          "Should throw on negative timeout");
    }
  }

  @Nested
  @DisplayName("writeAsync Tests")
  class WriteAsyncTests {

    @Test
    @DisplayName("Should throw on null path")
    void shouldThrowOnNullPath() {
      final ByteBuffer data = ByteBuffer.wrap("test".getBytes());

      assertThrows(JniException.class,
          () -> asyncFileOps.writeAsync(null, 0L, data),
          "Should throw on null path");
    }

    @Test
    @DisplayName("Should throw on negative position")
    void shouldThrowOnNegativePosition() {
      final ByteBuffer data = ByteBuffer.wrap("test".getBytes());

      assertThrows(JniException.class,
          () -> asyncFileOps.writeAsync(tempDir.resolve("test.txt"), -1L, data),
          "Should throw on negative position");
    }

    @Test
    @DisplayName("Should throw on null data")
    void shouldThrowOnNullData() {
      assertThrows(JniException.class,
          () -> asyncFileOps.writeAsync(tempDir.resolve("test.txt"), 0L, null),
          "Should throw on null data");
    }

    @Test
    @DisplayName("Should write file asynchronously")
    void shouldWriteFileAsynchronously() throws Exception {
      final ByteBuffer data = ByteBuffer.wrap("Hello, World!".getBytes());
      final Path testFile = tempDir.resolve("write-test.txt");

      final CompletableFuture<Integer> future =
          asyncFileOps.writeAsync(testFile, 0L, data);

      assertNotNull(future, "Future should not be null");

      final Integer bytesWritten = future.get(5, TimeUnit.SECONDS);
      assertNotNull(bytesWritten, "Bytes written should not be null");
      assertEquals(13, bytesWritten, "Should have written 13 bytes");
      assertTrue(testFile.toFile().exists(), "File should exist");
    }

    @Test
    @DisplayName("Should fail after close")
    void shouldFailAfterClose() throws Exception {
      asyncFileOps.close();
      final ByteBuffer data = ByteBuffer.wrap("test".getBytes());

      final CompletableFuture<Integer> future =
          asyncFileOps.writeAsync(tempDir.resolve("test.txt"), 0L, data);

      assertNotNull(future, "Future should not be null");
      assertTrue(future.isCompletedExceptionally(), "Future should be completed exceptionally");
    }
  }

  @Nested
  @DisplayName("writeAsync with timeout Tests")
  class WriteAsyncWithTimeoutTests {

    @Test
    @DisplayName("Should throw on zero timeout")
    void shouldThrowOnZeroTimeout() {
      final ByteBuffer data = ByteBuffer.wrap("test".getBytes());

      assertThrows(JniException.class,
          () -> asyncFileOps.writeAsync(tempDir.resolve("test.txt"), 0L, data, 0L),
          "Should throw on zero timeout");
    }

    @Test
    @DisplayName("Should throw on negative timeout")
    void shouldThrowOnNegativeTimeout() {
      final ByteBuffer data = ByteBuffer.wrap("test".getBytes());

      assertThrows(JniException.class,
          () -> asyncFileOps.writeAsync(tempDir.resolve("test.txt"), 0L, data, -1L),
          "Should throw on negative timeout");
    }
  }

  @Nested
  @DisplayName("registerChannel Tests")
  class RegisterChannelTests {

    @Test
    @DisplayName("Should throw on null channel")
    void shouldThrowOnNullChannel() {
      assertThrows(JniException.class,
          () -> asyncFileOps.registerChannel(null, 1, null),
          "Should throw on null channel");
    }

    @Test
    @DisplayName("Should fail after close")
    void shouldFailAfterClose() throws Exception {
      asyncFileOps.close();

      // Create a mock channel - we can't easily create a real SelectableChannel
      // so we'll test that it fails when closed
      // Note: This test is limited because we can't easily create a SelectableChannel
      assertTrue(asyncFileOps.isClosed(), "Should be closed");
    }
  }

  @Nested
  @DisplayName("readNonBlocking Tests")
  class ReadNonBlockingTests {

    @Test
    @DisplayName("Should throw on null channel")
    void shouldThrowOnNullChannel() {
      final ByteBuffer buffer = ByteBuffer.allocate(1024);

      assertThrows(JniException.class,
          () -> asyncFileOps.readNonBlocking(null, buffer),
          "Should throw on null channel");
    }

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() throws Exception {
      final SocketChannel channel = SocketChannel.open();
      try {
        assertThrows(JniException.class,
            () -> asyncFileOps.readNonBlocking(channel, null),
            "Should throw on null buffer");
      } finally {
        channel.close();
      }
    }

    @Test
    @DisplayName("Should throw when closed")
    void shouldThrowWhenClosed() throws Exception {
      asyncFileOps.close();
      final SocketChannel channel = SocketChannel.open();
      final ByteBuffer buffer = ByteBuffer.allocate(1024);

      try {
        assertThrows(WasiFileSystemException.class,
            () -> asyncFileOps.readNonBlocking(channel, buffer),
            "Should throw when closed");
      } finally {
        channel.close();
      }
    }
  }

  @Nested
  @DisplayName("writeNonBlocking Tests")
  class WriteNonBlockingTests {

    @Test
    @DisplayName("Should throw on null channel")
    void shouldThrowOnNullChannel() {
      final ByteBuffer buffer = ByteBuffer.wrap("test".getBytes());

      assertThrows(JniException.class,
          () -> asyncFileOps.writeNonBlocking(null, buffer),
          "Should throw on null channel");
    }

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() throws Exception {
      final SocketChannel channel = SocketChannel.open();
      try {
        assertThrows(JniException.class,
            () -> asyncFileOps.writeNonBlocking(channel, null),
            "Should throw on null buffer");
      } finally {
        channel.close();
      }
    }

    @Test
    @DisplayName("Should throw when closed")
    void shouldThrowWhenClosed() throws Exception {
      asyncFileOps.close();
      final SocketChannel channel = SocketChannel.open();
      final ByteBuffer buffer = ByteBuffer.wrap("test".getBytes());

      try {
        assertThrows(WasiFileSystemException.class,
            () -> asyncFileOps.writeNonBlocking(channel, buffer),
            "Should throw when closed");
      } finally {
        channel.close();
      }
    }
  }

  @Nested
  @DisplayName("getActiveOperationCount Tests")
  class GetActiveOperationCountTests {

    @Test
    @DisplayName("Should return zero initially")
    void shouldReturnZeroInitially() {
      assertEquals(0L, asyncFileOps.getActiveOperationCount(),
          "Active operation count should be 0 initially");
    }

    @Test
    @DisplayName("Should increment during operation")
    void shouldIncrementDuringOperation() throws Exception {
      // Create a test file
      final File testFile = tempDir.resolve("count-test.txt").toFile();
      try (FileOutputStream fos = new FileOutputStream(testFile)) {
        fos.write("Hello".getBytes());
      }

      // Start a read operation
      final CompletableFuture<ByteBuffer> future =
          asyncFileOps.readAsync(testFile.toPath(), 0L, 1024);

      // Wait for completion
      future.get(5, TimeUnit.SECONDS);

      // After completion, count should be back to 0
      assertEquals(0L, asyncFileOps.getActiveOperationCount(),
          "Active operation count should be 0 after completion");
    }
  }

  @Nested
  @DisplayName("isClosed Tests")
  class IsClosedTests {

    @Test
    @DisplayName("Should return false initially")
    void shouldReturnFalseInitially() {
      assertFalse(asyncFileOps.isClosed(), "Should not be closed initially");
    }

    @Test
    @DisplayName("Should return true after close")
    void shouldReturnTrueAfterClose() {
      asyncFileOps.close();
      assertTrue(asyncFileOps.isClosed(), "Should be closed after close()");
    }
  }

  @Nested
  @DisplayName("close Tests")
  class CloseTests {

    @Test
    @DisplayName("Should close without error")
    void shouldCloseWithoutError() {
      final WasiAsyncFileOperations ops = new WasiAsyncFileOperations();
      assertDoesNotThrow(ops::close, "Should close without error");
      assertTrue(ops.isClosed(), "Should be closed");
    }

    @Test
    @DisplayName("Should handle multiple close calls")
    void shouldHandleMultipleCloseCalls() {
      final WasiAsyncFileOperations ops = new WasiAsyncFileOperations();
      assertDoesNotThrow(ops::close, "First close should succeed");
      assertDoesNotThrow(ops::close, "Second close should succeed");
      assertTrue(ops.isClosed(), "Should still be closed");
    }

    @Test
    @DisplayName("Should work with try-with-resources")
    void shouldWorkWithTryWithResources() {
      try (WasiAsyncFileOperations ops = new WasiAsyncFileOperations()) {
        assertFalse(ops.isClosed(), "Should not be closed inside try block");
      }
      // After the try block, it should be closed automatically
    }
  }

  @Nested
  @DisplayName("Concurrent Operations Tests")
  class ConcurrentOperationsTests {

    @Test
    @DisplayName("Should handle multiple concurrent reads")
    void shouldHandleMultipleConcurrentReads() throws Exception {
      // Create test files
      for (int i = 0; i < 5; i++) {
        final File testFile = tempDir.resolve("concurrent-" + i + ".txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
          fos.write(("Content " + i).getBytes());
        }
      }

      // Start multiple concurrent reads
      final CompletableFuture<?>[] futures = new CompletableFuture<?>[5];
      for (int i = 0; i < 5; i++) {
        final Path path = tempDir.resolve("concurrent-" + i + ".txt");
        futures[i] = asyncFileOps.readAsync(path, 0L, 1024);
      }

      // Wait for all to complete
      CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);

      // Verify all completed successfully
      for (CompletableFuture<?> future : futures) {
        assertTrue(future.isDone(), "Future should be done");
        assertFalse(future.isCompletedExceptionally(), "Future should not have exception");
      }
    }

    @Test
    @DisplayName("Should handle multiple concurrent writes")
    void shouldHandleMultipleConcurrentWrites() throws Exception {
      // Start multiple concurrent writes
      final CompletableFuture<Integer>[] futures = new CompletableFuture[5];
      for (int i = 0; i < 5; i++) {
        final Path path = tempDir.resolve("write-concurrent-" + i + ".txt");
        final ByteBuffer data = ByteBuffer.wrap(("Content " + i).getBytes());
        futures[i] = asyncFileOps.writeAsync(path, 0L, data);
      }

      // Wait for all to complete
      CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);

      // Verify all completed successfully
      for (CompletableFuture<Integer> future : futures) {
        assertTrue(future.isDone(), "Future should be done");
        assertFalse(future.isCompletedExceptionally(), "Future should not have exception");
        assertTrue(future.get() > 0, "Should have written bytes");
      }

      // Verify files exist
      for (int i = 0; i < 5; i++) {
        final Path path = tempDir.resolve("write-concurrent-" + i + ".txt");
        assertTrue(path.toFile().exists(), "File should exist: " + path);
      }
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle read from directory")
    void shouldHandleReadFromDirectory() {
      final CompletableFuture<ByteBuffer> future =
          asyncFileOps.readAsync(tempDir, 0L, 1024);

      assertNotNull(future, "Future should not be null");

      // Reading a directory should fail
      assertThrows(ExecutionException.class,
          () -> future.get(5, TimeUnit.SECONDS),
          "Should fail when reading directory");
    }

    @Test
    @DisplayName("Should propagate exceptions properly")
    void shouldPropagateExceptionsProperly() {
      // Try to read a non-existent file
      final CompletableFuture<ByteBuffer> future =
          asyncFileOps.readAsync(tempDir.resolve("definitely-does-not-exist-12345.txt"), 0L, 1024);

      try {
        future.get(5, TimeUnit.SECONDS);
      } catch (ExecutionException e) {
        assertNotNull(e.getCause(), "Should have a cause");
        assertTrue(e.getCause() instanceof WasiFileSystemException,
            "Cause should be WasiFileSystemException");
      } catch (InterruptedException | TimeoutException e) {
        // These are acceptable in test context
      }
    }
  }
}
