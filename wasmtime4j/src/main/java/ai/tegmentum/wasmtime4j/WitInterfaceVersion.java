package ai.tegmentum.wasmtime4j;

/**
 * Represents a version for WIT interface definitions.
 *
 * <p>WIT interface versions follow semantic versioning principles and are used for compatibility
 * checking and interface evolution.
 *
 * @since 1.0.0
 */
public final class WitInterfaceVersion implements Comparable<WitInterfaceVersion> {

  private final int major;
  private final int minor;
  private final int patch;
  private final String preRelease;
  private final String build;

  /**
   * Creates a new WIT interface version.
   *
   * @param major the major version number
   * @param minor the minor version number
   * @param patch the patch version number
   */
  public WitInterfaceVersion(final int major, final int minor, final int patch) {
    this(major, minor, patch, null, null);
  }

  /**
   * Creates a new WIT interface version with pre-release and build metadata.
   *
   * @param major the major version number
   * @param minor the minor version number
   * @param patch the patch version number
   * @param preRelease the pre-release identifier (may be null)
   * @param build the build metadata (may be null)
   */
  public WitInterfaceVersion(
      final int major,
      final int minor,
      final int patch,
      final String preRelease,
      final String build) {
    if (major < 0 || minor < 0 || patch < 0) {
      throw new IllegalArgumentException("Version numbers cannot be negative");
    }
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.preRelease = preRelease;
    this.build = build;
  }

  /**
   * Parses a version string in semantic version format.
   *
   * @param versionString the version string to parse
   * @return the parsed version
   * @throws IllegalArgumentException if the version string is invalid
   */
  public static WitInterfaceVersion parse(final String versionString) {
    if (versionString == null || versionString.trim().isEmpty()) {
      throw new IllegalArgumentException("Version string cannot be null or empty");
    }

    final String[] parts = versionString.split("\\.");
    if (parts.length < 3) {
      throw new IllegalArgumentException("Invalid version format: " + versionString);
    }

    try {
      final int major = Integer.parseInt(parts[0]);
      final int minor = Integer.parseInt(parts[1]);

      // Handle patch version that might have pre-release or build info
      String patchPart = parts[2];
      String preRelease = null;
      String build = null;

      // Extract pre-release (after -)
      if (patchPart.contains("-")) {
        final String[] patchSplit = patchPart.split("-", 2);
        patchPart = patchSplit[0];
        preRelease = patchSplit[1];

        // Extract build metadata (after +)
        if (preRelease.contains("+")) {
          final String[] preSplit = preRelease.split("\\+", 2);
          preRelease = preSplit[0];
          build = preSplit[1];
        }
      } else if (patchPart.contains("+")) {
        // Extract build metadata directly
        final String[] patchSplit = patchPart.split("\\+", 2);
        patchPart = patchSplit[0];
        build = patchSplit[1];
      }

      final int patch = Integer.parseInt(patchPart);

      return new WitInterfaceVersion(major, minor, patch, preRelease, build);
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException("Invalid version format: " + versionString, e);
    }
  }

  public int getMajor() {
    return major;
  }

  public int getMinor() {
    return minor;
  }

  public int getPatch() {
    return patch;
  }

  public String getPreRelease() {
    return preRelease;
  }

  public String getBuild() {
    return build;
  }

  /**
   * Checks if this version is compatible with another version.
   *
   * @param other the other version to check compatibility with
   * @return true if versions are compatible
   */
  public boolean isCompatibleWith(final WitInterfaceVersion other) {
    if (other == null) {
      return false;
    }

    // Major version must match
    if (this.major != other.major) {
      return false;
    }

    // Minor version compatibility (backward compatible)
    return this.minor >= other.minor;
  }

  @Override
  public int compareTo(final WitInterfaceVersion other) {
    if (other == null) {
      return 1;
    }

    int result = Integer.compare(this.major, other.major);
    if (result != 0) return result;

    result = Integer.compare(this.minor, other.minor);
    if (result != 0) return result;

    result = Integer.compare(this.patch, other.patch);
    if (result != 0) return result;

    // Pre-release versions have lower precedence
    if (this.preRelease != null && other.preRelease == null) return -1;
    if (this.preRelease == null && other.preRelease != null) return 1;
    if (this.preRelease != null && other.preRelease != null) {
      return this.preRelease.compareTo(other.preRelease);
    }

    return 0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    final WitInterfaceVersion that = (WitInterfaceVersion) obj;
    return major == that.major
        && minor == that.minor
        && patch == that.patch
        && java.util.Objects.equals(preRelease, that.preRelease)
        && java.util.Objects.equals(build, that.build);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(major, minor, patch, preRelease, build);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(major).append('.').append(minor).append('.').append(patch);

    if (preRelease != null) {
      sb.append('-').append(preRelease);
    }

    if (build != null) {
      sb.append('+').append(build);
    }

    return sb.toString();
  }
}
