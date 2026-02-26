package ai.tegmentum.wasmtime4j.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A buffer for batching Component Model values for stream I/O operations.
 *
 * <p>VecBuffer provides an efficient way to accumulate {@link ComponentVal} instances before
 * writing them to a stream or processing them after a batch read. It manages capacity and provides
 * push/drain semantics.
 *
 * @since 1.1.0
 */
public final class VecBuffer {

  private final List<ComponentVal> buffer;

  /**
   * Creates a new VecBuffer with the specified initial capacity.
   *
   * @param capacity the initial capacity
   * @return a new VecBuffer
   * @throws IllegalArgumentException if capacity is negative
   */
  public static VecBuffer withCapacity(final int capacity) {
    if (capacity < 0) {
      throw new IllegalArgumentException("Capacity cannot be negative: " + capacity);
    }
    return new VecBuffer(capacity);
  }

  private VecBuffer(final int capacity) {
    this.buffer = new ArrayList<>(capacity);
  }

  /**
   * Adds a value to the buffer.
   *
   * @param val the value to add
   * @throws IllegalArgumentException if val is null
   */
  public void push(final ComponentVal val) {
    if (val == null) {
      throw new IllegalArgumentException("ComponentVal cannot be null");
    }
    buffer.add(val);
  }

  /**
   * Removes and returns all values from the buffer.
   *
   * <p>After this call, the buffer will be empty.
   *
   * @return an unmodifiable list of all values that were in the buffer
   */
  public List<ComponentVal> drain() {
    final List<ComponentVal> result = Collections.unmodifiableList(new ArrayList<>(buffer));
    buffer.clear();
    return result;
  }

  /**
   * Returns the number of values in the buffer.
   *
   * @return the buffer size
   */
  public int size() {
    return buffer.size();
  }

  /**
   * Checks if the buffer is empty.
   *
   * @return true if the buffer contains no values
   */
  public boolean isEmpty() {
    return buffer.isEmpty();
  }
}
