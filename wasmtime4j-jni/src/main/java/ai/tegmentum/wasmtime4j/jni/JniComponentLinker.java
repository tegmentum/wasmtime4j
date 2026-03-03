/*
 * Copyright 2025 Tegmentum AI
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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentHostFunction;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.component.ComponentInstancePre;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.component.ComponentResourceDefinition;
import ai.tegmentum.wasmtime4j.component.ComponentTypeCodec;
import ai.tegmentum.wasmtime4j.component.ComponentTypeInfo;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.util.StreamUtils;
import ai.tegmentum.wasmtime4j.wasi.WasiPreview2Config;
import ai.tegmentum.wasmtime4j.wasi.WasiStdioConfig;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * JNI implementation of the ComponentLinker interface.
 *
 * <p>Provides Component Model linker functionality through JNI bindings to the native Wasmtime
 * library.
 *
 * @param <T> the type of user data associated with stores
 * @since 1.0.0
 */
public final class JniComponentLinker<T> extends JniResource implements ComponentLinker<T> {
  private static final Logger LOGGER = Logger.getLogger(JniComponentLinker.class.getName());
  private static final ConcurrentHashMap<Long, ComponentHostFunctionWrapper>
      HOST_FUNCTION_CALLBACKS = new ConcurrentHashMap<>();
  private static final AtomicLong CALLBACK_ID_GENERATOR = new AtomicLong(1);

  private final Engine engine;
  private final Map<String, Long> hostFunctions = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> definedInterfaces = new ConcurrentHashMap<>();
  private final Set<Long> registeredCallbackIds = Collections.synchronizedSet(new HashSet<Long>());
  private final ai.tegmentum.wasmtime4j.component.DefaultResourceTable resourceTable =
      new ai.tegmentum.wasmtime4j.component.DefaultResourceTable();

  /**
   * Creates a new JNI component linker with the given native handle.
   *
   * @param nativeHandle the native handle
   * @param engine the engine
   */
  public JniComponentLinker(final long nativeHandle, final Engine engine) {
    super(nativeHandle);
    this.engine = engine;
  }

  @Override
  public Engine getEngine() {
    return engine;
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

    final String witPath =
        interfaceNamespace + ":" + interfaceName + "/" + interfaceName + "#" + functionName;
    final long callbackId = registerHostFunctionCallback(implementation);
    hostFunctions.put(witPath, callbackId);

    // Wire to native component linker
    try {
      nativeDefineHostFunction(nativeHandle, witPath, callbackId);
    } catch (final Exception e) {
      // Remove from local tracking if native call fails
      hostFunctions.remove(witPath);
      HOST_FUNCTION_CALLBACKS.remove(callbackId);
      registeredCallbackIds.remove(callbackId);
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Failed to define host function: " + e.getMessage(), e);
    }

    // Track in defined interfaces
    final String interfaceKey = interfaceNamespace + ":" + interfaceName;
    Set<String> functions = definedInterfaces.get(interfaceKey);
    if (functions == null) {
      functions = Collections.synchronizedSet(new HashSet<String>());
      definedInterfaces.put(interfaceKey, functions);
    }
    functions.add(functionName);

    LOGGER.fine("Defined component function: " + witPath);
  }

  @Override
  public void defineFunction(final String witPath, final ComponentHostFunction implementation)
      throws WasmException {
    if (witPath == null) {
      throw new IllegalArgumentException("WIT path cannot be null");
    }
    if (implementation == null) {
      throw new IllegalArgumentException("Implementation cannot be null");
    }
    ensureNotClosed();

    final long callbackId = registerHostFunctionCallback(implementation);
    hostFunctions.put(witPath, callbackId);

    // Wire to native component linker
    try {
      nativeDefineHostFunction(nativeHandle, witPath, callbackId);
    } catch (final Exception e) {
      // Remove from local tracking if native call fails
      hostFunctions.remove(witPath);
      HOST_FUNCTION_CALLBACKS.remove(callbackId);
      registeredCallbackIds.remove(callbackId);
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Failed to define host function: " + e.getMessage(), e);
    }

    LOGGER.fine("Defined component function: " + witPath);
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

    final String witPath =
        interfaceNamespace + ":" + interfaceName + "/" + interfaceName + "#" + functionName;
    defineFunctionAsync(witPath, implementation);
  }

  @Override
  public void defineFunctionAsync(final String witPath, final ComponentHostFunction implementation)
      throws WasmException {
    if (witPath == null) {
      throw new IllegalArgumentException("WIT path cannot be null");
    }
    if (implementation == null) {
      throw new IllegalArgumentException("Implementation cannot be null");
    }
    ensureNotClosed();

    final long callbackId = registerHostFunctionCallback(implementation);
    hostFunctions.put(witPath, callbackId);

    // Wire to native component linker
    try {
      nativeDefineHostFunctionAsync(nativeHandle, witPath, callbackId);
    } catch (final Exception e) {
      // Remove from local tracking if native call fails
      hostFunctions.remove(witPath);
      HOST_FUNCTION_CALLBACKS.remove(callbackId);
      registeredCallbackIds.remove(callbackId);
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Failed to define async host function: " + e.getMessage(), e);
    }

    LOGGER.fine("Defined async component function: " + witPath);
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

    // Register constructor callback if present
    long constructorCallbackId = 0;
    if (resourceDefinition.getConstructor().isPresent()) {
      @SuppressWarnings("unchecked")
      final ComponentResourceDefinition<Object> typedDef =
          (ComponentResourceDefinition<Object>) resourceDefinition;
      final ComponentResourceDefinition.ResourceConstructor<Object> ctor =
          typedDef.getConstructor().get();
      constructorCallbackId =
          registerHostFunctionCallback(
              params -> {
                final Object resource = ctor.construct(params);
                final int handle = resourceTable.push(resource);
                return java.util.Collections.singletonList(
                    ai.tegmentum.wasmtime4j.component.ComponentVal.s32(handle));
              });
    }

    // Register destructor callback if present
    long destructorCallbackId = 0;
    if (resourceDefinition.getDestructor().isPresent()) {
      @SuppressWarnings("unchecked")
      final ComponentResourceDefinition<Object> typedDef =
          (ComponentResourceDefinition<Object>) resourceDefinition;
      final java.util.function.Consumer<Object> dtor = typedDef.getDestructor().get();
      destructorCallbackId =
          registerHostFunctionCallback(
              params -> {
                if (!params.isEmpty()) {
                  final int handle = params.get(0).asS32();
                  final java.util.Optional<Object> entry =
                      resourceTable.delete(handle, Object.class);
                  entry.ifPresent(dtor);
                }
                return java.util.Collections.emptyList();
              });
    }

    // Wire to native component linker
    try {
      nativeDefineResource(
          nativeHandle,
          interfaceNamespace,
          interfaceName,
          resourceName,
          constructorCallbackId,
          destructorCallbackId);
    } catch (final Exception e) {
      // Remove from local tracking if native call fails
      if (constructorCallbackId > 0) {
        HOST_FUNCTION_CALLBACKS.remove(constructorCallbackId);
        registeredCallbackIds.remove(constructorCallbackId);
      }
      if (destructorCallbackId > 0) {
        HOST_FUNCTION_CALLBACKS.remove(destructorCallbackId);
        registeredCallbackIds.remove(destructorCallbackId);
      }
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Failed to define resource: " + e.getMessage(), e);
    }

    // Track resource in defined interfaces
    final String interfaceKey = interfaceNamespace + ":" + interfaceName;
    Set<String> functions = definedInterfaces.get(interfaceKey);
    if (functions == null) {
      functions = Collections.synchronizedSet(new HashSet<String>());
      definedInterfaces.put(interfaceKey, functions);
    }
    functions.add("[resource]" + resourceName);

    LOGGER.fine(
        "Defined component resource: "
            + interfaceNamespace
            + ":"
            + interfaceName
            + "/"
            + resourceName);
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

    if (!(module instanceof JniModule)) {
      throw new IllegalArgumentException(
          "Module must be a JniModule, got: " + module.getClass().getName());
    }
    final long moduleHandle = ((JniModule) module).getNativeHandle();

    try {
      nativeDefineModule(nativeHandle, instancePath, name, moduleHandle);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Failed to define module: " + e.getMessage(), e);
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
      Set<String> functions = definedInterfaces.get(interfaceKey);
      if (functions == null) {
        functions = Collections.synchronizedSet(new HashSet<String>());
        definedInterfaces.put(interfaceKey, functions);
      }
      functions.add(exportName);
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

    // First instantiate, then link exports
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

    if (!(component instanceof JniComponentImpl)) {
      throw new WasmException(
          "Component must be a JniComponentImpl for JNI ComponentLinker pre-instantiation");
    }

    final JniComponentImpl jniComponent = (JniComponentImpl) component;
    final long componentHandle = jniComponent.getNativeHandle();

    final long preHandle = nativeInstantiatePre(nativeHandle, componentHandle);
    if (preHandle == 0) {
      throw new WasmException("Failed to pre-instantiate component");
    }

    return new JniComponentInstancePre(preHandle, engine, jniComponent);
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

    if (!(component instanceof JniComponentImpl)) {
      throw new WasmException(
          "Component must be a JniComponentImpl for JNI ComponentLinker instantiation");
    }

    final JniComponentImpl jniComponent = (JniComponentImpl) component;
    final long storeHandle = getStoreHandle(store);
    final long componentHandle = jniComponent.getNativeHandle();

    final long instanceHandle =
        nativeInstantiateWithLinker(nativeHandle, storeHandle, componentHandle);
    if (instanceHandle == 0) {
      throw new WasmException("Failed to instantiate component with linker");
    }

    final JniComponent.JniComponentInstanceHandle instanceWrapper =
        new JniComponent.JniComponentInstanceHandle(instanceHandle);
    return new JniComponentInstanceImpl(
        instanceWrapper, jniComponent, new ComponentInstanceConfig());
  }

  /**
   * Gets the native store handle from a Store object.
   *
   * @param store the store
   * @return the native handle or 0 if not available
   */
  private long getStoreHandle(final Store store) {
    if (store instanceof JniStore) {
      return ((JniStore) store).getNativeHandle();
    }
    return 0;
  }

  @Override
  public void enableWasiPreview2() throws WasmException {
    ensureNotClosed();

    try {
      nativeEnableWasiP2(nativeHandle);
    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw (WasmException) e;
      }
      throw new WasmException("Failed to enable WASI Preview 2", e);
    }
  }

  @Override
  public void enableWasiPreview2(final WasiPreview2Config config) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }
    ensureNotClosed();

    // Apply config settings before enabling WASI
    applyWasiConfig(config);

    // Enable WASI Preview 2
    enableWasiPreview2();
  }

  private void applyWasiConfig(final WasiPreview2Config config) {
    // Apply stdio configuration (WasiStdioConfig takes precedence over boolean flags)
    if (config.isInheritStdio()) {
      nativeSetWasiInheritStdio(nativeHandle, true);
    } else {
      applyStdinConfig(config);
      applyStdoutConfig(config);
      applyStderrConfig(config);
    }

    // Apply inherit env
    if (config.isInheritEnv()) {
      nativeSetWasiInheritEnv(nativeHandle, true);
    }

    // Apply inherit args
    if (config.isInheritArgs()) {
      nativeSetWasiInheritArgs(nativeHandle, true);
    }

    // Apply args
    if (config.getArgs() != null && !config.getArgs().isEmpty()) {
      nativeSetWasiArgs(nativeHandle, config.getArgs().toArray(new String[0]));
    }

    // Apply env vars
    if (config.getEnv() != null && !config.getEnv().isEmpty()) {
      for (final java.util.Map.Entry<String, String> entry : config.getEnv().entrySet()) {
        nativeAddWasiEnv(nativeHandle, entry.getKey(), entry.getValue());
      }
    }

    // Apply preopened dirs
    if (config.getPreopenDirs() != null) {
      for (final WasiPreview2Config.PreopenDir dir : config.getPreopenDirs()) {
        nativeAddWasiPreopenDir(
            nativeHandle,
            dir.getHostPath().toAbsolutePath().toString(),
            dir.getGuestPath(),
            dir.getDirPerms().getBits(),
            dir.getFilePerms().getBits());
      }
    }

    // Apply network controls
    nativeSetWasiAllowNetwork(nativeHandle, config.isAllowNetwork());
    nativeSetWasiAllowTcp(nativeHandle, config.isAllowTcp());
    nativeSetWasiAllowUdp(nativeHandle, config.isAllowUdp());
    nativeSetWasiAllowIpNameLookup(nativeHandle, config.isAllowIpNameLookup());

    // Apply blocking current thread
    nativeSetWasiAllowBlockingCurrentThread(nativeHandle, config.isAllowBlockingCurrentThread());

    // Apply insecure random seed
    if (config.hasInsecureRandomSeed()) {
      nativeSetWasiInsecureRandomSeed(nativeHandle, config.getInsecureRandomSeed());
    }

    // Apply custom wall clock
    if (config.getWallClock() != null) {
      nativeSetWasiWallClock(nativeHandle, config.getWallClock());
    }

    // Apply custom monotonic clock
    if (config.getMonotonicClock() != null) {
      nativeSetWasiMonotonicClock(nativeHandle, config.getMonotonicClock());
    }

    // Apply custom secure random
    if (config.getSecureRandom() != null) {
      nativeSetWasiSecureRandom(nativeHandle, config.getSecureRandom());
    }

    // Apply custom insecure random
    if (config.getInsecureRandom() != null) {
      nativeSetWasiInsecureRandom(nativeHandle, config.getInsecureRandom());
    }

    // Apply socket address check
    if (config.getSocketAddrCheck() != null) {
      nativeSetWasiSocketAddrCheck(nativeHandle, config.getSocketAddrCheck());
    }
  }

  private void applyStdinConfig(final WasiPreview2Config config) {
    final WasiStdioConfig stdinConfig = config.getStdinConfig();
    if (stdinConfig != null) {
      switch (stdinConfig.getType()) {
        case INHERIT:
          nativeSetWasiInheritStdin(nativeHandle, true);
          break;
        case INPUT_STREAM:
          try {
            final byte[] bytes = StreamUtils.readAllBytes(stdinConfig.getInputStream());
            nativeSetWasiStdinBytes(nativeHandle, bytes);
          } catch (java.io.IOException e) {
            LOGGER.warning("Failed to read stdin InputStream: " + e.getMessage());
          }
          break;
        case NULL:
          // Don't inherit stdin — native layer defaults to empty stdin
          nativeSetWasiStdinBytes(nativeHandle, new byte[0]);
          break;
        default:
          LOGGER.warning(
              "Unsupported stdin config type for component model: " + stdinConfig.getType());
          break;
      }
    } else if (config.isInheritStdin()) {
      nativeSetWasiInheritStdin(nativeHandle, true);
    }
  }

  private void applyStdoutConfig(final WasiPreview2Config config) {
    final WasiStdioConfig stdoutConfig = config.getStdoutConfig();
    if (stdoutConfig != null) {
      switch (stdoutConfig.getType()) {
        case INHERIT:
          nativeSetWasiInheritStdout(nativeHandle, true);
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
      nativeSetWasiInheritStdout(nativeHandle, true);
    }
  }

  private void applyStderrConfig(final WasiPreview2Config config) {
    final WasiStdioConfig stderrConfig = config.getStderrConfig();
    if (stderrConfig != null) {
      switch (stderrConfig.getType()) {
        case INHERIT:
          nativeSetWasiInheritStderr(nativeHandle, true);
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
      nativeSetWasiInheritStderr(nativeHandle, true);
    }
  }

  @Override
  public boolean isValid() {
    return !isClosed() && nativeHandle != 0;
  }

  @Override
  public boolean hasInterface(final String interfaceNamespace, final String interfaceName) {
    if (interfaceNamespace == null) {
      throw new IllegalArgumentException("Interface namespace cannot be null");
    }
    if (interfaceName == null) {
      throw new IllegalArgumentException("Interface name cannot be null");
    }
    final String key = interfaceNamespace + ":" + interfaceName;
    return definedInterfaces.containsKey(key);
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
    final String key = interfaceNamespace + ":" + interfaceName;
    final Set<String> functions = definedInterfaces.get(key);
    return functions != null && functions.contains(functionName);
  }

  @Override
  public Set<String> getDefinedInterfaces() {
    return Collections.unmodifiableSet(new HashSet<>(definedInterfaces.keySet()));
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
    final String key = interfaceNamespace + ":" + interfaceName;
    final Set<String> functions = definedInterfaces.get(key);
    return functions != null
        ? Collections.unmodifiableSet(new HashSet<>(functions))
        : Collections.<String>emptySet();
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

    final String fromKey = fromNamespace + ":" + fromInterface;
    final String toKey = toNamespace + ":" + toInterface;

    final Set<String> functions = definedInterfaces.get(fromKey);
    if (functions != null) {
      final Set<String> aliasFunctions = Collections.synchronizedSet(new HashSet<String>());
      aliasFunctions.addAll(functions);
      definedInterfaces.put(toKey, aliasFunctions);
    }

    LOGGER.fine("Created interface alias: " + fromKey + " -> " + toKey);
  }

  @Override
  public void allowShadowing(final boolean allow) {
    ensureNotClosed();
    nativeAllowShadowing(nativeHandle, allow);
  }

  @Override
  public void defineUnknownImportsAsTraps(final Component component) throws WasmException {
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }
    ensureNotClosed();

    if (!(component instanceof JniComponentImpl)) {
      throw new WasmException("Component must be a JniComponentImpl for JNI ComponentLinker");
    }

    final JniComponentImpl jniComponent = (JniComponentImpl) component;
    final long componentHandle = jniComponent.getNativeHandle();

    final int result = nativeDefineUnknownImportsAsTraps(nativeHandle, componentHandle);
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

    if (!(component instanceof JniComponentImpl)) {
      return component.componentType();
    }

    final JniComponentImpl jniComponent = (JniComponentImpl) component;
    try {
      final String json =
          JniComponent.nativeGetSubstitutedComponentTypeJson(
              nativeHandle, jniComponent.getNativeHandle());
      if (json == null) {
        return component.componentType();
      }
      return ComponentTypeCodec.deserialize(json);
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      return component.componentType();
    }
  }

  @Override
  protected void doClose() throws Exception {
    cleanupHostFunctionCallbacks();
    nativeDestroyComponentLinker(nativeHandle);
  }

  @Override
  protected String getResourceType() {
    return "ComponentLinker";
  }

  private long registerHostFunctionCallback(final ComponentHostFunction implementation) {
    final long callbackId = CALLBACK_ID_GENERATOR.getAndIncrement();
    final ComponentHostFunctionWrapper wrapper =
        new ComponentHostFunctionWrapper(callbackId, implementation);
    HOST_FUNCTION_CALLBACKS.put(callbackId, wrapper);
    registeredCallbackIds.add(callbackId);
    return callbackId;
  }

  private void cleanupHostFunctionCallbacks() {
    for (final Long callbackId : registeredCallbackIds) {
      HOST_FUNCTION_CALLBACKS.remove(callbackId);
    }
    registeredCallbackIds.clear();
  }

  /**
   * Dispatches a destructor callback from native code.
   *
   * <p>Called from the Rust JNI layer when a component resource is dropped. Looks up the registered
   * callback by ID and invokes it with the resource representation handle.
   *
   * @param callbackId the callback ID registered during resource definition
   * @param rep the resource representation handle (u32 from the component model)
   */
  @SuppressWarnings("unused") // Called from native code via JNI
  static void dispatchDestructorCallback(final long callbackId, final int rep) {
    final ComponentHostFunctionWrapper wrapper = HOST_FUNCTION_CALLBACKS.get(callbackId);
    if (wrapper == null) {
      LOGGER.warning(
          "Destructor callback not found for ID: "
              + callbackId
              + ", rep: "
              + rep
              + ". The callback may have been cleaned up.");
      return;
    }
    try {
      wrapper
          .getImplementation()
          .execute(
              java.util.Collections.singletonList(
                  ai.tegmentum.wasmtime4j.component.ComponentVal.s32(rep)));
    } catch (final Exception e) {
      LOGGER.warning(
          "Destructor callback failed for ID: "
              + callbackId
              + ", rep: "
              + rep
              + ": "
              + e.getMessage());
    }
  }

  /**
   * Dispatches a host function callback from native code.
   *
   * <p>Called from the Rust JNI layer when a component host function is invoked. Looks up the
   * registered callback by ID and invokes it with the provided parameters.
   *
   * @param callbackId the callback ID registered during host function definition
   * @param params the parameters from the component as a JSON-encoded string
   * @return the results as a JSON-encoded string, or null on error
   */
  @SuppressWarnings("unused") // Called from native code via JNI
  static String dispatchHostFunctionCallback(final long callbackId, final String params) {
    final ComponentHostFunctionWrapper wrapper = HOST_FUNCTION_CALLBACKS.get(callbackId);
    if (wrapper == null) {
      LOGGER.severe("Host function callback not found for ID: " + callbackId);
      return null;
    }
    try {
      final java.util.List<ai.tegmentum.wasmtime4j.component.ComponentVal> paramList =
          ai.tegmentum.wasmtime4j.component.ConcurrentCallCodec.deserializeVals(params);
      final java.util.List<ai.tegmentum.wasmtime4j.component.ComponentVal> results =
          wrapper.getImplementation().execute(paramList);
      return ai.tegmentum.wasmtime4j.component.ConcurrentCallCodec.serializeVals(results);
    } catch (final Exception e) {
      LOGGER.severe("Host function callback failed for ID: " + callbackId + ": " + e.getMessage());
      return null;
    }
  }

  /** Wrapper for component host function callbacks. */
  private static class ComponentHostFunctionWrapper {
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

  // Native method declarations

  private native void nativeEnableWasiP2(long linkerHandle);

  private native void nativeDestroyComponentLinker(long linkerHandle);

  private native void nativeDefineHostFunction(long linkerHandle, String witPath, long callbackId);

  private native void nativeDefineHostFunctionAsync(
      long linkerHandle, String witPath, long callbackId);

  private native long nativeDefineResource(
      long linkerHandle,
      String interfaceNamespace,
      String interfaceName,
      String resourceName,
      long constructorCallbackId,
      long destructorCallbackId);

  private native void nativeDefineModule(
      long linkerHandle, String instancePath, String name, long moduleHandle);

  private native long nativeInstantiateWithLinker(
      long linkerHandle, long storeHandle, long componentHandle);

  private native long nativeInstantiatePre(long linkerHandle, long componentHandle);

  private static native void nativeAllowShadowing(long linkerHandle, boolean allow);

  private static native int nativeDefineUnknownImportsAsTraps(
      long linkerHandle, long componentHandle);

  // WASI config native methods
  private static native void nativeSetWasiInheritStdio(long linkerHandle, boolean inherit);

  private static native void nativeSetWasiInheritStdin(long linkerHandle, boolean inherit);

  private static native void nativeSetWasiStdinBytes(long linkerHandle, byte[] data);

  private static native void nativeSetWasiInheritStdout(long linkerHandle, boolean inherit);

  private static native void nativeSetWasiInheritStderr(long linkerHandle, boolean inherit);

  private static native void nativeSetWasiInheritEnv(long linkerHandle, boolean inherit);

  private static native void nativeSetWasiInheritArgs(long linkerHandle, boolean inherit);

  private static native void nativeSetWasiArgs(long linkerHandle, String[] args);

  private static native void nativeAddWasiEnv(long linkerHandle, String key, String value);

  private static native void nativeAddWasiPreopenDir(
      long linkerHandle, String hostPath, String guestPath, int dirPermsBits, int filePermsBits);

  private static native void nativeSetWasiAllowNetwork(long linkerHandle, boolean allow);

  private static native void nativeSetWasiAllowTcp(long linkerHandle, boolean allow);

  private static native void nativeSetWasiAllowUdp(long linkerHandle, boolean allow);

  private static native void nativeSetWasiAllowIpNameLookup(long linkerHandle, boolean allow);

  private static native void nativeSetWasiAllowBlockingCurrentThread(
      long linkerHandle, boolean allow);

  private static native void nativeSetWasiInsecureRandomSeed(long linkerHandle, long seed);

  private static native void nativeSetWasiWallClock(long linkerHandle, Object wallClock);

  private static native void nativeSetWasiMonotonicClock(long linkerHandle, Object monotonicClock);

  private static native void nativeSetWasiSecureRandom(long linkerHandle, Object randomSource);

  private static native void nativeSetWasiInsecureRandom(long linkerHandle, Object randomSource);

  private static native void nativeSetWasiSocketAddrCheck(long linkerHandle, Object addrCheck);
}
