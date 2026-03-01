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
 * Tests for {@link FieldDefinition} class.
 *
 * <p>FieldDefinition represents a single field in a WebAssembly GC struct, with name, type,
 * mutability, and position information.
 */
@DisplayName("FieldDefinition Tests")
class FieldDefinitionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create field definition with all parameters")
    void shouldCreateFieldDefinitionWithAllParameters() {
      final FieldDefinition field = new FieldDefinition("x", FieldType.i32(), true, 0);

      assertNotNull(field, "Should create field definition");
      assertEquals("x", field.getName(), "Should have correct name");
      assertEquals(FieldType.i32(), field.getFieldType(), "Should have correct type");
      assertTrue(field.isMutable(), "Should be mutable");
      assertEquals(0, field.getIndex(), "Should have correct index");
    }

    @Test
    @DisplayName("should create field definition with null name")
    void shouldCreateFieldDefinitionWithNullName() {
      final FieldDefinition field = new FieldDefinition(null, FieldType.i64(), false, 1);

      assertNull(field.getName(), "Name should be null");
      assertFalse(field.hasName(), "Should not have name");
    }

    @Test
    @DisplayName("should throw for null field type")
    void shouldThrowForNullFieldType() {
      assertThrows(
          NullPointerException.class,
          () -> new FieldDefinition("x", null, true, 0),
          "Should throw for null field type");
    }

    @Test
    @DisplayName("should throw for negative index")
    void shouldThrowForNegativeIndex() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new FieldDefinition("x", FieldType.i32(), true, -1),
          "Should throw for negative index");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getName should return field name")
    void getNameShouldReturnFieldName() {
      final FieldDefinition field = new FieldDefinition("myField", FieldType.i32(), true, 0);
      assertEquals("myField", field.getName(), "Should return correct name");
    }

    @Test
    @DisplayName("getFieldType should return field type")
    void getFieldTypeShouldReturnFieldType() {
      final FieldDefinition field = new FieldDefinition("x", FieldType.f64(), false, 0);
      assertEquals(FieldType.f64(), field.getFieldType(), "Should return correct type");
    }

    @Test
    @DisplayName("isMutable should return mutability")
    void isMutableShouldReturnMutability() {
      final FieldDefinition mutableField = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition immutableField = new FieldDefinition("y", FieldType.i32(), false, 1);

      assertTrue(mutableField.isMutable(), "Should be mutable");
      assertFalse(immutableField.isMutable(), "Should be immutable");
    }

    @Test
    @DisplayName("getIndex should return field index")
    void getIndexShouldReturnFieldIndex() {
      final FieldDefinition field0 = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition field5 = new FieldDefinition("y", FieldType.i32(), true, 5);

      assertEquals(0, field0.getIndex(), "Should return index 0");
      assertEquals(5, field5.getIndex(), "Should return index 5");
    }
  }

  @Nested
  @DisplayName("hasName Tests")
  class HasNameTests {

    @Test
    @DisplayName("should return true for named field")
    void shouldReturnTrueForNamedField() {
      final FieldDefinition field = new FieldDefinition("x", FieldType.i32(), true, 0);
      assertTrue(field.hasName(), "Named field should return true");
    }

    @Test
    @DisplayName("should return false for unnamed field")
    void shouldReturnFalseForUnnamedField() {
      final FieldDefinition field = new FieldDefinition(null, FieldType.i32(), true, 0);
      assertFalse(field.hasName(), "Unnamed field should return false");
    }
  }

  @Nested
  @DisplayName("getSizeBytes Tests")
  class GetSizeBytesTests {

    @Test
    @DisplayName("should return size from field type")
    void shouldReturnSizeFromFieldType() {
      final FieldDefinition i32Field = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition i64Field = new FieldDefinition("y", FieldType.i64(), true, 1);

      assertEquals(
          FieldType.i32().getSizeBytes(), i32Field.getSizeBytes(), "Should return I32 size");
      assertEquals(
          FieldType.i64().getSizeBytes(), i64Field.getSizeBytes(), "Should return I64 size");
    }
  }

  @Nested
  @DisplayName("isCompatibleWith Tests")
  class IsCompatibleWithTests {

    @Test
    @DisplayName("should be compatible with same field definition")
    void shouldBeCompatibleWithSameFieldDefinition() {
      final FieldDefinition field1 = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition field2 = new FieldDefinition("x", FieldType.i32(), true, 0);

      assertTrue(field1.isCompatibleWith(field2), "Same fields should be compatible");
    }

    @Test
    @DisplayName("mutable should be compatible with immutable")
    void mutableShouldBeCompatibleWithImmutable() {
      final FieldDefinition mutable = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition immutable = new FieldDefinition("x", FieldType.i32(), false, 0);

      // Mutable can substitute for immutable in subtyping
      assertTrue(
          mutable.isCompatibleWith(immutable), "Mutable should be compatible with immutable");
    }

    @Test
    @DisplayName("immutable should not be compatible with mutable")
    void immutableShouldNotBeCompatibleWithMutable() {
      final FieldDefinition immutable = new FieldDefinition("x", FieldType.i32(), false, 0);
      final FieldDefinition mutable = new FieldDefinition("x", FieldType.i32(), true, 0);

      // Immutable cannot substitute for mutable in subtyping
      assertFalse(
          immutable.isCompatibleWith(mutable), "Immutable should not be compatible with mutable");
    }
  }

  @Nested
  @DisplayName("equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final FieldDefinition field = new FieldDefinition("x", FieldType.i32(), true, 0);
      assertEquals(field, field, "Should be equal to itself");
    }

    @Test
    @DisplayName("should be equal to equivalent field definition")
    void shouldBeEqualToEquivalentFieldDefinition() {
      final FieldDefinition field1 = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition field2 = new FieldDefinition("x", FieldType.i32(), true, 0);

      assertEquals(field1, field2, "Equivalent fields should be equal");
    }

    @Test
    @DisplayName("should not be equal with different name")
    void shouldNotBeEqualWithDifferentName() {
      final FieldDefinition field1 = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition field2 = new FieldDefinition("y", FieldType.i32(), true, 0);

      assertNotEquals(field1, field2, "Fields with different names should not be equal");
    }

    @Test
    @DisplayName("should not be equal with different type")
    void shouldNotBeEqualWithDifferentType() {
      final FieldDefinition field1 = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition field2 = new FieldDefinition("x", FieldType.i64(), true, 0);

      assertNotEquals(field1, field2, "Fields with different types should not be equal");
    }

    @Test
    @DisplayName("should not be equal with different mutability")
    void shouldNotBeEqualWithDifferentMutability() {
      final FieldDefinition field1 = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition field2 = new FieldDefinition("x", FieldType.i32(), false, 0);

      assertNotEquals(field1, field2, "Fields with different mutability should not be equal");
    }

    @Test
    @DisplayName("should not be equal with different index")
    void shouldNotBeEqualWithDifferentIndex() {
      final FieldDefinition field1 = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition field2 = new FieldDefinition("x", FieldType.i32(), true, 1);

      assertNotEquals(field1, field2, "Fields with different indices should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final FieldDefinition field = new FieldDefinition("x", FieldType.i32(), true, 0);
      assertNotEquals(null, field, "Should not be equal to null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final FieldDefinition field = new FieldDefinition("x", FieldType.i32(), true, 0);
      assertNotEquals("x", field, "Should not be equal to String");
    }
  }

  @Nested
  @DisplayName("hashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("should have consistent hashCode")
    void shouldHaveConsistentHashCode() {
      final FieldDefinition field = new FieldDefinition("x", FieldType.i32(), true, 0);
      final int hash1 = field.hashCode();
      final int hash2 = field.hashCode();

      assertEquals(hash1, hash2, "hashCode should be consistent");
    }

    @Test
    @DisplayName("equal objects should have same hashCode")
    void equalObjectsShouldHaveSameHashCode() {
      final FieldDefinition field1 = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition field2 = new FieldDefinition("x", FieldType.i32(), true, 0);

      assertEquals(field1.hashCode(), field2.hashCode(), "Equal objects should have same hashCode");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should include name for named field")
    void shouldIncludeNameForNamedField() {
      final FieldDefinition field = new FieldDefinition("x", FieldType.i32(), true, 0);
      final String str = field.toString();

      assertTrue(str.contains("x"), "toString should contain name");
    }

    @Test
    @DisplayName("should indicate mutability")
    void shouldIndicateMutability() {
      final FieldDefinition mutableField = new FieldDefinition("x", FieldType.i32(), true, 0);

      assertTrue(
          mutableField.toString().contains("mut"), "Mutable field toString should contain 'mut'");
    }

    @Test
    @DisplayName("should include field type")
    void shouldIncludeFieldType() {
      final FieldDefinition field = new FieldDefinition("x", FieldType.i32(), true, 0);
      final String str = field.toString();

      assertNotNull(str, "toString should not return null");
      assertTrue(str.length() > 0, "toString should not be empty");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support creating struct field definitions")
    void shouldSupportCreatingStructFieldDefinitions() {
      // Point struct with x, y coordinates
      final FieldDefinition x = new FieldDefinition("x", FieldType.f64(), true, 0);
      final FieldDefinition y = new FieldDefinition("y", FieldType.f64(), true, 1);

      assertEquals(0, x.getIndex(), "x should be first field");
      assertEquals(1, y.getIndex(), "y should be second field");
      assertTrue(x.isMutable(), "x should be mutable");
      assertTrue(y.isMutable(), "y should be mutable");
    }

    @Test
    @DisplayName("should support immutable fields")
    void shouldSupportImmutableFields() {
      // Immutable timestamp field
      final FieldDefinition timestamp = new FieldDefinition("timestamp", FieldType.i64(), false, 0);

      assertFalse(timestamp.isMutable(), "Timestamp should be immutable");
    }

    @Test
    @DisplayName("should support unnamed fields")
    void shouldSupportUnnamedFields() {
      // Positional fields without names
      final FieldDefinition field0 = new FieldDefinition(null, FieldType.i32(), true, 0);
      final FieldDefinition field1 = new FieldDefinition(null, FieldType.i32(), true, 1);

      assertFalse(field0.hasName(), "Field 0 should be unnamed");
      assertFalse(field1.hasName(), "Field 1 should be unnamed");
      assertEquals(0, field0.getIndex(), "Field 0 should have index 0");
      assertEquals(1, field1.getIndex(), "Field 1 should have index 1");
    }
  }
}
