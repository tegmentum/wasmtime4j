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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiFileStats} interface.
 *
 * <p>WasiFileStats provides comprehensive metadata information about files and directories.
 */
@DisplayName("WasiFileStats Tests")
class WasiFileStatsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiFileStats.class.getModifiers()), "WasiFileStats should be public");
      assertTrue(WasiFileStats.class.isInterface(), "WasiFileStats should be an interface");
    }

    @Test
    @DisplayName("should have Builder nested interface")
    void shouldHaveBuilderNestedInterface() {
      final var nestedClasses = WasiFileStats.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("Builder")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "Builder should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have Builder nested interface");
    }
  }

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getDevice method")
    void shouldHaveGetDeviceMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("getDevice");
      assertNotNull(method, "getDevice method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getInode method")
    void shouldHaveGetInodeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("getInode");
      assertNotNull(method, "getInode method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getFileType method")
    void shouldHaveGetFileTypeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("getFileType");
      assertNotNull(method, "getFileType method should exist");
      assertEquals(WasiFileType.class, method.getReturnType(), "Should return WasiFileType");
    }

    @Test
    @DisplayName("should have getLinkCount method")
    void shouldHaveGetLinkCountMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("getLinkCount");
      assertNotNull(method, "getLinkCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getAccessTime method")
    void shouldHaveGetAccessTimeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("getAccessTime");
      assertNotNull(method, "getAccessTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getModificationTime method")
    void shouldHaveGetModificationTimeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("getModificationTime");
      assertNotNull(method, "getModificationTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getStatusChangeTime method")
    void shouldHaveGetStatusChangeTimeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("getStatusChangeTime");
      assertNotNull(method, "getStatusChangeTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getCreationTime method")
    void shouldHaveGetCreationTimeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("getCreationTime");
      assertNotNull(method, "getCreationTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getPermissions method")
    void shouldHaveGetPermissionsMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("getPermissions");
      assertNotNull(method, "getPermissions method should exist");
      assertEquals(WasiPermissions.class, method.getReturnType(), "Should return WasiPermissions");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have isFile default method")
    void shouldHaveIsFileDefaultMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("isFile");
      assertNotNull(method, "isFile method should exist");
      assertTrue(method.isDefault(), "isFile should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isDirectory default method")
    void shouldHaveIsDirectoryDefaultMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("isDirectory");
      assertNotNull(method, "isDirectory method should exist");
      assertTrue(method.isDefault(), "isDirectory should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isSymbolicLink default method")
    void shouldHaveIsSymbolicLinkDefaultMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("isSymbolicLink");
      assertNotNull(method, "isSymbolicLink method should exist");
      assertTrue(method.isDefault(), "isSymbolicLink should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isSpecialFile default method")
    void shouldHaveIsSpecialFileDefaultMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("isSpecialFile");
      assertNotNull(method, "isSpecialFile method should exist");
      assertTrue(method.isDefault(), "isSpecialFile should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderStaticMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(WasiFileStats.Builder.class, method.getReturnType(), "Should return Builder");
    }
  }

  @Nested
  @DisplayName("Builder Interface Method Tests")
  class BuilderInterfaceMethodTests {

    @Test
    @DisplayName("Builder should have device method")
    void builderShouldHaveDeviceMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.Builder.class.getMethod("device", long.class);
      assertNotNull(method, "device method should exist");
      assertEquals(WasiFileStats.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have inode method")
    void builderShouldHaveInodeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.Builder.class.getMethod("inode", long.class);
      assertNotNull(method, "inode method should exist");
      assertEquals(WasiFileStats.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have fileType method")
    void builderShouldHaveFileTypeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.Builder.class.getMethod("fileType", WasiFileType.class);
      assertNotNull(method, "fileType method should exist");
      assertEquals(WasiFileStats.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have linkCount method")
    void builderShouldHaveLinkCountMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.Builder.class.getMethod("linkCount", long.class);
      assertNotNull(method, "linkCount method should exist");
      assertEquals(WasiFileStats.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have size method")
    void builderShouldHaveSizeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.Builder.class.getMethod("size", long.class);
      assertNotNull(method, "size method should exist");
      assertEquals(WasiFileStats.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have accessTime method")
    void builderShouldHaveAccessTimeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.Builder.class.getMethod("accessTime", Instant.class);
      assertNotNull(method, "accessTime method should exist");
      assertEquals(WasiFileStats.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have modificationTime method")
    void builderShouldHaveModificationTimeMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileStats.Builder.class.getMethod("modificationTime", Instant.class);
      assertNotNull(method, "modificationTime method should exist");
      assertEquals(WasiFileStats.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have statusChangeTime method")
    void builderShouldHaveStatusChangeTimeMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileStats.Builder.class.getMethod("statusChangeTime", Instant.class);
      assertNotNull(method, "statusChangeTime method should exist");
      assertEquals(WasiFileStats.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have creationTime method")
    void builderShouldHaveCreationTimeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.Builder.class.getMethod("creationTime", Instant.class);
      assertNotNull(method, "creationTime method should exist");
      assertEquals(WasiFileStats.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have permissions method")
    void builderShouldHavePermissionsMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileStats.Builder.class.getMethod("permissions", WasiPermissions.class);
      assertNotNull(method, "permissions method should exist");
      assertEquals(WasiFileStats.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = WasiFileStats.Builder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(WasiFileStats.class, method.getReturnType(), "Should return WasiFileStats");
    }
  }

  @Nested
  @DisplayName("Builder Factory Tests")
  class BuilderFactoryTests {

    @Test
    @DisplayName("builder should create non-null builder")
    void builderShouldCreateNonNullBuilder() {
      final WasiFileStats.Builder builder = WasiFileStats.builder();
      assertNotNull(builder, "Builder should not be null");
    }
  }

  @Nested
  @DisplayName("Default Method Behavior Tests")
  class DefaultMethodBehaviorTests {

    @Test
    @DisplayName("isFile should delegate to fileType")
    void isFileShouldDelegateToFileType() {
      // Create a test implementation
      final WasiFileStats regularFileStats = createTestStats(WasiFileType.REGULAR_FILE);
      final WasiFileStats directoryStats = createTestStats(WasiFileType.DIRECTORY);

      assertTrue(regularFileStats.isFile(), "Regular file should return true for isFile");
      assertFalse(directoryStats.isFile(), "Directory should return false for isFile");
    }

    @Test
    @DisplayName("isDirectory should delegate to fileType")
    void isDirectoryShouldDelegateToFileType() {
      final WasiFileStats regularFileStats = createTestStats(WasiFileType.REGULAR_FILE);
      final WasiFileStats directoryStats = createTestStats(WasiFileType.DIRECTORY);

      assertFalse(
          regularFileStats.isDirectory(), "Regular file should return false for isDirectory");
      assertTrue(directoryStats.isDirectory(), "Directory should return true for isDirectory");
    }

    @Test
    @DisplayName("isSymbolicLink should delegate to fileType")
    void isSymbolicLinkShouldDelegateToFileType() {
      final WasiFileStats regularFileStats = createTestStats(WasiFileType.REGULAR_FILE);
      final WasiFileStats symlinkStats = createTestStats(WasiFileType.SYMBOLIC_LINK);

      assertFalse(
          regularFileStats.isSymbolicLink(), "Regular file should return false for isSymbolicLink");
      assertTrue(symlinkStats.isSymbolicLink(), "Symlink should return true for isSymbolicLink");
    }

    @Test
    @DisplayName("isSpecialFile should delegate to fileType")
    void isSpecialFileShouldDelegateToFileType() {
      final WasiFileStats regularFileStats = createTestStats(WasiFileType.REGULAR_FILE);
      final WasiFileStats socketStats = createTestStats(WasiFileType.SOCKET_STREAM);
      final WasiFileStats blockDeviceStats = createTestStats(WasiFileType.BLOCK_DEVICE);

      assertFalse(
          regularFileStats.isSpecialFile(), "Regular file should return false for isSpecialFile");
      assertTrue(socketStats.isSpecialFile(), "Socket should return true for isSpecialFile");
      assertTrue(
          blockDeviceStats.isSpecialFile(), "Block device should return true for isSpecialFile");
    }

    private WasiFileStats createTestStats(final WasiFileType fileType) {
      return new WasiFileStats() {
        @Override
        public long getDevice() {
          return 0;
        }

        @Override
        public long getInode() {
          return 0;
        }

        @Override
        public WasiFileType getFileType() {
          return fileType;
        }

        @Override
        public long getLinkCount() {
          return 1;
        }

        @Override
        public long getSize() {
          return 0;
        }

        @Override
        public Instant getAccessTime() {
          return null;
        }

        @Override
        public Instant getModificationTime() {
          return null;
        }

        @Override
        public Instant getStatusChangeTime() {
          return null;
        }

        @Override
        public Instant getCreationTime() {
          return null;
        }

        @Override
        public WasiPermissions getPermissions() {
          return null;
        }
      };
    }
  }
}
