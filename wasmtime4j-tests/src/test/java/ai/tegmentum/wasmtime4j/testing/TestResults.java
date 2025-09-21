/*
 * Copyright 2024 Tegmentum AI Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.testing;

import java.time.Duration;
import java.util.List;

/**
 * Test execution results providing comprehensive information about test outcomes.
 *
 * <p>This interface aggregates test execution information including pass/fail counts,
 * execution times, and detailed failure information for analysis and reporting.
 */
public interface TestResults {

    /**
     * Creates a new test results builder.
     *
     * @return a new builder instance
     */
    static TestResultsBuilder builder() {
        return new DefaultTestResultsBuilder();
    }

    /**
     * Gets the total number of tests executed.
     *
     * @return total test count
     */
    int getTotalTests();

    /**
     * Gets the number of tests that passed.
     *
     * @return passed test count
     */
    int getPassedTests();

    /**
     * Gets the number of tests that failed.
     *
     * @return failed test count
     */
    int getFailedTests();

    /**
     * Gets the number of tests that were skipped.
     *
     * @return skipped test count
     */
    int getSkippedTests();

    /**
     * Gets detailed information about test failures.
     *
     * @return list of test failures
     */
    List<TestFailure> getFailures();

    /**
     * Gets the total execution time for all tests.
     *
     * @return total execution duration
     */
    Duration getTotalExecutionTime();

    /**
     * Gets statistical information about test execution.
     *
     * @return test statistics
     */
    TestStatistics getStatistics();

    /**
     * Gets the test success rate as a percentage.
     *
     * @return success rate from 0.0 to 100.0
     */
    default double getSuccessRate() {
        if (getTotalTests() == 0) {
            return 0.0;
        }
        return (double) getPassedTests() / getTotalTests() * 100.0;
    }

    /**
     * Checks if all tests passed.
     *
     * @return true if all tests passed
     */
    default boolean allTestsPassed() {
        return getFailedTests() == 0 && getTotalTests() > 0;
    }

    /**
     * Checks if any tests failed.
     *
     * @return true if at least one test failed
     */
    default boolean hasFailures() {
        return getFailedTests() > 0;
    }

    /**
     * Gets a summary of the test results.
     *
     * @return human-readable test summary
     */
    default String getSummary() {
        return String.format(
            "Tests: %d total, %d passed, %d failed, %d skipped (%.2f%% success rate)",
            getTotalTests(),
            getPassedTests(),
            getFailedTests(),
            getSkippedTests(),
            getSuccessRate()
        );
    }

    /**
     * Gets the average test execution time.
     *
     * @return average execution time per test
     */
    default Duration getAverageExecutionTime() {
        if (getTotalTests() == 0) {
            return Duration.ZERO;
        }
        return getTotalExecutionTime().dividedBy(getTotalTests());
    }
}