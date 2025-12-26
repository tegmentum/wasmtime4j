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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentMetadata} class.
 *
 * <p>ComponentMetadata provides descriptive information about a component.
 */
@DisplayName("ComponentMetadata Tests")
class ComponentMetadataTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentMetadata.class.getModifiers()),
          "ComponentMetadata should be public");
      assertTrue(
          Modifier.isFinal(ComponentMetadata.class.getModifiers()),
          "ComponentMetadata should be final");
    }
  }

  @Nested
  @DisplayName("Minimal Constructor Tests")
  class MinimalConstructorTests {

    @Test
    @DisplayName("should create instance with minimal parameters")
    void shouldCreateInstanceWithMinimalParameters() {
      final ComponentVersion version = new ComponentVersion(1, 0, 0);
      final var metadata = new ComponentMetadata("test-component", version, "A test component");

      assertEquals("test-component", metadata.getName(), "Name should match");
      assertEquals(version, metadata.getVersion(), "Version should match");
      assertEquals("A test component", metadata.getDescription(), "Description should match");
      assertNull(metadata.getAuthor(), "Author should be null");
      assertNull(metadata.getLicense(), "License should be null");
      assertTrue(metadata.getProperties().isEmpty(), "Properties should be empty");
    }

    @Test
    @DisplayName("should set timestamp on creation")
    void shouldSetTimestampOnCreation() {
      final long beforeCreation = System.currentTimeMillis();
      final var metadata =
          new ComponentMetadata("test", new ComponentVersion(1, 0, 0), "Test description");
      final long afterCreation = System.currentTimeMillis();

      assertTrue(
          metadata.getCreatedTimestamp() >= beforeCreation,
          "Timestamp should be >= creation start time");
      assertTrue(
          metadata.getCreatedTimestamp() <= afterCreation,
          "Timestamp should be <= creation end time");
    }
  }

  @Nested
  @DisplayName("Full Constructor Tests")
  class FullConstructorTests {

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final ComponentVersion version = new ComponentVersion(2, 1, 3);
      final Map<String, String> properties = Map.of("key1", "value1", "key2", "value2");
      final var metadata =
          new ComponentMetadata(
              "full-component", version, "Full description", "Test Author", "MIT", properties);

      assertEquals("full-component", metadata.getName(), "Name should match");
      assertEquals(version, metadata.getVersion(), "Version should match");
      assertEquals("Full description", metadata.getDescription(), "Description should match");
      assertEquals("Test Author", metadata.getAuthor(), "Author should match");
      assertEquals("MIT", metadata.getLicense(), "License should match");
      assertEquals(2, metadata.getProperties().size(), "Properties should have 2 entries");
      assertEquals("value1", metadata.getProperties().get("key1"), "Property key1 should match");
    }

    @Test
    @DisplayName("should handle null name")
    void shouldHandleNullName() {
      final var metadata =
          new ComponentMetadata(
              null, new ComponentVersion(1, 0, 0), "description", null, null, null);

      assertEquals("unknown", metadata.getName(), "Null name should default to 'unknown'");
    }

    @Test
    @DisplayName("should handle null version")
    void shouldHandleNullVersion() {
      final var metadata = new ComponentMetadata("test", null, "description", null, null, null);

      assertNotNull(metadata.getVersion(), "Version should not be null");
      assertEquals(1, metadata.getVersion().getMajor(), "Default version major should be 1");
      assertEquals(0, metadata.getVersion().getMinor(), "Default version minor should be 0");
      assertEquals(0, metadata.getVersion().getPatch(), "Default version patch should be 0");
    }

    @Test
    @DisplayName("should handle null description")
    void shouldHandleNullDescription() {
      final var metadata =
          new ComponentMetadata("test", new ComponentVersion(1, 0, 0), null, null, null, null);

      assertEquals(
          "", metadata.getDescription(), "Null description should default to empty string");
    }

    @Test
    @DisplayName("should handle null properties")
    void shouldHandleNullProperties() {
      final var metadata =
          new ComponentMetadata("test", new ComponentVersion(1, 0, 0), "desc", null, null, null);

      assertNotNull(metadata.getProperties(), "Properties should not be null");
      assertTrue(metadata.getProperties().isEmpty(), "Properties should be empty");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getName should return correct value")
    void getNameShouldReturnCorrectValue() {
      final var metadata =
          new ComponentMetadata("my-component", new ComponentVersion(1, 0, 0), "desc");

      assertEquals("my-component", metadata.getName(), "Should return component name");
    }

    @Test
    @DisplayName("getVersion should return correct value")
    void getVersionShouldReturnCorrectValue() {
      final ComponentVersion version = new ComponentVersion(3, 2, 1);
      final var metadata = new ComponentMetadata("comp", version, "desc");

      assertEquals(version, metadata.getVersion(), "Should return version");
    }

    @Test
    @DisplayName("getDescription should return correct value")
    void getDescriptionShouldReturnCorrectValue() {
      final var metadata =
          new ComponentMetadata("comp", new ComponentVersion(1, 0, 0), "My description");

      assertEquals("My description", metadata.getDescription(), "Should return description");
    }

    @Test
    @DisplayName("getAuthor should return correct value")
    void getAuthorShouldReturnCorrectValue() {
      final var metadata =
          new ComponentMetadata(
              "comp", new ComponentVersion(1, 0, 0), "desc", "John Doe", null, null);

      assertEquals("John Doe", metadata.getAuthor(), "Should return author");
    }

    @Test
    @DisplayName("getLicense should return correct value")
    void getLicenseShouldReturnCorrectValue() {
      final var metadata =
          new ComponentMetadata(
              "comp", new ComponentVersion(1, 0, 0), "desc", null, "Apache-2.0", null);

      assertEquals("Apache-2.0", metadata.getLicense(), "Should return license");
    }

    @Test
    @DisplayName("getProperties should return immutable copy")
    void getPropertiesShouldReturnImmutableCopy() {
      final Map<String, String> properties = Map.of("key", "value");
      final var metadata =
          new ComponentMetadata(
              "comp", new ComponentVersion(1, 0, 0), "desc", null, null, properties);

      final Map<String, String> returnedProperties = metadata.getProperties();
      assertEquals("value", returnedProperties.get("key"), "Should contain original value");
    }

    @Test
    @DisplayName("getCreatedTimestamp should return positive value")
    void getCreatedTimestampShouldReturnPositiveValue() {
      final var metadata = new ComponentMetadata("comp", new ComponentVersion(1, 0, 0), "desc");

      assertTrue(metadata.getCreatedTimestamp() > 0, "Created timestamp should be positive");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty strings")
    void shouldHandleEmptyStrings() {
      final var metadata =
          new ComponentMetadata("", new ComponentVersion(1, 0, 0), "", "", "", Map.of());

      assertEquals("", metadata.getName(), "Empty name should work");
      assertEquals("", metadata.getDescription(), "Empty description should work");
      assertEquals("", metadata.getAuthor(), "Empty author should work");
      assertEquals("", metadata.getLicense(), "Empty license should work");
    }

    @Test
    @DisplayName("should handle properties with many entries")
    void shouldHandlePropertiesWithManyEntries() {
      final Map<String, String> properties =
          Map.of(
              "key1", "value1",
              "key2", "value2",
              "key3", "value3",
              "key4", "value4",
              "key5", "value5");
      final var metadata =
          new ComponentMetadata(
              "comp", new ComponentVersion(1, 0, 0), "desc", null, null, properties);

      assertEquals(5, metadata.getProperties().size(), "Should have 5 properties");
    }

    @Test
    @DisplayName("should handle special characters in strings")
    void shouldHandleSpecialCharactersInStrings() {
      final var metadata =
          new ComponentMetadata(
              "comp-with-special!@#",
              new ComponentVersion(1, 0, 0),
              "Desc with émojis 🎉",
              null,
              null,
              null);

      assertEquals(
          "comp-with-special!@#", metadata.getName(), "Should handle special chars in name");
      assertEquals(
          "Desc with émojis 🎉", metadata.getDescription(), "Should handle unicode in description");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final var metadata =
          new ComponentMetadata(
              "my-component", new ComponentVersion(1, 2, 3), "Component description");

      final String result = metadata.toString();

      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("my-component"), "Should contain component name");
      assertTrue(result.contains("Component description"), "Should contain description");
    }
  }
}
