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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiProcessConfig} class.
 *
 * <p>Verifies builder construction, field accessors, validation, immutability of returned
 * collections, toBuilder cloning, and defensive copying behavior.
 */
@DisplayName("WasiProcessConfig Tests")
class WasiProcessConfigTest {

  @Nested
  @DisplayName("Builder Construction Tests")
  class BuilderConstructionTests {

    @Test
    @DisplayName("should build with all fields set")
    void shouldBuildWithAllFieldsSet() {
      final WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .addArgument("Hello")
              .addArgument("World")
              .setEnvironmentVariable("PATH", "/usr/bin")
              .setWorkingDirectory("/tmp")
              .build();

      assertEquals("/bin/echo", config.getProgram(), "Program should be /bin/echo");
      assertEquals(
          Arrays.asList("Hello", "World"), config.getArguments(),
          "Arguments should match");
      assertEquals("/usr/bin", config.getEnvironment().get("PATH"), "PATH env var should match");
      assertEquals("/tmp", config.getWorkingDirectory(), "Working directory should be /tmp");
    }

    @Test
    @DisplayName("should build with only program set")
    void shouldBuildWithOnlyProgramSet() {
      final WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/ls")
              .build();

      assertEquals("/bin/ls", config.getProgram(), "Program should be /bin/ls");
      assertTrue(config.getArguments().isEmpty(), "Arguments should be empty");
      assertTrue(config.getEnvironment().isEmpty(), "Environment should be empty");
      assertNull(config.getWorkingDirectory(), "Working directory should be null");
      assertNull(config.getResourceLimits(), "Resource limits should be null");
    }

    @Test
    @DisplayName("should throw when program is not set")
    void shouldThrowWhenProgramNotSet() {
      final WasiProcessConfig.Builder builder = WasiProcessConfig.builder();
      assertThrows(
          IllegalStateException.class,
          builder::build,
          "Should throw when program is not set");
    }

    @Test
    @DisplayName("should throw when program is null")
    void shouldThrowWhenProgramIsNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiProcessConfig.builder().setProgram(null),
          "Should throw when program is null");
    }

    @Test
    @DisplayName("should throw when program is empty")
    void shouldThrowWhenProgramIsEmpty() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiProcessConfig.builder().setProgram(""),
          "Should throw when program is empty");
    }

    @Test
    @DisplayName("should throw when program is whitespace only")
    void shouldThrowWhenProgramIsWhitespace() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiProcessConfig.builder().setProgram("   "),
          "Should throw when program is whitespace only");
    }
  }

  @Nested
  @DisplayName("Arguments Tests")
  class ArgumentsTests {

    @Test
    @DisplayName("addArgument should add to arguments list")
    void addArgumentShouldAddToList() {
      final WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .addArgument("arg1")
              .addArgument("arg2")
              .addArgument("arg3")
              .build();

      assertEquals(3, config.getArguments().size(), "Should have 3 arguments");
      assertEquals("arg1", config.getArguments().get(0), "First argument should be arg1");
      assertEquals("arg2", config.getArguments().get(1), "Second argument should be arg2");
      assertEquals("arg3", config.getArguments().get(2), "Third argument should be arg3");
    }

    @Test
    @DisplayName("addArgument should throw for null argument")
    void addArgumentShouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiProcessConfig.builder().addArgument(null),
          "Should throw for null argument");
    }

    @Test
    @DisplayName("setArguments should replace existing arguments")
    void setArgumentsShouldReplaceExisting() {
      final WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .addArgument("old")
              .setArguments(Arrays.asList("new1", "new2"))
              .build();

      assertEquals(2, config.getArguments().size(), "Should have 2 arguments after replace");
      assertEquals("new1", config.getArguments().get(0), "First argument should be new1");
    }

    @Test
    @DisplayName("setArguments should throw for null list")
    void setArgumentsShouldThrowForNullList() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiProcessConfig.builder().setArguments(null),
          "Should throw for null arguments list");
    }

    @Test
    @DisplayName("setArguments should throw for list containing null")
    void setArgumentsShouldThrowForListContainingNull() {
      final List<String> args = new ArrayList<>();
      args.add("valid");
      args.add(null);
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiProcessConfig.builder().setArguments(args),
          "Should throw for arguments list containing null");
    }

    @Test
    @DisplayName("getArguments should return immutable list")
    void getArgumentsShouldReturnImmutableList() {
      final WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .addArgument("arg1")
              .build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> config.getArguments().add("new"),
          "Returned arguments list should be immutable");
    }
  }

  @Nested
  @DisplayName("Environment Tests")
  class EnvironmentTests {

    @Test
    @DisplayName("setEnvironmentVariable should add env vars")
    void setEnvironmentVariableShouldAddEnvVars() {
      final WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .setEnvironmentVariable("KEY1", "val1")
              .setEnvironmentVariable("KEY2", "val2")
              .build();

      assertEquals(2, config.getEnvironment().size(), "Should have 2 env vars");
      assertEquals("val1", config.getEnvironment().get("KEY1"), "KEY1 should be val1");
      assertEquals("val2", config.getEnvironment().get("KEY2"), "KEY2 should be val2");
    }

    @Test
    @DisplayName("setEnvironmentVariable should throw for null key")
    void setEnvironmentVariableShouldThrowForNullKey() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiProcessConfig.builder().setEnvironmentVariable(null, "value"),
          "Should throw for null key");
    }

    @Test
    @DisplayName("setEnvironmentVariable should throw for null value")
    void setEnvironmentVariableShouldThrowForNullValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiProcessConfig.builder().setEnvironmentVariable("key", null),
          "Should throw for null value");
    }

    @Test
    @DisplayName("setEnvironment should replace all env vars")
    void setEnvironmentShouldReplaceAll() {
      final Map<String, String> env = new HashMap<>();
      env.put("NEW_KEY", "new_val");
      final WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .setEnvironmentVariable("OLD_KEY", "old_val")
              .setEnvironment(env)
              .build();

      assertEquals(1, config.getEnvironment().size(), "Should have 1 env var after replace");
      assertEquals("new_val", config.getEnvironment().get("NEW_KEY"), "NEW_KEY should be new_val");
    }

    @Test
    @DisplayName("setEnvironment should throw for null map")
    void setEnvironmentShouldThrowForNullMap() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiProcessConfig.builder().setEnvironment(null),
          "Should throw for null environment map");
    }

    @Test
    @DisplayName("getEnvironment should return immutable map")
    void getEnvironmentShouldReturnImmutableMap() {
      final WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .setEnvironmentVariable("KEY", "val")
              .build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> config.getEnvironment().put("NEW", "val"),
          "Returned environment map should be immutable");
    }
  }

  @Nested
  @DisplayName("ToBuilder Tests")
  class ToBuilderTests {

    @Test
    @DisplayName("toBuilder should copy all fields")
    void toBuilderShouldCopyAllFields() {
      final WasiProcessConfig original =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .addArgument("hello")
              .setEnvironmentVariable("KEY", "val")
              .setWorkingDirectory("/tmp")
              .build();

      final WasiProcessConfig copy = original.toBuilder().build();

      assertEquals(
          original.getProgram(), copy.getProgram(),
          "Program should be copied");
      assertEquals(
          original.getArguments(), copy.getArguments(),
          "Arguments should be copied");
      assertEquals(
          original.getEnvironment(), copy.getEnvironment(),
          "Environment should be copied");
      assertEquals(
          original.getWorkingDirectory(), copy.getWorkingDirectory(),
          "Working directory should be copied");
    }

    @Test
    @DisplayName("toBuilder should allow modifications")
    void toBuilderShouldAllowModifications() {
      final WasiProcessConfig original =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .build();

      final WasiProcessConfig modified =
          original.toBuilder()
              .setProgram("/bin/cat")
              .addArgument("file.txt")
              .build();

      assertEquals("/bin/cat", modified.getProgram(), "Modified program should be /bin/cat");
      assertEquals(1, modified.getArguments().size(), "Modified should have 1 argument");
    }
  }

  @Nested
  @DisplayName("Defensive Copy Tests")
  class DefensiveCopyTests {

    @Test
    @DisplayName("should defensively copy arguments list from builder")
    void shouldDefensivelyCopyArgumentsList() {
      final List<String> mutableArgs = new ArrayList<>(Arrays.asList("a", "b"));
      final WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .setArguments(mutableArgs)
              .build();

      mutableArgs.add("c");
      assertEquals(
          2, config.getArguments().size(),
          "Modifying original list should not affect config");
    }

    @Test
    @DisplayName("should defensively copy environment map from builder")
    void shouldDefensivelyCopyEnvironmentMap() {
      final Map<String, String> mutableEnv = new HashMap<>();
      mutableEnv.put("KEY", "val");
      final WasiProcessConfig config =
          WasiProcessConfig.builder()
              .setProgram("/bin/echo")
              .setEnvironment(mutableEnv)
              .build();

      mutableEnv.put("NEW_KEY", "new_val");
      assertEquals(
          1, config.getEnvironment().size(),
          "Modifying original map should not affect config");
    }
  }

  @Nested
  @DisplayName("Builder Factory Tests")
  class BuilderFactoryTests {

    @Test
    @DisplayName("builder() should return a non-null builder")
    void builderShouldReturnNonNullBuilder() {
      final WasiProcessConfig.Builder builder = WasiProcessConfig.builder();
      assertNotNull(builder, "Builder should not be null");
    }
  }
}
