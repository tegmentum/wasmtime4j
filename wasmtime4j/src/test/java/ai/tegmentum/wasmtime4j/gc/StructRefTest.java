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

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StructRef} class.
 *
 * <p>StructRef represents a WebAssembly GC structref reference type, wrapping a StructInstance.
 */
@DisplayName("StructRef Tests")
class StructRefTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(StructRef.class.getModifiers()), "StructRef should be final");
    }

    @Test
    @DisplayName("should implement GcRef interface")
    void shouldImplementGcRef() {
      assertTrue(
          GcRef.class.isAssignableFrom(StructRef.class), "StructRef should implement GcRef");
    }
  }

  @Nested
  @DisplayName("Null Reference Tests")
  class NullReferenceTests {

    @Test
    @DisplayName("nullRef should create null reference")
    void nullRefShouldCreateNullReference() {
      final StructRef ref = StructRef.nullRef();
      assertNotNull(ref, "Null ref instance should not be null");
      assertTrue(ref.isNull(), "nullRef should report isNull as true");
    }

    @Test
    @DisplayName("nullRef should have null instance")
    void nullRefShouldHaveNullInstance() {
      final StructRef ref = StructRef.nullRef();
      assertNull(ref.getInstance(), "Instance of null ref should be null");
    }

    @Test
    @DisplayName("nullRef should have STRUCT_REF reference type")
    void nullRefShouldHaveStructRefReferenceType() {
      final StructRef ref = StructRef.nullRef();
      assertEquals(
          GcReferenceType.STRUCT_REF,
          ref.getReferenceType(),
          "Reference type should be STRUCT_REF");
    }

    @Test
    @DisplayName("nullRef getStructType should throw IllegalStateException")
    void nullRefGetStructTypeShouldThrowIse() {
      final StructRef ref = StructRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> ref.getStructType(null),
          "getStructType on null ref should throw IllegalStateException");
    }

    @Test
    @DisplayName("nullRef getFieldCount should throw IllegalStateException")
    void nullRefGetFieldCountShouldThrowIse() {
      final StructRef ref = StructRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> ref.getFieldCount(null),
          "getFieldCount on null ref should throw IllegalStateException");
    }

    @Test
    @DisplayName("nullRef getField should throw IllegalStateException")
    void nullRefGetFieldShouldThrowIse() {
      final StructRef ref = StructRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> ref.getField(null, 0),
          "getField on null ref should throw IllegalStateException");
    }

    @Test
    @DisplayName("nullRef setField should throw IllegalStateException")
    void nullRefSetFieldShouldThrowIse() {
      final StructRef ref = StructRef.nullRef();
      assertThrows(
          IllegalStateException.class,
          () -> ref.setField(null, 0, GcValue.i32(1)),
          "setField on null ref should throw IllegalStateException");
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
          () -> StructRef.of(null),
          "of(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Upcast Tests")
  class UpcastTests {

    @Test
    @DisplayName("null StructRef toEqRef should produce null EqRef")
    void nullStructRefToEqRefShouldProduceNullEqRef() {
      final StructRef structRef = StructRef.nullRef();
      final EqRef eqRef = structRef.toEqRef();
      assertNotNull(eqRef, "toEqRef should not return null");
      assertTrue(eqRef.isNull(), "Upcasted null StructRef should produce null EqRef");
    }

    @Test
    @DisplayName("null StructRef toAnyRef should produce null AnyRef")
    void nullStructRefToAnyRefShouldProduceNullAnyRef() {
      final StructRef structRef = StructRef.nullRef();
      final AnyRef anyRef = structRef.toAnyRef();
      assertNotNull(anyRef, "toAnyRef should not return null");
      assertTrue(anyRef.isNull(), "Upcasted null StructRef should produce null AnyRef");
    }
  }

  @Nested
  @DisplayName("RefEquals Tests")
  class RefEqualsTests {

    @Test
    @DisplayName("two null refs should be ref-equal")
    void twoNullRefsShouldBeRefEqual() {
      final StructRef ref1 = StructRef.nullRef();
      final StructRef ref2 = StructRef.nullRef();
      assertTrue(ref1.refEquals(ref2), "Two null refs should be ref-equal");
    }

    @Test
    @DisplayName("null ref should not be ref-equal to null argument")
    void nullRefShouldNotBeRefEqualToNullArgument() {
      final StructRef ref = StructRef.nullRef();
      assertFalse(ref.refEquals(null), "Null ref should not be ref-equal to null argument");
    }
  }

  @Nested
  @DisplayName("ID Tests")
  class IdTests {

    @Test
    @DisplayName("each StructRef should have a unique ID")
    void eachStructRefShouldHaveUniqueId() {
      final StructRef ref1 = StructRef.nullRef();
      final StructRef ref2 = StructRef.nullRef();
      assertNotEquals(
          ref1.getId(), ref2.getId(), "Each StructRef should have a unique ID");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("two null refs should be equal")
    void twoNullRefsShouldBeEqual() {
      final StructRef ref1 = StructRef.nullRef();
      final StructRef ref2 = StructRef.nullRef();
      assertEquals(ref1, ref2, "Two null refs should be equal");
    }

    @Test
    @DisplayName("same instance should be equal to itself")
    void sameInstanceShouldBeEqualToItself() {
      final StructRef ref = StructRef.nullRef();
      assertEquals(ref, ref, "Same instance should be equal to itself");
    }

    @Test
    @DisplayName("StructRef should not be equal to non-StructRef")
    void structRefShouldNotBeEqualToNonStructRef() {
      final StructRef ref = StructRef.nullRef();
      assertNotEquals(ref, "not a StructRef", "StructRef should not equal a String");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("two null refs should have same hash code")
    void twoNullRefsShouldHaveSameHashCode() {
      final StructRef ref1 = StructRef.nullRef();
      final StructRef ref2 = StructRef.nullRef();
      assertEquals(
          ref1.hashCode(), ref2.hashCode(), "Two null refs should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("null ref toString should contain null")
    void nullRefToStringShouldContainNull() {
      final StructRef ref = StructRef.nullRef();
      final String str = ref.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("null"), "Null ref toString should contain 'null'");
    }

    @Test
    @DisplayName("toString should contain StructRef")
    void toStringShouldContainStructRef() {
      final StructRef ref = StructRef.nullRef();
      final String str = ref.toString();
      assertTrue(str.contains("StructRef"), "toString should contain 'StructRef'");
    }
  }
}
