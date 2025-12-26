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

import ai.tegmentum.wasmtime4j.FunctionReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaFunctionReference} class.
 *
 * <p>This test class verifies the Panama implementation of FunctionReference interface using
 * reflection-based testing to avoid triggering native library loading.
 */
@DisplayName("PanamaFunctionReference Tests")
class PanamaFunctionReferenceTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaFunctionReference should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaFunctionReference.class.getModifiers()),
          "PanamaFunctionReference should be final");
    }

    @Test
    @DisplayName("PanamaFunctionReference should implement FunctionReference interface")
    void shouldImplementFunctionReferenceInterface() {
      assertTrue(
          FunctionReference.class.isAssignableFrom(PanamaFunctionReference.class),
          "PanamaFunctionReference should implement FunctionReference");
    }

    @Test
    @DisplayName("PanamaFunctionReference should be public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(PanamaFunctionReference.class.getModifiers()),
          "PanamaFunctionReference should be public");
    }
  }

  @Nested
  @DisplayName("Interface Method Implementation Tests")
  class InterfaceMethodTests {

    @Test
    @DisplayName("Should implement getFunctionType method")
    void shouldImplementGetFunctionTypeMethod() throws NoSuchMethodException {
      final Method method = PanamaFunctionReference.class.getMethod("getFunctionType");
      assertNotNull(method, "getFunctionType method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.FunctionType.class,
          method.getReturnType(),
          "getFunctionType should return FunctionType");
    }

    @Test
    @DisplayName("Should implement call method with varargs")
    void shouldImplementCallMethod() throws NoSuchMethodException {
      final Method method =
          PanamaFunctionReference.class.getMethod(
              "call", ai.tegmentum.wasmtime4j.WasmValue[].class);
      assertNotNull(method, "call method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.WasmValue[].class,
          method.getReturnType(),
          "call should return WasmValue[]");
      assertTrue(method.isVarArgs(), "call should accept varargs");
    }

    @Test
    @DisplayName("Should implement getName method")
    void shouldImplementGetNameMethod() throws NoSuchMethodException {
      final Method method = PanamaFunctionReference.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "getName should return String");
    }

    @Test
    @DisplayName("Should implement isValid method")
    void shouldImplementIsValidMethod() throws NoSuchMethodException {
      final Method method = PanamaFunctionReference.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("Should implement getId method")
    void shouldImplementGetIdMethod() throws NoSuchMethodException {
      final Method method = PanamaFunctionReference.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "getId should return long");
    }
  }

  @Nested
  @DisplayName("Additional Method Tests")
  class AdditionalMethodTests {

    @Test
    @DisplayName("Should have isHostFunction method")
    void shouldHaveIsHostFunctionMethod() throws NoSuchMethodException {
      final Method method = PanamaFunctionReference.class.getMethod("isHostFunction");
      assertNotNull(method, "isHostFunction method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isHostFunction should return boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isHostFunction should be public");
    }

    @Test
    @DisplayName("Should have isWasmFunction method")
    void shouldHaveIsWasmFunctionMethod() throws NoSuchMethodException {
      final Method method = PanamaFunctionReference.class.getMethod("isWasmFunction");
      assertNotNull(method, "isWasmFunction method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isWasmFunction should return boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isWasmFunction should be public");
    }

    @Test
    @DisplayName("Should have getUpcallStub method")
    void shouldHaveGetUpcallStubMethod() throws NoSuchMethodException {
      final Method method = PanamaFunctionReference.class.getMethod("getUpcallStub");
      assertNotNull(method, "getUpcallStub method should exist");
      assertEquals(
          java.lang.foreign.MemorySegment.class,
          method.getReturnType(),
          "getUpcallStub should return MemorySegment");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getUpcallStub should be public");
    }

    @Test
    @DisplayName("Should have longValue method")
    void shouldHaveLongValueMethod() throws NoSuchMethodException {
      final Method method = PanamaFunctionReference.class.getMethod("longValue");
      assertNotNull(method, "longValue method should exist");
      assertEquals(long.class, method.getReturnType(), "longValue should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "longValue should be public");
    }

    @Test
    @DisplayName("Should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaFunctionReference.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "close should be public");
    }

    @Test
    @DisplayName("Should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = PanamaFunctionReference.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("Should have getRegistryStats static method")
    void shouldHaveGetRegistryStatsMethod() throws NoSuchMethodException {
      final Method method = PanamaFunctionReference.class.getDeclaredMethod("getRegistryStats");
      assertNotNull(method, "getRegistryStats method should exist");
      assertEquals(long[].class, method.getReturnType(), "getRegistryStats should return long[]");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getRegistryStats should be static");
    }

    @Test
    @DisplayName("Should have getFromRegistry static method")
    void shouldHaveGetFromRegistryMethod() throws NoSuchMethodException {
      final Method method =
          PanamaFunctionReference.class.getDeclaredMethod("getFromRegistry", long.class);
      assertNotNull(method, "getFromRegistry method should exist");
      assertEquals(
          PanamaFunctionReference.class,
          method.getReturnType(),
          "getFromRegistry should return PanamaFunctionReference");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getFromRegistry should be static");
    }

    @Test
    @DisplayName("Should have getFunctionReferenceById public static method")
    void shouldHaveGetFunctionReferenceByIdMethod() throws NoSuchMethodException {
      final Method method =
          PanamaFunctionReference.class.getMethod("getFunctionReferenceById", long.class);
      assertNotNull(method, "getFunctionReferenceById method should exist");
      assertEquals(
          FunctionReference.class,
          method.getReturnType(),
          "getFunctionReferenceById should return FunctionReference");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "getFunctionReferenceById should be static");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "getFunctionReferenceById should be public");
    }

    @Test
    @DisplayName("Should have invokeFunctionReferenceCallback static method")
    void shouldHaveInvokeFunctionReferenceCallbackMethod() throws NoSuchMethodException {
      final Method method =
          PanamaFunctionReference.class.getDeclaredMethod(
              "invokeFunctionReferenceCallback",
              long.class,
              ai.tegmentum.wasmtime4j.WasmValue[].class);
      assertNotNull(method, "invokeFunctionReferenceCallback method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.WasmValue[].class,
          method.getReturnType(),
          "invokeFunctionReferenceCallback should return WasmValue[]");
      assertTrue(
          Modifier.isStatic(method.getModifiers()),
          "invokeFunctionReferenceCallback should be static");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have constructor for host function")
    void shouldHaveHostFunctionConstructor() {
      boolean foundConstructor = false;
      for (final java.lang.reflect.Constructor<?> constructor :
          PanamaFunctionReference.class.getConstructors()) {
        final Class<?>[] paramTypes = constructor.getParameterTypes();
        if (paramTypes.length == 5
            && ai.tegmentum.wasmtime4j.HostFunction.class.isAssignableFrom(paramTypes[0])
            && ai.tegmentum.wasmtime4j.FunctionType.class.isAssignableFrom(paramTypes[1])
            && PanamaStore.class.isAssignableFrom(paramTypes[2])
            && ArenaResourceManager.class.isAssignableFrom(paramTypes[3])
            && PanamaErrorHandler.class.isAssignableFrom(paramTypes[4])) {
          foundConstructor = true;
          break;
        }
      }
      assertTrue(foundConstructor, "Should have constructor for HostFunction");
    }

    @Test
    @DisplayName("Should have constructor for wasm function")
    void shouldHaveWasmFunctionConstructor() {
      boolean foundConstructor = false;
      for (final java.lang.reflect.Constructor<?> constructor :
          PanamaFunctionReference.class.getConstructors()) {
        final Class<?>[] paramTypes = constructor.getParameterTypes();
        if (paramTypes.length == 4
            && ai.tegmentum.wasmtime4j.WasmFunction.class.isAssignableFrom(paramTypes[0])
            && PanamaStore.class.isAssignableFrom(paramTypes[1])
            && ArenaResourceManager.class.isAssignableFrom(paramTypes[2])
            && PanamaErrorHandler.class.isAssignableFrom(paramTypes[3])) {
          foundConstructor = true;
          break;
        }
      }
      assertTrue(foundConstructor, "Should have constructor for WasmFunction");
    }

    @Test
    @DisplayName("Should have exactly two public constructors")
    void shouldHaveTwoPublicConstructors() {
      final int publicConstructorCount =
          (int)
              Arrays.stream(PanamaFunctionReference.class.getConstructors())
                  .filter(c -> Modifier.isPublic(c.getModifiers()))
                  .count();
      assertEquals(2, publicConstructorCount, "Should have exactly 2 public constructors");
    }
  }

  @Nested
  @DisplayName("Method Signature Validation Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("call method should throw WasmException")
    void callMethodShouldThrowWasmException() throws NoSuchMethodException {
      final Method method =
          PanamaFunctionReference.class.getMethod(
              "call", ai.tegmentum.wasmtime4j.WasmValue[].class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "call method should declare exceptions");
      boolean foundWasmException = false;
      for (final Class<?> exceptionType : exceptionTypes) {
        if (ai.tegmentum.wasmtime4j.exception.WasmException.class.isAssignableFrom(exceptionType)) {
          foundWasmException = true;
          break;
        }
      }
      assertTrue(foundWasmException, "call method should throw WasmException");
    }

    @Test
    @DisplayName("close method should throw WasmException")
    void closeMethodShouldThrowWasmException() throws NoSuchMethodException {
      final Method method = PanamaFunctionReference.class.getMethod("close");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertTrue(exceptionTypes.length > 0, "close method should declare exceptions");
      boolean foundWasmException = false;
      for (final Class<?> exceptionType : exceptionTypes) {
        if (ai.tegmentum.wasmtime4j.exception.WasmException.class.isAssignableFrom(exceptionType)) {
          foundWasmException = true;
          break;
        }
      }
      assertTrue(foundWasmException, "close method should throw WasmException");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have FUNCTION_REFERENCE_REGISTRY static field")
    void shouldHaveFunctionReferenceRegistryField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaFunctionReference.class.getDeclaredField("FUNCTION_REFERENCE_REGISTRY");
      assertNotNull(field, "FUNCTION_REFERENCE_REGISTRY field should exist");
      assertTrue(
          Modifier.isStatic(field.getModifiers()), "FUNCTION_REFERENCE_REGISTRY should be static");
      assertTrue(
          Modifier.isFinal(field.getModifiers()), "FUNCTION_REFERENCE_REGISTRY should be final");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()),
          "FUNCTION_REFERENCE_REGISTRY should be private");
    }

    @Test
    @DisplayName("Should have NEXT_FUNCTION_REFERENCE_ID static field")
    void shouldHaveNextFunctionReferenceIdField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaFunctionReference.class.getDeclaredField("NEXT_FUNCTION_REFERENCE_ID");
      assertNotNull(field, "NEXT_FUNCTION_REFERENCE_ID field should exist");
      assertTrue(
          Modifier.isStatic(field.getModifiers()), "NEXT_FUNCTION_REFERENCE_ID should be static");
      assertTrue(
          Modifier.isFinal(field.getModifiers()), "NEXT_FUNCTION_REFERENCE_ID should be final");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "NEXT_FUNCTION_REFERENCE_ID should be private");
    }

    @Test
    @DisplayName("Should have LOGGER static field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaFunctionReference.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
    }

    @Test
    @DisplayName("Should have functionReferenceId field")
    void shouldHaveFunctionReferenceIdField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaFunctionReference.class.getDeclaredField("functionReferenceId");
      assertNotNull(field, "functionReferenceId field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "functionReferenceId should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "functionReferenceId should be private");
      assertEquals(long.class, field.getType(), "functionReferenceId should be long type");
    }

    @Test
    @DisplayName("Should have functionName field")
    void shouldHaveFunctionNameField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaFunctionReference.class.getDeclaredField("functionName");
      assertNotNull(field, "functionName field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "functionName should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "functionName should be private");
      assertEquals(String.class, field.getType(), "functionName should be String type");
    }

    @Test
    @DisplayName("Should have functionType field")
    void shouldHaveFunctionTypeField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaFunctionReference.class.getDeclaredField("functionType");
      assertNotNull(field, "functionType field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "functionType should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "functionType should be private");
    }

    @Test
    @DisplayName("Should have hostFunction field")
    void shouldHaveHostFunctionField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaFunctionReference.class.getDeclaredField("hostFunction");
      assertNotNull(field, "hostFunction field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "hostFunction should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "hostFunction should be private");
    }

    @Test
    @DisplayName("Should have wasmFunction field")
    void shouldHaveWasmFunctionField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaFunctionReference.class.getDeclaredField("wasmFunction");
      assertNotNull(field, "wasmFunction field should exist");
      assertTrue(Modifier.isFinal(field.getModifiers()), "wasmFunction should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "wasmFunction should be private");
    }

    @Test
    @DisplayName("Should have closed field")
    void shouldHaveClosedField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaFunctionReference.class.getDeclaredField("closed");
      assertNotNull(field, "closed field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "closed should be private");
      assertTrue(Modifier.isVolatile(field.getModifiers()), "closed should be volatile");
      assertEquals(boolean.class, field.getType(), "closed should be boolean type");
    }

    @Test
    @DisplayName("Should have upcallStub field")
    void shouldHaveUpcallStubField() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaFunctionReference.class.getDeclaredField("upcallStub");
      assertNotNull(field, "upcallStub field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "upcallStub should be private");
      assertEquals(
          java.lang.foreign.MemorySegment.class,
          field.getType(),
          "upcallStub should be MemorySegment type");
    }
  }

  @Nested
  @DisplayName("Interface Implementation Completeness Tests")
  class InterfaceImplementationTests {

    @Test
    @DisplayName("Should implement all FunctionReference interface methods")
    void shouldImplementAllFunctionReferenceMethods() {
      final Set<String> interfaceMethods =
          Arrays.stream(FunctionReference.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      final Set<String> classMethods =
          Arrays.stream(PanamaFunctionReference.class.getMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (final String methodName : interfaceMethods) {
        assertTrue(
            classMethods.contains(methodName),
            "PanamaFunctionReference should implement " + methodName);
      }
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Registry should use ConcurrentHashMap")
    void registryShouldUseConcurrentHashMap() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaFunctionReference.class.getDeclaredField("FUNCTION_REFERENCE_REGISTRY");
      assertTrue(
          java.util.concurrent.ConcurrentHashMap.class.isAssignableFrom(field.getType()),
          "FUNCTION_REFERENCE_REGISTRY should be ConcurrentHashMap");
    }

    @Test
    @DisplayName("Next ID counter should use AtomicLong")
    void nextIdShouldUseAtomicLong() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaFunctionReference.class.getDeclaredField("NEXT_FUNCTION_REFERENCE_ID");
      assertTrue(
          java.util.concurrent.atomic.AtomicLong.class.isAssignableFrom(field.getType()),
          "NEXT_FUNCTION_REFERENCE_ID should be AtomicLong");
    }

    @Test
    @DisplayName("closed field should be volatile for thread safety")
    void closedFieldShouldBeVolatile() throws NoSuchFieldException {
      final java.lang.reflect.Field field =
          PanamaFunctionReference.class.getDeclaredField("closed");
      assertTrue(
          Modifier.isVolatile(field.getModifiers()),
          "closed field should be volatile for thread safety");
    }
  }
}
