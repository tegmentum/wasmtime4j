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
 * Network operation error codes for WASI sockets.
 *
 * <p>Defines specific failure modes for network operations including connection errors, address
 * issues, and resource limitations.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/network@0.2.0
 */
public enum NetworkErrorCode {
  /** An unknown error occurred. */
  UNKNOWN,

  /** Access to the requested resource was denied. */
  ACCESS_DENIED,

  /** The operation is not supported on this platform. */
  NOT_SUPPORTED,

  /** One of the arguments is invalid. */
  INVALID_ARGUMENT,

  /** Not enough memory to complete the operation. */
  OUT_OF_MEMORY,

  /** The operation timed out. */
  TIMEOUT,

  /** This operation is in conflict with another in-progress operation. */
  CONCURRENCY_CONFLICT,

  /** No asynchronous operation is currently in progress. */
  NOT_IN_PROGRESS,

  /** The operation would block but non-blocking mode was requested. */
  WOULD_BLOCK,

  /** The socket is in an invalid state for this operation. */
  INVALID_STATE,

  /** System limit on number of open sockets reached. */
  NEW_SOCKET_LIMIT,

  /** The requested address is not available for binding. */
  ADDRESS_NOT_BINDABLE,

  /** The address is already in use. */
  ADDRESS_IN_USE,

  /** The remote address is unreachable. */
  REMOTE_UNREACHABLE,

  /** Connection was refused by the remote host. */
  CONNECTION_REFUSED,

  /** Connection was reset by the remote host. */
  CONNECTION_RESET,

  /** Connection was aborted. */
  CONNECTION_ABORTED,

  /** The datagram is too large to send. */
  DATAGRAM_TOO_LARGE,

  /** Name could not be resolved (DNS lookup failed). */
  NAME_UNRESOLVABLE,

  /** Temporary name resolution failure (retry may succeed). */
  TEMPORARY_RESOLVER_FAILURE,

  /** Permanent name resolution failure. */
  PERMANENT_RESOLVER_FAILURE
}
