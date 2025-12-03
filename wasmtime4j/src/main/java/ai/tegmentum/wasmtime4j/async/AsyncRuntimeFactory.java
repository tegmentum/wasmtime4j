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

package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Factory for creating AsyncRuntime instances.
 *
 * <p>This factory uses Java's ServiceLoader mechanism to discover and instantiate the appropriate
 * AsyncRuntime implementation based on the runtime environment.
 *
 * @since 1.0.0
 */
public final class AsyncRuntimeFactory {

  private static final Logger LOGGER = Logger.getLogger(AsyncRuntimeFactory.class.getName());

  private AsyncRuntimeFactory() {
    // Prevent instantiation
  }

  /**
   * Creates a new AsyncRuntime.
   *
   * @return a new AsyncRuntime instance
   * @throws WasmException if no implementation is available
   */
  public static AsyncRuntime create() throws WasmException {
    // Try to load via ServiceLoader
    final ServiceLoader<AsyncRuntimeProvider> providers =
        ServiceLoader.load(AsyncRuntimeProvider.class);
    for (final AsyncRuntimeProvider provider : providers) {
      try {
        final AsyncRuntime runtime = provider.create();
        if (runtime != null) {
          LOGGER.fine("Created AsyncRuntime using provider: " + provider.getClass().getName());
          return runtime;
        }
      } catch (Exception e) {
        LOGGER.warning("Provider " + provider.getClass().getName() + " failed: " + e.getMessage());
      }
    }

    throw new WasmException(
        "No AsyncRuntime implementation found. "
            + "Ensure wasmtime4j-jni or wasmtime4j-panama is on the classpath.");
  }

  /**
   * Gets the global shared AsyncRuntime instance.
   *
   * <p>This method returns a lazily-initialized singleton that is shared across the application.
   * The runtime is automatically initialized on first access.
   *
   * @return the shared AsyncRuntime instance
   * @throws WasmException if the runtime cannot be created
   */
  public static AsyncRuntime getSharedInstance() throws WasmException {
    return SharedRuntimeHolder.INSTANCE;
  }

  /** Lazy holder for thread-safe singleton initialization. */
  private static final class SharedRuntimeHolder {
    static final AsyncRuntime INSTANCE;

    static {
      try {
        INSTANCE = create();
        INSTANCE.initialize();
      } catch (WasmException e) {
        throw new ExceptionInInitializerError(e);
      }
    }
  }

  /**
   * Service provider interface for AsyncRuntime implementations.
   *
   * <p>Implementations should be registered via META-INF/services.
   */
  public interface AsyncRuntimeProvider {

    /**
     * Creates an AsyncRuntime instance.
     *
     * @return a new AsyncRuntime, or null if this provider cannot create one
     * @throws WasmException if creation fails
     */
    AsyncRuntime create() throws WasmException;
  }
}
