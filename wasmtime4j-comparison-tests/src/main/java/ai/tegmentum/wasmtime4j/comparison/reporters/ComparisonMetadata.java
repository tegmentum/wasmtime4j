package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Metadata about the comparison report and execution environment.
 *
 * @since 1.0.0
 */
public final class ComparisonMetadata {
  private final String testSuiteName;
  private final String testSuiteVersion;
  private final Set<RuntimeType> runtimeTypes;
  private final Map<String, String> environmentInfo;
  private final String wasmtime4jVersion;

  /**
   * Constructs a new ComparisonMetadata with the specified metadata information.
   *
   * @param testSuiteName the name of the test suite
   * @param testSuiteVersion the version of the test suite
   * @param runtimeTypes the set of runtime types tested
   * @param environmentInfo environmental information about the test execution
   * @param wasmtime4jVersion the version of wasmtime4j used
   */
  public ComparisonMetadata(
      final String testSuiteName,
      final String testSuiteVersion,
      final Set<RuntimeType> runtimeTypes,
      final Map<String, String> environmentInfo,
      final String wasmtime4jVersion) {
    this.testSuiteName = Objects.requireNonNull(testSuiteName, "testSuiteName cannot be null");
    this.testSuiteVersion =
        Objects.requireNonNull(testSuiteVersion, "testSuiteVersion cannot be null");
    this.runtimeTypes = Set.copyOf(runtimeTypes);
    this.environmentInfo = Map.copyOf(environmentInfo);
    this.wasmtime4jVersion =
        Objects.requireNonNull(wasmtime4jVersion, "wasmtime4jVersion cannot be null");
  }

  public String getTestSuiteName() {
    return testSuiteName;
  }

  /**
   * Gets the test suite name (alias for getTestSuiteName).
   *
   * @return test suite name
   */
  public String getTestSuite() {
    return testSuiteName;
  }

  public String getTestSuiteVersion() {
    return testSuiteVersion;
  }

  public Set<RuntimeType> getRuntimeTypes() {
    return runtimeTypes;
  }

  public Map<String, String> getEnvironmentInfo() {
    return environmentInfo;
  }

  public String getWasmtime4jVersion() {
    return wasmtime4jVersion;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ComparisonMetadata that = (ComparisonMetadata) obj;
    return Objects.equals(testSuiteName, that.testSuiteName)
        && Objects.equals(testSuiteVersion, that.testSuiteVersion)
        && Objects.equals(runtimeTypes, that.runtimeTypes)
        && Objects.equals(environmentInfo, that.environmentInfo)
        && Objects.equals(wasmtime4jVersion, that.wasmtime4jVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        testSuiteName, testSuiteVersion, runtimeTypes, environmentInfo, wasmtime4jVersion);
  }

  @Override
  public String toString() {
    return "ComparisonMetadata{"
        + "testSuite='"
        + testSuiteName
        + '\''
        + ", version='"
        + testSuiteVersion
        + '\''
        + ", runtimes="
        + runtimeTypes
        + '}';
  }
}
