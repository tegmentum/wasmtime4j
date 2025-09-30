package ai.tegmentum.wasmtime4j.exception;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for mapping native Wasmtime errors to specific Java exceptions.
 *
 * <p>This class provides consistent error mapping between native Wasmtime error codes and Java
 * exception types across both JNI and Panama implementations. It analyzes native error messages and
 * context to create appropriate specific exceptions with detailed information and recovery
 * suggestions.
 *
 * <p>Error mapping features:
 *
 * <ul>
 *   <li>Native error code to Java exception type mapping
 *   <li>Error message analysis and categorization
 *   <li>Context extraction from native error details
 *   <li>Recovery suggestion generation
 *   <li>Error chain preservation for debugging
 * </ul>
 *
 * @since 1.0.0
 */
public final class ErrorMapper {

  private static final Logger logger = Logger.getLogger(ErrorMapper.class.getName());

  // Native error code constants (from wasmtime4j-native/src/error.rs)
  /** Native success code. */
  public static final int SUCCESS = 0;

  /** Native compilation error code. */
  public static final int COMPILATION_ERROR = -1;

  /** Native validation error code. */
  public static final int VALIDATION_ERROR = -2;

  /** Native runtime error code. */
  public static final int RUNTIME_ERROR = -3;

  /** Native engine configuration error code. */
  public static final int ENGINE_CONFIG_ERROR = -4;

  /** Native store error code. */
  public static final int STORE_ERROR = -5;

  /** Native instance error code. */
  public static final int INSTANCE_ERROR = -6;

  /** Native memory error code. */
  public static final int MEMORY_ERROR = -7;

  /** Native function error code. */
  public static final int FUNCTION_ERROR = -8;

  /** Native import/export error code. */
  public static final int IMPORT_EXPORT_ERROR = -9;

  /** Native type error code. */
  public static final int TYPE_ERROR = -10;

  /** Native resource error code. */
  public static final int RESOURCE_ERROR = -11;

  /** Native I/O error code. */
  public static final int IO_ERROR = -12;

  /** Native invalid parameter error code. */
  public static final int INVALID_PARAMETER_ERROR = -13;

  /** Native concurrency error code. */
  public static final int CONCURRENCY_ERROR = -14;

  /** Native WASI error code. */
  public static final int WASI_ERROR = -15;

  /** Native component error code. */
  public static final int COMPONENT_ERROR = -16;

  /** Native interface error code. */
  public static final int INTERFACE_ERROR = -17;

  /** Native internal error code. */
  public static final int INTERNAL_ERROR = -18;

  // Error message pattern matching
  private static final Pattern TRAP_PATTERN =
      Pattern.compile("trap[: ](.+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern FUNCTION_PATTERN =
      Pattern.compile("function[: ]([\\w_]+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern OFFSET_PATTERN =
      Pattern.compile("offset[: ](\\d+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern IMPORT_PATTERN =
      Pattern.compile("import[: ]([\\w_]+)\\.([\\w_]+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern MODULE_PATTERN =
      Pattern.compile("module[: ]([\\w_]+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern SECTION_PATTERN =
      Pattern.compile("section[: ]([\\w_]+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern ERRNO_PATTERN =
      Pattern.compile("errno[: ](\\d+)", Pattern.CASE_INSENSITIVE);

  // Private constructor to prevent instantiation
  private ErrorMapper() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Maps a native error code and message to the appropriate Java exception.
   *
   * @param errorCode the native error code
   * @param errorMessage the error message from native code
   * @return the appropriate Java exception
   */
  public static WasmException mapError(final int errorCode, final String errorMessage) {
    return mapError(errorCode, errorMessage, null);
  }

  /**
   * Maps a native error code, message, and cause to the appropriate Java exception.
   *
   * @param errorCode the native error code
   * @param errorMessage the error message from native code
   * @param cause the underlying cause (may be null)
   * @return the appropriate Java exception
   */
  public static WasmException mapError(
      final int errorCode, final String errorMessage, final Throwable cause) {
    if (errorMessage == null || errorMessage.isEmpty()) {
      logger.warning("Empty error message for error code: " + errorCode);
      return createDefaultException(errorCode, "Unknown error", cause);
    }

    logger.fine("Mapping error code " + errorCode + " with message: " + errorMessage);

    switch (errorCode) {
      case SUCCESS:
        // This shouldn't happen, but handle gracefully
        return new WasmException("Success code received as error", cause);

      case COMPILATION_ERROR:
        return mapCompilationError(errorMessage, cause);

      case VALIDATION_ERROR:
        return mapValidationError(errorMessage, cause);

      case RUNTIME_ERROR:
        return mapRuntimeError(errorMessage, cause);

      case ENGINE_CONFIG_ERROR:
      case STORE_ERROR:
      case INSTANCE_ERROR:
        return mapInstantiationError(errorMessage, cause);

      case MEMORY_ERROR:
        return mapMemoryError(errorMessage, cause);

      case FUNCTION_ERROR:
        return mapFunctionError(errorMessage, cause);

      case IMPORT_EXPORT_ERROR:
        return mapLinkingError(errorMessage, cause);

      case TYPE_ERROR:
        return mapTypeError(errorMessage, cause);

      case RESOURCE_ERROR:
        return mapResourceError(errorMessage, cause);

      case IO_ERROR:
        return mapIoError(errorMessage, cause);

      case INVALID_PARAMETER_ERROR:
        return new WasmException("Invalid parameter: " + errorMessage, cause);

      case CONCURRENCY_ERROR:
        return new WasmException("Concurrency error: " + errorMessage, cause);

      case WASI_ERROR:
        return mapWasiError(errorMessage, cause);

      case COMPONENT_ERROR:
      case INTERFACE_ERROR:
        return mapComponentError(errorMessage, cause);

      case INTERNAL_ERROR:
      default:
        return createDefaultException(errorCode, errorMessage, cause);
    }
  }

  /** Maps compilation errors to specific compilation exception types. */
  private static CompilationException mapCompilationError(
      final String errorMessage, final Throwable cause) {
    final String lowerMessage = errorMessage.toLowerCase();

    // Determine compilation error type based on message content
    ModuleCompilationException.CompilationErrorType errorType;
    ModuleCompilationException.CompilationPhase phase =
        ModuleCompilationException.CompilationPhase.UNKNOWN;

    if (lowerMessage.contains("out of memory") || lowerMessage.contains("memory")) {
      errorType = ModuleCompilationException.CompilationErrorType.OUT_OF_MEMORY;
    } else if (lowerMessage.contains("timeout") || lowerMessage.contains("time")) {
      errorType = ModuleCompilationException.CompilationErrorType.TIMEOUT;
    } else if (lowerMessage.contains("too complex") || lowerMessage.contains("complex")) {
      errorType = ModuleCompilationException.CompilationErrorType.FUNCTION_TOO_COMPLEX;
    } else if (lowerMessage.contains("unsupported") || lowerMessage.contains("not supported")) {
      errorType = ModuleCompilationException.CompilationErrorType.UNSUPPORTED_INSTRUCTION;
    } else if (lowerMessage.contains("optimization") || lowerMessage.contains("optimize")) {
      errorType = ModuleCompilationException.CompilationErrorType.OPTIMIZATION_FAILED;
      phase = ModuleCompilationException.CompilationPhase.OPTIMIZATION;
    } else if (lowerMessage.contains("register") || lowerMessage.contains("allocation")) {
      errorType = ModuleCompilationException.CompilationErrorType.REGISTER_ALLOCATION_FAILED;
      phase = ModuleCompilationException.CompilationPhase.REGISTER_ALLOCATION;
    } else if (lowerMessage.contains("code generation") || lowerMessage.contains("codegen")) {
      errorType = ModuleCompilationException.CompilationErrorType.CODE_GENERATION_FAILED;
      phase = ModuleCompilationException.CompilationPhase.CODE_GENERATION;
    } else {
      errorType = ModuleCompilationException.CompilationErrorType.UNKNOWN;
    }

    // Extract function information if available
    final Matcher functionMatcher = FUNCTION_PATTERN.matcher(errorMessage);
    final String functionName = functionMatcher.find() ? functionMatcher.group(1) : null;

    return new ModuleCompilationException(
        errorType, errorMessage, phase, functionName, null, cause);
  }

  /** Maps validation errors to specific validation exception types. */
  private static ValidationException mapValidationError(
      final String errorMessage, final Throwable cause) {
    final String lowerMessage = errorMessage.toLowerCase();

    // Determine validation error type based on message content
    ModuleValidationException.ValidationErrorType errorType;

    if (lowerMessage.contains("magic") || lowerMessage.contains("version")) {
      errorType = ModuleValidationException.ValidationErrorType.INVALID_MAGIC_NUMBER;
    } else if (lowerMessage.contains("malformed") || lowerMessage.contains("corrupt")) {
      errorType = ModuleValidationException.ValidationErrorType.MALFORMED_MODULE;
    } else if (lowerMessage.contains("type mismatch") || lowerMessage.contains("type")) {
      errorType = ModuleValidationException.ValidationErrorType.TYPE_MISMATCH;
    } else if (lowerMessage.contains("import")) {
      errorType = ModuleValidationException.ValidationErrorType.INVALID_IMPORT;
    } else if (lowerMessage.contains("export")) {
      errorType = ModuleValidationException.ValidationErrorType.INVALID_EXPORT;
    } else if (lowerMessage.contains("memory")) {
      errorType = ModuleValidationException.ValidationErrorType.INVALID_MEMORY_DEFINITION;
    } else if (lowerMessage.contains("table")) {
      errorType = ModuleValidationException.ValidationErrorType.INVALID_TABLE_DEFINITION;
    } else if (lowerMessage.contains("function")) {
      errorType = ModuleValidationException.ValidationErrorType.INVALID_FUNCTION_BODY;
    } else if (lowerMessage.contains("unsupported") || lowerMessage.contains("feature")) {
      errorType = ModuleValidationException.ValidationErrorType.UNSUPPORTED_FEATURE;
    } else if (lowerMessage.contains("limit") || lowerMessage.contains("exceed")) {
      errorType = ModuleValidationException.ValidationErrorType.LIMIT_EXCEEDED;
    } else {
      errorType = ModuleValidationException.ValidationErrorType.UNKNOWN;
    }

    // Extract section and offset information if available
    final Matcher sectionMatcher = SECTION_PATTERN.matcher(errorMessage);
    final String moduleSection = sectionMatcher.find() ? sectionMatcher.group(1) : null;

    final Matcher offsetMatcher = OFFSET_PATTERN.matcher(errorMessage);
    final Integer byteOffset =
        offsetMatcher.find() ? Integer.parseInt(offsetMatcher.group(1)) : null;

    return new ModuleValidationException(errorType, errorMessage, moduleSection, byteOffset, cause);
  }

  /** Maps runtime errors to specific runtime or trap exception types. */
  private static RuntimeException mapRuntimeError(
      final String errorMessage, final Throwable cause) {
    final String lowerMessage = errorMessage.toLowerCase();

    // Check if this is a trap condition
    final Matcher trapMatcher = TRAP_PATTERN.matcher(errorMessage);
    if (trapMatcher.find() || lowerMessage.contains("trap")) {
      return mapTrapError(errorMessage, cause);
    }

    // Determine runtime error type
    RuntimeException.RuntimeErrorType errorType;

    if (lowerMessage.contains("timeout") || lowerMessage.contains("time")) {
      errorType = RuntimeException.RuntimeErrorType.TIMEOUT;
    } else if (lowerMessage.contains("interrupt")) {
      errorType = RuntimeException.RuntimeErrorType.INTERRUPTED;
    } else if (lowerMessage.contains("memory") || lowerMessage.contains("access")) {
      errorType = RuntimeException.RuntimeErrorType.MEMORY_ACCESS_VIOLATION;
    } else if (lowerMessage.contains("stack")) {
      errorType = RuntimeException.RuntimeErrorType.STACK_ERROR;
    } else if (lowerMessage.contains("resource") || lowerMessage.contains("limit")) {
      errorType = RuntimeException.RuntimeErrorType.RESOURCE_EXHAUSTED;
    } else if (lowerMessage.contains("function")) {
      errorType = RuntimeException.RuntimeErrorType.FUNCTION_EXECUTION_FAILED;
    } else if (lowerMessage.contains("host")) {
      errorType = RuntimeException.RuntimeErrorType.HOST_FUNCTION_FAILED;
    } else {
      errorType = RuntimeException.RuntimeErrorType.UNKNOWN;
    }

    // Extract function information if available
    final Matcher functionMatcher = FUNCTION_PATTERN.matcher(errorMessage);
    final String functionName = functionMatcher.find() ? functionMatcher.group(1) : null;

    return new RuntimeException(errorType, errorMessage, functionName, cause);
  }

  /** Maps trap errors to specific trap exception types. */
  private static TrapException mapTrapError(final String errorMessage, final Throwable cause) {
    final String lowerMessage = errorMessage.toLowerCase();

    // Determine trap type based on message content
    TrapException.TrapType trapType;

    if (lowerMessage.contains("stack overflow")) {
      trapType = TrapException.TrapType.STACK_OVERFLOW;
    } else if (lowerMessage.contains("out of bounds") && lowerMessage.contains("memory")) {
      trapType = TrapException.TrapType.MEMORY_OUT_OF_BOUNDS;
    } else if (lowerMessage.contains("out of bounds") && lowerMessage.contains("table")) {
      trapType = TrapException.TrapType.TABLE_OUT_OF_BOUNDS;
    } else if (lowerMessage.contains("out of bounds") && lowerMessage.contains("array")) {
      trapType = TrapException.TrapType.ARRAY_OUT_OF_BOUNDS;
    } else if (lowerMessage.contains("misaligned") || lowerMessage.contains("alignment")) {
      trapType = TrapException.TrapType.HEAP_MISALIGNED;
    } else if (lowerMessage.contains("null") && lowerMessage.contains("call")) {
      trapType = TrapException.TrapType.INDIRECT_CALL_TO_NULL;
    } else if (lowerMessage.contains("null") && lowerMessage.contains("reference")) {
      trapType = TrapException.TrapType.NULL_REFERENCE;
    } else if (lowerMessage.contains("signature") || lowerMessage.contains("type mismatch")) {
      trapType = TrapException.TrapType.BAD_SIGNATURE;
    } else if (lowerMessage.contains("overflow") && lowerMessage.contains("integer")) {
      trapType = TrapException.TrapType.INTEGER_OVERFLOW;
    } else if (lowerMessage.contains("division by zero")
        || lowerMessage.contains("divide by zero")) {
      trapType = TrapException.TrapType.INTEGER_DIVISION_BY_ZERO;
    } else if (lowerMessage.contains("conversion") || lowerMessage.contains("float")) {
      trapType = TrapException.TrapType.BAD_CONVERSION_TO_INTEGER;
    } else if (lowerMessage.contains("unreachable")) {
      trapType = TrapException.TrapType.UNREACHABLE_CODE_REACHED;
    } else if (lowerMessage.contains("interrupt")) {
      trapType = TrapException.TrapType.INTERRUPT;
    } else if (lowerMessage.contains("fuel") || lowerMessage.contains("out of fuel")) {
      trapType = TrapException.TrapType.OUT_OF_FUEL;
    } else {
      trapType = TrapException.TrapType.UNKNOWN;
    }

    // Extract function and offset information if available
    final Matcher functionMatcher = FUNCTION_PATTERN.matcher(errorMessage);
    final String functionName = functionMatcher.find() ? functionMatcher.group(1) : null;

    final Matcher offsetMatcher = OFFSET_PATTERN.matcher(errorMessage);
    final Integer instructionOffset =
        offsetMatcher.find() ? Integer.parseInt(offsetMatcher.group(1)) : null;

    return new TrapException(trapType, errorMessage, null, functionName, instructionOffset, cause);
  }

  /** Maps instantiation errors to specific instantiation exception types. */
  private static InstantiationException mapInstantiationError(
      final String errorMessage, final Throwable cause) {
    final String lowerMessage = errorMessage.toLowerCase();

    // Determine instantiation error type
    ModuleInstantiationException.InstantiationErrorType errorType;
    ModuleInstantiationException.InstantiationPhase phase =
        ModuleInstantiationException.InstantiationPhase.UNKNOWN;

    if (lowerMessage.contains("import") && lowerMessage.contains("not found")) {
      errorType = ModuleInstantiationException.InstantiationErrorType.MISSING_IMPORT;
      phase = ModuleInstantiationException.InstantiationPhase.IMPORT_RESOLUTION;
    } else if (lowerMessage.contains("import") && lowerMessage.contains("type")) {
      errorType = ModuleInstantiationException.InstantiationErrorType.IMPORT_TYPE_MISMATCH;
      phase = ModuleInstantiationException.InstantiationPhase.IMPORT_RESOLUTION;
    } else if (lowerMessage.contains("signature")) {
      errorType = ModuleInstantiationException.InstantiationErrorType.FUNCTION_SIGNATURE_MISMATCH;
      phase = ModuleInstantiationException.InstantiationPhase.IMPORT_RESOLUTION;
    } else if (lowerMessage.contains("memory") && lowerMessage.contains("allocation")) {
      errorType = ModuleInstantiationException.InstantiationErrorType.MEMORY_ALLOCATION_FAILED;
      phase = ModuleInstantiationException.InstantiationPhase.MEMORY_ALLOCATION;
    } else if (lowerMessage.contains("table") && lowerMessage.contains("allocation")) {
      errorType = ModuleInstantiationException.InstantiationErrorType.TABLE_ALLOCATION_FAILED;
      phase = ModuleInstantiationException.InstantiationPhase.TABLE_ALLOCATION;
    } else if (lowerMessage.contains("start function")) {
      errorType = ModuleInstantiationException.InstantiationErrorType.START_FUNCTION_FAILED;
      phase = ModuleInstantiationException.InstantiationPhase.START_FUNCTION_EXEC;
    } else if (lowerMessage.contains("data segment")) {
      errorType = ModuleInstantiationException.InstantiationErrorType.DATA_SEGMENT_INIT_FAILED;
      phase = ModuleInstantiationException.InstantiationPhase.DATA_SEGMENT_INIT;
    } else if (lowerMessage.contains("element segment")) {
      errorType = ModuleInstantiationException.InstantiationErrorType.ELEMENT_SEGMENT_INIT_FAILED;
      phase = ModuleInstantiationException.InstantiationPhase.ELEMENT_SEGMENT_INIT;
    } else if (lowerMessage.contains("timeout")) {
      errorType = ModuleInstantiationException.InstantiationErrorType.TIMEOUT;
    } else if (lowerMessage.contains("limit") || lowerMessage.contains("resource")) {
      errorType = ModuleInstantiationException.InstantiationErrorType.RESOURCE_LIMIT_EXCEEDED;
    } else {
      errorType = ModuleInstantiationException.InstantiationErrorType.UNKNOWN;
    }

    // Extract import information if available
    final Matcher importMatcher = IMPORT_PATTERN.matcher(errorMessage);
    final String moduleName = importMatcher.find() ? importMatcher.group(1) : null;
    final String importName = importMatcher.find() ? importMatcher.group(2) : null;

    return new ModuleInstantiationException(
        errorType, errorMessage, phase, importName, moduleName, cause);
  }

  /** Maps linking errors to specific linking exception types. */
  private static LinkingException mapLinkingError(
      final String errorMessage, final Throwable cause) {
    final String lowerMessage = errorMessage.toLowerCase();

    // Determine linking error type
    LinkingException.LinkingErrorType errorType;

    if (lowerMessage.contains("import") && lowerMessage.contains("not found")) {
      errorType = LinkingException.LinkingErrorType.IMPORT_NOT_FOUND;
    } else if (lowerMessage.contains("export") && lowerMessage.contains("not found")) {
      errorType = LinkingException.LinkingErrorType.EXPORT_NOT_FOUND;
    } else if (lowerMessage.contains("signature") || lowerMessage.contains("function")) {
      errorType = LinkingException.LinkingErrorType.FUNCTION_SIGNATURE_MISMATCH;
    } else if (lowerMessage.contains("memory") && lowerMessage.contains("size")) {
      errorType = LinkingException.LinkingErrorType.MEMORY_SIZE_MISMATCH;
    } else if (lowerMessage.contains("memory") && lowerMessage.contains("limit")) {
      errorType = LinkingException.LinkingErrorType.MEMORY_LIMITS_INCOMPATIBLE;
    } else if (lowerMessage.contains("table") && lowerMessage.contains("size")) {
      errorType = LinkingException.LinkingErrorType.TABLE_SIZE_MISMATCH;
    } else if (lowerMessage.contains("table") && lowerMessage.contains("type")) {
      errorType = LinkingException.LinkingErrorType.TABLE_TYPE_MISMATCH;
    } else if (lowerMessage.contains("global") && lowerMessage.contains("type")) {
      errorType = LinkingException.LinkingErrorType.GLOBAL_TYPE_MISMATCH;
    } else if (lowerMessage.contains("global") && lowerMessage.contains("mutability")) {
      errorType = LinkingException.LinkingErrorType.GLOBAL_MUTABILITY_MISMATCH;
    } else if (lowerMessage.contains("circular")) {
      errorType = LinkingException.LinkingErrorType.CIRCULAR_DEPENDENCY;
    } else if (lowerMessage.contains("namespace")) {
      errorType = LinkingException.LinkingErrorType.NAMESPACE_CONFLICT;
    } else if (lowerMessage.contains("host function")) {
      errorType = LinkingException.LinkingErrorType.HOST_FUNCTION_BINDING_FAILED;
    } else if (lowerMessage.contains("wasi")) {
      errorType = LinkingException.LinkingErrorType.WASI_IMPORT_FAILED;
    } else if (lowerMessage.contains("component")) {
      errorType = LinkingException.LinkingErrorType.COMPONENT_LINKING_FAILED;
    } else if (lowerMessage.contains("interface")) {
      errorType = LinkingException.LinkingErrorType.INTERFACE_TYPE_MISMATCH;
    } else {
      errorType = LinkingException.LinkingErrorType.UNKNOWN;
    }

    // Extract import/export information if available
    final Matcher importMatcher = IMPORT_PATTERN.matcher(errorMessage);
    final String moduleName = importMatcher.find() ? importMatcher.group(1) : null;
    final String itemName = importMatcher.find() ? importMatcher.group(2) : null;

    return new LinkingException(errorType, errorMessage, moduleName, itemName, null, null, cause);
  }

  /** Maps WASI errors to specific WASI exception types. */
  private static WasiException mapWasiError(final String errorMessage, final Throwable cause) {
    final String lowerMessage = errorMessage.toLowerCase();

    // Check if this is a file system error
    if (lowerMessage.contains("file")
        || lowerMessage.contains("directory")
        || lowerMessage.contains("path")) {
      return mapWasiFileSystemError(errorMessage, cause);
    }

    // Determine WASI error category
    WasiException.ErrorCategory category;
    if (lowerMessage.contains("network")) {
      category = WasiException.ErrorCategory.NETWORK;
    } else if (lowerMessage.contains("permission") || lowerMessage.contains("access")) {
      category = WasiException.ErrorCategory.PERMISSION;
    } else if (lowerMessage.contains("resource") || lowerMessage.contains("limit")) {
      category = WasiException.ErrorCategory.RESOURCE_LIMIT;
    } else if (lowerMessage.contains("component")) {
      category = WasiException.ErrorCategory.COMPONENT;
    } else if (lowerMessage.contains("config")) {
      category = WasiException.ErrorCategory.CONFIGURATION;
    } else {
      category = WasiException.ErrorCategory.SYSTEM;
    }

    return new WasiException(errorMessage, null, null, false, category, cause);
  }

  /** Maps WASI file system errors to specific file system exception types. */
  private static WasiFileSystemException mapWasiFileSystemError(
      final String errorMessage, final Throwable cause) {
    final String lowerMessage = errorMessage.toLowerCase();

    // Determine file system error type
    WasiFileSystemException.FileSystemErrorType errorType;

    if (lowerMessage.contains("not found") || lowerMessage.contains("enoent")) {
      errorType = WasiFileSystemException.FileSystemErrorType.NOT_FOUND;
    } else if (lowerMessage.contains("permission denied") || lowerMessage.contains("eacces")) {
      errorType = WasiFileSystemException.FileSystemErrorType.PERMISSION_DENIED;
    } else if (lowerMessage.contains("already exists") || lowerMessage.contains("eexist")) {
      errorType = WasiFileSystemException.FileSystemErrorType.ALREADY_EXISTS;
    } else if (lowerMessage.contains("is a directory") || lowerMessage.contains("eisdir")) {
      errorType = WasiFileSystemException.FileSystemErrorType.IS_DIRECTORY;
    } else if (lowerMessage.contains("not a directory") || lowerMessage.contains("enotdir")) {
      errorType = WasiFileSystemException.FileSystemErrorType.NOT_DIRECTORY;
    } else if (lowerMessage.contains("directory not empty") || lowerMessage.contains("enotempty")) {
      errorType = WasiFileSystemException.FileSystemErrorType.DIRECTORY_NOT_EMPTY;
    } else if (lowerMessage.contains("no space") || lowerMessage.contains("enospc")) {
      errorType = WasiFileSystemException.FileSystemErrorType.NO_SPACE;
    } else if (lowerMessage.contains("file too large") || lowerMessage.contains("efbig")) {
      errorType = WasiFileSystemException.FileSystemErrorType.FILE_TOO_LARGE;
    } else if (lowerMessage.contains("bad file descriptor") || lowerMessage.contains("ebadf")) {
      errorType = WasiFileSystemException.FileSystemErrorType.INVALID_FILE_DESCRIPTOR;
    } else if (lowerMessage.contains("i/o error") || lowerMessage.contains("eio")) {
      errorType = WasiFileSystemException.FileSystemErrorType.IO_ERROR;
    } else {
      errorType = WasiFileSystemException.FileSystemErrorType.UNKNOWN;
    }

    // Extract errno code if available
    final Matcher errnoMatcher = ERRNO_PATTERN.matcher(errorMessage);
    final Integer errnoCode = errnoMatcher.find() ? Integer.parseInt(errnoMatcher.group(1)) : null;

    return new WasiFileSystemException(errorType, errorMessage, null, null, errnoCode, cause);
  }

  // Helper methods for other error types (simplified for brevity)
  private static WasmException mapMemoryError(final String errorMessage, final Throwable cause) {
    return new RuntimeException(
        RuntimeException.RuntimeErrorType.MEMORY_ACCESS_VIOLATION, errorMessage, null, cause);
  }

  private static WasmException mapFunctionError(final String errorMessage, final Throwable cause) {
    return new RuntimeException(
        RuntimeException.RuntimeErrorType.FUNCTION_EXECUTION_FAILED, errorMessage, null, cause);
  }

  private static WasmException mapTypeError(final String errorMessage, final Throwable cause) {
    return new ValidationException("Type error: " + errorMessage, cause);
  }

  private static WasmException mapResourceError(final String errorMessage, final Throwable cause) {
    return new RuntimeException(
        RuntimeException.RuntimeErrorType.RESOURCE_EXHAUSTED, errorMessage, null, cause);
  }

  private static WasmException mapIoError(final String errorMessage, final Throwable cause) {
    return new WasiFileSystemException(
        WasiFileSystemException.FileSystemErrorType.IO_ERROR, errorMessage, cause);
  }

  private static WasmException mapComponentError(final String errorMessage, final Throwable cause) {
    return new WasiException(
        errorMessage, null, null, false, WasiException.ErrorCategory.COMPONENT, cause);
  }

  private static WasmException createDefaultException(
      final int errorCode, final String errorMessage, final Throwable cause) {
    return new WasmException("Error code " + errorCode + ": " + errorMessage, cause);
  }
}
