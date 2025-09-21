package ai.tegmentum.wasmtime4j.panama.error;

import ai.tegmentum.wasmtime4j.diagnostics.ErrorContext;
import ai.tegmentum.wasmtime4j.diagnostics.ErrorDiagnostics;
import ai.tegmentum.wasmtime4j.diagnostics.RuntimeEnvironment;
import ai.tegmentum.wasmtime4j.diagnostics.RuntimeMetrics;
import ai.tegmentum.wasmtime4j.diagnostics.WasmError;
import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced Panama FFI error handler that provides comprehensive error diagnostics.
 *
 * <p>This class integrates the new error handling and diagnostics framework with the Panama Foreign
 * Function Interface to provide detailed error information.
 *
 * @since 1.0.0
 */
public final class PanamaErrorHandler {

  private static final Logger LOGGER = Logger.getLogger(PanamaErrorHandler.class.getName());
  private static final AtomicLong ERROR_COUNTER = new AtomicLong(0);

  /**
   * Creates a comprehensive WasmError from a Panama exception.
   *
   * @param panamaException the Panama exception
   * @param errorContext the native error context segment
   * @return the comprehensive WebAssembly error
   */
  public static WasmError createWasmError(
      final PanamaException panamaException, final Optional<MemorySegment> errorContext) {
    final String errorId = generateErrorId();
    final int nativeErrorCode = extractNativeErrorCode(panamaException, errorContext);
    final WasmError.Category category = mapErrorCategory(nativeErrorCode);
    final WasmError.Severity severity = mapErrorSeverity(nativeErrorCode);

    final WasmError.Builder builder =
        WasmError.builder()
            .errorId(errorId)
            .message(panamaException.getMessage())
            .category(category)
            .severity(severity)
            .timestamp(Instant.now())
            .cause(panamaException);

    // Add enhanced diagnostics if context is available
    errorContext.ifPresent(context -> addEnhancedDiagnostics(builder, context, panamaException));

    // Add error context
    final ErrorContext contextInfo = createErrorContext(panamaException, errorContext);
    builder.context(contextInfo);

    // Add error diagnostics
    final ErrorDiagnostics diagnostics = createErrorDiagnostics(panamaException, errorContext);
    builder.diagnostics(diagnostics);

    // Determine if error is recoverable
    final boolean recoverable = isRecoverable(nativeErrorCode);
    builder.recoverable(recoverable);

    // Add retry information if applicable
    if (shouldRetry(nativeErrorCode)) {
      builder.shouldRetry(true);
      builder.retryDelayMs(calculateRetryDelay(nativeErrorCode));
    }

    return builder.build();
  }

  /**
   * Creates a comprehensive WasmError from a native error code.
   *
   * @param nativeErrorCode the native error code
   * @param errorContext the native error context segment
   * @param operation the operation that failed
   * @return the comprehensive WebAssembly error
   */
  public static WasmError createWasmError(
      final int nativeErrorCode,
      final Optional<MemorySegment> errorContext,
      final String operation) {
    // Create the underlying Panama exception first
    final String nativeMessage = extractNativeErrorMessage(errorContext);
    final PanamaException panamaException =
        createPanamaException(nativeErrorCode, nativeMessage, operation);

    return createWasmError(panamaException, errorContext);
  }

  /**
   * Handles a native error by creating comprehensive diagnostics and throwing appropriate
   * exception.
   *
   * @param nativeErrorCode the native error code
   * @param errorContext the native error context segment
   * @param operation the operation that failed
   * @throws PanamaException always thrown with comprehensive error information
   */
  public static void handleNativeError(
      final int nativeErrorCode, final Optional<MemorySegment> errorContext, final String operation)
      throws PanamaException {
    if (nativeErrorCode == 0) {
      return; // No error
    }

    final WasmError wasmError = createWasmError(nativeErrorCode, errorContext, operation);

    // Log the error for debugging
    LOGGER.log(Level.SEVERE, "Native error occurred: {0}", wasmError.getMessage());
    if (LOGGER.isLoggable(Level.FINE)) {
      LOGGER.log(Level.FINE, "Error diagnostics: {0}", wasmError.getDiagnostics().getDescription());
    }

    // Throw the underlying Panama exception
    throw (PanamaException)
        wasmError
            .getCause()
            .orElseThrow(
                () -> new PanamaException("Failed to extract Panama exception from WasmError"));
  }

  /**
   * Validates a native memory segment and throws comprehensive error if invalid.
   *
   * @param segment the memory segment to validate
   * @param resourceType the type of resource
   * @param errorContext the native error context segment
   * @throws PanamaException if the segment is invalid
   */
  public static void validateNativeSegment(
      final MemorySegment segment,
      final String resourceType,
      final Optional<MemorySegment> errorContext)
      throws PanamaException {
    if (segment == null || segment.address() == 0) {
      final WasmError wasmError =
          WasmError.builder()
              .errorId(generateErrorId())
              .message(String.format("Invalid %s segment: null pointer", resourceType))
              .category(WasmError.Category.RESOURCE)
              .severity(WasmError.Severity.ERROR)
              .timestamp(Instant.now())
              .context(createErrorContext(null, errorContext))
              .recoverable(false)
              .build();

      LOGGER.log(Level.WARNING, "Invalid native segment detected: {0}", resourceType);

      throw new PanamaException(wasmError.getMessage());
    }
  }

  /**
   * Safely reads an error code from a native memory segment.
   *
   * @param errorContext the error context segment
   * @return the error code, or 0 if unable to read
   */
  public static int safeReadErrorCode(final Optional<MemorySegment> errorContext) {
    return errorContext
        .map(
            segment -> {
              try {
                return segment.get(ValueLayout.JAVA_INT, 0);
              } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Failed to read error code from native segment", e);
                return 0;
              }
            })
        .orElse(0);
  }

  /**
   * Safely reads an error message from a native memory segment.
   *
   * @param errorContext the error context segment
   * @return the error message, or null if unable to read
   */
  public static String safeReadErrorMessage(final Optional<MemorySegment> errorContext) {
    return errorContext
        .map(
            segment -> {
              try {
                // Assuming the message is stored as a null-terminated string after the error code
                final long messageOffset = ValueLayout.JAVA_INT.byteSize();
                if (segment.byteSize() > messageOffset) {
                  return segment.getUtf8String(messageOffset);
                }
                return null;
              } catch (final Exception e) {
                LOGGER.log(Level.WARNING, "Failed to read error message from native segment", e);
                return null;
              }
            })
        .orElse(null);
  }

  private static void addEnhancedDiagnostics(
      final WasmError.Builder builder,
      final MemorySegment errorContext,
      final PanamaException panamaException) {
    try {
      // Add memory segment information
      builder.addProperty("segmentAddress", errorContext.address());
      builder.addProperty("segmentSize", errorContext.byteSize());

      // Try to extract additional context if available
      if (errorContext.byteSize() > 8) { // Has more than just error code
        final String additionalContext = safeReadErrorMessage(Optional.of(errorContext));
        if (additionalContext != null && !additionalContext.isEmpty()) {
          builder.addProperty("nativeContext", additionalContext);
        }
      }

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to add enhanced diagnostics", e);
      builder.addProperty("diagnosticsError", e.getMessage());
    }
  }

  private static ErrorContext createErrorContext(
      final PanamaException panamaException, final Optional<MemorySegment> errorContext) {
    final ErrorContext.Builder contextBuilder =
        ErrorContext.builder().runtimeEnvironment(RuntimeEnvironment.snapshot());

    // Add thread information
    contextBuilder.threadInfo(ai.tegmentum.wasmtime4j.diagnostics.ThreadInfo.current());

    // Add memory state
    contextBuilder.memoryState(ai.tegmentum.wasmtime4j.diagnostics.MemoryState.snapshot());

    // Add Panama-specific context
    if (panamaException != null) {
      contextBuilder.addProperty("panamaErrorType", panamaException.getClass().getSimpleName());
    }

    errorContext.ifPresent(
        segment -> {
          contextBuilder.addProperty("errorContextAddress", segment.address());
          contextBuilder.addProperty("errorContextSize", segment.byteSize());
        });

    contextBuilder.addProperty("ffiImplementation", "panama");
    contextBuilder.addProperty("javaVersion", System.getProperty("java.version"));

    return contextBuilder.build();
  }

  private static ErrorDiagnostics createErrorDiagnostics(
      final PanamaException panamaException, final Optional<MemorySegment> errorContext) {
    final int errorCode = extractNativeErrorCode(panamaException, errorContext);

    final ErrorDiagnostics.Builder diagnosticsBuilder =
        ErrorDiagnostics.builder()
            .level(ErrorDiagnostics.Level.DETAILED)
            .errorCode(String.valueOf(errorCode))
            .description(panamaException.getMessage())
            .collectionTimestamp(System.currentTimeMillis());

    // Add runtime metrics
    final RuntimeMetrics metrics = RuntimeMetrics.snapshot();
    diagnosticsBuilder.runtimeMetrics(metrics);

    // Add Panama-specific diagnostics
    diagnosticsBuilder.addProperty("panamaImplementation", "wasmtime4j-panama");
    diagnosticsBuilder.addProperty("foreignFunctionSupport", true);

    // Add recovery suggestions based on error type
    final List<String> suggestions = getRecoverySuggestions(errorCode);
    suggestions.forEach(diagnosticsBuilder::addSuggestedFix);

    return diagnosticsBuilder.build();
  }

  private static int extractNativeErrorCode(
      final PanamaException panamaException, final Optional<MemorySegment> errorContext) {
    // Try to extract from error context first
    final int contextErrorCode = safeReadErrorCode(errorContext);
    if (contextErrorCode != 0) {
      return contextErrorCode;
    }

    // Fall back to exception message parsing or default
    return -1; // Default to compilation error
  }

  private static String extractNativeErrorMessage(final Optional<MemorySegment> errorContext) {
    return safeReadErrorMessage(errorContext);
  }

  private static PanamaException createPanamaException(
      final int nativeErrorCode, final String nativeMessage, final String operation) {
    final String message =
        String.format(
            "Panama FFI error in %s: %s (code: %d)",
            operation != null ? operation : "unknown operation",
            nativeMessage != null ? nativeMessage : "unknown error",
            nativeErrorCode);

    return new PanamaException(message);
  }

  // Reuse the same mapping logic as JNI for consistency
  private static WasmError.Category mapErrorCategory(final int nativeErrorCode) {
    return switch (nativeErrorCode) {
      case -1 -> WasmError.Category.COMPILATION;
      case -2 -> WasmError.Category.COMPILATION; // Validation
      case -3 -> WasmError.Category.RUNTIME;
      case -4 -> WasmError.Category.ENGINE;
      case -5, -6 -> WasmError.Category.RUNTIME;
      case -7 -> WasmError.Category.MEMORY;
      case -8 -> WasmError.Category.FUNCTION;
      case -9 -> WasmError.Category.MODULE;
      case -10 -> WasmError.Category.COMPILATION; // Type error
      case -11 -> WasmError.Category.RESOURCE;
      case -12 -> WasmError.Category.IO;
      case -13 -> WasmError.Category.RUNTIME; // Invalid parameter
      case -14 -> WasmError.Category.RUNTIME; // Concurrency
      case -15 -> WasmError.Category.WASI;
      case -16, -17 -> WasmError.Category.MODULE; // Component/Interface
      case -18 -> WasmError.Category.RUNTIME; // Internal
      default -> WasmError.Category.UNKNOWN;
    };
  }

  private static WasmError.Severity mapErrorSeverity(final int nativeErrorCode) {
    return switch (nativeErrorCode) {
      case -1, -2 -> WasmError.Severity.ERROR; // Compilation/Validation
      case -3, -8 -> WasmError.Severity.ERROR; // Runtime/Function
      case -7, -11 -> WasmError.Severity.CRITICAL; // Memory/Resource
      case -18 -> WasmError.Severity.FATAL; // Internal error
      case -12 -> WasmError.Severity.WARNING; // I/O might be transient
      default -> WasmError.Severity.ERROR;
    };
  }

  private static boolean isRecoverable(final int nativeErrorCode) {
    return switch (nativeErrorCode) {
      case -12 -> true; // I/O errors might be transient
      case -13 -> true; // Invalid parameter can be corrected
      case -14 -> true; // Concurrency issues might be transient
      case -7, -11, -18 -> false; // Memory/Resource/Internal are serious
      default -> false;
    };
  }

  private static boolean shouldRetry(final int nativeErrorCode) {
    return switch (nativeErrorCode) {
      case -12 -> true; // I/O errors
      case -14 -> true; // Concurrency errors
      default -> false;
    };
  }

  private static Long calculateRetryDelay(final int nativeErrorCode) {
    return switch (nativeErrorCode) {
      case -12 -> 1000L; // I/O: 1 second
      case -14 -> 100L; // Concurrency: 100ms
      default -> null;
    };
  }

  private static List<String> getRecoverySuggestions(final int nativeErrorCode) {
    return switch (nativeErrorCode) {
      case -1 -> List.of("Check WebAssembly module validity", "Verify bytecode format");
      case -2 -> List.of("Validate module structure", "Check imports/exports");
      case -7 -> List.of("Check memory bounds", "Increase memory limits");
      case -11 -> List.of("Release unused resources", "Check resource limits");
      case -12 -> List.of("Retry operation", "Check network connectivity");
      case -13 -> List.of("Validate input parameters", "Check parameter types");
      case -14 -> List.of("Retry with backoff", "Check thread safety");
      default -> List.of("Check error logs", "Consult documentation");
    };
  }

  private static String generateErrorId() {
    return String.format("PANAMA-%016d", ERROR_COUNTER.incrementAndGet());
  }

  private PanamaErrorHandler() {
    // Utility class
  }
}
