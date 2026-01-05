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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the FuncRef interface.
 *
 * <p>FuncRef represents a WebAssembly function reference (funcref) that can be stored in tables,
 * passed as arguments, or returned from functions. It extends HeapType.
 */
@DisplayName("FuncRef Interface Tests")
class FuncRefTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(FuncRef.class.isInterface(), "FuncRef should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(FuncRef.class.getModifiers()), "FuncRef should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(FuncRef.class.getModifiers()),
          "FuncRef should not be final (interfaces cannot be final)");
    }

    @Test
    @DisplayName("should not have type parameters")
    void shouldNotHaveTypeParameters() {
      TypeVariable<?>[] typeParams = FuncRef.class.getTypeParameters();
      assertEquals(0, typeParams.length, "FuncRef should have no type parameters");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend HeapType")
    void shouldExtendHeapType() {
      Class<?>[] interfaces = FuncRef.class.getInterfaces();
      assertEquals(1, interfaces.length, "FuncRef should extend exactly 1 interface");
      assertEquals(HeapType.class, interfaces[0], "FuncRef should extend HeapType");
    }

    @Test
    @DisplayName("should be assignable from HeapType")
    void shouldBeAssignableFromHeapType() {
      assertTrue(
          HeapType.class.isAssignableFrom(FuncRef.class),
          "HeapType should be assignable from FuncRef");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have nullRef static method")
    void shouldHaveNullRefMethod() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("nullRef");
      assertNotNull(method, "nullRef method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullRef should be static");
      assertEquals(FuncRef.class, method.getReturnType(), "nullRef should return FuncRef");
      assertEquals(0, method.getParameterCount(), "nullRef should have no parameters");
    }

    @Test
    @DisplayName("should have fromFunction static method")
    void shouldHaveFromFunctionMethod() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("fromFunction", WasmFunction.class);
      assertNotNull(method, "fromFunction method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromFunction should be static");
      assertEquals(FuncRef.class, method.getReturnType(), "fromFunction should return FuncRef");
      assertEquals(1, method.getParameterCount(), "fromFunction should have 1 parameter");
      assertEquals(
          WasmFunction.class,
          method.getParameterTypes()[0],
          "fromFunction param should be WasmFunction");
    }

    @Test
    @DisplayName("should have exactly 2 static methods")
    void shouldHaveExactly2StaticMethods() {
      long staticMethods =
          Arrays.stream(FuncRef.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(2, staticMethods, "FuncRef should have exactly 2 static methods");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have isNull method")
    void shouldHaveIsNullMethod() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("isNull");
      assertNotNull(method, "isNull method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertFalse(method.isDefault(), "isNull should be abstract");
    }

    @Test
    @DisplayName("should have getFunction method")
    void shouldHaveGetFunctionMethod() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("getFunction");
      assertNotNull(method, "getFunction method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
      assertFalse(method.isDefault(), "getFunction should be abstract");
    }

    @Test
    @DisplayName("should have getFunctionType method")
    void shouldHaveGetFunctionTypeMethod() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("getFunctionType");
      assertNotNull(method, "getFunctionType method should exist");
      assertEquals(FunctionType.class, method.getReturnType(), "Should return FunctionType");
      assertFalse(method.isDefault(), "getFunctionType should be abstract");
    }

    @Test
    @DisplayName("should have call method with List parameter")
    void shouldHaveCallMethodWithList() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("call", List.class);
      assertNotNull(method, "call(List) method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
      assertFalse(method.isDefault(), "call(List) should be abstract");
    }

    @Test
    @DisplayName("should have exactly 4 abstract methods")
    void shouldHaveExactly4AbstractMethods() {
      long abstractMethods =
          Arrays.stream(FuncRef.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> !Modifier.isStatic(m.getModifiers()))
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(4, abstractMethods, "FuncRef should have exactly 4 abstract methods");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have call method with varargs as default")
    void shouldHaveCallMethodWithVarargs() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("call", WasmValue[].class);
      assertNotNull(method, "call(WasmValue...) method should exist");
      assertTrue(method.isDefault(), "call(WasmValue...) should be a default method");
      assertTrue(method.isVarArgs(), "call should accept varargs");
    }

    @Test
    @DisplayName("should have getValueType as default method")
    void shouldHaveGetValueTypeMethod() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("getValueType");
      assertNotNull(method, "getValueType method should exist");
      assertTrue(method.isDefault(), "getValueType should be a default method");
    }

    @Test
    @DisplayName("should have isBottom as default method")
    void shouldHaveIsBottomMethod() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("isBottom");
      assertNotNull(method, "isBottom method should exist");
      assertTrue(method.isDefault(), "isBottom should be a default method");
    }

    @Test
    @DisplayName("should have getTypeName as default method")
    void shouldHaveGetTypeNameMethod() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("getTypeName");
      assertNotNull(method, "getTypeName method should exist");
      assertTrue(method.isDefault(), "getTypeName should be a default method");
    }

    @Test
    @DisplayName("should have exactly 4 default methods")
    void shouldHaveExactly4DefaultMethods() {
      long defaultMethods =
          Arrays.stream(FuncRef.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(4, defaultMethods, "FuncRef should have exactly 4 default methods");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "nullRef",
              "fromFunction",
              "isNull",
              "getFunction",
              "getFunctionType",
              "call",
              "getValueType",
              "isBottom",
              "getTypeName");

      Set<String> actualMethods =
          Arrays.stream(FuncRef.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "FuncRef should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 10 declared methods")
    void shouldHaveExactly10DeclaredMethods() {
      // 2 static + 4 abstract + 4 default = 10 methods
      // Note: call appears twice (overloaded)
      long methodCount =
          Arrays.stream(FuncRef.class.getDeclaredMethods()).filter(m -> !m.isSynthetic()).count();
      assertEquals(10, methodCount, "FuncRef should have exactly 10 declared methods");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have exactly 2 nested classes")
    void shouldHave2NestedClasses() {
      assertEquals(
          2,
          FuncRef.class.getDeclaredClasses().length,
          "FuncRef should have exactly 2 nested classes");
    }

    @Test
    @DisplayName("should have NullFuncRefImpl nested class")
    void shouldHaveNullFuncRefImplClass() {
      Class<?>[] nestedClasses = FuncRef.class.getDeclaredClasses();
      boolean hasNullFuncRefImpl =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("NullFuncRefImpl"));
      assertTrue(hasNullFuncRefImpl, "FuncRef should have NullFuncRefImpl nested class");
    }

    @Test
    @DisplayName("should have WasmFunctionRefImpl nested class")
    void shouldHaveWasmFunctionRefImplClass() {
      Class<?>[] nestedClasses = FuncRef.class.getDeclaredClasses();
      boolean hasWasmFunctionRefImpl =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("WasmFunctionRefImpl"));
      assertTrue(hasWasmFunctionRefImpl, "FuncRef should have WasmFunctionRefImpl nested class");
    }

    @Test
    @DisplayName("NullFuncRefImpl should be final")
    void nullFuncRefImplShouldBeFinal() {
      Class<?>[] nestedClasses = FuncRef.class.getDeclaredClasses();
      Class<?> nullFuncRefImpl =
          Arrays.stream(nestedClasses)
              .filter(c -> c.getSimpleName().equals("NullFuncRefImpl"))
              .findFirst()
              .orElseThrow();
      assertTrue(
          Modifier.isFinal(nullFuncRefImpl.getModifiers()), "NullFuncRefImpl should be final");
    }

    @Test
    @DisplayName("WasmFunctionRefImpl should be final")
    void wasmFunctionRefImplShouldBeFinal() {
      Class<?>[] nestedClasses = FuncRef.class.getDeclaredClasses();
      Class<?> wasmFunctionRefImpl =
          Arrays.stream(nestedClasses)
              .filter(c -> c.getSimpleName().equals("WasmFunctionRefImpl"))
              .findFirst()
              .orElseThrow();
      assertTrue(
          Modifier.isFinal(wasmFunctionRefImpl.getModifiers()),
          "WasmFunctionRefImpl should be final");
    }

    @Test
    @DisplayName("NullFuncRefImpl should implement FuncRef")
    void nullFuncRefImplShouldImplementFuncRef() {
      Class<?>[] nestedClasses = FuncRef.class.getDeclaredClasses();
      Class<?> nullFuncRefImpl =
          Arrays.stream(nestedClasses)
              .filter(c -> c.getSimpleName().equals("NullFuncRefImpl"))
              .findFirst()
              .orElseThrow();
      assertTrue(
          FuncRef.class.isAssignableFrom(nullFuncRefImpl),
          "NullFuncRefImpl should implement FuncRef");
    }

    @Test
    @DisplayName("WasmFunctionRefImpl should implement FuncRef")
    void wasmFunctionRefImplShouldImplementFuncRef() {
      Class<?>[] nestedClasses = FuncRef.class.getDeclaredClasses();
      Class<?> wasmFunctionRefImpl =
          Arrays.stream(nestedClasses)
              .filter(c -> c.getSimpleName().equals("WasmFunctionRefImpl"))
              .findFirst()
              .orElseThrow();
      assertTrue(
          FuncRef.class.isAssignableFrom(wasmFunctionRefImpl),
          "WasmFunctionRefImpl should implement FuncRef");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields on interface")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0, FuncRef.class.getDeclaredFields().length, "FuncRef should have no declared fields");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("isNull should have no parameters")
    void isNullShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("isNull");
      assertEquals(0, method.getParameterCount(), "isNull should have no parameters");
    }

    @Test
    @DisplayName("getFunction should have no parameters")
    void getFunctionShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("getFunction");
      assertEquals(0, method.getParameterCount(), "getFunction should have no parameters");
    }

    @Test
    @DisplayName("getFunctionType should have no parameters")
    void getFunctionTypeShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("getFunctionType");
      assertEquals(0, method.getParameterCount(), "getFunctionType should have no parameters");
    }

    @Test
    @DisplayName("call with List should have 1 parameter")
    void callWithListShouldHave1Parameter() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("call", List.class);
      assertEquals(1, method.getParameterCount(), "call(List) should have 1 parameter");
    }

    @Test
    @DisplayName("call with varargs should have 1 parameter")
    void callWithVarargsShouldHave1Parameter() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("call", WasmValue[].class);
      assertEquals(1, method.getParameterCount(), "call(WasmValue...) should have 1 parameter");
    }
  }

  // ========================================================================
  // Method Visibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Visibility Tests")
  class MethodVisibilityTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      Arrays.stream(FuncRef.class.getDeclaredMethods())
          .filter(m -> !m.isSynthetic())
          .forEach(
              m ->
                  assertTrue(
                      Modifier.isPublic(m.getModifiers()),
                      "Method " + m.getName() + " should be public"));
    }
  }

  // ========================================================================
  // Generic Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Generic Return Type Tests")
  class GenericReturnTypeTests {

    @Test
    @DisplayName("getFunction should return Optional<WasmFunction>")
    void getFunctionShouldReturnOptionalWasmFunction() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("getFunction");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(Optional.class, paramType.getRawType(), "Raw type should be Optional");
      assertEquals(
          WasmFunction.class,
          paramType.getActualTypeArguments()[0],
          "Type argument should be WasmFunction");
    }

    @Test
    @DisplayName("call should return List<WasmValue>")
    void callShouldReturnListWasmValue() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("call", List.class);
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(List.class, paramType.getRawType(), "Raw type should be List");
      assertEquals(
          WasmValue.class,
          paramType.getActualTypeArguments()[0],
          "Type argument should be WasmValue");
    }

    @Test
    @DisplayName("call parameter should be List<WasmValue>")
    void callParameterShouldBeListWasmValue() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("call", List.class);
      Type[] paramTypes = method.getGenericParameterTypes();
      assertTrue(paramTypes[0] instanceof ParameterizedType, "Param type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) paramTypes[0];
      assertEquals(List.class, paramType.getRawType(), "Raw type should be List");
      assertEquals(
          WasmValue.class,
          paramType.getActualTypeArguments()[0],
          "Type argument should be WasmValue");
    }
  }

  // ========================================================================
  // Semantic Tests
  // ========================================================================

  @Nested
  @DisplayName("Semantic Tests")
  class SemanticTests {

    @Test
    @DisplayName("isNull should return primitive boolean")
    void isNullShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("isNull");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isNull should return primitive boolean, not Boolean");
    }

    @Test
    @DisplayName("getTypeName should return String")
    void getTypeNameShouldReturnString() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("getTypeName");
      assertEquals(String.class, method.getReturnType(), "getTypeName should return String");
    }

    @Test
    @DisplayName("isBottom should return primitive boolean")
    void isBottomShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("isBottom");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isBottom should return primitive boolean, not Boolean");
    }
  }

  // ========================================================================
  // Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Tests")
  class ExceptionTests {

    @Test
    @DisplayName("call with List should declare WasmException")
    void callWithListShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("call", List.class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "call(List) should declare 1 exception");
      assertEquals("WasmException", exceptions[0].getSimpleName(), "Should declare WasmException");
    }

    @Test
    @DisplayName("call with varargs should declare WasmException")
    void callWithVarargsShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("call", WasmValue[].class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "call(WasmValue...) should declare 1 exception");
      assertEquals("WasmException", exceptions[0].getSimpleName(), "Should declare WasmException");
    }

    @Test
    @DisplayName("isNull should not declare any exceptions")
    void isNullShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("isNull");
      assertEquals(0, method.getExceptionTypes().length, "isNull should not declare exceptions");
    }

    @Test
    @DisplayName("getFunction should not declare any exceptions")
    void getFunctionShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = FuncRef.class.getMethod("getFunction");
      assertEquals(
          0, method.getExceptionTypes().length, "getFunction should not declare exceptions");
    }
  }

  // ========================================================================
  // NullFuncRef Implementation Behavioral Tests
  // ========================================================================

  @Nested
  @DisplayName("NullFuncRef Implementation Behavioral Tests")
  class NullFuncRefBehavioralTests {

    @Test
    @DisplayName("nullRef should return non-null instance")
    void nullRefShouldReturnNonNullInstance() {
      FuncRef nullRef = FuncRef.nullRef();
      assertNotNull(nullRef, "nullRef should return a non-null instance");
    }

    @Test
    @DisplayName("nullRef should return same singleton instance")
    void nullRefShouldReturnSameSingletonInstance() {
      FuncRef first = FuncRef.nullRef();
      FuncRef second = FuncRef.nullRef();
      assertTrue(first == second, "nullRef should return the same singleton instance");
    }

    @Test
    @DisplayName("nullRef isNull should return true")
    void nullRefIsNullShouldReturnTrue() {
      FuncRef nullRef = FuncRef.nullRef();
      assertTrue(nullRef.isNull(), "nullRef.isNull() should return true");
    }

    @Test
    @DisplayName("nullRef getFunction should return empty Optional")
    void nullRefGetFunctionShouldReturnEmptyOptional() {
      FuncRef nullRef = FuncRef.nullRef();
      Optional<WasmFunction> function = nullRef.getFunction();
      assertNotNull(function, "getFunction should not return null");
      assertTrue(function.isEmpty(), "nullRef.getFunction() should return empty Optional");
    }

    @Test
    @DisplayName("nullRef getFunctionType should return null")
    void nullRefGetFunctionTypeShouldReturnNull() {
      FuncRef nullRef = FuncRef.nullRef();
      FunctionType type = nullRef.getFunctionType();
      assertTrue(type == null, "nullRef.getFunctionType() should return null");
    }

    @Test
    @DisplayName("nullRef call should throw IllegalStateException")
    void nullRefCallShouldThrowIllegalStateException() {
      FuncRef nullRef = FuncRef.nullRef();
      try {
        nullRef.call(List.of());
        assertTrue(false, "Expected IllegalStateException to be thrown");
      } catch (IllegalStateException e) {
        assertTrue(
            e.getMessage().contains("null"), "Exception message should mention null reference");
      } catch (WasmException e) {
        assertTrue(false, "Should throw IllegalStateException, not WasmException");
      }
    }

    @Test
    @DisplayName("nullRef isNullable should return true")
    void nullRefIsNullableShouldReturnTrue() {
      FuncRef nullRef = FuncRef.nullRef();
      assertTrue(nullRef.isNullable(), "nullRef.isNullable() should return true");
    }

    @Test
    @DisplayName("nullRef getValueType should return FUNCREF")
    void nullRefGetValueTypeShouldReturnFuncref() {
      FuncRef nullRef = FuncRef.nullRef();
      assertEquals(
          ai.tegmentum.wasmtime4j.WasmValueType.FUNCREF,
          nullRef.getValueType(),
          "nullRef.getValueType() should return FUNCREF");
    }

    @Test
    @DisplayName("nullRef isBottom should return false")
    void nullRefIsBottomShouldReturnFalse() {
      FuncRef nullRef = FuncRef.nullRef();
      assertFalse(nullRef.isBottom(), "nullRef.isBottom() should return false");
    }

    @Test
    @DisplayName("nullRef getTypeName should return funcref")
    void nullRefGetTypeNameShouldReturnFuncref() {
      FuncRef nullRef = FuncRef.nullRef();
      assertEquals("funcref", nullRef.getTypeName(), "nullRef.getTypeName() should return funcref");
    }

    @Test
    @DisplayName("nullRef toString should contain null")
    void nullRefToStringShouldContainNull() {
      FuncRef nullRef = FuncRef.nullRef();
      String str = nullRef.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("null"), "nullRef.toString() should contain 'null'");
    }

    @Test
    @DisplayName("nullRef equals with same instance should return true")
    void nullRefEqualsWithSameInstanceShouldReturnTrue() {
      FuncRef nullRef = FuncRef.nullRef();
      assertTrue(nullRef.equals(nullRef), "nullRef.equals(nullRef) should return true");
    }

    @Test
    @DisplayName("nullRef equals with another nullRef should return true")
    void nullRefEqualsWithAnotherNullRefShouldReturnTrue() {
      FuncRef first = FuncRef.nullRef();
      FuncRef second = FuncRef.nullRef();
      assertTrue(first.equals(second), "Two nullRefs should be equal");
    }

    @Test
    @DisplayName("nullRef hashCode should be consistent")
    void nullRefHashCodeShouldBeConsistent() {
      FuncRef first = FuncRef.nullRef();
      FuncRef second = FuncRef.nullRef();
      assertEquals(first.hashCode(), second.hashCode(), "Two nullRefs should have same hashCode");
    }

    @Test
    @DisplayName("nullRef isSubtypeOf FuncRef should return true")
    void nullRefIsSubtypeOfFuncRefShouldReturnTrue() {
      FuncRef nullRef = FuncRef.nullRef();
      assertTrue(nullRef.isSubtypeOf(nullRef), "nullRef should be subtype of another FuncRef");
    }

    @Test
    @DisplayName("nullRef isSubtypeOf NoFunc should return true")
    void nullRefIsSubtypeOfNoFuncShouldReturnTrue() {
      FuncRef nullRef = FuncRef.nullRef();
      NoFunc noFunc = NoFunc.INSTANCE;
      assertTrue(nullRef.isSubtypeOf(noFunc), "nullRef should be subtype of NoFunc");
    }
  }

  // ========================================================================
  // fromFunction Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("fromFunction Static Method Tests")
  class FromFunctionTests {

    @Test
    @DisplayName("fromFunction with null should throw IllegalArgumentException")
    void fromFunctionWithNullShouldThrowIllegalArgumentException() {
      try {
        FuncRef.fromFunction(null);
        assertTrue(false, "Expected IllegalArgumentException to be thrown");
      } catch (IllegalArgumentException e) {
        assertTrue(e.getMessage().contains("null"), "Exception message should mention null");
      }
    }
  }

  // ========================================================================
  // Default Method call(WasmValue...) Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method call(WasmValue...) Tests")
  class DefaultCallMethodTests {

    @Test
    @DisplayName("call with null varargs should be handled")
    void callWithNullVarargsShouldBeHandled() {
      FuncRef nullRef = FuncRef.nullRef();
      try {
        nullRef.call((WasmValue[]) null);
        assertTrue(false, "Expected exception to be thrown");
      } catch (IllegalStateException e) {
        // Expected - null ref cannot be called
        assertTrue(true);
      } catch (WasmException e) {
        assertTrue(false, "Should throw IllegalStateException, not WasmException");
      }
    }

    @Test
    @DisplayName("call with empty varargs should be handled")
    void callWithEmptyVarargsShouldBeHandled() {
      FuncRef nullRef = FuncRef.nullRef();
      try {
        nullRef.call();
        assertTrue(false, "Expected exception to be thrown");
      } catch (IllegalStateException e) {
        // Expected - null ref cannot be called
        assertTrue(true);
      } catch (WasmException e) {
        assertTrue(false, "Should throw IllegalStateException, not WasmException");
      }
    }
  }
}
