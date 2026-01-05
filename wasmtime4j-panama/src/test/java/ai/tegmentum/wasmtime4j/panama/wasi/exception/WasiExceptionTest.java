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

package ai.tegmentum.wasmtime4j.panama.wasi.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiException} class.
 *
 * <p>WasiException is thrown when WASI operations fail, providing information about the specific
 * operation that failed and the underlying error condition.
 */
@DisplayName("WasiException Tests")
class WasiExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(WasiException.class.getModifiers()), "WasiException should be public");
    }

    @Test
    @DisplayName("should extend PanamaException")
    void shouldExtendPanamaException() {
      assertTrue(
          PanamaException.class.isAssignableFrom(WasiException.class),
          "WasiException should extend PanamaException");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with String message")
    void shouldHaveConstructorWithMessage() throws NoSuchMethodException {
      final Constructor<?> constructor = WasiException.class.getConstructor(String.class);
      assertNotNull(constructor, "Constructor with String should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with String message and Throwable cause")
    void shouldHaveConstructorWithMessageAndCause() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiException.class.getConstructor(String.class, Throwable.class);
      assertNotNull(constructor, "Constructor with String and Throwable should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with Throwable cause")
    void shouldHaveConstructorWithCause() throws NoSuchMethodException {
      final Constructor<?> constructor = WasiException.class.getConstructor(Throwable.class);
      assertNotNull(constructor, "Constructor with Throwable should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Functional Tests")
  class FunctionalTests {

    @Test
    @DisplayName("constructor with message should set message")
    void constructorWithMessageShouldSetMessage() {
      final String message = "Test WASI error";
      final WasiException exception = new WasiException(message);
      assertEquals(message, exception.getMessage(), "Message should be set correctly");
    }

    @Test
    @DisplayName("constructor with message and cause should set both")
    void constructorWithMessageAndCauseShouldSetBoth() {
      final String message = "Test WASI error";
      final RuntimeException cause = new RuntimeException("Root cause");
      final WasiException exception = new WasiException(message, cause);
      assertEquals(message, exception.getMessage(), "Message should be set correctly");
      assertEquals(cause, exception.getCause(), "Cause should be set correctly");
    }

    @Test
    @DisplayName("constructor with cause should set cause")
    void constructorWithCauseShouldSetCause() {
      final RuntimeException cause = new RuntimeException("Root cause");
      final WasiException exception = new WasiException(cause);
      assertEquals(cause, exception.getCause(), "Cause should be set correctly");
    }
  }
}
