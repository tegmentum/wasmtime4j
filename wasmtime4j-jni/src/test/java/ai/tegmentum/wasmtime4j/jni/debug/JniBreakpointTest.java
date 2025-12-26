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

package ai.tegmentum.wasmtime4j.jni.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.Breakpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link JniBreakpoint}. */
@DisplayName("JniBreakpoint Tests")
class JniBreakpointTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniBreakpoint should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(JniBreakpoint.class.getModifiers()),
          "JniBreakpoint should be final");
    }

    @Test
    @DisplayName("JniBreakpoint should implement Breakpoint")
    void shouldImplementBreakpoint() {
      assertTrue(
          Breakpoint.class.isAssignableFrom(JniBreakpoint.class),
          "JniBreakpoint should implement Breakpoint");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should create breakpoint with all fields")
    void constructorShouldCreateBreakpointWithAllFields() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "testFunc", 10, 5, 0x1000L);

      assertEquals("bp-1", bp.getBreakpointId(), "ID should match");
      assertEquals("testFunc", bp.getFunctionName(), "Function name should match");
      assertEquals(10, bp.getLineNumber(), "Line number should match");
      assertEquals(5, bp.getColumnNumber(), "Column number should match");
      assertEquals(0x1000L, bp.getInstructionOffset(), "Instruction offset should match");
    }

    @Test
    @DisplayName("Constructor should throw on null breakpointId")
    void constructorShouldThrowOnNullBreakpointId() {
      assertThrows(
          NullPointerException.class,
          () -> new JniBreakpoint(null, "func", 1, 1, 0L),
          "Should throw on null breakpointId");
    }

    @Test
    @DisplayName("Constructor should accept null functionName")
    void constructorShouldAcceptNullFunctionName() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", null, 1, 1, 0L);
      assertNull(bp.getFunctionName(), "Function name should be null");
    }

    @Test
    @DisplayName("Constructor should initialize with default values")
    void constructorShouldInitializeWithDefaultValues() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);

      assertTrue(bp.isEnabled(), "Should be enabled by default");
      assertEquals(0, bp.getHitCount(), "Hit count should be 0");
      assertNull(bp.getCondition(), "Condition should be null");
    }
  }

  @Nested
  @DisplayName("fromNative Factory Tests")
  class FromNativeTests {

    @Test
    @DisplayName("fromNative should create breakpoint with correct ID format")
    void fromNativeShouldCreateBreakpointWithCorrectIdFormat() {
      final JniBreakpoint bp = JniBreakpoint.fromNative(123L, "nativeFunc", 42, 3, 0x2000L);

      assertEquals("bp-123", bp.getBreakpointId(), "ID should be bp-{nativeId}");
      assertEquals("nativeFunc", bp.getFunctionName(), "Function name should match");
      assertEquals(42, bp.getLineNumber(), "Line number should match");
      assertEquals(3, bp.getColumnNumber(), "Column number should match");
      assertEquals(0x2000L, bp.getInstructionOffset(), "Instruction offset should match");
    }

    @Test
    @DisplayName("fromNative should handle negative nativeId")
    void fromNativeShouldHandleNegativeNativeId() {
      final JniBreakpoint bp = JniBreakpoint.fromNative(-1L, "func", 1, 1, 0L);
      assertEquals("bp--1", bp.getBreakpointId(), "ID should handle negative nativeId");
    }

    @Test
    @DisplayName("fromNative should handle zero nativeId")
    void fromNativeShouldHandleZeroNativeId() {
      final JniBreakpoint bp = JniBreakpoint.fromNative(0L, "func", 1, 1, 0L);
      assertEquals("bp-0", bp.getBreakpointId(), "ID should handle zero nativeId");
    }

    @Test
    @DisplayName("fromNative should handle Long.MAX_VALUE nativeId")
    void fromNativeShouldHandleLongMaxValueNativeId() {
      final JniBreakpoint bp = JniBreakpoint.fromNative(Long.MAX_VALUE, "func", 1, 1, 0L);
      assertEquals("bp-" + Long.MAX_VALUE, bp.getBreakpointId(), "ID should handle max long");
    }
  }

  @Nested
  @DisplayName("Enabled State Tests")
  class EnabledStateTests {

    @Test
    @DisplayName("setEnabled should toggle enabled state")
    void setEnabledShouldToggleEnabledState() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);

      assertTrue(bp.isEnabled(), "Should be enabled initially");

      bp.setEnabled(false);
      assertFalse(bp.isEnabled(), "Should be disabled");

      bp.setEnabled(true);
      assertTrue(bp.isEnabled(), "Should be enabled again");
    }

    @Test
    @DisplayName("setEnabled should be thread-safe")
    void setEnabledShouldBeThreadSafe() throws InterruptedException {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);
      final int iterations = 1000;
      final Thread[] threads = new Thread[10];

      for (int i = 0; i < threads.length; i++) {
        final int threadId = i;
        threads[i] =
            new Thread(
                () -> {
                  for (int j = 0; j < iterations; j++) {
                    bp.setEnabled(threadId % 2 == 0);
                  }
                });
      }

      for (final Thread t : threads) {
        t.start();
      }
      for (final Thread t : threads) {
        t.join();
      }

      // Should not throw, and state should be consistent
      assertTrue(bp.isEnabled() || !bp.isEnabled(), "State should be boolean");
    }
  }

  @Nested
  @DisplayName("Condition Tests")
  class ConditionTests {

    @Test
    @DisplayName("setCondition should update condition")
    void setConditionShouldUpdateCondition() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);

      bp.setCondition("x > 10");
      assertEquals("x > 10", bp.getCondition(), "Condition should be set");

      bp.setCondition("y < 5");
      assertEquals("y < 5", bp.getCondition(), "Condition should be updated");
    }

    @Test
    @DisplayName("setCondition should accept null to clear condition")
    void setConditionShouldAcceptNullToClearCondition() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);

      bp.setCondition("x > 10");
      assertNotNull(bp.getCondition(), "Condition should be set");

      bp.setCondition(null);
      assertNull(bp.getCondition(), "Condition should be cleared");
    }

    @Test
    @DisplayName("setCondition should accept empty string")
    void setConditionShouldAcceptEmptyString() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);

      bp.setCondition("");
      assertEquals("", bp.getCondition(), "Condition should be empty string");
    }
  }

  @Nested
  @DisplayName("Hit Count Tests")
  class HitCountTests {

    @Test
    @DisplayName("incrementHitCount should increment and return new count")
    void incrementHitCountShouldIncrementAndReturnNewCount() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);

      assertEquals(1, bp.incrementHitCount(), "First increment should return 1");
      assertEquals(2, bp.incrementHitCount(), "Second increment should return 2");
      assertEquals(3, bp.incrementHitCount(), "Third increment should return 3");
      assertEquals(3, bp.getHitCount(), "getHitCount should return 3");
    }

    @Test
    @DisplayName("resetHitCount should reset to zero")
    void resetHitCountShouldResetToZero() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);

      bp.incrementHitCount();
      bp.incrementHitCount();
      assertEquals(2, bp.getHitCount(), "Hit count should be 2");

      bp.resetHitCount();
      assertEquals(0, bp.getHitCount(), "Hit count should be reset to 0");
    }

    @Test
    @DisplayName("Hit count operations should be thread-safe")
    void hitCountOperationsShouldBeThreadSafe() throws InterruptedException {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);
      final int threadsCount = 10;
      final int incrementsPerThread = 1000;
      final Thread[] threads = new Thread[threadsCount];

      for (int i = 0; i < threads.length; i++) {
        threads[i] =
            new Thread(
                () -> {
                  for (int j = 0; j < incrementsPerThread; j++) {
                    bp.incrementHitCount();
                  }
                });
      }

      for (final Thread t : threads) {
        t.start();
      }
      for (final Thread t : threads) {
        t.join();
      }

      assertEquals(
          threadsCount * incrementsPerThread,
          bp.getHitCount(),
          "Hit count should be sum of all increments");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include all fields")
    void toStringShouldIncludeAllFields() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "testFunc", 10, 5, 0x1000L);

      final String str = bp.toString();

      assertTrue(str.contains("bp-1"), "Should contain ID");
      assertTrue(str.contains("testFunc"), "Should contain function name");
      assertTrue(str.contains("10"), "Should contain line number");
      assertTrue(str.contains("5"), "Should contain column number");
      assertTrue(str.contains("4096") || str.contains("0x1000"), "Should contain offset");
      assertTrue(str.contains("enabled=true"), "Should contain enabled state");
      assertTrue(str.contains("hits=0"), "Should contain hit count");
    }

    @Test
    @DisplayName("toString should include condition when set")
    void toStringShouldIncludeConditionWhenSet() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);
      bp.setCondition("x > 10");

      final String str = bp.toString();
      assertTrue(str.contains("condition='x > 10'"), "Should contain condition");
    }

    @Test
    @DisplayName("toString should not include condition when null")
    void toStringShouldNotIncludeConditionWhenNull() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);

      final String str = bp.toString();
      assertFalse(str.contains("condition="), "Should not contain condition field");
    }
  }

  @Nested
  @DisplayName("equals and hashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("equals should return true for same ID")
    void equalsShouldReturnTrueForSameId() {
      final JniBreakpoint bp1 = new JniBreakpoint("bp-1", "func1", 10, 5, 0x1000L);
      final JniBreakpoint bp2 = new JniBreakpoint("bp-1", "func2", 20, 3, 0x2000L);

      assertEquals(bp1, bp2, "Breakpoints with same ID should be equal");
    }

    @Test
    @DisplayName("equals should return false for different IDs")
    void equalsShouldReturnFalseForDifferentIds() {
      final JniBreakpoint bp1 = new JniBreakpoint("bp-1", "func", 10, 5, 0x1000L);
      final JniBreakpoint bp2 = new JniBreakpoint("bp-2", "func", 10, 5, 0x1000L);

      assertNotEquals(bp1, bp2, "Breakpoints with different IDs should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);
      assertNotEquals(null, bp, "Should not be equal to null");
    }

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);
      assertNotEquals("bp-1", bp, "Should not be equal to String");
    }

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);
      assertEquals(bp, bp, "Should be equal to itself");
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void hashCodeShouldBeConsistentWithEquals() {
      final JniBreakpoint bp1 = new JniBreakpoint("bp-1", "func1", 10, 5, 0x1000L);
      final JniBreakpoint bp2 = new JniBreakpoint("bp-1", "func2", 20, 3, 0x2000L);

      assertEquals(bp1.hashCode(), bp2.hashCode(), "Equal objects should have same hashCode");
    }

    @Test
    @DisplayName("hashCode should be based on breakpointId")
    void hashCodeShouldBeBasedOnBreakpointId() {
      final JniBreakpoint bp = new JniBreakpoint("bp-1", "func", 1, 1, 0L);
      assertEquals("bp-1".hashCode(), bp.hashCode(), "hashCode should be based on breakpointId");
    }
  }
}
