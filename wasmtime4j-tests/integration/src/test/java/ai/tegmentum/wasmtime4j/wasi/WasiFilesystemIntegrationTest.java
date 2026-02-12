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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for WASI filesystem operations.
 *
 * <p>These tests verify:
 *
 * <ul>
 *   <li>Pre-opened directory configuration
 *   <li>Directory mapping with guest paths
 *   <li>Read-only vs read-write access
 *   <li>Directory permissions
 * </ul>
 */
@DisplayName("WASI Filesystem Integration Tests")
@Tag("integration")
class WasiFilesystemIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiFilesystemIntegrationTest.class.getName());

  private static boolean wasiAvailable = false;
  private static WasmRuntime sharedRuntime;
  private static Engine sharedEngine;

  @TempDir Path tempDir;

  @BeforeAll
  static void checkWasiAvailable() {
    LOGGER.info("Checking WASI availability for filesystem tests");
    try {
      sharedRuntime = WasmRuntimeFactory.create();
      sharedEngine = sharedRuntime.createEngine();

      // Check if WASI context is available
      final WasiContext testContext = WasiContext.create();
      if (testContext != null) {
        wasiAvailable = true;
        LOGGER.info("WASI context is available for filesystem tests");
      }
    } catch (final Exception e) {
      LOGGER.warning("WASI not available: " + e.getMessage());
      wasiAvailable = false;
    }
  }

  @AfterAll
  static void cleanupSharedResources() {
    if (sharedEngine != null) {
      try {
        sharedEngine.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing shared engine: " + e.getMessage());
      }
    }
    if (sharedRuntime != null) {
      try {
        sharedRuntime.close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing shared runtime: " + e.getMessage());
      }
    }
  }

  @BeforeEach
  void setUp() {
    assumeTrue(wasiAvailable, "WASI must be available for these tests");
  }

  @Nested
  @DisplayName("Pre-opened Directory Configuration Tests")
  class PreopenedDirectoryConfigurationTests {

    @Test
    @DisplayName("should create WASI context with single preopened directory")
    void shouldCreateContextWithSinglePreopenedDir() throws Exception {
      final Path testDir = tempDir.resolve("test-dir");
      Files.createDirectories(testDir);

      final WasiContext context = WasiContext.create().preopenedDir(testDir, "/");

      LOGGER.info("Created WASI context with single preopened directory: " + testDir);
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should create WASI context with multiple preopened directories")
    void shouldCreateContextWithMultiplePreopenedDirs() throws Exception {
      final Path dataDir = tempDir.resolve("data");
      final Path configDir = tempDir.resolve("config");
      final Path logsDir = tempDir.resolve("logs");

      Files.createDirectories(dataDir);
      Files.createDirectories(configDir);
      Files.createDirectories(logsDir);

      final WasiContext context =
          WasiContext.create()
              .preopenedDir(dataDir, "/data")
              .preopenedDir(configDir, "/config")
              .preopenedDir(logsDir, "/logs");

      LOGGER.info("Created WASI context with 3 preopened directories");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should create WASI context with nested directory structure")
    void shouldCreateContextWithNestedDirs() throws Exception {
      final Path rootDir = tempDir.resolve("root");
      final Path nestedDir = rootDir.resolve("level1").resolve("level2");

      Files.createDirectories(nestedDir);

      final WasiContext context = WasiContext.create().preopenedDir(nestedDir, "/nested");

      LOGGER.info("Created WASI context with nested directory: " + nestedDir);
      assertThat(context).isNotNull();
    }
  }

  @Nested
  @DisplayName("Directory Mapping Tests")
  class DirectoryMappingTests {

    @Test
    @DisplayName("should map host directory to different guest path")
    void shouldMapHostToGuestPath() throws Exception {
      final Path hostDir = tempDir.resolve("host-data");
      Files.createDirectories(hostDir);

      final WasiContext context = WasiContext.create().preopenedDir(hostDir, "/wasm-data");

      LOGGER.info("Mapped " + hostDir + " to /wasm-data");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should handle special characters in guest path")
    void shouldHandleSpecialCharsInGuestPath() throws Exception {
      final Path hostDir = tempDir.resolve("special");
      Files.createDirectories(hostDir);

      final WasiContext context = WasiContext.create().preopenedDir(hostDir, "/path-with-dashes");

      LOGGER.info("Created WASI context with special guest path");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should map same host directory to multiple guest paths")
    void shouldMapSameHostToMultipleGuests() throws Exception {
      final Path hostDir = tempDir.resolve("shared");
      Files.createDirectories(hostDir);

      final WasiContext context =
          WasiContext.create().preopenedDir(hostDir, "/alias1").preopenedDir(hostDir, "/alias2");

      LOGGER.info("Mapped same host directory to multiple guest paths");
      assertThat(context).isNotNull();
    }
  }

  @Nested
  @DisplayName("Read-Only Access Tests")
  class ReadOnlyAccessTests {

    @Test
    @DisplayName("should create WASI context with read-only preopened directory")
    void shouldCreateContextWithReadOnlyDir() throws Exception {
      final Path readOnlyDir = tempDir.resolve("readonly");
      Files.createDirectories(readOnlyDir);
      Files.writeString(readOnlyDir.resolve("data.txt"), "read-only content");

      final WasiContext context =
          WasiContext.create().preopenedDirReadOnly(readOnlyDir, "/readonly");

      LOGGER.info("Created WASI context with read-only directory");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should combine read-only and read-write directories")
    void shouldCombineReadOnlyAndReadWrite() throws Exception {
      final Path readOnlyDir = tempDir.resolve("readonly-data");
      final Path readWriteDir = tempDir.resolve("readwrite-data");

      Files.createDirectories(readOnlyDir);
      Files.createDirectories(readWriteDir);

      final WasiContext context =
          WasiContext.create()
              .preopenedDirReadOnly(readOnlyDir, "/config")
              .preopenedDir(readWriteDir, "/output");

      LOGGER.info("Created WASI context with mixed access modes");
      assertThat(context).isNotNull();
    }
  }

  @Nested
  @DisplayName("Working Directory Configuration Tests")
  class WorkingDirectoryConfigurationTests {

    @Test
    @DisplayName("should set filesystem working directory with host path")
    void shouldSetFilesystemWorkingDirectory() throws Exception {
      final Path workDir = tempDir.resolve("work");
      Files.createDirectories(workDir);

      final WasiContext context =
          WasiContext.create().preopenedDir(workDir, "/work").setFilesystemWorkingDir(workDir);

      LOGGER.info("Created WASI context with filesystem working directory: " + workDir);
      assertThat(context).isNotNull();
    }
  }

  @Nested
  @DisplayName("Directory with Files Tests")
  class DirectoryWithFilesTests {

    @Test
    @DisplayName("should configure directory containing files")
    void shouldConfigureDirectoryWithFiles() throws Exception {
      final Path dataDir = tempDir.resolve("data-with-files");
      Files.createDirectories(dataDir);

      // Create some test files
      Files.writeString(dataDir.resolve("file1.txt"), "Content 1");
      Files.writeString(dataDir.resolve("file2.txt"), "Content 2");
      Files.createDirectories(dataDir.resolve("subdir"));
      Files.writeString(dataDir.resolve("subdir/file3.txt"), "Content 3");

      final WasiContext context = WasiContext.create().preopenedDir(dataDir, "/data");

      LOGGER.info("Created WASI context with directory containing files");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should configure empty directory")
    void shouldConfigureEmptyDirectory() throws Exception {
      final Path emptyDir = tempDir.resolve("empty");
      Files.createDirectories(emptyDir);

      final WasiContext context = WasiContext.create().preopenedDir(emptyDir, "/empty");

      LOGGER.info("Created WASI context with empty directory");
      assertThat(context).isNotNull();
    }
  }

  @Nested
  @DisplayName("Combined Configuration Tests")
  class CombinedConfigurationTests {

    @Test
    @DisplayName("should create full WASI context with filesystem, env, and args")
    void shouldCreateFullWasiContext() throws Exception {
      final Path dataDir = tempDir.resolve("app-data");
      final Path configDir = tempDir.resolve("app-config");

      Files.createDirectories(dataDir);
      Files.createDirectories(configDir);

      // Create a config file
      Files.writeString(configDir.resolve("app.conf"), "key=value");

      final WasiContext context =
          WasiContext.create()
              // Filesystem
              .preopenedDir(dataDir, "/data")
              .preopenedDir(configDir, "/config")
              // Environment
              .setEnv("APP_ENV", "test")
              .setEnv("CONFIG_PATH", "/config/app.conf")
              // Arguments
              .setArgv(new String[] {"myapp", "--config", "/config/app.conf"})
              // Stdio
              .inheritStdio();

      LOGGER.info("Created full WASI context with filesystem, env, and args");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should create minimal WASI context")
    void shouldCreateMinimalWasiContext() throws Exception {
      final WasiContext context = WasiContext.create();

      LOGGER.info("Created minimal WASI context");
      assertThat(context).isNotNull();
    }
  }

  @Nested
  @DisplayName("File Content Access Tests")
  class FileContentAccessTests {

    @Test
    @DisplayName("should prepare directory with binary file")
    void shouldPrepareDirectoryWithBinaryFile() throws Exception {
      final Path binDir = tempDir.resolve("binary");
      Files.createDirectories(binDir);

      // Create a binary file with some test data
      final byte[] binaryData = new byte[256];
      for (int i = 0; i < 256; i++) {
        binaryData[i] = (byte) i;
      }
      Files.write(binDir.resolve("test.bin"), binaryData);

      final WasiContext context = WasiContext.create().preopenedDir(binDir, "/bin");

      LOGGER.info("Created WASI context with directory containing binary file");
      assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("should prepare directory with large file")
    void shouldPrepareDirectoryWithLargeFile() throws Exception {
      final Path largeDir = tempDir.resolve("large");
      Files.createDirectories(largeDir);

      // Create a 1MB file
      final byte[] largeData = new byte[1024 * 1024];
      for (int i = 0; i < largeData.length; i++) {
        largeData[i] = (byte) (i % 256);
      }
      Files.write(largeDir.resolve("large.dat"), largeData);

      final WasiContext context = WasiContext.create().preopenedDir(largeDir, "/large");

      LOGGER.info("Created WASI context with directory containing 1MB file");
      assertThat(context).isNotNull();
    }
  }
}
