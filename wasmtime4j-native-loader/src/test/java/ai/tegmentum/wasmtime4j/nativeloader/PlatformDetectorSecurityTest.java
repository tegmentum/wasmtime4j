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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

/**
 * Security validation tests for {@link PlatformDetector}.
 *
 * <p>This test class focuses on security aspects including:
 *
 * <ul>
 *   <li>Log injection prevention
 *   <li>Path traversal protection
 *   <li>Input validation
 *   <li>Resource path sanitization
 *   <li>Malicious input handling
 * </ul>
 */
@DisplayName("PlatformDetector Security Tests")
final class PlatformDetectorSecurityTest {

  private TestLogHandler testLogHandler;
  private Logger platformDetectorLogger;

  @BeforeEach
  void setUp() {
    testLogHandler = new TestLogHandler();
    platformDetectorLogger = Logger.getLogger(PlatformDetector.class.getName());
    platformDetectorLogger.addHandler(testLogHandler);
    platformDetectorLogger.setLevel(Level.ALL);
  }

  @AfterEach
  void tearDown() {
    if (platformDetectorLogger != null && testLogHandler != null) {
      platformDetectorLogger.removeHandler(testLogHandler);
    }
    PlatformDetectorTestUtils.clearCache();
  }

  /**
   * Provides malicious input data for security testing.
   *
   * @return stream of malicious input arguments
   */
  private static Stream<Arguments> provideMaliciousInputData() {
    return Stream.of(
        Arguments.of("log injection", "Linux\r\nINFO: Malicious log entry\n", "x86_64"),
        Arguments.of("CRLF injection", "Windows\r\n[EVIL] Injected\n", "amd64"),
        Arguments.of("tab injection", "macOS\t[INJECT]\t", "aarch64"),
        Arguments.of("null byte", "Linux\0admin", "x86_64"),
        Arguments.of("format string", "Windows%s%d%x", "amd64"),
        Arguments.of("unicode control", "Linux\u0001\u0002\u0003", "x86_64"),
        Arguments.of("vertical tab", "macOS\u000B[INJECT]", "aarch64"),
        Arguments.of("form feed", "Windows\f[MALICIOUS]", "x86_64"),
        Arguments.of("backspace", "Linux\b[EVIL]\b", "amd64"),
        Arguments.of("escape sequence", "macOS\u001B[31m[RED]\u001B[0m", "aarch64"));
  }

  /**
   * Provides path traversal attack strings for security testing.
   *
   * @return stream of path traversal strings
   */
  private static Stream<String> providePathTraversalStrings() {
    return Stream.of(
        "../../../etc/passwd",
        "..\\\\..\\\\..\\\\windows\\\\system32\\\\config\\\\sam",
        "/../../../../etc/shadow",
        "....//....//....//etc/hosts",
        "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd",
        "..%252f..%252f..%252fetc%252fpasswd",
        "..%c0%af..%c0%af..%c0%afetc%c0%afpasswd",
        "\\\\..\\\\..\\\\..\\\\etc\\\\passwd",
        "/var/../../../etc/passwd",
        "C:\\\\..\\\\..\\\\Windows\\\\System32\\\\drivers\\\\etc\\\\hosts");
  }

  @ParameterizedTest(name = "Should safely handle {0}: {1}")
  @MethodSource("provideMaliciousInputData")
  @DisplayName("Should prevent log injection attacks")
  @Disabled("System.class mocking causes infinite loops in newer Mockito versions")
  void testLogInjectionPrevention(
      final String attackType, final String maliciousOsName, final String osArch) {
    try (final MockedStatic<System> systemMock = mockStatic(System.class)) {
      systemMock.when(() -> System.getProperty("os.name")).thenReturn(maliciousOsName);
      systemMock.when(() -> System.getProperty("os.arch")).thenReturn(osArch);
      systemMock
          .when(() -> System.getProperty("java.version"))
          .thenReturn(System.getProperty("java.version"));

      PlatformDetectorTestUtils.clearCache();

      // This should not throw but may not detect a valid platform
      assertDoesNotThrow(
          () -> {
            try {
              final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
              // If detection succeeds, verify the platform ID is sanitized
              if (info != null) {
                final String platformId = info.getPlatformId();
                assertNotNull(platformId, "Platform ID should not be null");
                assertFalse(
                    platformId.contains("\r"), "Platform ID should not contain carriage return");
                assertFalse(platformId.contains("\n"), "Platform ID should not contain line feed");
                assertFalse(platformId.contains("\t"), "Platform ID should not contain tab");
              }
            } catch (final RuntimeException e) {
              // Expected for unsupported platforms - this is fine
            }
          },
          "Should not throw exceptions when processing malicious input");

      // Check that log messages are sanitized
      final String logOutput = testLogHandler.getCapturedOutput();
      if (!logOutput.isEmpty()) {
        assertFalse(
            logOutput.contains("\r\n"),
            "Log output should not contain CRLF sequences from malicious input");
        assertFalse(
            logOutput.contains("EVIL"), "Log output should not contain injected malicious content");
        assertFalse(logOutput.contains("INJECT"), "Log output should not contain injected content");
        assertFalse(
            logOutput.contains("MALICIOUS"), "Log output should not contain malicious markers");
      }
    }
  }

  @ParameterizedTest
  @MethodSource("providePathTraversalStrings")
  @DisplayName("Should prevent path traversal in resource paths")
  void testPathTraversalPrevention(final String pathTraversalString) {
    // Create a platform info with known good values
    final PlatformDetector.PlatformInfo info =
        PlatformDetectorTestUtils.createPlatformInfo(
            PlatformDetector.OperatingSystem.LINUX, PlatformDetector.Architecture.X86_64);

    // Test that library resource paths don't allow path traversal
    final String resourcePath = info.getLibraryResourcePath(pathTraversalString);
    assertNotNull(resourcePath, "Resource path should not be null");
    assertTrue(resourcePath.startsWith("/natives/"), "Resource path should start with /natives/");

    // Verify the path doesn't contain obvious traversal sequences
    assertFalse(resourcePath.contains("../"), "Resource path should not contain ../ sequences");
    assertFalse(
        resourcePath.contains("..\\\\"), "Resource path should not contain ..\\\\ sequences");
    assertFalse(
        resourcePath.contains("/etc/"), "Resource path should not reference /etc/ directory");
    assertFalse(
        resourcePath.contains("/root/"), "Resource path should not reference /root/ directory");
    assertFalse(
        resourcePath.contains("/tmp/"), "Resource path should not reference /tmp/ directory");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "", // empty string
        "   ", // whitespace only
        "\0", // null byte
        "\r\n", // CRLF
        "../../../", // path traversal
        "/etc/passwd", // absolute path
        "C:\\\\Windows\\\\System32", // Windows absolute path
        "library\u0000.dll", // embedded null
        "library\r\n.so", // CRLF injection
        "very-very-very-very-very-very-very-very-long-library-name-that-exceeds-reasonable-limits"
      })
  @DisplayName("Should handle malicious library names safely")
  void testMaliciousLibraryNames(final String maliciousLibraryName) {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();

    assertDoesNotThrow(
        () -> {
          final String fileName = info.getLibraryFileName(maliciousLibraryName);
          assertNotNull(fileName, "Library file name should not be null");

          // Verify the file name is sanitized
          assertFalse(fileName.contains("\0"), "File name should not contain null bytes");
          assertFalse(fileName.contains("\r"), "File name should not contain carriage returns");
          assertFalse(fileName.contains("\n"), "File name should not contain line feeds");
          assertFalse(fileName.contains("../"), "File name should not contain path traversal");
          assertFalse(
              fileName.contains("..\\"), "File name should not contain Windows path traversal");
        },
        "Should handle malicious library names without throwing exceptions");

    assertDoesNotThrow(
        () -> {
          final String resourcePath = info.getLibraryResourcePath(maliciousLibraryName);
          assertNotNull(resourcePath, "Resource path should not be null");
          assertTrue(
              resourcePath.startsWith("/natives/"),
              "Resource path should always start with /natives/");
        },
        "Should handle malicious library names in resource paths without throwing exceptions");
  }

  @Test
  @DisplayName("Should sanitize platform description output")
  @Disabled("System.class mocking causes infinite loops in newer Mockito versions")
  void testPlatformDescriptionSanitization() {
    // Test with potentially malicious system properties
    try (final MockedStatic<System> systemMock = mockStatic(System.class)) {
      systemMock.when(() -> System.getProperty("os.name")).thenReturn("Linux\r\n[INJECTED LOG]\n");
      systemMock.when(() -> System.getProperty("os.arch")).thenReturn("x86_64\t[EVIL]");
      systemMock.when(() -> System.getProperty("java.version")).thenReturn("11.0.1\0[MALICIOUS]");

      PlatformDetectorTestUtils.clearCache();

      final String description = PlatformDetector.getPlatformDescription();
      assertNotNull(description, "Platform description should not be null");

      // Verify description is sanitized
      assertFalse(description.contains("\r"), "Description should not contain carriage returns");
      assertFalse(description.contains("\n"), "Description should not contain line feeds");
      assertFalse(description.contains("\t"), "Description should not contain tabs");
      assertFalse(description.contains("\0"), "Description should not contain null bytes");
      assertFalse(description.contains("INJECTED"), "Description should not contain injected text");
      assertFalse(description.contains("EVIL"), "Description should not contain malicious text");
      assertFalse(
          description.contains("MALICIOUS"), "Description should not contain malicious text");
    }
  }

  @Test
  @DisplayName("Should handle concurrent access to platform detection safely")
  void testConcurrentAccessSafety() {
    PlatformDetectorTestUtils.clearCache();

    final int threadCount = 10;
    final Thread[] threads = new Thread[threadCount];
    final PlatformDetector.PlatformInfo[] results = new PlatformDetector.PlatformInfo[threadCount];

    // Start multiple threads that access platform detection simultaneously
    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] =
          new Thread(
              () -> {
                results[index] = PlatformDetector.detect();
              });
      threads[i].start();
    }

    // Wait for all threads to complete
    assertDoesNotThrow(
        () -> {
          for (final Thread thread : threads) {
            thread.join(5000); // 5-second timeout
          }
        },
        "All threads should complete without timing out");

    // Verify all results are consistent
    final PlatformDetector.PlatformInfo first = results[0];
    assertNotNull(first, "First result should not be null");

    for (int i = 1; i < threadCount; i++) {
      assertNotNull(results[i], "Result " + i + " should not be null");
      assertTrue(
          first.equals(results[i]),
          "All concurrent detection results should be equal due to caching");
    }
  }

  @Test
  @DisplayName("Should prevent instantiation of utility class")
  void testUtilityClassInstantiationPrevention() {
    assertDoesNotThrow(
        () -> {
          try {
            final java.lang.reflect.Constructor<PlatformDetector> constructor =
                PlatformDetector.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            // If we reach here, the constructor didn't throw - this is bad
            assertTrue(false, "Utility class constructor should throw AssertionError");
          } catch (final java.lang.reflect.InvocationTargetException e) {
            // Expected - the constructor should throw AssertionError
            assertTrue(
                e.getCause() instanceof AssertionError,
                "Constructor should throw AssertionError to prevent instantiation");
          }
        },
        "Reflection operations should not throw unexpected exceptions");
  }

  /** Custom log handler for capturing log output during tests. */
  private static final class TestLogHandler extends Handler {
    private final StringBuilder logOutput = new StringBuilder();

    public TestLogHandler() {
      setFormatter(new SimpleFormatter());
    }

    @Override
    public void publish(final LogRecord record) {
      if (isLoggable(record)) {
        logOutput.append(getFormatter().format(record));
      }
    }

    @Override
    public void flush() {
      // No-op for in-memory handler
    }

    @Override
    public void close() throws SecurityException {
      // No-op for in-memory handler
    }

    String getCapturedOutput() {
      return logOutput.toString();
    }
  }
}
