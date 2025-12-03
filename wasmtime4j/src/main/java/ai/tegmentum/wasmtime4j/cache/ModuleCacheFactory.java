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

package ai.tegmentum.wasmtime4j.cache;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Factory for creating ModuleCache instances.
 *
 * <p>This factory uses Java's ServiceLoader mechanism to discover and instantiate the appropriate
 * ModuleCache implementation based on the runtime environment.
 *
 * @since 1.0.0
 */
public final class ModuleCacheFactory {

  private static final Logger LOGGER = Logger.getLogger(ModuleCacheFactory.class.getName());

  private ModuleCacheFactory() {
    // Prevent instantiation
  }

  /**
   * Creates a new ModuleCache with default configuration.
   *
   * @param engine the engine to associate with the cache
   * @return a new ModuleCache instance
   * @throws WasmException if the cache cannot be created
   * @throws IllegalArgumentException if engine is null
   */
  public static ModuleCache create(final Engine engine) throws WasmException {
    return create(engine, ModuleCacheConfig.defaultConfig());
  }

  /**
   * Creates a new ModuleCache with the specified configuration.
   *
   * @param engine the engine to associate with the cache
   * @param config the cache configuration
   * @return a new ModuleCache instance
   * @throws WasmException if the cache cannot be created
   * @throws IllegalArgumentException if engine or config is null
   */
  public static ModuleCache create(final Engine engine, final ModuleCacheConfig config)
      throws WasmException {
    Objects.requireNonNull(engine, "engine cannot be null");
    Objects.requireNonNull(config, "config cannot be null");

    // Try to load via ServiceLoader
    final ServiceLoader<ModuleCacheProvider> providers =
        ServiceLoader.load(ModuleCacheProvider.class);
    for (final ModuleCacheProvider provider : providers) {
      try {
        final ModuleCache cache = provider.create(engine, config);
        if (cache != null) {
          LOGGER.fine("Created ModuleCache using provider: " + provider.getClass().getName());
          return cache;
        }
      } catch (Exception e) {
        LOGGER.warning("Provider " + provider.getClass().getName() + " failed: " + e.getMessage());
      }
    }

    throw new WasmException(
        "No ModuleCache implementation found. "
            + "Ensure wasmtime4j-jni or wasmtime4j-panama is on the classpath.");
  }

  /**
   * Service provider interface for ModuleCache implementations.
   *
   * <p>Implementations should be registered via META-INF/services.
   */
  public interface ModuleCacheProvider {

    /**
     * Creates a ModuleCache instance.
     *
     * @param engine the engine to use
     * @param config the cache configuration
     * @return a new ModuleCache, or null if this provider cannot create one
     * @throws WasmException if creation fails
     */
    ModuleCache create(Engine engine, ModuleCacheConfig config) throws WasmException;
  }
}
