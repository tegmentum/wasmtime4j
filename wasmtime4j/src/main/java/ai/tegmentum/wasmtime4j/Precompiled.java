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
 * Indicates the type of precompiled artifact detected.
 *
 * <p>This enum is returned from {@link Engine#detectPrecompiled(byte[])} to indicate what kind of
 * precompiled artifact the bytes represent.
 *
 * <p>Precompiled artifacts are produced by methods like {@link Module#serialize()} or {@link
 * Engine#precompileModule(byte[])} and can be loaded faster than compiling from source WebAssembly.
 *
 * @since 1.0.0
 */
public enum Precompiled {

  /**
   * The bytes represent a precompiled WebAssembly module.
   *
   * <p>This artifact was produced by {@link Module#serialize()} or {@link
   * Engine#precompileModule(byte[])}.
   */
  MODULE(0),

  /**
   * The bytes represent a precompiled WebAssembly component.
   *
   * <p>This artifact was produced by a component serialization method.
   */
  COMPONENT(1);

  private final int value;

  Precompiled(final int value) {
    this.value = value;
  }

  /**
   * Gets the native integer value for this precompiled type.
   *
   * @return the native value
   */
  public int getValue() {
    return value;
  }

  /**
   * Creates a Precompiled from its native integer value.
   *
   * @param value the native value
   * @return the corresponding Precompiled, or null for unknown values
   */
  public static Precompiled fromValue(final int value) {
    switch (value) {
      case 0:
        return MODULE;
      case 1:
        return COMPONENT;
      default:
        return null;
    }
  }
}
