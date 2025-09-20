package ai.tegmentum.wasmtime4j.performance;

import java.util.List;
import java.util.Map;

/**
 * Memory allocation and usage profiling data.
 *
 * <p>MemoryProfile provides detailed information about memory allocation patterns, usage trends,
 * and memory-related performance characteristics during WebAssembly execution.
 *
 * @since 1.0.0
 */
public interface MemoryProfile {

  /**
   * Gets the total memory allocated during the profiling session.
   *
   * @return total bytes allocated
   */
  long getTotalAllocatedBytes();

  /**
   * Gets the total memory deallocated during the profiling session.
   *
   * @return total bytes deallocated
   */
  long getTotalDeallocatedBytes();

  /**
   * Gets the peak memory usage observed during profiling.
   *
   * @return peak memory usage in bytes
   */
  long getPeakMemoryUsage();

  /**
   * Gets the current memory usage at the end of profiling.
   *
   * @return current memory usage in bytes
   */
  long getCurrentMemoryUsage();

  /**
   * Gets the number of allocation operations performed.
   *
   * @return total allocation count
   */
  long getAllocationCount();

  /**
   * Gets the number of deallocation operations performed.
   *
   * @return total deallocation count
   */
  long getDeallocationCount();

  /**
   * Gets the average allocation size.
   *
   * @return average bytes per allocation
   */
  double getAverageAllocationSize();

  /**
   * Gets the largest single allocation made.
   *
   * @return largest allocation size in bytes
   */
  long getLargestAllocation();

  /**
   * Gets allocation statistics by size ranges.
   *
   * @return map of size ranges to allocation counts
   */
  Map<MemorySizeRange, Long> getAllocationSizeDistribution();

  /**
   * Gets memory allocation timeline showing usage over time.
   *
   * @return memory usage timeline
   */
  MemoryTimeline getMemoryTimeline();

  /**
   * Gets allocation profiles by function.
   *
   * @return map of function names to their memory allocation data
   */
  Map<String, FunctionMemoryProfile> getFunctionMemoryProfiles();

  /**
   * Gets the most memory-intensive functions.
   *
   * @param count maximum number of functions to return
   * @return functions ordered by memory usage
   */
  List<FunctionMemoryProfile> getTopMemoryUsers(final int count);

  /**
   * Gets memory leak detection results.
   *
   * @return list of potential memory leaks
   */
  List<MemoryLeak> getPotentialLeaks();

  /**
   * Gets garbage collection impact on memory profiling.
   *
   * @return GC impact analysis
   */
  GCImpactAnalysis getGCImpact();

  /**
   * Gets memory fragmentation analysis.
   *
   * @return fragmentation metrics
   */
  MemoryFragmentation getFragmentationAnalysis();

  /**
   * Gets memory allocation patterns analysis.
   *
   * @return allocation pattern insights
   */
  AllocationPatterns getAllocationPatterns();

  /**
   * Gets the overhead of memory profiling itself.
   *
   * @return memory profiling overhead
   */
  MemoryProfilingOverhead getProfilingOverhead();

  /**
   * Gets WebAssembly linear memory usage statistics.
   *
   * @return linear memory profiling data
   */
  LinearMemoryProfile getLinearMemoryProfile();

  /**
   * Gets host memory usage statistics.
   *
   * @return host memory profiling data
   */
  HostMemoryProfile getHostMemoryProfile();
}
