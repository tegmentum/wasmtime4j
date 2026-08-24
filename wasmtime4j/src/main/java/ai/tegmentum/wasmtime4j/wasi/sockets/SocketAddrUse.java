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

/**
 * Describes the intended use of a socket address for use with socket address checks.
 *
 * <p>This enum is passed to {@link SocketAddrCheck} callbacks to indicate what operation is being
 * performed with the socket address, allowing fine-grained control over which network operations
 * are permitted.
 *
 * <p>Constants and their FFI codes are aligned with the upstream
 * {@code wasmtime_wasi::sockets::SocketAddrUse} enum. Wasmtime 48 dropped the standalone
 * {@code UDP_CONNECT} check (UDP connect now piggybacks on {@code UDP_BIND}) and renamed the
 * outgoing-datagram check to {@code UDP_SEND}; a symmetric {@code UDP_RECEIVE} check, plus
 * {@code TCP_LISTEN} and {@code TCP_ACCEPT}, are new.
 *
 * @since 2.0.0
 */
public enum SocketAddrUse {

  /** Binding a TCP socket to a local address. */
  TCP_BIND(0),

  /** Putting a TCP socket into listener mode. */
  TCP_LISTEN(1),

  /** Accepting a new client TCP socket. */
  TCP_ACCEPT(2),

  /** Connecting a TCP socket to a remote address. */
  TCP_CONNECT(3),

  /** Binding a UDP socket to a local address. */
  UDP_BIND(4),

  /** Sending a datagram on a UDP socket. */
  UDP_SEND(5),

  /** Receiving a datagram on a UDP socket. */
  UDP_RECEIVE(6);

  private final int value;

  SocketAddrUse(final int value) {
    this.value = value;
  }

  /**
   * Gets the integer value used for FFI communication.
   *
   * @return the integer value
   */
  public int getValue() {
    return value;
  }

  /**
   * Converts an integer value to the corresponding enum constant.
   *
   * @param value the integer value
   * @return the corresponding enum constant
   * @throws IllegalArgumentException if the value does not correspond to any constant
   */
  public static SocketAddrUse fromValue(final int value) {
    for (final SocketAddrUse use : values()) {
      if (use.value == value) {
        return use;
      }
    }
    throw new IllegalArgumentException("Unknown SocketAddrUse value: " + value);
  }
}
