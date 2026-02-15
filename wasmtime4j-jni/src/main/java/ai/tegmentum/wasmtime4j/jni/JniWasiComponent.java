package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiComponent;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiComponent interface.
 *
 * <p>This class provides a concrete implementation of WASI component functionality using JNI
 * bindings to the native Wasmtime component model. It manages component lifecycle, metadata
 * extraction, and instantiation through JNI calls.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Component loading from bytes with validation
 *   <li>Interface metadata extraction and caching
 *   <li>Component instantiation with configuration
 *   <li>Comprehensive error handling and resource cleanup
 *   <li>Thread-safe operations with defensive programming
 * </ul>
 *
 * <p>This implementation follows the unified API pattern while delegating to JNI-specific component
 * wrappers for native interactions.
 *
 * @since 1.0.0
 */
public final class JniWasiComponent implements WasiComponent {

  private static final Logger LOGGER = Logger.getLogger(JniWasiComponent.class.getName());

  private final JniComponent.JniComponentEngine componentEngine;
  private final JniComponent.JniComponentHandle componentHandle;
  private final String name;
  private volatile boolean closed = false;

  // Cached metadata to avoid repeated native calls
  private volatile List<String> cachedExports;
  private volatile List<String> cachedImports;

  /**
   * Creates a new JNI WASI component with the specified engine and component handle.
   *
   * @param componentEngine the component engine that loaded this component
   * @param componentHandle the native component handle
   * @param name the optional component name
   * @throws IllegalArgumentException if componentEngine or componentHandle is null
   */
  public JniWasiComponent(
      final JniComponent.JniComponentEngine componentEngine,
      final JniComponent.JniComponentHandle componentHandle,
      final String name) {
    this.componentEngine =
        Objects.requireNonNull(componentEngine, "Component engine cannot be null");
    this.componentHandle =
        Objects.requireNonNull(componentHandle, "Component handle cannot be null");
    this.name = name; // Can be null

    LOGGER.fine("Created JNI WASI component with name: " + (name != null ? name : "unnamed"));
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

      // Create component instance through JNI
      JniComponent.JniComponentInstanceHandle instanceHandle =
          componentEngine.instantiateComponent(componentHandle);

      // Create unified WasiInstance wrapper
      return new JniWasiInstance(this, instanceHandle, config);

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
  public boolean isValid() {
    return !closed && componentEngine.isValid() && componentHandle.isValid();
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;

      // Clear caches
      cachedExports = null;
      cachedImports = null;

      try {
        componentHandle.close();
      } catch (Exception e) {
        LOGGER.warning("Error closing component handle: " + e.getMessage());
      }

      LOGGER.fine("Closed JNI WASI component: " + (name != null ? name : "unnamed"));
    }
  }

  /**
   * Gets the underlying JNI component handle for internal use.
   *
   * @return the JNI component handle
   */
  JniComponent.JniComponentHandle getComponentHandle() {
    ensureNotClosed();
    return componentHandle;
  }

  /**
   * Gets the component engine for internal use.
   *
   * @return the component engine
   */
  JniComponent.JniComponentEngine getComponentEngine() {
    return componentEngine;
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Component has been closed");
    }
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

}
