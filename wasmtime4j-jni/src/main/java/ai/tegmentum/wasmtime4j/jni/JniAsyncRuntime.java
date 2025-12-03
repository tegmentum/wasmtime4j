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

import ai.tegmentum.wasmtime4j.async.AsyncRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * JNI implementation of AsyncRuntime.
 *
 * <p>This implementation uses the native Wasmtime async runtime via JNI calls.
 *
 * @since 1.0.0
 */
public final class JniAsyncRuntime implements AsyncRuntime {

  private static final Logger LOGGER = Logger.getLogger(JniAsyncRuntime.class.getName());

  private final AtomicBoolean initialized = new AtomicBoolean(false);
  private final AtomicBoolean shutdown = new AtomicBoolean(false);
  private final Map<Long, OperationStatus> operationStatuses = new ConcurrentHashMap<>();
  private final AtomicInteger activeOperations = new AtomicInteger(0);

  // Native methods
  private static native int nativeAsyncRuntimeInit();

  private static native String nativeAsyncRuntimeInfo();

  private static native int nativeAsyncRuntimeShutdown();

  private static native int nativeFuncCallAsync(
      long instancePtr,
      String functionName,
      Object[] args,
      long timeoutMs,
      Object callback,
      Object userData);

  private static native int nativeModuleCompileAsync(
      byte[] moduleBytes,
      long timeoutMs,
      Object completionCallback,
      Object progressCallback,
      Object userData);

  static {
    NativeLibraryLoader.loadLibrary();
  }

  /**
   * Creates a new JniAsyncRuntime.
   *
   * <p>The runtime is not initialized until {@link #initialize()} is called.
   */
  public JniAsyncRuntime() {
    LOGGER.fine("Created JniAsyncRuntime");
  }

  @Override
  public void initialize() throws WasmException {
    if (shutdown.get()) {
      throw new WasmException("AsyncRuntime has been shut down");
    }

    if (initialized.compareAndSet(false, true)) {
      final int result = nativeAsyncRuntimeInit();
      if (result != 0) {
        initialized.set(false);
        throw new WasmException("Failed to initialize async runtime, error code: " + result);
      }
      LOGGER.fine("Initialized JniAsyncRuntime");
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

    final String info = nativeAsyncRuntimeInfo();
    if (info == null) {
      return "No runtime info available";
    }
    return info;
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

    final int result =
        nativeFuncCallAsync(instancePtr, functionName, arguments, timeoutMs, callback, null);

    if (result < 0) {
      throw new WasmException("Failed to start async function call, error code: " + result);
    }

    final long operationId = result;
    operationStatuses.put(operationId, OperationStatus.RUNNING);
    activeOperations.incrementAndGet();

    LOGGER.fine("Started async function call " + operationId + " for " + functionName);
    return operationId;
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

    final int result =
        nativeModuleCompileAsync(wasmBytes, timeoutMs, completionCallback, progressCallback, null);

    if (result < 0) {
      throw new WasmException("Failed to start async compilation, error code: " + result);
    }

    final long operationId = result;
    operationStatuses.put(operationId, OperationStatus.RUNNING);
    activeOperations.incrementAndGet();

    LOGGER.fine("Started async compilation " + operationId);
    return operationId;
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
        nativeAsyncRuntimeShutdown();
      }

      LOGGER.fine("Shut down JniAsyncRuntime");
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
    return "JniAsyncRuntime{"
        + "initialized="
        + initialized.get()
        + ", shutdown="
        + shutdown.get()
        + ", activeOperations="
        + activeOperations.get()
        + '}';
  }
}
