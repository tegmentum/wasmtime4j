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

package ai.tegmentum.wasmtime4j.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExportFormat} enum.
 *
 * <p>ExportFormat defines supported export formats for performance profiling data.
 */
@DisplayName("ExportFormat Tests")
class ExportFormatTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeEnum() {
      assertTrue(ExportFormat.class.isEnum(), "ExportFormat should be an enum");
    }

    @Test
    @DisplayName("should have JSON constant")
    void shouldHaveJsonConstant() {
      assertNotNull(ExportFormat.JSON, "JSON constant should exist");
    }

    @Test
    @DisplayName("should have CSV constant")
    void shouldHaveCsvConstant() {
      assertNotNull(ExportFormat.CSV, "CSV constant should exist");
    }

    @Test
    @DisplayName("should have BINARY constant")
    void shouldHaveBinaryConstant() {
      assertNotNull(ExportFormat.BINARY, "BINARY constant should exist");
    }

    @Test
    @DisplayName("should have JFR constant")
    void shouldHaveJfrConstant() {
      assertNotNull(ExportFormat.JFR, "JFR constant should exist");
    }

    @Test
    @DisplayName("should have FLAME_GRAPH constant")
    void shouldHaveFlameGraphConstant() {
      assertNotNull(ExportFormat.FLAME_GRAPH, "FLAME_GRAPH constant should exist");
    }

    @Test
    @DisplayName("should have JMH_JSON constant")
    void shouldHaveJmhJsonConstant() {
      assertNotNull(ExportFormat.JMH_JSON, "JMH_JSON constant should exist");
    }

    @Test
    @DisplayName("should have 6 format types")
    void shouldHave6FormatTypes() {
      assertEquals(6, ExportFormat.values().length, "Should have 6 format types");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getFileExtension method")
    void shouldHaveGetFileExtensionMethod() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("getFileExtension");
      assertNotNull(method, "getFileExtension method should exist");
      assertEquals(String.class, method.getReturnType(), "getFileExtension should return String");
    }

    @Test
    @DisplayName("should have getMimeType method")
    void shouldHaveGetMimeTypeMethod() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("getMimeType");
      assertNotNull(method, "getMimeType method should exist");
      assertEquals(String.class, method.getReturnType(), "getMimeType should return String");
    }

    @Test
    @DisplayName("should have getDescription method")
    void shouldHaveGetDescriptionMethod() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("getDescription");
      assertNotNull(method, "getDescription method should exist");
      assertEquals(String.class, method.getReturnType(), "getDescription should return String");
    }

    @Test
    @DisplayName("should have isHumanReadable method")
    void shouldHaveIsHumanReadableMethod() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("isHumanReadable");
      assertNotNull(method, "isHumanReadable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isHumanReadable should return boolean");
    }

    @Test
    @DisplayName("should have isCompressed method")
    void shouldHaveIsCompressedMethod() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("isCompressed");
      assertNotNull(method, "isCompressed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isCompressed should return boolean");
    }
  }

  @Nested
  @DisplayName("Classification Method Tests")
  class ClassificationMethodTests {

    @Test
    @DisplayName("should have isWebCompatible method")
    void shouldHaveIsWebCompatibleMethod() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("isWebCompatible");
      assertNotNull(method, "isWebCompatible method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isWebCompatible should return boolean");
    }

    @Test
    @DisplayName("should have isVisualizationFormat method")
    void shouldHaveIsVisualizationFormatMethod() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("isVisualizationFormat");
      assertNotNull(method, "isVisualizationFormat method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isVisualizationFormat should return boolean");
    }

    @Test
    @DisplayName("should have isAnalysisFormat method")
    void shouldHaveIsAnalysisFormatMethod() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("isAnalysisFormat");
      assertNotNull(method, "isAnalysisFormat method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isAnalysisFormat should return boolean");
    }

    @Test
    @DisplayName("should have isArchivalFormat method")
    void shouldHaveIsArchivalFormatMethod() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("isArchivalFormat");
      assertNotNull(method, "isArchivalFormat method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isArchivalFormat should return boolean");
    }
  }

  @Nested
  @DisplayName("Utility Method Tests")
  class UtilityMethodTests {

    @Test
    @DisplayName("should have getFilename method")
    void shouldHaveGetFilenameMethod() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("getFilename", String.class);
      assertNotNull(method, "getFilename method should exist");
      assertEquals(String.class, method.getReturnType(), "getFilename should return String");
    }

    @Test
    @DisplayName("should have getEstimatedSizeMultiplier method")
    void shouldHaveGetEstimatedSizeMultiplierMethod() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("getEstimatedSizeMultiplier");
      assertNotNull(method, "getEstimatedSizeMultiplier method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getEstimatedSizeMultiplier should return double");
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have getBestMatch static method")
    void shouldHaveGetBestMatchStaticMethod() throws NoSuchMethodException {
      final Method method =
          ExportFormat.class.getMethod("getBestMatch", boolean.class, boolean.class, boolean.class);
      assertNotNull(method, "getBestMatch method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "getBestMatch should be static");
      assertEquals(
          ExportFormat.class, method.getReturnType(), "getBestMatch should return ExportFormat");
    }

    @Test
    @DisplayName("should have fromString static method")
    void shouldHaveFromStringStaticMethod() throws NoSuchMethodException {
      final Method method = ExportFormat.class.getMethod("fromString", String.class);
      assertNotNull(method, "fromString method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromString should be static");
      assertEquals(
          ExportFormat.class, method.getReturnType(), "fromString should return ExportFormat");
    }
  }

  @Nested
  @DisplayName("Format Properties Tests")
  class FormatPropertiesTests {

    @Test
    @DisplayName("JSON should have correct properties")
    void jsonShouldHaveCorrectProperties() {
      assertEquals("json", ExportFormat.JSON.getFileExtension(), "JSON extension should be json");
      assertEquals(
          "application/json", ExportFormat.JSON.getMimeType(), "JSON MIME type should match");
      assertTrue(ExportFormat.JSON.isHumanReadable(), "JSON should be human readable");
      assertFalse(ExportFormat.JSON.isCompressed(), "JSON should not be compressed");
      assertTrue(ExportFormat.JSON.isWebCompatible(), "JSON should be web compatible");
      assertTrue(ExportFormat.JSON.isAnalysisFormat(), "JSON should be analysis format");
    }

    @Test
    @DisplayName("CSV should have correct properties")
    void csvShouldHaveCorrectProperties() {
      assertEquals("csv", ExportFormat.CSV.getFileExtension(), "CSV extension should be csv");
      assertEquals("text/csv", ExportFormat.CSV.getMimeType(), "CSV MIME type should match");
      assertTrue(ExportFormat.CSV.isHumanReadable(), "CSV should be human readable");
      assertFalse(ExportFormat.CSV.isCompressed(), "CSV should not be compressed");
      assertTrue(ExportFormat.CSV.isWebCompatible(), "CSV should be web compatible");
      assertTrue(ExportFormat.CSV.isAnalysisFormat(), "CSV should be analysis format");
    }

    @Test
    @DisplayName("BINARY should have correct properties")
    void binaryShouldHaveCorrectProperties() {
      assertEquals("bin", ExportFormat.BINARY.getFileExtension(), "BINARY extension should be bin");
      assertFalse(ExportFormat.BINARY.isHumanReadable(), "BINARY should not be human readable");
      assertTrue(ExportFormat.BINARY.isCompressed(), "BINARY should be compressed");
      assertFalse(ExportFormat.BINARY.isWebCompatible(), "BINARY should not be web compatible");
      assertTrue(ExportFormat.BINARY.isArchivalFormat(), "BINARY should be archival format");
    }

    @Test
    @DisplayName("JFR should have correct properties")
    void jfrShouldHaveCorrectProperties() {
      assertEquals("jfr", ExportFormat.JFR.getFileExtension(), "JFR extension should be jfr");
      assertFalse(ExportFormat.JFR.isHumanReadable(), "JFR should not be human readable");
      assertTrue(ExportFormat.JFR.isCompressed(), "JFR should be compressed");
      assertTrue(ExportFormat.JFR.isVisualizationFormat(), "JFR should be visualization format");
      assertTrue(ExportFormat.JFR.isArchivalFormat(), "JFR should be archival format");
    }

    @Test
    @DisplayName("FLAME_GRAPH should have correct properties")
    void flameGraphShouldHaveCorrectProperties() {
      assertEquals(
          "svg",
          ExportFormat.FLAME_GRAPH.getFileExtension(),
          "FLAME_GRAPH extension should be svg");
      assertTrue(
          ExportFormat.FLAME_GRAPH.isHumanReadable(), "FLAME_GRAPH should be human readable");
      assertFalse(ExportFormat.FLAME_GRAPH.isCompressed(), "FLAME_GRAPH should not be compressed");
      assertTrue(
          ExportFormat.FLAME_GRAPH.isVisualizationFormat(),
          "FLAME_GRAPH should be visualization format");
    }

    @Test
    @DisplayName("JMH_JSON should have correct properties")
    void jmhJsonShouldHaveCorrectProperties() {
      assertEquals(
          "json", ExportFormat.JMH_JSON.getFileExtension(), "JMH_JSON extension should be json");
      assertTrue(ExportFormat.JMH_JSON.isHumanReadable(), "JMH_JSON should be human readable");
      assertFalse(ExportFormat.JMH_JSON.isCompressed(), "JMH_JSON should not be compressed");
      assertTrue(ExportFormat.JMH_JSON.isAnalysisFormat(), "JMH_JSON should be analysis format");
    }
  }

  @Nested
  @DisplayName("Instance Method Tests")
  class InstanceMethodTests {

    @Test
    @DisplayName("should generate correct filename")
    void shouldGenerateCorrectFilename() {
      assertEquals("report.json", ExportFormat.JSON.getFilename("report"), "Filename should match");
      assertEquals("data.csv", ExportFormat.CSV.getFilename("data"), "Filename should match");
      assertEquals(
          "archive.bin", ExportFormat.BINARY.getFilename("archive"), "Filename should match");
    }

    @Test
    @DisplayName("should return reasonable size multipliers")
    void shouldReturnReasonableSizeMultipliers() {
      assertTrue(
          ExportFormat.BINARY.getEstimatedSizeMultiplier() < 1.0, "BINARY should be compact");
      assertTrue(ExportFormat.JSON.getEstimatedSizeMultiplier() > 1.0, "JSON should be larger");
      assertTrue(ExportFormat.CSV.getEstimatedSizeMultiplier() > 1.0, "CSV should be larger");
    }
  }

  @Nested
  @DisplayName("Static Method Behavior Tests")
  class StaticMethodBehaviorTests {

    @Test
    @DisplayName("getBestMatch should return JSON for human readable web compatible")
    void getBestMatchShouldReturnJsonForHumanReadableWebCompatible() {
      assertEquals(
          ExportFormat.JSON,
          ExportFormat.getBestMatch(true, false, true),
          "Should return JSON for human readable web compatible");
    }

    @Test
    @DisplayName("getBestMatch should return CSV for non-human readable web compatible")
    void getBestMatchShouldReturnCsvForNonHumanReadableWebCompatible() {
      assertEquals(
          ExportFormat.CSV,
          ExportFormat.getBestMatch(false, false, true),
          "Should return CSV for non-human readable web compatible");
    }

    @Test
    @DisplayName("getBestMatch should return BINARY for compressed")
    void getBestMatchShouldReturnBinaryForCompressed() {
      assertEquals(
          ExportFormat.BINARY,
          ExportFormat.getBestMatch(false, true, false),
          "Should return BINARY for compressed");
    }

    @Test
    @DisplayName("fromString should parse format names correctly")
    void fromStringShouldParseFormatNamesCorrectly() {
      assertEquals(ExportFormat.JSON, ExportFormat.fromString("JSON"), "Should parse JSON");
      assertEquals(ExportFormat.JSON, ExportFormat.fromString("json"), "Should parse lowercase");
      assertEquals(ExportFormat.CSV, ExportFormat.fromString("CSV"), "Should parse CSV");
      assertEquals(ExportFormat.BINARY, ExportFormat.fromString("binary"), "Should parse binary");
      assertEquals(ExportFormat.JFR, ExportFormat.fromString("JFR"), "Should parse JFR");
      assertEquals(
          ExportFormat.FLAME_GRAPH,
          ExportFormat.fromString("FLAME_GRAPH"),
          "Should parse FLAME_GRAPH");
      assertEquals(
          ExportFormat.FLAME_GRAPH,
          ExportFormat.fromString("flame-graph"),
          "Should parse flame-graph with hyphen");
      assertEquals(
          ExportFormat.JMH_JSON, ExportFormat.fromString("JMH_JSON"), "Should parse JMH_JSON");
    }

    @Test
    @DisplayName("fromString should handle alternative names")
    void fromStringShouldHandleAlternativeNames() {
      assertEquals(
          ExportFormat.FLAME_GRAPH,
          ExportFormat.fromString("flamegraph"),
          "Should parse flamegraph");
      assertEquals(
          ExportFormat.FLAME_GRAPH, ExportFormat.fromString("flame"), "Should parse flame");
      assertEquals(ExportFormat.JMH_JSON, ExportFormat.fromString("jmh"), "Should parse jmh");
    }

    @Test
    @DisplayName("fromString should throw for null input")
    void fromStringShouldThrowForNullInput() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExportFormat.fromString(null),
          "Should throw for null input");
    }

    @Test
    @DisplayName("fromString should throw for empty input")
    void fromStringShouldThrowForEmptyInput() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExportFormat.fromString(""),
          "Should throw for empty input");
      assertThrows(
          IllegalArgumentException.class,
          () -> ExportFormat.fromString("  "),
          "Should throw for whitespace input");
    }

    @Test
    @DisplayName("fromString should throw for unknown format")
    void fromStringShouldThrowForUnknownFormat() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExportFormat.fromString("unknown"),
          "Should throw for unknown format");
    }
  }
}
