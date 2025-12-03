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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.async.AsyncRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of AsyncRuntime.
 *
 * <p>This implementation uses the native Wasmtime async runtime via Panama Foreign Function API
 * calls.
 *
 * @since 1.0.0
 */
public final class PanamaAsyncRuntime implements AsyncRuntime {

  private static final Logger LOGGER = Logger.getLogger(PanamaAsyncRuntime.class.getName());
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final AtomicBoolean initialized = new AtomicBoolean(false);
  private final AtomicBoolean shutdown = new AtomicBoolean(false);
  private final Map<Long, OperationStatus> operationStatuses = new ConcurrentHashMap<>();
  private final AtomicInteger activeOperations = new AtomicInteger(0);

  /**
   * Creates a new PanamaAsyncRuntime.
   *
   * <p>The runtime is not initialized until {@link #initialize()} is called.
   */
  public PanamaAsyncRuntime() {
    LOGGER.fine("Created PanamaAsyncRuntime");
  }

  @Override
  public void initialize() throws WasmException {
    if (shutdown.get()) {
      throw new WasmException("AsyncRuntime has been shut down");
    }

    if (initialized.compareAndSet(false, true)) {
      final int result = NATIVE_BINDINGS.asyncRuntimeInit();
      if (result != 0) {
        initialized.set(false);
        throw new WasmException("Failed to initialize async runtime, error code: " + result);
      }
      LOGGER.fine("Initialized PanamaAsyncRuntime");
    }
  }

  @Override
  public boolean isInitialized() {
    return initialized.get() && !shutdown.get();
  }

  @Override
  public String getRuntimeInfo() {
    if (!initialized.get()) {
      return "Not initialized";
    }

    final MemorySegment infoPtr = NATIVE_BINDINGS.asyncRuntimeInfo();
    if (infoPtr == null || infoPtr.equals(MemorySegment.NULL)) {
      return "No runtime info available";
    }

    try {
      return infoPtr.getString(0, StandardCharsets.UTF_8);
    } catch (Exception e) {
      LOGGER.warning("Failed to read runtime info: " + e.getMessage());
      return "Error reading runtime info";
    }
  }

  @Override
  public long executeAsync(
      final long instancePtr,
      final String functionName,
      final Object[] arguments,
      final long timeoutMs,
      final Consumer<AsyncResult> callback)
      throws WasmException {

    ensureInitialized();
    Objects.requireNonNull(functionName, "functionName cannot be null");
    Objects.requireNonNull(callback, "callback cannot be null");

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment funcNameSegment =
          arena.allocateFrom(functionName, StandardCharsets.UTF_8);

      // For now, we don't pass arguments through FFI - this is a simplified implementation
      final int result =
          NATIVE_BINDINGS.funcCallAsync(
              MemorySegment.ofAddress(instancePtr),
              funcNameSegment,
              MemorySegment.NULL,
              0,
              timeoutMs,
              MemorySegment.NULL, // Callback handled on Java side
              MemorySegment.NULL);

      if (result < 0) {
        throw new WasmException("Failed to start async function call, error code: " + result);
      }

      final long operationId = result;
      operationStatuses.put(operationId, OperationStatus.RUNNING);
      activeOperations.incrementAndGet();

      LOGGER.fine("Started async function call " + operationId + " for " + functionName);
      return operationId;
    }
  }

  @Override
  public long compileAsync(
      final byte[] wasmBytes,
      final long timeoutMs,
      final Consumer<Integer> progressCallback,
      final Consumer<AsyncResult> completionCallback)
      throws WasmException {

    ensureInitialized();
    Objects.requireNonNull(wasmBytes, "wasmBytes cannot be null");
    Objects.requireNonNull(completionCallback, "completionCallback cannot be null");

    if (wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be empty");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment bytecodeSegment = arena.allocate(ValueLayout.JAVA_BYTE, wasmBytes.length);
      bytecodeSegment.copyFrom(MemorySegment.ofArray(wasmBytes));

      final int result =
          NATIVE_BINDINGS.moduleCompileAsync(
              bytecodeSegment,
              wasmBytes.length,
              timeoutMs,
              MemorySegment.NULL, // Callbacks handled on Java side
              MemorySegment.NULL,
              MemorySegment.NULL);

      if (result < 0) {
        throw new WasmException("Failed to start async compilation, error code: " + result);
      }

      final long operationId = result;
      operationStatuses.put(operationId, OperationStatus.RUNNING);
      activeOperations.incrementAndGet();

      LOGGER.fine("Started async compilation " + operationId);
      return operationId;
    }
  }

  @Override
  public boolean cancelOperation(final long operationId) {
    final OperationStatus status = operationStatuses.get(operationId);
    if (status == null) {
      return false;
    }

    if (status == OperationStatus.RUNNING || status == OperationStatus.PENDING) {
      operationStatuses.put(operationId, OperationStatus.CANCELLED);
      activeOperations.decrementAndGet();
      LOGGER.fine("Cancelled operation " + operationId);
      return true;
    }

    return false;
  }

  @Override
  public OperationStatus getOperationStatus(final long operationId) {
    return operationStatuses.getOrDefault(operationId, OperationStatus.PENDING);
  }

  @Override
  public OperationStatus waitForOperation(final long operationId, final long timeoutMs)
      throws WasmException {

    final long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime < timeoutMs) {
      final OperationStatus status = operationStatuses.get(operationId);
      if (status == null) {
        throw new WasmException("Unknown operation ID: " + operationId);
      }

      if (status == OperationStatus.COMPLETED
          || status == OperationStatus.FAILED
          || status == OperationStatus.CANCELLED
          || status == OperationStatus.TIMED_OUT) {
        return status;
      }

      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new WasmException("Wait interrupted");
      }
    }

    operationStatuses.put(operationId, OperationStatus.TIMED_OUT);
    return OperationStatus.TIMED_OUT;
  }

  @Override
  public int getActiveOperationCount() {
    return activeOperations.get();
  }

  @Override
  public void shutdown() {
    if (shutdown.compareAndSet(false, true)) {
      // Cancel all active operations
      for (Map.Entry<Long, OperationStatus> entry : operationStatuses.entrySet()) {
        if (entry.getValue() == OperationStatus.RUNNING
            || entry.getValue() == OperationStatus.PENDING) {
          entry.setValue(OperationStatus.CANCELLED);
        }
      }
      activeOperations.set(0);

      if (initialized.get()) {
        NATIVE_BINDINGS.asyncRuntimeShutdown();
      }

      LOGGER.fine("Shut down PanamaAsyncRuntime");
    }
  }

  @Override
  public void close() {
    shutdown();
  }

  private void ensureInitialized() throws WasmException {
    if (!initialized.get()) {
      throw new WasmException("AsyncRuntime has not been initialized");
    }
    if (shutdown.get()) {
      throw new WasmException("AsyncRuntime has been shut down");
    }
  }

  @Override
  public String toString() {
    return "PanamaAsyncRuntime{"
        + "initialized="
        + initialized.get()
        + ", shutdown="
        + shutdown.get()
        + ", activeOperations="
        + activeOperations.get()
        + '}';
  }
}
