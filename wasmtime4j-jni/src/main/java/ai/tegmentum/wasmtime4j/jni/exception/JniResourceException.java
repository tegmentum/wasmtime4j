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
package ai.tegmentum.wasmtime4j.jni.exception;

/**
 * Exception thrown when there are issues with native resource management.
 *
 * <p>This exception is thrown when:
 *
 * <ul>
 *   <li>Native resource allocation fails
 *   <li>Native resource deallocation fails
 *   <li>Native resource is accessed after being freed
 *   <li>Native resource limits are exceeded
 *   <li>Memory management operations fail
 * </ul>
 *
 * <p>This is an unchecked exception because resource management errors typically indicate
 * programming errors (like using a closed resource) rather than recoverable error conditions.
 *
 * @since 1.0.0
 */
public final class JniResourceException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new JNI resource exception with the specified message.
   *
   * @param message the error message
   */
  public JniResourceException(final String message) {
    super(message);
  }

  /**
   * Creates a new JNI resource exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public JniResourceException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
