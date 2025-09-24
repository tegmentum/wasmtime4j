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

package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when resource management operations fail.
 *
 * <p>This exception indicates issues with WebAssembly resource lifecycle management, including
 * resource creation, access, reference counting, and cleanup failures.
 *
 * @since 1.0.0
 */
public class ResourceException extends WasmException {

  /**
   * Constructs a new resource exception with the specified detail message.
   *
   * @param message the detail message
   */
  public ResourceException(final String message) {
    super(message);
  }

  /**
   * Constructs a new resource exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public ResourceException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new resource exception with the specified cause.
   *
   * @param cause the cause
   */
  public ResourceException(final Throwable cause) {
    super(cause);
  }
}
