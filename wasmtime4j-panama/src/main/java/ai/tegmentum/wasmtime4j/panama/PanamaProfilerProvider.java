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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.profiler.Profiler;
import ai.tegmentum.wasmtime4j.profiler.ProfilerFactory;
import java.util.logging.Logger;

/**
 * ServiceLoader provider for Panama Profiler implementation.
 *
 * <p>This provider is automatically discovered by {@link ProfilerFactory} via the ServiceLoader
 * mechanism.
 *
 * @since 1.0.0
 */
public final class PanamaProfilerProvider implements ProfilerFactory.ProfilerProvider {

  private static final Logger LOGGER = Logger.getLogger(PanamaProfilerProvider.class.getName());

  /** Default constructor for ServiceLoader. */
  public PanamaProfilerProvider() {
    LOGGER.fine("Created PanamaProfilerProvider");
  }

  @Override
  public Profiler create() throws WasmException {
    try {
      // Verify Panama native library is available
      NativeFunctionBindings.getInstance();
      LOGGER.fine("Creating PanamaProfiler");
      return new PanamaProfiler();
    } catch (Exception e) {
      LOGGER.fine("Panama Profiler not available: " + e.getMessage());
      return null;
    }
  }
}
