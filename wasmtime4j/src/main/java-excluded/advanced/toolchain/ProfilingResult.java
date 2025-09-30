package ai.tegmentum.wasmtime4j.toolchain;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of a WebAssembly profiling operation.
 *
 * <p>Contains performance metrics, timing data, and analysis results
 * from profiling WebAssembly execution.
 *
 * @since 1.0.0
 */
public final class ProfilingResult {

  private final boolean successful;
  private final ProfilingMode profilingMode;
  private final Duration totalExecutionTime;
  private final Duration profilingOverhead;
  private final Instant profilingTimestamp;
  private final ProfilingMetrics metrics;
  private final List<FunctionProfile> functionProfiles;
  private final Optional<MemoryProfile> memoryProfile;
  private final Optional<Path> outputFile;
  private final List<String> profilingErrors;
  private final List<String> profilingWarnings;

  private ProfilingResult(final boolean successful,
                          final ProfilingMode profilingMode,
                          final Duration totalExecutionTime,
                          final Duration profilingOverhead,
                          final Instant profilingTimestamp,
                          final ProfilingMetrics metrics,
                          final List<FunctionProfile> functionProfiles,
                          final MemoryProfile memoryProfile,
                          final Path outputFile,
                          final List<String> profilingErrors,
                          final List<String> profilingWarnings) {
    this.successful = successful;
    this.profilingMode = Objects.requireNonNull(profilingMode);
    this.totalExecutionTime = Objects.requireNonNull(totalExecutionTime);
    this.profilingOverhead = Objects.requireNonNull(profilingOverhead);
    this.profilingTimestamp = Objects.requireNonNull(profilingTimestamp);
    this.metrics = metrics;
    this.functionProfiles = List.copyOf(functionProfiles);
    this.memoryProfile = Optional.ofNullable(memoryProfile);
    this.outputFile = Optional.ofNullable(outputFile);
    this.profilingErrors = List.copyOf(profilingErrors);
    this.profilingWarnings = List.copyOf(profilingWarnings);
  }

  /**
   * Creates a successful profiling result.
   *
   * @param profilingMode the profiling mode used
   * @param totalExecutionTime the total execution time
   * @param profilingOverhead the profiling overhead
   * @param metrics the profiling metrics
   * @param functionProfiles the function profiles
   * @return successful result
   */
  public static ProfilingResult success(final ProfilingMode profilingMode,
                                        final Duration totalExecutionTime,
                                        final Duration profilingOverhead,
                                        final ProfilingMetrics metrics,
                                        final List<FunctionProfile> functionProfiles) {
    return new ProfilingResult(true, profilingMode, totalExecutionTime, profilingOverhead,
                               Instant.now(), metrics, functionProfiles, null, null, List.of(), List.of());
  }

  /**
   * Creates a successful profiling result with memory profile.
   *
   * @param profilingMode the profiling mode used
   * @param totalExecutionTime the total execution time
   * @param profilingOverhead the profiling overhead
   * @param metrics the profiling metrics
   * @param functionProfiles the function profiles
   * @param memoryProfile the memory profile
   * @return successful result with memory profile
   */
  public static ProfilingResult success(final ProfilingMode profilingMode,
                                        final Duration totalExecutionTime,
                                        final Duration profilingOverhead,
                                        final ProfilingMetrics metrics,
                                        final List<FunctionProfile> functionProfiles,
                                        final MemoryProfile memoryProfile) {
    return new ProfilingResult(true, profilingMode, totalExecutionTime, profilingOverhead,
                               Instant.now(), metrics, functionProfiles, memoryProfile, null, List.of(), List.of());
  }

  /**
   * Creates a failed profiling result.
   *
   * @param profilingMode the profiling mode attempted
   * @param profilingErrors the profiling errors
   * @return failed result
   */
  public static ProfilingResult failure(final ProfilingMode profilingMode,
                                        final List<String> profilingErrors) {
    return new ProfilingResult(false, profilingMode, Duration.ZERO, Duration.ZERO,
                               Instant.now(), null, List.of(), null, null, profilingErrors, List.of());
  }

  /**
   * Checks if the profiling was successful.
   *
   * @return true if successful
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Gets the profiling mode used.
   *
   * @return profiling mode
   */
  public ProfilingMode getProfilingMode() {
    return profilingMode;
  }

  /**
   * Gets the total execution time.
   *
   * @return total execution time
   */
  public Duration getTotalExecutionTime() {
    return totalExecutionTime;
  }

  /**
   * Gets the profiling overhead.
   *
   * @return profiling overhead time
   */
  public Duration getProfilingOverhead() {
    return profilingOverhead;
  }

  /**
   * Gets the profiling timestamp.
   *
   * @return when profiling was performed
   */
  public Instant getProfilingTimestamp() {
    return profilingTimestamp;
  }

  /**
   * Gets the profiling metrics.
   *
   * @return profiling metrics, or null if not available
   */
  public ProfilingMetrics getMetrics() {
    return metrics;
  }

  /**
   * Gets the function profiles.
   *
   * @return list of function profiles
   */
  public List<FunctionProfile> getFunctionProfiles() {
    return functionProfiles;
  }

  /**
   * Gets the memory profile.
   *
   * @return memory profile, or empty if not available
   */
  public Optional<MemoryProfile> getMemoryProfile() {
    return memoryProfile;
  }

  /**
   * Gets the output file path.
   *
   * @return output file, or empty if results are in-memory
   */
  public Optional<Path> getOutputFile() {
    return outputFile;
  }

  /**
   * Gets the profiling errors.
   *
   * @return list of errors
   */
  public List<String> getProfilingErrors() {
    return profilingErrors;
  }

  /**
   * Gets the profiling warnings.
   *
   * @return list of warnings
   */
  public List<String> getProfilingWarnings() {
    return profilingWarnings;
  }

  /**
   * Checks if there were any errors.
   *
   * @return true if errors occurred
   */
  public boolean hasErrors() {
    return !profilingErrors.isEmpty();
  }

  /**
   * Checks if there were any warnings.
   *
   * @return true if warnings occurred
   */
  public boolean hasWarnings() {
    return !profilingWarnings.isEmpty();
  }

  /**
   * Gets the overhead percentage.
   *
   * @return profiling overhead as percentage of total execution time
   */
  public double getOverheadPercentage() {
    if (totalExecutionTime.isZero()) return 0.0;
    return (double) profilingOverhead.toNanos() / totalExecutionTime.toNanos() * 100.0;
  }

  @Override
  public String toString() {
    return String.format("ProfilingResult{successful=%s, mode=%s, execution=%s, overhead=%.1f%%, functions=%d}",
        successful, profilingMode, totalExecutionTime, getOverheadPercentage(), functionProfiles.size());
  }

  /**
   * Profiling metrics data.
   */
  public static final class ProfilingMetrics {
    private final long totalInstructionsExecuted;
    private final long totalFunctionCalls;
    private final long peakMemoryUsageBytes;
    private final int samplesCollected;
    private final Map<String, Long> instructionCounts;

    public ProfilingMetrics(final long totalInstructionsExecuted,
                            final long totalFunctionCalls,
                            final long peakMemoryUsageBytes,
                            final int samplesCollected,
                            final Map<String, Long> instructionCounts) {
      this.totalInstructionsExecuted = totalInstructionsExecuted;
      this.totalFunctionCalls = totalFunctionCalls;
      this.peakMemoryUsageBytes = peakMemoryUsageBytes;
      this.samplesCollected = samplesCollected;
      this.instructionCounts = Map.copyOf(instructionCounts);
    }

    public long getTotalInstructionsExecuted() { return totalInstructionsExecuted; }
    public long getTotalFunctionCalls() { return totalFunctionCalls; }
    public long getPeakMemoryUsageBytes() { return peakMemoryUsageBytes; }
    public int getSamplesCollected() { return samplesCollected; }
    public Map<String, Long> getInstructionCounts() { return instructionCounts; }

    @Override
    public String toString() {
      return String.format("ProfilingMetrics{instructions=%d, calls=%d, peakMemory=%d bytes, samples=%d}",
          totalInstructionsExecuted, totalFunctionCalls, peakMemoryUsageBytes, samplesCollected);
    }
  }

  /**
   * Function profiling data.
   */
  public static final class FunctionProfile {
    private final String functionName;
    private final Duration totalTime;
    private final Duration exclusiveTime;
    private final long callCount;
    private final double avgTimePerCall;

    public FunctionProfile(final String functionName,
                           final Duration totalTime,
                           final Duration exclusiveTime,
                           final long callCount) {
      this.functionName = Objects.requireNonNull(functionName);
      this.totalTime = Objects.requireNonNull(totalTime);
      this.exclusiveTime = Objects.requireNonNull(exclusiveTime);
      this.callCount = callCount;
      this.avgTimePerCall = callCount > 0 ? totalTime.toNanos() / (double) callCount : 0.0;
    }

    public String getFunctionName() { return functionName; }
    public Duration getTotalTime() { return totalTime; }
    public Duration getExclusiveTime() { return exclusiveTime; }
    public long getCallCount() { return callCount; }
    public double getAvgTimePerCall() { return avgTimePerCall; }

    @Override
    public String toString() {
      return String.format("FunctionProfile{name='%s', total=%s, calls=%d, avg=%.2fns}",
          functionName, totalTime, callCount, avgTimePerCall);
    }
  }

  /**
   * Memory profiling data.
   */
  public static final class MemoryProfile {
    private final long totalAllocations;
    private final long totalDeallocations;
    private final long peakMemoryUsage;
    private final long currentMemoryUsage;
    private final List<AllocationSite> topAllocationSites;

    public MemoryProfile(final long totalAllocations,
                         final long totalDeallocations,
                         final long peakMemoryUsage,
                         final long currentMemoryUsage,
                         final List<AllocationSite> topAllocationSites) {
      this.totalAllocations = totalAllocations;
      this.totalDeallocations = totalDeallocations;
      this.peakMemoryUsage = peakMemoryUsage;
      this.currentMemoryUsage = currentMemoryUsage;
      this.topAllocationSites = List.copyOf(topAllocationSites);
    }

    public long getTotalAllocations() { return totalAllocations; }
    public long getTotalDeallocations() { return totalDeallocations; }
    public long getPeakMemoryUsage() { return peakMemoryUsage; }
    public long getCurrentMemoryUsage() { return currentMemoryUsage; }
    public List<AllocationSite> getTopAllocationSites() { return topAllocationSites; }

    @Override
    public String toString() {
      return String.format("MemoryProfile{allocs=%d, deallocs=%d, peak=%d bytes, current=%d bytes}",
          totalAllocations, totalDeallocations, peakMemoryUsage, currentMemoryUsage);
    }

    /**
     * Memory allocation site information.
     */
    public static final class AllocationSite {
      private final String location;
      private final long totalBytes;
      private final long allocationCount;

      public AllocationSite(final String location, final long totalBytes, final long allocationCount) {
        this.location = Objects.requireNonNull(location);
        this.totalBytes = totalBytes;
        this.allocationCount = allocationCount;
      }

      public String getLocation() { return location; }
      public long getTotalBytes() { return totalBytes; }
      public long getAllocationCount() { return allocationCount; }

      @Override
      public String toString() {
        return String.format("AllocationSite{location='%s', bytes=%d, count=%d}",
            location, totalBytes, allocationCount);
      }
    }
  }
}