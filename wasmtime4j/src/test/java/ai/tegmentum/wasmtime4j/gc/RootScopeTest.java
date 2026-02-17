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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RootScope} class.
 *
 * <p>RootScope provides scoped lifecycle management for rooted GC references with automatic cleanup
 * when the scope is closed.
 */
@DisplayName("RootScope Tests")
class RootScopeTest {

  @Nested
  @DisplayName("Factory Method Null Validation Tests")
  class FactoryMethodNullTests {

    @Test
    @DisplayName("create with null store should throw NullPointerException")
    void createWithNullStoreShouldThrowNpe() {
      assertThrows(
          NullPointerException.class,
          () -> RootScope.create(null),
          "create(null) should throw NullPointerException");
    }
  }
}
