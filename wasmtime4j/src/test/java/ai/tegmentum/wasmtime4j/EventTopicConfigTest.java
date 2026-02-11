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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.config.EventTopicConfig;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EventTopicConfig} class.
 *
 * <p>EventTopicConfig defines settings for an event topic including topic name, max subscribers,
 * persistence, and delivery guarantee level.
 */
@DisplayName("EventTopicConfig Tests")
class EventTopicConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(EventTopicConfig.class.getModifiers()),
          "EventTopicConfig should be public");
      assertTrue(
          Modifier.isFinal(EventTopicConfig.class.getModifiers()),
          "EventTopicConfig should be final");
    }
  }

  @Nested
  @DisplayName("Simple Constructor Tests")
  class SimpleConstructorTests {

    @Test
    @DisplayName("should create config with topic name and defaults")
    void shouldCreateWithTopicNameAndDefaults() {
      final EventTopicConfig config = new EventTopicConfig("my-topic");

      assertNotNull(config, "Config should not be null");
      assertEquals("my-topic", config.getTopicName(), "topicName should be 'my-topic'");
      assertEquals(100, config.getMaxSubscribers(), "Default maxSubscribers should be 100");
      assertFalse(config.isPersistenceEnabled(), "Default persistence should be false");
      assertEquals(
          EventTopicConfig.DeliveryGuarantee.AT_LEAST_ONCE,
          config.getDeliveryGuarantee(),
          "Default delivery guarantee should be AT_LEAST_ONCE");
    }

    @Test
    @DisplayName("should reject null topicName")
    void shouldRejectNullTopicName() {
      assertThrows(
          NullPointerException.class,
          () -> new EventTopicConfig(null),
          "Null topicName should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Full Constructor Tests")
  class FullConstructorTests {

    @Test
    @DisplayName("should create config with all parameters")
    void shouldCreateWithAllParameters() {
      final EventTopicConfig config =
          new EventTopicConfig(
              "orders", 500, true, EventTopicConfig.DeliveryGuarantee.EXACTLY_ONCE);

      assertEquals("orders", config.getTopicName(), "topicName should be 'orders'");
      assertEquals(500, config.getMaxSubscribers(), "maxSubscribers should be 500");
      assertTrue(config.isPersistenceEnabled(), "persistence should be true");
      assertEquals(
          EventTopicConfig.DeliveryGuarantee.EXACTLY_ONCE,
          config.getDeliveryGuarantee(),
          "delivery guarantee should be EXACTLY_ONCE");
    }

    @Test
    @DisplayName("should reject null topicName in full constructor")
    void shouldRejectNullTopicNameInFullConstructor() {
      assertThrows(
          NullPointerException.class,
          () ->
              new EventTopicConfig(
                  null, 100, false, EventTopicConfig.DeliveryGuarantee.AT_LEAST_ONCE),
          "Null topicName should throw NullPointerException");
    }

    @Test
    @DisplayName("should reject null deliveryGuarantee")
    void shouldRejectNullDeliveryGuarantee() {
      assertThrows(
          NullPointerException.class,
          () -> new EventTopicConfig("topic", 100, false, null),
          "Null deliveryGuarantee should throw NullPointerException");
    }

    @Test
    @DisplayName("should accept AT_MOST_ONCE delivery guarantee")
    void shouldAcceptAtMostOnce() {
      final EventTopicConfig config =
          new EventTopicConfig(
              "fire-and-forget", 50, false, EventTopicConfig.DeliveryGuarantee.AT_MOST_ONCE);

      assertEquals(
          EventTopicConfig.DeliveryGuarantee.AT_MOST_ONCE,
          config.getDeliveryGuarantee(),
          "delivery guarantee should be AT_MOST_ONCE");
    }

    @Test
    @DisplayName("should accept zero maxSubscribers")
    void shouldAcceptZeroMaxSubscribers() {
      final EventTopicConfig config =
          new EventTopicConfig(
              "internal", 0, false, EventTopicConfig.DeliveryGuarantee.AT_LEAST_ONCE);

      assertEquals(0, config.getMaxSubscribers(), "maxSubscribers should be 0");
    }
  }

  @Nested
  @DisplayName("DeliveryGuarantee Enum Tests")
  class DeliveryGuaranteeEnumTests {

    @Test
    @DisplayName("should have all expected delivery guarantee values")
    void shouldHaveAllExpectedValues() {
      final EventTopicConfig.DeliveryGuarantee[] values =
          EventTopicConfig.DeliveryGuarantee.values();
      assertEquals(3, values.length, "DeliveryGuarantee should have 3 values");
      assertNotNull(
          EventTopicConfig.DeliveryGuarantee.valueOf("AT_MOST_ONCE"), "AT_MOST_ONCE should exist");
      assertNotNull(
          EventTopicConfig.DeliveryGuarantee.valueOf("AT_LEAST_ONCE"),
          "AT_LEAST_ONCE should exist");
      assertNotNull(
          EventTopicConfig.DeliveryGuarantee.valueOf("EXACTLY_ONCE"), "EXACTLY_ONCE should exist");
    }
  }

  @Nested
  @DisplayName("Various Configuration Scenarios")
  class ConfigurationScenariosTests {

    @Test
    @DisplayName("should create persistent topic with exactly-once guarantee")
    void shouldCreatePersistentExactlyOnce() {
      final EventTopicConfig config =
          new EventTopicConfig(
              "financial-transactions",
              1000,
              true,
              EventTopicConfig.DeliveryGuarantee.EXACTLY_ONCE);

      assertEquals(
          "financial-transactions",
          config.getTopicName(),
          "topicName should be 'financial-transactions'");
      assertEquals(1000, config.getMaxSubscribers(), "maxSubscribers should be 1000");
      assertTrue(config.isPersistenceEnabled(), "Persistence should be enabled");
      assertEquals(
          EventTopicConfig.DeliveryGuarantee.EXACTLY_ONCE,
          config.getDeliveryGuarantee(),
          "Should use EXACTLY_ONCE guarantee");
    }

    @Test
    @DisplayName("should create non-persistent topic with at-most-once guarantee")
    void shouldCreateNonPersistentAtMostOnce() {
      final EventTopicConfig config =
          new EventTopicConfig(
              "metrics", 200, false, EventTopicConfig.DeliveryGuarantee.AT_MOST_ONCE);

      assertEquals("metrics", config.getTopicName(), "topicName should be 'metrics'");
      assertFalse(config.isPersistenceEnabled(), "Persistence should be disabled");
      assertEquals(
          EventTopicConfig.DeliveryGuarantee.AT_MOST_ONCE,
          config.getDeliveryGuarantee(),
          "Should use AT_MOST_ONCE guarantee");
    }

    @Test
    @DisplayName("should handle negative maxSubscribers without validation")
    void shouldHandleNegativeMaxSubscribers() {
      final EventTopicConfig config =
          new EventTopicConfig("test", -1, false, EventTopicConfig.DeliveryGuarantee.AT_LEAST_ONCE);

      assertEquals(
          -1,
          config.getMaxSubscribers(),
          "maxSubscribers should be -1 (no validation in constructor)");
    }
  }
}
