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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TailCallInstructions}.
 *
 * <p>Verifies enum structure, constants, field accessors (opcode, mnemonic,
 * replacesFrame, isIndirect), fromOpcode, fromMnemonic, and the inner
 * TailCallIndirectTarget class.
 */
@DisplayName("TailCallInstructions Tests")
class TailCallInstructionsTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(TailCallInstructions.class.isEnum(),
          "TailCallInstructions should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 4 values")
    void shouldHaveExactValueCount() {
      assertEquals(4, TailCallInstructions.values().length,
          "TailCallInstructions should have exactly 4 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain TAIL_CALL")
    void shouldContainTailCall() {
      assertNotNull(TailCallInstructions.TAIL_CALL,
          "TAIL_CALL should exist");
    }

    @Test
    @DisplayName("should contain TAIL_CALL_INDIRECT")
    void shouldContainTailCallIndirect() {
      assertNotNull(TailCallInstructions.TAIL_CALL_INDIRECT,
          "TAIL_CALL_INDIRECT should exist");
    }

    @Test
    @DisplayName("should contain RETURN_CALL")
    void shouldContainReturnCall() {
      assertNotNull(TailCallInstructions.RETURN_CALL,
          "RETURN_CALL should exist");
    }

    @Test
    @DisplayName("should contain RETURN_CALL_INDIRECT")
    void shouldContainReturnCallIndirect() {
      assertNotNull(TailCallInstructions.RETURN_CALL_INDIRECT,
          "RETURN_CALL_INDIRECT should exist");
    }
  }

  @Nested
  @DisplayName("GetOpcode Tests")
  class GetOpcodeTests {

    @Test
    @DisplayName("should have correct opcodes for all constants")
    void shouldHaveCorrectOpcodes() {
      assertEquals(0x12, TailCallInstructions.TAIL_CALL.getOpcode(),
          "TAIL_CALL opcode should be 0x12");
      assertEquals(0x13, TailCallInstructions.TAIL_CALL_INDIRECT.getOpcode(),
          "TAIL_CALL_INDIRECT opcode should be 0x13");
      assertEquals(0x14, TailCallInstructions.RETURN_CALL.getOpcode(),
          "RETURN_CALL opcode should be 0x14");
      assertEquals(0x15, TailCallInstructions.RETURN_CALL_INDIRECT.getOpcode(),
          "RETURN_CALL_INDIRECT opcode should be 0x15");
    }

    @Test
    @DisplayName("should have unique opcodes")
    void shouldHaveUniqueOpcodes() {
      final Set<Integer> opcodes = new HashSet<>();
      for (final TailCallInstructions value : TailCallInstructions.values()) {
        opcodes.add(value.getOpcode());
      }
      assertEquals(TailCallInstructions.values().length, opcodes.size(),
          "All opcodes should be unique");
    }
  }

  @Nested
  @DisplayName("GetMnemonic Tests")
  class GetMnemonicTests {

    @Test
    @DisplayName("should have correct mnemonics for all constants")
    void shouldHaveCorrectMnemonics() {
      assertEquals("tail_call", TailCallInstructions.TAIL_CALL.getMnemonic(),
          "TAIL_CALL mnemonic should be 'tail_call'");
      assertEquals("tail_call_indirect",
          TailCallInstructions.TAIL_CALL_INDIRECT.getMnemonic(),
          "TAIL_CALL_INDIRECT mnemonic should be 'tail_call_indirect'");
      assertEquals("return_call", TailCallInstructions.RETURN_CALL.getMnemonic(),
          "RETURN_CALL mnemonic should be 'return_call'");
      assertEquals("return_call_indirect",
          TailCallInstructions.RETURN_CALL_INDIRECT.getMnemonic(),
          "RETURN_CALL_INDIRECT mnemonic should be 'return_call_indirect'");
    }

    @Test
    @DisplayName("should have unique mnemonics")
    void shouldHaveUniqueMnemonics() {
      final Set<String> mnemonics = new HashSet<>();
      for (final TailCallInstructions value : TailCallInstructions.values()) {
        mnemonics.add(value.getMnemonic());
      }
      assertEquals(TailCallInstructions.values().length, mnemonics.size(),
          "All mnemonics should be unique");
    }
  }

  @Nested
  @DisplayName("ReplacesFrame Tests")
  class ReplacesFrameTests {

    @Test
    @DisplayName("TAIL_CALL should replace frame")
    void tailCallShouldReplaceFrame() {
      assertTrue(TailCallInstructions.TAIL_CALL.replacesFrame(),
          "TAIL_CALL should replace frame");
    }

    @Test
    @DisplayName("TAIL_CALL_INDIRECT should replace frame")
    void tailCallIndirectShouldReplaceFrame() {
      assertTrue(TailCallInstructions.TAIL_CALL_INDIRECT.replacesFrame(),
          "TAIL_CALL_INDIRECT should replace frame");
    }

    @Test
    @DisplayName("RETURN_CALL should not replace frame")
    void returnCallShouldNotReplaceFrame() {
      assertFalse(TailCallInstructions.RETURN_CALL.replacesFrame(),
          "RETURN_CALL should not replace frame");
    }

    @Test
    @DisplayName("RETURN_CALL_INDIRECT should not replace frame")
    void returnCallIndirectShouldNotReplaceFrame() {
      assertFalse(TailCallInstructions.RETURN_CALL_INDIRECT.replacesFrame(),
          "RETURN_CALL_INDIRECT should not replace frame");
    }
  }

  @Nested
  @DisplayName("IsIndirect Tests")
  class IsIndirectTests {

    @Test
    @DisplayName("TAIL_CALL should not be indirect")
    void tailCallShouldNotBeIndirect() {
      assertFalse(TailCallInstructions.TAIL_CALL.isIndirect(),
          "TAIL_CALL should not be indirect");
    }

    @Test
    @DisplayName("TAIL_CALL_INDIRECT should be indirect")
    void tailCallIndirectShouldBeIndirect() {
      assertTrue(TailCallInstructions.TAIL_CALL_INDIRECT.isIndirect(),
          "TAIL_CALL_INDIRECT should be indirect");
    }

    @Test
    @DisplayName("RETURN_CALL should not be indirect")
    void returnCallShouldNotBeIndirect() {
      assertFalse(TailCallInstructions.RETURN_CALL.isIndirect(),
          "RETURN_CALL should not be indirect");
    }

    @Test
    @DisplayName("RETURN_CALL_INDIRECT should be indirect")
    void returnCallIndirectShouldBeIndirect() {
      assertTrue(TailCallInstructions.RETURN_CALL_INDIRECT.isIndirect(),
          "RETURN_CALL_INDIRECT should be indirect");
    }
  }

  @Nested
  @DisplayName("FromOpcode Tests")
  class FromOpcodeTests {

    @Test
    @DisplayName("should resolve all constants via fromOpcode")
    void shouldResolveAllConstantsViaFromOpcode() {
      for (final TailCallInstructions value : TailCallInstructions.values()) {
        assertEquals(value,
            TailCallInstructions.fromOpcode(value.getOpcode()),
            "fromOpcode should return " + value.name());
      }
    }

    @Test
    @DisplayName("should return null for invalid opcode")
    void shouldReturnNullForInvalidOpcode() {
      assertNull(TailCallInstructions.fromOpcode(0x00),
          "fromOpcode(0x00) should return null");
      assertNull(TailCallInstructions.fromOpcode(-1),
          "fromOpcode(-1) should return null");
      assertNull(TailCallInstructions.fromOpcode(0xFF),
          "fromOpcode(0xFF) should return null");
    }
  }

  @Nested
  @DisplayName("FromMnemonic Tests")
  class FromMnemonicTests {

    @Test
    @DisplayName("should resolve all constants via fromMnemonic")
    void shouldResolveAllConstantsViaFromMnemonic() {
      for (final TailCallInstructions value : TailCallInstructions.values()) {
        assertEquals(value,
            TailCallInstructions.fromMnemonic(value.getMnemonic()),
            "fromMnemonic should return " + value.name());
      }
    }

    @Test
    @DisplayName("should return null for invalid mnemonic")
    void shouldReturnNullForInvalidMnemonic() {
      assertNull(TailCallInstructions.fromMnemonic("invalid"),
          "fromMnemonic('invalid') should return null");
      assertNull(TailCallInstructions.fromMnemonic(""),
          "fromMnemonic('') should return null");
    }
  }

  @Nested
  @DisplayName("TailCallIndirectTarget Tests")
  class TailCallIndirectTargetTests {

    @Test
    @DisplayName("should create target with correct fields")
    void shouldCreateTargetWithCorrectFields() {
      final TailCallInstructions.TailCallIndirectTarget target =
          new TailCallInstructions.TailCallIndirectTarget(0, 5, null);
      assertEquals(0, target.getTableIndex(),
          "Table index should be 0");
      assertEquals(5, target.getFunctionIndex(),
          "Function index should be 5");
      assertNull(target.getExpectedType(),
          "Expected type should be null when not specified");
    }

    @Test
    @DisplayName("should preserve table and function indices")
    void shouldPreserveIndices() {
      final TailCallInstructions.TailCallIndirectTarget target =
          new TailCallInstructions.TailCallIndirectTarget(3, 42, null);
      assertEquals(3, target.getTableIndex(),
          "Table index should be preserved");
      assertEquals(42, target.getFunctionIndex(),
          "Function index should be preserved");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("indirect instructions should be paired with non-indirect")
    void indirectShouldBePairedWithNonIndirect() {
      assertTrue(TailCallInstructions.TAIL_CALL_INDIRECT.isIndirect(),
          "TAIL_CALL_INDIRECT should be indirect");
      assertFalse(TailCallInstructions.TAIL_CALL.isIndirect(),
          "TAIL_CALL should not be indirect");
      assertTrue(TailCallInstructions.RETURN_CALL_INDIRECT.isIndirect(),
          "RETURN_CALL_INDIRECT should be indirect");
      assertFalse(TailCallInstructions.RETURN_CALL.isIndirect(),
          "RETURN_CALL should not be indirect");
    }

    @Test
    @DisplayName("frame replacement and indirect should be independent properties")
    void frameReplacementAndIndirectShouldBeIndependent() {
      // TAIL_CALL: replaces=true, indirect=false
      assertTrue(TailCallInstructions.TAIL_CALL.replacesFrame(),
          "TAIL_CALL should replace frame");
      assertFalse(TailCallInstructions.TAIL_CALL.isIndirect(),
          "TAIL_CALL should not be indirect");

      // TAIL_CALL_INDIRECT: replaces=true, indirect=true
      assertTrue(TailCallInstructions.TAIL_CALL_INDIRECT.replacesFrame(),
          "TAIL_CALL_INDIRECT should replace frame");
      assertTrue(TailCallInstructions.TAIL_CALL_INDIRECT.isIndirect(),
          "TAIL_CALL_INDIRECT should be indirect");

      // RETURN_CALL: replaces=false, indirect=false
      assertFalse(TailCallInstructions.RETURN_CALL.replacesFrame(),
          "RETURN_CALL should not replace frame");
      assertFalse(TailCallInstructions.RETURN_CALL.isIndirect(),
          "RETURN_CALL should not be indirect");

      // RETURN_CALL_INDIRECT: replaces=false, indirect=true
      assertFalse(TailCallInstructions.RETURN_CALL_INDIRECT.replacesFrame(),
          "RETURN_CALL_INDIRECT should not replace frame");
      assertTrue(TailCallInstructions.RETURN_CALL_INDIRECT.isIndirect(),
          "RETURN_CALL_INDIRECT should be indirect");
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final TailCallInstructions value : TailCallInstructions.values()) {
        assertEquals(value, TailCallInstructions.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> TailCallInstructions.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final TailCallInstructions[] values = TailCallInstructions.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(),
            "Ordinal of " + values[i].name() + " should be " + i);
      }
    }
  }
}
