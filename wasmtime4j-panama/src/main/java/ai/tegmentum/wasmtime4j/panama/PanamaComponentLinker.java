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
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstancePre;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.component.ComponentResourceDefinition;
import ai.tegmentum.wasmtime4j.component.ComponentTypeCodec;
import ai.tegmentum.wasmtime4j.component.ComponentTypeInfo;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import ai.tegmentum.wasmtime4j.wasi.WasiPreview2Config;
import ai.tegmentum.wasmtime4j.wasi.WasiStdioConfig;
import ai.tegmentum.wasmtime4j.wasi.clocks.DateTime;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiMonotonicClock;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiWallClock;
import ai.tegmentum.wasmtime4j.wasi.random.WasiRandomSource;
import ai.tegmentum.wasmtime4j.wasi.sockets.SocketAddrCheck;
import ai.tegmentum.wasmtime4j.wasi.sockets.SocketAddrUse;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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
  private static final ConcurrentHashMap<Long, Object> WASI_CALLBACKS = new ConcurrentHashMap<>();
  private static final AtomicLong NEXT_CALLBACK_ID = new AtomicLong(1);
  private static final AtomicInteger RESOURCE_ID_COUNTER = new AtomicInteger(0);

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

    final MemorySegment capturedLinker = this.nativeLinker;
    final Arena capturedArena = this.arena;
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
            },
            this,
            () -> {
              if (capturedLinker != null && !capturedLinker.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.componentLinkerDestroy(capturedLinker);
              }
              if (capturedArena != null && capturedArena.scope().isAlive()) {
                capturedArena.close();
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
    final String interfaceKey = interfaceNamespace + ":" + interfaceName;
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
  public void defineFunctionAsync(
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
    final String interfaceKey = interfaceNamespace + ":" + interfaceName;
    definedInterfaces.computeIfAbsent(interfaceKey, k -> new HashSet<>()).add(functionName);

    LOGGER.fine(
        "Defined async component host function: " + witPath + " (callback ID: " + callbackId + ")");
  }

  @Override
  public void defineFunctionAsync(final String witPath, final ComponentHostFunction implementation)
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
    defineFunctionAsync(pathParts[0], pathParts[1], pathParts[2], implementation);
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

    // Build the interface path in WIT format: "ns:pkg/iface"
    final String interfacePath = interfaceNamespace + ":" + interfaceName;

    // Assign a unique resource ID
    final int resourceId = RESOURCE_ID_COUNTER.incrementAndGet();

    // Call native define_resource
    try (Arena tempArena = Arena.ofConfined()) {
      final byte[] pathBytes = interfacePath.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment pathSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, pathBytes);

      final byte[] nameBytes = resourceName.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment nameSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, nameBytes);

      final int errorCode =
          NATIVE_BINDINGS.componentLinkerDefineResource(
              nativeLinker,
              pathSegment,
              pathBytes.length,
              nameSegment,
              nameBytes.length,
              resourceId,
              MemorySegment.NULL, // No destructor function pointer for now
              0L);

      if (errorCode != 0) {
        throw PanamaErrorMapper.mapNativeError(
            errorCode, "Failed to define resource: " + resourceName);
      }
    }

    // Track resource in defined interfaces (use same key format as defineFunction)
    final String interfaceKey = interfaceNamespace + ":" + interfaceName;
    definedInterfaces
        .computeIfAbsent(interfaceKey, k -> ConcurrentHashMap.newKeySet())
        .add("[resource]" + resourceName);

    LOGGER.fine(
        "Defined component resource: "
            + interfacePath
            + "/"
            + resourceName
            + " (id="
            + resourceId
            + ")");
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
  public ComponentInstancePre instantiatePre(final Component component) throws WasmException {
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }
    ensureNotClosed();

    if (!(component instanceof PanamaComponentImpl)) {
      throw new IllegalArgumentException("Component must be a Panama implementation");
    }

    final PanamaComponentImpl panamaComponent = (PanamaComponentImpl) component;

    final MemorySegment preHandle =
        NATIVE_BINDINGS.componentLinkerInstantiatePre(
            nativeLinker, panamaComponent.getNativeHandle());

    if (preHandle == null || preHandle.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to pre-instantiate component through linker");
    }

    LOGGER.fine("Successfully pre-instantiated component through linker");

    return new PanamaComponentInstancePre(preHandle, engine, panamaComponent);
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
        throw PanamaErrorMapper.mapNativeError(
            errorCode, "Failed to instantiate component through linker");
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
      throw PanamaErrorMapper.mapNativeError(result, "Failed to enable WASI Preview 2");
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
          LOGGER.warning(
              "Failed to set WASI args: " + PanamaErrorMapper.getErrorDescription(result));
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
                "Failed to add WASI env var '"
                    + entry.getKey()
                    + "': "
                    + PanamaErrorMapper.getErrorDescription(result));
          }
        }
      }
    }

    // Apply inherit env flag
    final int inheritEnvResult =
        NATIVE_BINDINGS.componentLinkerSetWasiInheritEnv(
            nativeLinker, config.isInheritEnv() ? 1 : 0);
    if (inheritEnvResult != 0) {
      LOGGER.warning(
          "Failed to set WASI inherit env flag: "
              + PanamaErrorMapper.getErrorDescription(inheritEnvResult));
    }

    // Apply inherit args flag
    final int inheritArgsResult =
        NATIVE_BINDINGS.componentLinkerSetWasiInheritArgs(
            nativeLinker, config.isInheritArgs() ? 1 : 0);
    if (inheritArgsResult != 0) {
      LOGGER.warning(
          "Failed to set WASI inherit args flag: "
              + PanamaErrorMapper.getErrorDescription(inheritArgsResult));
    }

    // Apply inherit stdio flag
    final int inheritStdioResult =
        NATIVE_BINDINGS.componentLinkerSetWasiInheritStdio(
            nativeLinker, config.isInheritStdio() ? 1 : 0);
    if (inheritStdioResult != 0) {
      LOGGER.warning(
          "Failed to set WASI inherit stdio flag: "
              + PanamaErrorMapper.getErrorDescription(inheritStdioResult));
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
                  nativeLinker,
                  hostPathPtr,
                  guestPathPtr,
                  dir.getDirPerms().getBits(),
                  dir.getFilePerms().getBits());
          if (result != 0) {
            LOGGER.warning(
                "Failed to add preopened dir '"
                    + dir.getHostPath()
                    + "': "
                    + PanamaErrorMapper.getErrorDescription(result));
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
          "Failed to set WASI allow network flag: "
              + PanamaErrorMapper.getErrorDescription(allowNetworkResult));
    }

    // Apply allow clock flag
    final int allowClockResult =
        NATIVE_BINDINGS.componentLinkerSetWasiAllowClock(
            nativeLinker, config.isAllowClock() ? 1 : 0);
    if (allowClockResult != 0) {
      LOGGER.warning(
          "Failed to set WASI allow clock flag: "
              + PanamaErrorMapper.getErrorDescription(allowClockResult));
    }

    // Apply allow random flag
    final int allowRandomResult =
        NATIVE_BINDINGS.componentLinkerSetWasiAllowRandom(
            nativeLinker, config.isAllowRandom() ? 1 : 0);
    if (allowRandomResult != 0) {
      LOGGER.warning(
          "Failed to set WASI allow random flag: "
              + PanamaErrorMapper.getErrorDescription(allowRandomResult));
    }

    // Apply individual stdio config
    applyStdinConfig(config, nativeLinker);
    applyStdoutConfig(config, nativeLinker);
    applyStderrConfig(config, nativeLinker);

    // Apply granular network controls
    NATIVE_BINDINGS.componentLinkerSetWasiAllowTcp(nativeLinker, config.isAllowTcp() ? 1 : 0);
    NATIVE_BINDINGS.componentLinkerSetWasiAllowUdp(nativeLinker, config.isAllowUdp() ? 1 : 0);
    NATIVE_BINDINGS.componentLinkerSetWasiAllowIpNameLookup(
        nativeLinker, config.isAllowIpNameLookup() ? 1 : 0);

    // Apply allow blocking current thread
    NATIVE_BINDINGS.componentLinkerSetWasiAllowBlockingCurrentThread(
        nativeLinker, config.isAllowBlockingCurrentThread() ? 1 : 0);

    // Apply insecure random seed if set
    if (config.hasInsecureRandomSeed()) {
      NATIVE_BINDINGS.componentLinkerSetWasiInsecureRandomSeed(
          nativeLinker, config.getInsecureRandomSeed());
    }

    // Apply custom wall clock
    if (config.getWallClock() != null) {
      applyWasiWallClock(config.getWallClock());
    }

    // Apply custom monotonic clock
    if (config.getMonotonicClock() != null) {
      applyWasiMonotonicClock(config.getMonotonicClock());
    }

    // Apply custom secure random
    if (config.getSecureRandom() != null) {
      applyWasiRandomSource(config.getSecureRandom(), true);
    }

    // Apply custom insecure random
    if (config.getInsecureRandom() != null) {
      applyWasiRandomSource(config.getInsecureRandom(), false);
    }

    // Apply socket address check
    if (config.getSocketAddrCheck() != null) {
      applyWasiSocketAddrCheck(config.getSocketAddrCheck());
    }
  }

  /**
   * Applies stdin configuration from WasiStdioConfig or falls back to boolean inherit flag.
   *
   * @param config the WASI preview 2 configuration
   * @param nativeLinker the native linker memory segment
   */
  private void applyStdinConfig(final WasiPreview2Config config, final MemorySegment nativeLinker) {
    final WasiStdioConfig stdinConfig = config.getStdinConfig();
    if (stdinConfig != null) {
      switch (stdinConfig.getType()) {
        case INHERIT:
          NATIVE_BINDINGS.componentLinkerSetWasiInheritStdin(nativeLinker, 1);
          break;
        case INPUT_STREAM:
          try {
            final byte[] bytes = stdinConfig.getInputStream().readAllBytes();
            try (Arena arena = Arena.ofConfined()) {
              final MemorySegment dataSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, bytes);
              NATIVE_BINDINGS.componentLinkerSetWasiStdinBytes(nativeLinker, dataSeg, bytes.length);
            }
          } catch (java.io.IOException e) {
            LOGGER.warning("Failed to read stdin InputStream: " + e.getMessage());
          }
          break;
        case NULL:
          // Don't inherit stdin — native layer defaults to empty stdin
          try (Arena arena = Arena.ofConfined()) {
            final MemorySegment dataSeg = arena.allocate(ValueLayout.JAVA_BYTE, 0);
            NATIVE_BINDINGS.componentLinkerSetWasiStdinBytes(nativeLinker, dataSeg, 0);
          }
          break;
        default:
          LOGGER.warning(
              "Unsupported stdin config type for component model: " + stdinConfig.getType());
          break;
      }
    } else if (config.isInheritStdin()) {
      NATIVE_BINDINGS.componentLinkerSetWasiInheritStdin(nativeLinker, 1);
    }
  }

  /**
   * Applies stdout configuration from WasiStdioConfig or falls back to boolean inherit flag.
   *
   * @param config the WASI preview 2 configuration
   * @param nativeLinker the native linker memory segment
   */
  private void applyStdoutConfig(
      final WasiPreview2Config config, final MemorySegment nativeLinker) {
    final WasiStdioConfig stdoutConfig = config.getStdoutConfig();
    if (stdoutConfig != null) {
      switch (stdoutConfig.getType()) {
        case INHERIT:
          NATIVE_BINDINGS.componentLinkerSetWasiInheritStdout(nativeLinker, 1);
          break;
        case NULL:
          // Don't inherit stdout — native layer defaults to discarding output
          break;
        default:
          LOGGER.warning(
              "Unsupported stdout config type for component model: " + stdoutConfig.getType());
          break;
      }
    } else if (config.isInheritStdout()) {
      NATIVE_BINDINGS.componentLinkerSetWasiInheritStdout(nativeLinker, 1);
    }
  }

  /**
   * Applies stderr configuration from WasiStdioConfig or falls back to boolean inherit flag.
   *
   * @param config the WASI preview 2 configuration
   * @param nativeLinker the native linker memory segment
   */
  private void applyStderrConfig(
      final WasiPreview2Config config, final MemorySegment nativeLinker) {
    final WasiStdioConfig stderrConfig = config.getStderrConfig();
    if (stderrConfig != null) {
      switch (stderrConfig.getType()) {
        case INHERIT:
          NATIVE_BINDINGS.componentLinkerSetWasiInheritStderr(nativeLinker, 1);
          break;
        case NULL:
          // Don't inherit stderr — native layer defaults to discarding output
          break;
        default:
          LOGGER.warning(
              "Unsupported stderr config type for component model: " + stderrConfig.getType());
          break;
      }
    } else if (config.isInheritStderr()) {
      NATIVE_BINDINGS.componentLinkerSetWasiInheritStderr(nativeLinker, 1);
    }
  }

  /**
   * Creates an upcall stub for a wall clock and registers it with native code.
   *
   * @param clock the wall clock implementation
   */
  private void applyWasiWallClock(final WasiWallClock clock) {
    final long callbackId = NEXT_CALLBACK_ID.getAndIncrement();
    WASI_CALLBACKS.put(callbackId, clock);

    try {
      // Wall clock now/resolution: (callback_id: long, seconds_out: ptr, nanos_out: ptr) -> void
      final MethodHandle nowHandle =
          MethodHandles.lookup()
              .findStatic(
                  PanamaComponentLinker.class,
                  "wallClockNowCallback",
                  MethodType.methodType(
                      void.class, long.class, MemorySegment.class, MemorySegment.class));
      final MemorySegment nowStub =
          java.lang.foreign.Linker.nativeLinker()
              .upcallStub(
                  nowHandle,
                  FunctionDescriptor.ofVoid(
                      ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                  arena);

      final MethodHandle resHandle =
          MethodHandles.lookup()
              .findStatic(
                  PanamaComponentLinker.class,
                  "wallClockResolutionCallback",
                  MethodType.methodType(
                      void.class, long.class, MemorySegment.class, MemorySegment.class));
      final MemorySegment resStub =
          java.lang.foreign.Linker.nativeLinker()
              .upcallStub(
                  resHandle,
                  FunctionDescriptor.ofVoid(
                      ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                  arena);

      NATIVE_BINDINGS.componentLinkerSetWasiWallClock(nativeLinker, nowStub, resStub, callbackId);
    } catch (final NoSuchMethodException | IllegalAccessException e) {
      LOGGER.warning("Failed to set WASI wall clock: " + e.getMessage());
    }
  }

  /**
   * Creates an upcall stub for a monotonic clock and registers it with native code.
   *
   * @param clock the monotonic clock implementation
   */
  private void applyWasiMonotonicClock(final WasiMonotonicClock clock) {
    final long callbackId = NEXT_CALLBACK_ID.getAndIncrement();
    WASI_CALLBACKS.put(callbackId, clock);

    try {
      // Monotonic clock now/resolution: (callback_id: long) -> long (nanoseconds)
      final MethodHandle nowHandle =
          MethodHandles.lookup()
              .findStatic(
                  PanamaComponentLinker.class,
                  "monotonicClockNowCallback",
                  MethodType.methodType(long.class, long.class));
      final MemorySegment nowStub =
          java.lang.foreign.Linker.nativeLinker()
              .upcallStub(
                  nowHandle,
                  FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG),
                  arena);

      final MethodHandle resHandle =
          MethodHandles.lookup()
              .findStatic(
                  PanamaComponentLinker.class,
                  "monotonicClockResolutionCallback",
                  MethodType.methodType(long.class, long.class));
      final MemorySegment resStub =
          java.lang.foreign.Linker.nativeLinker()
              .upcallStub(
                  resHandle,
                  FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG),
                  arena);

      NATIVE_BINDINGS.componentLinkerSetWasiMonotonicClock(
          nativeLinker, nowStub, resStub, callbackId);
    } catch (final NoSuchMethodException | IllegalAccessException e) {
      LOGGER.warning("Failed to set WASI monotonic clock: " + e.getMessage());
    }
  }

  /**
   * Creates an upcall stub for a random source and registers it with native code.
   *
   * @param source the random source implementation
   * @param secure true for secure random, false for insecure
   */
  private void applyWasiRandomSource(final WasiRandomSource source, final boolean secure) {
    final long callbackId = NEXT_CALLBACK_ID.getAndIncrement();
    WASI_CALLBACKS.put(callbackId, source);

    try {
      // fill_bytes: (callback_id: long, buf_ptr: ptr, buf_len: long) -> void
      final MethodHandle fillHandle =
          MethodHandles.lookup()
              .findStatic(
                  PanamaComponentLinker.class,
                  "randomFillBytesCallback",
                  MethodType.methodType(void.class, long.class, MemorySegment.class, long.class));
      final MemorySegment fillStub =
          java.lang.foreign.Linker.nativeLinker()
              .upcallStub(
                  fillHandle,
                  FunctionDescriptor.ofVoid(
                      ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
                  arena);

      if (secure) {
        NATIVE_BINDINGS.componentLinkerSetWasiSecureRandom(nativeLinker, fillStub, callbackId);
      } else {
        NATIVE_BINDINGS.componentLinkerSetWasiInsecureRandom(nativeLinker, fillStub, callbackId);
      }
    } catch (final NoSuchMethodException | IllegalAccessException e) {
      LOGGER.warning("Failed to set WASI random source: " + e.getMessage());
    }
  }

  /**
   * Creates an upcall stub for socket address check and registers it with native code.
   *
   * @param check the socket address check callback
   */
  private void applyWasiSocketAddrCheck(final SocketAddrCheck check) {
    final long callbackId = NEXT_CALLBACK_ID.getAndIncrement();
    WASI_CALLBACKS.put(callbackId, check);

    try {
      // check: (callback_id, ip_version, ip_bytes_ptr, ip_bytes_len, port, use_type) -> int
      final MethodHandle checkHandle =
          MethodHandles.lookup()
              .findStatic(
                  PanamaComponentLinker.class,
                  "socketAddrCheckCallback",
                  MethodType.methodType(
                      int.class,
                      long.class,
                      int.class,
                      MemorySegment.class,
                      long.class,
                      short.class,
                      int.class));
      final MemorySegment checkStub =
          java.lang.foreign.Linker.nativeLinker()
              .upcallStub(
                  checkHandle,
                  FunctionDescriptor.of(
                      ValueLayout.JAVA_INT,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.JAVA_INT,
                      ValueLayout.ADDRESS,
                      ValueLayout.JAVA_LONG,
                      ValueLayout.JAVA_SHORT,
                      ValueLayout.JAVA_INT),
                  arena);

      NATIVE_BINDINGS.componentLinkerSetWasiSocketAddrCheck(nativeLinker, checkStub, callbackId);
    } catch (final NoSuchMethodException | IllegalAccessException e) {
      LOGGER.warning("Failed to set WASI socket addr check: " + e.getMessage());
    }
  }

  // --- Static upcall target methods for WASI callbacks ---

  @SuppressWarnings("unused")
  static void wallClockNowCallback(
      final long callbackId, final MemorySegment secondsOut, final MemorySegment nanosOut) {
    final Object obj = WASI_CALLBACKS.get(callbackId);
    if (obj instanceof WasiWallClock) {
      final DateTime dt = ((WasiWallClock) obj).now();
      secondsOut.set(ValueLayout.JAVA_LONG, 0, dt.getSeconds());
      nanosOut.set(ValueLayout.JAVA_INT, 0, dt.getNanoseconds());
    }
  }

  @SuppressWarnings("unused")
  static void wallClockResolutionCallback(
      final long callbackId, final MemorySegment secondsOut, final MemorySegment nanosOut) {
    final Object obj = WASI_CALLBACKS.get(callbackId);
    if (obj instanceof WasiWallClock) {
      final DateTime dt = ((WasiWallClock) obj).resolution();
      secondsOut.set(ValueLayout.JAVA_LONG, 0, dt.getSeconds());
      nanosOut.set(ValueLayout.JAVA_INT, 0, dt.getNanoseconds());
    }
  }

  @SuppressWarnings("unused")
  static long monotonicClockNowCallback(final long callbackId) {
    final Object obj = WASI_CALLBACKS.get(callbackId);
    if (obj instanceof WasiMonotonicClock) {
      return ((WasiMonotonicClock) obj).now();
    }
    return 0L;
  }

  @SuppressWarnings("unused")
  static long monotonicClockResolutionCallback(final long callbackId) {
    final Object obj = WASI_CALLBACKS.get(callbackId);
    if (obj instanceof WasiMonotonicClock) {
      return ((WasiMonotonicClock) obj).resolution();
    }
    return 1L; // 1 nanosecond default resolution
  }

  @SuppressWarnings("unused")
  static void randomFillBytesCallback(
      final long callbackId, final MemorySegment bufPtr, final long bufLen) {
    final Object obj = WASI_CALLBACKS.get(callbackId);
    if (obj instanceof WasiRandomSource && bufLen > 0) {
      final byte[] bytes = new byte[(int) bufLen];
      ((WasiRandomSource) obj).fillBytes(bytes);
      MemorySegment.copy(bytes, 0, bufPtr, ValueLayout.JAVA_BYTE, 0, (int) bufLen);
    }
  }

  @SuppressWarnings("unused")
  static int socketAddrCheckCallback(
      final long callbackId,
      final int ipVersion,
      final MemorySegment ipBytesPtr,
      final long ipBytesLen,
      final short port,
      final int useType) {
    final Object obj = WASI_CALLBACKS.get(callbackId);
    if (obj instanceof SocketAddrCheck) {
      try {
        final byte[] ipBytes = new byte[(int) ipBytesLen];
        MemorySegment.copy(ipBytesPtr, ValueLayout.JAVA_BYTE, 0, ipBytes, 0, (int) ipBytesLen);
        final InetAddress addr = InetAddress.getByAddress(ipBytes);
        final InetSocketAddress socketAddr = new InetSocketAddress(addr, Short.toUnsignedInt(port));
        final SocketAddrUse addrUse = SocketAddrUse.fromValue(useType);
        return ((SocketAddrCheck) obj).check(socketAddr, addrUse) ? 1 : 0;
      } catch (final Exception e) {
        LOGGER.warning("Socket addr check callback failed: " + e.getMessage());
        return 0; // Deny on error
      }
    }
    return 1; // Allow by default if callback not found
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
    final String interfaceKey = interfaceNamespace + ":" + interfaceName;
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
    final String interfaceKey = interfaceNamespace + ":" + interfaceName;
    final Set<String> functions = definedInterfaces.get(interfaceKey);
    return functions != null && functions.contains(functionName);
  }

  @Override
  public Set<String> getDefinedInterfaces() {
    ensureNotClosed();
    return new HashSet<>(definedInterfaces.keySet());
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

    final String key = interfaceNamespace + ":" + interfaceName;
    final Set<String> functions = definedInterfaces.get(key);
    return functions != null ? new HashSet<>(functions) : Set.of();
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
    final String fromKey = fromNamespace + ":" + fromInterface;
    final String toKey = toNamespace + ":" + toInterface;

    final Set<String> sourceFunctions = definedInterfaces.get(fromKey);
    if (sourceFunctions != null) {
      definedInterfaces.computeIfAbsent(toKey, k -> new HashSet<>()).addAll(sourceFunctions);

      // Also copy the host function registrations (witPath format: ns:iface/iface#func)
      for (final String function : sourceFunctions) {
        final String fromPath =
            fromNamespace + ":" + fromInterface + "/" + fromInterface + "#" + function;
        final String toPath = toNamespace + ":" + toInterface + "/" + toInterface + "#" + function;
        final Long callbackId = hostFunctions.get(fromPath);
        if (callbackId != null) {
          hostFunctions.put(toPath, callbackId);
        }
      }
    }

    LOGGER.fine("Created interface alias from " + fromKey + " to " + toKey);
  }

  @Override
  public void allowShadowing(final boolean allow) {
    ensureNotClosed();
    NATIVE_BINDINGS.componentLinkerAllowShadowing(nativeLinker, allow ? 1 : 0);
  }

  @Override
  public void defineUnknownImportsAsTraps(final Component component) throws WasmException {
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }
    ensureNotClosed();

    if (!(component instanceof PanamaComponentImpl)) {
      throw new IllegalArgumentException("Component must be a Panama implementation");
    }

    final PanamaComponentImpl panamaComponent = (PanamaComponentImpl) component;

    final int result =
        NATIVE_BINDINGS.componentLinkerDefineUnknownImportsAsTraps(
            nativeLinker, panamaComponent.getNativeHandle());
    if (result != 0) {
      throw new WasmException("Failed to define unknown imports as traps");
    }
  }

  @Override
  public ComponentTypeInfo substitutedComponentType(final Component component)
      throws WasmException {
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }
    ensureNotClosed();

    if (!(component instanceof PanamaComponentImpl)) {
      return component.componentType();
    }

    final PanamaComponentImpl panamaComponent = (PanamaComponentImpl) component;

    try (Arena tempArena = Arena.ofConfined()) {
      final MemorySegment jsonOut = tempArena.allocate(ValueLayout.ADDRESS);
      final int errorCode =
          NATIVE_BINDINGS.componentLinkerSubstitutedTypeJson(
              nativeLinker, panamaComponent.getNativeHandle(), jsonOut);

      if (errorCode != 0) {
        return component.componentType();
      }

      final MemorySegment jsonPtr = jsonOut.get(ValueLayout.ADDRESS, 0);
      if (jsonPtr == null || jsonPtr.equals(MemorySegment.NULL)) {
        return component.componentType();
      }

      try {
        final MemorySegment unbounded = jsonPtr.reinterpret(Long.MAX_VALUE);
        final String json = unbounded.getString(0);
        return ComponentTypeCodec.deserialize(json);
      } finally {
        NATIVE_BINDINGS.componentFreeString(jsonPtr);
      }
    }
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
