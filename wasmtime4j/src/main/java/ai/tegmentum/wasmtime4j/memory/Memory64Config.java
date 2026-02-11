package ai.tegmentum.wasmtime4j.memory;

import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for 64-bit WebAssembly memory instances.
 *
 * <p>This class provides comprehensive configuration options for creating WebAssembly memory
 * instances that support 64-bit addressing, enabling memories larger than 4GB.
 *
 * <p>Key configuration aspects:
 *
 * <ul>
 *   <li>Memory size limits (minimum and maximum pages)
 *   <li>Addressing mode (32-bit vs 64-bit)
 *   <li>Shared memory support
 *   <li>Memory growth policies
 *   <li>Performance optimization settings
 * </ul>
 *
 * @since 1.1.0
 */
public final class Memory64Config {

  private final long minimumPages;
  private final Long maximumPages;
  private final boolean isShared;
  private final MemoryAddressingMode addressingMode;
  private final boolean allowAutoGrowth;
  private final double growthFactor;
  private final long growthLimitPages;
  private final String debugName;

  private Memory64Config(final Builder builder) {
    this.minimumPages = builder.minimumPages;
    this.maximumPages = builder.maximumPages;
    this.isShared = builder.isShared;
    this.addressingMode = builder.addressingMode;
    this.allowAutoGrowth = builder.allowAutoGrowth;
    this.growthFactor = builder.growthFactor;
    this.growthLimitPages = builder.growthLimitPages;
    this.debugName = builder.debugName;

    validate();
  }

  /**
   * Gets the minimum number of memory pages required.
   *
   * @return the minimum page count (always >= 0)
   */
  public long getMinimumPages() {
    return minimumPages;
  }

  /**
   * Gets the maximum number of memory pages allowed.
   *
   * @return the maximum page count, or empty for unlimited memory
   */
  public Optional<Long> getMaximumPages() {
    return Optional.ofNullable(maximumPages);
  }

  /**
   * Checks if this memory is shared between threads.
   *
   * @return true if shared memory, false for thread-local memory
   */
  public boolean isShared() {
    return isShared;
  }

  /**
   * Gets the memory addressing mode.
   *
   * @return the addressing mode (32-bit or 64-bit)
   */
  public MemoryAddressingMode getAddressingMode() {
    return addressingMode;
  }

  /**
   * Checks if automatic memory growth is enabled.
   *
   * @return true if auto-growth is enabled, false otherwise
   */
  public boolean isAutoGrowthAllowed() {
    return allowAutoGrowth;
  }

  /**
   * Gets the growth factor for automatic memory expansion.
   *
   * <p>When memory needs to grow automatically, the new size is calculated as: {@code newSize =
   * currentSize * growthFactor}
   *
   * @return the growth factor (typically between 1.5 and 2.0)
   */
  public double getGrowthFactor() {
    return growthFactor;
  }

  /**
   * Gets the maximum number of pages that can be allocated through automatic growth.
   *
   * <p>This limit prevents runaway memory allocation during automatic growth, independent of the
   * overall maximum pages limit.
   *
   * @return the growth limit in pages
   */
  public long getGrowthLimitPages() {
    return growthLimitPages;
  }

  /**
   * Gets the debug name for this memory configuration.
   *
   * @return the debug name, or empty if not specified
   */
  public Optional<String> getDebugName() {
    return Optional.ofNullable(debugName);
  }

  /**
   * Checks if this configuration represents 64-bit addressing.
   *
   * @return true if 64-bit addressing, false for 32-bit
   */
  public boolean is64BitAddressing() {
    return addressingMode.is64Bit();
  }

  /**
   * Gets the minimum memory size in bytes.
   *
   * @return the minimum size in bytes (minimumPages × 64KB)
   */
  public long getMinimumSizeBytes() {
    return minimumPages * 65536L;
  }

  /**
   * Gets the maximum memory size in bytes.
   *
   * @return the maximum size in bytes, or empty for unlimited
   */
  public Optional<Long> getMaximumSizeBytes() {
    return getMaximumPages().map(pages -> pages * 65536L);
  }

  /**
   * Validates if the specified page count is within this configuration's limits.
   *
   * @param pageCount the number of pages to check
   * @return true if within limits, false otherwise
   */
  public boolean isWithinLimits(final long pageCount) {
    if (pageCount < minimumPages) {
      return false;
    }
    return getMaximumPages().map(maxPages -> pageCount <= maxPages).orElse(true);
  }

  /**
   * Validates if the specified memory size in bytes is within this configuration's limits.
   *
   * @param sizeBytes the memory size in bytes to check
   * @return true if within limits, false otherwise
   */
  public boolean isWithinSizeLimits(final long sizeBytes) {
    if (sizeBytes % 65536L != 0) {
      return false; // Must be page-aligned
    }
    final long pageCount = sizeBytes / 65536L;
    return isWithinLimits(pageCount);
  }

  /**
   * Calculates the next growth size based on the current memory size.
   *
   * @param currentPages the current number of pages
   * @return the suggested number of pages after growth, or empty if growth is not allowed
   */
  public Optional<Long> calculateGrowthSize(final long currentPages) {
    if (!allowAutoGrowth) {
      return Optional.empty();
    }

    final long newSize = (long) Math.ceil(currentPages * growthFactor);

    // Determine the maximum allowed size (either growthLimit or maximumPages, whichever is
    // relevant)
    long targetSize;
    if (getMaximumPages().isPresent()) {
      // If we have a maximum, use the minimum of calculated size, growth limit, and maximum
      targetSize = Math.min(Math.min(newSize, growthLimitPages), getMaximumPages().get());

      // Special case: if current size already exceeds growth limit but is below maximum,
      // allow growth up to maximum
      if (currentPages >= growthLimitPages && currentPages < getMaximumPages().get()) {
        targetSize = getMaximumPages().get();
      }
    } else {
      // No maximum, just apply growth limit
      targetSize = Math.min(newSize, growthLimitPages);
    }

    return targetSize > currentPages ? Optional.of(targetSize) : Optional.empty();
  }

  /**
   * Creates a new builder for Memory64Config.
   *
   * @param minimumPages the minimum number of pages required
   * @return a new builder instance
   */
  public static Builder builder(final long minimumPages) {
    return new Builder(minimumPages);
  }

  /**
   * Creates a default 64-bit memory configuration.
   *
   * @param initialPages the initial number of pages
   * @return a default 64-bit memory configuration
   */
  public static Memory64Config createDefault64Bit(final long initialPages) {
    return builder(initialPages).addressing64Bit().debugName("default-64bit-memory").build();
  }

  /**
   * Creates a default 32-bit memory configuration for compatibility.
   *
   * @param initialPages the initial number of pages
   * @return a default 32-bit memory configuration
   */
  public static Memory64Config createDefault32Bit(final long initialPages) {
    return builder(initialPages)
        .addressing32Bit()
        .maximumPages(65536L) // 4GB limit for 32-bit
        .debugName("default-32bit-memory")
        .build();
  }

  /**
   * Creates an unlimited 64-bit memory configuration.
   *
   * @param initialPages the initial number of pages
   * @return an unlimited 64-bit memory configuration
   */
  public static Memory64Config createUnlimited64Bit(final long initialPages) {
    return builder(initialPages)
        .addressing64Bit()
        .unlimitedGrowth()
        .autoGrowth(true, 2.0)
        .debugName("unlimited-64bit-memory")
        .build();
  }

  private void validate() {
    if (minimumPages < 0) {
      throw new IllegalArgumentException("Minimum pages cannot be negative: " + minimumPages);
    }

    if (maximumPages != null && maximumPages < minimumPages) {
      throw new IllegalArgumentException(
          String.format(
              "Maximum pages (%d) cannot be less than minimum pages (%d)",
              maximumPages, minimumPages));
    }

    if (!addressingMode.supportsPageCount(minimumPages)) {
      throw new IllegalArgumentException(
          String.format(
              "Minimum pages %d exceeds %s addressing limit",
              minimumPages, addressingMode.getDisplayName()));
    }

    if (maximumPages != null && !addressingMode.supportsPageCount(maximumPages)) {
      throw new IllegalArgumentException(
          String.format(
              "Maximum pages %d exceeds %s addressing limit",
              maximumPages, addressingMode.getDisplayName()));
    }

    if (growthFactor <= 1.0) {
      throw new IllegalArgumentException("Growth factor must be greater than 1.0: " + growthFactor);
    }

    if (growthLimitPages < minimumPages) {
      throw new IllegalArgumentException(
          String.format(
              "Growth limit (%d) cannot be less than minimum pages (%d)",
              growthLimitPages, minimumPages));
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final Memory64Config that = (Memory64Config) obj;
    return minimumPages == that.minimumPages
        && isShared == that.isShared
        && allowAutoGrowth == that.allowAutoGrowth
        && Double.compare(that.growthFactor, growthFactor) == 0
        && growthLimitPages == that.growthLimitPages
        && Objects.equals(maximumPages, that.maximumPages)
        && addressingMode == that.addressingMode
        && Objects.equals(debugName, that.debugName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        minimumPages,
        maximumPages,
        isShared,
        addressingMode,
        allowAutoGrowth,
        growthFactor,
        growthLimitPages,
        debugName);
  }

  @Override
  public String toString() {
    return String.format(
        "Memory64Config{min=%d pages, max=%s pages, %s, shared=%b, autoGrowth=%b, name='%s'}",
        minimumPages,
        maximumPages != null ? maximumPages.toString() : "unlimited",
        addressingMode.getDisplayName(),
        isShared,
        allowAutoGrowth,
        debugName != null ? debugName : "unnamed");
  }

  /** Builder for creating Memory64Config instances. */
  public static final class Builder {
    private final long minimumPages;
    private Long maximumPages;
    private boolean isShared = false;
    private MemoryAddressingMode addressingMode =
        MemoryAddressingMode.MEMORY32; // Default to 32-bit for compatibility
    private boolean allowAutoGrowth = false;
    private double growthFactor = 1.5;
    private long growthLimitPages = Long.MAX_VALUE;
    private String debugName;

    private Builder(final long minimumPages) {
      if (minimumPages < 0) {
        throw new IllegalArgumentException("Minimum pages cannot be negative: " + minimumPages);
      }
      this.minimumPages = minimumPages;
      this.growthLimitPages = Math.max(minimumPages * 10, 1024); // Default growth limit
    }

    /**
     * Sets the maximum number of pages allowed.
     *
     * @param maximumPages the maximum page count
     * @return this builder for method chaining
     */
    public Builder maximumPages(final long maximumPages) {
      if (maximumPages < minimumPages) {
        throw new IllegalArgumentException(
            String.format(
                "Maximum pages (%d) cannot be less than minimum pages (%d)",
                maximumPages, minimumPages));
      }
      this.maximumPages = maximumPages;
      return this;
    }

    /**
     * Enables unlimited memory growth (no maximum limit).
     *
     * @return this builder for method chaining
     */
    public Builder unlimitedGrowth() {
      this.maximumPages = null;
      return this;
    }

    /**
     * Enables shared memory between threads.
     *
     * @return this builder for method chaining
     */
    public Builder shared() {
      this.isShared = true;
      return this;
    }

    /**
     * Sets the memory addressing mode to 32-bit.
     *
     * @return this builder for method chaining
     */
    public Builder addressing32Bit() {
      this.addressingMode = MemoryAddressingMode.MEMORY32;
      return this;
    }

    /**
     * Sets the memory addressing mode to 64-bit.
     *
     * @return this builder for method chaining
     */
    public Builder addressing64Bit() {
      this.addressingMode = MemoryAddressingMode.MEMORY64;
      return this;
    }

    /**
     * Configures automatic memory growth.
     *
     * @param enabled whether auto-growth is enabled
     * @param growthFactor the factor by which to grow memory (must be > 1.0)
     * @return this builder for method chaining
     */
    public Builder autoGrowth(final boolean enabled, final double growthFactor) {
      if (enabled && growthFactor <= 1.0) {
        throw new IllegalArgumentException(
            "Growth factor must be greater than 1.0: " + growthFactor);
      }
      this.allowAutoGrowth = enabled;
      this.growthFactor = growthFactor;
      return this;
    }

    /**
     * Sets the growth limit for automatic memory expansion.
     *
     * @param growthLimitPages the maximum pages that can be allocated through auto-growth
     * @return this builder for method chaining
     */
    public Builder growthLimit(final long growthLimitPages) {
      if (growthLimitPages < minimumPages) {
        throw new IllegalArgumentException(
            String.format(
                "Growth limit (%d) cannot be less than minimum pages (%d)",
                growthLimitPages, minimumPages));
      }
      this.growthLimitPages = growthLimitPages;
      return this;
    }

    /**
     * Sets a debug name for this memory configuration.
     *
     * @param debugName the debug name for logging and debugging
     * @return this builder for method chaining
     */
    public Builder debugName(final String debugName) {
      this.debugName = debugName;
      return this;
    }

    /**
     * Builds the Memory64Config instance.
     *
     * @return a new Memory64Config instance
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public Memory64Config build() {
      return new Memory64Config(this);
    }
  }
}
