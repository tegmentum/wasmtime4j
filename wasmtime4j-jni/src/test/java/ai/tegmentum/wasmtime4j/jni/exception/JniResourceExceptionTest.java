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
  @DisplayName("Constructor(String) Tests")
  class ConstructorStringTests {

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      final String message = "Resource allocation failed";
      final JniResourceException exception = new JniResourceException(message);

      assertEquals(message, exception.getMessage(), "Message should match");
      assertNull(exception.getCause(), "Cause should be null");
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
  }

  @Nested
  @DisplayName("Serialization Tests")
  class SerializationTests {

    @Test
    @DisplayName("Should be serializable")
    void shouldBeSerializable() throws Exception {
      final JniResourceException original = new JniResourceException("Serialization test");

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(original);
      oos.close();

      final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      final ObjectInputStream ois = new ObjectInputStream(bais);
      final JniResourceException deserialized = (JniResourceException) ois.readObject();
      ois.close();

      assertEquals(
          original.getMessage(), deserialized.getMessage(), "Message should survive serialization");
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
      assertEquals(
          "Original cause", deserialized.getCause().getMessage(), "Cause message should match");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be throwable and catchable")
    void shouldBeThrowableAndCatchable() {
      try {
        throw new JniResourceException("Test throw");
      } catch (JniResourceException e) {
        assertEquals("Test throw", e.getMessage(), "Should catch with correct message");
      }
    }

    @Test
    @DisplayName("Should be catchable as RuntimeException")
    void shouldBeCatchableAsRuntimeException() {
      try {
        throw new JniResourceException("Runtime error");
      } catch (RuntimeException e) {
        assertTrue(e instanceof JniResourceException, "Should be instance of JniResourceException");
      }
    }

    @Test
    @DisplayName("Different exceptions should be distinguishable")
    void differentExceptionsShouldBeDistinguishable() {
      final JniResourceException e1 = new JniResourceException("Error 1");
      final JniResourceException e2 = new JniResourceException("Error 2");

      assertFalse(
          e1.getMessage().equals(e2.getMessage()), "Different messages should be different");
    }
  }
}
