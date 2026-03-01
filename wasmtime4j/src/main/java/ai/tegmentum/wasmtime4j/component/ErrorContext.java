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
package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;

/**
 * Represents a Component Model error context for async error propagation.
 *
 * <p>Error contexts carry structured error information across component boundaries in the Component
 * Model async proposal. They allow components to propagate error details when streams or futures
 * fail, providing richer diagnostics than simple error codes.
 *
 * @since 1.1.0
 */
public interface ErrorContext extends AutoCloseable {

  /**
   * Gets the opaque native handle for this error context.
   *
   * @return the native error context handle
   */
  long getHandle();

  /**
   * Gets the debug message associated with this error context, if any.
   *
   * <p>The debug message is a human-readable description of the error intended for logging and
   * diagnostics. It may not be present for all error contexts.
   *
   * @return the debug message, or empty if not available
   * @throws WasmException if retrieval fails
   */
  Optional<String> debugMessage() throws WasmException;

  /**
   * Checks if this error context is still valid.
   *
   * @return true if the error context can still be used
   */
  boolean isValid();

  /** Closes this error context, releasing associated resources. */
  @Override
  void close();
}
