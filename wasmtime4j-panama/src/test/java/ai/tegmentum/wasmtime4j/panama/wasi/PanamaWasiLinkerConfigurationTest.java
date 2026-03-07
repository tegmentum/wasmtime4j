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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.PanamaEngine;
import ai.tegmentum.wasmtime4j.panama.PanamaLinker;
import ai.tegmentum.wasmtime4j.wasi.WasiStdioConfig;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PanamaWasiLinker configuration methods.
 *
 * <p>Tests cover all configuration methods including directory access, environment variables,
 * arguments, stdio, network access, and resource limits.
 */
@DisplayName("PanamaWasiLinker Configuration Tests")
class PanamaWasiLinkerConfigurationTest {

  private PanamaEngine engine;
  private PanamaLinker<Object> linker;
  private PanamaWasiLinker wasiLinker;

  @BeforeEach
  void setUp() throws WasmException {
    engine = new PanamaEngine();
    linker = new PanamaLinker<>(engine);
    wasiLinker = new PanamaWasiLinker(linker, engine, null);
  }

  @AfterEach
  void tearDown() {
    if (wasiLinker != null) {
      wasiLinker.close();
    }
    if (linker != null) {
      linker.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Nested
  @DisplayName("Directory Access Configuration")
  class DirectoryAccessTests {

    @Test
    @DisplayName("Should allow directory access with default permissions")
    void testAllowDirectoryAccessWithDefaultPermissions() {
      final Path hostPath = Paths.get("/tmp");
      final String guestPath = "/guest/tmp";

      assertDoesNotThrow(() -> wasiLinker.allowDirectoryAccess(hostPath, guestPath));
    }

    @Test
    @DisplayName("Should throw on null host path")
    void testAllowDirectoryAccessNullHostPath() {
      final String guestPath = "/guest/tmp";

      assertThatThrownBy(() -> wasiLinker.allowDirectoryAccess(null, guestPath))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Host path cannot be null");
    }

    @Test
    @DisplayName("Should throw on null guest path")
    void testAllowDirectoryAccessNullGuestPath() {
      final Path hostPath = Paths.get("/tmp");

      assertThatThrownBy(() -> wasiLinker.allowDirectoryAccess(hostPath, null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Guest path cannot be null");
    }

    @Test
    @DisplayName("Should allow multiple directory mappings")
    void testMultipleDirectoryMappings() {
      assertDoesNotThrow(
          () -> {
            wasiLinker.allowDirectoryAccess(Paths.get("/tmp"), "/guest/tmp");
            wasiLinker.allowDirectoryAccess(Paths.get("/var"), "/guest/var");
            wasiLinker.allowDirectoryAccess(Paths.get("/home"), "/guest/home");
          });
    }
  }

  @Nested
  @DisplayName("Environment Variable Configuration")
  class EnvironmentVariableTests {

    @Test
    @DisplayName("Should set single environment variable")
    void testSetEnvironmentVariable() {
      assertDoesNotThrow(() -> wasiLinker.setEnvironmentVariable("PATH", "/usr/bin"));
    }

    @Test
    @DisplayName("Should set environment variable with null value")
    void testSetEnvironmentVariableNullValue() {
      // Null value should be allowed (represents unsetting or empty)
      assertDoesNotThrow(() -> wasiLinker.setEnvironmentVariable("EMPTY_VAR", null));
    }

    @Test
    @DisplayName("Should throw on null environment variable name")
    void testSetEnvironmentVariableNullName() {
      assertThatThrownBy(() -> wasiLinker.setEnvironmentVariable(null, "value"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Environment variable name cannot be null");
    }

    @Test
    @DisplayName("Should set multiple environment variables from map")
    void testSetEnvironmentVariablesFromMap() {
      final Map<String, String> env = new HashMap<>();
      env.put("PATH", "/usr/bin");
      env.put("HOME", "/home/user");
      env.put("LANG", "en_US.UTF-8");

      assertDoesNotThrow(() -> wasiLinker.setEnvironmentVariables(env));
    }

    @Test
    @DisplayName("Should throw on null environment map")
    void testSetEnvironmentVariablesNullMap() {
      assertThatThrownBy(() -> wasiLinker.setEnvironmentVariables(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Environment map cannot be null");
    }

    @Test
    @DisplayName("Should inherit all environment variables")
    void testInheritEnvironment() {
      assertDoesNotThrow(() -> wasiLinker.inheritEnvironment());
    }

    @Test
    @DisplayName("Should inherit specific environment variables")
    void testInheritEnvironmentVariables() {
      final List<String> vars = Arrays.asList("PATH", "HOME", "USER");

      assertDoesNotThrow(() -> wasiLinker.inheritEnvironmentVariables(vars));
    }

    @Test
    @DisplayName("Should throw on null variable names list")
    void testInheritEnvironmentVariablesNullList() {
      assertThatThrownBy(() -> wasiLinker.inheritEnvironmentVariables(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Variable names list cannot be null");
    }
  }

  @Nested
  @DisplayName("Argument Configuration")
  class ArgumentTests {

    @Test
    @DisplayName("Should set command line arguments")
    void testSetArguments() {
      final List<String> args = Arrays.asList("program", "--verbose", "-o", "output.txt");

      assertDoesNotThrow(() -> wasiLinker.setArguments(args));
    }

    @Test
    @DisplayName("Should set empty arguments list")
    void testSetEmptyArguments() {
      final List<String> args = Arrays.asList();

      assertDoesNotThrow(() -> wasiLinker.setArguments(args));
    }

    @Test
    @DisplayName("Should throw on null arguments list")
    void testSetArgumentsNullList() {
      assertThatThrownBy(() -> wasiLinker.setArguments(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Arguments list cannot be null");
    }

    @Test
    @DisplayName("Should replace previous arguments")
    void testSetArgumentsReplacePrevious() {
      assertDoesNotThrow(
          () -> {
            wasiLinker.setArguments(Arrays.asList("first", "args"));
            wasiLinker.setArguments(Arrays.asList("new", "args", "replace"));
          });
    }
  }

  @Nested
  @DisplayName("Stdio Configuration")
  class StdioTests {

    @Test
    @DisplayName("Should configure stdin to inherit")
    void testConfigureStdinInherit() {
      final WasiStdioConfig config = WasiStdioConfig.inherit();

      assertDoesNotThrow(() -> wasiLinker.configureStdin(config));
    }

    @Test
    @DisplayName("Should configure stdin to null")
    void testConfigureStdinNull() {
      final WasiStdioConfig config = WasiStdioConfig.nulled();

      assertDoesNotThrow(() -> wasiLinker.configureStdin(config));
    }

    @Test
    @DisplayName("Should configure stdin from file")
    void testConfigureStdinFromFile() {
      final WasiStdioConfig config = WasiStdioConfig.fromFile(Paths.get("/tmp/input.txt"));

      assertDoesNotThrow(() -> wasiLinker.configureStdin(config));
    }

    @Test
    @DisplayName("Should throw on null stdin config")
    void testConfigureStdinNullConfig() {
      assertThatThrownBy(() -> wasiLinker.configureStdin(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Config cannot be null");
    }

    @Test
    @DisplayName("Should configure stdout to inherit")
    void testConfigureStdoutInherit() {
      final WasiStdioConfig config = WasiStdioConfig.inherit();

      assertDoesNotThrow(() -> wasiLinker.configureStdout(config));
    }

    @Test
    @DisplayName("Should configure stdout to file")
    void testConfigureStdoutToFile() {
      final WasiStdioConfig config = WasiStdioConfig.fromFile(Paths.get("/tmp/output.txt"));

      assertDoesNotThrow(() -> wasiLinker.configureStdout(config));
    }

    @Test
    @DisplayName("Should throw on null stdout config")
    void testConfigureStdoutNullConfig() {
      assertThatThrownBy(() -> wasiLinker.configureStdout(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Config cannot be null");
    }

    @Test
    @DisplayName("Should configure stderr to inherit")
    void testConfigureStderrInherit() {
      final WasiStdioConfig config = WasiStdioConfig.inherit();

      assertDoesNotThrow(() -> wasiLinker.configureStderr(config));
    }

    @Test
    @DisplayName("Should configure stderr to file")
    void testConfigureStderrToFile() {
      final WasiStdioConfig config = WasiStdioConfig.fromFile(Paths.get("/tmp/error.txt"));

      assertDoesNotThrow(() -> wasiLinker.configureStderr(config));
    }

    @Test
    @DisplayName("Should throw on null stderr config")
    void testConfigureStderrNullConfig() {
      assertThatThrownBy(() -> wasiLinker.configureStderr(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Config cannot be null");
    }
  }

  @Nested
  @DisplayName("Network Access Configuration")
  class NetworkAccessTests {

    @Test
    @DisplayName("Should enable network access")
    void testEnableNetworkAccess() {
      assertDoesNotThrow(() -> wasiLinker.enableNetworkAccess());
    }

    @Test
    @DisplayName("Should disable network access")
    void testDisableNetworkAccess() {
      assertDoesNotThrow(() -> wasiLinker.disableNetworkAccess());
    }

    @Test
    @DisplayName("Should allow toggling network access")
    void testToggleNetworkAccess() {
      assertDoesNotThrow(
          () -> {
            wasiLinker.enableNetworkAccess();
            wasiLinker.disableNetworkAccess();
            wasiLinker.enableNetworkAccess();
          });
    }
  }

  @Nested
  @DisplayName("Resource Limits Configuration")
  class ResourceLimitsTests {

    @Test
    @DisplayName("Should set max open files limit")
    void testSetMaxOpenFiles() {
      assertDoesNotThrow(() -> wasiLinker.setMaxOpenFiles(1024));
    }

    @Test
    @DisplayName("Should set max open files to null (no limit)")
    void testSetMaxOpenFilesNull() {
      assertDoesNotThrow(() -> wasiLinker.setMaxOpenFiles(null));
    }

    @Test
    @DisplayName("Should set max file size limit")
    void testSetMaxFileSize() {
      assertDoesNotThrow(() -> wasiLinker.setMaxFileSize(1024L * 1024L * 100L)); // 100MB
    }

    @Test
    @DisplayName("Should set max file size to null (no limit)")
    void testSetMaxFileSizeNull() {
      assertDoesNotThrow(() -> wasiLinker.setMaxFileSize(null));
    }
  }

  @Nested
  @DisplayName("Linker State Tests")
  class LinkerStateTests {

    @Test
    @DisplayName("Should report valid state after creation")
    void testIsValidAfterCreation() {
      assertThat(wasiLinker.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should report invalid state after close")
    void testIsValidAfterClose() {
      wasiLinker.close();
      assertThat(wasiLinker.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should return engine")
    void testGetEngine() {
      assertThat(wasiLinker.getEngine()).isEqualTo(engine);
    }

    @Test
    @DisplayName("Should return underlying linker")
    void testGetLinker() {
      assertThat(wasiLinker.getLinker()).isEqualTo(linker);
    }

    @Test
    @DisplayName("Should return null config when created with null")
    void testGetConfigNull() {
      assertThat(wasiLinker.getConfig()).isNull();
    }

    @Test
    @DisplayName("Should allow double close without error")
    void testDoubleClose() {
      assertDoesNotThrow(
          () -> {
            wasiLinker.close();
            wasiLinker.close();
          });
    }
  }

  @Nested
  @DisplayName("Closed Linker Tests")
  class ClosedLinkerTests {

    @BeforeEach
    void closeLinker() {
      wasiLinker.close();
    }

    @Test
    @DisplayName("Should throw on allowDirectoryAccess after close")
    void testAllowDirectoryAccessAfterClose() {
      assertThatThrownBy(() -> wasiLinker.allowDirectoryAccess(Paths.get("/tmp"), "/guest/tmp"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("Should throw on setEnvironmentVariable after close")
    void testSetEnvironmentVariableAfterClose() {
      assertThatThrownBy(() -> wasiLinker.setEnvironmentVariable("PATH", "/usr/bin"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("Should throw on setArguments after close")
    void testSetArgumentsAfterClose() {
      assertThatThrownBy(() -> wasiLinker.setArguments(Arrays.asList("arg1")))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("Should throw on configureStdin after close")
    void testConfigureStdinAfterClose() {
      assertThatThrownBy(() -> wasiLinker.configureStdin(WasiStdioConfig.inherit()))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("Should throw on enableNetworkAccess after close")
    void testEnableNetworkAccessAfterClose() {
      assertThatThrownBy(() -> wasiLinker.enableNetworkAccess())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("Should throw on getLinker after close")
    void testGetLinkerAfterClose() {
      assertThatThrownBy(() -> wasiLinker.getLinker())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("closed");
    }
  }

  @Nested
  @DisplayName("Combined Configuration Tests")
  class CombinedConfigurationTests {

    @Test
    @DisplayName("Should accept full WASI configuration")
    void testFullConfiguration() {
      assertDoesNotThrow(
          () -> {
            // Directory access
            wasiLinker.allowDirectoryAccess(Paths.get("/tmp"), "/tmp");
            wasiLinker.allowDirectoryAccess(Paths.get("/var/log"), "/logs");

            // Environment variables
            wasiLinker.setEnvironmentVariable("APP_ENV", "production");
            final Map<String, String> env = new HashMap<>();
            env.put("PATH", "/usr/bin");
            env.put("HOME", "/home/app");
            wasiLinker.setEnvironmentVariables(env);

            // Arguments
            wasiLinker.setArguments(Arrays.asList("myapp", "--config", "/etc/app.conf"));

            // Stdio
            wasiLinker.configureStdin(WasiStdioConfig.inherit());
            wasiLinker.configureStdout(WasiStdioConfig.inherit());
            wasiLinker.configureStderr(WasiStdioConfig.fromFile(Paths.get("/var/log/app.err")));

            // Network
            wasiLinker.enableNetworkAccess();

            // Resource limits
            wasiLinker.setMaxOpenFiles(256);
            wasiLinker.setMaxFileSize(1024L * 1024L * 50L);
          });
    }

    @Test
    @DisplayName("Should allow reconfiguration")
    void testReconfiguration() {
      assertDoesNotThrow(
          () -> {
            // Initial configuration
            wasiLinker.setEnvironmentVariable("MODE", "development");
            wasiLinker.setMaxOpenFiles(100);
            wasiLinker.disableNetworkAccess();

            // Reconfigure
            wasiLinker.setEnvironmentVariable("MODE", "production");
            wasiLinker.setMaxOpenFiles(1000);
            wasiLinker.enableNetworkAccess();
          });
    }
  }
}
