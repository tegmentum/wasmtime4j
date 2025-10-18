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

package ai.tegmentum.wasmtime4j;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a semantic version for WebAssembly components.
 *
 * <p>This class follows semantic versioning (SemVer) principles with support for:
 *
 * <ul>
 *   <li>Major.Minor.Patch version numbers
 *   <li>Pre-release identifiers (alpha, beta, rc)
 *   <li>Build metadata
 *   <li>Version comparison and compatibility checking
 * </ul>
 *
 * @since 1.0.0
 */
public final class ComponentVersion implements Comparable<ComponentVersion> {

  // Simplified semver pattern to prevent ReDoS attacks
  // Pre-release and build metadata are validated separately after initial parse
  private static final Pattern VERSION_PATTERN =
      Pattern.compile("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"
          + "(?:-([0-9A-Za-z-.]+))?"
          + "(?:\\+([0-9A-Za-z-.]+))?$");

  private static final int MAX_VERSION_LENGTH = 256;

  private final int major;
  private final int minor;
  private final int patch;
  private final String preRelease;
  private final String buildMetadata;
  private final String versionString;

  /**
   * Creates a new component version.
   *
   * @param major the major version number
   * @param minor the minor version number
   * @param patch the patch version number
   */
  public ComponentVersion(final int major, final int minor, final int patch) {
    this(major, minor, patch, null, null);
  }

  /**
   * Creates a new component version with pre-release identifier.
   *
   * @param major the major version number
   * @param minor the minor version number
   * @param patch the patch version number
   * @param preRelease the pre-release identifier (optional)
   */
  public ComponentVersion(
      final int major, final int minor, final int patch, final String preRelease) {
    this(major, minor, patch, preRelease, null);
  }

  /**
   * Creates a new component version with pre-release and build metadata.
   *
   * @param major the major version number
   * @param minor the minor version number
   * @param patch the patch version number
   * @param preRelease the pre-release identifier (optional)
   * @param buildMetadata the build metadata (optional)
   */
  public ComponentVersion(
      final int major,
      final int minor,
      final int patch,
      final String preRelease,
      final String buildMetadata) {
    if (major < 0 || minor < 0 || patch < 0) {
      throw new IllegalArgumentException("Version numbers must be non-negative");
    }

    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.preRelease = preRelease;
    this.buildMetadata = buildMetadata;
    this.versionString = buildVersionString();
  }

  /**
   * Parses a component version from a string representation.
   *
   * @param versionString the version string to parse
   * @return the parsed component version
   * @throws IllegalArgumentException if the version string is invalid
   */
  public static ComponentVersion parse(final String versionString) {
    Objects.requireNonNull(versionString, "Version string cannot be null");

    // Prevent ReDoS by rejecting excessively long version strings
    if (versionString.length() > MAX_VERSION_LENGTH) {
      throw new IllegalArgumentException(
          "Version string exceeds maximum length of " + MAX_VERSION_LENGTH);
    }

    if (!VERSION_PATTERN.matcher(versionString).matches()) {
      throw new IllegalArgumentException("Invalid version string: " + versionString);
    }

    final String[] parts = versionString.split("[\\-\\+]");
    final String[] versionParts = parts[0].split("\\.");

    final int major = Integer.parseInt(versionParts[0]);
    final int minor = Integer.parseInt(versionParts[1]);
    final int patch = Integer.parseInt(versionParts[2]);

    String preRelease = null;
    String buildMetadata = null;

    if (parts.length > 1) {
      if (versionString.contains("-")) {
        preRelease = versionString.substring(versionString.indexOf('-') + 1);
        if (preRelease.contains("+")) {
          final int plusIndex = preRelease.indexOf('+');
          buildMetadata = preRelease.substring(plusIndex + 1);
          preRelease = preRelease.substring(0, plusIndex);
        }
      } else if (versionString.contains("+")) {
        buildMetadata = versionString.substring(versionString.indexOf('+') + 1);
      }
    }

    return new ComponentVersion(major, minor, patch, preRelease, buildMetadata);
  }

  /**
   * Gets the major version number.
   *
   * @return the major version
   */
  public int getMajor() {
    return major;
  }

  /**
   * Gets the minor version number.
   *
   * @return the minor version
   */
  public int getMinor() {
    return minor;
  }

  /**
   * Gets the patch version number.
   *
   * @return the patch version
   */
  public int getPatch() {
    return patch;
  }

  /**
   * Gets the pre-release identifier.
   *
   * @return the pre-release identifier, or null if none
   */
  public String getPreRelease() {
    return preRelease;
  }

  /**
   * Gets the build metadata.
   *
   * @return the build metadata, or null if none
   */
  public String getBuildMetadata() {
    return buildMetadata;
  }

  /**
   * Checks if this is a pre-release version.
   *
   * @return true if this is a pre-release version
   */
  public boolean isPreRelease() {
    return preRelease != null && !preRelease.isEmpty();
  }

  /**
   * Checks if this version is compatible with another version.
   *
   * <p>Two versions are compatible if they have the same major version number and this version is
   * greater than or equal to the other version.
   *
   * @param other the other version to check compatibility with
   * @return true if the versions are compatible
   */
  public boolean isCompatibleWith(final ComponentVersion other) {
    Objects.requireNonNull(other, "Other version cannot be null");
    return this.major == other.major && this.compareTo(other) >= 0;
  }

  /**
   * Checks if this version is backward compatible with another version.
   *
   * @param other the other version to check backward compatibility with
   * @return true if this version is backward compatible
   */
  public boolean isBackwardCompatibleWith(final ComponentVersion other) {
    Objects.requireNonNull(other, "Other version cannot be null");

    if (this.major != other.major) {
      return false;
    }

    if (this.minor > other.minor) {
      return true;
    }

    return this.minor == other.minor && this.patch >= other.patch;
  }

  /**
   * Creates the next major version.
   *
   * @return a new version with incremented major version
   */
  public ComponentVersion nextMajor() {
    return new ComponentVersion(major + 1, 0, 0);
  }

  /**
   * Creates the next minor version.
   *
   * @return a new version with incremented minor version
   */
  public ComponentVersion nextMinor() {
    return new ComponentVersion(major, minor + 1, 0);
  }

  /**
   * Creates the next patch version.
   *
   * @return a new version with incremented patch version
   */
  public ComponentVersion nextPatch() {
    return new ComponentVersion(major, minor, patch + 1);
  }

  private String buildVersionString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(major).append('.').append(minor).append('.').append(patch);

    if (preRelease != null && !preRelease.isEmpty()) {
      sb.append('-').append(preRelease);
    }

    if (buildMetadata != null && !buildMetadata.isEmpty()) {
      sb.append('+').append(buildMetadata);
    }

    return sb.toString();
  }

  @Override
  public int compareTo(final ComponentVersion other) {
    Objects.requireNonNull(other, "Other version cannot be null");

    // Compare major, minor, patch
    int result = Integer.compare(this.major, other.major);
    if (result != 0) {
      return result;
    }

    result = Integer.compare(this.minor, other.minor);
    if (result != 0) {
      return result;
    }

    result = Integer.compare(this.patch, other.patch);
    if (result != 0) {
      return result;
    }

    // Handle pre-release versions
    if (this.preRelease == null && other.preRelease == null) {
      return 0;
    }
    if (this.preRelease == null) {
      return 1; // Release version is greater than pre-release
    }
    if (other.preRelease == null) {
      return -1; // Pre-release is less than release version
    }

    // Compare pre-release strings lexically
    return this.preRelease.compareTo(other.preRelease);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ComponentVersion that = (ComponentVersion) obj;
    return major == that.major
        && minor == that.minor
        && patch == that.patch
        && Objects.equals(preRelease, that.preRelease)
        && Objects.equals(buildMetadata, that.buildMetadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch, preRelease, buildMetadata);
  }

  @Override
  public String toString() {
    return versionString;
  }
}
