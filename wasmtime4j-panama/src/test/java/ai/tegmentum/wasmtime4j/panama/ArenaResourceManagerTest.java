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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ArenaResourceManager} class.
 *
 * <p>ArenaResourceManager provides arena-based resource management for Panama FFI operations.
 */
@DisplayName("ArenaResourceManager Tests")
class ArenaResourceManagerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(ArenaResourceManager.class.getModifiers()),
          "ArenaResourceManager should be public");
      assertTrue(
          Modifier.isFinal(ArenaResourceManager.class.getModifiers()),
          "ArenaResourceManager should be final");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(ArenaResourceManager.class),
          "ArenaResourceManager should implement AutoCloseable");
    }

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = ArenaResourceManager.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with arena and tracking flag")
    void shouldHaveConstructorWithArenaAndTracking() throws NoSuchMethodException {
      final Constructor<?> constructor =
          ArenaResourceManager.class.getConstructor(Arena.class, boolean.class);
      assertNotNull(constructor, "Constructor with arena and tracking should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Arena Access Method Tests")
  class ArenaAccessMethodTests {

    @Test
    @DisplayName("should have getArena method")
    void shouldHaveGetArenaMethod() throws NoSuchMethodException {
      final Method method = ArenaResourceManager.class.getMethod("getArena");
      assertNotNull(method, "getArena method should exist");
      assertEquals(Arena.class, method.getReturnType(), "getArena should return Arena");
    }
  }

  @Nested
  @DisplayName("Allocation Method Tests")
  class AllocationMethodTests {

    @Test
    @DisplayName("should have allocate method with size")
    void shouldHaveAllocateWithSizeMethod() throws NoSuchMethodException {
      final Method method = ArenaResourceManager.class.getMethod("allocate", long.class);
      assertNotNull(method, "allocate(long) method should exist");
      assertEquals(
          ArenaResourceManager.ManagedMemorySegment.class,
          method.getReturnType(),
          "allocate should return ManagedMemorySegment");
    }
  }

  @Nested
  @DisplayName("Resource Management Method Tests")
  class ResourceManagementMethodTests {

    @Test
    @DisplayName("should have manageNativeResource method")
    void shouldHaveManageNativeResourceMethod() throws NoSuchMethodException {
      final Method method =
          ArenaResourceManager.class.getMethod(
              "manageNativeResource", MemorySegment.class, Runnable.class, String.class);
      assertNotNull(method, "manageNativeResource method should exist");
      assertEquals(
          ArenaResourceManager.ManagedNativeResource.class,
          method.getReturnType(),
          "manageNativeResource should return ManagedNativeResource");
    }

    @Test
    @DisplayName("should have registerManagedNativeResource method")
    void shouldHaveRegisterManagedNativeResourceMethod() throws NoSuchMethodException {
      final Method method =
          ArenaResourceManager.class.getMethod(
              "registerManagedNativeResource", Object.class, MemorySegment.class, Runnable.class);
      assertNotNull(method, "registerManagedNativeResource method should exist");
      assertEquals(
          ArenaResourceManager.ManagedNativeResource.class,
          method.getReturnType(),
          "registerManagedNativeResource should return ManagedNativeResource");
    }

    @Test
    @DisplayName("should have unregisterManagedResource method")
    void shouldHaveUnregisterManagedResourceMethod() throws NoSuchMethodException {
      final Method method =
          ArenaResourceManager.class.getMethod("unregisterManagedResource", Object.class);
      assertNotNull(method, "unregisterManagedResource method should exist");
      assertEquals(
          void.class, method.getReturnType(), "unregisterManagedResource should return void");
    }
  }

  @Nested
  @DisplayName("State and Tracking Method Tests")
  class StateAndTrackingMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = ArenaResourceManager.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = ArenaResourceManager.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          ArenaResourceManager.Statistics.class,
          method.getReturnType(),
          "getStatistics should return Statistics");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = ArenaResourceManager.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }

  @Nested
  @DisplayName("Nested Class Tests")
  class NestedClassTests {

    @Test
    @DisplayName("should have ManagedMemorySegment nested class")
    void shouldHaveManagedMemorySegmentNestedClass() {
      final Class<?>[] declaredClasses = ArenaResourceManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ManagedMemorySegment")) {
          found = true;
          assertTrue(
              Modifier.isPublic(clazz.getModifiers()), "ManagedMemorySegment should be public");
          assertTrue(
              Modifier.isFinal(clazz.getModifiers()), "ManagedMemorySegment should be final");
          assertTrue(
              Modifier.isStatic(clazz.getModifiers()), "ManagedMemorySegment should be static");
          break;
        }
      }
      assertTrue(found, "ManagedMemorySegment nested class should exist");
    }

    @Test
    @DisplayName("should have ManagedNativeResource nested class")
    void shouldHaveManagedNativeResourceNestedClass() {
      final Class<?>[] declaredClasses = ArenaResourceManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("ManagedNativeResource")) {
          found = true;
          assertTrue(
              Modifier.isPublic(clazz.getModifiers()), "ManagedNativeResource should be public");
          assertTrue(
              Modifier.isFinal(clazz.getModifiers()), "ManagedNativeResource should be final");
          assertTrue(
              Modifier.isStatic(clazz.getModifiers()), "ManagedNativeResource should be static");
          assertTrue(
              AutoCloseable.class.isAssignableFrom(clazz),
              "ManagedNativeResource should implement AutoCloseable");
          break;
        }
      }
      assertTrue(found, "ManagedNativeResource nested class should exist");
    }

    @Test
    @DisplayName("should have Statistics nested class")
    void shouldHaveStatisticsNestedClass() {
      final Class<?>[] declaredClasses = ArenaResourceManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("Statistics")) {
          found = true;
          assertTrue(Modifier.isPublic(clazz.getModifiers()), "Statistics should be public");
          assertTrue(Modifier.isFinal(clazz.getModifiers()), "Statistics should be final");
          assertTrue(Modifier.isStatic(clazz.getModifiers()), "Statistics should be static");
          break;
        }
      }
      assertTrue(found, "Statistics nested class should exist");
    }
  }
}
