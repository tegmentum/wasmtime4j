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

package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.func.CallbackRegistry;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of the callback registry for managing callbacks and asynchronous operations.
 *
 * <p>This registry provides centralized management of callback functions with thread safety,
 * resource cleanup, and performance monitoring. It supports both synchronous and asynchronous
 * callbacks with comprehensive error handling.
 *
 * <p>Key Features:
 *
 * <ul>
 *   <li>Thread-safe callback registration and invocation
 *   <li>Asynchronous callback support with timeouts
 *   <li>Automatic resource cleanup and lifecycle management
 *   <li>Performance metrics collection
 *   <li>Comprehensive error handling and recovery
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniCallbackRegistry implements CallbackRegistry {
  private static final Logger LOGGER = Logger.getLogger(JniCallbackRegistry.class.getName());
  private static final long DEFAULT_ASYNC_TIMEOUT_MILLIS = 30_000; // 30 seconds

  private final WeakReference<JniStore> storeRef;
  private final ConcurrentHashMap<Long, CallbackEntry> callbacks = new ConcurrentHashMap<>();
  private final AtomicLong nextCallbackId = new AtomicLong(1L);
  private final ScheduledExecutorService asyncExecutor;
  private final CallbackMetricsImpl metrics = new CallbackMetricsImpl();
  private volatile boolean closed = false;

  /** Internal callback entry for managing callback state. */
  private static class CallbackEntry {
    final CallbackHandle handle;
    final HostFunction syncCallback;
    final AsyncHostFunction asyncCallback;
    final FunctionReference functionReference;

    CallbackEntry(
        final CallbackHandle handle,
        final HostFunction syncCallback,
        final AsyncHostFunction asyncCallback,
        final FunctionReference functionReference) {
      this.handle = handle;
      this.syncCallback = syncCallback;
      this.asyncCallback = asyncCallback;
      this.functionReference = functionReference;
    }

    boolean isAsync() {
      return asyncCallback != null;
    }
  }

  /**
   * Creates a new callback registry for the given store.
   *
   * @param store the store this registry belongs to
   */
  JniCallbackRegistry(final JniStore store) {
    this.storeRef = new WeakReference<>(Objects.requireNonNull(store, "Store cannot be null"));
    this.asyncExecutor =
        Executors.newScheduledThreadPool(
            4,
            r -> {
              final Thread t = new Thread(r, "wasmtime4j-async-callback");
              t.setDaemon(true);
              return t;
            });

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Created callback registry for store");
    }
  }

  @Override
  public CallbackHandle registerCallback(
      final String name, final HostFunction callback, final FunctionType functionType)
      throws WasmException {
    ensureNotClosed();
    if (name == null) {
      throw new WasmException("Failed to register callback: Callback name cannot be null");
    }
    if (callback == null) {
      throw new WasmException("Failed to register callback: Callback cannot be null");
    }
    if (functionType == null) {
      throw new WasmException("Failed to register callback: Function type cannot be null");
    }

    final long callbackId = nextCallbackId.getAndIncrement();
    final CallbackHandleImpl handle = new CallbackHandleImpl(callbackId, name, functionType);

    try {
      // Create function reference for the callback
      final JniStore store = getStore();
      final FunctionReference functionReference =
          new JniFunctionReference(callback, functionType, store);

      // Register the callback
      final CallbackEntry entry = new CallbackEntry(handle, callback, null, functionReference);
      callbacks.put(callbackId, entry);

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine("Registered callback '" + name + "' with ID: " + callbackId);
      }

      return handle;

    } catch (Exception e) {
      callbacks.remove(callbackId);
      throw new WasmException("Failed to register callback: " + name, e);
    }
  }

  @Override
  public AsyncCallbackHandle registerAsyncCallback(
      final String name, final AsyncHostFunction callback, final FunctionType functionType)
      throws WasmException {
    ensureNotClosed();
    if (name == null) {
      throw new WasmException("Failed to register async callback: Callback name cannot be null");
    }
    if (callback == null) {
      throw new WasmException("Failed to register async callback: Callback cannot be null");
    }
    if (functionType == null) {
      throw new WasmException("Failed to register async callback: Function type cannot be null");
    }

    final long callbackId = nextCallbackId.getAndIncrement();
    final AsyncCallbackHandleImpl handle =
        new AsyncCallbackHandleImpl(callbackId, name, functionType, DEFAULT_ASYNC_TIMEOUT_MILLIS);

    try {
      // Create a wrapper host function that handles async execution
      final HostFunction syncWrapper =
          (params) -> {
            try {
              final CompletableFuture<WasmValue[]> future = callback.executeAsync(params);
              return future.get(handle.getTimeoutMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
              metrics.recordTimeout();
              throw new WasmException(
                  "Async callback timed out after " + handle.getTimeoutMillis() + "ms", e);
            } catch (Exception e) {
              throw new WasmException("Async callback execution failed", e);
            }
          };

      // Create function reference for the wrapper
      final JniStore store = getStore();
      final FunctionReference functionReference =
          new JniFunctionReference(syncWrapper, functionType, store);

      // Register the callback
      final CallbackEntry entry =
          new CallbackEntry(handle, syncWrapper, callback, functionReference);
      callbacks.put(callbackId, entry);

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine("Registered async callback '" + name + "' with ID: " + callbackId);
      }

      return handle;

    } catch (Exception e) {
      callbacks.remove(callbackId);
      throw new WasmException("Failed to register async callback: " + name, e);
    }
  }

  @Override
  public FunctionReference createFunctionReference(final CallbackHandle handle)
      throws WasmException {
    ensureNotClosed();
    Objects.requireNonNull(handle, "Callback handle cannot be null");

    final CallbackEntry entry = callbacks.get(handle.getId());
    if (entry == null) {
      throw new WasmException("Callback not found: " + handle.getName());
    }

    return entry.functionReference;
  }

  @Override
  public void unregisterCallback(final CallbackHandle handle) throws WasmException {
    ensureNotClosed();
    Objects.requireNonNull(handle, "Callback handle cannot be null");

    final CallbackEntry entry = callbacks.remove(handle.getId());
    if (entry == null) {
      LOGGER.warning("Attempted to unregister unknown callback: " + handle.getName());
      return;
    }

    try {
      // Invalidate the handle to prevent further use
      if (entry.handle instanceof CallbackHandleImpl) {
        final CallbackHandleImpl handleImpl = (CallbackHandleImpl) entry.handle;
        handleImpl.invalidate();
      }

      // Close the function reference to clean up native resources
      if (entry.functionReference instanceof JniFunctionReference) {
        final JniFunctionReference jniFuncRef = (JniFunctionReference) entry.functionReference;
        jniFuncRef.close();
      }

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine("Unregistered callback '" + handle.getName() + "' with ID: " + handle.getId());
      }

    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error cleaning up callback: " + handle.getName(), e);
      throw new WasmException("Failed to unregister callback: " + handle.getName(), e);
    }
  }

  @Override
  public WasmValue[] invokeCallback(final CallbackHandle handle, final WasmValue... params)
      throws WasmException {
    ensureNotClosed();
    Objects.requireNonNull(handle, "Callback handle cannot be null");
    Objects.requireNonNull(params, "Parameters cannot be null");

    // Check if handle is still valid
    if (!handle.isValid()) {
      throw new WasmException("Callback handle is no longer valid: " + handle.getName());
    }

    final CallbackEntry entry = callbacks.get(handle.getId());
    if (entry == null) {
      throw new WasmException("Callback not found: " + handle.getName());
    }

    final long startTime = System.nanoTime();
    try {
      final WasmValue[] result = entry.syncCallback.execute(params);
      final long executionTime = System.nanoTime() - startTime;
      metrics.recordInvocation(executionTime);
      return result;

    } catch (Exception e) {
      final long executionTime = System.nanoTime() - startTime;
      metrics.recordFailure(executionTime);
      throw new WasmException("Callback invocation failed: " + handle.getName(), e);
    }
  }

  @Override
  public CompletableFuture<WasmValue[]> invokeAsyncCallback(
      final AsyncCallbackHandle handle, final WasmValue... params) throws WasmException {
    ensureNotClosed();
    Objects.requireNonNull(handle, "Callback handle cannot be null");
    Objects.requireNonNull(params, "Parameters cannot be null");

    // Check if handle is still valid
    if (!handle.isValid()) {
      throw new WasmException("Async callback handle is no longer valid: " + handle.getName());
    }

    final CallbackEntry entry = callbacks.get(handle.getId());
    if (entry == null) {
      throw new WasmException("Async callback not found: " + handle.getName());
    }

    if (!entry.isAsync()) {
      throw new WasmException("Callback is not asynchronous: " + handle.getName());
    }

    return CompletableFuture.supplyAsync(
        () -> {
          final long startTime = System.nanoTime();
          try {
            final CompletableFuture<WasmValue[]> future = entry.asyncCallback.executeAsync(params);
            final WasmValue[] result = future.get(handle.getTimeoutMillis(), TimeUnit.MILLISECONDS);
            final long executionTime = System.nanoTime() - startTime;
            metrics.recordInvocation(executionTime);
            return result;

          } catch (TimeoutException e) {
            final long executionTime = System.nanoTime() - startTime;
            metrics.recordTimeout();
            throw new RuntimeException(
                "Async callback timed out after " + handle.getTimeoutMillis() + "ms", e);

          } catch (Exception e) {
            final long executionTime = System.nanoTime() - startTime;
            metrics.recordFailure(executionTime);
            throw new RuntimeException("Async callback execution failed", e);
          }
        },
        asyncExecutor);
  }

  @Override
  public CallbackMetrics getMetrics() {
    return metrics;
  }

  @Override
  public int getCallbackCount() {
    return callbacks.size();
  }

  @Override
  public boolean hasCallback(final String name) {
    Objects.requireNonNull(name, "Callback name cannot be null");
    return callbacks.values().stream().anyMatch(entry -> name.equals(entry.handle.getName()));
  }

  @Override
  public void close() throws WasmException {
    if (closed) {
      return;
    }

    synchronized (this) {
      if (closed) {
        return;
      }

      try {
        // Unregister all callbacks
        for (final CallbackEntry entry : callbacks.values()) {
          try {
            if (entry.functionReference instanceof JniFunctionReference) {
              final JniFunctionReference jniFuncRef =
                  (JniFunctionReference) entry.functionReference;
              jniFuncRef.close();
            }
          } catch (Exception e) {
            LOGGER.log(
                Level.WARNING,
                "Error closing callback function reference: " + entry.handle.getName(),
                e);
          }
        }
        callbacks.clear();

        // Shutdown async executor
        asyncExecutor.shutdown();
        try {
          if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            asyncExecutor.shutdownNow();
          }
        } catch (InterruptedException e) {
          asyncExecutor.shutdownNow();
          Thread.currentThread().interrupt();
        }

        if (LOGGER.isLoggable(Level.FINE)) {
          LOGGER.fine("Closed callback registry");
        }

      } catch (Exception e) {
        throw new WasmException("Failed to close callback registry", e);
      } finally {
        closed = true;
      }
    }
  }

  /**
   * Gets the store associated with this registry.
   *
   * @return the store
   * @throws WasmException if the store is no longer available
   */
  private JniStore getStore() throws WasmException {
    final JniStore store = storeRef.get();
    if (store == null) {
      throw new WasmException("Store is no longer available");
    }
    return store;
  }

  /**
   * Ensures the registry is not closed.
   *
   * @throws IllegalStateException if the registry is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Callback registry has been closed");
    }
  }

  /** Implementation of CallbackHandle. */
  private static class CallbackHandleImpl implements CallbackHandle {
    private final long id;
    private final String name;
    private final FunctionType functionType;
    private volatile boolean valid = true;

    CallbackHandleImpl(final long id, final String name, final FunctionType functionType) {
      this.id = id;
      this.name = name;
      this.functionType = functionType;
    }

    @Override
    public long getId() {
      return id;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public FunctionType getFunctionType() {
      return functionType;
    }

    @Override
    public boolean isValid() {
      return valid;
    }

    void invalidate() {
      this.valid = false;
    }
  }

  /** Implementation of AsyncCallbackHandle. */
  private static class AsyncCallbackHandleImpl extends CallbackHandleImpl
      implements AsyncCallbackHandle {
    private volatile long timeoutMillis;

    AsyncCallbackHandleImpl(
        final long id,
        final String name,
        final FunctionType functionType,
        final long timeoutMillis) {
      super(id, name, functionType);
      this.timeoutMillis = timeoutMillis;
    }

    @Override
    public long getTimeoutMillis() {
      return timeoutMillis;
    }

    @Override
    public void setTimeoutMillis(final long timeoutMillis) {
      if (timeoutMillis <= 0) {
        throw new IllegalArgumentException("Timeout must be positive");
      }
      this.timeoutMillis = timeoutMillis;
    }
  }

  /** Implementation of CallbackMetrics. */
  private static class CallbackMetricsImpl implements CallbackMetrics {
    private final AtomicLong totalInvocations = new AtomicLong(0);
    private final AtomicLong totalExecutionTimeNanos = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong timeoutCount = new AtomicLong(0);

    void recordInvocation(final long executionTimeNanos) {
      totalInvocations.incrementAndGet();
      totalExecutionTimeNanos.addAndGet(executionTimeNanos);
    }

    void recordFailure(final long executionTimeNanos) {
      totalInvocations.incrementAndGet();
      totalExecutionTimeNanos.addAndGet(executionTimeNanos);
      failureCount.incrementAndGet();
    }

    void recordTimeout() {
      timeoutCount.incrementAndGet();
    }

    @Override
    public long getTotalInvocations() {
      return totalInvocations.get();
    }

    @Override
    public double getAverageExecutionTimeNanos() {
      final long invocations = totalInvocations.get();
      return invocations > 0 ? (double) totalExecutionTimeNanos.get() / invocations : 0.0;
    }

    @Override
    public long getTotalExecutionTimeNanos() {
      return totalExecutionTimeNanos.get();
    }

    @Override
    public long getFailureCount() {
      return failureCount.get();
    }

    @Override
    public long getTimeoutCount() {
      return timeoutCount.get();
    }
  }
}
