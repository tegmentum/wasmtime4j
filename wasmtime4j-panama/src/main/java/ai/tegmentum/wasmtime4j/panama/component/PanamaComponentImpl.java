package ai.tegmentum.wasmtime4j.panama.component;

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.component.ComponentMetadata;
import ai.tegmentum.wasmtime4j.component.ComponentType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.ArenaResourceManager;
import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import ai.tegmentum.wasmtime4j.panama.PanamaComponent;
import ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler;
import ai.tegmentum.wasmtime4j.panama.PanamaStore;
import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the Component interface.
 *
 * <p>This class provides a bridge between the high-level Component interface and the low-level
 * Panama FFI component operations. It uses Arena-based resource management for optimal performance
 * and automatic cleanup.
 *
 * <p>The implementation maintains a handle to the native component and ensures proper resource
 * cleanup through the ArenaResourceManager.
 *
 * @since 1.0.0
 */
public final class PanamaComponentImpl implements Component {

  private static final Logger LOGGER = Logger.getLogger(PanamaComponentImpl.class.getName());

  private final ArenaResourceManager resourceManager;
  private final PanamaComponent.PanamaComponentHandle componentHandle;
  private final NativeFunctionBindings nativeFunctions;
  private final PanamaComponentMetadataImpl metadata;
  private final PanamaComponentTypeImpl componentType;
  private volatile boolean closed = false;

  /**
   * Creates a new Panama component implementation.
   *
   * @param resourceManager the arena resource manager for lifecycle management
   * @param componentHandle the underlying Panama component handle
   * @throws IllegalArgumentException if any parameter is null
   * @throws IllegalStateException if componentHandle is closed
   */
  public PanamaComponentImpl(
      final ArenaResourceManager resourceManager,
      final PanamaComponent.PanamaComponentHandle componentHandle) {

    this.resourceManager = Objects.requireNonNull(resourceManager, "resourceManager");
    this.componentHandle = Objects.requireNonNull(componentHandle, "componentHandle");
    this.nativeFunctions = NativeFunctionBindings.getInstance();

    if (!componentHandle.isValid()) {
      throw new IllegalStateException("Component handle is not valid");
    }

    this.metadata = new PanamaComponentMetadataImpl(resourceManager, componentHandle);
    this.componentType = new PanamaComponentTypeImpl(resourceManager, componentHandle);

    LOGGER.fine("Created Panama component implementation");
  }

  @Override
  public ComponentInstance instantiate(final Store store, final ComponentLinker linker)
      throws WasmException {
    Objects.requireNonNull(store, "store");
    Objects.requireNonNull(linker, "linker");
    ensureNotClosed();

    try {
      // Cast to Panama implementations to access native handles
      if (!(store instanceof PanamaStore)) {
        throw new WasmException("Store must be a Panama implementation");
      }

      if (!(linker instanceof PanamaComponentLinkerImpl)) {
        throw new WasmException("Linker must be a Panama implementation");
      }

      final PanamaStore panamaStore = (PanamaStore) store;
      final PanamaComponentLinkerImpl panamaLinker = (PanamaComponentLinkerImpl) linker;

      if (!panamaStore.isValid()) {
        throw new WasmException("Store is not valid");
      }

      if (!panamaLinker.isValid()) {
        throw new WasmException("Linker is not valid");
      }

      // Call native instantiation method
      final MemorySegment instancePtr =
          nativeInstantiateWithLinker(
              componentHandle.getResource(),
              panamaStore.getResourcePtr(),
              panamaLinker.getResourcePtr());

      PanamaErrorHandler.requireValidPointer(instancePtr, "instancePtr");

      final PanamaComponent.PanamaComponentInstanceHandle instanceHandle =
          new PanamaComponent.PanamaComponentInstanceHandle(resourceManager, instancePtr);

      return new PanamaComponentInstanceImpl(resourceManager, instanceHandle, this, store);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to instantiate component", e);
    }
  }

  @Override
  public ComponentType getType() throws WasmException {
    ensureNotClosed();
    return componentType;
  }

  @Override
  public byte[] serialize() throws WasmException {
    ensureNotClosed();

    try {
      final ArenaResourceManager.ManagedMemorySegment resultSegment =
          nativeSerializeComponent(componentHandle.getResource());

      if (resultSegment == null || resultSegment.segment().byteSize() == 0) {
        throw new WasmException("Failed to serialize component - empty result");
      }

      // Copy to Java byte array
      final byte[] result = new byte[(int) resultSegment.segment().byteSize()];
      MemorySegment.copy(resultSegment.segment(), 0, result, 0, result.length);

      return result;

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to serialize component", e);
    }
  }

  @Override
  public void validate() throws WasmException {
    ensureNotClosed();

    try {
      final boolean isValid = nativeValidateComponent(componentHandle.getResource());
      if (!isValid) {
        throw new WasmException("Component validation failed");
      }
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to validate component", e);
    }
  }

  @Override
  public ComponentMetadata getMetadata() {
    return metadata;
  }

  @Override
  public boolean isValid() {
    return !closed && componentHandle != null && componentHandle.isValid();
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      if (componentHandle != null) {
        componentHandle.close();
      }
      LOGGER.fine("Closed Panama component implementation");
    }
  }

  /**
   * Gets the underlying Panama component handle.
   *
   * @return the Panama component handle
   */
  public PanamaComponent.PanamaComponentHandle getComponentHandle() {
    return componentHandle;
  }

  /**
   * Gets the resource manager used by this component.
   *
   * @return the arena resource manager
   */
  public ArenaResourceManager getResourceManager() {
    return resourceManager;
  }

  /**
   * Ensures this component is not closed.
   *
   * @throws IllegalStateException if the component is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Component has been closed");
    }
  }

  // Native method implementations using Panama FFI

  /**
   * Instantiates a component with a store and linker.
   *
   * @param componentPtr the native component pointer
   * @param storePtr the native store pointer
   * @param linkerPtr the native linker pointer
   * @return native component instance pointer
   * @throws WasmException if instantiation fails
   */
  private MemorySegment nativeInstantiateWithLinker(
      final MemorySegment componentPtr, final MemorySegment storePtr, final MemorySegment linkerPtr)
      throws WasmException {
    try {
      return nativeFunctions.instantiateComponentWithLinker(componentPtr, storePtr, linkerPtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to instantiate component with linker", e);
    }
  }

  /**
   * Serializes a component to its binary representation.
   *
   * @param componentPtr the native component pointer
   * @return managed memory segment containing serialized bytes
   * @throws WasmException if serialization fails
   */
  private ArenaResourceManager.ManagedMemorySegment nativeSerializeComponent(
      final MemorySegment componentPtr) throws WasmException {
    try {
      final MemorySegment resultPtr = nativeFunctions.serializeComponent(componentPtr);
      if (resultPtr == null || resultPtr.equals(MemorySegment.NULL)) {
        return null;
      }

      // Get the size of the serialized data
      final long size = nativeFunctions.getSerializedComponentSize(resultPtr);
      if (size <= 0) {
        return null;
      }

      // Create managed memory segment
      final ArenaResourceManager.ManagedMemorySegment managedSegment =
          resourceManager.allocate((int) size);

      // Copy data from native result to managed segment
      MemorySegment.copy(resultPtr, 0, managedSegment.segment(), 0, size);

      // Clean up native result
      nativeFunctions.freeSerializedComponent(resultPtr);

      return managedSegment;

    } catch (final Exception e) {
      throw new WasmException("Failed to serialize component", e);
    }
  }

  /**
   * Validates a component for correctness.
   *
   * @param componentPtr the native component pointer
   * @return true if valid, false otherwise
   * @throws WasmException if validation fails
   */
  private boolean nativeValidateComponent(final MemorySegment componentPtr) throws WasmException {
    try {
      return nativeFunctions.validateComponent(componentPtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to validate component", e);
    }
  }
}
