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
 * Transaction isolation levels for WASI-keyvalue operations.
 *
 * @since 1.0.0
 */
public enum IsolationLevel {

  /** Read uncommitted - allows dirty reads. */
  READ_UNCOMMITTED,

  /** Read committed - prevents dirty reads. */
  READ_COMMITTED,

  /** Repeatable read - prevents non-repeatable reads. */
  REPEATABLE_READ,

  /** Serializable - strongest isolation, prevents phantom reads. */
  SERIALIZABLE,

  /** Snapshot isolation - consistent view of data at transaction start. */
  SNAPSHOT
}
