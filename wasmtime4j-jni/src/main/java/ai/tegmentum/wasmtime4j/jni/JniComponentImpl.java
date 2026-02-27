package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.ResourcesRequired;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentExportIndex;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.component.ComponentTypeCodec;
import ai.tegmentum.wasmtime4j.component.ComponentTypeInfo;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of the Component interface.
 *
 * <p>This class wraps a native WebAssembly component handle and provides Component Model
 * functionality through JNI calls to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class JniComponentImpl implements Component {

  private static final Logger LOGGER = Logger.getLogger(JniComponentImpl.class.getName());

  private final JniComponent.JniComponentHandle nativeComponent;
  private final JniComponentEngine engine;
  private final String componentId;

  /**
   * Creates a new JNI component implementation.
   *
   * @param nativeComponent the native component handle
   * @param engine the component engine that created this component
   */
  public JniComponentImpl(
      final JniComponent.JniComponentHandle nativeComponent, final JniComponentEngine engine) {
    Validation.requireNonNull(nativeComponent, "nativeComponent");
    Validation.requireNonNull(engine, "engine");
    this.nativeComponent = nativeComponent;
    this.engine = engine;
    this.componentId = "jni-component-" + System.nanoTime();
  }

  @Override
  public String getId() {
    return componentId;
  }

  @Override
  public long getSize() throws WasmException {
    ensureValid();

    try {
      return nativeComponent.getSize();
    } catch (final Exception e) {
      throw new WasmException("Failed to get component size", e);
    }
  }

  /**
   * Gets the component engine that created this component.
   *
   * @return the component engine
   */
  public JniComponentEngine getEngine() {
    return engine;
  }

  @Override
  public boolean exportsInterface(final String interfaceName) throws WasmException {
    Validation.requireNonEmpty(interfaceName, "interfaceName");
    ensureValid();

    try {
      return nativeComponent.exportsInterface(interfaceName);
    } catch (final Exception e) {
      throw new WasmException("Failed to check exported interface", e);
    }
  }

  @Override
  public boolean importsInterface(final String interfaceName) throws WasmException {
    Validation.requireNonEmpty(interfaceName, "interfaceName");
    ensureValid();

    try {
      return nativeComponent.importsInterface(interfaceName);
    } catch (final Exception e) {
      throw new WasmException("Failed to check imported interface", e);
    }
  }

  @Override
  public Set<String> getExportedInterfaces() throws WasmException {
    ensureValid();

    try {
      final Set<String> exports = new HashSet<>();
      final long handle = nativeComponent.getNativeHandle();
      final int exportCount = JniComponent.nativeGetComponentExportCount(handle);

      for (int i = 0; i < exportCount; i++) {
        final String name = JniComponent.nativeGetComponentExportName(handle, i);
        if (name != null) {
          exports.add(name);
        }
      }

      return exports;
    } catch (final Exception e) {
      throw new WasmException("Failed to get exported interfaces", e);
    }
  }

  @Override
  public Set<String> getImportedInterfaces() throws WasmException {
    ensureValid();

    try {
      final Set<String> imports = new HashSet<>();
      final long handle = nativeComponent.getNativeHandle();
      final int importCount = JniComponent.nativeGetComponentImportCount(handle);

      for (int i = 0; i < importCount; i++) {
        final String name = JniComponent.nativeGetComponentImportName(handle, i);
        if (name != null) {
          imports.add(name);
        }
      }

      return imports;
    } catch (final Exception e) {
      throw new WasmException("Failed to get imported interfaces", e);
    }
  }

  @Override
  public ComponentTypeInfo componentType() throws WasmException {
    ensureValid();
    try {
      final String json =
          JniComponent.nativeGetFullComponentTypeJson(
              nativeComponent.getNativeHandle(), engine.getNativeHandle());
      if (json == null) {
        return Component.super.componentType();
      }
      return ComponentTypeCodec.deserialize(json);
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.FINE, "Full component type not available, falling back to name-only", e);
      return Component.super.componentType();
    }
  }

  @Override
  public ComponentInstance instantiate() throws WasmException {
    return instantiate(new ComponentInstanceConfig());
  }

  @Override
  public ComponentInstance instantiate(final ComponentInstanceConfig config) throws WasmException {
    Validation.requireNonNull(config, "config");
    ensureValid();

    try {
      final JniComponent.JniComponentInstanceHandle instanceHandle =
          engine.instantiateComponent(nativeComponent);
      return new JniComponentInstanceImpl(instanceHandle, this, config);
    } catch (final Exception e) {
      throw new WasmException("Failed to instantiate component", e);
    }
  }

  @Override
  public WitInterfaceDefinition getWitInterface() throws WasmException {
    ensureValid();

    try {
      // Create a basic WIT interface definition based on component metadata
      // In a full implementation, this would parse actual WIT definitions from the component
      return new JniWitInterfaceDefinition(
          "component-interface-" + componentId,
          "1.0.0",
          "ai.tegmentum.wasmtime4j",
          getExportedInterfaces(),
          getImportedInterfaces());
    } catch (final Exception e) {
      throw new WasmException("Failed to get WIT interface", e);
    }
  }

  @Override
  public WitCompatibilityResult checkWitCompatibility(final Component other) throws WasmException {
    Validation.requireNonNull(other, "other");
    ensureValid();

    final WitInterfaceDefinition myInterface = getWitInterface();
    final WitInterfaceDefinition otherInterface = other.getWitInterface();

    return myInterface.isCompatibleWith(otherInterface);
  }

  @Override
  public Optional<ComponentExportIndex> exportIndex(
      final ComponentExportIndex instanceIndex, final String name) throws WasmException {
    Validation.requireNonEmpty(name, "name");
    ensureValid();

    try {
      final long parentPtr = instanceIndex != null ? instanceIndex.getNativeHandle() : 0;
      final long indexPtr =
          JniComponent.nativeGetExportIndex(nativeComponent.getNativeHandle(), parentPtr, name);
      if (indexPtr == 0) {
        return Optional.empty();
      }
      return Optional.of(new JniComponentExportIndex(indexPtr));
    } catch (final Exception e) {
      throw new WasmException("Failed to get export index for '" + name + "'", e);
    }
  }

  @Override
  public byte[] serialize() throws WasmException {
    ensureValid();

    try {
      final byte[] serialized =
          JniComponent.nativeSerializeComponent(nativeComponent.getNativeHandle());
      if (serialized == null) {
        throw new WasmException("Failed to serialize component: native call returned null");
      }
      return serialized;
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Failed to serialize component", e);
    }
  }

  @Override
  public Optional<ResourcesRequired> resourcesRequired() throws WasmException {
    ensureValid();

    try {
      final long[] data =
          JniComponent.nativeGetComponentResourcesRequired(nativeComponent.getNativeHandle());
      if (data == null || data.length < 4) {
        return Optional.empty();
      }

      // -2 sentinel means resources_required() returned None
      if (data[0] == -2) {
        return Optional.empty();
      }

      final int numMemories = (int) data[0];
      final long maxMemory = data[1]; // -1 means unbounded
      final int numTables = (int) data[2];
      final long maxTable = data[3]; // -1 means unbounded

      return Optional.of(
          new ResourcesRequired(
              0L, // minimumMemoryBytes - not available for components
              maxMemory, // maximumMemoryBytes (-1 if unbounded)
              0, // minimumTableElements - not available for components
              maxTable > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) maxTable,
              numMemories,
              numTables,
              0, // numGlobals - not available for components
              0)); // numFunctions - not available for components
    } catch (final Exception e) {
      throw new WasmException("Failed to get component resources required", e);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.ImageRange imageRange() throws WasmException {
    ensureValid();

    try {
      final long[] range =
          JniComponent.nativeGetComponentImageRange(nativeComponent.getNativeHandle());
      if (range == null || range.length < 2) {
        throw new WasmException("Failed to get component image range");
      }
      return new ai.tegmentum.wasmtime4j.ImageRange(range[0], range[1]);
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Failed to get component image range", e);
    }
  }

  @Override
  public void initializeCopyOnWriteImage() throws WasmException {
    ensureValid();

    JniComponent.nativeInitializeCopyOnWriteImage(nativeComponent.getNativeHandle());
  }

  @Override
  public ai.tegmentum.wasmtime4j.component.ComponentEngine getComponentEngine() {
    return engine;
  }

  @Override
  public boolean isValid() {
    return !nativeComponent.isClosed() && nativeComponent.isValid();
  }

  /**
   * Returns the native handle for this component.
   *
   * @return the native component handle
   */
  public long getNativeHandle() {
    return nativeComponent.getNativeHandle();
  }

  @Override
  public void close() {
    if (nativeComponent != null && !nativeComponent.isClosed()) {
      try {
        nativeComponent.close();
        LOGGER.fine("Closed component: " + componentId);
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Error closing component: " + componentId, e);
      }
    }
  }

  private void ensureValid() throws WasmException {
    if (!isValid()) {
      throw new WasmException("Component is no longer valid");
    }
  }
}
