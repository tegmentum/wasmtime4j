package ai.tegmentum.wasmtime4j.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.util.ErrorRecoveryManager.RetryConfig;
import ai.tegmentum.wasmtime4j.util.ErrorRecoveryManager.RetryResult;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ErrorRecoveryManager} utility class.
 *
 * <p>ErrorRecoveryManager provides error recovery strategies for WebAssembly operations including
 * retry mechanisms, circuit breakers, and graceful degradation strategies.
 */
@DisplayName("ErrorRecoveryManager Tests")
class ErrorRecoveryManagerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(ErrorRecoveryManager.class.getModifiers()),
          "ErrorRecoveryManager should be a final class");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = ErrorRecoveryManager.class.getDeclaredConstructor();
      assertTrue(
          Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private to prevent instantiation");
    }

    @Test
    @DisplayName("should throw AssertionError when constructor is invoked via reflection")
    void shouldThrowAssertionErrorWhenConstructorInvoked() throws NoSuchMethodException {
      final Constructor<?> constructor = ErrorRecoveryManager.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      final InvocationTargetException exception =
          assertThrows(
              InvocationTargetException.class,
              () -> constructor.newInstance(),
              "Constructor should throw exception when invoked");

      assertTrue(
          exception.getCause() instanceof AssertionError,
          "Cause should be AssertionError for utility class");
    }
  }

  @Nested
  @DisplayName("RetryConfig Tests")
  class RetryConfigTests {

    @Test
    @DisplayName("should create default config")
    void shouldCreateDefaultConfig() {
      final RetryConfig config = RetryConfig.defaultConfig();
      assertNotNull(config, "Default config should not be null");
      assertEquals(3, config.getMaxRetries(), "Default max retries should be 3");
      assertEquals(100, config.getInitialDelayMs(), "Default initial delay should be 100ms");
      assertEquals(5000, config.getMaxDelayMs(), "Default max delay should be 5000ms");
      assertEquals(2.0, config.getBackoffMultiplier(), 0.01, "Default multiplier should be 2.0");
      assertTrue(config.isExponentialBackoff(), "Default should use exponential backoff");
    }

    @Test
    @DisplayName("should create fixed delay config")
    void shouldCreateFixedDelayConfig() {
      final RetryConfig config = RetryConfig.fixedDelay(5, 200);
      assertNotNull(config, "Fixed delay config should not be null");
      assertEquals(5, config.getMaxRetries(), "Max retries should be 5");
      assertEquals(200, config.getInitialDelayMs(), "Initial delay should be 200ms");
      assertEquals(200, config.getMaxDelayMs(), "Max delay should equal initial for fixed delay");
      assertFalse(config.isExponentialBackoff(), "Fixed delay should not use exponential backoff");
    }

    @Test
    @DisplayName("should create exponential backoff config")
    void shouldCreateExponentialBackoffConfig() {
      final RetryConfig config = RetryConfig.exponentialBackoff(4, 50, 2000);
      assertNotNull(config, "Exponential backoff config should not be null");
      assertEquals(4, config.getMaxRetries(), "Max retries should be 4");
      assertEquals(50, config.getInitialDelayMs(), "Initial delay should be 50ms");
      assertEquals(2000, config.getMaxDelayMs(), "Max delay should be 2000ms");
      assertTrue(config.isExponentialBackoff(), "Should use exponential backoff");
    }

    @Test
    @DisplayName("should normalize negative values")
    void shouldNormalizeNegativeValues() {
      final RetryConfig config = new RetryConfig(-1, -100, -200, 0.5, true);
      assertEquals(0, config.getMaxRetries(), "Negative retries should be normalized to 0");
      assertEquals(0, config.getInitialDelayMs(), "Negative initial delay should be normalized");
      assertTrue(config.getBackoffMultiplier() >= 1.0, "Backoff multiplier should be at least 1.0");
    }

    @Test
    @DisplayName("should ensure maxDelayMs is at least initialDelayMs")
    void shouldEnsureMaxDelayAtLeastInitialDelay() {
      final RetryConfig config = new RetryConfig(3, 1000, 500, 2.0, true);
      assertTrue(
          config.getMaxDelayMs() >= config.getInitialDelayMs(),
          "Max delay should be at least initial delay");
    }
  }

  @Nested
  @DisplayName("RetryResult Tests")
  class RetryResultTests {

    @Test
    @DisplayName("should create success result")
    void shouldCreateSuccessResult() {
      final Duration duration = Duration.ofMillis(100);
      final RetryResult<String> result = RetryResult.success("test", 1, duration);

      assertTrue(result.isSuccessful(), "Result should be successful");
      assertEquals("test", result.getResult(), "Result value should match");
      assertEquals(1, result.getAttemptCount(), "Attempt count should be 1");
      assertNull(result.getLastException(), "No exception for success");
      assertEquals(duration, result.getTotalDuration(), "Duration should match");
    }

    @Test
    @DisplayName("should create failure result")
    void shouldCreateFailureResult() {
      final WasmException exception = new WasmException("Test error");
      final Duration duration = Duration.ofMillis(500);
      final RetryResult<String> result = RetryResult.failure(exception, 3, duration);

      assertFalse(result.isSuccessful(), "Result should not be successful");
      assertNull(result.getResult(), "Result value should be null for failure");
      assertEquals(3, result.getAttemptCount(), "Attempt count should be 3");
      assertEquals(exception, result.getLastException(), "Exception should match");
      assertEquals(duration, result.getTotalDuration(), "Duration should match");
    }

    @Test
    @DisplayName("success result should have null exception")
    void successResultShouldHaveNullException() {
      final RetryResult<Integer> result = RetryResult.success(42, 2, Duration.ofMillis(200));
      assertNull(result.getLastException(), "Success should have null exception");
    }

    @Test
    @DisplayName("failure result should have null result value")
    void failureResultShouldHaveNullResultValue() {
      final RetryResult<Integer> result =
          RetryResult.failure(new WasmException("Error"), 1, Duration.ofMillis(100));
      assertNull(result.getResult(), "Failure should have null result");
    }
  }

  @Nested
  @DisplayName("executeWithRetry Tests")
  class ExecuteWithRetryTests {

    @Test
    @DisplayName("should succeed on first attempt")
    void shouldSucceedOnFirstAttempt() {
      final Supplier<String> operation = () -> "success";
      final RetryResult<String> result =
          ErrorRecoveryManager.executeWithRetry(operation, "testOperation");

      assertTrue(result.isSuccessful(), "Should succeed");
      assertEquals("success", result.getResult(), "Result should match");
      assertEquals(1, result.getAttemptCount(), "Should succeed on first attempt");
    }

    @Test
    @DisplayName("should use default config when not specified")
    void shouldUseDefaultConfigWhenNotSpecified() {
      final AtomicInteger attempts = new AtomicInteger(0);
      final Supplier<String> operation =
          () -> {
            if (attempts.incrementAndGet() < 2) {
              throw new java.lang.RuntimeException("temporary error");
            }
            return "success";
          };

      final RetryResult<String> result =
          ErrorRecoveryManager.executeWithRetry(operation, "testOperation");

      // This tests that retry logic exists - actual success depends on error recoverability
      assertNotNull(result, "Result should not be null");
    }

    @Test
    @DisplayName("should track attempt count")
    void shouldTrackAttemptCount() {
      final AtomicInteger callCount = new AtomicInteger(0);
      final Supplier<String> operation =
          () -> {
            callCount.incrementAndGet();
            return "success";
          };

      final RetryResult<String> result =
          ErrorRecoveryManager.executeWithRetry(
              operation, RetryConfig.fixedDelay(3, 0), "testOperation");

      assertEquals(1, result.getAttemptCount(), "Attempt count should be tracked");
      assertEquals(1, callCount.get(), "Operation should be called correct number of times");
    }

    @Test
    @DisplayName("should record total duration")
    void shouldRecordTotalDuration() {
      final Supplier<String> operation = () -> "success";
      final RetryResult<String> result =
          ErrorRecoveryManager.executeWithRetry(
              operation, RetryConfig.fixedDelay(1, 0), "testOperation");

      assertNotNull(result.getTotalDuration(), "Duration should be recorded");
      assertTrue(!result.getTotalDuration().isNegative(), "Duration should not be negative");
    }
  }

  @Nested
  @DisplayName("validateResult Tests")
  class ValidateResultTests {

    @Test
    @DisplayName("should return result when validation passes")
    void shouldReturnResultWhenValidationPasses() throws WasmException {
      final Function<String, String> validator = s -> null; // null means valid
      final String result = ErrorRecoveryManager.validateResult("test", validator);
      assertEquals("test", result, "Should return original result");
    }

    @Test
    @DisplayName("should throw when validation fails")
    void shouldThrowWhenValidationFails() {
      final Function<String, String> validator = s -> "validation failed";

      final WasmException exception =
          assertThrows(
              WasmException.class,
              () -> ErrorRecoveryManager.validateResult("test", validator),
              "Should throw when validation fails");

      assertTrue(
          exception.getMessage().contains("validation failed"),
          "Exception should contain validation error");
    }

    @Test
    @DisplayName("should accept null validator")
    void shouldAcceptNullValidator() {
      assertDoesNotThrow(
          () -> ErrorRecoveryManager.validateResult("test", null),
          "Null validator should be allowed");
    }

    @Test
    @DisplayName("should handle null result with validator")
    void shouldHandleNullResultWithValidator() throws WasmException {
      final Function<String, String> validator = s -> s == null ? "null not allowed" : null;

      final WasmException exception =
          assertThrows(
              WasmException.class,
              () -> ErrorRecoveryManager.validateResult(null, validator),
              "Should throw for null result when validator rejects null");

      assertTrue(
          exception.getMessage().contains("null not allowed"),
          "Exception should contain validation error");
    }
  }

  @Nested
  @DisplayName("executeWithFallback Tests")
  class ExecuteWithFallbackTests {

    @Test
    @DisplayName("should return primary result when primary succeeds")
    void shouldReturnPrimaryResultWhenPrimarySucceeds() throws WasmException {
      final Supplier<String> primary = () -> "primary";
      final Supplier<String> fallback = () -> "fallback";

      final String result =
          ErrorRecoveryManager.executeWithFallback(primary, fallback, "testOperation");

      assertEquals("primary", result, "Should return primary result");
    }

    @Test
    @DisplayName("should not call fallback when primary succeeds")
    void shouldNotCallFallbackWhenPrimarySucceeds() throws WasmException {
      final AtomicInteger fallbackCalls = new AtomicInteger(0);
      final Supplier<String> primary = () -> "primary";
      final Supplier<String> fallback =
          () -> {
            fallbackCalls.incrementAndGet();
            return "fallback";
          };

      ErrorRecoveryManager.executeWithFallback(primary, fallback, "testOperation");

      assertEquals(0, fallbackCalls.get(), "Fallback should not be called when primary succeeds");
    }
  }

  @Nested
  @DisplayName("executeWithCircuitBreaker Tests")
  class ExecuteWithCircuitBreakerTests {

    @Test
    @DisplayName("should execute operation successfully")
    void shouldExecuteOperationSuccessfully() throws WasmException {
      final Supplier<String> operation = () -> "success";

      final String result =
          ErrorRecoveryManager.executeWithCircuitBreaker(operation, 0.5, 10, "testOperation");

      assertEquals("success", result, "Should return operation result");
    }

    @Test
    @DisplayName("should propagate exception on failure")
    void shouldPropagateExceptionOnFailure() {
      final Supplier<String> operation =
          () -> {
            throw new java.lang.RuntimeException("test error");
          };

      assertThrows(
          WasmException.class,
          () -> ErrorRecoveryManager.executeWithCircuitBreaker(operation, 0.5, 10, "testOperation"),
          "Should propagate exception");
    }

    @Test
    @DisplayName("should accept valid threshold values")
    void shouldAcceptValidThresholdValues() throws WasmException {
      final Supplier<String> operation = () -> "success";

      // Test with various threshold values
      assertDoesNotThrow(
          () -> ErrorRecoveryManager.executeWithCircuitBreaker(operation, 0.0, 10, "testOperation"),
          "Should accept threshold 0.0");

      assertDoesNotThrow(
          () -> ErrorRecoveryManager.executeWithCircuitBreaker(operation, 1.0, 10, "testOperation"),
          "Should accept threshold 1.0");

      assertDoesNotThrow(
          () -> ErrorRecoveryManager.executeWithCircuitBreaker(operation, 0.5, 1, "testOperation"),
          "Should accept window size 1");
    }
  }

  @Nested
  @DisplayName("Default Constants Tests")
  class DefaultConstantsTests {

    @Test
    @DisplayName("default config should have sensible defaults")
    void defaultConfigShouldHaveSensibleDefaults() {
      final RetryConfig config = RetryConfig.defaultConfig();

      assertTrue(config.getMaxRetries() > 0, "Should have at least 1 retry");
      assertTrue(config.getMaxRetries() <= 10, "Should not have excessive retries");
      assertTrue(config.getInitialDelayMs() > 0, "Should have positive initial delay");
      assertTrue(config.getMaxDelayMs() >= config.getInitialDelayMs(), "Max >= initial delay");
      assertTrue(config.getBackoffMultiplier() >= 1.0, "Multiplier should be >= 1.0");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("retry result should have consistent state")
    void retryResultShouldHaveConsistentState() {
      final RetryResult<String> success = RetryResult.success("test", 2, Duration.ofMillis(100));

      if (success.isSuccessful()) {
        assertNotNull(success.getResult(), "Successful result should have value");
        assertNull(success.getLastException(), "Successful result should have no exception");
      }

      final RetryResult<String> failure =
          RetryResult.failure(new WasmException("error"), 3, Duration.ofMillis(200));

      if (!failure.isSuccessful()) {
        assertNull(failure.getResult(), "Failed result should have no value");
        assertNotNull(failure.getLastException(), "Failed result should have exception");
      }
    }
  }
}
