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

package ai.tegmentum.wasmtime4j.panama.ffi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasmtimeBindings} class.
 *
 * <p>This test class verifies the Panama FFI bindings functionality for the Wasmtime library.
 */
@DisplayName("WasmtimeBindings Tests")
class WasmtimeBindingsTest {

  private SymbolLookup mockSymbolLookup;
  private WasmtimeBindings bindings;

  @BeforeEach
  void setUp() {
    // Use a mock symbol lookup that returns empty for all lookups
    mockSymbolLookup =
        name -> {
          // Return empty for all symbols - we're testing the binding structure, not actual calls
          return Optional.empty();
        };
    bindings = new WasmtimeBindings(mockSymbolLookup);
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasmtimeBindings should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasmtimeBindings.class.getModifiers()),
          "WasmtimeBindings should be final");
    }

    @Test
    @DisplayName("WASMTIME_ENGINE_LAYOUT should be defined")
    void wasmtimeEngineLayoutShouldBeDefined() {
      assertNotNull(
          WasmtimeBindings.WASMTIME_ENGINE_LAYOUT, "WASMTIME_ENGINE_LAYOUT should be defined");
      assertEquals(
          "wasmtime_engine_t",
          WasmtimeBindings.WASMTIME_ENGINE_LAYOUT.name().orElse(""),
          "Layout should have correct name");
    }

    @Test
    @DisplayName("WASMTIME_MODULE_LAYOUT should be defined")
    void wasmtimeModuleLayoutShouldBeDefined() {
      assertNotNull(
          WasmtimeBindings.WASMTIME_MODULE_LAYOUT, "WASMTIME_MODULE_LAYOUT should be defined");
      assertEquals(
          "wasmtime_module_t",
          WasmtimeBindings.WASMTIME_MODULE_LAYOUT.name().orElse(""),
          "Layout should have correct name");
    }

    @Test
    @DisplayName("WASMTIME_INSTANCE_LAYOUT should be defined")
    void wasmtimeInstanceLayoutShouldBeDefined() {
      assertNotNull(
          WasmtimeBindings.WASMTIME_INSTANCE_LAYOUT, "WASMTIME_INSTANCE_LAYOUT should be defined");
      assertEquals(
          "wasmtime_instance_t",
          WasmtimeBindings.WASMTIME_INSTANCE_LAYOUT.name().orElse(""),
          "Layout should have correct name");
    }

    @Test
    @DisplayName("WASMTIME_MEMORY_LAYOUT should be defined")
    void wasmtimeMemoryLayoutShouldBeDefined() {
      assertNotNull(
          WasmtimeBindings.WASMTIME_MEMORY_LAYOUT, "WASMTIME_MEMORY_LAYOUT should be defined");
      assertEquals(
          "wasmtime_memory_t",
          WasmtimeBindings.WASMTIME_MEMORY_LAYOUT.name().orElse(""),
          "Layout should have correct name");
    }

    @Test
    @DisplayName("WASMTIME_TABLE_LAYOUT should be defined")
    void wasmtimeTableLayoutShouldBeDefined() {
      assertNotNull(
          WasmtimeBindings.WASMTIME_TABLE_LAYOUT, "WASMTIME_TABLE_LAYOUT should be defined");
      assertEquals(
          "wasmtime_table_t",
          WasmtimeBindings.WASMTIME_TABLE_LAYOUT.name().orElse(""),
          "Layout should have correct name");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid SymbolLookup")
    void constructorShouldAcceptValidSymbolLookup() {
      final WasmtimeBindings testBindings = new WasmtimeBindings(mockSymbolLookup);

      assertNotNull(testBindings, "Bindings should be created");
      assertSame(
          mockSymbolLookup, testBindings.getSymbolLookup(), "Should store the symbol lookup");
    }

    @Test
    @DisplayName("Constructor should throw for null SymbolLookup")
    void constructorShouldThrowForNullSymbolLookup() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasmtimeBindings(null),
          "Should throw for null SymbolLookup");
    }

    @Test
    @DisplayName("Constructor should initialize linker")
    void constructorShouldInitializeLinker() {
      final WasmtimeBindings testBindings = new WasmtimeBindings(mockSymbolLookup);

      assertNotNull(testBindings.getLinker(), "Linker should be initialized");
    }

    @Test
    @DisplayName("Constructor should start with empty cache")
    void constructorShouldStartWithEmptyCache() {
      final WasmtimeBindings testBindings = new WasmtimeBindings(mockSymbolLookup);

      assertEquals(0, testBindings.getCacheSize(), "Cache should be empty initially");
    }
  }

  @Nested
  @DisplayName("getMethodHandle Tests")
  class GetMethodHandleTests {

    @Test
    @DisplayName("getMethodHandle should throw for null function name")
    void getMethodHandleShouldThrowForNullFunctionName() {
      final FunctionDescriptor descriptor = FunctionDescriptor.of(ValueLayout.ADDRESS);

      assertThrows(
          IllegalArgumentException.class,
          () -> bindings.getMethodHandle(null, descriptor),
          "Should throw for null function name");
    }

    @Test
    @DisplayName("getMethodHandle should throw for empty function name")
    void getMethodHandleShouldThrowForEmptyFunctionName() {
      final FunctionDescriptor descriptor = FunctionDescriptor.of(ValueLayout.ADDRESS);

      assertThrows(
          IllegalArgumentException.class,
          () -> bindings.getMethodHandle("", descriptor),
          "Should throw for empty function name");
    }

    @Test
    @DisplayName("getMethodHandle should throw for null descriptor")
    void getMethodHandleShouldThrowForNullDescriptor() {
      assertThrows(
          IllegalArgumentException.class,
          () -> bindings.getMethodHandle("test_function", null),
          "Should throw for null descriptor");
    }

    @Test
    @DisplayName("getMethodHandle should return null for non-existent symbol")
    void getMethodHandleShouldReturnNullForNonExistentSymbol() {
      final FunctionDescriptor descriptor = FunctionDescriptor.of(ValueLayout.ADDRESS);

      final MethodHandle handle = bindings.getMethodHandle("non_existent_function", descriptor);

      assertNull(handle, "Should return null for non-existent symbol");
    }

    @Test
    @DisplayName("getMethodHandle should cache null results")
    void getMethodHandleShouldCacheNullResults() {
      final FunctionDescriptor descriptor = FunctionDescriptor.of(ValueLayout.ADDRESS);

      // First call
      bindings.getMethodHandle("test_function", descriptor);
      final int cacheSize = bindings.getCacheSize();

      // Second call with same function
      bindings.getMethodHandle("test_function", descriptor);

      assertEquals(
          cacheSize, bindings.getCacheSize(), "Cache size should not change on second call");
    }

    @Test
    @DisplayName("getMethodHandle should use different cache entries for different functions")
    void getMethodHandleShouldUseDifferentCacheEntriesForDifferentFunctions() {
      final FunctionDescriptor descriptor = FunctionDescriptor.of(ValueLayout.ADDRESS);

      bindings.getMethodHandle("function_a", descriptor);
      bindings.getMethodHandle("function_b", descriptor);

      assertEquals(2, bindings.getCacheSize(), "Should have 2 cache entries");
    }
  }

  @Nested
  @DisplayName("Engine Method Handle Tests")
  class EngineMethodHandleTests {

    @Test
    @DisplayName("wasmtimeEngineNew should return null when symbol not found")
    void wasmtimeEngineNewShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeEngineNew();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtimeEngineDelete should return null when symbol not found")
    void wasmtimeEngineDeleteShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeEngineDelete();

      assertNull(handle, "Should return null when symbol not found");
    }
  }

  @Nested
  @DisplayName("Module Method Handle Tests")
  class ModuleMethodHandleTests {

    @Test
    @DisplayName("wasmtimeModuleNew should return null when symbol not found")
    void wasmtimeModuleNewShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeModuleNew();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtimeModuleDelete should return null when symbol not found")
    void wasmtimeModuleDeleteShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeModuleDelete();

      assertNull(handle, "Should return null when symbol not found");
    }
  }

  @Nested
  @DisplayName("Instance Method Handle Tests")
  class InstanceMethodHandleTests {

    @Test
    @DisplayName("wasmtimeInstanceNew should return null when symbol not found")
    void wasmtimeInstanceNewShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeInstanceNew();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtimeInstanceDelete should return null when symbol not found")
    void wasmtimeInstanceDeleteShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeInstanceDelete();

      assertNull(handle, "Should return null when symbol not found");
    }
  }

  @Nested
  @DisplayName("Memory Method Handle Tests")
  class MemoryMethodHandleTests {

    @Test
    @DisplayName("wasmtimeMemoryNew should return null when symbol not found")
    void wasmtimeMemoryNewShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeMemoryNew();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtimeMemorySize should return null when symbol not found")
    void wasmtimeMemorySizeShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeMemorySize();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtimeMemoryGrow should return null when symbol not found")
    void wasmtimeMemoryGrowShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeMemoryGrow();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtimeMemoryData should return null when symbol not found")
    void wasmtimeMemoryDataShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeMemoryData();

      assertNull(handle, "Should return null when symbol not found");
    }
  }

  @Nested
  @DisplayName("Table Method Handle Tests")
  class TableMethodHandleTests {

    @Test
    @DisplayName("wasmtimeTableNew should return null when symbol not found")
    void wasmtimeTableNewShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeTableNew();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtimeTableDelete should return null when symbol not found")
    void wasmtimeTableDeleteShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeTableDelete();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtimeTableSize should return null when symbol not found")
    void wasmtimeTableSizeShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeTableSize();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtimeTableGet should return null when symbol not found")
    void wasmtimeTableGetShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeTableGet();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtimeTableSet should return null when symbol not found")
    void wasmtimeTableSetShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeTableSet();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtimeTableGrow should return null when symbol not found")
    void wasmtimeTableGrowShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtimeTableGrow();

      assertNull(handle, "Should return null when symbol not found");
    }
  }

  @Nested
  @DisplayName("WASI Preview 2 Method Handle Tests")
  class WasiPreview2MethodHandleTests {

    @Test
    @DisplayName("wasiPreview2ContextNew should return null when symbol not found")
    void wasiPreview2ContextNewShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasiPreview2ContextNew();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasiPreview2ContextDestroy should return null when symbol not found")
    void wasiPreview2ContextDestroyShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasiPreview2ContextDestroy();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasiPreview2CompileComponent should return null when symbol not found")
    void wasiPreview2CompileComponentShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasiPreview2CompileComponent();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasiPreview2InstantiateComponent should return null when symbol not found")
    void wasiPreview2InstantiateComponentShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasiPreview2InstantiateComponent();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasiPreview2CreateInputStream should return null when symbol not found")
    void wasiPreview2CreateInputStreamShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasiPreview2CreateInputStream();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasiPreview2CreateOutputStream should return null when symbol not found")
    void wasiPreview2CreateOutputStreamShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasiPreview2CreateOutputStream();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasiPreview2StreamRead should return null when symbol not found")
    void wasiPreview2StreamReadShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasiPreview2StreamRead();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasiPreview2StreamWrite should return null when symbol not found")
    void wasiPreview2StreamWriteShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasiPreview2StreamWrite();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasiPreview2CloseStream should return null when symbol not found")
    void wasiPreview2CloseStreamShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasiPreview2CloseStream();

      assertNull(handle, "Should return null when symbol not found");
    }
  }

  @Nested
  @DisplayName("Component Model Method Handle Tests")
  class ComponentModelMethodHandleTests {

    @Test
    @DisplayName("wasmtime4jComponentEngineNew should return null when symbol not found")
    void wasmtime4jComponentEngineNewShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtime4jComponentEngineNew();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtime4jComponentEngineDestroy should return null when symbol not found")
    void wasmtime4jComponentEngineDestroyShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtime4jComponentEngineDestroy();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtime4jComponentCompile should return null when symbol not found")
    void wasmtime4jComponentCompileShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtime4jComponentCompile();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtime4jComponentCompileWat should return null when symbol not found")
    void wasmtime4jComponentCompileWatShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtime4jComponentCompileWat();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtime4jComponentInstantiate should return null when symbol not found")
    void wasmtime4jComponentInstantiateShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtime4jComponentInstantiate();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtime4jComponentExportCount should return null when symbol not found")
    void wasmtime4jComponentExportCountShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtime4jComponentExportCount();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtime4jComponentHasExport should return null when symbol not found")
    void wasmtime4jComponentHasExportShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtime4jComponentHasExport();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtime4jComponentValidate should return null when symbol not found")
    void wasmtime4jComponentValidateShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtime4jComponentValidate();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtime4jWitParserNew should return null when symbol not found")
    void wasmtime4jWitParserNewShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtime4jWitParserNew();

      assertNull(handle, "Should return null when symbol not found");
    }

    @Test
    @DisplayName("wasmtime4jWitParserValidateSyntax should return null when symbol not found")
    void wasmtime4jWitParserValidateSyntaxShouldReturnNullWhenSymbolNotFound() {
      final MethodHandle handle = bindings.wasmtime4jWitParserValidateSyntax();

      assertNull(handle, "Should return null when symbol not found");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getSymbolLookup should return the symbol lookup")
    void getSymbolLookupShouldReturnTheSymbolLookup() {
      assertSame(mockSymbolLookup, bindings.getSymbolLookup(), "Should return same symbol lookup");
    }

    @Test
    @DisplayName("getLinker should return a valid linker")
    void getLinkerShouldReturnValidLinker() {
      final Linker linker = bindings.getLinker();

      assertNotNull(linker, "Linker should not be null");
    }
  }

  @Nested
  @DisplayName("Cache Management Tests")
  class CacheManagementTests {

    @Test
    @DisplayName("clearCache should reset cache size to zero")
    void clearCacheShouldResetCacheSizeToZero() {
      // Add some entries to cache
      bindings.wasmtimeEngineNew();
      bindings.wasmtimeModuleNew();
      assertTrue(bindings.getCacheSize() > 0, "Cache should have entries");

      bindings.clearCache();

      assertEquals(0, bindings.getCacheSize(), "Cache should be empty after clear");
    }

    @Test
    @DisplayName("clearCache should be idempotent")
    void clearCacheShouldBeIdempotent() {
      bindings.clearCache();
      assertDoesNotThrow(bindings::clearCache, "Multiple clears should not throw");
      assertEquals(0, bindings.getCacheSize(), "Cache should remain empty");
    }

    @Test
    @DisplayName("getCacheSize should return correct count")
    void getCacheSizeShouldReturnCorrectCount() {
      assertEquals(0, bindings.getCacheSize(), "Initial cache size should be 0");

      bindings.wasmtimeEngineNew();
      assertEquals(1, bindings.getCacheSize(), "Cache size should be 1");

      bindings.wasmtimeModuleNew();
      assertEquals(2, bindings.getCacheSize(), "Cache size should be 2");

      bindings.wasmtimeInstanceNew();
      assertEquals(3, bindings.getCacheSize(), "Cache size should be 3");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Full binding lifecycle should work correctly")
    void fullBindingLifecycleShouldWorkCorrectly() {
      // Create bindings
      final WasmtimeBindings testBindings = new WasmtimeBindings(mockSymbolLookup);
      assertNotNull(testBindings, "Bindings should be created");

      // Get accessors
      assertNotNull(testBindings.getSymbolLookup(), "Symbol lookup should be accessible");
      assertNotNull(testBindings.getLinker(), "Linker should be accessible");

      // Try to get various method handles (all should be null since mock returns empty)
      assertNull(testBindings.wasmtimeEngineNew(), "Engine new should be null");
      assertNull(testBindings.wasmtimeModuleNew(), "Module new should be null");
      assertNull(testBindings.wasmtimeInstanceNew(), "Instance new should be null");

      // Check cache was populated
      assertTrue(testBindings.getCacheSize() > 0, "Cache should have entries");

      // Clear cache
      testBindings.clearCache();
      assertEquals(0, testBindings.getCacheSize(), "Cache should be empty after clear");
    }

    @Test
    @DisplayName("Multiple method calls should be cached")
    void multipleMethodCallsShouldBeCached() {
      // First round of calls
      bindings.wasmtimeEngineNew();
      bindings.wasmtimeEngineDelete();
      bindings.wasmtimeModuleNew();

      final int firstCacheSize = bindings.getCacheSize();

      // Second round of same calls
      bindings.wasmtimeEngineNew();
      bindings.wasmtimeEngineDelete();
      bindings.wasmtimeModuleNew();

      assertEquals(firstCacheSize, bindings.getCacheSize(), "Cache size should not change");
    }
  }
}
