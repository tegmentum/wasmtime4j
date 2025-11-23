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
 * WASI Preview 2 filesystem interfaces.
 *
 * <p>This package provides Java bindings for the WASI Preview 2 (WASI 0.2) filesystem API,
 * specifically the wasi:filesystem/types interface.
 *
 * <h2>Core Interfaces</h2>
 *
 * <ul>
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.filesystem.WasiDescriptor} - Reference to filesystem
 *       objects (files, directories, etc.)
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.filesystem.DescriptorType} - Type of filesystem object
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.filesystem.DescriptorFlags} - Access permission flags
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.filesystem.PathFlags} - Path resolution flags
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.filesystem.OpenFlags} - File creation flags
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Get preopened directory
 * WasiDescriptor rootDir = getPreopenedDirectory("/");
 *
 * // Create a file
 * WasiDescriptor file = rootDir.openAt(
 *   "data.txt",
 *   PathFlags.none(),
 *   OpenFlags.of(OpenFlags.CREATE, OpenFlags.TRUNCATE),
 *   DescriptorFlags.of(DescriptorFlags.READ, DescriptorFlags.WRITE)
 * );
 *
 * // Write to file via stream
 * try (WasiOutputStream out = file.writeViaStream(0)) {
 *   out.blockingWriteAndFlush("Hello, WASI!".getBytes());
 * }
 *
 * // Read directory
 * List<String> entries = rootDir.readDirectory();
 * for (String entry : entries) {
 *   System.out.println(entry);
 * }
 *
 * // Create directory
 * rootDir.createDirectoryAt("subdir");
 *
 * // Rename file
 * rootDir.renameAt("data.txt", rootDir, "newname.txt");
 * }</pre>
 *
 * <h2>Path Handling</h2>
 *
 * <p>All paths in WASI are relative paths interpreted relative to a descriptor referring to a base
 * directory:
 *
 * <ul>
 *   <li>Paths cannot be absolute (no leading /)
 *   <li>Paths are resolved relative to a descriptor
 *   <li>Symbolic links can be followed with {@link
 *       ai.tegmentum.wasmtime4j.wasi.filesystem.PathFlags#SYMLINK_FOLLOW}
 *   <li>Access is controlled by capability-based security
 * </ul>
 *
 * <h2>Descriptor Characteristics</h2>
 *
 * <p>Descriptors in WASI Preview 2:
 *
 * <ul>
 *   <li>Use randomized identifiers (not lowest-numbered like POSIX)
 *   <li>Are resources with ownership semantics
 *   <li>Can refer to files, directories, sockets, or other filesystem objects
 *   <li>Support stream-based I/O via {@link ai.tegmentum.wasmtime4j.wasi.io} interfaces
 *   <li>Must be explicitly closed to release system resources
 * </ul>
 *
 * @see <a href="https://github.com/WebAssembly/wasi-filesystem">WASI Filesystem Specification</a>
 * @see <a href="https://wa.dev/wasi:filesystem">WASI Filesystem Documentation</a>
 * @since 1.0.0
 */
package ai.tegmentum.wasmtime4j.wasi.filesystem;
