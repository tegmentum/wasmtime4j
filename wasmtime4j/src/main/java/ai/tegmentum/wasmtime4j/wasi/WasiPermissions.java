package ai.tegmentum.wasmtime4j.wasi;

/**
 * File and directory permissions in the WASI filesystem.
 *
 * <p>WasiPermissions represents the standard POSIX-style permission bits for files and directories
 * within the WASI sandbox. These permissions control read, write, and execute access for the owner,
 * group, and other users.
 *
 * <p>Permissions are used when creating files and directories within the WASI sandbox.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create permissions for a readable/writable file by owner only
 * WasiPermissions filePerms = WasiPermissions.builder()
 *     .ownerRead(true)
 *     .ownerWrite(true)
 *     .build();
 *
 * // Create permissions for a directory accessible by all
 * WasiPermissions dirPerms = WasiPermissions.of(0755);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiPermissions {

  /**
   * Gets the complete permission bits as an octal value.
   *
   * @return the permission bits (e.g., 0644, 0755)
   */
  int getMode();

  /**
   * Checks if the owner has read permission.
   *
   * @return true if owner can read
   */
  boolean isOwnerRead();

  /**
   * Checks if the owner has write permission.
   *
   * @return true if owner can write
   */
  boolean isOwnerWrite();

  /**
   * Checks if the owner has execute permission.
   *
   * @return true if owner can execute
   */
  boolean isOwnerExecute();

  /**
   * Checks if the group has read permission.
   *
   * @return true if group can read
   */
  boolean isGroupRead();

  /**
   * Checks if the group has write permission.
   *
   * @return true if group can write
   */
  boolean isGroupWrite();

  /**
   * Checks if the group has execute permission.
   *
   * @return true if group can execute
   */
  boolean isGroupExecute();

  /**
   * Checks if others have read permission.
   *
   * @return true if others can read
   */
  boolean isOtherRead();

  /**
   * Checks if others have write permission.
   *
   * @return true if others can write
   */
  boolean isOtherWrite();

  /**
   * Checks if others have execute permission.
   *
   * @return true if others can execute
   */
  boolean isOtherExecute();

  /**
   * Checks if the setuid bit is set.
   *
   * @return true if setuid is set
   */
  boolean isSetuid();

  /**
   * Checks if the setgid bit is set.
   *
   * @return true if setgid is set
   */
  boolean isSetgid();

  /**
   * Checks if the sticky bit is set.
   *
   * @return true if sticky bit is set
   */
  boolean isSticky();

  /**
   * Creates WasiPermissions from an octal mode value.
   *
   * @param mode the octal permission mode (e.g., 0644, 0755)
   * @return a new WasiPermissions instance
   * @throws IllegalArgumentException if mode is invalid
   */
  static WasiPermissions of(final int mode) {
    return new WasiPermissionsImpl(mode);
  }

  /**
   * Creates a new builder for WasiPermissions.
   *
   * @return a new builder instance
   */
  static Builder builder() {
    return new WasiPermissionsImpl.BuilderImpl();
  }

  /**
   * Gets the default file permissions (0644 - readable by all, writable by owner).
   *
   * @return default file permissions
   */
  static WasiPermissions defaultFilePermissions() {
    return of(0644);
  }

  /**
   * Gets the default directory permissions (0755 - readable/executable by all, writable by owner).
   *
   * @return default directory permissions
   */
  static WasiPermissions defaultDirectoryPermissions() {
    return of(0755);
  }

  /** Builder for creating WasiPermissions instances. */
  interface Builder {

    /**
     * Sets owner read permission.
     *
     * @param read true to allow owner read access
     * @return this builder
     */
    Builder ownerRead(final boolean read);

    /**
     * Sets owner write permission.
     *
     * @param write true to allow owner write access
     * @return this builder
     */
    Builder ownerWrite(final boolean write);

    /**
     * Sets owner execute permission.
     *
     * @param execute true to allow owner execute access
     * @return this builder
     */
    Builder ownerExecute(final boolean execute);

    /**
     * Sets group read permission.
     *
     * @param read true to allow group read access
     * @return this builder
     */
    Builder groupRead(final boolean read);

    /**
     * Sets group write permission.
     *
     * @param write true to allow group write access
     * @return this builder
     */
    Builder groupWrite(final boolean write);

    /**
     * Sets group execute permission.
     *
     * @param execute true to allow group execute access
     * @return this builder
     */
    Builder groupExecute(final boolean execute);

    /**
     * Sets other read permission.
     *
     * @param read true to allow other read access
     * @return this builder
     */
    Builder otherRead(final boolean read);

    /**
     * Sets other write permission.
     *
     * @param write true to allow other write access
     * @return this builder
     */
    Builder otherWrite(final boolean write);

    /**
     * Sets other execute permission.
     *
     * @param execute true to allow other execute access
     * @return this builder
     */
    Builder otherExecute(final boolean execute);

    /**
     * Sets the setuid bit.
     *
     * @param setuid true to set the setuid bit
     * @return this builder
     */
    Builder setuid(final boolean setuid);

    /**
     * Sets the setgid bit.
     *
     * @param setgid true to set the setgid bit
     * @return this builder
     */
    Builder setgid(final boolean setgid);

    /**
     * Sets the sticky bit.
     *
     * @param sticky true to set the sticky bit
     * @return this builder
     */
    Builder sticky(final boolean sticky);

    /**
     * Sets all permissions from an octal mode value.
     *
     * @param mode the octal permission mode
     * @return this builder
     */
    Builder mode(final int mode);

    /**
     * Builds the WasiPermissions instance.
     *
     * @return the built WasiPermissions
     */
    WasiPermissions build();
  }
}
