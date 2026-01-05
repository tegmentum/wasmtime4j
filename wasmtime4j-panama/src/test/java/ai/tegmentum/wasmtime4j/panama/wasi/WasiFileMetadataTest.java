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
 * Tests for {@link WasiFileMetadata} class.
 *
 * <p>WasiFileMetadata encapsulates file metadata that is returned by WASI stat operations.
 */
@DisplayName("WasiFileMetadata Tests")
class WasiFileMetadataTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiFileMetadata.class.getModifiers()),
          "WasiFileMetadata should be public");
      assertTrue(
          Modifier.isFinal(WasiFileMetadata.class.getModifiers()),
          "WasiFileMetadata should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with all parameters")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiFileMetadata.class.getConstructor(
              long.class,
              FileTime.class,
              FileTime.class,
              FileTime.class,
              boolean.class,
              boolean.class,
              boolean.class,
              boolean.class,
              boolean.class,
              boolean.class);
      assertNotNull(constructor, "Constructor with all parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Basic Accessor Method Tests")
  class BasicAccessorMethodTests {

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getLastModifiedTime method")
    void shouldHaveGetLastModifiedTimeMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("getLastModifiedTime");
      assertNotNull(method, "getLastModifiedTime method should exist");
      assertEquals(FileTime.class, method.getReturnType(), "Should return FileTime");
    }

    @Test
    @DisplayName("should have getLastAccessTime method")
    void shouldHaveGetLastAccessTimeMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("getLastAccessTime");
      assertNotNull(method, "getLastAccessTime method should exist");
      assertEquals(FileTime.class, method.getReturnType(), "Should return FileTime");
    }

    @Test
    @DisplayName("should have getCreationTime method")
    void shouldHaveGetCreationTimeMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("getCreationTime");
      assertNotNull(method, "getCreationTime method should exist");
      assertEquals(FileTime.class, method.getReturnType(), "Should return FileTime");
    }
  }

  @Nested
  @DisplayName("File Type Method Tests")
  class FileTypeMethodTests {

    @Test
    @DisplayName("should have isRegularFile method")
    void shouldHaveIsRegularFileMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("isRegularFile");
      assertNotNull(method, "isRegularFile method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isDirectory method")
    void shouldHaveIsDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("isDirectory");
      assertNotNull(method, "isDirectory method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isSymbolicLink method")
    void shouldHaveIsSymbolicLinkMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("isSymbolicLink");
      assertNotNull(method, "isSymbolicLink method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Permission Method Tests")
  class PermissionMethodTests {

    @Test
    @DisplayName("should have isReadable method")
    void shouldHaveIsReadableMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("isReadable");
      assertNotNull(method, "isReadable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isWritable method")
    void shouldHaveIsWritableMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("isWritable");
      assertNotNull(method, "isWritable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isExecutable method")
    void shouldHaveIsExecutableMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("isExecutable");
      assertNotNull(method, "isExecutable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Time Conversion Method Tests")
  class TimeConversionMethodTests {

    @Test
    @DisplayName("should have getLastModifiedTimeSeconds method")
    void shouldHaveGetLastModifiedTimeSecondsMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("getLastModifiedTimeSeconds");
      assertNotNull(method, "getLastModifiedTimeSeconds method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getLastModifiedTimeNanos method")
    void shouldHaveGetLastModifiedTimeNanosMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("getLastModifiedTimeNanos");
      assertNotNull(method, "getLastModifiedTimeNanos method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getLastAccessTimeSeconds method")
    void shouldHaveGetLastAccessTimeSecondsMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("getLastAccessTimeSeconds");
      assertNotNull(method, "getLastAccessTimeSeconds method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getLastAccessTimeNanos method")
    void shouldHaveGetLastAccessTimeNanosMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("getLastAccessTimeNanos");
      assertNotNull(method, "getLastAccessTimeNanos method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getCreationTimeSeconds method")
    void shouldHaveGetCreationTimeSecondsMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("getCreationTimeSeconds");
      assertNotNull(method, "getCreationTimeSeconds method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getCreationTimeNanos method")
    void shouldHaveGetCreationTimeNanosMethod() throws NoSuchMethodException {
      final Method method = WasiFileMetadata.class.getMethod("getCreationTimeNanos");
      assertNotNull(method, "getCreationTimeNanos method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }
}
