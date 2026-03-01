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
package ai.tegmentum.wasmtime4j.wasi.cli;

import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;

/**
 * WASI Preview 2 standard I/O interface.
 *
 * <p>Provides access to stdin, stdout, and stderr streams for CLI programs. This interface
 * corresponds to the wasi:cli/stdin, wasi:cli/stdout, and wasi:cli/stderr from the WASI Preview 2
 * specification.
 *
 * <p>All streams use the wasi:io stream interfaces, providing consistent non-blocking I/O
 * operations across the platform.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiStdio stdio = getStdio();
 *
 * // Read from stdin
 * WasiInputStream stdin = stdio.getStdin();
 * byte[] input = stdin.blockingRead(1024);
 *
 * // Write to stdout
 * WasiOutputStream stdout = stdio.getStdout();
 * stdout.blockingWriteAndFlush("Hello, WASI!".getBytes());
 *
 * // Write errors to stderr
 * WasiOutputStream stderr = stdio.getStderr();
 * stderr.blockingWriteAndFlush("Error occurred\n".getBytes());
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiStdio {

  /**
   * Gets the standard input stream.
   *
   * <p>Provides an input stream for reading program input. This stream supports both blocking and
   * non-blocking read operations.
   *
   * @return the stdin input stream
   */
  WasiInputStream getStdin();

  /**
   * Gets the standard output stream.
   *
   * <p>Provides an output stream for normal program output. This stream supports both blocking and
   * non-blocking write operations.
   *
   * @return the stdout output stream
   */
  WasiOutputStream getStdout();

  /**
   * Gets the standard error stream.
   *
   * <p>Provides an output stream for error messages and diagnostics. This stream supports both
   * blocking and non-blocking write operations.
   *
   * @return the stderr output stream
   */
  WasiOutputStream getStderr();
}
