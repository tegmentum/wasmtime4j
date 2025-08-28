package ai.tegmentum.wasmtime4j.panama.wasi.exception;

import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;

/**
 * Exception thrown when WASI file system operations fail in Panama FFI implementation.
 *
 * <p>This exception provides detailed error information for file system operations that fail due
 * to various reasons such as permission denied, file not found, or I/O errors.
 *
 * @since 1.0.0
 */
public class WasiFileSystemException extends PanamaException {

  /** The WASI error code associated with this exception. */
  private final String wasiErrorCode;

  /**
   * Creates a new WASI file system exception.
   *
   * @param message the error message
   * @param wasiErrorCode the WASI error code
   */
  public WasiFileSystemException(final String message, final String wasiErrorCode) {
    super(message);
    this.wasiErrorCode = wasiErrorCode;
  }

  /**
   * Creates a new WASI file system exception with a cause.
   *
   * @param message the error message
   * @param wasiErrorCode the WASI error code
   * @param cause the underlying cause
   */
  public WasiFileSystemException(final String message, final String wasiErrorCode,
      final Throwable cause) {
    super(message, cause);
    this.wasiErrorCode = wasiErrorCode;
  }

  /**
   * Gets the WASI error code associated with this exception.
   *
   * @return the WASI error code
   */
  public String getWasiErrorCode() {
    return wasiErrorCode;
  }

  @Override
  public String toString() {
    return String.format("WasiFileSystemException[%s]: %s", wasiErrorCode, getMessage());
  }
}