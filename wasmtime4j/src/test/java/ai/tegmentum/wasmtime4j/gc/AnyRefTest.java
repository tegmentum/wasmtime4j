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

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AnyRef} class.
 *
 * <p>AnyRef represents the WebAssembly anyref top reference type in the GC proposal hierarchy.
 */
@DisplayName("AnyRef Tests")
class AnyRefTest {

  @Nested
  @DisplayName("Null Reference Tests")
  class NullReferenceTests {

    @Test
    @DisplayName("nullRef should create null reference")
    void nullRefShouldCreateNullReference() {
      final AnyRef ref = AnyRef.nullRef();
      assertNotNull(ref, "Null ref instance should not be null");
      assertTrue(ref.isNull(), "nullRef should report isNull as true");
    }

    @Test
    @DisplayName("nullRef should have null underlying")
    void nullRefShouldHaveNullUnderlying() {
      final AnyRef ref = AnyRef.nullRef();
      assertNull(ref.getUnderlying(), "Underlying value of null ref should be null");
    }

    @Test
    @DisplayName("nullRef should have ANY_REF reference type")
    void nullRefShouldHaveAnyRefReferenceType() {
      final AnyRef ref = AnyRef.nullRef();
      assertEquals(
          GcReferenceType.ANY_REF, ref.getReferenceType(), "Reference type should be ANY_REF");
    }

    @Test
    @DisplayName("nullRef should not be i31")
    void nullRefShouldNotBeI31() {
      final AnyRef ref = AnyRef.nullRef();
      assertFalse(ref.isI31(), "Null ref should not be i31");
    }

    @Test
    @DisplayName("nullRef should not be struct")
    void nullRefShouldNotBeStruct() {
      final AnyRef ref = AnyRef.nullRef();
      assertFalse(ref.isStruct(), "Null ref should not be struct");
    }

    @Test
    @DisplayName("nullRef should not be array")
    void nullRefShouldNotBeArray() {
      final AnyRef ref = AnyRef.nullRef();
      assertFalse(ref.isArray(), "Null ref should not be array");
    }

    @Test
    @DisplayName("nullRef should not be eq")
    void nullRefShouldNotBeEq() {
      final AnyRef ref = AnyRef.nullRef();
      assertFalse(ref.isEq(), "Null ref should not be eq");
    }

    @Test
    @DisplayName("nullRef asEq should return empty")
    void nullRefAsEqShouldReturnEmpty() {
      final AnyRef ref = AnyRef.nullRef();
      final Optional<EqRef> eqRef = ref.asEq();
      assertFalse(eqRef.isPresent(), "Null ref asEq should return empty optional");
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
          () -> AnyRef.of((GcObject) null),
          "of(null GcObject) should throw NullPointerException");
    }

    @Test
    @DisplayName("of with null EqRef should throw NullPointerException")
    void ofWithNullEqRefShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> AnyRef.of((EqRef) null),
          "of(null EqRef) should throw NullPointerException");
    }

    @Test
    @DisplayName("of with null StructRef should throw NullPointerException")
    void ofWithNullStructRefShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> AnyRef.of((StructRef) null),
          "of(null StructRef) should throw NullPointerException");
    }

    @Test
    @DisplayName("of with null ArrayRef should throw NullPointerException")
    void ofWithNullArrayRefShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> AnyRef.of((ArrayRef) null),
          "of(null ArrayRef) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("ID Tests")
  class IdTests {

    @Test
    @DisplayName("each AnyRef should have a unique ID")
    void eachAnyRefShouldHaveUniqueId() {
      final AnyRef ref1 = AnyRef.nullRef();
      final AnyRef ref2 = AnyRef.nullRef();
      assertNotEquals(ref1.getId(), ref2.getId(), "Each AnyRef should have a unique ID");
    }

    @Test
    @DisplayName("ID should be positive")
    void idShouldBePositive() {
      final AnyRef ref = AnyRef.nullRef();
      assertTrue(ref.getId() > 0, "ID should be positive");
    }
  }

  @Nested
  @DisplayName("RefEquals Tests")
  class RefEqualsTests {

    @Test
    @DisplayName("two null refs should be ref-equal")
    void twoNullRefsShouldBeRefEqual() {
      final AnyRef ref1 = AnyRef.nullRef();
      final AnyRef ref2 = AnyRef.nullRef();
      assertTrue(ref1.refEquals(ref2), "Two null refs should be ref-equal");
    }

    @Test
    @DisplayName("null ref should not be ref-equal to null argument")
    void nullRefShouldNotBeRefEqualToNullArgument() {
      final AnyRef ref = AnyRef.nullRef();
      assertFalse(ref.refEquals(null), "Null ref should not be ref-equal to null argument");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("two null refs should be equal")
    void twoNullRefsShouldBeEqual() {
      final AnyRef ref1 = AnyRef.nullRef();
      final AnyRef ref2 = AnyRef.nullRef();
      assertEquals(ref1, ref2, "Two null refs should be equal");
    }

    @Test
    @DisplayName("same instance should be equal to itself")
    void sameInstanceShouldBeEqualToItself() {
      final AnyRef ref = AnyRef.nullRef();
      assertEquals(ref, ref, "Same instance should be equal to itself");
    }

    @Test
    @DisplayName("AnyRef should not be equal to non-AnyRef")
    void anyRefShouldNotBeEqualToNonAnyRef() {
      final AnyRef ref = AnyRef.nullRef();
      assertNotEquals(ref, "not an AnyRef", "AnyRef should not equal a String");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("two null refs should have same hash code")
    void twoNullRefsShouldHaveSameHashCode() {
      final AnyRef ref1 = AnyRef.nullRef();
      final AnyRef ref2 = AnyRef.nullRef();
      assertEquals(ref1.hashCode(), ref2.hashCode(), "Two null refs should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("null ref toString should contain null")
    void nullRefToStringShouldContainNull() {
      final AnyRef ref = AnyRef.nullRef();
      final String str = ref.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("null"), "Null ref toString should contain 'null'");
    }

    @Test
    @DisplayName("toString should contain AnyRef")
    void toStringShouldContainAnyRef() {
      final AnyRef ref = AnyRef.nullRef();
      final String str = ref.toString();
      assertTrue(str.contains("AnyRef"), "toString should contain 'AnyRef'");
    }
  }
}
