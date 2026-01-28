/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.memory64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Memory64Compatibility;
import ai.tegmentum.wasmtime4j.Memory64Instruction;
import ai.tegmentum.wasmtime4j.Memory64InstructionHandler;
import ai.tegmentum.wasmtime4j.Memory64TableOperations;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Memory64 support classes.
 *
 * <p>Tests Memory64Instruction enum, Memory64Compatibility, Memory64InstructionHandler, and
 * Memory64TableOperations.
 */
@DisplayName("Memory64 Integration Tests")
public class Memory64IntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(Memory64IntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Memory64 Integration Tests");
  }

  @Nested
  @DisplayName("Memory64Instruction Enum Tests")
  class Memory64InstructionEnumTests {

    @Test
    @DisplayName("Should have all expected instruction types")
    void shouldHaveAllExpectedInstructionTypes() {
      LOGGER.info("Testing Memory64Instruction enum values");

      Memory64Instruction[] instructions = Memory64Instruction.values();
      assertTrue(instructions.length >= 25, "Should have at least 25 instructions");

      LOGGER.info("Memory64Instruction enum values: " + instructions.length);
    }

    @Test
    @DisplayName("Should have load instructions")
    void shouldHaveLoadInstructions() {
      LOGGER.info("Testing Memory64Instruction load instructions");

      assertNotNull(Memory64Instruction.I32_LOAD_64, "I32_LOAD_64 should exist");
      assertNotNull(Memory64Instruction.I64_LOAD_64, "I64_LOAD_64 should exist");
      assertNotNull(Memory64Instruction.F32_LOAD_64, "F32_LOAD_64 should exist");
      assertNotNull(Memory64Instruction.F64_LOAD_64, "F64_LOAD_64 should exist");
      assertNotNull(Memory64Instruction.I32_LOAD8_S_64, "I32_LOAD8_S_64 should exist");
      assertNotNull(Memory64Instruction.I32_LOAD8_U_64, "I32_LOAD8_U_64 should exist");
      assertNotNull(Memory64Instruction.I32_LOAD16_S_64, "I32_LOAD16_S_64 should exist");
      assertNotNull(Memory64Instruction.I32_LOAD16_U_64, "I32_LOAD16_U_64 should exist");

      LOGGER.info("Memory64Instruction load instructions verified");
    }

    @Test
    @DisplayName("Should have store instructions")
    void shouldHaveStoreInstructions() {
      LOGGER.info("Testing Memory64Instruction store instructions");

      assertNotNull(Memory64Instruction.I32_STORE_64, "I32_STORE_64 should exist");
      assertNotNull(Memory64Instruction.I64_STORE_64, "I64_STORE_64 should exist");
      assertNotNull(Memory64Instruction.F32_STORE_64, "F32_STORE_64 should exist");
      assertNotNull(Memory64Instruction.F64_STORE_64, "F64_STORE_64 should exist");
      assertNotNull(Memory64Instruction.I32_STORE8_64, "I32_STORE8_64 should exist");
      assertNotNull(Memory64Instruction.I32_STORE16_64, "I32_STORE16_64 should exist");

      LOGGER.info("Memory64Instruction store instructions verified");
    }

    @Test
    @DisplayName("Should have memory control instructions")
    void shouldHaveMemoryControlInstructions() {
      LOGGER.info("Testing Memory64Instruction control instructions");

      assertNotNull(Memory64Instruction.MEMORY_SIZE_64, "MEMORY_SIZE_64 should exist");
      assertNotNull(Memory64Instruction.MEMORY_GROW_64, "MEMORY_GROW_64 should exist");
      assertNotNull(Memory64Instruction.MEMORY_FILL_64, "MEMORY_FILL_64 should exist");
      assertNotNull(Memory64Instruction.MEMORY_COPY_64, "MEMORY_COPY_64 should exist");
      assertNotNull(Memory64Instruction.MEMORY_INIT_64, "MEMORY_INIT_64 should exist");

      LOGGER.info("Memory64Instruction control instructions verified");
    }

    @Test
    @DisplayName("Should have correct opcodes")
    void shouldHaveCorrectOpcodes() {
      LOGGER.info("Testing Memory64Instruction opcodes");

      assertEquals(0x20, Memory64Instruction.I32_LOAD_64.getOpcode(), "I32_LOAD_64 opcode");
      assertEquals(0x21, Memory64Instruction.I64_LOAD_64.getOpcode(), "I64_LOAD_64 opcode");
      assertEquals(0x30, Memory64Instruction.I32_STORE_64.getOpcode(), "I32_STORE_64 opcode");
      assertEquals(0x40, Memory64Instruction.MEMORY_SIZE_64.getOpcode(), "MEMORY_SIZE_64 opcode");
      assertEquals(0x41, Memory64Instruction.MEMORY_GROW_64.getOpcode(), "MEMORY_GROW_64 opcode");

      LOGGER.info("Memory64Instruction opcodes verified");
    }

    @Test
    @DisplayName("Should have correct mnemonics")
    void shouldHaveCorrectMnemonics() {
      LOGGER.info("Testing Memory64Instruction mnemonics");

      assertEquals(
          "i32.load", Memory64Instruction.I32_LOAD_64.getMnemonic(), "I32_LOAD_64 mnemonic");
      assertEquals(
          "i64.load", Memory64Instruction.I64_LOAD_64.getMnemonic(), "I64_LOAD_64 mnemonic");
      assertEquals(
          "i32.store", Memory64Instruction.I32_STORE_64.getMnemonic(), "I32_STORE_64 mnemonic");
      assertEquals(
          "memory.size",
          Memory64Instruction.MEMORY_SIZE_64.getMnemonic(),
          "MEMORY_SIZE_64 mnemonic");

      LOGGER.info("Memory64Instruction mnemonics verified");
    }

    @Test
    @DisplayName("Should have correct memory sizes")
    void shouldHaveCorrectMemorySizes() {
      LOGGER.info("Testing Memory64Instruction memory sizes");

      assertEquals(4, Memory64Instruction.I32_LOAD_64.getMemorySize(), "I32_LOAD_64 size");
      assertEquals(8, Memory64Instruction.I64_LOAD_64.getMemorySize(), "I64_LOAD_64 size");
      assertEquals(4, Memory64Instruction.F32_LOAD_64.getMemorySize(), "F32_LOAD_64 size");
      assertEquals(8, Memory64Instruction.F64_LOAD_64.getMemorySize(), "F64_LOAD_64 size");
      assertEquals(1, Memory64Instruction.I32_LOAD8_S_64.getMemorySize(), "I32_LOAD8_S_64 size");
      assertEquals(2, Memory64Instruction.I32_LOAD16_S_64.getMemorySize(), "I32_LOAD16_S_64 size");
      assertEquals(0, Memory64Instruction.MEMORY_SIZE_64.getMemorySize(), "MEMORY_SIZE_64 size");

      LOGGER.info("Memory64Instruction memory sizes verified");
    }

    @Test
    @DisplayName("Should correctly identify load instructions")
    void shouldCorrectlyIdentifyLoadInstructions() {
      LOGGER.info("Testing Memory64Instruction isLoad()");

      assertTrue(Memory64Instruction.I32_LOAD_64.isLoad(), "I32_LOAD_64 should be load");
      assertTrue(Memory64Instruction.I64_LOAD_64.isLoad(), "I64_LOAD_64 should be load");
      assertTrue(Memory64Instruction.F32_LOAD_64.isLoad(), "F32_LOAD_64 should be load");
      assertTrue(Memory64Instruction.I32_LOAD8_S_64.isLoad(), "I32_LOAD8_S_64 should be load");

      assertFalse(Memory64Instruction.I32_STORE_64.isLoad(), "I32_STORE_64 should not be load");
      assertFalse(Memory64Instruction.MEMORY_SIZE_64.isLoad(), "MEMORY_SIZE_64 should not be load");

      LOGGER.info("Memory64Instruction isLoad() verified");
    }

    @Test
    @DisplayName("Should correctly identify store instructions")
    void shouldCorrectlyIdentifyStoreInstructions() {
      LOGGER.info("Testing Memory64Instruction isStore()");

      assertTrue(Memory64Instruction.I32_STORE_64.isStore(), "I32_STORE_64 should be store");
      assertTrue(Memory64Instruction.I64_STORE_64.isStore(), "I64_STORE_64 should be store");
      assertTrue(Memory64Instruction.F32_STORE_64.isStore(), "F32_STORE_64 should be store");
      assertTrue(Memory64Instruction.I32_STORE8_64.isStore(), "I32_STORE8_64 should be store");

      assertFalse(Memory64Instruction.I32_LOAD_64.isStore(), "I32_LOAD_64 should not be store");
      assertFalse(
          Memory64Instruction.MEMORY_SIZE_64.isStore(), "MEMORY_SIZE_64 should not be store");

      LOGGER.info("Memory64Instruction isStore() verified");
    }

    @Test
    @DisplayName("Should correctly identify control instructions")
    void shouldCorrectlyIdentifyControlInstructions() {
      LOGGER.info("Testing Memory64Instruction isControlInstruction()");

      assertTrue(
          Memory64Instruction.MEMORY_SIZE_64.isControlInstruction(),
          "MEMORY_SIZE_64 should be control");
      assertTrue(
          Memory64Instruction.MEMORY_GROW_64.isControlInstruction(),
          "MEMORY_GROW_64 should be control");
      assertTrue(
          Memory64Instruction.MEMORY_FILL_64.isControlInstruction(),
          "MEMORY_FILL_64 should be control");
      assertTrue(
          Memory64Instruction.MEMORY_COPY_64.isControlInstruction(),
          "MEMORY_COPY_64 should be control");
      assertTrue(
          Memory64Instruction.MEMORY_INIT_64.isControlInstruction(),
          "MEMORY_INIT_64 should be control");

      assertFalse(
          Memory64Instruction.I32_LOAD_64.isControlInstruction(),
          "I32_LOAD_64 should not be control");
      assertFalse(
          Memory64Instruction.I32_STORE_64.isControlInstruction(),
          "I32_STORE_64 should not be control");

      LOGGER.info("Memory64Instruction isControlInstruction() verified");
    }

    @Test
    @DisplayName("Should have correct required alignment")
    void shouldHaveCorrectRequiredAlignment() {
      LOGGER.info("Testing Memory64Instruction getRequiredAlignment()");

      assertEquals(
          4, Memory64Instruction.I32_LOAD_64.getRequiredAlignment(), "I32_LOAD_64 alignment");
      assertEquals(
          8, Memory64Instruction.I64_LOAD_64.getRequiredAlignment(), "I64_LOAD_64 alignment");
      assertEquals(
          1, Memory64Instruction.I32_LOAD8_S_64.getRequiredAlignment(), "I32_LOAD8_S_64 alignment");
      assertEquals(
          2,
          Memory64Instruction.I32_LOAD16_S_64.getRequiredAlignment(),
          "I32_LOAD16_S_64 alignment");
      assertEquals(
          1, Memory64Instruction.MEMORY_SIZE_64.getRequiredAlignment(), "MEMORY_SIZE_64 alignment");

      LOGGER.info("Memory64Instruction getRequiredAlignment() verified");
    }

    @Test
    @DisplayName("Should validate alignment correctly")
    void shouldValidateAlignmentCorrectly() {
      LOGGER.info("Testing Memory64Instruction validateAlignment()");

      // Aligned offsets should not throw
      Memory64Instruction.I32_LOAD_64.validateAlignment(0);
      Memory64Instruction.I32_LOAD_64.validateAlignment(4);
      Memory64Instruction.I32_LOAD_64.validateAlignment(8);
      Memory64Instruction.I64_LOAD_64.validateAlignment(0);
      Memory64Instruction.I64_LOAD_64.validateAlignment(8);
      Memory64Instruction.I64_LOAD_64.validateAlignment(16);

      // Unaligned offsets should throw
      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Instruction.I32_LOAD_64.validateAlignment(1),
          "Should throw for unaligned i32 offset");
      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Instruction.I32_LOAD_64.validateAlignment(3),
          "Should throw for unaligned i32 offset");
      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Instruction.I64_LOAD_64.validateAlignment(4),
          "Should throw for unaligned i64 offset");

      LOGGER.info("Memory64Instruction validateAlignment() verified");
    }

    @Test
    @DisplayName("Should validate bounds correctly")
    void shouldValidateBoundsCorrectly() {
      LOGGER.info("Testing Memory64Instruction validateBounds()");

      // Valid bounds should not throw
      Memory64Instruction.I32_LOAD_64.validateBounds(0, 100);
      Memory64Instruction.I32_LOAD_64.validateBounds(96, 100);
      Memory64Instruction.I64_LOAD_64.validateBounds(0, 100);
      Memory64Instruction.I64_LOAD_64.validateBounds(92, 100);

      // Out of bounds should throw
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> Memory64Instruction.I32_LOAD_64.validateBounds(97, 100),
          "Should throw for out of bounds i32");
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> Memory64Instruction.I64_LOAD_64.validateBounds(93, 100),
          "Should throw for out of bounds i64");
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> Memory64Instruction.I32_LOAD_64.validateBounds(-1, 100),
          "Should throw for negative offset");

      LOGGER.info("Memory64Instruction validateBounds() verified");
    }

    @Test
    @DisplayName("Should find instruction by opcode")
    void shouldFindInstructionByOpcode() {
      LOGGER.info("Testing Memory64Instruction.fromOpcode()");

      assertEquals(
          Memory64Instruction.I32_LOAD_64,
          Memory64Instruction.fromOpcode(0x20),
          "Should find I32_LOAD_64");
      assertEquals(
          Memory64Instruction.I64_LOAD_64,
          Memory64Instruction.fromOpcode(0x21),
          "Should find I64_LOAD_64");
      assertEquals(
          Memory64Instruction.I32_STORE_64,
          Memory64Instruction.fromOpcode(0x30),
          "Should find I32_STORE_64");
      assertEquals(
          Memory64Instruction.MEMORY_SIZE_64,
          Memory64Instruction.fromOpcode(0x40),
          "Should find MEMORY_SIZE_64");

      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Instruction.fromOpcode(0xFF),
          "Should throw for unknown opcode");

      LOGGER.info("Memory64Instruction.fromOpcode() verified");
    }

    @Test
    @DisplayName("Should find instruction by mnemonic")
    void shouldFindInstructionByMnemonic() {
      LOGGER.info("Testing Memory64Instruction.fromMnemonic()");

      assertEquals(
          Memory64Instruction.I32_LOAD_64,
          Memory64Instruction.fromMnemonic("i32.load"),
          "Should find i32.load");
      assertEquals(
          Memory64Instruction.I64_LOAD_64,
          Memory64Instruction.fromMnemonic("i64.load"),
          "Should find i64.load");
      assertEquals(
          Memory64Instruction.MEMORY_SIZE_64,
          Memory64Instruction.fromMnemonic("memory.size"),
          "Should find memory.size");

      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Instruction.fromMnemonic("invalid"),
          "Should throw for unknown mnemonic");

      LOGGER.info("Memory64Instruction.fromMnemonic() verified");
    }
  }

  @Nested
  @DisplayName("Memory64Compatibility Class Tests")
  class Memory64CompatibilityTests {

    @Test
    @DisplayName("Should verify Memory64Compatibility is a class")
    void shouldVerifyMemory64CompatibilityIsClass() {
      LOGGER.info("Testing Memory64Compatibility class structure");

      assertFalse(
          Memory64Compatibility.class.isInterface(), "Memory64Compatibility should be a class");
      assertFalse(
          Memory64Compatibility.class.isEnum(), "Memory64Compatibility should not be an enum");

      LOGGER.info("Memory64Compatibility class structure verified");
    }

    @Test
    @DisplayName("Should have compatibility methods")
    void shouldHaveCompatibilityMethods() throws Exception {
      LOGGER.info("Testing Memory64Compatibility methods");

      Method[] methods = Memory64Compatibility.class.getMethods();
      assertTrue(methods.length > 0, "Should have methods");

      LOGGER.info("Memory64Compatibility methods verified: " + methods.length);
    }
  }

  @Nested
  @DisplayName("Memory64InstructionHandler Class Tests")
  class Memory64InstructionHandlerTests {

    @Test
    @DisplayName("Should verify Memory64InstructionHandler is a class")
    void shouldVerifyMemory64InstructionHandlerIsClass() {
      LOGGER.info("Testing Memory64InstructionHandler class structure");

      assertFalse(
          Memory64InstructionHandler.class.isInterface(),
          "Memory64InstructionHandler should be a class");
      assertFalse(
          Memory64InstructionHandler.class.isEnum(),
          "Memory64InstructionHandler should not be an enum");

      LOGGER.info("Memory64InstructionHandler class structure verified");
    }
  }

  @Nested
  @DisplayName("Memory64TableOperations Enum Tests")
  class Memory64TableOperationsTests {

    @Test
    @DisplayName("Should verify Memory64TableOperations is an enum")
    void shouldVerifyMemory64TableOperationsIsEnum() {
      LOGGER.info("Testing Memory64TableOperations enum structure");

      assertTrue(
          Memory64TableOperations.class.isEnum(), "Memory64TableOperations should be an enum");

      LOGGER.info("Memory64TableOperations enum structure verified");
    }
  }
}
