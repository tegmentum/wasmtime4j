/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

/**
 * Enumeration of API parity violation severity levels.
 *
 * <p>Indicates the impact and urgency of addressing different types
 * of parity violations.
 *
 * @since 1.0.0
 */
public enum ViolationSeverity {

    /**
     * Critical violations that break API compatibility.
     *
     * <p>These violations:
     * <ul>
     *   <li>Prevent successful compilation or runtime execution</li>
     *   <li>Cause different functional outcomes</li>
     *   <li>Break existing user code</li>
     *   <li>Require immediate attention</li>
     * </ul>
     */
    CRITICAL,

    /**
     * High-priority violations affecting user experience.
     *
     * <p>These violations:
     * <ul>
     *   <li>Cause significant behavioral differences</li>
     *   <li>May lead to unexpected results</li>
     *   <li>Affect performance significantly</li>
     *   <li>Should be addressed soon</li>
     * </ul>
     */
    HIGH,

    /**
     * Medium-priority violations with moderate impact.
     *
     * <p>These violations:
     * <ul>
     *   <li>Cause minor behavioral differences</li>
     *   <li>May affect some use cases</li>
     *   <li>Have moderate performance impact</li>
     *   <li>Should be addressed when convenient</li>
     * </ul>
     */
    MEDIUM,

    /**
     * Low-priority violations with minimal impact.
     *
     * <p>These violations:
     * <ul>
     *   <li>Cause cosmetic or minor differences</li>
     *   <li>Don't affect core functionality</li>
     *   <li>Have minimal performance impact</li>
     *   <li>Can be addressed in future releases</li>
     * </ul>
     */
    LOW,

    /**
     * Informational differences that don't require action.
     *
     * <p>These violations:
     * <ul>
     *   <li>Document expected implementation differences</li>
     *   <li>Don't affect user experience</li>
     *   <li>Are within acceptable tolerance</li>
     *   <li>Serve as documentation only</li>
     * </ul>
     */
    INFO
}