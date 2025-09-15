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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.logging.Logger;

/**
 * Test utilities for Panama-specific WebAssembly operations.
 *
 * <p>This utility class provides helper methods for creating test WebAssembly modules
 * and performing Panama-specific operations in tests.
 */
public final class PanamaTestUtils {

  private static final Logger LOGGER = Logger.getLogger(PanamaTestUtils.class.getName());

  private PanamaTestUtils() {
    // Utility class - prevent instantiation
  }

  /**
   * Checks if Panama FFI is available in the current runtime.
   *
   * @return true if Panama FFI is available, false otherwise
   */
  public static boolean isPanamaAvailable() {
    try {
      Class.forName("java.lang.foreign.Arena");
      Class.forName("java.lang.foreign.MemorySegment");
      return true;
    } catch (ClassNotFoundException e) {
      LOGGER.fine("Panama FFI classes not available: " + e.getMessage());
      return false;
    }
  }

  /**
   * Creates a simple WebAssembly module with a mutable i32 global.
   *
   * @param initialValue the initial value for the global
   * @return WebAssembly module bytes
   */
  public static byte[] createModuleWithMutableI32Global(int initialValue) {
    // This is a minimal WebAssembly module with a mutable i32 global named "test_global"
    // (module
    //   (global $test_global (mut i32) (i32.const {initialValue}))
    //   (export "test_global" (global $test_global))
    // )
    return new byte[] {
        0x00, 0x61, 0x73, 0x6d, // WASM magic number
        0x01, 0x00, 0x00, 0x00, // Version 1
        0x06, 0x07, 0x01, 0x7f, 0x01, 0x41, (byte) initialValue, 0x0b, // Global section
        0x07, 0x10, 0x01, 0x0b, 0x74, 0x65, 0x73, 0x74, 0x5f, 0x67, 0x6c, 0x6f, 0x62, 0x61, 0x6c, 0x03, 0x00
        // Export section
    };
  }

  /**
   * Creates a simple WebAssembly module with an immutable i32 global.
   *
   * @param value the value for the global
   * @return WebAssembly module bytes
   */
  public static byte[] createModuleWithImmutableI32Global(int value) {
    // This is a minimal WebAssembly module with an immutable i32 global named "test_global"
    // (module
    //   (global $test_global i32 (i32.const {value}))
    //   (export "test_global" (global $test_global))
    // )
    return new byte[] {
        0x00, 0x61, 0x73, 0x6d, // WASM magic number
        0x01, 0x00, 0x00, 0x00, // Version 1
        0x06, 0x07, 0x01, 0x7f, 0x00, 0x41, (byte) value, 0x0b, // Global section (immutable)
        0x07, 0x10, 0x01, 0x0b, 0x74, 0x65, 0x73, 0x74, 0x5f, 0x67, 0x6c, 0x6f, 0x62, 0x61, 0x6c, 0x03, 0x00
        // Export section
    };
  }

  /**
   * Creates a WebAssembly module with multiple globals for testing.
   *
   * @return WebAssembly module bytes with multiple globals
   */
  public static byte[] createModuleWithMultipleGlobals() {
    // This is a minimal WebAssembly module with multiple globals
    // (module
    //   (global $global1 (mut i32) (i32.const 1))
    //   (global $global2 (mut i32) (i32.const 2))
    //   (global $global3 i32 (i32.const 3))
    //   (export "global1" (global $global1))
    //   (export "global2" (global $global2))
    //   (export "global3" (global $global3))
    // )
    return new byte[] {
        0x00, 0x61, 0x73, 0x6d, // WASM magic number
        0x01, 0x00, 0x00, 0x00, // Version 1
        0x06, 0x15, 0x03, // Global section with 3 globals
        0x7f, 0x01, 0x41, 0x01, 0x0b, // Global 1: mutable i32, value 1
        0x7f, 0x01, 0x41, 0x02, 0x0b, // Global 2: mutable i32, value 2
        0x7f, 0x00, 0x41, 0x03, 0x0b, // Global 3: immutable i32, value 3
        0x07, 0x20, 0x03, // Export section with 3 exports
        0x07, 0x67, 0x6c, 0x6f, 0x62, 0x61, 0x6c, 0x31, 0x03, 0x00, // Export "global1" -> global 0
        0x07, 0x67, 0x6c, 0x6f, 0x62, 0x61, 0x6c, 0x32, 0x03, 0x01, // Export "global2" -> global 1
        0x07, 0x67, 0x6c, 0x6f, 0x62, 0x61, 0x6c, 0x33, 0x03, 0x02  // Export "global3" -> global 2
    };
  }

  /**
   * Gets a global from a WebAssembly instance by name.
   *
   * @param instance the WebAssembly instance
   * @param globalName the name of the global export
   * @return the global object
   * @throws IllegalArgumentException if the global is not found
   */
  public static PanamaGlobal getGlobalFromInstance(PanamaInstance instance, String globalName) {
    try {
      // Get the global export from the instance
      return (PanamaGlobal) instance.getGlobal(globalName)
          .orElseThrow(() -> new IllegalArgumentException("Global '" + globalName + "' not found"));
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to get global '" + globalName + "': " + e.getMessage(), e);
    }
  }
}