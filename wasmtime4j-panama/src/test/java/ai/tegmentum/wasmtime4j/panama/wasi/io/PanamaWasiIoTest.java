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

package ai.tegmentum.wasmtime4j.panama.wasi.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for Panama WASI I/O implementation classes.
 *
 * <p>Tests cover class structure, interface compliance, Panama FFI patterns, and MethodHandle field
 * verification for:
 *
 * <ul>
 *   <li>PanamaWasiInputStream - WASI input stream operations
 *   <li>PanamaWasiOutputStream - WASI output stream operations
 *   <li>PanamaWasiPollable - WASI pollable resource for async I/O
 * </ul>
 *
 * <p>Note: These tests use Class.forName with initialize=false to load classes without triggering
 * static initializers, which would attempt to load native libraries. This allows testing the class
 * structure without runtime dependencies.
 */
@DisplayName("Panama WASI I/O Tests")
class PanamaWasiIoTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiIoTest.class.getName());

  private static final String INPUT_STREAM_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiInputStream";
  private static final String OUTPUT_STREAM_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiOutputStream";
  private static final String POLLABLE_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiPollable";

  /**
   * Loads a class without initializing it (no static initializer runs). This prevents native
   * library loading attempts.
   */
  private static Class<?> loadClassWithoutInit(final String className)
      throws ClassNotFoundException {
    return Class.forName(className, false, PanamaWasiIoTest.class.getClassLoader());
  }

  @Nested
  @DisplayName("PanamaWasiInputStream Class Structure Tests")
  class InputStreamClassStructureTests {

    @Test
    @DisplayName("PanamaWasiInputStream should exist and be public final")
    void inputStreamClassShouldExistAndBePublic() {
      final Class<?> clazz =
          assertDoesNotThrow(
              () -> loadClassWithoutInit(INPUT_STREAM_CLASS),
              "PanamaWasiInputStream class should exist");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "PanamaWasiInputStream should be public");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaWasiInputStream should be final");
      LOGGER.info(
          "PanamaWasiInputStream class verified: public="
              + Modifier.isPublic(clazz.getModifiers())
              + ", final="
              + Modifier.isFinal(clazz.getModifiers()));
    }

    @Test
    @DisplayName("PanamaWasiInputStream should implement WasiInputStream interface")
    void inputStreamShouldImplementInterface() {
      final Class<?> clazz = assertDoesNotThrow(() -> loadClassWithoutInit(INPUT_STREAM_CLASS));
      assertTrue(
          WasiInputStream.class.isAssignableFrom(clazz),
          "PanamaWasiInputStream should implement WasiInputStream");
      LOGGER.info(
          "PanamaWasiInputStream implements WasiInputStream: "
              + WasiInputStream.class.isAssignableFrom(clazz));
    }

    @Test
    @DisplayName("PanamaWasiInputStream should have required WASI I/O methods")
    void inputStreamShouldHaveRequiredMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(INPUT_STREAM_CLASS);
      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("read");
      requiredMethods.add("blockingRead");
      requiredMethods.add("skip");
      requiredMethods.add("blockingSkip");
      requiredMethods.add("subscribe");
      requiredMethods.add("close");

      final Set<String> foundMethods = new HashSet<>();
      for (Method method : clazz.getDeclaredMethods()) {
        foundMethods.add(method.getName());
      }

      for (String methodName : requiredMethods) {
        assertTrue(
            foundMethods.contains(methodName),
            "PanamaWasiInputStream should have method: " + methodName);
        LOGGER.info("Found required method: " + methodName);
      }
    }

    @Test
    @DisplayName("PanamaWasiInputStream should have MethodHandle fields for FFI")
    void inputStreamShouldHaveMethodHandleFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(INPUT_STREAM_CLASS);
      int methodHandleCount = 0;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == MethodHandle.class) {
          assertTrue(
              Modifier.isStatic(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be static");
          assertTrue(
              Modifier.isFinal(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be final");
          methodHandleCount++;
          LOGGER.info("Found MethodHandle field: " + field.getName());
        }
      }
      assertTrue(
          methodHandleCount >= 4,
          "PanamaWasiInputStream should have at least 4 MethodHandle fields for FFI, found: "
              + methodHandleCount);
      LOGGER.info("Total MethodHandle fields: " + methodHandleCount);
    }

    @Test
    @DisplayName("PanamaWasiInputStream should have contextHandle field")
    void inputStreamShouldHaveContextHandleField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(INPUT_STREAM_CLASS);
      boolean hasContextHandle = false;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("contextHandle")) {
          hasContextHandle = true;
          assertTrue(Modifier.isPrivate(field.getModifiers()), "contextHandle should be private");
          assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
          LOGGER.info("Found contextHandle field with type: " + field.getType().getName());
          break;
        }
      }
      assertTrue(hasContextHandle, "PanamaWasiInputStream should have contextHandle field");
    }
  }

  @Nested
  @DisplayName("PanamaWasiOutputStream Class Structure Tests")
  class OutputStreamClassStructureTests {

    @Test
    @DisplayName("PanamaWasiOutputStream should exist and be public final")
    void outputStreamClassShouldExistAndBePublic() {
      final Class<?> clazz =
          assertDoesNotThrow(
              () -> loadClassWithoutInit(OUTPUT_STREAM_CLASS),
              "PanamaWasiOutputStream class should exist");
      assertTrue(
          Modifier.isPublic(clazz.getModifiers()), "PanamaWasiOutputStream should be public");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaWasiOutputStream should be final");
      LOGGER.info(
          "PanamaWasiOutputStream class verified: public="
              + Modifier.isPublic(clazz.getModifiers())
              + ", final="
              + Modifier.isFinal(clazz.getModifiers()));
    }

    @Test
    @DisplayName("PanamaWasiOutputStream should implement WasiOutputStream interface")
    void outputStreamShouldImplementInterface() {
      final Class<?> clazz = assertDoesNotThrow(() -> loadClassWithoutInit(OUTPUT_STREAM_CLASS));
      assertTrue(
          WasiOutputStream.class.isAssignableFrom(clazz),
          "PanamaWasiOutputStream should implement WasiOutputStream");
      LOGGER.info(
          "PanamaWasiOutputStream implements WasiOutputStream: "
              + WasiOutputStream.class.isAssignableFrom(clazz));
    }

    @Test
    @DisplayName("PanamaWasiOutputStream should have required WASI I/O methods")
    void outputStreamShouldHaveRequiredMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(OUTPUT_STREAM_CLASS);
      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("checkWrite");
      requiredMethods.add("write");
      requiredMethods.add("blockingWriteAndFlush");
      requiredMethods.add("flush");
      requiredMethods.add("blockingFlush");
      requiredMethods.add("writeZeroes");
      requiredMethods.add("blockingWriteZeroesAndFlush");
      requiredMethods.add("splice");
      requiredMethods.add("blockingSplice");
      requiredMethods.add("subscribe");
      requiredMethods.add("close");

      final Set<String> foundMethods = new HashSet<>();
      for (Method method : clazz.getDeclaredMethods()) {
        foundMethods.add(method.getName());
      }

      for (String methodName : requiredMethods) {
        assertTrue(
            foundMethods.contains(methodName),
            "PanamaWasiOutputStream should have method: " + methodName);
        LOGGER.info("Found required method: " + methodName);
      }
    }

    @Test
    @DisplayName("PanamaWasiOutputStream should have MethodHandle fields for FFI")
    void outputStreamShouldHaveMethodHandleFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(OUTPUT_STREAM_CLASS);
      int methodHandleCount = 0;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == MethodHandle.class) {
          assertTrue(
              Modifier.isStatic(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be static");
          assertTrue(
              Modifier.isFinal(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be final");
          methodHandleCount++;
          LOGGER.info("Found MethodHandle field: " + field.getName());
        }
      }
      assertTrue(
          methodHandleCount >= 8,
          "PanamaWasiOutputStream should have at least 8 MethodHandle fields for FFI, found: "
              + methodHandleCount);
      LOGGER.info("Total MethodHandle fields: " + methodHandleCount);
    }

    @Test
    @DisplayName("PanamaWasiOutputStream should have contextHandle field")
    void outputStreamShouldHaveContextHandleField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(OUTPUT_STREAM_CLASS);
      boolean hasContextHandle = false;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("contextHandle")) {
          hasContextHandle = true;
          assertTrue(Modifier.isPrivate(field.getModifiers()), "contextHandle should be private");
          assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
          LOGGER.info("Found contextHandle field with type: " + field.getType().getName());
          break;
        }
      }
      assertTrue(hasContextHandle, "PanamaWasiOutputStream should have contextHandle field");
    }
  }

  @Nested
  @DisplayName("PanamaWasiPollable Class Structure Tests")
  class PollableClassStructureTests {

    @Test
    @DisplayName("PanamaWasiPollable should exist and be public final")
    void pollableClassShouldExistAndBePublic() {
      final Class<?> clazz =
          assertDoesNotThrow(
              () -> loadClassWithoutInit(POLLABLE_CLASS), "PanamaWasiPollable class should exist");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "PanamaWasiPollable should be public");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaWasiPollable should be final");
      LOGGER.info(
          "PanamaWasiPollable class verified: public="
              + Modifier.isPublic(clazz.getModifiers())
              + ", final="
              + Modifier.isFinal(clazz.getModifiers()));
    }

    @Test
    @DisplayName("PanamaWasiPollable should implement WasiPollable interface")
    void pollableShouldImplementInterface() {
      final Class<?> clazz = assertDoesNotThrow(() -> loadClassWithoutInit(POLLABLE_CLASS));
      assertTrue(
          WasiPollable.class.isAssignableFrom(clazz),
          "PanamaWasiPollable should implement WasiPollable");
      LOGGER.info(
          "PanamaWasiPollable implements WasiPollable: "
              + WasiPollable.class.isAssignableFrom(clazz));
    }

    @Test
    @DisplayName("PanamaWasiPollable should have required pollable methods")
    void pollableShouldHaveRequiredMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(POLLABLE_CLASS);
      final Set<String> requiredMethods = new HashSet<>();
      requiredMethods.add("block");
      requiredMethods.add("ready");
      requiredMethods.add("close");

      final Set<String> foundMethods = new HashSet<>();
      for (Method method : clazz.getDeclaredMethods()) {
        foundMethods.add(method.getName());
      }

      for (String methodName : requiredMethods) {
        assertTrue(
            foundMethods.contains(methodName),
            "PanamaWasiPollable should have method: " + methodName);
        LOGGER.info("Found required method: " + methodName);
      }
    }

    @Test
    @DisplayName("PanamaWasiPollable should have MethodHandle fields for FFI")
    void pollableShouldHaveMethodHandleFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(POLLABLE_CLASS);
      int methodHandleCount = 0;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == MethodHandle.class) {
          assertTrue(
              Modifier.isStatic(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be static");
          assertTrue(
              Modifier.isFinal(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be final");
          methodHandleCount++;
          LOGGER.info("Found MethodHandle field: " + field.getName());
        }
      }
      assertTrue(
          methodHandleCount >= 3,
          "PanamaWasiPollable should have at least 3 MethodHandle fields for FFI, found: "
              + methodHandleCount);
      LOGGER.info("Total MethodHandle fields: " + methodHandleCount);
    }

    @Test
    @DisplayName("PanamaWasiPollable should have contextHandle field")
    void pollableShouldHaveContextHandleField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(POLLABLE_CLASS);
      boolean hasContextHandle = false;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("contextHandle")) {
          hasContextHandle = true;
          assertTrue(Modifier.isPrivate(field.getModifiers()), "contextHandle should be private");
          assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
          LOGGER.info("Found contextHandle field with type: " + field.getType().getName());
          break;
        }
      }
      assertTrue(hasContextHandle, "PanamaWasiPollable should have contextHandle field");
    }
  }

  @Nested
  @DisplayName("Input Stream API Contract Tests")
  class InputStreamApiContractTests {

    @Test
    @DisplayName("WasiInputStream interface methods should be properly declared")
    void inputStreamInterfaceMethodsShouldBeDeclared() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(INPUT_STREAM_CLASS);
      // Check read method with long parameter
      boolean hasReadWithLong = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("read")
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == long.class) {
          hasReadWithLong = true;
          assertEquals(byte[].class, method.getReturnType(), "read(long) should return byte[]");
          LOGGER.info("Found read(long) method with return type: " + method.getReturnType());
          break;
        }
      }
      assertTrue(hasReadWithLong, "WasiInputStream should have read(long) method");
    }

    @Test
    @DisplayName("Input stream should have blocking read method")
    void inputStreamShouldHaveBlockingRead() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(INPUT_STREAM_CLASS);
      boolean hasBlockingRead = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("blockingRead")
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == long.class) {
          hasBlockingRead = true;
          assertEquals(
              byte[].class, method.getReturnType(), "blockingRead(long) should return byte[]");
          LOGGER.info(
              "Found blockingRead(long) method with return type: " + method.getReturnType());
          break;
        }
      }
      assertTrue(hasBlockingRead, "WasiInputStream should have blockingRead(long) method");
    }

    @Test
    @DisplayName("Input stream should have subscribe method returning WasiPollable")
    void inputStreamShouldHaveSubscribeMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(INPUT_STREAM_CLASS);
      boolean hasSubscribe = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("subscribe") && method.getParameterCount() == 0) {
          hasSubscribe = true;
          assertTrue(
              WasiPollable.class.isAssignableFrom(method.getReturnType()),
              "subscribe() should return WasiPollable");
          LOGGER.info("Found subscribe() method with return type: " + method.getReturnType());
          break;
        }
      }
      assertTrue(hasSubscribe, "WasiInputStream should have subscribe() method");
    }
  }

  @Nested
  @DisplayName("Output Stream API Contract Tests")
  class OutputStreamApiContractTests {

    @Test
    @DisplayName("WasiOutputStream should have checkWrite method")
    void outputStreamShouldHaveCheckWriteMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(OUTPUT_STREAM_CLASS);
      boolean hasCheckWrite = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("checkWrite") && method.getParameterCount() == 0) {
          hasCheckWrite = true;
          assertEquals(long.class, method.getReturnType(), "checkWrite() should return long");
          LOGGER.info("Found checkWrite() method with return type: " + method.getReturnType());
          break;
        }
      }
      assertTrue(hasCheckWrite, "WasiOutputStream should have checkWrite() method");
    }

    @Test
    @DisplayName("Output stream should have write method accepting byte array")
    void outputStreamShouldHaveWriteMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(OUTPUT_STREAM_CLASS);
      boolean hasWrite = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("write")
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == byte[].class) {
          hasWrite = true;
          assertEquals(void.class, method.getReturnType(), "write(byte[]) should return void");
          LOGGER.info("Found write(byte[]) method");
          break;
        }
      }
      assertTrue(hasWrite, "WasiOutputStream should have write(byte[]) method");
    }

    @Test
    @DisplayName("Output stream should have splice method for stream composition")
    void outputStreamShouldHaveSpliceMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(OUTPUT_STREAM_CLASS);
      boolean hasSplice = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("splice") && method.getParameterCount() == 2) {
          final Class<?>[] paramTypes = method.getParameterTypes();
          if (WasiInputStream.class.isAssignableFrom(paramTypes[0])
              && paramTypes[1] == long.class) {
            hasSplice = true;
            assertEquals(
                long.class,
                method.getReturnType(),
                "splice(WasiInputStream, long) should return long");
            LOGGER.info("Found splice(WasiInputStream, long) method");
            break;
          }
        }
      }
      assertTrue(hasSplice, "WasiOutputStream should have splice(WasiInputStream, long) method");
    }
  }

  @Nested
  @DisplayName("Pollable API Contract Tests")
  class PollableApiContractTests {

    @Test
    @DisplayName("WasiPollable should have block method")
    void pollableShouldHaveBlockMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(POLLABLE_CLASS);
      boolean hasBlock = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("block") && method.getParameterCount() == 0) {
          hasBlock = true;
          assertEquals(void.class, method.getReturnType(), "block() should return void");
          LOGGER.info("Found block() method");
          break;
        }
      }
      assertTrue(hasBlock, "WasiPollable should have block() method");
    }

    @Test
    @DisplayName("WasiPollable should have ready method returning boolean")
    void pollableShouldHaveReadyMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(POLLABLE_CLASS);
      boolean hasReady = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("ready") && method.getParameterCount() == 0) {
          hasReady = true;
          assertEquals(boolean.class, method.getReturnType(), "ready() should return boolean");
          LOGGER.info("Found ready() method with return type: " + method.getReturnType());
          break;
        }
      }
      assertTrue(hasReady, "WasiPollable should have ready() method");
    }
  }

  @Nested
  @DisplayName("Panama FFI Pattern Tests")
  class PanamaFfiPatternTests {

    @Test
    @DisplayName("All I/O classes should implement AutoCloseable")
    void allIoClassesShouldImplementAutoCloseable() throws ClassNotFoundException {
      final String[] classNames = {INPUT_STREAM_CLASS, OUTPUT_STREAM_CLASS, POLLABLE_CLASS};

      for (String className : classNames) {
        final Class<?> clazz = loadClassWithoutInit(className);
        assertTrue(
            AutoCloseable.class.isAssignableFrom(clazz),
            className + " should implement AutoCloseable");
        LOGGER.info(className + " implements AutoCloseable: true");
      }
    }

    @Test
    @DisplayName("I/O classes should have static MethodHandle fields initialized in static block")
    void ioClassesShouldHaveStaticMethodHandles() throws ClassNotFoundException {
      final String[] classNames = {INPUT_STREAM_CLASS, OUTPUT_STREAM_CLASS, POLLABLE_CLASS};
      final int[] expectedMinCounts = {4, 8, 3};

      for (int i = 0; i < classNames.length; i++) {
        final Class<?> clazz = loadClassWithoutInit(classNames[i]);
        int staticFinalMethodHandleCount = 0;
        for (Field field : clazz.getDeclaredFields()) {
          if (field.getType() == MethodHandle.class
              && Modifier.isStatic(field.getModifiers())
              && Modifier.isFinal(field.getModifiers())) {
            staticFinalMethodHandleCount++;
          }
        }
        assertTrue(
            staticFinalMethodHandleCount >= expectedMinCounts[i],
            classNames[i]
                + " should have at least "
                + expectedMinCounts[i]
                + " static final MethodHandle fields, found: "
                + staticFinalMethodHandleCount);
        LOGGER.info(
            classNames[i]
                + " has "
                + staticFinalMethodHandleCount
                + " static final MethodHandle fields");
      }
    }

    @Test
    @DisplayName("I/O classes should have Logger field")
    void ioClassesShouldHaveLoggerField() throws ClassNotFoundException {
      final String[] classNames = {INPUT_STREAM_CLASS, OUTPUT_STREAM_CLASS, POLLABLE_CLASS};

      for (String className : classNames) {
        final Class<?> clazz = loadClassWithoutInit(className);
        boolean hasLogger = false;
        for (Field field : clazz.getDeclaredFields()) {
          if (field.getType() == Logger.class) {
            hasLogger = true;
            assertTrue(
                Modifier.isPrivate(field.getModifiers()),
                "Logger in " + className + " should be private");
            assertTrue(
                Modifier.isStatic(field.getModifiers()),
                "Logger in " + className + " should be static");
            assertTrue(
                Modifier.isFinal(field.getModifiers()),
                "Logger in " + className + " should be final");
            LOGGER.info(className + " has Logger field: " + field.getName());
            break;
          }
        }
        assertTrue(hasLogger, className + " should have a Logger field");
      }
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("I/O stream classes should implement AutoCloseable")
    void ioStreamClassesShouldImplementAutoCloseable() throws ClassNotFoundException {
      final String[] classNames = {INPUT_STREAM_CLASS, OUTPUT_STREAM_CLASS, POLLABLE_CLASS};

      for (String className : classNames) {
        final Class<?> clazz = loadClassWithoutInit(className);
        assertTrue(
            AutoCloseable.class.isAssignableFrom(clazz),
            className + " should implement AutoCloseable");
        LOGGER.info(
            className
                + " implements AutoCloseable: "
                + AutoCloseable.class.isAssignableFrom(clazz));
      }
    }

    @Test
    @DisplayName("I/O classes should have WasiResource methods")
    void ioClassesShouldHaveWasiResourceMethods() throws ClassNotFoundException {
      final String[] classNames = {INPUT_STREAM_CLASS, OUTPUT_STREAM_CLASS, POLLABLE_CLASS};
      final Set<String> wasiResourceMethods = new HashSet<>();
      wasiResourceMethods.add("getId");
      wasiResourceMethods.add("getType");
      wasiResourceMethods.add("isValid");
      wasiResourceMethods.add("getAvailableOperations");
      wasiResourceMethods.add("invoke");

      for (String className : classNames) {
        final Class<?> clazz = loadClassWithoutInit(className);
        final Set<String> foundMethods = new HashSet<>();
        for (Method method : clazz.getMethods()) {
          foundMethods.add(method.getName());
        }

        for (String methodName : wasiResourceMethods) {
          assertTrue(
              foundMethods.contains(methodName),
              className + " should have WasiResource method: " + methodName);
        }
        LOGGER.info(className + " has all WasiResource methods");
      }
    }

    @Test
    @DisplayName("I/O classes should have close method")
    void ioClassesShouldHaveCloseMethod() throws ClassNotFoundException {
      final String[] classNames = {INPUT_STREAM_CLASS, OUTPUT_STREAM_CLASS, POLLABLE_CLASS};

      for (String className : classNames) {
        final Class<?> clazz = loadClassWithoutInit(className);
        boolean hasClose = false;
        for (Method method : clazz.getDeclaredMethods()) {
          if (method.getName().equals("close") && method.getParameterCount() == 0) {
            hasClose = true;
            assertTrue(
                Modifier.isPublic(method.getModifiers()),
                "close in " + className + " should be public");
            LOGGER.info(className + " has close method");
            break;
          }
        }
        assertTrue(hasClose, className + " should have close method");
      }
    }

    @Test
    @DisplayName("I/O classes should have NativeResourceHandle field for lifecycle management")
    void ioClassesShouldHaveResourceHandleField() throws ClassNotFoundException {
      final String[] classNames = {INPUT_STREAM_CLASS, OUTPUT_STREAM_CLASS, POLLABLE_CLASS};

      for (String className : classNames) {
        final Class<?> clazz = loadClassWithoutInit(className);
        boolean hasResourceHandle = false;
        for (Field field : clazz.getDeclaredFields()) {
          if (field.getType().getSimpleName().equals("NativeResourceHandle")) {
            hasResourceHandle = true;
            LOGGER.info(className + " has NativeResourceHandle field: " + field.getName());
            break;
          }
        }
        assertTrue(hasResourceHandle, className + " should have NativeResourceHandle field");
      }
    }
  }

  @Nested
  @DisplayName("Cross-Class Integration Tests")
  class CrossClassIntegrationTests {

    @Test
    @DisplayName("InputStream.subscribe should return type compatible with WasiPollable")
    void inputStreamSubscribeShouldReturnPollableType() throws ClassNotFoundException {
      final Class<?> inputStreamClass = loadClassWithoutInit(INPUT_STREAM_CLASS);
      for (Method method : inputStreamClass.getMethods()) {
        if (method.getName().equals("subscribe") && method.getParameterCount() == 0) {
          assertTrue(
              WasiPollable.class.isAssignableFrom(method.getReturnType()),
              "InputStream.subscribe() should return WasiPollable-compatible type");
          LOGGER.info("InputStream.subscribe() returns: " + method.getReturnType().getName());
          break;
        }
      }
    }

    @Test
    @DisplayName("OutputStream.subscribe should return type compatible with WasiPollable")
    void outputStreamSubscribeShouldReturnPollableType() throws ClassNotFoundException {
      final Class<?> outputStreamClass = loadClassWithoutInit(OUTPUT_STREAM_CLASS);
      for (Method method : outputStreamClass.getMethods()) {
        if (method.getName().equals("subscribe") && method.getParameterCount() == 0) {
          assertTrue(
              WasiPollable.class.isAssignableFrom(method.getReturnType()),
              "OutputStream.subscribe() should return WasiPollable-compatible type");
          LOGGER.info("OutputStream.subscribe() returns: " + method.getReturnType().getName());
          break;
        }
      }
    }

    @Test
    @DisplayName("OutputStream.splice should accept WasiInputStream as parameter")
    void outputStreamSpliceShouldAcceptInputStream() throws ClassNotFoundException {
      final Class<?> outputStreamClass = loadClassWithoutInit(OUTPUT_STREAM_CLASS);
      boolean hasSpliceWithInputStream = false;
      for (Method method : outputStreamClass.getMethods()) {
        if (method.getName().equals("splice")) {
          final Class<?>[] paramTypes = method.getParameterTypes();
          if (paramTypes.length >= 1 && WasiInputStream.class.isAssignableFrom(paramTypes[0])) {
            hasSpliceWithInputStream = true;
            LOGGER.info("OutputStream.splice accepts: " + Arrays.toString(paramTypes));
            break;
          }
        }
      }
      assertTrue(
          hasSpliceWithInputStream,
          "OutputStream.splice should accept WasiInputStream as parameter");
    }

    @Test
    @DisplayName("All I/O classes should implement AutoCloseable")
    void allIoClassesShouldImplementAutoCloseable() throws ClassNotFoundException {
      final String[] classNames = {INPUT_STREAM_CLASS, OUTPUT_STREAM_CLASS, POLLABLE_CLASS};

      for (String className : classNames) {
        final Class<?> clazz = loadClassWithoutInit(className);
        assertTrue(
            AutoCloseable.class.isAssignableFrom(clazz),
            className + " should implement AutoCloseable");
        LOGGER.info(className + " implements AutoCloseable: true");
      }
    }
  }
}
