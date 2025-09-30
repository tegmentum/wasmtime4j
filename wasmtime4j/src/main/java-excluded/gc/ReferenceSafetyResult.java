package ai.tegmentum.wasmtime4j.gc;

import java.util.List;
import java.util.Map;

/**
 * Results of reference safety validation for WebAssembly GC objects.
 *
 * <p>Provides comprehensive analysis of reference safety, including type safety violations,
 * dangling references, null pointer access patterns, and other safety concerns.
 *
 * @since 1.0.0
 */
public interface ReferenceSafetyResult {

  /**
   * Gets whether all references passed safety validation.
   *
   * @return true if all references are safe
   */
  boolean isAllSafe();

  /**
   * Gets the total number of references validated.
   *
   * @return total reference count
   */
  long getTotalReferencesValidated();

  /**
   * Gets the number of safety violations found.
   *
   * @return violation count
   */
  int getViolationCount();

  /**
   * Gets detailed information about safety violations.
   *
   * @return list of safety violations
   */
  List<SafetyViolation> getSafetyViolations();

  /**
   * Gets the overall safety score (0.0 to 1.0).
   *
   * @return safety score where 1.0 is completely safe
   */
  double getSafetyScore();

  /**
   * Gets safety statistics by violation type.
   *
   * @return mapping from violation type to count
   */
  Map<ViolationType, Integer> getViolationStatistics();

  /**
   * Gets recommendations for improving reference safety.
   *
   * @return list of safety recommendations
   */
  List<SafetyRecommendation> getRecommendations();

  /**
   * Gets potentially dangerous reference patterns.
   *
   * @return list of dangerous patterns
   */
  List<DangerousReferencePattern> getDangerousPatterns();

  /**
   * Information about a reference safety violation.
   */
  interface SafetyViolation {
    /** Gets the violation ID. */
    long getViolationId();

    /** Gets the violation type. */
    ViolationType getViolationType();

    /** Gets the violation severity. */
    ViolationSeverity getSeverity();

    /** Gets the object ID involved in the violation. */
    long getObjectId();

    /** Gets the reference path to the violation. */
    List<Long> getReferencePath();

    /** Gets a description of the violation. */
    String getDescription();

    /** Gets the expected type (for type violations). */
    String getExpectedType();

    /** Gets the actual type (for type violations). */
    String getActualType();

    /** Gets suggested fixes for this violation. */
    List<String> getSuggestedFixes();
  }

  /**
   * Types of reference safety violations.
   */
  enum ViolationType {
    /** Type cast violation. */
    TYPE_CAST_VIOLATION,
    /** Null reference access. */
    NULL_REFERENCE_ACCESS,
    /** Dangling reference. */
    DANGLING_REFERENCE,
    /** Type mismatch in assignment. */
    TYPE_ASSIGNMENT_MISMATCH,
    /** Invalid field access. */
    INVALID_FIELD_ACCESS,
    /** Invalid array access. */
    INVALID_ARRAY_ACCESS,
    /** Reference to finalized object. */
    FINALIZED_OBJECT_REFERENCE,
    /** Circular dependency. */
    CIRCULAR_DEPENDENCY,
    /** Cross-module reference violation. */
    CROSS_MODULE_VIOLATION
  }

  /**
   * Severity levels for safety violations.
   */
  enum ViolationSeverity {
    /** Information level - not a serious problem. */
    INFO,
    /** Warning level - should be addressed. */
    WARNING,
    /** Error level - will cause runtime problems. */
    ERROR,
    /** Critical level - will cause crashes or corruption. */
    CRITICAL
  }

  /**
   * A dangerous reference pattern that may lead to safety issues.
   */
  interface DangerousReferencePattern {
    /** Gets the pattern name. */
    String getPatternName();

    /** Gets the pattern description. */
    String getDescription();

    /** Gets objects involved in the pattern. */
    List<Long> getInvolvedObjects();

    /** Gets the risk level. */
    RiskLevel getRiskLevel();

    /** Gets the likelihood of problems. */
    double getProblemLikelihood();

    /** Gets mitigation strategies. */
    List<String> getMitigationStrategies();
  }

  /**
   * Risk levels for dangerous patterns.
   */
  enum RiskLevel {
    /** Low risk pattern. */
    LOW,
    /** Medium risk pattern. */
    MEDIUM,
    /** High risk pattern. */
    HIGH,
    /** Critical risk pattern. */
    CRITICAL
  }

  /**
   * A recommendation for improving reference safety.
   */
  interface SafetyRecommendation {
    /** Gets the recommendation type. */
    RecommendationType getType();

    /** Gets the recommendation description. */
    String getDescription();

    /** Gets the affected objects. */
    List<Long> getAffectedObjects();

    /** Gets the expected impact. */
    String getExpectedImpact();

    /** Gets the implementation difficulty. */
    ImplementationDifficulty getDifficulty();

    /** Gets the recommendation priority. */
    RecommendationPriority getPriority();
  }

  /**
   * Types of safety recommendations.
   */
  enum RecommendationType {
    /** Add type validation. */
    ADD_TYPE_VALIDATION,
    /** Add null checks. */
    ADD_NULL_CHECKS,
    /** Fix type casting. */
    FIX_TYPE_CASTING,
    /** Improve lifecycle management. */
    IMPROVE_LIFECYCLE,
    /** Add bounds checking. */
    ADD_BOUNDS_CHECKING,
    /** Implement proper cleanup. */
    IMPLEMENT_CLEANUP,
    /** Use safer reference patterns. */
    USE_SAFER_PATTERNS
  }

  /**
   * Implementation difficulty levels.
   */
  enum ImplementationDifficulty {
    /** Easy to implement. */
    EASY,
    /** Moderate effort required. */
    MODERATE,
    /** Significant effort required. */
    DIFFICULT,
    /** Very difficult to implement. */
    VERY_DIFFICULT
  }

  /**
   * Priority levels for recommendations.
   */
  enum RecommendationPriority {
    /** Critical priority. */
    CRITICAL,
    /** High priority. */
    HIGH,
    /** Medium priority. */
    MEDIUM,
    /** Low priority. */
    LOW
  }
}