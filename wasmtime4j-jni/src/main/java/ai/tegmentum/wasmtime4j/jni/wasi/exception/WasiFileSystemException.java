package ai.tegmentum.wasmtime4j.jni.wasi.exception;

/**
 * WASI exception for file system operation failures.
 *
 * <p>This exception is thrown when WASI file system operations fail, including:
 *
 * <ul>
 *   <li>File not found errors
 *   <li>Permission denied errors
 *   <li>I/O errors
 *   <li>Disk space errors
 *   <li>File system corruption errors
 * </ul>
 *
 * <p>The exception provides specific context about the file system operation that failed and
 * includes the file path or descriptor information for debugging.
 *
 * @since 1.0.0
 */
public final class WasiFileSystemException extends WasiException {

  private static final long serialVersionUID = 1L;

  /** The file path associated with the error. */
  private final String filePath;

  /** The file descriptor associated with the error. */
  private final int fileDescriptor;

  /**
   * Creates a new WASI file system exception.
   *
   * @param message the error message
   * @param errorCode the WASI error code
   * @param operation the file system operation that failed
   * @param filePath the file path associated with the error
   */
  public WasiFileSystemException(
      final String message,
      final WasiErrorCode errorCode,
      final String operation,
      final String filePath) {
    super(message, errorCode, operation, filePath);
    this.filePath = filePath;
    this.fileDescriptor = -1;
  }

  /**
   * Creates a new WASI file system exception with file descriptor.
   *
   * @param message the error message
   * @param errorCode the WASI error code
   * @param operation the file system operation that failed
   * @param fileDescriptor the file descriptor associated with the error
   */
  public WasiFileSystemException(
      final String message,
      final WasiErrorCode errorCode,
      final String operation,
      final int fileDescriptor) {
    super(message, errorCode, operation, "fd:" + fileDescriptor);
    this.filePath = null;
    this.fileDescriptor = fileDescriptor;
  }

  /**
   * Creates a new WASI file system exception with cause.
   *
   * @param message the error message
   * @param errorCode the WASI error code
   * @param operation the file system operation that failed
   * @param filePath the file path associated with the error
   * @param cause the underlying cause
   */
  public WasiFileSystemException(
      final String message,
      final WasiErrorCode errorCode,
      final String operation,
      final String filePath,
      final Throwable cause) {
    super(message, errorCode, operation, filePath, cause);
    this.filePath = filePath;
    this.fileDescriptor = -1;
  }

  /**
   * Creates a new WASI file system exception from error code and path.
   *
   * @param errorCode the WASI error code
   * @param operation the file system operation that failed
   * @param filePath the file path associated with the error
   */
  public WasiFileSystemException(
      final WasiErrorCode errorCode, final String operation, final String filePath) {
    this(errorCode.getDescription(), errorCode, operation, filePath);
  }

  /**
   * Creates a new WASI file system exception from error code and file descriptor.
   *
   * @param errorCode the WASI error code
   * @param operation the file system operation that failed
   * @param fileDescriptor the file descriptor associated with the error
   */
  public WasiFileSystemException(
      final WasiErrorCode errorCode, final String operation, final int fileDescriptor) {
    this(errorCode.getDescription(), errorCode, operation, fileDescriptor);
  }

  /**
   * Gets the file path associated with this error.
   *
   * @return the file path, or null if not applicable
   */
  public String getFilePath() {
    return filePath;
  }

  /**
   * Gets the file descriptor associated with this error.
   *
   * @return the file descriptor, or -1 if not applicable
   */
  public int getFileDescriptor() {
    return fileDescriptor;
  }

  /**
   * Checks if this exception involves a file path.
   *
   * @return true if a file path is associated with this error, false otherwise
   */
  public boolean hasFilePath() {
    return filePath != null;
  }

  /**
   * Checks if this exception involves a file descriptor.
   *
   * @return true if a file descriptor is associated with this error, false otherwise
   */
  public boolean hasFileDescriptor() {
    return fileDescriptor != -1;
  }

  /**
   * Factory method for file not found errors.
   *
   * @param operation the operation that failed
   * @param filePath the file path that was not found
   * @return a new file not found exception
   */
  public static WasiFileSystemException fileNotFound(final String operation, final String filePath) {
    return new WasiFileSystemException(WasiErrorCode.ENOENT, operation, filePath);
  }

  /**
   * Factory method for permission denied errors.
   *
   * @param operation the operation that failed
   * @param filePath the file path with denied access
   * @return a new permission denied exception
   */
  public static WasiFileSystemException permissionDenied(final String operation, final String filePath) {
    return new WasiFileSystemException(WasiErrorCode.EACCES, operation, filePath);
  }

  /**
   * Factory method for I/O errors.
   *
   * @param operation the operation that failed
   * @param filePath the file path with I/O error
   * @return a new I/O exception
   */
  public static WasiFileSystemException ioError(final String operation, final String filePath) {
    return new WasiFileSystemException(WasiErrorCode.EIO, operation, filePath);
  }

  /**
   * Factory method for disk space errors.
   *
   * @param operation the operation that failed
   * @param filePath the file path where disk space was exhausted
   * @return a new disk space exception
   */
  public static WasiFileSystemException noSpaceLeft(final String operation, final String filePath) {
    return new WasiFileSystemException(WasiErrorCode.ENOSPC, operation, filePath);
  }

  /**
   * Factory method for bad file descriptor errors.
   *
   * @param operation the operation that failed
   * @param fileDescriptor the invalid file descriptor
   * @return a new bad file descriptor exception
   */
  public static WasiFileSystemException badFileDescriptor(final String operation, final int fileDescriptor) {
    return new WasiFileSystemException(WasiErrorCode.EBADF, operation, fileDescriptor);
  }

  /**
   * Factory method for file already exists errors.
   *
   * @param operation the operation that failed
   * @param filePath the file path that already exists
   * @return a new file exists exception
   */
  public static WasiFileSystemException fileExists(final String operation, final String filePath) {
    return new WasiFileSystemException(WasiErrorCode.EEXIST, operation, filePath);
  }

  /**
   * Factory method for directory not empty errors.
   *
   * @param operation the operation that failed
   * @param filePath the directory path that is not empty
   * @return a new directory not empty exception
   */
  public static WasiFileSystemException directoryNotEmpty(final String operation, final String filePath) {
    return new WasiFileSystemException(WasiErrorCode.ENOTEMPTY, operation, filePath);
  }

  /**
   * Factory method for read-only file system errors.
   *
   * @param operation the operation that failed
   * @param filePath the file path on read-only file system
   * @return a new read-only file system exception
   */
  public static WasiFileSystemException readOnlyFileSystem(final String operation, final String filePath) {
    return new WasiFileSystemException(WasiErrorCode.EROFS, operation, filePath);
  }

  /**
   * Factory method for file name too long errors.
   *
   * @param operation the operation that failed
   * @param filePath the file path that is too long
   * @return a new file name too long exception
   */
  public static WasiFileSystemException fileNameTooLong(final String operation, final String filePath) {
    return new WasiFileSystemException(WasiErrorCode.ENAMETOOLONG, operation, filePath);
  }

  /**
   * Factory method for too many symbolic links errors.
   *
   * @param operation the operation that failed
   * @param filePath the file path with too many symbolic links
   * @return a new too many symbolic links exception
   */
  public static WasiFileSystemException tooManySymbolicLinks(final String operation, final String filePath) {
    return new WasiFileSystemException(WasiErrorCode.ELOOP, operation, filePath);
  }
}