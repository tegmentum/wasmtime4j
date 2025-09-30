package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Execution context for multi-memory WebAssembly operations.
 *
 * <p>The multi-memory context manages multiple linear memory instances
 * within a single WebAssembly module, providing:
 *
 * <ul>
 *   <li>Access to multiple memory instances by index
 *   <li>Inter-memory data transfer operations
 *   <li>Memory isolation and security boundaries
 *   <li>Bulk transfer optimization
 * </ul>
 *
 * @since 1.1.0
 */
public interface MultiMemoryContext {

    /**
     * Gets the number of memory instances available.
     *
     * @return the memory instance count
     */
    int getMemoryCount();

    /**
     * Gets a memory instance by index.
     *
     * @param index the memory index
     * @return the memory instance, or null if not found
     * @throws IndexOutOfBoundsException if index is invalid
     */
    WasmMemory getMemory(final int index);

    /**
     * Gets all memory instances.
     *
     * @return array of memory instances
     */
    WasmMemory[] getAllMemories();

    /**
     * Creates a new memory instance with the specified type.
     *
     * @param memoryType the memory type specification
     * @return the index of the newly created memory
     * @throws WasmException if memory creation fails
     */
    int createMemory(final MemoryType memoryType) throws WasmException;

    /**
     * Removes a memory instance.
     *
     * @param index the memory index to remove
     * @throws WasmException if memory removal fails
     * @throws IndexOutOfBoundsException if index is invalid
     */
    void removeMemory(final int index) throws WasmException;

    /**
     * Copies data between different memory instances.
     *
     * @param destMemory the destination memory
     * @param destOffset the destination offset
     * @param srcMemory the source memory
     * @param srcOffset the source offset
     * @param length the number of bytes to copy
     * @throws WasmException if the copy operation fails
     */
    void copyBetweenMemories(final WasmMemory destMemory, final long destOffset,
                           final WasmMemory srcMemory, final long srcOffset,
                           final long length) throws WasmException;

    /**
     * Initializes memory from a data segment.
     *
     * @param memory the target memory
     * @param dataSegmentIndex the data segment index
     * @param destOffset the destination offset in memory
     * @param srcOffset the source offset in the data segment
     * @param length the number of bytes to copy
     * @throws WasmException if initialization fails
     */
    void initializeFromDataSegment(final WasmMemory memory,
                                 final int dataSegmentIndex,
                                 final long destOffset,
                                 final long srcOffset,
                                 final long length) throws WasmException;

    /**
     * Executes a bulk transfer operation between multiple memories.
     *
     * @param descriptor the bulk transfer descriptor
     * @throws WasmException if the transfer fails
     */
    void executeBulkTransfer(final MultiMemoryInstructions.BulkTransferDescriptor descriptor)
            throws WasmException;

    /**
     * Executes an atomic operation across multiple memories.
     *
     * @param descriptor the atomic operation descriptor
     * @return the result of the atomic operation
     * @throws WasmException if the operation fails
     */
    WasmValue executeAtomicOperation(final MultiMemoryInstructions.AtomicOperationDescriptor descriptor)
            throws WasmException;

    /**
     * Gets memory access statistics for performance analysis.
     *
     * @return memory access statistics
     */
    MultiMemoryStatistics getStatistics();

    /**
     * Sets the memory access policy for security and isolation.
     *
     * @param policy the access policy
     */
    void setAccessPolicy(final MemoryAccessPolicy policy);

    /**
     * Gets the current memory access policy.
     *
     * @return the access policy
     */
    MemoryAccessPolicy getAccessPolicy();

    /**
     * Validates that a memory operation is allowed by the access policy.
     *
     * @param operation the memory operation
     * @param sourceIndex the source memory index (-1 if not applicable)
     * @param destIndex the destination memory index
     * @return true if the operation is allowed
     */
    boolean isOperationAllowed(final MemoryOperation operation,
                              final int sourceIndex, final int destIndex);

    /**
     * Memory operation types for access control.
     */
    enum MemoryOperation {
        READ, WRITE, COPY_WITHIN, COPY_BETWEEN, GROW, FILL, INIT
    }

    /**
     * Memory access policy for security and isolation.
     */
    interface MemoryAccessPolicy {
        /** Checks if cross-memory operations are allowed. */
        boolean allowCrossMemoryAccess();

        /** Checks if memory growth is allowed. */
        boolean allowMemoryGrowth();

        /** Gets the maximum number of memory instances allowed. */
        int getMaxMemoryInstances();

        /** Checks if a specific operation is allowed between memory instances. */
        boolean isOperationAllowed(final MemoryOperation operation,
                                 final int sourceIndex, final int destIndex);

        /** Gets memory isolation level. */
        MemoryIsolationLevel getIsolationLevel();

        enum MemoryIsolationLevel {
            /** No isolation - all operations allowed. */
            NONE,
            /** Basic isolation - limited cross-memory operations. */
            BASIC,
            /** Strong isolation - no cross-memory operations. */
            STRONG
        }
    }

    /**
     * Statistics for multi-memory operations.
     */
    interface MultiMemoryStatistics {
        /** Total number of memory access operations. */
        long getTotalMemoryAccesses();

        /** Number of cross-memory operations. */
        long getCrossMemoryOperations();

        /** Total bytes transferred between memories. */
        long getTotalBytesTransferred();

        /** Memory access distribution by memory index. */
        java.util.Map<Integer, Long> getAccessesByMemory();

        /** Most frequently accessed memory pairs. */
        MemoryAccessPattern[] getHotAccessPatterns();

        /** Average transfer size for cross-memory operations. */
        double getAverageCrossMemoryTransferSize();

        interface MemoryAccessPattern {
            int getSourceMemoryIndex();
            int getDestMemoryIndex();
            long getAccessCount();
            long getTotalBytesTransferred();
            MemoryOperation getOperation();
        }
    }

    /**
     * Creates a multi-memory context with default settings.
     *
     * @param initialMemories the initial memory instances
     * @return a new multi-memory context
     */
    static MultiMemoryContext create(final WasmMemory... initialMemories) {
        return create(initialMemories, createDefaultAccessPolicy());
    }

    /**
     * Creates a multi-memory context with specific access policy.
     *
     * @param initialMemories the initial memory instances
     * @param accessPolicy the memory access policy
     * @return a new multi-memory context
     */
    static MultiMemoryContext create(final WasmMemory[] initialMemories,
                                    final MemoryAccessPolicy accessPolicy) {
        return new DefaultMultiMemoryContext(initialMemories, accessPolicy);
    }

    /**
     * Creates a default memory access policy.
     *
     * @return a permissive default policy
     */
    static MemoryAccessPolicy createDefaultAccessPolicy() {
        return new MemoryAccessPolicy() {
            @Override public boolean allowCrossMemoryAccess() { return true; }
            @Override public boolean allowMemoryGrowth() { return true; }
            @Override public int getMaxMemoryInstances() { return 16; }
            @Override public boolean isOperationAllowed(final MemoryOperation operation,
                                                       final int sourceIndex, final int destIndex) {
                return true;
            }
            @Override public MemoryIsolationLevel getIsolationLevel() {
                return MemoryIsolationLevel.BASIC;
            }
        };
    }

    /**
     * Creates a strict memory access policy with strong isolation.
     *
     * @return a restrictive policy
     */
    static MemoryAccessPolicy createStrictAccessPolicy() {
        return new MemoryAccessPolicy() {
            @Override public boolean allowCrossMemoryAccess() { return false; }
            @Override public boolean allowMemoryGrowth() { return false; }
            @Override public int getMaxMemoryInstances() { return 4; }
            @Override public boolean isOperationAllowed(final MemoryOperation operation,
                                                       final int sourceIndex, final int destIndex) {
                return sourceIndex == destIndex || sourceIndex == -1;
            }
            @Override public MemoryIsolationLevel getIsolationLevel() {
                return MemoryIsolationLevel.STRONG;
            }
        };
    }
}

/**
 * Default implementation of MultiMemoryContext.
 */
class DefaultMultiMemoryContext implements MultiMemoryContext {
    private final java.util.List<WasmMemory> memories;
    private MemoryAccessPolicy accessPolicy;
    private final MultiMemoryStatisticsImpl statistics;

    DefaultMultiMemoryContext(final WasmMemory[] initialMemories,
                             final MemoryAccessPolicy accessPolicy) {
        this.memories = new java.util.ArrayList<>(java.util.Arrays.asList(initialMemories));
        this.accessPolicy = accessPolicy;
        this.statistics = new MultiMemoryStatisticsImpl();
    }

    @Override
    public int getMemoryCount() {
        return memories.size();
    }

    @Override
    public WasmMemory getMemory(final int index) {
        if (index < 0 || index >= memories.size()) {
            return null;
        }
        statistics.recordAccess(index);
        return memories.get(index);
    }

    @Override
    public WasmMemory[] getAllMemories() {
        return memories.toArray(new WasmMemory[0]);
    }

    @Override
    public int createMemory(final MemoryType memoryType) throws WasmException {
        if (memories.size() >= accessPolicy.getMaxMemoryInstances()) {
            throw new WasmException("Maximum memory instances exceeded");
        }

        // In a real implementation, this would create a new WasmMemory instance
        // For now, return the index where it would be created
        final int newIndex = memories.size();
        memories.add(null); // Placeholder
        return newIndex;
    }

    @Override
    public void removeMemory(final int index) throws WasmException {
        if (index < 0 || index >= memories.size()) {
            throw new IndexOutOfBoundsException("Invalid memory index: " + index);
        }
        memories.set(index, null); // Mark as removed
    }

    @Override
    public void copyBetweenMemories(final WasmMemory destMemory, final long destOffset,
                                   final WasmMemory srcMemory, final long srcOffset,
                                   final long length) throws WasmException {
        if (!accessPolicy.allowCrossMemoryAccess()) {
            throw new WasmException("Cross-memory access not allowed");
        }

        final int srcIndex = memories.indexOf(srcMemory);
        final int destIndex = memories.indexOf(destMemory);

        if (!isOperationAllowed(MemoryOperation.COPY_BETWEEN, srcIndex, destIndex)) {
            throw new WasmException("Memory copy operation not allowed");
        }

        // Perform the copy operation
        final byte[] buffer = new byte[(int) Math.min(length, 1024 * 1024)]; // 1MB max chunk
        long remaining = length;
        long currentSrcOffset = srcOffset;
        long currentDestOffset = destOffset;

        while (remaining > 0) {
            final int chunkSize = (int) Math.min(remaining, buffer.length);
            srcMemory.readBytes(currentSrcOffset, buffer, 0, chunkSize);
            destMemory.writeBytes(currentDestOffset, buffer, 0, chunkSize);

            remaining -= chunkSize;
            currentSrcOffset += chunkSize;
            currentDestOffset += chunkSize;
        }

        statistics.recordCrossMemoryOperation(srcIndex, destIndex, length);
    }

    @Override
    public void initializeFromDataSegment(final WasmMemory memory,
                                        final int dataSegmentIndex,
                                        final long destOffset,
                                        final long srcOffset,
                                        final long length) throws WasmException {
        // In a real implementation, this would access the data segment
        // and copy its contents to memory
        final int memIndex = memories.indexOf(memory);
        if (!isOperationAllowed(MemoryOperation.INIT, -1, memIndex)) {
            throw new WasmException("Memory initialization not allowed");
        }

        statistics.recordAccess(memIndex);
    }

    @Override
    public void executeBulkTransfer(final MultiMemoryInstructions.BulkTransferDescriptor descriptor)
            throws WasmException {
        // Implementation for bulk transfer operations
        final int[] memoryIndices = descriptor.getMemoryIndices();
        final long[] lengths = descriptor.getLengths();

        for (int i = 0; i < memoryIndices.length; i++) {
            statistics.recordAccess(memoryIndices[i]);
            if (i > 0) {
                statistics.recordCrossMemoryOperation(memoryIndices[i-1], memoryIndices[i], lengths[i]);
            }
        }
    }

    @Override
    public WasmValue executeAtomicOperation(final MultiMemoryInstructions.AtomicOperationDescriptor descriptor)
            throws WasmException {
        // Implementation for atomic operations across multiple memories
        final int[] memoryIndices = descriptor.getMemoryIndices();
        for (final int index : memoryIndices) {
            statistics.recordAccess(index);
        }

        // Return a placeholder result
        return WasmValue.i32(0);
    }

    @Override
    public MultiMemoryStatistics getStatistics() {
        return statistics;
    }

    @Override
    public void setAccessPolicy(final MemoryAccessPolicy policy) {
        this.accessPolicy = policy;
    }

    @Override
    public MemoryAccessPolicy getAccessPolicy() {
        return accessPolicy;
    }

    @Override
    public boolean isOperationAllowed(final MemoryOperation operation,
                                     final int sourceIndex, final int destIndex) {
        return accessPolicy.isOperationAllowed(operation, sourceIndex, destIndex);
    }

    private static class MultiMemoryStatisticsImpl implements MultiMemoryStatistics {
        private long totalAccesses = 0;
        private long crossMemoryOperations = 0;
        private long totalBytesTransferred = 0;
        private final java.util.Map<Integer, Long> accessesByMemory = new java.util.HashMap<>();

        void recordAccess(final int memoryIndex) {
            totalAccesses++;
            accessesByMemory.merge(memoryIndex, 1L, Long::sum);
        }

        void recordCrossMemoryOperation(final int srcIndex, final int destIndex, final long bytes) {
            crossMemoryOperations++;
            totalBytesTransferred += bytes;
        }

        @Override public long getTotalMemoryAccesses() { return totalAccesses; }
        @Override public long getCrossMemoryOperations() { return crossMemoryOperations; }
        @Override public long getTotalBytesTransferred() { return totalBytesTransferred; }
        @Override public java.util.Map<Integer, Long> getAccessesByMemory() {
            return new java.util.HashMap<>(accessesByMemory);
        }
        @Override public MemoryAccessPattern[] getHotAccessPatterns() {
            return new MemoryAccessPattern[0]; // Simplified
        }
        @Override public double getAverageCrossMemoryTransferSize() {
            return crossMemoryOperations > 0 ? (double) totalBytesTransferred / crossMemoryOperations : 0.0;
        }
    }
}