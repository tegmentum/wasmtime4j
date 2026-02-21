/*
 * Copyright 2024 Tegmentum AI
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

import java.net.InetSocketAddress;

/**
 * Callback interface for validating socket addresses before use.
 *
 * <p>This callback is invoked for each socket operation to determine whether it should be
 * permitted. Returning {@code true} allows the operation, while returning {@code false}
 * denies it.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SocketAddrCheck check = (addr, use) -> {
 *     // Only allow connections to port 443
 *     return addr.getPort() == 443;
 * };
 *
 * WasiPreview2Config config = WasiPreview2Config.builder()
 *     .socketAddrCheck(check)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface SocketAddrCheck {

  /**
   * Checks whether a socket address operation should be permitted.
   *
   * @param address the socket address being used
   * @param use the intended use of the socket address
   * @return {@code true} to allow the operation, {@code false} to deny it
   */
  boolean check(InetSocketAddress address, SocketAddrUse use);
}
