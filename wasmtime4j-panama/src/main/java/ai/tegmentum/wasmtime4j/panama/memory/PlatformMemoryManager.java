package ai.tegmentum.wasmtime4j.panama.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Platform-specific memory management for WebAssembly runtimes using Panama FFI with huge pages,
 * NUMA awareness, and advanced optimizations.
 *
 * <p>This class provides Panama Foreign Function Interface bindings to the native platform memory
 * allocator implemented in Rust, offering comprehensive memory management features: - Huge pages
 * support for Linux, macOS, and Windows - NUMA-aware memory allocation and thread binding - Custom
 * memory allocators with platform-specific optimization - Memory prefetching and cache optimization
 * strategies - Memory compression and deduplication for WebAssembly heaps - Comprehensive memory
 * usage monitoring and leak detection - Memory pool management with size-based allocation
 * strategies - Platform-specific virtual memory management and protection
 */
public final class PlatformMemoryManager implements AutoCloseable {
  private static final Logger LOGGER = Logger.getLogger(PlatformMemoryManager.class.getName());

  // Native library symbols - loaded once per class
  private static final Linker LINKER = Linker.nativeLinker();
  private static final SymbolLookup LOOKUP =
      SymbolLookup.libraryLookup("wasmtime4j_native", Arena.ofAuto());

  // Native function handles
  private static final MethodHandle CREATE_ALLOCATOR;
  private static final MethodHandle ALLOCATE_MEMORY;
  private static final MethodHandle DEALLOCATE_MEMORY;
  private static final MethodHandle GET_STATS;
  private static final MethodHandle GET_PLATFORM_INFO;
  private static final MethodHandle DETECT_LEAKS;
  private static final MethodHandle PREFETCH_MEMORY;
  private static final MethodHandle COMPRESS_MEMORY;
  private static final MethodHandle DEDUPLICATE_MEMORY;
  private static final MethodHandle DESTROY_ALLOCATOR;

  // Memory layouts for native structures
  private static final GroupLayout PLATFORM_CONFIG_LAYOUT =
      MemoryLayout.structLayout(
          ValueLayout.JAVA_BOOLEAN.withName("enable_huge_pages"),
          ValueLayout.JAVA_INT.withName("numa_node"),
          ValueLayout.JAVA_LONG.withName("initial_pool_size"),
          ValueLayout.JAVA_LONG.withName("max_pool_size"),
          ValueLayout.JAVA_BOOLEAN.withName("enable_compression"),
          ValueLayout.JAVA_BOOLEAN.withName("enable_deduplication"),
          ValueLayout.JAVA_LONG.withName("prefetch_buffer_size"),
          ValueLayout.JAVA_BOOLEAN.withName("enable_leak_detection"),
          ValueLayout.JAVA_INT.withName("alignment"),
          ValueLayout.JAVA_INT.withName("page_size"));

  private static final GroupLayout MEMORY_STATS_LAYOUT =
      MemoryLayout.structLayout(
          ValueLayout.JAVA_LONG.withName("total_allocated"),
          ValueLayout.JAVA_LONG.withName("total_freed"),
          ValueLayout.JAVA_LONG.withName("current_usage"),
          ValueLayout.JAVA_LONG.withName("peak_usage"),
          ValueLayout.JAVA_LONG.withName("allocation_count"),
          ValueLayout.JAVA_LONG.withName("deallocation_count"),
          ValueLayout.JAVA_DOUBLE.withName("fragmentation_ratio"),
          ValueLayout.JAVA_DOUBLE.withName("compression_ratio"),
          ValueLayout.JAVA_LONG.withName("deduplication_savings"),
          ValueLayout.JAVA_LONG.withName("huge_pages_used"),
          ValueLayout.JAVA_DOUBLE.withName("numa_hit_rate"));

  private static final GroupLayout PLATFORM_INFO_LAYOUT =
      MemoryLayout.structLayout(
          ValueLayout.JAVA_LONG.withName("total_physical_memory"),
          ValueLayout.JAVA_LONG.withName("available_memory"),
          ValueLayout.JAVA_LONG.withName("page_size"),
          ValueLayout.JAVA_LONG.withName("huge_page_size"),
          ValueLayout.JAVA_INT.withName("numa_nodes"),
          ValueLayout.JAVA_INT.withName("cpu_cores"),
          ValueLayout.JAVA_INT.withName("cache_line_size"),
          ValueLayout.JAVA_BOOLEAN.withName("supports_huge_pages"),
          ValueLayout.JAVA_BOOLEAN.withName("supports_numa"));

  static {
    try {
      // Initialize native method handles
      CREATE_ALLOCATOR =
          LINKER.downcallHandle(
              LOOKUP.find("wasmtime4j_platform_memory_allocator_create").orElseThrow(),
              FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      ALLOCATE_MEMORY =
          LINKER.downcallHandle(
              LOOKUP.find("wasmtime4j_platform_memory_allocate").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_INT));

      DEALLOCATE_MEMORY =
          LINKER.downcallHandle(
              LOOKUP.find("wasmtime4j_platform_memory_deallocate").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      GET_STATS =
          LINKER.downcallHandle(
              LOOKUP.find("wasmtime4j_platform_memory_get_stats").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      GET_PLATFORM_INFO =
          LINKER.downcallHandle(
              LOOKUP.find("wasmtime4j_platform_memory_get_info").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      DETECT_LEAKS =
          LINKER.downcallHandle(
              LOOKUP.find("wasmtime4j_platform_memory_detect_leaks").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      PREFETCH_MEMORY =
          LINKER.downcallHandle(
              LOOKUP.find("wasmtime4j_platform_memory_prefetch").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG));

      COMPRESS_MEMORY =
          LINKER.downcallHandle(
              LOOKUP.find("wasmtime4j_platform_memory_compress").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      DEDUPLICATE_MEMORY =
          LINKER.downcallHandle(
              LOOKUP.find("wasmtime4j_platform_memory_deduplicate").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG));

      DESTROY_ALLOCATOR =
          LINKER.downcallHandle(
              LOOKUP.find("wasmtime4j_platform_memory_allocator_destroy").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

      LOGGER.log(Level.INFO, "PlatformMemoryManager Panama FFI initialized successfully");

    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize PlatformMemoryManager native bindings", e);
    }
  }

  // Native handle to the platform memory allocator
  private MemorySegment nativeHandle;
  private final Arena arena;
  private volatile boolean closed = false;

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

    /** Page size enumeration for memory allocation. */
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

  /** Platform memory information. */
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
    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }

    this.arena = Arena.ofAuto();

    try {
      // Create native configuration structure
      MemorySegment configSegment = arena.allocate(PLATFORM_CONFIG_LAYOUT);

      configSegment.set(
          ValueLayout.JAVA_BOOLEAN,
          PLATFORM_CONFIG_LAYOUT.byteOffset(
              MemoryLayout.PathElement.groupElement("enable_huge_pages")),
          config.enableHugePages);
      configSegment.set(
          ValueLayout.JAVA_INT,
          PLATFORM_CONFIG_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("numa_node")),
          config.numaNode);
      configSegment.set(
          ValueLayout.JAVA_LONG,
          PLATFORM_CONFIG_LAYOUT.byteOffset(
              MemoryLayout.PathElement.groupElement("initial_pool_size")),
          config.initialPoolSizeBytes);
      configSegment.set(
          ValueLayout.JAVA_LONG,
          PLATFORM_CONFIG_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("max_pool_size")),
          config.maxPoolSizeBytes);
      configSegment.set(
          ValueLayout.JAVA_BOOLEAN,
          PLATFORM_CONFIG_LAYOUT.byteOffset(
              MemoryLayout.PathElement.groupElement("enable_compression")),
          config.enableCompression);
      configSegment.set(
          ValueLayout.JAVA_BOOLEAN,
          PLATFORM_CONFIG_LAYOUT.byteOffset(
              MemoryLayout.PathElement.groupElement("enable_deduplication")),
          config.enableDeduplication);
      configSegment.set(
          ValueLayout.JAVA_LONG,
          PLATFORM_CONFIG_LAYOUT.byteOffset(
              MemoryLayout.PathElement.groupElement("prefetch_buffer_size")),
          config.prefetchBufferSizeBytes);
      configSegment.set(
          ValueLayout.JAVA_BOOLEAN,
          PLATFORM_CONFIG_LAYOUT.byteOffset(
              MemoryLayout.PathElement.groupElement("enable_leak_detection")),
          config.enableLeakDetection);
      configSegment.set(
          ValueLayout.JAVA_INT,
          PLATFORM_CONFIG_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("alignment")),
          config.alignmentBytes);
      configSegment.set(
          ValueLayout.JAVA_INT,
          PLATFORM_CONFIG_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("page_size")),
          config.pageSize.getValue());

      // Call native allocator creation
      this.nativeHandle = (MemorySegment) CREATE_ALLOCATOR.invoke(configSegment);

      if (nativeHandle == null || nativeHandle.address() == 0) {
        throw new RuntimeException("Failed to create platform memory allocator");
      }

      LOGGER.log(
          Level.INFO, "Platform memory manager created with handle: {0}", nativeHandle.address());

    } catch (Throwable t) {
      throw new RuntimeException("Failed to create platform memory manager", t);
    }
  }

  /**
   * Allocates memory with platform-specific optimizations.
   *
   * @param size Size in bytes to allocate
   * @param alignment Memory alignment requirement (0 for default)
   * @return Native pointer as MemorySegment
   * @throws IllegalArgumentException if size is 0 or negative
   * @throws RuntimeException if allocation fails
   */
  public MemorySegment allocate(long size, int alignment) {
    ensureNotClosed();

    if (size <= 0) {
      throw new IllegalArgumentException("Size must be positive: " + size);
    }

    try {
      MemorySegment ptr = (MemorySegment) ALLOCATE_MEMORY.invoke(nativeHandle, size, alignment);
      if (ptr == null || ptr.address() == 0) {
        throw new RuntimeException("Failed to allocate " + size + " bytes");
      }

      LOGGER.log(
          Level.FINE,
          "Allocated {0} bytes at 0x{1}",
          new Object[] {size, Long.toHexString(ptr.address())});
      return ptr;

    } catch (Throwable t) {
      throw new RuntimeException("Memory allocation failed", t);
    }
  }

  /**
   * Deallocates previously allocated memory.
   *
   * @param ptr Memory segment to deallocate
   * @throws IllegalArgumentException if ptr is null or invalid
   * @throws RuntimeException if deallocation fails
   */
  public void deallocate(MemorySegment ptr) {
    ensureNotClosed();

    if (ptr == null || ptr.address() == 0) {
      throw new IllegalArgumentException("Pointer cannot be null");
    }

    try {
      boolean success = (boolean) DEALLOCATE_MEMORY.invoke(nativeHandle, ptr);
      if (!success) {
        throw new RuntimeException(
            "Failed to deallocate memory at 0x" + Long.toHexString(ptr.address()));
      }

      LOGGER.log(Level.FINE, "Deallocated memory at 0x{0}", Long.toHexString(ptr.address()));

    } catch (Throwable t) {
      throw new RuntimeException("Memory deallocation failed", t);
    }
  }

  /**
   * Gets current memory allocation statistics.
   *
   * @return Memory statistics
   */
  public MemoryStats getStats() {
    ensureNotClosed();

    try {
      MemorySegment statsSegment = arena.allocate(MEMORY_STATS_LAYOUT);
      boolean success = (boolean) GET_STATS.invoke(nativeHandle, statsSegment);

      if (!success) {
        throw new RuntimeException("Failed to get memory statistics");
      }

      return new MemoryStats(
          statsSegment.get(
              ValueLayout.JAVA_LONG,
              MEMORY_STATS_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("total_allocated"))),
          statsSegment.get(
              ValueLayout.JAVA_LONG,
              MEMORY_STATS_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("total_freed"))),
          statsSegment.get(
              ValueLayout.JAVA_LONG,
              MEMORY_STATS_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("current_usage"))),
          statsSegment.get(
              ValueLayout.JAVA_LONG,
              MEMORY_STATS_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("peak_usage"))),
          statsSegment.get(
              ValueLayout.JAVA_LONG,
              MEMORY_STATS_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("allocation_count"))),
          statsSegment.get(
              ValueLayout.JAVA_LONG,
              MEMORY_STATS_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("deallocation_count"))),
          statsSegment.get(
              ValueLayout.JAVA_DOUBLE,
              MEMORY_STATS_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("fragmentation_ratio"))),
          statsSegment.get(
              ValueLayout.JAVA_DOUBLE,
              MEMORY_STATS_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("compression_ratio"))),
          statsSegment.get(
              ValueLayout.JAVA_LONG,
              MEMORY_STATS_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("deduplication_savings"))),
          statsSegment.get(
              ValueLayout.JAVA_LONG,
              MEMORY_STATS_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("huge_pages_used"))),
          statsSegment.get(
              ValueLayout.JAVA_DOUBLE,
              MEMORY_STATS_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("numa_hit_rate"))));

    } catch (Throwable t) {
      throw new RuntimeException("Failed to get memory statistics", t);
    }
  }

  /**
   * Gets platform memory information.
   *
   * @return Platform memory information
   */
  public PlatformInfo getPlatformInfo() {
    ensureNotClosed();

    try {
      MemorySegment infoSegment = arena.allocate(PLATFORM_INFO_LAYOUT);
      boolean success = (boolean) GET_PLATFORM_INFO.invoke(nativeHandle, infoSegment);

      if (!success) {
        throw new RuntimeException("Failed to get platform information");
      }

      return new PlatformInfo(
          infoSegment.get(
              ValueLayout.JAVA_LONG,
              PLATFORM_INFO_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("total_physical_memory"))),
          infoSegment.get(
              ValueLayout.JAVA_LONG,
              PLATFORM_INFO_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("available_memory"))),
          infoSegment.get(
              ValueLayout.JAVA_LONG,
              PLATFORM_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("page_size"))),
          infoSegment.get(
              ValueLayout.JAVA_LONG,
              PLATFORM_INFO_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("huge_page_size"))),
          infoSegment.get(
              ValueLayout.JAVA_INT,
              PLATFORM_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("numa_nodes"))),
          infoSegment.get(
              ValueLayout.JAVA_INT,
              PLATFORM_INFO_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement("cpu_cores"))),
          infoSegment.get(
              ValueLayout.JAVA_INT,
              PLATFORM_INFO_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("cache_line_size"))),
          infoSegment.get(
              ValueLayout.JAVA_BOOLEAN,
              PLATFORM_INFO_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("supports_huge_pages"))),
          infoSegment.get(
              ValueLayout.JAVA_BOOLEAN,
              PLATFORM_INFO_LAYOUT.byteOffset(
                  MemoryLayout.PathElement.groupElement("supports_numa"))));

    } catch (Throwable t) {
      throw new RuntimeException("Failed to get platform information", t);
    }
  }

  /**
   * Prefetches memory region for improved cache performance.
   *
   * @param ptr Memory segment to prefetch
   * @param size Size of memory region in bytes
   * @throws IllegalArgumentException if ptr is null or size is negative
   */
  public void prefetchMemory(MemorySegment ptr, long size) {
    ensureNotClosed();

    if (ptr == null || ptr.address() == 0) {
      throw new IllegalArgumentException("Pointer cannot be null");
    }
    if (size < 0) {
      throw new IllegalArgumentException("Size cannot be negative: " + size);
    }

    try {
      boolean success = (boolean) PREFETCH_MEMORY.invoke(nativeHandle, ptr, size);
      if (!success) {
        LOGGER.log(
            Level.WARNING, "Failed to prefetch memory at 0x{0}", Long.toHexString(ptr.address()));
      }
    } catch (Throwable t) {
      LOGGER.log(Level.WARNING, "Memory prefetch failed", t);
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

    try {
      // Allocate native memory for input data
      MemorySegment inputSegment = arena.allocate(data.length);
      inputSegment.copyFrom(MemorySegment.ofArray(data));

      // Allocate pointers for output
      MemorySegment compressedPtrPtr = arena.allocate(ValueLayout.ADDRESS);
      MemorySegment compressedLenPtr = arena.allocate(ValueLayout.JAVA_LONG);

      boolean success =
          (boolean)
              COMPRESS_MEMORY.invoke(
                  nativeHandle,
                  inputSegment,
                  (long) data.length,
                  compressedPtrPtr,
                  compressedLenPtr);

      if (!success) {
        throw new RuntimeException("Failed to compress data");
      }

      // Extract compressed data
      MemorySegment compressedPtr = compressedPtrPtr.get(ValueLayout.ADDRESS, 0);
      long compressedLen = compressedLenPtr.get(ValueLayout.JAVA_LONG, 0);

      byte[] compressed = new byte[(int) compressedLen];
      compressedPtr.reinterpret(compressedLen).asByteBuffer().get(compressed);

      LOGGER.log(
          Level.FINE,
          "Compressed {0} bytes to {1} bytes (ratio: {2})",
          new Object[] {data.length, compressed.length, (double) compressed.length / data.length});
      return compressed;

    } catch (Throwable t) {
      throw new RuntimeException("Memory compression failed", t);
    }
  }

  /**
   * Performs memory deduplication on data.
   *
   * @param data Data to deduplicate
   * @return Memory segment pointing to deduplicated memory (may be shared)
   * @throws IllegalArgumentException if data is null or empty
   * @throws RuntimeException if deduplication fails
   */
  public MemorySegment deduplicateMemory(byte[] data) {
    ensureNotClosed();

    if (data == null || data.length == 0) {
      throw new IllegalArgumentException("Data cannot be null or empty");
    }

    try {
      // Allocate native memory for input data
      MemorySegment inputSegment = arena.allocate(data.length);
      inputSegment.copyFrom(MemorySegment.ofArray(data));

      MemorySegment ptr =
          (MemorySegment) DEDUPLICATE_MEMORY.invoke(nativeHandle, inputSegment, (long) data.length);

      if (ptr == null || ptr.address() == 0) {
        throw new RuntimeException("Failed to deduplicate data");
      }

      return ptr;

    } catch (Throwable t) {
      throw new RuntimeException("Memory deduplication failed", t);
    }
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
    if (closed) {
      return;
    }
    closed = true;

    if (nativeHandle != null && nativeHandle.address() != 0) {
      try {
        DESTROY_ALLOCATOR.invoke(nativeHandle);
        nativeHandle = null;
        LOGGER.log(Level.INFO, "Platform memory manager closed");
      } catch (Throwable t) {
        LOGGER.log(Level.WARNING, "Error during platform memory manager cleanup", t);
      }
    }
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Platform memory manager has been closed");
    }
  }
}
