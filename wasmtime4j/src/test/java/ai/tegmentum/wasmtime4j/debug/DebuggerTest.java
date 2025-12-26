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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.DebugEventListener.DebugEvent;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Debugger} interface.
 *
 * <p>Debugger provides the core interface for WebAssembly debugging capabilities including creating
 * sessions, attaching to instances, and managing debug events.
 */
@DisplayName("Debugger Tests")
class DebuggerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(Debugger.class.isInterface(), "Debugger should be an interface");
    }

    @Test
    @DisplayName("should have createSession method")
    void shouldHaveCreateSessionMethod() throws NoSuchMethodException {
      final Method method = Debugger.class.getMethod("createSession", DebugConfig.class);
      assertNotNull(method, "createSession method should exist");
      assertEquals(DebugSession.class, method.getReturnType(), "Should return DebugSession");
    }

    @Test
    @DisplayName("should have attach method")
    void shouldHaveAttachMethod() throws NoSuchMethodException {
      final Method method = Debugger.class.getMethod("attach", String.class);
      assertNotNull(method, "attach method should exist");
      assertEquals(DebugSession.class, method.getReturnType(), "Should return DebugSession");
    }

    @Test
    @DisplayName("should have detach method")
    void shouldHaveDetachMethod() throws NoSuchMethodException {
      final Method method = Debugger.class.getMethod("detach");
      assertNotNull(method, "detach method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getDebuggerName method")
    void shouldHaveGetDebuggerNameMethod() throws NoSuchMethodException {
      final Method method = Debugger.class.getMethod("getDebuggerName");
      assertNotNull(method, "getDebuggerName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = Debugger.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have setEventListener method")
    void shouldHaveSetEventListenerMethod() throws NoSuchMethodException {
      final Method method = Debugger.class.getMethod("setEventListener", DebugEventListener.class);
      assertNotNull(method, "setEventListener method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getCurrentSession method")
    void shouldHaveGetCurrentSessionMethod() throws NoSuchMethodException {
      final Method method = Debugger.class.getMethod("getCurrentSession");
      assertNotNull(method, "getCurrentSession method should exist");
      assertEquals(DebugSession.class, method.getReturnType(), "Should return DebugSession");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock debugger should implement all methods")
    void mockDebuggerShouldImplementAllMethods() {
      final Debugger mockDebugger = new MockDebugger();

      assertNotNull(mockDebugger.getDebuggerName(), "getDebuggerName should not return null");
      assertFalse(mockDebugger.isEnabled(), "isEnabled should return default value");
      assertNull(
          mockDebugger.getCurrentSession(), "getCurrentSession should return null initially");
    }

    @Test
    @DisplayName("mock debugger should create session")
    void mockDebuggerShouldCreateSession() {
      final Debugger mockDebugger = new MockDebugger();
      final DebugConfig mockConfig = new MockDebugConfig();

      final DebugSession session = mockDebugger.createSession(mockConfig);
      assertNotNull(session, "createSession should return a session");
    }

    @Test
    @DisplayName("mock debugger should attach to instance")
    void mockDebuggerShouldAttachToInstance() {
      final Debugger mockDebugger = new MockDebugger();

      final DebugSession session = mockDebugger.attach("test-instance-123");
      assertNotNull(session, "attach should return a session");
    }

    @Test
    @DisplayName("mock debugger should handle event listener")
    void mockDebuggerShouldHandleEventListener() {
      final MockDebugger mockDebugger = new MockDebugger();
      final DebugEventListener listener = new MockDebugEventListener();

      mockDebugger.setEventListener(listener);
      assertTrue(mockDebugger.hasListener(), "Listener should be set");
    }
  }

  /** Mock implementation of DebugEventListener for testing. */
  private static class MockDebugEventListener implements DebugEventListener {
    @Override
    public void onBreakpoint(final DebugEvent event) {
      // Mock implementation
    }

    @Override
    public void onPaused(final DebugEvent event) {
      // Mock implementation
    }

    @Override
    public void onResumed(final DebugEvent event) {
      // Mock implementation
    }

    @Override
    public void onException(final DebugEvent event) {
      // Mock implementation
    }

    @Override
    public void onSessionEnd(final DebugEvent event) {
      // Mock implementation
    }
  }

  /** Mock implementation of Debugger for testing. */
  private static class MockDebugger implements Debugger {
    private DebugSession currentSession;
    private DebugEventListener listener;

    @Override
    public DebugSession createSession(final DebugConfig config) {
      currentSession = new MockDebugSession("session-" + System.currentTimeMillis());
      return currentSession;
    }

    @Override
    public DebugSession attach(final String instanceId) {
      currentSession = new MockDebugSession("attached-" + instanceId);
      return currentSession;
    }

    @Override
    public void detach() {
      currentSession = null;
    }

    @Override
    public String getDebuggerName() {
      return "MockDebugger";
    }

    @Override
    public boolean isEnabled() {
      return false;
    }

    @Override
    public void setEventListener(final DebugEventListener listener) {
      this.listener = listener;
    }

    @Override
    public DebugSession getCurrentSession() {
      return currentSession;
    }

    public boolean hasListener() {
      return listener != null;
    }
  }

  /** Mock implementation of DebugSession for testing. */
  private static class MockDebugSession implements DebugSession {
    private final String sessionId;
    private boolean active;

    MockDebugSession(final String sessionId) {
      this.sessionId = sessionId;
      this.active = false;
    }

    @Override
    public void start() {
      active = true;
    }

    @Override
    public void stop() {
      active = false;
    }

    @Override
    public void step(final StepType stepType) {
      // Mock step
    }

    @Override
    public void continueExecution() {
      // Mock continue
    }

    @Override
    public void addBreakpoint(final Breakpoint breakpoint) {
      // Mock add breakpoint
    }

    @Override
    public void removeBreakpoint(final Breakpoint breakpoint) {
      // Mock remove breakpoint
    }

    @Override
    public String getSessionId() {
      return sessionId;
    }

    @Override
    public boolean isActive() {
      return active;
    }
  }

  /** Mock implementation of DebugConfig for testing. */
  private static class MockDebugConfig implements DebugConfig {
    @Override
    public int getDebugPort() {
      return 9229;
    }

    @Override
    public String getHostAddress() {
      return "localhost";
    }

    @Override
    public boolean isRemoteDebuggingEnabled() {
      return false;
    }

    @Override
    public long getSessionTimeout() {
      return 30000;
    }

    @Override
    public boolean isBreakpointsEnabled() {
      return true;
    }

    @Override
    public int getMaxBreakpoints() {
      return 100;
    }

    @Override
    public boolean isStepDebuggingEnabled() {
      return true;
    }

    @Override
    public String getLogLevel() {
      return "INFO";
    }
  }
}
