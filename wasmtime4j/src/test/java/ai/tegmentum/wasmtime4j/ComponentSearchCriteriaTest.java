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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentSearchCriteria} class.
 *
 * <p>ComponentSearchCriteria provides search criteria for finding components in a
 * ComponentRegistry.
 */
@DisplayName("ComponentSearchCriteria Tests")
class ComponentSearchCriteriaTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentSearchCriteria.class.getModifiers()),
          "ComponentSearchCriteria should be public");
      assertTrue(
          Modifier.isFinal(ComponentSearchCriteria.class.getModifiers()),
          "ComponentSearchCriteria should be final");
    }

    @Test
    @DisplayName("should have public static Builder class")
    void shouldHavePublicStaticBuilderClass() {
      final var nestedClasses = ComponentSearchCriteria.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("Builder")) {
          found = true;
          assertTrue(Modifier.isPublic(nestedClass.getModifiers()), "Builder should be public");
          assertTrue(Modifier.isStatic(nestedClass.getModifiers()), "Builder should be static");
          assertTrue(Modifier.isFinal(nestedClass.getModifiers()), "Builder should be final");
          break;
        }
      }
      assertTrue(found, "Should have Builder nested class");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create builder via static method")
    void shouldCreateBuilderViaStaticMethod() {
      final var builder = ComponentSearchCriteria.builder();

      assertNotNull(builder, "Builder should not be null");
    }

    @Test
    @DisplayName("should build empty criteria")
    void shouldBuildEmptyCriteria() {
      final var criteria = ComponentSearchCriteria.builder().build();

      assertNotNull(criteria, "Criteria should not be null");
      assertTrue(criteria.getNamePattern().isEmpty(), "Name pattern should be empty");
      assertTrue(criteria.getMinVersion().isEmpty(), "Min version should be empty");
      assertTrue(criteria.getMaxVersion().isEmpty(), "Max version should be empty");
      assertTrue(criteria.getRequiredInterfaces().isEmpty(), "Required interfaces should be empty");
      assertTrue(criteria.getExcludedInterfaces().isEmpty(), "Excluded interfaces should be empty");
      assertTrue(criteria.getAuthor().isEmpty(), "Author should be empty");
      assertTrue(criteria.getDescription().isEmpty(), "Description should be empty");
      assertTrue(criteria.getTags().isEmpty(), "Tags should be empty");
      assertTrue(criteria.getMaxSize().isEmpty(), "Max size should be empty");
      assertTrue(criteria.getMinSize().isEmpty(), "Min size should be empty");
    }

    @Test
    @DisplayName("should build criteria with name pattern")
    void shouldBuildCriteriaWithNamePattern() {
      final var criteria = ComponentSearchCriteria.builder().namePattern("test-*").build();

      assertTrue(criteria.getNamePattern().isPresent(), "Name pattern should be present");
      assertEquals("test-*", criteria.getNamePattern().get(), "Name pattern should match");
    }

    @Test
    @DisplayName("should build criteria with version range")
    void shouldBuildCriteriaWithVersionRange() {
      final ComponentVersion minVersion = new ComponentVersion(1, 0, 0);
      final ComponentVersion maxVersion = new ComponentVersion(2, 0, 0);
      final var criteria =
          ComponentSearchCriteria.builder().minVersion(minVersion).maxVersion(maxVersion).build();

      assertTrue(criteria.getMinVersion().isPresent(), "Min version should be present");
      assertTrue(criteria.getMaxVersion().isPresent(), "Max version should be present");
      assertEquals(minVersion, criteria.getMinVersion().get(), "Min version should match");
      assertEquals(maxVersion, criteria.getMaxVersion().get(), "Max version should match");
    }

    @Test
    @DisplayName("should build criteria with required interfaces")
    void shouldBuildCriteriaWithRequiredInterfaces() {
      final Set<String> interfaces = Set.of("interface1", "interface2");
      final var criteria = ComponentSearchCriteria.builder().requiredInterfaces(interfaces).build();

      assertEquals(2, criteria.getRequiredInterfaces().size(), "Should have 2 required interfaces");
      assertTrue(
          criteria.getRequiredInterfaces().contains("interface1"), "Should contain interface1");
      assertTrue(
          criteria.getRequiredInterfaces().contains("interface2"), "Should contain interface2");
    }

    @Test
    @DisplayName("should build criteria with excluded interfaces")
    void shouldBuildCriteriaWithExcludedInterfaces() {
      final Set<String> interfaces = Set.of("excluded1");
      final var criteria = ComponentSearchCriteria.builder().excludedInterfaces(interfaces).build();

      assertEquals(1, criteria.getExcludedInterfaces().size(), "Should have 1 excluded interface");
      assertTrue(
          criteria.getExcludedInterfaces().contains("excluded1"), "Should contain excluded1");
    }

    @Test
    @DisplayName("should build criteria with author")
    void shouldBuildCriteriaWithAuthor() {
      final var criteria = ComponentSearchCriteria.builder().author("John Doe").build();

      assertTrue(criteria.getAuthor().isPresent(), "Author should be present");
      assertEquals("John Doe", criteria.getAuthor().get(), "Author should match");
    }

    @Test
    @DisplayName("should build criteria with description")
    void shouldBuildCriteriaWithDescription() {
      final var criteria =
          ComponentSearchCriteria.builder().description("search description").build();

      assertTrue(criteria.getDescription().isPresent(), "Description should be present");
      assertEquals(
          "search description", criteria.getDescription().get(), "Description should match");
    }

    @Test
    @DisplayName("should build criteria with tags")
    void shouldBuildCriteriaWithTags() {
      final Set<String> tags = Set.of("tag1", "tag2", "tag3");
      final var criteria = ComponentSearchCriteria.builder().tags(tags).build();

      assertEquals(3, criteria.getTags().size(), "Should have 3 tags");
      assertTrue(criteria.getTags().contains("tag1"), "Should contain tag1");
    }

    @Test
    @DisplayName("should build criteria with size constraints")
    void shouldBuildCriteriaWithSizeConstraints() {
      final var criteria =
          ComponentSearchCriteria.builder().minSize(1024L).maxSize(1024L * 1024).build();

      assertTrue(criteria.getMinSize().isPresent(), "Min size should be present");
      assertTrue(criteria.getMaxSize().isPresent(), "Max size should be present");
      assertEquals(1024L, criteria.getMinSize().get(), "Min size should be 1KB");
      assertEquals(1024L * 1024, criteria.getMaxSize().get(), "Max size should be 1MB");
    }

    @Test
    @DisplayName("should support method chaining")
    void shouldSupportMethodChaining() {
      final var criteria =
          ComponentSearchCriteria.builder()
              .namePattern("*-component")
              .author("Test Author")
              .description("Test Description")
              .tags(Set.of("production"))
              .minSize(100L)
              .maxSize(10000L)
              .build();

      assertTrue(criteria.getNamePattern().isPresent(), "Name pattern should be set");
      assertTrue(criteria.getAuthor().isPresent(), "Author should be set");
      assertTrue(criteria.getDescription().isPresent(), "Description should be set");
      assertFalse(criteria.getTags().isEmpty(), "Tags should be set");
      assertTrue(criteria.getMinSize().isPresent(), "Min size should be set");
      assertTrue(criteria.getMaxSize().isPresent(), "Max size should be set");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getNamePattern should return Optional")
    void getNamePatternShouldReturnOptional() {
      final var criteria = ComponentSearchCriteria.builder().build();

      final Optional<String> result = criteria.getNamePattern();
      assertNotNull(result, "Should return Optional");
      assertTrue(result.isEmpty(), "Should be empty when not set");
    }

    @Test
    @DisplayName("getRequiredInterfaces should return immutable set")
    void getRequiredInterfacesShouldReturnImmutableSet() {
      final var criteria =
          ComponentSearchCriteria.builder().requiredInterfaces(Set.of("interface1")).build();

      final Set<String> result = criteria.getRequiredInterfaces();
      assertNotNull(result, "Should return Set");
      assertEquals(1, result.size(), "Should have 1 interface");
    }

    @Test
    @DisplayName("getTags should return immutable set")
    void getTagsShouldReturnImmutableSet() {
      final var criteria = ComponentSearchCriteria.builder().tags(Set.of("tag1", "tag2")).build();

      final Set<String> result = criteria.getTags();
      assertNotNull(result, "Should return Set");
      assertEquals(2, result.size(), "Should have 2 tags");
    }
  }

  @Nested
  @DisplayName("Null Handling Tests")
  class NullHandlingTests {

    @Test
    @DisplayName("should handle null name pattern")
    void shouldHandleNullNamePattern() {
      final var criteria = ComponentSearchCriteria.builder().namePattern(null).build();

      assertTrue(criteria.getNamePattern().isEmpty(), "Null should result in empty Optional");
    }

    @Test
    @DisplayName("should handle null required interfaces")
    void shouldHandleNullRequiredInterfaces() {
      final var criteria = ComponentSearchCriteria.builder().requiredInterfaces(null).build();

      assertNotNull(criteria.getRequiredInterfaces(), "Should return empty set, not null");
      assertTrue(criteria.getRequiredInterfaces().isEmpty(), "Should be empty");
    }

    @Test
    @DisplayName("should handle null excluded interfaces")
    void shouldHandleNullExcludedInterfaces() {
      final var criteria = ComponentSearchCriteria.builder().excludedInterfaces(null).build();

      assertNotNull(criteria.getExcludedInterfaces(), "Should return empty set, not null");
      assertTrue(criteria.getExcludedInterfaces().isEmpty(), "Should be empty");
    }

    @Test
    @DisplayName("should handle null tags")
    void shouldHandleNullTags() {
      final var criteria = ComponentSearchCriteria.builder().tags(null).build();

      assertNotNull(criteria.getTags(), "Should return empty set, not null");
      assertTrue(criteria.getTags().isEmpty(), "Should be empty");
    }

    @Test
    @DisplayName("should handle null author")
    void shouldHandleNullAuthor() {
      final var criteria = ComponentSearchCriteria.builder().author(null).build();

      assertTrue(criteria.getAuthor().isEmpty(), "Null should result in empty Optional");
    }

    @Test
    @DisplayName("should handle null description")
    void shouldHandleNullDescription() {
      final var criteria = ComponentSearchCriteria.builder().description(null).build();

      assertTrue(criteria.getDescription().isEmpty(), "Null should result in empty Optional");
    }

    @Test
    @DisplayName("should handle null versions")
    void shouldHandleNullVersions() {
      final var criteria =
          ComponentSearchCriteria.builder().minVersion(null).maxVersion(null).build();

      assertTrue(criteria.getMinVersion().isEmpty(), "Null min version should be empty");
      assertTrue(criteria.getMaxVersion().isEmpty(), "Null max version should be empty");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty strings in pattern")
    void shouldHandleEmptyStringsInPattern() {
      final var criteria = ComponentSearchCriteria.builder().namePattern("").build();

      assertTrue(criteria.getNamePattern().isPresent(), "Empty string should be present");
      assertEquals("", criteria.getNamePattern().get(), "Should be empty string");
    }

    @Test
    @DisplayName("should handle empty sets")
    void shouldHandleEmptySets() {
      final var criteria =
          ComponentSearchCriteria.builder()
              .requiredInterfaces(Set.of())
              .excludedInterfaces(Set.of())
              .tags(Set.of())
              .build();

      assertTrue(criteria.getRequiredInterfaces().isEmpty(), "Should be empty");
      assertTrue(criteria.getExcludedInterfaces().isEmpty(), "Should be empty");
      assertTrue(criteria.getTags().isEmpty(), "Should be empty");
    }

    @Test
    @DisplayName("should handle large size values")
    void shouldHandleLargeSizeValues() {
      final var criteria =
          ComponentSearchCriteria.builder()
              .minSize(Long.MAX_VALUE / 2)
              .maxSize(Long.MAX_VALUE)
              .build();

      assertEquals(Long.MAX_VALUE / 2, criteria.getMinSize().get(), "Should handle large min size");
      assertEquals(Long.MAX_VALUE, criteria.getMaxSize().get(), "Should handle max long");
    }
  }
}
