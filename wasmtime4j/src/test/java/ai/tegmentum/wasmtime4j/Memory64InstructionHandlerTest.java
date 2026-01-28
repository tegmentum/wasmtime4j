package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Memory64InstructionHandler}.
 *
 * <p>Validates instruction lookup, cache initialization, null argument handling, instruction
 * support queries, and execution statistics tracking.
 */
@DisplayName("Memory64InstructionHandler Tests")
class Memory64InstructionHandlerTest {

  private static final Logger LOGGER =
      Logger.getLogger(Memory64InstructionHandlerTest.class.getName());

  private Memory64InstructionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new Memory64InstructionHandler();
    LOGGER.info("Created new Memory64InstructionHandler");
  }

  @Nested
  @DisplayName("Instruction Support by Opcode Tests")
  class InstructionSupportByOpcodeTests {

    @Test
    @DisplayName("should report known opcodes as supported")
    void shouldReportKnownOpcodesAsSupported() {
      // I32_LOAD_64 has opcode 0x20
      assertTrue(
          handler.isInstructionSupported(0x20), "Opcode 0x20 (i32.load) should be supported");
      // I64_LOAD_64 has opcode 0x21
      assertTrue(
          handler.isInstructionSupported(0x21), "Opcode 0x21 (i64.load) should be supported");
      // I32_STORE_64 has opcode 0x30
      assertTrue(
          handler.isInstructionSupported(0x30), "Opcode 0x30 (i32.store) should be supported");
      LOGGER.info("Known opcodes verified as supported");
    }

    @Test
    @DisplayName("should report unknown opcodes as not supported")
    void shouldReportUnknownOpcodesAsNotSupported() {
      assertFalse(handler.isInstructionSupported(0xFF), "Opcode 0xFF should not be supported");
      assertFalse(handler.isInstructionSupported(0x00), "Opcode 0x00 should not be supported");
      assertFalse(handler.isInstructionSupported(-1), "Opcode -1 should not be supported");
      LOGGER.info("Unknown opcodes verified as not supported");
    }
  }

  @Nested
  @DisplayName("Instruction Support by Mnemonic Tests")
  class InstructionSupportByMnemonicTests {

    @Test
    @DisplayName("should report known mnemonics as supported")
    void shouldReportKnownMnemonicsAsSupported() {
      assertTrue(
          handler.isInstructionSupported("i32.load"), "Mnemonic 'i32.load' should be supported");
      assertTrue(
          handler.isInstructionSupported("i64.load"), "Mnemonic 'i64.load' should be supported");
      assertTrue(
          handler.isInstructionSupported("i32.store"), "Mnemonic 'i32.store' should be supported");
      LOGGER.info("Known mnemonics verified as supported");
    }

    @Test
    @DisplayName("should report unknown mnemonics as not supported")
    void shouldReportUnknownMnemonicsAsNotSupported() {
      assertFalse(
          handler.isInstructionSupported("nonexistent"),
          "Unknown mnemonic should not be supported");
      assertFalse(handler.isInstructionSupported(""), "Empty mnemonic should not be supported");
      LOGGER.info("Unknown mnemonics verified as not supported");
    }
  }

  @Nested
  @DisplayName("Null Argument Validation Tests")
  class NullArgumentValidationTests {

    @Test
    @DisplayName("executeInstruction with null instruction should throw")
    void executeInstructionWithNullInstructionShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> handler.executeInstruction(null, null, 0, 0),
          "executeInstruction(null, ...) should throw IllegalArgumentException");
      LOGGER.info("Null instruction validation verified");
    }

    @Test
    @DisplayName("validateMemory64Support with null memory should throw")
    void validateMemory64SupportWithNullMemoryShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> handler.validateMemory64Support(null),
          "validateMemory64Support(null) should throw IllegalArgumentException");
      LOGGER.info("Null memory validation verified");
    }

    @Test
    @DisplayName("validateInstructionParameters with null instruction should throw")
    void validateInstructionParametersWithNullInstructionShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> handler.validateInstructionParameters(null, null, 0),
          "validateInstructionParameters(null, ...) should throw IllegalArgumentException");
      LOGGER.info("Null instruction parameter validation verified");
    }
  }

  @Nested
  @DisplayName("Execution Statistics Tests")
  class ExecutionStatisticsTests {

    @Test
    @DisplayName("getStatistics should return non-null statistics")
    void getStatisticsShouldReturnNonNull() {
      final Memory64InstructionHandler.ExecutionStatistics stats = handler.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      LOGGER.info("Statistics object: " + stats);
    }

    @Test
    @DisplayName("getStatistics should return same instance")
    void getStatisticsShouldReturnSameInstance() {
      final Memory64InstructionHandler.ExecutionStatistics stats1 = handler.getStatistics();
      final Memory64InstructionHandler.ExecutionStatistics stats2 = handler.getStatistics();
      assertSame(stats1, stats2, "getStatistics() should return the same instance");
      LOGGER.info("Statistics singleton verified");
    }

    @Test
    @DisplayName("initial statistics should be zero")
    void initialStatisticsShouldBeZero() {
      final Memory64InstructionHandler.ExecutionStatistics stats = handler.getStatistics();
      assertEquals(0, stats.getTotalExecutions(), "Initial total executions should be 0");
      assertEquals(0, stats.getTotalErrors(), "Initial total errors should be 0");
      assertEquals(
          0.0, stats.getOverallErrorRate(), 0.001, "Initial overall error rate should be 0.0");
      LOGGER.info(
          "Initial statistics: totalExec="
              + stats.getTotalExecutions()
              + ", totalErr="
              + stats.getTotalErrors()
              + ", errorRate="
              + stats.getOverallErrorRate());
    }

    @Test
    @DisplayName("resetStatistics should clear all counters")
    void resetStatisticsShouldClearAllCounters() {
      // Reset and verify
      handler.resetStatistics();
      final Memory64InstructionHandler.ExecutionStatistics stats = handler.getStatistics();
      assertEquals(0, stats.getTotalExecutions(), "Total executions should be 0 after reset");
      assertEquals(0, stats.getTotalErrors(), "Total errors should be 0 after reset");
      LOGGER.info("Statistics after reset: " + stats);
    }

    @Test
    @DisplayName("statistics toString should not throw")
    void statisticsToStringShouldNotThrow() {
      final Memory64InstructionHandler.ExecutionStatistics stats = handler.getStatistics();
      final String str = assertDoesNotThrow(stats::toString, "toString should not throw");
      assertNotNull(str, "toString should not return null");
      LOGGER.info("Statistics toString: " + str);
    }

    @Test
    @DisplayName("per-instruction statistics should be zero for unexecuted instruction")
    void perInstructionStatsShouldBeZeroForUnexecuted() {
      final Memory64InstructionHandler.ExecutionStatistics stats = handler.getStatistics();
      final Memory64Instruction instruction = Memory64Instruction.I32_LOAD_64;

      assertEquals(
          0,
          stats.getExecutionCount(instruction),
          "Execution count for unexecuted instruction should be 0");
      assertEquals(
          0,
          stats.getTotalExecutionTime(instruction),
          "Execution time for unexecuted instruction should be 0");
      assertEquals(
          0.0,
          stats.getAverageExecutionTime(instruction),
          0.001,
          "Average execution time for unexecuted instruction should be 0.0");
      assertEquals(
          0,
          stats.getErrorCount(instruction),
          "Error count for unexecuted instruction should be 0");
      assertEquals(
          0.0,
          stats.getErrorRate(instruction),
          0.001,
          "Error rate for unexecuted instruction should be 0.0");
      LOGGER.info("Per-instruction zero stats verified for " + instruction);
    }
  }

  @Nested
  @DisplayName("Cache Initialization Tests")
  class CacheInitializationTests {

    @Test
    @DisplayName("all Memory64Instruction enum values should be cached by opcode")
    void allEnumValuesShouldBeCachedByOpcode() {
      for (final Memory64Instruction instruction : Memory64Instruction.values()) {
        assertTrue(
            handler.isInstructionSupported(instruction.getOpcode()),
            "Instruction "
                + instruction.name()
                + " with opcode 0x"
                + Integer.toHexString(instruction.getOpcode())
                + " should be supported");
      }
      LOGGER.info("All " + Memory64Instruction.values().length + " instructions cached by opcode");
    }

    @Test
    @DisplayName("all Memory64Instruction enum values should be cached by mnemonic")
    void allEnumValuesShouldBeCachedByMnemonic() {
      for (final Memory64Instruction instruction : Memory64Instruction.values()) {
        assertTrue(
            handler.isInstructionSupported(instruction.getMnemonic()),
            "Instruction "
                + instruction.name()
                + " with mnemonic '"
                + instruction.getMnemonic()
                + "' should be supported");
      }
      LOGGER.info(
          "All " + Memory64Instruction.values().length + " instructions cached by mnemonic");
    }
  }
}
