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

package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.wasi.AbstractWasiConfig;
import ai.tegmentum.wasmtime4j.wasi.AbstractWasiConfigBuilder;
import ai.tegmentum.wasmtime4j.wasi.WasiVersion;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Panama implementation of WASI configuration.
 *
 * @since 1.0.0
 */
public final class PanamaWasiConfig extends AbstractWasiConfig {

  /**
   * Creates a new WASI configuration.
   *
   * @param environment environment variables
   * @param arguments command line arguments
   * @param preopenDirectories pre-opened directories
   * @param workingDirectory working directory
   * @param inheritEnvironment whether to inherit host environment
   * @param executionTimeout execution timeout
   * @param validationEnabled whether validation is enabled
   * @param strictModeEnabled whether strict mode is enabled
   * @param wasiVersion WASI version
   * @param asyncOperations whether async operations are enabled
   * @param maxAsyncOperations maximum concurrent async operations
   * @param asyncOperationTimeout timeout for async operations
   */
  PanamaWasiConfig(
      final Map<String, String> environment,
      final List<String> arguments,
      final Map<String, Path> preopenDirectories,
      final String workingDirectory,
      final boolean inheritEnvironment,
      final Duration executionTimeout,
      final boolean validationEnabled,
      final boolean strictModeEnabled,
      final WasiVersion wasiVersion,
      final boolean asyncOperations,
      final Integer maxAsyncOperations,
      final Duration asyncOperationTimeout) {
    super(
        environment,
        arguments,
        preopenDirectories,
        workingDirectory,
        inheritEnvironment,
        executionTimeout,
        validationEnabled,
        strictModeEnabled,
        wasiVersion,
        asyncOperations,
        maxAsyncOperations,
        asyncOperationTimeout);
  }

  @Override
  protected AbstractWasiConfigBuilder createBuilder() {
    return new PanamaWasiConfigBuilder();
  }
}
