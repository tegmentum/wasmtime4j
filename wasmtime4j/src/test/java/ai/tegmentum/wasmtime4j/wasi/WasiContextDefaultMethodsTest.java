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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link WasiContext} default methods (getEnvironment, getArguments). */
@DisplayName("WasiContext Default Methods Tests")
class WasiContextDefaultMethodsTest {

  /** Minimal stub implementing WasiContext with only required abstract methods. */
  private static final WasiContext STUB =
      new WasiContext() {
        @Override
        public WasiContext setArgv(String[] argv) {
          return this;
        }

        @Override
        public WasiContext setEnv(String key, String value) {
          return this;
        }

        @Override
        public WasiContext setEnv(Map<String, String> env) {
          return this;
        }

        @Override
        public WasiContext inheritEnv() {
          return this;
        }

        @Override
        public WasiContext inheritArgs() {
          return this;
        }

        @Override
        public WasiContext inheritStdio() {
          return this;
        }

        @Override
        public WasiContext setStdin(Path path) {
          return this;
        }

        @Override
        public WasiContext setStdinBytes(byte[] data) {
          return this;
        }

        @Override
        public WasiContext setStdout(Path path) {
          return this;
        }

        @Override
        public WasiContext setStderr(Path path) {
          return this;
        }

        @Override
        public WasiContext setStdoutAppend(Path path) {
          return this;
        }

        @Override
        public WasiContext setStderrAppend(Path path) {
          return this;
        }

        @Override
        public WasiContext preopenedDir(Path hostPath, String guestPath) throws WasmException {
          return this;
        }

        @Override
        public WasiContext preopenedDirReadOnly(Path hostPath, String guestPath)
            throws WasmException {
          return this;
        }

        @Override
        public WasiContext setWorkingDirectory(String workingDir) {
          return this;
        }

        @Override
        public WasiContext setNetworkEnabled(boolean enabled) {
          return this;
        }

        @Override
        public WasiContext setMaxOpenFiles(int maxFds) {
          return this;
        }

        @Override
        public WasiContext setAsyncIoEnabled(boolean enabled) {
          return this;
        }

        @Override
        public WasiContext setMaxAsyncOperations(int maxOps) {
          return this;
        }

        @Override
        public WasiContext setAsyncTimeout(long timeoutMs) {
          return this;
        }

        @Override
        public WasiContext setComponentModelEnabled(boolean enabled) {
          return this;
        }

        @Override
        public WasiContext setProcessEnabled(boolean enabled) {
          return this;
        }

        @Override
        public WasiContext setFilesystemWorkingDir(Path workingDir) {
          return this;
        }

        @Override
        public WasiContext enableOutputCapture() throws WasmException {
          return this;
        }

        @Override
        public byte[] getStdoutCapture() {
          return null;
        }

        @Override
        public byte[] getStderrCapture() {
          return null;
        }

        @Override
        public boolean hasStdoutCapture() {
          return false;
        }

        @Override
        public boolean hasStderrCapture() {
          return false;
        }
      };

  @Nested
  @DisplayName("getEnvironment default method")
  class GetEnvironmentDefault {

    @Test
    @DisplayName("should return empty map by default")
    void shouldReturnEmptyMapByDefault() {
      Map<String, String> env = STUB.getEnvironment();

      assertNotNull(env);
      assertTrue(env.isEmpty());
    }
  }

  @Nested
  @DisplayName("getArguments default method")
  class GetArgumentsDefault {

    @Test
    @DisplayName("should return empty list by default")
    void shouldReturnEmptyListByDefault() {
      List<String> args = STUB.getArguments();

      assertNotNull(args);
      assertTrue(args.isEmpty());
    }
  }
}
