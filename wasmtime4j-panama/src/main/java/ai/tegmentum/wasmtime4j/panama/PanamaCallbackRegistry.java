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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.AbstractCallbackRegistry;
import ai.tegmentum.wasmtime4j.func.CallbackEntry;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the callback registry for managing callbacks and asynchronous
 * operations.
 *
 * <p>This registry extends {@link AbstractCallbackRegistry} with Panama-specific function reference
 * creation and resource cleanup through {@link NativeResourceHandle} and {@link
 * ArenaResourceManager}.
 *
 * @since 1.0.0
 */
public final class PanamaCallbackRegistry extends AbstractCallbackRegistry {
  private static final Logger LOGGER = Logger.getLogger(PanamaCallbackRegistry.class.getName());

  private final WeakReference<PanamaStore> storeRef;
  private final ArenaResourceManager arenaManager;
  private volatile ScheduledExecutorService asyncExecutor;
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new callback registry for the given store.
   *
   * @param store the store this registry belongs to
   * @param arenaManager the arena resource manager
   */
  PanamaCallbackRegistry(final PanamaStore store, final ArenaResourceManager arenaManager) {
    this.storeRef = new WeakReference<>(Objects.requireNonNull(store, "Store cannot be null"));
    this.arenaManager = Objects.requireNonNull(arenaManager, "Arena manager cannot be null");

    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaCallbackRegistry",
            () -> {
              for (final CallbackEntry entry : callbacks.values()) {
                try {
                  closeFunctionReference(entry.getFunctionReference());
                } catch (final Exception e) {
                  LOGGER.log(
                      Level.WARNING,
                      "Error closing callback function reference: " + entry.getHandle().getName(),
                      e);
                }
              }
              callbacks.clear();

              if (asyncExecutor != null) {
                asyncExecutor.shutdownNow();
              }

              if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Closed Panama callback registry");
              }
            });

    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.fine("Created Panama callback registry for store");
    }
  }

  @Override
  protected FunctionReference createFunctionReferenceForCallback(
      final HostFunction callback, final FunctionType functionType) throws WasmException {
    final PanamaStore store = getStore();
    return new PanamaFunctionReference(callback, functionType, store, arenaManager);
  }

  @Override
  protected void closeFunctionReference(final FunctionReference ref) {
    if (ref instanceof PanamaFunctionReference panamaFuncRef) {
      try {
        panamaFuncRef.close();
      } catch (final WasmException e) {
        LOGGER.log(Level.WARNING, "Error closing Panama function reference", e);
      }
    }
  }

  @Override
  protected ScheduledExecutorService getAsyncExecutor() {
    if (asyncExecutor == null) {
      synchronized (this) {
        if (asyncExecutor == null) {
          asyncExecutor =
              Executors.newScheduledThreadPool(
                  4,
                  r -> {
                    final Thread t = new Thread(r, "wasmtime4j-panama-async-callback");
                    t.setDaemon(true);
                    return t;
                  });
        }
      }
    }
    return asyncExecutor;
  }

  @Override
  protected void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  @Override
  public void close() throws WasmException {
    resourceHandle.close();
  }

  /**
   * Gets the store associated with this registry.
   *
   * @return the store
   * @throws WasmException if the store is no longer available
   */
  private PanamaStore getStore() throws WasmException {
    final PanamaStore store = storeRef.get();
    if (store == null) {
      throw new WasmException("Store is no longer available");
    }
    return store;
  }
}
