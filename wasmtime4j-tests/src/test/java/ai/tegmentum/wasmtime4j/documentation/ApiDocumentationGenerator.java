/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

/**
 * Interface for generating comprehensive API documentation and validation reports.
 *
 * <p>This generator analyzes the wasmtime4j API to ensure complete documentation coverage
 * and validates consistency between JNI and Panama implementations.
 *
 * @since 1.0.0
 */
public interface ApiDocumentationGenerator {

    /**
     * Generates a comprehensive documentation report for all public APIs.
     *
     * <p>The report includes:
     * <ul>
     *   <li>Coverage statistics for Javadoc documentation</li>
     *   <li>List of documented and undocumented API endpoints</li>
     *   <li>API parity analysis between implementations</li>
     *   <li>Quality metrics and recommendations</li>
     * </ul>
     *
     * @return complete documentation report with all analysis results
     * @throws DocumentationException if analysis fails or encounters errors
     */
    DocumentationReport generateReport();

    /**
     * Validates API parity between JNI and Panama implementations.
     *
     * <p>Performs comprehensive comparison of:
     * <ul>
     *   <li>Method signatures and parameter types</li>
     *   <li>Return types and exception handling</li>
     *   <li>Behavioral consistency across implementations</li>
     *   <li>Documentation consistency</li>
     * </ul>
     *
     * @throws ParityValidationException if parity violations are detected
     */
    void validateApiParity();

    /**
     * Generates working code examples for all major API features.
     *
     * <p>Creates examples demonstrating:
     * <ul>
     *   <li>Basic usage patterns for each major component</li>
     *   <li>Advanced use cases and integration scenarios</li>
     *   <li>Error handling and resource management</li>
     *   <li>Performance optimization techniques</li>
     * </ul>
     *
     * @throws ExampleGenerationException if example creation fails
     */
    void generateExamples();

    /**
     * Validates that all generated examples compile and execute correctly.
     *
     * <p>Verification includes:
     * <ul>
     *   <li>Compilation verification for all examples</li>
     *   <li>Runtime execution validation</li>
     *   <li>Output verification against expected results</li>
     *   <li>Cross-platform compatibility testing</li>
     * </ul>
     *
     * @throws ExampleValidationException if any examples fail validation
     */
    void validateExamples();
}