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
package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PanamaTypedFunc}.
 *
 * <p>These tests verify parameter validation of PanamaTypedFunc without requiring actual native
 * library operations.
 */
@DisplayName("PanamaTypedFunc Tests")
class PanamaTypedFuncTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("constructor should reject null function")
    void constructorShouldRejectNullFunction() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> new PanamaTypedFunc(null, "ii->i"));
      assertTrue(
          ex.getMessage().contains("Function cannot be null"),
          "Expected message to contain 'Function cannot be null': " + ex.getMessage());
    }
  }
}
