/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for MemoryLayouts. */
class MemoryLayoutsTest {

  @Test
  void testValueKindConstants() {
    assertEquals(0, MemoryLayouts.WASM_I32);
    assertEquals(1, MemoryLayouts.WASM_I64);
    assertEquals(2, MemoryLayouts.WASM_F32);
    assertEquals(3, MemoryLayouts.WASM_F64);
    assertEquals(4, MemoryLayouts.WASM_V128);
    assertEquals(5, MemoryLayouts.WASM_ANYREF);
    assertEquals(6, MemoryLayouts.WASM_FUNCREF);
  }

  @Test
  void testGetValueSize() {
    assertEquals(4, MemoryLayouts.getValueSize(MemoryLayouts.WASM_I32));
    assertEquals(8, MemoryLayouts.getValueSize(MemoryLayouts.WASM_I64));
    assertEquals(4, MemoryLayouts.getValueSize(MemoryLayouts.WASM_F32));
    assertEquals(8, MemoryLayouts.getValueSize(MemoryLayouts.WASM_F64));
    assertTrue(MemoryLayouts.getValueSize(MemoryLayouts.WASM_ANYREF) > 0);
    assertTrue(MemoryLayouts.getValueSize(MemoryLayouts.WASM_FUNCREF) > 0);
  }

  @Test
  void testGetValueSizeInvalidKind() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          MemoryLayouts.getValueSize(999);
        });
  }

  @Test
  void testIsReferenceType() {
    assertFalse(MemoryLayouts.isReferenceType(MemoryLayouts.WASM_I32));
    assertFalse(MemoryLayouts.isReferenceType(MemoryLayouts.WASM_I64));
    assertFalse(MemoryLayouts.isReferenceType(MemoryLayouts.WASM_F32));
    assertFalse(MemoryLayouts.isReferenceType(MemoryLayouts.WASM_F64));
    assertTrue(MemoryLayouts.isReferenceType(MemoryLayouts.WASM_ANYREF));
    assertTrue(MemoryLayouts.isReferenceType(MemoryLayouts.WASM_FUNCREF));
  }

  @Test
  void testIsNumericType() {
    assertTrue(MemoryLayouts.isNumericType(MemoryLayouts.WASM_I32));
    assertTrue(MemoryLayouts.isNumericType(MemoryLayouts.WASM_I64));
    assertTrue(MemoryLayouts.isNumericType(MemoryLayouts.WASM_F32));
    assertTrue(MemoryLayouts.isNumericType(MemoryLayouts.WASM_F64));
    assertFalse(MemoryLayouts.isNumericType(MemoryLayouts.WASM_ANYREF));
    assertFalse(MemoryLayouts.isNumericType(MemoryLayouts.WASM_FUNCREF));
  }

  @Test
  void testValueKindToString() {
    assertEquals("i32", MemoryLayouts.valueKindToString(MemoryLayouts.WASM_I32));
    assertEquals("i64", MemoryLayouts.valueKindToString(MemoryLayouts.WASM_I64));
    assertEquals("f32", MemoryLayouts.valueKindToString(MemoryLayouts.WASM_F32));
    assertEquals("f64", MemoryLayouts.valueKindToString(MemoryLayouts.WASM_F64));
    assertEquals("anyref", MemoryLayouts.valueKindToString(MemoryLayouts.WASM_ANYREF));
    assertEquals("funcref", MemoryLayouts.valueKindToString(MemoryLayouts.WASM_FUNCREF));
    assertTrue(MemoryLayouts.valueKindToString(999).startsWith("unknown"));
  }

  @Test
  void testExternKindToString() {
    assertEquals("function", MemoryLayouts.externKindToString(MemoryLayouts.WASM_EXTERN_FUNC));
    assertEquals("global", MemoryLayouts.externKindToString(MemoryLayouts.WASM_EXTERN_GLOBAL));
    assertEquals("table", MemoryLayouts.externKindToString(MemoryLayouts.WASM_EXTERN_TABLE));
    assertEquals("memory", MemoryLayouts.externKindToString(MemoryLayouts.WASM_EXTERN_MEMORY));
    assertTrue(MemoryLayouts.externKindToString(999).startsWith("unknown"));
  }

  @Test
  void testMutabilityConstants() {
    assertEquals(0, MemoryLayouts.WASM_CONST);
    assertEquals(1, MemoryLayouts.WASM_VAR);
  }

  @Test
  void testExternalTypeConstants() {
    assertEquals(0, MemoryLayouts.WASM_EXTERN_FUNC);
    assertEquals(1, MemoryLayouts.WASM_EXTERN_GLOBAL);
    assertEquals(2, MemoryLayouts.WASM_EXTERN_TABLE);
    assertEquals(3, MemoryLayouts.WASM_EXTERN_MEMORY);
  }

  @Test
  void testMemoryLayoutsNotNull() {
    assertNotNull(MemoryLayouts.C_CHAR);
    assertNotNull(MemoryLayouts.C_INT);
    assertNotNull(MemoryLayouts.C_SIZE_T);
    assertNotNull(MemoryLayouts.C_POINTER);
    assertNotNull(MemoryLayouts.C_BOOL);
    assertNotNull(MemoryLayouts.WASM_ERROR_CODE);
    assertNotNull(MemoryLayouts.WASMTIME_ERROR);
    assertNotNull(MemoryLayouts.WASM_VAL);
    assertNotNull(MemoryLayouts.WASM_BYTE_VEC);
    assertNotNull(MemoryLayouts.WASM_FUNCTYPE);
    assertNotNull(MemoryLayouts.WASM_GLOBALTYPE);
    assertNotNull(MemoryLayouts.WASM_LIMITS);
    assertNotNull(MemoryLayouts.WASM_MEMORYTYPE);
    assertNotNull(MemoryLayouts.WASM_TABLETYPE);
    assertNotNull(MemoryLayouts.WASM_IMPORTTYPE);
    assertNotNull(MemoryLayouts.WASM_EXPORTTYPE);
    assertNotNull(MemoryLayouts.WASMTIME_CONFIG);
  }

  @Test
  void testVarHandlesNotNull() {
    assertNotNull(MemoryLayouts.WASMTIME_ERROR_CODE);
    assertNotNull(MemoryLayouts.WASMTIME_ERROR_MESSAGE);
    assertNotNull(MemoryLayouts.WASMTIME_ERROR_MESSAGE_LEN);
    assertNotNull(MemoryLayouts.WASM_VAL_KIND);
    assertNotNull(MemoryLayouts.WASM_VAL_I32);
    assertNotNull(MemoryLayouts.WASM_VAL_I64);
    assertNotNull(MemoryLayouts.WASM_VAL_F32);
    assertNotNull(MemoryLayouts.WASM_VAL_F64);
    assertNotNull(MemoryLayouts.WASM_VAL_REF);
    assertNotNull(MemoryLayouts.WASM_BYTE_VEC_SIZE);
    assertNotNull(MemoryLayouts.WASM_BYTE_VEC_DATA);
  }
}
