package ai.tegmentum.wasmtime4j;

/**
 * Represents the execution context for WebAssembly operations.
 *
 * <p>The execution context provides access to runtime state and optimization facilities, including
 * support for branch hints, profile-guided optimization, and performance monitoring.
 *
 * @since 1.1.0
 */
public interface WasmExecutionContext {

  /**
   * Applies a branch hint at the specified instruction offset.
   *
   * <p>Branch hints are used by the runtime for performance optimization and do not affect the
   * semantic behavior of the program.
   *
   * @param hint the branch hint to apply
   * @param instructionOffset the byte offset of the target instruction
   * @param additionalData optional additional data for the hint
   * @return true if the hint was successfully applied
   */
  boolean applyBranchHint(
      final BranchHintingInstructions hint,
      final long instructionOffset,
      final Object... additionalData);

  /**
   * Gets the current execution statistics for performance analysis.
   *
   * @return execution statistics
   */
  ExecutionStatistics getExecutionStatistics();

  /**
   * Checks if profile-guided optimization is enabled.
   *
   * @return true if PGO is enabled
   */
  boolean isProfileGuidedOptimizationEnabled();

  /**
   * Gets the optimization level for this execution context.
   *
   * @return the optimization level
   */
  OptimizationLevel getOptimizationLevel();

  /**
   * Sets the optimization level for this execution context.
   *
   * @param level the new optimization level
   */
  void setOptimizationLevel(final OptimizationLevel level);

  /**
   * Gets branch prediction statistics if available.
   *
   * @return branch prediction statistics, or null if not available
   */
  BranchPredictionStatistics getBranchPredictionStatistics();

  /** Resets performance counters and statistics. */
  void resetStatistics();

  /** Optimization level enumeration. */
  enum OptimizationLevel {
    /** No optimization - fastest compilation. */
    NONE(0),
    /** Basic optimization - balanced compilation/execution speed. */
    BASIC(1),
    /** Aggressive optimization - slower compilation, faster execution. */
    AGGRESSIVE(2),
    /** Maximum optimization - slowest compilation, maximum performance. */
    MAXIMUM(3);

    private final int level;

    OptimizationLevel(final int level) {
      this.level = level;
    }

    public int getLevel() {
      return level;
    }

    public boolean isAtLeast(final OptimizationLevel other) {
      return this.level >= other.level;
    }
  }

  /** Execution statistics for performance analysis. */
  interface ExecutionStatistics {
    /** Total number of instructions executed. */
    long getInstructionCount();

    /** Total number of function calls. */
    long getFunctionCallCount();

    /** Total number of branches taken. */
    long getBranchCount();

    /** Number of correctly predicted branches. */
    long getCorrectBranchPredictions();

    /** Total execution time in nanoseconds. */
    long getExecutionTimeNanos();

    /** Memory allocation statistics. */
    MemoryStatistics getMemoryStatistics();

    /** Hot spot analysis results. */
    HotSpotAnalysis getHotSpotAnalysis();

    /** Memory allocation and usage statistics. */
    interface MemoryStatistics {
      long getTotalAllocatedBytes();

      long getCurrentUsageBytes();

      long getPeakUsageBytes();

      int getGarbageCollections();
    }

    /** Analysis of performance hot spots in execution. */
    interface HotSpotAnalysis {
      /** Most frequently executed functions. */
      FunctionHotSpot[] getHotFunctions();

      /** Most frequently taken branches. */
      BranchHotSpot[] getHotBranches();

      /** Most memory-intensive operations. */
      MemoryHotSpot[] getMemoryHotSpots();

      /** Information about frequently executed functions. */
      interface FunctionHotSpot {
        String getFunctionName();

        long getCallCount();

        long getTotalExecutionTime();

        double getPercentageOfTotalTime();
      }

      /** Information about frequently taken branches. */
      interface BranchHotSpot {
        long getInstructionOffset();

        long getBranchCount();

        double getTakenPercentage();

        BranchHintingInstructions.BranchType getBranchType();
      }

      /** Information about memory-intensive operations. */
      interface MemoryHotSpot {
        long getInstructionOffset();

        long getAllocationCount();

        long getTotalBytesAllocated();

        String getOperationType();
      }
    }
  }

  /** Branch prediction statistics. */
  interface BranchPredictionStatistics {
    /** Total number of branch predictions made. */
    long getTotalPredictions();

    /** Number of correct predictions. */
    long getCorrectPredictions();

    /** Branch prediction accuracy (0.0 to 1.0). */
    double getAccuracy();

    /** Number of mispredicted branches by type. */
    java.util.Map<BranchHintingInstructions.BranchType, Long> getMispredictionsByType();

    /** Most frequently mispredicted branch locations. */
    MispredictedBranch[] getTopMispredictions();

    /** Information about mispredicted branch locations. */
    interface MispredictedBranch {
      long getInstructionOffset();

      long getMispredictionCount();

      BranchHintingInstructions.BranchType getBranchType();

      double getActualTakenPercentage();

      BranchHintingInstructions getAppliedHint();
    }
  }

  /**
   * Creates a basic execution context with default settings.
   *
   * @return a new execution context
   */
  static WasmExecutionContext createDefault() {
    return create(OptimizationLevel.BASIC, false);
  }

  /**
   * Creates an execution context with specified settings.
   *
   * @param optimizationLevel the optimization level
   * @param enablePGO whether to enable profile-guided optimization
   * @return a new execution context
   */
  static WasmExecutionContext create(
      final OptimizationLevel optimizationLevel, final boolean enablePGO) {
    return new DefaultWasmExecutionContext(optimizationLevel, enablePGO);
  }
}
