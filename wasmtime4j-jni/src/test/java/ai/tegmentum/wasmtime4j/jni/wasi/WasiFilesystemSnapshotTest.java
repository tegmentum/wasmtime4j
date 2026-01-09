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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiFilesystemSnapshot.CompressionStatistics;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiFilesystemSnapshot.DeduplicationStatistics;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiFilesystemSnapshot.RestoreOptions;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiFilesystemSnapshot.SnapshotInfo;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiFilesystemSnapshot.SnapshotMetadata;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiFilesystemSnapshot.SnapshotOptions;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiFilesystemSnapshot.SnapshotPerformanceMetrics;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiFilesystemSnapshot.SnapshotType;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiFilesystemSnapshot.SnapshotVerificationResult;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link WasiFilesystemSnapshot}. */
@DisplayName("WasiFilesystemSnapshot Tests")
class WasiFilesystemSnapshotTest {

  private WasiContext testContext;
  private ExecutorService executorService;
  private WasiFilesystemSnapshot snapshotHandler;
  private boolean setupSucceeded = false;

  @BeforeEach
  void setUp() {
    testContext = TestWasiContextFactory.createTestContext();
    executorService = Executors.newSingleThreadExecutor();
    try {
      snapshotHandler = new WasiFilesystemSnapshot(testContext, executorService);
      setupSucceeded = true;
    } catch (final UnsatisfiedLinkError e) {
      // Native library not available - tests will be skipped via assumption
      setupSucceeded = false;
    }
    Assumptions.assumeTrue(setupSucceeded, "Native library not available - skipping test");
  }

  @AfterEach
  void tearDown() {
    if (snapshotHandler != null) {
      try {
        snapshotHandler.close();
      } catch (final UnsatisfiedLinkError e) {
        // Ignore - native library not available
      }
    }
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiFilesystemSnapshot should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiFilesystemSnapshot.class.getModifiers()),
          "WasiFilesystemSnapshot should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should throw on null context")
    void shouldThrowOnNullContext() {
      assertThrows(
          JniException.class,
          () -> new WasiFilesystemSnapshot(null, executorService),
          "Should throw on null context");
    }

    @Test
    @DisplayName("Should throw on null executor")
    void shouldThrowOnNullExecutor() {
      assertThrows(
          JniException.class,
          () -> new WasiFilesystemSnapshot(testContext, null),
          "Should throw on null executor");
    }

    @Test
    @DisplayName("Should create snapshot handler with valid parameters")
    void shouldCreateSnapshotHandlerWithValidParameters() {
      final WasiFilesystemSnapshot handler =
          new WasiFilesystemSnapshot(testContext, executorService);

      assertNotNull(handler, "Snapshot handler should be created");
      handler.close();
    }
  }

  @Nested
  @DisplayName("createFullSnapshotAsync Tests")
  class CreateFullSnapshotAsyncTests {

    @Test
    @DisplayName("Should throw on null root path")
    void shouldThrowOnNullRootPath() {
      assertThrows(
          JniException.class,
          () -> snapshotHandler.createFullSnapshotAsync(null, SnapshotOptions.defaultOptions()),
          "Should throw on null root path");
    }

    @Test
    @DisplayName("Should throw on empty root path")
    void shouldThrowOnEmptyRootPath() {
      assertThrows(
          JniException.class,
          () -> snapshotHandler.createFullSnapshotAsync("", SnapshotOptions.defaultOptions()),
          "Should throw on empty root path");
    }

    @Test
    @DisplayName("Should throw on null options")
    void shouldThrowOnNullOptions() {
      assertThrows(
          JniException.class,
          () -> snapshotHandler.createFullSnapshotAsync("/tmp", null),
          "Should throw on null options");
    }

    @Test
    @DisplayName("Should return CompletableFuture")
    void shouldReturnCompletableFuture() {
      final CompletableFuture<Long> future =
          snapshotHandler.createFullSnapshotAsync("/tmp", SnapshotOptions.defaultOptions());

      assertNotNull(future, "Future should not be null");

      // Cancel to cleanup
      future.cancel(true);
    }
  }

  @Nested
  @DisplayName("createIncrementalSnapshotAsync Tests")
  class CreateIncrementalSnapshotAsyncTests {

    @Test
    @DisplayName("Should throw on null root path")
    void shouldThrowOnNullRootPath() {
      assertThrows(
          JniException.class,
          () ->
              snapshotHandler.createIncrementalSnapshotAsync(
                  null, 1L, SnapshotOptions.defaultOptions()),
          "Should throw on null root path");
    }

    @Test
    @DisplayName("Should throw on empty root path")
    void shouldThrowOnEmptyRootPath() {
      assertThrows(
          JniException.class,
          () ->
              snapshotHandler.createIncrementalSnapshotAsync(
                  "", 1L, SnapshotOptions.defaultOptions()),
          "Should throw on empty root path");
    }

    @Test
    @DisplayName("Should throw on null options")
    void shouldThrowOnNullOptions() {
      assertThrows(
          JniException.class,
          () -> snapshotHandler.createIncrementalSnapshotAsync("/tmp", 1L, null),
          "Should throw on null options");
    }

    @Test
    @DisplayName("Should throw on non-existent base snapshot")
    void shouldThrowOnNonExistentBaseSnapshot() {
      assertThrows(
          WasiException.class,
          () ->
              snapshotHandler.createIncrementalSnapshotAsync(
                  "/tmp", 999L, SnapshotOptions.defaultOptions()),
          "Should throw on non-existent base snapshot");
    }
  }

  @Nested
  @DisplayName("createDifferentialSnapshotAsync Tests")
  class CreateDifferentialSnapshotAsyncTests {

    @Test
    @DisplayName("Should throw on null root path")
    void shouldThrowOnNullRootPath() {
      assertThrows(
          JniException.class,
          () ->
              snapshotHandler.createDifferentialSnapshotAsync(
                  null, SnapshotOptions.defaultOptions()),
          "Should throw on null root path");
    }

    @Test
    @DisplayName("Should throw on empty root path")
    void shouldThrowOnEmptyRootPath() {
      assertThrows(
          JniException.class,
          () ->
              snapshotHandler.createDifferentialSnapshotAsync("", SnapshotOptions.defaultOptions()),
          "Should throw on empty root path");
    }

    @Test
    @DisplayName("Should throw on null options")
    void shouldThrowOnNullOptions() {
      assertThrows(
          JniException.class,
          () -> snapshotHandler.createDifferentialSnapshotAsync("/tmp", null),
          "Should throw on null options");
    }

    @Test
    @DisplayName("Should throw when no full snapshot exists")
    void shouldThrowWhenNoFullSnapshotExists() {
      assertThrows(
          WasiException.class,
          () ->
              snapshotHandler.createDifferentialSnapshotAsync(
                  "/tmp", SnapshotOptions.defaultOptions()),
          "Should throw when no full snapshot exists");
    }
  }

  @Nested
  @DisplayName("restoreFromSnapshotAsync Tests")
  class RestoreFromSnapshotAsyncTests {

    @Test
    @DisplayName("Should throw on null target path")
    void shouldThrowOnNullTargetPath() {
      assertThrows(
          JniException.class,
          () -> snapshotHandler.restoreFromSnapshotAsync(1L, null, RestoreOptions.defaultOptions()),
          "Should throw on null target path");
    }

    @Test
    @DisplayName("Should throw on empty target path")
    void shouldThrowOnEmptyTargetPath() {
      assertThrows(
          JniException.class,
          () -> snapshotHandler.restoreFromSnapshotAsync(1L, "", RestoreOptions.defaultOptions()),
          "Should throw on empty target path");
    }

    @Test
    @DisplayName("Should throw on null options")
    void shouldThrowOnNullOptions() {
      assertThrows(
          JniException.class,
          () -> snapshotHandler.restoreFromSnapshotAsync(1L, "/tmp", null),
          "Should throw on null options");
    }

    @Test
    @DisplayName("Should throw on non-existent snapshot")
    void shouldThrowOnNonExistentSnapshot() {
      assertThrows(
          WasiException.class,
          () ->
              snapshotHandler.restoreFromSnapshotAsync(
                  999L, "/tmp", RestoreOptions.defaultOptions()),
          "Should throw on non-existent snapshot");
    }
  }

  @Nested
  @DisplayName("verifySnapshotAsync Tests")
  class VerifySnapshotAsyncTests {

    @Test
    @DisplayName("Should throw on non-existent snapshot")
    void shouldThrowOnNonExistentSnapshot() {
      assertThrows(
          WasiException.class,
          () -> snapshotHandler.verifySnapshotAsync(999L),
          "Should throw on non-existent snapshot");
    }
  }

  @Nested
  @DisplayName("listSnapshots Tests")
  class ListSnapshotsTests {

    @Test
    @DisplayName("Should return empty list initially")
    void shouldReturnEmptyListInitially() {
      final List<SnapshotInfo> snapshots = snapshotHandler.listSnapshots();

      assertNotNull(snapshots, "Snapshots list should not be null");
      assertTrue(snapshots.isEmpty(), "Snapshots list should be empty initially");
    }
  }

  @Nested
  @DisplayName("getSnapshotMetadata Tests")
  class GetSnapshotMetadataTests {

    @Test
    @DisplayName("Should throw on non-existent snapshot")
    void shouldThrowOnNonExistentSnapshot() {
      assertThrows(
          WasiException.class,
          () -> snapshotHandler.getSnapshotMetadata(999L),
          "Should throw on non-existent snapshot");
    }
  }

  @Nested
  @DisplayName("deleteSnapshot Tests")
  class DeleteSnapshotTests {

    @Test
    @DisplayName("Should throw on non-existent snapshot")
    void shouldThrowOnNonExistentSnapshot() {
      assertThrows(
          WasiException.class,
          () -> snapshotHandler.deleteSnapshot(999L),
          "Should throw on non-existent snapshot");
    }
  }

  @Nested
  @DisplayName("close Tests")
  class CloseTests {

    @Test
    @DisplayName("Should close gracefully")
    void shouldCloseGracefully() {
      snapshotHandler.close();
      snapshotHandler = null; // Prevent double close in tearDown

      assertTrue(true, "Close should complete without error");
    }

    @Test
    @DisplayName("Should be idempotent")
    void shouldBeIdempotent() {
      snapshotHandler.close();
      snapshotHandler.close();
      snapshotHandler = null; // Prevent double close in tearDown

      assertTrue(true, "Multiple closes should not throw");
    }
  }

  @Nested
  @DisplayName("SnapshotType Tests")
  class SnapshotTypeTests {

    @Test
    @DisplayName("Should have all snapshot types")
    void shouldHaveAllSnapshotTypes() {
      assertEquals(3, SnapshotType.values().length, "Should have 3 snapshot types");
      assertNotNull(SnapshotType.FULL, "FULL should exist");
      assertNotNull(SnapshotType.INCREMENTAL, "INCREMENTAL should exist");
      assertNotNull(SnapshotType.DIFFERENTIAL, "DIFFERENTIAL should exist");
    }
  }

  @Nested
  @DisplayName("SnapshotOptions Tests")
  class SnapshotOptionsTests {

    @Test
    @DisplayName("Default options should have expected values")
    void defaultOptionsShouldHaveExpectedValues() {
      final SnapshotOptions options = SnapshotOptions.defaultOptions();

      assertFalse(options.includeHiddenFiles, "Default should not include hidden files");
      assertEquals(6, options.compressionLevel, "Default compression level should be 6");
      assertFalse(options.encryptionEnabled, "Default should not enable encryption");
    }

    @Test
    @DisplayName("Should clamp compression level")
    void shouldClampCompressionLevel() {
      final SnapshotOptions lowOptions = new SnapshotOptions(false, -5, false, null);
      assertEquals(0, lowOptions.compressionLevel, "Should clamp to 0");

      final SnapshotOptions highOptions = new SnapshotOptions(false, 15, false, null);
      assertEquals(9, highOptions.compressionLevel, "Should clamp to 9");
    }

    @Test
    @DisplayName("Should handle encryption key")
    void shouldHandleEncryptionKey() {
      final byte[] key = new byte[] {1, 2, 3, 4};
      final SnapshotOptions options = new SnapshotOptions(false, 6, true, key);

      assertTrue(options.encryptionEnabled, "Encryption should be enabled");
      assertNotNull(options.encryptionKey, "Encryption key should not be null");
      assertEquals(4, options.encryptionKey.length, "Encryption key length should match");
    }

    @Test
    @DisplayName("Should handle null encryption key")
    void shouldHandleNullEncryptionKey() {
      final SnapshotOptions options = new SnapshotOptions(false, 6, false, null);

      assertFalse(options.encryptionEnabled, "Encryption should be disabled");
    }
  }

  @Nested
  @DisplayName("RestoreOptions Tests")
  class RestoreOptionsTests {

    @Test
    @DisplayName("Default options should have expected values")
    void defaultOptionsShouldHaveExpectedValues() {
      final RestoreOptions options = RestoreOptions.defaultOptions();

      assertTrue(options.overwriteExisting, "Default should overwrite existing");
      assertTrue(options.preservePermissions, "Default should preserve permissions");
      assertTrue(options.preserveTimestamps, "Default should preserve timestamps");
      assertTrue(options.verifyIntegrity, "Default should verify integrity");
    }

    @Test
    @DisplayName("Should create with custom values")
    void shouldCreateWithCustomValues() {
      final RestoreOptions options = new RestoreOptions(false, false, false, false);

      assertFalse(options.overwriteExisting, "Should not overwrite");
      assertFalse(options.preservePermissions, "Should not preserve permissions");
      assertFalse(options.preserveTimestamps, "Should not preserve timestamps");
      assertFalse(options.verifyIntegrity, "Should not verify integrity");
    }
  }

  @Nested
  @DisplayName("SnapshotInfo Tests")
  class SnapshotInfoTests {

    @Test
    @DisplayName("Should create with all fields")
    void shouldCreateWithAllFields() {
      final SnapshotMetadata metadata =
          new SnapshotMetadata(1L, "/tmp", SnapshotType.FULL, Instant.now(), false, 6, false, null);

      final SnapshotInfo info = new SnapshotInfo(1L, "/tmp", SnapshotType.FULL, 1024L, metadata);

      assertEquals(1L, info.handle, "Handle should match");
      assertEquals("/tmp", info.rootPath, "Root path should match");
      assertEquals(SnapshotType.FULL, info.type, "Type should match");
      assertEquals(1024L, info.snapshotSize, "Size should match");
      assertNotNull(info.metadata, "Metadata should not be null");
      assertTrue(info.createdAt > 0, "Created timestamp should be positive");
    }
  }

  @Nested
  @DisplayName("SnapshotMetadata Tests")
  class SnapshotMetadataTests {

    @Test
    @DisplayName("Should create with all fields")
    void shouldCreateWithAllFields() {
      final Instant now = Instant.now();
      final SnapshotMetadata metadata =
          new SnapshotMetadata(1L, "/tmp", SnapshotType.INCREMENTAL, now, true, 9, true, 0L);

      assertEquals(1L, metadata.handle, "Handle should match");
      assertEquals("/tmp", metadata.rootPath, "Root path should match");
      assertEquals(SnapshotType.INCREMENTAL, metadata.type, "Type should match");
      assertEquals(now, metadata.createdAt, "Created time should match");
      assertTrue(metadata.includeHiddenFiles, "Include hidden should be true");
      assertEquals(9, metadata.compressionLevel, "Compression level should match");
      assertTrue(metadata.encryptionEnabled, "Encryption should be enabled");
      assertEquals(0L, metadata.baseSnapshotHandle, "Base snapshot handle should match");
    }
  }

  @Nested
  @DisplayName("SnapshotVerificationResult Tests")
  class SnapshotVerificationResultTests {

    @Test
    @DisplayName("Should create with all fields")
    void shouldCreateWithAllFields() {
      final SnapshotVerificationResult result = new SnapshotVerificationResult(true, 100, 2, 3, 1);

      assertTrue(result.isValid, "Should be valid");
      assertEquals(100, result.checkedFiles, "Checked files should match");
      assertEquals(2, result.corruptedFiles, "Corrupted files should match");
      assertEquals(3, result.missingFiles, "Missing files should match");
      assertEquals(1, result.checksumMismatch, "Checksum mismatch should match");
    }

    @Test
    @DisplayName("Should create for invalid snapshot")
    void shouldCreateForInvalidSnapshot() {
      final SnapshotVerificationResult result = new SnapshotVerificationResult(false, 50, 10, 5, 3);

      assertFalse(result.isValid, "Should be invalid");
      assertEquals(50, result.checkedFiles, "Checked files should match");
    }
  }

  @Nested
  @DisplayName("SnapshotPerformanceMetrics Tests")
  class SnapshotPerformanceMetricsTests {

    @Test
    @DisplayName("Should create with default values")
    void shouldCreateWithDefaultValues() {
      final SnapshotPerformanceMetrics metrics = new SnapshotPerformanceMetrics();

      assertEquals(0, metrics.snapshotCreationTimeMs, "Creation time should be 0");
      assertEquals(0, metrics.snapshotRestoreTimeMs, "Restore time should be 0");
      assertEquals(0, metrics.totalBytesProcessed, "Bytes processed should be 0");
      assertEquals(0, metrics.filesProcessed, "Files processed should be 0");
    }
  }

  @Nested
  @DisplayName("DeduplicationStatistics Tests")
  class DeduplicationStatisticsTests {

    @Test
    @DisplayName("Should create with default values")
    void shouldCreateWithDefaultValues() {
      final DeduplicationStatistics stats = new DeduplicationStatistics();

      assertEquals(0, stats.duplicateBlocksFound, "Duplicate blocks should be 0");
      assertEquals(
          0, stats.bytesDeduplicatedCompressionStatistics, "Bytes deduplicated should be 0");
      assertEquals(0.0, stats.deduplicationRatio, 0.001, "Deduplication ratio should be 0");
    }
  }

  @Nested
  @DisplayName("CompressionStatistics Tests")
  class CompressionStatisticsTests {

    @Test
    @DisplayName("Should create with default values")
    void shouldCreateWithDefaultValues() {
      final CompressionStatistics stats = new CompressionStatistics();

      assertEquals(0, stats.originalSize, "Original size should be 0");
      assertEquals(0, stats.compressedSize, "Compressed size should be 0");
      assertEquals(0.0, stats.compressionRatio, 0.001, "Compression ratio should be 0");
      assertEquals("none", stats.compressionAlgorithm, "Algorithm should be 'none'");
    }
  }
}
