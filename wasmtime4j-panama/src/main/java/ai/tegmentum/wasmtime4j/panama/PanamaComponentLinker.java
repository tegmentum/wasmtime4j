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
import ai.tegmentum.wasmtime4j.component.ComponentVal;
import ai.tegmentum.wasmtime4j.component.ConcurrentCallCodec;
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
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
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

  private static volatile MemorySegment hostFunctionUpcallStub;
  private static volatile MemorySegment resourceDestructorUpcallStub;

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

    // Build WIT path: "namespace:package/interface#function"
    final String witPath =
        interfaceNamespace + ":" + interfaceName + "/" + interfaceName + "#" + functionName;
    defineHostFunctionNative(witPath, implementation, false);
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

    defineHostFunctionNative(witPath, implementation, false);
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

    // Build WIT path: "namespace:package/interface#function"
    final String witPath =
        interfaceNamespace + ":" + interfaceName + "/" + interfaceName + "#" + functionName;
    defineHostFunctionNative(witPath, implementation, true);
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

    defineHostFunctionNative(witPath, implementation, true);
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

    // Set up destructor if provided
    MemorySegment destructorStub = MemorySegment.NULL;
    long destructorCallbackId = 0L;
    if (resourceDefinition.getDestructor().isPresent()) {
      destructorCallbackId = NEXT_CALLBACK_ID.getAndIncrement();
      WASI_CALLBACKS.put(destructorCallbackId, resourceDefinition.getDestructor().get());
      destructorStub = getOrCreateResourceDestructorUpcallStub();
    }

    // Call native define_resource
    try (Arena tempArena = Arena.ofConfined()) {
      final byte[] pathBytes = interfacePath.getBytes(StandardCharsets.UTF_8);
      final MemorySegment pathSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, pathBytes);

      final byte[] nameBytes = resourceName.getBytes(StandardCharsets.UTF_8);
      final MemorySegment nameSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, nameBytes);

      final int errorCode =
          NATIVE_BINDINGS.componentLinkerDefineResource(
              nativeLinker,
              pathSegment,
              pathBytes.length,
              nameSegment,
              nameBytes.length,
              resourceId,
              destructorStub,
              destructorCallbackId);

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
  public void defineModule(
      final String instancePath, final String name, final ai.tegmentum.wasmtime4j.Module module)
      throws WasmException {
    if (instancePath == null) {
      throw new IllegalArgumentException("Instance path cannot be null");
    }
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Name cannot be null or empty");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    ensureNotClosed();

    if (!(module instanceof PanamaModule)) {
      throw new IllegalArgumentException(
          "Module must be a PanamaModule, got: " + module.getClass().getName());
    }
    final MemorySegment moduleHandle = ((PanamaModule) module).getNativeModule();

    try (Arena tempArena = Arena.ofConfined()) {
      final byte[] pathBytes = instancePath.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment pathSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, pathBytes);

      final byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment nameSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, nameBytes);

      final int errorCode =
          NATIVE_BINDINGS.componentLinkerDefineModule(
              nativeLinker,
              pathSegment,
              pathBytes.length,
              nameSegment,
              nameBytes.length,
              moduleHandle);

      if (errorCode != 0) {
        throw PanamaErrorMapper.mapNativeError(errorCode, "Failed to define module: " + name);
      }
    }

    LOGGER.fine("Defined core module '" + name + "' on instance path '" + instancePath + "'");
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

      // Also copy the host function registrations (witPath format: ns/iface#func)
      for (final String function : sourceFunctions) {
        final String fromPath = fromNamespace + "/" + fromInterface + "#" + function;
        final String toPath = toNamespace + "/" + toInterface + "#" + function;
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
   * Defines a host function via native FFI, registering it with the Wasmtime linker.
   *
   * <p>This creates a Panama upcall stub for the callback and passes it to native code which
   * registers it with Wasmtime's component linker.
   *
   * @param witPath the WIT path (e.g., "wasi:cli/stdout#print")
   * @param implementation the host function implementation
   * @param async true to register as async function
   * @throws WasmException if registration fails
   */
  private void defineHostFunctionNative(
      final String witPath, final ComponentHostFunction implementation, final boolean async)
      throws WasmException {
    // Register the callback in Java-side map
    final long callbackId = registerHostFunctionCallback(implementation);

    // Get or create the shared upcall stub
    final MemorySegment stub = getOrCreateHostFunctionUpcallStub();

    // Call native to register the host function with Wasmtime's linker
    try (Arena tempArena = Arena.ofConfined()) {
      final byte[] pathBytes = witPath.getBytes(StandardCharsets.UTF_8);
      final MemorySegment pathSegment = tempArena.allocateFrom(ValueLayout.JAVA_BYTE, pathBytes);

      final int errorCode;
      if (async) {
        errorCode =
            NATIVE_BINDINGS.componentLinkerDefineHostFunctionAsync(
                nativeLinker, pathSegment, pathBytes.length, stub, callbackId);
      } else {
        errorCode =
            NATIVE_BINDINGS.componentLinkerDefineHostFunction(
                nativeLinker, pathSegment, pathBytes.length, stub, callbackId);
      }

      if (errorCode != 0) {
        HOST_CALLBACKS.remove(callbackId);
        throw PanamaErrorMapper.mapNativeError(
            errorCode, "Failed to define host function: " + witPath);
      }
    }

    // Track in Java-side maps for hasFunction/hasInterface queries
    hostFunctions.put(witPath, callbackId);

    // Extract function name and build interface key matching hasInterface format (ns:name)
    final int hashIndex = witPath.indexOf('#');
    if (hashIndex > 0) {
      final String interfacePath = witPath.substring(0, hashIndex);
      final String functionName = witPath.substring(hashIndex + 1);
      // Extract "ns:pkg" from "ns:pkg/iface" — drop everything after the last '/'
      final int lastSlash = interfacePath.lastIndexOf('/');
      final String interfaceKey =
          lastSlash > 0 ? interfacePath.substring(0, lastSlash) : interfacePath;
      definedInterfaces.computeIfAbsent(interfaceKey, k -> new HashSet<>()).add(functionName);
    }

    LOGGER.fine(
        "Defined "
            + (async ? "async " : "")
            + "component host function: "
            + witPath
            + " (callback ID: "
            + callbackId
            + ")");
  }

  /**
   * Gets or creates the shared upcall stub for host function callbacks.
   *
   * <p>This stub is shared across all host function registrations since the callback dispatch is
   * handled by the callback ID parameter.
   *
   * @return the upcall stub memory segment
   * @throws WasmException if stub creation fails
   */
  private MemorySegment getOrCreateHostFunctionUpcallStub() throws WasmException {
    MemorySegment stub = hostFunctionUpcallStub;
    if (stub != null) {
      return stub;
    }
    synchronized (PanamaComponentLinker.class) {
      stub = hostFunctionUpcallStub;
      if (stub != null) {
        return stub;
      }
      try {
        // Callback signature: (callback_id, params_json_ptr, params_json_len,
        //                      results_json_out, results_json_len_out) -> int
        final MethodHandle callbackHandle =
            MethodHandles.lookup()
                .findStatic(
                    PanamaComponentLinker.class,
                    "hostFunctionCallback",
                    MethodType.methodType(
                        int.class,
                        long.class,
                        MemorySegment.class,
                        long.class,
                        MemorySegment.class,
                        MemorySegment.class));
        stub =
            java.lang.foreign.Linker.nativeLinker()
                .upcallStub(
                    callbackHandle,
                    FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS),
                    Arena.global());
        hostFunctionUpcallStub = stub;
        return stub;
      } catch (final NoSuchMethodException | IllegalAccessException e) {
        throw new WasmException("Failed to create host function upcall stub: " + e.getMessage());
      }
    }
  }

  /**
   * Static upcall target for host function callbacks from native code.
   *
   * <p>Called by native code when a WASM component invokes a host function. Deserializes the params
   * JSON, dispatches to the registered Java callback, and serializes the results.
   *
   * @param callbackId the callback ID identifying the Java callback
   * @param paramsJsonPtr pointer to UTF-8 JSON array of params
   * @param paramsJsonLen length of the params JSON
   * @param resultsJsonOut pointer to write the result JSON pointer
   * @param resultsJsonLenOut pointer to write the result JSON length
   * @return 0 on success, non-zero on error
   */
  @SuppressWarnings("unused")
  static int hostFunctionCallback(
      final long callbackId,
      final MemorySegment paramsJsonPtr,
      final long paramsJsonLen,
      final MemorySegment resultsJsonOut,
      final MemorySegment resultsJsonLenOut) {
    try {
      // Look up the callback
      final ComponentHostFunctionWrapper wrapper = HOST_CALLBACKS.get(callbackId);
      if (wrapper == null) {
        LOGGER.warning("Host function callback not found for ID: " + callbackId);
        return -1;
      }

      // Deserialize params from JSON
      List<ComponentVal> params;
      if (paramsJsonLen > 0 && !paramsJsonPtr.equals(MemorySegment.NULL)) {
        final MemorySegment unbounded = paramsJsonPtr.reinterpret(paramsJsonLen);
        final byte[] jsonBytes = new byte[(int) paramsJsonLen];
        MemorySegment.copy(unbounded, ValueLayout.JAVA_BYTE, 0, jsonBytes, 0, (int) paramsJsonLen);
        final String paramsJson = new String(jsonBytes, StandardCharsets.UTF_8);
        params = ConcurrentCallCodec.deserializeVals(paramsJson);
      } else {
        params = List.of();
      }

      // Execute the callback
      final List<ComponentVal> results = wrapper.getImplementation().execute(params);

      // Serialize results to JSON
      final String resultsJson =
          results != null ? ConcurrentCallCodec.serializeVals(results) : "[]";
      final byte[] resultBytes = resultsJson.getBytes(StandardCharsets.UTF_8);

      // Allocate native buffer and copy results
      final MemorySegment resultBuf =
          NativeComponentBindings.getInstance()
              .componentHostCallbackAllocResult(resultBytes.length);
      if (resultBuf != null && !resultBuf.equals(MemorySegment.NULL)) {
        final MemorySegment unboundedResult = resultBuf.reinterpret(resultBytes.length);
        MemorySegment.copy(
            resultBytes, 0, unboundedResult, ValueLayout.JAVA_BYTE, 0, resultBytes.length);
        resultsJsonOut.set(ValueLayout.ADDRESS, 0, resultBuf);
        resultsJsonLenOut.set(ValueLayout.JAVA_LONG, 0, resultBytes.length);
      }

      return 0;
    } catch (final Exception e) {
      LOGGER.warning("Host function callback failed: " + e.getMessage());
      // Write error message to results buffer
      try {
        final byte[] errorBytes = e.getMessage().getBytes(StandardCharsets.UTF_8);
        final MemorySegment errorBuf =
            NativeComponentBindings.getInstance()
                .componentHostCallbackAllocResult(errorBytes.length);
        if (errorBuf != null && !errorBuf.equals(MemorySegment.NULL)) {
          final MemorySegment unboundedError = errorBuf.reinterpret(errorBytes.length);
          MemorySegment.copy(
              errorBytes, 0, unboundedError, ValueLayout.JAVA_BYTE, 0, errorBytes.length);
          resultsJsonOut.set(ValueLayout.ADDRESS, 0, errorBuf);
          resultsJsonLenOut.set(ValueLayout.JAVA_LONG, 0, errorBytes.length);
        }
      } catch (final Exception ignored) {
        // Cannot propagate error further
      }
      return -1;
    }
  }

  /**
   * Gets or creates the shared upcall stub for resource destructor callbacks.
   *
   * @return the upcall stub memory segment
   * @throws WasmException if stub creation fails
   */
  private MemorySegment getOrCreateResourceDestructorUpcallStub() throws WasmException {
    MemorySegment stub = resourceDestructorUpcallStub;
    if (stub != null) {
      return stub;
    }
    synchronized (PanamaComponentLinker.class) {
      stub = resourceDestructorUpcallStub;
      if (stub != null) {
        return stub;
      }
      try {
        // Destructor signature: (callback_id: u64, rep: u32) -> i32
        final MethodHandle destructorHandle =
            MethodHandles.lookup()
                .findStatic(
                    PanamaComponentLinker.class,
                    "resourceDestructorCallback",
                    MethodType.methodType(int.class, long.class, int.class));
        stub =
            java.lang.foreign.Linker.nativeLinker()
                .upcallStub(
                    destructorHandle,
                    FunctionDescriptor.of(
                        ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT),
                    Arena.global());
        resourceDestructorUpcallStub = stub;
        return stub;
      } catch (final NoSuchMethodException | IllegalAccessException e) {
        throw new WasmException(
            "Failed to create resource destructor upcall stub: " + e.getMessage());
      }
    }
  }

  /**
   * Static upcall target for resource destructor callbacks from native code.
   *
   * @param callbackId the callback ID identifying the Java destructor
   * @param rep the resource representation ID
   * @return 0 on success, non-zero on error
   */
  @SuppressWarnings({"unused", "unchecked"})
  static int resourceDestructorCallback(final long callbackId, final int rep) {
    try {
      final Object obj = WASI_CALLBACKS.get(callbackId);
      if (obj instanceof java.util.function.Consumer) {
        ((java.util.function.Consumer<Object>) obj).accept(rep);
      }
      return 0;
    } catch (final Exception e) {
      LOGGER.warning("Resource destructor callback failed: " + e.getMessage());
      return -1;
    }
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
