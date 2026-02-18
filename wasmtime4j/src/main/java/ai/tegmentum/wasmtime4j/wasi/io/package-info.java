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
 * WASI Preview 2 I/O interfaces.
 *
 * <p>This package provides Java bindings for the WASI Preview 2 (WASI 0.2) I/O interfaces,
 * specifically the wasi:io/streams and wasi:io/poll APIs.
 *
 * <h2>Core Interfaces</h2>
 *
 * <ul>
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream} - Non-blocking input stream for
 *       reading bytes
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream} - Non-blocking output stream for
 *       writing bytes
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.io.WasiPollable} - Event notification for I/O readiness
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Reading from a stream
 * try (WasiInputStream input = openInputStream()) {
 *   byte[] data = input.blockingRead(1024);
 *   // Process data...
 * }
 *
 * // Writing to a stream
 * try (WasiOutputStream output = openOutputStream()) {
 *   long capacity = output.checkWrite();
 *   if (capacity >= data.length) {
 *     output.write(data);
 *     output.flush();
 *   }
 * }
 *
 * // Non-blocking I/O with polling
 * List<WasiPollable> pollables = Arrays.asList(
 *   inputStream.subscribe(),
 *   outputStream.subscribe()
 * );
 * // Wait for any pollable to become ready
 * // (poll implementation depends on WASI runtime)
 * }</pre>
 *
 * <h2>Stream Characteristics</h2>
 *
 * <p>WASI Preview 2 streams are non-blocking to the extent practical on the underlying platform:
 *
 * <ul>
 *   <li><b>Non-blocking operations</b> return immediately with available data (possibly zero bytes)
 *   <li><b>Blocking operations</b> wait until data is available or the stream is closed
 *   <li><b>Pollable resources</b> enable efficient waiting on multiple I/O sources
 * </ul>
 *
 * <h2>Component Model Integration</h2>
 *
 * <p>These interfaces are designed for use with WebAssembly components using the Component Model.
 * Streams are resources that can be passed between components and have well-defined ownership
 * semantics.
 *
 * @see <a href="https://github.com/WebAssembly/wasi-io">WASI I/O Specification</a>
 * @see <a href="https://wasi.dev/interfaces">WASI Interfaces Documentation</a>
 * @since 1.0.0
 */
package ai.tegmentum.wasmtime4j.wasi.io;
