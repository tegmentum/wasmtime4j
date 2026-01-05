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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiProcessConfig} class.
 *
 * <p>WasiProcessConfig encapsulates configuration parameters for spawning WASI processes.
 */
@DisplayName("WasiProcessConfig Tests")
class WasiProcessConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiProcessConfig.class.getModifiers()),
          "WasiProcessConfig should be public");
      assertTrue(
          Modifier.isFinal(WasiProcessConfig.class.getModifiers()),
          "WasiProcessConfig should be final");
    }

    @Test
    @DisplayName("should have static builder method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      final Method method = WasiProcessConfig.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          WasiProcessConfig.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have toBuilder method")
    void shouldHaveToBuilderMethod() throws NoSuchMethodException {
      final Method method = WasiProcessConfig.class.getMethod("toBuilder");
      assertNotNull(method, "toBuilder method should exist");
      assertEquals(
          WasiProcessConfig.Builder.class, method.getReturnType(), "Should return Builder");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("should have getProgram method")
    void shouldHaveGetProgramMethod() throws NoSuchMethodException {
      final Method method = WasiProcessConfig.class.getMethod("getProgram");
      assertNotNull(method, "getProgram method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getArguments method")
    void shouldHaveGetArgumentsMethod() throws NoSuchMethodException {
      final Method method = WasiProcessConfig.class.getMethod("getArguments");
      assertNotNull(method, "getArguments method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getEnvironment method")
    void shouldHaveGetEnvironmentMethod() throws NoSuchMethodException {
      final Method method = WasiProcessConfig.class.getMethod("getEnvironment");
      assertNotNull(method, "getEnvironment method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getWorkingDirectory method")
    void shouldHaveGetWorkingDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiProcessConfig.class.getMethod("getWorkingDirectory");
      assertNotNull(method, "getWorkingDirectory method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getResourceLimits method")
    void shouldHaveGetResourceLimitsMethod() throws NoSuchMethodException {
      final Method method = WasiProcessConfig.class.getMethod("getResourceLimits");
      assertNotNull(method, "getResourceLimits method should exist");
      assertEquals(
          WasiResourceLimits.class, method.getReturnType(), "Should return WasiResourceLimits");
    }
  }

  @Nested
  @DisplayName("Builder Behavior Tests")
  class BuilderBehaviorTests {

    @Test
    @DisplayName("should build config with program")
    void shouldBuildConfigWithProgram() {
      final WasiProcessConfig config = WasiProcessConfig.builder().setProgram("/bin/echo").build();

      assertEquals("/bin/echo", config.getProgram(), "Program should match");
    }

    @Test
    @DisplayName("should build config with arguments")
    void shouldBuildConfigWithArguments() {
      final WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .addArgument("Hello")
              .addArgument("World")
              .build();

      assertEquals(2, config.getArguments().size(), "Should have 2 arguments");
      assertEquals("Hello", config.getArguments().get(0), "First argument should match");
      assertEquals("World", config.getArguments().get(1), "Second argument should match");
    }

    @Test
    @DisplayName("should build config with setArguments")
    void shouldBuildConfigWithSetArguments() {
      final List<String> args = Arrays.asList("arg1", "arg2", "arg3");
      final WasiProcessConfig config =
          WasiProcessConfig.builder().setProgram("/bin/test").setArguments(args).build();

      assertEquals(3, config.getArguments().size(), "Should have 3 arguments");
      assertEquals(args, config.getArguments(), "Arguments should match");
    }

    @Test
    @DisplayName("should build config with environment variable")
    void shouldBuildConfigWithEnvironmentVariable() {
      final WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/test")
              .setEnvironmentVariable("PATH", "/usr/bin")
              .setEnvironmentVariable("HOME", "/home/user")
              .build();

      assertEquals(2, config.getEnvironment().size(), "Should have 2 env vars");
      assertEquals("/usr/bin", config.getEnvironment().get("PATH"), "PATH should match");
      assertEquals("/home/user", config.getEnvironment().get("HOME"), "HOME should match");
    }

    @Test
    @DisplayName("should build config with setEnvironment")
    void shouldBuildConfigWithSetEnvironment() {
      final Map<String, String> env = new HashMap<>();
      env.put("KEY1", "VALUE1");
      env.put("KEY2", "VALUE2");

      final WasiProcessConfig config =
          WasiProcessConfig.builder().setProgram("/bin/test").setEnvironment(env).build();

      assertEquals(2, config.getEnvironment().size(), "Should have 2 env vars");
      assertEquals("VALUE1", config.getEnvironment().get("KEY1"), "KEY1 should match");
    }

    @Test
    @DisplayName("should build config with working directory")
    void shouldBuildConfigWithWorkingDirectory() {
      final WasiProcessConfig config =
          WasiProcessConfig.builder().setProgram("/bin/test").setWorkingDirectory("/tmp").build();

      assertEquals("/tmp", config.getWorkingDirectory(), "Working directory should match");
    }

    @Test
    @DisplayName("should return null working directory when not set")
    void shouldReturnNullWorkingDirectoryWhenNotSet() {
      final WasiProcessConfig config = WasiProcessConfig.builder().setProgram("/bin/test").build();

      assertNull(config.getWorkingDirectory(), "Working directory should be null");
    }

    @Test
    @DisplayName("should return null resource limits when not set")
    void shouldReturnNullResourceLimitsWhenNotSet() {
      final WasiProcessConfig config = WasiProcessConfig.builder().setProgram("/bin/test").build();

      assertNull(config.getResourceLimits(), "Resource limits should be null");
    }

    @Test
    @DisplayName("toBuilder should preserve all values")
    void toBuilderShouldPreserveAllValues() {
      final WasiProcessConfig original =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .addArgument("Hello")
              .setEnvironmentVariable("KEY", "VALUE")
              .setWorkingDirectory("/tmp")
              .build();

      final WasiProcessConfig copy = original.toBuilder().build();

      assertEquals(original.getProgram(), copy.getProgram(), "Program should match");
      assertEquals(original.getArguments(), copy.getArguments(), "Arguments should match");
      assertEquals(original.getEnvironment(), copy.getEnvironment(), "Environment should match");
      assertEquals(
          original.getWorkingDirectory(),
          copy.getWorkingDirectory(),
          "Working directory should match");
    }
  }

  @Nested
  @DisplayName("Builder Validation Tests")
  class BuilderValidationTests {

    @Test
    @DisplayName("should throw on null program")
    void shouldThrowOnNullProgram() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            WasiProcessConfig.builder().setProgram(null).build();
          },
          "Should throw on null program");
    }

    @Test
    @DisplayName("should throw on empty program")
    void shouldThrowOnEmptyProgram() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            WasiProcessConfig.builder().setProgram("").build();
          },
          "Should throw on empty program");
    }

    @Test
    @DisplayName("should throw on whitespace-only program")
    void shouldThrowOnWhitespaceOnlyProgram() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            WasiProcessConfig.builder().setProgram("   ").build();
          },
          "Should throw on whitespace-only program");
    }

    @Test
    @DisplayName("should throw on build without program")
    void shouldThrowOnBuildWithoutProgram() {
      assertThrows(
          IllegalStateException.class,
          () -> {
            WasiProcessConfig.builder().build();
          },
          "Should throw when program is not set");
    }

    @Test
    @DisplayName("should throw on null argument")
    void shouldThrowOnNullArgument() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            WasiProcessConfig.builder().setProgram("/bin/test").addArgument(null);
          },
          "Should throw on null argument");
    }

    @Test
    @DisplayName("should throw on null arguments list")
    void shouldThrowOnNullArgumentsList() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            WasiProcessConfig.builder().setProgram("/bin/test").setArguments(null);
          },
          "Should throw on null arguments list");
    }

    @Test
    @DisplayName("should throw on null environment variable key")
    void shouldThrowOnNullEnvironmentVariableKey() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            WasiProcessConfig.builder()
                .setProgram("/bin/test")
                .setEnvironmentVariable(null, "value");
          },
          "Should throw on null key");
    }

    @Test
    @DisplayName("should throw on null environment variable value")
    void shouldThrowOnNullEnvironmentVariableValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            WasiProcessConfig.builder().setProgram("/bin/test").setEnvironmentVariable("key", null);
          },
          "Should throw on null value");
    }

    @Test
    @DisplayName("should throw on null environment map")
    void shouldThrowOnNullEnvironmentMap() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            WasiProcessConfig.builder().setProgram("/bin/test").setEnvironment(null);
          },
          "Should throw on null environment map");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("arguments list should be immutable")
    void argumentsListShouldBeImmutable() {
      final WasiProcessConfig config =
          WasiProcessConfig.builder().setProgram("/bin/test").addArgument("arg1").build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> {
            config.getArguments().add("arg2");
          },
          "Arguments list should be immutable");
    }

    @Test
    @DisplayName("environment map should be immutable")
    void environmentMapShouldBeImmutable() {
      final WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/test")
              .setEnvironmentVariable("KEY", "VALUE")
              .build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> {
            config.getEnvironment().put("NEW_KEY", "NEW_VALUE");
          },
          "Environment map should be immutable");
    }
  }
}
