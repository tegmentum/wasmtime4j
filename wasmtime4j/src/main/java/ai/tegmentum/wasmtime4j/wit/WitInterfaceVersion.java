package ai.tegmentum.wasmtime4j.wit;

/**
 * WIT interface version class for WebAssembly interface types.
 *
 * @since 1.0.0
 */
public class WitInterfaceVersion implements Comparable<WitInterfaceVersion> {

  private final int major;
  private final int minor;
  private final int patch;
  private final String preRelease;

  /**
   * Creates a new WIT interface version.
   *
   * @param major major version
   * @param minor minor version
   * @param patch patch version
   */
  public WitInterfaceVersion(int major, int minor, int patch) {
    this(major, minor, patch, null);
  }

  /**
   * Creates a new WIT interface version with pre-release.
   *
   * @param major major version
   * @param minor minor version
   * @param patch patch version
   * @param preRelease pre-release identifier
   */
  public WitInterfaceVersion(int major, int minor, int patch, String preRelease) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.preRelease = preRelease;
  }

  /**
   * Gets the major version.
   *
   * @return major version
   */
  public int getMajor() {
    return major;
  }

  /**
   * Gets the minor version.
   *
   * @return minor version
   */
  public int getMinor() {
    return minor;
  }

  /**
   * Gets the patch version.
   *
   * @return patch version
   */
  public int getPatch() {
    return patch;
  }

  /**
   * Gets the pre-release identifier.
   *
   * @return pre-release identifier or null
   */
  public String getPreRelease() {
    return preRelease;
  }

  /**
   * Gets the version string.
   *
   * @return version string
   */
  public String getVersion() {
    return toString();
  }

  /**
   * Gets the interface name (stub implementation).
   *
   * @return interface name
   */
  public String getInterfaceName() {
    return "unknown";
  }

  /**
   * Gets the WIT interface definition (stub implementation).
   *
   * @return interface definition
   */
  public WitInterfaceDefinition getInterface() {
    return null; // Stub - would return actual interface
  }

  /**
   * Checks if this version is compatible with another version.
   *
   * @param other other version
   * @return true if compatible
   */
  public boolean isCompatibleWith(WitInterfaceVersion other) {
    return this.major == other.major;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(major).append('.').append(minor).append('.').append(patch);
    if (preRelease != null) {
      sb.append('-').append(preRelease);
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    WitInterfaceVersion that = (WitInterfaceVersion) obj;
    return major == that.major
        && minor == that.minor
        && patch == that.patch
        && java.util.Objects.equals(preRelease, that.preRelease);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(major, minor, patch, preRelease);
  }

  @Override
  public int compareTo(WitInterfaceVersion other) {
    if (this.major != other.major) {
      return Integer.compare(this.major, other.major);
    }
    if (this.minor != other.minor) {
      return Integer.compare(this.minor, other.minor);
    }
    if (this.patch != other.patch) {
      return Integer.compare(this.patch, other.patch);
    }
    if (this.preRelease == null && other.preRelease == null) {
      return 0;
    }
    if (this.preRelease == null) {
      return 1; // Release version is greater than pre-release
    }
    if (other.preRelease == null) {
      return -1; // Pre-release version is less than release
    }
    return this.preRelease.compareTo(other.preRelease);
  }
}
