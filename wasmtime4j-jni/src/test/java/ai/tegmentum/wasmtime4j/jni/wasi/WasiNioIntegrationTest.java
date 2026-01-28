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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiNioIntegration} class.
 *
 * <p>WasiNioIntegration provides high-performance NIO file operations including bulk I/O, vectored
 * I/O, async I/O, file transfer, and file locking. These tests verify class structure, method
 * signatures, and API contracts without native library loading.
 */
@DisplayName("WasiNioIntegration Tests")
class WasiNioIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(WasiNioIntegrationTest.class.getName());

  /**
   * Loads the class without triggering static initialization.
   *
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit() throws ClassNotFoundException {
    return Class.forName(
        "ai.tegmentum.wasmtime4j.jni.wasi.WasiNioIntegration", false, getClass().getClassLoader());
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      LOGGER.info("Testing WasiNioIntegration class modifiers");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "WasiNioIntegration should be final");
      LOGGER.info("WasiNioIntegration is correctly marked as final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() throws ClassNotFoundException {
      LOGGER.info("Testing WasiNioIntegration visibility");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "WasiNioIntegration should be public");
      LOGGER.info("WasiNioIntegration is correctly marked as public");
    }

    @Test
    @DisplayName("should have no-arg constructor")
    void shouldHaveNoArgConstructor() throws ClassNotFoundException {
      LOGGER.info("Testing WasiNioIntegration constructor");
      final Class<?> clazz = loadClassWithoutInit();

      boolean hasNoArgConstructor = false;
      for (final Constructor<?> constructor : clazz.getConstructors()) {
        if (constructor.getParameterCount() == 0) {
          hasNoArgConstructor = true;
          break;
        }
      }

      assertTrue(hasNoArgConstructor, "Should have no-arg constructor");
      LOGGER.info("WasiNioIntegration has no-arg constructor");
    }
  }

  @Nested
  @DisplayName("Bulk I/O Method Tests")
  class BulkIoMethodTests {

    @Test
    @DisplayName("should have bulkRead method returning ByteBuffer")
    void shouldHaveBulkReadMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing bulkRead method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("bulkRead", FileChannel.class, long.class, int.class);

      assertNotNull(method, "bulkRead method should exist");
      assertEquals(ByteBuffer.class, method.getReturnType(), "bulkRead should return ByteBuffer");
      assertTrue(Modifier.isPublic(method.getModifiers()), "bulkRead should be public");
      LOGGER.info("bulkRead method signature verified: " + method);
    }

    @Test
    @DisplayName("should have bulkWrite method returning int")
    void shouldHaveBulkWriteMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing bulkWrite method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("bulkWrite", FileChannel.class, long.class, ByteBuffer.class);

      assertNotNull(method, "bulkWrite method should exist");
      assertEquals(int.class, method.getReturnType(), "bulkWrite should return int");
      assertTrue(Modifier.isPublic(method.getModifiers()), "bulkWrite should be public");
      LOGGER.info("bulkWrite method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Vectored I/O Method Tests")
  class VectoredIoMethodTests {

    @Test
    @DisplayName("should have vectoredRead method returning long")
    void shouldHaveVectoredReadMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing vectoredRead method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("vectoredRead", FileChannel.class, ByteBuffer[].class, long.class);

      assertNotNull(method, "vectoredRead method should exist");
      assertEquals(long.class, method.getReturnType(), "vectoredRead should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "vectoredRead should be public");
      LOGGER.info("vectoredRead method signature verified: " + method);
    }

    @Test
    @DisplayName("should have vectoredWrite method returning long")
    void shouldHaveVectoredWriteMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing vectoredWrite method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("vectoredWrite", FileChannel.class, ByteBuffer[].class, long.class);

      assertNotNull(method, "vectoredWrite method should exist");
      assertEquals(long.class, method.getReturnType(), "vectoredWrite should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "vectoredWrite should be public");
      LOGGER.info("vectoredWrite method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Async I/O Method Tests")
  class AsyncIoMethodTests {

    @Test
    @DisplayName("should have asyncRead method returning CompletableFuture<ByteBuffer>")
    void shouldHaveAsyncReadMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing asyncRead method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("asyncRead", Path.class, long.class, int.class);

      assertNotNull(method, "asyncRead method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "asyncRead should return CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "asyncRead should be public");
      LOGGER.info("asyncRead method signature verified: " + method);
    }

    @Test
    @DisplayName("should have asyncWrite method returning CompletableFuture<Integer>")
    void shouldHaveAsyncWriteMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing asyncWrite method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("asyncWrite", Path.class, long.class, ByteBuffer.class);

      assertNotNull(method, "asyncWrite method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "asyncWrite should return CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "asyncWrite should be public");
      LOGGER.info("asyncWrite method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("File Transfer Method Tests")
  class FileTransferMethodTests {

    @Test
    @DisplayName("should have transferFile method returning long")
    void shouldHaveTransferFileMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing transferFile method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod(
              "transferFile",
              ReadableByteChannel.class,
              long.class,
              long.class,
              WritableByteChannel.class);

      assertNotNull(method, "transferFile method should exist");
      assertEquals(long.class, method.getReturnType(), "transferFile should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "transferFile should be public");
      assertEquals(4, method.getParameterCount(), "transferFile should have 4 parameters");
      LOGGER.info("transferFile method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("File Locking Method Tests")
  class FileLockingMethodTests {

    @Test
    @DisplayName("should have lockFile method returning FileLock")
    void shouldHaveLockFileMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing lockFile method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("lockFile", FileChannel.class, long.class, long.class, boolean.class);

      assertNotNull(method, "lockFile method should exist");
      assertEquals(FileLock.class, method.getReturnType(), "lockFile should return FileLock");
      assertTrue(Modifier.isPublic(method.getModifiers()), "lockFile should be public");
      LOGGER.info("lockFile method signature verified: " + method);
    }

    @Test
    @DisplayName("should have tryLockFile method returning FileLock")
    void shouldHaveTryLockFileMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing tryLockFile method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("tryLockFile", FileChannel.class, long.class, long.class, boolean.class);

      assertNotNull(method, "tryLockFile method should exist");
      assertEquals(FileLock.class, method.getReturnType(), "tryLockFile should return FileLock");
      assertTrue(Modifier.isPublic(method.getModifiers()), "tryLockFile should be public");
      LOGGER.info("tryLockFile method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Shutdown Method Tests")
  class ShutdownMethodTests {

    @Test
    @DisplayName("should have shutdown method returning void")
    void shouldHaveShutdownMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing shutdown method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("shutdown");

      assertNotNull(method, "shutdown method should exist");
      assertEquals(void.class, method.getReturnType(), "shutdown should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "shutdown should be public");
      LOGGER.info("shutdown method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Private Field Tests")
  class PrivateFieldTests {

    @Test
    @DisplayName("should have LOGGER field")
    void shouldHaveLoggerField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing LOGGER field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("LOGGER");

      assertNotNull(field, "LOGGER field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      assertEquals(Logger.class, field.getType(), "LOGGER should be Logger type");
      LOGGER.info("LOGGER field verified");
    }

    @Test
    @DisplayName("should have DEFAULT_BUFFER_SIZE constant")
    void shouldHaveDefaultBufferSizeConstant() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing DEFAULT_BUFFER_SIZE constant");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("DEFAULT_BUFFER_SIZE");

      assertNotNull(field, "DEFAULT_BUFFER_SIZE field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "DEFAULT_BUFFER_SIZE should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "DEFAULT_BUFFER_SIZE should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "DEFAULT_BUFFER_SIZE should be final");
      assertEquals(int.class, field.getType(), "DEFAULT_BUFFER_SIZE should be int type");
      LOGGER.info("DEFAULT_BUFFER_SIZE constant verified");
    }

    @Test
    @DisplayName("should have MAX_MEMORY_MAP_SIZE constant")
    void shouldHaveMaxMemoryMapSizeConstant() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing MAX_MEMORY_MAP_SIZE constant");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("MAX_MEMORY_MAP_SIZE");

      assertNotNull(field, "MAX_MEMORY_MAP_SIZE field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "MAX_MEMORY_MAP_SIZE should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "MAX_MEMORY_MAP_SIZE should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "MAX_MEMORY_MAP_SIZE should be final");
      assertEquals(int.class, field.getType(), "MAX_MEMORY_MAP_SIZE should be int type");
      LOGGER.info("MAX_MEMORY_MAP_SIZE constant verified");
    }
  }

  @Nested
  @DisplayName("Method Count Verification Tests")
  class MethodCountVerificationTests {

    @Test
    @DisplayName("should have expected public methods")
    void shouldHaveExpectedPublicMethods() throws ClassNotFoundException {
      LOGGER.info("Testing public method count");
      final Class<?> clazz = loadClassWithoutInit();

      final Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "bulkRead",
                  "bulkWrite",
                  "vectoredRead",
                  "vectoredWrite",
                  "asyncRead",
                  "asyncWrite",
                  "transferFile",
                  "lockFile",
                  "tryLockFile",
                  "shutdown"));

      int foundMethodCount = 0;
      for (final Method method : clazz.getMethods()) {
        if (method.getDeclaringClass() == clazz && Modifier.isPublic(method.getModifiers())) {
          foundMethodCount++;
          LOGGER.info("Found public method: " + method.getName());
        }
      }

      assertTrue(
          foundMethodCount >= expectedMethods.size(),
          "Should have at least "
              + expectedMethods.size()
              + " public methods, found: "
              + foundMethodCount);
      LOGGER.info("Public method count verified: " + foundMethodCount);
    }

    @Test
    @DisplayName("should have async methods returning CompletableFuture")
    void shouldHaveAsyncMethods() throws ClassNotFoundException {
      LOGGER.info("Testing async method signatures");
      final Class<?> clazz = loadClassWithoutInit();

      int asyncMethodCount = 0;
      for (final Method method : clazz.getMethods()) {
        if (method.getDeclaringClass() == clazz
            && Modifier.isPublic(method.getModifiers())
            && method.getReturnType() == CompletableFuture.class) {
          asyncMethodCount++;
          LOGGER.info("Found async method: " + method.getName());
        }
      }

      assertTrue(
          asyncMethodCount >= 2, "Should have at least 2 async methods (asyncRead, asyncWrite)");
      LOGGER.info("Async method count verified: " + asyncMethodCount);
    }
  }
}
