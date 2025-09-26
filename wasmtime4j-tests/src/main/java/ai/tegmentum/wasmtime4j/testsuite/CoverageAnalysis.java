package ai.tegmentum.wasmtime4j.testsuite;

import java.util.List;
import java.util.Map;

/**
 * Analysis results for test coverage across WebAssembly features and categories.
 */
public final class CoverageAnalysis {

    private final Map<TestCategory, Integer> testCountsByCategory;
    private final List<String> coverageGaps;

    public CoverageAnalysis(final Map<TestCategory, Integer> testCountsByCategory,
                           final List<String> coverageGaps) {
        this.testCountsByCategory = Map.copyOf(testCountsByCategory);
        this.coverageGaps = List.copyOf(coverageGaps);
    }

    public Map<TestCategory, Integer> getTestCountsByCategory() { return testCountsByCategory; }
    public List<String> getCoverageGaps() { return coverageGaps; }

    /**
     * Checks if there are coverage gaps.
     *
     * @return true if coverage gaps exist
     */
    public boolean hasCoverageGaps() {
        return !coverageGaps.isEmpty();
    }

    /**
     * Gets total number of test categories covered.
     *
     * @return number of covered categories
     */
    public int getCoveredCategoriesCount() {
        return testCountsByCategory.size();
    }

    /**
     * Gets total number of tests across all categories.
     *
     * @return total test count
     */
    public int getTotalTestCount() {
        return testCountsByCategory.values().stream().mapToInt(Integer::intValue).sum();
    }
}