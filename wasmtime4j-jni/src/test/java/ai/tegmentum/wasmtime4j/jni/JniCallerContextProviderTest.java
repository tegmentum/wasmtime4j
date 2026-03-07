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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    assertThat(provider)
        .as("JniCallerContextProvider should implement CallerContextProvider")
        .isInstanceOf(CallerContextProvider.class);
  }

  @Test
  @DisplayName(
      "getCurrentCaller() outside callback context should throw UnsupportedOperationException")
  void getCurrentCallerOutsideContextShouldThrow() {
    final JniCallerContextProvider provider = new JniCallerContextProvider();

    assertThatThrownBy(provider::getCurrentCaller)
        .as("getCurrentCaller() should throw when no callback context is active")
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("Caller context not available");
  }
}
