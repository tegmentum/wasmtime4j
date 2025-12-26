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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link KeyValueErrorCode} enum.
 *
 * <p>KeyValueErrorCode represents error codes for WASI-keyvalue operations.
 */
@DisplayName("KeyValueErrorCode Tests")
class KeyValueErrorCodeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(KeyValueErrorCode.class.isEnum(), "KeyValueErrorCode should be an enum");
    }

    @Test
    @DisplayName("should have exactly 16 values")
    void shouldHaveExactlySixteenValues() {
      final KeyValueErrorCode[] values = KeyValueErrorCode.values();
      assertEquals(16, values.length, "Should have exactly 16 error codes");
    }

    @Test
    @DisplayName("should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(KeyValueErrorCode.valueOf("UNKNOWN"), "Should have UNKNOWN");
    }

    @Test
    @DisplayName("should have KEY_NOT_FOUND value")
    void shouldHaveKeyNotFoundValue() {
      assertNotNull(KeyValueErrorCode.valueOf("KEY_NOT_FOUND"), "Should have KEY_NOT_FOUND");
    }

    @Test
    @DisplayName("should have KEY_EXISTS value")
    void shouldHaveKeyExistsValue() {
      assertNotNull(KeyValueErrorCode.valueOf("KEY_EXISTS"), "Should have KEY_EXISTS");
    }

    @Test
    @DisplayName("should have INVALID_KEY value")
    void shouldHaveInvalidKeyValue() {
      assertNotNull(KeyValueErrorCode.valueOf("INVALID_KEY"), "Should have INVALID_KEY");
    }

    @Test
    @DisplayName("should have INVALID_VALUE value")
    void shouldHaveInvalidValueValue() {
      assertNotNull(KeyValueErrorCode.valueOf("INVALID_VALUE"), "Should have INVALID_VALUE");
    }

    @Test
    @DisplayName("should have CAPACITY_EXCEEDED value")
    void shouldHaveCapacityExceededValue() {
      assertNotNull(
          KeyValueErrorCode.valueOf("CAPACITY_EXCEEDED"), "Should have CAPACITY_EXCEEDED");
    }

    @Test
    @DisplayName("should have TRANSACTION_CONFLICT value")
    void shouldHaveTransactionConflictValue() {
      assertNotNull(
          KeyValueErrorCode.valueOf("TRANSACTION_CONFLICT"), "Should have TRANSACTION_CONFLICT");
    }

    @Test
    @DisplayName("should have TRANSACTION_TIMEOUT value")
    void shouldHaveTransactionTimeoutValue() {
      assertNotNull(
          KeyValueErrorCode.valueOf("TRANSACTION_TIMEOUT"), "Should have TRANSACTION_TIMEOUT");
    }

    @Test
    @DisplayName("should have CAS_FAILED value")
    void shouldHaveCasFailedValue() {
      assertNotNull(KeyValueErrorCode.valueOf("CAS_FAILED"), "Should have CAS_FAILED");
    }

    @Test
    @DisplayName("should have NOT_PERMITTED value")
    void shouldHaveNotPermittedValue() {
      assertNotNull(KeyValueErrorCode.valueOf("NOT_PERMITTED"), "Should have NOT_PERMITTED");
    }

    @Test
    @DisplayName("should have CONNECTION_FAILED value")
    void shouldHaveConnectionFailedValue() {
      assertNotNull(
          KeyValueErrorCode.valueOf("CONNECTION_FAILED"), "Should have CONNECTION_FAILED");
    }

    @Test
    @DisplayName("should have READ_ONLY value")
    void shouldHaveReadOnlyValue() {
      assertNotNull(KeyValueErrorCode.valueOf("READ_ONLY"), "Should have READ_ONLY");
    }

    @Test
    @DisplayName("should have TIMEOUT value")
    void shouldHaveTimeoutValue() {
      assertNotNull(KeyValueErrorCode.valueOf("TIMEOUT"), "Should have TIMEOUT");
    }

    @Test
    @DisplayName("should have CONSISTENCY_VIOLATION value")
    void shouldHaveConsistencyViolationValue() {
      assertNotNull(
          KeyValueErrorCode.valueOf("CONSISTENCY_VIOLATION"), "Should have CONSISTENCY_VIOLATION");
    }

    @Test
    @DisplayName("should have REPLICATION_FAILED value")
    void shouldHaveReplicationFailedValue() {
      assertNotNull(
          KeyValueErrorCode.valueOf("REPLICATION_FAILED"), "Should have REPLICATION_FAILED");
    }

    @Test
    @DisplayName("should have INTERNAL_ERROR value")
    void shouldHaveInternalErrorValue() {
      assertNotNull(KeyValueErrorCode.valueOf("INTERNAL_ERROR"), "Should have INTERNAL_ERROR");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final KeyValueErrorCode code : KeyValueErrorCode.values()) {
        assertTrue(ordinals.add(code.ordinal()), "Ordinal should be unique: " + code);
      }
    }

    @Test
    @DisplayName("should have unique names")
    void shouldHaveUniqueNames() {
      final Set<String> names = new HashSet<>();
      for (final KeyValueErrorCode code : KeyValueErrorCode.values()) {
        assertTrue(names.add(code.name()), "Name should be unique: " + code);
      }
    }

    @Test
    @DisplayName("should be retrievable by name")
    void shouldBeRetrievableByName() {
      for (final KeyValueErrorCode code : KeyValueErrorCode.values()) {
        assertEquals(code, KeyValueErrorCode.valueOf(code.name()), "Should be retrievable by name");
      }
    }
  }

  @Nested
  @DisplayName("Error Category Tests")
  class ErrorCategoryTests {

    @Test
    @DisplayName("should have key-related errors")
    void shouldHaveKeyRelatedErrors() {
      final Set<KeyValueErrorCode> keyErrors =
          Set.of(
              KeyValueErrorCode.KEY_NOT_FOUND,
              KeyValueErrorCode.KEY_EXISTS,
              KeyValueErrorCode.INVALID_KEY);

      for (final KeyValueErrorCode error : keyErrors) {
        assertNotNull(error, "Should have key error: " + error);
      }
    }

    @Test
    @DisplayName("should have transaction errors")
    void shouldHaveTransactionErrors() {
      final Set<KeyValueErrorCode> txErrors =
          Set.of(
              KeyValueErrorCode.TRANSACTION_CONFLICT,
              KeyValueErrorCode.TRANSACTION_TIMEOUT,
              KeyValueErrorCode.CAS_FAILED);

      for (final KeyValueErrorCode error : txErrors) {
        assertNotNull(error, "Should have transaction error: " + error);
      }
    }

    @Test
    @DisplayName("should have resource errors")
    void shouldHaveResourceErrors() {
      final Set<KeyValueErrorCode> resourceErrors =
          Set.of(KeyValueErrorCode.CAPACITY_EXCEEDED, KeyValueErrorCode.READ_ONLY);

      for (final KeyValueErrorCode error : resourceErrors) {
        assertNotNull(error, "Should have resource error: " + error);
      }
    }

    @Test
    @DisplayName("should have connection errors")
    void shouldHaveConnectionErrors() {
      final Set<KeyValueErrorCode> connectionErrors =
          Set.of(
              KeyValueErrorCode.CONNECTION_FAILED,
              KeyValueErrorCode.TIMEOUT,
              KeyValueErrorCode.REPLICATION_FAILED);

      for (final KeyValueErrorCode error : connectionErrors) {
        assertNotNull(error, "Should have connection error: " + error);
      }
    }

    @Test
    @DisplayName("should have permission errors")
    void shouldHavePermissionErrors() {
      assertNotNull(KeyValueErrorCode.NOT_PERMITTED, "Should have NOT_PERMITTED");
    }

    @Test
    @DisplayName("should have consistency errors")
    void shouldHaveConsistencyErrors() {
      assertNotNull(KeyValueErrorCode.CONSISTENCY_VIOLATION, "Should have CONSISTENCY_VIOLATION");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support switch statement")
    void shouldSupportSwitchStatement() {
      final KeyValueErrorCode error = KeyValueErrorCode.KEY_NOT_FOUND;

      final String userMessage;
      switch (error) {
        case KEY_NOT_FOUND:
          userMessage = "The requested key does not exist";
          break;
        case KEY_EXISTS:
          userMessage = "The key already exists";
          break;
        case CAPACITY_EXCEEDED:
          userMessage = "Storage capacity exceeded";
          break;
        case TIMEOUT:
          userMessage = "Operation timed out, please retry";
          break;
        case CAS_FAILED:
          userMessage = "Concurrent modification detected";
          break;
        default:
          userMessage = "An error occurred";
      }

      assertEquals("The requested key does not exist", userMessage, "KEY_NOT_FOUND message");
    }

    @Test
    @DisplayName("should be usable in collections")
    void shouldBeUsableInCollections() {
      final Set<KeyValueErrorCode> retryableErrors = new HashSet<>();
      retryableErrors.add(KeyValueErrorCode.TIMEOUT);
      retryableErrors.add(KeyValueErrorCode.TRANSACTION_TIMEOUT);
      retryableErrors.add(KeyValueErrorCode.CAS_FAILED);
      retryableErrors.add(KeyValueErrorCode.REPLICATION_FAILED);

      assertTrue(retryableErrors.contains(KeyValueErrorCode.TIMEOUT), "TIMEOUT is retryable");
      assertTrue(retryableErrors.contains(KeyValueErrorCode.CAS_FAILED), "CAS_FAILED is retryable");
      assertEquals(4, retryableErrors.size(), "Should have 4 retryable errors");
    }

    @Test
    @DisplayName("should support error handling pattern")
    void shouldSupportErrorHandlingPattern() {
      final KeyValueErrorCode error = KeyValueErrorCode.CAS_FAILED;

      final boolean shouldRetry =
          error == KeyValueErrorCode.TIMEOUT
              || error == KeyValueErrorCode.TRANSACTION_TIMEOUT
              || error == KeyValueErrorCode.CAS_FAILED
              || error == KeyValueErrorCode.REPLICATION_FAILED;

      assertTrue(shouldRetry, "CAS_FAILED should be retryable");
    }
  }

  @Nested
  @DisplayName("Database Error Mapping Tests")
  class DatabaseErrorMappingTests {

    @Test
    @DisplayName("should map to common database errors")
    void shouldMapToCommonDatabaseErrors() {
      // Common database errors should have corresponding codes
      assertNotNull(KeyValueErrorCode.KEY_NOT_FOUND, "Like SQL NOT FOUND");
      assertNotNull(KeyValueErrorCode.KEY_EXISTS, "Like SQL DUPLICATE KEY");
      assertNotNull(KeyValueErrorCode.TRANSACTION_CONFLICT, "Like SQL SERIALIZATION FAILURE");
      assertNotNull(KeyValueErrorCode.TIMEOUT, "Like SQL STATEMENT TIMEOUT");
    }

    @Test
    @DisplayName("should map to Redis errors")
    void shouldMapToRedisErrors() {
      // Common Redis error scenarios
      assertNotNull(KeyValueErrorCode.KEY_NOT_FOUND, "Redis: nil for missing key");
      assertNotNull(KeyValueErrorCode.CAPACITY_EXCEEDED, "Redis: OOM");
      assertNotNull(KeyValueErrorCode.READ_ONLY, "Redis: READONLY slave");
    }
  }

  @Nested
  @DisplayName("Optimistic Locking Tests")
  class OptimisticLockingTests {

    @Test
    @DisplayName("should have CAS error for optimistic concurrency")
    void shouldHaveCasErrorForOptimisticConcurrency() {
      // CAS (Compare-And-Swap) failed indicates optimistic lock failure
      final KeyValueErrorCode casError = KeyValueErrorCode.CAS_FAILED;

      assertEquals("CAS_FAILED", casError.name(), "CAS failure for optimistic locking");
    }

    @Test
    @DisplayName("should distinguish between CAS and transaction conflicts")
    void shouldDistinguishBetweenCasAndTransactionConflicts() {
      // CAS is for single-key optimistic locking
      // TRANSACTION_CONFLICT is for multi-key transactions
      final KeyValueErrorCode casError = KeyValueErrorCode.CAS_FAILED;
      final KeyValueErrorCode txError = KeyValueErrorCode.TRANSACTION_CONFLICT;

      assertTrue(
          casError.ordinal() != txError.ordinal(), "CAS and TX conflicts are distinct errors");
    }
  }

  @Nested
  @DisplayName("Distributed System Error Tests")
  class DistributedSystemErrorTests {

    @Test
    @DisplayName("should have replication errors")
    void shouldHaveReplicationErrors() {
      assertNotNull(KeyValueErrorCode.REPLICATION_FAILED, "Replication failure error");
    }

    @Test
    @DisplayName("should have consistency errors")
    void shouldHaveConsistencyErrors() {
      assertNotNull(KeyValueErrorCode.CONSISTENCY_VIOLATION, "Consistency violation error");
    }

    @Test
    @DisplayName("should have connection errors")
    void shouldHaveConnectionErrors() {
      assertNotNull(KeyValueErrorCode.CONNECTION_FAILED, "Connection failure error");
    }

    @Test
    @DisplayName("should support partition tolerance pattern")
    void shouldSupportPartitionTolerancePattern() {
      // During network partition, these errors might occur
      final Set<KeyValueErrorCode> partitionErrors =
          Set.of(
              KeyValueErrorCode.CONNECTION_FAILED,
              KeyValueErrorCode.TIMEOUT,
              KeyValueErrorCode.REPLICATION_FAILED,
              KeyValueErrorCode.CONSISTENCY_VIOLATION);

      assertEquals(4, partitionErrors.size(), "Partition-related errors");
    }
  }
}
