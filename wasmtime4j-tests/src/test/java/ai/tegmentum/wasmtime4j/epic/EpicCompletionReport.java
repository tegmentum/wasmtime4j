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

package ai.tegmentum.wasmtime4j.epic;

import java.util.List;

/**
 * Represents the comprehensive epic completion report containing all validation results
 * and metrics for the 100% Wasmtime API coverage epic.
 */
public interface EpicCompletionReport {

    /**
     * Gets the API coverage percentage.
     *
     * @return percentage of Wasmtime APIs implemented (0.0 to 100.0)
     */
    double getApiCoveragePercentage();

    /**
     * Checks if full parity is achieved between JNI and Panama implementations.
     *
     * @return true if both implementations have identical functionality
     */
    boolean isParityAchieved();

    /**
     * Checks if all tests are currently passing.
     *
     * @return true if all tests pass
     */
    boolean areAllTestsPassing();

    /**
     * Checks if documentation is complete for all APIs.
     *
     * @return true if all APIs are documented
     */
    boolean isDocumentationComplete();

    /**
     * Gets list of completion criteria that have not been met.
     *
     * @return list of unmet criteria
     */
    List<EpicCompletionValidator.CompletionCriteria> getUnmetCriteria();

    /**
     * Gets list of validation errors encountered during epic validation.
     *
     * @return list of validation error messages
     */
    List<String> getValidationErrors();

    /**
     * Checks if the epic is fully complete based on all criteria.
     *
     * @return true if epic meets all completion requirements
     */
    default boolean isEpicComplete() {
        return getApiCoveragePercentage() >= 100.0
                && isParityAchieved()
                && areAllTestsPassing()
                && isDocumentationComplete()
                && getUnmetCriteria().isEmpty();
    }

    /**
     * Gets a summary of the completion status.
     *
     * @return human-readable completion summary
     */
    default String getCompletionSummary() {
        if (isEpicComplete()) {
            return "Epic Complete: All criteria met, 100% API coverage achieved";
        }

        final StringBuilder summary = new StringBuilder("Epic Incomplete: ");
        if (getApiCoveragePercentage() < 100.0) {
            summary.append(String.format("API Coverage %.2f%% ", getApiCoveragePercentage()));
        }
        if (!isParityAchieved()) {
            summary.append("Parity Missing ");
        }
        if (!areAllTestsPassing()) {
            summary.append("Tests Failing ");
        }
        if (!isDocumentationComplete()) {
            summary.append("Docs Incomplete ");
        }
        if (!getUnmetCriteria().isEmpty()) {
            summary.append(String.format("%d Unmet Criteria ", getUnmetCriteria().size()));
        }

        return summary.toString().trim();
    }
}