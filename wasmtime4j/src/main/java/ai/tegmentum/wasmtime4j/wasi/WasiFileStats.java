package ai.tegmentum.wasmtime4j.wasi;

import java.time.Instant;

/**
 * File statistics and metadata information corresponding to WASI Preview 1 filestat.
 *
 * <p>This class contains comprehensive metadata about a file or directory, including size,
 * timestamps, permissions, and type information. It is returned by file metadata operations
 * and can be used to set file metadata.
 *
 * @since 1.0.0
 */
public final class WasiFileStats {

  private final long device;
  private final long inode;
  private final WasiFileType type;
  private final long linkCount;
  private final long size;
  private final Instant accessTime;
  private final Instant modifyTime;
  private final Instant changeTime;

  /**
   * Creates a new WasiFileStats instance.
   *
   * @param device the device ID of the device containing the file
   * @param inode the inode number of the file
   * @param type the type of the file
   * @param linkCount the number of hard links to the file
   * @param size the size of the file in bytes
   * @param accessTime the time of last access
   * @param modifyTime the time of last modification
   * @param changeTime the time of last status change
   */
  public WasiFileStats(
      final long device,
      final long inode,
      final WasiFileType type,
      final long linkCount,
      final long size,
      final Instant accessTime,
      final Instant modifyTime,
      final Instant changeTime) {
    this.device = device;
    this.inode = inode;
    this.type = type;
    this.linkCount = linkCount;
    this.size = size;
    this.accessTime = accessTime;
    this.modifyTime = modifyTime;
    this.changeTime = changeTime;
  }

  /**
   * Gets the device ID of the device containing the file.
   *
   * <p>This identifies the device (filesystem) where the file is stored. Files on the same
   * device will have the same device ID.
   *
   * @return the device ID
   */
  public long getDevice() {
    return device;
  }

  /**
   * Gets the inode number of the file.
   *
   * <p>The inode number uniquely identifies the file within its device/filesystem. Files with
   * the same device ID and inode number are the same file (hard links).
   *
   * @return the inode number
   */
  public long getInode() {
    return inode;
  }

  /**
   * Gets the type of the file.
   *
   * <p>This indicates whether the file is a regular file, directory, symbolic link, or other
   * special file type.
   *
   * @return the file type
   */
  public WasiFileType getType() {
    return type;
  }

  /**
   * Gets the number of hard links to the file.
   *
   * <p>This is the number of directory entries that point to this file. Regular files typically
   * have a link count of 1, while directories have at least 2 (the entry itself and the "."
   * entry).
   *
   * @return the hard link count
   */
  public long getLinkCount() {
    return linkCount;
  }

  /**
   * Gets the size of the file in bytes.
   *
   * <p>For regular files, this is the number of bytes in the file. For directories, the meaning
   * is filesystem-dependent. For other file types, the size may be 0 or have special meaning.
   *
   * @return the file size in bytes
   */
  public long getSize() {
    return size;
  }

  /**
   * Gets the time of last access.
   *
   * <p>This is the timestamp when the file was last accessed (read). Some filesystems may not
   * track access times or may update them infrequently for performance reasons.
   *
   * @return the last access time
   */
  public Instant getAccessTime() {
    return accessTime;
  }

  /**
   * Gets the time of last modification.
   *
   * <p>This is the timestamp when the file's content was last modified (written to). This is
   * typically the most useful timestamp for determining file freshness.
   *
   * @return the last modification time
   */
  public Instant getModifyTime() {
    return modifyTime;
  }

  /**
   * Gets the time of last status change.
   *
   * <p>This is the timestamp when the file's metadata (permissions, owner, link count, etc.)
   * was last changed. On many systems, this is also updated when the file content is modified.
   *
   * @return the last status change time
   */
  public Instant getChangeTime() {
    return changeTime;
  }

  /**
   * Checks if this represents a regular file.
   *
   * @return true if this is a regular file, false otherwise
   */
  public boolean isRegularFile() {
    return type.isRegularFile();
  }

  /**
   * Checks if this represents a directory.
   *
   * @return true if this is a directory, false otherwise
   */
  public boolean isDirectory() {
    return type.isDirectory();
  }

  /**
   * Checks if this represents a symbolic link.
   *
   * @return true if this is a symbolic link, false otherwise
   */
  public boolean isSymbolicLink() {
    return type.isSymbolicLink();
  }

  /**
   * Creates a builder for constructing WasiFileStats instances.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a builder initialized with the values from this instance.
   *
   * @return a new builder instance with values copied from this instance
   */
  public Builder toBuilder() {
    return new Builder()
        .device(device)
        .inode(inode)
        .type(type)
        .linkCount(linkCount)
        .size(size)
        .accessTime(accessTime)
        .modifyTime(modifyTime)
        .changeTime(changeTime);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final WasiFileStats other = (WasiFileStats) obj;
    return device == other.device
        && inode == other.inode
        && linkCount == other.linkCount
        && size == other.size
        && type == other.type
        && accessTime.equals(other.accessTime)
        && modifyTime.equals(other.modifyTime)
        && changeTime.equals(other.changeTime);
  }

  @Override
  public int hashCode() {
    int result = Long.hashCode(device);
    result = 31 * result + Long.hashCode(inode);
    result = 31 * result + type.hashCode();
    result = 31 * result + Long.hashCode(linkCount);
    result = 31 * result + Long.hashCode(size);
    result = 31 * result + accessTime.hashCode();
    result = 31 * result + modifyTime.hashCode();
    result = 31 * result + changeTime.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return String.format(
        "WasiFileStats{device=%d, inode=%d, type=%s, linkCount=%d, size=%d, "
            + "accessTime=%s, modifyTime=%s, changeTime=%s}",
        device, inode, type, linkCount, size, accessTime, modifyTime, changeTime);
  }

  /**
   * Builder for creating WasiFileStats instances.
   */
  public static final class Builder {
    private long device = 0;
    private long inode = 0;
    private WasiFileType type = WasiFileType.UNKNOWN;
    private long linkCount = 1;
    private long size = 0;
    private Instant accessTime = Instant.EPOCH;
    private Instant modifyTime = Instant.EPOCH;
    private Instant changeTime = Instant.EPOCH;

    private Builder() {}

    public Builder device(final long device) {
      this.device = device;
      return this;
    }

    public Builder inode(final long inode) {
      this.inode = inode;
      return this;
    }

    public Builder type(final WasiFileType type) {
      this.type = type;
      return this;
    }

    public Builder linkCount(final long linkCount) {
      this.linkCount = linkCount;
      return this;
    }

    public Builder size(final long size) {
      this.size = size;
      return this;
    }

    public Builder accessTime(final Instant accessTime) {
      this.accessTime = accessTime;
      return this;
    }

    public Builder modifyTime(final Instant modifyTime) {
      this.modifyTime = modifyTime;
      return this;
    }

    public Builder changeTime(final Instant changeTime) {
      this.changeTime = changeTime;
      return this;
    }

    public WasiFileStats build() {
      return new WasiFileStats(
          device, inode, type, linkCount, size, accessTime, modifyTime, changeTime);
    }
  }
}