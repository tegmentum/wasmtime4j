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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiConfigBuilder;
import ai.tegmentum.wasmtime4j.wasi.WasiImportResolver;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceLimits;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceLimitsBuilder;
import ai.tegmentum.wasmtime4j.wasi.WasiSecurityPolicy;
import ai.tegmentum.wasmtime4j.wasi.WasiVersion;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PanamaWasiConfigBuilder}.
 *
 * <p>These tests exercise actual method calls to improve JaCoCo coverage.
 */
@DisplayName("PanamaWasiConfigBuilder Integration Tests")
class PanamaWasiConfigBuilderTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaWasiConfigBuilderTest.class.getName());

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create builder with default constructor")
    void shouldCreateBuilderWithDefaultConstructor() {
      LOGGER.info("Testing default constructor");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();
      assertNotNull(builder, "Builder should be created");

      final WasiConfig config = builder.build();
      assertNotNull(config, "Config should be created from default builder");
      assertTrue(config.getEnvironment().isEmpty(), "Environment should be empty by default");
      assertTrue(config.getArguments().isEmpty(), "Arguments should be empty by default");

      LOGGER.info("Default constructor test passed");
    }
  }

  @Nested
  @DisplayName("Environment Method Tests")
  class EnvironmentMethodTests {

    @Test
    @DisplayName("Should add single environment variable")
    void shouldAddSingleEnvironmentVariable() {
      LOGGER.info("Testing withEnvironment(name, value)");

      final WasiConfigBuilder builder = new PanamaWasiConfigBuilder();
      final WasiConfigBuilder result = builder.withEnvironment("PATH", "/usr/bin");

      assertNotNull(result, "Should return builder for chaining");
      assertEquals(builder, result, "Should return same builder");

      final WasiConfig config = builder.build();
      assertEquals("/usr/bin", config.getEnvironment().get("PATH"));

      LOGGER.info("Single environment variable added");
    }

    @Test
    @DisplayName("Should add multiple environment variables")
    void shouldAddMultipleEnvironmentVariables() {
      LOGGER.info("Testing withEnvironment(name, value) multiple times");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withEnvironment("PATH", "/usr/bin")
              .withEnvironment("HOME", "/home/user")
              .withEnvironment("USER", "testuser")
              .build();

      assertEquals(3, config.getEnvironment().size());
      assertEquals("/usr/bin", config.getEnvironment().get("PATH"));
      assertEquals("/home/user", config.getEnvironment().get("HOME"));
      assertEquals("testuser", config.getEnvironment().get("USER"));

      LOGGER.info("Multiple environment variables added");
    }

    @Test
    @DisplayName("Should throw on null environment name")
    void shouldThrowOnNullEnvironmentName() {
      LOGGER.info("Testing withEnvironment with null name");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> builder.withEnvironment(null, "value"),
              "Should throw on null name");

      assertTrue(
          exception.getMessage().contains("name cannot be null"),
          "Exception message should mention null name");

      LOGGER.info("Null name throws exception: " + exception.getMessage());
    }

    @Test
    @DisplayName("Should add environment from map")
    void shouldAddEnvironmentFromMap() {
      LOGGER.info("Testing withEnvironment(map)");

      final Map<String, String> env = new HashMap<>();
      env.put("KEY1", "value1");
      env.put("KEY2", "value2");

      final WasiConfig config = new PanamaWasiConfigBuilder().withEnvironment(env).build();

      assertEquals(2, config.getEnvironment().size());
      assertEquals("value1", config.getEnvironment().get("KEY1"));
      assertEquals("value2", config.getEnvironment().get("KEY2"));

      LOGGER.info("Environment map added");
    }

    @Test
    @DisplayName("Should throw on null environment map")
    void shouldThrowOnNullEnvironmentMap() {
      LOGGER.info("Testing withEnvironment with null map");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withEnvironment((Map<String, String>) null),
          "Should throw on null map");

      LOGGER.info("Null map throws exception");
    }

    @Test
    @DisplayName("Should remove environment variable")
    void shouldRemoveEnvironmentVariable() {
      LOGGER.info("Testing withoutEnvironment");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withEnvironment("KEY1", "value1")
              .withEnvironment("KEY2", "value2")
              .withoutEnvironment("KEY1")
              .build();

      assertEquals(1, config.getEnvironment().size());
      assertFalse(config.getEnvironment().containsKey("KEY1"));
      assertTrue(config.getEnvironment().containsKey("KEY2"));

      LOGGER.info("Environment variable removed");
    }

    @Test
    @DisplayName("Should throw on null environment name for removal")
    void shouldThrowOnNullEnvironmentNameForRemoval() {
      LOGGER.info("Testing withoutEnvironment with null name");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withoutEnvironment(null),
          "Should throw on null name");

      LOGGER.info("Null name for removal throws exception");
    }

    @Test
    @DisplayName("Should throw on empty environment name for removal")
    void shouldThrowOnEmptyEnvironmentNameForRemoval() {
      LOGGER.info("Testing withoutEnvironment with empty name");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withoutEnvironment(""),
          "Should throw on empty name");

      LOGGER.info("Empty name for removal throws exception");
    }

    @Test
    @DisplayName("Should clear all environment variables")
    void shouldClearAllEnvironmentVariables() {
      LOGGER.info("Testing clearEnvironment");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withEnvironment("KEY1", "value1")
              .withEnvironment("KEY2", "value2")
              .clearEnvironment()
              .build();

      assertTrue(config.getEnvironment().isEmpty(), "Environment should be empty");

      LOGGER.info("Environment cleared");
    }
  }

  @Nested
  @DisplayName("Arguments Method Tests")
  class ArgumentsMethodTests {

    @Test
    @DisplayName("Should add single argument")
    void shouldAddSingleArgument() {
      LOGGER.info("Testing withArgument");

      final WasiConfig config = new PanamaWasiConfigBuilder().withArgument("--config").build();

      assertEquals(1, config.getArguments().size());
      assertEquals("--config", config.getArguments().get(0));

      LOGGER.info("Single argument added");
    }

    @Test
    @DisplayName("Should add multiple arguments")
    void shouldAddMultipleArguments() {
      LOGGER.info("Testing withArgument multiple times");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withArgument("--config")
              .withArgument("file.conf")
              .withArgument("--verbose")
              .build();

      assertEquals(3, config.getArguments().size());
      assertEquals("--config", config.getArguments().get(0));
      assertEquals("file.conf", config.getArguments().get(1));
      assertEquals("--verbose", config.getArguments().get(2));

      LOGGER.info("Multiple arguments added");
    }

    @Test
    @DisplayName("Should throw on null argument")
    void shouldThrowOnNullArgument() {
      LOGGER.info("Testing withArgument with null");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withArgument(null),
          "Should throw on null argument");

      LOGGER.info("Null argument throws exception");
    }

    @Test
    @DisplayName("Should replace arguments with list")
    void shouldReplaceArgumentsWithList() {
      LOGGER.info("Testing withArguments(list)");

      final List<String> args = Arrays.asList("arg1", "arg2", "arg3");

      final WasiConfig config =
          new PanamaWasiConfigBuilder().withArgument("old-arg").withArguments(args).build();

      assertEquals(3, config.getArguments().size());
      assertEquals("arg1", config.getArguments().get(0));
      assertEquals("arg2", config.getArguments().get(1));
      assertEquals("arg3", config.getArguments().get(2));

      LOGGER.info("Arguments replaced with list");
    }

    @Test
    @DisplayName("Should throw on null arguments list")
    void shouldThrowOnNullArgumentsList() {
      LOGGER.info("Testing withArguments with null list");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withArguments(null),
          "Should throw on null list");

      LOGGER.info("Null arguments list throws exception");
    }

    @Test
    @DisplayName("Should clear all arguments")
    void shouldClearAllArguments() {
      LOGGER.info("Testing clearArguments");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withArgument("arg1")
              .withArgument("arg2")
              .clearArguments()
              .build();

      assertTrue(config.getArguments().isEmpty(), "Arguments should be empty");

      LOGGER.info("Arguments cleared");
    }
  }

  @Nested
  @DisplayName("Preopen Directory Method Tests")
  class PreopenDirectoryMethodTests {

    @Test
    @DisplayName("Should add preopen directory")
    void shouldAddPreopenDirectory() {
      LOGGER.info("Testing withPreopenDirectory");

      final WasiConfig config =
          new PanamaWasiConfigBuilder().withPreopenDirectory("/guest", Path.of("/host")).build();

      assertEquals(1, config.getPreopenDirectories().size());
      assertEquals(Path.of("/host"), config.getPreopenDirectories().get("/guest"));

      LOGGER.info("Preopen directory added");
    }

    @Test
    @DisplayName("Should throw on null guest path")
    void shouldThrowOnNullGuestPath() {
      LOGGER.info("Testing withPreopenDirectory with null guest path");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withPreopenDirectory(null, Path.of("/host")),
          "Should throw on null guest path");

      LOGGER.info("Null guest path throws exception");
    }

    @Test
    @DisplayName("Should throw on empty guest path")
    void shouldThrowOnEmptyGuestPath() {
      LOGGER.info("Testing withPreopenDirectory with empty guest path");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withPreopenDirectory("", Path.of("/host")),
          "Should throw on empty guest path");

      LOGGER.info("Empty guest path throws exception");
    }

    @Test
    @DisplayName("Should throw on null host path")
    void shouldThrowOnNullHostPath() {
      LOGGER.info("Testing withPreopenDirectory with null host path");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withPreopenDirectory("/guest", null),
          "Should throw on null host path");

      LOGGER.info("Null host path throws exception");
    }

    @Test
    @DisplayName("Should add preopen directories from map")
    void shouldAddPreopenDirectoriesFromMap() {
      LOGGER.info("Testing withPreopenDirectories(map)");

      final Map<String, Path> dirs = new HashMap<>();
      dirs.put("/", Path.of("/root"));
      dirs.put("/tmp", Path.of("/tmp"));

      final WasiConfig config = new PanamaWasiConfigBuilder().withPreopenDirectories(dirs).build();

      assertEquals(2, config.getPreopenDirectories().size());
      assertEquals(Path.of("/root"), config.getPreopenDirectories().get("/"));
      assertEquals(Path.of("/tmp"), config.getPreopenDirectories().get("/tmp"));

      LOGGER.info("Preopen directories map added");
    }

    @Test
    @DisplayName("Should throw on null directories map")
    void shouldThrowOnNullDirectoriesMap() {
      LOGGER.info("Testing withPreopenDirectories with null map");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withPreopenDirectories(null),
          "Should throw on null map");

      LOGGER.info("Null directories map throws exception");
    }

    @Test
    @DisplayName("Should remove preopen directory")
    void shouldRemovePreopenDirectory() {
      LOGGER.info("Testing withoutPreopenDirectory");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withPreopenDirectory("/", Path.of("/root"))
              .withPreopenDirectory("/tmp", Path.of("/tmp"))
              .withoutPreopenDirectory("/")
              .build();

      assertEquals(1, config.getPreopenDirectories().size());
      assertFalse(config.getPreopenDirectories().containsKey("/"));
      assertTrue(config.getPreopenDirectories().containsKey("/tmp"));

      LOGGER.info("Preopen directory removed");
    }

    @Test
    @DisplayName("Should throw on null path for removal")
    void shouldThrowOnNullPathForRemoval() {
      LOGGER.info("Testing withoutPreopenDirectory with null");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withoutPreopenDirectory(null),
          "Should throw on null path");

      LOGGER.info("Null path for removal throws exception");
    }

    @Test
    @DisplayName("Should throw on empty path for removal")
    void shouldThrowOnEmptyPathForRemoval() {
      LOGGER.info("Testing withoutPreopenDirectory with empty");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withoutPreopenDirectory(""),
          "Should throw on empty path");

      LOGGER.info("Empty path for removal throws exception");
    }

    @Test
    @DisplayName("Should clear all preopen directories")
    void shouldClearAllPreopenDirectories() {
      LOGGER.info("Testing clearPreopenDirectories");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withPreopenDirectory("/", Path.of("/root"))
              .withPreopenDirectory("/tmp", Path.of("/tmp"))
              .clearPreopenDirectories()
              .build();

      assertTrue(config.getPreopenDirectories().isEmpty(), "Directories should be empty");

      LOGGER.info("Preopen directories cleared");
    }
  }

  @Nested
  @DisplayName("Working Directory Method Tests")
  class WorkingDirectoryMethodTests {

    @Test
    @DisplayName("Should set working directory")
    void shouldSetWorkingDirectory() {
      LOGGER.info("Testing withWorkingDirectory");

      final WasiConfig config = new PanamaWasiConfigBuilder().withWorkingDirectory("/work").build();

      assertTrue(config.getWorkingDirectory().isPresent());
      assertEquals("/work", config.getWorkingDirectory().get());

      LOGGER.info("Working directory set");
    }

    @Test
    @DisplayName("Should throw on null working directory")
    void shouldThrowOnNullWorkingDirectory() {
      LOGGER.info("Testing withWorkingDirectory with null");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withWorkingDirectory(null),
          "Should throw on null directory");

      LOGGER.info("Null working directory throws exception");
    }

    @Test
    @DisplayName("Should throw on empty working directory")
    void shouldThrowOnEmptyWorkingDirectory() {
      LOGGER.info("Testing withWorkingDirectory with empty");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withWorkingDirectory(""),
          "Should throw on empty directory");

      LOGGER.info("Empty working directory throws exception");
    }

    @Test
    @DisplayName("Should clear working directory")
    void shouldClearWorkingDirectory() {
      LOGGER.info("Testing withoutWorkingDirectory");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withWorkingDirectory("/work")
              .withoutWorkingDirectory()
              .build();

      assertFalse(config.getWorkingDirectory().isPresent(), "Working directory should be empty");

      LOGGER.info("Working directory cleared");
    }
  }

  @Nested
  @DisplayName("Inherit Environment Tests")
  class InheritEnvironmentTests {

    @Test
    @DisplayName("Should enable inherit environment")
    void shouldEnableInheritEnvironment() {
      LOGGER.info("Testing inheritEnvironment");

      final WasiConfig config = new PanamaWasiConfigBuilder().inheritEnvironment().build();

      if (config instanceof PanamaWasiConfig) {
        assertTrue(
            ((PanamaWasiConfig) config).isInheritEnvironment(),
            "Inherit environment should be enabled");
      }

      LOGGER.info("Inherit environment enabled");
    }
  }

  @Nested
  @DisplayName("Memory Limit Method Tests")
  class MemoryLimitMethodTests {

    @Test
    @DisplayName("Should throw UnsupportedOperationException for withMemoryLimit")
    void shouldThrowUnsupportedForWithMemoryLimit() {
      LOGGER.info("Testing withMemoryLimit");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          UnsupportedOperationException.class,
          () -> builder.withMemoryLimit(1024L),
          "Should throw UnsupportedOperationException");

      LOGGER.info("withMemoryLimit throws UnsupportedOperationException");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for non-positive memory limit")
    void shouldThrowIllegalArgumentForNonPositiveMemoryLimit() {
      LOGGER.info("Testing withMemoryLimit with non-positive value");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withMemoryLimit(0),
          "Should throw IllegalArgumentException for zero");

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withMemoryLimit(-1),
          "Should throw IllegalArgumentException for negative");

      LOGGER.info("Non-positive memory limit throws IllegalArgumentException");
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for withoutMemoryLimit")
    void shouldThrowUnsupportedForWithoutMemoryLimit() {
      LOGGER.info("Testing withoutMemoryLimit");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          UnsupportedOperationException.class,
          () -> builder.withoutMemoryLimit(),
          "Should throw UnsupportedOperationException");

      LOGGER.info("withoutMemoryLimit throws UnsupportedOperationException");
    }
  }

  @Nested
  @DisplayName("Execution Timeout Method Tests")
  class ExecutionTimeoutMethodTests {

    @Test
    @DisplayName("Should set execution timeout")
    void shouldSetExecutionTimeout() {
      LOGGER.info("Testing withExecutionTimeout");

      final Duration timeout = Duration.ofSeconds(30);
      final WasiConfig config = new PanamaWasiConfigBuilder().withExecutionTimeout(timeout).build();

      assertTrue(config.getExecutionTimeout().isPresent());
      assertEquals(timeout, config.getExecutionTimeout().get());

      LOGGER.info("Execution timeout set");
    }

    @Test
    @DisplayName("Should throw on null timeout")
    void shouldThrowOnNullTimeout() {
      LOGGER.info("Testing withExecutionTimeout with null");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withExecutionTimeout(null),
          "Should throw on null timeout");

      LOGGER.info("Null timeout throws exception");
    }

    @Test
    @DisplayName("Should throw on negative timeout")
    void shouldThrowOnNegativeTimeout() {
      LOGGER.info("Testing withExecutionTimeout with negative value");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withExecutionTimeout(Duration.ofSeconds(-1)),
          "Should throw on negative timeout");

      LOGGER.info("Negative timeout throws exception");
    }

    @Test
    @DisplayName("Should clear execution timeout")
    void shouldClearExecutionTimeout() {
      LOGGER.info("Testing withoutExecutionTimeout");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withExecutionTimeout(Duration.ofSeconds(30))
              .withoutExecutionTimeout()
              .build();

      assertFalse(config.getExecutionTimeout().isPresent(), "Timeout should be empty");

      LOGGER.info("Execution timeout cleared");
    }
  }

  @Nested
  @DisplayName("Resource Limits Method Tests")
  class ResourceLimitsMethodTests {

    @Test
    @DisplayName("Should throw UnsupportedOperationException for withResourceLimits")
    void shouldThrowUnsupportedForWithResourceLimits() {
      LOGGER.info("Testing withResourceLimits");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          UnsupportedOperationException.class,
          () -> builder.withResourceLimits(new TestWasiResourceLimits()),
          "Should throw UnsupportedOperationException");

      LOGGER.info("withResourceLimits throws UnsupportedOperationException");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for null resource limits")
    void shouldThrowIllegalArgumentForNullResourceLimits() {
      LOGGER.info("Testing withResourceLimits with null");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withResourceLimits(null),
          "Should throw IllegalArgumentException for null");

      LOGGER.info("Null resource limits throws IllegalArgumentException");
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for withoutResourceLimits")
    void shouldThrowUnsupportedForWithoutResourceLimits() {
      LOGGER.info("Testing withoutResourceLimits");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          UnsupportedOperationException.class,
          () -> builder.withoutResourceLimits(),
          "Should throw UnsupportedOperationException");

      LOGGER.info("withoutResourceLimits throws UnsupportedOperationException");
    }
  }

  @Nested
  @DisplayName("Security Policy Method Tests")
  class SecurityPolicyMethodTests {

    @Test
    @DisplayName("Should set security policy")
    void shouldSetSecurityPolicy() {
      LOGGER.info("Testing withSecurityPolicy");

      final WasiSecurityPolicy policy = new TestWasiSecurityPolicy();
      final WasiConfig config = new PanamaWasiConfigBuilder().withSecurityPolicy(policy).build();

      assertTrue(config.getSecurityPolicy().isPresent());
      assertEquals(policy, config.getSecurityPolicy().get());

      LOGGER.info("Security policy set");
    }

    @Test
    @DisplayName("Should throw on null security policy")
    void shouldThrowOnNullSecurityPolicy() {
      LOGGER.info("Testing withSecurityPolicy with null");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withSecurityPolicy(null),
          "Should throw on null policy");

      LOGGER.info("Null security policy throws exception");
    }

    @Test
    @DisplayName("Should clear security policy")
    void shouldClearSecurityPolicy() {
      LOGGER.info("Testing withoutSecurityPolicy");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withSecurityPolicy(new TestWasiSecurityPolicy())
              .withoutSecurityPolicy()
              .build();

      assertFalse(config.getSecurityPolicy().isPresent(), "Security policy should be empty");

      LOGGER.info("Security policy cleared");
    }
  }

  @Nested
  @DisplayName("Import Resolver Method Tests")
  class ImportResolverMethodTests {

    @Test
    @DisplayName("Should add import resolver")
    void shouldAddImportResolver() {
      LOGGER.info("Testing withImportResolver");

      final WasiImportResolver resolver = new TestWasiImportResolver();
      final WasiConfig config =
          new PanamaWasiConfigBuilder().withImportResolver("test-interface", resolver).build();

      assertEquals(1, config.getImportResolvers().size());
      assertEquals(resolver, config.getImportResolvers().get("test-interface"));

      LOGGER.info("Import resolver added");
    }

    @Test
    @DisplayName("Should throw on null interface name")
    void shouldThrowOnNullInterfaceName() {
      LOGGER.info("Testing withImportResolver with null name");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withImportResolver(null, new TestWasiImportResolver()),
          "Should throw on null name");

      LOGGER.info("Null interface name throws exception");
    }

    @Test
    @DisplayName("Should throw on empty interface name")
    void shouldThrowOnEmptyInterfaceName() {
      LOGGER.info("Testing withImportResolver with empty name");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withImportResolver("", new TestWasiImportResolver()),
          "Should throw on empty name");

      LOGGER.info("Empty interface name throws exception");
    }

    @Test
    @DisplayName("Should throw on null resolver")
    void shouldThrowOnNullResolver() {
      LOGGER.info("Testing withImportResolver with null resolver");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withImportResolver("test", null),
          "Should throw on null resolver");

      LOGGER.info("Null resolver throws exception");
    }

    @Test
    @DisplayName("Should add import resolvers from map")
    void shouldAddImportResolversFromMap() {
      LOGGER.info("Testing withImportResolvers(map)");

      final Map<String, WasiImportResolver> resolvers = new HashMap<>();
      resolvers.put("interface1", new TestWasiImportResolver());
      resolvers.put("interface2", new TestWasiImportResolver());

      final WasiConfig config =
          new PanamaWasiConfigBuilder().withImportResolvers(resolvers).build();

      assertEquals(2, config.getImportResolvers().size());

      LOGGER.info("Import resolvers map added");
    }

    @Test
    @DisplayName("Should throw on null resolvers map")
    void shouldThrowOnNullResolversMap() {
      LOGGER.info("Testing withImportResolvers with null map");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withImportResolvers(null),
          "Should throw on null map");

      LOGGER.info("Null resolvers map throws exception");
    }

    @Test
    @DisplayName("Should remove import resolver")
    void shouldRemoveImportResolver() {
      LOGGER.info("Testing withoutImportResolver");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withImportResolver("interface1", new TestWasiImportResolver())
              .withImportResolver("interface2", new TestWasiImportResolver())
              .withoutImportResolver("interface1")
              .build();

      assertEquals(1, config.getImportResolvers().size());
      assertFalse(config.getImportResolvers().containsKey("interface1"));
      assertTrue(config.getImportResolvers().containsKey("interface2"));

      LOGGER.info("Import resolver removed");
    }

    @Test
    @DisplayName("Should throw on null interface name for removal")
    void shouldThrowOnNullInterfaceNameForRemoval() {
      LOGGER.info("Testing withoutImportResolver with null name");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withoutImportResolver(null),
          "Should throw on null name");

      LOGGER.info("Null interface name for removal throws exception");
    }

    @Test
    @DisplayName("Should throw on empty interface name for removal")
    void shouldThrowOnEmptyInterfaceNameForRemoval() {
      LOGGER.info("Testing withoutImportResolver with empty name");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withoutImportResolver(""),
          "Should throw on empty name");

      LOGGER.info("Empty interface name for removal throws exception");
    }

    @Test
    @DisplayName("Should clear all import resolvers")
    void shouldClearAllImportResolvers() {
      LOGGER.info("Testing clearImportResolvers");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withImportResolver("interface1", new TestWasiImportResolver())
              .withImportResolver("interface2", new TestWasiImportResolver())
              .clearImportResolvers()
              .build();

      assertTrue(config.getImportResolvers().isEmpty(), "Import resolvers should be empty");

      LOGGER.info("Import resolvers cleared");
    }
  }

  @Nested
  @DisplayName("Validation Mode Method Tests")
  class ValidationModeMethodTests {

    @Test
    @DisplayName("Should enable validation")
    void shouldEnableValidation() {
      LOGGER.info("Testing withValidation(true)");

      final WasiConfig config = new PanamaWasiConfigBuilder().withValidation(true).build();

      assertTrue(config.isValidationEnabled(), "Validation should be enabled");

      LOGGER.info("Validation enabled");
    }

    @Test
    @DisplayName("Should disable validation")
    void shouldDisableValidation() {
      LOGGER.info("Testing withValidation(false)");

      final WasiConfig config = new PanamaWasiConfigBuilder().withValidation(false).build();

      assertFalse(config.isValidationEnabled(), "Validation should be disabled");

      LOGGER.info("Validation disabled");
    }

    @Test
    @DisplayName("Should enable strict mode")
    void shouldEnableStrictMode() {
      LOGGER.info("Testing withStrictMode(true)");

      final WasiConfig config = new PanamaWasiConfigBuilder().withStrictMode(true).build();

      assertTrue(config.isStrictModeEnabled(), "Strict mode should be enabled");

      LOGGER.info("Strict mode enabled");
    }

    @Test
    @DisplayName("Should disable strict mode")
    void shouldDisableStrictMode() {
      LOGGER.info("Testing withStrictMode(false)");

      final WasiConfig config = new PanamaWasiConfigBuilder().withStrictMode(false).build();

      assertFalse(config.isStrictModeEnabled(), "Strict mode should be disabled");

      LOGGER.info("Strict mode disabled");
    }
  }

  @Nested
  @DisplayName("WASI Version Method Tests")
  class WasiVersionMethodTests {

    @Test
    @DisplayName("Should set WASI version to PREVIEW_1")
    void shouldSetWasiVersionToPreview1() {
      LOGGER.info("Testing withWasiVersion(PREVIEW_1)");

      final WasiConfig config =
          new PanamaWasiConfigBuilder().withWasiVersion(WasiVersion.PREVIEW_1).build();

      assertEquals(WasiVersion.PREVIEW_1, config.getWasiVersion());

      LOGGER.info("WASI version set to PREVIEW_1");
    }

    @Test
    @DisplayName("Should set WASI version to PREVIEW_2")
    void shouldSetWasiVersionToPreview2() {
      LOGGER.info("Testing withWasiVersion(PREVIEW_2)");

      final WasiConfig config =
          new PanamaWasiConfigBuilder().withWasiVersion(WasiVersion.PREVIEW_2).build();

      assertEquals(WasiVersion.PREVIEW_2, config.getWasiVersion());

      LOGGER.info("WASI version set to PREVIEW_2");
    }

    @Test
    @DisplayName("Should throw on null WASI version")
    void shouldThrowOnNullWasiVersion() {
      LOGGER.info("Testing withWasiVersion with null");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withWasiVersion(null),
          "Should throw on null version");

      LOGGER.info("Null WASI version throws exception");
    }
  }

  @Nested
  @DisplayName("Async Operations Method Tests")
  class AsyncOperationsMethodTests {

    @Test
    @DisplayName("Should enable async operations")
    void shouldEnableAsyncOperations() {
      LOGGER.info("Testing withAsyncOperations(true)");

      final WasiConfig config = new PanamaWasiConfigBuilder().withAsyncOperations(true).build();

      if (config instanceof PanamaWasiConfig) {
        assertTrue(
            ((PanamaWasiConfig) config).isAsyncOperationsEnabled(),
            "Async operations should be enabled");
      }

      LOGGER.info("Async operations enabled");
    }

    @Test
    @DisplayName("Should disable async operations")
    void shouldDisableAsyncOperations() {
      LOGGER.info("Testing withAsyncOperations(false)");

      final WasiConfig config = new PanamaWasiConfigBuilder().withAsyncOperations(false).build();

      if (config instanceof PanamaWasiConfig) {
        assertFalse(
            ((PanamaWasiConfig) config).isAsyncOperationsEnabled(),
            "Async operations should be disabled");
      }

      LOGGER.info("Async operations disabled");
    }

    @Test
    @DisplayName("Should set max async operations")
    void shouldSetMaxAsyncOperations() {
      LOGGER.info("Testing withMaxAsyncOperations");

      final WasiConfig config = new PanamaWasiConfigBuilder().withMaxAsyncOperations(50).build();

      if (config instanceof PanamaWasiConfig) {
        assertEquals(50, ((PanamaWasiConfig) config).getMaxAsyncOperations().get());
      }

      LOGGER.info("Max async operations set");
    }

    @Test
    @DisplayName("Should throw on non-positive max async operations")
    void shouldThrowOnNonPositiveMaxAsyncOperations() {
      LOGGER.info("Testing withMaxAsyncOperations with non-positive value");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withMaxAsyncOperations(0),
          "Should throw on zero");

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withMaxAsyncOperations(-1),
          "Should throw on negative");

      LOGGER.info("Non-positive max async operations throws exception");
    }

    @Test
    @DisplayName("Should clear max async operations")
    void shouldClearMaxAsyncOperations() {
      LOGGER.info("Testing withoutMaxAsyncOperations");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withMaxAsyncOperations(50)
              .withoutMaxAsyncOperations()
              .build();

      if (config instanceof PanamaWasiConfig) {
        assertFalse(
            ((PanamaWasiConfig) config).getMaxAsyncOperations().isPresent(),
            "Max async operations should be empty");
      }

      LOGGER.info("Max async operations cleared");
    }

    @Test
    @DisplayName("Should set async operation timeout")
    void shouldSetAsyncOperationTimeout() {
      LOGGER.info("Testing withAsyncOperationTimeout");

      final Duration timeout = Duration.ofSeconds(10);
      final WasiConfig config =
          new PanamaWasiConfigBuilder().withAsyncOperationTimeout(timeout).build();

      if (config instanceof PanamaWasiConfig) {
        assertEquals(timeout, ((PanamaWasiConfig) config).getAsyncOperationTimeout().get());
      }

      LOGGER.info("Async operation timeout set");
    }

    @Test
    @DisplayName("Should throw on null async operation timeout")
    void shouldThrowOnNullAsyncOperationTimeout() {
      LOGGER.info("Testing withAsyncOperationTimeout with null");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withAsyncOperationTimeout(null),
          "Should throw on null timeout");

      LOGGER.info("Null async operation timeout throws exception");
    }

    @Test
    @DisplayName("Should throw on negative async operation timeout")
    void shouldThrowOnNegativeAsyncOperationTimeout() {
      LOGGER.info("Testing withAsyncOperationTimeout with negative value");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withAsyncOperationTimeout(Duration.ofSeconds(-1)),
          "Should throw on negative timeout");

      LOGGER.info("Negative async operation timeout throws exception");
    }

    @Test
    @DisplayName("Should clear async operation timeout")
    void shouldClearAsyncOperationTimeout() {
      LOGGER.info("Testing withoutAsyncOperationTimeout");

      final WasiConfig config =
          new PanamaWasiConfigBuilder()
              .withAsyncOperationTimeout(Duration.ofSeconds(10))
              .withoutAsyncOperationTimeout()
              .build();

      if (config instanceof PanamaWasiConfig) {
        assertFalse(
            ((PanamaWasiConfig) config).getAsyncOperationTimeout().isPresent(),
            "Async operation timeout should be empty");
      }

      LOGGER.info("Async operation timeout cleared");
    }
  }

  @Nested
  @DisplayName("Build Method Tests")
  class BuildMethodTests {

    @Test
    @DisplayName("Should validate successfully")
    void shouldValidateSuccessfully() {
      LOGGER.info("Testing validate");

      final PanamaWasiConfigBuilder builder = new PanamaWasiConfigBuilder();
      assertDoesNotThrow(builder::validate, "Validation should not throw");

      LOGGER.info("Validation passed");
    }

    @Test
    @DisplayName("Should build config")
    void shouldBuildConfig() {
      LOGGER.info("Testing build");

      final WasiConfig config = new PanamaWasiConfigBuilder().build();

      assertNotNull(config, "Config should be created");
      assertTrue(config instanceof PanamaWasiConfig, "Should be PanamaWasiConfig");

      LOGGER.info("Config built successfully");
    }

    @Test
    @DisplayName("Should build fully configured instance")
    void shouldBuildFullyConfiguredInstance() {
      LOGGER.info("Testing build with all settings");

      final PanamaWasiConfigBuilder panamaBuilder = new PanamaWasiConfigBuilder();
      panamaBuilder
          .withEnvironment("PATH", "/usr/bin")
          .withArgument("--test")
          .withPreopenDirectory("/", Path.of("/root"))
          .withWorkingDirectory("/work");
      panamaBuilder.inheritEnvironment();
      panamaBuilder
          .withExecutionTimeout(Duration.ofMinutes(5))
          .withSecurityPolicy(new TestWasiSecurityPolicy())
          .withImportResolver("test", new TestWasiImportResolver())
          .withValidation(true)
          .withStrictMode(true)
          .withWasiVersion(WasiVersion.PREVIEW_2)
          .withAsyncOperations(true)
          .withMaxAsyncOperations(100)
          .withAsyncOperationTimeout(Duration.ofSeconds(30));
      final WasiConfig config = panamaBuilder.build();

      assertNotNull(config, "Config should be created");
      assertEquals(1, config.getEnvironment().size());
      assertEquals(1, config.getArguments().size());
      assertEquals(1, config.getPreopenDirectories().size());
      assertTrue(config.getWorkingDirectory().isPresent());
      assertTrue(config.getExecutionTimeout().isPresent());
      assertTrue(config.getSecurityPolicy().isPresent());
      assertEquals(1, config.getImportResolvers().size());
      assertTrue(config.isValidationEnabled());
      assertTrue(config.isStrictModeEnabled());
      assertEquals(WasiVersion.PREVIEW_2, config.getWasiVersion());

      if (config instanceof PanamaWasiConfig) {
        final PanamaWasiConfig panamaConfig = (PanamaWasiConfig) config;
        assertTrue(panamaConfig.isInheritEnvironment());
        assertTrue(panamaConfig.isAsyncOperationsEnabled());
        assertEquals(100, panamaConfig.getMaxAsyncOperations().get());
        assertEquals(Duration.ofSeconds(30), panamaConfig.getAsyncOperationTimeout().get());
      }

      LOGGER.info("Fully configured instance built successfully");
    }
  }

  @Nested
  @DisplayName("Method Chaining Tests")
  class MethodChainingTests {

    @Test
    @DisplayName("Should support method chaining for all methods")
    void shouldSupportMethodChainingForAllMethods() {
      LOGGER.info("Testing method chaining");

      final WasiConfigBuilder builder = new PanamaWasiConfigBuilder();

      // All withXxx methods should return the builder for chaining
      final WasiConfigBuilder result =
          builder
              .withEnvironment("KEY", "value")
              .withEnvironment(Collections.singletonMap("KEY2", "value2"))
              .withoutEnvironment("KEY2")
              .clearEnvironment()
              .withEnvironment("FINAL_KEY", "final_value")
              .withArgument("arg1")
              .withArguments(Arrays.asList("arg2", "arg3"))
              .clearArguments()
              .withArgument("final_arg")
              .withPreopenDirectory("/guest", Path.of("/host"))
              .withPreopenDirectories(Collections.singletonMap("/tmp", Path.of("/tmp")))
              .withoutPreopenDirectory("/tmp")
              .clearPreopenDirectories()
              .withPreopenDirectory("/", Path.of("/"))
              .withWorkingDirectory("/work")
              .withoutWorkingDirectory()
              .withWorkingDirectory("/final_work")
              .withExecutionTimeout(Duration.ofSeconds(30))
              .withoutExecutionTimeout()
              .withExecutionTimeout(Duration.ofMinutes(1))
              .withSecurityPolicy(new TestWasiSecurityPolicy())
              .withoutSecurityPolicy()
              .withImportResolver("test", new TestWasiImportResolver())
              .clearImportResolvers()
              .withValidation(true)
              .withStrictMode(true)
              .withWasiVersion(WasiVersion.PREVIEW_1)
              .withAsyncOperations(true)
              .withMaxAsyncOperations(10)
              .withoutMaxAsyncOperations()
              .withMaxAsyncOperations(20)
              .withAsyncOperationTimeout(Duration.ofSeconds(5))
              .withoutAsyncOperationTimeout()
              .withAsyncOperationTimeout(Duration.ofSeconds(10));

      assertNotNull(result, "Builder should be returned for chaining");
      assertEquals(builder, result, "Should return same builder instance");

      final WasiConfig config = result.build();
      assertNotNull(config, "Config should be built from chained builder");

      LOGGER.info("Method chaining works correctly");
    }
  }

  // Test helper classes

  /** Test implementation of WasiResourceLimits. */
  private static final class TestWasiResourceLimits implements WasiResourceLimits {

    @Override
    public java.util.Optional<Long> getMemoryLimit() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Duration> getExecutionTimeout() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Duration> getTotalExecutionTimeout() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Integer> getFileHandleLimit() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Integer> getNetworkConnectionLimit() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Integer> getThreadLimit() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Integer> getStackDepthLimit() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Integer> getResourceCountLimit() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Long> getFileWriteLimit() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Long> getFileReadLimit() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Long> getNetworkSendLimit() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Long> getNetworkReceiveLimit() {
      return java.util.Optional.empty();
    }

    @Override
    public java.util.Optional<Duration> getCpuTimeLimit() {
      return java.util.Optional.empty();
    }

    @Override
    public boolean isUnlimited() {
      return true;
    }

    @Override
    public boolean hasLimits() {
      return false;
    }

    @Override
    public WasiResourceLimitsBuilder toBuilder() {
      throw new UnsupportedOperationException("Test implementation");
    }

    @Override
    public void validate() {
      // No-op for testing
    }

    @Override
    public String getSummary() {
      return "TestWasiResourceLimits";
    }
  }

  /** Test implementation of WasiSecurityPolicy. */
  private static final class TestWasiSecurityPolicy implements WasiSecurityPolicy {

    @Override
    public boolean isFileSystemAccessAllowed(final Path path, final String operation) {
      return true;
    }

    @Override
    public boolean isNetworkAccessAllowed(
        final String host, final int port, final String protocol) {
      return true;
    }

    @Override
    public java.util.Set<String> getAllowedFileSystemOperations() {
      return java.util.Collections.emptySet();
    }

    @Override
    public java.util.Set<String> getAllowedNetworkOperations() {
      return java.util.Collections.emptySet();
    }

    @Override
    public java.util.List<Path> getAllowedPaths() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<Path> getBlockedPaths() {
      return java.util.Collections.emptyList();
    }

    @Override
    public boolean isEnvironmentVariableAllowed(final String name) {
      return true;
    }

    @Override
    public boolean isProcessSpawningAllowed() {
      return true;
    }

    @Override
    public boolean isThreadingAllowed() {
      return true;
    }

    @Override
    public void validate() {
      // No-op for testing
    }
  }

  /** Test implementation of WasiImportResolver. */
  private static final class TestWasiImportResolver implements WasiImportResolver {

    @Override
    public String getInterfaceName() {
      return "test-interface";
    }

    @Override
    public String getInterfaceVersion() {
      return "1.0.0";
    }

    @Override
    public java.util.List<String> getProvidedFunctions() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<String> getProvidedResourceTypes() {
      return java.util.Collections.emptyList();
    }

    @Override
    public Object resolveFunction(
        final String functionName, final java.util.List<Object> parameters) {
      throw new UnsupportedOperationException("Test implementation");
    }

    @Override
    public ai.tegmentum.wasmtime4j.wasi.WasiResource createResource(
        final String resourceType, final Object... parameters) {
      throw new UnsupportedOperationException("Test implementation");
    }

    @Override
    public ai.tegmentum.wasmtime4j.wasi.WasiFunctionMetadata getFunctionMetadata(
        final String functionName) {
      throw new UnsupportedOperationException("Test implementation");
    }

    @Override
    public ai.tegmentum.wasmtime4j.wasi.WasiResourceTypeMetadata getResourceTypeMetadata(
        final String resourceType) {
      throw new UnsupportedOperationException("Test implementation");
    }

    @Override
    public boolean canResolveFunction(final String functionName) {
      return false;
    }

    @Override
    public boolean canCreateResourceType(final String resourceType) {
      return false;
    }

    @Override
    public java.util.Map<String, Object> getProperties() {
      return java.util.Collections.emptyMap();
    }

    @Override
    public void setProperty(final String key, final Object value) {
      // No-op for testing
    }

    @Override
    public void validate() {
      // No-op for testing
    }

    @Override
    public void initialize(final ai.tegmentum.wasmtime4j.wasi.WasiComponent component) {
      // No-op for testing
    }

    @Override
    public void cleanup() {
      // No-op for testing
    }
  }
}
