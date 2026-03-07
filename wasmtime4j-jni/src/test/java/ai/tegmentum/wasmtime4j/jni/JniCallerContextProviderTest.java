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
package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.spi.CallerContextProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniCallerContextProvider}.
 *
 * <p>Verifies that the provider correctly implements the CallerContextProvider SPI and delegates to
 * JniHostFunction's ThreadLocal-based caller context mechanism.
 */
@DisplayName("JniCallerContextProvider Tests")
class JniCallerContextProviderTest {

  @Test
  @DisplayName("Should implement CallerContextProvider interface")
  void shouldImplementCallerContextProvider() {
    final JniCallerContextProvider provider = new JniCallerContextProvider();
    assertInstanceOf(
        CallerContextProvider.class,
        provider,
        "JniCallerContextProvider should implement CallerContextProvider");
  }

  @Test
  @DisplayName(
      "getCurrentCaller() outside callback context should throw UnsupportedOperationException")
  void getCurrentCallerOutsideContextShouldThrow() {
    final JniCallerContextProvider provider = new JniCallerContextProvider();

    UnsupportedOperationException e =
        assertThrows(
            UnsupportedOperationException.class,
            provider::getCurrentCaller,
            "getCurrentCaller() should throw when no callback context is active");
    assertTrue(
        e.getMessage().contains("Caller context not available"),
        "Expected message to contain: Caller context not available");
  }
}
