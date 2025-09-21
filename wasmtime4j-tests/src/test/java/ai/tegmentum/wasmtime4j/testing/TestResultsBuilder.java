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
 * Builder interface for creating TestResults instances.
 *
 * <p>This builder allows for incremental construction of test results by aggregating
 * individual test outcomes and statistics.
 */
public interface TestResultsBuilder {

    /**
     * Adds results from another test execution.
     *
     * @param results the test results to add
     * @return this builder
     */
    TestResultsBuilder addResults(TestResults results);

    /**
     * Adds a successful test.
     *
     * @param testName the test name
     * @param executionTime the execution time
     * @return this builder
     */
    TestResultsBuilder addSuccess(String testName, Duration executionTime);

    /**
     * Adds a failed test.
     *
     * @param failure the test failure
     * @return this builder
     */
    TestResultsBuilder addFailure(TestFailure failure);

    /**
     * Adds a failed test with basic information.
     *
     * @param testName the test name
     * @param errorMessage the error message
     * @param executionTime the execution time
     * @return this builder
     */
    TestResultsBuilder addFailure(String testName, String errorMessage, Duration executionTime);

    /**
     * Adds a skipped test.
     *
     * @param testName the test name
     * @param reason the reason for skipping
     * @return this builder
     */
    TestResultsBuilder addSkipped(String testName, String reason);

    /**
     * Sets the test statistics.
     *
     * @param statistics the test statistics
     * @return this builder
     */
    TestResultsBuilder withStatistics(TestStatistics statistics);

    /**
     * Builds the final TestResults instance.
     *
     * @return the test results
     */
    TestResults build();
}