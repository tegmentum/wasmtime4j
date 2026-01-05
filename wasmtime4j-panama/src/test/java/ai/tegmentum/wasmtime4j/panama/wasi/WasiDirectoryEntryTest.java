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

package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiDirectoryEntry} class.
 *
 * <p>WasiDirectoryEntry represents a single entry in a directory listing, providing essential
 * information about files and subdirectories.
 */
@DisplayName("WasiDirectoryEntry Tests")
class WasiDirectoryEntryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiDirectoryEntry.class.getModifiers()),
          "WasiDirectoryEntry should be public");
      assertTrue(
          Modifier.isFinal(WasiDirectoryEntry.class.getModifiers()),
          "WasiDirectoryEntry should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with all parameters")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiDirectoryEntry.class.getConstructor(
              String.class,
              boolean.class,
              boolean.class,
              boolean.class,
              long.class,
              FileTime.class);
      assertNotNull(constructor, "Constructor with all parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryEntry.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryEntry.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getLastModifiedTime method")
    void shouldHaveGetLastModifiedTimeMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryEntry.class.getMethod("getLastModifiedTime");
      assertNotNull(method, "getLastModifiedTime method should exist");
      assertEquals(FileTime.class, method.getReturnType(), "Should return FileTime");
    }
  }

  @Nested
  @DisplayName("File Type Method Tests")
  class FileTypeMethodTests {

    @Test
    @DisplayName("should have isRegularFile method")
    void shouldHaveIsRegularFileMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryEntry.class.getMethod("isRegularFile");
      assertNotNull(method, "isRegularFile method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isDirectory method")
    void shouldHaveIsDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryEntry.class.getMethod("isDirectory");
      assertNotNull(method, "isDirectory method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isSymbolicLink method")
    void shouldHaveIsSymbolicLinkMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryEntry.class.getMethod("isSymbolicLink");
      assertNotNull(method, "isSymbolicLink method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getWasiFileType method")
    void shouldHaveGetWasiFileTypeMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryEntry.class.getMethod("getWasiFileType");
      assertNotNull(method, "getWasiFileType method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryEntry.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryEntry.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = WasiDirectoryEntry.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
