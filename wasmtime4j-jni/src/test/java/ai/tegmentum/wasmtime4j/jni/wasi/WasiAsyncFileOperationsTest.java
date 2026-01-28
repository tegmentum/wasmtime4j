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
import java.nio.channels.SelectableChannel;
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
 * Tests for {@link WasiAsyncFileOperations} class.
 *
 * <p>WasiAsyncFileOperations provides async/non-blocking file I/O operations. These tests verify
 * class structure, method signatures, and API contracts without native library loading.
 */
@DisplayName("WasiAsyncFileOperations Tests")
class WasiAsyncFileOperationsTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiAsyncFileOperationsTest.class.getName());

  /**
   * Loads the class without triggering static initialization.
   *
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit() throws ClassNotFoundException {
    return Class.forName(
        "ai.tegmentum.wasmtime4j.jni.wasi.WasiAsyncFileOperations",
        false,
        getClass().getClassLoader());
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      LOGGER.info("Testing WasiAsyncFileOperations class modifiers");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "WasiAsyncFileOperations should be final");
      LOGGER.info("WasiAsyncFileOperations is correctly marked as final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() throws ClassNotFoundException {
      LOGGER.info("Testing WasiAsyncFileOperations visibility");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(
          Modifier.isPublic(clazz.getModifiers()), "WasiAsyncFileOperations should be public");
      LOGGER.info("WasiAsyncFileOperations is correctly marked as public");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() throws ClassNotFoundException {
      LOGGER.info("Testing AutoCloseable interface implementation");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(
          AutoCloseable.class.isAssignableFrom(clazz),
          "WasiAsyncFileOperations should implement AutoCloseable");
      LOGGER.info("WasiAsyncFileOperations implements AutoCloseable");
    }

    @Test
    @DisplayName("should have no-arg constructor")
    void shouldHaveNoArgConstructor() throws ClassNotFoundException {
      LOGGER.info("Testing WasiAsyncFileOperations constructor");
      final Class<?> clazz = loadClassWithoutInit();

      boolean hasNoArgConstructor = false;
      for (final Constructor<?> constructor : clazz.getConstructors()) {
        if (constructor.getParameterCount() == 0) {
          hasNoArgConstructor = true;
          break;
        }
      }

      assertTrue(hasNoArgConstructor, "Should have no-arg constructor");
      LOGGER.info("WasiAsyncFileOperations has no-arg constructor");
    }
  }

  @Nested
  @DisplayName("Async Read Method Tests")
  class AsyncReadMethodTests {

    @Test
    @DisplayName("should have readAsync method with basic parameters")
    void shouldHaveReadAsyncBasicMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing readAsync basic method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("readAsync", Path.class, long.class, int.class);

      assertNotNull(method, "readAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "readAsync should return CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "readAsync should be public");
      LOGGER.info("readAsync basic method signature verified: " + method);
    }

    @Test
    @DisplayName("should have readAsync method with timeout parameter")
    void shouldHaveReadAsyncWithTimeoutMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing readAsync with timeout method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("readAsync", Path.class, long.class, int.class, long.class);

      assertNotNull(method, "readAsync with timeout method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "readAsync should return CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "readAsync should be public");
      assertEquals(4, method.getParameterCount(), "readAsync with timeout should have 4 params");
      LOGGER.info("readAsync with timeout method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Async Write Method Tests")
  class AsyncWriteMethodTests {

    @Test
    @DisplayName("should have writeAsync method with basic parameters")
    void shouldHaveWriteAsyncBasicMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing writeAsync basic method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("writeAsync", Path.class, long.class, ByteBuffer.class);

      assertNotNull(method, "writeAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "writeAsync should return CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "writeAsync should be public");
      LOGGER.info("writeAsync basic method signature verified: " + method);
    }

    @Test
    @DisplayName("should have writeAsync method with timeout parameter")
    void shouldHaveWriteAsyncWithTimeoutMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing writeAsync with timeout method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("writeAsync", Path.class, long.class, ByteBuffer.class, long.class);

      assertNotNull(method, "writeAsync with timeout method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "writeAsync should return CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "writeAsync should be public");
      assertEquals(4, method.getParameterCount(), "writeAsync with timeout should have 4 params");
      LOGGER.info("writeAsync with timeout method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Non-Blocking I/O Method Tests")
  class NonBlockingIoMethodTests {

    @Test
    @DisplayName("should have registerChannel method returning CompletableFuture<SelectionKey>")
    void shouldHaveRegisterChannelMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing registerChannel method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("registerChannel", SelectableChannel.class, int.class, Object.class);

      assertNotNull(method, "registerChannel method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "registerChannel should return CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "registerChannel should be public");
      LOGGER.info("registerChannel method signature verified: " + method);
    }

    @Test
    @DisplayName("should have readNonBlocking method returning int")
    void shouldHaveReadNonBlockingMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing readNonBlocking method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("readNonBlocking", SelectableChannel.class, ByteBuffer.class);

      assertNotNull(method, "readNonBlocking method should exist");
      assertEquals(int.class, method.getReturnType(), "readNonBlocking should return int");
      assertTrue(Modifier.isPublic(method.getModifiers()), "readNonBlocking should be public");
      LOGGER.info("readNonBlocking method signature verified: " + method);
    }

    @Test
    @DisplayName("should have writeNonBlocking method returning int")
    void shouldHaveWriteNonBlockingMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing writeNonBlocking method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("writeNonBlocking", SelectableChannel.class, ByteBuffer.class);

      assertNotNull(method, "writeNonBlocking method should exist");
      assertEquals(int.class, method.getReturnType(), "writeNonBlocking should return int");
      assertTrue(Modifier.isPublic(method.getModifiers()), "writeNonBlocking should be public");
      LOGGER.info("writeNonBlocking method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Concurrency Method Tests")
  class ConcurrencyMethodTests {

    @Test
    @DisplayName("should have getActiveOperationCount method returning long")
    void shouldHaveGetActiveOperationCountMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing getActiveOperationCount method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("getActiveOperationCount");

      assertNotNull(method, "getActiveOperationCount method should exist");
      assertEquals(
          long.class, method.getReturnType(), "getActiveOperationCount should return long");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "getActiveOperationCount should be public");
      LOGGER.info("getActiveOperationCount method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Shutdown Method Tests")
  class ShutdownMethodTests {

    @Test
    @DisplayName("should have isClosed method returning boolean")
    void shouldHaveIsClosedMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing isClosed method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("isClosed");

      assertNotNull(method, "isClosed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isClosed should return boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isClosed should be public");
      LOGGER.info("isClosed method signature verified: " + method);
    }

    @Test
    @DisplayName("should have close method returning void")
    void shouldHaveCloseMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing close method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("close");

      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "close should be public");
      LOGGER.info("close method signature verified: " + method);
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
    @DisplayName("should have DEFAULT_TIMEOUT_SECONDS constant")
    void shouldHaveDefaultTimeoutConstant() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing DEFAULT_TIMEOUT_SECONDS constant");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("DEFAULT_TIMEOUT_SECONDS");

      assertNotNull(field, "DEFAULT_TIMEOUT_SECONDS field should exist");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "DEFAULT_TIMEOUT_SECONDS should be private");
      assertTrue(
          Modifier.isStatic(field.getModifiers()), "DEFAULT_TIMEOUT_SECONDS should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "DEFAULT_TIMEOUT_SECONDS should be final");
      assertEquals(int.class, field.getType(), "DEFAULT_TIMEOUT_SECONDS should be int type");
      LOGGER.info("DEFAULT_TIMEOUT_SECONDS constant verified");
    }

    @Test
    @DisplayName("should have MAX_CONCURRENT_OPERATIONS constant")
    void shouldHaveMaxConcurrentOpsConstant() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing MAX_CONCURRENT_OPERATIONS constant");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("MAX_CONCURRENT_OPERATIONS");

      assertNotNull(field, "MAX_CONCURRENT_OPERATIONS field should exist");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "MAX_CONCURRENT_OPERATIONS should be private");
      assertTrue(
          Modifier.isStatic(field.getModifiers()), "MAX_CONCURRENT_OPERATIONS should be static");
      assertTrue(
          Modifier.isFinal(field.getModifiers()), "MAX_CONCURRENT_OPERATIONS should be final");
      assertEquals(int.class, field.getType(), "MAX_CONCURRENT_OPERATIONS should be int type");
      LOGGER.info("MAX_CONCURRENT_OPERATIONS constant verified");
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
                  "readAsync",
                  "writeAsync",
                  "registerChannel",
                  "readNonBlocking",
                  "writeNonBlocking",
                  "getActiveOperationCount",
                  "isClosed",
                  "close"));

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
          asyncMethodCount >= 3,
          "Should have at least 3 async methods (readAsync, writeAsync, registerChannel)");
      LOGGER.info("Async method count verified: " + asyncMethodCount);
    }
  }
}
