package ai.tegmentum.wasmtime4j.exception;

/**
 * Error codes from the native Wasmtime FFI layer.
 *
 * <p>This enum mirrors the Rust {@code ErrorCode} enum in {@code
 * wasmtime4j-native/src/error/mod.rs} exactly. It serves as the single source of truth for
 * Java-side error code mapping, ensuring consistent error classification across both JNI and Panama
 * implementations.
 *
 * <p>Each variant corresponds to a specific category of native error and maps to an integer code
 * used in the C FFI interface. The integer codes are negative values from -1 to -26, with 0
 * representing success.
 *
 * @since 1.0.0
 */
public enum WasmErrorCode {

  /** Operation completed successfully. */
  SUCCESS(0, "No error"),

  /** WebAssembly compilation failed. */
  COMPILATION_ERROR(-1, "WebAssembly compilation failed"),

  /** WebAssembly module validation failed. */
  VALIDATION_ERROR(-2, "WebAssembly module validation failed"),

  /** WebAssembly runtime error occurred. */
  RUNTIME_ERROR(-3, "WebAssembly runtime error"),

  /** Engine configuration error. */
  ENGINE_CONFIG_ERROR(-4, "Engine configuration error"),

  /** Store creation or management error. */
  STORE_ERROR(-5, "Store error"),

  /** Instance creation or management error. */
  INSTANCE_ERROR(-6, "Instance error"),

  /** Memory access or allocation error. */
  MEMORY_ERROR(-7, "Memory access or allocation error"),

  /** Function invocation error. */
  FUNCTION_ERROR(-8, "Function invocation error"),

  /** Import or export resolution error. */
  IMPORT_EXPORT_ERROR(-9, "Import or export resolution error"),

  /** Type conversion or validation error. */
  TYPE_ERROR(-10, "Type conversion or validation error"),

  /** Resource management error. */
  RESOURCE_ERROR(-11, "Resource management error"),

  /** I/O operation error. */
  IO_ERROR(-12, "I/O operation error"),

  /** Invalid parameter provided. */
  INVALID_PARAMETER_ERROR(-13, "Invalid parameter"),

  /** Threading or concurrency error. */
  CONCURRENCY_ERROR(-14, "Threading or concurrency error"),

  /** WASI-related error. */
  WASI_ERROR(-15, "WASI error"),

  /** Security and permission violation error. */
  SECURITY_ERROR(-16, "Security and permission violation error"),

  /** Component model error. */
  COMPONENT_ERROR(-17, "Component model error"),

  /** Interface definition or binding error. */
  INTERFACE_ERROR(-18, "Interface definition or binding error"),

  /** Network operation error. */
  NETWORK_ERROR(-19, "Network operation error"),

  /** Process execution error. */
  PROCESS_ERROR(-20, "Process execution error"),

  /** Internal system error. */
  INTERNAL_ERROR(-21, "Internal system error"),

  /** Security violation error. */
  SECURITY_VIOLATION(-22, "Security violation error"),

  /** Invalid data format error. */
  INVALID_DATA(-23, "Invalid data format error"),

  /** I/O operation error (secondary). */
  IO_OPERATION_ERROR(-24, "I/O operation error"),

  /** Unsupported operation error. */
  UNSUPPORTED_OPERATION(-25, "Unsupported operation"),

  /** Operation would block (non-blocking I/O). */
  WOULD_BLOCK(-26, "Operation would block");

  private final int code;
  private final String description;

  WasmErrorCode(final int code, final String description) {
    this.code = code;
    this.description = description;
  }

  /**
   * Returns the integer error code used in the native FFI layer.
   *
   * @return the native error code
   */
  public int getCode() {
    return code;
  }

  /**
   * Returns a human-readable description of the error.
   *
   * @return the error description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Looks up a {@code WasmErrorCode} by its integer code.
   *
   * @param code the native error code
   * @return the corresponding {@code WasmErrorCode}, or {@code null} if no match is found
   */
  public static WasmErrorCode fromCode(final int code) {
    for (final WasmErrorCode errorCode : values()) {
      if (errorCode.code == code) {
        return errorCode;
      }
    }
    return null;
  }
}
