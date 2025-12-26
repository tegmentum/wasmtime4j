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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link JniWasiThreadsContext}. */
@DisplayName("JniWasiThreadsContext Tests")
class JniWasiThreadsContextTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniWasiThreadsContext should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiThreadsContext.class.getModifiers()),
          "JniWasiThreadsContext should be final");
    }

    @Test
    @DisplayName("JniWasiThreadsContext should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniWasiThreadsContext.class),
          "JniWasiThreadsContext should extend JniResource");
    }

    @Test
    @DisplayName("JniWasiThreadsContext should implement WasiThreadsContext")
    void shouldImplementWasiThreadsContext() {
      assertTrue(
          WasiThreadsContext.class.isAssignableFrom(JniWasiThreadsContext.class),
          "JniWasiThreadsContext should implement WasiThreadsContext");
    }

    @Test
    @DisplayName("JniWasiThreadsContext should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(JniWasiThreadsContext.class.getModifiers()),
          "JniWasiThreadsContext should be public");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have package-private constructor with long and boolean parameters")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      Constructor<JniWasiThreadsContext> constructor =
          JniWasiThreadsContext.class.getDeclaredConstructor(long.class, boolean.class);
      assertNotNull(constructor, "Constructor should exist");
      // Package-private: not public, not private, not protected
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "Constructor should be package-private");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("spawn method should exist and return int")
    void spawnMethodShouldExist() throws NoSuchMethodException {
      Method method = JniWasiThreadsContext.class.getMethod("spawn", int.class);
      assertNotNull(method, "spawn method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("getThreadCount method should exist and return int")
    void getThreadCountMethodShouldExist() throws NoSuchMethodException {
      Method method = JniWasiThreadsContext.class.getMethod("getThreadCount");
      assertNotNull(method, "getThreadCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("isEnabled method should exist and return boolean")
    void isEnabledMethodShouldExist() throws NoSuchMethodException {
      Method method = JniWasiThreadsContext.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("getMaxThreadId method should exist and return int")
    void getMaxThreadIdMethodShouldExist() throws NoSuchMethodException {
      Method method = JniWasiThreadsContext.class.getMethod("getMaxThreadId");
      assertNotNull(method, "getMaxThreadId method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("isValid method should exist and return boolean")
    void isValidMethodShouldExist() throws NoSuchMethodException {
      Method method = JniWasiThreadsContext.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("onThreadCompleted method should exist with int parameter")
    void onThreadCompletedMethodShouldExist() throws NoSuchMethodException {
      Method method = JniWasiThreadsContext.class.getDeclaredMethod("onThreadCompleted", int.class);
      assertNotNull(method, "onThreadCompleted method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      // Package-private
      int modifiers = method.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isPrivate(modifiers)
              && !Modifier.isProtected(modifiers),
          "onThreadCompleted should be package-private");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      Field field = JniWasiThreadsContext.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertEquals(Logger.class, field.getType(), "Should be Logger type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have MAX_THREAD_ID constant")
    void shouldHaveMaxThreadIdConstant() throws NoSuchFieldException {
      Field field = JniWasiThreadsContext.class.getDeclaredField("MAX_THREAD_ID");
      assertNotNull(field, "MAX_THREAD_ID field should exist");
      assertEquals(int.class, field.getType(), "Should be int type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have maxThreadId AtomicInteger field")
    void shouldHaveMaxThreadIdField() throws NoSuchFieldException {
      Field field = JniWasiThreadsContext.class.getDeclaredField("maxThreadId");
      assertNotNull(field, "maxThreadId field should exist");
      assertEquals(AtomicInteger.class, field.getType(), "Should be AtomicInteger type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have threadCount AtomicInteger field")
    void shouldHaveThreadCountField() throws NoSuchFieldException {
      Field field = JniWasiThreadsContext.class.getDeclaredField("threadCount");
      assertNotNull(field, "threadCount field should exist");
      assertEquals(AtomicInteger.class, field.getType(), "Should be AtomicInteger type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have enabled boolean field")
    void shouldHaveEnabledField() throws NoSuchFieldException {
      Field field = JniWasiThreadsContext.class.getDeclaredField("enabled");
      assertNotNull(field, "enabled field should exist");
      assertEquals(boolean.class, field.getType(), "Should be boolean type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }
  }

  @Nested
  @DisplayName("Native Method Tests")
  class NativeMethodTests {

    @Test
    @DisplayName("Should have nativeSpawn native method")
    void shouldHaveNativeSpawnMethod() throws NoSuchMethodException {
      Method method =
          JniWasiThreadsContext.class.getDeclaredMethod("nativeSpawn", long.class, int.class);
      assertNotNull(method, "nativeSpawn method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("Should have nativeClose native method")
    void shouldHaveNativeCloseMethod() throws NoSuchMethodException {
      Method method = JniWasiThreadsContext.class.getDeclaredMethod("nativeClose", long.class);
      assertNotNull(method, "nativeClose method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("Should have nativeIsSupported native method")
    void shouldHaveNativeIsSupportedMethod() throws NoSuchMethodException {
      Method method = JniWasiThreadsContext.class.getDeclaredMethod("nativeIsSupported");
      assertNotNull(method, "nativeIsSupported method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("Should have nativeCreate native method")
    void shouldHaveNativeCreateMethod() throws NoSuchMethodException {
      Method method =
          JniWasiThreadsContext.class.getDeclaredMethod(
              "nativeCreate", long.class, long.class, long.class);
      assertNotNull(method, "nativeCreate method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("Should have nativeAddToLinker native method")
    void shouldHaveNativeAddToLinkerMethod() throws NoSuchMethodException {
      Method method =
          JniWasiThreadsContext.class.getDeclaredMethod(
              "nativeAddToLinker", long.class, long.class, long.class);
      assertNotNull(method, "nativeAddToLinker method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "Should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("JniResource Integration Tests")
  class JniResourceIntegrationTests {

    @Test
    @DisplayName("Should have getResourceType method from JniResource")
    void shouldHaveGetResourceTypeMethod() throws NoSuchMethodException {
      Method method = JniWasiThreadsContext.class.getDeclaredMethod("getResourceType");
      assertNotNull(method, "getResourceType method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("Should have doClose method from JniResource")
    void shouldHaveDoCloseMethod() throws NoSuchMethodException {
      Method method = JniWasiThreadsContext.class.getDeclaredMethod("doClose");
      assertNotNull(method, "doClose method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("Should implement all WasiThreadsContext interface methods")
    void shouldImplementAllInterfaceMethods() {
      for (Method interfaceMethod : WasiThreadsContext.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : JniWasiThreadsContext.class.getMethods()) {
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
  @DisplayName("Thread ID Specification Tests")
  class ThreadIdSpecificationTests {

    @Test
    @DisplayName("MAX_THREAD_ID should match WASI-Threads specification")
    void maxThreadIdShouldMatchSpec() throws Exception {
      Field field = JniWasiThreadsContext.class.getDeclaredField("MAX_THREAD_ID");
      field.setAccessible(true);
      int maxThreadId = field.getInt(null);
      assertEquals(0x1FFFFFFF, maxThreadId, "MAX_THREAD_ID should be 0x1FFFFFFF per spec");
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
          JniWasiThreadsContext.class.getPackage().getName(),
          "Should be in jni.wasi.threads package");
    }
  }
}
