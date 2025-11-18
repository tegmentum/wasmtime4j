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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.PanamaEngine;
import ai.tegmentum.wasmtime4j.panama.PanamaLinker;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiLinker;
import ai.tegmentum.wasmtime4j.wasi.WasiPermissions;
import ai.tegmentum.wasmtime4j.wasi.WasiStdioConfig;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Panama implementation of a WASI-enabled linker.
 *
 * <p>This linker wraps a standard Panama linker and adds WASI functionality through Panama FFI
 * bindings.
 *
 * @since 1.0.0
 */
public final class PanamaWasiLinker implements WasiLinker {

  private final PanamaLinker<Object> linker;
  private final PanamaEngine engine;
  private final WasiConfig config;
  private boolean closed;

  /**
   * Creates a new WASI linker with the specified configuration.
   *
   * @param linker the Panama linker instance
   * @param engine the engine
   * @param config WASI configuration
   */
  public PanamaWasiLinker(
      final PanamaLinker<Object> linker, final PanamaEngine engine, final WasiConfig config) {
    this.linker = linker;
    this.engine = engine;
    this.config = config;
    this.closed = false;
  }

  @Override
  public void allowDirectoryAccess(
      final Path hostPath, final String guestPath, final WasiPermissions permissions)
      throws WasmException {
    ensureNotClosed();
    if (hostPath == null) {
      throw new IllegalArgumentException("Host path cannot be null");
    }
    if (guestPath == null) {
      throw new IllegalArgumentException("Guest path cannot be null");
    }
    if (permissions == null) {
      throw new IllegalArgumentException("Permissions cannot be null");
    }
    // WASI directory access is configured through WasiConfig
    throw new UnsupportedOperationException(
        "Directory access must be configured through WasiConfig.builder()");
  }

  @Override
  public void allowDirectoryAccess(final Path hostPath, final String guestPath)
      throws WasmException {
    // 0644 = rw-r--r-- (read-write for owner, read for group and others)
    allowDirectoryAccess(hostPath, guestPath, WasiPermissions.of(0644));
  }

  @Override
  public void setEnvironmentVariable(final String name, final String value) {
    ensureNotClosed();
    if (name == null) {
      throw new IllegalArgumentException("Environment variable name cannot be null");
    }
    // Environment variables must be set through WasiConfig
    throw new UnsupportedOperationException(
        "Environment variables must be configured through WasiConfig.builder()");
  }

  @Override
  public void setEnvironmentVariables(final Map<String, String> environment) {
    ensureNotClosed();
    if (environment == null) {
      throw new IllegalArgumentException("Environment map cannot be null");
    }
    throw new UnsupportedOperationException(
        "Environment variables must be configured through WasiConfig.builder()");
  }

  @Override
  public void inheritEnvironment() throws WasmException {
    ensureNotClosed();
    throw new UnsupportedOperationException(
        "Environment inheritance must be configured through WasiConfig.builder()");
  }

  @Override
  public void inheritEnvironmentVariables(final List<String> variableNames) throws WasmException {
    ensureNotClosed();
    if (variableNames == null) {
      throw new IllegalArgumentException("Variable names list cannot be null");
    }
    throw new UnsupportedOperationException(
        "Environment inheritance must be configured through WasiConfig.builder()");
  }

  @Override
  public void setArguments(final List<String> arguments) {
    ensureNotClosed();
    if (arguments == null) {
      throw new IllegalArgumentException("Arguments list cannot be null");
    }
    throw new UnsupportedOperationException(
        "Arguments must be configured through WasiConfig.builder()");
  }

  @Override
  public void configureStdin(final WasiStdioConfig config) throws WasmException {
    ensureNotClosed();
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }
    throw new UnsupportedOperationException("Stdio configuration not yet implemented");
  }

  @Override
  public void configureStdout(final WasiStdioConfig config) throws WasmException {
    ensureNotClosed();
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }
    throw new UnsupportedOperationException("Stdio configuration not yet implemented");
  }

  @Override
  public void configureStderr(final WasiStdioConfig config) throws WasmException {
    ensureNotClosed();
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }
    throw new UnsupportedOperationException("Stdio configuration not yet implemented");
  }

  @Override
  public void enableNetworkAccess() throws WasmException {
    ensureNotClosed();
    throw new UnsupportedOperationException("Network access configuration not yet implemented");
  }

  @Override
  public void disableNetworkAccess() {
    ensureNotClosed();
    // Network access is disabled by default
  }

  @Override
  public void setMaxFileSize(final Long maxSizeBytes) {
    ensureNotClosed();
    throw new UnsupportedOperationException("File size limits not yet implemented");
  }

  @Override
  public void setMaxOpenFiles(final Integer maxOpenFiles) {
    ensureNotClosed();
    throw new UnsupportedOperationException("Open file limits not yet implemented");
  }

  @Override
  public Instance instantiate(final Store store, final Module module) throws WasmException {
    ensureNotClosed();
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (module == null) {
      throw new IllegalArgumentException("Module cannot be null");
    }
    // Delegate to underlying linker
    return linker.instantiate(store, module);
  }

  @Override
  public Linker<?> getLinker() {
    ensureNotClosed();
    return linker;
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public WasiConfig getConfig() {
    return config;
  }

  @Override
  public boolean isValid() {
    return !closed && linker.isValid();
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      linker.close();
    }
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("WASI linker has been closed");
    }
  }
}
