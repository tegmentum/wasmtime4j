/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

/**
 * Enumeration of API parity statuses between JNI and Panama implementations.
 *
 * <p>These statuses indicate the level of compatibility and consistency
 * between the two implementation approaches.
 *
 * @since 1.0.0
 */
public enum ParityStatus {

    /**
     * Complete parity with identical behavior and signatures.
     *
     * <p>Indicates:
     * <ul>
     *   <li>Identical method signatures</li>
     *   <li>Equivalent behavioral outcomes</li>
     *   <li>Consistent exception handling</li>
     *   <li>Compatible performance characteristics</li>
     * </ul>
     */
    IDENTICAL,

    /**
     * Functionally equivalent with minor implementation differences.
     *
     * <p>Indicates:
     * <ul>
     *   <li>Same functional outcomes</li>
     *   <li>Minor performance variations within acceptable limits</li>
     *   <li>Equivalent error handling with minor message differences</li>
     *   <li>Compatible but not identical internal behavior</li>
     * </ul>
     */
    MINOR_DIFFERENCES,

    /**
     * Significant differences requiring attention or documentation.
     *
     * <p>Indicates:
     * <ul>
     *   <li>Different behavioral outcomes for some inputs</li>
     *   <li>Significant performance variations</li>
     *   <li>Different exception types or handling patterns</li>
     *   <li>Incompatible side effects or state changes</li>
     * </ul>
     */
    MAJOR_DIFFERENCES,

    /**
     * Method or feature exists in only one implementation.
     *
     * <p>Indicates:
     * <ul>
     *   <li>Method present in JNI but not Panama implementation</li>
     *   <li>Method present in Panama but not JNI implementation</li>
     *   <li>Feature supported by only one runtime</li>
     *   <li>Implementation-specific functionality</li>
     * </ul>
     */
    MISSING
}