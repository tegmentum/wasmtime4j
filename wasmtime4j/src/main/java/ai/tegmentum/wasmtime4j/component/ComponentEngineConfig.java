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
package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.config.EngineConfig;

/**
 * Configuration options for WebAssembly Component Model engine creation.
 *
 * <p>This class provides a convenience wrapper around {@link EngineConfig} that pre-configures
 * Component Model support. All real Wasmtime configuration is delegated to the underlying {@link
 * EngineConfig}.
 *
 * <p>For advanced configuration, use {@link #toEngineConfig()} to obtain the underlying {@link
 * EngineConfig} and modify it directly.
 *
 * @since 1.0.0
 */
public final class ComponentEngineConfig {

  private boolean componentModelEnabled = true;

  /** Creates a new component engine configuration with default settings. */
  public ComponentEngineConfig() {
    // Default configuration with Component Model enabled
  }

  /**
   * Enables or disables the Component Model feature.
   *
   * @param enabled true to enable Component Model support
   * @return this configuration for method chaining
   */
  public ComponentEngineConfig componentModelEnabled(final boolean enabled) {
    this.componentModelEnabled = enabled;
    return this;
  }

  /**
   * Returns whether the Component Model feature is enabled.
   *
   * @return true if Component Model support is enabled
   */
  public boolean isComponentModelEnabled() {
    return componentModelEnabled;
  }

  /**
   * Converts this ComponentEngineConfig to an {@link EngineConfig} with Component Model settings
   * applied.
   *
   * @return an EngineConfig configured for component model execution
   */
  public EngineConfig toEngineConfig() {
    final EngineConfig config = new EngineConfig();
    config.wasmComponentModel(componentModelEnabled);
    return config;
  }
}
