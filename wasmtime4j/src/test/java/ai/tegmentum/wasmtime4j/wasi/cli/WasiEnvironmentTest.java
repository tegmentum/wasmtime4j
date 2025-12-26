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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiEnvironment} interface.
 *
 * <p>WasiEnvironment provides access to POSIX-style environment variables and command-line
 * arguments for CLI programs according to WASI Preview 2 specification.
 */
@DisplayName("WasiEnvironment Tests")
class WasiEnvironmentTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiEnvironment.class.isInterface(), "WasiEnvironment should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiEnvironment.class.getModifiers()),
          "WasiEnvironment should be public");
    }

    @Test
    @DisplayName("should have getEnvironmentVariables method")
    void shouldHaveGetEnvironmentVariablesMethod() throws NoSuchMethodException {
      final Method method = WasiEnvironment.class.getMethod("getEnvironmentVariables");
      assertNotNull(method, "getEnvironmentVariables method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getVariable method")
    void shouldHaveGetVariableMethod() throws NoSuchMethodException {
      final Method method = WasiEnvironment.class.getMethod("getVariable", String.class);
      assertNotNull(method, "getVariable method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getArguments method")
    void shouldHaveGetArgumentsMethod() throws NoSuchMethodException {
      final Method method = WasiEnvironment.class.getMethod("getArguments");
      assertNotNull(method, "getArguments method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getInitialCwd method")
    void shouldHaveGetInitialCwdMethod() throws NoSuchMethodException {
      final Method method = WasiEnvironment.class.getMethod("getInitialCwd");
      assertNotNull(method, "getInitialCwd method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have exactly four methods")
    void shouldHaveExactlyFourMethods() {
      int methodCount = 0;
      for (final Method method : WasiEnvironment.class.getDeclaredMethods()) {
        if (!method.isSynthetic()) {
          methodCount++;
        }
      }
      assertEquals(4, methodCount, "WasiEnvironment should have exactly 4 methods");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("getEnvironmentVariables should take no parameters")
    void getEnvironmentVariablesShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WasiEnvironment.class.getMethod("getEnvironmentVariables");
      assertEquals(0, method.getParameterCount(), "Should take no parameters");
    }

    @Test
    @DisplayName("getVariable should take String parameter")
    void getVariableShouldTakeStringParameter() throws NoSuchMethodException {
      final Method method = WasiEnvironment.class.getMethod("getVariable", String.class);
      assertEquals(1, method.getParameterCount(), "Should take one parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
    }

    @Test
    @DisplayName("getArguments should take no parameters")
    void getArgumentsShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WasiEnvironment.class.getMethod("getArguments");
      assertEquals(0, method.getParameterCount(), "Should take no parameters");
    }

    @Test
    @DisplayName("getInitialCwd should take no parameters")
    void getInitialCwdShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WasiEnvironment.class.getMethod("getInitialCwd");
      assertEquals(0, method.getParameterCount(), "Should take no parameters");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock should return environment variables")
    void mockShouldReturnEnvironmentVariables() {
      final MockWasiEnvironment env = new MockWasiEnvironment();
      env.setEnvironmentVariable("PATH", "/usr/bin:/bin");
      env.setEnvironmentVariable("HOME", "/home/user");

      final Map<String, String> vars = env.getEnvironmentVariables();

      assertNotNull(vars, "Environment variables should not be null");
      assertEquals(2, vars.size(), "Should have 2 variables");
      assertEquals("/usr/bin:/bin", vars.get("PATH"), "PATH should match");
      assertEquals("/home/user", vars.get("HOME"), "HOME should match");
    }

    @Test
    @DisplayName("mock should return specific variable")
    void mockShouldReturnSpecificVariable() {
      final MockWasiEnvironment env = new MockWasiEnvironment();
      env.setEnvironmentVariable("USER", "testuser");

      final Optional<String> user = env.getVariable("USER");

      assertTrue(user.isPresent(), "USER should be present");
      assertEquals("testuser", user.get(), "USER value should match");
    }

    @Test
    @DisplayName("mock should return empty for missing variable")
    void mockShouldReturnEmptyForMissingVariable() {
      final MockWasiEnvironment env = new MockWasiEnvironment();

      final Optional<String> result = env.getVariable("NONEXISTENT");

      assertFalse(result.isPresent(), "Should be empty for missing variable");
    }

    @Test
    @DisplayName("mock should return command-line arguments")
    void mockShouldReturnCommandLineArguments() {
      final MockWasiEnvironment env = new MockWasiEnvironment();
      env.setArguments(List.of("program", "--verbose", "-o", "output.txt"));

      final List<String> args = env.getArguments();

      assertNotNull(args, "Arguments should not be null");
      assertEquals(4, args.size(), "Should have 4 arguments");
      assertEquals("program", args.get(0), "First arg should be program name");
      assertEquals("--verbose", args.get(1), "Second arg should be --verbose");
    }

    @Test
    @DisplayName("mock should return initial working directory")
    void mockShouldReturnInitialWorkingDirectory() {
      final MockWasiEnvironment env = new MockWasiEnvironment();
      env.setInitialCwd("/home/user/project");

      final Optional<String> cwd = env.getInitialCwd();

      assertTrue(cwd.isPresent(), "Initial CWD should be present");
      assertEquals("/home/user/project", cwd.get(), "CWD should match");
    }

    @Test
    @DisplayName("mock should return empty for no initial cwd")
    void mockShouldReturnEmptyForNoInitialCwd() {
      final MockWasiEnvironment env = new MockWasiEnvironment();

      final Optional<String> cwd = env.getInitialCwd();

      assertFalse(cwd.isPresent(), "Should be empty when no CWD set");
    }

    @Test
    @DisplayName("mock should return empty arguments list by default")
    void mockShouldReturnEmptyArgumentsListByDefault() {
      final MockWasiEnvironment env = new MockWasiEnvironment();

      final List<String> args = env.getArguments();

      assertNotNull(args, "Arguments should not be null");
      assertTrue(args.isEmpty(), "Arguments should be empty by default");
    }

    @Test
    @DisplayName("mock should return empty environment by default")
    void mockShouldReturnEmptyEnvironmentByDefault() {
      final MockWasiEnvironment env = new MockWasiEnvironment();

      final Map<String, String> vars = env.getEnvironmentVariables();

      assertNotNull(vars, "Environment should not be null");
      assertTrue(vars.isEmpty(), "Environment should be empty by default");
    }
  }

  /** Mock implementation of WasiEnvironment for testing. */
  private static class MockWasiEnvironment implements WasiEnvironment {
    private final Map<String, String> environment = new HashMap<>();
    private List<String> arguments = List.of();
    private String initialCwd = null;

    @Override
    public Map<String, String> getEnvironmentVariables() {
      return Map.copyOf(environment);
    }

    @Override
    public Optional<String> getVariable(final String name) {
      return Optional.ofNullable(environment.get(name));
    }

    @Override
    public List<String> getArguments() {
      return arguments;
    }

    @Override
    public Optional<String> getInitialCwd() {
      return Optional.ofNullable(initialCwd);
    }

    public void setEnvironmentVariable(final String name, final String value) {
      environment.put(name, value);
    }

    public void setArguments(final List<String> args) {
      this.arguments = List.copyOf(args);
    }

    public void setInitialCwd(final String cwd) {
      this.initialCwd = cwd;
    }
  }
}
