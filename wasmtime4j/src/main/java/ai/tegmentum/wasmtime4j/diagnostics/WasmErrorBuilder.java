package ai.tegmentum.wasmtime4j.diagnostics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builder for constructing WasmError instances.
 *
 * <p>This builder provides a fluent API for creating comprehensive WebAssembly error objects
 * with detailed diagnostics, context, and recovery information.
 *
 * @since 1.0.0
 */
public final class WasmErrorBuilder {

    private String errorId;
    private String message;
    private WasmError.Category category = WasmError.Category.UNKNOWN;
    private WasmError.Severity severity = WasmError.Severity.ERROR;
    private Instant timestamp = Instant.now();
    private ErrorContext context;
    private ErrorDiagnostics diagnostics;
    private WasmStackTrace stackTrace;
    private final List<String> recoverySuggestions = new ArrayList<>();
    private final Map<String, Object> properties = new HashMap<>();
    private Throwable cause;
    private boolean recoverable = false;
    private boolean securityViolation = false;
    private boolean shouldRetry = false;
    private Long retryDelayMs;

    /**
     * Sets the error ID.
     *
     * @param errorId the error ID
     * @return this builder
     */
    public WasmErrorBuilder errorId(final String errorId) {
        this.errorId = errorId;
        return this;
    }

    /**
     * Sets the error message.
     *
     * @param message the error message
     * @return this builder
     */
    public WasmErrorBuilder message(final String message) {
        this.message = message;
        return this;
    }

    /**
     * Sets the error category.
     *
     * @param category the error category
     * @return this builder
     */
    public WasmErrorBuilder category(final WasmError.Category category) {
        this.category = category;
        return this;
    }

    /**
     * Sets the error severity.
     *
     * @param severity the error severity
     * @return this builder
     */
    public WasmErrorBuilder severity(final WasmError.Severity severity) {
        this.severity = severity;
        return this;
    }

    /**
     * Sets the error timestamp.
     *
     * @param timestamp the error timestamp
     * @return this builder
     */
    public WasmErrorBuilder timestamp(final Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Sets the error context.
     *
     * @param context the error context
     * @return this builder
     */
    public WasmErrorBuilder context(final ErrorContext context) {
        this.context = context;
        return this;
    }

    /**
     * Sets the error diagnostics.
     *
     * @param diagnostics the error diagnostics
     * @return this builder
     */
    public WasmErrorBuilder diagnostics(final ErrorDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
        return this;
    }

    /**
     * Sets the stack trace.
     *
     * @param stackTrace the stack trace
     * @return this builder
     */
    public WasmErrorBuilder stackTrace(final WasmStackTrace stackTrace) {
        this.stackTrace = stackTrace;
        return this;
    }

    /**
     * Adds a recovery suggestion.
     *
     * @param suggestion the recovery suggestion
     * @return this builder
     */
    public WasmErrorBuilder addRecoverySuggestion(final String suggestion) {
        this.recoverySuggestions.add(suggestion);
        return this;
    }

    /**
     * Sets the recovery suggestions.
     *
     * @param suggestions the recovery suggestions
     * @return this builder
     */
    public WasmErrorBuilder recoverySuggestions(final List<String> suggestions) {
        this.recoverySuggestions.clear();
        this.recoverySuggestions.addAll(suggestions);
        return this;
    }

    /**
     * Adds a property.
     *
     * @param key the property key
     * @param value the property value
     * @return this builder
     */
    public WasmErrorBuilder addProperty(final String key, final Object value) {
        this.properties.put(key, value);
        return this;
    }

    /**
     * Sets the properties.
     *
     * @param properties the properties
     * @return this builder
     */
    public WasmErrorBuilder properties(final Map<String, Object> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
        return this;
    }

    /**
     * Sets the underlying cause.
     *
     * @param cause the underlying cause
     * @return this builder
     */
    public WasmErrorBuilder cause(final Throwable cause) {
        this.cause = cause;
        return this;
    }

    /**
     * Sets whether the error is recoverable.
     *
     * @param recoverable true if the error is recoverable
     * @return this builder
     */
    public WasmErrorBuilder recoverable(final boolean recoverable) {
        this.recoverable = recoverable;
        return this;
    }

    /**
     * Sets whether the error indicates a security violation.
     *
     * @param securityViolation true if the error indicates a security violation
     * @return this builder
     */
    public WasmErrorBuilder securityViolation(final boolean securityViolation) {
        this.securityViolation = securityViolation;
        return this;
    }

    /**
     * Sets whether the error should be retried.
     *
     * @param shouldRetry true if the error should be retried
     * @return this builder
     */
    public WasmErrorBuilder shouldRetry(final boolean shouldRetry) {
        this.shouldRetry = shouldRetry;
        return this;
    }

    /**
     * Sets the retry delay in milliseconds.
     *
     * @param retryDelayMs the retry delay in milliseconds
     * @return this builder
     */
    public WasmErrorBuilder retryDelayMs(final Long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
        return this;
    }

    /**
     * Builds the WasmError instance.
     *
     * @return the constructed WasmError
     * @throws IllegalStateException if required fields are missing
     */
    public WasmError build() {
        if (errorId == null) {
            throw new IllegalStateException("Error ID is required");
        }
        if (message == null) {
            throw new IllegalStateException("Error message is required");
        }

        return new WasmErrorImpl(
            errorId,
            message,
            category,
            severity,
            timestamp,
            context,
            diagnostics,
            stackTrace,
            new ArrayList<>(recoverySuggestions),
            new HashMap<>(properties),
            cause,
            recoverable,
            securityViolation,
            shouldRetry,
            retryDelayMs
        );
    }

    /**
     * Internal implementation of WasmError.
     */
    private static final class WasmErrorImpl implements WasmError {
        private final String errorId;
        private final String message;
        private final Category category;
        private final Severity severity;
        private final Instant timestamp;
        private final ErrorContext context;
        private final ErrorDiagnostics diagnostics;
        private final WasmStackTrace stackTrace;
        private final List<String> recoverySuggestions;
        private final Map<String, Object> properties;
        private final Throwable cause;
        private final boolean recoverable;
        private final boolean securityViolation;
        private final boolean shouldRetry;
        private final Long retryDelayMs;

        private WasmErrorImpl(final String errorId, final String message, final Category category,
                             final Severity severity, final Instant timestamp, final ErrorContext context,
                             final ErrorDiagnostics diagnostics, final WasmStackTrace stackTrace,
                             final List<String> recoverySuggestions, final Map<String, Object> properties,
                             final Throwable cause, final boolean recoverable, final boolean securityViolation,
                             final boolean shouldRetry, final Long retryDelayMs) {
            this.errorId = errorId;
            this.message = message;
            this.category = category;
            this.severity = severity;
            this.timestamp = timestamp;
            this.context = context;
            this.diagnostics = diagnostics;
            this.stackTrace = stackTrace;
            this.recoverySuggestions = recoverySuggestions;
            this.properties = properties;
            this.cause = cause;
            this.recoverable = recoverable;
            this.securityViolation = securityViolation;
            this.shouldRetry = shouldRetry;
            this.retryDelayMs = retryDelayMs;
        }

        @Override
        public String getErrorId() {
            return errorId;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public Category getCategory() {
            return category;
        }

        @Override
        public Severity getSeverity() {
            return severity;
        }

        @Override
        public Instant getTimestamp() {
            return timestamp;
        }

        @Override
        public ErrorContext getContext() {
            return context;
        }

        @Override
        public ErrorDiagnostics getDiagnostics() {
            return diagnostics;
        }

        @Override
        public Optional<WasmStackTrace> getStackTrace() {
            return Optional.ofNullable(stackTrace);
        }

        @Override
        public List<String> getRecoverySuggestions() {
            return new ArrayList<>(recoverySuggestions);
        }

        @Override
        public Map<String, Object> getProperties() {
            return new HashMap<>(properties);
        }

        @Override
        public Optional<Throwable> getCause() {
            return Optional.ofNullable(cause);
        }

        @Override
        public boolean isRecoverable() {
            return recoverable;
        }

        @Override
        public boolean isSecurityViolation() {
            return securityViolation;
        }

        @Override
        public boolean shouldRetry() {
            return shouldRetry;
        }

        @Override
        public Optional<Long> getRetryDelayMs() {
            return Optional.ofNullable(retryDelayMs);
        }
    }
}