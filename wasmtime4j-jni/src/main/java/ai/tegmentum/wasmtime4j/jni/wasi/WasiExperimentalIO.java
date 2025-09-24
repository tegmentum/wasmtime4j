package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of experimental WASI I/O operations.
 *
 * <p>This class provides experimental I/O capabilities as defined in WASI Preview 2, including
 * async file I/O, memory-mapped files, advanced file locking, and directory watching. These
 * features enable high-performance and sophisticated file operations in WASM applications.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Async file I/O with completion callbacks and batching
 *   <li>Memory-mapped file support with efficient access patterns
 *   <li>Advanced file locking mechanisms (shared, exclusive, advisory)
 *   <li>Directory watching and change notification
 *   <li>Vectored I/O operations for improved performance
 *   <li>File system monitoring and event handling
 * </ul>
 *
 * <p>This is an experimental feature and may change in future WASI releases.
 *
 * @since 1.0.0
 */
public final class WasiExperimentalIO {

  private static final Logger LOGGER = Logger.getLogger(WasiExperimentalIO.class.getName());

  /** Maximum number of concurrent async I/O operations. */
  private static final int MAX_CONCURRENT_IO_OPERATIONS = 1000;

  /** Maximum memory-mapped file size (2GB). */
  private static final long MAX_MMAP_SIZE = 2L * 1024L * 1024L * 1024L;

  /** Maximum vectored I/O buffer count. */
  private static final int MAX_VECTOR_BUFFERS = 1024;

  /** The WASI context this I/O handler belongs to. */
  private final WasiContext wasiContext;

  /** Executor service for async operations. */
  private final ExecutorService asyncExecutor;

  /** I/O operation handle generator. */
  private final AtomicLong ioOperationHandleGenerator = new AtomicLong(1);

  /** Active async I/O operations tracking. */
  private final Map<Long, AsyncIOOperation> activeOperations = new ConcurrentHashMap<>();

  /** Memory-mapped files tracking. */
  private final Map<Long, MemoryMappedFileInfo> mappedFiles = new ConcurrentHashMap<>();

  /** Directory watchers tracking. */
  private final Map<Long, DirectoryWatcherInfo> directoryWatchers = new ConcurrentHashMap<>();

  /** File locks tracking. */
  private final Map<Long, FileLockInfo> fileLocks = new ConcurrentHashMap<>();

  /**
   * Creates a new WASI experimental I/O handler.
   *
   * @param wasiContext the WASI context to operate within
   * @param asyncExecutor the executor service for async operations
   * @throws JniException if parameters are null
   */
  public WasiExperimentalIO(final WasiContext wasiContext, final ExecutorService asyncExecutor) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    JniValidation.requireNonNull(asyncExecutor, "asyncExecutor");

    this.wasiContext = wasiContext;
    this.asyncExecutor = asyncExecutor;

    LOGGER.info("Created WASI experimental I/O handler");
  }

  /**
   * Performs async file read with completion callback.
   *
   * <p>Reads data from a file asynchronously, calling the completion callback when done.
   *
   * @param fileHandle the file handle to read from
   * @param offset the file offset to read from
   * @param buffer the buffer to read data into
   * @param completionCallback the callback to invoke on completion
   * @return CompletableFuture that resolves to the I/O operation handle
   * @throws WasiException if async read setup fails
   */
  public CompletableFuture<Long> asyncReadFileAsync(
      final long fileHandle,
      final long offset,
      final ByteBuffer buffer,
      final Consumer<AsyncIOResult> completionCallback) {
    JniValidation.requireNonNull(buffer, "buffer");
    JniValidation.requireNonNull(completionCallback, "completionCallback");
    JniValidation.requireNonNegative(offset, "offset");

    if (activeOperations.size() >= MAX_CONCURRENT_IO_OPERATIONS) {
      throw new WasiException("Maximum concurrent I/O operations exceeded", WasiErrorCode.ENOMEM);
    }

    final int bufferSize = buffer.remaining();
    if (bufferSize == 0) {
      completionCallback.accept(new AsyncIOResult(0, null, true));
      return CompletableFuture.completedFuture(0L);
    }

    LOGGER.fine(
        () ->
            String.format(
                "Starting async file read: handle=%d, offset=%d, size=%d",
                fileHandle, offset, bufferSize));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long operationHandle = ioOperationHandleGenerator.getAndIncrement();

            // Create async operation info
            final AsyncIOOperation operation =
                new AsyncIOOperation(
                    operationHandle,
                    fileHandle,
                    IOOperationType.READ,
                    offset,
                    bufferSize,
                    completionCallback,
                    System.currentTimeMillis());
            activeOperations.put(operationHandle, operation);

            // Start native async read
            final int result =
                nativeAsyncReadFile(
                    wasiContext.getNativeHandle(),
                    operationHandle,
                    fileHandle,
                    offset,
                    buffer,
                    bufferSize);

            if (result != 0) {
              activeOperations.remove(operationHandle);
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
              throw new WasiException(
                  "Failed to start async file read: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            LOGGER.fine(
                () -> String.format("Started async file read: operation=%d", operationHandle));

            return operationHandle;

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start async file read", e);
            throw new RuntimeException("Async file read failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Performs async file write with completion callback.
   *
   * <p>Writes data to a file asynchronously, calling the completion callback when done.
   *
   * @param fileHandle the file handle to write to
   * @param offset the file offset to write at
   * @param buffer the buffer containing data to write
   * @param completionCallback the callback to invoke on completion
   * @return CompletableFuture that resolves to the I/O operation handle
   * @throws WasiException if async write setup fails
   */
  public CompletableFuture<Long> asyncWriteFileAsync(
      final long fileHandle,
      final long offset,
      final ByteBuffer buffer,
      final Consumer<AsyncIOResult> completionCallback) {
    JniValidation.requireNonNull(buffer, "buffer");
    JniValidation.requireNonNull(completionCallback, "completionCallback");
    JniValidation.requireNonNegative(offset, "offset");

    if (activeOperations.size() >= MAX_CONCURRENT_IO_OPERATIONS) {
      throw new WasiException("Maximum concurrent I/O operations exceeded", WasiErrorCode.ENOMEM);
    }

    final int bufferSize = buffer.remaining();
    if (bufferSize == 0) {
      completionCallback.accept(new AsyncIOResult(0, null, true));
      return CompletableFuture.completedFuture(0L);
    }

    LOGGER.fine(
        () ->
            String.format(
                "Starting async file write: handle=%d, offset=%d, size=%d",
                fileHandle, offset, bufferSize));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long operationHandle = ioOperationHandleGenerator.getAndIncrement();

            // Create async operation info
            final AsyncIOOperation operation =
                new AsyncIOOperation(
                    operationHandle,
                    fileHandle,
                    IOOperationType.WRITE,
                    offset,
                    bufferSize,
                    completionCallback,
                    System.currentTimeMillis());
            activeOperations.put(operationHandle, operation);

            // Start native async write
            final int result =
                nativeAsyncWriteFile(
                    wasiContext.getNativeHandle(),
                    operationHandle,
                    fileHandle,
                    offset,
                    buffer,
                    bufferSize);

            if (result != 0) {
              activeOperations.remove(operationHandle);
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
              throw new WasiException(
                  "Failed to start async file write: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            LOGGER.fine(
                () -> String.format("Started async file write: operation=%d", operationHandle));

            return operationHandle;

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start async file write", e);
            throw new RuntimeException("Async file write failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Creates a memory-mapped file mapping.
   *
   * <p>Maps a file into memory for efficient access without explicit read/write operations.
   *
   * @param fileHandle the file handle to map
   * @param offset the file offset to start mapping from
   * @param length the length of the mapping
   * @param protection the memory protection flags
   * @param flags the mapping flags
   * @return CompletableFuture that resolves to the memory mapping handle
   * @throws WasiException if memory mapping fails
   */
  public CompletableFuture<Long> createMemoryMappingAsync(
      final long fileHandle,
      final long offset,
      final long length,
      final MemoryProtection protection,
      final MappingFlags flags) {
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requirePositive(length, "length");
    JniValidation.requireNonNull(protection, "protection");
    JniValidation.requireNonNull(flags, "flags");

    if (length > MAX_MMAP_SIZE) {
      throw new WasiException(
          "Memory mapping too large: " + length + " > " + MAX_MMAP_SIZE, WasiErrorCode.ENOMEM);
    }

    LOGGER.info(
        () ->
            String.format(
                "Creating memory mapping: file=%d, offset=%d, length=%d, protection=%s",
                fileHandle, offset, length, protection));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long mappingHandle = ioOperationHandleGenerator.getAndIncrement();

            final MemoryMappingResult result =
                nativeCreateMemoryMapping(
                    wasiContext.getNativeHandle(),
                    mappingHandle,
                    fileHandle,
                    offset,
                    length,
                    protection.ordinal(),
                    flags.value);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Failed to create memory mapping: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            // Track memory mapping
            final MemoryMappedFileInfo mappingInfo =
                new MemoryMappedFileInfo(
                    mappingHandle,
                    fileHandle,
                    offset,
                    length,
                    protection,
                    result.mappedAddress,
                    System.currentTimeMillis());
            mappedFiles.put(mappingHandle, mappingInfo);

            LOGGER.info(
                () ->
                    String.format(
                        "Created memory mapping: handle=%d, address=0x%x",
                        mappingHandle, result.mappedAddress));

            return mappingHandle;

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create memory mapping", e);
            throw new RuntimeException("Memory mapping failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Acquires a file lock.
   *
   * <p>Acquires an exclusive or shared lock on a file or portion of a file.
   *
   * @param fileHandle the file handle to lock
   * @param offset the offset to start locking from
   * @param length the length to lock (0 for entire file)
   * @param lockType the type of lock to acquire
   * @param blocking whether to block if lock cannot be acquired immediately
   * @return CompletableFuture that resolves to the lock handle
   * @throws WasiException if file locking fails
   */
  public CompletableFuture<Long> acquireFileLockAsync(
      final long fileHandle,
      final long offset,
      final long length,
      final FileLockType lockType,
      final boolean blocking) {
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requireNonNegative(length, "length");
    JniValidation.requireNonNull(lockType, "lockType");

    LOGGER.fine(
        () ->
            String.format(
                "Acquiring file lock: file=%d, offset=%d, length=%d, type=%s, blocking=%b",
                fileHandle, offset, length, lockType, blocking));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long lockHandle = ioOperationHandleGenerator.getAndIncrement();

            final FileLockResult result =
                nativeAcquireFileLock(
                    wasiContext.getNativeHandle(),
                    lockHandle,
                    fileHandle,
                    offset,
                    length,
                    lockType.ordinal(),
                    blocking);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Failed to acquire file lock: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            // Track file lock
            final FileLockInfo lockInfo =
                new FileLockInfo(
                    lockHandle,
                    fileHandle,
                    offset,
                    length,
                    lockType,
                    result.acquired,
                    System.currentTimeMillis());
            fileLocks.put(lockHandle, lockInfo);

            LOGGER.fine(
                () ->
                    String.format(
                        "Acquired file lock: handle=%d, acquired=%b", lockHandle, result.acquired));

            return lockHandle;

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to acquire file lock", e);
            throw new RuntimeException("File lock failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Creates a directory watcher for file system events.
   *
   * <p>Watches a directory for file system changes and invokes callbacks on events.
   *
   * @param directoryPath the directory path to watch
   * @param recursive whether to watch subdirectories recursively
   * @param eventMask the types of events to watch for
   * @param eventCallback the callback to invoke for events
   * @return CompletableFuture that resolves to the watcher handle
   * @throws WasiException if directory watching fails
   */
  public CompletableFuture<Long> createDirectoryWatcherAsync(
      final String directoryPath,
      final boolean recursive,
      final FileSystemEventMask eventMask,
      final Consumer<FileSystemEvent> eventCallback) {
    JniValidation.requireNonEmpty(directoryPath, "directoryPath");
    JniValidation.requireNonNull(eventMask, "eventMask");
    JniValidation.requireNonNull(eventCallback, "eventCallback");

    LOGGER.info(
        () ->
            String.format(
                "Creating directory watcher: path=%s, recursive=%b, events=%s",
                directoryPath, recursive, eventMask));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            final long watcherHandle = ioOperationHandleGenerator.getAndIncrement();

            final DirectoryWatcherResult result =
                nativeCreateDirectoryWatcher(
                    wasiContext.getNativeHandle(),
                    watcherHandle,
                    directoryPath,
                    recursive,
                    eventMask.value);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Failed to create directory watcher: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            // Track directory watcher
            final DirectoryWatcherInfo watcherInfo =
                new DirectoryWatcherInfo(
                    watcherHandle,
                    directoryPath,
                    recursive,
                    eventMask,
                    eventCallback,
                    true,
                    System.currentTimeMillis());
            directoryWatchers.put(watcherHandle, watcherInfo);

            LOGGER.info(
                () ->
                    String.format(
                        "Created directory watcher: handle=%d, watching=%d items",
                        watcherHandle, result.watchedItems));

            return watcherHandle;

          } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create directory watcher", e);
            throw new RuntimeException("Directory watcher failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Performs vectored I/O read operation.
   *
   * <p>Reads data into multiple buffers in a single system call for improved efficiency.
   *
   * @param fileHandle the file handle to read from
   * @param offset the file offset to read from
   * @param buffers the list of buffers to read data into
   * @return CompletableFuture that resolves to the number of bytes read
   * @throws WasiException if vectored read fails
   */
  public CompletableFuture<Integer> vectoredReadAsync(
      final long fileHandle, final long offset, final List<ByteBuffer> buffers) {
    JniValidation.requireNonNull(buffers, "buffers");
    JniValidation.requireNonNegative(offset, "offset");

    if (buffers.isEmpty()) {
      return CompletableFuture.completedFuture(0);
    }

    if (buffers.size() > MAX_VECTOR_BUFFERS) {
      throw new WasiException(
          "Too many vector buffers: " + buffers.size() + " > " + MAX_VECTOR_BUFFERS,
          WasiErrorCode.EINVAL);
    }

    final int totalSize = buffers.stream().mapToInt(ByteBuffer::remaining).sum();
    LOGGER.fine(
        () ->
            String.format(
                "Vectored read: file=%d, offset=%d, buffers=%d, totalSize=%d",
                fileHandle, offset, buffers.size(), totalSize));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            // Prepare buffer info for native call
            final int[] bufferSizes = new int[buffers.size()];
            final ByteBuffer[] bufferArray = new ByteBuffer[buffers.size()];

            for (int i = 0; i < buffers.size(); i++) {
              bufferArray[i] = buffers.get(i);
              bufferSizes[i] = buffers.get(i).remaining();
            }

            final VectoredIOResult result =
                nativeVectoredRead(
                    wasiContext.getNativeHandle(), fileHandle, offset, bufferArray, bufferSizes);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Vectored read failed: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            LOGGER.fine(
                () ->
                    String.format(
                        "Vectored read completed: file=%d, bytes=%d", fileHandle, result.bytesIO));

            return result.bytesIO;

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Vectored read failed", e);
            throw new RuntimeException("Vectored read failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Performs vectored I/O write operation.
   *
   * <p>Writes data from multiple buffers in a single system call for improved efficiency.
   *
   * @param fileHandle the file handle to write to
   * @param offset the file offset to write at
   * @param buffers the list of buffers containing data to write
   * @return CompletableFuture that resolves to the number of bytes written
   * @throws WasiException if vectored write fails
   */
  public CompletableFuture<Integer> vectoredWriteAsync(
      final long fileHandle, final long offset, final List<ByteBuffer> buffers) {
    JniValidation.requireNonNull(buffers, "buffers");
    JniValidation.requireNonNegative(offset, "offset");

    if (buffers.isEmpty()) {
      return CompletableFuture.completedFuture(0);
    }

    if (buffers.size() > MAX_VECTOR_BUFFERS) {
      throw new WasiException(
          "Too many vector buffers: " + buffers.size() + " > " + MAX_VECTOR_BUFFERS,
          WasiErrorCode.EINVAL);
    }

    final int totalSize = buffers.stream().mapToInt(ByteBuffer::remaining).sum();
    LOGGER.fine(
        () ->
            String.format(
                "Vectored write: file=%d, offset=%d, buffers=%d, totalSize=%d",
                fileHandle, offset, buffers.size(), totalSize));

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            // Prepare buffer info for native call
            final int[] bufferSizes = new int[buffers.size()];
            final ByteBuffer[] bufferArray = new ByteBuffer[buffers.size()];

            for (int i = 0; i < buffers.size(); i++) {
              bufferArray[i] = buffers.get(i);
              bufferSizes[i] = buffers.get(i).remaining();
            }

            final VectoredIOResult result =
                nativeVectoredWrite(
                    wasiContext.getNativeHandle(), fileHandle, offset, bufferArray, bufferSizes);

            if (result.errorCode != 0) {
              final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result.errorCode);
              throw new WasiException(
                  "Vectored write failed: "
                      + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
                  errorCode != null ? errorCode : WasiErrorCode.EIO);
            }

            LOGGER.fine(
                () ->
                    String.format(
                        "Vectored write completed: file=%d, bytes=%d", fileHandle, result.bytesIO));

            return result.bytesIO;

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Vectored write failed", e);
            throw new RuntimeException("Vectored write failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Releases a file lock.
   *
   * @param lockHandle the lock handle to release
   * @throws WasiException if lock release fails
   */
  public void releaseFileLock(final long lockHandle) {
    final FileLockInfo lockInfo = fileLocks.get(lockHandle);
    if (lockInfo == null) {
      throw new WasiException("Invalid lock handle: " + lockHandle, WasiErrorCode.EBADF);
    }

    LOGGER.fine(() -> String.format("Releasing file lock: handle=%d", lockHandle));

    try {
      final int result = nativeReleaseFileLock(wasiContext.getNativeHandle(), lockHandle);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Failed to release file lock: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      fileLocks.remove(lockHandle);
      LOGGER.fine(() -> String.format("Released file lock: handle=%d", lockHandle));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to release file lock: " + lockHandle, e);
      throw new WasiException("Lock release failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Unmaps a memory-mapped file.
   *
   * @param mappingHandle the mapping handle to unmap
   * @throws WasiException if unmapping fails
   */
  public void unmapMemoryMapping(final long mappingHandle) {
    final MemoryMappedFileInfo mappingInfo = mappedFiles.get(mappingHandle);
    if (mappingInfo == null) {
      throw new WasiException("Invalid mapping handle: " + mappingHandle, WasiErrorCode.EBADF);
    }

    LOGGER.info(() -> String.format("Unmapping memory mapping: handle=%d", mappingHandle));

    try {
      final int result = nativeUnmapMemoryMapping(wasiContext.getNativeHandle(), mappingHandle);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Failed to unmap memory mapping: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      mappedFiles.remove(mappingHandle);
      LOGGER.info(() -> String.format("Unmapped memory mapping: handle=%d", mappingHandle));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to unmap memory mapping: " + mappingHandle, e);
      throw new WasiException("Memory unmap failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Stops a directory watcher.
   *
   * @param watcherHandle the watcher handle to stop
   */
  public void stopDirectoryWatcher(final long watcherHandle) {
    final DirectoryWatcherInfo watcherInfo = directoryWatchers.get(watcherHandle);
    if (watcherInfo == null) {
      LOGGER.fine(
          () ->
              String.format(
                  "Directory watcher already stopped or invalid: handle=%d", watcherHandle));
      return;
    }

    LOGGER.info(() -> String.format("Stopping directory watcher: handle=%d", watcherHandle));

    try {
      final int result = nativeStopDirectoryWatcher(wasiContext.getNativeHandle(), watcherHandle);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        LOGGER.warning(
            "Failed to stop directory watcher "
                + watcherHandle
                + ": "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"));
      }

      watcherInfo.active = false;
      directoryWatchers.remove(watcherHandle);
      LOGGER.info(() -> String.format("Stopped directory watcher: handle=%d", watcherHandle));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error stopping directory watcher: " + watcherHandle, e);
      directoryWatchers.remove(watcherHandle);
    }
  }

  /** Closes the experimental I/O handler and cleans up resources. */
  public void close() {
    LOGGER.info("Closing experimental I/O handler");

    // Stop all directory watchers
    for (final Long watcherHandle : directoryWatchers.keySet()) {
      try {
        stopDirectoryWatcher(watcherHandle);
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Error stopping directory watcher during shutdown", e);
      }
    }

    // Release all file locks
    for (final Long lockHandle : fileLocks.keySet()) {
      try {
        releaseFileLock(lockHandle);
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Error releasing file lock during shutdown", e);
      }
    }

    // Unmap all memory mappings
    for (final Long mappingHandle : mappedFiles.keySet()) {
      try {
        unmapMemoryMapping(mappingHandle);
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Error unmapping memory during shutdown", e);
      }
    }

    // Cancel all active operations
    for (final AsyncIOOperation operation : activeOperations.values()) {
      try {
        nativeCancelAsyncOperation(wasiContext.getNativeHandle(), operation.handle);
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Error canceling async operation during shutdown", e);
      }
    }

    activeOperations.clear();
    mappedFiles.clear();
    directoryWatchers.clear();
    fileLocks.clear();

    LOGGER.info("Experimental I/O handler closed successfully");
  }

  // Native method declarations
  private static native int nativeAsyncReadFile(
      long contextHandle, long operationHandle, long fileHandle, long offset, ByteBuffer buffer, int bufferSize);

  private static native int nativeAsyncWriteFile(
      long contextHandle, long operationHandle, long fileHandle, long offset, ByteBuffer buffer, int bufferSize);

  private static native MemoryMappingResult nativeCreateMemoryMapping(
      long contextHandle, long mappingHandle, long fileHandle, long offset, long length, int protection, int flags);

  private static native FileLockResult nativeAcquireFileLock(
      long contextHandle, long lockHandle, long fileHandle, long offset, long length, int lockType, boolean blocking);

  private static native DirectoryWatcherResult nativeCreateDirectoryWatcher(
      long contextHandle, long watcherHandle, String directoryPath, boolean recursive, int eventMask);

  private static native VectoredIOResult nativeVectoredRead(
      long contextHandle, long fileHandle, long offset, ByteBuffer[] buffers, int[] bufferSizes);

  private static native VectoredIOResult nativeVectoredWrite(
      long contextHandle, long fileHandle, long offset, ByteBuffer[] buffers, int[] bufferSizes);

  private static native int nativeReleaseFileLock(long contextHandle, long lockHandle);

  private static native int nativeUnmapMemoryMapping(long contextHandle, long mappingHandle);

  private static native int nativeStopDirectoryWatcher(long contextHandle, long watcherHandle);

  private static native int nativeCancelAsyncOperation(long contextHandle, long operationHandle);

  /** I/O operation type enumeration. */
  public enum IOOperationType {
    READ,
    WRITE,
    SYNC
  }

  /** Memory protection enumeration. */
  public enum MemoryProtection {
    READ_ONLY,
    READ_WRITE,
    EXECUTE_READ,
    EXECUTE_READ_WRITE
  }

  /** File lock type enumeration. */
  public enum FileLockType {
    SHARED,
    EXCLUSIVE,
    ADVISORY
  }

  /** File system event types. */
  public enum FileSystemEventType {
    CREATED,
    MODIFIED,
    DELETED,
    RENAMED,
    MOVED,
    ATTRIBUTE_CHANGED
  }

  /** Mapping flags. */
  public static final class MappingFlags {
    public static final MappingFlags PRIVATE = new MappingFlags(0x01);
    public static final MappingFlags SHARED = new MappingFlags(0x02);
    public static final MappingFlags ANONYMOUS = new MappingFlags(0x04);
    public static final MappingFlags FIXED = new MappingFlags(0x08);

    public final int value;

    private MappingFlags(final int value) {
      this.value = value;
    }
  }

  /** File system event mask. */
  public static final class FileSystemEventMask {
    public static final FileSystemEventMask ALL = new FileSystemEventMask(0xFF);
    public static final FileSystemEventMask CREATE = new FileSystemEventMask(0x01);
    public static final FileSystemEventMask MODIFY = new FileSystemEventMask(0x02);
    public static final FileSystemEventMask DELETE = new FileSystemEventMask(0x04);
    public static final FileSystemEventMask RENAME = new FileSystemEventMask(0x08);
    public static final FileSystemEventMask MOVE = new FileSystemEventMask(0x10);
    public static final FileSystemEventMask ATTRIBUTE = new FileSystemEventMask(0x20);

    public final int value;

    private FileSystemEventMask(final int value) {
      this.value = value;
    }
  }

  /** Async I/O result. */
  public static final class AsyncIOResult {
    public final int bytesTransferred;
    public final WasiException error;
    public final boolean completed;

    public AsyncIOResult(final int bytesTransferred, final WasiException error, final boolean completed) {
      this.bytesTransferred = bytesTransferred;
      this.error = error;
      this.completed = completed;
    }
  }

  /** File system event. */
  public static final class FileSystemEvent {
    public final FileSystemEventType eventType;
    public final String path;
    public final String oldPath; // For rename/move events
    public final long timestamp;

    public FileSystemEvent(
        final FileSystemEventType eventType, final String path, final String oldPath, final long timestamp) {
      this.eventType = eventType;
      this.path = path;
      this.oldPath = oldPath;
      this.timestamp = timestamp;
    }
  }

  /** Async I/O operation information. */
  private static final class AsyncIOOperation {
    public final long handle;
    public final long fileHandle;
    public final IOOperationType operationType;
    public final long offset;
    public final int bufferSize;
    public final Consumer<AsyncIOResult> callback;
    public final long startTime;

    public AsyncIOOperation(
        final long handle,
        final long fileHandle,
        final IOOperationType operationType,
        final long offset,
        final int bufferSize,
        final Consumer<AsyncIOResult> callback,
        final long startTime) {
      this.handle = handle;
      this.fileHandle = fileHandle;
      this.operationType = operationType;
      this.offset = offset;
      this.bufferSize = bufferSize;
      this.callback = callback;
      this.startTime = startTime;
    }
  }

  /** Memory-mapped file information. */
  private static final class MemoryMappedFileInfo {
    public final long handle;
    public final long fileHandle;
    public final long offset;
    public final long length;
    public final MemoryProtection protection;
    public final long mappedAddress;
    public final long createdAt;

    public MemoryMappedFileInfo(
        final long handle,
        final long fileHandle,
        final long offset,
        final long length,
        final MemoryProtection protection,
        final long mappedAddress,
        final long createdAt) {
      this.handle = handle;
      this.fileHandle = fileHandle;
      this.offset = offset;
      this.length = length;
      this.protection = protection;
      this.mappedAddress = mappedAddress;
      this.createdAt = createdAt;
    }
  }

  /** File lock information. */
  private static final class FileLockInfo {
    public final long handle;
    public final long fileHandle;
    public final long offset;
    public final long length;
    public final FileLockType lockType;
    public final boolean acquired;
    public final long createdAt;

    public FileLockInfo(
        final long handle,
        final long fileHandle,
        final long offset,
        final long length,
        final FileLockType lockType,
        final boolean acquired,
        final long createdAt) {
      this.handle = handle;
      this.fileHandle = fileHandle;
      this.offset = offset;
      this.length = length;
      this.lockType = lockType;
      this.acquired = acquired;
      this.createdAt = createdAt;
    }
  }

  /** Directory watcher information. */
  private static final class DirectoryWatcherInfo {
    public final long handle;
    public final String directoryPath;
    public final boolean recursive;
    public final FileSystemEventMask eventMask;
    public final Consumer<FileSystemEvent> callback;
    public volatile boolean active;
    public final long createdAt;

    public DirectoryWatcherInfo(
        final long handle,
        final String directoryPath,
        final boolean recursive,
        final FileSystemEventMask eventMask,
        final Consumer<FileSystemEvent> callback,
        final boolean active,
        final long createdAt) {
      this.handle = handle;
      this.directoryPath = directoryPath;
      this.recursive = recursive;
      this.eventMask = eventMask;
      this.callback = callback;
      this.active = active;
      this.createdAt = createdAt;
    }
  }

  // Native result classes
  private static final class MemoryMappingResult {
    public final int errorCode;
    public final long mappedAddress;

    public MemoryMappingResult(final int errorCode, final long mappedAddress) {
      this.errorCode = errorCode;
      this.mappedAddress = mappedAddress;
    }
  }

  private static final class FileLockResult {
    public final int errorCode;
    public final boolean acquired;

    public FileLockResult(final int errorCode, final boolean acquired) {
      this.errorCode = errorCode;
      this.acquired = acquired;
    }
  }

  private static final class DirectoryWatcherResult {
    public final int errorCode;
    public final int watchedItems;

    public DirectoryWatcherResult(final int errorCode, final int watchedItems) {
      this.errorCode = errorCode;
      this.watchedItems = watchedItems;
    }
  }

  private static final class VectoredIOResult {
    public final int errorCode;
    public final int bytesIO;

    public VectoredIOResult(final int errorCode, final int bytesIO) {
      this.errorCode = errorCode;
      this.bytesIO = bytesIO;
    }
  }
}