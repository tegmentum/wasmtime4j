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
 * Tests for {@link WitTuple} class.
 *
 * <p>WitTuple represents a WIT tuple value (fixed-size, heterogeneous sequence).
 */
@DisplayName("WitTuple Tests")
class WitTupleTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WitTuple.class.getModifiers()), "WitTuple should be final");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitTuple.class),
          "WitTuple should extend WitValue");
    }
  }

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
      final WitTuple tuple = WitTuple.builder()
          .add(WitS32.of(1))
          .add(WitS32.of(2))
          .build();
      assertNotNull(tuple, "Builder should create non-null tuple");
      assertEquals(2, tuple.size(), "Builder tuple size should be 2");
    }

    @Test
    @DisplayName("builder with explicit type should create tuple")
    void builderWithExplicitTypeShouldCreateTuple() {
      final WitTuple tuple = WitTuple.builder()
          .add(WitType.createS32(), WitS32.of(42))
          .build();
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
      assertEquals(
          t1.hashCode(), t2.hashCode(), "Same tuples should have same hash code");
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
}
