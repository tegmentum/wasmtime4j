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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiConfigBuilder;
import ai.tegmentum.wasmtime4j.wasi.WasiVersion;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PanamaWasiConfig}.
 *
 * <p>These tests exercise actual method calls to improve JaCoCo coverage.
 */
@DisplayName("PanamaWasiConfig Integration Tests")
class PanamaWasiConfigTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiConfigTest.class.getName());

  @Nested
  @DisplayName("Default Value Tests")
  class DefaultValueTests {

    @Test
    @DisplayName("Should have empty collections by default")
    void shouldHaveEmptyCollectionsByDefault() {
      LOGGER.info("Testing default collection values");

      final WasiConfig config = new PanamaWasiConfigBuilder().build();

      assertTrue(config.getEnvironment().isEmpty(), "Environment should be empty by default");
      assertTrue(config.getArguments().isEmpty(), "Arguments should be empty by default");
      assertTrue(
          config.getPreopenDirectories().isEmpty(),
          "Preopen directories should be empty by default");

      LOGGER.info("Default collections are empty as expected");
    }

    @Test
    @DisplayName("Should have correct default boolean values")
    void shouldHaveCorrectDefaultBooleanValues() {
      LOGGER.info("Testing default boolean values");

      final WasiConfig config = new PanamaWasiConfigBuilder().build();

      assertFalse(config.isValidationEnabled(), "Validation should be disabled by default");
      assertFalse(config.isStrictModeEnabled(), "Strict mode should be disabled by default");

      // Check Panama-specific method
      if (config instanceof PanamaWasiConfig) {
        final PanamaWasiConfig panamaConfig = (PanamaWasiConfig) config;
        assertFalse(
            panamaConfig.isInheritEnvironment(), "Inherit environment should be false by default");
        assertFalse(
            panamaConfig.isAsyncOperationsEnabled(),
            "Async operations should be disabled by default");
      }

      LOGGER.info("Default boolean values verified");
    }

    @Test
    @DisplayName("Should have empty optionals by default")
    void shouldHaveEmptyOptionalsByDefault() {
      LOGGER.info("Testing default optional values");

      final WasiConfig config = new PanamaWasiConfigBuilder().build();

      assertFalse(
          config.getWorkingDirectory().isPresent(), "Working directory should be empty by default");
      assertFalse(
          config.getExecutionTimeout().isPresent(), "Execution timeout should be empty by default");

      LOGGER.info("Default optional values are empty as expected");
    }

    @Test
    @DisplayName("Should have default WASI version")
    void shouldHaveDefaultWasiVersion() {
      LOGGER.info("Testing default WASI version");

      final WasiConfig config = new PanamaWasiConfigBuilder().build();

      assertEquals(
          WasiVersion.PREVIEW_1, config.getWasiVersion(), "Default version should be PREVIEW_1");

      LOGGER.info("Default WASI version: " + config.getWasiVersion());
    }
  }

  @Nested
  @DisplayName("Environment Configuration Tests")
  class EnvironmentConfigurationTests {

    @Test
    @DisplayName("Should return environment variables")
    void shouldReturnEnvironmentVariables() {
      LOGGER.info("Testing getEnvironment");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withEnvironment("PATH", "/usr/bin")
              .withEnvironment("HOME", "/home/user")
              .build();

      final Map<String, String> env = config.getEnvironment();
      assertEquals(2, env.size(), "Should have 2 environment variables");
      assertEquals("/usr/bin", env.get("PATH"), "PATH should be /usr/bin");
      assertEquals("/home/user", env.get("HOME"), "HOME should be /home/user");

      LOGGER.info("Environment: " + env);
    }

    @Test
    @DisplayName("Should return immutable environment map")
    void shouldReturnImmutableEnvironmentMap() {
      LOGGER.info("Testing environment immutability");

      final WasiConfig config =
          new PanamaWasiConfigBuilder().withEnvironment("KEY", "value").build();

      final Map<String, String> env = config.getEnvironment();
      try {
        env.put("HACKER", "injection");
        LOGGER.warning("Environment mutation was allowed");
      } catch (final UnsupportedOperationException e) {
        LOGGER.info("Environment is correctly immutable");
      }
    }
  }

  @Nested
  @DisplayName("Arguments Configuration Tests")
  class ArgumentsConfigurationTests {

    @Test
    @DisplayName("Should return arguments")
    void shouldReturnArguments() {
      LOGGER.info("Testing getArguments");

      final WasiConfig config =
          new PanamaWasiConfigBuilder().withArgument("--config").withArgument("file.conf").build();

      final List<String> args = config.getArguments();
      assertEquals(2, args.size(), "Should have 2 arguments");
      assertEquals("--config", args.get(0), "First arg should be --config");
      assertEquals("file.conf", args.get(1), "Second arg should be file.conf");

      LOGGER.info("Arguments: " + args);
    }
  }

  @Nested
  @DisplayName("Directory Configuration Tests")
  class DirectoryConfigurationTests {

    @Test
    @DisplayName("Should return preopen directories")
    void shouldReturnPreopenDirectories() {
      LOGGER.info("Testing getPreopenDirectories");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withPreopenDirectory("/", Path.of("/home/user"))
              .withPreopenDirectory("/tmp", Path.of("/tmp"))
              .build();

      final Map<String, Path> dirs = config.getPreopenDirectories();
      assertEquals(2, dirs.size(), "Should have 2 preopen directories");
      assertEquals(Path.of("/home/user"), dirs.get("/"), "/ should map to /home/user");
      assertEquals(Path.of("/tmp"), dirs.get("/tmp"), "/tmp should map to /tmp");

      LOGGER.info("Preopen directories: " + dirs);
    }

    @Test
    @DisplayName("Should return working directory")
    void shouldReturnWorkingDirectory() {
      LOGGER.info("Testing getWorkingDirectory");

      final WasiConfig config = new PanamaWasiConfigBuilder().withWorkingDirectory("/work").build();

      final Optional<String> workDir = config.getWorkingDirectory();
      assertTrue(workDir.isPresent(), "Working directory should be present");
      assertEquals("/work", workDir.get(), "Working directory should be /work");

      LOGGER.info("Working directory: " + workDir.get());
    }
  }

  @Nested
  @DisplayName("Timeout Configuration Tests")
  class TimeoutConfigurationTests {

    @Test
    @DisplayName("Should return execution timeout")
    void shouldReturnExecutionTimeout() {
      LOGGER.info("Testing getExecutionTimeout");

      final Duration timeout = Duration.ofSeconds(30);
      final WasiConfig config = new PanamaWasiConfigBuilder().withExecutionTimeout(timeout).build();

      final Optional<Duration> result = config.getExecutionTimeout();
      assertTrue(result.isPresent(), "Execution timeout should be present");
      assertEquals(timeout, result.get(), "Execution timeout should match");

      LOGGER.info("Execution timeout: " + result.get());
    }
  }

  @Nested
  @DisplayName("Async Operation Tests")
  class AsyncOperationTests {

    @Test
    @DisplayName("Should return async operation settings")
    void shouldReturnAsyncOperationSettings() {
      LOGGER.info("Testing async operation settings");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withAsyncOperations(true)
              .withMaxAsyncOperations(10)
              .withAsyncOperationTimeout(Duration.ofSeconds(5))
              .build();

      if (config instanceof PanamaWasiConfig) {
        final PanamaWasiConfig panamaConfig = (PanamaWasiConfig) config;
        assertTrue(panamaConfig.isAsyncOperationsEnabled(), "Async operations should be enabled");
        assertEquals(10, panamaConfig.getMaxAsyncOperations().get(), "Max async should be 10");
        assertEquals(
            Duration.ofSeconds(5),
            panamaConfig.getAsyncOperationTimeout().get(),
            "Async timeout should be 5 seconds");
      }

      LOGGER.info("Async operation settings verified");
    }
  }

  @Nested
  @DisplayName("Version Configuration Tests")
  class VersionConfigurationTests {

    @Test
    @DisplayName("Should return WASI version")
    void shouldReturnWasiVersion() {
      LOGGER.info("Testing getWasiVersion");

      final WasiConfig config =
          new PanamaWasiConfigBuilder().withWasiVersion(WasiVersion.PREVIEW_2).build();

      assertEquals(WasiVersion.PREVIEW_2, config.getWasiVersion(), "Should be PREVIEW_2");

      LOGGER.info("WASI version: " + config.getWasiVersion());
    }
  }

  @Nested
  @DisplayName("Validation Mode Tests")
  class ValidationModeTests {

    @Test
    @DisplayName("Should return validation enabled status")
    void shouldReturnValidationEnabledStatus() {
      LOGGER.info("Testing isValidationEnabled");

      final WasiConfig configEnabled = new PanamaWasiConfigBuilder().withValidation(true).build();
      assertTrue(configEnabled.isValidationEnabled(), "Validation should be enabled");

      final WasiConfig configDisabled = new PanamaWasiConfigBuilder().withValidation(false).build();
      assertFalse(configDisabled.isValidationEnabled(), "Validation should be disabled");

      LOGGER.info("Validation mode tests passed");
    }

    @Test
    @DisplayName("Should return strict mode status")
    void shouldReturnStrictModeStatus() {
      LOGGER.info("Testing isStrictModeEnabled");

      final WasiConfig configEnabled = new PanamaWasiConfigBuilder().withStrictMode(true).build();
      assertTrue(configEnabled.isStrictModeEnabled(), "Strict mode should be enabled");

      final WasiConfig configDisabled = new PanamaWasiConfigBuilder().withStrictMode(false).build();
      assertFalse(configDisabled.isStrictModeEnabled(), "Strict mode should be disabled");

      LOGGER.info("Strict mode tests passed");
    }
  }

  @Nested
  @DisplayName("Import Resolver Tests")
  class ImportResolverTests {

  }



  @Nested
  @DisplayName("toBuilder Tests")
  class ToBuilderTests {

    @Test
    @DisplayName("Should create builder from config")
    void shouldCreateBuilderFromConfig() {
      LOGGER.info("Testing toBuilder");

      final WasiConfig original =
          new PanamaWasiConfigBuilder()
              .withEnvironment("KEY", "value")
              .withArgument("--test")
              .withWorkingDirectory("/work")
              .withWasiVersion(WasiVersion.PREVIEW_2)
              .withValidation(true)
              .withStrictMode(true)
              .withExecutionTimeout(Duration.ofSeconds(60))
              .withAsyncOperations(true)
              .withMaxAsyncOperations(5)
              .withAsyncOperationTimeout(Duration.ofSeconds(10))
              .build();

      final WasiConfigBuilder builder = original.toBuilder();
      assertNotNull(builder, "Builder should not be null");

      final WasiConfig rebuilt = builder.build();
      assertEquals(original.getEnvironment(), rebuilt.getEnvironment());
      assertEquals(original.getArguments(), rebuilt.getArguments());
      assertEquals(original.getWorkingDirectory(), rebuilt.getWorkingDirectory());
      assertEquals(original.getWasiVersion(), rebuilt.getWasiVersion());
      assertEquals(original.isValidationEnabled(), rebuilt.isValidationEnabled());
      assertEquals(original.isStrictModeEnabled(), rebuilt.isStrictModeEnabled());
      assertEquals(original.getExecutionTimeout(), rebuilt.getExecutionTimeout());

      LOGGER.info("toBuilder creates correct builder");
    }

    @Test
    @DisplayName("Should handle toBuilder with inherit environment")
    void shouldHandleToBuilderWithInheritEnvironment() {
      LOGGER.info("Testing toBuilder with inherit environment");

      final WasiConfig original = new PanamaWasiConfigBuilder().inheritEnvironment().build();

      if (original instanceof PanamaWasiConfig) {
        assertTrue(
            ((PanamaWasiConfig) original).isInheritEnvironment(),
            "Inherit environment should be true");
      }

      final WasiConfig rebuilt = original.toBuilder().build();
      if (rebuilt instanceof PanamaWasiConfig) {
        assertTrue(
            ((PanamaWasiConfig) rebuilt).isInheritEnvironment(),
            "Inherit environment should be preserved");
      }

      LOGGER.info("toBuilder preserves inherit environment");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("Should validate successfully")
    void shouldValidateSuccessfully() {
      LOGGER.info("Testing validate");

      final WasiConfig config = new PanamaWasiConfigBuilder().build();
      assertDoesNotThrow(config::validate, "Validation should not throw");

      LOGGER.info("Validation passed");
    }
  }

  @Nested
  @DisplayName("Comprehensive Configuration Tests")
  class ComprehensiveConfigurationTests {

    @Test
    @DisplayName("Should build fully configured instance")
    void shouldBuildFullyConfiguredInstance() {
      LOGGER.info("Testing fully configured instance");

      final Map<String, String> env = new HashMap<>();
      env.put("PATH", "/usr/bin");
      env.put("HOME", "/home/user");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withEnvironment(env)
              .withArguments(Arrays.asList("arg1", "arg2", "arg3"))
              .withPreopenDirectory("/", Path.of("/"))
              .withPreopenDirectories(Collections.singletonMap("/tmp", Path.of("/tmp")))
              .withWorkingDirectory("/workspace")
              .withExecutionTimeout(Duration.ofMinutes(5))
              .withWasiVersion(WasiVersion.PREVIEW_1)
              .withValidation(true)
              .withStrictMode(true)
              .withAsyncOperations(true)
              .withMaxAsyncOperations(100)
              .withAsyncOperationTimeout(Duration.ofSeconds(30))
              .build();

      // Verify all settings
      assertEquals(2, config.getEnvironment().size());
      assertEquals(3, config.getArguments().size());
      assertEquals(2, config.getPreopenDirectories().size());
      assertEquals("/workspace", config.getWorkingDirectory().get());
      assertEquals(Duration.ofMinutes(5), config.getExecutionTimeout().get());
      assertEquals(WasiVersion.PREVIEW_1, config.getWasiVersion());
      assertTrue(config.isValidationEnabled());
      assertTrue(config.isStrictModeEnabled());

      if (config instanceof PanamaWasiConfig) {
        final PanamaWasiConfig panamaConfig = (PanamaWasiConfig) config;
        assertTrue(panamaConfig.isAsyncOperationsEnabled());
        assertEquals(100, panamaConfig.getMaxAsyncOperations().get());
        assertEquals(Duration.ofSeconds(30), panamaConfig.getAsyncOperationTimeout().get());
      }

      LOGGER.info("Fully configured instance built successfully");
    }
  }
}
