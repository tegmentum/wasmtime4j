package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import java.util.HashSet;
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
      final int exportCount =
          JniComponent.nativeGetComponentExportCount(nativeComponent.getNativeHandle());

      // For now, generate placeholder names based on export count
      // Full implementation would enumerate actual export names
      for (int i = 0; i < exportCount; i++) {
        exports.add("export-" + i);
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
      final int importCount =
          JniComponent.nativeGetComponentImportCount(nativeComponent.getNativeHandle());

      // For now, generate placeholder names based on import count
      // Full implementation would enumerate actual import names
      for (int i = 0; i < importCount; i++) {
        imports.add("import-" + i);
      }

      return imports;
    } catch (final Exception e) {
      throw new WasmException("Failed to get imported interfaces", e);
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

    return WitCompatibilityResult.compatible(
        "Full WIT compatibility (stub implementation)", new HashSet<>());
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
