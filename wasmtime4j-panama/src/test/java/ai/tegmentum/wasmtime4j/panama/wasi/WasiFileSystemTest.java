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

import ai.tegmentum.wasmtime4j.wasi.WasiFileMetadata;
import ai.tegmentum.wasmtime4j.wasi.WasiFileOperation;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.attribute.FileTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiFileSystem} class.
 *
 * <p>WasiFileSystem provides Panama FFI implementation of WASI file system operations with
 * comprehensive sandbox security.
 */
@DisplayName("WasiFileSystem Tests")
class WasiFileSystemTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiFileSystem.class.getModifiers()),
          "WasiFileSystem should be public");
      assertTrue(
          Modifier.isFinal(WasiFileSystem.class.getModifiers()), "WasiFileSystem should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with WasiContext")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = WasiFileSystem.class.getConstructor(WasiContext.class);
      assertNotNull(constructor, "Constructor with WasiContext should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("File Open Method Tests")
  class FileOpenMethodTests {

    @Test
    @DisplayName("should have openFile method")
    void shouldHaveOpenFileMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod(
              "openFile", String.class, WasiFileOperation.class, boolean.class, boolean.class);
      assertNotNull(method, "openFile method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("File Read Method Tests")
  class FileReadMethodTests {

    @Test
    @DisplayName("should have readFile method")
    void shouldHaveReadFileMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod("readFile", int.class, byte[].class, int.class, int.class);
      assertNotNull(method, "readFile method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have readFileZeroCopy method")
    void shouldHaveReadFileZeroCopyMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod(
              "readFileZeroCopy", int.class, MemorySegment.class, int.class, int.class);
      assertNotNull(method, "readFileZeroCopy method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("File Write Method Tests")
  class FileWriteMethodTests {

    @Test
    @DisplayName("should have writeFile method")
    void shouldHaveWriteFileMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod(
              "writeFile", int.class, byte[].class, int.class, int.class);
      assertNotNull(method, "writeFile method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have writeFileZeroCopy method")
    void shouldHaveWriteFileZeroCopyMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod(
              "writeFileZeroCopy", int.class, MemorySegment.class, int.class, int.class);
      assertNotNull(method, "writeFileZeroCopy method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("File Position Method Tests")
  class FilePositionMethodTests {

    @Test
    @DisplayName("should have seekFile method")
    void shouldHaveSeekFileMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod("seekFile", int.class, long.class, int.class);
      assertNotNull(method, "seekFile method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have syncFile method")
    void shouldHaveSyncFileMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("syncFile", int.class, boolean.class);
      assertNotNull(method, "syncFile method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have truncateFile method")
    void shouldHaveTruncateFileMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("truncateFile", int.class, long.class);
      assertNotNull(method, "truncateFile method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("File Close Method Tests")
  class FileCloseMethodTests {

    @Test
    @DisplayName("should have closeFile method")
    void shouldHaveCloseFileMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("closeFile", int.class);
      assertNotNull(method, "closeFile method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have closeAll method")
    void shouldHaveCloseAllMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("closeAll");
      assertNotNull(method, "closeAll method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Metadata Method Tests")
  class MetadataMethodTests {

    @Test
    @DisplayName("should have getFileMetadata method")
    void shouldHaveGetFileMetadataMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("getFileMetadata", String.class);
      assertNotNull(method, "getFileMetadata method should exist");
      assertEquals(
          WasiFileMetadata.class, method.getReturnType(), "Should return WasiFileMetadata");
    }

    @Test
    @DisplayName("should have setFileTimes method")
    void shouldHaveSetFileTimesMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod(
              "setFileTimes", String.class, FileTime.class, FileTime.class);
      assertNotNull(method, "setFileTimes method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Directory Method Tests")
  class DirectoryMethodTests {

    @Test
    @DisplayName("should have listDirectory method")
    void shouldHaveListDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("listDirectory", String.class);
      assertNotNull(method, "listDirectory method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have createDirectory method")
    void shouldHaveCreateDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("createDirectory", String.class);
      assertNotNull(method, "createDirectory method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have removeFileOrDirectory method")
    void shouldHaveRemoveFileOrDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("removeFileOrDirectory", String.class);
      assertNotNull(method, "removeFileOrDirectory method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have renameFileOrDirectory method")
    void shouldHaveRenameFileOrDirectoryMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod("renameFileOrDirectory", String.class, String.class);
      assertNotNull(method, "renameFileOrDirectory method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getOpenFileCount method")
    void shouldHaveGetOpenFileCountMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("getOpenFileCount");
      assertNotNull(method, "getOpenFileCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }
}
