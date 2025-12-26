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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EqRef} class.
 *
 * <p>EqRef represents the WebAssembly eqref type - references that support equality testing.
 */
@DisplayName("EqRef Tests")
class EqRefTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(Modifier.isPublic(EqRef.class.getModifiers()), "EqRef should be public");
      assertTrue(Modifier.isFinal(EqRef.class.getModifiers()), "EqRef should be final");
    }

    @Test
    @DisplayName("should implement GcRef interface")
    void shouldImplementGcRefInterface() {
      assertTrue(GcRef.class.isAssignableFrom(EqRef.class), "EqRef should implement GcRef");
    }

    @Test
    @DisplayName("should have static factory method of(GcObject)")
    void shouldHaveStaticFactoryMethodOfGcObject() throws NoSuchMethodException {
      final Method method = EqRef.class.getMethod("of", GcObject.class);
      assertNotNull(method, "of(GcObject) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(EqRef.class, method.getReturnType(), "of should return EqRef");
    }

    @Test
    @DisplayName("should have static factory method of(StructRef)")
    void shouldHaveStaticFactoryMethodOfStructRef() throws NoSuchMethodException {
      final Method method = EqRef.class.getMethod("of", StructRef.class);
      assertNotNull(method, "of(StructRef) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
    }

    @Test
    @DisplayName("should have static factory method of(ArrayRef)")
    void shouldHaveStaticFactoryMethodOfArrayRef() throws NoSuchMethodException {
      final Method method = EqRef.class.getMethod("of", ArrayRef.class);
      assertNotNull(method, "of(ArrayRef) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
    }

    @Test
    @DisplayName("should have static factory method of(StructInstance)")
    void shouldHaveStaticFactoryMethodOfStructInstance() throws NoSuchMethodException {
      final Method method = EqRef.class.getMethod("of", StructInstance.class);
      assertNotNull(method, "of(StructInstance) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
    }

    @Test
    @DisplayName("should have static factory method of(ArrayInstance)")
    void shouldHaveStaticFactoryMethodOfArrayInstance() throws NoSuchMethodException {
      final Method method = EqRef.class.getMethod("of", ArrayInstance.class);
      assertNotNull(method, "of(ArrayInstance) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
    }

    @Test
    @DisplayName("should have static factory method of(I31Instance)")
    void shouldHaveStaticFactoryMethodOfI31Instance() throws NoSuchMethodException {
      final Method method = EqRef.class.getMethod("of", I31Instance.class);
      assertNotNull(method, "of(I31Instance) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
    }

    @Test
    @DisplayName("should have static nullRef method")
    void shouldHaveStaticNullRefMethod() throws NoSuchMethodException {
      final Method method = EqRef.class.getMethod("nullRef");
      assertNotNull(method, "nullRef method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullRef should be static");
      assertEquals(EqRef.class, method.getReturnType(), "nullRef should return EqRef");
    }
  }

  @Nested
  @DisplayName("Null Reference Tests")
  class NullReferenceTests {

    @Test
    @DisplayName("nullRef should return a null reference")
    void nullRefShouldReturnNullReference() {
      final EqRef nullRef = EqRef.nullRef();
      assertNotNull(nullRef, "nullRef should not return Java null");
      assertTrue(nullRef.isNull(), "nullRef should indicate it is null");
    }

    @Test
    @DisplayName("nullRef should return EQ_REF type")
    void nullRefShouldReturnEqRefType() {
      final EqRef nullRef = EqRef.nullRef();
      assertEquals(
          GcReferenceType.EQ_REF, nullRef.getReferenceType(), "Null eqref should have EQ_REF type");
    }

    @Test
    @DisplayName("nullRef should have underlying value of null")
    void nullRefShouldHaveUnderlyingValueOfNull() {
      final EqRef nullRef = EqRef.nullRef();
      assertNull(nullRef.getUnderlying(), "Underlying value should be null");
    }

    @Test
    @DisplayName("nullRef should have unique id")
    void nullRefShouldHaveUniqueId() {
      final EqRef nullRef1 = EqRef.nullRef();
      final EqRef nullRef2 = EqRef.nullRef();
      assertNotEquals(
          nullRef1.getId(),
          nullRef2.getId(),
          "Each nullRef call should return a new instance with unique ID");
    }
  }

  @Nested
  @DisplayName("Type Check Methods Tests")
  class TypeCheckMethodsTests {

    @Test
    @DisplayName("should have isI31 method")
    void shouldHaveIsI31Method() throws NoSuchMethodException {
      final Method method = EqRef.class.getMethod("isI31");
      assertNotNull(method, "isI31 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isI31 should return boolean");
    }

    @Test
    @DisplayName("should have isStruct method")
    void shouldHaveIsStructMethod() throws NoSuchMethodException {
      final Method method = EqRef.class.getMethod("isStruct");
      assertNotNull(method, "isStruct method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isStruct should return boolean");
    }

    @Test
    @DisplayName("should have isArray method")
    void shouldHaveIsArrayMethod() throws NoSuchMethodException {
      final Method method = EqRef.class.getMethod("isArray");
      assertNotNull(method, "isArray method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isArray should return boolean");
    }

    @Test
    @DisplayName("null ref should return false for all type checks")
    void nullRefShouldReturnFalseForAllTypeChecks() {
      final EqRef nullRef = EqRef.nullRef();
      assertFalse(nullRef.isI31(), "Null ref should not be I31");
      assertFalse(nullRef.isStruct(), "Null ref should not be struct");
      assertFalse(nullRef.isArray(), "Null ref should not be array");
    }
  }

  @Nested
  @DisplayName("Conversion Methods Tests")
  class ConversionMethodsTests {

    @Test
    @DisplayName("should have toAnyRef method")
    void shouldHaveToAnyRefMethod() throws NoSuchMethodException {
      final Method method = EqRef.class.getMethod("toAnyRef");
      assertNotNull(method, "toAnyRef method should exist");
      assertEquals(AnyRef.class, method.getReturnType(), "toAnyRef should return AnyRef");
    }

    @Test
    @DisplayName("null eqref should convert to null anyref")
    void nullEqRefShouldConvertToNullAnyRef() {
      final EqRef nullRef = EqRef.nullRef();
      final AnyRef anyRef = nullRef.toAnyRef();
      assertNotNull(anyRef, "Converted AnyRef should not be Java null");
      assertTrue(anyRef.isNull(), "Converted AnyRef should be null reference");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("null refs should be refEquals")
    void nullRefsShouldBeRefEquals() {
      final EqRef nullRef1 = EqRef.nullRef();
      final EqRef nullRef2 = EqRef.nullRef();
      assertTrue(nullRef1.refEquals(nullRef2), "Two null refs should be refEquals");
    }

    @Test
    @DisplayName("refEquals with null argument should return false")
    void refEqualsWithNullArgumentShouldReturnFalse() {
      final EqRef eqRef = EqRef.nullRef();
      assertFalse(eqRef.refEquals(null), "refEquals with null argument should return false");
    }

    @Test
    @DisplayName("null refs should be equal")
    void nullRefsShouldBeEqual() {
      final EqRef nullRef1 = EqRef.nullRef();
      final EqRef nullRef2 = EqRef.nullRef();
      assertEquals(nullRef1, nullRef2, "Two null refs should be equal");
    }

    @Test
    @DisplayName("equals with non-EqRef should return false")
    void equalsWithNonEqRefShouldReturnFalse() {
      final EqRef eqRef = EqRef.nullRef();
      assertFalse(eqRef.equals("not an EqRef"), "equals with non-EqRef should return false");
    }

    @Test
    @DisplayName("null refs should have same hashCode")
    void nullRefsShouldHaveSameHashCode() {
      final EqRef nullRef1 = EqRef.nullRef();
      final EqRef nullRef2 = EqRef.nullRef();
      assertEquals(
          nullRef1.hashCode(), nullRef2.hashCode(), "Equal objects should have same hashCode");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("null ref toString should contain 'null'")
    void nullRefToStringShouldContainNull() {
      final EqRef nullRef = EqRef.nullRef();
      final String str = nullRef.toString();
      assertTrue(str.contains("null"), "toString of null ref should contain 'null': " + str);
    }

    @Test
    @DisplayName("toString should contain 'EqRef'")
    void toStringShouldContainEqRef() {
      final EqRef nullRef = EqRef.nullRef();
      final String str = nullRef.toString();
      assertTrue(str.contains("EqRef"), "toString should contain 'EqRef': " + str);
    }
  }

  @Nested
  @DisplayName("Factory Method Null Argument Tests")
  class FactoryMethodNullArgumentTests {

    @Test
    @DisplayName("of(GcObject) should throw NPE for null argument")
    void ofGcObjectShouldThrowNpeForNullArgument() {
      assertThrows(
          NullPointerException.class,
          () -> EqRef.of((GcObject) null),
          "of(GcObject) should throw NPE for null");
    }

    @Test
    @DisplayName("of(StructRef) should throw NPE for null argument")
    void ofStructRefShouldThrowNpeForNullArgument() {
      assertThrows(
          NullPointerException.class,
          () -> EqRef.of((StructRef) null),
          "of(StructRef) should throw NPE for null");
    }

    @Test
    @DisplayName("of(ArrayRef) should throw NPE for null argument")
    void ofArrayRefShouldThrowNpeForNullArgument() {
      assertThrows(
          NullPointerException.class,
          () -> EqRef.of((ArrayRef) null),
          "of(ArrayRef) should throw NPE for null");
    }

    @Test
    @DisplayName("of(StructInstance) should throw NPE for null argument")
    void ofStructInstanceShouldThrowNpeForNullArgument() {
      assertThrows(
          NullPointerException.class,
          () -> EqRef.of((StructInstance) null),
          "of(StructInstance) should throw NPE for null");
    }

    @Test
    @DisplayName("of(ArrayInstance) should throw NPE for null argument")
    void ofArrayInstanceShouldThrowNpeForNullArgument() {
      assertThrows(
          NullPointerException.class,
          () -> EqRef.of((ArrayInstance) null),
          "of(ArrayInstance) should throw NPE for null");
    }

    @Test
    @DisplayName("of(I31Instance) should throw NPE for null argument")
    void ofI31InstanceShouldThrowNpeForNullArgument() {
      assertThrows(
          NullPointerException.class,
          () -> EqRef.of((I31Instance) null),
          "of(I31Instance) should throw NPE for null");
    }
  }
}
