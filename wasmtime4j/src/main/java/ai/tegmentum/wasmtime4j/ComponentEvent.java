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

package ai.tegmentum.wasmtime4j;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an event in the component event system.
 *
 * <p>Events are used for communication between WebAssembly components, carrying data and metadata
 * about component interactions.
 *
 * @since 1.0.0
 */
public final class ComponentEvent {

  private final String eventId;
  private final String eventType;
  private final String sourceComponentId;
  private final String targetComponentId;
  private final Object payload;
  private final Map<String, String> metadata;
  private final Instant timestamp;
  private final int priority;

  /**
   * Creates a new component event.
   *
   * @param eventType the type of event
   * @param sourceComponentId the source component identifier
   * @param payload the event payload
   */
  public ComponentEvent(
      final String eventType, final String sourceComponentId, final Object payload) {
    this(eventType, sourceComponentId, null, payload, Map.of(), 0);
  }

  /**
   * Creates a new component event with full configuration.
   *
   * @param eventType the type of event
   * @param sourceComponentId the source component identifier
   * @param targetComponentId the target component identifier (may be null for broadcast)
   * @param payload the event payload
   * @param metadata additional event metadata
   * @param priority event priority (higher is more urgent)
   */
  public ComponentEvent(
      final String eventType,
      final String sourceComponentId,
      final String targetComponentId,
      final Object payload,
      final Map<String, String> metadata,
      final int priority) {
    this.eventId = UUID.randomUUID().toString();
    this.eventType = Objects.requireNonNull(eventType, "eventType cannot be null");
    this.sourceComponentId =
        Objects.requireNonNull(sourceComponentId, "sourceComponentId cannot be null");
    this.targetComponentId = targetComponentId;
    this.payload = payload;
    this.metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata cannot be null"));
    this.timestamp = Instant.now();
    this.priority = priority;
  }

  /**
   * Gets the unique event identifier.
   *
   * @return event ID
   */
  public String getEventId() {
    return eventId;
  }

  /**
   * Gets the event type.
   *
   * @return event type
   */
  public String getEventType() {
    return eventType;
  }

  /**
   * Gets the source component identifier.
   *
   * @return source component ID
   */
  public String getSourceComponentId() {
    return sourceComponentId;
  }

  /**
   * Gets the target component identifier.
   *
   * @return target component ID, or null if broadcast
   */
  public String getTargetComponentId() {
    return targetComponentId;
  }

  /**
   * Gets the event payload.
   *
   * @return event payload
   */
  public Object getPayload() {
    return payload;
  }

  /**
   * Gets the event metadata.
   *
   * @return immutable map of metadata
   */
  public Map<String, String> getMetadata() {
    return metadata;
  }

  /**
   * Gets the event timestamp.
   *
   * @return timestamp when event was created
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Gets the event priority.
   *
   * @return event priority (higher is more urgent)
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Checks if this event is targeted to a specific component.
   *
   * @return true if event has a specific target
   */
  public boolean isTargeted() {
    return targetComponentId != null;
  }

  /**
   * Creates a builder for component events.
   *
   * @param eventType the event type
   * @param sourceComponentId the source component identifier
   * @return new builder
   */
  public static Builder builder(final String eventType, final String sourceComponentId) {
    return new Builder(eventType, sourceComponentId);
  }

  /** Builder for component events. */
  public static final class Builder {
    private final String eventType;
    private final String sourceComponentId;
    private String targetComponentId;
    private Object payload;
    private Map<String, String> metadata = Map.of();
    private int priority = 0;

    private Builder(final String eventType, final String sourceComponentId) {
      this.eventType = eventType;
      this.sourceComponentId = sourceComponentId;
    }

    /**
     * Sets the target component identifier.
     *
     * @param targetComponentId target component ID
     * @return this builder
     */
    public Builder targetComponentId(final String targetComponentId) {
      this.targetComponentId = targetComponentId;
      return this;
    }

    /**
     * Sets the event payload.
     *
     * @param payload event payload
     * @return this builder
     */
    public Builder payload(final Object payload) {
      this.payload = payload;
      return this;
    }

    /**
     * Sets the event metadata.
     *
     * @param metadata event metadata
     * @return this builder
     */
    public Builder metadata(final Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    /**
     * Sets the event priority.
     *
     * @param priority event priority
     * @return this builder
     */
    public Builder priority(final int priority) {
      this.priority = priority;
      return this;
    }

    /**
     * Builds the component event.
     *
     * @return new event instance
     */
    public ComponentEvent build() {
      return new ComponentEvent(
          eventType, sourceComponentId, targetComponentId, payload, metadata, priority);
    }
  }
}
