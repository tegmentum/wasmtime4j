package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Set;

/**
 * Panama implementation of Component.
 *
 * @since 1.0.0
 */
final class PanamaComponentImpl implements Component {

  private static final NativeComponentBindings NATIVE_BINDINGS =
      NativeComponentBindings.getInstance();

  private final MemorySegment componentHandle;
  private final String componentId;
  private final PanamaComponentEngine engine;
  private final NativeResourceHandle resourceHandle;

  PanamaComponentImpl(
      final MemorySegment componentHandle,
      final String componentId,
      final PanamaComponentEngine engine) {
    this.componentHandle = componentHandle;
    this.componentId = componentId;
    this.engine = engine;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaComponentImpl",
            () -> {
              if (componentHandle != null && !componentHandle.equals(MemorySegment.NULL)) {
                try {
                  NATIVE_BINDINGS.componentDestroy(componentHandle);
                } catch (final Throwable t) {
                  throw new Exception("Error closing PanamaComponentImpl", t);
                }
              }
            });
  }

  @Override
  public String getId() {
    return componentId;
  }

  @Override
  public long getSize() throws WasmException {
    ensureNotClosed();
    return NATIVE_BINDINGS.componentGetSize(componentHandle);
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
        final int errorCode = NATIVE_BINDINGS.componentGetExportName(componentHandle, i, nameOut);

        if (errorCode != 0) {
          continue; // Skip this export if we can't get the name
        }

        final MemorySegment namePtr = nameOut.get(ValueLayout.ADDRESS, 0);
        if (namePtr != null && !namePtr.equals(MemorySegment.NULL)) {
          try {
            // Reinterpret as unbounded segment to read C string
            final MemorySegment unboundedPtr = namePtr.reinterpret(Long.MAX_VALUE);
            final String exportName = unboundedPtr.getString(0);
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
        final int errorCode = NATIVE_BINDINGS.componentGetImportName(componentHandle, i, nameOut);

        if (errorCode != 0) {
          continue; // Skip this import if we can't get the name
        }

        final MemorySegment namePtr = nameOut.get(ValueLayout.ADDRESS, 0);
        if (namePtr != null && !namePtr.equals(MemorySegment.NULL)) {
          try {
            // Reinterpret as unbounded segment to read C string
            final MemorySegment unboundedPtr = namePtr.reinterpret(Long.MAX_VALUE);
            final String importName = unboundedPtr.getString(0);
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
  public WitInterfaceDefinition getWitInterface() throws WasmException {
    ensureNotClosed();

    // Build WIT interface from component exports and imports
    final StringBuilder witBuilder = new StringBuilder();
    witBuilder.append("interface ").append(componentId).append(" {\n");

    // Add exported interfaces
    for (final String export : getExportedInterfaces()) {
      witBuilder.append("  export ").append(export).append(";\n");
    }

    // Add imported interfaces
    for (final String imp : getImportedInterfaces()) {
      witBuilder.append("  import ").append(imp).append(";\n");
    }

    witBuilder.append("}\n");

    return new PanamaWitInterfaceDefinition(
        componentId,
        "1.0.0",
        "", // Package name not available from component metadata
        witBuilder.toString());
  }

  @Override
  public WitCompatibilityResult checkWitCompatibility(final Component other) throws WasmException {
    if (other == null) {
      throw new IllegalArgumentException("other cannot be null");
    }
    ensureNotClosed();

    // Get WIT interfaces for both components
    final WitInterfaceDefinition myInterface = getWitInterface();
    final WitInterfaceDefinition otherInterface = other.getWitInterface();

    // Use the WitInterfaceDefinition's built-in compatibility check
    return myInterface.isCompatibleWith(otherInterface);
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed()
        && componentHandle != null
        && !componentHandle.equals(MemorySegment.NULL);
  }

  @Override
  public void close() {
    resourceHandle.close();
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
    resourceHandle.ensureNotClosed();
  }
}
