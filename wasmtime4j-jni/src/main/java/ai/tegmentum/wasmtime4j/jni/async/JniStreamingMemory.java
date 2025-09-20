package ai.tegmentum.wasmtime4j.jni.async;

import ai.tegmentum.wasmtime4j.async.StreamingMemory;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.JniMemory;
import ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of the StreamingMemory interface.
 *
 * <p>This implementation provides asynchronous and streaming memory operations using JNI to
 * interface with the native Wasmtime runtime. It supports bulk operations, streaming I/O,
 * and comprehensive statistics collection for performance monitoring.
 *
 * @since 1.0.0
 */
public final class JniStreamingMemory extends JniMemory implements StreamingMemory {
  private static final Logger LOGGER = Logger.getLogger(JniStreamingMemory.class.getName());

  private final AtomicLong operationCounter = new AtomicLong();
  private final ConcurrentHashMap<Long, AsyncMemoryOperation> activeOperations = new ConcurrentHashMap<>();
  private final StreamingMemoryStatisticsImpl statistics = new StreamingMemoryStatisticsImpl();

  // Native method declarations
  private static native long nativeReadMemoryAsync(
      long memoryHandle, int offset, int length, long operationId);

  private static native long nativeWriteMemoryAsync(
      long memoryHandle, int offset, ByteBuffer data, long operationId);

  private static native long nativeBulkCopyAsync(
      long destMemoryHandle, long srcMemoryHandle, int srcOffset, int destOffset, int length, long operationId);

  private static native long nativeBulkFillAsync(
      long memoryHandle, int offset, int length, byte value, long operationId);

  private static native int nativePollMemoryOperation(long memoryHandle, long operationId);

  private static native ByteBuffer nativeGetMemoryOperationResult(long memoryHandle, long operationId);

  private static native void nativeCleanupMemoryOperation(long memoryHandle, long operationId);

  private static native int nativeCancelMemoryOperation(long memoryHandle, long operationId);

  private static native long nativeCreateMemoryStream(
      long memoryHandle, int offset, int length, int bufferSize, int mode);

  private static native void nativeGetStreamingStatistics(long memoryHandle, long[] statsArray);

  static {
    try {
      System.loadLibrary("wasmtime4j");
    } catch (UnsatisfiedLinkError e) {
      LOGGER.log(Level.SEVERE, "Failed to load native library", e);
      throw new RuntimeException("Failed to load wasmtime4j native library", e);
    }
  }

  public JniStreamingMemory(final long memoryHandle) {
    super(memoryHandle);
  }

  @Override
  public CompletableFuture<ByteBuffer> readAsync(final int offset, final int length) {
    return readAsync(offset, length, createDefaultReadOptions());
  }

  @Override
  public CompletableFuture<ByteBuffer> readAsync(
      final int offset, final int length, final ReadOptions options) {
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requirePositive(length, "length");
    JniValidation.requireNonNull(options, "options");

    if (!isValid()) {
      return CompletableFuture.failedFuture(new WasmException("Memory is not valid"));
    }

    final long operationId = operationCounter.getAndIncrement();
    final CompletableFuture<ByteBuffer> future = new CompletableFuture<>();

    final AsyncMemoryOperation operation = new AsyncMemoryOperation(
        operationId, AsyncMemoryOperationType.READ, future, System.nanoTime(), options);

    activeOperations.put(operationId, operation);
    statistics.incrementAsyncReadsStarted();

    // Apply timeout if specified
    if (options.getTimeout() != null) {
      future.orTimeout(options.getTimeout().toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    final Executor executor = options.getExecutor() != null ?
        options.getExecutor() : ForkJoinPool.commonPool();

    executor.execute(() -> {
      try {
        final long nativeOperationId = nativeReadMemoryAsync(getHandle(), offset, length, operationId);
        if (nativeOperationId == 0) {
          completeExceptionally(operationId, new WasmException("Failed to start async read"));
          return;
        }

        pollOperationCompletion(operationId, executor);

      } catch (Exception e) {
        completeExceptionally(operationId, JniExceptionMapper.mapToWasmException(e));
      }
    });

    return future;
  }

  @Override
  public CompletableFuture<Void> writeAsync(final int offset, final ByteBuffer data) {
    return writeAsync(offset, data, createDefaultWriteOptions());
  }

  @Override
  public CompletableFuture<Void> writeAsync(
      final int offset, final ByteBuffer data, final WriteOptions options) {
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requireNonNull(data, "data");
    JniValidation.requireNonNull(options, "options");

    if (!isValid()) {
      return CompletableFuture.failedFuture(new WasmException("Memory is not valid"));
    }

    final long operationId = operationCounter.getAndIncrement();
    final CompletableFuture<Void> future = new CompletableFuture<>();

    final AsyncMemoryOperation operation = new AsyncMemoryOperation(
        operationId, AsyncMemoryOperationType.WRITE, future, System.nanoTime(), options);

    activeOperations.put(operationId, operation);
    statistics.incrementAsyncWritesStarted();

    // Apply timeout if specified
    if (options.getTimeout() != null) {
      future.orTimeout(options.getTimeout().toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    final Executor executor = options.getExecutor() != null ?
        options.getExecutor() : ForkJoinPool.commonPool();

    executor.execute(() -> {
      try {
        final long nativeOperationId = nativeWriteMemoryAsync(getHandle(), offset, data, operationId);
        if (nativeOperationId == 0) {
          completeExceptionally(operationId, new WasmException("Failed to start async write"));
          return;
        }

        pollOperationCompletion(operationId, executor);

      } catch (Exception e) {
        completeExceptionally(operationId, JniExceptionMapper.mapToWasmException(e));
      }
    });

    return future;
  }

  @Override
  public InputStream createInputStream(final int offset, final int length) {
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requireNonNegative(length, "length");

    return new MemoryInputStream(offset, length);
  }

  @Override
  public OutputStream createOutputStream(final int offset, final int maxLength) {
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requireNonNegative(maxLength, "maxLength");

    return new MemoryOutputStream(offset, maxLength);
  }

  @Override
  public MemoryStream createMemoryStream(final int offset, final int length, final StreamOptions options) {
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requireNonNegative(length, "length");
    JniValidation.requireNonNull(options, "options");

    final long streamHandle = nativeCreateMemoryStream(
        getHandle(), offset, length, options.getBufferSize(), getStreamModeValue(options.getReadMode()));

    if (streamHandle == 0) {
      throw new WasmException("Failed to create memory stream");
    }

    return new JniMemoryStream(streamHandle, options);
  }

  @Override
  public CompletableFuture<Void> bulkCopy(
      final StreamingMemory source, final int srcOffset, final int destOffset, final int length) {
    JniValidation.requireNonNull(source, "source");
    JniValidation.requireNonNegative(srcOffset, "srcOffset");
    JniValidation.requireNonNegative(destOffset, "destOffset");
    JniValidation.requirePositive(length, "length");

    if (!isValid()) {
      return CompletableFuture.failedFuture(new WasmException("Memory is not valid"));
    }

    final long operationId = operationCounter.getAndIncrement();
    final CompletableFuture<Void> future = new CompletableFuture<>();

    final AsyncMemoryOperation operation = new AsyncMemoryOperation(
        operationId, AsyncMemoryOperationType.BULK_COPY, future, System.nanoTime(), null);

    activeOperations.put(operationId, operation);
    statistics.incrementBulkCopyOperations();

    ForkJoinPool.commonPool().execute(() -> {
      try {
        final long sourceHandle = ((JniStreamingMemory) source).getHandle();
        final long nativeOperationId = nativeBulkCopyAsync(
            getHandle(), sourceHandle, srcOffset, destOffset, length, operationId);

        if (nativeOperationId == 0) {
          completeExceptionally(operationId, new WasmException("Failed to start bulk copy"));
          return;
        }

        pollOperationCompletion(operationId, ForkJoinPool.commonPool());

      } catch (Exception e) {
        completeExceptionally(operationId, JniExceptionMapper.mapToWasmException(e));
      }
    });

    return future;
  }

  @Override
  public CompletableFuture<Void> bulkFill(final int offset, final int length, final byte value) {
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requirePositive(length, "length");

    if (!isValid()) {
      return CompletableFuture.failedFuture(new WasmException("Memory is not valid"));
    }

    final long operationId = operationCounter.getAndIncrement();
    final CompletableFuture<Void> future = new CompletableFuture<>();

    final AsyncMemoryOperation operation = new AsyncMemoryOperation(
        operationId, AsyncMemoryOperationType.BULK_FILL, future, System.nanoTime(), null);

    activeOperations.put(operationId, operation);
    statistics.incrementBulkFillOperations();

    ForkJoinPool.commonPool().execute(() -> {
      try {
        final long nativeOperationId = nativeBulkFillAsync(getHandle(), offset, length, value, operationId);
        if (nativeOperationId == 0) {
          completeExceptionally(operationId, new WasmException("Failed to start bulk fill"));
          return;
        }

        pollOperationCompletion(operationId, ForkJoinPool.commonPool());

      } catch (Exception e) {
        completeExceptionally(operationId, JniExceptionMapper.mapToWasmException(e));
      }
    });

    return future;
  }

  @Override
  public StreamingMemoryStatistics getStreamingStatistics() {
    updateNativeStatistics();
    return statistics;
  }

  @Override
  public void close() {
    LOGGER.info("Closing JNI streaming memory and cancelling active operations");

    // Cancel all active operations
    for (final AsyncMemoryOperation operation : activeOperations.values()) {
      try {
        nativeCancelMemoryOperation(getHandle(), operation.operationId);
        operation.future.cancel(true);
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Failed to cancel operation " + operation.operationId, e);
      }
    }

    activeOperations.clear();
    super.close();
  }

  // Private helper methods

  private void pollOperationCompletion(final long operationId, final Executor executor) {
    executor.execute(() -> {
      try {
        final AsyncMemoryOperation operation = activeOperations.get(operationId);
        if (operation == null || operation.future.isDone()) {
          return;
        }

        final int status = nativePollMemoryOperation(getHandle(), operationId);

        switch (status) {
          case 0: // Pending
            // Schedule next poll
            try {
              Thread.sleep(5); // Brief pause before next poll
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              completeExceptionally(operationId, new WasmException("Operation interrupted"));
              return;
            }
            pollOperationCompletion(operationId, executor);
            break;

          case 1: // Completed successfully
            if (operation.operationType == AsyncMemoryOperationType.READ) {
              final ByteBuffer result = nativeGetMemoryOperationResult(getHandle(), operationId);
              completeSuccessfully(operationId, result);
            } else {
              completeSuccessfully(operationId, null);
            }
            break;

          case -1: // Failed
            completeExceptionally(operationId, new WasmException("Async memory operation failed"));
            break;

          case -2: // Cancelled
            operation.future.cancel(true);
            cleanupOperation(operationId);
            break;

          default:
            completeExceptionally(operationId, new WasmException("Unknown operation status: " + status));
            break;
        }
      } catch (Exception e) {
        completeExceptionally(operationId, JniExceptionMapper.mapToWasmException(e));
      }
    });
  }

  @SuppressWarnings("unchecked")
  private void completeSuccessfully(final long operationId, final Object result) {
    final AsyncMemoryOperation operation = activeOperations.get(operationId);
    if (operation == null) {
      return;
    }

    try {
      ((CompletableFuture<Object>) operation.future).complete(result);

      final long durationNanos = System.nanoTime() - operation.startTime;
      updateSuccessStatistics(operation.operationType, durationNanos);

    } catch (Exception e) {
      completeExceptionally(operationId, JniExceptionMapper.mapToWasmException(e));
    } finally {
      cleanupOperation(operationId);
    }
  }

  @SuppressWarnings("unchecked")
  private void completeExceptionally(final long operationId, final Throwable throwable) {
    final AsyncMemoryOperation operation = activeOperations.get(operationId);
    if (operation == null) {
      return;
    }

    ((CompletableFuture<Object>) operation.future).completeExceptionally(throwable);
    updateFailureStatistics(operation.operationType);
    cleanupOperation(operationId);
  }

  private void cleanupOperation(final long operationId) {
    activeOperations.remove(operationId);

    try {
      nativeCleanupMemoryOperation(getHandle(), operationId);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to cleanup native operation " + operationId, e);
    }
  }

  private void updateNativeStatistics() {
    final long[] statsArray = new long[10];
    try {
      nativeGetStreamingStatistics(getHandle(), statsArray);
      statistics.updateFromNative(statsArray);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to update native statistics", e);
    }
  }

  private void updateSuccessStatistics(final AsyncMemoryOperationType operationType, final long durationNanos) {
    final double durationMs = durationNanos / 1_000_000.0;

    switch (operationType) {
      case READ:
        statistics.incrementAsyncReadsCompleted();
        statistics.updateAverageReadTime(durationMs);
        break;
      case WRITE:
        statistics.incrementAsyncWritesCompleted();
        statistics.updateAverageWriteTime(durationMs);
        break;
      case BULK_COPY:
      case BULK_FILL:
        // Handled by specific increment methods
        break;
    }
  }

  private void updateFailureStatistics(final AsyncMemoryOperationType operationType) {
    switch (operationType) {
      case READ:
        statistics.incrementAsyncReadsFailed();
        break;
      case WRITE:
        statistics.incrementAsyncWritesFailed();
        break;
      case BULK_COPY:
      case BULK_FILL:
        // Handled by specific failure tracking
        break;
    }
  }

  private ReadOptions createDefaultReadOptions() {
    return new ReadOptionsImpl(null, null, 8192, true, 0);
  }

  private WriteOptions createDefaultWriteOptions() {
    return new WriteOptionsImpl(null, null, 8192, true, false, 0);
  }

  private int getStreamModeValue(final StreamMode mode) {
    switch (mode) {
      case SEQUENTIAL: return 0;
      case RANDOM: return 1;
      case BUFFERED: return 2;
      case DIRECT: return 3;
      default: return 0;
    }
  }

  // Inner classes

  private enum AsyncMemoryOperationType {
    READ, WRITE, BULK_COPY, BULK_FILL
  }

  private static final class AsyncMemoryOperation {
    final long operationId;
    final AsyncMemoryOperationType operationType;
    final CompletableFuture<?> future;
    final long startTime;
    final Object options;

    AsyncMemoryOperation(final long operationId, final AsyncMemoryOperationType operationType,
                        final CompletableFuture<?> future, final long startTime, final Object options) {
      this.operationId = operationId;
      this.operationType = operationType;
      this.future = future;
      this.startTime = startTime;
      this.options = options;
    }
  }

  private final class MemoryInputStream extends InputStream {
    private final int startOffset;
    private final int maxLength;
    private int currentPosition;

    MemoryInputStream(final int offset, final int length) {
      this.startOffset = offset;
      this.maxLength = length;
      this.currentPosition = 0;
    }

    @Override
    public int read() throws IOException {
      if (currentPosition >= maxLength) {
        return -1;
      }

      try {
        final byte[] buffer = new byte[1];
        final int bytesRead = read(buffer, 0, 1);
        return bytesRead == -1 ? -1 : (buffer[0] & 0xFF);
      } catch (Exception e) {
        throw new IOException("Failed to read from memory", e);
      }
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      if (currentPosition >= maxLength) {
        return -1;
      }

      final int actualLen = Math.min(len, maxLength - currentPosition);
      try {
        final ByteBuffer buffer = JniStreamingMemory.this.read(startOffset + currentPosition, actualLen);
        buffer.get(b, off, actualLen);
        currentPosition += actualLen;
        return actualLen;
      } catch (Exception e) {
        throw new IOException("Failed to read from memory", e);
      }
    }
  }

  private final class MemoryOutputStream extends OutputStream {
    private final int startOffset;
    private final int maxLength;
    private int currentPosition;

    MemoryOutputStream(final int offset, final int maxLength) {
      this.startOffset = offset;
      this.maxLength = maxLength;
      this.currentPosition = 0;
    }

    @Override
    public void write(final int b) throws IOException {
      write(new byte[]{(byte) b}, 0, 1);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
      if (currentPosition + len > maxLength) {
        throw new IOException("Write would exceed maximum length");
      }

      try {
        final ByteBuffer buffer = ByteBuffer.allocate(len);
        buffer.put(b, off, len);
        buffer.flip();
        JniStreamingMemory.this.write(startOffset + currentPosition, buffer);
        currentPosition += len;
      } catch (Exception e) {
        throw new IOException("Failed to write to memory", e);
      }
    }
  }

  private static final class JniMemoryStream implements MemoryStream {
    private final long streamHandle;
    private final StreamOptions options;

    JniMemoryStream(final long streamHandle, final StreamOptions options) {
      this.streamHandle = streamHandle;
      this.options = options;
    }

    @Override
    public CompletableFuture<Integer> readAsync(final ByteBuffer buffer) {
      // Implementation would use native stream read
      return CompletableFuture.supplyAsync(() -> 0);
    }

    @Override
    public CompletableFuture<Integer> writeAsync(final ByteBuffer buffer) {
      // Implementation would use native stream write
      return CompletableFuture.supplyAsync(() -> buffer.remaining());
    }

    @Override
    public CompletableFuture<Void> flush() {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {
      // Native cleanup would happen here
    }

    @Override
    public long getPosition() {
      return 0; // Implementation would track position
    }

    @Override
    public void setPosition(final long position) {
      // Implementation would set native stream position
    }

    @Override
    public long getRemaining() {
      return 0; // Implementation would calculate remaining bytes
    }

    @Override
    public boolean isEOF() {
      return false; // Implementation would check native EOF status
    }

    @Override
    public StreamOptions getStreamOptions() {
      return options;
    }
  }

  // Implementation classes for options
  private static final class ReadOptionsImpl implements ReadOptions {
    private final Duration timeout;
    private final Executor executor;
    private final int bufferSize;
    private final boolean cancellable;
    private final int priority;

    ReadOptionsImpl(final Duration timeout, final Executor executor, final int bufferSize,
                   final boolean cancellable, final int priority) {
      this.timeout = timeout;
      this.executor = executor;
      this.bufferSize = bufferSize;
      this.cancellable = cancellable;
      this.priority = priority;
    }

    @Override
    public Duration getTimeout() { return timeout; }
    @Override
    public Executor getExecutor() { return executor; }
    @Override
    public int getBufferSize() { return bufferSize; }
    @Override
    public boolean isCancellable() { return cancellable; }
    @Override
    public int getPriority() { return priority; }
  }

  private static final class WriteOptionsImpl implements WriteOptions {
    private final Duration timeout;
    private final Executor executor;
    private final int bufferSize;
    private final boolean cancellable;
    private final boolean immediateFlush;
    private final int priority;

    WriteOptionsImpl(final Duration timeout, final Executor executor, final int bufferSize,
                    final boolean cancellable, final boolean immediateFlush, final int priority) {
      this.timeout = timeout;
      this.executor = executor;
      this.bufferSize = bufferSize;
      this.cancellable = cancellable;
      this.immediateFlush = immediateFlush;
      this.priority = priority;
    }

    @Override
    public Duration getTimeout() { return timeout; }
    @Override
    public Executor getExecutor() { return executor; }
    @Override
    public int getBufferSize() { return bufferSize; }
    @Override
    public boolean isCancellable() { return cancellable; }
    @Override
    public boolean isImmediateFlush() { return immediateFlush; }
    @Override
    public int getPriority() { return priority; }
  }

  private static final class StreamingMemoryStatisticsImpl implements StreamingMemoryStatistics {
    private final AtomicLong asyncReadsStarted = new AtomicLong();
    private final AtomicLong asyncReadsCompleted = new AtomicLong();
    private final AtomicLong asyncReadsFailed = new AtomicLong();
    private final AtomicLong asyncWritesStarted = new AtomicLong();
    private final AtomicLong asyncWritesCompleted = new AtomicLong();
    private final AtomicLong asyncWritesFailed = new AtomicLong();
    private final AtomicLong bulkCopyOperations = new AtomicLong();
    private final AtomicLong bulkFillOperations = new AtomicLong();
    private final AtomicLong totalBytesRead = new AtomicLong();
    private final AtomicLong totalBytesWritten = new AtomicLong();
    private volatile double averageReadTimeMs = 0.0;
    private volatile double averageWriteTimeMs = 0.0;
    private volatile long peakStreamingMemoryUsage = 0;

    void incrementAsyncReadsStarted() { asyncReadsStarted.incrementAndGet(); }
    void incrementAsyncReadsCompleted() { asyncReadsCompleted.incrementAndGet(); }
    void incrementAsyncReadsFailed() { asyncReadsFailed.incrementAndGet(); }
    void incrementAsyncWritesStarted() { asyncWritesStarted.incrementAndGet(); }
    void incrementAsyncWritesCompleted() { asyncWritesCompleted.incrementAndGet(); }
    void incrementAsyncWritesFailed() { asyncWritesFailed.incrementAndGet(); }
    void incrementBulkCopyOperations() { bulkCopyOperations.incrementAndGet(); }
    void incrementBulkFillOperations() { bulkFillOperations.incrementAndGet(); }

    void updateAverageReadTime(final double timeMs) {
      final long completedCount = asyncReadsCompleted.get();
      if (completedCount > 0) {
        averageReadTimeMs = ((averageReadTimeMs * (completedCount - 1)) + timeMs) / completedCount;
      }
    }

    void updateAverageWriteTime(final double timeMs) {
      final long completedCount = asyncWritesCompleted.get();
      if (completedCount > 0) {
        averageWriteTimeMs = ((averageWriteTimeMs * (completedCount - 1)) + timeMs) / completedCount;
      }
    }

    void updateFromNative(final long[] stats) {
      // Update statistics from native array
      if (stats.length >= 10) {
        totalBytesRead.set(stats[6]);
        totalBytesWritten.set(stats[7]);
        peakStreamingMemoryUsage = stats[9];
      }
    }

    @Override
    public long getAsyncReadsStarted() { return asyncReadsStarted.get(); }
    @Override
    public long getAsyncReadsCompleted() { return asyncReadsCompleted.get(); }
    @Override
    public long getAsyncReadsFailed() { return asyncReadsFailed.get(); }
    @Override
    public long getAsyncWritesStarted() { return asyncWritesStarted.get(); }
    @Override
    public long getAsyncWritesCompleted() { return asyncWritesCompleted.get(); }
    @Override
    public long getAsyncWritesFailed() { return asyncWritesFailed.get(); }
    @Override
    public long getBulkCopyOperations() { return bulkCopyOperations.get(); }
    @Override
    public long getBulkFillOperations() { return bulkFillOperations.get(); }
    @Override
    public long getTotalBytesRead() { return totalBytesRead.get(); }
    @Override
    public long getTotalBytesWritten() { return totalBytesWritten.get(); }
    @Override
    public double getAverageReadTimeMs() { return averageReadTimeMs; }
    @Override
    public double getAverageWriteTimeMs() { return averageWriteTimeMs; }
    @Override
    public int getActiveStreamingOperations() {
      return (int) (asyncReadsStarted.get() + asyncWritesStarted.get() -
                   asyncReadsCompleted.get() - asyncWritesCompleted.get() -
                   asyncReadsFailed.get() - asyncWritesFailed.get());
    }
    @Override
    public long getPeakStreamingMemoryUsage() { return peakStreamingMemoryUsage; }
  }
}