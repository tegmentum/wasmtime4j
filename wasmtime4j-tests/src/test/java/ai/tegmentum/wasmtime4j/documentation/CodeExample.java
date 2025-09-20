/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.util.List;
import java.util.Objects;

/**
 * Represents a validated code example with compilation and execution results.
 *
 * <p>Code examples demonstrate proper usage of API features and are validated
 * to ensure they compile and execute correctly.
 *
 * @since 1.0.0
 */
public final class CodeExample {

    private final String title;
    private final String description;
    private final String sourceCode;
    private final List<String> requiredDependencies;
    private final boolean compilationSuccessful;
    private final String compilationOutput;
    private final boolean executionSuccessful;
    private final String executionOutput;
    private final String expectedOutput;
    private final List<String> supportedPlatforms;

    /**
     * Creates a new code example with validation results.
     *
     * @param title the example title
     * @param description detailed description of what the example demonstrates
     * @param sourceCode the complete source code
     * @param requiredDependencies list of required dependencies
     * @param compilationSuccessful whether compilation succeeded
     * @param compilationOutput compiler output or error messages
     * @param executionSuccessful whether execution succeeded
     * @param executionOutput actual execution output
     * @param expectedOutput expected execution output
     * @param supportedPlatforms list of platforms where example works
     */
    public CodeExample(final String title,
                       final String description,
                       final String sourceCode,
                       final List<String> requiredDependencies,
                       final boolean compilationSuccessful,
                       final String compilationOutput,
                       final boolean executionSuccessful,
                       final String executionOutput,
                       final String expectedOutput,
                       final List<String> supportedPlatforms) {
        this.title = Objects.requireNonNull(title, "title");
        this.description = Objects.requireNonNull(description, "description");
        this.sourceCode = Objects.requireNonNull(sourceCode, "sourceCode");
        this.requiredDependencies = List.copyOf(Objects.requireNonNull(requiredDependencies, "requiredDependencies"));
        this.compilationSuccessful = compilationSuccessful;
        this.compilationOutput = Objects.requireNonNull(compilationOutput, "compilationOutput");
        this.executionSuccessful = executionSuccessful;
        this.executionOutput = Objects.requireNonNull(executionOutput, "executionOutput");
        this.expectedOutput = Objects.requireNonNull(expectedOutput, "expectedOutput");
        this.supportedPlatforms = List.copyOf(Objects.requireNonNull(supportedPlatforms, "supportedPlatforms"));
    }

    /**
     * Returns the example title.
     *
     * @return example title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns detailed description of what the example demonstrates.
     *
     * @return example description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the complete source code.
     *
     * @return source code
     */
    public String getSourceCode() {
        return sourceCode;
    }

    /**
     * Returns list of required dependencies.
     *
     * @return immutable list of dependencies
     */
    public List<String> getRequiredDependencies() {
        return requiredDependencies;
    }

    /**
     * Checks if compilation was successful.
     *
     * @return {@code true} if compilation succeeded, {@code false} otherwise
     */
    public boolean isCompilationSuccessful() {
        return compilationSuccessful;
    }

    /**
     * Returns compiler output or error messages.
     *
     * @return compilation output
     */
    public String getCompilationOutput() {
        return compilationOutput;
    }

    /**
     * Checks if execution was successful.
     *
     * @return {@code true} if execution succeeded, {@code false} otherwise
     */
    public boolean isExecutionSuccessful() {
        return executionSuccessful;
    }

    /**
     * Returns actual execution output.
     *
     * @return execution output
     */
    public String getExecutionOutput() {
        return executionOutput;
    }

    /**
     * Returns expected execution output.
     *
     * @return expected output
     */
    public String getExpectedOutput() {
        return expectedOutput;
    }

    /**
     * Returns list of platforms where example works correctly.
     *
     * @return immutable list of supported platforms
     */
    public List<String> getSupportedPlatforms() {
        return supportedPlatforms;
    }

    /**
     * Checks if the example is fully validated.
     *
     * <p>An example is considered validated if:
     * <ul>
     *   <li>Compilation is successful</li>
     *   <li>Execution is successful</li>
     *   <li>Actual output matches expected output</li>
     * </ul>
     *
     * @return {@code true} if fully validated, {@code false} otherwise
     */
    public boolean isFullyValidated() {
        return compilationSuccessful
                && executionSuccessful
                && Objects.equals(executionOutput.trim(), expectedOutput.trim());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final CodeExample that = (CodeExample) obj;
        return Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }

    @Override
    public String toString() {
        return "CodeExample{"
                + "title='" + title + '\''
                + ", compilationSuccessful=" + compilationSuccessful
                + ", executionSuccessful=" + executionSuccessful
                + ", fullyValidated=" + isFullyValidated()
                + '}';
    }
}