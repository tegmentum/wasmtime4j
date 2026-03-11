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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link AbstractWasiContextBuilder} via a concrete test subclass.
 */
@DisplayName("AbstractWasiContextBuilder Tests")
class AbstractWasiContextBuilderTest {

  /** Concrete test subclass to test the abstract builder. */
  private static final class TestBuilder extends AbstractWasiContextBuilder<TestBuilder> {}

  @Nested
  @DisplayName("Default State")
  class DefaultState {

    @Test
    @DisplayName("should have empty environment by default")
    void shouldHaveEmptyEnvironmentByDefault() {
      TestBuilder builder = new TestBuilder();
      assertTrue(builder.getEnvironment().isEmpty());
    }

    @Test
    @DisplayName("should have empty arguments by default")
    void shouldHaveEmptyArgumentsByDefault() {
      TestBuilder builder = new TestBuilder();
      assertTrue(builder.getArguments().isEmpty());
    }

    @Test
    @DisplayName("should have empty preopened directories by default")
    void shouldHaveEmptyPreopenedDirectoriesByDefault() {
      TestBuilder builder = new TestBuilder();
      assertTrue(builder.getPreopenedDirectories().isEmpty());
    }

    @Test
    @DisplayName("should have default working directory of /")
    void shouldHaveDefaultWorkingDirectory() {
      TestBuilder builder = new TestBuilder();
      assertEquals(Paths.get("/"), builder.getWorkingDirectory());
    }
  }

  @Nested
  @DisplayName("Environment Variables")
  class EnvironmentVariables {

    @Test
    @DisplayName("should add single environment variable")
    void shouldAddSingleEnvironmentVariable() {
      TestBuilder builder = new TestBuilder();
      builder.withEnvironment("HOME", "/app");

      Map<String, String> env = builder.getEnvironment();
      assertEquals(1, env.size());
      assertEquals("/app", env.get("HOME"));
    }

    @Test
    @DisplayName("should add multiple environment variables via map")
    void shouldAddMultipleEnvironmentVariablesViaMap() {
      Map<String, String> envMap = new HashMap<>();
      envMap.put("A", "1");
      envMap.put("B", "2");

      TestBuilder builder = new TestBuilder();
      builder.withEnvironment(envMap);

      assertEquals(2, builder.getEnvironment().size());
    }

    @Test
    @DisplayName("should return defensive copy of environment")
    void shouldReturnDefensiveCopyOfEnvironment() {
      TestBuilder builder = new TestBuilder();
      builder.withEnvironment("KEY", "value");

      Map<String, String> env = builder.getEnvironment();
      env.put("EXTRA", "modified");

      assertEquals(1, builder.getEnvironment().size(),
          "External modification should not affect builder state");
    }

    @Test
    @DisplayName("fluent chaining should work")
    void fluentChainingShouldWork() {
      TestBuilder builder = new TestBuilder();
      TestBuilder result = builder.withEnvironment("KEY", "value");

      assertEquals(builder, result, "withEnvironment should return same builder");
    }
  }

  @Nested
  @DisplayName("Arguments")
  class Arguments {

    @Test
    @DisplayName("should add single argument")
    void shouldAddSingleArgument() {
      TestBuilder builder = new TestBuilder();
      builder.withArgument("--verbose");

      List<String> args = builder.getArguments();
      assertEquals(1, args.size());
      assertEquals("--verbose", args.get(0));
    }

    @Test
    @DisplayName("should add multiple arguments")
    void shouldAddMultipleArguments() {
      TestBuilder builder = new TestBuilder();
      builder.withArguments("--verbose", "--debug");

      assertEquals(2, builder.getArguments().size());
    }

    @Test
    @DisplayName("should return defensive copy of arguments")
    void shouldReturnDefensiveCopyOfArguments() {
      TestBuilder builder = new TestBuilder();
      builder.withArgument("arg1");

      List<String> args = builder.getArguments();
      args.add("extra");

      assertEquals(1, builder.getArguments().size());
    }
  }

  @Nested
  @DisplayName("Preopened Directories")
  class PreopenedDirectories {

    @Test
    @DisplayName("should add preopened directory with guest and host paths")
    void shouldAddPreopenedDirectory(@TempDir File tempDir) {
      TestBuilder builder = new TestBuilder();
      String hostDir = tempDir.getAbsolutePath();
      builder.withPreopenDirectory("/guest", hostDir);

      Map<String, Path> dirs = builder.getPreopenedDirectories();
      assertEquals(1, dirs.size());
      assertTrue(dirs.containsKey("/guest"));
    }

    @Test
    @DisplayName("should add preopened directory with same path")
    void shouldAddPreopenedDirectoryWithSamePath(@TempDir File tempDir) {
      TestBuilder builder = new TestBuilder();
      String dir = tempDir.getAbsolutePath();
      builder.withPreopenDirectory(dir);

      assertEquals(1, builder.getPreopenedDirectories().size());
    }

    @Test
    @DisplayName("should throw when host directory does not exist")
    void shouldThrowWhenHostDirectoryDoesNotExist() {
      TestBuilder builder = new TestBuilder();
      assertThrows(IllegalArgumentException.class,
          () -> builder.withPreopenDirectory("/guest", "/nonexistent/path/abc123"));
    }

    @Test
    @DisplayName("should return defensive copy")
    void shouldReturnDefensiveCopy(@TempDir File tempDir) {
      TestBuilder builder = new TestBuilder();
      builder.withPreopenDirectory("/guest", tempDir.getAbsolutePath());

      Map<String, Path> dirs = builder.getPreopenedDirectories();
      dirs.put("/extra", Paths.get("/somewhere"));

      assertEquals(1, builder.getPreopenedDirectories().size());
    }
  }

  @Nested
  @DisplayName("Working Directory")
  class WorkingDirectory {

    @Test
    @DisplayName("should set working directory")
    void shouldSetWorkingDirectory() {
      TestBuilder builder = new TestBuilder();
      builder.withWorkingDirectory("/app");

      assertEquals(Paths.get("/app").normalize(), builder.getWorkingDirectory());
    }
  }

  @Nested
  @DisplayName("Network Configuration")
  class NetworkConfiguration {

    @Test
    @DisplayName("should configure network access")
    void shouldConfigureNetworkAccess() {
      TestBuilder builder = new TestBuilder();
      TestBuilder result = builder.withAllowNetwork(true);

      assertEquals(builder, result);
    }

    @Test
    @DisplayName("should configure TCP access")
    void shouldConfigureTcpAccess() {
      TestBuilder builder = new TestBuilder();
      TestBuilder result = builder.withAllowTcp(false);

      assertEquals(builder, result);
    }

    @Test
    @DisplayName("should configure UDP access")
    void shouldConfigureUdpAccess() {
      TestBuilder builder = new TestBuilder();
      TestBuilder result = builder.withAllowUdp(false);

      assertEquals(builder, result);
    }

    @Test
    @DisplayName("should configure IP name lookup")
    void shouldConfigureIpNameLookup() {
      TestBuilder builder = new TestBuilder();
      TestBuilder result = builder.withAllowIpNameLookup(false);

      assertEquals(builder, result);
    }

    @Test
    @DisplayName("should configure blocking current thread")
    void shouldConfigureBlockingCurrentThread() {
      TestBuilder builder = new TestBuilder();
      TestBuilder result = builder.withAllowBlockingCurrentThread(true);

      assertEquals(builder, result);
    }
  }

  @Nested
  @DisplayName("Insecure Random Seed")
  class InsecureRandomSeed {

    @Test
    @DisplayName("should set insecure random seed")
    void shouldSetInsecureRandomSeed() {
      TestBuilder builder = new TestBuilder();
      TestBuilder result = builder.withInsecureRandomSeed(42L);

      assertEquals(builder, result);
    }
  }

  @Nested
  @DisplayName("Convert Methods")
  class ConvertMethods {

    @Test
    @DisplayName("convertEnvironmentToArray should produce key=value pairs")
    void convertEnvironmentToArrayShouldProduceKeyValuePairs() {
      TestBuilder builder = new TestBuilder();
      builder.withEnvironment("KEY", "value");

      String[] arr = builder.convertEnvironmentToArray();
      assertEquals(1, arr.length);
      assertEquals("KEY=value", arr[0]);
    }

    @Test
    @DisplayName("convertPreopenDirectoriesToArray should produce alternating guest/host pairs")
    void convertPreopenDirectoriesToArrayShouldProducePairs(@TempDir File tempDir) {
      TestBuilder builder = new TestBuilder();
      builder.withPreopenDirectory("/guest", tempDir.getAbsolutePath());

      String[] arr = builder.convertPreopenDirectoriesToArray();
      assertEquals(2, arr.length);
      assertEquals("/guest", arr[0]);
      assertNotNull(arr[1]);
    }
  }

  @Nested
  @DisplayName("Inherit Environment")
  class InheritEnvironment {

    @Test
    @DisplayName("should inherit environment variables from host")
    void shouldInheritEnvironmentVariablesFromHost() {
      TestBuilder builder = new TestBuilder();
      TestBuilder result = builder.withInheritedEnvironment();

      assertEquals(builder, result);
      assertFalse(builder.getEnvironment().isEmpty(),
          "After inheriting, environment should contain host env vars");
    }
  }
}
