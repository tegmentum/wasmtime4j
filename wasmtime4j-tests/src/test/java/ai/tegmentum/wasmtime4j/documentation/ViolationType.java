/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

/**
 * Enumeration of API parity violation types.
 *
 * <p>Categorizes different kinds of inconsistencies that can occur
 * between JNI and Panama implementations.
 *
 * @since 1.0.0
 */
public enum ViolationType {

    /**
     * Method exists in one implementation but not the other.
     */
    MISSING_METHOD,

    /**
     * Method signatures differ between implementations.
     */
    SIGNATURE_MISMATCH,

    /**
     * Methods produce different return values for identical inputs.
     */
    BEHAVIORAL_DIFFERENCE,

    /**
     * Exception handling differs between implementations.
     */
    EXCEPTION_HANDLING_DIFFERENCE,

    /**
     * Performance characteristics differ significantly.
     */
    PERFORMANCE_DIFFERENCE,

    /**
     * Documentation differs between implementations.
     */
    DOCUMENTATION_INCONSISTENCY,

    /**
     * Type definitions or generics differ between implementations.
     */
    TYPE_INCOMPATIBILITY,

    /**
     * Thread safety or concurrency behavior differs.
     */
    CONCURRENCY_DIFFERENCE,

    /**
     * Resource management or cleanup behavior differs.
     */
    RESOURCE_MANAGEMENT_DIFFERENCE,

    /**
     * State management or side effects differ.
     */
    STATE_MANAGEMENT_DIFFERENCE
}