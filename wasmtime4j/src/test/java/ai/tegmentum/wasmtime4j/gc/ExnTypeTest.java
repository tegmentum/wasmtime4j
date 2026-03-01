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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.DefaultTagType;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExnType} class.
 *
 * <p>ExnType wraps a TagType and provides field-level access for GC exception types.
 */
@DisplayName("ExnType Tests")
class ExnTypeTest {

  private DefaultTagType createTagType(final WasmValueType... paramTypes) {
    final FunctionType funcType = new FunctionType(paramTypes, new WasmValueType[0]);
    return new DefaultTagType(funcType);
  }

  @Nested
  @DisplayName("Construction Tests")
  class ConstructionTests {

    @Test
    @DisplayName("should create ExnType from TagType")
    void shouldCreateFromTagType() {
      final DefaultTagType tag = createTagType(WasmValueType.I32, WasmValueType.I64);
      final ExnType exnType = new ExnType(tag);
      assertNotNull(exnType);
      assertEquals(tag, exnType.tagType());
    }

    @Test
    @DisplayName("should throw on null TagType")
    void shouldThrowOnNullTagType() {
      assertThrows(IllegalArgumentException.class, () -> new ExnType(null));
    }
  }

  @Nested
  @DisplayName("Field Access Tests")
  class FieldAccessTests {

    @Test
    @DisplayName("should return correct field count")
    void shouldReturnCorrectFieldCount() {
      final ExnType exnType =
          new ExnType(createTagType(WasmValueType.I32, WasmValueType.F64, WasmValueType.I64));
      assertEquals(3, exnType.fieldCount());
    }

    @Test
    @DisplayName("should return correct field by index")
    void shouldReturnCorrectFieldByIndex() {
      final ExnType exnType = new ExnType(createTagType(WasmValueType.I32, WasmValueType.F64));
      assertEquals(WasmValueType.I32, exnType.field(0));
      assertEquals(WasmValueType.F64, exnType.field(1));
    }

    @Test
    @DisplayName("should return all fields")
    void shouldReturnAllFields() {
      final ExnType exnType = new ExnType(createTagType(WasmValueType.I32, WasmValueType.I64));
      final List<WasmValueType> fields = exnType.fields();
      assertEquals(2, fields.size());
      assertEquals(WasmValueType.I32, fields.get(0));
      assertEquals(WasmValueType.I64, fields.get(1));
    }

    @Test
    @DisplayName("should throw on invalid index")
    void shouldThrowOnInvalidIndex() {
      final ExnType exnType = new ExnType(createTagType(WasmValueType.I32));
      assertThrows(IndexOutOfBoundsException.class, () -> exnType.field(1));
      assertThrows(IndexOutOfBoundsException.class, () -> exnType.field(-1));
    }

    @Test
    @DisplayName("empty tag should have zero fields")
    void emptyTagShouldHaveZeroFields() {
      final ExnType exnType = new ExnType(createTagType());
      assertEquals(0, exnType.fieldCount());
      assertTrue(exnType.fields().isEmpty());
    }
  }

  @Nested
  @DisplayName("Matches Tests")
  class MatchesTests {

    @Test
    @DisplayName("same tag type should match")
    void sameTagTypeShouldMatch() {
      final DefaultTagType tag = createTagType(WasmValueType.I32);
      final ExnType a = new ExnType(tag);
      final ExnType b = new ExnType(tag);
      assertTrue(a.matches(b));
    }

    @Test
    @DisplayName("different field count should not match")
    void differentFieldCountShouldNotMatch() {
      final ExnType a = new ExnType(createTagType(WasmValueType.I32));
      final ExnType b = new ExnType(createTagType(WasmValueType.I32, WasmValueType.I64));
      assertFalse(a.matches(b));
    }

    @Test
    @DisplayName("different field types should not match")
    void differentFieldTypesShouldNotMatch() {
      final ExnType a = new ExnType(createTagType(WasmValueType.I32));
      final ExnType b = new ExnType(createTagType(WasmValueType.F64));
      assertFalse(a.matches(b));
    }

    @Test
    @DisplayName("null should not match")
    void nullShouldNotMatch() {
      final ExnType exnType = new ExnType(createTagType(WasmValueType.I32));
      assertFalse(exnType.matches(null));
    }
  }
}
