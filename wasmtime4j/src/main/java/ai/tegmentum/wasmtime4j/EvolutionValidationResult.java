/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j;

import java.util.List;
import java.util.Map;

/**
 * Result of interface evolution strategy validation.
 *
 * <p>This class contains the results of validating an interface evolution strategy,
 * including any issues, warnings, and recommendations for improvement.
 *
 * @since 1.0.0
 */
public final class EvolutionValidationResult {

  private final boolean isValid;
  private final List<ValidationIssue> issues;
  private final List<ValidationWarning> warnings;
  private final List<ValidationRecommendation> recommendations;
  private final Map<String, Object> validationMetrics;
  private final double confidenceScore;
  private final ValidationSummary summary;

  public EvolutionValidationResult(
      boolean isValid,
      List<ValidationIssue> issues,
      List<ValidationWarning> warnings,
      List<ValidationRecommendation> recommendations,
      Map<String, Object> validationMetrics,
      double confidenceScore,
      ValidationSummary summary) {
    this.isValid = isValid;
    this.issues = List.copyOf(issues != null ? issues : List.of());
    this.warnings = List.copyOf(warnings != null ? warnings : List.of());
    this.recommendations = List.copyOf(recommendations != null ? recommendations : List.of());
    this.validationMetrics = Map.copyOf(validationMetrics != null ? validationMetrics : Map.of());
    this.confidenceScore = Math.max(0.0, Math.min(1.0, confidenceScore));
    this.summary = summary != null ? summary : ValidationSummary.empty();
  }

  /**
   * Checks if the evolution strategy is valid.
   *
   * @return true if the strategy is valid
   */
  public boolean isValid() {
    return isValid;
  }

  /**
   * Gets the validation issues.
   *
   * @return list of validation issues
   */
  public List<ValidationIssue> getIssues() {
    return issues;
  }

  /**
   * Gets the validation warnings.
   *
   * @return list of validation warnings
   */
  public List<ValidationWarning> getWarnings() {
    return warnings;
  }

  /**
   * Gets the validation recommendations.
   *
   * @return list of validation recommendations
   */
  public List<ValidationRecommendation> getRecommendations() {
    return recommendations;
  }

  /**
   * Gets the validation metrics.
   *
   * @return validation metrics map
   */
  public Map<String, Object> getValidationMetrics() {
    return validationMetrics;
  }

  /**
   * Gets the confidence score for the validation.
   *
   * @return confidence score (0.0 to 1.0)
   */
  public double getConfidenceScore() {
    return confidenceScore;
  }

  /**
   * Gets the validation summary.
   *
   * @return validation summary
   */
  public ValidationSummary getSummary() {
    return summary;
  }

  /**
   * Checks if there are any critical issues.
   *
   * @return true if there are critical issues
   */
  public boolean hasCriticalIssues() {
    return issues.stream().anyMatch(issue -> issue.getSeverity() == ValidationSeverity.CRITICAL);
  }

  /**
   * Gets the count of issues by severity.
   *
   * @return map of severity to issue count
   */
  public Map<ValidationSeverity, Long> getIssueCountBySeverity() {
    return issues.stream()
        .collect(java.util.stream.Collectors.groupingBy(
            ValidationIssue::getSeverity,
            java.util.stream.Collectors.counting()));
  }

  /**
   * Creates a successful validation result.
   *
   * @return successful validation result
   */
  public static EvolutionValidationResult success() {
    return new EvolutionValidationResult(
        true,
        List.of(),
        List.of(),
        List.of(),
        Map.of(),
        1.0,
        ValidationSummary.success());
  }

  /**
   * Creates a failed validation result.
   *
   * @param issues the validation issues
   * @return failed validation result
   */
  public static EvolutionValidationResult failure(List<ValidationIssue> issues) {
    return new EvolutionValidationResult(
        false,
        issues,
        List.of(),
        List.of(),
        Map.of(),
        0.0,
        ValidationSummary.failure(issues.size()));
  }

  /**
   * Validation issue details.
   */
  public static final class ValidationIssue {
    private final String code;
    private final String message;
    private final ValidationSeverity severity;
    private final String location;
    private final List<String> suggestedFixes;

    public ValidationIssue(String code, String message, ValidationSeverity severity,
                           String location, List<String> suggestedFixes) {
      this.code = code;
      this.message = message;
      this.severity = severity;
      this.location = location;
      this.suggestedFixes = List.copyOf(suggestedFixes != null ? suggestedFixes : List.of());
    }

    public String getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    public ValidationSeverity getSeverity() {
      return severity;
    }

    public String getLocation() {
      return location;
    }

    public List<String> getSuggestedFixes() {
      return suggestedFixes;
    }
  }

  /**
   * Validation warning details.
   */
  public static final class ValidationWarning {
    private final String code;
    private final String message;
    private final String location;
    private final List<String> suggestions;

    public ValidationWarning(String code, String message, String location, List<String> suggestions) {
      this.code = code;
      this.message = message;
      this.location = location;
      this.suggestions = List.copyOf(suggestions != null ? suggestions : List.of());
    }

    public String getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    public String getLocation() {
      return location;
    }

    public List<String> getSuggestions() {
      return suggestions;
    }
  }

  /**
   * Validation recommendation details.
   */
  public static final class ValidationRecommendation {
    private final String category;
    private final String title;
    private final String description;
    private final RecommendationPriority priority;
    private final List<String> benefits;

    public ValidationRecommendation(String category, String title, String description,
                                    RecommendationPriority priority, List<String> benefits) {
      this.category = category;
      this.title = title;
      this.description = description;
      this.priority = priority;
      this.benefits = List.copyOf(benefits != null ? benefits : List.of());
    }

    public String getCategory() {
      return category;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public RecommendationPriority getPriority() {
      return priority;
    }

    public List<String> getBenefits() {
      return benefits;
    }
  }

  /**
   * Validation summary.
   */
  public static final class ValidationSummary {
    private final int totalIssues;
    private final int totalWarnings;
    private final int totalRecommendations;
    private final ValidationStatus status;
    private final String overallAssessment;

    public ValidationSummary(int totalIssues, int totalWarnings, int totalRecommendations,
                             ValidationStatus status, String overallAssessment) {
      this.totalIssues = totalIssues;
      this.totalWarnings = totalWarnings;
      this.totalRecommendations = totalRecommendations;
      this.status = status;
      this.overallAssessment = overallAssessment;
    }

    public int getTotalIssues() {
      return totalIssues;
    }

    public int getTotalWarnings() {
      return totalWarnings;
    }

    public int getTotalRecommendations() {
      return totalRecommendations;
    }

    public ValidationStatus getStatus() {
      return status;
    }

    public String getOverallAssessment() {
      return overallAssessment;
    }

    public static ValidationSummary empty() {
      return new ValidationSummary(0, 0, 0, ValidationStatus.PASSED, "No issues found");
    }

    public static ValidationSummary success() {
      return new ValidationSummary(0, 0, 0, ValidationStatus.PASSED, "Strategy is valid");
    }

    public static ValidationSummary failure(int issueCount) {
      return new ValidationSummary(issueCount, 0, 0, ValidationStatus.FAILED,
          "Strategy has " + issueCount + " issue(s)");
    }
  }

  /**
   * Validation severity levels.
   */
  public enum ValidationSeverity {
    /** Information only */
    INFO,
    /** Minor issue that should be addressed */
    WARNING,
    /** Significant issue that may cause problems */
    ERROR,
    /** Critical issue that will cause failure */
    CRITICAL
  }

  /**
   * Recommendation priority levels.
   */
  public enum RecommendationPriority {
    /** Low priority recommendation */
    LOW,
    /** Medium priority recommendation */
    MEDIUM,
    /** High priority recommendation */
    HIGH,
    /** Critical recommendation that should be implemented */
    CRITICAL
  }

  /**
   * Validation status enum.
   */
  public enum ValidationStatus {
    /** Validation passed without issues */
    PASSED,
    /** Validation passed with warnings */
    PASSED_WITH_WARNINGS,
    /** Validation failed with errors */
    FAILED,
    /** Validation could not be completed */
    INCOMPLETE
  }
}