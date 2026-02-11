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

import ai.tegmentum.wasmtime4j.memory.Memory64TableOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Memory64TableOperations}.
 *
 * <p>Verifies enum structure, constants, field accessors (opcode, mnemonic,
 * returnsValue), fromOpcode, fromMnemonic, and validation methods.
 */
@DisplayName("Memory64TableOperations Tests")
class Memory64TableOperationsTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(Memory64TableOperations.class.isEnum(),
          "Memory64TableOperations should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 7 values")
    void shouldHaveExactValueCount() {
      assertEquals(7, Memory64TableOperations.values().length,
          "Memory64TableOperations should have exactly 7 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain all expected constants")
    void shouldContainAllExpectedConstants() {
      assertNotNull(Memory64TableOperations.TABLE_GET_64,
          "TABLE_GET_64 should exist");
      assertNotNull(Memory64TableOperations.TABLE_SET_64,
          "TABLE_SET_64 should exist");
      assertNotNull(Memory64TableOperations.TABLE_SIZE_64,
          "TABLE_SIZE_64 should exist");
      assertNotNull(Memory64TableOperations.TABLE_GROW_64,
          "TABLE_GROW_64 should exist");
      assertNotNull(Memory64TableOperations.TABLE_FILL_64,
          "TABLE_FILL_64 should exist");
      assertNotNull(Memory64TableOperations.TABLE_COPY_64,
          "TABLE_COPY_64 should exist");
      assertNotNull(Memory64TableOperations.TABLE_INIT_64,
          "TABLE_INIT_64 should exist");
    }
  }

  @Nested
  @DisplayName("GetOpcode Tests")
  class GetOpcodeTests {

    @Test
    @DisplayName("should have correct opcodes for all constants")
    void shouldHaveCorrectOpcodes() {
      assertEquals(0x25, Memory64TableOperations.TABLE_GET_64.getOpcode(),
          "TABLE_GET_64 opcode should be 0x25");
      assertEquals(0x26, Memory64TableOperations.TABLE_SET_64.getOpcode(),
          "TABLE_SET_64 opcode should be 0x26");
      assertEquals(0x27, Memory64TableOperations.TABLE_SIZE_64.getOpcode(),
          "TABLE_SIZE_64 opcode should be 0x27");
      assertEquals(0x28, Memory64TableOperations.TABLE_GROW_64.getOpcode(),
          "TABLE_GROW_64 opcode should be 0x28");
      assertEquals(0x29, Memory64TableOperations.TABLE_FILL_64.getOpcode(),
          "TABLE_FILL_64 opcode should be 0x29");
      assertEquals(0x2A, Memory64TableOperations.TABLE_COPY_64.getOpcode(),
          "TABLE_COPY_64 opcode should be 0x2A");
      assertEquals(0x2B, Memory64TableOperations.TABLE_INIT_64.getOpcode(),
          "TABLE_INIT_64 opcode should be 0x2B");
    }

    @Test
    @DisplayName("should have unique opcodes")
    void shouldHaveUniqueOpcodes() {
      final Set<Integer> opcodes = new HashSet<>();
      for (final Memory64TableOperations value : Memory64TableOperations.values()) {
        opcodes.add(value.getOpcode());
      }
      assertEquals(Memory64TableOperations.values().length, opcodes.size(),
          "All opcodes should be unique");
    }
  }

  @Nested
  @DisplayName("GetMnemonic Tests")
  class GetMnemonicTests {

    @Test
    @DisplayName("should have correct mnemonics for all constants")
    void shouldHaveCorrectMnemonics() {
      assertEquals("table.get64",
          Memory64TableOperations.TABLE_GET_64.getMnemonic(),
          "TABLE_GET_64 mnemonic should be 'table.get64'");
      assertEquals("table.set64",
          Memory64TableOperations.TABLE_SET_64.getMnemonic(),
          "TABLE_SET_64 mnemonic should be 'table.set64'");
      assertEquals("table.size64",
          Memory64TableOperations.TABLE_SIZE_64.getMnemonic(),
          "TABLE_SIZE_64 mnemonic should be 'table.size64'");
      assertEquals("table.grow64",
          Memory64TableOperations.TABLE_GROW_64.getMnemonic(),
          "TABLE_GROW_64 mnemonic should be 'table.grow64'");
      assertEquals("table.fill64",
          Memory64TableOperations.TABLE_FILL_64.getMnemonic(),
          "TABLE_FILL_64 mnemonic should be 'table.fill64'");
      assertEquals("table.copy64",
          Memory64TableOperations.TABLE_COPY_64.getMnemonic(),
          "TABLE_COPY_64 mnemonic should be 'table.copy64'");
      assertEquals("table.init64",
          Memory64TableOperations.TABLE_INIT_64.getMnemonic(),
          "TABLE_INIT_64 mnemonic should be 'table.init64'");
    }

    @Test
    @DisplayName("should have unique mnemonics")
    void shouldHaveUniqueMnemonics() {
      final Set<String> mnemonics = new HashSet<>();
      for (final Memory64TableOperations value : Memory64TableOperations.values()) {
        mnemonics.add(value.getMnemonic());
      }
      assertEquals(Memory64TableOperations.values().length, mnemonics.size(),
          "All mnemonics should be unique");
    }
  }

  @Nested
  @DisplayName("ReturnsValue Tests")
  class ReturnsValueTests {

    @Test
    @DisplayName("TABLE_GET_64 and TABLE_SIZE_64 should return values")
    void getAndSizeShouldReturnValues() {
      assertTrue(Memory64TableOperations.TABLE_GET_64.returnsValue(),
          "TABLE_GET_64 should return a value");
      assertTrue(Memory64TableOperations.TABLE_SIZE_64.returnsValue(),
          "TABLE_SIZE_64 should return a value");
    }

    @Test
    @DisplayName("other operations should not return values")
    void otherOperationsShouldNotReturnValues() {
      assertFalse(Memory64TableOperations.TABLE_SET_64.returnsValue(),
          "TABLE_SET_64 should not return a value");
      assertFalse(Memory64TableOperations.TABLE_GROW_64.returnsValue(),
          "TABLE_GROW_64 should not return a value");
      assertFalse(Memory64TableOperations.TABLE_FILL_64.returnsValue(),
          "TABLE_FILL_64 should not return a value");
      assertFalse(Memory64TableOperations.TABLE_COPY_64.returnsValue(),
          "TABLE_COPY_64 should not return a value");
      assertFalse(Memory64TableOperations.TABLE_INIT_64.returnsValue(),
          "TABLE_INIT_64 should not return a value");
    }
  }

  @Nested
  @DisplayName("FromOpcode Tests")
  class FromOpcodeTests {

    @Test
    @DisplayName("should resolve all constants via fromOpcode")
    void shouldResolveAllConstantsViaFromOpcode() {
      for (final Memory64TableOperations value : Memory64TableOperations.values()) {
        assertEquals(value,
            Memory64TableOperations.fromOpcode(value.getOpcode()),
            "fromOpcode should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid opcode")
    void shouldThrowForInvalidOpcode() {
      assertThrows(IllegalArgumentException.class,
          () -> Memory64TableOperations.fromOpcode(0x00),
          "fromOpcode(0x00) should throw IllegalArgumentException");
      assertThrows(IllegalArgumentException.class,
          () -> Memory64TableOperations.fromOpcode(-1),
          "fromOpcode(-1) should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("FromMnemonic Tests")
  class FromMnemonicTests {

    @Test
    @DisplayName("should resolve all constants via fromMnemonic")
    void shouldResolveAllConstantsViaFromMnemonic() {
      for (final Memory64TableOperations value : Memory64TableOperations.values()) {
        assertEquals(value,
            Memory64TableOperations.fromMnemonic(value.getMnemonic()),
            "fromMnemonic should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid mnemonic")
    void shouldThrowForInvalidMnemonic() {
      assertThrows(IllegalArgumentException.class,
          () -> Memory64TableOperations.fromMnemonic("invalid"),
          "fromMnemonic('invalid') should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("ValidateIndex Tests")
  class ValidateIndexTests {

    @Test
    @DisplayName("should accept valid index within bounds")
    void shouldAcceptValidIndex() {
      Memory64TableOperations.TABLE_GET_64.validateIndex(0, 10);
      Memory64TableOperations.TABLE_GET_64.validateIndex(9, 10);
    }

    @Test
    @DisplayName("should throw for negative index")
    void shouldThrowForNegativeIndex() {
      assertThrows(IndexOutOfBoundsException.class,
          () -> Memory64TableOperations.TABLE_GET_64.validateIndex(-1, 10),
          "validateIndex with negative index should throw");
    }

    @Test
    @DisplayName("should throw for index equal to table size")
    void shouldThrowForIndexEqualToSize() {
      assertThrows(IndexOutOfBoundsException.class,
          () -> Memory64TableOperations.TABLE_GET_64.validateIndex(10, 10),
          "validateIndex with index == tableSize should throw");
    }
  }

  @Nested
  @DisplayName("ValidateRange Tests")
  class ValidateRangeTests {

    @Test
    @DisplayName("should accept valid range within bounds")
    void shouldAcceptValidRange() {
      Memory64TableOperations.TABLE_FILL_64.validateRange(0, 5, 10);
      Memory64TableOperations.TABLE_FILL_64.validateRange(5, 5, 10);
    }

    @Test
    @DisplayName("should throw for negative start index")
    void shouldThrowForNegativeStartIndex() {
      assertThrows(IndexOutOfBoundsException.class,
          () -> Memory64TableOperations.TABLE_FILL_64.validateRange(-1, 5, 10),
          "validateRange with negative start should throw");
    }

    @Test
    @DisplayName("should throw for negative length")
    void shouldThrowForNegativeLength() {
      assertThrows(IllegalArgumentException.class,
          () -> Memory64TableOperations.TABLE_FILL_64.validateRange(0, -1, 10),
          "validateRange with negative length should throw");
    }

    @Test
    @DisplayName("should throw for range exceeding table size")
    void shouldThrowForRangeExceedingSize() {
      assertThrows(IndexOutOfBoundsException.class,
          () -> Memory64TableOperations.TABLE_FILL_64.validateRange(5, 6, 10),
          "validateRange exceeding table size should throw");
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final Memory64TableOperations value : Memory64TableOperations.values()) {
        assertEquals(value, Memory64TableOperations.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> Memory64TableOperations.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final Memory64TableOperations[] values = Memory64TableOperations.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(),
            "Ordinal of " + values[i].name() + " should be " + i);
      }
    }
  }
}
