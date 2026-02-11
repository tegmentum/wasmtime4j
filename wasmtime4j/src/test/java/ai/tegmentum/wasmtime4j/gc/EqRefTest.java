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
 * Tests for {@link EqRef} class.
 *
 * <p>EqRef represents the WebAssembly eqref type supporting structural equality via ref.eq.
 */
@DisplayName("EqRef Tests")
class EqRefTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(EqRef.class.getModifiers()), "EqRef should be final");
    }

    @Test
    @DisplayName("should implement GcRef interface")
    void shouldImplementGcRef() {
      assertTrue(GcRef.class.isAssignableFrom(EqRef.class), "EqRef should implement GcRef");
    }
  }

  @Nested
  @DisplayName("Null Reference Tests")
  class NullReferenceTests {

    @Test
    @DisplayName("nullRef should create null reference")
    void nullRefShouldCreateNullReference() {
      final EqRef ref = EqRef.nullRef();
      assertNotNull(ref, "Null ref instance should not be null");
      assertTrue(ref.isNull(), "nullRef should report isNull as true");
    }

    @Test
    @DisplayName("nullRef should have null underlying")
    void nullRefShouldHaveNullUnderlying() {
      final EqRef ref = EqRef.nullRef();
      assertNull(ref.getUnderlying(), "Underlying value of null ref should be null");
    }

    @Test
    @DisplayName("nullRef should have EQ_REF reference type")
    void nullRefShouldHaveEqRefReferenceType() {
      final EqRef ref = EqRef.nullRef();
      assertEquals(
          GcReferenceType.EQ_REF, ref.getReferenceType(), "Reference type should be EQ_REF");
    }

    @Test
    @DisplayName("nullRef should not be i31")
    void nullRefShouldNotBeI31() {
      final EqRef ref = EqRef.nullRef();
      assertFalse(ref.isI31(), "Null ref should not be i31");
    }

    @Test
    @DisplayName("nullRef should not be struct")
    void nullRefShouldNotBeStruct() {
      final EqRef ref = EqRef.nullRef();
      assertFalse(ref.isStruct(), "Null ref should not be struct");
    }

    @Test
    @DisplayName("nullRef should not be array")
    void nullRefShouldNotBeArray() {
      final EqRef ref = EqRef.nullRef();
      assertFalse(ref.isArray(), "Null ref should not be array");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("of with null GcObject should throw NullPointerException")
    void ofWithNullGcObjectShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> EqRef.of((GcObject) null),
          "of(null GcObject) should throw NullPointerException");
    }

    @Test
    @DisplayName("of with null StructRef should throw NullPointerException")
    void ofWithNullStructRefShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> EqRef.of((StructRef) null),
          "of(null StructRef) should throw NullPointerException");
    }

    @Test
    @DisplayName("of with null ArrayRef should throw NullPointerException")
    void ofWithNullArrayRefShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> EqRef.of((ArrayRef) null),
          "of(null ArrayRef) should throw NullPointerException");
    }

    @Test
    @DisplayName("of with null StructInstance should throw NullPointerException")
    void ofWithNullStructInstanceShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> EqRef.of((StructInstance) null),
          "of(null StructInstance) should throw NullPointerException");
    }

    @Test
    @DisplayName("of with null ArrayInstance should throw NullPointerException")
    void ofWithNullArrayInstanceShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> EqRef.of((ArrayInstance) null),
          "of(null ArrayInstance) should throw NullPointerException");
    }

    @Test
    @DisplayName("of with null I31Instance should throw NullPointerException")
    void ofWithNullI31InstanceShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> EqRef.of((I31Instance) null),
          "of(null I31Instance) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Upcast Tests")
  class UpcastTests {

    @Test
    @DisplayName("null EqRef toAnyRef should produce null AnyRef")
    void nullEqRefToAnyRefShouldProduceNullAnyRef() {
      final EqRef eqRef = EqRef.nullRef();
      final AnyRef anyRef = eqRef.toAnyRef();
      assertNotNull(anyRef, "toAnyRef should not return null");
      assertTrue(anyRef.isNull(), "Upcasted null EqRef should produce null AnyRef");
    }
  }

  @Nested
  @DisplayName("RefEquals Tests")
  class RefEqualsTests {

    @Test
    @DisplayName("two null refs should be ref-equal")
    void twoNullRefsShouldBeRefEqual() {
      final EqRef ref1 = EqRef.nullRef();
      final EqRef ref2 = EqRef.nullRef();
      assertTrue(ref1.refEquals(ref2), "Two null refs should be ref-equal");
    }

    @Test
    @DisplayName("null ref should not be ref-equal to null argument")
    void nullRefShouldNotBeRefEqualToNullArgument() {
      final EqRef ref = EqRef.nullRef();
      assertFalse(ref.refEquals(null), "Null ref should not be ref-equal to null argument");
    }
  }

  @Nested
  @DisplayName("ID Tests")
  class IdTests {

    @Test
    @DisplayName("each EqRef should have a unique ID")
    void eachEqRefShouldHaveUniqueId() {
      final EqRef ref1 = EqRef.nullRef();
      final EqRef ref2 = EqRef.nullRef();
      assertNotEquals(ref1.getId(), ref2.getId(), "Each EqRef should have a unique ID");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("two null refs should be equal")
    void twoNullRefsShouldBeEqual() {
      final EqRef ref1 = EqRef.nullRef();
      final EqRef ref2 = EqRef.nullRef();
      assertEquals(ref1, ref2, "Two null refs should be equal");
    }

    @Test
    @DisplayName("same instance should be equal to itself")
    void sameInstanceShouldBeEqualToItself() {
      final EqRef ref = EqRef.nullRef();
      assertEquals(ref, ref, "Same instance should be equal to itself");
    }

    @Test
    @DisplayName("EqRef should not be equal to non-EqRef")
    void eqRefShouldNotBeEqualToNonEqRef() {
      final EqRef ref = EqRef.nullRef();
      assertNotEquals(ref, "not an EqRef", "EqRef should not equal a String");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("two null refs should have same hash code")
    void twoNullRefsShouldHaveSameHashCode() {
      final EqRef ref1 = EqRef.nullRef();
      final EqRef ref2 = EqRef.nullRef();
      assertEquals(ref1.hashCode(), ref2.hashCode(), "Two null refs should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("null ref toString should contain null")
    void nullRefToStringShouldContainNull() {
      final EqRef ref = EqRef.nullRef();
      final String str = ref.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("null"), "Null ref toString should contain 'null'");
    }

    @Test
    @DisplayName("toString should contain EqRef")
    void toStringShouldContainEqRef() {
      final EqRef ref = EqRef.nullRef();
      final String str = ref.toString();
      assertTrue(str.contains("EqRef"), "toString should contain 'EqRef'");
    }
  }
}
