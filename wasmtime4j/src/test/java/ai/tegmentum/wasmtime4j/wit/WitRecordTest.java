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
      final WitRecord record =
          WitRecord.builder().field("x", WitS32.of(10)).field("y", WitS32.of(20)).build();
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
      final WitRecord record = WitRecord.builder().field("x", WitS32.of(10)).build();
      final WitValue field = record.getField("x");
      assertNotNull(field, "getField should return non-null for existing field");
      assertEquals(WitS32.of(10), field, "Field value should match");
    }

    @Test
    @DisplayName("getField for nonexistent field should return null")
    void getFieldForNonexistentShouldReturnNull() {
      final WitRecord record = WitRecord.builder().field("x", WitS32.of(10)).build();
      assertNull(record.getField("nonexistent"), "getField for missing field should return null");
    }

    @Test
    @DisplayName("getFields should return unmodifiable map")
    void getFieldsShouldReturnUnmodifiableMap() {
      final WitRecord record = WitRecord.builder().field("x", WitS32.of(10)).build();
      final Map<String, WitValue> fields = record.getFields();
      assertThrows(
          UnsupportedOperationException.class,
          () -> fields.put("new", WitS32.of(1)),
          "getFields should return unmodifiable map");
    }

    @Test
    @DisplayName("getFieldCount should return correct count")
    void getFieldCountShouldReturnCorrectCount() {
      final WitRecord record =
          WitRecord.builder().field("x", WitS32.of(10)).field("y", WitS32.of(20)).build();
      assertEquals(2, record.getFieldCount(), "Field count should be 2");
    }

    @Test
    @DisplayName("hasField should return true for existing field")
    void hasFieldShouldReturnTrueForExisting() {
      final WitRecord record = WitRecord.builder().field("x", WitS32.of(10)).build();
      assertTrue(record.hasField("x"), "hasField should return true for existing field");
    }

    @Test
    @DisplayName("hasField should return false for nonexistent field")
    void hasFieldShouldReturnFalseForNonexistent() {
      final WitRecord record = WitRecord.builder().field("x", WitS32.of(10)).build();
      assertFalse(record.hasField("y"), "hasField should return false for nonexistent field");
    }
  }

  @Nested
  @DisplayName("ToJava Tests")
  class ToJavaTests {

    @Test
    @DisplayName("toJava should return Map")
    void toJavaShouldReturnMap() {
      final WitRecord record = WitRecord.builder().field("x", WitS32.of(10)).build();
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
      assertEquals(r1.hashCode(), r2.hashCode(), "Same records should have same hash code");
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

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("of should reject map with null value")
    void ofShouldRejectMapWithNullValue() {
      final Map<String, WitValue> fieldsWithNull = new LinkedHashMap<>();
      fieldsWithNull.put("valid", WitS32.of(1));
      fieldsWithNull.put("invalid", null);

      // NPE is thrown during type construction when accessing null.getType()
      assertThrows(
          NullPointerException.class,
          () -> WitRecord.of(fieldsWithNull),
          "Should throw NPE for null field value during type construction");
    }

    @Test
    @DisplayName("of should reject map with empty field name")
    void ofShouldRejectMapWithEmptyFieldName() {
      final Map<String, WitValue> fieldsWithEmptyName = new LinkedHashMap<>();
      fieldsWithEmptyName.put("", WitS32.of(1));

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> WitRecord.of(fieldsWithEmptyName),
              "Should throw for empty field name");
      assertTrue(
          ex.getMessage().contains("empty") || ex.getMessage().contains("null"),
          "Exception message should mention empty or null: " + ex.getMessage());
    }

    @Test
    @DisplayName("should accept valid field names and values")
    void shouldAcceptValidFieldNamesAndValues() {
      final Map<String, WitValue> validFields = new LinkedHashMap<>();
      validFields.put("field1", WitS32.of(1));
      validFields.put("field2", WitS64.of(2L));
      validFields.put("field3", WitBool.of(true));

      final WitRecord record = WitRecord.of(validFields);
      assertEquals(3, record.getFieldCount(), "Should create record with all fields");
    }
  }

  @Nested
  @DisplayName("Mutation Killing Tests")
  class MutationKillingTests {

    @Test
    @DisplayName("hasField must return true for existing and false for non-existing")
    void hasFieldMutationTest() {
      final WitRecord record = WitRecord.builder().field("exists", WitS32.of(1)).build();

      // Existing field must return true
      assertTrue(record.hasField("exists"), "hasField() for existing must return exactly true");
      assertFalse(!record.hasField("exists"), "hasField() result must be true, not false");

      // Non-existing field must return false
      assertFalse(record.hasField("missing"), "hasField() for missing must return exactly false");
      assertTrue(!record.hasField("missing"), "hasField() result must be false, not true");
    }

    @Test
    @DisplayName("getFieldCount must return exact count")
    void getFieldCountMutationTest() {
      // Single field record: count must be exactly 1
      final WitRecord one = WitRecord.builder().field("x", WitS32.of(1)).build();
      assertEquals(1, one.getFieldCount(), "One field count must be exactly 1");
      assertTrue(one.getFieldCount() == 1, "One field count == 1 must be true");
      assertFalse(one.getFieldCount() == 0, "One field count == 0 must be false");
      assertFalse(one.getFieldCount() == 2, "One field count == 2 must be false");

      // Three field record: count must be exactly 3
      final WitRecord three =
          WitRecord.builder()
              .field("a", WitS32.of(1))
              .field("b", WitS32.of(2))
              .field("c", WitS32.of(3))
              .build();
      assertEquals(3, three.getFieldCount(), "Three field count must be exactly 3");
      assertTrue(three.getFieldCount() == 3, "Three field count == 3 must be true");
    }

    @Test
    @DisplayName("getField must return correct value for each field")
    void getFieldMutationTest() {
      final WitRecord record =
          WitRecord.builder().field("first", WitS32.of(10)).field("second", WitS32.of(20)).build();

      // First field must return correct value
      assertEquals(WitS32.of(10), record.getField("first"), "getField('first') must return 10");
      assertNotEquals(
          WitS32.of(20), record.getField("first"), "getField('first') must not return 20");

      // Second field must return correct value
      assertEquals(WitS32.of(20), record.getField("second"), "getField('second') must return 20");

      // Missing field must return null
      assertNull(record.getField("missing"), "getField('missing') must return null");
    }

    @Test
    @DisplayName("toJava must return map with correct values")
    void toJavaMutationTest() {
      final WitRecord record =
          WitRecord.builder().field("num", WitS32.of(42)).field("flag", WitBool.TRUE).build();

      final Map<String, Object> javaMap = record.toJava();
      assertEquals(2, javaMap.size(), "toJava map must have correct size");
      assertEquals(42, javaMap.get("num"), "toJava map['num'] must be 42");
      assertEquals(true, javaMap.get("flag"), "toJava map['flag'] must be true");
      assertFalse(javaMap.containsKey("missing"), "toJava map must not have extra keys");
    }

    @Test
    @DisplayName("equals must handle edge cases correctly")
    void equalsMutationTest() {
      final WitRecord record = WitRecord.builder().field("x", WitS32.of(1)).build();

      // Reflexive - same object
      assertTrue(record.equals(record), "equals(self) must return true");

      // Null comparison
      assertFalse(record.equals(null), "equals(null) must return false");

      // Different type
      assertFalse(record.equals("record"), "equals(String) must return false");
      assertFalse(record.equals(42), "equals(Integer) must return false");

      // Different field name, same value
      final WitRecord differentName = WitRecord.builder().field("y", WitS32.of(1)).build();
      assertFalse(record.equals(differentName), "Different field name must not be equal");

      // Different field value, same name
      final WitRecord differentValue = WitRecord.builder().field("x", WitS32.of(99)).build();
      assertFalse(record.equals(differentValue), "Different field value must not be equal");

      // Different number of fields
      final WitRecord moreFields =
          WitRecord.builder().field("x", WitS32.of(1)).field("y", WitS32.of(2)).build();
      assertFalse(record.equals(moreFields), "Different field count must not be equal");
    }

    @Test
    @DisplayName("builder field replacement must use latest value")
    void builderFieldReplacementMutationTest() {
      // Adding same field twice should use the last value
      final WitRecord record =
          WitRecord.builder()
              .field("x", WitS32.of(1))
              .field("x", WitS32.of(99)) // Replace
              .build();

      assertEquals(WitS32.of(99), record.getField("x"), "Field replacement must use latest value");
      assertEquals(1, record.getFieldCount(), "Replacement must not increase count");
    }

    @Test
    @DisplayName("getFields must preserve field order")
    void getFieldsOrderMutationTest() {
      final WitRecord record =
          WitRecord.builder()
              .field("first", WitS32.of(1))
              .field("second", WitS32.of(2))
              .field("third", WitS32.of(3))
              .build();

      final Map<String, WitValue> fields = record.getFields();
      final String[] keys = fields.keySet().toArray(new String[0]);

      assertEquals("first", keys[0], "First field must be 'first'");
      assertEquals("second", keys[1], "Second field must be 'second'");
      assertEquals("third", keys[2], "Third field must be 'third'");
    }
  }
}
