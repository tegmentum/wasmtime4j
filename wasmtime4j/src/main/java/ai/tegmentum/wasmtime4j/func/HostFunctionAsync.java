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
package ai.tegmentum.wasmtime4j.func;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an asynchronous host function that can be called from WebAssembly.
 *
 * <p>Async host functions return a {@link CompletableFuture} instead of blocking. This enables
 * cooperative scheduling with Wasmtime's async executor, allowing other WebAssembly tasks to make
 * progress while the host function awaits an external result (database query, HTTP request, etc.).
 *
 * <p>Unlike {@link HostFunction}, async host functions always receive a {@link Caller} context,
 * which provides access to the calling instance's exports, fuel, and store data.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * HostFunctionAsync asyncFetch = (caller, params) -> {
 *     String url = params[0].asString();
 *     return httpClient.fetchAsync(url)
 *         .thenApply(body -> new WasmValue[] { WasmValue.i32(body.length()) });
 * };
 *
 * linker.defineHostFunctionAsync("env", "fetch", functionType, asyncFetch);
 * }</pre>
 *
 * @since 1.1.0
 */
@FunctionalInterface
public interface HostFunctionAsync {

  /**
   * Executes the async host function with the given caller context and parameters.
   *
   * <p>The returned future must eventually complete (normally or exceptionally). Completing
   * exceptionally with a {@link ai.tegmentum.wasmtime4j.exception.WasmException} will cause a
   * WebAssembly trap. Any other exception type will be wrapped in a WasmException.
   *
   * @param caller the calling instance context, providing access to exports, fuel, and store data
   * @param params the parameters passed from WebAssembly, never null
   * @return a future that completes with the result values to return to WebAssembly
   */
  CompletableFuture<WasmValue[]> execute(Caller<?> caller, WasmValue[] params);
}
