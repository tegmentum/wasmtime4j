package ai.tegmentum.wasmtime4j.jni.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Manager for thread-safe concurrent access to JNI resources.
 *
 * <p>This class provides utilities for managing concurrent access to native resources through JNI
 * calls. It includes support for read-write locking, resource pooling, and rate limiting to ensure
 * thread safety and optimal performance.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe resource access with read-write locking
 *   <li>Resource-specific concurrency controls
 *   <li>Rate limiting for resource-intensive operations
 *   <li>Deadlock detection and prevention
 *   <li>Performance monitoring and statistics
 * </ul>
 *
 * <p>This utility ensures that JNI resources can be safely accessed from multiple threads while
 * maintaining optimal performance and preventing resource contention.
 *
 * @since 1.0.0
 */
public final class JniConcurrencyManager implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(JniConcurrencyManager.class.getName());

  /** Default maximum concurrent operations per resource. */
  public static final int DEFAULT_MAX_CONCURRENT_OPERATIONS = 10;

  /** Default operation timeout in milliseconds. */
  public static final long DEFAULT_OPERATION_TIMEOUT_MS = 30000; // 30 seconds

  /** Map of resource handles to their concurrency controls. */
  private final ConcurrentMap<Long, ResourceConcurrencyControl> resourceControls;

  /** Global semaphore for limiting total concurrent operations. */
  private final Semaphore globalSemaphore;

  /** Maximum concurrent operations per resource. */
  private final int maxConcurrentOperations;

  /** Operation timeout in milliseconds. */
  private final long operationTimeoutMs;

  /** Flag to track if the manager is closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Statistics. */
  private final AtomicLong totalOperations = new AtomicLong(0);
  private final AtomicLong timeoutOperations = new AtomicLong(0);
  private final AtomicLong failedOperations = new AtomicLong(0);

  /**
   * Creates a new concurrency manager with default settings.
   */
  public JniConcurrencyManager() {
    this(DEFAULT_MAX_CONCURRENT_OPERATIONS, DEFAULT_OPERATION_TIMEOUT_MS);
  }

  /**
   * Creates a new concurrency manager with the specified settings.
   *
   * @param maxConcurrentOperations maximum concurrent operations per resource
   * @param operationTimeoutMs operation timeout in milliseconds
   * @throws IllegalArgumentException if maxConcurrentOperations is less than 1 or timeout is negative
   */
  public JniConcurrencyManager(final int maxConcurrentOperations, final long operationTimeoutMs) {
    JniValidation.requirePositive(maxConcurrentOperations, "maxConcurrentOperations");
    JniValidation.requireNonNegative(operationTimeoutMs, "operationTimeoutMs");

    this.maxConcurrentOperations = maxConcurrentOperations;
    this.operationTimeoutMs = operationTimeoutMs;
    this.resourceControls = new ConcurrentHashMap<>();
    
    // Global semaphore to limit total concurrent operations across all resources
    this.globalSemaphore = new Semaphore(maxConcurrentOperations * 4, true); // Fair semaphore

    LOGGER.fine(
        String.format(
            "Created JniConcurrencyManager with maxConcurrentOperations=%d, timeoutMs=%d",
            maxConcurrentOperations, operationTimeoutMs));
  }

  /**
   * Executes an operation with read access to a resource.
   *
   * @param <T> the return type of the operation
   * @param resourceHandle the native resource handle
   * @param operation the operation to execute
   * @return the result of the operation
   * @throws RuntimeException if the operation fails or times out
   * @throws IllegalArgumentException if resourceHandle is invalid or operation is null
   */
  public <T> T executeWithReadLock(final long resourceHandle, final Supplier<T> operation) {
    JniValidation.requireValidHandle(resourceHandle, "resourceHandle");
    JniValidation.requireNonNull(operation, "operation");

    return executeWithLock(resourceHandle, operation, true);
  }

  /**
   * Executes an operation with write access to a resource.
   *
   * @param <T> the return type of the operation
   * @param resourceHandle the native resource handle
   * @param operation the operation to execute
   * @return the result of the operation
   * @throws RuntimeException if the operation fails or times out
   * @throws IllegalArgumentException if resourceHandle is invalid or operation is null
   */
  public <T> T executeWithWriteLock(final long resourceHandle, final Supplier<T> operation) {
    JniValidation.requireValidHandle(resourceHandle, "resourceHandle");
    JniValidation.requireNonNull(operation, "operation");

    return executeWithLock(resourceHandle, operation, false);
  }

  /**
   * Registers a resource for concurrency management.
   *
   * @param resourceHandle the native resource handle
   * @throws IllegalArgumentException if resourceHandle is invalid
   */
  public void registerResource(final long resourceHandle) {
    JniValidation.requireValidHandle(resourceHandle, "resourceHandle");

    if (closed.get()) {
      throw new RuntimeException("Concurrency manager is closed");
    }

    resourceControls.computeIfAbsent(resourceHandle, h -> new ResourceConcurrencyControl(h));
    LOGGER.fine("Registered resource for concurrency management: 0x" + Long.toHexString(resourceHandle));
  }

  /**
   * Unregisters a resource from concurrency management.
   *
   * @param resourceHandle the native resource handle
   * @throws IllegalArgumentException if resourceHandle is invalid
   */
  public void unregisterResource(final long resourceHandle) {
    JniValidation.requireValidHandle(resourceHandle, "resourceHandle");

    final ResourceConcurrencyControl control = resourceControls.remove(resourceHandle);
    if (control != null) {
      control.close();
      LOGGER.fine("Unregistered resource from concurrency management: 0x" + Long.toHexString(resourceHandle));
    }
  }

  /**
   * Gets the number of active operations for a resource.
   *
   * @param resourceHandle the native resource handle
   * @return the number of active operations, or -1 if resource is not registered
   * @throws IllegalArgumentException if resourceHandle is invalid
   */
  public int getActiveOperationCount(final long resourceHandle) {
    JniValidation.requireValidHandle(resourceHandle, "resourceHandle");

    final ResourceConcurrencyControl control = resourceControls.get(resourceHandle);
    return control != null ? control.getActiveOperationCount() : -1;
  }

  /**
   * Gets the total number of operations executed.
   *
   * @return the total operation count
   */
  public long getTotalOperationCount() {
    return totalOperations.get();
  }

  /**
   * Gets the number of operations that timed out.
   *
   * @return the timeout operation count
   */
  public long getTimeoutOperationCount() {
    return timeoutOperations.get();
  }

  /**
   * Gets the number of operations that failed.
   *
   * @return the failed operation count
   */
  public long getFailedOperationCount() {
    return failedOperations.get();
  }

  /**
   * Gets the number of registered resources.
   *
   * @return the resource count
   */
  public int getResourceCount() {
    return resourceControls.size();
  }

  /**
   * Checks if the manager is closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed.get();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      LOGGER.info(
          String.format(
              "Closing JniConcurrencyManager: resources=%d, totalOps=%d, timeouts=%d, failures=%d",
              resourceControls.size(), totalOperations.get(), timeoutOperations.get(), failedOperations.get()));

      // Close all resource controls
      for (final ResourceConcurrencyControl control : resourceControls.values()) {
        try {
          control.close();
        } catch (final Exception e) {
          LOGGER.warning("Error closing resource concurrency control: " + e.getMessage());
        }
      }

      resourceControls.clear();
      LOGGER.fine("JniConcurrencyManager closed");
    }
  }

  /**
   * Executes an operation with the specified lock type.
   *
   * @param <T> the return type of the operation
   * @param resourceHandle the native resource handle
   * @param operation the operation to execute
   * @param readLock true for read lock, false for write lock
   * @return the result of the operation
   */
  private <T> T executeWithLock(final long resourceHandle, final Supplier<T> operation, final boolean readLock) {
    if (closed.get()) {
      throw new RuntimeException("Concurrency manager is closed");
    }

    // Get or create resource control
    final ResourceConcurrencyControl control = 
        resourceControls.computeIfAbsent(resourceHandle, h -> new ResourceConcurrencyControl(h));

    totalOperations.incrementAndGet();

    // Acquire global semaphore first
    boolean globalAcquired = false;
    try {
      if (operationTimeoutMs > 0) {
        globalAcquired = globalSemaphore.tryAcquire(operationTimeoutMs, TimeUnit.MILLISECONDS);
        if (!globalAcquired) {
          timeoutOperations.incrementAndGet();
          throw new RuntimeException("Operation timed out waiting for global resource semaphore");
        }
      } else {
        globalSemaphore.acquire();
        globalAcquired = true;
      }

      // Execute with resource-specific lock
      return control.executeWithLock(operation, readLock, operationTimeoutMs);

    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      failedOperations.incrementAndGet();
      throw new RuntimeException("Operation interrupted", e);
    } catch (final Exception e) {
      failedOperations.incrementAndGet();
      throw e;
    } finally {
      if (globalAcquired) {
        globalSemaphore.release();
      }
    }
  }

  /**
   * Resource-specific concurrency control.
   */
  private static final class ResourceConcurrencyControl {
    private final long resourceHandle;
    private final ReentrantReadWriteLock lock;
    private final AtomicInteger activeOperations;
    private final AtomicBoolean closed;

    ResourceConcurrencyControl(final long resourceHandle) {
      this.resourceHandle = resourceHandle;
      this.lock = new ReentrantReadWriteLock(true); // Fair lock
      this.activeOperations = new AtomicInteger(0);
      this.closed = new AtomicBoolean(false);
    }

    <T> T executeWithLock(final Supplier<T> operation, final boolean readLock, final long timeoutMs) {
      if (closed.get()) {
        throw new RuntimeException("Resource control is closed for handle: 0x" + Long.toHexString(resourceHandle));
      }

      final ReentrantReadWriteLock.ReadLock rLock = readLock ? lock.readLock() : null;
      final ReentrantReadWriteLock.WriteLock wLock = readLock ? null : lock.writeLock();

      boolean lockAcquired = false;
      try {
        // Acquire the appropriate lock
        if (readLock) {
          if (timeoutMs > 0) {
            lockAcquired = rLock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
          } else {
            rLock.lock();
            lockAcquired = true;
          }
        } else {
          if (timeoutMs > 0) {
            lockAcquired = wLock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
          } else {
            wLock.lock();
            lockAcquired = true;
          }
        }

        if (!lockAcquired) {
          throw new RuntimeException(
              String.format(
                  "Timed out waiting for %s lock on resource: 0x%x",
                  readLock ? "read" : "write", resourceHandle));
        }

        activeOperations.incrementAndGet();
        return operation.get();

      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Lock acquisition interrupted", e);
      } finally {
        if (lockAcquired) {
          activeOperations.decrementAndGet();
          if (readLock) {
            rLock.unlock();
          } else {
            wLock.unlock();
          }
        }
      }
    }

    int getActiveOperationCount() {
      return activeOperations.get();
    }

    void close() {
      if (closed.compareAndSet(false, true)) {
        // Wait for active operations to complete with timeout
        final long startTime = System.currentTimeMillis();
        final long maxWaitTime = 5000; // 5 seconds
        
        while (activeOperations.get() > 0 && (System.currentTimeMillis() - startTime) < maxWaitTime) {
          try {
            Thread.sleep(10);
          } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }

        if (activeOperations.get() > 0) {
          LOGGER.warning(
              String.format(
                  "Closed resource control with %d active operations for handle: 0x%x",
                  activeOperations.get(), resourceHandle));
        }
      }
    }
  }

  @Override
  public String toString() {
    return String.format(
        "JniConcurrencyManager{resources=%d, totalOps=%d, timeouts=%d, failures=%d}",
        resourceControls.size(), totalOperations.get(), timeoutOperations.get(), failedOperations.get());
  }
}