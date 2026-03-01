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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for NetworkErrorCode enum.
 *
 * <p>Verifies enum constants, ordinals, valueOf, values, toString, and switch statement coverage
 * for WASI socket network error codes.
 */
@DisplayName("NetworkErrorCode Tests")
class NetworkErrorCodeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have exactly 21 enum constants")
    void shouldHaveExactlyTwentyOneEnumConstants() {
      final NetworkErrorCode[] values = NetworkErrorCode.values();

      assertEquals(21, values.length, "NetworkErrorCode should have exactly 21 constants");
    }

    @Test
    @DisplayName("should be a valid enum type")
    void shouldBeValidEnumType() {
      assertTrue(NetworkErrorCode.class.isEnum(), "NetworkErrorCode should be an enum type");
    }

    @Test
    @DisplayName("all constants should be non-null")
    void allConstantsShouldBeNonNull() {
      for (final NetworkErrorCode code : NetworkErrorCode.values()) {
        assertNotNull(code, "Every NetworkErrorCode constant should be non-null");
      }
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have UNKNOWN constant")
    void shouldHaveUnknownConstant() {
      final NetworkErrorCode code = NetworkErrorCode.UNKNOWN;

      assertNotNull(code, "UNKNOWN should not be null");
      assertEquals("UNKNOWN", code.name(), "Name should be UNKNOWN");
    }

    @Test
    @DisplayName("should have ACCESS_DENIED constant")
    void shouldHaveAccessDeniedConstant() {
      final NetworkErrorCode code = NetworkErrorCode.ACCESS_DENIED;

      assertNotNull(code, "ACCESS_DENIED should not be null");
      assertEquals("ACCESS_DENIED", code.name(), "Name should be ACCESS_DENIED");
    }

    @Test
    @DisplayName("should have NOT_SUPPORTED constant")
    void shouldHaveNotSupportedConstant() {
      final NetworkErrorCode code = NetworkErrorCode.NOT_SUPPORTED;

      assertNotNull(code, "NOT_SUPPORTED should not be null");
      assertEquals("NOT_SUPPORTED", code.name(), "Name should be NOT_SUPPORTED");
    }

    @Test
    @DisplayName("should have INVALID_ARGUMENT constant")
    void shouldHaveInvalidArgumentConstant() {
      final NetworkErrorCode code = NetworkErrorCode.INVALID_ARGUMENT;

      assertNotNull(code, "INVALID_ARGUMENT should not be null");
      assertEquals("INVALID_ARGUMENT", code.name(), "Name should be INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("should have OUT_OF_MEMORY constant")
    void shouldHaveOutOfMemoryConstant() {
      final NetworkErrorCode code = NetworkErrorCode.OUT_OF_MEMORY;

      assertNotNull(code, "OUT_OF_MEMORY should not be null");
      assertEquals("OUT_OF_MEMORY", code.name(), "Name should be OUT_OF_MEMORY");
    }

    @Test
    @DisplayName("should have TIMEOUT constant")
    void shouldHaveTimeoutConstant() {
      final NetworkErrorCode code = NetworkErrorCode.TIMEOUT;

      assertNotNull(code, "TIMEOUT should not be null");
      assertEquals("TIMEOUT", code.name(), "Name should be TIMEOUT");
    }

    @Test
    @DisplayName("should have CONCURRENCY_CONFLICT constant")
    void shouldHaveConcurrencyConflictConstant() {
      final NetworkErrorCode code = NetworkErrorCode.CONCURRENCY_CONFLICT;

      assertNotNull(code, "CONCURRENCY_CONFLICT should not be null");
      assertEquals("CONCURRENCY_CONFLICT", code.name(), "Name should be CONCURRENCY_CONFLICT");
    }

    @Test
    @DisplayName("should have NOT_IN_PROGRESS constant")
    void shouldHaveNotInProgressConstant() {
      final NetworkErrorCode code = NetworkErrorCode.NOT_IN_PROGRESS;

      assertNotNull(code, "NOT_IN_PROGRESS should not be null");
      assertEquals("NOT_IN_PROGRESS", code.name(), "Name should be NOT_IN_PROGRESS");
    }

    @Test
    @DisplayName("should have WOULD_BLOCK constant")
    void shouldHaveWouldBlockConstant() {
      final NetworkErrorCode code = NetworkErrorCode.WOULD_BLOCK;

      assertNotNull(code, "WOULD_BLOCK should not be null");
      assertEquals("WOULD_BLOCK", code.name(), "Name should be WOULD_BLOCK");
    }

    @Test
    @DisplayName("should have INVALID_STATE constant")
    void shouldHaveInvalidStateConstant() {
      final NetworkErrorCode code = NetworkErrorCode.INVALID_STATE;

      assertNotNull(code, "INVALID_STATE should not be null");
      assertEquals("INVALID_STATE", code.name(), "Name should be INVALID_STATE");
    }

    @Test
    @DisplayName("should have NEW_SOCKET_LIMIT constant")
    void shouldHaveNewSocketLimitConstant() {
      final NetworkErrorCode code = NetworkErrorCode.NEW_SOCKET_LIMIT;

      assertNotNull(code, "NEW_SOCKET_LIMIT should not be null");
      assertEquals("NEW_SOCKET_LIMIT", code.name(), "Name should be NEW_SOCKET_LIMIT");
    }

    @Test
    @DisplayName("should have ADDRESS_NOT_BINDABLE constant")
    void shouldHaveAddressNotBindableConstant() {
      final NetworkErrorCode code = NetworkErrorCode.ADDRESS_NOT_BINDABLE;

      assertNotNull(code, "ADDRESS_NOT_BINDABLE should not be null");
      assertEquals("ADDRESS_NOT_BINDABLE", code.name(), "Name should be ADDRESS_NOT_BINDABLE");
    }

    @Test
    @DisplayName("should have ADDRESS_IN_USE constant")
    void shouldHaveAddressInUseConstant() {
      final NetworkErrorCode code = NetworkErrorCode.ADDRESS_IN_USE;

      assertNotNull(code, "ADDRESS_IN_USE should not be null");
      assertEquals("ADDRESS_IN_USE", code.name(), "Name should be ADDRESS_IN_USE");
    }

    @Test
    @DisplayName("should have REMOTE_UNREACHABLE constant")
    void shouldHaveRemoteUnreachableConstant() {
      final NetworkErrorCode code = NetworkErrorCode.REMOTE_UNREACHABLE;

      assertNotNull(code, "REMOTE_UNREACHABLE should not be null");
      assertEquals("REMOTE_UNREACHABLE", code.name(), "Name should be REMOTE_UNREACHABLE");
    }

    @Test
    @DisplayName("should have CONNECTION_REFUSED constant")
    void shouldHaveConnectionRefusedConstant() {
      final NetworkErrorCode code = NetworkErrorCode.CONNECTION_REFUSED;

      assertNotNull(code, "CONNECTION_REFUSED should not be null");
      assertEquals("CONNECTION_REFUSED", code.name(), "Name should be CONNECTION_REFUSED");
    }

    @Test
    @DisplayName("should have CONNECTION_RESET constant")
    void shouldHaveConnectionResetConstant() {
      final NetworkErrorCode code = NetworkErrorCode.CONNECTION_RESET;

      assertNotNull(code, "CONNECTION_RESET should not be null");
      assertEquals("CONNECTION_RESET", code.name(), "Name should be CONNECTION_RESET");
    }

    @Test
    @DisplayName("should have CONNECTION_ABORTED constant")
    void shouldHaveConnectionAbortedConstant() {
      final NetworkErrorCode code = NetworkErrorCode.CONNECTION_ABORTED;

      assertNotNull(code, "CONNECTION_ABORTED should not be null");
      assertEquals("CONNECTION_ABORTED", code.name(), "Name should be CONNECTION_ABORTED");
    }

    @Test
    @DisplayName("should have DATAGRAM_TOO_LARGE constant")
    void shouldHaveDatagramTooLargeConstant() {
      final NetworkErrorCode code = NetworkErrorCode.DATAGRAM_TOO_LARGE;

      assertNotNull(code, "DATAGRAM_TOO_LARGE should not be null");
      assertEquals("DATAGRAM_TOO_LARGE", code.name(), "Name should be DATAGRAM_TOO_LARGE");
    }

    @Test
    @DisplayName("should have NAME_UNRESOLVABLE constant")
    void shouldHaveNameUnresolvableConstant() {
      final NetworkErrorCode code = NetworkErrorCode.NAME_UNRESOLVABLE;

      assertNotNull(code, "NAME_UNRESOLVABLE should not be null");
      assertEquals("NAME_UNRESOLVABLE", code.name(), "Name should be NAME_UNRESOLVABLE");
    }

    @Test
    @DisplayName("should have TEMPORARY_RESOLVER_FAILURE constant")
    void shouldHaveTemporaryResolverFailureConstant() {
      final NetworkErrorCode code = NetworkErrorCode.TEMPORARY_RESOLVER_FAILURE;

      assertNotNull(code, "TEMPORARY_RESOLVER_FAILURE should not be null");
      assertEquals(
          "TEMPORARY_RESOLVER_FAILURE", code.name(), "Name should be TEMPORARY_RESOLVER_FAILURE");
    }

    @Test
    @DisplayName("should have PERMANENT_RESOLVER_FAILURE constant")
    void shouldHavePermanentResolverFailureConstant() {
      final NetworkErrorCode code = NetworkErrorCode.PERMANENT_RESOLVER_FAILURE;

      assertNotNull(code, "PERMANENT_RESOLVER_FAILURE should not be null");
      assertEquals(
          "PERMANENT_RESOLVER_FAILURE", code.name(), "Name should be PERMANENT_RESOLVER_FAILURE");
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return correct constant for each name")
    void valueOfShouldReturnCorrectConstant() {
      assertEquals(
          NetworkErrorCode.UNKNOWN,
          NetworkErrorCode.valueOf("UNKNOWN"),
          "valueOf('UNKNOWN') should return UNKNOWN");
      assertEquals(
          NetworkErrorCode.ACCESS_DENIED,
          NetworkErrorCode.valueOf("ACCESS_DENIED"),
          "valueOf('ACCESS_DENIED') should return ACCESS_DENIED");
      assertEquals(
          NetworkErrorCode.NOT_SUPPORTED,
          NetworkErrorCode.valueOf("NOT_SUPPORTED"),
          "valueOf('NOT_SUPPORTED') should return NOT_SUPPORTED");
      assertEquals(
          NetworkErrorCode.INVALID_ARGUMENT,
          NetworkErrorCode.valueOf("INVALID_ARGUMENT"),
          "valueOf('INVALID_ARGUMENT') should return INVALID_ARGUMENT");
      assertEquals(
          NetworkErrorCode.OUT_OF_MEMORY,
          NetworkErrorCode.valueOf("OUT_OF_MEMORY"),
          "valueOf('OUT_OF_MEMORY') should return OUT_OF_MEMORY");
      assertEquals(
          NetworkErrorCode.TIMEOUT,
          NetworkErrorCode.valueOf("TIMEOUT"),
          "valueOf('TIMEOUT') should return TIMEOUT");
      assertEquals(
          NetworkErrorCode.CONCURRENCY_CONFLICT,
          NetworkErrorCode.valueOf("CONCURRENCY_CONFLICT"),
          "valueOf('CONCURRENCY_CONFLICT') should return CONCURRENCY_CONFLICT");
      assertEquals(
          NetworkErrorCode.NOT_IN_PROGRESS,
          NetworkErrorCode.valueOf("NOT_IN_PROGRESS"),
          "valueOf('NOT_IN_PROGRESS') should return NOT_IN_PROGRESS");
      assertEquals(
          NetworkErrorCode.WOULD_BLOCK,
          NetworkErrorCode.valueOf("WOULD_BLOCK"),
          "valueOf('WOULD_BLOCK') should return WOULD_BLOCK");
      assertEquals(
          NetworkErrorCode.INVALID_STATE,
          NetworkErrorCode.valueOf("INVALID_STATE"),
          "valueOf('INVALID_STATE') should return INVALID_STATE");
      assertEquals(
          NetworkErrorCode.NEW_SOCKET_LIMIT,
          NetworkErrorCode.valueOf("NEW_SOCKET_LIMIT"),
          "valueOf('NEW_SOCKET_LIMIT') should return NEW_SOCKET_LIMIT");
      assertEquals(
          NetworkErrorCode.ADDRESS_NOT_BINDABLE,
          NetworkErrorCode.valueOf("ADDRESS_NOT_BINDABLE"),
          "valueOf('ADDRESS_NOT_BINDABLE') should return ADDRESS_NOT_BINDABLE");
      assertEquals(
          NetworkErrorCode.ADDRESS_IN_USE,
          NetworkErrorCode.valueOf("ADDRESS_IN_USE"),
          "valueOf('ADDRESS_IN_USE') should return ADDRESS_IN_USE");
      assertEquals(
          NetworkErrorCode.REMOTE_UNREACHABLE,
          NetworkErrorCode.valueOf("REMOTE_UNREACHABLE"),
          "valueOf('REMOTE_UNREACHABLE') should return REMOTE_UNREACHABLE");
      assertEquals(
          NetworkErrorCode.CONNECTION_REFUSED,
          NetworkErrorCode.valueOf("CONNECTION_REFUSED"),
          "valueOf('CONNECTION_REFUSED') should return CONNECTION_REFUSED");
      assertEquals(
          NetworkErrorCode.CONNECTION_RESET,
          NetworkErrorCode.valueOf("CONNECTION_RESET"),
          "valueOf('CONNECTION_RESET') should return CONNECTION_RESET");
      assertEquals(
          NetworkErrorCode.CONNECTION_ABORTED,
          NetworkErrorCode.valueOf("CONNECTION_ABORTED"),
          "valueOf('CONNECTION_ABORTED') should return CONNECTION_ABORTED");
      assertEquals(
          NetworkErrorCode.DATAGRAM_TOO_LARGE,
          NetworkErrorCode.valueOf("DATAGRAM_TOO_LARGE"),
          "valueOf('DATAGRAM_TOO_LARGE') should return DATAGRAM_TOO_LARGE");
      assertEquals(
          NetworkErrorCode.NAME_UNRESOLVABLE,
          NetworkErrorCode.valueOf("NAME_UNRESOLVABLE"),
          "valueOf('NAME_UNRESOLVABLE') should return NAME_UNRESOLVABLE");
      assertEquals(
          NetworkErrorCode.TEMPORARY_RESOLVER_FAILURE,
          NetworkErrorCode.valueOf("TEMPORARY_RESOLVER_FAILURE"),
          "valueOf('TEMPORARY_RESOLVER_FAILURE') should return TEMPORARY_RESOLVER_FAILURE");
      assertEquals(
          NetworkErrorCode.PERMANENT_RESOLVER_FAILURE,
          NetworkErrorCode.valueOf("PERMANENT_RESOLVER_FAILURE"),
          "valueOf('PERMANENT_RESOLVER_FAILURE') should return PERMANENT_RESOLVER_FAILURE");
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NetworkErrorCode.valueOf("INVALID"),
          "valueOf('INVALID') should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null name")
    void valueOfShouldThrowForNullName() {
      assertThrows(
          NullPointerException.class,
          () -> NetworkErrorCode.valueOf(null),
          "valueOf(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array of length 21")
    void valuesShouldReturnArrayOfLengthTwentyOne() {
      assertEquals(
          21, NetworkErrorCode.values().length, "values() should return array with 21 elements");
    }

    @Test
    @DisplayName("values should contain all constants")
    void valuesShouldContainAllConstants() {
      final Set<NetworkErrorCode> valueSet =
          new HashSet<>(Arrays.asList(NetworkErrorCode.values()));

      assertTrue(valueSet.contains(NetworkErrorCode.UNKNOWN), "values() should contain UNKNOWN");
      assertTrue(
          valueSet.contains(NetworkErrorCode.ACCESS_DENIED),
          "values() should contain ACCESS_DENIED");
      assertTrue(
          valueSet.contains(NetworkErrorCode.NOT_SUPPORTED),
          "values() should contain NOT_SUPPORTED");
      assertTrue(
          valueSet.contains(NetworkErrorCode.INVALID_ARGUMENT),
          "values() should contain INVALID_ARGUMENT");
      assertTrue(
          valueSet.contains(NetworkErrorCode.OUT_OF_MEMORY),
          "values() should contain OUT_OF_MEMORY");
      assertTrue(valueSet.contains(NetworkErrorCode.TIMEOUT), "values() should contain TIMEOUT");
      assertTrue(
          valueSet.contains(NetworkErrorCode.CONCURRENCY_CONFLICT),
          "values() should contain CONCURRENCY_CONFLICT");
      assertTrue(
          valueSet.contains(NetworkErrorCode.NOT_IN_PROGRESS),
          "values() should contain NOT_IN_PROGRESS");
      assertTrue(
          valueSet.contains(NetworkErrorCode.WOULD_BLOCK), "values() should contain WOULD_BLOCK");
      assertTrue(
          valueSet.contains(NetworkErrorCode.INVALID_STATE),
          "values() should contain INVALID_STATE");
      assertTrue(
          valueSet.contains(NetworkErrorCode.NEW_SOCKET_LIMIT),
          "values() should contain NEW_SOCKET_LIMIT");
      assertTrue(
          valueSet.contains(NetworkErrorCode.ADDRESS_NOT_BINDABLE),
          "values() should contain ADDRESS_NOT_BINDABLE");
      assertTrue(
          valueSet.contains(NetworkErrorCode.ADDRESS_IN_USE),
          "values() should contain ADDRESS_IN_USE");
      assertTrue(
          valueSet.contains(NetworkErrorCode.REMOTE_UNREACHABLE),
          "values() should contain REMOTE_UNREACHABLE");
      assertTrue(
          valueSet.contains(NetworkErrorCode.CONNECTION_REFUSED),
          "values() should contain CONNECTION_REFUSED");
      assertTrue(
          valueSet.contains(NetworkErrorCode.CONNECTION_RESET),
          "values() should contain CONNECTION_RESET");
      assertTrue(
          valueSet.contains(NetworkErrorCode.CONNECTION_ABORTED),
          "values() should contain CONNECTION_ABORTED");
      assertTrue(
          valueSet.contains(NetworkErrorCode.DATAGRAM_TOO_LARGE),
          "values() should contain DATAGRAM_TOO_LARGE");
      assertTrue(
          valueSet.contains(NetworkErrorCode.NAME_UNRESOLVABLE),
          "values() should contain NAME_UNRESOLVABLE");
      assertTrue(
          valueSet.contains(NetworkErrorCode.TEMPORARY_RESOLVER_FAILURE),
          "values() should contain TEMPORARY_RESOLVER_FAILURE");
      assertTrue(
          valueSet.contains(NetworkErrorCode.PERMANENT_RESOLVER_FAILURE),
          "values() should contain PERMANENT_RESOLVER_FAILURE");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final NetworkErrorCode[] first = NetworkErrorCode.values();
      final NetworkErrorCode[] second = NetworkErrorCode.values();

      assertTrue(first != second, "values() should return a new array instance each call");
      assertEquals(
          Arrays.asList(first),
          Arrays.asList(second),
          "values() arrays should have identical contents");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should match name for all constants")
    void toStringShouldMatchNameForAllConstants() {
      for (final NetworkErrorCode code : NetworkErrorCode.values()) {
        assertEquals(
            code.name(), code.toString(), "toString() should match name() for " + code.name());
      }
    }

    @Test
    @DisplayName("toString should return 'UNKNOWN' for UNKNOWN")
    void toStringShouldReturnUnknown() {
      assertEquals(
          "UNKNOWN", NetworkErrorCode.UNKNOWN.toString(), "toString() should return 'UNKNOWN'");
    }

    @Test
    @DisplayName("toString should return 'CONNECTION_REFUSED' for CONNECTION_REFUSED")
    void toStringShouldReturnConnectionRefused() {
      assertEquals(
          "CONNECTION_REFUSED",
          NetworkErrorCode.CONNECTION_REFUSED.toString(),
          "toString() should return 'CONNECTION_REFUSED'");
    }

    @Test
    @DisplayName("toString should return 'PERMANENT_RESOLVER_FAILURE' for that constant")
    void toStringShouldReturnPermanentResolverFailure() {
      assertEquals(
          "PERMANENT_RESOLVER_FAILURE",
          NetworkErrorCode.PERMANENT_RESOLVER_FAILURE.toString(),
          "toString() should return 'PERMANENT_RESOLVER_FAILURE'");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should handle all constants in switch statement")
    void shouldHandleAllConstantsInSwitchStatement() {
      for (final NetworkErrorCode code : NetworkErrorCode.values()) {
        final String result;
        switch (code) {
          case UNKNOWN:
            result = "unknown";
            break;
          case ACCESS_DENIED:
            result = "access_denied";
            break;
          case NOT_SUPPORTED:
            result = "not_supported";
            break;
          case INVALID_ARGUMENT:
            result = "invalid_argument";
            break;
          case OUT_OF_MEMORY:
            result = "out_of_memory";
            break;
          case TIMEOUT:
            result = "timeout";
            break;
          case CONCURRENCY_CONFLICT:
            result = "concurrency_conflict";
            break;
          case NOT_IN_PROGRESS:
            result = "not_in_progress";
            break;
          case WOULD_BLOCK:
            result = "would_block";
            break;
          case INVALID_STATE:
            result = "invalid_state";
            break;
          case NEW_SOCKET_LIMIT:
            result = "new_socket_limit";
            break;
          case ADDRESS_NOT_BINDABLE:
            result = "address_not_bindable";
            break;
          case ADDRESS_IN_USE:
            result = "address_in_use";
            break;
          case REMOTE_UNREACHABLE:
            result = "remote_unreachable";
            break;
          case CONNECTION_REFUSED:
            result = "connection_refused";
            break;
          case CONNECTION_RESET:
            result = "connection_reset";
            break;
          case CONNECTION_ABORTED:
            result = "connection_aborted";
            break;
          case DATAGRAM_TOO_LARGE:
            result = "datagram_too_large";
            break;
          case NAME_UNRESOLVABLE:
            result = "name_unresolvable";
            break;
          case TEMPORARY_RESOLVER_FAILURE:
            result = "temporary_resolver_failure";
            break;
          case PERMANENT_RESOLVER_FAILURE:
            result = "permanent_resolver_failure";
            break;
          default:
            result = "unhandled";
        }
        assertTrue(
            Arrays.asList(
                    "unknown",
                    "access_denied",
                    "not_supported",
                    "invalid_argument",
                    "out_of_memory",
                    "timeout",
                    "concurrency_conflict",
                    "not_in_progress",
                    "would_block",
                    "invalid_state",
                    "new_socket_limit",
                    "address_not_bindable",
                    "address_in_use",
                    "remote_unreachable",
                    "connection_refused",
                    "connection_reset",
                    "connection_aborted",
                    "datagram_too_large",
                    "name_unresolvable",
                    "temporary_resolver_failure",
                    "permanent_resolver_failure")
                .contains(result),
            "Switch should handle " + code + " but got: " + result);
      }
    }
  }
}
