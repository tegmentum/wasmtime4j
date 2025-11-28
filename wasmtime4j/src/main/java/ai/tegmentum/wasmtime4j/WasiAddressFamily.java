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
 * WASI Preview 2 Address Family enumeration.
 *
 * <p>Represents the address family for socket operations.
 *
 * <p>This enum maps to the wasi:sockets/network@0.2.0 ip-address-family type.
 *
 * @since 1.0.0
 */
public enum WasiAddressFamily {

  /** IPv4 address family. */
  IPV4(0),

  /** IPv6 address family. */
  IPV6(1);

  private final int value;

  WasiAddressFamily(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this address family.
   *
   * @return the numeric value
   */
  public int getValue() {
    return value;
  }

  /**
   * Converts a numeric value to an address family.
   *
   * @param value the numeric value
   * @return the corresponding address family
   * @throws IllegalArgumentException if the value is not recognized
   */
  public static WasiAddressFamily fromValue(final int value) {
    for (WasiAddressFamily family : values()) {
      if (family.value == value) {
        return family;
      }
    }
    throw new IllegalArgumentException("Unknown address family value: " + value);
  }
}
