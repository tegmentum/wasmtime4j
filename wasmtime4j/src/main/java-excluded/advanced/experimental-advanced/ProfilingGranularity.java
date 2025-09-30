package ai.tegmentum.wasmtime4j.experimental;

/**
 * Granularity levels for experimental profiling and performance analysis.
 *
 * <p>These granularity levels control the detail level of performance data collection
 * in experimental profiling features.
 *
 * @since 1.0.0
 */
public enum ProfilingGranularity {

    /**
     * Module-level profiling granularity.
     * Collects performance data at the WebAssembly module level.
     * Lowest overhead but least detailed information.
     */
    MODULE("module", "Module-level profiling", 0),

    /**
     * Function-level profiling granularity.
     * Collects performance data for each WebAssembly function.
     * Balanced overhead and detail level.
     */
    FUNCTION("function", "Function-level profiling", 1),

    /**
     * Basic block-level profiling granularity.
     * Collects performance data for each basic block within functions.
     * Higher overhead but more detailed information.
     */
    BASIC_BLOCK("basic-block", "Basic block-level profiling", 2),

    /**
     * Instruction-level profiling granularity.
     * Collects performance data for individual WebAssembly instructions.
     * Highest overhead but most detailed information.
     */
    INSTRUCTION("instruction", "Instruction-level profiling", 3);

    private final String key;
    private final String description;
    private final int level;

    ProfilingGranularity(final String key, final String description, final int level) {
        this.key = key;
        this.description = description;
        this.level = level;
    }

    /**
     * Gets the unique key identifier for this granularity level.
     *
     * @return the granularity key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the human-readable description of this granularity level.
     *
     * @return the granularity description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the numeric level value for comparison.
     *
     * @return the numeric granularity level (higher = more detailed)
     */
    public int getLevel() {
        return level;
    }

    /**
     * Checks if this granularity level is at least as detailed as the specified level.
     *
     * @param other the granularity level to compare against
     * @return true if this level is at least as detailed as the other level
     */
    public boolean isAtLeast(final ProfilingGranularity other) {
        return this.level >= other.level;
    }

    /**
     * Checks if this granularity level is more detailed than the specified level.
     *
     * @param other the granularity level to compare against
     * @return true if this level is more detailed than the other level
     */
    public boolean isMoreDetailedThan(final ProfilingGranularity other) {
        return this.level > other.level;
    }

    @Override
    public String toString() {
        return key + " (" + description + ")";
    }
}