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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link ProducersSection} producers custom section data. */
@DisplayName("ProducersSection")
final class ProducersSectionTest {

  @Nested
  @DisplayName("builder defaults")
  final class BuilderDefaultTests {

    @Test
    @DisplayName("should build empty producers section")
    void shouldBuildEmptySection() {
      final ProducersSection section = ProducersSection.builder().build();
      assertNotNull(section, "Built section should not be null");
      assertTrue(section.getLanguages().isEmpty(), "Default languages should be empty");
      assertTrue(section.getProcessedBy().isEmpty(), "Default processedBy should be empty");
      assertTrue(section.getSdk().isEmpty(), "Default SDK should be empty");
      assertTrue(section.isEmpty(), "Empty section should return true for isEmpty");
    }

    @Test
    @DisplayName("should return empty getAllEntries for empty section")
    void shouldReturnEmptyAllEntries() {
      final ProducersSection section = ProducersSection.builder().build();
      assertTrue(section.getAllEntries().isEmpty(), "getAllEntries should be empty for empty section");
    }
  }

  @Nested
  @DisplayName("builder with set methods")
  final class BuilderSetMethodTests {

    @Test
    @DisplayName("should set languages list")
    void shouldSetLanguages() {
      final List<ProducersSection.ProducerEntry> langs =
          List.of(new ProducersSection.ProducerEntry("Rust", "1.70.0"));
      final ProducersSection section =
          ProducersSection.builder().setLanguages(langs).build();
      assertEquals(1, section.getLanguages().size(), "Should have 1 language entry");
      assertEquals("Rust", section.getLanguages().get(0).getName(), "Language name should be Rust");
    }

    @Test
    @DisplayName("should set processedBy list")
    void shouldSetProcessedBy() {
      final List<ProducersSection.ProducerEntry> tools =
          List.of(new ProducersSection.ProducerEntry("clang", "15.0"));
      final ProducersSection section =
          ProducersSection.builder().setProcessedBy(tools).build();
      assertEquals(1, section.getProcessedBy().size(), "Should have 1 processedBy entry");
      assertEquals("clang", section.getProcessedBy().get(0).getName(), "Tool name should be clang");
    }

    @Test
    @DisplayName("should set SDK list")
    void shouldSetSdk() {
      final List<ProducersSection.ProducerEntry> sdks =
          List.of(new ProducersSection.ProducerEntry("emscripten", "3.1.0"));
      final ProducersSection section =
          ProducersSection.builder().setSdk(sdks).build();
      assertEquals(1, section.getSdk().size(), "Should have 1 SDK entry");
      assertEquals(
          "emscripten", section.getSdk().get(0).getName(), "SDK name should be emscripten");
    }

    @Test
    @DisplayName("should handle null set values gracefully")
    void shouldHandleNullSetValues() {
      final ProducersSection section =
          ProducersSection.builder()
              .setLanguages(null)
              .setProcessedBy(null)
              .setSdk(null)
              .build();
      assertTrue(section.getLanguages().isEmpty(), "Null languages should default to empty");
      assertTrue(section.getProcessedBy().isEmpty(), "Null processedBy should default to empty");
      assertTrue(section.getSdk().isEmpty(), "Null SDK should default to empty");
    }
  }

  @Nested
  @DisplayName("builder add methods")
  final class BuilderAddMethodTests {

    @Test
    @DisplayName("should add language entry")
    void shouldAddLanguage() {
      final ProducersSection section =
          ProducersSection.builder()
              .addLanguage(new ProducersSection.ProducerEntry("C", "17"))
              .addLanguage(new ProducersSection.ProducerEntry("C++", "20"))
              .build();
      assertEquals(2, section.getLanguages().size(), "Should have 2 language entries");
    }

    @Test
    @DisplayName("should add processedBy entry")
    void shouldAddProcessedBy() {
      final ProducersSection section =
          ProducersSection.builder()
              .addProcessedBy(new ProducersSection.ProducerEntry("wasm-ld", "15.0"))
              .build();
      assertEquals(1, section.getProcessedBy().size(), "Should have 1 processedBy entry");
    }

    @Test
    @DisplayName("should add SDK entry")
    void shouldAddSdk() {
      final ProducersSection section =
          ProducersSection.builder()
              .addSdk(new ProducersSection.ProducerEntry("wasmtime-sdk", "10.0.0"))
              .build();
      assertEquals(1, section.getSdk().size(), "Should have 1 SDK entry");
    }

    @Test
    @DisplayName("should reject null language entry")
    void shouldRejectNullLanguageEntry() {
      final ProducersSection.Builder builder = ProducersSection.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.addLanguage(null),
          "Expected IllegalArgumentException for null language entry");
    }

    @Test
    @DisplayName("should reject null processedBy entry")
    void shouldRejectNullProcessedByEntry() {
      final ProducersSection.Builder builder = ProducersSection.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.addProcessedBy(null),
          "Expected IllegalArgumentException for null processedBy entry");
    }

    @Test
    @DisplayName("should reject null SDK entry")
    void shouldRejectNullSdkEntry() {
      final ProducersSection.Builder builder = ProducersSection.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.addSdk(null),
          "Expected IllegalArgumentException for null SDK entry");
    }
  }

  @Nested
  @DisplayName("isEmpty and getSummary")
  final class IsEmptyAndSummaryTests {

    @Test
    @DisplayName("should not be empty when has entries")
    void shouldNotBeEmptyWithEntries() {
      final ProducersSection section =
          ProducersSection.builder()
              .addLanguage(new ProducersSection.ProducerEntry("Rust", "1.70.0"))
              .build();
      assertFalse(section.isEmpty(), "Section with entries should not be empty");
    }

    @Test
    @DisplayName("should return accurate summary")
    void shouldReturnAccurateSummary() {
      final ProducersSection section =
          ProducersSection.builder()
              .addLanguage(new ProducersSection.ProducerEntry("Rust", "1.70.0"))
              .addProcessedBy(new ProducersSection.ProducerEntry("rustc", "1.70.0"))
              .addProcessedBy(new ProducersSection.ProducerEntry("wasm-ld", "15.0"))
              .addSdk(new ProducersSection.ProducerEntry("wasmtime", "10.0"))
              .build();
      final String summary = section.getSummary();
      assertTrue(summary.contains("languages=1"), "Summary should contain languages=1");
      assertTrue(summary.contains("processedBy=2"), "Summary should contain processedBy=2");
      assertTrue(summary.contains("sdk=1"), "Summary should contain sdk=1");
    }
  }

  @Nested
  @DisplayName("getAllEntries")
  final class GetAllEntriesTests {

    @Test
    @DisplayName("should combine all entries")
    void shouldCombineAllEntries() {
      final ProducersSection section =
          ProducersSection.builder()
              .addLanguage(new ProducersSection.ProducerEntry("Rust", "1.70.0"))
              .addProcessedBy(new ProducersSection.ProducerEntry("rustc", "1.70.0"))
              .addSdk(new ProducersSection.ProducerEntry("wasmtime", "10.0"))
              .build();
      final List<ProducersSection.ProducerEntry> allEntries = section.getAllEntries();
      assertEquals(3, allEntries.size(), "Should have 3 total entries from all categories");
    }
  }

  @Nested
  @DisplayName("findProducerByName")
  final class FindProducerByNameTests {

    @Test
    @DisplayName("should find existing producer by name")
    void shouldFindExistingProducer() {
      final ProducersSection section =
          ProducersSection.builder()
              .addLanguage(new ProducersSection.ProducerEntry("Rust", "1.70.0"))
              .addProcessedBy(new ProducersSection.ProducerEntry("rustc", "1.70.0"))
              .build();
      final Optional<ProducersSection.ProducerEntry> result = section.findProducerByName("rustc");
      assertTrue(result.isPresent(), "Should find producer named 'rustc'");
      assertEquals("rustc", result.get().getName(), "Found producer name should be rustc");
    }

    @Test
    @DisplayName("should return empty for non-existent producer")
    void shouldReturnEmptyForNonExistent() {
      final ProducersSection section = ProducersSection.builder().build();
      final Optional<ProducersSection.ProducerEntry> result =
          section.findProducerByName("nonexistent");
      assertFalse(result.isPresent(), "Should not find non-existent producer");
    }

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      final ProducersSection section = ProducersSection.builder().build();
      assertThrows(
          IllegalArgumentException.class,
          () -> section.findProducerByName(null),
          "Expected IllegalArgumentException for null name");
    }
  }

  @Nested
  @DisplayName("findProducersByType")
  final class FindProducersByTypeTests {

    @Test
    @DisplayName("should find producers by LANGUAGE type")
    void shouldFindByLanguageType() {
      final ProducersSection section =
          ProducersSection.builder()
              .addLanguage(new ProducersSection.ProducerEntry("Rust", "1.70.0"))
              .addLanguage(new ProducersSection.ProducerEntry("C", "17"))
              .addProcessedBy(new ProducersSection.ProducerEntry("rustc", "1.70.0"))
              .build();
      final List<ProducersSection.ProducerEntry> languages =
          section.findProducersByType(ProducersSection.ProducerType.LANGUAGE);
      assertEquals(2, languages.size(), "Should find 2 language producers");
    }

    @Test
    @DisplayName("should find producers by PROCESSED_BY type")
    void shouldFindByProcessedByType() {
      final ProducersSection section =
          ProducersSection.builder()
              .addProcessedBy(new ProducersSection.ProducerEntry("clang", "15.0"))
              .build();
      final List<ProducersSection.ProducerEntry> tools =
          section.findProducersByType(ProducersSection.ProducerType.PROCESSED_BY);
      assertEquals(1, tools.size(), "Should find 1 processedBy producer");
    }

    @Test
    @DisplayName("should find producers by SDK type")
    void shouldFindBySdkType() {
      final ProducersSection section =
          ProducersSection.builder()
              .addSdk(new ProducersSection.ProducerEntry("emscripten", "3.1.0"))
              .build();
      final List<ProducersSection.ProducerEntry> sdks =
          section.findProducersByType(ProducersSection.ProducerType.SDK);
      assertEquals(1, sdks.size(), "Should find 1 SDK producer");
    }

    @Test
    @DisplayName("should reject null type")
    void shouldRejectNullType() {
      final ProducersSection section = ProducersSection.builder().build();
      assertThrows(
          IllegalArgumentException.class,
          () -> section.findProducersByType(null),
          "Expected IllegalArgumentException for null type");
    }
  }

  @Nested
  @DisplayName("ProducerEntry")
  final class ProducerEntryTests {

    @Test
    @DisplayName("should store name and version")
    void shouldStoreNameAndVersion() {
      final ProducersSection.ProducerEntry entry =
          new ProducersSection.ProducerEntry("Rust", "1.70.0");
      assertEquals("Rust", entry.getName(), "Name should be Rust");
      assertEquals("1.70.0", entry.getVersion(), "Version should be 1.70.0");
    }

    @Test
    @DisplayName("should store metadata")
    void shouldStoreMetadata() {
      final Map<String, String> metadata = Map.of("target", "wasm32-unknown-unknown");
      final ProducersSection.ProducerEntry entry =
          new ProducersSection.ProducerEntry("Rust", "1.70.0", metadata);
      assertTrue(entry.hasMetadata(), "Entry should have metadata");
      assertEquals(1, entry.getMetadata().size(), "Metadata should have 1 entry");
    }

    @Test
    @DisplayName("should handle null metadata gracefully")
    void shouldHandleNullMetadata() {
      final ProducersSection.ProducerEntry entry =
          new ProducersSection.ProducerEntry("Rust", "1.70.0", null);
      assertFalse(entry.hasMetadata(), "Entry with null metadata should have no metadata");
      assertTrue(entry.getMetadata().isEmpty(), "Null metadata should default to empty map");
    }

    @Test
    @DisplayName("should detect version presence")
    void shouldDetectVersionPresence() {
      final ProducersSection.ProducerEntry withVersion =
          new ProducersSection.ProducerEntry("Rust", "1.70.0");
      final ProducersSection.ProducerEntry withoutVersion =
          new ProducersSection.ProducerEntry("Rust", null);
      final ProducersSection.ProducerEntry withEmptyVersion =
          new ProducersSection.ProducerEntry("Rust", "  ");
      assertTrue(withVersion.hasVersion(), "Entry with version should have version");
      assertFalse(withoutVersion.hasVersion(), "Entry with null version should not have version");
      assertFalse(
          withEmptyVersion.hasVersion(), "Entry with blank version should not have version");
    }

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProducersSection.ProducerEntry(null, "1.0"),
          "Expected IllegalArgumentException for null name");
    }

    @Test
    @DisplayName("should reject empty name")
    void shouldRejectEmptyName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ProducersSection.ProducerEntry("  ", "1.0"),
          "Expected IllegalArgumentException for blank name");
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEquals() {
      final ProducersSection.ProducerEntry entry1 =
          new ProducersSection.ProducerEntry("Rust", "1.70.0");
      final ProducersSection.ProducerEntry entry2 =
          new ProducersSection.ProducerEntry("Rust", "1.70.0");
      final ProducersSection.ProducerEntry entry3 =
          new ProducersSection.ProducerEntry("C", "17");
      assertEquals(entry1, entry2, "Entries with same name and version should be equal");
      assertFalse(entry1.equals(entry3), "Entries with different names should not be equal");
    }

    @Test
    @DisplayName("should implement hashCode consistently with equals")
    void shouldImplementHashCode() {
      final ProducersSection.ProducerEntry entry1 =
          new ProducersSection.ProducerEntry("Rust", "1.70.0");
      final ProducersSection.ProducerEntry entry2 =
          new ProducersSection.ProducerEntry("Rust", "1.70.0");
      assertEquals(
          entry1.hashCode(), entry2.hashCode(), "Equal entries should have same hashCode");
    }

    @Test
    @DisplayName("should produce readable toString")
    void shouldProduceReadableToString() {
      final ProducersSection.ProducerEntry entry =
          new ProducersSection.ProducerEntry("Rust", "1.70.0");
      final String str = entry.toString();
      assertTrue(str.contains("Rust"), "toString should contain name");
      assertTrue(str.contains("1.70.0"), "toString should contain version");
    }
  }

  @Nested
  @DisplayName("ProducerType enum")
  final class ProducerTypeTests {

    @Test
    @DisplayName("should have three types")
    void shouldHaveThreeTypes() {
      assertEquals(
          3,
          ProducersSection.ProducerType.values().length,
          "Should have 3 producer types: LANGUAGE, PROCESSED_BY, SDK");
    }
  }

  @Nested
  @DisplayName("toString")
  final class ToStringTests {

    @Test
    @DisplayName("should return summary as toString")
    void shouldReturnSummaryAsToString() {
      final ProducersSection section = ProducersSection.builder().build();
      final String str = section.toString();
      assertEquals(section.getSummary(), str, "toString should return same value as getSummary");
    }
  }
}
