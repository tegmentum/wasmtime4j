/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.util.List;

/**
 * Validates API parity between JNI and Panama implementations.
 *
 * <p>This validator performs comprehensive analysis to ensure 100% functional
 * parity between the JNI and Panama implementations of the wasmtime4j API.
 *
 * @since 1.0.0
 */
public interface ParityValidator {

    /**
     * Validates complete API parity between JNI and Panama implementations.
     *
     * <p>Performs comprehensive validation including:
     * <ul>
     *   <li>Method signature compatibility</li>
     *   <li>Parameter and return type consistency</li>
     *   <li>Exception handling parity</li>
     *   <li>Behavioral equivalence testing</li>
     *   <li>Documentation consistency</li>
     * </ul>
     *
     * @return detailed parity report with analysis results
     * @throws ParityValidationException if validation process fails
     */
    ParityReport validateFullParity();

    /**
     * Identifies specific parity violations between implementations.
     *
     * <p>Violations include:
     * <ul>
     *   <li>Missing methods in either implementation</li>
     *   <li>Incompatible method signatures</li>
     *   <li>Different exception handling behavior</li>
     *   <li>Inconsistent return values or side effects</li>
     *   <li>Documentation inconsistencies</li>
     * </ul>
     *
     * @return list of all detected parity violations
     */
    List<ParityViolation> findViolations();

    /**
     * Checks if complete API parity has been achieved.
     *
     * <p>Returns {@code true} only if:
     * <ul>
     *   <li>All methods are present in both implementations</li>
     *   <li>All method signatures are identical</li>
     *   <li>All behavioral tests pass for both implementations</li>
     *   <li>Exception handling is consistent</li>
     *   <li>No critical documentation inconsistencies exist</li>
     * </ul>
     *
     * @return {@code true} if full parity is achieved, {@code false} otherwise
     */
    boolean isFullParityAchieved();

    /**
     * Validates behavioral parity for specific API method.
     *
     * <p>Executes identical test scenarios against both implementations
     * and compares results for consistency.
     *
     * @param methodName the fully qualified method name to validate
     * @return behavioral parity result for the specified method
     * @throws IllegalArgumentException if method name is invalid
     */
    BehavioralParityResult validateMethodBehavior(final String methodName);

    /**
     * Validates performance parity between implementations.
     *
     * <p>Compares performance characteristics including:
     * <ul>
     *   <li>Execution time benchmarks</li>
     *   <li>Memory usage patterns</li>
     *   <li>Resource allocation behavior</li>
     *   <li>Throughput measurements</li>
     * </ul>
     *
     * @return performance parity analysis results
     */
    PerformanceParityResult validatePerformanceParity();
}