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

package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiEvent;
import ai.tegmentum.wasmtime4j.wasi.WasiSubscription;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for WASI data classes (POJOs).
 *
 * <p>Tests WasiSubscription and WasiEvent.
 */
@DisplayName("WASI Data Classes Tests")
public class WasiDataClassesTest {

  private static final Logger LOGGER = Logger.getLogger(WasiDataClassesTest.class.getName());

  @Nested
  @DisplayName("WasiSubscription Tests")
  class WasiSubscriptionTests {

    @Test
    @DisplayName("Should create empty subscription")
    void shouldCreateEmptySubscription() {
      LOGGER.info("Testing default constructor");

      final WasiSubscription sub = new WasiSubscription();

      assertEquals(0L, sub.getUserData());
      assertEquals(0, sub.getType());
      assertEquals(0, sub.getFd());
      assertEquals(0, sub.getFlags());

      LOGGER.info("Created empty subscription: " + sub);
    }

    @Test
    @DisplayName("Should set and get user data")
    void shouldSetAndGetUserData() {
      LOGGER.info("Testing userData getter/setter");

      final WasiSubscription sub = new WasiSubscription();
      sub.setUserData(12345L);

      assertEquals(12345L, sub.getUserData());

      LOGGER.info("UserData set/get verified");
    }

    @Test
    @DisplayName("Should set and get type")
    void shouldSetAndGetType() {
      LOGGER.info("Testing type getter/setter");

      final WasiSubscription sub = new WasiSubscription();
      sub.setType(1);

      assertEquals(1, sub.getType());

      LOGGER.info("Type set/get verified");
    }

    @Test
    @DisplayName("Should set and get fd")
    void shouldSetAndGetFd() {
      LOGGER.info("Testing fd getter/setter");

      final WasiSubscription sub = new WasiSubscription();
      sub.setFd(42);

      assertEquals(42, sub.getFd());

      LOGGER.info("Fd set/get verified");
    }

    @Test
    @DisplayName("Should set and get flags")
    void shouldSetAndGetFlags() {
      LOGGER.info("Testing flags getter/setter");

      final WasiSubscription sub = new WasiSubscription();
      sub.setFlags(0xFF);

      assertEquals(0xFF, sub.getFlags());

      LOGGER.info("Flags set/get verified");
    }

    @Test
    @DisplayName("Should set fd readwrite")
    void shouldSetFdReadwrite() {
      LOGGER.info("Testing setFdReadwrite");

      final WasiSubscription sub = new WasiSubscription();
      sub.setFdReadwrite(10, 3);

      assertEquals(10, sub.getFd());
      assertEquals(3, sub.getFlags());

      LOGGER.info("FdReadwrite set verified");
    }

    @Test
    @DisplayName("Should produce readable toString")
    void shouldProduceReadableToString() {
      LOGGER.info("Testing toString");

      final WasiSubscription sub = new WasiSubscription();
      sub.setUserData(100L);
      sub.setType(1);
      sub.setFd(5);
      sub.setFlags(2);

      final String str = sub.toString();
      assertNotNull(str);
      assertTrue(str.contains("WasiSubscription"));
      assertTrue(str.contains("userData=100"));
      assertTrue(str.contains("type=1"));
      assertTrue(str.contains("fd=5"));
      assertTrue(str.contains("flags=2"));

      LOGGER.info("toString: " + str);
    }
  }

  @Nested
  @DisplayName("WasiEvent Tests")
  class WasiEventTests {

    @Test
    @DisplayName("Should create event with all fields")
    void shouldCreateEventWithAllFields() {
      LOGGER.info("Testing WasiEvent constructor");

      final WasiEvent event = new WasiEvent(12345L, 0, 1, 1024);

      assertEquals(12345L, event.getUserData());
      assertEquals(0, event.getError());
      assertEquals(1, event.getType());
      assertEquals(1024, event.getNbytes());
      assertFalse(event.hasError());

      LOGGER.info("Created: " + event);
    }

    @Test
    @DisplayName("Should detect error condition")
    void shouldDetectErrorCondition() {
      LOGGER.info("Testing hasError");

      final WasiEvent successEvent = new WasiEvent(1L, 0, 1, 0);
      final WasiEvent errorEvent = new WasiEvent(1L, 1, 1, 0);

      assertFalse(successEvent.hasError(), "Error=0 should not indicate error");
      assertTrue(errorEvent.hasError(), "Error>0 should indicate error");

      LOGGER.info("Error detection verified");
    }

    @Test
    @DisplayName("Should handle negative error codes")
    void shouldHandleNegativeErrorCodes() {
      LOGGER.info("Testing negative error codes");

      final WasiEvent event = new WasiEvent(1L, -1, 1, 0);

      assertTrue(event.hasError(), "Negative error code should indicate error");
      assertEquals(-1, event.getError());

      LOGGER.info("Negative error codes handled");
    }

    @Test
    @DisplayName("Should produce readable toString")
    void shouldProduceReadableToString() {
      LOGGER.info("Testing toString");

      final WasiEvent event = new WasiEvent(100L, 0, 2, 512);

      final String str = event.toString();
      assertNotNull(str);
      assertTrue(str.contains("WasiEvent"));
      assertTrue(str.contains("userData=100"));
      assertTrue(str.contains("error=0"));
      assertTrue(str.contains("type=2"));
      assertTrue(str.contains("nbytes=512"));

      LOGGER.info("toString: " + str);
    }

    @Test
    @DisplayName("Should handle max values")
    void shouldHandleMaxValues() {
      LOGGER.info("Testing max values");

      final WasiEvent event =
          new WasiEvent(Long.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, event.getUserData());
      assertEquals(Integer.MAX_VALUE, event.getError());
      assertEquals(Integer.MAX_VALUE, event.getType());
      assertEquals(Integer.MAX_VALUE, event.getNbytes());

      LOGGER.info("Max values handled");
    }
  }
}
