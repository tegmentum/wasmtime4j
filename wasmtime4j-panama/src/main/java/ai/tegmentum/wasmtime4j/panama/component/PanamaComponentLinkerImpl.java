package ai.tegmentum.wasmtime4j.panama.component;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentFunction;
import ai.tegmentum.wasmtime4j.component.ComponentImportType;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.component.ComponentLinkerMetadata;
import ai.tegmentum.wasmtime4j.component.ComponentResource;
import ai.tegmentum.wasmtime4j.component.InterfaceType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.ArenaResourceManager;
import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import ai.tegmentum.wasmtime4j.panama.PanamaComponent;
import ai.tegmentum.wasmtime4j.panama.PanamaEngine;
import ai.tegmentum.wasmtime4j.panama.PanamaErrorHandler;
import ai.tegmentum.wasmtime4j.panama.PanamaStore;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the ComponentLinker interface.
 *
 * <p>This class provides component linking capabilities through Panama FFI calls to the native
 * Wasmtime component linker. It manages import definitions and handles component instantiation with
 * proper import resolution.
 *
 * @since 1.0.0
 */
public final class PanamaComponentLinkerImpl implements ComponentLinker {

  private static final Logger LOGGER = Logger.getLogger(PanamaComponentLinkerImpl.class.getName());

  private final ArenaResourceManager resourceManager;
  private final Engine engine;
  private final NativeFunctionBindings nativeFunctions;
  private final ArenaResourceManager.ManagedNativeResource linkerResource;
  private final PanamaComponentLinkerMetadataImpl metadata;
  private final ConcurrentMap<String, Object> importDefinitions = new ConcurrentHashMap<>();
  private volatile boolean closed = false;

  /**
   * Creates a new Panama component linker implementation.
   *
   * @param resourceManager the arena resource manager for lifecycle management
   * @param linkerPtr the native linker pointer
   * @param engine the engine this linker is associated with
   * @throws IllegalArgumentException if any parameter is null
   * @throws IllegalStateException if linkerPtr is null
   */
  public PanamaComponentLinkerImpl(
      final ArenaResourceManager resourceManager,
      final MemorySegment linkerPtr,
      final Engine engine) {

    this.resourceManager = Objects.requireNonNull(resourceManager, "resourceManager");
    this.engine = Objects.requireNonNull(engine, "engine");
    this.nativeFunctions = NativeFunctionBindings.getInstance();

    PanamaErrorHandler.requireValidPointer(linkerPtr, "linkerPtr");

    this.linkerResource =
        resourceManager.manageNativeResource(
            linkerPtr, () -> destroyNativeLinker(linkerPtr), "Wasmtime Component Linker");

    this.metadata = new PanamaComponentLinkerMetadataImpl(resourceManager, linkerResource);

    LOGGER.fine("Created Panama component linker implementation");
  }

  /**
   * Creates a new ComponentLinker for the given engine.
   *
   * @param engine the engine to create the linker for
   * @return a new ComponentLinker instance
   * @throws WasmException if linker creation fails
   * @throws IllegalArgumentException if engine is null
   */
  public static ComponentLinker create(final Engine engine) throws WasmException {
    Objects.requireNonNull(engine, "engine");

    if (!(engine instanceof PanamaEngine)) {
      throw new WasmException("Engine must be a Panama implementation");
    }

    final PanamaEngine panamaEngine = (PanamaEngine) engine;
    if (!panamaEngine.isValid()) {
      throw new WasmException("Engine is not valid");
    }

    try {
      final ArenaResourceManager resourceManager = panamaEngine.getResourceManager();
      final MemorySegment linkerPtr = createNativeLinker(panamaEngine.getResourcePtr());
      PanamaErrorHandler.requireValidPointer(linkerPtr, "linkerPtr");

      return new PanamaComponentLinkerImpl(resourceManager, linkerPtr, engine);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create component linker", e);
    }
  }

  @Override
  public void defineComponent(final String name, final Component component) throws WasmException {
    PanamaValidation.requireNonEmpty(name, "name");
    Objects.requireNonNull(component, "component");
    ensureNotClosed();

    if (!(component instanceof PanamaComponentImpl)) {
      throw new WasmException("Component must be a Panama implementation");
    }

    final PanamaComponentImpl panamaComponent = (PanamaComponentImpl) component;
    if (!panamaComponent.isValid()) {
      throw new WasmException("Component is not valid");
    }

    try {
      final boolean success =
          nativeDefineComponent(
              linkerResource.resource(), name, panamaComponent.getComponentHandle().getResource());

      if (!success) {
        throw new WasmException("Failed to define component: " + name);
      }

      importDefinitions.put(name, component);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to define component: " + name, e);
    }
  }

  @Override
  public void defineInterface(final String name, final InterfaceType interfaceType)
      throws WasmException {
    PanamaValidation.requireNonEmpty(name, "name");
    Objects.requireNonNull(interfaceType, "interfaceType");
    ensureNotClosed();

    if (!(interfaceType instanceof PanamaInterfaceTypeImpl)) {
      throw new WasmException("InterfaceType must be a Panama implementation");
    }

    final PanamaInterfaceTypeImpl panamaInterface = (PanamaInterfaceTypeImpl) interfaceType;

    try {
      final boolean success =
          nativeDefineInterface(linkerResource.resource(), name, panamaInterface.getResourcePtr());

      if (!success) {
        throw new WasmException("Failed to define interface: " + name);
      }

      importDefinitions.put(name, interfaceType);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to define interface: " + name, e);
    }
  }

  @Override
  public void defineFunction(final String name, final ComponentFunction function)
      throws WasmException {
    PanamaValidation.requireNonEmpty(name, "name");
    Objects.requireNonNull(function, "function");
    ensureNotClosed();

    if (!(function instanceof PanamaComponentFunctionImpl)) {
      throw new WasmException("ComponentFunction must be a Panama implementation");
    }

    final PanamaComponentFunctionImpl panamaFunction = (PanamaComponentFunctionImpl) function;

    try {
      final boolean success =
          nativeDefineFunction(linkerResource.resource(), name, panamaFunction.getResourcePtr());

      if (!success) {
        throw new WasmException("Failed to define function: " + name);
      }

      importDefinitions.put(name, function);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to define function: " + name, e);
    }
  }

  @Override
  public void defineResource(final String name, final ComponentResource resource)
      throws WasmException {
    PanamaValidation.requireNonEmpty(name, "name");
    Objects.requireNonNull(resource, "resource");
    ensureNotClosed();

    if (!(resource instanceof PanamaComponentResourceImpl)) {
      throw new WasmException("ComponentResource must be a Panama implementation");
    }

    final PanamaComponentResourceImpl panamaResource = (PanamaComponentResourceImpl) resource;

    try {
      final boolean success =
          nativeDefineResource(linkerResource.resource(), name, panamaResource.getResourcePtr());

      if (!success) {
        throw new WasmException("Failed to define resource: " + name);
      }

      importDefinitions.put(name, resource);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to define resource: " + name, e);
    }
  }

  @Override
  public ComponentInstance instantiate(final Store store, final Component component)
      throws WasmException {
    Objects.requireNonNull(store, "store");
    Objects.requireNonNull(component, "component");
    ensureNotClosed();

    if (!(store instanceof PanamaStore)) {
      throw new WasmException("Store must be a Panama implementation");
    }

    if (!(component instanceof PanamaComponentImpl)) {
      throw new WasmException("Component must be a Panama implementation");
    }

    final PanamaStore panamaStore = (PanamaStore) store;
    final PanamaComponentImpl panamaComponent = (PanamaComponentImpl) component;

    if (!panamaStore.isValid()) {
      throw new WasmException("Store is not valid");
    }

    if (!panamaComponent.isValid()) {
      throw new WasmException("Component is not valid");
    }

    try {
      final MemorySegment instancePtr =
          nativeInstantiate(
              linkerResource.resource(),
              panamaStore.getResourcePtr(),
              panamaComponent.getComponentHandle().getResource());

      PanamaErrorHandler.requireValidPointer(instancePtr, "instancePtr");

      final PanamaComponent.PanamaComponentInstanceHandle instanceHandle =
          new PanamaComponent.PanamaComponentInstanceHandle(resourceManager, instancePtr);

      return new PanamaComponentInstanceImpl(resourceManager, instanceHandle, component, store);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to instantiate component", e);
    }
  }

  @Override
  public List<String> getDefinedImports() {
    return new ArrayList<>(importDefinitions.keySet());
  }

  @Override
  public boolean hasImport(final String name) {
    PanamaValidation.requireNonEmpty(name, "name");
    return importDefinitions.containsKey(name);
  }

  @Override
  public Optional<ComponentImportType> getImportType(final String name) {
    PanamaValidation.requireNonEmpty(name, "name");
    ensureNotClosed();

    try {
      final MemorySegment importTypePtr = nativeGetImportType(linkerResource.resource(), name);
      if (importTypePtr == null || importTypePtr.equals(MemorySegment.NULL)) {
        return Optional.empty();
      }

      return Optional.of(new PanamaComponentImportTypeImpl(resourceManager, importTypePtr, name));

    } catch (final Exception e) {
      LOGGER.warning("Failed to get import type for: " + name + " - " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public void validateImports(final Component component) throws WasmException {
    Objects.requireNonNull(component, "component");
    ensureNotClosed();

    if (!(component instanceof PanamaComponentImpl)) {
      throw new WasmException("Component must be a Panama implementation");
    }

    final PanamaComponentImpl panamaComponent = (PanamaComponentImpl) component;
    if (!panamaComponent.isValid()) {
      throw new WasmException("Component is not valid");
    }

    try {
      final boolean isValid =
          nativeValidateImports(
              linkerResource.resource(), panamaComponent.getComponentHandle().getResource());

      if (!isValid) {
        throw new WasmException("Import validation failed for component");
      }

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to validate imports", e);
    }
  }

  @Override
  public ComponentLinkerMetadata getMetadata() {
    return metadata;
  }

  @Override
  public ComponentLinker clone() throws WasmException {
    ensureNotClosed();

    try {
      final MemorySegment clonedPtr = nativeCloneLinker(linkerResource.resource());
      PanamaErrorHandler.requireValidPointer(clonedPtr, "clonedPtr");

      final ComponentLinker cloned =
          new PanamaComponentLinkerImpl(resourceManager, clonedPtr, engine);

      // Copy import definitions
      for (final String name : importDefinitions.keySet()) {
        final Object definition = importDefinitions.get(name);
        if (definition instanceof Component) {
          cloned.defineComponent(name, (Component) definition);
        } else if (definition instanceof InterfaceType) {
          cloned.defineInterface(name, (InterfaceType) definition);
        } else if (definition instanceof ComponentFunction) {
          cloned.defineFunction(name, (ComponentFunction) definition);
        } else if (definition instanceof ComponentResource) {
          cloned.defineResource(name, (ComponentResource) definition);
        }
      }

      return cloned;

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to clone linker", e);
    }
  }

  @Override
  public boolean isValid() {
    return !closed && linkerResource != null && linkerResource.isValid();
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      importDefinitions.clear();
      if (linkerResource != null) {
        linkerResource.close();
      }
      LOGGER.fine("Closed Panama component linker implementation");
    }
  }

  /**
   * Gets the native resource pointer for internal use.
   *
   * @return the native linker pointer
   */
  public MemorySegment getResourcePtr() {
    ensureNotClosed();
    return linkerResource.resource();
  }

  /**
   * Ensures this linker is not closed.
   *
   * @throws IllegalStateException if the linker is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Component linker has been closed");
    }
  }

  // Native method implementations using Panama FFI

  /**
   * Creates a new component linker.
   *
   * @param enginePtr the native engine pointer
   * @return native linker pointer
   * @throws WasmException if creation fails
   */
  private static MemorySegment createNativeLinker(final MemorySegment enginePtr)
      throws WasmException {
    try {
      final NativeFunctionBindings nativeFunctions = NativeFunctionBindings.getInstance();
      return nativeFunctions.createComponentLinker(enginePtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to create native linker", e);
    }
  }

  /**
   * Defines a component import.
   *
   * @param linkerPtr the native linker pointer
   * @param name the import name
   * @param componentPtr the native component pointer
   * @return true on success, false on failure
   * @throws WasmException if operation fails
   */
  private boolean nativeDefineComponent(
      final MemorySegment linkerPtr, final String name, final MemorySegment componentPtr)
      throws WasmException {
    try {
      return nativeFunctions.defineComponentImport(linkerPtr, name, componentPtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to define component", e);
    }
  }

  /**
   * Defines an interface import.
   *
   * @param linkerPtr the native linker pointer
   * @param name the import name
   * @param interfacePtr the native interface pointer
   * @return true on success, false on failure
   * @throws WasmException if operation fails
   */
  private boolean nativeDefineInterface(
      final MemorySegment linkerPtr, final String name, final MemorySegment interfacePtr)
      throws WasmException {
    try {
      return nativeFunctions.defineInterfaceImport(linkerPtr, name, interfacePtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to define interface", e);
    }
  }

  /**
   * Defines a function import.
   *
   * @param linkerPtr the native linker pointer
   * @param name the import name
   * @param functionPtr the native function pointer
   * @return true on success, false on failure
   * @throws WasmException if operation fails
   */
  private boolean nativeDefineFunction(
      final MemorySegment linkerPtr, final String name, final MemorySegment functionPtr)
      throws WasmException {
    try {
      return nativeFunctions.defineFunctionImport(linkerPtr, name, functionPtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to define function", e);
    }
  }

  /**
   * Defines a resource import.
   *
   * @param linkerPtr the native linker pointer
   * @param name the import name
   * @param resourcePtr the native resource pointer
   * @return true on success, false on failure
   * @throws WasmException if operation fails
   */
  private boolean nativeDefineResource(
      final MemorySegment linkerPtr, final String name, final MemorySegment resourcePtr)
      throws WasmException {
    try {
      return nativeFunctions.defineResourceImport(linkerPtr, name, resourcePtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to define resource", e);
    }
  }

  /**
   * Instantiates a component with this linker.
   *
   * @param linkerPtr the native linker pointer
   * @param storePtr the native store pointer
   * @param componentPtr the native component pointer
   * @return native instance pointer
   * @throws WasmException if instantiation fails
   */
  private MemorySegment nativeInstantiate(
      final MemorySegment linkerPtr, final MemorySegment storePtr, final MemorySegment componentPtr)
      throws WasmException {
    try {
      return nativeFunctions.instantiateComponentWithLinker(linkerPtr, storePtr, componentPtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to instantiate", e);
    }
  }

  /**
   * Gets import type information.
   *
   * @param linkerPtr the native linker pointer
   * @param name the import name
   * @return native import type pointer or null if not found
   * @throws WasmException if operation fails
   */
  private MemorySegment nativeGetImportType(final MemorySegment linkerPtr, final String name)
      throws WasmException {
    try {
      return nativeFunctions.getComponentImportType(linkerPtr, name);
    } catch (final Exception e) {
      throw new WasmException("Failed to get import type", e);
    }
  }

  /**
   * Validates imports for a component.
   *
   * @param linkerPtr the native linker pointer
   * @param componentPtr the native component pointer
   * @return true if valid, false otherwise
   * @throws WasmException if validation fails
   */
  private boolean nativeValidateImports(
      final MemorySegment linkerPtr, final MemorySegment componentPtr) throws WasmException {
    try {
      return nativeFunctions.validateComponentImports(linkerPtr, componentPtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to validate imports", e);
    }
  }

  /**
   * Clones a linker.
   *
   * @param linkerPtr the native linker pointer
   * @return native cloned linker pointer
   * @throws WasmException if cloning fails
   */
  private MemorySegment nativeCloneLinker(final MemorySegment linkerPtr) throws WasmException {
    try {
      return nativeFunctions.cloneComponentLinker(linkerPtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to clone linker", e);
    }
  }

  /**
   * Destroys a component linker.
   *
   * @param linkerPtr the native linker pointer
   */
  private void destroyNativeLinker(final MemorySegment linkerPtr) {
    try {
      nativeFunctions.destroyComponentLinker(linkerPtr);
    } catch (final Exception e) {
      LOGGER.warning("Error destroying component linker: " + e.getMessage());
    }
  }
}
