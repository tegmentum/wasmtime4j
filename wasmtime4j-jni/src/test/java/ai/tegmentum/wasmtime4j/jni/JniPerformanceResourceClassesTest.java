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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.nativelib.NativeMethodBindings;
import ai.tegmentum.wasmtime4j.jni.pool.JniPoolingAllocator;
import ai.tegmentum.wasmtime4j.pool.PoolingAllocator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for JNI Performance and Resource classes.
 *
 * <p>This test class verifies the structure and interface compliance of JNI performance and
 * resource management classes without loading native libraries.
 *
 * <p>Classes tested:
 *
 * <ul>
 *   <li>{@link JniPoolingAllocator}
 *   <li>{@link NativeMethodBindings}
 * </ul>
 */
@DisplayName("JNI Performance and Resource Classes Tests")
class JniPerformanceResourceClassesTest {

  // ===================================================================================
  // JniPoolingAllocator Tests
  // ===================================================================================

  @Nested
  @DisplayName("JniPoolingAllocator Tests")
  class JniPoolingAllocatorTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("JniPoolingAllocator should be final class")
      void shouldBeFinalClass() {
        assertTrue(
            Modifier.isFinal(JniPoolingAllocator.class.getModifiers()),
            "JniPoolingAllocator should be final");
      }

      @Test
      @DisplayName("JniPoolingAllocator should be public")
      void shouldBePublic() {
        assertTrue(
            Modifier.isPublic(JniPoolingAllocator.class.getModifiers()),
            "JniPoolingAllocator should be public");
      }

      @Test
      @DisplayName("JniPoolingAllocator should implement PoolingAllocator")
      void shouldImplementPoolingAllocator() {
        assertTrue(
            PoolingAllocator.class.isAssignableFrom(JniPoolingAllocator.class),
            "JniPoolingAllocator should implement PoolingAllocator");
      }
    }

    @Nested
    @DisplayName("Field Tests")
    class FieldTests {

      @Test
      @DisplayName("Should have LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        Field field = JniPoolingAllocator.class.getDeclaredField("LOGGER");
        assertNotNull(field, "LOGGER field should exist");
        assertEquals(Logger.class, field.getType(), "LOGGER should be Logger type");
        assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
        assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      }

      @Test
      @DisplayName("Should have config field")
      void shouldHaveConfigField() throws NoSuchFieldException {
        Field field = JniPoolingAllocator.class.getDeclaredField("config");
        assertNotNull(field, "config field should exist");
        assertTrue(Modifier.isFinal(field.getModifiers()), "config should be final");
      }

      @Test
      @DisplayName("Should have nativeHandle field")
      void shouldHaveNativeHandleField() throws NoSuchFieldException {
        Field field = JniPoolingAllocator.class.getDeclaredField("nativeHandle");
        assertNotNull(field, "nativeHandle field should exist");
        assertEquals(long.class, field.getType(), "nativeHandle should be long type");
        assertTrue(Modifier.isFinal(field.getModifiers()), "nativeHandle should be final");
      }

      @Test
      @DisplayName("Should have closed field")
      void shouldHaveClosedField() throws NoSuchFieldException {
        Field field = JniPoolingAllocator.class.getDeclaredField("closed");
        assertNotNull(field, "closed field should exist");
        assertEquals(boolean.class, field.getType(), "closed should be boolean type");
        assertTrue(Modifier.isVolatile(field.getModifiers()), "closed should be volatile");
      }
    }

    @Nested
    @DisplayName("Method Tests")
    class MethodTests {

      @Test
      @DisplayName("Should have getConfig method")
      void shouldHaveGetConfigMethod() throws NoSuchMethodException {
        Method method = JniPoolingAllocator.class.getMethod("getConfig");
        assertNotNull(method, "getConfig method should exist");
        assertTrue(Modifier.isPublic(method.getModifiers()), "getConfig should be public");
      }

      @Test
      @DisplayName("Should have allocateInstance method")
      void shouldHaveAllocateInstanceMethod() throws NoSuchMethodException {
        Method method = JniPoolingAllocator.class.getMethod("allocateInstance");
        assertNotNull(method, "allocateInstance method should exist");
        assertEquals(long.class, method.getReturnType(), "allocateInstance should return long");
      }

      @Test
      @DisplayName("Should have reuseInstance method")
      void shouldHaveReuseInstanceMethod() throws NoSuchMethodException {
        Method method = JniPoolingAllocator.class.getMethod("reuseInstance", long.class);
        assertNotNull(method, "reuseInstance method should exist");
        assertEquals(void.class, method.getReturnType(), "reuseInstance should return void");
      }

      @Test
      @DisplayName("Should have releaseInstance method")
      void shouldHaveReleaseInstanceMethod() throws NoSuchMethodException {
        Method method = JniPoolingAllocator.class.getMethod("releaseInstance", long.class);
        assertNotNull(method, "releaseInstance method should exist");
        assertEquals(void.class, method.getReturnType(), "releaseInstance should return void");
      }

      @Test
      @DisplayName("Should have getStatistics method")
      void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
        Method method = JniPoolingAllocator.class.getMethod("getStatistics");
        assertNotNull(method, "getStatistics method should exist");
        assertTrue(Modifier.isPublic(method.getModifiers()), "getStatistics should be public");
      }

      @Test
      @DisplayName("Should have warmPools method")
      void shouldHaveWarmPoolsMethod() throws NoSuchMethodException {
        Method method = JniPoolingAllocator.class.getMethod("warmPools");
        assertNotNull(method, "warmPools method should exist");
        assertEquals(void.class, method.getReturnType(), "warmPools should return void");
      }

      @Test
      @DisplayName("Should have performMaintenance method")
      void shouldHavePerformMaintenanceMethod() throws NoSuchMethodException {
        Method method = JniPoolingAllocator.class.getMethod("performMaintenance");
        assertNotNull(method, "performMaintenance method should exist");
        assertEquals(void.class, method.getReturnType(), "performMaintenance should return void");
      }

      @Test
      @DisplayName("Should have isValid method")
      void shouldHaveIsValidMethod() throws NoSuchMethodException {
        Method method = JniPoolingAllocator.class.getMethod("isValid");
        assertNotNull(method, "isValid method should exist");
        assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
      }

      @Test
      @DisplayName("Should have close method")
      void shouldHaveCloseMethod() throws NoSuchMethodException {
        Method method = JniPoolingAllocator.class.getMethod("close");
        assertNotNull(method, "close method should exist");
        assertEquals(void.class, method.getReturnType(), "close should return void");
      }

      @Test
      @DisplayName("Should have getUptime method")
      void shouldHaveGetUptimeMethod() throws NoSuchMethodException {
        Method method = JniPoolingAllocator.class.getMethod("getUptime");
        assertNotNull(method, "getUptime method should exist");
        assertTrue(Modifier.isPublic(method.getModifiers()), "getUptime should be public");
      }

      @Test
      @DisplayName("Should have getNativeHandle method")
      void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
        Method method = JniPoolingAllocator.class.getMethod("getNativeHandle");
        assertNotNull(method, "getNativeHandle method should exist");
        assertEquals(long.class, method.getReturnType(), "getNativeHandle should return long");
      }
    }

    @Nested
    @DisplayName("Native Method Tests")
    class NativeMethodTests {

      @Test
      @DisplayName("Should have native methods for pooling operations")
      void shouldHaveNativePoolingOperations() {
        Set<String> nativeMethods = new HashSet<>();
        for (Method method : JniPoolingAllocator.class.getDeclaredMethods()) {
          if (Modifier.isNative(method.getModifiers())) {
            nativeMethods.add(method.getName());
          }
        }

        assertTrue(
            nativeMethods.contains("nativeCreateWithConfig"), "Should have nativeCreateWithConfig");
        assertTrue(
            nativeMethods.contains("nativeAllocateInstance"), "Should have nativeAllocateInstance");
        assertTrue(
            nativeMethods.contains("nativeReuseInstance"), "Should have nativeReuseInstance");
        assertTrue(
            nativeMethods.contains("nativeReleaseInstance"), "Should have nativeReleaseInstance");
        assertTrue(
            nativeMethods.contains("nativeGetStatistics"), "Should have nativeGetStatistics");
        assertTrue(nativeMethods.contains("nativeDestroy"), "Should have nativeDestroy");
      }
    }

    @Nested
    @DisplayName("Package Location Tests")
    class PackageLocationTests {

      @Test
      @DisplayName("Class should be in correct package")
      void shouldBeInCorrectPackage() {
        assertEquals(
            "ai.tegmentum.wasmtime4j.jni.pool",
            JniPoolingAllocator.class.getPackage().getName(),
            "Should be in ai.tegmentum.wasmtime4j.jni.pool package");
      }
    }
  }

  // ===================================================================================
  // NativeMethodBindings Tests
  // ===================================================================================

  @Nested
  @DisplayName("NativeMethodBindings Tests")
  class NativeMethodBindingsTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("NativeMethodBindings should be final class")
      void shouldBeFinalClass() {
        assertTrue(
            Modifier.isFinal(NativeMethodBindings.class.getModifiers()),
            "NativeMethodBindings should be final");
      }

      @Test
      @DisplayName("NativeMethodBindings should be public")
      void shouldBePublic() {
        assertTrue(
            Modifier.isPublic(NativeMethodBindings.class.getModifiers()),
            "NativeMethodBindings should be public");
      }

      @Test
      @DisplayName("NativeMethodBindings should have private constructor (utility class)")
      void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<?> constructor = NativeMethodBindings.class.getDeclaredConstructor();
        assertNotNull(constructor, "Constructor should exist");
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
      }
    }

    @Nested
    @DisplayName("Field Tests")
    class FieldTests {

      @Test
      @DisplayName("Should have LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        Field field = NativeMethodBindings.class.getDeclaredField("LOGGER");
        assertNotNull(field, "LOGGER field should exist");
        assertEquals(Logger.class, field.getType(), "LOGGER should be Logger type");
      }

      @Test
      @DisplayName("Should have INITIALIZED field")
      void shouldHaveInitializedField() throws NoSuchFieldException {
        Field field = NativeMethodBindings.class.getDeclaredField("INITIALIZED");
        assertNotNull(field, "INITIALIZED field should exist");
        assertEquals(AtomicBoolean.class, field.getType(), "INITIALIZED should be AtomicBoolean");
        assertTrue(Modifier.isStatic(field.getModifiers()), "INITIALIZED should be static");
        assertTrue(Modifier.isFinal(field.getModifiers()), "INITIALIZED should be final");
      }
    }

    @Nested
    @DisplayName("Method Tests")
    class MethodTests {

      @Test
      @DisplayName("Should have initialize static method")
      void shouldHaveInitializeMethod() throws NoSuchMethodException {
        Method method = NativeMethodBindings.class.getMethod("initialize");
        assertNotNull(method, "initialize method should exist");
        assertTrue(Modifier.isStatic(method.getModifiers()), "initialize should be static");
        assertEquals(void.class, method.getReturnType(), "initialize should return void");
      }

      @Test
      @DisplayName("Should have isInitialized static method")
      void shouldHaveIsInitializedMethod() throws NoSuchMethodException {
        Method method = NativeMethodBindings.class.getMethod("isInitialized");
        assertNotNull(method, "isInitialized method should exist");
        assertTrue(Modifier.isStatic(method.getModifiers()), "isInitialized should be static");
        assertEquals(boolean.class, method.getReturnType(), "isInitialized should return boolean");
      }

      @Test
      @DisplayName("Should have ensureInitialized static method")
      void shouldHaveEnsureInitializedMethod() throws NoSuchMethodException {
        Method method = NativeMethodBindings.class.getMethod("ensureInitialized");
        assertNotNull(method, "ensureInitialized method should exist");
        assertTrue(Modifier.isStatic(method.getModifiers()), "ensureInitialized should be static");
        assertEquals(void.class, method.getReturnType(), "ensureInitialized should return void");
      }

      @Test
      @DisplayName("Should have getNativeLibraryVersion static method")
      void shouldHaveGetNativeLibraryVersionMethod() throws NoSuchMethodException {
        Method method = NativeMethodBindings.class.getMethod("getNativeLibraryVersion");
        assertNotNull(method, "getNativeLibraryVersion method should exist");
        assertTrue(
            Modifier.isStatic(method.getModifiers()), "getNativeLibraryVersion should be static");
        assertEquals(
            String.class, method.getReturnType(), "getNativeLibraryVersion should return String");
      }

      @Test
      @DisplayName("Should have getLibraryInfo static method")
      void shouldHaveGetLibraryInfoMethod() throws NoSuchMethodException {
        Method method = NativeMethodBindings.class.getMethod("getLibraryInfo");
        assertNotNull(method, "getLibraryInfo method should exist");
        assertTrue(Modifier.isStatic(method.getModifiers()), "getLibraryInfo should be static");
        assertEquals(String.class, method.getReturnType(), "getLibraryInfo should return String");
      }
    }

    @Nested
    @DisplayName("Native Method Tests")
    class NativeMethodTests {

      @Test
      @DisplayName("Should have native methods for library operations")
      void shouldHaveNativeLibraryOperations() {
        Set<String> nativeMethods = new HashSet<>();
        for (Method method : NativeMethodBindings.class.getDeclaredMethods()) {
          if (Modifier.isNative(method.getModifiers())) {
            nativeMethods.add(method.getName());
          }
        }

        assertTrue(
            nativeMethods.contains("nativeGetWasmtimeVersion"),
            "Should have nativeGetWasmtimeVersion");
        assertTrue(
            nativeMethods.contains("nativeCreateRuntime"), "Should have nativeCreateRuntime");
        assertTrue(
            nativeMethods.contains("nativeDestroyRuntime"), "Should have nativeDestroyRuntime");
      }
    }

    @Nested
    @DisplayName("Package Location Tests")
    class PackageLocationTests {

      @Test
      @DisplayName("Class should be in correct package")
      void shouldBeInCorrectPackage() {
        assertEquals(
            "ai.tegmentum.wasmtime4j.jni.nativelib",
            NativeMethodBindings.class.getPackage().getName(),
            "Should be in ai.tegmentum.wasmtime4j.jni.nativelib package");
      }
    }
  }

  // ===================================================================================
  // Cross-Class Consistency Tests
  // ===================================================================================

  @Nested
  @DisplayName("Cross-Class Consistency Tests")
  class CrossClassConsistencyTests {

    @Test
    @DisplayName("All performance classes should be final")
    void allPerformanceClassesShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(JniPoolingAllocator.class.getModifiers()),
          "JniPoolingAllocator should be final");
      assertTrue(
          Modifier.isFinal(NativeMethodBindings.class.getModifiers()),
          "NativeMethodBindings should be final");
    }

    @Test
    @DisplayName("All performance classes should be public")
    void allPerformanceClassesShouldBePublic() {
      assertTrue(
          Modifier.isPublic(JniPoolingAllocator.class.getModifiers()),
          "JniPoolingAllocator should be public");
      assertTrue(
          Modifier.isPublic(NativeMethodBindings.class.getModifiers()),
          "NativeMethodBindings should be public");
    }

    @Test
    @DisplayName("All classes should have LOGGER field")
    void allClassesShouldHaveLoggerField() throws NoSuchFieldException {
      Field[] loggerFields = {
        JniPoolingAllocator.class.getDeclaredField("LOGGER"),
        NativeMethodBindings.class.getDeclaredField("LOGGER")
      };

      for (Field field : loggerFields) {
        assertNotNull(field, "LOGGER field should exist");
        assertEquals(Logger.class, field.getType(), "LOGGER should be Logger type");
        assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
        assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      }
    }

    @Test
    @DisplayName("Closeable classes should have close method")
    void closeableClassesShouldHaveCloseMethod() throws NoSuchMethodException {
      Method m1 = JniPoolingAllocator.class.getMethod("close");

      assertNotNull(m1, "JniPoolingAllocator should have close method");

      assertEquals(void.class, m1.getReturnType());
    }

    @Test
    @DisplayName("Classes with native handles should have nativeHandle field")
    void classesWithNativeHandlesShouldHaveField() throws NoSuchFieldException {
      Field f1 = JniPoolingAllocator.class.getDeclaredField("nativeHandle");

      assertNotNull(f1, "JniPoolingAllocator should have nativeHandle");

      assertEquals(long.class, f1.getType());
    }
  }
}
