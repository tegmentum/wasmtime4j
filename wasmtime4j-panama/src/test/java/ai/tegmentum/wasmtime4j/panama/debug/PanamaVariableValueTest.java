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

package ai.tegmentum.wasmtime4j.panama.debug;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link PanamaVariableValue} class. */
@DisplayName("PanamaVariableValue Tests")
public class PanamaVariableValueTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaVariableValueTest.class.getName());

  @Test
  @DisplayName("Create i32 value")
  public void testI32Value() {
    LOGGER.info("Testing i32 value creation");

    final PanamaVariableValue value = PanamaVariableValue.i32(42);

    assertNotNull(value, "Value should not be null");
    assertEquals(PanamaVariableValue.ValueType.I32, value.getType(), "Type should be I32");
    assertEquals(42, value.asI32(), "Value should be 42");
    assertFalse(value.isNull(), "I32 value should not be null");

    LOGGER.info("i32 value test passed: " + value);
  }

  @Test
  @DisplayName("Create i32 value with negative number")
  public void testI32ValueNegative() {
    LOGGER.info("Testing i32 value with negative number");

    final PanamaVariableValue value = PanamaVariableValue.i32(-12345);

    assertEquals(-12345, value.asI32(), "Value should be -12345");

    LOGGER.info("i32 negative value test passed: " + value);
  }

  @Test
  @DisplayName("Create i64 value")
  public void testI64Value() {
    LOGGER.info("Testing i64 value creation");

    final PanamaVariableValue value = PanamaVariableValue.i64(Long.MAX_VALUE);

    assertNotNull(value, "Value should not be null");
    assertEquals(PanamaVariableValue.ValueType.I64, value.getType(), "Type should be I64");
    assertEquals(Long.MAX_VALUE, value.asI64(), "Value should be Long.MAX_VALUE");

    LOGGER.info("i64 value test passed: " + value);
  }

  @Test
  @DisplayName("Create f32 value")
  public void testF32Value() {
    LOGGER.info("Testing f32 value creation");

    final PanamaVariableValue value = PanamaVariableValue.f32(3.14f);

    assertNotNull(value, "Value should not be null");
    assertEquals(PanamaVariableValue.ValueType.F32, value.getType(), "Type should be F32");
    assertEquals(3.14f, value.asF32(), 0.001f, "Value should be approximately 3.14");

    LOGGER.info("f32 value test passed: " + value);
  }

  @Test
  @DisplayName("Create f64 value")
  public void testF64Value() {
    LOGGER.info("Testing f64 value creation");

    final PanamaVariableValue value = PanamaVariableValue.f64(Math.PI);

    assertNotNull(value, "Value should not be null");
    assertEquals(PanamaVariableValue.ValueType.F64, value.getType(), "Type should be F64");
    assertEquals(Math.PI, value.asF64(), 0.0001, "Value should be approximately PI");

    LOGGER.info("f64 value test passed: " + value);
  }

  @Test
  @DisplayName("Create v128 value")
  public void testV128Value() {
    LOGGER.info("Testing v128 value creation");

    final byte[] bytes = new byte[16];
    for (int i = 0; i < 16; i++) {
      bytes[i] = (byte) i;
    }

    final PanamaVariableValue value = PanamaVariableValue.v128(bytes);

    assertNotNull(value, "Value should not be null");
    assertEquals(PanamaVariableValue.ValueType.V128, value.getType(), "Type should be V128");
    assertArrayEquals(bytes, value.asV128(), "Bytes should match");

    LOGGER.info("v128 value test passed: " + value);
  }

  @Test
  @DisplayName("v128 value is defensive copy")
  public void testV128DefensiveCopy() {
    LOGGER.info("Testing v128 defensive copy");

    final byte[] bytes = new byte[16];
    for (int i = 0; i < 16; i++) {
      bytes[i] = (byte) i;
    }

    final PanamaVariableValue value = PanamaVariableValue.v128(bytes);

    // Modify original array
    bytes[0] = 99;

    // Value should not be affected
    final byte[] retrieved = value.asV128();
    assertEquals(0, retrieved[0], "Value should not be affected by original array modification");

    // Modify retrieved array
    retrieved[1] = 99;

    // Value should not be affected
    final byte[] retrieved2 = value.asV128();
    assertEquals(1, retrieved2[1], "Value should not be affected by retrieved array modification");

    LOGGER.info("v128 defensive copy test passed");
  }

  @Test
  @DisplayName("v128 rejects invalid size")
  public void testV128InvalidSize() {
    LOGGER.info("Testing v128 invalid size rejection");

    assertThrows(
        IllegalArgumentException.class,
        () -> PanamaVariableValue.v128(new byte[8]),
        "Should reject array smaller than 16 bytes");

    assertThrows(
        IllegalArgumentException.class,
        () -> PanamaVariableValue.v128(new byte[32]),
        "Should reject array larger than 16 bytes");

    assertThrows(
        IllegalArgumentException.class,
        () -> PanamaVariableValue.v128(null),
        "Should reject null array");

    LOGGER.info("v128 invalid size rejection test passed");
  }

  @Test
  @DisplayName("Create funcref value")
  public void testFuncRefValue() {
    LOGGER.info("Testing funcref value creation");

    final PanamaVariableValue value = PanamaVariableValue.funcRef(5);

    assertNotNull(value, "Value should not be null");
    assertEquals(PanamaVariableValue.ValueType.FUNCREF, value.getType(), "Type should be FUNCREF");
    assertEquals(Integer.valueOf(5), value.asFuncRef(), "Function index should be 5");
    assertFalse(value.isNull(), "Non-null funcref should not be null");

    LOGGER.info("funcref value test passed: " + value);
  }

  @Test
  @DisplayName("Create null funcref value")
  public void testNullFuncRefValue() {
    LOGGER.info("Testing null funcref value creation");

    final PanamaVariableValue value = PanamaVariableValue.funcRef(null);

    assertNotNull(value, "Value object should not be null");
    assertEquals(PanamaVariableValue.ValueType.FUNCREF, value.getType(), "Type should be FUNCREF");
    assertNull(value.asFuncRef(), "Function index should be null");
    assertTrue(value.isNull(), "Null funcref should be null");

    LOGGER.info("null funcref value test passed: " + value);
  }

  @Test
  @DisplayName("Create externref value")
  public void testExternRefValue() {
    LOGGER.info("Testing externref value creation");

    final PanamaVariableValue value = PanamaVariableValue.externRef(0x1234567890L);

    assertNotNull(value, "Value should not be null");
    assertEquals(
        PanamaVariableValue.ValueType.EXTERNREF, value.getType(), "Type should be EXTERNREF");
    assertEquals(Long.valueOf(0x1234567890L), value.asExternRef(), "Reference should match");
    assertFalse(value.isNull(), "Non-null externref should not be null");

    LOGGER.info("externref value test passed: " + value);
  }

  @Test
  @DisplayName("Create null externref value")
  public void testNullExternRefValue() {
    LOGGER.info("Testing null externref value creation");

    final PanamaVariableValue value = PanamaVariableValue.externRef(null);

    assertNotNull(value, "Value object should not be null");
    assertEquals(
        PanamaVariableValue.ValueType.EXTERNREF, value.getType(), "Type should be EXTERNREF");
    assertNull(value.asExternRef(), "Reference should be null");
    assertTrue(value.isNull(), "Null externref should be null");

    LOGGER.info("null externref value test passed: " + value);
  }

  @Test
  @DisplayName("Create memory value")
  public void testMemoryValue() {
    LOGGER.info("Testing memory value creation");

    final PanamaVariableValue value = PanamaVariableValue.memory(0x1000L, 4096L);

    assertNotNull(value, "Value should not be null");
    assertEquals(PanamaVariableValue.ValueType.MEMORY, value.getType(), "Type should be MEMORY");
    assertEquals(0x1000L, value.getMemoryAddress(), "Memory address should match");
    assertEquals(4096L, value.getMemorySize(), "Memory size should match");

    LOGGER.info("memory value test passed: " + value);
  }

  @Test
  @DisplayName("Create complex value")
  public void testComplexValue() {
    LOGGER.info("Testing complex value creation");

    final String json = "{\"type\": \"struct\", \"fields\": [{\"name\": \"x\", \"value\": 10}]}";
    final PanamaVariableValue value = PanamaVariableValue.complex(json);

    assertNotNull(value, "Value should not be null");
    assertEquals(PanamaVariableValue.ValueType.COMPLEX, value.getType(), "Type should be COMPLEX");
    assertEquals(json, value.asComplex(), "JSON should match");

    LOGGER.info("complex value test passed: " + value);
  }

  @Test
  @DisplayName("Type mismatch throws IllegalStateException for i32")
  public void testTypeMismatchI32() {
    LOGGER.info("Testing type mismatch for i32");

    final PanamaVariableValue value = PanamaVariableValue.i64(100L);

    assertThrows(IllegalStateException.class, value::asI32, "Should throw on type mismatch");

    LOGGER.info("Type mismatch i32 test passed");
  }

  @Test
  @DisplayName("Type mismatch throws IllegalStateException for i64")
  public void testTypeMismatchI64() {
    LOGGER.info("Testing type mismatch for i64");

    final PanamaVariableValue value = PanamaVariableValue.i32(100);

    assertThrows(IllegalStateException.class, value::asI64, "Should throw on type mismatch");

    LOGGER.info("Type mismatch i64 test passed");
  }

  @Test
  @DisplayName("Type mismatch throws IllegalStateException for f32")
  public void testTypeMismatchF32() {
    LOGGER.info("Testing type mismatch for f32");

    final PanamaVariableValue value = PanamaVariableValue.f64(3.14);

    assertThrows(IllegalStateException.class, value::asF32, "Should throw on type mismatch");

    LOGGER.info("Type mismatch f32 test passed");
  }

  @Test
  @DisplayName("Type mismatch throws IllegalStateException for f64")
  public void testTypeMismatchF64() {
    LOGGER.info("Testing type mismatch for f64");

    final PanamaVariableValue value = PanamaVariableValue.f32(3.14f);

    assertThrows(IllegalStateException.class, value::asF64, "Should throw on type mismatch");

    LOGGER.info("Type mismatch f64 test passed");
  }

  @Test
  @DisplayName("Type mismatch throws IllegalStateException for v128")
  public void testTypeMismatchV128() {
    LOGGER.info("Testing type mismatch for v128");

    final PanamaVariableValue value = PanamaVariableValue.i32(100);

    assertThrows(IllegalStateException.class, value::asV128, "Should throw on type mismatch");

    LOGGER.info("Type mismatch v128 test passed");
  }

  @Test
  @DisplayName("Type mismatch throws IllegalStateException for funcRef")
  public void testTypeMismatchFuncRef() {
    LOGGER.info("Testing type mismatch for funcRef");

    final PanamaVariableValue value = PanamaVariableValue.i32(100);

    assertThrows(IllegalStateException.class, value::asFuncRef, "Should throw on type mismatch");

    LOGGER.info("Type mismatch funcRef test passed");
  }

  @Test
  @DisplayName("Type mismatch throws IllegalStateException for externRef")
  public void testTypeMismatchExternRef() {
    LOGGER.info("Testing type mismatch for externRef");

    final PanamaVariableValue value = PanamaVariableValue.i32(100);

    assertThrows(IllegalStateException.class, value::asExternRef, "Should throw on type mismatch");

    LOGGER.info("Type mismatch externRef test passed");
  }

  @Test
  @DisplayName("Type mismatch throws IllegalStateException for memory address")
  public void testTypeMismatchMemoryAddress() {
    LOGGER.info("Testing type mismatch for memory address");

    final PanamaVariableValue value = PanamaVariableValue.i32(100);

    assertThrows(
        IllegalStateException.class, value::getMemoryAddress, "Should throw on type mismatch");

    LOGGER.info("Type mismatch memory address test passed");
  }

  @Test
  @DisplayName("Type mismatch throws IllegalStateException for memory size")
  public void testTypeMismatchMemorySize() {
    LOGGER.info("Testing type mismatch for memory size");

    final PanamaVariableValue value = PanamaVariableValue.i32(100);

    assertThrows(
        IllegalStateException.class, value::getMemorySize, "Should throw on type mismatch");

    LOGGER.info("Type mismatch memory size test passed");
  }

  @Test
  @DisplayName("Type mismatch throws IllegalStateException for complex")
  public void testTypeMismatchComplex() {
    LOGGER.info("Testing type mismatch for complex");

    final PanamaVariableValue value = PanamaVariableValue.i32(100);

    assertThrows(IllegalStateException.class, value::asComplex, "Should throw on type mismatch");

    LOGGER.info("Type mismatch complex test passed");
  }

  @Test
  @DisplayName("Test toString for all types")
  public void testToString() {
    LOGGER.info("Testing toString for all types");

    assertEquals("i32(42)", PanamaVariableValue.i32(42).toString());
    assertEquals("i64(100)", PanamaVariableValue.i64(100L).toString());
    assertTrue(PanamaVariableValue.f32(3.14f).toString().startsWith("f32("));
    assertTrue(PanamaVariableValue.f64(3.14).toString().startsWith("f64("));
    assertTrue(PanamaVariableValue.v128(new byte[16]).toString().startsWith("v128("));
    assertEquals("funcref(5)", PanamaVariableValue.funcRef(5).toString());
    assertEquals("funcref(null)", PanamaVariableValue.funcRef(null).toString());
    assertEquals("externref(100)", PanamaVariableValue.externRef(100L).toString());
    assertEquals("externref(null)", PanamaVariableValue.externRef(null).toString());
    assertTrue(PanamaVariableValue.memory(0x1000L, 4096L).toString().startsWith("memory("));
    assertTrue(PanamaVariableValue.complex("{}").toString().startsWith("complex("));

    LOGGER.info("toString test passed");
  }

  @Test
  @DisplayName("Test equality for same values")
  public void testEqualitySameValues() {
    LOGGER.info("Testing equality for same values");

    assertEquals(PanamaVariableValue.i32(42), PanamaVariableValue.i32(42));
    assertEquals(PanamaVariableValue.i64(100L), PanamaVariableValue.i64(100L));
    assertEquals(PanamaVariableValue.f32(3.14f), PanamaVariableValue.f32(3.14f));
    assertEquals(PanamaVariableValue.f64(3.14), PanamaVariableValue.f64(3.14));
    assertEquals(PanamaVariableValue.funcRef(5), PanamaVariableValue.funcRef(5));
    assertEquals(PanamaVariableValue.externRef(100L), PanamaVariableValue.externRef(100L));
    assertEquals(PanamaVariableValue.funcRef(null), PanamaVariableValue.funcRef(null));

    final byte[] bytes = new byte[16];
    assertEquals(PanamaVariableValue.v128(bytes), PanamaVariableValue.v128(bytes));
    assertEquals(
        PanamaVariableValue.memory(0x1000L, 4096L), PanamaVariableValue.memory(0x1000L, 4096L));

    LOGGER.info("Equality test passed");
  }

  @Test
  @DisplayName("Test inequality for different values")
  public void testInequalityDifferentValues() {
    LOGGER.info("Testing inequality for different values");

    assertNotEquals(PanamaVariableValue.i32(42), PanamaVariableValue.i32(43));
    assertNotEquals(PanamaVariableValue.i32(42), PanamaVariableValue.i64(42L));
    assertNotEquals(PanamaVariableValue.funcRef(5), PanamaVariableValue.funcRef(6));

    LOGGER.info("Inequality test passed");
  }

  @Test
  @DisplayName("Test hashCode consistency")
  public void testHashCodeConsistency() {
    LOGGER.info("Testing hashCode consistency");

    final PanamaVariableValue v1 = PanamaVariableValue.i32(42);
    final PanamaVariableValue v2 = PanamaVariableValue.i32(42);

    assertEquals(v1.hashCode(), v2.hashCode(), "Equal values should have same hashCode");

    final byte[] bytes = new byte[16];
    final PanamaVariableValue v3 = PanamaVariableValue.v128(bytes);
    final PanamaVariableValue v4 = PanamaVariableValue.v128(bytes);

    assertEquals(v3.hashCode(), v4.hashCode(), "Equal v128 values should have same hashCode");

    LOGGER.info("hashCode consistency test passed");
  }

  @Test
  @DisplayName("Test isNull for non-reference types")
  public void testIsNullForNonReferenceTypes() {
    LOGGER.info("Testing isNull for non-reference types");

    assertFalse(PanamaVariableValue.i32(0).isNull(), "i32 should never be null");
    assertFalse(PanamaVariableValue.i64(0L).isNull(), "i64 should never be null");
    assertFalse(PanamaVariableValue.f32(0.0f).isNull(), "f32 should never be null");
    assertFalse(PanamaVariableValue.f64(0.0).isNull(), "f64 should never be null");
    assertFalse(PanamaVariableValue.v128(new byte[16]).isNull(), "v128 should never be null");
    assertFalse(PanamaVariableValue.memory(0, 0).isNull(), "memory should never be null");
    assertFalse(PanamaVariableValue.complex("").isNull(), "complex should never be null");

    LOGGER.info("isNull for non-reference types test passed");
  }
}
