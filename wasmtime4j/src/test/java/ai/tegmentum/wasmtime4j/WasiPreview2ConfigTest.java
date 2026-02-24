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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiPreview2Config;
import ai.tegmentum.wasmtime4j.wasi.WasiStdioConfig;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link WasiPreview2Config} Preview2 configuration. */
@DisplayName("WasiPreview2Config")
final class WasiPreview2ConfigTest {

  @Nested
  @DisplayName("minimal configuration")
  final class MinimalConfigTests {

    @Test
    @DisplayName("should create minimal config with empty args")
    void shouldCreateMinimalConfigWithEmptyArgs() {
      final WasiPreview2Config config = WasiPreview2Config.minimal();
      assertNotNull(config, "Minimal config should not be null");
      assertTrue(config.getArgs().isEmpty(), "Minimal config should have no args");
    }

    @Test
    @DisplayName("should create minimal config with empty env")
    void shouldCreateMinimalConfigWithEmptyEnv() {
      final WasiPreview2Config config = WasiPreview2Config.minimal();
      assertTrue(config.getEnv().isEmpty(), "Minimal config should have no environment variables");
    }

    @Test
    @DisplayName("should create minimal config with all permissions disabled")
    void shouldCreateMinimalConfigWithAllDisabled() {
      final WasiPreview2Config config = WasiPreview2Config.minimal();
      assertFalse(config.isInheritEnv(), "Minimal config should not inherit env");
      assertFalse(config.isInheritStdio(), "Minimal config should not inherit stdio");
      assertFalse(config.isAllowNetwork(), "Minimal config should not allow network");
      assertFalse(config.isAllowClock(), "Minimal config should not allow clock");
      assertFalse(config.isAllowRandom(), "Minimal config should not allow random");
    }

    @Test
    @DisplayName("should create minimal config with no preopened dirs")
    void shouldCreateMinimalConfigWithNoPreopenDirs() {
      final WasiPreview2Config config = WasiPreview2Config.minimal();
      assertTrue(config.getPreopenDirs().isEmpty(), "Minimal config should have no preopened dirs");
    }
  }

  @Nested
  @DisplayName("inherited configuration")
  final class InheritedConfigTests {

    @Test
    @DisplayName("should create inherited config with stdio and env enabled")
    void shouldCreateInheritedConfigWithStdioAndEnv() {
      final WasiPreview2Config config = WasiPreview2Config.inherited();
      assertTrue(config.isInheritStdio(), "Inherited config should inherit stdio");
      assertTrue(config.isInheritEnv(), "Inherited config should inherit env");
    }

    @Test
    @DisplayName("should create inherited config with clock and random enabled")
    void shouldCreateInheritedConfigWithClockAndRandom() {
      final WasiPreview2Config config = WasiPreview2Config.inherited();
      assertTrue(config.isAllowClock(), "Inherited config should allow clock");
      assertTrue(config.isAllowRandom(), "Inherited config should allow random");
    }
  }

  @Nested
  @DisplayName("builder args methods")
  final class BuilderArgsTests {

    @Test
    @DisplayName("should set args via varargs")
    void shouldSetArgsViaVarargs() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder().args("arg1", "arg2", "arg3").build();
      assertEquals(3, config.getArgs().size(), "Should have 3 args");
      assertEquals("arg1", config.getArgs().get(0), "First arg should be arg1");
      assertEquals("arg2", config.getArgs().get(1), "Second arg should be arg2");
      assertEquals("arg3", config.getArgs().get(2), "Third arg should be arg3");
    }

    @Test
    @DisplayName("should replace args when called multiple times")
    void shouldReplaceArgs() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder().args("first").args("second").build();
      assertEquals(1, config.getArgs().size(), "Should have 1 arg after replacement");
      assertEquals("second", config.getArgs().get(0), "Arg should be the replacement value");
    }

    @Test
    @DisplayName("should add args incrementally")
    void shouldAddArgsIncrementally() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder().args("base").addArgs("extra1", "extra2").build();
      assertEquals(3, config.getArgs().size(), "Should have 3 args after addArgs");
      assertEquals("base", config.getArgs().get(0), "First arg should be base");
      assertEquals("extra1", config.getArgs().get(1), "Second arg should be extra1");
    }
  }

  @Nested
  @DisplayName("builder env methods")
  final class BuilderEnvTests {

    @Test
    @DisplayName("should set single environment variable")
    void shouldSetSingleEnvVar() {
      final WasiPreview2Config config = WasiPreview2Config.builder().env("KEY", "VALUE").build();
      assertEquals(1, config.getEnv().size(), "Should have 1 environment variable");
      assertEquals("VALUE", config.getEnv().get("KEY"), "Environment variable KEY should be VALUE");
    }

    @Test
    @DisplayName("should set multiple environment variables from map")
    void shouldSetMultipleEnvVars() {
      final Map<String, String> envMap = Map.of("A", "1", "B", "2");
      final WasiPreview2Config config = WasiPreview2Config.builder().env(envMap).build();
      assertEquals(2, config.getEnv().size(), "Should have 2 environment variables");
      assertEquals("1", config.getEnv().get("A"), "Environment variable A should be 1");
      assertEquals("2", config.getEnv().get("B"), "Environment variable B should be 2");
    }
  }

  @Nested
  @DisplayName("builder preopenDir methods")
  final class BuilderPreopenDirTests {

    @Test
    @DisplayName("should add preopened directory with Path")
    void shouldAddPreopenDirWithPath() {
      final Path hostPath = Path.of("/tmp");
      final WasiPreview2Config config =
          WasiPreview2Config.builder().preopenDir(hostPath, "/sandbox/tmp").build();
      assertEquals(1, config.getPreopenDirs().size(), "Should have 1 preopened dir");
      final WasiPreview2Config.PreopenDir dir = config.getPreopenDirs().get(0);
      assertEquals(hostPath, dir.getHostPath(), "Host path should match");
      assertEquals("/sandbox/tmp", dir.getGuestPath(), "Guest path should match");
      assertFalse(dir.isReadOnly(), "Default preopened dir should be read-write");
    }

    @Test
    @DisplayName("should add preopened directory with String path")
    void shouldAddPreopenDirWithStringPath() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder().preopenDir("/tmp", "/sandbox/tmp").build();
      assertEquals(1, config.getPreopenDirs().size(), "Should have 1 preopened dir");
      assertEquals(
          Path.of("/tmp"),
          config.getPreopenDirs().get(0).getHostPath(),
          "Host path should be /tmp");
    }

    @Test
    @DisplayName("should add read-only preopened directory")
    void shouldAddReadOnlyPreopenDir() {
      final Path hostPath = Path.of("/data");
      final WasiPreview2Config config =
          WasiPreview2Config.builder().preopenDir(hostPath, "/sandbox/data", true).build();
      assertTrue(config.getPreopenDirs().get(0).isReadOnly(), "Preopened dir should be read-only");
    }
  }

  @Nested
  @DisplayName("builder permission flags")
  final class BuilderPermissionFlagsTests {

    @Test
    @DisplayName("should enable network access")
    void shouldEnableNetwork() {
      final WasiPreview2Config config = WasiPreview2Config.builder().allowNetwork(true).build();
      assertTrue(config.isAllowNetwork(), "Network should be allowed");
    }

    @Test
    @DisplayName("should enable clock access")
    void shouldEnableClock() {
      final WasiPreview2Config config = WasiPreview2Config.builder().allowClock(true).build();
      assertTrue(config.isAllowClock(), "Clock should be allowed");
    }

    @Test
    @DisplayName("should enable random access")
    void shouldEnableRandom() {
      final WasiPreview2Config config = WasiPreview2Config.builder().allowRandom(true).build();
      assertTrue(config.isAllowRandom(), "Random should be allowed");
    }

    @Test
    @DisplayName("should enable stdio inheritance")
    void shouldEnableStdioInheritance() {
      final WasiPreview2Config config = WasiPreview2Config.builder().inheritStdio().build();
      assertTrue(config.isInheritStdio(), "Stdio should be inherited");
    }

    @Test
    @DisplayName("should enable env inheritance")
    void shouldEnableEnvInheritance() {
      final WasiPreview2Config config = WasiPreview2Config.builder().inheritEnv().build();
      assertTrue(config.isInheritEnv(), "Env should be inherited");
    }
  }

  @Nested
  @DisplayName("PreopenDir data class")
  final class PreopenDirTests {

    @Test
    @DisplayName("should store all fields correctly")
    void shouldStoreAllFieldsCorrectly() {
      final Path hostPath = Path.of("/usr/local");
      final WasiPreview2Config.PreopenDir dir =
          new WasiPreview2Config.PreopenDir(hostPath, "/guest/local", true);
      assertEquals(hostPath, dir.getHostPath(), "Host path should match");
      assertEquals("/guest/local", dir.getGuestPath(), "Guest path should match");
      assertTrue(dir.isReadOnly(), "Read-only flag should be true");
    }
  }

  @Nested
  @DisplayName("builder stdio config methods")
  final class BuilderStdioConfigTests {

    @Test
    @DisplayName("should default to null stdio configs")
    void shouldDefaultToNullStdioConfigs() {
      final WasiPreview2Config config = WasiPreview2Config.builder().build();
      assertNull(config.getStdinConfig(), "Default stdin config should be null");
      assertNull(config.getStdoutConfig(), "Default stdout config should be null");
      assertNull(config.getStderrConfig(), "Default stderr config should be null");
    }

    @Test
    @DisplayName("should set stdin to inherit")
    void shouldSetStdinToInherit() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder().stdin(WasiStdioConfig.inherit()).build();
      assertNotNull(config.getStdinConfig(), "Stdin config should not be null");
      assertEquals(
          WasiStdioConfig.Type.INHERIT,
          config.getStdinConfig().getType(),
          "Stdin should be INHERIT type");
    }

    @Test
    @DisplayName("should set stdin from input stream")
    void shouldSetStdinFromInputStream() {
      final ByteArrayInputStream bais = new ByteArrayInputStream("hello".getBytes());
      final WasiPreview2Config config =
          WasiPreview2Config.builder().stdin(WasiStdioConfig.fromInputStream(bais)).build();
      assertNotNull(config.getStdinConfig(), "Stdin config should not be null");
      assertEquals(
          WasiStdioConfig.Type.INPUT_STREAM,
          config.getStdinConfig().getType(),
          "Stdin should be INPUT_STREAM type");
      assertEquals(bais, config.getStdinConfig().getInputStream(), "InputStream should match");
    }

    @Test
    @DisplayName("should set stdin to null (empty)")
    void shouldSetStdinToNull() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder().stdin(WasiStdioConfig.nulled()).build();
      assertNotNull(config.getStdinConfig(), "Stdin config should not be null");
      assertEquals(
          WasiStdioConfig.Type.NULL,
          config.getStdinConfig().getType(),
          "Stdin should be NULL type");
    }

    @Test
    @DisplayName("should set stdout to inherit")
    void shouldSetStdoutToInherit() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder().stdout(WasiStdioConfig.inherit()).build();
      assertNotNull(config.getStdoutConfig(), "Stdout config should not be null");
      assertEquals(
          WasiStdioConfig.Type.INHERIT,
          config.getStdoutConfig().getType(),
          "Stdout should be INHERIT type");
    }

    @Test
    @DisplayName("should set stderr to inherit")
    void shouldSetStderrToInherit() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder().stderr(WasiStdioConfig.inherit()).build();
      assertNotNull(config.getStderrConfig(), "Stderr config should not be null");
      assertEquals(
          WasiStdioConfig.Type.INHERIT,
          config.getStderrConfig().getType(),
          "Stderr should be INHERIT type");
    }

    @Test
    @DisplayName("should set all three stdio configs independently")
    void shouldSetAllThreeStdioConfigs() {
      final ByteArrayInputStream bais = new ByteArrayInputStream("input".getBytes());
      final WasiPreview2Config config =
          WasiPreview2Config.builder()
              .stdin(WasiStdioConfig.fromInputStream(bais))
              .stdout(WasiStdioConfig.inherit())
              .stderr(WasiStdioConfig.nulled())
              .build();
      assertEquals(
          WasiStdioConfig.Type.INPUT_STREAM,
          config.getStdinConfig().getType(),
          "Stdin should be INPUT_STREAM");
      assertEquals(
          WasiStdioConfig.Type.INHERIT,
          config.getStdoutConfig().getType(),
          "Stdout should be INHERIT");
      assertEquals(
          WasiStdioConfig.Type.NULL,
          config.getStderrConfig().getType(),
          "Stderr should be NULL");
    }

    @Test
    @DisplayName("minimal config should have null stdio configs")
    void minimalConfigShouldHaveNullStdioConfigs() {
      final WasiPreview2Config config = WasiPreview2Config.minimal();
      assertNull(config.getStdinConfig(), "Minimal stdin config should be null");
      assertNull(config.getStdoutConfig(), "Minimal stdout config should be null");
      assertNull(config.getStderrConfig(), "Minimal stderr config should be null");
    }

    @Test
    @DisplayName("inherited config should have null stdio configs (uses boolean flags instead)")
    void inheritedConfigShouldHaveNullStdioConfigs() {
      final WasiPreview2Config config = WasiPreview2Config.inherited();
      assertNull(
          config.getStdinConfig(),
          "Inherited config should use boolean flags, not WasiStdioConfig");
      assertNull(
          config.getStdoutConfig(),
          "Inherited config should use boolean flags, not WasiStdioConfig");
      assertNull(
          config.getStderrConfig(),
          "Inherited config should use boolean flags, not WasiStdioConfig");
    }

    @Test
    @DisplayName("stdio config should override boolean inherit flags")
    void stdioConfigShouldWorkAlongsideBooleanFlags() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder()
              .inheritStdio()
              .stdin(WasiStdioConfig.nulled())
              .build();
      // Both are set — implementations should prefer WasiStdioConfig when present
      assertTrue(config.isInheritStdio(), "Boolean inheritStdio should still be true");
      assertNotNull(
          config.getStdinConfig(), "WasiStdioConfig stdin should also be set");
      assertEquals(
          WasiStdioConfig.Type.NULL,
          config.getStdinConfig().getType(),
          "WasiStdioConfig should be NULL type");
    }
  }
}
