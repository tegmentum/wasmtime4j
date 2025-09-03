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

import java.lang.reflect.Field;

/**
 * Utility methods for testing {@link PlatformDetector}.
 *
 * <p>This class provides test-specific utilities that use reflection to access internal state for
 * testing purposes. These methods should only be used in test code.
 */
final class PlatformDetectorTestUtils {

  /** Private constructor to prevent instantiation of utility class. */
  private PlatformDetectorTestUtils() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Clears the cached platform information in {@link PlatformDetector} to force re-detection.
   *
   * <p>This method uses reflection to access the private cache field and should only be used in
   * tests.
   *
   * @throws RuntimeException if reflection fails
   */
  static void clearCache() {
    try {
      final Field cachedPlatformInfoField =
          PlatformDetector.class.getDeclaredField("cachedPlatformInfo");
      cachedPlatformInfoField.setAccessible(true);
      cachedPlatformInfoField.set(null, null);
    } catch (final NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to clear PlatformDetector cache", e);
    }
  }

  /**
   * Creates a {@link PlatformDetector.PlatformInfo} instance for testing purposes.
   *
   * <p>This method uses reflection to create instances with specific OS and architecture
   * combinations for testing.
   *
   * @param os the operating system
   * @param arch the architecture
   * @return a new PlatformInfo instance
   * @throws RuntimeException if reflection fails
   */
  static PlatformDetector.PlatformInfo createPlatformInfo(
      final PlatformDetector.OperatingSystem os, final PlatformDetector.Architecture arch) {
    try {
      final Class<?>[] paramTypes = {
        PlatformDetector.OperatingSystem.class, PlatformDetector.Architecture.class
      };
      final java.lang.reflect.Constructor<PlatformDetector.PlatformInfo> constructor =
          PlatformDetector.PlatformInfo.class.getDeclaredConstructor(paramTypes);
      constructor.setAccessible(true);
      return constructor.newInstance(os, arch);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create PlatformInfo instance", e);
    }
  }

  /**
   * Simulates a malicious platform ID for security testing.
   *
   * @return a potentially dangerous platform ID string
   */
  static String getMaliciousPlatformId() {
    return "../../../etc/passwd";
  }

  /**
   * Creates test data with control characters for log injection testing.
   *
   * @return a string containing various control characters
   */
  static String getLogInjectionTestData() {
    return "test\r\nINFO: Injected log message\n\t\0x01\0x02";
  }

  /**
   * Creates test data with path traversal attempts.
   *
   * @return a string containing path traversal sequences
   */
  static String getPathTraversalTestData() {
    return "../../../etc/shadow";
  }
}
