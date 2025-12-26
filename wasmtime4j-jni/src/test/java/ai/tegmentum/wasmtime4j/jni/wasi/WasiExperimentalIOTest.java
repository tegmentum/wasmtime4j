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
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link WasiExperimentalIO}.
 */
@DisplayName("WasiExperimentalIO Tests")
class WasiExperimentalIOTest {

  private WasiContext testContext;
  private ExecutorService executorService;
  private WasiExperimentalIO experimentalIO;

  @BeforeEach
  void setUp() {
    testContext = TestWasiContextFactory.createTestContext();
    executorService = Executors.newSingleThreadExecutor();
    experimentalIO = new WasiExperimentalIO(testContext, executorService);
  }

  @AfterEach
  void tearDown() {
    if (experimentalIO != null) {
      experimentalIO.close();
    }
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiExperimentalIO should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiExperimentalIO.class.getModifiers()),
          "WasiExperimentalIO should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should throw on null context")
    void constructorShouldThrowOnNullContext() {
      assertThrows(JniException.class,
          () -> new WasiExperimentalIO(null, executorService),
          "Should throw on null context");
    }

    @Test
    @DisplayName("Constructor should throw on null executor")
    void constructorShouldThrowOnNullExecutor() {
      assertThrows(JniException.class,
          () -> new WasiExperimentalIO(testContext, null),
          "Should throw on null executor");
    }

    @Test
    @DisplayName("Constructor should create handler with valid parameters")
    void constructorShouldCreateHandlerWithValidParameters() {
      final WasiExperimentalIO io = new WasiExperimentalIO(testContext, executorService);
      assertNotNull(io, "Handler should be created");
      io.close();
    }
  }

  @Nested
  @DisplayName("asyncReadFileAsync Tests")
  class AsyncReadFileAsyncTests {

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() {
      final Consumer<WasiExperimentalIO.AsyncIOResult> callback = result -> {};

      assertThrows(JniException.class,
          () -> experimentalIO.asyncReadFileAsync(1L, 0L, null, callback),
          "Should throw on null buffer");
    }

    @Test
    @DisplayName("Should throw on null callback")
    void shouldThrowOnNullCallback() {
      final ByteBuffer buffer = ByteBuffer.allocate(1024);

      assertThrows(JniException.class,
          () -> experimentalIO.asyncReadFileAsync(1L, 0L, buffer, null),
          "Should throw on null callback");
    }

    @Test
    @DisplayName("Should throw on negative offset")
    void shouldThrowOnNegativeOffset() {
      final ByteBuffer buffer = ByteBuffer.allocate(1024);
      final Consumer<WasiExperimentalIO.AsyncIOResult> callback = result -> {};

      assertThrows(JniException.class,
          () -> experimentalIO.asyncReadFileAsync(1L, -1L, buffer, callback),
          "Should throw on negative offset");
    }

    @Test
    @DisplayName("Should complete immediately with empty buffer")
    void shouldCompleteImmediatelyWithEmptyBuffer() throws Exception {
      final ByteBuffer buffer = ByteBuffer.allocate(0);
      final Consumer<WasiExperimentalIO.AsyncIOResult> callback = result -> {
        assertEquals(0, result.bytesTransferred, "Bytes transferred should be 0");
        assertTrue(result.completed, "Should be completed");
      };

      final CompletableFuture<Long> future =
          experimentalIO.asyncReadFileAsync(1L, 0L, buffer, callback);
      assertNotNull(future, "Future should not be null");
      assertEquals(0L, future.get(5, TimeUnit.SECONDS), "Handle should be 0 for empty buffer");
    }
  }

  @Nested
  @DisplayName("asyncWriteFileAsync Tests")
  class AsyncWriteFileAsyncTests {

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() {
      final Consumer<WasiExperimentalIO.AsyncIOResult> callback = result -> {};

      assertThrows(JniException.class,
          () -> experimentalIO.asyncWriteFileAsync(1L, 0L, null, callback),
          "Should throw on null buffer");
    }

    @Test
    @DisplayName("Should throw on null callback")
    void shouldThrowOnNullCallback() {
      final ByteBuffer buffer = ByteBuffer.allocate(1024);

      assertThrows(JniException.class,
          () -> experimentalIO.asyncWriteFileAsync(1L, 0L, buffer, null),
          "Should throw on null callback");
    }

    @Test
    @DisplayName("Should throw on negative offset")
    void shouldThrowOnNegativeOffset() {
      final ByteBuffer buffer = ByteBuffer.allocate(1024);
      final Consumer<WasiExperimentalIO.AsyncIOResult> callback = result -> {};

      assertThrows(JniException.class,
          () -> experimentalIO.asyncWriteFileAsync(1L, -1L, buffer, callback),
          "Should throw on negative offset");
    }

    @Test
    @DisplayName("Should complete immediately with empty buffer")
    void shouldCompleteImmediatelyWithEmptyBuffer() throws Exception {
      final ByteBuffer buffer = ByteBuffer.allocate(0);
      final Consumer<WasiExperimentalIO.AsyncIOResult> callback = result -> {
        assertEquals(0, result.bytesTransferred, "Bytes transferred should be 0");
        assertTrue(result.completed, "Should be completed");
      };

      final CompletableFuture<Long> future =
          experimentalIO.asyncWriteFileAsync(1L, 0L, buffer, callback);
      assertNotNull(future, "Future should not be null");
      assertEquals(0L, future.get(5, TimeUnit.SECONDS), "Handle should be 0 for empty buffer");
    }
  }

  @Nested
  @DisplayName("createMemoryMappingAsync Tests")
  class CreateMemoryMappingAsyncTests {

    @Test
    @DisplayName("Should throw on negative offset")
    void shouldThrowOnNegativeOffset() {
      assertThrows(JniException.class,
          () -> experimentalIO.createMemoryMappingAsync(
              1L, -1L, 1024L,
              WasiExperimentalIO.MemoryProtection.READ_ONLY,
              WasiExperimentalIO.MappingFlags.PRIVATE),
          "Should throw on negative offset");
    }

    @Test
    @DisplayName("Should throw on zero length")
    void shouldThrowOnZeroLength() {
      assertThrows(JniException.class,
          () -> experimentalIO.createMemoryMappingAsync(
              1L, 0L, 0L,
              WasiExperimentalIO.MemoryProtection.READ_ONLY,
              WasiExperimentalIO.MappingFlags.PRIVATE),
          "Should throw on zero length");
    }

    @Test
    @DisplayName("Should throw on null protection")
    void shouldThrowOnNullProtection() {
      assertThrows(JniException.class,
          () -> experimentalIO.createMemoryMappingAsync(
              1L, 0L, 1024L,
              null,
              WasiExperimentalIO.MappingFlags.PRIVATE),
          "Should throw on null protection");
    }

    @Test
    @DisplayName("Should throw on null flags")
    void shouldThrowOnNullFlags() {
      assertThrows(JniException.class,
          () -> experimentalIO.createMemoryMappingAsync(
              1L, 0L, 1024L,
              WasiExperimentalIO.MemoryProtection.READ_ONLY,
              null),
          "Should throw on null flags");
    }

    @Test
    @DisplayName("Should throw on too large mapping")
    void shouldThrowOnTooLargeMapping() {
      final long tooLarge = 3L * 1024L * 1024L * 1024L; // 3GB

      assertThrows(WasiException.class,
          () -> experimentalIO.createMemoryMappingAsync(
              1L, 0L, tooLarge,
              WasiExperimentalIO.MemoryProtection.READ_ONLY,
              WasiExperimentalIO.MappingFlags.PRIVATE),
          "Should throw on mapping too large");
    }

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final CompletableFuture<Long> future = experimentalIO.createMemoryMappingAsync(
          1L, 0L, 1024L,
          WasiExperimentalIO.MemoryProtection.READ_ONLY,
          WasiExperimentalIO.MappingFlags.PRIVATE);

      assertNotNull(future, "Future should not be null");
      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("acquireFileLockAsync Tests")
  class AcquireFileLockAsyncTests {

    @Test
    @DisplayName("Should throw on negative offset")
    void shouldThrowOnNegativeOffset() {
      assertThrows(JniException.class,
          () -> experimentalIO.acquireFileLockAsync(
              1L, -1L, 100L,
              WasiExperimentalIO.FileLockType.EXCLUSIVE,
              false),
          "Should throw on negative offset");
    }

    @Test
    @DisplayName("Should throw on negative length")
    void shouldThrowOnNegativeLength() {
      assertThrows(JniException.class,
          () -> experimentalIO.acquireFileLockAsync(
              1L, 0L, -1L,
              WasiExperimentalIO.FileLockType.EXCLUSIVE,
              false),
          "Should throw on negative length");
    }

    @Test
    @DisplayName("Should throw on null lock type")
    void shouldThrowOnNullLockType() {
      assertThrows(JniException.class,
          () -> experimentalIO.acquireFileLockAsync(
              1L, 0L, 100L,
              null,
              false),
          "Should throw on null lock type");
    }

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final CompletableFuture<Long> future = experimentalIO.acquireFileLockAsync(
          1L, 0L, 100L,
          WasiExperimentalIO.FileLockType.EXCLUSIVE,
          false);

      assertNotNull(future, "Future should not be null");
      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("createDirectoryWatcherAsync Tests")
  class CreateDirectoryWatcherAsyncTests {

    @Test
    @DisplayName("Should throw on null path")
    void shouldThrowOnNullPath() {
      final Consumer<WasiExperimentalIO.FileSystemEvent> callback = event -> {};

      assertThrows(JniException.class,
          () -> experimentalIO.createDirectoryWatcherAsync(
              null, true,
              WasiExperimentalIO.FileSystemEventMask.ALL,
              callback),
          "Should throw on null path");
    }

    @Test
    @DisplayName("Should throw on empty path")
    void shouldThrowOnEmptyPath() {
      final Consumer<WasiExperimentalIO.FileSystemEvent> callback = event -> {};

      assertThrows(JniException.class,
          () -> experimentalIO.createDirectoryWatcherAsync(
              "", true,
              WasiExperimentalIO.FileSystemEventMask.ALL,
              callback),
          "Should throw on empty path");
    }

    @Test
    @DisplayName("Should throw on null event mask")
    void shouldThrowOnNullEventMask() {
      final Consumer<WasiExperimentalIO.FileSystemEvent> callback = event -> {};

      assertThrows(JniException.class,
          () -> experimentalIO.createDirectoryWatcherAsync(
              "/tmp", true,
              null,
              callback),
          "Should throw on null event mask");
    }

    @Test
    @DisplayName("Should throw on null callback")
    void shouldThrowOnNullCallback() {
      assertThrows(JniException.class,
          () -> experimentalIO.createDirectoryWatcherAsync(
              "/tmp", true,
              WasiExperimentalIO.FileSystemEventMask.ALL,
              null),
          "Should throw on null callback");
    }

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final Consumer<WasiExperimentalIO.FileSystemEvent> callback = event -> {};

      final CompletableFuture<Long> future = experimentalIO.createDirectoryWatcherAsync(
          "/tmp", true,
          WasiExperimentalIO.FileSystemEventMask.ALL,
          callback);

      assertNotNull(future, "Future should not be null");
      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("vectoredReadAsync Tests")
  class VectoredReadAsyncTests {

    @Test
    @DisplayName("Should throw on null buffers")
    void shouldThrowOnNullBuffers() {
      assertThrows(JniException.class,
          () -> experimentalIO.vectoredReadAsync(1L, 0L, null),
          "Should throw on null buffers");
    }

    @Test
    @DisplayName("Should throw on negative offset")
    void shouldThrowOnNegativeOffset() {
      final List<ByteBuffer> buffers = new ArrayList<>();
      buffers.add(ByteBuffer.allocate(1024));

      assertThrows(JniException.class,
          () -> experimentalIO.vectoredReadAsync(1L, -1L, buffers),
          "Should throw on negative offset");
    }

    @Test
    @DisplayName("Should complete immediately with empty buffer list")
    void shouldCompleteImmediatelyWithEmptyBufferList() throws Exception {
      final List<ByteBuffer> buffers = new ArrayList<>();

      final CompletableFuture<Integer> future =
          experimentalIO.vectoredReadAsync(1L, 0L, buffers);

      assertNotNull(future, "Future should not be null");
      assertEquals(0, future.get(5, TimeUnit.SECONDS), "Should return 0 for empty list");
    }

    @Test
    @DisplayName("Should throw on too many buffers")
    void shouldThrowOnTooManyBuffers() {
      final List<ByteBuffer> buffers = new ArrayList<>();
      for (int i = 0; i < 1025; i++) {
        buffers.add(ByteBuffer.allocate(1));
      }

      assertThrows(WasiException.class,
          () -> experimentalIO.vectoredReadAsync(1L, 0L, buffers),
          "Should throw on too many buffers");
    }
  }

  @Nested
  @DisplayName("vectoredWriteAsync Tests")
  class VectoredWriteAsyncTests {

    @Test
    @DisplayName("Should throw on null buffers")
    void shouldThrowOnNullBuffers() {
      assertThrows(JniException.class,
          () -> experimentalIO.vectoredWriteAsync(1L, 0L, null),
          "Should throw on null buffers");
    }

    @Test
    @DisplayName("Should throw on negative offset")
    void shouldThrowOnNegativeOffset() {
      final List<ByteBuffer> buffers = new ArrayList<>();
      buffers.add(ByteBuffer.allocate(1024));

      assertThrows(JniException.class,
          () -> experimentalIO.vectoredWriteAsync(1L, -1L, buffers),
          "Should throw on negative offset");
    }

    @Test
    @DisplayName("Should complete immediately with empty buffer list")
    void shouldCompleteImmediatelyWithEmptyBufferList() throws Exception {
      final List<ByteBuffer> buffers = new ArrayList<>();

      final CompletableFuture<Integer> future =
          experimentalIO.vectoredWriteAsync(1L, 0L, buffers);

      assertNotNull(future, "Future should not be null");
      assertEquals(0, future.get(5, TimeUnit.SECONDS), "Should return 0 for empty list");
    }

    @Test
    @DisplayName("Should throw on too many buffers")
    void shouldThrowOnTooManyBuffers() {
      final List<ByteBuffer> buffers = new ArrayList<>();
      for (int i = 0; i < 1025; i++) {
        buffers.add(ByteBuffer.allocate(1));
      }

      assertThrows(WasiException.class,
          () -> experimentalIO.vectoredWriteAsync(1L, 0L, buffers),
          "Should throw on too many buffers");
    }
  }

  @Nested
  @DisplayName("releaseFileLock Tests")
  class ReleaseFileLockTests {

    @Test
    @DisplayName("Should throw on invalid lock handle")
    void shouldThrowOnInvalidLockHandle() {
      assertThrows(WasiException.class,
          () -> experimentalIO.releaseFileLock(999L),
          "Should throw on invalid lock handle");
    }
  }

  @Nested
  @DisplayName("unmapMemoryMapping Tests")
  class UnmapMemoryMappingTests {

    @Test
    @DisplayName("Should throw on invalid mapping handle")
    void shouldThrowOnInvalidMappingHandle() {
      assertThrows(WasiException.class,
          () -> experimentalIO.unmapMemoryMapping(999L),
          "Should throw on invalid mapping handle");
    }
  }

  @Nested
  @DisplayName("stopDirectoryWatcher Tests")
  class StopDirectoryWatcherTests {

    @Test
    @DisplayName("Should handle invalid watcher handle gracefully")
    void shouldHandleInvalidWatcherHandleGracefully() {
      // Should not throw, just log
      assertDoesNotThrow(
          () -> experimentalIO.stopDirectoryWatcher(999L),
          "Should handle invalid watcher handle gracefully");
    }
  }

  @Nested
  @DisplayName("close Tests")
  class CloseTests {

    @Test
    @DisplayName("Should close without error")
    void shouldCloseWithoutError() {
      final WasiExperimentalIO io = new WasiExperimentalIO(testContext, executorService);
      assertDoesNotThrow(io::close, "Should close without error");
    }

    @Test
    @DisplayName("Should handle multiple close calls")
    void shouldHandleMultipleCloseCalls() {
      final WasiExperimentalIO io = new WasiExperimentalIO(testContext, executorService);
      assertDoesNotThrow(io::close, "First close should succeed");
      assertDoesNotThrow(io::close, "Second close should succeed");
    }
  }

  @Nested
  @DisplayName("IOOperationType Enum Tests")
  class IOOperationTypeEnumTests {

    @Test
    @DisplayName("Should have expected values")
    void shouldHaveExpectedValues() {
      final WasiExperimentalIO.IOOperationType[] values =
          WasiExperimentalIO.IOOperationType.values();
      assertEquals(3, values.length, "Should have 3 values");
      assertNotNull(WasiExperimentalIO.IOOperationType.READ, "READ should exist");
      assertNotNull(WasiExperimentalIO.IOOperationType.WRITE, "WRITE should exist");
      assertNotNull(WasiExperimentalIO.IOOperationType.SYNC, "SYNC should exist");
    }

    @Test
    @DisplayName("Should support valueOf")
    void shouldSupportValueOf() {
      assertEquals(WasiExperimentalIO.IOOperationType.READ,
          WasiExperimentalIO.IOOperationType.valueOf("READ"),
          "valueOf should work for READ");
    }
  }

  @Nested
  @DisplayName("MemoryProtection Enum Tests")
  class MemoryProtectionEnumTests {

    @Test
    @DisplayName("Should have expected values")
    void shouldHaveExpectedValues() {
      final WasiExperimentalIO.MemoryProtection[] values =
          WasiExperimentalIO.MemoryProtection.values();
      assertEquals(4, values.length, "Should have 4 values");
      assertNotNull(WasiExperimentalIO.MemoryProtection.READ_ONLY, "READ_ONLY should exist");
      assertNotNull(WasiExperimentalIO.MemoryProtection.READ_WRITE, "READ_WRITE should exist");
      assertNotNull(WasiExperimentalIO.MemoryProtection.EXECUTE_READ, "EXECUTE_READ should exist");
      assertNotNull(WasiExperimentalIO.MemoryProtection.EXECUTE_READ_WRITE,
          "EXECUTE_READ_WRITE should exist");
    }
  }

  @Nested
  @DisplayName("FileLockType Enum Tests")
  class FileLockTypeEnumTests {

    @Test
    @DisplayName("Should have expected values")
    void shouldHaveExpectedValues() {
      final WasiExperimentalIO.FileLockType[] values =
          WasiExperimentalIO.FileLockType.values();
      assertEquals(3, values.length, "Should have 3 values");
      assertNotNull(WasiExperimentalIO.FileLockType.SHARED, "SHARED should exist");
      assertNotNull(WasiExperimentalIO.FileLockType.EXCLUSIVE, "EXCLUSIVE should exist");
      assertNotNull(WasiExperimentalIO.FileLockType.ADVISORY, "ADVISORY should exist");
    }
  }

  @Nested
  @DisplayName("FileSystemEventType Enum Tests")
  class FileSystemEventTypeEnumTests {

    @Test
    @DisplayName("Should have expected values")
    void shouldHaveExpectedValues() {
      final WasiExperimentalIO.FileSystemEventType[] values =
          WasiExperimentalIO.FileSystemEventType.values();
      assertEquals(6, values.length, "Should have 6 values");
      assertNotNull(WasiExperimentalIO.FileSystemEventType.CREATED, "CREATED should exist");
      assertNotNull(WasiExperimentalIO.FileSystemEventType.MODIFIED, "MODIFIED should exist");
      assertNotNull(WasiExperimentalIO.FileSystemEventType.DELETED, "DELETED should exist");
      assertNotNull(WasiExperimentalIO.FileSystemEventType.RENAMED, "RENAMED should exist");
      assertNotNull(WasiExperimentalIO.FileSystemEventType.MOVED, "MOVED should exist");
      assertNotNull(WasiExperimentalIO.FileSystemEventType.ATTRIBUTE_CHANGED,
          "ATTRIBUTE_CHANGED should exist");
    }
  }

  @Nested
  @DisplayName("MappingFlags Tests")
  class MappingFlagsTests {

    @Test
    @DisplayName("Should have expected static values")
    void shouldHaveExpectedStaticValues() {
      assertNotNull(WasiExperimentalIO.MappingFlags.PRIVATE, "PRIVATE should exist");
      assertNotNull(WasiExperimentalIO.MappingFlags.SHARED, "SHARED should exist");
      assertNotNull(WasiExperimentalIO.MappingFlags.ANONYMOUS, "ANONYMOUS should exist");
      assertNotNull(WasiExperimentalIO.MappingFlags.FIXED, "FIXED should exist");
    }

    @Test
    @DisplayName("Should have expected values")
    void shouldHaveExpectedValues() {
      assertEquals(0x01, WasiExperimentalIO.MappingFlags.PRIVATE.value, "PRIVATE value");
      assertEquals(0x02, WasiExperimentalIO.MappingFlags.SHARED.value, "SHARED value");
      assertEquals(0x04, WasiExperimentalIO.MappingFlags.ANONYMOUS.value, "ANONYMOUS value");
      assertEquals(0x08, WasiExperimentalIO.MappingFlags.FIXED.value, "FIXED value");
    }
  }

  @Nested
  @DisplayName("FileSystemEventMask Tests")
  class FileSystemEventMaskTests {

    @Test
    @DisplayName("Should have expected static values")
    void shouldHaveExpectedStaticValues() {
      assertNotNull(WasiExperimentalIO.FileSystemEventMask.ALL, "ALL should exist");
      assertNotNull(WasiExperimentalIO.FileSystemEventMask.CREATE, "CREATE should exist");
      assertNotNull(WasiExperimentalIO.FileSystemEventMask.MODIFY, "MODIFY should exist");
      assertNotNull(WasiExperimentalIO.FileSystemEventMask.DELETE, "DELETE should exist");
      assertNotNull(WasiExperimentalIO.FileSystemEventMask.RENAME, "RENAME should exist");
      assertNotNull(WasiExperimentalIO.FileSystemEventMask.MOVE, "MOVE should exist");
      assertNotNull(WasiExperimentalIO.FileSystemEventMask.ATTRIBUTE, "ATTRIBUTE should exist");
    }

    @Test
    @DisplayName("Should have expected values")
    void shouldHaveExpectedValues() {
      assertEquals(0xFF, WasiExperimentalIO.FileSystemEventMask.ALL.value, "ALL value");
      assertEquals(0x01, WasiExperimentalIO.FileSystemEventMask.CREATE.value, "CREATE value");
      assertEquals(0x02, WasiExperimentalIO.FileSystemEventMask.MODIFY.value, "MODIFY value");
      assertEquals(0x04, WasiExperimentalIO.FileSystemEventMask.DELETE.value, "DELETE value");
      assertEquals(0x08, WasiExperimentalIO.FileSystemEventMask.RENAME.value, "RENAME value");
      assertEquals(0x10, WasiExperimentalIO.FileSystemEventMask.MOVE.value, "MOVE value");
      assertEquals(0x20, WasiExperimentalIO.FileSystemEventMask.ATTRIBUTE.value, "ATTRIBUTE value");
    }
  }

  @Nested
  @DisplayName("AsyncIOResult Tests")
  class AsyncIOResultTests {

    @Test
    @DisplayName("Should create result with values")
    void shouldCreateResultWithValues() {
      final WasiExperimentalIO.AsyncIOResult result =
          new WasiExperimentalIO.AsyncIOResult(1024, null, true);

      assertEquals(1024, result.bytesTransferred, "Bytes transferred should match");
      assertFalse(result.error != null, "Error should be null");
      assertTrue(result.completed, "Should be completed");
    }

    @Test
    @DisplayName("Should create result with error")
    void shouldCreateResultWithError() {
      final WasiException error = new WasiException("Test error",
          ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode.EIO);
      final WasiExperimentalIO.AsyncIOResult result =
          new WasiExperimentalIO.AsyncIOResult(0, error, false);

      assertEquals(0, result.bytesTransferred, "Bytes transferred should be 0");
      assertNotNull(result.error, "Error should not be null");
      assertFalse(result.completed, "Should not be completed");
    }
  }

  @Nested
  @DisplayName("FileSystemEvent Tests")
  class FileSystemEventTests {

    @Test
    @DisplayName("Should create event with values")
    void shouldCreateEventWithValues() {
      final WasiExperimentalIO.FileSystemEvent event =
          new WasiExperimentalIO.FileSystemEvent(
              WasiExperimentalIO.FileSystemEventType.CREATED,
              "/path/to/file",
              null,
              System.currentTimeMillis());

      assertEquals(WasiExperimentalIO.FileSystemEventType.CREATED, event.eventType,
          "Event type should match");
      assertEquals("/path/to/file", event.path, "Path should match");
      assertFalse(event.oldPath != null, "Old path should be null");
      assertTrue(event.timestamp > 0, "Timestamp should be positive");
    }

    @Test
    @DisplayName("Should create rename event with old path")
    void shouldCreateRenameEventWithOldPath() {
      final WasiExperimentalIO.FileSystemEvent event =
          new WasiExperimentalIO.FileSystemEvent(
              WasiExperimentalIO.FileSystemEventType.RENAMED,
              "/new/path",
              "/old/path",
              System.currentTimeMillis());

      assertEquals(WasiExperimentalIO.FileSystemEventType.RENAMED, event.eventType,
          "Event type should match");
      assertEquals("/new/path", event.path, "New path should match");
      assertEquals("/old/path", event.oldPath, "Old path should match");
    }
  }
}
