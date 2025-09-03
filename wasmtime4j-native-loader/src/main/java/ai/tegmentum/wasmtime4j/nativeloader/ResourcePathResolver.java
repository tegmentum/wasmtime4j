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

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utility class for resolving resource paths using pattern substitution.
 *
 * <p>This class provides secure path resolution with placeholder substitution for native library
 * resource loading. It supports various placeholder patterns and includes security validation to
 * prevent path traversal attacks.
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
 * <p><strong>Security Features:</strong>
 *
 * <ul>
 *   <li>Path traversal attack prevention
 *   <li>Malicious pattern validation
 *   <li>Safe placeholder substitution
 *   <li>Input sanitization
 * </ul>
 *
 * @since 1.0.0
 */
final class ResourcePathResolver {

  /** Pattern to detect path traversal attempts. */
  private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile("\\.\\.[\\\\/]");

  /** Pattern to detect absolute paths in patterns. */
  private static final Pattern ABSOLUTE_PATH_PATTERN = Pattern.compile("^[a-zA-Z]:|^[\\\\/]");

  /** Pattern to validate placeholder syntax. */
  private static final Pattern PLACEHOLDER_PATTERN =
      Pattern.compile("\\{([a-zA-Z][a-zA-Z0-9_]*)\\}");

  /** Private constructor to prevent instantiation. */
  private ResourcePathResolver() {
    // Utility class
  }

  /**
   * Resolves a resource path by substituting placeholders with actual values.
   *
   * <p>This method performs secure placeholder substitution using the provided platform information
   * and library name. All paths are validated for security before resolution.
   *
   * @param pattern the resource path pattern with placeholders
   * @param libraryName the base library name
   * @param platformInfo the platform information for substitution
   * @return the resolved resource path
   * @throws IllegalArgumentException if any parameter is null or invalid
   * @throws SecurityException if the pattern or result contains security violations
   */
  static String resolvePath(
      final String pattern,
      final String libraryName,
      final PlatformDetector.PlatformInfo platformInfo) {
    Objects.requireNonNull(pattern, "pattern must not be null");
    Objects.requireNonNull(libraryName, "libraryName must not be null");
    Objects.requireNonNull(platformInfo, "platformInfo must not be null");

    if (pattern.trim().isEmpty()) {
      throw new IllegalArgumentException("pattern must not be empty");
    }

    if (libraryName.trim().isEmpty()) {
      throw new IllegalArgumentException("libraryName must not be empty");
    }

    // Validate the pattern for security
    validatePattern(pattern);

    // Perform placeholder substitution
    String resolvedPath = pattern;
    resolvedPath = resolvedPath.replace("{platform}", platformInfo.getPlatformId());
    resolvedPath = resolvedPath.replace("{os}", platformInfo.getOperatingSystem().getName());
    resolvedPath = resolvedPath.replace("{arch}", platformInfo.getArchitecture().getName());
    resolvedPath =
        resolvedPath.replace("{lib}", platformInfo.getOperatingSystem().getLibraryPrefix());
    resolvedPath = resolvedPath.replace("{name}", sanitizeLibraryName(libraryName));
    resolvedPath =
        resolvedPath.replace("{ext}", platformInfo.getOperatingSystem().getLibraryExtension());

    // Validate the resolved path
    validateResolvedPath(resolvedPath);

    return resolvedPath;
  }

  /**
   * Validates a resource path pattern for security and correctness.
   *
   * <p>This method checks for common security vulnerabilities and malicious patterns:
   *
   * <ul>
   *   <li>Path traversal attempts (../)
   *   <li>Absolute path references
   *   <li>Invalid placeholder syntax
   *   <li>Null bytes and control characters
   * </ul>
   *
   * @param pattern the pattern to validate
   * @throws IllegalArgumentException if the pattern is null or empty
   * @throws SecurityException if the pattern contains security violations
   */
  static void validatePattern(final String pattern) {
    Objects.requireNonNull(pattern, "pattern must not be null");

    if (pattern.trim().isEmpty()) {
      throw new IllegalArgumentException("pattern must not be empty");
    }

    // Check for path traversal attempts
    if (PATH_TRAVERSAL_PATTERN.matcher(pattern).find()) {
      throw new SecurityException("Pattern contains path traversal sequences: " + pattern);
    }

    // Check for absolute paths (except for leading slash which is normal for resources)
    final String patternWithoutLeadingSlash =
        pattern.startsWith("/") ? pattern.substring(1) : pattern;
    if (ABSOLUTE_PATH_PATTERN.matcher(patternWithoutLeadingSlash).find()) {
      throw new SecurityException("Pattern contains absolute path references: " + pattern);
    }

    // Check for null bytes and control characters
    if (pattern.contains("\0")) {
      throw new SecurityException("Pattern contains null bytes: " + pattern);
    }

    for (int i = 0; i < pattern.length(); i++) {
      final char c = pattern.charAt(i);
      if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
        throw new SecurityException(
            String.format("Pattern contains control character at position %d: %s", i, pattern));
      }
    }

    // Validate placeholder syntax
    if (!PLACEHOLDER_PATTERN.matcher(pattern).find() && pattern.contains("{")) {
      // Pattern contains { but no valid placeholders
      throw new IllegalArgumentException("Pattern contains malformed placeholders: " + pattern);
    }
  }

  /**
   * Validates a resolved resource path for security.
   *
   * @param resolvedPath the resolved path to validate
   * @throws SecurityException if the path contains security violations
   */
  private static void validateResolvedPath(final String resolvedPath) {
    // Check for remaining path traversal sequences after resolution
    if (PATH_TRAVERSAL_PATTERN.matcher(resolvedPath).find()) {
      throw new SecurityException(
          "Resolved path contains path traversal sequences: " + resolvedPath);
    }

    // Check for null bytes
    if (resolvedPath.contains("\0")) {
      throw new SecurityException("Resolved path contains null bytes: " + resolvedPath);
    }

    // Check for unresolved placeholders (indicates missing platform information)
    if (resolvedPath.contains("{") && resolvedPath.contains("}")) {
      throw new IllegalArgumentException(
          "Resolved path contains unresolved placeholders: " + resolvedPath);
    }
  }

  /**
   * Sanitizes a library name to prevent path traversal and injection attacks.
   *
   * @param libraryName the library name to sanitize
   * @return the sanitized library name
   */
  private static String sanitizeLibraryName(final String libraryName) {
    Objects.requireNonNull(libraryName, "libraryName must not be null");

    // Remove any path separators from library name
    String sanitized = libraryName.replace("/", "").replace("\\", "");

    // Remove any control characters
    final StringBuilder result = new StringBuilder(sanitized.length());
    for (int i = 0; i < sanitized.length(); i++) {
      final char c = sanitized.charAt(i);
      if (!Character.isISOControl(c) && c != '\0') {
        result.append(c);
      }
    }

    return result.toString();
  }
}
