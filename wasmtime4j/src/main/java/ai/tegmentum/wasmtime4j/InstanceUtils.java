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
 * Package-private utility methods for {@link Instance}.
 *
 * <p>Extracted from the Instance interface to maintain Java 8 compatibility, since private static
 * methods in interfaces require Java 9+.
 */
final class InstanceUtils {

  private InstanceUtils() {}

  /**
   * Converts a WasmValueType to its signature character representation.
   *
   * @param type the value type
   * @return the signature character
   */
  static char wasmTypeToSignatureChar(final WasmValueType type) {
    switch (type) {
      case I32:
        return 'i';
      case I64:
        return 'I';
      case F32:
        return 'f';
      case F64:
        return 'F';
      default:
        throw new IllegalArgumentException("Unsupported type for typed functions: " + type);
    }
  }
}
