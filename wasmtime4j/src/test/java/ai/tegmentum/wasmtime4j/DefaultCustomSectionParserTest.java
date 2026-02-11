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

import ai.tegmentum.wasmtime4j.func.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ai.tegmentum.wasmtime4j.metadata.CustomSection;
import ai.tegmentum.wasmtime4j.metadata.CustomSectionParser;
import ai.tegmentum.wasmtime4j.metadata.CustomSectionType;
import ai.tegmentum.wasmtime4j.metadata.CustomSectionValidationResult;
import ai.tegmentum.wasmtime4j.metadata.DefaultCustomSectionParser;
import ai.tegmentum.wasmtime4j.metadata.NameSection;
import ai.tegmentum.wasmtime4j.metadata.ProducersSection;
import ai.tegmentum.wasmtime4j.metadata.TargetFeaturesSection;

/** Tests for {@link DefaultCustomSectionParser} custom section parsing. */
@DisplayName("DefaultCustomSectionParser")
final class DefaultCustomSectionParserTest {

  private final DefaultCustomSectionParser parser = new DefaultCustomSectionParser();

  @Nested
  @DisplayName("supports")
  final class SupportsTests {

    @Test
    @DisplayName("should support name section")
    void shouldSupportNameSection() {
      assertTrue(parser.supports("name"), "Parser should support 'name' section");
    }

    @Test
    @DisplayName("should support producers section")
    void shouldSupportProducersSection() {
      assertTrue(parser.supports("producers"), "Parser should support 'producers' section");
    }

    @Test
    @DisplayName("should support target_features section")
    void shouldSupportTargetFeaturesSection() {
      assertTrue(
          parser.supports("target_features"),
          "Parser should support 'target_features' section");
    }

    @Test
    @DisplayName("should support unknown section names")
    void shouldSupportUnknownSectionNames() {
      assertTrue(
          parser.supports("my_custom_section"),
          "Parser should support unknown section names (maps to UNKNOWN type)");
    }

    @Test
    @DisplayName("should reject null section name")
    void shouldRejectNullSectionName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.supports(null),
          "Expected IllegalArgumentException for null section name");
    }
  }

  @Nested
  @DisplayName("getSupportedTypes")
  final class GetSupportedTypesTests {

    @Test
    @DisplayName("should return supported types including NAME, PRODUCERS, TARGET_FEATURES, UNKNOWN")
    void shouldReturnSupportedTypes() {
      final Set<CustomSectionType> types = parser.getSupportedTypes();
      assertNotNull(types, "Supported types should not be null");
      assertTrue(types.contains(CustomSectionType.NAME), "Should support NAME type");
      assertTrue(types.contains(CustomSectionType.PRODUCERS), "Should support PRODUCERS type");
      assertTrue(
          types.contains(CustomSectionType.TARGET_FEATURES),
          "Should support TARGET_FEATURES type");
      assertTrue(types.contains(CustomSectionType.UNKNOWN), "Should support UNKNOWN type");
    }
  }

  @Nested
  @DisplayName("parseCustomSection")
  final class ParseCustomSectionTests {

    @Test
    @DisplayName("should parse a custom section with known name")
    void shouldParseKnownSection() {
      final Optional<CustomSection> result = parser.parseCustomSection("name", new byte[0]);
      assertTrue(result.isPresent(), "Should parse a section named 'name'");
      assertEquals("name", result.get().getName(), "Section name should be 'name'");
    }

    @Test
    @DisplayName("should parse a custom section with unknown name")
    void shouldParseUnknownSection() {
      final Optional<CustomSection> result =
          parser.parseCustomSection("my_section", new byte[] {1, 2, 3});
      assertTrue(result.isPresent(), "Should parse an unknown section");
      assertEquals("my_section", result.get().getName(), "Section name should be 'my_section'");
    }

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.parseCustomSection(null, new byte[0]),
          "Expected IllegalArgumentException for null section name");
    }

    @Test
    @DisplayName("should reject null data")
    void shouldRejectNullData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.parseCustomSection("name", null),
          "Expected IllegalArgumentException for null section data");
    }
  }

  @Nested
  @DisplayName("parseNameSection")
  final class ParseNameSectionTests {

    @Test
    @DisplayName("should parse name section with module name subsection")
    void shouldParseModuleNameSubsection() {
      // Binary format: subsection_type=0, size=ULEB128, name_length=ULEB128, name_bytes
      // Module name "test": type=0, size=5, name_len=4, "test"
      final byte[] data = new byte[] {0x00, 0x05, 0x04, 0x74, 0x65, 0x73, 0x74};
      final Optional<NameSection> result = parser.parseNameSection(data);
      assertTrue(result.isPresent(), "Should parse name section with module name");
      assertTrue(
          result.get().getModuleName().isPresent(), "Module name should be present");
      assertEquals("test", result.get().getModuleName().get(), "Module name should be 'test'");
    }

    @Test
    @DisplayName("should parse name section with function names subsection")
    void shouldParseFunctionNamesSubsection() {
      // Binary format: subsection_type=1, size=ULEB128, count=ULEB128,
      //   then pairs of (index=ULEB128, name_len=ULEB128, name_bytes)
      // Function names: {0: "add"}
      // type=1, size=6, count=1, idx=0, name_len=3, "add"
      final byte[] data =
          new byte[] {0x01, 0x06, 0x01, 0x00, 0x03, 0x61, 0x64, 0x64};
      final Optional<NameSection> result = parser.parseNameSection(data);
      assertTrue(result.isPresent(), "Should parse name section with function names");
      final Map<Integer, String> funcNames = result.get().getFunctionNames();
      assertEquals(1, funcNames.size(), "Should have 1 function name");
      assertEquals("add", funcNames.get(0), "Function 0 should be named 'add'");
    }

    @Test
    @DisplayName("should handle empty data")
    void shouldHandleEmptyData() {
      final Optional<NameSection> result = parser.parseNameSection(new byte[0]);
      assertTrue(result.isPresent(), "Should parse empty data into empty name section");
    }

    @Test
    @DisplayName("should handle malformed data gracefully")
    void shouldHandleMalformedData() {
      // Malformed: subsection type with size that exceeds remaining data
      final byte[] malformed = new byte[] {0x01, 0x7F};
      final Optional<NameSection> result = parser.parseNameSection(malformed);
      // Should either parse what it can or return empty - not throw
      assertNotNull(result, "Result should not be null for malformed data");
    }

    @Test
    @DisplayName("should reject null data")
    void shouldRejectNullData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.parseNameSection(null),
          "Expected IllegalArgumentException for null data");
    }
  }

  @Nested
  @DisplayName("parseProducersSection")
  final class ParseProducersSectionTests {

    @Test
    @DisplayName("should parse producers section with language field")
    void shouldParseLanguageField() {
      // Binary format: field_count=ULEB128,
      //   field_name_len=ULEB128, field_name, entry_count=ULEB128,
      //     name_len=ULEB128, name, version_len=ULEB128, version
      // field_count=1, field="language", entries=[("Rust", "1.70")]
      final byte[] data =
          buildProducersData("language", "Rust", "1.70");
      final Optional<ProducersSection> result = parser.parseProducersSection(data);
      assertTrue(result.isPresent(), "Should parse producers section");
      assertEquals(
          1, result.get().getLanguages().size(), "Should have 1 language entry");
      assertEquals(
          "Rust",
          result.get().getLanguages().get(0).getName(),
          "Language name should be Rust");
    }

    @Test
    @DisplayName("should parse producers section with processed-by field")
    void shouldParseProcessedByField() {
      final byte[] data =
          buildProducersData("processed-by", "clang", "15.0");
      final Optional<ProducersSection> result = parser.parseProducersSection(data);
      assertTrue(result.isPresent(), "Should parse producers section");
      assertEquals(
          1,
          result.get().getProcessedBy().size(),
          "Should have 1 processed-by entry");
    }

    @Test
    @DisplayName("should parse producers section with sdk field")
    void shouldParseSdkField() {
      final byte[] data = buildProducersData("sdk", "emscripten", "3.1");
      final Optional<ProducersSection> result = parser.parseProducersSection(data);
      assertTrue(result.isPresent(), "Should parse producers section");
      assertEquals(1, result.get().getSdk().size(), "Should have 1 SDK entry");
    }

    @Test
    @DisplayName("should handle empty data")
    void shouldHandleEmptyData() {
      // field_count=0
      final byte[] data = new byte[] {0x00};
      final Optional<ProducersSection> result = parser.parseProducersSection(data);
      assertTrue(result.isPresent(), "Should parse empty producers section");
      assertTrue(result.get().isEmpty(), "Parsed section should be empty");
    }

    @Test
    @DisplayName("should reject null data")
    void shouldRejectNullData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.parseProducersSection(null),
          "Expected IllegalArgumentException for null data");
    }

    /**
     * Builds binary data for a producers section with one field and one entry.
     * Format: field_count(1), field_name, entry_count(1), entry_name, entry_version
     */
    private byte[] buildProducersData(
        final String fieldName, final String entryName, final String entryVersion) {
      final byte[] fieldNameBytes = fieldName.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final byte[] entryNameBytes = entryName.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final byte[] versionBytes = entryVersion.getBytes(java.nio.charset.StandardCharsets.UTF_8);

      // field_count=1 + field_name_len + field_name + entry_count=1
      // + entry_name_len + entry_name + version_len + version
      final int totalSize =
          1
              + 1 + fieldNameBytes.length
              + 1
              + 1 + entryNameBytes.length
              + 1 + versionBytes.length;
      final byte[] data = new byte[totalSize];
      int offset = 0;

      data[offset++] = 0x01; // field_count = 1
      data[offset++] = (byte) fieldNameBytes.length;
      System.arraycopy(fieldNameBytes, 0, data, offset, fieldNameBytes.length);
      offset += fieldNameBytes.length;
      data[offset++] = 0x01; // entry_count = 1
      data[offset++] = (byte) entryNameBytes.length;
      System.arraycopy(entryNameBytes, 0, data, offset, entryNameBytes.length);
      offset += entryNameBytes.length;
      data[offset++] = (byte) versionBytes.length;
      System.arraycopy(versionBytes, 0, data, offset, versionBytes.length);

      return data;
    }
  }

  @Nested
  @DisplayName("parseTargetFeaturesSection")
  final class ParseTargetFeaturesSectionTests {

    @Test
    @DisplayName("should parse target features with required prefix")
    void shouldParseRequiredFeature() {
      // Binary format: feature_count=ULEB128, prefix_byte, name_len=ULEB128, name_bytes
      // '+' = 0x2B for REQUIRED
      final byte[] featureName = "mutable-globals".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final byte[] data = new byte[1 + 1 + 1 + featureName.length];
      int offset = 0;
      data[offset++] = 0x01; // feature_count = 1
      data[offset++] = 0x2B; // prefix '+' = REQUIRED
      data[offset++] = (byte) featureName.length;
      System.arraycopy(featureName, 0, data, offset, featureName.length);

      final Optional<TargetFeaturesSection> result = parser.parseTargetFeaturesSection(data);
      assertTrue(result.isPresent(), "Should parse target features section");
      assertEquals(1, result.get().getFeatures().size(), "Should have 1 feature");
      assertTrue(
          result.get().getFeatures().get(0).isRequired(),
          "Feature should be REQUIRED");
      assertEquals(
          "mutable-globals",
          result.get().getFeatures().get(0).getName(),
          "Feature name should be mutable-globals");
    }

    @Test
    @DisplayName("should parse target features with used prefix")
    void shouldParseUsedFeature() {
      // '=' = 0x3D for USED
      final byte[] featureName = "sign-ext".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final byte[] data = new byte[1 + 1 + 1 + featureName.length];
      int offset = 0;
      data[offset++] = 0x01;
      data[offset++] = 0x3D; // prefix '=' = USED
      data[offset++] = (byte) featureName.length;
      System.arraycopy(featureName, 0, data, offset, featureName.length);

      final Optional<TargetFeaturesSection> result = parser.parseTargetFeaturesSection(data);
      assertTrue(result.isPresent(), "Should parse target features section");
      assertTrue(
          result.get().getFeatures().get(0).isUsed(), "Feature should be USED");
    }

    @Test
    @DisplayName("should parse target features with disabled prefix")
    void shouldParseDisabledFeature() {
      // '-' = 0x2D for DISABLED
      final byte[] featureName = "threads".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final byte[] data = new byte[1 + 1 + 1 + featureName.length];
      int offset = 0;
      data[offset++] = 0x01;
      data[offset++] = 0x2D; // prefix '-' = DISABLED
      data[offset++] = (byte) featureName.length;
      System.arraycopy(featureName, 0, data, offset, featureName.length);

      final Optional<TargetFeaturesSection> result = parser.parseTargetFeaturesSection(data);
      assertTrue(result.isPresent(), "Should parse target features section");
      assertTrue(
          result.get().getFeatures().get(0).isDisabled(), "Feature should be DISABLED");
    }

    @Test
    @DisplayName("should handle empty features count")
    void shouldHandleEmptyFeaturesCount() {
      final byte[] data = new byte[] {0x00}; // feature_count = 0
      final Optional<TargetFeaturesSection> result = parser.parseTargetFeaturesSection(data);
      assertTrue(result.isPresent(), "Should parse empty target features section");
      assertTrue(result.get().isEmpty(), "Parsed section should be empty");
    }

    @Test
    @DisplayName("should reject null data")
    void shouldRejectNullData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.parseTargetFeaturesSection(null),
          "Expected IllegalArgumentException for null data");
    }
  }

  @Nested
  @DisplayName("validateSection")
  final class ValidateSectionTests {

    @Test
    @DisplayName("should validate empty section with warning")
    void shouldValidateEmptySectionWithWarning() {
      final CustomSectionValidationResult result =
          parser.validateSection("name", new byte[0]);
      assertTrue(result.hasIssues(), "Empty section should produce issues");
    }

    @Test
    @DisplayName("should validate valid name section data")
    void shouldValidateValidNameSection() {
      // Valid module name subsection
      final byte[] data = new byte[] {0x00, 0x05, 0x04, 0x74, 0x65, 0x73, 0x74};
      final CustomSectionValidationResult result = parser.validateSection("name", data);
      assertTrue(result.isValid(), "Valid name section should pass validation");
    }

    @Test
    @DisplayName("should reject null name in validate")
    void shouldRejectNullNameInValidate() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.validateSection(null, new byte[0]),
          "Expected IllegalArgumentException for null name");
    }

    @Test
    @DisplayName("should reject null data in validate")
    void shouldRejectNullDataInValidate() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.validateSection("name", null),
          "Expected IllegalArgumentException for null data");
    }
  }

  @Nested
  @DisplayName("createCustomSection")
  final class CreateCustomSectionTests {

    @Test
    @DisplayName("should reject null name")
    void shouldRejectNullName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.createCustomSection(null, CustomSectionType.NAME, "data"),
          "Expected IllegalArgumentException for null name");
    }

    @Test
    @DisplayName("should reject null type")
    void shouldRejectNullType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.createCustomSection("name", null, "data"),
          "Expected IllegalArgumentException for null type");
    }

    @Test
    @DisplayName("should reject null structured data")
    void shouldRejectNullStructuredData() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.createCustomSection("name", CustomSectionType.NAME, null),
          "Expected IllegalArgumentException for null structured data");
    }

    @Test
    @DisplayName("should return empty for type mismatch")
    void shouldReturnEmptyForTypeMismatch() {
      // Passing a String when NAME type expects NameSection
      final Optional<CustomSection> result =
          parser.createCustomSection("name", CustomSectionType.NAME, "not a NameSection");
      assertFalse(result.isPresent(), "Should return empty for type mismatch");
    }

    @Test
    @DisplayName("should create name section from NameSection object")
    void shouldCreateNameSectionFromObject() {
      final NameSection nameSection =
          NameSection.builder().setModuleName("myModule").build();
      final Optional<CustomSection> result =
          parser.createCustomSection("name", CustomSectionType.NAME, nameSection);
      assertTrue(result.isPresent(), "Should create custom section from NameSection");
      assertEquals("name", result.get().getName(), "Section name should be 'name'");
    }

    @Test
    @DisplayName("should create producers section from ProducersSection object")
    void shouldCreateProducersSectionFromObject() {
      final ProducersSection producersSection =
          ProducersSection.builder()
              .addLanguage(new ProducersSection.ProducerEntry("Rust", "1.70.0"))
              .build();
      final Optional<CustomSection> result =
          parser.createCustomSection(
              "producers", CustomSectionType.PRODUCERS, producersSection);
      assertTrue(result.isPresent(), "Should create custom section from ProducersSection");
    }

    @Test
    @DisplayName("should create target features section from TargetFeaturesSection object")
    void shouldCreateTargetFeaturesSectionFromObject() {
      final TargetFeaturesSection targetSection =
          TargetFeaturesSection.builder()
              .addRequiredFeature("mutable-globals")
              .build();
      final Optional<CustomSection> result =
          parser.createCustomSection(
              "target_features", CustomSectionType.TARGET_FEATURES, targetSection);
      assertTrue(result.isPresent(), "Should create custom section from TargetFeaturesSection");
    }
  }

  @Nested
  @DisplayName("serialize null validation")
  final class SerializeNullValidationTests {

    @Test
    @DisplayName("should reject null in serializeNameSection")
    void shouldRejectNullNameSection() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.serializeNameSection(null),
          "Expected IllegalArgumentException for null name section");
    }

    @Test
    @DisplayName("should reject null in serializeProducersSection")
    void shouldRejectNullProducersSection() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.serializeProducersSection(null),
          "Expected IllegalArgumentException for null producers section");
    }

    @Test
    @DisplayName("should reject null in serializeTargetFeaturesSection")
    void shouldRejectNullTargetFeaturesSection() {
      assertThrows(
          IllegalArgumentException.class,
          () -> parser.serializeTargetFeaturesSection(null),
          "Expected IllegalArgumentException for null target features section");
    }
  }

  @Nested
  @DisplayName("roundtrip serialization")
  final class RoundtripSerializationTests {

    @Test
    @DisplayName("should roundtrip name section with module name")
    void shouldRoundtripNameSection() {
      final NameSection original =
          NameSection.builder().setModuleName("myModule").build();
      final Optional<byte[]> serialized = parser.serializeNameSection(original);
      assertTrue(serialized.isPresent(), "Should serialize name section");

      final Optional<NameSection> parsed = parser.parseNameSection(serialized.get());
      assertTrue(parsed.isPresent(), "Should parse serialized name section");
      assertTrue(parsed.get().getModuleName().isPresent(), "Module name should be present");
      assertEquals(
          "myModule",
          parsed.get().getModuleName().get(),
          "Roundtripped module name should be 'myModule'");
    }

    @Test
    @DisplayName("should roundtrip name section with function names")
    void shouldRoundtripFunctionNames() {
      final NameSection original =
          NameSection.builder()
              .setFunctionNames(Map.of(0, "add", 1, "sub"))
              .build();
      final Optional<byte[]> serialized = parser.serializeNameSection(original);
      assertTrue(serialized.isPresent(), "Should serialize name section with function names");

      final Optional<NameSection> parsed = parser.parseNameSection(serialized.get());
      assertTrue(parsed.isPresent(), "Should parse serialized name section");
      assertEquals(
          2,
          parsed.get().getFunctionNames().size(),
          "Parsed section should have 2 function names");
    }

    @Test
    @DisplayName("should roundtrip producers section")
    void shouldRoundtripProducersSection() {
      final ProducersSection original =
          ProducersSection.builder()
              .addLanguage(new ProducersSection.ProducerEntry("Rust", "1.70.0"))
              .addProcessedBy(new ProducersSection.ProducerEntry("rustc", "1.70.0"))
              .build();
      final Optional<byte[]> serialized = parser.serializeProducersSection(original);
      assertTrue(serialized.isPresent(), "Should serialize producers section");

      final Optional<ProducersSection> parsed =
          parser.parseProducersSection(serialized.get());
      assertTrue(parsed.isPresent(), "Should parse serialized producers section");
      assertEquals(
          1,
          parsed.get().getLanguages().size(),
          "Parsed section should have 1 language");
      assertEquals(
          1,
          parsed.get().getProcessedBy().size(),
          "Parsed section should have 1 processedBy");
    }

    @Test
    @DisplayName("should roundtrip target features section")
    void shouldRoundtripTargetFeaturesSection() {
      final TargetFeaturesSection original =
          TargetFeaturesSection.builder()
              .addRequiredFeature("mutable-globals")
              .addUsedFeature("sign-ext")
              .addDisabledFeature("threads")
              .build();
      final Optional<byte[]> serialized =
          parser.serializeTargetFeaturesSection(original);
      assertTrue(serialized.isPresent(), "Should serialize target features section");

      final Optional<TargetFeaturesSection> parsed =
          parser.parseTargetFeaturesSection(serialized.get());
      assertTrue(parsed.isPresent(), "Should parse serialized target features section");
      assertEquals(
          3,
          parsed.get().getFeatures().size(),
          "Parsed section should have 3 features");
      assertTrue(
          parsed.get().isFeatureRequired("mutable-globals"),
          "mutable-globals should be required after roundtrip");
      assertTrue(
          parsed.get().isFeatureUsed("sign-ext"),
          "sign-ext should be used after roundtrip");
      assertTrue(
          parsed.get().isFeatureDisabled("threads"),
          "threads should be disabled after roundtrip");
    }
  }
}
