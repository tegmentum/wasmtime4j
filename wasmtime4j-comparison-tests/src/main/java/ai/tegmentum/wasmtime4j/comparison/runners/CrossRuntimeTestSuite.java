package ai.tegmentum.wasmtime4j.comparison.runners;

import java.util.List;
import java.util.Objects;

/**
 * Collection of related cross-runtime tests organized as a logical test suite for comprehensive
 * validation across WebAssembly runtime implementations.
 *
 * @since 1.0.0
 */
public final class CrossRuntimeTestSuite {
  private final String name;
  private final String description;
  private final List<CrossRuntimeTest> tests;
  private final TestSuiteCategory category;
  private final boolean critical;

  /**
   * Creates a new cross-runtime test suite with the specified details.
   *
   * @param name the suite name
   * @param description the suite description
   * @param tests the tests in this suite
   * @param category the suite category
   * @param critical whether this suite is critical for production readiness
   */
  public CrossRuntimeTestSuite(
      final String name,
      final String description,
      final List<CrossRuntimeTest> tests,
      final TestSuiteCategory category,
      final boolean critical) {
    this.name = Objects.requireNonNull(name, "name cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.tests = List.copyOf(Objects.requireNonNull(tests, "tests cannot be null"));
    this.category = Objects.requireNonNull(category, "category cannot be null");
    this.critical = critical;
  }

  /**
   * Creates a new functional test suite with default settings.
   *
   * @param name the suite name
   * @param description the suite description
   * @param tests the tests in this suite
   */
  public CrossRuntimeTestSuite(
      final String name, final String description, final List<CrossRuntimeTest> tests) {
    this(name, description, tests, TestSuiteCategory.FUNCTIONAL, true);
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public List<CrossRuntimeTest> getTests() {
    return tests;
  }

  public TestSuiteCategory getCategory() {
    return category;
  }

  public boolean isCritical() {
    return critical;
  }

  /**
   * Gets the number of tests in this suite.
   *
   * @return the test count
   */
  public int getTestCount() {
    return tests.size();
  }

  /**
   * Gets the number of critical tests in this suite.
   *
   * @return the critical test count
   */
  public long getCriticalTestCount() {
    return tests.stream().mapToLong(test -> test.isCritical() ? 1 : 0).sum();
  }

  /**
   * Checks if this suite contains any tests.
   *
   * @return true if the suite has tests
   */
  public boolean hasTests() {
    return !tests.isEmpty();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CrossRuntimeTestSuite that = (CrossRuntimeTestSuite) obj;
    return critical == that.critical
        && Objects.equals(name, that.name)
        && Objects.equals(description, that.description)
        && Objects.equals(tests, that.tests)
        && category == that.category;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, tests, category, critical);
  }

  @Override
  public String toString() {
    return String.format(
        "CrossRuntimeTestSuite{name='%s', category=%s, tests=%d, critical=%s}",
        name, category, tests.size(), critical);
  }

  /** Categories for organizing test suites. */
  public enum TestSuiteCategory {
    FUNCTIONAL,
    PERFORMANCE,
    COMPATIBILITY,
    REGRESSION,
    STRESS,
    INTEGRATION,
    WASI,
    MEMORY_MANAGEMENT,
    ERROR_HANDLING,
    SECURITY
  }
}
