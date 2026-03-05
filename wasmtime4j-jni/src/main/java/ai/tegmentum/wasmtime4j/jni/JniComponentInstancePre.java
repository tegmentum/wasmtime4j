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
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.component.ComponentInstancePre;
import ai.tegmentum.wasmtime4j.component.ComponentStoreConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.logging.Logger;

/**
 * JNI implementation of the ComponentInstancePre interface.
 *
 * <p>Provides pre-instantiated component functionality through JNI bindings to the native Wasmtime
 * library. Pre-instantiation performs expensive type-checking and import resolution once, allowing
 * fast repeated instantiation.
 *
 * @since 1.0.0
 */
public final class JniComponentInstancePre implements ComponentInstancePre {
  private static final Logger LOGGER = Logger.getLogger(JniComponentInstancePre.class.getName());

  private final long nativeHandle;
  private final Engine engine;
  private final JniComponentImpl component;
  private volatile boolean closed = false;

  /**
   * Creates a new JNI ComponentInstancePre with the given native handle.
   *
   * @param nativeHandle the native handle
   * @param engine the engine
   * @param component the component that was pre-instantiated
   */
  public JniComponentInstancePre(
      final long nativeHandle, final Engine engine, final JniComponentImpl component) {
    this.nativeHandle = nativeHandle;
    this.engine = engine;
    this.component = component;
  }

  @Override
  public ComponentInstance instantiate() throws WasmException {
    ensureNotClosed();

    final long engineHandle = component.getEngine().getNativeHandle();
    final long instanceId = nativeInstantiate(nativeHandle, engineHandle);
    if (instanceId == 0) {
      throw new WasmException("Failed to instantiate from ComponentInstancePre");
    }

    final JniComponent.JniComponentInstanceHandle instanceWrapper =
        new JniComponent.JniComponentInstanceHandle(engineHandle, instanceId);
    return new JniComponentInstanceImpl(instanceWrapper, component, new ComponentInstanceConfig());
  }

  @Override
  public ComponentInstance instantiate(final ComponentStoreConfig config) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("config must not be null");
    }
    ensureNotClosed();

    final long engineHandle = component.getEngine().getNativeHandle();
    final long instanceId =
        nativeInstantiateWithConfig(
            nativeHandle,
            engineHandle,
            config.getFuelLimit(),
            config.getEpochDeadline(),
            config.getMaxMemoryBytes());
    if (instanceId == 0) {
      throw new WasmException("Failed to instantiate from ComponentInstancePre with config");
    }

    final JniComponent.JniComponentInstanceHandle instanceWrapper =
        new JniComponent.JniComponentInstanceHandle(engineHandle, instanceId);
    return new JniComponentInstanceImpl(instanceWrapper, component, new ComponentInstanceConfig());
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
    if (closed || nativeHandle == 0) {
      return false;
    }
    return nativeIsValid(nativeHandle) != 0;
  }

  @Override
  public long getInstanceCount() {
    if (closed || nativeHandle == 0) {
      return 0;
    }
    return nativeInstanceCount(nativeHandle);
  }

  @Override
  public long getPreparationTimeNs() {
    if (closed || nativeHandle == 0) {
      return 0;
    }
    return nativePreparationTimeNs(nativeHandle);
  }

  @Override
  public long getAverageInstantiationTimeNs() {
    if (closed || nativeHandle == 0) {
      return 0;
    }
    return nativeAvgInstantiationTimeNs(nativeHandle);
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;

      if (nativeHandle == 0) {
        return;
      }

      try {
        nativeDestroy(nativeHandle);
      } catch (final Exception e) {
        LOGGER.warning("Error destroying ComponentInstancePre: " + e.getMessage());
      }
    }
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("ComponentInstancePre has been closed");
    }
  }

  // Native method declarations

  private static native long nativeInstantiate(long preHandle, long engineHandle);

  private static native long nativeInstantiateWithConfig(
      long preHandle, long engineHandle, long fuelLimit, long epochDeadline, long maxMemoryBytes);

  private static native byte nativeIsValid(long preHandle);

  private static native long nativeInstanceCount(long preHandle);

  private static native long nativePreparationTimeNs(long preHandle);

  private static native long nativeAvgInstantiationTimeNs(long preHandle);

  private static native void nativeDestroy(long preHandle);
}
