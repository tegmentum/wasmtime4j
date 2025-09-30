package ai.tegmentum.wasmtime4j.debug;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Result of a multi-target execution operation.
 *
 * <p>This class contains the results of executing an operation across multiple
 * debug targets, including success/failure status and individual target results.
 *
 * @param operationName the name of the operation that was executed
 * @param results the individual target execution results
 * @param startTime the operation start time in milliseconds since epoch
 *
 * @since 1.0.0
 */
public record MultiTargetExecutionResult(
        String operationName,
        List<TargetExecutionResult> results,
        long startTime
) {

    /**
     * Gets the operation duration.
     *
     * @return the duration in milliseconds
     */
    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Gets the number of successful target operations.
     *
     * @return the success count
     */
    public int getSuccessCount() {
        return (int) results.stream()
            .filter(TargetExecutionResult::isSuccess)
            .count();
    }

    /**
     * Gets the number of failed target operations.
     *
     * @return the failure count
     */
    public int getFailureCount() {
        return results.size() - getSuccessCount();
    }

    /**
     * Gets the success rate as a percentage.
     *
     * @return the success rate (0.0 to 1.0)
     */
    public double getSuccessRate() {
        return results.isEmpty() ? 1.0 : (double) getSuccessCount() / results.size();
    }

    /**
     * Checks if all target operations were successful.
     *
     * @return true if all operations succeeded
     */
    public boolean isAllSuccessful() {
        return getFailureCount() == 0;
    }

    /**
     * Checks if any target operations were successful.
     *
     * @return true if at least one operation succeeded
     */
    public boolean isAnySuccessful() {
        return getSuccessCount() > 0;
    }

    /**
     * Gets results grouped by success status.
     *
     * @return map of success status to results
     */
    public Map<Boolean, List<TargetExecutionResult>> getResultsByStatus() {
        return results.stream()
            .collect(Collectors.groupingBy(TargetExecutionResult::isSuccess));
    }

    /**
     * Gets successful target results.
     *
     * @return list of successful results
     */
    public List<TargetExecutionResult> getSuccessfulResults() {
        return results.stream()
            .filter(TargetExecutionResult::isSuccess)
            .collect(Collectors.toList());
    }

    /**
     * Gets failed target results.
     *
     * @return list of failed results
     */
    public List<TargetExecutionResult> getFailedResults() {
        return results.stream()
            .filter(result -> !result.isSuccess())
            .collect(Collectors.toList());
    }

    /**
     * Gets a summary of the execution result.
     *
     * @return execution summary
     */
    public MultiTargetExecutionSummary getSummary() {
        return new MultiTargetExecutionSummary(
            operationName,
            results.size(),
            getSuccessCount(),
            getFailureCount(),
            getSuccessRate(),
            getDuration(),
            startTime
        );
    }

    /**
     * Formats the result as a string.
     *
     * @return formatted result
     */
    public String format() {
        return String.format(
            "%s: %d targets, %d successful, %d failed (%.1f%% success rate), duration: %dms",
            operationName,
            results.size(),
            getSuccessCount(),
            getFailureCount(),
            getSuccessRate() * 100,
            getDuration()
        );
    }

    /**
     * Summary of a multi-target execution.
     *
     * @param operationName the operation name
     * @param totalTargets the total number of targets
     * @param successCount the number of successful operations
     * @param failureCount the number of failed operations
     * @param successRate the success rate (0.0 to 1.0)
     * @param durationMs the duration in milliseconds
     * @param startTime the start time in milliseconds since epoch
     */
    public record MultiTargetExecutionSummary(
            String operationName,
            int totalTargets,
            int successCount,
            int failureCount,
            double successRate,
            long durationMs,
            long startTime
    ) {
        /**
         * Checks if the operation was completely successful.
         *
         * @return true if all targets succeeded
         */
        public boolean isCompleteSuccess() {
            return failureCount == 0;
        }

        /**
         * Checks if the operation was a complete failure.
         *
         * @return true if all targets failed
         */
        public boolean isCompleteFailure() {
            return successCount == 0;
        }

        /**
         * Checks if the operation had partial success.
         *
         * @return true if some but not all targets succeeded
         */
        public boolean isPartialSuccess() {
            return successCount > 0 && failureCount > 0;
        }

        /**
         * Formats the summary as a string.
         *
         * @return formatted summary
         */
        public String format() {
            final String status = isCompleteSuccess() ? "SUCCESS" :
                                  isCompleteFailure() ? "FAILURE" :
                                  "PARTIAL";

            return String.format(
                "[%s] %s: %d/%d targets (%.1f%%) in %dms",
                status,
                operationName,
                successCount,
                totalTargets,
                successRate * 100,
                durationMs
            );
        }
    }
}