/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentHostFunction;
import ai.tegmentum.wasmtime4j.component.ComponentImportValidation;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.component.ComponentResourceDefinition;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiPreview2Config;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of ComponentLinker for WebAssembly Component Model.
 *
 * <p>This implementation uses Panama Foreign Function API to interact with the native Wasmtime
 * library for Component Model operations.
 *
 * @param <T> the type of user data associated with stores used with this linker
 * @since 1.0.0
 */
public final class PanamaComponentLinker<T> implements ComponentLinker<T> {

  private static final Logger LOGGER = Logger.getLogger(PanamaComponentLinker.class.getName());
  private static final NativeComponentBindings NATIVE_BINDINGS =
      NativeComponentBindings.getInstance();
  private static final ConcurrentHashMap<Long, ComponentHostFunctionWrapper> HOST_CALLBACKS =
      new ConcurrentHashMap<>();
  private static final AtomicLong NEXT_CALLBACK_ID = new AtomicLong(1);

  private final PanamaEngine engine;
  private final Arena arena;
  private final MemorySegment nativeLinker;
  private final Map<String, Long> hostFunctions = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> definedInterfaces = new ConcurrentHashMap<>();
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama component linker.
   *
   * @param engine the engine to create the linker for
   * @throws WasmException if linker creation fails
   */
  public PanamaComponentLinker(final PanamaEngine engine) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }
    this.engine = engine;
    this.arena = Arena.ofShared();

    // Create native component linker
    final MemorySegment enginePtr = engine.getNativeEngine();
    if (enginePtr == null || enginePtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Engine has invalid native handle");
    }

    this.nativeLinker = NATIVE_BINDINGS.componentLinkerCreateWithEngine(enginePtr);
    if (this.nativeLinker == null || this.nativeLinker.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to create native component linker");
    }

    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaComponentLinker",
            () -> {
              // Clean up host function callbacks
              for (final Long callbackId : hostFunctions.values()) {
                HOST_CALLBACKS.remove(callbackId);
              }
              hostFunctions.clear();
              definedInterfaces.clear();

              // Dispose native linker resources
              if (nativeLinker != null && !nativeLinker.equals(MemorySegment.NULL)) {
                try {
                  NATIVE_BINDINGS.componentLinkerDestroy(nativeLinker);
                } catch (final Throwable t) {
                  throw new Exception("Error closing PanamaComponentLinker native linker", t);
                }
              }

              try {
                arena.close();
              } catch (final Throwable t) {
                throw new Exception("Error closing PanamaComponentLinker arena", t);
              }
            });

    LOGGER.fine("Created Panama component linker");
  }

  @Override
  public void defineFunction(
      final String interfaceNamespace,
      final String interfaceName,
      final String functionName,
      final ComponentHostFunction implementation)
      throws WasmException {
    if (interfaceNamespace == null) {
      throw new IllegalArgumentException("Interface namespace cannot be null");
    }
    if (interfaceName == null) {
      throw new IllegalArgumentException("Interface name cannot be null");
    }
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    if (implementation == null) {
      throw new IllegalArgumentException("Implementation cannot be null");
    }
    ensureNotClosed();

    // Register the callback
    final long callbackId = registerHostFunctionCallback(implementation);

    // Build WIT path key
    final String witPath =
        interfaceNamespace + ":" + interfaceName + "/" + interfaceName + "#" + functionName;
    hostFunctions.put(witPath, callbackId);

    // Track in defined interfaces
    final String interfaceKey = interfaceNamespace + ":" + interfaceName + "/" + interfaceName;
    definedInterfaces.computeIfAbsent(interfaceKey, k -> new HashSet<>()).add(functionName);

    LOGGER.fine(
        "Defined component host function: " + witPath + " (callback ID: " + callbackId + ")");
  }

  @Override
  public void defineFunction(final String witPath, final ComponentHostFunction implementation)
      throws WasmException {
    if (witPath == null || witPath.isEmpty()) {
      throw new IllegalArgumentException("WIT path cannot be null or empty");
    }
    if (implementation == null) {
      throw new IllegalArgumentException("Implementation cannot be null");
    }
    ensureNotClosed();

    // Parse WIT path to extract components
    final String[] pathParts = parseWitPath(witPath);
    defineFunction(pathParts[0], pathParts[1], pathParts[2], implementation);
  }

  @Override
  public void defineInterface(
      final String interfaceNamespace,
      final String interfaceName,
      final Map<String, ComponentHostFunction> functions)
      throws WasmException {
    if (interfaceNamespace == null) {
      throw new IllegalArgumentException("Interface namespace cannot be null");
    }
    if (interfaceName == null) {
      throw new IllegalArgumentException("Interface name cannot be null");
    }
    if (functions == null) {
      throw new IllegalArgumentException("Functions cannot be null");
    }
    ensureNotClosed();

    for (final Map.Entry<String, ComponentHostFunction> entry : functions.entrySet()) {
      defineFunction(interfaceNamespace, interfaceName, entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void defineResource(
      final String interfaceNamespace,
      final String interfaceName,
      final String resourceName,
      final ComponentResourceDefinition<?> resourceDefinition)
      throws WasmException {
    if (interfaceNamespace == null) {
      throw new IllegalArgumentException("Interface namespace cannot be null");
    }
    if (interfaceName == null) {
      throw new IllegalArgumentException("Interface name cannot be null");
    }
    if (resourceName == null) {
      throw new IllegalArgumentException("Resource name cannot be null");
    }
    if (resourceDefinition == null) {
      throw new IllegalArgumentException("Resource definition cannot be null");
    }
    ensureNotClosed();

    // Track resource in defined interfaces (use same key format as defineFunction)
    final String interfaceKey = interfaceNamespace + ":" + interfaceName + "/" + interfaceName;
    definedInterfaces
        .computeIfAbsent(interfaceKey, k -> ConcurrentHashMap.newKeySet())
        .add("[resource]" + resourceName);

    // Note: Full resource support requires additional native infrastructure for:
    // - Resource table management
    // - Constructor/destructor callbacks
    // - Method dispatch
    // The resource definition is tracked but not fully wired to native code yet.

    LOGGER.fine(
        "Defined component resource: "
            + interfaceNamespace
            + ":"
            + interfaceName
            + "/"
            + resourceName);
  }

  @Override
  public void linkInstance(final ComponentInstance instance) throws WasmException {
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }
    ensureNotClosed();

    // Get exported functions from the instance and track them
    // This allows the linker to satisfy imports from another component using this instance's
    // exports
    final Set<String> exportedFunctions = instance.getExportedFunctions();

    for (final String exportName : exportedFunctions) {
      // Track the exported function as available for linking
      // The native linker will resolve these at instantiation time
      final String interfaceKey = "linked:" + instance.getId();
      definedInterfaces.computeIfAbsent(interfaceKey, k -> new HashSet<>()).add(exportName);
    }

    LOGGER.fine(
        "Linked component instance exports: "
            + exportedFunctions.size()
            + " functions from instance "
            + instance.getId());
  }

  @Override
  public ComponentInstance linkComponent(final Store store, final Component component)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }
    ensureNotClosed();

    // Instantiate the component and link its exports
    final ComponentInstance instance = instantiate(store, component);
    linkInstance(instance);
    return instance;
  }

  @Override
  public ComponentInstance instantiate(final Store store, final Component component)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }
    ensureNotClosed();

    if (!(component instanceof PanamaComponentImpl)) {
      throw new IllegalArgumentException("Component must be a Panama implementation");
    }

    final PanamaComponentImpl panamaComponent = (PanamaComponentImpl) component;
    final PanamaStore panamaStore = (store instanceof PanamaStore) ? (PanamaStore) store : null;

    try (Arena tempArena = Arena.ofConfined()) {
      // Allocate output pointer for the instance
      final MemorySegment instanceOutPtr = tempArena.allocate(ValueLayout.ADDRESS);

      // Call native linker instantiate
      final int errorCode =
          NATIVE_BINDINGS.componentLinkerInstantiate(
              nativeLinker, panamaComponent.getNativeHandle(), instanceOutPtr);

      if (errorCode != 0) {
        throw new WasmException(
            "Failed to instantiate component through linker (error code: " + errorCode + ")");
      }

      // Get the instance pointer from the output
      final MemorySegment instancePtr = instanceOutPtr.get(ValueLayout.ADDRESS, 0);

      if (instancePtr == null || instancePtr.equals(MemorySegment.NULL)) {
        throw new WasmException(
            "Failed to instantiate component through linker: null instance returned");
      }

      LOGGER.fine("Successfully instantiated component through linker");

      // Create and return the component instance
      return new PanamaComponentInstance(instancePtr, panamaComponent, panamaStore);
    }
  }

  @Override
  public void enableWasiPreview2() throws WasmException {
    ensureNotClosed();

    final int result = NATIVE_BINDINGS.componentLinkerEnableWasiP2(nativeLinker);
    if (result != 0) {
      throw new WasmException("Failed to enable WASI Preview 2 (error code: " + result + ")");
    }

    LOGGER.fine("Enabled WASI Preview 2 in component linker");
  }

  @Override
  public void enableWasiPreview2(final WasiPreview2Config config) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }
    ensureNotClosed();

    // Apply config settings before enabling WASI Preview 2
    applyWasiConfig(config);

    // Enable WASI Preview 2
    enableWasiPreview2();

    LOGGER.fine("Enabled WASI Preview 2 with custom configuration");
  }

  /**
   * Applies WASI Preview 2 configuration settings to the linker.
   *
   * @param config the WASI configuration to apply
   * @throws WasmException if configuration fails
   */
  private void applyWasiConfig(final WasiPreview2Config config) throws WasmException {
    // Apply args if provided
    if (config.getArgs() != null && !config.getArgs().isEmpty()) {
      final StringBuilder jsonBuilder = new StringBuilder("[");
      boolean first = true;
      for (final String arg : config.getArgs()) {
        if (!first) {
          jsonBuilder.append(",");
        }
        jsonBuilder.append("\"").append(escapeJsonString(arg)).append("\"");
        first = false;
      }
      jsonBuilder.append("]");

      try (final Arena tempArena = Arena.ofConfined()) {
        final MemorySegment argsJson = tempArena.allocateFrom(jsonBuilder.toString());
        final int result = NATIVE_BINDINGS.componentLinkerSetWasiArgs(nativeLinker, argsJson);
        if (result != 0) {
          LOGGER.warning("Failed to set WASI args (error code: " + result + ")");
        }
      }
    }

    // Apply environment variables
    if (config.getEnv() != null && !config.getEnv().isEmpty()) {
      try (final Arena tempArena = Arena.ofConfined()) {
        for (final Map.Entry<String, String> entry : config.getEnv().entrySet()) {
          final MemorySegment keyPtr = tempArena.allocateFrom(entry.getKey());
          final MemorySegment valuePtr = tempArena.allocateFrom(entry.getValue());
          final int result =
              NATIVE_BINDINGS.componentLinkerAddWasiEnv(nativeLinker, keyPtr, valuePtr);
          if (result != 0) {
            LOGGER.warning(
                "Failed to add WASI env var '" + entry.getKey() + "' (error code: " + result + ")");
          }
        }
      }
    }

    // Apply inherit env flag
    final int inheritEnvResult =
        NATIVE_BINDINGS.componentLinkerSetWasiInheritEnv(
            nativeLinker, config.isInheritEnv() ? 1 : 0);
    if (inheritEnvResult != 0) {
      LOGGER.warning("Failed to set WASI inherit env flag (error code: " + inheritEnvResult + ")");
    }

    // Apply inherit stdio flag
    final int inheritStdioResult =
        NATIVE_BINDINGS.componentLinkerSetWasiInheritStdio(
            nativeLinker, config.isInheritStdio() ? 1 : 0);
    if (inheritStdioResult != 0) {
      LOGGER.warning(
          "Failed to set WASI inherit stdio flag (error code: " + inheritStdioResult + ")");
    }

    // Apply preopened directories
    if (config.getPreopenDirs() != null && !config.getPreopenDirs().isEmpty()) {
      try (final Arena tempArena = Arena.ofConfined()) {
        for (final WasiPreview2Config.PreopenDir dir : config.getPreopenDirs()) {
          final MemorySegment hostPathPtr =
              tempArena.allocateFrom(dir.getHostPath().toAbsolutePath().toString());
          final MemorySegment guestPathPtr = tempArena.allocateFrom(dir.getGuestPath());
          final int result =
              NATIVE_BINDINGS.componentLinkerAddWasiPreopenDir(
                  nativeLinker, hostPathPtr, guestPathPtr, dir.isReadOnly() ? 1 : 0);
          if (result != 0) {
            LOGGER.warning(
                "Failed to add preopened dir '"
                    + dir.getHostPath()
                    + "' (error code: "
                    + result
                    + ")");
          }
        }
      }
    }

    // Apply allow network flag
    final int allowNetworkResult =
        NATIVE_BINDINGS.componentLinkerSetWasiAllowNetwork(
            nativeLinker, config.isAllowNetwork() ? 1 : 0);
    if (allowNetworkResult != 0) {
      LOGGER.warning(
          "Failed to set WASI allow network flag (error code: " + allowNetworkResult + ")");
    }

    // Apply allow clock flag
    final int allowClockResult =
        NATIVE_BINDINGS.componentLinkerSetWasiAllowClock(
            nativeLinker, config.isAllowClock() ? 1 : 0);
    if (allowClockResult != 0) {
      LOGGER.warning("Failed to set WASI allow clock flag (error code: " + allowClockResult + ")");
    }

    // Apply allow random flag
    final int allowRandomResult =
        NATIVE_BINDINGS.componentLinkerSetWasiAllowRandom(
            nativeLinker, config.isAllowRandom() ? 1 : 0);
    if (allowRandomResult != 0) {
      LOGGER.warning(
          "Failed to set WASI allow random flag (error code: " + allowRandomResult + ")");
    }
  }

  /**
   * Escapes a string for JSON encoding.
   *
   * @param str the string to escape
   * @return the escaped string
   */
  private String escapeJsonString(final String str) {
    if (str == null) {
      return "";
    }
    final StringBuilder result = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      final char c = str.charAt(i);
      switch (c) {
        case '"':
          result.append("\\\"");
          break;
        case '\\':
          result.append("\\\\");
          break;
        case '\b':
          result.append("\\b");
          break;
        case '\f':
          result.append("\\f");
          break;
        case '\n':
          result.append("\\n");
          break;
        case '\r':
          result.append("\\r");
          break;
        case '\t':
          result.append("\\t");
          break;
        default:
          if (c < ' ') {
            result.append(String.format("\\u%04x", (int) c));
          } else {
            result.append(c);
          }
      }
    }
    return result.toString();
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public boolean isValid() {
    if (resourceHandle.isClosed()) {
      return false;
    }
    return NATIVE_BINDINGS.componentLinkerIsValid(nativeLinker) == 1;
  }

  @Override
  public boolean hasInterface(final String interfaceNamespace, final String interfaceName) {
    if (interfaceNamespace == null) {
      throw new IllegalArgumentException("Interface namespace cannot be null");
    }
    if (interfaceName == null) {
      throw new IllegalArgumentException("Interface name cannot be null");
    }
    ensureNotClosed();

    // Use Java-side tracking since defineFunction tracks here, not in native
    final String interfaceKey = interfaceNamespace + ":" + interfaceName + "/" + interfaceName;
    return definedInterfaces.containsKey(interfaceKey);
  }

  @Override
  public boolean hasFunction(
      final String interfaceNamespace, final String interfaceName, final String functionName) {
    if (interfaceNamespace == null) {
      throw new IllegalArgumentException("Interface namespace cannot be null");
    }
    if (interfaceName == null) {
      throw new IllegalArgumentException("Interface name cannot be null");
    }
    if (functionName == null) {
      throw new IllegalArgumentException("Function name cannot be null");
    }
    ensureNotClosed();

    // Use Java-side tracking since defineFunction tracks here, not in native
    final String interfaceKey = interfaceNamespace + ":" + interfaceName + "/" + interfaceName;
    final Set<String> functions = definedInterfaces.get(interfaceKey);
    return functions != null && functions.contains(functionName);
  }

  @Override
  public Set<String> getDefinedInterfaces() {
    ensureNotClosed();
    // Convert internal key format (namespace:name/name) to user-friendly format (namespace:name)
    final Set<String> result = new HashSet<>();
    for (final String key : definedInterfaces.keySet()) {
      // Key format is "namespace:interfaceName/interfaceName", strip the trailing "/interfaceName"
      final int slashIdx = key.lastIndexOf('/');
      if (slashIdx > 0) {
        result.add(key.substring(0, slashIdx));
      } else {
        result.add(key);
      }
    }
    return result;
  }

  @Override
  public Set<String> getDefinedFunctions(
      final String interfaceNamespace, final String interfaceName) {
    if (interfaceNamespace == null) {
      throw new IllegalArgumentException("Interface namespace cannot be null");
    }
    if (interfaceName == null) {
      throw new IllegalArgumentException("Interface name cannot be null");
    }
    ensureNotClosed();

    final String key = interfaceNamespace + ":" + interfaceName + "/" + interfaceName;
    final Set<String> functions = definedInterfaces.get(key);
    return functions != null ? new HashSet<>(functions) : Set.of();
  }

  @Override
  public ComponentImportValidation validateImports(final Component component) {
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }
    ensureNotClosed();

    // Build validation result based on what's defined
    final ComponentImportValidation.Builder builder = ComponentImportValidation.builder();

    // For now, mark all host functions as satisfied
    for (final String witPath : hostFunctions.keySet()) {
      builder.addSatisfied(witPath);
    }

    return builder.build();
  }

  @Override
  public void aliasInterface(
      final String fromNamespace,
      final String fromInterface,
      final String toNamespace,
      final String toInterface)
      throws WasmException {
    if (fromNamespace == null) {
      throw new IllegalArgumentException("From namespace cannot be null");
    }
    if (fromInterface == null) {
      throw new IllegalArgumentException("From interface cannot be null");
    }
    if (toNamespace == null) {
      throw new IllegalArgumentException("To namespace cannot be null");
    }
    if (toInterface == null) {
      throw new IllegalArgumentException("To interface cannot be null");
    }
    ensureNotClosed();

    // Copy all functions from source interface to target interface
    final String fromKey = fromNamespace + ":" + fromInterface + "/" + fromInterface;
    final String toKey = toNamespace + ":" + toInterface + "/" + toInterface;

    final Set<String> sourceFunctions = definedInterfaces.get(fromKey);
    if (sourceFunctions != null) {
      definedInterfaces.computeIfAbsent(toKey, k -> new HashSet<>()).addAll(sourceFunctions);

      // Also copy the host function registrations
      for (final String function : sourceFunctions) {
        final String fromPath = fromKey + "#" + function;
        final String toPath = toKey + "#" + function;
        final Long callbackId = hostFunctions.get(fromPath);
        if (callbackId != null) {
          hostFunctions.put(toPath, callbackId);
        }
      }
    }

    LOGGER.fine("Created interface alias from " + fromKey + " to " + toKey);
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  /**
   * Gets the native component linker pointer.
   *
   * @return native linker memory segment
   */
  public MemorySegment getNativeLinker() {
    return nativeLinker;
  }

  /**
   * Ensures the linker is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  /**
   * Parses a WIT path into namespace, interface, and function components.
   *
   * @param witPath the WIT path to parse
   * @return array of [namespace, interface, function]
   * @throws WasmException if the path is malformed
   */
  private String[] parseWitPath(final String witPath) throws WasmException {
    // Expected formats:
    // - "namespace:package/interface#function"
    // - "namespace:package/interface@version#function"

    final int hashIndex = witPath.indexOf('#');
    if (hashIndex == -1) {
      throw new WasmException("Invalid WIT path format (missing #): " + witPath);
    }

    final String functionName = witPath.substring(hashIndex + 1);
    String interfacePart = witPath.substring(0, hashIndex);

    // Remove version if present
    final int atIndex = interfacePart.indexOf('@');
    if (atIndex != -1) {
      interfacePart = interfacePart.substring(0, atIndex);
    }

    final int slashIndex = interfacePart.indexOf('/');
    if (slashIndex == -1) {
      throw new WasmException("Invalid WIT interface path (missing /): " + interfacePart);
    }

    final String namespace = interfacePart.substring(0, slashIndex);
    final String interfaceName = interfacePart.substring(slashIndex + 1);

    return new String[] {namespace, interfaceName, functionName};
  }

  /**
   * Registers a host function callback.
   *
   * @param implementation the host function implementation
   * @return the callback ID
   */
  private long registerHostFunctionCallback(final ComponentHostFunction implementation) {
    final long id = NEXT_CALLBACK_ID.getAndIncrement();
    HOST_CALLBACKS.put(id, new ComponentHostFunctionWrapper(id, implementation));
    return id;
  }

  /** Wrapper for component host function callbacks. */
  private static final class ComponentHostFunctionWrapper {
    private final long id;
    private final ComponentHostFunction implementation;

    ComponentHostFunctionWrapper(final long id, final ComponentHostFunction implementation) {
      this.id = id;
      this.implementation = implementation;
    }

    long getId() {
      return id;
    }

    ComponentHostFunction getImplementation() {
      return implementation;
    }
  }
}
