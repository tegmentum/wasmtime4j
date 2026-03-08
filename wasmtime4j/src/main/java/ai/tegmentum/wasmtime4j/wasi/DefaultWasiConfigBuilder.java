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
package ai.tegmentum.wasmtime4j.wasi;

/**
 * Default implementation of the WASI configuration builder.
 *
 * <p>This is the sole concrete implementation of {@link AbstractWasiConfigBuilder}, used by both
 * JNI and Panama runtimes since WASI configuration is a pure data object with no runtime-specific
 * behavior.
 *
 * @since 1.0.0
 */
public final class DefaultWasiConfigBuilder extends AbstractWasiConfigBuilder {

  /** Creates a new WASI configuration builder. */
  public DefaultWasiConfigBuilder() {}

  @Override
  public WasiConfig build() {
    validate();
    return new DefaultWasiConfig(
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
}
