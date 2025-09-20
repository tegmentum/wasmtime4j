package ai.tegmentum.wasmtime4j.wasi;

import java.time.Instant;

/**
 * File statistics and metadata for WASI filesystem entries.
 *
 * <p>WasiFileStats provides comprehensive metadata information about files and directories within
 * the WASI sandbox. This includes size, timestamps, permissions, and other filesystem attributes.
 *
 * <p>File statistics are obtained through {@link WasiFilesystem#getFileStats} operations and can
 * be modified using {@link WasiFilesystem#setFileStats} where permissions allow.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiFileStats stats = filesystem.getFileStats("/data/file.txt");
 * System.out.printf("File size: %d bytes\n", stats.getSize());
 * System.out.printf("Last modified: %s\n", stats.getModificationTime());
 * System.out.printf("Permissions: %s\n", stats.getPermissions());
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiFileStats {

  /**
   * Gets the device ID containing this file.
   *
   * @return the device ID
   */
  long getDevice();

  /**
   * Gets the inode number for this file.
   *
   * @return the inode number
   */
  long getInode();

  /**
   * Gets the file type.
   *
   * @return the file type
   */
  WasiFileType getFileType();

  /**
   * Gets the number of hard links to this file.
   *
   * @return the number of hard links
   */
  long getLinkCount();

  /**
   * Gets the size of this file in bytes.
   *
   * @return the file size in bytes
   */
  long getSize();

  /**
   * Gets the last access time.
   *
   * @return the last access time, or null if not available
   */
  Instant getAccessTime();

  /**
   * Gets the last modification time.
   *
   * @return the last modification time, or null if not available
   */
  Instant getModificationTime();

  /**
   * Gets the status change time.
   *
   * @return the status change time, or null if not available
   */
  Instant getStatusChangeTime();

  /**
   * Gets the creation time (birth time).
   *
   * @return the creation time, or null if not available
   */
  Instant getCreationTime();

  /**
   * Gets the file permissions.
   *
   * @return the file permissions
   */
  WasiPermissions getPermissions();

  /**
   * Checks if this represents a regular file.
   *
   * @return true if this is a regular file
   */
  default boolean isFile() {
    return getFileType().isRegularFile();
  }

  /**
   * Checks if this represents a directory.
   *
   * @return true if this is a directory
   */
  default boolean isDirectory() {
    return getFileType().isDirectory();
  }

  /**
   * Checks if this represents a symbolic link.
   *
   * @return true if this is a symbolic link
   */
  default boolean isSymbolicLink() {
    return getFileType().isSymbolicLink();
  }

  /**
   * Checks if this represents a special file.
   *
   * @return true if this is a special file (device, socket, etc.)
   */
  default boolean isSpecialFile() {
    return getFileType().isSpecialFile();
  }

  /**
   * Builder for creating WasiFileStats instances.
   */
  interface Builder {

    /**
     * Sets the device ID.
     *
     * @param device the device ID
     * @return this builder
     */
    Builder device(final long device);

    /**
     * Sets the inode number.
     *
     * @param inode the inode number
     * @return this builder
     */
    Builder inode(final long inode);

    /**
     * Sets the file type.
     *
     * @param fileType the file type
     * @return this builder
     * @throws IllegalArgumentException if fileType is null
     */
    Builder fileType(final WasiFileType fileType);

    /**
     * Sets the number of hard links.
     *
     * @param linkCount the number of hard links
     * @return this builder
     */
    Builder linkCount(final long linkCount);

    /**
     * Sets the file size.
     *
     * @param size the file size in bytes
     * @return this builder
     */
    Builder size(final long size);

    /**
     * Sets the last access time.
     *
     * @param accessTime the last access time
     * @return this builder
     */
    Builder accessTime(final Instant accessTime);

    /**
     * Sets the last modification time.
     *
     * @param modificationTime the last modification time
     * @return this builder
     */
    Builder modificationTime(final Instant modificationTime);

    /**
     * Sets the status change time.
     *
     * @param statusChangeTime the status change time
     * @return this builder
     */
    Builder statusChangeTime(final Instant statusChangeTime);

    /**
     * Sets the creation time.
     *
     * @param creationTime the creation time
     * @return this builder
     */
    Builder creationTime(final Instant creationTime);

    /**
     * Sets the file permissions.
     *
     * @param permissions the file permissions
     * @return this builder
     * @throws IllegalArgumentException if permissions is null
     */
    Builder permissions(final WasiPermissions permissions);

    /**
     * Builds the WasiFileStats instance.
     *
     * @return the built WasiFileStats
     * @throws IllegalStateException if required fields are not set
     */
    WasiFileStats build();
  }

  /**
   * Creates a new builder for WasiFileStats.
   *
   * @return a new builder instance
   */
  static Builder builder() {
    return new WasiFileStatsImpl.BuilderImpl();
  }
}