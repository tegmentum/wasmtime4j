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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiStreamOperations} class.
 *
 * <p>WasiStreamOperations provides WASI Preview 2 stream operations (wasi:io/streams). These tests
 * verify class structure, method signatures, and API contracts without native library loading.
 */
@DisplayName("WasiStreamOperations Tests")
class WasiStreamOperationsTest {

  private static final Logger LOGGER = Logger.getLogger(WasiStreamOperationsTest.class.getName());

  /**
   * Loads the class without triggering static initialization.
   *
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit() throws ClassNotFoundException {
    return Class.forName(
        "ai.tegmentum.wasmtime4j.jni.wasi.WasiStreamOperations",
        false,
        getClass().getClassLoader());
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      LOGGER.info("Testing WasiStreamOperations class modifiers");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "WasiStreamOperations should be final");
      LOGGER.info("WasiStreamOperations is correctly marked as final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() throws ClassNotFoundException {
      LOGGER.info("Testing WasiStreamOperations visibility");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "WasiStreamOperations should be public");
      LOGGER.info("WasiStreamOperations is correctly marked as public");
    }

    @Test
    @DisplayName("should have constructor with WasiContext and ExecutorService parameters")
    void shouldHaveRequiredConstructor() throws ClassNotFoundException {
      LOGGER.info("Testing WasiStreamOperations constructor");
      final Class<?> clazz = loadClassWithoutInit();
      final Class<?> wasiContextClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.jni.wasi.WasiContext", false, getClass().getClassLoader());

      boolean hasRequiredConstructor = false;
      for (final Constructor<?> constructor : clazz.getConstructors()) {
        final Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 2
            && params[0] == wasiContextClass
            && params[1] == ExecutorService.class) {
          hasRequiredConstructor = true;
          break;
        }
      }

      assertTrue(
          hasRequiredConstructor,
          "Should have constructor with WasiContext and ExecutorService parameters");
      LOGGER.info("WasiStreamOperations has required constructor");
    }
  }

  @Nested
  @DisplayName("Stream Creation Method Tests")
  class StreamCreationMethodTests {

    @Test
    @DisplayName("should have openInputStream method returning long")
    void shouldHaveOpenInputStreamMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing openInputStream method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("openInputStream", long.class);

      assertNotNull(method, "openInputStream method should exist");
      assertEquals(long.class, method.getReturnType(), "openInputStream should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "openInputStream should be public");
      LOGGER.info("openInputStream method signature verified: " + method);
    }

    @Test
    @DisplayName("should have openOutputStream method returning long")
    void shouldHaveOpenOutputStreamMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing openOutputStream method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("openOutputStream", long.class);

      assertNotNull(method, "openOutputStream method should exist");
      assertEquals(long.class, method.getReturnType(), "openOutputStream should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "openOutputStream should be public");
      LOGGER.info("openOutputStream method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Read/Write Method Tests")
  class ReadWriteMethodTests {

    @Test
    @DisplayName("should have read method returning int")
    void shouldHaveReadMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing read method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("read", long.class, ByteBuffer.class);

      assertNotNull(method, "read method should exist");
      assertEquals(int.class, method.getReturnType(), "read should return int");
      assertTrue(Modifier.isPublic(method.getModifiers()), "read should be public");
      LOGGER.info("read method signature verified: " + method);
    }

    @Test
    @DisplayName("should have write method returning int")
    void shouldHaveWriteMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing write method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("write", long.class, ByteBuffer.class);

      assertNotNull(method, "write method should exist");
      assertEquals(int.class, method.getReturnType(), "write should return int");
      assertTrue(Modifier.isPublic(method.getModifiers()), "write should be public");
      LOGGER.info("write method signature verified: " + method);
    }

    @Test
    @DisplayName("should have flush method returning void")
    void shouldHaveFlushMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing flush method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("flush", long.class);

      assertNotNull(method, "flush method should exist");
      assertEquals(void.class, method.getReturnType(), "flush should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "flush should be public");
      LOGGER.info("flush method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Stream Management Method Tests")
  class StreamManagementMethodTests {

    @Test
    @DisplayName("should have closeStream method returning void")
    void shouldHaveCloseStreamMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing closeStream method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("closeStream", long.class);

      assertNotNull(method, "closeStream method should exist");
      assertEquals(void.class, method.getReturnType(), "closeStream should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "closeStream should be public");
      LOGGER.info("closeStream method signature verified: " + method);
    }

    @Test
    @DisplayName("should have getStreamInfo method")
    void shouldHaveGetStreamInfoMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing getStreamInfo method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("getStreamInfo", long.class);

      assertNotNull(method, "getStreamInfo method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getStreamInfo should be public");
      LOGGER.info("getStreamInfo method signature verified: " + method);
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
    @DisplayName("should have wasiContext field")
    void shouldHaveWasiContextField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing wasiContext field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("wasiContext");

      assertNotNull(field, "wasiContext field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "wasiContext should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "wasiContext should be final");
      LOGGER.info("wasiContext field verified");
    }

    @Test
    @DisplayName("should have activeStreams field")
    void shouldHaveActiveStreamsField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing activeStreams field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("activeStreams");

      assertNotNull(field, "activeStreams field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "activeStreams should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "activeStreams should be final");
      LOGGER.info("activeStreams field verified");
    }

    @Test
    @DisplayName("should have streamHandleGenerator field")
    void shouldHaveStreamHandleGeneratorField()
        throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing streamHandleGenerator field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("streamHandleGenerator");

      assertNotNull(field, "streamHandleGenerator field should exist");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "streamHandleGenerator should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "streamHandleGenerator should be final");
      LOGGER.info("streamHandleGenerator field verified");
    }
  }

  @Nested
  @DisplayName("Inner Type Tests")
  class InnerTypeTests {

    @Test
    @DisplayName("should have StreamType inner enum")
    void shouldHaveStreamTypeEnum() throws ClassNotFoundException {
      LOGGER.info("Testing StreamType inner enum");
      final Class<?> clazz = loadClassWithoutInit();

      Class<?>[] declaredClasses = clazz.getDeclaredClasses();
      boolean foundStreamType = false;

      for (final Class<?> innerClass : declaredClasses) {
        if (innerClass.getSimpleName().equals("StreamType") && innerClass.isEnum()) {
          foundStreamType = true;
          LOGGER.info("Found StreamType enum: " + innerClass.getName());
          break;
        }
      }

      assertTrue(foundStreamType, "Should have StreamType inner enum");
    }

    @Test
    @DisplayName("should have StreamInfo inner class")
    void shouldHaveStreamInfoClass() throws ClassNotFoundException {
      LOGGER.info("Testing StreamInfo inner class");
      final Class<?> clazz = loadClassWithoutInit();

      Class<?>[] declaredClasses = clazz.getDeclaredClasses();
      boolean foundStreamInfo = false;

      for (final Class<?> innerClass : declaredClasses) {
        if (innerClass.getSimpleName().equals("StreamInfo")) {
          foundStreamInfo = true;
          LOGGER.info("Found StreamInfo class: " + innerClass.getName());
          break;
        }
      }

      assertTrue(foundStreamInfo, "Should have StreamInfo inner class");
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
                  "openInputStream",
                  "openOutputStream",
                  "read",
                  "write",
                  "flush",
                  "closeStream",
                  "getStreamInfo",
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
  }
}
