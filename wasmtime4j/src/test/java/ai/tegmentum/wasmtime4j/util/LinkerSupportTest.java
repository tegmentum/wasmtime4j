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
package ai.tegmentum.wasmtime4j.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Linker.LinkerDefinition;
import ai.tegmentum.wasmtime4j.type.ExternType;
import ai.tegmentum.wasmtime4j.validation.ImportInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LinkerSupport} utility class.
 *
 * <p>Tests the pure-Java import tracking methods. Skips validateImports which requires native
 * Module.
 */
@DisplayName("LinkerSupport Tests")
class LinkerSupportTest {

  @Nested
  @DisplayName("importKey Tests")
  class ImportKeyTests {

    @Test
    @DisplayName("should concatenate module name and import name with separator")
    void shouldConcatenateWithSeparator() {
      final String key = LinkerSupport.importKey("env", "memory");

      assertEquals("env::memory", key, "Import key should be 'env::memory', got: " + key);
    }

    @Test
    @DisplayName("should handle empty module name")
    void shouldHandleEmptyModuleName() {
      final String key = LinkerSupport.importKey("", "func");

      assertEquals("::func", key, "Import key with empty module should be '::func', got: " + key);
    }

    @Test
    @DisplayName("should handle empty import name")
    void shouldHandleEmptyImportName() {
      final String key = LinkerSupport.importKey("env", "");

      assertEquals("env::", key, "Import key with empty name should be 'env::', got: " + key);
    }

    @Test
    @DisplayName("should produce unique keys for different imports")
    void shouldProduceUniqueKeys() {
      final String key1 = LinkerSupport.importKey("env", "memory");
      final String key2 = LinkerSupport.importKey("env", "table");
      final String key3 = LinkerSupport.importKey("wasi", "memory");

      assertFalse(key1.equals(key2), "Different import names should produce different keys");
      assertFalse(key1.equals(key3), "Different module names should produce different keys");
    }
  }

  @Nested
  @DisplayName("hasImport Tests")
  class HasImportTests {

    @Test
    @DisplayName("should return true for existing import")
    void shouldReturnTrueForExistingImport() {
      final Set<String> imports = new HashSet<>();
      imports.add("env::memory");

      assertTrue(
          LinkerSupport.hasImport(imports, "env", "memory"),
          "Should find existing import 'env::memory'");
    }

    @Test
    @DisplayName("should return false for missing import")
    void shouldReturnFalseForMissingImport() {
      final Set<String> imports = new HashSet<>();
      imports.add("env::memory");

      assertFalse(
          LinkerSupport.hasImport(imports, "env", "table"),
          "Should not find missing import 'env::table'");
    }

    @Test
    @DisplayName("should throw for null module name")
    void shouldThrowForNullModuleName() {
      final Set<String> imports = new HashSet<>();

      assertThrows(
          IllegalArgumentException.class,
          () -> LinkerSupport.hasImport(imports, null, "func"),
          "Should throw for null module name");
    }

    @Test
    @DisplayName("should throw for empty module name")
    void shouldThrowForEmptyModuleName() {
      final Set<String> imports = new HashSet<>();

      assertThrows(
          IllegalArgumentException.class,
          () -> LinkerSupport.hasImport(imports, "", "func"),
          "Should throw for empty module name");
    }

    @Test
    @DisplayName("should throw for null import name")
    void shouldThrowForNullImportName() {
      final Set<String> imports = new HashSet<>();

      assertThrows(
          IllegalArgumentException.class,
          () -> LinkerSupport.hasImport(imports, "env", null),
          "Should throw for null import name");
    }

    @Test
    @DisplayName("should throw for empty import name")
    void shouldThrowForEmptyImportName() {
      final Set<String> imports = new HashSet<>();

      assertThrows(
          IllegalArgumentException.class,
          () -> LinkerSupport.hasImport(imports, "env", ""),
          "Should throw for empty import name");
    }
  }

  @Nested
  @DisplayName("addImport Tests")
  class AddImportTests {

    @Test
    @DisplayName("should add import to tracking set")
    void shouldAddImportToSet() {
      final Set<String> imports = new HashSet<>();

      LinkerSupport.addImport(imports, "env", "memory");

      assertTrue(
          imports.contains("env::memory"),
          "Import set should contain 'env::memory' after addImport");
    }

    @Test
    @DisplayName("should not duplicate imports")
    void shouldNotDuplicateImports() {
      final Set<String> imports = new HashSet<>();

      LinkerSupport.addImport(imports, "env", "memory");
      LinkerSupport.addImport(imports, "env", "memory");

      assertEquals(
          1,
          imports.size(),
          "Adding same import twice should not duplicate, got size: " + imports.size());
    }

    @Test
    @DisplayName("should add multiple distinct imports")
    void shouldAddMultipleDistinctImports() {
      final Set<String> imports = new HashSet<>();

      LinkerSupport.addImport(imports, "env", "memory");
      LinkerSupport.addImport(imports, "env", "table");
      LinkerSupport.addImport(imports, "wasi", "fd_write");

      assertEquals(3, imports.size(), "Should have 3 distinct imports, got: " + imports.size());
    }
  }

  @Nested
  @DisplayName("addImportWithMetadata Tests")
  class AddImportWithMetadataTests {

    @Test
    @DisplayName("should add import to tracking set and registry")
    void shouldAddToSetAndRegistry() {
      final Set<String> imports = new HashSet<>();
      final Map<String, ImportInfo> registry = new HashMap<>();

      LinkerSupport.addImportWithMetadata(
          imports, registry, "env", "memory", ImportInfo.ImportKind.MEMORY, "Memory(min=1)");

      assertTrue(imports.contains("env::memory"), "Tracking set should contain 'env::memory'");
      assertTrue(registry.containsKey("env::memory"), "Registry should contain 'env::memory'");
    }

    @Test
    @DisplayName("should create ImportInfo with correct metadata")
    void shouldCreateImportInfoWithCorrectMetadata() {
      final Set<String> imports = new HashSet<>();
      final Map<String, ImportInfo> registry = new HashMap<>();

      LinkerSupport.addImportWithMetadata(
          imports, registry, "env", "memory", ImportInfo.ImportKind.MEMORY, "Memory(min=1)");

      final ImportInfo info = registry.get("env::memory");
      assertNotNull(info, "ImportInfo should not be null");
      assertEquals("env", info.getModuleName(), "Module name should be 'env'");
      assertEquals("memory", info.getImportName(), "Import name should be 'memory'");
      assertEquals(
          ImportInfo.ImportKind.MEMORY, info.getImportKind(), "Import kind should be MEMORY");
      assertTrue(info.getTypeSignature().isPresent(), "Type signature should be present");
      assertEquals("Memory(min=1)", info.getTypeSignature().get(), "Type signature should match");
      assertTrue(info.isHostFunction(), "Should be marked as host-provided");
    }

    @Test
    @DisplayName("should handle null type signature")
    void shouldHandleNullTypeSignature() {
      final Set<String> imports = new HashSet<>();
      final Map<String, ImportInfo> registry = new HashMap<>();

      LinkerSupport.addImportWithMetadata(
          imports, registry, "env", "func", ImportInfo.ImportKind.FUNCTION, null);

      final ImportInfo info = registry.get("env::func");
      assertNotNull(info, "ImportInfo should not be null");
      assertFalse(
          info.getTypeSignature().isPresent(), "Type signature should be empty for null input");
    }
  }

  @Nested
  @DisplayName("iterDefinitions Tests")
  class IterDefinitionsTests {

    @Test
    @DisplayName("should return empty list for empty registry")
    void shouldReturnEmptyForEmptyRegistry() {
      final Map<String, ImportInfo> registry = new HashMap<>();

      final List<LinkerDefinition> definitions = LinkerSupport.iterDefinitions(registry);

      assertNotNull(definitions, "Definitions should not be null");
      assertTrue(definitions.isEmpty(), "Definitions should be empty for empty registry");
    }

    @Test
    @DisplayName("should map FUNCTION import kind to FUNC extern type")
    void shouldMapFunctionToFunc() {
      final Map<String, ImportInfo> registry = new HashMap<>();
      registry.put(
          "env::func",
          new ImportInfo(
              "env",
              "func",
              ImportInfo.ImportKind.FUNCTION,
              Optional.of("(i32) -> i32"),
              java.time.Instant.now(),
              true,
              Optional.of("Host-provided")));

      final List<LinkerDefinition> definitions = LinkerSupport.iterDefinitions(registry);

      assertEquals(1, definitions.size(), "Should have 1 definition");
      final LinkerDefinition def = definitions.get(0);
      assertEquals("env", def.getModuleName(), "Module name should be 'env'");
      assertEquals("func", def.getName(), "Import name should be 'func'");
      assertEquals(ExternType.FUNC, def.getType(), "FUNCTION import should map to FUNC ExternType");
    }

    @Test
    @DisplayName("should map MEMORY import kind to MEMORY extern type")
    void shouldMapMemoryToMemory() {
      final Map<String, ImportInfo> registry = new HashMap<>();
      registry.put(
          "env::memory",
          new ImportInfo(
              "env",
              "memory",
              ImportInfo.ImportKind.MEMORY,
              Optional.empty(),
              java.time.Instant.now(),
              false,
              Optional.empty()));

      final List<LinkerDefinition> definitions = LinkerSupport.iterDefinitions(registry);

      assertEquals(1, definitions.size(), "Should have 1 definition");
      assertEquals(
          ExternType.MEMORY,
          definitions.get(0).getType(),
          "MEMORY import should map to MEMORY ExternType");
    }

    @Test
    @DisplayName("should map TABLE import kind to TABLE extern type")
    void shouldMapTableToTable() {
      final Map<String, ImportInfo> registry = new HashMap<>();
      registry.put(
          "env::table",
          new ImportInfo(
              "env",
              "table",
              ImportInfo.ImportKind.TABLE,
              Optional.empty(),
              java.time.Instant.now(),
              false,
              Optional.empty()));

      final List<LinkerDefinition> definitions = LinkerSupport.iterDefinitions(registry);

      assertEquals(ExternType.TABLE, definitions.get(0).getType(), "TABLE should map to TABLE");
    }

    @Test
    @DisplayName("should map GLOBAL import kind to GLOBAL extern type")
    void shouldMapGlobalToGlobal() {
      final Map<String, ImportInfo> registry = new HashMap<>();
      registry.put(
          "env::global",
          new ImportInfo(
              "env",
              "global",
              ImportInfo.ImportKind.GLOBAL,
              Optional.empty(),
              java.time.Instant.now(),
              false,
              Optional.empty()));

      final List<LinkerDefinition> definitions = LinkerSupport.iterDefinitions(registry);

      assertEquals(ExternType.GLOBAL, definitions.get(0).getType(), "GLOBAL should map to GLOBAL");
    }

    @Test
    @DisplayName("should handle multiple entries")
    void shouldHandleMultipleEntries() {
      final Map<String, ImportInfo> registry = new HashMap<>();
      registry.put(
          "env::func",
          new ImportInfo(
              "env",
              "func",
              ImportInfo.ImportKind.FUNCTION,
              Optional.empty(),
              java.time.Instant.now(),
              true,
              Optional.empty()));
      registry.put(
          "env::mem",
          new ImportInfo(
              "env",
              "mem",
              ImportInfo.ImportKind.MEMORY,
              Optional.empty(),
              java.time.Instant.now(),
              false,
              Optional.empty()));

      final List<LinkerDefinition> definitions = LinkerSupport.iterDefinitions(registry);

      assertEquals(2, definitions.size(), "Should have 2 definitions, got: " + definitions.size());
    }
  }
}
