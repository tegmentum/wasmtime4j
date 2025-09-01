package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeMethodBindings;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * JNI implementation wrapper for WebAssembly Component operations.
 *
 * <p>This class provides a bridge between the Java component model API and the native Rust
 * implementation via JNI calls. It handles component engine management, component loading,
 * instantiation, and resource cleanup.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic resource management with {@link AutoCloseable}
 *   <li>Defensive programming to prevent JVM crashes
 *   <li>Comprehensive parameter validation
 *   <li>Thread-safe operations
 *   <li>Component lifecycle management
 * </ul>
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * try (JniComponentEngine engine = JniComponent.createComponentEngine()) {
 *   JniComponentHandle component = engine.loadComponentFromBytes(wasmBytes);
 *   long instanceHandle = engine.instantiateComponent(component.getNativeHandle());
 *   // Use the instance...
 * }
 * }</pre>
 *
 * <p>This implementation extends {@link JniResource} to provide automatic native resource
 * management and follows defensive programming practices to prevent native crashes.
 *
 * @since 1.0.0
 */
public final class JniComponent {

  private static final Logger LOGGER = Logger.getLogger(JniComponent.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniComponent: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Prevent instantiation - this class contains only static factory methods.
   */
  private JniComponent() {}

  /**
   * Creates a new component engine with default configuration.
   *
   * <p>The component engine manages component loading, instantiation, and lifecycle. It should be
   * reused for multiple components to amortize initialization costs.
   *
   * @return a new component engine instance
   * @throws JniException if engine creation fails
   */
  public static JniComponentEngine createComponentEngine() {
    NativeMethodBindings.ensureInitialized();

    try {
      final long engineHandle = nativeCreateComponentEngine();
      JniValidation.requireValidHandle(engineHandle, "engineHandle");
      return new JniComponentEngine(engineHandle);
    } catch (final Exception e) {
      throw new JniException("Failed to create component engine", e);
    }
  }

  // Native method declarations

  /**
   * Creates a new native component engine with default configuration.
   *
   * @return native component engine handle or 0 on failure
   */
  private static native long nativeCreateComponentEngine();

  /**
   * Loads a component from WebAssembly bytes using the specified engine.
   *
   * @param engineHandle the native component engine handle
   * @param wasmBytes the WebAssembly component bytes
   * @return native component handle or 0 on failure
   */
  static native long nativeLoadComponentFromBytes(long engineHandle, byte[] wasmBytes);

  /**
   * Instantiates a component using the specified engine.
   *
   * @param engineHandle the native component engine handle
   * @param componentHandle the native component handle
   * @return native component instance handle or 0 on failure
   */
  static native long nativeInstantiateComponent(long engineHandle, long componentHandle);

  /**
   * Gets the size of a component in bytes.
   *
   * @param componentHandle the native component handle
   * @return component size in bytes or 0 on failure
   */
  static native long nativeGetComponentSize(long componentHandle);

  /**
   * Checks if a component exports the specified interface.
   *
   * @param componentHandle the native component handle
   * @param interfaceName the interface name to check
   * @return true if the interface is exported, false otherwise
   */
  static native boolean nativeExportsInterface(long componentHandle, String interfaceName);

  /**
   * Checks if a component imports the specified interface.
   *
   * @param componentHandle the native component handle
   * @param interfaceName the interface name to check
   * @return true if the interface is imported, false otherwise
   */
  static native boolean nativeImportsInterface(long componentHandle, String interfaceName);

  /**
   * Gets the number of active instances for a component engine.
   *
   * @param engineHandle the native component engine handle
   * @return number of active instances
   */
  static native int nativeGetActiveInstancesCount(long engineHandle);

  /**
   * Cleans up inactive instances for a component engine.
   *
   * @param engineHandle the native component engine handle
   * @return number of instances cleaned up
   */
  static native int nativeCleanupInstances(long engineHandle);

  /**
   * Destroys a component engine and releases associated resources.
   *
   * @param engineHandle the native component engine handle
   */
  static native void nativeDestroyComponentEngine(long engineHandle);

  /**
   * Destroys a component and releases associated resources.
   *
   * @param componentHandle the native component handle
   */
  static native void nativeDestroyComponent(long componentHandle);

  /**
   * Destroys a component instance and releases associated resources.
   *
   * @param instanceHandle the native component instance handle
   */
  static native void nativeDestroyComponentInstance(long instanceHandle);

  /**
   * JNI wrapper for component engine operations.
   *
   * <p>Manages the lifecycle of a native component engine and provides methods for loading and
   * instantiating components. Implements automatic resource cleanup through {@link JniResource}.
   */
  public static final class JniComponentEngine extends JniResource {

    /**
     * Creates a new component engine wrapper with the given native handle.
     *
     * @param nativeHandle the native component engine handle
     * @throws JniResourceException if nativeHandle is invalid
     */
    JniComponentEngine(final long nativeHandle) {
      super(nativeHandle);
      LOGGER.fine("Created component engine with handle: 0x" + Long.toHexString(nativeHandle));
    }

    /**
     * Loads a component from WebAssembly bytes.
     *
     * <p>This method validates and compiles the provided WebAssembly component bytes into a
     * component that can be instantiated and executed.
     *
     * @param wasmBytes the WebAssembly component bytes to load
     * @return a component handle wrapper
     * @throws JniException if loading fails
     * @throws JniResourceException if this engine has been closed
     */
    public JniComponentHandle loadComponentFromBytes(final byte[] wasmBytes) throws WasmException {
      JniValidation.requireNonEmpty(wasmBytes, "wasmBytes");
      ensureNotClosed();

      final byte[] wasmBytesCopy = JniValidation.defensiveCopy(wasmBytes);

      try {
        final long componentHandle = nativeLoadComponentFromBytes(getNativeHandle(), wasmBytesCopy);
        JniValidation.requireValidHandle(componentHandle, "componentHandle");
        return new JniComponentHandle(componentHandle);
      } catch (final Exception e) {
        if (e instanceof JniException) {
          throw new WasmException(e.getMessage(), e);
        }
        throw new WasmException("Failed to load component from bytes", e);
      }
    }

    /**
     * Instantiates a component.
     *
     * <p>Creates a new instance of the specified component that can be used to call exported
     * functions and interact with component resources.
     *
     * @param component the component to instantiate
     * @return a component instance handle wrapper
     * @throws JniException if instantiation fails
     * @throws JniResourceException if this engine has been closed
     */
    public JniComponentInstanceHandle instantiateComponent(final JniComponentHandle component)
        throws WasmException {
      JniValidation.requireNonNull(component, "component");
      ensureNotClosed();
      if (component.isClosed()) {
        throw new JniResourceException("Component has been closed");
      }

      try {
        final long instanceHandle =
            nativeInstantiateComponent(getNativeHandle(), component.getNativeHandle());
        JniValidation.requireValidHandle(instanceHandle, "instanceHandle");
        return new JniComponentInstanceHandle(instanceHandle);
      } catch (final Exception e) {
        if (e instanceof JniException) {
          throw new WasmException(e.getMessage(), e);
        }
        throw new WasmException("Failed to instantiate component", e);
      }
    }

    /**
     * Gets the number of active component instances.
     *
     * @return the number of active instances
     * @throws JniResourceException if this engine has been closed
     */
    public int getActiveInstancesCount() {
      ensureNotClosed();

      try {
        return nativeGetActiveInstancesCount(getNativeHandle());
      } catch (final Exception e) {
        throw new JniException("Failed to get active instances count", e);
      }
    }

    /**
     * Cleans up inactive component instances.
     *
     * <p>Removes references to component instances that are no longer active, freeing up resources
     * and preventing memory leaks.
     *
     * @return the number of instances that were cleaned up
     * @throws JniResourceException if this engine has been closed
     */
    public int cleanupInstances() {
      ensureNotClosed();

      try {
        return nativeCleanupInstances(getNativeHandle());
      } catch (final Exception e) {
        throw new JniException("Failed to cleanup instances", e);
      }
    }

    @Override
    protected void doClose() throws Exception {
      if (getNativeHandle() != 0) {
        nativeDestroyComponentEngine(getNativeHandle());
        LOGGER.fine(
            "Destroyed component engine with handle: 0x" + Long.toHexString(getNativeHandle()));
      }
    }

    @Override
    protected String getResourceType() {
      return "ComponentEngine";
    }

    public boolean isValid() {
      return !isClosed() && getNativeHandle() != 0;
    }
  }

  /**
   * JNI wrapper for component handles.
   *
   * <p>Represents a compiled WebAssembly component that can be instantiated multiple times.
   * Implements automatic resource cleanup through {@link JniResource}.
   */
  public static final class JniComponentHandle extends JniResource {

    /**
     * Creates a new component handle wrapper with the given native handle.
     *
     * @param nativeHandle the native component handle
     * @throws JniResourceException if nativeHandle is invalid
     */
    JniComponentHandle(final long nativeHandle) {
      super(nativeHandle);
      LOGGER.fine("Created component handle with handle: 0x" + Long.toHexString(nativeHandle));
    }

    /**
     * Gets the size of the component in bytes.
     *
     * @return the component size in bytes
     * @throws JniResourceException if this component has been closed
     */
    public long getSize() {
      ensureNotClosed();

      try {
        return nativeGetComponentSize(getNativeHandle());
      } catch (final Exception e) {
        throw new JniException("Failed to get component size", e);
      }
    }

    /**
     * Checks if the component exports the specified interface.
     *
     * @param interfaceName the interface name to check
     * @return true if the interface is exported, false otherwise
     * @throws JniResourceException if this component has been closed
     */
    public boolean exportsInterface(final String interfaceName) {
      JniValidation.requireNonEmpty(interfaceName, "interfaceName");
      ensureNotClosed();

      try {
        return nativeExportsInterface(getNativeHandle(), interfaceName);
      } catch (final Exception e) {
        throw new JniException("Failed to check exported interface", e);
      }
    }

    /**
     * Checks if the component imports the specified interface.
     *
     * @param interfaceName the interface name to check
     * @return true if the interface is imported, false otherwise
     * @throws JniResourceException if this component has been closed
     */
    public boolean importsInterface(final String interfaceName) {
      JniValidation.requireNonEmpty(interfaceName, "interfaceName");
      ensureNotClosed();

      try {
        return nativeImportsInterface(getNativeHandle(), interfaceName);
      } catch (final Exception e) {
        throw new JniException("Failed to check imported interface", e);
      }
    }

    @Override
    protected void doClose() throws Exception {
      if (getNativeHandle() != 0) {
        nativeDestroyComponent(getNativeHandle());
        LOGGER.fine(
            "Destroyed component with handle: 0x" + Long.toHexString(getNativeHandle()));
      }
    }

    @Override
    protected String getResourceType() {
      return "Component";
    }

    public boolean isValid() {
      return !isClosed() && getNativeHandle() != 0;
    }
  }

  /**
   * JNI wrapper for component instance handles.
   *
   * <p>Represents an instantiated WebAssembly component that can be used to call exported
   * functions and interact with component resources. Implements automatic resource cleanup through
   * {@link JniResource}.
   */
  public static final class JniComponentInstanceHandle extends JniResource {

    /**
     * Creates a new component instance handle wrapper with the given native handle.
     *
     * @param nativeHandle the native component instance handle
     * @throws JniResourceException if nativeHandle is invalid
     */
    JniComponentInstanceHandle(final long nativeHandle) {
      super(nativeHandle);
      LOGGER.fine(
          "Created component instance with handle: 0x" + Long.toHexString(nativeHandle));
    }

    @Override
    protected void doClose() throws Exception {
      if (getNativeHandle() != 0) {
        nativeDestroyComponentInstance(getNativeHandle());
        LOGGER.fine(
            "Destroyed component instance with handle: 0x" + Long.toHexString(getNativeHandle()));
      }
    }

    @Override
    protected String getResourceType() {
      return "ComponentInstance";
    }

    public boolean isValid() {
      return !isClosed() && getNativeHandle() != 0;
    }
  }
}