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
package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when the pooling allocator's concurrency limit is exceeded.
 *
 * <p>This exception is thrown when attempting to create more concurrent instances than the pooling
 * allocator allows. The concurrency limit is configured via {@code instancePoolSize} in the engine
 * configuration.
 *
 * @since 1.1.0
 */
public class PoolConcurrencyLimitException extends ResourceException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new pool concurrency limit exception with the specified message.
   *
   * @param message the error message describing the limit exceeded
   */
  public PoolConcurrencyLimitException(final String message) {
    super(message);
  }

  /**
   * Creates a new pool concurrency limit exception with the specified message and cause.
   *
   * @param message the error message describing the limit exceeded
   * @param cause the underlying cause
   */
  public PoolConcurrencyLimitException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
