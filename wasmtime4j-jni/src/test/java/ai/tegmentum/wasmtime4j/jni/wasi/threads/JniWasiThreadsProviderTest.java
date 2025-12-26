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

package ai.tegmentum.wasmtime4j.jni.wasi.threads;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContextBuilder;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsProvider;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link JniWasiThreadsProvider}. */
@DisplayName("JniWasiThreadsProvider Tests")
class JniWasiThreadsProviderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniWasiThreadsProvider should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiThreadsProvider.class.getModifiers()),
          "JniWasiThreadsProvider should be final");
    }

    @Test
    @DisplayName("JniWasiThreadsProvider should implement WasiThreadsProvider")
    void shouldImplementWasiThreadsProvider() {
      assertTrue(
          WasiThreadsProvider.class.isAssignableFrom(JniWasiThreadsProvider.class),
          "JniWasiThreadsProvider should implement WasiThreadsProvider");
    }

    @Test
    @DisplayName("JniWasiThreadsProvider should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(JniWasiThreadsProvider.class.getModifiers()),
          "JniWasiThreadsProvider should be public");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have public no-arg constructor for ServiceLoader")
    void shouldHavePublicNoArgConstructor() {
      assertDoesNotThrow(
          () -> JniWasiThreadsProvider.class.getConstructor(),
          "Should have public no-arg constructor");
    }

    @Test
    @DisplayName("Should be instantiable via no-arg constructor")
    void shouldBeInstantiable() {
      JniWasiThreadsProvider provider = new JniWasiThreadsProvider();
      assertNotNull(provider, "Should be instantiable");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("isAvailable method should exist and return boolean")
    void isAvailableMethodShouldExist() throws NoSuchMethodException {
      Method method = JniWasiThreadsProvider.class.getMethod("isAvailable");
      assertNotNull(method, "isAvailable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("createBuilder method should exist and return WasiThreadsContextBuilder")
    void createBuilderMethodShouldExist() throws NoSuchMethodException {
      Method method = JniWasiThreadsProvider.class.getMethod("createBuilder");
      assertNotNull(method, "createBuilder method should exist");
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "Should return WasiThreadsContextBuilder");
    }

    @Test
    @DisplayName("addToLinker method should exist with correct parameters")
    void addToLinkerMethodShouldExist() throws NoSuchMethodException {
      Method method =
          JniWasiThreadsProvider.class.getMethod(
              "addToLinker", Linker.class, Store.class, Module.class);
      assertNotNull(method, "addToLinker method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      Field field = JniWasiThreadsProvider.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertEquals(Logger.class, field.getType(), "Should be Logger type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have available cache field")
    void shouldHaveAvailableCacheField() throws NoSuchFieldException {
      Field field = JniWasiThreadsProvider.class.getDeclaredField("available");
      assertNotNull(field, "available field should exist");
      assertEquals(Boolean.class, field.getType(), "Should be Boolean type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "Should be volatile");
    }
  }

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("Should have checkAvailability private method")
    void shouldHaveCheckAvailabilityMethod() throws NoSuchMethodException {
      Method method = JniWasiThreadsProvider.class.getDeclaredMethod("checkAvailability");
      assertNotNull(method, "checkAvailability method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("Should implement all WasiThreadsProvider interface methods")
    void shouldImplementAllInterfaceMethods() {
      for (Method interfaceMethod : WasiThreadsProvider.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : JniWasiThreadsProvider.class.getMethods()) {
            if (implMethod.getName().equals(interfaceMethod.getName())
                && arrayEquals(
                    implMethod.getParameterTypes(), interfaceMethod.getParameterTypes())) {
              found = true;
              break;
            }
          }
          assertTrue(found, "Should implement interface method: " + interfaceMethod.getName());
        }
      }
    }

    private boolean arrayEquals(Class<?>[] a, Class<?>[] b) {
      if (a.length != b.length) {
        return false;
      }
      for (int i = 0; i < a.length; i++) {
        if (!a[i].equals(b[i])) {
          return false;
        }
      }
      return true;
    }
  }

  @Nested
  @DisplayName("Double-Checked Locking Tests")
  class DoubleCheckedLockingTests {

    @Test
    @DisplayName("isAvailable should use cached result on subsequent calls")
    void isAvailableShouldUseCachedResult() {
      JniWasiThreadsProvider provider = new JniWasiThreadsProvider();

      // First call initializes the cache
      boolean firstResult = provider.isAvailable();

      // Subsequent calls should return same result (from cache)
      boolean secondResult = provider.isAvailable();
      boolean thirdResult = provider.isAvailable();

      assertEquals(firstResult, secondResult, "Results should be consistent");
      assertEquals(secondResult, thirdResult, "Results should be consistent");
    }
  }

  @Nested
  @DisplayName("Package Location Tests")
  class PackageLocationTests {

    @Test
    @DisplayName("Class should be in correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.wasi.threads",
          JniWasiThreadsProvider.class.getPackage().getName(),
          "Should be in jni.wasi.threads package");
    }
  }
}
