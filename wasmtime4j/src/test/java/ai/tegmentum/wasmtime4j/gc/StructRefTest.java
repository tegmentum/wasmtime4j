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
 * Tests for {@link StructRef} class.
 *
 * <p>StructRef represents a reference to a WebAssembly struct instance.
 */
@DisplayName("StructRef Tests")
class StructRefTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(Modifier.isPublic(StructRef.class.getModifiers()), "StructRef should be public");
      assertTrue(Modifier.isFinal(StructRef.class.getModifiers()), "StructRef should be final");
    }

    @Test
    @DisplayName("should implement GcRef interface")
    void shouldImplementGcRefInterface() {
      assertTrue(GcRef.class.isAssignableFrom(StructRef.class), "StructRef should implement GcRef");
    }

    @Test
    @DisplayName("should have static factory method of(StructInstance)")
    void shouldHaveStaticFactoryMethodOfStructInstance() throws NoSuchMethodException {
      final Method method = StructRef.class.getMethod("of", StructInstance.class);
      assertNotNull(method, "of(StructInstance) method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(StructRef.class, method.getReturnType(), "of should return StructRef");
    }

    @Test
    @DisplayName("should have static nullRef method")
    void shouldHaveStaticNullRefMethod() throws NoSuchMethodException {
      final Method method = StructRef.class.getMethod("nullRef");
      assertNotNull(method, "nullRef method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nullRef should be static");
      assertEquals(StructRef.class, method.getReturnType(), "nullRef should return StructRef");
    }

    @Test
    @DisplayName("should have getInstance method")
    void shouldHaveGetInstanceMethod() throws NoSuchMethodException {
      final Method method = StructRef.class.getMethod("getInstance");
      assertNotNull(method, "getInstance method should exist");
      assertEquals(
          StructInstance.class, method.getReturnType(), "getInstance should return StructInstance");
    }
  }

  @Nested
  @DisplayName("Null Reference Tests")
  class NullReferenceTests {

    @Test
    @DisplayName("nullRef should return a null reference")
    void nullRefShouldReturnNullReference() {
      final StructRef nullRef = StructRef.nullRef();
      assertNotNull(nullRef, "nullRef should not return Java null");
      assertTrue(nullRef.isNull(), "nullRef should indicate it is null");
    }

    @Test
    @DisplayName("nullRef should return STRUCT_REF type")
    void nullRefShouldReturnStructRefType() {
      final StructRef nullRef = StructRef.nullRef();
      assertEquals(
          GcReferenceType.STRUCT_REF,
          nullRef.getReferenceType(),
          "Null structref should have STRUCT_REF type");
    }

    @Test
    @DisplayName("nullRef should have instance of null")
    void nullRefShouldHaveInstanceOfNull() {
      final StructRef nullRef = StructRef.nullRef();
      assertNull(nullRef.getInstance(), "Instance should be null");
    }

    @Test
    @DisplayName("nullRef should have unique id")
    void nullRefShouldHaveUniqueId() {
      final StructRef nullRef1 = StructRef.nullRef();
      final StructRef nullRef2 = StructRef.nullRef();
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
      final Method method = StructRef.class.getMethod("toEqRef");
      assertNotNull(method, "toEqRef method should exist");
      assertEquals(EqRef.class, method.getReturnType(), "toEqRef should return EqRef");
    }

    @Test
    @DisplayName("should have toAnyRef method")
    void shouldHaveToAnyRefMethod() throws NoSuchMethodException {
      final Method method = StructRef.class.getMethod("toAnyRef");
      assertNotNull(method, "toAnyRef method should exist");
      assertEquals(AnyRef.class, method.getReturnType(), "toAnyRef should return AnyRef");
    }

    @Test
    @DisplayName("null structref should convert to null eqref")
    void nullStructRefShouldConvertToNullEqRef() {
      final StructRef nullRef = StructRef.nullRef();
      final EqRef eqRef = nullRef.toEqRef();
      assertNotNull(eqRef, "Converted EqRef should not be Java null");
      assertTrue(eqRef.isNull(), "Converted EqRef should be null reference");
    }

    @Test
    @DisplayName("null structref should convert to null anyref")
    void nullStructRefShouldConvertToNullAnyRef() {
      final StructRef nullRef = StructRef.nullRef();
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
      final StructRef nullRef1 = StructRef.nullRef();
      final StructRef nullRef2 = StructRef.nullRef();
      assertTrue(nullRef1.refEquals(nullRef2), "Two null refs should be refEquals");
    }

    @Test
    @DisplayName("refEquals with null argument should return false")
    void refEqualsWithNullArgumentShouldReturnFalse() {
      final StructRef structRef = StructRef.nullRef();
      assertFalse(structRef.refEquals(null), "refEquals with null argument should return false");
    }

    @Test
    @DisplayName("null refs should be equal")
    void nullRefsShouldBeEqual() {
      final StructRef nullRef1 = StructRef.nullRef();
      final StructRef nullRef2 = StructRef.nullRef();
      assertEquals(nullRef1, nullRef2, "Two null refs should be equal");
    }

    @Test
    @DisplayName("equals with non-StructRef should return false")
    void equalsWithNonStructRefShouldReturnFalse() {
      final StructRef structRef = StructRef.nullRef();
      assertFalse(
          structRef.equals("not a StructRef"), "equals with non-StructRef should return false");
    }

    @Test
    @DisplayName("null refs should have same hashCode")
    void nullRefsShouldHaveSameHashCode() {
      final StructRef nullRef1 = StructRef.nullRef();
      final StructRef nullRef2 = StructRef.nullRef();
      assertEquals(
          nullRef1.hashCode(), nullRef2.hashCode(), "Equal objects should have same hashCode");
    }
  }

  @Nested
  @DisplayName("Null Reference Operation Tests")
  class NullReferenceOperationTests {

    @Test
    @DisplayName("getStructType on null ref should throw IllegalStateException")
    void getStructTypeOnNullRefShouldThrowIllegalStateException() {
      final StructRef nullRef = StructRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> nullRef.getStructType(null),
          "getStructType on null ref should throw IllegalStateException");
    }

    @Test
    @DisplayName("getFieldCount on null ref should throw IllegalStateException")
    void getFieldCountOnNullRefShouldThrowIllegalStateException() {
      final StructRef nullRef = StructRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> nullRef.getFieldCount(null),
          "getFieldCount on null ref should throw IllegalStateException");
    }

    @Test
    @DisplayName("getField on null ref should throw IllegalStateException")
    void getFieldOnNullRefShouldThrowIllegalStateException() {
      final StructRef nullRef = StructRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> nullRef.getField(null, 0),
          "getField on null ref should throw IllegalStateException");
    }

    @Test
    @DisplayName("setField on null ref should throw IllegalStateException")
    void setFieldOnNullRefShouldThrowIllegalStateException() {
      final StructRef nullRef = StructRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> nullRef.setField(null, 0, null),
          "setField on null ref should throw IllegalStateException");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("null ref toString should contain 'null'")
    void nullRefToStringShouldContainNull() {
      final StructRef nullRef = StructRef.nullRef();
      final String str = nullRef.toString();
      assertTrue(str.contains("null"), "toString of null ref should contain 'null': " + str);
    }

    @Test
    @DisplayName("toString should contain 'StructRef'")
    void toStringShouldContainStructRef() {
      final StructRef nullRef = StructRef.nullRef();
      final String str = nullRef.toString();
      assertTrue(str.contains("StructRef"), "toString should contain 'StructRef': " + str);
    }
  }

  @Nested
  @DisplayName("Factory Method Null Argument Tests")
  class FactoryMethodNullArgumentTests {

    @Test
    @DisplayName("of(StructInstance) should throw NPE for null argument")
    void ofStructInstanceShouldThrowNpeForNullArgument() {
      assertThrows(
          NullPointerException.class,
          () -> StructRef.of(null),
          "of(StructInstance) should throw NPE for null");
    }
  }
}
