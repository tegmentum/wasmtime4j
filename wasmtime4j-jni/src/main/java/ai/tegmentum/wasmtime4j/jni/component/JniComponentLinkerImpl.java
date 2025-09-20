package ai.tegmentum.wasmtime4j.jni.component;

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
import ai.tegmentum.wasmtime4j.jni.JniEngine;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * JNI implementation of the ComponentLinker interface.
 *
 * <p>This class provides component linking capabilities through JNI calls to the native Wasmtime
 * component linker. It manages import definitions and handles component instantiation with proper
 * import resolution.
 *
 * @since 1.0.0
 */
public final class JniComponentLinkerImpl extends JniResource implements ComponentLinker {

  private static final Logger LOGGER = Logger.getLogger(JniComponentLinkerImpl.class.getName());

  private final Engine engine;
  private final JniComponentLinkerMetadataImpl metadata;
  private final ConcurrentMap<String, Object> importDefinitions = new ConcurrentHashMap<>();

  /**
   * Creates a new JNI component linker implementation.
   *
   * @param nativeHandle the native linker handle
   * @param engine the engine this linker is associated with
   * @throws IllegalArgumentException if engine is null
   * @throws JniResourceException if nativeHandle is invalid
   */
  public JniComponentLinkerImpl(final long nativeHandle, final Engine engine) {
    super(nativeHandle);

    JniValidation.requireNonNull(engine, "engine");
    JniValidation.requireValidHandle(nativeHandle, "nativeHandle");

    this.engine = engine;
    this.metadata = new JniComponentLinkerMetadataImpl(nativeHandle);

    LOGGER.fine("Created JNI component linker implementation with handle: 0x"
        + Long.toHexString(nativeHandle));
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
    JniValidation.requireNonNull(engine, "engine");

    if (!(engine instanceof JniEngine)) {
      throw new WasmException("Engine must be a JNI implementation");
    }

    final JniEngine jniEngine = (JniEngine) engine;
    if (jniEngine.isClosed()) {
      throw new WasmException("Engine is closed");
    }

    try {
      final long linkerHandle = nativeCreateLinker(jniEngine.getNativeHandle());
      JniValidation.requireValidHandle(linkerHandle, "linkerHandle");

      return new JniComponentLinkerImpl(linkerHandle, engine);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to create component linker", e);
    }
  }

  @Override
  public void defineComponent(final String name, final Component component) throws WasmException {
    JniValidation.requireNonEmpty(name, "name");
    JniValidation.requireNonNull(component, "component");
    ensureNotClosed();

    if (!(component instanceof JniComponentImpl)) {
      throw new WasmException("Component must be a JNI implementation");
    }

    final JniComponentImpl jniComponent = (JniComponentImpl) component;
    if (jniComponent.isClosed()) {
      throw new WasmException("Component is closed");
    }

    try {
      final boolean success = nativeDefineComponent(
          getNativeHandle(),
          name,
          jniComponent.getNativeHandle()
      );

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
    JniValidation.requireNonEmpty(name, "name");
    JniValidation.requireNonNull(interfaceType, "interfaceType");
    ensureNotClosed();

    if (!(interfaceType instanceof JniInterfaceTypeImpl)) {
      throw new WasmException("InterfaceType must be a JNI implementation");
    }

    final JniInterfaceTypeImpl jniInterface = (JniInterfaceTypeImpl) interfaceType;

    try {
      final boolean success = nativeDefineInterface(
          getNativeHandle(),
          name,
          jniInterface.getNativeHandle()
      );

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
    JniValidation.requireNonEmpty(name, "name");
    JniValidation.requireNonNull(function, "function");
    ensureNotClosed();

    if (!(function instanceof JniComponentFunctionImpl)) {
      throw new WasmException("ComponentFunction must be a JNI implementation");
    }

    final JniComponentFunctionImpl jniFunction = (JniComponentFunctionImpl) function;

    try {
      final boolean success = nativeDefineFunction(
          getNativeHandle(),
          name,
          jniFunction.getNativeHandle()
      );

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
    JniValidation.requireNonEmpty(name, "name");
    JniValidation.requireNonNull(resource, "resource");
    ensureNotClosed();

    if (!(resource instanceof JniComponentResourceImpl)) {
      throw new WasmException("ComponentResource must be a JNI implementation");
    }

    final JniComponentResourceImpl jniResource = (JniComponentResourceImpl) resource;

    try {
      final boolean success = nativeDefineResource(
          getNativeHandle(),
          name,
          jniResource.getNativeHandle()
      );

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
    JniValidation.requireNonNull(store, "store");
    JniValidation.requireNonNull(component, "component");
    ensureNotClosed();

    if (!(store instanceof ai.tegmentum.wasmtime4j.jni.JniStore)) {
      throw new WasmException("Store must be a JNI implementation");
    }

    if (!(component instanceof JniComponentImpl)) {
      throw new WasmException("Component must be a JNI implementation");
    }

    final ai.tegmentum.wasmtime4j.jni.JniStore jniStore =
        (ai.tegmentum.wasmtime4j.jni.JniStore) store;
    final JniComponentImpl jniComponent = (JniComponentImpl) component;

    if (jniStore.isClosed()) {
      throw new WasmException("Store is closed");
    }

    if (jniComponent.isClosed()) {
      throw new WasmException("Component is closed");
    }

    try {
      final long instanceHandle = nativeInstantiate(
          getNativeHandle(),
          jniStore.getNativeHandle(),
          jniComponent.getNativeHandle()
      );

      JniValidation.requireValidHandle(instanceHandle, "instanceHandle");

      final JniComponent.JniComponentInstanceHandle jniInstanceHandle =
          new JniComponent.JniComponentInstanceHandle(instanceHandle);

      return new JniComponentInstanceImpl(jniInstanceHandle, component, store);

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
    JniValidation.requireNonEmpty(name, "name");
    return importDefinitions.containsKey(name);
  }

  @Override
  public Optional<ComponentImportType> getImportType(final String name) {
    JniValidation.requireNonEmpty(name, "name");
    ensureNotClosed();

    try {
      final long importTypeHandle = nativeGetImportType(getNativeHandle(), name);
      if (importTypeHandle == 0) {
        return Optional.empty();
      }

      return Optional.of(new JniComponentImportTypeImpl(importTypeHandle, name));

    } catch (final Exception e) {
      LOGGER.warning("Failed to get import type for: " + name + " - " + e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public void validateImports(final Component component) throws WasmException {
    JniValidation.requireNonNull(component, "component");
    ensureNotClosed();

    if (!(component instanceof JniComponentImpl)) {
      throw new WasmException("Component must be a JNI implementation");
    }

    final JniComponentImpl jniComponent = (JniComponentImpl) component;
    if (jniComponent.isClosed()) {
      throw new WasmException("Component is closed");
    }

    try {
      final boolean isValid = nativeValidateImports(
          getNativeHandle(),
          jniComponent.getNativeHandle()
      );

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
      final long clonedHandle = nativeCloneLinker(getNativeHandle());
      JniValidation.requireValidHandle(clonedHandle, "clonedHandle");

      final ComponentLinker cloned = new JniComponentLinkerImpl(clonedHandle, engine);

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
    return !isClosed() && getNativeHandle() != 0;
  }

  @Override
  protected void doClose() throws Exception {
    importDefinitions.clear();

    if (getNativeHandle() != 0) {
      nativeDestroyLinker(getNativeHandle());
      LOGGER.fine("Destroyed JNI component linker with handle: 0x"
          + Long.toHexString(getNativeHandle()));
    }
  }

  @Override
  protected String getResourceType() {
    return "ComponentLinkerImpl";
  }

  // Native method declarations

  /**
   * Creates a new component linker.
   *
   * @param engineHandle the native engine handle
   * @return native linker handle or 0 on failure
   */
  private static native long nativeCreateLinker(long engineHandle);

  /**
   * Defines a component import.
   *
   * @param linkerHandle the native linker handle
   * @param name the import name
   * @param componentHandle the native component handle
   * @return true on success, false on failure
   */
  private static native boolean nativeDefineComponent(
      long linkerHandle, String name, long componentHandle);

  /**
   * Defines an interface import.
   *
   * @param linkerHandle the native linker handle
   * @param name the import name
   * @param interfaceHandle the native interface handle
   * @return true on success, false on failure
   */
  private static native boolean nativeDefineInterface(
      long linkerHandle, String name, long interfaceHandle);

  /**
   * Defines a function import.
   *
   * @param linkerHandle the native linker handle
   * @param name the import name
   * @param functionHandle the native function handle
   * @return true on success, false on failure
   */
  private static native boolean nativeDefineFunction(
      long linkerHandle, String name, long functionHandle);

  /**
   * Defines a resource import.
   *
   * @param linkerHandle the native linker handle
   * @param name the import name
   * @param resourceHandle the native resource handle
   * @return true on success, false on failure
   */
  private static native boolean nativeDefineResource(
      long linkerHandle, String name, long resourceHandle);

  /**
   * Instantiates a component with this linker.
   *
   * @param linkerHandle the native linker handle
   * @param storeHandle the native store handle
   * @param componentHandle the native component handle
   * @return native instance handle or 0 on failure
   */
  private static native long nativeInstantiate(
      long linkerHandle, long storeHandle, long componentHandle);

  /**
   * Gets import type information.
   *
   * @param linkerHandle the native linker handle
   * @param name the import name
   * @return native import type handle or 0 if not found
   */
  private static native long nativeGetImportType(long linkerHandle, String name);

  /**
   * Validates imports for a component.
   *
   * @param linkerHandle the native linker handle
   * @param componentHandle the native component handle
   * @return true if valid, false otherwise
   */
  private static native boolean nativeValidateImports(long linkerHandle, long componentHandle);

  /**
   * Clones a linker.
   *
   * @param linkerHandle the native linker handle
   * @return native cloned linker handle or 0 on failure
   */
  private static native long nativeCloneLinker(long linkerHandle);

  /**
   * Destroys a component linker.
   *
   * @param linkerHandle the native linker handle
   */
  private static native void nativeDestroyLinker(long linkerHandle);
}