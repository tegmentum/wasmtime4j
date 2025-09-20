/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.util.List;
import java.util.Map;

/**
 * Comprehensive report of API parity analysis between JNI and Panama implementations.
 *
 * <p>This report provides detailed information about the consistency and compatibility
 * between the two implementation approaches.
 *
 * @since 1.0.0
 */
public interface ParityReport {

    /**
     * Returns method-level parity status mapping.
     *
     * <p>Each entry maps a method signature to its parity status:
     * <ul>
     *   <li>IDENTICAL - Complete parity with no differences</li>
     *   <li>MINOR_DIFFERENCES - Functionally equivalent with minor variations</li>
     *   <li>MAJOR_DIFFERENCES - Significant behavioral or signature differences</li>
     *   <li>MISSING - Method exists in only one implementation</li>
     * </ul>
     *
     * @return immutable map of method signatures to parity status
     */
    Map<String, ParityStatus> getMethodParity();

    /**
     * Returns type-level parity status mapping.
     *
     * <p>Analyzes consistency of:
     * <ul>
     *   <li>Class and interface definitions</li>
     *   <li>Type parameter compatibility</li>
     *   <li>Inheritance hierarchies</li>
     *   <li>Annotation consistency</li>
     * </ul>
     *
     * @return immutable map of type names to parity status
     */
    Map<String, ParityStatus> getTypeParity();

    /**
     * Returns list of methods missing from either implementation.
     *
     * <p>Each entry specifies:
     * <ul>
     *   <li>Method signature</li>
     *   <li>Which implementation is missing the method</li>
     *   <li>Expected implementation source</li>
     * </ul>
     *
     * @return immutable list of missing method identifiers
     */
    List<String> getMissingMethods();

    /**
     * Returns list of behavioral inconsistencies between implementations.
     *
     * <p>Inconsistencies include:
     * <ul>
     *   <li>Different return values for identical inputs</li>
     *   <li>Varying exception throwing behavior</li>
     *   <li>Different side effects or state changes</li>
     *   <li>Performance characteristic variations</li>
     * </ul>
     *
     * @return immutable list of behavioral inconsistency descriptions
     */
    List<String> getInconsistentBehaviors();

    /**
     * Returns list of all detected parity violations.
     *
     * <p>Comprehensive list including:
     * <ul>
     *   <li>Critical violations requiring immediate attention</li>
     *   <li>Minor violations with suggested improvements</li>
     *   <li>Documentation inconsistencies</li>
     *   <li>Performance discrepancies</li>
     * </ul>
     *
     * @return immutable list of all parity violations
     */
    List<ParityViolation> getAllViolations();

    /**
     * Returns overall parity compliance percentage.
     *
     * <p>Calculated based on:
     * <ul>
     *   <li>Method signature compatibility (40%)</li>
     *   <li>Behavioral consistency (35%)</li>
     *   <li>Type system compatibility (15%)</li>
     *   <li>Documentation consistency (10%)</li>
     * </ul>
     *
     * @return compliance percentage from 0.0 to 100.0
     */
    double getCompliancePercentage();

    /**
     * Checks if complete parity has been achieved.
     *
     * <p>Returns {@code true} only if:
     * <ul>
     *   <li>No missing methods exist</li>
     *   <li>No behavioral inconsistencies detected</li>
     *   <li>All types have identical definitions</li>
     *   <li>No critical violations exist</li>
     * </ul>
     *
     * @return {@code true} if complete parity achieved, {@code false} otherwise
     */
    boolean isCompleteParityAchieved();

    /**
     * Returns human-readable summary of parity analysis.
     *
     * <p>Summary includes:
     * <ul>
     *   <li>Overall compliance status</li>
     *   <li>Critical issues requiring attention</li>
     *   <li>Improvement recommendations</li>
     *   <li>Implementation progress metrics</li>
     * </ul>
     *
     * @return formatted parity analysis summary
     */
    String getSummary();
}