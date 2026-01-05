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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiFileStat} class.
 *
 * <p>WasiFileStat represents file metadata returned by WASI filestat operations, including device
 * ID, inode, file type, link count, size, and various timestamps.
 */
@DisplayName("WasiFileStat Tests")
class WasiFileStatTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiFileStat.class.getModifiers()), "WasiFileStat should be public");
      assertTrue(
          Modifier.isFinal(WasiFileStat.class.getModifiers()), "WasiFileStat should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with all parameters")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiFileStat.class.getConstructor(
              long.class, // device
              long.class, // inode
              int.class, // fileType
              long.class, // linkCount
              long.class, // size
              long.class, // accessTime
              long.class, // modificationTime
              long.class // changeTime
              );
      assertNotNull(constructor, "Constructor with all parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Device and Inode Method Tests")
  class DeviceAndInodeMethodTests {

    @Test
    @DisplayName("should have getDevice method")
    void shouldHaveGetDeviceMethod() throws NoSuchMethodException {
      final Method method = WasiFileStat.class.getMethod("getDevice");
      assertNotNull(method, "getDevice method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getInode method")
    void shouldHaveGetInodeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStat.class.getMethod("getInode");
      assertNotNull(method, "getInode method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("File Type and Link Count Method Tests")
  class FileTypeAndLinkCountMethodTests {

    @Test
    @DisplayName("should have getFileType method")
    void shouldHaveGetFileTypeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStat.class.getMethod("getFileType");
      assertNotNull(method, "getFileType method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getLinkCount method")
    void shouldHaveGetLinkCountMethod() throws NoSuchMethodException {
      final Method method = WasiFileStat.class.getMethod("getLinkCount");
      assertNotNull(method, "getLinkCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Size Method Tests")
  class SizeMethodTests {

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStat.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Timestamp Method Tests")
  class TimestampMethodTests {

    @Test
    @DisplayName("should have getAccessTime method")
    void shouldHaveGetAccessTimeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStat.class.getMethod("getAccessTime");
      assertNotNull(method, "getAccessTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getModificationTime method")
    void shouldHaveGetModificationTimeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStat.class.getMethod("getModificationTime");
      assertNotNull(method, "getModificationTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getChangeTime method")
    void shouldHaveGetChangeTimeMethod() throws NoSuchMethodException {
      final Method method = WasiFileStat.class.getMethod("getChangeTime");
      assertNotNull(method, "getChangeTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("ToString Method Tests")
  class ToStringMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = WasiFileStat.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
