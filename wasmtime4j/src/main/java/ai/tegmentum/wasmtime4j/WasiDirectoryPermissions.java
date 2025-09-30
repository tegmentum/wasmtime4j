package ai.tegmentum.wasmtime4j;

/**
 * Represents permissions for WASI directory access in Preview 2.
 *
 * <p>WasiDirectoryPermissions provides fine-grained control over what operations a WebAssembly
 * module can perform on a directory. This enables secure sandboxing with precise permission models.
 *
 * <p>Permissions can be combined using the fluent API for complex access patterns.
 *
 * @since 1.0.0
 */
public final class WasiDirectoryPermissions {

  private final boolean read;
  private final boolean write;
  private final boolean create;
  private final boolean delete;
  private final boolean list;
  private final boolean traverse;
  private final boolean metadata;

  private WasiDirectoryPermissions(Builder builder) {
    this.read = builder.read;
    this.write = builder.write;
    this.create = builder.create;
    this.delete = builder.delete;
    this.list = builder.list;
    this.traverse = builder.traverse;
    this.metadata = builder.metadata;
  }

  /**
   * Returns whether read permission is granted.
   *
   * @return true if read operations are allowed
   */
  public boolean canRead() {
    return read;
  }

  /**
   * Returns whether write permission is granted.
   *
   * @return true if write operations are allowed
   */
  public boolean canWrite() {
    return write;
  }

  /**
   * Returns whether create permission is granted.
   *
   * @return true if file/directory creation is allowed
   */
  public boolean canCreate() {
    return create;
  }

  /**
   * Returns whether delete permission is granted.
   *
   * @return true if file/directory deletion is allowed
   */
  public boolean canDelete() {
    return delete;
  }

  /**
   * Returns whether list permission is granted.
   *
   * @return true if directory listing is allowed
   */
  public boolean canList() {
    return list;
  }

  /**
   * Returns whether traverse permission is granted.
   *
   * @return true if directory traversal is allowed
   */
  public boolean canTraverse() {
    return traverse;
  }

  /**
   * Returns whether metadata access permission is granted.
   *
   * @return true if metadata access is allowed
   */
  public boolean canAccessMetadata() {
    return metadata;
  }

  /**
   * Creates a new permissions builder.
   *
   * @return a new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates read-only permissions.
   *
   * @return permissions for read-only access
   */
  public static WasiDirectoryPermissions readOnly() {
    return builder().allowRead().allowList().allowTraverse().allowMetadata().build();
  }

  /**
   * Creates read-write permissions.
   *
   * @return permissions for read-write access
   */
  public static WasiDirectoryPermissions readWrite() {
    return builder()
        .allowRead()
        .allowWrite()
        .allowCreate()
        .allowDelete()
        .allowList()
        .allowTraverse()
        .allowMetadata()
        .build();
  }

  /**
   * Creates full permissions (all operations allowed).
   *
   * @return permissions for all operations
   */
  public static WasiDirectoryPermissions full() {
    return readWrite(); // Currently same as read-write
  }

  /**
   * Creates no permissions (no operations allowed).
   *
   * @return permissions denying all operations
   */
  public static WasiDirectoryPermissions none() {
    return builder().build();
  }

  /** Builder for WasiDirectoryPermissions. */
  public static final class Builder {
    private boolean read = false;
    private boolean write = false;
    private boolean create = false;
    private boolean delete = false;
    private boolean list = false;
    private boolean traverse = false;
    private boolean metadata = false;

    private Builder() {}

    /**
     * Allows read operations on files.
     *
     * @return this builder for method chaining
     */
    public Builder allowRead() {
      this.read = true;
      return this;
    }

    /**
     * Allows write operations on files.
     *
     * @return this builder for method chaining
     */
    public Builder allowWrite() {
      this.write = true;
      return this;
    }

    /**
     * Allows creation of new files and directories.
     *
     * @return this builder for method chaining
     */
    public Builder allowCreate() {
      this.create = true;
      return this;
    }

    /**
     * Allows deletion of files and directories.
     *
     * @return this builder for method chaining
     */
    public Builder allowDelete() {
      this.delete = true;
      return this;
    }

    /**
     * Allows listing directory contents.
     *
     * @return this builder for method chaining
     */
    public Builder allowList() {
      this.list = true;
      return this;
    }

    /**
     * Allows traversing into subdirectories.
     *
     * @return this builder for method chaining
     */
    public Builder allowTraverse() {
      this.traverse = true;
      return this;
    }

    /**
     * Allows accessing file and directory metadata.
     *
     * @return this builder for method chaining
     */
    public Builder allowMetadata() {
      this.metadata = true;
      return this;
    }

    /**
     * Builds the WasiDirectoryPermissions instance.
     *
     * @return the configured permissions
     */
    public WasiDirectoryPermissions build() {
      return new WasiDirectoryPermissions(this);
    }
  }

  @Override
  public String toString() {
    return "WasiDirectoryPermissions{"
        + "read="
        + read
        + ", write="
        + write
        + ", create="
        + create
        + ", delete="
        + delete
        + ", list="
        + list
        + ", traverse="
        + traverse
        + ", metadata="
        + metadata
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WasiDirectoryPermissions that = (WasiDirectoryPermissions) o;

    return read == that.read
        && write == that.write
        && create == that.create
        && delete == that.delete
        && list == that.list
        && traverse == that.traverse
        && metadata == that.metadata;
  }

  @Override
  public int hashCode() {
    int result = (read ? 1 : 0);
    result = 31 * result + (write ? 1 : 0);
    result = 31 * result + (create ? 1 : 0);
    result = 31 * result + (delete ? 1 : 0);
    result = 31 * result + (list ? 1 : 0);
    result = 31 * result + (traverse ? 1 : 0);
    result = 31 * result + (metadata ? 1 : 0);
    return result;
  }
}
