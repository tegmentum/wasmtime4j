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
 * WASI Preview 2 command-line interface (CLI) interfaces.
 *
 * <p>This package provides Java bindings for the WASI Preview 2 (WASI 0.2) CLI APIs, specifically
 * the wasi:cli package including environment, stdin/stdout/stderr, and exit interfaces.
 *
 * <h2>Core Interfaces</h2>
 *
 * <ul>
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.cli.WasiEnvironment} - Environment variables and
 *       command-line arguments
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.cli.WasiStdio} - Standard I/O streams (stdin, stdout,
 *       stderr)
 *   <li>{@link ai.tegmentum.wasmtime4j.wasi.cli.WasiExit} - Program termination with exit codes
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Environment and arguments
 * WasiEnvironment env = getEnvironment();
 * List<String> args = env.getArguments();
 * Map<String, String> vars = env.getEnvironmentVariables();
 * String path = vars.get("PATH");
 *
 * // Standard I/O
 * WasiStdio stdio = getStdio();
 *
 * // Read from stdin
 * WasiInputStream stdin = stdio.getStdin();
 * byte[] input = stdin.blockingRead(1024);
 *
 * // Write to stdout
 * WasiOutputStream stdout = stdio.getStdout();
 * stdout.blockingWriteAndFlush("Hello, WASI!\n".getBytes());
 *
 * // Write errors to stderr
 * WasiOutputStream stderr = stdio.getStderr();
 * stderr.blockingWriteAndFlush("Error message\n".getBytes());
 *
 * // Exit program
 * WasiExit exit = getExit();
 * exit.exit(WasiExit.EXIT_SUCCESS);
 * }</pre>
 *
 * <h2>Design Philosophy</h2>
 *
 * <p>The wasi:cli interfaces maintain traditional CLI conventions:
 *
 * <ul>
 *   <li><b>List-of-strings arguments</b> - Standard command-line argument format
 *   <li><b>POSIX-style environment</b> - Name-value pairs for environment variables
 *   <li><b>Stream-based I/O</b> - Uses wasi:io streams for stdin/stdout/stderr
 *   <li><b>Standard exit codes</b> - 0 for success, non-zero for failure
 * </ul>
 *
 * <h2>Integration with I/O Streams</h2>
 *
 * <p>Standard I/O streams are provided as {@link ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream}
 * and {@link ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream} instances, enabling:
 *
 * <ul>
 *   <li>Non-blocking I/O operations
 *   <li>Pollable resource support for event-driven I/O
 *   <li>Consistent stream interface across the platform
 * </ul>
 *
 * @see <a href="https://github.com/WebAssembly/wasi-cli">WASI CLI Specification</a>
 * @see <a href="https://wa.dev/wasi:cli">WASI CLI Documentation</a>
 * @since 1.0.0
 */
package ai.tegmentum.wasmtime4j.wasi.cli;
