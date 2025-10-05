package ai.tegmentum.wasmtime4j;

/**
 * Represents WebAssembly memory64 proposal instructions for 64-bit memory operations.
 *
 * <p>The memory64 proposal extends WebAssembly with support for linear memories larger than 4GB by
 * introducing 64-bit memory addressing. This interface defines the instruction operations that work
 * with 64-bit memory indices and offsets.
 *
 * <p>Key differences from 32-bit memory instructions:
 *
 * <ul>
 *   <li>Memory indices are 64-bit (i64) instead of 32-bit (i32)
 *   <li>Offsets can exceed 4GB boundaries
 *   <li>Memory size and growth operations use 64-bit values
 *   <li>Bulk operations can handle larger data ranges
 * </ul>
 *
 * @since 1.1.0
 */
public enum Memory64Instruction {

  // Load Instructions with 64-bit addressing

  /** Load 32-bit integer from memory with 64-bit offset (i64.load). */
  I32_LOAD_64(0x20, "i32.load", 4, true),

  /** Load 64-bit integer from memory with 64-bit offset (i64.load). */
  I64_LOAD_64(0x21, "i64.load", 8, true),

  /** Load 32-bit float from memory with 64-bit offset (f32.load). */
  F32_LOAD_64(0x22, "f32.load", 4, true),

  /** Load 64-bit float from memory with 64-bit offset (f64.load). */
  F64_LOAD_64(0x23, "f64.load", 8, true),

  /** Load signed 8-bit integer from memory with 64-bit offset (i32.load8_s). */
  I32_LOAD8_S_64(0x24, "i32.load8_s", 1, true),

  /** Load unsigned 8-bit integer from memory with 64-bit offset (i32.load8_u). */
  I32_LOAD8_U_64(0x25, "i32.load8_u", 1, true),

  /** Load signed 16-bit integer from memory with 64-bit offset (i32.load16_s). */
  I32_LOAD16_S_64(0x26, "i32.load16_s", 2, true),

  /** Load unsigned 16-bit integer from memory with 64-bit offset (i32.load16_u). */
  I32_LOAD16_U_64(0x27, "i32.load16_u", 2, true),

  /** Load signed 8-bit integer from memory with 64-bit offset (i64.load8_s). */
  I64_LOAD8_S_64(0x28, "i64.load8_s", 1, true),

  /** Load unsigned 8-bit integer from memory with 64-bit offset (i64.load8_u). */
  I64_LOAD8_U_64(0x29, "i64.load8_u", 1, true),

  /** Load signed 16-bit integer from memory with 64-bit offset (i64.load16_s). */
  I64_LOAD16_S_64(0x2A, "i64.load16_s", 2, true),

  /** Load unsigned 16-bit integer from memory with 64-bit offset (i64.load16_u). */
  I64_LOAD16_U_64(0x2B, "i64.load16_u", 2, true),

  /** Load signed 32-bit integer from memory with 64-bit offset (i64.load32_s). */
  I64_LOAD32_S_64(0x2C, "i64.load32_s", 4, true),

  /** Load unsigned 32-bit integer from memory with 64-bit offset (i64.load32_u). */
  I64_LOAD32_U_64(0x2D, "i64.load32_u", 4, true),

  // Store Instructions with 64-bit addressing

  /** Store 32-bit integer to memory with 64-bit offset (i32.store). */
  I32_STORE_64(0x30, "i32.store", 4, false),

  /** Store 64-bit integer to memory with 64-bit offset (i64.store). */
  I64_STORE_64(0x31, "i64.store", 8, false),

  /** Store 32-bit float to memory with 64-bit offset (f32.store). */
  F32_STORE_64(0x32, "f32.store", 4, false),

  /** Store 64-bit float to memory with 64-bit offset (f64.store). */
  F64_STORE_64(0x33, "f64.store", 8, false),

  /** Store 8-bit integer to memory with 64-bit offset (i32.store8). */
  I32_STORE8_64(0x34, "i32.store8", 1, false),

  /** Store 16-bit integer to memory with 64-bit offset (i32.store16). */
  I32_STORE16_64(0x35, "i32.store16", 2, false),

  /** Store 8-bit integer to memory with 64-bit offset (i64.store8). */
  I64_STORE8_64(0x36, "i64.store8", 1, false),

  /** Store 16-bit integer to memory with 64-bit offset (i64.store16). */
  I64_STORE16_64(0x37, "i64.store16", 2, false),

  /** Store 32-bit integer to memory with 64-bit offset (i64.store32). */
  I64_STORE32_64(0x38, "i64.store32", 4, false),

  // Memory Control Instructions with 64-bit addressing

  /** Get current memory size in pages (memory.size with 64-bit result). */
  MEMORY_SIZE_64(0x40, "memory.size", 0, false),

  /** Grow memory by given number of pages (memory.grow with 64-bit operands). */
  MEMORY_GROW_64(0x41, "memory.grow", 0, false),

  /** Fill memory region with byte value (memory.fill with 64-bit addressing). */
  MEMORY_FILL_64(0x42, "memory.fill", 0, false),

  /** Copy memory region (memory.copy with 64-bit addressing). */
  MEMORY_COPY_64(0x43, "memory.copy", 0, false),

  /** Initialize memory from data segment (memory.init with 64-bit addressing). */
  MEMORY_INIT_64(0x44, "memory.init", 0, false);

  private final int opcode;
  private final String mnemonic;
  private final int memorySize;
  private final boolean isLoad;

  Memory64Instruction(
      final int opcode, final String mnemonic, final int memorySize, final boolean isLoad) {
    this.opcode = opcode;
    this.mnemonic = mnemonic;
    this.memorySize = memorySize;
    this.isLoad = isLoad;
  }

  /**
   * Gets the instruction opcode.
   *
   * @return the opcode value
   */
  public int getOpcode() {
    return opcode;
  }

  /**
   * Gets the instruction mnemonic (human-readable name).
   *
   * @return the mnemonic string
   */
  public String getMnemonic() {
    return mnemonic;
  }

  /**
   * Gets the memory size in bytes that this instruction operates on.
   *
   * @return the memory size in bytes, or 0 for control instructions
   */
  public int getMemorySize() {
    return memorySize;
  }

  /**
   * Checks if this is a load instruction.
   *
   * @return true if this is a load instruction, false for store or control instructions
   */
  public boolean isLoad() {
    return isLoad;
  }

  /**
   * Checks if this is a store instruction.
   *
   * @return true if this is a store instruction, false for load or control instructions
   */
  public boolean isStore() {
    return !isLoad && memorySize > 0;
  }

  /**
   * Checks if this is a memory control instruction.
   *
   * @return true if this is a control instruction (size, grow, fill, copy, init)
   */
  public boolean isControlInstruction() {
    return memorySize == 0;
  }

  /**
   * Gets the required memory alignment for this instruction.
   *
   * <p>Returns the natural alignment for the memory size. For control instructions, returns 1 (byte
   * alignment).
   *
   * @return the required alignment in bytes
   */
  public int getRequiredAlignment() {
    return memorySize > 0 ? memorySize : 1;
  }

  /**
   * Validates that the given offset is properly aligned for this instruction.
   *
   * @param offset the memory offset to validate
   * @throws IllegalArgumentException if the offset is not properly aligned
   */
  public void validateAlignment(final long offset) {
    final int alignment = getRequiredAlignment();
    if (offset % alignment != 0) {
      throw new IllegalArgumentException(
          String.format(
              "Offset 0x%X is not aligned to %d bytes for instruction %s",
              offset, alignment, mnemonic));
    }
  }

  /**
   * Validates that the operation is within memory bounds.
   *
   * @param offset the memory offset
   * @param memorySize the current memory size in bytes
   * @throws IndexOutOfBoundsException if the operation would exceed memory bounds
   */
  public void validateBounds(final long offset, final long memorySize) {
    if (this.memorySize > 0) {
      if (offset < 0) {
        throw new IndexOutOfBoundsException("Negative memory offset: " + offset);
      }
      if (offset + this.memorySize > memorySize) {
        throw new IndexOutOfBoundsException(
            String.format(
                "Memory access at offset 0x%X + %d bytes exceeds memory size 0x%X",
                offset, this.memorySize, memorySize));
      }
    }
  }

  /**
   * Executes a memory operation instruction.
   *
   * <p>This is a generic execution interface that can be implemented by memory runtimes to handle
   * 64-bit memory instructions uniformly.
   *
   * @param memory the memory instance to operate on
   * @param offset the memory offset (64-bit)
   * @param value the value for store operations (ignored for load operations)
   * @return the loaded value for load operations, or 0 for store/control operations
   * @throws UnsupportedOperationException if the instruction is not supported
   * @throws IndexOutOfBoundsException if the operation is out of bounds
   * @throws IllegalArgumentException if parameters are invalid
   */
  public long execute(final WasmMemory memory, final long offset, final long value) {
    if (!memory.supports64BitAddressing()) {
      throw new UnsupportedOperationException(
          "Memory does not support 64-bit addressing for instruction: " + mnemonic);
    }

    // Validate bounds first (security), then alignment (performance)
    validateBounds(offset, memory.getSizeInBytes64());
    validateAlignment(offset);

    switch (this) {
      case I32_LOAD_64:
        return loadInt32(memory, offset);
      case I64_LOAD_64:
        return loadInt64(memory, offset);
      case F32_LOAD_64:
        return Float.floatToIntBits(loadFloat32(memory, offset));
      case F64_LOAD_64:
        return Double.doubleToLongBits(loadFloat64(memory, offset));
      case I32_LOAD8_S_64:
        return loadInt8Signed(memory, offset);
      case I32_LOAD8_U_64:
        return loadInt8Unsigned(memory, offset);
      case I32_LOAD16_S_64:
        return loadInt16Signed(memory, offset);
      case I32_LOAD16_U_64:
        return loadInt16Unsigned(memory, offset);
      case I64_LOAD8_S_64:
        return loadInt8Signed(memory, offset);
      case I64_LOAD8_U_64:
        return loadInt8Unsigned(memory, offset);
      case I64_LOAD16_S_64:
        return loadInt16Signed(memory, offset);
      case I64_LOAD16_U_64:
        return loadInt16Unsigned(memory, offset);
      case I64_LOAD32_S_64:
        return loadInt32Signed(memory, offset);
      case I64_LOAD32_U_64:
        return loadInt32Unsigned(memory, offset);
      case I32_STORE_64:
        storeInt32(memory, offset, (int) value);
        return 0;
      case I64_STORE_64:
        storeInt64(memory, offset, value);
        return 0;
      case F32_STORE_64:
        storeFloat32(memory, offset, Float.intBitsToFloat((int) value));
        return 0;
      case F64_STORE_64:
        storeFloat64(memory, offset, Double.longBitsToDouble(value));
        return 0;
      case I32_STORE8_64:
      case I64_STORE8_64:
        storeInt8(memory, offset, (byte) value);
        return 0;
      case I32_STORE16_64:
      case I64_STORE16_64:
        storeInt16(memory, offset, (short) value);
        return 0;
      case I64_STORE32_64:
        storeInt32(memory, offset, (int) value);
        return 0;
      case MEMORY_SIZE_64:
        return memory.getSize64();
      case MEMORY_GROW_64:
        return memory.grow64(value);
      default:
        throw new UnsupportedOperationException("Instruction not implemented: " + mnemonic);
    }
  }

  private int loadInt32(final WasmMemory memory, final long offset) {
    final byte[] bytes = new byte[4];
    memory.readBytes64(offset, bytes, 0, 4);
    return (bytes[0] & 0xFF)
        | ((bytes[1] & 0xFF) << 8)
        | ((bytes[2] & 0xFF) << 16)
        | ((bytes[3] & 0xFF) << 24);
  }

  private long loadInt64(final WasmMemory memory, final long offset) {
    final byte[] bytes = new byte[8];
    memory.readBytes64(offset, bytes, 0, 8);
    return (bytes[0] & 0xFFL)
        | ((bytes[1] & 0xFFL) << 8)
        | ((bytes[2] & 0xFFL) << 16)
        | ((bytes[3] & 0xFFL) << 24)
        | ((bytes[4] & 0xFFL) << 32)
        | ((bytes[5] & 0xFFL) << 40)
        | ((bytes[6] & 0xFFL) << 48)
        | ((bytes[7] & 0xFFL) << 56);
  }

  private float loadFloat32(final WasmMemory memory, final long offset) {
    return Float.intBitsToFloat(loadInt32(memory, offset));
  }

  private double loadFloat64(final WasmMemory memory, final long offset) {
    return Double.longBitsToDouble(loadInt64(memory, offset));
  }

  private long loadInt8Signed(final WasmMemory memory, final long offset) {
    return memory.readByte64(offset);
  }

  private long loadInt8Unsigned(final WasmMemory memory, final long offset) {
    return memory.readByte64(offset) & 0xFF;
  }

  private long loadInt16Signed(final WasmMemory memory, final long offset) {
    final byte[] bytes = new byte[2];
    memory.readBytes64(offset, bytes, 0, 2);
    return (short) ((bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8));
  }

  private long loadInt16Unsigned(final WasmMemory memory, final long offset) {
    final byte[] bytes = new byte[2];
    memory.readBytes64(offset, bytes, 0, 2);
    return (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8);
  }

  private long loadInt32Signed(final WasmMemory memory, final long offset) {
    return loadInt32(memory, offset);
  }

  private long loadInt32Unsigned(final WasmMemory memory, final long offset) {
    return loadInt32(memory, offset) & 0xFFFFFFFFL;
  }

  private void storeInt8(final WasmMemory memory, final long offset, final byte value) {
    memory.writeByte64(offset, value);
  }

  private void storeInt16(final WasmMemory memory, final long offset, final short value) {
    final byte[] bytes = new byte[2];
    bytes[0] = (byte) (value & 0xFF);
    bytes[1] = (byte) ((value >> 8) & 0xFF);
    memory.writeBytes64(offset, bytes, 0, 2);
  }

  private void storeInt32(final WasmMemory memory, final long offset, final int value) {
    final byte[] bytes = new byte[4];
    bytes[0] = (byte) (value & 0xFF);
    bytes[1] = (byte) ((value >> 8) & 0xFF);
    bytes[2] = (byte) ((value >> 16) & 0xFF);
    bytes[3] = (byte) ((value >> 24) & 0xFF);
    memory.writeBytes64(offset, bytes, 0, 4);
  }

  private void storeInt64(final WasmMemory memory, final long offset, final long value) {
    final byte[] bytes = new byte[8];
    bytes[0] = (byte) (value & 0xFF);
    bytes[1] = (byte) ((value >> 8) & 0xFF);
    bytes[2] = (byte) ((value >> 16) & 0xFF);
    bytes[3] = (byte) ((value >> 24) & 0xFF);
    bytes[4] = (byte) ((value >> 32) & 0xFF);
    bytes[5] = (byte) ((value >> 40) & 0xFF);
    bytes[6] = (byte) ((value >> 48) & 0xFF);
    bytes[7] = (byte) ((value >> 56) & 0xFF);
    memory.writeBytes64(offset, bytes, 0, 8);
  }

  private void storeFloat32(final WasmMemory memory, final long offset, final float value) {
    storeInt32(memory, offset, Float.floatToIntBits(value));
  }

  private void storeFloat64(final WasmMemory memory, final long offset, final double value) {
    storeInt64(memory, offset, Double.doubleToLongBits(value));
  }

  /**
   * Finds a Memory64Instruction by its opcode.
   *
   * @param opcode the instruction opcode
   * @return the matching instruction
   * @throws IllegalArgumentException if no instruction matches the opcode
   */
  public static Memory64Instruction fromOpcode(final int opcode) {
    for (final Memory64Instruction instruction : values()) {
      if (instruction.opcode == opcode) {
        return instruction;
      }
    }
    throw new IllegalArgumentException(
        "Unknown memory64 instruction opcode: 0x" + Integer.toHexString(opcode));
  }

  /**
   * Finds a Memory64Instruction by its mnemonic.
   *
   * @param mnemonic the instruction mnemonic
   * @return the matching instruction
   * @throws IllegalArgumentException if no instruction matches the mnemonic
   */
  public static Memory64Instruction fromMnemonic(final String mnemonic) {
    for (final Memory64Instruction instruction : values()) {
      if (instruction.mnemonic.equals(mnemonic)) {
        return instruction;
      }
    }
    throw new IllegalArgumentException("Unknown memory64 instruction mnemonic: " + mnemonic);
  }
}
