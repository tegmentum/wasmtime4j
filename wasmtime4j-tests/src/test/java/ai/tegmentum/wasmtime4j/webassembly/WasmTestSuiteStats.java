package ai.tegmentum.wasmtime4j.webassembly;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Statistics about WebAssembly test suites.
 */
public final class WasmTestSuiteStats {
    private final Map<WasmTestSuiteLoader.TestSuiteType, Integer> testCounts;
    
    /**
     * Creates a new test suite statistics object.
     */
    public WasmTestSuiteStats() {
        this.testCounts = new EnumMap<>(WasmTestSuiteLoader.TestSuiteType.class);
        
        // Initialize all counts to zero
        for (final WasmTestSuiteLoader.TestSuiteType suiteType : WasmTestSuiteLoader.TestSuiteType.values()) {
            testCounts.put(suiteType, 0);
        }
    }
    
    /**
     * Adds statistics for a test suite.
     *
     * @param suiteType the test suite type
     * @param testCount the number of test cases in the suite
     */
    public void addSuiteStats(final WasmTestSuiteLoader.TestSuiteType suiteType, final int testCount) {
        Objects.requireNonNull(suiteType, "suiteType cannot be null");
        if (testCount < 0) {
            throw new IllegalArgumentException("testCount cannot be negative");
        }
        
        testCounts.put(suiteType, testCount);
    }
    
    /**
     * Gets the number of test cases for a specific suite type.
     *
     * @param suiteType the test suite type
     * @return the number of test cases
     */
    public int getTestCount(final WasmTestSuiteLoader.TestSuiteType suiteType) {
        return testCounts.getOrDefault(suiteType, 0);
    }
    
    /**
     * Gets the total number of test cases across all suites.
     *
     * @return the total number of test cases
     */
    public int getTotalTestCount() {
        return testCounts.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
    }
    
    /**
     * Gets the number of WebAssembly specification test cases.
     *
     * @return the number of spec test cases
     */
    public int getSpecTestCount() {
        return getTestCount(WasmTestSuiteLoader.TestSuiteType.WEBASSEMBLY_SPEC);
    }
    
    /**
     * Gets the number of Wasmtime-specific test cases.
     *
     * @return the number of Wasmtime test cases
     */
    public int getWasmtimeTestCount() {
        return getTestCount(WasmTestSuiteLoader.TestSuiteType.WASMTIME_TESTS);
    }
    
    /**
     * Gets the number of WASI test cases.
     *
     * @return the number of WASI test cases
     */
    public int getWasiTestCount() {
        return getTestCount(WasmTestSuiteLoader.TestSuiteType.WASI_TESTS);
    }
    
    /**
     * Gets the number of custom test cases.
     *
     * @return the number of custom test cases
     */
    public int getCustomTestCount() {
        return getTestCount(WasmTestSuiteLoader.TestSuiteType.CUSTOM_TESTS);
    }
    
    /**
     * Checks if any test suites are available.
     *
     * @return true if at least one test suite has test cases
     */
    public boolean hasAnyTests() {
        return getTotalTestCount() > 0;
    }
    
    /**
     * Gets a breakdown of test counts by suite type.
     *
     * @return a map of suite types to test counts
     */
    public Map<WasmTestSuiteLoader.TestSuiteType, Integer> getTestCountBreakdown() {
        return new EnumMap<>(testCounts);
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("WasmTestSuiteStats{\n");
        
        for (final Map.Entry<WasmTestSuiteLoader.TestSuiteType, Integer> entry : testCounts.entrySet()) {
            sb.append("  ")
              .append(entry.getKey().name())
              .append(": ")
              .append(entry.getValue())
              .append(" tests\n");
        }
        
        sb.append("  TOTAL: ").append(getTotalTestCount()).append(" tests\n");
        sb.append("}");
        
        return sb.toString();
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        final WasmTestSuiteStats that = (WasmTestSuiteStats) obj;
        return Objects.equals(testCounts, that.testCounts);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(testCounts);
    }
}