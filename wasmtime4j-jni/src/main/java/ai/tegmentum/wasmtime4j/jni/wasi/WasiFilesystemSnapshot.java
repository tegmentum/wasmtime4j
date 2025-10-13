package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of experimental WASI filesystem snapshot operations.
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

  /** Maximum snapshot size in bytes (10GB). */
  private static final long MAX_SNAPSHOT_SIZE = 10L * 1024L * 1024L * 1024L;

  /** Maximum number of snapshots that can be active simultaneously. */
  private static final int MAX_ACTIVE_SNAPSHOTS = 100;

  /** Maximum incremental snapshot chain depth. */
  private static final int MAX_INCREMENTAL_CHAIN_DEPTH = 10;

  /** The WASI context this snapshot handler belongs to. */
  private final WasiContext wasiContext;

  /** Executor service for async snapshot operations. */
  private final ExecutorService asyncExecutor;

  /** Snapshot handle generator. */
  private final AtomicLong snapshotHandleGenerator = new AtomicLong(1);

  /** Active snapshots tracking. */
  private final Map<Long, SnapshotInfo> activeSnapshots = new ConcurrentHashMap<>();

  /** Snapshot metadata cache. */
  private final Map<Long, SnapshotMetadata> snapshotMetadata = new ConcurrentHashMap<>();

  /** Snapshot performance metrics. */
  private final Map<Long, SnapshotPerformanceMetrics> performanceMetrics =
      new ConcurrentHashMap<>();

  /** Cleanup scheduler for background maintenance. */
  private final java.util.concurrent.ScheduledExecutorService cleanupScheduler =
      java.util.concurrent.Executors.newScheduledThreadPool(1);

  /** Deduplication statistics. */
  private volatile DeduplicationStatistics dedupStats = new DeduplicationStatistics();

  /** Compression statistics. */
  private volatile CompressionStatistics compressionStats = new CompressionStatistics();

  /**
   * Creates a new WASI filesystem snapshot handler.
   *
   * @param wasiContext the WASI context to operate within
   * @param asyncExecutor the executor service for async operations
   * @throws JniException if parameters are null
   */
  public WasiFilesystemSnapshot(
      final WasiContext wasiContext, final ExecutorService asyncExecutor) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    JniValidation.requireNonNull(asyncExecutor, "asyncExecutor");

    this.wasiContext = wasiContext;
    this.asyncExecutor = asyncExecutor;

    // Initialize native snapshot manager
    // TODO: Implement native snapshot manager initialization
    // final int initResult = nativeInitSnapshotManager();
    // if (initResult != 0) {
    //   throw new JniException("Failed to initialize native snapshot manager");
    // }

    // Schedule periodic cleanup
    cleanupScheduler.scheduleAtFixedRate(
        this::performPeriodicCleanup,
        1, // initial delay (hours)
        1, // period (hours)
        java.util.concurrent.TimeUnit.HOURS);

    LOGGER.info("Created WASI filesystem snapshot handler with advanced features");
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
    JniValidation.requireNonEmpty(rootPath, "rootPath");
    JniValidation.requireNonNull(options, "options");

    if (activeSnapshots.size() >= MAX_ACTIVE_SNAPSHOTS) {
      throw new WasiException("Maximum number of active snapshots exceeded", WasiErrorCode.ENOMEM);
    }

    LOGGER.info(() -> String.format("Creating full snapshot: rootPath=%s", rootPath));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long snapshotHandle = snapshotHandleGenerator.getAndIncrement();

            // Create comprehensive snapshot metadata
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

            // Add advanced metadata fields
            // TODO: Implement advanced metadata fields when CreationContext and SnapshotVersion
            // classes are created
            // metadata.creationContext =
            //     new CreationContext(
            //         System.getProperty("user.name"),
            //         getPid(),
            //         createHostInfo(),
            //         "Programmatic snapshot creation");
            // metadata.version = new SnapshotVersion(1, 0, 0);
            metadata.tags = new ArrayList<>();
            metadata.properties = new HashMap<>();

            snapshotMetadata.put(snapshotHandle, metadata);

            // Create native snapshot with advanced options
            final SnapshotCreateResult result =
                nativeCreateAdvancedSnapshot(
                    wasiContext.getNativeHandle(),
                    snapshotHandle,
                    rootPath,
                    0, // snapshot type: FULL
                    0, // no base snapshot for full
                    options.includeHiddenFiles,
                    options.compressionLevel,
                    options.encryptionEnabled,
                    options.encryptionKey,
                    true, // enable deduplication
                    true, // enable integrity checking
                    "", // name - TODO: add name field to metadata
                    ""); // description - TODO: add description field to metadata

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
    JniValidation.requireNonEmpty(rootPath, "rootPath");
    JniValidation.requireNonNull(options, "options");

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

            // Create native incremental snapshot with advanced options
            final SnapshotCreateResult result =
                nativeCreateAdvancedSnapshot(
                    wasiContext.getNativeHandle(),
                    snapshotHandle,
                    rootPath,
                    1, // snapshot type: INCREMENTAL
                    baseSnapshotHandle,
                    options.includeHiddenFiles,
                    options.compressionLevel,
                    options.encryptionEnabled,
                    options.encryptionKey,
                    true, // enable deduplication
                    true, // enable integrity checking
                    "", // name - TODO: add name field to metadata
                    ""); // description - TODO: add description field to metadata

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
    JniValidation.requireNonEmpty(targetPath, "targetPath");
    JniValidation.requireNonNull(options, "options");

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
            final int result =
                nativeRestoreSnapshot(
                    wasiContext.getNativeHandle(),
                    snapshotHandle,
                    targetPath,
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
    return new ArrayList<>(activeSnapshots.values());
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

    // Shutdown cleanup scheduler
    cleanupScheduler.shutdown();
    try {
      if (!cleanupScheduler.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
        cleanupScheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      cleanupScheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }

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
    performanceMetrics.clear();

    // Cleanup native resources
    nativeCleanupSnapshotManager();

    LOGGER.info("Filesystem snapshot handler closed successfully");
  }

  /** Creates a differential snapshot (changes since last full snapshot). */
  public CompletableFuture<Long> createDifferentialSnapshotAsync(
      final String rootPath, final SnapshotOptions options) {
    JniValidation.requireNonEmpty(rootPath, "rootPath");
    JniValidation.requireNonNull(options, "options");

    if (activeSnapshots.size() >= MAX_ACTIVE_SNAPSHOTS) {
      throw new WasiException("Maximum number of active snapshots exceeded", WasiErrorCode.ENOMEM);
    }

    // Find last full snapshot for this path
    final Optional<Long> lastFullSnapshot = findLastFullSnapshot(rootPath);
    if (!lastFullSnapshot.isPresent()) {
      throw new WasiException("No full snapshot found for differential", WasiErrorCode.ENOENT);
    }

    LOGGER.info(
        () ->
            String.format(
                "Creating differential snapshot: rootPath=%s, baseSnapshot=%d",
                rootPath, lastFullSnapshot.get()));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long snapshotHandle = snapshotHandleGenerator.getAndIncrement();

            final SnapshotMetadata metadata =
                new SnapshotMetadata(
                    snapshotHandle,
                    rootPath,
                    SnapshotType.DIFFERENTIAL,
                    Instant.now(),
                    options.includeHiddenFiles,
                    options.compressionLevel,
                    options.encryptionEnabled,
                    lastFullSnapshot.get());

            snapshotMetadata.put(snapshotHandle, metadata);

            final SnapshotCreateResult result =
                nativeCreateAdvancedSnapshot(
                    wasiContext.getNativeHandle(),
                    snapshotHandle,
                    rootPath,
                    2, // DIFFERENTIAL type
                    lastFullSnapshot.get(),
                    options.includeHiddenFiles,
                    options.compressionLevel,
                    options.encryptionEnabled,
                    options.encryptionKey,
                    true, // enable deduplication
                    true, // enable integrity checking
                    "", // name - TODO: add name field to metadata
                    ""); // description - TODO: add description field to metadata

            if (result.errorCode != 0) {
              snapshotMetadata.remove(snapshotHandle);
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Failed to create differential snapshot: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            final SnapshotInfo snapshotInfo =
                new SnapshotInfo(
                    snapshotHandle,
                    rootPath,
                    SnapshotType.DIFFERENTIAL,
                    result.snapshotSize,
                    metadata);
            activeSnapshots.put(snapshotHandle, snapshotInfo);

            metadata.snapshotSize = result.snapshotSize;
            metadata.fileCount = result.fileCount;

            LOGGER.info(
                () ->
                    String.format(
                        "Created differential snapshot: handle=%d, size=%d bytes, files=%d",
                        snapshotHandle, result.snapshotSize, result.fileCount));

            return snapshotHandle;

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create differential snapshot", e);
            throw new RuntimeException(
                "Differential snapshot creation failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  // Native method declarations
  private static native long nativeInitSnapshotManager(long wasiContextHandle);

  private static native void nativeCleanupSnapshotManager();

  private static native SnapshotCreateResult nativeCreateAdvancedSnapshot(
      long wasiContextHandle,
      long snapshotHandle,
      String rootPath,
      int snapshotType,
      long baseSnapshotHandle,
      boolean includeHiddenFiles,
      int compressionLevel,
      boolean encryptionEnabled,
      byte[] encryptionKey,
      boolean enableDeduplication,
      boolean enableIntegrityChecking,
      String name,
      String description);

  private static native SnapshotCreateResult nativeCreateSnapshot(
      long contextHandle,
      long snapshotHandle,
      String rootPath,
      boolean fullSnapshot,
      long baseSnapshotHandle,
      boolean includeHidden,
      int compressionLevel,
      boolean encryptionEnabled,
      byte[] encryptionKey);

  private static native int nativeRestoreSnapshot(
      long contextHandle,
      long snapshotHandle,
      String targetPath,
      boolean overwriteExisting,
      boolean preservePermissions,
      boolean preserveTimestamps,
      boolean verifyIntegrity);

  private static native SnapshotVerifyResult nativeVerifySnapshot(
      long contextHandle, long snapshotHandle);

  private static native int nativeDeleteSnapshot(long contextHandle, long snapshotHandle);

  /** Snapshot type enumeration. */
  public enum SnapshotType {
    FULL,
    INCREMENTAL,
    DIFFERENTIAL
  }

  /** Snapshot options for creation. */
  public static final class SnapshotOptions {
    public final boolean includeHiddenFiles;
    public final int compressionLevel; // 0-9, 0 = no compression, 9 = maximum
    public final boolean encryptionEnabled;
    public final byte[] encryptionKey;

    /**
     * Creates snapshot options.
     *
     * @param includeHiddenFiles whether to include hidden files
     * @param compressionLevel compression level (0-9)
     * @param encryptionEnabled whether encryption is enabled
     * @param encryptionKey encryption key (may be null)
     */
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

    /**
     * Creates restore options.
     *
     * @param overwriteExisting whether to overwrite existing files
     * @param preservePermissions whether to preserve permissions
     * @param preserveTimestamps whether to preserve timestamps
     * @param verifyIntegrity whether to verify integrity
     */
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
  @SuppressFBWarnings(
      value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
      justification =
          "Data class for snapshot information, fields accessed by native code and reflection")
  public static final class SnapshotInfo {
    public final long handle;
    public final String rootPath;
    public final SnapshotType type;
    public final long snapshotSize;
    public final SnapshotMetadata metadata;
    public final long createdAt;

    /**
     * Creates snapshot information.
     *
     * @param handle the snapshot handle
     * @param rootPath the root path
     * @param type the snapshot type
     * @param snapshotSize the snapshot size
     * @param metadata the snapshot metadata
     */
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
  @SuppressFBWarnings(
      value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
      justification =
          "Data class for snapshot metadata, fields accessed by native code and reflection")
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
    public volatile java.util.List<String> tags;
    public volatile java.util.Map<String, String> properties;

    /**
     * Creates snapshot metadata.
     *
     * @param handle the snapshot handle
     * @param rootPath the root path
     * @param type the snapshot type
     * @param createdAt creation timestamp
     * @param includeHiddenFiles whether hidden files are included
     * @param compressionLevel compression level
     * @param encryptionEnabled whether encryption is enabled
     * @param baseSnapshotHandle base snapshot handle (may be null)
     */
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

    /**
     * Creates snapshot verification result.
     *
     * @param isValid whether the snapshot is valid
     * @param checkedFiles number of checked files
     * @param corruptedFiles number of corrupted files
     * @param missingFiles number of missing files
     * @param checksumMismatch number of checksum mismatches
     */
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

    /**
     * Creates snapshot creation result.
     *
     * @param errorCode the error code
     * @param snapshotSize the snapshot size
     * @param fileCount the file count
     */
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

    /**
     * Creates snapshot verification result.
     *
     * @param errorCode the error code
     * @param isValid whether the snapshot is valid
     * @param checkedFiles number of checked files
     * @param corruptedFiles number of corrupted files
     * @param missingFiles number of missing files
     * @param checksumMismatch number of checksum mismatches
     */
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

  /** Snapshot performance metrics. */
  public static final class SnapshotPerformanceMetrics {
    public volatile long snapshotCreationTimeMs;
    public volatile long snapshotRestoreTimeMs;
    public volatile long totalBytesProcessed;
    public volatile int filesProcessed;

    /** Creates snapshot performance metrics with default values. */
    public SnapshotPerformanceMetrics() {
      this.snapshotCreationTimeMs = 0;
      this.snapshotRestoreTimeMs = 0;
      this.totalBytesProcessed = 0;
      this.filesProcessed = 0;
    }
  }

  /** Deduplication statistics. */
  public static final class DeduplicationStatistics {
    public volatile long duplicateBlocksFound;
    public volatile long bytesDeduplicatedCompressionStatistics;
    public volatile double deduplicationRatio;

    /** Creates deduplication statistics with default values. */
    public DeduplicationStatistics() {
      this.duplicateBlocksFound = 0;
      this.bytesDeduplicatedCompressionStatistics = 0;
      this.deduplicationRatio = 0.0;
    }
  }

  /** Compression statistics. */
  public static final class CompressionStatistics {
    public volatile long originalSize;
    public volatile long compressedSize;
    public volatile double compressionRatio;
    public volatile String compressionAlgorithm;

    /** Creates compression statistics with default values. */
    public CompressionStatistics() {
      this.originalSize = 0;
      this.compressedSize = 0;
      this.compressionRatio = 0.0;
      this.compressionAlgorithm = "none";
    }
  }

  /** Performs periodic cleanup of old or expired snapshots. */
  private Optional<Long> findLastFullSnapshot(final String rootPath) {
    // TODO: Implement finding last full snapshot for a given path
    LOGGER.fine("Finding last full snapshot for: " + rootPath);
    return Optional.empty();
  }

  private void performPeriodicCleanup() {
    // TODO: Implement periodic cleanup of old snapshots
    LOGGER.fine("Performing periodic snapshot cleanup");
  }
}
