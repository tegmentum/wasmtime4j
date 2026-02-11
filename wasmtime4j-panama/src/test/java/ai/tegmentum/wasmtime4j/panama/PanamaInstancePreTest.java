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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.InstancePre;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.validation.PreInstantiationStatistics;
import ai.tegmentum.wasmtime4j.Store;
import java.io.Closeable;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaInstancePre} class.
 *
 * <p>This test class verifies the Panama implementation of InstancePre interface using
 * reflection-based testing to avoid triggering native library loading.
 */
@DisplayName("PanamaInstancePre Tests")
class PanamaInstancePreTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaInstancePre should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaInstancePre.class.getModifiers()),
          "PanamaInstancePre should be final");
    }

    @Test
    @DisplayName("PanamaInstancePre should implement InstancePre interface")
    void shouldImplementInstancePreInterface() {
      assertTrue(
          InstancePre.class.isAssignableFrom(PanamaInstancePre.class),
          "PanamaInstancePre should implement InstancePre");
    }

    @Test
    @DisplayName("PanamaInstancePre should implement Closeable interface")
    void shouldImplementCloseableInterface() {
      assertTrue(
          Closeable.class.isAssignableFrom(PanamaInstancePre.class),
          "PanamaInstancePre should implement Closeable (via InstancePre)");
    }

    @Test
    @DisplayName("PanamaInstancePre should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(PanamaInstancePre.class.getModifiers()),
          "PanamaInstancePre should be public");
    }
  }

  @Nested
  @DisplayName("Interface Method Implementation Tests")
  class InterfaceMethodTests {

    @Test
    @DisplayName("Should implement instantiate with Store method")
    void shouldImplementInstantiateWithStoreMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("instantiate", Store.class);
      assertNotNull(method, "instantiate(Store) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }

    @Test
    @DisplayName("Should implement instantiate with Store and ImportMap method")
    void shouldImplementInstantiateWithImportsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaInstancePre.class.getMethod("instantiate", Store.class, ImportMap.class);
      assertNotNull(method, "instantiate(Store, ImportMap) method should exist");
      assertEquals(Instance.class, method.getReturnType(), "Should return Instance");
    }

    @Test
    @DisplayName("Should implement getModule method")
    void shouldImplementGetModuleMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("getModule");
      assertNotNull(method, "getModule method should exist");
      assertEquals(Module.class, method.getReturnType(), "getModule should return Module");
    }

    @Test
    @DisplayName("Should implement getEngine method")
    void shouldImplementGetEngineMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("getEngine");
      assertNotNull(method, "getEngine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "getEngine should return Engine");
    }

    @Test
    @DisplayName("Should implement isValid method")
    void shouldImplementIsValidMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("Should implement getInstanceCount method")
    void shouldImplementGetInstanceCountMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("getInstanceCount");
      assertNotNull(method, "getInstanceCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getInstanceCount should return long");
    }

    @Test
    @DisplayName("Should implement getStatistics method")
    void shouldImplementGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          PreInstantiationStatistics.class,
          method.getReturnType(),
          "getStatistics should return PreInstantiationStatistics");
    }

    @Test
    @DisplayName("Should implement close method")
    void shouldImplementCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }

  @Nested
  @DisplayName("Async Method Tests")
  class AsyncMethodTests {

    @Test
    @DisplayName("Should implement instantiateAsync with Store method")
    void shouldImplementInstantiateAsyncMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("instantiateAsync", Store.class);
      assertNotNull(method, "instantiateAsync(Store) method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "instantiateAsync should return CompletableFuture");
    }

    @Test
    @DisplayName("Should implement instantiateAsync with Store and ImportMap method")
    void shouldImplementInstantiateAsyncWithImportsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaInstancePre.class.getMethod("instantiateAsync", Store.class, ImportMap.class);
      assertNotNull(method, "instantiateAsync(Store, ImportMap) method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "instantiateAsync should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("Additional Method Tests")
  class AdditionalMethodTests {

    @Test
    @DisplayName("Should have getNativeInstancePre method")
    void shouldHaveGetNativeInstancePreMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("getNativeInstancePre");
      assertNotNull(method, "getNativeInstancePre method should exist");
      assertEquals(
          MemorySegment.class,
          method.getReturnType(),
          "getNativeInstancePre should return MemorySegment");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getNativeInstancePre should be public");
    }

    @Test
    @DisplayName("Should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have constructor with MemorySegment, Module, and Engine")
    void shouldHaveConstructorWithRequiredParams() {
      boolean foundConstructor = false;
      for (final java.lang.reflect.Constructor<?> constructor :
          PanamaInstancePre.class.getConstructors()) {
        final Class<?>[] paramTypes = constructor.getParameterTypes();
        if (paramTypes.length == 3
            && MemorySegment.class.isAssignableFrom(paramTypes[0])
            && Module.class.isAssignableFrom(paramTypes[1])
            && Engine.class.isAssignableFrom(paramTypes[2])) {
          foundConstructor = true;
          break;
        }
      }
      assertTrue(foundConstructor, "Should have constructor(MemorySegment, Module, Engine)");
    }

    @Test
    @DisplayName("Should have exactly one public constructor")
    void shouldHaveOnePublicConstructor() {
      final int publicConstructorCount =
          (int)
              Arrays.stream(PanamaInstancePre.class.getConstructors())
                  .filter(c -> Modifier.isPublic(c.getModifiers()))
                  .count();
      assertEquals(1, publicConstructorCount, "Should have exactly 1 public constructor");
    }
  }

  @Nested
  @DisplayName("Method Signature Validation Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("instantiate should throw WasmException")
    void instantiateShouldThrowWasmException() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getMethod("instantiate", Store.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "instantiate method should declare exceptions");
      boolean foundWasmException = false;
      for (final Class<?> exceptionType : exceptionTypes) {
        if (ai.tegmentum.wasmtime4j.exception.WasmException.class.isAssignableFrom(exceptionType)) {
          foundWasmException = true;
          break;
        }
      }
      assertTrue(foundWasmException, "instantiate method should throw WasmException");
    }

    @Test
    @DisplayName("instantiate with imports should throw WasmException")
    void instantiateWithImportsShouldThrowWasmException() throws NoSuchMethodException {
      final Method method =
          PanamaInstancePre.class.getMethod("instantiate", Store.class, ImportMap.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "instantiate method should declare exceptions");
      boolean foundWasmException = false;
      for (final Class<?> exceptionType : exceptionTypes) {
        if (ai.tegmentum.wasmtime4j.exception.WasmException.class.isAssignableFrom(exceptionType)) {
          foundWasmException = true;
          break;
        }
      }
      assertTrue(foundWasmException, "instantiate method should throw WasmException");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have LOGGER static field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      final java.lang.reflect.Field field = PanamaInstancePre.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
    }

    @Test
    @DisplayName("Should have NATIVE_BINDINGS static field")
    void shouldHaveNativeBindingsField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaInstancePre.class.getDeclaredField("NATIVE_BINDINGS");
      assertNotNull(field, "NATIVE_BINDINGS field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "NATIVE_BINDINGS should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "NATIVE_BINDINGS should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "NATIVE_BINDINGS should be private");
    }

    @Test
    @DisplayName("Should have nativeInstancePre field")
    void shouldHaveNativeInstancePreField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaInstancePre.class.getDeclaredField("nativeInstancePre");
      assertNotNull(field, "nativeInstancePre field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "nativeInstancePre should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "nativeInstancePre should be private");
      assertEquals(
          MemorySegment.class, field.getType(), "nativeInstancePre should be MemorySegment type");
    }

    @Test
    @DisplayName("Should have module field")
    void shouldHaveModuleField() throws NoSuchFieldException {
      final java.lang.reflect.Field field = PanamaInstancePre.class.getDeclaredField("module");
      assertNotNull(field, "module field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "module should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "module should be private");
      assertEquals(Module.class, field.getType(), "module should be Module type");
    }

    @Test
    @DisplayName("Should have engine field")
    void shouldHaveEngineField() throws NoSuchFieldException {
      final java.lang.reflect.Field field = PanamaInstancePre.class.getDeclaredField("engine");
      assertNotNull(field, "engine field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "engine should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "engine should be private");
      assertEquals(Engine.class, field.getType(), "engine should be Engine type");
    }

    @Test
    @DisplayName("Should have creationTime field")
    void shouldHaveCreationTimeField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaInstancePre.class.getDeclaredField("creationTime");
      assertNotNull(field, "creationTime field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "creationTime should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "creationTime should be private");
      assertEquals(java.time.Instant.class, field.getType(), "creationTime should be Instant type");
    }

    @Test
    @DisplayName("Should have closed field using AtomicBoolean")
    void shouldHaveClosedField() throws NoSuchFieldException {
      final java.lang.reflect.Field field = PanamaInstancePre.class.getDeclaredField("closed");
      assertNotNull(field, "closed field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "closed should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "closed should be private");
      assertEquals(
          java.util.concurrent.atomic.AtomicBoolean.class,
          field.getType(),
          "closed should be AtomicBoolean type");
    }
  }

  @Nested
  @DisplayName("Interface Implementation Completeness Tests")
  class InterfaceImplementationTests {

    @Test
    @DisplayName("Should implement all InstancePre interface methods")
    void shouldImplementAllInstancePreMethods() {
      final Set<String> interfaceMethods =
          Arrays.stream(InstancePre.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      final Set<String> classMethods =
          Arrays.stream(PanamaInstancePre.class.getMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String methodName : interfaceMethods) {
        assertTrue(
            classMethods.contains(methodName), "PanamaInstancePre should implement " + methodName);
      }
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("closed field should use AtomicBoolean for thread safety")
    void closedFieldShouldUseAtomicBoolean() throws NoSuchFieldException {
      final java.lang.reflect.Field field = PanamaInstancePre.class.getDeclaredField("closed");
      assertEquals(
          java.util.concurrent.atomic.AtomicBoolean.class,
          field.getType(),
          "closed should be AtomicBoolean for thread safety");
    }
  }

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("Should have ensureNotClosed private method")
    void shouldHaveEnsureNotClosedMethod() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getDeclaredMethod("ensureNotClosed");
      assertNotNull(method, "ensureNotClosed method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "ensureNotClosed should be private");
      assertEquals(void.class, method.getReturnType(), "ensureNotClosed should return void");
    }
  }

  @Nested
  @DisplayName("Exception Handling Tests")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("ensureNotClosed should throw WasmException")
    void ensureNotClosedShouldThrowWasmException() throws NoSuchMethodException {
      final Method method = PanamaInstancePre.class.getDeclaredMethod("ensureNotClosed");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "ensureNotClosed should declare exceptions");
      boolean foundWasmException = false;
      for (final Class<?> exceptionType : exceptionTypes) {
        if (ai.tegmentum.wasmtime4j.exception.WasmException.class.isAssignableFrom(exceptionType)) {
          foundWasmException = true;
          break;
        }
      }
      assertTrue(foundWasmException, "ensureNotClosed should throw WasmException");
    }
  }
}
