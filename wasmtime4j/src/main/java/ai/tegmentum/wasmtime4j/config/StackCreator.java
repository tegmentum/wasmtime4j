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
package ai.tegmentum.wasmtime4j.config;

/**
 * Factory interface for creating custom fiber stack allocations for async WebAssembly execution.
 *
 * <p>When set on an {@link EngineConfig} via {@link EngineConfig#withHostStack(StackCreator)},
 * Wasmtime will call this creator instead of using its default stack allocation strategy whenever a
 * new fiber stack is needed for async execution.
 *
 * <p>This is an advanced feature for users who need control over fiber stack allocation. It
 * requires async support to be enabled in the engine configuration.
 *
 * <p>Implementations must be thread-safe since Wasmtime may call {@link #newStack} from multiple
 * threads concurrently.
 *
 * @since 1.1.0
 */
public interface StackCreator {

  /**
   * Creates a new fiber stack.
   *
   * @param size the requested stack size in bytes
   * @param zeroed whether the stack memory must be zero-filled. If true, the entire stack region
   *     (excluding guard pages) must be zeroed before use.
   * @return a new stack memory allocation
   */
  StackMemory newStack(long size, boolean zeroed);
}
