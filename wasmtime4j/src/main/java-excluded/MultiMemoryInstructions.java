package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * WebAssembly multi-memory proposal instructions for multiple linear memory support.
 *
 * <p>The multi-memory proposal extends WebAssembly to support multiple linear memory
 * instances per module, enabling:
 *
 * <ul>
 *   <li>Memory isolation between different program components
 *   <li>Efficient data transfer between memory instances
 *   <li>Memory specialization (e.g., separate heaps for different data types)
 *   <li>Better security boundaries within a single module
 * </ul>
 *
 * <p>Each memory instruction includes a memory index to specify which linear
 * memory instance to operate on, allowing fine-grained control over memory
 * access patterns.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Load from memory instance 1
 * WasmValue value = MultiMemoryInstructions.I32_LOAD_MEMORY.execute(
 *     context, 1, 0x1000); // memory index 1, offset 0x1000
 *
 * // Copy data between memory instances
 * MultiMemoryInstructions.MEMORY_COPY_MULTI.execute(
 *     context, 0, 1, 0, 0x100, 256); // dest mem 0, src mem 1, offsets, length
 * }</pre>
 *
 * @since 1.1.0
 */
public enum MultiMemoryInstructions {

    // Memory Load Instructions with Memory Index

    /** Load 32-bit integer from specified memory (i32.load mem_idx). */
    I32_LOAD_MEMORY(0x40, "i32.load_memory", 4, true),

    /** Load 64-bit integer from specified memory (i64.load mem_idx). */
    I64_LOAD_MEMORY(0x41, "i64.load_memory", 8, true),

    /** Load 32-bit float from specified memory (f32.load mem_idx). */
    F32_LOAD_MEMORY(0x42, "f32.load_memory", 4, true),

    /** Load 64-bit float from specified memory (f64.load mem_idx). */
    F64_LOAD_MEMORY(0x43, "f64.load_memory", 8, true),

    // Memory Store Instructions with Memory Index

    /** Store 32-bit integer to specified memory (i32.store mem_idx). */
    I32_STORE_MEMORY(0x44, "i32.store_memory", 4, false),

    /** Store 64-bit integer to specified memory (i64.store mem_idx). */
    I64_STORE_MEMORY(0x45, "i64.store_memory", 8, false),

    /** Store 32-bit float to specified memory (f32.store mem_idx). */
    F32_STORE_MEMORY(0x46, "f32.store_memory", 4, false),

    /** Store 64-bit float to specified memory (f64.store mem_idx). */
    F64_STORE_MEMORY(0x47, "f64.store_memory", 8, false),

    // Memory Control Instructions with Memory Index

    /** Get size of specified memory (memory.size mem_idx). */
    MEMORY_SIZE_MULTI(0x48, "memory.size_multi", 0, true),

    /** Grow specified memory (memory.grow mem_idx). */
    MEMORY_GROW_MULTI(0x49, "memory.grow_multi", 0, false),

    /** Fill memory region in specified memory (memory.fill mem_idx). */
    MEMORY_FILL_MULTI(0x4A, "memory.fill_multi", 0, false),

    /** Copy within specified memory (memory.copy mem_idx). */
    MEMORY_COPY_MULTI(0x4B, "memory.copy_multi", 0, false),

    /** Copy between different memories (memory.copy dest_mem src_mem). */
    MEMORY_COPY_BETWEEN(0x4C, "memory.copy_between", 0, false),

    /** Initialize memory from data segment with memory index (memory.init mem_idx). */
    MEMORY_INIT_MULTI(0x4D, "memory.init_multi", 0, false),

    // Bulk Data Transfer Operations

    /** Bulk transfer between memories with different addressing modes. */
    MEMORY_TRANSFER_BULK(0x4E, "memory.transfer_bulk", 0, false),

    /** Atomic memory operation across multiple memories. */
    MEMORY_ATOMIC_MULTI(0x4F, "memory.atomic_multi", 0, false);

    private final int opcode;
    private final String mnemonic;
    private final int operandSize;
    private final boolean isLoad;

    MultiMemoryInstructions(final int opcode, final String mnemonic,
                           final int operandSize, final boolean isLoad) {
        this.opcode = opcode;
        this.mnemonic = mnemonic;
        this.operandSize = operandSize;
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
     * Gets the instruction mnemonic.
     *
     * @return the mnemonic string
     */
    public String getMnemonic() {
        return mnemonic;
    }

    /**
     * Gets the operand size in bytes for load/store operations.
     *
     * @return the operand size, or 0 for control instructions
     */
    public int getOperandSize() {
        return operandSize;
    }

    /**
     * Checks if this is a load instruction.
     *
     * @return true if this is a load instruction
     */
    public boolean isLoad() {
        return isLoad;
    }

    /**
     * Checks if this is a store instruction.
     *
     * @return true if this is a store instruction
     */
    public boolean isStore() {
        return !isLoad && operandSize > 0;
    }

    /**
     * Checks if this is a control instruction.
     *
     * @return true if this is a control instruction (size, grow, etc.)
     */
    public boolean isControl() {
        return operandSize == 0;
    }

    /**
     * Executes a multi-memory instruction with the specified parameters.
     *
     * @param context the multi-memory execution context
     * @param args instruction-specific arguments
     * @return the result value for load instructions, null for store/control
     * @throws WasmException if the operation fails
     */
    public WasmValue execute(final MultiMemoryContext context, final Object... args)
            throws WasmException {

        if (context == null) {
            throw new WasmException("Multi-memory context required");
        }

        switch (this) {
            case I32_LOAD_MEMORY:
                return executeLoad(context, WasmValueType.I32, args);
            case I64_LOAD_MEMORY:
                return executeLoad(context, WasmValueType.I64, args);
            case F32_LOAD_MEMORY:
                return executeLoad(context, WasmValueType.F32, args);
            case F64_LOAD_MEMORY:
                return executeLoad(context, WasmValueType.F64, args);

            case I32_STORE_MEMORY:
                return executeStore(context, WasmValueType.I32, args);
            case I64_STORE_MEMORY:
                return executeStore(context, WasmValueType.I64, args);
            case F32_STORE_MEMORY:
                return executeStore(context, WasmValueType.F32, args);
            case F64_STORE_MEMORY:
                return executeStore(context, WasmValueType.F64, args);

            case MEMORY_SIZE_MULTI:
                return executeMemorySize(context, args);
            case MEMORY_GROW_MULTI:
                return executeMemoryGrow(context, args);
            case MEMORY_FILL_MULTI:
                return executeMemoryFill(context, args);
            case MEMORY_COPY_MULTI:
                return executeMemoryCopy(context, args);
            case MEMORY_COPY_BETWEEN:
                return executeMemoryCopyBetween(context, args);
            case MEMORY_INIT_MULTI:
                return executeMemoryInit(context, args);
            case MEMORY_TRANSFER_BULK:
                return executeMemoryTransferBulk(context, args);
            case MEMORY_ATOMIC_MULTI:
                return executeMemoryAtomic(context, args);

            default:
                throw new UnsupportedOperationException("Instruction not implemented: " + mnemonic);
        }
    }

    private WasmValue executeLoad(final MultiMemoryContext context,
                                 final WasmValueType valueType,
                                 final Object... args) throws WasmException {
        if (args.length < 2) {
            throw new WasmException("Load requires memory index and offset");
        }

        final int memoryIndex = ((Number) args[0]).intValue();
        final long offset = ((Number) args[1]).longValue();

        final WasmMemory memory = context.getMemory(memoryIndex);
        if (memory == null) {
            throw new WasmException("Memory not found at index: " + memoryIndex);
        }

        // Perform the load operation based on value type
        switch (valueType) {
            case I32:
                final int intValue = memory.readInt32(offset);
                return WasmValue.i32(intValue);
            case I64:
                final long longValue = memory.readInt64(offset);
                return WasmValue.i64(longValue);
            case F32:
                final float floatValue = memory.readFloat32(offset);
                return WasmValue.f32(floatValue);
            case F64:
                final double doubleValue = memory.readFloat64(offset);
                return WasmValue.f64(doubleValue);
            default:
                throw new WasmException("Unsupported load type: " + valueType);
        }
    }

    private WasmValue executeStore(final MultiMemoryContext context,
                                  final WasmValueType valueType,
                                  final Object... args) throws WasmException {
        if (args.length < 3) {
            throw new WasmException("Store requires memory index, offset, and value");
        }

        final int memoryIndex = ((Number) args[0]).intValue();
        final long offset = ((Number) args[1]).longValue();
        final WasmValue value = (WasmValue) args[2];

        final WasmMemory memory = context.getMemory(memoryIndex);
        if (memory == null) {
            throw new WasmException("Memory not found at index: " + memoryIndex);
        }

        // Perform the store operation based on value type
        switch (valueType) {
            case I32:
                memory.writeInt32(offset, value.asInt());
                break;
            case I64:
                memory.writeInt64(offset, value.asLong());
                break;
            case F32:
                memory.writeFloat32(offset, value.asFloat());
                break;
            case F64:
                memory.writeFloat64(offset, value.asDouble());
                break;
            default:
                throw new WasmException("Unsupported store type: " + valueType);
        }

        return null; // Store operations don't return values
    }

    private WasmValue executeMemorySize(final MultiMemoryContext context,
                                       final Object... args) throws WasmException {
        if (args.length < 1) {
            throw new WasmException("Memory size requires memory index");
        }

        final int memoryIndex = ((Number) args[0]).intValue();
        final WasmMemory memory = context.getMemory(memoryIndex);
        if (memory == null) {
            throw new WasmException("Memory not found at index: " + memoryIndex);
        }

        return WasmValue.i32(memory.getSize());
    }

    private WasmValue executeMemoryGrow(final MultiMemoryContext context,
                                       final Object... args) throws WasmException {
        if (args.length < 2) {
            throw new WasmException("Memory grow requires memory index and page count");
        }

        final int memoryIndex = ((Number) args[0]).intValue();
        final int pageCount = ((Number) args[1]).intValue();

        final WasmMemory memory = context.getMemory(memoryIndex);
        if (memory == null) {
            throw new WasmException("Memory not found at index: " + memoryIndex);
        }

        final int previousSize = memory.grow(pageCount);
        return WasmValue.i32(previousSize);
    }

    private WasmValue executeMemoryFill(final MultiMemoryContext context,
                                       final Object... args) throws WasmException {
        if (args.length < 4) {
            throw new WasmException("Memory fill requires memory index, offset, length, and value");
        }

        final int memoryIndex = ((Number) args[0]).intValue();
        final long offset = ((Number) args[1]).longValue();
        final long length = ((Number) args[2]).longValue();
        final byte value = ((Number) args[3]).byteValue();

        final WasmMemory memory = context.getMemory(memoryIndex);
        if (memory == null) {
            throw new WasmException("Memory not found at index: " + memoryIndex);
        }

        memory.fill(offset, length, value);
        return null;
    }

    private WasmValue executeMemoryCopy(final MultiMemoryContext context,
                                       final Object... args) throws WasmException {
        if (args.length < 4) {
            throw new WasmException("Memory copy requires memory index, dest offset, src offset, and length");
        }

        final int memoryIndex = ((Number) args[0]).intValue();
        final long destOffset = ((Number) args[1]).longValue();
        final long srcOffset = ((Number) args[2]).longValue();
        final long length = ((Number) args[3]).longValue();

        final WasmMemory memory = context.getMemory(memoryIndex);
        if (memory == null) {
            throw new WasmException("Memory not found at index: " + memoryIndex);
        }

        memory.copy(destOffset, srcOffset, length);
        return null;
    }

    private WasmValue executeMemoryCopyBetween(final MultiMemoryContext context,
                                              final Object... args) throws WasmException {
        if (args.length < 5) {
            throw new WasmException("Memory copy between requires dest memory index, src memory index, dest offset, src offset, and length");
        }

        final int destMemoryIndex = ((Number) args[0]).intValue();
        final int srcMemoryIndex = ((Number) args[1]).intValue();
        final long destOffset = ((Number) args[2]).longValue();
        final long srcOffset = ((Number) args[3]).longValue();
        final long length = ((Number) args[4]).longValue();

        final WasmMemory destMemory = context.getMemory(destMemoryIndex);
        final WasmMemory srcMemory = context.getMemory(srcMemoryIndex);

        if (destMemory == null) {
            throw new WasmException("Destination memory not found at index: " + destMemoryIndex);
        }
        if (srcMemory == null) {
            throw new WasmException("Source memory not found at index: " + srcMemoryIndex);
        }

        context.copyBetweenMemories(destMemory, destOffset, srcMemory, srcOffset, length);
        return null;
    }

    private WasmValue executeMemoryInit(final MultiMemoryContext context,
                                       final Object... args) throws WasmException {
        if (args.length < 5) {
            throw new WasmException("Memory init requires memory index, data segment index, dest offset, src offset, and length");
        }

        final int memoryIndex = ((Number) args[0]).intValue();
        final int dataSegmentIndex = ((Number) args[1]).intValue();
        final long destOffset = ((Number) args[2]).longValue();
        final long srcOffset = ((Number) args[3]).longValue();
        final long length = ((Number) args[4]).longValue();

        final WasmMemory memory = context.getMemory(memoryIndex);
        if (memory == null) {
            throw new WasmException("Memory not found at index: " + memoryIndex);
        }

        context.initializeFromDataSegment(memory, dataSegmentIndex, destOffset, srcOffset, length);
        return null;
    }

    private WasmValue executeMemoryTransferBulk(final MultiMemoryContext context,
                                               final Object... args) throws WasmException {
        // Bulk transfer implementation would handle large data transfers
        // between multiple memories with optimization
        if (args.length < 1) {
            throw new WasmException("Bulk transfer requires transfer descriptor");
        }

        final BulkTransferDescriptor descriptor = (BulkTransferDescriptor) args[0];
        context.executeBulkTransfer(descriptor);
        return null;
    }

    private WasmValue executeMemoryAtomic(final MultiMemoryContext context,
                                         final Object... args) throws WasmException {
        // Atomic operations across multiple memories
        if (args.length < 1) {
            throw new WasmException("Atomic operation requires operation descriptor");
        }

        final AtomicOperationDescriptor descriptor = (AtomicOperationDescriptor) args[0];
        return context.executeAtomicOperation(descriptor);
    }

    /**
     * Descriptor for bulk transfer operations between memories.
     */
    public static class BulkTransferDescriptor {
        private final int[] memoryIndices;
        private final long[] offsets;
        private final long[] lengths;
        private final TransferMode mode;

        public BulkTransferDescriptor(final int[] memoryIndices,
                                    final long[] offsets,
                                    final long[] lengths,
                                    final TransferMode mode) {
            this.memoryIndices = memoryIndices.clone();
            this.offsets = offsets.clone();
            this.lengths = lengths.clone();
            this.mode = mode;
        }

        public int[] getMemoryIndices() { return memoryIndices.clone(); }
        public long[] getOffsets() { return offsets.clone(); }
        public long[] getLengths() { return lengths.clone(); }
        public TransferMode getMode() { return mode; }

        public enum TransferMode {
            SEQUENTIAL, PARALLEL, VECTORIZED, STREAMING
        }
    }

    /**
     * Descriptor for atomic operations across multiple memories.
     */
    public static class AtomicOperationDescriptor {
        private final AtomicOperationType operation;
        private final int[] memoryIndices;
        private final long[] offsets;
        private final WasmValue[] operands;

        public AtomicOperationDescriptor(final AtomicOperationType operation,
                                       final int[] memoryIndices,
                                       final long[] offsets,
                                       final WasmValue[] operands) {
            this.operation = operation;
            this.memoryIndices = memoryIndices.clone();
            this.offsets = offsets.clone();
            this.operands = operands.clone();
        }

        public AtomicOperationType getOperation() { return operation; }
        public int[] getMemoryIndices() { return memoryIndices.clone(); }
        public long[] getOffsets() { return offsets.clone(); }
        public WasmValue[] getOperands() { return operands.clone(); }

        public enum AtomicOperationType {
            COMPARE_AND_SWAP, LOAD_LINKED, STORE_CONDITIONAL,
            FETCH_AND_ADD, FETCH_AND_SUB, FETCH_AND_OR, FETCH_AND_AND
        }
    }

    /**
     * Finds a multi-memory instruction by opcode.
     *
     * @param opcode the instruction opcode
     * @return the matching instruction, or null if not found
     */
    public static MultiMemoryInstructions fromOpcode(final int opcode) {
        for (final MultiMemoryInstructions instruction : values()) {
            if (instruction.opcode == opcode) {
                return instruction;
            }
        }
        return null;
    }

    /**
     * Finds a multi-memory instruction by mnemonic.
     *
     * @param mnemonic the instruction mnemonic
     * @return the matching instruction, or null if not found
     */
    public static MultiMemoryInstructions fromMnemonic(final String mnemonic) {
        for (final MultiMemoryInstructions instruction : values()) {
            if (instruction.mnemonic.equals(mnemonic)) {
                return instruction;
            }
        }
        return null;
    }
}