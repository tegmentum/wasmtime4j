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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.validation.ImportInfo;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ImportInfo} class.
 *
 * <p>ImportInfo provides comprehensive metadata about registered imports including their types,
 * signatures, and registration details.
 */
@DisplayName("ImportInfo Tests")
class ImportInfoTest {

  private ImportInfo createDefaultImportInfo() {
    return new ImportInfo(
        "env",
        "log",
        ImportInfo.ImportKind.FUNCTION,
        Optional.of("(i32) -> void"),
        Instant.parse("2025-01-01T00:00:00Z"),
        true,
        Optional.of("host"));
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final Instant now = Instant.now();
      final ImportInfo info =
          new ImportInfo(
              "wasi_snapshot_preview1",
              "fd_write",
              ImportInfo.ImportKind.FUNCTION,
              Optional.of("(i32, i32, i32, i32) -> i32"),
              now,
              true,
              Optional.of("WASI implementation"));

      assertEquals("wasi_snapshot_preview1", info.getModuleName(), "moduleName should match");
      assertEquals("fd_write", info.getImportName(), "importName should match");
      assertEquals(
          ImportInfo.ImportKind.FUNCTION, info.getImportKind(), "importType should be FUNCTION");
      assertTrue(info.getTypeSignature().isPresent(), "typeSignature should be present");
      assertEquals(now, info.getDefinedAt(), "definedAt should match");
      assertTrue(info.isHostFunction(), "isHostFunction should be true");
      assertTrue(info.getSourceDescription().isPresent(), "sourceDescription should be present");
    }

    @Test
    @DisplayName("should create instance with empty optionals")
    void shouldCreateInstanceWithEmptyOptionals() {
      final ImportInfo info =
          new ImportInfo(
              "mod",
              "imp",
              ImportInfo.ImportKind.MEMORY,
              Optional.empty(),
              Instant.EPOCH,
              false,
              Optional.empty());

      assertFalse(info.getTypeSignature().isPresent(), "typeSignature should be empty");
      assertFalse(info.isHostFunction(), "isHostFunction should be false");
      assertFalse(info.getSourceDescription().isPresent(), "sourceDescription should be empty");
    }

    @Test
    @DisplayName("should throw NullPointerException for null moduleName")
    void shouldThrowForNullModuleName() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ImportInfo(
                  null,
                  "imp",
                  ImportInfo.ImportKind.FUNCTION,
                  Optional.empty(),
                  Instant.now(),
                  false,
                  Optional.empty()),
          "null moduleName should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null importName")
    void shouldThrowForNullImportName() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ImportInfo(
                  "mod",
                  null,
                  ImportInfo.ImportKind.FUNCTION,
                  Optional.empty(),
                  Instant.now(),
                  false,
                  Optional.empty()),
          "null importName should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null importType")
    void shouldThrowForNullImportType() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ImportInfo(
                  "mod", "imp", null, Optional.empty(), Instant.now(), false, Optional.empty()),
          "null importType should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null typeSignature Optional")
    void shouldThrowForNullTypeSignatureOptional() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ImportInfo(
                  "mod",
                  "imp",
                  ImportInfo.ImportKind.FUNCTION,
                  null,
                  Instant.now(),
                  false,
                  Optional.empty()),
          "null typeSignature should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null definedAt")
    void shouldThrowForNullDefinedAt() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ImportInfo(
                  "mod",
                  "imp",
                  ImportInfo.ImportKind.FUNCTION,
                  Optional.empty(),
                  null,
                  false,
                  Optional.empty()),
          "null definedAt should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null sourceDescription Optional")
    void shouldThrowForNullSourceDescriptionOptional() {
      assertThrows(
          NullPointerException.class,
          () ->
              new ImportInfo(
                  "mod",
                  "imp",
                  ImportInfo.ImportKind.FUNCTION,
                  Optional.empty(),
                  Instant.now(),
                  false,
                  null),
          "null sourceDescription should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("ImportType Enum Tests")
  class ImportTypeEnumTests {

    @Test
    @DisplayName("should have all expected import types")
    void shouldHaveAllExpectedImportTypes() {
      final ImportInfo.ImportKind[] values = ImportInfo.ImportKind.values();

      assertEquals(5, values.length, "Should have 5 ImportType values");
      assertNotNull(ImportInfo.ImportKind.FUNCTION, "FUNCTION should exist");
      assertNotNull(ImportInfo.ImportKind.MEMORY, "MEMORY should exist");
      assertNotNull(ImportInfo.ImportKind.TABLE, "TABLE should exist");
      assertNotNull(ImportInfo.ImportKind.GLOBAL, "GLOBAL should exist");
      assertNotNull(ImportInfo.ImportKind.INSTANCE, "INSTANCE should exist");
    }

    @Test
    @DisplayName("valueOf should return correct enum values")
    void valueOfShouldReturnCorrectEnumValues() {
      assertEquals(
          ImportInfo.ImportKind.FUNCTION,
          ImportInfo.ImportKind.valueOf("FUNCTION"),
          "valueOf('FUNCTION') should return FUNCTION");
      assertEquals(
          ImportInfo.ImportKind.MEMORY,
          ImportInfo.ImportKind.valueOf("MEMORY"),
          "valueOf('MEMORY') should return MEMORY");
    }
  }

  @Nested
  @DisplayName("getImportIdentifier Tests")
  class GetImportIdentifierTests {

    @Test
    @DisplayName("should return moduleName::importName format")
    void shouldReturnCorrectFormat() {
      final ImportInfo info = createDefaultImportInfo();

      assertEquals(
          "env::log", info.getImportIdentifier(), "getImportIdentifier should return 'env::log'");
    }

    @Test
    @DisplayName("should handle different module and import names")
    void shouldHandleDifferentNames() {
      final ImportInfo info =
          new ImportInfo(
              "wasi",
              "fd_write",
              ImportInfo.ImportKind.FUNCTION,
              Optional.empty(),
              Instant.EPOCH,
              false,
              Optional.empty());

      assertEquals(
          "wasi::fd_write",
          info.getImportIdentifier(),
          "getImportIdentifier should return 'wasi::fd_write'");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for identical values")
    void equalsShouldReturnTrueForIdenticalValues() {
      final Instant now = Instant.parse("2025-06-15T12:00:00Z");
      final ImportInfo info1 =
          new ImportInfo(
              "mod",
              "imp",
              ImportInfo.ImportKind.FUNCTION,
              Optional.of("sig"),
              now,
              true,
              Optional.of("src"));
      final ImportInfo info2 =
          new ImportInfo(
              "mod",
              "imp",
              ImportInfo.ImportKind.FUNCTION,
              Optional.of("sig"),
              now,
              true,
              Optional.of("src"));

      assertEquals(info1, info2, "ImportInfo with same values should be equal");
    }

    @Test
    @DisplayName("equals should return false for different moduleName")
    void equalsShouldReturnFalseForDifferentModuleName() {
      final Instant now = Instant.now();
      final ImportInfo info1 =
          new ImportInfo(
              "modA",
              "imp",
              ImportInfo.ImportKind.FUNCTION,
              Optional.empty(),
              now,
              false,
              Optional.empty());
      final ImportInfo info2 =
          new ImportInfo(
              "modB",
              "imp",
              ImportInfo.ImportKind.FUNCTION,
              Optional.empty(),
              now,
              false,
              Optional.empty());

      assertNotEquals(info1, info2, "Different moduleName should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different importType")
    void equalsShouldReturnFalseForDifferentImportType() {
      final Instant now = Instant.now();
      final ImportInfo info1 =
          new ImportInfo(
              "mod",
              "imp",
              ImportInfo.ImportKind.FUNCTION,
              Optional.empty(),
              now,
              false,
              Optional.empty());
      final ImportInfo info2 =
          new ImportInfo(
              "mod",
              "imp",
              ImportInfo.ImportKind.MEMORY,
              Optional.empty(),
              now,
              false,
              Optional.empty());

      assertNotEquals(info1, info2, "Different importType should not be equal");
    }

    @Test
    @DisplayName("equals should return true for same object")
    void equalsShouldReturnTrueForSameObject() {
      final ImportInfo info = createDefaultImportInfo();

      assertEquals(info, info, "Same object should be equal to itself");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final ImportInfo info = createDefaultImportInfo();

      assertNotEquals(null, info, "ImportInfo should not be equal to null");
    }

    @Test
    @DisplayName("hashCode should be consistent for equal instances")
    void hashCodeShouldBeConsistentForEqualInstances() {
      final Instant now = Instant.parse("2025-06-15T12:00:00Z");
      final ImportInfo info1 =
          new ImportInfo(
              "mod",
              "imp",
              ImportInfo.ImportKind.FUNCTION,
              Optional.of("sig"),
              now,
              true,
              Optional.of("src"));
      final ImportInfo info2 =
          new ImportInfo(
              "mod",
              "imp",
              ImportInfo.ImportKind.FUNCTION,
              Optional.of("sig"),
              now,
              true,
              Optional.of("src"));

      assertEquals(
          info1.hashCode(),
          info2.hashCode(),
          "Equal ImportInfo instances should have same hashCode");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain ImportInfo prefix")
    void toStringShouldContainImportInfoPrefix() {
      final ImportInfo info = createDefaultImportInfo();

      assertTrue(info.toString().contains("ImportInfo{"), "toString should contain 'ImportInfo{'");
    }

    @Test
    @DisplayName("toString should contain import identifier")
    void toStringShouldContainImportIdentifier() {
      final ImportInfo info = createDefaultImportInfo();

      assertTrue(info.toString().contains("env::log"), "toString should contain import identifier");
    }

    @Test
    @DisplayName("toString should contain type")
    void toStringShouldContainType() {
      final ImportInfo info = createDefaultImportInfo();

      assertTrue(info.toString().contains("type=FUNCTION"), "toString should contain import type");
    }

    @Test
    @DisplayName("toString should include signature when present")
    void toStringShouldIncludeSignatureWhenPresent() {
      final ImportInfo info = createDefaultImportInfo();

      assertTrue(
          info.toString().contains("signature="), "toString should include signature when present");
    }

    @Test
    @DisplayName("toString should include hostFunction when true")
    void toStringShouldIncludeHostFunctionWhenTrue() {
      final ImportInfo info = createDefaultImportInfo();

      assertTrue(
          info.toString().contains("hostFunction"),
          "toString should include hostFunction when true");
    }

    @Test
    @DisplayName("toString should include source when present")
    void toStringShouldIncludeSourceWhenPresent() {
      final ImportInfo info = createDefaultImportInfo();

      assertTrue(
          info.toString().contains("source=host"), "toString should include source when present");
    }
  }
}
