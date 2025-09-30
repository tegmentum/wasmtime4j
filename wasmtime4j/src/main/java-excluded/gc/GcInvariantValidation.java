package ai.tegmentum.wasmtime4j.gc;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Results of GC invariant validation for WebAssembly GC runtime.
 *
 * <p>Validates critical invariants that must hold for the garbage collector to function
 * correctly, including type safety, reference consistency, heap integrity, and other
 * fundamental assumptions.
 *
 * @since 1.0.0
 */
public interface GcInvariantValidation {

  /**
   * Gets whether all invariants are satisfied.
   *
   * @return true if all invariants hold
   */
  boolean areAllInvariantsSatisfied();

  /**
   * Gets the total number of invariants checked.
   *
   * @return invariant count
   */
  int getTotalInvariantCount();

  /**
   * Gets the number of invariant violations.
   *
   * @return violation count
   */
  int getViolationCount();

  /**
   * Gets detailed information about invariant violations.
   *
   * @return list of violations
   */
  List<InvariantViolation> getViolations();

  /**
   * Gets the overall invariant satisfaction score.
   *
   * @return satisfaction score (0.0 to 1.0)
   */
  double getSatisfactionScore();

  /**
   * Gets validation results by invariant category.
   *
   * @return mapping from category to validation result
   */
  Map<InvariantCategory, CategoryValidation> getCategoryResults();

  /**
   * Gets critical invariants that must never be violated.
   *
   * @return list of critical invariant results
   */
  List<CriticalInvariantResult> getCriticalInvariants();

  /**
   * Gets performance impact of invariant checking.
   *
   * @return performance impact assessment
   */
  ValidationPerformanceImpact getPerformanceImpact();

  /**
   * Information about an invariant violation.
   */
  interface InvariantViolation {
    /** Gets the violation ID. */
    long getViolationId();

    /** Gets the invariant name. */
    String getInvariantName();

    /** Gets the invariant category. */
    InvariantCategory getCategory();

    /** Gets the violation severity. */
    ViolationSeverity getSeverity();

    /** Gets the violation description. */
    String getDescription();

    /** Gets the affected objects. */
    List<Long> getAffectedObjects();

    /** Gets the context where the violation occurred. */
    ViolationContext getContext();

    /** Gets the expected condition. */
    String getExpectedCondition();

    /** Gets the actual condition found. */
    String getActualCondition();

    /** Gets suggested remediation actions. */
    List<String> getRemediationActions();
  }

  /**
   * Categories of GC invariants.
   */
  enum InvariantCategory {
    /** Type safety invariants. */
    TYPE_SAFETY,
    /** Reference consistency invariants. */
    REFERENCE_CONSISTENCY,
    /** Memory layout invariants. */
    MEMORY_LAYOUT,
    /** Object lifecycle invariants. */
    OBJECT_LIFECYCLE,
    /** Heap structure invariants. */
    HEAP_STRUCTURE,
    /** Collection invariants. */
    COLLECTION_INVARIANTS,
    /** Thread safety invariants. */
    THREAD_SAFETY,
    /** Performance invariants. */
    PERFORMANCE
  }

  /**
   * Severity levels for violations.
   */
  enum ViolationSeverity {
    /** Informational - not a serious issue. */
    INFO,
    /** Warning - should be investigated. */
    WARNING,
    /** Error - will cause problems. */
    ERROR,
    /** Critical - system may crash or corrupt. */
    CRITICAL
  }

  /**
   * Context information for a violation.
   */
  interface ViolationContext {
    /** Gets the operation that triggered the violation. */
    String getTriggeringOperation();

    /** Gets the thread ID where violation occurred. */
    long getThreadId();

    /** Gets the call stack trace. */
    List<String> getCallStack();

    /** Gets the GC phase when violation occurred. */
    String getGcPhase();

    /** Gets additional contextual information. */
    Map<String, Object> getContextData();
  }

  /**
   * Validation result for an invariant category.
   */
  interface CategoryValidation {
    /** Gets the category name. */
    InvariantCategory getCategory();

    /** Gets whether all invariants in this category are satisfied. */
    boolean isAllSatisfied();

    /** Gets the number of invariants checked. */
    int getInvariantCount();

    /** Gets the number of violations. */
    int getViolationCount();

    /** Gets the category satisfaction score. */
    double getSatisfactionScore();

    /** Gets specific invariant results. */
    List<SpecificInvariantResult> getInvariantResults();
  }

  /**
   * Result for a specific invariant.
   */
  interface SpecificInvariantResult {
    /** Gets the invariant name. */
    String getInvariantName();

    /** Gets whether the invariant is satisfied. */
    boolean isSatisfied();

    /** Gets the invariant description. */
    String getDescription();

    /** Gets the check frequency. */
    CheckFrequency getCheckFrequency();

    /** Gets the last check result details. */
    String getLastCheckDetails();

    /** Gets the check count. */
    long getCheckCount();

    /** Gets the violation history. */
    List<ViolationRecord> getViolationHistory();
  }

  /**
   * How frequently an invariant is checked.
   */
  enum CheckFrequency {
    /** Checked on every operation. */
    ALWAYS,
    /** Checked periodically. */
    PERIODIC,
    /** Checked only during GC. */
    GC_ONLY,
    /** Checked on demand. */
    ON_DEMAND,
    /** Checked during debug builds only. */
    DEBUG_ONLY
  }

  /**
   * Record of a past violation.
   */
  interface ViolationRecord {
    /** Gets when the violation occurred. */
    java.time.Instant getTimestamp();

    /** Gets the violation details. */
    String getViolationDetails();

    /** Gets whether the violation was resolved. */
    boolean isResolved();

    /** Gets how the violation was resolved. */
    String getResolutionMethod();
  }

  /**
   * Result for a critical invariant.
   */
  interface CriticalInvariantResult {
    /** Gets the invariant name. */
    String getInvariantName();

    /** Gets whether this critical invariant holds. */
    boolean holds();

    /** Gets the criticality level. */
    CriticalityLevel getCriticalityLevel();

    /** Gets the failure impact if violated. */
    String getFailureImpact();

    /** Gets the check implementation details. */
    String getCheckImplementation();

    /** Gets the last successful check time. */
    java.time.Instant getLastSuccessfulCheck();
  }

  /**
   * Criticality levels for invariants.
   */
  enum CriticalityLevel {
    /** Must never be violated - system will crash. */
    ABSOLUTELY_CRITICAL,
    /** Should never be violated - system may corrupt. */
    HIGHLY_CRITICAL,
    /** Important but not system-threatening. */
    MODERATELY_CRITICAL,
    /** Desirable but not critical. */
    LOW_CRITICAL
  }

  /**
   * Performance impact of invariant validation.
   */
  interface ValidationPerformanceImpact {
    /** Gets the total time spent on validation. */
    java.time.Duration getTotalValidationTime();

    /** Gets the percentage of total runtime spent on validation. */
    double getValidationOverheadPercentage();

    /** Gets validation time by category. */
    Map<InvariantCategory, java.time.Duration> getTimeByCategory();

    /** Gets the most expensive invariants to check. */
    List<ExpensiveInvariant> getMostExpensiveInvariants();

    /** Gets optimization recommendations. */
    List<String> getOptimizationRecommendations();
  }

  /**
   * Information about an expensive invariant check.
   */
  interface ExpensiveInvariant {
    /** Gets the invariant name. */
    String getInvariantName();

    /** Gets the time spent checking this invariant. */
    java.time.Duration getCheckTime();

    /** Gets the percentage of total validation time. */
    double getTimePercentage();

    /** Gets the number of times checked. */
    long getCheckCount();

    /** Gets the average time per check. */
    java.time.Duration getAverageCheckTime();

    /** Gets optimization suggestions. */
    List<String> getOptimizationSuggestions();
  }

  // ========== Specific Invariant Categories ==========

  /**
   * Type safety invariant checks.
   */
  interface TypeSafetyInvariants {
    /** Validates that all type casts are safe. */
    boolean validateTypeCasts();

    /** Validates that field access matches field types. */
    boolean validateFieldAccess();

    /** Validates that array element types are consistent. */
    boolean validateArrayElementTypes();

    /** Validates that reference types match their targets. */
    boolean validateReferenceTypes();

    /** Validates type hierarchy consistency. */
    boolean validateTypeHierarchy();
  }

  /**
   * Reference consistency invariant checks.
   */
  interface ReferenceConsistencyInvariants {
    /** Validates that all references point to valid objects. */
    boolean validateReferenceTargets();

    /** Validates that reference counts are accurate. */
    boolean validateReferenceCounts();

    /** Validates that no dangling references exist. */
    boolean validateNoDanglingReferences();

    /** Validates reference graph acyclicity where required. */
    boolean validateAcyclicity();

    /** Validates weak reference consistency. */
    boolean validateWeakReferences();
  }

  /**
   * Memory layout invariant checks.
   */
  interface MemoryLayoutInvariants {
    /** Validates that object headers are intact. */
    boolean validateObjectHeaders();

    /** Validates that object sizes are consistent. */
    boolean validateObjectSizes();

    /** Validates that memory alignment is correct. */
    boolean validateMemoryAlignment();

    /** Validates that no buffer overflows exist. */
    boolean validateBufferBounds();

    /** Validates heap metadata integrity. */
    boolean validateHeapMetadata();
  }

  /**
   * Object lifecycle invariant checks.
   */
  interface ObjectLifecycleInvariants {
    /** Validates that finalized objects are not accessible. */
    boolean validateFinalizedObjects();

    /** Validates object state transitions. */
    boolean validateStateTransitions();

    /** Validates that no use-after-free occurs. */
    boolean validateUseAfterFree();

    /** Validates proper resource cleanup. */
    boolean validateResourceCleanup();

    /** Validates object creation consistency. */
    boolean validateObjectCreation();
  }

  /**
   * Gets specific invariant validators.
   *
   * @return mapping from category to specific validator
   */
  Map<InvariantCategory, Object> getSpecificValidators();
}