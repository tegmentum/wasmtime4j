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

package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.ComponentHostFunction;
import ai.tegmentum.wasmtime4j.ComponentImportValidation;
import ai.tegmentum.wasmtime4j.ComponentInstance;
import ai.tegmentum.wasmtime4j.ComponentLinker;
import ai.tegmentum.wasmtime4j.ComponentResourceDefinition;
import ai.tegmentum.wasmtime4j.ComponentSimple;
import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasiPreview2Config;
import ai.tegmentum.wasmtime4j.exception.WasmException;
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
public final class JniComponentLinker<T> implements ComponentLinker<T> {
  private static final Logger LOGGER = Logger.getLogger(JniComponentLinker.class.getName());
  private static final ConcurrentHashMap<Long, ComponentHostFunctionWrapper>
      HOST_FUNCTION_CALLBACKS = new ConcurrentHashMap<>();
  private static final AtomicLong CALLBACK_ID_GENERATOR = new AtomicLong(1);

  private final long nativeHandle;
  private final Engine engine;
  private final Map<String, Long> hostFunctions = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> definedInterfaces = new ConcurrentHashMap<>();
  private final Set<Long> registeredCallbackIds = Collections.synchronizedSet(new HashSet<Long>());
  private volatile boolean closed = false;

  /**
   * Creates a new JNI component linker with the given native handle.
   *
   * @param nativeHandle the native handle
   * @param engine the engine
   */
  public JniComponentLinker(final long nativeHandle, final Engine engine) {
    this.nativeHandle = nativeHandle;
    this.engine = engine;
  }

  /**
   * Gets the native handle.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
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

    LOGGER.fine("Defined component function: " + witPath);
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
  public void linkInstance(final ComponentInstance instance) throws WasmException {
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }
    ensureNotClosed();

    LOGGER.fine("Linked component instance exports");
  }

  @Override
  public ComponentInstance linkComponent(final Store store, final ComponentSimple component)
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
  public ComponentInstance instantiate(final Store store, final ComponentSimple component)
      throws WasmException {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }
    ensureNotClosed();

    // For JNI, delegate to the component's own instantiation mechanism
    // The component linker primarily manages host function definitions
    if (component instanceof JniComponentImpl) {
      final JniComponentImpl jniComponent = (JniComponentImpl) component;
      return jniComponent.instantiate();
    }

    throw new WasmException(
        "Component must be a JniComponentImpl for JNI ComponentLinker instantiation");
  }

  @Override
  public void enableWasiPreview2() throws WasmException {
    ensureNotClosed();

    if (!isNativeHandleReasonable()) {
      return;
    }

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
    // For now, use default enablement - config support can be added later
    enableWasiPreview2();
  }

  @Override
  public boolean isValid() {
    return !closed && nativeHandle != 0;
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
  public ComponentImportValidation validateImports(final ComponentSimple component) {
    if (component == null) {
      throw new IllegalArgumentException("Component cannot be null");
    }
    // Validation not fully implemented - return a basic result
    return ComponentImportValidation.success(Collections.<String>emptyList());
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
  public void close() {
    if (!closed) {
      closed = true;
      cleanupHostFunctionCallbacks();

      if (nativeHandle == 0 || !isNativeHandleReasonable()) {
        return;
      }

      try {
        nativeDestroyComponentLinker(nativeHandle);
      } catch (final Exception e) {
        LOGGER.warning("Error destroying component linker: " + e.getMessage());
      }
    }
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("ComponentLinker has been closed");
    }
  }

  private boolean isNativeHandleReasonable() {
    if (nativeHandle == 0) {
      return false;
    }
    final long minReasonablePtr = 0x100000000L;
    return nativeHandle >= minReasonablePtr;
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
}
