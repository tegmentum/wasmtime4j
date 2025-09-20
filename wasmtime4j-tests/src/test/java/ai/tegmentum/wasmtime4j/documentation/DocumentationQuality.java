/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

/**
 * Enumeration of documentation quality levels for API endpoints.
 *
 * <p>Quality assessment considers completeness, clarity, accuracy, and usefulness of the
 * documentation.
 *
 * @since 1.0.0
 */
public enum DocumentationQuality {

  /**
   * Comprehensive, high-quality documentation.
   *
   * <p>Characteristics:
   *
   * <ul>
   *   <li>Complete Javadoc with clear descriptions
   *   <li>All parameters and return values documented
   *   <li>All exceptions documented with conditions
   *   <li>Usage examples or code snippets provided
   *   <li>Cross-references to related methods
   * </ul>
   */
  EXCELLENT,

  /**
   * Good documentation with minor areas for improvement.
   *
   * <p>Characteristics:
   *
   * <ul>
   *   <li>Complete basic documentation
   *   <li>All required elements present
   *   <li>Clear and accurate descriptions
   *   <li>Minor enhancements possible (examples, cross-refs)
   * </ul>
   */
  GOOD,

  /**
   * Adequate documentation meeting minimum requirements.
   *
   * <p>Characteristics:
   *
   * <ul>
   *   <li>Basic Javadoc present
   *   <li>Key parameters and return values documented
   *   <li>Major exceptions documented
   *   <li>Some descriptions may lack detail
   * </ul>
   */
  ADEQUATE,

  /**
   * Poor documentation with significant gaps.
   *
   * <p>Characteristics:
   *
   * <ul>
   *   <li>Minimal or incomplete Javadoc
   *   <li>Missing parameter or return documentation
   *   <li>Unclear or inaccurate descriptions
   *   <li>Missing exception documentation
   * </ul>
   */
  POOR,

  /**
   * No documentation present.
   *
   * <p>Characteristics:
   *
   * <ul>
   *   <li>No Javadoc comments
   *   <li>No parameter documentation
   *   <li>No return value documentation
   *   <li>No exception documentation
   * </ul>
   */
  NONE
}
