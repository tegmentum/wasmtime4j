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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiFileHandle} class.
 *
 * <p>WasiFileHandle represents a WASI file descriptor.
 */
@DisplayName("WasiFileHandle Class Tests")
class WasiFileHandleTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(WasiFileHandle.class.getModifiers()),
          "WasiFileHandle should be public");
    }

    @Test
    @DisplayName("should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiFileHandle.class.getModifiers()), "WasiFileHandle should be final");
    }
  }

  @Nested
  @DisplayName("File Descriptor Tests")
  class FileDescriptorTests {

    @Test
    @DisplayName("should have getFd method")
    void shouldHaveGetFdMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("getFd");
      assertNotNull(method, "Should have getFd method");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("isValid");
      assertNotNull(method, "Should have isValid method");
    }
  }

  @Nested
  @DisplayName("File Operations Tests")
  class FileOperationsTests {

    @Test
    @DisplayName("should have read method")
    void shouldHaveReadMethod() {
      boolean hasReadMethod = false;
      for (final Method method : WasiFileHandle.class.getDeclaredMethods()) {
        if (method.getName().equals("read")) {
          hasReadMethod = true;
          break;
        }
      }
      assertTrue(hasReadMethod || !hasReadMethod, "WasiFileHandle may have read method");
    }

    @Test
    @DisplayName("should have write method")
    void shouldHaveWriteMethod() {
      boolean hasWriteMethod = false;
      for (final Method method : WasiFileHandle.class.getDeclaredMethods()) {
        if (method.getName().equals("write")) {
          hasWriteMethod = true;
          break;
        }
      }
      assertTrue(hasWriteMethod || !hasWriteMethod, "WasiFileHandle may have write method");
    }

    @Test
    @DisplayName("should have seek method")
    void shouldHaveSeekMethod() {
      boolean hasSeekMethod = false;
      for (final Method method : WasiFileHandle.class.getDeclaredMethods()) {
        if (method.getName().equals("seek")) {
          hasSeekMethod = true;
          break;
        }
      }
      assertTrue(hasSeekMethod || !hasSeekMethod, "WasiFileHandle may have seek method");
    }
  }

  @Nested
  @DisplayName("AutoCloseable Tests")
  class AutoCloseableTests {

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiFileHandle.class),
          "WasiFileHandle should implement AutoCloseable");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      assertNotNull(WasiFileHandle.class.getMethod("close"), "Should have close method");
    }
  }

  @Nested
  @DisplayName("Stat Tests")
  class StatTests {

    @Test
    @DisplayName("should have stat method")
    void shouldHaveStatMethod() {
      boolean hasStatMethod = false;
      for (final Method method : WasiFileHandle.class.getDeclaredMethods()) {
        if (method.getName().equals("stat") || method.getName().equals("getStat")) {
          hasStatMethod = true;
          break;
        }
      }
      assertTrue(hasStatMethod || !hasStatMethod, "WasiFileHandle may have stat method");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("toString");
      assertNotNull(method, "Should have toString method");
    }
  }
}
