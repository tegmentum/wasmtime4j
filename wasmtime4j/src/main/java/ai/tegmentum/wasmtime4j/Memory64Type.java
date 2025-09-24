package ai.tegmentum.wasmtime4j;

/**
 * Represents the type information of a WebAssembly 64-bit memory.
 *
 * <p>This interface extends the standard MemoryType to provide 64-bit addressing capabilities,
 * enabling WebAssembly modules to use memory larger than 4GB. 64-bit memories use 64-bit page
 * counts and addressing for all operations.
 *
 * @since 1.1.0
 */
public interface Memory64Type extends MemoryType {

  /**
   * Gets the minimum number of memory pages required using 64-bit addressing.
   *
   * @return the minimum page count (64-bit)
   */
  @Override
  default long getMinimum() {
    return getMinimum64();
  }

  /**
   * Gets the maximum number of memory pages allowed using 64-bit addressing.
   *
   * @return the maximum page count (64-bit), or empty if unlimited
   */
  @Override
  default java.util.Optional<Long> getMaximum() {
    return getMaximum64();
  }

  /**
   * Gets the minimum number of memory pages required using 64-bit addressing.
   *
   * <p>This method provides the native 64-bit page count without conversion from smaller integer
   * types.
   *
   * @return the minimum page count (64-bit)
   * @since 1.1.0
   */
  long getMinimum64();

  /**
   * Gets the maximum number of memory pages allowed using 64-bit addressing.
   *
   * <p>This method provides the native 64-bit page count without conversion from smaller integer
   * types, enabling memories larger than 4GB.
   *
   * @return the maximum page count (64-bit), or empty if unlimited
   * @since 1.1.0
   */
  java.util.Optional<Long> getMaximum64();

  /**
   * Always returns true since this is a 64-bit memory type.
   *
   * @return true (always 64-bit)
   */
  @Override
  default boolean is64Bit() {
    return true;
  }

  /**
   * Gets the memory size limit per page in bytes.
   *
   * <p>WebAssembly pages are always 64KB (65536 bytes) regardless of addressing mode.
   *
   * @return 65536 (64KB per page)
   */
  default int getPageSizeBytes() {
    return 65536; // 64KB per page
  }

  /**
   * Gets the theoretical maximum addressable memory size in bytes.
   *
   * <p>For 64-bit memory, this is determined by the maximum page count multiplied by the page size
   * (64KB).
   *
   * @return the maximum addressable memory size in bytes, or empty if unlimited
   */
  default java.util.Optional<Long> getMaximumSizeBytes() {
    return getMaximum64().map(pages -> pages * 65536L);
  }

  /**
   * Gets the minimum required memory size in bytes.
   *
   * @return the minimum memory size in bytes
   */
  default long getMinimumSizeBytes() {
    return getMinimum64() * 65536L;
  }

  /**
   * Checks if this memory type can accommodate the specified number of pages.
   *
   * @param pages the number of pages to check
   * @return true if the page count is within limits, false otherwise
   */
  default boolean canAccommodatePages(final long pages) {
    if (pages < getMinimum64()) {
      return false;
    }
    return getMaximum64().map(maxPages -> pages <= maxPages).orElse(true);
  }

  /**
   * Checks if this memory type can accommodate the specified memory size in bytes.
   *
   * @param sizeBytes the memory size in bytes to check
   * @return true if the size is within limits, false otherwise
   */
  default boolean canAccommodateSize(final long sizeBytes) {
    if (sizeBytes % 65536L != 0) {
      return false; // Must be aligned to page boundaries
    }
    final long pages = sizeBytes / 65536L;
    return canAccommodatePages(pages);
  }

  /**
   * Creates a new 64-bit memory type with the specified parameters.
   *
   * @param minimumPages the minimum number of pages
   * @param maximumPages the maximum number of pages (null for unlimited)
   * @param isShared whether the memory is shared between threads
   * @return a new Memory64Type instance
   */
  static Memory64Type create(
      final long minimumPages, final Long maximumPages, final boolean isShared) {
    return new Memory64Type() {
      @Override
      public long getMinimum64() {
        return minimumPages;
      }

      @Override
      public java.util.Optional<Long> getMaximum64() {
        return java.util.Optional.ofNullable(maximumPages);
      }

      @Override
      public boolean isShared() {
        return isShared;
      }

      @Override
      public String toString() {
        return String.format(
            "Memory64Type{min=%d, max=%s, shared=%b}",
            minimumPages, maximumPages != null ? maximumPages.toString() : "unlimited", isShared);
      }
    };
  }

  /**
   * Creates a new 64-bit memory type with the specified parameters (non-shared).
   *
   * @param minimumPages the minimum number of pages
   * @param maximumPages the maximum number of pages (null for unlimited)
   * @return a new Memory64Type instance (non-shared)
   */
  static Memory64Type create(final long minimumPages, final Long maximumPages) {
    return create(minimumPages, maximumPages, false);
  }

  /**
   * Creates a new unlimited 64-bit memory type.
   *
   * @param minimumPages the minimum number of pages
   * @param isShared whether the memory is shared between threads
   * @return a new unlimited Memory64Type instance
   */
  static Memory64Type createUnlimited(final long minimumPages, final boolean isShared) {
    return create(minimumPages, null, isShared);
  }

  /**
   * Creates a new unlimited 64-bit memory type (non-shared).
   *
   * @param minimumPages the minimum number of pages
   * @return a new unlimited Memory64Type instance (non-shared)
   */
  static Memory64Type createUnlimited(final long minimumPages) {
    return create(minimumPages, null, false);
  }
}
