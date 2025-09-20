/*
 * Copyright 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.documentation.impl;

import ai.tegmentum.wasmtime4j.documentation.ParityReport;
import ai.tegmentum.wasmtime4j.documentation.ParityStatus;
import ai.tegmentum.wasmtime4j.documentation.ParityViolation;
import ai.tegmentum.wasmtime4j.documentation.ViolationSeverity;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default implementation of parity report.
 *
 * <p>Provides comprehensive analysis of API parity between JNI and Panama implementations with
 * detailed violation tracking and compliance metrics.
 *
 * @since 1.0.0
 */
public final class DefaultParityReport implements ParityReport {

  private final Map<String, ParityStatus> methodParity;
  private final Map<String, ParityStatus> typeParity;
  private final List<String> missingMethods;
  private final List<String> inconsistentBehaviors;
  private final List<ParityViolation> allViolations;
  private final double compliancePercentage;
  private final boolean completeParityAchieved;

  /**
   * Creates a new parity report.
   *
   * @param methodParity map of method signatures to parity status
   * @param typeParity map of type names to parity status
   * @param missingMethods list of missing method identifiers
   * @param inconsistentBehaviors list of behavioral inconsistency descriptions
   * @param allViolations list of all detected parity violations
   * @param compliancePercentage overall compliance percentage
   * @param completeParityAchieved whether complete parity is achieved
   */
  public DefaultParityReport(
      final Map<String, ParityStatus> methodParity,
      final Map<String, ParityStatus> typeParity,
      final List<String> missingMethods,
      final List<String> inconsistentBehaviors,
      final List<ParityViolation> allViolations,
      final double compliancePercentage,
      final boolean completeParityAchieved) {
    this.methodParity = Map.copyOf(Objects.requireNonNull(methodParity, "methodParity"));
    this.typeParity = Map.copyOf(Objects.requireNonNull(typeParity, "typeParity"));
    this.missingMethods = List.copyOf(Objects.requireNonNull(missingMethods, "missingMethods"));
    this.inconsistentBehaviors =
        List.copyOf(Objects.requireNonNull(inconsistentBehaviors, "inconsistentBehaviors"));
    this.allViolations = List.copyOf(Objects.requireNonNull(allViolations, "allViolations"));
    this.compliancePercentage = compliancePercentage;
    this.completeParityAchieved = completeParityAchieved;
  }

  @Override
  public Map<String, ParityStatus> getMethodParity() {
    return methodParity;
  }

  @Override
  public Map<String, ParityStatus> getTypeParity() {
    return typeParity;
  }

  @Override
  public List<String> getMissingMethods() {
    return missingMethods;
  }

  @Override
  public List<String> getInconsistentBehaviors() {
    return inconsistentBehaviors;
  }

  @Override
  public List<ParityViolation> getAllViolations() {
    return allViolations;
  }

  @Override
  public double getCompliancePercentage() {
    return compliancePercentage;
  }

  @Override
  public boolean isCompleteParityAchieved() {
    return completeParityAchieved;
  }

  @Override
  public String getSummary() {
    final StringBuilder summary = new StringBuilder();

    summary.append("=== API Parity Analysis Summary ===\n\n");

    // Overall Compliance
    summary.append(String.format("Overall Compliance: %.2f%%\n", compliancePercentage));
    summary.append(
        String.format(
            "Complete Parity: %s\n\n", completeParityAchieved ? "✓ ACHIEVED" : "⚠ NOT ACHIEVED"));

    // Method Parity Analysis
    summary.append("=== Method Parity Analysis ===\n");
    summary.append(String.format("Total Methods Analyzed: %d\n", methodParity.size()));

    final Map<ParityStatus, Long> methodStatusCounts =
        methodParity.values().stream()
            .collect(Collectors.groupingBy(status -> status, Collectors.counting()));

    for (final ParityStatus status : ParityStatus.values()) {
      final long count = methodStatusCounts.getOrDefault(status, 0L);
      final double percentage =
          methodParity.isEmpty() ? 0.0 : (double) count / methodParity.size() * 100.0;
      summary.append(String.format("  %s: %d (%.1f%%)\n", status, count, percentage));
    }

    // Type Parity Analysis
    summary.append("\n=== Type Parity Analysis ===\n");
    summary.append(String.format("Total Types Analyzed: %d\n", typeParity.size()));

    final Map<ParityStatus, Long> typeStatusCounts =
        typeParity.values().stream()
            .collect(Collectors.groupingBy(status -> status, Collectors.counting()));

    for (final ParityStatus status : ParityStatus.values()) {
      final long count = typeStatusCounts.getOrDefault(status, 0L);
      final double percentage =
          typeParity.isEmpty() ? 0.0 : (double) count / typeParity.size() * 100.0;
      summary.append(String.format("  %s: %d (%.1f%%)\n", status, count, percentage));
    }

    // Violations Summary
    summary.append("\n=== Violations Summary ===\n");
    summary.append(String.format("Total Violations: %d\n", allViolations.size()));

    final Map<ViolationSeverity, Long> severityCounts =
        allViolations.stream()
            .collect(Collectors.groupingBy(ParityViolation::getSeverity, Collectors.counting()));

    for (final ViolationSeverity severity : ViolationSeverity.values()) {
      final long count = severityCounts.getOrDefault(severity, 0L);
      if (count > 0) {
        summary.append(String.format("  %s: %d\n", severity, count));
      }
    }

    // Critical Issues
    summary.append("\n=== Critical Issues ===\n");
    final List<ParityViolation> criticalViolations =
        allViolations.stream()
            .filter(v -> v.getSeverity() == ViolationSeverity.CRITICAL)
            .collect(Collectors.toList());

    if (criticalViolations.isEmpty()) {
      summary.append("✓ No critical violations detected\n");
    } else {
      summary.append(
          String.format(
              "⚠ %d critical violations require immediate attention:\n",
              criticalViolations.size()));
      for (final ParityViolation violation : criticalViolations) {
        summary.append(
            String.format(
                "  - %s: %s\n", violation.getMethodSignature(), violation.getDescription()));
      }
    }

    // Missing Methods
    if (!missingMethods.isEmpty()) {
      summary.append(String.format("\n=== Missing Methods (%d) ===\n", missingMethods.size()));
      for (final String method : missingMethods) {
        summary.append(String.format("  - %s\n", method));
      }
    }

    // Behavioral Inconsistencies
    if (!inconsistentBehaviors.isEmpty()) {
      summary.append(
          String.format(
              "\n=== Behavioral Inconsistencies (%d) ===\n", inconsistentBehaviors.size()));
      for (final String behavior : inconsistentBehaviors) {
        summary.append(String.format("  - %s\n", behavior));
      }
    }

    // Recommendations
    summary.append("\n=== Recommendations ===\n");
    if (!criticalViolations.isEmpty()) {
      summary.append("1. Address critical violations immediately to restore API compatibility\n");
    }
    if (!missingMethods.isEmpty()) {
      summary.append("2. Implement missing methods in both JNI and Panama implementations\n");
    }
    if (!inconsistentBehaviors.isEmpty()) {
      summary.append("3. Investigate and resolve behavioral inconsistencies\n");
    }
    if (compliancePercentage < 95.0) {
      summary.append("4. Improve overall compliance to achieve 95%+ target\n");
    }
    if (completeParityAchieved) {
      summary.append("✓ API parity requirements met - maintain through continuous validation\n");
    }

    return summary.toString();
  }

  @Override
  public String toString() {
    return "DefaultParityReport{"
        + "methodCount="
        + methodParity.size()
        + ", typeCount="
        + typeParity.size()
        + ", violations="
        + allViolations.size()
        + ", compliance="
        + String.format("%.2f%%", compliancePercentage)
        + ", completeParityAchieved="
        + completeParityAchieved
        + '}';
  }
}
