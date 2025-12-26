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

package ai.tegmentum.wasmtime4j.panama.wasi.threads;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContext;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContextBuilder;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsProvider;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for Panama WASI-Threads implementation classes.
 *
 * <p>Tests verify class structure, method signatures, field definitions, and API contracts using
 * reflection to avoid triggering native library loading.
 */
@DisplayName("Panama WASI-Threads Implementation Tests")
class PanamaWasiThreadsTest {

  @Nested
  @DisplayName("PanamaWasiThreadsProvider Tests")
  class PanamaWasiThreadsProviderTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("PanamaWasiThreadsProvider should be final class")
      void shouldBeFinalClass() {
        assertTrue(
            Modifier.isFinal(PanamaWasiThreadsProvider.class.getModifiers()),
            "PanamaWasiThreadsProvider should be final");
      }

      @Test
      @DisplayName("PanamaWasiThreadsProvider should implement WasiThreadsProvider")
      void shouldImplementWasiThreadsProvider() {
        assertTrue(
            WasiThreadsProvider.class.isAssignableFrom(PanamaWasiThreadsProvider.class),
            "PanamaWasiThreadsProvider should implement WasiThreadsProvider");
      }

      @Test
      @DisplayName("PanamaWasiThreadsProvider should be public")
      void shouldBePublic() {
        assertTrue(
            Modifier.isPublic(PanamaWasiThreadsProvider.class.getModifiers()),
            "PanamaWasiThreadsProvider should be public");
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("Should have public no-arg constructor for ServiceLoader")
      void shouldHavePublicNoArgConstructor() {
        assertDoesNotThrow(
            () -> PanamaWasiThreadsProvider.class.getConstructor(),
            "Should have public no-arg constructor");
      }

      @Test
      @DisplayName("Should be instantiable via no-arg constructor")
      void shouldBeInstantiable() {
        PanamaWasiThreadsProvider provider = new PanamaWasiThreadsProvider();
        assertNotNull(provider, "Should be instantiable");
      }
    }

    @Nested
    @DisplayName("Method Signature Tests")
    class MethodSignatureTests {

      @Test
      @DisplayName("isAvailable method should exist and return boolean")
      void isAvailableMethodShouldExist() throws NoSuchMethodException {
        Method method = PanamaWasiThreadsProvider.class.getMethod("isAvailable");
        assertNotNull(method, "isAvailable method should exist");
        assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      }

      @Test
      @DisplayName("createBuilder method should exist and return WasiThreadsContextBuilder")
      void createBuilderMethodShouldExist() throws NoSuchMethodException {
        Method method = PanamaWasiThreadsProvider.class.getMethod("createBuilder");
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
            PanamaWasiThreadsProvider.class.getMethod(
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
        Field field = PanamaWasiThreadsProvider.class.getDeclaredField("LOGGER");
        assertNotNull(field, "LOGGER field should exist");
        assertEquals(Logger.class, field.getType(), "Should be Logger type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
        assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
        assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      }

      @Test
      @DisplayName("Should have available cache field")
      void shouldHaveAvailableCacheField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsProvider.class.getDeclaredField("available");
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
        Method method = PanamaWasiThreadsProvider.class.getDeclaredMethod("checkAvailability");
        assertNotNull(method, "checkAvailability method should exist");
        assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
        assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
        assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      }
    }

    @Nested
    @DisplayName("Double-Checked Locking Tests")
    class DoubleCheckedLockingTests {

      @Test
      @DisplayName("isAvailable should use cached result on subsequent calls")
      void isAvailableShouldUseCachedResult() {
        PanamaWasiThreadsProvider provider = new PanamaWasiThreadsProvider();

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
    @DisplayName("Interface Compliance Tests")
    class InterfaceComplianceTests {

      @Test
      @DisplayName("Should implement all WasiThreadsProvider interface methods")
      void shouldImplementAllInterfaceMethods() {
        for (Method interfaceMethod : WasiThreadsProvider.class.getMethods()) {
          if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
            boolean found = false;
            for (Method implMethod : PanamaWasiThreadsProvider.class.getMethods()) {
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
    @DisplayName("Package Location Tests")
    class PackageLocationTests {

      @Test
      @DisplayName("Class should be in correct package")
      void shouldBeInCorrectPackage() {
        assertEquals(
            "ai.tegmentum.wasmtime4j.panama.wasi.threads",
            PanamaWasiThreadsProvider.class.getPackage().getName(),
            "Should be in panama.wasi.threads package");
      }
    }
  }

  @Nested
  @DisplayName("PanamaWasiThreadsContextBuilder Tests")
  class PanamaWasiThreadsContextBuilderTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("PanamaWasiThreadsContextBuilder should be final class")
      void shouldBeFinalClass() {
        assertTrue(
            Modifier.isFinal(PanamaWasiThreadsContextBuilder.class.getModifiers()),
            "PanamaWasiThreadsContextBuilder should be final");
      }

      @Test
      @DisplayName("PanamaWasiThreadsContextBuilder should implement WasiThreadsContextBuilder")
      void shouldImplementWasiThreadsContextBuilder() {
        assertTrue(
            WasiThreadsContextBuilder.class.isAssignableFrom(PanamaWasiThreadsContextBuilder.class),
            "PanamaWasiThreadsContextBuilder should implement WasiThreadsContextBuilder");
      }

      @Test
      @DisplayName("PanamaWasiThreadsContextBuilder should be public")
      void shouldBePublic() {
        assertTrue(
            Modifier.isPublic(PanamaWasiThreadsContextBuilder.class.getModifiers()),
            "PanamaWasiThreadsContextBuilder should be public");
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("Should have public no-arg constructor")
      void shouldHavePublicNoArgConstructor() {
        assertDoesNotThrow(
            () -> PanamaWasiThreadsContextBuilder.class.getConstructor(),
            "Should have public no-arg constructor");
      }

      @Test
      @DisplayName("Should be instantiable via no-arg constructor")
      void shouldBeInstantiable() {
        PanamaWasiThreadsContextBuilder builder = new PanamaWasiThreadsContextBuilder();
        assertNotNull(builder, "Should be instantiable");
      }
    }

    @Nested
    @DisplayName("Method Signature Tests")
    class MethodSignatureTests {

      @Test
      @DisplayName("withModule method should exist and return WasiThreadsContextBuilder")
      void withModuleMethodShouldExist() throws NoSuchMethodException {
        Method method = PanamaWasiThreadsContextBuilder.class.getMethod("withModule", Module.class);
        assertNotNull(method, "withModule method should exist");
        assertEquals(
            WasiThreadsContextBuilder.class,
            method.getReturnType(),
            "Should return WasiThreadsContextBuilder for fluent API");
      }

      @Test
      @DisplayName("withLinker method should exist and return WasiThreadsContextBuilder")
      void withLinkerMethodShouldExist() throws NoSuchMethodException {
        Method method = PanamaWasiThreadsContextBuilder.class.getMethod("withLinker", Linker.class);
        assertNotNull(method, "withLinker method should exist");
        assertEquals(
            WasiThreadsContextBuilder.class,
            method.getReturnType(),
            "Should return WasiThreadsContextBuilder for fluent API");
      }

      @Test
      @DisplayName("withStore method should exist and return WasiThreadsContextBuilder")
      void withStoreMethodShouldExist() throws NoSuchMethodException {
        Method method = PanamaWasiThreadsContextBuilder.class.getMethod("withStore", Store.class);
        assertNotNull(method, "withStore method should exist");
        assertEquals(
            WasiThreadsContextBuilder.class,
            method.getReturnType(),
            "Should return WasiThreadsContextBuilder for fluent API");
      }

      @Test
      @DisplayName("build method should exist and return WasiThreadsContext")
      void buildMethodShouldExist() throws NoSuchMethodException {
        Method method = PanamaWasiThreadsContextBuilder.class.getMethod("build");
        assertNotNull(method, "build method should exist");
        assertEquals(
            WasiThreadsContext.class, method.getReturnType(), "Should return WasiThreadsContext");
      }
    }

    @Nested
    @DisplayName("Field Tests")
    class FieldTests {

      @Test
      @DisplayName("Should have LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContextBuilder.class.getDeclaredField("LOGGER");
        assertNotNull(field, "LOGGER field should exist");
        assertEquals(Logger.class, field.getType(), "Should be Logger type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
        assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
        assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      }

      @Test
      @DisplayName("Should have NATIVE_BINDINGS field")
      void shouldHaveNativeBindingsField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContextBuilder.class.getDeclaredField("NATIVE_BINDINGS");
        assertNotNull(field, "NATIVE_BINDINGS field should exist");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
        assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
        assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      }

      @Test
      @DisplayName("Should have module field")
      void shouldHaveModuleField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContextBuilder.class.getDeclaredField("module");
        assertNotNull(field, "module field should exist");
        assertEquals(Module.class, field.getType(), "Should be Module type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      }

      @Test
      @DisplayName("Should have linker field")
      void shouldHaveLinkerField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContextBuilder.class.getDeclaredField("linker");
        assertNotNull(field, "linker field should exist");
        assertEquals(Linker.class, field.getType(), "Should be Linker type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      }

      @Test
      @DisplayName("Should have store field")
      void shouldHaveStoreField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContextBuilder.class.getDeclaredField("store");
        assertNotNull(field, "store field should exist");
        assertEquals(Store.class, field.getType(), "Should be Store type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      }
    }

    @Nested
    @DisplayName("Private Method Tests")
    class PrivateMethodTests {

      @Test
      @DisplayName("Should have validateConfiguration private method")
      void shouldHaveValidateConfigurationMethod() throws NoSuchMethodException {
        Method method =
            PanamaWasiThreadsContextBuilder.class.getDeclaredMethod("validateConfiguration");
        assertNotNull(method, "validateConfiguration method should exist");
        assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
        assertEquals(void.class, method.getReturnType(), "Should return void");
      }

      @Test
      @DisplayName("Should have getNativeSegment private method")
      void shouldHaveGetNativeSegmentMethod() throws NoSuchMethodException {
        Method method =
            PanamaWasiThreadsContextBuilder.class.getDeclaredMethod(
                "getNativeSegment", Object.class, String.class);
        assertNotNull(method, "getNativeSegment method should exist");
        assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
        assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
      }
    }

    @Nested
    @DisplayName("Interface Compliance Tests")
    class InterfaceComplianceTests {

      @Test
      @DisplayName("Should implement all WasiThreadsContextBuilder interface methods")
      void shouldImplementAllInterfaceMethods() {
        for (Method interfaceMethod : WasiThreadsContextBuilder.class.getMethods()) {
          if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
            boolean found = false;
            for (Method implMethod : PanamaWasiThreadsContextBuilder.class.getMethods()) {
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
    @DisplayName("Package Location Tests")
    class PackageLocationTests {

      @Test
      @DisplayName("Class should be in correct package")
      void shouldBeInCorrectPackage() {
        assertEquals(
            "ai.tegmentum.wasmtime4j.panama.wasi.threads",
            PanamaWasiThreadsContextBuilder.class.getPackage().getName(),
            "Should be in panama.wasi.threads package");
      }
    }
  }

  @Nested
  @DisplayName("PanamaWasiThreadsContext Tests")
  class PanamaWasiThreadsContextTests {

    @Nested
    @DisplayName("Class Structure Tests")
    class ClassStructureTests {

      @Test
      @DisplayName("PanamaWasiThreadsContext should be final class")
      void shouldBeFinalClass() {
        assertTrue(
            Modifier.isFinal(PanamaWasiThreadsContext.class.getModifiers()),
            "PanamaWasiThreadsContext should be final");
      }

      @Test
      @DisplayName("PanamaWasiThreadsContext should implement WasiThreadsContext")
      void shouldImplementWasiThreadsContext() {
        assertTrue(
            WasiThreadsContext.class.isAssignableFrom(PanamaWasiThreadsContext.class),
            "PanamaWasiThreadsContext should implement WasiThreadsContext");
      }

      @Test
      @DisplayName("PanamaWasiThreadsContext should be public")
      void shouldBePublic() {
        assertTrue(
            Modifier.isPublic(PanamaWasiThreadsContext.class.getModifiers()),
            "PanamaWasiThreadsContext should be public");
      }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

      @Test
      @DisplayName("Should have package-private constructor with MemorySegment, Arena, and boolean")
      void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
        var constructor =
            PanamaWasiThreadsContext.class.getDeclaredConstructor(
                MemorySegment.class, Arena.class, boolean.class);
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
        Method method = PanamaWasiThreadsContext.class.getMethod("spawn", int.class);
        assertNotNull(method, "spawn method should exist");
        assertEquals(int.class, method.getReturnType(), "Should return int");
      }

      @Test
      @DisplayName("getThreadCount method should exist and return int")
      void getThreadCountMethodShouldExist() throws NoSuchMethodException {
        Method method = PanamaWasiThreadsContext.class.getMethod("getThreadCount");
        assertNotNull(method, "getThreadCount method should exist");
        assertEquals(int.class, method.getReturnType(), "Should return int");
      }

      @Test
      @DisplayName("isEnabled method should exist and return boolean")
      void isEnabledMethodShouldExist() throws NoSuchMethodException {
        Method method = PanamaWasiThreadsContext.class.getMethod("isEnabled");
        assertNotNull(method, "isEnabled method should exist");
        assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      }

      @Test
      @DisplayName("getMaxThreadId method should exist and return int")
      void getMaxThreadIdMethodShouldExist() throws NoSuchMethodException {
        Method method = PanamaWasiThreadsContext.class.getMethod("getMaxThreadId");
        assertNotNull(method, "getMaxThreadId method should exist");
        assertEquals(int.class, method.getReturnType(), "Should return int");
      }

      @Test
      @DisplayName("isValid method should exist and return boolean")
      void isValidMethodShouldExist() throws NoSuchMethodException {
        Method method = PanamaWasiThreadsContext.class.getMethod("isValid");
        assertNotNull(method, "isValid method should exist");
        assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      }

      @Test
      @DisplayName("getNativeContext method should exist and return MemorySegment")
      void getNativeContextMethodShouldExist() throws NoSuchMethodException {
        Method method = PanamaWasiThreadsContext.class.getMethod("getNativeContext");
        assertNotNull(method, "getNativeContext method should exist");
        assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
        assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      }

      @Test
      @DisplayName("onThreadCompleted method should exist with int parameter")
      void onThreadCompletedMethodShouldExist() throws NoSuchMethodException {
        Method method =
            PanamaWasiThreadsContext.class.getDeclaredMethod("onThreadCompleted", int.class);
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

      @Test
      @DisplayName("close method should exist")
      void closeMethodShouldExist() throws NoSuchMethodException {
        Method method = PanamaWasiThreadsContext.class.getMethod("close");
        assertNotNull(method, "close method should exist");
        assertEquals(void.class, method.getReturnType(), "Should return void");
      }
    }

    @Nested
    @DisplayName("Field Tests")
    class FieldTests {

      @Test
      @DisplayName("Should have LOGGER field")
      void shouldHaveLoggerField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContext.class.getDeclaredField("LOGGER");
        assertNotNull(field, "LOGGER field should exist");
        assertEquals(Logger.class, field.getType(), "Should be Logger type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
        assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
        assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      }

      @Test
      @DisplayName("Should have NATIVE_BINDINGS field")
      void shouldHaveNativeBindingsField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContext.class.getDeclaredField("NATIVE_BINDINGS");
        assertNotNull(field, "NATIVE_BINDINGS field should exist");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
        assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
        assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      }

      @Test
      @DisplayName("Should have MAX_THREAD_ID constant")
      void shouldHaveMaxThreadIdConstant() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContext.class.getDeclaredField("MAX_THREAD_ID");
        assertNotNull(field, "MAX_THREAD_ID field should exist");
        assertEquals(int.class, field.getType(), "Should be int type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
        assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
        assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      }

      @Test
      @DisplayName("Should have nativeContext MemorySegment field")
      void shouldHaveNativeContextField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContext.class.getDeclaredField("nativeContext");
        assertNotNull(field, "nativeContext field should exist");
        assertEquals(MemorySegment.class, field.getType(), "Should be MemorySegment type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
        assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      }

      @Test
      @DisplayName("Should have arena Arena field")
      void shouldHaveArenaField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContext.class.getDeclaredField("arena");
        assertNotNull(field, "arena field should exist");
        assertEquals(Arena.class, field.getType(), "Should be Arena type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
        assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      }

      @Test
      @DisplayName("Should have maxThreadId AtomicInteger field")
      void shouldHaveMaxThreadIdField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContext.class.getDeclaredField("maxThreadId");
        assertNotNull(field, "maxThreadId field should exist");
        assertEquals(AtomicInteger.class, field.getType(), "Should be AtomicInteger type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
        assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      }

      @Test
      @DisplayName("Should have threadCount AtomicInteger field")
      void shouldHaveThreadCountField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContext.class.getDeclaredField("threadCount");
        assertNotNull(field, "threadCount field should exist");
        assertEquals(AtomicInteger.class, field.getType(), "Should be AtomicInteger type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
        assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      }

      @Test
      @DisplayName("Should have enabled boolean field")
      void shouldHaveEnabledField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContext.class.getDeclaredField("enabled");
        assertNotNull(field, "enabled field should exist");
        assertEquals(boolean.class, field.getType(), "Should be boolean type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
        assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      }

      @Test
      @DisplayName("Should have closed AtomicBoolean field")
      void shouldHaveClosedField() throws NoSuchFieldException {
        Field field = PanamaWasiThreadsContext.class.getDeclaredField("closed");
        assertNotNull(field, "closed field should exist");
        assertEquals(AtomicBoolean.class, field.getType(), "Should be AtomicBoolean type");
        assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
        assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      }
    }

    @Nested
    @DisplayName("Private Method Tests")
    class PrivateMethodTests {

      @Test
      @DisplayName("Should have ensureNotClosed private method")
      void shouldHaveEnsureNotClosedMethod() throws NoSuchMethodException {
        Method method = PanamaWasiThreadsContext.class.getDeclaredMethod("ensureNotClosed");
        assertNotNull(method, "ensureNotClosed method should exist");
        assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
        assertEquals(void.class, method.getReturnType(), "Should return void");
      }
    }

    @Nested
    @DisplayName("Thread ID Specification Tests")
    class ThreadIdSpecificationTests {

      @Test
      @DisplayName("MAX_THREAD_ID should match WASI-Threads specification")
      void maxThreadIdShouldMatchSpec() throws Exception {
        Field field = PanamaWasiThreadsContext.class.getDeclaredField("MAX_THREAD_ID");
        field.setAccessible(true);
        int maxThreadId = field.getInt(null);
        assertEquals(0x1FFFFFFF, maxThreadId, "MAX_THREAD_ID should be 0x1FFFFFFF per spec");
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
            for (Method implMethod : PanamaWasiThreadsContext.class.getMethods()) {
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
    @DisplayName("AutoCloseable Pattern Tests")
    class AutoCloseablePatternTests {

      @Test
      @DisplayName("PanamaWasiThreadsContext should implement AutoCloseable")
      void shouldImplementAutoCloseable() {
        assertTrue(
            AutoCloseable.class.isAssignableFrom(PanamaWasiThreadsContext.class),
            "PanamaWasiThreadsContext should implement AutoCloseable");
      }
    }

    @Nested
    @DisplayName("Package Location Tests")
    class PackageLocationTests {

      @Test
      @DisplayName("Class should be in correct package")
      void shouldBeInCorrectPackage() {
        assertEquals(
            "ai.tegmentum.wasmtime4j.panama.wasi.threads",
            PanamaWasiThreadsContext.class.getPackage().getName(),
            "Should be in panama.wasi.threads package");
      }
    }
  }

  @Nested
  @DisplayName("Panama FFI Pattern Tests")
  class PanamaFfiPatternTests {

    @Test
    @DisplayName("Context should use MemorySegment for native handle")
    void contextShouldUseMemorySegment() throws NoSuchFieldException {
      Field field = PanamaWasiThreadsContext.class.getDeclaredField("nativeContext");
      assertEquals(
          MemorySegment.class,
          field.getType(),
          "Should use MemorySegment for native handle (Panama pattern)");
    }

    @Test
    @DisplayName("Context should use Arena for memory management")
    void contextShouldUseArena() throws NoSuchFieldException {
      Field field = PanamaWasiThreadsContext.class.getDeclaredField("arena");
      assertEquals(
          Arena.class, field.getType(), "Should use Arena for memory management (Panama pattern)");
    }

    @Test
    @DisplayName("Builder should return MemorySegment from getNativeSegment")
    void builderShouldReturnMemorySegment() throws NoSuchMethodException {
      Method method =
          PanamaWasiThreadsContextBuilder.class.getDeclaredMethod(
              "getNativeSegment", Object.class, String.class);
      assertEquals(
          MemorySegment.class,
          method.getReturnType(),
          "getNativeSegment should return MemorySegment");
    }
  }
}
