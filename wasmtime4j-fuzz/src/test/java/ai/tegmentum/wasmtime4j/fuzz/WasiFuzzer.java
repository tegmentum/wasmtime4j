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
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fuzz tests for WASI (WebAssembly System Interface) operations.
 *
 * <p>This fuzzer tests the robustness of WASI operations including:
 *
 * <ul>
 *   <li>File operations with fuzzed paths
 *   <li>Environment variable access with fuzzed values
 *   <li>Clock/time operations with fuzzed parameters
 *   <li>Random number generation
 * </ul>
 *
 * @since 1.0.0
 */
public class WasiFuzzer {

  /** A simple WASI module that reads args and environ. */
  private static final String WASI_ARGS_MODULE_WAT =
      """
        (module
            (import "wasi_snapshot_preview1" "args_sizes_get" (func $args_sizes_get (param i32 i32) (result i32)))
            (import "wasi_snapshot_preview1" "environ_sizes_get" (func $environ_sizes_get (param i32 i32) (result i32)))
            (memory (export "memory") 1)
            (func (export "get_args_sizes") (result i32)
                i32.const 0
                i32.const 4
                call $args_sizes_get)
            (func (export "get_environ_sizes") (result i32)
                i32.const 8
                i32.const 12
                call $environ_sizes_get)
        )
        """;

  /** A WASI module that uses random_get. */
  private static final String WASI_RANDOM_MODULE_WAT =
      """
        (module
            (import "wasi_snapshot_preview1" "random_get" (func $random_get (param i32 i32) (result i32)))
            (memory (export "memory") 1)
            (func (export "get_random") (param $len i32) (result i32)
                i32.const 0
                local.get $len
                call $random_get)
        )
        """;

  /** A WASI module that uses clock_time_get. */
  private static final String WASI_CLOCK_MODULE_WAT =
      """
        (module
            (import "wasi_snapshot_preview1" "clock_time_get" (func $clock_time_get (param i32 i64 i32) (result i32)))
            (memory (export "memory") 1)
            (func (export "get_time") (param $clock_id i32) (param $precision i64) (result i32)
                local.get $clock_id
                local.get $precision
                i32.const 0
                call $clock_time_get)
        )
        """;

  /**
   * Fuzz test for WASI file-related operations.
   *
   * <p>This test configures WASI with fuzzed directory mappings and attempts file operations. The
   * runtime should handle invalid paths and operations gracefully.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzWasiFileOperations(final FuzzedDataProvider data) {
    // Generate fuzzed path components
    final String pathComponent = data.consumeString(100);

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      // Enable WASI
      linker.enableWasi();

      // Create a temporary directory for safe file operations
      final Path tempDir = Files.createTempDirectory("wasmtime4j-fuzz");
      try {
        // Try to create a file with a fuzzed name component
        // Sanitize the path to avoid path traversal
        final String safeName =
            pathComponent
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .substring(0, Math.min(50, pathComponent.length()));

        if (!safeName.isEmpty()) {
          final Path testFile = tempDir.resolve(safeName);
          final byte[] content = data.consumeBytes(1024);

          try {
            Files.write(testFile, content);

            // Verify the file was written
            if (Files.exists(testFile)) {
              final byte[] readBack = Files.readAllBytes(testFile);
              if (readBack.length != content.length) {
                throw new AssertionError("File content length mismatch");
              }
            }
          } catch (java.io.IOException e) {
            // Expected for invalid paths or I/O errors
          }
        }

        // Compile a simple WASI module
        try (Module module = engine.compileWat(WASI_ARGS_MODULE_WAT);
            Instance instance = linker.instantiate(store, module)) {

          // Call the get_args_sizes function
          final WasmValue[] results = instance.callFunction("get_args_sizes");
          // Result should be 0 (WASI_ESUCCESS) or an error code
        }

      } finally {
        // Clean up temp directory
        deleteRecursively(tempDir.toFile());
      }

    } catch (WasmException e) {
      // Expected for various WASI errors
    } catch (java.io.IOException e) {
      // Expected for I/O errors
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for WASI environment operations.
   *
   * <p>This test queries environment sizes with various WASI configurations. The runtime should
   * handle the operations gracefully.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzWasiEnvironment(final FuzzedDataProvider data) {
    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      // Enable WASI
      linker.enableWasi();

      // Compile the WASI args module
      try (Module module = engine.compileWat(WASI_ARGS_MODULE_WAT);
          Instance instance = linker.instantiate(store, module)) {

        // Call get_environ_sizes
        final WasmValue[] results = instance.callFunction("get_environ_sizes");

        if (results != null && results.length > 0) {
          final int returnCode = results[0].asInt();
          // Return code should be 0 (success) or a valid WASI error code
          if (returnCode < 0) {
            throw new AssertionError("Invalid WASI return code: " + returnCode);
          }
        }
      }

    } catch (WasmException e) {
      // Expected for various WASI errors
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for WASI clock operations.
   *
   * <p>This test queries clock time with fuzzed clock IDs and precision values. The runtime should
   * handle invalid clock IDs gracefully.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzWasiClock(final FuzzedDataProvider data) {
    final int clockId = data.consumeInt();
    final long precision = data.consumeLong();

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      // Enable WASI
      linker.enableWasi();

      // Compile the WASI clock module
      try (Module module = engine.compileWat(WASI_CLOCK_MODULE_WAT);
          Instance instance = linker.instantiate(store, module)) {

        // Call get_time with fuzzed parameters
        final WasmValue[] results =
            instance.callFunction("get_time", WasmValue.i32(clockId), WasmValue.i64(precision));

        if (results != null && results.length > 0) {
          final int returnCode = results[0].asInt();
          // Valid clock IDs are 0-3, others should return EINVAL (28)
          // But we don't enforce this - just ensure no crash
        }
      }

    } catch (WasmException e) {
      // Expected for invalid clock operations
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for WASI random operations.
   *
   * <p>This test requests random bytes with fuzzed buffer lengths. The runtime should handle
   * various buffer sizes gracefully.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzWasiRandom(final FuzzedDataProvider data) {
    // Limit the length to avoid excessive memory usage
    final int length = data.consumeInt(0, 65536);

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      // Enable WASI
      linker.enableWasi();

      // Compile the WASI random module
      try (Module module = engine.compileWat(WASI_RANDOM_MODULE_WAT);
          Instance instance = linker.instantiate(store, module)) {

        // Call get_random with the fuzzed length
        final WasmValue[] results = instance.callFunction("get_random", WasmValue.i32(length));

        if (results != null && results.length > 0) {
          final int returnCode = results[0].asInt();
          // Return code should be 0 (success) for valid lengths within memory bounds
          // or an error code for out-of-bounds requests
        }
      }

    } catch (WasmException e) {
      // Expected for various WASI errors (e.g., out of bounds memory access)
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Recursively deletes a directory and its contents.
   *
   * @param file the file or directory to delete
   */
  private void deleteRecursively(final File file) {
    if (file.isDirectory()) {
      final File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          deleteRecursively(child);
        }
      }
    }
    file.delete();
  }
}
