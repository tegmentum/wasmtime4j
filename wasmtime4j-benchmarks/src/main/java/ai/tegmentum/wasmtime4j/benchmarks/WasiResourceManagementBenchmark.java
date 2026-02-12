package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.jni.wasi.WasiFileOperation;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiResourceLeakDetector;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiResourceUsageTracker;
import ai.tegmentum.wasmtime4j.wasi.permission.WasiResourceLimits;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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
import org.openjdk.jmh.annotations.Warmup;

/**
 * Performance benchmarks for WASI resource management operations.
 *
 * <p>This benchmark suite measures the performance characteristics of various resource management
 * components including leak detection, usage tracking, and context isolation validation. The
 * benchmarks help ensure that resource management operations have minimal impact on WASI operation
 * performance.
 *
 * @since 1.0.0
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class WasiResourceManagementBenchmark {

  @Param({"false", "true"})
  private boolean detailedTracking;

  private WasiResourceLeakDetector leakDetector;
  private WasiResourceUsageTracker usageTracker;
  private WasiResourceLimits resourceLimits;

  // Test data
  private String contextId;
  private Path testPath;
  private Object testMemorySegment;
  private MockWasiContext mockContext;

  @Setup(Level.Trial)
  public void setupTrial() {
    // Create resource limits
    resourceLimits =
        WasiResourceLimits.builder()
            .withMaxMemoryBytes(100L * 1024L * 1024L) // 100 MB
            .withMaxFileDescriptors(1000)
            .withMaxDiskReadsPerSecond(10000)
            .withMaxDiskWritesPerSecond(10000)
            .withMaxExecutionTime(Duration.ofMinutes(1))
            .build();

    // Initialize components
    leakDetector =
        new WasiResourceLeakDetector(
            1000, // threshold
            Duration.ofMinutes(5), // age threshold
            60 // monitoring interval
            );

    usageTracker = new WasiResourceUsageTracker(resourceLimits, detailedTracking);

    // Setup test data
    contextId = "benchmark-context";
    testPath = Paths.get("/tmp/benchmark/test.txt");
    testMemorySegment = new Object();
    mockContext = new MockWasiContext(contextId);

    // Register context in usage tracker only
    // Note: IsolationValidator requires a real WasiContext with native handle,
    // so we skip registering with isolationValidator for benchmarking purposes.
    // The isolation validator benchmarks will be skipped.
    usageTracker.registerContext(contextId);
  }

  @TearDown(Level.Trial)
  public void teardownTrial() {
    if (leakDetector != null) {
      leakDetector.close();
    }
    if (usageTracker != null) {
      usageTracker.unregisterContext(contextId);
    }
  }

  @Setup(Level.Iteration)
  public void setupIteration() {
    // Reset any iteration-specific state if needed
  }

  @Benchmark
  public void benchmarkLeakDetectorTracking() {
    // Benchmark resource tracking in leak detector
    leakDetector.trackWasiContext(contextId + System.nanoTime(), null);
    leakDetector.trackMemorySegment(System.nanoTime(), testMemorySegment);
  }

  @Benchmark
  public void benchmarkLeakDetectorUntracking() {
    // Pre-track resources
    final String dynamicContextId = contextId + System.nanoTime();
    final long dynamicAddress = System.nanoTime();
    leakDetector.trackWasiContext(dynamicContextId, null);
    leakDetector.trackMemorySegment(dynamicAddress, testMemorySegment);

    // Benchmark untracking
    leakDetector.untrackWasiContext(dynamicContextId);
    leakDetector.untrackMemorySegment(dynamicAddress);
  }

  @Benchmark
  public void benchmarkLeakDetection() {
    // Benchmark leak detection operation
    leakDetector.performLeakDetection();
  }

  @Benchmark
  public void benchmarkMemoryAllocationTracking() {
    // Benchmark memory allocation tracking
    usageTracker.recordMemoryAllocation(contextId, 1024);
  }

  @Benchmark
  public void benchmarkMemoryDeallocationTracking() {
    // Benchmark memory deallocation tracking
    usageTracker.recordMemoryDeallocation(contextId, 512);
  }

  @Benchmark
  public void benchmarkFileSystemOperationTracking() {
    // Benchmark file system operation tracking
    usageTracker.recordFileSystemOperation(
        contextId, WasiFileOperation.READ, 1024, 1000000); // 1ms operation
  }

  @Benchmark
  public void benchmarkCpuTimeTracking() {
    // Benchmark CPU time tracking
    usageTracker.recordCpuTime(contextId, 1000000); // 1ms CPU time
  }

  @Benchmark
  public void benchmarkExecutionTimeTracking() {
    // Benchmark execution time tracking
    usageTracker.recordExecutionTime(contextId, 2000000); // 2ms execution time
  }

  @Benchmark
  public void benchmarkContextUsageSnapshot() {
    // Benchmark getting context usage snapshot
    usageTracker.getContextUsage(contextId);
  }

  @Benchmark
  public void benchmarkGlobalUsageSnapshot() {
    // Benchmark getting global usage snapshot
    usageTracker.getGlobalUsage();
  }

  @Benchmark
  public void benchmarkCompleteResourceOperation() {
    // Benchmark a complete resource operation using leak detector and usage tracker
    final long startTime = System.nanoTime();

    // 1. Track operation
    usageTracker.recordFileSystemOperation(contextId, WasiFileOperation.READ, 2048, 1500000);

    // 2. Track memory allocation
    usageTracker.recordMemoryAllocation(contextId, 2048);

    // 3. Track execution time
    final long executionTime = System.nanoTime() - startTime;
    usageTracker.recordExecutionTime(contextId, executionTime);
  }

  @Benchmark
  public void benchmarkConcurrentResourceTracking() {
    // Benchmark concurrent operations that might happen in real usage
    final long timestamp = System.nanoTime();

    // Simulate concurrent resource management operations
    usageTracker.recordMemoryAllocation(contextId, 512);
    usageTracker.recordCpuTime(contextId, 500000);
    leakDetector.trackMemorySegment(timestamp, testMemorySegment);

    // Cleanup
    usageTracker.recordMemoryDeallocation(contextId, 512);
    leakDetector.untrackMemorySegment(timestamp);
  }

  @Benchmark
  public void benchmarkResourceCleanup() {
    // Benchmark resource cleanup operations
    final long timestamp = System.nanoTime();
    final String dynamicContextId = "cleanup-context-" + timestamp;

    // Setup resources
    leakDetector.trackWasiContext(dynamicContextId, null);
    leakDetector.trackMemorySegment(timestamp, testMemorySegment);

    // Benchmark cleanup
    leakDetector.untrackWasiContext(dynamicContextId);
    leakDetector.untrackMemorySegment(timestamp);
  }

  @Benchmark
  public void benchmarkStatisticsCollection() {
    // Benchmark collecting statistics from leak detector and usage tracker
    leakDetector.getStatistics();
    usageTracker.getGlobalUsage();
  }

  /** Benchmark-specific resource limits for performance testing. */
  private static WasiResourceLimits createBenchmarkResourceLimits() {
    return WasiResourceLimits.builder()
        .withMaxMemoryBytes(10L * 1024L * 1024L) // 10 MB for benchmark
        .withMaxFileDescriptors(500)
        .withMaxDiskReadsPerSecond(5000)
        .withMaxDiskWritesPerSecond(5000)
        .withMaxExecutionTime(Duration.ofSeconds(30))
        .build();
  }

  /** Mock WASI context for benchmarking. */
  private static final class MockWasiContext {
    private final String contextId;

    MockWasiContext(final String contextId) {
      this.contextId = contextId;
    }

    public String getContextId() {
      return contextId;
    }

    @Override
    public String toString() {
      return "MockWasiContext{contextId='" + contextId + "'}";
    }
  }
}
