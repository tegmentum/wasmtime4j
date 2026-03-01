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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ArrayRef} class.
 *
 * <p>ArrayRef represents a WebAssembly GC arrayref reference type, wrapping an ArrayInstance.
 */
@DisplayName("ArrayRef Tests")
class ArrayRefTest {

  @Nested
  @DisplayName("Null Reference Tests")
  class NullReferenceTests {

    @Test
    @DisplayName("nullRef should create null reference")
    void nullRefShouldCreateNullReference() {
      final ArrayRef ref = ArrayRef.nullRef();
      assertNotNull(ref, "Null ref instance should not be null");
      assertTrue(ref.isNull(), "nullRef should report isNull as true");
    }

    @Test
    @DisplayName("nullRef should have null instance")
    void nullRefShouldHaveNullInstance() {
      final ArrayRef ref = ArrayRef.nullRef();
      assertNull(ref.getInstance(), "Instance of null ref should be null");
    }

    @Test
    @DisplayName("nullRef should have ARRAY_REF reference type")
    void nullRefShouldHaveArrayRefReferenceType() {
      final ArrayRef ref = ArrayRef.nullRef();
      assertEquals(
          GcReferenceType.ARRAY_REF, ref.getReferenceType(), "Reference type should be ARRAY_REF");
    }

    @Test
    @DisplayName("nullRef getArrayType should throw IllegalStateException")
    void nullRefGetArrayTypeShouldThrowIse() {
      final ArrayRef ref = ArrayRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> ref.getArrayType(null),
          "getArrayType on null ref should throw IllegalStateException");
    }

    @Test
    @DisplayName("nullRef getLength should throw IllegalStateException")
    void nullRefGetLengthShouldThrowIse() {
      final ArrayRef ref = ArrayRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> ref.getLength(null),
          "getLength on null ref should throw IllegalStateException");
    }

    @Test
    @DisplayName("nullRef getElement should throw IllegalStateException")
    void nullRefGetElementShouldThrowIse() {
      final ArrayRef ref = ArrayRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> ref.getElement(null, 0),
          "getElement on null ref should throw IllegalStateException");
    }

    @Test
    @DisplayName("nullRef setElement should throw IllegalStateException")
    void nullRefSetElementShouldThrowIse() {
      final ArrayRef ref = ArrayRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> ref.setElement(null, 0, GcValue.i32(1)),
          "setElement on null ref should throw IllegalStateException");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("of with null should throw NullPointerException")
    void ofWithNullShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> ArrayRef.of(null),
          "of(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Upcast Tests")
  class UpcastTests {

    @Test
    @DisplayName("null ArrayRef toEqRef should produce null EqRef")
    void nullArrayRefToEqRefShouldProduceNullEqRef() {
      final ArrayRef arrayRef = ArrayRef.nullRef();
      final EqRef eqRef = arrayRef.toEqRef();
      assertNotNull(eqRef, "toEqRef should not return null");
      assertTrue(eqRef.isNull(), "Upcasted null ArrayRef should produce null EqRef");
    }

    @Test
    @DisplayName("null ArrayRef toAnyRef should produce null AnyRef")
    void nullArrayRefToAnyRefShouldProduceNullAnyRef() {
      final ArrayRef arrayRef = ArrayRef.nullRef();
      final AnyRef anyRef = arrayRef.toAnyRef();
      assertNotNull(anyRef, "toAnyRef should not return null");
      assertTrue(anyRef.isNull(), "Upcasted null ArrayRef should produce null AnyRef");
    }
  }

  @Nested
  @DisplayName("RefEquals Tests")
  class RefEqualsTests {

    @Test
    @DisplayName("two null refs should be ref-equal")
    void twoNullRefsShouldBeRefEqual() {
      final ArrayRef ref1 = ArrayRef.nullRef();
      final ArrayRef ref2 = ArrayRef.nullRef();
      assertTrue(ref1.refEquals(ref2), "Two null refs should be ref-equal");
    }

    @Test
    @DisplayName("null ref should not be ref-equal to null argument")
    void nullRefShouldNotBeRefEqualToNullArgument() {
      final ArrayRef ref = ArrayRef.nullRef();
      assertFalse(ref.refEquals(null), "Null ref should not be ref-equal to null argument");
    }
  }

  @Nested
  @DisplayName("ID Tests")
  class IdTests {

    @Test
    @DisplayName("each ArrayRef should have a unique ID")
    void eachArrayRefShouldHaveUniqueId() {
      final ArrayRef ref1 = ArrayRef.nullRef();
      final ArrayRef ref2 = ArrayRef.nullRef();
      assertNotEquals(ref1.getId(), ref2.getId(), "Each ArrayRef should have a unique ID");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("two null refs should be equal")
    void twoNullRefsShouldBeEqual() {
      final ArrayRef ref1 = ArrayRef.nullRef();
      final ArrayRef ref2 = ArrayRef.nullRef();
      assertEquals(ref1, ref2, "Two null refs should be equal");
    }

    @Test
    @DisplayName("same instance should be equal to itself")
    void sameInstanceShouldBeEqualToItself() {
      final ArrayRef ref = ArrayRef.nullRef();
      assertEquals(ref, ref, "Same instance should be equal to itself");
    }

    @Test
    @DisplayName("ArrayRef should not be equal to non-ArrayRef")
    void arrayRefShouldNotBeEqualToNonArrayRef() {
      final ArrayRef ref = ArrayRef.nullRef();
      assertNotEquals(ref, "not an ArrayRef", "ArrayRef should not equal a String");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("two null refs should have same hash code")
    void twoNullRefsShouldHaveSameHashCode() {
      final ArrayRef ref1 = ArrayRef.nullRef();
      final ArrayRef ref2 = ArrayRef.nullRef();
      assertEquals(ref1.hashCode(), ref2.hashCode(), "Two null refs should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("null ref toString should contain null")
    void nullRefToStringShouldContainNull() {
      final ArrayRef ref = ArrayRef.nullRef();
      final String str = ref.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("null"), "Null ref toString should contain 'null'");
    }

    @Test
    @DisplayName("toString should contain ArrayRef")
    void toStringShouldContainArrayRef() {
      final ArrayRef ref = ArrayRef.nullRef();
      final String str = ref.toString();
      assertTrue(str.contains("ArrayRef"), "toString should contain 'ArrayRef'");
    }
  }
}
