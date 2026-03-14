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

import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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
  void testInvalidSystemPropertyHandling(
      final String scenario, final String osName, final String osArch) {
    // Using testable detect(String, String) method instead of mocking System.getProperty()

    if (osName == null || osArch == null) {
      // Null values should throw NullPointerException from the detect(String, String) method
      assertThrows(
          NullPointerException.class,
          () -> PlatformDetector.detect(osName, osArch),
          "Should throw NullPointerException for null inputs in scenario: " + scenario);
    } else {
      // Other invalid strings should throw UnsupportedOperationException
      final RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> PlatformDetector.detect(osName, osArch),
              "Should throw RuntimeException for scenario: " + scenario);

      assertNotNull(exception.getMessage(), "Exception message should not be null");
    }
  }

  @Test
  void testNullInputHandling() {
    // Test that null inputs are properly handled by the testable methods

    // Null OS name should throw appropriate exception
    assertThrows(
        UnsupportedOperationException.class,
        () -> PlatformDetector.detectOperatingSystemFromString(null),
        "Should throw UnsupportedOperationException for null OS name");

    // Null arch name should throw appropriate exception
    assertThrows(
        UnsupportedOperationException.class,
        () -> PlatformDetector.detectArchitectureFromString(null),
        "Should throw UnsupportedOperationException for null arch name");

    // Null values to detect(String, String) should throw NullPointerException
    assertThrows(
        NullPointerException.class,
        () -> PlatformDetector.detect(null, "x86_64"),
        "Should throw NullPointerException for null OS name in detect");

    assertThrows(
        NullPointerException.class,
        () -> PlatformDetector.detect("Linux", null),
        "Should throw NullPointerException for null arch name in detect");
  }

  @ParameterizedTest(name = "Edge case: {0}")
  @MethodSource("provideEdgeCasePlatforms")
  void testEdgeCasePlatforms(final String caseName, final String osName, final String osArch) {
    // Using testable detect(String, String) method instead of mocking System.getProperty()
    try {
      final PlatformDetector.PlatformInfo info = PlatformDetector.detect(osName, osArch);
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

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  ", "\t", "\n", "\r\n", "\0"})
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
  void testExtremelyLongInputStrings() {
    final String veryLongOsName = "Linux" + "A".repeat(100000);
    final String veryLongArchName = "x86_64" + "B".repeat(100000);

    // Using testable detect(String, String) method instead of mocking System.getProperty()
    // Should handle extremely long strings without crashing
    // May throw RuntimeException for unsupported platform, but should not cause OutOfMemoryError
    try {
      final PlatformDetector.PlatformInfo info =
          PlatformDetector.detect(veryLongOsName, veryLongArchName);
      // If detection succeeds (Linux substring found), verify result is valid
      assertNotNull(info, "Platform info should not be null");
      assertEquals(
          PlatformDetector.OperatingSystem.LINUX,
          info.getOperatingSystem(),
          "Should detect Linux despite long string");
    } catch (final RuntimeException e) {
      // Expected if architecture is not recognized due to extra characters
      assertNotNull(e.getMessage(), "Exception message should not be null");
    }
  }

  @Test
  void testUnicodeCharacterHandling() {
    final String unicodeOsName = "Linux" + "中文αβγ";
    final String unicodeArchName = "x86_64";

    // Using testable detect(String, String) method instead of mocking System.getProperty()
    try {
      final PlatformDetector.PlatformInfo info =
          PlatformDetector.detect(unicodeOsName, unicodeArchName);
      // If it succeeds (because "Linux" substring is found), verify it's valid
      assertNotNull(info, "Platform info should not be null");
      assertEquals(
          PlatformDetector.OperatingSystem.LINUX,
          info.getOperatingSystem(),
          "Should detect Linux despite Unicode characters");
      assertEquals(
          PlatformDetector.Architecture.X86_64,
          info.getArchitecture(),
          "Should detect x86_64 architecture");
    } catch (final RuntimeException e) {
      // Unexpected - the detection should succeed for these inputs
      throw new AssertionError("Detection should succeed for valid OS with Unicode suffix", e);
    }
  }

  @Test
  void testConsistentErrorMessages() {
    // Using testable methods instead of mocking System.getProperty()

    // Test OS error message
    final RuntimeException osException =
        assertThrows(
            RuntimeException.class,
            () -> PlatformDetector.detect("UnsupportedOS", "x86_64"),
            "Should throw for unsupported OS");

    // Test architecture error message
    final RuntimeException archException =
        assertThrows(
            RuntimeException.class,
            () -> PlatformDetector.detect("Linux", "UnsupportedArch"),
            "Should throw for unsupported architecture");

    // Verify error message patterns
    assertTrue(
        osException.getMessage().contains("Unsupported operating system"),
        "OS error should mention unsupported operating system");
    assertTrue(
        archException.getMessage().contains("Unsupported architecture"),
        "Architecture error should mention unsupported architecture");
  }

  @Test
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
