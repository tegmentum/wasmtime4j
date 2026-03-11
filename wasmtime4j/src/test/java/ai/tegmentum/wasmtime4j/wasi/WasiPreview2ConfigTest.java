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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiPreview2Config} and its inner {@link WasiPreview2Config.Builder} and {@link
 * WasiPreview2Config.PreopenDir}.
 */
@DisplayName("WasiPreview2Config Tests")
class WasiPreview2ConfigTest {

  @Nested
  @DisplayName("Builder Defaults")
  class BuilderDefaults {

    @Test
    @DisplayName("minimal config should have secure defaults")
    void minimalConfigShouldHaveSecureDefaults() {
      WasiPreview2Config config = WasiPreview2Config.minimal();

      assertNotNull(config);
      assertTrue(config.getArgs().isEmpty());
      assertFalse(config.isInheritArgs());
      assertTrue(config.getEnv().isEmpty());
      assertFalse(config.isInheritEnv());
      assertFalse(config.isInheritStdio());
      assertFalse(config.isInheritStdin());
      assertFalse(config.isInheritStdout());
      assertFalse(config.isInheritStderr());
      assertNull(config.getStdinConfig());
      assertNull(config.getStdoutConfig());
      assertNull(config.getStderrConfig());
      assertTrue(config.getPreopenDirs().isEmpty());
      assertFalse(config.isAllowNetwork());
      assertTrue(config.isAllowTcp());
      assertTrue(config.isAllowUdp());
      assertTrue(config.isAllowIpNameLookup());
      assertFalse(config.isAllowBlockingCurrentThread());
      assertFalse(config.hasInsecureRandomSeed());
      assertFalse(config.hasMaxRandomSize());
      assertNull(config.getWallClock());
      assertNull(config.getMonotonicClock());
      assertNull(config.getSecureRandom());
      assertNull(config.getInsecureRandom());
      assertNull(config.getSocketAddrCheck());
    }
  }

  @Nested
  @DisplayName("Inherited Config")
  class InheritedConfig {

    @Test
    @DisplayName("inherited config should enable stdio, args, and env")
    void inheritedConfigShouldEnableStdioArgsAndEnv() {
      WasiPreview2Config config = WasiPreview2Config.inherited();

      assertTrue(config.isInheritStdio());
      assertTrue(config.isInheritArgs());
      assertTrue(config.isInheritEnv());
    }
  }

  @Nested
  @DisplayName("Args Configuration")
  class ArgsConfiguration {

    @Test
    @DisplayName("should set args")
    void shouldSetArgs() {
      WasiPreview2Config config = WasiPreview2Config.builder().args("app", "--verbose").build();

      assertEquals(2, config.getArgs().size());
      assertEquals("app", config.getArgs().get(0));
      assertEquals("--verbose", config.getArgs().get(1));
    }

    @Test
    @DisplayName("should add args to existing")
    void shouldAddArgsToExisting() {
      WasiPreview2Config config =
          WasiPreview2Config.builder().args("app").addArgs("--debug", "--log").build();

      assertEquals(3, config.getArgs().size());
    }

    @Test
    @DisplayName("args should replace previous args")
    void argsShouldReplacePreviousArgs() {
      WasiPreview2Config config = WasiPreview2Config.builder().args("old").args("new").build();

      assertEquals(1, config.getArgs().size());
      assertEquals("new", config.getArgs().get(0));
    }
  }

  @Nested
  @DisplayName("Environment Configuration")
  class EnvironmentConfiguration {

    @Test
    @DisplayName("should set single env variable")
    void shouldSetSingleEnvVariable() {
      WasiPreview2Config config = WasiPreview2Config.builder().env("HOME", "/app").build();

      assertEquals("/app", config.getEnv().get("HOME"));
    }

    @Test
    @DisplayName("should set multiple env variables via map")
    void shouldSetMultipleEnvVariablesViaMap() {
      Map<String, String> envMap = Map.of("A", "1", "B", "2");

      WasiPreview2Config config = WasiPreview2Config.builder().env(envMap).build();

      assertEquals("1", config.getEnv().get("A"));
      assertEquals("2", config.getEnv().get("B"));
    }
  }

  @Nested
  @DisplayName("Stdio Configuration")
  class StdioConfiguration {

    @Test
    @DisplayName("should set individual stdio inheritance")
    void shouldSetIndividualStdioInheritance() {
      WasiPreview2Config config =
          WasiPreview2Config.builder().inheritStdin().inheritStdout().inheritStderr().build();

      assertTrue(config.isInheritStdin());
      assertTrue(config.isInheritStdout());
      assertTrue(config.isInheritStderr());
      assertFalse(config.isInheritStdio());
    }

    @Test
    @DisplayName("should set stdio configs")
    void shouldSetStdioConfigs() {
      WasiStdioConfig stdinCfg = WasiStdioConfig.inherit();
      WasiStdioConfig stdoutCfg = WasiStdioConfig.nulled();
      WasiStdioConfig stderrCfg = WasiStdioConfig.nulled();

      WasiPreview2Config config =
          WasiPreview2Config.builder().stdin(stdinCfg).stdout(stdoutCfg).stderr(stderrCfg).build();

      assertEquals(stdinCfg, config.getStdinConfig());
      assertEquals(stdoutCfg, config.getStdoutConfig());
      assertEquals(stderrCfg, config.getStderrConfig());
    }
  }

  @Nested
  @DisplayName("Network Configuration")
  class NetworkConfiguration {

    @Test
    @DisplayName("should configure network access")
    void shouldConfigureNetworkAccess() {
      WasiPreview2Config config =
          WasiPreview2Config.builder()
              .allowNetwork(true)
              .allowTcp(false)
              .allowUdp(false)
              .allowIpNameLookup(false)
              .build();

      assertTrue(config.isAllowNetwork());
      assertFalse(config.isAllowTcp());
      assertFalse(config.isAllowUdp());
      assertFalse(config.isAllowIpNameLookup());
    }

    @Test
    @DisplayName("should configure blocking current thread")
    void shouldConfigureBlockingCurrentThread() {
      WasiPreview2Config config =
          WasiPreview2Config.builder().allowBlockingCurrentThread(true).build();

      assertTrue(config.isAllowBlockingCurrentThread());
    }
  }

  @Nested
  @DisplayName("Random Configuration")
  class RandomConfiguration {

    @Test
    @DisplayName("should set insecure random seed")
    void shouldSetInsecureRandomSeed() {
      WasiPreview2Config config = WasiPreview2Config.builder().insecureRandomSeed(42L).build();

      assertTrue(config.hasInsecureRandomSeed());
      assertEquals(42L, config.getInsecureRandomSeed());
    }

    @Test
    @DisplayName("should set max random size")
    void shouldSetMaxRandomSize() {
      WasiPreview2Config config = WasiPreview2Config.builder().maxRandomSize(1024).build();

      assertTrue(config.hasMaxRandomSize());
      assertEquals(1024, config.getMaxRandomSize());
    }

    @Test
    @DisplayName("should throw when max random size is zero")
    void shouldThrowWhenMaxRandomSizeIsZero() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiPreview2Config.builder().maxRandomSize(0));
    }

    @Test
    @DisplayName("should throw when max random size is negative")
    void shouldThrowWhenMaxRandomSizeIsNegative() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiPreview2Config.builder().maxRandomSize(-1));
    }
  }

  @Nested
  @DisplayName("PreopenDir Tests")
  class PreopenDirTests {

    @Test
    @DisplayName("should create read-write preopen dir")
    void shouldCreateReadWritePreopenDir() {
      WasiPreview2Config.PreopenDir dir =
          new WasiPreview2Config.PreopenDir(Paths.get("/tmp"), "/sandbox", false);

      assertEquals(Paths.get("/tmp"), dir.getHostPath());
      assertEquals("/sandbox", dir.getGuestPath());
      assertFalse(dir.isReadOnly());
    }

    @Test
    @DisplayName("should create read-only preopen dir")
    void shouldCreateReadOnlyPreopenDir() {
      WasiPreview2Config.PreopenDir dir =
          new WasiPreview2Config.PreopenDir(Paths.get("/tmp"), "/sandbox", true);

      assertTrue(dir.isReadOnly());
    }

    @Test
    @DisplayName("should create preopen dir with granular permissions")
    void shouldCreatePreopenDirWithGranularPermissions() {
      DirPerms dirPerms = DirPerms.readOnly();
      FilePerms filePerms = FilePerms.readOnly();

      WasiPreview2Config.PreopenDir dir =
          new WasiPreview2Config.PreopenDir(Paths.get("/tmp"), "/sandbox", dirPerms, filePerms);

      assertEquals(dirPerms, dir.getDirPerms());
      assertEquals(filePerms, dir.getFilePerms());
      assertTrue(dir.isReadOnly());
    }

    @Test
    @DisplayName("should create preopen dir with full permissions")
    void shouldCreatePreopenDirWithFullPermissions() {
      DirPerms dirPerms = DirPerms.all();
      FilePerms filePerms = FilePerms.all();

      WasiPreview2Config.PreopenDir dir =
          new WasiPreview2Config.PreopenDir(Paths.get("/tmp"), "/sandbox", dirPerms, filePerms);

      assertFalse(dir.isReadOnly());
    }
  }

  @Nested
  @DisplayName("Builder Preopen Dir")
  class BuilderPreopenDir {

    @Test
    @DisplayName("should add preopen dir via builder with Path")
    void shouldAddPreopenDirViaBuilderWithPath() {
      WasiPreview2Config config =
          WasiPreview2Config.builder().preopenDir(Paths.get("/tmp"), "/sandbox").build();

      assertEquals(1, config.getPreopenDirs().size());
      assertEquals("/sandbox", config.getPreopenDirs().get(0).getGuestPath());
      assertFalse(config.getPreopenDirs().get(0).isReadOnly());
    }

    @Test
    @DisplayName("should add preopen dir via builder with String")
    void shouldAddPreopenDirViaBuilderWithString() {
      WasiPreview2Config config =
          WasiPreview2Config.builder().preopenDir("/tmp", "/sandbox").build();

      assertEquals(1, config.getPreopenDirs().size());
    }

    @Test
    @DisplayName("should add read-only preopen dir via builder")
    void shouldAddReadOnlyPreopenDirViaBuilder() {
      WasiPreview2Config config =
          WasiPreview2Config.builder().preopenDir(Paths.get("/tmp"), "/sandbox", true).build();

      assertTrue(config.getPreopenDirs().get(0).isReadOnly());
    }
  }

  @Nested
  @DisplayName("Custom Providers")
  class CustomProviders {

    @Test
    @DisplayName("should set custom random sources")
    void shouldSetCustomRandomSources() {
      ai.tegmentum.wasmtime4j.wasi.random.WasiRandomSource source = (dest) -> {};

      WasiPreview2Config config =
          WasiPreview2Config.builder().secureRandom(source).insecureRandom(source).build();

      assertNotNull(config.getSecureRandom());
      assertNotNull(config.getInsecureRandom());
    }

    @Test
    @DisplayName("should set socket addr check")
    void shouldSetSocketAddrCheck() {
      ai.tegmentum.wasmtime4j.wasi.sockets.SocketAddrCheck check = (addr, use) -> true;

      WasiPreview2Config config = WasiPreview2Config.builder().socketAddrCheck(check).build();

      assertNotNull(config.getSocketAddrCheck());
    }
  }
}
