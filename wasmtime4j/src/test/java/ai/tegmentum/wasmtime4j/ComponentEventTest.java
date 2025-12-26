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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ComponentEvent class.
 *
 * <p>This test class verifies the class structure, constructors, methods, and Builder pattern for
 * ComponentEvent using reflection-based testing.
 */
@DisplayName("ComponentEvent Tests")
class ComponentEventTest {

  // ========================================================================
  // Class Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ComponentEvent should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(ComponentEvent.class.getModifiers()),
          "ComponentEvent should be a final class");
    }

    @Test
    @DisplayName("ComponentEvent should be a public class")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComponentEvent.class.getModifiers()),
          "ComponentEvent should be public");
    }

    @Test
    @DisplayName("ComponentEvent should have at least 7 private fields")
    void shouldHaveAtLeastSevenPrivateFields() {
      Field[] fields = ComponentEvent.class.getDeclaredFields();
      int privateFieldCount = 0;
      for (Field field : fields) {
        if (Modifier.isPrivate(field.getModifiers())) {
          privateFieldCount++;
        }
      }
      assertTrue(
          privateFieldCount >= 7,
          "ComponentEvent should have at least 7 private fields, found: " + privateFieldCount);
    }

    @Test
    @DisplayName("ComponentEvent should have eventId field")
    void shouldHaveEventIdField() throws NoSuchFieldException {
      Field field = ComponentEvent.class.getDeclaredField("eventId");
      assertNotNull(field, "eventId field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "eventId should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "eventId should be final");
      assertEquals(String.class, field.getType(), "eventId should be String type");
    }

    @Test
    @DisplayName("ComponentEvent should have eventType field")
    void shouldHaveEventTypeField() throws NoSuchFieldException {
      Field field = ComponentEvent.class.getDeclaredField("eventType");
      assertNotNull(field, "eventType field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "eventType should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "eventType should be final");
      assertEquals(String.class, field.getType(), "eventType should be String type");
    }

    @Test
    @DisplayName("ComponentEvent should have sourceComponentId field")
    void shouldHaveSourceComponentIdField() throws NoSuchFieldException {
      Field field = ComponentEvent.class.getDeclaredField("sourceComponentId");
      assertNotNull(field, "sourceComponentId field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "sourceComponentId should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "sourceComponentId should be final");
      assertEquals(String.class, field.getType(), "sourceComponentId should be String type");
    }

    @Test
    @DisplayName("ComponentEvent should have targetComponentId field")
    void shouldHaveTargetComponentIdField() throws NoSuchFieldException {
      Field field = ComponentEvent.class.getDeclaredField("targetComponentId");
      assertNotNull(field, "targetComponentId field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "targetComponentId should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "targetComponentId should be final");
      assertEquals(String.class, field.getType(), "targetComponentId should be String type");
    }

    @Test
    @DisplayName("ComponentEvent should have payload field")
    void shouldHavePayloadField() throws NoSuchFieldException {
      Field field = ComponentEvent.class.getDeclaredField("payload");
      assertNotNull(field, "payload field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "payload should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "payload should be final");
      assertEquals(Object.class, field.getType(), "payload should be Object type");
    }

    @Test
    @DisplayName("ComponentEvent should have metadata field")
    void shouldHaveMetadataField() throws NoSuchFieldException {
      Field field = ComponentEvent.class.getDeclaredField("metadata");
      assertNotNull(field, "metadata field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "metadata should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "metadata should be final");
      assertEquals(Map.class, field.getType(), "metadata should be Map type");
    }

    @Test
    @DisplayName("ComponentEvent should have timestamp field")
    void shouldHaveTimestampField() throws NoSuchFieldException {
      Field field = ComponentEvent.class.getDeclaredField("timestamp");
      assertNotNull(field, "timestamp field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "timestamp should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "timestamp should be final");
      assertEquals(Instant.class, field.getType(), "timestamp should be Instant type");
    }

    @Test
    @DisplayName("ComponentEvent should have priority field")
    void shouldHavePriorityField() throws NoSuchFieldException {
      Field field = ComponentEvent.class.getDeclaredField("priority");
      assertNotNull(field, "priority field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "priority should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "priority should be final");
      assertEquals(int.class, field.getType(), "priority should be int type");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("ComponentEvent should have two public constructors")
    void shouldHaveTwoPublicConstructors() {
      Constructor<?>[] constructors = ComponentEvent.class.getConstructors();
      assertEquals(2, constructors.length, "ComponentEvent should have 2 public constructors");
    }

    @Test
    @DisplayName("Simple constructor should exist with 3 parameters")
    void shouldHaveSimpleConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          ComponentEvent.class.getConstructor(String.class, String.class, Object.class);
      assertNotNull(constructor, "Simple constructor should exist");
      assertEquals(3, constructor.getParameterCount(), "Simple constructor should have 3 params");
    }

    @Test
    @DisplayName("Full constructor should exist with 6 parameters")
    void shouldHaveFullConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          ComponentEvent.class.getConstructor(
              String.class, String.class, String.class, Object.class, Map.class, int.class);
      assertNotNull(constructor, "Full constructor should exist");
      assertEquals(6, constructor.getParameterCount(), "Full constructor should have 6 params");
    }

    @Test
    @DisplayName("Simple constructor should create event with default values")
    void simpleConstructorShouldSetDefaultValues() {
      ComponentEvent event = new ComponentEvent("test-type", "source-1", "payload");

      assertNotNull(event.getEventId(), "eventId should be generated");
      assertEquals("test-type", event.getEventType(), "eventType should match");
      assertEquals("source-1", event.getSourceComponentId(), "sourceComponentId should match");
      assertNull(event.getTargetComponentId(), "targetComponentId should be null");
      assertEquals("payload", event.getPayload(), "payload should match");
      assertTrue(event.getMetadata().isEmpty(), "metadata should be empty");
      assertNotNull(event.getTimestamp(), "timestamp should be set");
      assertEquals(0, event.getPriority(), "priority should be 0");
    }

    @Test
    @DisplayName("Full constructor should create event with all values")
    void fullConstructorShouldSetAllValues() {
      Map<String, String> metadata = Map.of("key", "value");
      ComponentEvent event =
          new ComponentEvent("test-type", "source-1", "target-1", "payload", metadata, 5);

      assertNotNull(event.getEventId(), "eventId should be generated");
      assertEquals("test-type", event.getEventType(), "eventType should match");
      assertEquals("source-1", event.getSourceComponentId(), "sourceComponentId should match");
      assertEquals("target-1", event.getTargetComponentId(), "targetComponentId should match");
      assertEquals("payload", event.getPayload(), "payload should match");
      assertEquals(1, event.getMetadata().size(), "metadata should have 1 entry");
      assertEquals("value", event.getMetadata().get("key"), "metadata value should match");
      assertNotNull(event.getTimestamp(), "timestamp should be set");
      assertEquals(5, event.getPriority(), "priority should be 5");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null eventType")
    void constructorShouldThrowForNullEventType() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentEvent(null, "source-1", "payload"),
          "Should throw NullPointerException for null eventType");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null sourceComponentId")
    void constructorShouldThrowForNullSourceComponentId() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentEvent("test-type", null, "payload"),
          "Should throw NullPointerException for null sourceComponentId");
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException for null metadata")
    void constructorShouldThrowForNullMetadata() {
      assertThrows(
          NullPointerException.class,
          () -> new ComponentEvent("test-type", "source-1", "target-1", "payload", null, 0),
          "Should throw NullPointerException for null metadata");
    }
  }

  // ========================================================================
  // Getter Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("getEventId should return unique UUIDs")
    void getEventIdShouldReturnUniqueIds() {
      ComponentEvent event1 = new ComponentEvent("type", "source", null);
      ComponentEvent event2 = new ComponentEvent("type", "source", null);

      assertNotNull(event1.getEventId(), "First event ID should not be null");
      assertNotNull(event2.getEventId(), "Second event ID should not be null");
      assertNotEquals(
          event1.getEventId(), event2.getEventId(), "Event IDs should be unique for each event");
    }

    @Test
    @DisplayName("getEventType should return the event type")
    void getEventTypeShouldReturnType() throws NoSuchMethodException {
      Method method = ComponentEvent.class.getMethod("getEventType");
      assertNotNull(method, "getEventType method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("getSourceComponentId should return the source ID")
    void getSourceComponentIdShouldReturnSourceId() throws NoSuchMethodException {
      Method method = ComponentEvent.class.getMethod("getSourceComponentId");
      assertNotNull(method, "getSourceComponentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("getTargetComponentId should return the target ID or null")
    void getTargetComponentIdShouldReturnTargetId() throws NoSuchMethodException {
      Method method = ComponentEvent.class.getMethod("getTargetComponentId");
      assertNotNull(method, "getTargetComponentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("getPayload should return the payload")
    void getPayloadShouldReturnPayload() throws NoSuchMethodException {
      Method method = ComponentEvent.class.getMethod("getPayload");
      assertNotNull(method, "getPayload method should exist");
      assertEquals(Object.class, method.getReturnType(), "Return type should be Object");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("getMetadata should return immutable map")
    void getMetadataShouldReturnImmutableMap() throws NoSuchMethodException {
      Method method = ComponentEvent.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");

      Map<String, String> original = new HashMap<>();
      original.put("key", "value");
      ComponentEvent event = new ComponentEvent("type", "source", null, null, original, 0);

      Map<String, String> metadata = event.getMetadata();
      assertThrows(
          UnsupportedOperationException.class,
          () -> metadata.put("new", "value"),
          "Returned metadata should be immutable");
    }

    @Test
    @DisplayName("getTimestamp should return the timestamp")
    void getTimestampShouldReturnTimestamp() throws NoSuchMethodException {
      Method method = ComponentEvent.class.getMethod("getTimestamp");
      assertNotNull(method, "getTimestamp method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Return type should be Instant");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");

      Instant before = Instant.now();
      ComponentEvent event = new ComponentEvent("type", "source", null);
      Instant after = Instant.now();

      assertTrue(
          !event.getTimestamp().isBefore(before) && !event.getTimestamp().isAfter(after),
          "Timestamp should be between before and after");
    }

    @Test
    @DisplayName("getPriority should return the priority")
    void getPriorityShouldReturnPriority() throws NoSuchMethodException {
      Method method = ComponentEvent.class.getMethod("getPriority");
      assertNotNull(method, "getPriority method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("isTargeted should return true when target is set")
    void isTargetedShouldReturnTrueWhenTargetSet() {
      ComponentEvent untargeted = new ComponentEvent("type", "source", null);
      ComponentEvent targeted = new ComponentEvent("type", "source", "target", null, Map.of(), 0);

      assertFalse(untargeted.isTargeted(), "Untargeted event should return false");
      assertTrue(targeted.isTargeted(), "Targeted event should return true");
    }
  }

  // ========================================================================
  // Builder Tests
  // ========================================================================

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should be a public static final nested class")
    void builderShouldBePublicStaticFinalClass() {
      Class<?>[] declaredClasses = ComponentEvent.class.getDeclaredClasses();
      Class<?> builderClass = null;
      for (Class<?> cls : declaredClasses) {
        if (cls.getSimpleName().equals("Builder")) {
          builderClass = cls;
          break;
        }
      }
      assertNotNull(builderClass, "Builder class should exist");
      assertTrue(Modifier.isPublic(builderClass.getModifiers()), "Builder should be public");
      assertTrue(Modifier.isStatic(builderClass.getModifiers()), "Builder should be static");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Builder should be final");
    }

    @Test
    @DisplayName("builder static method should return Builder")
    void builderMethodShouldReturnBuilder() throws NoSuchMethodException {
      Method method = ComponentEvent.class.getMethod("builder", String.class, String.class);
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          ComponentEvent.Builder.class,
          method.getReturnType(),
          "Return type should be ComponentEvent.Builder");
    }

    @Test
    @DisplayName("Builder should create event with builder pattern")
    void builderShouldCreateEventWithPattern() {
      Map<String, String> metadata = Map.of("key1", "value1");
      ComponentEvent event =
          ComponentEvent.builder("event-type", "source-id")
              .targetComponentId("target-id")
              .payload("test-payload")
              .metadata(metadata)
              .priority(10)
              .build();

      assertEquals("event-type", event.getEventType(), "eventType should match");
      assertEquals("source-id", event.getSourceComponentId(), "sourceComponentId should match");
      assertEquals("target-id", event.getTargetComponentId(), "targetComponentId should match");
      assertEquals("test-payload", event.getPayload(), "payload should match");
      assertEquals(1, event.getMetadata().size(), "metadata should have 1 entry");
      assertEquals(10, event.getPriority(), "priority should be 10");
    }

    @Test
    @DisplayName("Builder methods should return Builder for chaining")
    void builderMethodsShouldReturnBuilderForChaining() {
      ComponentEvent.Builder builder = ComponentEvent.builder("type", "source");

      assertNotNull(builder.targetComponentId("target"), "targetComponentId should return builder");
      assertNotNull(builder.payload("payload"), "payload should return builder");
      assertNotNull(builder.metadata(Map.of()), "metadata should return builder");
      assertNotNull(builder.priority(1), "priority should return builder");
    }

    @Test
    @DisplayName("Builder should have targetComponentId method")
    void builderShouldHaveTargetComponentIdMethod() throws NoSuchMethodException {
      Method method = ComponentEvent.Builder.class.getMethod("targetComponentId", String.class);
      assertNotNull(method, "targetComponentId method should exist");
      assertEquals(
          ComponentEvent.Builder.class, method.getReturnType(), "Return type should be Builder");
    }

    @Test
    @DisplayName("Builder should have payload method")
    void builderShouldHavePayloadMethod() throws NoSuchMethodException {
      Method method = ComponentEvent.Builder.class.getMethod("payload", Object.class);
      assertNotNull(method, "payload method should exist");
      assertEquals(
          ComponentEvent.Builder.class, method.getReturnType(), "Return type should be Builder");
    }

    @Test
    @DisplayName("Builder should have metadata method")
    void builderShouldHaveMetadataMethod() throws NoSuchMethodException {
      Method method = ComponentEvent.Builder.class.getMethod("metadata", Map.class);
      assertNotNull(method, "metadata method should exist");
      assertEquals(
          ComponentEvent.Builder.class, method.getReturnType(), "Return type should be Builder");
    }

    @Test
    @DisplayName("Builder should have priority method")
    void builderShouldHavePriorityMethod() throws NoSuchMethodException {
      Method method = ComponentEvent.Builder.class.getMethod("priority", int.class);
      assertNotNull(method, "priority method should exist");
      assertEquals(
          ComponentEvent.Builder.class, method.getReturnType(), "Return type should be Builder");
    }

    @Test
    @DisplayName("Builder should have build method returning ComponentEvent")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = ComponentEvent.Builder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          ComponentEvent.class, method.getReturnType(), "Return type should be ComponentEvent");
    }

    @Test
    @DisplayName("Builder with null metadata should throw NullPointerException")
    void builderWithNullMetadataShouldThrowException() {
      // The metadata method stores null when passed null, and the constructor throws
      // NullPointerException for null metadata when Map.copyOf() is called
      assertThrows(
          NullPointerException.class,
          () -> ComponentEvent.builder("type", "source").metadata(null).build(),
          "Building with null metadata should throw NullPointerException");
    }
  }

  // ========================================================================
  // Functional Tests
  // ========================================================================

  @Nested
  @DisplayName("Functional Tests")
  class FunctionalTests {

    @Test
    @DisplayName("Event with null payload should be valid")
    void eventWithNullPayloadShouldBeValid() {
      ComponentEvent event = new ComponentEvent("type", "source", null);
      assertNull(event.getPayload(), "Null payload should be allowed");
    }

    @Test
    @DisplayName("Event with complex payload should preserve payload")
    void eventWithComplexPayloadShouldPreservePayload() {
      Map<String, Object> complexPayload = Map.of("name", "test", "count", 42);
      ComponentEvent event = new ComponentEvent("type", "source", complexPayload);
      assertEquals(complexPayload, event.getPayload(), "Complex payload should be preserved");
    }

    @Test
    @DisplayName("Event with negative priority should be allowed")
    void eventWithNegativePriorityShouldBeAllowed() {
      ComponentEvent event = new ComponentEvent("type", "source", null, null, Map.of(), -5);
      assertEquals(-5, event.getPriority(), "Negative priority should be allowed");
    }

    @Test
    @DisplayName("Event ID should be UUID format")
    void eventIdShouldBeUuidFormat() {
      ComponentEvent event = new ComponentEvent("type", "source", null);
      String eventId = event.getEventId();

      // UUID format: 8-4-4-4-12 = 36 characters with hyphens
      assertEquals(36, eventId.length(), "Event ID should be 36 characters (UUID format)");
      assertTrue(
          eventId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
          "Event ID should match UUID format");
    }

    @Test
    @DisplayName("Metadata should be a defensive copy")
    void metadataShouldBeDefensiveCopy() {
      Map<String, String> original = new HashMap<>();
      original.put("key", "value");
      ComponentEvent event = new ComponentEvent("type", "source", null, null, original, 0);

      // Modify original map
      original.put("newKey", "newValue");

      // Event metadata should not be affected
      assertEquals(1, event.getMetadata().size(), "Metadata should not be affected by original");
      assertFalse(
          event.getMetadata().containsKey("newKey"),
          "Metadata should not contain key added after creation");
    }
  }
}
