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
 * IP protocol version selection for WASI sockets.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/network@0.2.0
 */
public enum IpAddressFamily {
  /** Internet Protocol version 4 (AF_INET equivalent). */
  IPV4,

  /** Internet Protocol version 6 (AF_INET6 equivalent). */
  IPV6
}
