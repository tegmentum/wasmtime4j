/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Diagnostic test to verify native library loading and symbol availability.
 *
 * @since 1.0.0
 */
final class NativeLibraryDiagnosticTest {

  @Test
  @DisplayName("Should load native library successfully")
  void testLibraryLoads() {
    final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
    assertNotNull(loader);
    assertTrue(loader.isLoaded(), "Library should be loaded");
  }

  @Test
  @DisplayName("Should find enhanced component engine symbols")
  void testEnhancedComponentSymbols() {
    final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
    final SymbolLookup symbolLookup = loader.getSymbolLookup();
    assertNotNull(symbolLookup);

    System.out.println("Library path: " + loader.getLoadInfo().getExtractedPath());

    // Test each enhanced component engine function
    final String[] functions = {
      "wasmtime4j_panama_enhanced_component_engine_create",
      "wasmtime4j_panama_enhanced_component_engine_destroy",
      "wasmtime4j_panama_enhanced_component_instantiate",
      "wasmtime4j_panama_enhanced_component_invoke",
      "wasmtime4j_panama_enhanced_component_get_exports"
    };

    for (final String functionName : functions) {
      System.out.println("Looking up: " + functionName);
      Optional<MemorySegment> symbol = symbolLookup.find(functionName);

      // Try with underscore prefix if not found (macOS C symbols)
      if (symbol.isEmpty()) {
        System.out.println("  Not found, trying with underscore prefix...");
        symbol = symbolLookup.find("_" + functionName);
      }

      assertTrue(
          symbol.isPresent(),
          "Symbol not found: " + functionName + " (tried with and without underscore prefix)");
      System.out.println("  Found!");
    }
  }

  @Test
  @DisplayName("Should create method handle for enhanced component engine create")
  void testMethodHandleCreation() {
    final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
    final FunctionDescriptor descriptor = FunctionDescriptor.of(ValueLayout.ADDRESS);

    final Optional<java.lang.invoke.MethodHandle> methodHandle =
        loader.lookupFunction("wasmtime4j_panama_enhanced_component_engine_create", descriptor);

    assertTrue(
        methodHandle.isPresent(),
        "Method handle creation failed for wasmtime4j_panama_enhanced_component_engine_create");
  }
}
