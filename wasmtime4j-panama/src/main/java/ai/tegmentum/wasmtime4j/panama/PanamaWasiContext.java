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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasiContext;
import ai.tegmentum.wasmtime4j.WasiDirectoryPermissions;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Panama FFI stub implementation of WasiContext.
 *
 * <p>WASI context functionality is not yet implemented for Panama. All methods throw
 * UnsupportedOperationException.
 *
 * @since 1.0.0
 */
public final class PanamaWasiContext implements WasiContext {

  /** Creates a new Panama WASI context stub. */
  public PanamaWasiContext() {
    // Stub constructor
  }

  @Override
  public WasiContext setArgv(final String[] argv) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setEnv(final String key, final String value) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setEnv(final Map<String, String> env) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext inheritEnv() {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext inheritStdio() {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setStdin(final Path path) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setStdout(final Path path) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setStderr(final Path path) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext preopenedDir(final Path hostPath, final String guestPath)
      throws WasmException {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext preopenedDirReadOnly(final Path hostPath, final String guestPath)
      throws WasmException {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setWorkingDirectory(final String workingDir) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setNetworkEnabled(final boolean enabled) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setMaxOpenFiles(final int maxFds) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setAsyncIoEnabled(final boolean enabled) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setMaxAsyncOperations(final int maxOps) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setAsyncTimeout(final long timeoutMs) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setComponentModelEnabled(final boolean enabled) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setProcessEnabled(final boolean enabled) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext setFilesystemWorkingDir(final Path workingDir) {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }

  @Override
  public WasiContext preopenedDirWithPermissions(
      final Path hostPath, final String guestPath, final WasiDirectoryPermissions permissions)
      throws WasmException {
    throw new UnsupportedOperationException("WASI context not yet implemented for Panama");
  }
}
