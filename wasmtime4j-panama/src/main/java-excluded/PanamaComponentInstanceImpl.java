package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Component;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.ComponentInstanceState;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the ComponentInstance interface.
 *
 * <p>This class wraps a native WebAssembly component instance handle and provides Component Model
 * instance functionality through Panama FFI calls to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class PanamaComponentInstanceImpl implements ComponentInstance {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaComponentInstanceImpl.class.getName());

  private final PanamaComponent.PanamaComponentInstanceHandle nativeInstance;
  private final Component parentComponent;
  private final ArenaResourceManager resourceManager;
  private final ComponentInstanceConfig config;
  private final String instanceId;

  /**
   * Creates a new Panama component instance implementation.
   *
   * @param nativeInstance the native component instance handle
   * @param parentComponent the component that created this instance
   * @param resourceManager the arena resource manager
   * @param config the instance configuration
   */
  public PanamaComponentInstanceImpl(
      final PanamaComponent.PanamaComponentInstanceHandle nativeInstance,
      final Component parentComponent,
      final ArenaResourceManager resourceManager,
      final ComponentInstanceConfig config) {
    this.nativeInstance = Objects.requireNonNull(nativeInstance, "nativeInstance");
    this.parentComponent = Objects.requireNonNull(parentComponent, "parentComponent");
    this.resourceManager = Objects.requireNonNull(resourceManager, "resourceManager");
    this.config = Objects.requireNonNull(config, "config");
    this.instanceId = "panama-instance-" + System.nanoTime();
  }

  @Override
  public String getId() {
    return instanceId;
  }

  @Override
  public Component getComponent() {
    return parentComponent;
  }

  @Override
  public ComponentInstanceConfig getConfig() {
    return config;
  }

  @Override
  public ComponentInstanceState getState() {
    if (isValid()) {
      return ComponentInstanceState.ACTIVE;
    } else {
      return ComponentInstanceState.DISPOSED;
    }
  }

  @Override
  public boolean isValid() {
    return nativeInstance.isValid();
  }

  @Override
  public void close() {
    try {
      if (nativeInstance != null) {
        nativeInstance.close();
        LOGGER.fine("Closed Panama component instance: " + instanceId);
      }
    } catch (final Exception e) {
      LOGGER.warning("Error closing component instance: " + e.getMessage());
    }
  }

  private void ensureValid() throws WasmException {
    if (!isValid()) {
      throw new WasmException("Component instance is no longer valid");
    }
  }
}
