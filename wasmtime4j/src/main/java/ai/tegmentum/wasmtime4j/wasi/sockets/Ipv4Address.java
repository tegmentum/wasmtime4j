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
package ai.tegmentum.wasmtime4j.wasi.sockets;

import java.util.Arrays;

/**
 * IPv4 address represented as four octets.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/network@0.2.0
 */
public final class Ipv4Address {
  private final byte[] octets;

  /**
   * Creates an IPv4 address from four octets.
   *
   * @param octets array of exactly 4 bytes representing the IPv4 address
   * @throws IllegalArgumentException if octets is null or not exactly 4 bytes
   */
  public Ipv4Address(final byte[] octets) {
    if (octets == null) {
      throw new IllegalArgumentException("octets cannot be null");
    }
    if (octets.length != 4) {
      throw new IllegalArgumentException(
          "IPv4 address must be exactly 4 bytes, got: " + octets.length);
    }
    this.octets = octets.clone();
  }

  /**
   * Gets the octets of this IPv4 address.
   *
   * @return defensive copy of the 4-byte array
   */
  public byte[] getOctets() {
    return octets.clone();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ipv4Address that = (Ipv4Address) o;
    return Arrays.equals(octets, that.octets);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(octets);
  }

  @Override
  public String toString() {
    return String.format(
        "%d.%d.%d.%d", octets[0] & 0xFF, octets[1] & 0xFF, octets[2] & 0xFF, octets[3] & 0xFF);
  }
}
