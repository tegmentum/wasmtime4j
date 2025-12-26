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
 * Tests for {@link AnyRef} class.
 *
 * <p>AnyRef represents the WebAssembly anyref type - the top type of the reference hierarchy.
 */
@DisplayName("AnyRef Tests")
class AnyRefTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(Modifier.isPublic(AnyRef.class.getModifiers()), "AnyRef should be public");
      assertTrue(Modifier.isFinal(AnyRef.class.getModifiers()), "AnyRef should be final");
    }

    @Test
    @DisplayName("should implement GcRef interface")
    void shouldImplementGcRefInterface() {
      assertTrue(GcRef.class.isAssignableFrom(AnyRef.class), "AnyRef should implement GcRef");
    }

    @Test
    @DisplayName("should have static factory method of(GcObject)")
    void shouldHaveStaticFactoryMethodOfGcObject() throws NoSuchMethodException {
      final Method method = AnyRef.class.getMethod("of", GcObject.class);
      assertNotNull(method, "of(GcObject) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(AnyRef.class, method.getReturnType(), "of should return AnyRef");
    }

    @Test
    @DisplayName("should have static factory method of(EqRef)")
    void shouldHaveStaticFactoryMethodOfEqRef() throws NoSuchMethodException {
      final Method method = AnyRef.class.getMethod("of", EqRef.class);
      assertNotNull(method, "of(EqRef) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
    }

    @Test
    @DisplayName("should have static factory method of(StructRef)")
    void shouldHaveStaticFactoryMethodOfStructRef() throws NoSuchMethodException {
      final Method method = AnyRef.class.getMethod("of", StructRef.class);
      assertNotNull(method, "of(StructRef) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
    }

    @Test
    @DisplayName("should have static factory method of(ArrayRef)")
    void shouldHaveStaticFactoryMethodOfArrayRef() throws NoSuchMethodException {
      final Method method = AnyRef.class.getMethod("of", ArrayRef.class);
      assertNotNull(method, "of(ArrayRef) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
    }

    @Test
    @DisplayName("should have static nullRef method")
    void shouldHaveStaticNullRefMethod() throws NoSuchMethodException {
      final Method method = AnyRef.class.getMethod("nullRef");
      assertNotNull(method, "nullRef method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullRef should be static");
      assertEquals(AnyRef.class, method.getReturnType(), "nullRef should return AnyRef");
    }
  }

  @Nested
  @DisplayName("Null Reference Tests")
  class NullReferenceTests {

    @Test
    @DisplayName("nullRef should return a null reference")
    void nullRefShouldReturnNullReference() {
      final AnyRef nullRef = AnyRef.nullRef();
      assertNotNull(nullRef, "nullRef should not return Java null");
      assertTrue(nullRef.isNull(), "nullRef should indicate it is null");
    }

    @Test
    @DisplayName("nullRef should return ANY_REF type")
    void nullRefShouldReturnAnyRefType() {
      final AnyRef nullRef = AnyRef.nullRef();
      assertEquals(
          GcReferenceType.ANY_REF,
          nullRef.getReferenceType(),
          "Null anyref should have ANY_REF type");
    }

    @Test
    @DisplayName("nullRef should have underlying value of null")
    void nullRefShouldHaveUnderlyingValueOfNull() {
      final AnyRef nullRef = AnyRef.nullRef();
      assertNull(nullRef.getUnderlying(), "Underlying value should be null");
    }

    @Test
    @DisplayName("nullRef should have unique id")
    void nullRefShouldHaveUniqueId() {
      final AnyRef nullRef1 = AnyRef.nullRef();
      final AnyRef nullRef2 = AnyRef.nullRef();
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
      final Method method = AnyRef.class.getMethod("isI31");
      assertNotNull(method, "isI31 method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isI31 should return boolean");
    }

    @Test
    @DisplayName("should have isStruct method")
    void shouldHaveIsStructMethod() throws NoSuchMethodException {
      final Method method = AnyRef.class.getMethod("isStruct");
      assertNotNull(method, "isStruct method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isStruct should return boolean");
    }

    @Test
    @DisplayName("should have isArray method")
    void shouldHaveIsArrayMethod() throws NoSuchMethodException {
      final Method method = AnyRef.class.getMethod("isArray");
      assertNotNull(method, "isArray method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isArray should return boolean");
    }

    @Test
    @DisplayName("should have isEq method")
    void shouldHaveIsEqMethod() throws NoSuchMethodException {
      final Method method = AnyRef.class.getMethod("isEq");
      assertNotNull(method, "isEq method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isEq should return boolean");
    }

    @Test
    @DisplayName("null ref should return false for all type checks")
    void nullRefShouldReturnFalseForAllTypeChecks() {
      final AnyRef nullRef = AnyRef.nullRef();
      assertFalse(nullRef.isI31(), "Null ref should not be I31");
      assertFalse(nullRef.isStruct(), "Null ref should not be struct");
      assertFalse(nullRef.isArray(), "Null ref should not be array");
      assertFalse(nullRef.isEq(), "Null ref should not be eq");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("null refs should be refEquals")
    void nullRefsShouldBeRefEquals() {
      final AnyRef nullRef1 = AnyRef.nullRef();
      final AnyRef nullRef2 = AnyRef.nullRef();
      assertTrue(nullRef1.refEquals(nullRef2), "Two null refs should be refEquals");
    }

    @Test
    @DisplayName("refEquals with null argument should return false")
    void refEqualsWithNullArgumentShouldReturnFalse() {
      final AnyRef anyRef = AnyRef.nullRef();
      assertFalse(anyRef.refEquals(null), "refEquals with null argument should return false");
    }

    @Test
    @DisplayName("null refs should be equal")
    void nullRefsShouldBeEqual() {
      final AnyRef nullRef1 = AnyRef.nullRef();
      final AnyRef nullRef2 = AnyRef.nullRef();
      assertEquals(nullRef1, nullRef2, "Two null refs should be equal");
    }

    @Test
    @DisplayName("equals with non-AnyRef should return false")
    void equalsWithNonAnyRefShouldReturnFalse() {
      final AnyRef anyRef = AnyRef.nullRef();
      assertFalse(anyRef.equals("not an AnyRef"), "equals with non-AnyRef should return false");
    }

    @Test
    @DisplayName("null refs should have same hashCode")
    void nullRefsShouldHaveSameHashCode() {
      final AnyRef nullRef1 = AnyRef.nullRef();
      final AnyRef nullRef2 = AnyRef.nullRef();
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
      final AnyRef nullRef = AnyRef.nullRef();
      final String str = nullRef.toString();
      assertTrue(str.contains("null"), "toString of null ref should contain 'null': " + str);
    }

    @Test
    @DisplayName("toString should contain 'AnyRef'")
    void toStringShouldContainAnyRef() {
      final AnyRef nullRef = AnyRef.nullRef();
      final String str = nullRef.toString();
      assertTrue(str.contains("AnyRef"), "toString should contain 'AnyRef': " + str);
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
          () -> AnyRef.of((GcObject) null),
          "of(GcObject) should throw NPE for null");
    }

    @Test
    @DisplayName("of(EqRef) should throw NPE for null argument")
    void ofEqRefShouldThrowNpeForNullArgument() {
      assertThrows(
          NullPointerException.class,
          () -> AnyRef.of((EqRef) null),
          "of(EqRef) should throw NPE for null");
    }

    @Test
    @DisplayName("of(StructRef) should throw NPE for null argument")
    void ofStructRefShouldThrowNpeForNullArgument() {
      assertThrows(
          NullPointerException.class,
          () -> AnyRef.of((StructRef) null),
          "of(StructRef) should throw NPE for null");
    }

    @Test
    @DisplayName("of(ArrayRef) should throw NPE for null argument")
    void ofArrayRefShouldThrowNpeForNullArgument() {
      assertThrows(
          NullPointerException.class,
          () -> AnyRef.of((ArrayRef) null),
          "of(ArrayRef) should throw NPE for null");
    }
  }
}
