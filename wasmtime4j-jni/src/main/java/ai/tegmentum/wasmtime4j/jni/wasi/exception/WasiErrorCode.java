package ai.tegmentum.wasmtime4j.jni.wasi.exception;

/**
 * WASI error codes corresponding to standard errno values.
 *
 * <p>This enum maps WASI system call error codes to their corresponding errno values and provides
 * categorization for different types of system errors. It enables proper error handling and
 * provides context for system operation failures.
 *
 * <p>Error codes are organized by category:
 *
 * <ul>
 *   <li>File system errors (ENOENT, EACCES, EIO, etc.)
 *   <li>Network errors (ECONNREFUSED, ETIMEDOUT, etc.)
 *   <li>Permission errors (EPERM, EACCES, etc.)
 *   <li>Resource limit errors (ENOMEM, EMFILE, etc.)
 *   <li>Invalid argument errors (EINVAL, EBADF, etc.)
 * </ul>
 *
 * @since 1.0.0
 */
public enum WasiErrorCode {

  // Success
  SUCCESS(0, "No error", false, false, false, false, false),

  // File system errors
  ENOENT(2, "No such file or directory", true, false, false, false, true),
  EIO(5, "Input/output error", true, false, false, false, false),
  EBADF(9, "Bad file descriptor", true, false, false, false, false),
  EACCES(13, "Permission denied", true, false, true, false, false),
  EEXIST(17, "File exists", true, false, false, false, false),
  ENOTDIR(20, "Not a directory", true, false, false, false, false),
  EISDIR(21, "Is a directory", true, false, false, false, false),
  EINVAL(22, "Invalid argument", false, false, false, false, false),
  EMFILE(24, "Too many open files", true, false, false, true, true),
  EFBIG(27, "File too large", true, false, false, true, false),
  ENOSPC(28, "No space left on device", true, false, false, true, true),
  EROFS(30, "Read-only file system", true, false, true, false, false),
  EPIPE(32, "Broken pipe", false, true, false, false, false),
  ENAMETOOLONG(36, "File name too long", true, false, false, false, false),
  ENOTEMPTY(39, "Directory not empty", true, false, false, false, false),
  ELOOP(40, "Too many symbolic links", true, false, false, false, true),

  // Network errors
  ECONNREFUSED(61, "Connection refused", false, true, false, false, true),
  ETIMEDOUT(60, "Operation timed out", false, true, false, false, true),
  EHOSTUNREACH(65, "No route to host", false, true, false, false, true),
  ENETDOWN(50, "Network is down", false, true, false, false, true),
  ENETUNREACH(51, "Network is unreachable", false, true, false, false, true),
  ECONNRESET(54, "Connection reset by peer", false, true, false, false, true),
  EADDRINUSE(48, "Address already in use", false, true, false, true, false),
  EADDRNOTAVAIL(49, "Address not available", false, true, false, false, true),
  ENOTCONN(57, "Socket is not connected", false, true, false, false, false),

  // Permission and security errors
  EPERM(1, "Operation not permitted", false, false, true, false, false),
  ESRCH(3, "No such process", false, false, true, false, false),

  // Resource limit errors
  ENOMEM(12, "Cannot allocate memory", false, false, false, true, true),
  ENFILE(23, "Too many open files in system", false, false, false, true, true),
  ENOBUFS(55, "No buffer space available", false, false, false, true, true),
  ENODEV(19, "No such device", false, false, false, true, false),

  // Process and signal errors
  EINTR(4, "Interrupted system call", false, false, false, false, true),
  ECHILD(10, "No child processes", false, false, false, false, false),
  EAGAIN(11, "Resource temporarily unavailable", false, false, false, true, true),
  EWOULDBLOCK(11, "Operation would block", false, false, false, true, true),
  EALREADY(37, "Operation already in progress", false, false, false, false, true),
  EINPROGRESS(36, "Operation now in progress", false, false, false, false, true),

  // Miscellaneous errors
  EDOM(33, "Numerical argument out of domain", false, false, false, false, false),
  ERANGE(34, "Numerical result out of range", false, false, false, false, false),
  EDEADLK(35, "Resource deadlock avoided", false, false, false, false, true),
  ENOLCK(77, "No locks available", false, false, false, true, true),
  ENOSYS(78, "Function not implemented", false, false, false, false, false),
  EMSGSIZE(90, "Message too long", false, true, false, false, false),
  EOVERFLOW(84, "Value too large for defined data type", false, false, false, false, false),
  ECANCELED(89, "Operation canceled", false, false, false, false, false),
  EIDRM(82, "Identifier removed", false, false, false, false, false),
  ENOMSG(91, "No message of desired type", false, false, false, false, true),
  EILSEQ(
      92, "Invalid or incomplete multibyte or wide character", false, false, false, false, false),
  EBADMSG(74, "Bad message", false, false, false, false, false),
  EMULTIHOP(95, "Multihop attempted", false, true, false, false, false),
  ENODATA(96, "No data available", false, false, false, false, true),
  ENOLINK(97, "Link has been severed", false, true, false, false, false),
  ENOSR(98, "No STREAM resources", false, false, false, true, true),
  ENOSTR(99, "Device not a stream", false, false, false, false, false),
  EPROTO(100, "Protocol error", false, true, false, false, false),
  ETIME(101, "Timer expired", false, false, false, false, true),
  ETXTBSY(26, "Text file busy", true, false, false, true, true),
  ENOTTY(25, "Inappropriate ioctl for device", false, false, false, false, false),

  /** Unknown error code for unrecognized errno values. */
  UNKNOWN(-1, "Unknown error", false, false, false, false, false);

  /** The errno value for this error code. */
  private final int errno;

  /** Human-readable description of the error. */
  private final String description;

  /** Whether this is a file system related error. */
  private final boolean fileSystemError;

  /** Whether this is a network related error. */
  private final boolean networkError;

  /** Whether this is a permission or security related error. */
  private final boolean permissionError;

  /** Whether this is a resource limit related error. */
  private final boolean resourceLimitError;

  /** Whether the operation can potentially be retried. */
  private final boolean retryable;

  /**
   * Creates a new WASI error code.
   *
   * @param errno the errno value
   * @param description the error description
   * @param fileSystemError whether this is a file system error
   * @param networkError whether this is a network error
   * @param permissionError whether this is a permission error
   * @param resourceLimitError whether this is a resource limit error
   * @param retryable whether the operation can be retried
   */
  WasiErrorCode(
      final int errno,
      final String description,
      final boolean fileSystemError,
      final boolean networkError,
      final boolean permissionError,
      final boolean resourceLimitError,
      final boolean retryable) {
    this.errno = errno;
    this.description = description;
    this.fileSystemError = fileSystemError;
    this.networkError = networkError;
    this.permissionError = permissionError;
    this.resourceLimitError = resourceLimitError;
    this.retryable = retryable;
  }

  /**
   * Gets the errno value for this error code.
   *
   * @return the errno value
   */
  public int getErrno() {
    return errno;
  }

  /**
   * Gets the human-readable description of this error.
   *
   * @return the error description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if this is a file system related error.
   *
   * @return true if this is a file system error, false otherwise
   */
  public boolean isFileSystemError() {
    return fileSystemError;
  }

  /**
   * Checks if this is a network related error.
   *
   * @return true if this is a network error, false otherwise
   */
  public boolean isNetworkError() {
    return networkError;
  }

  /**
   * Checks if this is a permission or security related error.
   *
   * @return true if this is a permission error, false otherwise
   */
  public boolean isPermissionError() {
    return permissionError;
  }

  /**
   * Checks if this is a resource limit related error.
   *
   * @return true if this is a resource limit error, false otherwise
   */
  public boolean isResourceLimitError() {
    return resourceLimitError;
  }

  /**
   * Checks if the operation can potentially be retried.
   *
   * @return true if the operation can be retried, false otherwise
   */
  public boolean isRetryable() {
    return retryable;
  }

  /**
   * Gets a WASI error code by its errno value.
   *
   * @param errno the errno value
   * @return the corresponding WASI error code
   * @throws IllegalArgumentException if the errno is not recognized
   */
  public static WasiErrorCode fromErrno(final int errno) {
    for (final WasiErrorCode errorCode : values()) {
      if (errorCode.errno == errno) {
        return errorCode;
      }
    }

    throw new IllegalArgumentException("Unknown errno value: " + errno);
  }

  /**
   * Gets a WASI error code by its errno value, or returns null if not found.
   *
   * @param errno the errno value
   * @return the corresponding WASI error code, or null if not found
   */
  public static WasiErrorCode fromErrnoOrNull(final int errno) {
    try {
      return fromErrno(errno);
    } catch (final IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Creates a generic error code for unknown errno values.
   *
   * @param errno the unknown errno value (note: ignored, always returns UNKNOWN)
   * @return the UNKNOWN error code
   */
  public static WasiErrorCode createGeneric(final int errno) {
    // Return the UNKNOWN enum value since we can't create new enum instances
    return UNKNOWN;
  }

  @Override
  public String toString() {
    return String.format("%s (%d): %s", name(), errno, description);
  }
}
