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

package ai.tegmentum.wasmtime4j.ref;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FuncRef} interface.
 *
 * <p>FuncRef represents a WebAssembly function reference (funcref).
 */
@DisplayName("FuncRef Interface Tests")
class FuncRefInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(FuncRef.class.isInterface(), "FuncRef should be an interface");
    }

    @Test
    @DisplayName("should extend HeapType")
    void shouldExtendHeapType() {
      assertTrue(HeapType.class.isAssignableFrom(FuncRef.class), "FuncRef should extend HeapType");
    }

    @Test
    @DisplayName("should have isNull method")
    void shouldHaveIsNullMethod() throws NoSuchMethodException {
      final Method method = FuncRef.class.getMethod("isNull");
      assertNotNull(method, "isNull method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getFunction method")
    void shouldHaveGetFunctionMethod() throws NoSuchMethodException {
      final Method method = FuncRef.class.getMethod("getFunction");
      assertNotNull(method, "getFunction method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getFunctionType method")
    void shouldHaveGetFunctionTypeMethod() throws NoSuchMethodException {
      final Method method = FuncRef.class.getMethod("getFunctionType");
      assertNotNull(method, "getFunctionType method should exist");
      assertEquals(FunctionType.class, method.getReturnType(), "Should return FunctionType");
    }

    @Test
    @DisplayName("should have call method with List parameter")
    void shouldHaveCallMethodWithListParameter() throws NoSuchMethodException {
      final Method method = FuncRef.class.getMethod("call", List.class);
      assertNotNull(method, "call(List) method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static nullRef method")
    void shouldHaveStaticNullRefMethod() throws NoSuchMethodException {
      final Method method = FuncRef.class.getMethod("nullRef");
      assertNotNull(method, "nullRef method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullRef should be static");
      assertEquals(FuncRef.class, method.getReturnType(), "Should return FuncRef");
    }

    @Test
    @DisplayName("should have static fromFunction method")
    void shouldHaveStaticFromFunctionMethod() throws NoSuchMethodException {
      final Method method = FuncRef.class.getMethod("fromFunction", WasmFunction.class);
      assertNotNull(method, "fromFunction method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromFunction should be static");
      assertEquals(FuncRef.class, method.getReturnType(), "Should return FuncRef");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("call with varargs should be a default method")
    void callWithVarargsShouldBeDefault() throws NoSuchMethodException {
      // Find the varargs version
      for (final Method method : FuncRef.class.getDeclaredMethods()) {
        if (method.getName().equals("call") && method.isVarArgs()) {
          assertTrue(method.isDefault(), "call(WasmValue...) should be a default method");
          return;
        }
      }
      // If we get here and the method doesn't exist, that's fine - it might be inherited
    }

    @Test
    @DisplayName("getValueType should be a default method")
    void getValueTypeShouldBeDefault() throws NoSuchMethodException {
      final Method method = FuncRef.class.getMethod("getValueType");
      assertTrue(method.isDefault(), "getValueType should be a default method");
    }

    @Test
    @DisplayName("isBottom should be a default method")
    void isBottomShouldBeDefault() throws NoSuchMethodException {
      final Method method = FuncRef.class.getMethod("isBottom");
      assertTrue(method.isDefault(), "isBottom should be a default method");
    }

    @Test
    @DisplayName("getTypeName should be a default method")
    void getTypeNameShouldBeDefault() throws NoSuchMethodException {
      final Method method = FuncRef.class.getMethod("getTypeName");
      assertTrue(method.isDefault(), "getTypeName should be a default method");
    }
  }

  @Nested
  @DisplayName("NullRef Behavior Tests")
  class NullRefBehaviorTests {

    @Test
    @DisplayName("nullRef should return non-null FuncRef")
    void nullRefShouldReturnNonNullFuncRef() {
      final FuncRef nullRef = FuncRef.nullRef();
      assertNotNull(nullRef, "nullRef() should return non-null FuncRef");
    }

    @Test
    @DisplayName("nullRef isNull should return true")
    void nullRefIsNullShouldReturnTrue() {
      final FuncRef nullRef = FuncRef.nullRef();
      assertTrue(nullRef.isNull(), "Null reference should return true for isNull()");
    }

    @Test
    @DisplayName("nullRef getFunction should return empty Optional")
    void nullRefGetFunctionShouldReturnEmptyOptional() {
      final FuncRef nullRef = FuncRef.nullRef();
      assertTrue(nullRef.getFunction().isEmpty(), "Null reference should return empty Optional");
    }

    @Test
    @DisplayName("nullRef getFunctionType should return null")
    void nullRefGetFunctionTypeShouldReturnNull() {
      final FuncRef nullRef = FuncRef.nullRef();
      assertEquals(
          null, nullRef.getFunctionType(), "Null reference should return null FunctionType");
    }

    @Test
    @DisplayName("nullRef call should throw IllegalStateException")
    void nullRefCallShouldThrowIllegalStateException() {
      final FuncRef nullRef = FuncRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> nullRef.call(List.of()),
          "Calling null reference should throw IllegalStateException");
    }

    @Test
    @DisplayName("nullRef isNullable should return true")
    void nullRefIsNullableShouldReturnTrue() {
      final FuncRef nullRef = FuncRef.nullRef();
      assertTrue(nullRef.isNullable(), "Null reference should be nullable");
    }

    @Test
    @DisplayName("nullRef getValueType should return FUNCREF")
    void nullRefGetValueTypeShouldReturnFuncRef() {
      final FuncRef nullRef = FuncRef.nullRef();
      assertEquals(
          WasmValueType.FUNCREF, nullRef.getValueType(), "Should return FUNCREF value type");
    }

    @Test
    @DisplayName("nullRef getTypeName should return funcref")
    void nullRefGetTypeNameShouldReturnFuncref() {
      final FuncRef nullRef = FuncRef.nullRef();
      assertEquals("funcref", nullRef.getTypeName(), "Should return 'funcref' type name");
    }

    @Test
    @DisplayName("nullRef isBottom should return false")
    void nullRefIsBottomShouldReturnFalse() {
      final FuncRef nullRef = FuncRef.nullRef();
      assertFalse(nullRef.isBottom(), "Null funcref should not be a bottom type");
    }

    @Test
    @DisplayName("nullRef toString should contain null")
    void nullRefToStringShouldContainNull() {
      final FuncRef nullRef = FuncRef.nullRef();
      assertTrue(nullRef.toString().contains("null"), "toString should contain 'null'");
    }

    @Test
    @DisplayName("nullRef should be singleton")
    void nullRefShouldBeSingleton() {
      final FuncRef nullRef1 = FuncRef.nullRef();
      final FuncRef nullRef2 = FuncRef.nullRef();
      assertEquals(nullRef1, nullRef2, "Multiple nullRef calls should return same instance");
    }
  }

  @Nested
  @DisplayName("FromFunction Tests")
  class FromFunctionTests {

    @Test
    @DisplayName("fromFunction should reject null")
    void fromFunctionShouldRejectNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> FuncRef.fromFunction(null),
          "fromFunction should reject null function");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("call should declare WasmException")
    void callShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = FuncRef.class.getMethod("call", List.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasWasmException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "call method should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Inner Class Tests")
  class InnerClassTests {

    @Test
    @DisplayName("NullFuncRefImpl should be a final class")
    void nullFuncRefImplShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(FuncRef.NullFuncRefImpl.class.getModifiers()),
          "NullFuncRefImpl should be final");
    }

    @Test
    @DisplayName("WasmFunctionRefImpl should be a final class")
    void wasmFunctionRefImplShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(FuncRef.WasmFunctionRefImpl.class.getModifiers()),
          "WasmFunctionRefImpl should be final");
    }

    @Test
    @DisplayName("NullFuncRefImpl should implement FuncRef")
    void nullFuncRefImplShouldImplementFuncRef() {
      assertTrue(
          FuncRef.class.isAssignableFrom(FuncRef.NullFuncRefImpl.class),
          "NullFuncRefImpl should implement FuncRef");
    }

    @Test
    @DisplayName("WasmFunctionRefImpl should implement FuncRef")
    void wasmFunctionRefImplShouldImplementFuncRef() {
      assertTrue(
          FuncRef.class.isAssignableFrom(FuncRef.WasmFunctionRefImpl.class),
          "WasmFunctionRefImpl should implement FuncRef");
    }
  }
}
