package ai.tegmentum.wasmtime4j.wasi;

/**
 * File and directory permissions for WASI filesystem operations.
 *
 * <p>This class represents Unix-style file permissions using a standard rwx (read, write,
 * execute) model for owner, group, and others. The permissions are stored as a standard Unix
 * permission mode integer.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create permissions for owner read/write, group/others read-only
 * WasiPermissions perms = WasiPermissions.builder()
 *     .ownerRead(true)
 *     .ownerWrite(true)
 *     .groupRead(true)
 *     .othersRead(true)
 *     .build();
 *
 * // Or use predefined common permissions
 * WasiPermissions readWrite = WasiPermissions.OWNER_READ_WRITE;
 * WasiPermissions publicRead = WasiPermissions.ALL_READ;
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasiPermissions {

  // Permission bit constants
  private static final int OWNER_READ = 0400;
  private static final int OWNER_WRITE = 0200;
  private static final int OWNER_EXECUTE = 0100;
  private static final int GROUP_READ = 0040;
  private static final int GROUP_WRITE = 0020;
  private static final int GROUP_EXECUTE = 0010;
  private static final int OTHERS_READ = 0004;
  private static final int OTHERS_WRITE = 0002;
  private static final int OTHERS_EXECUTE = 0001;

  // Common permission combinations
  /** No permissions (000). */
  public static final WasiPermissions NONE = new WasiPermissions(0);

  /** Owner read/write only (600). */
  public static final WasiPermissions OWNER_READ_WRITE =
      new WasiPermissions(OWNER_READ | OWNER_WRITE);

  /** Owner read/write/execute only (700). */
  public static final WasiPermissions OWNER_ALL =
      new WasiPermissions(OWNER_READ | OWNER_WRITE | OWNER_EXECUTE);

  /** Owner read/write, group/others read (644). */
  public static final WasiPermissions ALL_READ =
      new WasiPermissions(OWNER_READ | OWNER_WRITE | GROUP_READ | OTHERS_READ);

  /** All read/write (666). */
  public static final WasiPermissions ALL_READ_WRITE =
      new WasiPermissions(
          OWNER_READ | OWNER_WRITE | GROUP_READ | GROUP_WRITE | OTHERS_READ | OTHERS_WRITE);

  /** Standard executable file permissions (755). */
  public static final WasiPermissions EXECUTABLE =
      new WasiPermissions(
          OWNER_READ
              | OWNER_WRITE
              | OWNER_EXECUTE
              | GROUP_READ
              | GROUP_EXECUTE
              | OTHERS_READ
              | OTHERS_EXECUTE);

  /** Full permissions for all (777). */
  public static final WasiPermissions ALL =
      new WasiPermissions(
          OWNER_READ
              | OWNER_WRITE
              | OWNER_EXECUTE
              | GROUP_READ
              | GROUP_WRITE
              | GROUP_EXECUTE
              | OTHERS_READ
              | OTHERS_WRITE
              | OTHERS_EXECUTE);

  private final int mode;

  /**
   * Creates permissions with the specified mode.
   *
   * @param mode the Unix-style permission mode
   */
  public WasiPermissions(final int mode) {
    this.mode = mode & 0777; // Keep only permission bits
  }

  /**
   * Gets the Unix-style permission mode.
   *
   * @return the permission mode
   */
  public int getMode() {
    return mode;
  }

  /**
   * Gets the octal string representation of the permissions.
   *
   * @return the permissions as a 3-digit octal string
   */
  public String getOctal() {
    return String.format("%03o", mode);
  }

  /**
   * Gets the symbolic string representation of the permissions (e.g., "rwxr-xr-x").
   *
   * @return the permissions as a symbolic string
   */
  public String getSymbolic() {
    final StringBuilder sb = new StringBuilder(9);

    // Owner permissions
    sb.append(hasOwnerRead() ? 'r' : '-');
    sb.append(hasOwnerWrite() ? 'w' : '-');
    sb.append(hasOwnerExecute() ? 'x' : '-');

    // Group permissions
    sb.append(hasGroupRead() ? 'r' : '-');
    sb.append(hasGroupWrite() ? 'w' : '-');
    sb.append(hasGroupExecute() ? 'x' : '-');

    // Others permissions
    sb.append(hasOthersRead() ? 'r' : '-');
    sb.append(hasOthersWrite() ? 'w' : '-');
    sb.append(hasOthersExecute() ? 'x' : '-');

    return sb.toString();
  }

  // Owner permission checks
  public boolean hasOwnerRead() {
    return (mode & OWNER_READ) != 0;
  }

  public boolean hasOwnerWrite() {
    return (mode & OWNER_WRITE) != 0;
  }

  public boolean hasOwnerExecute() {
    return (mode & OWNER_EXECUTE) != 0;
  }

  // Group permission checks
  public boolean hasGroupRead() {
    return (mode & GROUP_READ) != 0;
  }

  public boolean hasGroupWrite() {
    return (mode & GROUP_WRITE) != 0;
  }

  public boolean hasGroupExecute() {
    return (mode & GROUP_EXECUTE) != 0;
  }

  // Others permission checks
  public boolean hasOthersRead() {
    return (mode & OTHERS_READ) != 0;
  }

  public boolean hasOthersWrite() {
    return (mode & OTHERS_WRITE) != 0;
  }

  public boolean hasOthersExecute() {
    return (mode & OTHERS_EXECUTE) != 0;
  }

  /**
   * Creates a new permissions instance with additional permissions.
   *
   * @param additionalMode the additional permission bits to add
   * @return a new WasiPermissions instance with combined permissions
   */
  public WasiPermissions add(final int additionalMode) {
    return new WasiPermissions(mode | additionalMode);
  }

  /**
   * Creates a new permissions instance with specified permissions removed.
   *
   * @param removeMode the permission bits to remove
   * @return a new WasiPermissions instance with specified permissions removed
   */
  public WasiPermissions remove(final int removeMode) {
    return new WasiPermissions(mode & ~removeMode);
  }

  /**
   * Creates a builder for constructing WasiPermissions.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates permissions from an octal string (e.g., "644", "755").
   *
   * @param octal the octal string representation
   * @return the corresponding WasiPermissions
   * @throws IllegalArgumentException if the octal string is invalid
   */
  public static WasiPermissions fromOctal(final String octal) {
    try {
      final int mode = Integer.parseInt(octal, 8);
      return new WasiPermissions(mode);
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException("Invalid octal permissions: " + octal, e);
    }
  }

  /**
   * Creates permissions from a Unix-style mode integer.
   *
   * @param mode the Unix-style permission mode
   * @return the corresponding WasiPermissions
   */
  public static WasiPermissions fromMode(final int mode) {
    return new WasiPermissions(mode);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final WasiPermissions other = (WasiPermissions) obj;
    return mode == other.mode;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(mode);
  }

  @Override
  public String toString() {
    return String.format("WasiPermissions{mode=%s (%s)}", getOctal(), getSymbolic());
  }

  /**
   * Builder for creating WasiPermissions instances.
   */
  public static final class Builder {
    private int mode = 0;

    private Builder() {}

    public Builder ownerRead(final boolean enabled) {
      if (enabled) {
        mode |= OWNER_READ;
      } else {
        mode &= ~OWNER_READ;
      }
      return this;
    }

    public Builder ownerWrite(final boolean enabled) {
      if (enabled) {
        mode |= OWNER_WRITE;
      } else {
        mode &= ~OWNER_WRITE;
      }
      return this;
    }

    public Builder ownerExecute(final boolean enabled) {
      if (enabled) {
        mode |= OWNER_EXECUTE;
      } else {
        mode &= ~OWNER_EXECUTE;
      }
      return this;
    }

    public Builder groupRead(final boolean enabled) {
      if (enabled) {
        mode |= GROUP_READ;
      } else {
        mode &= ~GROUP_READ;
      }
      return this;
    }

    public Builder groupWrite(final boolean enabled) {
      if (enabled) {
        mode |= GROUP_WRITE;
      } else {
        mode &= ~GROUP_WRITE;
      }
      return this;
    }

    public Builder groupExecute(final boolean enabled) {
      if (enabled) {
        mode |= GROUP_EXECUTE;
      } else {
        mode &= ~GROUP_EXECUTE;
      }
      return this;
    }

    public Builder othersRead(final boolean enabled) {
      if (enabled) {
        mode |= OTHERS_READ;
      } else {
        mode &= ~OTHERS_READ;
      }
      return this;
    }

    public Builder othersWrite(final boolean enabled) {
      if (enabled) {
        mode |= OTHERS_WRITE;
      } else {
        mode &= ~OTHERS_WRITE;
      }
      return this;
    }

    public Builder othersExecute(final boolean enabled) {
      if (enabled) {
        mode |= OTHERS_EXECUTE;
      } else {
        mode &= ~OTHERS_EXECUTE;
      }
      return this;
    }

    public Builder mode(final int mode) {
      this.mode = mode & 0777;
      return this;
    }

    public WasiPermissions build() {
      return new WasiPermissions(mode);
    }
  }
}