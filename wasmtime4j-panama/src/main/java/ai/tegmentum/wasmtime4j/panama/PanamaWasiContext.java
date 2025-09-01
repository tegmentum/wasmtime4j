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
import ai.tegmentum.wasmtime4j.wasi.WasiComponent;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiRuntimeInfo;
import ai.tegmentum.wasmtime4j.wasi.WasiRuntimeType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiContext interface.
 *
 * <p>This class provides a concrete implementation of WASI context functionality using Panama
 * Foreign Function API bindings to the native Wasmtime component model. It manages component
 * loading, runtime information, and context lifecycle through Panama FFI calls with Arena-based
 * resource management.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Component loading from bytes and files with zero-copy optimization
 *   <li>Arena-based automatic resource management
 *   <li>Runtime information and version reporting
 *   <li>Optimal performance through direct FFI calls
 *   <li>Thread-safe operations with defensive programming
 * </ul>
 *
 * <p>This implementation follows the unified API pattern while using Panama-specific component
 * infrastructure for native interactions with optimal performance characteristics.
 *
 * @since 1.0.0
 */
public final class PanamaWasiContext implements WasiContext {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiContext.class.getName());

  private final ArenaResourceManager resourceManager;
  private final PanamaComponent.PanamaComponentEngine componentEngine;
  private final WasiRuntimeInfo runtimeInfo;
  private volatile boolean closed = false;

  /**
   * Creates a new Panama WASI context with default configuration.
   *
   * @throws WasmException if context creation fails
   */
  public PanamaWasiContext() throws WasmException {
    try {
      // Create arena resource manager for lifecycle management
      this.resourceManager = new ArenaResourceManager();

      // Create the underlying component engine
      this.componentEngine = PanamaComponent.createComponentEngine(resourceManager);

      // Create runtime information
      this.runtimeInfo = createRuntimeInfo();

      LOGGER.info("Created Panama WASI context successfully");

    } catch (final Exception e) {
      // Clean up on failure
      if (resourceManager != null) {
        try {
          resourceManager.close();
        } catch (Exception cleanupException) {
          e.addSuppressed(cleanupException);
        }
      }
      throw new WasmException("Failed to create Panama WASI context", e);
    }
  }

  @Override
  public WasiComponent createComponent(final byte[] wasmBytes) throws WasmException {
    Objects.requireNonNull(wasmBytes, "WebAssembly bytes cannot be null");
    PanamaErrorHandler.requirePositive(wasmBytes.length, "wasmBytes.length");
    ensureNotClosed();

    try {
      // Load component through Panama component engine
      PanamaComponent.PanamaComponentHandle componentHandle =
          componentEngine.loadComponentFromBytes(wasmBytes);

      // Create unified WasiComponent wrapper
      return new PanamaWasiComponent(resourceManager, componentEngine, componentHandle, null);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create component from bytes", e);
    }
  }

  /**
   * Creates a component from a WebAssembly file.
   *
   * <p>This is a convenience method for loading components from files. The file is read into memory
   * and then loaded as bytes using zero-copy optimization where possible.
   *
   * @param wasmFile the path to the WebAssembly component file
   * @return a new WasiComponent instance
   * @throws WasmException if component creation fails
   * @throws IllegalArgumentException if wasmFile is null
   */
  public WasiComponent createComponentFromFile(final Path wasmFile) throws WasmException {
    Objects.requireNonNull(wasmFile, "WebAssembly file path cannot be null");
    ensureNotClosed();

    try {
      // Read file into bytes
      byte[] wasmBytes = Files.readAllBytes(wasmFile);

      // Load component through Panama component engine
      PanamaComponent.PanamaComponentHandle componentHandle =
          componentEngine.loadComponentFromBytes(wasmBytes);

      // Extract file name for component name
      String componentName = wasmFile.getFileName().toString();

      // Create unified WasiComponent wrapper
      return new PanamaWasiComponent(
          resourceManager, componentEngine, componentHandle, componentName);

    } catch (final IOException e) {
      throw new WasmException("Failed to read WebAssembly file: " + wasmFile, e);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create component from file: " + wasmFile, e);
    }
  }

  /**
   * Creates a component from a WebAssembly file with a custom name.
   *
   * @param wasmFile the path to the WebAssembly component file
   * @param componentName the custom name for the component
   * @return a new WasiComponent instance
   * @throws WasmException if component creation fails
   * @throws IllegalArgumentException if wasmFile or componentName is null
   */
  public WasiComponent createComponentFromFile(final Path wasmFile, final String componentName)
      throws WasmException {
    Objects.requireNonNull(wasmFile, "WebAssembly file path cannot be null");
    Objects.requireNonNull(componentName, "Component name cannot be null");
    ensureNotClosed();

    try {
      // Read file into bytes
      byte[] wasmBytes = Files.readAllBytes(wasmFile);

      // Load component through Panama component engine
      PanamaComponent.PanamaComponentHandle componentHandle =
          componentEngine.loadComponentFromBytes(wasmBytes);

      // Create unified WasiComponent wrapper
      return new PanamaWasiComponent(
          resourceManager, componentEngine, componentHandle, componentName);

    } catch (final IOException e) {
      throw new WasmException("Failed to read WebAssembly file: " + wasmFile, e);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create component from file: " + wasmFile, e);
    }
  }

  @Override
  public WasiRuntimeInfo getRuntimeInfo() {
    return runtimeInfo;
  }

  @Override
  public boolean isValid() {
    return !closed && resourceManager.isValid() && componentEngine.isValid();
  }

  /**
   * Gets the number of active component instances managed by this context.
   *
   * @return the number of active instances
   */
  public int getActiveInstancesCount() {
    ensureNotClosed();
    return componentEngine.getActiveInstancesCount();
  }

  /**
   * Cleans up inactive component instances managed by this context.
   *
   * <p>This method can be called periodically to free up resources from components and instances
   * that are no longer referenced by application code. With Arena-based resource management, this
   * also helps clean up native memory more efficiently.
   *
   * @return the number of instances that were cleaned up
   */
  public int cleanupInstances() {
    ensureNotClosed();
    return componentEngine.cleanupInstances();
  }

  /**
   * Gets memory usage information for this context.
   *
   * @return arena resource manager statistics
   */
  public ArenaResourceManager.Statistics getMemoryStatistics() {
    ensureNotClosed();
    return resourceManager.getStatistics();
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;

      try {
        componentEngine.close();
      } catch (Exception e) {
        LOGGER.warning("Error closing component engine: " + e.getMessage());
      }

      try {
        resourceManager.close();
      } catch (Exception e) {
        LOGGER.warning("Error closing resource manager: " + e.getMessage());
      }

      LOGGER.fine("Closed Panama WASI context");
    }
  }

  /**
   * Gets the underlying Panama component engine for internal use.
   *
   * @return the Panama component engine
   */
  PanamaComponent.PanamaComponentEngine getComponentEngine() {
    ensureNotClosed();
    return componentEngine;
  }

  /**
   * Gets the resource manager for internal use.
   *
   * @return the arena resource manager
   */
  ArenaResourceManager getResourceManager() {
    ensureNotClosed();
    return resourceManager;
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("WASI context has been closed");
    }
  }

  /**
   * Creates runtime information for this Panama context.
   *
   * @return runtime information
   */
  private WasiRuntimeInfo createRuntimeInfo() {
    return new WasiRuntimeInfo() {
      @Override
      public WasiRuntimeType getType() {
        return WasiRuntimeType.PANAMA;
      }

      @Override
      public String getVersion() {
        // TODO: Extract actual version from native layer
        return "1.0.0-panama";
      }

      @Override
      public String getWasmtimeVersion() {
        // TODO: Extract actual Wasmtime version from native layer
        return "36.0.2";
      }

      @Override
      public boolean supportsComponentModel() {
        return true;
      }

      @Override
      public boolean supportsAsync() {
        return true;
      }

      @Override
      public boolean supportsResourceSharing() {
        return true;
      }

      @Override
      public String getDescription() {
        return "Panama FFI-based WASI runtime implementation using Wasmtime component model with"
            + " Arena-based resource management";
      }
    };
  }
}
