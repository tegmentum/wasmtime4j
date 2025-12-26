/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.jni.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JniResourceException} class.
 *
 * <p>This test class verifies JniResourceException constructors and behavior.
 */
@DisplayName("JniResourceException Tests")
class JniResourceExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniResourceException should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniResourceException.class.getModifiers()),
          "JniResourceException should be final");
    }

    @Test
    @DisplayName("JniResourceException should extend JniException")
    void shouldExtendJniException() {
      assertTrue(
          JniException.class.isAssignableFrom(JniResourceException.class),
          "JniResourceException should extend JniException");
    }

    @Test
    @DisplayName("JniResourceException should be a RuntimeException")
    void shouldBeRuntimeException() {
      assertTrue(
          RuntimeException.class.isAssignableFrom(JniResourceException.class),
          "JniResourceException should be a RuntimeException");
    }
  }

  @Nested
  @DisplayName("Constructor(String) Tests")
  class ConstructorStringTests {

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      final String message = "Resource allocation failed";
      final JniResourceException exception = new JniResourceException(message);

      assertEquals(message, exception.getMessage(), "Message should match");
      assertNull(exception.getCause(), "Cause should be null");
      assertFalse(exception.hasNativeErrorCode(), "Should not have native error code");
      assertNull(exception.getNativeErrorCode(), "Native error code should be null");
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
      final JniResourceException exception = new JniResourceException(null);

      assertNull(exception.getMessage(), "Message should be null");
    }

    @Test
    @DisplayName("Should handle empty message")
    void shouldHandleEmptyMessage() {
      final JniResourceException exception = new JniResourceException("");

      assertEquals("", exception.getMessage(), "Message should be empty");
    }
  }

  @Nested
  @DisplayName("Constructor(String, Throwable) Tests")
  class ConstructorStringThrowableTests {

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Resource deallocation failed";
      final Throwable cause = new RuntimeException("Underlying error");
      final JniResourceException exception = new JniResourceException(message, cause);

      assertEquals(message, exception.getMessage(), "Message should match");
      assertSame(cause, exception.getCause(), "Cause should match");
      assertFalse(exception.hasNativeErrorCode(), "Should not have native error code");
    }

    @Test
    @DisplayName("Should handle null cause")
    void shouldHandleNullCause() {
      final JniResourceException exception = new JniResourceException("Error", (Throwable) null);

      assertEquals("Error", exception.getMessage(), "Message should match");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Should preserve exception chain")
    void shouldPreserveExceptionChain() {
      final Exception root = new IllegalStateException("Root cause");
      final Exception middle = new RuntimeException("Middle cause", root);
      final JniResourceException exception = new JniResourceException("Final", middle);

      assertSame(middle, exception.getCause(), "Direct cause should match");
      assertSame(root, exception.getCause().getCause(), "Root cause should be preserved");
    }
  }

  @Nested
  @DisplayName("Constructor(String, int) Tests")
  class ConstructorStringIntTests {

    @Test
    @DisplayName("Should create exception with message and native error code")
    void shouldCreateExceptionWithMessageAndErrorCode() {
      final String message = "Resource limit exceeded";
      final int errorCode = 42;
      final JniResourceException exception = new JniResourceException(message, errorCode);

      assertEquals(message, exception.getMessage(), "Message should match");
      assertNull(exception.getCause(), "Cause should be null");
      assertTrue(exception.hasNativeErrorCode(), "Should have native error code");
      assertEquals(Integer.valueOf(errorCode), exception.getNativeErrorCode(),
          "Native error code should match");
    }

    @Test
    @DisplayName("Should handle zero error code")
    void shouldHandleZeroErrorCode() {
      final JniResourceException exception = new JniResourceException("Error", 0);

      assertTrue(exception.hasNativeErrorCode(), "Should have native error code");
      assertEquals(Integer.valueOf(0), exception.getNativeErrorCode(),
          "Native error code should be zero");
    }

    @Test
    @DisplayName("Should handle negative error code")
    void shouldHandleNegativeErrorCode() {
      final JniResourceException exception = new JniResourceException("Error", -1);

      assertTrue(exception.hasNativeErrorCode(), "Should have native error code");
      assertEquals(Integer.valueOf(-1), exception.getNativeErrorCode(),
          "Native error code should be -1");
    }

    @Test
    @DisplayName("Should handle max integer error code")
    void shouldHandleMaxIntegerErrorCode() {
      final JniResourceException exception =
          new JniResourceException("Error", Integer.MAX_VALUE);

      assertEquals(Integer.valueOf(Integer.MAX_VALUE), exception.getNativeErrorCode(),
          "Native error code should be MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("Constructor(String, Throwable, int) Tests")
  class ConstructorStringThrowableIntTests {

    @Test
    @DisplayName("Should create exception with all parameters")
    void shouldCreateExceptionWithAllParameters() {
      final String message = "Memory management failure";
      final Throwable cause = new OutOfMemoryError("Not enough memory");
      final int errorCode = 100;
      final JniResourceException exception =
          new JniResourceException(message, cause, errorCode);

      assertEquals(message, exception.getMessage(), "Message should match");
      assertSame(cause, exception.getCause(), "Cause should match");
      assertTrue(exception.hasNativeErrorCode(), "Should have native error code");
      assertEquals(Integer.valueOf(errorCode), exception.getNativeErrorCode(),
          "Native error code should match");
    }

    @Test
    @DisplayName("Should handle null cause with error code")
    void shouldHandleNullCauseWithErrorCode() {
      final JniResourceException exception =
          new JniResourceException("Error", (Throwable) null, 50);

      assertNull(exception.getCause(), "Cause should be null");
      assertEquals(Integer.valueOf(50), exception.getNativeErrorCode(),
          "Native error code should match");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include class name and message")
    void toStringShouldIncludeClassNameAndMessage() {
      final JniResourceException exception = new JniResourceException("Test error");
      final String str = exception.toString();

      assertTrue(str.contains("JniResourceException"), "Should contain class name");
      assertTrue(str.contains("Test error"), "Should contain message");
    }

    @Test
    @DisplayName("toString should include native error code when present")
    void toStringShouldIncludeNativeErrorCodeWhenPresent() {
      final JniResourceException exception = new JniResourceException("Test error", 123);
      final String str = exception.toString();

      assertTrue(str.contains("123"), "Should contain native error code");
      assertTrue(str.contains("native error code"), "Should indicate native error code");
    }

    @Test
    @DisplayName("toString should not include native error code when absent")
    void toStringShouldNotIncludeNativeErrorCodeWhenAbsent() {
      final JniResourceException exception = new JniResourceException("Test error");
      final String str = exception.toString();

      assertFalse(str.contains("native error code"), "Should not mention native error code");
    }
  }

  @Nested
  @DisplayName("Serialization Tests")
  class SerializationTests {

    @Test
    @DisplayName("Should be serializable")
    void shouldBeSerializable() throws Exception {
      final JniResourceException original =
          new JniResourceException("Serialization test", 99);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(original);
      oos.close();

      final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      final ObjectInputStream ois = new ObjectInputStream(bais);
      final JniResourceException deserialized = (JniResourceException) ois.readObject();
      ois.close();

      assertEquals(original.getMessage(), deserialized.getMessage(),
          "Message should survive serialization");
      assertEquals(original.getNativeErrorCode(), deserialized.getNativeErrorCode(),
          "Native error code should survive serialization");
    }

    @Test
    @DisplayName("Should serialize with cause")
    void shouldSerializeWithCause() throws Exception {
      final RuntimeException cause = new RuntimeException("Original cause");
      final JniResourceException original = new JniResourceException("Test", cause);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(original);
      oos.close();

      final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      final ObjectInputStream ois = new ObjectInputStream(bais);
      final JniResourceException deserialized = (JniResourceException) ois.readObject();
      ois.close();

      assertNotNull(deserialized.getCause(), "Cause should survive serialization");
      assertEquals("Original cause", deserialized.getCause().getMessage(),
          "Cause message should match");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be throwable and catchable")
    void shouldBeThrowableAndCatchable() {
      try {
        throw new JniResourceException("Test throw", 1);
      } catch (JniResourceException e) {
        assertEquals("Test throw", e.getMessage(), "Should catch with correct message");
        assertEquals(Integer.valueOf(1), e.getNativeErrorCode(),
            "Should catch with correct error code");
      }
    }

    @Test
    @DisplayName("Should be catchable as JniException")
    void shouldBeCatchableAsJniException() {
      try {
        throw new JniResourceException("Resource error");
      } catch (JniException e) {
        assertTrue(e instanceof JniResourceException,
            "Should be instance of JniResourceException");
      }
    }

    @Test
    @DisplayName("Should be catchable as RuntimeException")
    void shouldBeCatchableAsRuntimeException() {
      try {
        throw new JniResourceException("Runtime error");
      } catch (RuntimeException e) {
        assertTrue(e instanceof JniResourceException,
            "Should be instance of JniResourceException");
      }
    }

    @Test
    @DisplayName("Different exceptions should be distinguishable")
    void differentExceptionsShouldBeDistinguishable() {
      final JniResourceException e1 = new JniResourceException("Error 1");
      final JniResourceException e2 = new JniResourceException("Error 2");
      final JniResourceException e3 = new JniResourceException("Error 1", 10);

      assertFalse(e1.getMessage().equals(e2.getMessage()),
          "Different messages should be different");
      assertEquals(e1.getMessage(), e3.getMessage(), "Same messages should be equal");
      assertFalse(e1.hasNativeErrorCode() == e3.hasNativeErrorCode(),
          "Error code presence should differ");
    }
  }
}
