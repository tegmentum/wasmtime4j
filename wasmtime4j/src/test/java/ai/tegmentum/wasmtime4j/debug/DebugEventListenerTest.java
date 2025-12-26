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

package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.DebugEventListener.DebugEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DebugEventListener} interface.
 *
 * <p>DebugEventListener provides callback methods for WebAssembly debug events including
 * breakpoints, pause/resume, exceptions, and session lifecycle.
 */
@DisplayName("DebugEventListener Tests")
class DebugEventListenerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          DebugEventListener.class.isInterface(), "DebugEventListener should be an interface");
    }

    @Test
    @DisplayName("should have onBreakpoint method")
    void shouldHaveOnBreakpointMethod() throws NoSuchMethodException {
      final Method method = DebugEventListener.class.getMethod("onBreakpoint", DebugEvent.class);
      assertNotNull(method, "onBreakpoint method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("should have onPaused method")
    void shouldHaveOnPausedMethod() throws NoSuchMethodException {
      final Method method = DebugEventListener.class.getMethod("onPaused", DebugEvent.class);
      assertNotNull(method, "onPaused method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("should have onResumed method")
    void shouldHaveOnResumedMethod() throws NoSuchMethodException {
      final Method method = DebugEventListener.class.getMethod("onResumed", DebugEvent.class);
      assertNotNull(method, "onResumed method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("should have onException method")
    void shouldHaveOnExceptionMethod() throws NoSuchMethodException {
      final Method method = DebugEventListener.class.getMethod("onException", DebugEvent.class);
      assertNotNull(method, "onException method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("should have onSessionEnd method")
    void shouldHaveOnSessionEndMethod() throws NoSuchMethodException {
      final Method method = DebugEventListener.class.getMethod("onSessionEnd", DebugEvent.class);
      assertNotNull(method, "onSessionEnd method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
    }

    @Test
    @DisplayName("should have exactly five listener methods")
    void shouldHaveExactlyFiveListenerMethods() {
      int listenerMethodCount = 0;
      for (final Method method : DebugEventListener.class.getDeclaredMethods()) {
        if (method.getParameterCount() == 1
            && method.getParameterTypes()[0] == DebugEvent.class
            && method.getReturnType() == void.class) {
          listenerMethodCount++;
        }
      }
      assertEquals(5, listenerMethodCount, "Should have exactly 5 listener methods");
    }
  }

  @Nested
  @DisplayName("DebugEvent Nested Interface Tests")
  class DebugEventInterfaceTests {

    @Test
    @DisplayName("should have DebugEvent nested interface")
    void shouldHaveDebugEventNestedInterface() {
      boolean hasDebugEvent = false;
      for (final Class<?> inner : DebugEventListener.class.getDeclaredClasses()) {
        if ("DebugEvent".equals(inner.getSimpleName()) && inner.isInterface()) {
          hasDebugEvent = true;
          break;
        }
      }
      assertTrue(hasDebugEvent, "Should have DebugEvent nested interface");
    }

    @Test
    @DisplayName("DebugEvent should have getEventType method")
    void debugEventShouldHaveGetEventTypeMethod() throws NoSuchMethodException {
      final Method method = DebugEvent.class.getMethod("getEventType");
      assertNotNull(method, "getEventType method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("DebugEvent should have getTimestamp method")
    void debugEventShouldHaveGetTimestampMethod() throws NoSuchMethodException {
      final Method method = DebugEvent.class.getMethod("getTimestamp");
      assertNotNull(method, "getTimestamp method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("DebugEvent should have getEventData method")
    void debugEventShouldHaveGetEventDataMethod() throws NoSuchMethodException {
      final Method method = DebugEvent.class.getMethod("getEventData");
      assertNotNull(method, "getEventData method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("DebugEvent should have getSessionId method")
    void debugEventShouldHaveGetSessionIdMethod() throws NoSuchMethodException {
      final Method method = DebugEvent.class.getMethod("getSessionId");
      assertNotNull(method, "getSessionId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("DebugEvent should have exactly four methods")
    void debugEventShouldHaveExactlyFourMethods() {
      final Method[] methods = DebugEvent.class.getDeclaredMethods();
      assertEquals(4, methods.length, "DebugEvent should have exactly 4 methods");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock listener should receive breakpoint events")
    void mockListenerShouldReceiveBreakpointEvents() {
      final MockDebugEventListener listener = new MockDebugEventListener();
      final MockDebugEvent event = new MockDebugEvent("BREAKPOINT", "session-1");

      listener.onBreakpoint(event);

      assertEquals(1, listener.getBreakpointEvents().size(), "Should have 1 breakpoint event");
      assertEquals(
          "BREAKPOINT",
          listener.getBreakpointEvents().get(0).getEventType(),
          "Event type should match");
    }

    @Test
    @DisplayName("mock listener should receive paused events")
    void mockListenerShouldReceivePausedEvents() {
      final MockDebugEventListener listener = new MockDebugEventListener();
      final MockDebugEvent event = new MockDebugEvent("PAUSED", "session-2");

      listener.onPaused(event);

      assertEquals(1, listener.getPausedEvents().size(), "Should have 1 paused event");
      assertEquals(
          "session-2", listener.getPausedEvents().get(0).getSessionId(), "Session ID should match");
    }

    @Test
    @DisplayName("mock listener should receive resumed events")
    void mockListenerShouldReceiveResumedEvents() {
      final MockDebugEventListener listener = new MockDebugEventListener();
      final MockDebugEvent event = new MockDebugEvent("RESUMED", "session-3");

      listener.onResumed(event);

      assertEquals(1, listener.getResumedEvents().size(), "Should have 1 resumed event");
    }

    @Test
    @DisplayName("mock listener should receive exception events")
    void mockListenerShouldReceiveExceptionEvents() {
      final MockDebugEventListener listener = new MockDebugEventListener();
      final MockDebugEvent event = new MockDebugEvent("EXCEPTION", "session-4");
      event.setEventData("RuntimeError: division by zero");

      listener.onException(event);

      assertEquals(1, listener.getExceptionEvents().size(), "Should have 1 exception event");
      assertEquals(
          "RuntimeError: division by zero",
          listener.getExceptionEvents().get(0).getEventData(),
          "Event data should contain exception info");
    }

    @Test
    @DisplayName("mock listener should receive session end events")
    void mockListenerShouldReceiveSessionEndEvents() {
      final MockDebugEventListener listener = new MockDebugEventListener();
      final MockDebugEvent event = new MockDebugEvent("SESSION_END", "session-5");

      listener.onSessionEnd(event);

      assertEquals(1, listener.getSessionEndEvents().size(), "Should have 1 session end event");
    }

    @Test
    @DisplayName("mock listener should track all event types")
    void mockListenerShouldTrackAllEventTypes() {
      final MockDebugEventListener listener = new MockDebugEventListener();

      listener.onBreakpoint(new MockDebugEvent("BREAKPOINT", "s1"));
      listener.onPaused(new MockDebugEvent("PAUSED", "s1"));
      listener.onResumed(new MockDebugEvent("RESUMED", "s1"));
      listener.onException(new MockDebugEvent("EXCEPTION", "s1"));
      listener.onSessionEnd(new MockDebugEvent("SESSION_END", "s1"));

      assertEquals(5, listener.getTotalEventCount(), "Should have 5 total events");
    }

    @Test
    @DisplayName("mock event should return correct timestamp")
    void mockEventShouldReturnCorrectTimestamp() {
      final long before = System.currentTimeMillis();
      final MockDebugEvent event = new MockDebugEvent("TEST", "session");
      final long after = System.currentTimeMillis();

      assertTrue(event.getTimestamp() >= before, "Timestamp should be >= test start time");
      assertTrue(event.getTimestamp() <= after, "Timestamp should be <= test end time");
    }
  }

  /** Mock implementation of DebugEventListener for testing. */
  private static class MockDebugEventListener implements DebugEventListener {
    private final List<DebugEvent> breakpointEvents = new ArrayList<>();
    private final List<DebugEvent> pausedEvents = new ArrayList<>();
    private final List<DebugEvent> resumedEvents = new ArrayList<>();
    private final List<DebugEvent> exceptionEvents = new ArrayList<>();
    private final List<DebugEvent> sessionEndEvents = new ArrayList<>();

    @Override
    public void onBreakpoint(final DebugEvent event) {
      breakpointEvents.add(event);
    }

    @Override
    public void onPaused(final DebugEvent event) {
      pausedEvents.add(event);
    }

    @Override
    public void onResumed(final DebugEvent event) {
      resumedEvents.add(event);
    }

    @Override
    public void onException(final DebugEvent event) {
      exceptionEvents.add(event);
    }

    @Override
    public void onSessionEnd(final DebugEvent event) {
      sessionEndEvents.add(event);
    }

    public List<DebugEvent> getBreakpointEvents() {
      return breakpointEvents;
    }

    public List<DebugEvent> getPausedEvents() {
      return pausedEvents;
    }

    public List<DebugEvent> getResumedEvents() {
      return resumedEvents;
    }

    public List<DebugEvent> getExceptionEvents() {
      return exceptionEvents;
    }

    public List<DebugEvent> getSessionEndEvents() {
      return sessionEndEvents;
    }

    public int getTotalEventCount() {
      return breakpointEvents.size()
          + pausedEvents.size()
          + resumedEvents.size()
          + exceptionEvents.size()
          + sessionEndEvents.size();
    }
  }

  /** Mock implementation of DebugEvent for testing. */
  private static class MockDebugEvent implements DebugEvent {
    private final String eventType;
    private final long timestamp;
    private String eventData;
    private final String sessionId;

    MockDebugEvent(final String eventType, final String sessionId) {
      this.eventType = eventType;
      this.timestamp = System.currentTimeMillis();
      this.eventData = "";
      this.sessionId = sessionId;
    }

    @Override
    public String getEventType() {
      return eventType;
    }

    @Override
    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public String getEventData() {
      return eventData;
    }

    public void setEventData(final String eventData) {
      this.eventData = eventData;
    }

    @Override
    public String getSessionId() {
      return sessionId;
    }
  }
}
