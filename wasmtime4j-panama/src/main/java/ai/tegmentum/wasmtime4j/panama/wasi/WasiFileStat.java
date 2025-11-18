package ai.tegmentum.wasmtime4j.panama.wasi;

/**
 * WASI file status information.
 *
 * <p>This class represents file metadata returned by WASI filestat operations.
 *
 * @since 1.0.0
 */
public final class WasiFileStat {
  private final long device;
  private final long inode;
  private final int fileType;
  private final long linkCount;
  private final long size;
  private final long accessTime;
  private final long modificationTime;
  private final long changeTime;

  /**
   * Creates a new WASI file stat instance.
   *
   * @param device the device ID
   * @param inode the inode number
   * @param fileType the file type
   * @param linkCount the number of hard links
   * @param size the file size in bytes
   * @param accessTime the last access time in nanoseconds
   * @param modificationTime the last modification time in nanoseconds
   * @param changeTime the last change time in nanoseconds
   */
  public WasiFileStat(
      final long device,
      final long inode,
      final int fileType,
      final long linkCount,
      final long size,
      final long accessTime,
      final long modificationTime,
      final long changeTime) {
    this.device = device;
    this.inode = inode;
    this.fileType = fileType;
    this.linkCount = linkCount;
    this.size = size;
    this.accessTime = accessTime;
    this.modificationTime = modificationTime;
    this.changeTime = changeTime;
  }

  /**
   * Gets the device ID.
   *
   * @return the device ID
   */
  public long getDevice() {
    return device;
  }

  /**
   * Gets the inode number.
   *
   * @return the inode number
   */
  public long getInode() {
    return inode;
  }

  /**
   * Gets the file type.
   *
   * @return the file type
   */
  public int getFileType() {
    return fileType;
  }

  /**
   * Gets the number of hard links.
   *
   * @return the link count
   */
  public long getLinkCount() {
    return linkCount;
  }

  /**
   * Gets the file size in bytes.
   *
   * @return the file size
   */
  public long getSize() {
    return size;
  }

  /**
   * Gets the last access time in nanoseconds since epoch.
   *
   * @return the access time
   */
  public long getAccessTime() {
    return accessTime;
  }

  /**
   * Gets the last modification time in nanoseconds since epoch.
   *
   * @return the modification time
   */
  public long getModificationTime() {
    return modificationTime;
  }

  /**
   * Gets the last change time in nanoseconds since epoch.
   *
   * @return the change time
   */
  public long getChangeTime() {
    return changeTime;
  }

  @Override
  public String toString() {
    return String.format(
        "WasiFileStat{device=%d, inode=%d, fileType=%d, linkCount=%d, size=%d, "
            + "accessTime=%d, modificationTime=%d, changeTime=%d}",
        device, inode, fileType, linkCount, size, accessTime, modificationTime, changeTime);
  }
}
