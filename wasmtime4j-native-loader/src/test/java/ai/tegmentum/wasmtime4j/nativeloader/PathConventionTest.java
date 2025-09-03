/*
 * Copyright 2024 Tegmentum AI
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Comprehensive tests for PathConvention enum and resource path generation functionality.
 *
 * <p>This test class verifies that path conventions correctly generate resource paths for all
 * supported platforms and library naming patterns.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
final class PathConventionTest {

  private PlatformDetector.PlatformInfo linuxX64Info;
  private PlatformDetector.PlatformInfo windowsX64Info;
  private PlatformDetector.PlatformInfo darwinX64Info;
  private PlatformDetector.PlatformInfo linuxAarch64Info;

  @BeforeEach
  void setUp() {
    // Create platform info objects for testing different platforms
    linuxX64Info =
        new PlatformDetector.PlatformInfo(
            PlatformDetector.OperatingSystem.LINUX, PlatformDetector.Architecture.X86_64);

    windowsX64Info =
        new PlatformDetector.PlatformInfo(
            PlatformDetector.OperatingSystem.WINDOWS, PlatformDetector.Architecture.X86_64);

    darwinX64Info =
        new PlatformDetector.PlatformInfo(
            PlatformDetector.OperatingSystem.MACOS, PlatformDetector.Architecture.X86_64);

    linuxAarch64Info =
        new PlatformDetector.PlatformInfo(
            PlatformDetector.OperatingSystem.LINUX, PlatformDetector.Architecture.AARCH64);
  }

  @Test
  @DisplayName("WASMTIME4J convention should generate correct paths for all platforms")
  void testWasmtime4jConvention() {
    final String libraryName = "wasmtime4j";

    assertEquals(
        "/native/linux-x86_64/libwasmtime4j.so",
        PathConvention.WASMTIME4J.generatePath(libraryName, linuxX64Info),
        "WASMTIME4J convention should generate correct Linux x86_64 path");

    assertEquals(
        "/native/windows-x86_64/wasmtime4j.dll",
        PathConvention.WASMTIME4J.generatePath(libraryName, windowsX64Info),
        "WASMTIME4J convention should generate correct Windows x86_64 path");

    assertEquals(
        "/native/macos-x86_64/libwasmtime4j.dylib",
        PathConvention.WASMTIME4J.generatePath(libraryName, darwinX64Info),
        "WASMTIME4J convention should generate correct macOS x86_64 path");

    assertEquals(
        "/native/linux-aarch64/libwasmtime4j.so",
        PathConvention.WASMTIME4J.generatePath(libraryName, linuxAarch64Info),
        "WASMTIME4J convention should generate correct Linux aarch64 path");
  }

  @Test
  @DisplayName("MAVEN_NATIVE convention should generate correct paths for all platforms")
  void testMavenNativeConvention() {
    final String libraryName = "testlib";

    assertEquals(
        "/natives/linux-x86_64/libtestlib.so",
        PathConvention.MAVEN_NATIVE.generatePath(libraryName, linuxX64Info),
        "MAVEN_NATIVE convention should generate correct Linux x86_64 path");

    assertEquals(
        "/natives/windows-x86_64/testlib.dll",
        PathConvention.MAVEN_NATIVE.generatePath(libraryName, windowsX64Info),
        "MAVEN_NATIVE convention should generate correct Windows x86_64 path");

    assertEquals(
        "/natives/macos-x86_64/libtestlib.dylib",
        PathConvention.MAVEN_NATIVE.generatePath(libraryName, darwinX64Info),
        "MAVEN_NATIVE convention should generate correct macOS x86_64 path");
  }

  @Test
  @DisplayName("JNA convention should generate correct paths for all platforms")
  void testJnaConvention() {
    final String libraryName = "jnalib";

    assertEquals(
        "/linux-x86_64/jnalib.so",
        PathConvention.JNA.generatePath(libraryName, linuxX64Info),
        "JNA convention should generate correct Linux x86_64 path");

    assertEquals(
        "/windows-x86_64/jnalib.dll",
        PathConvention.JNA.generatePath(libraryName, windowsX64Info),
        "JNA convention should generate correct Windows x86_64 path");

    assertEquals(
        "/macos-x86_64/jnalib.dylib",
        PathConvention.JNA.generatePath(libraryName, darwinX64Info),
        "JNA convention should generate correct macOS x86_64 path");
  }

  @Test
  @DisplayName("CUSTOM convention should throw exception when used directly")
  void testCustomConventionThrowsException() {
    assertThrows(
        IllegalStateException.class,
        () -> PathConvention.CUSTOM.generatePath("test", linuxX64Info),
        "CUSTOM convention should throw exception when used without pattern");
  }

  @Test
  @DisplayName("Custom path convention factory should work correctly")
  void testCustomPathConventionFactory() {
    final String customPattern = "/lib/{platform}/{name}{ext}";
    final PathConvention.CustomPathConvention custom = PathConvention.custom(customPattern);

    assertNotNull(custom, "Custom convention should not be null");
    assertEquals(customPattern, custom.getPattern(), "Custom pattern should match input");

    assertEquals(
        "/lib/linux-x86_64/testlib.so",
        custom.generatePath("testlib", linuxX64Info),
        "Custom convention should generate correct path");
  }

  @Test
  @DisplayName("Custom path convention should validate patterns")
  void testCustomPatternValidation() {
    assertThrows(
        NullPointerException.class,
        () -> PathConvention.custom(null),
        "Should throw NPE for null pattern");

    assertThrows(
        IllegalArgumentException.class,
        () -> PathConvention.custom(""),
        "Should throw IAE for empty pattern");

    assertThrows(
        IllegalArgumentException.class,
        () -> PathConvention.custom("   "),
        "Should throw IAE for whitespace-only pattern");
  }

  @Test
  @DisplayName("Custom path convention should prevent path traversal")
  void testCustomPatternSecurityValidation() {
    assertThrows(
        SecurityException.class,
        () -> PathConvention.custom("../../../etc/passwd"),
        "Should reject path traversal patterns");

    assertThrows(
        SecurityException.class,
        () -> PathConvention.custom("/lib/{name}/../{platform}"),
        "Should reject patterns with path traversal sequences");
  }

  @ParameterizedTest
  @EnumSource(PathConvention.class)
  @DisplayName("All standard conventions should handle null inputs gracefully")
  void testNullInputHandling(final PathConvention convention) {
    if (convention == PathConvention.CUSTOM) {
      return; // Skip CUSTOM as it has different behavior
    }

    assertThrows(
        NullPointerException.class,
        () -> convention.generatePath(null, linuxX64Info),
        "Should throw NPE for null library name");

    assertThrows(
        NullPointerException.class,
        () -> convention.generatePath("test", null),
        "Should throw NPE for null platform info");
  }

  @ParameterizedTest
  @EnumSource(PathConvention.class)
  @DisplayName("All standard conventions should handle empty library names")
  void testEmptyLibraryNameHandling(final PathConvention convention) {
    if (convention == PathConvention.CUSTOM) {
      return; // Skip CUSTOM as it has different behavior
    }

    assertThrows(
        IllegalArgumentException.class,
        () -> convention.generatePath("", linuxX64Info),
        "Should throw IAE for empty library name");

    assertThrows(
        IllegalArgumentException.class,
        () -> convention.generatePath("   ", linuxX64Info),
        "Should throw IAE for whitespace-only library name");
  }

  @Test
  @DisplayName("Pattern placeholders should be properly substituted")
  void testPlaceholderSubstitution() {
    final String pattern = "/{os}/{arch}/{platform}/{lib}{name}{ext}";
    final PathConvention.CustomPathConvention custom = PathConvention.custom(pattern);

    final String result = custom.generatePath("mylib", linuxX64Info);

    assertEquals(
        "/linux/x86_64/linux-x86_64/libmylib.so",
        result,
        "All placeholders should be properly substituted");
  }

  @Test
  @DisplayName("Library name should be sanitized in custom patterns")
  void testLibraryNameSanitization() {
    final String pattern = "/lib/{name}{ext}";
    final PathConvention.CustomPathConvention custom = PathConvention.custom(pattern);

    // Test that path separators are removed from library name
    final String result = custom.generatePath("lib/with/slashes", linuxX64Info);

    assertEquals(
        "/lib/libwithslashes.so",
        result,
        "Library name should be sanitized to remove path separators");
  }

  @Test
  @DisplayName("Convention toString methods should provide useful output")
  void testToStringMethods() {
    assertEquals(
        "PathConvention.WASMTIME4J(\"/native/{platform}/{lib}{name}{ext}\")",
        PathConvention.WASMTIME4J.toString(),
        "WASMTIME4J toString should show pattern");

    assertEquals(
        "PathConvention.MAVEN_NATIVE(\"/natives/{os}-{arch}/{lib}{name}{ext}\")",
        PathConvention.MAVEN_NATIVE.toString(),
        "MAVEN_NATIVE toString should show pattern");

    assertEquals(
        "PathConvention.JNA(\"/{platform}/{name}{ext}\")",
        PathConvention.JNA.toString(),
        "JNA toString should show pattern");

    assertEquals(
        "PathConvention.CUSTOM",
        PathConvention.CUSTOM.toString(),
        "CUSTOM toString should show CUSTOM identifier");

    final PathConvention.CustomPathConvention custom = PathConvention.custom("/test/{name}");
    assertEquals(
        "PathConvention.CUSTOM(\"/test/{name}\")",
        custom.toString(),
        "Custom convention toString should show pattern");
  }
}
