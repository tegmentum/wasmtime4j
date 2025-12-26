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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiEvent} class.
 *
 * <p>WasiEvent represents an event returned by WASI polling operations.
 */
@DisplayName("WasiEvent Class Tests")
class WasiEventTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create event with all fields")
    void shouldCreateEventWithAllFields() {
      final WasiEvent event = new WasiEvent(12345L, 0, 1, 1024);

      assertNotNull(event, "WasiEvent should be created");
    }

    @Test
    @DisplayName("should create event with zero values")
    void shouldCreateEventWithZeroValues() {
      final WasiEvent event = new WasiEvent(0L, 0, 0, 0);

      assertNotNull(event, "WasiEvent should be created with zero values");
      assertEquals(0L, event.getUserData());
      assertEquals(0, event.getError());
      assertEquals(0, event.getType());
      assertEquals(0, event.getNbytes());
    }

    @Test
    @DisplayName("should create event with error code")
    void shouldCreateEventWithErrorCode() {
      final WasiEvent event = new WasiEvent(100L, 1, 1, 0);

      assertEquals(1, event.getError(), "Error code should be 1");
      assertTrue(event.hasError(), "hasError should return true");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    private final WasiEvent event = new WasiEvent(99999L, 2, 1, 512);

    @Test
    @DisplayName("getUserData should return user data")
    void getUserDataShouldReturnUserData() {
      assertEquals(99999L, event.getUserData());
    }

    @Test
    @DisplayName("getError should return error code")
    void getErrorShouldReturnErrorCode() {
      assertEquals(2, event.getError());
    }

    @Test
    @DisplayName("getType should return event type")
    void getTypeShouldReturnEventType() {
      assertEquals(1, event.getType());
    }

    @Test
    @DisplayName("getNbytes should return number of bytes")
    void getNbytesShouldReturnNumberOfBytes() {
      assertEquals(512, event.getNbytes());
    }
  }

  @Nested
  @DisplayName("HasError Tests")
  class HasErrorTests {

    @Test
    @DisplayName("hasError should return false when error is 0")
    void hasErrorShouldReturnFalseWhenErrorIsZero() {
      final WasiEvent event = new WasiEvent(0L, 0, 0, 0);
      assertFalse(event.hasError());
    }

    @Test
    @DisplayName("hasError should return true when error is non-zero")
    void hasErrorShouldReturnTrueWhenErrorIsNonZero() {
      final WasiEvent event1 = new WasiEvent(0L, 1, 0, 0);
      final WasiEvent event2 = new WasiEvent(0L, 28, 0, 0); // EINVAL
      final WasiEvent event3 = new WasiEvent(0L, -1, 0, 0);

      assertTrue(event1.hasError());
      assertTrue(event2.hasError());
      assertTrue(event3.hasError());
    }
  }

  @Nested
  @DisplayName("Event Type Tests")
  class EventTypeTests {

    @Test
    @DisplayName("type 0 should represent CLOCK event")
    void typeZeroShouldRepresentClockEvent() {
      final WasiEvent event = new WasiEvent(0L, 0, 0, 0);
      assertEquals(0, event.getType(), "Type 0 should represent CLOCK event");
    }

    @Test
    @DisplayName("type 1 should represent FD_READ event")
    void typeOneShouldRepresentFdReadEvent() {
      final WasiEvent event = new WasiEvent(0L, 0, 1, 100);
      assertEquals(1, event.getType(), "Type 1 should represent FD_READ event");
      assertEquals(100, event.getNbytes(), "nbytes should indicate available bytes");
    }

    @Test
    @DisplayName("type 2 should represent FD_WRITE event")
    void typeTwoShouldRepresentFdWriteEvent() {
      final WasiEvent event = new WasiEvent(0L, 0, 2, 200);
      assertEquals(2, event.getType(), "Type 2 should represent FD_WRITE event");
      assertEquals(200, event.getNbytes(), "nbytes should indicate writable bytes");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include all fields")
    void toStringShouldIncludeAllFields() {
      final WasiEvent event = new WasiEvent(12345L, 0, 1, 1024);
      final String str = event.toString();

      assertTrue(str.contains("WasiEvent"), "Should contain class name");
      assertTrue(str.contains("userData=12345"), "Should contain userData");
      assertTrue(str.contains("error=0"), "Should contain error");
      assertTrue(str.contains("type=1"), "Should contain type");
      assertTrue(str.contains("nbytes=1024"), "Should contain nbytes");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle Long.MAX_VALUE for userData")
    void shouldHandleMaxValueForUserData() {
      final WasiEvent event = new WasiEvent(Long.MAX_VALUE, 0, 0, 0);
      assertEquals(Long.MAX_VALUE, event.getUserData());
    }

    @Test
    @DisplayName("should handle negative userData")
    void shouldHandleNegativeUserData() {
      final WasiEvent event = new WasiEvent(-1L, 0, 0, 0);
      assertEquals(-1L, event.getUserData());
    }

    @Test
    @DisplayName("should handle Integer.MAX_VALUE for nbytes")
    void shouldHandleMaxValueForNbytes() {
      final WasiEvent event = new WasiEvent(0L, 0, 1, Integer.MAX_VALUE);
      assertEquals(Integer.MAX_VALUE, event.getNbytes());
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("WasiEvent should be final class")
    void wasiEventShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiEvent.class.getModifiers()),
          "WasiEvent should be final");
    }

    @Test
    @DisplayName("should have no setter methods")
    void shouldHaveNoSetterMethods() {
      final java.lang.reflect.Method[] methods = WasiEvent.class.getDeclaredMethods();
      for (final java.lang.reflect.Method method : methods) {
        assertFalse(
            method.getName().startsWith("set"),
            "Should not have setter method: " + method.getName());
      }
    }
  }
}
