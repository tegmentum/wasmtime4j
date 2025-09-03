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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/**
 * Error handling and edge case tests for {@link PlatformDetector}.
 *
 * <p>This test class focuses on comprehensive error handling scenarios including:
 *
 * <ul>
 *   <li>System property access failures
 *   <li>Security manager restrictions
 *   <li>Null and empty input handling
 *   <li>Invalid platform combinations
 *   <li>Resource access failures
 *   <li>Edge cases and boundary conditions
 * </ul>
 */
@DisplayName("PlatformDetector Error Handling Tests")
final class PlatformDetectorErrorHandlingTest {

  @AfterEach
  void tearDown() {
    PlatformDetectorTestUtils.clearCache();
  }

  /**
   * Provides various null and invalid system property scenarios.
   *
   * @return stream of test arguments with property names and values
   */
  private static Stream<Arguments> provideInvalidSystemPropertyScenarios() {
    return Stream.of(
        Arguments.of("null os.name", null, "x86_64"),
        Arguments.of("null os.arch", "Linux", null),
        Arguments.of("both null", null, null),
        Arguments.of("empty os.name", "", "x86_64"),
        Arguments.of("empty os.arch", "Linux", ""),
        Arguments.of("whitespace os.name", "   ", "x86_64"),
        Arguments.of("whitespace os.arch", "Linux", "   "),
        Arguments.of("very long os.name", "A".repeat(10000), "x86_64"),
        Arguments.of("very long os.arch", "Linux", "B".repeat(10000)));
  }

  /**
   * Provides edge case platform combinations that should fail gracefully.
   *
   * @return stream of edge case platform arguments
   */
  private static Stream<Arguments> provideEdgeCasePlatforms() {
    return Stream.of(
        Arguments.of("case sensitivity mix", "lInUx", "X86_64"),
        Arguments.of("extra spaces", "  Linux  ", "  x86_64  "),
        Arguments.of("mixed case arch", "Linux", "AaRcH64"),
        Arguments.of("partial match OS", "MyLinuxDistro", "x86_64"),
        Arguments.of("partial match arch", "Linux", "custom-x86_64"),
        Arguments.of("numeric OS", "Linux123", "x86_64"),
        Arguments.of("numeric arch", "Linux", "x86_64_v2"));
  }

  @ParameterizedTest(name = "Should handle {0}")
  @MethodSource("provideInvalidSystemPropertyScenarios")
  @DisplayName("Should handle invalid system property scenarios gracefully")
  @Disabled("System.class mocking causes infinite loops in newer Mockito versions")
  void testInvalidSystemPropertyHandling(
      final String scenario, final String osName, final String osArch) {
    try (final MockedStatic<System> systemMock = mockStatic(System.class)) {
      systemMock.when(() -> System.getProperty("os.name")).thenReturn(osName);
      systemMock.when(() -> System.getProperty("os.arch")).thenReturn(osArch);
      systemMock
          .when(() -> System.getProperty("java.version"))
          .thenReturn(System.getProperty("java.version"));

      PlatformDetectorTestUtils.clearCache();

      // Should throw RuntimeException for invalid combinations
      final RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              PlatformDetector::detect,
              "Should throw RuntimeException for scenario: " + scenario);

      assertNotNull(exception.getMessage(), "Exception message should not be null");

      // Verify isPlatformSupported returns false for these cases
      PlatformDetectorTestUtils.clearCache();
      assertFalse(
          PlatformDetector.isPlatformSupported(),
          "isPlatformSupported should return false for invalid scenarios");

      // Verify platform description handles the error gracefully
      final String description = PlatformDetector.getPlatformDescription();
      assertNotNull(description, "Platform description should not be null even on error");
      assertTrue(
          description.contains("unsupported") || description.contains("Error"),
          "Description should indicate error condition");
    }
  }

  @Test
  @DisplayName("Should handle system property access failures gracefully")
  @Disabled("System.class mocking causes infinite loops in newer Mockito versions")
  void testSystemPropertyAccessFailures() {
    try (final MockedStatic<System> systemMock = mockStatic(System.class)) {
      // Mock system property access to throw SecurityException
      systemMock
          .when(() -> System.getProperty("os.name"))
          .thenThrow(new SecurityException("Access denied"));
      systemMock.when(() -> System.getProperty("os.arch")).thenReturn("x86_64");

      PlatformDetectorTestUtils.clearCache();

      // Should throw RuntimeException wrapping the SecurityException
      final RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              PlatformDetector::detect,
              "Should throw RuntimeException when system property access fails");

      // Verify the cause is preserved
      assertTrue(
          exception.getCause() instanceof SecurityException
              || exception.getMessage().contains("Access denied"),
          "Exception should indicate security access failure");
    }
  }

  @ParameterizedTest(name = "Edge case: {0}")
  @MethodSource("provideEdgeCasePlatforms")
  @DisplayName("Should handle edge case platform strings appropriately")
  @Disabled("System.class mocking causes infinite loops in newer Mockito versions")
  void testEdgeCasePlatforms(final String caseName, final String osName, final String osArch) {
    try (final MockedStatic<System> systemMock = mockStatic(System.class)) {
      systemMock.when(() -> System.getProperty("os.name")).thenReturn(osName);
      systemMock.when(() -> System.getProperty("os.arch")).thenReturn(osArch);
      systemMock
          .when(() -> System.getProperty("java.version"))
          .thenReturn(System.getProperty("java.version"));

      PlatformDetectorTestUtils.clearCache();

      try {
        final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
        // If detection succeeds, verify the result is valid
        assertNotNull(info, "Platform info should not be null if detection succeeds");
        assertNotNull(info.getOperatingSystem(), "Operating system should not be null");
        assertNotNull(info.getArchitecture(), "Architecture should not be null");
        assertNotNull(info.getPlatformId(), "Platform ID should not be null");
        assertFalse(info.getPlatformId().isEmpty(), "Platform ID should not be empty");
      } catch (final RuntimeException e) {
        // Expected for unsupported edge cases - verify error message is helpful
        assertNotNull(e.getMessage(), "Error message should not be null");
        assertTrue(
            e.getMessage().contains("Unsupported"),
            "Error message should indicate unsupported platform/architecture");
      }
    }
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  ", "\t", "\n", "\r\n", "\0"})
  @DisplayName("Should handle null and invalid library names in PlatformInfo methods")
  void testPlatformInfoWithInvalidLibraryNames(final String libraryName) {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();

    if (libraryName == null) {
      // Null should throw NullPointerException
      assertThrows(
          NullPointerException.class,
          () -> info.getLibraryFileName(libraryName),
          "getLibraryFileName should throw NPE for null input");
      assertThrows(
          NullPointerException.class,
          () -> info.getLibraryResourcePath(libraryName),
          "getLibraryResourcePath should throw NPE for null input");
    } else {
      // Other invalid strings should be handled gracefully
      final String fileName = info.getLibraryFileName(libraryName);
      assertNotNull(fileName, "Library file name should not be null");
      assertTrue(
          fileName.endsWith(info.getOperatingSystem().getLibraryExtension()),
          "File name should end with correct extension");

      final String resourcePath = info.getLibraryResourcePath(libraryName);
      assertNotNull(resourcePath, "Resource path should not be null");
      assertTrue(resourcePath.startsWith("/natives/"), "Resource path should start with /natives/");
    }
  }

  @Test
  @DisplayName("Should handle PlatformInfo construction with null values")
  void testPlatformInfoConstructionWithNulls() {
    // Test construction via reflection with null values
    assertThrows(
        RuntimeException.class,
        () ->
            PlatformDetectorTestUtils.createPlatformInfo(
                null, PlatformDetector.Architecture.X86_64),
        "Should reject null operating system");

    assertThrows(
        RuntimeException.class,
        () ->
            PlatformDetectorTestUtils.createPlatformInfo(
                PlatformDetector.OperatingSystem.LINUX, null),
        "Should reject null architecture");

    assertThrows(
        RuntimeException.class,
        () -> PlatformDetectorTestUtils.createPlatformInfo(null, null),
        "Should reject both null values");
  }

  @Test
  @DisplayName("Should handle cache corruption gracefully")
  void testCacheCorruptionHandling() {
    // First, populate the cache
    final PlatformDetector.PlatformInfo first = PlatformDetector.detect();
    assertNotNull(first, "First detection should succeed");

    // Clear cache and detect again - should work consistently
    PlatformDetectorTestUtils.clearCache();
    final PlatformDetector.PlatformInfo second = PlatformDetector.detect();
    assertNotNull(second, "Second detection should succeed after cache clear");
    assertEquals(first, second, "Results should be consistent after cache operations");
  }

  @Test
  @DisplayName("Should handle extremely long input strings")
  @Disabled("System.class mocking causes infinite loops in newer Mockito versions")
  void testExtremelyLongInputStrings() {
    final String veryLongOsName = "Linux" + "A".repeat(100000);
    final String veryLongArchName = "x86_64" + "B".repeat(100000);

    try (final MockedStatic<System> systemMock = mockStatic(System.class)) {
      systemMock.when(() -> System.getProperty("os.name")).thenReturn(veryLongOsName);
      systemMock.when(() -> System.getProperty("os.arch")).thenReturn(veryLongArchName);

      PlatformDetectorTestUtils.clearCache();

      // Should handle extremely long strings without crashing
      // May throw RuntimeException for unsupported platform, but should not cause OutOfMemoryError
      assertThrows(
          RuntimeException.class,
          PlatformDetector::detect,
          "Should throw RuntimeException for unsupported extremely long input");
    }
  }

  @Test
  @DisplayName("Should handle special Unicode characters in system properties")
  @Disabled("System.class mocking causes infinite loops in newer Mockito versions")
  void testUnicodeCharacterHandling() {
    final String unicodeOsName = "Linux" + "中文αβγ";
    final String unicodeArchName = "x86_64" + "ABC";

    try (final MockedStatic<System> systemMock = mockStatic(System.class)) {
      systemMock.when(() -> System.getProperty("os.name")).thenReturn(unicodeOsName);
      systemMock.when(() -> System.getProperty("os.arch")).thenReturn(unicodeArchName);

      PlatformDetectorTestUtils.clearCache();

      try {
        final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
        // If it succeeds (because "Linux" substring is found), verify it's valid
        assertNotNull(info, "Platform info should not be null");
        assertEquals(
            PlatformDetector.OperatingSystem.LINUX,
            info.getOperatingSystem(),
            "Should detect Linux despite Unicode characters");
      } catch (final RuntimeException e) {
        // Expected if architecture is not recognized
        assertNotNull(e.getMessage(), "Exception message should not be null");
      }
    }
  }

  @Test
  @DisplayName("Should provide consistent error messages")
  @Disabled("System.class mocking causes infinite loops in newer Mockito versions")
  void testConsistentErrorMessages() {
    try (final MockedStatic<System> systemMock = mockStatic(System.class)) {
      systemMock.when(() -> System.getProperty("os.name")).thenReturn("UnsupportedOS");
      systemMock.when(() -> System.getProperty("os.arch")).thenReturn("x86_64");

      PlatformDetectorTestUtils.clearCache();

      final RuntimeException osException =
          assertThrows(RuntimeException.class, PlatformDetector::detect);

      // Test consistent architecture error
      systemMock.when(() -> System.getProperty("os.name")).thenReturn("Linux");
      systemMock.when(() -> System.getProperty("os.arch")).thenReturn("UnsupportedArch");

      PlatformDetectorTestUtils.clearCache();

      final RuntimeException archException =
          assertThrows(RuntimeException.class, PlatformDetector::detect);

      // Verify error message patterns
      assertTrue(
          osException.getMessage().contains("Unsupported operating system"),
          "OS error should mention unsupported operating system");
      assertTrue(
          archException.getMessage().contains("Unsupported architecture"),
          "Architecture error should mention unsupported architecture");
    }
  }

  @Test
  @DisplayName("Should handle detection method delegation correctly")
  void testDetectionMethodDelegation() {
    // Clear cache to ensure fresh detection
    PlatformDetectorTestUtils.clearCache();

    final PlatformDetector.PlatformInfo fullInfo = PlatformDetector.detect();
    final PlatformDetector.OperatingSystem os = PlatformDetector.detectOperatingSystem();
    final PlatformDetector.Architecture arch = PlatformDetector.detectArchitecture();

    // Verify delegation consistency
    assertEquals(
        fullInfo.getOperatingSystem(), os, "detectOperatingSystem should match detect() result");
    assertEquals(
        fullInfo.getArchitecture(), arch, "detectArchitecture should match detect() result");

    // Test that multiple calls are consistent due to caching
    assertEquals(
        os,
        PlatformDetector.detectOperatingSystem(),
        "Multiple detectOperatingSystem calls should be consistent");
    assertEquals(
        arch,
        PlatformDetector.detectArchitecture(),
        "Multiple detectArchitecture calls should be consistent");
  }
}
