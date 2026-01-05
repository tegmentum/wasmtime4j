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
 * Tests for {@link WasiDirEntry} interface.
 *
 * <p>WasiDirEntry represents an entry in a WASI directory listing.
 */
@DisplayName("WasiDirEntry Tests")
class WasiDirEntryTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiDirEntry.class.getModifiers()), "WasiDirEntry should be public");
      assertTrue(WasiDirEntry.class.isInterface(), "WasiDirEntry should be an interface");
    }
  }

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = WasiDirEntry.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = WasiDirEntry.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(WasiFileType.class, method.getReturnType(), "Should return WasiFileType");
    }

    @Test
    @DisplayName("should have getInode method")
    void shouldHaveGetInodeMethod() throws NoSuchMethodException {
      final Method method = WasiDirEntry.class.getMethod("getInode");
      assertNotNull(method, "getInode method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = WasiDirEntry.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getAccessTime method")
    void shouldHaveGetAccessTimeMethod() throws NoSuchMethodException {
      final Method method = WasiDirEntry.class.getMethod("getAccessTime");
      assertNotNull(method, "getAccessTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getModificationTime method")
    void shouldHaveGetModificationTimeMethod() throws NoSuchMethodException {
      final Method method = WasiDirEntry.class.getMethod("getModificationTime");
      assertNotNull(method, "getModificationTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getCreationTime method")
    void shouldHaveGetCreationTimeMethod() throws NoSuchMethodException {
      final Method method = WasiDirEntry.class.getMethod("getCreationTime");
      assertNotNull(method, "getCreationTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getPermissions method")
    void shouldHaveGetPermissionsMethod() throws NoSuchMethodException {
      final Method method = WasiDirEntry.class.getMethod("getPermissions");
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
      final Method method = WasiDirEntry.class.getMethod("isFile");
      assertNotNull(method, "isFile method should exist");
      assertTrue(method.isDefault(), "isFile should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isDirectory default method")
    void shouldHaveIsDirectoryDefaultMethod() throws NoSuchMethodException {
      final Method method = WasiDirEntry.class.getMethod("isDirectory");
      assertNotNull(method, "isDirectory method should exist");
      assertTrue(method.isDefault(), "isDirectory should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isSymbolicLink default method")
    void shouldHaveIsSymbolicLinkDefaultMethod() throws NoSuchMethodException {
      final Method method = WasiDirEntry.class.getMethod("isSymbolicLink");
      assertNotNull(method, "isSymbolicLink method should exist");
      assertTrue(method.isDefault(), "isSymbolicLink should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isSpecialFile default method")
    void shouldHaveIsSpecialFileDefaultMethod() throws NoSuchMethodException {
      final Method method = WasiDirEntry.class.getMethod("isSpecialFile");
      assertNotNull(method, "isSpecialFile method should exist");
      assertTrue(method.isDefault(), "isSpecialFile should be a default method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Default Method Behavior Tests")
  class DefaultMethodBehaviorTests {

    @Test
    @DisplayName("isFile should return true for REGULAR_FILE type")
    void isFileShouldReturnTrueForRegularFileType() {
      final WasiDirEntry entry = createTestEntry(WasiFileType.REGULAR_FILE);
      assertTrue(entry.isFile(), "Regular file should return true for isFile");
    }

    @Test
    @DisplayName("isFile should return false for DIRECTORY type")
    void isFileShouldReturnFalseForDirectoryType() {
      final WasiDirEntry entry = createTestEntry(WasiFileType.DIRECTORY);
      assertFalse(entry.isFile(), "Directory should return false for isFile");
    }

    @Test
    @DisplayName("isDirectory should return true for DIRECTORY type")
    void isDirectoryShouldReturnTrueForDirectoryType() {
      final WasiDirEntry entry = createTestEntry(WasiFileType.DIRECTORY);
      assertTrue(entry.isDirectory(), "Directory should return true for isDirectory");
    }

    @Test
    @DisplayName("isDirectory should return false for REGULAR_FILE type")
    void isDirectoryShouldReturnFalseForRegularFileType() {
      final WasiDirEntry entry = createTestEntry(WasiFileType.REGULAR_FILE);
      assertFalse(entry.isDirectory(), "Regular file should return false for isDirectory");
    }

    @Test
    @DisplayName("isSymbolicLink should return true for SYMBOLIC_LINK type")
    void isSymbolicLinkShouldReturnTrueForSymbolicLinkType() {
      final WasiDirEntry entry = createTestEntry(WasiFileType.SYMBOLIC_LINK);
      assertTrue(entry.isSymbolicLink(), "Symbolic link should return true for isSymbolicLink");
    }

    @Test
    @DisplayName("isSymbolicLink should return false for REGULAR_FILE type")
    void isSymbolicLinkShouldReturnFalseForRegularFileType() {
      final WasiDirEntry entry = createTestEntry(WasiFileType.REGULAR_FILE);
      assertFalse(entry.isSymbolicLink(), "Regular file should return false for isSymbolicLink");
    }

    @Test
    @DisplayName("isSpecialFile should return true for BLOCK_DEVICE type")
    void isSpecialFileShouldReturnTrueForBlockDeviceType() {
      final WasiDirEntry entry = createTestEntry(WasiFileType.BLOCK_DEVICE);
      assertTrue(entry.isSpecialFile(), "Block device should return true for isSpecialFile");
    }

    @Test
    @DisplayName("isSpecialFile should return true for CHARACTER_DEVICE type")
    void isSpecialFileShouldReturnTrueForCharacterDeviceType() {
      final WasiDirEntry entry = createTestEntry(WasiFileType.CHARACTER_DEVICE);
      assertTrue(entry.isSpecialFile(), "Character device should return true for isSpecialFile");
    }

    @Test
    @DisplayName("isSpecialFile should return true for SOCKET_STREAM type")
    void isSpecialFileShouldReturnTrueForSocketStreamType() {
      final WasiDirEntry entry = createTestEntry(WasiFileType.SOCKET_STREAM);
      assertTrue(entry.isSpecialFile(), "Socket stream should return true for isSpecialFile");
    }

    @Test
    @DisplayName("isSpecialFile should return true for SOCKET_DGRAM type")
    void isSpecialFileShouldReturnTrueForSocketDgramType() {
      final WasiDirEntry entry = createTestEntry(WasiFileType.SOCKET_DGRAM);
      assertTrue(entry.isSpecialFile(), "Socket dgram should return true for isSpecialFile");
    }

    @Test
    @DisplayName("isSpecialFile should return false for REGULAR_FILE type")
    void isSpecialFileShouldReturnFalseForRegularFileType() {
      final WasiDirEntry entry = createTestEntry(WasiFileType.REGULAR_FILE);
      assertFalse(entry.isSpecialFile(), "Regular file should return false for isSpecialFile");
    }

    @Test
    @DisplayName("isSpecialFile should return false for DIRECTORY type")
    void isSpecialFileShouldReturnFalseForDirectoryType() {
      final WasiDirEntry entry = createTestEntry(WasiFileType.DIRECTORY);
      assertFalse(entry.isSpecialFile(), "Directory should return false for isSpecialFile");
    }

    private WasiDirEntry createTestEntry(final WasiFileType fileType) {
      return new WasiDirEntry() {
        @Override
        public String getName() {
          return "test-entry";
        }

        @Override
        public WasiFileType getType() {
          return fileType;
        }

        @Override
        public long getInode() {
          return 12345;
        }

        @Override
        public long getSize() {
          return 1024;
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
