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

/**
 * JNI implementation of WASI-Threads support for WebAssembly thread spawning.
 *
 * <p>This package provides the JNI-based implementation of WASI-Threads support, enabling
 * WebAssembly modules to spawn threads. The implementation uses the wasmtime-wasi-threads crate for
 * native thread management.
 *
 * <p>Key classes:
 *
 * <ul>
 *   <li>{@link ai.tegmentum.wasmtime4j.jni.wasi.threads.JniWasiThreadsContext} - The main context
 *       implementation for thread spawning
 *   <li>{@link ai.tegmentum.wasmtime4j.jni.wasi.threads.JniWasiThreadsContextBuilder} - Builder for
 *       creating context instances
 *   <li>{@link ai.tegmentum.wasmtime4j.jni.wasi.threads.JniWasiThreadsProvider} - ServiceLoader
 *       provider for discovery
 * </ul>
 *
 * <p><strong>Important limitations:</strong>
 *
 * <ul>
 *   <li>A trap or WASI exit in one thread will exit the entire process
 *   <li>Not suitable for multi-tenant embeddings
 *   <li>Requires WASI Preview 1 (not compatible with WASI 0.2)
 * </ul>
 *
 * @since 1.0.0
 * @see ai.tegmentum.wasmtime4j.wasi.threads
 */
package ai.tegmentum.wasmtime4j.jni.wasi.threads;
