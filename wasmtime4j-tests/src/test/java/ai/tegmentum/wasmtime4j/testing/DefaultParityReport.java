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
import java.util.stream.Collectors;

/**
 * Default implementation of ParityReport.
 *
 * <p>Provides comprehensive parity analysis between JNI and Panama implementations
 * including violation details and missing implementation tracking.
 */
final class DefaultParityReport implements ParityReport {

    private final double parityPercentage;
    private final int totalApisTested;
    private final int violationCount;
    private final List<ParityViolation> violations;
    private final List<String> missingJniApis;
    private final List<String> missingPanamaApis;

    DefaultParityReport(
            final double parityPercentage,
            final int totalApisTested,
            final int violationCount,
            final List<ParityViolation> violations,
            final List<String> missingJniApis,
            final List<String> missingPanamaApis) {
        this.parityPercentage = parityPercentage;
        this.totalApisTested = totalApisTested;
        this.violationCount = violationCount;
        this.violations = new java.util.ArrayList<>(violations);
        this.missingJniApis = new java.util.ArrayList<>(missingJniApis);
        this.missingPanamaApis = new java.util.ArrayList<>(missingPanamaApis);
    }

    @Override
    public double getParityPercentage() {
        return parityPercentage;
    }

    @Override
    public int getTotalApisTested() {
        return totalApisTested;
    }

    @Override
    public int getViolationCount() {
        return violationCount;
    }

    @Override
    public List<ParityViolation> getViolations() {
        return new java.util.ArrayList<>(violations);
    }

    @Override
    public List<ParityViolation> getViolationsBySeverity(final ParityViolation.Severity severity) {
        return violations.stream()
            .filter(violation -> violation.getSeverity() == severity)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getMissingJniApis() {
        return new java.util.ArrayList<>(missingJniApis);
    }

    @Override
    public List<String> getMissingPanamaApis() {
        return new java.util.ArrayList<>(missingPanamaApis);
    }

    @Override
    public String toString() {
        return String.format(
            "ParityReport{parity=%.2f%%, tested=%d, violations=%d, missingJni=%d, missingPanama=%d}",
            parityPercentage, totalApisTested, violationCount, missingJniApis.size(), missingPanamaApis.size()
        );
    }
}