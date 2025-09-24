package ai.tegmentum.wasmtime4j;

/**
 * Represents the execution context for WebAssembly operations.
 *
 * <p>The execution context provides access to runtime state and optimization
 * facilities, including support for branch hints, profile-guided optimization,
 * and performance monitoring.
 *
 * @since 1.1.0
 */
public interface WasmExecutionContext {

    /**
     * Applies a branch hint at the specified instruction offset.
     *
     * <p>Branch hints are used by the runtime for performance optimization
     * and do not affect the semantic behavior of the program.
     *
     * @param hint the branch hint to apply
     * @param instructionOffset the byte offset of the target instruction
     * @param additionalData optional additional data for the hint
     * @return true if the hint was successfully applied
     */
    boolean applyBranchHint(final BranchHintingInstructions hint,
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

    /**
     * Resets performance counters and statistics.
     */
    void resetStatistics();

    /**
     * Optimization level enumeration.
     */
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

    /**
     * Execution statistics for performance analysis.
     */
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

        interface MemoryStatistics {
            long getTotalAllocatedBytes();
            long getCurrentUsageBytes();
            long getPeakUsageBytes();
            int getGarbageCollections();
        }

        interface HotSpotAnalysis {
            /** Most frequently executed functions. */
            FunctionHotSpot[] getHotFunctions();

            /** Most frequently taken branches. */
            BranchHotSpot[] getHotBranches();

            /** Most memory-intensive operations. */
            MemoryHotSpot[] getMemoryHotSpots();

            interface FunctionHotSpot {
                String getFunctionName();
                long getCallCount();
                long getTotalExecutionTime();
                double getPercentageOfTotalTime();
            }

            interface BranchHotSpot {
                long getInstructionOffset();
                long getBranchCount();
                double getTakenPercentage();
                BranchHintingInstructions.BranchType getBranchType();
            }

            interface MemoryHotSpot {
                long getInstructionOffset();
                long getAllocationCount();
                long getTotalBytesAllocated();
                String getOperationType();
            }
        }
    }

    /**
     * Branch prediction statistics.
     */
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
    static WasmExecutionContext create(final OptimizationLevel optimizationLevel,
                                      final boolean enablePGO) {
        return new DefaultWasmExecutionContext(optimizationLevel, enablePGO);
    }
}

/**
 * Default implementation of WasmExecutionContext.
 */
class DefaultWasmExecutionContext implements WasmExecutionContext {
    private OptimizationLevel optimizationLevel;
    private final boolean profileGuidedOptimizationEnabled;
    private final ExecutionStatisticsImpl statistics;
    private final BranchPredictionStatisticsImpl branchStats;

    DefaultWasmExecutionContext(final OptimizationLevel optimizationLevel,
                               final boolean enablePGO) {
        this.optimizationLevel = optimizationLevel;
        this.profileGuidedOptimizationEnabled = enablePGO;
        this.statistics = new ExecutionStatisticsImpl();
        this.branchStats = new BranchPredictionStatisticsImpl();
    }

    @Override
    public boolean applyBranchHint(final BranchHintingInstructions hint,
                                  final long instructionOffset,
                                  final Object... additionalData) {
        // Record the hint for statistics
        branchStats.recordHint(hint, instructionOffset);

        // In a real implementation, this would communicate with the runtime
        // For now, we just track that the hint was applied
        return true;
    }

    @Override
    public ExecutionStatistics getExecutionStatistics() {
        return statistics;
    }

    @Override
    public boolean isProfileGuidedOptimizationEnabled() {
        return profileGuidedOptimizationEnabled;
    }

    @Override
    public OptimizationLevel getOptimizationLevel() {
        return optimizationLevel;
    }

    @Override
    public void setOptimizationLevel(final OptimizationLevel level) {
        this.optimizationLevel = level;
    }

    @Override
    public BranchPredictionStatistics getBranchPredictionStatistics() {
        return branchStats;
    }

    @Override
    public void resetStatistics() {
        statistics.reset();
        branchStats.reset();
    }

    // Simplified implementation classes
    private static class ExecutionStatisticsImpl implements ExecutionStatistics {
        private long instructionCount = 0;
        private long functionCallCount = 0;
        private long branchCount = 0;
        private long correctBranchPredictions = 0;
        private long executionTimeNanos = 0;

        void reset() {
            instructionCount = 0;
            functionCallCount = 0;
            branchCount = 0;
            correctBranchPredictions = 0;
            executionTimeNanos = 0;
        }

        @Override public long getInstructionCount() { return instructionCount; }
        @Override public long getFunctionCallCount() { return functionCallCount; }
        @Override public long getBranchCount() { return branchCount; }
        @Override public long getCorrectBranchPredictions() { return correctBranchPredictions; }
        @Override public long getExecutionTimeNanos() { return executionTimeNanos; }

        @Override public MemoryStatistics getMemoryStatistics() {
            return new MemoryStatistics() {
                @Override public long getTotalAllocatedBytes() { return 0; }
                @Override public long getCurrentUsageBytes() { return 0; }
                @Override public long getPeakUsageBytes() { return 0; }
                @Override public int getGarbageCollections() { return 0; }
            };
        }

        @Override public HotSpotAnalysis getHotSpotAnalysis() {
            return new HotSpotAnalysis() {
                @Override public FunctionHotSpot[] getHotFunctions() { return new FunctionHotSpot[0]; }
                @Override public BranchHotSpot[] getHotBranches() { return new BranchHotSpot[0]; }
                @Override public MemoryHotSpot[] getMemoryHotSpots() { return new MemoryHotSpot[0]; }
            };
        }
    }

    private static class BranchPredictionStatisticsImpl implements BranchPredictionStatistics {
        private long totalPredictions = 0;
        private long correctPredictions = 0;
        private final java.util.Map<BranchHintingInstructions.BranchType, Long> mispredictions
            = new java.util.HashMap<>();
        private final java.util.Map<Long, BranchHintingInstructions> appliedHints
            = new java.util.HashMap<>();

        void recordHint(final BranchHintingInstructions hint, final long offset) {
            appliedHints.put(offset, hint);
        }

        void reset() {
            totalPredictions = 0;
            correctPredictions = 0;
            mispredictions.clear();
            appliedHints.clear();
        }

        @Override public long getTotalPredictions() { return totalPredictions; }
        @Override public long getCorrectPredictions() { return correctPredictions; }
        @Override public double getAccuracy() {
            return totalPredictions > 0 ? (double) correctPredictions / totalPredictions : 0.0;
        }
        @Override public java.util.Map<BranchHintingInstructions.BranchType, Long> getMispredictionsByType() {
            return new java.util.HashMap<>(mispredictions);
        }
        @Override public MispredictedBranch[] getTopMispredictions() { return new MispredictedBranch[0]; }
    }
}