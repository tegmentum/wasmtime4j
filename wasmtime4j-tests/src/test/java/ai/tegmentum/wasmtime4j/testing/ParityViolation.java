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
 * Represents a parity violation between JNI and Panama implementations.
 *
 * <p>This class captures detailed information about inconsistencies between
 * implementation backends that could affect application behavior.
 */
public final class ParityViolation {

    private final String apiName;
    private final String methodName;
    private final ParityViolationType type;
    private final String jniResult;
    private final String panamaResult;
    private final String description;
    private final Severity severity;

    private ParityViolation(final Builder builder) {
        this.apiName = builder.apiName;
        this.methodName = builder.methodName;
        this.type = builder.type;
        this.jniResult = builder.jniResult;
        this.panamaResult = builder.panamaResult;
        this.description = builder.description;
        this.severity = builder.severity;
    }

    /**
     * Creates a new parity violation builder.
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
     * Gets the type of parity violation.
     *
     * @return violation type
     */
    public ParityViolationType getType() {
        return type;
    }

    /**
     * Gets the JNI implementation result.
     *
     * @return JNI result description
     */
    public String getJniResult() {
        return jniResult;
    }

    /**
     * Gets the Panama implementation result.
     *
     * @return Panama result description
     */
    public String getPanamaResult() {
        return panamaResult;
    }

    /**
     * Gets the violation description.
     *
     * @return violation description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the violation severity.
     *
     * @return violation severity
     */
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return String.format(
            "ParityViolation{api='%s.%s', type=%s, severity=%s, description='%s'}",
            apiName, methodName, type, severity, description
        );
    }

    /**
     * Types of parity violations between implementations.
     */
    public enum ParityViolationType {
        /** Different return values for same input */
        DIFFERENT_RETURN_VALUE,
        /** Different exceptions thrown */
        DIFFERENT_EXCEPTION,
        /** Different side effects or behavior */
        DIFFERENT_BEHAVIOR,
        /** Significant performance differences */
        PERFORMANCE_DIFFERENCE,
        /** Implementation missing in one backend */
        MISSING_IMPLEMENTATION,
        /** Incompatible data types or structures */
        INCOMPATIBLE_TYPES
    }

    /**
     * Severity levels for parity violations.
     */
    public enum Severity {
        /** Low impact, minor differences that don't affect functionality */
        LOW,
        /** Medium impact, differences that may affect some use cases */
        MEDIUM,
        /** High impact, significant differences affecting most use cases */
        HIGH,
        /** Critical impact, fundamental incompatibilities */
        CRITICAL
    }

    /**
     * Builder for creating ParityViolation instances.
     */
    public static final class Builder {

        private final String apiName;
        private final String methodName;
        private ParityViolationType type;
        private String jniResult;
        private String panamaResult;
        private String description;
        private Severity severity = Severity.MEDIUM;

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
        public Builder withType(final ParityViolationType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the JNI implementation result.
         *
         * @param jniResult the JNI result
         * @return this builder
         */
        public Builder withJniResult(final String jniResult) {
            this.jniResult = jniResult;
            return this;
        }

        /**
         * Sets the Panama implementation result.
         *
         * @param panamaResult the Panama result
         * @return this builder
         */
        public Builder withPanamaResult(final String panamaResult) {
            this.panamaResult = panamaResult;
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
         * Builds the ParityViolation instance.
         *
         * @return the parity violation
         */
        public ParityViolation build() {
            return new ParityViolation(this);
        }
    }
}