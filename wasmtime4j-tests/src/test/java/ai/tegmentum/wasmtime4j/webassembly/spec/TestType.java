/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 *
 * This software is the confidential and proprietary information of Tegmentum AI.
 * You may not disclose such confidential information and may only use it in
 * accordance with the terms of the license agreement you entered into with
 * Tegmentum AI.
 */
package ai.tegmentum.wasmtime4j.webassembly.spec;

/**
 * Enumeration of WebAssembly test case types for comprehensive specification testing.
 * Categorizes test cases based on their validation purpose and execution requirements.
 *
 * <p>This enumeration provides clear categorization for different types of
 * WebAssembly tests, enabling targeted test execution and comprehensive coverage
 * of WebAssembly specification compliance.
 *
 * @since 1.0.0
 */
public enum TestType {

    /**
     * Official WebAssembly specification tests.
     * Tests that validate compliance with the WebAssembly specification.
     */
    SPECIFICATION("WebAssembly Specification Tests"),

    /**
     * WebAssembly conformance validation tests.
     * Tests that ensure implementation conformance to WebAssembly standards.
     */
    CONFORMANCE("WebAssembly Conformance Tests"),

    /**
     * Edge case and corner case tests.
     * Tests that validate behavior in unusual or boundary conditions.
     */
    EDGE_CASE("WebAssembly Edge Case Tests"),

    /**
     * Cross-implementation compatibility tests.
     * Tests that validate compatibility across different WebAssembly implementations.
     */
    CROSS_IMPLEMENTATION("Cross-Implementation Compatibility Tests"),

    /**
     * Performance regression tests.
     * Tests that validate performance characteristics and detect regressions.
     */
    PERFORMANCE("WebAssembly Performance Tests"),

    /**
     * Security validation tests.
     * Tests that validate security boundaries and sandboxing.
     */
    SECURITY("WebAssembly Security Tests"),

    /**
     * Memory management tests.
     * Tests that validate memory allocation, deallocation, and safety.
     */
    MEMORY("WebAssembly Memory Tests"),

    /**
     * WASI (WebAssembly System Interface) tests.
     * Tests that validate WASI functionality and compliance.
     */
    WASI("WASI Integration Tests"),

    /**
     * Custom wasmtime4j-specific tests.
     * Tests that validate wasmtime4j-specific functionality and features.
     */
    WASMTIME4J_SPECIFIC("Wasmtime4j Specific Tests");

    private final String displayName;

    TestType(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this test type.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns whether this test type represents specification compliance tests.
     *
     * @return true if this is a specification test type
     */
    public boolean isSpecificationTest() {
        return this == SPECIFICATION || this == CONFORMANCE;
    }

    /**
     * Returns whether this test type represents functional tests.
     *
     * @return true if this is a functional test type
     */
    public boolean isFunctionalTest() {
        return this == SPECIFICATION ||
               this == CONFORMANCE ||
               this == EDGE_CASE ||
               this == CROSS_IMPLEMENTATION ||
               this == WASMTIME4J_SPECIFIC;
    }

    /**
     * Returns whether this test type represents non-functional tests.
     *
     * @return true if this is a non-functional test type
     */
    public boolean isNonFunctionalTest() {
        return this == PERFORMANCE ||
               this == SECURITY ||
               this == MEMORY;
    }

    /**
     * Returns whether this test type represents integration tests.
     *
     * @return true if this is an integration test type
     */
    public boolean isIntegrationTest() {
        return this == WASI ||
               this == CROSS_IMPLEMENTATION ||
               this == WASMTIME4J_SPECIFIC;
    }

    /**
     * Returns the priority level for this test type.
     * Lower values indicate higher priority.
     *
     * @return priority level (0-10, where 0 is highest priority)
     */
    public int getPriority() {
        switch (this) {
            case SPECIFICATION:
                return 0;
            case CONFORMANCE:
                return 1;
            case EDGE_CASE:
                return 2;
            case SECURITY:
                return 3;
            case MEMORY:
                return 4;
            case WASI:
                return 5;
            case CROSS_IMPLEMENTATION:
                return 6;
            case WASMTIME4J_SPECIFIC:
                return 7;
            case PERFORMANCE:
                return 8;
            default:
                return 10;
        }
    }

    /**
     * Returns whether this test type should be executed in parallel.
     *
     * @return true if parallel execution is recommended
     */
    public boolean supportsParallelExecution() {
        // Performance tests should typically run in isolation
        return this != PERFORMANCE;
    }

    /**
     * Returns the default timeout for this test type in milliseconds.
     *
     * @return default timeout in milliseconds
     */
    public long getDefaultTimeoutMillis() {
        switch (this) {
            case SPECIFICATION:
            case CONFORMANCE:
            case EDGE_CASE:
                return 30_000L; // 30 seconds
            case CROSS_IMPLEMENTATION:
            case WASMTIME4J_SPECIFIC:
                return 60_000L; // 1 minute
            case SECURITY:
            case MEMORY:
                return 120_000L; // 2 minutes
            case WASI:
                return 180_000L; // 3 minutes
            case PERFORMANCE:
                return 300_000L; // 5 minutes
            default:
                return 60_000L; // 1 minute default
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}