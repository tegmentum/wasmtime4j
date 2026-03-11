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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ErrorContext}.
 *
 * @since 1.1.0
 */
@DisplayName("ErrorContext")
class ErrorContextTest {

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("creates with valid handle")
    void createsWithValidHandle() {
      final ErrorContext ctx = ErrorContext.create(1);
      assertEquals(1, ctx.getHandle());
      assertTrue(ctx.isValid());
    }

    @Test
    @DisplayName("rejects zero handle")
    void rejectsZeroHandle() {
      assertThrows(IllegalArgumentException.class, () -> ErrorContext.create(0));
    }

    @Test
    @DisplayName("rejects negative handle")
    void rejectsNegativeHandle() {
      assertThrows(IllegalArgumentException.class, () -> ErrorContext.create(-1));
    }
  }

  @Nested
  @DisplayName("create with close action")
  class CreateWithCloseAction {

    @Test
    @DisplayName("creates with close action")
    void createsWithCloseAction() {
      final AtomicBoolean closed = new AtomicBoolean(false);
      final ErrorContext ctx = ErrorContext.create(42, () -> closed.set(true));
      assertEquals(42, ctx.getHandle());
      assertTrue(ctx.isValid());
      ctx.close();
      assertTrue(closed.get());
      assertFalse(ctx.isValid());
    }

    @Test
    @DisplayName("close action runs only once")
    void closeActionRunsOnce() {
      final int[] count = {0};
      final ErrorContext ctx = ErrorContext.create(1, () -> count[0]++);
      ctx.close();
      ctx.close();
      ctx.close();
      assertEquals(1, count[0]);
    }

    @Test
    @DisplayName("null close action is safe")
    void nullCloseActionIsSafe() {
      final ErrorContext ctx = ErrorContext.create(1, null);
      ctx.close();
      assertFalse(ctx.isValid());
    }
  }

  @Nested
  @DisplayName("lifecycle")
  class Lifecycle {

    @Test
    @DisplayName("close invalidates context")
    void closeInvalidates() {
      final ErrorContext ctx = ErrorContext.create(5);
      assertTrue(ctx.isValid());
      ctx.close();
      assertFalse(ctx.isValid());
    }

    @Test
    @DisplayName("close is idempotent")
    void closeIsIdempotent() {
      final ErrorContext ctx = ErrorContext.create(5);
      ctx.close();
      ctx.close();
      assertFalse(ctx.isValid());
    }
  }

  @Nested
  @DisplayName("equals and hashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("equal contexts with same handle")
    void equalContexts() {
      final ErrorContext c1 = ErrorContext.create(10);
      final ErrorContext c2 = ErrorContext.create(10);
      assertEquals(c1, c2);
      assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    @DisplayName("different handles not equal")
    void differentHandlesNotEqual() {
      final ErrorContext c1 = ErrorContext.create(1);
      final ErrorContext c2 = ErrorContext.create(2);
      assertNotEquals(c1, c2);
    }

    @Test
    @DisplayName("equal to self")
    void equalToSelf() {
      final ErrorContext c1 = ErrorContext.create(1);
      assertEquals(c1, c1);
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTests {

    @Test
    @DisplayName("includes handle and valid status")
    void includesDetails() {
      final ErrorContext ctx = ErrorContext.create(99);
      final String str = ctx.toString();
      assertTrue(str.contains("99"));
      assertTrue(str.contains("valid=true"));
    }

    @Test
    @DisplayName("shows invalid after close")
    void showsInvalidAfterClose() {
      final ErrorContext ctx = ErrorContext.create(99);
      ctx.close();
      final String str = ctx.toString();
      assertTrue(str.contains("valid=false"));
    }
  }
}
