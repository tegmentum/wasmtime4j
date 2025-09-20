package ai.tegmentum.wasmtime4j.jni.concurrency;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.concurrency.ConcurrentWasmFunction;
import ai.tegmentum.wasmtime4j.concurrency.StoreConcurrencyStatistics;
import ai.tegmentum.wasmtime4j.concurrency.ThreadSafeEngine;
import ai.tegmentum.wasmtime4j.concurrency.ThreadSafeStore;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniConcurrencyManager;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * JNI implementation of a thread-safe WebAssembly store.
 *
 * <p>This implementation wraps a standard JNI store with thread safety guarantees
 * using the JniConcurrencyManager for synchronization.
 *
 * @since 1.0.0
 */
public final class JniThreadSafeStore implements ThreadSafeStore {

  private static final Logger LOGGER = Logger.getLogger(JniThreadSafeStore.class.getName());

  private final Store delegate;
  private final JniConcurrencyManager concurrencyManager;
  private final ReadWriteLock synchronizationLock;
  private final AtomicReference<Object> data;
  private final AtomicInteger currentAccessors;
  private final AtomicLong totalOperations;
  private final long storeHandle;
  private volatile boolean closed;

  /**
   * Creates a new thread-safe store wrapping the provided store.
   *
   * @param delegate the underlying store
   * @param concurrencyManager the concurrency manager for synchronization
   * @throws IllegalArgumentException if delegate or concurrencyManager is null
   */
  public JniThreadSafeStore(final Store delegate, final JniConcurrencyManager concurrencyManager) {
    this.delegate = JniValidation.requireNonNull(delegate, "delegate");
    this.concurrencyManager = JniValidation.requireNonNull(concurrencyManager, "concurrencyManager");
    this.synchronizationLock = new ReentrantReadWriteLock(true); // Fair lock
    this.data = new AtomicReference<>(delegate.getData());
    this.currentAccessors = new AtomicInteger(0);
    this.totalOperations = new AtomicLong(0);
    this.storeHandle = System.identityHashCode(delegate);
    this.closed = false;

    // Register this store with the concurrency manager
    concurrencyManager.registerResource(storeHandle);

    LOGGER.fine("Created JniThreadSafeStore with handle: 0x" + Long.toHexString(storeHandle));
  }

  @Override
  public ThreadSafeEngine getEngine() {
    // This would need to be a ThreadSafeEngine instance
    // For now, wrap the delegate's engine if needed
    return (ThreadSafeEngine) delegate.getEngine();
  }

  @Override
  public Object getData() {
    validateNotClosed();
    return executeWithReadLock(() -> data.get());
  }

  @Override
  public void setData(final Object data) {
    validateNotClosed();
    executeWithWriteLock(() -> {
      this.data.set(data);
      delegate.setData(data);
      return null;
    });
  }

  @Override
  public void setFuel(final long fuel) throws WasmException {
    JniValidation.requireNonNegative(fuel, "fuel");
    validateNotClosed();

    executeWithWriteLock(() -> {
      try {
        delegate.setFuel(fuel);
        return null;
      } catch (final WasmException e) {
        throw new RuntimeException("Failed to set fuel", e);
      }
    });
  }

  @Override
  public long getFuel() throws WasmException {
    validateNotClosed();

    return executeWithReadLock(() -> {
      try {
        return delegate.getFuel();
      } catch (final WasmException e) {
        throw new RuntimeException("Failed to get fuel", e);
      }
    });
  }

  @Override
  public void addFuel(final long fuel) throws WasmException {
    JniValidation.requireNonNegative(fuel, "fuel");
    validateNotClosed();

    executeWithWriteLock(() -> {
      try {
        delegate.addFuel(fuel);
        return null;
      } catch (final WasmException e) {
        throw new RuntimeException("Failed to add fuel", e);
      }
    });
  }

  @Override
  public void setEpochDeadline(final long ticks) throws WasmException {
    validateNotClosed();

    executeWithWriteLock(() -> {
      try {
        delegate.setEpochDeadline(ticks);
        return null;
      } catch (final WasmException e) {
        throw new RuntimeException("Failed to set epoch deadline", e);
      }
    });
  }

  @Override
  public ConcurrentWasmFunction createHostFunction(
      final String name, final FunctionType functionType, final HostFunction implementation)
      throws WasmException {
    JniValidation.requireNonNull(name, "name");
    JniValidation.requireNonNull(functionType, "functionType");
    JniValidation.requireNonNull(implementation, "implementation");
    validateNotClosed();

    return executeWithWriteLock(() -> {
      try {
        final WasmFunction function = delegate.createHostFunction(name, functionType, implementation);
        return new JniConcurrentWasmFunction(function, concurrencyManager);
      } catch (final WasmException e) {
        throw new RuntimeException("Failed to create host function", e);
      }
    });
  }

  @Override
  public boolean compareAndSetFuel(final long expectedFuel, final long newFuel) throws WasmException {
    validateNotClosed();

    return executeWithWriteLock(() -> {
      try {
        final long currentFuel = delegate.getFuel();
        if (currentFuel == expectedFuel) {
          delegate.setFuel(newFuel);
          return true;
        }
        return false;
      } catch (final WasmException e) {
        throw new RuntimeException("Failed to compare and set fuel", e);
      }
    });
  }

  @Override
  public long getAndAddFuel(final long delta) throws WasmException {
    validateNotClosed();

    return executeWithWriteLock(() -> {
      try {
        final long currentFuel = delegate.getFuel();
        final long newFuel = currentFuel + delta;
        delegate.setFuel(newFuel);
        return newFuel;
      } catch (final WasmException e) {
        throw new RuntimeException("Failed to get and add fuel", e);
      }
    });
  }

  @Override
  public long getCurrentEpochTicks() throws WasmException {
    validateNotClosed();

    return executeWithReadLock(() -> {
      // This would need to be implemented in the underlying store
      // For now, return a placeholder
      return System.currentTimeMillis() / 1000; // Approximate epoch ticks
    });
  }

  @Override
  public long incrementEpoch() throws WasmException {
    validateNotClosed();

    return executeWithWriteLock(() -> {
      // This would need to be implemented in the underlying store
      // For now, return current time
      return System.currentTimeMillis() / 1000;
    });
  }

  @Override
  public ReadWriteLock getSynchronizationLock() {
    return synchronizationLock;
  }

  @Override
  public <T> T executeWithReadLock(final Supplier<T> operation) throws WasmException {
    JniValidation.requireNonNull(operation, "operation");
    validateNotClosed();

    return concurrencyManager.executeWithReadLock(storeHandle, () -> {
      currentAccessors.incrementAndGet();
      totalOperations.incrementAndGet();
      try {
        return operation.get();
      } finally {
        currentAccessors.decrementAndGet();
      }
    });
  }

  @Override
  public <T> T executeWithWriteLock(final Supplier<T> operation) throws WasmException {
    JniValidation.requireNonNull(operation, "operation");
    validateNotClosed();

    return concurrencyManager.executeWithWriteLock(storeHandle, () -> {
      currentAccessors.incrementAndGet();
      totalOperations.incrementAndGet();
      try {
        return operation.get();
      } finally {
        currentAccessors.decrementAndGet();
      }
    });
  }

  @Override
  public boolean isThreadSafe() {
    return !closed
        && concurrencyManager != null
        && !concurrencyManager.isClosed()
        && synchronizationLock != null;
  }

  @Override
  public StoreConcurrencyStatistics getConcurrencyStatistics() {
    return new JniStoreConcurrencyStatistics(
        concurrencyManager,
        storeHandle,
        totalOperations.get(),
        currentAccessors.get()
    );
  }

  @Override
  public int getCurrentAccessorCount() {
    return currentAccessors.get();
  }

  @Override
  public long getTotalConcurrentOperations() {
    return totalOperations.get();
  }

  @Override
  public boolean awaitQuiescence(final long timeoutMillis) throws InterruptedException {
    final long startTime = System.currentTimeMillis();
    while (currentAccessors.get() > 0 && (System.currentTimeMillis() - startTime) < timeoutMillis) {
      Thread.sleep(10);
    }
    return currentAccessors.get() == 0;
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;

      // Unregister from concurrency manager
      concurrencyManager.unregisterResource(storeHandle);

      // Close delegate store
      delegate.close();

      LOGGER.fine("JniThreadSafeStore closed with handle: 0x" + Long.toHexString(storeHandle));
    }
  }

  private void validateNotClosed() {
    if (closed) {
      throw new IllegalStateException("Store is closed");
    }
  }
}