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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstancePre;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
import ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of {@link InstancePre}.
 *
 * <p>This class wraps a native pre-instantiated module handle and provides fast instantiation
 * capabilities. Pre-instantiation performs expensive setup work once, allowing subsequent instance
 * creation to be significantly faster.
 *
 * @since 1.0.0
 */
public final class JniInstancePre implements InstancePre {

  private static final Logger LOGGER = Logger.getLogger(JniInstancePre.class.getName());

  private final long nativeHandle;
  private final Module module;
  private final Engine engine;
  private final Instant creationTime;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new JniInstancePre with the given native handle.
   *
   * @param nativeHandle the native handle to the pre-instantiated module
   * @param module the module that was pre-instantiated
   * @param engine the engine used for pre-instantiation
   * @throws IllegalArgumentException if nativeHandle is 0 or module/engine is null
   */
  public JniInstancePre(final long nativeHandle, final Module module, final Engine engine) {
    if (nativeHandle == 0) {
      throw new IllegalArgumentException("Native handle cannot be 0");
    }
    this.nativeHandle = nativeHandle;
    this.module = Objects.requireNonNull(module, "module cannot be null");
    this.engine = Objects.requireNonNull(engine, "engine cannot be null");
    this.creationTime = Instant.now();
    LOGGER.fine("Created JniInstancePre with handle: " + nativeHandle);
  }

  @Override
  public Instance instantiate(final Store store) throws WasmException {
    Objects.requireNonNull(store, "store cannot be null");
    ensureNotClosed();

    if (!(store instanceof JniStore)) {
      throw new IllegalArgumentException("Store must be a JniStore instance for JNI InstancePre");
    }

    final JniStore jniStore = (JniStore) store;
    final long instanceHandle = nativeInstantiate(nativeHandle, jniStore.getNativeHandle());

    if (instanceHandle == 0) {
      throw new WasmException("Failed to instantiate from InstancePre");
    }

    return new JniInstance(instanceHandle, module, store);
  }

  @Override
  public CompletableFuture<Instance> instantiateAsync(final Store store) {
    Objects.requireNonNull(store, "store cannot be null");
    return CompletableFuture.supplyAsync(() -> {
      try {
        ensureNotClosed();
      } catch (final WasmException e) {
        throw new java.util.concurrent.CompletionException(e);
      }

      if (!(store instanceof JniStore)) {
        throw new java.util.concurrent.CompletionException(
            new IllegalArgumentException(
                "Store must be a JniStore instance for JNI InstancePre"));
      }

      final JniStore jniStore = (JniStore) store;
      final long instanceHandle =
          nativeInstantiateAsync(nativeHandle, jniStore.getNativeHandle());

      if (instanceHandle == 0) {
        throw new java.util.concurrent.CompletionException(
            new WasmException("Failed to async instantiate from InstancePre"));
      }

      return new JniInstance(instanceHandle, module, store);
    });
  }

  @Override
  public Instance instantiate(final Store store, final ImportMap imports) throws WasmException {
    Objects.requireNonNull(store, "store cannot be null");
    Objects.requireNonNull(imports, "imports cannot be null");
    ensureNotClosed();

    // For now, InstancePre doesn't support additional imports - it uses what was defined
    // in the linker at pre-instantiation time. Just call the regular instantiate.
    LOGGER.fine("instantiate with ImportMap called - imports are resolved at pre-instantiation");
    return instantiate(store);
  }

  @Override
  public Module getModule() {
    return module;
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public boolean isValid() {
    if (closed.get()) {
      return false;
    }
    return nativeIsValid(nativeHandle) != 0;
  }

  @Override
  public long getInstanceCount() {
    if (closed.get()) {
      return 0;
    }
    return nativeGetInstanceCount(nativeHandle);
  }

  @Override
  public PreInstantiationStatistics getStatistics() {
    if (closed.get()) {
      return PreInstantiationStatistics.builder().build();
    }

    final long preparationTimeNs = nativeGetPreparationTimeNs(nativeHandle);
    final long avgInstantiationTimeNs = nativeGetAvgInstantiationTimeNs(nativeHandle);
    final long instanceCount = nativeGetInstanceCount(nativeHandle);

    return PreInstantiationStatistics.builder()
        .creationTime(creationTime)
        .preparationTime(Duration.ofNanos(preparationTimeNs))
        .instancesCreated(instanceCount)
        .averageInstantiationTime(Duration.ofNanos(avgInstantiationTimeNs))
        .build();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      nativeDestroy(nativeHandle);
      LOGGER.fine("Closed JniInstancePre with handle: " + nativeHandle);
    }
  }

  /**
   * Returns the native handle for this InstancePre.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  private void ensureNotClosed() throws WasmException {
    if (closed.get()) {
      throw new WasmException("InstancePre has been closed");
    }
  }

  @Override
  public String toString() {
    return "JniInstancePre{"
        + "handle="
        + nativeHandle
        + ", module="
        + module
        + ", valid="
        + isValid()
        + ", instanceCount="
        + getInstanceCount()
        + '}';
  }

  // Native methods
  private static native long nativeInstantiate(long instancePreHandle, long storeHandle);

  private static native long nativeInstantiateAsync(long instancePreHandle, long storeHandle);

  private static native int nativeIsValid(long instancePreHandle);

  private static native long nativeGetInstanceCount(long instancePreHandle);

  private static native long nativeGetPreparationTimeNs(long instancePreHandle);

  private static native long nativeGetAvgInstantiationTimeNs(long instancePreHandle);

  private static native void nativeDestroy(long instancePreHandle);
}
