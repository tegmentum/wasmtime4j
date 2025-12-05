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
 * Consistency models supported by WASI-keyvalue.
 *
 * @since 1.0.0
 */
public enum ConsistencyModel {

  /** Eventual consistency - best performance, eventual convergence. */
  EVENTUAL,

  /** Strong consistency - immediate consistency, slower performance. */
  STRONG,

  /** Causal consistency - maintains causal relationships between operations. */
  CAUSAL,

  /** Sequential consistency - operations appear in some sequential order. */
  SEQUENTIAL,

  /** Linearizable consistency - strongest consistency model. */
  LINEARIZABLE,

  /** Session consistency - consistency within a single session. */
  SESSION,

  /** Monotonic read consistency - reads never go backwards. */
  MONOTONIC_READ,

  /** Monotonic write consistency - writes are applied in order. */
  MONOTONIC_WRITE
}
