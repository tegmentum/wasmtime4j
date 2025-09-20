/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.util.Objects;

/**
 * Represents a test failure with detailed diagnostic information.
 *
 * <p>Provides comprehensive information about failed tests to aid
 * in debugging and issue resolution.
 *
 * @since 1.0.0
 */
public final class TestFailure {

    private final String testName;
    private final String testClass;
    private final String methodName;
    private final String failureMessage;
    private final String stackTrace;
    private final FailureType type;
    private final String expectedValue;
    private final String actualValue;

    /**
     * Creates a new test failure record.
     *
     * @param testName the name of the failed test
     * @param testClass the test class name
     * @param methodName the test method name
     * @param failureMessage the failure message
     * @param stackTrace the exception stack trace
     * @param type the type of failure
     * @param expectedValue the expected test value
     * @param actualValue the actual test value
     */
    public TestFailure(final String testName,
                       final String testClass,
                       final String methodName,
                       final String failureMessage,
                       final String stackTrace,
                       final FailureType type,
                       final String expectedValue,
                       final String actualValue) {
        this.testName = Objects.requireNonNull(testName, "testName");
        this.testClass = Objects.requireNonNull(testClass, "testClass");
        this.methodName = Objects.requireNonNull(methodName, "methodName");
        this.failureMessage = Objects.requireNonNull(failureMessage, "failureMessage");
        this.stackTrace = Objects.requireNonNull(stackTrace, "stackTrace");
        this.type = Objects.requireNonNull(type, "type");
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
    }

    /**
     * Returns the name of the failed test.
     *
     * @return test name
     */
    public String getTestName() {
        return testName;
    }

    /**
     * Returns the test class name.
     *
     * @return test class name
     */
    public String getTestClass() {
        return testClass;
    }

    /**
     * Returns the test method name.
     *
     * @return test method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns the failure message.
     *
     * @return failure message
     */
    public String getFailureMessage() {
        return failureMessage;
    }

    /**
     * Returns the exception stack trace.
     *
     * @return stack trace
     */
    public String getStackTrace() {
        return stackTrace;
    }

    /**
     * Returns the type of failure.
     *
     * @return failure type
     */
    public FailureType getType() {
        return type;
    }

    /**
     * Returns the expected test value.
     *
     * @return expected value, or {@code null} if not applicable
     */
    public String getExpectedValue() {
        return expectedValue;
    }

    /**
     * Returns the actual test value.
     *
     * @return actual value, or {@code null} if not applicable
     */
    public String getActualValue() {
        return actualValue;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TestFailure that = (TestFailure) obj;
        return Objects.equals(testName, that.testName)
                && Objects.equals(testClass, that.testClass)
                && Objects.equals(methodName, that.methodName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testName, testClass, methodName);
    }

    @Override
    public String toString() {
        return "TestFailure{"
                + "test='" + testClass + "." + methodName + '\''
                + ", type=" + type
                + ", message='" + failureMessage + '\''
                + '}';
    }

    /**
     * Enumeration of test failure types.
     */
    public enum FailureType {
        ASSERTION_FAILURE,
        EXCEPTION,
        TIMEOUT,
        SETUP_FAILURE,
        TEARDOWN_FAILURE,
        COMPILATION_ERROR,
        RUNTIME_ERROR
    }
}