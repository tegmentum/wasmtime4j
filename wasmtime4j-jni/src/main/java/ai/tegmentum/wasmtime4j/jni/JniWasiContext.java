package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
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
 * JNI implementation of the WasiContext interface.
 *
 * <p>This class provides a concrete implementation of WASI context functionality using JNI bindings
 * to the native Wasmtime component model. It manages component loading, runtime information, and
 * context lifecycle through JNI calls.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Component loading from bytes and files
 *   <li>Runtime information and version reporting
 *   <li>Resource management and cleanup
 *   <li>Thread-safe operations with defensive programming
 *   <li>Integration with JNI component engine
 * </ul>
 *
 * <p>This implementation follows the unified API pattern while using JNI-specific component
 * infrastructure for native interactions.
 *
 * @since 1.0.0
 */
public final class JniWasiContext implements WasiContext {

  private static final Logger LOGGER = Logger.getLogger(JniWasiContext.class.getName());

  private final JniComponent.JniComponentEngine componentEngine;
  private final WasiRuntimeInfo runtimeInfo;
  private volatile boolean closed = false;

  /**
   * Creates a new JNI WASI context with default configuration.
   *
   * @throws WasmException if context creation fails
   */
  public JniWasiContext() throws WasmException {
    try {
      // Create the underlying component engine
      this.componentEngine = JniComponent.createComponentEngine();

      // Create runtime information
      this.runtimeInfo = createRuntimeInfo();

      LOGGER.info("Created JNI WASI context successfully");

    } catch (final Exception e) {
      throw new WasmException("Failed to create JNI WASI context", e);
    }
  }

  @Override
  public WasiComponent createComponent(final byte[] wasmBytes) throws WasmException {
    Objects.requireNonNull(wasmBytes, "WebAssembly bytes cannot be null");
    JniValidation.requireNonEmpty(wasmBytes, "wasmBytes");
    ensureNotClosed();

    try {
      // Load component through JNI component engine
      JniComponent.JniComponentHandle componentHandle =
          componentEngine.loadComponentFromBytes(wasmBytes);

      // Create unified WasiComponent wrapper
      return new JniWasiComponent(componentEngine, componentHandle, null);

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
   * and then loaded as bytes.
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

      // Load component through JNI component engine
      JniComponent.JniComponentHandle componentHandle =
          componentEngine.loadComponentFromBytes(wasmBytes);

      // Extract file name for component name
      String componentName = wasmFile.getFileName().toString();

      // Create unified WasiComponent wrapper
      return new JniWasiComponent(componentEngine, componentHandle, componentName);

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

      // Load component through JNI component engine
      JniComponent.JniComponentHandle componentHandle =
          componentEngine.loadComponentFromBytes(wasmBytes);

      // Create unified WasiComponent wrapper
      return new JniWasiComponent(componentEngine, componentHandle, componentName);

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
    return !closed && componentEngine.isValid();
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
   * that are no longer referenced by application code.
   *
   * @return the number of instances that were cleaned up
   */
  public int cleanupInstances() {
    ensureNotClosed();
    return componentEngine.cleanupInstances();
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

      LOGGER.fine("Closed JNI WASI context");
    }
  }

  /**
   * Gets the underlying JNI component engine for internal use.
   *
   * @return the JNI component engine
   */
  JniComponent.JniComponentEngine getComponentEngine() {
    ensureNotClosed();
    return componentEngine;
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("WASI context has been closed");
    }
  }

  /**
   * Creates runtime information for this JNI context.
   *
   * @return runtime information
   */
  private WasiRuntimeInfo createRuntimeInfo() {
    // TODO: Extract actual versions from native layer
    return new WasiRuntimeInfo(WasiRuntimeType.JNI, "1.0.0-jni", "36.0.2");
  }
}
