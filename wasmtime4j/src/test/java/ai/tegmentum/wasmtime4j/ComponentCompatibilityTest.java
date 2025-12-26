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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentCompatibility} class.
 *
 * <p>ComponentCompatibility represents the compatibility status between two components.
 */
@DisplayName("ComponentCompatibility Tests")
class ComponentCompatibilityTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentCompatibility.class.getModifiers()),
          "ComponentCompatibility should be public");
      assertTrue(
          Modifier.isFinal(ComponentCompatibility.class.getModifiers()),
          "ComponentCompatibility should be final");
    }

    @Test
    @DisplayName("should have constructor with boolean and String")
    void shouldHaveConstructorWithBooleanAndString() throws NoSuchMethodException {
      final Constructor<ComponentCompatibility> constructor =
          ComponentCompatibility.class.getConstructor(boolean.class, String.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create compatible instance")
    void shouldCreateCompatibleInstance() {
      final ComponentCompatibility compatibility =
          new ComponentCompatibility(true, "Components are compatible");

      assertTrue(compatibility.isCompatible(), "Should be compatible");
      assertEquals("Components are compatible", compatibility.getMessage(), "Message should match");
    }

    @Test
    @DisplayName("should create incompatible instance")
    void shouldCreateIncompatibleInstance() {
      final ComponentCompatibility compatibility =
          new ComponentCompatibility(false, "Version mismatch");

      assertFalse(compatibility.isCompatible(), "Should not be compatible");
      assertEquals("Version mismatch", compatibility.getMessage(), "Message should match");
    }

    @Test
    @DisplayName("should allow null message")
    void shouldAllowNullMessage() {
      final ComponentCompatibility compatibility = new ComponentCompatibility(true, null);

      assertTrue(compatibility.isCompatible(), "Should be compatible");
      assertNull(compatibility.getMessage(), "Message should be null");
    }

    @Test
    @DisplayName("should allow empty message")
    void shouldAllowEmptyMessage() {
      final ComponentCompatibility compatibility = new ComponentCompatibility(true, "");

      assertTrue(compatibility.isCompatible(), "Should be compatible");
      assertEquals("", compatibility.getMessage(), "Message should be empty");
    }
  }

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have isCompatible method")
    void shouldHaveIsCompatibleMethod() throws NoSuchMethodException {
      final Method method = ComponentCompatibility.class.getMethod("isCompatible");
      assertNotNull(method, "isCompatible method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMessage method")
    void shouldHaveGetMessageMethod() throws NoSuchMethodException {
      final Method method = ComponentCompatibility.class.getMethod("getMessage");
      assertNotNull(method, "getMessage method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("isCompatible should return true for compatible")
    void isCompatibleShouldReturnTrueForCompatible() {
      final ComponentCompatibility compatibility = new ComponentCompatibility(true, "Compatible");

      assertTrue(compatibility.isCompatible(), "isCompatible should return true");
    }

    @Test
    @DisplayName("isCompatible should return false for incompatible")
    void isCompatibleShouldReturnFalseForIncompatible() {
      final ComponentCompatibility compatibility =
          new ComponentCompatibility(false, "Incompatible");

      assertFalse(compatibility.isCompatible(), "isCompatible should return false");
    }

    @Test
    @DisplayName("getMessage should return the message")
    void getMessageShouldReturnTheMessage() {
      final String expectedMessage = "Test message";
      final ComponentCompatibility compatibility =
          new ComponentCompatibility(true, expectedMessage);

      assertEquals(expectedMessage, compatibility.getMessage(), "getMessage should return message");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle very long message")
    void shouldHandleVeryLongMessage() {
      final String longMessage = "A".repeat(10000);
      final ComponentCompatibility compatibility = new ComponentCompatibility(true, longMessage);

      assertEquals(longMessage, compatibility.getMessage(), "Long message should be stored");
    }

    @Test
    @DisplayName("should handle message with special characters")
    void shouldHandleMessageWithSpecialCharacters() {
      final String specialMessage =
          "Compatibility check failed: \n\t - Missing interface\n\t - Version < 1.0";
      final ComponentCompatibility compatibility =
          new ComponentCompatibility(false, specialMessage);

      assertEquals(
          specialMessage, compatibility.getMessage(), "Special characters should be preserved");
    }

    @Test
    @DisplayName("should handle unicode in message")
    void shouldHandleUnicodeInMessage() {
      final String unicodeMessage = "兼容性检查失败 - Compatibility check failed - 互換性チェック失敗";
      final ComponentCompatibility compatibility =
          new ComponentCompatibility(false, unicodeMessage);

      assertEquals(unicodeMessage, compatibility.getMessage(), "Unicode should be preserved");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("should not have setters")
    void shouldNotHaveSetters() {
      final var methods = ComponentCompatibility.class.getDeclaredMethods();

      for (final var method : methods) {
        assertFalse(
            method.getName().startsWith("set"),
            "Should not have setter methods: " + method.getName());
      }
    }

    @Test
    @DisplayName("fields should be final")
    void fieldsShouldBeFinal() {
      final var fields = ComponentCompatibility.class.getDeclaredFields();

      for (final var field : fields) {
        assertTrue(
            Modifier.isFinal(field.getModifiers()), "Field should be final: " + field.getName());
      }
    }
  }
}
