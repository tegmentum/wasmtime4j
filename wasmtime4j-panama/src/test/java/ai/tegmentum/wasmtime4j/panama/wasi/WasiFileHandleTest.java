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

import ai.tegmentum.wasmtime4j.wasi.WasiFileOperation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiFileHandle} class.
 *
 * <p>WasiFileHandle provides file handle wrapper for WASI file system operations with resource
 * management in Panama FFI context.
 */
@DisplayName("WasiFileHandle Tests")
class WasiFileHandleTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiFileHandle.class.getModifiers()),
          "WasiFileHandle should be public");
      assertTrue(
          Modifier.isFinal(WasiFileHandle.class.getModifiers()), "WasiFileHandle should be final");
    }

    @Test
    @DisplayName("should implement AutoCloseable interface")
    void shouldImplementAutoCloseableInterface() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiFileHandle.class),
          "WasiFileHandle should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with all parameters")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiFileHandle.class.getConstructor(
              int.class,
              Path.class,
              SeekableByteChannel.class,
              FileChannel.class,
              WasiFileOperation.class);
      assertNotNull(constructor, "Constructor with all parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getFileDescriptor method")
    void shouldHaveGetFileDescriptorMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("getFileDescriptor");
      assertNotNull(method, "getFileDescriptor method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getPath method")
    void shouldHaveGetPathMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("getPath");
      assertNotNull(method, "getPath method should exist");
      assertEquals(Path.class, method.getReturnType(), "Should return Path");
    }

    @Test
    @DisplayName("should have getChannel method")
    void shouldHaveGetChannelMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("getChannel");
      assertNotNull(method, "getChannel method should exist");
      assertEquals(
          SeekableByteChannel.class, method.getReturnType(), "Should return SeekableByteChannel");
    }

    @Test
    @DisplayName("should have getFileChannel method")
    void shouldHaveGetFileChannelMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("getFileChannel");
      assertNotNull(method, "getFileChannel method should exist");
      assertEquals(FileChannel.class, method.getReturnType(), "Should return FileChannel");
    }

    @Test
    @DisplayName("should have getOperation method")
    void shouldHaveGetOperationMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("getOperation");
      assertNotNull(method, "getOperation method should exist");
      assertEquals(
          WasiFileOperation.class, method.getReturnType(), "Should return WasiFileOperation");
    }
  }

  @Nested
  @DisplayName("State Method Tests")
  class StateMethodTests {

    @Test
    @DisplayName("should have isClosed method")
    void shouldHaveIsClosedMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("isClosed");
      assertNotNull(method, "isClosed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandle.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
