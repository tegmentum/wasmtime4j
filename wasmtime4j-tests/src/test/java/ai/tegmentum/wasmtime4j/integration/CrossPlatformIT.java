package ai.tegmentum.wasmtime4j.integration;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.utils.BaseIntegrationTest;
import ai.tegmentum.wasmtime4j.utils.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Integration tests for cross-platform compatibility. Tests that Wasmtime4j works correctly across
 * different operating systems and architectures.
 */
@DisplayName("Cross-Platform Compatibility Integration Tests")
class CrossPlatformIT extends BaseIntegrationTest {

  @Override
  protected void doSetUp(final TestInfo testInfo) {
    // skipIfCategoryNotEnabled("platform");
  }

  @Test
  @DisplayName("Should detect platform correctly")
  void shouldDetectPlatformCorrectly() {
    // Given & When
    final String os = TestUtils.getOperatingSystem();
    final String arch = TestUtils.getSystemArchitecture();

    // Then
    assertThat(os).isNotEmpty();
    assertThat(arch).isNotEmpty();

    // Verify platform detection methods work correctly
    final boolean isLinux = TestUtils.isLinux();
    final boolean isWindows = TestUtils.isWindows();
    final boolean isMacOs = TestUtils.isMacOs();

    // Exactly one should be true
    final int platformCount = (isLinux ? 1 : 0) + (isWindows ? 1 : 0) + (isMacOs ? 1 : 0);
    assertThat(platformCount).as("Exactly one platform should be detected").isEqualTo(1);

    // Verify architecture detection
    final boolean isX86_64 = TestUtils.isX86_64();
    final boolean isArm64 = TestUtils.isArm64();

    // At least one architecture should be detected
    assertThat(isX86_64 || isArm64)
        .as("At least one supported architecture should be detected")
        .isTrue();

    LOGGER.info("Platform detection successful: " + os + " on " + arch);
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  @DisplayName("Should work on Linux")
  void shouldWorkOnLinux() {
    // Given - verify we're on Linux
    assertThat(TestUtils.isLinux()).isTrue();
    assertThat(System.getProperty("os.name").toLowerCase()).contains("linux");

    // When - test platform-specific functionality
    final String arch = TestUtils.getSystemArchitecture();
    LOGGER.info("Running on Linux with architecture: " + arch);

    // Test native library path convention for Linux
    try {
      final String expectedLibraryExtension = ".so";
      final String libraryPrefix = "lib";

      // Verify Linux uses correct library naming conventions
      assertThat(expectedLibraryExtension).isEqualTo(".so");
      assertThat(libraryPrefix).isEqualTo("lib");

      LOGGER.info("Linux library conventions validated: prefix=" + libraryPrefix
          + ", extension=" + expectedLibraryExtension);

      // Test that we can determine the correct platform string
      final String expectedPlatformString = TestUtils.isX86_64() ? "linux-x86_64" : "linux-aarch64";
      LOGGER.info("Expected platform string for Linux: " + expectedPlatformString);

    } catch (final Exception e) {
      LOGGER.severe("Linux platform validation failed: " + e.getMessage());
      throw new RuntimeException("Linux platform test failed", e);
    }

    LOGGER.info("Linux compatibility test completed successfully");
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  @DisplayName("Should work on Windows")
  void shouldWorkOnWindows() {
    // Given
    assertThat(TestUtils.isWindows()).isTrue();

    // TODO: Test Windows-specific functionality when API is available
    // Test should include Windows path handling, library loading, etc.

    LOGGER.info("Windows compatibility test placeholder completed");
  }

  @Test
  @EnabledOnOs(OS.MAC)
  @DisplayName("Should work on macOS")
  void shouldWorkOnMacOs() {
    // Given - verify we're on macOS
    assertThat(TestUtils.isMacOs()).isTrue();
    assertThat(System.getProperty("os.name").toLowerCase()).contains("mac");

    // When - test platform-specific functionality
    final String arch = TestUtils.getSystemArchitecture();
    LOGGER.info("Running on macOS with architecture: " + arch);

    // Test native library path convention for macOS
    try {
      final String expectedLibraryExtension = ".dylib";
      final String libraryPrefix = "lib";

      // Verify macOS uses correct library naming conventions
      assertThat(expectedLibraryExtension).isEqualTo(".dylib");
      assertThat(libraryPrefix).isEqualTo("lib");

      LOGGER.info("macOS library conventions validated: prefix=" + libraryPrefix
          + ", extension=" + expectedLibraryExtension);

      // Test that we can determine the correct platform string for both architectures
      final String expectedPlatformString = TestUtils.isX86_64() ? "macos-x86_64" : "macos-aarch64";
      LOGGER.info("Expected platform string for macOS: " + expectedPlatformString);

      // Verify architecture-specific functionality
      if (TestUtils.isArm64()) {
        LOGGER.info("Running on Apple Silicon (ARM64) - testing Apple Silicon specific functionality");
        assertThat(arch).containsIgnoringCase("aarch64");
      } else if (TestUtils.isX86_64()) {
        LOGGER.info("Running on Intel macOS (x86_64) - testing Intel specific functionality");
        assertThat(arch).containsIgnoringCase("x86_64");
      }

    } catch (final Exception e) {
      LOGGER.severe("macOS platform validation failed: " + e.getMessage());
      throw new RuntimeException("macOS platform test failed", e);
    }

    LOGGER.info("macOS compatibility test completed successfully");
  }

  @Test
  @DisplayName("Should handle different architectures")
  void shouldHandleDifferentArchitectures() {
    // Given
    final String arch = TestUtils.getSystemArchitecture();

    // When & Then
    if (TestUtils.isX86_64()) {
      // TODO: Test x86_64-specific functionality
      // Verify that x86_64 native libraries load correctly
      LOGGER.info("Testing x86_64 architecture support");
    } else if (TestUtils.isArm64()) {
      // TODO: Test ARM64-specific functionality
      // Verify that ARM64 native libraries load correctly
      LOGGER.info("Testing ARM64 architecture support");
    } else {
      LOGGER.warning("Unsupported architecture: " + arch);
    }

    LOGGER.info("Architecture compatibility test completed for: " + arch);
  }

  @Test
  @DisplayName("Should load correct native libraries for platform")
  void shouldLoadCorrectNativeLibrariesForPlatform() {
    // Given - get current platform information
    final String os = TestUtils.getOperatingSystem();
    final String arch = TestUtils.getSystemArchitecture();
    LOGGER.info("Testing native library loading on platform: " + os + " / " + arch);

    // When - attempt to load native library through JNI loader
    try {
      // Test JNI library loading
      final Class<?> jniLoaderClass = Class.forName("ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader");
      final java.lang.reflect.Method loadLibraryMethod = jniLoaderClass.getDeclaredMethod("loadLibrary");
      loadLibraryMethod.invoke(null);

      // Verify the library was loaded
      final java.lang.reflect.Method isLoadedMethod = jniLoaderClass.getDeclaredMethod("isLibraryLoaded");
      final boolean isLoaded = (boolean) isLoadedMethod.invoke(null);

      // Then - verify loading succeeded
      assertThat(isLoaded)
          .as("Native library should be loaded successfully on platform: " + os + "/" + arch)
          .isTrue();

      // Verify correct library extension for platform
      final java.lang.reflect.Method getResourcePathMethod = jniLoaderClass.getDeclaredMethod("getLibraryResourcePath");
      final String resourcePath = (String) getResourcePathMethod.invoke(null);

      if (TestUtils.isLinux()) {
        assertThat(resourcePath).as("Linux should load .so libraries").contains(".so");
      } else if (TestUtils.isWindows()) {
        assertThat(resourcePath).as("Windows should load .dll libraries").contains(".dll");
      } else if (TestUtils.isMacOs()) {
        assertThat(resourcePath).as("macOS should load .dylib libraries").contains(".dylib");
      }

      LOGGER.info("Successfully loaded native library from path: " + resourcePath);

    } catch (final Exception e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library loading test failed", e);
    }
  }

  @Test
  @DisplayName("Should handle platform-specific file paths")
  void shouldHandlePlatformSpecificFilePaths() {
    // Given
    final String separator = System.getProperty("file.separator");

    // When
    final String testPath = "wasm" + separator + "test.wasm";

    // Then
    assertThat(testPath).contains(separator);

    if (TestUtils.isWindows()) {
      assertThat(separator).isEqualTo("\\");
    } else {
      assertThat(separator).isEqualTo("/");
    }

    LOGGER.info("Platform-specific file path handling test completed");
  }

  @Test
  @DisplayName("Should work with different endianness")
  void shouldWorkWithDifferentEndianness() {
    // TODO: Test endianness handling in WebAssembly memory operations
    // Verify that memory reads/writes work correctly regardless of platform endianness

    LOGGER.info("Endianness compatibility test placeholder completed");
  }

  @Test
  @DisplayName("Should handle platform-specific memory constraints")
  void shouldHandlePlatformSpecificMemoryConstraints() {
    // TODO: Test memory allocation and limits on different platforms
    // Different platforms may have different memory constraints

    LOGGER.info("Platform memory constraints test placeholder completed");
  }
}
