package ai.tegmentum.wasmtime4j.component;

import java.util.OptionalLong;

/**
 * Size and alignment information for component types.
 *
 * <p>ComponentTypeSize provides detailed information about the memory layout, size requirements,
 * and alignment constraints for component types. This information is crucial for efficient
 * memory management and data layout optimization.
 *
 * @since 1.0.0
 */
public interface ComponentTypeSize {

  /**
   * Gets the fixed size of this type in bytes.
   *
   * <p>Returns the exact size if this type has a fixed size, or empty if the size is variable
   * or cannot be determined statically.
   *
   * @return fixed size in bytes, or empty if variable size
   */
  OptionalLong getFixedSize();

  /**
   * Gets the minimum size of this type in bytes.
   *
   * <p>Returns the smallest possible size for values of this type, including any header or
   * metadata overhead.
   *
   * @return minimum size in bytes
   */
  long getMinimumSize();

  /**
   * Gets the maximum size of this type in bytes.
   *
   * <p>Returns the largest possible size for values of this type, or empty if there is no
   * practical upper bound.
   *
   * @return maximum size in bytes, or empty if unbounded
   */
  OptionalLong getMaximumSize();

  /**
   * Gets the alignment requirement for this type.
   *
   * <p>Returns the memory alignment boundary that values of this type must be placed on for
   * optimal performance and correctness.
   *
   * @return alignment requirement in bytes
   */
  long getAlignment();

  /**
   * Checks if this type has a variable size.
   *
   * <p>Returns true if the size of values of this type can vary at runtime (e.g., strings,
   * lists, or dynamic structures).
   *
   * @return true if variable size, false if fixed size
   */
  boolean isVariableSize();

  /**
   * Checks if this type requires stack allocation.
   *
   * <p>Returns true if values of this type should preferably be allocated on the stack rather
   * than the heap for performance reasons.
   *
   * @return true if stack allocation is preferred, false otherwise
   */
  boolean prefersStackAllocation();

  /**
   * Gets the estimated overhead for heap allocation.
   *
   * <p>Returns the additional memory overhead (headers, metadata, etc.) required when
   * allocating values of this type on the heap.
   *
   * @return heap allocation overhead in bytes
   */
  long getHeapAllocationOverhead();

  /**
   * Gets the estimated size for a value with specific characteristics.
   *
   * <p>Calculates the expected size for a value of this type given specific parameters such as
   * element count, string length, or other size-determining factors.
   *
   * @param sizeParameters parameters that affect the size calculation
   * @return estimated size in bytes
   * @throws IllegalArgumentException if sizeParameters is null or invalid
   */
  long estimateSize(final ComponentSizeParameters sizeParameters);

  /**
   * Checks if two values of this type can share memory.
   *
   * <p>Returns true if multiple values of this type can safely share the same memory allocation
   * without conflicts or corruption.
   *
   * @return true if memory sharing is safe, false otherwise
   */
  boolean supportsMemorySharing();

  /**
   * Gets padding requirements for this type.
   *
   * <p>Returns information about padding that must be added when this type is used as part of
   * larger structures or arrays.
   *
   * @return padding requirements
   */
  ComponentPaddingInfo getPaddingInfo();

  /**
   * Gets cache line efficiency information.
   *
   * <p>Returns metrics about how efficiently this type utilizes CPU cache lines, which affects
   * performance in cache-sensitive scenarios.
   *
   * @return cache efficiency metrics
   */
  ComponentCacheEfficiency getCacheEfficiency();
}