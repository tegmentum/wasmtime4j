package ai.tegmentum.wasmtime4j.wasi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Summary of WASI integration test execution results. */
public final class WasiExecutionSummary {
  private final int totalWasiTests;
  private final int wasiCompliantTests;
  private final int successfulTests;
  private final int failedTests;
  private final List<WasiTestResult> allResults;
  private final Instant summaryTime;

  private WasiExecutionSummary(final Builder builder) {
    this.totalWasiTests = builder.totalWasiTests;
    this.wasiCompliantTests = builder.wasiCompliantTests;
    this.successfulTests = builder.successfulTests;
    this.failedTests = builder.failedTests;
    this.allResults = Collections.unmodifiableList(new ArrayList<>(builder.allResults));
    this.summaryTime = builder.summaryTime;
  }

  /**
   * Gets the total number of WASI tests executed.
   *
   * @return the total WASI test count
   */
  public int getTotalWasiTests() {
    return totalWasiTests;
  }

  /**
   * Gets the number of WASI-compliant tests.
   *
   * @return the WASI-compliant test count
   */
  public int getWasiCompliantTests() {
    return wasiCompliantTests;
  }

  /**
   * Gets the number of successful tests.
   *
   * @return the successful test count
   */
  public int getSuccessfulTests() {
    return successfulTests;
  }

  /**
   * Gets the number of failed tests.
   *
   * @return the failed test count
   */
  public int getFailedTests() {
    return failedTests;
  }

  /**
   * Gets all WASI test results.
   *
   * @return the list of all results
   */
  public List<WasiTestResult> getAllResults() {
    return allResults;
  }

  /**
   * Gets the summary creation time.
   *
   * @return the summary time
   */
  public Instant getSummaryTime() {
    return summaryTime;
  }

  /**
   * Gets the WASI compliance rate.
   *
   * @return the compliance rate as a percentage
   */
  public double getWasiComplianceRate() {
    if (totalWasiTests == 0) {
      return 0.0;
    }
    return (double) wasiCompliantTests / totalWasiTests * 100.0;
  }

  /**
   * Gets the overall success rate.
   *
   * @return the success rate as a percentage
   */
  public double getSuccessRate() {
    if (totalWasiTests == 0) {
      return 0.0;
    }
    return (double) successfulTests / totalWasiTests * 100.0;
  }

  /**
   * Checks if all tests were WASI compliant.
   *
   * @return true if all tests are WASI compliant
   */
  public boolean isFullyWasiCompliant() {
    return totalWasiTests > 0 && wasiCompliantTests == totalWasiTests;
  }

  /**
   * Gets the failed test results.
   *
   * @return list of failed test results
   */
  public List<WasiTestResult> getFailedResults() {
    return allResults.stream().filter(result -> !result.isSuccessful()).toList();
  }

  /**
   * Gets the non-WASI-compliant test results.
   *
   * @return list of non-compliant test results
   */
  public List<WasiTestResult> getNonCompliantResults() {
    return allResults.stream().filter(result -> !result.isWasiCompliant()).toList();
  }

  /**
   * Creates a formatted report.
   *
   * @return the formatted report
   */
  public String createReport() {
    final StringBuilder sb = new StringBuilder();
    sb.append("WASI Integration Test Execution Summary\n");
    sb.append("======================================\n\n");

    sb.append("Summary Time: ").append(summaryTime).append('\n');
    sb.append("Total WASI Tests: ").append(totalWasiTests).append('\n');
    sb.append("WASI Compliant: ").append(wasiCompliantTests).append('\n');
    sb.append("Successful: ").append(successfulTests).append('\n');
    sb.append("Failed: ").append(failedTests).append('\n');
    sb.append('\n');

    sb.append("WASI Compliance Rate: ")
        .append(String.format("%.1f%%", getWasiComplianceRate()))
        .append('\n');
    sb.append("Overall Success Rate: ")
        .append(String.format("%.1f%%", getSuccessRate()))
        .append('\n');
    sb.append('\n');

    // Failed tests details
    final List<WasiTestResult> failedResults = getFailedResults();
    if (!failedResults.isEmpty()) {
      sb.append("Failed Tests:\n");
      for (final WasiTestResult failedResult : failedResults) {
        sb.append("  - ").append(failedResult.getTestName());
        if (failedResult.hasErrors()) {
          sb.append(" (").append(failedResult.getErrors().size()).append(" errors)");
        }
        sb.append('\n');
      }
      sb.append('\n');
    }

    // Non-compliant tests details
    final List<WasiTestResult> nonCompliantResults = getNonCompliantResults();
    if (!nonCompliantResults.isEmpty()) {
      sb.append("Non-WASI-Compliant Tests:\n");
      for (final WasiTestResult nonCompliantResult : nonCompliantResults) {
        sb.append("  - ").append(nonCompliantResult.getTestName()).append('\n');
      }
      sb.append('\n');
    }

    return sb.toString();
  }

  @Override
  public String toString() {
    return "WasiExecutionSummary{"
        + "totalTests="
        + totalWasiTests
        + ", wasiCompliant="
        + wasiCompliantTests
        + ", successful="
        + successfulTests
        + ", failed="
        + failedTests
        + ", complianceRate="
        + String.format("%.1f%%", getWasiComplianceRate())
        + ", successRate="
        + String.format("%.1f%%", getSuccessRate())
        + '}';
  }

  /** Builder for creating WASI execution summaries. */
  public static final class Builder {
    private int totalWasiTests = 0;
    private int wasiCompliantTests = 0;
    private int successfulTests = 0;
    private int failedTests = 0;
    private final List<WasiTestResult> allResults = new ArrayList<>();
    private final Instant summaryTime;

    public Builder() {
      this.summaryTime = Instant.now();
    }

    /**
     * Adds a WASI test result.
     *
     * @param result the WASI test result
     * @return this builder
     */
    public Builder addWasiTestResult(final WasiTestResult result) {
      totalWasiTests++;
      allResults.add(result);

      if (result.isWasiCompliant()) {
        wasiCompliantTests++;
      }

      if (result.isSuccessful()) {
        successfulTests++;
      } else {
        failedTests++;
      }

      return this;
    }

    /**
     * Builds the WASI execution summary.
     *
     * @return the summary
     */
    public WasiExecutionSummary build() {
      return new WasiExecutionSummary(this);
    }
  }
}
