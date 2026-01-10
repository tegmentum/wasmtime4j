package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Memory;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Memory management optimization and leak prevention benchmark.
 *
 * <p>This benchmark validates memory management optimizations and ensures proper resource cleanup
 * to prevent memory leaks. It tests memory allocation patterns, garbage collection efficiency, and
 * resource cleanup under various load scenarios.
 *
 * <p>Memory management areas tested:
 *
 * <ul>
 *   <li>Resource allocation and deallocation efficiency
 *   <li>Memory leak detection and prevention
 *   <li>Garbage collection impact and optimization
 *   <li>Memory pool management and reuse
 *   <li>Large object allocation patterns
 *   <li>Concurrent memory access safety
 * </ul>
 *
 * <p>Optimization targets:
 *
 * <ul>
 *   <li>Zero memory leaks under normal and stress conditions
 *   <li>Minimal garbage collection pressure from native resources
 *   <li>Efficient memory reuse through pooling and caching
 *   <li>Predictable memory usage patterns
 * </ul>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1) // Use single fork to better track memory usage
@State(Scope.Benchmark)
public class MemoryManagementOptimizationBenchmark extends BenchmarkBase {

  /** Memory-intensive WebAssembly module. */
  private static final byte[] MEMORY_INTENSIVE_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // WASM magic
        0x01,
        0x00,
        0x00,
        0x00, // Version
        0x01,
        0x18,
        0x06, // Type section
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x00, // Type 0: (i32, i32) -> () (alloc)
        0x60,
        0x01,
        0x7f,
        0x00, // Type 1: (i32) -> () (free)
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // Type 2: (i32, i32) -> i32 (copy)
        0x60,
        0x03,
        0x7f,
        0x7f,
        0x7f,
        0x00, // Type 3: (i32, i32, i32) -> () (fill)
        0x60,
        0x00,
        0x01,
        0x7f, // Type 4: () -> i32 (get_used)
        0x60,
        0x00,
        0x00, // Type 5: () -> () (gc)
        0x03,
        0x07,
        0x06,
        0x00,
        0x01,
        0x02,
        0x03,
        0x04,
        0x05, // Function section: 6 functions
        0x05,
        0x03,
        0x01,
        0x00,
        0x40, // Memory section: 64 pages initial (4MB)
        0x06,
        0x09,
        0x01,
        0x7f,
        0x01,
        0x41,
        0x00,
        0x0b, // Global section: allocation pointer
        0x07,
        0x36,
        0x06, // Export section
        0x05,
        0x61,
        0x6c,
        0x6c,
        0x6f,
        0x63,
        0x00,
        0x00, // Export "alloc"
        0x04,
        0x66,
        0x72,
        0x65,
        0x65,
        0x00,
        0x01, // Export "free"
        0x04,
        0x63,
        0x6f,
        0x70,
        0x79,
        0x00,
        0x02, // Export "copy"
        0x04,
        0x66,
        0x69,
        0x6c,
        0x6c,
        0x00,
        0x03, // Export "fill"
        0x08,
        0x67,
        0x65,
        0x74,
        0x5f,
        0x75,
        0x73,
        0x65,
        0x64,
        0x00,
        0x04, // Export "get_used"
        0x02,
        0x67,
        0x63,
        0x00,
        0x05, // Export "gc"
        0x0a,
        0x4a,
        0x06, // Code section: 6 function bodies
        // Function 0: alloc (simplified bump allocator)
        0x0d,
        0x00,
        0x23,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x24,
        0x00,
        0x23,
        0x00,
        0x20,
        0x00,
        0x36,
        0x02,
        0x00,
        0x0b,
        // Function 1: free (noop for simplicity)
        0x02,
        0x00,
        0x0b,
        // Function 2: copy
        0x11,
        0x00,
        0x20,
        0x02,
        0x04,
        0x40,
        0x03,
        0x40,
        0x20,
        0x00,
        0x20,
        0x01,
        0x2d,
        0x00,
        0x00,
        0x3a,
        0x00,
        0x00,
        0x20,
        0x00,
        0x41,
        0x01,
        0x6a,
        0x21,
        0x00,
        0x20,
        0x01,
        0x41,
        0x01,
        0x6a,
        0x21,
        0x01,
        0x20,
        0x02,
        0x41,
        0x01,
        0x6b,
        0x22,
        0x02,
        0x0d,
        0x00,
        0x0b,
        0x0b,
        0x20,
        0x00,
        0x0b,
        // Function 3: fill
        0x0d,
        0x00,
        0x20,
        0x02,
        0x04,
        0x40,
        0x03,
        0x40,
        0x20,
        0x00,
        0x20,
        0x01,
        0x3a,
        0x00,
        0x00,
        0x20,
        0x00,
        0x41,
        0x01,
        0x6a,
        0x21,
        0x00,
        0x20,
        0x02,
        0x41,
        0x01,
        0x6b,
        0x22,
        0x02,
        0x0d,
        0x00,
        0x0b,
        0x0b,
        0x0b,
        // Function 4: get_used
        0x04,
        0x00,
        0x23,
        0x00,
        0x0b,
        // Function 5: gc (noop)
        0x02,
        0x00,
        0x0b
      };

  /** Large object allocation WebAssembly module. */
  private static final byte[] LARGE_OBJECT_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // WASM magic
        0x01,
        0x00,
        0x00,
        0x00, // Version
        0x01,
        0x0c,
        0x03, // Type section
        0x60,
        0x01,
        0x7f,
        0x01,
        0x7f, // Type 0: (i32) -> i32 (alloc_large)
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x00, // Type 1: (i32, i32) -> () (init_large)
        0x60,
        0x01,
        0x7f,
        0x01,
        0x7f, // Type 2: (i32) -> i32 (checksum)
        0x03,
        0x04,
        0x03,
        0x00,
        0x01,
        0x02, // Function section: 3 functions
        0x05,
        0x04,
        0x01,
        0x01,
        0x00,
        0x80, // Memory section: min=1, max=128 pages
        0x07,
        0x26,
        0x03, // Export section
        0x0a,
        0x61,
        0x6c,
        0x6c,
        0x6f,
        0x63,
        0x5f,
        0x6c,
        0x61,
        0x72,
        0x67,
        0x65,
        0x00,
        0x00, // Export "alloc_large"
        0x09,
        0x69,
        0x6e,
        0x69,
        0x74,
        0x5f,
        0x6c,
        0x61,
        0x72,
        0x67,
        0x65,
        0x00,
        0x01, // Export "init_large"
        0x08,
        0x63,
        0x68,
        0x65,
        0x63,
        0x6b,
        0x73,
        0x75,
        0x6d,
        0x00,
        0x02, // Export "checksum"
        0x0a,
        0x30,
        0x03, // Code section: 3 function bodies
        // Function 0: alloc_large (grow memory)
        0x09,
        0x00,
        0x20,
        0x00,
        0x40,
        0x00,
        0x41,
        0x01,
        0x6b,
        0x0b,
        // Function 1: init_large (initialize memory range)
        0x11,
        0x00,
        0x20,
        0x01,
        0x04,
        0x40,
        0x03,
        0x40,
        0x20,
        0x00,
        0x41,
        0xaa,
        0x01,
        0x3a,
        0x00,
        0x00,
        0x20,
        0x00,
        0x41,
        0x01,
        0x6a,
        0x21,
        0x00,
        0x20,
        0x01,
        0x41,
        0x01,
        0x6b,
        0x22,
        0x01,
        0x0d,
        0x00,
        0x0b,
        0x0b,
        0x0b,
        // Function 2: checksum (simple checksum of memory range)
        0x13,
        0x01,
        0x01,
        0x7f,
        0x41,
        0x00,
        0x21,
        0x01,
        0x20,
        0x00,
        0x04,
        0x40,
        0x03,
        0x40,
        0x20,
        0x01,
        0x20,
        0x00,
        0x2d,
        0x00,
        0x00,
        0x6a,
        0x21,
        0x01,
        0x20,
        0x00,
        0x41,
        0x01,
        0x6a,
        0x21,
        0x00,
        0x20,
        0x00,
        0x41,
        0xe8,
        0x07,
        0x49,
        0x0d,
        0x00,
        0x0b,
        0x0b,
        0x20,
        0x01,
        0x0b
      };

  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  @Param({"BASELINE", "OPTIMIZED"})
  private String memoryOptimization;

  private Engine engine;
  private MemoryMXBean memoryBean;
  private long initialHeapUsage;

  @Setup(Level.Trial)
  public void setupTrial() throws WasmException {
    super.setupRuntime(runtimeTypeName);

    // Create engine with memory optimization settings
    final Engine.Builder engineBuilder = Engine.builder();

    if ("OPTIMIZED".equals(memoryOptimization)) {
      engineBuilder
          .poolingAllocator(true)
          .poolSize(100)
          .maxMemoryPerInstance(64 * 1024 * 1024L) // 64MB per instance
          .enableMemoryDecommit(true)
          .memoryGuardSize(65536) // 64KB guard
          .moduleCaching(true)
          .maxCacheSize(50 * 1024 * 1024L); // 50MB cache
    }

    engine = engineBuilder.build();

    // Initialize memory monitoring
    memoryBean = ManagementFactory.getMemoryMXBean();

    // Force GC to get baseline
    System.gc();
    Thread.yield();
    System.gc();

    initialHeapUsage = memoryBean.getHeapMemoryUsage().getUsed();

    logInfo(
        "Memory management benchmark setup completed for runtime: "
            + runtimeTypeName
            + ", optimization: "
            + memoryOptimization
            + ", initial heap: "
            + (initialHeapUsage / 1024 / 1024)
            + " MB");
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() {
    closeQuietly(engine);

    // Force GC and measure final memory usage
    System.gc();
    Thread.yield();
    System.gc();

    final long finalHeapUsage = memoryBean.getHeapMemoryUsage().getUsed();
    final long heapDelta = finalHeapUsage - initialHeapUsage;

    logInfo(
        "Memory management benchmark teardown completed for runtime: "
            + runtimeTypeName
            + ", final heap: "
            + (finalHeapUsage / 1024 / 1024)
            + " MB"
            + ", delta: "
            + (heapDelta / 1024 / 1024)
            + " MB");

    super.tearDownRuntime();
  }

  /** Benchmarks basic resource allocation and cleanup. */
  @Benchmark
  public void benchmarkBasicResourceLifecycle() throws WasmException {
    final Store store = Store.withoutData(engine);
    try {
      final Module module = Module.fromBinary(engine, MEMORY_INTENSIVE_WASM);
      try {
        final Instance instance = Instance.create(store, module);
        try {
          final Function alloc = instance.getFunction("alloc");
          final Function free = instance.getFunction("free");

          // Allocate and free some memory
          alloc.call(1024, 1024); // Allocate 1KB
          free.call(1024);

        } finally {
          closeQuietly(instance);
        }
      } finally {
        closeQuietly(module);
      }
    } finally {
      closeQuietly(store);
    }
  }

  /** Benchmarks memory allocation patterns with no leaks. */
  @Benchmark
  public void benchmarkMemoryAllocationPatterns() throws WasmException {
    final Store store = Store.withoutData(engine);
    try {
      final Module module = Module.fromBinary(engine, MEMORY_INTENSIVE_WASM);
      try {
        final Instance instance = Instance.create(store, module);
        try {
          final Function alloc = instance.getFunction("alloc");
          final Function getUsed = instance.getFunction("get_used");

          // Pattern 1: Small frequent allocations
          for (int i = 0; i < 10; i++) {
            alloc.call(i * 64, 64);
          }

          // Pattern 2: Medium allocations
          for (int i = 0; i < 5; i++) {
            alloc.call(1024 + i * 256, 256);
          }

          // Pattern 3: Large allocation
          alloc.call(4096, 2048);

          // Check memory usage
          final int used = getUsed.call();
          if (used < 0) {
            throw new WasmException("Invalid memory usage");
          }

        } finally {
          closeQuietly(instance);
        }
      } finally {
        closeQuietly(module);
      }
    } finally {
      closeQuietly(store);
    }
  }

  /** Benchmarks large object allocation and cleanup. */
  @Benchmark
  public void benchmarkLargeObjectAllocation() throws WasmException {
    final Store store = Store.withoutData(engine);
    try {
      final Module module = Module.fromBinary(engine, LARGE_OBJECT_WASM);
      try {
        final Instance instance = Instance.create(store, module);
        try {
          final Function allocLarge = instance.getFunction("alloc_large");
          final Function initLarge = instance.getFunction("init_large");
          final Function checksum = instance.getFunction("checksum");

          // Allocate large memory chunks
          final int pages = allocLarge.call(10); // Try to allocate 10 pages
          if (pages >= 0) {
            // Initialize large memory area
            initLarge.call(0, 1000); // Initialize 1000 bytes

            // Verify integrity
            final int sum = checksum.call(1000);
            if (sum < 0) {
              throw new WasmException("Memory corruption detected");
            }
          }

        } finally {
          closeQuietly(instance);
        }
      } finally {
        closeQuietly(module);
      }
    } finally {
      closeQuietly(store);
    }
  }

  /** Benchmarks memory operations through Java API. */
  @Benchmark
  public void benchmarkJavaMemoryOperations() throws WasmException {
    final Store store = Store.withoutData(engine);
    try {
      final Module module = Module.fromBinary(engine, MEMORY_INTENSIVE_WASM);
      try {
        final Instance instance = Instance.create(store, module);
        try {
          final Memory memory = instance.getMemory("memory");
          final ByteBuffer buffer = memory.buffer();

          // Write pattern
          for (int i = 0; i < 100; i++) {
            buffer.putInt(i * 4, i);
          }

          // Read and verify pattern
          for (int i = 0; i < 100; i++) {
            final int value = buffer.getInt(i * 4);
            if (value != i) {
              throw new WasmException("Memory corruption: expected " + i + ", got " + value);
            }
          }

        } finally {
          closeQuietly(instance);
        }
      } finally {
        closeQuietly(module);
      }
    } finally {
      closeQuietly(store);
    }
  }

  /** Benchmarks concurrent memory operations safety. */
  @Benchmark
  @Threads(4)
  public void benchmarkConcurrentMemoryOperations() throws WasmException {
    final Store store = Store.withoutData(engine);
    try {
      final Module module = Module.fromBinary(engine, MEMORY_INTENSIVE_WASM);
      try {
        final Instance instance = Instance.create(store, module);
        try {
          final Function alloc = instance.getFunction("alloc");
          final Function copy = instance.getFunction("copy");

          final int threadId = Thread.currentThread().hashCode() % 1000;
          final int offset = Math.abs(threadId) % 100;

          // Thread-safe operations
          alloc.call(offset * 64, 64);
          copy.call(offset * 64, (offset + 1) * 64, 32);

        } finally {
          closeQuietly(instance);
        }
      } finally {
        closeQuietly(module);
      }
    } finally {
      closeQuietly(store);
    }
  }

  /** Benchmarks memory pool reuse efficiency. */
  @Benchmark
  public void benchmarkMemoryPoolReuse() throws WasmException {
    // Test multiple allocation cycles to validate pool reuse
    for (int cycle = 0; cycle < 5; cycle++) {
      final Store store = Store.withoutData(engine);
      try {
        final Module module = Module.fromBinary(engine, MEMORY_INTENSIVE_WASM);
        try {
          final Instance instance = Instance.create(store, module);
          try {
            final Function alloc = instance.getFunction("alloc");
            final Function fill = instance.getFunction("fill");

            // Use memory to trigger pool allocation
            alloc.call(0, 1024);
            fill.call(0, 0xFF, 1024);

          } finally {
            closeQuietly(instance);
          }
        } finally {
          closeQuietly(module);
        }
      } finally {
        closeQuietly(store);
      }
    }
  }

  /** Benchmarks memory leak prevention under stress. */
  @Benchmark
  public void benchmarkMemoryLeakPrevention() throws WasmException {
    final List<Store> stores = new ArrayList<>();
    final List<Module> modules = new ArrayList<>();
    final List<Instance> instances = new ArrayList<>();

    try {
      // Create multiple resources
      for (int i = 0; i < 10; i++) {
        final Store store = Store.withoutData(engine);
        final Module module = Module.fromBinary(engine, MEMORY_INTENSIVE_WASM);
        final Instance instance = Instance.create(store, module);

        stores.add(store);
        modules.add(module);
        instances.add(instance);

        // Use some memory
        final Function alloc = instance.getFunction("alloc");
        alloc.call(i * 128, 128);
      }

      // Use all instances
      for (int i = 0; i < instances.size(); i++) {
        final Instance instance = instances.get(i);
        final Function copy = instance.getFunction("copy");
        copy.call(0, i * 128, 64);
      }

    } finally {
      // Cleanup all resources in reverse order
      for (int i = instances.size() - 1; i >= 0; i--) {
        closeQuietly(instances.get(i));
        closeQuietly(modules.get(i));
        closeQuietly(stores.get(i));
      }
    }
  }

  /** Benchmarks memory cleanup efficiency. */
  @Benchmark
  public void benchmarkMemoryCleanupEfficiency() throws WasmException {
    final long startTime = System.nanoTime();

    for (int i = 0; i < 20; i++) {
      final Store store = Store.withoutData(engine);
      try {
        final Module module = Module.fromBinary(engine, MEMORY_INTENSIVE_WASM);
        try {
          final Instance instance = Instance.create(store, module);
          try {
            final Function alloc = instance.getFunction("alloc");
            final Function gc = instance.getFunction("gc");

            // Allocate memory
            alloc.call(i * 64, 64);

            // Trigger cleanup
            gc.call();

          } finally {
            closeQuietly(instance);
          }
        } finally {
          closeQuietly(module);
        }
      } finally {
        closeQuietly(store);
      }
    }

    final long elapsedTime = System.nanoTime() - startTime;
    if (elapsedTime < 0) {
      throw new WasmException("Timer overflow");
    }
  }

  /** Benchmarks memory bounds checking performance. */
  @Benchmark
  public void benchmarkMemoryBoundsChecking() throws WasmException {
    final Store store = Store.withoutData(engine);
    try {
      final Module module = Module.fromBinary(engine, MEMORY_INTENSIVE_WASM);
      try {
        final Instance instance = Instance.create(store, module);
        try {
          final Memory memory = instance.getMemory("memory");
          final ByteBuffer buffer = memory.buffer();
          final int capacity = buffer.capacity();

          // Test bounds checking at various offsets
          final int[] testOffsets = {0, capacity / 4, capacity / 2, capacity * 3 / 4, capacity - 4};

          for (final int offset : testOffsets) {
            if (offset >= 0 && offset < capacity - 4) {
              buffer.putInt(offset, 0x12345678);
              final int value = buffer.getInt(offset);
              if (value != 0x12345678) {
                throw new WasmException("Memory bounds checking failed");
              }
            }
          }

        } finally {
          closeQuietly(instance);
        }
      } finally {
        closeQuietly(module);
      }
    } finally {
      closeQuietly(store);
    }
  }

  /** Benchmarks memory fragmentation handling. */
  @Benchmark
  public void benchmarkMemoryFragmentationHandling() throws WasmException {
    final Store store = Store.withoutData(engine);
    try {
      final Module module = Module.fromBinary(engine, MEMORY_INTENSIVE_WASM);
      try {
        final Instance instance = Instance.create(store, module);
        try {
          final Function alloc = instance.getFunction("alloc");
          final Function free = instance.getFunction("free");
          final Function fill = instance.getFunction("fill");

          // Create fragmentation pattern
          final int[] allocations = new int[10];
          for (int i = 0; i < allocations.length; i++) {
            alloc.call(i * 128, 64); // Allocate 64 bytes with gaps
            allocations[i] = i * 128;
          }

          // Fill memory to test fragmentation handling
          for (int i = 0; i < allocations.length; i++) {
            fill.call(allocations[i], 0xAA, 64);
          }

          // Free every other allocation to create holes
          for (int i = 1; i < allocations.length; i += 2) {
            free.call(allocations[i]);
          }

          // Try to allocate in the holes
          for (int i = 1; i < allocations.length; i += 2) {
            alloc.call(allocations[i], 32); // Smaller allocation that should fit
          }

        } finally {
          closeQuietly(instance);
        }
      } finally {
        closeQuietly(module);
      }
    } finally {
      closeQuietly(store);
    }
  }

  /** Benchmarks garbage collection impact measurement. */
  @Benchmark
  public void benchmarkGarbageCollectionImpact() throws WasmException {
    final MemoryUsage heapBefore = memoryBean.getHeapMemoryUsage();

    // Create temporary resources that should be GC'd
    for (int i = 0; i < 5; i++) {
      final Store store = Store.withoutData(engine);
      try {
        final Module module = Module.fromBinary(engine, MEMORY_INTENSIVE_WASM);
        try {
          final Instance instance = Instance.create(store, module);
          try {
            final Function alloc = instance.getFunction("alloc");
            alloc.call(i * 256, 256);
          } finally {
            closeQuietly(instance);
          }
        } finally {
          closeQuietly(module);
        }
      } finally {
        closeQuietly(store);
      }
    }

    // Force GC to measure impact
    System.gc();
    Thread.yield();

    final MemoryUsage heapAfter = memoryBean.getHeapMemoryUsage();
    final long heapDelta = heapAfter.getUsed() - heapBefore.getUsed();

    // Verify no significant heap growth (allow for some variance)
    if (heapDelta > 10 * 1024 * 1024) { // More than 10MB growth is concerning
      logWarn("Significant heap growth detected: " + (heapDelta / 1024 / 1024) + " MB");
    }
  }

  private void closeQuietly(final AutoCloseable resource) {
    if (resource != null) {
      try {
        resource.close();
      } catch (final Exception e) {
        logWarn("Error closing resource: " + e.getMessage());
      }
    }
  }
}
