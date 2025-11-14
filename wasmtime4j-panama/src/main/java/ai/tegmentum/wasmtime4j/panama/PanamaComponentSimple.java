package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ComponentCompatibility;
import ai.tegmentum.wasmtime4j.ComponentDependencyGraph;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.ComponentLifecycleState;
import ai.tegmentum.wasmtime4j.ComponentMetadata;
import ai.tegmentum.wasmtime4j.ComponentRegistry;
import ai.tegmentum.wasmtime4j.ComponentResourceUsage;
import ai.tegmentum.wasmtime4j.ComponentSimple;
import ai.tegmentum.wasmtime4j.ComponentValidationConfig;
import ai.tegmentum.wasmtime4j.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.ComponentVersion;
import ai.tegmentum.wasmtime4j.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Panama implementation of ComponentSimple.
 *
 * @since 1.0.0
 */
final class PanamaComponentSimple implements ComponentSimple {

  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final MemorySegment componentHandle;
  private final String componentId;
  private final PanamaComponentEngine engine;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  PanamaComponentSimple(
      final MemorySegment componentHandle,
      final String componentId,
      final PanamaComponentEngine engine) {
    this.componentHandle = componentHandle;
    this.componentId = componentId;
    this.engine = engine;
  }

  @Override
  public String getId() {
    return componentId;
  }

  @Override
  public ComponentVersion getVersion() {
    return new ComponentVersion(1, 0, 0);
  }

  @Override
  public long getSize() throws WasmException {
    ensureNotClosed();
    return NATIVE_BINDINGS.componentGetSize(componentHandle);
  }

  @Override
  public ComponentMetadata getMetadata() {
    return new ComponentMetadata(componentId, new ComponentVersion(1, 0, 0), "Component");
  }

  @Override
  public boolean exportsInterface(final String interfaceName) throws WasmException {
    ensureNotClosed();
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(interfaceName);
      return NATIVE_BINDINGS.componentExportsInterface(componentHandle, nameSegment) != 0;
    }
  }

  @Override
  public boolean importsInterface(final String interfaceName) throws WasmException {
    ensureNotClosed();
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment nameSegment = arena.allocateFrom(interfaceName);
      return NATIVE_BINDINGS.componentImportsInterface(componentHandle, nameSegment) != 0;
    }
  }

  @Override
  public Set<String> getExportedInterfaces() throws WasmException {
    ensureNotClosed();
    final long exportCount = NATIVE_BINDINGS.componentExportCount(componentHandle);
    final Set<String> exports = new java.util.HashSet<>();

    try (Arena arena = Arena.ofConfined()) {
      for (long i = 0; i < exportCount; i++) {
        final MemorySegment nameOut = arena.allocate(ValueLayout.ADDRESS);
        final int errorCode =
            NATIVE_BINDINGS.componentGetExportName(componentHandle, i, nameOut);

        if (errorCode != 0) {
          continue; // Skip this export if we can't get the name
        }

        final MemorySegment namePtr = nameOut.get(ValueLayout.ADDRESS, 0);
        if (namePtr != null && !namePtr.equals(MemorySegment.NULL)) {
          try {
            final String exportName = namePtr.getString(0);
            exports.add(exportName);
          } finally {
            NATIVE_BINDINGS.componentFreeString(namePtr);
          }
        }
      }
    }

    return Set.copyOf(exports);
  }

  @Override
  public Set<String> getImportedInterfaces() throws WasmException {
    ensureNotClosed();
    final long importCount = NATIVE_BINDINGS.componentImportCount(componentHandle);
    final Set<String> imports = new java.util.HashSet<>();

    try (Arena arena = Arena.ofConfined()) {
      for (long i = 0; i < importCount; i++) {
        final MemorySegment nameOut = arena.allocate(ValueLayout.ADDRESS);
        final int errorCode =
            NATIVE_BINDINGS.componentGetImportName(componentHandle, i, nameOut);

        if (errorCode != 0) {
          continue; // Skip this import if we can't get the name
        }

        final MemorySegment namePtr = nameOut.get(ValueLayout.ADDRESS, 0);
        if (namePtr != null && !namePtr.equals(MemorySegment.NULL)) {
          try {
            final String importName = namePtr.getString(0);
            imports.add(importName);
          } finally {
            NATIVE_BINDINGS.componentFreeString(namePtr);
          }
        }
      }
    }

    return Set.copyOf(imports);
  }

  @Override
  public ComponentInstance instantiate() throws WasmException {
    throw new UnsupportedOperationException("Use engine.createInstance() instead");
  }

  @Override
  public ComponentInstance instantiate(final ComponentInstanceConfig config) throws WasmException {
    throw new UnsupportedOperationException("Use engine.createInstance() instead");
  }

  @Override
  public ComponentDependencyGraph getDependencyGraph() throws WasmException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public Set<ComponentSimple> resolveDependencies(final ComponentRegistry registry)
      throws WasmException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public ComponentCompatibility checkCompatibility(final ComponentSimple other)
      throws WasmException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public WitInterfaceDefinition getWitInterface() throws WasmException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public WitCompatibilityResult checkWitCompatibility(final ComponentSimple other)
      throws WasmException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public ComponentResourceUsage getResourceUsage() {
    return new ComponentResourceUsage(componentId);
  }

  @Override
  public ComponentLifecycleState getLifecycleState() {
    return closed.get() ? ComponentLifecycleState.ERROR : ComponentLifecycleState.READY;
  }

  @Override
  public boolean isValid() {
    return !closed.get() && componentHandle != null && !componentHandle.equals(MemorySegment.NULL);
  }

  @Override
  public ComponentValidationResult validate(final ComponentValidationConfig validationConfig)
      throws WasmException {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      if (componentHandle != null && !componentHandle.equals(MemorySegment.NULL)) {
        try {
          NATIVE_BINDINGS.componentDestroy(componentHandle);
        } catch (final Exception e) {
          // Log and continue
        }
      }
    }
  }

  MemorySegment getNativeHandle() {
    return componentHandle;
  }

  String getComponentId() {
    return componentId;
  }

  PanamaComponentEngine getEngine() {
    return engine;
  }

  private void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("Component is closed");
    }
  }
}
