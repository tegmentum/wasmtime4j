package ai.tegmentum.wasmtime4j.security;

/**
 * File system permissions for sandboxed access.
 *
 * @since 1.0.0
 */
public final class FilePermissions {

  private final boolean read;
  private final boolean write;
  private final boolean create;
  private final boolean delete;
  private final boolean list;

  /**
   * Creates new file permissions.
   *
   * @param read read permission
   * @param write write permission
   * @param create create permission
   * @param delete delete permission
   * @param list list directory permission
   */
  public FilePermissions(
      final boolean read,
      final boolean write,
      final boolean create,
      final boolean delete,
      final boolean list) {
    this.read = read;
    this.write = write;
    this.create = create;
    this.delete = delete;
    this.list = list;
  }

  /**
   * Creates read-only permissions.
   *
   * @return read-only file permissions
   */
  public static FilePermissions readOnly() {
    return new FilePermissions(true, false, false, false, true);
  }

  /**
   * Creates read-write permissions.
   *
   * @return read-write file permissions
   */
  public static FilePermissions readWrite() {
    return new FilePermissions(true, true, true, false, true);
  }

  /**
   * Creates full permissions.
   *
   * @return full file permissions
   */
  public static FilePermissions full() {
    return new FilePermissions(true, true, true, true, true);
  }

  public boolean canRead() {
    return read;
  }

  public boolean canWrite() {
    return write;
  }

  public boolean canCreate() {
    return create;
  }

  public boolean canDelete() {
    return delete;
  }

  public boolean canList() {
    return list;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof FilePermissions)) return false;
    final FilePermissions that = (FilePermissions) obj;
    return read == that.read
        && write == that.write
        && create == that.create
        && delete == that.delete
        && list == that.list;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(read, write, create, delete, list);
  }

  @Override
  public String toString() {
    return String.format(
        "FilePermissions{read=%s, write=%s, create=%s, delete=%s, list=%s}",
        read, write, create, delete, list);
  }
}
