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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link GcRef} interface.
 *
 * <p>Verifies the contract of isNull(), getReferenceType(), and getId() using anonymous
 * implementations. Tests cover null references, non-null references, all GcReferenceType values,
 * and various ID edge cases.
 */
@DisplayName("GcRef Interface Tests")
class GcRefTest {

  /**
   * Creates a GcRef implementation with the given parameters.
   *
   * @param isNull whether this is a null reference
   * @param referenceType the GC reference type
   * @param id the unique reference identifier
   * @return a GcRef instance
   */
  private static GcRef gcRefOf(
      final boolean isNull, final GcReferenceType referenceType, final long id) {
    return new GcRef() {
      @Override
      public boolean isNull() {
        return isNull;
      }

      @Override
      public GcReferenceType getReferenceType() {
        return referenceType;
      }

      @Override
      public long getId() {
        return id;
      }
    };
  }

  @Nested
  @DisplayName("isNull Tests")
  class IsNullTests {

    @Test
    @DisplayName("null reference should return true for isNull")
    void nullReferenceShouldReturnTrue() {
      final GcRef ref = gcRefOf(true, GcReferenceType.NULL_REF, 0L);
      assertTrue(ref.isNull(), "A null reference should return true for isNull()");
    }

    @Test
    @DisplayName("non-null reference should return false for isNull")
    void nonNullReferenceShouldReturnFalse() {
      final GcRef ref = gcRefOf(false, GcReferenceType.ANY_REF, 42L);
      assertFalse(ref.isNull(), "A non-null reference should return false for isNull()");
    }

    @Test
    @DisplayName("null reference with ANY_REF type should be possible")
    void nullReferenceWithAnyRefTypeShouldBePossible() {
      final GcRef ref = gcRefOf(true, GcReferenceType.ANY_REF, 0L);
      assertTrue(ref.isNull(), "A null ANY_REF reference should return true for isNull()");
      assertEquals(
          GcReferenceType.ANY_REF,
          ref.getReferenceType(),
          "Reference type should be ANY_REF even when null");
    }
  }

  @Nested
  @DisplayName("getReferenceType Tests")
  class GetReferenceTypeTests {

    @Test
    @DisplayName("should return ANY_REF for anyref references")
    void shouldReturnAnyRefForAnyrefReferences() {
      final GcRef ref = gcRefOf(false, GcReferenceType.ANY_REF, 1L);
      assertEquals(
          GcReferenceType.ANY_REF,
          ref.getReferenceType(),
          "getReferenceType() should return ANY_REF");
    }

    @Test
    @DisplayName("should return EQ_REF for eqref references")
    void shouldReturnEqRefForEqrefReferences() {
      final GcRef ref = gcRefOf(false, GcReferenceType.EQ_REF, 2L);
      assertEquals(
          GcReferenceType.EQ_REF,
          ref.getReferenceType(),
          "getReferenceType() should return EQ_REF");
    }

    @Test
    @DisplayName("should return I31_REF for i31ref references")
    void shouldReturnI31RefForI31refReferences() {
      final GcRef ref = gcRefOf(false, GcReferenceType.I31_REF, 3L);
      assertEquals(
          GcReferenceType.I31_REF,
          ref.getReferenceType(),
          "getReferenceType() should return I31_REF");
    }

    @Test
    @DisplayName("should return STRUCT_REF for structref references")
    void shouldReturnStructRefForStructrefReferences() {
      final GcRef ref = gcRefOf(false, GcReferenceType.STRUCT_REF, 4L);
      assertEquals(
          GcReferenceType.STRUCT_REF,
          ref.getReferenceType(),
          "getReferenceType() should return STRUCT_REF");
    }

    @Test
    @DisplayName("should return ARRAY_REF for arrayref references")
    void shouldReturnArrayRefForArrayrefReferences() {
      final GcRef ref = gcRefOf(false, GcReferenceType.ARRAY_REF, 5L);
      assertEquals(
          GcReferenceType.ARRAY_REF,
          ref.getReferenceType(),
          "getReferenceType() should return ARRAY_REF");
    }

    @Test
    @DisplayName("should return EXN_REF for exnref references")
    void shouldReturnExnRefForExnrefReferences() {
      final GcRef ref = gcRefOf(false, GcReferenceType.EXN_REF, 6L);
      assertEquals(
          GcReferenceType.EXN_REF,
          ref.getReferenceType(),
          "getReferenceType() should return EXN_REF");
    }

    @Test
    @DisplayName("should return NULL_REF for nullref references")
    void shouldReturnNullRefForNullrefReferences() {
      final GcRef ref = gcRefOf(true, GcReferenceType.NULL_REF, 0L);
      assertEquals(
          GcReferenceType.NULL_REF,
          ref.getReferenceType(),
          "getReferenceType() should return NULL_REF");
    }

    @Test
    @DisplayName("should return NULL_FUNC_REF for nullfuncref references")
    void shouldReturnNullFuncRefForNullfuncrefReferences() {
      final GcRef ref = gcRefOf(true, GcReferenceType.NULL_FUNC_REF, 0L);
      assertEquals(
          GcReferenceType.NULL_FUNC_REF,
          ref.getReferenceType(),
          "getReferenceType() should return NULL_FUNC_REF");
    }

    @Test
    @DisplayName("should return NULL_EXTERN_REF for nullexternref references")
    void shouldReturnNullExternRefForNullexternrefReferences() {
      final GcRef ref = gcRefOf(true, GcReferenceType.NULL_EXTERN_REF, 0L);
      assertEquals(
          GcReferenceType.NULL_EXTERN_REF,
          ref.getReferenceType(),
          "getReferenceType() should return NULL_EXTERN_REF");
    }

    @Test
    @DisplayName("should support all GcReferenceType values")
    void shouldSupportAllGcReferenceTypeValues() {
      for (final GcReferenceType type : GcReferenceType.values()) {
        final GcRef ref = gcRefOf(false, type, 100L);
        assertNotNull(
            ref.getReferenceType(),
            "getReferenceType() should not be null for type " + type.name());
        assertEquals(
            type, ref.getReferenceType(), "getReferenceType() should return " + type.name());
      }
    }
  }

  @Nested
  @DisplayName("getId Tests")
  class GetIdTests {

    @Test
    @DisplayName("should return zero ID")
    void shouldReturnZeroId() {
      final GcRef ref = gcRefOf(false, GcReferenceType.ANY_REF, 0L);
      assertEquals(0L, ref.getId(), "getId() should return 0 for a zero-ID reference");
    }

    @Test
    @DisplayName("should return positive ID")
    void shouldReturnPositiveId() {
      final GcRef ref = gcRefOf(false, GcReferenceType.STRUCT_REF, 42L);
      assertEquals(42L, ref.getId(), "getId() should return 42 for reference with ID 42");
    }

    @Test
    @DisplayName("should return large positive ID")
    void shouldReturnLargePositiveId() {
      final long largeId = Long.MAX_VALUE;
      final GcRef ref = gcRefOf(false, GcReferenceType.ARRAY_REF, largeId);
      assertEquals(
          largeId, ref.getId(), "getId() should return Long.MAX_VALUE for reference with max ID");
    }

    @Test
    @DisplayName("should return negative ID for unsigned interpretation")
    void shouldReturnNegativeIdForUnsignedInterpretation() {
      // Native pointers may appear as negative longs in Java (unsigned 64-bit values)
      final long negativeId = -1L;
      final GcRef ref = gcRefOf(false, GcReferenceType.EQ_REF, negativeId);
      assertEquals(
          negativeId,
          ref.getId(),
          "getId() should handle negative IDs representing unsigned native pointers");
    }

    @Test
    @DisplayName("should return Long.MIN_VALUE ID")
    void shouldReturnMinValueId() {
      final GcRef ref = gcRefOf(false, GcReferenceType.I31_REF, Long.MIN_VALUE);
      assertEquals(
          Long.MIN_VALUE,
          ref.getId(),
          "getId() should return Long.MIN_VALUE for reference with min ID");
    }
  }

  @Nested
  @DisplayName("Combined Behavior Tests")
  class CombinedBehaviorTests {

    @Test
    @DisplayName("null reference should still have a valid reference type and ID")
    void nullReferenceShouldHaveValidTypeAndId() {
      final GcRef ref = gcRefOf(true, GcReferenceType.NULL_REF, 0L);
      assertTrue(ref.isNull(), "Reference should be null");
      assertEquals(
          GcReferenceType.NULL_REF,
          ref.getReferenceType(),
          "Null reference should have NULL_REF type");
      assertEquals(0L, ref.getId(), "Null reference should have ID 0");
    }

    @Test
    @DisplayName("non-null struct reference should have correct type and non-zero ID")
    void nonNullStructRefShouldHaveCorrectTypeAndId() {
      final GcRef ref = gcRefOf(false, GcReferenceType.STRUCT_REF, 999L);
      assertFalse(ref.isNull(), "Reference should not be null");
      assertEquals(
          GcReferenceType.STRUCT_REF,
          ref.getReferenceType(),
          "Reference type should be STRUCT_REF");
      assertEquals(999L, ref.getId(), "ID should be 999");
    }

    @Test
    @DisplayName("two references with same properties should behave identically")
    void twoReferencesWithSamePropertiesShouldBehaveIdentically() {
      final GcRef ref1 = gcRefOf(false, GcReferenceType.ARRAY_REF, 123L);
      final GcRef ref2 = gcRefOf(false, GcReferenceType.ARRAY_REF, 123L);

      assertEquals(
          ref1.isNull(), ref2.isNull(), "Both references should have same isNull() result");
      assertEquals(
          ref1.getReferenceType(),
          ref2.getReferenceType(),
          "Both references should have same getReferenceType()");
      assertEquals(ref1.getId(), ref2.getId(), "Both references should have same getId()");
    }

    @Test
    @DisplayName("different reference types with same ID should be distinguishable")
    void differentTypesWithSameIdShouldBeDistinguishable() {
      final long sharedId = 50L;
      final GcRef anyRef = gcRefOf(false, GcReferenceType.ANY_REF, sharedId);
      final GcRef eqRef = gcRefOf(false, GcReferenceType.EQ_REF, sharedId);
      final GcRef structRef = gcRefOf(false, GcReferenceType.STRUCT_REF, sharedId);

      assertEquals(anyRef.getId(), eqRef.getId(), "IDs should be equal");
      assertEquals(eqRef.getId(), structRef.getId(), "IDs should be equal");

      assertEquals(
          GcReferenceType.ANY_REF, anyRef.getReferenceType(), "First ref should be ANY_REF");
      assertEquals(GcReferenceType.EQ_REF, eqRef.getReferenceType(), "Second ref should be EQ_REF");
      assertEquals(
          GcReferenceType.STRUCT_REF,
          structRef.getReferenceType(),
          "Third ref should be STRUCT_REF");
    }
  }
}
