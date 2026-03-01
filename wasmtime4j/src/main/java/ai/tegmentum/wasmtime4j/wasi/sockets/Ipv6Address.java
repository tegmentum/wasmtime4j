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
 * IPv6 address represented as eight 16-bit segments.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/network@0.2.0
 */
public final class Ipv6Address {
  private final short[] segments;

  /**
   * Creates an IPv6 address from eight 16-bit segments.
   *
   * @param segments array of exactly 8 shorts representing the IPv6 address
   * @throws IllegalArgumentException if segments is null or not exactly 8 shorts
   */
  public Ipv6Address(final short[] segments) {
    if (segments == null) {
      throw new IllegalArgumentException("segments cannot be null");
    }
    if (segments.length != 8) {
      throw new IllegalArgumentException(
          "IPv6 address must be exactly 8 segments, got: " + segments.length);
    }
    this.segments = segments.clone();
  }

  /**
   * Gets the segments of this IPv6 address.
   *
   * @return defensive copy of the 8-segment array
   */
  public short[] getSegments() {
    return segments.clone();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Ipv6Address that = (Ipv6Address) o;
    return Arrays.equals(segments, that.segments);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(segments);
  }

  @Override
  public String toString() {
    return String.format(
        "%x:%x:%x:%x:%x:%x:%x:%x",
        segments[0] & 0xFFFF,
        segments[1] & 0xFFFF,
        segments[2] & 0xFFFF,
        segments[3] & 0xFFFF,
        segments[4] & 0xFFFF,
        segments[5] & 0xFFFF,
        segments[6] & 0xFFFF,
        segments[7] & 0xFFFF);
  }
}
