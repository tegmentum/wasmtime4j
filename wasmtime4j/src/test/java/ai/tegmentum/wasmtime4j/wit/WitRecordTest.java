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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitRecord} class.
 *
 * <p>WitRecord represents a WIT record value with named fields, similar to structs.
 */
@DisplayName("WitRecord Tests")
class WitRecordTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WitRecord.class.getModifiers()), "WitRecord should be final");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitRecord.class),
          "WitRecord should extend WitValue");
    }
  }

  @Nested
  @DisplayName("Creation Tests")
  class CreationTests {

    @Test
    @DisplayName("of should create record from map")
    void ofShouldCreateRecordFromMap() {
      final Map<String, WitValue> fields = new LinkedHashMap<>();
      fields.put("name", WitS32.of(42));
      final WitRecord record = WitRecord.of(fields);
      assertNotNull(record, "WitRecord.of should create non-null record");
    }

    @Test
    @DisplayName("builder should create record")
    void builderShouldCreateRecord() {
      final WitRecord record = WitRecord.builder()
          .field("x", WitS32.of(10))
          .field("y", WitS32.of(20))
          .build();
      assertNotNull(record, "Builder should create non-null record");
    }

    @Test
    @DisplayName("empty fields should throw IllegalArgumentException")
    void emptyFieldsShouldThrowIae() {
      final Map<String, WitValue> empty = new LinkedHashMap<>();
      assertThrows(
          IllegalArgumentException.class,
          () -> WitRecord.of(empty),
          "Empty fields should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("builder with null field name should throw")
    void builderWithNullFieldNameShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitRecord.builder().field(null, WitS32.of(1)),
          "Null field name should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("builder with null field value should throw")
    void builderWithNullFieldValueShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitRecord.builder().field("x", null),
          "Null field value should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("builder with empty field name should throw")
    void builderWithEmptyFieldNameShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitRecord.builder().field("", WitS32.of(1)),
          "Empty field name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Field Access Tests")
  class FieldAccessTests {

    @Test
    @DisplayName("getField should return correct value")
    void getFieldShouldReturnCorrectValue() {
      final WitRecord record = WitRecord.builder()
          .field("x", WitS32.of(10))
          .build();
      final WitValue field = record.getField("x");
      assertNotNull(field, "getField should return non-null for existing field");
      assertEquals(WitS32.of(10), field, "Field value should match");
    }

    @Test
    @DisplayName("getField for nonexistent field should return null")
    void getFieldForNonexistentShouldReturnNull() {
      final WitRecord record = WitRecord.builder()
          .field("x", WitS32.of(10))
          .build();
      assertNull(
          record.getField("nonexistent"),
          "getField for missing field should return null");
    }

    @Test
    @DisplayName("getFields should return unmodifiable map")
    void getFieldsShouldReturnUnmodifiableMap() {
      final WitRecord record = WitRecord.builder()
          .field("x", WitS32.of(10))
          .build();
      final Map<String, WitValue> fields = record.getFields();
      assertThrows(
          UnsupportedOperationException.class,
          () -> fields.put("new", WitS32.of(1)),
          "getFields should return unmodifiable map");
    }

    @Test
    @DisplayName("getFieldCount should return correct count")
    void getFieldCountShouldReturnCorrectCount() {
      final WitRecord record = WitRecord.builder()
          .field("x", WitS32.of(10))
          .field("y", WitS32.of(20))
          .build();
      assertEquals(2, record.getFieldCount(), "Field count should be 2");
    }

    @Test
    @DisplayName("hasField should return true for existing field")
    void hasFieldShouldReturnTrueForExisting() {
      final WitRecord record = WitRecord.builder()
          .field("x", WitS32.of(10))
          .build();
      assertTrue(record.hasField("x"), "hasField should return true for existing field");
    }

    @Test
    @DisplayName("hasField should return false for nonexistent field")
    void hasFieldShouldReturnFalseForNonexistent() {
      final WitRecord record = WitRecord.builder()
          .field("x", WitS32.of(10))
          .build();
      assertFalse(
          record.hasField("y"), "hasField should return false for nonexistent field");
    }
  }

  @Nested
  @DisplayName("ToJava Tests")
  class ToJavaTests {

    @Test
    @DisplayName("toJava should return Map")
    void toJavaShouldReturnMap() {
      final WitRecord record = WitRecord.builder()
          .field("x", WitS32.of(10))
          .build();
      final Object javaValue = record.toJava();
      assertTrue(javaValue instanceof Map, "toJava should return a Map");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same fields should be equal")
    void sameFieldsShouldBeEqual() {
      final WitRecord r1 = WitRecord.builder().field("x", WitS32.of(10)).build();
      final WitRecord r2 = WitRecord.builder().field("x", WitS32.of(10)).build();
      assertEquals(r1, r2, "Records with same fields should be equal");
    }

    @Test
    @DisplayName("different fields should not be equal")
    void differentFieldsShouldNotBeEqual() {
      final WitRecord r1 = WitRecord.builder().field("x", WitS32.of(10)).build();
      final WitRecord r2 = WitRecord.builder().field("x", WitS32.of(20)).build();
      assertNotEquals(r1, r2, "Records with different field values should not be equal");
    }

    @Test
    @DisplayName("same instance should be equal to itself")
    void sameInstanceShouldBeEqualToItself() {
      final WitRecord r = WitRecord.builder().field("x", WitS32.of(10)).build();
      assertEquals(r, r, "Same instance should equal itself");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same records should have same hash code")
    void sameRecordsShouldHaveSameHashCode() {
      final WitRecord r1 = WitRecord.builder().field("x", WitS32.of(10)).build();
      final WitRecord r2 = WitRecord.builder().field("x", WitS32.of(10)).build();
      assertEquals(
          r1.hashCode(), r2.hashCode(),
          "Same records should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain WitRecord")
    void toStringShouldContainClassName() {
      final WitRecord record = WitRecord.builder().field("x", WitS32.of(10)).build();
      final String str = record.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("WitRecord"), "toString should contain 'WitRecord'");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitRecord record = WitRecord.builder().field("x", WitS32.of(10)).build();
      assertNotNull(record.getType(), "Should have WitType");
    }
  }
}
