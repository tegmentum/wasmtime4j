package ai.tegmentum.wasmtime4j.panama.wasi;

/**
 * Enumeration of WASI file system operations for permission validation in Panama FFI
 * implementation.
 *
 * <p>This enum defines the types of file system operations that can be performed through WASI,
 * allowing for fine-grained permission control and security validation in the Panama FFI context.
 *
 * <p>Each operation type corresponds to specific WASI system calls and represents different levels
 * of access that may be granted or denied based on the security policy.
 *
 * @since 1.0.0
 */
public enum WasiFileOperation {

  /** Reading file contents or metadata. */
  READ("read", "Reading file or directory contents"),

  /** Writing file contents or creating new files. */
  WRITE("write", "Writing file contents or creating files"),

  /** Executing files as programs. */
  EXECUTE("execute", "Executing files"),

  /** Creating new directories. */
  CREATE_DIRECTORY("create_directory", "Creating new directories"),

  /** Removing files or directories. */
  DELETE("delete", "Removing files or directories"),

  /** Renaming or moving files and directories. */
  RENAME("rename", "Renaming or moving files and directories"),

  /** Reading file or directory metadata (stat operations). */
  METADATA("metadata", "Reading file or directory metadata"),

  /** Modifying file permissions or ownership. */
  CHANGE_PERMISSIONS("change_permissions", "Modifying file permissions or ownership"),

  /** Creating symbolic or hard links. */
  CREATE_LINK("create_link", "Creating symbolic or hard links"),

  /** Following symbolic links. */
  FOLLOW_SYMLINKS("follow_symlinks", "Following symbolic links"),

  /** Listing directory contents. */
  LIST_DIRECTORY("list_directory", "Listing directory contents"),

  /** Setting file timestamps. */
  SET_TIMES("set_times", "Setting file timestamps"),

  /** Truncating files. */
  TRUNCATE("truncate", "Truncating files"),

  /** Synchronizing file contents to disk. */
  SYNC("sync", "Synchronizing file contents to disk"),

  /** Seeking within files. */
  SEEK("seek", "Seeking within files"),

  /** Polling file descriptors for I/O readiness. */
  POLL("poll", "Polling file descriptors for I/O readiness"),

  /** Opening files or directories. */
  OPEN("open", "Opening files or directories"),

  /** Closing files or directories. */
  CLOSE("close", "Closing files or directories");

  /** The operation identifier used in native code. */
  private final String operationId;

  /** Human-readable description of the operation. */
  private final String description;

  /**
   * Creates a new WASI file operation.
   *
   * @param operationId the operation identifier
   * @param description the operation description
   */
  WasiFileOperation(final String operationId, final String description) {
    this.operationId = operationId;
    this.description = description;
  }

  /**
   * Gets the operation identifier used in native code.
   *
   * @return the operation identifier
   */
  public String getOperationId() {
    return operationId;
  }

  /**
   * Gets the human-readable description of the operation.
   *
   * @return the operation description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if this operation requires read access.
   *
   * @return true if read access is required, false otherwise
   */
  public boolean requiresReadAccess() {
    return switch (this) {
      case READ, METADATA, LIST_DIRECTORY, FOLLOW_SYMLINKS, POLL -> true;
      default -> false;
    };
  }

  /**
   * Checks if this operation requires write access.
   *
   * @return true if write access is required, false otherwise
   */
  public boolean requiresWriteAccess() {
    return switch (this) {
      case WRITE,
          CREATE_DIRECTORY,
          DELETE,
          RENAME,
          CHANGE_PERMISSIONS,
          CREATE_LINK,
          SET_TIMES,
          TRUNCATE,
          SYNC ->
          true;
      default -> false;
    };
  }

  /**
   * Checks if this operation requires execute access.
   *
   * @return true if execute access is required, false otherwise
   */
  public boolean requiresExecuteAccess() {
    return this == EXECUTE;
  }

  /**
   * Checks if this operation modifies the file system.
   *
   * @return true if the operation modifies the file system, false otherwise
   */
  public boolean isModifyingOperation() {
    return switch (this) {
      case WRITE,
          CREATE_DIRECTORY,
          DELETE,
          RENAME,
          CHANGE_PERMISSIONS,
          CREATE_LINK,
          SET_TIMES,
          TRUNCATE ->
          true;
      default -> false;
    };
  }

  /**
   * Checks if this operation is potentially dangerous and requires elevated permissions.
   *
   * @return true if the operation is potentially dangerous, false otherwise
   */
  public boolean isDangerous() {
    return switch (this) {
      case EXECUTE, DELETE, RENAME, CHANGE_PERMISSIONS, CREATE_LINK -> true;
      default -> false;
    };
  }

  /**
   * Checks if this operation is a write operation.
   *
   * @return true if the operation is a write operation, false otherwise
   */
  public boolean isWriteOperation() {
    return requiresWriteAccess();
  }

  /**
   * Gets a WASI file operation by its operation identifier.
   *
   * @param operationId the operation identifier
   * @return the corresponding WASI file operation
   * @throws IllegalArgumentException if the operation ID is not found
   */
  public static WasiFileOperation fromOperationId(final String operationId) {
    if (operationId == null || operationId.isEmpty()) {
      throw new IllegalArgumentException("Operation ID cannot be null or empty");
    }

    for (final WasiFileOperation operation : values()) {
      if (operation.operationId.equals(operationId)) {
        return operation;
      }
    }

    throw new IllegalArgumentException("Unknown WASI file operation: " + operationId);
  }

  @Override
  public String toString() {
    return String.format("%s (%s)", name(), description);
  }
}
