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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentFunc;
import ai.tegmentum.wasmtime4j.component.ComponentTypedFunc;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link PanamaComponentTypedFunc}.
 *
 * <p>These tests verify class structure, method signatures, and field definitions using reflection.
 * This approach allows testing without requiring native library loading.
 */
@DisplayName("PanamaComponentTypedFunc Tests")
class PanamaComponentTypedFuncTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaComponentTypedFunc should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaComponentTypedFunc.class.getModifiers()),
          "PanamaComponentTypedFunc should be public");
      assertTrue(
          Modifier.isFinal(PanamaComponentTypedFunc.class.getModifiers()),
          "PanamaComponentTypedFunc should be final");
    }

    @Test
    @DisplayName("PanamaComponentTypedFunc should implement ComponentTypedFunc")
    void shouldImplementComponentTypedFunc() {
      assertTrue(
          ComponentTypedFunc.class.isAssignableFrom(PanamaComponentTypedFunc.class),
          "PanamaComponentTypedFunc should implement ComponentTypedFunc");
    }

    @Test
    @DisplayName("Class should be in correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.panama",
          PanamaComponentTypedFunc.class.getPackage().getName(),
          "Should be in panama package");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      Field field = PanamaComponentTypedFunc.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertEquals(Logger.class, field.getType(), "Should be Logger type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have function field")
    void shouldHaveFunctionField() throws NoSuchFieldException {
      Field field = PanamaComponentTypedFunc.class.getDeclaredField("function");
      assertNotNull(field, "function field should exist");
      assertEquals(ComponentFunc.class, field.getType(), "Should be ComponentFunc type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have signature field")
    void shouldHaveSignatureField() throws NoSuchFieldException {
      Field field = PanamaComponentTypedFunc.class.getDeclaredField("signature");
      assertNotNull(field, "signature field should exist");
      assertEquals(String.class, field.getType(), "Should be String type");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("Should have resourceHandle field")
    void shouldHaveResourceHandleField() throws NoSuchFieldException {
      Field field = PanamaComponentTypedFunc.class.getDeclaredField("resourceHandle");
      assertNotNull(field, "resourceHandle field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should have public constructor with ComponentFunc and String")
    void shouldHavePublicConstructor() throws NoSuchMethodException {
      java.lang.reflect.Constructor<?> constructor =
          PanamaComponentTypedFunc.class.getConstructor(ComponentFunc.class, String.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("Constructor should throw on null function")
    void constructorShouldThrowOnNullFunction() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaComponentTypedFunc(null, "s32->s32"),
          "Should throw on null function");
    }

    @Test
    @DisplayName("Constructor should throw on null signature")
    void constructorShouldThrowOnNullSignature() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaComponentTypedFunc(new StubComponentFunc(), null),
          "Should throw on null signature");
    }

    @Test
    @DisplayName("Constructor should throw on empty signature")
    void constructorShouldThrowOnEmptySignature() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaComponentTypedFunc(new StubComponentFunc(), ""),
          "Should throw on empty signature");
    }
  }

  @Nested
  @DisplayName("Void Return Method Tests")
  class VoidReturnMethodTests {

    @Test
    @DisplayName("callVoidToVoid should exist and return void")
    void callVoidToVoidShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("callVoidToVoid");
      assertNotNull(method, "callVoidToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("callS32ToVoid should exist with int parameter")
    void callS32ToVoidShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("callS32ToVoid", int.class);
      assertNotNull(method, "callS32ToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("callS32S32ToVoid should exist with two int parameters")
    void callS32S32ToVoidShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentTypedFunc.class.getMethod("callS32S32ToVoid", int.class, int.class);
      assertNotNull(method, "callS32S32ToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("callS64ToVoid should exist with long parameter")
    void callS64ToVoidShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("callS64ToVoid", long.class);
      assertNotNull(method, "callS64ToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("callS64S64ToVoid should exist with two long parameters")
    void callS64S64ToVoidShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentTypedFunc.class.getMethod("callS64S64ToVoid", long.class, long.class);
      assertNotNull(method, "callS64S64ToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("callStringToVoid should exist with String parameter")
    void callStringToVoidShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("callStringToVoid", String.class);
      assertNotNull(method, "callStringToVoid method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("S32 Return Method Tests")
  class S32ReturnMethodTests {

    @Test
    @DisplayName("callS32ToS32 should exist and return int")
    void callS32ToS32ShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("callS32ToS32", int.class);
      assertNotNull(method, "callS32ToS32 method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("callS32S32ToS32 should exist and return int")
    void callS32S32ToS32ShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentTypedFunc.class.getMethod("callS32S32ToS32", int.class, int.class);
      assertNotNull(method, "callS32S32ToS32 method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("callS32S32S32ToS32 should exist and return int")
    void callS32S32S32ToS32ShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentTypedFunc.class.getMethod(
              "callS32S32S32ToS32", int.class, int.class, int.class);
      assertNotNull(method, "callS32S32S32ToS32 method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("callS64ToS32 should exist and return int")
    void callS64ToS32ShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("callS64ToS32", long.class);
      assertNotNull(method, "callS64ToS32 method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("S64 Return Method Tests")
  class S64ReturnMethodTests {

    @Test
    @DisplayName("callS64ToS64 should exist and return long")
    void callS64ToS64ShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("callS64ToS64", long.class);
      assertNotNull(method, "callS64ToS64 method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("callS64S64ToS64 should exist and return long")
    void callS64S64ToS64ShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentTypedFunc.class.getMethod("callS64S64ToS64", long.class, long.class);
      assertNotNull(method, "callS64S64ToS64 method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("callS64S64S64ToS64 should exist and return long")
    void callS64S64S64ToS64ShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentTypedFunc.class.getMethod(
              "callS64S64S64ToS64", long.class, long.class, long.class);
      assertNotNull(method, "callS64S64S64ToS64 method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("callS32S32ToS64 should exist and return long")
    void callS32S32ToS64ShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentTypedFunc.class.getMethod("callS32S32ToS64", int.class, int.class);
      assertNotNull(method, "callS32S32ToS64 method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("F32 Return Method Tests")
  class F32ReturnMethodTests {

    @Test
    @DisplayName("callF32ToF32 should exist and return float")
    void callF32ToF32ShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("callF32ToF32", float.class);
      assertNotNull(method, "callF32ToF32 method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }

    @Test
    @DisplayName("callF32F32ToF32 should exist and return float")
    void callF32F32ToF32ShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentTypedFunc.class.getMethod("callF32F32ToF32", float.class, float.class);
      assertNotNull(method, "callF32F32ToF32 method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }

    @Test
    @DisplayName("callF32F32F32ToF32 should exist and return float")
    void callF32F32F32ToF32ShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentTypedFunc.class.getMethod(
              "callF32F32F32ToF32", float.class, float.class, float.class);
      assertNotNull(method, "callF32F32F32ToF32 method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }
  }

  @Nested
  @DisplayName("F64 Return Method Tests")
  class F64ReturnMethodTests {

    @Test
    @DisplayName("callF64ToF64 should exist and return double")
    void callF64ToF64ShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("callF64ToF64", double.class);
      assertNotNull(method, "callF64ToF64 method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("callF64F64ToF64 should exist and return double")
    void callF64F64ToF64ShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentTypedFunc.class.getMethod("callF64F64ToF64", double.class, double.class);
      assertNotNull(method, "callF64F64ToF64 method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }

    @Test
    @DisplayName("callF64F64F64ToF64 should exist and return double")
    void callF64F64F64ToF64ShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentTypedFunc.class.getMethod(
              "callF64F64F64ToF64", double.class, double.class, double.class);
      assertNotNull(method, "callF64F64F64ToF64 method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("String Return Method Tests")
  class StringReturnMethodTests {

    @Test
    @DisplayName("callVoidToString should exist and return String")
    void callVoidToStringShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("callVoidToString");
      assertNotNull(method, "callVoidToString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("callStringToString should exist and return String")
    void callStringToStringShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("callStringToString", String.class);
      assertNotNull(method, "callStringToString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("callStringStringToString should exist and return String")
    void callStringStringToStringShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentTypedFunc.class.getMethod(
              "callStringStringToString", String.class, String.class);
      assertNotNull(method, "callStringStringToString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Bool Method Tests")
  class BoolMethodTests {

    @Test
    @DisplayName("callVoidToBool should exist and return boolean")
    void callVoidToBoolShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("callVoidToBool");
      assertNotNull(method, "callVoidToBool method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("callBoolToBool should exist and return boolean")
    void callBoolToBoolShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("callBoolToBool", boolean.class);
      assertNotNull(method, "callBoolToBool method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Metadata Method Tests")
  class MetadataMethodTests {

    @Test
    @DisplayName("getSignature should exist and return String")
    void getSignatureShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("getSignature");
      assertNotNull(method, "getSignature method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("getFunction should exist and return ComponentFunc")
    void getFunctionShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("getFunction");
      assertNotNull(method, "getFunction method should exist");
      assertEquals(ComponentFunc.class, method.getReturnType(), "Should return ComponentFunc");
    }

    @Test
    @DisplayName("close should exist and return void")
    void closeShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("toString should exist and return String")
    void toStringShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("validateSingleResult should exist as private method")
    void validateSingleResultShouldExist() throws NoSuchMethodException {
      Method method =
          PanamaComponentTypedFunc.class.getDeclaredMethod(
              "validateSingleResult", java.util.List.class);
      assertNotNull(method, "validateSingleResult method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("ensureNotClosed should exist as private method")
    void ensureNotClosedShouldExist() throws NoSuchMethodException {
      Method method = PanamaComponentTypedFunc.class.getDeclaredMethod("ensureNotClosed");
      assertNotNull(method, "ensureNotClosed method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("Should implement all ComponentTypedFunc interface methods")
    void shouldImplementAllInterfaceMethods() {
      for (Method interfaceMethod : ComponentTypedFunc.class.getMethods()) {
        if (!interfaceMethod.isDefault() && !Modifier.isStatic(interfaceMethod.getModifiers())) {
          boolean found = false;
          for (Method implMethod : PanamaComponentTypedFunc.class.getMethods()) {
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

    private boolean arrayEquals(final Class<?>[] a, final Class<?>[] b) {
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
  @DisplayName("AutoCloseable Tests")
  class AutoCloseableTests {

    @Test
    @DisplayName("Should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaComponentTypedFunc.class),
          "Should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Instance Behavior Tests")
  class InstanceBehaviorTests {

    @Test
    @DisplayName("getSignature should return signature provided in constructor")
    void getSignatureShouldReturnProvidedSignature() {
      StubComponentFunc stubFunc = new StubComponentFunc();
      String signature = "s32,s32->s32";
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, signature);

      assertEquals(signature, typedFunc.getSignature(), "Should return provided signature");
    }

    @Test
    @DisplayName("getFunction should return function provided in constructor")
    void getFunctionShouldReturnProvidedFunction() {
      StubComponentFunc stubFunc = new StubComponentFunc();
      String signature = "s32->s32";
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, signature);

      assertEquals(stubFunc, typedFunc.getFunction(), "Should return provided function");
    }

    @Test
    @DisplayName("close should not throw")
    void closeShouldNotThrow() {
      StubComponentFunc stubFunc = new StubComponentFunc();
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, "s32->s32");

      typedFunc.close(); // Should not throw
    }

    @Test
    @DisplayName("close should be idempotent")
    void closeShouldBeIdempotent() {
      StubComponentFunc stubFunc = new StubComponentFunc();
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, "s32->s32");

      typedFunc.close();
      typedFunc.close(); // Should not throw
    }

    @Test
    @DisplayName("toString should contain signature")
    void toStringShouldContainSignature() {
      StubComponentFunc stubFunc = new StubComponentFunc();
      String signature = "s32,s32->s32";
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, signature);

      String result = typedFunc.toString();
      assertTrue(result.contains(signature), "toString should contain signature");
    }

    @Test
    @DisplayName("toString should contain class name")
    void toStringShouldContainClassName() {
      StubComponentFunc stubFunc = new StubComponentFunc();
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, "s32->s32");

      String result = typedFunc.toString();
      assertTrue(result.contains("PanamaComponentTypedFunc"), "toString should contain class name");
    }

    @Test
    @DisplayName("closed state should be tracked")
    void closedStateShouldBeTracked() throws Exception {
      StubComponentFunc stubFunc = new StubComponentFunc();
      PanamaComponentTypedFunc typedFunc = new PanamaComponentTypedFunc(stubFunc, "s32->s32");

      Field resourceHandleField = PanamaComponentTypedFunc.class.getDeclaredField("resourceHandle");
      resourceHandleField.setAccessible(true);
      Object resourceHandle = resourceHandleField.get(typedFunc);
      Method isClosedMethod = resourceHandle.getClass().getMethod("isClosed");

      assertFalse(
          (boolean) isClosedMethod.invoke(resourceHandle), "Should not be closed initially");

      typedFunc.close();

      assertTrue((boolean) isClosedMethod.invoke(resourceHandle), "Should be closed after close()");
    }
  }

  /** Stub implementation of ComponentFunc for testing constructor behavior. */
  private static class StubComponentFunc implements ComponentFunc {

    @Override
    public String getName() {
      return "stub";
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.component.ComponentTypeDescriptor>
        getParameterTypes() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.component.ComponentTypeDescriptor>
        getResultTypes() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.component.ComponentVal> call(
        final ai.tegmentum.wasmtime4j.component.ComponentVal... args) {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ai.tegmentum.wasmtime4j.component.ComponentVal> call(
        final java.util.List<ai.tegmentum.wasmtime4j.component.ComponentVal> args) {
      return java.util.Collections.emptyList();
    }

    @Override
    public boolean isValid() {
      return true;
    }
  }
}
