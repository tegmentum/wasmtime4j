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
 * Default implementation of FunctionalityReport.
 *
 * <p>Provides comprehensive functionality validation results including API validation scores,
 * detailed test results, and functionality violations.
 */
final class DefaultFunctionalityReport implements FunctionalityReport {

    private final double functionalityScore;
    private final Map<String, Double> functionalityByCategory;
    private final List<String> validatedApis;
    private final List<String> failedApis;
    private final Map<String, TestResults> detailedResults;
    private final List<FunctionalityViolation> violations;

    DefaultFunctionalityReport(
            final double functionalityScore,
            final Map<String, Double> functionalityByCategory,
            final List<String> validatedApis,
            final List<String> failedApis,
            final Map<String, TestResults> detailedResults,
            final List<FunctionalityViolation> violations) {
        this.functionalityScore = functionalityScore;
        this.functionalityByCategory = new java.util.HashMap<>(functionalityByCategory);
        this.validatedApis = new java.util.ArrayList<>(validatedApis);
        this.failedApis = new java.util.ArrayList<>(failedApis);
        this.detailedResults = new java.util.HashMap<>(detailedResults);
        this.violations = new java.util.ArrayList<>(violations);
    }

    @Override
    public double getFunctionalityScore() {
        return functionalityScore;
    }

    @Override
    public Map<String, Double> getFunctionalityByCategory() {
        return new java.util.HashMap<>(functionalityByCategory);
    }

    @Override
    public List<String> getValidatedApis() {
        return new java.util.ArrayList<>(validatedApis);
    }

    @Override
    public List<String> getFailedApis() {
        return new java.util.ArrayList<>(failedApis);
    }

    @Override
    public Map<String, TestResults> getDetailedResults() {
        return new java.util.HashMap<>(detailedResults);
    }

    @Override
    public List<FunctionalityViolation> getViolations() {
        return new java.util.ArrayList<>(violations);
    }

    @Override
    public String toString() {
        return String.format(
            "FunctionalityReport{score=%.2f%%, validatedApis=%d, failedApis=%d, violations=%d}",
            functionalityScore, validatedApis.size(), failedApis.size(), violations.size()
        );
    }
}