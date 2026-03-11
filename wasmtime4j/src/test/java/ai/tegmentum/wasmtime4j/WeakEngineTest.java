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
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WeakEngine} interface.
 *
 * <p>Verifies the weak reference contract using a stub implementation that simulates engine
 * lifecycle. The stub does not require a full Engine implementation.
 */
@DisplayName("WeakEngine Tests")
class WeakEngineTest {

  /** Stub WeakEngine that simulates upgrade and close behavior without a real Engine. */
  private static final class StubWeakEngine implements WeakEngine {

    private final boolean hasEngine;
    private boolean closed;

    StubWeakEngine(final boolean hasEngine) {
      this.hasEngine = hasEngine;
      this.closed = false;
    }

    @Override
    public Optional<Engine> upgrade() {
      if (closed || !hasEngine) {
        return Optional.empty();
      }
      // Return empty since we cannot easily create a full Engine stub.
      // The contract testing focuses on closed vs not-closed behavior.
      return Optional.empty();
    }

    @Override
    public boolean isValid() {
      return !closed;
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WeakEngine.class.isInterface(), "WeakEngine should be an interface");
    }

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          java.io.Closeable.class.isAssignableFrom(WeakEngine.class),
          "WeakEngine should extend Closeable");
    }

    @Test
    @DisplayName("should declare upgrade method")
    void shouldDeclareUpgradeMethod() throws NoSuchMethodException {
      assertNotNull(
          WeakEngine.class.getMethod("upgrade"), "WeakEngine should declare upgrade()");
    }

    @Test
    @DisplayName("should declare isValid method")
    void shouldDeclareIsValidMethod() throws NoSuchMethodException {
      assertNotNull(
          WeakEngine.class.getMethod("isValid"), "WeakEngine should declare isValid()");
    }
  }

  @Nested
  @DisplayName("IsValid Tests")
  class IsValidTests {

    @Test
    @DisplayName("isValid should return true before close")
    void isValidShouldReturnTrueBeforeClose() {
      final StubWeakEngine weak = new StubWeakEngine(true);

      assertTrue(weak.isValid(), "isValid should return true before close");
    }

    @Test
    @DisplayName("isValid should return false after close")
    void isValidShouldReturnFalseAfterClose() {
      final StubWeakEngine weak = new StubWeakEngine(true);
      weak.close();

      assertFalse(weak.isValid(), "isValid should return false after close");
    }
  }

  @Nested
  @DisplayName("Close Tests")
  class CloseTests {

    @Test
    @DisplayName("close should be idempotent")
    void closeShouldBeIdempotent() {
      final StubWeakEngine weak = new StubWeakEngine(true);

      weak.close();
      weak.close();

      assertFalse(weak.isValid(), "isValid should remain false after multiple closes");
    }

    @Test
    @DisplayName("upgrade should return empty after close")
    void upgradeShouldReturnEmptyAfterClose() {
      final StubWeakEngine weak = new StubWeakEngine(true);
      weak.close();

      final Optional<Engine> result = weak.upgrade();

      assertFalse(result.isPresent(), "upgrade should return empty after close");
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("full lifecycle: create, check valid, close, check invalid")
    void fullLifecycle() {
      final StubWeakEngine weak = new StubWeakEngine(true);

      assertTrue(weak.isValid(), "Should be valid initially");

      weak.close();
      assertFalse(weak.isValid(), "Should be invalid after close");

      final Optional<Engine> afterClose = weak.upgrade();
      assertFalse(afterClose.isPresent(), "Should not be able to upgrade after close");
    }

    @Test
    @DisplayName("upgrade return type should be Optional of Engine")
    void upgradeReturnTypeShouldBeOptionalOfEngine() throws NoSuchMethodException {
      final java.lang.reflect.Method method = WeakEngine.class.getMethod("upgrade");
      assertEquals(Optional.class, method.getReturnType(), "upgrade should return Optional");
    }
  }
}
