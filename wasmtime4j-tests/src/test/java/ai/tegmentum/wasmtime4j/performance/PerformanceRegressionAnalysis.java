package ai.tegmentum.wasmtime4j.performance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Analysis of performance regressions across test runs. */
public final class PerformanceRegressionAnalysis {
  private final String testName;
  private final List<PerformanceRegression> regressions;
  private final List<String> warnings;
  private final List<String> info;

  /**
   * Creates a new performance regression analysis from a builder.
   *
   * @param builder the builder containing analysis data
   */
  private PerformanceRegressionAnalysis(final Builder builder) {
    this.testName = Objects.requireNonNull(builder.testName);
    this.regressions = Collections.unmodifiableList(new ArrayList<>(builder.regressions));
    this.warnings = Collections.unmodifiableList(new ArrayList<>(builder.warnings));
    this.info = Collections.unmodifiableList(new ArrayList<>(builder.info));
  }

  public String getTestName() {
    return testName;
  }

  public List<PerformanceRegression> getRegressions() {
    return regressions;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public List<String> getInfo() {
    return info;
  }

  public boolean hasRegressions() {
    return !regressions.isEmpty();
  }

  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Gets a summary of the regression analysis.
   *
   * @return a summary string describing any detected regressions
   */
  public String getSummary() {
    if (hasRegressions()) {
      return "REGRESSION DETECTED - " + regressions.size() + " regression(s) found for " + testName;
    } else {
      return "NO REGRESSIONS - " + testName + " performance within acceptable range";
    }
  }

  /** Builder for creating performance regression analysis instances. */
  public static final class Builder {
    private final String testName;
    private final List<PerformanceRegression> regressions = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> info = new ArrayList<>();

    public Builder(final String testName) {
      this.testName = Objects.requireNonNull(testName);
    }

    public Builder addRegression(final PerformanceRegression regression) {
      regressions.add(Objects.requireNonNull(regression));
      return this;
    }

    public Builder addWarning(final String warning) {
      warnings.add(Objects.requireNonNull(warning));
      return this;
    }

    public Builder addInfo(final String info) {
      this.info.add(Objects.requireNonNull(info));
      return this;
    }

    public PerformanceRegressionAnalysis build() {
      return new PerformanceRegressionAnalysis(this);
    }
  }
}
