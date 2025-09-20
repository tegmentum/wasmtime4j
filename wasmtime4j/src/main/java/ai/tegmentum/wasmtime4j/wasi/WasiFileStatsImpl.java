package ai.tegmentum.wasmtime4j.wasi;

import java.time.Instant;
import java.util.Objects;

/**
 * Implementation of WasiFileStats for file metadata.
 *
 * @since 1.0.0
 */
final class WasiFileStatsImpl implements WasiFileStats {

  private final long device;
  private final long inode;
  private final WasiFileType fileType;
  private final long linkCount;
  private final long size;
  private final Instant accessTime;
  private final Instant modificationTime;
  private final Instant statusChangeTime;
  private final Instant creationTime;
  private final WasiPermissions permissions;

  private WasiFileStatsImpl(
      final long device,
      final long inode,
      final WasiFileType fileType,
      final long linkCount,
      final long size,
      final Instant accessTime,
      final Instant modificationTime,
      final Instant statusChangeTime,
      final Instant creationTime,
      final WasiPermissions permissions) {
    this.device = device;
    this.inode = inode;
    this.fileType = Objects.requireNonNull(fileType, "File type cannot be null");
    this.linkCount = linkCount;
    this.size = size;
    this.accessTime = accessTime;
    this.modificationTime = modificationTime;
    this.statusChangeTime = statusChangeTime;
    this.creationTime = creationTime;
    this.permissions = Objects.requireNonNull(permissions, "Permissions cannot be null");
  }

  @Override
  public long getDevice() {
    return device;
  }

  @Override
  public long getInode() {
    return inode;
  }

  @Override
  public WasiFileType getFileType() {
    return fileType;
  }

  @Override
  public long getLinkCount() {
    return linkCount;
  }

  @Override
  public long getSize() {
    return size;
  }

  @Override
  public Instant getAccessTime() {
    return accessTime;
  }

  @Override
  public Instant getModificationTime() {
    return modificationTime;
  }

  @Override
  public Instant getStatusChangeTime() {
    return statusChangeTime;
  }

  @Override
  public Instant getCreationTime() {
    return creationTime;
  }

  @Override
  public WasiPermissions getPermissions() {
    return permissions;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final WasiFileStatsImpl that = (WasiFileStatsImpl) obj;
    return device == that.device
        && inode == that.inode
        && linkCount == that.linkCount
        && size == that.size
        && fileType == that.fileType
        && Objects.equals(accessTime, that.accessTime)
        && Objects.equals(modificationTime, that.modificationTime)
        && Objects.equals(statusChangeTime, that.statusChangeTime)
        && Objects.equals(creationTime, that.creationTime)
        && Objects.equals(permissions, that.permissions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        device,
        inode,
        fileType,
        linkCount,
        size,
        accessTime,
        modificationTime,
        statusChangeTime,
        creationTime,
        permissions);
  }

  @Override
  public String toString() {
    return "WasiFileStats{"
        + "device=" + device
        + ", inode=" + inode
        + ", fileType=" + fileType
        + ", linkCount=" + linkCount
        + ", size=" + size
        + ", accessTime=" + accessTime
        + ", modificationTime=" + modificationTime
        + ", statusChangeTime=" + statusChangeTime
        + ", creationTime=" + creationTime
        + ", permissions=" + permissions
        + "}";
  }

  /**
   * Builder implementation for WasiFileStats.
   */
  static final class BuilderImpl implements WasiFileStats.Builder {
    private long device;
    private long inode;
    private WasiFileType fileType;
    private long linkCount = 1;
    private long size;
    private Instant accessTime;
    private Instant modificationTime;
    private Instant statusChangeTime;
    private Instant creationTime;
    private WasiPermissions permissions;

    @Override
    public Builder device(final long device) {
      this.device = device;
      return this;
    }

    @Override
    public Builder inode(final long inode) {
      this.inode = inode;
      return this;
    }

    @Override
    public Builder fileType(final WasiFileType fileType) {
      this.fileType = Objects.requireNonNull(fileType, "File type cannot be null");
      return this;
    }

    @Override
    public Builder linkCount(final long linkCount) {
      if (linkCount < 0) {
        throw new IllegalArgumentException("Link count cannot be negative: " + linkCount);
      }
      this.linkCount = linkCount;
      return this;
    }

    @Override
    public Builder size(final long size) {
      if (size < 0) {
        throw new IllegalArgumentException("Size cannot be negative: " + size);
      }
      this.size = size;
      return this;
    }

    @Override
    public Builder accessTime(final Instant accessTime) {
      this.accessTime = accessTime;
      return this;
    }

    @Override
    public Builder modificationTime(final Instant modificationTime) {
      this.modificationTime = modificationTime;
      return this;
    }

    @Override
    public Builder statusChangeTime(final Instant statusChangeTime) {
      this.statusChangeTime = statusChangeTime;
      return this;
    }

    @Override
    public Builder creationTime(final Instant creationTime) {
      this.creationTime = creationTime;
      return this;
    }

    @Override
    public Builder permissions(final WasiPermissions permissions) {
      this.permissions = Objects.requireNonNull(permissions, "Permissions cannot be null");
      return this;
    }

    @Override
    public WasiFileStats build() {
      if (fileType == null) {
        throw new IllegalStateException("File type must be set");
      }
      if (permissions == null) {
        permissions = fileType.isDirectory()
            ? WasiPermissions.defaultDirectoryPermissions()
            : WasiPermissions.defaultFilePermissions();
      }

      return new WasiFileStatsImpl(
          device,
          inode,
          fileType,
          linkCount,
          size,
          accessTime,
          modificationTime,
          statusChangeTime,
          creationTime,
          permissions);
    }
  }
}