package ai.tegmentum.wasmtime4j.gc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Results of memory corruption detection analysis for WebAssembly GC objects.
 *
 * <p>Provides detailed information about potential memory corruption issues, including
 * buffer overflows, use-after-free conditions, double-free errors, and other memory
 * safety violations.
 *
 * @since 1.0.0
 */
public interface MemoryCorruptionAnalysis {

  /**
   * Gets the analysis timestamp.
   *
   * @return when the analysis was performed
   */
  Instant getAnalysisTime();

  /**
   * Gets whether any memory corruption was detected.
   *
   * @return true if corruption was found
   */
  boolean isCorruptionDetected();

  /**
   * Gets the corruption severity level.
   *
   * @return severity level of detected corruption
   */
  CorruptionSeverity getCorruptionSeverity();

  /**
   * Gets detailed information about detected corruption.
   *
   * @return list of corruption issues
   */
  List<CorruptionIssue> getCorruptionIssues();

  /**
   * Gets memory integrity check results.
   *
   * @return integrity check results
   */
  MemoryIntegrityResult getIntegrityResult();

  /**
   * Gets heap consistency check results.
   *
   * @return consistency check results
   */
  HeapConsistencyResult getConsistencyResult();

  /**
   * Gets object lifecycle violation results.
   *
   * @return lifecycle violation results
   */
  LifecycleViolationResult getLifecycleViolationResult();

  /**
   * Gets recommendations for addressing corruption.
   *
   * @return list of recommendations
   */
  List<CorruptionRecommendation> getRecommendations();

  /**
   * Information about a memory corruption issue.
   */
  interface CorruptionIssue {
    /** Gets the issue ID. */
    long getIssueId();

    /** Gets the corruption type. */
    CorruptionType getCorruptionType();

    /** Gets the issue severity. */
    CorruptionSeverity getSeverity();

    /** Gets the affected object ID. */
    long getAffectedObjectId();

    /** Gets the memory address if applicable. */
    long getMemoryAddress();

    /** Gets the corruption description. */
    String getDescription();

    /** Gets the detection method used. */
    String getDetectionMethod();

    /** Gets the confidence level of the detection. */
    double getConfidenceLevel();

    /** Gets the potential causes. */
    List<String> getPotentialCauses();

    /** Gets the expected vs actual values. */
    CorruptionEvidence getEvidence();
  }

  /**
   * Types of memory corruption.
   */
  enum CorruptionType {
    /** Buffer overflow detected. */
    BUFFER_OVERFLOW,
    /** Use after free detected. */
    USE_AFTER_FREE,
    /** Double free detected. */
    DOUBLE_FREE,
    /** Invalid pointer dereference. */
    INVALID_POINTER,
    /** Heap metadata corruption. */
    HEAP_METADATA_CORRUPTION,
    /** Type information corruption. */
    TYPE_INFO_CORRUPTION,
    /** Reference count corruption. */
    REFERENCE_COUNT_CORRUPTION,
    /** Object header corruption. */
    OBJECT_HEADER_CORRUPTION,
    /** Memory layout corruption. */
    LAYOUT_CORRUPTION
  }

  /**
   * Severity levels for corruption.
   */
  enum CorruptionSeverity {
    /** Potential corruption that needs investigation. */
    POTENTIAL,
    /** Minor corruption with limited impact. */
    MINOR,
    /** Moderate corruption that may cause issues. */
    MODERATE,
    /** Serious corruption that will cause problems. */
    SERIOUS,
    /** Critical corruption that may crash the system. */
    CRITICAL
  }

  /**
   * Evidence supporting a corruption detection.
   */
  interface CorruptionEvidence {
    /** Gets the expected value. */
    Object getExpectedValue();

    /** Gets the actual value found. */
    Object getActualValue();

    /** Gets the memory location. */
    long getMemoryLocation();

    /** Gets the size of the corrupted region. */
    int getCorruptedSize();

    /** Gets additional forensic information. */
    Map<String, Object> getForensicData();
  }

  /**
   * Results of memory integrity checking.
   */
  interface MemoryIntegrityResult {
    /** Gets whether memory integrity is intact. */
    boolean isIntegrityIntact();

    /** Gets the number of integrity violations. */
    int getViolationCount();

    /** Gets detailed violation information. */
    List<IntegrityViolation> getViolations();

    /** Gets the integrity score (0.0 to 1.0). */
    double getIntegrityScore();

    /** Gets checksum validation results. */
    Map<String, Boolean> getChecksumResults();
  }

  /**
   * An integrity violation.
   */
  interface IntegrityViolation {
    /** Gets the violation type. */
    String getViolationType();

    /** Gets the memory region affected. */
    MemoryRegion getAffectedRegion();

    /** Gets the expected integrity value. */
    String getExpectedValue();

    /** Gets the actual integrity value. */
    String getActualValue();

    /** Gets the violation severity. */
    ViolationSeverity getSeverity();
  }

  /**
   * Results of heap consistency checking.
   */
  interface HeapConsistencyResult {
    /** Gets whether the heap is consistent. */
    boolean isConsistent();

    /** Gets the number of consistency errors. */
    int getErrorCount();

    /** Gets detailed error information. */
    List<ConsistencyError> getErrors();

    /** Gets free list validation results. */
    FreeListValidation getFreeListValidation();

    /** Gets object graph validation results. */
    ObjectGraphValidation getObjectGraphValidation();
  }

  /**
   * A heap consistency error.
   */
  interface ConsistencyError {
    /** Gets the error type. */
    String getErrorType();

    /** Gets the error description. */
    String getDescription();

    /** Gets the affected objects. */
    List<Long> getAffectedObjects();

    /** Gets the error severity. */
    ErrorSeverity getSeverity();
  }

  /**
   * Results of lifecycle violation checking.
   */
  interface LifecycleViolationResult {
    /** Gets whether lifecycle violations were found. */
    boolean hasViolations();

    /** Gets the violation count. */
    int getViolationCount();

    /** Gets detailed violation information. */
    List<LifecycleViolation> getViolations();

    /** Gets object state validation results. */
    Map<Long, ObjectStateValidation> getStateValidations();
  }

  /**
   * A lifecycle violation.
   */
  interface LifecycleViolation {
    /** Gets the violation type. */
    LifecycleViolationType getViolationType();

    /** Gets the affected object ID. */
    long getObjectId();

    /** Gets the current object state. */
    String getCurrentState();

    /** Gets the expected object state. */
    String getExpectedState();

    /** Gets the violation description. */
    String getDescription();
  }

  /**
   * Types of lifecycle violations.
   */
  enum LifecycleViolationType {
    /** Object used after finalization. */
    USE_AFTER_FINALIZATION,
    /** Object accessed during finalization. */
    ACCESS_DURING_FINALIZATION,
    /** Invalid state transition. */
    INVALID_STATE_TRANSITION,
    /** Resource leak detected. */
    RESOURCE_LEAK,
    /** Premature cleanup. */
    PREMATURE_CLEANUP
  }

  /**
   * A memory region description.
   */
  interface MemoryRegion {
    /** Gets the start address. */
    long getStartAddress();

    /** Gets the end address. */
    long getEndAddress();

    /** Gets the region size. */
    long getSize();

    /** Gets the region type. */
    String getRegionType();

    /** Gets the region permissions. */
    Set<MemoryPermission> getPermissions();
  }

  /**
   * Memory permissions.
   */
  enum MemoryPermission {
    /** Read permission. */
    READ,
    /** Write permission. */
    WRITE,
    /** Execute permission. */
    EXECUTE
  }

  /**
   * Validation of the free list structure.
   */
  interface FreeListValidation {
    /** Gets whether the free list is valid. */
    boolean isValid();

    /** Gets the number of free blocks. */
    int getFreeBlockCount();

    /** Gets the total free space. */
    long getTotalFreeSpace();

    /** Gets free list errors. */
    List<String> getErrors();
  }

  /**
   * Validation of the object reference graph.
   */
  interface ObjectGraphValidation {
    /** Gets whether the object graph is valid. */
    boolean isValid();

    /** Gets the number of orphaned objects. */
    int getOrphanedObjectCount();

    /** Gets the number of circular references. */
    int getCircularReferenceCount();

    /** Gets graph validation errors. */
    List<String> getErrors();
  }

  /**
   * Validation of object state.
   */
  interface ObjectStateValidation {
    /** Gets whether the object state is valid. */
    boolean isValid();

    /** Gets the current object state. */
    String getCurrentState();

    /** Gets state validation errors. */
    List<String> getErrors();

    /** Gets the state transition history. */
    List<String> getStateHistory();
  }

  /**
   * Severity levels for various violations and errors.
   */
  enum ViolationSeverity {
    /** Informational level. */
    INFO,
    /** Warning level. */
    WARNING,
    /** Error level. */
    ERROR,
    /** Critical level. */
    CRITICAL
  }

  /**
   * Error severity levels.
   */
  enum ErrorSeverity {
    /** Minor error. */
    MINOR,
    /** Major error. */
    MAJOR,
    /** Critical error. */
    CRITICAL
  }

  /**
   * A recommendation for addressing memory corruption.
   */
  interface CorruptionRecommendation {
    /** Gets the recommendation type. */
    RecommendationType getType();

    /** Gets the recommendation description. */
    String getDescription();

    /** Gets the affected areas. */
    List<String> getAffectedAreas();

    /** Gets the implementation priority. */
    Priority getPriority();

    /** Gets the expected impact. */
    String getExpectedImpact();
  }

  /**
   * Types of corruption recommendations.
   */
  enum RecommendationType {
    /** Add bounds checking. */
    ADD_BOUNDS_CHECKING,
    /** Improve memory management. */
    IMPROVE_MEMORY_MANAGEMENT,
    /** Add corruption detection. */
    ADD_CORRUPTION_DETECTION,
    /** Fix lifecycle management. */
    FIX_LIFECYCLE_MANAGEMENT,
    /** Implement memory barriers. */
    IMPLEMENT_MEMORY_BARRIERS,
    /** Add integrity checking. */
    ADD_INTEGRITY_CHECKING
  }

  /**
   * Recommendation priorities.
   */
  enum Priority {
    /** Low priority. */
    LOW,
    /** Medium priority. */
    MEDIUM,
    /** High priority. */
    HIGH,
    /** Critical priority. */
    CRITICAL
  }
}