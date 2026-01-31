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
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link TargetFeaturesSection} target features custom section data. */
@DisplayName("TargetFeaturesSection")
final class TargetFeaturesSectionTest {

  @Nested
  @DisplayName("builder defaults")
  final class BuilderDefaultTests {

    @Test
    @DisplayName("should build empty target features section")
    void shouldBuildEmptySection() {
      final TargetFeaturesSection section = TargetFeaturesSection.builder().build();
      assertNotNull(section, "Built section should not be null");
      assertTrue(section.getFeatures().isEmpty(), "Default features should be empty");
      assertTrue(section.isEmpty(), "Empty section should return true for isEmpty");
    }
  }

  @Nested
  @DisplayName("builder setFeatures")
  final class BuilderSetFeaturesTests {

    @Test
    @DisplayName("should set features list")
    void shouldSetFeaturesList() {
      final List<TargetFeaturesSection.FeatureEntry> features =
          List.of(
              TargetFeaturesSection.FeatureEntry.required("mutable-globals"),
              TargetFeaturesSection.FeatureEntry.used("sign-ext"));
      final TargetFeaturesSection section =
          TargetFeaturesSection.builder().setFeatures(features).build();
      assertEquals(2, section.getFeatures().size(), "Should have 2 feature entries");
    }

    @Test
    @DisplayName("should handle null features gracefully")
    void shouldHandleNullFeatures() {
      final TargetFeaturesSection section =
          TargetFeaturesSection.builder().setFeatures(null).build();
      assertTrue(section.getFeatures().isEmpty(), "Null features should default to empty");
    }
  }

  @Nested
  @DisplayName("builder addFeature methods")
  final class BuilderAddFeatureTests {

    @Test
    @DisplayName("should add feature entry directly")
    void shouldAddFeatureEntry() {
      final TargetFeaturesSection section =
          TargetFeaturesSection.builder()
              .addFeature(
                  new TargetFeaturesSection.FeatureEntry(
                      "bulk-memory", TargetFeaturesSection.FeatureStatus.REQUIRED))
              .build();
      assertEquals(1, section.getFeatures().size(), "Should have 1 feature entry");
    }

    @Test
    @DisplayName("should add required feature by name")
    void shouldAddRequiredFeature() {
      final TargetFeaturesSection section =
          TargetFeaturesSection.builder().addRequiredFeature("mutable-globals").build();
      assertEquals(1, section.getRequiredFeatures().size(), "Should have 1 required feature");
      assertTrue(
          section.isFeatureRequired("mutable-globals"),
          "mutable-globals should be required");
    }

    @Test
    @DisplayName("should add used feature by name")
    void shouldAddUsedFeature() {
      final TargetFeaturesSection section =
          TargetFeaturesSection.builder().addUsedFeature("sign-ext").build();
      assertEquals(1, section.getUsedFeatures().size(), "Should have 1 used feature");
      assertTrue(section.isFeatureUsed("sign-ext"), "sign-ext should be used");
    }

    @Test
    @DisplayName("should add disabled feature by name")
    void shouldAddDisabledFeature() {
      final TargetFeaturesSection section =
          TargetFeaturesSection.builder().addDisabledFeature("threads").build();
      assertEquals(1, section.getDisabledFeatures().size(), "Should have 1 disabled feature");
      assertTrue(section.isFeatureDisabled("threads"), "threads should be disabled");
    }

    @Test
    @DisplayName("should reject null feature entry")
    void shouldRejectNullFeatureEntry() {
      final TargetFeaturesSection.Builder builder = TargetFeaturesSection.builder();
      assertThrows(
          IllegalArgumentException.class,
          () -> builder.addFeature(null),
          "Expected IllegalArgumentException for null feature entry");
    }
  }

  @Nested
  @DisplayName("feature queries")
  final class FeatureQueryTests {

    private TargetFeaturesSection createTestSection() {
      return TargetFeaturesSection.builder()
          .addRequiredFeature("mutable-globals")
          .addRequiredFeature("bulk-memory")
          .addUsedFeature("sign-ext")
          .addUsedFeature("nontrapping-fptoint")
          .addDisabledFeature("threads")
          .build();
    }

    @Test
    @DisplayName("should return required features only")
    void shouldReturnRequiredFeaturesOnly() {
      final TargetFeaturesSection section = createTestSection();
      final List<TargetFeaturesSection.FeatureEntry> required = section.getRequiredFeatures();
      assertEquals(2, required.size(), "Should have 2 required features");
      assertTrue(
          required.stream().allMatch(TargetFeaturesSection.FeatureEntry::isRequired),
          "All returned features should be required");
    }

    @Test
    @DisplayName("should return optional (non-required) features")
    void shouldReturnOptionalFeatures() {
      final TargetFeaturesSection section = createTestSection();
      final List<TargetFeaturesSection.FeatureEntry> optional = section.getOptionalFeatures();
      assertEquals(3, optional.size(), "Should have 3 optional features (used + disabled)");
    }

    @Test
    @DisplayName("should return used features only")
    void shouldReturnUsedFeaturesOnly() {
      final TargetFeaturesSection section = createTestSection();
      final List<TargetFeaturesSection.FeatureEntry> used = section.getUsedFeatures();
      assertEquals(2, used.size(), "Should have 2 used features");
    }

    @Test
    @DisplayName("should return disabled features only")
    void shouldReturnDisabledFeaturesOnly() {
      final TargetFeaturesSection section = createTestSection();
      final List<TargetFeaturesSection.FeatureEntry> disabled = section.getDisabledFeatures();
      assertEquals(1, disabled.size(), "Should have 1 disabled feature");
    }

    @Test
    @DisplayName("should return all feature names")
    void shouldReturnAllFeatureNames() {
      final TargetFeaturesSection section = createTestSection();
      final Set<String> names = section.getFeatureNames();
      assertEquals(5, names.size(), "Should have 5 feature names");
      assertTrue(names.contains("mutable-globals"), "Should contain mutable-globals");
      assertTrue(names.contains("threads"), "Should contain threads");
    }

    @Test
    @DisplayName("should detect feature presence")
    void shouldDetectFeaturePresence() {
      final TargetFeaturesSection section = createTestSection();
      assertTrue(section.hasFeature("mutable-globals"), "Should have mutable-globals");
      assertFalse(section.hasFeature("nonexistent"), "Should not have nonexistent feature");
    }

    @Test
    @DisplayName("should get feature entry by name")
    void shouldGetFeatureByName() {
      final TargetFeaturesSection section = createTestSection();
      final Optional<TargetFeaturesSection.FeatureEntry> feature =
          section.getFeature("sign-ext");
      assertTrue(feature.isPresent(), "Should find sign-ext feature");
      assertEquals(
          TargetFeaturesSection.FeatureStatus.USED,
          feature.get().getStatus(),
          "sign-ext should have USED status");
    }

    @Test
    @DisplayName("should return empty for non-existent feature")
    void shouldReturnEmptyForNonExistent() {
      final TargetFeaturesSection section = createTestSection();
      final Optional<TargetFeaturesSection.FeatureEntry> result =
          section.getFeature("nonexistent");
      assertFalse(result.isPresent(), "Should not find non-existent feature");
    }
  }

  @Nested
  @DisplayName("null validation on queries")
  final class NullValidationTests {

    @Test
    @DisplayName("should reject null in hasFeature")
    void shouldRejectNullInHasFeature() {
      final TargetFeaturesSection section = TargetFeaturesSection.builder().build();
      assertThrows(
          IllegalArgumentException.class,
          () -> section.hasFeature(null),
          "Expected IllegalArgumentException for null featureName in hasFeature");
    }

    @Test
    @DisplayName("should reject null in isFeatureRequired")
    void shouldRejectNullInIsFeatureRequired() {
      final TargetFeaturesSection section = TargetFeaturesSection.builder().build();
      assertThrows(
          IllegalArgumentException.class,
          () -> section.isFeatureRequired(null),
          "Expected IllegalArgumentException for null featureName in isFeatureRequired");
    }

    @Test
    @DisplayName("should reject null in isFeatureUsed")
    void shouldRejectNullInIsFeatureUsed() {
      final TargetFeaturesSection section = TargetFeaturesSection.builder().build();
      assertThrows(
          IllegalArgumentException.class,
          () -> section.isFeatureUsed(null),
          "Expected IllegalArgumentException for null featureName in isFeatureUsed");
    }

    @Test
    @DisplayName("should reject null in isFeatureDisabled")
    void shouldRejectNullInIsFeatureDisabled() {
      final TargetFeaturesSection section = TargetFeaturesSection.builder().build();
      assertThrows(
          IllegalArgumentException.class,
          () -> section.isFeatureDisabled(null),
          "Expected IllegalArgumentException for null featureName in isFeatureDisabled");
    }

    @Test
    @DisplayName("should reject null in getFeature")
    void shouldRejectNullInGetFeature() {
      final TargetFeaturesSection section = TargetFeaturesSection.builder().build();
      assertThrows(
          IllegalArgumentException.class,
          () -> section.getFeature(null),
          "Expected IllegalArgumentException for null featureName in getFeature");
    }
  }

  @Nested
  @DisplayName("getSummary")
  final class GetSummaryTests {

    @Test
    @DisplayName("should return accurate summary")
    void shouldReturnAccurateSummary() {
      final TargetFeaturesSection section =
          TargetFeaturesSection.builder()
              .addRequiredFeature("mutable-globals")
              .addUsedFeature("sign-ext")
              .addDisabledFeature("threads")
              .build();
      final String summary = section.getSummary();
      assertTrue(summary.contains("total=3"), "Summary should contain total=3");
      assertTrue(summary.contains("required=1"), "Summary should contain required=1");
      assertTrue(summary.contains("used=1"), "Summary should contain used=1");
      assertTrue(summary.contains("disabled=1"), "Summary should contain disabled=1");
    }
  }

  @Nested
  @DisplayName("validateCompatibility")
  final class ValidateCompatibilityTests {

    @Test
    @DisplayName("should be compatible when all required features supported")
    void shouldBeCompatibleWhenAllRequired() {
      final TargetFeaturesSection section =
          TargetFeaturesSection.builder()
              .addRequiredFeature("mutable-globals")
              .addRequiredFeature("bulk-memory")
              .build();
      final Set<String> supported = Set.of("mutable-globals", "bulk-memory", "sign-ext");
      final TargetFeaturesSection.FeatureCompatibilityResult result =
          section.validateCompatibility(supported);
      assertTrue(result.isCompatible(), "Should be compatible when all required features present");
      assertTrue(
          result.getMissingRequiredFeatures().isEmpty(),
          "Should have no missing required features");
    }

    @Test
    @DisplayName("should not be compatible when required feature missing")
    void shouldNotBeCompatibleWhenMissing() {
      final TargetFeaturesSection section =
          TargetFeaturesSection.builder()
              .addRequiredFeature("mutable-globals")
              .addRequiredFeature("bulk-memory")
              .build();
      final Set<String> supported = Set.of("mutable-globals");
      final TargetFeaturesSection.FeatureCompatibilityResult result =
          section.validateCompatibility(supported);
      assertFalse(result.isCompatible(), "Should not be compatible with missing required feature");
      assertEquals(
          1,
          result.getMissingRequiredFeatures().size(),
          "Should have 1 missing required feature");
      assertTrue(
          result.getMissingRequiredFeatures().contains("bulk-memory"),
          "bulk-memory should be in missing required");
    }

    @Test
    @DisplayName("should warn about unsupported used features")
    void shouldWarnAboutUnsupportedUsed() {
      final TargetFeaturesSection section =
          TargetFeaturesSection.builder()
              .addUsedFeature("sign-ext")
              .addUsedFeature("nontrapping-fptoint")
              .build();
      final Set<String> supported = Set.of("sign-ext");
      final TargetFeaturesSection.FeatureCompatibilityResult result =
          section.validateCompatibility(supported);
      assertTrue(result.isCompatible(), "Should be compatible (only used, not required)");
      assertTrue(result.hasWarnings(), "Should have warnings for unsupported used features");
      assertEquals(
          1,
          result.getUnsupportedUsedFeatures().size(),
          "Should have 1 unsupported used feature");
    }

    @Test
    @DisplayName("should reject null supported features")
    void shouldRejectNullSupportedFeatures() {
      final TargetFeaturesSection section = TargetFeaturesSection.builder().build();
      assertThrows(
          IllegalArgumentException.class,
          () -> section.validateCompatibility(null),
          "Expected IllegalArgumentException for null supported features");
    }
  }

  @Nested
  @DisplayName("FeatureEntry")
  final class FeatureEntryTests {

    @Test
    @DisplayName("should create required entry via factory method")
    void shouldCreateRequiredEntry() {
      final TargetFeaturesSection.FeatureEntry entry =
          TargetFeaturesSection.FeatureEntry.required("mutable-globals");
      assertEquals("mutable-globals", entry.getName(), "Name should be mutable-globals");
      assertEquals(
          TargetFeaturesSection.FeatureStatus.REQUIRED,
          entry.getStatus(),
          "Status should be REQUIRED");
      assertTrue(entry.isRequired(), "isRequired should be true");
      assertFalse(entry.isUsed(), "isUsed should be false");
      assertFalse(entry.isDisabled(), "isDisabled should be false");
    }

    @Test
    @DisplayName("should create used entry via factory method")
    void shouldCreateUsedEntry() {
      final TargetFeaturesSection.FeatureEntry entry =
          TargetFeaturesSection.FeatureEntry.used("sign-ext");
      assertTrue(entry.isUsed(), "isUsed should be true");
      assertFalse(entry.isRequired(), "isRequired should be false");
    }

    @Test
    @DisplayName("should create disabled entry via factory method")
    void shouldCreateDisabledEntry() {
      final TargetFeaturesSection.FeatureEntry entry =
          TargetFeaturesSection.FeatureEntry.disabled("threads");
      assertTrue(entry.isDisabled(), "isDisabled should be true");
      assertFalse(entry.isRequired(), "isRequired should be false");
    }

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TargetFeaturesSection.FeatureEntry(
              null, TargetFeaturesSection.FeatureStatus.REQUIRED),
          "Expected IllegalArgumentException for null name");
    }

    @Test
    @DisplayName("should reject empty name")
    void shouldRejectEmptyName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TargetFeaturesSection.FeatureEntry(
              "  ", TargetFeaturesSection.FeatureStatus.REQUIRED),
          "Expected IllegalArgumentException for blank name");
    }

    @Test
    @DisplayName("should reject null status")
    void shouldRejectNullStatus() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new TargetFeaturesSection.FeatureEntry("feature", null),
          "Expected IllegalArgumentException for null status");
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEquals() {
      final TargetFeaturesSection.FeatureEntry entry1 =
          TargetFeaturesSection.FeatureEntry.required("mutable-globals");
      final TargetFeaturesSection.FeatureEntry entry2 =
          TargetFeaturesSection.FeatureEntry.required("mutable-globals");
      final TargetFeaturesSection.FeatureEntry entry3 =
          TargetFeaturesSection.FeatureEntry.used("mutable-globals");
      assertEquals(entry1, entry2, "Entries with same name and status should be equal");
      assertFalse(
          entry1.equals(entry3),
          "Entries with different status should not be equal");
    }

    @Test
    @DisplayName("should implement hashCode consistently with equals")
    void shouldImplementHashCode() {
      final TargetFeaturesSection.FeatureEntry entry1 =
          TargetFeaturesSection.FeatureEntry.required("mutable-globals");
      final TargetFeaturesSection.FeatureEntry entry2 =
          TargetFeaturesSection.FeatureEntry.required("mutable-globals");
      assertEquals(
          entry1.hashCode(), entry2.hashCode(), "Equal entries should have same hashCode");
    }

    @Test
    @DisplayName("should produce readable toString")
    void shouldProduceReadableToString() {
      final TargetFeaturesSection.FeatureEntry entry =
          TargetFeaturesSection.FeatureEntry.required("mutable-globals");
      final String str = entry.toString();
      assertTrue(str.contains("mutable-globals"), "toString should contain feature name");
      assertTrue(str.contains("REQUIRED"), "toString should contain status");
    }
  }

  @Nested
  @DisplayName("FeatureStatus enum")
  final class FeatureStatusTests {

    @Test
    @DisplayName("should have three statuses")
    void shouldHaveThreeStatuses() {
      assertEquals(
          3,
          TargetFeaturesSection.FeatureStatus.values().length,
          "Should have 3 feature statuses: REQUIRED, USED, DISABLED");
    }
  }

  @Nested
  @DisplayName("FeatureCompatibilityResult")
  final class FeatureCompatibilityResultTests {

    @Test
    @DisplayName("should handle empty lists")
    void shouldHandleEmptyLists() {
      final TargetFeaturesSection.FeatureCompatibilityResult result =
          new TargetFeaturesSection.FeatureCompatibilityResult(
              List.of(), List.of());
      assertTrue(result.isCompatible(), "Empty missing should be compatible");
      assertFalse(result.hasWarnings(), "Empty unsupported should have no warnings");
    }

    @Test
    @DisplayName("should handle null lists gracefully")
    void shouldHandleNullLists() {
      final TargetFeaturesSection.FeatureCompatibilityResult result =
          new TargetFeaturesSection.FeatureCompatibilityResult(null, null);
      assertTrue(result.isCompatible(), "Null missing should default to compatible");
      assertFalse(result.hasWarnings(), "Null unsupported should default to no warnings");
    }

    @Test
    @DisplayName("should produce readable toString")
    void shouldProduceReadableToString() {
      final TargetFeaturesSection.FeatureCompatibilityResult result =
          new TargetFeaturesSection.FeatureCompatibilityResult(
              List.of("missing-feature"), List.of("unsupported-feature"));
      final String str = result.toString();
      assertTrue(str.contains("compatible=false"), "toString should show incompatible");
      assertTrue(str.contains("missingRequired=1"), "toString should show missing count");
    }
  }

  @Nested
  @DisplayName("toString")
  final class ToStringTests {

    @Test
    @DisplayName("should return summary as toString")
    void shouldReturnSummaryAsToString() {
      final TargetFeaturesSection section = TargetFeaturesSection.builder().build();
      final String str = section.toString();
      assertEquals(section.getSummary(), str, "toString should return same value as getSummary");
    }
  }
}
