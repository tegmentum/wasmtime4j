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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiComponent;
import ai.tegmentum.wasmtime4j.wasi.WasiComponentStats;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiInstance;
import ai.tegmentum.wasmtime4j.wasi.WasiInterfaceMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiComponent interface.
 *
 * <p>This class provides a concrete implementation of WASI component functionality using Panama
 * Foreign Function API bindings to the native Wasmtime component model. It manages component
 * lifecycle, metadata extraction, and instantiation through Panama FFI calls with Arena-based
 * resource management.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Component loading from bytes with validation
 *   <li>Interface metadata extraction and caching
 *   <li>Component instantiation with configuration
 *   <li>Arena-based resource management for automatic cleanup
 *   <li>Zero-copy optimized memory access where possible
 *   <li>Thread-safe operations with defensive programming
 * </ul>
 *
 * <p>This implementation follows the unified API pattern while using Panama-specific component
 * wrappers for native interactions with optimal performance characteristics.
 *
 * @since 1.0.0
 */
public final class PanamaWasiComponent implements WasiComponent {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiComponent.class.getName());

  private final ArenaResourceManager resourceManager;
  private final PanamaComponent.PanamaComponentEngine componentEngine;
  private final PanamaComponent.PanamaComponentHandle componentHandle;
  private final String name;
  private final NativeResourceHandle resourceHandle;

  // Cached metadata to avoid repeated native calls
  private volatile List<String> cachedExports;
  private volatile List<String> cachedImports;
  private volatile WasiComponentStats cachedStats;

  /**
   * Creates a new Panama WASI component with the specified engine and component handle.
   *
   * @param resourceManager the arena resource manager for lifecycle management
   * @param componentEngine the component engine that loaded this component
   * @param componentHandle the native component handle
   * @param name the optional component name
   * @throws IllegalArgumentException if any required parameter is null
   */
  public PanamaWasiComponent(
      final ArenaResourceManager resourceManager,
      final PanamaComponent.PanamaComponentEngine componentEngine,
      final PanamaComponent.PanamaComponentHandle componentHandle,
      final String name) {
    this.resourceManager =
        Objects.requireNonNull(resourceManager, "Resource manager cannot be null");
    this.componentEngine =
        Objects.requireNonNull(componentEngine, "Component engine cannot be null");
    this.componentHandle =
        Objects.requireNonNull(componentHandle, "Component handle cannot be null");
    this.name = name; // Can be null

    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaWasiComponent",
            () -> {
              // Clear caches
              cachedExports = null;
              cachedImports = null;
              cachedStats = null;

              try {
                componentHandle.close();
              } catch (final Throwable t) {
                throw new Exception("Error closing PanamaWasiComponent component handle", t);
              }
            });

    LOGGER.fine("Created Panama WASI component with name: " + (name != null ? name : "unnamed"));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<String> getExports() throws WasmException {
    ensureNotClosed();

    if (cachedExports == null) {
      synchronized (this) {
        if (cachedExports == null) {
          cachedExports = extractExports();
        }
      }
    }
    return new ArrayList<>(cachedExports);
  }

  @Override
  public List<String> getImports() throws WasmException {
    ensureNotClosed();

    if (cachedImports == null) {
      synchronized (this) {
        if (cachedImports == null) {
          cachedImports = extractImports();
        }
      }
    }
    return new ArrayList<>(cachedImports);
  }

  @Override
  public WasiInterfaceMetadata getExportMetadata(final String interfaceName) throws WasmException {
    Objects.requireNonNull(interfaceName, "Interface name cannot be null");
    if (interfaceName.trim().isEmpty()) {
      throw new IllegalArgumentException("Interface name cannot be empty");
    }
    ensureNotClosed();

    // Check if interface exists
    if (!getExports().contains(interfaceName)) {
      throw new WasmException("Interface not found in exports: " + interfaceName);
    }

    throw new UnsupportedOperationException(
        "not yet implemented: native export metadata extraction");
  }

  @Override
  public WasiInterfaceMetadata getImportMetadata(final String interfaceName) throws WasmException {
    Objects.requireNonNull(interfaceName, "Interface name cannot be null");
    if (interfaceName.trim().isEmpty()) {
      throw new IllegalArgumentException("Interface name cannot be empty");
    }
    ensureNotClosed();

    // Check if interface exists
    if (!getImports().contains(interfaceName)) {
      throw new WasmException("Interface not found in imports: " + interfaceName);
    }

    throw new UnsupportedOperationException(
        "not yet implemented: native import metadata extraction");
  }

  @Override
  public WasiInstance instantiate() throws WasmException {
    // Use default configuration
    return instantiate(WasiConfig.defaultConfig());
  }

  @Override
  public WasiInstance instantiate(final WasiConfig config) throws WasmException {
    Objects.requireNonNull(config, "Configuration cannot be null");
    ensureNotClosed();

    try {
      // Validate configuration before instantiation
      config.validate();

      // Create component instance through Panama FFI
      PanamaComponent.PanamaComponentInstanceHandle instanceHandle =
          componentEngine.instantiateComponent(componentHandle);

      // Create unified WasiInstance wrapper
      return new PanamaWasiInstance(resourceManager, this, instanceHandle, config);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to instantiate component", e);
    }
  }

  @Override
  public void validate() throws WasmException {
    validate(WasiConfig.defaultConfig());
  }

  @Override
  public void validate(final WasiConfig config) throws WasmException {
    Objects.requireNonNull(config, "Configuration cannot be null");
    ensureNotClosed();

    try {
      // Validate configuration
      config.validate();

      // TODO: Implement component-specific validation
      // For now, just check if component is still valid
      if (!componentHandle.isValid()) {
        throw new WasmException("Component handle is no longer valid");
      }

      // Validate exports and imports
      getExports(); // This will cache and validate exports
      getImports(); // This will cache and validate imports

      LOGGER.fine("Component validation passed for: " + (name != null ? name : "unnamed"));

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Component validation failed", e);
    }
  }

  @Override
  public WasiComponentStats getStats() {
    ensureNotClosed();

    if (cachedStats == null) {
      synchronized (this) {
        if (cachedStats == null) {
          cachedStats = extractStats();
        }
      }
    }
    return cachedStats;
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed() && componentEngine.isValid() && componentHandle.isValid();
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  /**
   * Gets the underlying Panama component handle for internal use.
   *
   * @return the Panama component handle
   */
  PanamaComponent.PanamaComponentHandle getComponentHandle() {
    ensureNotClosed();
    return componentHandle;
  }

  /**
   * Gets the component engine for internal use.
   *
   * @return the component engine
   */
  PanamaComponent.PanamaComponentEngine getComponentEngine() {
    return componentEngine;
  }

  /**
   * Gets the resource manager for internal use.
   *
   * @return the arena resource manager
   */
  ArenaResourceManager getResourceManager() {
    return resourceManager;
  }

  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  /**
   * Extracts the list of exported interfaces from the native component.
   *
   * @return list of exported interface names
   * @throws WasmException if extraction fails
   */
  private List<String> extractExports() throws WasmException {
    throw new UnsupportedOperationException(
        "not yet implemented: native component export extraction");
  }

  /**
   * Extracts the list of imported interfaces from the native component.
   *
   * @return list of imported interface names
   * @throws WasmException if extraction fails
   */
  private List<String> extractImports() throws WasmException {
    throw new UnsupportedOperationException(
        "not yet implemented: native component import extraction");
  }

  private WasiComponentStats extractStats() {
    throw new UnsupportedOperationException(
        "not yet implemented: native component statistics extraction");
  }
}
