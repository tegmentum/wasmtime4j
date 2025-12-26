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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitTypeCategory} enum.
 *
 * <p>WitTypeCategory defines categories of WebAssembly Interface Types (WIT) for validation and
 * marshalling.
 */
@DisplayName("WitTypeCategory Tests")
class WitTypeCategoryTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WitTypeCategory.class.isEnum(), "WitTypeCategory should be an enum");
    }

    @Test
    @DisplayName("should have 10 type categories")
    void shouldHave10TypeCategories() {
      final var values = WitTypeCategory.values();
      assertEquals(10, values.length);
    }

    @Test
    @DisplayName("should have all expected categories")
    void shouldHaveAllExpectedCategories() {
      assertNotNull(WitTypeCategory.PRIMITIVE);
      assertNotNull(WitTypeCategory.RECORD);
      assertNotNull(WitTypeCategory.VARIANT);
      assertNotNull(WitTypeCategory.ENUM);
      assertNotNull(WitTypeCategory.FLAGS);
      assertNotNull(WitTypeCategory.LIST);
      assertNotNull(WitTypeCategory.OPTION);
      assertNotNull(WitTypeCategory.RESULT);
      assertNotNull(WitTypeCategory.TUPLE);
      assertNotNull(WitTypeCategory.RESOURCE);
    }
  }

  @Nested
  @DisplayName("Category Purpose Tests")
  class CategoryPurposeTests {

    @Test
    @DisplayName("PRIMITIVE should represent scalar types")
    void primitiveShouldRepresentScalarTypes() {
      // PRIMITIVE is for bool, integers, floats, char, string
      assertEquals("PRIMITIVE", WitTypeCategory.PRIMITIVE.name());
    }

    @Test
    @DisplayName("RECORD should represent record types with named fields")
    void recordShouldRepresentRecordTypes() {
      assertEquals("RECORD", WitTypeCategory.RECORD.name());
    }

    @Test
    @DisplayName("VARIANT should represent variant types with discriminated unions")
    void variantShouldRepresentVariantTypes() {
      assertEquals("VARIANT", WitTypeCategory.VARIANT.name());
    }

    @Test
    @DisplayName("ENUM should represent enumeration types")
    void enumShouldRepresentEnumerationTypes() {
      assertEquals("ENUM", WitTypeCategory.ENUM.name());
    }

    @Test
    @DisplayName("FLAGS should represent flag types with boolean flags")
    void flagsShouldRepresentFlagTypes() {
      assertEquals("FLAGS", WitTypeCategory.FLAGS.name());
    }

    @Test
    @DisplayName("LIST should represent list types with homogeneous elements")
    void listShouldRepresentListTypes() {
      assertEquals("LIST", WitTypeCategory.LIST.name());
    }

    @Test
    @DisplayName("OPTION should represent optional types")
    void optionShouldRepresentOptionalTypes() {
      assertEquals("OPTION", WitTypeCategory.OPTION.name());
    }

    @Test
    @DisplayName("RESULT should represent result types for success/error")
    void resultShouldRepresentResultTypes() {
      assertEquals("RESULT", WitTypeCategory.RESULT.name());
    }

    @Test
    @DisplayName("TUPLE should represent tuple types with heterogeneous elements")
    void tupleShouldRepresentTupleTypes() {
      assertEquals("TUPLE", WitTypeCategory.TUPLE.name());
    }

    @Test
    @DisplayName("RESOURCE should represent resource types with opaque handles")
    void resourceShouldRepresentResourceTypes() {
      assertEquals("RESOURCE", WitTypeCategory.RESOURCE.name());
    }
  }

  @Nested
  @DisplayName("valueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return correct enum for valid names")
    void valueOfShouldReturnCorrectEnum() {
      assertEquals(WitTypeCategory.PRIMITIVE, WitTypeCategory.valueOf("PRIMITIVE"));
      assertEquals(WitTypeCategory.RECORD, WitTypeCategory.valueOf("RECORD"));
      assertEquals(WitTypeCategory.VARIANT, WitTypeCategory.valueOf("VARIANT"));
      assertEquals(WitTypeCategory.ENUM, WitTypeCategory.valueOf("ENUM"));
      assertEquals(WitTypeCategory.FLAGS, WitTypeCategory.valueOf("FLAGS"));
      assertEquals(WitTypeCategory.LIST, WitTypeCategory.valueOf("LIST"));
      assertEquals(WitTypeCategory.OPTION, WitTypeCategory.valueOf("OPTION"));
      assertEquals(WitTypeCategory.RESULT, WitTypeCategory.valueOf("RESULT"));
      assertEquals(WitTypeCategory.TUPLE, WitTypeCategory.valueOf("TUPLE"));
      assertEquals(WitTypeCategory.RESOURCE, WitTypeCategory.valueOf("RESOURCE"));
    }
  }

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("ordinals should be consistent")
    void ordinalsShouldBeConsistent() {
      final var values = WitTypeCategory.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal());
      }
    }
  }
}
