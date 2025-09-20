package ai.tegmentum.wasmtime4j.jni.component;

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentExport;
import ai.tegmentum.wasmtime4j.component.ComponentExportType;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceStats;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.JniComponent;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * JNI implementation of the ComponentInstance interface.
 *
 * <p>This class provides a bridge between the high-level ComponentInstance interface and the
 * low-level JNI component instance operations. It manages the lifecycle of component exports and
 * provides type-safe access to component functionality.
 *
 * @since 1.0.0
 */
public final class JniComponentInstanceImpl extends JniResource implements ComponentInstance {

  private static final Logger LOGGER = Logger.getLogger(JniComponentInstanceImpl.class.getName());

  private final JniComponent.JniComponentInstanceHandle instanceHandle;
  private final Component parentComponent;
  private final Store store;
  private final JniComponentInstanceStatsImpl stats;
  private final Map<String, ComponentExport> exportCache = new ConcurrentHashMap<>();

  /**
   * Creates a new JNI component instance implementation.
   *
   * @param instanceHandle the underlying JNI component instance handle
   * @param parentComponent the component that created this instance
   * @param store the store used for execution
   * @throws IllegalArgumentException if any parameter is null
   * @throws JniResourceException if instanceHandle is invalid or closed
   */
  public JniComponentInstanceImpl(
      final JniComponent.JniComponentInstanceHandle instanceHandle,
      final Component parentComponent,
      final Store store) {
    super(instanceHandle != null ? instanceHandle.getNativeHandle() : 0);

    JniValidation.requireNonNull(instanceHandle, "instanceHandle");
    JniValidation.requireNonNull(parentComponent, "parentComponent");
    JniValidation.requireNonNull(store, "store");

    if (instanceHandle.isClosed()) {
      throw new JniResourceException("Component instance handle is closed");
    }

    this.instanceHandle = instanceHandle;
    this.parentComponent = parentComponent;
    this.store = store;
    this.stats = new JniComponentInstanceStatsImpl(instanceHandle);

    LOGGER.fine(
        "Created JNI component instance implementation with handle: 0x"
            + Long.toHexString(instanceHandle.getNativeHandle()));
  }

  @Override
  public Optional<ComponentExport> getExport(final String name) throws WasmException {
    JniValidation.requireNonEmpty(name, "name");
    ensureNotClosed();

    // Check cache first
    ComponentExport cached = exportCache.get(name);
    if (cached != null) {
      return Optional.of(cached);
    }

    try {
      final long exportHandle = nativeGetExport(getNativeHandle(), name);
      if (exportHandle == 0) {
        return Optional.empty();
      }

      final ComponentExport export = new JniComponentExportImpl(exportHandle, name, this);
      exportCache.put(name, export);
      return Optional.of(export);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to get export: " + name, e);
    }
  }

  @Override
  public Map<String, ComponentExport> getExports() throws WasmException {
    ensureNotClosed();

    try {
      final String[] exportNames = nativeGetExportNames(getNativeHandle());
      if (exportNames == null) {
        return Collections.emptyMap();
      }

      final Map<String, ComponentExport> exports = new HashMap<>();
      for (final String name : exportNames) {
        final Optional<ComponentExport> export = getExport(name);
        export.ifPresent(e -> exports.put(name, e));
      }

      return Collections.unmodifiableMap(exports);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to get exports", e);
    }
  }

  @Override
  public List<String> getExportNames() throws WasmException {
    ensureNotClosed();

    try {
      final String[] exportNames = nativeGetExportNames(getNativeHandle());
      if (exportNames == null) {
        return Collections.emptyList();
      }

      final List<String> names = new ArrayList<>();
      for (final String name : exportNames) {
        if (name != null && !name.trim().isEmpty()) {
          names.add(name);
        }
      }

      return Collections.unmodifiableList(names);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to get export names", e);
    }
  }

  @Override
  public boolean hasExport(final String name) throws WasmException {
    JniValidation.requireNonEmpty(name, "name");
    ensureNotClosed();

    try {
      return nativeHasExport(getNativeHandle(), name);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to check export: " + name, e);
    }
  }

  @Override
  public Optional<ComponentExportType> getExportType(final String name) throws WasmException {
    JniValidation.requireNonEmpty(name, "name");
    ensureNotClosed();

    try {
      final long exportTypeHandle = nativeGetExportType(getNativeHandle(), name);
      if (exportTypeHandle == 0) {
        return Optional.empty();
      }

      return Optional.of(new JniComponentExportTypeImpl(exportTypeHandle, name));

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to get export type: " + name, e);
    }
  }

  @Override
  public Component getComponent() {
    return parentComponent;
  }

  @Override
  public ComponentInstanceStats getStats() {
    return stats;
  }

  @Override
  public boolean isValid() {
    return !isClosed() && instanceHandle != null && instanceHandle.isValid();
  }

  @Override
  protected void doClose() throws Exception {
    // Clear export cache
    exportCache.clear();

    if (instanceHandle != null && !instanceHandle.isClosed()) {
      instanceHandle.close();
      LOGGER.fine(
          "Closed JNI component instance implementation with handle: 0x"
              + Long.toHexString(getNativeHandle()));
    }
  }

  @Override
  protected String getResourceType() {
    return "ComponentInstanceImpl";
  }

  /**
   * Gets the underlying JNI component instance handle.
   *
   * @return the JNI component instance handle
   */
  public JniComponent.JniComponentInstanceHandle getInstanceHandle() {
    return instanceHandle;
  }

  /**
   * Gets the store used by this instance.
   *
   * @return the store
   */
  public Store getStore() {
    return store;
  }

  // Native method declarations

  /**
   * Gets a component export by name.
   *
   * @param instanceHandle the native component instance handle
   * @param name the export name
   * @return native export handle or 0 if not found
   */
  private static native long nativeGetExport(long instanceHandle, String name);

  /**
   * Gets all export names from the component instance.
   *
   * @param instanceHandle the native component instance handle
   * @return array of export names or null on failure
   */
  private static native String[] nativeGetExportNames(long instanceHandle);

  /**
   * Checks if an export exists.
   *
   * @param instanceHandle the native component instance handle
   * @param name the export name
   * @return true if export exists, false otherwise
   */
  private static native boolean nativeHasExport(long instanceHandle, String name);

  /**
   * Gets export type information.
   *
   * @param instanceHandle the native component instance handle
   * @param name the export name
   * @return native export type handle or 0 if not found
   */
  private static native long nativeGetExportType(long instanceHandle, String name);

  /**
   * Gets statistics information for the component instance.
   *
   * @param instanceHandle the native component instance handle
   * @return statistics structure or null on failure
   */
  static native JniComponentInstanceStatsImpl.StatsStruct nativeGetInstanceStats(
      long instanceHandle);
}
