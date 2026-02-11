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

package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.memory.Memory;

/**
 * Defines the strategy for allocating WebAssembly instances.
 *
 * <p>The allocation strategy affects performance and memory usage characteristics of WebAssembly
 * execution. Different strategies are optimized for different use cases.
 *
 * @since 1.0.0
 */
public enum InstanceAllocationStrategy {

  /**
   * On-demand allocation strategy.
   *
   * <p>This is the default strategy where each instance is allocated and deallocated individually.
   * Resources are created as needed and released when no longer in use.
   *
   * <p>Best suited for:
   *
   * <ul>
   *   <li>Low-volume instance creation
   *   <li>Long-lived instances
   *   <li>Simple deployment scenarios
   *   <li>Memory-constrained environments
   * </ul>
   */
  ON_DEMAND,

  /**
   * Pooling allocation strategy.
   *
   * <p>Pre-allocates a pool of instance slots and reuses them for faster instantiation. This
   * strategy reduces allocation overhead by maintaining a pool of pre-allocated resources.
   *
   * <p>Best suited for:
   *
   * <ul>
   *   <li>High-volume, short-lived instances
   *   <li>Serverless/function-as-a-service workloads
   *   <li>Low-latency requirements
   *   <li>Scenarios with predictable instance counts
   * </ul>
   *
   * <p>When using this strategy, configure pool settings via {@link
   * ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig}.
   */
  POOLING
}
