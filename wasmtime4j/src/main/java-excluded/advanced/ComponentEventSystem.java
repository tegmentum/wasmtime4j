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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Event-driven communication system for components.
 *
 * <p>The ComponentEventSystem enables asynchronous communication between components
 * through publish-subscribe patterns, direct event routing, and event filtering.
 * It supports both local and distributed event communication.
 *
 * @since 1.0.0
 */
public interface ComponentEventSystem extends AutoCloseable {

  /**
   * Gets the event system identifier.
   *
   * @return the event system ID
   */
  String getId();

  /**
   * Gets the event system configuration.
   *
   * @return the configuration
   */
  ComponentEventConfig getConfiguration();

  /**
   * Publishes an event to all subscribers.
   *
   * @param event the event to publish
   * @return future that completes when publishing is done
   * @throws WasmException if publishing fails
   */
  CompletableFuture<EventPublishResult> publishEvent(ComponentEvent event) throws WasmException;

  /**
   * Publishes an event to a specific topic.
   *
   * @param topic the topic to publish to
   * @param event the event to publish
   * @return future that completes when publishing is done
   * @throws WasmException if publishing fails
   */
  CompletableFuture<EventPublishResult> publishEvent(String topic, ComponentEvent event) throws WasmException;

  /**
   * Subscribes a component to events from a specific topic.
   *
   * @param topic the topic to subscribe to
   * @param subscriber the subscribing component
   * @param handler the event handler
   * @return subscription handle for managing the subscription
   * @throws WasmException if subscription fails
   */
  EventSubscription subscribe(String topic, ComponentSimple subscriber, EventHandler handler) throws WasmException;

  /**
   * Subscribes a component to events with a filter.
   *
   * @param subscriber the subscribing component
   * @param filter the event filter
   * @param handler the event handler
   * @return subscription handle for managing the subscription
   * @throws WasmException if subscription fails
   */
  EventSubscription subscribe(ComponentSimple subscriber, EventFilter filter, EventHandler handler) throws WasmException;

  /**
   * Unsubscribes from events using a subscription handle.
   *
   * @param subscription the subscription to cancel
   * @throws WasmException if unsubscription fails
   */
  void unsubscribe(EventSubscription subscription) throws WasmException;

  /**
   * Unsubscribes a component from all events.
   *
   * @param component the component to unsubscribe
   * @throws WasmException if unsubscription fails
   */
  void unsubscribeAll(ComponentSimple component) throws WasmException;

  /**
   * Sends a direct event to a specific component.
   *
   * @param targetComponent the target component
   * @param event the event to send
   * @return future that completes when the event is delivered
   * @throws WasmException if sending fails
   */
  CompletableFuture<EventDeliveryResult> sendDirectEvent(ComponentSimple targetComponent, ComponentEvent event) throws WasmException;

  /**
   * Sends a request event and waits for a response.
   *
   * @param targetComponent the target component
   * @param request the request event
   * @return future containing the response event
   * @throws WasmException if request fails
   */
  CompletableFuture<ComponentEvent> sendRequestEvent(ComponentSimple targetComponent, ComponentEvent request) throws WasmException;

  /**
   * Creates a new topic for event communication.
   *
   * @param topicName the name of the topic
   * @param topicConfig the topic configuration
   * @return the created event topic
   * @throws WasmException if topic creation fails
   */
  EventTopic createTopic(String topicName, EventTopicConfig topicConfig) throws WasmException;

  /**
   * Removes a topic and all its subscriptions.
   *
   * @param topicName the name of the topic to remove
   * @throws WasmException if topic removal fails
   */
  void removeTopic(String topicName) throws WasmException;

  /**
   * Gets all available topics.
   *
   * @return set of available topic names
   */
  Set<String> getTopics();

  /**
   * Gets information about a specific topic.
   *
   * @param topicName the topic name
   * @return topic information, if the topic exists
   */
  Optional<EventTopic> getTopicInfo(String topicName);

  /**
   * Gets all active subscriptions.
   *
   * @return list of active subscriptions
   */
  List<EventSubscription> getActiveSubscriptions();

  /**
   * Gets subscriptions for a specific component.
   *
   * @param component the component
   * @return list of subscriptions for the component
   */
  List<EventSubscription> getSubscriptions(ComponentSimple component);

  /**
   * Gets event system statistics.
   *
   * @return event system statistics
   */
  EventSystemStatistics getStatistics();

  /**
   * Starts the event system.
   *
   * @throws WasmException if startup fails
   */
  void start() throws WasmException;

  /**
   * Stops the event system and completes pending operations.
   *
   * @throws WasmException if shutdown fails
   */
  void stop() throws WasmException;

  /**
   * Checks if the event system is running.
   *
   * @return true if running
   */
  boolean isRunning();

  /**
   * Sets the global event filter.
   *
   * @param filter the global event filter
   */
  void setGlobalEventFilter(EventFilter filter);

  /**
   * Removes the global event filter.
   */
  void removeGlobalEventFilter();

  /**
   * Adds an event interceptor for monitoring or modifying events.
   *
   * @param interceptor the event interceptor
   */
  void addEventInterceptor(EventInterceptor interceptor);

  /**
   * Removes an event interceptor.
   *
   * @param interceptor the event interceptor to remove
   */
  void removeEventInterceptor(EventInterceptor interceptor);

  @Override
  void close();

  /**
   * Event handler interface for processing received events.
   */
  @FunctionalInterface
  interface EventHandler {
    /**
     * Handles a received event.
     *
     * @param event the received event
     * @param context the event handling context
     * @return future that completes when handling is done
     */
    CompletableFuture<Void> handleEvent(ComponentEvent event, EventContext context);
  }

  /**
   * Event filter interface for filtering events.
   */
  @FunctionalInterface
  interface EventFilter {
    /**
     * Tests if an event should be processed.
     *
     * @param event the event to test
     * @return true if the event should be processed
     */
    boolean test(ComponentEvent event);

    /**
     * Creates a filter that matches events by topic.
     *
     * @param topic the topic to match
     * @return topic filter
     */
    static EventFilter byTopic(String topic) {
      return event -> topic.equals(event.getTopic());
    }

    /**
     * Creates a filter that matches events by source component.
     *
     * @param sourceComponent the source component to match
     * @return source component filter
     */
    static EventFilter bySource(ComponentSimple sourceComponent) {
      return event -> sourceComponent.getId().equals(event.getSource());
    }

    /**
     * Creates a filter that matches events by event type.
     *
     * @param eventType the event type to match
     * @return event type filter
     */
    static EventFilter byType(String eventType) {
      return event -> eventType.equals(event.getType());
    }

    /**
     * Combines this filter with another using AND logic.
     *
     * @param other the other filter
     * @return combined filter
     */
    default EventFilter and(EventFilter other) {
      return event -> this.test(event) && other.test(event);
    }

    /**
     * Combines this filter with another using OR logic.
     *
     * @param other the other filter
     * @return combined filter
     */
    default EventFilter or(EventFilter other) {
      return event -> this.test(event) || other.test(event);
    }

    /**
     * Negates this filter.
     *
     * @return negated filter
     */
    default EventFilter negate() {
      return event -> !this.test(event);
    }
  }

  /**
   * Event interceptor interface for monitoring or modifying events.
   */
  interface EventInterceptor {
    /**
     * Intercepts an event before it is published.
     *
     * @param event the event being published
     * @return the event to actually publish (may be modified)
     */
    ComponentEvent beforePublish(ComponentEvent event);

    /**
     * Intercepts an event after it is published.
     *
     * @param event the event that was published
     * @param result the publish result
     */
    void afterPublish(ComponentEvent event, EventPublishResult result);

    /**
     * Intercepts an event before it is delivered to a handler.
     *
     * @param event the event being delivered
     * @param handler the target handler
     * @return the event to actually deliver (may be modified)
     */
    ComponentEvent beforeDeliver(ComponentEvent event, EventHandler handler);

    /**
     * Intercepts an event after it is delivered to a handler.
     *
     * @param event the event that was delivered
     * @param handler the handler that processed it
     * @param result the handling result
     */
    void afterDeliver(ComponentEvent event, EventHandler handler, CompletableFuture<Void> result);
  }

  /**
   * Event subscription handle.
   */
  interface EventSubscription {
    /**
     * Gets the subscription identifier.
     *
     * @return the subscription ID
     */
    String getId();

    /**
     * Gets the subscribing component.
     *
     * @return the subscribing component
     */
    ComponentSimple getComponent();

    /**
     * Gets the subscription topic, if any.
     *
     * @return the topic name, if subscribed to a specific topic
     */
    Optional<String> getTopic();

    /**
     * Gets the event filter.
     *
     * @return the event filter
     */
    EventFilter getFilter();

    /**
     * Gets the event handler.
     *
     * @return the event handler
     */
    EventHandler getHandler();

    /**
     * Gets the subscription creation time.
     *
     * @return the creation timestamp
     */
    Instant getCreatedAt();

    /**
     * Gets the number of events processed by this subscription.
     *
     * @return the processed event count
     */
    long getProcessedEventCount();

    /**
     * Checks if the subscription is active.
     *
     * @return true if active
     */
    boolean isActive();

    /**
     * Pauses the subscription.
     */
    void pause();

    /**
     * Resumes the subscription.
     */
    void resume();

    /**
     * Cancels the subscription.
     */
    void cancel();
  }

  /**
   * Event topic information.
   */
  interface EventTopic {
    /**
     * Gets the topic name.
     *
     * @return the topic name
     */
    String getName();

    /**
     * Gets the topic configuration.
     *
     * @return the topic configuration
     */
    EventTopicConfig getConfiguration();

    /**
     * Gets the number of subscribers to this topic.
     *
     * @return the subscriber count
     */
    int getSubscriberCount();

    /**
     * Gets the total number of events published to this topic.
     *
     * @return the total event count
     */
    long getTotalEventCount();

    /**
     * Gets the topic creation time.
     *
     * @return the creation timestamp
     */
    Instant getCreatedAt();

    /**
     * Checks if the topic is active.
     *
     * @return true if active
     */
    boolean isActive();
  }

  /**
   * Event system statistics.
   */
  interface EventSystemStatistics {
    /**
     * Gets the total number of events published.
     *
     * @return the total published events
     */
    long getTotalEventsPublished();

    /**
     * Gets the total number of events delivered.
     *
     * @return the total delivered events
     */
    long getTotalEventsDelivered();

    /**
     * Gets the total number of active subscriptions.
     *
     * @return the active subscription count
     */
    int getActiveSubscriptionCount();

    /**
     * Gets the total number of topics.
     *
     * @return the topic count
     */
    int getTopicCount();

    /**
     * Gets the average event processing time.
     *
     * @return the average processing time in milliseconds
     */
    double getAverageProcessingTime();

    /**
     * Gets events published per second.
     *
     * @return the event throughput
     */
    double getEventThroughput();

    /**
     * Gets the error rate for event processing.
     *
     * @return the error rate as a percentage
     */
    double getErrorRate();

    /**
     * Gets statistics by topic.
     *
     * @return map of topic name to topic statistics
     */
    Map<String, TopicStatistics> getTopicStatistics();

    /**
     * Topic-specific statistics.
     */
    interface TopicStatistics {
      String getTopicName();
      long getEventCount();
      int getSubscriberCount();
      double getAverageDeliveryTime();
      double getErrorRate();
    }
  }

  /**
   * Event context providing information about the event handling environment.
   */
  interface EventContext {
    /**
     * Gets the subscription that received the event.
     *
     * @return the subscription
     */
    EventSubscription getSubscription();

    /**
     * Gets the event delivery timestamp.
     *
     * @return the delivery timestamp
     */
    Instant getDeliveryTime();

    /**
     * Gets additional context properties.
     *
     * @return context properties
     */
    Map<String, Object> getProperties();

    /**
     * Acknowledges successful event processing.
     */
    void acknowledge();

    /**
     * Rejects the event and requests redelivery.
     *
     * @param reason the rejection reason
     */
    void reject(String reason);
  }

  /**
   * Result of event publishing operation.
   */
  interface EventPublishResult {
    /**
     * Checks if publishing was successful.
     *
     * @return true if successful
     */
    boolean isSuccessful();

    /**
     * Gets the number of subscribers that received the event.
     *
     * @return the delivery count
     */
    int getDeliveryCount();

    /**
     * Gets the publishing timestamp.
     *
     * @return the publish timestamp
     */
    Instant getPublishTime();

    /**
     * Gets any error that occurred during publishing.
     *
     * @return the error, if any
     */
    Optional<Exception> getError();

    /**
     * Gets additional result metadata.
     *
     * @return result metadata
     */
    Map<String, Object> getMetadata();
  }

  /**
   * Result of direct event delivery.
   */
  interface EventDeliveryResult {
    /**
     * Checks if delivery was successful.
     *
     * @return true if successful
     */
    boolean isSuccessful();

    /**
     * Gets the delivery timestamp.
     *
     * @return the delivery timestamp
     */
    Instant getDeliveryTime();

    /**
     * Gets the processing time in milliseconds.
     *
     * @return the processing time
     */
    long getProcessingTime();

    /**
     * Gets any error that occurred during delivery.
     *
     * @return the error, if any
     */
    Optional<Exception> getError();
  }
}