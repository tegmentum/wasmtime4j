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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.wasi.WasiFileMetadata;
import ai.tegmentum.wasmtime4j.wasi.WasiFileOperation;
import java.lang.reflect.Field;
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
 * <p>WasiFileSystem provides JNI-based WASI file system operations with comprehensive sandbox
 * security. These tests verify the class structure, method signatures, and API contracts.
 */
@DisplayName("WasiFileSystem Tests")
class WasiFileSystemTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertThat(Modifier.isFinal(WasiFileSystem.class.getModifiers()))
          .as("WasiFileSystem should be final")
          .isTrue();
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertThat(Modifier.isPublic(WasiFileSystem.class.getModifiers()))
          .as("WasiFileSystem should be public")
          .isTrue();
    }

    @Test
    @DisplayName("should have private LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      final Field field = WasiFileSystem.class.getDeclaredField("LOGGER");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have MAX_OPEN_FILES constant")
    void shouldHaveMaxOpenFilesConstant() throws NoSuchFieldException {
      final Field field = WasiFileSystem.class.getDeclaredField("MAX_OPEN_FILES");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("should have DEFAULT_BUFFER_SIZE constant")
    void shouldHaveDefaultBufferSizeConstant() throws NoSuchFieldException {
      final Field field = WasiFileSystem.class.getDeclaredField("DEFAULT_BUFFER_SIZE");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(int.class);
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with WasiContext parameter")
    void shouldHaveWasiContextConstructor() throws NoSuchMethodException {
      final java.lang.reflect.Constructor<?> constructor =
          WasiFileSystem.class.getConstructor(WasiContext.class);
      assertThat(constructor).isNotNull();
      assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("File Operation Method Tests")
  class FileOperationMethodTests {

    @Test
    @DisplayName("should have openFile method with correct signature")
    void shouldHaveOpenFileMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod(
              "openFile", String.class, WasiFileOperation.class, boolean.class, boolean.class);
      assertThat(method.getReturnType()).isEqualTo(int.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have readFile method with correct signature")
    void shouldHaveReadFileMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod("readFile", int.class, byte[].class, int.class, int.class);
      assertThat(method.getReturnType()).isEqualTo(int.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have writeFile method with correct signature")
    void shouldHaveWriteFileMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod(
              "writeFile", int.class, byte[].class, int.class, int.class);
      assertThat(method.getReturnType()).isEqualTo(int.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have seekFile method with correct signature")
    void shouldHaveSeekFileMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod("seekFile", int.class, long.class, int.class);
      assertThat(method.getReturnType()).isEqualTo(long.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have syncFile method with correct signature")
    void shouldHaveSyncFileMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("syncFile", int.class, boolean.class);
      assertThat(method.getReturnType()).isEqualTo(void.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have truncateFile method with correct signature")
    void shouldHaveTruncateFileMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("truncateFile", int.class, long.class);
      assertThat(method.getReturnType()).isEqualTo(void.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have closeFile method with correct signature")
    void shouldHaveCloseFileMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("closeFile", int.class);
      assertThat(method.getReturnType()).isEqualTo(void.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("Metadata Method Tests")
  class MetadataMethodTests {

    @Test
    @DisplayName("should have getFileMetadata method with correct signature")
    void shouldHaveGetFileMetadataMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("getFileMetadata", String.class);
      assertThat(method.getReturnType()).isEqualTo(WasiFileMetadata.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have setFileTimes method with correct signature")
    void shouldHaveSetFileTimesMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod(
              "setFileTimes", String.class, FileTime.class, FileTime.class);
      assertThat(method.getReturnType()).isEqualTo(void.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("Directory Method Tests")
  class DirectoryMethodTests {

    @Test
    @DisplayName("should have listDirectory method with correct signature")
    void shouldHaveListDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("listDirectory", String.class);
      assertThat(method.getReturnType()).isEqualTo(List.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have createDirectory method with correct signature")
    void shouldHaveCreateDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("createDirectory", String.class);
      assertThat(method.getReturnType()).isEqualTo(void.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have removeFileOrDirectory method with correct signature")
    void shouldHaveRemoveFileOrDirectoryMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("removeFileOrDirectory", String.class);
      assertThat(method.getReturnType()).isEqualTo(void.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have renameFileOrDirectory method with correct signature")
    void shouldHaveRenameFileOrDirectoryMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileSystem.class.getMethod("renameFileOrDirectory", String.class, String.class);
      assertThat(method.getReturnType()).isEqualTo(void.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have closeAll method with correct signature")
    void shouldHaveCloseAllMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("closeAll");
      assertThat(method.getReturnType()).isEqualTo(void.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have getOpenFileCount method with correct signature")
    void shouldHaveGetOpenFileCountMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystem.class.getMethod("getOpenFileCount");
      assertThat(method.getReturnType()).isEqualTo(int.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected number of public methods")
    void shouldHaveExpectedPublicMethods() {
      long publicMethodCount =
          java.util.Arrays.stream(WasiFileSystem.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();

      // Expected methods: openFile, readFile, writeFile, seekFile, syncFile, truncateFile,
      // closeFile, getFileMetadata, listDirectory, createDirectory, removeFileOrDirectory,
      // renameFileOrDirectory, setFileTimes, closeAll, getOpenFileCount
      assertThat(publicMethodCount).isGreaterThanOrEqualTo(15);
    }
  }
}
