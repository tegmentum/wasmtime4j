package ai.tegmentum.wasmtime4j.panama.component;

import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentExport;
import ai.tegmentum.wasmtime4j.component.ComponentExportType;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceStats;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.ArenaResourceManager;
import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import ai.tegmentum.wasmtime4j.panama.PanamaComponent;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the ComponentInstance interface.
 *
 * <p>This class provides a bridge between the high-level ComponentInstance interface and the
 * low-level Panama FFI component instance operations. It manages the lifecycle of component exports
 * and provides type-safe access to component functionality.
 *
 * @since 1.0.0
 */
public final class PanamaComponentInstanceImpl implements ComponentInstance {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaComponentInstanceImpl.class.getName());

  private final ArenaResourceManager resourceManager;
  private final PanamaComponent.PanamaComponentInstanceHandle instanceHandle;
  private final Component parentComponent;
  private final Store store;
  private final NativeFunctionBindings nativeFunctions;
  private final PanamaComponentInstanceStatsImpl stats;
  private final Map<String, ComponentExport> exportCache = new ConcurrentHashMap<>();
  private volatile boolean closed = false;

  /**
   * Creates a new Panama component instance implementation.
   *
   * @param resourceManager the arena resource manager for lifecycle management
   * @param instanceHandle the underlying Panama component instance handle
   * @param parentComponent the component that created this instance
   * @param store the store used for execution
   * @throws IllegalArgumentException if any parameter is null
   * @throws IllegalStateException if instanceHandle is not valid
   */
  public PanamaComponentInstanceImpl(
      final ArenaResourceManager resourceManager,
      final PanamaComponent.PanamaComponentInstanceHandle instanceHandle,
      final Component parentComponent,
      final Store store) {

    this.resourceManager = Objects.requireNonNull(resourceManager, "resourceManager");
    this.instanceHandle = Objects.requireNonNull(instanceHandle, "instanceHandle");
    this.parentComponent = Objects.requireNonNull(parentComponent, "parentComponent");
    this.store = Objects.requireNonNull(store, "store");
    this.nativeFunctions = NativeFunctionBindings.getInstance();

    if (!instanceHandle.isValid()) {
      throw new IllegalStateException("Component instance handle is not valid");
    }

    this.stats = new PanamaComponentInstanceStatsImpl(resourceManager, instanceHandle);

    LOGGER.fine("Created Panama component instance implementation");
  }

  @Override
  public Optional<ComponentExport> getExport(final String name) throws WasmException {
    PanamaValidation.requireNonEmpty(name, "name");
    ensureNotClosed();

    // Check cache first
    ComponentExport cached = exportCache.get(name);
    if (cached != null) {
      return Optional.of(cached);
    }

    try {
      final MemorySegment exportPtr = nativeGetExport(instanceHandle.getResource(), name);
      if (exportPtr == null || exportPtr.equals(MemorySegment.NULL)) {
        return Optional.empty();
      }

      final ComponentExport export =
          new PanamaComponentExportImpl(resourceManager, exportPtr, name, this);
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
      final List<String> exportNames = getExportNames();
      if (exportNames.isEmpty()) {
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
      final ArenaResourceManager.ManagedMemorySegment namesSegment =
          nativeGetExportNames(instanceHandle.getResource());

      if (namesSegment == null || namesSegment.segment().byteSize() == 0) {
        return Collections.emptyList();
      }

      // Parse the names from the native memory segment
      final List<String> names = new ArrayList<>();
      final String[] nameArray = parseStringArray(namesSegment.segment());

      for (final String name : nameArray) {
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
    PanamaValidation.requireNonEmpty(name, "name");
    ensureNotClosed();

    try {
      return nativeHasExport(instanceHandle.getResource(), name);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to check export: " + name, e);
    }
  }

  @Override
  public Optional<ComponentExportType> getExportType(final String name) throws WasmException {
    PanamaValidation.requireNonEmpty(name, "name");
    ensureNotClosed();

    try {
      final MemorySegment exportTypePtr = nativeGetExportType(instanceHandle.getResource(), name);
      if (exportTypePtr == null || exportTypePtr.equals(MemorySegment.NULL)) {
        return Optional.empty();
      }

      return Optional.of(new PanamaComponentExportTypeImpl(resourceManager, exportTypePtr, name));

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
    return !closed && instanceHandle != null && instanceHandle.isValid();
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      exportCache.clear();
      if (instanceHandle != null) {
        instanceHandle.close();
      }
      LOGGER.fine("Closed Panama component instance implementation");
    }
  }

  /**
   * Gets the underlying Panama component instance handle.
   *
   * @return the Panama component instance handle
   */
  public PanamaComponent.PanamaComponentInstanceHandle getInstanceHandle() {
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

  /**
   * Gets the resource manager used by this instance.
   *
   * @return the arena resource manager
   */
  public ArenaResourceManager getResourceManager() {
    return resourceManager;
  }

  /**
   * Ensures this instance is not closed.
   *
   * @throws IllegalStateException if the instance is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Component instance has been closed");
    }
  }

  /**
   * Parses a string array from a native memory segment.
   *
   * @param segment the memory segment containing string array data
   * @return array of strings
   */
  private String[] parseStringArray(final MemorySegment segment) {
    try {
      // This is a simplified implementation - in practice, you'd need to
      // parse the specific format returned by the native function
      final int count = nativeFunctions.getStringArrayCount(segment);
      if (count <= 0) {
        return new String[0];
      }

      final String[] result = new String[count];
      for (int i = 0; i < count; i++) {
        final MemorySegment stringPtr = nativeFunctions.getStringArrayElement(segment, i);
        if (stringPtr != null && !stringPtr.equals(MemorySegment.NULL)) {
          result[i] = nativeFunctions.convertToJavaString(stringPtr);
        }
      }

      return result;

    } catch (final Exception e) {
      LOGGER.warning("Failed to parse string array: " + e.getMessage());
      return new String[0];
    }
  }

  // Native method implementations using Panama FFI

  /**
   * Gets a component export by name.
   *
   * @param instancePtr the native component instance pointer
   * @param name the export name
   * @return native export pointer or null if not found
   * @throws WasmException if operation fails
   */
  private MemorySegment nativeGetExport(final MemorySegment instancePtr, final String name)
      throws WasmException {
    try {
      return nativeFunctions.getComponentExport(instancePtr, name);
    } catch (final Exception e) {
      throw new WasmException("Failed to get export", e);
    }
  }

  /**
   * Gets all export names from the component instance.
   *
   * @param instancePtr the native component instance pointer
   * @return managed memory segment containing export names
   * @throws WasmException if operation fails
   */
  private ArenaResourceManager.ManagedMemorySegment nativeGetExportNames(
      final MemorySegment instancePtr) throws WasmException {
    try {
      final MemorySegment namesPtr = nativeFunctions.getComponentExportNames(instancePtr);
      if (namesPtr == null || namesPtr.equals(MemorySegment.NULL)) {
        return null;
      }

      final long size = nativeFunctions.getExportNamesSize(namesPtr);
      if (size <= 0) {
        return null;
      }

      final ArenaResourceManager.ManagedMemorySegment managedSegment =
          resourceManager.allocate((int) size);

      MemorySegment.copy(namesPtr, 0, managedSegment.segment(), 0, size);
      nativeFunctions.freeExportNames(namesPtr);

      return managedSegment;

    } catch (final Exception e) {
      throw new WasmException("Failed to get export names", e);
    }
  }

  /**
   * Checks if an export exists.
   *
   * @param instancePtr the native component instance pointer
   * @param name the export name
   * @return true if export exists, false otherwise
   * @throws WasmException if operation fails
   */
  private boolean nativeHasExport(final MemorySegment instancePtr, final String name)
      throws WasmException {
    try {
      return nativeFunctions.hasComponentExport(instancePtr, name);
    } catch (final Exception e) {
      throw new WasmException("Failed to check export", e);
    }
  }

  /**
   * Gets export type information.
   *
   * @param instancePtr the native component instance pointer
   * @param name the export name
   * @return native export type pointer or null if not found
   * @throws WasmException if operation fails
   */
  private MemorySegment nativeGetExportType(final MemorySegment instancePtr, final String name)
      throws WasmException {
    try {
      return nativeFunctions.getComponentExportType(instancePtr, name);
    } catch (final Exception e) {
      throw new WasmException("Failed to get export type", e);
    }
  }
}
