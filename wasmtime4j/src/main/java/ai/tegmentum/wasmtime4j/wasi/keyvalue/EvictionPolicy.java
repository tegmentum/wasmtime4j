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

package ai.tegmentum.wasmtime4j.wasi.keyvalue;

/**
 * Cache eviction policies for key-value stores.
 *
 * @since 1.0.0
 */
public enum EvictionPolicy {

  /** Least Recently Used - evict entries not accessed recently. */
  LRU,

  /** Least Frequently Used - evict entries accessed least often. */
  LFU,

  /** First In, First Out - evict oldest entries first. */
  FIFO,

  /** Time-based expiration using TTL. */
  TTL,

  /** Size-based eviction - evict largest entries first. */
  SIZE_BASED,

  /** Random eviction. */
  RANDOM,

  /** No eviction - fail when full. */
  NONE
}
