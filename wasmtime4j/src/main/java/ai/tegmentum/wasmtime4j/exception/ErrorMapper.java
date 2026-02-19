package ai.tegmentum.wasmtime4j.exception;

import ai.tegmentum.wasmtime4j.exception.LinkingException.LinkingErrorType;
import java.util.logging.Logger;

/**
 * Shared utility for mapping native error codes to categorized Java exceptions.
 *
 * <p>This class provides the canonical error code to exception mapping used by both JNI and Panama
 * implementations. It ensures consistent error categorization regardless of the underlying native
 * binding mechanism.
 *
 * <p>Error codes are defined by the Rust {@code ErrorCode} enum and mirrored in {@link
 * WasmErrorCode}. This mapper translates those codes into the appropriate exception hierarchy.
 *
 * @since 1.0.0
 */
public final class ErrorMapper {

  private static final Logger LOGGER = Logger.getLogger(ErrorMapper.class.getName());

  /** Private constructor to prevent instantiation of utility class. */
  private ErrorMapper() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Maps a native error code and context message to an appropriate Java exception.
   *
   * <p>The returned exception type is chosen based on the error code category:
   *
   * <ul>
   *   <li>Compilation errors (-1) -&gt; CompilationException
   *   <li>Validation errors (-2) -&gt; ValidationException
   *   <li>Runtime/memory/function/type errors -&gt; WasmRuntimeException
   *   <li>Import/export errors (-9) -&gt; LinkingException
   *   <li>Instance errors (-6) -&gt; InstantiationException
   *   <li>Resource errors (-11) -&gt; ResourceException
   *   <li>WASI errors (-15) -&gt; WasiException
   *   <li>Security errors (-16, -22) -&gt; WasmSecurityException
   *   <li>All others -&gt; WasmException
   * </ul>
   *
   * @param errorCode the native error code
   * @param context a description of the operation that failed
   * @return the appropriate Java exception
   */
  public static WasmException mapErrorCode(final int errorCode, final String context) {
    final WasmErrorCode wasmErrorCode = WasmErrorCode.fromCode(errorCode);
    final String prefix = context != null ? context + ": " : "";

    if (wasmErrorCode == null) {
      LOGGER.warning("Unknown native error code: " + errorCode);
      return new WasmException(prefix + "Unknown native error (code " + errorCode + ")");
    }

    final String message = prefix + wasmErrorCode.getDescription();

    switch (wasmErrorCode) {
      case SUCCESS:
        LOGGER.warning("mapErrorCode called with SUCCESS");
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
  public static WasmException mapErrorCode(final int errorCode) {
    return mapErrorCode(errorCode, null);
  }
}
