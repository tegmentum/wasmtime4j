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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.func.AbstractCallbackRegistry;
import ai.tegmentum.wasmtime4j.func.CallbackEntry;
import ai.tegmentum.wasmtime4j.func.FunctionReference;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of the callback registry for managing callbacks and asynchronous operations.
 *
 * <p>This registry extends {@link AbstractCallbackRegistry} with JNI-specific function reference
 * creation and resource cleanup. It manages a dedicated thread pool for async callback execution
 * and provides lifecycle management through manual close coordination.
 *
 * @since 1.0.0
 */
public final class JniCallbackRegistry extends AbstractCallbackRegistry {
  private static final Logger LOGGER = Logger.getLogger(JniCallbackRegistry.class.getName());

  private final WeakReference<JniStore> storeRef;
  private final ScheduledExecutorService asyncExecutor;
  private volatile boolean closed = false;

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
  protected FunctionReference createFunctionReferenceForCallback(
      final HostFunction callback, final FunctionType functionType) throws WasmException {
    final JniStore store = getStore();
    return new JniFunctionReference(callback, functionType, store);
  }

  @Override
  protected void closeFunctionReference(final FunctionReference ref) {
    if (ref instanceof JniFunctionReference) {
      ((JniFunctionReference) ref).close();
    }
  }

  @Override
  protected ScheduledExecutorService getAsyncExecutor() {
    return asyncExecutor;
  }

  @Override
  protected void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Callback registry has been closed");
    }
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
        for (final CallbackEntry entry : callbacks.values()) {
          try {
            closeFunctionReference(entry.getFunctionReference());
          } catch (Exception e) {
            LOGGER.log(
                Level.WARNING,
                "Error closing callback function reference: " + entry.getHandle().getName(),
                e);
          }
        }
        callbacks.clear();

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
}
