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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NetworkErrorCode} enum.
 *
 * <p>NetworkErrorCode defines specific failure modes for network operations per WASI Preview 2
 * specification.
 */
@DisplayName("NetworkErrorCode Tests")
class NetworkErrorCodeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NetworkErrorCode.class.isEnum(), "NetworkErrorCode should be an enum");
    }

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      final NetworkErrorCode[] values = NetworkErrorCode.values();
      assertEquals(21, values.length, "Should have 21 error codes");
    }

    @Test
    @DisplayName("should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(NetworkErrorCode.valueOf("UNKNOWN"), "Should have UNKNOWN");
    }

    @Test
    @DisplayName("should have ACCESS_DENIED value")
    void shouldHaveAccessDeniedValue() {
      assertNotNull(NetworkErrorCode.valueOf("ACCESS_DENIED"), "Should have ACCESS_DENIED");
    }

    @Test
    @DisplayName("should have NOT_SUPPORTED value")
    void shouldHaveNotSupportedValue() {
      assertNotNull(NetworkErrorCode.valueOf("NOT_SUPPORTED"), "Should have NOT_SUPPORTED");
    }

    @Test
    @DisplayName("should have INVALID_ARGUMENT value")
    void shouldHaveInvalidArgumentValue() {
      assertNotNull(NetworkErrorCode.valueOf("INVALID_ARGUMENT"), "Should have INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("should have OUT_OF_MEMORY value")
    void shouldHaveOutOfMemoryValue() {
      assertNotNull(NetworkErrorCode.valueOf("OUT_OF_MEMORY"), "Should have OUT_OF_MEMORY");
    }

    @Test
    @DisplayName("should have TIMEOUT value")
    void shouldHaveTimeoutValue() {
      assertNotNull(NetworkErrorCode.valueOf("TIMEOUT"), "Should have TIMEOUT");
    }

    @Test
    @DisplayName("should have CONCURRENCY_CONFLICT value")
    void shouldHaveConcurrencyConflictValue() {
      assertNotNull(
          NetworkErrorCode.valueOf("CONCURRENCY_CONFLICT"), "Should have CONCURRENCY_CONFLICT");
    }

    @Test
    @DisplayName("should have NOT_IN_PROGRESS value")
    void shouldHaveNotInProgressValue() {
      assertNotNull(NetworkErrorCode.valueOf("NOT_IN_PROGRESS"), "Should have NOT_IN_PROGRESS");
    }

    @Test
    @DisplayName("should have WOULD_BLOCK value")
    void shouldHaveWouldBlockValue() {
      assertNotNull(NetworkErrorCode.valueOf("WOULD_BLOCK"), "Should have WOULD_BLOCK");
    }

    @Test
    @DisplayName("should have INVALID_STATE value")
    void shouldHaveInvalidStateValue() {
      assertNotNull(NetworkErrorCode.valueOf("INVALID_STATE"), "Should have INVALID_STATE");
    }

    @Test
    @DisplayName("should have NEW_SOCKET_LIMIT value")
    void shouldHaveNewSocketLimitValue() {
      assertNotNull(NetworkErrorCode.valueOf("NEW_SOCKET_LIMIT"), "Should have NEW_SOCKET_LIMIT");
    }

    @Test
    @DisplayName("should have ADDRESS_NOT_BINDABLE value")
    void shouldHaveAddressNotBindableValue() {
      assertNotNull(
          NetworkErrorCode.valueOf("ADDRESS_NOT_BINDABLE"), "Should have ADDRESS_NOT_BINDABLE");
    }

    @Test
    @DisplayName("should have ADDRESS_IN_USE value")
    void shouldHaveAddressInUseValue() {
      assertNotNull(NetworkErrorCode.valueOf("ADDRESS_IN_USE"), "Should have ADDRESS_IN_USE");
    }

    @Test
    @DisplayName("should have REMOTE_UNREACHABLE value")
    void shouldHaveRemoteUnreachableValue() {
      assertNotNull(
          NetworkErrorCode.valueOf("REMOTE_UNREACHABLE"), "Should have REMOTE_UNREACHABLE");
    }

    @Test
    @DisplayName("should have CONNECTION_REFUSED value")
    void shouldHaveConnectionRefusedValue() {
      assertNotNull(
          NetworkErrorCode.valueOf("CONNECTION_REFUSED"), "Should have CONNECTION_REFUSED");
    }

    @Test
    @DisplayName("should have CONNECTION_RESET value")
    void shouldHaveConnectionResetValue() {
      assertNotNull(NetworkErrorCode.valueOf("CONNECTION_RESET"), "Should have CONNECTION_RESET");
    }

    @Test
    @DisplayName("should have CONNECTION_ABORTED value")
    void shouldHaveConnectionAbortedValue() {
      assertNotNull(
          NetworkErrorCode.valueOf("CONNECTION_ABORTED"), "Should have CONNECTION_ABORTED");
    }

    @Test
    @DisplayName("should have DATAGRAM_TOO_LARGE value")
    void shouldHaveDatagramTooLargeValue() {
      assertNotNull(
          NetworkErrorCode.valueOf("DATAGRAM_TOO_LARGE"), "Should have DATAGRAM_TOO_LARGE");
    }

    @Test
    @DisplayName("should have NAME_UNRESOLVABLE value")
    void shouldHaveNameUnresolvableValue() {
      assertNotNull(NetworkErrorCode.valueOf("NAME_UNRESOLVABLE"), "Should have NAME_UNRESOLVABLE");
    }

    @Test
    @DisplayName("should have TEMPORARY_RESOLVER_FAILURE value")
    void shouldHaveTemporaryResolverFailureValue() {
      assertNotNull(
          NetworkErrorCode.valueOf("TEMPORARY_RESOLVER_FAILURE"),
          "Should have TEMPORARY_RESOLVER_FAILURE");
    }

    @Test
    @DisplayName("should have PERMANENT_RESOLVER_FAILURE value")
    void shouldHavePermanentResolverFailureValue() {
      assertNotNull(
          NetworkErrorCode.valueOf("PERMANENT_RESOLVER_FAILURE"),
          "Should have PERMANENT_RESOLVER_FAILURE");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final NetworkErrorCode errorCode : NetworkErrorCode.values()) {
        assertTrue(ordinals.add(errorCode.ordinal()), "Ordinal should be unique: " + errorCode);
      }
    }

    @Test
    @DisplayName("should have unique names")
    void shouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (final NetworkErrorCode errorCode : NetworkErrorCode.values()) {
        assertTrue(names.add(errorCode.name()), "Name should be unique: " + errorCode);
      }
    }

    @Test
    @DisplayName("should be retrievable by name")
    void shouldBeRetrievableByName() {
      for (final NetworkErrorCode errorCode : NetworkErrorCode.values()) {
        assertEquals(
            errorCode, NetworkErrorCode.valueOf(errorCode.name()), "Should be retrievable by name");
      }
    }
  }

  @Nested
  @DisplayName("Error Category Tests")
  class ErrorCategoryTests {

    @Test
    @DisplayName("should have connection errors")
    void shouldHaveConnectionErrors() {
      final Set<NetworkErrorCode> connectionErrors =
          Set.of(
              NetworkErrorCode.CONNECTION_REFUSED,
              NetworkErrorCode.CONNECTION_RESET,
              NetworkErrorCode.CONNECTION_ABORTED,
              NetworkErrorCode.REMOTE_UNREACHABLE);

      for (final NetworkErrorCode error : connectionErrors) {
        assertNotNull(
            Arrays.stream(NetworkErrorCode.values())
                .filter(e -> e == error)
                .findFirst()
                .orElse(null),
            "Should have connection error: " + error);
      }
    }

    @Test
    @DisplayName("should have address errors")
    void shouldHaveAddressErrors() {
      final Set<NetworkErrorCode> addressErrors =
          Set.of(NetworkErrorCode.ADDRESS_NOT_BINDABLE, NetworkErrorCode.ADDRESS_IN_USE);

      for (final NetworkErrorCode error : addressErrors) {
        assertNotNull(
            Arrays.stream(NetworkErrorCode.values())
                .filter(e -> e == error)
                .findFirst()
                .orElse(null),
            "Should have address error: " + error);
      }
    }

    @Test
    @DisplayName("should have DNS resolver errors")
    void shouldHaveDnsResolverErrors() {
      final Set<NetworkErrorCode> dnsErrors =
          Set.of(
              NetworkErrorCode.NAME_UNRESOLVABLE,
              NetworkErrorCode.TEMPORARY_RESOLVER_FAILURE,
              NetworkErrorCode.PERMANENT_RESOLVER_FAILURE);

      for (final NetworkErrorCode error : dnsErrors) {
        assertNotNull(
            Arrays.stream(NetworkErrorCode.values())
                .filter(e -> e == error)
                .findFirst()
                .orElse(null),
            "Should have DNS error: " + error);
      }
    }

    @Test
    @DisplayName("should have resource errors")
    void shouldHaveResourceErrors() {
      final Set<NetworkErrorCode> resourceErrors =
          Set.of(NetworkErrorCode.OUT_OF_MEMORY, NetworkErrorCode.NEW_SOCKET_LIMIT);

      for (final NetworkErrorCode error : resourceErrors) {
        assertNotNull(
            Arrays.stream(NetworkErrorCode.values())
                .filter(e -> e == error)
                .findFirst()
                .orElse(null),
            "Should have resource error: " + error);
      }
    }

    @Test
    @DisplayName("should have state errors")
    void shouldHaveStateErrors() {
      final Set<NetworkErrorCode> stateErrors =
          Set.of(
              NetworkErrorCode.INVALID_STATE,
              NetworkErrorCode.NOT_IN_PROGRESS,
              NetworkErrorCode.CONCURRENCY_CONFLICT);

      for (final NetworkErrorCode error : stateErrors) {
        assertNotNull(
            Arrays.stream(NetworkErrorCode.values())
                .filter(e -> e == error)
                .findFirst()
                .orElse(null),
            "Should have state error: " + error);
      }
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support switch statement")
    void shouldSupportSwitchStatement() {
      final NetworkErrorCode errorCode = NetworkErrorCode.CONNECTION_REFUSED;

      final String message;
      switch (errorCode) {
        case CONNECTION_REFUSED:
          message = "Connection refused by remote host";
          break;
        case CONNECTION_RESET:
          message = "Connection reset by peer";
          break;
        case TIMEOUT:
          message = "Operation timed out";
          break;
        default:
          message = "Unknown error";
      }

      assertEquals("Connection refused by remote host", message, "Should match switch case");
    }

    @Test
    @DisplayName("should be usable in collections")
    void shouldBeUsableInCollections() {
      final Set<NetworkErrorCode> retryableErrors = new HashSet<>();
      retryableErrors.add(NetworkErrorCode.TIMEOUT);
      retryableErrors.add(NetworkErrorCode.TEMPORARY_RESOLVER_FAILURE);
      retryableErrors.add(NetworkErrorCode.WOULD_BLOCK);

      assertTrue(retryableErrors.contains(NetworkErrorCode.TIMEOUT), "Should contain TIMEOUT");
      assertTrue(
          retryableErrors.contains(NetworkErrorCode.TEMPORARY_RESOLVER_FAILURE),
          "Should contain TEMPORARY_RESOLVER_FAILURE");
    }

    @Test
    @DisplayName("should support error handling pattern")
    void shouldSupportErrorHandlingPattern() {
      final NetworkErrorCode errorCode = NetworkErrorCode.WOULD_BLOCK;

      final boolean shouldRetry =
          errorCode == NetworkErrorCode.WOULD_BLOCK
              || errorCode == NetworkErrorCode.TIMEOUT
              || errorCode == NetworkErrorCode.TEMPORARY_RESOLVER_FAILURE;

      assertTrue(shouldRetry, "WOULD_BLOCK should be retryable");
    }
  }

  @Nested
  @DisplayName("WASI Specification Compliance Tests")
  class WasiSpecificationComplianceTests {

    @Test
    @DisplayName("should cover all WASI network error codes")
    void shouldCoverAllWasiNetworkErrorCodes() {
      // These are the error codes defined in wasi:sockets/network@0.2.0
      final String[] expectedErrorCodes = {
        "UNKNOWN",
        "ACCESS_DENIED",
        "NOT_SUPPORTED",
        "INVALID_ARGUMENT",
        "OUT_OF_MEMORY",
        "TIMEOUT",
        "CONCURRENCY_CONFLICT",
        "NOT_IN_PROGRESS",
        "WOULD_BLOCK",
        "INVALID_STATE",
        "NEW_SOCKET_LIMIT",
        "ADDRESS_NOT_BINDABLE",
        "ADDRESS_IN_USE",
        "REMOTE_UNREACHABLE",
        "CONNECTION_REFUSED",
        "CONNECTION_RESET",
        "CONNECTION_ABORTED",
        "DATAGRAM_TOO_LARGE",
        "NAME_UNRESOLVABLE",
        "TEMPORARY_RESOLVER_FAILURE",
        "PERMANENT_RESOLVER_FAILURE"
      };

      for (final String expectedName : expectedErrorCodes) {
        assertNotNull(
            NetworkErrorCode.valueOf(expectedName), "Should have error code: " + expectedName);
      }

      assertEquals(
          expectedErrorCodes.length, NetworkErrorCode.values().length, "Should have exact count");
    }
  }
}
