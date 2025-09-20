package ai.tegmentum.wasmtime4j.serialization;

/**
 * Target platforms supported for module serialization and AOT compilation.
 *
 * <p>TargetPlatform represents the combination of operating system and CPU architecture that a
 * serialized module is optimized for. Modules serialized for one platform may not be compatible
 * with others due to architecture-specific optimizations and calling conventions.
 *
 * @since 1.0.0
 */
public enum TargetPlatform {

  /** Linux operating system on x86_64 architecture. */
  LINUX_X86_64("linux", "x86_64", "linux-x86_64"),

  /** Linux operating system on AArch64 (ARM64) architecture. */
  LINUX_AARCH64("linux", "aarch64", "linux-aarch64"),

  /** Windows operating system on x86_64 architecture. */
  WINDOWS_X86_64("windows", "x86_64", "windows-x86_64"),

  /** Windows operating system on AArch64 (ARM64) architecture. */
  WINDOWS_AARCH64("windows", "aarch64", "windows-aarch64"),

  /** macOS operating system on x86_64 architecture. */
  MACOS_X86_64("macos", "x86_64", "macos-x86_64"),

  /** macOS operating system on AArch64 (ARM64) architecture. */
  MACOS_AARCH64("macos", "aarch64", "macos-aarch64");

  private final String os;
  private final String arch;
  private final String identifier;

  TargetPlatform(final String os, final String arch, final String identifier) {
    this.os = os;
    this.arch = arch;
    this.identifier = identifier;
  }

  /**
   * Gets the operating system name for this platform.
   *
   * @return the operating system name
   */
  public String getOs() {
    return os;
  }

  /**
   * Gets the CPU architecture for this platform.
   *
   * @return the CPU architecture
   */
  public String getArch() {
    return arch;
  }

  /**
   * Gets the unique identifier for this platform.
   *
   * @return the platform identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Checks if this platform is compatible with another platform.
   *
   * <p>Platforms are considered compatible if they have the same operating system and CPU
   * architecture. This method is used to validate that serialized modules can be loaded on the
   * current platform.
   *
   * @param other the platform to check compatibility with
   * @return true if the platforms are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  public boolean isCompatibleWith(final TargetPlatform other) {
    if (other == null) {
      throw new IllegalArgumentException("Other platform cannot be null");
    }
    return this.os.equals(other.os) && this.arch.equals(other.arch);
  }

  /**
   * Checks if this platform uses the same CPU architecture as another platform.
   *
   * @param other the platform to check architecture compatibility with
   * @return true if the platforms use the same CPU architecture, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  public boolean hasSameArchitecture(final TargetPlatform other) {
    if (other == null) {
      throw new IllegalArgumentException("Other platform cannot be null");
    }
    return this.arch.equals(other.arch);
  }

  /**
   * Checks if this platform uses the same operating system as another platform.
   *
   * @param other the platform to check OS compatibility with
   * @return true if the platforms use the same operating system, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  public boolean hasSameOs(final TargetPlatform other) {
    if (other == null) {
      throw new IllegalArgumentException("Other platform cannot be null");
    }
    return this.os.equals(other.os);
  }

  /**
   * Gets the current platform based on system properties.
   *
   * <p>This method detects the current operating system and CPU architecture to determine the
   * appropriate TargetPlatform. If the current platform is not supported, an exception is thrown.
   *
   * @return the current platform
   * @throws UnsupportedOperationException if the current platform is not supported
   */
  public static TargetPlatform current() {
    final String osName = System.getProperty("os.name").toLowerCase();
    final String archName = System.getProperty("os.arch").toLowerCase();

    final String normalizedOs;
    if (osName.contains("windows")) {
      normalizedOs = "windows";
    } else if (osName.contains("linux")) {
      normalizedOs = "linux";
    } else if (osName.contains("mac") || osName.contains("darwin")) {
      normalizedOs = "macos";
    } else {
      throw new UnsupportedOperationException("Unsupported operating system: " + osName);
    }

    final String normalizedArch;
    if (archName.equals("x86_64") || archName.equals("amd64")) {
      normalizedArch = "x86_64";
    } else if (archName.equals("aarch64") || archName.equals("arm64")) {
      normalizedArch = "aarch64";
    } else {
      throw new UnsupportedOperationException("Unsupported architecture: " + archName);
    }

    for (final TargetPlatform platform : values()) {
      if (platform.os.equals(normalizedOs) && platform.arch.equals(normalizedArch)) {
        return platform;
      }
    }

    throw new UnsupportedOperationException(
        "Unsupported platform: " + normalizedOs + "-" + normalizedArch);
  }

  /**
   * Gets a TargetPlatform by its identifier string.
   *
   * @param identifier the platform identifier
   * @return the corresponding TargetPlatform
   * @throws IllegalArgumentException if the identifier is not recognized
   */
  public static TargetPlatform fromIdentifier(final String identifier) {
    if (identifier == null) {
      throw new IllegalArgumentException("Platform identifier cannot be null");
    }

    for (final TargetPlatform platform : values()) {
      if (platform.identifier.equals(identifier)) {
        return platform;
      }
    }

    throw new IllegalArgumentException("Unknown platform identifier: " + identifier);
  }

  /**
   * Gets a TargetPlatform by operating system and architecture.
   *
   * @param os the operating system name
   * @param arch the CPU architecture name
   * @return the corresponding TargetPlatform
   * @throws IllegalArgumentException if the combination is not recognized or if os or arch is null
   */
  public static TargetPlatform fromOsAndArch(final String os, final String arch) {
    if (os == null) {
      throw new IllegalArgumentException("Operating system cannot be null");
    }
    if (arch == null) {
      throw new IllegalArgumentException("Architecture cannot be null");
    }

    for (final TargetPlatform platform : values()) {
      if (platform.os.equals(os) && platform.arch.equals(arch)) {
        return platform;
      }
    }

    throw new IllegalArgumentException("Unknown platform: " + os + "-" + arch);
  }

  @Override
  public String toString() {
    return identifier;
  }
}
