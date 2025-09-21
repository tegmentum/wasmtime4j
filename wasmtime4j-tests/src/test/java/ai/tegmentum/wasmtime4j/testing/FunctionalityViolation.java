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

/**
 * Represents a violation of expected API functionality.
 *
 * <p>This class captures detailed information about functionality violations
 * discovered during comprehensive API testing and validation.
 */
public final class FunctionalityViolation {

    private final String apiName;
    private final String methodName;
    private final FunctionalityViolationType type;
    private final String expectedBehavior;
    private final String actualBehavior;
    private final Severity severity;
    private final String description;

    private FunctionalityViolation(final Builder builder) {
        this.apiName = builder.apiName;
        this.methodName = builder.methodName;
        this.type = builder.type;
        this.expectedBehavior = builder.expectedBehavior;
        this.actualBehavior = builder.actualBehavior;
        this.severity = builder.severity;
        this.description = builder.description;
    }

    /**
     * Creates a new functionality violation builder.
     *
     * @param apiName the API name
     * @param methodName the method name
     * @return a new builder instance
     */
    public static Builder builder(final String apiName, final String methodName) {
        return new Builder(apiName, methodName);
    }

    /**
     * Gets the API name where the violation occurred.
     *
     * @return API name
     */
    public String getApiName() {
        return apiName;
    }

    /**
     * Gets the method name where the violation occurred.
     *
     * @return method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Gets the type of functionality violation.
     *
     * @return violation type
     */
    public FunctionalityViolationType getType() {
        return type;
    }

    /**
     * Gets the expected behavior description.
     *
     * @return expected behavior
     */
    public String getExpectedBehavior() {
        return expectedBehavior;
    }

    /**
     * Gets the actual behavior observed.
     *
     * @return actual behavior
     */
    public String getActualBehavior() {
        return actualBehavior;
    }

    /**
     * Gets the severity of the violation.
     *
     * @return violation severity
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Gets the detailed description of the violation.
     *
     * @return violation description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format(
            "FunctionalityViolation{api='%s', method='%s', type=%s, severity=%s, description='%s'}",
            apiName, methodName, type, severity, description
        );
    }

    /**
     * Types of functionality violations.
     */
    public enum FunctionalityViolationType {
        /** API returns incorrect values */
        INCORRECT_RETURN_VALUE,
        /** API throws unexpected exceptions */
        UNEXPECTED_EXCEPTION,
        /** API has incorrect side effects */
        INCORRECT_SIDE_EFFECTS,
        /** API behavior is inconsistent */
        INCONSISTENT_BEHAVIOR,
        /** API doesn't handle edge cases properly */
        POOR_EDGE_CASE_HANDLING,
        /** API has performance issues */
        PERFORMANCE_ISSUE,
        /** API has memory leaks */
        MEMORY_LEAK,
        /** API violates thread safety */
        THREAD_SAFETY_VIOLATION
    }

    /**
     * Severity levels for functionality violations.
     */
    public enum Severity {
        /** Low impact, cosmetic issues */
        LOW,
        /** Medium impact, functional issues */
        MEDIUM,
        /** High impact, major functional problems */
        HIGH,
        /** Critical impact, system instability */
        CRITICAL
    }

    /**
     * Builder for creating FunctionalityViolation instances.
     */
    public static final class Builder {

        private final String apiName;
        private final String methodName;
        private FunctionalityViolationType type;
        private String expectedBehavior;
        private String actualBehavior;
        private Severity severity = Severity.MEDIUM;
        private String description;

        private Builder(final String apiName, final String methodName) {
            this.apiName = apiName;
            this.methodName = methodName;
        }

        /**
         * Sets the violation type.
         *
         * @param type the violation type
         * @return this builder
         */
        public Builder withType(final FunctionalityViolationType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the expected behavior.
         *
         * @param expectedBehavior the expected behavior
         * @return this builder
         */
        public Builder withExpectedBehavior(final String expectedBehavior) {
            this.expectedBehavior = expectedBehavior;
            return this;
        }

        /**
         * Sets the actual behavior.
         *
         * @param actualBehavior the actual behavior
         * @return this builder
         */
        public Builder withActualBehavior(final String actualBehavior) {
            this.actualBehavior = actualBehavior;
            return this;
        }

        /**
         * Sets the violation severity.
         *
         * @param severity the severity
         * @return this builder
         */
        public Builder withSeverity(final Severity severity) {
            this.severity = severity;
            return this;
        }

        /**
         * Sets the violation description.
         *
         * @param description the description
         * @return this builder
         */
        public Builder withDescription(final String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds the FunctionalityViolation instance.
         *
         * @return the functionality violation
         */
        public FunctionalityViolation build() {
            return new FunctionalityViolation(this);
        }
    }
}