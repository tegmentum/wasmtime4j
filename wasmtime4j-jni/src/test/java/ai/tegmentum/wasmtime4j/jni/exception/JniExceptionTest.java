package ai.tegmentum.wasmtime4j.jni.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link JniException}. */
class JniExceptionTest {

  @Test
  void testConstructorWithMessage() {
    final String message = "Test error message";
    final JniException exception = new JniException(message);

    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getNativeErrorCode()).isNull();
    assertFalse(exception.hasNativeErrorCode());
  }

  @Test
  void testConstructorWithMessageAndCause() {
    final String message = "Test error message";
    final RuntimeException cause = new RuntimeException("Root cause");
    final JniException exception = new JniException(message, cause);

    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
    assertThat(exception.getNativeErrorCode()).isNull();
    assertFalse(exception.hasNativeErrorCode());
  }

  @Test
  void testConstructorWithMessageAndNativeErrorCode() {
    final String message = "Test error message";
    final int errorCode = 42;
    final JniException exception = new JniException(message, errorCode);

    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isNull();
    assertThat(exception.getNativeErrorCode()).isEqualTo(errorCode);
    assertTrue(exception.hasNativeErrorCode());
  }

  @Test
  void testConstructorWithAllParameters() {
    final String message = "Test error message";
    final RuntimeException cause = new RuntimeException("Root cause");
    final int errorCode = 123;
    final JniException exception = new JniException(message, cause, errorCode);

    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
    assertThat(exception.getNativeErrorCode()).isEqualTo(errorCode);
    assertTrue(exception.hasNativeErrorCode());
  }

  @Test
  void testToStringWithoutNativeErrorCode() {
    final String message = "Test error message";
    final JniException exception = new JniException(message);
    final String toString = exception.toString();

    assertThat(toString).contains(message);
    assertThat(toString).contains("JniException");
    assertThat(toString).doesNotContain("native error code");
  }

  @Test
  void testToStringWithNativeErrorCode() {
    final String message = "Test error message";
    final int errorCode = 42;
    final JniException exception = new JniException(message, errorCode);
    final String toString = exception.toString();

    assertThat(toString).contains(message);
    assertThat(toString).contains("JniException");
    assertThat(toString).contains("native error code: 42");
  }

  @Test
  void testInheritanceFromRuntimeException() {
    final JniException exception = new JniException("test");
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  void testSerialVersionUid() {
    // Ensure serialVersionUID is defined for serialization compatibility
    assertThat(JniException.class.getDeclaredFields())
        .anyMatch(field -> field.getName().equals("serialVersionUID"));
  }
}
