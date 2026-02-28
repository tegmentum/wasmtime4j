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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WASI environment variables and command-line arguments.
 *
 * <p>These tests verify:
 *
 * <ul>
 *   <li>Environment variable configuration
 *   <li>Command-line argument configuration
 *   <li>Environment inheritance options
 *   <li>Program name configuration
 * </ul>
 */
@DisplayName("WASI Environment Integration Tests")
@Tag("integration")
class WasiEnvironmentTest {

  private static final Logger LOGGER = Logger.getLogger(WasiEnvironmentTest.class.getName());

  private static boolean wasiAvailable = false;
  private static WasmRuntime sharedRuntime;
  private static Engine sharedEngine;

  @BeforeAll
  static void checkWasiAvailable() {
    LOGGER.info("Checking WASI availability");
    try {
      sharedRuntime = WasmRuntimeFactory.create();
      sharedEngine = sharedRuntime.createEngine();

      // Check if WASI context is available
      final WasiContext testContext = WasiContext.create();
      if (testContext != null) {
        wasiAvailable = true;
        LOGGER.info("WASI context is available");
      }
    } catch (final Exception e) {
      LOGGER.warning("WASI not available: " + e.getMessage());
      wasiAvailable = false;
    }
  }

  @AfterAll
  static void cleanupSharedResources() {
    if (sharedEngine != null) {
      try {
        sharedEngine.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing shared engine: " + e.getMessage());
      }
    }
    if (sharedRuntime != null) {
      try {
        sharedRuntime.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing shared runtime: " + e.getMessage());
      }
    }
  }

  @BeforeEach
  void setUp() {
    assumeTrue(wasiAvailable, "WASI must be available for these tests");
  }

  @Nested
  @DisplayName("Environment Variable Configuration Tests")
  class EnvironmentVariableConfigurationTests {

    @Test
    @DisplayName("should create WASI context with single environment variable")
    void shouldCreateContextWithSingleEnvVar() throws Exception {
      final WasiContext context = WasiContext.create().setEnv("TEST_VAR", "test_value");

      LOGGER.info("Created WASI context with single env var");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should create WASI context with multiple environment variables")
    void shouldCreateContextWithMultipleEnvVars() throws Exception {
      final Map<String, String> envVars = new HashMap<>();
      envVars.put("PATH", "/usr/bin:/bin");
      envVars.put("HOME", "/home/user");
      envVars.put("USER", "testuser");
      envVars.put("LANG", "en_US.UTF-8");

      final WasiContext context = WasiContext.create().setEnv(envVars);

      LOGGER.info("Created WASI context with " + envVars.size() + " env vars");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should create WASI context with empty environment variable value")
    void shouldCreateContextWithEmptyEnvValue() throws Exception {
      final WasiContext context = WasiContext.create().setEnv("EMPTY_VAR", "");

      LOGGER.info("Created WASI context with empty env var value");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should create WASI context with special characters in environment variable")
    void shouldCreateContextWithSpecialCharsInEnv() throws Exception {
      final WasiContext context =
          WasiContext.create().setEnv("SPECIAL", "value with spaces and !@#$%^&*()");

      LOGGER.info("Created WASI context with special chars in env var");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should create WASI context with chained environment variables")
    void shouldCreateContextWithChainedEnvVars() throws Exception {
      final WasiContext context =
          WasiContext.create()
              .setEnv("VAR1", "value1")
              .setEnv("VAR2", "value2")
              .setEnv("VAR3", "value3");

      LOGGER.info("Created WASI context with chained env vars");
      assertThat(context).isNotNull();
    }
  }

  @Nested
  @DisplayName("Command-Line Argument Configuration Tests")
  class CommandLineArgumentConfigurationTests {

    @Test
    @DisplayName("should create WASI context with single argument")
    void shouldCreateContextWithSingleArg() throws Exception {
      final WasiContext context = WasiContext.create().setArgv(new String[] {"--help"});

      LOGGER.info("Created WASI context with single arg");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should create WASI context with multiple arguments")
    void shouldCreateContextWithMultipleArgs() throws Exception {
      final WasiContext context =
          WasiContext.create()
              .setArgv(
                  new String[] {
                    "program_name",
                    "--config",
                    "/path/to/config.yaml",
                    "--verbose",
                    "--output",
                    "result.txt"
                  });

      LOGGER.info("Created WASI context with multiple args");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should create WASI context with arguments containing spaces")
    void shouldCreateContextWithArgsContainingSpaces() throws Exception {
      final WasiContext context =
          WasiContext.create()
              .setArgv(
                  new String[] {"program", "argument with spaces", "--message", "Hello World"});

      LOGGER.info("Created WASI context with args containing spaces");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should create WASI context with empty arguments array")
    void shouldCreateContextWithNoArgs() throws Exception {
      final WasiContext context = WasiContext.create().setArgv(new String[] {});

      LOGGER.info("Created WASI context with no args");
      assertThat(context).isNotNull();
    }
  }

  @Nested
  @DisplayName("Combined Configuration Tests")
  class CombinedConfigurationTests {

    @Test
    @DisplayName("should create WASI context with both env vars and args")
    void shouldCreateContextWithEnvAndArgs() throws Exception {
      final WasiContext context =
          WasiContext.create()
              .setEnv("HOME", "/home/testuser")
              .setEnv("PATH", "/usr/bin")
              .setArgv(new String[] {"myprogram", "--verbose"});

      LOGGER.info("Created WASI context with env vars and args");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should create WASI context with realistic configuration")
    void shouldCreateContextWithRealisticConfig() throws Exception {
      final Map<String, String> envVars = new HashMap<>();
      envVars.put("HOME", "/home/wasm");
      envVars.put("USER", "wasm");
      envVars.put("PATH", "/usr/local/bin:/usr/bin:/bin");
      envVars.put("TERM", "xterm-256color");
      envVars.put("LANG", "en_US.UTF-8");

      final WasiContext context =
          WasiContext.create()
              .setEnv(envVars)
              .setArgv(
                  new String[] {
                    "application", "--config", "/etc/app/config.json", "--log-level", "debug"
                  });

      LOGGER.info("Created WASI context with realistic configuration");
      assertThat(context).isNotNull();
    }
  }

  @Nested
  @DisplayName("Stdio Configuration Tests")
  class StdioConfigurationTests {

    @Test
    @DisplayName("should create WASI context that inherits all stdio")
    void shouldCreateContextInheritingAllStdio() throws Exception {
      final WasiContext context = WasiContext.create().inheritStdio();

      LOGGER.info("Created WASI context inheriting all stdio");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should create WASI context with combined stdio and env")
    void shouldCreateContextWithStdioAndEnv() throws Exception {
      final WasiContext context =
          WasiContext.create()
              .inheritStdio()
              .setEnv("TEST_MODE", "true")
              .setArgv(new String[] {"app", "--test"});

      LOGGER.info("Created WASI context with stdio and env");
      assertThat(context).isNotNull();
    }
  }

  @Nested
  @DisplayName("Environment Inheritance Tests")
  class EnvironmentInheritanceTests {

    @Test
    @DisplayName("should create WASI context that inherits host environment")
    void shouldCreateContextInheritingEnv() throws Exception {
      final WasiContext context = WasiContext.create().inheritEnv();

      LOGGER.info("Created WASI context inheriting host environment");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should create WASI context with inherited env and additional vars")
    void shouldCreateContextWithInheritedAndAdditionalEnv() throws Exception {
      final WasiContext context =
          WasiContext.create().inheritEnv().setEnv("ADDITIONAL_VAR", "additional_value");

      LOGGER.info("Created WASI context with inherited and additional env");
      assertThat(context).isNotNull();
    }
  }
}
