package ai.tegmentum.wasmtime4j.testsuite;

/**
 * Results from CI test execution with CI-specific metadata.
 */
public final class CiExecutionResults {

    private final TestExecutionResults executionResults;
    private final TestAnalysisReport analysisReport;
    private final long totalDurationMs;
    private final String ciEnvironment;
    private final int exitCode;

    private CiExecutionResults(final Builder builder) {
        this.executionResults = builder.executionResults;
        this.analysisReport = builder.analysisReport;
        this.totalDurationMs = builder.totalDurationMs;
        this.ciEnvironment = builder.ciEnvironment;
        this.exitCode = builder.exitCode;
    }

    // Getters
    public TestExecutionResults getExecutionResults() { return executionResults; }
    public TestAnalysisReport getAnalysisReport() { return analysisReport; }
    public long getTotalDurationMs() { return totalDurationMs; }
    public String getCiEnvironment() { return ciEnvironment; }
    public int getExitCode() { return exitCode; }

    /**
     * Gets a summary string for CI output.
     *
     * @return summary string
     */
    public String getSummary() {
        return String.format("CI Test Suite: %d tests, %d passed, %d failed, %.1f%% pass rate, %d ms, exit code %d",
                executionResults.getTotalTestCount(),
                executionResults.getTotalPassedCount(),
                executionResults.getTotalFailedCount(),
                executionResults.getPassRate(),
                totalDurationMs,
                exitCode);
    }

    /**
     * Creates a new builder for CiExecutionResults.
     *
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for CiExecutionResults.
     */
    public static final class Builder {
        private TestExecutionResults executionResults;
        private TestAnalysisReport analysisReport;
        private long totalDurationMs;
        private String ciEnvironment = "Unknown";
        private int exitCode;

        public Builder executionResults(final TestExecutionResults executionResults) {
            this.executionResults = executionResults;
            return this;
        }

        public Builder analysisReport(final TestAnalysisReport analysisReport) {
            this.analysisReport = analysisReport;
            return this;
        }

        public Builder totalDurationMs(final long totalDurationMs) {
            this.totalDurationMs = totalDurationMs;
            return this;
        }

        public Builder ciEnvironment(final String ciEnvironment) {
            this.ciEnvironment = ciEnvironment != null ? ciEnvironment : "Unknown";
            return this;
        }

        public Builder exitCode(final int exitCode) {
            this.exitCode = exitCode;
            return this;
        }

        public CiExecutionResults build() {
            if (executionResults == null) {
                throw new IllegalStateException("Execution results must be set");
            }
            return new CiExecutionResults(this);
        }
    }
}