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

package ai.tegmentum.wasmtime4j.profiler;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Factory for creating Profiler instances.
 *
 * <p>This factory uses Java's ServiceLoader mechanism to discover and instantiate the appropriate
 * Profiler implementation based on the runtime environment.
 *
 * @since 1.0.0
 */
public final class ProfilerFactory {

  private static final Logger LOGGER = Logger.getLogger(ProfilerFactory.class.getName());

  private ProfilerFactory() {
    // Prevent instantiation
  }

  /**
   * Creates a new Profiler with default configuration.
   *
   * @return a new Profiler instance
   * @throws WasmException if the profiler cannot be created
   */
  public static Profiler create() throws WasmException {
    // Try to load via ServiceLoader
    final ServiceLoader<ProfilerProvider> providers = ServiceLoader.load(ProfilerProvider.class);
    for (final ProfilerProvider provider : providers) {
      try {
        final Profiler profiler = provider.create();
        if (profiler != null) {
          LOGGER.fine("Created Profiler using provider: " + provider.getClass().getName());
          return profiler;
        }
      } catch (Exception e) {
        LOGGER.warning("Provider " + provider.getClass().getName() + " failed: " + e.getMessage());
      }
    }

    throw new WasmException(
        "No Profiler implementation found. "
            + "Ensure wasmtime4j-jni or wasmtime4j-panama is on the classpath.");
  }

  /**
   * Service provider interface for Profiler implementations.
   *
   * <p>Implementations should be registered via META-INF/services.
   */
  public interface ProfilerProvider {

    /**
     * Creates a Profiler instance.
     *
     * @return a new Profiler, or null if this provider cannot create one
     * @throws WasmException if creation fails
     */
    Profiler create() throws WasmException;
  }
}
