package ai.tegmentum.wasmtime4j.jni.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link JniValidationException}. */
class JniValidationExceptionTest {

  @Test
  void testConstructorWithMessage() {
    final String message = "Validation failed";
    final JniValidationException exception = new JniValidationException(message);

    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getParameterName()).isNull();
    assertThat(exception.getParameterValue()).isNull();
    assertFalse(exception.hasParameterDetails());
  }

  @Test
  void testConstructorWithParameterDetails() {
    final String message = "Parameter validation failed";
    final String parameterName = "testParam";
    final Object parameterValue = "invalidValue";
    final JniValidationException exception =
        new JniValidationException(message, parameterName, parameterValue);

    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getParameterName()).isEqualTo(parameterName);
    assertThat(exception.getParameterValue()).isEqualTo(parameterValue);
    assertTrue(exception.hasParameterDetails());
  }

  @Test
  void testToStringWithoutParameterDetails() {
    final String message = "Validation failed";
    final JniValidationException exception = new JniValidationException(message);
    final String toString = exception.toString();

    assertThat(toString).contains(message);
    assertThat(toString).contains("JniValidationException");
    assertThat(toString).doesNotContain("parameter:");
  }

  @Test
  void testToStringWithParameterDetails() {
    final String message = "Validation failed";
    final String parameterName = "testParam";
    final String parameterValue = "badValue";
    final JniValidationException exception =
        new JniValidationException(message, parameterName, parameterValue);
    final String toString = exception.toString();

    assertThat(toString).contains(message);
    assertThat(toString).contains("JniValidationException");
    assertThat(toString).contains("parameter: testParam = badValue");
  }

  @Test
  void testParameterDetailsWithNullValue() {
    final String message = "Validation failed";
    final String parameterName = "nullParam";
    final Object parameterValue = null;
    final JniValidationException exception =
        new JniValidationException(message, parameterName, parameterValue);

    assertThat(exception.getParameterName()).isEqualTo(parameterName);
    assertThat(exception.getParameterValue()).isNull();
    assertTrue(exception.hasParameterDetails());

    final String toString = exception.toString();
    assertThat(toString).contains("parameter: nullParam = null");
  }

  @Test
  void testInheritanceFromRuntimeException() {
    final JniValidationException exception = new JniValidationException("test");
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  void testSerialVersionUid() {
    // Ensure serialVersionUID is defined for serialization compatibility
    assertThat(JniValidationException.class.getDeclaredFields())
        .anyMatch(field -> field.getName().equals("serialVersionUID"));
  }
}
