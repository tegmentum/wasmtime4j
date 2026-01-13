package ai.tegmentum.wasmtime4j.fuzz;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;

/**
 * Fuzz tests for WebAssembly module loading.
 *
 * <p>This fuzzer tests the robustness of WASM/WAT module parsing by feeding arbitrary byte
 * sequences to the module loader. It aims to discover:
 *
 * <ul>
 *   <li>Malformed magic bytes handling
 *   <li>Invalid section length handling
 *   <li>Type index out of bounds
 *   <li>Circular type references
 *   <li>Maximum limits exceeded
 *   <li>Memory safety issues in parsing code
 * </ul>
 *
 * @since 1.0.0
 */
public class ModuleLoadFuzzer {

  /**
   * Fuzz test for loading WASM modules from arbitrary bytes.
   *
   * <p>This test feeds arbitrary byte sequences to the module loader. The module loader should
   * handle all inputs gracefully without crashing.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzModuleFromBytes(final FuzzedDataProvider data) {
    final byte[] wasmBytes = data.consumeRemainingAsBytes();

    try (Engine engine = Engine.create()) {
      // Try to compile arbitrary bytes as a WASM module
      // This should either succeed or throw WasmException, never crash
      try (Module module = engine.compileModule(wasmBytes)) {
        // If compilation succeeded, the module is valid
        // We can optionally inspect it
        module.getExports();
        module.getImports();
      }
    } catch (WasmException e) {
      // Expected for invalid modules - this is fine
    } catch (IllegalArgumentException e) {
      // Expected for null/empty input - this is fine
    } catch (Exception e) {
      // Unexpected exception - might indicate a bug
      // Jazzer will capture this as a finding
      throw e;
    }
  }

  /**
   * Fuzz test for loading WASM modules from WAT text format.
   *
   * <p>This test feeds arbitrary strings to the WAT parser. The parser should handle all inputs
   * gracefully.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzModuleFromWat(final FuzzedDataProvider data) {
    final String watText = data.consumeRemainingAsString();

    // Skip empty strings as they would just throw IllegalArgumentException
    if (watText == null || watText.isEmpty()) {
      return;
    }

    try (Engine engine = Engine.create()) {
      // Try to compile arbitrary text as WAT
      try (Module module = engine.compileWat(watText)) {
        // If compilation succeeded, inspect the module
        module.getExports();
        module.getImports();
      }
    } catch (WasmException e) {
      // Expected for invalid WAT - this is fine
    } catch (IllegalArgumentException e) {
      // Expected for null/empty input - this is fine
    } catch (Exception e) {
      // Unexpected exception - might indicate a bug
      throw e;
    }
  }

  /**
   * Fuzz test for module validation without full compilation.
   *
   * <p>Tests the validation path separately from full compilation to ensure validation handles
   * malformed modules correctly.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzModuleValidation(final FuzzedDataProvider data) {
    final byte[] wasmBytes = data.consumeRemainingAsBytes();

    try (Engine engine = Engine.create()) {
      // Try to compile - validation happens implicitly during compilation
      // The compile should never crash on any input, only throw exceptions
      try (Module module = engine.compileModule(wasmBytes)) {
        // If compilation succeeded, the module is valid
        module.getExports();
        module.getImports();
      }
    } catch (WasmException e) {
      // Expected - validation or compilation failed
    } catch (IllegalArgumentException e) {
      // Expected for null/empty input
    } catch (Exception e) {
      // Unexpected exception
      throw e;
    }
  }

  /**
   * Fuzz test for module precompilation (AOT serialization).
   *
   * <p>Tests the precompilation path which serializes compiled modules.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzModulePrecompilation(final FuzzedDataProvider data) {
    final byte[] wasmBytes = data.consumeRemainingAsBytes();

    try (Engine engine = Engine.create()) {
      // Try to precompile arbitrary bytes
      final byte[] precompiled = engine.precompileModule(wasmBytes);

      // If precompilation succeeded, try to deserialize
      if (precompiled != null && precompiled.length > 0) {
        try (Module module = Module.deserialize(engine, precompiled)) {
          module.getExports();
        }
      }
    } catch (WasmException e) {
      // Expected for invalid modules
    } catch (IllegalArgumentException e) {
      // Expected for null/empty input
    } catch (Exception e) {
      // Unexpected exception
      throw e;
    }
  }
}
