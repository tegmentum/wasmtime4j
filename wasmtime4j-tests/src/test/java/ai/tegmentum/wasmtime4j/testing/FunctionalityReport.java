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

import java.util.List;
import java.util.Map;

/**
 * Report of API functionality validation results.
 *
 * <p>This report provides comprehensive information about the functional correctness
 * of implemented APIs, including behavioral validation and runtime testing results.
 */
public interface FunctionalityReport {

    /**
     * Gets the overall functionality validation score.
     *
     * @return validation score from 0.0 to 100.0
     */
    double getFunctionalityScore();

    /**
     * Gets functionality results broken down by API category.
     *
     * @return map of API categories to their functionality scores
     */
    Map<String, Double> getFunctionalityByCategory();

    /**
     * Gets list of APIs that passed all functionality tests.
     *
     * @return list of validated API names
     */
    List<String> getValidatedApis();

    /**
     * Gets list of APIs that failed functionality tests.
     *
     * @return list of failed API names
     */
    List<String> getFailedApis();

    /**
     * Gets detailed functionality test results for each API.
     *
     * @return map of API names to their test results
     */
    Map<String, TestResults> getDetailedResults();

    /**
     * Gets list of functionality violations found during testing.
     *
     * @return list of functionality violations
     */
    List<FunctionalityViolation> getViolations();

    /**
     * Checks if all APIs pass functionality validation.
     *
     * @return true if all APIs are functionally correct
     */
    default boolean isFullyFunctional() {
        return getFunctionalityScore() >= 100.0 && getFailedApis().isEmpty();
    }

    /**
     * Gets a summary of the functionality validation.
     *
     * @return human-readable functionality summary
     */
    default String getSummary() {
        return String.format(
            "Functionality: %.2f%% (%d APIs validated, %d failed, %d violations)",
            getFunctionalityScore(),
            getValidatedApis().size(),
            getFailedApis().size(),
            getViolations().size()
        );
    }
}