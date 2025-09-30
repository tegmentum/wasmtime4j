package ai.tegmentum.wasmtime4j;

/**
 * Represents WebAssembly Memory64 proposal table operations with 64-bit addressing.
 *
 * <p>The Memory64 proposal extends table operations to support 64-bit indices and table sizes,
 * enabling tables to hold more than 4 billion elements. This is critical for large-scale
 * WebAssembly applications that need extensive function tables or reference tables.
 *
 * <p>Key features of Memory64 table operations:
 *
 * <ul>
 *   <li>64-bit table indices (i64 instead of i32)
 *   <li>Tables can exceed 4 billion elements
 *   <li>Bulk table operations with 64-bit ranges
 *   <li>Cross-table operations with large indices
 * </ul>
 *
 * @since 1.1.0
 */
public enum Memory64TableOperations {

  /** Get element at 64-bit table index (table.get with i64 index). */
  TABLE_GET_64(0x25, "table.get64", true),

  /** Set element at 64-bit table index (table.set with i64 index). */
  TABLE_SET_64(0x26, "table.set64", false),

  /** Get table size using 64-bit result (table.size with i64 result). */
  TABLE_SIZE_64(0x27, "table.size64", true),

  /** Grow table by 64-bit amount (table.grow with i64 operands). */
  TABLE_GROW_64(0x28, "table.grow64", false),

  /** Fill table region with value (table.fill with i64 addressing). */
  TABLE_FILL_64(0x29, "table.fill64", false),

  /** Copy elements between tables (table.copy with i64 addressing). */
  TABLE_COPY_64(0x2A, "table.copy64", false),

  /** Initialize table from element segment (table.init with i64 addressing). */
  TABLE_INIT_64(0x2B, "table.init64", false);

  private final int opcode;
  private final String mnemonic;
  private final boolean returnsValue;

  Memory64TableOperations(final int opcode, final String mnemonic, final boolean returnsValue) {
    this.opcode = opcode;
    this.mnemonic = mnemonic;
    this.returnsValue = returnsValue;
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
   * Checks if this operation returns a value.
   *
   * @return true if the operation returns a value, false otherwise
   */
  public boolean returnsValue() {
    return returnsValue;
  }

  /**
   * Validates a 64-bit table index for bounds checking.
   *
   * @param index the table index to validate
   * @param tableSize the current table size
   * @throws IndexOutOfBoundsException if the index is out of bounds
   */
  public void validateIndex(final long index, final long tableSize) {
    if (index < 0) {
      throw new IndexOutOfBoundsException("Negative table index: " + index);
    }
    if (index >= tableSize) {
      throw new IndexOutOfBoundsException(
          String.format("Table index %d exceeds table size %d", index, tableSize));
    }
  }

  /**
   * Validates a range operation for 64-bit table addressing.
   *
   * @param startIndex the starting index
   * @param length the length of the range
   * @param tableSize the current table size
   * @throws IndexOutOfBoundsException if the range is invalid
   */
  public void validateRange(final long startIndex, final long length, final long tableSize) {
    if (startIndex < 0) {
      throw new IndexOutOfBoundsException("Negative start index: " + startIndex);
    }
    if (length < 0) {
      throw new IllegalArgumentException("Negative length: " + length);
    }
    if (startIndex + length > tableSize) {
      throw new IndexOutOfBoundsException(
          String.format(
              "Range [%d, %d) exceeds table size %d", startIndex, startIndex + length, tableSize));
    }
  }

  /**
   * Executes a 64-bit table operation.
   *
   * <p>This provides a generic execution interface for table operations with 64-bit addressing
   * capabilities.
   *
   * @param table the table to operate on
   * @param args the operation arguments (varies by operation)
   * @return the result value for operations that return values, or null
   * @throws UnsupportedOperationException if the operation is not supported
   * @throws IndexOutOfBoundsException if indices are out of bounds
   */
  public WasmValue execute(final WasmTable table, final Object... args) {
    if (!table.supports64BitAddressing()) {
      throw new UnsupportedOperationException(
          "Table does not support 64-bit addressing for operation: " + mnemonic);
    }

    final long tableSize = table.getSize64();

    switch (this) {
      case TABLE_GET_64:
        if (args.length < 1) {
          throw new IllegalArgumentException("TABLE_GET_64 requires index argument");
        }
        final long getIndex = ((Number) args[0]).longValue();
        validateIndex(getIndex, tableSize);
        return table.get64(getIndex);

      case TABLE_SET_64:
        if (args.length < 2) {
          throw new IllegalArgumentException("TABLE_SET_64 requires index and value arguments");
        }
        final long setIndex = ((Number) args[0]).longValue();
        final WasmValue setValue = (WasmValue) args[1];
        validateIndex(setIndex, tableSize);
        table.set64(setIndex, setValue);
        return null;

      case TABLE_SIZE_64:
        return WasmValue.i64(tableSize);

      case TABLE_GROW_64:
        if (args.length < 2) {
          throw new IllegalArgumentException("TABLE_GROW_64 requires size and init arguments");
        }
        final long growSize = ((Number) args[0]).longValue();
        final WasmValue initValue = (WasmValue) args[1];
        final long previousSize = table.grow64(growSize, initValue);
        return WasmValue.i64(previousSize);

      case TABLE_FILL_64:
        if (args.length < 3) {
          throw new IllegalArgumentException(
              "TABLE_FILL_64 requires start, length, and value arguments");
        }
        final long fillStart = ((Number) args[0]).longValue();
        final long fillLength = ((Number) args[1]).longValue();
        final WasmValue fillValue = (WasmValue) args[2];
        validateRange(fillStart, fillLength, tableSize);
        table.fill64(fillStart, fillLength, fillValue);
        return null;

      case TABLE_COPY_64:
        if (args.length < 4) {
          throw new IllegalArgumentException(
              "TABLE_COPY_64 requires dest, src, and length arguments");
        }
        final long destStart = ((Number) args[0]).longValue();
        final long srcStart = ((Number) args[1]).longValue();
        final long copyLength = ((Number) args[2]).longValue();
        final WasmTable srcTable = args.length > 3 ? (WasmTable) args[3] : table;
        validateRange(destStart, copyLength, tableSize);
        validateRange(srcStart, copyLength, srcTable.getSize64());
        table.copy64(destStart, srcTable, srcStart, copyLength);
        return null;

      case TABLE_INIT_64:
        if (args.length < 4) {
          throw new IllegalArgumentException(
              "TABLE_INIT_64 requires dest, src, length, and segment arguments");
        }
        final long initDest = ((Number) args[0]).longValue();
        final long initSrc = ((Number) args[1]).longValue();
        final long initLength = ((Number) args[2]).longValue();
        final int elementSegment = ((Number) args[3]).intValue();
        validateRange(initDest, initLength, tableSize);
        table.init64(initDest, elementSegment, initSrc, initLength);
        return null;

      default:
        throw new UnsupportedOperationException("Operation not implemented: " + mnemonic);
    }
  }

  /**
   * Finds a Memory64TableOperations by its opcode.
   *
   * @param opcode the operation opcode
   * @return the matching operation
   * @throws IllegalArgumentException if no operation matches the opcode
   */
  public static Memory64TableOperations fromOpcode(final int opcode) {
    for (final Memory64TableOperations operation : values()) {
      if (operation.opcode == opcode) {
        return operation;
      }
    }
    throw new IllegalArgumentException(
        "Unknown memory64 table operation opcode: 0x" + Integer.toHexString(opcode));
  }

  /**
   * Finds a Memory64TableOperations by its mnemonic.
   *
   * @param mnemonic the operation mnemonic
   * @return the matching operation
   * @throws IllegalArgumentException if no operation matches the mnemonic
   */
  public static Memory64TableOperations fromMnemonic(final String mnemonic) {
    for (final Memory64TableOperations operation : values()) {
      if (operation.mnemonic.equals(mnemonic)) {
        return operation;
      }
    }
    throw new IllegalArgumentException("Unknown memory64 table operation mnemonic: " + mnemonic);
  }
}
