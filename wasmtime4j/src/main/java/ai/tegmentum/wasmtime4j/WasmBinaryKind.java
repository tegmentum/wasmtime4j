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

/**
 * Indicates the type of source WebAssembly binary.
 *
 * <p>This enum is returned from {@link Engine#detectWasmType(byte[])} to indicate whether the
 * provided bytes represent a WebAssembly module, a WebAssembly component, or an unknown/invalid
 * format.
 *
 * <p>Unlike {@link Precompiled} which identifies precompiled (serialized) artifacts, this enum
 * identifies the type of source WASM bytecode before compilation.
 *
 * <p>Detection is based on the WASM binary format header:
 *
 * <ul>
 *   <li>All WASM binaries start with the magic bytes {@code \0asm}
 *   <li>Modules use version {@code 0x01 0x00 0x00 0x00}
 *   <li>Components use version {@code 0x0d 0x00 0x01 0x00}
 * </ul>
 *
 * @since 1.1.0
 */
public enum WasmBinaryKind {

  /** The bytes represent a standard WebAssembly module. */
  MODULE,

  /** The bytes represent a WebAssembly component (Component Model). */
  COMPONENT,

  /** The bytes do not represent a recognized WebAssembly binary format. */
  UNKNOWN;

  /** WASM magic bytes: {@code \0asm}. */
  private static final byte[] WASM_MAGIC = {0x00, 0x61, 0x73, 0x6D};

  /** Module version bytes: 1.0.0.0. */
  private static final byte[] MODULE_VERSION = {0x01, 0x00, 0x00, 0x00};

  /** Component version bytes: 0x0d 0x00 0x01 0x00. */
  private static final byte[] COMPONENT_VERSION = {0x0d, 0x00, 0x01, 0x00};

  /**
   * Detects the kind of WebAssembly binary from its header bytes.
   *
   * <p>This method inspects the first 8 bytes of the input to determine whether the bytes represent
   * a WebAssembly module, a WebAssembly component, or an unknown format. At least 8 bytes are
   * required for successful detection.
   *
   * @param bytes the bytes to inspect
   * @return the detected binary kind
   * @throws IllegalArgumentException if bytes is null
   */
  public static WasmBinaryKind detect(final byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("bytes cannot be null");
    }
    if (bytes.length < 8) {
      return UNKNOWN;
    }

    // Check magic bytes
    for (int i = 0; i < WASM_MAGIC.length; i++) {
      if (bytes[i] != WASM_MAGIC[i]) {
        return UNKNOWN;
      }
    }

    // Check version bytes to distinguish module vs component
    boolean isModule = true;
    boolean isComponent = true;
    for (int i = 0; i < 4; i++) {
      if (bytes[4 + i] != MODULE_VERSION[i]) {
        isModule = false;
      }
      if (bytes[4 + i] != COMPONENT_VERSION[i]) {
        isComponent = false;
      }
    }

    if (isModule) {
      return MODULE;
    }
    if (isComponent) {
      return COMPONENT;
    }
    return UNKNOWN;
  }
}
