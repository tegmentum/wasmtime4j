/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

/**
 * Enumeration of documentation quality levels for API endpoints.
 *
 * <p>Quality assessment considers completeness, clarity, accuracy,
 * and usefulness of the documentation.
 *
 * @since 1.0.0
 */
public enum DocumentationQuality {

    /**
     * Comprehensive, high-quality documentation.
     *
     * <p>Characteristics:
     * <ul>
     *   <li>Complete Javadoc with clear descriptions</li>
     *   <li>All parameters and return values documented</li>
     *   <li>All exceptions documented with conditions</li>
     *   <li>Usage examples or code snippets provided</li>
     *   <li>Cross-references to related methods</li>
     * </ul>
     */
    EXCELLENT,

    /**
     * Good documentation with minor areas for improvement.
     *
     * <p>Characteristics:
     * <ul>
     *   <li>Complete basic documentation</li>
     *   <li>All required elements present</li>
     *   <li>Clear and accurate descriptions</li>
     *   <li>Minor enhancements possible (examples, cross-refs)</li>
     * </ul>
     */
    GOOD,

    /**
     * Adequate documentation meeting minimum requirements.
     *
     * <p>Characteristics:
     * <ul>
     *   <li>Basic Javadoc present</li>
     *   <li>Key parameters and return values documented</li>
     *   <li>Major exceptions documented</li>
     *   <li>Some descriptions may lack detail</li>
     * </ul>
     */
    ADEQUATE,

    /**
     * Poor documentation with significant gaps.
     *
     * <p>Characteristics:
     * <ul>
     *   <li>Minimal or incomplete Javadoc</li>
     *   <li>Missing parameter or return documentation</li>
     *   <li>Unclear or inaccurate descriptions</li>
     *   <li>Missing exception documentation</li>
     * </ul>
     */
    POOR,

    /**
     * No documentation present.
     *
     * <p>Characteristics:
     * <ul>
     *   <li>No Javadoc comments</li>
     *   <li>No parameter documentation</li>
     *   <li>No return value documentation</li>
     *   <li>No exception documentation</li>
     * </ul>
     */
    NONE
}