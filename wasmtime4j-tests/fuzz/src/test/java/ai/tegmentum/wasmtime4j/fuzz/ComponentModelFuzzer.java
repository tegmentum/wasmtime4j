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

package ai.tegmentum.wasmtime4j.fuzz;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

/**
 * Fuzz tests for WebAssembly Component Model operations.
 *
 * <p>This fuzzer tests the robustness of Component Model operations including:
 *
 * <ul>
 *   <li>Component/module instantiation with fuzzed bytecode
 *   <li>Module linking and dependency resolution
 *   <li>Module export inspection
 * </ul>
 *
 * <p>Note: Component Model support may vary by engine. This fuzzer focuses on core module
 * operations that are universally available.
 *
 * @since 1.0.0
 */
public class ComponentModelFuzzer {

  /**
   * Fuzz test for component/module instantiation with fuzzed bytes.
   *
   * <p>This test feeds arbitrary byte sequences to the module loader. The module loader should
   * handle all inputs gracefully without crashing.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzComponentInstantiation(final FuzzedDataProvider data) {
    final byte[] moduleBytes = data.consumeRemainingAsBytes();

    try (Engine engine = Engine.create()) {
      // Try to compile arbitrary bytes as a module
      try (Module module = engine.compileModule(moduleBytes)) {
        // If compilation succeeded, try to get some metadata
        module.getExports();
        module.getImports();
      }
    } catch (WasmException e) {
      // Expected for invalid module bytes
    } catch (IllegalArgumentException e) {
      // Expected for null/empty input
    } catch (Exception e) {
      // Unexpected exception - might indicate a bug
      throw e;
    }
  }

  /**
   * Fuzz test for module linking with fuzzed export names.
   *
   * <p>This test checks module exports with fuzzed export names. The runtime should handle invalid
   * export names gracefully.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzComponentLinking(final FuzzedDataProvider data) {
    final String exportName = data.consumeString(200);

    try (Engine engine = Engine.create()) {
      // Create a minimal valid module
      final byte[] minimalModule = createMinimalModuleBytes();

      try (Module module = engine.compileModule(minimalModule)) {
        // Check if the fuzzed export name exists (it almost certainly won't)
        final var exports = module.getExports();
        final var imports = module.getImports();

        // These should never be null
        if (exports == null || imports == null) {
          throw new AssertionError("Export/Import lists should not be null");
        }

        // Check if any export matches the fuzzed name
        boolean found = false;
        for (var export : exports) {
          if (export.getName().equals(exportName)) {
            found = true;
            break;
          }
        }
        // Whether found or not is fine - we're testing for crashes
      }
    } catch (WasmException e) {
      // Expected for various module errors
    } catch (IllegalArgumentException e) {
      // Expected for null/invalid inputs
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for module export inspection.
   *
   * <p>This test inspects module exports and metadata. The runtime should handle all queries
   * gracefully.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzComponentExports(final FuzzedDataProvider data) {
    final boolean inspectExports = data.consumeBoolean();
    final boolean inspectImports = data.consumeBoolean();

    try (Engine engine = Engine.create()) {
      // Try to create and inspect a module
      final byte[] moduleBytes = createMinimalModuleBytes();

      try (Module module = engine.compileModule(moduleBytes)) {
        // Get exports
        if (inspectExports) {
          final var exports = module.getExports();
          if (exports != null) {
            for (var export : exports) {
              // Access export properties to ensure they work
              export.getName();
              export.getType();
            }
          }
        }

        // Get imports
        if (inspectImports) {
          final var imports = module.getImports();
          if (imports != null) {
            for (var imp : imports) {
              // Access import properties to ensure they work
              imp.getModuleName();
              imp.getName();
              imp.getType();
            }
          }
        }
      }
    } catch (WasmException e) {
      // Expected for various module errors
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Creates minimal module bytes for testing.
   *
   * <p>This returns a valid minimal WebAssembly module binary.
   *
   * @return minimal module bytes
   */
  private byte[] createMinimalModuleBytes() {
    // A minimal valid WebAssembly module:
    // Magic number: 0x00 0x61 0x73 0x6D ("\0asm")
    // Version: 0x01 0x00 0x00 0x00 (version 1)
    return new byte[] {
      0x00, 0x61, 0x73, 0x6D, // magic: \0asm
      0x01, 0x00, 0x00, 0x00 // version 1
    };
  }
}
