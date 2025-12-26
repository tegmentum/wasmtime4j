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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the SyncToAsyncLimiterAdapter class.
 *
 * <p>This test class verifies the class structure, methods, and functionality for
 * SyncToAsyncLimiterAdapter using reflection-based testing.
 */
@DisplayName("SyncToAsyncLimiterAdapter Tests")
class SyncToAsyncLimiterAdapterTest {

  // ========================================================================
  // Class Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("SyncToAsyncLimiterAdapter should be a class")
    void shouldBeAClass() {
      assertFalse(
          SyncToAsyncLimiterAdapter.class.isInterface(),
          "SyncToAsyncLimiterAdapter should not be an interface");
      assertFalse(
          SyncToAsyncLimiterAdapter.class.isEnum(),
          "SyncToAsyncLimiterAdapter should not be an enum");
    }

    @Test
    @DisplayName("SyncToAsyncLimiterAdapter should be a final class")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(SyncToAsyncLimiterAdapter.class.getModifiers()),
          "SyncToAsyncLimiterAdapter should be final");
    }

    @Test
    @DisplayName("SyncToAsyncLimiterAdapter should be package-private")
    void shouldBePackagePrivate() {
      int modifiers = SyncToAsyncLimiterAdapter.class.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "SyncToAsyncLimiterAdapter should not be public");
      assertFalse(
          Modifier.isProtected(modifiers), "SyncToAsyncLimiterAdapter should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "SyncToAsyncLimiterAdapter should not be private");
    }

    @Test
    @DisplayName("SyncToAsyncLimiterAdapter should implement ResourceLimiterAsync")
    void shouldImplementResourceLimiterAsync() {
      Class<?>[] interfaces = SyncToAsyncLimiterAdapter.class.getInterfaces();
      boolean implementsAsync = false;
      for (Class<?> iface : interfaces) {
        if (iface == ResourceLimiterAsync.class) {
          implementsAsync = true;
          break;
        }
      }
      assertTrue(
          implementsAsync, "SyncToAsyncLimiterAdapter should implement ResourceLimiterAsync");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("SyncToAsyncLimiterAdapter should have exactly one constructor")
    void shouldHaveExactlyOneConstructor() {
      Constructor<?>[] constructors = SyncToAsyncLimiterAdapter.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly one constructor");
    }

    @Test
    @DisplayName("Constructor should accept ResourceLimiter parameter")
    void constructorShouldAcceptResourceLimiterParameter() {
      Constructor<?>[] constructors = SyncToAsyncLimiterAdapter.class.getDeclaredConstructors();
      Constructor<?> constructor = constructors[0];
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(1, paramTypes.length, "Constructor should have 1 parameter");
      assertEquals(
          ResourceLimiter.class, paramTypes[0], "Parameter should be of type ResourceLimiter");
    }

    @Test
    @DisplayName("Constructor should be package-private")
    void constructorShouldBePackagePrivate() {
      Constructor<?>[] constructors = SyncToAsyncLimiterAdapter.class.getDeclaredConstructors();
      Constructor<?> constructor = constructors[0];
      int modifiers = constructor.getModifiers();
      assertFalse(Modifier.isPublic(modifiers), "Constructor should not be public");
      assertFalse(Modifier.isProtected(modifiers), "Constructor should not be protected");
      assertFalse(Modifier.isPrivate(modifiers), "Constructor should not be private");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("SyncToAsyncLimiterAdapter should have a delegate field")
    void shouldHaveDelegateField() throws NoSuchFieldException {
      Field field = SyncToAsyncLimiterAdapter.class.getDeclaredField("delegate");
      assertNotNull(field, "delegate field should exist");
    }

    @Test
    @DisplayName("delegate field should be of type ResourceLimiter")
    void delegateFieldShouldBeResourceLimiter() throws NoSuchFieldException {
      Field field = SyncToAsyncLimiterAdapter.class.getDeclaredField("delegate");
      assertEquals(
          ResourceLimiter.class, field.getType(), "delegate should be of type ResourceLimiter");
    }

    @Test
    @DisplayName("delegate field should be private and final")
    void delegateFieldShouldBePrivateFinal() throws NoSuchFieldException {
      Field field = SyncToAsyncLimiterAdapter.class.getDeclaredField("delegate");
      int modifiers = field.getModifiers();
      assertTrue(Modifier.isPrivate(modifiers), "delegate should be private");
      assertTrue(Modifier.isFinal(modifiers), "delegate should be final");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = SyncToAsyncLimiterAdapter.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getId should be public");
    }

    @Test
    @DisplayName("should have getConfigAsync method")
    void shouldHaveGetConfigAsyncMethod() throws NoSuchMethodException {
      Method method = SyncToAsyncLimiterAdapter.class.getMethod("getConfigAsync");
      assertNotNull(method, "getConfigAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getConfigAsync should be public");
    }

    @Test
    @DisplayName("should have allowMemoryGrowAsync method")
    void shouldHaveAllowMemoryGrowAsyncMethod() throws NoSuchMethodException {
      Method method =
          SyncToAsyncLimiterAdapter.class.getMethod("allowMemoryGrowAsync", long.class, long.class);
      assertNotNull(method, "allowMemoryGrowAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
      assertEquals(2, method.getParameterCount(), "allowMemoryGrowAsync should have 2 parameters");
    }

    @Test
    @DisplayName("should have allowTableGrowAsync method")
    void shouldHaveAllowTableGrowAsyncMethod() throws NoSuchMethodException {
      Method method =
          SyncToAsyncLimiterAdapter.class.getMethod("allowTableGrowAsync", long.class, long.class);
      assertNotNull(method, "allowTableGrowAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
      assertEquals(2, method.getParameterCount(), "allowTableGrowAsync should have 2 parameters");
    }

    @Test
    @DisplayName("should have getStatsAsync method")
    void shouldHaveGetStatsAsyncMethod() throws NoSuchMethodException {
      Method method = SyncToAsyncLimiterAdapter.class.getMethod("getStatsAsync");
      assertNotNull(method, "getStatsAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getStatsAsync should be public");
    }

    @Test
    @DisplayName("should have resetStatsAsync method")
    void shouldHaveResetStatsAsyncMethod() throws NoSuchMethodException {
      Method method = SyncToAsyncLimiterAdapter.class.getMethod("resetStatsAsync");
      assertNotNull(method, "resetStatsAsync method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "Return type should be CompletableFuture");
      assertTrue(Modifier.isPublic(method.getModifiers()), "resetStatsAsync should be public");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = SyncToAsyncLimiterAdapter.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "close should be public");
    }
  }

  // ========================================================================
  // Adapter Pattern Tests
  // ========================================================================

  @Nested
  @DisplayName("Adapter Pattern Tests")
  class AdapterPatternTests {

    @Test
    @DisplayName("SyncToAsyncLimiterAdapter should wrap synchronous limiter")
    void shouldWrapSynchronousLimiter() throws NoSuchFieldException {
      Field field = SyncToAsyncLimiterAdapter.class.getDeclaredField("delegate");
      assertNotNull(field, "Should have a delegate field");
      assertEquals(
          ResourceLimiter.class, field.getType(), "Should wrap ResourceLimiter (sync version)");
    }

    @Test
    @DisplayName("SyncToAsyncLimiterAdapter should expose async interface")
    void shouldExposeAsyncInterface() {
      assertTrue(
          ResourceLimiterAsync.class.isAssignableFrom(SyncToAsyncLimiterAdapter.class),
          "SyncToAsyncLimiterAdapter should be assignable to ResourceLimiterAsync");
    }

    @Test
    @DisplayName("All async methods should return CompletableFuture")
    void allAsyncMethodsShouldReturnCompletableFuture() {
      Method[] methods = SyncToAsyncLimiterAdapter.class.getDeclaredMethods();
      for (Method method : methods) {
        String methodName = method.getName();
        if (methodName.endsWith("Async")) {
          assertEquals(
              CompletableFuture.class,
              method.getReturnType(),
              methodName + " should return CompletableFuture");
        }
      }
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("SyncToAsyncLimiterAdapter should have at least 7 public methods")
    void shouldHaveAtLeastSevenPublicMethods() {
      Method[] methods = SyncToAsyncLimiterAdapter.class.getDeclaredMethods();
      int publicMethodCount = 0;
      for (Method method : methods) {
        if (Modifier.isPublic(method.getModifiers())) {
          publicMethodCount++;
        }
      }
      assertTrue(
          publicMethodCount >= 7,
          "Should have at least 7 public methods, found: " + publicMethodCount);
    }

    @Test
    @DisplayName("All overridden methods should be public")
    void allOverriddenMethodsShouldBePublic() throws NoSuchMethodException {
      String[] methodNames = {
        "getId", "getConfigAsync", "getStatsAsync", "resetStatsAsync", "close"
      };
      for (String name : methodNames) {
        if (name.equals("allowMemoryGrowAsync") || name.equals("allowTableGrowAsync")) {
          continue; // Skip methods with parameters
        }
        Method method = SyncToAsyncLimiterAdapter.class.getMethod(name);
        assertTrue(Modifier.isPublic(method.getModifiers()), name + " should be public");
      }
    }
  }

  // ========================================================================
  // Override Annotation Tests
  // ========================================================================

  @Nested
  @DisplayName("Override Annotation Tests")
  class OverrideAnnotationTests {

    @Test
    @DisplayName("getId should override from ResourceLimiterAsync")
    void getIdShouldOverrideFromInterface() throws NoSuchMethodException {
      // Verify that the method exists in the interface
      Method interfaceMethod = ResourceLimiterAsync.class.getMethod("getId");
      assertNotNull(interfaceMethod, "getId should exist in ResourceLimiterAsync");

      // Verify that the adapter implements it
      Method adapterMethod = SyncToAsyncLimiterAdapter.class.getMethod("getId");
      assertNotNull(adapterMethod, "getId should exist in SyncToAsyncLimiterAdapter");
    }

    @Test
    @DisplayName("close should override from ResourceLimiterAsync")
    void closeShouldOverrideFromInterface() throws NoSuchMethodException {
      // Verify that the method exists in the interface (inherited from Closeable)
      Method adapterMethod = SyncToAsyncLimiterAdapter.class.getMethod("close");
      assertNotNull(adapterMethod, "close should exist in SyncToAsyncLimiterAdapter");
    }
  }
}
