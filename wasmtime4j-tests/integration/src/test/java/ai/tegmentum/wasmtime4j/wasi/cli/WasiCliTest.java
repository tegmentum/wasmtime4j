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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WASI CLI package interfaces.
 *
 * <p>This test class validates the WasiEnvironment, WasiExit, and WasiStdio interfaces.
 */
@DisplayName("WASI CLI Integration Tests")
public class WasiCliTest {

  private static final Logger LOGGER = Logger.getLogger(WasiCliTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting WASI CLI Integration Tests");
  }

  @Nested
  @DisplayName("WasiEnvironment Interface Tests")
  class WasiEnvironmentTests {

    @Test
    @DisplayName("Should verify WasiEnvironment interface exists")
    void shouldVerifyWasiEnvironmentInterfaceExists() {
      LOGGER.info("Testing WasiEnvironment interface existence");

      assertNotNull(WasiEnvironment.class, "WasiEnvironment interface should exist");
      assertTrue(WasiEnvironment.class.isInterface(), "WasiEnvironment should be an interface");

      LOGGER.info("WasiEnvironment interface verified");
    }

    @Test
    @DisplayName("Should implement WasiEnvironment with environment variables")
    void shouldImplementWasiEnvironmentWithEnvironmentVariables() {
      LOGGER.info("Testing WasiEnvironment getEnvironmentVariables method");

      Map<String, String> envVars = new HashMap<>();
      envVars.put("PATH", "/usr/bin:/bin");
      envVars.put("HOME", "/home/user");
      envVars.put("USER", "testuser");

      WasiEnvironment env = createTestEnvironment(envVars, List.of("program", "arg1"), "/cwd");

      Map<String, String> result = env.getEnvironmentVariables();

      assertNotNull(result, "Environment variables should not be null");
      assertEquals(3, result.size(), "Should have 3 environment variables");
      assertEquals("/usr/bin:/bin", result.get("PATH"), "PATH should match");
      assertEquals("/home/user", result.get("HOME"), "HOME should match");
      assertEquals("testuser", result.get("USER"), "USER should match");

      LOGGER.info("Environment variables verified: " + result.size() + " vars");
    }

    @Test
    @DisplayName("Should implement WasiEnvironment with getVariable method")
    void shouldImplementWasiEnvironmentWithGetVariableMethod() {
      LOGGER.info("Testing WasiEnvironment getVariable method");

      Map<String, String> envVars = new HashMap<>();
      envVars.put("WASM_DEBUG", "true");
      envVars.put("WASM_LEVEL", "3");

      WasiEnvironment env = createTestEnvironment(envVars, List.of(), null);

      Optional<String> debug = env.getVariable("WASM_DEBUG");
      assertTrue(debug.isPresent(), "WASM_DEBUG should be present");
      assertEquals("true", debug.get(), "WASM_DEBUG value should match");

      Optional<String> level = env.getVariable("WASM_LEVEL");
      assertTrue(level.isPresent(), "WASM_LEVEL should be present");
      assertEquals("3", level.get(), "WASM_LEVEL value should match");

      Optional<String> missing = env.getVariable("NONEXISTENT");
      assertTrue(missing.isEmpty(), "NONEXISTENT should be empty");

      LOGGER.info("getVariable method verified");
    }

    @Test
    @DisplayName("Should implement WasiEnvironment with arguments")
    void shouldImplementWasiEnvironmentWithArguments() {
      LOGGER.info("Testing WasiEnvironment getArguments method");

      List<String> args = List.of("myprogram", "--config", "test.toml", "-v");

      WasiEnvironment env = createTestEnvironment(Map.of(), args, null);

      List<String> result = env.getArguments();

      assertNotNull(result, "Arguments should not be null");
      assertEquals(4, result.size(), "Should have 4 arguments");
      assertEquals("myprogram", result.get(0), "First arg should be program name");
      assertEquals("--config", result.get(1), "Second arg should be --config");
      assertEquals("test.toml", result.get(2), "Third arg should be config file");
      assertEquals("-v", result.get(3), "Fourth arg should be -v");

      LOGGER.info("Arguments verified: " + result.size() + " args");
    }

    @Test
    @DisplayName("Should implement WasiEnvironment with initial working directory")
    void shouldImplementWasiEnvironmentWithInitialCwd() {
      LOGGER.info("Testing WasiEnvironment getInitialCwd method");

      WasiEnvironment envWithCwd = createTestEnvironment(Map.of(), List.of(), "/workspace/project");
      WasiEnvironment envWithoutCwd = createTestEnvironment(Map.of(), List.of(), null);

      Optional<String> cwd = envWithCwd.getInitialCwd();
      Optional<String> noCwd = envWithoutCwd.getInitialCwd();

      assertTrue(cwd.isPresent(), "Initial CWD should be present");
      assertEquals("/workspace/project", cwd.get(), "CWD should match");

      assertTrue(noCwd.isEmpty(), "Initial CWD should be empty when not provided");

      LOGGER.info("Initial CWD verified");
    }

    @Test
    @DisplayName("Should handle empty environment correctly")
    void shouldHandleEmptyEnvironmentCorrectly() {
      LOGGER.info("Testing empty environment handling");

      WasiEnvironment env = createTestEnvironment(Map.of(), List.of(), null);

      Map<String, String> vars = env.getEnvironmentVariables();
      assertNotNull(vars, "Variables should not be null");
      assertTrue(vars.isEmpty(), "Variables should be empty");

      List<String> args = env.getArguments();
      assertNotNull(args, "Arguments should not be null");
      assertTrue(args.isEmpty(), "Arguments should be empty");

      Optional<String> cwd = env.getInitialCwd();
      assertTrue(cwd.isEmpty(), "CWD should be empty");

      LOGGER.info("Empty environment handling verified");
    }

    private WasiEnvironment createTestEnvironment(
        final Map<String, String> envVars, final List<String> args, final String cwd) {
      return new WasiEnvironment() {
        @Override
        public Map<String, String> getEnvironmentVariables() {
          return Map.copyOf(envVars);
        }

        @Override
        public Optional<String> getVariable(final String name) {
          if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Variable name cannot be null or empty");
          }
          return Optional.ofNullable(envVars.get(name));
        }

        @Override
        public List<String> getArguments() {
          return List.copyOf(args);
        }

        @Override
        public Optional<String> getInitialCwd() {
          return Optional.ofNullable(cwd);
        }
      };
    }
  }

  @Nested
  @DisplayName("WasiExit Interface Tests")
  class WasiExitTests {

    @Test
    @DisplayName("Should verify WasiExit interface exists")
    void shouldVerifyWasiExitInterfaceExists() {
      LOGGER.info("Testing WasiExit interface existence");

      assertNotNull(WasiExit.class, "WasiExit interface should exist");
      assertTrue(WasiExit.class.isInterface(), "WasiExit should be an interface");

      LOGGER.info("WasiExit interface verified");
    }

    @Test
    @DisplayName("Should have correct exit status constants")
    void shouldHaveCorrectExitStatusConstants() {
      LOGGER.info("Testing WasiExit exit status constants");

      assertEquals(0, WasiExit.EXIT_SUCCESS, "EXIT_SUCCESS should be 0");
      assertEquals(1, WasiExit.EXIT_FAILURE, "EXIT_FAILURE should be 1");

      LOGGER.info("Exit status constants verified");
    }

    @Test
    @DisplayName("Should implement WasiExit with exit method")
    void shouldImplementWasiExitWithExitMethod() {
      LOGGER.info("Testing WasiExit exit method");

      AtomicInteger capturedExitCode = new AtomicInteger(-1);

      WasiExit exit =
          statusCode -> {
            capturedExitCode.set(statusCode);
          };

      // Test success exit
      exit.exit(WasiExit.EXIT_SUCCESS);
      assertEquals(0, capturedExitCode.get(), "Exit code should be 0 for success");

      // Test failure exit
      exit.exit(WasiExit.EXIT_FAILURE);
      assertEquals(1, capturedExitCode.get(), "Exit code should be 1 for failure");

      // Test custom exit code
      exit.exit(42);
      assertEquals(42, capturedExitCode.get(), "Exit code should be 42");

      LOGGER.info("Exit method verified");
    }

    @Test
    @DisplayName("Should handle negative exit codes")
    void shouldHandleNegativeExitCodes() {
      LOGGER.info("Testing WasiExit with negative exit codes");

      AtomicInteger capturedExitCode = new AtomicInteger(0);

      WasiExit exit = capturedExitCode::set;

      exit.exit(-1);
      assertEquals(-1, capturedExitCode.get(), "Exit code should be -1");

      exit.exit(-128);
      assertEquals(-128, capturedExitCode.get(), "Exit code should be -128");

      LOGGER.info("Negative exit codes verified");
    }

    @Test
    @DisplayName("Should handle large exit codes")
    void shouldHandleLargeExitCodes() {
      LOGGER.info("Testing WasiExit with large exit codes");

      AtomicInteger capturedExitCode = new AtomicInteger(0);

      WasiExit exit = capturedExitCode::set;

      exit.exit(255);
      assertEquals(255, capturedExitCode.get(), "Exit code should be 255");

      exit.exit(Integer.MAX_VALUE);
      assertEquals(Integer.MAX_VALUE, capturedExitCode.get(), "Exit code should be MAX_VALUE");

      LOGGER.info("Large exit codes verified");
    }
  }

  @Nested
  @DisplayName("WasiStdio Interface Tests")
  class WasiStdioTests {

    @Test
    @DisplayName("Should verify WasiStdio interface exists")
    void shouldVerifyWasiStdioInterfaceExists() {
      LOGGER.info("Testing WasiStdio interface existence");

      assertNotNull(WasiStdio.class, "WasiStdio interface should exist");
      assertTrue(WasiStdio.class.isInterface(), "WasiStdio should be an interface");

      LOGGER.info("WasiStdio interface verified");
    }

    @Test
    @DisplayName("Should have getStdin method")
    void shouldHaveGetStdinMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiStdio getStdin method signature");

      java.lang.reflect.Method method = WasiStdio.class.getMethod("getStdin");
      assertNotNull(method, "getStdin method should exist");
      assertEquals(
          WasiInputStream.class, method.getReturnType(), "getStdin should return WasiInputStream");

      LOGGER.info("getStdin method signature verified");
    }

    @Test
    @DisplayName("Should have getStdout method")
    void shouldHaveGetStdoutMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiStdio getStdout method signature");

      java.lang.reflect.Method method = WasiStdio.class.getMethod("getStdout");
      assertNotNull(method, "getStdout method should exist");
      assertEquals(
          WasiOutputStream.class,
          method.getReturnType(),
          "getStdout should return WasiOutputStream");

      LOGGER.info("getStdout method signature verified");
    }

    @Test
    @DisplayName("Should have getStderr method")
    void shouldHaveGetStderrMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiStdio getStderr method signature");

      java.lang.reflect.Method method = WasiStdio.class.getMethod("getStderr");
      assertNotNull(method, "getStderr method should exist");
      assertEquals(
          WasiOutputStream.class,
          method.getReturnType(),
          "getStderr should return WasiOutputStream");

      LOGGER.info("getStderr method signature verified");
    }

    @Test
    @DisplayName("Should have exactly three stream accessor methods")
    void shouldHaveExactlyThreeStreamAccessorMethods() {
      LOGGER.info("Testing WasiStdio has exactly three methods");

      java.lang.reflect.Method[] methods = WasiStdio.class.getDeclaredMethods();
      assertEquals(3, methods.length, "WasiStdio should have exactly 3 methods");

      java.util.Set<String> methodNames =
          java.util.Arrays.stream(methods)
              .map(java.lang.reflect.Method::getName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(methodNames.contains("getStdin"), "Should have getStdin method");
      assertTrue(methodNames.contains("getStdout"), "Should have getStdout method");
      assertTrue(methodNames.contains("getStderr"), "Should have getStderr method");

      LOGGER.info("All expected methods verified: " + methodNames);
    }
  }
}
