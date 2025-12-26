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

package ai.tegmentum.wasmtime4j.cache;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MetadataCache} class.
 *
 * <p>MetadataCache provides high-performance caching for WebAssembly module metadata.
 */
@DisplayName("MetadataCache Tests")
class MetadataCacheTest {

  private MetadataCache cache;

  @BeforeEach
  void setUp() {
    cache = MetadataCache.getInstance();
    cache.clear();
  }

  @Nested
  @DisplayName("Singleton Tests")
  class SingletonTests {

    @Test
    @DisplayName("should return same instance")
    void shouldReturnSameInstance() {
      final MetadataCache instance1 = MetadataCache.getInstance();
      final MetadataCache instance2 = MetadataCache.getInstance();
      assertEquals(instance1, instance2, "Should return same instance");
    }

    @Test
    @DisplayName("getInstance should return non-null")
    void getInstanceShouldReturnNonNull() {
      assertNotNull(MetadataCache.getInstance(), "Instance should not be null");
    }
  }

  @Nested
  @DisplayName("Basic Cache Operations Tests")
  class BasicCacheOperationsTests {

    @Test
    @DisplayName("should put and get value")
    void shouldPutAndGetValue() {
      cache.put("key1", "value1");
      assertEquals("value1", cache.get("key1"), "Should retrieve stored value");
    }

    @Test
    @DisplayName("should return null for non-existent key")
    void shouldReturnNullForNonExistentKey() {
      assertNull(cache.get("nonexistent"), "Should return null for non-existent key");
    }

    @Test
    @DisplayName("should invalidate key")
    void shouldInvalidateKey() {
      cache.put("key1", "value1");
      cache.invalidate("key1");
      assertNull(cache.get("key1"), "Should return null after invalidation");
    }

    @Test
    @DisplayName("should clear all entries")
    void shouldClearAllEntries() {
      cache.put("key1", "value1");
      cache.put("key2", "value2");
      cache.clear();
      assertNull(cache.get("key1"), "key1 should be cleared");
      assertNull(cache.get("key2"), "key2 should be cleared");
    }

    @Test
    @DisplayName("should handle null key on get")
    void shouldHandleNullKeyOnGet() {
      assertNull(cache.get(null), "Should return null for null key");
    }

    @Test
    @DisplayName("should not store null value")
    void shouldNotStoreNullValue() {
      cache.put("key", null);
      assertNull(cache.get("key"), "Should not store null value");
    }
  }

  @Nested
  @DisplayName("ComputeIfAbsent Tests")
  class ComputeIfAbsentTests {

    @Test
    @DisplayName("should compute value if absent")
    void shouldComputeValueIfAbsent() {
      final String result = cache.computeIfAbsent("newKey", key -> "computed_" + key);
      assertEquals("computed_newKey", result, "Should compute and return value");
    }

    @Test
    @DisplayName("should return existing value without computing")
    void shouldReturnExistingValueWithoutComputing() {
      cache.put("existingKey", "existingValue");
      final String result = cache.computeIfAbsent("existingKey", key -> "newValue");
      assertEquals("existingValue", result, "Should return existing value");
    }

    @Test
    @DisplayName("should store computed value")
    void shouldStoreComputedValue() {
      cache.computeIfAbsent("computedKey", key -> "computedValue");
      assertEquals("computedValue", cache.get("computedKey"), "Should store computed value");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("should return non-null statistics")
    void shouldReturnNonNullStatistics() {
      assertNotNull(cache.getStatistics(), "Statistics should not be null");
    }

    @Test
    @DisplayName("statistics should include cache info")
    void statisticsShouldIncludeCacheInfo() {
      cache.put("key", "value");
      cache.get("key"); // Hit
      cache.get("missing"); // Miss
      final String stats = cache.getStatistics();
      assertTrue(stats.contains("Cache"), "Should contain cache info");
    }

    @Test
    @DisplayName("should return hit rate")
    void shouldReturnHitRate() {
      cache.put("key", "value");
      cache.get("key"); // Hit
      cache.get("key"); // Hit
      cache.get("missing"); // Miss
      final double hitRate = cache.getHitRate();
      assertTrue(hitRate >= 0.0 && hitRate <= 100.0, "Hit rate should be between 0 and 100");
    }

    @Test
    @DisplayName("should return average access time")
    void shouldReturnAverageAccessTime() {
      cache.put("key", "value");
      cache.get("key");
      final double avgTime = cache.getAverageAccessTimeNs();
      assertTrue(avgTime >= 0.0, "Average access time should be non-negative");
    }
  }

  @Nested
  @DisplayName("Enable/Disable Tests")
  class EnableDisableTests {

    @Test
    @DisplayName("should be enabled by default")
    void shouldBeEnabledByDefault() {
      assertTrue(MetadataCache.isEnabled(), "Cache should be enabled by default");
    }

    @Test
    @DisplayName("should allow enabling and disabling")
    void shouldAllowEnablingAndDisabling() {
      final boolean originalState = MetadataCache.isEnabled();
      try {
        MetadataCache.setEnabled(false);
        assertFalse(MetadataCache.isEnabled(), "Cache should be disabled");
        MetadataCache.setEnabled(true);
        assertTrue(MetadataCache.isEnabled(), "Cache should be enabled");
      } finally {
        MetadataCache.setEnabled(originalState);
      }
    }
  }

  @Nested
  @DisplayName("Maintenance Tests")
  class MaintenanceTests {

    @Test
    @DisplayName("should perform maintenance without error")
    void shouldPerformMaintenanceWithoutError() {
      cache.put("key1", "value1");
      cache.put("key2", "value2");
      // Should not throw
      cache.performMaintenance();
    }
  }

  @Nested
  @DisplayName("ModuleMetadata Tests")
  class ModuleMetadataTests {

    @Test
    @DisplayName("should create ModuleMetadata with all fields")
    void shouldCreateModuleMetadataWithAllFields() {
      final Map<String, MetadataCache.FunctionSignature> functions = new HashMap<>();
      functions.put(
          "testFunc",
          new MetadataCache.FunctionSignature(
              "testFunc", new String[] {"i32"}, new String[] {"i64"}, false));

      final Map<String, MetadataCache.GlobalMetadata> globals = new HashMap<>();
      globals.put("counter", new MetadataCache.GlobalMetadata("counter", "i32", true, 0));

      final Map<String, MetadataCache.MemoryMetadata> memories = new HashMap<>();
      memories.put("main", new MetadataCache.MemoryMetadata("main", 1, 10, false));

      final Map<String, MetadataCache.TableMetadata> tables = new HashMap<>();
      tables.put("funcTable", new MetadataCache.TableMetadata("funcTable", "funcref", 0, 100));

      final Set<String> imports = new HashSet<>();
      imports.add("wasi_snapshot_preview1");

      final MetadataCache.TypeValidationResult validation =
          new MetadataCache.TypeValidationResult(true, null, null, 10);

      final MetadataCache.ModuleMetadata metadata =
          new MetadataCache.ModuleMetadata(
              "abc123", functions, globals, memories, tables, imports, null, validation);

      assertEquals("abc123", metadata.getModuleHash(), "Module hash should match");
      assertEquals(1, metadata.getExportedFunctions().size(), "Should have 1 function");
      assertEquals(1, metadata.getGlobals().size(), "Should have 1 global");
      assertEquals(1, metadata.getMemories().size(), "Should have 1 memory");
      assertEquals(1, metadata.getTables().size(), "Should have 1 table");
      assertTrue(
          metadata.getImportedModules().contains("wasi_snapshot_preview1"), "Should have import");
      assertTrue(metadata.getValidationResult().isValid(), "Validation should be valid");
      assertTrue(metadata.getCreatedTime() > 0, "Created time should be set");
    }

    @Test
    @DisplayName("should handle null collections in ModuleMetadata")
    void shouldHandleNullCollectionsInModuleMetadata() {
      final MetadataCache.ModuleMetadata metadata =
          new MetadataCache.ModuleMetadata("hash", null, null, null, null, null, null, null);

      assertNotNull(metadata.getExportedFunctions(), "Functions should not be null");
      assertNotNull(metadata.getGlobals(), "Globals should not be null");
      assertNotNull(metadata.getMemories(), "Memories should not be null");
      assertNotNull(metadata.getTables(), "Tables should not be null");
      assertNotNull(metadata.getImportedModules(), "Imports should not be null");
      assertNotNull(metadata.getCustomSections(), "Custom sections should not be null");
    }
  }

  @Nested
  @DisplayName("FunctionSignature Tests")
  class FunctionSignatureTests {

    @Test
    @DisplayName("should create FunctionSignature with all fields")
    void shouldCreateFunctionSignatureWithAllFields() {
      final MetadataCache.FunctionSignature sig =
          new MetadataCache.FunctionSignature(
              "add", new String[] {"i32", "i32"}, new String[] {"i32"}, false);

      assertEquals("add", sig.getName(), "Name should match");
      assertArrayEquals(
          new String[] {"i32", "i32"}, sig.getParameterTypes(), "Params should match");
      assertArrayEquals(new String[] {"i32"}, sig.getReturnTypes(), "Returns should match");
      assertFalse(sig.isHostFunction(), "Should not be host function");
    }

    @Test
    @DisplayName("should handle null arrays in FunctionSignature")
    void shouldHandleNullArraysInFunctionSignature() {
      final MetadataCache.FunctionSignature sig =
          new MetadataCache.FunctionSignature("empty", null, null, true);

      assertArrayEquals(new String[0], sig.getParameterTypes(), "Params should be empty array");
      assertArrayEquals(new String[0], sig.getReturnTypes(), "Returns should be empty array");
      assertTrue(sig.isHostFunction(), "Should be host function");
    }
  }

  @Nested
  @DisplayName("GlobalMetadata Tests")
  class GlobalMetadataTests {

    @Test
    @DisplayName("should create GlobalMetadata with all fields")
    void shouldCreateGlobalMetadataWithAllFields() {
      final MetadataCache.GlobalMetadata global =
          new MetadataCache.GlobalMetadata("stack_pointer", "i32", true, 65536);

      assertEquals("stack_pointer", global.getName(), "Name should match");
      assertEquals("i32", global.getType(), "Type should match");
      assertTrue(global.isMutable(), "Should be mutable");
      assertEquals(65536, global.getInitialValue(), "Initial value should match");
    }
  }

  @Nested
  @DisplayName("MemoryMetadata Tests")
  class MemoryMetadataTests {

    @Test
    @DisplayName("should create MemoryMetadata with all fields")
    void shouldCreateMemoryMetadataWithAllFields() {
      final MetadataCache.MemoryMetadata memory =
          new MetadataCache.MemoryMetadata("main", 1, 256, true);

      assertEquals("main", memory.getName(), "Name should match");
      assertEquals(1, memory.getMinPages(), "Min pages should match");
      assertEquals(256, memory.getMaxPages(), "Max pages should match");
      assertTrue(memory.isShared(), "Should be shared");
    }
  }

  @Nested
  @DisplayName("TableMetadata Tests")
  class TableMetadataTests {

    @Test
    @DisplayName("should create TableMetadata with all fields")
    void shouldCreateTableMetadataWithAllFields() {
      final MetadataCache.TableMetadata table =
          new MetadataCache.TableMetadata("indirect_table", "funcref", 10, 1000);

      assertEquals("indirect_table", table.getName(), "Name should match");
      assertEquals("funcref", table.getElementType(), "Element type should match");
      assertEquals(10, table.getMinSize(), "Min size should match");
      assertEquals(1000, table.getMaxSize(), "Max size should match");
    }
  }

  @Nested
  @DisplayName("TypeValidationResult Tests")
  class TypeValidationResultTests {

    @Test
    @DisplayName("should create valid TypeValidationResult")
    void shouldCreateValidTypeValidationResult() {
      final MetadataCache.TypeValidationResult result =
          new MetadataCache.TypeValidationResult(true, null, new String[] {"warning1"}, 50);

      assertTrue(result.isValid(), "Should be valid");
      assertEquals(0, result.getErrors().length, "Should have no errors");
      assertEquals(1, result.getWarnings().length, "Should have 1 warning");
      assertEquals(50, result.getValidationTimeMs(), "Validation time should match");
    }

    @Test
    @DisplayName("should create invalid TypeValidationResult")
    void shouldCreateInvalidTypeValidationResult() {
      final MetadataCache.TypeValidationResult result =
          new MetadataCache.TypeValidationResult(
              false, new String[] {"error1", "error2"}, null, 100);

      assertFalse(result.isValid(), "Should be invalid");
      assertEquals(2, result.getErrors().length, "Should have 2 errors");
      assertEquals(0, result.getWarnings().length, "Should have no warnings");
    }
  }
}
