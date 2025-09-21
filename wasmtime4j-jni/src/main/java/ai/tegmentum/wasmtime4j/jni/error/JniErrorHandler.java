package ai.tegmentum.wasmtime4j.jni.error;

import ai.tegmentum.wasmtime4j.diagnostics.ErrorContext;
import ai.tegmentum.wasmtime4j.diagnostics.ErrorDiagnostics;
import ai.tegmentum.wasmtime4j.diagnostics.RuntimeEnvironment;
import ai.tegmentum.wasmtime4j.diagnostics.RuntimeMetrics;
import ai.tegmentum.wasmtime4j.diagnostics.WasmError;
import ai.tegmentum.wasmtime4j.diagnostics.WasmStackTrace;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniExceptionMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced JNI error handler that provides comprehensive error diagnostics.
 *
 * <p>This class integrates the new error handling and diagnostics framework with
 * the existing JNI error mapping system to provide detailed error information.
 *
 * @since 1.0.0
 */
public final class JniErrorHandler {

    private static final Logger LOGGER = Logger.getLogger(JniErrorHandler.class.getName());
    private static final AtomicLong ERROR_COUNTER = new AtomicLong(0);

    // Native method declarations for enhanced error handling
    private static native String nativeGetLastErrorMessage(long contextPtr);
    private static native int nativeGetLastErrorCode(long contextPtr);
    private static native String nativeGetErrorContext(long contextPtr);
    private static native long nativeGetStackTrace(long contextPtr);
    private static native void nativeClearLastError(long contextPtr);

    /**
     * Creates a comprehensive WasmError from a JNI exception.
     *
     * @param jniException the JNI exception
     * @param contextPtr the native context pointer for additional error info
     * @return the comprehensive WebAssembly error
     */
    public static WasmError createWasmError(final JniException jniException, final long contextPtr) {
        final String errorId = generateErrorId();
        final WasmError.Category category = mapErrorCategory(jniException.getNativeErrorCode());
        final WasmError.Severity severity = mapErrorSeverity(jniException.getNativeErrorCode());

        final WasmError.Builder builder = WasmError.builder()
            .errorId(errorId)
            .message(jniException.getMessage())
            .category(category)
            .severity(severity)
            .timestamp(Instant.now())
            .cause(jniException);

        // Add enhanced diagnostics if context is available
        if (contextPtr != 0) {
            addEnhancedDiagnostics(builder, contextPtr, jniException);
        }

        // Add error context
        final ErrorContext errorContext = createErrorContext(jniException, contextPtr);
        builder.context(errorContext);

        // Add error diagnostics
        final ErrorDiagnostics diagnostics = createErrorDiagnostics(jniException, contextPtr);
        builder.diagnostics(diagnostics);

        // Determine if error is recoverable
        final boolean recoverable = isRecoverable(jniException.getNativeErrorCode());
        builder.recoverable(recoverable);

        // Add retry information if applicable
        if (shouldRetry(jniException.getNativeErrorCode())) {
            builder.shouldRetry(true);
            builder.retryDelayMs(calculateRetryDelay(jniException.getNativeErrorCode()));
        }

        return builder.build();
    }

    /**
     * Creates a comprehensive WasmError from a native error code.
     *
     * @param nativeErrorCode the native error code
     * @param contextPtr the native context pointer
     * @param operation the operation that failed
     * @return the comprehensive WebAssembly error
     */
    public static WasmError createWasmError(final int nativeErrorCode, final long contextPtr,
                                           final String operation) {
        // Create the underlying JNI exception first
        final String nativeMessage = contextPtr != 0 ? nativeGetLastErrorMessage(contextPtr) : null;
        final JniException jniException = JniExceptionMapper.mapNativeError(nativeErrorCode, nativeMessage);

        return createWasmError(jniException, contextPtr);
    }

    /**
     * Handles a native error by creating comprehensive diagnostics and throwing appropriate exception.
     *
     * @param nativeErrorCode the native error code
     * @param contextPtr the native context pointer
     * @param operation the operation that failed
     * @throws JniException always thrown with comprehensive error information
     */
    public static void handleNativeError(final int nativeErrorCode, final long contextPtr,
                                        final String operation) throws JniException {
        if (nativeErrorCode == 0) {
            return; // No error
        }

        final WasmError wasmError = createWasmError(nativeErrorCode, contextPtr, operation);

        // Log the error for debugging
        LOGGER.log(Level.SEVERE, "Native error occurred: {0}", wasmError.getMessage());
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Error diagnostics: {0}", wasmError.getDiagnostics().getDescription());
        }

        // Clear the native error after processing
        if (contextPtr != 0) {
            nativeClearLastError(contextPtr);
        }

        // Throw the underlying JNI exception
        throw (JniException) wasmError.getCause().orElseThrow(
            () -> new JniException("Failed to extract JNI exception from WasmError"));
    }

    /**
     * Validates a native handle and throws comprehensive error if invalid.
     *
     * @param handle the native handle to validate
     * @param resourceType the type of resource
     * @param contextPtr the native context pointer
     * @throws JniException if the handle is invalid
     */
    public static void validateNativeHandle(final long handle, final String resourceType,
                                           final long contextPtr) throws JniException {
        if (handle == 0) {
            final WasmError wasmError = WasmError.builder()
                .errorId(generateErrorId())
                .message(String.format("Invalid %s handle: null pointer", resourceType))
                .category(WasmError.Category.RESOURCE)
                .severity(WasmError.Severity.ERROR)
                .timestamp(Instant.now())
                .context(createErrorContext(null, contextPtr))
                .recoverable(false)
                .build();

            LOGGER.log(Level.WARNING, "Invalid native handle detected: {0}", resourceType);

            throw new JniException(wasmError.getMessage());
        }
    }

    private static void addEnhancedDiagnostics(final WasmError.Builder builder, final long contextPtr,
                                              final JniException jniException) {
        try {
            // Get additional error context from native layer
            final String nativeContext = nativeGetErrorContext(contextPtr);
            if (nativeContext != null && !nativeContext.isEmpty()) {
                builder.addProperty("nativeContext", nativeContext);
            }

            // Get stack trace if available
            final long stackTracePtr = nativeGetStackTrace(contextPtr);
            if (stackTracePtr != 0) {
                final WasmStackTrace stackTrace = createStackTrace(stackTracePtr);
                builder.stackTrace(stackTrace);
            }

        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to add enhanced diagnostics", e);
            builder.addProperty("diagnosticsError", e.getMessage());
        }
    }

    private static ErrorContext createErrorContext(final JniException jniException, final long contextPtr) {
        final ErrorContext.Builder contextBuilder = ErrorContext.builder()
            .runtimeEnvironment(RuntimeEnvironment.snapshot());

        // Add thread information
        contextBuilder.threadInfo(ai.tegmentum.wasmtime4j.diagnostics.ThreadInfo.current());

        // Add memory state
        contextBuilder.memoryState(ai.tegmentum.wasmtime4j.diagnostics.MemoryState.snapshot());

        // Add JNI-specific context
        if (jniException != null) {
            contextBuilder.addProperty("jniErrorCode", jniException.getNativeErrorCode());
            contextBuilder.addProperty("jniErrorType", jniException.getClass().getSimpleName());
        }

        if (contextPtr != 0) {
            contextBuilder.addProperty("nativeContextPtr", contextPtr);
        }

        return contextBuilder.build();
    }

    private static ErrorDiagnostics createErrorDiagnostics(final JniException jniException,
                                                           final long contextPtr) {
        final ErrorDiagnostics.Builder diagnosticsBuilder = ErrorDiagnostics.builder()
            .level(ErrorDiagnostics.Level.DETAILED)
            .errorCode(String.valueOf(jniException.getNativeErrorCode()))
            .description(jniException.getMessage())
            .collectionTimestamp(System.currentTimeMillis());

        // Add runtime metrics
        final RuntimeMetrics metrics = RuntimeMetrics.snapshot();
        diagnosticsBuilder.runtimeMetrics(metrics);

        // Add JNI-specific diagnostics
        diagnosticsBuilder.addProperty("jniImplementation", "wasmtime4j-jni");
        diagnosticsBuilder.addProperty("nativeLibraryLoaded", true);

        // Add recovery suggestions based on error type
        final List<String> suggestions = getRecoverySuggestions(jniException.getNativeErrorCode());
        suggestions.forEach(diagnosticsBuilder::addSuggestedFix);

        return diagnosticsBuilder.build();
    }

    private static WasmStackTrace createStackTrace(final long stackTracePtr) {
        // This would be implemented to convert native stack trace to Java representation
        // For now, return a placeholder
        return WasmStackTrace.builder()
            .addFrame(WasmStackTrace.FrameType.NATIVE, "native_frame", "unknown")
            .build();
    }

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
            case -14 -> 100L;  // Concurrency: 100ms
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
        return String.format("JNI-%016d", ERROR_COUNTER.incrementAndGet());
    }

    private JniErrorHandler() {
        // Utility class
    }
}