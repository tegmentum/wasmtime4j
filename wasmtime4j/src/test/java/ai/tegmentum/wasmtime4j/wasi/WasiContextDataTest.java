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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiContextData}.
 *
 * <p>Validates constructor behavior, defensive copying, accessor methods, path validation, and
 * clearState lifecycle management.
 */
@DisplayName("WasiContextData Tests")
class WasiContextDataTest {

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should accept all non-null parameters")
    void shouldAcceptAllNonNullParameters() {
      Map<String, String> env = Map.of("HOME", "/home/wasm", "PATH", "/usr/bin");
      List<String> args = List.of("program", "--verbose");
      Map<String, Path> dirs = Map.of("/guest", Paths.get("/host/dir"));
      Path workDir = Paths.get("/work");

      WasiContextData data = new WasiContextData(env, args, dirs, workDir);

      assertEquals(2, data.getEnvironmentCount(), "Should have 2 environment variables");
      assertEquals(2, data.getArgumentCount(), "Should have 2 arguments");
      assertEquals(1, data.getPreopenedDirectoryCount(), "Should have 1 preopen directory");
      assertEquals(workDir, data.getWorkingDirectory(), "Working directory should match");
    }

    @Test
    @DisplayName("should handle all null parameters gracefully")
    void shouldHandleAllNullParametersGracefully() {
      WasiContextData data = new WasiContextData(null, null, null, null);

      assertEquals(0, data.getEnvironmentCount(), "Should have 0 environment variables");
      assertEquals(0, data.getArgumentCount(), "Should have 0 arguments");
      assertEquals(0, data.getPreopenedDirectoryCount(), "Should have 0 preopen directories");
      assertEquals(Paths.get("/"), data.getWorkingDirectory(), "Should default to root");
    }

    @Test
    @DisplayName("should handle empty collections")
    void shouldHandleEmptyCollections() {
      WasiContextData data =
          new WasiContextData(Map.of(), List.of(), Map.of(), Paths.get("/empty"));

      assertEquals(0, data.getEnvironmentCount());
      assertEquals(0, data.getArgumentCount());
      assertEquals(0, data.getPreopenedDirectoryCount());
      assertEquals(Paths.get("/empty"), data.getWorkingDirectory());
    }

    @Test
    @DisplayName("should defensively copy environment map")
    void shouldDefensivelyCopyEnvironmentMap() {
      Map<String, String> env = new HashMap<>();
      env.put("KEY", "value");

      WasiContextData data = new WasiContextData(env, null, null, null);

      // Mutate original map after construction
      env.put("NEW_KEY", "new_value");

      assertEquals(
          1, data.getEnvironmentCount(), "Mutation of original map should not affect data");
      assertNull(
          data.getEnvironmentVariable("NEW_KEY"),
          "New key from mutated original should not be visible");
    }

    @Test
    @DisplayName("should defensively copy arguments list")
    void shouldDefensivelyCopyArgumentsList() {
      List<String> args = new ArrayList<>();
      args.add("arg1");

      WasiContextData data = new WasiContextData(null, args, null, null);

      // Mutate original list after construction
      args.add("arg2");

      assertEquals(1, data.getArgumentCount(), "Mutation of original list should not affect data");
    }

    @Test
    @DisplayName("should defensively copy preopen directories map")
    void shouldDefensivelyCopyPreopenDirectoriesMap() {
      Map<String, Path> dirs = new HashMap<>();
      dirs.put("/guest", Paths.get("/host"));

      WasiContextData data = new WasiContextData(null, null, dirs, null);

      // Mutate original map after construction
      dirs.put("/other", Paths.get("/other/host"));

      assertEquals(
          1, data.getPreopenedDirectoryCount(), "Mutation of original map should not affect data");
    }
  }

  // ========================================================================
  // Environment Variable Tests
  // ========================================================================

  @Nested
  @DisplayName("Environment Variable Tests")
  class EnvironmentVariableTests {

    @Test
    @DisplayName("should return environment variable value when set")
    void shouldReturnEnvironmentVariableValueWhenSet() {
      WasiContextData data = new WasiContextData(Map.of("HOME", "/home/wasm"), null, null, null);

      assertEquals("/home/wasm", data.getEnvironmentVariable("HOME"));
    }

    @Test
    @DisplayName("should return null for missing environment variable")
    void shouldReturnNullForMissingEnvironmentVariable() {
      WasiContextData data = new WasiContextData(Map.of("HOME", "/home"), null, null, null);

      assertNull(
          data.getEnvironmentVariable("NONEXISTENT"), "Should return null for missing variable");
    }

    @Test
    @DisplayName("should throw for null environment variable name")
    void shouldThrowForNullEnvironmentVariableName() {
      WasiContextData data = new WasiContextData(null, null, null, null);

      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> data.getEnvironmentVariable(null),
              "Should throw for null name");
      assertTrue(
          e.getMessage().contains("null or empty"),
          "Message should mention null or empty, got: " + e.getMessage());
    }

    @Test
    @DisplayName("should throw for empty environment variable name")
    void shouldThrowForEmptyEnvironmentVariableName() {
      WasiContextData data = new WasiContextData(null, null, null, null);

      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> data.getEnvironmentVariable(""),
              "Should throw for empty name");
      assertTrue(
          e.getMessage().contains("null or empty"),
          "Message should mention null or empty, got: " + e.getMessage());
    }

    @Test
    @DisplayName("should return unmodifiable environment map")
    void shouldReturnUnmodifiableEnvironmentMap() {
      WasiContextData data = new WasiContextData(Map.of("A", "1", "B", "2"), null, null, null);

      Map<String, String> env = data.getEnvironment();
      assertEquals(2, env.size(), "Should have 2 entries");

      assertThrows(
          UnsupportedOperationException.class,
          () -> env.put("C", "3"),
          "Returned map should be unmodifiable");
    }

    @Test
    @DisplayName("should return defensive copy of environment map")
    void shouldReturnDefensiveCopyOfEnvironmentMap() {
      WasiContextData data = new WasiContextData(Map.of("K", "V"), null, null, null);

      Map<String, String> first = data.getEnvironment();
      Map<String, String> second = data.getEnvironment();

      assertNotSame(first, second, "Each call should return a new defensive copy");
      assertEquals(first, second, "Copies should have equal content");
    }
  }

  // ========================================================================
  // Arguments Tests
  // ========================================================================

  @Nested
  @DisplayName("Arguments Tests")
  class ArgumentsTests {

    @Test
    @DisplayName("should return arguments array")
    void shouldReturnArgumentsArray() {
      WasiContextData data =
          new WasiContextData(null, List.of("prog", "-v", "--output=test"), null, null);

      String[] args = data.getArguments();
      assertArrayEquals(
          new String[] {"prog", "-v", "--output=test"}, args, "Arguments should match");
    }

    @Test
    @DisplayName("should return empty array when no arguments")
    void shouldReturnEmptyArrayWhenNoArguments() {
      WasiContextData data = new WasiContextData(null, null, null, null);

      String[] args = data.getArguments();
      assertNotNull(args, "Arguments should not be null");
      assertEquals(0, args.length, "Arguments should be empty");
    }

    @Test
    @DisplayName("should return defensive copy of arguments")
    void shouldReturnDefensiveCopyOfArguments() {
      WasiContextData data = new WasiContextData(null, List.of("arg1"), null, null);

      String[] first = data.getArguments();
      first[0] = "mutated";

      String[] second = data.getArguments();
      assertEquals(
          "arg1", second[0], "Mutation of returned array should not affect internal state");
    }
  }

  // ========================================================================
  // Pre-opened Directories Tests
  // ========================================================================

  @Nested
  @DisplayName("Pre-opened Directories Tests")
  class PreopenedDirectoriesTests {

    @Test
    @DisplayName("should return preopen directories")
    void shouldReturnPreopenDirectories() {
      Map<String, Path> dirs = Map.of("/guest", Paths.get("/host"), "/tmp", Paths.get("/sandbox"));
      WasiContextData data = new WasiContextData(null, null, dirs, null);

      Map<String, Path> result = data.getPreopenedDirectories();
      assertEquals(2, result.size(), "Should have 2 directories");
      assertEquals(Paths.get("/host"), result.get("/guest"));
      assertEquals(Paths.get("/sandbox"), result.get("/tmp"));
    }

    @Test
    @DisplayName("should return unmodifiable preopen directories map")
    void shouldReturnUnmodifiablePreopenDirectoriesMap() {
      WasiContextData data = new WasiContextData(null, null, Map.of("/g", Paths.get("/h")), null);

      Map<String, Path> dirs = data.getPreopenedDirectories();
      assertThrows(
          UnsupportedOperationException.class,
          () -> dirs.put("/new", Paths.get("/new")),
          "Returned map should be unmodifiable");
    }

    @Test
    @DisplayName("should return defensive copy of preopen directories map")
    void shouldReturnDefensiveCopyOfPreopenDirectoriesMap() {
      WasiContextData data = new WasiContextData(null, null, Map.of("/g", Paths.get("/h")), null);

      Map<String, Path> first = data.getPreopenedDirectories();
      Map<String, Path> second = data.getPreopenedDirectories();

      assertNotSame(first, second, "Each call should return a new defensive copy");
      assertEquals(first, second, "Copies should have equal content");
    }
  }

  // ========================================================================
  // Working Directory Tests
  // ========================================================================

  @Nested
  @DisplayName("Working Directory Tests")
  class WorkingDirectoryTests {

    @Test
    @DisplayName("should return configured working directory")
    void shouldReturnConfiguredWorkingDirectory() {
      Path workDir = Paths.get("/app/data");
      WasiContextData data = new WasiContextData(null, null, null, workDir);

      assertEquals(workDir, data.getWorkingDirectory());
    }

    @Test
    @DisplayName("should default to root when working directory is null")
    void shouldDefaultToRootWhenWorkingDirectoryIsNull() {
      WasiContextData data = new WasiContextData(null, null, null, null);

      assertEquals(Paths.get("/"), data.getWorkingDirectory());
    }
  }

  // ========================================================================
  // Path Validation Tests
  // ========================================================================

  @Nested
  @DisplayName("Path Validation Tests")
  class PathValidationTests {

    @Test
    @DisplayName("should resolve absolute path without modification")
    void shouldResolveAbsolutePathWithoutModification() {
      WasiContextData data = new WasiContextData(null, null, null, Paths.get("/work"));

      Path result = data.validatePath("/usr/local/bin");
      assertTrue(result.isAbsolute(), "Result should be absolute");
      assertEquals(
          Paths.get("/usr/local/bin").toAbsolutePath().normalize(),
          result,
          "Absolute path should be preserved");
    }

    @Test
    @DisplayName("should resolve relative path against working directory")
    void shouldResolveRelativePathAgainstWorkingDirectory() {
      WasiContextData data = new WasiContextData(null, null, null, Paths.get("/work"));

      Path result = data.validatePath("subdir/file.txt");
      assertTrue(result.isAbsolute(), "Result should be absolute");
      assertTrue(
          result.toString().contains("work"),
          "Should resolve against working directory, got: " + result);
      assertTrue(
          result.toString().contains("subdir"),
          "Should contain relative path components, got: " + result);
    }

    @Test
    @DisplayName("should normalize path with traversal components")
    void shouldNormalizePathWithTraversalComponents() {
      WasiContextData data = new WasiContextData(null, null, null, Paths.get("/work"));

      Path result = data.validatePath("/work/subdir/../file.txt");
      assertTrue(result.isAbsolute(), "Result should be absolute");
      // After normalization, ../subdir should be collapsed
      String resultStr = result.toString();
      assertTrue(
          !resultStr.contains(".."),
          "Normalized path should not contain '..' components, got: " + resultStr);
    }

    @Test
    @DisplayName("should throw for null path")
    void shouldThrowForNullPath() {
      WasiContextData data = new WasiContextData(null, null, null, null);

      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> data.validatePath(null),
              "Should throw for null path");
      assertTrue(
          e.getMessage().contains("null or empty"),
          "Message should mention null or empty, got: " + e.getMessage());
    }

    @Test
    @DisplayName("should throw for empty path")
    void shouldThrowForEmptyPath() {
      WasiContextData data = new WasiContextData(null, null, null, null);

      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> data.validatePath(""),
              "Should throw for empty path");
      assertTrue(
          e.getMessage().contains("null or empty"),
          "Message should mention null or empty, got: " + e.getMessage());
    }
  }

  // ========================================================================
  // clearState Tests
  // ========================================================================

  @Nested
  @DisplayName("clearState Tests")
  class ClearStateTests {

    @Test
    @DisplayName("should clear environment after clearState")
    void shouldClearEnvironmentAfterClearState() {
      WasiContextData data = new WasiContextData(Map.of("KEY", "value"), null, null, null);

      assertEquals(1, data.getEnvironmentCount(), "Should have 1 env var before clear");
      data.clearState();
      assertEquals(0, data.getEnvironmentCount(), "Should have 0 env vars after clear");
      assertTrue(data.getEnvironment().isEmpty(), "Environment map should be empty after clear");
    }

    @Test
    @DisplayName("should clear preopen directories after clearState")
    void shouldClearPreopenDirectoriesAfterClearState() {
      WasiContextData data = new WasiContextData(null, null, Map.of("/g", Paths.get("/h")), null);

      assertEquals(1, data.getPreopenedDirectoryCount(), "Should have 1 dir before clear");
      data.clearState();
      assertEquals(0, data.getPreopenedDirectoryCount(), "Should have 0 dirs after clear");
      assertTrue(
          data.getPreopenedDirectories().isEmpty(),
          "Preopen directories map should be empty after clear");
    }

    @Test
    @DisplayName("should preserve arguments after clearState")
    void shouldPreserveArgumentsAfterClearState() {
      WasiContextData data = new WasiContextData(null, List.of("arg1"), null, null);

      data.clearState();
      // Arguments are stored as a final array - clearState only clears mutable collections
      assertEquals(1, data.getArgumentCount(), "Arguments are immutable and should be preserved");
    }

    @Test
    @DisplayName("should be idempotent")
    void shouldBeIdempotent() {
      WasiContextData data =
          new WasiContextData(Map.of("K", "V"), null, Map.of("/g", Paths.get("/h")), null);

      data.clearState();
      data.clearState(); // Second call should not throw

      assertEquals(0, data.getEnvironmentCount());
      assertEquals(0, data.getPreopenedDirectoryCount());
    }
  }

  // ========================================================================
  // Count Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Count Method Tests")
  class CountMethodTests {

    @Test
    @DisplayName("should return correct environment count")
    void shouldReturnCorrectEnvironmentCount() {
      WasiContextData data =
          new WasiContextData(Map.of("A", "1", "B", "2", "C", "3"), null, null, null);

      assertEquals(3, data.getEnvironmentCount());
    }

    @Test
    @DisplayName("should return correct argument count")
    void shouldReturnCorrectArgumentCount() {
      WasiContextData data = new WasiContextData(null, List.of("a", "b", "c", "d"), null, null);

      assertEquals(4, data.getArgumentCount());
    }

    @Test
    @DisplayName("should return correct preopen directory count")
    void shouldReturnCorrectPreopenDirectoryCount() {
      Map<String, Path> dirs = Map.of("/a", Paths.get("/x"), "/b", Paths.get("/y"));
      WasiContextData data = new WasiContextData(null, null, dirs, null);

      assertEquals(2, data.getPreopenedDirectoryCount());
    }

    @Test
    @DisplayName("should return zero counts for empty data")
    void shouldReturnZeroCountsForEmptyData() {
      WasiContextData data = new WasiContextData(null, null, null, null);

      assertEquals(0, data.getEnvironmentCount());
      assertEquals(0, data.getArgumentCount());
      assertEquals(0, data.getPreopenedDirectoryCount());
    }
  }
}
