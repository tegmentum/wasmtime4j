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
package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CompletedTaskExit}.
 *
 * @since 1.1.0
 */
@DisplayName("CompletedTaskExit")
class CompletedTaskExitTest {

  @Nested
  @DisplayName("singleton")
  class Singleton {

    @Test
    @DisplayName("INSTANCE is not null")
    void instanceNotNull() {
      assertNotNull(CompletedTaskExit.INSTANCE);
    }

    @Test
    @DisplayName("INSTANCE is same reference")
    void instanceIsSame() {
      assertSame(CompletedTaskExit.INSTANCE, CompletedTaskExit.INSTANCE);
    }
  }

  @Nested
  @DisplayName("behavior")
  class Behavior {

    @Test
    @DisplayName("isCompleted returns true")
    void isCompletedReturnsTrue() {
      assertTrue(CompletedTaskExit.INSTANCE.isCompleted());
    }

    @Test
    @DisplayName("block does not throw")
    void blockDoesNotThrow() {
      CompletedTaskExit.INSTANCE.block();
    }

    @Test
    @DisplayName("toString returns expected value")
    void toStringValue() {
      assertEquals("TaskExit[completed]", CompletedTaskExit.INSTANCE.toString());
    }
  }
}
