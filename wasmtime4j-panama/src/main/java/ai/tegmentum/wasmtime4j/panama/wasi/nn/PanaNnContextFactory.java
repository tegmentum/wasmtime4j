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

package ai.tegmentum.wasmtime4j.panama.wasi.nn;

import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import ai.tegmentum.wasmtime4j.wasi.nn.NnContext;
import ai.tegmentum.wasmtime4j.wasi.nn.NnContextFactory;
import ai.tegmentum.wasmtime4j.wasi.nn.NnException;
import ai.tegmentum.wasmtime4j.wasi.nn.NnExecutionTarget;
import java.lang.foreign.MemorySegment;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama implementation of the NnContextFactory interface.
 *
 * <p>This factory creates Panama-based WASI-NN contexts for neural network inference operations.
 *
 * @since 1.0.0
 */
public final class PanaNnContextFactory implements NnContextFactory {

  private static final Logger LOGGER = Logger.getLogger(PanaNnContextFactory.class.getName());

  /** Creates a new Panama WASI-NN context factory. */
  public PanaNnContextFactory() {
    // Default constructor
  }

  @Override
  public NnContext createNnContext() throws NnException {
    if (!isNnAvailable()) {
      throw new NnException("WASI-NN is not available in this build");
    }

    final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
    final MemorySegment contextPtr = bindings.wasiNnContextCreate();
    if (contextPtr == null || contextPtr.equals(MemorySegment.NULL)) {
      throw new NnException("Failed to create WASI-NN context");
    }
    LOGGER.log(Level.FINE, "Created WASI-NN context with handle: {0}", contextPtr);
    return new PanaNnContext(contextPtr);
  }

  @Override
  public boolean isNnAvailable() {
    final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
    return bindings.wasiNnIsAvailable() != 0;
  }

  @Override
  public NnExecutionTarget getDefaultExecutionTarget() {
    final NativeFunctionBindings bindings = NativeFunctionBindings.getInstance();
    final int ordinal = bindings.wasiNnGetDefaultTarget();
    if (ordinal >= 0 && ordinal < NnExecutionTarget.values().length) {
      return NnExecutionTarget.values()[ordinal];
    }
    return NnExecutionTarget.CPU;
  }
}
