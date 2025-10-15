package ai.tegmentum.wasmtime4j.jni.memory;

import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Platform-specific memory management for WebAssembly runtimes with huge pages, NUMA awareness, and
 * advanced optimizations.
 *
 * <p>This class provides JNI bindings to the native platform memory allocator implemented in Rust,
 * offering comprehensive memory management features: - Huge pages support for Linux, macOS, and
 * Windows - NUMA-aware memory allocation and thread binding - Custom memory allocators with
 * platform-specific optimization - Memory prefetching and cache optimization strategies - Memory
 * compression and deduplication for WebAssembly heaps - Comprehensive memory usage monitoring and
 * leak detection - Memory pool management with size-based allocation strategies - Platform-specific
 * virtual memory management and protection
 */
public final class PlatformMemoryManager implements AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(PlatformMemoryManager.class.getName());

  // Native handle to the platform memory allocator
  private long nativeHandle;
  private boolean closed = false;

  /** Configuration for platform-specific memory management. */
  public static final class Config {
    public boolean enableHugePages = true;
    public int numaNode = -1; // -1 for automatic
    public long initialPoolSizeBytes = 64 * 1024 * 1024; // 64MB
    public long maxPoolSizeBytes = 2L * 1024 * 1024 * 1024; // 2GB
    public boolean enableCompression = true;
    public boolean enableDeduplication = true;
    public long prefetchBufferSizeBytes = 4 * 1024 * 1024; // 4MB
    public boolean enableLeakDetection = true;
    public int alignmentBytes = 64; // Cache line alignment
    public PageSize pageSize = PageSize.DEFAULT;

    /** Page size options for memory allocation. */
    public enum PageSize {
      DEFAULT(0),
      SMALL(1), // 4KB
      LARGE(2), // 2MB
      HUGE(3); // 1GB

      private final int value;

      PageSize(int value) {
        this.value = value;
      }

      public int getValue() {
        return value;
      }
    }
  }

  /** Memory allocation information. */
  @SuppressFBWarnings(
      value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
      justification = "Diagnostic fields for memory leak detection and debugging")
  public static final class AllocationInfo {
    public final long ptr;
    public final long size;
    public final int alignment;
    public final Config.PageSize pageType;
    public final int numaNode;
    public final long timestampMillis;
    public final String stackTrace;

    private AllocationInfo(
        long ptr,
        long size,
        int alignment,
        int pageType,
        int numaNode,
        long timestampMillis,
        String stackTrace) {
      this.ptr = ptr;
      this.size = size;
      this.alignment = alignment;
      this.pageType = Config.PageSize.values()[pageType];
      this.numaNode = numaNode;
      this.timestampMillis = timestampMillis;
      this.stackTrace = stackTrace;
    }
  }

  /** Platform memory information. */
  @SuppressFBWarnings(
      value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
      justification = "Data class for platform memory information, fields used for monitoring")
  public static final class PlatformInfo {
    public final long totalPhysicalMemory;
    public final long availableMemory;
    public final long pageSize;
    public final long hugePageSize;
    public final int numaNodes;
    public final int cpuCores;
    public final int cacheLineSize;
    public final boolean supportsHugePages;
    public final boolean supportsNuma;

    private PlatformInfo(
        long totalPhysicalMemory,
        long availableMemory,
        long pageSize,
        long hugePageSize,
        int numaNodes,
        int cpuCores,
        int cacheLineSize,
        boolean supportsHugePages,
        boolean supportsNuma) {
      this.totalPhysicalMemory = totalPhysicalMemory;
      this.availableMemory = availableMemory;
      this.pageSize = pageSize;
      this.hugePageSize = hugePageSize;
      this.numaNodes = numaNodes;
      this.cpuCores = cpuCores;
      this.cacheLineSize = cacheLineSize;
      this.supportsHugePages = supportsHugePages;
      this.supportsNuma = supportsNuma;
    }
  }

  /** Memory pool statistics. */
  @SuppressFBWarnings(
      value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
      justification =
          "Data class for memory statistics, fields used for monitoring and diagnostics")
  public static final class MemoryStats {
    public final long totalAllocated;
    public final long totalFreed;
    public final long currentUsage;
    public final long peakUsage;
    public final long allocationCount;
    public final long deallocationCount;
    public final double fragmentationRatio;
    public final double compressionRatio;
    public final long deduplicationSavings;
    public final long hugePagesUsed;
    public final double numaHitRate;

    private MemoryStats(
        long totalAllocated,
        long totalFreed,
        long currentUsage,
        long peakUsage,
        long allocationCount,
        long deallocationCount,
        double fragmentationRatio,
        double compressionRatio,
        long deduplicationSavings,
        long hugePagesUsed,
        double numaHitRate) {
      this.totalAllocated = totalAllocated;
      this.totalFreed = totalFreed;
      this.currentUsage = currentUsage;
      this.peakUsage = peakUsage;
      this.allocationCount = allocationCount;
      this.deallocationCount = deallocationCount;
      this.fragmentationRatio = fragmentationRatio;
      this.compressionRatio = compressionRatio;
      this.deduplicationSavings = deduplicationSavings;
      this.hugePagesUsed = hugePagesUsed;
      this.numaHitRate = numaHitRate;
    }
  }

  /** Memory leak information. */
  @SuppressFBWarnings(
      value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
      justification =
          "Data class for memory leak detection, fields used for diagnostics and reporting")
  public static final class MemoryLeak {
    public final AllocationInfo allocationInfo;
    public final long ageMillis;
    public final boolean isSuspectedLeak;
    public final double confidenceScore;

    private MemoryLeak(
        AllocationInfo allocationInfo,
        long ageMillis,
        boolean isSuspectedLeak,
        double confidenceScore) {
      this.allocationInfo = allocationInfo;
      this.ageMillis = ageMillis;
      this.isSuspectedLeak = isSuspectedLeak;
      this.confidenceScore = confidenceScore;
    }
  }

  /** Creates a new platform memory manager with default configuration. */
  public PlatformMemoryManager() {
    this(new Config());
  }

  /**
   * Creates a new platform memory manager with specific configuration.
   *
   * @param config Configuration for platform-specific memory management
   * @throws IllegalArgumentException if configuration is invalid
   * @throws RuntimeException if native allocator creation fails
   */
  public PlatformMemoryManager(Config config) {
    JniValidation.requireNonNull(config, "Config cannot be null");

    this.nativeHandle =
        nativeCreate(
            config.enableHugePages,
            config.numaNode,
            config.initialPoolSizeBytes,
            config.maxPoolSizeBytes,
            config.enableCompression,
            config.enableDeduplication,
            config.prefetchBufferSizeBytes,
            config.enableLeakDetection,
            config.alignmentBytes,
            config.pageSize.getValue());

    if (this.nativeHandle == 0) {
      throw new RuntimeException("Failed to create platform memory allocator");
    }

    LOGGER.log(Level.INFO, "Platform memory manager created with handle: {0}", this.nativeHandle);
  }

  /**
   * Allocates memory with platform-specific optimizations.
   *
   * @param size Size in bytes to allocate
   * @param alignment Memory alignment requirement (0 for default)
   * @return Native pointer to allocated memory
   * @throws IllegalArgumentException if size is 0 or negative
   * @throws RuntimeException if allocation fails
   */
  public long allocate(long size, int alignment) {
    ensureNotClosed();

    if (size <= 0) {
      throw new IllegalArgumentException("Size must be positive: " + size);
    }

    long ptr = nativeAllocate(nativeHandle, size, alignment);
    if (ptr == 0) {
      throw new RuntimeException("Failed to allocate " + size + " bytes");
    }

    LOGGER.log(
        Level.FINE, "Allocated {0} bytes at 0x{1}", new Object[] {size, Long.toHexString(ptr)});
    return ptr;
  }

  /**
   * Deallocates previously allocated memory.
   *
   * @param ptr Pointer to memory to deallocate
   * @throws IllegalArgumentException if ptr is 0
   * @throws RuntimeException if deallocation fails
   */
  public void deallocate(long ptr) {
    ensureNotClosed();

    if (ptr == 0) {
      throw new IllegalArgumentException("Pointer cannot be null");
    }

    if (!nativeDeallocate(nativeHandle, ptr)) {
      throw new RuntimeException("Failed to deallocate memory at 0x" + Long.toHexString(ptr));
    }

    LOGGER.log(Level.FINE, "Deallocated memory at 0x{0}", Long.toHexString(ptr));
  }

  /**
   * Gets current memory allocation statistics.
   *
   * @return Memory statistics
   */
  public MemoryStats getStats() {
    ensureNotClosed();
    return nativeGetStats(nativeHandle);
  }

  /**
   * Gets platform memory information.
   *
   * @return Platform memory information
   */
  public PlatformInfo getPlatformInfo() {
    ensureNotClosed();
    return nativeGetPlatformInfo(nativeHandle);
  }

  /**
   * Detects potential memory leaks.
   *
   * @return Array of detected memory leaks
   */
  public MemoryLeak[] detectLeaks() {
    ensureNotClosed();
    return nativeDetectLeaks(nativeHandle);
  }

  /**
   * Prefetches memory region for improved cache performance.
   *
   * @param ptr Pointer to memory region
   * @param size Size of memory region in bytes
   * @throws IllegalArgumentException if ptr is 0 or size is negative
   */
  public void prefetchMemory(long ptr, long size) {
    ensureNotClosed();

    if (ptr == 0) {
      throw new IllegalArgumentException("Pointer cannot be null");
    }
    if (size < 0) {
      throw new IllegalArgumentException("Size cannot be negative: " + size);
    }

    if (!nativePrefetch(nativeHandle, ptr, size)) {
      LOGGER.log(Level.WARNING, "Failed to prefetch memory at 0x{0}", Long.toHexString(ptr));
    }
  }

  /**
   * Compresses data using platform-specific compression.
   *
   * @param data Data to compress
   * @return Compressed data
   * @throws IllegalArgumentException if data is null or empty
   * @throws RuntimeException if compression fails
   */
  public byte[] compressMemory(byte[] data) {
    ensureNotClosed();

    if (data == null || data.length == 0) {
      throw new IllegalArgumentException("Data cannot be null or empty");
    }

    byte[] compressed = nativeCompress(nativeHandle, data);
    if (compressed == null) {
      throw new RuntimeException("Failed to compress data");
    }

    LOGGER.log(
        Level.FINE,
        "Compressed {0} bytes to {1} bytes (ratio: {2})",
        new Object[] {data.length, compressed.length, (double) compressed.length / data.length});
    return compressed;
  }

  /**
   * Performs memory deduplication on data.
   *
   * @param data Data to deduplicate
   * @return Pointer to deduplicated memory (may be shared)
   * @throws IllegalArgumentException if data is null or empty
   * @throws RuntimeException if deduplication fails
   */
  public long deduplicateMemory(byte[] data) {
    ensureNotClosed();

    if (data == null || data.length == 0) {
      throw new IllegalArgumentException("Data cannot be null or empty");
    }

    long ptr = nativeDeduplicate(nativeHandle, data);
    if (ptr == 0) {
      throw new RuntimeException("Failed to deduplicate data");
    }

    return ptr;
  }

  /**
   * Checks if the memory manager has been closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed;
  }

  /** Closes the platform memory manager and releases native resources. */
  @Override
  public void close() {
    if (!closed && nativeHandle != 0) {
      nativeDestroy(nativeHandle);
      nativeHandle = 0;
      closed = true;
      LOGGER.log(Level.INFO, "Platform memory manager closed");
    }
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Platform memory manager has been closed");
    }
  }

  // Native method declarations
  private static native long nativeCreate(
      boolean enableHugePages,
      int numaNode,
      long initialPoolSize,
      long maxPoolSize,
      boolean enableCompression,
      boolean enableDeduplication,
      long prefetchBufferSize,
      boolean enableLeakDetection,
      int alignment,
      int pageSize);

  private static native long nativeAllocate(long handle, long size, int alignment);

  private static native boolean nativeDeallocate(long handle, long ptr);

  private static native MemoryStats nativeGetStats(long handle);

  private static native PlatformInfo nativeGetPlatformInfo(long handle);

  private static native MemoryLeak[] nativeDetectLeaks(long handle);

  private static native boolean nativePrefetch(long handle, long ptr, long size);

  private static native byte[] nativeCompress(long handle, byte[] data);

  private static native long nativeDeduplicate(long handle, byte[] data);

  private static native void nativeDestroy(long handle);

  static {
    // Native library will be loaded by the main JNI module
    LOGGER.log(Level.INFO, "PlatformMemoryManager JNI class loaded");
  }
}
