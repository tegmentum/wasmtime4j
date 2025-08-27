package ai.tegmentum.wasmtime4j.jni.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniValidation}.
 */
class JniValidationTest {

  @Test
  void testRequireNonNullWithValidObject() {
    final String validObject = "test";
    assertDoesNotThrow(() -> JniValidation.requireNonNull(validObject, "validObject"));
  }

  @Test
  void testRequireNonNullWithNullObject() {
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> JniValidation.requireNonNull(null, "nullParam"));

    assertThat(exception.getMessage()).contains("nullParam");
    assertThat(exception.getMessage()).contains("must not be null");
    assertThat(exception.getParameterName()).isEqualTo("nullParam");
    assertThat(exception.getParameterValue()).isNull();
  }

  @Test
  void testRequireNonEmptyStringWithValidString() {
    final String validString = "test";
    assertDoesNotThrow(() -> JniValidation.requireNonEmpty(validString, "validString"));
  }

  @Test
  void testRequireNonEmptyStringWithNullString() {
    assertThrows(JniValidationException.class,
        () -> JniValidation.requireNonEmpty((String) null, "nullString"));
  }

  @Test
  void testRequireNonEmptyStringWithEmptyString() {
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> JniValidation.requireNonEmpty("", "emptyString"));

    assertThat(exception.getMessage()).contains("emptyString");
    assertThat(exception.getMessage()).contains("must not be empty");
    assertThat(exception.getParameterName()).isEqualTo("emptyString");
    assertThat(exception.getParameterValue()).isEqualTo("");
  }

  @Test
  void testRequireNonEmptyArrayWithValidArray() {
    final byte[] validArray = {1, 2, 3};
    assertDoesNotThrow(() -> JniValidation.requireNonEmpty(validArray, "validArray"));
  }

  @Test
  void testRequireNonEmptyArrayWithNullArray() {
    assertThrows(JniValidationException.class,
        () -> JniValidation.requireNonEmpty((byte[]) null, "nullArray"));
  }

  @Test
  void testRequireNonEmptyArrayWithEmptyArray() {
    final byte[] emptyArray = {};
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> JniValidation.requireNonEmpty(emptyArray, "emptyArray"));

    assertThat(exception.getMessage()).contains("emptyArray");
    assertThat(exception.getMessage()).contains("must not be empty");
    assertThat(exception.getParameterName()).isEqualTo("emptyArray");
    assertThat(exception.getParameterValue()).isEqualTo("byte[0]");
  }

  @Test
  void testRequireInRangeIntWithValidValue() {
    assertDoesNotThrow(() -> JniValidation.requireInRange(5, 0, 10, "validInt"));
    assertDoesNotThrow(() -> JniValidation.requireInRange(0, 0, 10, "minBoundary"));
    assertDoesNotThrow(() -> JniValidation.requireInRange(10, 0, 10, "maxBoundary"));
  }

  @Test
  void testRequireInRangeIntWithInvalidValue() {
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> JniValidation.requireInRange(-1, 0, 10, "belowMin"));

    assertThat(exception.getMessage()).contains("belowMin");
    assertThat(exception.getMessage()).contains("must be in range [0, 10]");
    assertThat(exception.getMessage()).contains("got -1");
    assertThat(exception.getParameterName()).isEqualTo("belowMin");
    assertThat(exception.getParameterValue()).isEqualTo(-1);

    final JniValidationException exception2 = assertThrows(JniValidationException.class,
        () -> JniValidation.requireInRange(11, 0, 10, "aboveMax"));

    assertThat(exception2.getMessage()).contains("aboveMax");
    assertThat(exception2.getMessage()).contains("got 11");
  }

  @Test
  void testRequireInRangeLongWithValidValue() {
    assertDoesNotThrow(() -> JniValidation.requireInRange(5L, 0L, 10L, "validLong"));
    assertDoesNotThrow(() -> JniValidation.requireInRange(0L, 0L, 10L, "minBoundary"));
    assertDoesNotThrow(() -> JniValidation.requireInRange(10L, 0L, 10L, "maxBoundary"));
  }

  @Test
  void testRequireInRangeLongWithInvalidValue() {
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> JniValidation.requireInRange(-1L, 0L, 10L, "belowMin"));

    assertThat(exception.getMessage()).contains("belowMin");
    assertThat(exception.getMessage()).contains("must be in range [0, 10]");
    assertThat(exception.getMessage()).contains("got -1");
  }

  @Test
  void testRequirePositiveIntWithValidValue() {
    assertDoesNotThrow(() -> JniValidation.requirePositive(1, "positiveInt"));
    assertDoesNotThrow(() -> JniValidation.requirePositive(Integer.MAX_VALUE, "maxInt"));
  }

  @Test
  void testRequirePositiveIntWithInvalidValue() {
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> JniValidation.requirePositive(0, "zeroInt"));

    assertThat(exception.getMessage()).contains("zeroInt");
    assertThat(exception.getMessage()).contains("must be positive");
    assertThat(exception.getMessage()).contains("got 0");

    assertThrows(JniValidationException.class,
        () -> JniValidation.requirePositive(-1, "negativeInt"));
  }

  @Test
  void testRequirePositiveLongWithValidValue() {
    assertDoesNotThrow(() -> JniValidation.requirePositive(1L, "positiveLong"));
    assertDoesNotThrow(() -> JniValidation.requirePositive(Long.MAX_VALUE, "maxLong"));
  }

  @Test
  void testRequirePositiveLongWithInvalidValue() {
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> JniValidation.requirePositive(0L, "zeroLong"));

    assertThat(exception.getMessage()).contains("zeroLong");
    assertThat(exception.getMessage()).contains("must be positive");
    assertThat(exception.getMessage()).contains("got 0");

    assertThrows(JniValidationException.class,
        () -> JniValidation.requirePositive(-1L, "negativeLong"));
  }

  @Test
  void testRequireNonNegativeIntWithValidValue() {
    assertDoesNotThrow(() -> JniValidation.requireNonNegative(0, "zeroInt"));
    assertDoesNotThrow(() -> JniValidation.requireNonNegative(1, "positiveInt"));
    assertDoesNotThrow(() -> JniValidation.requireNonNegative(Integer.MAX_VALUE, "maxInt"));
  }

  @Test
  void testRequireNonNegativeIntWithInvalidValue() {
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> JniValidation.requireNonNegative(-1, "negativeInt"));

    assertThat(exception.getMessage()).contains("negativeInt");
    assertThat(exception.getMessage()).contains("must be non-negative");
    assertThat(exception.getMessage()).contains("got -1");
  }

  @Test
  void testRequireNonNegativeLongWithValidValue() {
    assertDoesNotThrow(() -> JniValidation.requireNonNegative(0L, "zeroLong"));
    assertDoesNotThrow(() -> JniValidation.requireNonNegative(1L, "positiveLong"));
    assertDoesNotThrow(() -> JniValidation.requireNonNegative(Long.MAX_VALUE, "maxLong"));
  }

  @Test
  void testRequireNonNegativeLongWithInvalidValue() {
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> JniValidation.requireNonNegative(-1L, "negativeLong"));

    assertThat(exception.getMessage()).contains("negativeLong");
    assertThat(exception.getMessage()).contains("must be non-negative");
    assertThat(exception.getMessage()).contains("got -1");
  }

  @Test
  void testRequireValidHandleWithValidHandle() {
    assertDoesNotThrow(() -> JniValidation.requireValidHandle(1L, "validHandle"));
    assertDoesNotThrow(() -> JniValidation.requireValidHandle(Long.MAX_VALUE, "maxHandle"));
    assertDoesNotThrow(() -> JniValidation.requireValidHandle(-1L, "negativeHandle")); // Negative handles can be valid
  }

  @Test
  void testRequireValidHandleWithInvalidHandle() {
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> JniValidation.requireValidHandle(0L, "nullHandle"));

    assertThat(exception.getMessage()).contains("nullHandle");
    assertThat(exception.getMessage()).contains("invalid native handle");
    assertThat(exception.getMessage()).contains("null pointer");
  }

  @Test
  void testRequireValidBoundsWithValidBounds() {
    final byte[] array = {1, 2, 3, 4, 5};
    assertDoesNotThrow(() -> JniValidation.requireValidBounds(array, 0, 5, "fullArray"));
    assertDoesNotThrow(() -> JniValidation.requireValidBounds(array, 1, 3, "subArray"));
    assertDoesNotThrow(() -> JniValidation.requireValidBounds(array, 5, 0, "emptyAtEnd"));
    assertDoesNotThrow(() -> JniValidation.requireValidBounds(array, 0, 0, "emptyAtStart"));
  }

  @Test
  void testRequireValidBoundsWithInvalidBounds() {
    final byte[] array = {1, 2, 3, 4, 5};

    // Test null array
    assertThrows(JniValidationException.class,
        () -> JniValidation.requireValidBounds(null, 0, 1, "nullArray"));

    // Test negative offset
    assertThrows(JniValidationException.class,
        () -> JniValidation.requireValidBounds(array, -1, 1, "negativeOffset"));

    // Test negative length
    assertThrows(JniValidationException.class,
        () -> JniValidation.requireValidBounds(array, 0, -1, "negativeLength"));

    // Test offset beyond array
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> JniValidation.requireValidBounds(array, 6, 1, "offsetBeyond"));
    assertThat(exception.getMessage()).contains("Offset 6 exceeds array length 5");

    // Test offset + length beyond array
    final JniValidationException exception2 = assertThrows(JniValidationException.class,
        () -> JniValidation.requireValidBounds(array, 3, 3, "lengthBeyond"));
    assertThat(exception2.getMessage()).contains("Offset 3 + length 3 exceeds array length 5");
  }

  @Test
  void testRequireConditionWithValidCondition() {
    assertDoesNotThrow(() -> JniValidation.require(true, "Should not throw"));
  }

  @Test
  void testRequireConditionWithInvalidCondition() {
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> JniValidation.require(false, "Custom error message"));

    assertThat(exception.getMessage()).isEqualTo("Custom error message");
  }

  @Test
  void testRequireConditionWithParameterDetailsWithValidCondition() {
    assertDoesNotThrow(() -> JniValidation.require(true, "Should not throw", "param", "value"));
  }

  @Test
  void testRequireConditionWithParameterDetailsWithInvalidCondition() {
    final JniValidationException exception = assertThrows(JniValidationException.class,
        () -> JniValidation.require(false, "Custom error message", "paramName", "paramValue"));

    assertThat(exception.getMessage()).isEqualTo("Custom error message");
    assertThat(exception.getParameterName()).isEqualTo("paramName");
    assertThat(exception.getParameterValue()).isEqualTo("paramValue");
  }

  @Test
  void testDefensiveCopyWithValidArray() {
    final byte[] original = {1, 2, 3};
    final byte[] copy = JniValidation.defensiveCopy(original);

    assertThat(copy).isEqualTo(original);
    assertThat(copy).isNotSameAs(original); // Different object reference

    // Modify original to ensure copy is independent
    original[0] = 99;
    assertThat(copy[0]).isEqualTo((byte) 1);
  }

  @Test
  void testDefensiveCopyWithNullArray() {
    final byte[] copy = JniValidation.defensiveCopy(null);
    assertThat(copy).isNull();
  }

  @Test
  void testToBytesWithValidString() {
    final String testString = "Hello, World!";
    final byte[] bytes = JniValidation.toBytes(testString, "testString");

    assertThat(bytes).isEqualTo(testString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  @Test
  void testToBytesWithNullString() {
    assertThrows(JniValidationException.class,
        () -> JniValidation.toBytes(null, "nullString"));
  }

  @Test
  void testToBytesWithEmptyString() {
    final byte[] bytes = JniValidation.toBytes("", "emptyString");
    assertThat(bytes).isEmpty();
  }

  @Test
  void testToBytesWithUnicodeString() {
    final String unicodeString = "Hello 世界";
    final byte[] bytes = JniValidation.toBytes(unicodeString, "unicodeString");

    assertThat(bytes).isEqualTo(unicodeString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    assertThat(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(unicodeString);
  }

  @Test
  void testUtilityClassCannotBeInstantiated() {
    // Ensure utility class cannot be instantiated
    assertThrows(AssertionError.class, () -> {
      try {
        final java.lang.reflect.Constructor<?> constructor = JniValidation.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
      } catch (Exception e) {
        if (e.getCause() instanceof AssertionError) {
          throw (AssertionError) e.getCause();
        }
        throw new RuntimeException(e);
      }
    });
  }
}