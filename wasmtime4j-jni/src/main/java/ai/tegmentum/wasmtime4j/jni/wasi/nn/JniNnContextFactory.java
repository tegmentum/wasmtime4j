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
package ai.tegmentum.wasmtime4j.jni.wasi.nn;

import ai.tegmentum.wasmtime4j.wasi.nn.NnContext;
import ai.tegmentum.wasmtime4j.wasi.nn.NnContextFactory;
import ai.tegmentum.wasmtime4j.wasi.nn.NnException;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of the NnContextFactory interface.
 *
 * <p>This factory creates JNI-based WASI-NN contexts for neural network inference operations.
 *
 * @since 1.0.0
 */
public final class JniNnContextFactory implements NnContextFactory {

  private static final Logger LOGGER = Logger.getLogger(JniNnContextFactory.class.getName());

  /** Creates a new JNI WASI-NN context factory. */
  public JniNnContextFactory() {
    // Default constructor
  }

  @Override
  public NnContext createNnContext() throws NnException {
    if (!isNnAvailable()) {
      throw new NnException("WASI-NN is not available in this build");
    }

    final long handle = nativeCreateContext();
    if (handle == 0) {
      throw new NnException("Failed to create WASI-NN context");
    }
    LOGGER.log(Level.FINE, "Created WASI-NN context with handle: {0}", handle);
    return new JniNnContext(handle);
  }

  @Override
  public boolean isNnAvailable() {
    return nativeIsNnAvailable() != 0;
  }

  @Override
  public NnExecutionTarget getDefaultExecutionTarget() {
    final int ordinal = nativeGetDefaultExecutionTarget();
    if (ordinal >= 0 && ordinal < NnExecutionTarget.values().length) {
      return NnExecutionTarget.values()[ordinal];
    }
    return NnExecutionTarget.CPU;
  }

  // ===== Native method declarations =====

  private static native long nativeCreateContext();

  private static native int nativeIsNnAvailable();

  private static native int nativeGetDefaultExecutionTarget();
}
