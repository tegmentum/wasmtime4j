package ai.tegmentum.wasmtime4j.gc;

import java.time.Instant;
import java.util.List;

/**
 * Results of a memory leak detection analysis for WebAssembly GC objects.
 *
 * <p>Provides detailed information about potential memory leaks, including objects that should have
 * been garbage collected but are still reachable, circular references, and other memory leak
 * patterns.
 *
 * @since 1.0.0
 */
public interface MemoryLeakAnalysis {

  /**
   * Gets the analysis timestamp.
   *
   * @return when the analysis was performed
   */
  Instant getAnalysisTime();

  /**
   * Gets the total number of objects analyzed.
   *
   * @return the total object count
   */
  long getTotalObjectCount();

  /**
   * Gets the number of potentially leaked objects found.
   *
   * @return the potential leak count
   */
  int getPotentialLeakCount();

  /**
   * Gets detailed information about potential memory leaks.
   *
   * @return list of potential leak information
   */
  List<PotentialLeak> getPotentialLeaks();

  /**
   * Gets circular reference cycles that may prevent garbage collection.
   *
   * @return list of circular reference cycles
   */
  List<CircularReference> getCircularReferences();

  /**
   * Gets objects that have been alive for an unusually long time.
   *
   * @return list of long-lived objects
   */
  List<LongLivedObject> getLongLivedObjects();

  /**
   * Gets objects with unusually high reference counts.
   *
   * @return list of highly referenced objects
   */
  List<HighlyReferencedObject> getHighlyReferencedObjects();

  /**
   * Gets memory usage trends that may indicate leaks.
   *
   * @return memory usage trend analysis
   */
  MemoryUsageTrend getMemoryUsageTrend();

  /**
   * Gets leak severity assessment.
   *
   * @return severity assessment
   */
  LeakSeverity getLeakSeverity();

  /**
   * Gets recommendations for addressing detected leaks.
   *
   * @return list of recommendations
   */
  List<LeakRecommendation> getRecommendations();

  /** Information about a potential memory leak. */
  interface PotentialLeak {
    /** Gets the object ID. */
    long getObjectId();

    /** Gets the leak confidence score (0.0 to 1.0). */
    double getConfidence();

    /** Gets the leak type. */
    LeakType getLeakType();

    /** Gets the object type. */
    GcReferenceType getObjectType();

    /** Gets the object creation time. */
    Instant getCreationTime();

    /** Gets the time since last access. */
    long getTimeSinceLastAccess();

    /** Gets the current reference count. */
    int getReferenceCount();

    /** Gets the reason why this object is considered a potential leak. */
    String getReason();

    /** Gets the reference path keeping the object alive. */
    List<Long> getReferencePath();
  }

  /** Information about a circular reference. */
  interface CircularReference {
    /** Gets the objects involved in the cycle. */
    List<Long> getCycleObjects();

    /** Gets the cycle length. */
    int getCycleLength();

    /** Gets whether this cycle is preventing garbage collection. */
    boolean isBlockingGc();

    /** Gets the reference path forming the cycle. */
    List<ReferencePathElement> getReferencePath();
  }

  /** Information about a long-lived object. */
  interface LongLivedObject {
    /** Gets the object ID. */
    long getObjectId();

    /** Gets the object age in milliseconds. */
    long getAgeMillis();

    /** Gets the object type. */
    GcReferenceType getObjectType();

    /** Gets the number of accesses over its lifetime. */
    long getAccessCount();

    /** Gets the last access time. */
    Instant getLastAccess();

    /** Gets whether this object is likely to be legitimately long-lived. */
    boolean isLegitimate();
  }

  /** Information about a highly referenced object. */
  interface HighlyReferencedObject {
    /** Gets the object ID. */
    long getObjectId();

    /** Gets the current reference count. */
    int getReferenceCount();

    /** Gets the object type. */
    GcReferenceType getObjectType();

    /** Gets the average reference count for objects of this type. */
    double getAverageReferenceCount();

    /** Gets whether the high reference count is suspicious. */
    boolean isSuspicious();
  }

  /** Memory usage trend analysis. */
  interface MemoryUsageTrend {
    /** Gets whether memory usage is increasing over time. */
    boolean isIncreasing();

    /** Gets the rate of memory growth (bytes per second). */
    double getGrowthRate();

    /** Gets the correlation between time and memory usage. */
    double getCorrelation();

    /** Gets the projected time until memory exhaustion. */
    long getTimeToExhaustionMillis();

    /** Gets whether the growth pattern indicates a leak. */
    boolean isLeakPattern();
  }

  /** Element of a reference path. */
  interface ReferencePathElement {
    /** Gets the source object ID. */
    long getSourceObjectId();

    /** Gets the target object ID. */
    long getTargetObjectId();

    /** Gets the reference type. */
    String getReferenceType();

    /** Gets the field or element index. */
    int getIndex();
  }

  /** Types of memory leaks. */
  enum LeakType {
    /** Unreachable objects that should be collected. */
    UNREACHABLE_OBJECT,
    /** Circular reference preventing collection. */
    CIRCULAR_REFERENCE,
    /** Object kept alive by expired weak references. */
    WEAK_REFERENCE_LEAK,
    /** Long-lived object that should be temporary. */
    LONG_LIVED_TEMPORARY,
    /** Cache or collection growing without bounds. */
    UNBOUNDED_GROWTH,
    /** Resource not properly closed or released. */
    RESOURCE_LEAK
  }

  /** Severity of detected leaks. */
  enum LeakSeverity {
    /** Minor leaks with low impact. */
    LOW,
    /** Moderate leaks that should be addressed. */
    MODERATE,
    /** Serious leaks that may cause problems. */
    HIGH,
    /** Critical leaks that will cause memory exhaustion. */
    CRITICAL
  }

  /** Recommendation for addressing a memory leak. */
  interface LeakRecommendation {
    /** Gets the recommendation type. */
    RecommendationType getType();

    /** Gets the recommendation description. */
    String getDescription();

    /** Gets the affected objects. */
    List<Long> getAffectedObjects();

    /** Gets the expected impact of following this recommendation. */
    String getExpectedImpact();

    /** Gets the priority of this recommendation. */
    RecommendationPriority getPriority();
  }

  /** Types of leak recommendations. */
  enum RecommendationType {
    /** Break circular references. */
    BREAK_CYCLE,
    /** Clear expired weak references. */
    CLEAR_WEAK_REFERENCES,
    /** Implement proper resource cleanup. */
    IMPLEMENT_CLEANUP,
    /** Add bounds to growing collections. */
    ADD_BOUNDS,
    /** Review object lifecycle management. */
    REVIEW_LIFECYCLE,
    /** Tune garbage collection parameters. */
    TUNE_GC
  }

  /** Priority levels for recommendations. */
  enum RecommendationPriority {
    /** Should be addressed immediately. */
    URGENT,
    /** Should be addressed soon. */
    HIGH,
    /** Should be addressed when convenient. */
    MEDIUM,
    /** Can be addressed later. */
    LOW
  }
}
