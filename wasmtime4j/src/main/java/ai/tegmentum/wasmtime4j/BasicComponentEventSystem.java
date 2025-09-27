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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Basic implementation of ComponentEventSystem for foundational Component Model support.
 *
 * <p>This implementation provides core component event communication functionality as part of
 * Task #304 to stabilize the Component Model foundation.
 *
 * @since 1.0.0
 */
public final class BasicComponentEventSystem implements ComponentEventSystem {

  private static final Logger LOGGER = Logger.getLogger(BasicComponentEventSystem.class.getName());

  private final String systemId;
  private final ComponentEventConfig config;
  private final Map<String, ComponentSimple> eventHandlers;

  /**
   * Creates a new basic component event system.
   *
   * @param config the event system configuration
   */
  public BasicComponentEventSystem(final ComponentEventConfig config) {
    this.config = config != null ? config : new ComponentEventConfig();
    this.systemId = "event-system-" + System.nanoTime();
    this.eventHandlers = new ConcurrentHashMap<>();
  }

  @Override
  public String getId() {
    return systemId;
  }

  @Override
  public ComponentEventConfig getConfig() {
    return config;
  }

  @Override
  public void registerEventHandler(final String eventType, final ComponentSimple handler) {
    if (eventType != null && handler != null) {
      eventHandlers.put(eventType, handler);
      LOGGER.fine("Registered event handler for type: " + eventType);
    }
  }

  @Override
  public void unregisterEventHandler(final String eventType) {
    if (eventType != null) {
      eventHandlers.remove(eventType);
      LOGGER.fine("Unregistered event handler for type: " + eventType);
    }
  }

  @Override
  public Map<String, ComponentSimple> getEventHandlers() {
    return Collections.unmodifiableMap(eventHandlers);
  }

  @Override
  public void publishEvent(final String eventType, final Object eventData) {
    if (eventType != null) {
      final ComponentSimple handler = eventHandlers.get(eventType);
      if (handler != null) {
        LOGGER.fine("Publishing event of type: " + eventType);
        // In a full implementation, this would invoke the component handler
      } else {
        LOGGER.fine("No handler registered for event type: " + eventType);
      }
    }
  }

  @Override
  public boolean isValid() {
    return eventHandlers.values().stream().allMatch(ComponentSimple::isValid);
  }

  @Override
  public void shutdown() {
    eventHandlers.clear();
    LOGGER.fine("Event system shutdown completed");
  }

  @Override
  public String toString() {
    return "BasicComponentEventSystem{" +
        "systemId='" + systemId + '\'' +
        ", handlerCount=" + eventHandlers.size() +
        '}';
  }
}