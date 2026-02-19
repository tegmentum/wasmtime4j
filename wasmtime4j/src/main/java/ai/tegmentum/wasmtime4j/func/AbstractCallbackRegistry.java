package ai.tegmentum.wasmtime4j.func;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base implementation of {@link CallbackRegistry} providing shared callback management
 * logic for both JNI and Panama implementations.
 *
 * <p>This class implements the common callback lifecycle operations (registration, invocation,
 * unregistration, metrics) while delegating implementation-specific concerns to subclasses via
 * template methods.
 *
 * <p>Subclasses must implement:
 *
 * <ul>
 *   <li>{@link #createFunctionReferenceForCallback} — creates the runtime-specific function
 *       reference
 *   <li>{@link #closeFunctionReference} — closes a runtime-specific function reference
 *   <li>{@link #getAsyncExecutor} — provides the executor for async callbacks
 *   <li>{@link #ensureNotClosed} — validates the registry is still open
 *   <li>{@link #close} — performs runtime-specific cleanup
 * </ul>
 *
 * @since 1.0.0
 */
public abstract class AbstractCallbackRegistry implements CallbackRegistry {

  private static final Logger LOGGER = Logger.getLogger(AbstractCallbackRegistry.class.getName());

  /** Default timeout for async callbacks in milliseconds. */
  protected static final long DEFAULT_ASYNC_TIMEOUT_MILLIS = 30_000;

  /** Map of callback ID to callback entry. */
  protected final ConcurrentHashMap<Long, CallbackEntry> callbacks = new ConcurrentHashMap<>();

  /** ID generator for callbacks. */
  protected final AtomicLong nextCallbackId = new AtomicLong(1L);

  /** Metrics collector for callback performance. */
  protected final CallbackMetricsImpl metrics = new CallbackMetricsImpl();

  /**
   * Creates a runtime-specific function reference for a callback.
   *
   * @param callback the host function callback
   * @param functionType the function type signature
   * @return the created function reference
   * @throws WasmException if creation fails
   */
  protected abstract FunctionReference createFunctionReferenceForCallback(
      final HostFunction callback, final FunctionType functionType) throws WasmException;

  /**
   * Closes a runtime-specific function reference, releasing native resources.
   *
   * @param ref the function reference to close
   */
  protected abstract void closeFunctionReference(final FunctionReference ref);

  /**
   * Gets the executor service for async callback execution.
   *
   * @return the scheduled executor service
   */
  protected abstract ScheduledExecutorService getAsyncExecutor();

  /**
   * Ensures the registry is not closed.
   *
   * @throws IllegalStateException if the registry is closed
   */
  protected abstract void ensureNotClosed();

  @Override
  public CallbackHandle registerCallback(
      final String name, final HostFunction callback, final FunctionType functionType)
      throws WasmException {
    ensureNotClosed();
    validateCallbackName(name, "register callback");
    validateCallbackNotNull(callback, "register callback");
    validateFunctionType(functionType, "register callback");

    final long callbackId = nextCallbackId.getAndIncrement();
    final CallbackHandleImpl handle = new CallbackHandleImpl(callbackId, name, functionType);

    try {
      final FunctionReference functionReference =
          createFunctionReferenceForCallback(callback, functionType);

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
    validateCallbackName(name, "register async callback");
    validateAsyncCallbackNotNull(callback, "register async callback");
    validateFunctionType(functionType, "register async callback");

    final long callbackId = nextCallbackId.getAndIncrement();
    final AsyncCallbackHandleImpl handle =
        new AsyncCallbackHandleImpl(callbackId, name, functionType, DEFAULT_ASYNC_TIMEOUT_MILLIS);

    try {
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

      final FunctionReference functionReference =
          createFunctionReferenceForCallback(syncWrapper, functionType);

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

    return entry.getFunctionReference();
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
      if (entry.getHandle() instanceof CallbackHandleImpl) {
        ((CallbackHandleImpl) entry.getHandle()).invalidate();
      }

      closeFunctionReference(entry.getFunctionReference());

      if (LOGGER.isLoggable(Level.FINE)) {
        LOGGER.fine(
            "Unregistered callback '" + handle.getName() + "' with ID: " + handle.getId());
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

    if (!handle.isValid()) {
      throw new WasmException("Callback handle is no longer valid: " + handle.getName());
    }

    final CallbackEntry entry = callbacks.get(handle.getId());
    if (entry == null) {
      throw new WasmException("Callback not found: " + handle.getName());
    }

    final long startTime = System.nanoTime();
    try {
      final WasmValue[] result = entry.getSyncCallback().execute(params);
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
            final CompletableFuture<WasmValue[]> future =
                entry.getAsyncCallback().executeAsync(params);
            final WasmValue[] result =
                future.get(handle.getTimeoutMillis(), TimeUnit.MILLISECONDS);
            final long executionTime = System.nanoTime() - startTime;
            metrics.recordInvocation(executionTime);
            return result;

          } catch (TimeoutException e) {
            metrics.recordTimeout();
            throw new RuntimeException(
                "Async callback timed out after " + handle.getTimeoutMillis() + "ms", e);

          } catch (Exception e) {
            final long executionTime = System.nanoTime() - startTime;
            metrics.recordFailure(executionTime);
            throw new RuntimeException("Async callback execution failed", e);
          }
        },
        getAsyncExecutor());
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
    return callbacks.values().stream()
        .anyMatch(entry -> name.equals(entry.getHandle().getName()));
  }

  private void validateCallbackName(final String name, final String operation) throws WasmException {
    if (name == null) {
      throw new WasmException("Failed to " + operation + ": Callback name cannot be null");
    }
  }

  private void validateCallbackNotNull(final HostFunction callback, final String operation)
      throws WasmException {
    if (callback == null) {
      throw new WasmException("Failed to " + operation + ": Callback cannot be null");
    }
  }

  private void validateAsyncCallbackNotNull(
      final AsyncHostFunction callback, final String operation) throws WasmException {
    if (callback == null) {
      throw new WasmException("Failed to " + operation + ": Callback cannot be null");
    }
  }

  private void validateFunctionType(final FunctionType functionType, final String operation)
      throws WasmException {
    if (functionType == null) {
      throw new WasmException("Failed to " + operation + ": Function type cannot be null");
    }
  }
}
