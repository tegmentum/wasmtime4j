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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ImportMap;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstancePre;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.PreInstantiationStatistics;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of {@link InstancePre}.
 *
 * <p>This class wraps a native pre-instantiated module handle and provides fast instantiation
 * capabilities. Pre-instantiation performs expensive setup work once, allowing subsequent instance
 * creation to be significantly faster.
 *
 * @since 1.0.0
 */
public final class PanamaInstancePre implements InstancePre {

  private static final Logger LOGGER = Logger.getLogger(PanamaInstancePre.class.getName());
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final MemorySegment nativeInstancePre;
  private final Module module;
  private final Engine engine;
  private final Instant creationTime;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new PanamaInstancePre with the given native handle.
   *
   * @param nativeInstancePre the native InstancePre memory segment
   * @param module the module that was pre-instantiated
   * @param engine the engine used for pre-instantiation
   * @throws IllegalArgumentException if nativeInstancePre is null or module/engine is null
   */
  public PanamaInstancePre(
      final MemorySegment nativeInstancePre, final Module module, final Engine engine) {
    if (nativeInstancePre == null || nativeInstancePre.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native InstancePre cannot be null");
    }
    this.nativeInstancePre = nativeInstancePre;
    this.module = Objects.requireNonNull(module, "module cannot be null");
    this.engine = Objects.requireNonNull(engine, "engine cannot be null");
    this.creationTime = Instant.now();
    LOGGER.fine("Created PanamaInstancePre");
  }

  @Override
  public Instance instantiate(final Store store) throws WasmException {
    Objects.requireNonNull(store, "store cannot be null");
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new IllegalArgumentException(
          "Store must be a PanamaStore instance for Panama InstancePre");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final MemorySegment instancePtr =
        NATIVE_BINDINGS.instancePreInstantiate(nativeInstancePre, panamaStore.getNativeStore());

    if (instancePtr == null || instancePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to instantiate from InstancePre");
    }

    if (!(module instanceof PanamaModule)) {
      throw new WasmException("Module must be a PanamaModule for Panama InstancePre");
    }

    return new PanamaInstance(instancePtr, (PanamaModule) module, panamaStore);
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
    return NATIVE_BINDINGS.instancePreIsValid(nativeInstancePre) != 0;
  }

  @Override
  public long getInstanceCount() {
    if (closed.get()) {
      return 0;
    }
    return NATIVE_BINDINGS.instancePreGetInstanceCount(nativeInstancePre);
  }

  @Override
  public CompletableFuture<Instance> instantiateAsync(final Store store) {
    Objects.requireNonNull(store, "store cannot be null");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return instantiate(store);
          } catch (WasmException e) {
            throw new RuntimeException("Failed to instantiate asynchronously", e);
          }
        });
  }

  @Override
  public CompletableFuture<Instance> instantiateAsync(final Store store, final ImportMap imports) {
    Objects.requireNonNull(store, "store cannot be null");
    Objects.requireNonNull(imports, "imports cannot be null");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return instantiate(store, imports);
          } catch (WasmException e) {
            throw new RuntimeException("Failed to instantiate asynchronously with imports", e);
          }
        });
  }

  @Override
  public PreInstantiationStatistics getStatistics() {
    if (closed.get()) {
      return PreInstantiationStatistics.builder().build();
    }

    final long preparationTimeNs =
        NATIVE_BINDINGS.instancePreGetPreparationTimeNs(nativeInstancePre);
    final long avgInstantiationTimeNs =
        NATIVE_BINDINGS.instancePreGetAvgInstantiationTimeNs(nativeInstancePre);
    final long instanceCount = NATIVE_BINDINGS.instancePreGetInstanceCount(nativeInstancePre);

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
      NATIVE_BINDINGS.instancePreDestroy(nativeInstancePre);
      LOGGER.fine("Closed PanamaInstancePre");
    }
  }

  /**
   * Returns the native InstancePre memory segment.
   *
   * @return the native memory segment
   */
  public MemorySegment getNativeInstancePre() {
    return nativeInstancePre;
  }

  private void ensureNotClosed() throws WasmException {
    if (closed.get()) {
      throw new WasmException("InstancePre has been closed");
    }
  }

  @Override
  public String toString() {
    return "PanamaInstancePre{"
        + "module="
        + module
        + ", valid="
        + isValid()
        + ", instanceCount="
        + getInstanceCount()
        + '}';
  }
}
