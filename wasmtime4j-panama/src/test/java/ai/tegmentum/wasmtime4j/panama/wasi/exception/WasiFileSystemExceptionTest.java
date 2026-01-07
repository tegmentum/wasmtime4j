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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiFileSystemException} class.
 *
 * <p>WasiFileSystemException is thrown when WASI file system operations fail.
 */
@DisplayName("WasiFileSystemException Tests")
class WasiFileSystemExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(WasiFileSystemException.class.getModifiers()),
          "WasiFileSystemException should be public");
    }

    @Test
    @DisplayName("should extend PanamaException")
    void shouldExtendPanamaException() {
      assertTrue(
          ai.tegmentum.wasmtime4j.panama.exception.PanamaException.class.isAssignableFrom(
              WasiFileSystemException.class),
          "WasiFileSystemException should extend PanamaException");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with String message and String wasiErrorCode")
    void shouldHaveConstructorWithMessageAndErrorCode() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiFileSystemException.class.getConstructor(String.class, String.class);
      assertNotNull(constructor, "Constructor with String, String should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with message, wasiErrorCode, and cause")
    void shouldHaveConstructorWithMessageErrorCodeAndCause() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiFileSystemException.class.getConstructor(String.class, String.class, Throwable.class);
      assertNotNull(constructor, "Constructor with String, String, Throwable should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }
}
