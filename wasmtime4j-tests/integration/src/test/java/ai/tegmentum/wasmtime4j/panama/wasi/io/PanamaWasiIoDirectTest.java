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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Direct integration tests for Panama WASI IO classes.
 *
 * <p>These tests verify class structure and method signatures without creating instances with
 * invalid native handles (which would cause JVM crashes).
 */
@DisplayName("Panama WASI IO Direct Tests")
public class PanamaWasiIoDirectTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiIoDirectTest.class.getName());
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for WASI IO tests");
    final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
    assertTrue(loader.isLoaded(), "Native library should be loaded");
    LOGGER.info("Native library loaded successfully");
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up " + resources.size() + " resources");
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  /** Tests for PanamaWasiInputStream class. */
  @Nested
  @DisplayName("PanamaWasiInputStream Tests")
  class InputStreamTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing PanamaWasiInputStream class structure");

      final Class<?> clazz = PanamaWasiInputStream.class;

      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");

      final Constructor<?>[] constructors = clazz.getConstructors();
      assertEquals(1, constructors.length, "Should have exactly one public constructor");

      final Class<?>[] paramTypes = constructors[0].getParameterTypes();
      assertEquals(2, paramTypes.length, "Constructor should have 2 parameters");
      assertEquals(MemorySegment.class, paramTypes[0], "First param should be MemorySegment");
      assertEquals(MemorySegment.class, paramTypes[1], "Second param should be MemorySegment");

      LOGGER.info("Class structure verified successfully");
    }

    @Test
    @DisplayName("Should have required static method handles")
    void shouldHaveRequiredStaticMethodHandles() throws Exception {
      LOGGER.info("Testing PanamaWasiInputStream static method handles");

      final Class<?> clazz = PanamaWasiInputStream.class;
      final String[] expectedHandles = {
        "READ_HANDLE", "BLOCKING_READ_HANDLE", "SKIP_HANDLE", "SUBSCRIBE_HANDLE", "CLOSE_HANDLE"
      };

      for (final String handleName : expectedHandles) {
        final Field field = clazz.getDeclaredField(handleName);
        field.setAccessible(true);
        assertNotNull(field, "Should have " + handleName + " field");
        assertTrue(
            java.lang.reflect.Modifier.isStatic(field.getModifiers()),
            handleName + " should be static");
        LOGGER.info("Verified " + handleName + " exists");
      }
    }

    @Test
    @DisplayName("Should have all required public methods")
    void shouldHaveAllRequiredPublicMethods() throws Exception {
      LOGGER.info("Testing PanamaWasiInputStream public methods");

      final Class<?> clazz = PanamaWasiInputStream.class;
      final String[] expectedMethods = {
        "read",
        "blockingRead",
        "skip",
        "blockingSkip",
        "subscribe",
        "getId",
        "getType",
        "isOwned",
        "isValid",
        "getAvailableOperations",
        "invoke",
        "getLastAccessedAt",
        "getCreatedAt"
      };

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("Should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      LOGGER.info("Testing AutoCloseable implementation");

      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaWasiInputStream.class),
          "Should implement AutoCloseable");
    }
  }

  /** Tests for PanamaWasiOutputStream class. */
  @Nested
  @DisplayName("PanamaWasiOutputStream Tests")
  class OutputStreamTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing PanamaWasiOutputStream class structure");

      final Class<?> clazz = PanamaWasiOutputStream.class;

      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");

      final Constructor<?>[] constructors = clazz.getConstructors();
      assertEquals(1, constructors.length, "Should have exactly one public constructor");

      final Class<?>[] paramTypes = constructors[0].getParameterTypes();
      assertEquals(2, paramTypes.length, "Constructor should have 2 parameters");
      assertEquals(MemorySegment.class, paramTypes[0], "First param should be MemorySegment");
      assertEquals(MemorySegment.class, paramTypes[1], "Second param should be MemorySegment");

      LOGGER.info("Class structure verified successfully");
    }

    @Test
    @DisplayName("Should have required static method handles")
    void shouldHaveRequiredStaticMethodHandles() throws Exception {
      LOGGER.info("Testing PanamaWasiOutputStream static method handles");

      final Class<?> clazz = PanamaWasiOutputStream.class;
      final String[] expectedHandles = {
        "CHECK_WRITE_HANDLE",
        "WRITE_HANDLE",
        "BLOCKING_WRITE_AND_FLUSH_HANDLE",
        "FLUSH_HANDLE",
        "BLOCKING_FLUSH_HANDLE",
        "WRITE_ZEROES_HANDLE",
        "BLOCKING_WRITE_ZEROES_AND_FLUSH_HANDLE",
        "SPLICE_HANDLE",
        "BLOCKING_SPLICE_HANDLE",
        "SUBSCRIBE_HANDLE",
        "CLOSE_HANDLE"
      };

      for (final String handleName : expectedHandles) {
        final Field field = clazz.getDeclaredField(handleName);
        field.setAccessible(true);
        assertNotNull(field, "Should have " + handleName + " field");
        assertTrue(
            java.lang.reflect.Modifier.isStatic(field.getModifiers()),
            handleName + " should be static");
        LOGGER.info("Verified " + handleName + " exists");
      }
    }

    @Test
    @DisplayName("Should have all required public methods")
    void shouldHaveAllRequiredPublicMethods() throws Exception {
      LOGGER.info("Testing PanamaWasiOutputStream public methods");

      final Class<?> clazz = PanamaWasiOutputStream.class;
      final String[] expectedMethods = {
        "checkWrite",
        "write",
        "blockingWriteAndFlush",
        "flush",
        "blockingFlush",
        "writeZeroes",
        "blockingWriteZeroesAndFlush",
        "splice",
        "blockingSplice",
        "subscribe",
        "getId",
        "getType",
        "isOwned",
        "isValid",
        "getAvailableOperations",
        "invoke",
        "getLastAccessedAt",
        "getCreatedAt"
      };

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("Should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      LOGGER.info("Testing AutoCloseable implementation");

      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaWasiOutputStream.class),
          "Should implement AutoCloseable");
    }
  }

  /** Tests for PanamaWasiPollable class. */
  @Nested
  @DisplayName("PanamaWasiPollable Tests")
  class PollableTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing PanamaWasiPollable class structure");

      final Class<?> clazz = PanamaWasiPollable.class;

      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");

      final Constructor<?>[] constructors = clazz.getConstructors();
      assertEquals(1, constructors.length, "Should have exactly one public constructor");

      final Class<?>[] paramTypes = constructors[0].getParameterTypes();
      assertEquals(2, paramTypes.length, "Constructor should have 2 parameters");
      assertEquals(MemorySegment.class, paramTypes[0], "First param should be MemorySegment");
      assertEquals(MemorySegment.class, paramTypes[1], "Second param should be MemorySegment");

      LOGGER.info("Class structure verified successfully");
    }

    @Test
    @DisplayName("Should have required static method handles")
    void shouldHaveRequiredStaticMethodHandles() throws Exception {
      LOGGER.info("Testing PanamaWasiPollable static method handles");

      final Class<?> clazz = PanamaWasiPollable.class;
      final String[] expectedHandles = {"BLOCK_HANDLE", "READY_HANDLE", "CLOSE_HANDLE"};

      for (final String handleName : expectedHandles) {
        final Field field = clazz.getDeclaredField(handleName);
        field.setAccessible(true);
        assertNotNull(field, "Should have " + handleName + " field");
        assertTrue(
            java.lang.reflect.Modifier.isStatic(field.getModifiers()),
            handleName + " should be static");
        LOGGER.info("Verified " + handleName + " exists");
      }
    }

    @Test
    @DisplayName("Should have all required public methods")
    void shouldHaveAllRequiredPublicMethods() throws Exception {
      LOGGER.info("Testing PanamaWasiPollable public methods");

      final Class<?> clazz = PanamaWasiPollable.class;
      final String[] expectedMethods = {
        "block",
        "ready",
        "getId",
        "getType",
        "isOwned",
        "isValid",
        "getAvailableOperations",
        "invoke",
        "getLastAccessedAt",
        "getCreatedAt"
      };

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("Should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      LOGGER.info("Testing AutoCloseable implementation");

      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaWasiPollable.class),
          "Should implement AutoCloseable");
    }
  }

  /** Cross-class integration tests. */
  @Nested
  @DisplayName("IO Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should verify consistent type patterns across IO classes")
    void shouldVerifyConsistentTypePatternsAcrossIoClasses() {
      LOGGER.info("Verifying consistent type patterns across IO classes");

      // Verify all classes are final
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaWasiInputStream.class.getModifiers()),
          "InputStream should be final");
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaWasiOutputStream.class.getModifiers()),
          "OutputStream should be final");
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaWasiPollable.class.getModifiers()),
          "Pollable should be final");

      // Verify all have consistent constructor signatures
      final Constructor<?>[] inputConstructors = PanamaWasiInputStream.class.getConstructors();
      final Constructor<?>[] outputConstructors = PanamaWasiOutputStream.class.getConstructors();
      final Constructor<?>[] pollableConstructors = PanamaWasiPollable.class.getConstructors();

      assertEquals(1, inputConstructors.length, "InputStream should have 1 constructor");
      assertEquals(1, outputConstructors.length, "OutputStream should have 1 constructor");
      assertEquals(1, pollableConstructors.length, "Pollable should have 1 constructor");

      LOGGER.info("All IO classes have consistent structure");
    }

    @Test
    @DisplayName("Should verify all IO classes have CLOSE_HANDLE")
    void shouldVerifyAllIoClassesHaveCloseHandle() throws Exception {
      LOGGER.info("Verifying CLOSE_HANDLE across IO classes");

      final Class<?>[] classes = {
        PanamaWasiInputStream.class, PanamaWasiOutputStream.class, PanamaWasiPollable.class
      };

      for (final Class<?> clazz : classes) {
        final Field closeHandle = clazz.getDeclaredField("CLOSE_HANDLE");
        closeHandle.setAccessible(true);
        assertNotNull(closeHandle, clazz.getSimpleName() + " should have CLOSE_HANDLE");
        assertTrue(
            java.lang.reflect.Modifier.isStatic(closeHandle.getModifiers()),
            "CLOSE_HANDLE should be static in " + clazz.getSimpleName());
        LOGGER.info(clazz.getSimpleName() + " has CLOSE_HANDLE");
      }
    }

    @Test
    @DisplayName("Should verify all IO classes implement AutoCloseable")
    void shouldVerifyAllIoClassesImplementAutoCloseable() {
      LOGGER.info("Verifying AutoCloseable implementation across IO classes");

      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaWasiInputStream.class),
          "InputStream should implement AutoCloseable");
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaWasiOutputStream.class),
          "OutputStream should implement AutoCloseable");
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaWasiPollable.class),
          "Pollable should implement AutoCloseable");

      LOGGER.info("All IO classes implement AutoCloseable");
    }
  }
}
