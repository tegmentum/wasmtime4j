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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;

/**
 * Stream of resolved IP addresses from a DNS lookup.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/ip-name-lookup@0.2.0
 *
 * <p>This resource represents the result stream from a DNS name resolution operation. Each call to
 * {@link #resolveNextAddress()} returns the next resolved IP address, or empty when the stream is
 * exhausted.
 *
 * @since 1.0.0
 */
public interface ResolveAddressStream extends AutoCloseable {

  /**
   * Returns the next resolved IP address from the stream.
   *
   * <p>This method returns the next IP address resolved from the DNS query. When there are no more
   * addresses available, it returns an empty Optional.
   *
   * @return the next IP address, or empty if no more addresses are available
   * @throws WasmException if the resolution fails with a network error
   * @throws IllegalStateException if the stream has been closed
   */
  Optional<IpAddress> resolveNextAddress() throws WasmException;

  /**
   * Subscribes to the stream to wait for more addresses to become available.
   *
   * <p>This method is used for async notification when more addresses may be available. In
   * synchronous contexts, this may be a no-op.
   *
   * @throws WasmException if subscription fails
   * @throws IllegalStateException if the stream has been closed
   */
  void subscribe() throws WasmException;

  /**
   * Checks if the stream has been closed.
   *
   * @return true if the stream is closed, false otherwise
   */
  boolean isClosed();

  /**
   * Closes the address resolution stream and releases associated resources.
   *
   * @throws WasmException if closing fails
   */
  @Override
  void close() throws WasmException;
}
