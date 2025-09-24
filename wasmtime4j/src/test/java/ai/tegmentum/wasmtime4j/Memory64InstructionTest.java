package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for Memory64 instruction support in wasmtime4j.
 *
 * <p>These tests validate the WebAssembly memory64 proposal implementation, covering all
 * instruction types, execution patterns, error handling, and performance characteristics.
 */
@DisplayName("Memory64 Instruction Tests")
class Memory64InstructionTest {

  private static final Logger LOGGER = Logger.getLogger(Memory64InstructionTest.class.getName());

  @Nested
  @DisplayName("Memory64Instruction Enum Tests")
  class Memory64InstructionEnumTest {

    @Test
    @DisplayName("Should have all required load instructions")
    void shouldHaveAllLoadInstructions() {
      // Test that all expected load instructions are present
      assertNotNull(Memory64Instruction.I32_LOAD_64);
      assertNotNull(Memory64Instruction.I64_LOAD_64);
      assertNotNull(Memory64Instruction.F32_LOAD_64);
      assertNotNull(Memory64Instruction.F64_LOAD_64);
      assertNotNull(Memory64Instruction.I32_LOAD8_S_64);
      assertNotNull(Memory64Instruction.I32_LOAD8_U_64);
      assertNotNull(Memory64Instruction.I32_LOAD16_S_64);
      assertNotNull(Memory64Instruction.I32_LOAD16_U_64);
      assertNotNull(Memory64Instruction.I64_LOAD8_S_64);
      assertNotNull(Memory64Instruction.I64_LOAD8_U_64);
      assertNotNull(Memory64Instruction.I64_LOAD16_S_64);
      assertNotNull(Memory64Instruction.I64_LOAD16_U_64);
      assertNotNull(Memory64Instruction.I64_LOAD32_S_64);
      assertNotNull(Memory64Instruction.I64_LOAD32_U_64);
    }

    @Test
    @DisplayName("Should have all required store instructions")
    void shouldHaveAllStoreInstructions() {
      // Test that all expected store instructions are present
      assertNotNull(Memory64Instruction.I32_STORE_64);
      assertNotNull(Memory64Instruction.I64_STORE_64);
      assertNotNull(Memory64Instruction.F32_STORE_64);
      assertNotNull(Memory64Instruction.F64_STORE_64);
      assertNotNull(Memory64Instruction.I32_STORE8_64);
      assertNotNull(Memory64Instruction.I32_STORE16_64);
      assertNotNull(Memory64Instruction.I64_STORE8_64);
      assertNotNull(Memory64Instruction.I64_STORE16_64);
      assertNotNull(Memory64Instruction.I64_STORE32_64);
    }

    @Test
    @DisplayName("Should have all required control instructions")
    void shouldHaveAllControlInstructions() {
      // Test that all expected control instructions are present
      assertNotNull(Memory64Instruction.MEMORY_SIZE_64);
      assertNotNull(Memory64Instruction.MEMORY_GROW_64);
      assertNotNull(Memory64Instruction.MEMORY_FILL_64);
      assertNotNull(Memory64Instruction.MEMORY_COPY_64);
      assertNotNull(Memory64Instruction.MEMORY_INIT_64);
    }

    @Test
    @DisplayName("Should classify load instructions correctly")
    void shouldClassifyLoadInstructionsCorrectly() {
      assertTrue(Memory64Instruction.I32_LOAD_64.isLoad());
      assertFalse(Memory64Instruction.I32_LOAD_64.isStore());
      assertFalse(Memory64Instruction.I32_LOAD_64.isControlInstruction());
      assertEquals(4, Memory64Instruction.I32_LOAD_64.getMemorySize());
      assertEquals(4, Memory64Instruction.I32_LOAD_64.getRequiredAlignment());
    }

    @Test
    @DisplayName("Should classify store instructions correctly")
    void shouldClassifyStoreInstructionsCorrectly() {
      assertFalse(Memory64Instruction.I32_STORE_64.isLoad());
      assertTrue(Memory64Instruction.I32_STORE_64.isStore());
      assertFalse(Memory64Instruction.I32_STORE_64.isControlInstruction());
      assertEquals(4, Memory64Instruction.I32_STORE_64.getMemorySize());
      assertEquals(4, Memory64Instruction.I32_STORE_64.getRequiredAlignment());
    }

    @Test
    @DisplayName("Should classify control instructions correctly")
    void shouldClassifyControlInstructionsCorrectly() {
      assertFalse(Memory64Instruction.MEMORY_SIZE_64.isLoad());
      assertFalse(Memory64Instruction.MEMORY_SIZE_64.isStore());
      assertTrue(Memory64Instruction.MEMORY_SIZE_64.isControlInstruction());
      assertEquals(0, Memory64Instruction.MEMORY_SIZE_64.getMemorySize());
      assertEquals(1, Memory64Instruction.MEMORY_SIZE_64.getRequiredAlignment());
    }

    @Test
    @DisplayName("Should find instructions by opcode")
    void shouldFindInstructionsByOpcode() {
      Memory64Instruction instruction = Memory64Instruction.fromOpcode(0x20);
      assertEquals(Memory64Instruction.I32_LOAD_64, instruction);
      assertEquals("i32.load", instruction.getMnemonic());
    }

    @Test
    @DisplayName("Should find instructions by mnemonic")
    void shouldFindInstructionsByMnemonic() {
      Memory64Instruction instruction = Memory64Instruction.fromMnemonic("i32.load");
      assertEquals(Memory64Instruction.I32_LOAD_64, instruction);
      assertEquals(0x20, instruction.getOpcode());
    }

    @Test
    @DisplayName("Should throw exception for invalid opcode")
    void shouldThrowExceptionForInvalidOpcode() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> {
                Memory64Instruction.fromOpcode(0xFF);
              });
      assertTrue(exception.getMessage().contains("Unknown memory64 instruction opcode"));
    }

    @Test
    @DisplayName("Should throw exception for invalid mnemonic")
    void shouldThrowExceptionForInvalidMnemonic() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> {
                Memory64Instruction.fromMnemonic("invalid.instruction");
              });
      assertTrue(exception.getMessage().contains("Unknown memory64 instruction mnemonic"));
    }

    @Test
    @DisplayName("Should validate alignment correctly")
    void shouldValidateAlignmentCorrectly() {
      Memory64Instruction instruction = Memory64Instruction.I32_LOAD_64;

      // Valid alignments
      assertDoesNotThrow(() -> instruction.validateAlignment(0));
      assertDoesNotThrow(() -> instruction.validateAlignment(4));
      assertDoesNotThrow(() -> instruction.validateAlignment(8));

      // Invalid alignments
      IllegalArgumentException exception1 =
          assertThrows(
              IllegalArgumentException.class,
              () -> {
                instruction.validateAlignment(1);
              });
      assertTrue(exception1.getMessage().contains("not aligned"));

      IllegalArgumentException exception2 =
          assertThrows(
              IllegalArgumentException.class,
              () -> {
                instruction.validateAlignment(3);
              });
      assertTrue(exception2.getMessage().contains("not aligned"));
    }

    @Test
    @DisplayName("Should validate bounds correctly")
    void shouldValidateBoundsCorrectly() {
      Memory64Instruction instruction = Memory64Instruction.I32_LOAD_64;

      // Valid bounds
      assertDoesNotThrow(() -> instruction.validateBounds(0, 1024));
      assertDoesNotThrow(() -> instruction.validateBounds(1020, 1024));

      // Invalid bounds - negative offset
      IndexOutOfBoundsException exception1 =
          assertThrows(
              IndexOutOfBoundsException.class,
              () -> {
                instruction.validateBounds(-1, 1024);
              });
      assertTrue(exception1.getMessage().contains("Negative memory offset"));

      // Invalid bounds - exceeds memory size
      IndexOutOfBoundsException exception2 =
          assertThrows(
              IndexOutOfBoundsException.class,
              () -> {
                instruction.validateBounds(1021, 1024);
              });
      assertTrue(exception2.getMessage().contains("exceeds memory size"));
    }
  }

  @Nested
  @DisplayName("Memory64InstructionHandler Tests")
  class Memory64InstructionHandlerTest {

    private Memory64InstructionHandler handler;
    private MockMemory64 mockMemory;

    @BeforeEach
    void setUp() {
      handler = new Memory64InstructionHandler();
      mockMemory = new MockMemory64();
    }

    @Test
    @DisplayName("Should execute load instructions by opcode")
    void shouldExecuteLoadInstructionsByOpcode() {
      // Set up test data in mock memory
      mockMemory.writeInt32(0, 0x12345678);

      // Execute i32.load instruction
      long result = handler.executeByOpcode(0x20, mockMemory, 0, 0);
      assertEquals(0x12345678, result);

      // Verify statistics
      Memory64InstructionHandler.ExecutionStatistics stats = handler.getStatistics();
      assertEquals(1, stats.getTotalExecutions());
      assertEquals(1, stats.getExecutionCount(Memory64Instruction.I32_LOAD_64));
    }

    @Test
    @DisplayName("Should execute store instructions by mnemonic")
    void shouldExecuteStoreInstructionsByMnemonic() {
      // Execute i32.store instruction
      long result = handler.executeByMnemonic("i32.store", mockMemory, 0, 0x87654321);
      assertEquals(0, result); // Store operations return 0

      // Verify the value was stored
      assertEquals(0x87654321, mockMemory.readInt32(0));
    }

    @Test
    @DisplayName("Should execute control instructions")
    void shouldExecuteControlInstructions() {
      // Execute memory.size instruction
      long size = handler.executeByOpcode(0x40, mockMemory, 0, 0);
      assertEquals(mockMemory.getSize64(), size);

      // Execute memory.grow instruction
      long oldSize = handler.executeByOpcode(0x41, mockMemory, 0, 1);
      assertEquals(mockMemory.getSize64() - 1, oldSize);
    }

    @Test
    @DisplayName("Should validate memory support")
    void shouldValidateMemorySupport() {
      // Test with 64-bit memory
      assertDoesNotThrow(() -> handler.validateMemory64Support(mockMemory));

      // Test with 32-bit memory
      MockMemory32 memory32 = new MockMemory32();
      UnsupportedOperationException exception =
          assertThrows(
              UnsupportedOperationException.class,
              () -> {
                handler.validateMemory64Support(memory32);
              });
      assertTrue(exception.getMessage().contains("does not support 64-bit addressing"));
    }

    @Test
    @DisplayName("Should validate instruction parameters")
    void shouldValidateInstructionParameters() {
      Memory64Instruction instruction = Memory64Instruction.I32_LOAD_64;

      // Valid parameters
      assertDoesNotThrow(() -> handler.validateInstructionParameters(instruction, mockMemory, 0));

      // Invalid alignment
      IllegalArgumentException alignException =
          assertThrows(
              IllegalArgumentException.class,
              () -> {
                handler.validateInstructionParameters(instruction, mockMemory, 1);
              });
      assertTrue(alignException.getMessage().contains("not aligned"));

      // Out of bounds
      IndexOutOfBoundsException boundsException =
          assertThrows(
              IndexOutOfBoundsException.class,
              () -> {
                handler.validateInstructionParameters(
                    instruction, mockMemory, mockMemory.getSizeInBytes64());
              });
      assertTrue(boundsException.getMessage().contains("exceeds memory size"));
    }

    @Test
    @DisplayName("Should track execution statistics")
    void shouldTrackExecutionStatistics() {
      // Execute several instructions
      handler.executeByOpcode(0x20, mockMemory, 0, 0); // i32.load
      handler.executeByOpcode(0x30, mockMemory, 4, 42); // i32.store
      handler.executeByOpcode(0x20, mockMemory, 8, 0); // i32.load again

      Memory64InstructionHandler.ExecutionStatistics stats = handler.getStatistics();
      assertEquals(3, stats.getTotalExecutions());
      assertEquals(2, stats.getExecutionCount(Memory64Instruction.I32_LOAD_64));
      assertEquals(1, stats.getExecutionCount(Memory64Instruction.I32_STORE_64));

      assertTrue(stats.getAverageExecutionTime(Memory64Instruction.I32_LOAD_64) >= 0);
    }

    @Test
    @DisplayName("Should handle instruction errors")
    void shouldHandleInstructionErrors() {
      // Test with invalid memory (closed)
      mockMemory.close();

      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> {
                handler.executeByOpcode(0x20, mockMemory, 0, 0);
              });
      assertTrue(exception.getMessage().contains("Memory is closed"));

      Memory64InstructionHandler.ExecutionStatistics stats = handler.getStatistics();
      assertEquals(1, stats.getTotalErrors());
      assertEquals(1, stats.getErrorCount(Memory64Instruction.I32_LOAD_64));
    }

    @Test
    @DisplayName("Should support instruction queries")
    void shouldSupportInstructionQueries() {
      assertTrue(handler.isInstructionSupported(0x20)); // i32.load
      assertTrue(handler.isInstructionSupported("i32.store"));
      assertFalse(handler.isInstructionSupported(0xFF)); // Invalid opcode
      assertFalse(handler.isInstructionSupported("invalid.instruction"));
    }

    @Test
    @DisplayName("Should reset statistics")
    void shouldResetStatistics() {
      // Execute some instructions
      handler.executeByOpcode(0x20, mockMemory, 0, 0);
      assertEquals(1, handler.getStatistics().getTotalExecutions());

      // Reset statistics
      handler.resetStatistics();
      assertEquals(0, handler.getStatistics().getTotalExecutions());
    }
  }

  @Nested
  @DisplayName("Memory64 Instruction Execution Tests")
  class Memory64InstructionExecutionTest {

    private MockMemory64 mockMemory;

    @BeforeEach
    void setUp() {
      mockMemory = new MockMemory64();
    }

    @Test
    @DisplayName("Should execute load/store instruction pairs correctly")
    void shouldExecuteLoadStoreInstructionPairsCorrectly() {
      Memory64Instruction storeInst = Memory64Instruction.I32_STORE_64;
      Memory64Instruction loadInst = Memory64Instruction.I32_LOAD_64;

      // Store a value
      long storeResult = storeInst.execute(mockMemory, 0, 0xDEADBEEF);
      assertEquals(0, storeResult);

      // Load the value back
      long loadResult = loadInst.execute(mockMemory, 0, 0);
      assertEquals(0xDEADBEEF, loadResult);
    }

    @Test
    @DisplayName("Should handle signed/unsigned load variations")
    void shouldHandleSignedUnsignedLoadVariations() {
      // Store a byte with the sign bit set
      mockMemory.writeByte(0, (byte) 0xFF);

      // Load as signed 8-bit
      long signedResult = Memory64Instruction.I32_LOAD8_S_64.execute(mockMemory, 0, 0);
      assertEquals(-1, signedResult);

      // Load as unsigned 8-bit
      long unsignedResult = Memory64Instruction.I32_LOAD8_U_64.execute(mockMemory, 0, 0);
      assertEquals(255, unsignedResult);
    }

    @Test
    @DisplayName("Should handle float operations correctly")
    void shouldHandleFloatOperationsCorrectly() {
      float testValue = 3.14159f;
      int floatBits = Float.floatToIntBits(testValue);

      // Store float as int bits
      Memory64Instruction.F32_STORE_64.execute(mockMemory, 0, floatBits);

      // Load back as float
      long loadedBits = Memory64Instruction.F32_LOAD_64.execute(mockMemory, 0, 0);
      float loadedValue = Float.intBitsToFloat((int) loadedBits);

      assertEquals(testValue, loadedValue, 0.0001f);
    }

    @Test
    @DisplayName("Should handle double operations correctly")
    void shouldHandleDoubleOperationsCorrectly() {
      double testValue = 2.71828;
      long doubleBits = Double.doubleToLongBits(testValue);

      // Store double as long bits
      Memory64Instruction.F64_STORE_64.execute(mockMemory, 0, doubleBits);

      // Load back as double
      long loadedBits = Memory64Instruction.F64_LOAD_64.execute(mockMemory, 0, 0);
      double loadedValue = Double.longBitsToDouble(loadedBits);

      assertEquals(testValue, loadedValue, 0.0001);
    }

    @Test
    @DisplayName("Should handle memory control operations")
    void shouldHandleMemoryControlOperations() {
      long initialSize = mockMemory.getSize64();

      // Test memory.size
      long sizeResult = Memory64Instruction.MEMORY_SIZE_64.execute(mockMemory, 0, 0);
      assertEquals(initialSize, sizeResult);

      // Test memory.grow
      long growResult = Memory64Instruction.MEMORY_GROW_64.execute(mockMemory, 0, 2);
      assertEquals(initialSize, growResult); // Returns previous size
      assertEquals(initialSize + 2, mockMemory.getSize64()); // New size should be increased
    }

    @Test
    @DisplayName("Should enforce alignment requirements")
    void shouldEnforceAlignmentRequirements() {
      Memory64Instruction instruction = Memory64Instruction.I32_LOAD_64;

      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> {
                instruction.execute(mockMemory, 1, 0); // Misaligned offset
              });
      assertTrue(exception.getMessage().contains("not aligned"));
    }

    @Test
    @DisplayName("Should enforce bounds checking")
    void shouldEnforceBoundsChecking() {
      Memory64Instruction instruction = Memory64Instruction.I32_LOAD_64;
      long memorySize = mockMemory.getSizeInBytes64();

      IndexOutOfBoundsException exception =
          assertThrows(
              IndexOutOfBoundsException.class,
              () -> {
                instruction.execute(
                    mockMemory, memorySize - 2, 0); // Would read beyond memory bounds
              });
      assertTrue(exception.getMessage().contains("exceeds memory size"));
    }

    @Test
    @DisplayName("Should reject unsupported memory types")
    void shouldRejectUnsupportedMemoryTypes() {
      MockMemory32 memory32 = new MockMemory32();
      Memory64Instruction instruction = Memory64Instruction.I32_LOAD_64;

      UnsupportedOperationException exception =
          assertThrows(
              UnsupportedOperationException.class,
              () -> {
                instruction.execute(memory32, 0, 0);
              });
      assertTrue(exception.getMessage().contains("does not support 64-bit addressing"));
    }
  }

  // Mock implementations for testing

  private static class MockMemory64 implements WasmMemory {
    private byte[] memory = new byte[8192]; // 8KB test memory
    private boolean closed = false;
    private long currentSize = 1; // 1 page initially

    @Override
    public boolean supports64BitAddressing() {
      return true;
    }

    @Override
    public int getSize() {
      return (int) currentSize;
    }

    @Override
    public long getSize64() {
      return currentSize;
    }

    @Override
    public int getMaxSize() {
      return 16; // 16 pages max
    }

    @Override
    public long getMaxSize64() {
      return 16;
    }

    @Override
    public int grow(int pages) {
      return (int) grow64(pages);
    }

    @Override
    public long grow64(long pages) {
      if (closed) throw new RuntimeException("Memory is closed");
      long oldSize = currentSize;
      currentSize += pages;
      return oldSize;
    }

    @Override
    public ByteBuffer getBuffer() {
      if (closed) throw new RuntimeException("Memory is closed");
      return ByteBuffer.wrap(memory);
    }

    @Override
    public byte readByte(int offset) {
      return readByte64(offset);
    }

    @Override
    public byte readByte64(long offset) {
      if (closed) throw new RuntimeException("Memory is closed");
      return memory[(int) offset];
    }

    @Override
    public void writeByte(int offset, byte value) {
      writeByte64(offset, value);
    }

    @Override
    public void writeByte64(long offset, byte value) {
      if (closed) throw new RuntimeException("Memory is closed");
      memory[(int) offset] = value;
    }

    @Override
    public void readBytes(int offset, byte[] dest, int destOffset, int length) {
      readBytes64(offset, dest, destOffset, length);
    }

    @Override
    public void readBytes64(long offset, byte[] dest, int destOffset, int length) {
      if (closed) throw new RuntimeException("Memory is closed");
      System.arraycopy(memory, (int) offset, dest, destOffset, length);
    }

    @Override
    public void writeBytes(int offset, byte[] src, int srcOffset, int length) {
      writeBytes64(offset, src, srcOffset, length);
    }

    @Override
    public void writeBytes64(long offset, byte[] src, int srcOffset, int length) {
      if (closed) throw new RuntimeException("Memory is closed");
      System.arraycopy(src, srcOffset, memory, (int) offset, length);
    }

    public void writeInt32(long offset, int value) {
      writeBytes64(
          offset,
          new byte[] {
            (byte) (value & 0xFF),
            (byte) ((value >> 8) & 0xFF),
            (byte) ((value >> 16) & 0xFF),
            (byte) ((value >> 24) & 0xFF)
          },
          0,
          4);
    }

    public int readInt32(long offset) {
      byte[] bytes = new byte[4];
      readBytes64(offset, bytes, 0, 4);
      return (bytes[0] & 0xFF)
          | ((bytes[1] & 0xFF) << 8)
          | ((bytes[2] & 0xFF) << 16)
          | ((bytes[3] & 0xFF) << 24);
    }

    public void close() {
      closed = true;
    }

    // Minimal implementations for other required methods
    @Override
    public void copy(int destOffset, int srcOffset, int length) {}

    @Override
    public void fill(int offset, byte value, int length) {}

    @Override
    public void init(int destOffset, int dataSegmentIndex, int srcOffset, int length) {}

    @Override
    public void dropDataSegment(int dataSegmentIndex) {}

    @Override
    public boolean isShared() {
      return false;
    }

    @Override
    public int atomicCompareAndSwapInt(int offset, int expected, int newValue) {
      return 0;
    }

    @Override
    public long atomicCompareAndSwapLong(int offset, long expected, long newValue) {
      return 0;
    }

    @Override
    public int atomicLoadInt(int offset) {
      return 0;
    }

    @Override
    public long atomicLoadLong(int offset) {
      return 0;
    }

    @Override
    public void atomicStoreInt(int offset, int value) {}

    @Override
    public void atomicStoreLong(int offset, long value) {}

    @Override
    public int atomicAddInt(int offset, int value) {
      return 0;
    }

    @Override
    public long atomicAddLong(int offset, long value) {
      return 0;
    }

    @Override
    public int atomicAndInt(int offset, int value) {
      return 0;
    }

    @Override
    public int atomicOrInt(int offset, int value) {
      return 0;
    }

    @Override
    public int atomicXorInt(int offset, int value) {
      return 0;
    }

    @Override
    public void atomicFence() {}

    @Override
    public int atomicNotify(int offset, int count) {
      return 0;
    }

    @Override
    public int atomicWait32(int offset, int expected, long timeoutNanos) {
      return 0;
    }

    @Override
    public int atomicWait64(int offset, long expected, long timeoutNanos) {
      return 0;
    }
  }

  private static class MockMemory32 implements WasmMemory {
    @Override
    public boolean supports64BitAddressing() {
      return false;
    }

    @Override
    public int getSize() {
      return 1;
    }

    @Override
    public int getMaxSize() {
      return 16;
    }

    @Override
    public int grow(int pages) {
      return 0;
    }

    @Override
    public ByteBuffer getBuffer() {
      return ByteBuffer.allocate(65536);
    }

    @Override
    public byte readByte(int offset) {
      return 0;
    }

    @Override
    public void writeByte(int offset, byte value) {}

    @Override
    public void readBytes(int offset, byte[] dest, int destOffset, int length) {}

    @Override
    public void writeBytes(int offset, byte[] src, int srcOffset, int length) {}

    @Override
    public void copy(int destOffset, int srcOffset, int length) {}

    @Override
    public void fill(int offset, byte value, int length) {}

    @Override
    public void init(int destOffset, int dataSegmentIndex, int srcOffset, int length) {}

    @Override
    public void dropDataSegment(int dataSegmentIndex) {}

    @Override
    public boolean isShared() {
      return false;
    }

    @Override
    public int atomicCompareAndSwapInt(int offset, int expected, int newValue) {
      return 0;
    }

    @Override
    public long atomicCompareAndSwapLong(int offset, long expected, long newValue) {
      return 0;
    }

    @Override
    public int atomicLoadInt(int offset) {
      return 0;
    }

    @Override
    public long atomicLoadLong(int offset) {
      return 0;
    }

    @Override
    public void atomicStoreInt(int offset, int value) {}

    @Override
    public void atomicStoreLong(int offset, long value) {}

    @Override
    public int atomicAddInt(int offset, int value) {
      return 0;
    }

    @Override
    public long atomicAddLong(int offset, long value) {
      return 0;
    }

    @Override
    public int atomicAndInt(int offset, int value) {
      return 0;
    }

    @Override
    public int atomicOrInt(int offset, int value) {
      return 0;
    }

    @Override
    public int atomicXorInt(int offset, int value) {
      return 0;
    }

    @Override
    public void atomicFence() {}

    @Override
    public int atomicNotify(int offset, int count) {
      return 0;
    }

    @Override
    public int atomicWait32(int offset, int expected, long timeoutNanos) {
      return 0;
    }

    @Override
    public int atomicWait64(int offset, long expected, long timeoutNanos) {
      return 0;
    }
  }
}
