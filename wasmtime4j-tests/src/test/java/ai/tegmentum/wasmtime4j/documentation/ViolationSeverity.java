/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

/**
 * Enumeration of API parity violation severity levels.
 *
 * <p>Indicates the impact and urgency of addressing different types of parity violations.
 *
 * @since 1.0.0
 */
public enum ViolationSeverity {

  /**
   * Critical violations that break API compatibility.
   *
   * <p>These violations:
   *
   * <ul>
   *   <li>Prevent successful compilation or runtime execution
   *   <li>Cause different functional outcomes
   *   <li>Break existing user code
   *   <li>Require immediate attention
   * </ul>
   */
  CRITICAL,

  /**
   * High-priority violations affecting user experience.
   *
   * <p>These violations:
   *
   * <ul>
   *   <li>Cause significant behavioral differences
   *   <li>May lead to unexpected results
   *   <li>Affect performance significantly
   *   <li>Should be addressed soon
   * </ul>
   */
  HIGH,

  /**
   * Medium-priority violations with moderate impact.
   *
   * <p>These violations:
   *
   * <ul>
   *   <li>Cause minor behavioral differences
   *   <li>May affect some use cases
   *   <li>Have moderate performance impact
   *   <li>Should be addressed when convenient
   * </ul>
   */
  MEDIUM,

  /**
   * Low-priority violations with minimal impact.
   *
   * <p>These violations:
   *
   * <ul>
   *   <li>Cause cosmetic or minor differences
   *   <li>Don't affect core functionality
   *   <li>Have minimal performance impact
   *   <li>Can be addressed in future releases
   * </ul>
   */
  LOW,

  /**
   * Informational differences that don't require action.
   *
   * <p>These violations:
   *
   * <ul>
   *   <li>Document expected implementation differences
   *   <li>Don't affect user experience
   *   <li>Are within acceptable tolerance
   *   <li>Serve as documentation only
   * </ul>
   */
  INFO
}
