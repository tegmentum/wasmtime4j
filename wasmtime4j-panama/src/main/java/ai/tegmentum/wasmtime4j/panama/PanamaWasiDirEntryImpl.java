package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.wasi.WasiDirEntry;
import ai.tegmentum.wasmtime4j.wasi.WasiFileType;
import ai.tegmentum.wasmtime4j.wasi.WasiPermissions;
import java.time.Instant;

/**
 * Panama implementation of WasiDirEntry.
 *
 * @since 1.0.0
 */
final class PanamaWasiDirEntryImpl implements WasiDirEntry {

  private final String name;
  private final WasiFileType type;
  private final long inode;
  private final long size;
  private final Instant accessTime;
  private final Instant modificationTime;
  private final Instant creationTime;
  private final WasiPermissions permissions;

  PanamaWasiDirEntryImpl(
      final String name,
      final WasiFileType type,
      final long inode,
      final long size,
      final Instant accessTime,
      final Instant modificationTime,
      final Instant creationTime,
      final WasiPermissions permissions) {
    this.name = name;
    this.type = type;
    this.inode = inode;
    this.size = size;
    this.accessTime = accessTime;
    this.modificationTime = modificationTime;
    this.creationTime = creationTime;
    this.permissions = permissions;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public WasiFileType getType() {
    return type;
  }

  @Override
  public long getInode() {
    return inode;
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
  public Instant getCreationTime() {
    return creationTime;
  }

  @Override
  public WasiPermissions getPermissions() {
    return permissions;
  }
}
