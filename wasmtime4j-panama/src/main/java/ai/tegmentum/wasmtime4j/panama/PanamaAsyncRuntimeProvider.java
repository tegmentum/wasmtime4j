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

import ai.tegmentum.wasmtime4j.async.AsyncRuntime;
import ai.tegmentum.wasmtime4j.async.AsyncRuntimeFactory;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.logging.Logger;

/**
 * ServiceLoader provider for Panama AsyncRuntime implementation.
 *
 * <p>This provider is automatically discovered by {@link AsyncRuntimeFactory} via the ServiceLoader
 * mechanism.
 *
 * @since 1.0.0
 */
public final class PanamaAsyncRuntimeProvider implements AsyncRuntimeFactory.AsyncRuntimeProvider {

  private static final Logger LOGGER = Logger.getLogger(PanamaAsyncRuntimeProvider.class.getName());

  /** Default constructor for ServiceLoader. */
  public PanamaAsyncRuntimeProvider() {
    LOGGER.fine("Created PanamaAsyncRuntimeProvider");
  }

  @Override
  public AsyncRuntime create() throws WasmException {
    try {
      // Verify Panama native library is available
      NativeLibraryLoader.getInstance();
      LOGGER.fine("Creating PanamaAsyncRuntime");
      return new PanamaAsyncRuntime();
    } catch (Exception e) {
      LOGGER.fine("Panama AsyncRuntime not available: " + e.getMessage());
      return null;
    }
  }
}
