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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BranchHintingInstructions}.
 *
 * <p>Verifies enum structure, constants, field accessors (opcode, mnemonic,
 * probability, branchType), fromOpcode, fromMnemonic, inner types
 * (BranchProbability, BranchType, BranchHintAnnotation).
 */
@DisplayName("BranchHintingInstructions Tests")
class BranchHintingInstructionsTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(BranchHintingInstructions.class.isEnum(),
          "BranchHintingInstructions should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 9 values")
    void shouldHaveExactValueCount() {
      assertEquals(9, BranchHintingInstructions.values().length,
          "BranchHintingInstructions should have exactly 9 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain all expected constants")
    void shouldContainAllExpectedConstants() {
      assertNotNull(BranchHintingInstructions.LIKELY_TAKEN,
          "LIKELY_TAKEN should exist");
      assertNotNull(BranchHintingInstructions.UNLIKELY_TAKEN,
          "UNLIKELY_TAKEN should exist");
      assertNotNull(BranchHintingInstructions.LOOP_HOT,
          "LOOP_HOT should exist");
      assertNotNull(BranchHintingInstructions.LOOP_COLD,
          "LOOP_COLD should exist");
      assertNotNull(BranchHintingInstructions.HOT_PATH,
          "HOT_PATH should exist");
      assertNotNull(BranchHintingInstructions.COLD_PATH,
          "COLD_PATH should exist");
      assertNotNull(BranchHintingInstructions.SWITCH_HOT_CASE,
          "SWITCH_HOT_CASE should exist");
      assertNotNull(BranchHintingInstructions.CALL_HOT,
          "CALL_HOT should exist");
      assertNotNull(BranchHintingInstructions.CALL_COLD,
          "CALL_COLD should exist");
    }
  }

  @Nested
  @DisplayName("GetOpcode Tests")
  class GetOpcodeTests {

    @Test
    @DisplayName("should have correct opcodes for all constants")
    void shouldHaveCorrectOpcodes() {
      assertEquals(0x01, BranchHintingInstructions.LIKELY_TAKEN.getOpcode(),
          "LIKELY_TAKEN opcode should be 0x01");
      assertEquals(0x02, BranchHintingInstructions.UNLIKELY_TAKEN.getOpcode(),
          "UNLIKELY_TAKEN opcode should be 0x02");
      assertEquals(0x03, BranchHintingInstructions.LOOP_HOT.getOpcode(),
          "LOOP_HOT opcode should be 0x03");
      assertEquals(0x04, BranchHintingInstructions.LOOP_COLD.getOpcode(),
          "LOOP_COLD opcode should be 0x04");
      assertEquals(0x05, BranchHintingInstructions.HOT_PATH.getOpcode(),
          "HOT_PATH opcode should be 0x05");
      assertEquals(0x06, BranchHintingInstructions.COLD_PATH.getOpcode(),
          "COLD_PATH opcode should be 0x06");
      assertEquals(0x07, BranchHintingInstructions.SWITCH_HOT_CASE.getOpcode(),
          "SWITCH_HOT_CASE opcode should be 0x07");
      assertEquals(0x08, BranchHintingInstructions.CALL_HOT.getOpcode(),
          "CALL_HOT opcode should be 0x08");
      assertEquals(0x09, BranchHintingInstructions.CALL_COLD.getOpcode(),
          "CALL_COLD opcode should be 0x09");
    }

    @Test
    @DisplayName("should have unique opcodes")
    void shouldHaveUniqueOpcodes() {
      final Set<Integer> opcodes = new HashSet<>();
      for (final BranchHintingInstructions value : BranchHintingInstructions.values()) {
        opcodes.add(value.getOpcode());
      }
      assertEquals(BranchHintingInstructions.values().length, opcodes.size(),
          "All opcodes should be unique");
    }
  }

  @Nested
  @DisplayName("GetMnemonic Tests")
  class GetMnemonicTests {

    @Test
    @DisplayName("should have correct mnemonics for all constants")
    void shouldHaveCorrectMnemonics() {
      assertEquals("hint.likely",
          BranchHintingInstructions.LIKELY_TAKEN.getMnemonic(),
          "LIKELY_TAKEN mnemonic should be 'hint.likely'");
      assertEquals("hint.unlikely",
          BranchHintingInstructions.UNLIKELY_TAKEN.getMnemonic(),
          "UNLIKELY_TAKEN mnemonic should be 'hint.unlikely'");
      assertEquals("hint.loop_hot",
          BranchHintingInstructions.LOOP_HOT.getMnemonic(),
          "LOOP_HOT mnemonic should be 'hint.loop_hot'");
      assertEquals("hint.loop_cold",
          BranchHintingInstructions.LOOP_COLD.getMnemonic(),
          "LOOP_COLD mnemonic should be 'hint.loop_cold'");
      assertEquals("hint.hot_path",
          BranchHintingInstructions.HOT_PATH.getMnemonic(),
          "HOT_PATH mnemonic should be 'hint.hot_path'");
      assertEquals("hint.cold_path",
          BranchHintingInstructions.COLD_PATH.getMnemonic(),
          "COLD_PATH mnemonic should be 'hint.cold_path'");
      assertEquals("hint.switch_hot",
          BranchHintingInstructions.SWITCH_HOT_CASE.getMnemonic(),
          "SWITCH_HOT_CASE mnemonic should be 'hint.switch_hot'");
      assertEquals("hint.call_hot",
          BranchHintingInstructions.CALL_HOT.getMnemonic(),
          "CALL_HOT mnemonic should be 'hint.call_hot'");
      assertEquals("hint.call_cold",
          BranchHintingInstructions.CALL_COLD.getMnemonic(),
          "CALL_COLD mnemonic should be 'hint.call_cold'");
    }

    @Test
    @DisplayName("should have unique mnemonics")
    void shouldHaveUniqueMnemonics() {
      final Set<String> mnemonics = new HashSet<>();
      for (final BranchHintingInstructions value : BranchHintingInstructions.values()) {
        mnemonics.add(value.getMnemonic());
      }
      assertEquals(BranchHintingInstructions.values().length, mnemonics.size(),
          "All mnemonics should be unique");
    }
  }

  @Nested
  @DisplayName("GetProbability Tests")
  class GetProbabilityTests {

    @Test
    @DisplayName("high probability hints should have HIGH probability")
    void highProbabilityHintsShouldHaveHigh() {
      assertEquals(BranchHintingInstructions.BranchProbability.HIGH,
          BranchHintingInstructions.LIKELY_TAKEN.getProbability(),
          "LIKELY_TAKEN should have HIGH probability");
      assertEquals(BranchHintingInstructions.BranchProbability.HIGH,
          BranchHintingInstructions.LOOP_HOT.getProbability(),
          "LOOP_HOT should have HIGH probability");
      assertEquals(BranchHintingInstructions.BranchProbability.HIGH,
          BranchHintingInstructions.HOT_PATH.getProbability(),
          "HOT_PATH should have HIGH probability");
      assertEquals(BranchHintingInstructions.BranchProbability.HIGH,
          BranchHintingInstructions.SWITCH_HOT_CASE.getProbability(),
          "SWITCH_HOT_CASE should have HIGH probability");
      assertEquals(BranchHintingInstructions.BranchProbability.HIGH,
          BranchHintingInstructions.CALL_HOT.getProbability(),
          "CALL_HOT should have HIGH probability");
    }

    @Test
    @DisplayName("low probability hints should have LOW probability")
    void lowProbabilityHintsShouldHaveLow() {
      assertEquals(BranchHintingInstructions.BranchProbability.LOW,
          BranchHintingInstructions.UNLIKELY_TAKEN.getProbability(),
          "UNLIKELY_TAKEN should have LOW probability");
      assertEquals(BranchHintingInstructions.BranchProbability.LOW,
          BranchHintingInstructions.LOOP_COLD.getProbability(),
          "LOOP_COLD should have LOW probability");
      assertEquals(BranchHintingInstructions.BranchProbability.LOW,
          BranchHintingInstructions.COLD_PATH.getProbability(),
          "COLD_PATH should have LOW probability");
      assertEquals(BranchHintingInstructions.BranchProbability.LOW,
          BranchHintingInstructions.CALL_COLD.getProbability(),
          "CALL_COLD should have LOW probability");
    }
  }

  @Nested
  @DisplayName("GetBranchType Tests")
  class GetBranchTypeTests {

    @Test
    @DisplayName("conditional hints should have CONDITIONAL branch type")
    void conditionalHintsShouldHaveConditionalType() {
      assertEquals(BranchHintingInstructions.BranchType.CONDITIONAL,
          BranchHintingInstructions.LIKELY_TAKEN.getBranchType(),
          "LIKELY_TAKEN should have CONDITIONAL branch type");
      assertEquals(BranchHintingInstructions.BranchType.CONDITIONAL,
          BranchHintingInstructions.UNLIKELY_TAKEN.getBranchType(),
          "UNLIKELY_TAKEN should have CONDITIONAL branch type");
    }

    @Test
    @DisplayName("loop hints should have LOOP branch type")
    void loopHintsShouldHaveLoopType() {
      assertEquals(BranchHintingInstructions.BranchType.LOOP,
          BranchHintingInstructions.LOOP_HOT.getBranchType(),
          "LOOP_HOT should have LOOP branch type");
      assertEquals(BranchHintingInstructions.BranchType.LOOP,
          BranchHintingInstructions.LOOP_COLD.getBranchType(),
          "LOOP_COLD should have LOOP branch type");
    }

    @Test
    @DisplayName("unconditional hints should have UNCONDITIONAL branch type")
    void unconditionalHintsShouldHaveUnconditionalType() {
      assertEquals(BranchHintingInstructions.BranchType.UNCONDITIONAL,
          BranchHintingInstructions.HOT_PATH.getBranchType(),
          "HOT_PATH should have UNCONDITIONAL branch type");
      assertEquals(BranchHintingInstructions.BranchType.UNCONDITIONAL,
          BranchHintingInstructions.COLD_PATH.getBranchType(),
          "COLD_PATH should have UNCONDITIONAL branch type");
    }

    @Test
    @DisplayName("switch hint should have SWITCH branch type")
    void switchHintShouldHaveSwitchType() {
      assertEquals(BranchHintingInstructions.BranchType.SWITCH,
          BranchHintingInstructions.SWITCH_HOT_CASE.getBranchType(),
          "SWITCH_HOT_CASE should have SWITCH branch type");
    }

    @Test
    @DisplayName("call hints should have CALL branch type")
    void callHintsShouldHaveCallType() {
      assertEquals(BranchHintingInstructions.BranchType.CALL,
          BranchHintingInstructions.CALL_HOT.getBranchType(),
          "CALL_HOT should have CALL branch type");
      assertEquals(BranchHintingInstructions.BranchType.CALL,
          BranchHintingInstructions.CALL_COLD.getBranchType(),
          "CALL_COLD should have CALL branch type");
    }
  }

  @Nested
  @DisplayName("FromOpcode Tests")
  class FromOpcodeTests {

    @Test
    @DisplayName("should resolve all constants via fromOpcode")
    void shouldResolveAllConstantsViaFromOpcode() {
      for (final BranchHintingInstructions value : BranchHintingInstructions.values()) {
        assertEquals(value, BranchHintingInstructions.fromOpcode(value.getOpcode()),
            "fromOpcode should return " + value.name());
      }
    }

    @Test
    @DisplayName("should return null for invalid opcode")
    void shouldReturnNullForInvalidOpcode() {
      assertNull(BranchHintingInstructions.fromOpcode(0x00),
          "fromOpcode(0x00) should return null");
      assertNull(BranchHintingInstructions.fromOpcode(0xFF),
          "fromOpcode(0xFF) should return null");
      assertNull(BranchHintingInstructions.fromOpcode(-1),
          "fromOpcode(-1) should return null");
    }
  }

  @Nested
  @DisplayName("FromMnemonic Tests")
  class FromMnemonicTests {

    @Test
    @DisplayName("should resolve all constants via fromMnemonic")
    void shouldResolveAllConstantsViaFromMnemonic() {
      for (final BranchHintingInstructions value : BranchHintingInstructions.values()) {
        assertEquals(value,
            BranchHintingInstructions.fromMnemonic(value.getMnemonic()),
            "fromMnemonic should return " + value.name());
      }
    }

    @Test
    @DisplayName("should return null for invalid mnemonic")
    void shouldReturnNullForInvalidMnemonic() {
      assertNull(BranchHintingInstructions.fromMnemonic("invalid"),
          "fromMnemonic('invalid') should return null");
      assertNull(BranchHintingInstructions.fromMnemonic(""),
          "fromMnemonic('') should return null");
    }
  }

  @Nested
  @DisplayName("BranchProbability Inner Enum Tests")
  class BranchProbabilityTests {

    @Test
    @DisplayName("should have exactly 5 values")
    void shouldHaveFiveValues() {
      assertEquals(5, BranchHintingInstructions.BranchProbability.values().length,
          "BranchProbability should have exactly 5 values");
    }

    @Test
    @DisplayName("should have correct probability values")
    void shouldHaveCorrectProbabilityValues() {
      assertEquals(0.05,
          BranchHintingInstructions.BranchProbability.VERY_LOW.getValue(), 0.001,
          "VERY_LOW should have value 0.05");
      assertEquals(0.25,
          BranchHintingInstructions.BranchProbability.LOW.getValue(), 0.001,
          "LOW should have value 0.25");
      assertEquals(0.5,
          BranchHintingInstructions.BranchProbability.MEDIUM.getValue(), 0.001,
          "MEDIUM should have value 0.5");
      assertEquals(0.75,
          BranchHintingInstructions.BranchProbability.HIGH.getValue(), 0.001,
          "HIGH should have value 0.75");
      assertEquals(0.95,
          BranchHintingInstructions.BranchProbability.VERY_HIGH.getValue(), 0.001,
          "VERY_HIGH should have value 0.95");
    }
  }

  @Nested
  @DisplayName("BranchType Inner Enum Tests")
  class BranchTypeTests {

    @Test
    @DisplayName("should have exactly 5 values")
    void shouldHaveFiveValues() {
      assertEquals(5, BranchHintingInstructions.BranchType.values().length,
          "BranchType should have exactly 5 values");
    }

    @Test
    @DisplayName("should contain all expected types")
    void shouldContainAllExpectedTypes() {
      assertNotNull(BranchHintingInstructions.BranchType.CONDITIONAL,
          "CONDITIONAL should exist");
      assertNotNull(BranchHintingInstructions.BranchType.UNCONDITIONAL,
          "UNCONDITIONAL should exist");
      assertNotNull(BranchHintingInstructions.BranchType.LOOP,
          "LOOP should exist");
      assertNotNull(BranchHintingInstructions.BranchType.SWITCH,
          "SWITCH should exist");
      assertNotNull(BranchHintingInstructions.BranchType.CALL,
          "CALL should exist");
    }
  }

  @Nested
  @DisplayName("BranchHintAnnotation Tests")
  class BranchHintAnnotationTests {

    @Test
    @DisplayName("should create annotation with correct fields")
    void shouldCreateAnnotationWithCorrectFields() {
      final BranchHintingInstructions.BranchHintAnnotation annotation =
          BranchHintingInstructions.LIKELY_TAKEN.createAnnotation("target", 0.9);
      assertNotNull(annotation, "Annotation should not be null");
      assertEquals(BranchHintingInstructions.LIKELY_TAKEN, annotation.getHint(),
          "Hint should be LIKELY_TAKEN");
      assertEquals("target", annotation.getTargetLabel(),
          "Target label should be 'target'");
      assertEquals(0.9, annotation.getConfidence(), 0.001,
          "Confidence should be 0.9");
      assertTrue(annotation.getTimestamp() > 0,
          "Timestamp should be positive");
    }

    @Test
    @DisplayName("should clamp confidence to valid range")
    void shouldClampConfidence() {
      final BranchHintingInstructions.BranchHintAnnotation lowAnnotation =
          BranchHintingInstructions.LIKELY_TAKEN.createAnnotation("t", -0.5);
      assertTrue(lowAnnotation.getConfidence() >= 0.0,
          "Confidence should be clamped to at least 0.0");

      final BranchHintingInstructions.BranchHintAnnotation highAnnotation =
          BranchHintingInstructions.LIKELY_TAKEN.createAnnotation("t", 1.5);
      assertTrue(highAnnotation.getConfidence() <= 1.0,
          "Confidence should be clamped to at most 1.0");
    }

    @Test
    @DisplayName("should return effective probability")
    void shouldReturnEffectiveProbability() {
      final BranchHintingInstructions.BranchHintAnnotation annotation =
          BranchHintingInstructions.LIKELY_TAKEN.createAnnotation("t", 1.0);
      final double effectiveProb = annotation.getEffectiveProbability();
      assertTrue(effectiveProb > 0.0 && effectiveProb <= 1.0,
          "Effective probability should be between 0 and 1, got " + effectiveProb);
    }

    @Test
    @DisplayName("toString should contain mnemonic")
    void toStringShouldContainMnemonic() {
      final BranchHintingInstructions.BranchHintAnnotation annotation =
          BranchHintingInstructions.LIKELY_TAKEN.createAnnotation("target", 0.8);
      assertTrue(annotation.toString().contains("hint.likely"),
          "toString should contain the mnemonic");
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final BranchHintingInstructions value :
          BranchHintingInstructions.values()) {
        assertEquals(value, BranchHintingInstructions.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> BranchHintingInstructions.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final BranchHintingInstructions[] values = BranchHintingInstructions.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(),
            "Ordinal of " + values[i].name() + " should be " + i);
      }
    }
  }
}
