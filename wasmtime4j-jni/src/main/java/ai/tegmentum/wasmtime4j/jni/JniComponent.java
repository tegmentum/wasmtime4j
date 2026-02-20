package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeMethodBindings;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.util.Validation;
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

  /** Prevent instantiation - this class contains only static factory methods. */
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
  public static JniComponentEngine createComponentEngine() throws JniException {
    NativeMethodBindings.ensureInitialized();

    try {
      final long engineHandle = nativeCreateComponentEngine();
      Validation.requireValidHandle(engineHandle, "engineHandle");
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
   * Destroys a component instance by removing it from the engine's internal map.
   *
   * @param engineHandle the native component engine handle
   * @param instanceId the instance ID to destroy
   */
  static native void nativeDestroyComponentInstance(long engineHandle, long instanceId);

  /**
   * Compiles a WebAssembly component from WAT (WebAssembly Text format).
   *
   * @param engineHandle the native component engine handle
   * @param watText the WAT text to compile
   * @return component handle on success, 0 on failure
   */
  static native long nativeCompileComponentWat(long engineHandle, String watText);

  /**
   * Gets the number of exports from a component.
   *
   * @param componentHandle the native component handle
   * @return number of exports
   */
  static native int nativeGetComponentExportCount(long componentHandle);

  /**
   * Gets the number of imports required by a component.
   *
   * @param componentHandle the native component handle
   * @return number of imports
   */
  static native int nativeGetComponentImportCount(long componentHandle);

  /**
   * Gets the name of an exported interface by index.
   *
   * @param componentHandle the native component handle
   * @param index the export index
   * @return export interface name or null if index is out of bounds
   */
  static native String nativeGetComponentExportName(long componentHandle, int index);

  /**
   * Gets the name of an imported interface by index.
   *
   * @param componentHandle the native component handle
   * @param index the import index
   * @return import interface name or null if index is out of bounds
   */
  static native String nativeGetComponentImportName(long componentHandle, int index);

  /**
   * Checks if a component has a specific export.
   *
   * @param componentHandle the native component handle
   * @param exportName the export name to check
   * @return true if export exists, false otherwise
   */
  static native boolean nativeComponentHasExport(long componentHandle, String exportName);

  /**
   * Checks if a component requires a specific import.
   *
   * @param componentHandle the native component handle
   * @param importName the import name to check
   * @return true if import is required, false otherwise
   */
  static native boolean nativeComponentHasImport(long componentHandle, String importName);

  /**
   * Validates a component against WIT interface requirements.
   *
   * @param componentHandle the native component handle
   * @param witInterface the WIT interface to validate against
   * @return true if valid, false otherwise
   */
  static native boolean nativeComponentValidate(long componentHandle, String witInterface);

  /**
   * Cleanups unused component instances in the engine.
   *
   * @param engineHandle the native component engine handle
   * @return 0 on success, error code on failure
   */
  static native int nativeComponentEngineCleanupInstances(long engineHandle);

  /**
   * Checks if a component engine supports a specific feature.
   *
   * @param engineHandle the native component engine handle
   * @param featureName the feature name to check
   * @return true if supported, false otherwise
   */
  static native boolean nativeComponentEngineSupportsFeature(long engineHandle, String featureName);

  /**
   * Gets interface definition for a component export as JSON.
   *
   * @param componentHandle the native component handle
   * @param exportName the export name
   * @return JSON string of interface definition or null if not found
   */
  static native String nativeComponentGetExportInterface(long componentHandle, String exportName);

  /**
   * Creates a new WIT parser context.
   *
   * @return WIT parser handle on success, 0 on failure
   */
  static native long nativeWitParserNew();

  /**
   * Destroys a WIT parser context.
   *
   * @param parserHandle the WIT parser handle
   */
  static native void nativeWitParserDestroy(long parserHandle);

  /**
   * Parses a WIT interface definition.
   *
   * @param parserHandle the WIT parser handle
   * @param witText the WIT text to parse
   * @return interface handle on success, 0 on failure
   */
  static native long nativeWitParserParseInterface(long parserHandle, String witText);

  /**
   * Validates WIT interface syntax.
   *
   * @param parserHandle the WIT parser handle
   * @param witText the WIT text to validate
   * @return true if valid syntax, false otherwise
   */
  static native boolean nativeWitParserValidateSyntax(long parserHandle, String witText);

  /**
   * Invokes a component function with marshalled WIT values.
   *
   * <p>This method accepts parameters as marshalled WIT values (type discriminators and binary
   * data) and returns the result as a marshalled WIT value. The marshalling format follows the WIT
   * value serialization specification for cross-language interoperability.
   *
   * @param engineHandle the native component engine handle
   * @param instanceId the native component instance ID
   * @param functionName the name of the function to invoke
   * @param paramTypeDiscriminators array of type discriminators for parameters (1=bool, 2=s32,
   *     3=s64, 4=float64, 5=char, 6=string)
   * @param paramData array of serialized parameter data corresponding to each discriminator
   * @return two-element array: [0]=result type discriminator, [1]=result data as byte array, or
   *     null if function returns no value
   * @throws RuntimeException if function invocation fails or parameters are invalid
   */
  static native Object[] nativeComponentInvokeFunction(
      long engineHandle,
      long instanceId,
      String functionName,
      int[] paramTypeDiscriminators,
      byte[][] paramData);

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
      Validation.requireNonEmpty(wasmBytes, "wasmBytes");
      ensureNotClosed();

      final byte[] wasmBytesCopy = Validation.defensiveCopy(wasmBytes);

      try {
        final long componentHandle = nativeLoadComponentFromBytes(getNativeHandle(), wasmBytesCopy);
        Validation.requireValidHandle(componentHandle, "componentHandle");
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
      Validation.requireNonNull(component, "component");
      ensureNotClosed();
      if (component.isClosed()) {
        throw new JniResourceException("Component has been closed");
      }

      try {
        final long engineHandle = getNativeHandle();
        final long instanceId =
            nativeInstantiateComponent(engineHandle, component.getNativeHandle());
        Validation.requireValidHandle(instanceId, "instanceId");
        return new JniComponentInstanceHandle(engineHandle, instanceId);
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
     * @throws JniException if operation fails
     */
    public int getActiveInstancesCount() throws JniException {
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
     * @throws JniException if operation fails
     */
    public int cleanupInstances() throws JniException {
      ensureNotClosed();

      try {
        return nativeCleanupInstances(getNativeHandle());
      } catch (final Exception e) {
        throw new JniException("Failed to cleanup instances", e);
      }
    }

    @Override
    protected void doClose() throws Exception {
      if (nativeHandle != 0) {
        nativeDestroyComponentEngine(nativeHandle);
        LOGGER.fine("Destroyed component engine with handle: 0x" + Long.toHexString(nativeHandle));
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
     * @throws WasmException if operation fails
     */
    public long getSize() throws WasmException {
      ensureNotClosed();

      try {
        return nativeGetComponentSize(getNativeHandle());
      } catch (final Exception e) {
        throw new WasmException("Failed to get component size", e);
      }
    }

    /**
     * Checks if the component exports the specified interface.
     *
     * @param interfaceName the interface name to check
     * @return true if the interface is exported, false otherwise
     * @throws JniResourceException if this component has been closed
     * @throws WasmException if operation fails
     */
    public boolean exportsInterface(final String interfaceName) throws WasmException {
      Validation.requireNonEmpty(interfaceName, "interfaceName");
      ensureNotClosed();

      try {
        return nativeExportsInterface(getNativeHandle(), interfaceName);
      } catch (final Exception e) {
        throw new WasmException("Failed to check exported interface", e);
      }
    }

    /**
     * Checks if the component imports the specified interface.
     *
     * @param interfaceName the interface name to check
     * @return true if the interface is imported, false otherwise
     * @throws JniResourceException if this component has been closed
     * @throws WasmException if operation fails
     */
    public boolean importsInterface(final String interfaceName) throws WasmException {
      Validation.requireNonEmpty(interfaceName, "interfaceName");
      ensureNotClosed();

      try {
        return nativeImportsInterface(getNativeHandle(), interfaceName);
      } catch (final Exception e) {
        throw new WasmException("Failed to check imported interface", e);
      }
    }

    @Override
    protected void doClose() throws Exception {
      if (nativeHandle != 0) {
        nativeDestroyComponent(nativeHandle);
        LOGGER.fine("Destroyed component with handle: 0x" + Long.toHexString(nativeHandle));
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
   * <p>Represents an instantiated WebAssembly component that can be used to call exported functions
   * and interact with component resources. Implements automatic resource cleanup through {@link
   * JniResource}.
   */
  public static final class JniComponentInstanceHandle extends JniResource {

    /** The engine handle that owns this instance. Zero if instance is not engine-managed. */
    private final long engineHandle;

    /**
     * Creates a new component instance handle wrapper with the given native handle.
     *
     * <p>This constructor is for stub/placeholder instances that are not managed by an engine
     * (e.g., linker-based instantiation stubs). These instances will not be destroyed via native
     * call.
     *
     * @param instanceId the instance ID (not a raw pointer)
     * @throws JniResourceException if instanceId is invalid
     */
    JniComponentInstanceHandle(final long instanceId) {
      super(instanceId);
      this.engineHandle = 0;
      LOGGER.fine("Created unmanaged component instance with ID: " + instanceId);
    }

    /**
     * Creates a new component instance handle wrapper with the given native handle.
     *
     * @param engineHandle the native component engine handle that owns this instance
     * @param instanceId the instance ID (not a raw pointer)
     * @throws JniResourceException if instanceId is invalid
     */
    JniComponentInstanceHandle(final long engineHandle, final long instanceId) {
      super(instanceId);
      this.engineHandle = engineHandle;
      LOGGER.fine(
          "Created component instance with ID: "
              + instanceId
              + " in engine: 0x"
              + Long.toHexString(engineHandle));
    }

    /**
     * Gets the engine handle that owns this instance.
     *
     * @return the engine handle, or 0 if not engine-managed
     */
    public long getEngineHandle() {
      return engineHandle;
    }

    @Override
    protected void doClose() throws Exception {
      if (nativeHandle != 0 && engineHandle != 0) {
        nativeDestroyComponentInstance(engineHandle, nativeHandle);
        LOGGER.fine(
            "Destroyed component instance with ID: "
                + nativeHandle
                + " from engine: 0x"
                + Long.toHexString(engineHandle));
      } else if (nativeHandle != 0) {
        // Unmanaged instance - no native cleanup needed
        LOGGER.fine("Closed unmanaged component instance with ID: " + nativeHandle);
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
