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
 * Tests for {@link ArrayRef} class.
 *
 * <p>ArrayRef represents a reference to a WebAssembly array instance.
 */
@DisplayName("ArrayRef Tests")
class ArrayRefTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(Modifier.isPublic(ArrayRef.class.getModifiers()), "ArrayRef should be public");
      assertTrue(Modifier.isFinal(ArrayRef.class.getModifiers()), "ArrayRef should be final");
    }

    @Test
    @DisplayName("should implement GcRef interface")
    void shouldImplementGcRefInterface() {
      assertTrue(GcRef.class.isAssignableFrom(ArrayRef.class), "ArrayRef should implement GcRef");
    }

    @Test
    @DisplayName("should have static factory method of(ArrayInstance)")
    void shouldHaveStaticFactoryMethodOfArrayInstance() throws NoSuchMethodException {
      final Method method = ArrayRef.class.getMethod("of", ArrayInstance.class);
      assertNotNull(method, "of(ArrayInstance) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(ArrayRef.class, method.getReturnType(), "of should return ArrayRef");
    }

    @Test
    @DisplayName("should have static nullRef method")
    void shouldHaveStaticNullRefMethod() throws NoSuchMethodException {
      final Method method = ArrayRef.class.getMethod("nullRef");
      assertNotNull(method, "nullRef method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullRef should be static");
      assertEquals(ArrayRef.class, method.getReturnType(), "nullRef should return ArrayRef");
    }

    @Test
    @DisplayName("should have getInstance method")
    void shouldHaveGetInstanceMethod() throws NoSuchMethodException {
      final Method method = ArrayRef.class.getMethod("getInstance");
      assertNotNull(method, "getInstance method should exist");
      assertEquals(
          ArrayInstance.class, method.getReturnType(), "getInstance should return ArrayInstance");
    }
  }

  @Nested
  @DisplayName("Null Reference Tests")
  class NullReferenceTests {

    @Test
    @DisplayName("nullRef should return a null reference")
    void nullRefShouldReturnNullReference() {
      final ArrayRef nullRef = ArrayRef.nullRef();
      assertNotNull(nullRef, "nullRef should not return Java null");
      assertTrue(nullRef.isNull(), "nullRef should indicate it is null");
    }

    @Test
    @DisplayName("nullRef should return ARRAY_REF type")
    void nullRefShouldReturnArrayRefType() {
      final ArrayRef nullRef = ArrayRef.nullRef();
      assertEquals(
          GcReferenceType.ARRAY_REF,
          nullRef.getReferenceType(),
          "Null arrayref should have ARRAY_REF type");
    }

    @Test
    @DisplayName("nullRef should have instance of null")
    void nullRefShouldHaveInstanceOfNull() {
      final ArrayRef nullRef = ArrayRef.nullRef();
      assertNull(nullRef.getInstance(), "Instance should be null");
    }

    @Test
    @DisplayName("nullRef should have unique id")
    void nullRefShouldHaveUniqueId() {
      final ArrayRef nullRef1 = ArrayRef.nullRef();
      final ArrayRef nullRef2 = ArrayRef.nullRef();
      assertNotEquals(
          nullRef1.getId(),
          nullRef2.getId(),
          "Each nullRef call should return a new instance with unique ID");
    }
  }

  @Nested
  @DisplayName("Conversion Methods Tests")
  class ConversionMethodsTests {

    @Test
    @DisplayName("should have toEqRef method")
    void shouldHaveToEqRefMethod() throws NoSuchMethodException {
      final Method method = ArrayRef.class.getMethod("toEqRef");
      assertNotNull(method, "toEqRef method should exist");
      assertEquals(EqRef.class, method.getReturnType(), "toEqRef should return EqRef");
    }

    @Test
    @DisplayName("should have toAnyRef method")
    void shouldHaveToAnyRefMethod() throws NoSuchMethodException {
      final Method method = ArrayRef.class.getMethod("toAnyRef");
      assertNotNull(method, "toAnyRef method should exist");
      assertEquals(AnyRef.class, method.getReturnType(), "toAnyRef should return AnyRef");
    }

    @Test
    @DisplayName("null arrayref should convert to null eqref")
    void nullArrayRefShouldConvertToNullEqRef() {
      final ArrayRef nullRef = ArrayRef.nullRef();
      final EqRef eqRef = nullRef.toEqRef();
      assertNotNull(eqRef, "Converted EqRef should not be Java null");
      assertTrue(eqRef.isNull(), "Converted EqRef should be null reference");
    }

    @Test
    @DisplayName("null arrayref should convert to null anyref")
    void nullArrayRefShouldConvertToNullAnyRef() {
      final ArrayRef nullRef = ArrayRef.nullRef();
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
      final ArrayRef nullRef1 = ArrayRef.nullRef();
      final ArrayRef nullRef2 = ArrayRef.nullRef();
      assertTrue(nullRef1.refEquals(nullRef2), "Two null refs should be refEquals");
    }

    @Test
    @DisplayName("refEquals with null argument should return false")
    void refEqualsWithNullArgumentShouldReturnFalse() {
      final ArrayRef arrayRef = ArrayRef.nullRef();
      assertFalse(arrayRef.refEquals(null), "refEquals with null argument should return false");
    }

    @Test
    @DisplayName("null refs should be equal")
    void nullRefsShouldBeEqual() {
      final ArrayRef nullRef1 = ArrayRef.nullRef();
      final ArrayRef nullRef2 = ArrayRef.nullRef();
      assertEquals(nullRef1, nullRef2, "Two null refs should be equal");
    }

    @Test
    @DisplayName("equals with non-ArrayRef should return false")
    void equalsWithNonArrayRefShouldReturnFalse() {
      final ArrayRef arrayRef = ArrayRef.nullRef();
      assertFalse(
          arrayRef.equals("not an ArrayRef"), "equals with non-ArrayRef should return false");
    }

    @Test
    @DisplayName("null refs should have same hashCode")
    void nullRefsShouldHaveSameHashCode() {
      final ArrayRef nullRef1 = ArrayRef.nullRef();
      final ArrayRef nullRef2 = ArrayRef.nullRef();
      assertEquals(
          nullRef1.hashCode(), nullRef2.hashCode(), "Equal objects should have same hashCode");
    }
  }

  @Nested
  @DisplayName("Null Reference Operation Tests")
  class NullReferenceOperationTests {

    @Test
    @DisplayName("getArrayType on null ref should throw IllegalStateException")
    void getArrayTypeOnNullRefShouldThrowIllegalStateException() {
      final ArrayRef nullRef = ArrayRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> nullRef.getArrayType(null),
          "getArrayType on null ref should throw IllegalStateException");
    }

    @Test
    @DisplayName("getLength on null ref should throw IllegalStateException")
    void getLengthOnNullRefShouldThrowIllegalStateException() {
      final ArrayRef nullRef = ArrayRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> nullRef.getLength(null),
          "getLength on null ref should throw IllegalStateException");
    }

    @Test
    @DisplayName("getElement on null ref should throw IllegalStateException")
    void getElementOnNullRefShouldThrowIllegalStateException() {
      final ArrayRef nullRef = ArrayRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> nullRef.getElement(null, 0),
          "getElement on null ref should throw IllegalStateException");
    }

    @Test
    @DisplayName("setElement on null ref should throw IllegalStateException")
    void setElementOnNullRefShouldThrowIllegalStateException() {
      final ArrayRef nullRef = ArrayRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> nullRef.setElement(null, 0, null),
          "setElement on null ref should throw IllegalStateException");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("null ref toString should contain 'null'")
    void nullRefToStringShouldContainNull() {
      final ArrayRef nullRef = ArrayRef.nullRef();
      final String str = nullRef.toString();
      assertTrue(str.contains("null"), "toString of null ref should contain 'null': " + str);
    }

    @Test
    @DisplayName("toString should contain 'ArrayRef'")
    void toStringShouldContainArrayRef() {
      final ArrayRef nullRef = ArrayRef.nullRef();
      final String str = nullRef.toString();
      assertTrue(str.contains("ArrayRef"), "toString should contain 'ArrayRef': " + str);
    }
  }

  @Nested
  @DisplayName("Factory Method Null Argument Tests")
  class FactoryMethodNullArgumentTests {

    @Test
    @DisplayName("of(ArrayInstance) should throw NPE for null argument")
    void ofArrayInstanceShouldThrowNpeForNullArgument() {
      assertThrows(
          NullPointerException.class,
          () -> ArrayRef.of(null),
          "of(ArrayInstance) should throw NPE for null");
    }
  }
}
