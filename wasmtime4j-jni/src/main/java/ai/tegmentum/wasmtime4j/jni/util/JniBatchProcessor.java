package ai.tegmentum.wasmtime4j.jni.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Utility for batching JNI calls to improve performance.
 *
 * <p>This class allows batching multiple JNI operations together to reduce the overhead of crossing
 * the JNI boundary. It provides both synchronous and asynchronous batching capabilities.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe batching of JNI operations
 *   <li>Automatic batch size optimization
 *   <li>Configurable batch timeout
 *   <li>Exception handling and error propagation
 * </ul>
 *
 * <p>This utility is designed to improve performance by reducing JNI call overhead while
 * maintaining defensive programming practices and thread safety.
 *
 * @since 1.0.0
 */
public final class JniBatchProcessor implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(JniBatchProcessor.class.getName());

  /** Default maximum batch size. */
  public static final int DEFAULT_MAX_BATCH_SIZE = 100;

  /** Default batch timeout in milliseconds. */
  public static final long DEFAULT_BATCH_TIMEOUT_MS = 10;

  /** Maximum batch size. */
  private final int maxBatchSize;

  /** Batch timeout in milliseconds. */
  private final long batchTimeoutMs;

  /** Queue for pending operations. */
  private final BlockingQueue<BatchOperation<?>> operationQueue;

  /** Flag to track if the processor is closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Background thread for processing batches. */
  private Thread processingThread;

  /** Creates a new batch processor with default settings. */
  public JniBatchProcessor() {
    this(DEFAULT_MAX_BATCH_SIZE, DEFAULT_BATCH_TIMEOUT_MS);
  }

  /**
   * Creates a new batch processor with the specified settings.
   *
   * @param maxBatchSize the maximum number of operations per batch
   * @param batchTimeoutMs the maximum time to wait before processing a partial batch
   * @throws IllegalArgumentException if maxBatchSize is less than 1 or batchTimeoutMs is negative
   */
  public JniBatchProcessor(final int maxBatchSize, final long batchTimeoutMs) {
    JniValidation.requirePositive(maxBatchSize, "maxBatchSize");
    JniValidation.requireNonNegative(batchTimeoutMs, "batchTimeoutMs");

    this.maxBatchSize = maxBatchSize;
    this.batchTimeoutMs = batchTimeoutMs;
    this.operationQueue = new LinkedBlockingQueue<>();

    startProcessingThread();
    LOGGER.fine(
        String.format(
            "Created JniBatchProcessor with maxBatchSize=%d, batchTimeoutMs=%d",
            maxBatchSize, batchTimeoutMs));
  }

  /**
   * Executes an operation, potentially batching it with other operations.
   *
   * @param <T> the return type of the operation
   * @param operation the operation to execute
   * @return the result of the operation
   * @throws RuntimeException if the operation fails or the processor is closed
   * @throws IllegalArgumentException if operation is null
   */
  public <T> T execute(final Supplier<T> operation) {
    JniValidation.requireNonNull(operation, "operation");

    if (closed.get()) {
      throw new RuntimeException("Batch processor is closed");
    }

    final BatchOperation<T> batchOp = new BatchOperation<>(operation);

    try {
      operationQueue.put(batchOp);
      return batchOp.getResult();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Operation interrupted", e);
    }
  }

  /**
   * Gets the current queue size.
   *
   * @return the number of pending operations
   */
  public int getQueueSize() {
    return operationQueue.size();
  }

  /**
   * Gets the maximum batch size.
   *
   * @return the maximum batch size
   */
  public int getMaxBatchSize() {
    return maxBatchSize;
  }

  /**
   * Gets the batch timeout in milliseconds.
   *
   * @return the batch timeout
   */
  public long getBatchTimeoutMs() {
    return batchTimeoutMs;
  }

  /**
   * Checks if this processor is closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed.get();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      LOGGER.fine("Closing JniBatchProcessor");

      // Stop the processing thread
      if (processingThread != null) {
        processingThread.interrupt();
        try {
          processingThread.join(1000); // Wait up to 1 second for thread to finish
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          LOGGER.warning("Interrupted while waiting for processing thread to finish");
        }
      }

      // Process any remaining operations
      processRemainingOperations();

      LOGGER.fine("JniBatchProcessor closed");
    }
  }

  /** Starts the background processing thread. */
  private void startProcessingThread() {
    processingThread = new Thread(this::processOperations, "JniBatchProcessor");
    processingThread.setDaemon(true);
    processingThread.start();
  }

  /** Main processing loop for batching operations. */
  private void processOperations() {
    final List<BatchOperation<?>> batch = new ArrayList<>();

    while (!closed.get() && !Thread.currentThread().isInterrupted()) {
      try {
        // Wait for the first operation
        final BatchOperation<?> firstOp =
            operationQueue.poll(batchTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (firstOp == null) {
          continue; // Timeout, check for shutdown
        }

        batch.clear();
        batch.add(firstOp);

        // Collect additional operations up to batch size
        final long batchStartTime = System.currentTimeMillis();
        while (batch.size() < maxBatchSize && !closed.get()) {
          final long timeRemaining = batchTimeoutMs - (System.currentTimeMillis() - batchStartTime);
          if (timeRemaining <= 0) {
            break;
          }

          final BatchOperation<?> op =
              operationQueue.poll(timeRemaining, java.util.concurrent.TimeUnit.MILLISECONDS);
          if (op == null) {
            break; // Timeout
          }
          batch.add(op);
        }

        // Process the batch
        processBatch(batch);

      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (final Exception e) {
        LOGGER.warning("Error in batch processing: " + e.getMessage());
      }
    }
  }

  /**
   * Processes a batch of operations.
   *
   * @param batch the operations to process
   */
  private void processBatch(final List<BatchOperation<?>> batch) {
    if (batch.isEmpty()) {
      return;
    }

    LOGGER.fine("Processing batch of " + batch.size() + " operations");

    // Execute all operations in the batch
    for (final BatchOperation<?> operation : batch) {
      try {
        operation.execute();
      } catch (final Exception e) {
        operation.setException(e);
      }
    }
  }

  /** Processes any remaining operations during shutdown. */
  private void processRemainingOperations() {
    final List<BatchOperation<?>> remaining = new ArrayList<>();
    operationQueue.drainTo(remaining);

    if (!remaining.isEmpty()) {
      LOGGER.fine("Processing " + remaining.size() + " remaining operations");
      for (final BatchOperation<?> operation : remaining) {
        try {
          operation.execute();
        } catch (final Exception e) {
          operation.setException(e);
        }
      }
    }
  }

  /**
   * Represents a batched operation.
   *
   * @param <T> the return type of the operation
   */
  private static final class BatchOperation<T> {
    private final Supplier<T> operation;
    private volatile T result;
    private volatile Exception exception;
    private final Object resultLock = new Object();
    private volatile boolean completed = false;

    BatchOperation(final Supplier<T> operation) {
      this.operation = operation;
    }

    void execute() {
      try {
        final T operationResult = operation.get();
        setResult(operationResult);
      } catch (final Exception e) {
        setException(e);
      }
    }

    void setResult(final T result) {
      synchronized (resultLock) {
        this.result = result;
        this.completed = true;
        resultLock.notifyAll();
      }
    }

    void setException(final Exception exception) {
      synchronized (resultLock) {
        this.exception = exception;
        this.completed = true;
        resultLock.notifyAll();
      }
    }

    T getResult() throws RuntimeException {
      synchronized (resultLock) {
        while (!completed) {
          try {
            resultLock.wait();
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operation interrupted", e);
          }
        }

        if (exception != null) {
          if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
          } else {
            throw new RuntimeException("Operation failed", exception);
          }
        }

        return result;
      }
    }
  }
}
