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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JniLibraryException} class.
 *
 * <p>This test class verifies JniLibraryException constructors and behavior.
 */
@DisplayName("JniLibraryException Tests")
class JniLibraryExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniLibraryException should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniLibraryException.class.getModifiers()),
          "JniLibraryException should be final");
    }

    @Test
    @DisplayName("JniLibraryException should extend JniException")
    void shouldExtendJniException() {
      assertTrue(
          JniException.class.isAssignableFrom(JniLibraryException.class),
          "JniLibraryException should extend JniException");
    }

    @Test
    @DisplayName("JniLibraryException should be a RuntimeException")
    void shouldBeRuntimeException() {
      assertTrue(
          RuntimeException.class.isAssignableFrom(JniLibraryException.class),
          "JniLibraryException should be a RuntimeException");
    }
  }

  @Nested
  @DisplayName("Constructor(String) Tests")
  class ConstructorStringTests {

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      final String message = "Native library not found";
      final JniLibraryException exception = new JniLibraryException(message);

      assertEquals(message, exception.getMessage(), "Message should match");
      assertNull(exception.getCause(), "Cause should be null");
      assertFalse(exception.hasNativeErrorCode(), "Should not have native error code");
      assertNull(exception.getNativeErrorCode(), "Native error code should be null");
    }

    @Test
    @DisplayName("Should handle null message")
    void shouldHandleNullMessage() {
      final JniLibraryException exception = new JniLibraryException(null);

      assertNull(exception.getMessage(), "Message should be null");
    }

    @Test
    @DisplayName("Should handle empty message")
    void shouldHandleEmptyMessage() {
      final JniLibraryException exception = new JniLibraryException("");

      assertEquals("", exception.getMessage(), "Message should be empty");
    }

    @Test
    @DisplayName("Should handle typical library loading error messages")
    void shouldHandleTypicalLibraryLoadingMessages() {
      final String[] messages = {
        "Unable to load native library: libwasmtime4j.so",
        "Library libwasmtime4j.dylib not found in java.library.path",
        "UnsatisfiedLinkError: no wasmtime4j in java.library.path",
        "Failed to initialize native runtime"
      };

      for (String message : messages) {
        final JniLibraryException exception = new JniLibraryException(message);
        assertEquals(message, exception.getMessage(), "Message should match exactly");
      }
    }
  }

  @Nested
  @DisplayName("Constructor(String, Throwable) Tests")
  class ConstructorStringThrowableTests {

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      final String message = "Native library initialization failed";
      final Throwable cause = new UnsatisfiedLinkError("libwasmtime4j.so not found");
      final JniLibraryException exception = new JniLibraryException(message, cause);

      assertEquals(message, exception.getMessage(), "Message should match");
      assertSame(cause, exception.getCause(), "Cause should match");
      assertFalse(exception.hasNativeErrorCode(), "Should not have native error code");
    }

    @Test
    @DisplayName("Should handle null cause")
    void shouldHandleNullCause() {
      final JniLibraryException exception = new JniLibraryException("Library load failed", null);

      assertEquals("Library load failed", exception.getMessage(), "Message should match");
      assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    @DisplayName("Should wrap UnsatisfiedLinkError as cause")
    void shouldWrapUnsatisfiedLinkErrorAsCause() {
      final UnsatisfiedLinkError linkError =
          new UnsatisfiedLinkError("no wasmtime4j in java.library.path");
      final JniLibraryException exception =
          new JniLibraryException("Failed to load native library", linkError);

      assertSame(linkError, exception.getCause(), "Cause should be the UnsatisfiedLinkError");
      assertTrue(
          exception.getCause() instanceof UnsatisfiedLinkError,
          "Cause should be UnsatisfiedLinkError");
    }

    @Test
    @DisplayName("Should wrap SecurityException as cause")
    void shouldWrapSecurityExceptionAsCause() {
      final SecurityException securityException =
          new SecurityException("Access to native library denied");
      final JniLibraryException exception =
          new JniLibraryException("Security violation during library load", securityException);

      assertSame(securityException, exception.getCause(), "Cause should be the SecurityException");
    }

    @Test
    @DisplayName("Should preserve exception chain")
    void shouldPreserveExceptionChain() {
      final Exception root = new IllegalStateException("File corrupted");
      final UnsatisfiedLinkError middle = new UnsatisfiedLinkError("Library format invalid");
      middle.initCause(root);
      final JniLibraryException exception = new JniLibraryException("Cannot load library", middle);

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
      final JniLibraryException exception =
          new JniLibraryException("Library initialization failed");
      final String str = exception.toString();

      assertTrue(str.contains("JniLibraryException"), "Should contain class name");
      assertTrue(str.contains("Library initialization failed"), "Should contain message");
    }

    @Test
    @DisplayName("toString should not include native error code")
    void toStringShouldNotIncludeNativeErrorCode() {
      // JniLibraryException only has constructors without error code
      final JniLibraryException exception = new JniLibraryException("Test error");
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
      final JniLibraryException original = new JniLibraryException("Serialization test");

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(original);
      oos.close();

      final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      final ObjectInputStream ois = new ObjectInputStream(bais);
      final JniLibraryException deserialized = (JniLibraryException) ois.readObject();
      ois.close();

      assertEquals(
          original.getMessage(), deserialized.getMessage(), "Message should survive serialization");
    }

    @Test
    @DisplayName("Should serialize with cause")
    void shouldSerializeWithCause() throws Exception {
      final UnsatisfiedLinkError cause = new UnsatisfiedLinkError("Library not found");
      final JniLibraryException original = new JniLibraryException("Failed to load library", cause);

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(original);
      oos.close();

      final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      final ObjectInputStream ois = new ObjectInputStream(bais);
      final JniLibraryException deserialized = (JniLibraryException) ois.readObject();
      ois.close();

      assertNotNull(deserialized.getCause(), "Cause should survive serialization");
      assertEquals(
          "Library not found", deserialized.getCause().getMessage(), "Cause message should match");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should be throwable and catchable")
    void shouldBeThrowableAndCatchable() {
      try {
        throw new JniLibraryException("Test library exception");
      } catch (JniLibraryException e) {
        assertEquals("Test library exception", e.getMessage(), "Should catch with correct message");
      }
    }

    @Test
    @DisplayName("Should be catchable as JniException")
    void shouldBeCatchableAsJniException() {
      try {
        throw new JniLibraryException("Library not found");
      } catch (JniException e) {
        assertTrue(e instanceof JniLibraryException, "Should be instance of JniLibraryException");
      }
    }

    @Test
    @DisplayName("Should be catchable as WasmException")
    void shouldBeCatchableAsWasmException() {
      try {
        throw new JniLibraryException("Library error");
      } catch (WasmException e) {
        assertTrue(e instanceof JniLibraryException, "Should be instance of JniLibraryException");
      }
    }

    @Test
    @DisplayName("Should be distinguishable from JniResourceException")
    void shouldBeDistinguishableFromJniResourceException() {
      final JniLibraryException libraryException = new JniLibraryException("Library error");
      final JniResourceException resourceException = new JniResourceException("Resource error");

      assertTrue(
          libraryException instanceof JniException, "Library exception should be JniException");
      assertFalse(
          JniException.class.isAssignableFrom(JniResourceException.class),
          "Resource exception should not be JniException (it is unchecked)");
      assertFalse(
          libraryException.getClass().equals(resourceException.getClass()),
          "Different exception types should be distinguishable");
    }

    @Test
    @DisplayName("Typical usage pattern with library loading")
    void typicalUsagePatternWithLibraryLoading() {
      // Simulate typical library loading error handling
      final String libraryPath = "/usr/lib/libwasmtime4j.so";
      final JniLibraryException exception;

      try {
        // Simulate loading failure
        throw new UnsatisfiedLinkError("Cannot load library: " + libraryPath);
      } catch (UnsatisfiedLinkError e) {
        exception =
            new JniLibraryException(
                "Failed to load Wasmtime native library from " + libraryPath, e);
      }

      assertNotNull(exception, "Exception should be created");
      assertTrue(
          exception.getMessage().contains(libraryPath), "Message should contain library path");
      assertNotNull(exception.getCause(), "Cause should be present");
      assertTrue(
          exception.getCause() instanceof UnsatisfiedLinkError,
          "Cause should be UnsatisfiedLinkError");
    }
  }
}
