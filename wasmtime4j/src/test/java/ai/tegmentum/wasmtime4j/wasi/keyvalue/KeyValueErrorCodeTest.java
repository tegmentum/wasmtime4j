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

package ai.tegmentum.wasmtime4j.wasi.keyvalue;

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
 * Tests for KeyValueErrorCode enum.
 *
 * <p>Verifies enum constants, ordinals, valueOf, values, toString, and switch statement coverage for
 * WASI key-value error codes.
 */
@DisplayName("KeyValueErrorCode Tests")
class KeyValueErrorCodeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should have exactly 16 enum constants")
    void shouldHaveExactlySixteenEnumConstants() {
      final KeyValueErrorCode[] values = KeyValueErrorCode.values();

      assertEquals(16, values.length, "KeyValueErrorCode should have exactly 16 constants");
    }

    @Test
    @DisplayName("should be a valid enum type")
    void shouldBeValidEnumType() {
      assertTrue(
          KeyValueErrorCode.class.isEnum(), "KeyValueErrorCode should be an enum type");
    }

    @Test
    @DisplayName("all constants should be non-null")
    void allConstantsShouldBeNonNull() {
      for (final KeyValueErrorCode code : KeyValueErrorCode.values()) {
        assertNotNull(code, "Every KeyValueErrorCode constant should be non-null");
      }
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have UNKNOWN constant")
    void shouldHaveUnknownConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.UNKNOWN;

      assertNotNull(code, "UNKNOWN should not be null");
      assertEquals("UNKNOWN", code.name(), "Name should be UNKNOWN");
    }

    @Test
    @DisplayName("should have KEY_NOT_FOUND constant")
    void shouldHaveKeyNotFoundConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.KEY_NOT_FOUND;

      assertNotNull(code, "KEY_NOT_FOUND should not be null");
      assertEquals("KEY_NOT_FOUND", code.name(), "Name should be KEY_NOT_FOUND");
    }

    @Test
    @DisplayName("should have KEY_EXISTS constant")
    void shouldHaveKeyExistsConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.KEY_EXISTS;

      assertNotNull(code, "KEY_EXISTS should not be null");
      assertEquals("KEY_EXISTS", code.name(), "Name should be KEY_EXISTS");
    }

    @Test
    @DisplayName("should have INVALID_KEY constant")
    void shouldHaveInvalidKeyConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.INVALID_KEY;

      assertNotNull(code, "INVALID_KEY should not be null");
      assertEquals("INVALID_KEY", code.name(), "Name should be INVALID_KEY");
    }

    @Test
    @DisplayName("should have INVALID_VALUE constant")
    void shouldHaveInvalidValueConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.INVALID_VALUE;

      assertNotNull(code, "INVALID_VALUE should not be null");
      assertEquals("INVALID_VALUE", code.name(), "Name should be INVALID_VALUE");
    }

    @Test
    @DisplayName("should have CAPACITY_EXCEEDED constant")
    void shouldHaveCapacityExceededConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.CAPACITY_EXCEEDED;

      assertNotNull(code, "CAPACITY_EXCEEDED should not be null");
      assertEquals("CAPACITY_EXCEEDED", code.name(), "Name should be CAPACITY_EXCEEDED");
    }

    @Test
    @DisplayName("should have TRANSACTION_CONFLICT constant")
    void shouldHaveTransactionConflictConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.TRANSACTION_CONFLICT;

      assertNotNull(code, "TRANSACTION_CONFLICT should not be null");
      assertEquals(
          "TRANSACTION_CONFLICT", code.name(), "Name should be TRANSACTION_CONFLICT");
    }

    @Test
    @DisplayName("should have TRANSACTION_TIMEOUT constant")
    void shouldHaveTransactionTimeoutConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.TRANSACTION_TIMEOUT;

      assertNotNull(code, "TRANSACTION_TIMEOUT should not be null");
      assertEquals(
          "TRANSACTION_TIMEOUT", code.name(), "Name should be TRANSACTION_TIMEOUT");
    }

    @Test
    @DisplayName("should have CAS_FAILED constant")
    void shouldHaveCasFailedConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.CAS_FAILED;

      assertNotNull(code, "CAS_FAILED should not be null");
      assertEquals("CAS_FAILED", code.name(), "Name should be CAS_FAILED");
    }

    @Test
    @DisplayName("should have NOT_PERMITTED constant")
    void shouldHaveNotPermittedConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.NOT_PERMITTED;

      assertNotNull(code, "NOT_PERMITTED should not be null");
      assertEquals("NOT_PERMITTED", code.name(), "Name should be NOT_PERMITTED");
    }

    @Test
    @DisplayName("should have CONNECTION_FAILED constant")
    void shouldHaveConnectionFailedConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.CONNECTION_FAILED;

      assertNotNull(code, "CONNECTION_FAILED should not be null");
      assertEquals("CONNECTION_FAILED", code.name(), "Name should be CONNECTION_FAILED");
    }

    @Test
    @DisplayName("should have READ_ONLY constant")
    void shouldHaveReadOnlyConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.READ_ONLY;

      assertNotNull(code, "READ_ONLY should not be null");
      assertEquals("READ_ONLY", code.name(), "Name should be READ_ONLY");
    }

    @Test
    @DisplayName("should have TIMEOUT constant")
    void shouldHaveTimeoutConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.TIMEOUT;

      assertNotNull(code, "TIMEOUT should not be null");
      assertEquals("TIMEOUT", code.name(), "Name should be TIMEOUT");
    }

    @Test
    @DisplayName("should have CONSISTENCY_VIOLATION constant")
    void shouldHaveConsistencyViolationConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.CONSISTENCY_VIOLATION;

      assertNotNull(code, "CONSISTENCY_VIOLATION should not be null");
      assertEquals(
          "CONSISTENCY_VIOLATION", code.name(), "Name should be CONSISTENCY_VIOLATION");
    }

    @Test
    @DisplayName("should have REPLICATION_FAILED constant")
    void shouldHaveReplicationFailedConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.REPLICATION_FAILED;

      assertNotNull(code, "REPLICATION_FAILED should not be null");
      assertEquals(
          "REPLICATION_FAILED", code.name(), "Name should be REPLICATION_FAILED");
    }

    @Test
    @DisplayName("should have INTERNAL_ERROR constant")
    void shouldHaveInternalErrorConstant() {
      final KeyValueErrorCode code = KeyValueErrorCode.INTERNAL_ERROR;

      assertNotNull(code, "INTERNAL_ERROR should not be null");
      assertEquals("INTERNAL_ERROR", code.name(), "Name should be INTERNAL_ERROR");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("UNKNOWN should have ordinal 0")
    void unknownShouldHaveOrdinalZero() {
      assertEquals(0, KeyValueErrorCode.UNKNOWN.ordinal(), "UNKNOWN ordinal should be 0");
    }

    @Test
    @DisplayName("KEY_NOT_FOUND should have ordinal 1")
    void keyNotFoundShouldHaveOrdinalOne() {
      assertEquals(
          1, KeyValueErrorCode.KEY_NOT_FOUND.ordinal(), "KEY_NOT_FOUND ordinal should be 1");
    }

    @Test
    @DisplayName("INTERNAL_ERROR should have ordinal 15")
    void internalErrorShouldHaveOrdinalFifteen() {
      assertEquals(
          15,
          KeyValueErrorCode.INTERNAL_ERROR.ordinal(),
          "INTERNAL_ERROR ordinal should be 15");
    }

    @Test
    @DisplayName("ordinals should be sequential starting at 0")
    void ordinalsShouldBeSequential() {
      final KeyValueErrorCode[] values = KeyValueErrorCode.values();

      for (int i = 0; i < values.length; i++) {
        assertEquals(
            i, values[i].ordinal(), "Ordinal should be " + i + " for " + values[i]);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return correct constant for each name")
    void valueOfShouldReturnCorrectConstant() {
      assertEquals(
          KeyValueErrorCode.UNKNOWN,
          KeyValueErrorCode.valueOf("UNKNOWN"),
          "valueOf('UNKNOWN') should return UNKNOWN");
      assertEquals(
          KeyValueErrorCode.KEY_NOT_FOUND,
          KeyValueErrorCode.valueOf("KEY_NOT_FOUND"),
          "valueOf('KEY_NOT_FOUND') should return KEY_NOT_FOUND");
      assertEquals(
          KeyValueErrorCode.KEY_EXISTS,
          KeyValueErrorCode.valueOf("KEY_EXISTS"),
          "valueOf('KEY_EXISTS') should return KEY_EXISTS");
      assertEquals(
          KeyValueErrorCode.INVALID_KEY,
          KeyValueErrorCode.valueOf("INVALID_KEY"),
          "valueOf('INVALID_KEY') should return INVALID_KEY");
      assertEquals(
          KeyValueErrorCode.INVALID_VALUE,
          KeyValueErrorCode.valueOf("INVALID_VALUE"),
          "valueOf('INVALID_VALUE') should return INVALID_VALUE");
      assertEquals(
          KeyValueErrorCode.CAPACITY_EXCEEDED,
          KeyValueErrorCode.valueOf("CAPACITY_EXCEEDED"),
          "valueOf('CAPACITY_EXCEEDED') should return CAPACITY_EXCEEDED");
      assertEquals(
          KeyValueErrorCode.TRANSACTION_CONFLICT,
          KeyValueErrorCode.valueOf("TRANSACTION_CONFLICT"),
          "valueOf('TRANSACTION_CONFLICT') should return TRANSACTION_CONFLICT");
      assertEquals(
          KeyValueErrorCode.TRANSACTION_TIMEOUT,
          KeyValueErrorCode.valueOf("TRANSACTION_TIMEOUT"),
          "valueOf('TRANSACTION_TIMEOUT') should return TRANSACTION_TIMEOUT");
      assertEquals(
          KeyValueErrorCode.CAS_FAILED,
          KeyValueErrorCode.valueOf("CAS_FAILED"),
          "valueOf('CAS_FAILED') should return CAS_FAILED");
      assertEquals(
          KeyValueErrorCode.NOT_PERMITTED,
          KeyValueErrorCode.valueOf("NOT_PERMITTED"),
          "valueOf('NOT_PERMITTED') should return NOT_PERMITTED");
      assertEquals(
          KeyValueErrorCode.CONNECTION_FAILED,
          KeyValueErrorCode.valueOf("CONNECTION_FAILED"),
          "valueOf('CONNECTION_FAILED') should return CONNECTION_FAILED");
      assertEquals(
          KeyValueErrorCode.READ_ONLY,
          KeyValueErrorCode.valueOf("READ_ONLY"),
          "valueOf('READ_ONLY') should return READ_ONLY");
      assertEquals(
          KeyValueErrorCode.TIMEOUT,
          KeyValueErrorCode.valueOf("TIMEOUT"),
          "valueOf('TIMEOUT') should return TIMEOUT");
      assertEquals(
          KeyValueErrorCode.CONSISTENCY_VIOLATION,
          KeyValueErrorCode.valueOf("CONSISTENCY_VIOLATION"),
          "valueOf('CONSISTENCY_VIOLATION') should return CONSISTENCY_VIOLATION");
      assertEquals(
          KeyValueErrorCode.REPLICATION_FAILED,
          KeyValueErrorCode.valueOf("REPLICATION_FAILED"),
          "valueOf('REPLICATION_FAILED') should return REPLICATION_FAILED");
      assertEquals(
          KeyValueErrorCode.INTERNAL_ERROR,
          KeyValueErrorCode.valueOf("INTERNAL_ERROR"),
          "valueOf('INTERNAL_ERROR') should return INTERNAL_ERROR");
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> KeyValueErrorCode.valueOf("INVALID"),
          "valueOf('INVALID') should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("valueOf should throw NullPointerException for null name")
    void valueOfShouldThrowForNullName() {
      assertThrows(
          NullPointerException.class,
          () -> KeyValueErrorCode.valueOf(null),
          "valueOf(null) should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("values should return array of length 16")
    void valuesShouldReturnArrayOfLengthSixteen() {
      assertEquals(
          16,
          KeyValueErrorCode.values().length,
          "values() should return array with 16 elements");
    }

    @Test
    @DisplayName("values should contain all constants")
    void valuesShouldContainAllConstants() {
      final Set<KeyValueErrorCode> valueSet =
          new HashSet<>(Arrays.asList(KeyValueErrorCode.values()));

      assertTrue(
          valueSet.contains(KeyValueErrorCode.UNKNOWN), "values() should contain UNKNOWN");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.KEY_NOT_FOUND),
          "values() should contain KEY_NOT_FOUND");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.KEY_EXISTS),
          "values() should contain KEY_EXISTS");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.INVALID_KEY),
          "values() should contain INVALID_KEY");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.INVALID_VALUE),
          "values() should contain INVALID_VALUE");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.CAPACITY_EXCEEDED),
          "values() should contain CAPACITY_EXCEEDED");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.TRANSACTION_CONFLICT),
          "values() should contain TRANSACTION_CONFLICT");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.TRANSACTION_TIMEOUT),
          "values() should contain TRANSACTION_TIMEOUT");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.CAS_FAILED),
          "values() should contain CAS_FAILED");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.NOT_PERMITTED),
          "values() should contain NOT_PERMITTED");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.CONNECTION_FAILED),
          "values() should contain CONNECTION_FAILED");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.READ_ONLY),
          "values() should contain READ_ONLY");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.TIMEOUT),
          "values() should contain TIMEOUT");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.CONSISTENCY_VIOLATION),
          "values() should contain CONSISTENCY_VIOLATION");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.REPLICATION_FAILED),
          "values() should contain REPLICATION_FAILED");
      assertTrue(
          valueSet.contains(KeyValueErrorCode.INTERNAL_ERROR),
          "values() should contain INTERNAL_ERROR");
    }

    @Test
    @DisplayName("values should return new array each call")
    void valuesShouldReturnNewArrayEachCall() {
      final KeyValueErrorCode[] first = KeyValueErrorCode.values();
      final KeyValueErrorCode[] second = KeyValueErrorCode.values();

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
      for (final KeyValueErrorCode code : KeyValueErrorCode.values()) {
        assertEquals(
            code.name(),
            code.toString(),
            "toString() should match name() for " + code.name());
      }
    }

    @Test
    @DisplayName("toString should return 'UNKNOWN' for UNKNOWN")
    void toStringShouldReturnUnknown() {
      assertEquals(
          "UNKNOWN",
          KeyValueErrorCode.UNKNOWN.toString(),
          "toString() should return 'UNKNOWN'");
    }

    @Test
    @DisplayName("toString should return 'INTERNAL_ERROR' for INTERNAL_ERROR")
    void toStringShouldReturnInternalError() {
      assertEquals(
          "INTERNAL_ERROR",
          KeyValueErrorCode.INTERNAL_ERROR.toString(),
          "toString() should return 'INTERNAL_ERROR'");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should handle all constants in switch statement")
    void shouldHandleAllConstantsInSwitchStatement() {
      for (final KeyValueErrorCode code : KeyValueErrorCode.values()) {
        final String result;
        switch (code) {
          case UNKNOWN:
            result = "unknown";
            break;
          case KEY_NOT_FOUND:
            result = "key_not_found";
            break;
          case KEY_EXISTS:
            result = "key_exists";
            break;
          case INVALID_KEY:
            result = "invalid_key";
            break;
          case INVALID_VALUE:
            result = "invalid_value";
            break;
          case CAPACITY_EXCEEDED:
            result = "capacity_exceeded";
            break;
          case TRANSACTION_CONFLICT:
            result = "transaction_conflict";
            break;
          case TRANSACTION_TIMEOUT:
            result = "transaction_timeout";
            break;
          case CAS_FAILED:
            result = "cas_failed";
            break;
          case NOT_PERMITTED:
            result = "not_permitted";
            break;
          case CONNECTION_FAILED:
            result = "connection_failed";
            break;
          case READ_ONLY:
            result = "read_only";
            break;
          case TIMEOUT:
            result = "timeout";
            break;
          case CONSISTENCY_VIOLATION:
            result = "consistency_violation";
            break;
          case REPLICATION_FAILED:
            result = "replication_failed";
            break;
          case INTERNAL_ERROR:
            result = "internal_error";
            break;
          default:
            result = "unhandled";
        }
        assertTrue(
            Arrays.asList(
                    "unknown",
                    "key_not_found",
                    "key_exists",
                    "invalid_key",
                    "invalid_value",
                    "capacity_exceeded",
                    "transaction_conflict",
                    "transaction_timeout",
                    "cas_failed",
                    "not_permitted",
                    "connection_failed",
                    "read_only",
                    "timeout",
                    "consistency_violation",
                    "replication_failed",
                    "internal_error")
                .contains(result),
            "Switch should handle " + code + " but got: " + result);
      }
    }
  }
}
