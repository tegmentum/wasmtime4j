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
 * Tests for {@link WasiPermissionException} class.
 *
 * <p>WasiPermissionException is thrown when WASI permission operations fail.
 */
@DisplayName("WasiPermissionException Tests")
class WasiPermissionExceptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(WasiPermissionException.class.getModifiers()),
          "WasiPermissionException should be public");
    }

    @Test
    @DisplayName("should extend WasiFileSystemException")
    void shouldExtendWasiFileSystemException() {
      assertTrue(
          WasiFileSystemException.class.isAssignableFrom(WasiPermissionException.class),
          "WasiPermissionException should extend WasiFileSystemException");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with String message")
    void shouldHaveConstructorWithMessage() throws NoSuchMethodException {
      final Constructor<?> constructor = WasiPermissionException.class.getConstructor(String.class);
      assertNotNull(constructor, "Constructor with String should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with String message and Throwable cause")
    void shouldHaveConstructorWithMessageAndCause() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiPermissionException.class.getConstructor(String.class, Throwable.class);
      assertNotNull(constructor, "Constructor with String and Throwable should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }
}
