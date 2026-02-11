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

import ai.tegmentum.wasmtime4j.metadata.CustomSection;
import ai.tegmentum.wasmtime4j.metadata.CustomSectionParser;
import ai.tegmentum.wasmtime4j.metadata.CustomSectionType;
import ai.tegmentum.wasmtime4j.metadata.DefaultCustomSectionMetadata;
import ai.tegmentum.wasmtime4j.metadata.DefaultCustomSectionParser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link DefaultCustomSectionMetadata} custom section metadata implementation. */
@DisplayName("DefaultCustomSectionMetadata")
final class DefaultCustomSectionMetadataTest {

  private final CustomSectionParser parser = new DefaultCustomSectionParser();

  private CustomSection createSection(
      final String name, final byte[] data, final CustomSectionType type) {
    return new CustomSection(name, data, type);
  }

  private DefaultCustomSectionMetadata createMetadata(final List<CustomSection> sections) {
    return new DefaultCustomSectionMetadata(sections, parser);
  }

  @Nested
  @DisplayName("constructor validation")
  final class ConstructorValidationTests {

    @Test
    @DisplayName("should reject null custom sections list")
    void shouldRejectNullSections() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new DefaultCustomSectionMetadata(null, parser),
          "Expected IllegalArgumentException for null sections list");
    }

    @Test
    @DisplayName("should reject null parser")
    void shouldRejectNullParser() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new DefaultCustomSectionMetadata(List.of(), null),
          "Expected IllegalArgumentException for null parser");
    }
  }

  @Nested
  @DisplayName("getAllCustomSections")
  final class GetAllCustomSectionsTests {

    @Test
    @DisplayName("should return all sections")
    void shouldReturnAllSections() {
      final CustomSection section1 =
          createSection(
              "sec1", "data1".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN);
      final CustomSection section2 =
          createSection(
              "sec2", "data2".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN);
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of(section1, section2));
      assertEquals(2, metadata.getAllCustomSections().size(), "Should return all 2 sections");
    }

    @Test
    @DisplayName("should return empty list for no sections")
    void shouldReturnEmptyListForNoSections() {
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of());
      assertTrue(
          metadata.getAllCustomSections().isEmpty(),
          "Should return empty list when no sections exist");
    }
  }

  @Nested
  @DisplayName("getCustomSectionsByName")
  final class GetCustomSectionsByNameTests {

    @Test
    @DisplayName("should find sections by name")
    void shouldFindSectionsByName() {
      final CustomSection section =
          createSection("test", "data".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN);
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of(section));
      final List<CustomSection> found = metadata.getCustomSectionsByName("test");
      assertEquals(1, found.size(), "Should find 1 section with name 'test'");
    }

    @Test
    @DisplayName("should return empty list for unknown name")
    void shouldReturnEmptyListForUnknownName() {
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of());
      assertTrue(
          metadata.getCustomSectionsByName("nonexistent").isEmpty(),
          "Should return empty list for nonexistent name");
    }

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of());
      assertThrows(
          IllegalArgumentException.class,
          () -> metadata.getCustomSectionsByName(null),
          "Expected IllegalArgumentException for null name");
    }
  }

  @Nested
  @DisplayName("getCustomSectionsByType")
  final class GetCustomSectionsByTypeTests {

    @Test
    @DisplayName("should find sections by type")
    void shouldFindSectionsByType() {
      final CustomSection section =
          createSection(
              "unknown_sec", "data".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN);
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of(section));
      final List<CustomSection> found = metadata.getCustomSectionsByType(CustomSectionType.UNKNOWN);
      assertEquals(1, found.size(), "Should find 1 section of type UNKNOWN");
    }

    @Test
    @DisplayName("should reject null type")
    void shouldRejectNullType() {
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of());
      assertThrows(
          IllegalArgumentException.class,
          () -> metadata.getCustomSectionsByType(null),
          "Expected IllegalArgumentException for null type");
    }
  }

  @Nested
  @DisplayName("getFirstCustomSection")
  final class GetFirstCustomSectionTests {

    @Test
    @DisplayName("should return first section with given name")
    void shouldReturnFirstSection() {
      final CustomSection section1 =
          createSection(
              "duplicate", "first".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN);
      final CustomSection section2 =
          createSection(
              "duplicate", "second".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN);
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of(section1, section2));
      final Optional<CustomSection> first = metadata.getFirstCustomSection("duplicate");
      assertTrue(first.isPresent(), "Should find first section named 'duplicate'");
    }

    @Test
    @DisplayName("should return empty for nonexistent name")
    void shouldReturnEmptyForNonexistent() {
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of());
      final Optional<CustomSection> result = metadata.getFirstCustomSection("missing");
      assertFalse(result.isPresent(), "Should return empty for missing section");
    }

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullNameForFirstSection() {
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of());
      assertThrows(
          IllegalArgumentException.class,
          () -> metadata.getFirstCustomSection(null),
          "Expected IllegalArgumentException for null name");
    }
  }

  @Nested
  @DisplayName("hasCustomSection")
  final class HasCustomSectionTests {

    @Test
    @DisplayName("should return true for existing section name")
    void shouldReturnTrueForExistingName() {
      final CustomSection section =
          createSection("present", "d".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN);
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of(section));
      assertTrue(
          metadata.hasCustomSection("present"),
          "hasCustomSection should be true for existing name");
    }

    @Test
    @DisplayName("should return false for missing section name")
    void shouldReturnFalseForMissingName() {
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of());
      assertFalse(
          metadata.hasCustomSection("missing"),
          "hasCustomSection should be false for missing name");
    }

    @Test
    @DisplayName("should check by type")
    void shouldCheckByType() {
      final CustomSection section =
          createSection("test", "d".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN);
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of(section));
      assertTrue(
          metadata.hasCustomSection(CustomSectionType.UNKNOWN),
          "hasCustomSection by type should be true for UNKNOWN");
      assertFalse(
          metadata.hasCustomSection(CustomSectionType.NAME),
          "hasCustomSection by type should be false for NAME");
    }
  }

  @Nested
  @DisplayName("count and size")
  final class CountAndSizeTests {

    @Test
    @DisplayName("should count custom sections")
    void shouldCountCustomSections() {
      final CustomSection s1 =
          createSection("a", "data".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN);
      final CustomSection s2 =
          createSection("b", "more".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN);
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of(s1, s2));
      assertEquals(2, metadata.getCustomSectionCount(), "Count should be 2");
    }

    @Test
    @DisplayName("should calculate total size")
    void shouldCalculateTotalSize() {
      final byte[] data1 = "hello".getBytes(StandardCharsets.UTF_8);
      final byte[] data2 = "world!".getBytes(StandardCharsets.UTF_8);
      final CustomSection s1 = createSection("a", data1, CustomSectionType.UNKNOWN);
      final CustomSection s2 = createSection("b", data2, CustomSectionType.UNKNOWN);
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of(s1, s2));
      assertEquals(
          data1.length + data2.length,
          metadata.getCustomSectionsTotalSize(),
          "Total size should be sum of section data sizes");
    }
  }

  @Nested
  @DisplayName("section names and types")
  final class SectionNamesAndTypesTests {

    @Test
    @DisplayName("should return set of section names")
    void shouldReturnSetOfNames() {
      final CustomSection s1 =
          createSection("alpha", "d".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN);
      final CustomSection s2 =
          createSection("beta", "d".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN);
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of(s1, s2));
      final Set<String> names = metadata.getCustomSectionNames();
      assertEquals(2, names.size(), "Should have 2 unique names");
      assertTrue(names.contains("alpha"), "Names should contain 'alpha'");
      assertTrue(names.contains("beta"), "Names should contain 'beta'");
    }

    @Test
    @DisplayName("should return set of section types")
    void shouldReturnSetOfTypes() {
      final CustomSection section =
          createSection("test", "d".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN);
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of(section));
      final Set<CustomSectionType> types = metadata.getCustomSectionTypes();
      assertTrue(types.contains(CustomSectionType.UNKNOWN), "Types should contain UNKNOWN");
    }
  }

  @Nested
  @DisplayName("summary")
  final class SummaryTests {

    @Test
    @DisplayName("should produce non-null summary")
    void shouldProduceNonNullSummary() {
      final DefaultCustomSectionMetadata metadata = createMetadata(List.of());
      final String summary = metadata.getCustomSectionsSummary();
      assertNotNull(summary, "Summary should not be null");
      assertTrue(summary.contains("Custom Sections Summary"), "Summary should contain header text");
    }
  }
}
