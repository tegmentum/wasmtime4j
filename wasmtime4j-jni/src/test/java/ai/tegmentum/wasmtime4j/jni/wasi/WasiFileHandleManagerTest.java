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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiFileHandleManager} class.
 *
 * <p>WasiFileHandleManager provides comprehensive file handle management and resource cleanup for
 * WASI file operations. These tests verify the class structure, method signatures, and inner types.
 */
@DisplayName("WasiFileHandleManager Tests")
class WasiFileHandleManagerTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertThat(Modifier.isFinal(WasiFileHandleManager.class.getModifiers()))
          .as("WasiFileHandleManager should be final")
          .isTrue();
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertThat(Modifier.isPublic(WasiFileHandleManager.class.getModifiers()))
          .as("WasiFileHandleManager should be public")
          .isTrue();
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertThat(AutoCloseable.class.isAssignableFrom(WasiFileHandleManager.class))
          .as("WasiFileHandleManager should implement AutoCloseable")
          .isTrue();
    }

    @Test
    @DisplayName("should have private LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      final Field field = WasiFileHandleManager.class.getDeclaredField("LOGGER");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have DEFAULT_MAX_HANDLES constant")
    void shouldHaveDefaultMaxHandlesConstant() throws NoSuchFieldException {
      final Field field = WasiFileHandleManager.class.getDeclaredField("DEFAULT_MAX_HANDLES");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("should have DEFAULT_CLEANUP_INTERVAL_SECONDS constant")
    void shouldHaveDefaultCleanupIntervalConstant() throws NoSuchFieldException {
      final Field field =
          WasiFileHandleManager.class.getDeclaredField("DEFAULT_CLEANUP_INTERVAL_SECONDS");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("should have DEFAULT_HANDLE_TIMEOUT_SECONDS constant")
    void shouldHaveDefaultHandleTimeoutConstant() throws NoSuchFieldException {
      final Field field =
          WasiFileHandleManager.class.getDeclaredField("DEFAULT_HANDLE_TIMEOUT_SECONDS");
      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(int.class);
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have no-arg constructor")
    void shouldHaveNoArgConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = WasiFileHandleManager.class.getConstructor();
      assertThat(constructor).isNotNull();
      assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have constructor with maxHandles and timeout")
    void shouldHaveParameterizedConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasiFileHandleManager.class.getConstructor(int.class, int.class);
      assertThat(constructor).isNotNull();
      assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
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
      assertThat(method.getReturnType().getSimpleName()).isEqualTo("ManagedFileHandle");
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have getHandle method")
    void shouldHaveGetHandleMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandleManager.class.getMethod("getHandle", int.class);
      assertThat(method.getReturnType().getSimpleName()).isEqualTo("ManagedFileHandle");
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have unregisterHandle method")
    void shouldHaveUnregisterHandleMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandleManager.class.getMethod("unregisterHandle", int.class);
      assertThat(method.getReturnType()).isEqualTo(void.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getActiveHandleCount method")
    void shouldHaveGetActiveHandleCountMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandleManager.class.getMethod("getActiveHandleCount");
      assertThat(method.getReturnType()).isEqualTo(int.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandleManager.class.getMethod("getStats");
      assertThat(method.getReturnType().getSimpleName()).isEqualTo("HandleManagerStats");
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("Cleanup Method Tests")
  class CleanupMethodTests {

    @Test
    @DisplayName("should have forceCleanup method")
    void shouldHaveForceCleanupMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandleManager.class.getMethod("forceCleanup");
      assertThat(method.getReturnType()).isEqualTo(int.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = WasiFileHandleManager.class.getMethod("close");
      assertThat(method.getReturnType()).isEqualTo(void.class);
      assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("ManagedFileHandle Inner Class Tests")
  class ManagedFileHandleInnerClassTests {

    @Test
    @DisplayName("should have ManagedFileHandle inner class")
    void shouldHaveManagedFileHandleInnerClass() {
      Class<?>[] declaredClasses = WasiFileHandleManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> innerClass : declaredClasses) {
        if (innerClass.getSimpleName().equals("ManagedFileHandle")) {
          found = true;
          assertThat(Modifier.isPublic(innerClass.getModifiers()))
              .as("ManagedFileHandle should be public")
              .isTrue();
          assertThat(Modifier.isStatic(innerClass.getModifiers()))
              .as("ManagedFileHandle should be static")
              .isTrue();
          assertThat(Modifier.isFinal(innerClass.getModifiers()))
              .as("ManagedFileHandle should be final")
              .isTrue();
          break;
        }
      }
      assertThat(found).as("ManagedFileHandle inner class should exist").isTrue();
    }

    @Test
    @DisplayName("ManagedFileHandle should have getHandle method")
    void managedFileHandleShouldHaveGetHandleMethod() throws ClassNotFoundException {
      final Class<?> managedFileHandleClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.WasiFileHandleManager$ManagedFileHandle");
      boolean hasGetHandle = false;
      for (final Method method : managedFileHandleClass.getDeclaredMethods()) {
        if (method.getName().equals("getHandle") && method.getParameterCount() == 0) {
          hasGetHandle = true;
          assertThat(method.getReturnType()).isEqualTo(WasiFileHandle.class);
          assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
          break;
        }
      }
      assertThat(hasGetHandle).as("ManagedFileHandle should have getHandle method").isTrue();
    }

    @Test
    @DisplayName("ManagedFileHandle should have getLastAccessTime method")
    void managedFileHandleShouldHaveGetLastAccessTimeMethod() throws ClassNotFoundException {
      final Class<?> managedFileHandleClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.wasi.WasiFileHandleManager$ManagedFileHandle");
      boolean hasGetLastAccessTime = false;
      for (final Method method : managedFileHandleClass.getDeclaredMethods()) {
        if (method.getName().equals("getLastAccessTime") && method.getParameterCount() == 0) {
          hasGetLastAccessTime = true;
          assertThat(method.getReturnType()).isEqualTo(long.class);
          assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
          break;
        }
      }
      assertThat(hasGetLastAccessTime)
          .as("ManagedFileHandle should have getLastAccessTime method")
          .isTrue();
    }
  }

  @Nested
  @DisplayName("HandleManagerStats Inner Class Tests")
  class HandleManagerStatsInnerClassTests {

    @Test
    @DisplayName("should have HandleManagerStats inner class")
    void shouldHaveHandleManagerStatsInnerClass() {
      Class<?>[] declaredClasses = WasiFileHandleManager.class.getDeclaredClasses();
      boolean found = false;
      for (final Class<?> innerClass : declaredClasses) {
        if (innerClass.getSimpleName().equals("HandleManagerStats")) {
          found = true;
          assertThat(Modifier.isPublic(innerClass.getModifiers()))
              .as("HandleManagerStats should be public")
              .isTrue();
          assertThat(Modifier.isStatic(innerClass.getModifiers()))
              .as("HandleManagerStats should be static")
              .isTrue();
          assertThat(Modifier.isFinal(innerClass.getModifiers()))
              .as("HandleManagerStats should be final")
              .isTrue();
          break;
        }
      }
      assertThat(found).as("HandleManagerStats inner class should exist").isTrue();
    }

    @Test
    @DisplayName("HandleManagerStats should have getActiveHandles method")
    void handleManagerStatsShouldHaveGetActiveHandlesMethod() throws ClassNotFoundException {
      final Class<?> statsClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.jni.wasi.WasiFileHandleManager$HandleManagerStats");
      boolean hasMethod = false;
      for (final Method method : statsClass.getDeclaredMethods()) {
        if (method.getName().equals("getActiveHandles") && method.getParameterCount() == 0) {
          hasMethod = true;
          assertThat(method.getReturnType()).isEqualTo(int.class);
          assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
          break;
        }
      }
      assertThat(hasMethod).as("HandleManagerStats should have getActiveHandles method").isTrue();
    }

    @Test
    @DisplayName("HandleManagerStats should have getMaxHandles method")
    void handleManagerStatsShouldHaveGetMaxHandlesMethod() throws ClassNotFoundException {
      final Class<?> statsClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.jni.wasi.WasiFileHandleManager$HandleManagerStats");
      boolean hasMethod = false;
      for (final Method method : statsClass.getDeclaredMethods()) {
        if (method.getName().equals("getMaxHandles") && method.getParameterCount() == 0) {
          hasMethod = true;
          assertThat(method.getReturnType()).isEqualTo(int.class);
          assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
          break;
        }
      }
      assertThat(hasMethod).as("HandleManagerStats should have getMaxHandles method").isTrue();
    }

    @Test
    @DisplayName("HandleManagerStats should have getTotalHandlesCreated method")
    void handleManagerStatsShouldHaveGetTotalHandlesCreatedMethod() throws ClassNotFoundException {
      final Class<?> statsClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.jni.wasi.WasiFileHandleManager$HandleManagerStats");
      boolean hasMethod = false;
      for (final Method method : statsClass.getDeclaredMethods()) {
        if (method.getName().equals("getTotalHandlesCreated") && method.getParameterCount() == 0) {
          hasMethod = true;
          assertThat(method.getReturnType()).isEqualTo(long.class);
          assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
          break;
        }
      }
      assertThat(hasMethod)
          .as("HandleManagerStats should have getTotalHandlesCreated method")
          .isTrue();
    }

    @Test
    @DisplayName("HandleManagerStats should have getTotalHandlesClosed method")
    void handleManagerStatsShouldHaveGetTotalHandlesClosedMethod() throws ClassNotFoundException {
      final Class<?> statsClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.jni.wasi.WasiFileHandleManager$HandleManagerStats");
      boolean hasMethod = false;
      for (final Method method : statsClass.getDeclaredMethods()) {
        if (method.getName().equals("getTotalHandlesClosed") && method.getParameterCount() == 0) {
          hasMethod = true;
          assertThat(method.getReturnType()).isEqualTo(long.class);
          assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
          break;
        }
      }
      assertThat(hasMethod)
          .as("HandleManagerStats should have getTotalHandlesClosed method")
          .isTrue();
    }

    @Test
    @DisplayName("HandleManagerStats should have getTotalHandlesGarbageCollected method")
    void handleManagerStatsShouldHaveGetTotalHandlesGarbageCollectedMethod()
        throws ClassNotFoundException {
      final Class<?> statsClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.jni.wasi.WasiFileHandleManager$HandleManagerStats");
      boolean hasMethod = false;
      for (final Method method : statsClass.getDeclaredMethods()) {
        if (method.getName().equals("getTotalHandlesGarbageCollected")
            && method.getParameterCount() == 0) {
          hasMethod = true;
          assertThat(method.getReturnType()).isEqualTo(long.class);
          assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
          break;
        }
      }
      assertThat(hasMethod)
          .as("HandleManagerStats should have getTotalHandlesGarbageCollected method")
          .isTrue();
    }

    @Test
    @DisplayName("HandleManagerStats should have getPhantomReferences method")
    void handleManagerStatsShouldHaveGetPhantomReferencesMethod() throws ClassNotFoundException {
      final Class<?> statsClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.jni.wasi.WasiFileHandleManager$HandleManagerStats");
      boolean hasMethod = false;
      for (final Method method : statsClass.getDeclaredMethods()) {
        if (method.getName().equals("getPhantomReferences") && method.getParameterCount() == 0) {
          hasMethod = true;
          assertThat(method.getReturnType()).isEqualTo(int.class);
          assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
          break;
        }
      }
      assertThat(hasMethod)
          .as("HandleManagerStats should have getPhantomReferences method")
          .isTrue();
    }

    @Test
    @DisplayName("HandleManagerStats should have toString method")
    void handleManagerStatsShouldHaveToStringMethod() throws ClassNotFoundException {
      final Class<?> statsClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.jni.wasi.WasiFileHandleManager$HandleManagerStats");
      boolean hasToString = false;
      for (final Method method : statsClass.getDeclaredMethods()) {
        if (method.getName().equals("toString") && method.getParameterCount() == 0) {
          hasToString = true;
          assertThat(method.getReturnType()).isEqualTo(String.class);
          assertThat(Modifier.isPublic(method.getModifiers())).isTrue();
          break;
        }
      }
      assertThat(hasToString).as("HandleManagerStats should have toString method").isTrue();
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected number of public methods")
    void shouldHaveExpectedPublicMethods() {
      long publicMethodCount =
          java.util.Arrays.stream(WasiFileHandleManager.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();

      // Expected methods: registerHandle, getHandle, unregisterHandle, getActiveHandleCount,
      // getStats, forceCleanup, close
      assertThat(publicMethodCount).isGreaterThanOrEqualTo(7);
    }
  }
}
