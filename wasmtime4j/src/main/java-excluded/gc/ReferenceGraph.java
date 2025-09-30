package ai.tegmentum.wasmtime4j.gc;

import java.util.List;
import java.util.Set;

/**
 * Represents the reference graph of objects in the WebAssembly GC heap.
 *
 * <p>The reference graph shows the relationships between objects, including which objects
 * reference which other objects. This is useful for debugging memory leaks and understanding
 * object lifecycle dependencies.
 *
 * @since 1.0.0
 */
public interface ReferenceGraph {

  /**
   * Gets all objects in the reference graph.
   *
   * @return set of all object IDs
   */
  Set<Long> getAllObjects();

  /**
   * Gets all objects that the specified object references.
   *
   * @param objectId the object ID
   * @return set of referenced object IDs
   */
  Set<Long> getOutgoingReferences(long objectId);

  /**
   * Gets all objects that reference the specified object.
   *
   * @param objectId the object ID
   * @return set of referencing object IDs
   */
  Set<Long> getIncomingReferences(long objectId);

  /**
   * Gets the reference path from a root object to the specified object.
   *
   * @param objectId the target object ID
   * @return list of object IDs forming the reference path, or empty if unreachable
   */
  List<Long> getPathFromRoot(long objectId);

  /**
   * Gets all objects that are reachable from the specified object.
   *
   * @param objectId the starting object ID
   * @return set of reachable object IDs
   */
  Set<Long> getReachableObjects(long objectId);

  /**
   * Gets all objects that can reach the specified object.
   *
   * @param objectId the target object ID
   * @return set of objects that can reach the target
   */
  Set<Long> getObjectsReaching(long objectId);

  /**
   * Finds strongly connected components in the reference graph.
   *
   * @return list of strongly connected components
   */
  List<Set<Long>> findStronglyConnectedComponents();

  /**
   * Finds potential memory leaks (objects that should be garbage collected but aren't).
   *
   * @return set of potentially leaked object IDs
   */
  Set<Long> findPotentialLeaks();

  /**
   * Gets detailed information about a specific reference edge.
   *
   * @param fromObjectId the source object ID
   * @param toObjectId the target object ID
   * @return reference edge information
   */
  ReferenceEdgeInfo getReferenceEdge(long fromObjectId, long toObjectId);

  /**
   * Information about a reference edge between two objects.
   */
  interface ReferenceEdgeInfo {
    /** Gets the source object ID. */
    long getFromObjectId();

    /** Gets the target object ID. */
    long getToObjectId();

    /** Gets the reference type (e.g., "field", "element", "root"). */
    String getReferenceType();

    /** Gets the field index or element index if applicable. */
    int getIndex();

    /** Gets additional reference information. */
    String getDescription();
  }
}