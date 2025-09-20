/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation;

import java.util.Objects;

/**
 * Represents a specific API parity violation between JNI and Panama implementations.
 *
 * <p>Violations indicate areas where the two implementations differ in ways
 * that may affect compatibility or user experience.
 *
 * @since 1.0.0
 */
public final class ParityViolation {

    private final ViolationType type;
    private final ViolationSeverity severity;
    private final String methodSignature;
    private final String description;
    private final String jniImplementation;
    private final String panamaImplementation;
    private final String recommendation;

    /**
     * Creates a new parity violation record.
     *
     * @param type the type of violation detected
     * @param severity the severity level of the violation
     * @param methodSignature the method signature where violation was detected
     * @param description detailed description of the violation
     * @param jniImplementation description of JNI implementation behavior
     * @param panamaImplementation description of Panama implementation behavior
     * @param recommendation suggested action to resolve the violation
     */
    public ParityViolation(final ViolationType type,
                           final ViolationSeverity severity,
                           final String methodSignature,
                           final String description,
                           final String jniImplementation,
                           final String panamaImplementation,
                           final String recommendation) {
        this.type = Objects.requireNonNull(type, "type");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.methodSignature = Objects.requireNonNull(methodSignature, "methodSignature");
        this.description = Objects.requireNonNull(description, "description");
        this.jniImplementation = Objects.requireNonNull(jniImplementation, "jniImplementation");
        this.panamaImplementation = Objects.requireNonNull(panamaImplementation, "panamaImplementation");
        this.recommendation = Objects.requireNonNull(recommendation, "recommendation");
    }

    /**
     * Returns the type of violation.
     *
     * @return violation type classification
     */
    public ViolationType getType() {
        return type;
    }

    /**
     * Returns the severity level of the violation.
     *
     * @return violation severity assessment
     */
    public ViolationSeverity getSeverity() {
        return severity;
    }

    /**
     * Returns the method signature where the violation was detected.
     *
     * @return affected method signature
     */
    public String getMethodSignature() {
        return methodSignature;
    }

    /**
     * Returns detailed description of the violation.
     *
     * @return violation description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns description of JNI implementation behavior.
     *
     * @return JNI implementation details
     */
    public String getJniImplementation() {
        return jniImplementation;
    }

    /**
     * Returns description of Panama implementation behavior.
     *
     * @return Panama implementation details
     */
    public String getPanamaImplementation() {
        return panamaImplementation;
    }

    /**
     * Returns suggested action to resolve the violation.
     *
     * @return resolution recommendation
     */
    public String getRecommendation() {
        return recommendation;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ParityViolation that = (ParityViolation) obj;
        return type == that.type
                && severity == that.severity
                && Objects.equals(methodSignature, that.methodSignature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, severity, methodSignature);
    }

    @Override
    public String toString() {
        return "ParityViolation{"
                + "type=" + type
                + ", severity=" + severity
                + ", method='" + methodSignature + '\''
                + ", description='" + description + '\''
                + '}';
    }
}