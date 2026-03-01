/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

      case WASI_EXIT:
        return parseI32Exit(context);

      case SECURITY_ERROR:
      case SECURITY_VIOLATION:
        return new WasmSecurityException(message);

      case MEMORY_ACCESS_ERROR:
        return new MemoryAccessException(message);

      case GC_HEAP_OOM:
        return new GcHeapOutOfMemoryException(message);

      case POOL_CONCURRENCY_LIMIT:
        return new PoolConcurrencyLimitException(message);

      case UNKNOWN_IMPORT:
        return new UnknownImportException(message);

      case RESOURCE_TABLE_ERROR:
        return new ResourceTableException(ResourceTableException.ErrorKind.NOT_PRESENT, message);

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
        return parseTrapOrRuntime(message);

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

  /**
   * Checks if the message indicates a WebAssembly trap and creates the appropriate exception.
   *
   * <p>Messages from the native layer that indicate a trap have the format "WebAssembly trap: ..."
   * and may optionally be prefixed with "[coredump:ID]" when coredump data is available.
   *
   * @param message the error message
   * @return a TrapException if the message indicates a trap, otherwise a WasmRuntimeException
   */
  private static WasmException parseTrapOrRuntime(final String message) {
    if (message == null) {
      return new WasmRuntimeException("Unknown runtime error");
    }

    // Check for coredump prefix and/or trap indicator
    String workingMessage = message;
    if (workingMessage.startsWith("[coredump:") || workingMessage.contains("WebAssembly trap:")) {
      return TrapException.fromNativeMessage(TrapException.TrapType.UNKNOWN, workingMessage);
    }

    return new WasmRuntimeException(message);
  }

  /**
   * Parses the exit code from an I32Exit error message and creates an I32ExitException.
   *
   * <p>The native layer sends the exit code in the message in the format "exit_code:N" where N is
   * the integer exit code. If parsing fails, defaults to exit code 1.
   *
   * @param message the error message from native code
   * @return an I32ExitException with the parsed exit code
   */
  private static I32ExitException parseI32Exit(final String message) {
    if (message != null) {
      final int colonIndex = message.lastIndexOf(':');
      if (colonIndex >= 0) {
        try {
          final int exitCode = Integer.parseInt(message.substring(colonIndex + 1).trim());
          return new I32ExitException(exitCode);
        } catch (NumberFormatException ignored) {
          // Fall through to default
        }
      }
    }
    return new I32ExitException(1);
  }
}
