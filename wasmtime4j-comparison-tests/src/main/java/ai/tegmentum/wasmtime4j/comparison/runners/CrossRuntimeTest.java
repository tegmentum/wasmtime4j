package ai.tegmentum.wasmtime4j.comparison.runners;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.Objects;

/**
 * Interface for cross-runtime tests that can be executed across multiple WebAssembly runtime
 * implementations to validate behavioral consistency and functional equivalence.
 *
 * @since 1.0.0
 */
public interface CrossRuntimeTest {

  /**
   * Gets the unique name of this test.
   *
   * @return the test name
   */
  String getName();

  /**
   * Gets a description of what this test validates.
   *
   * @return the test description
   */
  String getDescription();

  /**
   * Executes the test on the specified runtime.
   *
   * @param runtime the runtime to execute the test on
   * @return the test result
   * @throws Exception if the test execution fails
   */
  Object execute(RuntimeType runtime) throws Exception;

  /**
   * Gets the expected test category for classification purposes.
   *
   * @return the test category
   */
  default TestCategory getCategory() {
    return TestCategory.FUNCTIONAL;
  }

  /**
   * Checks if this test is critical for production readiness.
   *
   * @return true if this is a critical test
   */
  default boolean isCritical() {
    return true;
  }

  /** Test categories for organizational purposes. */
  enum TestCategory {
    FUNCTIONAL,
    PERFORMANCE,
    MEMORY,
    COMPATIBILITY,
    REGRESSION,
    STRESS
  }

  /** Abstract base implementation providing common functionality. */
  abstract class AbstractCrossRuntimeTest implements CrossRuntimeTest {
    private final String name;
    private final String description;
    private final TestCategory category;
    private final boolean critical;

    protected AbstractCrossRuntimeTest(
        final String name,
        final String description,
        final TestCategory category,
        final boolean critical) {
      this.name = Objects.requireNonNull(name, "name cannot be null");
      this.description = Objects.requireNonNull(description, "description cannot be null");
      this.category = Objects.requireNonNull(category, "category cannot be null");
      this.critical = critical;
    }

    protected AbstractCrossRuntimeTest(final String name, final String description) {
      this(name, description, TestCategory.FUNCTIONAL, true);
    }

    @Override
    public final String getName() {
      return name;
    }

    @Override
    public final String getDescription() {
      return description;
    }

    @Override
    public final TestCategory getCategory() {
      return category;
    }

    @Override
    public final boolean isCritical() {
      return critical;
    }

    @Override
    public String toString() {
      return String.format(
          "CrossRuntimeTest{name='%s', category=%s, critical=%s}", name, category, critical);
    }
  }
}
