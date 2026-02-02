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

import ai.tegmentum.wasmtime4j.WitType;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitList} class.
 *
 * <p>WitList represents a WIT list value (variable-length homogeneous array).
 */
@DisplayName("WitList Tests")
class WitListTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WitList.class.getModifiers()), "WitList should be final");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitList.class),
          "WitList should extend WitValue");
    }
  }

  @Nested
  @DisplayName("Creation Tests")
  class CreationTests {

    @Test
    @DisplayName("of varargs should create list")
    void ofVarargsShouldCreateList() {
      final WitList list = WitList.of(WitS32.of(1), WitS32.of(2), WitS32.of(3));
      assertNotNull(list, "WitList.of should create non-null list");
      assertEquals(3, list.size(), "List size should be 3");
    }

    @Test
    @DisplayName("of Java List should create WitList")
    void ofJavaListShouldCreateWitList() {
      final List<WitValue> elements = List.of(WitS32.of(10), WitS32.of(20));
      final WitList list = WitList.of(elements);
      assertNotNull(list, "WitList.of(List) should create non-null list");
      assertEquals(2, list.size(), "List size should be 2");
    }

    @Test
    @DisplayName("empty should create empty list")
    void emptyShouldCreateEmptyList() {
      final WitList list = WitList.empty(WitType.createS32());
      assertNotNull(list, "WitList.empty should create non-null list");
      assertTrue(list.isEmpty(), "Empty list should be empty");
      assertEquals(0, list.size(), "Empty list size should be 0");
    }

    @Test
    @DisplayName("builder should create list")
    void builderShouldCreateList() {
      final WitList list = WitList.builder(WitType.createS32())
          .add(WitS32.of(1))
          .add(WitS32.of(2))
          .build();
      assertNotNull(list, "Builder should create non-null list");
      assertEquals(2, list.size(), "Builder list size should be 2");
    }

    @Test
    @DisplayName("of with null varargs should throw")
    void ofWithNullVarargsShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitList.of((WitValue[]) null),
          "of(null) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("of with empty varargs should throw")
    void ofWithEmptyVarargsShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitList.of(new WitValue[0]),
          "of(empty) should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Element Access Tests")
  class ElementAccessTests {

    @Test
    @DisplayName("get should return correct element")
    void getShouldReturnCorrectElement() {
      final WitList list = WitList.of(WitS32.of(10), WitS32.of(20));
      assertEquals(WitS32.of(10), list.get(0), "First element should be 10");
      assertEquals(WitS32.of(20), list.get(1), "Second element should be 20");
    }

    @Test
    @DisplayName("get out of bounds should throw")
    void getOutOfBoundsShouldThrow() {
      final WitList list = WitList.of(WitS32.of(10));
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> list.get(5),
          "get(5) on size-1 list should throw IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("getElements should return unmodifiable list")
    void getElementsShouldReturnUnmodifiable() {
      final WitList list = WitList.of(WitS32.of(10));
      final List<WitValue> elements = list.getElements();
      assertThrows(
          UnsupportedOperationException.class,
          () -> elements.add(WitS32.of(1)),
          "getElements should return unmodifiable list");
    }

    @Test
    @DisplayName("getElementType should return correct type")
    void getElementTypeShouldReturnCorrectType() {
      final WitList list = WitList.of(WitS32.of(10));
      assertNotNull(list.getElementType(), "getElementType should return non-null type");
    }

    @Test
    @DisplayName("isEmpty should return false for non-empty list")
    void isEmptyShouldReturnFalseForNonEmpty() {
      final WitList list = WitList.of(WitS32.of(10));
      assertFalse(list.isEmpty(), "Non-empty list isEmpty should return false");
    }
  }

  @Nested
  @DisplayName("ToJava Tests")
  class ToJavaTests {

    @Test
    @DisplayName("toJava should return List")
    void toJavaShouldReturnList() {
      final WitList list = WitList.of(WitS32.of(10), WitS32.of(20));
      final Object javaValue = list.toJava();
      assertTrue(javaValue instanceof List, "toJava should return a List");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same elements should be equal")
    void sameElementsShouldBeEqual() {
      final WitList l1 = WitList.of(WitS32.of(1), WitS32.of(2));
      final WitList l2 = WitList.of(WitS32.of(1), WitS32.of(2));
      assertEquals(l1, l2, "Lists with same elements should be equal");
    }

    @Test
    @DisplayName("different elements should not be equal")
    void differentElementsShouldNotBeEqual() {
      final WitList l1 = WitList.of(WitS32.of(1));
      final WitList l2 = WitList.of(WitS32.of(2));
      assertNotEquals(l1, l2, "Lists with different elements should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same lists should have same hash code")
    void sameListsShouldHaveSameHashCode() {
      final WitList l1 = WitList.of(WitS32.of(1), WitS32.of(2));
      final WitList l2 = WitList.of(WitS32.of(1), WitS32.of(2));
      assertEquals(
          l1.hashCode(), l2.hashCode(), "Same lists should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain WitList")
    void toStringShouldContainClassName() {
      final WitList list = WitList.of(WitS32.of(1));
      final String str = list.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("WitList"), "toString should contain 'WitList'");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitList list = WitList.of(WitS32.of(1));
      assertNotNull(list.getType(), "Should have WitType");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("should reject null element in list")
    void shouldRejectNullElementInList() {
      final List<WitValue> elementsWithNull = new java.util.ArrayList<>();
      elementsWithNull.add(WitS32.of(1));
      elementsWithNull.add(null);
      elementsWithNull.add(WitS32.of(3));

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> WitList.of(elementsWithNull),
              "Should throw for null element");
      assertTrue(
          ex.getMessage().contains("null"),
          "Exception message should mention null: " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("index"),
          "Exception message should mention index: " + ex.getMessage());
    }

    @Test
    @DisplayName("should reject type mismatch in list elements")
    void shouldRejectTypeMismatchInListElements() {
      // First create a list with S32 elements, then try to add U64
      final List<WitValue> mixedTypes = new java.util.ArrayList<>();
      mixedTypes.add(WitS32.of(1));
      mixedTypes.add(WitU64.of(100L)); // Different type

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> WitList.of(mixedTypes),
              "Should throw for type mismatch");
      assertTrue(
          ex.getMessage().contains("type"),
          "Exception message should mention type: " + ex.getMessage());
      assertTrue(
          ex.getMessage().contains("expected"),
          "Exception message should mention expected: " + ex.getMessage());
    }

    @Test
    @DisplayName("should accept homogeneous list")
    void shouldAcceptHomogeneousList() {
      final List<WitValue> sameType = List.of(WitS32.of(1), WitS32.of(2), WitS32.of(3));
      final WitList list = WitList.of(sameType);
      assertEquals(3, list.size(), "Should create list with all elements");
    }
  }
}
