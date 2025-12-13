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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/**
 * Parameterized tests for {@link PlatformDetector} covering all supported platform combinations.
 *
 * <p>This test class focuses on systematic testing of all platform detection scenarios including
 * the 6 supported platform combinations and error cases for unsupported platforms.
 */
@Disabled("System.class mocking causes infinite loops in newer Mockito versions")
final class PlatformDetectorParameterizedTest {

  /**
   * Provides test arguments for all supported platform combinations.
   *
   * @return stream of test arguments containing OS name, architecture, expected OS, expected
   *     architecture
   */
  private static Stream<Arguments> provideSupportedPlatforms() {
    return Stream.of(
        // Linux platforms
        Arguments.of(
            "Linux",
            "amd64",
            PlatformDetector.OperatingSystem.LINUX,
            PlatformDetector.Architecture.X86_64),
        Arguments.of(
            "linux",
            "x86_64",
            PlatformDetector.OperatingSystem.LINUX,
            PlatformDetector.Architecture.X86_64),
        Arguments.of(
            "GNU/Linux",
            "aarch64",
            PlatformDetector.OperatingSystem.LINUX,
            PlatformDetector.Architecture.AARCH64),
        Arguments.of(
            "linux",
            "arm64",
            PlatformDetector.OperatingSystem.LINUX,
            PlatformDetector.Architecture.AARCH64),
        // Windows platforms
        Arguments.of(
            "Windows 10",
            "amd64",
            PlatformDetector.OperatingSystem.WINDOWS,
            PlatformDetector.Architecture.X86_64),
        Arguments.of(
            "Windows 11",
            "x86_64",
            PlatformDetector.OperatingSystem.WINDOWS,
            PlatformDetector.Architecture.X86_64),
        Arguments.of(
            "windows",
            "aarch64",
            PlatformDetector.OperatingSystem.WINDOWS,
            PlatformDetector.Architecture.AARCH64),
        Arguments.of(
            "Windows Server 2022",
            "arm64",
            PlatformDetector.OperatingSystem.WINDOWS,
            PlatformDetector.Architecture.AARCH64),
        // macOS platforms
        Arguments.of(
            "Mac OS X",
            "x86_64",
            PlatformDetector.OperatingSystem.MACOS,
            PlatformDetector.Architecture.X86_64),
        Arguments.of(
            "macOS",
            "amd64",
            PlatformDetector.OperatingSystem.MACOS,
            PlatformDetector.Architecture.X86_64),
        Arguments.of(
            "Darwin",
            "aarch64",
            PlatformDetector.OperatingSystem.MACOS,
            PlatformDetector.Architecture.AARCH64),
        Arguments.of(
            "mac",
            "arm64",
            PlatformDetector.OperatingSystem.MACOS,
            PlatformDetector.Architecture.AARCH64));
  }

  /**
   * Provides test arguments for unsupported operating systems.
   *
   * @return stream of unsupported OS names
   */
  private static Stream<String> provideUnsupportedOperatingSystems() {
    return Stream.of(
        "FreeBSD",
        "OpenBSD",
        "NetBSD",
        "Solaris",
        "AIX",
        "HP-UX",
        "OS/2",
        "BeOS",
        "QNX",
        "UnknownOS",
        "",
        "   ");
  }

  /**
   * Provides test arguments for unsupported architectures.
   *
   * @return stream of unsupported architecture names
   */
  private static Stream<String> provideUnsupportedArchitectures() {
    return Stream.of(
        "x86",
        "i386",
        "i686",
        "sparc",
        "sparc64",
        "ppc",
        "ppc64",
        "ppc64le",
        "mips",
        "mips64",
        "s390",
        "s390x",
        "riscv32",
        "riscv64",
        "UnknownArch",
        "",
        "   ");
  }

  @ParameterizedTest(name = "Platform {0}/{1} should detect as {2}/{3}")
  @MethodSource("provideSupportedPlatforms")
  void testSupportedPlatformDetection(
      final String osName,
      final String osArch,
      final PlatformDetector.OperatingSystem expectedOs,
      final PlatformDetector.Architecture expectedArch) {

    // Mock system properties to simulate different platforms
    try (final MockedStatic<System> systemMock = mockStatic(System.class)) {
      systemMock.when(() -> System.getProperty("os.name")).thenReturn(osName);
      systemMock.when(() -> System.getProperty("os.arch")).thenReturn(osArch);
      systemMock
          .when(() -> System.getProperty("java.version"))
          .thenReturn(System.getProperty("java.version"));
      systemMock
          .when(() -> System.getProperty("java.io.tmpdir"))
          .thenReturn(System.getProperty("java.io.tmpdir"));

      // Clear the cached platform info to force re-detection
      PlatformDetectorTestUtils.clearCache();

      // Test detection
      final PlatformDetector.PlatformInfo info = PlatformDetector.detect();

      assertNotNull(info, "Platform info should not be null");
      assertEquals(
          expectedOs,
          info.getOperatingSystem(),
          String.format("OS should be detected as %s for input %s", expectedOs, osName));
      assertEquals(
          expectedArch,
          info.getArchitecture(),
          String.format(
              "Architecture should be detected as %s for input %s", expectedArch, osArch));

      // Verify platform ID format
      final String expectedPlatformId = expectedOs.getName() + "-" + expectedArch.getName();
      assertEquals(
          expectedPlatformId, info.getPlatformId(), "Platform ID should match expected format");

      // Verify library file name construction
      final String libraryName = "test";
      final String expectedFileName =
          expectedOs.getLibraryPrefix() + libraryName + expectedOs.getLibraryExtension();
      assertEquals(
          expectedFileName,
          info.getLibraryFileName(libraryName),
          "Library file name should be correctly constructed");

      // Verify resource path construction
      final String expectedResourcePath = "/natives/" + expectedPlatformId + "/" + expectedFileName;
      assertEquals(
          expectedResourcePath,
          info.getLibraryResourcePath(libraryName),
          "Library resource path should be correctly constructed");
    }
  }

  @ParameterizedTest(name = "Unsupported OS: {0}")
  @MethodSource("provideUnsupportedOperatingSystems")
  void testUnsupportedOperatingSystemDetection(final String unsupportedOsName) {
    try (final MockedStatic<System> systemMock = mockStatic(System.class)) {
      systemMock.when(() -> System.getProperty("os.name")).thenReturn(unsupportedOsName);
      systemMock.when(() -> System.getProperty("os.arch")).thenReturn("x86_64");

      // Clear cache
      PlatformDetectorTestUtils.clearCache();

      // Should throw RuntimeException for unsupported OS
      final RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              PlatformDetector::detect,
              "Should throw RuntimeException for unsupported OS: " + unsupportedOsName);

      assertNotNull(exception.getMessage(), "Exception message should not be null");
      assertTrue(
          exception.getMessage().contains("Unsupported operating system"),
          "Exception message should indicate unsupported OS");
    }
  }

  @ParameterizedTest(name = "Unsupported Architecture: {0}")
  @MethodSource("provideUnsupportedArchitectures")
  void testUnsupportedArchitectureDetection(final String unsupportedArchName) {
    try (final MockedStatic<System> systemMock = mockStatic(System.class)) {
      systemMock.when(() -> System.getProperty("os.name")).thenReturn("Linux");
      systemMock.when(() -> System.getProperty("os.arch")).thenReturn(unsupportedArchName);

      // Clear cache
      PlatformDetectorTestUtils.clearCache();

      // Should throw RuntimeException for unsupported architecture
      final RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              PlatformDetector::detect,
              "Should throw RuntimeException for unsupported architecture: " + unsupportedArchName);

      assertNotNull(exception.getMessage(), "Exception message should not be null");
      assertTrue(
          exception.getMessage().contains("Unsupported architecture"),
          "Exception message should indicate unsupported architecture");
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Linux, x86_64, linux-x86_64",
    "Windows, aarch64, windows-aarch64",
    "macOS, x86_64, darwin-x86_64"
  })
  void testPlatformIdFormat(
      final String osName, final String osArch, final String expectedPlatformId) {
    try (final MockedStatic<System> systemMock = mockStatic(System.class)) {
      systemMock.when(() -> System.getProperty("os.name")).thenReturn(osName);
      systemMock.when(() -> System.getProperty("os.arch")).thenReturn(osArch);

      PlatformDetectorTestUtils.clearCache();

      final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
      assertEquals(expectedPlatformId, info.getPlatformId(), "Platform ID should match expected");
      assertEquals(expectedPlatformId, info.toString(), "toString should return platform ID");
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "testlib",
        "wasmtime4j",
        "native-lib",
        "lib123",
        "a",
        "very-long-library-name-with-dashes"
      })
  void testLibraryFileNameConstruction(final String libraryName) {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    final String fileName = info.getLibraryFileName(libraryName);

    assertNotNull(fileName, "Library file name should not be null");
    assertTrue(fileName.contains(libraryName), "File name should contain library name");
    assertTrue(
        fileName.endsWith(info.getOperatingSystem().getLibraryExtension()),
        "File name should end with correct extension");

    if (!info.getOperatingSystem().getLibraryPrefix().isEmpty()) {
      assertTrue(
          fileName.startsWith(info.getOperatingSystem().getLibraryPrefix()),
          "File name should start with correct prefix when prefix is not empty");
    }
  }

  @Test
  void testPlatformDetectionCaching() {
    // Clear cache first
    PlatformDetectorTestUtils.clearCache();

    final PlatformDetector.PlatformInfo first = PlatformDetector.detect();
    final PlatformDetector.PlatformInfo second = PlatformDetector.detect();

    // Should return the same instance due to caching
    assertEquals(first, second, "Multiple calls should return equal instances due to caching");
    assertEquals(
        first.hashCode(), second.hashCode(), "Hash codes should be equal for cached instances");
  }

  @Test
  void testIsPlatformSupportedConsistency() {
    final boolean isSupported = PlatformDetector.isPlatformSupported();
    final boolean secondCheck = PlatformDetector.isPlatformSupported();

    assertEquals(isSupported, secondCheck, "isPlatformSupported should return consistent results");

    if (isSupported) {
      // If supported, detect() should not throw
      final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
      assertNotNull(info, "Platform info should not be null when platform is supported");
    }
  }
}
