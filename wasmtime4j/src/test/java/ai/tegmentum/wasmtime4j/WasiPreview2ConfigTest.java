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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiPreview2Config;
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
      assertTrue(
          config.getPreopenDirs().isEmpty(), "Minimal config should have no preopened dirs");
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
      final WasiPreview2Config config =
          WasiPreview2Config.builder().env("KEY", "VALUE").build();
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
      assertTrue(
          config.getPreopenDirs().get(0).isReadOnly(),
          "Preopened dir should be read-only");
    }
  }

  @Nested
  @DisplayName("builder permission flags")
  final class BuilderPermissionFlagsTests {

    @Test
    @DisplayName("should enable network access")
    void shouldEnableNetwork() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder().allowNetwork(true).build();
      assertTrue(config.isAllowNetwork(), "Network should be allowed");
    }

    @Test
    @DisplayName("should enable clock access")
    void shouldEnableClock() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder().allowClock(true).build();
      assertTrue(config.isAllowClock(), "Clock should be allowed");
    }

    @Test
    @DisplayName("should enable random access")
    void shouldEnableRandom() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder().allowRandom(true).build();
      assertTrue(config.isAllowRandom(), "Random should be allowed");
    }

    @Test
    @DisplayName("should enable stdio inheritance")
    void shouldEnableStdioInheritance() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder().inheritStdio().build();
      assertTrue(config.isInheritStdio(), "Stdio should be inherited");
    }

    @Test
    @DisplayName("should enable env inheritance")
    void shouldEnableEnvInheritance() {
      final WasiPreview2Config config =
          WasiPreview2Config.builder().inheritEnv().build();
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
}
