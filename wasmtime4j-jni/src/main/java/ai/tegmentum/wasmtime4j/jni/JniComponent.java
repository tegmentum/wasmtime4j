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
   * Serializes a component to a byte array.
   *
   * @param componentHandle the native component handle
   * @return the serialized component bytes, or null on failure
   */
  static native byte[] nativeSerializeComponent(long componentHandle);

  /**
   * Gets the image range of a compiled component.
   *
   * @param componentHandle the native component handle
   * @return long[2] = [start, end] or null on failure
   */
  static native long[] nativeGetComponentImageRange(long componentHandle);

  /**
   * Pre-initializes a component's copy-on-write image for faster instantiation.
   *
   * @param componentHandle the native component handle
   * @return true on success, false on failure
   */
  static native boolean nativeInitializeCopyOnWriteImage(long componentHandle);

  /**
   * Deserializes a component from previously serialized bytes.
   *
   * @param engineHandle the native component engine handle
   * @param serializedData the serialized component data
   * @return native component handle or 0 on failure
   */
  static native long nativeDeserializeComponent(long engineHandle, byte[] serializedData);

  /**
   * Deserializes a component from a previously serialized file.
   *
   * @param engineHandle the native component engine handle
   * @param filePath the path to the serialized component file
   * @return native component handle or 0 on failure
   */
  static native long nativeDeserializeComponentFile(long engineHandle, String filePath);

  /**
   * Gets the resources required by a component.
   *
   * @param componentHandle the native component handle
   * @return long array with [numMemories, maxMemorySize, numTables, maxTableSize], or null on
   *     failure. Values of -2 indicate resources_required() returned None. Values of -1 indicate
   *     unbounded.
   */
  static native long[] nativeGetComponentResourcesRequired(long componentHandle);

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
   * Checks if a component instance has a specific function export.
   *
   * @param engineHandle the engine handle
   * @param instanceId the instance ID
   * @param functionName the function name to check
   * @return 1 if found, 0 if not found
   */
  static native int nativeComponentInstanceHasFunc(
      long engineHandle, long instanceId, String functionName);

  /**
   * Looks up a core module exported by a component instance.
   *
   * @param engineHandle the engine handle
   * @param instanceId the instance ID
   * @param moduleName the module name to look up
   * @return the module handle pointer, or 0 if not found
   */
  static native long nativeComponentInstanceGetModule(
      long engineHandle, long instanceId, String moduleName);

  /**
   * Checks if a resource type is exported by a component instance.
   *
   * @param engineHandle the engine handle
   * @param instanceId the instance ID
   * @param resourceName the resource name to check
   * @return 1 if found, 0 if not found
   */
  static native int nativeComponentInstanceHasResource(
      long engineHandle, long instanceId, String resourceName);

  /**
   * Gets a pre-computed export index for efficient repeated lookups.
   *
   * @param componentHandle the component handle
   * @param instanceIndexPtr optional parent instance export index pointer (0 for root-level)
   * @param name the export name
   * @return the export index pointer, or 0 if not found
   */
  static native long nativeGetExportIndex(long componentHandle, long instanceIndexPtr, String name);

  /**
   * Destroys a component export index.
   *
   * @param indexPtr the export index pointer
   */
  static native void nativeDestroyExportIndex(long indexPtr);

  /**
   * Checks if a component instance has a function at the given export index.
   *
   * @param engineHandle the engine handle
   * @param instanceId the instance ID
   * @param indexPtr the export index pointer
   * @return 1 if found, 0 if not found
   */
  static native int nativeComponentInstanceHasFuncByIndex(
      long engineHandle, long instanceId, long indexPtr);

  /**
   * Looks up a general export by name on a component instance.
   *
   * <p>Returns a long[] of [kindCode, exportIndexPtr] on success, or null if not found. Kind codes:
   * 0=ComponentFunc, 1=CoreFunc, 2=Module, 3=Component, 4=ComponentInstance, 5=Type, 6=Resource.
   *
   * @param engineHandle the engine handle
   * @param instanceId the instance ID
   * @param parentIndexPtr the parent export index pointer (0 for root-level exports)
   * @param name the export name to look up
   * @return long[] of [kind, exportIndexPtr], or null if not found
   */
  static native long[] nativeComponentInstanceGetExport(
      long engineHandle, long instanceId, long parentIndexPtr, String name);

  /**
   * Executes multiple component function calls concurrently using native Wasmtime support.
   *
   * <p>The input and output are JSON strings matching the Rust {@code concurrent_call_json} format.
   *
   * @param engineHandle the engine handle
   * @param instanceId the instance ID
   * @param jsonInput JSON array of concurrent calls
   * @return JSON array of result arrays
   */
  static native String nativeRunConcurrentCalls(
      long engineHandle, long instanceId, String jsonInput);

  /**
   * Gets the full component type as a JSON string with complete type information.
   *
   * @param componentHandle the component handle
   * @param engineHandle the engine handle
   * @return JSON string with full type info, or null on error
   */
  static native String nativeGetFullComponentTypeJson(long componentHandle, long engineHandle);

  /**
   * Gets the substituted component type as a JSON string.
   *
   * @param linkerHandle the linker handle
   * @param componentHandle the component handle
   * @return JSON string with substituted type info, or null on error
   */
  static native String nativeGetSubstitutedComponentTypeJson(
      long linkerHandle, long componentHandle);

  /**
   * Closes an async val handle (Future/Stream/ErrorContext) in the native AsyncValRegistry.
   *
   * <p>This removes the handle from the global registry, dropping the stored Val and releasing any
   * native resources. Safe to call with handles that have already been consumed or closed (no-op).
   *
   * @param handle the async val handle to close
   */
  static native void nativeAsyncValClose(long handle);

  /**
   * Drops a ResourceAny held in the global resource registry.
   *
   * <p>Takes the resource from the registry and calls resource_drop on it using the store
   * associated with the given component instance.
   *
   * @param engineHandle the enhanced component engine handle
   * @param instanceId the component instance ID that owns the store
   * @param resourceHandle the resource handle ID from the global registry
   */
  static native void nativeResourceAnyDrop(long engineHandle, long instanceId, long resourceHandle);

  /**
   * Creates a {@link Runnable} close action that invokes {@link #nativeAsyncValClose(long)} for the
   * given handle. Suitable for use with {@link
   * ai.tegmentum.wasmtime4j.component.StreamAny#create(long, Runnable)}, {@link
   * ai.tegmentum.wasmtime4j.component.FutureAny#create(long, Runnable)}, and {@link
   * ai.tegmentum.wasmtime4j.component.ErrorContext#create(long, Runnable)}.
   *
   * @param handle the async val handle
   * @return a Runnable that will close the handle when invoked
   */
  public static Runnable createAsyncValCloseAction(final long handle) {
    return () -> nativeAsyncValClose(handle);
  }

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
