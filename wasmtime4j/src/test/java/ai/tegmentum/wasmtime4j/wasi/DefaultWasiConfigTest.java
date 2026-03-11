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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultWasiConfig} and {@link DefaultWasiConfigBuilder}, exercising the builder
 * pattern, getters, validation, and immutability via {@link AbstractWasiConfig} and {@link
 * AbstractWasiConfigBuilder}.
 */
@DisplayName("DefaultWasiConfig and DefaultWasiConfigBuilder Tests")
class DefaultWasiConfigTest {

  @Nested
  @DisplayName("Builder Default Values")
  class BuilderDefaultValues {

    @Test
    @DisplayName("should build config with empty defaults")
    void shouldBuildConfigWithEmptyDefaults() {
      WasiConfig config = WasiConfig.builder().build();

      assertNotNull(config);
      assertTrue(config.getEnvironment().isEmpty());
      assertTrue(config.getArguments().isEmpty());
      assertTrue(config.getPreopenDirectories().isEmpty());
      assertFalse(config.getWorkingDirectory().isPresent());
      assertFalse(config.getExecutionTimeout().isPresent());
      assertFalse(config.isValidationEnabled());
      assertFalse(config.isStrictModeEnabled());
      assertEquals(WasiVersion.PREVIEW_1, config.getWasiVersion());
    }

    @Test
    @DisplayName("defaultConfig static method should produce valid config")
    void defaultConfigShouldProduceValidConfig() {
      WasiConfig config = WasiConfig.defaultConfig();

      assertNotNull(config);
      assertTrue(config.getEnvironment().isEmpty());
    }
  }

  @Nested
  @DisplayName("Environment Variable Configuration")
  class EnvironmentVariableConfiguration {

    @Test
    @DisplayName("should set single environment variable")
    void shouldSetSingleEnvironmentVariable() {
      WasiConfig config = WasiConfig.builder().withEnvironment("HOME", "/app").build();

      assertEquals(1, config.getEnvironment().size());
      assertEquals("/app", config.getEnvironment().get("HOME"));
    }

    @Test
    @DisplayName("should set multiple environment variables via map")
    void shouldSetMultipleEnvironmentVariablesViaMap() {
      Map<String, String> env = new HashMap<>();
      env.put("HOME", "/app");
      env.put("PATH", "/usr/bin");

      WasiConfig config = WasiConfig.builder().withEnvironment(env).build();

      assertEquals(2, config.getEnvironment().size());
      assertEquals("/app", config.getEnvironment().get("HOME"));
      assertEquals("/usr/bin", config.getEnvironment().get("PATH"));
    }

    @Test
    @DisplayName("should throw when environment variable name is null")
    void shouldThrowWhenEnvironmentNameIsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiConfig.builder().withEnvironment(null, "value"));
    }

    @Test
    @DisplayName("should throw when environment map is null")
    void shouldThrowWhenEnvironmentMapIsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiConfig.builder().withEnvironment((Map<String, String>) null));
    }

    @Test
    @DisplayName("should remove environment variable")
    void shouldRemoveEnvironmentVariable() {
      WasiConfig config =
          WasiConfig.builder()
              .withEnvironment("HOME", "/app")
              .withEnvironment("PATH", "/usr/bin")
              .withoutEnvironment("HOME")
              .build();

      assertEquals(1, config.getEnvironment().size());
      assertFalse(config.getEnvironment().containsKey("HOME"));
    }

    @Test
    @DisplayName("should throw when removing null environment variable")
    void shouldThrowWhenRemovingNullEnvironment() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiConfig.builder().withoutEnvironment(null));
    }

    @Test
    @DisplayName("should throw when removing empty environment variable")
    void shouldThrowWhenRemovingEmptyEnvironment() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiConfig.builder().withoutEnvironment(""));
    }

    @Test
    @DisplayName("should clear all environment variables")
    void shouldClearAllEnvironmentVariables() {
      WasiConfig config =
          WasiConfig.builder().withEnvironment("HOME", "/app").clearEnvironment().build();

      assertTrue(config.getEnvironment().isEmpty());
    }

    @Test
    @DisplayName("environment should be immutable")
    void environmentShouldBeImmutable() {
      WasiConfig config = WasiConfig.builder().withEnvironment("KEY", "value").build();

      assertThrows(
          UnsupportedOperationException.class, () -> config.getEnvironment().put("NEW", "entry"));
    }
  }

  @Nested
  @DisplayName("Arguments Configuration")
  class ArgumentsConfiguration {

    @Test
    @DisplayName("should add single argument")
    void shouldAddSingleArgument() {
      WasiConfig config = WasiConfig.builder().withArgument("--verbose").build();

      assertEquals(1, config.getArguments().size());
      assertEquals("--verbose", config.getArguments().get(0));
    }

    @Test
    @DisplayName("should set arguments list")
    void shouldSetArgumentsList() {
      List<String> args = Arrays.asList("--verbose", "--debug");

      WasiConfig config = WasiConfig.builder().withArguments(args).build();

      assertEquals(2, config.getArguments().size());
      assertEquals("--verbose", config.getArguments().get(0));
      assertEquals("--debug", config.getArguments().get(1));
    }

    @Test
    @DisplayName("should throw when argument is null")
    void shouldThrowWhenArgumentIsNull() {
      assertThrows(IllegalArgumentException.class, () -> WasiConfig.builder().withArgument(null));
    }

    @Test
    @DisplayName("should throw when arguments list is null")
    void shouldThrowWhenArgumentsListIsNull() {
      assertThrows(IllegalArgumentException.class, () -> WasiConfig.builder().withArguments(null));
    }

    @Test
    @DisplayName("should clear arguments")
    void shouldClearArguments() {
      WasiConfig config = WasiConfig.builder().withArgument("--verbose").clearArguments().build();

      assertTrue(config.getArguments().isEmpty());
    }

    @Test
    @DisplayName("arguments should be immutable")
    void argumentsShouldBeImmutable() {
      WasiConfig config = WasiConfig.builder().withArgument("--flag").build();

      assertThrows(UnsupportedOperationException.class, () -> config.getArguments().add("new"));
    }
  }

  @Nested
  @DisplayName("Preopen Directory Configuration")
  class PreopenDirectoryConfiguration {

    @Test
    @DisplayName("should add preopen directory")
    void shouldAddPreopenDirectory() {
      Path hostPath = Paths.get("/tmp");

      WasiConfig config = WasiConfig.builder().withPreopenDirectory("/sandbox", hostPath).build();

      assertEquals(1, config.getPreopenDirectories().size());
      assertEquals(hostPath, config.getPreopenDirectories().get("/sandbox"));
    }

    @Test
    @DisplayName("should throw when guest path is null")
    void shouldThrowWhenGuestPathIsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiConfig.builder().withPreopenDirectory(null, Paths.get("/tmp")));
    }

    @Test
    @DisplayName("should throw when guest path is empty")
    void shouldThrowWhenGuestPathIsEmpty() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiConfig.builder().withPreopenDirectory("", Paths.get("/tmp")));
    }

    @Test
    @DisplayName("should throw when host path is null")
    void shouldThrowWhenHostPathIsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiConfig.builder().withPreopenDirectory("/sandbox", (Path) null));
    }

    @Test
    @DisplayName("should remove preopen directory")
    void shouldRemovePreopenDirectory() {
      WasiConfig config =
          WasiConfig.builder()
              .withPreopenDirectory("/a", Paths.get("/tmp"))
              .withPreopenDirectory("/b", Paths.get("/var"))
              .withoutPreopenDirectory("/a")
              .build();

      assertEquals(1, config.getPreopenDirectories().size());
      assertFalse(config.getPreopenDirectories().containsKey("/a"));
    }

    @Test
    @DisplayName("should clear preopen directories")
    void shouldClearPreopenDirectories() {
      WasiConfig config =
          WasiConfig.builder()
              .withPreopenDirectory("/sandbox", Paths.get("/tmp"))
              .clearPreopenDirectories()
              .build();

      assertTrue(config.getPreopenDirectories().isEmpty());
    }

    @Test
    @DisplayName("preopen directories should be immutable")
    void preopenDirectoriesShouldBeImmutable() {
      WasiConfig config =
          WasiConfig.builder().withPreopenDirectory("/x", Paths.get("/tmp")).build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> config.getPreopenDirectories().put("/new", Paths.get("/")));
    }
  }

  @Nested
  @DisplayName("Working Directory Configuration")
  class WorkingDirectoryConfiguration {

    @Test
    @DisplayName("should set working directory")
    void shouldSetWorkingDirectory() {
      WasiConfig config = WasiConfig.builder().withWorkingDirectory("/app").build();

      assertTrue(config.getWorkingDirectory().isPresent());
      assertEquals("/app", config.getWorkingDirectory().get());
    }

    @Test
    @DisplayName("should throw when working directory is null")
    void shouldThrowWhenWorkingDirectoryIsNull() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiConfig.builder().withWorkingDirectory(null));
    }

    @Test
    @DisplayName("should throw when working directory is empty")
    void shouldThrowWhenWorkingDirectoryIsEmpty() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiConfig.builder().withWorkingDirectory(""));
    }

    @Test
    @DisplayName("should remove working directory")
    void shouldRemoveWorkingDirectory() {
      WasiConfig config =
          WasiConfig.builder().withWorkingDirectory("/app").withoutWorkingDirectory().build();

      assertFalse(config.getWorkingDirectory().isPresent());
    }
  }

  @Nested
  @DisplayName("Timeout Configuration")
  class TimeoutConfiguration {

    @Test
    @DisplayName("should set execution timeout")
    void shouldSetExecutionTimeout() {
      Duration timeout = Duration.ofSeconds(30);

      WasiConfig config = WasiConfig.builder().withExecutionTimeout(timeout).build();

      assertTrue(config.getExecutionTimeout().isPresent());
      assertEquals(timeout, config.getExecutionTimeout().get());
    }

    @Test
    @DisplayName("should throw when execution timeout is null")
    void shouldThrowWhenExecutionTimeoutIsNull() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiConfig.builder().withExecutionTimeout(null));
    }

    @Test
    @DisplayName("should throw when execution timeout is negative")
    void shouldThrowWhenExecutionTimeoutIsNegative() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiConfig.builder().withExecutionTimeout(Duration.ofSeconds(-1)));
    }

    @Test
    @DisplayName("should remove execution timeout")
    void shouldRemoveExecutionTimeout() {
      WasiConfig config =
          WasiConfig.builder()
              .withExecutionTimeout(Duration.ofSeconds(30))
              .withoutExecutionTimeout()
              .build();

      assertFalse(config.getExecutionTimeout().isPresent());
    }
  }

  @Nested
  @DisplayName("WASI Version Configuration")
  class WasiVersionConfiguration {

    @Test
    @DisplayName("should default to PREVIEW_1")
    void shouldDefaultToPreview1() {
      WasiConfig config = WasiConfig.builder().build();

      assertEquals(WasiVersion.PREVIEW_1, config.getWasiVersion());
    }

    @Test
    @DisplayName("should set WASI version")
    void shouldSetWasiVersion() {
      WasiConfig config = WasiConfig.builder().withWasiVersion(WasiVersion.PREVIEW_2).build();

      assertEquals(WasiVersion.PREVIEW_2, config.getWasiVersion());
    }

    @Test
    @DisplayName("should throw when WASI version is null")
    void shouldThrowWhenWasiVersionIsNull() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiConfig.builder().withWasiVersion(null));
    }
  }

  @Nested
  @DisplayName("Async Operation Configuration")
  class AsyncOperationConfiguration {

    @Test
    @DisplayName("should set async operations enabled")
    void shouldSetAsyncOperationsEnabled() {
      WasiConfig config = WasiConfig.builder().withAsyncOperations(true).build();

      assertTrue(config instanceof DefaultWasiConfig);
      assertTrue(((DefaultWasiConfig) config).isAsyncOperationsEnabled());
    }

    @Test
    @DisplayName("should set max async operations")
    void shouldSetMaxAsyncOperations() {
      WasiConfig config = WasiConfig.builder().withMaxAsyncOperations(10).build();

      assertTrue(config instanceof DefaultWasiConfig);
      assertTrue(((DefaultWasiConfig) config).getMaxAsyncOperations().isPresent());
      assertEquals(10, ((DefaultWasiConfig) config).getMaxAsyncOperations().get().intValue());
    }

    @Test
    @DisplayName("should throw when max async operations is zero")
    void shouldThrowWhenMaxAsyncOperationsIsZero() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiConfig.builder().withMaxAsyncOperations(0));
    }

    @Test
    @DisplayName("should throw when max async operations is negative")
    void shouldThrowWhenMaxAsyncOperationsIsNegative() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiConfig.builder().withMaxAsyncOperations(-1));
    }

    @Test
    @DisplayName("should remove max async operations")
    void shouldRemoveMaxAsyncOperations() {
      WasiConfig config =
          WasiConfig.builder().withMaxAsyncOperations(10).withoutMaxAsyncOperations().build();

      assertTrue(config instanceof DefaultWasiConfig);
      assertFalse(((DefaultWasiConfig) config).getMaxAsyncOperations().isPresent());
    }

    @Test
    @DisplayName("should set async operation timeout")
    void shouldSetAsyncOperationTimeout() {
      Duration timeout = Duration.ofSeconds(5);

      WasiConfig config = WasiConfig.builder().withAsyncOperationTimeout(timeout).build();

      assertTrue(config instanceof DefaultWasiConfig);
      assertTrue(((DefaultWasiConfig) config).getAsyncOperationTimeout().isPresent());
      assertEquals(timeout, ((DefaultWasiConfig) config).getAsyncOperationTimeout().get());
    }

    @Test
    @DisplayName("should remove async operation timeout")
    void shouldRemoveAsyncOperationTimeout() {
      WasiConfig config =
          WasiConfig.builder()
              .withAsyncOperationTimeout(Duration.ofSeconds(5))
              .withoutAsyncOperationTimeout()
              .build();

      assertTrue(config instanceof DefaultWasiConfig);
      assertFalse(((DefaultWasiConfig) config).getAsyncOperationTimeout().isPresent());
    }
  }

  @Nested
  @DisplayName("Validation and Strict Mode")
  class ValidationAndStrictMode {

    @Test
    @DisplayName("should enable validation")
    void shouldEnableValidation() {
      WasiConfig config = WasiConfig.builder().withValidation(true).build();

      assertTrue(config.isValidationEnabled());
    }

    @Test
    @DisplayName("should enable strict mode")
    void shouldEnableStrictMode() {
      WasiConfig config = WasiConfig.builder().withStrictMode(true).build();

      assertTrue(config.isStrictModeEnabled());
    }
  }

  @Nested
  @DisplayName("toBuilder Round-Trip")
  class ToBuilderRoundTrip {

    @Test
    @DisplayName("should preserve all settings through toBuilder")
    void shouldPreserveAllSettingsThroughToBuilder() {
      WasiConfig original =
          WasiConfig.builder()
              .withEnvironment("KEY", "value")
              .withArgument("--flag")
              .withPreopenDirectory("/guest", Paths.get("/tmp"))
              .withWorkingDirectory("/app")
              .withExecutionTimeout(Duration.ofSeconds(30))
              .withValidation(true)
              .withStrictMode(true)
              .withWasiVersion(WasiVersion.PREVIEW_2)
              .withAsyncOperations(true)
              .withMaxAsyncOperations(5)
              .withAsyncOperationTimeout(Duration.ofSeconds(10))
              .build();

      WasiConfig rebuilt = original.toBuilder().build();

      assertEquals(original.getEnvironment(), rebuilt.getEnvironment());
      assertEquals(original.getArguments(), rebuilt.getArguments());
      assertEquals(original.getPreopenDirectories(), rebuilt.getPreopenDirectories());
      assertEquals(original.getWorkingDirectory(), rebuilt.getWorkingDirectory());
      assertEquals(original.getExecutionTimeout(), rebuilt.getExecutionTimeout());
      assertEquals(original.isValidationEnabled(), rebuilt.isValidationEnabled());
      assertEquals(original.isStrictModeEnabled(), rebuilt.isStrictModeEnabled());
      assertEquals(original.getWasiVersion(), rebuilt.getWasiVersion());
    }
  }

  @Nested
  @DisplayName("Validate Method")
  class ValidateMethod {

    @Test
    @DisplayName("valid config should pass validation")
    void validConfigShouldPassValidation() {
      WasiConfig config =
          WasiConfig.builder()
              .withEnvironment("KEY", "value")
              .withArgument("arg")
              .withExecutionTimeout(Duration.ofSeconds(30))
              .build();

      config.validate(); // should not throw
    }

    @Test
    @DisplayName("should pass validation for empty config")
    void shouldPassValidationForEmptyConfig() {
      WasiConfig.defaultConfig().validate(); // should not throw
    }
  }

  @Nested
  @DisplayName("Fluent Chaining")
  class FluentChaining {

    @Test
    @DisplayName("all builder methods should return the builder for chaining")
    void allBuilderMethodsShouldReturnBuilderForChaining() {
      WasiConfigBuilder builder = WasiConfig.builder();

      WasiConfigBuilder result =
          builder
              .withEnvironment("KEY", "value")
              .withArgument("arg")
              .withWorkingDirectory("/app")
              .withExecutionTimeout(Duration.ofSeconds(30))
              .withValidation(true)
              .withStrictMode(true)
              .withWasiVersion(WasiVersion.PREVIEW_2)
              .withAsyncOperations(true)
              .withMaxAsyncOperations(5)
              .withAsyncOperationTimeout(Duration.ofSeconds(10));

      assertNotNull(result);
      assertNotNull(result.build());
    }
  }
}
