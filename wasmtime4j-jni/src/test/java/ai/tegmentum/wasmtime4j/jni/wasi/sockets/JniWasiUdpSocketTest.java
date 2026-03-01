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
package ai.tegmentum.wasmtime4j.jni.wasi.sockets;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddressFamily;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link JniWasiUdpSocket}. */
@DisplayName("JniWasiUdpSocket Tests")
class JniWasiUdpSocketTest {

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("create should throw on zero context handle")
    void createShouldThrowOnZeroContextHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> JniWasiUdpSocket.create(0L, IpAddressFamily.IPV4),
          "create should throw IllegalArgumentException on zero context handle");
    }

    @Test
    @DisplayName("create should throw on null address family")
    void createShouldThrowOnNullAddressFamily() {
      assertThrows(
          IllegalArgumentException.class,
          () -> JniWasiUdpSocket.create(1L, null),
          "create should throw IllegalArgumentException on null address family");
    }
  }
}
