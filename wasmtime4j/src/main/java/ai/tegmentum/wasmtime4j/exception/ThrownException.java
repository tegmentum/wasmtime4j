/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.exception;

import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.Tag;
import ai.tegmentum.wasmtime4j.WasmValue;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an exception that was thrown during WebAssembly execution.
 *
 * <p>This class provides access to exception details when a WebAssembly exception is thrown,
 * including the exception tag, payload values, and the exception reference if available.
 *
 * <p>ThrownException is used in conjunction with the WebAssembly exception handling proposal to
 * inspect exceptions caught during execution or exceptions that propagated out of WebAssembly code.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try {
 *     instance.call("function_that_throws");
 * } catch (WasmTrapException e) {
 *     Optional<ThrownException> thrown = e.getThrownException();
 *     if (thrown.isPresent()) {
 *         ThrownException tex = thrown.get();
 *         System.out.println("Tag: " + tex.getTag());
 *         System.out.println("Payload: " + tex.getPayload());
 *     }
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public final class ThrownException {

  private final Tag tag;
  private final List<WasmValue> payload;
  private final ExnRef exnRef;

  /**
   * Creates a new ThrownException.
   *
   * @param tag the exception tag
   * @param payload the exception payload values
   * @param exnRef the exception reference, may be null
   */
  public ThrownException(final Tag tag, final List<WasmValue> payload, final ExnRef exnRef) {
    this.tag = Objects.requireNonNull(tag, "tag cannot be null");
    this.payload =
        payload != null ? Collections.unmodifiableList(payload) : Collections.emptyList();
    this.exnRef = exnRef;
  }

  /**
   * Creates a new ThrownException without an ExnRef.
   *
   * @param tag the exception tag
   * @param payload the exception payload values
   */
  public ThrownException(final Tag tag, final List<WasmValue> payload) {
    this(tag, payload, null);
  }

  /**
   * Gets the exception tag.
   *
   * <p>The tag identifies the type of exception that was thrown. Tags are defined in the
   * WebAssembly module and can be used to distinguish between different exception types.
   *
   * @return the exception tag
   */
  public Tag getTag() {
    return tag;
  }

  /**
   * Gets the exception payload.
   *
   * <p>The payload contains the values that were passed when the exception was thrown. The types
   * and number of values are determined by the tag's type signature.
   *
   * @return an unmodifiable list of payload values
   */
  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP",
      justification = "Payload is already unmodifiable via Collections.unmodifiableList")
  public List<WasmValue> getPayload() {
    return payload;
  }

  /**
   * Gets the exception reference if available.
   *
   * <p>The ExnRef provides a handle to the exception object in WebAssembly memory, which can be
   * used to rethrow or further inspect the exception.
   *
   * @return an Optional containing the ExnRef, or empty if not available
   */
  public Optional<ExnRef> getExnRef() {
    return Optional.ofNullable(exnRef);
  }

  /**
   * Checks if this exception has a payload.
   *
   * @return true if the exception has a non-empty payload
   */
  public boolean hasPayload() {
    return !payload.isEmpty();
  }

  /**
   * Gets the payload value at the specified index.
   *
   * @param index the index of the payload value
   * @return the payload value
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public WasmValue getPayloadValue(final int index) {
    return payload.get(index);
  }

  /**
   * Gets the number of values in the payload.
   *
   * @return the payload size
   */
  public int getPayloadSize() {
    return payload.size();
  }

  @Override
  public String toString() {
    return "ThrownException{tag="
        + tag
        + ", payload="
        + payload
        + ", hasExnRef="
        + (exnRef != null)
        + "}";
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ThrownException other = (ThrownException) obj;
    return Objects.equals(tag, other.tag)
        && Objects.equals(payload, other.payload)
        && Objects.equals(exnRef, other.exnRef);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tag, payload, exnRef);
  }

  /**
   * Creates a builder for constructing ThrownException instances.
   *
   * @param tag the exception tag
   * @return a new builder
   */
  public static Builder builder(final Tag tag) {
    return new Builder(tag);
  }

  /** Builder for ThrownException instances. */
  public static final class Builder {
    private final Tag tag;
    private List<WasmValue> payload = Collections.emptyList();
    private ExnRef exnRef;

    private Builder(final Tag tag) {
      this.tag = Objects.requireNonNull(tag, "tag cannot be null");
    }

    /**
     * Sets the exception payload.
     *
     * @param payload the payload values
     * @return this builder
     */
    public Builder payload(final List<WasmValue> payload) {
      this.payload = payload != null ? new java.util.ArrayList<>(payload) : null;
      return this;
    }

    /**
     * Sets the exception reference.
     *
     * @param exnRef the exception reference
     * @return this builder
     */
    public Builder exnRef(final ExnRef exnRef) {
      this.exnRef = exnRef;
      return this;
    }

    /**
     * Builds the ThrownException.
     *
     * @return a new ThrownException instance
     */
    public ThrownException build() {
      return new ThrownException(tag, payload, exnRef);
    }
  }
}
