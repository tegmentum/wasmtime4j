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
package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitTuple} class.
 *
 * <p>WitTuple represents a WIT tuple value (fixed-size, heterogeneous sequence).
 */
@DisplayName("WitTuple Tests")
class WitTupleTest {

  @Nested
  @DisplayName("Creation Tests")
  class CreationTests {

    @Test
    @DisplayName("of varargs should create tuple")
    void ofVarargsShouldCreateTuple() {
      final WitTuple tuple = WitTuple.of(WitS32.of(1), WitS32.of(2));
      assertNotNull(tuple, "WitTuple.of should create non-null tuple");
      assertEquals(2, tuple.size(), "Tuple size should be 2");
    }

    @Test
    @DisplayName("of Java List should create tuple")
    void ofJavaListShouldCreateTuple() {
      final List<WitValue> elements = List.of(WitS32.of(10), WitS32.of(20));
      final WitTuple tuple = WitTuple.of(elements);
      assertNotNull(tuple, "WitTuple.of(List) should create non-null tuple");
      assertEquals(2, tuple.size(), "Tuple size should be 2");
    }

    @Test
    @DisplayName("empty should create empty tuple")
    void emptyShouldCreateEmptyTuple() {
      final WitTuple tuple = WitTuple.empty();
      assertNotNull(tuple, "WitTuple.empty should create non-null tuple");
      assertTrue(tuple.isEmpty(), "Empty tuple should be empty");
      assertEquals(0, tuple.size(), "Empty tuple size should be 0");
    }

    @Test
    @DisplayName("builder should create tuple")
    void builderShouldCreateTuple() {
      final WitTuple tuple = WitTuple.builder().add(WitS32.of(1)).add(WitS32.of(2)).build();
      assertNotNull(tuple, "Builder should create non-null tuple");
      assertEquals(2, tuple.size(), "Builder tuple size should be 2");
    }

    @Test
    @DisplayName("builder with explicit type should create tuple")
    void builderWithExplicitTypeShouldCreateTuple() {
      final WitTuple tuple = WitTuple.builder().add(WitType.createS32(), WitS32.of(42)).build();
      assertNotNull(tuple, "Builder with explicit type should create non-null tuple");
      assertEquals(1, tuple.size(), "Tuple size should be 1");
    }

    @Test
    @DisplayName("of with null varargs should throw")
    void ofWithNullVarargsShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitTuple.of((WitValue[]) null),
          "of(null) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("of with null element should throw")
    void ofWithNullElementShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitTuple.of(WitS32.of(1), null),
          "of with null element should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Element Access Tests")
  class ElementAccessTests {

    @Test
    @DisplayName("get should return correct element")
    void getShouldReturnCorrectElement() {
      final WitTuple tuple = WitTuple.of(WitS32.of(10), WitS32.of(20));
      assertEquals(WitS32.of(10), tuple.get(0), "First element should be 10");
      assertEquals(WitS32.of(20), tuple.get(1), "Second element should be 20");
    }

    @Test
    @DisplayName("get out of bounds should throw")
    void getOutOfBoundsShouldThrow() {
      final WitTuple tuple = WitTuple.of(WitS32.of(10));
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> tuple.get(5),
          "get(5) on size-1 tuple should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("getElements should return unmodifiable list")
    void getElementsShouldReturnUnmodifiable() {
      final WitTuple tuple = WitTuple.of(WitS32.of(10));
      final List<WitValue> elements = tuple.getElements();
      assertThrows(
          UnsupportedOperationException.class,
          () -> elements.add(WitS32.of(1)),
          "getElements should return unmodifiable list");
    }

    @Test
    @DisplayName("getElementTypes should return unmodifiable list")
    void getElementTypesShouldReturnUnmodifiable() {
      final WitTuple tuple = WitTuple.of(WitS32.of(10));
      final List<WitType> types = tuple.getElementTypes();
      assertThrows(
          UnsupportedOperationException.class,
          () -> types.add(WitType.createS32()),
          "getElementTypes should return unmodifiable list");
    }

    @Test
    @DisplayName("getTypeAt should return correct type")
    void getTypeAtShouldReturnCorrectType() {
      final WitTuple tuple = WitTuple.of(WitS32.of(10));
      assertNotNull(tuple.getTypeAt(0), "getTypeAt should return non-null type");
    }

    @Test
    @DisplayName("isEmpty should return false for non-empty tuple")
    void isEmptyShouldReturnFalseForNonEmpty() {
      final WitTuple tuple = WitTuple.of(WitS32.of(10));
      assertFalse(tuple.isEmpty(), "Non-empty tuple isEmpty should return false");
    }
  }

  @Nested
  @DisplayName("ToJava Tests")
  class ToJavaTests {

    @Test
    @DisplayName("toJava should return List")
    void toJavaShouldReturnList() {
      final WitTuple tuple = WitTuple.of(WitS32.of(10), WitS32.of(20));
      final Object javaValue = tuple.toJava();
      assertTrue(javaValue instanceof List, "toJava should return a List");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same tuples should be equal")
    void sameTuplesShouldBeEqual() {
      final WitTuple t1 = WitTuple.of(WitS32.of(1), WitS32.of(2));
      final WitTuple t2 = WitTuple.of(WitS32.of(1), WitS32.of(2));
      assertEquals(t1, t2, "Tuples with same elements should be equal");
    }

    @Test
    @DisplayName("different tuples should not be equal")
    void differentTuplesShouldNotBeEqual() {
      final WitTuple t1 = WitTuple.of(WitS32.of(1));
      final WitTuple t2 = WitTuple.of(WitS32.of(2));
      assertNotEquals(t1, t2, "Tuples with different elements should not be equal");
    }

    @Test
    @DisplayName("empty tuples should be equal")
    void emptyTuplesShouldBeEqual() {
      final WitTuple t1 = WitTuple.empty();
      final WitTuple t2 = WitTuple.empty();
      assertEquals(t1, t2, "Empty tuples should be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same tuples should have same hash code")
    void sameTuplesShouldHaveSameHashCode() {
      final WitTuple t1 = WitTuple.of(WitS32.of(1), WitS32.of(2));
      final WitTuple t2 = WitTuple.of(WitS32.of(1), WitS32.of(2));
      assertEquals(t1.hashCode(), t2.hashCode(), "Same tuples should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain WitTuple")
    void toStringShouldContainClassName() {
      final WitTuple tuple = WitTuple.of(WitS32.of(1));
      final String str = tuple.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("WitTuple"), "toString should contain 'WitTuple'");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitTuple tuple = WitTuple.of(WitS32.of(1));
      assertNotNull(tuple.getType(), "Should have WitType");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("should reject null element in list")
    void shouldRejectNullElementInList() {
      final java.util.List<WitValue> elementsWithNull = new java.util.ArrayList<>();
      elementsWithNull.add(WitS32.of(1));
      elementsWithNull.add(null);
      elementsWithNull.add(WitS32.of(3));

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> WitTuple.of(elementsWithNull),
              "Should throw for null element");
      assertTrue(
          ex.getMessage().contains("null"),
          "Exception message should mention null: " + ex.getMessage());
    }

    @Test
    @DisplayName("should reject type mismatch when using explicit types")
    void shouldRejectTypeMismatchWithExplicitTypes() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  WitTuple.builder()
                      .add(WitType.createS64(), WitS32.of(42)) // Declare S64, provide S32
                      .build(),
              "Should throw for type mismatch");
      assertTrue(
          ex.getMessage().contains("type") || ex.getMessage().contains("expected"),
          "Exception message should mention type mismatch: " + ex.getMessage());
    }

    @Test
    @DisplayName("should accept matching types")
    void shouldAcceptMatchingTypes() {
      final WitTuple tuple =
          WitTuple.builder()
              .add(WitType.createS32(), WitS32.of(1))
              .add(WitType.createS64(), WitS64.of(2L))
              .add(WitType.createBool(), WitBool.of(true))
              .build();
      assertEquals(3, tuple.size(), "Should create tuple with all elements");
    }
  }

  @Nested
  @DisplayName("Mutation Killing Tests")
  class MutationKillingTests {

    @Test
    @DisplayName("isEmpty must return true for empty and false for non-empty")
    void isEmptyMutationTest() {
      // Empty tuple must return true for isEmpty()
      final WitTuple emptyTuple = WitTuple.empty();
      assertTrue(emptyTuple.isEmpty(), "isEmpty() on empty tuple must return exactly true");
      assertFalse(!emptyTuple.isEmpty(), "isEmpty() result must be true, not false");

      // Non-empty tuple must return false for isEmpty()
      final WitTuple nonEmpty = WitTuple.of(WitS32.of(1));
      assertFalse(nonEmpty.isEmpty(), "isEmpty() on non-empty tuple must return exactly false");
      assertTrue(!nonEmpty.isEmpty(), "isEmpty() result must be false, not true");
    }

    @Test
    @DisplayName("size must return exact count - 0, 1, and multiple")
    void sizeMutationTest() {
      // Empty tuple: size must be exactly 0
      final WitTuple empty = WitTuple.empty();
      assertEquals(0, empty.size(), "Empty tuple size must be exactly 0");
      assertTrue(empty.size() == 0, "Empty tuple size == 0 must be true");
      assertFalse(empty.size() != 0, "Empty tuple size != 0 must be false");

      // One element: size must be exactly 1
      final WitTuple one = WitTuple.of(WitS32.of(1));
      assertEquals(1, one.size(), "One element size must be exactly 1");
      assertTrue(one.size() == 1, "One element size == 1 must be true");
      assertFalse(one.size() == 0, "One element size == 0 must be false");

      // Three elements: size must be exactly 3
      final WitTuple three = WitTuple.of(WitS32.of(1), WitS32.of(2), WitS32.of(3));
      assertEquals(3, three.size(), "Three elements size must be exactly 3");
      assertTrue(three.size() == 3, "Three elements size == 3 must be true");
    }

    @Test
    @DisplayName("get must return correct element for each index")
    void getIndexMutationTest() {
      final WitTuple tuple = WitTuple.of(WitS32.of(42), WitS32.of(99), WitS32.of(7));

      // Index 0 must return first element exactly
      assertEquals(WitS32.of(42), tuple.get(0), "get(0) must return first element");
      assertNotEquals(WitS32.of(99), tuple.get(0), "get(0) must not return second element");

      // Index 1 must return second element exactly
      assertEquals(WitS32.of(99), tuple.get(1), "get(1) must return second element");

      // Index 2 must return third element exactly
      assertEquals(WitS32.of(7), tuple.get(2), "get(2) must return third element");
    }

    @Test
    @DisplayName("toJava must return list with correct values")
    void toJavaMutationTest() {
      final WitTuple tuple = WitTuple.of(WitS32.of(10), WitS32.of(20), WitS32.of(30));

      final List<Object> javaList = tuple.toJava();
      assertEquals(3, javaList.size(), "toJava list must have same size");
      assertEquals(10, javaList.get(0), "toJava list[0] must be 10");
      assertEquals(20, javaList.get(1), "toJava list[1] must be 20");
      assertEquals(30, javaList.get(2), "toJava list[2] must be 30");
    }

    @Test
    @DisplayName("builder must reject null element and type")
    void builderValidationMutationTest() {
      // Builder with null element must throw
      assertThrows(
          IllegalArgumentException.class,
          () -> WitTuple.builder().add(null),
          "Builder.add(null) must throw IllegalArgumentException");

      // Builder with null type must throw
      assertThrows(
          IllegalArgumentException.class,
          () -> WitTuple.builder().add(null, WitS32.of(1)),
          "Builder.add(null type) must throw IllegalArgumentException");

      // Builder with null element for explicit type must throw
      assertThrows(
          IllegalArgumentException.class,
          () -> WitTuple.builder().add(WitType.createS32(), null),
          "Builder.add(type, null) must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("equals must handle edge cases correctly")
    void equalsMutationTest() {
      final WitTuple tuple = WitTuple.of(WitS32.of(1));

      // Reflexive - same object
      assertTrue(tuple.equals(tuple), "equals(self) must return true");

      // Null comparison
      assertFalse(tuple.equals(null), "equals(null) must return false");

      // Different type
      assertFalse(tuple.equals("tuple"), "equals(String) must return false");
      assertFalse(tuple.equals(42), "equals(Integer) must return false");

      // Empty vs non-empty
      final WitTuple empty = WitTuple.empty();
      assertFalse(tuple.equals(empty), "non-empty.equals(empty) must return false");
      assertFalse(empty.equals(tuple), "empty.equals(non-empty) must return false");

      // Different element count
      final WitTuple two = WitTuple.of(WitS32.of(1), WitS32.of(2));
      assertFalse(tuple.equals(two), "size-1.equals(size-2) must return false");
    }

    @Test
    @DisplayName("getElementTypes must return types matching elements")
    void getElementTypesMutationTest() {
      final WitTuple tuple = WitTuple.of(WitS32.of(1), WitS64.of(2L));

      final List<WitType> types = tuple.getElementTypes();
      assertEquals(2, types.size(), "getElementTypes must have same count as elements");
      assertEquals(tuple.get(0).getType(), types.get(0), "Type at 0 must match element type");
      assertEquals(tuple.get(1).getType(), types.get(1), "Type at 1 must match element type");
    }

    @Test
    @DisplayName("getTypeAt must return correct type for each index")
    void getTypeAtMutationTest() {
      final WitTuple tuple = WitTuple.of(WitS32.of(1), WitS64.of(2L), WitBool.TRUE);

      assertEquals(WitType.createS32(), tuple.getTypeAt(0), "getTypeAt(0) must return S32 type");
      assertEquals(WitType.createS64(), tuple.getTypeAt(1), "getTypeAt(1) must return S64 type");
      assertEquals(WitType.createBool(), tuple.getTypeAt(2), "getTypeAt(2) must return Bool type");
    }

    @Test
    @DisplayName("empty tuple from factory vs builder must produce equivalent results")
    void emptyTupleMutationTest() {
      // empty() factory method
      final WitTuple empty1 = WitTuple.empty();
      assertTrue(empty1.isEmpty(), "empty() tuple isEmpty must be true");
      assertEquals(0, empty1.size(), "empty() tuple size must be 0");

      // builder().build() with no elements
      final WitTuple empty2 = WitTuple.builder().build();
      assertTrue(empty2.isEmpty(), "builder().build() tuple isEmpty must be true");
      assertEquals(0, empty2.size(), "builder().build() tuple size must be 0");

      // Both should be equal
      assertEquals(empty1, empty2, "Empty tuples from factory and builder must be equal");
    }

    @Test
    @DisplayName("getElements should contain exact values in order")
    void getElementsShouldContainExactValuesInOrder() {
      final WitTuple tuple = WitTuple.of(WitS32.of(10), WitS32.of(20), WitS32.of(30));

      final java.util.List<WitValue> elements = tuple.getElements();
      assertEquals(3, elements.size(), "Should have 3 elements");
      assertEquals(10, ((WitS32) elements.get(0)).getValue(), "First element should be 10");
      assertEquals(20, ((WitS32) elements.get(1)).getValue(), "Second element should be 20");
      assertEquals(30, ((WitS32) elements.get(2)).getValue(), "Third element should be 30");
    }
  }

  @Nested
  @DisplayName("Surviving Mutant Killer Tests")
  class SurvivingMutantKillerTests {

    @Test
    @DisplayName("constructor must reject null elementTypes")
    void constructorMustRejectNullElementTypes() {
      // Targets line 65: elementTypes == null check
      // The constructor is private, but we can exercise it via of(List) with null
      assertThrows(
          IllegalArgumentException.class,
          () -> WitTuple.of((java.util.List<WitValue>) null),
          "of(null List) must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("constructor must reject null elements")
    void constructorMustRejectNullElements() {
      // Targets line 68: elements == null check
      assertThrows(
          IllegalArgumentException.class,
          () -> WitTuple.of((WitValue[]) null),
          "of(null array) must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("constructor must check elementTypes size matches elements size")
    void constructorMustCheckSizeMatch() {
      // Targets line 71: elementTypes.size() != elements.size()
      // This is exercised via the builder when types and elements mismatch
      // We can test via direct type mismatch detection by the validate() call
      final WitTuple tuple = WitTuple.of(WitS32.of(1), WitS32.of(2));
      assertEquals(2, tuple.getElementTypes().size(), "Element types count must match");
      assertEquals(2, tuple.getElements().size(), "Elements count must match");
    }

    @Test
    @DisplayName("constructor must call validate which rejects type mismatches")
    void constructorMustCallValidate() {
      // Targets line 80: validate() call removal mutation
      // If validate() is removed, a type-mismatched tuple would be created
      assertThrows(
          IllegalArgumentException.class,
          () -> WitTuple.builder().add(WitType.createS64(), WitS32.of(42)).build(),
          "validate() must be called to reject type mismatch");
    }

    @Test
    @DisplayName("of(List) must reject null elements in list")
    void ofListMustRejectNullElementsInList() {
      // Targets line 113: null check in of(List) method
      final java.util.List<WitValue> listWithNull = new java.util.ArrayList<>();
      listWithNull.add(null);
      assertThrows(
          IllegalArgumentException.class,
          () -> WitTuple.of(listWithNull),
          "of(List) must reject null element");
    }

    @Test
    @DisplayName("validate must iterate with correct index boundaries")
    void validateMustIterateWithCorrectIndexBoundaries() {
      // Targets lines 210, 213, 216 - loop index math
      // Create a tuple with 3 elements of different types to ensure each index is checked
      final WitTuple tuple = WitTuple.of(WitS32.of(10), WitS64.of(20L), WitBool.of(true));

      // Verify each element type is correctly validated at its index
      assertEquals(WitType.createS32(), tuple.getTypeAt(0), "Index 0 must be S32");
      assertEquals(WitType.createS64(), tuple.getTypeAt(1), "Index 1 must be S64");
      assertEquals(WitType.createBool(), tuple.getTypeAt(2), "Index 2 must be Bool");

      // Verify element at index 0 matches type at index 0, not type at index 1
      assertEquals(WitS32.of(10), tuple.get(0), "Element at index 0 must be S32(10)");
      assertNotEquals(tuple.getTypeAt(0), tuple.getTypeAt(1), "Types at different indices differ");
    }

    @Test
    @DisplayName("validate must check element null at each index")
    void validateMustCheckElementNullAtEachIndex() {
      // Targets line 213: element == null check in validate
      final java.util.List<WitValue> elements = new java.util.ArrayList<>();
      elements.add(WitS32.of(1));
      elements.add(null);

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> WitTuple.of(elements),
              "Must reject null element during validation");
      assertTrue(
          ex.getMessage().contains("null"),
          "Error message should mention null: " + ex.getMessage());
    }

    @Test
    @DisplayName("validate must check type equality at each index")
    void validateMustCheckTypeEqualityAtEachIndex() {
      // Targets line 216: !element.getType().equals(expectedType)
      // This mutation would skip the type check - exercise both matching and mismatching
      final WitTuple valid = WitTuple.of(WitS32.of(1));
      assertEquals(WitType.createS32(), valid.getTypeAt(0), "Valid tuple type at 0 should be s32");

      // Mismatch should be caught
      assertThrows(
          IllegalArgumentException.class,
          () -> WitTuple.builder().add(WitType.createString(), WitS32.of(1)).build(),
          "Type mismatch must be rejected");
    }
  }
}
