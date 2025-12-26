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

package ai.tegmentum.wasmtime4j.jni.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.TableType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JniTableType} class.
 *
 * <p>This test class verifies the JNI implementation of TableType interface
 * for WebAssembly table types.
 */
@DisplayName("JniTableType Tests")
class JniTableTypeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniTableType should be final class")
    void shouldBeFinalClass() {
      assertTrue(java.lang.reflect.Modifier.isFinal(JniTableType.class.getModifiers()),
          "JniTableType should be final");
    }

    @Test
    @DisplayName("JniTableType should implement TableType")
    void shouldImplementTableType() {
      assertTrue(TableType.class.isAssignableFrom(JniTableType.class),
          "JniTableType should implement TableType");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should create with FUNCREF element type")
    void constructorShouldCreateWithFuncrefElementType() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 0, null);
      assertNotNull(tableType, "TableType should not be null");
      assertEquals(WasmValueType.FUNCREF, tableType.getElementType(), "Element type should be FUNCREF");
    }

    @Test
    @DisplayName("Constructor should create with EXTERNREF element type")
    void constructorShouldCreateWithExternrefElementType() {
      final JniTableType tableType = new JniTableType(WasmValueType.EXTERNREF, 0, null);
      assertEquals(WasmValueType.EXTERNREF, tableType.getElementType(), "Element type should be EXTERNREF");
    }

    @Test
    @DisplayName("Constructor should create with minimum only")
    void constructorShouldCreateWithMinimumOnly() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 10, null);
      assertEquals(10, tableType.getMinimum(), "Minimum should be 10");
      assertFalse(tableType.getMaximum().isPresent(), "Maximum should be empty");
    }

    @Test
    @DisplayName("Constructor should create with minimum and maximum")
    void constructorShouldCreateWithMinimumAndMaximum() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 10, 100L);
      assertEquals(10, tableType.getMinimum(), "Minimum should be 10");
      assertTrue(tableType.getMaximum().isPresent(), "Maximum should be present");
      assertEquals(100L, tableType.getMaximum().get(), "Maximum should be 100");
    }

    @Test
    @DisplayName("Constructor should accept zero minimum")
    void constructorShouldAcceptZeroMinimum() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 0, null);
      assertEquals(0, tableType.getMinimum(), "Minimum should be 0");
    }

    @Test
    @DisplayName("Constructor should throw for null element type")
    void constructorShouldThrowForNullElementType() {
      assertThrows(IllegalArgumentException.class, () ->
              new JniTableType(null, 0, null),
          "Should throw for null element type");
    }

    @Test
    @DisplayName("Constructor should throw for negative minimum")
    void constructorShouldThrowForNegativeMinimum() {
      assertThrows(IllegalArgumentException.class, () ->
              new JniTableType(WasmValueType.FUNCREF, -1, null),
          "Should throw for negative minimum");
    }

    @Test
    @DisplayName("Constructor should throw when maximum less than minimum")
    void constructorShouldThrowWhenMaximumLessThanMinimum() {
      assertThrows(IllegalArgumentException.class, () ->
              new JniTableType(WasmValueType.FUNCREF, 100, 50L),
          "Should throw when maximum < minimum");
    }

    @Test
    @DisplayName("Constructor should throw for non-reference element type")
    void constructorShouldThrowForNonReferenceElementType() {
      assertThrows(IllegalArgumentException.class, () ->
              new JniTableType(WasmValueType.I32, 0, null),
          "Should throw for non-reference element type (I32)");
    }

    @Test
    @DisplayName("Constructor should throw for I64 element type")
    void constructorShouldThrowForI64ElementType() {
      assertThrows(IllegalArgumentException.class, () ->
              new JniTableType(WasmValueType.I64, 0, null),
          "Should throw for non-reference element type (I64)");
    }

    @Test
    @DisplayName("Constructor should throw for F32 element type")
    void constructorShouldThrowForF32ElementType() {
      assertThrows(IllegalArgumentException.class, () ->
              new JniTableType(WasmValueType.F32, 0, null),
          "Should throw for non-reference element type (F32)");
    }

    @Test
    @DisplayName("Constructor should throw for F64 element type")
    void constructorShouldThrowForF64ElementType() {
      assertThrows(IllegalArgumentException.class, () ->
              new JniTableType(WasmValueType.F64, 0, null),
          "Should throw for non-reference element type (F64)");
    }

    @Test
    @DisplayName("Constructor should throw for V128 element type")
    void constructorShouldThrowForV128ElementType() {
      assertThrows(IllegalArgumentException.class, () ->
              new JniTableType(WasmValueType.V128, 0, null),
          "Should throw for non-reference element type (V128)");
    }

    @Test
    @DisplayName("Constructor should accept maximum equal to minimum")
    void constructorShouldAcceptMaximumEqualToMinimum() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 10, 10L);
      assertEquals(10, tableType.getMinimum(), "Minimum should be 10");
      assertEquals(10L, tableType.getMaximum().get(), "Maximum should be 10");
    }
  }

  @Nested
  @DisplayName("getKind Tests")
  class GetKindTests {

    @Test
    @DisplayName("getKind should return TABLE")
    void getKindShouldReturnTable() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 0, null);
      assertEquals(WasmTypeKind.TABLE, tableType.getKind(),
          "Kind should be TABLE");
    }
  }

  @Nested
  @DisplayName("Equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("Same instance should be equal")
    void sameInstanceShouldBeEqual() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 10, 100L);
      assertEquals(tableType, tableType, "Same instance should be equal");
    }

    @Test
    @DisplayName("Equal values should be equal")
    void equalValuesShouldBeEqual() {
      final JniTableType tableType1 = new JniTableType(WasmValueType.FUNCREF, 10, 100L);
      final JniTableType tableType2 = new JniTableType(WasmValueType.FUNCREF, 10, 100L);
      assertEquals(tableType1, tableType2, "Equal values should be equal");
    }

    @Test
    @DisplayName("Different element type should not be equal")
    void differentElementTypeShouldNotBeEqual() {
      final JniTableType tableType1 = new JniTableType(WasmValueType.FUNCREF, 10, null);
      final JniTableType tableType2 = new JniTableType(WasmValueType.EXTERNREF, 10, null);
      assertNotEquals(tableType1, tableType2, "Different element type should not be equal");
    }

    @Test
    @DisplayName("Different minimum should not be equal")
    void differentMinimumShouldNotBeEqual() {
      final JniTableType tableType1 = new JniTableType(WasmValueType.FUNCREF, 10, null);
      final JniTableType tableType2 = new JniTableType(WasmValueType.FUNCREF, 20, null);
      assertNotEquals(tableType1, tableType2, "Different minimum should not be equal");
    }

    @Test
    @DisplayName("Different maximum should not be equal")
    void differentMaximumShouldNotBeEqual() {
      final JniTableType tableType1 = new JniTableType(WasmValueType.FUNCREF, 10, 100L);
      final JniTableType tableType2 = new JniTableType(WasmValueType.FUNCREF, 10, 200L);
      assertNotEquals(tableType1, tableType2, "Different maximum should not be equal");
    }

    @Test
    @DisplayName("With and without maximum should not be equal")
    void withAndWithoutMaximumShouldNotBeEqual() {
      final JniTableType tableType1 = new JniTableType(WasmValueType.FUNCREF, 10, 100L);
      final JniTableType tableType2 = new JniTableType(WasmValueType.FUNCREF, 10, null);
      assertNotEquals(tableType1, tableType2, "With and without maximum should not be equal");
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 0, null);
      assertFalse(tableType.equals(null), "Should not be equal to null");
    }

    @Test
    @DisplayName("Should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 0, null);
      assertFalse(tableType.equals("string"), "Should not be equal to different type");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("Equal objects should have equal hashCodes")
    void equalObjectsShouldHaveEqualHashCodes() {
      final JniTableType tableType1 = new JniTableType(WasmValueType.FUNCREF, 10, 100L);
      final JniTableType tableType2 = new JniTableType(WasmValueType.FUNCREF, 10, 100L);
      assertEquals(tableType1.hashCode(), tableType2.hashCode(),
          "Equal objects should have equal hashCodes");
    }

    @Test
    @DisplayName("HashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final JniTableType tableType = new JniTableType(WasmValueType.EXTERNREF, 5, 50L);
      final int hash1 = tableType.hashCode();
      final int hash2 = tableType.hashCode();
      assertEquals(hash1, hash2, "HashCode should be consistent");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include element type")
    void toStringShouldIncludeElementType() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 0, null);
      final String str = tableType.toString();
      assertTrue(str.contains("FUNCREF") || str.contains("funcref"),
          "toString should include element type");
    }

    @Test
    @DisplayName("toString should include minimum")
    void toStringShouldIncludeMinimum() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 42, null);
      final String str = tableType.toString();
      assertTrue(str.contains("42") || str.contains("min"), "toString should include minimum");
    }

    @Test
    @DisplayName("toString should include maximum when present")
    void toStringShouldIncludeMaximumWhenPresent() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 0, 100L);
      final String str = tableType.toString();
      assertTrue(str.contains("100") || str.contains("max"), "toString should include maximum");
    }

    @Test
    @DisplayName("toString should indicate unlimited when no maximum")
    void toStringShouldIndicateUnlimitedWhenNoMaximum() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 0, null);
      final String str = tableType.toString();
      assertTrue(str.contains("unlimited") || str.contains("null") || str.contains("max="),
          "toString should indicate unlimited");
    }

    @Test
    @DisplayName("toString should include TableType")
    void toStringShouldIncludeTableType() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 0, null);
      final String str = tableType.toString();
      assertTrue(str.contains("TableType"), "toString should include TableType");
    }
  }

  @Nested
  @DisplayName("fromNative Tests")
  class FromNativeTests {

    @Test
    @DisplayName("fromNative should throw for zero handle")
    void fromNativeShouldThrowForZeroHandle() {
      assertThrows(IllegalArgumentException.class, () ->
              JniTableType.fromNative(0),
          "Should throw for zero handle");
    }

    @Test
    @DisplayName("fromNative should throw for negative handle")
    void fromNativeShouldThrowForNegativeHandle() {
      assertThrows(IllegalArgumentException.class, () ->
              JniTableType.fromNative(-1),
          "Should throw for negative handle");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Large table should work")
    void largeTableShouldWork() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 1000, 1000000L);
      assertEquals(1000, tableType.getMinimum(), "Large minimum should work");
      assertEquals(1000000L, tableType.getMaximum().get(), "Large maximum should work");
    }

    @Test
    @DisplayName("FUNCREF table with bounds should work")
    void funcrefTableWithBoundsShouldWork() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 10, 100L);
      assertEquals(WasmValueType.FUNCREF, tableType.getElementType(), "Element type should be FUNCREF");
      assertEquals(10, tableType.getMinimum(), "Minimum should be 10");
      assertEquals(100L, tableType.getMaximum().get(), "Maximum should be 100");
      assertEquals(WasmTypeKind.TABLE, tableType.getKind(), "Kind should be TABLE");
    }

    @Test
    @DisplayName("EXTERNREF table with bounds should work")
    void externrefTableWithBoundsShouldWork() {
      final JniTableType tableType = new JniTableType(WasmValueType.EXTERNREF, 5, 50L);
      assertEquals(WasmValueType.EXTERNREF, tableType.getElementType(), "Element type should be EXTERNREF");
      assertEquals(5, tableType.getMinimum(), "Minimum should be 5");
      assertEquals(50L, tableType.getMaximum().get(), "Maximum should be 50");
    }
  }
}
