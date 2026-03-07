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
package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstancePre;
import ai.tegmentum.wasmtime4j.component.ComponentStoreConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of ComponentInstancePre for fast repeated component instantiation.
 *
 * <p>This implementation uses Panama Foreign Function API to interact with the native Wasmtime
 * library for pre-instantiated component operations.
 *
 * @since 1.0.0
 */
public final class PanamaComponentInstancePre implements ComponentInstancePre {

  private static final Logger LOGGER = Logger.getLogger(PanamaComponentInstancePre.class.getName());
  private static final NativeComponentBindings NATIVE_BINDINGS =
      NativeComponentBindings.getInstance();

  private final PanamaEngine engine;
  private final PanamaComponentImpl component;
  private final MemorySegment nativePreHandle;
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama ComponentInstancePre.
   *
   * @param nativePreHandle the native pre-instantiated handle
   * @param engine the engine
   * @param component the component that was pre-instantiated
   */
  public PanamaComponentInstancePre(
      final MemorySegment nativePreHandle,
      final PanamaEngine engine,
      final PanamaComponentImpl component) {
    this.nativePreHandle = nativePreHandle;
    this.engine = engine;
    this.component = component;

    final MemorySegment capturedHandle = this.nativePreHandle;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaComponentInstancePre",
            () -> {
              if (nativePreHandle != null && !nativePreHandle.equals(MemorySegment.NULL)) {
                try {
                  NATIVE_BINDINGS.componentInstancePreDestroy(nativePreHandle);
                } catch (final Throwable t) {
                  throw new Exception("Error closing PanamaComponentInstancePre native handle", t);
                }
              }
            },
            this,
            () -> {
              if (capturedHandle != null && !capturedHandle.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.componentInstancePreDestroy(capturedHandle);
              }
            });

    LOGGER.fine("Created Panama ComponentInstancePre");
  }

  @Override
  public ComponentInstance instantiate() throws WasmException {
    resourceHandle.beginOperation();
    try {

      try (Arena tempArena = Arena.ofConfined()) {
        final MemorySegment instanceOutPtr = tempArena.allocate(ValueLayout.ADDRESS);

        final int errorCode =
            NATIVE_BINDINGS.componentInstancePreInstantiate(nativePreHandle, instanceOutPtr);

        if (errorCode != 0) {
          throw PanamaErrorMapper.mapNativeError(
              errorCode, "Failed to instantiate from ComponentInstancePre");
        }

        final MemorySegment instancePtr = instanceOutPtr.get(ValueLayout.ADDRESS, 0);

        if (instancePtr == null || instancePtr.equals(MemorySegment.NULL)) {
          throw new WasmException(
              "Failed to instantiate from ComponentInstancePre: null instance returned");
        }

        LOGGER.fine("Successfully instantiated component from ComponentInstancePre");

        return new PanamaComponentInstance(instancePtr, component, null, this);
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public ComponentInstance instantiate(final ComponentStoreConfig config) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("config must not be null");
    }
    resourceHandle.beginOperation();
    try {

      try (Arena tempArena = Arena.ofConfined()) {
        final MemorySegment instanceOutPtr = tempArena.allocate(ValueLayout.ADDRESS);

        final int errorCode =
            NATIVE_BINDINGS.componentInstancePreInstantiateWithConfig(
                nativePreHandle,
                config.getFuelLimit(),
                config.getEpochDeadline(),
                config.getMaxMemoryBytes(),
                instanceOutPtr);

        if (errorCode != 0) {
          throw PanamaErrorMapper.mapNativeError(
              errorCode, "Failed to instantiate from ComponentInstancePre with config");
        }

        final MemorySegment instancePtr = instanceOutPtr.get(ValueLayout.ADDRESS, 0);

        if (instancePtr == null || instancePtr.equals(MemorySegment.NULL)) {
          throw new WasmException(
              "Failed to instantiate from ComponentInstancePre with config: null instance"
                  + " returned");
        }

        LOGGER.fine("Successfully instantiated component from ComponentInstancePre with config");

        return new PanamaComponentInstance(instancePtr, component, null, this);
      }
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public ai.tegmentum.wasmtime4j.component.Component getComponent() {
    return component;
  }

  @Override
  public boolean isValid() {
    if (!resourceHandle.tryBeginOperation()) {
      return false;
    }
    try {
      return NATIVE_BINDINGS.componentInstancePreIsValid(nativePreHandle) == 1;
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public long getInstanceCount() {
    if (!resourceHandle.tryBeginOperation()) {
      return 0;
    }
    try {
      return NATIVE_BINDINGS.componentInstancePreInstanceCount(nativePreHandle);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public long getPreparationTimeNs() {
    if (!resourceHandle.tryBeginOperation()) {
      return 0;
    }
    try {
      return NATIVE_BINDINGS.componentInstancePrePreparationTimeNs(nativePreHandle);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public long getAverageInstantiationTimeNs() {
    if (!resourceHandle.tryBeginOperation()) {
      return 0;
    }
    try {
      return NATIVE_BINDINGS.componentInstancePreAvgInstantiationTimeNs(nativePreHandle);
    } finally {
      resourceHandle.endOperation();
    }
  }

  @Override
  public void close() {
    resourceHandle.close();
  }

  /**
   * Gets the native handle for this pre-instantiated component.
   *
   * @return the native handle
   */
  public MemorySegment getNativeHandle() {
    return nativePreHandle;
  }
}
