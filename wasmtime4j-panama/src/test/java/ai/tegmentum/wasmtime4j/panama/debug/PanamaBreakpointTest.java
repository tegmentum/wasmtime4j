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

package ai.tegmentum.wasmtime4j.panama.debug;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link PanamaBreakpoint} class. */
@DisplayName("PanamaBreakpoint Tests")
public class PanamaBreakpointTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaBreakpointTest.class.getName());

  @Test
  @DisplayName("Create breakpoint at function entry")
  public void testCreateBreakpointAtFunction() {
    LOGGER.info("Testing breakpoint creation at function entry");

    final PanamaBreakpoint breakpoint = PanamaBreakpoint.atFunction("test_func");

    assertNotNull(breakpoint, "Breakpoint should not be null");
    assertNotNull(breakpoint.getBreakpointId(), "Breakpoint ID should not be null");
    assertTrue(
        breakpoint.getBreakpointId().startsWith("bp-"), "Breakpoint ID should start with bp-");
    assertEquals("test_func", breakpoint.getFunctionName(), "Function name should match");
    assertTrue(breakpoint.isEnabled(), "Breakpoint should be enabled by default");
    assertEquals(0, breakpoint.getHitCount(), "Hit count should be 0 initially");
    assertEquals(-1, breakpoint.getLineNumber(), "Line number should be -1 when not set");
    assertEquals(-1, breakpoint.getColumnNumber(), "Column number should be -1 when not set");
    assertEquals(
        -1, breakpoint.getInstructionOffset(), "Instruction offset should be -1 when not set");
    assertNull(breakpoint.getCondition(), "Condition should be null by default");

    LOGGER.info("Breakpoint at function created successfully: " + breakpoint);
  }

  @Test
  @DisplayName("Create breakpoint at specific line")
  public void testCreateBreakpointAtLine() {
    LOGGER.info("Testing breakpoint creation at specific line");

    final PanamaBreakpoint breakpoint = PanamaBreakpoint.atLine(42);

    assertNotNull(breakpoint, "Breakpoint should not be null");
    assertEquals(42, breakpoint.getLineNumber(), "Line number should match");
    assertNull(breakpoint.getFunctionName(), "Function name should be null when not set");

    LOGGER.info("Breakpoint at line created successfully: " + breakpoint);
  }

  @Test
  @DisplayName("Create breakpoint at instruction offset")
  public void testCreateBreakpointAtOffset() {
    LOGGER.info("Testing breakpoint creation at instruction offset");

    final PanamaBreakpoint breakpoint = PanamaBreakpoint.atOffset(0x1000L);

    assertNotNull(breakpoint, "Breakpoint should not be null");
    assertEquals(0x1000L, breakpoint.getInstructionOffset(), "Instruction offset should match");
    assertNull(breakpoint.getFunctionName(), "Function name should be null when not set");
    assertEquals(-1, breakpoint.getLineNumber(), "Line number should be -1 when not set");

    LOGGER.info("Breakpoint at offset created successfully: " + breakpoint);
  }

  @Test
  @DisplayName("Create breakpoint using builder with all properties")
  public void testCreateBreakpointWithBuilder() {
    LOGGER.info("Testing breakpoint creation using builder with all properties");

    final PanamaBreakpoint breakpoint =
        PanamaBreakpoint.builder()
            .breakpointId("custom-bp-1")
            .functionName("my_function")
            .lineNumber(100)
            .columnNumber(5)
            .instructionOffset(0x2000L)
            .enabled(false)
            .condition("x > 10")
            .build();

    assertNotNull(breakpoint, "Breakpoint should not be null");
    assertEquals("custom-bp-1", breakpoint.getBreakpointId(), "Breakpoint ID should match");
    assertEquals("my_function", breakpoint.getFunctionName(), "Function name should match");
    assertEquals(100, breakpoint.getLineNumber(), "Line number should match");
    assertEquals(5, breakpoint.getColumnNumber(), "Column number should match");
    assertEquals(0x2000L, breakpoint.getInstructionOffset(), "Instruction offset should match");
    assertFalse(breakpoint.isEnabled(), "Breakpoint should be disabled");
    assertEquals("x > 10", breakpoint.getCondition(), "Condition should match");

    LOGGER.info("Breakpoint with builder created successfully: " + breakpoint);
  }

  @Test
  @DisplayName("Enable and disable breakpoint")
  public void testEnableDisableBreakpoint() {
    LOGGER.info("Testing enable/disable breakpoint");

    final PanamaBreakpoint breakpoint = PanamaBreakpoint.atFunction("test_func");

    assertTrue(breakpoint.isEnabled(), "Breakpoint should be enabled by default");

    breakpoint.setEnabled(false);
    assertFalse(breakpoint.isEnabled(), "Breakpoint should be disabled after setEnabled(false)");

    breakpoint.setEnabled(true);
    assertTrue(breakpoint.isEnabled(), "Breakpoint should be enabled after setEnabled(true)");

    LOGGER.info("Enable/disable breakpoint test passed");
  }

  @Test
  @DisplayName("Test hit count increment and reset")
  public void testHitCountIncrementAndReset() {
    LOGGER.info("Testing hit count increment and reset");

    final PanamaBreakpoint breakpoint = PanamaBreakpoint.atFunction("test_func");

    assertEquals(0, breakpoint.getHitCount(), "Hit count should be 0 initially");

    int newCount = breakpoint.incrementHitCount();
    assertEquals(1, newCount, "incrementHitCount should return 1");
    assertEquals(1, breakpoint.getHitCount(), "Hit count should be 1 after increment");

    newCount = breakpoint.incrementHitCount();
    assertEquals(2, newCount, "incrementHitCount should return 2");
    assertEquals(2, breakpoint.getHitCount(), "Hit count should be 2 after second increment");

    breakpoint.resetHitCount();
    assertEquals(0, breakpoint.getHitCount(), "Hit count should be 0 after reset");

    LOGGER.info("Hit count test passed");
  }

  @Test
  @DisplayName("Test condition setting")
  public void testConditionSetting() {
    LOGGER.info("Testing condition setting");

    final PanamaBreakpoint breakpoint = PanamaBreakpoint.atFunction("test_func");

    assertNull(breakpoint.getCondition(), "Condition should be null by default");

    breakpoint.setCondition("i < 100");
    assertEquals("i < 100", breakpoint.getCondition(), "Condition should match after setCondition");

    breakpoint.setCondition(null);
    assertNull(breakpoint.getCondition(), "Condition should be null after setCondition(null)");

    LOGGER.info("Condition setting test passed");
  }

  @Test
  @DisplayName("Test breakpoint equality based on ID")
  public void testBreakpointEquality() {
    LOGGER.info("Testing breakpoint equality");

    final PanamaBreakpoint bp1 =
        PanamaBreakpoint.builder().breakpointId("bp-123").functionName("func1").build();
    final PanamaBreakpoint bp2 =
        PanamaBreakpoint.builder().breakpointId("bp-123").functionName("func2").build();
    final PanamaBreakpoint bp3 =
        PanamaBreakpoint.builder().breakpointId("bp-456").functionName("func1").build();

    assertEquals(bp1, bp2, "Breakpoints with same ID should be equal");
    assertNotEquals(bp1, bp3, "Breakpoints with different IDs should not be equal");
    assertEquals(bp1.hashCode(), bp2.hashCode(), "Equal breakpoints should have same hashCode");

    LOGGER.info("Breakpoint equality test passed");
  }

  @Test
  @DisplayName("Test breakpoint equals with null and different type")
  public void testBreakpointEqualsEdgeCases() {
    LOGGER.info("Testing breakpoint equals edge cases");

    final PanamaBreakpoint breakpoint = PanamaBreakpoint.atFunction("test_func");

    assertFalse(breakpoint.equals(null), "Breakpoint should not equal null");
    assertFalse(breakpoint.equals("not a breakpoint"), "Breakpoint should not equal string");
    assertTrue(breakpoint.equals(breakpoint), "Breakpoint should equal itself");

    LOGGER.info("Breakpoint equals edge cases test passed");
  }

  @Test
  @DisplayName("Test toString contains all relevant fields")
  public void testToStringContainsFields() {
    LOGGER.info("Testing toString contains all relevant fields");

    final PanamaBreakpoint breakpoint =
        PanamaBreakpoint.builder()
            .breakpointId("test-bp")
            .functionName("my_func")
            .lineNumber(42)
            .columnNumber(10)
            .instructionOffset(0x1000L)
            .enabled(true)
            .condition("x > 0")
            .build();

    final String str = breakpoint.toString();

    assertTrue(str.contains("test-bp"), "toString should contain breakpoint ID");
    assertTrue(str.contains("my_func"), "toString should contain function name");
    assertTrue(str.contains("42"), "toString should contain line number");
    assertTrue(str.contains("10"), "toString should contain column number");
    assertTrue(str.contains("4096"), "toString should contain instruction offset");
    assertTrue(str.contains("enabled=true"), "toString should contain enabled status");
    assertTrue(str.contains("x > 0"), "toString should contain condition");

    LOGGER.info("toString test passed: " + str);
  }

  @Test
  @DisplayName("Test auto-generated breakpoint ID uniqueness")
  public void testAutoGeneratedIdUniqueness() {
    LOGGER.info("Testing auto-generated breakpoint ID uniqueness");

    final PanamaBreakpoint bp1 = PanamaBreakpoint.atFunction("func1");
    final PanamaBreakpoint bp2 = PanamaBreakpoint.atFunction("func2");
    final PanamaBreakpoint bp3 = PanamaBreakpoint.atLine(100);

    assertNotEquals(
        bp1.getBreakpointId(), bp2.getBreakpointId(), "Auto-generated IDs should be unique");
    assertNotEquals(
        bp2.getBreakpointId(), bp3.getBreakpointId(), "Auto-generated IDs should be unique");
    assertNotEquals(
        bp1.getBreakpointId(), bp3.getBreakpointId(), "Auto-generated IDs should be unique");

    LOGGER.info("Auto-generated ID uniqueness test passed");
  }

  @Test
  @DisplayName("Test concurrent hit count increments")
  public void testConcurrentHitCountIncrements() throws InterruptedException {
    LOGGER.info("Testing concurrent hit count increments");

    final PanamaBreakpoint breakpoint = PanamaBreakpoint.atFunction("concurrent_func");
    final int threadCount = 10;
    final int incrementsPerThread = 100;
    final Thread[] threads = new Thread[threadCount];

    for (int i = 0; i < threadCount; i++) {
      threads[i] =
          new Thread(
              () -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                  breakpoint.incrementHitCount();
                }
              });
    }

    for (final Thread thread : threads) {
      thread.start();
    }

    for (final Thread thread : threads) {
      thread.join();
    }

    assertEquals(
        threadCount * incrementsPerThread,
        breakpoint.getHitCount(),
        "Hit count should reflect all concurrent increments");

    LOGGER.info("Concurrent hit count test passed with final count: " + breakpoint.getHitCount());
  }
}
