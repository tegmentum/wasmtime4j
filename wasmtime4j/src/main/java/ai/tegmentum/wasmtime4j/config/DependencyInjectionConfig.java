/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.config;

import java.util.Map;
import java.util.Objects;

/**
 * Configuration for dependency injection in component linking.
 *
 * <p>This class defines how dependencies should be resolved and injected when linking components.
 *
 * @since 1.0.0
 */
public final class DependencyInjectionConfig {

  private final boolean enableAutowiring;
  private final boolean enableLazyLoading;
  private final Map<String, Object> explicitBindings;

  /**
   * Creates a new dependency injection configuration with default values.
   *
   * <p>Default values:
   *
   * <ul>
   *   <li>enableAutowiring: true
   *   <li>enableLazyLoading: false
   *   <li>explicitBindings: empty map
   * </ul>
   */
  public DependencyInjectionConfig() {
    this(true, false, Map.of());
  }

  /**
   * Creates a new dependency injection configuration.
   *
   * @param enableAutowiring whether to enable automatic dependency resolution
   * @param enableLazyLoading whether to enable lazy loading of dependencies
   * @param explicitBindings map of explicit dependency bindings
   */
  public DependencyInjectionConfig(
      final boolean enableAutowiring,
      final boolean enableLazyLoading,
      final Map<String, Object> explicitBindings) {
    this.enableAutowiring = enableAutowiring;
    this.enableLazyLoading = enableLazyLoading;
    this.explicitBindings =
        Map.copyOf(Objects.requireNonNull(explicitBindings, "explicitBindings cannot be null"));
  }

  /**
   * Checks if autowiring is enabled.
   *
   * @return true if autowiring is enabled
   */
  public boolean isAutowiringEnabled() {
    return enableAutowiring;
  }

  /**
   * Checks if lazy loading is enabled.
   *
   * @return true if lazy loading is enabled
   */
  public boolean isLazyLoadingEnabled() {
    return enableLazyLoading;
  }

  /**
   * Gets the explicit dependency bindings.
   *
   * @return immutable map of explicit bindings
   */
  public Map<String, Object> getExplicitBindings() {
    return explicitBindings;
  }
}
