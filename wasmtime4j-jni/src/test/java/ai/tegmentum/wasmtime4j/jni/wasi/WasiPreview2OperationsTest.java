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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiPreview2Operations} class.
 *
 * <p>WasiPreview2Operations provides asynchronous WASI Preview 2 system interface operations with
 * WIT interface support. These tests verify class structure, method signatures, and API contracts
 * without native library loading.
 */
@DisplayName("WasiPreview2Operations Tests")
class WasiPreview2OperationsTest {

  private static final Logger LOGGER = Logger.getLogger(WasiPreview2OperationsTest.class.getName());

  /**
   * Loads the class without triggering static initialization.
   *
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit() throws ClassNotFoundException {
    return Class.forName(
        "ai.tegmentum.wasmtime4j.jni.wasi.WasiPreview2Operations",
        false,
        getClass().getClassLoader());
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      LOGGER.info("Testing WasiPreview2Operations class modifiers");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(
          Modifier.isFinal(clazz.getModifiers()), "WasiPreview2Operations should be final");
      LOGGER.info("WasiPreview2Operations is correctly marked as final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() throws ClassNotFoundException {
      LOGGER.info("Testing WasiPreview2Operations visibility");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(
          Modifier.isPublic(clazz.getModifiers()), "WasiPreview2Operations should be public");
      LOGGER.info("WasiPreview2Operations is correctly marked as public");
    }

    @Test
    @DisplayName("should have constructor with WasiContext parameter")
    void shouldHaveConstructorWithWasiContextParameter() throws ClassNotFoundException {
      LOGGER.info("Testing WasiPreview2Operations constructor");
      final Class<?> clazz = loadClassWithoutInit();
      final Class<?> wasiContextClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.jni.wasi.WasiContext", false, getClass().getClassLoader());

      boolean hasRequiredConstructor = false;
      for (final Constructor<?> constructor : clazz.getConstructors()) {
        final Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 1 && params[0] == wasiContextClass) {
          hasRequiredConstructor = true;
          break;
        }
      }

      assertTrue(hasRequiredConstructor, "Should have constructor with WasiContext parameter");
      LOGGER.info("WasiPreview2Operations has required constructor");
    }
  }

  @Nested
  @DisplayName("Resource Lifecycle Method Tests")
  class ResourceLifecycleMethodTests {

    @Test
    @DisplayName("should have createResource method returning long")
    void shouldHaveCreateResourceMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing createResource method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("createResource", String.class, ByteBuffer.class);

      assertNotNull(method, "createResource method should exist");
      assertEquals(long.class, method.getReturnType(), "createResource should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "createResource should be public");
      LOGGER.info("createResource method signature verified: " + method);
    }

    @Test
    @DisplayName("should have destroyResource method returning void")
    void shouldHaveDestroyResourceMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing destroyResource method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("destroyResource", long.class);

      assertNotNull(method, "destroyResource method should exist");
      assertEquals(void.class, method.getReturnType(), "destroyResource should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "destroyResource should be public");
      LOGGER.info("destroyResource method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Stream Operation Method Tests")
  class StreamOperationMethodTests {

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

    @Test
    @DisplayName("should have readAsync method returning CompletableFuture<Integer>")
    void shouldHaveReadAsyncMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing readAsync method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("readAsync", long.class, ByteBuffer.class);

      assertNotNull(method, "readAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "readAsync should return CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "readAsync should be public");
      LOGGER.info("readAsync method signature verified: " + method);
    }

    @Test
    @DisplayName("should have writeAsync method returning CompletableFuture<Integer>")
    void shouldHaveWriteAsyncMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing writeAsync method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("writeAsync", long.class, ByteBuffer.class);

      assertNotNull(method, "writeAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "writeAsync should return CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "writeAsync should be public");
      LOGGER.info("writeAsync method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("TCP Socket Method Tests")
  class TcpSocketMethodTests {

    @Test
    @DisplayName("should have createTcpSocket method returning long")
    void shouldHaveCreateTcpSocketMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing createTcpSocket method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("createTcpSocket", int.class);

      assertNotNull(method, "createTcpSocket method should exist");
      assertEquals(long.class, method.getReturnType(), "createTcpSocket should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "createTcpSocket should be public");
      LOGGER.info("createTcpSocket method signature verified: " + method);
    }

    @Test
    @DisplayName("should have connectTcpAsync method returning CompletableFuture<Void>")
    void shouldHaveConnectTcpAsyncMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing connectTcpAsync method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("connectTcpAsync", long.class, String.class, int.class);

      assertNotNull(method, "connectTcpAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "connectTcpAsync should return CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "connectTcpAsync should be public");
      LOGGER.info("connectTcpAsync method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("UDP Socket Method Tests")
  class UdpSocketMethodTests {

    @Test
    @DisplayName("should have createUdpSocket method returning long")
    void shouldHaveCreateUdpSocketMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing createUdpSocket method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("createUdpSocket", int.class);

      assertNotNull(method, "createUdpSocket method should exist");
      assertEquals(long.class, method.getReturnType(), "createUdpSocket should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "createUdpSocket should be public");
      LOGGER.info("createUdpSocket method signature verified: " + method);
    }

    @Test
    @DisplayName("should have sendUdpAsync method returning CompletableFuture<Void>")
    void shouldHaveSendUdpAsyncMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing sendUdpAsync method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("sendUdpAsync", long.class, ByteBuffer.class, String.class, int.class);

      assertNotNull(method, "sendUdpAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "sendUdpAsync should return CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "sendUdpAsync should be public");
      LOGGER.info("sendUdpAsync method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Polling Method Tests")
  class PollingMethodTests {

    @Test
    @DisplayName("should have poll method returning List<Integer>")
    void shouldHavePollMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing poll method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("poll", List.class, long.class);

      assertNotNull(method, "poll method should exist");
      assertEquals(List.class, method.getReturnType(), "poll should return List");
      assertTrue(Modifier.isPublic(method.getModifiers()), "poll should be public");
      LOGGER.info("poll method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Shutdown Method Tests")
  class ShutdownMethodTests {

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
    @DisplayName("should have MAX_ASYNC_OPERATIONS constant")
    void shouldHaveMaxAsyncOperationsConstant() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing MAX_ASYNC_OPERATIONS constant");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("MAX_ASYNC_OPERATIONS");

      assertNotNull(field, "MAX_ASYNC_OPERATIONS field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "MAX_ASYNC_OPERATIONS should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "MAX_ASYNC_OPERATIONS should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "MAX_ASYNC_OPERATIONS should be final");
      assertEquals(int.class, field.getType(), "MAX_ASYNC_OPERATIONS should be int type");
      LOGGER.info("MAX_ASYNC_OPERATIONS constant verified");
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
    @DisplayName("should have asyncExecutor field")
    void shouldHaveAsyncExecutorField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing asyncExecutor field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("asyncExecutor");

      assertNotNull(field, "asyncExecutor field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "asyncExecutor should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "asyncExecutor should be final");
      LOGGER.info("asyncExecutor field verified");
    }

    @Test
    @DisplayName("should have resourceHandleGenerator field")
    void shouldHaveResourceHandleGeneratorField()
        throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing resourceHandleGenerator field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("resourceHandleGenerator");

      assertNotNull(field, "resourceHandleGenerator field should exist");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "resourceHandleGenerator should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "resourceHandleGenerator should be final");
      LOGGER.info("resourceHandleGenerator field verified");
    }

    @Test
    @DisplayName("should have activeOperations field")
    void shouldHaveActiveOperationsField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing activeOperations field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("activeOperations");

      assertNotNull(field, "activeOperations field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "activeOperations should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "activeOperations should be final");
      LOGGER.info("activeOperations field verified");
    }

    @Test
    @DisplayName("should have streamOperations field")
    void shouldHaveStreamOperationsField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing streamOperations field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("streamOperations");

      assertNotNull(field, "streamOperations field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "streamOperations should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "streamOperations should be final");
      LOGGER.info("streamOperations field verified");
    }

    @Test
    @DisplayName("should have networkOperations field")
    void shouldHaveNetworkOperationsField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing networkOperations field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("networkOperations");

      assertNotNull(field, "networkOperations field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "networkOperations should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "networkOperations should be final");
      LOGGER.info("networkOperations field verified");
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
                  "createResource",
                  "destroyResource",
                  "openInputStream",
                  "openOutputStream",
                  "readAsync",
                  "writeAsync",
                  "createTcpSocket",
                  "connectTcpAsync",
                  "createUdpSocket",
                  "sendUdpAsync",
                  "poll",
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
          asyncMethodCount >= 4,
          "Should have at least 4 async methods (readAsync, writeAsync, connectTcpAsync, sendUdpAsync)");
      LOGGER.info("Async method count verified: " + asyncMethodCount);
    }
  }

  @Nested
  @DisplayName("Native Method Declaration Tests")
  class NativeMethodDeclarationTests {

    @Test
    @DisplayName("should have native methods for resource management")
    void shouldHaveNativeMethodsForResourceManagement() throws ClassNotFoundException {
      LOGGER.info("Testing native method declarations");
      final Class<?> clazz = loadClassWithoutInit();

      int nativeMethodCount = 0;
      for (final Method method : clazz.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          nativeMethodCount++;
          LOGGER.info("Found native method: " + method.getName());
        }
      }

      assertTrue(nativeMethodCount >= 2, "Should have native methods for resource management");
      LOGGER.info("Native method count: " + nativeMethodCount);
    }
  }
}
