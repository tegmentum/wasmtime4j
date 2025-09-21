package ai.tegmentum.wasmtime4j.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Comprehensive tests for WasmError functionality.
 */
class WasmErrorTest {

    @Test
    void testBasicErrorCreation() {
        final WasmError error = WasmError.builder()
            .errorId("TEST-001")
            .message("Test error message")
            .category(WasmError.Category.COMPILATION)
            .severity(WasmError.Severity.ERROR)
            .build();

        assertThat(error.getErrorId()).isEqualTo("TEST-001");
        assertThat(error.getMessage()).isEqualTo("Test error message");
        assertThat(error.getCategory()).isEqualTo(WasmError.Category.COMPILATION);
        assertThat(error.getSeverity()).isEqualTo(WasmError.Severity.ERROR);
        assertThat(error.getTimestamp()).isNotNull();
        assertThat(error.isRecoverable()).isFalse();
        assertThat(error.shouldRetry()).isFalse();
    }

    @Test
    void testErrorWithAllProperties() {
        final Instant timestamp = Instant.now();
        final ErrorContext context = createTestErrorContext();
        final ErrorDiagnostics diagnostics = createTestErrorDiagnostics();
        final RuntimeException cause = new RuntimeException("Original cause");

        final WasmError error = WasmError.builder()
            .errorId("TEST-002")
            .message("Complete error")
            .category(WasmError.Category.RUNTIME)
            .severity(WasmError.Severity.CRITICAL)
            .timestamp(timestamp)
            .context(context)
            .diagnostics(diagnostics)
            .cause(cause)
            .recoverable(true)
            .shouldRetry(true)
            .retryDelayMs(5000L)
            .addRecoverySuggestion("Try again later")
            .addProperty("customProperty", "customValue")
            .build();

        assertThat(error.getErrorId()).isEqualTo("TEST-002");
        assertThat(error.getMessage()).isEqualTo("Complete error");
        assertThat(error.getCategory()).isEqualTo(WasmError.Category.RUNTIME);
        assertThat(error.getSeverity()).isEqualTo(WasmError.Severity.CRITICAL);
        assertThat(error.getTimestamp()).isEqualTo(timestamp);
        assertThat(error.getContext()).isSameAs(context);
        assertThat(error.getDiagnostics()).isSameAs(diagnostics);
        assertThat(error.getCause()).contains(cause);
        assertThat(error.isRecoverable()).isTrue();
        assertThat(error.shouldRetry()).isTrue();
        assertThat(error.getRetryDelayMs()).contains(5000L);
        assertThat(error.getRecoverySuggestions()).contains("Try again later");
        assertThat(error.getProperties()).containsEntry("customProperty", "customValue");
    }

    @Test
    void testBuilderRequiredFields() {
        assertThatThrownBy(() ->
            WasmError.builder().build()
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("Error ID is required");

        assertThatThrownBy(() ->
            WasmError.builder()
                .errorId("TEST-003")
                .build()
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("Error message is required");
    }

    @ParameterizedTest
    @EnumSource(WasmError.Category.class)
    void testAllErrorCategories(final WasmError.Category category) {
        final WasmError error = WasmError.builder()
            .errorId("CAT-" + category.name())
            .message("Testing category " + category)
            .category(category)
            .build();

        assertThat(error.getCategory()).isEqualTo(category);
    }

    @ParameterizedTest
    @EnumSource(WasmError.Severity.class)
    void testAllErrorSeverities(final WasmError.Severity severity) {
        final WasmError error = WasmError.builder()
            .errorId("SEV-" + severity.name())
            .message("Testing severity " + severity)
            .severity(severity)
            .build();

        assertThat(error.getSeverity()).isEqualTo(severity);
    }

    @Test
    void testSecurityViolationHandling() {
        final WasmError securityError = WasmError.builder()
            .errorId("SEC-001")
            .message("Security violation detected")
            .category(WasmError.Category.SECURITY)
            .severity(WasmError.Severity.CRITICAL)
            .securityViolation(true)
            .build();

        assertThat(securityError.isSecurityViolation()).isTrue();
        assertThat(securityError.isRecoverable()).isFalse(); // Security violations shouldn't be recoverable
    }

    @Test
    void testErrorWithStackTrace() {
        final WasmStackTrace stackTrace = createTestStackTrace();

        final WasmError error = WasmError.builder()
            .errorId("STACK-001")
            .message("Error with stack trace")
            .stackTrace(stackTrace)
            .build();

        assertThat(error.getStackTrace()).contains(stackTrace);
    }

    @Test
    void testRetryConfiguration() {
        final WasmError retryableError = WasmError.builder()
            .errorId("RETRY-001")
            .message("Retryable error")
            .shouldRetry(true)
            .retryDelayMs(1500L)
            .build();

        assertThat(retryableError.shouldRetry()).isTrue();
        assertThat(retryableError.getRetryDelayMs()).contains(1500L);

        final WasmError nonRetryableError = WasmError.builder()
            .errorId("RETRY-002")
            .message("Non-retryable error")
            .shouldRetry(false)
            .build();

        assertThat(nonRetryableError.shouldRetry()).isFalse();
        assertThat(nonRetryableError.getRetryDelayMs()).isEmpty();
    }

    @Test
    void testRecoverySuggestions() {
        final WasmError error = WasmError.builder()
            .errorId("RECOVERY-001")
            .message("Error with suggestions")
            .addRecoverySuggestion("First suggestion")
            .addRecoverySuggestion("Second suggestion")
            .recoverySuggestions(List.of("Third suggestion", "Fourth suggestion"))
            .build();

        assertThat(error.getRecoverySuggestions())
            .hasSize(2) // recoverySuggestions() replaces previous ones
            .containsExactly("Third suggestion", "Fourth suggestion");
    }

    @Test
    void testCustomProperties() {
        final Map<String, Object> properties = Map.of(
            "moduleId", "test-module",
            "functionIndex", 42,
            "timestamp", System.currentTimeMillis()
        );

        final WasmError error = WasmError.builder()
            .errorId("PROPS-001")
            .message("Error with properties")
            .properties(properties)
            .addProperty("additionalProp", "additionalValue")
            .build();

        assertThat(error.getProperties())
            .containsAllEntriesOf(properties)
            .containsEntry("additionalProp", "additionalValue");
    }

    @Test
    void testImmutability() {
        final WasmError error = WasmError.builder()
            .errorId("IMMUT-001")
            .message("Immutable error")
            .addRecoverySuggestion("Original suggestion")
            .addProperty("original", "value")
            .build();

        // Test that returned collections are defensive copies
        final List<String> suggestions = error.getRecoverySuggestions();
        final Map<String, Object> properties = error.getProperties();

        // These should not affect the original error
        suggestions.add("Modified suggestion");
        properties.put("modified", "value");

        assertThat(error.getRecoverySuggestions())
            .hasSize(1)
            .containsExactly("Original suggestion");

        assertThat(error.getProperties())
            .hasSize(1)
            .containsOnlyKeys("original");
    }

    private ErrorContext createTestErrorContext() {
        return ErrorContext.builder()
            .moduleName("test-module")
            .functionName("test-function")
            .functionIndex(0)
            .instructionOffset(100L)
            .runtimeEnvironment(RuntimeEnvironment.snapshot())
            .build();
    }

    private ErrorDiagnostics createTestErrorDiagnostics() {
        return ErrorDiagnostics.builder()
            .errorCode("TEST-DIAG")
            .description("Test diagnostics")
            .level(ErrorDiagnostics.Level.BASIC)
            .build();
    }

    private WasmStackTrace createTestStackTrace() {
        return WasmStackTrace.builder()
            .addFrame(WasmStackTrace.FrameType.WASM, "test-module", "test-function")
            .addFrame(WasmStackTrace.FrameType.HOST, "host", "callback")
            .build();
    }
}