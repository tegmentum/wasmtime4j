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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for DebugEvent class.
 *
 * <p>Verifies debug event creation and data access.
 */
@DisplayName("DebugEvent Tests")
class DebugEventTest {

  private ExecutionState mockExecutionState;

  @BeforeEach
  void setUp() {
    mockExecutionState = createMockExecutionState();
  }

  private ExecutionState createMockExecutionState() {
    return new ExecutionState() {
      @Override
      public ExecutionStatus getStatus() {
        return ExecutionStatus.PAUSED;
      }

      @Override
      public long getInstructionPointer() {
        return 1000L;
      }

      @Override
      public List<StackFrame> getStackFrames() {
        return Collections.emptyList();
      }

      @Override
      public String getCurrentModule() {
        return "test_module";
      }

      @Override
      public String getCurrentFunction() {
        return "test_function";
      }

      @Override
      public ExecutionStatistics getStatistics() {
        return new ExecutionStatistics() {
          @Override
          public long getInstructionCount() {
            return 5000L;
          }

          @Override
          public long getExecutionTime() {
            return 100L;
          }

          @Override
          public long getFunctionCallCount() {
            return 50L;
          }
        };
      }
    };
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create event with type and state")
    void shouldCreateEventWithTypeAndState() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.BREAKPOINT, mockExecutionState);

      assertNotNull(event, "Event should not be null");
      assertEquals(DebugEvent.DebugEventType.BREAKPOINT, event.getType(), "Type should match");
      assertEquals(mockExecutionState, event.getExecutionState(), "Execution state should match");
      assertTrue(event.getData().isEmpty(), "Data should be empty");
      assertTrue(event.getTimestamp() > 0, "Timestamp should be set");
    }

    @Test
    @DisplayName("should create event with type, state, and data")
    void shouldCreateEventWithTypeStateAndData() {
      Map<String, Object> data = new HashMap<>();
      data.put("breakpoint_id", "bp-001");
      data.put("hit_count", 5);

      DebugEvent event =
          new DebugEvent(DebugEvent.DebugEventType.BREAKPOINT, mockExecutionState, data);

      assertEquals(DebugEvent.DebugEventType.BREAKPOINT, event.getType(), "Type should match");
      assertEquals(mockExecutionState, event.getExecutionState(), "Execution state should match");
      assertEquals(2, event.getData().size(), "Should have 2 data entries");
      assertEquals("bp-001", event.getData("breakpoint_id"), "Breakpoint ID should match");
      assertEquals(5, event.getData("hit_count"), "Hit count should match");
    }

    @Test
    @DisplayName("should throw on null type")
    void shouldThrowOnNullType() {
      assertThrows(
          NullPointerException.class,
          () -> new DebugEvent(null, mockExecutionState),
          "Should throw on null type");
    }

    @Test
    @DisplayName("should throw on null execution state")
    void shouldThrowOnNullExecutionState() {
      assertThrows(
          NullPointerException.class,
          () -> new DebugEvent(DebugEvent.DebugEventType.STEP, null),
          "Should throw on null execution state");
    }
  }

  @Nested
  @DisplayName("GetType Tests")
  class GetTypeTests {

    @Test
    @DisplayName("should return breakpoint type")
    void shouldReturnBreakpointType() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.BREAKPOINT, mockExecutionState);

      assertEquals(DebugEvent.DebugEventType.BREAKPOINT, event.getType(), "Should be BREAKPOINT");
    }

    @Test
    @DisplayName("should return step type")
    void shouldReturnStepType() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState);

      assertEquals(DebugEvent.DebugEventType.STEP, event.getType(), "Should be STEP");
    }

    @Test
    @DisplayName("should return pause type")
    void shouldReturnPauseType() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.PAUSE, mockExecutionState);

      assertEquals(DebugEvent.DebugEventType.PAUSE, event.getType(), "Should be PAUSE");
    }

    @Test
    @DisplayName("should return resume type")
    void shouldReturnResumeType() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.RESUME, mockExecutionState);

      assertEquals(DebugEvent.DebugEventType.RESUME, event.getType(), "Should be RESUME");
    }

    @Test
    @DisplayName("should return complete type")
    void shouldReturnCompleteType() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.COMPLETE, mockExecutionState);

      assertEquals(DebugEvent.DebugEventType.COMPLETE, event.getType(), "Should be COMPLETE");
    }

    @Test
    @DisplayName("should return exception type")
    void shouldReturnExceptionType() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.EXCEPTION, mockExecutionState);

      assertEquals(DebugEvent.DebugEventType.EXCEPTION, event.getType(), "Should be EXCEPTION");
    }

    @Test
    @DisplayName("should return terminate type")
    void shouldReturnTerminateType() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.TERMINATE, mockExecutionState);

      assertEquals(DebugEvent.DebugEventType.TERMINATE, event.getType(), "Should be TERMINATE");
    }
  }

  @Nested
  @DisplayName("GetData Tests")
  class GetDataTests {

    @Test
    @DisplayName("should return empty data map")
    void shouldReturnEmptyDataMap() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState);

      assertTrue(event.getData().isEmpty(), "Data should be empty");
    }

    @Test
    @DisplayName("should return data by key")
    void shouldReturnDataByKey() {
      Map<String, Object> data = new HashMap<>();
      data.put("key1", "value1");
      data.put("key2", 42);

      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState, data);

      assertEquals("value1", event.getData("key1"), "Should return value1");
      assertEquals(42, event.getData("key2"), "Should return 42");
    }

    @Test
    @DisplayName("should return null for missing key")
    void shouldReturnNullForMissingKey() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState);

      assertNull(event.getData("nonexistent"), "Should return null for missing key");
    }

    @Test
    @DisplayName("should return unmodifiable data map")
    void shouldReturnUnmodifiableDataMap() {
      Map<String, Object> data = new HashMap<>();
      data.put("key", "value");

      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState, data);

      assertThrows(
          UnsupportedOperationException.class,
          () -> event.getData().put("new_key", "new_value"),
          "Data map should be unmodifiable");
    }
  }

  @Nested
  @DisplayName("HasData Tests")
  class HasDataTests {

    @Test
    @DisplayName("should return true when data exists")
    void shouldReturnTrueWhenDataExists() {
      Map<String, Object> data = new HashMap<>();
      data.put("existing_key", "value");

      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState, data);

      assertTrue(event.hasData("existing_key"), "Should return true for existing key");
    }

    @Test
    @DisplayName("should return false when data does not exist")
    void shouldReturnFalseWhenDataDoesNotExist() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState);

      assertFalse(event.hasData("missing_key"), "Should return false for missing key");
    }
  }

  @Nested
  @DisplayName("GetTimestamp Tests")
  class GetTimestampTests {

    @Test
    @DisplayName("should return positive timestamp")
    void shouldReturnPositiveTimestamp() {
      long beforeCreation = System.currentTimeMillis();
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState);
      long afterCreation = System.currentTimeMillis();

      assertTrue(event.getTimestamp() >= beforeCreation, "Timestamp should be >= beforeCreation");
      assertTrue(event.getTimestamp() <= afterCreation, "Timestamp should be <= afterCreation");
    }
  }

  @Nested
  @DisplayName("Equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState);

      assertEquals(event, event, "Event should be equal to itself");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState);

      assertNotEquals(null, event, "Event should not be equal to null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState);

      assertNotEquals("not an event", event, "Event should not be equal to string");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("should produce consistent hash code")
    void shouldProduceConsistentHashCode() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState);

      int hash1 = event.hashCode();
      int hash2 = event.hashCode();

      assertEquals(hash1, hash2, "Hash codes should be consistent");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce non-null toString")
    void shouldProduceNonNullToString() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState);

      assertNotNull(event.toString(), "toString should not return null");
    }

    @Test
    @DisplayName("should include type in toString")
    void shouldIncludeTypeInToString() {
      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.BREAKPOINT, mockExecutionState);

      assertTrue(event.toString().contains("BREAKPOINT"), "toString should contain type");
    }

    @Test
    @DisplayName("should include data in toString")
    void shouldIncludeDataInToString() {
      Map<String, Object> data = new HashMap<>();
      data.put("test_key", "test_value");

      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState, data);

      assertTrue(event.toString().contains("test_key"), "toString should contain data key");
    }
  }

  @Nested
  @DisplayName("DebugEventType Enum Tests")
  class DebugEventTypeEnumTests {

    @Test
    @DisplayName("should have all expected event types")
    void shouldHaveAllExpectedEventTypes() {
      DebugEvent.DebugEventType[] types = DebugEvent.DebugEventType.values();

      assertEquals(7, types.length, "Should have 7 event types");
    }

    @Test
    @DisplayName("should convert from string")
    void shouldConvertFromString() {
      assertEquals(
          DebugEvent.DebugEventType.BREAKPOINT,
          DebugEvent.DebugEventType.valueOf("BREAKPOINT"),
          "BREAKPOINT from string");
      assertEquals(
          DebugEvent.DebugEventType.STEP,
          DebugEvent.DebugEventType.valueOf("STEP"),
          "STEP from string");
      assertEquals(
          DebugEvent.DebugEventType.PAUSE,
          DebugEvent.DebugEventType.valueOf("PAUSE"),
          "PAUSE from string");
      assertEquals(
          DebugEvent.DebugEventType.RESUME,
          DebugEvent.DebugEventType.valueOf("RESUME"),
          "RESUME from string");
      assertEquals(
          DebugEvent.DebugEventType.COMPLETE,
          DebugEvent.DebugEventType.valueOf("COMPLETE"),
          "COMPLETE from string");
      assertEquals(
          DebugEvent.DebugEventType.EXCEPTION,
          DebugEvent.DebugEventType.valueOf("EXCEPTION"),
          "EXCEPTION from string");
      assertEquals(
          DebugEvent.DebugEventType.TERMINATE,
          DebugEvent.DebugEventType.valueOf("TERMINATE"),
          "TERMINATE from string");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should represent breakpoint hit event")
    void shouldRepresentBreakpointHitEvent() {
      Map<String, Object> data = new HashMap<>();
      data.put("breakpoint_id", "bp-main-001");
      data.put("hit_count", 1);
      data.put("condition", "x > 10");

      DebugEvent event =
          new DebugEvent(DebugEvent.DebugEventType.BREAKPOINT, mockExecutionState, data);

      assertEquals(DebugEvent.DebugEventType.BREAKPOINT, event.getType(), "Type");
      assertEquals("bp-main-001", event.getData("breakpoint_id"), "Breakpoint ID");
      assertEquals(1, event.getData("hit_count"), "Hit count");
      assertEquals("x > 10", event.getData("condition"), "Condition");
    }

    @Test
    @DisplayName("should represent exception event")
    void shouldRepresentExceptionEvent() {
      Map<String, Object> data = new HashMap<>();
      data.put("exception_type", "OutOfMemoryError");
      data.put("message", "Memory limit exceeded");
      data.put("stack_trace", "at func1:10\nat func2:20");

      DebugEvent event =
          new DebugEvent(DebugEvent.DebugEventType.EXCEPTION, mockExecutionState, data);

      assertEquals(DebugEvent.DebugEventType.EXCEPTION, event.getType(), "Type");
      assertTrue(event.hasData("exception_type"), "Has exception type");
      assertTrue(event.hasData("message"), "Has message");
      assertTrue(event.hasData("stack_trace"), "Has stack trace");
    }

    @Test
    @DisplayName("should represent step event")
    void shouldRepresentStepEvent() {
      Map<String, Object> data = new HashMap<>();
      data.put("step_type", "step_over");
      data.put("previous_line", 10);
      data.put("current_line", 11);

      DebugEvent event = new DebugEvent(DebugEvent.DebugEventType.STEP, mockExecutionState, data);

      assertEquals(DebugEvent.DebugEventType.STEP, event.getType(), "Type");
      assertEquals("step_over", event.getData("step_type"), "Step type");
      assertEquals(10, event.getData("previous_line"), "Previous line");
      assertEquals(11, event.getData("current_line"), "Current line");
    }
  }
}
