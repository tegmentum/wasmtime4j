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
package ai.tegmentum.wasmtime4j.spi;

import ai.tegmentum.wasmtime4j.func.Caller;

/**
 * Service provider interface for accessing caller context in host functions.
 *
 * <p>This SPI allows runtime implementations (JNI, Panama) to provide caller context to
 * CallerAwareHostFunction instances. The runtime sets the caller context in a thread-local before
 * invoking host functions, and CallerAwareHostFunction retrieves it through this provider.
 *
 * <p>Implementations should be registered using the Java ServiceLoader mechanism by creating a file
 * {@code META-INF/services/ai.tegmentum.wasmtime4j.spi.CallerContextProvider} containing the fully
 * qualified class name of the implementation.
 *
 * @since 1.0.0
 */
public interface CallerContextProvider {

  /**
   * Gets the current caller context for the executing thread.
   *
   * <p>This method is called by CallerAwareHostFunction to retrieve the caller context during host
   * function execution. The runtime implementation must have previously set the context using a
   * thread-local mechanism before invoking the host function.
   *
   * @param <T> the type of user data associated with the store
   * @return the current caller context
   * @throws UnsupportedOperationException if no caller context is available
   */
  <T> Caller<T> getCurrentCaller();
}
