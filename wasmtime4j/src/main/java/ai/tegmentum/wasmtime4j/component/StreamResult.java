/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of a stream read operation.
 *
 * <p>Contains the values read from the stream and a flag indicating whether the stream has been
 * closed by the writer.
 *
 * @since 1.1.0
 */
public final class StreamResult {

  private final List<ComponentVal> values;
  private final boolean closed;

  /**
   * Creates a new stream result.
   *
   * @param values the values read from the stream (may be empty)
   * @param closed true if the stream was closed by the writer
   */
  public StreamResult(final List<ComponentVal> values, final boolean closed) {
    this.values = values != null ? Collections.unmodifiableList(values) : Collections.emptyList();
    this.closed = closed;
  }

  /**
   * Gets the values read from the stream.
   *
   * @return an unmodifiable list of values (may be empty)
   */
  public List<ComponentVal> getValues() {
    return values;
  }

  /**
   * Checks if the stream was closed by the writer.
   *
   * @return true if no more values will be produced
   */
  public boolean isClosed() {
    return closed;
  }

  /**
   * Checks if any values were read.
   *
   * @return true if at least one value was read
   */
  public boolean hasValues() {
    return !values.isEmpty();
  }

  /**
   * Gets the number of values read.
   *
   * @return the count of values
   */
  public int size() {
    return values.size();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StreamResult)) {
      return false;
    }
    final StreamResult that = (StreamResult) o;
    return closed == that.closed && Objects.equals(values, that.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(values, closed);
  }

  @Override
  public String toString() {
    return "StreamResult{values=" + values.size() + ", closed=" + closed + "}";
  }
}
