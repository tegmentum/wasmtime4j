package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama implementation of experimental WASI filesystem snapshot operations.
 *
 * <p>This class provides experimental filesystem snapshot capabilities as defined in WASI Preview
 * 2, including filesystem state capture, restoration, and incremental snapshots. These features
 * allow applications to create point-in-time snapshots of filesystem state and restore from them.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Full filesystem state capture and restoration
 *   <li>Incremental snapshot support with efficient delta tracking
 *   <li>Cross-platform snapshot compatibility with metadata preservation
 *   <li>Snapshot versioning and metadata management
 *   <li>Async snapshot operations for large filesystem states
 *   <li>Snapshot verification and integrity checking
 * </ul>
 *
 * <p>This is an experimental feature and may change in future WASI releases.
 *
 * @since 1.0.0
 */
public final class WasiFilesystemSnapshot {

  private static final Logger LOGGER = Logger.getLogger(WasiFilesystemSnapshot.class.getName());

  /** Maximum snapshot size in bytes (1GB). */
  private static final long MAX_SNAPSHOT_SIZE = 1024L * 1024L * 1024L;

  /** Maximum number of snapshots that can be active simultaneously. */
  private static final int MAX_ACTIVE_SNAPSHOTS = 100;

  /** The WASI context this snapshot handler belongs to. */
  private final WasiContext wasiContext;

  /** Executor service for async snapshot operations. */
  private final ExecutorService asyncExecutor;

  /** Memory arena for native operations. */
  private final Arena arena;

  /** Snapshot handle generator. */
  private final AtomicLong snapshotHandleGenerator = new AtomicLong(1);

  /** Active snapshots tracking. */
  private final Map<Long, SnapshotInfo> activeSnapshots = new ConcurrentHashMap<>();

  /** Snapshot metadata cache. */
  private final Map<Long, SnapshotMetadata> snapshotMetadata = new ConcurrentHashMap<>();

  /**
   * Creates a new WASI filesystem snapshot handler.
   *
   * @param wasiContext the WASI context to operate within
   * @param asyncExecutor the executor service for async operations
   * @throws PanamaException if parameters are null
   */
  public WasiFilesystemSnapshot(
      final WasiContext wasiContext, final ExecutorService asyncExecutor) {
    PanamaValidation.requireNonNull(wasiContext, "wasiContext");
    PanamaValidation.requireNonNull(asyncExecutor, "asyncExecutor");

    this.wasiContext = wasiContext;
    this.asyncExecutor = asyncExecutor;
    this.arena = Arena.ofShared();

    LOGGER.info("Created WASI filesystem snapshot handler (Panama)");
  }

  /**
   * Creates a full filesystem snapshot.
   *
   * <p>Captures the complete state of the filesystem at a point in time, including file contents,
   * metadata, permissions, and directory structure.
   *
   * @param rootPath the root path to snapshot
   * @param options the snapshot options
   * @return CompletableFuture that resolves to the snapshot handle
   * @throws WasiException if snapshot creation fails
   */
  public CompletableFuture<Long> createFullSnapshotAsync(
      final String rootPath, final SnapshotOptions options) {
    PanamaValidation.requireNonEmpty(rootPath, "rootPath");
    PanamaValidation.requireNonNull(options, "options");

    if (activeSnapshots.size() >= MAX_ACTIVE_SNAPSHOTS) {
      throw new WasiException("Maximum number of active snapshots exceeded", WasiErrorCode.ENOMEM);
    }

    LOGGER.info(() -> String.format("Creating full snapshot: rootPath=%s", rootPath));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long snapshotHandle = snapshotHandleGenerator.getAndIncrement();

            // Create snapshot metadata
            final SnapshotMetadata metadata =
                new SnapshotMetadata(
                    snapshotHandle,
                    rootPath,
                    SnapshotType.FULL,
                    Instant.now(),
                    options.includeHiddenFiles,
                    options.compressionLevel,
                    options.encryptionEnabled,
                    null);

            snapshotMetadata.put(snapshotHandle, metadata);

            // Create native snapshot using Panama FFI
            try (final Arena callArena = Arena.ofConfined()) {
              final MemorySegment rootPathSegment = callArena.allocateUtf8String(rootPath);
              final MemorySegment encryptionKeySegment =
                  options.encryptionKey != null
                      ? callArena.allocateArray(ValueLayout.JAVA_BYTE, options.encryptionKey)
                      : MemorySegment.NULL;

              final SnapshotCreateResult result =
                  nativeCreateSnapshot(
                      wasiContext.getNativeHandle(),
                      snapshotHandle,
                      rootPathSegment,
                      true, // full snapshot
                      0L, // no base snapshot for full
                      options.includeHiddenFiles,
                      options.compressionLevel,
                      options.encryptionEnabled,
                      encryptionKeySegment);

              if (result.errorCode != 0) {
                snapshotMetadata.remove(snapshotHandle);
                final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
                throw new WasiException(
                    "Failed to create filesystem snapshot: "
                        + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                    errorCode != null ? errorCode : WasiErrorCode.EIO);
              }

              // Track active snapshot
              final SnapshotInfo snapshotInfo =
                  new SnapshotInfo(
                      snapshotHandle, rootPath, SnapshotType.FULL, result.snapshotSize, metadata);
              activeSnapshots.put(snapshotHandle, snapshotInfo);

              // Update metadata with actual size
              metadata.snapshotSize = result.snapshotSize;
              metadata.fileCount = result.fileCount;

              LOGGER.info(
                  () ->
                      String.format(
                          "Created full snapshot: handle=%d, size=%d bytes, files=%d",
                          snapshotHandle, result.snapshotSize, result.fileCount));

              return snapshotHandle;
            }

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create full snapshot", e);
            throw new RuntimeException("Snapshot creation failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Creates an incremental snapshot based on a previous snapshot.
   *
   * <p>Captures only the changes since the base snapshot, providing efficient storage and faster
   * snapshot creation for large filesystems.
   *
   * @param rootPath the root path to snapshot
   * @param baseSnapshotHandle the base snapshot to compare against
   * @param options the snapshot options
   * @return CompletableFuture that resolves to the incremental snapshot handle
   * @throws WasiException if incremental snapshot creation fails
   */
  public CompletableFuture<Long> createIncrementalSnapshotAsync(
      final String rootPath, final long baseSnapshotHandle, final SnapshotOptions options) {
    PanamaValidation.requireNonEmpty(rootPath, "rootPath");
    PanamaValidation.requireNonNull(options, "options");

    final SnapshotInfo baseSnapshot = activeSnapshots.get(baseSnapshotHandle);
    if (baseSnapshot == null) {
      throw new WasiException(
          "Base snapshot not found: " + baseSnapshotHandle, WasiErrorCode.ENOENT);
    }

    if (activeSnapshots.size() >= MAX_ACTIVE_SNAPSHOTS) {
      throw new WasiException("Maximum number of active snapshots exceeded", WasiErrorCode.ENOMEM);
    }

    LOGGER.info(
        () ->
            String.format(
                "Creating incremental snapshot: rootPath=%s, baseSnapshot=%d",
                rootPath, baseSnapshotHandle));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long snapshotHandle = snapshotHandleGenerator.getAndIncrement();

            // Create snapshot metadata
            final SnapshotMetadata metadata =
                new SnapshotMetadata(
                    snapshotHandle,
                    rootPath,
                    SnapshotType.INCREMENTAL,
                    Instant.now(),
                    options.includeHiddenFiles,
                    options.compressionLevel,
                    options.encryptionEnabled,
                    baseSnapshotHandle);

            snapshotMetadata.put(snapshotHandle, metadata);

            // Create native incremental snapshot using Panama FFI
            try (final Arena callArena = Arena.ofConfined()) {
              final MemorySegment rootPathSegment = callArena.allocateUtf8String(rootPath);
              final MemorySegment encryptionKeySegment =
                  options.encryptionKey != null
                      ? callArena.allocateArray(ValueLayout.JAVA_BYTE, options.encryptionKey)
                      : MemorySegment.NULL;

              final SnapshotCreateResult result =
                  nativeCreateSnapshot(
                      wasiContext.getNativeHandle(),
                      snapshotHandle,
                      rootPathSegment,
                      false, // incremental snapshot
                      baseSnapshotHandle,
                      options.includeHiddenFiles,
                      options.compressionLevel,
                      options.encryptionEnabled,
                      encryptionKeySegment);

              if (result.errorCode != 0) {
                snapshotMetadata.remove(snapshotHandle);
                final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
                throw new WasiException(
                    "Failed to create incremental snapshot: "
                        + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                    errorCode != null ? errorCode : WasiErrorCode.EIO);
              }

              // Track active snapshot
              final SnapshotInfo snapshotInfo =
                  new SnapshotInfo(
                      snapshotHandle,
                      rootPath,
                      SnapshotType.INCREMENTAL,
                      result.snapshotSize,
                      metadata);
              activeSnapshots.put(snapshotHandle, snapshotInfo);

              // Update metadata with actual size
              metadata.snapshotSize = result.snapshotSize;
              metadata.fileCount = result.fileCount;

              LOGGER.info(
                  () ->
                      String.format(
                          "Created incremental snapshot: handle=%d, size=%d bytes, files=%d",
                          snapshotHandle, result.snapshotSize, result.fileCount));

              return snapshotHandle;
            }

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create incremental snapshot", e);
            throw new RuntimeException(
                "Incremental snapshot creation failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Restores a filesystem from a snapshot.
   *
   * <p>Restores the filesystem state to match the specified snapshot, including file contents,
   * metadata, and directory structure.
   *
   * @param snapshotHandle the snapshot to restore from
   * @param targetPath the target path to restore to
   * @param options the restore options
   * @return CompletableFuture that completes when restore is finished
   * @throws WasiException if restore operation fails
   */
  public CompletableFuture<Void> restoreFromSnapshotAsync(
      final long snapshotHandle, final String targetPath, final RestoreOptions options) {
    PanamaValidation.requireNonEmpty(targetPath, "targetPath");
    PanamaValidation.requireNonNull(options, "options");

    final SnapshotInfo snapshot = activeSnapshots.get(snapshotHandle);
    if (snapshot == null) {
      throw new WasiException("Snapshot not found: " + snapshotHandle, WasiErrorCode.ENOENT);
    }

    LOGGER.info(
        () ->
            String.format(
                "Restoring from snapshot: handle=%d, targetPath=%s", snapshotHandle, targetPath));

    return CompletableFuture.runAsync(
        () -> {
          try {
            try (final Arena callArena = Arena.ofConfined()) {
              final MemorySegment targetPathSegment = callArena.allocateUtf8String(targetPath);

              final int result =
                  nativeRestoreSnapshot(
                      wasiContext.getNativeHandle(),
                      snapshotHandle,
                      targetPathSegment,
                      options.overwriteExisting,
                      options.preservePermissions,
                      options.preserveTimestamps,
                      options.verifyIntegrity);

              if (result != 0) {
                final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
                throw new WasiException(
                    "Failed to restore from snapshot: "
                        + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                    errorCode != null ? errorCode : WasiErrorCode.EIO);
              }

              LOGGER.info(() -> String.format("Restored from snapshot: handle=%d", snapshotHandle));
            }

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to restore from snapshot", e);
            throw new RuntimeException("Snapshot restore failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Verifies the integrity of a snapshot.
   *
   * @param snapshotHandle the snapshot to verify
   * @return CompletableFuture that resolves to the verification result
   * @throws WasiException if verification fails
   */
  public CompletableFuture<SnapshotVerificationResult> verifySnapshotAsync(
      final long snapshotHandle) {
    final SnapshotInfo snapshot = activeSnapshots.get(snapshotHandle);
    if (snapshot == null) {
      throw new WasiException("Snapshot not found: " + snapshotHandle, WasiErrorCode.ENOENT);
    }

    LOGGER.fine(() -> String.format("Verifying snapshot: handle=%d", snapshotHandle));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final SnapshotVerifyResult result =
                nativeVerifySnapshot(wasiContext.getNativeHandle(), snapshotHandle);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Snapshot verification failed: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            final SnapshotVerificationResult verificationResult =
                new SnapshotVerificationResult(
                    result.isValid,
                    result.checkedFiles,
                    result.corruptedFiles,
                    result.missingFiles,
                    result.checksumMismatch);

            LOGGER.fine(
                () ->
                    String.format(
                        "Snapshot verification completed: handle=%d, valid=%b, checked=%d",
                        snapshotHandle, result.isValid, result.checkedFiles));

            return verificationResult;

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Snapshot verification failed", e);
            throw new RuntimeException("Snapshot verification failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Lists all available snapshots.
   *
   * @return list of snapshot information
   */
  public List<SnapshotInfo> listSnapshots() {
    return List.copyOf(activeSnapshots.values());
  }

  /**
   * Gets metadata for a specific snapshot.
   *
   * @param snapshotHandle the snapshot handle
   * @return the snapshot metadata
   * @throws WasiException if snapshot not found
   */
  public SnapshotMetadata getSnapshotMetadata(final long snapshotHandle) {
    final SnapshotMetadata metadata = snapshotMetadata.get(snapshotHandle);
    if (metadata == null) {
      throw new WasiException("Snapshot not found: " + snapshotHandle, WasiErrorCode.ENOENT);
    }
    return metadata;
  }

  /**
   * Deletes a snapshot and frees its resources.
   *
   * @param snapshotHandle the snapshot to delete
   * @throws WasiException if deletion fails
   */
  public void deleteSnapshot(final long snapshotHandle) {
    final SnapshotInfo snapshot = activeSnapshots.get(snapshotHandle);
    if (snapshot == null) {
      throw new WasiException("Snapshot not found: " + snapshotHandle, WasiErrorCode.ENOENT);
    }

    LOGGER.info(() -> String.format("Deleting snapshot: handle=%d", snapshotHandle));

    try {
      final int result = nativeDeleteSnapshot(wasiContext.getNativeHandle(), snapshotHandle);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Failed to delete snapshot: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      activeSnapshots.remove(snapshotHandle);
      snapshotMetadata.remove(snapshotHandle);

      LOGGER.info(() -> String.format("Deleted snapshot: handle=%d", snapshotHandle));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to delete snapshot: " + snapshotHandle, e);
      throw new WasiException("Snapshot deletion failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /** Closes the filesystem snapshot handler and cleans up resources. */
  public void close() {
    LOGGER.info("Closing filesystem snapshot handler");

    // Delete all active snapshots
    for (final Long snapshotHandle : activeSnapshots.keySet()) {
      try {
        deleteSnapshot(snapshotHandle);
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Error deleting snapshot during shutdown: " + snapshotHandle, e);
      }
    }

    activeSnapshots.clear();
    snapshotMetadata.clear();

    // Close the arena
    arena.close();

    LOGGER.info("Filesystem snapshot handler closed successfully");
  }

  // Native method declarations using Panama FFI
  private native SnapshotCreateResult nativeCreateSnapshot(
      long contextHandle,
      long snapshotHandle,
      MemorySegment rootPath,
      boolean fullSnapshot,
      long baseSnapshotHandle,
      boolean includeHidden,
      int compressionLevel,
      boolean encryptionEnabled,
      MemorySegment encryptionKey);

  private native int nativeRestoreSnapshot(
      long contextHandle,
      long snapshotHandle,
      MemorySegment targetPath,
      boolean overwriteExisting,
      boolean preservePermissions,
      boolean preserveTimestamps,
      boolean verifyIntegrity);

  private native SnapshotVerifyResult nativeVerifySnapshot(long contextHandle, long snapshotHandle);

  private native int nativeDeleteSnapshot(long contextHandle, long snapshotHandle);

  /** Snapshot type enumeration. */
  public enum SnapshotType {
    FULL,
    INCREMENTAL
  }

  /** Snapshot options for creation. */
  public static final class SnapshotOptions {
    public final boolean includeHiddenFiles;
    public final int compressionLevel; // 0-9, 0 = no compression, 9 = maximum
    public final boolean encryptionEnabled;
    public final byte[] encryptionKey;

    public SnapshotOptions(
        final boolean includeHiddenFiles,
        final int compressionLevel,
        final boolean encryptionEnabled,
        final byte[] encryptionKey) {
      this.includeHiddenFiles = includeHiddenFiles;
      this.compressionLevel = Math.max(0, Math.min(9, compressionLevel));
      this.encryptionEnabled = encryptionEnabled;
      this.encryptionKey = encryptionKey != null ? encryptionKey.clone() : null;
    }

    public static SnapshotOptions defaultOptions() {
      return new SnapshotOptions(false, 6, false, null);
    }
  }

  /** Restore options for snapshot restoration. */
  public static final class RestoreOptions {
    public final boolean overwriteExisting;
    public final boolean preservePermissions;
    public final boolean preserveTimestamps;
    public final boolean verifyIntegrity;

    public RestoreOptions(
        final boolean overwriteExisting,
        final boolean preservePermissions,
        final boolean preserveTimestamps,
        final boolean verifyIntegrity) {
      this.overwriteExisting = overwriteExisting;
      this.preservePermissions = preservePermissions;
      this.preserveTimestamps = preserveTimestamps;
      this.verifyIntegrity = verifyIntegrity;
    }

    public static RestoreOptions defaultOptions() {
      return new RestoreOptions(true, true, true, true);
    }
  }

  /** Snapshot information class. */
  public static final class SnapshotInfo {
    public final long handle;
    public final String rootPath;
    public final SnapshotType type;
    public final long snapshotSize;
    public final SnapshotMetadata metadata;
    public final long createdAt;

    public SnapshotInfo(
        final long handle,
        final String rootPath,
        final SnapshotType type,
        final long snapshotSize,
        final SnapshotMetadata metadata) {
      this.handle = handle;
      this.rootPath = rootPath;
      this.type = type;
      this.snapshotSize = snapshotSize;
      this.metadata = metadata;
      this.createdAt = System.currentTimeMillis();
    }
  }

  /** Snapshot metadata class. */
  public static final class SnapshotMetadata {
    public final long handle;
    public final String rootPath;
    public final SnapshotType type;
    public final Instant createdAt;
    public final boolean includeHiddenFiles;
    public final int compressionLevel;
    public final boolean encryptionEnabled;
    public final Long baseSnapshotHandle;
    public volatile long snapshotSize;
    public volatile int fileCount;

    public SnapshotMetadata(
        final long handle,
        final String rootPath,
        final SnapshotType type,
        final Instant createdAt,
        final boolean includeHiddenFiles,
        final int compressionLevel,
        final boolean encryptionEnabled,
        final Long baseSnapshotHandle) {
      this.handle = handle;
      this.rootPath = rootPath;
      this.type = type;
      this.createdAt = createdAt;
      this.includeHiddenFiles = includeHiddenFiles;
      this.compressionLevel = compressionLevel;
      this.encryptionEnabled = encryptionEnabled;
      this.baseSnapshotHandle = baseSnapshotHandle;
    }
  }

  /** Snapshot verification result. */
  public static final class SnapshotVerificationResult {
    public final boolean isValid;
    public final int checkedFiles;
    public final int corruptedFiles;
    public final int missingFiles;
    public final int checksumMismatch;

    public SnapshotVerificationResult(
        final boolean isValid,
        final int checkedFiles,
        final int corruptedFiles,
        final int missingFiles,
        final int checksumMismatch) {
      this.isValid = isValid;
      this.checkedFiles = checkedFiles;
      this.corruptedFiles = corruptedFiles;
      this.missingFiles = missingFiles;
      this.checksumMismatch = checksumMismatch;
    }
  }

  /** Native snapshot creation result. */
  private static final class SnapshotCreateResult {
    public final int errorCode;
    public final long snapshotSize;
    public final int fileCount;

    public SnapshotCreateResult(final int errorCode, final long snapshotSize, final int fileCount) {
      this.errorCode = errorCode;
      this.snapshotSize = snapshotSize;
      this.fileCount = fileCount;
    }
  }

  /** Native snapshot verification result. */
  private static final class SnapshotVerifyResult {
    public final int errorCode;
    public final boolean isValid;
    public final int checkedFiles;
    public final int corruptedFiles;
    public final int missingFiles;
    public final int checksumMismatch;

    public SnapshotVerifyResult(
        final int errorCode,
        final boolean isValid,
        final int checkedFiles,
        final int corruptedFiles,
        final int missingFiles,
        final int checksumMismatch) {
      this.errorCode = errorCode;
      this.isValid = isValid;
      this.checkedFiles = checkedFiles;
      this.corruptedFiles = corruptedFiles;
      this.missingFiles = missingFiles;
      this.checksumMismatch = checksumMismatch;
    }
  }

  /** Additional classes for advanced snapshot functionality. */

  /** Comprehensive snapshot metrics. */
  public static final class SnapshotMetrics {
    public final long totalSnapshotsCreated;
    public final int activeSnapshots;
    public final long totalOperations;
    public final long successfulOperations;
    public final long failedOperations;
    public final long totalStorageUsed;
    public final long totalOriginalSize;
    public final DeduplicationStatistics dedupStats;
    public final CompressionStatistics compressionStats;
    public final SnapshotPerformanceMetrics performanceMetrics;

    public SnapshotMetrics(
        final long totalSnapshotsCreated,
        final int activeSnapshots,
        final long totalOperations,
        final long successfulOperations,
        final long failedOperations,
        final long totalStorageUsed,
        final long totalOriginalSize,
        final DeduplicationStatistics dedupStats,
        final CompressionStatistics compressionStats,
        final SnapshotPerformanceMetrics performanceMetrics) {
      this.totalSnapshotsCreated = totalSnapshotsCreated;
      this.activeSnapshots = activeSnapshots;
      this.totalOperations = totalOperations;
      this.successfulOperations = successfulOperations;
      this.failedOperations = failedOperations;
      this.totalStorageUsed = totalStorageUsed;
      this.totalOriginalSize = totalOriginalSize;
      this.dedupStats = dedupStats;
      this.compressionStats = compressionStats;
      this.performanceMetrics = performanceMetrics;
    }
  }

  /** Performance metrics. */
  public static final class SnapshotPerformanceMetrics {
    public final double avgSnapshotCreationTimeMs;
    public final double avgRestoreTimeMs;
    public final double avgValidationTimeMs;
    public final double throughputBytesPerSec;
    public final double operationsPerSec;

    public SnapshotPerformanceMetrics(
        final double avgSnapshotCreationTimeMs,
        final double avgRestoreTimeMs,
        final double avgValidationTimeMs,
        final double throughputBytesPerSec,
        final double operationsPerSec) {
      this.avgSnapshotCreationTimeMs = avgSnapshotCreationTimeMs;
      this.avgRestoreTimeMs = avgRestoreTimeMs;
      this.avgValidationTimeMs = avgValidationTimeMs;
      this.throughputBytesPerSec = throughputBytesPerSec;
      this.operationsPerSec = operationsPerSec;
    }
  }

  /** Deduplication statistics. */
  public static final class DeduplicationStatistics {
    public volatile long totalBlocks;
    public volatile long uniqueBlocks;
    public volatile long spaceSaved;
    public volatile double deduplicationRatio;

    public DeduplicationStatistics() {
      this.totalBlocks = 0;
      this.uniqueBlocks = 0;
      this.spaceSaved = 0;
      this.deduplicationRatio = 0.0;
    }
  }

  /** Compression statistics. */
  public static final class CompressionStatistics {
    public volatile long compressedFiles;
    public volatile long originalSize;
    public volatile long compressedSize;
    public volatile double avgCompressionRatio;

    public CompressionStatistics() {
      this.compressedFiles = 0;
      this.originalSize = 0;
      this.compressedSize = 0;
      this.avgCompressionRatio = 0.0;
    }
  }

  /** Snapshot filter for advanced listing. */
  public interface SnapshotFilter {
    boolean matches(SnapshotInfo snapshot);

    static SnapshotFilter byType(SnapshotType type) {
      return snapshot -> snapshot.type == type;
    }

    static SnapshotFilter byPath(String rootPath) {
      return snapshot -> snapshot.rootPath.equals(rootPath);
    }

    static SnapshotFilter byAgeNewer(long ageMs) {
      final long cutoff = System.currentTimeMillis() - ageMs;
      return snapshot -> snapshot.createdAt > cutoff;
    }
  }

  /** Storage optimization result. */
  public static final class OptimizationResult {
    public final long blocksRemoved;
    public final long spaceReclaimed;
    public final long optimizationTimeMs;

    public OptimizationResult(
        final long blocksRemoved, final long spaceReclaimed, final long optimizationTimeMs) {
      this.blocksRemoved = blocksRemoved;
      this.spaceReclaimed = spaceReclaimed;
      this.optimizationTimeMs = optimizationTimeMs;
    }
  }

  /** Native metrics result structure. */
  private static final class SnapshotNativeMetrics {
    public final long totalSnapshotsCreated;
    public final long totalOperations;
    public final long successfulOperations;
    public final long failedOperations;
    public final long totalStorageUsed;
    public final long totalOriginalSize;
    public final double avgSnapshotCreationTimeMs;
    public final double avgRestoreTimeMs;
    public final double avgValidationTimeMs;
    public final double throughputBytesPerSec;
    public final double operationsPerSec;

    public SnapshotNativeMetrics(
        final long totalSnapshotsCreated,
        final long totalOperations,
        final long successfulOperations,
        final long failedOperations,
        final long totalStorageUsed,
        final long totalOriginalSize,
        final double avgSnapshotCreationTimeMs,
        final double avgRestoreTimeMs,
        final double avgValidationTimeMs,
        final double throughputBytesPerSec,
        final double operationsPerSec) {
      this.totalSnapshotsCreated = totalSnapshotsCreated;
      this.totalOperations = totalOperations;
      this.successfulOperations = successfulOperations;
      this.failedOperations = failedOperations;
      this.totalStorageUsed = totalStorageUsed;
      this.totalOriginalSize = totalOriginalSize;
      this.avgSnapshotCreationTimeMs = avgSnapshotCreationTimeMs;
      this.avgRestoreTimeMs = avgRestoreTimeMs;
      this.avgValidationTimeMs = avgValidationTimeMs;
      this.throughputBytesPerSec = throughputBytesPerSec;
      this.operationsPerSec = operationsPerSec;
    }
  }

  /** Native optimization result structure. */
  private static final class OptimizationNativeResult {
    public final int errorCode;
    public final long blocksRemoved;
    public final long spaceReclaimed;
    public final long optimizationTimeMs;

    public OptimizationNativeResult(
        final int errorCode,
        final long blocksRemoved,
        final long spaceReclaimed,
        final long optimizationTimeMs) {
      this.errorCode = errorCode;
      this.blocksRemoved = blocksRemoved;
      this.spaceReclaimed = spaceReclaimed;
      this.optimizationTimeMs = optimizationTimeMs;
    }
  }
}
