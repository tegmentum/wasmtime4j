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

/**
 * WASI key-value store API interfaces for wasi:keyvalue.
 *
 * <p>This package provides key-value storage operations including:
 *
 * <ul>
 *   <li>Basic CRUD operations (get, set, delete, exists)
 *   <li>Atomic operations (compare-and-swap, increment)
 *   <li>Batch operations
 *   <li>TTL (time-to-live) support
 *   <li>Multiple consistency models
 *   <li>Transaction support
 * </ul>
 *
 * <p>WASI-keyvalue specification: wasi:keyvalue@0.2.0
 *
 * @since 1.0.0
 */
package ai.tegmentum.wasmtime4j.wasi.keyvalue;
