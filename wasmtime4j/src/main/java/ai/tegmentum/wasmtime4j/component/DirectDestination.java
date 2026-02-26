package ai.tegmentum.wasmtime4j.component;

import java.util.List;

/**
 * A direct destination that provides synchronous access for writing values.
 *
 * <p>DirectDestination extends {@link Destination} with the ability to write values directly
 * without going through the asynchronous write path. This is useful for destinations that can
 * immediately accept values without buffering.
 *
 * @since 1.1.0
 */
public interface DirectDestination extends Destination {

  /**
   * Directly writes values to the destination without suspending.
   *
   * <p>Returns the number of values actually written. If the destination cannot accept any values
   * immediately, returns 0 without blocking.
   *
   * @param values the values to write
   * @return the number of values written
   * @throws IllegalArgumentException if values is null
   */
  int writeDirect(List<ComponentVal> values);
}
