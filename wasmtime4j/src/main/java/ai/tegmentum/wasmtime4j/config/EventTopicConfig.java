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

package ai.tegmentum.wasmtime4j.config;

import java.util.Objects;

/**
 * Configuration for an event topic.
 *
 * <p>This class defines the settings for an event topic including delivery guarantees and
 * persistence options.
 *
 * @since 1.0.0
 */
public final class EventTopicConfig {

  private final String topicName;
  private final int maxSubscribers;
  private final boolean enablePersistence;
  private final DeliveryGuarantee deliveryGuarantee;

  /** Delivery guarantee levels for event topics. */
  public enum DeliveryGuarantee {
    /** At most once delivery - messages may be lost. */
    AT_MOST_ONCE,
    /** At least once delivery - messages may be duplicated. */
    AT_LEAST_ONCE,
    /** Exactly once delivery - messages delivered exactly once. */
    EXACTLY_ONCE
  }

  /**
   * Creates a new event topic configuration with default values.
   *
   * @param topicName the topic name
   */
  public EventTopicConfig(final String topicName) {
    this(topicName, 100, false, DeliveryGuarantee.AT_LEAST_ONCE);
  }

  /**
   * Creates a new event topic configuration.
   *
   * @param topicName the topic name
   * @param maxSubscribers maximum number of subscribers
   * @param enablePersistence whether to persist events
   * @param deliveryGuarantee the delivery guarantee level
   */
  public EventTopicConfig(
      final String topicName,
      final int maxSubscribers,
      final boolean enablePersistence,
      final DeliveryGuarantee deliveryGuarantee) {
    this.topicName = Objects.requireNonNull(topicName, "topicName cannot be null");
    this.maxSubscribers = maxSubscribers;
    this.enablePersistence = enablePersistence;
    this.deliveryGuarantee =
        Objects.requireNonNull(deliveryGuarantee, "deliveryGuarantee cannot be null");
  }

  /**
   * Gets the topic name.
   *
   * @return topic name
   */
  public String getTopicName() {
    return topicName;
  }

  /**
   * Gets the maximum number of subscribers.
   *
   * @return maximum subscribers
   */
  public int getMaxSubscribers() {
    return maxSubscribers;
  }

  /**
   * Checks if persistence is enabled.
   *
   * @return true if persistence is enabled
   */
  public boolean isPersistenceEnabled() {
    return enablePersistence;
  }

  /**
   * Gets the delivery guarantee level.
   *
   * @return delivery guarantee
   */
  public DeliveryGuarantee getDeliveryGuarantee() {
    return deliveryGuarantee;
  }
}
