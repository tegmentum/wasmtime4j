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
 * Tests for {@link WasiFileHandleManager} class.
 *
 * <p>WasiFileHandleManager provides comprehensive file handle management and resource cleanup
 * system for WASI file operations in Panama FFI context.
 */
@DisplayName("WasiFileHandleManager Tests")
class WasiFileHandleManagerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiFileHandleManager.class.getModifiers()),
          "WasiFileHandleManager should be public");
      assertTrue(
          Modifier.isFinal(WasiFileHandleManager.class.getModifiers()),
          "WasiFileHandleManager should be final");
    }

    @Test
    @DisplayName("should implement AutoCloseable interface")
    void shouldImplementAutoCloseableInterface() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiFileHandleManager.class),
          "WasiFileHandleManager should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = WasiFileHandleManager.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("should have constructor with max handles and timeout")
    void shouldHaveParameterizedConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiFileHandleManager.class.getConstructor(int.class, int.class);
      assertNotNull(constructor, "Constructor with parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Handle Registration Method Tests")
  class HandleRegistrationMethodTests {

    @Test
    @DisplayName("should have registerHandle method")
    void shouldHaveRegisterHandleMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileHandleManager.class.getMethod("registerHandle", WasiFileHandle.class);
      assertNotNull(method, "registerHandle method should exist");
      assertEquals(
          WasiFileHandleManager.ManagedFileHandle.class,
          method.getReturnType(),
          "Should return ManagedFileHandle");
    }

    @Test
    @DisplayName("should have getHandle method")
    void shouldHaveGetHandleMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandleManager.class.getMethod("getHandle", int.class);
      assertNotNull(method, "getHandle method should exist");
      assertEquals(
          WasiFileHandleManager.ManagedFileHandle.class,
          method.getReturnType(),
          "Should return ManagedFileHandle");
    }

    @Test
    @DisplayName("should have unregisterHandle method")
    void shouldHaveUnregisterHandleMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandleManager.class.getMethod("unregisterHandle", int.class);
      assertNotNull(method, "unregisterHandle method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getActiveHandleCount method")
    void shouldHaveGetActiveHandleCountMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandleManager.class.getMethod("getActiveHandleCount");
      assertNotNull(method, "getActiveHandleCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandleManager.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
      assertEquals(
          WasiFileHandleManager.HandleManagerStats.class,
          method.getReturnType(),
          "Should return HandleManagerStats");
    }
  }

  @Nested
  @DisplayName("Cleanup Method Tests")
  class CleanupMethodTests {

    @Test
    @DisplayName("should have forceCleanup method")
    void shouldHaveForceCleanupMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandleManager.class.getMethod("forceCleanup");
      assertNotNull(method, "forceCleanup method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandleManager.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("ManagedFileHandle Inner Class Tests")
  class ManagedFileHandleTests {

    @Test
    @DisplayName("ManagedFileHandle should be public static final class")
    void managedFileHandleShouldBePublicStaticFinal() {
      Class<?> innerClass = WasiFileHandleManager.ManagedFileHandle.class;
      assertTrue(Modifier.isPublic(innerClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(innerClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(innerClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("ManagedFileHandle should have getHandle method")
    void managedFileHandleShouldHaveGetHandleMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandleManager.ManagedFileHandle.class.getMethod("getHandle");
      assertNotNull(method, "getHandle method should exist");
      assertEquals(WasiFileHandle.class, method.getReturnType(), "Should return WasiFileHandle");
    }

    @Test
    @DisplayName("ManagedFileHandle should have getLastAccessTime method")
    void managedFileHandleShouldHaveGetLastAccessTimeMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileHandleManager.ManagedFileHandle.class.getMethod("getLastAccessTime");
      assertNotNull(method, "getLastAccessTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("HandleManagerStats Inner Class Tests")
  class HandleManagerStatsTests {

    @Test
    @DisplayName("HandleManagerStats should be public static final class")
    void handleManagerStatsShouldBePublicStaticFinal() {
      Class<?> innerClass = WasiFileHandleManager.HandleManagerStats.class;
      assertTrue(Modifier.isPublic(innerClass.getModifiers()), "Should be public");
      assertTrue(Modifier.isStatic(innerClass.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(innerClass.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("HandleManagerStats should have getActiveHandles method")
    void handleManagerStatsShouldHaveGetActiveHandlesMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileHandleManager.HandleManagerStats.class.getMethod("getActiveHandles");
      assertNotNull(method, "getActiveHandles method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("HandleManagerStats should have getMaxHandles method")
    void handleManagerStatsShouldHaveGetMaxHandlesMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileHandleManager.HandleManagerStats.class.getMethod("getMaxHandles");
      assertNotNull(method, "getMaxHandles method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("HandleManagerStats should have getTotalHandlesCreated method")
    void handleManagerStatsShouldHaveGetTotalHandlesCreatedMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileHandleManager.HandleManagerStats.class.getMethod("getTotalHandlesCreated");
      assertNotNull(method, "getTotalHandlesCreated method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("HandleManagerStats should have getTotalHandlesClosed method")
    void handleManagerStatsShouldHaveGetTotalHandlesClosedMethod() throws NoSuchMethodException {
      final Method method =
          WasiFileHandleManager.HandleManagerStats.class.getMethod("getTotalHandlesClosed");
      assertNotNull(method, "getTotalHandlesClosed method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("HandleManagerStats should have getTotalHandlesGarbageCollected method")
    void handleManagerStatsShouldHaveGetTotalHandlesGarbageCollectedMethod()
        throws NoSuchMethodException {
      final Method method =
          WasiFileHandleManager.HandleManagerStats.class.getMethod(
              "getTotalHandlesGarbageCollected");
      assertNotNull(method, "getTotalHandlesGarbageCollected method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }
}
