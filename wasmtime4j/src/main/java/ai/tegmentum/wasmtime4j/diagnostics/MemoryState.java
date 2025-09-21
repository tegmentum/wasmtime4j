package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.Map;
import java.util.Optional;

/**
 * Memory state information for WebAssembly diagnostics.
 *
 * <p>This interface provides detailed information about memory usage and state
 * at the time an error occurred during WebAssembly execution.
 *
 * @since 1.0.0
 */
public interface MemoryState {

    /**
     * Gets the total heap memory in bytes.
     *
     * @return the total heap memory, or empty if not available
     */
    Optional<Long> getTotalHeapMemory();

    /**
     * Gets the used heap memory in bytes.
     *
     * @return the used heap memory, or empty if not available
     */
    Optional<Long> getUsedHeapMemory();

    /**
     * Gets the free heap memory in bytes.
     *
     * @return the free heap memory, or empty if not available
     */
    Optional<Long> getFreeHeapMemory();

    /**
     * Gets the maximum heap memory in bytes.
     *
     * @return the maximum heap memory, or empty if not available
     */
    Optional<Long> getMaxHeapMemory();

    /**
     * Gets the WebAssembly linear memory size in bytes.
     *
     * @return the linear memory size, or empty if not available
     */
    Optional<Long> getLinearMemorySize();

    /**
     * Gets the WebAssembly linear memory usage in bytes.
     *
     * @return the linear memory usage, or empty if not available
     */
    Optional<Long> getLinearMemoryUsage();

    /**
     * Gets the number of WebAssembly memory instances.
     *
     * @return the memory instance count, or empty if not available
     */
    Optional<Integer> getMemoryInstanceCount();

    /**
     * Gets additional memory-related properties.
     *
     * @return map of memory properties
     */
    Map<String, Object> getProperties();

    /**
     * Gets the memory state collection timestamp.
     *
     * @return the collection timestamp
     */
    long getCollectionTimestamp();

    /**
     * Creates a MemoryState snapshot of current memory usage.
     *
     * @return the current memory state
     */
    static MemoryState snapshot() {
        final Runtime runtime = Runtime.getRuntime();
        return new MemoryStateImpl(
            runtime.totalMemory(),
            runtime.totalMemory() - runtime.freeMemory(),
            runtime.freeMemory(),
            runtime.maxMemory(),
            System.currentTimeMillis()
        );
    }

    /**
     * Simple implementation of MemoryState.
     */
    final class MemoryStateImpl implements MemoryState {
        private final long totalHeapMemory;
        private final long usedHeapMemory;
        private final long freeHeapMemory;
        private final long maxHeapMemory;
        private final long collectionTimestamp;

        public MemoryStateImpl(final long totalHeapMemory, final long usedHeapMemory,
                              final long freeHeapMemory, final long maxHeapMemory,
                              final long collectionTimestamp) {
            this.totalHeapMemory = totalHeapMemory;
            this.usedHeapMemory = usedHeapMemory;
            this.freeHeapMemory = freeHeapMemory;
            this.maxHeapMemory = maxHeapMemory;
            this.collectionTimestamp = collectionTimestamp;
        }

        @Override
        public Optional<Long> getTotalHeapMemory() {
            return Optional.of(totalHeapMemory);
        }

        @Override
        public Optional<Long> getUsedHeapMemory() {
            return Optional.of(usedHeapMemory);
        }

        @Override
        public Optional<Long> getFreeHeapMemory() {
            return Optional.of(freeHeapMemory);
        }

        @Override
        public Optional<Long> getMaxHeapMemory() {
            return Optional.of(maxHeapMemory);
        }

        @Override
        public Optional<Long> getLinearMemorySize() {
            return Optional.empty();
        }

        @Override
        public Optional<Long> getLinearMemoryUsage() {
            return Optional.empty();
        }

        @Override
        public Optional<Integer> getMemoryInstanceCount() {
            return Optional.empty();
        }

        @Override
        public Map<String, Object> getProperties() {
            return Map.of(
                "totalHeapMemory", totalHeapMemory,
                "usedHeapMemory", usedHeapMemory,
                "freeHeapMemory", freeHeapMemory,
                "maxHeapMemory", maxHeapMemory,
                "collectionTimestamp", collectionTimestamp
            );
        }

        @Override
        public long getCollectionTimestamp() {
            return collectionTimestamp;
        }
    }
}