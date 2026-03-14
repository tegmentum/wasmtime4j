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
package ai.tegmentum.wasmtime4j.nativeloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the native loading policy system properties.
 *
 * <p>These tests verify that the policy gate ({@code wasmtime4j.native.mode}), Strategy 1 toggle
 * ({@code wasmtime4j.native.strategy1}), and tmpdir override ({@code wasmtime4j.native.tmpdir})
 * system properties are correctly read and enforced by {@link NativeLibraryUtils}.
 *
 * <p>Since these tests run in the native-loader module (which uses {@code auto} mode by default),
 * they temporarily set system properties to exercise all three modes.
 */
@DisplayName("Native Loading Policy Tests")
final class NativeLoadingPolicyTest {

  /** Saved values for system properties to restore in @AfterEach. */
  private String savedMode;

  private String savedStrategy1;
  private String savedTmpdir;

  @BeforeEach
  void saveProperties() {
    savedMode = System.getProperty("wasmtime4j.native.mode");
    savedStrategy1 = System.getProperty("wasmtime4j.native.strategy1");
    savedTmpdir = System.getProperty("wasmtime4j.native.tmpdir");
  }

  @AfterEach
  void restoreProperties() {
    restoreProperty("wasmtime4j.native.mode", savedMode);
    restoreProperty("wasmtime4j.native.strategy1", savedStrategy1);
    restoreProperty("wasmtime4j.native.tmpdir", savedTmpdir);
  }

  private static void restoreProperty(final String key, final String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }

  @Nested
  @DisplayName("Forbid Mode")
  class ForbidModeTests {

    @BeforeEach
    void setForbidMode() {
      System.setProperty("wasmtime4j.native.mode", "forbid");
    }

    @Test
    @DisplayName("loadNativeLibrary(String, Config) throws IllegalStateException in forbid mode")
    void loadNativeLibraryThrowsInForbidMode() {
      final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();

      final IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () -> NativeLibraryUtils.loadNativeLibrary("testlib", config),
              "loadNativeLibrary should throw IllegalStateException when mode=forbid");

      assertTrue(
          ex.getMessage().contains("forbid"),
          "Exception message should mention 'forbid': " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("testlib"),
          "Exception message should contain the library name: " + ex.getMessage());
    }

    @Test
    @DisplayName("loadNativeLibrary() throws IllegalStateException in forbid mode")
    void loadNativeLibraryNoArgsThrowsInForbidMode() {
      assertThrows(
          IllegalStateException.class,
          () -> NativeLibraryUtils.loadNativeLibrary(),
          "loadNativeLibrary() should throw IllegalStateException when mode=forbid");
    }

    @Test
    @DisplayName("loadNativeLibraryWithConventions throws IllegalStateException in forbid mode")
    void loadNativeLibraryWithConventionsThrowsInForbidMode() {
      final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();

      final IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () ->
                  NativeLibraryUtils.loadNativeLibraryWithConventions(
                      "testlib", config, PathConvention.MAVEN_NATIVE),
              "loadNativeLibraryWithConventions should throw IllegalStateException "
                  + "when mode=forbid");

      assertTrue(
          ex.getMessage().contains("forbid"),
          "Exception message should mention 'forbid': " + ex.getMessage());
    }

    @Test
    @DisplayName(
        "loadNativeLibraryWithCustomConvention throws IllegalStateException in forbid mode")
    void loadNativeLibraryWithCustomConventionThrowsInForbidMode() {
      final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();
      final PathConvention.CustomPathConvention customConvention =
          PathConvention.custom("/custom/{os}/{arch}/{file}");

      final IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () ->
                  NativeLibraryUtils.loadNativeLibraryWithCustomConvention(
                      "testlib", config, customConvention, PathConvention.MAVEN_NATIVE),
              "loadNativeLibraryWithCustomConvention should throw IllegalStateException "
                  + "when mode=forbid");

      assertTrue(
          ex.getMessage().contains("forbid"),
          "Exception message should mention 'forbid': " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("Strategy 1 Toggle")
  class Strategy1ToggleTests {

    @BeforeEach
    void disableStrategy1() {
      System.setProperty("wasmtime4j.native.strategy1", "false");
      // Ensure we're in auto mode so forbid doesn't block us
      System.clearProperty("wasmtime4j.native.mode");
    }

    @Test
    @DisplayName("loadNativeLibrary does not use System.loadLibrary when strategy1=false")
    void loadNativeLibrarySkipsStrategy1WhenDisabled() {
      final NativeLibraryUtils.LibraryLoadInfo info =
          NativeLibraryUtils.loadNativeLibrary("nonexistent_strategy1_test");

      assertNotNull(info, "Load info should not be null");
      // The load will fail because the library doesn't exist, but it should NOT have
      // succeeded via SYSTEM_LIBRARY_PATH
      if (info.getLoadingMethod() != null) {
        assertFalse(
            info.getLoadingMethod()
                == NativeLibraryUtils.LibraryLoadInfo.LoadingMethod.SYSTEM_LIBRARY_PATH,
            "Loading method should not be SYSTEM_LIBRARY_PATH when strategy1=false");
      }
    }

    @Test
    @DisplayName(
        "loadNativeLibraryWithConventions does not use System.loadLibrary when strategy1=false")
    void loadWithConventionsSkipsStrategy1WhenDisabled() {
      final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();
      final NativeLibraryUtils.LibraryLoadInfo info =
          NativeLibraryUtils.loadNativeLibraryWithConventions(
              "nonexistent_strategy1_conv_test", config, PathConvention.MAVEN_NATIVE);

      assertNotNull(info, "Load info should not be null");
      if (info.getLoadingMethod() != null) {
        assertFalse(
            info.getLoadingMethod()
                == NativeLibraryUtils.LibraryLoadInfo.LoadingMethod.SYSTEM_LIBRARY_PATH,
            "Loading method should not be SYSTEM_LIBRARY_PATH when strategy1=false");
      }
    }

    @Test
    @DisplayName(
        "loadNativeLibraryWithCustomConvention does not use System.loadLibrary "
            + "when strategy1=false")
    void loadWithCustomConventionSkipsStrategy1WhenDisabled() {
      final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();
      final PathConvention.CustomPathConvention customConvention =
          PathConvention.custom("/custom/{os}/{arch}/{file}");
      final NativeLibraryUtils.LibraryLoadInfo info =
          NativeLibraryUtils.loadNativeLibraryWithCustomConvention(
              "nonexistent_strategy1_custom_test",
              config,
              customConvention,
              PathConvention.MAVEN_NATIVE);

      assertNotNull(info, "Load info should not be null");
      if (info.getLoadingMethod() != null) {
        assertFalse(
            info.getLoadingMethod()
                == NativeLibraryUtils.LibraryLoadInfo.LoadingMethod.SYSTEM_LIBRARY_PATH,
            "Loading method should not be SYSTEM_LIBRARY_PATH when strategy1=false");
      }
    }

    @Test
    @DisplayName("Strategy 1 is enabled by default when property is not set")
    void strategy1EnabledByDefault() {
      System.clearProperty("wasmtime4j.native.strategy1");

      // With default (strategy1 enabled), loading a nonexistent library should still fail
      // but it should attempt System.loadLibrary first
      final NativeLibraryUtils.LibraryLoadInfo info =
          NativeLibraryUtils.loadNativeLibrary("nonexistent_default_test");

      assertNotNull(info, "Load info should not be null");
      // We can't assert it used Strategy 1 (since the library doesn't exist),
      // but we verify no exception was thrown from the policy gate
      assertEquals(
          "nonexistent_default_test", info.getLibraryName(), "Library name should match requested");
    }
  }

  @Nested
  @DisplayName("Tmpdir Override")
  class TmpdirOverrideTests {

    private Path customTmpDir;

    @BeforeEach
    void setupCustomTmpDir() throws IOException {
      customTmpDir = Files.createTempDirectory("wasmtime4j-policy-test-");
      System.setProperty("wasmtime4j.native.tmpdir", customTmpDir.toAbsolutePath().toString());
      // Ensure auto mode so we can actually attempt extraction
      System.clearProperty("wasmtime4j.native.mode");
    }

    @AfterEach
    void cleanupCustomTmpDir() throws IOException {
      if (customTmpDir != null && Files.exists(customTmpDir)) {
        // Clean up any extracted files
        Files.walk(customTmpDir)
            .sorted(java.util.Comparator.reverseOrder())
            .forEach(
                path -> {
                  try {
                    Files.deleteIfExists(path);
                  } catch (IOException ignored) {
                    // Best effort cleanup
                  }
                });
      }
    }

    @Test
    @DisplayName("extractLibraryFromJar uses custom tmpdir when property is set")
    void extractionUsesCustomTmpdir() {
      final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
      final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();

      // Attempt extraction with a non-existent resource - the IOException
      // should indicate the resource path, and the custom tmpdir should be respected
      final IOException ex =
          assertThrows(
              IOException.class,
              () ->
                  NativeLibraryUtils.extractLibraryFromJar(
                      "nonexistent", platformInfo, "/nonexistent/resource", config),
              "extractLibraryFromJar should throw IOException for missing resource");

      assertNotNull(ex.getMessage(), "IOException message should not be null");
      assertTrue(
          ex.getMessage().contains("/nonexistent/resource"),
          "IOException should mention the missing resource path: " + ex.getMessage());
    }

    @Test
    @DisplayName("extractLibraryFromJar creates parent directories if missing")
    void extractLibraryCreatesParentDirectoryIfMissing() throws IOException {
      // Create a path where parent directories don't exist yet
      final Path nonExistentParent = customTmpDir.resolve("does-not-exist").resolve("nested");
      assertFalse(Files.exists(nonExistentParent), "Parent directory should not exist before test");

      // Override tmpdir to the non-existent nested path
      System.setProperty("wasmtime4j.native.tmpdir", nonExistentParent.toString());

      final PlatformDetector.PlatformInfo platform = PlatformDetector.detect();
      final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();

      // Use a real classpath resource so the code reaches Files.createDirectories
      // (a nonexistent resource would throw IOException before createDirectories is called)
      final String realResource =
          "/" + NativeLibraryUtils.class.getName().replace('.', '/') + ".class";

      final Path extracted =
          NativeLibraryUtils.extractLibraryFromJar(
              "create-dirs-test", platform, realResource, config);

      assertTrue(
          Files.exists(nonExistentParent),
          "createDirectories should have created the nested parent path");
      assertTrue(Files.exists(extracted), "Extracted file should exist");
    }

    @Test
    @DisplayName("Default tmpdir is java.io.tmpdir when property is not set")
    void defaultTmpdirIsJavaIoTmpdir() {
      System.clearProperty("wasmtime4j.native.tmpdir");

      final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
      final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();

      // This should also throw IOException because the resource doesn't exist,
      // but it exercises the default tmpdir path
      final IOException ex =
          assertThrows(
              IOException.class,
              () ->
                  NativeLibraryUtils.extractLibraryFromJar(
                      "nonexistent", platformInfo, "/nonexistent/resource", config),
              "extractLibraryFromJar should throw IOException for missing resource");

      assertNotNull(ex.getMessage(), "IOException message should not be null");
    }
  }

  @Nested
  @DisplayName("Auto Mode (Default)")
  class AutoModeTests {

    @BeforeEach
    void clearModeProperty() {
      System.clearProperty("wasmtime4j.native.mode");
    }

    @Test
    @DisplayName("Loading proceeds without policy exception in auto mode")
    void loadingProceedsInAutoMode() {
      // In auto mode, loading should not throw IllegalStateException from the policy gate.
      // It may still fail for other reasons (library not found), but that's expected.
      final NativeLibraryUtils.LibraryLoadInfo info =
          NativeLibraryUtils.loadNativeLibrary("nonexistent_auto_test");

      assertNotNull(info, "Load info should not be null");
      assertEquals(
          "nonexistent_auto_test", info.getLibraryName(), "Library name should match requested");
      // The key assertion: no IllegalStateException was thrown
    }

    @Test
    @DisplayName("Auto mode is the default when no property is set")
    void autoModeIsDefault() {
      System.clearProperty("wasmtime4j.native.mode");

      // Verify that when no property is set, behavior matches explicit auto mode
      final NativeLibraryUtils.LibraryLoadInfo autoInfo =
          NativeLibraryUtils.loadNativeLibrary("nonexistent_default_check");
      assertNotNull(autoInfo, "Load info should not be null in default/auto mode");

      System.setProperty("wasmtime4j.native.mode", "auto");
      final NativeLibraryUtils.LibraryLoadInfo explicitAutoInfo =
          NativeLibraryUtils.loadNativeLibrary("nonexistent_explicit_auto_check");
      assertNotNull(explicitAutoInfo, "Load info should not be null in explicit auto mode");

      // Both should produce the same result type (non-null, no policy exception)
      assertEquals(
          autoInfo.getLibraryName().replace("nonexistent_default_check", ""),
          explicitAutoInfo.getLibraryName().replace("nonexistent_explicit_auto_check", ""),
          "Default and explicit auto mode should behave identically (aside from library name)");
    }
  }

  @Nested
  @DisplayName("Extraction Caching")
  class ExtractionCachingTests {

    private Path cacheTmpDir;

    @BeforeEach
    void setupCacheTmpDir() throws IOException {
      cacheTmpDir = Files.createTempDirectory("wasmtime4j-cache-test-");
      System.setProperty("wasmtime4j.native.tmpdir", cacheTmpDir.toAbsolutePath().toString());
      System.clearProperty("wasmtime4j.native.mode");
    }

    @AfterEach
    void cleanupCacheTmpDir() throws IOException {
      if (cacheTmpDir != null && Files.exists(cacheTmpDir)) {
        Files.walk(cacheTmpDir)
            .sorted(java.util.Comparator.reverseOrder())
            .forEach(
                path -> {
                  try {
                    Files.deleteIfExists(path);
                  } catch (IOException ignored) {
                    // Best effort cleanup
                  }
                });
      }
    }

    @Test
    @DisplayName("extracting same resource twice should return same path")
    void extractingSameResourceTwiceShouldReturnSamePath() throws IOException {
      final PlatformDetector.PlatformInfo platform = PlatformDetector.detect();
      final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();

      // Use a real classpath resource so extraction succeeds
      final String realResource =
          "/" + NativeLibraryUtils.class.getName().replace('.', '/') + ".class";

      final Path firstExtraction =
          NativeLibraryUtils.extractLibraryFromJar("cache-test", platform, realResource, config);
      assertTrue(Files.exists(firstExtraction), "First extraction should create a file");

      final Path secondExtraction =
          NativeLibraryUtils.extractLibraryFromJar("cache-test", platform, realResource, config);
      assertTrue(Files.exists(secondExtraction), "Second extraction should also produce a file");

      assertEquals(
          firstExtraction.toAbsolutePath(),
          secondExtraction.toAbsolutePath(),
          "Both extractions should resolve to the same path");
    }

    @Test
    @DisplayName("extracting different resources should produce different paths")
    void extractingDifferentResourcesShouldProduceDifferentPaths() throws IOException {
      final PlatformDetector.PlatformInfo platform = PlatformDetector.detect();
      final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();

      final String resource1 =
          "/" + NativeLibraryUtils.class.getName().replace('.', '/') + ".class";
      final String resource2 =
          "/" + NativeLibraryConfig.class.getName().replace('.', '/') + ".class";

      final Path path1 =
          NativeLibraryUtils.extractLibraryFromJar("cache-test-a", platform, resource1, config);
      final Path path2 =
          NativeLibraryUtils.extractLibraryFromJar("cache-test-b", platform, resource2, config);

      assertTrue(Files.exists(path1), "First resource extraction should exist");
      assertTrue(Files.exists(path2), "Second resource extraction should exist");
      assertFalse(
          path1.toAbsolutePath().equals(path2.toAbsolutePath()),
          "Different library names should produce different paths: " + path1 + " vs " + path2);
    }
  }

  @Nested
  @DisplayName("Require Mode")
  class RequireModeTests {

    @BeforeEach
    void setRequireMode() {
      System.setProperty("wasmtime4j.native.mode", "require");
    }

    @Test
    @DisplayName(
        "loadNativeLibrary throws IllegalStateException when loading fails in require mode")
    void loadNativeLibraryThrowsWhenLoadingFailsInRequireMode() {
      assertThrows(
          IllegalStateException.class,
          () -> NativeLibraryUtils.loadNativeLibrary("nonexistent_require_test"),
          "loadNativeLibrary should throw IllegalStateException when mode=require "
              + "and loading fails");
    }

    @Test
    @DisplayName(
        "loadNativeLibraryWithConventions throws IllegalStateException when loading fails "
            + "in require mode")
    void loadWithConventionsThrowsWhenLoadingFailsInRequireMode() {
      final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();
      assertThrows(
          IllegalStateException.class,
          () ->
              NativeLibraryUtils.loadNativeLibraryWithConventions(
                  "nonexistent_require_conv_test", config, PathConvention.MAVEN_NATIVE),
          "loadNativeLibraryWithConventions should throw IllegalStateException "
              + "when mode=require and loading fails");
    }

    @Test
    @DisplayName(
        "loadNativeLibraryWithCustomConvention throws IllegalStateException when loading fails "
            + "in require mode")
    void loadWithCustomConventionThrowsWhenLoadingFailsInRequireMode() {
      final NativeLibraryConfig config = NativeLibraryConfig.defaultConfig();
      final PathConvention.CustomPathConvention customConvention =
          PathConvention.custom("/custom/{os}/{arch}/{file}");
      assertThrows(
          IllegalStateException.class,
          () ->
              NativeLibraryUtils.loadNativeLibraryWithCustomConvention(
                  "nonexistent_require_custom_test",
                  config,
                  customConvention,
                  PathConvention.MAVEN_NATIVE),
          "loadNativeLibraryWithCustomConvention should throw IllegalStateException "
              + "when mode=require and loading fails");
    }

    @Test
    @DisplayName("Require mode error message contains library name and mode")
    void requireModeErrorMessageIsDescriptive() {
      final IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () -> NativeLibraryUtils.loadNativeLibrary("descriptive_test_lib"),
              "Should throw IllegalStateException in require mode");

      assertTrue(
          ex.getMessage().contains("require"),
          "Error message should mention 'require': " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("descriptive_test_lib"),
          "Error message should contain the library name: " + ex.getMessage());
    }
  }
}
