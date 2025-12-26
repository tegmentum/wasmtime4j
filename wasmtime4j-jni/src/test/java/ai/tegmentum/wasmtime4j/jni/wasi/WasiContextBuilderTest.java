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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiContextBuilder} class.
 *
 * <p>WasiContextBuilder provides a fluent API for building WASI contexts.
 */
@DisplayName("WasiContextBuilder Class Tests")
class WasiContextBuilderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiContextBuilder.class.getModifiers()),
          "WasiContextBuilder should be final");
    }

    @Test
    @DisplayName("should have static builder factory method")
    void shouldHaveStaticBuilderFactoryMethod() throws NoSuchMethodException {
      final Method method = WasiContextBuilder.class.getMethod("builder");
      assertNotNull(method, "Should have builder method");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          WasiContextBuilder.class,
          method.getReturnType(),
          "builder should return WasiContextBuilder");
    }

    @Test
    @DisplayName("should have withEnvironment methods")
    void shouldHaveWithEnvironmentMethods() throws NoSuchMethodException {
      assertNotNull(
          WasiContextBuilder.class.getMethod("withEnvironment", String.class, String.class),
          "Should have withEnvironment(String, String)");
      assertNotNull(
          WasiContextBuilder.class.getMethod("withEnvironment", Map.class),
          "Should have withEnvironment(Map)");
    }

    @Test
    @DisplayName("should have withInheritedEnvironment method")
    void shouldHaveWithInheritedEnvironmentMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiContextBuilder.class.getMethod("withInheritedEnvironment"),
          "Should have withInheritedEnvironment");
    }

    @Test
    @DisplayName("should have withArgument methods")
    void shouldHaveWithArgumentMethods() throws NoSuchMethodException {
      assertNotNull(
          WasiContextBuilder.class.getMethod("withArgument", String.class),
          "Should have withArgument");
      assertNotNull(
          WasiContextBuilder.class.getMethod("withArguments", String[].class),
          "Should have withArguments");
    }

    @Test
    @DisplayName("should have withPreopenDirectory methods")
    void shouldHaveWithPreopenDirectoryMethods() throws NoSuchMethodException {
      assertNotNull(
          WasiContextBuilder.class.getMethod("withPreopenDirectory", String.class, String.class),
          "Should have withPreopenDirectory(String, String)");
      assertNotNull(
          WasiContextBuilder.class.getMethod("withPreopenDirectory", String.class),
          "Should have withPreopenDirectory(String)");
    }

    @Test
    @DisplayName("should have withWorkingDirectory method")
    void shouldHaveWithWorkingDirectoryMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiContextBuilder.class.getMethod("withWorkingDirectory", String.class),
          "Should have withWorkingDirectory");
    }

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = WasiContextBuilder.class.getMethod("build");
      assertNotNull(method, "Should have build method");
      assertEquals(WasiContext.class, method.getReturnType(), "build should return WasiContext");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("builder should create new instance")
    void builderShouldCreateNewInstance() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      assertNotNull(builder, "builder() should return non-null instance");
    }

    @Test
    @DisplayName("builder should create independent instances")
    void builderShouldCreateIndependentInstances() {
      final WasiContextBuilder builder1 = WasiContextBuilder.builder();
      final WasiContextBuilder builder2 = WasiContextBuilder.builder();
      assertTrue(builder1 != builder2, "Each call should create new instance");
    }
  }

  @Nested
  @DisplayName("Environment Configuration Tests")
  class EnvironmentConfigurationTests {

    @Test
    @DisplayName("withEnvironment should return same builder for chaining")
    void withEnvironmentShouldReturnSameBuilderForChaining() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      final WasiContextBuilder result = builder.withEnvironment("KEY", "value");
      assertTrue(result == builder, "Should return same builder for chaining");
    }

    @Test
    @DisplayName("withEnvironment should throw for null name")
    void withEnvironmentShouldThrowForNullName() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withEnvironment(null, "value"),
          "Should throw for null name");
    }

    @Test
    @DisplayName("withEnvironment should throw for empty name")
    void withEnvironmentShouldThrowForEmptyName() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withEnvironment("", "value"),
          "Should throw for empty name");
    }

    @Test
    @DisplayName("withEnvironment should throw for null value")
    void withEnvironmentShouldThrowForNullValue() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withEnvironment("KEY", null),
          "Should throw for null value");
    }

    @Test
    @DisplayName("withEnvironment map should throw for null map")
    void withEnvironmentMapShouldThrowForNullMap() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withEnvironment((Map<String, String>) null),
          "Should throw for null map");
    }

    @Test
    @DisplayName("getEnvironment should return copy of environment")
    void getEnvironmentShouldReturnCopyOfEnvironment() {
      final WasiContextBuilder builder =
          WasiContextBuilder.builder()
              .withEnvironment("KEY1", "value1")
              .withEnvironment("KEY2", "value2");

      final Map<String, String> env = builder.getEnvironment();
      assertEquals(2, env.size());
      assertEquals("value1", env.get("KEY1"));
      assertEquals("value2", env.get("KEY2"));
    }

    @Test
    @DisplayName("addEnvironmentVariable should be alias for withEnvironment")
    void addEnvironmentVariableShouldBeAliasForWithEnvironment() {
      final WasiContextBuilder builder =
          WasiContextBuilder.builder().addEnvironmentVariable("KEY", "value");

      final Map<String, String> env = builder.getEnvironment();
      assertEquals(1, env.size());
      assertEquals("value", env.get("KEY"));
    }
  }

  @Nested
  @DisplayName("Argument Configuration Tests")
  class ArgumentConfigurationTests {

    @Test
    @DisplayName("withArgument should return same builder for chaining")
    void withArgumentShouldReturnSameBuilderForChaining() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      final WasiContextBuilder result = builder.withArgument("--verbose");
      assertTrue(result == builder, "Should return same builder for chaining");
    }

    @Test
    @DisplayName("withArgument should throw for null argument")
    void withArgumentShouldThrowForNullArgument() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withArgument(null),
          "Should throw for null argument");
    }

    @Test
    @DisplayName("withArguments should add multiple arguments")
    void withArgumentsShouldAddMultipleArguments() {
      final WasiContextBuilder builder =
          WasiContextBuilder.builder().withArguments("--verbose", "--debug", "file.txt");

      final List<String> args = builder.getArguments();
      assertEquals(3, args.size());
      assertEquals("--verbose", args.get(0));
      assertEquals("--debug", args.get(1));
      assertEquals("file.txt", args.get(2));
    }

    @Test
    @DisplayName("withArguments should throw for null array")
    void withArgumentsShouldThrowForNullArray() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withArguments((String[]) null),
          "Should throw for null array");
    }
  }

  @Nested
  @DisplayName("Working Directory Tests")
  class WorkingDirectoryTests {

    @Test
    @DisplayName("getWorkingDirectory should return default path")
    void getWorkingDirectoryShouldReturnDefaultPath() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      final Path workDir = builder.getWorkingDirectory();
      assertNotNull(workDir, "Working directory should not be null");
      assertEquals(Paths.get("/"), workDir, "Default working directory should be /");
    }

    @Test
    @DisplayName("withWorkingDirectory should update working directory")
    void withWorkingDirectoryShouldUpdateWorkingDirectory() {
      final WasiContextBuilder builder = WasiContextBuilder.builder().withWorkingDirectory("/app");

      assertEquals(Paths.get("/app"), builder.getWorkingDirectory());
    }

    @Test
    @DisplayName("withWorkingDirectory should throw for null")
    void withWorkingDirectoryShouldThrowForNull() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withWorkingDirectory(null),
          "Should throw for null directory");
    }

    @Test
    @DisplayName("withWorkingDirectory should throw for empty string")
    void withWorkingDirectoryShouldThrowForEmptyString() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withWorkingDirectory(""),
          "Should throw for empty directory");
    }
  }

  @Nested
  @DisplayName("Preopen Directory Tests")
  class PreopenDirectoryTests {

    @Test
    @DisplayName("getPreopenedDirectories should return empty map by default")
    void getPreopenedDirectoriesShouldReturnEmptyMapByDefault() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      assertTrue(
          builder.getPreopenedDirectories().isEmpty(),
          "Should have no preopened directories by default");
    }

    @Test
    @DisplayName("withPreopenDirectory should throw for null guest dir")
    void withPreopenDirectoryShouldThrowForNullGuestDir() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withPreopenDirectory(null, "/tmp"),
          "Should throw for null guest dir");
    }

    @Test
    @DisplayName("withPreopenDirectory should throw for null host dir")
    void withPreopenDirectoryShouldThrowForNullHostDir() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.withPreopenDirectory("/guest", null),
          "Should throw for null host dir");
    }

    @Test
    @DisplayName("addPreopenedDirectory should work with Path and String")
    void addPreopenedDirectoryShouldWorkWithPathAndString() throws NoSuchMethodException {
      final Method method =
          WasiContextBuilder.class.getMethod("addPreopenedDirectory", Path.class, String.class);
      assertNotNull(method, "Should have addPreopenedDirectory(Path, String)");
    }
  }

  @Nested
  @DisplayName("Fluent API Tests")
  class FluentApiTests {

    @Test
    @DisplayName("all with* methods should return same builder")
    void allWithMethodsShouldReturnSameBuilder() {
      final WasiContextBuilder builder = WasiContextBuilder.builder();

      assertTrue(builder == builder.withEnvironment("KEY", "value"));
      assertTrue(builder == builder.withEnvironment(new HashMap<>()));
      assertTrue(builder == builder.withInheritedEnvironment());
      assertTrue(builder == builder.withArgument("arg"));
      assertTrue(builder == builder.withArguments("arg1", "arg2"));
      assertTrue(builder == builder.withWorkingDirectory("/app"));
    }

    @Test
    @DisplayName("should support method chaining")
    void shouldSupportMethodChaining() {
      final WasiContextBuilder builder =
          WasiContextBuilder.builder()
              .withEnvironment("HOME", "/home/user")
              .withEnvironment("PATH", "/usr/bin")
              .withArgument("--verbose")
              .withArgument("--debug")
              .withWorkingDirectory("/app");

      assertNotNull(builder);
      assertEquals(2, builder.getEnvironment().size());
      assertEquals(2, builder.getArguments().size());
      assertEquals(Paths.get("/app"), builder.getWorkingDirectory());
    }
  }
}
