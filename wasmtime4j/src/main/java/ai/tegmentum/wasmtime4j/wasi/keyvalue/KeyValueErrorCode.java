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
 * Error codes for WASI-keyvalue operations.
 *
 * @since 1.0.0
 */
public enum KeyValueErrorCode {

  /** Unknown or unspecified error. */
  UNKNOWN,

  /** Key not found. */
  KEY_NOT_FOUND,

  /** Key already exists (for create operations). */
  KEY_EXISTS,

  /** Invalid key format. */
  INVALID_KEY,

  /** Invalid value format. */
  INVALID_VALUE,

  /** Storage capacity exceeded. */
  CAPACITY_EXCEEDED,

  /** Operation not permitted. */
  NOT_PERMITTED,

  /** Connection to storage backend failed. */
  CONNECTION_FAILED,

  /** Storage backend is read-only. */
  READ_ONLY,

  /** Operation timed out. */
  TIMEOUT,

  /** Internal error. */
  INTERNAL_ERROR
}
