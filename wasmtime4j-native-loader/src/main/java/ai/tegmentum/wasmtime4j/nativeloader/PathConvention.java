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

import java.util.Objects;

/**
 * Defines different conventions for locating native libraries within JAR resources.
 *
 * <p>This enum provides predefined patterns for common native library packaging approaches used
 * across the Java ecosystem. Each convention defines a specific resource path pattern that supports
 * placeholder substitution for platform-specific values.
 *
 * <p><strong>Supported Placeholders:</strong>
 *
 * <ul>
 *   <li><code>{platform}</code> - Full platform identifier (e.g., "linux-x86_64")
 *   <li><code>{os}</code> - Operating system name (e.g., "linux", "windows", "darwin")
 *   <li><code>{arch}</code> - Architecture name (e.g., "x86_64", "aarch64")
 *   <li><code>{lib}</code> - Platform-specific library prefix (e.g., "lib" on Unix, "" on Windows)
 *   <li><code>{name}</code> - Library name without prefixes or extensions
 *   <li><code>{ext}</code> - Platform-specific library extension (e.g., ".so", ".dll", ".dylib")
 * </ul>
 *
 * <p><strong>Usage Examples:</strong>
 *
 * <pre>{@code
 * // Using predefined conventions
 * PathConvention convention = PathConvention.MAVEN_NATIVE;
 * String path = convention.generatePath("mylib", platformInfo);
 *
 * // Using custom convention
 * PathConvention custom = PathConvention.custom("/lib/{platform}/{name}{ext}");
 * String customPath = custom.generatePath("mylib", platformInfo);
 * }</pre>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.InefficientEmptyStringCheck")
public enum PathConvention {

  /**
   * Maven Native Plugin convention.
   *
   * <p>Pattern: {@code /natives/{os}-{arch}/{lib}{name}{ext}}
   *
   * <p>This convention matches the standard Maven Native Plugin resource layout used by many Java
   * native library projects.
   *
   * <p>Example paths:
   *
   * <ul>
   *   <li>Linux: {@code /natives/linux-x86_64/libwasmtime4j.so}
   *   <li>Windows: {@code /natives/windows-x86_64/wasmtime4j.dll}
   *   <li>macOS: {@code /natives/darwin-x86_64/libwasmtime4j.dylib}
   * </ul>
   */
  MAVEN_NATIVE("/natives/{os}-{arch}/{lib}{name}{ext}"),

  /**
   * JNA (Java Native Access) convention.
   *
   * <p>Pattern: {@code /{platform}/{name}{ext}}
   *
   * <p>This convention provides compatibility with JNA's resource loading approach, placing
   * libraries directly under platform directories without additional path prefixes.
   *
   * <p>Example paths:
   *
   * <ul>
   *   <li>Linux: {@code /linux-x86_64/wasmtime4j.so}
   *   <li>Windows: {@code /windows-x86_64/wasmtime4j.dll}
   *   <li>macOS: {@code /darwin-x86_64/wasmtime4j.dylib}
   * </ul>
   */
  JNA("/{platform}/{name}{ext}"),

  /**
   * Custom convention with user-defined pattern.
   *
   * <p>This special convention allows users to define their own resource path patterns. When using
   * CUSTOM, the pattern must be provided separately through configuration methods.
   *
   * <p>Custom patterns support all standard placeholders and undergo security validation to prevent
   * path traversal attacks.
   */
  CUSTOM("");

  /** The resource path pattern for this convention. */
  private final String pattern;

  /**
   * Creates a path convention with the specified pattern.
   *
   * @param pattern the resource path pattern with placeholders
   */
  PathConvention(final String pattern) {
    this.pattern = pattern;
  }

  /**
   * Gets the resource path pattern for this convention.
   *
   * @return the path pattern with placeholders
   */
  public String getPattern() {
    return pattern;
  }

  /**
   * Generates a complete resource path for the specified library and platform.
   *
   * <p>This method substitutes placeholders in the convention's pattern with actual values from the
   * platform information.
   *
   * @param libraryName the base name of the library
   * @param platformInfo the platform information for placeholder substitution
   * @return the complete resource path
   * @throws IllegalArgumentException if libraryName is null or empty
   * @throws IllegalArgumentException if platformInfo is null
   * @throws IllegalStateException if this is a CUSTOM convention without a pattern
   */
  public String generatePath(
      final String libraryName, final PlatformDetector.PlatformInfo platformInfo) {
    Objects.requireNonNull(libraryName, "libraryName must not be null");
    Objects.requireNonNull(platformInfo, "platformInfo must not be null");

    if (libraryName.trim().isEmpty()) {
      throw new IllegalArgumentException("libraryName must not be empty");
    }

    if (this == CUSTOM) {
      throw new IllegalStateException(
          "CUSTOM convention requires pattern to be set via customPattern() method");
    }

    return ResourcePathResolver.resolvePath(pattern, libraryName, platformInfo);
  }

  /**
   * Creates a custom path convention with the specified pattern.
   *
   * <p>This method creates a wrapper that can be used with convention-based loading. The pattern
   * supports all standard placeholders and undergoes security validation.
   *
   * @param customPattern the custom resource path pattern
   * @return a custom path convention wrapper
   * @throws IllegalArgumentException if customPattern is null or invalid
   */
  public static CustomPathConvention custom(final String customPattern) {
    Objects.requireNonNull(customPattern, "customPattern must not be null");

    if (customPattern.trim().isEmpty()) {
      throw new IllegalArgumentException("customPattern must not be empty");
    }

    // Validate the custom pattern for security
    ResourcePathResolver.validatePattern(customPattern);

    return new CustomPathConvention(customPattern);
  }

  /**
   * Wrapper class for custom path conventions with user-defined patterns.
   *
   * <p>PMD: AccessorClassGeneration - Private constructor is intentional for factory pattern.
   * InefficientEmptyStringCheck - Using trim().isEmpty() for Java 8 compatibility.
   */
  @SuppressWarnings({"PMD.AccessorClassGeneration", "PMD.InefficientEmptyStringCheck"})
  public static final class CustomPathConvention {
    private final String customPattern;

    CustomPathConvention(final String customPattern) {
      this.customPattern = customPattern;
    }

    /**
     * Gets the custom pattern.
     *
     * @return the custom pattern
     */
    public String getPattern() {
      return customPattern;
    }

    /**
     * Generates a path using the custom pattern.
     *
     * @param libraryName the library name
     * @param platformInfo the platform information
     * @return the generated path
     */
    public String generatePath(
        final String libraryName, final PlatformDetector.PlatformInfo platformInfo) {
      Objects.requireNonNull(libraryName, "libraryName must not be null");
      Objects.requireNonNull(platformInfo, "platformInfo must not be null");

      if (libraryName.trim().isEmpty()) {
        throw new IllegalArgumentException("libraryName must not be empty");
      }

      return ResourcePathResolver.resolvePath(customPattern, libraryName, platformInfo);
    }

    @Override
    public String toString() {
      return "PathConvention.CUSTOM(\"" + customPattern + "\")";
    }
  }

  @Override
  public String toString() {
    if (this == CUSTOM) {
      return "PathConvention.CUSTOM";
    }
    return "PathConvention." + name() + "(\"" + pattern + "\")";
  }
}
