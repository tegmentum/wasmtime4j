package ai.tegmentum.wasmtime4j.gc;

import java.util.List;
import java.util.Map;

/**
 * Results of a GC heap inspection operation.
 *
 * <p>Provides detailed information about the current state of the WebAssembly GC heap, including
 * object counts, type distributions, memory usage, and reference relationships.
 *
 * @since 1.0.0
 */
public interface GcHeapInspection {

  /**
   * Gets the total number of objects in the heap.
   *
   * @return the total object count
   */
  long getTotalObjectCount();

  /**
   * Gets the total heap size in bytes.
   *
   * @return the total heap size
   */
  long getTotalHeapSize();

  /**
   * Gets the amount of used heap memory in bytes.
   *
   * @return the used heap memory
   */
  long getUsedHeapSize();

  /**
   * Gets the amount of free heap memory in bytes.
   *
   * @return the free heap memory
   */
  long getFreeHeapSize();

  /**
   * Gets the distribution of objects by type.
   *
   * @return mapping from type name to object count
   */
  Map<String, Long> getObjectTypeDistribution();

  /**
   * Gets the distribution of memory usage by type.
   *
   * @return mapping from type name to memory usage in bytes
   */
  Map<String, Long> getMemoryUsageByType();

  /**
   * Gets information about all struct instances in the heap.
   *
   * @return list of struct instance information
   */
  List<StructInstanceInfo> getStructInstances();

  /**
   * Gets information about all array instances in the heap.
   *
   * @return list of array instance information
   */
  List<ArrayInstanceInfo> getArrayInstances();

  /**
   * Gets information about all I31 instances in the heap.
   *
   * @return list of I31 instance information
   */
  List<I31InstanceInfo> getI31Instances();

  /**
   * Gets the reference graph showing object relationships.
   *
   * @return the reference graph
   */
  ReferenceGraph getReferenceGraph();

  /**
   * Gets statistics about garbage collection activity.
   *
   * @return GC statistics
   */
  GcStats getGcStats();

  /**
   * Gets heap fragmentation information.
   *
   * @return fragmentation analysis
   */
  HeapFragmentation getFragmentationInfo();

  /**
   * Gets information about root objects (GC roots).
   *
   * @return list of root object information
   */
  List<RootObjectInfo> getRootObjects();

  /** Information about a struct instance in the heap. */
  interface StructInstanceInfo {
    /** Gets the object ID. */
    long getObjectId();

    /** Gets the struct type. */
    StructType getStructType();

    /** Gets the object size in bytes. */
    int getSize();

    /** Gets the field values. */
    List<GcValue> getFieldValues();

    /** Gets incoming reference count. */
    int getIncomingReferences();

    /** Gets outgoing reference count. */
    int getOutgoingReferences();
  }

  /** Information about an array instance in the heap. */
  interface ArrayInstanceInfo {
    /** Gets the object ID. */
    long getObjectId();

    /** Gets the array type. */
    ArrayType getArrayType();

    /** Gets the object size in bytes. */
    int getSize();

    /** Gets the array length. */
    int getLength();

    /** Gets the element values. */
    List<GcValue> getElements();

    /** Gets incoming reference count. */
    int getIncomingReferences();

    /** Gets outgoing reference count. */
    int getOutgoingReferences();
  }

  /** Information about an I31 instance in the heap. */
  interface I31InstanceInfo {
    /** Gets the object ID. */
    long getObjectId();

    /** Gets the I31 value. */
    int getValue();

    /** Gets incoming reference count. */
    int getIncomingReferences();
  }

  /** Information about heap fragmentation. */
  interface HeapFragmentation {
    /** Gets the fragmentation ratio (0.0 to 1.0). */
    double getFragmentationRatio();

    /** Gets the number of free blocks. */
    int getFreeBlockCount();

    /** Gets the largest free block size. */
    long getLargestFreeBlock();

    /** Gets the average free block size. */
    long getAverageFreeBlockSize();
  }

  /** Information about a root object. */
  interface RootObjectInfo {
    /** Gets the object ID. */
    long getObjectId();

    /** Gets the root type (e.g., "stack", "global", "call_ref"). */
    String getRootType();

    /** Gets the root location description. */
    String getRootLocation();

    /** Gets the object type. */
    GcReferenceType getObjectType();
  }
}
