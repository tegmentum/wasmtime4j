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

  /**
   * Cache for the detected platform information.
   *
   * <p>PMD: AvoidUsingVolatile - Volatile is required for double-checked locking pattern to ensure
   * visibility across threads. FieldDeclarationsShouldBeAtStartOfClass - Moved to top as required.
   */
  @SuppressWarnings("PMD.AvoidUsingVolatile")
  private static volatile PlatformInfo cachedPlatformInfo;

  /** Supported operating systems. */
  public enum OperatingSystem {
    LINUX("linux", ".so", "lib"),
    WINDOWS("windows", ".dll", ""),
    MACOS("darwin", ".dylib", "lib");

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
   * <p>PMD: CognitiveComplexity/CyclomaticComplexity - Security-critical path traversal prevention
   * logic requires thorough character-by-character validation. Splitting would reduce clarity.
   *
   * @param libraryName the library name to sanitize
   * @return the sanitized library name safe for use in file paths
   */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
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
   * <p>PMD: AvoidSynchronizedStatement - Synchronized required for thread-safe double-checked
   * locking pattern with volatile field.
   *
   * @return the platform information
   * @throws RuntimeException if the current platform is not supported
   */
  @SuppressWarnings("PMD.AvoidSynchronizedStatement")
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
   * Detects the platform using the provided OS name and architecture strings.
   *
   * <p>This method is provided for testing purposes to allow detection logic to be tested without
   * mocking System.getProperty(). The detection logic is identical to the standard detect() method.
   *
   * @param osName the operating system name (e.g., "Mac OS X", "Linux", "Windows 10")
   * @param osArch the architecture name (e.g., "amd64", "x86_64", "aarch64", "arm64")
   * @return the platform information
   * @throws RuntimeException if the platform is not supported
   * @throws NullPointerException if osName or osArch is null
   */
  public static PlatformInfo detect(final String osName, final String osArch) {
    Objects.requireNonNull(osName, "osName must not be null");
    Objects.requireNonNull(osArch, "osArch must not be null");

    final OperatingSystem os = detectOperatingSystemFromString(osName);
    final Architecture arch = detectArchitectureFromString(osArch);
    return new PlatformInfo(os, arch);
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
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
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
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
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
    final String osName = System.getProperty("os.name");
    return detectOperatingSystemFromString(osName);
  }

  /**
   * Detects the operating system from a given OS name string.
   *
   * <p>This method is provided for testing purposes.
   *
   * @param osName the operating system name string
   * @return the operating system
   * @throws UnsupportedOperationException if the operating system is not supported
   */
  public static OperatingSystem detectOperatingSystemFromString(final String osName) {
    if (osName == null) {
      throw new UnsupportedOperationException("Operating system name is null");
    }

    final String normalizedName = osName.toLowerCase(Locale.ENGLISH);

    if (normalizedName.contains("linux")) {
      return OperatingSystem.LINUX;
    } else if (normalizedName.contains("windows")) {
      return OperatingSystem.WINDOWS;
    } else if (normalizedName.contains("mac") || normalizedName.contains("darwin")) {
      return OperatingSystem.MACOS;
    } else {
      throw new UnsupportedOperationException("Unsupported operating system: " + osName);
    }
  }

  /**
   * Internal method to detect the CPU architecture.
   *
   * @return the architecture
   * @throws RuntimeException if not supported
   */
  private static Architecture detectArchitectureInternal() {
    final String archName = System.getProperty("os.arch");
    return detectArchitectureFromString(archName);
  }

  /**
   * Detects the CPU architecture from a given architecture name string.
   *
   * <p>This method is provided for testing purposes.
   *
   * @param archName the architecture name string
   * @return the architecture
   * @throws UnsupportedOperationException if the architecture is not supported
   */
  public static Architecture detectArchitectureFromString(final String archName) {
    if (archName == null) {
      throw new UnsupportedOperationException("Architecture name is null");
    }

    final String normalizedArch = archName.toLowerCase(Locale.ENGLISH);

    if ("amd64".equals(normalizedArch) || "x86_64".equals(normalizedArch)) {
      return Architecture.X86_64;
    } else if ("aarch64".equals(normalizedArch) || "arm64".equals(normalizedArch)) {
      return Architecture.AARCH64;
    } else {
      throw new UnsupportedOperationException("Unsupported architecture: " + archName);
    }
  }
}
