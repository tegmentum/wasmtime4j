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

package ai.tegmentum.wasmtime4j;

/**
 * Listener for network-related events during streaming compilation.
 *
 * <p>Implementations can receive notifications about network operations, data transfers, and
 * connection status.
 *
 * @since 1.0.0
 */
public interface NetworkEventListener {

  /**
   * Called when a network connection is established.
   *
   * @param connectionId the connection identifier
   */
  void onConnectionEstablished(String connectionId);

  /**
   * Called when data is received from the network.
   *
   * @param connectionId the connection identifier
   * @param bytesReceived the number of bytes received
   */
  void onDataReceived(String connectionId, long bytesReceived);

  /**
   * Called when data is sent over the network.
   *
   * @param connectionId the connection identifier
   * @param bytesSent the number of bytes sent
   */
  void onDataSent(String connectionId, long bytesSent);

  /**
   * Called when a network connection is closed.
   *
   * @param connectionId the connection identifier
   */
  void onConnectionClosed(String connectionId);

  /**
   * Called when a network error occurs.
   *
   * @param connectionId the connection identifier
   * @param error the error that occurred
   */
  void onError(String connectionId, Throwable error);
}
