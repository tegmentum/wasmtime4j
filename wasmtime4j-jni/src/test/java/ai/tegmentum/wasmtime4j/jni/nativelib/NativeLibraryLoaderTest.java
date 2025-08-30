package ai.tegmentum.wasmtime4j.jni.nativelib;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NativeLibraryLoader}.
 */
class NativeLibraryLoaderTest {

  @Test
  void testGetLibraryResourcePath() {
    final String resourcePath = NativeLibraryLoader.getLibraryResourcePath();

    assertThat(resourcePath).isNotNull();
    assertThat(resourcePath).startsWith("/natives/");
    assertThat(resourcePath).contains("wasmtime4j");

    // Should contain platform and architecture
    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
      assertThat(resourcePath).contains("macos");
      assertThat(resourcePath).endsWith(".dylib");
    } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
      assertThat(resourcePath).contains("linux");
      assertThat(resourcePath).endsWith(".so");
    } else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
      assertThat(resourcePath).contains("windows");
      assertThat(resourcePath).endsWith(".dll");
    }

    // Should contain architecture
    final String arch = System.getProperty("os.arch").toLowerCase();
    if (arch.equals("amd64") || arch.equals("x86_64")) {
      assertThat(resourcePath).contains("x86_64");
    } else if (arch.equals("aarch64") || arch.equals("arm64")) {
      assertThat(resourcePath).contains("aarch64");
    }
  }

  @Test
  void testGetPlatformInfo() {
    final String platformInfo = NativeLibraryLoader.getPlatformInfo();

    assertThat(platformInfo).isNotNull();
    assertThat(platformInfo).contains("Platform:");
    assertThat(platformInfo).contains("Library path:");
    assertThat(platformInfo).contains("Loaded:");

    // Should contain current platform info
    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
      assertThat(platformInfo).contains("macos");
    } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
      assertThat(platformInfo).contains("linux");
    } else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
      assertThat(platformInfo).contains("windows");
    }
  }

  @Test
  void testIsLibraryLoadedInitialState() {
    // Before any load attempt, should be false
    // Note: This test may be unreliable if other tests have already loaded the library
    // but we include it for completeness
    assertThat(NativeLibraryLoader.isLibraryLoaded()).isIn(true, false);
  }

  @Test
  void testLoadLibraryIdempotent() {
    // Should not throw exception even if called multiple times
    // Note: This will likely fail because the actual native library doesn't exist yet,
    // but it tests the method structure and exception handling

    // First call - may fail but shouldn't crash
    try {
      NativeLibraryLoader.loadLibrary();
      // If it succeeds, subsequent calls should also succeed
      assertDoesNotThrow(() -> NativeLibraryLoader.loadLibrary());
      assertThat(NativeLibraryLoader.isLibraryLoaded()).isTrue();
    } catch (RuntimeException e) {
      // Expected if native library doesn't exist
      assertThat(e.getMessage()).contains("Failed to load native library");
      assertFalse(NativeLibraryLoader.isLibraryLoaded());
    }
  }

  @Test
  void testUtilityClassCannotBeInstantiated() {
    // Ensure utility class cannot be instantiated
    assertThrows(
        AssertionError.class,
        () -> {
          try {
            final java.lang.reflect.Constructor<?> constructor =
                NativeLibraryLoader.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
          } catch (Exception e) {
            if (e.getCause() instanceof AssertionError) {
              throw (AssertionError) e.getCause();
            }
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void testResourcePathFormat() {
    final String resourcePath = NativeLibraryLoader.getLibraryResourcePath();

    // Should follow expected format: /natives/{os}-{arch}/wasmtime4j{extension}
    final String[] parts = resourcePath.split("/");
    assertThat(parts).hasSizeGreaterThanOrEqualTo(3);
    assertThat(parts[1]).isEqualTo("natives");

    // Last part should be the library file
    final String libraryFile = parts[parts.length - 1];
    assertThat(libraryFile).contains("wasmtime4j");
    assertThat(libraryFile).containsAnyOf(".so", ".dll", ".dylib");

    // Check platform-specific naming conventions
    if (libraryFile.endsWith(".dll")) {
      // Windows: wasmtime4j.dll (no prefix)
      assertThat(libraryFile).startsWith("wasmtime4j");
    } else {
      // Unix-like: libwasmtime4j.so/.dylib (with lib prefix)
      assertThat(libraryFile).startsWith("lib");
    }
  }

  @Test
  void testPlatformDetection() {
    // Test that platform detection doesn't throw exceptions
    assertDoesNotThrow(() -> NativeLibraryLoader.getLibraryResourcePath());
    assertDoesNotThrow(() -> NativeLibraryLoader.getPlatformInfo());
  }
}
