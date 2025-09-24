package ai.tegmentum.wasmtime4j;

/**
 * Represents the addressing mode for WebAssembly memory.
 *
 * <p>WebAssembly supports two addressing modes:
 *
 * <ul>
 *   <li><b>32-bit addressing:</b> Standard WebAssembly memory with 4GB limit (65536 pages × 64KB)
 *   <li><b>64-bit addressing:</b> Extended memory proposal supporting memories larger than 4GB
 * </ul>
 *
 * <p>The addressing mode affects:
 *
 * <ul>
 *   <li>Maximum memory size
 *   <li>Address calculation methods
 *   <li>Memory instruction behavior
 *   <li>Performance characteristics
 * </ul>
 *
 * @since 1.1.0
 */
public enum MemoryAddressingMode {

  /**
   * 32-bit memory addressing (standard WebAssembly).
   *
   * <p>This is the traditional WebAssembly memory model with a 4GB limit. Memory addresses and page
   * counts are represented as 32-bit integers.
   *
   * <p>Characteristics:
   *
   * <ul>
   *   <li>Maximum memory size: 4GB (65536 pages × 64KB)
   *   <li>Address range: 0 to 4,294,967,295 bytes
   *   <li>Page count range: 0 to 65535 pages
   *   <li>Compatible with all WebAssembly implementations
   *   <li>Lower memory overhead for address calculations
   * </ul>
   */
  MEMORY32("32-bit", 4_294_967_296L, 65536L, Integer.class, Integer.class),

  /**
   * 64-bit memory addressing (memory64 proposal).
   *
   * <p>This is the extended WebAssembly memory model supporting memories larger than 4GB. Memory
   * addresses and page counts are represented as 64-bit integers.
   *
   * <p>Characteristics:
   *
   * <ul>
   *   <li>Maximum memory size: 2^63 bytes (theoretical limit)
   *   <li>Address range: 0 to 9,223,372,036,854,775,807 bytes
   *   <li>Page count range: 0 to 140,737,488,355,327 pages
   *   <li>Requires memory64 proposal support in WebAssembly runtime
   *   <li>Higher memory overhead for address calculations
   * </ul>
   */
  MEMORY64("64-bit", Long.MAX_VALUE, Long.MAX_VALUE / 65536L, Long.class, Long.class);

  private final String displayName;
  private final long maxMemorySize;
  private final long maxPageCount;
  private final Class<?> addressType;
  private final Class<?> pageCountType;

  /**
   * Creates a new memory addressing mode.
   *
   * @param displayName human-readable name for this mode
   * @param maxMemorySize maximum memory size in bytes
   * @param maxPageCount maximum number of pages
   * @param addressType Java type used for memory addresses
   * @param pageCountType Java type used for page counts
   */
  MemoryAddressingMode(
      final String displayName,
      final long maxMemorySize,
      final long maxPageCount,
      final Class<?> addressType,
      final Class<?> pageCountType) {
    this.displayName = displayName;
    this.maxMemorySize = maxMemorySize;
    this.maxPageCount = maxPageCount;
    this.addressType = addressType;
    this.pageCountType = pageCountType;
  }

  /**
   * Gets the human-readable display name of this addressing mode.
   *
   * @return the display name (e.g., "32-bit", "64-bit")
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Gets the maximum memory size supported by this addressing mode.
   *
   * @return the maximum memory size in bytes
   */
  public long getMaxMemorySize() {
    return maxMemorySize;
  }

  /**
   * Gets the maximum page count supported by this addressing mode.
   *
   * @return the maximum number of pages
   */
  public long getMaxPageCount() {
    return maxPageCount;
  }

  /**
   * Gets the Java type used to represent memory addresses in this mode.
   *
   * @return the address type (Integer.class for 32-bit, Long.class for 64-bit)
   */
  public Class<?> getAddressType() {
    return addressType;
  }

  /**
   * Gets the Java type used to represent page counts in this mode.
   *
   * @return the page count type (Integer.class for 32-bit, Long.class for 64-bit)
   */
  public Class<?> getPageCountType() {
    return pageCountType;
  }

  /**
   * Checks if this addressing mode is 64-bit.
   *
   * @return true if this is 64-bit addressing, false for 32-bit
   */
  public boolean is64Bit() {
    return this == MEMORY64;
  }

  /**
   * Checks if the specified memory size is supported by this addressing mode.
   *
   * @param sizeInBytes the memory size in bytes to check
   * @return true if the size is within limits, false otherwise
   */
  public boolean supportsMemorySize(final long sizeInBytes) {
    return sizeInBytes >= 0 && sizeInBytes <= maxMemorySize;
  }

  /**
   * Checks if the specified page count is supported by this addressing mode.
   *
   * @param pageCount the number of pages to check
   * @return true if the page count is within limits, false otherwise
   */
  public boolean supportsPageCount(final long pageCount) {
    return pageCount >= 0 && pageCount <= maxPageCount;
  }

  /**
   * Converts a page count to memory size in bytes.
   *
   * @param pageCount the number of pages
   * @return the memory size in bytes (pageCount × 64KB)
   * @throws IllegalArgumentException if the page count is invalid for this addressing mode
   */
  public long pagesToBytes(final long pageCount) {
    if (!supportsPageCount(pageCount)) {
      throw new IllegalArgumentException(
          String.format(
              "Page count %d exceeds %s addressing limit %d",
              pageCount, displayName, maxPageCount));
    }

    final long sizeInBytes = pageCount * 65536L; // 64KB per page
    if (!supportsMemorySize(sizeInBytes)) {
      throw new IllegalArgumentException(
          String.format(
              "Memory size %d bytes exceeds %s addressing limit %d bytes",
              sizeInBytes, displayName, maxMemorySize));
    }

    return sizeInBytes;
  }

  /**
   * Converts a memory size in bytes to page count.
   *
   * @param sizeInBytes the memory size in bytes
   * @return the number of pages (sizeInBytes ÷ 64KB)
   * @throws IllegalArgumentException if the size is not page-aligned or invalid for this addressing
   *     mode
   */
  public long bytesToPages(final long sizeInBytes) {
    if (sizeInBytes % 65536L != 0) {
      throw new IllegalArgumentException(
          String.format(
              "Memory size %d bytes is not aligned to page boundaries (64KB)", sizeInBytes));
    }

    if (!supportsMemorySize(sizeInBytes)) {
      throw new IllegalArgumentException(
          String.format(
              "Memory size %d bytes exceeds %s addressing limit %d bytes",
              sizeInBytes, displayName, maxMemorySize));
    }

    return sizeInBytes / 65536L;
  }

  /**
   * Gets the optimal addressing mode for the specified memory requirements.
   *
   * @param requiredPages the minimum number of pages required
   * @param maxPages the maximum number of pages (null for unlimited)
   * @return the most suitable addressing mode
   */
  public static MemoryAddressingMode getOptimalMode(final long requiredPages, final Long maxPages) {
    final long targetPages = maxPages != null ? maxPages : requiredPages;

    if (MEMORY32.supportsPageCount(targetPages)) {
      return MEMORY32; // Prefer 32-bit for compatibility and performance
    } else {
      return MEMORY64; // Requires 64-bit addressing
    }
  }

  /**
   * Gets the optimal addressing mode for the specified memory size in bytes.
   *
   * @param requiredBytes the minimum memory size in bytes
   * @param maxBytes the maximum memory size in bytes (null for unlimited)
   * @return the most suitable addressing mode
   */
  public static MemoryAddressingMode getOptimalModeForSize(
      final long requiredBytes, final Long maxBytes) {
    final long targetBytes = maxBytes != null ? maxBytes : requiredBytes;

    if (MEMORY32.supportsMemorySize(targetBytes)) {
      return MEMORY32; // Prefer 32-bit for compatibility and performance
    } else {
      return MEMORY64; // Requires 64-bit addressing
    }
  }

  /**
   * Detects the addressing mode required by a memory type.
   *
   * @param memoryType the memory type to analyze
   * @return the required addressing mode
   */
  public static MemoryAddressingMode detectMode(final MemoryType memoryType) {
    if (memoryType.is64Bit()) {
      return MEMORY64;
    } else {
      return MEMORY32;
    }
  }

  @Override
  public String toString() {
    return String.format(
        "MemoryAddressingMode{%s, maxSize=%d bytes, maxPages=%d}",
        displayName, maxMemorySize, maxPageCount);
  }
}
