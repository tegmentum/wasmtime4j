package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when WASI file system operations fail.
 *
 * <p>This exception is thrown when WASI file system operations encounter errors such as permission
 * denied, file not found, or I/O failures. WASI file system operations provide sandboxed access to
 * the host file system through capability-based security.
 *
 * <p>WASI file system exceptions provide detailed information about file system failures:
 *
 * <ul>
 *   <li>File system error type and errno code
 *   <li>File path and operation context
 *   <li>Permission and capability information
 *   <li>Recovery and configuration suggestions
 * </ul>
 *
 * @since 1.0.0
 */
public class WasiFileSystemException extends WasiException {

  private static final long serialVersionUID = 1L;

  /** The specific file system error type. */
  private final FileSystemErrorType errorType;

  /** WASI errno code (if available). */
  private final Integer errnoCode;

  /** File path where the error occurred. */
  private final String filePath;

  /** File operation that failed. */
  private final String fileOperation;

  /** Enumeration of WASI file system error types. */
  public enum FileSystemErrorType {
    /** File or directory not found. */
    NOT_FOUND("File or directory not found", 44), // ENOENT
    /** Permission denied. */
    PERMISSION_DENIED("Permission denied", 63), // EACCES
    /** File already exists. */
    ALREADY_EXISTS("File already exists", 20), // EEXIST
    /** Is a directory (expected file). */
    IS_DIRECTORY("Is a directory", 31), // EISDIR
    /** Not a directory (expected directory). */
    NOT_DIRECTORY("Not a directory", 54), // ENOTDIR
    /** Directory not empty. */
    DIRECTORY_NOT_EMPTY("Directory not empty", 66), // ENOTEMPTY
    /** No space left on device. */
    NO_SPACE("No space left on device", 51), // ENOSPC
    /** File too large. */
    FILE_TOO_LARGE("File too large", 27), // EFBIG
    /** Invalid file descriptor. */
    INVALID_FILE_DESCRIPTOR("Invalid file descriptor", 8), // EBADF
    /** I/O error. */
    IO_ERROR("I/O error", 29), // EIO
    /** Read-only file system. */
    READ_ONLY("Read-only file system", 67), // EROFS
    /** Too many open files. */
    TOO_MANY_OPEN_FILES("Too many open files", 24), // EMFILE
    /** File name too long. */
    NAME_TOO_LONG("File name too long", 37), // ENAMETOOLONG
    /** Invalid seek operation. */
    INVALID_SEEK("Invalid seek operation", 70), // ESPIPE
    /** Cross-device link. */
    CROSS_DEVICE_LINK("Cross-device link", 75), // EXDEV
    /** Operation not supported. */
    NOT_SUPPORTED("Operation not supported", 58), // ENOTSUP
    /** Invalid argument. */
    INVALID_ARGUMENT("Invalid argument", 28), // EINVAL
    /** Broken pipe. */
    BROKEN_PIPE("Broken pipe", 32), // EPIPE
    /** Connection reset. */
    CONNECTION_RESET("Connection reset", 104), // ECONNRESET
    /** Operation would block. */
    WOULD_BLOCK("Operation would block", 6), // EAGAIN
    /** Unknown file system error. */
    UNKNOWN("Unknown file system error", -1);

    private final String description;
    private final int errnoCode;

    FileSystemErrorType(final String description, final int errnoCode) {
      this.description = description;
      this.errnoCode = errnoCode;
    }

    /**
     * Gets a human-readable description of this file system error type.
     *
     * @return the error type description
     */
    public String getDescription() {
      return description;
    }

    /**
     * Gets the WASI errno code for this error type.
     *
     * @return the errno code
     */
    public int getErrnoCode() {
      return errnoCode;
    }

    /**
     * Gets the file system error type from errno code.
     *
     * @param errno the errno code
     * @return the corresponding error type, or UNKNOWN if not found
     */
    public static FileSystemErrorType fromErrno(final int errno) {
      for (FileSystemErrorType type : values()) {
        if (type.errnoCode == errno) {
          return type;
        }
      }
      return UNKNOWN;
    }
  }

  /**
   * Creates a new WASI file system exception with the specified error type and message.
   *
   * @param errorType the specific file system error type
   * @param message the error message
   */
  public WasiFileSystemException(final FileSystemErrorType errorType, final String message) {
    this(errorType, message, null, null, null, null);
  }

  /**
   * Creates a new WASI file system exception with the specified error type, message, and cause.
   *
   * @param errorType the specific file system error type
   * @param message the error message
   * @param cause the underlying cause
   */
  public WasiFileSystemException(
      final FileSystemErrorType errorType, final String message, final Throwable cause) {
    this(errorType, message, null, null, null, cause);
  }

  /**
   * Creates a new WASI file system exception with detailed error information.
   *
   * @param errorType the specific file system error type
   * @param message the error message
   * @param filePath file path where error occurred (may be null)
   * @param fileOperation file operation that failed (may be null)
   * @param errnoCode WASI errno code (may be null)
   * @param cause the underlying cause (may be null)
   */
  public WasiFileSystemException(
      final FileSystemErrorType errorType,
      final String message,
      final String filePath,
      final String fileOperation,
      final Integer errnoCode,
      final Throwable cause) {
    super(
        formatFileSystemMessage(errorType, message, filePath, fileOperation),
        fileOperation,
        filePath,
        isRetryableError(errorType),
        ErrorCategory.FILE_SYSTEM,
        cause);
    this.errorType = errorType != null ? errorType : FileSystemErrorType.UNKNOWN;
    this.filePath = filePath;
    this.fileOperation = fileOperation;
    this.errnoCode = errnoCode != null ? errnoCode : this.errorType.getErrnoCode();
  }

  /**
   * Gets the specific file system error type.
   *
   * @return the file system error type
   */
  public FileSystemErrorType getFileSystemErrorType() {
    return errorType;
  }

  /**
   * Gets the WASI errno code.
   *
   * @return the errno code
   */
  public Integer getErrnoCode() {
    return errnoCode;
  }

  /**
   * Gets the file path where the error occurred.
   *
   * @return the file path, or null if not available
   */
  public String getFilePath() {
    return filePath;
  }

  /**
   * Gets the file operation that failed.
   *
   * @return the file operation, or null if not available
   */
  public String getFileOperation() {
    return fileOperation;
  }

  /**
   * Checks if this file system error is related to permissions.
   *
   * @return true if this is a permission error, false otherwise
   */
  public boolean isPermissionError() {
    return errorType == FileSystemErrorType.PERMISSION_DENIED
        || errorType == FileSystemErrorType.READ_ONLY;
  }

  /**
   * Checks if this file system error is related to file existence.
   *
   * @return true if this is a file existence error, false otherwise
   */
  public boolean isExistenceError() {
    return errorType == FileSystemErrorType.NOT_FOUND
        || errorType == FileSystemErrorType.ALREADY_EXISTS;
  }

  /**
   * Checks if this file system error is related to file type expectations.
   *
   * @return true if this is a file type error, false otherwise
   */
  public boolean isFileTypeError() {
    return errorType == FileSystemErrorType.IS_DIRECTORY
        || errorType == FileSystemErrorType.NOT_DIRECTORY
        || errorType == FileSystemErrorType.DIRECTORY_NOT_EMPTY;
  }

  /**
   * Checks if this file system error is related to resource limits.
   *
   * @return true if this is a resource limit error, false otherwise
   */
  public boolean isResourceLimitError() {
    return errorType == FileSystemErrorType.NO_SPACE
        || errorType == FileSystemErrorType.FILE_TOO_LARGE
        || errorType == FileSystemErrorType.TOO_MANY_OPEN_FILES
        || errorType == FileSystemErrorType.NAME_TOO_LONG;
  }

  /**
   * Checks if this file system error is transient and may be retryable.
   *
   * @return true if this error might be retryable, false otherwise
   */
  public boolean isTransientError() {
    return errorType == FileSystemErrorType.IO_ERROR
        || errorType == FileSystemErrorType.WOULD_BLOCK
        || errorType == FileSystemErrorType.CONNECTION_RESET;
  }

  /**
   * Formats the exception message with file system error details.
   *
   * @param errorType the file system error type
   * @param message the base message
   * @param filePath the file path
   * @param fileOperation the file operation
   * @return the formatted message
   */
  private static String formatFileSystemMessage(
      final FileSystemErrorType errorType,
      final String message,
      final String filePath,
      final String fileOperation) {
    if (message == null || message.isEmpty()) {
      throw new IllegalArgumentException("Message cannot be null or empty");
    }

    final StringBuilder sb = new StringBuilder();

    if (errorType != null) {
      sb.append("[").append(errorType.name()).append("] ");
    }

    sb.append(message);

    if (fileOperation != null && !fileOperation.isEmpty()) {
      sb.append(" (operation: ").append(fileOperation).append(")");
    }

    if (filePath != null && !filePath.isEmpty()) {
      sb.append(" (path: ").append(filePath).append(")");
    }

    return sb.toString();
  }

  /**
   * Determines if a file system error type is potentially retryable.
   *
   * @param errorType the file system error type
   * @return true if retryable, false otherwise
   */
  private static boolean isRetryableError(final FileSystemErrorType errorType) {
    switch (errorType) {
      case IO_ERROR:
      case WOULD_BLOCK:
      case CONNECTION_RESET:
        return true;
      default:
        return false;
    }
  }
}
