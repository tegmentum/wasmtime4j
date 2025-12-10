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

/**
 * IP name lookup service for DNS resolution.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/ip-name-lookup@0.2.0
 *
 * <p>This interface provides DNS name resolution capabilities, allowing WebAssembly modules to
 * resolve hostnames to IP addresses. The resolution is performed asynchronously and results are
 * returned via a {@link ResolveAddressStream}.
 *
 * @since 1.0.0
 */
public interface WasiIpNameLookup {

  /**
   * Resolves a hostname to a stream of IP addresses.
   *
   * <p>This method initiates a DNS query to resolve the given hostname. The result is a stream that
   * yields IP addresses as they become available. The stream may contain both IPv4 and IPv6
   * addresses depending on the DNS records and the address family hint provided.
   *
   * <p>If the hostname is already an IP address literal, it will be parsed and returned directly
   * without performing a DNS query.
   *
   * @param network the network context to use for the resolution
   * @param name the hostname to resolve (e.g., "example.com")
   * @return a stream of resolved IP addresses
   * @throws WasmException if the resolution cannot be initiated, including:
   *     <ul>
   *       <li>{@link NetworkErrorCode#INVALID_ARGUMENT} - if the name is malformed
   *       <li>{@link NetworkErrorCode#NAME_UNRESOLVABLE} - if the name cannot be resolved
   *       <li>{@link NetworkErrorCode#TEMPORARY_RESOLVER_FAILURE} - temporary DNS failure
   *       <li>{@link NetworkErrorCode#PERMANENT_RESOLVER_FAILURE} - permanent DNS failure
   *     </ul>
   *
   * @throws IllegalArgumentException if network or name is null
   */
  ResolveAddressStream resolveAddresses(WasiNetwork network, String name) throws WasmException;

  /**
   * Resolves a hostname to a stream of IP addresses with an address family hint.
   *
   * <p>This overload allows specifying a preferred address family to filter the results. For
   * example, specifying {@link IpAddressFamily#IPV4} will only return IPv4 addresses.
   *
   * @param network the network context to use for the resolution
   * @param name the hostname to resolve (e.g., "example.com")
   * @param addressFamily the preferred address family, or null for all families
   * @return a stream of resolved IP addresses filtered by the address family
   * @throws WasmException if the resolution cannot be initiated
   * @throws IllegalArgumentException if network or name is null
   */
  ResolveAddressStream resolveAddresses(
      WasiNetwork network, String name, IpAddressFamily addressFamily) throws WasmException;
}
