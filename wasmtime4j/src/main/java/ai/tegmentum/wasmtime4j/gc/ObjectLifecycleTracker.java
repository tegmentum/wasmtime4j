package ai.tegmentum.wasmtime4j.gc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Tracks the lifecycle of WebAssembly GC objects for debugging and profiling.
 *
 * <p>Provides detailed information about object creation, access patterns, reference changes, and
 * garbage collection events for a specific set of tracked objects.
 *
 * @since 1.0.0
 */
public interface ObjectLifecycleTracker {

  /**
   * Gets the list of objects being tracked.
   *
   * @return set of tracked object IDs
   */
  List<Long> getTrackedObjects();

  /**
   * Gets the lifecycle events for a specific object.
   *
   * @param objectId the object ID
   * @return list of lifecycle events in chronological order
   */
  List<LifecycleEvent> getLifecycleEvents(long objectId);

  /**
   * Gets the current status of all tracked objects.
   *
   * @return mapping from object ID to current status
   */
  Map<Long, ObjectStatus> getObjectStatuses();

  /**
   * Gets access statistics for tracked objects.
   *
   * @return mapping from object ID to access statistics
   */
  Map<Long, AccessStatistics> getAccessStatistics();

  /**
   * Gets reference change history for tracked objects.
   *
   * @return mapping from object ID to reference change history
   */
  Map<Long, List<ReferenceChange>> getReferenceHistory();

  /**
   * Stops tracking all objects and returns a summary report.
   *
   * @return lifecycle tracking summary
   */
  LifecycleTrackingSummary stopTracking();

  /**
   * Adds additional objects to track.
   *
   * @param objects objects to start tracking
   */
  void trackAdditionalObjects(List<GcObject> objects);

  /**
   * Removes objects from tracking.
   *
   * @param objectIds object IDs to stop tracking
   */
  void stopTrackingObjects(List<Long> objectIds);

  /** A lifecycle event for a tracked object. */
  interface LifecycleEvent {
    /** Gets the event timestamp. */
    Instant getTimestamp();

    /** Gets the event type. */
    LifecycleEventType getEventType();

    /** Gets the object ID. */
    long getObjectId();

    /** Gets event-specific details. */
    String getDetails();

    /** Gets the thread ID where the event occurred. */
    long getThreadId();
  }

  /** Types of lifecycle events. */
  enum LifecycleEventType {
    /** Object was created. */
    CREATED,
    /** Object was accessed (read from). */
    ACCESSED,
    /** Object was modified (written to). */
    MODIFIED,
    /** Reference to object was added. */
    REFERENCE_ADDED,
    /** Reference to object was removed. */
    REFERENCE_REMOVED,
    /** Object became unreachable from roots. */
    UNREACHABLE,
    /** Object was garbage collected. */
    COLLECTED
  }

  /** Current status of a tracked object. */
  interface ObjectStatus {
    /** Gets the object ID. */
    long getObjectId();

    /** Checks if the object is still alive. */
    boolean isAlive();

    /** Gets the number of references to the object. */
    int getReferenceCount();

    /** Gets the last access timestamp. */
    Instant getLastAccessed();

    /** Gets the creation timestamp. */
    Instant getCreationTime();

    /** Gets the object type. */
    GcReferenceType getObjectType();
  }

  /** Access statistics for a tracked object. */
  interface AccessStatistics {
    /** Gets the object ID. */
    long getObjectId();

    /** Gets the total number of read accesses. */
    long getReadCount();

    /** Gets the total number of write accesses. */
    long getWriteCount();

    /** Gets the number of unique threads that accessed the object. */
    int getAccessingThreadCount();

    /** Gets the first access timestamp. */
    Instant getFirstAccess();

    /** Gets the last access timestamp. */
    Instant getLastAccess();

    /** Gets the average time between accesses. */
    long getAverageAccessInterval();
  }

  /** A reference change event. */
  interface ReferenceChange {
    /** Gets the change timestamp. */
    Instant getTimestamp();

    /** Gets the change type. */
    ReferenceChangeType getChangeType();

    /** Gets the source object ID (the object holding the reference). */
    long getSourceObjectId();

    /** Gets the target object ID (the referenced object). */
    long getTargetObjectId();

    /** Gets the field or element index if applicable. */
    int getIndex();

    /** Gets additional change details. */
    String getDetails();
  }

  /** Types of reference changes. */
  enum ReferenceChangeType {
    /** A new reference was created. */
    REFERENCE_CREATED,
    /** An existing reference was removed. */
    REFERENCE_REMOVED,
    /** A reference was updated to point to a different object. */
    REFERENCE_UPDATED
  }

  /** Summary of lifecycle tracking results. */
  interface LifecycleTrackingSummary {
    /** Gets the tracking duration. */
    long getTrackingDurationMillis();

    /** Gets the number of objects tracked. */
    int getTrackedObjectCount();

    /** Gets the number of objects that were collected during tracking. */
    int getCollectedObjectCount();

    /** Gets the total number of lifecycle events recorded. */
    long getTotalEventCount();

    /** Gets the most frequently accessed objects. */
    List<Long> getMostAccessedObjects();

    /** Gets the longest-lived objects. */
    List<Long> getLongestLivedObjects();

    /** Gets objects that may have memory leaks. */
    List<Long> getPotentialLeaks();
  }
}
