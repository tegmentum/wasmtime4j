package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;

/**
 * Information about WebAssembly memory for debugging purposes.
 *
 * <p>Provides details about memory layout, size limits, and current usage
 * that can be used for memory inspection and debugging operations.
 *
 * @since 1.0.0
 */
public final class MemoryInfo {

    private final long baseAddress;
    private final long currentSize;
    private final long maxSize;
    private final int pageSize;
    private final boolean growable;
    private final boolean shared;
    private final long usedBytes;
    private final long freeBytes;
    private final int memoryIndex;
    private final String description;

    /**
     * Creates memory information.
     *
     * @param baseAddress base memory address
     * @param currentSize current memory size in bytes
     * @param maxSize maximum memory size in bytes
     * @param pageSize memory page size in bytes
     * @param growable whether memory can grow
     * @param shared whether memory is shared
     * @param usedBytes number of used bytes
     * @param freeBytes number of free bytes
     * @param memoryIndex memory index
     * @param description optional description
     */
    public MemoryInfo(final long baseAddress, final long currentSize, final long maxSize,
                     final int pageSize, final boolean growable, final boolean shared,
                     final long usedBytes, final long freeBytes, final int memoryIndex,
                     final String description) {
        this.baseAddress = baseAddress;
        this.currentSize = currentSize;
        this.maxSize = maxSize;
        this.pageSize = pageSize;
        this.growable = growable;
        this.shared = shared;
        this.usedBytes = usedBytes;
        this.freeBytes = freeBytes;
        this.memoryIndex = memoryIndex;
        this.description = description;
    }

    /**
     * Creates basic memory information.
     *
     * @param baseAddress base address
     * @param currentSize current size
     * @param maxSize maximum size
     * @return basic memory info
     */
    public static MemoryInfo basic(final long baseAddress, final long currentSize, final long maxSize) {
        return new MemoryInfo(baseAddress, currentSize, maxSize, 65536, true, false,
                currentSize, 0, 0, null);
    }

    /**
     * Gets the base memory address.
     *
     * @return base address
     */
    public long getBaseAddress() {
        return baseAddress;
    }

    /**
     * Gets the current memory size in bytes.
     *
     * @return current size
     */
    public long getCurrentSize() {
        return currentSize;
    }

    /**
     * Gets the maximum memory size in bytes.
     *
     * @return maximum size
     */
    public long getMaxSize() {
        return maxSize;
    }

    /**
     * Gets the memory page size in bytes.
     *
     * @return page size
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Checks if memory is growable.
     *
     * @return true if growable
     */
    public boolean isGrowable() {
        return growable;
    }

    /**
     * Checks if memory is shared.
     *
     * @return true if shared
     */
    public boolean isShared() {
        return shared;
    }

    /**
     * Gets the number of used bytes.
     *
     * @return used bytes
     */
    public long getUsedBytes() {
        return usedBytes;
    }

    /**
     * Gets the number of free bytes.
     *
     * @return free bytes
     */
    public long getFreeBytes() {
        return freeBytes;
    }

    /**
     * Gets the memory index.
     *
     * @return memory index
     */
    public int getMemoryIndex() {
        return memoryIndex;
    }

    /**
     * Gets the description.
     *
     * @return description or null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the current number of memory pages.
     *
     * @return number of pages
     */
    public long getCurrentPages() {
        return currentSize / pageSize;
    }

    /**
     * Gets the maximum number of memory pages.
     *
     * @return maximum pages
     */
    public long getMaxPages() {
        return maxSize / pageSize;
    }

    /**
     * Gets the memory utilization percentage.
     *
     * @return utilization percentage (0.0 to 1.0)
     */
    public double getUtilization() {
        if (maxSize == 0) {
            return 0.0;
        }
        return (double) currentSize / maxSize;
    }

    /**
     * Checks if address is within memory bounds.
     *
     * @param address the address to check
     * @return true if address is valid
     */
    public boolean isValidAddress(final long address) {
        return address >= baseAddress && address < baseAddress + currentSize;
    }

    /**
     * Checks if address range is within memory bounds.
     *
     * @param address start address
     * @param length number of bytes
     * @return true if range is valid
     */
    public boolean isValidRange(final long address, final int length) {
        return isValidAddress(address) &&
               address + length <= baseAddress + currentSize;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final MemoryInfo other = (MemoryInfo) obj;
        return baseAddress == other.baseAddress &&
                currentSize == other.currentSize &&
                maxSize == other.maxSize &&
                pageSize == other.pageSize &&
                growable == other.growable &&
                shared == other.shared &&
                usedBytes == other.usedBytes &&
                freeBytes == other.freeBytes &&
                memoryIndex == other.memoryIndex &&
                Objects.equals(description, other.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseAddress, currentSize, maxSize, pageSize, growable,
                shared, usedBytes, freeBytes, memoryIndex, description);
    }

    @Override
    public String toString() {
        return "MemoryInfo{" +
                "baseAddress=0x" + Long.toHexString(baseAddress) +
                ", currentSize=" + currentSize +
                ", maxSize=" + maxSize +
                ", pageSize=" + pageSize +
                ", pages=" + getCurrentPages() + "/" + getMaxPages() +
                ", growable=" + growable +
                ", shared=" + shared +
                ", utilization=" + String.format("%.1f%%", getUtilization() * 100) +
                ", memoryIndex=" + memoryIndex +
                (description != null ? ", description='" + description + '\'' : "") +
                '}';
    }
}