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

import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Utility class for detecting the current platform (operating system and architecture).
 *
 * <p>This class provides consistent platform detection across all wasmtime4j modules. It normalizes
 * operating system and architecture names to standard values used throughout the project.
 *
 * <p>The detector supports automatic detection of the runtime platform and provides methods for
 * constructing platform-specific resource paths and library names.
 */
public final class PlatformDetector {

  private static final Logger LOGGER = Logger.getLogger(PlatformDetector.class.getName());

  /** Supported operating systems. */
  public enum OperatingSystem {
    LINUX("linux", ".so", "lib"),
    WINDOWS("windows", ".dll", ""),
    MACOS("macos", ".dylib", "lib");

    private final String name;
    private final String libraryExtension;
    private final String libraryPrefix;

    OperatingSystem(final String name, final String libraryExtension, final String libraryPrefix) {
      this.name = name;
      this.libraryExtension = libraryExtension;
      this.libraryPrefix = libraryPrefix;
    }

    /**
     * Gets the normalized operating system name.
     *
     * @return the operating system name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the file extension for native libraries on this operating system.
     *
     * @return the library file extension (including the dot)
     */
    public String getLibraryExtension() {
      return libraryExtension;
    }

    /**
     * Gets the prefix for native libraries on this operating system.
     *
     * @return the library prefix (e.g., "lib" for Unix systems, empty for Windows)
     */
    public String getLibraryPrefix() {
      return libraryPrefix;
    }
  }

  /** Supported CPU architectures. */
  public enum Architecture {
    X86_64("x86_64"),
    AARCH64("aarch64");

    private final String name;

    Architecture(final String name) {
      this.name = name;
    }

    /**
     * Gets the normalized architecture name.
     *
     * @return the architecture name
     */
    public String getName() {
      return name;
    }
  }

  /** Information about the detected platform. */
  public static final class PlatformInfo {
    private final OperatingSystem operatingSystem;
    private final Architecture architecture;

    PlatformInfo(final OperatingSystem operatingSystem, final Architecture architecture) {
      this.operatingSystem =
          Objects.requireNonNull(operatingSystem, "operatingSystem must not be null");
      this.architecture = Objects.requireNonNull(architecture, "architecture must not be null");
    }

    /**
     * Gets the operating system.
     *
     * @return the operating system
     */
    public OperatingSystem getOperatingSystem() {
      return operatingSystem;
    }

    /**
     * Gets the architecture.
     *
     * @return the architecture
     */
    public Architecture getArchitecture() {
      return architecture;
    }

    /**
     * Gets the platform identifier string (e.g., "linux-x86_64").
     *
     * @return the platform identifier
     */
    public String getPlatformId() {
      return operatingSystem.getName() + "-" + architecture.getName();
    }

    /**
     * Gets the native library file name for the given library name.
     *
     * @param libraryName the base library name (without prefix or extension)
     * @return the complete library file name
     */
    public String getLibraryFileName(final String libraryName) {
      Objects.requireNonNull(libraryName, "libraryName must not be null");
      final String sanitizedName = sanitizeLibraryName(libraryName);
      return operatingSystem.getLibraryPrefix()
          + sanitizedName
          + operatingSystem.getLibraryExtension();
    }

    /**
     * Gets the resource path for a native library.
     *
     * @param libraryName the base library name
     * @return the complete resource path
     */
    public String getLibraryResourcePath(final String libraryName) {
      return "/natives/" + getPlatformId() + "/" + getLibraryFileName(libraryName);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final PlatformInfo that = (PlatformInfo) obj;
      return operatingSystem == that.operatingSystem && architecture == that.architecture;
    }

    @Override
    public int hashCode() {
      return Objects.hash(operatingSystem, architecture);
    }

    @Override
    public String toString() {
      return getPlatformId();
    }
  }

  /** Cache for the detected platform information. */
  private static volatile PlatformInfo cachedPlatformInfo;

  /** Private constructor to prevent instantiation of utility class. */
  private PlatformDetector() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Sanitizes a string for safe logging by removing CRLF injection characters.
   *
   * @param input the string to sanitize for logging
   * @return the sanitized string safe for logging
   */
  private static String sanitizeForLog(final String input) {
    if (input == null) {
      return "null";
    }
    // Remove all control and format characters to prevent log injection
    return input.replaceAll("[\\p{Cntrl}\\p{Cf}]", "_");
  }

  /**
   * Sanitizes a library name for safe use in file paths by removing malicious characters and path
   * traversal sequences. Optimized for performance.
   *
   * @param libraryName the library name to sanitize
   * @return the sanitized library name safe for use in file paths
   */
  private static String sanitizeLibraryName(final String libraryName) {
    if (libraryName == null) {
      return "";
    }

    // Fast path: if the library name looks safe, return as-is
    boolean needsSanitization = false;
    final int len = libraryName.length();
    for (int i = 0; i < len; i++) {
      final char c = libraryName.charAt(i);
      if (c < 32 || c == '/' || c == '\\' || c == '\0' || c == '\r' || c == '\n') {
        needsSanitization = true;
        break;
      }
    }

    // Also check for .. sequences
    if (!needsSanitization && libraryName.contains("..")) {
      needsSanitization = true;
    }

    if (!needsSanitization) {
      return libraryName;
    }

    // Slow path: build sanitized string
    final StringBuilder result = new StringBuilder(len);
    boolean lastWasDot = false;
    for (int i = 0; i < len; i++) {
      final char c = libraryName.charAt(i);
      if (c >= 32 && c != '/' && c != '\\' && c != '\0' && c != '\r' && c != '\n') {
        if (c == '.') {
          if (lastWasDot) {
            // Skip second dot in .. sequence
            lastWasDot = false;
            continue;
          }
          lastWasDot = true;
        } else {
          lastWasDot = false;
        }
        result.append(c);
      }
    }

    return result.toString();
  }

  /**
   * Detects and returns information about the current platform.
   *
   * <p>This method caches the result after the first call for performance.
   *
   * @return the platform information
   * @throws RuntimeException if the current platform is not supported
   */
  public static PlatformInfo detect() {
    PlatformInfo result = cachedPlatformInfo;
    if (result == null) {
      synchronized (PlatformDetector.class) {
        result = cachedPlatformInfo;
        if (result == null) {
          result = detectPlatform();
          cachedPlatformInfo = result;
          LOGGER.info("Detected platform: " + sanitizeForLog(result.getPlatformId()));
        }
      }
    }
    return result;
  }

  /**
   * Detects the current operating system.
   *
   * @return the operating system
   * @throws RuntimeException if the operating system is not supported
   */
  public static OperatingSystem detectOperatingSystem() {
    return detect().getOperatingSystem();
  }

  /**
   * Detects the current CPU architecture.
   *
   * @return the architecture
   * @throws RuntimeException if the architecture is not supported
   */
  public static Architecture detectArchitecture() {
    return detect().getArchitecture();
  }

  /**
   * Checks if the current platform is supported.
   *
   * @return true if the platform is supported, false otherwise
   */
  public static boolean isPlatformSupported() {
    try {
      detect();
      return true;
    } catch (final RuntimeException e) {
      return false;
    }
  }

  /**
   * Gets a human-readable description of the current platform.
   *
   * @return the platform description
   */
  public static String getPlatformDescription() {
    final String osName = System.getProperty("os.name");
    final String osArch = System.getProperty("os.arch");
    final String javaVersion = System.getProperty("java.version");

    try {
      final PlatformInfo info = detect();
      return String.format(
          "Platform: %s (detected as %s), Java: %s",
          osName + " " + osArch, info.getPlatformId(), javaVersion);
    } catch (final RuntimeException e) {
      return String.format(
          "Platform: %s %s (unsupported), Java: %s, Error: %s",
          osName, osArch, javaVersion, e.getMessage());
    }
  }

  /**
   * Performs the actual platform detection.
   *
   * @return the platform information
   * @throws RuntimeException if the platform is not supported
   */
  private static PlatformInfo detectPlatform() {
    final OperatingSystem os = detectOperatingSystemInternal();
    final Architecture arch = detectArchitectureInternal();
    return new PlatformInfo(os, arch);
  }

  /**
   * Internal method to detect the operating system.
   *
   * @return the operating system
   * @throws RuntimeException if not supported
   */
  private static OperatingSystem detectOperatingSystemInternal() {
    final String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    if (osName.contains("linux")) {
      return OperatingSystem.LINUX;
    } else if (osName.contains("windows")) {
      return OperatingSystem.WINDOWS;
    } else if (osName.contains("mac") || osName.contains("darwin")) {
      return OperatingSystem.MACOS;
    } else {
      throw new RuntimeException("Unsupported operating system: " + osName);
    }
  }

  /**
   * Internal method to detect the CPU architecture.
   *
   * @return the architecture
   * @throws RuntimeException if not supported
   */
  private static Architecture detectArchitectureInternal() {
    final String archName = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);

    if ("amd64".equals(archName) || "x86_64".equals(archName)) {
      return Architecture.X86_64;
    } else if ("aarch64".equals(archName) || "arm64".equals(archName)) {
      return Architecture.AARCH64;
    } else {
      throw new RuntimeException("Unsupported architecture: " + archName);
    }
  }
}
