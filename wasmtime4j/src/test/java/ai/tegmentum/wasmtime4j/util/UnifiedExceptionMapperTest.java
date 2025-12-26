package ai.tegmentum.wasmtime4j.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.InstantiationException;
import ai.tegmentum.wasmtime4j.exception.ResourceException;
import ai.tegmentum.wasmtime4j.exception.RuntimeException;
import ai.tegmentum.wasmtime4j.exception.SecurityException;
import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasiComponentException;
import ai.tegmentum.wasmtime4j.exception.WasiConfigurationException;
import ai.tegmentum.wasmtime4j.exception.WasiException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UnifiedExceptionMapper} utility class.
 *
 * <p>UnifiedExceptionMapper converts implementation-specific exceptions to public API exceptions.
 * It ensures consistent error handling across different runtime implementations while preserving
 * stack trace information and error context.
 */
@DisplayName("UnifiedExceptionMapper Tests")
class UnifiedExceptionMapperTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(UnifiedExceptionMapper.class.getModifiers()),
          "UnifiedExceptionMapper should be a final class");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = UnifiedExceptionMapper.class.getDeclaredConstructor();
      assertTrue(
          Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private to prevent instantiation");
    }

    @Test
    @DisplayName("should throw AssertionError when constructor is invoked via reflection")
    void shouldThrowAssertionErrorWhenConstructorInvoked() throws NoSuchMethodException {
      final Constructor<?> constructor = UnifiedExceptionMapper.class.getDeclaredConstructor();
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
  @DisplayName("mapToPublicException Tests")
  class MapToPublicExceptionTests {

    @Test
    @DisplayName("should return WasmException for null input")
    void shouldReturnWasmExceptionForNull() {
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(null);
      assertNotNull(result, "Should return exception for null input");
      assertTrue(
          result.getMessage().contains("Unknown error"), "Message should indicate unknown error");
    }

    @Test
    @DisplayName("should return same exception if already WasmException")
    void shouldReturnSameExceptionIfAlreadyWasmException() {
      final WasmException original = new WasmException("test message");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertEquals(original, result, "Should return same WasmException");
    }

    @Test
    @DisplayName("should map IllegalArgumentException to ValidationException")
    void shouldMapIllegalArgumentExceptionToValidationException() {
      final IllegalArgumentException original = new IllegalArgumentException("invalid input");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(result instanceof ValidationException, "Should map to ValidationException");
      assertTrue(result.getMessage().contains("invalid input"), "Should preserve original message");
    }

    @Test
    @DisplayName("should map IllegalStateException to RuntimeException")
    void shouldMapIllegalStateExceptionToRuntimeException() {
      final IllegalStateException original = new IllegalStateException("bad state");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(result instanceof RuntimeException, "Should map to RuntimeException");
    }

    @Test
    @DisplayName("should map OutOfMemoryError to ResourceException")
    void shouldMapOutOfMemoryErrorToResourceException() {
      final OutOfMemoryError original = new OutOfMemoryError("heap space");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(result instanceof ResourceException, "Should map to ResourceException");
    }

    @Test
    @DisplayName("should map SecurityException to SecurityException")
    void shouldMapSecurityExceptionToSecurityException() {
      final java.lang.SecurityException original = new java.lang.SecurityException("access denied");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(result instanceof SecurityException, "Should map to SecurityException");
    }

    @Test
    @DisplayName("should map NullPointerException to ValidationException")
    void shouldMapNullPointerExceptionToValidationException() {
      final NullPointerException original = new NullPointerException("null value");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(result instanceof ValidationException, "Should map to ValidationException");
    }

    @Test
    @DisplayName("should map IndexOutOfBoundsException to ValidationException")
    void shouldMapIndexOutOfBoundsExceptionToValidationException() {
      final IndexOutOfBoundsException original = new IndexOutOfBoundsException("index 10");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(result instanceof ValidationException, "Should map to ValidationException");
    }

    @Test
    @DisplayName("should map ClassNotFoundException to ResourceException")
    void shouldMapClassNotFoundExceptionToResourceException() {
      final ClassNotFoundException original = new ClassNotFoundException("SomeClass");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(result instanceof ResourceException, "Should map to ResourceException");
    }
  }

  @Nested
  @DisplayName("Message Content Mapping Tests")
  class MessageContentMappingTests {

    @Test
    @DisplayName("should map compilation-related messages to CompilationException")
    void shouldMapCompilationMessages() {
      final java.lang.RuntimeException original =
          new java.lang.RuntimeException("Module compilation failed");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(
          result instanceof CompilationException,
          "Should map compilation message to CompilationException");
    }

    @Test
    @DisplayName("should map validation-related messages to ValidationException")
    void shouldMapValidationMessages() {
      final java.lang.RuntimeException original =
          new java.lang.RuntimeException("Invalid parameter");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(
          result instanceof ValidationException,
          "Should map validation message to ValidationException");
    }

    @Test
    @DisplayName("should map runtime-related messages to RuntimeException")
    void shouldMapRuntimeMessages() {
      final java.lang.RuntimeException original =
          new java.lang.RuntimeException("Runtime execution trap");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(
          result instanceof RuntimeException, "Should map runtime message to RuntimeException");
    }

    @Test
    @DisplayName("should map memory-related messages to ResourceException")
    void shouldMapMemoryMessages() {
      final java.lang.RuntimeException original =
          new java.lang.RuntimeException("Memory allocation failed");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(
          result instanceof ResourceException, "Should map memory message to ResourceException");
    }

    @Test
    @DisplayName("should map instantiation-related messages to InstantiationException")
    void shouldMapInstantiationMessages() {
      final java.lang.RuntimeException original =
          new java.lang.RuntimeException("Failed to instantiate module");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(
          result instanceof InstantiationException,
          "Should map instantiation message to InstantiationException");
    }

    @Test
    @DisplayName("should map WASI component messages to WasiComponentException")
    void shouldMapWasiComponentMessages() {
      final java.lang.RuntimeException original =
          new java.lang.RuntimeException("WASI component error");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(
          result instanceof WasiComponentException,
          "Should map WASI component message to WasiComponentException");
    }

    @Test
    @DisplayName("should map WASI config messages to WasiConfigurationException")
    void shouldMapWasiConfigMessages() {
      final java.lang.RuntimeException original =
          new java.lang.RuntimeException("WASI configuration error");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(
          result instanceof WasiConfigurationException,
          "Should map WASI config message to WasiConfigurationException");
    }

    @Test
    @DisplayName("should map WASI resource messages to ResourceException")
    void shouldMapWasiResourceMessages() {
      // Note: "WASI resource error" contains "resource" which is checked BEFORE "wasi"
      // in mapByMessageContent, so it maps to ResourceException, not WasiResourceException
      final java.lang.RuntimeException original =
          new java.lang.RuntimeException("WASI resource error");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(
          result instanceof ResourceException,
          "Should map message containing 'resource' to ResourceException");
    }

    @Test
    @DisplayName("should map generic WASI messages to WasiException")
    void shouldMapWasiMessages() {
      final java.lang.RuntimeException original =
          new java.lang.RuntimeException("WASI file system error");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(result instanceof WasiException, "Should map WASI message to WasiException");
    }

    @Test
    @DisplayName("should map security-related messages to SecurityException")
    void shouldMapSecurityMessages() {
      final java.lang.RuntimeException original =
          new java.lang.RuntimeException("Security permission denied");
      final WasmException result = UnifiedExceptionMapper.mapToPublicException(original);
      assertTrue(
          result instanceof SecurityException, "Should map security message to SecurityException");
    }
  }

  @Nested
  @DisplayName("isRecoverableError Tests")
  class IsRecoverableErrorTests {

    @Test
    @DisplayName("should return false for null exception")
    void shouldReturnFalseForNull() {
      assertFalse(
          UnifiedExceptionMapper.isRecoverableError(null),
          "Null exception should not be recoverable");
    }

    @Test
    @DisplayName("should return false for ValidationException")
    void shouldReturnFalseForValidationException() {
      final ValidationException exception = new ValidationException("validation error");
      assertFalse(
          UnifiedExceptionMapper.isRecoverableError(exception),
          "ValidationException should not be recoverable");
    }

    @Test
    @DisplayName("should return false for CompilationException")
    void shouldReturnFalseForCompilationException() {
      final CompilationException exception = new CompilationException("compilation error");
      assertFalse(
          UnifiedExceptionMapper.isRecoverableError(exception),
          "CompilationException should not be recoverable");
    }

    @Test
    @DisplayName("should return true for RuntimeException")
    void shouldReturnTrueForRuntimeException() {
      final RuntimeException exception = new RuntimeException("runtime error");
      assertTrue(
          UnifiedExceptionMapper.isRecoverableError(exception),
          "RuntimeException might be recoverable");
    }

    @Test
    @DisplayName("should return true for ResourceException")
    void shouldReturnTrueForResourceException() {
      final ResourceException exception = new ResourceException("resource error");
      assertTrue(
          UnifiedExceptionMapper.isRecoverableError(exception),
          "ResourceException might be recoverable");
    }

    @Test
    @DisplayName("should return true for 'not found' messages")
    void shouldReturnTrueForNotFoundMessages() {
      final WasmException exception = new WasmException("resource not found");
      assertTrue(
          UnifiedExceptionMapper.isRecoverableError(exception),
          "Not found error might be recoverable");
    }

    @Test
    @DisplayName("should return true for 'timeout' messages")
    void shouldReturnTrueForTimeoutMessages() {
      final WasmException exception = new WasmException("operation timeout");
      assertTrue(
          UnifiedExceptionMapper.isRecoverableError(exception),
          "Timeout error might be recoverable");
    }

    @Test
    @DisplayName("should return false for 'out of memory' messages")
    void shouldReturnFalseForOutOfMemoryMessages() {
      final WasmException exception = new WasmException("out of memory error");
      assertFalse(
          UnifiedExceptionMapper.isRecoverableError(exception),
          "Out of memory error should not be recoverable");
    }

    @Test
    @DisplayName("should return false for 'corruption' messages")
    void shouldReturnFalseForCorruptionMessages() {
      final WasmException exception = new WasmException("data corruption detected");
      assertFalse(
          UnifiedExceptionMapper.isRecoverableError(exception),
          "Corruption error should not be recoverable");
    }
  }

  @Nested
  @DisplayName("createContextualErrorMessage Tests")
  class CreateContextualErrorMessageTests {

    @Test
    @DisplayName("should create message with operation name")
    void shouldCreateMessageWithOperationName() {
      final String message =
          UnifiedExceptionMapper.createContextualErrorMessage("compile", null, "syntax error");
      assertTrue(message.contains("compile"), "Message should contain operation name");
      assertTrue(message.contains("syntax error"), "Message should contain original message");
    }

    @Test
    @DisplayName("should create message with context")
    void shouldCreateMessageWithContext() {
      final String message =
          UnifiedExceptionMapper.createContextualErrorMessage(
              "compile", "module.wasm", "invalid bytes");
      assertTrue(message.contains("compile"), "Should contain operation");
      assertTrue(message.contains("module.wasm"), "Should contain context");
      assertTrue(message.contains("invalid bytes"), "Should contain original message");
    }

    @Test
    @DisplayName("should handle null operation name")
    void shouldHandleNullOperationName() {
      final String message =
          UnifiedExceptionMapper.createContextualErrorMessage(null, null, "error");
      assertNotNull(message, "Should return message even with null operation");
      assertTrue(
          message.contains("WebAssembly operation"), "Should use default operation description");
    }

    @Test
    @DisplayName("should handle null original message")
    void shouldHandleNullOriginalMessage() {
      final String message =
          UnifiedExceptionMapper.createContextualErrorMessage("compile", null, null);
      assertNotNull(message, "Should return message even with null original");
      assertTrue(message.contains("compile"), "Should contain operation name");
    }

    @Test
    @DisplayName("should handle empty strings")
    void shouldHandleEmptyStrings() {
      final String message = UnifiedExceptionMapper.createContextualErrorMessage("", "  ", "  ");
      assertNotNull(message, "Should return message for empty strings");
    }
  }

  @Nested
  @DisplayName("wrapWithContext Tests")
  class WrapWithContextTests {

    @Test
    @DisplayName("should wrap exception with context")
    void shouldWrapExceptionWithContext() {
      final java.lang.RuntimeException original = new java.lang.RuntimeException("error");
      final WasmException wrapped =
          UnifiedExceptionMapper.wrapWithContext("test", "context", original);

      assertNotNull(wrapped, "Wrapped exception should not be null");
      assertTrue(wrapped.getMessage().contains("test"), "Should contain operation name");
      assertTrue(wrapped.getMessage().contains("context"), "Should contain context");
    }

    @Test
    @DisplayName("should preserve exception type when wrapping CompilationException")
    void shouldPreserveCompilationExceptionType() {
      final CompilationException original = new CompilationException("compile error");
      final WasmException wrapped =
          UnifiedExceptionMapper.wrapWithContext("compile", null, original);

      assertTrue(
          wrapped instanceof CompilationException, "Should preserve CompilationException type");
    }

    @Test
    @DisplayName("should preserve exception type when wrapping ValidationException")
    void shouldPreserveValidationExceptionType() {
      final ValidationException original = new ValidationException("validation error");
      final WasmException wrapped =
          UnifiedExceptionMapper.wrapWithContext("validate", null, original);

      assertTrue(
          wrapped instanceof ValidationException, "Should preserve ValidationException type");
    }

    @Test
    @DisplayName("should handle null original exception")
    void shouldHandleNullOriginalException() {
      final WasmException wrapped = UnifiedExceptionMapper.wrapWithContext("test", "context", null);

      assertNotNull(wrapped, "Should return wrapped exception even for null original");
    }
  }

  @Nested
  @DisplayName("Public Methods Tests")
  class PublicMethodsTests {

    @Test
    @DisplayName("should have mapToPublicException method")
    void shouldHaveMapToPublicExceptionMethod() throws NoSuchMethodException {
      final Method method =
          UnifiedExceptionMapper.class.getMethod("mapToPublicException", Throwable.class);
      assertNotNull(method, "mapToPublicException method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
      assertEquals(WasmException.class, method.getReturnType(), "Should return WasmException");
    }

    @Test
    @DisplayName("should have isRecoverableError method")
    void shouldHaveIsRecoverableErrorMethod() throws NoSuchMethodException {
      final Method method =
          UnifiedExceptionMapper.class.getMethod("isRecoverableError", WasmException.class);
      assertNotNull(method, "isRecoverableError method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have createContextualErrorMessage method")
    void shouldHaveCreateContextualErrorMessageMethod() throws NoSuchMethodException {
      final Method method =
          UnifiedExceptionMapper.class.getMethod(
              "createContextualErrorMessage", String.class, String.class, String.class);
      assertNotNull(method, "createContextualErrorMessage method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have wrapWithContext method")
    void shouldHaveWrapWithContextMethod() throws NoSuchMethodException {
      final Method method =
          UnifiedExceptionMapper.class.getMethod(
              "wrapWithContext", String.class, String.class, Throwable.class);
      assertNotNull(method, "wrapWithContext method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Method should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Method should be public");
      assertEquals(WasmException.class, method.getReturnType(), "Should return WasmException");
    }
  }
}
