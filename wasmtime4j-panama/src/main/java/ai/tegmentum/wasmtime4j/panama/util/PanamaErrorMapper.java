package ai.tegmentum.wasmtime4j.panama.util;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.InstantiationException;
import ai.tegmentum.wasmtime4j.exception.LinkingException;
import ai.tegmentum.wasmtime4j.exception.LinkingException.LinkingErrorType;
import ai.tegmentum.wasmtime4j.exception.ResourceException;
import ai.tegmentum.wasmtime4j.exception.WasmSecurityException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasiException;
import ai.tegmentum.wasmtime4j.exception.WasmErrorCode;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import java.util.logging.Logger;

/**
 * Utility class for mapping native error codes to appropriate Java exceptions in Panama.
 *
 * <p>This class translates native error codes (as defined by {@link WasmErrorCode}) into the
 * appropriate exception types from the public API exception hierarchy. It ensures that Panama
 * callers produce meaningful, categorized exceptions instead of raw error code integers.
 */
public final class PanamaErrorMapper {

  private static final Logger LOGGER = Logger.getLogger(PanamaErrorMapper.class.getName());

  /** Private constructor to prevent instantiation of utility class. */
  private PanamaErrorMapper() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Maps a native error code and context message to an appropriate Java exception.
   *
   * <p>The returned exception type is chosen based on the error code category:
   *
   * <ul>
   *   <li>Compilation errors (-1) → {@link CompilationException}
   *   <li>Validation errors (-2) → {@link ValidationException}
   *   <li>Runtime/memory/function/type errors → {@link WasmRuntimeException}
   *   <li>Import/export errors (-9) → {@link LinkingException}
   *   <li>Instance errors (-6) → {@link InstantiationException}
   *   <li>Resource errors (-11) → {@link ResourceException}
   *   <li>WASI errors (-15) → {@link WasiException}
   *   <li>Security errors (-16, -22) → {@link WasmSecurityException}
   *   <li>All others → {@link WasmException}
   * </ul>
   *
   * @param errorCode the native error code
   * @param context a description of the operation that failed
   * @return the appropriate Java exception
   */
  public static WasmException mapNativeError(final int errorCode, final String context) {
    final WasmErrorCode wasmErrorCode = WasmErrorCode.fromCode(errorCode);
    final String prefix = context != null ? context + ": " : "";

    if (wasmErrorCode == null) {
      LOGGER.warning("Unknown native error code: " + errorCode);
      return new WasmException(prefix + "Unknown native error (code " + errorCode + ")");
    }

    final String message = prefix + wasmErrorCode.getDescription();

    switch (wasmErrorCode) {
      case SUCCESS:
        LOGGER.warning("mapNativeError called with SUCCESS");
        return new WasmException(prefix + "No error occurred");

      case COMPILATION_ERROR:
        return new CompilationException(message);

      case VALIDATION_ERROR:
        return new ValidationException(message);

      case INSTANCE_ERROR:
        return new InstantiationException(message);

      case IMPORT_EXPORT_ERROR:
        return new LinkingException(LinkingErrorType.UNKNOWN, message);

      case RESOURCE_ERROR:
        return new ResourceException(message);

      case WASI_ERROR:
        return new WasiException(message);

      case SECURITY_ERROR:
      case SECURITY_VIOLATION:
        return new WasmSecurityException(message);

      case RUNTIME_ERROR:
      case ENGINE_CONFIG_ERROR:
      case STORE_ERROR:
      case MEMORY_ERROR:
      case FUNCTION_ERROR:
      case TYPE_ERROR:
      case CONCURRENCY_ERROR:
      case COMPONENT_ERROR:
      case INTERFACE_ERROR:
      case NETWORK_ERROR:
      case PROCESS_ERROR:
      case INTERNAL_ERROR:
      case IO_ERROR:
      case IO_OPERATION_ERROR:
      case INVALID_PARAMETER_ERROR:
      case INVALID_DATA:
      case UNSUPPORTED_OPERATION:
      case WOULD_BLOCK:
        return new WasmRuntimeException(message);

      default:
        LOGGER.warning("Unhandled WasmErrorCode: " + wasmErrorCode);
        return new WasmException(prefix + "Unknown native error (code " + errorCode + ")");
    }
  }

  /**
   * Maps a native error code to an appropriate Java exception with no additional context.
   *
   * @param errorCode the native error code
   * @return the appropriate Java exception
   */
  public static WasmException mapNativeError(final int errorCode) {
    return mapNativeError(errorCode, null);
  }

  /**
   * Returns a human-readable description for a native error code.
   *
   * @param errorCode the native error code
   * @return the error description, or a default message for unknown codes
   */
  public static String getErrorDescription(final int errorCode) {
    final WasmErrorCode wasmErrorCode = WasmErrorCode.fromCode(errorCode);
    if (wasmErrorCode != null) {
      return wasmErrorCode.getDescription();
    }
    return "Unknown native error (code " + errorCode + ")";
  }
}
